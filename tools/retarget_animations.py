#!/usr/bin/env python3
"""
tools/retarget_animations.py
----------------------------
Retargets animations from Animate Anything / Meshy AI GLB skeletons to the
Universal Minecraft Humanoid Player Skeleton for Better Player Model (BPM).

Mathematical Pipeline:
1. Parse GLB binary buffers (accessors, samplers, animation channels).
2. For each keyframe time t:
   - Calculate hierarchical forward kinematics for torso, head, arms, legs, and root.
   - Map from Animate Anything T-pose basis into Minecraft humanoid hanging posture.
   - Decompose into clean Euler angles [rx, ry, rz] in degrees for Bedrock format 1.8.0.
   - Clean up AI rig artifacts (mesh bones, helper bones, fingers, facial markers).
3. Output valid Bedrock animation JSON structures compatible with BPM and Blockbench.
"""

import os
import sys
import json
import struct
import math
import argparse
import numpy as np
from scipy.spatial.transform import Rotation as R

# Standard loop configurations for the 22 universal animations
DEFAULT_LOOP_CONFIG = {
    "idle": True,
    "walk": True,
    "walk_back": True,
    "run": True,
    "jump_start": False,
    "jump": True,
    "fall": True,
    "land": False,
    "running_jump": False,
    "sneak_idle": True,
    "sneak_walk": True,
    "crawl": True,
    "attack": False,
    "punch": False,
    "block": True,
    "use_item": True,
    "use_tool": False,
    "eat": True,
    "item_idle": True,
    "sleep": "hold_on_last_frame",
    "sleep_idle": True,
    "death": "hold_on_last_frame"
}

def parse_glb(glb_path):
    with open(glb_path, "rb") as f:
        magic, ver, length = struct.unpack("<4sII", f.read(12))
        if magic != b"glTF":
            raise ValueError(f"Invalid GLB: {glb_path}")
        chunk_len, chunk_type = struct.unpack("<I4s", f.read(8))
        json_data = f.read(chunk_len)
        gltf = json.loads(json_data.decode("utf-8"))
        
        bin_len, bin_type = struct.unpack("<I4s", f.read(8))
        bin_data = f.read(bin_len)
        
    return gltf, bin_data

class GlbAnimationExtractor:
    def __init__(self, glb_path):
        self.gltf, self.bin_data = parse_glb(glb_path)
        self.nodes = self.gltf.get("nodes", [])
        self.accessors = self.gltf.get("accessors", [])
        self.bviews = self.gltf.get("bufferViews", [])
        self.animations = self.gltf.get("animations", [])
        
        self.parent_map = {}
        for i, n in enumerate(self.nodes):
            for c in n.get("children", []):
                self.parent_map[c] = i
                
        self.node_by_name = {n.get("name"): i for i, n in enumerate(self.nodes)}
        
    def _read_accessor(self, acc_idx):
        acc = self.accessors[acc_idx]
        bv = self.bviews[acc["bufferView"]]
        offset = bv.get("byteOffset", 0) + acc.get("byteOffset", 0)
        count = acc["count"]
        acc_type = acc["type"]
        
        if acc_type == "SCALAR":
            return list(struct.unpack(f"<{count}f", self.bin_data[offset:offset + count * 4]))
        elif acc_type == "VEC3":
            return [struct.unpack(f"<3f", self.bin_data[offset + i * 12:offset + i * 12 + 12]) for i in range(count)]
        elif acc_type == "VEC4":
            return [struct.unpack(f"<4f", self.bin_data[offset + i * 16:offset + i * 16 + 16]) for i in range(count)]
        else:
            return []

    def get_animation_data(self, anim_index=0):
        if not self.animations or anim_index >= len(self.animations):
            return None
            
        anim = self.animations[anim_index]
        channels = {}
        all_times = set()
        
        for ch in anim.get("channels", []):
            node_idx = ch["target"]["node"]
            node_name = self.nodes[node_idx].get("name", "")
            prop = ch["target"]["path"]
            sampler = anim["samplers"][ch["sampler"]]
            
            times = self._read_accessor(sampler["input"])
            values = self._read_accessor(sampler["output"])
            
            channels[(node_name, prop)] = (times, values)
            all_times.update(times)
            
        sorted_times = sorted(list(all_times))
        return channels, sorted_times

    def evaluate_node_transform(self, node_name, prop, t, channels, default_val):
        key = (node_name, prop)
        if key not in channels:
            # Fall back to node static property
            node_idx = self.node_by_name.get(node_name)
            if node_idx is not None:
                n = self.nodes[node_idx]
                if prop in n:
                    return n[prop]
            return default_val
            
        times, values = channels[key]
        if not times:
            return default_val
        if len(times) == 1 or t <= times[0]:
            return values[0]
        if t >= times[-1]:
            return values[-1]
            
        # Linear search / binary search for nearest
        idx = min(range(len(times)), key=lambda i: abs(times[i] - t))
        return values[idx]

def vector_to_euler_bedrock(vec, is_left=True):
    """
    Converts a 3D direction vector (pointing along the limb length)
    into Bedrock Euler angles [rx, ry, rz] in degrees,
    assuming resting limb hangs straight DOWN at [0, -1, 0].
    """
    vx, vy, vz = vec[0], vec[1], vec[2]
    # Normalize
    length = math.sqrt(vx * vx + vy * vy + vz * vz)
    if length < 1e-6:
        return [0.0, 0.0, 0.0]
    vx, vy, vz = vx / length, vy / length, vz / length
    
    # Forward/backward pitch around X:
    # In MC Bedrock, +rx pitches the limb forward (Z decreases / increases)
    rx_rad = math.atan2(vz, -vy)
    
    # Outward/inward roll around Z:
    rz_rad = math.atan2(vx, -vy)
    
    rx_deg = round(math.degrees(rx_rad), 3)
    rz_deg = round(math.degrees(rz_rad), 3)
    ry_deg = 0.0
    
    return [rx_deg, ry_deg, rz_deg]

def retarget_single_animation(glb_path, anim_id, loop_setting=None):
    """
    Retargets a single animation from GLB to universal Bedrock animation format.
    """
    extractor = GlbAnimationExtractor(glb_path)
    anim_data = extractor.get_animation_data(0)
    if not anim_data:
        raise ValueError(f"No animation found in {glb_path}")
        
    channels, sorted_times = anim_data
    if not sorted_times:
        raise ValueError(f"No keyframe timestamps found in {glb_path}")
        
    t_min = sorted_times[0]
    t_max = sorted_times[-1]
    duration = round(max(0.01, t_max - t_min), 4)
    
    if loop_setting is None:
        loop_setting = DEFAULT_LOOP_CONFIG.get(anim_id, True)
        
    # Chains to calculate forward kinematics
    torso_chain = ["body", "body_top0", "body_top1", "body_top2"]
    head_chain = ["neck", "head"]
    larm_chain = ["shoulder_left", "arm_left_top"]
    rarm_chain = ["shoulder_right", "arm_right_top"]
    lleg_chain = ["leg_left_top"]
    rleg_chain = ["leg_right_top"]
    
    # Store keyframes for each universal bone
    bones_output = {
        "root": {"rotation": {}, "position": {}},
        "body": {"rotation": {}},
        "head": {"rotation": {}},
        "left_arm": {"rotation": {}},
        "right_arm": {"rotation": {}},
        "left_leg": {"rotation": {}},
        "right_leg": {"rotation": {}}
    }
    
    # Get base rest height of body/pelvis
    base_body_trans = extractor.evaluate_node_transform("body", "translation", t_min, channels, [0, 37.0, 0])
    base_y = base_body_trans[1]
    
    # Sample at ~30 fps or using original keyframes
    # For clean looping Bedrock data, use 30fps uniform or direct timestamps
    for t in sorted_times:
        rel_t = round(t - t_min, 4)
        t_str = f"{rel_t:.4f}".rstrip("0").rstrip(".")
        if t_str == "": t_str = "0.0"
        
        # 1. ROOT MOTION (Vertical bobbing and position)
        body_trans = extractor.evaluate_node_transform("body", "translation", t, channels, base_body_trans)
        root_rot_quat = extractor.evaluate_node_transform("root", "rotation", t, channels, [-0.7071, 0, 0, 0.7071])
        
        # Scale vertical delta to Minecraft player scale (~1.8m tall, 24-32 units)
        # GLB character is ~70 units tall, so scale factor is ~0.4 - 0.5
        dy = (body_trans[1] - base_y) * 0.4
        dx = (body_trans[0] - base_body_trans[0]) * 0.4
        dz = (body_trans[2] - base_body_trans[2]) * 0.4
        
        # For crawl/sleep/death, body translation delta may be larger
        if anim_id in ["crawl", "sneak_idle", "sneak_walk"]:
            # lower body down
            dy -= 2.0
        elif anim_id in ["sleep", "sleep_idle"]:
            dy -= 10.0
            
        bones_output["root"]["position"][t_str] = [round(dx, 3), round(dy, 3), round(dz, 3)]
        
        # Root tilt (e.g. for sleep, lie down, death)
        if anim_id in ["sleep", "sleep_idle"]:
            bones_output["root"]["rotation"][t_str] = [90.0, 0.0, 0.0]
        elif anim_id == "death":
            # Rotate body onto floor as time progresses
            progress = min(1.0, rel_t / max(0.1, duration))
            bones_output["root"]["rotation"][t_str] = [round(90.0 * progress, 2), 0.0, 0.0]
        else:
            bones_output["root"]["rotation"][t_str] = [0.0, 0.0, 0.0]
            
        # 2. TORSO / BODY ROTATION
        r_torso = R.identity()
        for b in torso_chain:
            q = extractor.evaluate_node_transform(b, "rotation", t, channels, [0, 0, 0, 1])
            r_torso = r_torso * R.from_quat(q)
            
        body_euler = r_torso.as_euler("xyz", degrees=True)
        # Filter small spine tilt into clean player torso angles
        # Clamp to avoid extreme bending
        bx = max(-45.0, min(45.0, body_euler[0]))
        by = max(-45.0, min(45.0, body_euler[1]))
        bz = max(-45.0, min(45.0, body_euler[2]))
        bones_output["body"]["rotation"][t_str] = [round(bx, 3), round(by, 3), round(bz, 3)]
        
        # 3. HEAD ROTATION
        r_head = R.identity()
        for b in head_chain:
            q = extractor.evaluate_node_transform(b, "rotation", t, channels, [0, 0, 0, 1])
            r_head = r_head * R.from_quat(q)
            
        head_euler = r_head.as_euler("xyz", degrees=True)
        hx = max(-60.0, min(60.0, head_euler[0]))
        hy = max(-60.0, min(60.0, head_euler[1]))
        hz = max(-45.0, min(45.0, head_euler[2]))
        bones_output["head"]["rotation"][t_str] = [round(hx, 3), round(hy, 3), round(hz, 3)]
        
        # 4. LEFT ARM ROTATION
        r_larm = r_torso
        for b in larm_chain:
            q = extractor.evaluate_node_transform(b, "rotation", t, channels, [0, 0, 0, 1])
            r_larm = r_larm * R.from_quat(q)
            
        # Bone length is along local +Y in GLB
        v_larm = r_larm.apply([0, 1, 0])
        larm_euler = vector_to_euler_bedrock(v_larm, is_left=True)
        bones_output["left_arm"]["rotation"][t_str] = larm_euler
        
        # 5. RIGHT ARM ROTATION
        r_rarm = r_torso
        for b in rarm_chain:
            q = extractor.evaluate_node_transform(b, "rotation", t, channels, [0, 0, 0, 1])
            r_rarm = r_rarm * R.from_quat(q)
            
        v_rarm = r_rarm.apply([0, 1, 0])
        rarm_euler = vector_to_euler_bedrock(v_rarm, is_left=False)
        bones_output["right_arm"]["rotation"][t_str] = rarm_euler
        
        # 6. LEFT LEG ROTATION
        r_lleg = R.identity()
        for b in lleg_chain:
            q = extractor.evaluate_node_transform(b, "rotation", t, channels, [0, 0, 0, 1])
            r_lleg = r_lleg * R.from_quat(q)
            
        v_lleg = r_lleg.apply([0, 1, 0])
        lleg_euler = vector_to_euler_bedrock(v_lleg, is_left=True)
        bones_output["left_leg"]["rotation"][t_str] = lleg_euler
        
        # 7. RIGHT LEG ROTATION
        r_rleg = R.identity()
        for b in rleg_chain:
            q = extractor.evaluate_node_transform(b, "rotation", t, channels, [0, 0, 0, 1])
            r_rleg = r_rleg * R.from_quat(q)
            
        v_rleg = r_rleg.apply([0, 1, 0])
        rleg_euler = vector_to_euler_bedrock(v_rleg, is_left=False)
        bones_output["right_leg"]["rotation"][t_str] = rleg_euler
        
    return {
        "loop": loop_setting,
        "animation_length": duration,
        "bones": bones_output
    }

def retarget_all_animations(stage_dir, output_bedrock_json=None):
    """
    Retargets all animations in stage_dir into a single Bedrock animation package.
    """
    stage_dir = os.path.abspath(stage_dir)
    anim_dirs = sorted([d for d in os.listdir(stage_dir) if os.path.isdir(os.path.join(stage_dir, d))])
    
    bedrock_anims = {}
    
    for anim_id in anim_dirs:
        anim_folder = os.path.join(stage_dir, anim_id)
        glbs = [f for f in os.listdir(anim_folder) if f.lower().endswith(".glb")]
        if not glbs:
            continue
            
        glb_path = os.path.join(anim_folder, glbs[0])
        print(f"[*] Retargeting {anim_id} from {os.path.basename(glb_path)}...")
        
        anim_obj = retarget_single_animation(glb_path, anim_id)
        bedrock_anims[anim_id] = anim_obj
        
        # Also register common BPM aliases:
        if anim_id == "sneak_idle":
            bedrock_anims["sneaking"] = anim_obj
        elif anim_id == "sneak_walk":
            bedrock_anims["sneak"] = anim_obj
        elif anim_id == "attack":
            bedrock_anims["swing_hand"] = anim_obj
        elif anim_id == "use_item":
            bedrock_anims["use_mainhand"] = anim_obj
            
    result = {
        "format_version": "1.8.0",
        "animations": bedrock_anims
    }
    
    if output_bedrock_json:
        os.makedirs(os.path.dirname(os.path.abspath(output_bedrock_json)), exist_ok=True)
        with open(output_bedrock_json, "w", encoding="utf-8") as f:
            json.dump(result, f, indent=2)
        print(f"[OK] Generated Bedrock animation file with {len(bedrock_anims)} entries at {output_bedrock_json}")
        
    return result

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Retarget GLB animations to Bedrock format.")
    parser.add_argument("--stage", default=os.path.join("Animated", ".extracted_stage"), help="Staged animations directory")
    parser.add_argument("--output", default=os.path.join("Animated", "UniversalAnimations", "BPM", "universal_humanoid", "animations", "main.animation.json"), help="Output Bedrock animation JSON")
    args = parser.parse_args()
    
    retarget_all_animations(args.stage, args.output)
