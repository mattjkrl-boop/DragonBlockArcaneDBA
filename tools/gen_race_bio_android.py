"""Bio-Android (Perfect Cell-inspired, per concepts/bio-android concept sheet).

Pale face with purple eye patches, purple eyes, black cheek lines and a gold
chin patch. Green spotted exoskeleton (skin-tinted shell with darker spots and
segment banding on the limbs), black crown dome with two tall green prongs,
tan ear caps, black chest plate and shoulder armor, black wing plates folded
on the back, orange hip sections, white hands, black pointed feet. The curled
segmented stinger tail is procedural (TrailingTailLayer), not part of the OBJ.
"""
import math, zlib, sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from dba_model_lib import RaceModel, clamp, mul, h01, eye_tint

FACE_V = 230; SHELL_V = 178
BLACK = (26, 28, 32); BLACK_D = (17, 19, 24); WING = (20, 22, 29)
ORANGE = (216, 116, 32); ORANGE_D = (178, 92, 24)
EAR = (238, 198, 138); EAR_IN = (172, 124, 74)
PURPLE = (148, 104, 216); PURPLE_D = (104, 70, 168); GOLD = (232, 184, 72)

m = RaceModel('bio_android', None)

def seg_chain(name, bone, mat, pts, w0, w1, export=True):
    """Tapered cubes along a y/z polyline, Saiyan-script convention:
    each segment is a cube grown along +y from its start point, pitched by
    atan2(dz, dy) about that point."""
    for i in range(len(pts) - 1):
        a, b = pts[i], pts[i + 1]
        dy, dz = b[1] - a[1], b[2] - a[2]
        L = math.hypot(dy, dz)
        ang = math.degrees(math.atan2(dz, dy))
        w = w0 + (w1 - w0) * (i + 0.5) / (len(pts) - 1)
        m.cube('%s_%d' % (name, i), bone, mat,
               [a[0] - w / 2, a[1], a[2] - w / 2], [a[0] + w / 2, a[1] + L + 0.3, a[2] + w / 2],
               list(a), (ang, 0, 0), export=export)

# Legs: spotted green shell, banded shins, black pointed feet
for side, bone in ((1, 'right_leg'), (-1, 'left_leg')):
    def X(a, b): return (a, b) if side > 0 else (-b, -a)
    x = X(0.1, 4.9)
    m.cube('thigh_' + bone, bone, 'shell', [x[0], 5.2, -2.5], [x[1], 12, 2.5])
    xs = X(0.25, 4.65)
    m.cube('shin_' + bone, bone, 'shell', [xs[0], 1.5, -2.35], [xs[1], 5.6, 2.35])
    xf = X(0.3, 4.55)
    m.cube('foot_' + bone, bone, 'plate', [xf[0], 0, -2.4], [xf[1], 1.7, 2.3])
    cx = 2.45 if side > 0 else -2.45
    seg_chain('toe_' + bone, bone, 'plate',
              [[cx, 0.95, -2.2], [cx, 0.6, -3.8], [cx, 0.3, -5.1]], 1.7, 0.35)

# Torso: black chest plate, green abdomen, orange hip sections, back wings
m.cube('chest', 'body', 'plate', [-5.4, 17.4, -3.1], [5.4, 24, 3.1])
m.cube('abdomen', 'body', 'shell', [-4.9, 12.0, -2.9], [4.9, 17.8, 2.9])
m.mirror('hip', ('body', 'body'), 'hipmat', [0.35, 12.0, -3.25], [5.05, 13.7, -2.55])
m.cube('groin', 'body', 'plate', [-1.7, 12.0, -3.2], [1.7, 13.5, -2.6])
m.mirror('wing', ('body', 'body'), 'wing', [0.35, 13.0, 3.35], [5.0, 23.2, 3.95], [0.4, 19.0, 3.6], (0, -8, 5))

# Arms: black shoulder pads, segmented green arms, white hands
m.mirror('pad', ('right_arm', 'left_arm'), 'plate', [4.95, 21.5, -2.95], [10.05, 24.7, 2.95], [5.6, 22.0, 0], (0, 0, 7))
m.mirror('upper', ('right_arm', 'left_arm'), 'shell', [5.4, 16.4, -2.2], [9.6, 21.9, 2.2])
m.mirror('fore', ('right_arm', 'left_arm'), 'shell', [5.35, 11.7, -2.3], [9.65, 16.8, 2.3])
m.mirror('hand', ('right_arm', 'left_arm'), 'face', [5.6, 8.5, -1.9], [9.4, 11.8, 1.9])

# Head: pale face, black crown dome, green prongs/band/skirts, tan ear caps
m.cube('head', 'head', 'face', [-4, 24, -4], [4, 32, 4])
m.cube('dome', 'head', 'crown', [-3.0, 31.7, -3.4], [3.0, 34.1, 3.2])
m.cube('band', 'head', 'shell', [-4.32, 29.9, -4.35], [4.32, 32.7, -3.5])
m.mirror('skirt', ('head', 'head'), 'shell', [3.85, 27.5, -2.7], [4.35, 32.7, 4.35])
m.cube('crownback', 'head', 'shell', [-4.32, 28.4, 3.55], [4.32, 32.7, 4.35])
m.mirror('earcap', ('head', 'head'), 'ear', [4.0, 26.5, -1.15], [5.15, 29.7, 1.15])
m.spike('prong', [2.75, 32.0, -0.4], 11.5, 3.0, (14, 0, -9), True, bone='head', mat='shell')

# Tail preview (Blockbench/renders only; in game the tail is the procedural
# TrailingTailLayer stinger tail). Curled up behind the legs like the concept.
_tail_pts = [[0, 13.2, 3.0], [0, 10.5, 5.6], [0, 7.6, 7.4], [0, 5.2, 7.8], [0, 3.4, 6.6], [0, 3.0, 4.8], [0, 4.2, 3.4]]
seg_chain('tailprev', 'tail', 'shell', _tail_pts, 2.3, 0.5, export=False)
seg_chain('tailtip', 'tail', 'hipmat', [[0, 3.2, 4.9], [0, 4.2, 3.4], [0, 5.4, 2.95]], 0.6, 0.15, export=False)

# ------------------------------------------------------------------ painting
FACE = (226, 240, 214)   # pale, unmasked: Cell's face stays pale on any skin tint

def paint_face(i, j, W, H, closed=False):
    """Cell face: purple patches, narrow eyes, cheek lines, gold chin."""
    sx = i - W // 2
    k = FACE_V
    for side in (-1, 1):
        a = sx if side > 0 else -sx - 1
        if 1 <= a <= 6:
            if j == 7 and a >= 2: return (58, 48, 72), None               # heavy brow ridge
            if 8 <= j <= 12 and 2 <= a <= 6:
                base = PURPLE if (j + a) % 3 else PURPLE_D
                if j == 9 and 2 <= a <= 6: return (22, 18, 24), None      # upper lid line
                if j == 10:
                    # The eyelid is the purple patch. The shut line stays eye-tinted.
                    if closed:
                        if a in (3, 4, 5): return eye_tint(200)
                        return (22, 18, 24), None
                    if a in (2, 6): return (22, 18, 24), None
                    if a == 4: return eye_tint(235)
                    if a in (3, 5): return eye_tint(210)
                    return (242, 242, 242), None
                if j == 11 and a in (2, 6): return (22, 18, 24), None
                return base, None                                         # purple patch
            if 12 <= j <= 15 and a == 3: return (20, 16, 20), None        # cheek line
    if j == 12 and sx == -1: return mul(FACE, 0.86), None                 # nose shade
    if j == 14 and -1 <= sx <= 1: return mul(FACE, 0.45), None            # flat mouth
    if j == 15 and -3 <= sx <= 2: return GOLD, None                       # gold chin patch
    if j >= 14 and (sx <= -6 or sx >= 5): return mul(FACE, 0.9), None
    return None

def paint(model, c, fn, s, t, W, H, i, j, P):
    mt = c['mat']; n = h01(zlib.crc32(c['name'].encode()) % 997, zlib.crc32(fn.encode()) % 97, i, j)
    edge = i == 0 or j == 0 or i == W - 1 or j == H - 1
    if mt == 'shell':
        v = SHELL_V * (0.95 + 0.07 * n)
        cell = h01(int(P[0] * 1.15 + 60), int(P[1] * 1.15 + 60), int(P[2] * 1.15 + 60))
        cell2 = h01(int(P[0] * 3.1 + 9), int(P[1] * 3.1 + 9), int(P[2] * 3.1 + 9))
        if cell > 0.64: v *= 0.55                                         # big dark spots
        elif cell2 > 0.90: v *= 0.8                                       # rare speckles
        if c['bone'] in ('right_arm', 'left_arm', 'right_leg', 'left_leg') and (P[1] % 2.8) < 0.55:
            v *= 0.7                                                      # limb segment bands
        if c['name'].startswith('head_prong3') or 'prong3' in c['name']: v *= 0.3   # black prong tips
        if edge: v *= 0.88
        if fn == 'down': v *= 0.7
        return (v, v, v), 'R'
    if mt in ('plate', 'crown', 'wing'):
        base = WING if mt == 'wing' else BLACK
        k = 0.92 + 0.12 * n
        if mt == 'plate' and c['name'] == 'chest' and fn == 'north':
            dy = abs(P[1] - 19.6); dx = abs(P[0])
            if dx < max(0.0, 1.85 - dy * 0.8): return mul((52, 56, 66), k), None   # chest gem shading
        if edge: k *= 0.85
        if fn == 'down': k *= 0.65
        return mul(base if P[1] > 4 else BLACK_D, k), None
    if mt == 'hipmat':
        k = 0.92 + 0.1 * n
        if edge: k *= 0.85
        if fn == 'down': k *= 0.65
        return mul(ORANGE if P[1] > 12.4 else ORANGE_D, k), None
    if mt == 'ear':
        if fn in ('east', 'west') and 0.28 < s < 0.72 and 0.28 < t < 0.72:
            return mul(EAR_IN, 0.9 + 0.1 * n), None
        k = 0.94 + 0.08 * n
        if edge: k *= 0.87
        return mul(EAR, k), None
    if mt == 'face':
        shade = 0.97 + 0.05 * n
        if c['bone'] == 'head' and fn == 'north':
            r = paint_face(i, j, W, H, model.blink)
            if r: return r
        if fn == 'down': shade *= 0.8
        if c['bone'] != 'head' and fn == 'north' and 0.3 < t < 0.42 and i % 2 == 0: shade *= 0.85   # finger lines
        return mul(FACE, shade), None
    return (200, 200, 200), None

m.paint_texel = paint
m.finish('gen_race_bio_android.py')
