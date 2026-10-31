"""Generate GeckoLib geo + animation json for Shenron and Porunga.

Geometry is transcribed from the old vanilla ModelPart layer definitions
(ShenronModel/PorungaModel). Keyframes are sampled from the old procedural
setupAnim math so the GeckoLib version moves the same way, plus new roar and
wish animations. Java model space (y down, x mirrored) is converted to
Bedrock space: pivot (-x, 24 - y, z), rotation (-rx, -ry, rz) in degrees.
Run from the repo root: python3 tools/gen_dragon_geo.py
"""
import json, math, os

OUT = "src/main/resources/assets/dragonblockarcanedba/geckolib"
D = math.degrees


class Part:
    def __init__(self, name, parent, tex, boxes, off=(0, 0, 0), rot=(0, 0, 0)):
        self.name, self.parent, self.tex, self.boxes = name, parent, tex, boxes
        self.off, self.rot = off, rot

    def abs(self, parts):
        p = parts[self.parent].abs(parts) if self.parent else (0, 0, 0)
        return tuple(a + b for a, b in zip(p, self.off))


def geo(ident, parts, tw, th):
    pm = {p.name: p for p in parts}
    bones = []
    for p in parts:
        P = p.abs(pm)
        cubes = []
        for b in p.boxes:
            (bx, by, bz, sx, sy, sz), infl = b[:6], (b[6] if len(b) > 6 else 0)
            ax, ay, az = P[0] + bx, P[1] + by, P[2] + bz
            c = {"origin": [-(ax + sx), 24 - (ay + sy), az], "size": [sx, sy, sz], "uv": list(b[7] if len(b) > 7 else p.tex)}
            if infl:
                c["inflate"] = infl
            cubes.append(c)
        bone = {"name": p.name, "pivot": [-P[0], 24 - P[1], P[2]]}
        if p.parent:
            bone["parent"] = p.parent
        if any(p.rot):
            bone["rotation"] = [r(-D(p.rot[0])), r(-D(p.rot[1])), r(D(p.rot[2]))]
        if cubes:
            bone["cubes"] = cubes
        bones.append(bone)
    return {"format_version": "1.12.0", "minecraft:geometry": [{"description": {
        "identifier": "geometry." + ident, "texture_width": tw, "texture_height": th,
        "visible_bounds_width": 16, "visible_bounds_height": 24, "visible_bounds_offset": [0, 8, 0]},
        "bones": bones}]}


def r(v):
    return round(v, 3)


class Anim:
    """Collects sampled java-space poses and converts them to keyframe deltas."""

    def __init__(self, parts, length, loop):
        self.pm = {p.name: p for p in parts}
        self.length, self.loop = length, loop
        self.bones = {}

    def key(self, t, bone, rot=None, pos=None, scale=None):
        p = self.pm[bone]
        b = self.bones.setdefault(bone, {})
        ts = "%.2f" % t
        if rot is not None:
            d = [rot[i] - p.rot[i] for i in range(3)]
            b.setdefault("rotation", {})[ts] = [-D(d[0]), -D(d[1]), D(d[2])]
        if pos is not None:
            d = [pos[i] - p.off[i] for i in range(3)]
            b.setdefault("position", {})[ts] = [r(-d[0]), r(-d[1]), r(d[2])]
        if scale is not None:
            b.setdefault("scale", {})[ts] = [r(s) for s in scale]

    def json(self):
        for b in self.bones.values():
            rot = b.get("rotation")
            if rot:  # unwrap angles so interpolation never spins the long way round
                prev = None
                for k in rot:
                    v = rot[k]
                    if prev:
                        v = [v[i] + 360 * round((prev[i] - v[i]) / 360) for i in range(3)]
                    rot[k] = v
                    prev = v
                for k in rot:
                    rot[k] = [r(x) for x in rot[k]]
        return {"loop": self.loop, "animation_length": self.length, "bones": self.bones}


def samples(length, step):
    n = int(round(length / step))
    return [i * step for i in range(n + 1)]


# ---------------------------------------------------------------- Shenron
NUM = 20
sh = [
    Part("head", None, (0, 0), [(-12, -12, -12, 24, 24, 24)], (0, -30, 0)),
    Part("snout", "head", (0, 64), [(-7, -4, -30, 14, 8, 18)]),
    Part("jaw", "head", (64, 64), [(-7, 4, -29, 14, 6, 18)], (0, 0, 0), (0.2, 0, 0)),
    Part("left_horn", "head", (128, 0), [(-1.5, -18, -1.5, 3, 18, 3), (-6, -12, -1.5, 6, 3, 3)], (-9, -12, -3), (-0.4, 0, -0.3)),
    Part("right_horn", "head", (128, 0), [(-1.5, -18, -1.5, 3, 18, 3), (0, -12, -1.5, 6, 3, 3)], (9, -12, -3), (-0.4, 0, 0.3)),
    Part("left_whisker", "head", (192, 0), [(-1, -1, -24, 2, 2, 24)], (-7, 2, -28), (0.4, -0.6, 0)),
    Part("right_whisker", "head", (192, 0), [(-1, -1, -24, 2, 2, 24)], (7, 2, -28), (0.4, 0.6, 0)),
]
for i in range(NUM):
    seg = "segment_%d" % i
    sh.append(Part(seg, None, (0, 128), [(-12, -12, -12, 24, 24, 24)]))
    if i < NUM - 1:
        sh.append(Part("mane_%d" % i, seg, (128, 64), [(-3, -20, -9, 6, 8, 18)]))
    if i == NUM - 1:
        for k, (z, inf) in enumerate([(10, -2), (27, -5), (38, -8), (43, -11)], 1):
            sh.append(Part("tail_%d" % k, seg, (0, 128), [(-12, -12, z, 24, 24, 24, inf)], (0, 0, 0), (-0.6, 0, 0)))
    if i in (2, 14):
        a, b = ("left_arm", "right_arm") if i == 2 else ("left_leg", "right_leg")
        s = 1 if i == 2 else -1
        sh.append(Part(a, seg, (128, 128), [(-24, -3, -3, 20, 6, 6), (-26, 0, -6, 8, 6, 12)], (-10, 4, 0), (0, 0.4 * s, -0.4)))
        sh.append(Part(b, seg, (128, 128), [(4, -3, -3, 20, 6, 6), (18, 0, -6, 8, 6, 12)], (10, 4, 0), (0, -0.4 * s, 0.4)))

TAU = 2 * math.pi
LOOP_T = 400  # ticks per serpent loop (20 s); all idle frequencies divide it evenly
W = TAU / LOOP_T


def shenron_pose(ticks, head_y=-152.0):
    """Segment poses from the old ShenronModel.setupAnim with loop-friendly frequencies."""
    spin = ticks * W
    idle = ticks * W * 4

    def ring(i):
        rad = 40.0
        if i < 4:
            rad = max(0, i) * 10.0
        elif i > 10:
            rad = 40.0 + (i - 10) * 3.0
        a = i * 0.45 + spin
        yp = max(0, 16 - i) / 16.0
        y = -12.0 - 120.0 * yp * yp + math.sin(idle + i * 0.5) * 1.5
        return math.sin(a) * rad, y, math.cos(a) * rad

    out = {}
    for i in range(NUM):
        x, y, z = ring(i)
        px, py, pz = (0.0, head_y, -10.0) if i == 0 else ring(i - 1)
        dx, dy, dz = px - x, py - y, pz - z
        yaw = math.atan2(dx, dz) + math.pi
        h = math.hypot(dx, dz)
        pitch = math.atan2(dy, h) if h > 1e-3 else math.pi / 2
        out["segment_%d" % i] = ((x, y, z), (pitch, yaw, 0))
    return out, idle


def build_shenron():
    anims = {}
    # serpent: the coiled body slowly spirals and breathes (20 s loop)
    a = Anim(sh, LOOP_T / 20.0, True)
    for t in samples(a.length, 0.25):
        segs, idle = shenron_pose(t * 20)
        for n, (pos, rot) in segs.items():
            a.key(t, n, rot=rot, pos=pos)
        for n, ph in (("left_arm", 0), ("right_arm", math.pi)):
            p = a.pm[n].rot
            a.key(t, n, rot=(math.sin(idle + ph) * 0.2, p[1], p[2]))
        for n, ph in (("left_leg", 0), ("right_leg", math.pi)):
            p = a.pm[n].rot
            a.key(t, n, rot=(math.cos(idle + ph) * 0.2, p[1], p[2]))
    anims["serpent"] = a.json()

    # idle: head bob and sway, breathing jaw, whiskers in the wind (5 s loop)
    a = Anim(sh, 5.0, True)
    for t in samples(5.0, 0.25):
        w = TAU * t / 5.0
        a.key(t, "head", pos=(0, -152 + math.sin(w) * 3, -10), rot=(0.2 + math.sin(w) * 0.05, math.sin(w) * 0.06, 0))
        a.key(t, "jaw", rot=(0.1 + math.sin(w * 2) * 0.1, 0, 0))
        a.key(t, "left_whisker", rot=(0.4 + math.sin(w * 2) * 0.2, -0.6, 0))
        a.key(t, "right_whisker", rot=(0.4 - math.sin(w * 2) * 0.2, 0.6, 0))
    anims["idle"] = a.json()

    # roar: rear back, jaw wide, whiskers flare, then settle (3 s, once)
    a = Anim(sh, 3.0, False)
    for t, lift, pitch, jaw, flare, shake in [
            (0, 0, 0.2, 0.1, 0, 0), (0.6, 14, -0.45, 0.25, 0.3, 0), (0.9, 18, -0.6, 1.0, 0.6, 0.06),
            (1.3, 18, -0.55, 1.05, 0.65, -0.06), (1.7, 18, -0.6, 1.0, 0.6, 0.06), (2.1, 16, -0.5, 0.9, 0.5, 0),
            (3.0, 0, 0.2, 0.1, 0, 0)]:
        a.key(t, "head", pos=(0, -152 - lift, -10 + lift * 0.5), rot=(pitch, shake, 0))
        a.key(t, "jaw", rot=(jaw, 0, 0))
        a.key(t, "left_whisker", rot=(0.4 - flare, -0.6 - flare, 0))
        a.key(t, "right_whisker", rot=(0.4 - flare, 0.6 + flare, 0))
        a.key(t, "left_horn", rot=(-0.4 - flare * 0.3, 0, -0.3))
        a.key(t, "right_horn", rot=(-0.4 - flare * 0.3, 0, 0.3))
    anims["roar"] = a.json()

    # wish: lower the head toward the summoner and speak (4 s loop)
    a = Anim(sh, 4.0, True)
    for t in samples(4.0, 0.25):
        w = TAU * t / 4.0
        a.key(t, "head", pos=(0, -140 + math.sin(w) * 2, -22), rot=(0.55 + math.sin(w) * 0.04, math.sin(w * 0.5) * 0.04, 0))
        a.key(t, "jaw", rot=(0.15 + abs(math.sin(w * 3)) * 0.35, 0, 0))
        a.key(t, "left_whisker", rot=(0.1 + math.sin(w) * 0.15, -0.9, 0))
        a.key(t, "right_whisker", rot=(0.1 - math.sin(w) * 0.15, 0.9, 0))
    anims["wish"] = a.json()
    return anims


# ---------------------------------------------------------------- Porunga
def claws(side):
    h = side + "_hand"
    return [Part(side + "_claw_%d" % (k + 1), h, (128, 0), [(-1, 0, -1, 2, 6, 2)], o, rr) for k, (o, rr) in enumerate([
        ((-2, 10, -2), (0.2, 0, 0.2)), ((2, 10, -2), (0.2, 0, -0.2)), ((0, 10, 2), (0.2, 0, 0))])]


po = [
    Part("upper_torso", None, (0, 0), [(-24, -24, -12, 48, 24, 24)], (0, -60, 0)),
    Part("left_shoulder_spike", "upper_torso", (128, 0), [(-2, -10, -2, 4, 10, 4)], (-14, -24, 0), (0, 0, -0.6)),
    Part("right_shoulder_spike", "upper_torso", (128, 0), [(-2, -10, -2, 4, 10, 4)], (14, -24, 0), (0, 0, 0.6)),
    Part("back_fin", "upper_torso", (150, 0), [(-1, -8, 10, 2, 24, 12)]),
    Part("head", "upper_torso", (0, 48), [(-10, -10, -16, 20, 20, 20)], (0, -28, -6)),
    Part("snout", "head", (80, 48), [(-8, -2, -24, 16, 10, 8)]),
    Part("jaw", "head", (80, 68), [(-8, 2, -23, 16, 6, 10)], (0, 6, 0), (0.2, 0, 0)),
    Part("left_horn", "head", (128, 0), [(-2, -14, -2, 4, 14, 4)], (-6, -8, -8), (-0.6, 0, -0.2)),
    Part("right_horn", "head", (128, 0), [(-2, -14, -2, 4, 14, 4)], (6, -8, -8), (-0.6, 0, 0.2)),
    Part("left_cheek_spike", "head", (128, 0), [(-2, -2, -8, 4, 4, 8)], (-10, 2, -8), (0, -0.6, 0)),
    Part("right_cheek_spike", "head", (128, 0), [(-2, -2, -8, 4, 4, 8)], (10, 2, -8), (0, 0.6, 0)),
    Part("lower_torso", None, (0, 88), [(-16, 0, -10, 32, 24, 20)], (0, -60, 0)),
    Part("left_arm", "upper_torso", (100, 100), [(-8, -4, -6, 12, 16, 12)], (-28, -12, 0), (0, 0, 0.2)),
    Part("left_arm_spike", "left_arm", (128, 0), [(-2, -12, -2, 4, 12, 4)], (-6, 2, 0), (0, 0, -1.2)),
    Part("left_forearm", "left_arm", (100, 130), [(-6, 0, -5, 10, 16, 10)], (-1, 12, 0), (-0.4, 0, 0)),
    Part("left_hand", "left_forearm", (100, 160), [(-4, 0, -4, 8, 10, 8)], (0, 16, 0), (0.2, 0, 0)),
] + claws("left") + [
    Part("right_arm", "upper_torso", (100, 100), [(-4, -4, -6, 12, 16, 12)], (28, -12, 0), (0, 0, -0.2)),
    Part("right_arm_spike", "right_arm", (128, 0), [(-2, -12, -2, 4, 12, 4)], (6, 2, 0), (0, 0, 1.2)),
    Part("right_forearm", "right_arm", (100, 130), [(-4, 0, -5, 10, 16, 10)], (1, 12, 0), (-0.4, 0, 0)),
    Part("right_hand", "right_forearm", (100, 160), [(-4, 0, -4, 8, 10, 8)], (0, 16, 0), (0.2, 0, 0)),
] + claws("right")
parent, yo = "lower_torso", 24.0
for i, size in enumerate([30, 27, 24, 21, 18, 15, 12, 9, 6, 3]):
    po.append(Part("tail_%d" % i, parent, (0, 150 + i * 20), [(-size / 2, 0, -size / 2, size, 20, size)], (0, yo, 0)))
    parent, yo = "tail_%d" % i, 20.0

ARMS_REST = {  # the hunched, arms-crossed pose from the old setupAnim
    "left_arm": (-0.3, 0.2, -0.1), "left_forearm": (-0.6, 0.6, 0.4),
    "right_arm": (-0.3, -0.2, 0.1), "right_forearm": (-0.6, -0.6, -0.4)}


def build_porunga():
    anims = {}
    # serpent: hover, hunch and the coiled tail undulating (6 s loop)
    a = Anim(po, 6.0, True)
    for t in samples(6.0, 0.25):
        w = TAU * t / 6.0
        hover = -250 + math.sin(w) * 4
        a.key(t, "upper_torso", pos=(0, hover, 0), rot=(0.6 + math.sin(w) * 0.02, 0, 0), scale=(3, 3, 3))
        a.key(t, "lower_torso", pos=(0, hover, 0), rot=(0.4 + math.sin(w) * 0.02, 0, 0), scale=(2.8, 2.2, 2.8))
        for i in range(10):
            cx, cy = (-0.1, 0.0) if i <= 1 else ((-0.5, 0.4) if i <= 5 else (-0.6, 0.6))
            a.key(t, "tail_%d" % i, rot=(cx + math.sin(w + i * 0.6) * 0.06, cy + math.cos(w + i * 0.6) * 0.08, 0.1),
                  scale=(1, 1, 0.666) if i == 0 else None)
    anims["serpent"] = a.json()

    # idle: breathing jaw, slight head sway, arms crossed (4 s loop)
    a = Anim(po, 4.0, True)
    for t in samples(4.0, 0.25):
        w = TAU * t / 4.0
        a.key(t, "head", rot=(-0.6, math.sin(w) * 0.05, 0))
        a.key(t, "jaw", rot=(0.1 + math.sin(w * 2) * 0.1, 0, 0))
        for n, v in ARMS_REST.items():
            a.key(t, n, rot=v)
    anims["idle"] = a.json()

    # roar: throw the arms wide, head back, jaw open (3 s, once)
    a = Anim(po, 3.0, False)
    for t, k in [(0, 0), (0.6, 1), (1.0, 1.1), (1.4, 1), (1.8, 1.1), (2.2, 1), (3.0, 0)]:
        a.key(t, "head", rot=(-0.6 - 0.5 * k, 0, 0))
        a.key(t, "jaw", rot=(0.1 + 0.8 * k, 0, 0))
        a.key(t, "left_arm", rot=(-0.3 - 1.2 * k, 0.2, -0.1 + 1.0 * k))
        a.key(t, "right_arm", rot=(-0.3 - 1.2 * k, -0.2, 0.1 - 1.0 * k))
        a.key(t, "left_forearm", rot=(-0.6 + 0.4 * k, 0.6 - 0.6 * k, 0.4 - 0.4 * k))
        a.key(t, "right_forearm", rot=(-0.6 + 0.4 * k, -0.6 + 0.6 * k, -0.4 + 0.4 * k))
    anims["roar"] = a.json()

    # wish: lean in, open hands toward the summoner and speak (4 s loop)
    a = Anim(po, 4.0, True)
    for t in samples(4.0, 0.25):
        w = TAU * t / 4.0
        a.key(t, "head", rot=(-0.3 + math.sin(w) * 0.04, 0, 0))
        a.key(t, "jaw", rot=(0.15 + abs(math.sin(w * 3)) * 0.35, 0, 0))
        a.key(t, "left_arm", rot=(-1.0 + math.sin(w) * 0.05, 0.3, 0.2))
        a.key(t, "right_arm", rot=(-1.0 + math.sin(w) * 0.05, -0.3, -0.2))
        a.key(t, "left_forearm", rot=(-0.3, 0.0, 0.0))
        a.key(t, "right_forearm", rot=(-0.3, 0.0, 0.0))
    anims["wish"] = a.json()
    return anims


for name, parts, tw, th, build in (("shenron", sh, 256, 256, build_shenron), ("porunga", po, 512, 512, build_porunga)):
    os.makedirs(OUT + "/models/entity", exist_ok=True)
    os.makedirs(OUT + "/animations/entity", exist_ok=True)
    with open("%s/models/entity/%s.geo.json" % (OUT, name), "w") as f:
        json.dump(geo(name, parts, tw, th), f, separators=(",", ":"))
    with open("%s/animations/entity/%s.animation.json" % (OUT, name), "w") as f:
        json.dump({"format_version": "1.8.0", "animations": build()}, f, separators=(",", ":"))
    print(name, len(parts), "bones")
