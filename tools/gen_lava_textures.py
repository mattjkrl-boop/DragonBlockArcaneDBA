#!/usr/bin/env python3
"""Procedurally generate the Lava Sword redesign block textures.

Writes the magma crust stage textures (molten, cooling, dark crust,
top and side) and the mini volcano rock texture into
assets/dragonblockarcanedba/textures/block/. Same house method as
tools/gen_bespoke_textures.py: seeded value-noise fbm, deterministic
per texture, so regeneration is reproducible. Block textures are
32x32 (twice vanilla resolution, so the cooling cracks read).

Run: python3 tools/gen_lava_textures.py
"""
import os

import numpy as np
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src", "main", "resources", "assets", "dragonblockarcanedba",
                    "textures", "block")
SIZE = 32


def grid_noise(rng, cells, size=SIZE):
    g = rng.random((cells + 1, cells + 1))
    g[-1, :] = g[0, :]
    g[:, -1] = g[:, 0]
    img = Image.fromarray((g * 255).astype(np.uint8)).resize((size, size), Image.BILINEAR)
    return np.asarray(img, dtype=np.float64) / 255.0


def fbm(rng, size=SIZE):
    return (0.55 * grid_noise(rng, 3, size)
            + 0.30 * grid_noise(rng, 6, size)
            + 0.15 * grid_noise(rng, 12, size))


def ramp(stops):
    """stops: list of (t, (r, g, b)) -> per-pixel color array from a 0..1 field."""
    ts = np.array([s[0] for s in stops])
    cols = np.array([s[1] for s in stops], dtype=np.float64)

    def apply(field):
        idx = np.clip(np.searchsorted(ts, field) - 1, 0, len(stops) - 2)
        t0, t1 = ts[idx], ts[idx + 1]
        frac = np.clip((field - t0) / np.maximum(t1 - t0, 1e-6), 0, 1)
        out = np.zeros(field.shape + (3,), dtype=np.float64)
        for c in range(3):
            out[..., c] = cols[idx, c] * (1 - frac) + cols[idx + 1, c] * frac
        return out

    return apply


def cracks(rng, count, size=SIZE):
    """Random-walk crack polylines as a 0..1 brightness field."""
    field = np.zeros((size, size), dtype=np.float64)
    for _ in range(count):
        x, y = rng.integers(2, size - 2), rng.integers(2, size - 2)
        ang = rng.random() * np.pi * 2
        steps = rng.integers(8, 20)
        for _ in range(steps):
            ang += (rng.random() - 0.5) * 1.1
            x = int(np.clip(x + np.cos(ang) * 1.4, 0, size - 1))
            y = int(np.clip(y + np.sin(ang) * 1.4, 0, size - 1))
            field[y, x] = 1.0
            if rng.random() < 0.3:
                field[min(y + 1, size - 1), x] = max(field[min(y + 1, size - 1), x], 0.5)
    return field


def save(name, rgb):
    img = np.clip(rgb, 0, 255).astype(np.uint8)
    alpha = np.full((SIZE, SIZE, 1), 255, dtype=np.uint8)
    Image.fromarray(np.concatenate([img, alpha], axis=2), "RGBA").save(
        os.path.join(ROOT, name + ".png"))
    print("wrote", name)


ROCK_DARK = [(0.0, (18, 13, 10)), (0.45, (34, 26, 20)), (0.75, (48, 37, 28)), (1.0, (62, 48, 36))]
MOLTEN = [(0.0, (24, 16, 12)), (0.30, (58, 26, 14)), (0.48, (166, 52, 4)),
          (0.62, (230, 92, 0)), (0.78, (255, 152, 16)), (0.92, (255, 208, 63)), (1.0, (255, 236, 150))]


def molten_top(seed):
    rng = np.random.default_rng(seed)
    n = fbm(rng)
    # Mostly molten: the lava field dominates, dark islands where noise is low.
    field = np.clip(0.25 + n * 0.95, 0, 1)
    rgb = ramp(MOLTEN)(field)
    speckle = grid_noise(rng, 16) * 14 - 7
    return rgb + speckle[..., None]


def molten_side(seed):
    rng = np.random.default_rng(seed)
    n = fbm(rng)
    rgb = ramp(ROCK_DARK)(n)
    # A molten band along the top that drips down in tongues.
    yy = np.linspace(0, 1, SIZE)[:, None]
    drip = 0.16 + 0.30 * grid_noise(rng, 5)[0:1, :]
    band = np.clip((drip - yy) * 6.0, 0, 1) * (0.55 + 0.45 * fbm(rng))
    lava = ramp(MOLTEN)(np.clip(0.45 + band * 0.6, 0, 1))
    return rgb * (1 - band[..., None]) + lava * band[..., None]


def cooling_top(seed):
    rng = np.random.default_rng(seed)
    n = fbm(rng)
    # Dark plates take over; lava survives in patches and crack lines.
    plates = ramp(ROCK_DARK)(np.clip(n * 1.15, 0, 1))
    ck = cracks(rng, 9)
    glow = ramp(MOLTEN)(np.clip(0.35 + ck * 0.55 + fbm(rng) * 0.15, 0, 1))
    mask = np.clip(ck * 1.4 + np.clip(n - 0.78, 0, 1) * 1.6, 0, 1)
    return plates * (1 - mask[..., None]) + glow * mask[..., None]


def cooling_side(seed):
    rng = np.random.default_rng(seed)
    n = fbm(rng)
    rgb = ramp(ROCK_DARK)(n)
    ck = cracks(rng, 4)
    glow = ramp(MOLTEN)(np.clip(0.3 + ck * 0.5, 0, 1))
    return rgb * (1 - ck[..., None] * 0.85) + glow * ck[..., None] * 0.85


def dark_top(seed):
    rng = np.random.default_rng(seed)
    n = fbm(rng)
    rgb = ramp(ROCK_DARK)(np.clip(n * 1.1 - 0.08, 0, 1))
    ck = cracks(rng, 7)
    # Hairline glow only: the seams still breathe heat but the surface is rock.
    seam = np.array([255, 106, 0], dtype=np.float64)
    dim = np.array([140, 44, 0], dtype=np.float64)
    layer = dim[None, None, :] * (1 - ck[..., None]) + seam[None, None, :] * ck[..., None]
    return rgb * (1 - ck[..., None]) + layer * ck[..., None]


def dark_side(seed):
    rng = np.random.default_rng(seed)
    n = fbm(rng)
    rgb = ramp(ROCK_DARK)(np.clip(n * 1.1 - 0.08, 0, 1))
    ck = cracks(rng, 2)
    seam = np.array([120, 38, 0], dtype=np.float64)
    return rgb * (1 - ck[..., None] * 0.7) + seam[None, None, :] * ck[..., None] * 0.7


def volcano_rock(seed):
    rng = np.random.default_rng(seed)
    n = fbm(rng)
    stops = [(0.0, (26, 18, 13)), (0.4, (52, 36, 24)), (0.7, (74, 52, 34)), (1.0, (96, 70, 46))]
    rgb = ramp(stops)(n)
    speckle = grid_noise(rng, 16) * 16 - 8
    return rgb + speckle[..., None]


def main():
    os.makedirs(ROOT, exist_ok=True)
    save("magma_crust_molten_top", molten_top(1101))
    save("magma_crust_molten_side", molten_side(1102))
    save("magma_crust_cooling_top", cooling_top(1103))
    save("magma_crust_cooling_side", cooling_side(1104))
    save("magma_crust_dark_top", dark_top(1105))
    save("magma_crust_dark_side", dark_side(1106))
    save("mini_volcano_rock", volcano_rock(1107))


if __name__ == "__main__":
    main()
