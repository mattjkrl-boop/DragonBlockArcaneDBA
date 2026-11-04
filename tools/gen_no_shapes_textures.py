#!/usr/bin/env python3
"""Generate the no-shapes realism pass material sprites.

The bespoke textures were 64 px soft sprites. This generator writes
128 px, eight-frame material sets whose silhouettes come from fractal
noise, cellular breakup, domain-warped ridges, tendrils, and erosion.
They are white or gray intensity images with alpha: particle color is
carried by the particle options or style, so one set serves all themes.

Run: python3 tools/gen_no_shapes_textures.py
"""
import json
import math
import os
import zlib

import numpy as np
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src", "main", "resources", "assets", "dragonblockarcanedba")
TEXTURE_ROOT = os.path.join(ROOT, "textures", "particle")
PARTICLE_ROOT = os.path.join(ROOT, "particles")
SIZE = 128
FRAMES = 8


def stable_seed(name):
    return zlib.crc32(name.encode("utf-8")) & 0x7FFFFFFF


def grid_noise(rng, cells, size=SIZE):
    g = rng.random((cells + 1, cells + 1))
    g[-1, :] = g[0, :]
    g[:, -1] = g[:, 0]
    img = Image.fromarray((g * 255).astype(np.uint8)).resize((size, size), Image.BILINEAR)
    return np.asarray(img, dtype=np.float64) / 255.0


def fbm(rng, size=SIZE):
    return (0.42 * grid_noise(rng, 3, size)
            + 0.27 * grid_noise(rng, 6, size)
            + 0.19 * grid_noise(rng, 12, size)
            + 0.12 * grid_noise(rng, 24, size))


def ridged(rng, size=SIZE):
    n = fbm(rng, size)
    return 1.0 - np.abs(2.0 * n - 1.0)


def cellular(rng, size=SIZE, points=22):
    y, x = np.mgrid[0:size, 0:size]
    x = x / size
    y = y / size
    pts = rng.random((points, 2))
    d1 = np.full((size, size), 9.0)
    d2 = np.full((size, size), 9.0)
    for px, py in pts:
        dx = np.abs(x - px)
        dy = np.abs(y - py)
        dx = np.minimum(dx, 1.0 - dx)
        dy = np.minimum(dy, 1.0 - dy)
        d = dx * dx + dy * dy
        closer = d < d1
        d2 = np.where(closer, d1, np.minimum(d2, d))
        d1 = np.where(closer, d, d1)
    return np.sqrt(d1), np.sqrt(np.maximum(d2, d1))


def coords(size=SIZE):
    y, x = np.mgrid[0:size, 0:size]
    cx = (x - size / 2 + 0.5) / (size / 2)
    cy = (y - size / 2 + 0.5) / (size / 2)
    r = np.sqrt(cx * cx + cy * cy)
    ang = np.arctan2(cy, cx)
    return cx, cy, r, ang


def smoothstep(a, b, x):
    den = b - a
    den = np.where(np.abs(den) < 1e-6, np.where(den < 0, -1e-6, 1e-6), den)
    t = np.clip((x - a) / den, 0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)


def to_image(alpha, intensity=None):
    a = np.clip(alpha, 0.0, 1.0)
    lum = a if intensity is None else np.clip(intensity, 0.0, 1.0)
    rgba = np.zeros((SIZE, SIZE, 4), dtype=np.uint8)
    rgba[..., 0] = (lum * 255).astype(np.uint8)
    rgba[..., 1] = (lum * 255).astype(np.uint8)
    rgba[..., 2] = (lum * 255).astype(np.uint8)
    rgba[..., 3] = (a * 255).astype(np.uint8)
    return Image.fromarray(rgba, "RGBA")


def stamp(field, cx, cy, radius, strength, rng, irregular=0.45):
    """Add one noisy gaussian fleck without using a regular outline."""
    y, x = np.mgrid[0:SIZE, 0:SIZE]
    dx = (x - cx) / max(1.0, radius)
    dy = (y - cy) / max(1.0, radius * (0.55 + rng.random() * 0.8))
    d = dx * dx + dy * dy
    fleck = np.exp(-d * (2.2 + rng.random() * 2.0))
    fleck *= 0.75 + irregular * rng.random()
    field[:] = np.maximum(field, fleck * strength)


def plasma(frame, rng):
    _, _, r, ang = coords()
    n = fbm(rng)
    ridge = ridged(rng)
    _, f2 = cellular(rng)
    fall = smoothstep(1.05, 0.12, r)
    filaments = np.power(ridge, 4.0) * fall
    tendrils = np.power(0.5 + 0.5 * np.sin(ang * 7.0 + n * 9.0 + frame * 0.7), 7.0) * smoothstep(1.0, 0.25, r)
    cell_edges = smoothstep(0.28, 0.02, f2) * 0.35 * fall
    alpha = fall * (0.30 + 0.62 * n) + filaments * 0.72 + tendrils * 0.42 + cell_edges
    alpha = smoothstep(0.18, 0.78, alpha)
    core = smoothstep(0.72, 0.0, r)
    return to_image(alpha, core * 0.75 + filaments * 0.65 + alpha * 0.35)


def flame(frame, rng):
    cx, cy, r, _ = coords()
    n = fbm(rng)
    ridge = ridged(rng)
    h = np.clip((cy + 1.0) * 0.5, 0.0, 1.0)  # 0 top, 1 bottom in image space
    wobble = (n - 0.5) * (0.42 + 0.22 * np.sin(frame * 0.9))
    xr = cx + wobble * (0.35 + h)
    width = 0.10 + 0.46 * np.power(h, 0.72) + 0.13 * ridge
    body = smoothstep(width, width * 0.18, np.abs(xr))
    vertical = smoothstep(-0.08, 0.25, cy + 1.0) * smoothstep(1.08, 0.48, cy + 1.0)
    licks = np.power(ridge, 3.0) * body * (0.55 + 0.45 * np.sin(frame * 1.3 + cx * 4.0))
    alpha = body * vertical * (0.38 + 0.58 * n) + licks * 0.55
    alpha = smoothstep(0.16, 0.72, alpha)
    core = body * smoothstep(0.6, 1.0, h) * (0.55 + 0.45 * n)
    return to_image(alpha, core + licks * 0.5 + alpha * 0.22)


def smoke(frame, rng, density=0.88, intensity=0.72):
    _, _, r, _ = coords()
    n = fbm(rng)
    n2 = fbm(rng)
    f1, f2 = cellular(rng)
    edge = 1.02 - frame * 0.018
    mass = n * 0.72 + (1.0 - f1) * 0.28
    alpha = smoothstep(0.34, 0.86, mass) * smoothstep(edge, edge * 0.42, r)
    holes = smoothstep(0.68, 0.95, n2) * smoothstep(0.85, 0.25, r) * 0.42
    alpha = np.clip(alpha * density - holes, 0.0, 1.0)
    lum = intensity * (0.35 + 0.65 * n) * (0.75 + 0.25 * np.sin(frame * 0.5 + f2 * 5.0))
    return to_image(alpha, lum)


def vapor(frame, rng):
    cx, cy, r, _ = coords()
    n = fbm(rng)
    ridge = ridged(rng)
    bands = 0.5 + 0.5 * np.sin(cy * 5.0 + n * 8.0 + frame * 0.55 + cx * 1.4)
    alpha = smoothstep(1.02, 0.18, r) * (0.18 + 0.46 * n + 0.28 * bands * ridge)
    alpha = smoothstep(0.12, 0.68, alpha) * 0.78
    return to_image(alpha, 0.55 + 0.45 * n)


def dust(frame, rng):
    _, _, r, _ = coords()
    n = fbm(rng)
    f1, _ = cellular(rng, points=28)
    grit = grid_noise(rng, 36)
    clumps = smoothstep(0.28, 0.82, n * 0.72 + (1.0 - f1) * 0.38)
    alpha = clumps * smoothstep(1.0, 0.22, r)
    specks = smoothstep(0.82, 0.98, grit) * smoothstep(0.92, 0.3, r) * 0.5
    alpha = np.clip(alpha * 0.82 + specks, 0.0, 1.0)
    return to_image(alpha, 0.42 + 0.48 * n + specks * 0.35)


def mist(frame, rng):
    _, cy, r, _ = coords()
    n = fbm(rng)
    n2 = fbm(rng)
    layered = n * 0.68 + n2 * 0.32
    alpha = smoothstep(0.38, 0.88, layered) * smoothstep(1.08, 0.30, r)
    alpha *= 0.52 + 0.10 * np.sin(frame * 0.45 + cy * 3.0)
    return to_image(np.clip(alpha, 0.0, 0.62), 0.48 + 0.42 * layered)


def soul(frame, rng):
    cx, cy, r, _ = coords()
    n = fbm(rng)
    ridge = ridged(rng)
    h = np.clip((cy + 1.0) * 0.5, 0.0, 1.0)
    wobble = (n - 0.5) * 0.52
    xr = cx + wobble * (0.25 + h)
    width = 0.08 + 0.34 * np.power(h, 0.8)
    body = smoothstep(width, width * 0.16, np.abs(xr))
    tendrils = np.power(ridge, 4.0) * smoothstep(1.0, 0.18, r)
    alpha = body * (0.28 + 0.52 * n) + tendrils * 0.34
    alpha = smoothstep(0.13, 0.70, alpha) * 0.88
    return to_image(alpha, 0.48 + 0.52 * body + tendrils * 0.35)


def darkness(frame, rng):
    img = smoke(frame, rng, density=1.0, intensity=0.30)
    arr = np.asarray(img).astype(np.float64) / 255.0
    arr[..., 3] = np.clip(arr[..., 3] * 1.22, 0.0, 1.0)
    return Image.fromarray((arr * 255).astype(np.uint8), "RGBA")


def flecks(frame, rng, count, strength, base_alpha=0.10, radius_min=2.0, radius_span=7.0):
    _, _, r, _ = coords()
    n = fbm(rng)
    field = smoothstep(1.0, 0.2, r) * base_alpha * n
    for _ in range(count):
        stamp(field, rng.random() * SIZE, rng.random() * SIZE,
              radius_min + rng.random() * radius_span,
              strength * (0.55 + rng.random() * 0.75), rng)
    return field


def glass_dust(frame, rng):
    field = flecks(frame, rng, 170, 0.92, base_alpha=0.012, radius_min=0.7, radius_span=1.9)
    # Hairline glints: short irregular bright traces, kept microscopic so
    # the set reads as suspended glass dust rather than shard pictures.
    y, x = np.mgrid[0:SIZE, 0:SIZE]
    for _ in range(14):
        x0, y0 = rng.random() * SIZE, rng.random() * SIZE
        length = 5.0 + rng.random() * 13.0
        ang = rng.random() * math.pi
        dx, dy = math.cos(ang), math.sin(ang)
        t = np.clip(((x - x0) * dx + (y - y0) * dy) / length, 0.0, 1.0)
        px = x0 + dx * length * t
        py = y0 + dy * length * t
        d = np.sqrt((x - px) ** 2 + (y - py) ** 2)
        line = np.exp(-d * d / 1.4) * np.sin(t * math.pi) ** 2
        field = np.maximum(field, line * (0.35 + rng.random() * 0.45))
    return to_image(field, np.clip(field * 1.25, 0.0, 1.0))


def ember(frame, rng):
    field = flecks(frame, rng, 30, 0.88, base_alpha=0.08)
    _, _, r, _ = coords()
    halo = smoothstep(0.72, 0.0, r) * 0.20
    return to_image(np.clip(field + halo, 0.0, 1.0), np.clip(field * 1.2 + halo, 0.0, 1.0))


def ash(frame, rng):
    field = flecks(frame, rng, 42, 0.58, base_alpha=0.16)
    return to_image(field, field * 0.72)


MAKERS = {
    "plasma": plasma,
    "flame": flame,
    "smoke": smoke,
    "vapor": vapor,
    "dust": dust,
    "dust_puff": dust,
    "mist": mist,
    "soul": soul,
    "darkness": darkness,
    "glass_dust": glass_dust,
    "ember": ember,
    "ash": ash,
}


def main():
    os.makedirs(TEXTURE_ROOT, exist_ok=True)
    os.makedirs(PARTICLE_ROOT, exist_ok=True)
    for name, maker in MAKERS.items():
        rng = np.random.default_rng(stable_seed(name))
        for frame in range(FRAMES):
            maker(frame, rng).save(os.path.join(TEXTURE_ROOT, f"{name}_{frame}.png"))
        payload = {"textures": [f"dragonblockarcanedba:{name}_{i}" for i in range(FRAMES)]}
        with open(os.path.join(PARTICLE_ROOT, f"{name}.json"), "w") as fh:
            json.dump(payload, fh, indent=2)
            fh.write("\n")
        print("wrote", name, "x", FRAMES)


if __name__ == "__main__":
    main()
