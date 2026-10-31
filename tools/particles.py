"""Generates the custom particle sprites (white, tinted in code) and particle json files."""
import json, math, os
from PIL import Image, ImageDraw, ImageFilter
A = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/dragonblockarcanedba')
T = os.path.join(A, 'textures/particle'); P = os.path.join(A, 'particles')
os.makedirs(T, exist_ok=True); os.makedirs(P, exist_ok=True)
def soft(size, radius, power):
    im = Image.new('RGBA', (size, size))
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - c, y - c) / radius
            a = max(0.0, 1 - d) ** power
            im.putpixel((x, y), (255, 255, 255, int(255 * a)))
    return im
def save(name, frames):
    for i, im in enumerate(frames):
        im.save(os.path.join(T, f'{name}_{i}.png'))
    json.dump({'textures': [f'dragonblockarcanedba:{name}_{i}' for i in range(len(frames))]}, open(os.path.join(P, f'{name}.json'), 'w'), indent=2)
# spark: bright core with a 4 point star, shrinking
spark = []
for r in (7, 6, 5, 4):
    im = soft(16, r, 2.2); d = ImageDraw.Draw(im)
    d.line([(8 - r, 8), (8 + r, 8)], fill=(255, 255, 255, 200)); d.line([(8, 8 - r), (8, 8 + r)], fill=(255, 255, 255, 200))
    spark.append(im)
save('spark', spark)
save('ember', [soft(16, r, 1.6) for r in (6, 5, 4, 3)])
# void smoke: lumpy blobs
import random
random.seed(7); smoke = []
for i in range(4):
    im = Image.new('RGBA', (16, 16)); d = ImageDraw.Draw(im)
    for _ in range(6):
        x, y, r = random.uniform(4, 12), random.uniform(4, 12), random.uniform(2, 4)
        d.ellipse([x - r, y - r, x + r, y + r], fill=(255, 255, 255, 150))
    smoke.append(im.filter(ImageFilter.GaussianBlur(1.2)))
save('void_smoke', smoke)
# wind: curved streaks
wind = []
for i in range(4):
    im = Image.new('RGBA', (16, 16)); d = ImageDraw.Draw(im)
    d.arc([1, 3 + i, 15, 13 + i // 2], 200, 340 - i * 20, fill=(255, 255, 255, 220), width=1)
    d.arc([3, 6, 13, 15], 210, 320, fill=(255, 255, 255, 120), width=1)
    wind.append(im.filter(ImageFilter.GaussianBlur(0.4)))
save('wind', wind)
print('ok')
