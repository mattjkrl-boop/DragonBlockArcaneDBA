"""Human (Yamcha-inspired, per concepts/human concept sheet): cube model + atlas + mask.

Orange Turtle-style gi with a white circle emblem, blue sash and wristbands, and
long black hair: center-parted fringe, shoulder-length sides, and a long back
falling to mid-back in downward spikes. X scar on the left cheek. No tail.
Humans use the base hair group only (Kaioken needs no hair swap).
"""
import math, zlib, sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from dba_model_lib import RaceModel, clamp, mul, h01, eye_tint, skin_lid

SKIN_V = 214; HAIR_V = 210
ORANGE = (238, 116, 30); ORANGE_D = (196, 82, 18); BLUE = (34, 64, 186); BLUE_D = (22, 40, 132)
SOLE = (40, 40, 52)

m = RaceModel('human', None)

# Legs: baggy orange gi pants, blue boots (same pattern as the Saiyan build)
for side, bone in ((1, 'right_leg'), (-1, 'left_leg')):
    def X(a, b): return (a, b) if side > 0 else (-b, -a)
    x = X(0, 5)
    m.cube('pants_' + bone, bone, 'pants', [x[0], 4.2, -2.6], [x[1], 12, 2.6])
    xb = X(0.0, 5.3)
    m.cube('pants_bag_' + bone, bone, 'pants', [xb[0], 5.4, -2.95], [xb[1], 9.6, 2.95])
    xc = X(0.15, 4.85)
    m.cube('pants_cuff_' + bone, bone, 'pants_cuff', [xc[0], 4.0, -2.8], [xc[1], 5.4, 2.7])
    xo = X(0.3, 4.7)
    m.cube('boot_' + bone, bone, 'boot', [xo[0], 0, -2.7], [xo[1], 4.4, 2.4])
    m.cube('boot_toe_' + bone, bone, 'boot', [xo[0], 0, -3.8], [xo[1], 2.2, -2.7])
    xd = X(0.2, 4.8)
    m.cube('boot_band_' + bone, bone, 'band', [xd[0], 3.5, -2.85], [xd[1], 4.5, 2.55])
# blue sash ends hang at the front of each thigh
m.cube('sash_end_r', 'right_leg', 'belt', [0.5, 6.8, -3.2], [1.7, 11.9, -2.9], [1.1, 11.9, -3.05], (0, 0, 5))
m.cube('sash_end_l', 'left_leg', 'belt', [-2.8, 7.4, -3.2], [-1.6, 11.9, -2.9], [-2.2, 11.9, -3.05], (0, 0, -7))

# Torso: orange gi, blue sash
m.cube('torso', 'body', 'gi', [-5, 12, -3], [5, 24, 3])
m.cube('chest', 'body', 'gi', [-5.5, 16.5, -3.35], [5.5, 24, 3.15])
m.cube('belt', 'body', 'belt', [-5.35, 12.4, -3.4], [5.35, 15.0, 3.3])
m.cube('belt_knot', 'body', 'belt_knot', [-0.4, 12.5, -4.1], [2.2, 15.2, -3.3])

# Arms: short gi sleeve, long skin arm, blue wristband
m.mirror('arm_skin', ('right_arm', 'left_arm'), 'skin_arm', [5.5, 13.6, -2.0], [9.5, 21.5, 2.0])
m.mirror('bicep', ('right_arm', 'left_arm'), 'skin_arm', [5.5, 15.6, -2.25], [9.75, 20.5, 2.2])
m.mirror('sleeve', ('right_arm', 'left_arm'), 'gi', [5.3, 21.2, -2.5], [9.8, 24.3, 2.5])
m.mirror('wristband', ('right_arm', 'left_arm'), 'wrist', [5.3, 11.6, -2.3], [9.7, 13.8, 2.3])
m.mirror('hand', ('right_arm', 'left_arm'), 'hand', [5.6, 9.0, -1.9], [9.4, 11.7, 1.9])

# Head
m.cube('head', 'head', 'head', [-4, 24, -4], [4, 32, 4])
m.mirror('ear', ('head', 'head'), 'skin', [4.0, 27.0, -0.6], [4.7, 29.2, 0.9])

# Hair: long Yamcha fall. Top cap, center-part fringe, long sides, long back with downward tips.
hb = 'hair'
m.cube('hair_top', hb, 'hair', [-4.45, 30.4, -4.5], [4.45, 33.1, 4.55])
m.cube('hair_back', hb, 'hair', [-4.5, 24.6, 0.6], [4.5, 32.4, 4.65])
# long back fall below the head, hugging the back
m.cube('hair_fall', hb, 'hair', [-4.35, 15.5, 2.9], [4.35, 25.0, 4.5])
m.spike('fall_tip', [0, 17.5, 3.7], 6.5, 5.6, (172, 0, 0), False, bone=hb)
m.spike('fall_tip_s', [2.9, 18.0, 3.6], 5.5, 4.2, (170, 0, -3), True, bone=hb)
m.spike('fall_tip_o', [4.0, 19.5, 3.4], 4.5, 3.4, (168, 0, -4), True, bone=hb)
# long sides down to the shoulders, in front of the ears
m.mirror('hair_side', (hb, hb), 'hair', [3.95, 20.5, -3.3], [4.5, 32.2, 1.0])
m.spike('side_tip', [4.2, 22.5, -1.2], 4.0, 2.6, (178, 0, -4), True, bone=hb)
# center-part fringe: two angled slabs with a notch at the middle
m.cube('fringe_r', hb, 'hair', [0.55, 30.3, -4.6], [4.45, 33.1, -3.85], [0.55, 33.1, -4.2], (0, 0, 7))
m.cube('fringe_l', hb, 'hair', [-4.45, 30.3, -4.6], [-0.55, 33.1, -3.85], [-0.55, 33.1, -4.2], (0, 0, -7))

# ------------------------------------------------------------------ painting
def paint_face(i, j, W, H, closed=False):
    """16x16 Yamcha face: confident brows, two-row eyes, X scar on his left cheek."""
    sx = i - W // 2
    k = SKIN_V
    # X scar: character's left cheek (sx > 0 side), crossing below the eye
    if (sx, j) in ((4, 12), (6, 12), (5, 13), (4, 14), (6, 14)):
        return (k * 0.52,) * 3, 'R'
    for side in (-1, 1):
        a = sx if side > 0 else -sx - 1
        if 1 <= a <= 6:
            by = 7 - (1 if a >= 4 else 0)
            if j == by or (j == by + 1 and a <= 2): return (110, 110, 110), 'G'
            if j == 9 and 2 <= a <= 6: return (24, 20, 20), None
            if j in (10, 11):
                # Two-row eye. The top row becomes the lid; the lower row is the shut line.
                if closed:
                    if j == 10: return skin_lid(k)
                    return eye_tint(140 if a in (2, 6) else 210)
                if a == 6 or (a == 2 and j == 10): return (24, 20, 20), None
                if a == 2: return (k * 0.75,) * 3, 'R'
                if a == 3: return eye_tint(80)
                if a == 4: return eye_tint(235)
                return (246, 246, 246), None
            if j == 12 and 3 <= a <= 5: return (k * 0.86,) * 3, 'R'
    if j == 13 and sx in (-1, 0): return (k * 0.8,) * 3, 'R'
    if j == 12 and sx == -1: return (k * 0.88,) * 3, 'R'
    if j == 14 and -1 <= sx <= 1: return (k * 0.45,) * 3, 'R'
    if j >= 14 and (sx <= -6 or sx >= 5): return (k * 0.88,) * 3, 'R'
    return None

def paint(model, c, fn, s, t, W, H, i, j, P):
    mt = c['mat']; n = h01(zlib.crc32(c['name'].encode()) % 997, zlib.crc32(fn.encode()) % 97, i, j)
    edge = i == 0 or j == 0 or i == W - 1 or j == H - 1
    front = fn == 'north'
    if mt in ('gi', 'pants', 'pants_cuff'):
        col = ORANGE
        k = 0.94 + 0.10 * n
        if mt == 'gi' and front and c['name'] in ('torso', 'chest'):
            ax = P[0]; y = P[1]
            # white circle emblem on his left chest
            if c['name'] == 'chest':
                d2 = (ax + 2.7) ** 2 + (y - 19.2) ** 2
                if d2 < 1.35 ** 2: return (246, 246, 246), None
                if d2 < 1.65 ** 2: return mul(ORANGE_D, 0.75), None
            axa = abs(ax)
            if y > 23.2 and axa < 1.7: return (SKIN_V * 0.92,) * 3, 'R'
            if y > 24 - (2.6 - axa) * 1.9 and axa < 2.6: return (SKIN_V * (0.95 - 0.05 * (axa > 1.6)),) * 3, 'R'
            if y > 24 - (4.6 - axa) * 1.75 and axa < 4.6:
                if y < 24 - (4.6 - axa) * 1.75 + 0.6: return mul(ORANGE_D, 0.8), None
                return mul(ORANGE_D, 0.9 + 0.06 * n), None     # deep open gi shadow (no undershirt)
            if y > 24 - (4.6 - axa) * 1.75 - 0.6 and axa < 4.8: return mul(ORANGE_D, 0.92), None
            if 15 < y < 21 and abs(((P[0] * 0.8 + y) % 3.2) - 1.6) < 0.35: k *= 0.86
        if mt == 'pants':
            wv = math.sin(P[0] * 1.3 + P[2] * 0.9) * 0.5
            if abs(((P[1] + wv) % 2.4) - 1.2) < 0.3: k *= 0.84
            if abs(P[0]) < 0.6 and front: k *= 0.85
        if mt == 'pants_cuff':
            k *= 0.9 if abs(((P[0] + P[2]) % 1.6) - 0.8) < 0.3 else 1.0
        if t > 0.92 and fn not in ('up', 'down'): k *= 0.82
        if edge: k *= 0.86
        if fn == 'down': k *= 0.7
        return mul(col, k), None
    if mt in ('belt', 'belt_knot', 'wrist', 'band'):
        col = BLUE if mt != 'band' else (206, 158, 86)
        k = 0.92 + 0.12 * n
        if mt == 'wrist' and (j == 1 or j == H - 2): k *= 0.82
        if mt == 'belt' and abs(((P[1] * 2 + P[0] * 0.3) % 2.0) - 1.0) < 0.25: k *= 0.85
        if mt == 'belt_knot' and abs(s - 0.5) < 0.12: k *= 0.75
        if edge: k *= 0.82
        if fn == 'down': k *= 0.6
        return mul(col, k), None
    if mt == 'boot':
        if P[1] < 0.6: return mul(SOLE, 0.9 + 0.1 * n), None
        k = 0.92 + 0.12 * n
        if edge: k *= 0.82
        if fn == 'down': k *= 0.6
        return mul(BLUE, k), None
    if mt == 'hair':
        v = HAIR_V * (0.86 + 0.14 * (1 - t)) * (0.9 + 0.12 * n)
        if i % 3 == 0 and fn not in ('up', 'down'): v *= 0.88
        if edge: v *= 0.8
        if fn == 'down': v *= 0.7
        return (v, v, v), 'G'
    if mt in ('skin', 'skin_arm', 'hand', 'head'):
        v = SKIN_V * (0.96 + 0.06 * n)
        if mt == 'skin_arm':
            if fn in ('north', 'south') and abs(t - 0.75) < 0.06 and c['name'].startswith('arm'): v *= 0.9
        if mt == 'hand':
            if fn == 'north' and 0.3 < t < 0.42 and i % 2 == 0: v *= 0.8
            if fn == 'down' or t > 0.85: v *= 0.86
        if mt == 'head' and fn == 'north':
            r = paint_face(i, j, W, H, model.blink)
            if r: return r
        if mt == 'head' and fn == 'down': v *= 0.75
        if edge and mt != 'head': v *= 0.88
        return (v, v, v), 'R'
    return (200, 200, 200), None

m.paint_texel = paint
m.finish('gen_race_human.py')
