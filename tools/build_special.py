#!/usr/bin/env python3
"""
Phase 1 special builder for Dragon Block Arcane: Whis Staff (silver shaft, gold ring,
black orb with a cyan core), Bansho Fan (green leaf, red rim, red handle), Blaster Gun
(energy pistol). Same asset conventions as the other builders.

Run from the repo root: python3 tools/build_special.py [weapon_id ...]
"""
import json
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
        "pal": {"blade_light": (16, 16, 22), "blade_dark": (8, 8, 12),
                "grip_main": (224, 228, 234), "grip_second": (180, 186, 196),
                "metal": (206, 168, 56), "glow": (72, 196, 240),
                "metal_dark": (148, 118, 42), "accent": (232, 196, 96)},
    },
    "bansho_fan": {
        "kind": "fan", "pommel": "ball", "swing_len": 0.42, "charge_tilt": -9, "bounds": 9,
        "pal": {"blade_light": (18, 124, 58), "blade_dark": (8, 72, 36),
                "grip_main": (168, 36, 36), "grip_second": (110, 24, 24),
                "metal": (196, 154, 64), "glow": (210, 32, 36),
                "metal_dark": (120, 22, 24), "accent": (200, 28, 32)},
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
    # Cyan core proud of both faces of the black orb, so it reads from the side.
    g.append(cube([-0.55, 16.7, -2.15], [1.1, 0.95, 4.3], "glow"))
    return bones_cubes


def fan(spec):
    """Green leaf with a red rim and a short red handle.

    The icon is the leaf-shaped Bansho Fan, not a gold-ribbed folding sensu.
    Plates are thick enough to read as a fan from the side. Handle crosses y=0.
    """
    bones_cubes = {"shaft": [], "head": [], "head_glow": []}
    s = bones_cubes["shaft"]
    s.append(cube([-0.55, -6.0, -0.55], [1.1, 8.0, 1.1], "grip_main"))
    s.append(cube([-0.72, -6.9, -0.72], [1.44, 1.05, 1.44], "accent"))
    s.append(cube([-0.75, 1.55, -0.75], [1.5, 0.85, 1.5], "metal"))
    # Small red bud beside the handle, as on the icon.
    s.append(cube([0.85, -1.6, -0.32], [0.7, 1.35, 0.64], "accent"))
    h = bones_cubes["head"]
    g = bones_cubes["head_glow"]
    h.append(cube([-0.32, 2.3, -0.2], [0.64, 12.2, 0.4], "blade_dark"))
    rows = (
        (2.5, 1.7, 0.42),
        (4.0, 3.1, 0.38),
        (5.6, 4.5, 0.34),
        (7.2, 5.0, 0.32),
        (8.8, 4.5, 0.3),
        (10.4, 3.4, 0.28),
        (11.8, 2.15, 0.26),
        (13.0, 1.05, 0.24),
    )
    for y, hw, th in rows:
        h.append(cube([-hw, y, -th / 2], [hw * 2, 1.65, th], "blade_light", "blade_dark"))
        h.append(cube([-hw - 0.32, y, -th / 2 - 0.04], [0.4, 1.65, th + 0.08], "accent"))
        h.append(cube([hw - 0.08, y, -th / 2 - 0.04], [0.4, 1.65, th + 0.08], "accent"))
    h.append(cube([-0.42, 14.35, -0.32], [0.84, 1.25, 0.64], "accent"))
    g.append(cube([-0.22, 14.55, -0.16], [0.44, 0.7, 0.32], "glow"))
    return bones_cubes


def blaster(spec):
    """Frieza Force / early-Z ray gun.

    Same upright block layout as the other signature weapons: barrel along +Y.
    GeckoLib holds the bedrock origin, so the grip is built through y=0.
    The weapon bone pivot stays at [0, 2, 0] with the other weapons; the grip's
    own pivot is the top of the handle, where it meets the receiver.
    """
    bones_cubes = {"shaft": [], "head": [], "head_glow": []}
    b = bones_cubes["shaft"]
    grip_pivot = [0, 2.0, 0]
    # Thick black grip. The unrotated shaft crosses the hand at y=0, then kicks back.
    b.append(cube([-0.88, -3.35, -0.78], [1.76, 5.45, 1.56], "grip_main",
                  rotation=[0, 0, 16], pivot=grip_pivot))
    b.append(cube([-1.02, -4.05, -0.9], [2.04, 0.85, 1.8], "grip_second",
                  rotation=[0, 0, 16], pivot=grip_pivot))
    # Short thick receiver: dark gray belly, light gray upper housing.
    b.append(cube([-1.7, 1.5, -1.15], [3.4, 1.9, 2.3], "metal_dark", "blade_dark"))
    b.append(cube([-1.6, 3.2, -1.05], [3.2, 2.85, 2.1], "blade_light", "metal"))
    b.append(cube([-1.15, 3.55, 1.02], [2.15, 1.7, 0.16], "metal_dark"))
    b.append(cube([-1.15, 3.55, -1.18], [2.15, 1.7, 0.16], "metal_dark"))
    # Low top housing and a small rear sight, on the back of the receiver.
    b.append(cube([-2.2, 3.45, -0.42], [0.7, 2.15, 0.84], "metal"))
    b.append(cube([-2.35, 5.35, -0.3], [0.55, 0.42, 0.6], "metal_dark"))
    # Open trigger in the crook ahead of the grip. A lever, not a modern guard.
    b.append(cube([0.78, 0.55, -0.24], [0.42, 1.55, 0.48], "metal_dark"))
    h = bones_cubes["head"]
    # Heavy barrel socketed into the receiver, shorter than the body, then a wider muzzle.
    h.append(cube([-1.05, 5.2, -0.85], [2.1, 3.4, 1.7], "blade_dark", "metal_dark"))
    h.append(cube([-1.28, 8.25, -1.02], [2.56, 0.85, 2.04], "metal", "metal_dark"))
    h.append(cube([-0.5, 8.85, -0.42], [1.0, 0.5, 0.84], "metal_dark"))
    g = bones_cubes["head_glow"]
    g.append(cube([-0.22, 9.15, -0.18], [0.44, 0.32, 0.36], "glow"))
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
