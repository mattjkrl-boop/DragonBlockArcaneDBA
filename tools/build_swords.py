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
import math
import os
import sys
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/dragonblockarcanedba")

TEX = 256
REGIONS = {
    "blade_light": (0, 0, 64, 64),
    "blade_dark": (64, 0, 64, 64),
    "grip_main": (0, 64, 64, 64),
    "grip_second": (64, 64, 64, 64),
    "metal": (128, 0, 64, 64),
    "glow": (128, 64, 64, 64),
    "metal_dark": (192, 0, 64, 64),
    "accent": (192, 64, 64, 64),
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
    "katana": {
        "grip_len": 9.5, "blade_w": 2.1, "blade_len": 20.8, "thick": 0.5,
        "curve": 0.17, "guard": "tsuba", "pommel": "ball",
        "swing_len": 0.35, "charge_tilt": -7,
        "pal": {"blade_light": (216, 228, 240), "blade_dark": (128, 142, 158),
                "grip_main": (18, 16, 22), "grip_second": (233, 235, 239),
                "metal": (188, 196, 206), "glow": (214, 242, 255),
                "metal_dark": (106, 114, 126), "accent": (188, 196, 206)},
    },
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
        "grip_len": 9.2, "blade_w": 2.85, "blade_len": 21.0, "thick": 0.66,
        "curve": 0.0, "guard": "double_bar", "pommel": "gem", "serrated": True,
        "swing_len": 0.5, "charge_tilt": -4,
        "pal": {"blade_light": (186, 196, 208), "blade_dark": (118, 128, 142),
                "grip_main": (32, 26, 30), "grip_second": (20, 16, 20),
                "metal": (220, 176, 58), "glow": (214, 220, 228),
                "metal_dark": (150, 112, 36), "accent": (236, 200, 84)},
    },
    "dabura_sword": {
        "grip_len": 8.4, "blade_w": 2.15, "blade_len": 20.0, "thick": 0.52,
        "curve": 0.0, "arc": True, "guard": "double_bar", "pommel": "ball",
        "swing_len": 0.42, "charge_tilt": -5,
        "pal": {"blade_light": (228, 232, 238), "blade_dark": (150, 156, 166),
                "grip_main": (28, 24, 28), "grip_second": (16, 14, 18),
                "metal": (214, 168, 48), "glow": (220, 224, 230),
                "metal_dark": (140, 104, 32), "accent": (214, 168, 48)},
    },
    "cursed_blade": {
        # style "cursed" skips the shared sword silhouette. The icon is a gold
        # blade with three pairs of barbs, a short navy hilt, and a purple gem.
        "style": "cursed",
        "grip_len": 9.0, "blade_w": 1.85, "blade_len": 18.2, "thick": 0.62,
        "curve": 0.0, "guard": "cross", "pommel": "gem",
        "swing_len": 0.36, "charge_tilt": -7,
        "pal": {"blade_light": (236, 184, 36), "blade_dark": (186, 132, 24),
                "grip_main": (32, 36, 64), "grip_second": (18, 20, 40),
                "metal": (228, 176, 40), "glow": (168, 86, 226),
                "metal_dark": (28, 32, 58), "accent": (176, 96, 232)},
    },
    "hollows_edge": {
        "grip_len": 8.6, "blade_w": 2.7, "blade_len": 20.5, "thick": 0.7,
        "curve": 0.0, "guard": "double_bar", "pommel": "orb", "crystal": True,
        "ricasso": "metal",
        "swing_len": 0.32, "charge_tilt": -8,
        "pal": {"blade_light": (198, 96, 236), "blade_dark": (118, 36, 176),
                "grip_main": (176, 36, 44), "grip_second": (120, 22, 30),
                "metal": (168, 32, 42), "glow": (244, 186, 255),
                "metal_dark": (148, 152, 162), "accent": (186, 190, 198)},
    },
    "saber": {
        "grip_len": 8.5, "blade_w": 1.9, "blade_len": 22.0, "thick": 0.5,
        "curve": 0.05, "guard": "double_bar", "pommel": "gem",
        "swing_len": 0.28, "charge_tilt": -9,
        "pal": {"blade_light": (226, 232, 240), "blade_dark": (140, 150, 164),
                "grip_main": (22, 19, 28), "grip_second": (52, 48, 60),
                "metal": (96, 102, 116), "glow": (102, 204, 255),
                "metal_dark": (40, 42, 52), "accent": (102, 204, 255)},
    },
    "dimensional_sword": {
        "guard": "wings", "pommel": "gem", "swing_len": 0.42, "charge_tilt": -7,
        "blade_len": 20.5, "blade_w": 2.6, "curve": 0.0,
        "grip_len": 9.0, "thick": 0.62,
        "pal": {"blade_light": (196, 42, 52), "blade_dark": (122, 22, 32),
                "grip_main": (206, 210, 218), "grip_second": (154, 160, 170),
                "metal": (124, 62, 184), "glow": (255, 86, 96),
                "metal_dark": (72, 34, 112), "accent": (204, 40, 52)},
    },
    "spirit_sword": {
        "guard": "bars", "pommel": "orb", "dual_edge": True, "swing_len": 0.4, "charge_tilt": -8,
        "blade_len": 21.5, "blade_w": 3.1, "curve": 0.0,
        "grip_len": 9.5, "thick": 0.6,
        "pal": {"blade_light": (198, 250, 255), "blade_dark": (116, 222, 242),
                "grip_main": (30, 38, 58), "grip_second": (18, 24, 36),
                "metal": (214, 244, 248), "glow": (125, 249, 255),
                "metal_dark": (24, 28, 40), "accent": (125, 249, 255)},
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


def arc_blade(spec):
    """Scimitar: overlapping segments along a shallow arc, each spun about its own center.

    A pure x-offset per segment opens gaps once the curve is strong enough to read
    as Dabura's sword. Rotation is about the segment center, not the weapon pivot.
    """
    blade, glow = [], []
    # a=0 is the hilt, tangent straight up. The blade bows toward +x.
    r = 24.0
    sweep = 0.85
    n = 8
    bw = spec["blade_w"]
    th = spec["thick"]
    base_y = 4.1
    length = (r * sweep) / n * 1.38
    for i in range(n):
        a = sweep * (i + 0.5) / n
        x = r * (1.0 - math.cos(a))
        y = base_y + r * math.sin(a)
        t = i / (n - 1)
        w = max(0.34, bw * (1.0 - 0.78 * t))
        th_i = max(0.22, th * (1.0 - 0.4 * t))
        # Negative Z rotation sends the local +Y axis toward +X, along the tangent.
        rot = [0, 0, -math.degrees(a)]
        blade.append(cube([x - w / 2, y - length / 2, -th_i / 2], [w, length, th_i],
                          "blade_light", "blade_dark", rotation=rot, pivot=[x, y, 0]))
        glow.append(cube([x + w / 2 - 0.16, y - length / 2, -th_i / 2 - 0.02],
                         [0.2, length, th_i + 0.04], "glow", rotation=rot, pivot=[x, y, 0]))
    a = sweep
    x = r * (1.0 - math.cos(a))
    y = base_y + r * math.sin(a)
    blade.append(cube([x - 0.12, y - 0.1, -0.1], [0.24, 1.15, 0.2], "blade_light", "blade_dark",
                      rotation=[0, 0, -math.degrees(a)], pivot=[x, y, 0]))
    return blade, glow


def cursed_cubes():
    """Gold barbed blade, short navy hilt, purple gem in a small crossguard.

    Matches the cursed blade icon. The old geo reused Brave Sword's upward
    wings and a dark purple blade, which read as a different weapon. The grip
    still crosses y=0 and every bone pivots at [0, 2, 0].
    """
    grip = [cube([-0.7, -6.3, -0.7], [1.4, 7.9, 1.4], "grip_second")]
    y = -5.5
    while y < 1.15:
        grip.append(cube([-0.86, y, -0.86], [1.72, 0.62, 1.72], "grip_main"))
        y += 1.45
    grip.append(cube([-0.78, -7.15, -0.78], [1.56, 0.95, 1.56], "metal_dark"))

    guard = [
        cube([-2.35, 1.55, -0.48], [4.7, 0.8, 0.96], "metal_dark"),
        cube([-1.0, 1.2, -0.72], [2.0, 1.55, 1.44], "metal_dark"),
        cube([-0.5, 1.5, -0.38], [1.0, 1.0, 0.76], "accent",
             rotation=[0, 0, 45], pivot=[0, 2.0, 0]),
    ]
    # Gem only. The gold blade is painted, not an emissive outline.
    glow = [cube([-0.32, 1.62, -0.2], [0.64, 0.64, 0.4], "glow",
                 rotation=[0, 0, 45], pivot=[0, 1.94, 0])]

    blade = []
    base, bl, segs = 3.2, 18.2, 8
    seg_h = bl / segs
    bw, th = 1.85, 0.62
    for i in range(segs):
        t = i / (segs - 1)
        w = max(0.28, bw * (1.0 - 0.72 * t))
        th_i = max(0.22, th * (1.0 - 0.4 * t))
        y0 = base + i * seg_h
        blade.append(cube([-w / 2, y0, -th_i / 2], [w, seg_h, th_i], "blade_light", "blade_dark"))
    blade.append(cube([-0.12, base + bl - 0.05, -0.1], [0.24, 1.15, 0.2], "blade_light", "blade_dark"))

    # Three barb pairs, stepped out and up so they stay connected without a
    # rotated box (rotated tips opened gaps on the other swords).
    for frac, reach in ((0.30, 2.55), (0.52, 2.25), (0.74, 1.85)):
        y = base + frac * bl
        steps = (
            (0.45, 0.0, reach * 0.42, 0.78, 0.5),
            (0.45 + reach * 0.36, 0.62, reach * 0.40, 0.58, 0.42),
            (0.45 + reach * 0.70, 1.25, reach * 0.36, 0.42, 0.34),
        )
        for x0, dy, w, h, z in steps:
            blade.append(cube([x0, y + dy, -z / 2], [w, h, z], "blade_light", "metal"))
            blade.append(cube([-(x0 + w), y + dy, -z / 2], [w, h, z], "blade_light", "metal"))
    return grip, guard, blade, glow


def build_geo(spec):
    if spec.get("style") == "cursed":
        grip, guard, blade, glow = cursed_cubes()
        bones = [
            {"name": "weapon", "pivot": [0, 2, 0], "cubes": []},
            {"name": "grip", "parent": "weapon", "pivot": [0, 2, 0], "cubes": grip},
            {"name": "guard", "parent": "weapon", "pivot": [0, 2, 0], "cubes": guard},
            {"name": "blade", "parent": "weapon", "pivot": [0, 2, 0], "cubes": blade},
            {"name": "blade_glow", "parent": "weapon", "pivot": [0, 2, 0], "cubes": glow},
        ]
        return {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": "geometry.SPEC_ID", "texture_width": TEX,
                            "texture_height": TEX, "visible_bounds_width": 5,
                            "visible_bounds_height": 5, "visible_bounds_offset": [0, 1.5, 0]},
            "bones": bones}]}

    gl = spec["grip_len"]
    bw = spec["blade_w"]
    bl = spec["blade_len"]
    th = spec["thick"]
    grip_top = 2.0
    grip_bot = grip_top - gl

    grip = [cube([-0.83, grip_bot + 0.8, -0.83], [1.66, gl - 0.8, 1.66], "grip_second")]
    y = grip_bot + 1.3
    while y < grip_top - 0.5:
        grip.append(cube([-0.98, y, -0.98], [1.96, 0.8, 1.96], "grip_main"))
        y += 1.62
    if spec["pommel"] == "ball":
        grip.append(cube([-1.1, grip_bot - 0.5, -1.1], [2.2, 1.4, 2.2], "metal"))
    elif spec["pommel"] == "gem":
        grip.append(cube([-1.0, grip_bot - 0.45, -1.0], [2.0, 1.15, 2.0], "metal"))
        grip.append(cube([-0.6, grip_bot - 0.85, -0.6], [1.2, 0.65, 1.2], "accent"))
    elif spec["pommel"] == "spike":
        grip.append(cube([-1.05, grip_bot - 0.35, -1.05], [2.1, 1.05, 2.1], "metal_dark"))
        grip.append(cube([-0.5, grip_bot - 1.55, -0.5], [1.0, 1.35, 1.0], "metal_dark",
                         rotation=[0, 0, 45], pivot=[0, grip_bot - 0.35, 0]))
    elif spec["pommel"] == "orb":
        grip.append(cube([-0.9, grip_bot - 0.45, -0.9], [1.8, 1.05, 1.8], "metal_dark"))
        grip.append(cube([-0.68, grip_bot - 1.45, -0.68], [1.36, 1.15, 1.36], "accent"))

    guard = []
    gw = bw + 3.4
    if spec["guard"] == "tsuba":
        guard.append(cube([-3.1, 1.75, -2.2], [6.2, 0.5, 4.4], "metal"))
        guard.append(cube([-2.2, 1.75, -3.1], [4.4, 0.5, 6.2], "metal"))
        guard.append(cube([-1.1, 2.25, -1.1], [2.2, 1.35, 2.2], "metal"))
    elif spec["guard"] == "double_bar":
        guard.append(cube([-gw / 2, 1.6, -1.05], [gw, 0.62, 2.1], "metal"))
        guard.append(cube([-gw / 2 - 0.3, 2.22, -1.2], [gw + 0.6, 0.62, 2.4], "metal"))
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
    elif spec["guard"] == "bars":
        # One straight crossguard. "bars" used to fall through and leave only
        # the ricasso, so Spirit Sword rendered with no guard at all.
        guard.append(cube([-1.15, 1.35, -0.85], [2.3, 1.7, 1.7], "metal"))
        guard.append(cube([-gw / 2, 1.85, -0.55], [gw, 0.85, 1.1], "metal"))
    if spec["guard"] != "tsuba":
        guard.append(cube([-bw / 2 - 0.22, 3.32, -0.68], [bw + 0.44, 0.65, 1.36],
                          spec.get("ricasso", "metal_dark")))

    if spec.get("arc"):
        blade, glow = arc_blade(spec)
        bones = [
            {"name": "weapon", "pivot": [0, 2, 0], "cubes": []},
            {"name": "grip", "parent": "weapon", "pivot": [0, 2, 0], "cubes": grip},
            {"name": "guard", "parent": "weapon", "pivot": [0, 2, 0], "cubes": guard},
            {"name": "blade", "parent": "weapon", "pivot": [0, 2, 0], "cubes": blade},
            {"name": "blade_glow", "parent": "weapon", "pivot": [0, 2, 0], "cubes": glow},
        ]
        return {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": "geometry.SPEC_ID", "texture_width": TEX,
                            "texture_height": TEX, "visible_bounds_width": 5,
                            "visible_bounds_height": 5, "visible_bounds_offset": [0, 1.5, 0]},
            "bones": bones}]}

    blade = []
    glow = []
    segs = 8
    seg_h = bl / segs
    for i in range(segs):
        y0 = 4.1 + i * seg_h
        # Profile: gentle taper, then the last three segments converge to a real point.
        # Width shrinks hard and the center shifts toward the spine (+x), so the cutting
        # edge line runs straight into the point while the spine drops to meet it, and
        # the final piece is thin in both width and thickness. No flat ends.
        # No rotated tip pieces: rotations about the base pivot opened gaps. The point
        # is pure convergence - full-height segments, hard width and thickness taper,
        # center shifting toward the spine, ending in one micro segment.
        if i < segs - 3:
            w = bw * (1.0 - 0.05 * i)
            shift = 0.0
            th_i = th
            hh = seg_h
        elif i == segs - 3:
            w = bw * 0.74
            shift = (bw - w) * 0.3
            th_i = th * 0.94
            hh = seg_h
        elif i == segs - 2:
            w = bw * 0.48
            shift = (bw - w) * 0.35
            th_i = th * 0.8
            hh = seg_h
        else:
            w = bw * 0.2
            shift = (bw - w) * 0.4
            th_i = th * 0.55
            hh = seg_h * 0.92
        xoff = spec["curve"] * (i / (segs - 1)) ** 2 * 2.2 + shift
        rot = None
        piv = None
        blade.append(cube([xoff - w / 2, y0, -th_i / 2], [w, hh, th_i], "blade_light", "blade_dark",
                          rotation=rot, pivot=piv))
        # edge bevel facets on both faces: thin angled boxes hugging the faces near
        # the cutting edge, so the blade reads faceted like real steel, not a slab
        bx = xoff - w / 2 + w * 0.15
        zc = th_i / 2 - 0.13
        if zc > 0.02:
            blade.append(cube([bx - w * 0.17, y0, zc - 0.06], [w * 0.34, hh, 0.12], "blade_dark",
                              rotation=[0, 14, 0], pivot=[bx, y0, zc]))
            blade.append(cube([bx - w * 0.17, y0, -zc - 0.06], [w * 0.34, hh, 0.12], "blade_dark",
                              rotation=[0, -14, 0], pivot=[bx, y0, -zc]))
        gw_ = max(0.12, 0.3 * (w / bw))
        glow.append(cube([xoff - w / 2 - 0.1, y0, -th_i / 2 - 0.02], [gw_, hh, th_i + 0.04], "glow",
                         rotation=rot, pivot=piv))
        if spec.get("dual_edge"):
            glow.append(cube([xoff + w / 2 - 0.2, y0, -th_i / 2 - 0.02], [gw_, hh, th_i + 0.04], "glow",
                             rotation=rot, pivot=piv))
        if spec.get("serrated") and i % 2 == 0 and i < segs - 2:
            blade.append(cube([xoff - w / 2 - 0.42, y0 + hh * 0.15, -th_i * 0.28],
                              [0.55, hh * 0.42, th_i * 0.56], "blade_light", "blade_dark"))
            blade.append(cube([xoff + w / 2 - 0.12, y0 + hh * 0.48, -th_i * 0.28],
                              [0.5, hh * 0.38, th_i * 0.56], "blade_dark", "blade_light"))
        if spec.get("crystal") and 0 < i < segs - 1 and i % 2 == 0:
            blade.append(cube([xoff - w / 2 - 0.32, y0 + hh * 0.2, -0.16],
                              [0.48, hh * 0.34, 0.32], "glow", "blade_light"))
            blade.append(cube([xoff + w / 2 - 0.16, y0 + hh * 0.5, -0.16],
                              [0.46, hh * 0.3, 0.32], "blade_light", "glow"))
    # micro point: the actual tip, centered on the convergence line
    last_w = bw * 0.2
    last_shift = (bw - last_w) * 0.4
    tip_x = spec["curve"] * 2.2 + last_shift
    tip_y = 4.1 + segs * seg_h - seg_h * 0.08
    blade.append(cube([tip_x - 0.11, tip_y, -th * 0.14], [0.22, seg_h * 0.52, th * 0.28],
                      "blade_light", "blade_dark"))
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
        "description": {"identifier": "geometry." + "SPEC_ID", "texture_width": TEX,
                        "texture_height": TEX, "visible_bounds_width": 5,
                        "visible_bounds_height": 5, "visible_bounds_offset": [0, 1.5, 0]},
        "bones": bones}]}


def _shade(base, f):
    return tuple(max(0, min(255, int(v * f))) for v in base) + (255,)


def paint(spec, glowmask=False):
    """256px atlas with painted realism: blade fuller and edge highlights, brushed
    metal, crosshatched wrap. Deterministic pseudo-noise so renders are stable."""
    import random
    pal = spec["pal"]
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0 if glowmask else 255))
    d = ImageDraw.Draw(img)
    rng = random.Random(hash(spec["pal"]["glow"]) & 0xFFFF)
    for name, (x, y, w, h) in REGIONS.items():
        base = pal[name]
        if glowmask:
            if name == "glow":
                d.rectangle([x, y, x + w - 1, y + h - 1], fill=base + (255,))
            elif name == "accent" and spec["pommel"] in ("gem", "orb"):
                d.rectangle([x, y, x + w - 1, y + h - 1], fill=base + (255,))
            continue
        grad = 0.09 if name.startswith("blade") else 0.2
        for i in range(h):
            t = i / h
            d.line([(x, y + i), (x + w - 1, y + i)], fill=_shade(base, 1.0 + grad / 2 - grad * t))
        # subtle speckle everywhere
        for _ in range(90):
            px, py = x + rng.randrange(w), y + rng.randrange(h)
            f = rng.choice((0.9, 1.1))
            d.point((px, py), fill=_shade(base, f))
        if name == "blade_light":
            # edge highlights on both long edges of the face
            d.rectangle([x, y, x + 3, y + h - 1], fill=_shade(base, 1.35))
            d.rectangle([x + w - 4, y, x + w - 1, y + h - 1], fill=_shade(base, 1.28))
            # fuller: bright ridge then dark groove, slightly off center
            cx = x + int(w * 0.46)
            d.line([(cx, y), (cx, y + h - 1)], fill=_shade(base, 1.3))
            d.line([(cx + 2, y), (cx + 2, y + h - 1)], fill=_shade(base, 0.62))
            d.line([(cx + 3, y), (cx + 3, y + h - 1)], fill=_shade(base, 0.78))
            # a few fine scratches
            for _ in range(7):
                sx = x + rng.randrange(6, w - 6)
                sy = y + rng.randrange(4, h - 14)
                d.line([(sx, sy), (sx + 1, sy + rng.randrange(5, 12))], fill=_shade(base, 1.22))
        if name == "blade_dark":
            d.line([(x + int(w * 0.5), y), (x + int(w * 0.5), y + h - 1)], fill=_shade(base, 0.7))
        if name == "grip_main":
            # diagonal crosshatch, reads as wrapped cord or leather diamonds
            step = 8
            for i in range(-h, w, step):
                d.line([(x + i, y), (x + i + h, y + h - 1)], fill=_shade(base, 0.72))
                d.line([(x + i, y + h - 1), (x + i + h, y)], fill=_shade(base, 1.18))
            if spec.get("scale_grip"):
                for i in range(0, w, 9):
                    for j in range(0, h, 9):
                        d.rectangle([x + i, y + j, x + i + 4, y + j + 4], outline=_shade(base, 0.6))
        if name in ("metal", "metal_dark"):
            for _ in range(26):
                sy = y + rng.randrange(h)
                d.line([(x, sy), (x + w - 1, sy)], fill=_shade(base, rng.choice((0.92, 1.08))))
            d.rectangle([x, y, x + w - 1, y + 3], fill=_shade(base, 1.15))
        if name == "glow":
            d.rectangle([x, y, x + 5, y + h - 1], fill=(255, 255, 255, 255))
            d.rectangle([x + 6, y, x + 10, y + h - 1], fill=_shade(base, 1.3))
    return img


def animations(spec):
    sl = spec["swing_len"]
    tilt = spec["charge_tilt"]
    return {"format_version": "1.8.0", "animations": {
        "idle": {"loop": True, "animation_length": 3.0, "bones": {
            "weapon": {"rotation": {"0.0": [0, 0, 0], "1.5": [0, 0, 1.5], "3.0": [0, 0, 0]},
                       "position": {"0.0": [0, 0, 0], "1.5": [0, 0.25, 0], "3.0": [0, 0, 0]}},
}},
        "swing": {"loop": True, "animation_length": sl, "bones": {
            "weapon": {"rotation": {"0.0": [0, 0, 0], "0.1": [0, 0, 26], "0.25": [0, 0, -18], str(sl): [0, 0, 0]},
                       "scale": {"0.0": [1, 1, 1], "0.1": [1.04, 1.04, 1.04], str(sl): [1, 1, 1]}},
}},
        "charge": {"loop": True, "animation_length": 0.8, "bones": {
            "weapon": {"rotation": {"0.0": [0, 0, tilt], "0.2": [0.6, 0, tilt + 1], "0.4": [-0.6, 0, tilt - 1], "0.6": [0.6, 0, tilt + 1], "0.8": [0, 0, tilt]},
                       "position": {"0.0": [0, 0.3, 0], "0.8": [0, 0.3, 0]}},

            "blade": {"rotation": {"0.0": [0, 0, 0], "0.2": [0, 0, 0.5], "0.4": [0, 0, -0.5], "0.6": [0, 0, 0.5], "0.8": [0, 0, 0]}}}},
    }}


def render_preview(geo, atlas):
    """Flat preview. Z rotation is applied so a scimitar does not draw as gaps."""
    cubes = [c for b in geo["minecraft:geometry"][0]["bones"] for c in b["cubes"]]

    def corners(c):
        ox, oy = c["origin"][0], c["origin"][1]
        sx, sy = c["size"][0], c["size"][1]
        pts = [(ox, oy), (ox + sx, oy), (ox + sx, oy + sy), (ox, oy + sy)]
        rot = c.get("rotation")
        if not rot or abs(rot[2]) < 0.01:
            return pts
        piv = c.get("pivot") or c["origin"]
        a = math.radians(rot[2])
        ca, sa = math.cos(a), math.sin(a)
        out = []
        for x, y in pts:
            dx, dy = x - piv[0], y - piv[1]
            out.append((piv[0] + dx * ca - dy * sa, piv[1] + dx * sa + dy * ca))
        return out

    polys = [corners(c) for c in cubes]
    xs = [p[0] for poly in polys for p in poly]
    ys = [p[1] for poly in polys for p in poly]
    min_x, max_x, min_y, max_y = min(xs), max(xs), min(ys), max(ys)
    scale, pad = 11, 10
    w = int((max_x - min_x) * scale) + pad * 2
    h = int((max_y - min_y) * scale) + pad * 2
    img = Image.new("RGBA", (max(w, 1), max(h, 1)), (18, 18, 24, 255))
    d = ImageDraw.Draw(img)
    order = sorted(range(len(cubes)), key=lambda i: cubes[i]["size"][0] * cubes[i]["size"][2])
    for i in order:
        c = cubes[i]
        uv = c["uv"]["north"]
        cx = int(uv["uv"][0] + uv["uv_size"][0] / 2)
        cy = int(uv["uv"][1] + uv["uv_size"][1] / 2)
        color = atlas.getpixel((cx, cy))
        pts = [(pad + (x - min_x) * scale, h - pad - (y - min_y) * scale) for x, y in polys[i]]
        d.polygon(pts, fill=color, outline=(0, 0, 0, 120))
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
