"""Shared builder for the generated GeckoLib ability piece models.

Same output contract as tools/gen_time_shard.py (geo 1.12.0 with the
identifier geometry.dragonblockarcanedba.<name>, animations 1.10.0,
texture plus glowmask PNG at the declared size), generalized so each
piece is a short list of bones, cubes, and clip functions:

  - cubes name a material; the painter shades every face with its own
    value step, a vertical light gradient, bevelled edges, two scales of
    noise, and a per material surface (brushed steel, hammered iron,
    gold leaf, glassy obsidian, energy core to rim). Physical materials
    paint black into the glowmask (world lit); only energy glows.
  - UVs pack on shelves at the largest whole pixel scale that fits.
  - clips are functions of time sampled every step seconds, so each
    clip carries dense keyframes on several bones.
No em dashes anywhere in this file.
"""
import json
import math
import pathlib

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources/assets/dragonblockarcanedba"
FACES = ("north", "south", "east", "west", "up", "down")
FACE_STEP = {"up": 1.14, "north": 1.0, "east": 0.9, "west": 0.82,
             "south": 0.76, "down": 0.58}


def clamp(v):
    return max(0, min(255, int(round(v))))


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def hexc(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def hash01(x, y, s=0):
    n = (x * 73856093) ^ (y * 19349663) ^ (s * 83492791)
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return (n & 0xFFFF) / 65535.0


def smooth_noise(x, y, cell, s):
    """Value noise at a cell size, bilinear between lattice hashes."""
    fx, fy = x / cell, y / cell
    ix, iy = int(math.floor(fx)), int(math.floor(fy))
    tx, ty = fx - ix, fy - iy
    tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
    a = hash01(ix, iy, s)
    b = hash01(ix + 1, iy, s)
    c = hash01(ix, iy + 1, s)
    d = hash01(ix + 1, iy + 1, s)
    return (a * (1 - tx) + b * tx) * (1 - ty) + (c * (1 - tx) + d * tx) * ty


def r2(v):
    return round(v, 3)


class Model:
    def __init__(self, name, tex=256, bounds=3):
        self.name = name
        self.tex = tex
        self.bounds = bounds
        self.bones = []
        self.cubes = []
        self.mats = {}
        self.clips = {}

    # ------------------------------------------------------------ build
    def material(self, key, kind, dark, mid, light, glow=None):
        self.mats[key] = dict(kind=kind, dark=hexc(dark), mid=hexc(mid),
                              light=hexc(light),
                              glow=None if glow is None else tuple(hexc(g) for g in glow))

    def bone(self, name, parent=None, pivot=(0, 0, 0), rot=None):
        b = {"name": name, "pivot": [r2(v) for v in pivot]}
        if parent:
            b["parent"] = parent
        if rot:
            b["rotation"] = [r2(v) for v in rot]
        self.bones.append(b)
        return name

    def box(self, bone, center, size, mat, rot=None, pivot=None):
        """A cube by its center; rotation turns it about pivot (default center)."""
        origin = [center[i] - size[i] / 2.0 for i in range(3)]
        c = dict(bone=bone, origin=[r2(v) for v in origin],
                 size=[r2(max(0.05, v)) for v in size], mat=mat,
                 id="c%d" % len(self.cubes), seed=len(self.cubes) * 7 + 3)
        if rot and any(abs(v) > 1e-6 for v in rot):
            c["rotation"] = [r2(v) for v in rot]
            c["pivot"] = [r2(v) for v in (pivot if pivot else center)]
        self.cubes.append(c)
        return c

    def clip(self, name, length, loop, fns, step=0.05):
        """fns: {bone: {channel: f(t) -> [x, y, z]}}, sampled every step."""
        n = max(2, int(round(length / step)))
        bones = {}
        for bone, chans in fns.items():
            bones[bone] = {}
            for ch, f in chans.items():
                keys = {}
                for k in range(n + 1):
                    t = length * k / n
                    keys["%.3f" % t] = [r2(v) for v in f(t)]
                bones[bone][ch] = keys
        self.clips[name] = {"loop": loop, "animation_length": r2(length), "bones": bones}

    # ------------------------------------------------------------ uv
    def _pack(self, scale):
        rects = []
        for c in self.cubes:
            dx, dy, dz = c["size"]
            dims = {"north": (dx, dy), "south": (dx, dy), "east": (dz, dy),
                    "west": (dz, dy), "up": (dx, dz), "down": (dx, dz)}
            for f in FACES:
                w, h = dims[f]
                rects.append((c["id"], f, max(1, int(math.ceil(w * scale))),
                              max(1, int(math.ceil(h * scale)))))
        rects.sort(key=lambda r: (-r[3], -r[2]))
        placed = {}
        x = y = shelf = 0
        for cid, f, w, h in rects:
            if w > self.tex:
                return None
            if x + w > self.tex:
                x, y, shelf = 0, y + shelf, 0
            if y + h > self.tex:
                return None
            placed[(cid, f)] = (x, y, w, h)
            x += w
            shelf = max(shelf, h)
        return placed

    def pack(self):
        for scale in (8, 7, 6, 5, 4, 3, 2, 1):
            placed = self._pack(scale)
            if placed:
                self.uv_scale = scale
                for c in self.cubes:
                    c["uv"] = {f: placed[(c["id"], f)] for f in FACES}
                return
        raise SystemExit("atlas overflow for " + self.name)

    # ------------------------------------------------------------ paint
    def paint_px(self, c, face, nx, ny, gu, gv):
        m = self.mats[c["mat"]]
        kind = m["kind"]
        s = c["seed"]
        edge = min(nx, 1 - nx, ny, 1 - ny)
        fine = hash01(gu, gv, s)
        blot = smooth_noise(gu, gv, 5.0, s + 1)
        broad = smooth_noise(gu, gv, 14.0, s + 2)
        if kind == "energy":
            rim, mid, core = m["glow"]
            # core to rim: bright along the cube's long middle line
            d = abs(nx - 0.5) * 2.0 if face not in ("up", "down") else math.hypot(nx - 0.5, ny - 0.5) * 2.0
            d = min(1.0, d + (blot - 0.5) * 0.35)
            flick = 0.5 + 0.5 * math.sin((gv * 0.9 + gu * 0.35) + s)
            col = lerp(core, mid, d * 1.3)
            col = lerp(col, rim, max(0.0, d - 0.55) * 2.2)
            col = lerp(col, core, flick * 0.12 * (1 - d))
            col = tuple(v * (0.92 + fine * 0.12) for v in col)
            return col, col
        dark, mid, light = m["dark"], m["mid"], m["light"]
        step = FACE_STEP[face]
        t = 0.52 + (0.5 - ny) * 0.32 + (broad - 0.5) * 0.3
        if kind == "steel":
            streak = hash01(gu if face in ("up", "down") else gv, 0, s + 5)
            t += (streak - 0.5) * 0.22 + (fine - 0.5) * 0.06
            sheen = max(0.0, 1.0 - abs((nx - ny) * 0.9 - (broad - 0.5)) * 5.0)
            col = lerp(dark, mid, t * 1.5) if t < 0.66 else lerp(mid, light, (t - 0.66) * 3)
            col = lerp(col, light, sheen * 0.45)
        elif kind == "gold":
            t += (fine - 0.5) * 0.12
            col = lerp(dark, mid, t * 1.4) if t < 0.7 else lerp(mid, light, (t - 0.7) * 3.3)
            sheen = max(0.0, 1.0 - abs(nx + ny - 1.0 - (blot - 0.5)) * 4.0)
            col = lerp(col, light, sheen * 0.5)
        elif kind == "obsidian":
            t = 0.25 + (0.5 - ny) * 0.2 + (broad - 0.5) * 0.3 + (fine - 0.5) * 0.08
            col = lerp(dark, mid, t)
            band = max(0.0, 1.0 - abs((nx * 0.8 + ny * 0.6) - 0.35 - broad * 0.4) * 9.0)
            col = lerp(col, light, band * 0.55)
            crack = abs(smooth_noise(gu, gv, 3.0, s + 9) - 0.5) < 0.025
            if crack:
                col = lerp(col, (6, 4, 6), 0.7)
        else:  # iron: hammered, mottled
            t += (blot - 0.5) * 0.45 + (fine - 0.5) * 0.1
            col = lerp(dark, mid, t * 1.3) if t < 0.75 else lerp(mid, light, (t - 0.75) * 4)
            if fine > 0.965:
                col = lerp(col, light, 0.5)
            pit = smooth_noise(gu, gv, 2.2, s + 7)
            if pit < 0.12:
                col = lerp(col, dark, 0.6)
        # face value step
        if step < 1.0:
            col = lerp(col, dark, (1.0 - step) * 1.1)
        else:
            col = lerp(col, light, (step - 1.0) * 1.6)
        # bevel: lit top-left rim, shadowed bottom-right rim
        if edge < 0.09:
            if nx < 0.09 or ny < 0.09:
                col = lerp(col, light, 0.45)
            else:
                col = lerp(col, dark, 0.55)
        return col, (0, 0, 0)

    def paint(self):
        base = Image.new("RGBA", (self.tex, self.tex), (0, 0, 0, 0))
        glow = Image.new("RGBA", (self.tex, self.tex), (0, 0, 0, 0))
        bp, gp = base.load(), glow.load()
        for c in self.cubes:
            for f in FACES:
                u0, v0, w, h = c["uv"][f]
                for j in range(h):
                    for i in range(w):
                        b, g = self.paint_px(c, f, (i + 0.5) / w, (j + 0.5) / h, u0 + i, v0 + j)
                        bp[u0 + i, v0 + j] = tuple(clamp(v) for v in b) + (255,)
                        gp[u0 + i, v0 + j] = tuple(clamp(v) for v in g) + (255,)
        return base, glow

    # ------------------------------------------------------------ write
    def geo(self):
        bones = []
        for b in self.bones:
            nb = dict(b)
            cubes = []
            for c in self.cubes:
                if c["bone"] != b["name"]:
                    continue
                cube = {"origin": c["origin"], "size": c["size"]}
                if "rotation" in c:
                    cube["pivot"] = c["pivot"]
                    cube["rotation"] = c["rotation"]
                cube["uv"] = {f: {"uv": [c["uv"][f][0], c["uv"][f][1]],
                                  "uv_size": [c["uv"][f][2], c["uv"][f][3]]} for f in FACES}
                cubes.append(cube)
            if cubes:
                nb["cubes"] = cubes
            bones.append(nb)
        return {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": "geometry.dragonblockarcanedba." + self.name,
                            "texture_width": self.tex, "texture_height": self.tex,
                            "visible_bounds_width": self.bounds,
                            "visible_bounds_height": self.bounds,
                            "visible_bounds_offset": [0, 0.5, 0]},
            "bones": bones}]}

    def write(self):
        self.pack()
        base, glow = self.paint()
        paths = {
            "geo": RES / "geckolib/models/entity" / (self.name + ".geo.json"),
            "anim": RES / "geckolib/animations/entity" / (self.name + ".animation.json"),
            "tex": RES / "textures/entity" / (self.name + ".png"),
            "glow": RES / "textures/entity" / (self.name + "_glowmask.png"),
        }
        for p in paths.values():
            p.parent.mkdir(parents=True, exist_ok=True)
        paths["geo"].write_text(json.dumps(self.geo(), indent=1))
        paths["anim"].write_text(json.dumps(
            {"format_version": "1.10.0", "animations": self.clips}, indent=1))
        base.save(paths["tex"])
        glow.save(paths["glow"])
        rotated = sum(1 for c in self.cubes if "rotation" in c)
        colors = len(set(base.getdata()))
        keys = {n: sum(len(ch) for b in cl["bones"].values() for ch in b.values())
                for n, cl in self.clips.items()}
        print("%s: %d bones, %d cubes (%d rotated), tex %d at uv x%d, %d colors, keys %s"
              % (self.name, len(self.bones), len(self.cubes), rotated, self.tex,
                 self.uv_scale, colors, keys))


# ---------------------------------------------------------------- easing
def ease_out(t):
    t = max(0.0, min(1.0, t))
    return 1 - (1 - t) ** 3


def ease_in(t):
    t = max(0.0, min(1.0, t))
    return t ** 3


def overshoot(t, k=1.7):
    t = max(0.0, min(1.0, t)) - 1
    return 1 + t * t * ((k + 1) * t + k)


def wave(t, period, phase=0.0):
    return math.sin((t / period + phase) * math.pi * 2)
