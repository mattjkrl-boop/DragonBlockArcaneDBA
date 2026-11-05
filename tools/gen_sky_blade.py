#!/usr/bin/env python3
"""Generate the sky_blade entity model for the Eclipse Domain execute.

These fall from the sky over a dark domain and shatter on the ground.
Built pointing DOWN like the celestial sword: model origin (0, 0, 0)
is the tip and the blade rises +Y. Total height is about 56 units
(3.5 blocks). The blade is broad (base width 7) and heavy: rotated
edge bevels run down both cutting edges, a dark fuller channel is
inset into both broad faces, panel seams cross the blade, and four
jagged back-edge spikes are individual rooted cubes with varied
rotation. The guard is a heavy layered structure (main bar, top and
underside plates, downturned end blocks and caps) around a pronged
center stone setting. The hilt carries four bronze grip wrap rings
and a collared pommel spike.

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
TEX = 256

# Palette
STEEL = (27, 16, 48)          # #1B1030 dark violet-black steel
STEEL_DARK = (16, 9, 30)
VIOLET = (147, 51, 234)       # #9333EA
RUNE = (232, 121, 249)        # #E879F9
BRONZE = (74, 54, 32)         # blackened bronze
BRONZE_DARK = (44, 32, 20)

# UV regions on the 256x256 atlas: name -> (x, y, w, h). Each region
# is 64x64, double the old 32x32 regions in each dimension.
REGIONS = {
    "blade": (0, 0, 64, 64),
    "blade_side": (64, 0, 64, 64),
    "guard": (128, 0, 64, 64),
    "guard_side": (192, 0, 64, 64),
    "stone": (0, 64, 64, 64),
    "grip": (64, 64, 64, 64),
    "grip_side": (128, 64, 64, 64),
    "bronze": (192, 64, 64, 64),
    "edge": (0, 128, 64, 64),
    "fuller": (64, 128, 64, 64),
    "plate": (128, 128, 64, 64),
}

# Rune glyphs inside the blade region, as (cx, cy, kind) in region
# local texel coordinates. Painted down the blade center in both the
# color texture (bright magenta) and the glowmask (the only glow on
# the blade). Kinds: diamond, cross, bars, chevron.
RUNES = [
    (32, 8, "diamond"),
    (32, 22, "cross"),
    (32, 36, "bars"),
    (32, 50, "chevron"),
]

# Cube layout as data: (bone, origin, size, main region, side region,
# rotation, pivot). Origin is the low corner in absolute model units,
# +Z is the model front, +Y is up. Broad blade faces are north/south.
CUBES = [
    # blade bone: 3 stacked segments, base width 7 at the guard line
    # (y 40) tapering to 5.2 near the tip.
    ("blade", [-3.5, 30.0, -1.2], [7.0, 10.0, 2.4], "blade", "blade_side", None, None),
    ("blade", [-3.1, 19.0, -1.1], [6.2, 11.0, 2.2], "blade", "blade_side", None, None),
    ("blade", [-2.6, 10.0, -1.0], [5.2, 9.0, 2.0], "blade", "blade_side", None, None),
    # fuller channel: dark inset strips proud of both broad faces.
    ("blade", [-0.7, 12.0, 1.05], [1.4, 26.0, 0.3], "fuller", "fuller", None, None),
    ("blade", [-0.7, 12.0, -1.35], [1.4, 26.0, 0.3], "fuller", "fuller", None, None),
    # rotated edge bevels along both cutting edges.
    ("blade", [-3.85, 12.0, -1.3], [0.8, 27.0, 2.6], "edge", "edge", [0, 0, 3], [-3.45, 25.0, 0]),
    ("blade", [3.05, 12.0, -1.3], [0.8, 27.0, 2.6], "edge", "edge", [0, 0, -3], [3.45, 25.0, 0]),
    # jagged back-edge spikes along +X: individual rooted cubes with
    # varied size and rotation so the silhouette reads vicious.
    ("blade", [3.5, 33.0, -0.7], [2.2, 1.8, 1.4], "blade", "blade_side", [0, 0, -24], [3.5, 34.0, 0]),
    ("blade", [3.1, 24.5, -0.65], [2.0, 1.6, 1.3], "blade", "blade_side", [0, 0, -30], [3.1, 25.3, 0]),
    ("blade", [2.6, 16.5, -0.6], [1.9, 1.5, 1.2], "blade", "blade_side", [0, 0, -18], [2.6, 17.2, 0]),
    ("blade", [2.6, 10.5, -0.55], [1.7, 1.4, 1.1], "blade", "blade_side", [0, 0, -34], [2.6, 11.2, 0]),
    # raised spine ridge down the back face.
    ("blade", [-0.5, 14.0, -1.5], [1.0, 24.0, 0.35], "blade_side", "blade_side", None, None),
    # panel seam bars crossing the blade at the segment joins.
    ("blade", [-3.3, 29.7, -1.25], [6.6, 0.5, 2.5], "fuller", "fuller", None, None),
    ("blade", [-2.9, 18.7, -1.15], [5.8, 0.5, 2.3], "fuller", "fuller", None, None),
    # blade_tip bone: forged point, center block, two wedges rotated
    # about the plant point, two bevel strips and a diamond ridge cap.
    ("blade_tip", [-1.9, 4.0, -0.8], [3.8, 6.0, 1.6], "blade", "blade_side", None, None),
    ("blade_tip", [-1.9, 0.0, -0.6], [1.9, 5.0, 1.2], "blade", "blade_side", [0, 0, 20], [0, 0, 0]),
    ("blade_tip", [0.0, 0.0, -0.6], [1.9, 5.0, 1.2], "blade", "blade_side", [0, 0, -20], [0, 0, 0]),
    ("blade_tip", [-2.15, 0.9, -0.85], [0.7, 4.6, 1.7], "edge", "edge", [0, 0, 18], [-1.8, 3.1, 0]),
    ("blade_tip", [1.45, 0.9, -0.85], [0.7, 4.6, 1.7], "edge", "edge", [0, 0, -18], [1.8, 3.1, 0]),
    ("blade_tip", [-0.7, 2.2, -0.7], [1.4, 1.4, 1.4], "edge", "edge", [0, 0, 45], [0, 2.9, 0]),
    # guard bone: heavy layered crossbar. Main bar, top plate,
    # underside plate, downturned end blocks and angled end caps,
    # and a collar block wrapping the blade base.
    ("guard", [-6.5, 39.5, -1.5], [13.0, 3.0, 3.0], "guard", "guard_side", None, None),
    ("guard", [-5.5, 42.3, -1.2], [11.0, 0.8, 2.4], "plate", "plate", None, None),
    ("guard", [-5.5, 38.7, -1.2], [11.0, 0.8, 2.4], "plate", "plate", None, None),
    ("guard", [-8.2, 38.8, -1.25], [2.5, 2.5, 2.5], "guard", "guard_side", [0, 0, 20], [-6.5, 40.5, 0]),
    ("guard", [5.7, 38.8, -1.25], [2.5, 2.5, 2.5], "guard", "guard_side", [0, 0, -20], [6.5, 40.5, 0]),
    ("guard", [-9.6, 39.4, -1.0], [1.8, 2.0, 2.0], "plate", "plate", [0, 0, 38], [-8.7, 40.4, 0]),
    ("guard", [7.8, 39.4, -1.0], [1.8, 2.0, 2.0], "plate", "plate", [0, 0, -38], [8.7, 40.4, 0]),
    ("guard", [-2.4, 41.6, -1.9], [4.8, 1.6, 3.8], "bronze", "bronze", None, None),
    # guard_stone bone: center stone on both faces, held by four
    # bronze prongs on the front setting. Own bone so the stone can
    # pulse while falling and break away in the shatter.
    ("guard_stone", [-1.25, 39.75, -2.0], [2.5, 2.5, 1.0], "stone", "stone", None, None),
    ("guard_stone", [-1.25, 39.75, 1.0], [2.5, 2.5, 1.0], "stone", "stone", None, None),
    ("guard_stone", [-1.6, 41.9, -2.1], [0.7, 0.7, 0.7], "bronze", "bronze", None, None),
    ("guard_stone", [0.9, 41.9, -2.1], [0.7, 0.7, 0.7], "bronze", "bronze", None, None),
    ("guard_stone", [-1.6, 39.4, -2.1], [0.7, 0.7, 0.7], "bronze", "bronze", None, None),
    ("guard_stone", [0.9, 39.4, -2.1], [0.7, 0.7, 0.7], "bronze", "bronze", None, None),
    # hilt bone: two grip segments, four bronze wrap rings, pommel
    # collar, heavy pommel block, and a pommel spike with a rotated
    # diamond tip, top at about y 56.5.
    ("hilt", [-1.6, 42.5, -1.6], [3.2, 5.0, 3.2], "grip", "grip_side", None, None),
    ("hilt", [-1.6, 47.5, -1.6], [3.2, 4.5, 3.2], "grip", "grip_side", None, None),
    ("hilt", [-1.8, 44.0, -1.8], [3.6, 0.7, 3.6], "bronze", "bronze", None, None),
    ("hilt", [-1.8, 46.2, -1.8], [3.6, 0.7, 3.6], "bronze", "bronze", None, None),
    ("hilt", [-1.8, 48.4, -1.8], [3.6, 0.7, 3.6], "bronze", "bronze", None, None),
    ("hilt", [-1.8, 50.6, -1.8], [3.6, 0.7, 3.6], "bronze", "bronze", None, None),
    ("hilt", [-1.7, 51.6, -1.7], [3.4, 0.7, 3.4], "plate", "plate", None, None),
    ("hilt", [-2.0, 52.0, -2.0], [4.0, 2.5, 4.0], "bronze", "bronze", None, None),
    ("hilt", [-0.75, 54.3, -0.75], [1.5, 1.7, 1.5], "bronze", "bronze", None, None),
    ("hilt", [-0.5, 55.6, -0.5], [1.0, 0.9, 1.0], "edge", "edge", [0, 45, 0], [0, 56.0, 0]),
]

# Bone tree: name -> (parent, pivot)
BONES = [
    ("root", None, [0, 0, 0]),
    ("blade", "root", [0, 40, 0]),
    ("blade_tip", "blade", [0, 10, 0]),
    ("guard", "root", [0, 40, 0]),
    ("guard_stone", "guard", [0, 41, 0]),
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
            },
            "blade": {
                "rotation": {
                    "0.0": [0, 0, 0.9],
                    "0.1": [0.4, 0, -0.9],
                    "0.2": [-0.4, 0, 0.9],
                    "0.3": [0.4, 0, -0.9],
                    "0.4": [0, 0, 0.9],
                }
            },
            "blade_tip": {
                "rotation": {
                    "0.0": [0.6, 0, 0.4],
                    "0.1": [-0.6, 0, -0.4],
                    "0.2": [0.6, 0, -0.4],
                    "0.3": [-0.6, 0, 0.4],
                    "0.4": [0.6, 0, 0.4],
                }
            },
            "guard": {
                "rotation": {
                    "0.0": [0, 0, -0.7],
                    "0.1": [0, 0, 0.7],
                    "0.2": [0, 0, -0.7],
                    "0.3": [0, 0, 0.7],
                    "0.4": [0, 0, -0.7],
                }
            },
            "guard_stone": {
                "scale": {
                    "0.0": [1.0, 1.0, 1.0],
                    "0.1": [1.18, 1.18, 1.18],
                    "0.2": [1.0, 1.0, 1.0],
                    "0.3": [1.24, 1.24, 1.24],
                    "0.4": [1.0, 1.0, 1.0],
                }
            },
            "hilt": {
                "rotation": {
                    "0.0": [0, 0, -0.5],
                    "0.1": [0, 0, 0.5],
                    "0.2": [0, 0, -0.5],
                    "0.3": [0, 0, 0.5],
                    "0.4": [0, 0, -0.5],
                }
            },
        },
    },
    "shatter": {
        "loop": "hold_on_last_frame",
        "animation_length": 0.35,
        "bones": {
            "root": {
                "position": {
                    "0.0": [0, 0, 0],
                    "0.0875": [0, -6, 0],
                    "0.175": [0, -14, 0],
                    "0.35": [0, -14, 0],
                },
                "rotation": {
                    "0.0": [0, 0, 0],
                    "0.0875": [0, 0, 7],
                    "0.175": [0, 0, 18],
                    "0.35": [0, 0, 18],
                },
            },
            "blade": {
                "rotation": {
                    "0.0": [0, 0, 0],
                    "0.12": [0, 0, -6],
                    "0.25": [0, 0, -14],
                    "0.35": [0, 0, -19],
                },
                "position": {
                    "0.0": [0, 0, 0],
                    "0.175": [-1.5, -2, 0],
                    "0.35": [-3, -3.5, 0],
                },
            },
            "blade_tip": {
                "rotation": {
                    "0.0": [0, 0, 0],
                    "0.15": [0, 0, 24],
                    "0.35": [0, 0, 48],
                },
                "position": {
                    "0.0": [0, 0, 0],
                    "0.2": [1, -4, 0],
                    "0.35": [2.5, -7, 0],
                },
            },
            "guard": {
                "rotation": {
                    "0.0": [0, 0, 0],
                    "0.175": [0, 0, 10],
                    "0.35": [0, 0, 16],
                },
                "position": {
                    "0.0": [0, 0, 0],
                    "0.175": [2, -1, 0],
                    "0.35": [4, -2, 0],
                },
            },
            "guard_stone": {
                "position": {
                    "0.0": [0, 0, 0],
                    "0.15": [0, -3, -3],
                    "0.35": [0, -8, -6],
                },
                "rotation": {
                    "0.0": [0, 0, 0],
                    "0.15": [90, 0, 30],
                    "0.35": [180, 0, 60],
                },
                "scale": {
                    "0.0": [1.0, 1.0, 1.0],
                    "0.15": [0.85, 0.85, 0.85],
                    "0.35": [0.65, 0.65, 0.65],
                },
            },
            "hilt": {
                "rotation": {
                    "0.0": [0, 0, 0],
                    "0.175": [0, 0, -8],
                    "0.35": [0, 0, -13],
                },
                "position": {
                    "0.0": [0, 0, 0],
                    "0.175": [1.5, 1, 0],
                    "0.35": [3, 2, 0],
                },
            },
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


def lerp(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def draw_rune(draw, ox, oy, cx, cy, kind, color, width=3, s=6):
    """One rune glyph centered at region-local (cx, cy), half-size s."""
    x, y = ox + cx, oy + cy
    a = max(2, (s * 2) // 3)
    b = max(1, s // 3)
    if kind == "diamond":
        draw.polygon([(x, y - s), (x + s, y), (x, y + s), (x - s, y)], outline=color, width=width)
    elif kind == "cross":
        draw.line([x, y - s, x, y + s], fill=color, width=width)
        draw.line([x - a, y - b, x + a, y - b], fill=color, width=width)
    elif kind == "bars":
        draw.line([x - s, y - a, x + s, y - a], fill=color, width=width)
        draw.line([x - a, y + b, x + a, y + b], fill=color, width=width)
    elif kind == "chevron":
        draw.line([x - s, y - a, x, y + a], fill=color, width=width)
        draw.line([x, y + a, x + s, y - a], fill=color, width=width)


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
    # blade: dark violet-black steel with a painted shading gradient,
    # a brighter violet core stripe in the center columns, bright
    # outer edge columns, panel seam lines, and the runes over the
    # stripe.
    x0, y0, w, h = REGIONS["blade"]
    for j in range(h):
        grad = 1.06 - 0.30 * (j / max(1, h - 1))
        for i in range(w):
            if abs(i - (w - 1) / 2.0) <= 5.5:
                c = shade((86, 44, 140), grad)
            elif i <= 3 or i >= w - 4:
                c = shade((64, 40, 104), grad)
            else:
                c = shade(STEEL, grad)
                # long brushed sheen streaks off the core stripe
                if (i * 7 + j * 3) % 29 == 0:
                    c = shade(c, 1.35)
            v = vary(i, j, 1)
            px[x0 + i, y0 + j] = (clamp(c[0] * v), clamp(c[1] * v), clamp(c[2] * v), 255)
    for cx, cy, kind in RUNES:
        draw_rune(draw, x0, y0, cx, cy, kind, RUNE)
    for j in (15, 31, 47):
        draw.line([x0 + 2, y0 + j, x0 + w - 3, y0 + j], fill=STEEL_DARK, width=2)
    for (rx, ry) in ((4, 4), (w - 5, 4), (4, h - 5), (w - 5, h - 5)):
        draw.ellipse([x0 + rx - 1, y0 + ry - 1, x0 + rx + 1, y0 + ry + 1], fill=shade(VIOLET, 1.2))
    draw.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], outline=STEEL_DARK, width=2)
    # blade_side: near-black edge steel with a violet file line and
    # notch ticks along one side.
    x0, y0, w, h = REGIONS["blade_side"]
    for j in range(h):
        for i in range(w):
            c = shade(STEEL_DARK, vary(i, j, 2) * (1.05 - 0.2 * (j / max(1, h - 1))))
            px[x0 + i, y0 + j] = (c[0], c[1], c[2], 255)
    draw.line([x0 + w // 2, y0, x0 + w // 2, y0 + h - 1], fill=shade(VIOLET, 0.8), width=2)
    for j in range(6, h, 12):
        draw.line([x0 + w - 10, y0 + j, x0 + w - 3, y0 + j + 4], fill=shade(VIOLET, 0.55), width=2)
    # edge: bright forged bevel steel, light core fading to dark
    # violet at the borders so rotated bevels catch the eye.
    x0, y0, w, h = REGIONS["edge"]
    for j in range(h):
        for i in range(w):
            t = abs(i - (w - 1) / 2.0) / (w / 2.0)
            c = lerp((168, 140, 214), (50, 32, 88), t)
            c = shade(c, vary(i, j, 9) * (1.04 - 0.16 * (j / max(1, h - 1))))
            px[x0 + i, y0 + j] = (c[0], c[1], c[2], 255)
    draw.line([x0 + w // 2, y0, x0 + w // 2, y0 + h - 1], fill=(232, 222, 250), width=2)
    # fuller: near-black channel with a deep violet center groove.
    x0, y0, w, h = REGIONS["fuller"]
    for j in range(h):
        for i in range(w):
            t = abs(i - (w - 1) / 2.0) / (w / 2.0)
            c = lerp((30, 16, 52), (10, 6, 20), t)
            c = shade(c, vary(i, j, 10))
            px[x0 + i, y0 + j] = (c[0], c[1], c[2], 255)
    draw.line([x0 + w // 2, y0, x0 + w // 2, y0 + h - 1], fill=(52, 26, 88), width=3)
    # plate: heavy dark bronze plate with rivets and a worn top edge.
    x0, y0, w, h = REGIONS["plate"]
    for j in range(h):
        for i in range(w):
            c = shade(lerp((96, 70, 42), (40, 28, 16), j / max(1, h - 1)), vary(i, j, 11))
            px[x0 + i, y0 + j] = (c[0], c[1], c[2], 255)
    draw.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], outline=(18, 12, 6), width=3)
    draw.line([x0, y0 + 5, x0 + w - 1, y0 + 5], fill=shade(BRONZE, 1.4), width=2)
    for rx in (8, w // 2, w - 9):
        for ry in (10, h - 11):
            draw.ellipse([x0 + rx - 2, y0 + ry - 2, x0 + rx + 2, y0 + ry + 2],
                         fill=shade(BRONZE, 1.5), outline=(18, 12, 6))
    # guard: blackened bronze, mottled, vertical shading gradient,
    # darker border and a bright worn highlight line.
    for region, base, salt in (("guard", BRONZE, 3), ("guard_side", BRONZE_DARK, 4)):
        x0, y0, w, h = REGIONS[region]
        for j in range(h):
            grad = 1.10 - 0.35 * (j / max(1, h - 1))
            for i in range(w):
                c = shade(base, vary(i, j, salt) * grad)
                px[x0 + i, y0 + j] = (c[0], c[1], c[2], 255)
        draw.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], outline=(20, 14, 8), width=3)
        draw.line([x0, y0 + 8, x0 + w - 1, y0 + 8], fill=shade(BRONZE, 1.3), width=2)
        draw.line([x0, y0 + h - 9, x0 + w - 1, y0 + h - 9], fill=(24, 17, 10), width=2)
    # stone: dark socket with a bright magenta center stone and a
    # faceted highlight.
    x0, y0, w, h = REGIONS["stone"]
    for j in range(h):
        for i in range(w):
            d = max(abs(i - (w - 1) / 2.0), abs(j - (h - 1) / 2.0)) / (w / 2.0)
            c = RUNE if d <= 0.38 else (shade(VIOLET, 0.8) if d <= 0.55 else (18, 12, 24))
            cc = shade(c, vary(i, j, 5)) if d > 0.38 else c
            px[x0 + i, y0 + j] = (cc[0], cc[1], cc[2], 255)
    draw.line([x0 + 22, y0 + 26, x0 + 32, y0 + 18], fill=(250, 200, 255), width=3)
    draw.line([x0 + 32, y0 + 18, x0 + 42, y0 + 26], fill=(214, 120, 235), width=3)
    # grip: wrapped look from horizontal bands, near-black leather
    # alternating with dark violet wraps and thin bronze separators.
    for region, salt in (("grip", 6), ("grip_side", 7)):
        x0, y0, w, h = REGIONS[region]
        for j in range(h):
            band = (j // 8) % 2
            base = (26, 20, 34) if band == 0 else (48, 30, 74)
            grad = 1.08 - 0.25 * ((j % 8) / 7.0)
            for i in range(w):
                c = shade(base, vary(i, j, salt) * grad)
                px[x0 + i, y0 + j] = (c[0], c[1], c[2], 255)
        for j in range(7, h, 16):
            draw.line([x0, y0 + j, x0 + w - 1, y0 + j], fill=BRONZE_DARK, width=2)
    # bronze: pommel and ring metal, same family as the guard,
    # heavier shade with a worn highlight band.
    x0, y0, w, h = REGIONS["bronze"]
    for j in range(h):
        grad = 1.12 - 0.38 * (j / max(1, h - 1))
        for i in range(w):
            c = shade(shade(BRONZE, 0.9), vary(i, j, 8) * grad)
            px[x0 + i, y0 + j] = (c[0], c[1], c[2], 255)
    draw.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], outline=(20, 14, 8), width=3)
    draw.line([x0, y0 + 10, x0 + w - 1, y0 + 10], fill=shade(BRONZE, 1.35), width=2)


def paint_glow(draw, img):
    """Paint the glowmask: black base. Only the blade runes and the
    core of the guard center stone glow, in #E879F9 family color.
    The blade material itself stays black on the mask."""
    x0, y0, w, h = REGIONS["blade"]
    for cx, cy, kind in RUNES:
        draw_rune(draw, x0, y0, cx, cy, kind, RUNE)
    x0, y0, w, h = REGIONS["stone"]
    draw.ellipse([x0 + 24, y0 + 24, x0 + w - 25, y0 + h - 25], fill=RUNE)
    draw.ellipse([x0 + 27, y0 + 27, x0 + w - 28, y0 + h - 28], fill=(250, 200, 255))


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
