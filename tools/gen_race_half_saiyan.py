"""Half-Saiyan (Gohan-inspired, per concepts/half-saiyan concept sheet): cube model + atlas + mask.

Purple Piccolo-style gi, red sash, tan wristbands, dark blue boots. Hair is a neat
bowl with one long front bang and two small cowlick prongs; SSJ2 spikes it sharply
up (single thinner bang, spark bolts), SSJ3 adds the long mane. Brown tail preview
in the bbmodel only; the runtime tail is the segmented TrailingTailLayer.
"""
import math, zlib, sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from dba_model_lib import RaceModel, clamp, mul, h01

SKIN_V = 214; HAIR_V = 210
PURPLE = (92, 62, 172); PURPLE_D = (60, 40, 122); PANTS = (80, 54, 152)
RED = (198, 32, 40); RED_D = (148, 22, 30); TAN = (208, 172, 112)
BOOT = (40, 46, 100); SOLE = (32, 32, 44); BROWN = (110, 70, 40)

m = RaceModel('half_saiyan', None)  # painter attached below

# ------------------------------------------------------------------ model
for side, bone in ((1, 'right_leg'), (-1, 'left_leg')):
    def X(a, b): return (a, b) if side > 0 else (-b, -a)
    x = X(0, 4.9)
    m.cube('pants_' + bone, bone, 'pants', [x[0], 4.2, -2.55], [x[1], 12, 2.55])
    xb = X(0.0, 5.2)
    m.cube('pants_bag_' + bone, bone, 'pants', [xb[0], 5.4, -2.9], [xb[1], 9.6, 2.9])
    xc = X(0.15, 4.75)
    m.cube('pants_cuff_' + bone, bone, 'pants_cuff', [xc[0], 4.0, -2.75], [xc[1], 5.4, 2.65])
    xo = X(0.3, 4.6)
    m.cube('boot_' + bone, bone, 'boot', [xo[0], 0, -2.6], [xo[1], 4.4, 2.35])
    m.cube('boot_toe_' + bone, bone, 'boot', [xo[0], 0, -3.7], [xo[1], 2.2, -2.6])
    xd = X(0.2, 4.7)
    m.cube('boot_band_' + bone, bone, 'boot_band', [xd[0], 3.5, -2.75], [xd[1], 4.5, 2.5])
# red sash ends hang at the front of each thigh
m.cube('sash_end_r', 'right_leg', 'belt', [0.5, 6.8, -3.2], [1.7, 11.9, -2.9], [1.1, 11.9, -3.05], (0, 0, 5))
m.cube('sash_end_l', 'left_leg', 'belt', [-2.8, 7.4, -3.2], [-1.6, 11.9, -2.9], [-2.2, 11.9, -3.05], (0, 0, -7))

# Torso: purple gi, slightly slimmer teen build, red sash
m.cube('torso', 'body', 'gi', [-4.8, 12, -2.9], [4.8, 24, 2.9])
m.cube('chest', 'body', 'gi', [-5.2, 16.5, -3.2], [5.2, 24, 3.0])
m.cube('belt', 'body', 'belt', [-5.1, 12.4, -3.25], [5.1, 15.0, 3.15])
m.cube('belt_knot', 'body', 'belt_knot', [0.6, 12.5, -3.95], [3.0, 15.2, -3.2])

# Arms: long purple sleeve to the wrist, tan wristband worn over it, skin hand
m.mirror('sleeve', ('right_arm', 'left_arm'), 'gi', [5.2, 13.9, -2.3], [9.7, 24.2, 2.3])
m.mirror('bicep', ('right_arm', 'left_arm'), 'gi', [5.15, 15.6, -2.4], [9.8, 19.6, 2.35])
m.mirror('wristband', ('right_arm', 'left_arm'), 'wrist', [5.05, 11.9, -2.42], [9.85, 14.1, 2.42])
m.mirror('hand', ('right_arm', 'left_arm'), 'hand', [5.4, 9.0, -1.85], [9.2, 11.95, 1.85])

# Head
m.cube('head', 'head', 'head', [-4, 24, -4], [4, 32, 4])
m.mirror('ear', ('head', 'head'), 'skin', [4.0, 26.8, -0.6], [4.7, 29.0, 0.9])

# Hair (base + SSJ): smooth bowl, straight fringe line, ONE long bang, two cowlick prongs
for hb in ('hair', 'hair_ssj2', 'hair_ssj3'):
    if hb == 'hair':
        m.cube('hair_top', hb, 'hair', [-4.45, 30.2, -4.5], [4.45, 33.1, 4.5])
        m.cube('hair_back', hb, 'hair', [-4.5, 25.2, 0.4], [4.5, 32.4, 4.6])
        m.cube('hair_fringe', hb, 'hair', [-4.45, 30.9, -4.6], [4.45, 33.1, -3.8])
        m.mirror('hair_side', (hb, hb), 'hair', [3.95, 28.8, -3.5], [4.5, 32.2, 0.8])
        m.mirror('hair_burn', (hb, hb), 'hair', [3.95, 26.6, -3.3], [4.45, 28.9, -1.5])
        # the one long bang, hanging down the front just left of center (as in the concept sheet)
        m.spike('bang_main', [-1.7, 32.9, -4.45], 6.4, 2.0, (5, 0, 173), False, bone=hb)
        m.spike('bang_wisp', [-3.3, 32.7, -4.35], 2.8, 1.4, (7, 0, 164), False, bone=hb)
        # two small cowlick prongs at the crown
        m.spike('cowlick', [0.8, 33.0, 0.3], 2.4, 1.15, (26, 0, -14), True, bone=hb)
    else:
        # SSJ2/SSJ3: hair sweeps sharply up into defined spikes, thinner single bang
        m.cube(hb + '_top', hb, 'hair', [-4.4, 30.6, -4.5], [4.4, 33.0, 4.5])
        m.cube(hb + '_back', hb, 'hair', [-4.45, 25.4, 0.3], [4.45, 32.2, 4.6])
        m.mirror(hb + '_side', (hb, hb), 'hair', [3.9, 28.6, -3.6], [4.5, 32.0, 1.0])
        SPIKES = [
            ('top', [0.5, 32.4, 0.2], 12.5, 5.4, (18, 0, -5), False),
            ('upper', [2.5, 32.0, 0.0], 12.0, 4.8, (14, 0, -24), True),
            ('side_hi', [3.8, 30.8, 0.6], 10.5, 4.4, (12, 0, -50), True),
            ('side', [4.0, 28.8, 1.2], 8.5, 4.0, (10, 0, -72), True),
            ('back_hi', [0, 31.0, 3.4], 11.5, 5.2, (50, 0, 0), False),
            ('back_side', [2.6, 30.0, 3.4], 10.0, 4.6, (62, 0, -36), True),
            ('back_lo', [0, 27.8, 3.9], 8.0, 4.8, (112, 0, 0), False),
        ]
        for n, bs, L, w, r, both in SPIKES:
            m.spike(n, bs, L, w, r, both, bone=hb)
        m.spike('bang_one', [-1.3, 32.9, -4.45], 5.6, 2.2, (4, 0, 170), False, bone=hb)
        if hb == 'hair_ssj2':
            for k, (bx, by, bz, ry) in enumerate([(5.6, 32.6, -0.5, 0), (-6.0, 30.8, 1.0, 0), (2.2, 38.4, -1.0, 90), (-3.0, 35.0, 4.2, 90)]):
                zz = [(0, 0, 1.7, 18), (0.85, 1.5, 1.7, -28), (0.2, 2.9, 1.5, 22)]
                for q, (dx, dy, l, rz) in enumerate(zz):
                    o = [bx + dx, by + dy, bz]
                    m.cube('hair_ssj2_bolt%d_%d' % (k, q), 'hair_ssj2', 'spark', [o[0] - 0.38, o[1], o[2] - 0.38], [o[0] + 0.38, o[1] + l, o[2] + 0.38], o, (0, ry, rz))
        if hb == 'hair_ssj3':
            m.cube('hair_ssj3_mane_root', 'hair_ssj3', 'hair', [-4.9, 22.2, 1.0], [4.9, 31.5, 5.9])
            m.spike('mane_c', [0, 26.0, 4.7], 21.0, 8.0, (168, 0, 0), False, bone='hair_ssj3')
            m.spike('mane_s', [2.7, 26.5, 3.9], 18.0, 6.0, (168, 0, 4), True, bone='hair_ssj3')
            m.spike('mane_f', [4.4, 29.0, -0.6], 10.5, 3.6, (176, 0, 14), True, bone='hair_ssj3')

# Tail preview (Blockbench only)
pts = [(0, 13.5, 3.0), (0, 12.2, 5.6), (0, 10.0, 7.8), (0, 7.4, 9.4), (0, 4.8, 10.6), (0, 2.8, 12.6), (0, 2.6, 15.2), (0, 4.4, 17.0), (0, 6.6, 17.2)]
for i in range(len(pts) - 1):
    a, b = pts[i], pts[i + 1]
    dy, dz = b[1] - a[1], b[2] - a[2]
    L = math.hypot(dy, dz); ang = math.degrees(math.atan2(dz, dy))
    w = 2.6 - i * 0.12
    m.cube('tail_%d' % i, 'tail', 'tail_tip' if i == len(pts) - 2 else ('tail' if i % 2 == 0 else 'tail_dark'),
           [a[0] - w / 2, a[1], a[2] - w / 2], [a[0] + w / 2, a[1] + L + 0.3, a[2] + w / 2], list(a), (ang, 0, 0), export=False)

# ------------------------------------------------------------------ painting
def paint_face(i, j, W, H):
    """16x16 Gohan face: steep angry brows, tall eyes. Row 0 is the top of the head front."""
    sx = i - W // 2
    k = SKIN_V
    for side in (-1, 1):
        ex = side * 3
        a = sx if side > 0 else -sx - 1
        if 1 <= a <= 6:
            by = 8 - (1 if a >= 3 else 0) - (1 if a >= 5 else 0)   # steeper brow slope than Goku
            if j == by or (j == by + 1 and a <= 2): return (110, 110, 110), 'G'
            if j == 9 and 2 <= a <= 6: return (24, 20, 20), None
            if j in (10, 11, 12):
                if a == 6: return (24, 20, 20), None
                if a == 2 and j <= 11: return (24, 20, 20), None
                if a == 2: return (k * 0.75,) * 3, 'R'
                if a == 3: return (20, 20, 24), None
                if a == 4: return (90, 90, 90), 'B'
                return (246, 246, 246), None
            if j == 13 and 3 <= a <= 5: return (k * 0.86,) * 3, 'R'
    if j == 14 and sx in (-1, 0): return (k * 0.8,) * 3, 'R'
    if j == 13 and sx == -1: return (k * 0.88,) * 3, 'R'
    if j == 15 and -1 <= sx <= 1: return (k * 0.45,) * 3, 'R'
    if j >= 14 and (sx <= -6 or sx >= 5): return (k * 0.88,) * 3, 'R'
    return None

def paint(model, c, fn, s, t, W, H, i, j, P):
    mt = c['mat']; n = h01(zlib.crc32(c['name'].encode()) % 997, zlib.crc32(fn.encode()) % 97, i, j)
    edge = i == 0 or j == 0 or i == W - 1 or j == H - 1
    front = fn == 'north'
    if mt in ('gi', 'pants', 'pants_cuff'):
        col = PURPLE if mt == 'gi' else PANTS
        k = 0.94 + 0.10 * n
        if mt == 'gi' and front and c['name'] in ('torso', 'chest'):
            ax = abs(P[0]); y = P[1]
            if y > 23.4 and ax < 1.5: return (SKIN_V * 0.92,) * 3, 'R'                     # skin at the neck V
            if y > 24 - (3.6 - ax) * 1.8 and ax < 3.6:
                if y < 24 - (3.6 - ax) * 1.8 + 0.6: return mul(PURPLE_D, 0.8), None       # lapel edge
                return mul(PURPLE_D, 0.92 + 0.06 * n), None                               # inner gi shadow
            if y > 24 - (3.6 - ax) * 1.8 - 0.6 and ax < 3.8: return mul(PURPLE_D, 0.9), None
            if 15 < y < 21 and abs(((P[0] * 0.8 + y) % 3.2) - 1.6) < 0.35: k *= 0.88
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
    if mt in ('belt', 'belt_knot'):
        k = 0.92 + 0.12 * n
        if mt == 'belt' and abs(((P[1] * 2 + P[0] * 0.3) % 2.0) - 1.0) < 0.25: k *= 0.85
        if mt == 'belt_knot' and abs(s - 0.5) < 0.12: k *= 0.75
        if edge: k *= 0.82
        if fn == 'down': k *= 0.6
        return mul(RED if mt == 'belt' else RED_D, k), None
    if mt == 'wrist':
        k = 0.92 + 0.12 * n
        if j == 1 or j == H - 2: k *= 0.82
        if edge: k *= 0.84
        return mul(TAN, k), None
    if mt in ('boot', 'boot_band'):
        if P[1] < 0.6: return mul(SOLE, 0.9 + 0.1 * n), None
        k = (0.92 + 0.12 * n) * (1.15 if mt == 'boot_band' else 1.0)
        if edge: k *= 0.82
        if fn == 'down': k *= 0.6
        return mul(BOOT, k), None
    if mt in ('tail', 'tail_dark', 'tail_tip'):
        k = (0.85 if mt == 'tail_dark' else 1.0) * (0.7 if mt == 'tail_tip' else 1.0) * (0.9 + 0.15 * n)
        return mul(BROWN, k), None
    if mt == 'spark':
        return ((235, 250, 255) if (i + j) % 2 == 0 else (120, 200, 255)), None
    if mt == 'hair':
        v = HAIR_V * (0.86 + 0.14 * (1 - t)) * (0.9 + 0.12 * n)
        if i % 3 == 0 and fn not in ('up', 'down'): v *= 0.88
        if edge: v *= 0.8
        if fn == 'down': v *= 0.7
        return (v, v, v), 'G'
    if mt in ('skin', 'skin_arm', 'hand', 'head'):
        v = SKIN_V * (0.96 + 0.06 * n)
        if mt == 'hand':
            if fn == 'north' and 0.3 < t < 0.42 and i % 2 == 0: v *= 0.8
            if fn == 'down' or t > 0.85: v *= 0.86
        if mt == 'head' and fn == 'north':
            r = paint_face(i, j, W, H)
            if r: return r
        if mt == 'head' and fn == 'down': v *= 0.75
        if edge and mt != 'head': v *= 0.88
        return (v, v, v), 'R'
    return (200, 200, 200), None

m.paint_texel = paint
m.finish('gen_race_half_saiyan.py')
