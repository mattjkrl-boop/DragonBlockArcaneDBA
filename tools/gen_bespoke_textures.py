#!/usr/bin/env python3
"""Procedurally generate the bespoke pass particle textures.

Writes 4-frame sprite sets for every DbaParticles type added by the
bespoke pass into assets/dragonblockarcanedba/textures/particle/.
Textures are white shapes with alpha: the particle Style records carry
the colors, so one texture set serves every theme. Deterministic: a
fixed seed per texture, so regeneration is reproducible.

Run: python3 tools/gen_bespoke_textures.py
"""
import math
import os

import numpy as np
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src", "main", "resources", "assets", "dragonblockarcanedba",
                    "textures", "particle")
SIZE = 64
FRAMES = 4


def grid_noise(rng, cells, size=SIZE):
    """Value noise: random coarse grid, bilinearly upscaled, 0..1."""
    g = rng.random((cells + 1, cells + 1))
    # wrap edges so blobs tile inward cleanly
    g[-1, :] = g[0, :]
    g[:, -1] = g[:, 0]
    img = Image.fromarray((g * 255).astype(np.uint8)).resize((size, size), Image.BILINEAR)
    return np.asarray(img, dtype=np.float64) / 255.0


def fbm(rng, size=SIZE):
    return (0.55 * grid_noise(rng, 3, size)
            + 0.30 * grid_noise(rng, 6, size)
            + 0.15 * grid_noise(rng, 12, size))


def coords():
    y, x = np.mgrid[0:SIZE, 0:SIZE]
    cx = (x - SIZE / 2 + 0.5) / (SIZE / 2)
    cy = (y - SIZE / 2 + 0.5) / (SIZE / 2)
    r = np.sqrt(cx * cx + cy * cy)
    ang = np.arctan2(cy, cx)
    return cx, cy, r, ang


def to_image(alpha, intensity=None):
    a = np.clip(alpha, 0.0, 1.0)
    lum = a if intensity is None else np.clip(intensity, 0.0, 1.0)
    rgba = np.zeros((SIZE, SIZE, 4), dtype=np.uint8)
    rgba[..., 0] = (lum * 255).astype(np.uint8)
    rgba[..., 1] = (lum * 255).astype(np.uint8)
    rgba[..., 2] = (lum * 255).astype(np.uint8)
    rgba[..., 3] = (a * 255).astype(np.uint8)
    return Image.fromarray(rgba, "RGBA")


def save(name, frames):
    for i, img in enumerate(frames):
        img.save(os.path.join(ROOT, f"{name}_{i}.png"))
    print("wrote", name, "x", len(frames))


def dust_puff(frame, rng):
    _, _, r, _ = coords()
    n = fbm(rng)
    edge = 0.95 - frame * 0.10
    body = np.clip((edge - r) * 2.2, 0, 1) * (0.45 + 0.75 * n)
    holes = np.clip((n - 0.62) * 2.0, 0, 0.55) * np.clip((edge - r) * 3.0, 0, 1)
    return to_image(body - holes)


def crit_spark(frame, rng):
    cx, cy, r, ang = coords()
    # four-point star: thin along the diagonals, wide on the axes
    star = np.clip(1.0 - np.abs(np.sin(ang * 2)) * 1.15 - r * (0.55 + frame * 0.1), 0, 1)
    core = np.clip(1.15 - r * 2.6, 0, 1)
    jitter = 0.9 + 0.2 * rng.random()
    return to_image(np.clip(star * 0.85 + core, 0, 1) * jitter)


def flame(frame, rng):
    cx, cy, r, _ = coords()
    n = grid_noise(rng, 5)
    # teardrop: narrow at the top, round at the bottom, wobbled by noise
    wob = (n - 0.5) * 0.55
    xr = cx + wob * (1.0 - np.abs(cy))
    width = 0.52 * (1.0 - 0.45 * np.clip(cy + 0.6, 0, 1.4))
    body = np.clip(1.0 - np.abs(xr) / np.maximum(width, 0.05), 0, 1)
    vert = np.clip(1.15 - np.abs(cy + 0.08) * 1.05, 0, 1)
    flicker = 0.85 + 0.3 * grid_noise(rng, 3)
    return to_image(body * vert * flicker)


def blast(frame, rng):
    _, _, r, ang = coords()
    n = grid_noise(rng, 7)
    ring_r = 0.30 + frame * 0.19
    ring = np.clip(1.0 - np.abs(r - ring_r) * 4.2, 0, 1) * (0.55 + 0.7 * n)
    fill = np.clip((ring_r - r) * 1.6, 0, 1) * 0.30 * (0.5 + n)
    return to_image(np.clip(ring + fill, 0, 1))


def ash(frame, rng):
    cx, cy, r, ang = coords()
    n = grid_noise(rng, 4)
    sides = 5 + frame
    poly = np.cos(ang * sides) * 0.10 + 0.16 * np.sin(ang * 3 + frame)
    body = np.clip((0.52 + poly - r) * 3.2, 0, 1) * (0.5 + 0.65 * n)
    return to_image(body)


def wisp(frame, rng):
    _, _, r, _ = coords()
    n = grid_noise(rng, 6)
    halo = np.clip(1.0 - r, 0, 1) ** 2 * (0.55 + 0.5 * n)
    core = np.clip(1.0 - r * 2.8, 0, 1)
    return to_image(np.clip(halo * 0.8 + core, 0, 1))


def rain(frame, rng):
    cx, cy, r, _ = coords()
    del r
    streak = np.clip(1.0 - np.abs(cx) * 9.0, 0, 1)
    vert = np.clip(1.0 - np.abs(cy) * 1.15, 0, 1) ** 1.5
    head = np.clip(1.2 - (cy + 0.55) * 2.2, 0, 1) * 0.35
    return to_image(streak * vert + streak * head)


def theme_dust(frame, rng):
    _, _, r, _ = coords()
    n = grid_noise(rng, 5)
    body = np.clip(1.0 - r * 1.25, 0, 1) ** 1.6 * (0.7 + 0.45 * n)
    return to_image(body)


def cloud_puff(frame, rng):
    _, _, r, _ = coords()
    n = fbm(rng)
    # bigger and softer than dust_puff: a sky cloud body with shallow erosion
    edge = 1.02 - frame * 0.05
    body = np.clip((edge - r) * 1.5, 0, 1) * (0.55 + 0.60 * n)
    holes = np.clip((n - 0.70) * 1.6, 0, 0.35) * np.clip((edge - r) * 2.0, 0, 1)
    return to_image((body - holes) * 0.85)


def damage_spark(frame, rng):
    _, _, r, ang = coords()
    # jagged six-point star, sharper and more broken than crit_spark
    spike = 0.55 + 0.45 * np.abs(np.sin(ang * 3 + frame * 0.35))
    star = np.clip(1.0 - r / np.maximum(spike, 0.08) * 1.05, 0, 1) ** 1.4
    core = np.clip(1.1 - r * 2.4, 0, 1)
    return to_image(np.clip(star + core * 0.9, 0, 1))


def happy_sparkle(frame, rng):
    _, _, r, ang = coords()
    # rounded four-point sparkle with a soft halo, wider arms than crit_spark
    star = np.clip(1.0 - np.abs(np.sin(ang * 2)) * 0.8 - r * (0.75 + frame * 0.06), 0, 1)
    core = np.clip(1.25 - r * 2.0, 0, 1)
    glow = np.clip(1.0 - r * 1.1, 0, 1) ** 2 * 0.35
    return to_image(np.clip(star * 0.8 + core + glow, 0, 1))


MAKERS = {
    "dust_puff": dust_puff,
    "crit_spark": crit_spark,
    "flame": flame,
    "blast": blast,
    "ash": ash,
    "wisp": wisp,
    "rain": rain,
    "theme_dust": theme_dust,
    "cloud_puff": cloud_puff,
    "damage_spark": damage_spark,
    "happy_sparkle": happy_sparkle,
}


def main():
    os.makedirs(ROOT, exist_ok=True)
    for name, maker in MAKERS.items():
        rng = np.random.default_rng(abs(hash(name)) % (2 ** 31))
        save(name, [maker(f, rng) for f in range(FRAMES)])
        json_dir = os.path.join(os.path.dirname(os.path.dirname(ROOT)), "particles")
        with open(os.path.join(json_dir, f"{name}.json"), "w") as fh:
            fh.write('{\n  "textures": [\n')
            fh.write(",\n".join(f'    "dragonblockarcanedba:{name}_{i}"' for i in range(FRAMES)))
            fh.write("\n  ]\n}\n")
        print("wrote particles/" + name + ".json")


if __name__ == "__main__":
    main()
