"""Shared cube-model builder for DBA races, generalized from tools/gen_blocky_saiyan.py.

Authoring space is Blockbench's: px units, y up, character faces north (-z), the
character's right side is +x. Player frame is 32px tall (legs 0-12, torso 12-24,
head 24-32), though races may shift the split (the runtime auto-derives pivots
and scale from the body group bounds in ObjMesh).

A race spec builds cubes with cube()/mirror()/spike(), supplies paint_texel(),
then calls finish() which:
  - packs face UVs into a 512x512 atlas (2 texels per model px)
  - paints <race>_base.png and <race>_mask.png (R = skin tint, G = hair tint, B = eye tint)
  - paints <race>_blink.png / <race>_blink_mask.png, the same atlas with only the
    face's own eye texels replaced by that race's closed eyelid
  - exports models/3d/<race>.obj with the render groups the client knows
  - writes assets/blockbench/races/<race>.bbmodel (Bedrock project, dba_exporter tagged)
"""
import json, zlib, math, pathlib, base64, io, uuid, time
from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / 'src/main/resources/assets/dragonblockarcanedba'
BBDIR = ROOT / 'assets/blockbench/races'
TEX = 512
D = 2          # texels per model pixel (crisp pixel-art look)

MASKC = {'R': (255, 0, 0, 255), 'G': (0, 255, 0, 255), 'B': (0, 0, 255, 255), None: (0, 0, 0, 0)}

def clamp(v): return max(0, min(255, int(round(v))))

def save_png(img, path, tries=8):
    """The live client briefly locks textures it has open. Retry the replace."""
    path = pathlib.Path(path)
    last = None
    for i in range(tries):
        try:
            img.save(path)
            return
        except OSError as e:
            last = e
            time.sleep(0.2 * (i + 1))
    raise last
def mul(c, k): return tuple(clamp(x * k) for x in c)
def h01(*a):
    n = 0
    for v in a: n = (n * 73856093) ^ int(v * 7919 + 1013)
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return (n & 0xFFFF) / 65535.0

def rot(p, o, r):
    x, y, z = p[0] - o[0], p[1] - o[1], p[2] - o[2]
    ax, ay, az = (math.radians(a) for a in r)
    y, z = y * math.cos(ax) - z * math.sin(ax), y * math.sin(ax) + z * math.cos(ax)
    x, z = x * math.cos(ay) + z * math.sin(ay), -x * math.sin(ay) + z * math.cos(ay)
    x, y = x * math.cos(az) - y * math.sin(az), x * math.sin(az) + y * math.cos(az)
    return (x + o[0], y + o[1], z + o[2])

def face_corners(c):
    (x0, y0, z0), (x1, y1, z1) = c['f'], c['t']
    return {  # TL, TR, BR, BL as seen from outside (Blockbench UV orientation)
        'north': [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
        'south': [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
        'east':  [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
        'west':  [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
        'up':    [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
        'down':  [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)],
    }
def dist(a, b): return math.sqrt(sum((a[i] - b[i]) ** 2 for i in range(3)))

def eye_tint(lum):
    """Grayscale eye texel. Mask B, so the player's eye color tints it. No baked hue."""
    v = clamp(lum)
    return (v, v, v), 'B'

def skin_lid(k, scale=0.72):
    """Eyelid fold in the face's own skin tint."""
    return (k * scale,) * 3, 'R'

GROUP = {'right_arm': 'rightarm', 'body': 'body', 'left_arm': 'leftarm',
         'right_leg': 'rightleg', 'head': 'head', 'left_leg': 'leftleg',
         'hair': 'hair', 'hair_ssj3': 'hair_ssj3', 'hair_ssj2': 'hair_ssj2'}
NORM = {'north': (0, 0, -1), 'south': (0, 0, 1), 'east': (1, 0, 0), 'west': (-1, 0, 0),
        'up': (0, 1, 0), 'down': (0, -1, 0)}
GROUP_ORDER = ['rightarm', 'body', 'leftarm', 'rightleg', 'head', 'hair', 'hair_ssj3', 'hair_ssj2', 'leftleg']
STD_PIV = {'body': [0, 24, 0], 'head': [0, 24, 0], 'hair': [0, 24, 0], 'hair_ssj3': [0, 24, 0],
           'hair_ssj2': [0, 24, 0], 'right_arm': [6.5, 22, 0], 'left_arm': [-6.5, 22, 0],
           'right_leg': [2.5, 12, 0], 'left_leg': [-2.5, 12, 0], 'tail': [0, 13.5, 3]}


class RaceModel:
    def __init__(self, race, paint_texel, extra_groups=None, pivots=None, ns='dba'):
        self.race = race
        self.paint_texel = paint_texel
        self.cubes = []
        self.group_map = dict(GROUP)
        if extra_groups: self.group_map.update(extra_groups)
        self.pivots = dict(STD_PIV)
        if pivots: self.pivots.update(pivots)
        self.ns = ns
        self.blink = False

    # ---------------------------------------------------------- authoring
    def cube(self, name, bone, mat, f, t, o=None, r=(0, 0, 0), export=True):
        self.cubes.append(dict(name=name, bone=bone, mat=mat, f=list(f), t=list(t),
                               o=list(o or [0, 0, 0]), r=list(r), export=export))

    def mirror(self, name, bone, mat, f, t, o=None, r=(0, 0, 0), **k):
        """Add a cube and its mirror across x=0 (rotations about Y and Z flip)."""
        self.cube(name + '_r', bone[0], mat, f, t, o, r, **k)
        f2 = [-t[0], f[1], f[2]]; t2 = [-f[0], t[1], t[2]]
        o2 = [-(o or [0, 0, 0])[0], (o or [0, 0, 0])[1], (o or [0, 0, 0])[2]]
        self.cube(name + '_l', bone[1], mat, f2, t2, o2, (r[0], -r[1], -r[2]), **k)

    def spike(self, name, base, L, w, rot_, both=True, bone='hair', mat='hair'):
        """Tapered spike: four stacked cubes growing along local +y from base, ending in a point."""
        segs = [(0.0, 0.34, 1.0), (0.28, 0.58, 0.64), (0.52, 0.80, 0.38), (0.74, 1.0, 0.16)]
        for i, (a, b, s) in enumerate(segs):
            ww = w * s
            f = [base[0] - ww / 2, base[1] + a * L, base[2] - ww / 2]
            t = [base[0] + ww / 2, base[1] + b * L, base[2] + ww / 2]
            if both: self.mirror('%s_%s%d' % (bone, name, i), (bone, bone), mat, f, t, base, rot_)
            else: self.cube('%s_%s%d' % (bone, name, i), bone, mat, f, t, base, rot_)

    def chain_cubes(self, name, bone, mat, pts, t0, t1, overlap=0.35):
        """Row of overlapping cubes along a 3D polyline, tapering t0 -> t1 (antennae, tendrils, horns)."""
        import math as m
        def sub(a, b): return [a[i] - b[i] for i in range(3)]
        def norm(a):
            l = m.sqrt(sum(x * x for x in a)) or 1.0
            return [x / l for x in a]
        n = len(pts) - 1
        for i in range(n):
            a, b = list(pts[i]), list(pts[i + 1])
            d = norm(sub(b, a))
            a = [a[k] - d[k] * overlap for k in range(3)]
            b = [b[k] + d[k] * overlap for k in range(3)]
            ta = t0 + (t1 - t0) * i / n; tb = t0 + (t1 - t0) * (i + 1) / n
            t = (ta + tb) / 2
            c = [(a[k] + b[k]) / 2 for k in range(3)]
            ln = dist(a, b)
            # orient the segment: pitch about X from its Y/Z direction, yaw about Y from X/Z
            pitch = -m.degrees(m.atan2(d[2], d[1])) if abs(d[1]) > 1e-6 or abs(d[2]) > 1e-6 else 0.0
            f = [c[0] - t / 2, c[1] - ln / 2, c[2] - t / 2]
            tt = [c[0] + t / 2, c[1] + ln / 2, c[2] + t / 2]
            self.cube('%s_%d' % (name, i), bone, mat, f, tt, c, (pitch, 0, 0))

    # ---------------------------------------------------------- build
    def finish(self, script_name):
        cubes = self.cubes
        # UV packing
        class Packer:
            def __init__(s): s.x = s.y = s.row = 0
            def take(s, w, h):
                if s.x + w > TEX: s.x, s.y, s.row = 0, s.y + s.row, 0
                r = (s.x, s.y); s.x += w; s.row = max(s.row, h)
                assert s.y + h <= TEX, 'atlas full'
                return r
        pk = Packer()
        order = sorted(range(len(cubes)), key=lambda i: -(cubes[i]['t'][1] - cubes[i]['f'][1]))
        for i in order:
            c = cubes[i]; c['uv'] = {}
            for fn, q in face_corners(c).items():
                w = max(1, round(dist(q[0], q[1]) * D)); h = max(1, round(dist(q[0], q[3]) * D))
                u, v = pk.take(w, h)
                c['uv'][fn] = (u, v, u + w, v + h)
        def paint_atlas(blink):
            # Closed pass differs only where paint_face swaps the eye texels for the eyelid.
            self.blink = blink
            img = Image.new('RGBA', (TEX, TEX), (0, 0, 0, 0))
            msk = Image.new('RGBA', (TEX, TEX), (0, 0, 0, 0))
            ip, mp = img.load(), msk.load()
            for c in cubes:
                for fn, q in face_corners(c).items():
                    u0, v0, u1, v1 = c['uv'][fn]; W, H = u1 - u0, v1 - v0
                    for j in range(H):
                        for i in range(W):
                            s, t = (i + 0.5) / W, (j + 0.5) / H
                            top = [q[0][k] + (q[1][k] - q[0][k]) * s for k in range(3)]
                            bot = [q[3][k] + (q[2][k] - q[3][k]) * s for k in range(3)]
                            P = [top[k] + (bot[k] - top[k]) * t for k in range(3)]
                            rgb, ch = self.paint_texel(self, c, fn, s, t, W, H, i, j, P)
                            ip[u0 + i, v0 + j] = (clamp(rgb[0]), clamp(rgb[1]), clamp(rgb[2]), 255)
                            mp[u0 + i, v0 + j] = MASKC[ch]
            self.blink = False
            return img, msk
        base, mask = paint_atlas(False)
        blink, blink_mask = paint_atlas(True)
        # OBJ export
        order_groups = [g for g in GROUP_ORDER if g in set(self.group_map[b] for b in {c['bone'] for c in cubes} if b in self.group_map)]
        for g in self.group_map.values():
            if g not in order_groups: order_groups.append(g)
        L = ['# exported from assets/blockbench/races/%s.bbmodel by tools/%s' % (self.race, script_name)]
        vi = 1
        for g in order_groups:
            L.append('o ' + g)
            for c in cubes:
                if not c['export'] or self.group_map.get(c['bone']) != g: continue
                for fn, q in face_corners(c).items():
                    u0, v0, u1, v1 = c['uv'][fn]
                    uvs = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]
                    n = rot(NORM[fn], (0, 0, 0), c['r'])
                    pts = [rot(p, c['o'], c['r']) for p in q]
                    idx = []
                    for p, uv in list(zip(pts, uvs))[::-1]:        # CCW from outside
                        L.append('v %.6f %.6f %.6f' % (-p[0] / 16, p[1] / 16, -p[2] / 16))
                        L.append('vt %.6f %.6f' % (uv[0] / TEX, 1 - uv[1] / TEX))
                        L.append('vn %.5f %.5f %.5f' % (-n[0], n[1], -n[2]))
                        idx.append('%d/%d/%d' % (vi, vi, vi)); vi += 1
                    L.append('f ' + ' '.join(idx))
        (RES / ('models/3d/%s.obj' % self.race)).write_text('\n'.join(L) + '\n')
        save_png(base, RES / ('textures/entity/player/%s_base.png' % self.race))
        save_png(mask, RES / ('textures/entity/player/%s_mask.png' % self.race))
        save_png(blink, RES / ('textures/entity/player/%s_blink.png' % self.race))
        save_png(blink_mask, RES / ('textures/entity/player/%s_blink_mask.png' % self.race))
        # Blockbench project (Bedrock format)
        buf = io.BytesIO(); base.save(buf, 'PNG')
        def uid(s): return str(uuid.uuid5(uuid.NAMESPACE_URL, '%s-%s-%s' % (self.ns, self.race, s)))
        els = []
        for c in cubes:
            els.append({'name': c['name'], 'type': 'cube', 'uuid': uid(c['name']), 'from': c['f'], 'to': c['t'], 'origin': c['o'],
                        'rotation': c['r'], 'autouv': 0, 'color': 0, 'export': c['export'], 'box_uv': False, 'rescale': False, 'locked': False,
                        'faces': {fn: {'uv': list(c['uv'][fn]), 'texture': 0} for fn in c['uv']}})
        bones_present = []
        for c in cubes:
            if c['bone'] not in bones_present: bones_present.append(c['bone'])
        def bone(name, kids=()):
            return {'name': name, 'uuid': uid('bone-' + name), 'origin': self.pivots.get(name, [0, 24, 0]), 'rotation': [0, 0, 0],
                    'export': True, 'isOpen': True,
                    'children': [uid(c['name']) for c in cubes if c['bone'] == name] + list(kids)}
        head_kids = []
        for b in bones_present:
            if b.startswith('hair'):
                vis = None
                node = bone(b)
                if b in ('hair_ssj2', 'hair_ssj3'): node['visibility'] = False
                head_kids.append(node)
        top = [b for b in ('right_arm', 'left_arm', 'right_leg', 'left_leg', 'tail') if b in bones_present]
        extra = [bone(b) for b in bones_present if b not in
                 ('body', 'head', 'right_arm', 'left_arm', 'right_leg', 'left_leg', 'tail') and not b.startswith('hair')]
        outliner = [bone('body', [bone('head', head_kids)] + [bone(b) for b in top] + extra)]
        bb = {'meta': {'format_version': '4.10', 'model_format': 'bedrock', 'box_uv': False}, 'name': self.race,
              'dba_exporter': 'tools/' + script_name,
              'geometry_name': 'dba.' + self.race, 'visible_box': [3, 3.5, 1.25], 'resolution': {'width': TEX, 'height': TEX},
              'elements': els, 'outliner': outliner,
              'textures': [{'name': '%s_base.png' % self.race, 'id': '0', 'width': TEX, 'height': TEX, 'uv_width': TEX, 'uv_height': TEX,
                            'source': 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()}]}
        BBDIR.mkdir(parents=True, exist_ok=True)
        (BBDIR / ('%s.bbmodel' % self.race)).write_text(json.dumps(bb, indent=1))
        print(self.race, 'cubes', len(cubes), 'exported', sum(c['export'] for c in cubes), 'atlas rows used to', pk.y + pk.row)
        return base, mask
