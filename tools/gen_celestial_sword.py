#!/usr/bin/env python3
"""Generate the celestial_sword entity model for Spirit Impale.

Six of these plant tip-first around a ground seal. The sword is built
pointing DOWN: model origin (0, 0, 0) is the plant point at the tip and
the sword rises +Y from there. Total height is 66 units (about 4.1
blocks): blade about 46 long, guard at y 46, grip and pommel above.

Palette (Spirit): core #FFFFFF, mid #4DE8E0, rim #0E7490. Guard and
hilt are deep teal with gold trim; the blade is near-white with a pale
cyan center fuller stripe. The glowmask carries only the blade edge
columns, the fuller stripe, and the gem in cyan-white; every metal
part stays black on the mask.

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
import os

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src", "main", "resources", "assets", "dragonblockarcanedba")
NAME = "celestial_sword"
TEX = 128

# Palette
CORE = (255, 255, 255)        # #FFFFFF
MID = (77, 232, 224)          # #4DE8E0
RIM = (14, 116, 144)          # #0E7490
GOLD = (216, 178, 72)
GOLD_DARK = (150, 112, 36)

# UV regions on the 128x128 atlas: name -> (x, y, w, h). Every cube face
# maps to one full region, exactly like the migrated weapon pipeline.
REGIONS = {
    "blade": (0, 0, 32, 32),
    "blade_side": (32, 0, 32, 32),
    "guard": (64, 0, 32, 32),
    "guard_side": (96, 0, 32, 32),
    "gold": (0, 32, 32, 32),
    "grip": (32, 32, 32, 32),
    "grip_side": (64, 32, 32, 32),
    "gem": (96, 32, 32, 32),
}

# Cube layout as data: (bone, origin, size, main region, side region,
# rotation, pivot). Origin is the low corner in absolute model units,
# +Z is the model front, +Y is up. Broad blade faces are north/south.
CUBES = [
    # blade bone: 4 stacked segments tapering from width 5.5 at the
    # guard line (y 46) down to width 4.2 near the tip (y 10).
    ("blade", [-2.75, 37.0, -1.0], [5.5, 9.0, 2.0], "blade", "blade_side", None, None),
    ("blade", [-2.55, 28.0, -1.0], [5.1, 9.0, 2.0], "blade", "blade_side", None, None),
    ("blade", [-2.3, 19.0, -0.9], [4.6, 9.0, 1.8], "blade", "blade_side", None, None),
    ("blade", [-2.1, 10.0, -0.8], [4.2, 9.0, 1.6], "blade", "blade_side", None, None),
    # blade_tip bone: forged point. A center block plus two wedges
    # rotated about the plant point so the point reads as forged,
    # not as a staircase.
    ("blade_tip", [-1.7, 4.0, -0.7], [3.4, 6.5, 1.4], "blade", "blade_side", None, None),
    ("blade_tip", [-1.7, 0.0, -0.6], [1.7, 5.0, 1.2], "blade", "blade_side", [0, 0, 18], [0, 0, 0]),
    ("blade_tip", [0.0, 0.0, -0.6], [1.7, 5.0, 1.2], "blade", "blade_side", [0, 0, -18], [0, 0, 0]),
    # guard bone: 15 wide horizontal bar, center block, small cyan
    # stone on the front face, and upswept end boxes.
    ("guard", [-7.5, 45.5, -1.25], [15.0, 2.5, 2.5], "guard", "guard_side", None, None),
    ("guard", [-2.0, 45.0, -1.75], [4.0, 3.5, 3.5], "guard", "guard_side", None, None),
    ("guard", [-1.0, 45.75, -2.3], [2.0, 2.0, 0.8], "gem", "gem", None, None),
    ("guard", [-9.2, 46.6, -1.0], [2.6, 2.0, 2.0], "guard", "guard_side", [0, 0, -28], [-7.5, 47.0, 0]),
    ("guard", [6.6, 46.6, -1.0], [2.6, 2.0, 2.0], "guard", "guard_side", [0, 0, 28], [7.5, 47.0, 0]),
    # hilt bone: grip y 48 to 58 as two stacked boxes, gold collar.
    ("hilt", [-1.5, 48.0, -1.5], [3.0, 5.0, 3.0], "grip", "grip_side", None, None),
    ("hilt", [-1.5, 53.0, -1.5], [3.0, 5.0, 3.0], "grip", "grip_side", None, None),
    ("hilt", [-2.0, 58.0, -2.0], [4.0, 1.5, 4.0], "gold", "gold", None, None),
    # gem bone: pommel gem and a small gold crown spike to y 66.
    ("gem", [-1.75, 59.5, -1.75], [3.5, 3.5, 3.5], "gem", "gem", None, None),
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

ANIMATIONS = {
    "descend": {
        "loop": True,
        "animation_length": 0.8,
        "bones": {
            "root": {
                "rotation": {
                    "0.0": [0, 0, -2],
                    "0.4": [0, 12, 2],
                    "0.8": [0, 0, -2],
                }
            }
        },
    },
    "hold": {
        "loop": True,
        "animation_length": 2.4,
        "bones": {
            "root": {
                "position": {
                    "0.0": [0, 0, 0],
                    "1.2": [0, 0.6, 0],
                    "2.4": [0, 0, 0],
                },
                "rotation": {
                    "0.0": [0, 0, 0],
                    "1.2": [0, 6, 0],
                    "2.4": [0, 0, 0],
                },
            }
        },
    },
    "release": {
        "loop": "hold_on_last_frame",
        "animation_length": 0.5,
        "bones": {
            "root": {
                "position": {
                    "0.0": [0, 0, 0],
                    "0.5": [0, 10, 0],
                },
                "rotation": {
                    "0.0": [0, 0, 0],
                    "0.5": [0, 40, 0],
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
    return 0.94 + (n / 65535.0) * 0.12


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
    # blade: near-white, pale cyan fuller stripe in the center columns,
    # bright edge columns, slight vertical gradient.
    x0, y0, w, h = REGIONS["blade"]
    for j in range(h):
        grad = 1.0 - 0.08 * (j / max(1, h - 1))
        for i in range(w):
            if i <= 2 or i >= w - 3:
                c = shade((232, 252, 250), grad)
            elif abs(i - (w - 1) / 2.0) <= 2.5:
                c = shade((178, 245, 240), grad)
            else:
                c = shade((248, 253, 253), grad)
            v = vary(i, j, 1)
            px[x0 + i, y0 + j] = (clamp(c[0] * v), clamp(c[1] * v), clamp(c[2] * v), 255)
    draw.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], outline=shade(MID, 0.75))
    # blade_side: the thin edge steel, pale cyan grey.
    x0, y0, w, h = REGIONS["blade_side"]
    for j in range(h):
        for i in range(w):
            c = shade((168, 226, 224), vary(i, j, 2))
            px[x0 + i, y0 + j] = (c[0], c[1], c[2], 255)
    draw.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], outline=shade(RIM, 1.0))
    # guard: deep teal with a gold trim border.
    for region, base, salt in (("guard", RIM, 3), ("guard_side", shade(RIM, 0.7), 4)):
        x0, y0, w, h = REGIONS[region]
        for j in range(h):
            for i in range(w):
                c = shade(base, vary(i, j, salt))
                px[x0 + i, y0 + j] = (c[0], c[1], c[2], 255)
        draw.rectangle([x0 + 2, y0 + 2, x0 + w - 3, y0 + h - 3], outline=GOLD, width=2)
        draw.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], outline=shade(RIM, 0.55))
    # gold: collar and crown, warm metal with a bright top edge.
    x0, y0, w, h = REGIONS["gold"]
    for j in range(h):
        for i in range(w):
            base = GOLD if j > 3 else shade(GOLD, 1.18)
            c = shade(base, vary(i, j, 5))
            px[x0 + i, y0 + j] = (c[0], c[1], c[2], 255)
    draw.rectangle([x0, y0 + h - 4, x0 + w - 1, y0 + h - 1], fill=GOLD_DARK)
    # grip: deep teal wrap with darker diagonal-feel bands (rows).
    for region, base, salt in (("grip", shade(RIM, 0.82), 6), ("grip_side", shade(RIM, 0.6), 7)):
        x0, y0, w, h = REGIONS[region]
        for j in range(h):
            band = 0.82 if (j // 5) % 2 == 0 else 1.0
            for i in range(w):
                c = shade(base, vary(i, j, salt) * band)
                px[x0 + i, y0 + j] = (c[0], c[1], c[2], 255)
        for j in range(4, h, 10):
            draw.line([x0, y0 + j, x0 + w - 1, y0 + j], fill=GOLD_DARK)
    # gem: bright cyan with a white core and faceted corners.
    x0, y0, w, h = REGIONS["gem"]
    for j in range(h):
        for i in range(w):
            d = max(abs(i - (w - 1) / 2.0), abs(j - (h - 1) / 2.0)) / (w / 2.0)
            c = MID if d > 0.45 else (205, 255, 250)
            cc = shade(c, vary(i, j, 8))
            px[x0 + i, y0 + j] = (cc[0], cc[1], cc[2], 255)
    draw.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], outline=shade(MID, 0.7), width=2)


def paint_glow(draw, img):
    """Paint the glowmask: black base, only blade edges, the fuller
    stripe, and the gem glow cyan-white. Metal stays black."""
    px = img.load()
    glow = (190, 255, 252)
    x0, y0, w, h = REGIONS["blade"]
    for j in range(h):
        for i in range(w):
            if i <= 2 or i >= w - 3:
                px[x0 + i, y0 + j] = (glow[0], glow[1], glow[2], 255)
            elif abs(i - (w - 1) / 2.0) <= 2.5:
                px[x0 + i, y0 + j] = (140, 255, 248, 255)
    x0, y0, w, h = REGIONS["blade_side"]
    for j in range(h):
        for i in range(w):
            px[x0 + i, y0 + j] = (120, 235, 230, 255)
    x0, y0, w, h = REGIONS["gem"]
    draw.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], fill=(235, 255, 253))


def build_textures():
    tex_dir = os.path.join(ROOT, "textures", "entity")
    os.makedirs(tex_dir, exist_ok=True)
    img = Image.new("RGBA", (TEX, TEX), (24, 24, 28, 255))
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
