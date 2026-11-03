"""Tuffle (Baby-inspired, per concepts/tuffle concept sheet): cube model + atlas + mask.

Pale skin, silver pompadour hair with a high M hairline (smooth swept slabs with
painted strand ridges), narrow red eyes and a sly smirk. White chest armor with
gold trim and gold shoulder and wrist guards over a navy undersuit, white baggy
pants, navy boots. Compact slim build.
"""
import math, zlib, sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from dba_model_lib import RaceModel, clamp, mul, h01, eye_tint, skin_lid

SKIN_V = 206; HAIR_V = 216
NAVY = (38, 40, 72); NAVY_D = (28, 30, 56); WHITE = (238, 240, 246); WHITE_D = (206, 210, 222)
GOLD = (216, 164, 44); GOLD_D = (172, 126, 28); SOLE = (24, 24, 34)

m = RaceModel('tuffle', None)

# Legs: navy undersuit base, white baggy pants over it, navy boots
for side, bone in ((1, 'right_leg'), (-1, 'left_leg')):
    def X(a, b): return (a, b) if side > 0 else (-b, -a)
    x = X(0.1, 4.6)
    m.cube('suit_' + bone, bone, 'suit', [x[0], 3.6, -2.4], [x[1], 12, 2.4])
    xb = X(0.0, 5.05)
    m.cube('pants_' + bone, bone, 'pants', [xb[0], 4.6, -2.8], [xb[1], 12.0, 2.8])
    xc = X(0.15, 4.6)
    m.cube('pants_cuff_' + bone, bone, 'pants', [xc[0], 3.4, -2.65], [xc[1], 4.8, 2.55])
    xo = X(0.3, 4.4)
    m.cube('boot_' + bone, bone, 'boot', [xo[0], 0, -2.5], [xo[1], 4.4, 2.3])
    m.cube('boot_toe_' + bone, bone, 'boot', [xo[0], 0, -3.55], [xo[1], 1.9, -2.5])
# Navy wedge between the legs. It hangs on the legs so the torso bounds stay
# at the waist; a body cube down to y=8.4 was scaling the gi short of the pants.
m.cube('hip_wedge_r', 'right_leg', 'suit', [0.0, 8.4, -3.05], [1.8, 12.0, -2.45])
m.cube('hip_wedge_l', 'left_leg', 'suit', [-1.8, 8.4, -3.05], [0.0, 12.0, -2.45])
m.cube('hip_wedge_tuck', 'body', 'suit', [-1.8, 12.0, -3.05], [1.8, 12.4, -2.45])

# Torso: navy undersuit, white chest armor with gold trim
m.cube('torso', 'body', 'suit', [-4.6, 12, -2.85], [4.6, 24, 2.85])
m.cube('armor', 'body', 'armor', [-5.25, 15.2, -3.4], [5.25, 24.0, 3.25])
m.cube('armor_waist', 'body', 'armor', [-5.0, 13.6, -3.25], [5.0, 15.6, 3.1])

# Arms: navy sleeves, gold shoulder guards, gold wrist guards, pale hands
m.mirror('sleeve', ('right_arm', 'left_arm'), 'suit', [5.15, 13.8, -2.1], [9.5, 24.1, 2.1])
m.mirror('shoulder', ('right_arm', 'left_arm'), 'gold', [4.95, 22.6, -2.85], [10.25, 25.1, 2.85], [5.6, 22.8, 0], (0, 0, 7))
m.mirror('bicep', ('right_arm', 'left_arm'), 'suit', [5.15, 15.8, -2.3], [9.65, 20.0, 2.25])
m.mirror('wristguard', ('right_arm', 'left_arm'), 'gold', [5.05, 11.8, -2.35], [9.7, 14.6, 2.35])
m.mirror('hand', ('right_arm', 'left_arm'), 'hand', [5.35, 9.0, -1.85], [9.25, 11.85, 1.85])

# Head
m.cube('head', 'head', 'head', [-4, 24, -4], [4, 32, 4])
m.mirror('ear', ('head', 'head'), 'skin', [4.0, 26.8, -0.6], [4.65, 29.0, 0.9])

# Hair: silver pompadour. Smooth swept masses, M hairline, strand ridges in the paint.
hb = 'hair'
m.cube('hair_top', hb, 'hair', [-4.5, 31.0, -4.55], [4.5, 33.7, 4.6])
m.cube('hair_sweep', hb, 'hair', [-4.55, 29.6, -3.2], [4.55, 33.3, 4.85], [0, 31.0, -3.2], (13, 0, 0))
m.cube('hair_back', hb, 'hair', [-4.5, 25.6, 0.6], [4.5, 32.2, 4.65])
m.mirror('hair_side', (hb, hb), 'hair', [3.95, 27.9, -3.5], [4.5, 32.4, 1.0])
m.mirror('hair_lobe', (hb, hb), 'hair', [1.3, 29.7, -4.65], [4.5, 33.2, -3.8])
m.cube('hair_dip', hb, 'hair', [-1.3, 31.8, -4.6], [1.3, 33.6, -3.85])
m.mirror('hair_wisp', (hb, hb), 'hair', [3.9, 26.6, -3.1], [4.45, 28.1, -1.6])

# ------------------------------------------------------------------ painting
def paint_face(i, j, W, H, closed=False):
    """Sly Tuffle face: furrowed brow lines, narrow eye slits, smirk."""
    sx = i - W // 2
    k = SKIN_V
    for side in (-1, 1):
        a = sx if side > 0 else -sx - 1
        if 1 <= a <= 6:
            if j == 7 and a >= 3: return (k * 0.7,) * 3, 'R'              # furrow line
            if j == 8 and a <= 3: return (k * 0.7,) * 3, 'R'
            if j == 10 and 2 <= a <= 5: return (26, 20, 22), None         # heavy upper lid
            if j == 11:
                # The slit is already one row. Closed, it is a darker eyelid line of the same width.
                if closed:
                    if a in (3, 4): return eye_tint(200)
                    if a in (2, 5): return skin_lid(k)
                if a in (2, 5): return (26, 20, 22), None
                if a in (3, 4): return eye_tint(235)
            if j == 12 and 3 <= a <= 4: return (k * 0.82,) * 3, 'R'
    if j == 13 and sx == -1: return (k * 0.85,) * 3, 'R'                  # nose
    if j == 14 and 0 <= sx <= 2: return (k * 0.5,) * 3, 'R'               # smirk
    if j == 13 and sx == 3: return (k * 0.5,) * 3, 'R'                    # smirk upturn
    if j >= 14 and (sx <= -6 or sx >= 5): return (k * 0.9,) * 3, 'R'
    return None

def paint(model, c, fn, s, t, W, H, i, j, P):
    mt = c['mat']; n = h01(zlib.crc32(c['name'].encode()) % 997, zlib.crc32(fn.encode()) % 97, i, j)
    edge = i == 0 or j == 0 or i == W - 1 or j == H - 1
    if mt == 'hair':
        v = HAIR_V * (0.88 + 0.12 * (1 - t)) * (0.92 + 0.1 * n)
        # pompadour strand ridges running front to back
        if fn in ('up', 'north', 'south'): v *= 0.93 + 0.07 * math.sin(P[0] * 2.3 + 1.0)
        if fn in ('east', 'west'): v *= 0.93 + 0.07 * math.sin(P[2] * 2.3)
        if edge: v *= 0.86
        if fn == 'down': v *= 0.72
        return (v, v, v), 'G'
    if mt == 'armor':
        if edge: return mul(GOLD, 0.9 + 0.12 * n), None                   # gold trim border
        k = 0.96 + 0.06 * n
        if fn == 'north' and abs(P[0]) < 0.35: k *= 0.88                  # center seam
        if fn == 'down': k *= 0.7
        return mul(WHITE if P[1] > 15 else WHITE_D, k), None
    if mt == 'gold':
        k = 0.92 + 0.14 * n
        if j == 1 or j == H - 2: k *= 0.85
        if edge: k *= 0.85
        if fn == 'down': k *= 0.65
        return mul(GOLD if (i + j) % 5 else GOLD_D, k), None
    if mt == 'suit':
        k = 0.93 + 0.1 * n
        if edge: k *= 0.85
        if fn == 'down': k *= 0.65
        return mul(NAVY if P[1] > 8 else NAVY_D, k), None
    if mt == 'pants':
        k = 0.95 + 0.08 * n
        wv = math.sin(P[0] * 1.3 + P[2] * 0.9) * 0.5
        if abs(((P[1] + wv) % 2.4) - 1.2) < 0.3: k *= 0.9
        if edge: k *= 0.88
        if fn == 'down': k *= 0.72
        return mul(WHITE, k), None
    if mt == 'boot':
        if P[1] < 0.5: return mul(SOLE, 0.9 + 0.1 * n), None
        k = 0.92 + 0.1 * n
        if edge: k *= 0.84
        if fn == 'down': k *= 0.6
        return mul(NAVY, k), None
    if mt in ('skin', 'hand', 'head'):
        v = SKIN_V * (0.96 + 0.06 * n)
        if mt == 'hand':
            if fn == 'north' and 0.3 < t < 0.42 and i % 2 == 0: v *= 0.82
            if fn == 'down' or t > 0.85: v *= 0.87
        if mt == 'head' and fn == 'north':
            r = paint_face(i, j, W, H, model.blink)
            if r: return r
        if mt == 'head' and fn == 'down': v *= 0.78
        if edge and mt != 'head': v *= 0.89
        return (v, v, v), 'R'
    return (200, 200, 200), None

m.paint_texel = paint
m.finish('gen_race_tuffle.py')
