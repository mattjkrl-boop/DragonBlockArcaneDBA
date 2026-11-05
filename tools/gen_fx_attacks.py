"""Attack fx models for the PARTIAL list, built facing +Z (forward).

Run: python3 tools/gen_fx_attacks.py [name ...]
Physical matter uses lit kinds (steel, iron, gold, obsidian); only energy glows.
"""
import math
import sys

sys.path.insert(0, __import__("os").path.dirname(__file__))
from fx_model_lib import Model, ease_in, ease_out, overshoot, wave  # noqa: E402
import gen_fx_pieces  # noqa: E402

G = "#000000"


def energy(m, key, rim, mid, core):
    m.material(key, "energy", G, G, G, glow=(rim, mid, core))


def shift(m, dz):
    """Move the whole model along Z (pivots and cubes), for set-pieces that
    stand off the anchor when the behavior ignores models.forward."""
    for b in m.bones:
        b["pivot"][2] = round(b["pivot"][2] + dz, 2)
    for c in m.cubes:
        c["origin"][2] = round(c["origin"][2] + dz, 2)
        if "pivot" in c:
            c["pivot"][2] = round(c["pivot"][2] + dz, 2)


def clamp(t):
    return max(0.0, min(1.0, t))


def arc_xz(m, bone, radius, a0, a1, n, size_fn, mat, y=0.0, zoff=0.0, prefix=None):
    """Tangent boxes along a horizontal arc; angle 0 points +Z."""
    for i in range(n):
        f = i / max(1, n - 1)
        a = math.radians(a0 + (a1 - a0) * f)
        x, z = radius * math.sin(a), radius * math.cos(a) + zoff
        w, h, seg = size_fn(f)
        b = bone(i) if callable(bone) else bone
        m.box(b, (x, y, z), (seg, h, w), mat, rot=(0, math.degrees(a), 0))


# ================================================================ Blitz Flurry
def saber_line_slash():
    """The finisher: one dimensional line cut 7 blocks ahead, rift torn open
    along it, star flares at both ends."""
    m = Model("saber_line_slash", tex=256, bounds=8)
    energy(m, "core", "#2EC5FF", "#BFF3FF", "#FFFFFF")
    energy(m, "mid", "#0B4A73", "#2EC5FF", "#BFF3FF")
    energy(m, "rift", "#1A0633", "#5B2BB8", "#C9A8FF")
    m.bone("root")
    m.bone("line", "root")
    m.bone("rift", "root")
    m.bone("flare_f", "root", (0, 0, 112))
    m.bone("flare_b", "root", (0, 0, 0))
    m.bone("sparks", "root", (0, 0, 56))
    m.box("line", (0, 0, 56), (0.8, 0.8, 112), "core")
    m.box("line", (0, 0, 56), (2.4, 0.5, 104), "mid", rot=(0, 0, 45))
    m.box("line", (0, 0, 56), (3.6, 0.3, 92), "mid", rot=(0, 0, -20))
    for i in range(9):
        z = 8 + i * 12
        s = 1 if i % 2 else -1
        h = 3.5 + 2.5 * math.sin(i * 1.7) ** 2
        m.box("rift", (0, s * h * 0.35, z), (0.5, h, 9), "rift", rot=(12 * s, 0, 28 * s))
    for r in (0, 45, 90, 135):
        m.box("flare_f", (0, 0, 112), (0.7, 14 if r % 90 == 0 else 8, 0.7), "core", rot=(0, 0, r))
        m.box("flare_b", (0, 0, 0), (0.5, 7 if r % 90 == 0 else 4, 0.5), "mid", rot=(0, 0, r))
    m.box("flare_f", (0, 0, 112), (2.5, 2.5, 2.5), "core", rot=(45, 0, 45))
    for i in range(6):
        s = 1 if i % 2 else -1
        m.box("sparks", (s * 2.5, (i - 2.5) * 0.8, -40 + i * 16), (0.4, 0.4, 5), "mid",
              rot=(0, 35 * s, 0))
    m.clip("slash", 0.3, "hold_on_last_frame", {
        "line": {"scale": lambda t: [1, 1, max(0.02, ease_out(t / 0.18))]},
        "rift": {"scale": lambda t: [1, max(0.02, overshoot(clamp((t - 0.1) / 0.2))), max(0.02, ease_out(t / 0.18))]},
        "flare_f": {"scale": lambda t: [max(0.02, overshoot(clamp((t - 0.12) / 0.18)))] * 3},
        "flare_b": {"scale": lambda t: [max(0.02, overshoot(t / 0.15))] * 3},
        "sparks": {"scale": lambda t: [1, 1, max(0.02, ease_out(t / 0.3))]},
    })
    m.clip("linger", 0.6, True, {
        "line": {"scale": lambda t: [1 + 0.25 * wave(t, 0.15), 1 + 0.25 * wave(t, 0.15), 1]},
        "rift": {"rotation": lambda t: [0, 0, 4 * wave(t, 0.3)]},
        "flare_f": {"rotation": lambda t: [0, 0, 120 * t]},
        "sparks": {"position": lambda t: [0, 0, 8 * (t / 0.6)]},
    })
    m.clip("fade", 0.3, "hold_on_last_frame", {
        "line": {"scale": lambda t: [max(0.02, 1 - ease_in(t / 0.3)), max(0.02, 1 - ease_in(t / 0.3)), 1.05]},
        "rift": {"scale": lambda t: [1, max(0.02, 1 - t / 0.3), 1]},
        "flare_f": {"scale": lambda t: [1 + t * 2] * 3},
        "flare_b": {"scale": lambda t: [max(0.02, 1 - t / 0.3)] * 3},
        "sparks": {"scale": lambda t: [max(0.02, 1 - t / 0.3)] * 3},
    })
    return m


def saber_afterimage():
    """Cyan afterimage of the saber dash: the katana afterimage pose re-lit."""
    m = gen_fx_pieces.katana_afterimage()
    m.name = "saber_afterimage"
    m.material("body", "energy", G, G, G, glow=("#0B4A73", "#2EC5FF", "#D6F7FF"))
    m.material("coat", "energy", G, G, G, glow=("#062F4A", "#1690C8", "#A6E8FF"))
    m.material("blade", "energy", G, G, G, glow=("#2EC5FF", "#9BEBFF", "#FFFFFF"))
    return m


# ================================================================ rifts
def void_rift_portal():
    """Tall diamond void rift standing upright, broadside to the caster."""
    m = Model("void_rift_portal", tex=256, bounds=4)
    energy(m, "void", "#05010A", "#12041F", "#2A0A47")
    energy(m, "rim", "#3B1163", "#8B3DFF", "#F0E2FF")
    energy(m, "swirl", "#24083F", "#6A2BD1", "#C9A8FF")
    m.bone("root")
    m.bone("portal", "root", (0, 24, 0))
    m.bone("core", "portal", (0, 24, 0))
    m.bone("swirl", "portal", (0, 24, 0))
    m.bone("spikes", "portal", (0, 24, 0))
    m.bone("base", "root")
    H, W = 48.0, 20.0
    for i in range(8):
        y = 3 + i * 6
        w = W * (1 - abs(y - 24) / 24.0) + 1
        m.box("core", (0, y, 0), (w, 6.2, 0.8), "void")
    half = math.hypot(W / 2, H / 2)
    ang = math.degrees(math.atan2(W / 2, H / 2))
    for sx in (-1, 1):
        for sy in (-1, 1):
            cx, cy = sx * W / 4, 24 + sy * H / 4
            m.box("portal", (cx, cy, 0), (1.4, half, 1.6), "rim", rot=(0, 0, -sx * sy * ang))
    for i, (x, y, r, l) in enumerate(((-7, 30, 50, 6), (7, 18, 50, 6), (-5, 12, -40, 5),
                                       (6, 34, -40, 5), (0, 50, 0, 5), (0, -2, 0, 5),
                                       (-10.5, 24, 90, 4), (10.5, 24, 90, 4))):
        m.box("spikes", (x, y, 0), (0.8, l, 0.9), "rim", rot=(0, 0, r))
    for r, s in ((25, 9), (-35, 7), (70, 5)):
        m.box("swirl", (0, 24, 0.2), (0.6, s * 2.2, 0.5), "swirl", rot=(0, 0, r))
    m.box("base", (0, 0.2, 0), (22, 0.4, 8), "swirl")
    m.box("base", (0, 0.25, 0), (12, 0.4, 4), "rim", rot=(0, 45, 0))
    m.clip("open", 0.4, "hold_on_last_frame", {
        "portal": {"scale": lambda t: [max(0.02, overshoot(clamp((t - 0.1) / 0.3))),
                                       max(0.02, ease_out(t / 0.25)), 1]},
        "base": {"scale": lambda t: [max(0.02, ease_out(t / 0.2))] * 3},
    })
    m.clip("hold", 1.0, True, {
        "swirl": {"rotation": lambda t: [0, 0, 360 * t]},
        "core": {"scale": lambda t: [1 + 0.06 * wave(t, 0.5), 1, 1]},
        "spikes": {"scale": lambda t: [1 + 0.12 * wave(t, 0.25), 1 + 0.08 * wave(t, 0.25, 0.3), 1]},
    })
    m.clip("close", 0.3, "hold_on_last_frame", {
        "portal": {"scale": lambda t: [max(0.02, 1 - ease_in(t / 0.3)), 1 + 0.2 * t / 0.3, 1]},
        "base": {"scale": lambda t: [max(0.02, 1 - t / 0.3)] * 3},
    })
    return m


# ================================================================ Assault
def brave_cross_slash():
    """Golden and cyan X cross slash standing upright ahead of the caster."""
    m = Model("brave_cross_slash", tex=256, bounds=5)
    energy(m, "gold", "#7A4A00", "#FFD23F", "#FFFFFF")
    energy(m, "cyan", "#0A5C80", "#2EC5FF", "#FFFFFF")
    energy(m, "star", "#B8860B", "#FFF1B0", "#FFFFFF")
    m.bone("root", None, (0, 0, 0))
    m.bone("blade_a", "root", (0, 0, 0), rot=(0, 0, 42))
    m.bone("blade_b", "root", (0, 0, 0), rot=(0, 0, -42))
    m.bone("star", "root")
    m.bone("chips", "root")
    for bone, mat in (("blade_a", "gold"), ("blade_b", "cyan")):
        m.box(bone, (0, 0, 0), (0.9, 80, 0.9), "star")
        m.box(bone, (0, 0, 0), (2.6, 64, 0.5), mat)
        m.box(bone, (0, 0, 0), (4.2, 40, 0.35), mat)
        m.box(bone, (0, 0, 0), (6.0, 18, 0.25), mat)
        for s in (-1, 1):
            m.box(bone, (0, s * 42, 0), (1.6, 6, 0.6), mat, rot=(0, 0, 45))
    for r in (0, 45, 90, 135):
        m.box("star", (0, 0, 0.4), (0.8, 22 if r % 90 == 0 else 12, 0.6), "star", rot=(0, 0, r))
    m.box("star", (0, 0, 0.4), (4, 4, 4), "star", rot=(45, 0, 45))
    for i in range(6):
        a = math.radians(i * 60 + 15)
        m.box("chips", (18 * math.cos(a), 18 * math.sin(a), 0), (0.6, 3, 0.6),
              "gold" if i % 2 else "cyan", rot=(0, 0, i * 60 + 15))
    m.clip("cross", 0.35, "hold_on_last_frame", {
        "blade_a": {"scale": lambda t: [1, max(0.02, ease_out(t / 0.15)), 1]},
        "blade_b": {"scale": lambda t: [1, max(0.02, ease_out(clamp((t - 0.15) / 0.15))), 1]},
        "star": {"scale": lambda t: [max(0.02, overshoot(clamp((t - 0.2) / 0.15)))] * 3},
        "chips": {"scale": lambda t: [max(0.02, ease_out(clamp((t - 0.2) / 0.15)))] * 3},
    })
    m.clip("hold", 0.5, True, {
        "star": {"rotation": lambda t: [0, 0, 180 * t / 0.5],
                 "scale": lambda t: [1 + 0.15 * wave(t, 0.25)] * 3},
        "blade_a": {"scale": lambda t: [1 + 0.2 * wave(t, 0.2), 1, 1]},
        "blade_b": {"scale": lambda t: [1 + 0.2 * wave(t, 0.2, 0.5), 1, 1]},
        "chips": {"scale": lambda t: [1 + 0.4 * t / 0.5] * 3},
    })
    m.clip("fade", 0.3, "hold_on_last_frame", {
        "blade_a": {"scale": lambda t: [max(0.02, 1 - ease_in(t / 0.3)), 1.1, 1]},
        "blade_b": {"scale": lambda t: [max(0.02, 1 - ease_in(t / 0.3)), 1.1, 1]},
        "star": {"scale": lambda t: [1 + 1.5 * t / 0.3] * 3},
        "chips": {"scale": lambda t: [max(0.02, 1 - t / 0.3)] * 3},
    })
    return m


# ================================================================ chains
def _chain_bind(name, iron, glow):
    """Three tilted chain loops clamped round a body, spinning against each
    other. Links are lit metal; only the curse seam inside them glows."""
    m = Model(name, tex=256, bounds=3)
    m.material("link", "iron", *iron)
    energy(m, "seam", *glow)
    m.bone("root")
    loops = (("loop_a", 9, (14, 0, 0)), ("loop_b", 18, (-8, 0, 16)), ("loop_c", 27, (6, 0, -20)))
    for name_, y, rot in loops:
        m.bone(name_, "root", (0, y, 0), rot=rot)
        r, n = 11.0, 16
        for i in range(n):
            a = 2 * math.pi * i / n
            x, z = r * math.sin(a), r * math.cos(a)
            yaw = math.degrees(a)
            if i % 2:
                m.box(name_, (x, y, z), (3.6, 1.1, 2.4), "link", rot=(0, yaw, 0))
            else:
                m.box(name_, (x, y, z), (3.6, 2.4, 1.1), "link", rot=(0, yaw, 0))
                m.box(name_, (x, y, z), (2.2, 0.6, 1.25), "seam", rot=(0, yaw, 0))
    m.bone("lock", "root", (0, 18, 0))
    m.box("lock", (0, 18, 11.8), (3.2, 3.6, 1.4), "link")
    m.box("lock", (0, 18, 12.6), (1.2, 1.2, 0.4), "seam", rot=(0, 0, 45))
    spin = {"loop_a": 1, "loop_b": -1, "loop_c": 1}
    m.clip("latch", 0.3, "hold_on_last_frame", {
        **{k: {"scale": lambda t: [2.2 - 1.2 * ease_out(t / 0.3), 1, 2.2 - 1.2 * ease_out(t / 0.3)]}
           for k in spin},
        "lock": {"scale": lambda t: [max(0.02, overshoot(clamp((t - 0.15) / 0.15)))] * 3},
    })
    m.clip("bind", 2.0, True, {
        **{k: {"rotation": (lambda s: lambda t: [0, s * 180 * t, 0])(s),
               "scale": (lambda s: lambda t: [1 - 0.04 * wave(t, 0.5, s * 0.25)] * 3)(s)}
           for k, s in spin.items()},
        "lock": {"position": lambda t: [0, 0.3 * wave(t, 0.5), 0]},
    })
    m.clip("break", 0.3, "hold_on_last_frame", {
        **{k: {"scale": lambda t: [1 + 0.8 * ease_out(t / 0.3), max(0.02, 1 - t / 0.3), 1 + 0.8 * ease_out(t / 0.3)]}
           for k in spin},
        "lock": {"scale": lambda t: [max(0.02, 1 - t / 0.3)] * 3},
    })
    return m


def curse_chain_bind():
    return _chain_bind("curse_chain_bind", ("#140A14", "#3A2238", "#7A5A78"),
                       ("#7A1550", "#FF4FB8", "#FFD1EE"))


def hell_chain_bind():
    return _chain_bind("hell_chain_bind", ("#140806", "#3A1A12", "#7A4A3A"),
                       ("#8F1D1D", "#FF6A00", "#FFE9C4"))


# ================================================================ Void Rift
def void_fragment():
    """Black rock fragment torn loose, glowing void cracks in its faces."""
    m = Model("void_fragment", tex=256, bounds=2)
    m.material("rock", "obsidian", "#08050C", "#1C1226", "#3E2E52")
    m.material("rock2", "obsidian", "#050307", "#140C1C", "#2C2038")
    energy(m, "crack", "#3B1163", "#8B3DFF", "#E8D8FF")
    m.bone("root")
    m.bone("chunk", "root", rot=(18, 30, 10))
    m.bone("bits", "root")
    m.box("chunk", (0, 0, 0), (9, 7, 8), "rock")
    m.box("chunk", (1.5, 2.5, -1), (6, 5, 6), "rock2", rot=(25, 15, 30))
    m.box("chunk", (-2.5, -2, 1.5), (5, 4.5, 5), "rock2", rot=(-20, 40, -15))
    m.box("chunk", (0.5, -0.5, 3.4), (5, 4, 2.5), "rock", rot=(0, 0, 35))
    m.box("chunk", (-3, 2.5, -2.5), (3.5, 3, 3.5), "rock", rot=(45, 0, 20))
    m.box("chunk", (3.8, -1.5, 0.5), (2.5, 4, 4), "rock2", rot=(0, 30, -25))
    m.box("chunk", (0, 3.6, 0.5), (0.6, 0.4, 6.5), "crack", rot=(0, 25, 0))
    m.box("chunk", (4.6, 0.5, 0), (0.4, 5, 0.6), "crack", rot=(0, 0, 18))
    m.box("chunk", (-1, 0, 4.1), (5, 0.5, 0.4), "crack", rot=(0, 0, -30))
    m.box("chunk", (-4.6, -1, 0), (0.4, 0.5, 5.5), "crack", rot=(20, 0, 0))
    m.box("chunk", (1, -3.6, -1), (4, 0.4, 0.5), "crack", rot=(0, -40, 0))
    m.box("chunk", (0, 0, 0), (2.2, 2.2, 2.2), "crack", rot=(45, 45, 0))
    for i, (p, s) in enumerate((((-6.5, 3, 2), 1.6), ((6, 4, -3), 1.3), ((2, -5.5, 4), 1.1),
                                ((-4, -4.5, -4), 1.4), ((5, -3, 5), 0.9))):
        m.box("bits", p, (s, s, s), "rock2", rot=(i * 30, i * 50, i * 20))
    m.clip("rise", 0.3, "hold_on_last_frame", {
        "root": {"scale": lambda t: [max(0.02, overshoot(t / 0.3))] * 3},
        "bits": {"scale": lambda t: [max(0.02, 0.3 + 0.7 * ease_out(t / 0.3))] * 3},
    })
    m.clip("tumble", 2.0, True, {
        "chunk": {"rotation": lambda t: [180 * t, 360 * t / 2.0, 90 * t]},
        "bits": {"rotation": lambda t: [0, -360 * t / 2.0, 0],
                 "position": lambda t: [0, 0.8 * wave(t, 1.0), 0]},
    })
    m.clip("implode", 0.3, "hold_on_last_frame", {
        "root": {"scale": lambda t: [max(0.02, 1 - ease_in(t / 0.3))] * 3},
        "bits": {"position": lambda t: [0, 0, -6 * t / 0.3]},
    })
    return m


# ================================================================ Dimensional Slash
def dimensional_crescent():
    """Flying crescent slash bulging forward, a rift line torn along it."""
    m = Model("dimensional_crescent", tex=256, bounds=4)
    energy(m, "edge", "#2EC5FF", "#D6F7FF", "#FFFFFF")
    energy(m, "body", "#1A0633", "#6A2BD1", "#C9A8FF")
    energy(m, "rift", "#05010A", "#2A0A47", "#8B3DFF")
    m.bone("root")
    m.bone("arc", "root")
    m.bone("inner", "root")
    m.bone("shards", "root")
    m.bone("tips", "root")
    taper = lambda f: 1 - abs(f - 0.5) * 1.6
    arc_xz(m, "arc", 26, -72, 72, 13, lambda f: (3.2 * taper(f) + 0.4, 0.8 * taper(f) + 0.3, 6.8), "edge", zoff=-14)
    arc_xz(m, "arc", 24, -64, 64, 11, lambda f: (4.6 * taper(f) + 0.6, 0.5, 7.2), "body", zoff=-14)
    arc_xz(m, "inner", 21.5, -50, 50, 7, lambda f: (1.0, 0.4, 7.4), "rift", zoff=-14)
    for i in range(6):
        a = math.radians(-50 + i * 20)
        m.box("shards", (21 * math.sin(a), (1 if i % 2 else -1) * 1.4, 21 * math.cos(a) - 14),
              (0.5, 3, 2.5), "rift", rot=(0, math.degrees(a), 35 if i % 2 else -35))
    for s in (-1, 1):
        a = math.radians(80 * s)
        m.box("tips", (27 * math.sin(a), 0, 27 * math.cos(a) - 14), (0.5, 0.5, 6), "edge",
              rot=(0, math.degrees(a) + 90 - 20 * s, 0))
    for side, roll in (("l", 30), ("r", -30)):
        m.clip("fly_" + side, 0.5, True, {
            "root": {"rotation": (lambda r: lambda t: [0, 0, r])(roll)},
            "inner": {"scale": lambda t: [1 + 0.1 * wave(t, 0.25), 1 + 0.6 * wave(t, 0.25), 1]},
            "shards": {"rotation": lambda t: [0, 6 * wave(t, 0.5), 0]},
            "arc": {"scale": lambda t: [1, 1 + 0.25 * wave(t, 0.125), 1]},
        })
    m.clip("spawn", 0.3, "hold_on_last_frame", {
        "root": {"scale": lambda t: [max(0.02, overshoot(t / 0.3)), 1, 1]},
    })
    m.clip("end", 0.3, "hold_on_last_frame", {
        "root": {"scale": lambda t: [1 + 0.4 * t / 0.3, max(0.02, 1 - t / 0.3), max(0.02, 1 - ease_in(t / 0.3))]},
    })
    return m


# ================================================================ Prong Shot
def prong_bolt():
    """The thrown devil prong in gold with a hellfire tail streaming behind."""
    m = gen_fx_pieces.devil_shard()
    m.name = "prong_bolt"
    m.material("iron", "gold", "#3A2406", "#8A6410", "#E0B84A")
    m.material("iron_dark", "gold", "#24160A", "#5A3C0C", "#A07A22")
    m.material("edge", "gold", "#5A3A08", "#C89A2A", "#FFF0B0")
    energy(m, "tail", "#5A0A0A", "#FF6A00", "#FFE9C4")
    m.bone("tail", "root", (0, 0, -12))
    m.box("tail", (0, 0, -22), (1.4, 1.4, 20), "tail")
    m.box("tail", (0, 0, -30), (0.8, 0.8, 30), "tail", rot=(0, 0, 45))
    m.box("tail", (0, 0, -18), (3.0, 0.3, 10), "tail")
    m.box("tail", (0, 0, -18), (0.3, 3.0, 10), "tail")
    m.clips["fly"]["bones"]["tail"] = {}
    tail = Model("_t")
    tail.clip("x", 1.0, True, {"tail": {"scale": lambda t: [1 + 0.3 * wave(t, 0.2), 1 + 0.3 * wave(t, 0.2), 1 + 0.15 * wave(t, 0.25)]}})
    m.clips["fly"]["bones"]["tail"] = tail.clips["x"]["bones"]["tail"]
    return m


# ================================================================ Sorrow Slash
def sorrow_slash_arc():
    """Pale grief crescent swept right to left, violet trail and falling tears."""
    m = Model("sorrow_slash_arc", tex=256, bounds=4)
    energy(m, "core", "#5A6A80", "#DCE3EE", "#FFFFFF")
    energy(m, "trail", "#24083F", "#7A4FD1", "#D9C4FF")
    energy(m, "tear", "#2A3A66", "#8FB0FF", "#E6EEFF")
    m.bone("root")
    n = 11
    for i in range(n):
        m.bone("seg%d" % i, "root")
    m.bone("trail", "root")
    m.bone("tears", "root")
    taper = lambda f: 1 - abs(f - 0.45) * 1.5
    arc_xz(m, lambda i: "seg%d" % i, 34, 60, -60, n, lambda f: (3.4 * taper(f) + 0.5, 0.9 * taper(f) + 0.3, 7.0), "core", zoff=-12)
    arc_xz(m, "trail", 31, 58, -40, 9, lambda f: (2.6 * (1 - f) + 0.4, 0.4, 7.2), "trail", zoff=-12, y=-0.6)
    arc_xz(m, "trail", 37, 50, -30, 7, lambda f: (1.0 * (1 - f) + 0.3, 0.3, 8), "trail", zoff=-12, y=0.8)
    for i in range(5):
        a = math.radians(40 - i * 20)
        m.box("tears", (33 * math.sin(a), -2.5 - (i % 2) * 1.5, 33 * math.cos(a) - 12),
              (0.9, 1.6, 0.9), "tear", rot=(45, 0, 45))
    segs = {}
    for i in range(n):
        d = 0.18 * i / (n - 1)
        segs["seg%d" % i] = {"scale": (lambda d: lambda t: [max(0.02, ease_out(clamp((t - d) / 0.06)))] * 3)(d)}
    m.clip("sweep", 0.3, "hold_on_last_frame", {
        **segs,
        "trail": {"scale": lambda t: [max(0.02, ease_out(clamp((t - 0.1) / 0.2)))] * 3},
        "tears": {"position": lambda t: [0, -3 * clamp((t - 0.15) / 0.15), 0]},
    })
    m.clip("hold", 0.4, True, {
        "trail": {"rotation": lambda t: [0, -6 * t / 0.4, 0]},
        "tears": {"position": lambda t: [0, -3 - 4 * t / 0.4, 0]},
    })
    m.clip("fade", 0.3, "hold_on_last_frame", {
        "root": {"scale": lambda t: [1 + 0.1 * t / 0.3, max(0.02, 1 - ease_in(t / 0.3)), 1 + 0.1 * t / 0.3]},
        "tears": {"position": lambda t: [0, -7 - 6 * t / 0.3, 0]},
    })
    return m


# ================================================================ Ox Brace
def titan_aegis():
    """Spectral ox titan rising behind the caster: horned helm, pauldrons,
    tower shield braced forward, the Ox King's axe grounded at its side,
    lower body trailing into mist."""
    m = Model("titan_aegis", tex=256, bounds=6)
    energy(m, "body", "#123A5C", "#6EC6FF", "#EAF7FF")
    energy(m, "plate", "#0A2A44", "#3D9BE0", "#BFE6FF")
    energy(m, "fire", "#7A2A00", "#FF7A1A", "#FFE2B0")
    energy(m, "mist", "#06182A", "#1E5A8A", "#6EC6FF")
    m.bone("root")
    m.bone("torso", "root", (0, 22, 0), rot=(8, 0, 0))
    m.bone("head", "torso", (0, 38, 1), rot=(-6, 0, 0))
    m.bone("horn_l", "head", (4.5, 42, 1), rot=(0, 0, -35))
    m.bone("horn_r", "head", (-4.5, 42, 1), rot=(0, 0, 35))
    m.bone("arm_l", "torso", (9, 35, 0), rot=(-50, 0, -12))
    m.bone("fore_l", "arm_l", (10, 25, 0), rot=(-40, 0, 0))
    m.bone("shield", "fore_l", (10, 18, 4))
    m.bone("arm_r", "torso", (-9, 35, 0), rot=(10, 0, 14))
    m.bone("fore_r", "arm_r", (-11, 25, 0), rot=(-20, 0, 0))
    m.bone("axe", "fore_r", (-11, 17, 0), rot=(-10, 0, 0))
    m.bone("mist", "root", (0, 22, 0))
    m.box("torso", (0, 29, 0), (14, 16, 8), "body")
    m.box("torso", (0, 31, 3.8), (11, 10, 1.4), "plate")
    m.box("torso", (0, 31, 4.6), (2.4, 2.4, 0.6), "fire", rot=(0, 0, 45))
    m.box("torso", (0, 22.5, 0), (11, 4, 7), "plate")
    for s in (1, -1):
        m.box("torso", (s * 9.5, 36, 0), (6.5, 4, 8.5), "plate", rot=(0, 0, -s * 18))
        m.box("torso", (s * 11.5, 33.5, 0), (3, 3.5, 7), "plate", rot=(0, 0, -s * 30))
    m.box("head", (0, 41.5, 1), (7, 7, 7), "body")
    m.box("head", (0, 42, 4.4), (5.6, 1.2, 0.5), "fire")
    m.box("head", (0, 45.6, 1), (7.6, 1.4, 7.6), "plate")
    m.box("head", (0, 38.5, 4), (3.5, 2.5, 1.6), "plate", rot=(20, 0, 0))
    for s, b in ((1, "horn_l"), (-1, "horn_r")):
        m.box(b, (s * 6.5, 43, 1), (5, 2, 2), "plate")
        m.box(b, (s * 9.5, 45, 1), (2, 4.5, 1.6), "plate", rot=(0, 0, s * 20))
    for s, a, f in ((1, "arm_l", "fore_l"), (-1, "arm_r", "fore_r")):
        m.box(a, (s * 10, 30, 0), (4.5, 10, 4.5), "body")
        m.box(f, (s * 10.5, 21, 0), (4, 9, 4), "body")
        m.box(f, (s * 10.5, 22, 0), (4.8, 4, 4.8), "plate")
    m.box("shield", (10, 18, 6), (16, 26, 2), "plate")
    m.box("shield", (10, 18, 7.2), (12, 20, 0.6), "body")
    m.box("shield", (10, 18, 7.7), (4, 4, 0.6), "fire", rot=(0, 0, 45))
    m.box("shield", (10, 31.5, 6), (10, 1.5, 2.4), "fire")
    m.box("axe", (-11, 12, 0), (1.4, 26, 1.4), "plate")
    m.box("axe", (-11, 0, 0), (1.2, 4, 1.2), "fire")
    m.box("axe", (-11, 22, 2.6), (1.2, 9, 5.5), "fire")
    m.box("axe", (-11, 22, 5.8), (1.0, 12, 1.4), "body")
    for i, (w, y, d) in enumerate(((13, 17, 7), (11, 11, 6), (8, 5.5, 5), (5, 1, 3.5))):
        m.box("mist", (0, y, -i * 0.8), (w, 5.5, d), "mist", rot=(0, i * 20, 0))
    m.clip("rise", 0.3, "hold_on_last_frame", {
        "root": {"position": lambda t: [0, -18 * (1 - ease_out(t / 0.3)), 0],
                 "scale": lambda t: [max(0.02, 0.6 + 0.4 * overshoot(t / 0.3))] * 3},
        "arm_l": {"rotation": lambda t: [30 * (1 - ease_out(t / 0.3)), 0, 0]},
    })
    m.clip("stance", 2.0, True, {
        "torso": {"scale": lambda t: [1 + 0.02 * wave(t, 1.0), 1 + 0.03 * wave(t, 1.0), 1]},
        "head": {"rotation": lambda t: [2 * wave(t, 2.0), 3 * wave(t, 2.0, 0.25), 0]},
        "shield": {"position": lambda t: [0, 0, 0.6 * wave(t, 1.0)]},
        "mist": {"rotation": lambda t: [0, 180 * t, 0],
                 "scale": lambda t: [1 + 0.06 * wave(t, 0.5)] * 3},
    })
    m.clip("fade", 0.3, "hold_on_last_frame", {
        "root": {"scale": lambda t: [1 + 0.15 * t / 0.3, max(0.02, 1 - ease_in(t / 0.3)), 1 + 0.15 * t / 0.3]},
    })
    # stands behind the caster: orbit radius 0 ignores models.forward
    shift(m, -14)
    return m


# ================================================================ Blaster
def blaster_bolt():
    """Plasma slug: hot core, faceted sheath, compression rings, tail fins."""
    m = Model("blaster_bolt", tex=256, bounds=2)
    energy(m, "core", "#FF6A00", "#FFD9A0", "#FFFFFF")
    energy(m, "sheath", "#8F1D1D", "#FF6A00", "#FFC27A")
    energy(m, "ring", "#B8860B", "#FFD23F", "#FFF6D0")
    m.bone("root")
    m.bone("body", "root")
    m.bone("rings", "root")
    m.bone("fins", "root", (0, 0, -6))
    m.box("body", (0, 0, 0), (1.8, 1.8, 11), "core")
    m.box("body", (0, 0, 0.5), (2.8, 2.8, 8), "sheath", rot=(0, 0, 45))
    m.box("body", (0, 0, 5.4), (2.0, 2.0, 1.6), "core", rot=(0, 0, 45))
    m.box("body", (0, 0, 6.6), (1.0, 1.0, 1.2), "core")
    m.box("body", (0, 0, -6.5), (1.2, 1.2, 5), "sheath")
    for i, z in enumerate((3, 0, -3)):
        m.box("rings", (0, 0, z), (3.6 - i * 0.4, 3.6 - i * 0.4, 0.5), "ring", rot=(0, 0, 45 * (i % 2)))
    for r in (0, 90, 180, 270):
        a = math.radians(r)
        m.box("fins", (1.8 * math.cos(a), 1.8 * math.sin(a), -6), (0.3, 1.4, 4), "sheath", rot=(0, 0, r))
    m.box("fins", (0, 0, -10), (0.6, 0.6, 6), "sheath", rot=(0, 0, 45))
    m.clip("spawn", 0.3, "hold_on_last_frame", {
        "root": {"scale": lambda t: [max(0.02, overshoot(t / 0.3))] * 3},
    })
    m.clip("fly", 0.5, True, {
        "body": {"rotation": lambda t: [0, 0, 720 * t]},
        "rings": {"rotation": lambda t: [0, 0, -360 * t / 0.5],
                  "position": lambda t: [0, 0, -2 * (t / 0.5)]},
        "fins": {"scale": lambda t: [1, 1, 1 + 0.3 * wave(t, 0.1)]},
    })
    m.clip("end", 0.3, "hold_on_last_frame", {
        "root": {"scale": lambda t: [1 + 1.5 * t / 0.3, 1 + 1.5 * t / 0.3, max(0.02, 1 - t / 0.3)]},
    })
    return m


def blaster_casing():
    """Spent brass casing kicked out of the blaster, tumbling to the right."""
    m = Model("blaster_casing", tex=256, bounds=2)
    m.material("brass", "gold", "#4A3008", "#B8860B", "#FFE08A")
    m.material("brass_dark", "gold", "#2A1A06", "#7A5A14", "#C8A040")
    m.material("primer", "steel", "#2A2A30", "#6A6A72", "#C0C0C8")
    m.bone("root")
    m.bone("case", "root")
    m.box("case", (0, 0, 0), (2.4, 2.4, 6), "brass")
    m.box("case", (0, 0, 0), (2.6, 2.6, 5.4), "brass", rot=(0, 0, 45))
    m.box("case", (0, 0, -3.2), (3.0, 3.0, 0.6), "brass_dark")
    m.box("case", (0, 0, -3.25), (3.1, 3.1, 0.5), "brass_dark", rot=(0, 0, 45))
    m.box("case", (0, 0, -3.6), (1.0, 1.0, 0.3), "primer")
    m.box("case", (0, 0, 3.6), (1.8, 1.8, 1.4), "brass", rot=(0, 0, 22))
    m.box("case", (0, 0, 4.5), (2.0, 2.0, 0.4), "brass_dark")
    m.box("case", (0, 1.3, 0.5), (0.4, 0.2, 4), "brass_dark")
    m.box("case", (1.3, 0, -0.5), (0.2, 0.4, 3), "brass_dark")
    m.box("case", (0, -1.3, -1.5), (0.6, 0.2, 1), "brass_dark", rot=(0, 30, 0))
    m.clip("eject", 0.6, "hold_on_last_frame", {
        "root": {"position": lambda t: [10 * (t / 0.6), 9 * (t / 0.6) - 16 * (t / 0.6) ** 2, -3 * t / 0.6]},
        "case": {"rotation": lambda t: [900 * t, 300 * t, 0]},
    })
    return m


# ================================================================ Lava Cleave
def lava_fountain():
    """Molten fountain bursting through a cracked basalt crust."""
    m = Model("lava_fountain", tex=256, bounds=4)
    m.material("basalt", "obsidian", "#0E0A08", "#2A201A", "#4A3A30")
    energy(m, "lava", "#8F1D1D", "#FF5A00", "#FFD27A")
    energy(m, "hot", "#FF6A00", "#FFC04A", "#FFFFFF")
    m.bone("root")
    m.bone("crust", "root")
    m.bone("column", "root")
    for i in range(5):
        m.bone("blob%d" % i, "column", (0, 4 + i * 7, 0))
    m.bone("drops", "root", (0, 30, 0))
    for i in range(7):
        a = 2 * math.pi * i / 7
        m.box("crust", (7 * math.sin(a), 1, 7 * math.cos(a)), (5, 2.6 + (i % 3), 3.5), "basalt",
              rot=(-25 if i % 2 else -15, math.degrees(a), 10 * (i % 3 - 1)))
    m.box("crust", (0, 0.3, 0), (9, 0.6, 9), "lava", rot=(0, 20, 0))
    for i in range(4):
        a = 2 * math.pi * (i + 0.5) / 4
        m.box("crust", (6.5 * math.sin(a), 0.4, 6.5 * math.cos(a)), (0.6, 0.5, 5), "hot", rot=(0, math.degrees(a), 0))
    for i in range(5):
        s = 7 - i * 1.1
        m.box("blob%d" % i, (0.6 * math.sin(i * 2), 4 + i * 7, 0.6 * math.cos(i * 2)),
              (s, 7.5, s), "lava" if i % 2 else "hot", rot=(0, i * 27, 6 * (i % 2 * 2 - 1)))
    for i in range(6):
        a = 2 * math.pi * i / 6
        m.box("drops", (6 * math.sin(a), 30 - (i % 2) * 3, 6 * math.cos(a)), (1.6, 2.2, 1.6), "lava",
              rot=(45, math.degrees(a), 0))
    blobs = {}
    for i in range(5):
        blobs["blob%d" % i] = {"scale": (lambda i: lambda t: [max(0.02, overshoot(clamp((t - i * 0.05) / 0.2)))] * 3)(i)}
    m.clip("erupt", 0.4, "hold_on_last_frame", {
        **blobs,
        "column": {"scale": lambda t: [1, max(0.02, 1.25 * ease_out(t / 0.3)), 1]},
        "crust": {"scale": lambda t: [max(0.02, overshoot(t / 0.2))] * 3},
        "drops": {"scale": lambda t: [max(0.02, ease_out(clamp((t - 0.2) / 0.2)))] * 3},
    })
    gush = {}
    for i in range(5):
        gush["blob%d" % i] = {"position": (lambda i: lambda t: [0, 2.5 * wave(t, 0.4, i * 0.2), 0])(i),
                              "rotation": (lambda i: lambda t: [0, 90 * t * (1 if i % 2 else -1), 0])(i)}
    m.clip("gush", 0.8, True, {
        **gush,
        "column": {"scale": lambda t: [1 + 0.05 * wave(t, 0.2), 1.25, 1 + 0.05 * wave(t, 0.2)]},
        "drops": {"position": lambda t: [0, -6 * ((t / 0.8 * 2) % 1), 0],
                  "scale": lambda t: [1 + 0.6 * ((t / 0.8 * 2) % 1)] * 3},
    })
    m.clip("sink", 0.4, "hold_on_last_frame", {
        "column": {"scale": lambda t: [1, max(0.02, 1.25 * (1 - ease_in(t / 0.4))), 1]},
        "drops": {"position": lambda t: [0, -24 * ease_in(t / 0.4), 0]},
        "crust": {"position": lambda t: [0, -2 * t / 0.4, 0]},
    })
    return m


# ================================================================ Spike Flail
def flail_chain_ball():
    """Haft grip, nine-link chain along +Z and a spiked iron ball."""
    m = Model("flail_chain_ball", tex=256, bounds=4)
    m.material("iron", "iron", "#141416", "#3A3A40", "#8A8A94")
    m.material("steel", "steel", "#2A2A30", "#70707A", "#D0D0DA")
    m.material("leather", "iron", "#1A0E08", "#4A2A18", "#7A4A30")
    m.bone("root")
    m.bone("grip", "root")
    m.bone("chain", "root", (0, 0, 3))
    m.bone("ball", "chain", (0, 0, 38))
    m.box("grip", (0, 0, -2), (2.2, 2.2, 6), "leather")
    m.box("grip", (0, 0, 1.4), (2.8, 2.8, 1.0), "steel")
    m.box("grip", (0, 0, 2.6), (1.4, 1.4, 1.4), "iron", rot=(0, 0, 45))
    for i in range(9):
        z = 4.5 + i * 3.6
        m.box("chain", (0, 0, z), (1.0 if i % 2 else 2.2, 2.2 if i % 2 else 1.0, 4.0), "iron")
        m.box("chain", (0, 0, z), (0.4, 0.4, 2.0), "steel")
    m.box("ball", (0, 0, 42), (8, 8, 8), "iron")
    m.box("ball", (0, 0, 42), (8.6, 6, 6), "iron", rot=(0, 45, 0))
    m.box("ball", (0, 0, 42), (6, 8.6, 6), "iron", rot=(45, 0, 0))
    m.box("ball", (0, 0, 37.6), (3, 3, 1.4), "steel")
    dirs = [(1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1),
            (0.7, 0.7, 0), (-0.7, 0.7, 0), (0.7, -0.7, 0), (-0.7, -0.7, 0),
            (0, 0.7, 0.7), (0.7, 0, 0.7), (-0.7, 0, 0.7)]
    for dx, dy, dz in dirs:
        yaw = math.degrees(math.atan2(dx, dz))
        pitch = -math.degrees(math.asin(max(-1, min(1, dy))))
        m.box("ball", (dx * 5.6, dy * 5.6, 42 + dz * 5.6), (1.3, 1.3, 3.4), "steel", rot=(pitch, yaw, 45))
    m.clip("whirl", 0.5, True, {
        "chain": {"rotation": lambda t: [3 * wave(t, 0.25), 4 * wave(t, 0.5), 0]},
        "ball": {"rotation": lambda t: [0, 0, 720 * t]},
    })
    m.clip("swing", 0.5, "hold_on_last_frame", {
        "root": {"rotation": lambda t: [0, 80 - 160 * ease_out(t / 0.35), 0]},
        "chain": {"rotation": lambda t: [0, 25 * (1 - ease_out(t / 0.35)), 0]},
        "ball": {"rotation": lambda t: [0, 0, 540 * t]},
    })
    m.clip("retract", 0.3, "hold_on_last_frame", {
        "chain": {"scale": lambda t: [1, 1, max(0.05, 1 - ease_in(t / 0.3))]},
        "ball": {"scale": lambda t: [max(0.05, 1 - t / 0.3)] * 3},
    })
    return m


# ================================================================ Rising Blow
def rising_eruption():
    """Rock slabs heaved up in a fan ahead, a dust shock ring on the ground
    and a gold uplift streak where the caster rockets up."""
    m = Model("rising_eruption", tex=256, bounds=5)
    m.material("stone", "iron", "#2A2622", "#5E564C", "#A0927E")
    m.material("stone2", "iron", "#1E1A16", "#4A4238", "#7E705E")
    energy(m, "shock", "#7A5A10", "#E8C56A", "#FFF6D8")
    energy(m, "lift", "#B8860B", "#FFD23F", "#FFFFFF")
    m.bone("root")
    for i in range(7):
        m.bone("slab%d" % i, "root")
    m.bone("ring", "root")
    m.bone("lift", "root")
    for i in range(7):
        a = math.radians(-60 + i * 20)
        r = 11 + (i % 2) * 3
        h = 11 + 5 * math.sin(i * 1.3) ** 2
        m.box("slab%d" % i, (r * math.sin(a), h / 2 - 2, r * math.cos(a)), (7 + i % 3, h, 3.2),
              "stone" if i % 2 else "stone2", rot=(-35, math.degrees(a), 12 * ((i % 3) - 1)))
        m.box("slab%d" % i, ((r - 3) * math.sin(a), 0.5, (r - 3) * math.cos(a)), (3, 2.5, 3),
              "stone2", rot=(20, math.degrees(a) + 30, 15))
    for i in range(10):
        a = 2 * math.pi * i / 10
        m.box("ring", (9 * math.sin(a), 0.3, 9 * math.cos(a)), (6, 0.5, 1.0), "shock",
              rot=(0, math.degrees(a) + 90, 0))
    m.box("lift", (0, 18, 0), (1.4, 36, 1.4), "lift")
    m.box("lift", (0, 14, 0), (3.5, 24, 0.4), "shock", rot=(0, 45, 0))
    m.box("lift", (0, 14, 0), (3.5, 24, 0.4), "shock", rot=(0, -45, 0))
    slabs = {}
    for i in range(7):
        d = abs(i - 3) * 0.03
        slabs["slab%d" % i] = {"position": (lambda d: lambda t: [0, -14 * (1 - overshoot(clamp((t - d) / 0.2))), 0])(d)}
    m.clip("erupt", 0.35, "hold_on_last_frame", {
        **slabs,
        "ring": {"scale": lambda t: [max(0.02, 0.3 + 1.2 * ease_out(t / 0.35))] * 3},
        "lift": {"scale": lambda t: [1, max(0.02, ease_out(t / 0.15)), 1]},
    })
    m.clip("hold", 0.6, True, {
        "ring": {"scale": lambda t: [1.5 + 0.6 * t / 0.6, 1, 1.5 + 0.6 * t / 0.6]},
        "lift": {"scale": lambda t: [1 + 0.2 * wave(t, 0.15), 1, 1 + 0.2 * wave(t, 0.15)]},
    })
    crumble = {}
    for i in range(7):
        crumble["slab%d" % i] = {"position": (lambda i: lambda t: [0, -12 * ease_in(t / 0.4), 0])(i),
                                 "rotation": (lambda i: lambda t: [-25 * t / 0.4, 0, 10 * (i - 3) * t / 0.4])(i)}
    m.clip("crumble", 0.4, "hold_on_last_frame", {
        **crumble,
        "ring": {"scale": lambda t: [max(0.02, 2.1 * (1 - t / 0.4))] * 3},
        "lift": {"scale": lambda t: [max(0.02, 1 - t / 0.4), 1.2, max(0.02, 1 - t / 0.4)]},
    })
    return m


# ================================================================ Gale Force
def cyclone_debris():
    """Four real pieces of torn-up ground: stone block, grassy dirt clod,
    a split plank and a cobble lump, each tumbling on its own."""
    m = Model("cyclone_debris", tex=256, bounds=3)
    m.material("stone", "iron", "#3A3A3A", "#6E6E6E", "#A8A8A8")
    m.material("dirt", "iron", "#2A1A0E", "#5A3A22", "#8A6040")
    m.material("grass", "iron", "#1E3A12", "#3E7A26", "#7AB04A")
    m.material("wood", "iron", "#3A2412", "#7A5430", "#B88A58")
    m.material("cobble", "iron", "#2E2E2E", "#5A5A5A", "#8E8E8E")
    m.bone("root")
    for n, p in (("p_stone", (-9, 3, 4)), ("p_dirt", (8, -2, 6)), ("p_plank", (2, 6, -9)), ("p_cobble", (-4, -5, -6))):
        m.bone(n, "root", p)
    m.box("p_stone", (-9, 3, 4), (6, 6, 6), "stone", rot=(20, 30, 10))
    m.box("p_stone", (-9, 6.2, 4.6), (4, 1.4, 3.5), "stone", rot=(20, 50, 10))
    m.box("p_dirt", (8, -2, 6), (5.5, 4, 5), "dirt", rot=(-15, 20, 25))
    m.box("p_dirt", (8, 0.4, 6), (5.8, 1.2, 5.3), "grass", rot=(-15, 20, 25))
    m.box("p_dirt", (9.5, -4, 6.5), (3, 2.5, 3), "dirt", rot=(30, 0, 10))
    m.box("p_dirt", (7.2, 1.8, 5.5), (0.5, 2, 0.5), "grass", rot=(10, 0, 15))
    m.box("p_plank", (2, 6, -9), (12, 1.6, 3.5), "wood", rot=(10, 35, -20))
    m.box("p_plank", (6.5, 7.5, -6), (5, 1.2, 1.4), "wood", rot=(10, 55, -10))
    m.box("p_plank", (-2.5, 4.6, -11.5), (2.5, 1.0, 1.0), "wood", rot=(0, 20, 35))
    m.box("p_cobble", (-4, -5, -6), (4.5, 4, 4.5), "cobble", rot=(15, 15, 40))
    m.box("p_cobble", (-2, -3.5, -5), (3, 3, 3), "stone", rot=(40, 20, 0))
    m.box("p_cobble", (-5.5, -6.5, -4.5), (2.5, 2.5, 2.5), "cobble", rot=(0, 45, 20))
    for i, p in enumerate(((0, 0, 0), (3, -6, 1), (-6, 8, -2), (10, 4, -3))):
        m.box("root", p, (1.2, 1.2, 1.2), "stone" if i % 2 else "dirt", rot=(i * 40, i * 25, 30))
    m.clip("lift", 0.3, "hold_on_last_frame", {
        "root": {"scale": lambda t: [max(0.02, overshoot(t / 0.3))] * 3,
                 "position": lambda t: [0, -6 * (1 - ease_out(t / 0.3)), 0]},
    })
    spins = {"p_stone": (300, 120, 60), "p_dirt": (-200, 240, 0), "p_plank": (90, 360, 180), "p_cobble": (240, -120, 300)}
    m.clip("tumble", 2.0, True, {
        k: {"rotation": (lambda v: lambda t: [v[0] * t / 2.0, v[1] * t / 2.0, v[2] * t / 2.0])(v)}
        for k, v in spins.items()
    })
    m.clip("scatter", 0.4, "hold_on_last_frame", {
        "p_stone": {"position": lambda t: [-8 * t / 0.4, -10 * ease_in(t / 0.4), 4 * t / 0.4]},
        "p_dirt": {"position": lambda t: [8 * t / 0.4, -10 * ease_in(t / 0.4), 4 * t / 0.4]},
        "p_plank": {"position": lambda t: [2 * t / 0.4, -10 * ease_in(t / 0.4), -8 * t / 0.4]},
        "p_cobble": {"position": lambda t: [-4 * t / 0.4, -10 * ease_in(t / 0.4), -6 * t / 0.4]},
    })
    return m


BUILDERS = {f.__name__: f for f in (
    saber_line_slash, saber_afterimage, void_rift_portal, brave_cross_slash,
    curse_chain_bind, hell_chain_bind, void_fragment, dimensional_crescent,
    prong_bolt, sorrow_slash_arc, titan_aegis, blaster_bolt, blaster_casing,
    lava_fountain, flail_chain_ball, rising_eruption, cyclone_debris)}

if __name__ == "__main__":
    for n in sys.argv[1:] or list(BUILDERS):
        BUILDERS[n]().write()
