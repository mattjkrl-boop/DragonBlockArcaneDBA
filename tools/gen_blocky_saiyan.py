"""Blocky polished Saiyan (approved concept): cube model + pixel-art atlas + mask.

Writes:
  assets/blockbench/races/saiyan.bbmodel      Bedrock-format cube project (editable in Blockbench)
  models/3d/saiyan.obj                         runtime mesh, groups head/hair/body/arms/legs
  textures/entity/player/saiyan_base.png      grayscale-value skin/hair + painted outfit
  textures/entity/player/saiyan_mask.png      R = skin tint, G = hair tint, B = eye tint

Authoring space is Blockbench's: px units, y up, character faces north (-z), the
character's right side is +x. OBJ space is (-x, y, -z) / 16 (front +z, right arm -x).
Cube rotations use Blockbench's order (X, then Y, then Z) about the cube origin.
"""
import json, zlib, math, pathlib, base64, io, uuid
from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / 'src/main/resources/assets/dragonblockarcanedba'
TEX = 512
D = 2          # texels per model pixel (keeps a crisp pixel-art look)

cubes = []     # dict(name, bone, mat, f, t, o, r, export)
def cube(name, bone, mat, f, t, o=None, r=(0, 0, 0), export=True):
    cubes.append(dict(name=name, bone=bone, mat=mat, f=list(f), t=list(t), o=list(o or [0, 0, 0]), r=list(r), export=export))
def mirror(name, bone, mat, f, t, o=None, r=(0, 0, 0), **k):
    """Add a cube and its mirror across x=0 (rotations about Y and Z flip)."""
    cube(name + '_r', bone[0], mat, f, t, o, r, **k)
    f2 = [-t[0], f[1], f[2]]; t2 = [-f[0], t[1], t[2]]
    o2 = [-(o or [0, 0, 0])[0], (o or [0, 0, 0])[1], (o or [0, 0, 0])[2]]
    cube(name + '_l', bone[1], mat, f2, t2, o2, (r[0], -r[1], -r[2]), **k)

def spike(name, base, L, w, rot, both=True, bone='hair'):
    """Tapered hair spike: four stacked cubes growing along local +y from base, ending in a point."""
    segs = [(0.0, 0.34, 1.0), (0.28, 0.58, 0.64), (0.52, 0.80, 0.38), (0.74, 1.0, 0.16)]
    for i, (a, b, s) in enumerate(segs):
        ww = w * s
        f = [base[0] - ww / 2, base[1] + a * L, base[2] - ww / 2]
        t = [base[0] + ww / 2, base[1] + b * L, base[2] + ww / 2]
        if both: mirror('%s_%s%d' % (bone, name, i), (bone, bone), 'hair', f, t, base, rot)
        else: cube('%s_%s%d' % (bone, name, i), bone, 'hair', f, t, base, rot)

# ------------------------------------------------------------------ model
# Legs: flush at x=0 and under the hips, baggy gi pants, chunky boots with a tan band
for side, bone in ((1, 'right_leg'), (-1, 'left_leg')):
    def X(a, b): return (a, b) if side > 0 else (-b, -a)
    x = X(0, 5)
    cube('pants_' + bone, bone, 'pants', [x[0], 4.2, -2.6], [x[1], 12, 2.6])
    xb = X(0.0, 5.3) if side > 0 else X(0.0, 5.3)
    cube('pants_bag_' + bone, bone, 'pants', [xb[0], 5.4, -2.95], [xb[1], 9.6, 2.95])
    xc = X(0.15, 4.85)
    cube('pants_cuff_' + bone, bone, 'pants_cuff', [xc[0], 4.0, -2.8], [xc[1], 5.4, 2.7])
    xo = X(0.3, 4.7)
    cube('boot_' + bone, bone, 'boot', [xo[0], 0, -2.7], [xo[1], 4.4, 2.4])
    cube('boot_toe_' + bone, bone, 'boot', [xo[0], 0, -3.8], [xo[1], 2.2, -2.7])
    xd = X(0.2, 4.8)
    cube('boot_band_' + bone, bone, 'band', [xd[0], 3.5, -2.85], [xd[1], 4.5, 2.55])
# Sash ends hang over the right thigh (in the leg bone so the torso keeps its 12px height)
cube('sash_end_a', 'right_leg', 'belt', [0.4, 6.4, -3.25], [1.6, 11.9, -2.95], [1.0, 11.9, -3.1], (0, 0, 6))
cube('sash_end_b', 'right_leg', 'belt', [1.7, 7.4, -3.25], [2.9, 11.9, -2.95], [2.3, 11.9, -3.1], (0, 0, -8))

# Torso: broad gi chest, blue sash with a knot
cube('torso', 'body', 'gi', [-5, 12, -3], [5, 24, 3])
cube('chest', 'body', 'gi', [-5.5, 16.5, -3.35], [5.5, 24, 3.15])
cube('belt', 'body', 'belt', [-5.35, 12.4, -3.4], [5.35, 15.0, 3.3])
cube('belt_knot', 'body', 'belt_knot', [-0.4, 12.5, -4.1], [2.2, 15.2, -3.3])

# Arms: flush against the chest, blue undershirt sleeve under an orange gi shoulder
mirror('arm_skin', ('right_arm', 'left_arm'), 'skin_arm', [5.5, 13.6, -2.0], [9.5, 19.8, 2.0])
mirror('bicep', ('right_arm', 'left_arm'), 'skin_arm', [5.5, 15.6, -2.25], [9.75, 19.6, 2.2])
mirror('sleeve', ('right_arm', 'left_arm'), 'under', [5.4, 19.4, -2.4], [9.9, 22.9, 2.4])
mirror('gi_shoulder', ('right_arm', 'left_arm'), 'gi', [5.3, 22.6, -2.65], [9.7, 24.3, 2.65])
mirror('wristband', ('right_arm', 'left_arm'), 'wrist', [5.3, 11.6, -2.3], [9.7, 13.8, 2.3])
mirror('hand', ('right_arm', 'left_arm'), 'hand', [5.6, 9.0, -1.9], [9.4, 11.7, 1.9])

# Head. Eyes are painted on the face (two rows) and blink by swapping those texels.
cube('head', 'head', 'head', [-4, 24, -4], [4, 32, 4])
mirror('ear', ('head', 'head'), 'skin', [4.0, 27.0, -0.6], [4.7, 29.2, 0.9])

# Hair: scalp cap and back mass, then fewer, larger tapered spikes sweeping up and back (concept),
# side spikes flaring out, 4 pointed bang locks, back spikes pointing down and back.
# Built twice: 'hair' (base and SSJ/SSJ2) and 'hair_ssj3' (same crown plus a long mane down the back).
HAIR_SPIKES = [
    # name, base, length, width, rotation (x tilts tip back, -z tilts tip out to +x), mirrored
    ('top', [0.6, 32.4, 0.4], 11.5, 6.0, (30, 0, -6), False),
    ('upper', [2.6, 32.0, 0.0], 11.0, 5.6, (20, 0, -30), True),
    ('side_hi', [3.8, 30.8, 0.6], 10.0, 5.0, (14, 0, -58), True),
    ('side', [4.0, 28.8, 1.2], 7.5, 4.6, (10, 0, -80), True),
    ('back_hi', [0, 31.0, 3.4], 10.5, 5.8, (60, 0, 0), False),
    ('back_side', [2.6, 30.0, 3.4], 9.0, 5.0, (72, 0, -40), True),
    ('back_lo', [0, 27.6, 3.9], 7.5, 5.2, (120, 0, 0), False),
    ('back_lo_side', [2.8, 27.2, 3.6], 6.5, 4.4, (116, 0, -40), True),
]
BANGS = [('bang_in', [1.1, 32.9, -4.3], 4.0, 3.2, (6, 0, 186)), ('bang_out', [3.2, 32.7, -4.2], 3.8, 3.0, (8, 0, 208))]
SSJ2_SPIKES = [
    # SSJ2: spikier and more upright, thinner and longer, a single bang lock
    ('top', [0.6, 32.4, 0.4], 13.5, 5.2, (16, 0, -4), False),
    ('upper', [2.4, 32.0, 0.2], 13.0, 4.6, (12, 0, -20), True),
    ('upper2', [1.4, 32.2, 2.2], 12.0, 4.4, (30, 0, -12), True),
    ('side_hi', [3.8, 30.8, 0.6], 11.5, 4.2, (10, 0, -44), True),
    ('side', [4.0, 28.8, 1.2], 9.0, 4.0, (8, 0, -68), True),
    ('back_hi', [0, 31.0, 3.4], 12.0, 5.0, (46, 0, 0), False),
    ('back_side', [2.6, 30.0, 3.4], 10.5, 4.4, (58, 0, -34), True),
    ('back_lo', [0, 27.6, 3.9], 8.5, 4.6, (108, 0, 0), False),
    ('back_lo_side', [2.8, 27.2, 3.6], 7.5, 4.0, (104, 0, -36), True),
]
for hb in ('hair', 'hair_ssj3', 'hair_ssj2'):
    cube(hb + '_top', hb, 'hair', [-4.4, 30.6, -4.5], [4.4, 33.0, 4.5])
    cube(hb + '_back', hb, 'hair', [-4.45, 25.0, 0.2], [4.45, 32.2, 4.6])
    mirror(hb + '_side', (hb, hb), 'hair', [3.9, 29.2, -3.6], [4.5, 32.0, 1.0])
    mirror(hb + '_burn', (hb, hb), 'hair', [3.9, 27.2, -3.2], [4.4, 29.4, -1.4])
    for n, bs, L, w, r, both in (SSJ2_SPIKES if hb == 'hair_ssj2' else HAIR_SPIKES):
        spike(n, bs, L, w, r, both, bone=hb)
    if hb == 'hair_ssj2':
        spike('bang_one', [1.4, 32.9, -4.4], 6.2, 3.0, (4, 0, 196), False, bone=hb)
    else:
        for n, bs, L, w, r in BANGS:
            spike(n, bs, L, w, r, True, bone=hb)
# SSJ2 electricity: small zigzag bolts crackling around the crown (unmasked, stay electric blue-white)
for k, (bx, by, bz, ry) in enumerate([(6.2, 33.5, -0.5, 0), (-6.6, 31.5, 1.0, 0), (2.5, 40.5, -1.0, 90), (-3.5, 37.0, 4.5, 90), (7.0, 27.5, 2.0, 0)]):
    zz = [(0, 0, 1.8, 18), (0.9, 1.6, 1.8, -28), (0.2, 3.1, 1.6, 22)]
    for q, (dx, dy, l, rz) in enumerate(zz):
        o = [bx + dx, by + dy, bz]
        cube('hair_ssj2_bolt%d_%d' % (k, q), 'hair_ssj2', 'spark', [o[0] - 0.4, o[1], o[2] - 0.4], [o[0] + 0.4, o[1] + l, o[2] + 0.4], o, (0, ry, rz))
# SSJ3 long mane: thick tapered locks from the back of the scalp down past the waist
cube('hair_ssj3_mane_root', 'hair_ssj3', 'hair', [-5.0, 22.0, 1.0], [5.0, 31.5, 6.0])
spike('mane_c', [0, 26.0, 4.8], 22.0, 8.4, (168, 0, 0), False, bone='hair_ssj3')
spike('mane_s', [2.8, 26.5, 4.0], 19.0, 6.4, (168, 0, 4), True, bone='hair_ssj3')
spike('mane_o', [4.2, 28.0, 2.6], 15.0, 4.6, (170, 0, 7), True, bone='hair_ssj3')
# front-visible mane: wide locks falling past the ears to the shoulders and flaring out
spike('mane_f', [4.5, 29.0, -0.6], 11.0, 3.8, (176, 0, 14), True, bone='hair_ssj3')
spike('mane_w', [5.0, 28.0, 1.6], 14.0, 4.6, (172, 0, 18), True, bone='hair_ssj3')

# Tail preview (Blockbench only; the runtime tail is the segmented TrailingTailLayer)
pts = [(0, 13.5, 3.0), (0, 12.2, 5.6), (0, 10.0, 7.8), (0, 7.4, 9.4), (0, 4.8, 10.6), (0, 2.8, 12.6), (0, 2.6, 15.2), (0, 4.4, 17.0), (0, 6.6, 17.2)]
for i in range(len(pts) - 1):
    a, b = pts[i], pts[i + 1]
    dy, dz = b[1] - a[1], b[2] - a[2]
    L = math.hypot(dy, dz); ang = math.degrees(math.atan2(dz, dy))
    w = 2.6 - i * 0.12
    cube('tail_%d' % i, 'tail', 'tail_tip' if i == len(pts) - 2 else ('tail' if i % 2 == 0 else 'tail_dark'),
         [a[0] - w / 2, a[1], a[2] - w / 2], [a[0] + w / 2, a[1] + L + 0.3, a[2] + w / 2], list(a), (ang, 0, 0), export=False)

# ------------------------------------------------------------------ geometry helpers
def rot(p, o, r):
    x, y, z = p[0] - o[0], p[1] - o[1], p[2] - o[2]
    ax, ay, az = (math.radians(a) for a in r)
    y, z = y * math.cos(ax) - z * math.sin(ax), y * math.sin(ax) + z * math.cos(ax)
    x, z = x * math.cos(ay) + z * math.sin(ay), -x * math.sin(ay) + z * math.cos(ay)
    x, y = x * math.cos(az) - y * math.sin(az), x * math.sin(az) + y * math.cos(az)
    return (x + o[0], y + o[1], z + o[2])

def face_corners(c):
    (x0, y0, z0), (x1, y1, z1) = c['f'], c['t']
    return {  # TL, TR, BR, BL as seen from outside (Blockbench UV orientation)
        'north': [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
        'south': [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
        'east':  [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
        'west':  [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
        'up':    [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
        'down':  [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)],
    }
def dist(a, b): return math.sqrt(sum((a[i] - b[i]) ** 2 for i in range(3)))

# ------------------------------------------------------------------ UV packing
class Packer:
    def __init__(s): s.x = s.y = s.row = 0
    def take(s, w, h):
        if s.x + w > TEX: s.x, s.y, s.row = 0, s.y + s.row, 0
        r = (s.x, s.y); s.x += w; s.row = max(s.row, h)
        assert s.y + h <= TEX, 'atlas full'
        return r
pk = Packer()
order = sorted(range(len(cubes)), key=lambda i: -(cubes[i]['t'][1] - cubes[i]['f'][1]))
for i in order:
    c = cubes[i]; c['uv'] = {}
    for fn, q in face_corners(c).items():
        w = max(1, round(dist(q[0], q[1]) * D)); h = max(1, round(dist(q[0], q[3]) * D))
        u, v = pk.take(w, h)
        c['uv'][fn] = (u, v, u + w, v + h)

# ------------------------------------------------------------------ painting
BLINK = False
def h01(*a):
    n = 0
    for v in a: n = (n * 73856093) ^ int(v * 7919 + 1013)
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return (n & 0xFFFF) / 65535.0
def clamp(v): return max(0, min(255, int(round(v))))
def mul(c, k): return tuple(clamp(x * k) for x in c)

ORANGE = (238, 116, 30); ORANGE_D = (196, 82, 18); BLUE = (34, 64, 186); BLUE_D = (22, 40, 132)
TAN = (206, 158, 86); BROWN = (110, 70, 40); SOLE = (40, 40, 52)
SKIN_V = 214; HAIR_V = 210
MASKC = {'R': (255, 0, 0, 255), 'G': (0, 255, 0, 255), 'B': (0, 0, 255, 255), None: (0, 0, 0, 0)}

def paint_texel(c, fn, s, t, W, H, i, j, P):
    """s,t in [0,1] across the face (left to right, top to bottom); P = world point (unrotated)."""
    m = c['mat']; n = h01(zlib.crc32(c['name'].encode()) % 997, zlib.crc32(fn.encode()) % 97, i, j)
    edge = i == 0 or j == 0 or i == W - 1 or j == H - 1
    side = fn in ('east', 'west'); front = fn == 'north'
    if m in ('gi', 'pants', 'pants_cuff'):
        col = ORANGE
        k = 0.94 + 0.10 * n
        if m == 'gi':
            # gi lapels: blue undershirt V and a skin V at the neck on the front of the torso
            if front and c['name'] in ('torso', 'chest'):
                ax = abs(P[0]); y = P[1]
                if y > 23.2 - 0 and ax < 1.6: return (SKIN_V * 0.92,) * 3, 'R'
                if y > 24 - (2.2 - ax) * 1.9 and ax < 2.2: return (SKIN_V * (0.95 - 0.05 * (ax > 1.5)),) * 3, 'R'
                if y > 24 - (4.4 - ax) * 1.75 and ax < 4.4:
                    if y < 24 - (4.4 - ax) * 1.75 + 0.6: return mul(ORANGE_D, 0.8), None
                    return mul(BLUE, 0.95 + 0.08 * n), None
                if y > 24 - (4.4 - ax) * 1.75 - 0.6 and ax < 4.6: return mul(ORANGE_D, 0.92), None
                # diagonal gi fold lines below the chest
                if 15 < y < 21 and abs(((P[0] * 0.8 + y) % 3.2) - 1.6) < 0.35: k *= 0.86
            if fn == 'south' and c['name'] == 'chest' and P[1] > 23.2 and abs(P[0]) < 3.2:
                return mul(BLUE, 0.95), None          # undershirt collar at the back
            if c['name'] == 'torso' and 15 < P[1] < 18 and abs(((P[0] + P[2]) % 2.6) - 1.3) < 0.3: k *= 0.88
        if m == 'pants':
            # baggy folds: wavy horizontal creases and a vertical shadow at the inner leg
            wv = math.sin(P[0] * 1.3 + P[2] * 0.9) * 0.5
            if abs(((P[1] + wv) % 2.4) - 1.2) < 0.3: k *= 0.84
            if abs(P[0]) < 0.6 and front: k *= 0.85
        if m == 'pants_cuff':
            k *= 0.9 if abs(((P[0] + P[2]) % 1.6) - 0.8) < 0.3 else 1.0
        if t > 0.92 and not fn in ('up', 'down'): k *= 0.82
        if edge: k *= 0.86
        if fn == 'down': k *= 0.7
        return mul(col, k), None
    if m in ('under', 'wrist', 'belt', 'belt_knot', 'boot'):
        col = BLUE
        k = 0.92 + 0.12 * n
        if m == 'wrist' and (j == 1 or j == H - 2): k *= 0.82
        if m == 'belt' and abs(((P[1] * 2 + P[0] * 0.3) % 2.0) - 1.0) < 0.25: k *= 0.85
        if m == 'belt_knot' and abs(s - 0.5) < 0.12: k *= 0.75
        if m == 'boot':
            if P[1] < 0.6: return mul(SOLE, 0.9 + 0.1 * n), None
            if abs(P[0]) > 2.4 and abs(abs(P[0]) - 2.5) < 0.25 and front and P[1] > 2: return mul(TAN, 0.9), None   # front seam stripe
        if edge: k *= 0.82
        if fn == 'down': k *= 0.6
        return mul(col, k), None
    if m == 'band':
        k = 0.92 + 0.12 * n
        if j == 0 or j == H - 1: k *= 0.78
        return mul(TAN, k), None
    if m in ('tail', 'tail_dark', 'tail_tip'):
        k = (0.85 if m == 'tail_dark' else 1.0) * (0.7 if m == 'tail_tip' else 1.0) * (0.9 + 0.15 * n)
        return mul(BROWN, k), None
    if m == 'spark':
        return ((235, 250, 255) if (i + j) % 2 == 0 else (120, 200, 255)), None
    if m == 'hair':
        # grayscale value, multiplied by the hair tint in game; lighter toward spike tips, strand streaks
        v = HAIR_V * (0.86 + 0.14 * (1 - t)) * (0.9 + 0.12 * n)
        if i % 3 == 0 and fn not in ('up', 'down'): v *= 0.88
        if edge: v *= 0.8
        if fn == 'down': v *= 0.7
        return (v, v, v), 'G'
    if m in ('skin', 'skin_arm', 'hand', 'head'):
        v = SKIN_V * (0.96 + 0.06 * n)
        if m == 'skin_arm':
            if c['name'].startswith('bicep') and front and abs(s - 0.5) < 0.1: v *= 0.9
            if fn in ('north', 'south') and abs(t - 0.75) < 0.06 and c['name'].startswith('arm'): v *= 0.9   # elbow
        if m == 'hand':
            if fn == 'north' and 0.3 < t < 0.42 and i % 2 == 0: v *= 0.8   # knuckles
            if fn == 'down' or t > 0.85: v *= 0.86
            if fn in ('east', 'west') and t > 0.35 and abs(s - 0.5) < 0.12: v *= 0.82   # curled fingers
        if m == 'head' and fn == 'north':
            r = paint_face(i, j, W, H, BLINK)
            if r: return r
        if m == 'head' and fn == 'down': v *= 0.75
        if edge and m != 'head': v *= 0.88
        return (v, v, v), 'R'
    return (200, 200, 200), None

def paint_face(i, j, W, H, closed=False):
    """16x16 pixel-art Goku face; row 0 is the top of the head front. Two-row eyes."""
    sx = i - W // 2   # screen-left negative
    k = SKIN_V
    # brows: thick and angled down toward the nose (hair tint so they follow hair color)
    for side in (-1, 1):
        a = sx if side > 0 else -sx - 1    # 0..7 from the middle outward
        if 1 <= a <= 6:
            by = 7 - (1 if a >= 4 else 0) - (1 if a >= 6 else 0)
            if j == by or (j == by + 1 and a <= 2): return (110, 110, 110), 'G'
            # eye block: rows 10-11, columns a 2..5
            if j == 9 and 2 <= a <= 6: return (24, 20, 20), None                       # upper lid line
            if j in (10, 11):
                # Top row becomes the lid. The lower row is the shut line, still eye-tinted.
                if closed:
                    if j == 10: return (k * 0.72,) * 3, 'R'
                    return ((140, 140, 140) if a in (2, 6) else (210, 210, 210)), 'B'
                if a == 6 or (a == 2 and j == 10): return (24, 20, 20), None              # outer corner
                if a == 2: return (k * 0.75,) * 3, 'R'
                if a == 3: return (80, 80, 80), 'B'                                      # pupil, same eye tint, darker
                if a == 4: return (235, 235, 235), 'B'                                   # iris, eye color tint
                return (246, 246, 246), None                                             # sclera
            if j == 12 and 3 <= a <= 5: return (k * 0.86,) * 3, 'R'                    # lower lid shade
    # nose: shadow pixel under and beside the bridge
    if j == 13 and sx in (-1, 0): return (k * 0.8,) * 3, 'R'
    if j == 12 and sx == -1: return (k * 0.88,) * 3, 'R'
    # mouth: small line with a slight frown corner
    if j == 14 and -1 <= sx <= 1: return (k * 0.45,) * 3, 'R'
    if j == 15 and sx == 0: return (k * 0.85,) * 3, 'R'
    # jaw and cheek shading
    if j >= 14 and (sx <= -6 or sx >= 5): return (k * 0.88,) * 3, 'R'
    return None

def paint_atlas(blink):
    global BLINK
    BLINK = blink
    img = Image.new('RGBA', (TEX, TEX), (0, 0, 0, 0))
    msk = Image.new('RGBA', (TEX, TEX), (0, 0, 0, 0))
    ip, mp = img.load(), msk.load()
    for c in cubes:
        for fn, q in face_corners(c).items():
            u0, v0, u1, v1 = c['uv'][fn]; W, H = u1 - u0, v1 - v0
            for j in range(H):
                for i in range(W):
                    s, t = (i + 0.5) / W, (j + 0.5) / H
                    top = [q[0][k] + (q[1][k] - q[0][k]) * s for k in range(3)]
                    bot = [q[3][k] + (q[2][k] - q[3][k]) * s for k in range(3)]
                    P = [top[k] + (bot[k] - top[k]) * t for k in range(3)]
                    rgb, ch = paint_texel(c, fn, s, t, W, H, i, j, P)
                    ip[u0 + i, v0 + j] = (clamp(rgb[0]), clamp(rgb[1]), clamp(rgb[2]), 255)
                    mp[u0 + i, v0 + j] = MASKC[ch]
    BLINK = False
    return img, msk
base, mask = paint_atlas(False)
blink, blink_mask = paint_atlas(True)

# ------------------------------------------------------------------ OBJ export
GROUP = {'right_arm': 'rightarm', 'body': 'body', 'left_arm': 'leftarm', 'right_leg': 'rightleg', 'head': 'head', 'left_leg': 'leftleg', 'hair': 'hair', 'hair_ssj3': 'hair_ssj3', 'hair_ssj2': 'hair_ssj2'}
NORM = {'north': (0, 0, -1), 'south': (0, 0, 1), 'east': (1, 0, 0), 'west': (-1, 0, 0), 'up': (0, 1, 0), 'down': (0, -1, 0)}
L = ['# exported from assets/blockbench/races/saiyan.bbmodel by tools/gen_blocky_saiyan.py']
vi = 1
for g in ['rightarm', 'body', 'leftarm', 'rightleg', 'head', 'hair', 'hair_ssj3', 'hair_ssj2', 'leftleg']:
    L.append('o ' + g)
    for c in cubes:
        if not c['export'] or GROUP[c['bone']] != g: continue
        for fn, q in face_corners(c).items():
            u0, v0, u1, v1 = c['uv'][fn]
            uvs = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]
            n = rot(NORM[fn], (0, 0, 0), c['r'])
            pts = [rot(p, c['o'], c['r']) for p in q]
            idx = []
            for p, uv in list(zip(pts, uvs))[::-1]:        # CCW from outside
                L.append('v %.6f %.6f %.6f' % (-p[0] / 16, p[1] / 16, -p[2] / 16))
                L.append('vt %.6f %.6f' % (uv[0] / TEX, 1 - uv[1] / TEX))
                L.append('vn %.5f %.5f %.5f' % (-n[0], n[1], -n[2]))
                idx.append('%d/%d/%d' % (vi, vi, vi)); vi += 1
            L.append('f ' + ' '.join(idx))
(RES / 'models/3d/saiyan.obj').write_text('\n'.join(L) + '\n')

import time as _time
def save_png(img, path, tries=8):
    last = None
    for i in range(tries):
        try:
            img.save(path)
            return
        except OSError as e:
            last = e
            _time.sleep(0.2 * (i + 1))
    raise last
save_png(base, RES / 'textures/entity/player/saiyan_base.png'); save_png(mask, RES / 'textures/entity/player/saiyan_mask.png')
save_png(blink, RES / 'textures/entity/player/saiyan_blink.png'); save_png(blink_mask, RES / 'textures/entity/player/saiyan_blink_mask.png')

# ------------------------------------------------------------------ Blockbench project (Bedrock format)
buf = io.BytesIO(); base.save(buf, 'PNG')
def uid(s): return str(uuid.uuid5(uuid.NAMESPACE_URL, 'dba-saiyan-' + s))
els = []
for c in cubes:
    els.append({'name': c['name'], 'type': 'cube', 'uuid': uid(c['name']), 'from': c['f'], 'to': c['t'], 'origin': c['o'],
                'rotation': c['r'], 'autouv': 0, 'color': 0, 'export': c['export'], 'box_uv': False, 'rescale': False, 'locked': False,
                'faces': {fn: {'uv': list(c['uv'][fn]), 'texture': 0} for fn in c['uv']}})
PIV = {'body': [0, 24, 0], 'head': [0, 24, 0], 'hair': [0, 24, 0], 'hair_ssj3': [0, 24, 0], 'hair_ssj2': [0, 24, 0], 'right_arm': [6.5, 22, 0], 'left_arm': [-6.5, 22, 0],
       'right_leg': [2.5, 12, 0], 'left_leg': [-2.5, 12, 0], 'tail': [0, 13.5, 3]}
def bone(name, kids=()):
    return {'name': name, 'uuid': uid('bone-' + name), 'origin': PIV[name], 'rotation': [0, 0, 0], 'export': True, 'isOpen': True,
            'children': [uid(c['name']) for c in cubes if c['bone'] == name] + list(kids)}
outliner = [bone('body', [bone('head', [bone('hair'), dict(bone('hair_ssj3'), visibility=False), dict(bone('hair_ssj2'), visibility=False)]), bone('right_arm'), bone('left_arm'), bone('right_leg'), bone('left_leg'), bone('tail')])]
bb = {'meta': {'format_version': '4.10', 'model_format': 'bedrock', 'box_uv': False}, 'name': 'saiyan',
      'dba_exporter': 'tools/gen_blocky_saiyan.py',
      'geometry_name': 'dba.saiyan', 'visible_box': [3, 3.5, 1.25], 'resolution': {'width': TEX, 'height': TEX},
      'elements': els, 'outliner': outliner,
      'textures': [{'name': 'saiyan_base.png', 'id': '0', 'width': TEX, 'height': TEX, 'uv_width': TEX, 'uv_height': TEX,
                    'source': 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()}]}
(ROOT / 'assets/blockbench/races/saiyan.bbmodel').write_text(json.dumps(bb, indent=1))
print('cubes', len(cubes), 'exported', sum(c['export'] for c in cubes), 'atlas rows used to', pk.y + pk.row)
