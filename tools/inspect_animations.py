#!/usr/bin/env python3
"""
tools/inspect_animations.py
---------------------------
Inspects every extracted GLB animation file, analyzing armature hierarchy,
animation channels, durations, frame rates, and bone names.
Outputs UniversalAnimationConversionReport.json.
"""

import os
import sys
import json
import struct
import argparse

def parse_glb_header_and_json(glb_path):
    with open(glb_path, "rb") as f:
        magic, ver, length = struct.unpack("<4sII", f.read(12))
        if magic != b"glTF":
            raise ValueError(f"Not a valid GLB file: {glb_path}")
        chunk_len, chunk_type = struct.unpack("<I4s", f.read(8))
        if chunk_type != b"JSON":
            raise ValueError(f"Expected JSON chunk in GLB: {glb_path}")
        json_bytes = f.read(chunk_len)
        gltf = json.loads(json_bytes.decode("utf-8"))
        
        # Read BIN chunk header if present
        bin_len, bin_type = struct.unpack("<I4s", f.read(8))
        bin_data = f.read(bin_len)
        
    return gltf, bin_data

def inspect_single_glb(glb_path):
    gltf, bin_data = parse_glb_header_and_json(glb_path)
    nodes = gltf.get("nodes", [])
    accessors = gltf.get("accessors", [])
    animations = gltf.get("animations", [])
    
    node_names = [n.get("name", f"node_{i}") for i, n in enumerate(nodes)]
    
    anim_reports = []
    for anim in animations:
        anim_name = anim.get("name", "unnamed")
        channels = anim.get("channels", [])
        samplers = anim.get("samplers", [])
        
        animated_bones = set()
        channel_details = []
        min_time = float("inf")
        max_time = float("-inf")
        frame_counts = []
        
        for ch in channels:
            target_node_idx = ch["target"]["node"]
            target_path = ch["target"]["path"]
            node_name = node_names[target_node_idx] if target_node_idx < len(node_names) else f"node_{target_node_idx}"
            animated_bones.add(node_name)
            
            sampler_idx = ch["sampler"]
            sampler = samplers[sampler_idx]
            input_acc = accessors[sampler["input"]]
            output_acc = accessors[sampler["output"]]
            
            if "min" in input_acc and input_acc["min"]:
                min_time = min(min_time, input_acc["min"][0])
            if "max" in input_acc and input_acc["max"]:
                max_time = max(max_time, input_acc["max"][0])
            frame_counts.append(input_acc.get("count", 0))
            
            channel_details.append({
                "bone": node_name,
                "property": target_path,
                "keyframes_count": input_acc.get("count", 0),
                "interpolation": sampler.get("interpolation", "LINEAR")
            })
            
        duration = round(max(0.0, max_time - min_time), 4) if min_time != float("inf") else 0.0
        max_frames = max(frame_counts) if frame_counts else 0
        fps = round(max_frames / duration, 2) if duration > 0 and max_frames > 1 else 30.0
        
        anim_reports.append({
            "source_animation": anim_name,
            "duration": duration,
            "fps": fps,
            "keyframes_count": max_frames,
            "bones": sorted(list(animated_bones)),
            "channels": channel_details
        })
        
    return {
        "source_file": glb_path,
        "skeleton_nodes": node_names,
        "animations": anim_reports
    }

def inspect_all_animations(stage_dir, output_report_path=None):
    stage_dir = os.path.abspath(stage_dir)
    print(f"[*] Inspecting GLB files in {stage_dir}...")
    
    report = {
        "source": stage_dir,
        "animations": {}
    }
    
    anim_dirs = sorted([d for d in os.listdir(stage_dir) if os.path.isdir(os.path.join(stage_dir, d))])
    
    for anim_id in anim_dirs:
        anim_folder = os.path.join(stage_dir, anim_id)
        glbs = [f for f in os.listdir(anim_folder) if f.lower().endswith(".glb")]
        if not glbs:
            continue
            
        glb_path = os.path.join(anim_folder, glbs[0])
        details = inspect_single_glb(glb_path)
        
        primary_anim = details["animations"][0] if details["animations"] else {}
        report["animations"][anim_id] = {
            "source_file": os.path.relpath(glb_path, os.path.dirname(stage_dir)),
            "source_animation": primary_anim.get("source_animation", ""),
            "duration": primary_anim.get("duration", 0.0),
            "fps": primary_anim.get("fps", 30.0),
            "keyframes_count": primary_anim.get("keyframes_count", 0),
            "bones": primary_anim.get("bones", []),
            "channels_count": len(primary_anim.get("channels", []))
        }
        print(f"  [i] {anim_id:15}: duration={primary_anim.get('duration', 0.0)}s, bones={len(primary_anim.get('bones', []))}, fps={primary_anim.get('fps', 30.0)}")
        
    if output_report_path:
        os.makedirs(os.path.dirname(os.path.abspath(output_report_path)), exist_ok=True)
        with open(output_report_path, "w", encoding="utf-8") as f:
            json.dump(report, f, indent=2)
        print(f"[OK] Saved inspection report to {output_report_path}")
        
    return report

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Inspect GLB animation files.")
    parser.add_argument("--stage", default=os.path.join("Animated", ".extracted_stage"), help="Staged animations directory")
    parser.add_argument("--output", default=os.path.join("Animated", "UniversalAnimations", "UniversalAnimationConversionReport.json"), help="Output JSON report path")
    args = parser.parse_args()
    
    inspect_all_animations(args.stage, args.output)
