"""Arcosian (Frieza final form-inspired, per concepts/arcosian concept sheet).

Sleek pale bio-armor body (skin-tinted, so the golden form override turns it
gold while the purple gems stay purple), purple dome plate with a forehead
point, black horns sweeping up, back and hooking down, narrow red eyes with a
smug smile, purple chest gem, shoulder plates, forearm bands and shin plates.
The long tail is procedural in game; a lavender banded preview is included for
the Blockbench sheet.
"""
import math, zlib, sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from dba_model_lib import RaceModel, clamp, mul, h01, eye_tint

SKIN_V = 238
GEM = (146, 102, 224); GEM_L = (182, 144, 248); GEM_D = (110, 72, 178)
HORN = (24, 24, 31); RED = (216, 32, 46)

m = RaceModel('arcosian', None, extra_groups={
    'head_horns': 'head_horns',
    'head_horns__arcosian_form_2': 'head_horns__arcosian_form_2',
    'head_horns__arcosian_form_3': 'head_horns__arcosian_form_3'})

def seg_chain(name, bone, mat, pts, w0, w1, export=True):
    """Tapered cubes along a y/z polyline, Saiyan-script convention."""
    for i in range(len(pts) - 1):
        a, b = pts[i], pts[i + 1]
        dy, dz = b[1] - a[1], b[2] - a[2]
        L = math.hypot(dy, dz)
        ang = math.degrees(math.atan2(dz, dy))
        w = w0 + (w1 - w0) * (i + 0.5) / (len(pts) - 1)
        m.cube('%s_%d' % (name, i), bone, mat,
               [a[0] - w / 2, a[1], a[2] - w / 2], [a[0] + w / 2, a[1] + L + 0.3, a[2] + w / 2],
               list(a), (ang, 0, 0), export=export)

# Legs: slim white legs, purple shin plates, white feet with toe box
for side, bone in ((1, 'right_leg'), (-1, 'left_leg')):
    def X(a, b): return (a, b) if side > 0 else (-b, -a)
    x = X(0.15, 4.65)
    m.cube('leg_' + bone, bone, 'skin_leg', [x[0], 1.6, -2.4], [x[1], 12, 2.4])
    xp = X(1.3, 3.5)
    m.cube('shin_' + bone, bone, 'gem', [xp[0], 1.8, -2.78], [xp[1], 6.8, -2.18])
    xf = X(0.3, 4.5)
    m.cube('foot_' + bone, bone, 'skin_leg', [xf[0], 0, -2.4], [xf[1], 1.75, 2.3])
    m.cube('toe_' + bone, bone, 'skin_leg', [xf[0], 0, -3.55], [xf[1], 1.05, -2.4])

# Torso: slim white body, purple chest gem (diamond)
m.cube('torso', 'body', 'skin_body', [-4.7, 12, -2.85], [4.7, 24, 2.85])
m.cube('chest', 'body', 'skin_body', [-5.0, 16.8, -3.1], [5.0, 24, 3.0])
m.cube('gem', 'body', 'gem', [-1.35, 17.8, -3.62], [1.35, 20.5, -3.02], [0, 19.15, -3.3], (0, 0, 45))
m.cube('pelvis', 'body', 'skin_body', [-4.6, 11.6, -2.8], [4.6, 13.2, 2.8])

# Arms: white, purple shoulder plates and forearm bands
m.mirror('arm', ('right_arm', 'left_arm'), 'skin_arm', [5.35, 13.6, -2.05], [9.5, 24.0, 2.05])
m.mirror('plate', ('right_arm', 'left_arm'), 'gem', [4.9, 22.7, -2.7], [10.0, 24.55, 2.7])
m.mirror('band', ('right_arm', 'left_arm'), 'gem', [5.25, 14.3, -2.3], [9.6, 16.15, 2.3])
m.mirror('hand', ('right_arm', 'left_arm'), 'hand', [5.5, 9.4, -1.85], [9.35, 13.65, 1.85])

# Head: white face, purple dome with forehead point, black swept horns
m.cube('head', 'head', 'head', [-4, 24, -4], [4, 32, 4])
m.mirror('ear', ('head', 'head'), 'skin', [4.0, 27.4, -0.5], [4.55, 28.9, 0.85])
m.cube('dome', 'head', 'gem', [-3.3, 31.6, -3.5], [3.3, 33.3, 3.5])
m.cube('domepoint', 'head', 'gem', [-0.95, 29.6, -4.34], [0.95, 31.9, -3.9])
# Horns live in their own head group so forms can swap them (Custom3DModelLayer
# draws head_<part>__<formpath> in place of head_<part> while that form is active)
m.spike('horn1', [3.9, 30.8, -0.2], 7.0, 2.5, (38, 0, -34), True, bone='head_horns', mat='horn')
m.spike('horn2', [6.9, 35.2, 3.6], 5.5, 1.6, (100, 0, -20), True, bone='head_horns', mat='horn')

# Second form: thick bull horns out of the temples, curving outward then up,
# with a heavier purple dome rim
m.spike('f2horn1', [3.4, 29.6, 0.2], 6.4, 2.9, (6, 0, -62), True, bone='head_horns__arcosian_form_2', mat='horn')
m.spike('f2horn2', [8.6, 32.6, 0.4], 7.5, 2.4, (8, 0, -12), True, bone='head_horns__arcosian_form_2', mat='horn')
m.cube('f2rim', 'head_horns__arcosian_form_2', 'gem', [-4.15, 30.9, -4.15], [4.15, 32.0, 4.15])

# Third form: elongated cranium sweeping up and back in stepped pale plates
# with purple bands, small horn nubs at the temples
for k, (y0, z0, w) in enumerate(((29.8, 0.6, 7.8), (31.4, 2.2, 7.0), (32.8, 3.8, 6.0), (34.0, 5.4, 4.8), (35.0, 6.9, 3.4))):
    m.cube('f3crest%d' % k, 'head_horns__arcosian_form_3', 'skin' if k % 2 == 0 else 'gem',
           [-w / 2, y0, z0 - 3.0], [w / 2, y0 + 3.0, z0 + 2.6], [0, y0, z0], (-30, 0, 0))
m.spike('f3nub', [3.9, 29.8, -0.4], 2.4, 1.6, (20, 0, -50), True, bone='head_horns__arcosian_form_3', mat='horn')

# Tail preview (Blockbench/renders only; runtime tail is TrailingTailLayer)
seg_chain('tailprev', 'tail', 'tailp',
          [[0, 13.2, 3.0], [0, 10.2, 6.2], [0, 6.8, 8.8], [0, 3.6, 9.8], [0, 1.9, 7.8], [0, 2.4, 5.4], [0, 4.0, 3.6]],
          1.9, 0.35, export=False)

# ------------------------------------------------------------------ painting
def paint_face(i, j, W, H, closed=False):
    """Frieza face: narrow eyes, brow ridge shade, smug smile."""
    sx = i - W // 2
    k = SKIN_V
    for side in (-1, 1):
        a = sx if side > 0 else -sx - 1
        if 1 <= a <= 6:
            if j == 7 and a >= 2: return (k * 0.82,) * 3, 'R'             # brow ridge shade
            if j == 9 and 2 <= a <= 6: return (20, 18, 22), None          # liner
            if j == 10:
                if closed:
                    return eye_tint(130 if a in (2, 6) else 210)
                if a in (2, 6): return (20, 18, 22), None
                if a == 3: return eye_tint(100)
                if a == 4: return eye_tint(235)
                return (240, 240, 240), None
            if j == 11 and 3 <= a <= 5: return (k * 0.88,) * 3, 'R'
    if j == 12 and sx == -1: return (k * 0.9,) * 3, 'R'
    if j == 14 and -1 <= sx <= 1: return (122, 72, 132), None             # smug smile
    if j == 13 and sx == 2: return (122, 72, 132), None
    if j >= 14 and (sx <= -6 or sx >= 5): return (k * 0.92,) * 3, 'R'
    return None

def paint(model, c, fn, s, t, W, H, i, j, P):
    mt = c['mat']; n = h01(zlib.crc32(c['name'].encode()) % 997, zlib.crc32(fn.encode()) % 97, i, j)
    edge = i == 0 or j == 0 or i == W - 1 or j == H - 1
    if mt == 'gem':
        k = 0.93 + 0.1 * n
        col = GEM_L if fn == 'up' else (GEM if P[1] > 8 else GEM_D)
        if c['name'] == 'gem' and fn == 'north' and abs(s - 0.5) < 0.2 and abs(t - 0.5) < 0.2: col = GEM_L
        if edge: k *= 0.85
        if fn == 'down': k *= 0.62
        return mul(col, k), None
    if mt == 'horn':
        k = 0.9 + 0.16 * n
        if (P[1] % 2.2) < 0.4: k *= 0.8                                   # subtle ridges
        if edge: k *= 0.85
        return mul(HORN, k), None
    if mt == 'tailp':
        idx = int(c['name'].rsplit('_', 1)[1]) if c['name'].startswith('tailprev_') else 0
        col = (198, 180, 234) if idx % 2 == 0 else (150, 110, 215)
        if idx >= 5: col = (126, 84, 196)
        return mul(col, 0.93 + 0.09 * n), None
    if mt in ('skin', 'skin_arm', 'skin_leg', 'skin_body', 'hand', 'head'):
        v = SKIN_V * (0.97 + 0.045 * n)
        if mt == 'hand':
            if fn == 'north' and 0.3 < t < 0.45 and i % 3 == 1: v *= 0.84   # finger lines
            if fn == 'down' or t > 0.85: v *= 0.9
        if mt == 'skin_leg' and c['name'].startswith('toe') and fn == 'north' and i % 3 == 1: v *= 0.84
        if mt == 'head' and fn == 'north':
            r = paint_face(i, j, W, H, model.blink)
            if r: return r
        if mt == 'head' and fn == 'down': v *= 0.85
        if edge and mt != 'head': v *= 0.93
        return (v, v, v), 'R'
    return (200, 200, 200), None

m.paint_texel = paint
m.finish('gen_race_arcosian.py')
