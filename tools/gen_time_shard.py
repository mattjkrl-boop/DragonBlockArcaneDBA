"""Generator for the time_shard GeckoLib entity model.

For Whis Staff's Time Shatter: a cluster of frozen temporal glass
fragments that hang in the air at the hit point, then drop and shatter.
Palette: pale ice glass, hex EAF6FF and CDE9FF, with white cores.
One cluster about 20 units wide, six shards pointing outward.

Model pass upgrade: every shard is a tapered crystal built from
three stacked shrinking segments (base, mid, tip) with the mid
segment and the tip wedge carrying their own cube rotations, so
the facets exist in geometry instead of only in paint. Four tiny
satellite splinters float beside their parent shards. The texture
paints a different value step on every face, so a turning shard
reads as cut crystal, and the hang clip trembles each shard on
its own phase.

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
TEX = 128
UV_SCALE = 3

# ---------------------------------------------------------------- palette
ICE_DEEP = (127, 184, 232)     # deeper blue edge
ICE_MID = (205, 233, 255)      # hex CDE9FF
ICE_PALE = (234, 246, 255)     # hex EAF6FF
ICE_WHITE = (255, 255, 255)
GLOW_CORE = (214, 248, 255)    # soft white-cyan
GLOW_DIM = (46, 78, 98)
GLOW_EDGE = (8, 16, 24)

# Per-face value steps: every face of a cube shades differently.
FACE_STEP = {"north": 1.0, "east": 0.9, "south": 0.8,
             "west": 0.7, "up": 1.06, "down": 0.6}
MAT_TINT = {"ice": 0.0, "mid": 0.18, "tip": 0.38, "splinter": 0.26}

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
# Each shard is built along local +Y starting at y_start, in three
# stacked shrinking segments, then oriented by its bone rest rotation.
# Every shard bone pivot is the cluster center [0, 0, 0] so the hang,
# fall and shatter clips can scatter the shards from one shared origin.
# sat: optional satellite splinter (side offset, y fraction, rotation).
SHARDS = [
    dict(name="shard_0", rot=[0, 0, -8], center=[0, 5, 0], scatter=5.0,
         length=10.5, wid=3.2, dep=2.0, y_start=1.0,
         mid_rot=5, tip_rot=[10, 6, 16],
         sat=dict(side=1, yfrac=0.35, rot=[18, -12, 24])),
    dict(name="shard_1", rot=[15, 20, -50], center=[6, 2, 2], scatter=6.0,
         length=9.5, wid=2.8, dep=1.8, y_start=1.6,
         mid_rot=-6, tip_rot=[-12, 10, -18],
         sat=dict(side=-1, yfrac=0.45, rot=[-20, 14, -26])),
    dict(name="shard_2", rot=[-20, -15, -110], center=[5, -4, -2], scatter=4.5,
         length=8.5, wid=3.0, dep=1.6, y_start=0.8,
         mid_rot=7, tip_rot=[14, -8, 20], sat=None),
    dict(name="shard_3", rot=[10, -25, 55], center=[-6, 3, -1], scatter=6.5,
         length=10.0, wid=2.6, dep=2.0, y_start=1.4,
         mid_rot=-5, tip_rot=[-10, -12, -15],
         sat=dict(side=1, yfrac=0.3, rot=[22, 10, 30])),
    dict(name="shard_4", rot=[-15, 10, 125], center=[-5, -4, 2], scatter=5.5,
         length=9.0, wid=2.8, dep=1.7, y_start=1.2,
         mid_rot=6, tip_rot=[16, 8, -14],
         sat=dict(side=-1, yfrac=0.5, rot=[-16, -18, 20])),
    dict(name="shard_5", rot=[25, 30, 170], center=[1, -6, -3], scatter=7.0,
         length=10.5, wid=3.0, dep=2.2, y_start=0.6,
         mid_rot=-7, tip_rot=[-14, 12, 18], sat=None),
]

FACES = ("north", "south", "east", "west", "up", "down")


def build_cubes():
    """Expand each shard spec into its segment cubes."""
    cubes = []
    for si, sh in enumerate(SHARDS):
        L, W, D, y0 = sh["length"], sh["wid"], sh["dep"], sh["y_start"]
        phase = hash01(si * 17 + 3, si * 29 + 5, 4)
        base_h = L * 0.42
        mid_h = L * 0.34
        tip_h = L * 0.28
        mid_w, mid_d = W * 0.7, D * 0.72
        tip_w, tip_d = W * 0.4, D * 0.48
        mid_y = y0 + base_h
        tip_y = mid_y + mid_h
        cubes.append(dict(
            id="%s_0" % sh["name"], bone=sh["name"], mat="ice", phase=phase,
            origin=[r2(-W / 2), r2(y0), r2(-D / 2)], size=[r2(W), r2(base_h), r2(D)]))
        cubes.append(dict(
            id="%s_1" % sh["name"], bone=sh["name"], mat="mid", phase=phase,
            origin=[r2(-mid_w / 2), r2(mid_y), r2(-mid_d / 2)],
            size=[r2(mid_w), r2(mid_h), r2(mid_d)],
            pivot=[0, r2(mid_y + mid_h / 2), 0], rotation=[0, 0, sh["mid_rot"]]))
        cubes.append(dict(
            id="%s_2" % sh["name"], bone=sh["name"], mat="tip", phase=phase,
            origin=[r2(-tip_w / 2), r2(tip_y), r2(-tip_d / 2)],
            size=[r2(tip_w), r2(tip_h), r2(tip_d)],
            pivot=[0, r2(tip_y), 0], rotation=sh["tip_rot"]))
        if sh["sat"] is not None:
            sat = sh["sat"]
            sx = sat["side"] * (W / 2 + 1.0)
            sy = y0 + L * sat["yfrac"]
            cubes.append(dict(
                id="%s_3" % sh["name"], bone=sh["name"], mat="splinter", phase=phase,
                origin=[r2(sx - 0.42), r2(sy), -0.42], size=[0.85, 2.4, 0.85],
                pivot=[r2(sx), r2(sy + 1.2), 0], rotation=sat["rot"]))
    return cubes


ALL_CUBES = build_cubes()


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
            rects.append((c["id"], f, int(math.ceil(w * UV_SCALE)),
                          int(math.ceil(h * UV_SCALE))))
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

def facet_t(c, nx, ny):
    """Position across the facet highlight stripe, 1 at the stripe core."""
    ctr = 0.45 + 0.3 * c.get("phase", 0.0)
    d = abs((nx * 0.7 + ny * 0.7) - ctr)
    return max(0.0, 1.0 - d * 4.0)


def paint_base(c, face, nx, ny, gu, gv):
    n = hash01(gu, gv, 11)
    edge = min(nx, 1 - nx, ny, 1 - ny)
    col = lerp_c(ICE_MID, ICE_PALE, 0.35 + 0.45 * ny + (n - 0.5) * 0.14)
    col = lerp_c(col, ICE_WHITE, MAT_TINT[c["mat"]])
    step = FACE_STEP[face]
    if step < 1.0:
        col = lerp_c(col, ICE_DEEP, (1.0 - step) * 0.95)
    elif step > 1.0:
        col = lerp_c(col, ICE_WHITE, (step - 1.0) * 2.5)
    if face in ("north", "east", "up"):
        col = lerp_c(col, ICE_WHITE, facet_t(c, nx, ny) * 0.8)
    frac = abs((nx - ny) - (c.get("phase", 0.0) - 0.5) * 0.6)
    if frac < 0.035 and face in ("north", "south", "east", "west"):
        col = lerp_c(col, ICE_DEEP, 0.3)
    if edge < 0.14:
        col = lerp_c(col, ICE_DEEP, 0.65)
    return col


def paint_glow(c, face, nx, ny, gu, gv):
    edge = min(nx, 1 - nx, ny, 1 - ny)
    if edge < 0.14:
        return GLOW_EDGE
    # cores only: a narrow bright line down the middle of the side faces
    if face in ("north", "south", "east", "west"):
        core = max(0.0, 1.0 - abs(nx - 0.5) * 6.0)
        col = lerp_c(GLOW_DIM, GLOW_CORE, core)
        col = lerp_c(col, GLOW_CORE, facet_t(c, nx, ny) * 0.55)
        return col
    # end faces: a small core dot at the center, dark elsewhere
    d = math.sqrt((nx - 0.5) ** 2 + (ny - 0.5) ** 2)
    return lerp_c(GLOW_CORE, GLOW_EDGE, max(0.0, min(1.0, d * 2.6)))


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
            cube = {"origin": c["origin"], "size": c["size"]}
            if "pivot" in c:
                cube["pivot"] = c["pivot"]
                cube["rotation"] = c["rotation"]
            cube["uv"] = {f: {"uv": [c["uv"][f][0], c["uv"][f][1]],
                              "uv_size": [c["uv"][f][2], c["uv"][f][3]]}
                          for f in FACES}
            cubes.append(cube)
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
    """A small per shard drift, for hang and fall clips.

    GeckoLib adds clip rotation on top of the bone's rest rotation
    from the geo, so clip values are offsets from rest, never the
    rest rotation itself (that would turn each shard twice).
    """
    sgn = 1 if i % 2 == 0 else -1
    return [r2(sgn * amt), r2(-sgn * amt * 0.6), r2(sgn * amt * 0.8)]


def tremor(sh, dist):
    """Small position tremble along the shard scatter direction."""
    return scatter_vec(sh, dist)


def build_anim():
    hang_bones = {
        "root": {"position": {
            "0.0": [0, 0, 0], "0.3": [0, 0.18, 0], "0.6": [0, 0.3, 0],
            "0.9": [0, 0.12, 0], "1.2": [0, 0, 0]}}
    }
    fall_bones = {}
    shatter_bones = {}
    for i, sh in enumerate(SHARDS):
        base = [0, 0, 0]  # offsets from the rest rotation
        # hang: per shard tremble with phase offsets in time and amount
        ph = (i % 3) * 0.04
        amt = 0.8 + 0.18 * i
        t1 = r2(0.3 + ph)
        t2 = r2(0.9 - ph)
        hang_bones[sh["name"]] = {
            "rotation": {
                "0.0": base,
                ("%.2f" % t1): drift(sh, i, amt),
                "0.6": drift(sh, i, -amt * 0.4),
                ("%.2f" % t2): drift(sh, i, amt * 0.7),
                "1.2": base},
            "position": {
                "0.0": [0, 0, 0],
                ("%.2f" % r2(0.45 + ph)): tremor(sh, 0.16 if i % 2 == 0 else -0.13),
                "1.2": [0, 0, 0]},
        }
        spread = scatter_vec(sh, 0.7)
        spread_mid = scatter_vec(sh, 0.42)
        fall_bones[sh["name"]] = {
            "position": {"0.0": [0, 0, 0], "0.17": spread_mid,
                         "0.25": spread, "0.38": spread_mid, "0.5": [0, 0, 0]},
            "rotation": {"0.0": base, "0.17": drift(sh, i, 0.45),
                         "0.25": drift(sh, i, 0.7), "0.5": base},
        }
        out = scatter_vec(sh, sh["scatter"])
        out[1] = r2(out[1] - 3.0)
        half = [r2(v * 0.45) for v in out]
        snap = [r2(14 + i * 5), r2(-18 + i * 7), r2(22 - i * 6)]
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
