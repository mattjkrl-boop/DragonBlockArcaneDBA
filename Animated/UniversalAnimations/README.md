# Universal Player Animation Library for Better Player Model (BPM)

## Overview
This library provides character-agnostic, universal player animations converted from the AI-generated Animate Anything library.
The animations are completely decoupled from character models, meshes, and textures.
Any humanoid model using the **Universal Humanoid Skeleton** (`root` -> `head`, `body`, `left_arm`, `right_arm`, `left_leg`, `right_leg`) can immediately play all 22 animations.

## Canonical Universal Skeleton
```
root
├── head
├── body
├── left_arm
│   ├── leftitem          (Item attachment point)
│   └── LeftHandLocator   (BPM native locator alias)
├── right_arm
│   ├── rightitem         (Item attachment point)
│   └── RightHandLocator  (BPM native locator alias)
├── left_leg
└── right_leg
```

## Converted Animations Status (22 Total)
| Animation ID | Duration | Loop Mode | Source ZIP | Status |
| :--- | :--- | :--- | :--- | :--- |
| `attack` | 1.1667s | `False` | `attack.zip` | **Converted (PASS)** |
| `block` | 1.6s | `True` | `block.zip` | **Converted (PASS)** |
| `crawl` | 1.1333s | `True` | `crawl.zip` | **Converted (PASS)** |
| `death` | 6.0s | `hold_on_last_frame` | `death.zip` | **Converted (PASS)** |
| `eat` | 3.3333s | `True` | `eat.zip` | **Converted (PASS)** |
| `fall` | 1.0s | `True` | `fall.zip` | **Converted (PASS)** |
| `idle` | 5.4s | `True` | `idle.zip` | **Converted (PASS)** |
| `item_idle` | 2.5s | `True` | `item_idle.zip` | **Converted (PASS)** |
| `jump` | 0.9s | `True` | `jump.zip` | **Converted (PASS)** |
| `jump_start` | 0.3s | `False` | `jump_start.zip` | **Converted (PASS)** |
| `land` | 0.6s | `False` | `land.zip` | **Converted (PASS)** |
| `punch` | 1.1667s | `False` | `punch.zip` | **Converted (PASS)** |
| `run` | 0.4667s | `True` | `run.zip` | **Converted (PASS)** |
| `running_jump` | 1.9333s | `False` | `running_jump.zip` | **Converted (PASS)** |
| `sleep` | 3.3333s | `hold_on_last_frame` | `sleep.zip` | **Converted (PASS)** |
| `sleep_idle` | 4.0s | `True` | `sleep_idle.zip` | **Converted (PASS)** |
| `sneak_idle` | 0.9333s | `True` | `sneak_idle.zip` | **Converted (PASS)** |
| `sneak_walk` | 1.3333s | `True` | `sneak_walk.zip` | **Converted (PASS)** |
| `use_item` | 3.3333s | `True` | `use_item.zip` | **Converted (PASS)** |
| `use_tool` | 1.3333s | `False` | `use_tool.zip` | **Converted (PASS)** |
| `walk` | 1.0s | `True` | `walk.zip` | **Converted (PASS)** |
| `walk_back` | 1.0s | `True` | `walk_back.zip` | **Converted (PASS)** |

## Bone Mapping Reference
| Source AI Rig Bone(s) | Universal Target Bone | Action / Function |
| :--- | :--- | :--- |
| `root` | `root` | In-place vertical elevation, bobbing, roll/pitch |
| `body` + `body_top0..2` | `body` | Torso tilt, spine twist, leaning |
| `neck` + `head` | `head` | Head nod, look direction, tilt |
| `shoulder_left` + `arm_left_top` | `left_arm` | Left arm swing, block, tool usage |
| `shoulder_right` + `arm_right_top` | `right_arm` | Right arm swing, punch, parry |
| `leg_left_top` | `left_leg` | Left leg forward/back stride |
| `leg_right_top` | `right_leg` | Right leg forward/back stride |
| *(inherits arm)* | `leftitem` / `rightitem` | Held item attachment points (unbaked) |

## Better Player Model (BPM) Installation
1. Copy the folder `Animated/UniversalAnimations/BPM/universal_humanoid` directly to:
   ```
   .minecraft/config/better_player_model/custom/universal_humanoid/
   ```
2. Launch Minecraft with Better Player Model installed.
3. Open the BPM in-game GUI (default hotkey `Y` or ModMenu config) to select the Universal Humanoid model or assign its animations to your player character.

## Rebuilding the Library in the Future
To rebuild or add new animations in the future, simply place new ZIP files in `Animated/GoodAnimations/` and run:
```bash
python tools/build_universal_library.py
```
This runs the complete extraction, inspection, retargeting, manifest generation, and validation automatically.
