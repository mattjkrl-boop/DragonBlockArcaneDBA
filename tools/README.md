# Universal Player Animation Library Toolchain

This directory contains the automated pipeline to process, inspect, retarget, and validate animations from AI-generated Animate Anything ZIP files into the **Universal Player Animation Library** for Minecraft Java 26.2 and Better Player Model (BPM).

## Toolchain Architecture

```
tools/
├── extract_animations.py          # Non-destructively extracts GLB animations from ZIPs into staging
├── inspect_animations.py          # Parses GLB armatures, tracks, and outputs conversion reports
├── retarget_animations.py         # Mathematically retargets motions to canonical humanoid skeleton
├── build_universal_library.py     # Master orchestrator script that runs the full pipeline
├── validate_universal_animations.py # Automated test suite checking 100% compliance
└── README.md                      # This documentation
```

## Quick Start: One-Command Rebuild

Whenever you place new Animate Anything ZIP files into `Animated/GoodAnimations/`, simply run:

```bash
python tools/build_universal_library.py
```

This single command will:
1. Extract any new or updated animation GLBs to `.extracted_stage/` without modifying or deleting your original ZIPs.
2. Inspect the skeleton and animation channels, updating `UniversalAnimationConversionReport.json`.
3. Retarget the animations to the universal humanoid player skeleton, outputting clean Bedrock format (`1.8.0`).
4. Generate/update `manifest.json` and `universal_skeleton.json`.
5. Generate the minimal test model and first-person arm model.
6. Assemble the ready-to-copy BPM pack in `Animated/UniversalAnimations/BPM/universal_humanoid/`.
7. Update `UniversalAnimations/README.md`.
8. Run `validate_universal_animations.py` to guarantee zero errors, valid durations, and proper bone bindings.

---

## Individual Tool Usage

### 1. `extract_animations.py`
Extracts GLBs from ZIPs into a staging directory.
```bash
python tools/extract_animations.py --source Animated/GoodAnimations --output Animated/.extracted_stage
```

### 2. `inspect_animations.py`
Scans extracted GLB files and records durations, fps, and bone channels.
```bash
python tools/inspect_animations.py --stage Animated/.extracted_stage --output Animated/UniversalAnimations/UniversalAnimationConversionReport.json
```

### 3. `retarget_animations.py`
Retargets motion tracks from Animate Anything's T-pose armature to Bedrock JSON format for the universal humanoid skeleton.
```bash
python tools/retarget_animations.py --stage Animated/.extracted_stage --output Animated/UniversalAnimations/BPM/universal_humanoid/animations/main.animation.json
```

### 4. `validate_universal_animations.py`
Runs the automated test suite verifying file integrity, bone name validity, duration checks, and lack of AI rig artifacts.
```bash
python tools/validate_universal_animations.py --dir Animated/UniversalAnimations
```

---

## Canonical Universal Skeleton

All animations are retargeted strictly to this universal player hierarchy:

```
root
├── head
├── body
├── left_arm
│   ├── leftitem          (Item attachment locator)
│   └── LeftHandLocator   (BPM native locator alias)
├── right_arm
│   ├── rightitem         (Item attachment locator)
│   └── RightHandLocator  (BPM native locator alias)
├── left_leg
└── right_leg
```

Any character model using this hierarchy (Goku, Naruto, Namekian, Yardrat, etc.) will immediately support the entire universal animation library.
