#!/usr/bin/env python3
"""
tools/convert_extended_animations.py
------------------------------------
1. Cleans up redundant/duplicate raw animation ZIPs from Animated/Backups/extra_animations/.
2. Retargets all 25 remaining extended animations (Weapons, Sitting/Riding, Emotes, Combat).
3. Merges them into manifest.json and BPM's main.animation.json.
4. Distributes the updated library across all target locations.
"""

import os
import sys
sys.path.insert(0, os.path.abspath("."))
import json
import zipfile
import tempfile
import shutil
from tools.retarget_animations import retarget_single_animation

REDUNDANT_ZIPS = [
    "crouch.zip",
    "crouch_walk.zip",
    "die.zip",
    "jump_end.zip",
    "jump_fall.zip",
    "lie_down.zip",
    "lying_down_idle.zip",
    "punch_left.zip",
    "punch_right.zip",
    "axe_attack.zip",
    "running_jump_start.zip",
    "running_jump_fall.zip",
    "running_jump_end.zip"
]

LOOP_CONFIG = {
    # Emotes & Stances (Loops)
    "dance": True,
    "talk": True,
    "wave": True,
    "zombie_walk": True,
    "shout": True,
    "sit": True,
    "sitting_idle": True,
    "sit_down_to_drive": True,
    "sword_idle": True,
    "axe_idle": True,
    "arm_parry": True,
    "sword_parry": True,
    "axe_parry": True,
    
    # Actions & Transitions (One-shot)
    "kick_left": False,
    "kick_right": False,
    "cross_punch_left": False,
    "cross_punch_right": False,
    "sword_draw": False,
    "sword_sheathe": False,
    "axe_draw": False,
    "axe_sheathe": False,
    "sitting_eat": False,
    "get_up_from_sitting": False,
    "get_up_from_crouch": False,
    "get_up_from_lying_down": False
}

def clean_redundant(extra_dir):
    print("[*] Cleaning up redundant animations from extra_animations...")
    for red in REDUNDANT_ZIPS:
        p = os.path.join(extra_dir, red)
        if os.path.exists(p):
            try:
                os.remove(p)
                print(f"  [-] Removed redundant: {red}")
            except Exception as e:
                print(f"  [!] Error removing {red}: {e}")

def convert_all_extended():
    extra_dir = os.path.abspath("Animated/Backups/extra_animations")
    clean_redundant(extra_dir)
    
    bpm_anim_path = os.path.abspath("Animated/UniversalAnimations/BPM/universal_humanoid/animations/main.animation.json")
    manifest_path = os.path.abspath("Animated/UniversalAnimations/manifest.json")
    
    with open(bpm_anim_path, "r", encoding="utf-8") as f:
        bpm_data = json.load(f)
        
    with open(manifest_path, "r", encoding="utf-8") as f:
        manifest = json.load(f)
        
    animations_dict = bpm_data.get("animations", {})
    manifest_anims = manifest.get("animations", {})
    
    remaining_zips = sorted([f for f in os.listdir(extra_dir) if f.endswith(".zip")])
    print(f"\n[*] Retargeting {len(remaining_zips)} extended animations...")
    
    with tempfile.TemporaryDirectory() as td:
        for zname in remaining_zips:
            anim_id = os.path.splitext(zname)[0]
            zip_path = os.path.join(extra_dir, zname)
            
            extract_folder = os.path.join(td, anim_id)
            os.makedirs(extract_folder, exist_ok=True)
            
            with zipfile.ZipFile(zip_path) as z:
                z.extractall(extract_folder)
                
            glbs = [os.path.join(r, fname) for r, d, fs in os.walk(extract_folder) for fname in fs if fname.endswith(".glb") and "animation" in fname.lower()]
            if not glbs:
                glbs = [os.path.join(r, fname) for r, d, fs in os.walk(extract_folder) for fname in fs if fname.endswith(".glb")]
                
            if not glbs:
                print(f"  [!] No GLB found in {zname}, skipping.")
                continue
                
            glb_path = glbs[0]
            loop_mode = LOOP_CONFIG.get(anim_id, False)
            
            print(f"  [+] Converting: {anim_id} (loop={loop_mode})...")
            anim_obj = retarget_single_animation(glb_path, anim_id, loop_setting=loop_mode)
            animations_dict[anim_id] = anim_obj
            
            manifest_anims[anim_id] = {
                "source_file": zname,
                "duration_seconds": anim_obj.get("animation_length", 1.0),
                "loop_mode": loop_mode,
                "bones": list(anim_obj.get("bones", {}).keys())
            }
            
            # Map essential BPM state aliases:
            if anim_id == "sit":
                animations_dict["ride"] = anim_obj
                animations_dict["riding"] = anim_obj
            elif anim_id == "sitting_idle":
                animations_dict["ride_idle"] = anim_obj
            elif anim_id == "sword_idle":
                animations_dict["sword_walk"] = anim_obj
            elif anim_id == "axe_idle":
                animations_dict["axe_walk"] = anim_obj

    bpm_data["animations"] = animations_dict
    manifest["animations"] = manifest_anims
    
    # Save back to master UniversalAnimations
    with open(bpm_anim_path, "w", encoding="utf-8") as f:
        json.dump(bpm_data, f, indent=2)
    print(f"\n[OK] Updated BPM animation file: {bpm_anim_path} (Total animations: {len(animations_dict)})")
    
    with open(manifest_path, "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=2)
    print(f"[OK] Updated manifest: {manifest_path} (Total animations: {len(manifest_anims)})")
    
    # Copy to mod assets
    mod_bpm_anim = os.path.abspath("src/main/resources/assets/better_player_model/builtin/universal_humanoid/animations/main.animation.json")
    os.makedirs(os.path.dirname(mod_bpm_anim), exist_ok=True)
    shutil.copy2(bpm_anim_path, mod_bpm_anim)
    print(f"[OK] Updated bundled mod assets: {mod_bpm_anim}")
    
    # Copy to run config
    run_bpm_anim = os.path.abspath("run/config/better_player_model/custom/universal_humanoid/animations/main.animation.json")
    if os.path.exists(os.path.dirname(run_bpm_anim)):
        shutil.copy2(bpm_anim_path, run_bpm_anim)
        print(f"[OK] Updated runClient config: {run_bpm_anim}")

if __name__ == "__main__":
    convert_all_extended()
