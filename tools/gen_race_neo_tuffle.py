"""Neo-Tuffle (Baby Vegeta final form, per concepts/neo-tuffle concept sheet).

Tall silver hair in sharp upward spikes with an M hairline and a spiked back
fall, red eyes with black tear lines and a heavy maroon brow ridge. Black
bodysuit with maroon chest plates, big gold shoulder guards, gold belt with
buckle, gold wrist and ankle guards, bare pale arms. Muscular build.
"""
import math, zlib, sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from dba_model_lib import RaceModel, clamp, mul, h01

SKIN_V = 208; HAIR_V = 218
BLACK = (26, 26, 32); BLACK_D = (18, 18, 24); MAROON = (110, 30, 34); MAROON_D = (84, 22, 28)
GOLD = (216, 164, 44); GOLD_D = (172, 126, 28); SOLE = (16, 16, 20)

m = RaceModel('neo_tuffle', None)

# Legs: black suit pants, gold ankle guards, black boots
for side, bone in ((1, 'right_leg'), (-1, 'left_leg')):
    def X(a, b): return (a, b) if side > 0 else (-b, -a)
    x = X(0, 4.9)
    m.cube('pants_' + bone, bone, 'suit', [x[0], 5.6, -2.55], [x[1], 12, 2.55])
    xb = X(0.05, 5.15)
    m.cube('pants_bag_' + bone, bone, 'suit', [xb[0], 7.0, -2.8], [xb[1], 10.4, 2.8])
    xa = X(0.1, 5.0)
    m.cube('ankle_guard_' + bone, bone, 'gold', [xa[0], 3.3, -2.7], [xa[1], 6.0, 2.65])
    xo = X(0.3, 4.6)
    m.cube('boot_' + bone, bone, 'boot', [xo[0], 0, -2.6], [xo[1], 3.5, 2.4])
    m.cube('boot_toe_' + bone, bone, 'boot', [xo[0], 0, -3.7], [xo[1], 2.0, -2.6])

# Torso: black suit, maroon chest plates, gold belt with buckle
m.cube('torso', 'body', 'suit', [-5.1, 12, -3.0], [5.1, 24, 3.0])
m.cube('chest', 'body', 'suit', [-5.6, 16.5, -3.3], [5.6, 24, 3.1])
m.mirror('pec', ('body', 'body'), 'chestplate', [0.45, 18.6, -3.75], [5.35, 23.9, -3.15])
m.cube('sternum', 'body', 'chestplate', [-1.15, 15.4, -3.7], [1.15, 19.4, -3.15], [0, 19.4, -3.4], (0, 0, 45))
m.cube('belt', 'body', 'gold', [-5.3, 12.3, -3.3], [5.3, 14.6, 3.25])
m.cube('buckle', 'body', 'buckle', [-1.5, 12.15, -3.85], [1.5, 14.75, -3.3])

# Arms: bare pale skin, big gold shoulder guards, chunky gold wrist guards
m.mirror('arm', ('right_arm', 'left_arm'), 'skin_arm', [5.4, 13.9, -2.1], [9.6, 24.0, 2.1])
m.mirror('bicep', ('right_arm', 'left_arm'), 'skin_arm', [5.3, 16.0, -2.35], [9.85, 20.5, 2.3])
m.mirror('shoulder', ('right_arm', 'left_arm'), 'gold', [4.85, 22.2, -3.05], [10.8, 25.5, 3.05], [5.7, 22.6, 0], (0, 0, 10))
m.mirror('wristguard', ('right_arm', 'left_arm'), 'gold', [5.15, 11.4, -2.45], [9.8, 14.8, 2.45])
m.mirror('hand', ('right_arm', 'left_arm'), 'hand', [5.5, 8.8, -1.9], [9.4, 11.45, 1.9])

# Head
m.cube('head', 'head', 'head', [-4, 24, -4], [4, 32, 4])
m.mirror('ear', ('head', 'head'), 'skin', [4.0, 26.8, -0.6], [4.65, 29.0, 0.9])

# Hair: tall silver spikes swept up and back, M hairline, spiked fall down the back
hb = 'hair'
m.cube('hair_top', hb, 'hair', [-4.45, 30.5, -4.5], [4.45, 33.1, 4.55])
m.cube('hair_back', hb, 'hair', [-4.5, 25.2, 0.5], [4.5, 32.3, 4.65])
m.mirror('hair_lobe', (hb, hb), 'hair', [1.15, 29.9, -4.6], [4.5, 33.0, -3.8])
m.cube('hair_dip', hb, 'hair', [-1.15, 31.6, -4.55], [1.15, 33.3, -3.85])
m.mirror('hair_side', (hb, hb), 'hair', [3.95, 27.8, -3.5], [4.5, 32.2, 1.0])
SPIKES = [
    ('top', [0.5, 32.5, 0.6], 13.0, 5.6, (34, 0, -4), False),
    ('upper', [2.5, 32.2, 0.4], 12.0, 5.0, (28, 0, -22), True),
    ('side_hi', [3.8, 31.0, 0.6], 10.5, 4.6, (20, 0, -48), True),
    ('side', [4.0, 29.0, 1.2], 8.5, 4.2, (16, 0, -70), True),
    ('back_hi', [0, 31.2, 3.4], 12.5, 5.4, (55, 0, 0), False),
    ('back_side', [2.6, 30.2, 3.4], 11.0, 4.8, (66, 0, -34), True),
]
for n, bs, L, w, r, both in SPIKES:
    m.spike(n, bs, L, w, r, both, bone=hb)
# spiked fall down the back, ending in points around mid-back
m.spike('fall_c', [0, 27.0, 4.2], 11.5, 5.4, (172, 0, 0), False, bone=hb)
m.spike('fall_s', [3.2, 27.5, 3.8], 9.5, 4.2, (170, 0, -6), True, bone=hb)
m.spike('fall_o', [4.3, 28.5, 2.6], 7.5, 3.4, (168, 0, -11), True, bone=hb)

# ------------------------------------------------------------------ painting
def paint_face(i, j, W, H):
    """Baby Vegeta face: heavy maroon brow ridge, red eyes, black tear lines."""
    sx = i - W // 2
    k = SKIN_V
    for side in (-1, 1):
        a = sx if side > 0 else -sx - 1
        if 1 <= a <= 6:
            if j in (6, 7) and a >= 2: return (96, 40, 40), None          # brow ridge (dark maroon, unmasked)
            if j == 8 and a <= 3: return (96, 40, 40), None
            if j == 9 and 2 <= a <= 5: return (22, 18, 20), None          # upper lid
            if j == 10:
                if a in (2, 5): return (22, 18, 20), None
                if a in (3, 4): return (208, 36, 36), None                # red eyes
            if j == 11 and 3 <= a <= 4: return (k * 0.85,) * 3, 'R'
            if j in (12, 13, 14) and a in (3, 5): return (24, 20, 22), None   # tear lines
    if j == 13 and sx == -1: return (k * 0.82,) * 3, 'R'
    if j == 15 and -1 <= sx <= 1: return (k * 0.5,) * 3, 'R'              # stern mouth
    if j >= 14 and (sx <= -6 or sx >= 5): return (k * 0.9,) * 3, 'R'
    return None

def paint(model, c, fn, s, t, W, H, i, j, P):
    mt = c['mat']; n = h01(zlib.crc32(c['name'].encode()) % 997, zlib.crc32(fn.encode()) % 97, i, j)
    edge = i == 0 or j == 0 or i == W - 1 or j == H - 1
    if mt == 'hair':
        v = HAIR_V * (0.87 + 0.15 * (1 - t)) * (0.91 + 0.12 * n)
        if fn in ('up', 'north', 'south'): v *= 0.94 + 0.06 * math.sin(P[0] * 2.3 + 1.0)
        if i % 3 == 0 and fn not in ('up', 'down'): v *= 0.9
        if edge: v *= 0.82
        if fn == 'down': v *= 0.7
        return (v, v, v), 'G'
    if mt == 'chestplate':
        k = 0.93 + 0.1 * n
        if edge: k *= 0.8
        if fn == 'down': k *= 0.65
        return mul(MAROON if P[1] > 17 else MAROON_D, k), None
    if mt in ('gold', 'buckle'):
        k = 0.92 + 0.14 * n
        if mt == 'buckle' and abs(s - 0.5) < 0.3 and abs(t - 0.5) < 0.3: k *= 0.8
        if j == 1 or j == H - 2: k *= 0.85
        if edge: k *= 0.85
        if fn == 'down': k *= 0.65
        return mul(GOLD if (i + j) % 5 else GOLD_D, k), None
    if mt == 'suit':
        k = 0.93 + 0.1 * n
        if mt == 'suit' and fn in ('north', 'south') and abs(((P[1]) % 3.0) - 1.5) < 0.22: k *= 0.85   # subtle suit seams
        if edge: k *= 0.86
        if fn == 'down': k *= 0.65
        return mul(BLACK if P[1] > 6 else BLACK_D, k), None
    if mt == 'boot':
        if P[1] < 0.5: return mul(SOLE, 0.9 + 0.1 * n), None
        k = 0.92 + 0.1 * n
        if edge: k *= 0.84
        if fn == 'down': k *= 0.6
        return mul(BLACK, k), None
    if mt in ('skin', 'skin_arm', 'hand', 'head'):
        v = SKIN_V * (0.96 + 0.06 * n)
        if mt == 'skin_arm' and c['name'].startswith('bicep') and fn == 'north' and abs(s - 0.5) < 0.1: v *= 0.92
        if mt == 'hand':
            if fn == 'north' and 0.3 < t < 0.42 and i % 2 == 0: v *= 0.82
            if fn == 'down' or t > 0.85: v *= 0.87
        if mt == 'head' and fn == 'north':
            r = paint_face(i, j, W, H)
            if r: return r
        if mt == 'head' and fn == 'down': v *= 0.78
        if edge and mt != 'head': v *= 0.89
        return (v, v, v), 'R'
    return (200, 200, 200), None

m.paint_texel = paint
m.finish('gen_race_neo_tuffle.py')
