"""Yardrat (Pybara-inspired, per concepts/yardrat concept sheet): cube model + atlas + mask.

Bald mauve head with a wider cranium dome and scalp spots, and the race's signature
very long pointed ears sweeping up and out (tapered segment chains in the head
group). Cream shirt under a brown vest with orange trim, dark red sash, baggy
cream pants, brown shoes. Gentle face, no brows. Stocky standard frame.
"""
import math, zlib, sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from dba_model_lib import RaceModel, clamp, mul, h01

SKIN_V = 202
CREAM = (238, 228, 200); CREAM_D = (214, 202, 172); BROWN = (146, 92, 52); BROWN_D = (116, 70, 38)
TRIM = (232, 128, 40); SASH = (140, 34, 32); SASH_D = (108, 26, 26); SHOE = (110, 72, 44); SOLE = (70, 48, 30)

m = RaceModel('yardrat', None)

# Legs: baggy cream pants, skin ankle, brown shoes
for side, bone in ((1, 'right_leg'), (-1, 'left_leg')):
    def X(a, b): return (a, b) if side > 0 else (-b, -a)
    x = X(0.1, 4.9)
    m.cube('pants_' + bone, bone, 'pants', [x[0], 3.2, -2.5], [x[1], 12, 2.5])
    xb = X(0.0, 5.2)
    m.cube('pants_bag_' + bone, bone, 'pants', [xb[0], 5.0, -2.85], [xb[1], 9.4, 2.85])
    xc = X(0.2, 4.7)
    m.cube('ankle_' + bone, bone, 'skin', [xc[0], 2.0, -2.2], [xc[1], 3.4, 2.2])
    xo = X(0.3, 4.6)
    m.cube('shoe_' + bone, bone, 'shoe', [xo[0], 0, -2.5], [xo[1], 2.1, 2.3])
    m.cube('shoe_toe_' + bone, bone, 'shoe', [xo[0], 0, -3.6], [xo[1], 1.6, -2.5])

# Torso: cream shirt, brown open vest, orange trim, dark red sash
m.cube('torso', 'body', 'shirt', [-4.9, 12, -2.9], [4.9, 24, 2.9])
m.cube('vest_back', 'body', 'vest', [-5.15, 12.2, 1.9], [5.15, 24.2, 3.25])
m.mirror('vest_front', ('body', 'body'), 'vest', [2.1, 12.2, -3.3], [5.15, 24.2, -2.35])
m.cube('vest_skirt_back', 'body', 'vest', [-4.7, 5.8, 2.35], [4.7, 12.6, 3.2])
m.mirror('vest_skirt_front', ('body', 'body'), 'vest', [1.9, 5.8, -3.25], [4.7, 12.6, -2.45])
m.cube('belt', 'body', 'belt', [-5.2, 11.6, -3.2], [5.2, 13.6, 3.15])
m.cube('belt_knot', 'body', 'belt_knot', [-1.1, 8.2, -3.35], [0.3, 13.4, -2.95], [ -0.4, 13.2, -3.15], (6, 0, 4))

# Arms: cream long sleeves, vest shoulder caps, skin hands
m.mirror('sleeve', ('right_arm', 'left_arm'), 'shirt', [5.2, 13.2, -2.2], [9.6, 24.1, 2.2])
m.mirror('shoulder', ('right_arm', 'left_arm'), 'vest', [5.1, 20.6, -2.45], [9.75, 24.35, 2.45])
m.mirror('hand', ('right_arm', 'left_arm'), 'hand', [5.45, 9.2, -1.95], [9.35, 13.3, 1.95])

# Head: standard skull (face painted here) + wider cranium dome + the great ears
m.cube('head', 'head', 'head', [-4, 24, -4], [4, 32, 4])
m.cube('dome', 'head', 'dome', [-4.32, 28.6, -4.32], [4.32, 32.45, 4.32])

def ear(sign, tag):
    """Five tapered segments along a rising curve that also drifts backward, in the head group."""
    root = [4.05 * sign, 28.3, -0.4]
    angles = [34, 42, 50, 57, 62]
    widths = [3.5, 3.1, 2.6, 2.0, 1.25]
    seglen = 2.75
    p = list(root)
    for i, (phi, w) in enumerate(zip(angles, widths)):
        d = [math.cos(math.radians(phi)) * sign, math.sin(math.radians(phi)), 0]
        c = [p[0] + d[0] * seglen / 2, p[1] + d[1] * seglen / 2, p[2]]
        th = (phi - 90) * sign
        m.cube('ear_%s_%d' % (tag, i), 'head', 'ear',
               [c[0] - w / 2, c[1] - seglen / 2, c[2] - 0.8], [c[0] + w / 2, c[1] + seglen / 2, c[2] + 0.8],
               c, (0, -10 * sign, th))
        p = [p[0] + d[0] * seglen * 0.94, p[1] + d[1] * seglen * 0.94, p[2] + 0.62]
    # root wedge tying the ear into the skull
    m.cube('ear_%s_root' % tag, 'head', 'ear', [3.6 * sign - 0.9, 26.9, -1.2], [3.6 * sign + 0.9, 30.0, 0.9], [3.6 * sign, 28.4, -0.2], (0, 0, -18 * sign))
ear(1, 'r'); ear(-1, 'l')

# ------------------------------------------------------------------ painting
SPOTS = {(2, 1), (5, 0), (9, 2), (12, 1), (6, 3), (11, 4), (3, 5), (13, 3)}
def paint_face(i, j, W, H):
    """Gentle Yardrat face: no brows, calm eyes set a little low, small mouth."""
    sx = i - W // 2
    k = SKIN_V
    for side in (-1, 1):
        a = sx if side > 0 else -sx - 1
        if 2 <= a <= 5:
            if j == 9: return (k * 0.72,) * 3, 'R'                       # upper lid line (skin shade, no brow)
            if j in (10, 11):
                if a == 3: return (246, 246, 246), None                  # sclera
                if a == 4: return (30, 26, 34), None                     # pupil
                return (k * 0.9,) * 3, 'R'
            if j == 12 and 3 <= a <= 4: return (k * 0.84,) * 3, 'R'
    if j == 12 and sx in (-1, 0): return (k * 0.78,) * 3, 'R'            # nose shadow
    if j == 14 and -1 <= sx <= 1: return (k * 0.55,) * 3, 'R'            # small mouth
    if j >= 13 and (sx <= -6 or sx >= 5): return (k * 0.9,) * 3, 'R'     # jaw shade
    return None

def paint(model, c, fn, s, t, W, H, i, j, P):
    mt = c['mat']; n = h01(zlib.crc32(c['name'].encode()) % 997, zlib.crc32(fn.encode()) % 97, i, j)
    edge = i == 0 or j == 0 or i == W - 1 or j == H - 1
    if mt in ('skin', 'hand', 'head', 'dome', 'ear'):
        v = SKIN_V * (0.95 + 0.07 * n)
        if mt == 'ear':
            v *= 0.94
            if fn == 'north' and 0.25 < s < 0.75 and 0.15 < t < 0.85: v *= 0.86   # inner ear shading
        if mt == 'dome' and fn == 'up' and (i // 2, j // 2) in SPOTS: v *= 0.78  # scalp spots
        if mt == 'head' and fn == 'north':
            if j <= 2 and (i // 2, j) in ((3, 1), (11, 0), (7, 2)): v *= 0.8     # forehead spots
            r = paint_face(i, j, W, H)
            if r: return r
        if mt == 'hand':
            if fn == 'north' and 0.3 < t < 0.42 and i % 2 == 0: v *= 0.85
            if fn == 'down' or t > 0.85: v *= 0.88
        if edge and mt in ('skin', 'hand'): v *= 0.9
        if fn == 'down': v *= 0.8
        return (v, v, v), 'R'
    if mt == 'shirt' or mt == 'pants':
        col = CREAM if mt == 'shirt' else CREAM_D
        k = 0.95 + 0.08 * n
        if mt == 'pants':
            wv = math.sin(P[0] * 1.2 + P[2] * 0.9) * 0.5
            if abs(((P[1] + wv) % 2.4) - 1.2) < 0.3: k *= 0.9
        if t > 0.93 and fn not in ('up', 'down'): k *= 0.88
        if edge: k *= 0.9
        if fn == 'down': k *= 0.75
        return mul(col, k), None
    if mt == 'vest':
        # brown with orange trim only on the opening edges and hems, not every border
        if fn in ('north', 'south') and (i == 0 or i == W - 1 or j == H - 1): return mul(TRIM, 0.92 + 0.1 * n), None
        if fn in ('east', 'west') and j == H - 1: return mul(TRIM, 0.92 + 0.1 * n), None
        if fn == 'up': return mul(TRIM, 0.9 + 0.1 * n), None
        k = 0.93 + 0.1 * n
        if edge: k *= 0.88
        if fn == 'down': k *= 0.7
        return mul(BROWN if P[1] > 10 else BROWN_D, k), None
    if mt in ('belt', 'belt_knot'):
        k = 0.92 + 0.1 * n
        if edge: k *= 0.85
        return mul(SASH if mt == 'belt' else SASH_D, k), None
    if mt == 'shoe':
        if P[1] < 0.5: return mul(SOLE, 0.9 + 0.1 * n), None
        k = 0.92 + 0.1 * n
        if edge: k *= 0.85
        if fn == 'down': k *= 0.65
        return mul(SHOE, k), None
    return (200, 200, 200), None

m.paint_texel = paint
m.finish('gen_race_yardrat.py')
