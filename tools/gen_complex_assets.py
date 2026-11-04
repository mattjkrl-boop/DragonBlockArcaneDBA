#!/usr/bin/env python3
"""Generate the complex effects redo assets.

Two families, both seeded and irregular by construction (COMPLEX-STANDARD):

- Five complex particle sprite sets at 128 px, eight frames, written to
  textures/particle with their particles/*.json: shard_chip (hard polygon
  fragments with facet shades), spark_streak (tapered slivers with a hot
  core), wisp_curl (spiral comma bands), ember_fragment (dark crust around
  a bright cracked core), ash_flake (small multi-lobe clusters). No set is
  a radial gradient disc; frames vary the silhouette itself.
- Two geometry canvas textures at 128 px, written to textures/entity/fx:
  flame_tongue.png (one tall torn flame silhouette, wide base at the
  bottom of the file, streaked core) and tendril.png (a horizontal wisp
  strip, torn ridged edges, tapered ends). They are white intensity
  images: vertex color carries the theme.

Run: python3 tools/gen_complex_assets.py
"""
import json
import math
import os
import zlib

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src", "main", "resources", "assets", "dragonblockarcanedba")
TEXTURE_ROOT = os.path.join(ROOT, "textures", "particle")
PARTICLE_ROOT = os.path.join(ROOT, "particles")
FX_ROOT = os.path.join(ROOT, "textures", "entity", "fx")
SIZE = 128
FRAMES = 8
CENTER = SIZE / 2.0


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
    y, x = np.mgrid[0:SIZE, 0:SIZE]
    dx = (x - cx) / max(1.0, radius)
    dy = (y - cy) / max(1.0, radius * (0.55 + rng.random() * 0.8))
    d = dx * dx + dy * dy
    fleck = np.exp(-d * (2.2 + rng.random() * 2.0))
    fleck *= 0.75 + irregular * rng.random()
    field[:] = np.maximum(field, fleck * strength)


def jagged_polygon(rng, radius_min, radius_max, count_min=5, count_max=7):
    """A convex-ish irregular polygon: sorted angles, jittered radii."""
    count = int(rng.integers(count_min, count_max + 1))
    base = rng.random() * math.pi * 2.0
    angles = sorted(base + (math.pi * 2.0) * (i + rng.random() * 0.55) / count
                    for i in range(count))
    pts = []
    for ang in angles:
        rad = radius_min + rng.random() * (radius_max - radius_min)
        pts.append((CENTER + math.cos(ang) * rad, CENTER + math.sin(ang) * rad))
    return pts


def polygon_sprite(rng, pts, facet=True):
    """Rasterize a polygon with facet shading: an off-center interior
    point fans the polygon into triangles shaded in three levels, and
    the two edges facing the light get a bright rim on the intensity."""
    alpha = Image.new("L", (SIZE, SIZE), 0)
    lum = Image.new("L", (SIZE, SIZE), 0)
    da = ImageDraw.Draw(alpha)
    dl = ImageDraw.Draw(lum)
    da.polygon(pts, fill=255)
    dl.polygon(pts, fill=150)
    if facet:
        ix = CENTER + (rng.random() - 0.5) * 22.0
        iy = CENTER + (rng.random() - 0.5) * 22.0
        light = rng.random() * math.pi * 2.0
        lx, ly = math.cos(light), math.sin(light)
        n = len(pts)
        for i in range(n):
            x0, y0 = pts[i]
            x1, y1 = pts[(i + 1) % n]
            mx, my = (x0 + x1) / 2.0 - CENTER, (y0 + y1) / 2.0 - CENTER
            facing = (mx * lx + my * ly) / max(1.0, math.hypot(mx, my))
            shade = int(150 + facing * 62 + (rng.random() - 0.5) * 26)
            dl.polygon([(ix, iy), (x0, y0), (x1, y1)], fill=max(70, min(225, shade)))
            if facing > 0.45:
                dl.line([(x0, y0), (x1, y1)], fill=238, width=2)
    rgba = np.zeros((SIZE, SIZE, 4), dtype=np.uint8)
    rgba[..., 0] = np.asarray(lum)
    rgba[..., 1] = np.asarray(lum)
    rgba[..., 2] = np.asarray(lum)
    rgba[..., 3] = np.asarray(alpha)
    return Image.fromarray(rgba, "RGBA")


def shard_chip(frame, rng):
    pts = jagged_polygon(rng, 26.0, 50.0)
    return polygon_sprite(rng, pts, facet=True)


def ember_fragment(frame, rng):
    pts = jagged_polygon(rng, 13.0, 25.0, count_min=6, count_max=8)
    alpha = Image.new("L", (SIZE, SIZE), 0)
    lum = Image.new("L", (SIZE, SIZE), 0)
    da = ImageDraw.Draw(alpha)
    dl = ImageDraw.Draw(lum)
    da.polygon(pts, fill=255)
    dl.polygon(pts, fill=58)  # dark crust
    inner = [(CENTER + (x - CENTER) * 0.64 + (rng.random() - 0.5) * 3.0,
              CENTER + (y - CENTER) * 0.64 + (rng.random() - 0.5) * 3.0)
             for (x, y) in pts]
    dl.polygon(inner, fill=232)  # hot cracked core
    for _ in range(2):
        ang = rng.random() * math.pi * 2.0
        ex = CENTER + math.cos(ang) * 12.0
        ey = CENTER + math.sin(ang) * 12.0
        dl.line([(CENTER, CENTER), (ex, ey)], fill=40, width=2)
    rgba = np.zeros((SIZE, SIZE, 4), dtype=np.uint8)
    rgba[..., 0] = np.asarray(lum)
    rgba[..., 1] = np.asarray(lum)
    rgba[..., 2] = np.asarray(lum)
    rgba[..., 3] = np.asarray(alpha)
    img = Image.fromarray(rgba, "RGBA")
    return img.filter(ImageFilter.GaussianBlur(0.6))


def spark_streak(frame, rng):
    ang = frame * (math.pi / 8.0) + (rng.random() - 0.5) * 0.35
    dx, dy = math.cos(ang), math.sin(ang)
    px, py = -dy, dx
    length = 92.0 + rng.random() * 22.0
    half_w = 5.0 + rng.random() * 3.5
    head = (CENTER + dx * length * 0.55, CENTER + dy * length * 0.55)
    tail = (CENTER - dx * length * 0.45, CENTER - dy * length * 0.45)
    mid_a = (CENTER + dx * length * 0.10 + px * half_w, CENTER + dy * length * 0.10 + py * half_w)
    mid_b = (CENTER - dx * length * 0.18 - px * half_w, CENTER - dy * length * 0.18 - py * half_w)
    outer = [tail, mid_b, head, mid_a]
    alpha = Image.new("L", (SIZE, SIZE), 0)
    lum = Image.new("L", (SIZE, SIZE), 0)
    da = ImageDraw.Draw(alpha)
    dl = ImageDraw.Draw(lum)
    da.polygon(outer, fill=255)
    dl.polygon(outer, fill=168)
    core = [(CENTER + (x - CENTER) * 0.42, CENTER + (y - CENTER) * 0.42) for (x, y) in outer]
    dl.polygon(core, fill=255)
    # one small barb near the head, on a seeded side
    side = 1.0 if rng.random() > 0.5 else -1.0
    barb_base = (CENTER + dx * length * 0.28, CENTER + dy * length * 0.28)
    barb_tip = (barb_base[0] + dx * 9.0 + px * side * 13.0,
                barb_base[1] + dy * 9.0 + py * side * 13.0)
    barb_back = (CENTER + dx * length * 0.13, CENTER + dy * length * 0.13)
    da.polygon([barb_base, barb_tip, barb_back], fill=255)
    dl.polygon([barb_base, barb_tip, barb_back], fill=205)
    rgba = np.zeros((SIZE, SIZE, 4), dtype=np.uint8)
    rgba[..., 0] = np.asarray(lum)
    rgba[..., 1] = np.asarray(lum)
    rgba[..., 2] = np.asarray(lum)
    rgba[..., 3] = np.asarray(alpha)
    img = Image.fromarray(rgba, "RGBA")
    return img.filter(ImageFilter.GaussianBlur(0.9))


def wisp_curl(frame, rng):
    field = np.zeros((SIZE, SIZE))
    core = np.zeros((SIZE, SIZE))
    a0 = rng.random() * math.pi * 2.0
    sweep = 3.9 + rng.random() * 0.9
    steps = 42
    for i in range(steps):
        t = i / (steps - 1)
        ang = a0 + t * sweep
        rad = 5.0 + t * 40.0
        x = CENTER + math.cos(ang) * rad
        y = CENTER + math.sin(ang) * rad
        stamp(field, x, y, 10.5 * (1.0 - t) + 2.0, 0.88 * (1.0 - 0.5 * t), rng)
        stamp(core, x, y, 4.5 * (1.0 - t) + 1.0, 1.0 * (1.0 - 0.4 * t), rng)
    alpha = smoothstep(0.16, 0.78, field)
    intensity = np.clip(field * 0.55 + core * 0.75, 0.0, 1.0)
    return to_image(alpha, intensity)


def ash_flake(frame, rng):
    field = np.zeros((SIZE, SIZE))
    for _ in range(6):
        stamp(field, CENTER + (rng.random() - 0.5) * 30.0,
              CENTER + (rng.random() - 0.5) * 30.0,
              8.0 + rng.random() * 13.0, 0.66, rng, irregular=0.65)
    stamp(field, CENTER + (rng.random() - 0.5) * 14.0,
          CENTER + (rng.random() - 0.5) * 14.0, 6.0, 0.95, rng)
    alpha = smoothstep(0.08, 0.55, field) * 0.95
    return to_image(alpha, 0.42 + 0.42 * field)


def flame_tongue_canvas(rng):
    cx, cy, r, _ = coords()
    n = fbm(rng)
    ridge = ridged(rng)
    h = np.clip((cy + 1.0) * 0.5, 0.0, 1.0)  # 0 top (tip), 1 bottom (base)
    wobble = (n - 0.5) * 0.34
    xr = cx + wobble * (0.30 + (1.0 - h))
    width = 0.04 + 0.62 * np.power(h, 0.60) + 0.10 * ridge
    body = smoothstep(width, width * 0.15, np.abs(xr))
    tear = smoothstep(0.42, 0.92, ridge) * smoothstep(0.55, 0.05, h) * 1.05
    side_licks = smoothstep(0.60, 0.95, ridged(rng)) * smoothstep(0.35, 0.75, h) * (1.0 - smoothstep(0.75, 1.0, h)) * 0.5
    alpha = np.clip(body * (0.55 + 0.45 * n) - tear * body - side_licks * body, 0.0, 1.0)
    alpha = smoothstep(0.08, 0.50, alpha)
    core = body * smoothstep(0.55, 0.06, np.abs(xr)) * (0.45 + 0.55 * h)
    return to_image(alpha, np.clip(core + alpha * 0.30, 0.0, 1.0))


def tendril_canvas(rng):
    cx, cy, _, _ = coords()
    n = fbm(rng)
    ridge = ridged(rng)
    band = 0.13 + 0.22 * ridge
    body = smoothstep(band, band * 0.2, np.abs(cy + (n - 0.5) * 0.38))
    ends = smoothstep(1.02, 0.50, np.abs(cx))
    streaks = 0.5 + 0.5 * np.sin(cx * 11.0 + n * 12.0)
    alpha = body * ends * (0.30 + 0.45 * n) + body * ends * np.power(streaks, 2.0) * 0.55
    alpha = smoothstep(0.08, 0.55, alpha)
    core = body * smoothstep(0.30, 0.02, np.abs(cy)) * ends
    return to_image(alpha, np.clip(core * 0.9 + alpha * 0.30, 0.0, 1.0))


SPRITES = {
    "shard_chip": shard_chip,
    "spark_streak": spark_streak,
    "wisp_curl": wisp_curl,
    "ember_fragment": ember_fragment,
    "ash_flake": ash_flake,
}


def main():
    os.makedirs(TEXTURE_ROOT, exist_ok=True)
    os.makedirs(PARTICLE_ROOT, exist_ok=True)
    os.makedirs(FX_ROOT, exist_ok=True)
    for name, maker in SPRITES.items():
        rng = np.random.default_rng(stable_seed(name))
        for frame in range(FRAMES):
            maker(frame, rng).save(os.path.join(TEXTURE_ROOT, f"{name}_{frame}.png"))
        payload = {"textures": [f"dragonblockarcanedba:{name}_{i}" for i in range(FRAMES)]}
        with open(os.path.join(PARTICLE_ROOT, f"{name}.json"), "w") as fh:
            json.dump(payload, fh, indent=2)
            fh.write("\n")
        print("wrote sprite", name, "x", FRAMES)
    rng = np.random.default_rng(stable_seed("flame_tongue_canvas"))
    flame_tongue_canvas(rng).save(os.path.join(FX_ROOT, "flame_tongue.png"))
    print("wrote canvas flame_tongue.png")
    rng = np.random.default_rng(stable_seed("tendril_canvas"))
    tendril_canvas(rng).save(os.path.join(FX_ROOT, "tendril.png"))
    print("wrote canvas tendril.png")


if __name__ == "__main__":
    main()
