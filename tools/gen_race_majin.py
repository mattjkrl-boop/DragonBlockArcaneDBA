"""Majin (Fat Buu-inspired, per concepts/majin concept sheet).

Bubblegum pink skin (tinted), very wide belly torso, single curved head
tentacle, happy face with a big open smile, steam holes on the head sides.
Black vest with gold trim worn open, purple cape tied at the neck and hanging
long down the back, black belt with a gold M buckle, baggy white pants, gold
boots and wrist cuffs.
"""
import math, zlib, sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from dba_model_lib import RaceModel, clamp, mul, h01, eye_tint, skin_lid

SKIN_V = 212
VEST = (26, 26, 32); VEST_D = (18, 18, 24); GOLD = (222, 172, 52); GOLD_D = (180, 134, 34)
CAPE = (128, 84, 205); CAPE_D = (104, 64, 172); PANTS = (240, 242, 246); PANTS_D = (206, 210, 220)
BLACK = (22, 22, 28)

m = RaceModel('majin', None)

# Legs: baggy white balloon pants, black ankle bands, gold boots
for side, bone in ((1, 'right_leg'), (-1, 'left_leg')):
    def X(a, b): return (a, b) if side > 0 else (-b, -a)
    x = X(0.1, 5.7)
    m.cube('pants_' + bone, bone, 'pants', [x[0], 5.5, -3.35], [x[1], 12, 3.35])
    xb = X(0.3, 5.5)
    m.cube('pants_low_' + bone, bone, 'pants', [xb[0], 3.4, -3.2], [xb[1], 6.2, 3.2])
    xa = X(0.35, 5.4)
    m.cube('ankle_' + bone, bone, 'vest', [xa[0], 2.5, -3.1], [xa[1], 3.6, 3.1])
    xo = X(0.3, 5.5)
    m.cube('boot_' + bone, bone, 'gold', [xo[0], 0, -3.15], [xo[1], 2.6, 3.05])
    m.cube('boot_toe_' + bone, bone, 'gold', [xo[0], 0, -4.15], [xo[1], 1.6, -3.15])

# Torso: big pink belly, open black/gold vest, purple cape, M belt
m.cube('chest', 'body', 'skin_body', [-6.3, 19.8, -4.1], [6.3, 24, 4.1])
m.cube('belly', 'body', 'skin_body', [-6.7, 12.2, -4.3], [6.7, 20.6, 4.3])
m.cube('belly_front', 'body', 'skin_body', [-5.9, 13.2, -6.0], [5.9, 20.0, -4.15])
m.mirror('vest', ('body', 'body'), 'vest', [3.3, 15.6, -4.55], [7.0, 24.0, 4.5])
m.cube('vest_back', 'body', 'vest', [-7.0, 15.6, 3.3], [7.0, 24.0, 4.55])
m.cube('cape_top', 'body', 'cape', [-6.9, 23.3, -4.6], [6.9, 24.0, 4.7])
m.mirror('cape_tie', ('body', 'body'), 'cape', [0.2, 21.2, -4.8], [2.7, 23.5, -4.2])
m.cube('cape_back', 'body', 'cape', [-5.9, 13.8, 4.6], [5.9, 23.5, 5.05])
m.cube('belt', 'body', 'vest', [-6.85, 12.0, -4.5], [6.85, 13.5, 4.45])
m.cube('buckle', 'body', 'buckle', [-2.0, 12.0, -4.92], [2.0, 13.3, -4.48])

# Arms: chubby pink arms, gold wrist cuffs
m.mirror('upper', ('right_arm', 'left_arm'), 'skin_arm', [6.3, 16.0, -2.4], [10.8, 24.2, 2.4])
m.mirror('fore', ('right_arm', 'left_arm'), 'skin_arm', [6.4, 12.8, -2.25], [10.6, 16.4, 2.25])
m.mirror('cuff', ('right_arm', 'left_arm'), 'gold', [6.35, 11.55, -2.35], [10.65, 13.0, 2.35])
m.mirror('hand', ('right_arm', 'left_arm'), 'hand', [6.55, 8.65, -1.95], [10.4, 11.6, 1.95])

# Head: pink, ear flaps, curved tentacle with a hooked tip
m.cube('head', 'head', 'head', [-4, 24, -4], [4, 32, 4])
m.mirror('ear', ('head', 'head'), 'skin', [4.0, 27.2, -0.5], [4.7, 29.0, 1.0])
m.spike('tent', [0, 31.6, 0.4], 5.6, 2.7, (-24, 0, -8), False, bone='head', mat='skin')
m.cube('tent_tip', 'head', 'skin', [0.05, 35.8, -3.0], [1.55, 37.3, -1.55], [0.8, 36.5, -2.25], (-38, 0, -14))

# ------------------------------------------------------------------ painting
def paint_face(i, j, W, H, closed=False):
    """Buu face: small dark eyes, huge open smile with tongue."""
    sx = i - W // 2
    k = SKIN_V
    for side in (-1, 1):
        a = sx if side > 0 else -sx - 1
        if 1 <= a <= 6:
            if j == 8 and 3 <= a <= 5: return (k * 0.82,) * 3, 'R'        # lid shade
            if closed and j == 9 and 3 <= a <= 5: return eye_tint(200)    # small eye shuts to one line
            if closed and j == 10 and a in (3, 5): return skin_lid(k)
            if j == 9 and 3 <= a <= 5: return eye_tint(230)                # small eyes, player tint
            if j == 10 and a in (3, 5): return eye_tint(140)
    if j == 11 and sx == -1: return (k * 0.88,) * 3, 'R'
    if j == 12 and -2 <= sx <= 2: return (110, 52, 64), None              # open smile top
    if j == 13:
        if sx in (-1, 0): return (242, 132, 152), None                    # tongue
        if -2 <= sx <= 2: return (84, 36, 48), None
    if j == 14 and sx in (-3, 3): return (110, 52, 64), None              # smile corners up
    if j >= 12 and (sx <= -6 or sx >= 5): return (k * 0.88,) * 3, 'R'     # cheek shade
    return None

def paint(model, c, fn, s, t, W, H, i, j, P):
    mt = c['mat']; n = h01(zlib.crc32(c['name'].encode()) % 997, zlib.crc32(fn.encode()) % 97, i, j)
    edge = i == 0 or j == 0 or i == W - 1 or j == H - 1
    if mt == 'vest':
        k = 0.92 + 0.11 * n
        if c['name'] in ('vest_r', 'vest_l') and fn == 'north':
            inner = (i >= W - 2) if c['name'] == 'vest_r' else (i <= 1)
            if inner or j >= H - 2: return mul(GOLD, 0.92 + 0.1 * n), None   # trim on the opening and hem
        if c['name'] == 'vest_back' and fn == 'south' and j >= H - 2:
            return mul(GOLD, 0.92 + 0.1 * n), None
        if c['name'].startswith('vest') and fn == 'up': return mul(GOLD, 0.9), None
        if edge: k *= 0.85
        if fn == 'down': k *= 0.65
        return mul(VEST if P[1] > 8 else VEST_D, k), None
    if mt == 'buckle':
        u, v = (i + 0.5) / W, (j + 0.5) / H
        dark = u < 0.2 or u > 0.8
        if v < 0.52:
            dark = dark or (0.2 <= u < 0.5 and abs((u - 0.2) / 0.3 - v / 0.52) < 0.2) \
                        or (0.5 <= u <= 0.8 and abs((0.8 - u) / 0.3 - v / 0.52) < 0.2)
        if fn == 'north' and dark: return (96, 64, 12), None              # the M
        return mul(GOLD, 0.92 + 0.12 * n), None
    if mt == 'gold':
        k = 0.92 + 0.12 * n
        if j == 1 or j == H - 2: k *= 0.85
        if edge: k *= 0.86
        if fn == 'down': k *= 0.65
        return mul(GOLD if P[1] > 1 else GOLD_D, k), None
    if mt == 'cape':
        k = 0.93 + 0.09 * n
        if edge: k *= 0.86
        if fn == 'down': k *= 0.65
        return mul(CAPE if P[1] > 12 else CAPE_D, k), None
    if mt == 'pants':
        k = 0.96 + 0.05 * n
        if h01(int(P[0] * 1.6 + 30), int(P[2] * 1.6 + 30)) > 0.86: k *= 0.9   # soft creases
        if edge: k *= 0.9
        if fn == 'down': k *= 0.72
        return mul(PANTS if P[1] > 4 else PANTS_D, k), None
    if mt in ('skin', 'skin_arm', 'skin_body', 'hand', 'head'):
        v = SKIN_V * (0.96 + 0.06 * n)
        if mt == 'head' and fn in ('east', 'west') and i % 5 == 2 and j % 5 == 2: v *= 0.78   # steam holes
        if mt == 'hand':
            if fn == 'north' and 0.3 < t < 0.42 and i % 2 == 0: v *= 0.85
            if fn == 'down' or t > 0.85: v *= 0.88
        if mt == 'head' and fn == 'north':
            r = paint_face(i, j, W, H, model.blink)
            if r: return r
        if mt == 'head' and fn == 'down': v *= 0.8
        if edge and mt != 'head': v *= 0.9
        return (v, v, v), 'R'
    return (200, 200, 200), None

m.paint_texel = paint
m.finish('gen_race_majin.py')
