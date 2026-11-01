"""Render a DBA race bbmodel to a FRONT / SIDE / BACK sheet PNG, Blockbench-style.

Reads assets/blockbench/races/<race>.bbmodel (cubes, rotations, UVs, group
visibility) plus the painted <race>_base.png / <race>_mask.png from resources,
applies the same mask tint math as DynamicSkinManager, and rasterizes three
orthographic views with a z-buffer and simple directional shading.

Usage:
  python3 tools/render_obj.py <race> [--form base|ssj|ssj2|ssj3|custom]
      [--skin RRGGBB] [--hair RRGGBB] [--eye RRGGBB] [--hairform RRGGBB]
      [--out path.png] [--scale N]
Form handling: ssj tints hair gold; ssj2 shows the hair_ssj2 group in gold;
ssj3 shows the hair_ssj3 group in gold. Tailed races recolor the tail preview
with the form hair color, matching the in-game TrailingTailLayer fix.
"""
import json, math, sys, pathlib
import numpy as np
from PIL import Image, ImageDraw

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / 'src/main/resources/assets/dragonblockarcanedba'
BBDIR = ROOT / 'assets/blockbench/races'

def rot(p, o, r):
    x, y, z = p[0] - o[0], p[1] - o[1], p[2] - o[2]
    ax, ay, az = (math.radians(a) for a in r)
    y, z = y * math.cos(ax) - z * math.sin(ax), y * math.sin(ax) + z * math.cos(ax)
    x, z = x * math.cos(ay) + z * math.sin(ay), -x * math.sin(ay) + z * math.cos(ay)
    x, y = x * math.cos(az) - y * math.sin(az), x * math.sin(az) + y * math.cos(az)
    return (x + o[0], y + o[1], z + o[2])

FACES = {  # name -> corner builder in Blockbench space, TL TR BR BL from outside
    'north': lambda x0, y0, z0, x1, y1, z1: [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
    'south': lambda x0, y0, z0, x1, y1, z1: [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
    'east':  lambda x0, y0, z0, x1, y1, z1: [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
    'west':  lambda x0, y0, z0, x1, y1, z1: [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
    'up':    lambda x0, y0, z0, x1, y1, z1: [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
    'down':  lambda x0, y0, z0, x1, y1, z1: [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)],
}
NORM = {'north': (0, 0, -1), 'south': (0, 0, 1), 'east': (1, 0, 0), 'west': (-1, 0, 0), 'up': (0, 1, 0), 'down': (0, -1, 0)}

def hex2rgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))

def tint_texture(base, mask, skin, hair, eye):
    b = np.asarray(base).astype(np.float32)
    m = np.asarray(mask).astype(np.int32)
    out = b.copy()
    lum = (0.299 * b[:, :, 0] + 0.587 * b[:, :, 1] + 0.114 * b[:, :, 2])
    for ch, col, div in ((0, skin, 215.0), (1, hair, 235.0), (2, eye, 255.0)):
        sel = m[:, :, ch] > 128
        l = np.clip(lum / div, 0, 1.25)
        for k in range(3):
            out[:, :, k] = np.where(sel, np.clip(col[k] * l, 0, 255), out[:, :, k])
    out[:, :, 3] = b[:, :, 3]
    return out

def collect_faces(race, form):
    bb = json.load(open(BBDIR / (race + '.bbmodel')))
    els = {e['uuid']: e for e in bb['elements']}
    bone_of = {}
    hidden_bones = set()
    def walk(node, bone, hidden):
        if isinstance(node, str):
            bone_of[node] = (bone, hidden)
            return
        name = node.get('name')
        h = hidden or (node.get('visibility') is False)
        if h: hidden_bones.add(name)
        for ch in node.get('children', []):
            walk(ch, name, h)
    for n in bb['outliner']: walk(n, None, False)
    # form visibility overrides (these win over the bbmodel visibility flags)
    def bone_hidden(bone):
        if bone in ('hair', 'hair_ssj2', 'hair_ssj3'):
            if form == 'ssj2': return bone != 'hair_ssj2'
            if form == 'ssj3': return bone != 'hair_ssj3'
            return bone != 'hair'
        return bone in hidden_bones
    faces = []
    TW, TH = bb['resolution']['width'], bb['resolution']['height']
    for uid, e in els.items():
        bone, hidden = bone_of.get(uid, (None, False))
        if bone_hidden(bone): continue
        (x0, y0, z0), (x1, y1, z1) = e['from'], e['to']
        o, r = e.get('origin', [0, 0, 0]), e.get('rotation', [0, 0, 0])
        for fn, fb in e.get('faces', {}).items():
            if fb.get('texture') is None: continue
            quad = [rot(p, o, r) for p in FACES[fn](x0, y0, z0, x1, y1, z1)]
            n = rot(NORM[fn], (0, 0, 0), r)
            u0, v0, u1, v1 = fb['uv']
            uvs = [(u0 / TW, v0 / TH), (u1 / TW, v0 / TH), (u1 / TW, v1 / TH), (u0 / TW, v1 / TH)]
            faces.append((quad, uvs, n, bone, e['name']))
    return faces

VIEWS = {
    # name: (screen x source, depth source) in Blockbench coords; depth larger = closer
    'FRONT': (lambda p: (-p[0], p[1]), lambda p: -p[2]),
    'SIDE':  (lambda p: (p[2], p[1]), lambda p: p[0]),
    'BACK':  (lambda p: (p[0], p[1]), lambda p: p[2]),
}
LIGHT = {
    'FRONT': np.array([0.35, 0.55, -0.75]),
    'SIDE':  np.array([0.75, 0.55, -0.35]),
    'BACK':  np.array([-0.35, 0.55, 0.75]),
}

def render_view(faces, tex, view, W, H, bounds, tail_recolor=None):
    img = np.zeros((H, W, 4), dtype=np.float32)
    zbuf = np.full((H, W), -1e9, dtype=np.float32)
    (minx, maxx, miny, maxy) = bounds
    sc = min((W - 30) / (maxx - minx), (H - 30) / (maxy - miny))
    cx, cy = (minx + maxx) / 2, (miny + maxy) / 2
    L = LIGHT[view]; L = L / np.linalg.norm(L)
    TH, TW = tex.shape[0], tex.shape[1]
    proj, dep = VIEWS[view]
    tris = []
    for quad, uvs, n, bone, name in faces:
        pts = [proj(p) for p in quad]
        dps = [dep(p) for p in quad]
        for tri in ((0, 1, 2), (0, 2, 3)):
            tris.append(([pts[i] for i in tri], [uvs[i] for i in tri], [dps[i] for i in tri], n, bone, name))
    for pts, uvs, dps, n, bone, name in tris:
        nrm = np.array(n, dtype=np.float32)
        shade = 0.62 + 0.38 * max(0.0, float(np.dot(nrm, L)))
        if abs(nrm[1]) > 0.9: shade = 0.95 if nrm[1] > 0 else 0.55
        sx = [(p[0] - cx) * sc + W / 2 for p in pts]
        sy = [H / 2 - (p[1] - cy) * sc for p in pts]
        x0, x1 = max(0, int(min(sx))), min(W - 1, int(max(sx)) + 1)
        y0, y1 = max(0, int(min(sy))), min(H - 1, int(max(sy)) + 1)
        if x1 <= x0 or y1 <= y0: continue
        (ax, ay), (bx, by), (ccx, ccy) = (sx[0], sy[0]), (sx[1], sy[1]), (sx[2], sy[2])
        den = (by - ccy) * (ax - ccx) + (ccx - bx) * (ay - ccy)
        if abs(den) < 1e-9: continue
        ys, xs = np.mgrid[y0:y1 + 1, x0:x1 + 1].astype(np.float32)
        w0 = ((by - ccy) * (xs - ccx) + (ccx - bx) * (ys - ccy)) / den
        w1 = ((ccy - ay) * (xs - ccx) + (ax - ccx) * (ys - ccy)) / den
        w2 = 1 - w0 - w1
        inside = (w0 >= -0.001) & (w1 >= -0.001) & (w2 >= -0.001)
        if not inside.any(): continue
        d = w0 * dps[0] + w1 * dps[1] + w2 * dps[2]
        u = (w0 * uvs[0][0] + w1 * uvs[1][0] + w2 * uvs[2][0]) * (TW - 1)
        v = (w0 * uvs[0][1] + w1 * uvs[1][1] + w2 * uvs[2][1]) * (TH - 1)
        # clamp into this face's own UV rect (half-texel inset) so edge pixels
        # never bleed a neighboring packed face's texels
        fu = [uv[0] * (TW - 1) for uv in uvs]; fv = [uv[1] * (TH - 1) for uv in uvs]
        u = np.clip(u, min(fu) + 0.55, max(fu) - 0.55)
        v = np.clip(v, min(fv) + 0.55, max(fv) - 0.55)
        sub_z = zbuf[y0:y1 + 1, x0:x1 + 1]
        upd = inside & (d > sub_z)
        if not upd.any(): continue
        ui = np.clip(u.astype(np.int32), 0, TW - 1); vi = np.clip(v.astype(np.int32), 0, TH - 1)
        col = tex[vi, ui].copy()
        if tail_recolor is not None and bone == 'tail':
            lum = (0.299 * col[:, :, 0] + 0.587 * col[:, :, 1] + 0.114 * col[:, :, 2]) / 165.0
            lum = np.clip(lum, 0, 1.25)
            for k in range(3):
                col[:, :, k] = np.clip(tail_recolor[k] * lum, 0, 255)
        col[:, :, :3] *= shade
        sub = img[y0:y1 + 1, x0:x1 + 1]
        sub[upd] = col[upd]
        img[y0:y1 + 1, x0:x1 + 1] = sub
        sub_z[upd] = d[upd]
        zbuf[y0:y1 + 1, x0:x1 + 1] = sub_z
    return img

def main():
    args = sys.argv[1:]
    race = args[0]
    opt = {}
    for i in range(1, len(args) - 1, 2):
        opt[args[i].lstrip('-')] = args[i + 1]
    form = opt.get('form', 'base')
    skin = hex2rgb(opt.get('skin', 'F0B898'))
    hair = hex2rgb(opt.get('hair', '18181C'))
    eye = hex2rgb(opt.get('eye', '282830'))
    if form in ('ssj', 'ssj2', 'ssj3'):
        hair = hex2rgb(opt.get('hairform', 'FFD700'))
    base = Image.open(RES / ('textures/entity/player/%s_base.png' % race)).convert('RGBA')
    mask = Image.open(RES / ('textures/entity/player/%s_mask.png' % race)).convert('RGBA')
    tex = tint_texture(base, mask, skin, hair, eye)
    faces = collect_faces(race, form)
    allpts = [p for q, _, _, _, _ in faces for p in q]
    xs = [p[0] for p in allpts]; ys = [p[1] for p in allpts]; zs = [p[2] for p in allpts]
    PW, PH = int(opt.get('pw', 420)), int(opt.get('ph', 560))
    panels = []
    bg = (43, 46, 54)
    for view in ('FRONT', 'SIDE', 'BACK'):
        if view == 'SIDE':
            b = (min(zs) - 1, max(zs) + 1, min(ys) - 1, max(ys) + 1)
        else:
            b = (min(xs) - 1, max(xs) + 1, min(ys) - 1, max(ys) + 1)
        tail_col = hair if (form != 'base') else None
        img = render_view(faces, tex, view, PW, PH, b, tail_recolor=tail_col)
        panels.append((view, img))
    sheet = Image.new('RGBA', (PW * 3 + 16, PH + 34), bg + (255,))
    dr = ImageDraw.Draw(sheet)
    for i, (view, img) in enumerate(panels):
        x = 4 + i * (PW + 4)
        sheet.alpha_composite(Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), 'RGBA'), (x, 30))
        dr.text((x + PW / 2 - 18, 10), view, fill=(235, 238, 245, 255))
    out = opt.get('out', str(ROOT / 'renders' / ('%s_%s.png' % (race, form))))
    pathlib.Path(out).parent.mkdir(parents=True, exist_ok=True)
    sheet.convert('RGB').save(out)
    print('wrote', out)

if __name__ == '__main__':
    main()
