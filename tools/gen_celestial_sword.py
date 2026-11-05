#!/usr/bin/env python3
"""Generate the celestial_sword entity model for Spirit Impale.

Six of these plant tip-first around a ground seal. The sword is built
pointing DOWN: model origin (0, 0, 0) is the plant point at the tip and
the sword rises +Y from there. Total height is 66 units (about 4.1
blocks): blade about 46 long, guard at y 46, grip and pommel above.

Model quality pass: the sword is built to the same density as the
authored Spirit Sword item model in its own weapon family. Each blade
segment carries a raised central ridge spine, rotated diamond edge
bevels on both cutting edges, and a structured collar ring at every
segment joint. The tip is a true forged point: center block, two side
wedges, two thickness wedges, and a diamond spike. The guard is
layered (main bar, raised center block, underside gold plate, gold
end caps, upswept ends, gem stones front and back). The grip carries
gold wrap rings, and the pommel gem sits in a gold crown setting of
four prongs under a crown spike.

Palette (Spirit): core #FFFFFF, mid #4DE8E0, rim #0E7490. Guard and
hilt are deep teal with gold trim; the blade is near-white with a
deepened cyan fuller channel and seam shading so it does not read
washed out. The glowmask carries only energy accents: the blade edge
columns, the edge bevel rails, the fuller core on the ridge spine,
the blade side steel, and the gem. Every metal part stays black on
the mask, and the blade face itself never glows whole.

Outputs (entity pipeline):
- geckolib/models/entity/celestial_sword.geo.json
- geckolib/animations/entity/celestial_sword.animation.json
- textures/entity/celestial_sword.png
- textures/entity/celestial_sword_glowmask.png

The cube layout and the UV REGIONS below are the single source of
truth: the geo emitter and both texture painters read the same
constants, so a painted stripe always lands in the right UV cells.

Run from the repo root: python3 tools/gen_celestial_sword.py
"""
import json
import math
import os

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src", "main", "resources", "assets", "dragonblockarcanedba")
NAME = "celestial_sword"
TEX = 256

# Palette
CORE = (255, 255, 255)        # #FFFFFF
MID = (77, 232, 224)          # #4DE8E0
RIM = (14, 116, 144)          # #0E7490
CYAN_DEEP = (18, 168, 196)    # deepened fuller cyan
CYAN_DARK = (10, 110, 138)    # fuller channel shadow
GOLD = (216, 178, 72)
GOLD_DARK = (150, 112, 36)

# UV regions on the 256x256 atlas: name -> (x, y, w, h). Every cube
# face maps to one full region, exactly like the migrated weapon
# pipeline. Regions are 64x64, double the old 32x32 cells.
REGIONS = {
    "blade": (0, 0, 64, 64),
    "blade_side": (64, 0, 64, 64),
    "guard": (128, 0, 64, 64),
    "guard_side": (192, 0, 64, 64),
    "gold": (0, 64, 64, 64),
    "grip": (64, 64, 64, 64),
    "grip_side": (128, 64, 64, 64),
    "gem": (192, 64, 64, 64),
    "ridge": (0, 128, 64, 64),
    "bevel": (64, 128, 64, 64),
    "collar": (128, 128, 64, 64),
}

# Blade segments as data: (y, width, depth), stacked from the guard
# line (y 46) down to the tip joint (y 10), tapering as they fall.
SEGMENTS = [
    (37.0, 5.5, 2.0),
    (28.0, 5.1, 2.0),
    (19.0, 4.6, 1.8),
    (10.0, 4.2, 1.6),
]

# Cube layout as data: (bone, origin, size, main region, side region,
# rotation, pivot). Origin is the low corner in absolute model units,
# +Z is the model front, +Y is up. Broad blade faces are north/south.
CUBES = []

# blade bone: per segment a main slab, a raised ridge spine that
# protrudes 0.35 on both broad faces, and a rotated diamond bevel on
# each cutting edge. A structured collar ring (a band that protrudes
# on every side, painted teal with a gold stripe) sits at each joint.
for _y, _w, _d in SEGMENTS:
    CUBES.append(("blade", [-_w / 2.0, _y, -_d / 2.0], [_w, 9.0, _d],
                  "blade", "blade_side", None, None))
    CUBES.append(("blade", [-0.7, _y, -_d / 2.0 - 0.35], [1.4, 9.0, _d + 0.7],
                  "ridge", "blade_side", None, None))
    for _sx in (-1, 1):
        _cx = _sx * _w / 2.0
        CUBES.append(("blade", [_cx - 0.4, _y, -0.4], [0.8, 9.0, 0.8],
                      "bevel", "bevel", [0, 45, 0], [_cx, _y + 4.5, 0]))
# collar rings at the joints y 37, 28, 19 (sized to the segment below
# the joint) and at y 10 above the tip block.
for _jy, _w, _d in ((37.0, 5.1, 2.0), (28.0, 4.6, 1.8),
                    (19.0, 4.2, 1.6), (10.0, 3.4, 1.4)):
    CUBES.append(("blade", [-(_w + 0.7) / 2.0, _jy - 0.5, -(_d + 0.7) / 2.0],
                  [_w + 0.7, 1.0, _d + 0.7], "collar", "collar", None, None))

CUBES += [
    # blade_tip bone: forged point. Center block, two side wedges
    # rotated about the plant point, two thickness wedges rotated
    # about X so the point tapers in depth as well as width, and a
    # diamond cross-section spike that ends exactly at the origin.
    ("blade_tip", [-1.7, 4.0, -0.7], [3.4, 6.5, 1.4], "blade", "blade_side", None, None),
    ("blade_tip", [-1.7, 0.0, -0.6], [1.7, 5.0, 1.2], "blade", "blade_side", [0, 0, 18], [0, 0, 0]),
    ("blade_tip", [0.0, 0.0, -0.6], [1.7, 5.0, 1.2], "blade", "blade_side", [0, 0, -18], [0, 0, 0]),
    ("blade_tip", [-1.1, 0.5, 0.0], [2.2, 5.5, 0.85], "blade", "blade_side", [16, 0, 0], [0, 0, 0]),
    ("blade_tip", [-1.1, 0.5, -0.85], [2.2, 5.5, 0.85], "blade", "blade_side", [-16, 0, 0], [0, 0, 0]),
    ("blade_tip", [-0.45, 0.0, -0.45], [0.9, 3.0, 0.9], "bevel", "bevel", [0, 45, 0], [0, 1.5, 0]),
    # guard bone: layered. 15 wide main bar, raised center block,
    # underside gold plate, gold end caps, upswept end boxes, and a
    # cyan stone set into both the front and the back face.
    ("guard", [-7.5, 45.5, -1.25], [15.0, 2.5, 2.5], "guard", "guard_side", None, None),
    ("guard", [-2.0, 45.0, -1.75], [4.0, 3.5, 3.5], "guard", "guard_side", None, None),
    ("guard", [-5.5, 44.6, -1.6], [11.0, 0.9, 3.2], "gold", "gold", None, None),
    ("guard", [-7.9, 45.3, -1.45], [1.1, 2.9, 2.9], "gold", "gold", None, None),
    ("guard", [6.8, 45.3, -1.45], [1.1, 2.9, 2.9], "gold", "gold", None, None),
    ("guard", [-1.0, 45.75, -2.3], [2.0, 2.0, 0.8], "gem", "gem", None, None),
    ("guard", [-1.0, 45.75, 1.5], [2.0, 2.0, 0.8], "gem", "gem", None, None),
    ("guard", [-9.2, 46.6, -1.0], [2.6, 2.0, 2.0], "guard", "guard_side", [0, 0, -28], [-7.5, 47.0, 0]),
    ("guard", [6.6, 46.6, -1.0], [2.6, 2.0, 2.0], "guard", "guard_side", [0, 0, 28], [7.5, 47.0, 0]),
    # hilt bone: grip y 48 to 58 as two stacked boxes, gold wrap
    # rings standing proud of the grip, and the gold collar at y 58.
    ("hilt", [-1.5, 48.0, -1.5], [3.0, 5.0, 3.0], "grip", "grip_side", None, None),
    ("hilt", [-1.5, 53.0, -1.5], [3.0, 5.0, 3.0], "grip", "grip_side", None, None),
    ("hilt", [-1.7, 50.3, -1.7], [3.4, 0.7, 3.4], "gold", "gold", None, None),
    ("hilt", [-1.7, 55.3, -1.7], [3.4, 0.7, 3.4], "gold", "gold", None, None),
    ("hilt", [-2.0, 58.0, -2.0], [4.0, 1.5, 4.0], "gold", "gold", None, None),
    # gem bone: pommel gem held in a crown setting of four gold
    # prongs at its corners, plus the gold crown spike to y 66.
    ("gem", [-1.75, 59.5, -1.75], [3.5, 3.5, 3.5], "gem", "gem", None, None),
    ("gem", [1.2, 59.3, 1.2], [0.6, 2.4, 0.6], "gold", "gold", None, None),
    ("gem", [-1.8, 59.3, 1.2], [0.6, 2.4, 0.6], "gold", "gold", None, None),
    ("gem", [1.2, 59.3, -1.8], [0.6, 2.4, 0.6], "gold", "gold", None, None),
    ("gem", [-1.8, 59.3, -1.8], [0.6, 2.4, 0.6], "gold", "gold", None, None),
    ("gem", [-0.75, 63.0, -0.75], [1.5, 3.0, 1.5], "gold", "gold", None, None),
]

# Bone tree: name -> (parent, pivot)
BONES = [
    ("root", None, [0, 0, 0]),
    ("blade", "root", [0, 46, 0]),
    ("blade_tip", "blade", [0, 10, 0]),
    ("guard", "root", [0, 46, 0]),
    ("hilt", "root", [0, 46, 0]),
    ("gem", "hilt", [0, 59.5, 0]),
]

# Clip names, lengths, and loop flags are behavior and stay fixed.
# The motion inside the windows is layered: the root keeps its
# original travel, the blade flexes in micro-motion, the guard sways
# against the root, and the gem pulses scale in a shimmer. Release
# keeps its spin flare to 40 degrees and its lift to y 10, with lag
# in the blade and guard as follow-through.
ANIMATIONS = {
    "descend": {
        "loop": True,
        "animation_length": 0.8,
        "bones": {
            "root": {
                "position": {
                    "0.0": [0, 2.0, 0],
                    "0.4": [0, 0, 0],
                    "0.8": [0, 2.0, 0],
                },
                "rotation": {
                    "0.0": [0, 0, -3],
                    "0.2": [0, 10, 1],
                    "0.4": [0, 22, 3],
                    "0.6": [0, 10, 1],
                    "0.8": [0, 0, -3],
                },
            },
            "blade": {
                "rotation": {
                    "0.0": [0, 0, 0],
                    "0.4": [1.5, 0, 0.5],
                    "0.8": [0, 0, 0],
                }
            },
            "guard": {
                "rotation": {
                    "0.0": [0, 0, 2],
                    "0.4": [0, 0, -2],
                    "0.8": [0, 0, 2],
                }
            },
            "gem": {
                "scale": {
                    "0.0": [1, 1, 1],
                    "0.4": [1.15, 1.15, 1.15],
                    "0.8": [1, 1, 1],
                }
            },
        },
    },
    "hold": {
        "loop": True,
        "animation_length": 2.4,
        "bones": {
            "root": {
                "position": {
                    "0.0": [0, 0, 0],
                    "0.6": [0, 0.3, 0],
                    "1.2": [0, 0.6, 0],
                    "1.8": [0, 0.3, 0],
                    "2.4": [0, 0, 0],
                },
                "rotation": {
                    "0.0": [0, 0, 0],
                    "0.6": [0, 3, 1],
                    "1.2": [0, 6, 0],
                    "1.8": [0, 3, -1],
                    "2.4": [0, 0, 0],
                },
            },
            "blade": {
                "rotation": {
                    "0.0": [0, 0, 0],
                    "1.2": [0.8, 0, 0.6],
                    "2.4": [0, 0, 0],
                }
            },
            "blade_tip": {
                "rotation": {
                    "0.0": [0, 0, 0],
                    "1.2": [1.2, 0, 0],
                    "2.4": [0, 0, 0],
                }
            },
            "guard": {
                "rotation": {
                    "0.0": [0, 0, 0],
                    "0.6": [0, 0, -1.5],
                    "1.2": [0, 0, 0],
                    "1.8": [0, 0, 1.5],
                    "2.4": [0, 0, 0],
                }
            },
            "gem": {
                "scale": {
                    "0.0": [1, 1, 1],
                    "0.6": [1.12, 1.12, 1.12],
                    "1.2": [1, 1, 1],
                    "1.8": [1.18, 1.18, 1.18],
                    "2.4": [1, 1, 1],
                }
            },
        },
    },
    "release": {
        "loop": "hold_on_last_frame",
        "animation_length": 0.5,
        "bones": {
            "root": {
                "position": {
                    "0.0": [0, 0, 0],
                    "0.3": [0, 4, 0],
                    "0.5": [0, 10, 0],
                },
                "rotation": {
                    "0.0": [0, 0, 0],
                    "0.3": [0, 25, 2],
                    "0.5": [0, 40, 0],
                },
            },
            "blade": {
                "rotation": {
                    "0.0": [0, 0, 0],
                    "0.35": [-4, 0, 0],
                    "0.5": [-7, 0, 0],
                }
            },
            "guard": {
                "rotation": {
                    "0.0": [0, 0, 0],
                    "0.5": [0, 0, -6],
                }
            },
            "gem": {
                "scale": {
                    "0.0": [1, 1, 1],
                    "0.25": [1.35, 1.35, 1.35],
                    "0.5": [1.1, 1.1, 1.1],
                }
            },
        },
    },
}


def clamp(v):
    return max(0, min(255, int(round(v))))


def shade(c, k):
    return (clamp(c[0] * k), clamp(c[1] * k), clamp(c[2] * k))


def mix(a, b, t):
    return (clamp(a[0] + (b[0] - a[0]) * t),
            clamp(a[1] + (b[1] - a[1]) * t),
            clamp(a[2] + (b[2] - a[2]) * t))


def vary(x, y, salt=0):
    """Deterministic per-texel variation so faces never read flat."""
    n = (x * 73856093) ^ (y * 19349663) ^ (salt * 83492791)
    n = (n ^ (n >> 13)) & 0xFFFF
    return 0.94 + (n / 65535.0) * 0.12


def mottle(x, y, salt=0):
    """Low frequency tonal drift across a region, crystal style."""
    return 1.0 + 0.045 * math.sin((x * 0.31 + salt * 7.0)) * math.sin(y * 0.23 + salt * 3.0)


def put(px, region, i, j, c, salt):
    x0, y0, w, h = REGIONS[region]
    k = vary(i, j, salt) * mottle(i, j, salt)
    px[x0 + i, y0 + j] = (clamp(c[0] * k), clamp(c[1] * k), clamp(c[2] * k), 255)


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
                "visible_bounds_width": 8,
                "visible_bounds_height": 8,
                "visible_bounds_offset": [0, 2, 0],
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
    # blade: near-white with a real vertical gradient (bright at the
    # guard, deeper cyan toward the tip), bright edge highlight lines
    # with a shadow line just inside them, a shaded fuller channel in
    # the center columns, and dark panel seams with a highlight lip.
    x0, y0, w, h = REGIONS["blade"]
    for j in range(h):
        vgrad = j / max(1, h - 1)
        for i in range(w):
            d = abs(i - (w - 1) / 2.0)
            if i <= 2 or i >= w - 3:
                c = mix((238, 255, 252), (196, 244, 242), vgrad)
            elif i == 3 or i == w - 4:
                c = mix((110, 196, 208), (84, 168, 186), vgrad)
            elif d <= 5.5:
                c = mix((168, 238, 240), CYAN_DEEP, (5.5 - d) / 5.5 * 0.55 + vgrad * 0.2)
                if d <= 1.5:
                    c = mix(c, (225, 255, 251), 0.45)
            else:
                shoulder = min(1.0, (d - 5.5) / 9.0)
                c = mix((198, 240, 241), (238, 251, 250), shoulder)
                c = mix(c, (172, 226, 231), vgrad * 0.5)
            if j % 32 == 31:
                c = shade(CYAN_DARK, 1.15)
            elif j % 32 == 0 and j > 0:
                c = (238, 255, 252)
            put(px, "blade", i, j, c, 1)
    # blade_side: the thin edge steel, cyan grey with a bright spine
    # line and a darker gradient toward the tip rows.
    x0, y0, w, h = REGIONS["blade_side"]
    for j in range(h):
        vgrad = j / max(1, h - 1)
        for i in range(w):
            d = abs(i - (w - 1) / 2.0)
            base = mix((172, 230, 228), (120, 198, 205), vgrad)
            if d <= 3.0:
                base = mix(base, (235, 255, 252), 0.5)
            put(px, "blade_side", i, j, base, 2)
    draw.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], outline=shade(RIM, 1.0))
    # ridge: the raised spine. Bright crystal shoulders, shaded
    # channel walls, and a near-white core line down the middle.
    x0, y0, w, h = REGIONS["ridge"]
    for j in range(h):
        vgrad = j / max(1, h - 1)
        for i in range(w):
            d = abs(i - (w - 1) / 2.0)
            if d <= 2.5:
                c = mix((240, 255, 252), (205, 250, 246), vgrad)
            elif d <= 7.5:
                c = mix((150, 232, 234), CYAN_DEEP, (7.5 - d) / 5.0 * 0.4 + vgrad * 0.25)
            elif d <= 10.5:
                c = shade(CYAN_DARK, 1.25)
            else:
                c = mix((222, 250, 248), (186, 238, 238), vgrad)
            if j % 32 == 31:
                c = shade(CYAN_DARK, 1.1)
            put(px, "ridge", i, j, c, 9)
    # bevel: diamond edge rails. Near-white core with deep cyan
    # flanks, so the cutting edges read as bright forged lines.
    x0, y0, w, h = REGIONS["bevel"]
    for j in range(h):
        vgrad = j / max(1, h - 1)
        for i in range(w):
            d = abs(i - (w - 1) / 2.0)
            if d <= 9.0:
                c = mix((242, 255, 253), (208, 248, 245), vgrad)
            elif d <= 16.0:
                c = mix((168, 238, 238), CYAN_DEEP, (16.0 - d) / 7.0 * 0.35)
            else:
                c = mix(CYAN_DEEP, CYAN_DARK, vgrad * 0.5)
            put(px, "bevel", i, j, c, 10)
    # collar: structured ring band. Deep teal with dark top and
    # bottom rims and a gold stripe band through the middle rows.
    x0, y0, w, h = REGIONS["collar"]
    for j in range(h):
        for i in range(w):
            if j <= 5 or j >= h - 6:
                c = shade(RIM, 0.5)
            elif h // 2 - 5 <= j <= h // 2 + 4:
                c = mix(GOLD, GOLD_DARK, abs(j - h // 2) / 6.0)
            else:
                c = mix(shade(RIM, 0.95), shade(RIM, 0.65), j / h)
            put(px, "collar", i, j, c, 11)
    # guard: deep teal, darker toward the bottom rows, with a gold
    # trim border, an inner panel line, and gold rivets in the field.
    for region, base, salt in (("guard", RIM, 3), ("guard_side", shade(RIM, 0.7), 4)):
        x0, y0, w, h = REGIONS[region]
        for j in range(h):
            for i in range(w):
                c = mix(base, shade(base, 0.62), j / max(1, h - 1))
                put(px, region, i, j, c, salt)
        draw.rectangle([x0 + 3, y0 + 3, x0 + w - 4, y0 + h - 4], outline=GOLD, width=3)
        draw.rectangle([x0 + 9, y0 + 9, x0 + w - 10, y0 + h - 10],
                       outline=shade(RIM, 0.5), width=2)
        draw.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], outline=shade(RIM, 0.45), width=2)
        for rx, ry in ((16, 16), (w - 17, 16), (16, h - 17), (w - 17, h - 17)):
            draw.ellipse([x0 + rx - 3, y0 + ry - 3, x0 + rx + 3, y0 + ry + 3],
                         fill=GOLD, outline=GOLD_DARK)
    # gold: collar, caps, rings, prongs, and crown. Warm metal with
    # a bright top edge, brushed vertical streaks, and a dark base.
    x0, y0, w, h = REGIONS["gold"]
    for j in range(h):
        for i in range(w):
            base = GOLD if j > 5 else shade(GOLD, 1.18)
            streak = 1.0 + 0.05 * math.sin(i * 1.7)
            c = shade(base, streak * (1.0 - 0.18 * (j / max(1, h - 1))))
            put(px, "gold", i, j, c, 5)
    draw.rectangle([x0, y0 + h - 7, x0 + w - 1, y0 + h - 1], fill=GOLD_DARK)
    draw.rectangle([x0, y0 + 10, x0 + w - 1, y0 + 11], fill=shade(GOLD, 1.22))
    # grip: deep teal wrap with diagonal band shading and gold seam
    # rows, so the wrap rings on the geometry sit on a wrapped grip.
    for region, base, salt in (("grip", shade(RIM, 0.82), 6), ("grip_side", shade(RIM, 0.6), 7)):
        x0, y0, w, h = REGIONS[region]
        for j in range(h):
            for i in range(w):
                band = 0.8 if ((i + j) // 8) % 2 == 0 else 1.02
                c = shade(base, band * (1.0 - 0.15 * (j / max(1, h - 1))))
                put(px, region, i, j, c, salt)
        for j in range(7, h, 16):
            draw.line([x0, y0 + j, x0 + w - 1, y0 + j], fill=GOLD_DARK, width=2)
    # gem: faceted crystal. Four facet shades split by the diagonals,
    # deep teal corners, and a bright white core.
    x0, y0, w, h = REGIONS["gem"]
    for j in range(h):
        for i in range(w):
            dx = i - (w - 1) / 2.0
            dy = j - (h - 1) / 2.0
            d = max(abs(dx), abs(dy)) / (w / 2.0)
            if d <= 0.22:
                c = (232, 255, 251)
            elif abs(dx) > abs(dy):
                c = MID if dx > 0 else (140, 240, 234)
            else:
                c = (178, 248, 242) if dy > 0 else (105, 226, 224)
            if d > 0.86:
                c = CYAN_DEEP
            put(px, "gem", i, j, c, 8)
    draw.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], outline=shade(MID, 0.7), width=3)
    draw.line([x0, y0, x0 + w - 1, y0 + h - 1], fill=shade(MID, 0.8), width=2)
    draw.line([x0 + w - 1, y0, x0, y0 + h - 1], fill=shade(MID, 0.8), width=2)


def paint_glow(draw, img):
    """Paint the glowmask: black base, energy accents only. Blade
    edge columns, the edge bevel rails, the fuller core on the ridge
    spine, the blade side steel, and the gem glow cyan-white. The
    blade face, collar rings, guard, gold, and grip stay black."""
    px = img.load()
    glow = (190, 255, 252)
    x0, y0, w, h = REGIONS["blade"]
    for j in range(h):
        for i in range(w):
            if i <= 3 or i >= w - 4:
                px[x0 + i, y0 + j] = (glow[0], glow[1], glow[2], 255)
    x0, y0, w, h = REGIONS["ridge"]
    for j in range(h):
        for i in range(w):
            d = abs(i - (w - 1) / 2.0)
            if d <= 3.5:
                px[x0 + i, y0 + j] = (170, 255, 250, 255)
    x0, y0, w, h = REGIONS["bevel"]
    draw.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], fill=(215, 255, 253))
    x0, y0, w, h = REGIONS["blade_side"]
    for j in range(h):
        for i in range(w):
            d = abs(i - (w - 1) / 2.0)
            if d <= 5.0:
                px[x0 + i, y0 + j] = (120, 235, 230, 255)
    x0, y0, w, h = REGIONS["gem"]
    draw.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], fill=(235, 255, 253))


def build_textures():
    tex_dir = os.path.join(ROOT, "textures", "entity")
    os.makedirs(tex_dir, exist_ok=True)
    img = Image.new("RGBA", (TEX, TEX), (24, 24, 28, 255))
    paint_base(ImageDraw.Draw(img), img)
    # Posterize the color bands to 5 bits per channel: the gradients
    # stay, but the palette lands at a few hundred deliberate colors
    # like the authored family standard instead of per-texel noise
    # running into the thousands. Alpha is kept untouched.
    r, g, b, a = img.split()
    img = Image.merge("RGBA", (
        Image.eval(r, lambda v: (v >> 3) << 3),
        Image.eval(g, lambda v: (v >> 3) << 3),
        Image.eval(b, lambda v: (v >> 3) << 3),
        a))
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
