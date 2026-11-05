"""Generator for the spectral_spear GeckoLib entity model.

The thrown weapon of Evil Impale: a huge flaming spectral spear.
Built horizontal along Z: tip at the +Z front, butt at -Z.
Total length 84 units (5.25 blocks): butt spike tip z -38 to spear tip z 46.

Model pass upgrade: the head is a true leaf blade built from five
stacked tapering segments with a raised midrib on both faces and
rotated edge bevels, seated in a stacked socket (one ring rotated
45 degrees so it reads as a diamond from the front) with langets
running down the shaft. Wrap bands ring the shaft and a tapered
butt spike closes the rear. Five thin rotated flame tongues on
their own flame bone hug the blade so the fly clip can flicker
them independently.

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
TEX = 256
UV_SCALE = 2

# ---------------------------------------------------------------- palette
IRON_DARK = (38, 38, 44)
IRON_MID = (62, 62, 70)
IRON_LIGHT = (104, 108, 118)
WRAP = (92, 62, 34)
WRAP_DARK = (64, 42, 22)
WRAP_LIGHT = (128, 90, 48)
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


def crackle(gu, gv):
    """Blocky crackle cell value for the molten head, 0..1."""
    return hash01(gu // 2, gv // 2, 21)


# ---------------------------------------------------------------- cube data
# Each cube: bone, origin (min corner), size, material, optional pivot+rotation.
# Materials: shaft, wrap, pommel, socket, langet, head (uses zheat 0..1
# base heat), tip, barb, flame.
CUBES = [
    # ---- shaft bone: slim shaft z -32 to 10, wrap bands, butt spike
    dict(id="shaft_main", bone="shaft", origin=[-1.25, -1.25, -32], size=[2.5, 2.5, 42],
         mat="shaft"),
    dict(id="wrap_1", bone="shaft", origin=[-1.55, -1.55, -24], size=[3.1, 3.1, 1.8],
         mat="wrap"),
    dict(id="wrap_2", bone="shaft", origin=[-1.55, -1.55, -10], size=[3.1, 3.1, 1.8],
         mat="wrap"),
    dict(id="wrap_3", bone="shaft", origin=[-1.55, -1.55, 4], size=[3.1, 3.1, 1.8],
         mat="wrap"),
    dict(id="butt_collar", bone="shaft", origin=[-1.7, -1.7, -34.5], size=[3.4, 3.4, 2.5],
         mat="pommel"),
    dict(id="butt_spike", bone="shaft", origin=[-1.1, -1.1, -37], size=[2.2, 2.2, 2.5],
         mat="pommel"),
    dict(id="butt_tip", bone="shaft", origin=[-0.55, -0.55, -38], size=[1.1, 1.1, 1],
         mat="pommel"),
    # ---- head bone: socket, stacked shrinking squares z 10 to 21.5
    dict(id="socket_1", bone="head", origin=[-2.2, -2.2, 10], size=[4.4, 4.4, 4],
         mat="socket"),
    dict(id="socket_2", bone="head", origin=[-1.9, -1.9, 13.5], size=[3.8, 3.8, 3.5],
         mat="socket"),
    dict(id="socket_diamond", bone="head", origin=[-1.6, -1.6, 16.5], size=[3.2, 3.2, 3],
         mat="socket", pivot=[0, 0, 18], rotation=[0, 0, 45]),
    dict(id="socket_3", bone="head", origin=[-1.3, -1.3, 19], size=[2.6, 2.6, 2.5],
         mat="socket"),
    # langets: flat iron strips running from the socket down the shaft faces
    dict(id="langet_top", bone="head", origin=[-0.8, 1.25, 0], size=[1.6, 0.7, 11],
         mat="langet"),
    dict(id="langet_bottom", bone="head", origin=[-0.8, -1.95, 0], size=[1.6, 0.7, 11],
         mat="langet"),
    # leaf blade: five stacked tapering segments z 21 to 40.5
    dict(id="blade_1", bone="head", origin=[-1.7, -0.8, 21], size=[3.4, 1.6, 4],
         mat="head", zheat=0.2),
    dict(id="blade_2", bone="head", origin=[-2.8, -0.85, 24.5], size=[5.6, 1.7, 4.5],
         mat="head", zheat=0.45),
    dict(id="blade_3", bone="head", origin=[-3.6, -0.8, 28.5], size=[7.2, 1.6, 5],
         mat="head", zheat=0.65),
    dict(id="blade_4", bone="head", origin=[-2.6, -0.7, 33], size=[5.2, 1.4, 4.5],
         mat="head", zheat=0.82),
    dict(id="blade_5", bone="head", origin=[-1.6, -0.6, 37], size=[3.2, 1.2, 3.5],
         mat="head", zheat=0.92),
    # raised midrib: two segments on the top face, one on the bottom face
    dict(id="midrib_top_1", bone="head", origin=[-0.65, 0.8, 22], size=[1.3, 0.65, 9],
         mat="head", zheat=0.5),
    dict(id="midrib_top_2", bone="head", origin=[-0.55, 0.7, 31], size=[1.1, 0.55, 8],
         mat="head", zheat=0.8),
    dict(id="midrib_bottom", bone="head", origin=[-0.65, -1.45, 24], size=[1.3, 0.65, 8],
         mat="head", zheat=0.55),
    # rotated edge bevels along both blade edges, wide and tip pairs
    dict(id="bevel_l", bone="head", origin=[-4.2, -0.55, 26], size=[2, 1.1, 10],
         mat="head", zheat=0.6, pivot=[-3.2, 0, 31], rotation=[0, 0, 38]),
    dict(id="bevel_r", bone="head", origin=[2.2, -0.55, 26], size=[2, 1.1, 10],
         mat="head", zheat=0.6, pivot=[3.2, 0, 31], rotation=[0, 0, -38]),
    dict(id="bevel_l2", bone="head", origin=[-2.8, -0.5, 34.5], size=[1.6, 1, 6],
         mat="head", zheat=0.85, pivot=[-2, 0, 37.5], rotation=[0, 0, 38]),
    dict(id="bevel_r2", bone="head", origin=[1.2, -0.5, 34.5], size=[1.6, 1, 6],
         mat="head", zheat=0.85, pivot=[2, 0, 37.5], rotation=[0, 0, -38]),
    # barbs swept back at the head base, angled outward about Y
    dict(id="barb_l", bone="barb_l", origin=[1.8, -0.7, 9], size=[1.7, 1.4, 7.5],
         mat="head", zheat=0.1, pivot=[2.6, 0, 14.5], rotation=[0, 30, 0]),
    dict(id="barb_r", bone="barb_r", origin=[-3.5, -0.7, 9], size=[1.7, 1.4, 7.5],
         mat="head", zheat=0.1, pivot=[-2.6, 0, 14.5], rotation=[0, -30, 0]),
    # flame tongues on their own bone: thin, rotated, hugging the blade
    dict(id="flame_l", bone="flame", origin=[3.35, -0.45, 22], size=[0.7, 0.9, 11],
         mat="flame", pivot=[3.7, 0, 27.5], rotation=[0, 0, -16]),
    dict(id="flame_r", bone="flame", origin=[-4.05, -0.45, 22], size=[0.7, 0.9, 11],
         mat="flame", pivot=[-3.7, 0, 27.5], rotation=[0, 0, 16]),
    dict(id="flame_top", bone="flame", origin=[-0.7, 1.35, 24], size=[1.4, 0.55, 8],
         mat="flame", pivot=[0, 1.6, 28], rotation=[6, 0, 4]),
    dict(id="flame_bottom", bone="flame", origin=[-0.7, -1.9, 24], size=[1.4, 0.55, 8],
         mat="flame", pivot=[0, -1.6, 28], rotation=[-6, 0, -4]),
    dict(id="flame_wisp", bone="flame", origin=[0.95, 0.45, 35], size=[0.6, 1.2, 5],
         mat="flame", pivot=[1.25, 1.0, 37.5], rotation=[8, 0, -18]),
    # head_tip bone: final point z 40 to 46
    dict(id="tip_main", bone="head_tip", origin=[-1.05, -0.5, 40], size=[2.1, 1, 3.8],
         mat="tip", zheat=0.97),
    dict(id="tip_point", bone="head_tip", origin=[-0.5, -0.28, 43.8], size=[1, 0.56, 2.2],
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
            rects.append((c["id"], f, int(round(w * UV_SCALE)), int(round(h * UV_SCALE))))
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
        base = lerp_c(base, IRON_DARK, a * 0.25)
        if face in ("east", "west") and ny < 0.18:
            base = lerp_c(base, IRON_LIGHT, 0.45)
        if face == "up":
            base = lerp_c(base, IRON_LIGHT, 0.18)
        return base
    if mat == "wrap":
        base = lerp_c(WRAP, WRAP_DARK, n * 0.6)
        stripe = (nx * 3.0 + ny * 2.0) % 1.0
        if stripe < 0.35:
            base = lerp_c(base, WRAP_DARK, 0.55)
        elif stripe > 0.85:
            base = lerp_c(base, WRAP_LIGHT, 0.4)
        return base
    if mat == "pommel":
        base = lerp_c(IRON_DARK, IRON_MID, n * 0.5)
        if nx < 0.2 or nx > 0.8:
            base = lerp_c(base, MOLTEN_MID, 0.3)
        return base
    if mat == "socket":
        base = lerp_c(IRON_DARK, IRON_MID, 0.3 + n * 0.4)
        if face in ("east", "west", "up", "down") and nx > 0.78:
            base = lerp_c(base, IRON_LIGHT, 0.5)
        if face in ("north", "south") and (nx < 0.15 or nx > 0.85):
            base = lerp_c(base, IRON_LIGHT, 0.35)
        return base
    if mat == "langet":
        base = lerp_c(IRON_MID, IRON_DARK, n * 0.5)
        if face in ("up", "down") and 0.38 < nx < 0.62:
            base = lerp_c(base, MOLTEN_MID, 0.45)
        return base
    if mat in ("head", "tip", "barb"):
        heat = 0.6 * c.get("zheat", 0.4) + 0.4 * along(face, nx, ny)
        col = molten(max(0.0, min(1.0, heat + (n - 0.5) * 0.12)))
        cr = crackle(gu, gv)
        if cr > 0.88:
            col = lerp_c(col, MOLTEN_DARK, 0.45)
        elif cr < 0.05:
            col = lerp_c(col, MOLTEN_CORE, 0.35)
        edge = min(nx, 1 - nx, ny, 1 - ny)
        if edge < 0.16:
            col = lerp_c(col, MOLTEN_CORE, 0.55)
        if face == "down":
            col = lerp_c(col, MOLTEN_DARK, 0.22)
        elif face == "east":
            col = lerp_c(col, MOLTEN_DARK, 0.1)
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
    if mat in ("shaft", "pommel", "socket", "langet", "wrap"):
        return (0, 0, 0)
    if mat == "flame":
        t = 0.6 + 0.4 * hash01(gu, gv, 5)
        return lerp_c((255, 110, 0), (255, 190, 60), t)
    # head family: glow discipline is molten edges and crack veins only
    edge = min(nx, 1 - nx, ny, 1 - ny)
    heat = c.get("zheat", 0.4)
    if edge < 0.2:
        return lerp_c((170, 56, 0), (255, 150, 30), heat)
    if crackle(gu, gv) > 0.92:
        return lerp_c((60, 16, 0), (130, 44, 0), heat)
    return (0, 0, 0)


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
                            "0.15": [0.6, 0, 0.2],
                            "0.3": [0, 0, 1],
                            "0.45": [-0.6, 0, 0.2],
                            "0.6": [0, 0, -1],
                        }
                    },
                    "head": {
                        "rotation": {
                            "0.0": [0, 0, 0],
                            "0.2": [0.8, -1.2, 0],
                            "0.4": [-0.8, 1.2, 0],
                            "0.6": [0, 0, 0],
                        }
                    },
                    "head_tip": {
                        "rotation": {
                            "0.0": [0, 0, 0],
                            "0.3": [1.5, 0, 0],
                            "0.6": [0, 0, 0],
                        }
                    },
                    "flame": {
                        "scale": {
                            "0.0": [1.0, 1.0, 1.0],
                            "0.12": [1.18, 1.08, 1.12],
                            "0.25": [0.94, 1.02, 0.97],
                            "0.4": [1.22, 1.12, 1.15],
                            "0.6": [1.0, 1.0, 1.0],
                        },
                        "rotation": {
                            "0.0": [0, 0, 0],
                            "0.18": [2, 0, -3],
                            "0.38": [-2, 0, 3],
                            "0.6": [0, 0, 0],
                        },
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
