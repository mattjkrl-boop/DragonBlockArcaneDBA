#!/usr/bin/env python3
"""Geo, texture, animation, and icon pass for the war mace, spike flail, and lava sword.

Same atlas regions, bone pivot, and icon outline as tools/build_swords.py and tools/icons.py.
The lava blade is a stack of frames: the core scrolls, the cutting edge does not.
"""
import json
import math
import os
import random

from PIL import Image, ImageDraw, ImageFilter

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


def cube(origin, size, main, side=None):
    side = side or main
    return {"origin": [round(v, 3) for v in origin], "size": [round(v, 3) for v in size], "uv": {
        "north": face_uv(main), "south": face_uv(main),
        "up": face_uv(side), "down": face_uv(side),
        "east": face_uv(side), "west": face_uv(side)}}


def geo(wid, bones):
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {
            "identifier": "geometry." + wid,
            "texture_width": TEX,
            "texture_height": TEX,
            "visible_bounds_width": 5,
            "visible_bounds_height": 5,
            "visible_bounds_offset": [0, 1.5, 0],
        },
        "bones": bones,
    }]}


def bone(name, cubes, parent="weapon"):
    entry = {"name": name, "pivot": [0, 2, 0], "cubes": cubes}
    if parent:
        entry["parent"] = parent
    return entry


def shade(base, f):
    return tuple(max(0, min(255, int(v * f))) for v in base) + (255,)


def paint(pal, glowmask=False, seed=1):
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0 if glowmask else 255))
    draw = ImageDraw.Draw(img)
    rng = random.Random(seed)
    for name, (x, y, w, h) in REGIONS.items():
        base = pal[name]
        if glowmask:
            if name in ("glow", "accent"):
                draw.rectangle([x, y, x + w - 1, y + h - 1], fill=base + (255,))
            continue
        grad = 0.12 if name.startswith("blade") or name == "glow" else 0.18
        for i in range(h):
            t = i / h
            draw.line([(x, y + i), (x + w - 1, y + i)], fill=shade(base, 1.0 + grad / 2 - grad * t))
        for _ in range(70):
            px, py = x + rng.randrange(w), y + rng.randrange(h)
            draw.point((px, py), fill=shade(base, rng.choice((0.88, 1.12))))
        if name in ("metal", "metal_dark"):
            for _ in range(18):
                sy = y + rng.randrange(h)
                draw.line([(x, sy), (x + w - 1, sy)], fill=shade(base, rng.choice((0.9, 1.08))))
        if name == "grip_main":
            for i in range(-h, w, 8):
                draw.line([(x + i, y), (x + i + h, y + h - 1)], fill=shade(base, 0.72))
                draw.line([(x + i, y + h - 1), (x + i + h, y)], fill=shade(base, 1.16))
        if name == "glow":
            draw.rectangle([x, y, x + 6, y + h - 1], fill=(255, 244, 210, 255))
    return img


def scroll_core(img, shift):
    """Move molten streaks through the blade core. Edge and metal regions stay put."""
    out = img.copy()
    for name in ("blade_light", "blade_dark"):
        x, y, w, h = REGIONS[name]
        tile = img.crop((x, y, x + w, y + h))
        moved = Image.new("RGBA", (w, h))
        moved.paste(tile, (0, -(shift % h)))
        moved.paste(tile, (0, h - (shift % h)))
        out.paste(moved, (x, y))
        draw = ImageDraw.Draw(out)
        streak = (y + (shift * 3) % h)
        base = (255, 176, 48, 255) if name == "blade_light" else (120, 36, 12, 255)
        draw.rectangle([x + 8, streak, x + w - 9, min(y + h - 1, streak + 5)], fill=base)
    return out


def animations(swing_len, tilt):
    return {"format_version": "1.8.0", "animations": {
        "idle": {"loop": True, "animation_length": 3.0, "bones": {
            "weapon": {
                "rotation": {"0.0": [0, 0, 0], "1.5": [0, 0, 1.2], "3.0": [0, 0, 0]},
                "position": {"0.0": [0, 0, 0], "1.5": [0, 0.2, 0], "3.0": [0, 0, 0]},
            }}},
        "swing": {"loop": True, "animation_length": swing_len, "bones": {
            "weapon": {
                "rotation": {"0.0": [0, 0, 0], "0.12": [18, 0, 20], str(round(swing_len * 0.55, 2)): [-28, 0, -8], str(swing_len): [0, 0, 0]},
            }}},
        "charge": {"loop": True, "animation_length": 0.8, "bones": {
            "weapon": {
                "rotation": {"0.0": [tilt, 0, 0], "0.4": [tilt - 4, 0, 2], "0.8": [tilt, 0, 0]},
                "position": {"0.0": [0, 0.15, 0], "0.8": [0, 0.15, 0]},
            },
            "blade": {"rotation": {"0.0": [0, 0, 0], "0.4": [0, 6, 0], "0.8": [0, 0, 0]}},
        }},
    }}


def write_geo_pack(wid, bones, pal, seed, swing_len, tilt, frames=1):
    model = geo(wid, bones)
    path = os.path.join(ASSETS, "geckolib/models/item", wid + ".geo.json")
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(model, handle, indent=2)
    base = paint(pal, seed=seed)
    glow = paint(pal, glowmask=True, seed=seed)
    anim = os.path.join(ASSETS, "geckolib/animations/item", wid + ".animation.json")
    with open(anim, "w", encoding="utf-8") as handle:
        json.dump(animations(swing_len, tilt), handle, indent=2)
    tex_dir = os.path.join(ASSETS, "textures/item")
    if frames <= 1:
        base.save(os.path.join(tex_dir, wid + "_geo.png"))
        glow.save(os.path.join(tex_dir, wid + "_geo_glowmask.png"))
        return
    for i in range(frames):
        frame = scroll_core(base, i * 11)
        frame.save(os.path.join(tex_dir, f"{wid}_geo_f{i}.png"))
        glow.save(os.path.join(tex_dir, f"{wid}_geo_f{i}_glowmask.png"))
    base.save(os.path.join(tex_dir, wid + "_geo.png"))
    glow.save(os.path.join(tex_dir, wid + "_geo_glowmask.png"))


def item_models(wid):
    handheld = {"parent": "minecraft:item/handheld", "textures": {"layer0": "dragonblockarcanedba:item/" + wid}}
    for folder, name in (("models/item", wid + ".json"), ("models/item", wid + "_geo_base.json")):
        with open(os.path.join(ASSETS, folder, name), "w", encoding="utf-8") as handle:
            json.dump(handheld, handle, indent=2)
    definition = {
        "model": {
            "type": "minecraft:select",
            "property": "minecraft:display_context",
            "cases": [{
                "when": ["gui", "fixed", "ground"],
                "model": {"type": "minecraft:model", "model": "dragonblockarcanedba:item/" + wid},
            }],
            "fallback": {
                "type": "minecraft:special",
                "base": "dragonblockarcanedba:item/" + wid + "_geo_base",
                "model": {"type": "geckolib:geckolib"},
            },
        }
    }
    with open(os.path.join(ASSETS, "items", wid + ".json"), "w", encoding="utf-8") as handle:
        json.dump(definition, handle, indent=2)


def outline(im):
    alpha = im.split()[3].point(lambda v: 255 if v > 40 else 0)
    grown = alpha.filter(ImageFilter.MaxFilter(3))
    rim = Image.new("RGBA", im.size, (20, 16, 24, 255))
    base = Image.new("RGBA", im.size, (0, 0, 0, 0))
    base.paste(rim, mask=grown)
    base.alpha_composite(im)
    return base


def icon_mace():
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    haft = (92, 64, 40, 255)
    wrap = (48, 32, 24, 255)
    steel = (168, 172, 178, 255)
    dark = (78, 82, 90, 255)
    gold = (196, 148, 54, 255)
    d.polygon([(30, 58), (34, 58), (36, 18), (28, 18)], fill=haft)
    d.line([(31, 54), (32, 20)], fill=(214, 176, 120, 255), width=1)
    for y in range(22, 52, 6):
        d.rectangle([29, y, 35, y + 2], fill=wrap)
    d.ellipse([24, 54, 40, 62], fill=gold)
    d.rounded_rectangle([16, 14, 48, 28], radius=4, fill=steel, outline=dark)
    d.rounded_rectangle([12, 17, 20, 26], radius=2, fill=dark)
    d.rounded_rectangle([44, 17, 52, 26], radius=2, fill=dark)
    d.rectangle([20, 16, 44, 18], fill=gold)
    d.rectangle([20, 24, 44, 26], fill=gold)
    d.polygon([(22, 14), (42, 14), (38, 8), (26, 8)], fill=steel, outline=dark)
    d.polygon([(28, 8), (36, 8), (32, 4)], fill=gold)
    return outline(im)


def icon_flail():
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    haft = (70, 48, 32, 255)
    iron = (150, 154, 162, 255)
    dark = (54, 56, 64, 255)
    d.polygon([(14, 58), (22, 58), (26, 36), (16, 36)], fill=haft)
    d.ellipse([12, 54, 24, 62], fill=(186, 140, 48, 255))
    pts = []
    for i in range(8):
        t = i / 7
        x = 22 + math.sin(t * math.pi) * 16
        y = 36 - math.sin(t * math.pi * 0.5) * 22
        pts.append((x, y))
        r = 2.4 if i % 2 == 0 else 1.7
        d.ellipse([x - r, y - r, x + r, y + r], fill=iron, outline=dark)
    d.line(pts, fill=dark, width=2)
    bx, by, br = 40, 14, 9
    d.ellipse([bx - br, by - br, bx + br, by + br], fill=(120, 124, 132, 255), outline=dark)
    d.ellipse([bx - 4, by - 4, bx + 3, by + 3], fill=(210, 214, 220, 255))
    for ang in range(0, 360, 60):
        a = math.radians(ang)
        x0, y0 = bx + math.cos(a) * 6, by + math.sin(a) * 6
        x1, y1 = bx + math.cos(a) * 13, by + math.sin(a) * 13
        d.polygon([
            (x0 + math.cos(a + 1.2) * 2, y0 + math.sin(a + 1.2) * 2),
            (x0 + math.cos(a - 1.2) * 2, y0 + math.sin(a - 1.2) * 2),
            (x1, y1),
        ], fill=(196, 198, 204, 255))
    return outline(im)


def icon_lava_frame(shift):
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    grip = (48, 28, 24, 255)
    gold = (212, 150, 42, 255)
    d.polygon([(28, 62), (36, 62), (35, 40), (29, 40)], fill=grip)
    d.rectangle([24, 38, 40, 42], fill=gold)
    d.polygon([(18, 40), (22, 36), (22, 44)], fill=gold)
    d.polygon([(46, 40), (42, 36), (42, 44)], fill=gold)
    blade = [(30, 40), (34, 40), (40, 8), (32, 2), (24, 8)]
    d.polygon(blade, fill=(92, 28, 10, 255))
    d.polygon([(32, 38), (34, 38), (37, 10), (32, 6)], fill=(168, 52, 12, 255))
    d.line([(26, 12), (31, 36)], fill=(255, 214, 90, 255), width=2)
    d.line([(38, 12), (35, 36)], fill=(255, 236, 170, 255), width=1)
    y = 8 + (shift % 28)
    d.polygon([(29, y + 10), (34, y + 8), (33, y), (30, y + 2)], fill=(255, 160, 32, 230))
    d.ellipse([31, 4, 36, 9], fill=(255, 220, 120, 255))
    return outline(im)


def save_icon(wid, image):
    image.save(os.path.join(ASSETS, "textures/item", wid + ".png"))


def mace_bones():
    grip = [
        cube([-0.82, -4.4, -0.82], [1.64, 8.6, 1.64], "grip_main"),
        cube([-0.98, -2.2, -0.98], [1.96, 0.7, 1.96], "grip_second"),
        cube([-0.98, 0.4, -0.98], [1.96, 0.7, 1.96], "grip_second"),
        cube([-0.98, 2.6, -0.98], [1.96, 0.7, 1.96], "grip_second"),
        cube([-1.15, -5.35, -1.15], [2.3, 1.15, 2.3], "metal_dark"),
    ]
    guard = [
        cube([-1.7, 4.15, -1.7], [3.4, 1.35, 3.4], "metal"),
        cube([-1.15, 4.45, -1.15], [2.3, 0.85, 2.3], "accent"),
    ]
    blade = [
        cube([-2.55, 5.35, -2.15], [5.1, 4.7, 4.3], "metal", "metal_dark"),
        cube([-2.05, 9.7, -1.75], [4.1, 1.45, 3.5], "metal"),
        cube([-1.25, 10.95, -1.15], [2.5, 0.95, 2.3], "metal_dark"),
        cube([-4.15, 6.15, -1.35], [1.7, 3.3, 2.7], "metal_dark", "metal"),
        cube([2.45, 6.15, -1.35], [1.7, 3.3, 2.7], "metal_dark", "metal"),
        cube([-2.15, 6.35, -2.85], [4.3, 2.9, 0.85], "metal"),
        cube([-2.15, 6.55, 2.0], [4.3, 2.5, 0.75], "metal_dark"),
        cube([-2.35, 6.05, -1.9], [4.7, 0.55, 3.8], "accent"),
        cube([-2.2, 8.55, -1.85], [4.4, 0.5, 3.7], "accent"),
    ]
    glow = [
        cube([-4.35, 6.35, -1.15], [0.35, 2.9, 2.3], "glow"),
        cube([4.0, 6.35, -1.15], [0.35, 2.9, 2.3], "glow"),
        cube([-1.7, 11.7, -0.7], [3.4, 0.28, 1.4], "glow"),
    ]
    return [
        bone("weapon", [], parent=None),
        bone("grip", grip),
        bone("guard", guard),
        bone("blade", blade),
        bone("blade_glow", glow),
    ]


def flail_bones():
    grip = [
        cube([-0.7, -3.6, -0.7], [1.4, 7.2, 1.4], "grip_main"),
        cube([-0.86, -1.4, -0.86], [1.72, 0.55, 1.72], "grip_second"),
        cube([-0.86, 0.8, -0.86], [1.72, 0.55, 1.72], "grip_second"),
        cube([-1.05, -4.55, -1.05], [2.1, 1.1, 2.1], "metal"),
    ]
    guard = [cube([-1.05, 3.35, -1.05], [2.1, 1.15, 2.1], "metal_dark")]
    chain = []
    samples = []
    steps = 16
    for i in range(steps):
        t = i / (steps - 1)
        x = math.sin(t * math.pi) * 2.7
        y = 4.3 + math.sin(t * math.pi * 0.5) * 7.6
        z = math.sin(t * math.pi) * 1.35
        samples.append((x, y, z))
        span = 1.45 if i % 2 == 0 else 1.15
        chain.append(cube([x - span / 2, y - span / 2, z - span / 2], [span, span, span], "metal_dark", "metal"))
    bx, by, bz = samples[-1]
    ball = []
    for ox, oy, oz, s in (
        (0, 0, 0, 2.5),
        (0, 0.7, 0, 2.1),
        (0, -0.55, 0, 2.0),
        (-0.7, 0.1, 0, 1.7),
        (0.7, 0.1, 0, 1.7),
        (0, 0.15, 0.7, 1.6),
        (0, 0.15, -0.7, 1.6),
    ):
        ball.append(cube([bx + ox - s / 2, by + oy - s / 2, bz + oz - s / 2], [s, s, s], "metal", "metal_dark"))
    spikes = []
    for ox, oy, oz in ((0, 1.7, 0), (0, -1.5, 0), (1.6, 0.2, 0), (-1.6, 0.2, 0), (0, 0.2, 1.5), (0, 0.2, -1.5)):
        spikes.append(cube(
            [bx + ox - 0.28, by + oy - 0.28, bz + oz - 0.28],
            [0.56, 0.9, 0.56],
            "glow", "accent"))
    return [
        bone("weapon", [], parent=None),
        bone("grip", grip),
        bone("guard", guard),
        bone("blade", chain + ball),
        bone("blade_glow", spikes),
    ]


def lava_bones():
    grip = [
        cube([-0.72, -5.1, -0.72], [1.44, 6.6, 1.44], "grip_main"),
        cube([-0.9, -3.4, -0.9], [1.8, 0.5, 1.8], "grip_second"),
        cube([-0.9, -1.6, -0.9], [1.8, 0.5, 1.8], "grip_second"),
        cube([-1.0, -5.9, -1.0], [2.0, 1.0, 2.0], "metal_dark"),
        cube([-0.55, -6.7, -0.55], [1.1, 0.7, 1.1], "accent"),
    ]
    guard = [
        cube([-2.6, 1.45, -0.55], [5.2, 0.7, 1.1], "metal"),
        cube([-1.15, 1.15, -0.8], [2.3, 1.35, 1.6], "metal_dark"),
    ]
    blade = []
    glow = []
    segs = 8
    length = 18.5
    seg_h = length / segs
    base_w = 2.5
    for i in range(segs):
        t = i / (segs - 1)
        w = max(0.28, base_w * (1.0 - 0.78 * t * t))
        th = max(0.22, 0.62 * (1.0 - 0.45 * t))
        y0 = 2.7 + i * seg_h
        bow = math.sin(t * math.pi) * 0.55
        blade.append(cube([bow - w / 2, y0, -th / 2], [w, seg_h * 0.98, th], "blade_dark", "blade_light"))
        core_w = w * 0.42
        blade.append(cube([bow - core_w / 2, y0, -th * 0.28], [core_w, seg_h * 0.98, th * 0.56], "blade_light"))
        glow.append(cube([bow - w / 2 - 0.08, y0, -0.08], [0.22, seg_h * 0.98, 0.16], "glow"))
    tip_y = 2.7 + length
    blade.append(cube([-0.12, tip_y - 0.2, -0.1], [0.24, 1.05, 0.2], "blade_light"))
    glow.append(cube([-0.16, tip_y - 0.15, -0.06], [0.18, 0.9, 0.12], "glow"))
    return [
        bone("weapon", [], parent=None),
        bone("grip", grip),
        bone("guard", guard),
        bone("blade", blade),
        bone("blade_glow", glow),
    ]


def main():
    mace_pal = {
        "blade_light": (186, 190, 196), "blade_dark": (92, 96, 104),
        "grip_main": (96, 62, 36), "grip_second": (58, 36, 24),
        "metal": (176, 180, 188), "glow": (232, 196, 96),
        "metal_dark": (70, 74, 82), "accent": (196, 146, 52),
    }
    flail_pal = {
        "blade_light": (160, 164, 172), "blade_dark": (78, 80, 88),
        "grip_main": (86, 54, 32), "grip_second": (48, 30, 20),
        "metal": (168, 172, 180), "glow": (230, 232, 236),
        "metal_dark": (64, 66, 74), "accent": (200, 160, 70),
    }
    lava_pal = {
        "blade_light": (196, 78, 18), "blade_dark": (72, 22, 12),
        "grip_main": (52, 28, 22), "grip_second": (28, 16, 14),
        "metal": (186, 132, 42), "glow": (255, 214, 96),
        "metal_dark": (84, 48, 22), "accent": (255, 120, 28),
    }
    write_geo_pack("war_mace", mace_bones(), mace_pal, 11, 0.55, -8)
    write_geo_pack("spike_flail", flail_bones(), flail_pal, 19, 0.62, -6)
    write_geo_pack("lava_sword", lava_bones(), lava_pal, 23, 0.4, -7, frames=6)
    item_models("war_mace")
    item_models("spike_flail")
    item_models("lava_sword")
    save_icon("war_mace", icon_mace())
    save_icon("spike_flail", icon_flail())
    frames = [icon_lava_frame(i * 7) for i in range(4)]
    strip = Image.new("RGBA", (64, 64 * 4), (0, 0, 0, 0))
    for i, frame in enumerate(frames):
        strip.paste(frame, (0, i * 64))
    strip.save(os.path.join(ASSETS, "textures/item/lava_sword.png"))
    mcmeta = {"animation": {"frametime": 4, "interpolate": True}}
    with open(os.path.join(ASSETS, "textures/item/lava_sword.png.mcmeta"), "w", encoding="utf-8") as handle:
        json.dump(mcmeta, handle, indent=2)
    print("built war_mace, spike_flail, lava_sword")


if __name__ == "__main__":
    main()
