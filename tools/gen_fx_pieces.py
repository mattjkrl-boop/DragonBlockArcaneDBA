"""Generator for the five ability piece models (fx pieces pass).

  devil_shard        Devil Trident deploy: the 10 flying trident shards
                     (ride behavior on the swarm shards, faces +Z).
  cyclone_shard      Grand Cyclone: broken greatsword fragments thrown
                     round the spin (orbit, cast trigger, faces +Z).
  rain_sword_tip     Katchin Weight finale: broken Z Sword tips driven
                     down by the gravity slam (fall, point along -Y).
  evil_spike         Evil Impale full charge: short black spearheads
                     bursting out of the air round the caster's body
                     (burst ring, points outward along +Z).
  katana_afterimage  Flashdraw: the energy afterimage left at each hop
                     of the chain dash (burst point, step trigger).

Physical matter (iron, steel, gold, obsidian) is world lit and paints
black into the glowmask; only the energy parts glow.
Run: python3 tools/gen_fx_pieces.py [name ...]
No em dashes anywhere in this file.
"""
import math
import sys

from fx_model_lib import Model, ease_in, ease_out, overshoot, wave


# ================================================================ devil_shard
def devil_shard():
    m = Model("devil_shard", tex=256, bounds=2)
    m.material("iron", "iron", "#140708", "#3A0D0D", "#8F1D1D")
    m.material("iron_dark", "iron", "#0A0405", "#241011", "#5A1A1A")
    m.material("edge", "steel", "#2A1012", "#6A2A26", "#C9786A")
    m.material("core", "energy", "#000000", "#000000", "#000000",
               glow=("#8F1D1D", "#FF6A00", "#FFE9C4"))
    m.bone("root")
    m.bone("shaft", "root", (0, 0, 0))
    m.bone("head", "root", (0, 0, 1))
    m.bone("prong_l", "head", (-3.1, 0, 1.6))
    m.bone("prong_c", "head", (0, 0, 1.6))
    m.bone("prong_r", "head", (3.1, 0, 1.6))
    m.bone("core", "head", (0, 0, 0.8))
    # shaft stub with collars, a diamond collar, and a tail spike
    m.box("shaft", (0, 0, -5), (1.5, 1.5, 10), "iron")
    m.box("shaft", (0, 0, -1.2), (2.3, 2.3, 1.0), "iron_dark")
    m.box("shaft", (0, 0, -4.6), (2.1, 2.1, 0.8), "iron_dark")
    m.box("shaft", (0, 0, -2.9), (2.0, 2.0, 1.2), "iron", rot=(0, 0, 45))
    m.box("shaft", (0, 0, -8.4), (1.9, 1.9, 0.7), "iron_dark", rot=(0, 0, 45))
    m.box("shaft", (0, 0, -10.8), (1.1, 1.1, 2.2), "edge", rot=(0, 0, 45))
    m.box("shaft", (0, 0, -12.2), (0.6, 0.6, 1.2), "edge", rot=(0, 0, 45))
    # crossbar and swept shoulders
    m.box("head", (0, 0, 0.8), (6.4, 1.5, 1.6), "iron")
    m.box("head", (0, -0.9, 0.5), (3.0, 0.5, 1.0), "iron_dark")
    m.box("head", (-3.4, 0, 0.3), (1.8, 1.3, 1.4), "iron", rot=(0, 28, 0))
    m.box("head", (3.4, 0, 0.3), (1.8, 1.3, 1.4), "iron", rot=(0, -28, 0))
    m.box("head", (0, 0, 0.1), (2.4, 2.4, 0.9), "iron_dark", rot=(0, 0, 45))
    # centre prong: square base, diamond mid, needle tip, two back barbs
    m.box("prong_c", (0, 0, 3.4), (1.3, 1.3, 3.6), "iron")
    m.box("prong_c", (0, 0, 6.5), (1.0, 1.0, 3.0), "edge", rot=(0, 0, 45))
    m.box("prong_c", (0, 0, 8.9), (0.6, 0.6, 2.2), "edge", rot=(0, 0, 45))
    m.box("prong_c", (-0.9, 0, 6.6), (0.45, 0.45, 2.0), "edge", rot=(0, -38, 0), pivot=(-0.5, 0, 7.4))
    m.box("prong_c", (0.9, 0, 6.6), (0.45, 0.45, 2.0), "edge", rot=(0, 38, 0), pivot=(0.5, 0, 7.4))
    # side prongs curve inward, outer barbs
    for side, bone in ((-1, "prong_l"), (1, "prong_r")):
        x = 3.1 * side
        m.box(bone, (x, 0, 3.1), (1.1, 1.1, 3.2), "iron")
        m.box(bone, (x - 0.35 * side, 0, 5.9), (0.85, 0.85, 3.0), "edge",
              rot=(0, 14 * side, 45), pivot=(x, 0, 4.6))
        m.box(bone, (x - 0.9 * side, 0, 7.8), (0.5, 0.5, 1.8), "edge",
              rot=(0, 22 * side, 45), pivot=(x - 0.6 * side, 0, 7.0))
        m.box(bone, (x + 0.8 * side, 0, 5.4), (0.4, 0.4, 1.8), "edge",
              rot=(0, -40 * side, 0), pivot=(x + 0.5 * side, 0, 6.1))
    # glowing hellfire core: slit through the crossbar, ember in the socket,
    # and a vein along the centre prong
    m.box("core", (0, 0, 0.8), (4.2, 0.5, 1.75), "core")
    m.box("core", (0, 0, 0.8), (1.1, 1.1, 1.1), "core", rot=(45, 0, 45))
    m.box("core", (0, 0.68, 3.8), (0.3, 0.12, 3.0), "core")
    m.box("core", (0, -0.68, 3.8), (0.3, 0.12, 3.0), "core")

    zero = lambda t: [0, 0, 0]
    m.clip("spawn", 0.3, "hold_on_last_frame", {
        "root": {"scale": lambda t: [overshoot(t / 0.3)] * 3},
        "prong_l": {"rotation": lambda t: [0, -26 * (1 - ease_out(t / 0.3)), 0]},
        "prong_r": {"rotation": lambda t: [0, 26 * (1 - ease_out(t / 0.3)), 0]},
        "core": {"scale": lambda t: [1 + 0.8 * (1 - t / 0.3)] * 3},
    })
    m.clip("fly", 1.0, True, {
        "root": {"rotation": lambda t: [0, 0, 360 * t]},
        "shaft": {"rotation": lambda t: [1.5 * wave(t, 0.5), 1.5 * wave(t, 0.5, 0.25), 0]},
        "prong_l": {"rotation": lambda t: [0, 4 * wave(t, 0.25), 0]},
        "prong_r": {"rotation": lambda t: [0, -4 * wave(t, 0.25), 0]},
        "prong_c": {"position": lambda t: [0, 0, 0.15 * wave(t, 0.25, 0.5)]},
        "core": {"scale": lambda t: [1 + 0.18 * wave(t, 0.2)] * 3},
    })
    m.clip("end", 0.3, "hold_on_last_frame", {
        "root": {"scale": lambda t: [1 - ease_in(t / 0.3) * 0.95] * 3},
        "prong_l": {"rotation": lambda t: [0, -30 * t / 0.3, 0]},
        "prong_r": {"rotation": lambda t: [0, 30 * t / 0.3, 0]},
        "core": {"scale": lambda t: [1 + 1.2 * t / 0.3] * 3},
    })
    return m


# ================================================================ cyclone_shard
def cyclone_shard():
    m = Model("cyclone_shard", tex=256, bounds=2)
    m.material("steel", "steel", "#2B2F36", "#8A929C", "#E8EEF4")
    m.material("edge", "steel", "#4A5058", "#B9C2CC", "#FFFFFF")
    m.material("fuller", "steel", "#1C2026", "#4E5660", "#9AA4AE")
    m.material("gold", "gold", "#5A3410", "#C98A2A", "#FFE08A")
    m.material("break", "iron", "#1A1C20", "#3C4048", "#7A808A")
    m.bone("root")
    m.bone("blade", "root")
    m.bone("tip", "blade", (0, 0, 9))
    m.bone("chips", "root", (0, 0, -9))
    # stepped body slabs, blade plane vertical (Y), thin in X
    m.box("blade", (0, 0, -3), (1.2, 8.0, 10), "steel")
    m.box("blade", (0, 0, 4.5), (1.1, 6.4, 5), "steel")
    m.box("blade", (0, 0, 8.4), (1.0, 4.4, 3), "steel")
    # fuller groove (sits proud of the faces as a dark inset band)
    m.box("blade", (0, 0.2, -1.5), (1.32, 1.4, 12), "fuller")
    # bevelled edges: diamond rails top and bottom per step
    for z0, z1, h in ((-8, 2, 4.0), (2, 7, 3.2), (7, 9.9, 2.2)):
        for sgn in (1, -1):
            m.box("blade", (0, sgn * h, (z0 + z1) / 2), (0.9, 0.9, z1 - z0), "edge",
                  rot=(0, 0, 45))
    # gold inlay runes along the fuller, both faces
    for i, z in enumerate((-6.2, -3.4, -0.6, 2.2)):
        m.box("blade", (0, 0.2, z), (1.46, 0.9, 1.3), "gold", rot=(i % 2 * 45, 0, 0))
    m.box("blade", (0, -2.4, -6.5), (1.38, 0.5, 2.6), "gold")
    # chisel tip
    m.box("tip", (0, 0, 10.4), (0.95, 2.6, 2.6), "edge", rot=(45, 0, 0))
    m.box("tip", (0, 0, 11.6), (0.7, 1.3, 1.3), "edge", rot=(45, 0, 0))
    # jagged break at the rear
    for y, ln, rx in ((3.0, 2.2, 18), (0.6, 3.0, -12), (-1.9, 1.6, 24), (-3.4, 2.4, -20)):
        m.box("blade", (0, y, -8.0 - ln / 2), (1.15, 1.6, ln), "break",
              rot=(rx, 0, 0), pivot=(0, y, -8.0))
    # loose chips trailing the break (secondary motion)
    m.box("chips", (0.2, 1.6, -10.6), (0.8, 1.1, 1.0), "break", rot=(30, 20, 15))
    m.box("chips", (-0.3, -1.2, -11.3), (0.7, 0.8, 0.9), "steel", rot=(-25, 35, 40))
    m.box("chips", (0.1, 0.2, -12.4), (0.5, 0.6, 0.6), "edge", rot=(45, 10, 45))

    m.clip("spawn", 0.3, "hold_on_last_frame", {
        "root": {"scale": lambda t: [overshoot(t / 0.3)] * 3},
        "blade": {"rotation": lambda t: [0, 0, -180 * (1 - ease_out(t / 0.3))]},
        "chips": {"position": lambda t: [0, 0, -1.5 * (1 - t / 0.3)]},
    })
    m.clip("spin", 0.5, True, {
        "blade": {"rotation": lambda t: [3 * wave(t, 0.5), 2 * wave(t, 0.25), 720 * t]},
        "tip": {"rotation": lambda t: [2 * wave(t, 0.25, 0.3), 0, 0]},
        "chips": {"position": lambda t: [0.4 * wave(t, 0.5), 0.5 * wave(t, 0.5, 0.25),
                                          -0.6 - 0.4 * wave(t, 0.25)],
                  "rotation": lambda t: [360 * t, 180 * t, 0]},
    })
    m.clip("end", 0.3, "hold_on_last_frame", {
        "blade": {"scale": lambda t: [1 - ease_in(t / 0.3) * 0.9] * 3,
                  "rotation": lambda t: [40 * t / 0.3, 0, 200 * t / 0.3]},
        "chips": {"position": lambda t: [3 * t / 0.3, -4 * ease_in(t / 0.3), -5 * t / 0.3],
                  "rotation": lambda t: [300 * t, 120 * t, 90 * t]},
    })
    return m


# ================================================================ rain_sword_tip
def rain_sword_tip():
    m = Model("rain_sword_tip", tex=256, bounds=3)
    m.material("steel", "steel", "#23202C", "#6E6880", "#D9D3EA")
    m.material("edge", "steel", "#3A3548", "#ADA6C2", "#FFFFFF")
    m.material("fuller", "steel", "#120F1A", "#3A3150", "#7D6AA8")
    m.material("break", "iron", "#141218", "#34303E", "#6E6880")
    m.material("vein", "energy", "#000000", "#000000", "#000000",
               glow=("#3B1163", "#7C3AED", "#E9D5FF"))
    m.bone("root")
    m.bone("blade", "root", (0, 2, 0))
    m.bone("vein", "blade", (0, 10, 0))
    m.bone("shards", "root", (0, 22, 0))
    # tapering blade, point down at y=0, broken top near y=22
    m.box("blade", (0, 15.5, 0), (5.2, 11, 1.1), "steel")
    m.box("blade", (0, 7.5, 0), (4.4, 5, 1.05), "steel")
    m.box("blade", (0, 3.6, 0), (3.2, 2.8, 1.0), "steel")
    m.box("blade", (0, 1.4, 0), (2.0, 2.0, 0.95), "edge", rot=(0, 0, 45))
    m.box("blade", (0, 0.4, 0), (1.0, 1.0, 0.8), "edge", rot=(0, 0, 45))
    # diamond edge rails both sides, per step
    for y0, y1, w in ((10, 21, 2.6), (5, 10, 2.2), (2.2, 5, 1.6)):
        for sgn in (1, -1):
            m.box("blade", (sgn * w, (y0 + y1) / 2, 0), (0.8, y1 - y0, 0.8), "edge",
                  rot=(0, 45, 0))
    m.box("blade", (0, 13, 0), (1.3, 15, 1.22), "fuller")
    # jagged break at the top
    for x, ln, rz in ((-2.0, 2.4, 16), (-0.3, 3.2, -10), (1.6, 1.8, 22)):
        m.box("blade", (x, 21 + ln / 2, 0), (1.6, ln, 1.05), "break", rot=(0, 0, rz),
              pivot=(x, 21, 0))
    # violet gravity vein down the fuller (energy, the only glow)
    m.box("vein", (0, 12.5, 0), (0.4, 13, 1.3), "vein")
    m.box("vein", (0, 5.2, 0), (0.3, 2.6, 1.2), "vein", rot=(0, 0, 45))
    # trailing shards above the break
    m.box("shards", (1.2, 24.5, 0.3), (0.9, 1.4, 0.7), "break", rot=(20, 10, 35))
    m.box("shards", (-1.4, 26.0, -0.2), (0.7, 1.0, 0.6), "steel", rot=(-30, 25, -20))

    m.clip("fall", 0.6, True, {
        "blade": {"rotation": lambda t: [2 * wave(t, 0.3), 0, 2.5 * wave(t, 0.6, 0.2)]},
        "vein": {"scale": lambda t: [1 + 0.25 * wave(t, 0.15), 1, 1 + 0.25 * wave(t, 0.15)]},
        "shards": {"position": lambda t: [0.4 * wave(t, 0.3), 1.2 + 0.6 * wave(t, 0.6), 0],
                   "rotation": lambda t: [600 * t, 300 * t, 0]},
    })
    m.clip("impact", 0.4, "hold_on_last_frame", {
        "root": {"position": lambda t: [0, -7 * ease_out(t / 0.12), 0]},
        "blade": {"rotation": lambda t: [9 * ease_out(t / 0.4) + 3 * wave(t, 0.08) * (1 - t / 0.4),
                                         0, -6 * ease_out(t / 0.4)]},
        "vein": {"scale": lambda t: [max(0.05, 1.6 - 1.5 * t / 0.4), 1, max(0.05, 1.6 - 1.5 * t / 0.4)]},
        "shards": {"position": lambda t: [5 * t / 0.4, 2 - 9 * ease_in(t / 0.4), 3 * t / 0.4],
                   "rotation": lambda t: [500 * t, 200 * t, 90 * t]},
    })
    return m


# ================================================================ evil_spike
def evil_spike():
    """Short black spearhead bursting out of the air, outward along +Z.

    Sits at chest height (y 16 to 22) so a burst ring at the caster's
    feet reads as spikes round the body, and tilts up a little.
    """
    m = Model("evil_spike", tex=256, bounds=3)
    m.material("obs", "obsidian", "#050307", "#1A1220", "#6A4A7A")
    m.material("obs_edge", "obsidian", "#0A070C", "#2A2030", "#9A7AA8")
    m.material("ember", "energy", "#000000", "#000000", "#000000",
               glow=("#8F1D1D", "#FF6A00", "#FFE9C4"))
    m.bone("root", None, (0, 18, 0), rot=(-12, 0, 0))
    m.bone("head", "root", (0, 18, 0))
    m.bone("wings", "head", (0, 18, 3))
    m.bone("socket", "root", (0, 18, -1))
    m.bone("ember", "head", (0, 18, 2))
    # tapering spearhead: base, body, chisel point
    m.box("head", (0, 18, 2.5), (3.4, 3.4, 5), "obs", rot=(0, 0, 45))
    m.box("head", (0, 18, 6.6), (2.4, 2.4, 3.6), "obs", rot=(0, 0, 45))
    m.box("head", (0, 18, 9.4), (1.5, 1.5, 2.6), "obs_edge", rot=(0, 0, 45))
    m.box("head", (0, 18, 11.3), (0.8, 0.8, 1.8), "obs_edge", rot=(0, 0, 45))
    # flat cutting blade through the head
    m.box("head", (0, 18, 5.5), (5.6, 0.5, 6.2), "obs_edge")
    m.box("head", (0, 18, 8.9), (3.2, 0.45, 2.4), "obs_edge")
    # swept back wings (barbs)
    for sgn in (1, -1):
        m.box("wings", (sgn * 3.0, 18, 2.6), (0.6, 0.6, 3.6), "obs_edge",
              rot=(0, -30 * sgn, 45), pivot=(sgn * 2.4, 18, 4.0))
        m.box("wings", (sgn * 3.9, 18, 0.6), (0.4, 0.4, 1.8), "obs",
              rot=(0, -48 * sgn, 45), pivot=(sgn * 3.5, 18, 1.4))
    # socket where it tears out of the air: collar, ragged ring
    m.box("socket", (0, 18, -0.6), (2.6, 2.6, 1.6), "obs")
    m.box("socket", (0, 18, -1.6), (2.0, 2.0, 1.0), "obs_edge", rot=(0, 0, 45))
    for i in range(4):
        a = i * 90 + 20
        x, y = math.cos(math.radians(a)) * 2.0, math.sin(math.radians(a)) * 2.0
        m.box("socket", (x, 18 + y, -2.4), (0.7, 0.7, 1.6), "obs",
              rot=(y * 12, -x * 12, a), pivot=(x, 18 + y, -1.6))
    # hellfire seam: thin glowing line in the blade and a socket ember
    m.box("ember", (0, 18, 5.0), (4.2, 0.56, 0.3), "ember")
    m.box("ember", (0, 18, -0.6), (1.3, 1.3, 1.7), "ember", rot=(0, 0, 45))

    m.clip("burst", 0.2, "hold_on_last_frame", {
        "head": {"position": lambda t: [0, 0, -9 * (1 - ease_out(t / 0.2))],
                 "scale": lambda t: [0.6 + 0.4 * overshoot(t / 0.2), 0.6 + 0.4 * overshoot(t / 0.2),
                                     max(0.05, overshoot(t / 0.2))]},
        "wings": {"rotation": lambda t: [0, 0, 0], "scale": lambda t: [max(0.05, ease_out(t / 0.2))] * 3},
        "socket": {"scale": lambda t: [1 + 0.6 * (1 - t / 0.2)] * 3},
        "ember": {"scale": lambda t: [1 + 1.2 * (1 - t / 0.2)] * 3},
    }, step=0.025)
    m.clip("hold", 0.8, True, {
        "head": {"position": lambda t: [0, 0, 0.2 * wave(t, 0.4)]},
        "wings": {"rotation": lambda t: [0, 0, 3 * wave(t, 0.8)]},
        "socket": {"rotation": lambda t: [0, 0, 6 * wave(t, 0.8, 0.3)]},
        "ember": {"scale": lambda t: [1 + 0.2 * wave(t, 0.2)] * 3},
    })
    m.clip("retract", 0.3, "hold_on_last_frame", {
        "head": {"position": lambda t: [0, 0, -8 * ease_in(t / 0.3)],
                 "scale": lambda t: [max(0.05, 1 - ease_in(t / 0.3))] * 3},
        "socket": {"scale": lambda t: [max(0.05, 1 - ease_in(t / 0.3))] * 3},
        "ember": {"scale": lambda t: [max(0.05, 1 - t / 0.3)] * 3},
    })
    return m


# ================================================================ katana_afterimage
def katana_afterimage():
    """Energy afterimage in the iaido follow-through, facing +Z.

    Low lunge, right arm swept out to the right after the draw with
    the katana level, left hand back at the sheath on the hip, coat
    tails and the sash streaming behind. Everything is energy.
    """
    m = Model("katana_afterimage", tex=256, bounds=3)
    m.material("body", "energy", "#000000", "#000000", "#000000",
               glow=("#3B1E73", "#7C3AED", "#D9C4FF"))
    m.material("coat", "energy", "#000000", "#000000", "#000000",
               glow=("#2A1255", "#5B2BB8", "#B399F0"))
    m.material("blade", "energy", "#000000", "#000000", "#000000",
               glow=("#7C3AED", "#B366FF", "#FFFFFF"))
    m.bone("root")
    m.bone("hips", "root", (0, 11, 0), rot=(0, -15, 0))
    m.bone("torso", "hips", (0, 11, 0), rot=(18, 0, 0))
    m.bone("head", "torso", (0, 22, 0.6), rot=(-10, 10, 0))
    m.bone("arm_r", "torso", (-4.5, 20.5, 0), rot=(0, 0, 80))
    m.bone("fore_r", "arm_r", (-4.5, 15, 0), rot=(0, -20, 0))
    m.bone("katana", "fore_r", (-4.5, 10.5, 0), rot=(0, 0, 0))
    m.bone("arm_l", "torso", (4.5, 20.5, 0), rot=(30, 0, -10))
    m.bone("fore_l", "arm_l", (4.5, 15, 0), rot=(-60, 0, 0))
    m.bone("sheath", "hips", (4, 11, 0), rot=(70, -10, 0))
    m.bone("leg_l", "hips", (2, 11, 0), rot=(-50, 0, -6))
    m.bone("shin_l", "leg_l", (2, 5.5, 0), rot=(60, 0, 0))
    m.bone("leg_r", "hips", (-2, 11, 0), rot=(40, 0, 8))
    m.bone("shin_r", "leg_r", (-2, 5.5, 0), rot=(20, 0, 0))
    m.bone("tails", "hips", (0, 11, -2), rot=(28, 0, 0))
    m.bone("sash", "hips", (3, 12, -1), rot=(0, 0, 0))
    m.bone("hair", "head", (0, 27, -1.5), rot=(20, 0, 0))
    # torso, chest plate, shoulders, collar
    m.box("torso", (0, 16, 0), (7.5, 10, 4), "body")
    m.box("torso", (0, 19, 0.4), (8.4, 4.4, 4.4), "coat")
    m.box("torso", (0, 21.2, 0), (5.0, 1.4, 3.2), "coat", rot=(0, 45, 0))
    m.box("torso", (0, 13.0, 0), (7.8, 1.6, 4.4), "coat", rot=(0, 0, 4))
    # head, hair spikes
    m.box("head", (0, 24.6, 0.6), (5, 5, 5), "body")
    m.box("head", (0, 23.0, 2.9), (3, 1.4, 0.6), "coat")
    m.box("hair", (0, 27.6, -1.5), (4.6, 1.6, 3.6), "coat")
    m.box("hair", (-1.6, 28.4, -3.6), (1.2, 1.2, 3.2), "coat", rot=(30, -15, 0))
    m.box("hair", (1.4, 28.2, -3.8), (1.1, 1.1, 3.6), "coat", rot=(24, 18, 0))
    m.box("hair", (0, 27.2, -4.6), (1.4, 1.4, 4.0), "coat", rot=(12, 0, 0))
    # arms
    m.box("arm_r", (-4.5, 17.8, 0), (2.6, 5.6, 2.6), "body")
    m.box("arm_r", (-4.5, 19.6, 0), (3.2, 2.2, 3.2), "coat", rot=(0, 45, 0))
    m.box("fore_r", (-4.5, 12.8, 0), (2.3, 4.8, 2.3), "body")
    m.box("fore_r", (-4.5, 10.6, 0), (2.5, 1.5, 2.5), "body")
    m.box("arm_l", (4.5, 17.8, 0), (2.6, 5.6, 2.6), "body")
    m.box("arm_l", (4.5, 19.6, 0), (3.2, 2.2, 3.2), "coat", rot=(0, 45, 0))
    m.box("fore_l", (4.5, 12.8, 0), (2.3, 4.8, 2.3), "body")
    # katana: grip, guard, long blade continuing past the hand
    m.box("katana", (-4.5, 10.5, 1.8), (1.0, 1.0, 4.0), "body")
    m.box("katana", (-4.5, 10.5, 4.1), (2.6, 2.6, 0.5), "coat", rot=(0, 0, 45))
    m.box("katana", (-4.5, 10.5, 12.5), (0.5, 1.4, 16), "blade", rot=(4, 0, 0),
          pivot=(-4.5, 10.5, 4.4))
    m.box("katana", (-4.5, 10.0, 21.0), (0.45, 1.0, 2.4), "blade", rot=(16, 0, 0),
          pivot=(-4.5, 10.3, 20.2))
    # sheath at the left hip
    m.box("sheath", (4, 11, -5), (1.4, 1.6, 12), "coat")
    m.box("sheath", (4, 11, 1.2), (1.8, 2.0, 0.8), "body")
    # legs
    m.box("leg_l", (2, 8.2, 0), (3, 5.6, 3), "body")
    m.box("shin_l", (2, 2.8, 0), (2.7, 5.6, 2.7), "body")
    m.box("shin_l", (2, 0.3, 0.8), (2.8, 1.2, 4.0), "coat")
    m.box("leg_r", (-2, 8.2, 0), (3, 5.6, 3), "body")
    m.box("shin_r", (-2, 2.8, 0), (2.7, 5.6, 2.7), "body")
    m.box("shin_r", (-2, 0.3, 0.8), (2.8, 1.2, 4.0), "coat")
    # coat tails and sash streaming back (secondary motion)
    m.box("tails", (-1.8, 7.5, -2.2), (3.2, 7, 0.6), "coat", rot=(0, 0, 6))
    m.box("tails", (1.8, 7.2, -2.2), (3.2, 7.6, 0.6), "coat", rot=(0, 0, -6))
    m.box("tails", (0, 4.2, -2.6), (2.0, 3.0, 0.5), "coat", rot=(14, 0, 0))
    m.box("sash", (3.4, 11.5, -4.0), (0.8, 1.2, 6.0), "blade", rot=(-8, 12, 0))
    m.box("sash", (3.8, 10.8, -7.6), (0.6, 1.0, 3.4), "blade", rot=(-18, 20, 0),
          pivot=(3.6, 11.2, -6.8))

    def flutter(amp, period, phase=0.0):
        return lambda t: [amp * wave(t, period, phase), 0.4 * amp * wave(t, period, phase + 0.3), 0]
    m.clip("flash", 0.25, "hold_on_last_frame", {
        "root": {"scale": lambda t: [1.0 + 0.12 * (1 - ease_out(t / 0.25))] * 3,
                 "position": lambda t: [0, 0, 3 * (1 - ease_out(t / 0.25))]},
        "katana": {"rotation": lambda t: [0, -40 * (1 - ease_out(t / 0.25)), 0]},
        "tails": {"rotation": lambda t: [20 * (1 - t / 0.25), 0, 0]},
        "sash": {"rotation": lambda t: [0, 25 * (1 - t / 0.25), 0]},
    }, step=0.025)
    m.clip("linger", 1.0, True, {
        "root": {"position": lambda t: [0.15 * wave(t, 0.5), 0.2 * wave(t, 1.0), 0]},
        "tails": {"rotation": flutter(6, 0.5)},
        "sash": {"rotation": flutter(10, 0.33, 0.2)},
        "hair": {"rotation": flutter(5, 0.5, 0.4)},
        "katana": {"scale": lambda t: [1, 1 + 0.08 * wave(t, 0.25), 1]},
    })
    m.clip("fade", 0.35, "hold_on_last_frame", {
        "root": {"scale": lambda t: [max(0.05, 1 - 0.35 * ease_in(t / 0.35)),
                                     max(0.05, 1 - 0.9 * ease_in(t / 0.35)),
                                     max(0.05, 1 - 0.35 * ease_in(t / 0.35))],
                 "position": lambda t: [0, -2 * ease_in(t / 0.35), -4 * t / 0.35]},
        "head": {"position": lambda t: [0, 1.5 * t / 0.35, -2 * t / 0.35]},
        "arm_r": {"position": lambda t: [-2 * t / 0.35, 0.5 * t / 0.35, -1.5 * t / 0.35]},
        "tails": {"rotation": lambda t: [40 * t / 0.35, 0, 0]},
        "sash": {"position": lambda t: [1.5 * t / 0.35, 1 * t / 0.35, -4 * t / 0.35]},
        "katana": {"scale": lambda t: [1, max(0.05, 1 - t / 0.35), 1]},
    })
    return m


BUILDERS = {f.__name__: f for f in
            (devil_shard, cyclone_shard, rain_sword_tip, evil_spike, katana_afterimage)}

if __name__ == "__main__":
    names = sys.argv[1:] or list(BUILDERS)
    for n in names:
        BUILDERS[n]().write()
