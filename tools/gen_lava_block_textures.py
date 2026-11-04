#!/usr/bin/env python3
"""Generate the Lava Sword block textures.

The first Lava Sword blocks used small muddy noise textures and dotted
single-pixel cracks. This generator writes 64 px block textures with
periodic fractal noise, two scales of Voronoi crack networks, convection
cells, basalt plate shading, ash, pores, and stage-specific heat. The
crust side textures are full block-height cross-sections: the models
sample the band that matches each element's real height.

Run: python3 tools/gen_lava_block_textures.py
"""
import os
import zlib

import numpy as np
from PIL import Image, ImageFilter

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src", "main", "resources", "assets", "dragonblockarcanedba")
TEXTURE_ROOT = os.path.join(ROOT, "textures", "block")
SIZE = 64


def stable_seed(name):
    return zlib.crc32(name.encode("utf-8")) & 0x7FFFFFFF


def rng_for(name):
    return np.random.default_rng(stable_seed(name))


def grid_noise(rng, cells_x, cells_y=None, size=SIZE):
    if cells_y is None:
        cells_y = cells_x
    grid = rng.random((cells_y + 1, cells_x + 1))
    grid[-1, :] = grid[0, :]
    grid[:, -1] = grid[:, 0]
    img = Image.fromarray((grid * 255).astype(np.uint8)).resize(
        (size, size), Image.BILINEAR)
    return np.asarray(img, dtype=np.float64) / 255.0


def fbm(rng, size=SIZE):
    return (0.40 * grid_noise(rng, 3, 3, size)
            + 0.27 * grid_noise(rng, 6, 6, size)
            + 0.20 * grid_noise(rng, 12, 12, size)
            + 0.13 * grid_noise(rng, 24, 24, size))


def anisotropic_fbm(rng, size=SIZE):
    return (0.44 * grid_noise(rng, 16, 4, size)
            + 0.30 * grid_noise(rng, 28, 8, size)
            + 0.26 * grid_noise(rng, 44, 16, size))


def ridged(rng, size=SIZE):
    return 1.0 - np.abs(2.0 * fbm(rng, size) - 1.0)


def smoothstep(a, b, x):
    t = np.clip((x - a) / (b - a), 0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)


def lerp(a, b, t):
    return a + (b - a) * t[..., None]


def ramp(field, stops):
    """Interpolate an RGB ramp. Stops are (position, (r, g, b)) pairs."""
    out = np.zeros(field.shape + (3,), dtype=np.float64)
    for i in range(len(stops) - 1):
        p0, c0 = stops[i]
        p1, c1 = stops[i + 1]
        mask = (field >= p0) & (field <= p1)
        t = np.zeros_like(field)
        t[mask] = (field[mask] - p0) / max(1.0e-6, p1 - p0)
        out[mask] = np.array(c0) * (1.0 - t[mask, None]) + np.array(c1) * t[mask, None]
    out[field < stops[0][0]] = stops[0][1]
    out[field > stops[-1][0]] = stops[-1][1]
    return out


def voronoi(rng, points, size=SIZE):
    """Periodic Voronoi F1, F2, and a stable random value per cell."""
    y, x = np.mgrid[0:size, 0:size]
    x = x / size
    y = y / size
    pts = rng.random((points, 2))
    values = rng.random(points)
    d1 = np.full((size, size), 9.0)
    d2 = np.full((size, size), 9.0)
    nearest = np.zeros((size, size), dtype=np.float64)
    for oy in (-1, 0, 1):
        for ox in (-1, 0, 1):
            for i, (px, py) in enumerate(pts):
                dx = x - (px + ox)
                dy = y - (py + oy)
                d = dx * dx + dy * dy
                closer = d < d1
                d2 = np.where(closer, d1, np.minimum(d2, d))
                nearest = np.where(closer, values[i], nearest)
                d1 = np.where(closer, d, d1)
    return np.sqrt(d1), np.sqrt(np.maximum(d2, d1)), nearest


def blur_field(field, radius=0.7):
    img = Image.fromarray((np.clip(field, 0.0, 1.0) * 255).astype(np.uint8))
    return np.asarray(img.filter(ImageFilter.GaussianBlur(radius)), dtype=np.float64) / 255.0


def finish(rgb, rng, grain=0.10, pores=0.0, protect=None):
    rgb = np.clip(rgb, 0.0, 1.0)
    fine = grid_noise(rng, 48)
    grain_field = grain * (fine - 0.5)
    if protect is not None:
        grain_field *= (1.0 - np.clip(protect, 0.0, 1.0) * 0.88)
    rgb *= (1.0 + grain_field[..., None])
    if pores:
        pore = smoothstep(0.965, 0.995, grid_noise(rng, 40)) * pores
        if protect is not None:
            pore *= (1.0 - np.clip(protect, 0.0, 1.0))
        rgb = lerp(rgb, np.array([0.015, 0.010, 0.008]), pore)
    return Image.fromarray((np.clip(rgb, 0.0, 1.0) * 255).astype(np.uint8), "RGB")


BASALT = [(0.0, (0.018, 0.014, 0.012)),
          (0.35, (0.075, 0.058, 0.047)),
          (0.68, (0.155, 0.118, 0.092)),
          (1.0, (0.285, 0.224, 0.176))]
LAVA = [(0.0, (0.20, 0.018, 0.0)),
        (0.28, (0.48, 0.055, 0.0)),
        (0.52, (0.82, 0.20, 0.01)),
        (0.74, (1.0, 0.48, 0.02)),
        (0.90, (1.0, 0.76, 0.16)),
        (1.0, (1.0, 0.96, 0.66))]
GLOW = [(0.0, (0.10, 0.008, 0.0)),
        (0.42, (0.55, 0.055, 0.0)),
        (0.72, (0.94, 0.27, 0.01)),
        (1.0, (1.0, 0.88, 0.42))]


def crust_top(stage, plate, name):
    rng = rng_for(name)
    large_f1, large_f2, cell_heat = voronoi(rng, 15)
    small_f1, small_f2, _ = voronoi(rng, 38)
    flow = fbm(rng)
    ridge = ridged(rng)
    # Keep the raw edge-distance fields. Blurring them (tried first)
    # spreads near-zero values around crack junctions, so groove masks
    # merged into wide black blobs. The exponential and smoothstep
    # falloffs below are already smooth; the finish pass protects glow
    # from grain, which was the real source of the old dotted look.
    edge = large_f2 - large_f1
    fine_edge = small_f2 - small_f1
    crack = smoothstep(0.055, 0.006, edge)
    fine_crack = smoothstep(0.035, 0.004, fine_edge)
    cell_shade = 0.30 + 0.52 * cell_heat + 0.28 * flow

    if stage == 0 and not plate:
        heat = np.clip(0.18 + 0.46 * flow + 0.34 * ridge + 0.28 * cell_heat
                       + 0.38 * crack + 0.16 * fine_crack, 0.0, 1.0)
        rgb = ramp(heat, LAVA)
        # Dark basalt rafts float in the flow. Their rims catch the heat;
        # their interiors stay coarse, pitted charcoal.
        raft = ((cell_heat > 0.70) & (edge > 0.030)
                & (flow > 0.42)).astype(np.float64)
        raft = np.clip(raft + smoothstep(0.030, 0.055, edge) * (cell_heat > 0.70), 0.0, 1.0)
        rock = ramp(np.clip(cell_shade * 0.55 + ridge * 0.16, 0.0, 1.0), BASALT)
        rgb = lerp(rgb, rock, raft * 0.92)
        rim = smoothstep(0.075, 0.030, edge) * (cell_heat > 0.60)
        rgb = lerp(rgb, np.array([1.0, 0.34, 0.015]), rim * 0.48)
        bubbles = smoothstep(0.955, 0.995, grid_noise(rng, 44)) * (1.0 - raft)
        rgb = lerp(rgb, np.array([1.0, 0.88, 0.42]), bubbles * 0.55)
        return finish(rgb, rng, grain=0.13, pores=0.30)

    # Raised plates are always cooler and darker than the surface between
    # them. Stage 0 plates use the same art family as its floating rafts.
    basalt_field = np.clip(cell_shade * (0.72 if plate else 0.84)
                           + ridge * 0.18 - (0.03 if plate else 0.0), 0.0, 1.0)
    if plate:
        # Keep every plate readable as rock. Without a floor, one low
        # random cell becomes a flat black void at block scale. The
        # cooled stage needs the highest floor: it has no glow left to
        # separate one plate from the next.
        if stage == 2:
            # Compress the whole range upward: the cooled plate has no
            # glow to separate plates, so a low cell at the ramp's dark
            # end reads as a black hole next to its neighbours in game.
            basalt_field = 0.45 + 0.55 * basalt_field
        else:
            basalt_field = 0.18 + 0.82 * basalt_field
    rgb = ramp(basalt_field, BASALT)
    ash = smoothstep(0.62, 0.82, fbm(rng)) * (0.20 if stage == 1 else 0.13)
    rgb = lerp(rgb, np.array([0.46, 0.42, 0.37]), ash)

    if stage == 0:
        groove = smoothstep(0.020, 0.004, edge)
        rgb = lerp(rgb, np.array([0.015, 0.008, 0.006]), groove * 0.55)
        glow = np.clip(np.exp(-edge / 0.020) * 1.15
                       + np.exp(-fine_edge / 0.012) * 0.38, 0.0, 1.0)
        if plate:
            # A raised raft's own fractures are mostly cold. Only short
            # stretches still show heat, unlike the live surface below.
            glow *= 0.34 * smoothstep(0.42, 0.72, flow)
        rgb = lerp(rgb, ramp(glow, GLOW), np.clip(glow * 0.85, 0.0, 1.0))
        return finish(rgb, rng, grain=0.15, pores=0.46, protect=glow)
    elif stage == 1:
        glow_strength = 0.30 if plate else 0.92
        # A dark groove sits under the light so cracks have width and a
        # hot core, instead of the old dotted orange chain.
        groove = smoothstep(0.045, 0.008, edge)
        rgb = lerp(rgb, np.array([0.012, 0.008, 0.006]), groove * 0.72)
        glow = np.clip(np.exp(-edge / 0.017) * 1.22
                       + np.exp(-fine_edge / 0.010) * 0.30, 0.0, 1.0)
        rgb = lerp(rgb, ramp(glow, GLOW), np.clip(glow * glow_strength, 0.0, 1.0))
        rim = smoothstep(0.075, 0.038, edge) * (1.0 - smoothstep(0.038, 0.012, edge))
        rgb = lerp(rgb, np.array([0.34, 0.16, 0.055]), rim * 0.30)
        return finish(rgb, rng, grain=0.15, pores=0.46, protect=glow)
    else:
        # Narrow, shallow groove. Where several small cells cluster,
        # most pixels sit near a boundary, so a deep groove covers the
        # whole patch and reads as a black void instead of fractured
        # rock (measured: one such patch went from 54 to 11 luminance).
        groove = smoothstep(0.016, 0.003, edge)
        rgb = lerp(rgb, np.array([0.004, 0.003, 0.003]),
                   groove * (0.58 if plate else 0.68))
        # Only separated stretches of the cooled seams still hold embers.
        gate = smoothstep(0.56, 0.76, flow) * smoothstep(0.50, 0.72, ridge)
        glow = np.clip(np.exp(-edge / 0.010) * gate
                       + np.exp(-fine_edge / 0.007) * gate * 0.35, 0.0, 1.0)
        ember_color = ramp(np.clip(glow * 1.25, 0.0, 1.0), GLOW)
        rgb = lerp(rgb, ember_color, np.clip(glow * (0.42 if plate else 0.78), 0.0, 1.0))
        rgb *= 0.92 if plate else 0.88
    return finish(rgb, rng, grain=0.16, pores=0.52, protect=glow)


def crust_side(stage, name):
    rng = rng_for(name)
    y, x = np.mgrid[0:SIZE, 0:SIZE]
    strata = anisotropic_fbm(rng)
    vertical = grid_noise(rng, 30, 6)
    rgb = ramp(np.clip(0.24 + 0.58 * strata + 0.22 * vertical, 0.0, 1.0), BASALT)
    # Fine horizontal bedding runs through the whole cross-section. The
    # bottom quarter is the crust band the 0 to 6 high models sample.
    bedding = 0.5 + 0.5 * np.sin(y * 1.75 + strata * 9.0)
    rgb *= (0.88 + 0.20 * bedding[..., None])
    crust_band = smoothstep(37.0, 41.0, y)
    top_heat = np.exp(-np.maximum(0.0, y - 40.0) / (5.0 + stage * 2.5)) * crust_band
    drips = (smoothstep(0.58, 0.86, vertical)
             * np.exp(-np.maximum(0.0, y - 40.0) / 15.0) * crust_band)
    cracks = smoothstep(0.72, 0.94, ridged(rng)) * crust_band
    if stage == 0:
        heat = np.clip(top_heat * 1.15 + drips * 0.72 + cracks * 0.42, 0.0, 1.0)
        rgb = lerp(rgb, ramp(heat, GLOW), np.clip(heat * 0.86, 0.0, 1.0))
    elif stage == 1:
        heat = np.clip(top_heat * 0.72 + drips * 0.32 + cracks * 0.28, 0.0, 1.0)
        rgb = lerp(rgb, ramp(heat, GLOW), np.clip(heat * 0.62, 0.0, 1.0))
    else:
        gate = smoothstep(0.60, 0.82, fbm(rng))
        heat = np.clip((top_heat * 0.30 + cracks * 0.38) * gate, 0.0, 1.0)
        rgb = lerp(rgb, ramp(heat, GLOW), np.clip(heat * 0.65, 0.0, 1.0))
        rgb *= 0.92
        # The cooled side still needs visible bedding at block scale.
        rgb += ((strata - 0.5) * 0.10 * crust_band)[..., None]
    return finish(rgb, rng, grain=0.15, pores=0.42)


def volcano_rock(name, dark=False, ash_heavy=False):
    rng = rng_for(name)
    f1, f2, cell = voronoi(rng, 24)
    f1 = blur_field(f1, 0.55)
    f2 = blur_field(f2, 0.55)
    fine_f1, fine_f2, _ = voronoi(rng, 52)
    fine_f1 = blur_field(fine_f1, 0.45)
    fine_f2 = blur_field(fine_f2, 0.45)
    flow = fbm(rng)
    strata = anisotropic_fbm(rng)
    edge = f2 - f1
    field = np.clip(0.16 + 0.42 * cell + 0.30 * flow + 0.25 * strata, 0.0, 1.0)
    rgb = ramp(field, BASALT)
    groove = smoothstep(0.040, 0.006, edge)
    rgb = lerp(rgb, np.array([0.010, 0.008, 0.007]), groove * 0.66)
    fine_groove = smoothstep(0.025, 0.003, fine_f2 - fine_f1)
    rgb = lerp(rgb, np.array([0.020, 0.016, 0.013]), fine_groove * 0.30)
    bedding = 0.5 + 0.5 * np.sin(np.mgrid[0:SIZE, 0:SIZE][0] * 1.35 + strata * 10.0)
    rgb *= (0.86 + 0.24 * bedding[..., None])
    ash_mask = smoothstep(0.57, 0.80, fbm(rng)) * (0.62 if ash_heavy else 0.34)
    rgb = lerp(rgb, np.array([0.52, 0.485, 0.43]), ash_mask)
    oxide = smoothstep(0.68, 0.88, fbm(rng)) * 0.24
    rgb = lerp(rgb, np.array([0.34, 0.15, 0.065]), oxide)
    if dark:
        rgb *= 0.52
    return finish(rgb, rng, grain=0.17, pores=0.55)


def volcano_channel(name):
    rng = rng_for(name)
    rgb = np.asarray(volcano_rock(name + "-base"), dtype=np.float64) / 255.0
    y, x = np.mgrid[0:SIZE, 0:SIZE]
    wobble = grid_noise(rng, 2, 8)[:, 0][:, None]
    center = 32.0 + 2.0 * np.sin(y / SIZE * np.pi * 2.0 + 0.7) + (wobble - 0.5) * 3.0
    dist = np.abs(x - center)
    # Dark solidified banks frame the flow before the heat ramp begins.
    banks = smoothstep(15.0, 9.0, dist) * (1.0 - smoothstep(9.0, 4.5, dist))
    rgb = lerp(rgb, np.array([0.025, 0.018, 0.014]), banks * 0.72)
    glow = np.clip(np.exp(-dist / 8.5) * 1.12, 0.0, 1.0)
    rgb = lerp(rgb, ramp(glow, GLOW), np.clip(glow * 0.96, 0.0, 1.0))
    mid = smoothstep(9.0, 3.5, dist)
    rgb = lerp(rgb, np.array([1.0, 0.46, 0.025]), mid * 0.52)
    core = smoothstep(4.2, 1.0, dist)
    rgb = lerp(rgb, np.array([1.0, 0.86, 0.38]), core * 0.88)
    islands = smoothstep(0.82, 0.96, grid_noise(rng, 28)) * smoothstep(3.8, 1.4, dist)
    rgb = lerp(rgb, np.array([0.018, 0.012, 0.010]), islands * 0.62)
    protect = np.clip(glow + core, 0.0, 1.0)
    return finish(rgb, rng, grain=0.12, pores=0.25, protect=protect)


def volcano_crater(name):
    rng = rng_for(name)
    y, x = np.mgrid[0:SIZE, 0:SIZE]
    cx = (x - SIZE / 2 + 0.5) / (SIZE / 2)
    cy = (y - SIZE / 2 + 0.5) / (SIZE / 2)
    r = np.sqrt(cx * cx + cy * cy)
    flow = fbm(rng)
    # This texture is used on the depressed pool surface. Its middle is
    # live lava; its broken edge is the dark crust that meets the rim.
    heat = np.clip(0.26 + 0.48 * flow + 0.36 * ridged(rng), 0.0, 1.0)
    rgb = ramp(heat, LAVA)
    # The pool surface is square in the model, so its crust edge follows
    # a noise-broken square distance rather than a clean painted circle.
    square_r = np.maximum(np.abs(cx), np.abs(cy))
    edge = smoothstep(0.70, 0.96, square_r + (flow - 0.5) * 0.26)
    rgb = lerp(rgb, ramp(np.clip(0.20 + 0.45 * flow, 0.0, 1.0), BASALT), edge)
    center = smoothstep(0.34, 0.05, r)
    rgb = lerp(rgb, np.array([1.0, 0.82, 0.30]), center * 0.38)
    return finish(rgb, rng, grain=0.13, pores=0.30)


def save(image, filename):
    image.save(os.path.join(TEXTURE_ROOT, filename))
    print("wrote", filename)


def main():
    os.makedirs(TEXTURE_ROOT, exist_ok=True)
    stage_names = ["molten", "cooling", "dark"]
    for stage, label in enumerate(stage_names):
        save(crust_top(stage, False, f"magma_crust_{label}_top"),
             f"magma_crust_{label}_top.png")
        save(crust_top(stage, True, f"magma_crust_{label}_plate"),
             f"magma_crust_{label}_plate.png")
        save(crust_side(stage, f"magma_crust_{label}_side"),
             f"magma_crust_{label}_side.png")
        for direction in ("north", "east", "south", "west"):
            save(crust_side(stage, f"magma_crust_{label}_side_{direction}"),
                 f"magma_crust_{label}_side_{direction}.png")

    save(volcano_rock("mini_volcano_rock"), "mini_volcano_rock.png")
    save(volcano_rock("mini_volcano_rock_east"), "mini_volcano_rock_east.png")
    save(volcano_rock("mini_volcano_rock_west"), "mini_volcano_rock_west.png")
    save(volcano_rock("mini_volcano_dark", dark=True), "mini_volcano_dark.png")
    save(volcano_rock("mini_volcano_ash", ash_heavy=True), "mini_volcano_ash.png")
    save(volcano_channel("mini_volcano_channel"), "mini_volcano_channel.png")
    save(volcano_crater("mini_volcano_crater"), "mini_volcano_crater.png")


if __name__ == "__main__":
    main()
