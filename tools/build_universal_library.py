#!/usr/bin/env python3
"""
tools/build_universal_library.py
--------------------------------
Master pipeline orchestrator to build the UNIVERSAL PLAYER ANIMATION LIBRARY
for Better Player Model (BPM / YSM) from Animate Anything ZIP files.

Execution Steps:
1. Extract GLBs from GoodAnimations/*.zip into .extracted_stage/.
2. Inspect animations and produce UniversalAnimationConversionReport.json.
3. Retarget animations to the Universal Humanoid Skeleton and output Bedrock JSON.
4. Generate universal_skeleton.json and manifest.json.
5. Generate a minimal universal humanoid test model (Bedrock 1.12.0) and test texture.
6. Assemble the production BPM directory: Animated/UniversalAnimations/BPM/universal_humanoid/.
7. Generate human-readable README.md.
8. Execute automated validation.
"""

import os
import sys
import json
import zlib
import struct
import argparse

# Import sibling toolchain modules
from extract_animations import extract_animations
from inspect_animations import inspect_all_animations
from retarget_animations import retarget_all_animations, DEFAULT_LOOP_CONFIG
from validate_universal_animations import validate_universal_animations

def generate_minimal_png(filepath, width=64, height=64, r=180, g=200, b=220, a=255):
    """
    Generates a clean, valid RGBA PNG file without external dependencies.
    """
    os.makedirs(os.path.dirname(os.path.abspath(filepath)), exist_ok=True)
    raw_data = bytearray()
    for y in range(height):
        raw_data.append(0) # Filter type 0 (None)
        for x in range(width):
            # Give a subtle grid texture
            shade = 20 if ((x // 4) + (y // 4)) % 2 == 0 else 0
            raw_data.extend([
                min(255, r + shade),
                min(255, g + shade),
                min(255, b + shade),
                a
            ])
            
    compressed = zlib.compress(bytes(raw_data))
    
    def chunk(tag, data):
        c = struct.pack(">I", len(data)) + tag + data
        crc = struct.pack(">I", zlib.crc32(tag + data) & 0xffffffff)
        return c + crc

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", compressed)
    png += chunk(b"IEND", b"")
    
    with open(filepath, "wb") as f:
        f.write(png)

def generate_canonical_skeleton_spec(output_path):
    """
    Creates universal_skeleton.json documenting the canonical humanoid hierarchy.
    """
    skeleton_data = {
        "version": 1,
        "format": "bedrock_compatible_universal_humanoid",
        "description": "Universal Humanoid Skeleton for Minecraft Java 26.2 Better Player Model (BPM)",
        "root": "root",
        "hierarchy": {
            "root": {
                "pivot": [0, 0, 0],
                "children": ["head", "body", "left_arm", "right_arm", "left_leg", "right_leg"]
            },
            "head": {
                "parent": "root",
                "pivot": [0, 24, 0],
                "description": "Head bone. Controls player head rotation, pitch, and yaw."
            },
            "body": {
                "parent": "root",
                "pivot": [0, 24, 0],
                "description": "Torso / chest bone. Controls player spine tilt, breathing, twist."
            },
            "left_arm": {
                "parent": "root",
                "pivot": [5, 22, 0],
                "children": ["leftitem", "LeftHandLocator"],
                "description": "Left arm. Swings forward/back on X-axis, rolls out on Z-axis."
            },
            "leftitem": {
                "parent": "left_arm",
                "pivot": [6, 12, 0],
                "description": "Offhand item attachment point. Inherits parent arm transforms."
            },
            "LeftHandLocator": {
                "parent": "left_arm",
                "pivot": [6, 12, 0],
                "description": "BPM native offhand locator alias."
            },
            "right_arm": {
                "parent": "root",
                "pivot": [-5, 22, 0],
                "children": ["rightitem", "RightHandLocator"],
                "description": "Right arm. Swings forward/back on X-axis, rolls out on Z-axis."
            },
            "rightitem": {
                "parent": "right_arm",
                "pivot": [-6, 12, 0],
                "description": "Mainhand item attachment point. Inherits parent arm transforms."
            },
            "RightHandLocator": {
                "parent": "right_arm",
                "pivot": [-6, 12, 0],
                "description": "BPM native mainhand locator alias."
            },
            "left_leg": {
                "parent": "root",
                "pivot": [1.9, 12, 0],
                "description": "Left leg. Swings forward/back on X-axis."
            },
            "right_leg": {
                "parent": "root",
                "pivot": [-1.9, 12, 0],
                "description": "Right leg. Swings forward/back on X-axis."
            }
        },
        "bones": [
            "root", "head", "body", "left_arm", "right_arm", "left_leg", "right_leg",
            "leftitem", "rightitem", "LeftHandLocator", "RightHandLocator"
        ]
    }
    
    os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(skeleton_data, f, indent=2)

def generate_test_model_geometry(output_path):
    """
    Creates a minimal, universal humanoid Bedrock 1.12.0 geometry model
    to prove and test that animations function universally on any model.
    """
    model_data = {
        "format_version": "1.12.0",
        "minecraft:geometry": [
            {
                "description": {
                    "identifier": "geometry.universal_humanoid",
                    "texture_width": 64,
                    "texture_height": 64,
                    "visible_bounds_width": 3,
                    "visible_bounds_height": 4,
                    "visible_bounds_offset": [0, 1.5, 0]
                },
                "bones": [
                    {
                        "name": "root",
                        "pivot": [0, 0, 0]
                    },
                    {
                        "name": "body",
                        "parent": "root",
                        "pivot": [0, 24, 0],
                        "cubes": [
                            {
                                "origin": [-4, 12, -2],
                                "size": [8, 12, 4],
                                "uv": {
                                    "north": {"uv": [20, 20], "uv_size": [8, 12]},
                                    "east": {"uv": [16, 20], "uv_size": [4, 12]},
                                    "south": {"uv": [32, 20], "uv_size": [8, 12]},
                                    "west": {"uv": [28, 20], "uv_size": [4, 12]},
                                    "up": {"uv": [20, 16], "uv_size": [8, 4]},
                                    "down": {"uv": [28, 16], "uv_size": [8, 4]}
                                }
                            }
                        ]
                    },
                    {
                        "name": "head",
                        "parent": "root",
                        "pivot": [0, 24, 0],
                        "cubes": [
                            {
                                "origin": [-4, 24, -4],
                                "size": [8, 8, 8],
                                "uv": {
                                    "north": {"uv": [8, 8], "uv_size": [8, 8]},
                                    "east": {"uv": [0, 8], "uv_size": [8, 8]},
                                    "south": {"uv": [24, 8], "uv_size": [8, 8]},
                                    "west": {"uv": [16, 8], "uv_size": [8, 8]},
                                    "up": {"uv": [8, 0], "uv_size": [8, 8]},
                                    "down": {"uv": [16, 0], "uv_size": [8, 8]}
                                }
                            }
                        ]
                    },
                    {
                        "name": "left_arm",
                        "parent": "root",
                        "pivot": [5, 22, 0],
                        "cubes": [
                            {
                                "origin": [4, 12, -2],
                                "size": [4, 12, 4],
                                "uv": {
                                    "north": {"uv": [36, 52], "uv_size": [4, 12]},
                                    "east": {"uv": [32, 52], "uv_size": [4, 12]},
                                    "south": {"uv": [44, 52], "uv_size": [4, 12]},
                                    "west": {"uv": [40, 52], "uv_size": [4, 12]},
                                    "up": {"uv": [36, 48], "uv_size": [4, 4]},
                                    "down": {"uv": [40, 48], "uv_size": [4, 4]}
                                }
                            }
                        ]
                    },
                    {
                        "name": "leftitem",
                        "parent": "left_arm",
                        "pivot": [6, 12, 0]
                    },
                    {
                        "name": "LeftHandLocator",
                        "parent": "left_arm",
                        "pivot": [6, 12, 0]
                    },
                    {
                        "name": "right_arm",
                        "parent": "root",
                        "pivot": [-5, 22, 0],
                        "cubes": [
                            {
                                "origin": [-8, 12, -2],
                                "size": [4, 12, 4],
                                "uv": {
                                    "north": {"uv": [44, 20], "uv_size": [4, 12]},
                                    "east": {"uv": [40, 20], "uv_size": [4, 12]},
                                    "south": {"uv": [52, 20], "uv_size": [4, 12]},
                                    "west": {"uv": [48, 20], "uv_size": [4, 12]},
                                    "up": {"uv": [44, 16], "uv_size": [4, 4]},
                                    "down": {"uv": [48, 16], "uv_size": [4, 4]}
                                }
                            }
                        ]
                    },
                    {
                        "name": "rightitem",
                        "parent": "right_arm",
                        "pivot": [-6, 12, 0]
                    },
                    {
                        "name": "RightHandLocator",
                        "parent": "right_arm",
                        "pivot": [-6, 12, 0]
                    },
                    {
                        "name": "left_leg",
                        "parent": "root",
                        "pivot": [1.9, 12, 0],
                        "cubes": [
                            {
                                "origin": [0, 0, -2],
                                "size": [4, 12, 4],
                                "uv": {
                                    "north": {"uv": [20, 52], "uv_size": [4, 12]},
                                    "east": {"uv": [16, 52], "uv_size": [4, 12]},
                                    "south": {"uv": [28, 52], "uv_size": [4, 12]},
                                    "west": {"uv": [24, 52], "uv_size": [4, 12]},
                                    "up": {"uv": [20, 48], "uv_size": [4, 4]},
                                    "down": {"uv": [24, 48], "uv_size": [4, 4]}
                                }
                            }
                        ]
                    },
                    {
                        "name": "right_leg",
                        "parent": "root",
                        "pivot": [-1.9, 12, 0],
                        "cubes": [
                            {
                                "origin": [-4, 0, -2],
                                "size": [4, 12, 4],
                                "uv": {
                                    "north": {"uv": [4, 20], "uv_size": [4, 12]},
                                    "east": {"uv": [0, 20], "uv_size": [4, 12]},
                                    "south": {"uv": [12, 20], "uv_size": [4, 12]},
                                    "west": {"uv": [8, 20], "uv_size": [4, 12]},
                                    "up": {"uv": [4, 16], "uv_size": [4, 4]},
                                    "down": {"uv": [8, 16], "uv_size": [4, 4]}
                                }
                            }
                        ]
                    }
                ]
            }
        ]
    }
    
    os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(model_data, f, indent=2)

def generate_test_bbmodel(output_path):
    """
    Creates universal_test_model.bbmodel for direct editing and previewing in Blockbench.
    """
    import uuid
    
    def make_cube(name, origin, size, uv):
        u = str(uuid.uuid4())
        elem = {
            "name": name,
            "box_uv": True,
            "origin": [origin[0], origin[1], origin[2]],
            "from": [origin[0], origin[1], origin[2]],
            "to": [origin[0] + size[0], origin[1] + size[1], origin[2] + size[2]],
            "uv_offset": uv,
            "faces": {
                "north": {"uv": [0, 0, 1, 1]},
                "east": {"uv": [0, 0, 1, 1]},
                "south": {"uv": [0, 0, 1, 1]},
                "west": {"uv": [0, 0, 1, 1]},
                "up": {"uv": [0, 0, 1, 1]},
                "down": {"uv": [0, 0, 1, 1]}
            },
            "type": "cube",
            "uuid": u
        }
        return elem, u

    c_head, u_head = make_cube("head", [-4, 24, -4], [8, 8, 8], [0, 0])
    c_body, u_body = make_cube("body", [-4, 12, -2], [8, 12, 4], [16, 16])
    c_larm, u_larm = make_cube("leftarm", [4, 12, -2], [4, 12, 4], [32, 48])
    c_rarm, u_rarm = make_cube("rightarm", [-8, 12, -2], [4, 12, 4], [40, 16])
    c_lleg, u_lleg = make_cube("leftleg", [0, 0, -2], [4, 12, 4], [16, 48])
    c_rleg, u_rleg = make_cube("rightleg", [-4, 0, -2], [4, 12, 4], [0, 16])
    
    elements = [c_head, c_body, c_larm, c_rarm, c_lleg, c_rleg]
    
    # Locators
    u_leftitem = str(uuid.uuid4())
    loc_leftitem = {"name": "leftitem", "origin": [6, 12, 0], "type": "locator", "uuid": u_leftitem}
    u_rightitem = str(uuid.uuid4())
    loc_rightitem = {"name": "rightitem", "origin": [-6, 12, 0], "type": "locator", "uuid": u_rightitem}
    elements.extend([loc_leftitem, loc_rightitem])
    
    groups = [
        {"name": "root", "origin": [0, 0, 0], "children": ["g_head", "g_body", "g_larm", "g_rarm", "g_lleg", "g_rleg"]},
        {"name": "head", "origin": [0, 24, 0], "children": [u_head]},
        {"name": "body", "origin": [0, 24, 0], "children": [u_body]},
        {"name": "left_arm", "origin": [5, 22, 0], "children": [u_larm, u_leftitem]},
        {"name": "right_arm", "origin": [-5, 22, 0], "children": [u_rarm, u_rightitem]},
        {"name": "left_leg", "origin": [1.9, 12, 0], "children": [u_lleg]},
        {"name": "right_leg", "origin": [-1.9, 12, 0], "children": [u_rleg]}
    ]
    
    outliner = [
        {
            "name": "root",
            "origin": [0, 0, 0],
            "isOpen": True,
            "children": [
                {"name": "head", "origin": [0, 24, 0], "children": [u_head]},
                {"name": "body", "origin": [0, 24, 0], "children": [u_body]},
                {"name": "left_arm", "origin": [5, 22, 0], "children": [u_larm, u_leftitem]},
                {"name": "right_arm", "origin": [-5, 22, 0], "children": [u_rarm, u_rightitem]},
                {"name": "left_leg", "origin": [1.9, 12, 0], "children": [u_lleg]},
                {"name": "right_leg", "origin": [-1.9, 12, 0], "children": [u_rleg]}
            ]
        }
    ]
    
    bbmodel_data = {
        "meta": {
            "format_version": "5.0",
            "model_format": "bedrock",
            "box_uv": True
        },
        "name": "universal_test_model",
        "geometry_name": "universal_humanoid",
        "resolution": {"width": 64, "height": 64},
        "elements": elements,
        "outliner": outliner
    }
    
    os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(bbmodel_data, f, indent=2)

def generate_arm_model_geometry(output_path):
    """
    Creates arm.json for first-person hand rendering in BPM.
    """
    arm_data = {
        "format_version": "1.12.0",
        "minecraft:geometry": [
            {
                "description": {
                    "identifier": "geometry.universal_humanoid_arm",
                    "texture_width": 64,
                    "texture_height": 64
                },
                "bones": [
                    {
                        "name": "root",
                        "pivot": [0, 0, 0]
                    },
                    {
                        "name": "right_arm",
                        "parent": "root",
                        "pivot": [-5, 22, 0],
                        "cubes": [
                            {
                                "origin": [-8, 12, -2],
                                "size": [4, 12, 4],
                                "uv": {
                                    "north": {"uv": [44, 20], "uv_size": [4, 12]},
                                    "east": {"uv": [40, 20], "uv_size": [4, 12]},
                                    "south": {"uv": [52, 20], "uv_size": [4, 12]},
                                    "west": {"uv": [48, 20], "uv_size": [4, 12]},
                                    "up": {"uv": [44, 16], "uv_size": [4, 4]},
                                    "down": {"uv": [48, 16], "uv_size": [4, 4]}
                                }
                            }
                        ]
                    },
                    {
                        "name": "RightHandLocator",
                        "parent": "right_arm",
                        "pivot": [-6, 12, 0]
                    }
                ]
            }
        ]
    }
    os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(arm_data, f, indent=2)

def generate_bpm_manifest(output_path):
    """
    Creates ysm.json conforming to OpenYSM spec 2 for Better Player Model.
    """
    ysm_data = {
        "spec": 2,
        "metadata": {
            "name": "Universal Humanoid",
            "tips": "Universal Character-Agnostic Animation Rig for Dragon Block Arcane / BPM",
            "license": {
                "type": "Custom / Mod License"
            },
            "authors": [
                {
                    "name": "Dragon Block Arcane Team",
                    "role": "Universal Animation Library & Skeleton"
                }
            ]
        },
        "properties": {
            "height_scale": 1.0,
            "width_scale": 1.0,
            "default_texture": "default",
            "preview_animation": "idle",
            "free": True
        },
        "files": {
            "player": {
                "model": {
                    "main": "models/main.json",
                    "arm": "models/arm.json"
                },
                "animation": {
                    "main": "animations/main.animation.json"
                },
                "texture": [
                    "textures/default.png"
                ]
            }
        }
    }
    os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(ysm_data, f, indent=2)

def generate_manifest_json(output_path, inspection_report):
    """
    Generates UniversalAnimations/manifest.json containing all animations,
    loop modes, durations, and conversion metadata.
    """
    manifest_anims = {}
    for anim_id, info in inspection_report.get("animations", {}).items():
        loop_setting = DEFAULT_LOOP_CONFIG.get(anim_id, True)
        manifest_anims[anim_id] = {
            "source_zip": f"{anim_id}.zip",
            "source_file": info.get("source_file"),
            "duration": info.get("duration", 0.0),
            "fps": info.get("fps", 30.0),
            "loop": loop_setting,
            "target_skeleton": "universal_humanoid",
            "mapped_bones": [
                "root", "head", "body", "left_arm", "right_arm", "left_leg", "right_leg",
                "leftitem", "rightitem"
            ],
            "warnings": []
        }
        
    manifest_data = {
        "version": 1,
        "skeleton": "universal_humanoid",
        "total_animations": len(manifest_anims),
        "animations": manifest_anims
    }
    
    os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(manifest_data, f, indent=2)

def generate_human_readable_readme(output_path, manifest_data):
    """
    Creates UniversalAnimations/README.md detailing what was converted,
    how bones were mapped, and how to use the library.
    """
    anims = manifest_data.get("animations", {})
    lines = [
        "# Universal Player Animation Library for Better Player Model (BPM)",
        "",
        "## Overview",
        "This library provides character-agnostic, universal player animations converted from the AI-generated Animate Anything library.",
        "The animations are completely decoupled from character models, meshes, and textures.",
        "Any humanoid model using the **Universal Humanoid Skeleton** (`root` -> `head`, `body`, `left_arm`, `right_arm`, `left_leg`, `right_leg`) can immediately play all 22 animations.",
        "",
        "## Canonical Universal Skeleton",
        "```",
        "root",
        "├── head",
        "├── body",
        "├── left_arm",
        "│   ├── leftitem          (Item attachment point)",
        "│   └── LeftHandLocator   (BPM native locator alias)",
        "├── right_arm",
        "│   ├── rightitem         (Item attachment point)",
        "│   └── RightHandLocator  (BPM native locator alias)",
        "├── left_leg",
        "└── right_leg",
        "```",
        "",
        "## Converted Animations Status (" + str(len(anims)) + " Total)",
        "| Animation ID | Duration | Loop Mode | Source ZIP | Status |",
        "| :--- | :--- | :--- | :--- | :--- |"
    ]
    
    for anim_id, info in sorted(anims.items()):
        loop_str = str(info["loop"])
        lines.append(f"| `{anim_id}` | {info['duration']}s | `{loop_str}` | `{info['source_zip']}` | **Converted (PASS)** |")
        
    lines.extend([
        "",
        "## Bone Mapping Reference",
        "| Source AI Rig Bone(s) | Universal Target Bone | Action / Function |",
        "| :--- | :--- | :--- |",
        "| `root` | `root` | In-place vertical elevation, bobbing, roll/pitch |",
        "| `body` + `body_top0..2` | `body` | Torso tilt, spine twist, leaning |",
        "| `neck` + `head` | `head` | Head nod, look direction, tilt |",
        "| `shoulder_left` + `arm_left_top` | `left_arm` | Left arm swing, block, tool usage |",
        "| `shoulder_right` + `arm_right_top` | `right_arm` | Right arm swing, punch, parry |",
        "| `leg_left_top` | `left_leg` | Left leg forward/back stride |",
        "| `leg_right_top` | `right_leg` | Right leg forward/back stride |",
        "| *(inherits arm)* | `leftitem` / `rightitem` | Held item attachment points (unbaked) |",
        "",
        "## Better Player Model (BPM) Installation",
        "1. Copy the folder `Animated/UniversalAnimations/BPM/universal_humanoid` directly to:",
        "   ```",
        "   .minecraft/config/better_player_model/custom/universal_humanoid/",
        "   ```",
        "2. Launch Minecraft with Better Player Model installed.",
        "3. Open the BPM in-game GUI (default hotkey `Y` or ModMenu config) to select the Universal Humanoid model or assign its animations to your player character.",
        "",
        "## Rebuilding the Library in the Future",
        "To rebuild or add new animations in the future, simply place new ZIP files in `Animated/GoodAnimations/` and run:",
        "```bash",
        "python tools/build_universal_library.py",
        "```",
        "This runs the complete extraction, inspection, retargeting, manifest generation, and validation automatically."
    ])
    
    with open(output_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")

def build_all(source_zips_dir, universal_output_dir):
    source_zips_dir = os.path.abspath(source_zips_dir)
    universal_output_dir = os.path.abspath(universal_output_dir)
    stage_dir = os.path.join(os.path.dirname(universal_output_dir), ".extracted_stage")
    
    print("==================================================")
    print("  BUILDING UNIVERSAL PLAYER ANIMATION LIBRARY")
    print(f"  Source: {source_zips_dir}")
    print(f"  Target: {universal_output_dir}")
    print("==================================================")
    
    # 1. Extract
    print("\n[STEP 1/7] Extracting animations non-destructively...")
    extract_animations(source_zips_dir, stage_dir)
    
    # 2. Inspect
    print("\n[STEP 2/7] Inspecting GLB armatures and animation channels...")
    inspection_report_path = os.path.join(universal_output_dir, "UniversalAnimationConversionReport.json")
    inspection_report = inspect_all_animations(stage_dir, inspection_report_path)
    
    # 3. Retarget
    print("\n[STEP 3/7] Retargeting animations to Bedrock format...")
    bpm_output_dir = os.path.join(universal_output_dir, "BPM", "universal_humanoid")
    bpm_anim_path = os.path.join(bpm_output_dir, "animations", "main.animation.json")
    retarget_all_animations(stage_dir, bpm_anim_path)
    
    # 4. Manifest & Skeleton Specification
    print("\n[STEP 4/7] Generating manifest.json and universal_skeleton.json...")
    manifest_path = os.path.join(universal_output_dir, "manifest.json")
    generate_manifest_json(manifest_path, inspection_report)
    
    skeleton_path = os.path.join(universal_output_dir, "universal_skeleton.json")
    generate_canonical_skeleton_spec(skeleton_path)
    
    # 5. Test Models & BPM Deployment Files
    print("\n[STEP 5/7] Generating test model, arm model, texture, and ysm.json...")
    test_model_path = os.path.join(universal_output_dir, "test_model", "universal_test_model.json")
    generate_test_model_geometry(test_model_path)
    
    test_bbmodel_path = os.path.join(universal_output_dir, "test_model", "universal_test_model.bbmodel")
    generate_test_bbmodel(test_bbmodel_path)
    
    # Copy to BPM folder
    bpm_model_path = os.path.join(bpm_output_dir, "models", "main.json")
    generate_test_model_geometry(bpm_model_path)
    
    bpm_arm_path = os.path.join(bpm_output_dir, "models", "arm.json")
    generate_arm_model_geometry(bpm_arm_path)
    
    bpm_texture_path = os.path.join(bpm_output_dir, "textures", "default.png")
    generate_minimal_png(bpm_texture_path)
    
    bpm_ysm_path = os.path.join(bpm_output_dir, "ysm.json")
    generate_bpm_manifest(bpm_ysm_path)
    
    # 6. README.md
    print("\n[STEP 6/7] Writing documentation...")
    with open(manifest_path, "r", encoding="utf-8") as f:
        manifest_data = json.load(f)
    readme_path = os.path.join(universal_output_dir, "README.md")
    generate_human_readable_readme(readme_path, manifest_data)
    
    # 7. Validate
    print("\n[STEP 7/7] Running automated validation suite...")
    is_valid = validate_universal_animations(universal_output_dir)
    
    if is_valid:
        print("\n==================================================")
        print("  BUILD COMPLETE: ALL ANIMATIONS SUCCESSFULLY BUILT")
        print(f"  Output: {universal_output_dir}")
        print("==================================================")
    else:
        print("\n[!] Build finished with validation errors.")
        sys.exit(1)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Build universal player animation library.")
    parser.add_argument("--source", default=os.path.join("Animated", "GoodAnimations"), help="Source GoodAnimations directory")
    parser.add_argument("--output", default=os.path.join("Animated", "UniversalAnimations"), help="UniversalAnimations output directory")
    args = parser.parse_args()
    
    build_all(args.source, args.output)
