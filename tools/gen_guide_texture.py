#!/usr/bin/env python3
"""Procedurally generate the Otherworld guide texture (D12).

Paints a 64x64 texture in the exact VillagerModel UV layout (verified by
javap against the 26.2 client jar): head (0,0) 8x10x8, nose (24,0) 2x4x2,
body (16,20) 8x12x6, jacket (0,38) 8x20x6, arms (44,22) 4x8x4 twice plus
hands (40,38) 8x4x4, legs (0,22) 4x12x4. The hat and hat rim regions are
left transparent: the guide is bareheaded. The figure is a serene robed
attendant: warm tan skin, deep indigo robe with gold trim and a gold
circlet, dark belt, slate trousers.

Run: python3 tools/gen_guide_texture.py
"""
import os

import numpy as np
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src", "main", "resources", "assets", "dragonblockarcanedba",
                    "textures", "entity")
SIZE = 64

SKIN = (214, 176, 138)
SKIN_DARK = (188, 148, 112)
SKIN_LIGHT = (232, 198, 162)
ROBE = (46, 56, 108)
ROBE_DARK = (33, 41, 82)
ROBE_LIGHT = (76, 90, 154)
GOLD = (216, 178, 74)
GOLD_DARK = (178, 142, 54)
TEAL = (64, 200, 180)
BELT = (24, 22, 30)
TROUSER = (38, 40, 58)
SHOE = (18, 16, 22)
EYE_DARK = (30, 26, 40)
EYE_WHITE = (240, 240, 235)
BROW = (96, 70, 46)
MOUTH = (122, 74, 62)

rng = np.random.default_rng(20261103)
img = np.zeros((SIZE, SIZE, 4), dtype=np.uint8)


def faces(u, v, w, h, d):
    """Standard box UV faces for texOffs (u, v) and size (w, h, d)."""
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + 2 * d + w, v + d, w, h),
    }


def fill(rect, color, jitter=5):
    x, y, w, h = rect
    noise = rng.integers(-jitter, jitter + 1, size=(h, w, 1))
    base = np.array(color, dtype=np.int16).reshape(1, 1, 3)
    rgb = np.clip(base + noise, 0, 255).astype(np.uint8)
    img[y:y + h, x:x + w, :3] = rgb
    img[y:y + h, x:x + w, 3] = 255


def px(face, dx, dy, color):
    x, y = face[0] + dx, face[1] + dy
    img[y, x, :3] = color
    img[y, x, 3] = 255


def band(face, rows, color, cols=None):
    x, y, w, h = face
    c0, c1 = (0, w) if cols is None else cols
    for dy in rows:
        fill((x + c0, y + dy, c1 - c0, 1), color, jitter=3)


# Head: skin everywhere, circlet band on the second row of every side face.
head = faces(0, 0, 8, 10, 8)
for f in head.values():
    fill(f, SKIN)
fill(head["top"], SKIN_LIGHT)
fill(head["bottom"], SKIN_DARK)
for side in ("right", "front", "left", "back"):
    band(head[side], (1, 2), GOLD)
    band(head[side], (3,), GOLD_DARK)
px(head["front"], 3, 1, TEAL)
px(head["front"], 4, 1, TEAL)
px(head["front"], 3, 2, TEAL)
px(head["front"], 4, 2, TEAL)
# brow, eyes, mouth on the front face (8 wide, 10 tall)
band(head["front"], (4,), BROW, cols=(1, 7))
for ex in (1, 5):
    px(head["front"], ex, 5, EYE_WHITE)
    px(head["front"], ex + 1, 5, EYE_DARK)
    px(head["front"], ex, 6, EYE_DARK)
    px(head["front"], ex + 1, 6, EYE_DARK)
px(head["front"], 3, 8, MOUTH)
px(head["front"], 4, 8, MOUTH)

# Nose: plain shaded skin.
nose = faces(24, 0, 2, 4, 2)
for f in nose.values():
    fill(f, SKIN_DARK)

# Body: robe with a lighter front panel, gold collar, belt with gold buckle.
body = faces(16, 20, 8, 12, 6)
for f in body.values():
    fill(f, ROBE)
fill(body["front"], ROBE)  # base, panel drawn over
band(body["front"], (0,), GOLD)
for dy in range(1, 12):
    fill((body["front"][0] + 3, body["front"][1] + dy, 2, 1), ROBE_LIGHT, jitter=4)
band(body["front"], (9, 10), BELT)
px(body["front"], 3, 9, GOLD)
px(body["front"], 4, 9, GOLD)
px(body["front"], 3, 10, GOLD_DARK)
px(body["front"], 4, 10, GOLD_DARK)
band(body["right"], (9, 10), BELT)
band(body["left"], (9, 10), BELT)
band(body["back"], (9, 10), BELT)

# Jacket: the long outer robe shell, darker, gold hem on the last two rows.
jacket = faces(0, 38, 8, 20, 6)
for f in jacket.values():
    fill(f, ROBE_DARK)
for dy in range(1, 18):
    fill((jacket["front"][0] + 3, jacket["front"][1] + dy, 2, 1), ROBE, jitter=4)
band(jacket["front"], (0,), GOLD)
for side in ("right", "front", "left", "back"):
    band(jacket[side], (18,), GOLD_DARK)
    band(jacket[side], (19,), GOLD)

# Arms: robe sleeves; the crossed hands box is skin.
for f in faces(44, 22, 4, 8, 4).values():
    fill(f, ROBE_DARK)
for f in faces(40, 38, 8, 4, 4).values():
    fill(f, SKIN)

# Legs: slate trousers, dark shoes on the bottom three rows.
legs = faces(0, 22, 4, 12, 4)
for f in legs.values():
    fill(f, TROUSER)
for side in ("right", "front", "left", "back"):
    band(legs[side], (9, 10, 11), SHOE)

os.makedirs(ROOT, exist_ok=True)
out = os.path.join(ROOT, "otherworld_guide.png")
Image.fromarray(img, "RGBA").save(out)
print("wrote", out)
