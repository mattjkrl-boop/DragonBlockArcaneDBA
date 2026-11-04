#!/usr/bin/env python3
"""Procedurally generate the Evil Dread eye textures.

Writes the detailed red eye used by DreadGazeRenderer billboards into
assets/dragonblockarcanedba/textures/entity/dread_eye.png, and the
status effect icon into textures/mob_effect/evil_dread.png. The eye is
drawn, not dotted: a bloodshot almond sclera, a striated crimson iris,
a vertical slit pupil, a specular glint, and a soft red glow baked into
the alpha around the iris so the emissive render type blooms in the
dark. Deterministic: a fixed seed, so regeneration is reproducible.

Run: python3 tools/gen_dread_textures.py
"""
import os

import numpy as np
from PIL import Image

ASSETS = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                      "src", "main", "resources", "assets", "dragonblockarcanedba",
                      "textures")


def grid_noise(rng, cells, size):
    g = rng.random((cells + 1, cells + 1))
    g[-1, :] = g[0, :]
    g[:, -1] = g[:, 0]
    img = Image.fromarray((g * 255).astype(np.uint8)).resize((size, size), Image.BILINEAR)
    return np.asarray(img, dtype=np.float64) / 255.0


def fbm(rng, size):
    return (0.55 * grid_noise(rng, 3, size)
            + 0.30 * grid_noise(rng, 6, size)
            + 0.15 * grid_noise(rng, 12, size))


def smoothstep(x):
    x = np.clip(x, 0.0, 1.0)
    return x * x * (3.0 - 2.0 * x)


def draw_eye(size, almond_half_h, iris_r, pupil_hw):
    rng = np.random.default_rng(0xD3AD)
    y, x = np.mgrid[0:size, 0:size]
    cx = (x - size / 2 + 0.5) / (size / 2)
    cy = (y - size / 2 + 0.5) / (size / 2)
    r = np.sqrt(cx * cx + cy * cy)
    ang = np.arctan2(cy, cx)
    noise = fbm(rng, size)
    fine = grid_noise(rng, 24, size)

    # Almond outline: half height profile tapering to points at the corners.
    profile = almond_half_h * np.power(np.clip(1.0 - (cx / 1.04) ** 2, 0.0, 1.0), 0.62)
    inside = profile - np.abs(cy)
    eye_mask = smoothstep(inside * (size / 9.0) + 0.5)

    # Sclera: near-black bloodshot base, mottled.
    sclera = np.zeros((size, size, 3))
    sclera[..., 0] = 0.13 + 0.10 * noise
    sclera[..., 1] = 0.020 + 0.015 * noise
    sclera[..., 2] = 0.028 + 0.020 * fine
    # Veins: ridged noise streaks creeping in from both corners.
    ridged = 1.0 - np.abs(2.0 * fine - 1.0)
    vein = smoothstep((ridged - 0.90) * 6.0) * smoothstep((np.abs(cx) - 0.30) * 2.2) * eye_mask
    sclera[..., 0] += vein * 0.30
    sclera[..., 1] += vein * 0.01

    # Iris: radial gradient, hot orange-red heart to deep crimson rim,
    # with angular striations and a darker collarette ring.
    iris_mask = smoothstep((iris_r - r) * (size / 7.0) + 0.5)
    grad = np.clip(r / iris_r, 0.0, 1.0)
    stri = 0.78 + 0.22 * np.sin(ang * 26.0 + noise * 9.0) + 0.10 * np.sin(ang * 61.0 - fine * 5.0)
    iris = np.zeros((size, size, 3))
    iris[..., 0] = (1.00 - 0.48 * grad) * stri
    iris[..., 1] = (0.26 - 0.24 * grad) * stri
    iris[..., 2] = (0.05 - 0.03 * grad) * stri
    collar = smoothstep((np.abs(r - iris_r * 0.46) - 0.035) * -22.0 + 0.5)
    iris *= (1.0 - 0.35 * collar)[..., None]

    rgb = sclera * (1.0 - iris_mask[..., None]) + iris * iris_mask[..., None]

    # Pupil: a vertical slit, widest at the iris center, pure black.
    slit_hw = pupil_hw * np.clip(1.0 - (cy / (iris_r * 0.92)) ** 2, 0.0, 1.0)
    pupil_mask = smoothstep((slit_hw - np.abs(cx)) * (size / 3.0) + 0.5) * iris_mask
    rgb *= (1.0 - pupil_mask[..., None])

    # Specular glint: one hard dot high on the iris, one faint bounce low.
    spec = np.exp(-(((cx + iris_r * 0.34) ** 2 + (cy + iris_r * 0.38) ** 2) / 0.0022))
    spec += 0.35 * np.exp(-(((cx - iris_r * 0.22) ** 2 + (cy - iris_r * 0.52) ** 2) / 0.004))
    rgb += (spec * iris_mask)[..., None] * np.array([0.95, 0.90, 0.88])

    # Baked glow: soft red bloom in the transparent surround, strongest
    # near the iris, gone before the quad edge so billboards tile cleanly.
    glow = np.exp(-(r * r) / 0.42) * 0.38 * (1.0 - eye_mask)
    alpha = np.clip(eye_mask + glow, 0.0, 1.0) * smoothstep((1.30 - r) * 2.6)
    rgb = rgb * eye_mask[..., None] + np.array([0.72, 0.04, 0.05]) * glow[..., None]

    rgba = np.zeros((size, size, 4), dtype=np.uint8)
    rgba[..., 0] = (np.clip(rgb[..., 0], 0, 1) * 255).astype(np.uint8)
    rgba[..., 1] = (np.clip(rgb[..., 1], 0, 1) * 255).astype(np.uint8)
    rgba[..., 2] = (np.clip(rgb[..., 2], 0, 1) * 255).astype(np.uint8)
    rgba[..., 3] = (alpha * 255).astype(np.uint8)
    return Image.fromarray(rgba, "RGBA")


def main():
    eye = draw_eye(128, 0.50, 0.335, 0.052)
    eye_path = os.path.join(ASSETS, "entity", "dread_eye.png")
    eye.save(eye_path)
    print("wrote", eye_path, eye.size)

    icon = draw_eye(64, 0.60, 0.40, 0.075)
    icon_path = os.path.join(ASSETS, "mob_effect", "evil_dread.png")
    icon.save(icon_path)
    print("wrote", icon_path, icon.size)


if __name__ == "__main__":
    main()
