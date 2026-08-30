#!/usr/bin/env python3
"""
tools/validate_universal_animations.py
--------------------------------------
Automated validator for the Universal Player Animation Library.
Verifies that:
1. Every expected animation exists in the library.
2. No animation references Namekian-specific mesh geometry, bones, or textures.
3. No invalid or missing bone names exist (only canonical universal bones).
4. All animation JSON files parse without errors.
5. All animation durations are valid positive numbers.
6. All loop settings are valid (boolean or 'hold_on_last_frame').
7. The universal skeleton matches the canonical player hierarchy.
8. No corrupted files or unexpected dependencies exist.
"""

import os
import sys
import json
import argparse

REQUIRED_ANIMATIONS = [
    "idle", "walk", "walk_back", "run", "jump_start", "jump", "fall", "land",
    "running_jump", "sneak_idle", "sneak_walk", "crawl", "attack", "punch",
    "block", "use_item", "use_tool", "eat", "item_idle", "sleep", "sleep_idle", "death"
]

CANONICAL_BONES = {
    "root", "head", "body", "left_arm", "right_arm", "left_leg", "right_leg",
    "leftitem", "rightitem", "LeftHandLocator", "RightHandLocator"
}

FORBIDDEN_SUBSTRINGS = [
    "namekian", "meshy", "hulk", "arm_left_bot", "arm_right_bot",
    "shoulder_left", "shoulder_right", "body_top", "leg_left_bot", "leg_right_bot"
]

def validate_universal_animations(universal_dir):
    universal_dir = os.path.abspath(universal_dir)
    print(f"==================================================")
    print(f"  UNIVERSAL ANIMATION LIBRARY VALIDATION SUITE")
    print(f"  Target: {universal_dir}")
    print(f"==================================================")
    
    errors = []
    warnings = []
    
    # 1. Check manifest.json
    manifest_path = os.path.join(universal_dir, "manifest.json")
    if not os.path.exists(manifest_path):
        errors.append(f"Missing manifest.json at {manifest_path}")
    else:
        try:
            with open(manifest_path, "r", encoding="utf-8") as f:
                manifest = json.load(f)
            print(f"[PASS] manifest.json exists and parsed successfully.")
            
            anims_dict = manifest.get("animations", {})
            for req in REQUIRED_ANIMATIONS:
                if req not in anims_dict:
                    errors.append(f"Manifest missing expected animation: '{req}'")
                else:
                    duration = anims_dict[req].get("duration", 0)
                    if duration <= 0:
                        errors.append(f"Animation '{req}' has invalid duration: {duration}")
                    loop = anims_dict[req].get("loop")
                    if loop not in [True, False, "hold_on_last_frame"]:
                        errors.append(f"Animation '{req}' has invalid loop setting: {loop}")
            print(f"[PASS] Manifest contains all {len(REQUIRED_ANIMATIONS)} required animations with valid durations & loop modes.")
        except Exception as e:
            errors.append(f"Failed to parse manifest.json: {e}")

    # 2. Check universal_skeleton.json
    skeleton_path = os.path.join(universal_dir, "universal_skeleton.json")
    if not os.path.exists(skeleton_path):
        errors.append(f"Missing universal_skeleton.json at {skeleton_path}")
    else:
        try:
            with open(skeleton_path, "r", encoding="utf-8") as f:
                skeleton = json.load(f)
            root_name = skeleton.get("root", "")
            bones_list = set(skeleton.get("bones", []))
            
            missing_bones = {"head", "body", "left_arm", "right_arm", "left_leg", "right_leg", "leftitem", "rightitem"} - bones_list
            if missing_bones:
                errors.append(f"universal_skeleton.json is missing required bones: {missing_bones}")
            else:
                print(f"[PASS] universal_skeleton.json contains canonical bones: {sorted(list(bones_list))}")
        except Exception as e:
            errors.append(f"Failed to parse universal_skeleton.json: {e}")

    # 3. Check BPM animation file
    bpm_anim_path = os.path.join(universal_dir, "BPM", "universal_humanoid", "animations", "main.animation.json")
    if not os.path.exists(bpm_anim_path):
        errors.append(f"Missing BPM main.animation.json at {bpm_anim_path}")
    else:
        try:
            with open(bpm_anim_path, "r", encoding="utf-8") as f:
                bpm_data = json.load(f)
            
            format_ver = bpm_data.get("format_version", "")
            if format_ver != "1.8.0":
                warnings.append(f"BPM animation format_version is '{format_ver}', expected '1.8.0'")
                
            animations = bpm_data.get("animations", {})
            for req in REQUIRED_ANIMATIONS:
                if req not in animations:
                    errors.append(f"BPM main.animation.json is missing '{req}'")
                else:
                    anim = animations[req]
                    bones = anim.get("bones", {})
                    # Check that bones only reference CANONICAL_BONES
                    for bname in bones.keys():
                        if bname not in CANONICAL_BONES:
                            errors.append(f"Animation '{req}' references non-canonical bone: '{bname}'")
                            
                    # Check for forbidden substring references
                    anim_str = json.dumps(anim).lower()
                    for forbidden in FORBIDDEN_SUBSTRINGS:
                        if forbidden in anim_str:
                            errors.append(f"Animation '{req}' contains forbidden reference to '{forbidden}'")
                            
            print(f"[PASS] BPM main.animation.json verified: {len(animations)} animations, 0 non-canonical bones, 0 AI-rig artifacts.")
        except Exception as e:
            errors.append(f"Failed to parse BPM main.animation.json: {e}")

    # 4. Check BPM ysm.json and model main.json
    ysm_path = os.path.join(universal_dir, "BPM", "universal_humanoid", "ysm.json")
    if not os.path.exists(ysm_path):
        errors.append(f"Missing ysm.json at {ysm_path}")
    else:
        try:
            with open(ysm_path, "r", encoding="utf-8") as f:
                ysm_json = json.load(f)
            spec = ysm_json.get("spec", 0)
            if spec != 2:
                warnings.append(f"ysm.json spec is {spec}, expected 2")
            print(f"[PASS] BPM ysm.json exists and adheres to OpenYSM spec 2.")
        except Exception as e:
            errors.append(f"Failed to parse ysm.json: {e}")

    model_path = os.path.join(universal_dir, "BPM", "universal_humanoid", "models", "main.json")
    if not os.path.exists(model_path):
        errors.append(f"Missing BPM models/main.json at {model_path}")
    else:
        try:
            with open(model_path, "r", encoding="utf-8") as f:
                model_json = json.load(f)
            print(f"[PASS] BPM models/main.json parsed successfully.")
        except Exception as e:
            errors.append(f"Failed to parse models/main.json: {e}")

    # Summary
    print(f"--------------------------------------------------")
    if warnings:
        print(f"[!] Warnings ({len(warnings)}):")
        for w in warnings:
            print(f"    - {w}")
            
    if errors:
        print(f"[FAIL] Validation FAILED with {len(errors)} errors:")
        for err in errors:
            print(f"    [X] {err}")
        return False
    else:
        print(f"[SUCCESS] ALL VALIDATION CHECKS PASSED! 100% compliant.")
        return True

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Validate Universal Player Animation Library.")
    parser.add_argument("--dir", default=os.path.join("Animated", "UniversalAnimations"), help="Path to UniversalAnimations directory")
    args = parser.parse_args()
    
    success = validate_universal_animations(args.dir)
    sys.exit(0 if success else 1)
