"""Generate the Azure Dragon entity assets for DragonBlockArcaneDBA.

The Azure Dragon is the spectral storm dragon that wraps the player during
the Azure Dragon Sword Dragon Rush. It is a chained GeckoLib rig in the same
style as the in-mod Shenron precedent (tools/gen_dragon_geo.py): Bedrock
geometry, +Z is the model front, +Y up, 16 units per block. The dragon flies
horizontally: head at the +Z front, body trailing toward -Z, belly toward -Y.

This script is the single source of truth for the model. Cube layout and UV
regions are defined once as data below; the script emits:

  - geckolib/models/entity/azure_dragon.geo.json (format 1.12.0)
  - geckolib/animations/entity/azure_dragon.animation.json (format 1.10.0)
  - textures/entity/azure_dragon.png (256x256)
  - textures/entity/azure_dragon_glowmask.png (256x256, black where no glow)

The renderer applies the glowmask with GeckoLib AutoGlowingGeoLayer, the same
layer the lava weapons use. Palette: core #FFFFFF, mid #1E90FF, rim #7FDBFF.

Run from the repo root: python3 tools/gen_azure_dragon.py
No em dashes are used anywhere in this file by project rule.
"""

import json
import math
import pathlib

from PIL import Image, ImageDraw

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources/assets/dragonblockarcanedba"
GEO_PATH = RES / "geckolib/models/entity/azure_dragon.geo.json"
ANIM_PATH = RES / "geckolib/animations/entity/azure_dragon.animation.json"
TEX_PATH = RES / "textures/entity/azure_dragon.png"
GLOW_PATH = RES / "textures/entity/azure_dragon_glowmask.png"

TEX_SIZE = 256

# Palette (RGB)
CORE = (255, 255, 255)        # #FFFFFF
MID = (30, 144, 255)          # #1E90FF
RIM = (127, 219, 255)         # #7FDBFF
DEEP = (16, 78, 150)          # deep azure body base
DEEP_DARK = (9, 45, 92)       # scale outline / shadow
HEAD_BASE = (38, 122, 214)    # head slightly lighter than body
BELLY = (198, 233, 255)       # pale ice blue belly
BELLY_LINE = (148, 198, 238)
HORN_COL = (186, 228, 255)    # pale ice horns
HORN_BAND = (140, 195, 240)
FIN_COL = (146, 221, 255)     # pale cyan fin membrane
FIN_RIB = (62, 140, 205)
WHITE = (246, 251, 255)       # teeth, brow, claws
WHITE_SHADE = (196, 218, 238)
EYE_COL = (168, 240, 255)
GLOW = (127, 219, 255)        # glowmask paint (rim family)
GLOW_DIM = (70, 130, 160)     # dimmer glow for tips and stripes edges


def r3(v):
    """Round to 3 decimals, trimming negative zero."""
    out = round(float(v), 3)
    return 0.0 if out == 0 else out


def vec(vals):
    return [r3(v) for v in vals]


# ---------------------------------------------------------------------------
# Cube and bone data
#
# A cube is a dict: mat (paint material), origin, size, optional pivot and
# rotation (Bedrock per-cube rotation, degrees), optional glow tag used only
# by the glowmask painter, optional name for reporting.
# Materials: scale (body), headscale, horn, fin, white, whisker, eye.
# Glow tags: eye, fin, horn_tip, whisker_tip, dorsal (body top stripe).
# ---------------------------------------------------------------------------

def cube(mat, origin, size, pivot=None, rotation=None, glow=None, name=""):
    c = {"mat": mat, "origin": origin, "size": size, "glow": glow, "name": name}
    if pivot is not None:
        c["pivot"] = pivot
    if rotation is not None:
        c["rotation"] = rotation
    return c


BONES = []  # each: name, parent (or None), pivot, cubes


def bone(name, parent, pivot, cubes):
    BONES.append({"name": name, "parent": parent, "pivot": pivot, "cubes": cubes})


# Root: roughly center-front, everything hangs off it.
bone("root", None, [0, 8, 8], [])

# Head: pivot at the skull base.
bone("head", "root", [0, 8, 15], [
    cube("headscale", [-5.5, 4.5, 14], [11, 9, 12], name="skull"),
    cube("white", [-5.5, 12.0, 21], [5, 2, 5], name="brow_l"),
    cube("white", [0.5, 12.0, 21], [5, 2, 5], name="brow_r"),
    cube("eye", [-5.2, 9.5, 24.6], [2, 2, 1.6], glow="eye", name="eye_l"),
    cube("eye", [3.2, 9.5, 24.6], [2, 2, 1.6], glow="eye", name="eye_r"),
    cube("white", [-4.2, 2.6, 23.5], [1.6, 2.2, 1.6], name="skull_fang_l"),
    cube("white", [2.6, 2.6, 23.5], [1.6, 2.2, 1.6], name="skull_fang_r"),
])

# Snout: tapered muzzle extending +Z from the skull front (pivot z 26).
bone("snout", "head", [0, 8, 26], [
    cube("headscale", [-4, 5.5, 26], [8, 5, 8], name="muzzle"),
    cube("headscale", [-3, 9.5, 28], [6, 2, 6], name="muzzle_top"),
    cube("headscale", [-3, 10.8, 32.2], [2, 1.4, 2], name="nostril_l"),
    cube("headscale", [1, 10.8, 32.2], [2, 1.4, 2], name="nostril_r"),
])

# Jaw: lower jaw, slightly narrower than the skull, upward fangs at the front.
bone("jaw", "head", [0, 6, 26], [
    cube("headscale", [-3.5, 4.0, 25], [7, 2, 9], name="jaw_box"),
    cube("white", [-3.4, 6.0, 31.8], [1.5, 2.6, 1.5], name="jaw_fang_l"),
    cube("white", [1.9, 6.0, 31.8], [1.5, 2.6, 1.5], name="jaw_fang_r"),
])

# Horns: 3 stacked shrinking segments per side, stepped back, up and out,
# each with its own pivot and rotation so the sweep reads as one curve.
def horn_cubes(side):
    # Right side authored directly; left side is its exact X mirror.
    right = [
        # origin, size, pivot, rotation
        ([1.5, 12.2, 12.5], [3, 3, 6], [3.0, 13.5, 15.5], [18, 0, -14]),
        ([3.35, 14.6, 8.2], [2.5, 2.5, 5], [4.6, 15.8, 10.7], [26, 0, -20]),
        ([5.0, 16.8, 4.6], [2, 2, 4.4], [6.0, 17.8, 6.8], [34, 0, -26]),
    ]
    out = []
    for i, (org, size, piv, rot) in enumerate(right):
        o, p, rt = list(org), list(piv), list(rot)
        if side < 0:
            o[0] = -o[0] - size[0]
            p[0] = -p[0]
            rt[2] = -rt[2]
        out.append(cube("horn", o, size, pivot=p, rotation=rt,
                        glow="horn_tip" if i == 2 else None,
                        name="horn_seg%d" % i))
    return out

bone("horn_l", "head", [-3, 13, 16], horn_cubes(-1))
bone("horn_r", "head", [3, 13, 16], horn_cubes(1))

# Whiskers: 2 thin segments per side, angled down and forward off the snout.
def whisker_cubes(side):
    s = side
    a_org = [s * 4.2 - 0.5 if s > 0 else s * 4.2 - 0.5, 5.6, 31.0]
    b_org = [s * 6.4 - 0.5 if s > 0 else s * 6.4 - 0.5, 3.6, 36.4]
    if s < 0:
        a_org[0] = -4.2 - 0.5
        b_org[0] = -6.4 - 0.5
    else:
        a_org[0] = 4.2 - 0.5
        b_org[0] = 6.4 - 0.5
    return [
        cube("whisker", a_org, [1, 1, 7], pivot=[s * 4.2, 6.1, 31.5],
             rotation=[12, s * -16, 0], name="whisker_a"),
        cube("whisker", b_org, [1, 1, 6.4], pivot=[s * 6.4, 4.1, 36.9],
             rotation=[18, s * -24, 0], glow="whisker_tip", name="whisker_b"),
    ]

bone("whisker_l", "head", [-4, 6.5, 31], whisker_cubes(-1))
bone("whisker_r", "head", [4, 6.5, 31], whisker_cubes(1))

# Mane: fin plates behind the skull, thin in X, tall in Y.
bone("mane", "head", [0, 10, 14], [
    cube("fin", [-0.75, 10.0, 9.5], [1.5, 8, 5], glow="fin", name="mane_main"),
    cube("fin", [-0.75, 7.6, 6.2], [1.5, 5, 3.6], glow="fin", name="mane_low"),
])

# Body chain: seg0 behind the head, each next segment parented to the last.
# Pivots sit at each segment front face (spacing 10, cube length 10) so
# rotations accumulate into a traveling wave down the chain.
SEG_SIZE = [
    (10, 9), (10, 9), (9, 9), (9, 8), (8, 8), (8, 7), (7, 7), (6, 6),
]
SEG_PIVOT_Z = [14 - 10 * i for i in range(8)]
for i in range(8):
    w, h = SEG_SIZE[i]
    pz = SEG_PIVOT_Z[i]
    parent = "root" if i == 0 else "seg%d" % (i - 1)
    bone("seg%d" % i, parent, [0, 8, pz], [
        cube("scale", [-w / 2.0, 8 - h / 2.0, pz - 10], [w, h, 10],
             glow="dorsal", name="body%d" % i),
    ])

# Dorsal fins on seg1, seg3, seg5.
for seg_i, fh, fl in ((1, 7, 6), (3, 6, 5), (5, 5, 5)):
    w, h = SEG_SIZE[seg_i]
    pz = SEG_PIVOT_Z[seg_i]
    top = 8 + h / 2.0
    bone("fin%d" % seg_i, "seg%d" % seg_i, [0, top, pz - 5], [
        cube("fin", [-0.75, top - 0.4, pz - 5 - fl / 2.0], [1.5, fh, fl],
             glow="fin", name="fin%d_plate" % seg_i),
    ])

# Arms: children of seg1. One bone per arm holding upper arm, forearm and a
# 3-toe fanned claw, kept small so the silhouette stays dragon, not humanoid.
def arm_cubes(side):
    s = side  # -1 left, +1 right
    x_shoulder = s * 5.0
    upper_org = [x_shoulder - 1.5 if s > 0 else x_shoulder - 1.5, 2.2, 0.5]
    fore_org = [x_shoulder + s * 1.6 - 1.25, -1.2, 0.2]
    if s < 0:
        upper_org[0] = -6.5
        fore_org[0] = -7.85
    toes = []
    toe_base_x = fore_org[0] + 0.25
    for k, dz in enumerate((-1.9, 0.0, 1.9)):
        toes.append(cube(
            "white",
            [toe_base_x, -3.9, 0.9 + dz],
            [1, 3.2, 1],
            pivot=[toe_base_x + 0.5, -1.0, 1.4 + dz],
            rotation=[(k - 1) * 22, 0, s * 8],
            name="toe%d" % k))
    return [
        cube("scale", upper_org, [3, 4.2, 3],
             pivot=[x_shoulder, 6.2, 2.0], rotation=[0, 0, s * 18],
             name="upper_arm"),
        cube("scale", fore_org, [2.5, 3.6, 2.5],
             pivot=[fore_org[0] + 1.25, 2.2, 1.45], rotation=[0, 0, s * -10],
             name="forearm"),
    ] + toes

bone("arm_l", "seg1", [-5, 6, 2], arm_cubes(-1))
bone("arm_r", "seg1", [5, 6, 2], arm_cubes(1))

# Tail fin: fanned 3-plate fin on the last segment.
bone("tail_fin", "seg7", [0, 8, -64], [
    cube("fin", [-0.75, 5.0, -72], [1.5, 6, 8], glow="fin", name="tail_center"),
    cube("fin", [-3.6, 5.4, -71], [3, 5.2, 7], pivot=[-1.0, 7.5, -66],
         rotation=[0, 26, 0], glow="fin", name="tail_left"),
    cube("fin", [0.6, 5.4, -71], [3, 5.2, 7], pivot=[1.0, 7.5, -66],
         rotation=[0, -26, 0], glow="fin", name="tail_right"),
])

TOTAL_CUBES = sum(len(b["cubes"]) for b in BONES)


# ---------------------------------------------------------------------------
# UV packing: classic Bedrock box UV. For a cube of size (w, h, d) the UV
# region is (2w + 2d) wide and (d + h) tall:
#   row 0:        [pad d][top w][bottom w][pad d]
#   row 1 (d..):  [west d][north w][east d][south w], height h
# A simple shelf packer lays regions out on the 256x256 atlas.
# ---------------------------------------------------------------------------

def region_size(size):
    w, h, d = size
    return (2 * w + 2 * d, d + h)


def face_rects(uv, size):
    """Return {face: (x, y, fw, fh)} in atlas texels for a box-UV cube."""
    u0, v0 = uv
    w, h, d = size
    return {
        "top": (u0 + d, v0, w, d),
        "bottom": (u0 + d + w, v0, w, d),
        "west": (u0, v0 + d, d, h),
        "north": (u0 + d, v0 + d, w, h),
        "east": (u0 + d + w, v0 + d, d, h),
        "south": (u0 + 2 * d + w, v0 + d, w, h),
    }


def pack_uvs():
    flat = []
    for b in BONES:
        for c in b["cubes"]:
            rw, rh = region_size(c["size"])
            flat.append((rh, rw, c))
    flat.sort(key=lambda t: (-t[0], -t[1]))
    x = y = 0
    shelf_h = 0
    for rh, rw, c in flat:
        if x + rw > TEX_SIZE:
            x = 0
            y += shelf_h
            shelf_h = 0
        c["uv"] = (x, y)
        x += rw + 1
        shelf_h = max(shelf_h, rh)
    used_h = y + shelf_h
    assert used_h <= TEX_SIZE, "UV atlas overflow: height %s" % used_h
    return used_h


# ---------------------------------------------------------------------------
# Texture painting. Every face is painted as a small tile from the same cube
# constants, then pasted at its atlas rect, so geometry and texture can never
# drift apart. Face shading: top lightest, bottom darkest.
# ---------------------------------------------------------------------------

FACE_SHADE = {"top": 1.06, "bottom": 0.72, "north": 0.97,
              "south": 1.0, "west": 0.9, "east": 0.93}


def shade(col, k):
    return tuple(max(0, min(255, int(round(v * k)))) for v in col)


def lerp_col(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def paint_scales(draw, fw, fh, base, belly_blend=0.0):
    """Deep azure base with a scalloped scale pattern and rim highlights.

    belly_blend lightens the lower rows toward the belly color so the pale
    underside wraps a little way up the sides of the body.
    """
    for yy in range(fh):
        t = yy / max(1, fh - 1)
        col = base
        if belly_blend > 0 and t > 0.55:
            col = lerp_col(base, BELLY, belly_blend * (t - 0.55) / 0.45)
        draw.line([(0, yy), (fw, yy)], fill=shade(col, 1.0))
    # scallops: rows of overlapping arcs, alternating offset
    sw, sh = 4, 3
    row = 0
    yy = 0
    while yy < fh + sh:
        off = (sw // 2) if row % 2 else 0
        for xx in range(-sw, fw + sw, sw):
            box = [xx + off, yy, xx + off + sw, yy + sh * 2]
            draw.arc(box, start=0, end=180, fill=DEEP_DARK, width=1)
            draw.point((xx + off + sw // 2, yy + sh), fill=RIM)
        yy += sh
        row += 1


def paint_belly(draw, fw, fh):
    """Pale ice blue belly with segment plate lines."""
    draw.rectangle([0, 0, fw, fh], fill=BELLY)
    for yy in range(2, fh, 3):
        draw.line([(0, yy), (fw, yy)], fill=BELLY_LINE)
    for xx in range(fw):
        edge = 0.85 + 0.15 * abs(xx - fw / 2.0) / max(1.0, fw / 2.0)
        for yy in range(fh):
            if (xx, yy) == (0, 0):
                continue
    draw.line([(0, 0), (0, fh)], fill=BELLY_LINE)
    draw.line([(fw - 1, 0), (fw - 1, fh)], fill=BELLY_LINE)


def paint_fin(draw, fw, fh):
    """Pale cyan membrane with darker ribs fanning from the base."""
    draw.rectangle([0, 0, fw, fh], fill=FIN_COL)
    cx = fw / 2.0
    ribs = max(3, fw // 3)
    for k in range(ribs + 1):
        tx = (fw - 1) * k / float(ribs)
        draw.line([(cx, fh - 1), (tx, 0)], fill=FIN_RIB)
    draw.rectangle([0, 0, fw - 1, fh - 1], outline=FIN_RIB)


def paint_horn(draw, fw, fh):
    draw.rectangle([0, 0, fw, fh], fill=HORN_COL)
    for i in range(-fh, fw, 4):
        draw.line([(i, fh), (i + fh, 0)], fill=HORN_BAND)


def paint_white(draw, fw, fh):
    draw.rectangle([0, 0, fw, fh], fill=WHITE)
    if fh > 2:
        draw.line([(0, fh - 1), (fw, fh - 1)], fill=WHITE_SHADE)
        draw.line([(0, fh - 2), (fw, fh - 2)], fill=WHITE_SHADE)


def paint_eye(draw, fw, fh):
    draw.rectangle([0, 0, fw, fh], fill=EYE_COL)
    cx, cy = fw / 2.0, fh / 2.0
    draw.ellipse([cx - 1, cy - 1, cx + 1, cy + 1], fill=CORE)
    draw.rectangle([0, 0, fw - 1, fh - 1], outline=MID)


def paint_whisker(draw, fw, fh):
    for xx in range(fw):
        t = xx / max(1, fw - 1)
        draw.line([(xx, 0), (xx, fh)], fill=lerp_col(MID, RIM, t))


PAINTERS = {
    "horn": paint_horn,
    "fin": paint_fin,
    "white": paint_white,
    "eye": paint_eye,
    "whisker": paint_whisker,
}


def paint_face(c, face, fw, fh):
    tile = Image.new("RGB", (max(1, int(fw)), max(1, int(fh))), DEEP)
    draw = ImageDraw.Draw(tile)
    mat = c["mat"]
    if mat in ("scale", "headscale"):
        base = DEEP if mat == "scale" else HEAD_BASE
        if face == "bottom":
            paint_belly(draw, tile.width, tile.height)
        elif face == "top":
            paint_scales(draw, tile.width, tile.height, shade(base, 1.08))
            # bright dorsal ridge line down the center of the back
            cx = tile.width // 2
            draw.line([(cx, 0), (cx, tile.height)], fill=RIM)
        else:
            paint_scales(draw, tile.width, tile.height, base,
                         belly_blend=0.55 if mat == "scale" else 0.3)
    else:
        PAINTERS[mat](draw, tile.width, tile.height)
    # per-face shading pass
    k = FACE_SHADE[face]
    if k != 1.0:
        px = tile.load()
        for yy in range(tile.height):
            for xx in range(tile.width):
                px[xx, yy] = shade(px[xx, yy], k)
    return tile


def build_textures():
    tex = Image.new("RGB", (TEX_SIZE, TEX_SIZE), (0, 0, 0))
    glow = Image.new("RGB", (TEX_SIZE, TEX_SIZE), (0, 0, 0))
    for b in BONES:
        for c in b["cubes"]:
            rects = face_rects(c["uv"], c["size"])
            for face, (x, y, fw, fh) in rects.items():
                fw_i, fh_i = int(round(fw)), int(round(fh))
                if fw_i <= 0 or fh_i <= 0:
                    continue
                tile = paint_face(c, face, fw_i, fh_i)
                tex.paste(tile, (int(x), int(y)))
                gtile = glow_face(c, face, fw_i, fh_i)
                if gtile is not None:
                    glow.paste(gtile, (int(x), int(y)))
    return tex, glow


def glow_face(c, face, fw, fh):
    """Paint one glowmask face tile, or None when this face stays black."""
    tag = c["glow"]
    if tag is None:
        return None
    tile = Image.new("RGB", (fw, fh), (0, 0, 0))
    draw = ImageDraw.Draw(tile)
    if tag == "eye":
        draw.rectangle([0, 0, fw, fh], fill=GLOW)
        cx, cy = fw // 2, fh // 2
        draw.rectangle([cx, cy, cx + 1, cy + 1], fill=CORE)
    elif tag == "fin":
        # membrane center glows, ribs and edges stay dark
        if face in ("west", "east", "north", "south"):
            x0 = max(0, fw // 2 - 1)
            draw.rectangle([x0, 1, min(fw - 1, fw // 2 + 1), fh - 2],
                           fill=GLOW_DIM)
            draw.line([(fw // 2, 1), (fw // 2, fh - 2)], fill=GLOW)
    elif tag == "horn_tip":
        draw.rectangle([0, 0, fw, fh], fill=GLOW_DIM)
    elif tag == "whisker_tip":
        # only the far (front, +Z) end of the tip segment glows
        if face == "south":
            draw.rectangle([0, 0, fw - 1, fh - 1], fill=GLOW)
        elif face in ("west", "east"):
            draw.rectangle([fw // 2, 0, fw - 1, fh - 1], fill=GLOW_DIM)
        elif face == "north":
            draw.rectangle([0, 0, fw - 1, fh - 1], fill=(40, 80, 100))
    elif tag == "dorsal":
        # dorsal stripe: center band of the top face only
        if face == "top":
            cx = fw // 2
            draw.rectangle([max(0, cx - 1), 0, min(fw - 1, cx + 1), fh - 1],
                           fill=GLOW)
    return tile


# ---------------------------------------------------------------------------
# Geo emission
# ---------------------------------------------------------------------------

def build_geo():
    bones = []
    for b in BONES:
        bone = {"name": b["name"], "pivot": vec(b["pivot"])}
        if b["parent"]:
            bone["parent"] = b["parent"]
        cubes = []
        for c in b["cubes"]:
            out = {"origin": vec(c["origin"]), "size": vec(c["size"]),
                   "uv": list(c["uv"])}
            if "pivot" in c:
                out["pivot"] = vec(c["pivot"])
            if "rotation" in c:
                out["rotation"] = vec(c["rotation"])
            cubes.append(out)
        if cubes:
            bone["cubes"] = cubes
        bones.append(bone)
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.dragonblockarcanedba.azure_dragon",
                "texture_width": TEX_SIZE,
                "texture_height": TEX_SIZE,
                "visible_bounds_width": 14,
                "visible_bounds_height": 8,
                "visible_bounds_offset": [0, 0, -1],
            },
            "bones": bones,
        }],
    }


# ---------------------------------------------------------------------------
# Animations: a traveling sine wave down the segment chain.
#
# wave value for a bone at time t: amp * sin(2*pi*t/length - phase_index*step)
# where step comes from a fixed time lag per segment, so the wave visibly
# travels from head to tail. Keyframes are plain arrays (the Shenron file in
# this mod uses the same shape), sampled 9 times per loop so linear
# interpolation still reads smooth, with the last key equal to the first.
# ---------------------------------------------------------------------------

def sample_wave(length, keys, fn):
    out = {}
    for k in range(keys):
        t = length * k / float(keys - 1)
        out["%.2f" % t] = vec(fn(t))
    return out


def build_clip(length, lag, seg_amp_lo, seg_amp_hi, head_amp, tail_amp,
               jaw_amp, fin_amp, keys=9):
    step = 2.0 * math.pi * lag / length  # phase offset per chain index

    def phase(t, idx):
        return 2.0 * math.pi * t / length - idx * step

    bones = {}
    # root: gentle vertical bob, two beats per loop
    bones["root"] = {"position": sample_wave(
        length, keys, lambda t: (0, 0.5 * math.sin(2 * phase(t, 0)), 0))}
    # head leads the wave: opposite sign, small amplitude
    bones["head"] = {"rotation": sample_wave(
        length, keys,
        lambda t: (1.5 * math.sin(phase(t, 0) + math.pi / 2.0),
                   -head_amp * math.sin(phase(t, -1)),
                   0))}
    # jaw opens and closes 0 to jaw_amp degrees (twice per loop reads alive)
    bones["jaw"] = {"rotation": sample_wave(
        length, keys,
        lambda t: (jaw_amp * (0.5 - 0.5 * math.cos(2 * phase(t, 1))), 0, 0))}
    # whiskers trail the head motion
    for nm, sgn in (("whisker_l", 1), ("whisker_r", 1)):
        bones[nm] = {"rotation": sample_wave(
            length, keys,
            lambda t, s=sgn: (5 * math.sin(phase(t, 2) - 0.6),
                              3 * math.sin(phase(t, 2)), 0))}
    # mane ripples gently
    bones["mane"] = {"rotation": sample_wave(
        length, keys, lambda t: (0, 0, 3 * math.sin(phase(t, 1))))}
    # body segments: amplitude grows from neck to tail
    for i in range(8):
        amp = seg_amp_lo + (seg_amp_hi - seg_amp_lo) * i / 7.0
        bones["seg%d" % i] = {"rotation": sample_wave(
            length, keys,
            lambda t, i=i, a=amp: (
                2.0 * math.sin(phase(t, i) + math.pi / 2.0),
                a * math.sin(phase(t, i)),
                0))}
    # dorsal fins ripple side to side, chasing their segment
    for nm, idx in (("fin1", 1), ("fin3", 3), ("fin5", 5)):
        bones[nm] = {"rotation": sample_wave(
            length, keys,
            lambda t, i=idx: (0, 0, fin_amp * math.sin(phase(t, i) - 0.5)))}
    # arms sway against the wave so the claws trail
    bones["arm_l"] = {"rotation": sample_wave(
        length, keys, lambda t: (0, 0, 3 * math.sin(phase(t, 1) - 0.8)))}
    bones["arm_r"] = {"rotation": sample_wave(
        length, keys, lambda t: (0, 0, -3 * math.sin(phase(t, 1) - 0.8)))}
    # tail fin: largest swing, lagging one step behind seg7
    bones["tail_fin"] = {"rotation": sample_wave(
        length, keys,
        lambda t: (1.5 * math.sin(phase(t, 8) + math.pi / 2.0),
                   tail_amp * math.sin(phase(t, 8)), 0))}
    return {"loop": True, "animation_length": length, "bones": bones}


def build_animations():
    swim = build_clip(length=1.6, lag=0.2, seg_amp_lo=4.0, seg_amp_hi=10.0,
                      head_amp=3.0, tail_amp=12.0, jaw_amp=4.0, fin_amp=4.0)
    idle = build_clip(length=3.2, lag=0.4, seg_amp_lo=2.0, seg_amp_hi=5.0,
                      head_amp=1.5, tail_amp=6.0, jaw_amp=2.0, fin_amp=2.0)
    return {"format_version": "1.10.0",
            "animations": {"swim": swim, "idle": idle}}


# ---------------------------------------------------------------------------

def main():
    used_h = pack_uvs()
    geo = build_geo()
    anim = build_animations()
    tex, glow = build_textures()
    GEO_PATH.parent.mkdir(parents=True, exist_ok=True)
    ANIM_PATH.parent.mkdir(parents=True, exist_ok=True)
    TEX_PATH.parent.mkdir(parents=True, exist_ok=True)
    GEO_PATH.write_text(json.dumps(geo, indent=2))
    ANIM_PATH.write_text(json.dumps(anim, indent=2))
    tex.save(TEX_PATH)
    glow.save(GLOW_PATH)
    print("bones: %d" % len(BONES))
    print("bone names: %s" % ", ".join(b["name"] for b in BONES))
    print("cubes: %d" % TOTAL_CUBES)
    print("uv atlas height used: %d of %d" % (used_h, TEX_SIZE))
    print("wrote %s" % GEO_PATH.relative_to(ROOT))
    print("wrote %s" % ANIM_PATH.relative_to(ROOT))
    print("wrote %s" % TEX_PATH.relative_to(ROOT))
    print("wrote %s" % GLOW_PATH.relative_to(ROOT))


if __name__ == "__main__":
    main()
