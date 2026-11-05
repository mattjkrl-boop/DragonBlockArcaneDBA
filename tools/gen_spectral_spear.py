"""Generator for the spectral_spear GeckoLib entity model.

The thrown weapon of Evil Impale: a huge flaming spectral spear.
Built horizontal along Z: tip at the +Z front, butt at -Z.
Total length 84 units (5.25 blocks): butt cap z -38 to spear tip z 46.

Emits, from the SAME cube and UV constants below:
  - geckolib/models/entity/spectral_spear.geo.json
  - geckolib/animations/entity/spectral_spear.animation.json
  - textures/entity/spectral_spear.png
  - textures/entity/spectral_spear_glowmask.png

Run from anywhere: python3 tools/gen_spectral_spear.py
No em dashes anywhere in this file.
"""
import json
import pathlib

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources/assets/dragonblockarcanedba"
NAME = "spectral_spear"
TEX = 128

# ---------------------------------------------------------------- palette
IRON_DARK = (38, 38, 44)
IRON_MID = (62, 62, 70)
IRON_LIGHT = (104, 108, 118)
WRAP = (92, 62, 34)
WRAP_DARK = (64, 42, 22)
MOLTEN_DARK = (58, 18, 4)
MOLTEN_MID = (214, 74, 6)
MOLTEN_HOT = (255, 138, 20)
MOLTEN_CORE = (255, 196, 64)
FLAME_DEEP = (232, 84, 4)
FLAME_CORE = (255, 178, 44)

# ---------------------------------------------------------------- helpers

def clamp(v):
    return max(0, min(255, int(round(v))))


def lerp_c(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def molten(t):
    """Dark base to bright orange-gold ramp, t in 0..1."""
    if t < 0.5:
        return lerp_c(MOLTEN_DARK, MOLTEN_MID, t * 2.0)
    return lerp_c(MOLTEN_MID, MOLTEN_CORE, (t - 0.5) * 2.0)


def hash01(x, y, s=0):
    n = (x * 73856093) ^ (y * 19349663) ^ (s * 83492791)
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return (n & 0xFFFF) / 65535.0


# ---------------------------------------------------------------- cube data
# Each cube: bone, origin (min corner), size, material, optional pivot+rotation.
# Materials: shaft, pommel, head (uses zheat 0..1 base heat), tip, barb, flame.
CUBES = [
    # shaft bone: long thin shaft z -34 to 14, plus a butt cap z -38 to -34
    dict(id="shaft_main", bone="shaft", origin=[-1.5, -1.5, -34], size=[3, 3, 48],
         mat="shaft"),
    dict(id="shaft_butt", bone="shaft", origin=[-2, -2, -38], size=[4, 4, 4],
         mat="pommel"),
    # head bone: leaf blade z 14 to 40, three stacked segments widening then tapering
    dict(id="head_base", bone="head", origin=[-2, -1, 14], size=[4, 2, 8],
         mat="head", zheat=0.15),
    dict(id="head_mid", bone="head", origin=[-4, -1, 22], size=[8, 2, 10],
         mat="head", zheat=0.55),
    dict(id="head_taper", bone="head", origin=[-2.5, -1, 32], size=[5, 2, 8],
         mat="head", zheat=0.85),
    # barbs swept back at the head base, angled outward about Y
    dict(id="barb_l", bone="barb_l", origin=[2, -1, 9], size=[2, 2, 8],
         mat="head", zheat=0.1, pivot=[3, 0, 15], rotation=[0, 28, 0]),
    dict(id="barb_r", bone="barb_r", origin=[-4, -1, 9], size=[2, 2, 8],
         mat="head", zheat=0.1, pivot=[-3, 0, 15], rotation=[0, -28, 0]),
    # flame tongues hugging the blade edges (these carry the strongest glow)
    dict(id="flame_l", bone="flame", origin=[3.6, -1.5, 17], size=[1, 3, 21],
         mat="flame"),
    dict(id="flame_r", bone="flame", origin=[-4.6, -1.5, 17], size=[1, 3, 21],
         mat="flame"),
    # head_tip bone: final point z 40 to 46
    dict(id="head_tip", bone="head_tip", origin=[-1, -0.5, 40], size=[2, 1, 6],
         mat="tip", zheat=1.0),
]

BONES = [
    dict(name="root", pivot=[0, 0, 0]),
    dict(name="shaft", parent="root", pivot=[0, 0, 0]),
    dict(name="head", parent="root", pivot=[0, 0, 14]),
    dict(name="head_tip", parent="head", pivot=[0, 0, 40]),
    dict(name="barb_l", parent="head", pivot=[0, 0, 14]),
    dict(name="barb_r", parent="head", pivot=[0, 0, 14]),
    dict(name="flame", parent="head", pivot=[0, 0, 14]),
]

FACES = ("north", "south", "east", "west", "up", "down")


def face_dims(size):
    dx, dy, dz = size
    return {
        "north": (dx, dy), "south": (dx, dy),
        "east": (dz, dy), "west": (dz, dy),
        "up": (dx, dz), "down": (dx, dz),
    }


def pack_uvs():
    """Shelf packer: one rect per cube face, sorted tallest first."""
    rects = []
    for c in CUBES:
        dims = face_dims(c["size"])
        for f in FACES:
            w, h = dims[f]
            rects.append((c["id"], f, int(round(w)), int(round(h))))
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
    for c in CUBES:
        c["uv"] = {f: placed[(c["id"], f)] for f in FACES}
    return placed


# ---------------------------------------------------------------- painting

def along(face, nx, ny):
    """Fraction along the spear length direction within one face rect."""
    if face in ("east", "west"):
        return nx
    if face in ("up", "down"):
        return ny
    return 0.5


def paint_base(c, face, nx, ny, gu, gv):
    mat = c["mat"]
    n = hash01(gu, gv, 7)
    if mat == "shaft":
        base = lerp_c(IRON_DARK, IRON_MID, n * 0.7)
        a = along(face, nx, ny)
        length_units = a * 48.0
        if face in ("east", "west", "up", "down") and (length_units % 12.0) < 2.5:
            base = lerp_c(WRAP, WRAP_DARK, n)
        if face in ("east", "west") and ny < 0.18:
            base = lerp_c(base, IRON_LIGHT, 0.45)
        return base
    if mat == "pommel":
        base = lerp_c(IRON_DARK, IRON_MID, n * 0.5)
        if nx < 0.2 or nx > 0.8:
            base = lerp_c(base, MOLTEN_MID, 0.35)
        return base
    if mat in ("head", "tip", "barb"):
        heat = 0.6 * c.get("zheat", 0.4) + 0.4 * along(face, nx, ny)
        col = molten(max(0.0, min(1.0, heat + (n - 0.5) * 0.12)))
        edge = min(nx, 1 - nx, ny, 1 - ny)
        if edge < 0.16:
            col = lerp_c(col, MOLTEN_CORE, 0.55)
        return col
    if mat == "flame":
        t = 0.45 + 0.4 * ny + 0.3 * hash01(gu, gv, 3)
        col = lerp_c(FLAME_DEEP, FLAME_CORE, max(0.0, min(1.0, t)))
        if 0.35 < nx < 0.65:
            col = lerp_c(col, MOLTEN_CORE, 0.5)
        return col
    return (255, 0, 255)


def paint_glow(c, face, nx, ny, gu, gv):
    mat = c["mat"]
    if mat in ("shaft", "pommel"):
        return (0, 0, 0)
    if mat == "flame":
        t = 0.6 + 0.4 * hash01(gu, gv, 5)
        return lerp_c((255, 110, 0), (255, 190, 60), t)
    # head family: edges glow hot, faces glow dimmer toward the base
    edge = min(nx, 1 - nx, ny, 1 - ny)
    heat = c.get("zheat", 0.4)
    if edge < 0.22:
        return lerp_c((160, 52, 0), (255, 150, 30), heat)
    return lerp_c((24, 6, 0), (96, 30, 0), heat)


def paint_textures():
    base = Image.new("RGB", (TEX, TEX), (0, 0, 0))
    glow = Image.new("RGB", (TEX, TEX), (0, 0, 0))
    bp, gp = base.load(), glow.load()
    for c in CUBES:
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
    bones = []
    by_bone = {}
    for c in CUBES:
        by_bone.setdefault(c["bone"], []).append(c)
    for b in BONES:
        bone = {"name": b["name"], "pivot": b["pivot"]}
        if "parent" in b:
            bone["parent"] = b["parent"]
        cubes = []
        for c in by_bone.get(b["name"], []):
            cube = {"origin": c["origin"], "size": c["size"]}
            if "pivot" in c:
                cube["pivot"] = c["pivot"]
                cube["rotation"] = c["rotation"]
            cube["uv"] = {
                f: {"uv": [c["uv"][f][0], c["uv"][f][1]],
                    "uv_size": [c["uv"][f][2], c["uv"][f][3]]}
                for f in FACES
            }
            cubes.append(cube)
        if cubes:
            bone["cubes"] = cubes
        bones.append(bone)
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.dragonblockarcanedba." + NAME,
                "texture_width": TEX,
                "texture_height": TEX,
                "visible_bounds_width": 7,
                "visible_bounds_height": 7,
                "visible_bounds_offset": [0, 0, 0.5],
            },
            "bones": bones,
        }],
    }


def build_anim():
    return {
        "format_version": "1.10.0",
        "animations": {
            "fly": {
                "loop": True,
                "animation_length": 0.6,
                "bones": {
                    "root": {
                        "rotation": {
                            "0.0": [0, 0, -1],
                            "0.3": [0, 0, 1],
                            "0.6": [0, 0, -1],
                        }
                    },
                    "flame": {
                        "scale": {
                            "0.0": [1.0, 1.0, 1.0],
                            "0.3": [1.15, 1.15, 1.15],
                            "0.6": [1.0, 1.0, 1.0],
                        }
                    },
                },
            }
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
    print(NAME, "cubes", len(CUBES))
    print("wrote", geo_path)
    print("wrote", anim_path)
    print("wrote", tex_path)
    print("wrote", glow_path)


if __name__ == "__main__":
    main()
