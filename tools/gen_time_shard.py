"""Generator for the time_shard GeckoLib entity model.

For Whis Staff's Time Shatter: a cluster of frozen temporal glass
fragments that hang in the air at the hit point, then drop and shatter.
Palette: pale ice glass, hex EAF6FF and CDE9FF, with white cores.
One cluster about 20 units wide, six shards pointing outward.

Emits, from the SAME cube and UV constants below:
  - geckolib/models/entity/time_shard.geo.json
  - geckolib/animations/entity/time_shard.animation.json
  - textures/entity/time_shard.png
  - textures/entity/time_shard_glowmask.png

Run from anywhere: python3 tools/gen_time_shard.py
No em dashes anywhere in this file.
"""
import json
import math
import pathlib

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources/assets/dragonblockarcanedba"
NAME = "time_shard"
TEX = 64

# ---------------------------------------------------------------- palette
ICE_DEEP = (127, 184, 232)     # deeper blue edge
ICE_MID = (205, 233, 255)      # hex CDE9FF
ICE_PALE = (234, 246, 255)     # hex EAF6FF
ICE_WHITE = (255, 255, 255)
GLOW_CORE = (214, 248, 255)    # soft white-cyan
GLOW_DIM = (46, 78, 98)
GLOW_EDGE = (8, 16, 24)

# ---------------------------------------------------------------- helpers

def clamp(v):
    return max(0, min(255, int(round(v))))


def lerp_c(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def hash01(x, y, s=0):
    n = (x * 73856093) ^ (y * 19349663) ^ (s * 83492791)
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return (n & 0xFFFF) / 65535.0


def r2(v):
    return round(v, 2)


# ---------------------------------------------------------------- shard data
# Each shard: bone rotation (rest pose, degrees), a center offset used for
# scatter directions, and 1 to 2 boxes (main tapered box plus optional chip).
# Every shard bone pivot is the cluster center [0, 0, 0] so the hang, fall
# and shatter clips can scatter the shards from one shared origin.
SHARDS = [
    dict(name="shard_0", rot=[0, 0, -8], center=[0, 5, 0], scatter=5.0,
         boxes=[dict(origin=[-1.5, 1.5, -1], size=[3, 7, 2], mat="ice"),
                dict(origin=[-1, 8.5, -0.75], size=[2, 3, 1.5], mat="chip")]),
    dict(name="shard_1", rot=[15, 20, -50], center=[6, 2, 2], scatter=6.0,
         boxes=[dict(origin=[3.5, 0, 1], size=[2.5, 6, 2], mat="ice"),
                dict(origin=[4, 6, 1.25], size=[1.5, 2.5, 1.5], mat="chip")]),
    dict(name="shard_2", rot=[-20, -15, -110], center=[5, -4, -2], scatter=4.5,
         boxes=[dict(origin=[3, -6.5, -2.75], size=[3, 5, 1.5], mat="ice")]),
    dict(name="shard_3", rot=[10, -25, 55], center=[-6, 3, -1], scatter=6.5,
         boxes=[dict(origin=[-7, 0.5, -2], size=[2, 6, 2], mat="ice"),
                dict(origin=[-6.75, 6.5, -1.75], size=[1.5, 2.5, 1.5], mat="chip")]),
    dict(name="shard_4", rot=[-15, 10, 125], center=[-5, -4, 2], scatter=5.5,
         boxes=[dict(origin=[-6.25, -6, 1.25], size=[2.5, 5, 1.5], mat="ice")]),
    dict(name="shard_5", rot=[25, 30, 170], center=[1, -6, -3], scatter=7.0,
         boxes=[dict(origin=[0, -8.5, -4], size=[2, 5, 2], mat="ice"),
                dict(origin=[0.25, -11, -3.75], size=[1.5, 2.5, 1.5], mat="chip")]),
]

FACES = ("north", "south", "east", "west", "up", "down")
ALL_CUBES = []
for sh in SHARDS:
    for i, bx in enumerate(sh["boxes"]):
        ALL_CUBES.append(dict(id="%s_%d" % (sh["name"], i), bone=sh["name"], **bx))


def unit(v):
    ln = math.sqrt(sum(x * x for x in v)) or 1.0
    return [x / ln for x in v]


def scatter_vec(sh, dist):
    d = unit(sh["center"])
    return [r2(d[i] * dist) for i in range(3)]


def face_dims(size):
    dx, dy, dz = size
    return {
        "north": (dx, dy), "south": (dx, dy),
        "east": (dz, dy), "west": (dz, dy),
        "up": (dx, dz), "down": (dx, dz),
    }


def pack_uvs():
    rects = []
    for c in ALL_CUBES:
        dims = face_dims(c["size"])
        for f in FACES:
            w, h = dims[f]
            rects.append((c["id"], f, int(math.ceil(w)), int(math.ceil(h))))
    rects.sort(key=lambda r: -r[3])
    placed = {}
    x = y = shelf_h = 0
    for cid, f, w, h in rects:
        if x + w > TEX:
            x, y, shelf_h = 0, y + shelf_h, 0
        assert y + h <= TEX, "atlas overflow"
        placed[(cid, f)] = (x, y, w, h)
        x += w
        shelf_h = max(shelf_h, h)
    for c in ALL_CUBES:
        c["uv"] = {f: placed[(c["id"], f)] for f in FACES}


# ---------------------------------------------------------------- painting

def facet_t(nx, ny):
    """Position across the facet highlight stripe, 1 at the stripe core."""
    d = abs((nx * 0.7 + ny * 0.7) - 0.62)
    return max(0.0, 1.0 - d * 4.0)


def paint_base(c, face, nx, ny, gu, gv):
    n = hash01(gu, gv, 11)
    edge = min(nx, 1 - nx, ny, 1 - ny)
    col = lerp_c(ICE_MID, ICE_PALE, 0.35 + 0.45 * ny + (n - 0.5) * 0.14)
    if c["mat"] == "chip":
        col = lerp_c(col, ICE_WHITE, 0.35)
    col = lerp_c(col, ICE_WHITE, facet_t(nx, ny) * 0.85)
    if edge < 0.14:
        col = lerp_c(col, ICE_DEEP, 0.65)
    return col


def paint_glow(c, face, nx, ny, gu, gv):
    edge = min(nx, 1 - nx, ny, 1 - ny)
    if edge < 0.14:
        return GLOW_EDGE
    t = facet_t(nx, ny)
    col = lerp_c(GLOW_DIM, GLOW_CORE, t)
    if 0.4 < nx < 0.6 and 0.4 < ny < 0.6:
        col = lerp_c(col, GLOW_CORE, 0.6)
    return col


def paint_textures():
    base = Image.new("RGB", (TEX, TEX), (0, 0, 0))
    glow = Image.new("RGB", (TEX, TEX), (0, 0, 0))
    bp, gp = base.load(), glow.load()
    for c in ALL_CUBES:
        for f in FACES:
            u0, v0, w, h = c["uv"][f]
            for j in range(h):
                for i in range(w):
                    nx = (i + 0.5) / w
                    ny = (j + 0.5) / h
                    bp[u0 + i, v0 + j] = paint_base(c, f, nx, ny, u0 + i, v0 + j)
                    gp[u0 + i, v0 + j] = paint_glow(c, f, nx, ny, u0 + i, v0 + j)
    return base, glow


# ---------------------------------------------------------------- outputs

def build_geo():
    bones = [{"name": "root", "pivot": [0, 0, 0]}]
    for sh in SHARDS:
        bone = {"name": sh["name"], "parent": "root", "pivot": [0, 0, 0],
                "rotation": sh["rot"]}
        cubes = []
        for c in ALL_CUBES:
            if c["bone"] != sh["name"]:
                continue
            cubes.append({
                "origin": c["origin"], "size": c["size"],
                "uv": {f: {"uv": [c["uv"][f][0], c["uv"][f][1]],
                           "uv_size": [c["uv"][f][2], c["uv"][f][3]]}
                       for f in FACES},
            })
        bone["cubes"] = cubes
        bones.append(bone)
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.dragonblockarcanedba." + NAME,
                "texture_width": TEX,
                "texture_height": TEX,
                "visible_bounds_width": 3,
                "visible_bounds_height": 3,
                "visible_bounds_offset": [0, 0, 0],
            },
            "bones": bones,
        }],
    }


def drift(sh, i, amt):
    """Rest rotation plus a small per shard drift, for hang and fall clips."""
    sgn = 1 if i % 2 == 0 else -1
    return [r2(sh["rot"][0] + sgn * amt),
            r2(sh["rot"][1] - sgn * amt * 0.6),
            r2(sh["rot"][2] + sgn * amt * 0.8)]


def build_anim():
    hang_bones = {
        "root": {"position": {
            "0.0": [0, 0, 0], "0.3": [0, 0.18, 0], "0.6": [0, 0.3, 0],
            "0.9": [0, 0.12, 0], "1.2": [0, 0, 0]}}
    }
    fall_bones = {}
    shatter_bones = {}
    for i, sh in enumerate(SHARDS):
        base = [r2(v) for v in sh["rot"]]
        hang_bones[sh["name"]] = {"rotation": {
            "0.0": base, "0.6": drift(sh, i, 1.0), "1.2": base}}
        spread = scatter_vec(sh, 0.7)
        fall_bones[sh["name"]] = {
            "position": {"0.0": [0, 0, 0], "0.25": spread, "0.5": [0, 0, 0]},
            "rotation": {"0.0": base, "0.25": drift(sh, i, 0.7), "0.5": base},
        }
        out = scatter_vec(sh, sh["scatter"])
        out[1] = r2(out[1] - 3.0)
        half = [r2(v * 0.45) for v in out]
        snap = [r2(sh["rot"][0] + 14 + i * 5),
                r2(sh["rot"][1] - 18 + i * 7),
                r2(sh["rot"][2] + 22 - i * 6)]
        mid = [r2((base[k] + snap[k]) / 2.0) for k in range(3)]
        shatter_bones[sh["name"]] = {
            "position": {"0.0": [0, 0, 0], "0.2": half, "0.4": out},
            "rotation": {"0.0": base, "0.2": mid, "0.4": snap},
        }
    return {
        "format_version": "1.10.0",
        "animations": {
            "hang": {"loop": True, "animation_length": 1.2, "bones": hang_bones},
            "fall": {"loop": True, "animation_length": 0.5, "bones": fall_bones},
            "shatter": {"loop": "hold_on_last_frame", "animation_length": 0.4,
                        "bones": shatter_bones},
        },
    }


def main():
    pack_uvs()
    geo = build_geo()
    anim = build_anim()
    base, glow = paint_textures()
    geo_path = RES / "geckolib/models/entity" / (NAME + ".geo.json")
    anim_path = RES / "geckolib/animations/entity" / (NAME + ".animation.json")
    tex_path = RES / "textures/entity" / (NAME + ".png")
    glow_path = RES / "textures/entity" / (NAME + "_glowmask.png")
    for p in (geo_path, anim_path, tex_path, glow_path):
        p.parent.mkdir(parents=True, exist_ok=True)
    geo_path.write_text(json.dumps(geo, indent=2))
    anim_path.write_text(json.dumps(anim, indent=2))
    base.save(tex_path)
    glow.save(glow_path)
    print(NAME, "cubes", len(ALL_CUBES))
    print("wrote", geo_path)
    print("wrote", anim_path)
    print("wrote", tex_path)
    print("wrote", glow_path)


if __name__ == "__main__":
    main()
