#!/usr/bin/env python3
"""
Phase 1 sword builder for Dragon Block Arcane. Generalizes tools/build_katana.py:
same 5-bone structure (weapon > grip, guard, blade, blade_glow), per-weapon specs for
silhouette, palette, guard style, and animation timing. For each spec it writes:
- geckolib/models/item/<id>.geo.json
- textures/item/<id>_geo.png and <id>_geo_glowmask.png (128x128)
- geckolib/animations/item/<id>.animation.json
- renders/<id>_geo_preview.png

Run from the repo root: python3 tools/build_swords.py [weapon_id ...]
"""
import json
import os
import sys
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/dragonblockarcanedba")

REGIONS = {
    "blade_light": (0, 0, 32, 32),
    "blade_dark": (32, 0, 32, 32),
    "grip_main": (0, 32, 32, 32),
    "grip_second": (32, 32, 32, 32),
    "metal": (64, 0, 32, 32),
    "glow": (64, 32, 32, 32),
    "metal_dark": (96, 0, 32, 32),
    "accent": (96, 32, 32, 32),
}


def face_uv(region):
    x, y, w, h = REGIONS[region]
    return {"uv": [x, y], "uv_size": [w, h]}


def cube(origin, size, main, side=None, rotation=None, pivot=None):
    side = side or main
    c = {"origin": origin, "size": size, "uv": {
        "north": face_uv(main), "south": face_uv(main),
        "up": face_uv(side), "down": face_uv(side),
        "east": face_uv(side), "west": face_uv(side)}}
    if rotation is not None:
        c["rotation"] = rotation
        c["pivot"] = pivot
    return c


SPECS = {
    "z_sword": {
        "grip_len": 11.0, "blade_w": 4.6, "blade_len": 20.0, "thick": 0.7,
        "curve": 0.0, "guard": "double_bar", "pommel": "ball",
        "swing_len": 0.45, "charge_tilt": -5,
        "pal": {"blade_light": (196, 214, 226), "blade_dark": (110, 128, 144),
                "grip_main": (106, 72, 44), "grip_second": (84, 56, 34),
                "metal": (206, 164, 60), "glow": (191, 230, 255),
                "metal_dark": (150, 116, 40), "accent": (206, 164, 60)},
    },
    "brave_sword": {
        "grip_len": 9.0, "blade_w": 2.4, "blade_len": 21.0, "thick": 0.55,
        "curve": 0.06, "guard": "wings", "pommel": "gem",
        "swing_len": 0.35, "charge_tilt": -7,
        "pal": {"blade_light": (236, 240, 246), "blade_dark": (148, 156, 168),
                "grip_main": (148, 26, 34), "grip_second": (110, 18, 26),
                "metal": (222, 178, 72), "glow": (255, 213, 74),
                "metal_dark": (166, 128, 46), "accent": (216, 30, 44)},
    },
    "grand_sword": {
        "grip_len": 10.0, "blade_w": 5.4, "blade_len": 22.0, "thick": 0.9,
        "curve": 0.0, "guard": "plate", "pommel": "spike", "ember": True,
        "swing_len": 0.5, "charge_tilt": -4,
        "pal": {"blade_light": (52, 50, 56), "blade_dark": (30, 29, 34),
                "grip_main": (22, 20, 24), "grip_second": (40, 37, 42),
                "metal": (128, 102, 66), "glow": (255, 106, 43),
                "metal_dark": (70, 62, 58), "accent": (255, 150, 60)},
    },
    "dabura_sword": {
        "grip_len": 9.5, "blade_w": 3.6, "blade_len": 20.0, "thick": 0.75,
        "curve": 0.0, "guard": "fangs", "pommel": "spike",
        "swing_len": 0.42, "charge_tilt": -5,
        "pal": {"blade_light": (96, 100, 114), "blade_dark": (52, 55, 66),
                "grip_main": (104, 32, 28), "grip_second": (66, 20, 18),
                "metal": (74, 76, 88), "glow": (198, 202, 210),
                "metal_dark": (42, 44, 54), "accent": (178, 34, 44)},
    },
    "cursed_blade": {
        "grip_len": 9.0, "blade_w": 2.4, "blade_len": 21.0, "thick": 0.55,
        "curve": 0.08, "guard": "wings", "pommel": "gem", "dual_edge": True,
        "swing_len": 0.36, "charge_tilt": -7,
        "pal": {"blade_light": (34, 27, 50), "blade_dark": (18, 14, 30),
                "grip_main": (56, 22, 84), "grip_second": (36, 13, 55),
                "metal": (26, 22, 38), "glow": (150, 40, 255),
                "metal_dark": (13, 11, 21), "accent": (176, 80, 255)},
    },
    "hollows_edge": {
        "grip_len": 9.0, "blade_w": 2.3, "blade_len": 21.5, "thick": 0.5,
        "curve": 0.13, "guard": "double_bar", "pommel": "orb",
        "swing_len": 0.32, "charge_tilt": -8,
        "pal": {"blade_light": (198, 226, 221), "blade_dark": (122, 163, 156),
                "grip_main": (40, 54, 58), "grip_second": (25, 35, 39),
                "metal": (92, 112, 114), "glow": (0, 255, 204),
                "metal_dark": (54, 68, 70), "accent": (0, 255, 204)},
    },
    "azure_dragon_sword": {
        "grip_len": 9.5, "blade_w": 2.2, "blade_len": 21.5, "thick": 0.55,
        "curve": 0.16, "guard": "dragon_wings", "pommel": "orb", "scale_grip": True,
        "swing_len": 0.3, "charge_tilt": -8,
        "pal": {"blade_light": (64, 140, 220), "blade_dark": (30, 84, 150),
                "grip_main": (24, 110, 116), "grip_second": (16, 78, 84),
                "metal": (28, 96, 104), "glow": (120, 225, 255),
                "metal_dark": (18, 62, 70), "accent": (80, 190, 255)},
    },
}


def build_geo(spec):
    gl = spec["grip_len"]
    bw = spec["blade_w"]
    bl = spec["blade_len"]
    th = spec["thick"]
    grip_top = 2.0
    grip_bot = grip_top - gl

    grip = [cube([-0.95, grip_bot + 0.8, -0.95], [1.9, gl - 0.8, 1.9], "grip_second")]
    y = grip_bot + 1.4
    while y < grip_top - 0.6:
        grip.append(cube([-1.08, y, -1.08], [2.16, 1.0, 2.16], "grip_main"))
        y += 1.9
    if spec["pommel"] == "ball":
        grip.append(cube([-1.3, grip_bot - 0.6, -1.3], [2.6, 1.6, 2.6], "metal"))
    elif spec["pommel"] == "gem":
        grip.append(cube([-1.15, grip_bot - 0.5, -1.15], [2.3, 1.3, 2.3], "metal"))
        grip.append(cube([-0.7, grip_bot - 0.9, -0.7], [1.4, 0.7, 1.4], "accent"))
    elif spec["pommel"] == "spike":
        grip.append(cube([-1.2, grip_bot - 0.4, -1.2], [2.4, 1.2, 2.4], "metal_dark"))
        grip.append(cube([-0.55, grip_bot - 1.7, -0.55], [1.1, 1.5, 1.1], "metal_dark",
                         rotation=[0, 0, 45], pivot=[0, grip_bot - 0.4, 0]))
    elif spec["pommel"] == "orb":
        grip.append(cube([-1.0, grip_bot - 0.5, -1.0], [2.0, 1.2, 2.0], "metal_dark"))
        grip.append(cube([-0.8, grip_bot - 1.6, -0.8], [1.6, 1.3, 1.6], "accent"))

    guard = []
    gw = bw + 3.4
    if spec["guard"] == "double_bar":
        guard.append(cube([-gw / 2, 1.55, -1.15], [gw, 0.85, 2.3], "metal"))
        guard.append(cube([-gw / 2 - 0.3, 2.4, -1.3], [gw + 0.6, 0.85, 2.6], "metal"))
    elif spec["guard"] in ("wings", "dragon_wings"):
        # Stepped voxel wings: unrotated boxes that overlap, so they always read as one
        # connected wing. Rotated boxes left visible gaps at some angles (found in 3D).
        guard.append(cube([-1.5, 1.6, -1.5], [3.0, 0.9, 3.0], "metal"))
        guard.append(cube([-5.2, 1.9, -0.55], [4.4, 1.5, 1.1], "metal"))
        guard.append(cube([0.8, 1.9, -0.55], [4.4, 1.5, 1.1], "metal"))
        guard.append(cube([-6.3, 3.0, -0.45], [3.2, 1.15, 0.9], "metal"))
        guard.append(cube([3.1, 3.0, -0.45], [3.2, 1.15, 0.9], "metal"))
        guard.append(cube([-7.0, 3.95, -0.35], [2.1, 0.9, 0.7], "metal"))
        guard.append(cube([4.9, 3.95, -0.35], [2.1, 0.9, 0.7], "metal"))
    elif spec["guard"] == "fangs":
        # crossguard bars whose ends hook toward the blade, Dabura's claw guard
        guard.append(cube([-1.5, 1.6, -1.5], [3.0, 0.9, 3.0], "metal"))
        guard.append(cube([-4.6, 1.75, -0.6], [3.4, 1.2, 1.2], "metal"))
        guard.append(cube([1.2, 1.75, -0.6], [3.4, 1.2, 1.2], "metal"))
        guard.append(cube([-5.3, 2.6, -0.5], [1.6, 1.6, 1.0], "metal"))
        guard.append(cube([3.7, 2.6, -0.5], [1.6, 1.6, 1.0], "metal"))
        guard.append(cube([-5.0, 4.0, -0.4], [1.0, 1.3, 0.8], "metal_dark"))
        guard.append(cube([4.0, 4.0, -0.4], [1.0, 1.3, 0.8], "metal_dark"))
    elif spec["guard"] == "plate":
        guard.append(cube([-gw / 2, 1.3, -1.7], [gw, 2.2, 3.4], "metal"))
        guard.append(cube([-gw / 2 + 0.5, 2.1, -1.95], [1.1, 1.1, 0.4], "metal_dark"))
        guard.append(cube([gw / 2 - 1.6, 2.1, -1.95], [1.1, 1.1, 0.4], "metal_dark"))
    guard.append(cube([-bw / 2 - 0.25, 3.3, -0.75], [bw + 0.5, 0.8, 1.5], "metal_dark"))

    blade = []
    glow = []
    segs = 4
    seg_h = bl / segs
    for i in range(segs):
        y0 = 4.1 + i * seg_h
        w = bw * (1.0 - 0.14 * i)
        xoff = spec["curve"] * (i * i) * 0.22
        tip = i == segs - 1
        hh = seg_h * (0.72 if tip else 1.0)
        rot = [0, 0, -16] if tip else None
        blade.append(cube([xoff - w / 2, y0, -th / 2], [w, hh, th], "blade_light", "blade_dark",
                          rotation=rot, pivot=[xoff, y0, 0] if tip else None))
        glow.append(cube([xoff - w / 2 - 0.26, y0, -th / 2 - 0.03], [0.5, hh, th + 0.06], "glow",
                         rotation=rot, pivot=[xoff, y0, 0] if tip else None))
        if spec.get("dual_edge"):
            glow.append(cube([xoff + w / 2 - 0.24, y0, -th / 2 - 0.03], [0.5, hh, th + 0.06], "glow",
                             rotation=rot, pivot=[xoff, y0, 0] if tip else None))
    if spec.get("ember"):
        # ember crack cubes sitting proud of the blade faces
        for (ex, ey) in ((-1.6, 8.0), (0.4, 12.5), (-0.8, 17.0), (1.2, 20.0)):
            blade.append(cube([ex, ey, -th / 2 - 0.12], [0.55, 2.2, 0.16], "glow",
                              rotation=[0, 0, 24], pivot=[ex + 0.27, ey + 1.1, 0]))

    bones = [
        {"name": "weapon", "pivot": [0, 2, 0], "cubes": []},
        {"name": "grip", "parent": "weapon", "pivot": [0, 2, 0], "cubes": grip},
        {"name": "guard", "parent": "weapon", "pivot": [0, 2, 0], "cubes": guard},
        {"name": "blade", "parent": "weapon", "pivot": [0, 2, 0], "cubes": blade},
        {"name": "blade_glow", "parent": "weapon", "pivot": [0, 2, 0], "cubes": glow},
    ]
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry." + "SPEC_ID", "texture_width": 128,
                        "texture_height": 128, "visible_bounds_width": 5,
                        "visible_bounds_height": 5, "visible_bounds_offset": [0, 1.5, 0]},
        "bones": bones}]}


def paint(spec, glowmask=False):
    pal = spec["pal"]
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0 if glowmask else 255))
    d = ImageDraw.Draw(img)
    for name, (x, y, w, h) in REGIONS.items():
        base = pal[name]
        if glowmask:
            if name == "glow":
                d.rectangle([x, y, x + w - 1, y + h - 1], fill=base + (255,))
            elif name == "accent" and spec["pommel"] in ("gem", "orb"):
                d.rectangle([x, y, x + w - 1, y + h - 1], fill=base + (255,))
            continue
        # vertical gradient per region
        for i in range(h):
            t = i / h
            c = tuple(max(0, min(255, int(v * (1.08 - 0.22 * t)))) for v in base) + (255,)
            d.line([(x, y + i), (x + w - 1, y + i)], fill=c)
        if name == "grip_main" and spec.get("scale_grip"):
            for i in range(0, w, 7):
                for j in range(0, h, 7):
                    d.rectangle([x + i, y + j, x + i + 3, y + j + 3],
                                outline=tuple(max(0, v - 40) for v in base) + (255,))
        if name == "blade_light":
            d.line([(x + 5, y), (x + 5, y + h - 1)], fill=(255, 255, 255, 160))
    return img


def animations(spec):
    sl = spec["swing_len"]
    tilt = spec["charge_tilt"]
    return {"format_version": "1.8.0", "animations": {
        "idle": {"loop": True, "animation_length": 3.0, "bones": {
            "weapon": {"rotation": {"0.0": [0, 0, 0], "1.5": [0, 0, 1.5], "3.0": [0, 0, 0]},
                       "position": {"0.0": [0, 0, 0], "1.5": [0, 0.25, 0], "3.0": [0, 0, 0]}},
            "blade_glow": {"scale": {"0.0": [1, 1, 1], "1.5": [1.06, 1, 1.06], "3.0": [1, 1, 1]}}}},
        "swing": {"loop": True, "animation_length": sl, "bones": {
            "weapon": {"rotation": {"0.0": [0, 0, 0], "0.1": [0, 0, 26], "0.25": [0, 0, -18], str(sl): [0, 0, 0]},
                       "scale": {"0.0": [1, 1, 1], "0.1": [1.04, 1.04, 1.04], str(sl): [1, 1, 1]}},
            "blade_glow": {"scale": {"0.0": [1, 1, 1], "0.1": [1.25, 1, 1.25], str(sl): [1, 1, 1]}}}},
        "charge": {"loop": True, "animation_length": 0.8, "bones": {
            "weapon": {"rotation": {"0.0": [0, 0, tilt], "0.2": [0.6, 0, tilt + 1], "0.4": [-0.6, 0, tilt - 1], "0.6": [0.6, 0, tilt + 1], "0.8": [0, 0, tilt]},
                       "position": {"0.0": [0, 0.3, 0], "0.8": [0, 0.3, 0]}},
            "blade_glow": {"scale": {"0.0": [1, 1, 1], "0.4": [1.35, 1, 1.35], "0.8": [1, 1, 1]}},
            "blade": {"rotation": {"0.0": [0, 0, 0], "0.2": [0, 0, 0.5], "0.4": [0, 0, -0.5], "0.6": [0, 0, 0.5], "0.8": [0, 0, 0]}}}},
    }}


def render_preview(geo, atlas):
    cubes = [c for b in geo["minecraft:geometry"][0]["bones"] for c in b["cubes"]]
    min_x = min(c["origin"][0] for c in cubes)
    max_x = max(c["origin"][0] + c["size"][0] for c in cubes)
    min_y = min(c["origin"][1] for c in cubes)
    max_y = max(c["origin"][1] + c["size"][1] for c in cubes)
    scale, pad = 11, 10
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
