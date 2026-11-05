#!/usr/bin/env python3
"""Generate the sky_blade entity model for the Eclipse Domain execute.

These fall from the sky over a dark domain and shatter on the ground.
Built pointing DOWN like the celestial sword: model origin (0, 0, 0)
is the tip and the blade rises +Y. Total height is about 56 units
(3.5 blocks). The blade is broader (base width 7) and heavier than
the celestial sword, with a jagged back edge of three spike boxes
along the +X edge so the silhouette reads vicious.

Palette: core #FFFFFF, mid #9333EA, rim #E879F9, over dark violet-
black steel (#1B1030 family). Guard and hilt are blackened bronze.
The glowmask carries only the violet-magenta runes down the blade
center and the guard center stone in #E879F9 family color; the blade
material itself stays black on the mask.

Outputs (entity pipeline):
- geckolib/models/entity/sky_blade.geo.json
- geckolib/animations/entity/sky_blade.animation.json
- textures/entity/sky_blade.png
- textures/entity/sky_blade_glowmask.png

The cube layout, the UV REGIONS, and the RUNES list below are the
single source of truth: the geo emitter and both texture painters
read the same constants. The runes are painted at identical region
coordinates in the color texture and in the glowmask.

Run from the repo root: python3 tools/gen_sky_blade.py
"""
import json
import os

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src", "main", "resources", "assets", "dragonblockarcanedba")
NAME = "sky_blade"
TEX = 128

# Palette
STEEL = (27, 16, 48)          # #1B1030 dark violet-black steel
STEEL_DARK = (16, 9, 30)
VIOLET = (147, 51, 234)       # #9333EA
RUNE = (232, 121, 249)        # #E879F9
BRONZE = (74, 54, 32)         # blackened bronze
BRONZE_DARK = (44, 32, 20)

# UV regions on the 128x128 atlas: name -> (x, y, w, h).
REGIONS = {
    "blade": (0, 0, 32, 32),
    "blade_side": (32, 0, 32, 32),
    "guard": (64, 0, 32, 32),
    "guard_side": (96, 0, 32, 32),
    "stone": (0, 32, 32, 32),
    "grip": (32, 32, 32, 32),
    "grip_side": (64, 32, 32, 32),
    "bronze": (96, 32, 32, 32),
}

# Rune glyphs inside the blade region, as (cx, cy, kind) in region
# local texel coordinates. Painted down the blade center in both the
# color texture (bright magenta) and the glowmask (the only glow on
# the blade). Kinds: diamond, cross, bars, chevron.
RUNES = [
    (16, 4, "diamond"),
    (16, 11, "cross"),
    (16, 18, "bars"),
    (16, 25, "chevron"),
]

# Cube layout as data: (bone, origin, size, main region, side region,
# rotation, pivot). Origin is the low corner in absolute model units,
# +Z is the model front, +Y is up. Broad blade faces are north/south.
CUBES = [
    # blade bone: 3 stacked segments, base width 7 at the guard line
    # (y 40) tapering to 5.2 near the tip, plus 3 jagged spike boxes
    # along the +X back edge.
    ("blade", [-3.5, 30.0, -1.2], [7.0, 10.0, 2.4], "blade", "blade_side", None, None),
    ("blade", [-3.1, 19.0, -1.1], [6.2, 11.0, 2.2], "blade", "blade_side", None, None),
    ("blade", [-2.6, 10.0, -1.0], [5.2, 9.0, 2.0], "blade", "blade_side", None, None),
    ("blade", [3.5, 33.0, -0.7], [2.2, 1.8, 1.4], "blade", "blade_side", [0, 0, -24], [3.5, 34.0, 0]),
    ("blade", [3.1, 24.0, -0.65], [2.0, 1.6, 1.3], "blade", "blade_side", [0, 0, -24], [3.1, 25.0, 0]),
    ("blade", [2.6, 15.0, -0.6], [1.8, 1.5, 1.2], "blade", "blade_side", [0, 0, -24], [2.6, 16.0, 0]),
    # blade_tip bone: forged point, center block plus two wedges
    # rotated about the plant point.
    ("blade_tip", [-1.9, 4.0, -0.8], [3.8, 6.0, 1.6], "blade", "blade_side", None, None),
    ("blade_tip", [-1.9, 0.0, -0.6], [1.9, 5.0, 1.2], "blade", "blade_side", [0, 0, 20], [0, 0, 0]),
    ("blade_tip", [0.0, 0.0, -0.6], [1.9, 5.0, 1.2], "blade", "blade_side", [0, 0, -20], [0, 0, 0]),
    # guard bone: rough heavy crossbar, two downturned end blocks,
    # and a center stone on the front face that carries the glow.
    ("guard", [-6.5, 39.5, -1.5], [13.0, 3.0, 3.0], "guard", "guard_side", None, None),
    ("guard", [-8.2, 38.8, -1.25], [2.5, 2.5, 2.5], "guard", "guard_side", [0, 0, 20], [-6.5, 40.5, 0]),
    ("guard", [5.7, 38.8, -1.25], [2.5, 2.5, 2.5], "guard", "guard_side", [0, 0, -20], [6.5, 40.5, 0]),
    ("guard", [-1.25, 39.75, -2.0], [2.5, 2.5, 1.0], "stone", "stone", None, None),
    # hilt bone: short grip (wrapped look comes from texture bands),
    # heavy pommel block and a blunt pommel spike to about y 56.
    ("hilt", [-1.6, 42.5, -1.6], [3.2, 5.0, 3.2], "grip", "grip_side", None, None),
    ("hilt", [-1.6, 47.5, -1.6], [3.2, 4.5, 3.2], "grip", "grip_side", None, None),
    ("hilt", [-2.0, 52.0, -2.0], [4.0, 2.5, 4.0], "bronze", "bronze", None, None),
    ("hilt", [-0.75, 54.5, -0.75], [1.5, 2.0, 1.5], "bronze", "bronze", None, None),
]

# Bone tree: name -> (parent, pivot)
BONES = [
    ("root", None, [0, 0, 0]),
    ("blade", "root", [0, 40, 0]),
    ("blade_tip", "blade", [0, 10, 0]),
    ("guard", "root", [0, 40, 0]),
    ("hilt", "root", [0, 40, 0]),
]

ANIMATIONS = {
    "fall": {
        "loop": True,
        "animation_length": 0.4,
        "bones": {
            "root": {
                "rotation": {
                    "0.0": [1.5, 0, 1.5],
                    "0.1": [-1.5, 0, -1.5],
                    "0.2": [1.5, 0, -1.5],
                    "0.3": [-1.5, 0, 1.5],
                    "0.4": [1.5, 0, 1.5],
                }
            }
        },
    },
    "shatter": {
        "loop": "hold_on_last_frame",
        "animation_length": 0.35,
        "bones": {
            "root": {
                "position": {
                    "0.0": [0, 0, 0],
                    "0.175": [0, -14, 0],
                    "0.35": [0, -14, 0],
                },
                "rotation": {
                    "0.0": [0, 0, 0],
                    "0.175": [0, 0, 18],
                    "0.35": [0, 0, 18],
                },
            }
        },
    },
}


def clamp(v):
    return max(0, min(255, int(round(v))))


def shade(c, k):
    return (clamp(c[0] * k), clamp(c[1] * k), clamp(c[2] * k))


def vary(x, y, salt=0):
    """Deterministic per-texel variation so faces never read flat."""
    n = (x * 73856093) ^ (y * 19349663) ^ (salt * 83492791)
    n = (n ^ (n >> 13)) & 0xFFFF
    return 0.90 + (n / 65535.0) * 0.20


def draw_rune(draw, ox, oy, cx, cy, kind, color, width=2):
    """One small rune glyph centered at region-local (cx, cy)."""
    x, y = ox + cx, oy + cy
    if kind == "diamond":
        draw.polygon([(x, y - 3), (x + 3, y), (x, y + 3), (x - 3, y)], outline=color, width=width)
    elif kind == "cross":
        draw.line([x, y - 3, x, y + 3], fill=color, width=width)
        draw.line([x - 2, y - 1, x + 2, y - 1], fill=color, width=width)
    elif kind == "bars":
        draw.line([x - 3, y - 2, x + 3, y - 2], fill=color, width=width)
        draw.line([x - 2, y + 1, x + 2, y + 1], fill=color, width=width)
    elif kind == "chevron":
        draw.line([x - 3, y - 2, x, y + 2], fill=color, width=width)
        draw.line([x, y + 2, x + 3, y - 2], fill=color, width=width)


def face_uv(region):
    x, y, w, h = REGIONS[region]
    return {"uv": [x, y], "uv_size": [w, h]}


def build_geo():
    bones = []
    for name, parent, pivot in BONES:
        bone = {"name": name, "pivot": pivot}
        if parent is not None:
            bone["parent"] = parent
        cubes = []
        for bname, origin, size, main, side, rot, piv in CUBES:
            if bname != name:
                continue
            c = {"origin": origin, "size": size, "uv": {
                "north": face_uv(main), "south": face_uv(main),
                "up": face_uv(side), "down": face_uv(side),
                "east": face_uv(side), "west": face_uv(side)}}
            if rot is not None:
                c["rotation"] = rot
                c["pivot"] = piv
            cubes.append(c)
        if cubes:
            bone["cubes"] = cubes
        bones.append(bone)
    geo = {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.dragonblockarcanedba." + NAME,
                "texture_width": TEX,
                "texture_height": TEX,
                "visible_bounds_width": 7,
                "visible_bounds_height": 7,
                "visible_bounds_offset": [0, 1.75, 0],
            },
            "bones": bones,
        }],
    }
    path = os.path.join(ROOT, "geckolib", "models", "entity", NAME + ".geo.json")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        json.dump(geo, fh, indent=2)
        fh.write("\n")
    return path


def paint_base(draw, img):
    """Paint the color texture region by region."""
    px = img.load()
    # blade: dark violet-black steel, brighter violet core stripe in
    # the center columns, runes painted over the stripe below.
    x0, y0, w, h = REGIONS["blade"]
    for j in range(h):
        grad = 1.0 - 0.15 * (j / max(1, h - 1))
        for i in range(w):
            if abs(i - (w - 1) / 2.0) <= 2.5:
                c = shade((86, 44, 140), grad)
            elif i <= 1 or i >= w - 2:
                c = shade((52, 32, 84), grad)
            else:
                c = shade(STEEL, grad)
            v = vary(i, j, 1)
            px[x0 + i, y0 + j] = (clamp(c[0] * v), clamp(c[1] * v), clamp(c[2] * v), 255)
    for cx, cy, kind in RUNES:
        draw_rune(draw, x0, y0, cx, cy, kind, RUNE)
    draw.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], outline=STEEL_DARK)
    # blade_side: near-black edge with a single violet file line.
    x0, y0, w, h = REGIONS["blade_side"]
    for j in range(h):
        for i in range(w):
            c = shade(STEEL_DARK, vary(i, j, 2))
            px[x0 + i, y0 + j] = (c[0], c[1], c[2], 255)
    draw.line([x0 + w // 2, y0, x0 + w // 2, y0 + h - 1], fill=shade(VIOLET, 0.8))
    # guard: blackened bronze, mottled, darker border.
    for region, base, salt in (("guard", BRONZE, 3), ("guard_side", BRONZE_DARK, 4)):
        x0, y0, w, h = REGIONS[region]
        for j in range(h):
            for i in range(w):
                c = shade(base, vary(i, j, salt))
                px[x0 + i, y0 + j] = (c[0], c[1], c[2], 255)
        draw.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], outline=(20, 14, 8), width=2)
        draw.line([x0, y0 + 4, x0 + w - 1, y0 + 4], fill=shade(BRONZE, 1.25))
    # stone: dark socket with a bright magenta center stone.
    x0, y0, w, h = REGIONS["stone"]
    for j in range(h):
        for i in range(w):
            d = max(abs(i - (w - 1) / 2.0), abs(j - (h - 1) / 2.0)) / (w / 2.0)
            c = RUNE if d <= 0.38 else (shade(VIOLET, 0.8) if d <= 0.55 else (18, 12, 24))
            cc = shade(c, vary(i, j, 5)) if d > 0.38 else c
            px[x0 + i, y0 + j] = (cc[0], cc[1], cc[2], 255)
    # grip: wrapped look from horizontal bands, near-black leather
    # alternating with dark violet wraps and thin bronze separators.
    for region, salt in (("grip", 6), ("grip_side", 7)):
        x0, y0, w, h = REGIONS[region]
        for j in range(h):
            band = (j // 4) % 2
            base = (26, 20, 34) if band == 0 else (48, 30, 74)
            for i in range(w):
                c = shade(base, vary(i, j, salt))
                px[x0 + i, y0 + j] = (c[0], c[1], c[2], 255)
        for j in range(3, h, 8):
            draw.line([x0, y0 + j, x0 + w - 1, y0 + j], fill=BRONZE_DARK)
    # bronze: pommel metal, same family as the guard, heavier shade.
    x0, y0, w, h = REGIONS["bronze"]
    for j in range(h):
        for i in range(w):
            c = shade(shade(BRONZE, 0.9), vary(i, j, 8))
            px[x0 + i, y0 + j] = (c[0], c[1], c[2], 255)
    draw.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], outline=(20, 14, 8), width=2)


def paint_glow(draw, img):
    """Paint the glowmask: black base. Only the blade runes and the
    guard center stone glow, in #E879F9 family color. The blade
    material itself stays black on the mask."""
    x0, y0, w, h = REGIONS["blade"]
    for cx, cy, kind in RUNES:
        draw_rune(draw, x0, y0, cx, cy, kind, RUNE)
    x0, y0, w, h = REGIONS["stone"]
    draw.ellipse([x0 + 10, y0 + 10, x0 + w - 11, y0 + h - 11], fill=RUNE)
    draw.ellipse([x0 + 13, y0 + 13, x0 + w - 14, y0 + h - 14], fill=(250, 200, 255))


def build_textures():
    tex_dir = os.path.join(ROOT, "textures", "entity")
    os.makedirs(tex_dir, exist_ok=True)
    img = Image.new("RGBA", (TEX, TEX), (12, 8, 20, 255))
    paint_base(ImageDraw.Draw(img), img)
    tex_path = os.path.join(tex_dir, NAME + ".png")
    img.save(tex_path)
    glow = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 255))
    paint_glow(ImageDraw.Draw(glow), glow)
    glow_path = os.path.join(tex_dir, NAME + "_glowmask.png")
    glow.save(glow_path)
    return tex_path, glow_path


def build_animations():
    payload = {"format_version": "1.10.0", "animations": ANIMATIONS}
    path = os.path.join(ROOT, "geckolib", "animations", "entity", NAME + ".animation.json")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        json.dump(payload, fh, indent=2)
        fh.write("\n")
    return path


def main():
    geo = build_geo()
    tex, glow = build_textures()
    anim = build_animations()
    print(NAME, "cubes", len(CUBES))
    for p in (geo, tex, glow, anim):
        print("wrote", p)


if __name__ == "__main__":
    main()
