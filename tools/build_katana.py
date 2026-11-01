#!/usr/bin/env python3
"""
Builds the Phase 1 katana assets for Dragon Block Arcane:
- geckolib/models/item/katana.geo.json (multi-bone: weapon > grip, guard, blade, blade_glow)
- textures/item/katana_geo.png (128x128 atlas)
- textures/item/katana_geo_glowmask.png (only the blade edge region glows)
- geckolib/animations/item/katana.animation.json (per-weapon idle, swing, charge)
- renders/katana_geo_preview.png (side view proof sheet rendered from the cubes)

Design lock is concepts/media-generation-dba-katana-concept-*.webp: bright silver-white
curved blade with a pale energy edge, round silver tsuba, black diamond wrap handle
with white showing through, silver pommel cap.

Run from the repo root: python3 tools/build_katana.py
"""
import json
import os
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/dragonblockarcanedba")

# Atlas regions on the 128x128 texture: name -> (x, y, w, h)
REGIONS = {
    "steel_light": (0, 0, 32, 32),      # blade faces
    "steel_dark": (32, 0, 32, 32),      # blade edges, spine
    "wrap_black": (0, 32, 32, 32),      # handle wrap
    "rayskin_white": (32, 32, 32, 32),  # handle core showing through the wrap
    "silver": (64, 0, 32, 32),          # tsuba, collar
    "glow_edge": (64, 32, 32, 32),      # energy edge (glows via glowmask)
    "silver_dark": (96, 0, 32, 32),     # pommel
}


def face_uv(region):
    x, y, w, h = REGIONS[region]
    return {"uv": [x, y], "uv_size": [w, h]}


def cube(origin, size, main, side=None, rotation=None, pivot=None):
    side = side or main
    c = {
        "origin": origin,
        "size": size,
        "uv": {
            "north": face_uv(main),
            "south": face_uv(main),
            "up": face_uv(side),
            "down": face_uv(side),
            "east": face_uv(side),
            "west": face_uv(side),
        },
    }
    if rotation is not None:
        c["rotation"] = rotation
        c["pivot"] = pivot
    return c


def build_geo():
    grip = [
        # handle core (white rayskin showing between the wrap bands)
        cube([-0.9, -7.5, -0.9], [1.8, 9.5, 1.8], "rayskin_white"),
        # pommel cap
        cube([-1.1, -8.6, -1.1], [2.2, 1.1, 2.2], "silver_dark"),
    ]
    # black wrap bands over the core, leaving white diamonds of core visible in the gaps
    for y in (-6.9, -5.0, -3.1, -1.2, 0.7):
        grip.append(cube([-1.02, y, -1.02], [2.04, 1.0, 2.04], "wrap_black"))

    guard = [
        # tsuba: two crossed boxes read as a round disc at game scale
        cube([-3.2, 1.7, -2.3], [6.4, 0.6, 4.6], "silver"),
        cube([-2.3, 1.7, -3.2], [4.6, 0.6, 6.4], "silver"),
        # habaki collar above the guard
        cube([-1.2, 2.3, -1.2], [2.4, 1.6, 2.4], "silver"),
    ]

    # blade segments with a gentle curve (x drifts as y rises), tapering toward the tip
    blade = [
        cube([-1.1, 3.9, -0.28], [2.2, 6.0, 0.55], "steel_light", "steel_dark"),
        cube([-0.95, 9.9, -0.28], [2.05, 6.0, 0.55], "steel_light", "steel_dark"),
        cube([-0.75, 15.9, -0.28], [1.9, 5.5, 0.55], "steel_light", "steel_dark"),
        cube([-0.45, 21.4, -0.26], [1.55, 3.4, 0.5], "steel_light", "steel_dark",
             rotation=[0, 0, -14], pivot=[0.3, 21.4, 0]),
    ]

    # energy edge strip riding the cutting (left) edge of each segment
    glow = [
        cube([-1.38, 3.9, -0.3], [0.5, 6.0, 0.6], "glow_edge"),
        cube([-1.23, 9.9, -0.3], [0.5, 6.0, 0.6], "glow_edge"),
        cube([-1.03, 15.9, -0.3], [0.5, 5.5, 0.6], "glow_edge"),
        cube([-0.78, 21.4, -0.28], [0.45, 3.2, 0.55], "glow_edge",
             rotation=[0, 0, -14], pivot=[0.3, 21.4, 0]),
    ]

    bones = [
        {"name": "weapon", "pivot": [0, 2, 0], "cubes": []},
        {"name": "grip", "parent": "weapon", "pivot": [0, 2, 0], "cubes": grip},
        {"name": "guard", "parent": "weapon", "pivot": [0, 2, 0], "cubes": guard},
        {"name": "blade", "parent": "weapon", "pivot": [0, 2, 0], "cubes": blade},
        {"name": "blade_glow", "parent": "weapon", "pivot": [0, 2, 0], "cubes": glow},
    ]
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [
            {
                "description": {
                    "identifier": "geometry.katana",
                    "texture_width": 128,
                    "texture_height": 128,
                    "visible_bounds_width": 4,
                    "visible_bounds_height": 4,
                    "visible_bounds_offset": [0, 1, 0],
                },
                "bones": bones,
            }
        ],
    }


def paint_atlas():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 255))
    d = ImageDraw.Draw(img)

    def region(name, fn):
        x, y, w, h = REGIONS[name]
        fn(x, y, w, h)

    def steel_light(x, y, w, h):
        for i in range(h):
            t = i / h
            c = (int(228 - 40 * t), int(238 - 34 * t), int(248 - 28 * t), 255)
            d.line([(x, y + i), (x + w - 1, y + i)], fill=c)
        d.line([(x + 4, y), (x + 4, y + h - 1)], fill=(255, 255, 255, 255))

    def steel_dark(x, y, w, h):
        d.rectangle([x, y, x + w - 1, y + h - 1], fill=(120, 134, 148, 255))

    def wrap_black(x, y, w, h):
        d.rectangle([x, y, x + w - 1, y + h - 1], fill=(16, 14, 20, 255))
        for i in range(0, w, 6):
            d.line([(x + i, y), (x + i, y + h - 1)], fill=(34, 30, 42, 255))

    def rayskin(x, y, w, h):
        d.rectangle([x, y, x + w - 1, y + h - 1], fill=(232, 234, 238, 255))
        for i in range(4, w, 8):
            for j in range(4, h, 8):
                d.point((x + i, y + j), fill=(200, 204, 210, 255))

    def silver(x, y, w, h):
        d.rectangle([x, y, x + w - 1, y + h - 1], fill=(186, 194, 204, 255))
        d.rectangle([x + 6, y + 6, x + w - 7, y + h - 7], outline=(140, 150, 162, 255))
        d.rectangle([x + 11, y + 11, x + w - 12, y + h - 12], outline=(214, 220, 228, 255))

    def glow_edge(x, y, w, h):
        d.rectangle([x, y, x + w - 1, y + h - 1], fill=(214, 242, 255, 255))
        d.rectangle([x, y, x + 5, y + h - 1], fill=(255, 255, 255, 255))

    def silver_dark(x, y, w, h):
        d.rectangle([x, y, x + w - 1, y + h - 1], fill=(104, 112, 124, 255))

    region("steel_light", steel_light)
    region("steel_dark", steel_dark)
    region("wrap_black", wrap_black)
    region("rayskin_white", rayskin)
    region("silver", silver)
    region("glow_edge", glow_edge)
    region("silver_dark", silver_dark)
    return img


def paint_glowmask():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    x, y, w, h = REGIONS["glow_edge"]
    d.rectangle([x, y, x + w - 1, y + h - 1], fill=(235, 250, 255, 255))
    x, y, w, h = REGIONS["silver"]
    d.rectangle([x + 11, y + 11, x + w - 12, y + h - 12], outline=(60, 60, 60, 255))
    return img


def build_animations():
    return {
        "format_version": "1.8.0",
        "animations": {
            "idle": {
                "loop": True,
                "animation_length": 3.0,
                "bones": {
                    "weapon": {
                        "rotation": {"0.0": [0, 0, 0], "1.5": [0, 0, 1.5], "3.0": [0, 0, 0]},
                        "position": {"0.0": [0, 0, 0], "1.5": [0, 0.25, 0], "3.0": [0, 0, 0]},
                    },
                    "blade_glow": {
                        "scale": {"0.0": [1, 1, 1], "1.5": [1.06, 1, 1.06], "3.0": [1, 1, 1]}
                    },
                },
            },
            "swing": {
                "loop": True,
                "animation_length": 0.35,
                "bones": {
                    "weapon": {
                        "rotation": {"0.0": [0, 0, 0], "0.1": [0, 0, 26], "0.25": [0, 0, -18], "0.35": [0, 0, 0]},
                        "scale": {"0.0": [1, 1, 1], "0.1": [1.04, 1.04, 1.04], "0.35": [1, 1, 1]},
                    },
                    "blade_glow": {
                        "scale": {"0.0": [1, 1, 1], "0.1": [1.25, 1, 1.25], "0.35": [1, 1, 1]}
                    },
                },
            },
            "charge": {
                "loop": True,
                "animation_length": 0.8,
                "bones": {
                    "weapon": {
                        "rotation": {"0.0": [0, 0, -7], "0.2": [0.6, 0, -6], "0.4": [-0.6, 0, -8], "0.6": [0.6, 0, -6], "0.8": [0, 0, -7]},
                        "position": {"0.0": [0, 0.3, 0], "0.8": [0, 0.3, 0]},
                    },
                    "blade_glow": {
                        "scale": {"0.0": [1, 1, 1], "0.4": [1.35, 1, 1.35], "0.8": [1, 1, 1]}
                    },
                    "blade": {
                        "rotation": {"0.0": [0, 0, 0], "0.2": [0, 0, 0.5], "0.4": [0, 0, -0.5], "0.6": [0, 0, 0.5], "0.8": [0, 0, 0]}
                    },
                },
            },
        },
    }


def render_preview(geo, atlas):
    """Side view (looking along Z) of every cube, colored from its north-face UV center."""
    cubes = []
    for bone in geo["minecraft:geometry"][0]["bones"]:
        for c in bone["cubes"]:
            cubes.append(c)
    min_x = min(c["origin"][0] for c in cubes)
    max_x = max(c["origin"][0] + c["size"][0] for c in cubes)
    min_y = min(c["origin"][1] for c in cubes)
    max_y = max(c["origin"][1] + c["size"][1] for c in cubes)
    scale = 14
    pad = 10
    w = int((max_x - min_x) * scale) + pad * 2
    h = int((max_y - min_y) * scale) + pad * 2
    img = Image.new("RGBA", (w, h), (18, 18, 24, 255))
    d = ImageDraw.Draw(img)
    for c in sorted(cubes, key=lambda c: c["origin"][2]):
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


def main():
    geo = build_geo()
    atlas = paint_atlas()
    glow = paint_glowmask()
    anims = build_animations()

    geo_path = os.path.join(ASSETS, "geckolib/models/item/katana.geo.json")
    tex_path = os.path.join(ASSETS, "textures/item/katana_geo.png")
    glow_path = os.path.join(ASSETS, "textures/item/katana_geo_glowmask.png")
    anim_path = os.path.join(ASSETS, "geckolib/animations/item/katana.animation.json")
    preview_path = os.path.join(ROOT, "renders/katana_geo_preview.png")

    with open(geo_path, "w") as f:
        json.dump(geo, f, indent=2)
    atlas.save(tex_path)
    glow.save(glow_path)
    with open(anim_path, "w") as f:
        json.dump(anims, f, indent=2)
    render_preview(geo, atlas).save(preview_path)
    print("wrote", geo_path)
    print("wrote", tex_path)
    print("wrote", glow_path)
    print("wrote", anim_path)
    print("wrote", preview_path)


if __name__ == "__main__":
    main()
