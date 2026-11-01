"""Android (Android 17-inspired, per concepts/android concept sheet).

Straight black center-parted hair falling to the shoulders, calm blue eyes and
a slight smile. Orange bandana scarf with a hanging front point, black tee over
white long sleeves, brown belt with a hip pouch, blue jeans with back pockets,
green camo socks, brown shoes. Lean build, no tail.
"""
import math, zlib, sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from dba_model_lib import RaceModel, clamp, mul, h01

SKIN_V = 216; HAIR_V = 208
TEE = (34, 34, 40); TEE_D = (24, 24, 30); WHITE = (240, 240, 240)
DENIM = (86, 120, 176); DENIM_D = (62, 92, 144); BELT = (122, 84, 52); POUCH = (150, 116, 80)
SCARF = (240, 120, 26); SCARF_D = (196, 90, 16); SOCK = (96, 168, 60); SHOE = (128, 84, 54); SOLE = (84, 58, 38)
GOLD = (216, 164, 44)

m = RaceModel('android', None)

# Legs: straight blue jeans, green camo sock band, brown shoes
for side, bone in ((1, 'right_leg'), (-1, 'left_leg')):
    def X(a, b): return (a, b) if side > 0 else (-b, -a)
    x = X(0.1, 4.7)
    m.cube('jeans_' + bone, bone, 'jeans', [x[0], 4.1, -2.45], [x[1], 12, 2.45])
    xb = X(0.05, 4.9)
    m.cube('jeans_loose_' + bone, bone, 'jeans', [xb[0], 6.5, -2.6], [xb[1], 10.5, 2.6])
    xs = X(0.25, 4.5)
    m.cube('sock_' + bone, bone, 'sock', [xs[0], 2.0, -2.3], [xs[1], 4.2, 2.3])
    xo = X(0.3, 4.5)
    m.cube('shoe_' + bone, bone, 'shoe', [xo[0], 0, -2.45], [xo[1], 2.05, 2.25])
    m.cube('shoe_toe_' + bone, bone, 'shoe', [xo[0], 0, -3.5], [xo[1], 1.5, -2.45])

# Torso: black tee, brown belt with gold buckle, hip pouch on his left side
m.cube('torso', 'body', 'tee', [-4.9, 12, -2.9], [4.9, 24, 2.9])
m.cube('chest', 'body', 'tee', [-5.3, 16.5, -3.2], [5.3, 24, 3.05])
m.cube('belt', 'body', 'belt', [-5.15, 11.9, -3.15], [5.15, 13.7, 3.1])
m.cube('buckle', 'body', 'buckle', [-1.3, 11.8, -3.6], [1.3, 13.8, -3.15])
m.cube('pouch', 'body', 'pouch', [-7.6, 9.3, -2.6], [-4.95, 13.9, 0.6])
# bandana scarf: neck ring and the hanging front point
m.cube('scarf_front', 'body', 'scarf', [-3.4, 22.9, -3.6], [3.4, 24.9, -2.8])
m.cube('scarf_back', 'body', 'scarf', [-3.4, 22.9, 2.8], [3.4, 24.9, 3.55])
m.mirror('scarf_side', ('body', 'body'), 'scarf', [3.3, 22.9, -2.9], [4.05, 24.9, 2.9])
m.cube('scarf drape_r'.replace(' ', '_'), 'body', 'scarf', [0.15, 18.7, -3.55], [2.6, 23.2, -2.9], [0.3, 23.0, -3.2], (0, 0, 16))
m.cube('scarf_drape_l', 'body', 'scarf', [-2.6, 18.7, -3.55], [-0.15, 23.2, -2.9], [-0.3, 23.0, -3.2], (0, 0, -16))
m.cube('scarf_tip', 'body', 'scarf', [-0.75, 17.2, -3.5], [0.75, 19.2, -2.95])

# Arms: black short sleeve, white long undershirt, skin hands
m.mirror('sleeve', ('right_arm', 'left_arm'), 'tee', [5.2, 20.3, -2.35], [9.7, 24.25, 2.35])
m.mirror('undershirt', ('right_arm', 'left_arm'), 'under', [5.3, 13.4, -2.05], [9.5, 20.6, 2.05])
m.mirror('bicep', ('right_arm', 'left_arm'), 'under', [5.3, 15.8, -2.2], [9.65, 19.8, 2.15])
m.mirror('hand', ('right_arm', 'left_arm'), 'hand', [5.5, 9.2, -1.9], [9.35, 13.45, 1.9])

# Head
m.cube('head', 'head', 'head', [-4, 24, -4], [4, 32, 4])
m.mirror('ear', ('head', 'head'), 'skin', [4.0, 27.0, -0.6], [4.65, 29.2, 0.9])

# Hair: straight center-parted fall to the shoulders, flat fringe with a center notch
hb = 'hair'
m.cube('hair_top', hb, 'hair', [-4.42, 30.5, -4.5], [4.42, 33.05, 4.55])
m.cube('hair_back', hb, 'hair', [-4.45, 24.8, 0.6], [4.45, 32.3, 4.6])
m.cube('hair_fall', hb, 'hair', [-4.3, 19.6, 3.2], [4.3, 25.2, 4.65])
m.mirror('hair_side', (hb, hb), 'hair', [3.95, 20.6, -3.2], [4.48, 32.2, 0.9])
m.mirror('hair_strand', (hb, hb), 'hair', [3.85, 23.8, -3.9], [4.4, 28.6, -3.1])
m.cube('fringe_r', hb, 'hair', [0.8, 30.4, -4.58], [4.42, 33.05, -3.85])
m.cube('fringe_l', hb, 'hair', [-4.42, 30.4, -4.58], [-0.8, 33.05, -3.85])

# ------------------------------------------------------------------ painting
def paint_face(i, j, W, H):
    """Calm 17 face: light brows, blue eyes, relaxed slight smile."""
    sx = i - W // 2
    k = SKIN_V
    for side in (-1, 1):
        a = sx if side > 0 else -sx - 1
        if 1 <= a <= 6:
            if j == 7 and a >= 2: return (120, 110, 100), 'G'             # brow drawn in hair tint (dark)
            if j == 9 and 2 <= a <= 6: return (24, 20, 20), None
            if j == 10:
                if a == 6 or a == 2: return (24, 20, 20), None
                if a == 3: return (20, 20, 24), None
                if a == 4: return (120, 170, 220), 'B'
                return (246, 246, 246), None
            if j == 11 and 3 <= a <= 5: return (k * 0.88,) * 3, 'R'
    if j == 12 and sx == -1: return (k * 0.86,) * 3, 'R'
    if j == 14 and -1 <= sx <= 2: return (k * 0.5,) * 3, 'R'              # slight smile
    if j == 13 and sx == 3: return (k * 0.55,) * 3, 'R'
    if j >= 14 and (sx <= -6 or sx >= 5): return (k * 0.9,) * 3, 'R'
    return None

def paint(model, c, fn, s, t, W, H, i, j, P):
    mt = c['mat']; n = h01(zlib.crc32(c['name'].encode()) % 997, zlib.crc32(fn.encode()) % 97, i, j)
    edge = i == 0 or j == 0 or i == W - 1 or j == H - 1
    if mt == 'hair':
        v = HAIR_V * (0.9 + 0.1 * (1 - t)) * (0.93 + 0.09 * n)
        if i % 4 == 0 and fn not in ('up', 'down'): v *= 0.92             # straight strand lines
        if edge: v *= 0.84
        if fn == 'down': v *= 0.72
        return (v, v, v), 'G'
    if mt == 'scarf':
        # bandana print: small diamond dots on a checking grid
        cell = (int(P[0] * 2 + 40) + int(P[1] * 2)) % 4
        k = 0.94 + 0.08 * n
        if cell == 0: return mul(SCARF_D, k), None
        if edge: k *= 0.85
        return mul(SCARF, k), None
    if mt == 'tee':
        k = 0.94 + 0.08 * n
        if edge: k *= 0.86
        if fn == 'down': k *= 0.7
        return mul(TEE if P[1] > 10 else TEE_D, k), None
    if mt == 'under':
        k = 0.96 + 0.05 * n
        if edge: k *= 0.9
        if fn == 'down': k *= 0.75
        return mul(WHITE, k), None
    if mt == 'jeans':
        k = 0.94 + 0.08 * n
        if fn == 'south' and P[1] > 9.6 and abs(abs(P[0]) - 2.4) < 1.5:
            if abs(P[1] - 11.4) < 0.25 or abs(abs(P[0]) - 2.4) > 1.25: k *= 0.78   # back pocket outline
        if fn in ('north', 'south') and abs(abs(P[0]) - 0.15) < 0.18: k *= 0.85    # fly / center seam
        if abs(P[0]) < 0.5 and fn == 'north': k *= 0.9
        if edge: k *= 0.88
        if fn == 'down': k *= 0.7
        return mul(DENIM if P[1] > 5 else DENIM_D, k), None
    if mt == 'sock':
        cell = h01(int(P[0] * 3), int(P[1] * 3), int(P[2] * 3))
        base = SOCK if cell > 0.35 else (58, 110, 40)
        return mul(base, 0.92 + 0.12 * n), None
    if mt in ('belt', 'pouch'):
        k = 0.92 + 0.1 * n
        if mt == 'pouch' and t < 0.3: k *= 0.8                            # pouch flap
        if edge: k *= 0.84
        return mul(BELT if mt == 'belt' else POUCH, k), None
    if mt == 'buckle':
        return mul(GOLD, 0.9 + 0.14 * n), None
    if mt == 'shoe':
        if P[1] < 0.5: return mul(SOLE, 0.9 + 0.1 * n), None
        k = 0.92 + 0.1 * n
        if edge: k *= 0.85
        if fn == 'down': k *= 0.65
        return mul(SHOE, k), None
    if mt in ('skin', 'hand', 'head'):
        v = SKIN_V * (0.96 + 0.06 * n)
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
m.finish('gen_race_android.py')
