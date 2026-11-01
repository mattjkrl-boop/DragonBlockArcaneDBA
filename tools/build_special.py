#!/usr/bin/env python3
"""
Phase 1 special builder for Dragon Block Arcane: Whis Staff (angel attendant staff with
floating orb and halo ring), Bansho Fan (radiating rib fan), Blaster Gun (energy pistol).
Same asset conventions as the other builders. Whis's staff follows Dragon Ball canon:
dark staff, gold collar, black orb floating in a gold ring.

Run from the repo root: python3 tools/build_special.py [weapon_id ...]
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from build_swords import cube, paint, TEX  # noqa: E402
from build_polearms import animations  # noqa: E402
from PIL import Image, ImageDraw  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/dragonblockarcanedba")

SPECS = {
    "whis_staff": {
        "kind": "staff", "pommel": "gem", "swing_len": 0.5, "charge_tilt": -5, "bounds": 6,
        "pal": {"blade_light": (22, 22, 32), "blade_dark": (12, 12, 18),
                "grip_main": (30, 32, 46), "grip_second": (20, 22, 32),
                "metal": (206, 168, 66), "glow": (232, 196, 96),
                "metal_dark": (148, 118, 42), "accent": (232, 196, 96)},
    },
    "bansho_fan": {
        "kind": "fan", "pommel": "gem", "swing_len": 0.42, "charge_tilt": -9, "bounds": 9,
        "pal": {"blade_light": (34, 118, 122), "blade_dark": (20, 74, 80),
                "grip_main": (198, 160, 64), "grip_second": (34, 27, 22),
                "metal": (206, 168, 66), "glow": (128, 232, 214),
                "metal_dark": (148, 118, 42), "accent": (196, 44, 52)},
    },
    "blaster_gun": {
        "kind": "blaster", "pommel": "gem", "swing_len": 0.3, "charge_tilt": -3, "bounds": 5,
        "pal": {"blade_light": (218, 222, 230), "blade_dark": (72, 78, 90),
                "grip_main": (32, 34, 40), "grip_second": (22, 24, 28),
                "metal": (124, 132, 144), "glow": (255, 150, 50),
                "metal_dark": (70, 76, 86), "accent": (255, 150, 50)},
    },
}


def staff(spec):
    bones_cubes = {"shaft": [], "head": [], "head_glow": []}
    s = bones_cubes["shaft"]
    s.append(cube([-0.48, -13.0, -0.48], [0.96, 26.5, 0.96], "grip_main"))
    s.append(cube([-0.62, -1.2, -0.62], [1.24, 2.4, 1.24], "metal"))     # hand ring
    s.append(cube([-0.62, -13.6, -0.62], [1.24, 1.0, 1.24], "metal"))    # butt cap
    h = bones_cubes["head"]
    h.append(cube([-0.66, 13.5, -0.66], [1.32, 1.1, 1.32], "metal"))     # collar
    # floating black orb: three stepped layers
    h.append(cube([-1.3, 15.4, -1.3], [2.6, 1.1, 2.6], "blade_light", "blade_dark"))
    h.append(cube([-1.8, 16.5, -1.8], [3.6, 1.3, 3.6], "blade_light", "blade_dark"))
    h.append(cube([-1.3, 17.8, -1.3], [2.6, 1.1, 2.6], "blade_light", "blade_dark"))
    # gold halo ring around the orb
    h.append(cube([-2.9, 16.85, -0.28], [5.8, 0.55, 0.56], "metal"))
    h.append(cube([-0.28, 16.85, -2.9], [0.56, 0.55, 5.8], "metal"))
    g = bones_cubes["head_glow"]
    g.append(cube([-0.3, 18.9, -0.3], [0.6, 0.5, 0.6], "glow"))
    return bones_cubes


def fan(spec):
    bones_cubes = {"shaft": [], "head": [], "head_glow": []}
    s = bones_cubes["shaft"]
    s.append(cube([-0.6, -6.5, -0.6], [1.2, 8.5, 1.2], "grip_second"))
    y = -6.0
    while y < 1.2:
        s.append(cube([-0.72, y, -0.72], [1.44, 0.7, 1.44], "metal"))
        y += 1.5
    s.append(cube([-0.66, -7.6, -0.66], [1.32, 1.1, 1.32], "metal"))
    # tassel
    for dx in (-0.4, 0.0, 0.4):
        s.append(cube([dx - 0.14, -10.4, -0.14], [0.28, 2.8, 0.28], "accent"))
    h = bones_cubes["head"]
    g = bones_cubes["head_glow"]
    pivot = [0, 2, 0]
    # Membrane panels sit between ribs. Like the ribs, each panel is placed VERTICAL
    # in rest space and rotated about the pivot; pre-offsetting the center along the
    # angled direction as well would double-transform it.
    for i in range(6):
        mid = math.radians(-46 + i * 18.4 + 9.2)
        rot = [0, 0, -math.degrees(mid)]
        h.append(cube([-1.65, 4.0, -0.06], [3.3, 9.2, 0.12], "blade_light", "blade_dark",
                      rotation=rot, pivot=pivot))
        g.append(cube([-1.5, 12.7, -0.1], [3.0, 0.36, 0.2], "glow",
                      rotation=rot, pivot=pivot))
    # ribs
    for i in range(7):
        a = math.radians(-46 + i * 18.4)
        rot = [0, 0, -math.degrees(a)]
        h.append(cube([-0.34, 2.0, -0.13], [0.68, 11.6, 0.26], "metal",
                      rotation=rot, pivot=pivot))
    return bones_cubes


def blaster(spec):
    bones_cubes = {"shaft": [], "head": [], "head_glow": []}
    b = bones_cubes["shaft"]  # body
    b.append(cube([-1.1, -2.2, -0.8], [2.2, 6.4, 1.6], "blade_light", "blade_dark"))
    b.append(cube([-0.9, 4.2, -0.62], [1.8, 1.2, 1.24], "metal"))
    b.append(cube([-0.55, -8.6, -0.55], [1.1, 6.6, 1.1], "grip_main",
                  rotation=[0, 0, 14], pivot=[0, -2.2, 0]))
    b.append(cube([-1.25, -3.4, -0.2], [0.4, 1.6, 0.4], "metal_dark"))  # trigger guard post
    h = bones_cubes["head"]  # barrel
    h.append(cube([-0.62, 5.4, -0.62], [1.24, 5.4, 1.24], "blade_dark", "metal_dark"))
    h.append(cube([-0.75, 10.4, -0.75], [1.5, 0.9, 1.5], "metal"))
    h.append(cube([-1.5, 8.6, -0.3], [0.6, 2.2, 0.6], "metal"))          # fins
    h.append(cube([0.9, 8.6, -0.3], [0.6, 2.2, 0.6], "metal"))
    g = bones_cubes["head_glow"]
    g.append(cube([-0.6, 0.1, 0.82], [1.2, 2.4, 0.24], "glow"))          # energy cell
    g.append(cube([-0.62, 6.2, -0.66], [0.28, 3.8, 1.32], "glow"))       # barrel strip
    g.append(cube([0.34, 6.2, -0.66], [0.28, 3.8, 1.32], "glow"))
    return bones_cubes


BUILDERS = {"staff": staff, "fan": fan, "blaster": blaster}
BONE_NAMES = {"staff": ("shaft", "head", "head_glow"),
              "fan": ("shaft", "head", "head_glow"),
              "blaster": ("shaft", "head", "head_glow")}


def render_preview(geo, atlas):
    cubes = [c for b in geo["minecraft:geometry"][0]["bones"] for c in b["cubes"]]
    min_x = min(c["origin"][0] for c in cubes)
    max_x = max(c["origin"][0] + c["size"][0] for c in cubes)
    min_y = min(c["origin"][1] for c in cubes)
    max_y = max(c["origin"][1] + c["size"][1] for c in cubes)
    scale, pad = 9, 10
    w = int((max_x - min_x) * scale) + pad * 2
    h = int((max_y - min_y) * scale) + pad * 2
    img = Image.new("RGBA", (w, h), (18, 18, 24, 255))
    d = ImageDraw.Draw(img)
    for c in sorted(cubes, key=lambda c: (c["size"][0] * c["size"][2])):
        uv = c["uv"]["north"]
        cx = int(uv["uv"][0] + uv["uv_size"][0] / 2)
        cy = int(uv["uv"][1] + uv["uv_size"][1] / 2)
        color = atlas.getpixel((cx, cy))
        x0 = pad + (c["origin"][0] - min_x) * scale
        y1 = h - pad - (c["origin"][1] - min_y) * scale
        x1 = x0 + c["size"][0] * scale
        y0 = y1 - c["size"][1] * scale
        d.rectangle([x0, y0, x1, y1], fill=color, outline=(0, 0, 0, 120))
    return img


def build_one(wid):
    spec = SPECS[wid]
    parts = BUILDERS[spec["kind"]](spec)
    n1, n2, n3 = BONE_NAMES[spec["kind"]]
    bones = [
        {"name": "weapon", "pivot": [0, 2, 0], "cubes": []},
        {"name": n1, "parent": "weapon", "pivot": [0, 2, 0], "cubes": parts[n1]},
        {"name": n2, "parent": "weapon", "pivot": [0, 2, 0], "cubes": parts[n2]},
        {"name": n3, "parent": "weapon", "pivot": [0, 2, 0], "cubes": parts[n3]},
    ]
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry." + wid, "texture_width": TEX,
                        "texture_height": TEX, "visible_bounds_width": spec["bounds"],
                        "visible_bounds_height": spec["bounds"], "visible_bounds_offset": [0, 1, 0]},
        "bones": bones}]}
    atlas = paint(spec)
    glowm = paint(spec, glowmask=True)
    with open(os.path.join(ASSETS, f"geckolib/models/item/{wid}.geo.json"), "w") as f:
        json.dump(geo, f, indent=2)
    atlas.save(os.path.join(ASSETS, f"textures/item/{wid}_geo.png"))
    glowm.save(os.path.join(ASSETS, f"textures/item/{wid}_geo_glowmask.png"))
    with open(os.path.join(ASSETS, f"geckolib/animations/item/{wid}.animation.json"), "w") as f:
        json.dump(animations(spec), f, indent=2)
    render_preview(geo, atlas).save(os.path.join(ROOT, f"renders/{wid}_geo_preview.png"))
    print(wid, "bones:", [(b["name"], len(b["cubes"])) for b in bones])


if __name__ == "__main__":
    ids = sys.argv[1:] or list(SPECS.keys())
    for wid in ids:
        build_one(wid)
