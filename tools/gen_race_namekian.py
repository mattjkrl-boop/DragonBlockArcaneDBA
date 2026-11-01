"""Namekian (Piccolo-inspired, per concepts/namekian concept sheet).

Green skin, pointed ears, stepped antennae, white turban with a blue gem and a
back tail cloth, stern face. Pink segmented muscle patches on the chest and
arms (a distinct tissue color, painted unmasked), dark purple gi open at the
chest, white weighted shoulder cape, blue sash with hanging ends, baggy purple
pants, brown shoes.
"""
import math, zlib, sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from dba_model_lib import RaceModel, clamp, mul, h01

SKIN_V = 202
GI = (88, 62, 168); GI_D = (68, 46, 136); SASH = (52, 96, 205); SASH_D = (40, 76, 168)
WHITE = (238, 238, 240); PATCH = (240, 170, 180); PATCH_D = (210, 128, 142)
BROWN = (128, 82, 50); BROWN_D = (102, 64, 38); GEM = (52, 120, 235); GOLD = (216, 164, 44)

m = RaceModel('namekian', None)

# Legs: baggy purple gi pants, brown shoes
for side, bone in ((1, 'right_leg'), (-1, 'left_leg')):
    def X(a, b): return (a, b) if side > 0 else (-b, -a)
    x = X(0, 4.9)
    m.cube('pants_' + bone, bone, 'gi', [x[0], 3.9, -2.55], [x[1], 12, 2.55])
    xb = X(0.05, 5.15)
    m.cube('pants_bag_' + bone, bone, 'gi', [xb[0], 6.8, -2.8], [xb[1], 10.6, 2.8])
    xo = X(0.3, 4.6)
    m.cube('shoe_' + bone, bone, 'shoe', [xo[0], 0, -2.55], [xo[1], 4.0, 2.35])
    m.cube('shoe_toe_' + bone, bone, 'shoe', [xo[0], 0, -3.6], [xo[1], 2.2, -2.55])

# Torso: purple gi, pink segmented chest patch, white cape, blue sash
m.cube('torso', 'body', 'gi', [-5.0, 12, -3.0], [5.0, 24, 3.0])
m.cube('chest', 'body', 'gi', [-5.5, 16.5, -3.3], [5.5, 24, 3.1])
m.cube('chestpatch', 'body', 'patch', [-3.3, 16.9, -3.62], [3.3, 23.7, -3.05])
m.cube('cape_top', 'body', 'cape', [-5.9, 22.5, -3.5], [5.9, 24.7, 3.6])
m.cube('cape_back', 'body', 'cape', [-5.7, 12.7, 3.3], [5.7, 23.2, 3.85])
for wx in (-5.0, 0.0, 5.0):
    m.cube('cape_tip_%d' % int(wx + 5), 'body', 'cape', [wx - 0.55, 11.5, 3.35], [wx + 0.55, 12.8, 3.8])
    m.cube('cape_wt_%d' % int(wx + 5), 'body', 'gold', [wx - 0.55, 12.75, 3.35], [wx + 0.55, 13.35, 3.8])
m.cube('sash', 'body', 'sash', [-5.2, 12.2, -3.25], [5.2, 14.3, 3.2])
m.mirror('sash_end', ('body', 'body'), 'sash', [0.5, 6.8, -3.35], [2.2, 12.4, -2.9])

# Arms: pink segmented upper arms, green forearms with pink bands, brown wristbands
m.mirror('upper', ('right_arm', 'left_arm'), 'patch', [5.35, 16.6, -2.3], [9.7, 24.1, 2.3])
m.mirror('shoulder', ('right_arm', 'left_arm'), 'skin_arm', [5.3, 21.8, -2.35], [9.75, 24.3, 2.35])
m.mirror('fore', ('right_arm', 'left_arm'), 'skin_arm', [5.4, 13.9, -2.1], [9.55, 16.9, 2.1])
m.mirror('foreband', ('right_arm', 'left_arm'), 'patch', [5.3, 14.6, -2.25], [9.65, 16.2, 2.25])
m.mirror('wrist', ('right_arm', 'left_arm'), 'shoe', [5.35, 12.5, -2.2], [9.6, 13.95, 2.2])
m.mirror('hand', ('right_arm', 'left_arm'), 'hand', [5.5, 9.3, -1.9], [9.4, 12.55, 1.9])

# Head: green face, pointed ears, antennae, white turban with gem
m.cube('head', 'head', 'head', [-4, 24, -4], [4, 32, 4])
for s in (1, -1):
    m.cube('ear%d_b' % s, 'head', 'skin', [min(s * 4.0, s * 5.4), 27.9, -0.95], [max(s * 4.0, s * 5.4), 29.7, 0.95])
    m.cube('ear%d_m' % s, 'head', 'skin', [min(s * 5.3, s * 6.5), 28.2, -0.7], [max(s * 5.3, s * 6.5), 29.4, 0.7])
    m.cube('ear%d_t' % s, 'head', 'skin', [min(s * 6.4, s * 7.3), 28.45, -0.45], [max(s * 6.4, s * 7.3), 29.15, 0.45])
    for k, (cx, cy, sz) in enumerate(((1.5, 33.9, 0.78), (2.05, 35.0, 0.66), (2.7, 35.9, 0.54), (3.2, 36.6, 0.4))):
        m.cube('ant%d_%d' % (s, k), 'head', 'skin',
               [s * cx - sz / 2, cy - sz / 2, -3.85 - sz / 2], [s * cx + sz / 2, cy + sz / 2, -3.85 + sz / 2])
m.cube('turban_band', 'head', 'cape', [-4.38, 30.2, -4.38], [4.38, 32.3, 4.38])
m.cube('turban_top', 'head', 'cape', [-4.05, 32.2, -4.05], [4.05, 33.4, 4.05])
m.cube('turban_tail', 'head', 'cape', [-2.0, 25.8, 4.25], [2.0, 30.4, 4.72])
m.cube('gem', 'head', 'gem', [-0.78, 30.65, -4.66], [0.78, 32.05, -4.36])

# ------------------------------------------------------------------ painting
def paint_face(i, j, W, H):
    """Piccolo face: heavy slanted brows, narrow eyes, frown."""
    sx = i - W // 2
    k = SKIN_V
    for side in (-1, 1):
        a = sx if side > 0 else -sx - 1
        if 1 <= a <= 6:
            if j == 6 and a >= 4: return (26, 34, 22), None
            if j == 7 and a >= 2: return (26, 34, 22), None               # slanted heavy brow
            if j == 9 and 2 <= a <= 6: return (22, 26, 20), None          # upper lid
            if j == 10:
                if a in (2, 6): return (22, 26, 20), None
                if a == 4: return (16, 16, 16), None                      # pupil
                return (244, 244, 240), None                              # white narrow eye
            if j == 11 and 3 <= a <= 5: return (k * 0.85,) * 3, 'R'
    if j == 13 and sx in (-2, 1): return (k * 0.62,) * 3, 'R'             # nostrils
    if j == 14 and -1 <= sx <= 1: return (k * 0.45,) * 3, 'R'             # frown
    if j == 15 and sx in (-2, 2): return (k * 0.5,) * 3, 'R'
    if j >= 14 and (sx <= -6 or sx >= 5): return (k * 0.9,) * 3, 'R'
    return None

def paint(model, c, fn, s, t, W, H, i, j, P):
    mt = c['mat']; n = h01(zlib.crc32(c['name'].encode()) % 997, zlib.crc32(fn.encode()) % 97, i, j)
    edge = i == 0 or j == 0 or i == W - 1 or j == H - 1
    if mt == 'patch':
        k = 0.94 + 0.09 * n
        col = PATCH if (int(P[1] * 1.0) % 3) else PATCH
        if (P[1] % 2.7) < 0.5: col = PATCH_D                              # segment lines
        if c['name'] == 'chestpatch' and fn == 'north':
            if abs(P[0]) < 0.4: col = BROWN                               # center strap line
            if P[1] > 22.6 and abs(P[0]) > (23.7 - P[1]) * 2.2: col = GI  # V opening corners
        if edge: k *= 0.88
        if fn == 'down': k *= 0.7
        return mul(col, k), None
    if mt == 'gi':
        k = 0.93 + 0.1 * n
        if edge: k *= 0.86
        if fn == 'down': k *= 0.65
        return mul(GI if P[1] > 6 else GI_D, k), None
    if mt == 'cape':
        k = 0.96 + 0.05 * n
        if c['name'].startswith('turban') and fn == 'up': k *= 0.94
        if edge: k *= 0.9
        if fn == 'down': k *= 0.72
        return mul(WHITE, k), None
    if mt == 'gem':
        if 0.3 < s < 0.7 and 0.3 < t < 0.7: return (140, 190, 255), None
        return mul(GEM, 0.9 + 0.14 * n), None
    if mt in ('sash',):
        k = 0.93 + 0.1 * n
        if edge: k *= 0.86
        return mul(SASH if P[1] > 10 else SASH_D, k), None
    if mt == 'gold':
        return mul(GOLD, 0.9 + 0.14 * n), None
    if mt == 'shoe':
        if P[1] < 0.5: return mul(BROWN_D, 0.85 + 0.1 * n), None
        k = 0.92 + 0.1 * n
        if edge: k *= 0.85
        if fn == 'down': k *= 0.62
        return mul(BROWN, k), None
    if mt in ('skin', 'skin_arm', 'hand', 'head'):
        v = SKIN_V * (0.96 + 0.06 * n)
        if mt == 'skin_arm' and fn in ('east', 'west') and 0.35 < t < 0.5: v *= 0.9
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
m.finish('gen_race_namekian.py')
