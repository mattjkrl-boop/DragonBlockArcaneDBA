#!/usr/bin/env python3
"""
Phase 1 polearm builder for Dragon Block Arcane: Evil Spear, Devil Trident, Power Pole,
Sickle of Sorrow, Ox King's Ax. Long shaft plus a head, same asset conventions as the
swords (tools/build_swords.py is imported for the atlas painter and cube helpers):
- geckolib/models/item/<id>.geo.json (bones: weapon > shaft, head, head_glow)
- textures/item/<id>_geo.png and <id>_geo_glowmask.png (256)
- geckolib/animations/item/<id>.animation.json
- renders/<id>_geo_preview.png

Run from the repo root: python3 tools/build_polearms.py [weapon_id ...]
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from build_swords import cube, paint, REGIONS, TEX  # noqa: E402
from PIL import Image, ImageDraw  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/dragonblockarcanedba")

SPECS = {
    "evil_spear": {
        "head": "spear", "swing_len": 0.45, "charge_tilt": -6, "pommel": "spike",
        "pal": {"blade_light": (74, 76, 88), "blade_dark": (44, 46, 56),
                "grip_main": (76, 51, 34), "grip_second": (56, 37, 24),
                "metal": (42, 42, 50), "glow": (196, 10, 64),
                "metal_dark": (24, 24, 30), "accent": (196, 10, 64)},
    },
    "devil_trident": {
        "head": "trident", "swing_len": 0.48, "charge_tilt": -6, "pommel": "spike",
        "pal": {"blade_light": (54, 52, 62), "blade_dark": (30, 28, 36),
                "grip_main": (142, 22, 38), "grip_second": (24, 20, 28),
                "metal": (32, 28, 36), "glow": (255, 34, 68),
                "metal_dark": (16, 14, 20), "accent": (255, 34, 68)},
    },
    "power_pole": {
        "head": "pole", "swing_len": 0.4, "charge_tilt": -5, "pommel": "ball",
        "pal": {"blade_light": (178, 34, 40), "blade_dark": (148, 26, 32),
                "grip_main": (178, 34, 40), "grip_second": (150, 26, 32),
                "metal": (208, 164, 60), "glow": (232, 192, 92),
                "metal_dark": (150, 116, 40), "accent": (232, 192, 92)},
    },
    "sickle_of_sorrow": {
        "head": "sickle", "swing_len": 0.5, "charge_tilt": -5, "pommel": "spike",
        "pal": {"blade_light": (68, 72, 84), "blade_dark": (40, 44, 54),
                "grip_main": (54, 45, 41), "grip_second": (38, 32, 28),
                "metal": (36, 34, 38), "glow": (186, 22, 28),
                "metal_dark": (20, 19, 22), "accent": (186, 22, 28)},
    },
    "ox_kings_ax": {
        "head": "axe", "swing_len": 0.55, "charge_tilt": -4, "pommel": "spike",
        "pal": {"blade_light": (170, 178, 190), "blade_dark": (104, 110, 122),
                "grip_main": (112, 78, 45), "grip_second": (84, 57, 32),
                "metal": (98, 102, 112), "glow": (255, 106, 43),
                "metal_dark": (58, 60, 66), "accent": (255, 150, 60)},
    },
}

SHAFT_TOP = 12.5
SHAFT_BOT = -13.0


def shaft_cubes(spec):
    cubes = []
    # grip zone: wrapped section where the hands sit
    cubes.append(cube([-0.62, -8.5, -0.62], [1.24, 11.0, 1.24], "grip_second"))
    y = -8.0
    while y < 1.8:
        cubes.append(cube([-0.74, y, -0.74], [1.48, 0.8, 1.48], "grip_main"))
        y += 1.62
    # bare shaft above and below the grip, slight taper via two steps
    cubes.append(cube([-0.55, 2.5, -0.55], [1.1, 10.0, 1.1], "grip_main"))
    cubes.append(cube([-0.6, -13.0, -0.6], [1.2, 4.5, 1.2], "grip_main"))
    # iron rings
    for ry in (3.2, 7.5, 11.2):
        cubes.append(cube([-0.68, ry, -0.68], [1.36, 0.55, 1.36], "metal"))
    # butt end
    if spec["pommel"] == "spike":
        cubes.append(cube([-0.7, -13.9, -0.7], [1.4, 0.9, 1.4], "metal"))
        cubes.append(cube([-0.34, -15.2, -0.34], [0.68, 1.3, 0.68], "metal_dark"))
    else:
        cubes.append(cube([-0.78, -13.8, -0.78], [1.56, 1.1, 1.56], "metal"))
    return cubes


def head_cubes(spec):
    """Returns (head cubes, glow cubes). Head base sits at SHAFT_TOP."""
    kind = spec["head"]
    head, glow = [], []
    t = SHAFT_TOP
    if kind == "spear":
        head.append(cube([-0.85, t, -0.85], [1.7, 1.6, 1.7], "metal"))
        head.append(cube([-0.5, t + 1.4, -0.5], [1.0, 0.8, 1.0], "accent"))
        widths = [1.5, 2.25, 1.95, 1.15, 0.42]
        y = t + 1.6
        for i, w in enumerate(widths):
            th = 0.5 * (1 - 0.16 * i)
            head.append(cube([-w / 2, y, -th / 2], [w, 1.55, th], "blade_light", "blade_dark"))
            glow.append(cube([-w / 2 - 0.08, y, -th / 2 - 0.02], [0.24, 1.55, th + 0.04], "glow"))
            glow.append(cube([w / 2 - 0.16, y, -th / 2 - 0.02], [0.24, 1.55, th + 0.04], "glow"))
            y += 1.55
        head.append(cube([-0.1, y, -0.07], [0.2, 1.1, 0.14], "blade_light", "blade_dark"))
    elif kind == "trident":
        head.append(cube([-3.3, t, -0.6], [6.6, 0.9, 1.2], "metal"))
        head.append(cube([-2.5, t + 0.9, -0.45], [1.5, 0.8, 0.9], "metal"))
        head.append(cube([1.0, t + 0.9, -0.45], [1.5, 0.8, 0.9], "metal"))
        # center prong, longer
        for i, w in enumerate([1.15, 0.95, 0.62, 0.24]):
            y = t + 0.9 + i * 1.9
            head.append(cube([-w / 2, y, -0.21], [w, 1.9, 0.42], "blade_light", "blade_dark"))
            glow.append(cube([-w / 2 - 0.07, y, -0.23], [0.2, 1.9, 0.46], "glow"))
        # side prongs
        for sx in (-2.6, 2.6):
            for i, w in enumerate([0.85, 0.6, 0.22]):
                y = t + 1.7 + i * 1.7
                head.append(cube([sx - w / 2, y, -0.18], [w, 1.7, 0.36], "blade_light", "blade_dark"))
                glow.append(cube([sx - w / 2 - 0.06, y, -0.2], [0.18, 1.7, 0.4], "glow"))
    elif kind == "pole":
        # gold caps only; the shaft itself is the weapon
        head.append(cube([-0.85, t - 0.4, -0.85], [1.7, 1.5, 1.7], "metal"))
        head.append(cube([-0.7, t + 1.1, -0.7], [1.4, 0.8, 1.4], "metal"))
    elif kind == "sickle":
        head.append(cube([-0.85, t, -0.85], [1.7, 1.8, 1.7], "metal"))
        # Curved blade on an arc whose start sits in the socket: center is offset +x,
        # so angle 180 lands on the socket top and the blade rises, sweeps forward,
        # and the tip starts to fall. Glow rides the concave (inner) edge.
        import math
        cx, cy, r = 5.4, t + 0.8, 5.4
        for i in range(6):
            a = math.radians(180 - i * 24)
            bx = cx + r * math.cos(a)
            by = cy + r * math.sin(a)
            w = 2.6 - 0.3 * i
            rot = [0, 0, math.degrees(a) - 90]
            head.append(cube([bx - 1.7, by - w / 2, -0.2], [3.4, w, 0.4], "blade_light", "blade_dark",
                             rotation=rot, pivot=[bx, by, 0]))
            gx = bx - math.cos(a) * (w / 2 + 0.12)
            gy = by - math.sin(a) * (w / 2 + 0.12)
            glow.append(cube([gx - 1.7, gy - 0.15, -0.22], [3.4, 0.3, 0.44], "glow",
                             rotation=rot, pivot=[gx, gy, 0]))
    elif kind == "axe":
        head.append(cube([-0.95, t, -0.95], [1.9, 3.4, 1.9], "metal"))
        # fan of the axe head: stacked boxes reaching +x, edge at the far side
        rows = [(13.2, 3.1, 0.95), (14.8, 4.3, 0.8), (16.4, 4.9, 0.62), (18.0, 4.4, 0.45), (19.6, 3.2, 0.32)]
        for (ry, ext, th) in rows:
            head.append(cube([0.4, ry, -th / 2], [ext, 1.7, th], "blade_light", "blade_dark"))
            glow.append(cube([0.4 + ext - 0.05, ry, -th / 2 - 0.02], [0.26, 1.7, th + 0.04], "glow"))
        # top spike
        head.append(cube([-0.42, t + 3.4, -0.42], [0.84, 1.8, 0.84], "metal"))
        head.append(cube([-0.2, t + 5.2, -0.2], [0.4, 1.5, 0.4], "blade_light", "blade_dark"))
    return head, glow


def build_geo(spec):
    head, glow = head_cubes(spec)
    bones = [
        {"name": "weapon", "pivot": [0, 2, 0], "cubes": []},
        {"name": "shaft", "parent": "weapon", "pivot": [0, 2, 0], "cubes": shaft_cubes(spec)},
        {"name": "head", "parent": "weapon", "pivot": [0, 2, 0], "cubes": head},
        {"name": "head_glow", "parent": "weapon", "pivot": [0, 2, 0], "cubes": glow},
    ]
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.SPEC_ID", "texture_width": TEX,
                        "texture_height": TEX, "visible_bounds_width": 6,
                        "visible_bounds_height": 6, "visible_bounds_offset": [0, 1, 0]},
        "bones": bones}]}


def animations(spec):
    sl = spec["swing_len"]
    tilt = spec["charge_tilt"]
    return {"format_version": "1.8.0", "animations": {
        "idle": {"loop": True, "animation_length": 3.0, "bones": {
            "weapon": {"rotation": {"0.0": [0, 0, 0], "1.5": [0, 0, 1.5], "3.0": [0, 0, 0]},
                       "position": {"0.0": [0, 0, 0], "1.5": [0, 0.25, 0], "3.0": [0, 0, 0]}}}},
        "swing": {"loop": True, "animation_length": sl, "bones": {
            "weapon": {"rotation": {"0.0": [0, 0, 0], "0.1": [0, 0, 26], "0.25": [0, 0, -18], str(sl): [0, 0, 0]},
                       "scale": {"0.0": [1, 1, 1], "0.1": [1.04, 1.04, 1.04], str(sl): [1, 1, 1]}}}},
        "charge": {"loop": True, "animation_length": 0.8, "bones": {
            "weapon": {"rotation": {"0.0": [0, 0, tilt], "0.2": [0.6, 0, tilt + 1], "0.4": [-0.6, 0, tilt - 1], "0.6": [0.6, 0, tilt + 1], "0.8": [0, 0, tilt]},
                       "position": {"0.0": [0, 0.3, 0], "0.8": [0, 0.3, 0]}},
            "head": {"rotation": {"0.0": [0, 0, 0], "0.2": [0, 0, 0.5], "0.4": [0, 0, -0.5], "0.6": [0, 0, 0.5], "0.8": [0, 0, 0]}}}},
    }}


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
    geo = build_geo(spec)
    geo["minecraft:geometry"][0]["description"]["identifier"] = "geometry." + wid
    atlas = paint(spec)
    glow = paint(spec, glowmask=True)
    with open(os.path.join(ASSETS, f"geckolib/models/item/{wid}.geo.json"), "w") as f:
        json.dump(geo, f, indent=2)
    atlas.save(os.path.join(ASSETS, f"textures/item/{wid}_geo.png"))
    glow.save(os.path.join(ASSETS, f"textures/item/{wid}_geo_glowmask.png"))
    with open(os.path.join(ASSETS, f"geckolib/animations/item/{wid}.animation.json"), "w") as f:
        json.dump(animations(spec), f, indent=2)
    render_preview(geo, atlas).save(os.path.join(ROOT, f"renders/{wid}_geo_preview.png"))
    bones = geo["minecraft:geometry"][0]["bones"]
    print(wid, "bones:", [(b["name"], len(b["cubes"])) for b in bones])


if __name__ == "__main__":
    ids = sys.argv[1:] or list(SPECS.keys())
    for wid in ids:
        build_one(wid)
