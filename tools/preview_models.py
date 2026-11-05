#!/usr/bin/env python3
"""Deterministic preview renderer for any DBA geo model.

Renders geo.json + texture + animation files from any asset family
(entity attack models, item weapons, better_player_model characters,
geckolib entities) through ONE pipeline at comparable framing, for
quality comparison.

Transforms use true Bedrock semantics: bone pivots and cube origins
live in model space; a cube point is transformed by its own bone's
rotation about the bone pivot, then by each ancestor's rotation about
the ancestor's pivot, up to the root. (tools/render3d.py approximates
parent transforms and visibly explodes chained hierarchies such as
the azure dragon's segment chain, so this tool does its own face
collection and only borrows render3d's rasterizer.)

Usage (from repo root):
  python3 tools/preview_models.py NAME GEO_JSON TEXTURE_PNG \
      [--anim ANIM_JSON] [--clip CLIP_NAME] [--t SECONDS] \
      [--views three_quarter,side,front] [--out DIR]

Writes NAME_<view>.png per view into renders/model-quality/ (or --out).
"""
import argparse
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import numpy as np  # noqa: E402
import render3d  # noqa: E402
from PIL import Image  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")

VIEWS = {
    "three_quarter": (38, 16, 1.55, (0, 0, 0)),
    "side": (90, 6, 1.5, (0, 0, 0)),
    "front": (0, 10, 1.5, (0, 0, 0)),
}

FACES = render3d.FACES


def box_uv(cube):
    """Bedrock box UV: uv is [u, v]; face rects derive from cube size."""
    u, v = cube["uv"]
    w, h, d = cube["size"]
    return {
        "up": {"uv": [u + d, v], "uv_size": [w, d]},
        "down": {"uv": [u + d + w, v], "uv_size": [w, d]},
        "west": {"uv": [u, v + d], "uv_size": [d, h]},
        "north": {"uv": [u + d, v + d], "uv_size": [w, h]},
        "east": {"uv": [u + d + w, v + d], "uv_size": [d, h]},
        "south": {"uv": [u + d + w + d, v + d], "uv_size": [w, h]},
    }


def bone_pose(anim, bone_name, t):
    rot = np.zeros(3)
    pos = np.zeros(3)
    scale = np.ones(3)
    if anim is not None:
        b = anim.get("bones", {}).get(bone_name)
        if b:
            if "rotation" in b:
                rot = render3d.sample_track(b["rotation"], t)
            if "position" in b:
                pos = render3d.sample_track(b["position"], t)
            if "scale" in b:
                s = render3d.sample_track(b["scale"], t)
                scale = s if np.shape(s) == (3,) else np.repeat(s, 3)
    return rot, pos, scale


def collect_faces(geo, anim, t):
    bones = {b["name"]: b for b in geo["bones"]}
    out = []

    def chain(name):
        """Bone names from this bone up to the root."""
        seq = []
        seen = set()
        while name in bones and name not in seen:
            seen.add(name)
            seq.append(name)
            name = bones[name].get("parent")
        return seq

    for bone in geo["bones"]:
        seq = chain(bone["name"])
        levels = []
        for n in seq:
            rot, pos, scale = bone_pose(anim, n, t)
            levels.append((render3d.rot_matrix(*rot),
                           np.array(bones[n].get("pivot", [0, 0, 0]), dtype=float),
                           pos, scale))

        def xf(pt, levels=levels):
            for R, pivot, pos, scale in levels:
                pt = pivot + R @ ((pt - pivot) * scale) + pos
            return pt

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
                cR = render3d.rot_matrix(*cube["rotation"])
                corners = cp + (corners - cp) @ cR.T
            corners = np.array([xf(pt) for pt in corners])
            uv = cube["uv"]
            if isinstance(uv, list):
                uv = box_uv(cube)
            for key, idx, normal, shade in FACES:
                out.append((corners[idx], uv[key], shade,
                            np.array(normal, dtype=float)))
    return out


def load_geo(path):
    with open(path) as f:
        data = json.load(f)
    return data["minecraft:geometry"][0]


def load_clip(anim_path, clip_name):
    if anim_path is None:
        return None
    with open(anim_path) as f:
        data = json.load(f)
    anims = data.get("animations", {})
    if clip_name is not None:
        return anims.get(clip_name)
    for a in anims.values():
        return a
    return None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("name")
    ap.add_argument("geo")
    ap.add_argument("texture")
    ap.add_argument("--anim", default=None)
    ap.add_argument("--clip", default=None)
    ap.add_argument("--t", type=float, default=0.0)
    ap.add_argument("--views", default="three_quarter,side,front")
    ap.add_argument("--out", default=os.path.join(ROOT, "renders/model-quality"))
    args = ap.parse_args()

    geo = load_geo(args.geo)
    tex = Image.open(args.texture).convert("RGBA")
    anim = load_clip(args.anim, args.clip)
    faces = collect_faces(geo, anim, args.t)
    os.makedirs(args.out, exist_ok=True)
    for view in args.views.split(","):
        img = render3d.render(faces, tex, VIEWS[view])
        path = os.path.join(args.out, f"{args.name}_{view}.png")
        img.save(path)
        print("wrote", path)


if __name__ == "__main__":
    main()
