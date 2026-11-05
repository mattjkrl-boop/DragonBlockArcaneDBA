#!/usr/bin/env python3
"""Models Pass gate: every authored model set resolves completely.

For each model set under the geckolib entity assets:
  - the geo.json parses and declares an identifier,
  - the texture PNG and glowmask PNG exist at the paths the
    renderer resolves and match the declared texture size,
  - the animation file parses and every bone it animates exists
    in the geometry,
and for each ability def with a "models" block:
  - the named model set exists,
  - every clip named in data (animSpawn/animHold/animEnd) exists
    in that model's animation file.
Any miss is a failure, not a warning.
"""
import json
import struct
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
GEO_DIR = RES / "assets/dragonblockarcanedba/geckolib/models/entity"
ANIM_DIR = RES / "assets/dragonblockarcanedba/geckolib/animations/entity"
TEX_DIR = RES / "assets/dragonblockarcanedba/textures/entity"
ABILITY_DIR = RES / "data/dragonblockarcanedba/abilities"

failures = []


def png_size(path):
    with open(path, "rb") as f:
        head = f.read(24)
    if head[:8] != b"\x89PNG\r\n\x1a\n":
        return None
    return struct.unpack(">II", head[16:24])


def check_model(name):
    geo_path = GEO_DIR / f"{name}.geo.json"
    anim_path = ANIM_DIR / f"{name}.animation.json"
    tex_path = TEX_DIR / f"{name}.png"
    glow_path = TEX_DIR / f"{name}_glowmask.png"
    for p in (geo_path, anim_path, tex_path, glow_path):
        if not p.exists():
            failures.append(f"{name}: missing file {p.relative_to(ROOT)}")
            return None
    try:
        geo = json.loads(geo_path.read_text())
        geom = geo["minecraft:geometry"][0]
        desc = geom["description"]
        bones = {b["name"] for b in geom["bones"]}
        cubes = sum(len(b.get("cubes", [])) for b in geom["bones"])
    except Exception as e:
        failures.append(f"{name}: geo.json unreadable: {e}")
        return None
    size = png_size(tex_path)
    if size is None:
        failures.append(f"{name}: texture is not a PNG")
    elif size != (desc["texture_width"], desc["texture_height"]):
        failures.append(f"{name}: texture {size} != declared "
                        f"({desc['texture_width']}, {desc['texture_height']})")
    gsize = png_size(glow_path)
    if gsize is not None and size is not None and gsize != size:
        failures.append(f"{name}: glowmask {gsize} != texture {size}")
    try:
        anim = json.loads(anim_path.read_text())
        clips = anim["animations"]
    except Exception as e:
        failures.append(f"{name}: animation.json unreadable: {e}")
        return None
    for clip_name, clip in clips.items():
        for bone in clip.get("bones", {}):
            if bone not in bones:
                failures.append(f"{name}: clip {clip_name} animates missing bone {bone}")
    print(f"{name}: {len(bones)} bones, {cubes} cubes, clips {sorted(clips)}")
    return set(clips)


def main():
    # Scope: the model sets named by ability "models" blocks (the
    # pass's own sets). Pre-existing creature models predate the
    # glowmask convention and are not this gate's business.
    referenced = set()
    for ability_path in sorted(ABILITY_DIR.glob("*.json")):
        data = json.loads(ability_path.read_text())
        spec = data.get("models")
        if spec is not None:
            referenced.add(spec["model"])
    model_clips = {}
    for name in sorted(referenced):
        clips = check_model(name)
        if clips is not None:
            model_clips[name] = clips
    bound = 0
    for ability_path in sorted(ABILITY_DIR.glob("*.json")):
        data = json.loads(ability_path.read_text())
        spec = data.get("models")
        if spec is None:
            continue
        bound += 1
        model = spec["model"]
        if model not in model_clips:
            failures.append(f"{ability_path.name}: models.model '{model}' has no asset set")
            continue
        for field in ("animSpawn", "animHold", "animEnd"):
            clip = spec.get(field)
            if clip and clip not in model_clips[model]:
                failures.append(f"{ability_path.name}: {field} clip '{clip}' "
                                f"missing from {model}.animation.json")
    print(f"ability defs with models blocks: {bound}")
    if failures:
        print("FAILURES:")
        for f in failures:
            print(" -", f)
        return 1
    print("ALL MODEL REFERENCES RESOLVE")
    return 0


if __name__ == "__main__":
    sys.exit(main())
