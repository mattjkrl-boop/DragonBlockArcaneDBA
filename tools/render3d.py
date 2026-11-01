#!/usr/bin/env python3
"""
Real 3D renderer for DBA GeckoLib geo models. The sandbox cannot run Minecraft, so this
renders the actual model files the next best way: full perspective projection, bone
hierarchy with pivots, per-cube rotations, texture atlas sampling, simple directional
shading, and animation poses sampled from the weapon's own animation file.

Usage (from repo root):
  python3 tools/render3d.py <weapon_id> [--anim idle|swing|charge] [--t 0.15]
                            [--views three_quarter,side,edge,held] [--out DIR]
Writes <weapon_id>_3d_<view>.png per view into renders/3d/ (or --out).
"""
import argparse
import json
import math
import os

import numpy as np
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/dragonblockarcanedba")


def rot_matrix(rx, ry, rz):
    """Rotation matrix, degrees, applied Z then Y then X is wrong for bedrock;
    GeckoLib/bedrock order is Z, Y, X intrinsic-ish. We use R = Rx @ Ry @ Rz."""
    ax, ay, az = map(math.radians, (rx, ry, rz))
    cx, sx = math.cos(ax), math.sin(ax)
    cy, sy = math.cos(ay), math.sin(ay)
    cz, sz = math.cos(az), math.sin(az)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Rx @ Ry @ Rz


def sample_track(track, t):
    """track: dict time-string -> vec3 (or scalar list). Linear interpolation, wraps."""
    keys = sorted(track.keys(), key=lambda k: float(k))
    times = [float(k) for k in keys]
    vals = [np.array(track[k], dtype=float) for k in keys]
    if t <= times[0]:
        return vals[0]
    if t >= times[-1]:
        return vals[-1]
    for i in range(len(times) - 1):
        if times[i] <= t <= times[i + 1]:
            f = (t - times[i]) / max(1e-9, times[i + 1] - times[i])
            return vals[i] * (1 - f) + vals[i + 1] * f
    return vals[-1]


def bone_pose(anim, bone_name, t):
    rot = np.zeros(3)
    pos = np.zeros(3)
    scale = np.ones(3)
    if anim is not None:
        bones = anim.get("bones", {})
        if bone_name in bones:
            b = bones[bone_name]
            if "rotation" in b:
                rot = sample_track(b["rotation"], t)
            if "position" in b:
                pos = sample_track(b["position"], t)
            if "scale" in b:
                s = sample_track(b["scale"], t)
                scale = s if s.shape == (3,) else np.repeat(s, 3)
    return rot, pos, scale


def load_model(wid):
    with open(os.path.join(ASSETS, f"geckolib/models/item/{wid}.geo.json")) as f:
        geo = json.load(f)["minecraft:geometry"][0]
    tex = Image.open(os.path.join(ASSETS, f"textures/item/{wid}_geo.png")).convert("RGBA")
    return geo, tex


def load_anim(wid, name):
    if name is None:
        return None, 0.0
    path = os.path.join(ASSETS, f"geckolib/animations/item/{wid}.animation.json")
    if not os.path.exists(path):
        return None, 0.0
    with open(path) as f:
        data = json.load(f)
    a = data["animations"].get(name)
    if a is None:
        return None, 0.0
    return a, float(a.get("animation_length", 0.0))


# Face definitions: (uv key, corner indices into the 8 cube corners, outward normal)
# Corners: 0:(0,0,0) 1:(1,0,0) 2:(1,1,0) 3:(0,1,0) 4:(0,0,1) 5:(1,0,1) 6:(1,1,1) 7:(0,1,1)
# Bedrock geo: x right, y up, z toward viewer(south). North face is z=0 side.
FACES = [
    ("north", [0, 1, 2, 3], (0, 0, -1), 0.80),
    ("south", [5, 4, 7, 6], (0, 0, 1), 0.92),
    ("west", [4, 0, 3, 7], (-1, 0, 0), 0.68),
    ("east", [1, 5, 6, 2], (1, 0, 0), 0.68),
    ("up", [3, 2, 6, 7], (0, 1, 0), 1.0),
    ("down", [4, 5, 1, 0], (0, -1, 0), 0.5),
]


def collect_faces(geo, anim, t):
    """Returns list of (world_corners[4,3], uv_rect, shade) ready to project."""
    bones = {b["name"]: b for b in geo["bones"]}
    order = []
    seen = set()

    def visit(name):
        if name in seen:
            return
        seen.add(name)
        order.append(name)
        for b in geo["bones"]:
            if b.get("parent") == name:
                visit(b["name"])

    for b in geo["bones"]:
        if "parent" not in b:
            visit(b["name"])

    world = {}  # bone name -> (R, origin) mapping local point to world
    out = []
    for name in order:
        bone = bones[name]
        pivot = np.array(bone.get("pivot", [0, 0, 0]), dtype=float)
        rot, pos, scale = bone_pose(anim, name, t)
        R = rot_matrix(*rot)
        if "parent" in bone and bone["parent"] in world:
            pR, pO, _pxf = world[bone["parent"]]
            # child pivot expressed in parent space
            wpivot = pO + pR @ pivot
            wR = pR @ R
            # rotation/scale act about the bone pivot
            def xf(pt, pR=pR, pO=pO, wpivot=wpivot, wR=wR, pos=pos, scale=scale):
                base = pO + pR @ pt
                return wpivot + wR @ ((base - wpivot) * scale) + pos
            world[name] = (wR, wpivot)  # approximate origin for children pivots
            # store a full transform closure instead: keep via dict below
            world[name] = (wR, wpivot, xf)
        else:
            def xf(pt, R=R, pivot=pivot, pos=pos, scale=scale):
                return pivot + R @ ((pt - pivot) * scale) + pos
            world[name] = (R, pivot, xf)

        for cube in bone.get("cubes", []):
            o = np.array(cube["origin"], dtype=float)
            s = np.array(cube["size"], dtype=float)
            corners = np.array([
                o + np.array([dx * s[0], dy * s[1], dz * s[2]])
                for dx, dy, dz in [(0, 0, 0), (1, 0, 0), (1, 1, 0), (0, 1, 0),
                                   (0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1)]
            ])
            if "rotation" in cube:
                cp = np.array(cube.get("pivot", [0, 0, 0]), dtype=float)
                cR = rot_matrix(*cube["rotation"])
                corners = cp + (corners - cp) @ cR.T
            corners = np.array([xf(pt) for pt in corners])
            for key, idx, normal, shade in FACES:
                uv = cube["uv"][key]
                out.append((corners[idx], uv, shade, np.array(normal, dtype=float)))
    return out


def render(faces, tex, cam, img_size=760):
    az, el, dist_scale, target_off = cam
    W = H = img_size
    img = Image.new("RGBA", (W, H), (40, 43, 54, 255))
    draw = ImageDraw.Draw(img)

    all_pts = np.concatenate([f[0] for f in faces])
    center = (all_pts.min(axis=0) + all_pts.max(axis=0)) / 2 + np.array(target_off)
    extent = (all_pts.max(axis=0) - all_pts.min(axis=0)).max()

    azr, elr = math.radians(az), math.radians(el)
    eye = center + dist_scale * extent * np.array([
        math.cos(elr) * math.sin(azr), math.sin(elr), math.cos(elr) * math.cos(azr)])
    fwd = center - eye
    fwd = fwd / np.linalg.norm(fwd)
    right = np.cross(fwd, np.array([0, 1, 0.0]))
    right = right / np.linalg.norm(right)
    up = np.cross(right, fwd)
    focal = W * 1.15

    texarr = np.array(tex).astype(np.float64)
    th_, tw_ = texarr.shape[:2]

    def project(pt):
        v = pt - eye
        z = v @ fwd
        if z < 1e-6:
            return None
        x = (v @ right) / z * focal + W / 2
        y = -(v @ up) / z * focal + H / 2
        return np.array([x, y]), z

    canvas = np.zeros((H, W, 3), dtype=np.float64)
    canvas[:] = (40, 43, 54)
    zbuf = np.full((H, W), -1e18)

    for corners, uv, shade, normal in faces:
        u0, v0 = uv["uv"]
        uw, vh = uv["uv_size"]
        # quad corners in FACES order are (bottom-left, bottom-right, top-right, top-left)
        # in face space; texture v grows downward, so top maps to v0.
        uvq = np.array([
            [u0, v0 + vh], [u0 + uw, v0 + vh], [u0 + uw, v0], [u0, v0]
        ], dtype=np.float64)
        proj = [project(pt) for pt in corners]
        if any(p is None for p in proj):
            continue
        pts = np.array([p[0] for p in proj])
        zs = np.array([p[1] for p in proj])
        for tri in ((0, 1, 2), (0, 2, 3)):
            p = pts[list(tri)]
            z = zs[list(tri)]
            uvv = uvq[list(tri)]
            # backface culling: geo faces wind CCW from outside
            area = (p[1][0] - p[0][0]) * (p[2][1] - p[0][1]) - (p[1][1] - p[0][1]) * (p[2][0] - p[0][0])
            if area >= 0:
                continue
            minx = max(0, int(np.floor(p[:, 0].min())))
            maxx = min(W - 1, int(np.ceil(p[:, 0].max())))
            miny = max(0, int(np.floor(p[:, 1].min())))
            maxy = min(H - 1, int(np.ceil(p[:, 1].max())))
            if maxx < minx or maxy < miny:
                continue
            ys, xs = np.mgrid[miny:maxy + 1, minx:maxx + 1]
            xs = xs.astype(np.float64) + 0.5
            ys = ys.astype(np.float64) + 0.5
            d = (p[1][1] - p[2][1]) * (p[0][0] - p[2][0]) + (p[2][0] - p[1][0]) * (p[0][1] - p[2][1])
            if abs(d) < 1e-12:
                continue
            w0 = ((p[1][1] - p[2][1]) * (xs - p[2][0]) + (p[2][0] - p[1][0]) * (ys - p[2][1])) / d
            w1 = ((p[2][1] - p[0][1]) * (xs - p[2][0]) + (p[0][0] - p[2][0]) * (ys - p[2][1])) / d
            w2 = 1 - w0 - w1
            inside = (w0 >= -1e-6) & (w1 >= -1e-6) & (w2 >= -1e-6)
            if not inside.any():
                continue
            invz = w0 / z[0] + w1 / z[1] + w2 / z[2]
            pu = (w0 * uvv[0][0] / z[0] + w1 * uvv[1][0] / z[1] + w2 * uvv[2][0] / z[2]) / invz
            pv = (w0 * uvv[0][1] / z[0] + w1 * uvv[1][1] / z[1] + w2 * uvv[2][1] / z[2]) / invz
            tx = np.clip(pu.astype(int), 0, tw_ - 1)
            ty = np.clip(pv.astype(int), 0, th_ - 1)
            region = (slice(miny, maxy + 1), slice(minx, maxx + 1))
            better = inside & (invz > zbuf[region])
            if not better.any():
                continue
            sampled = texarr[ty, tx][:, :, :3] * shade
            canvas[region][better] = sampled[better]
            zbuf[region][better] = invz[better]

    out = Image.fromarray(np.clip(canvas, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    return out


VIEWS = {
    # azimuth, elevation, distance scale, target offset
    "three_quarter": (38, 16, 1.55, (0, 0, 0)),
    "side": (90, 6, 1.5, (0, 0, 0)),
    "edge": (0, 10, 1.5, (0, 0, 0)),
    "held": (-32, 24, 1.05, (0, -2, 0)),
    "tip": (34, 12, 0.62, (0, 11.5, 0)),
}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("weapon")
    ap.add_argument("--anim", default=None)
    ap.add_argument("--t", type=float, default=0.0)
    ap.add_argument("--views", default="three_quarter,side,edge,held")
    ap.add_argument("--out", default=os.path.join(ROOT, "renders/3d"))
    args = ap.parse_args()

    geo, tex = load_model(args.weapon)
    anim, length = load_anim(args.weapon, args.anim)
    t = args.t * length if args.anim and args.t <= 1.0 and length > 0 else args.t
    faces = collect_faces(geo, anim, t)
    os.makedirs(args.out, exist_ok=True)
    tag = f"_{args.anim}" if args.anim else ""
    for view in args.views.split(","):
        img = render(faces, tex, VIEWS[view])
        path = os.path.join(args.out, f"{args.weapon}_3d{tag}_{view}.png")
        img.save(path)
        print("wrote", path)


if __name__ == "__main__":
    main()
