"""Smooth stylized race mesh generator (Saiyan first).

Builds lofted, tapered, rounded parts (head, torso, arms with gloves, legs with boots,
nose, ears) as a Blockbench mesh model (assets/blockbench/races/<race>_smooth.bbmodel),
exports the runtime OBJ (same 6 groups and identical per-group bounds as the cube rig, so
ObjMesh derives the same pivots and animations are unchanged), and paints a 512px
hand-shaded atlas plus the DynamicSkinManager mask (R skin, G hair/brows, B iris).
"""
import json, math, base64, io, pathlib, sys
from PIL import Image, ImageDraw, ImageFilter

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / 'src/main/resources/assets/dragonblockarcanedba'
TEX = 512
NSEG = 24

def lerp(a, b, t): return a + (b - a) * t
def smooth(e0, e1, x):
    t = max(0.0, min(1.0, (x - e0) / (e1 - e0))); return t * t * (3 - 2 * t)
def interp(prof, t):
    """prof: list of (t, *vals) sorted; smooth-step interpolation."""
    if t <= prof[0][0]: return prof[0][1:]
    for a, b in zip(prof, prof[1:]):
        if t <= b[0]:
            k = (t - a[0]) / (b[0] - a[0]); k = k * k * (3 - 2 * k)
            return tuple(lerp(x, y, k) for x, y in zip(a[1:], b[1:]))
    return prof[-1][1:]

class Part:
    def __init__(s, name, group, rect):
        s.name, s.group, s.rect = name, group, rect
        s.V, s.UV, s.F = [], [], []   # F: list of vertex index lists (uv index == vertex index)

def loft(name, group, rect, prof, cx, cy0, bump=None, rings=28, cap_top=True, cap_bot=True):
    """prof rows: (t in [0,1] along height, height_px, rx, rz, dz). Ring i at y=cy0+h."""
    p = Part(name, group, rect)
    ts = [i / (rings - 1) for i in range(rings)]
    u0, v0, uw, vh = rect
    for i, t in enumerate(ts):
        h, rx, rz, dz = interp(prof, t)
        for j in range(NSEG + 1):
            th = 2 * math.pi * j / NSEG              # 0 = back (-z), pi = front (+z)
            sx, sz = math.sin(th), -math.cos(th)
            k = 1.0 + (bump(th, t) if bump else 0.0)
            p.V.append((cx + rx * sx * k, cy0 + h, dz + rz * sz * k))
            p.UV.append((u0 + uw * j / NSEG, v0 + vh * (1 - t)))
    W = NSEG + 1
    for i in range(rings - 1):
        for j in range(NSEG):
            a, b = i * W + j, i * W + j + 1
            p.F.append([a, a + W, b + W, b])
    for cap, i in ((cap_bot, 0), (cap_top, rings - 1)):
        if not cap: continue
        ring = [i * W + j for j in range(NSEG)]
        c = [sum(p.V[r][k] for r in ring) / NSEG for k in range(3)]
        c[1] += (0.25 if i else -0.25)
        p.V.append(tuple(c)); p.UV.append(p.UV[ring[0]]); ci = len(p.V) - 1
        for j in range(NSEG):
            a, b = ring[j], ring[(j + 1) % NSEG]
            p.F.append([a, b, ci] if i else [b, a, ci])
    p.prof, p.cx, p.cy0 = prof, cx, cy0
    return p

def ellipsoid(name, group, rect, c, r, rings=8, seg=10):
    p = Part(name, group, rect); u0, v0, uw, vh = rect
    for i in range(rings + 1):
        ph = math.pi * i / rings
        for j in range(seg + 1):
            th = 2 * math.pi * j / seg
            p.V.append((c[0] + r[0] * math.sin(ph) * math.sin(th), c[1] - r[1] * math.cos(ph), c[2] - r[2] * math.sin(ph) * math.cos(th)))
            p.UV.append((u0 + uw * j / seg, v0 + vh * (1 - i / rings)))
    W = seg + 1
    for i in range(rings):
        for j in range(seg):
            a, b = i * W + j, i * W + j + 1
            p.F.append([a, a + W, b + W, b])
    return p

def orient(p):
    """Make every face wind outward (normal away from part centroid region)."""
    cen = [sum(v[k] for v in p.V) / len(p.V) for k in range(3)]
    out = 0; tot = 0
    for f in p.F:
        a, b, c = (p.V[i] for i in f[:3])
        n = cross(sub(b, a), sub(c, a)); m = [sum(p.V[i][k] for i in f) / len(f) for k in range(3)]
        d = sum(n[k] * (m[k] - cen[k]) for k in range(3))
        if abs(d) > 1e-9: tot += 1; out += d > 0
    if out < tot / 2: p.F = [f[::-1] for f in p.F]

def sub(a, b): return [a[i] - b[i] for i in range(3)]
def cross(a, b): return [a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0]]
def norm(a):
    l = math.sqrt(sum(x*x for x in a)) or 1; return [x / l for x in a]

# ------------------------------------------------------------------ geometry (pixels, front +z)
def build():
    P = []
    # atlas rects (u, v, w, h) in texture px
    R = dict(head=(0, 0, 256, 160), torso=(256, 0, 256, 200), arm_r=(0, 160, 128, 190), arm_l=(128, 160, 128, 190),
             leg_r=(256, 200, 128, 190), leg_l=(384, 200, 128, 190), nose=(0, 360, 32, 24), ear_r=(40, 360, 32, 24), ear_l=(80, 360, 32, 24))
    # head: t 0 (neck) .. 1 (crown)
    head = [(0.00, 0.0, 1.5, 1.4, 0.0), (0.10, 0.8, 2.2, 2.3, 0.35), (0.25, 2.0, 3.0, 3.3, 0.45), (0.45, 3.6, 3.6, 3.7, 0.2),
            (0.65, 5.3, 3.7, 3.8, 0.0), (0.82, 6.8, 3.3, 3.5, -0.1), (0.94, 7.8, 2.2, 2.5, -0.1), (1.00, 8.2, 0.6, 0.7, -0.1)]
    def head_bump(th, t):
        f = math.exp(-((th - math.pi) / 0.55) ** 2)             # frontness
        brow = 0.07 * f * math.exp(-((t - 0.58) / 0.05) ** 2)
        cheek = 0.05 * math.exp(-((abs(th - math.pi) - 0.9) / 0.35) ** 2) * math.exp(-((t - 0.38) / 0.1) ** 2)
        return brow + cheek
    P.append(loft('head', 'head', R['head'], head, 0, 26.4, head_bump, rings=30))
    P.append(ellipsoid('nose', 'head', R['nose'], (0, 26.4 + 3.9, 3.9), (0.45, 0.8, 0.5), 6, 8))
    P.append(ellipsoid('ear_r', 'head', R['ear_r'], (-3.7, 26.4 + 4.0, -0.2), (0.45, 1.0, 0.7), 6, 8))
    P.append(ellipsoid('ear_l', 'head', R['ear_l'], (3.7, 26.4 + 4.0, -0.2), (0.45, 1.0, 0.7), 6, 8))
    # torso: hips .. neck; V-taper with pecs, lats and traps
    torso = [(0.00, 0.0, 3.0, 1.9, 0.0), (0.12, 1.6, 3.1, 2.0, 0.0), (0.28, 3.6, 2.6, 1.8, 0.05), (0.45, 6.0, 2.9, 1.95, 0.1),
             (0.66, 9.0, 3.7, 2.35, 0.15), (0.80, 11.2, 3.9, 2.3, 0.1), (0.90, 12.8, 3.2, 1.9, 0.0), (0.97, 13.9, 1.6, 1.3, 0.0), (1.00, 14.8, 1.25, 1.1, 0.0)]
    def torso_bump(th, t):
        front = math.exp(-((th - math.pi) / 0.8) ** 2)
        pec = 0.10 * math.exp(-((abs(th - math.pi) - 0.55) / 0.35) ** 2) * math.exp(-((t - 0.72) / 0.07) ** 2)
        abs_ = 0.035 * front * (0.5 + 0.5 * math.cos((t - 0.30) * 2 * math.pi / 0.11)) * smooth(0.2, 0.28, t) * (1 - smooth(0.52, 0.6, t))
        lat = 0.06 * math.exp(-((abs(th - math.pi / 2 if th < math.pi else th - 3 * math.pi / 2) ) / 0.5) ** 2) * math.exp(-((t - 0.6) / 0.12) ** 2)
        return pec + abs_ + lat
    P.append(loft('torso', 'body', R['torso'], torso, 0, 11.8, torso_bump, rings=34))
    # arms: hand (bottom) .. shoulder (top); glove up to wrist cuff
    arm = [(0.00, 0.0, 0.8, 0.9, 0.0), (0.05, 0.5, 1.25, 1.3, 0.05), (0.15, 1.6, 1.35, 1.35, 0.05), (0.20, 2.1, 1.5, 1.45, 0.0),
           (0.23, 2.4, 1.0, 1.0, 0.0), (0.40, 4.2, 1.35, 1.25, 0.0), (0.52, 5.4, 1.05, 1.05, 0.0), (0.70, 7.2, 1.45, 1.45, 0.2),
           (0.86, 8.9, 1.75, 1.7, 0.0), (0.95, 10.0, 1.55, 1.45, 0.0), (1.00, 10.6, 0.8, 0.8, 0.0)]
    P.append(loft('arm_r', 'rightarm', R['arm_r'], arm, -6.0, 14.6, rings=32))
    P.append(loft('arm_l', 'leftarm', R['arm_l'], arm, 6.0, 14.6, rings=32))
    # legs: sole .. hip; boot to mid shin with forward toe
    leg = [(0.00, 0.0, 1.35, 2.0, 0.5), (0.04, 0.5, 1.5, 2.15, 0.5), (0.12, 1.4, 1.35, 1.8, 0.35), (0.22, 2.6, 1.25, 1.35, 0.1),
           (0.36, 4.2, 1.4, 1.45, 0.0), (0.38, 4.45, 1.2, 1.25, 0.0), (0.50, 6.0, 1.35, 1.4, -0.15), (0.60, 7.2, 1.15, 1.2, 0.0),
           (0.80, 9.6, 1.55, 1.5, 0.05), (1.00, 12.0, 1.6, 1.55, 0.0)]
    P.append(loft('leg_r', 'rightleg', R['leg_r'], leg, -1.6, 0.0, rings=32))
    P.append(loft('leg_l', 'leftleg', R['leg_l'], leg, 1.6, 0.0, rings=32))
    for p in P: orient(p)
    return P

# Target per-group bounds from the cube rig (OBJ units), so ObjMesh pivots/scale stay identical.
TARGET = {
    'rightarm': ([-0.4906, 0.9095, -0.1469], [-0.2406, 1.6844, 0.1469]),
    'body': ([-0.2476, 0.7371, -0.1562], [0.2477, 1.6638, 0.1594]),
    'leftarm': ([0.2406, 0.9095, -0.1469], [0.4906, 1.6844, 0.1469]),
    'rightleg': ([-0.1954, 0.0, -0.0967], [0.0008, 0.75, 0.0968]),
    'leftleg': ([-0.0008, 0.0, -0.0967], [0.1954, 0.75, 0.0968]),
    'head': ([None, 1.6529, None], [None, 2.2941, None]),   # head: only vertical range is fitted
}

def fit(parts):
    by = {}
    for p in parts: by.setdefault(p.group, []).append(p)
    for g, ps in by.items():
        vs = [v for p in ps for v in p.V]
        lo = [min(v[k] for v in vs) for k in range(3)]; hi = [max(v[k] for v in vs) for k in range(3)]
        tlo, thi = TARGET[g]
        def m(v):
            o = []
            for k in range(3):
                if tlo[k] is None:
                    s = (thi[1] - tlo[1]) / (hi[1] - lo[1])   # uniform with vertical
                    o.append(v[k] / 16.0 * 16.0 * s + (0 if k == 0 else 0))
                else:
                    o.append(tlo[k] + (v[k] - lo[k]) / (hi[k] - lo[k]) * (thi[k] - tlo[k]))
            return tuple(o)
        for p in ps:
            p.V = [m(v) for v in p.V]

# ------------------------------------------------------------------ painting
SKIN, HAIR, IRIS, NONE = 'R', 'G', 'B', None
NAVY = (38, 46, 92); WHITE = (236, 236, 240); GOLD = (214, 170, 60)

def light(th, t, up=0.0):
    n = (math.sin(th), up, -math.cos(th)); L = norm([0.35, 0.55, 0.75])
    d = max(0.0, sum(a * b for a, b in zip(n, L)))
    rim = max(0.0, 1 - abs(-math.cos(th) * 0.0 + abs(math.sin(th)))) * 0.0
    return 0.62 + 0.42 * d

def paint(parts):
    base = Image.new('RGBA', (TEX, TEX), (0, 0, 0, 0)); mask = Image.new('RGBA', (TEX, TEX), (0, 0, 0, 0))
    bp, mp = base.load(), mask.load()
    def put(x, y, rgb, ch, a=255):
        bp[x, y] = (*[max(0, min(255, int(c))) for c in rgb], a)
        mp[x, y] = {SKIN: (255, 0, 0, 255), HAIR: (0, 255, 0, 255), IRIS: (0, 0, 255, 255), NONE: (0, 0, 0, 0)}[ch]
    for p in parts:
        u0, v0, uw, vh = p.rect
        for y in range(v0, v0 + vh):
            for x in range(u0, u0 + uw):
                fu = (x + 0.5 - u0) / uw; fv = (y + 0.5 - v0) / vh
                th = fu * 2 * math.pi; t = 1 - fv
                rgb, ch = shade_part(p.name, th, t)
                put(x, y, rgb, ch)
    return base, mask

def gray(v): return (v, v, v)
def line(d, w): return math.exp(-(d / w) ** 2)
def ang(th): return th - math.pi   # 0 at front center, +- toward sides

def shade_part(name, th, t):
    L = light(th, t)
    a = ang(th)
    if name == 'head':
        g = 205 * L
        # jaw and under-chin shadow, crown falloff
        g *= 1 - 0.25 * (1 - smooth(0.05, 0.2, t))
        # painted scalp under the coded hair: widow's peak in front, low at the back, jagged edge
        fa = abs(a) / math.pi
        hl = 0.70 + 0.06 * fa - 0.10 * math.exp(-(a / 0.18) ** 2) - 0.42 * smooth(0.45, 0.9, fa)
        hl += 0.025 * abs(math.sin(th * 9))
        if t > hl:
            hv = 120 + 50 * L / 1.04 - 25 * line(math.sin(th * 18), 0.3)
            return gray(hv * (0.8 if t < hl + 0.015 else 1.0)), HAIR
        if t > hl - 0.02: g *= 0.8
        if abs(a) < 1.3:
            # eyes
            for s in (-1, 1):
                ex, ey = s * 0.34, 0.52
                dx, dy = (a - ex) / 0.13, (t - ey) / 0.07
                r = dx * dx + dy * dy
                if r < 1.0:
                    ix, iy = (a - ex * 0.95) / 0.075, (t - ey + 0.004) / 0.075
                    ir = ix * ix + iy * iy
                    if ir < 0.3: return gray(15), NONE
                    if ((a - ex * 0.95 + 0.03) / 0.03) ** 2 + ((t - ey - 0.02) / 0.018) ** 2 < 1: return gray(255), NONE  # catchlight                       # pupil
                    if ir < 1.0: return gray(70 + 50 * max(0.0, -iy)), IRIS  # iris (eye color)
                    if dy > 0.6 or r > 0.8: return gray(25), NONE             # lid line / outline
                    return (248, 248, 250), NONE                                 # sclera
                # upper lid crease shadow
                if 1.0 <= r < 1.6 and dy > 0: g *= 0.82
                # brows: angled, thick, Saiyan scowl
                bx = (a - s * 0.38) / 0.24
                by = t - (0.615 - 0.03 * (abs(a) - 0.15) * 1.0 * (1 if abs(a) > 0.15 else 0))
                by = t - (0.60 + 0.045 * (abs(a) - 0.2))
                if abs(bx) < 1.0 and abs(by) < 0.02 + 0.006 * (1 - abs(bx)) and abs(a) > 0.08:
                    return gray(150 - 40 * abs(by) / 0.03), HAIR
                if abs(bx) < 1.1 and -0.035 < by < -0.02: g *= 0.8                # brow shadow
            # nose shading (nose bridge + side shadow)
            if 0.38 < t < 0.52: g *= 1 - 0.18 * line(abs(a) - 0.11, 0.04)
            # mouth
            if abs(a) < 0.22 and abs(t - 0.28) < 0.010: return gray(70), SKIN
            if abs(a) < 0.17 and -0.035 < t - 0.28 < -0.012: g *= 0.88            # lower lip shadow
            if 0.2 < abs(a) < 0.26 and abs(t - 0.285) < 0.02: g *= 0.8           # mouth corners
            # cheekbone highlight, nasolabial
            g *= 1 + 0.10 * line(abs(a) - 0.55, 0.12) * line(t - 0.45, 0.04)
            g *= 1 - 0.10 * line(abs(a) - 0.27, 0.03) * smooth(0.28, 0.32, t) * (1 - smooth(0.4, 0.44, t))
            g *= 1 - 0.12 * line(t - 0.18, 0.04) * line(a, 0.3)                    # chin dimple shadow
        # ears region darker sides / neck
        if t < 0.06: g *= 0.8
        return gray(g), SKIN
    if name in ('nose', 'ear_r', 'ear_l'):
        g = 205 * (0.8 + 0.25 * t)
        if name != 'nose' and 0.35 < t < 0.65 and abs(a) < 0.8: g *= 0.75        # ear canal
        return gray(g), SKIN
    if name == 'torso':
        g = 205 * L
        # pants: navy waist with gold belt line
        if t < 0.26:
            c = 0.75 + 0.35 * L / 1.04
            if t > 0.205: return tuple(v * c for v in GOLD) if t < 0.245 else gray(25), NONE
            fold = 1 - 0.12 * line(abs(a) - 0.9, 0.1)
            return tuple(v * c * fold for v in NAVY), NONE
        if t < 0.27: return gray(25), NONE
        f = abs(a)
        if f < 1.5:
            # pec lower edge curve, sternum, abs grid, obliques
            pec_edge = 0.66 - 0.06 * (f / 0.9) ** 2
            if f < 1.0 and abs(t - pec_edge) < 0.012: g *= 0.62
            elif f < 1.0 and 0 < t - pec_edge < 0.06: g *= 1 + 0.12 * (1 - (t - pec_edge) / 0.06)
            if f < 0.05 and 0.3 < t < 0.8: g *= 0.72
            if 0.3 < t < 0.6:
                if abs(f - 0.28) < 0.03: g *= 0.75
                for ay in (0.36, 0.45, 0.535):
                    if f < 0.28 and abs(t - ay) < 0.008: g *= 0.72
                if f < 0.28: g *= 1 + 0.08 * math.sin((t - 0.30) * math.pi / 0.09) ** 2
            if 0.28 < f < 0.6 and 0.3 < t < 0.5 and abs((t - 0.3) * 4 - (f - 0.28) * 3) < 0.06: g *= 0.8
        # back: spine, scapulae, traps
        else:
            b = math.pi - f
            if b < 0.06 and 0.3 < t < 0.9: g *= 0.75
            if abs(b - 0.6) < 0.03 and 0.62 < t < 0.85: g *= 0.8
        if t > 0.9: g *= 0.88
        return gray(g), SKIN
    if name.startswith('arm'):
        g = 205 * L
        if t < 0.215:                                   # white glove with cuff
            c = 0.72 + 0.32 * L / 1.04
            if t < 0.03: c *= 0.8
            if 0.17 < t < 0.215: c *= 0.92 if 0.19 < t < 0.205 else 1.0
            if t < 0.15 and abs(a) < 1.2 and 0.05 < t and abs((t * 30) % 1 - 0.5) < 0.08 and abs(a) < 0.9: c *= 0.8   # knuckles
            return tuple(v * c for v in WHITE), NONE
        if t < 0.225: return gray(40), NONE
        if 0.50 < t < 0.54: g *= 0.82                   # elbow crease
        if 0.62 < t < 0.78 and abs(a) < 0.9: g *= 1 + 0.08 * math.cos(a)  # bicep highlight
        if abs(t - 0.79) < 0.012: g *= 0.75             # delt line
        if t > 0.95: g *= 0.85
        return gray(g), SKIN
    if name.startswith('leg'):
        c = 0.75 + 0.35 * L / 1.04
        if t < 0.37:                                    # white boot, gold toe cap
            if t < 0.03: return gray(60), NONE          # sole
            if abs(a) < 0.9 and t < 0.12: return tuple(v * c for v in GOLD), NONE
            if 0.34 < t: return tuple(v * c * 0.9 for v in WHITE), NONE
            return tuple(v * c for v in WHITE), NONE
        if t < 0.385: return gray(30), NONE
        fold = 1 - 0.15 * line(((t * 14) % 1) - 0.5, 0.12) * line(abs(a) - 1.2, 0.4)
        return tuple(v * c * fold for v in NAVY), NONE
    return gray(200), SKIN

# ------------------------------------------------------------------ output
def vnormals(p):
    acc = [[0.0, 0.0, 0.0] for _ in p.V]
    pos = {}
    for i, v in enumerate(p.V): pos.setdefault(tuple(round(c, 5) for c in v), []).append(i)
    for f in p.F:
        a, b, c = (p.V[i] for i in f[:3])
        n = cross(sub(b, a), sub(c, a))
        for i in f:
            for k in range(3): acc[i][k] += n[k]
    for ids in pos.values():                      # weld seam normals
        s = [sum(acc[i][k] for i in ids) for k in range(3)]
        for i in ids: acc[i] = s
    return [norm(n) for n in acc]

def write_obj(parts, path):
    L = ['# generated by tools/gen_smooth_race.py (smooth lofted mesh, painted 512px atlas)']
    off = 1
    for g in ['rightarm', 'body', 'leftarm', 'rightleg', 'head', 'leftleg']:
        L.append('o ' + g)
        for p in [p for p in parts if p.group == g]:
            ns = vnormals(p)
            for v, uv, n in zip(p.V, p.UV, ns):
                L.append('v %.6f %.6f %.6f' % v); L.append('vt %.6f %.6f' % (uv[0] / TEX, 1 - uv[1] / TEX)); L.append('vn %.5f %.5f %.5f' % tuple(n))
            for f in p.F:
                L.append('f ' + ' '.join('%d/%d/%d' % (off + i, off + i, off + i) for i in f))
            off += len(p.V)
    open(path, 'w').write('\n'.join(L) + '\n')

def write_bbmodel(parts, base, path, race):
    buf = io.BytesIO(); base.save(buf, 'PNG')
    els, groups = [], {}
    for idx, p in enumerate(parts):
        uid = '%08x-0000-4000-8000-%012x' % (0x5a1a0000 + idx, idx)
        verts = {('v%d' % i): [x * 16 for x in v] for i, v in enumerate(p.V)}
        faces = {('f%d' % k): {'vertices': ['v%d' % i for i in f], 'uv': {('v%d' % i): list(p.UV[i]) for i in f}, 'texture': 0} for k, f in enumerate(p.F)}
        els.append({'name': p.name, 'type': 'mesh', 'uuid': uid, 'origin': [0, 0, 0], 'rotation': [0, 0, 0], 'vertices': verts, 'faces': faces,
                    'color': 0, 'export': True, 'visibility': True, 'locked': False})
        groups.setdefault(p.group, []).append(uid)
    outl = [{'name': g, 'uuid': '%08x-0000-4000-8000-0000000000%02x' % (0x6b000000 + i, i), 'origin': [0, 0, 0], 'children': ch, 'export': True, 'isOpen': True}
            for i, (g, ch) in enumerate(groups.items())]
    bb = {'meta': {'format_version': '4.10', 'model_format': 'free', 'box_uv': False}, 'name': race + '_smooth',
          'resolution': {'width': TEX, 'height': TEX}, 'elements': els, 'outliner': outl,
          'textures': [{'name': race + '_base.png', 'id': '0', 'width': TEX, 'height': TEX, 'uv_width': TEX, 'uv_height': TEX,
                        'source': 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()}]}
    json.dump(bb, open(path, 'w'))

if __name__ == '__main__':
    race = sys.argv[1] if len(sys.argv) > 1 else 'saiyan'
    parts = build(); fit(parts)
    base, mask = paint(parts)
    base.save(RES / 'textures/entity/player' / (race + '_base.png')); mask.save(RES / 'textures/entity/player' / (race + '_mask.png'))
    write_obj(parts, RES / 'models/3d' / (race + '.obj'))
    write_bbmodel(parts, base, ROOT / 'assets/blockbench/races' / (race + '.bbmodel'), race)
    print('parts', len(parts), 'faces', sum(len(p.F) for p in parts))
