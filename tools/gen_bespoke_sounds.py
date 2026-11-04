#!/usr/bin/env python3
"""Procedurally synthesize the bespoke pass weapon sound bank.

Every weapon sound in the mod comes from here: numpy synthesis to WAV,
ffmpeg to .ogg (vorbis), registered by sound/DbaSounds.java and listed
in assets/dragonblockarcanedba/sounds.json. Names are by USE CASE, with
theme variants where weapons used to fake a theme with a different
vanilla sound. Deterministic seed, so regeneration is reproducible.
The seven non-weapon sounds already in sounds.json (transforms, race
hurts, space pod, kamehameha) are preserved and not regenerated here.

Run: python3 tools/gen_bespoke_sounds.py   (needs numpy and ffmpeg)
"""
import json
import os
import subprocess
import sys
import wave

import numpy as np

SR = 44100
RNG = np.random.default_rng(20261103)
ASSETS = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                      "src", "main", "resources", "assets", "dragonblockarcanedba")
SND_DIR = os.path.join(ASSETS, "sounds")
TMP = "/tmp/bespoke_snd"


def tt(d):
    return np.arange(int(SR * d)) / SR


def noise(d):
    return RNG.standard_normal(int(SR * d))


def adsr(n, a=0.01, d=0.1, s=0.6, r=0.25):
    e = np.ones(n)
    na, nd, nr = max(1, int(a * SR)), max(1, int(d * SR)), max(1, int(r * SR))
    e[:na] = np.linspace(0, 1, na)
    if nd < n:
        e[na:na + nd] = np.linspace(1, s, nd)
    e[-nr:] *= np.linspace(1, 0, nr)
    return e


def fade(n, a=0.01, r=0.3):
    e = np.ones(n)
    na, nr = max(1, int(a * SR)), max(1, int(r * SR))
    e[:na] = np.linspace(0, 1, na)
    e[-nr:] *= np.linspace(1, 0, nr)
    return e


def lp(x, k):
    k = max(1, int(k))
    return np.convolve(x, np.ones(k) / k, "same")


def hp(x, k):
    return x - lp(x, k)


def sweep(f0, f1, d, shape="sin"):
    f = np.geomspace(max(1.0, f0), max(1.0, f1), int(SR * d))
    ph = 2 * np.pi * np.cumsum(f) / SR
    if shape == "sin":
        return np.sin(ph)
    if shape == "saw":
        return 2 * (ph / (2 * np.pi) % 1) - 1
    return np.sign(np.sin(ph))


def tone(freq, d, shape="sin", glide=0.0):
    x = tt(d)
    f = freq * (1 + glide * x / d)
    ph = 2 * np.pi * np.cumsum(f) / SR
    if shape == "saw":
        return 2 * (ph / (2 * np.pi) % 1) - 1
    if shape == "square":
        return np.sign(np.sin(ph))
    return np.sin(ph)


def mix(*xs):
    n = max(len(x) for x in xs)
    out = np.zeros(n)
    for x in xs:
        out[:len(x)] += x
    return out


def pad(x, delay, total=None):
    n = int(SR * delay)
    out = np.concatenate([np.zeros(n), x])
    return out if total is None else out[:int(SR * total)]


def boom(d=1.2, f=50.0, grit=0.35):
    x = tt(d)
    sub = np.sin(2 * np.pi * f * x * np.exp(-x * 1.6)) * np.exp(-x * 2.6)
    wash = lp(noise(d), 60) * np.exp(-x * 2.2) * grit
    crack = lp(noise(0.2), 2) * np.exp(-tt(0.2) * 18) * 0.7
    return mix(sub, wash, crack)


def whoosh(d=0.4, k0=4, k1=26, rise=0.45):
    x = noise(d)
    env = np.sin(np.pi * np.clip(tt(d) / d, 0, 1)) ** 1.5
    # time-varying lowpass: sweep the smoothing window by segment
    out = np.zeros_like(x)
    segs = 24
    seg = len(x) // segs
    for i in range(segs + 1):
        k = k0 + (k1 - k0) * abs(i / segs - rise) * 2
        s = lp(x, max(1, int(k)))
        a, b = i * seg, min(len(x), (i + 1) * seg)
        out[a:b] = s[a:b]
    return out * env


def pluck(freqs, d=0.6, decays=None, gain=0.5):
    x = tt(d)
    out = np.zeros(len(x))
    for i, fq in enumerate(freqs):
        dec = (decays[i] if decays else 6.0)
        out += np.sin(2 * np.pi * fq * x) * np.exp(-x * dec) * (gain / (i + 1))
    return out


def loopify(x, cross=0.25):
    """Crossfade the tail into the head so the clip loops seamlessly."""
    n = int(SR * cross)
    if len(x) <= 2 * n:
        return x
    head = x[:n] * np.linspace(0, 1, n) + x[-n:] * np.linspace(1, 0, n)
    out = np.concatenate([head, x[n:-n]])
    return out * fade(len(out), 0.005, 0.005)


def rumble(d=2.2, f=38.0):
    x = tt(d)
    body = np.sin(2 * np.pi * f * x) * (0.6 + 0.4 * np.sin(2 * np.pi * 0.7 * x))
    grit = lp(noise(d), 90) * (0.5 + 0.5 * np.sin(2 * np.pi * 1.3 * x + 1.0))
    env = np.clip(x / 0.25, 0, 1) * np.exp(-np.clip(x - d * 0.45, 0, None) * 1.1)
    return (body * 0.8 + grit * 0.5) * env


SOUNDS = {}

# --- regenerated core (names already registered before this pass) ---
SOUNDS["ability_charge"] = mix(sweep(200, 900, 1.2) * 0.30, lp(noise(1.2), 3) * np.linspace(0.02, 0.22, int(SR * 1.2))) * fade(int(SR * 1.2), 0.15, 0.25)
SOUNDS["blade_shing"] = mix(pluck([3150, 4720, 6130, 7980], 0.35, [16, 20, 24, 30], 0.55), hp(noise(0.12), 2) * np.exp(-tt(0.12) * 30) * 0.4)
SOUNDS["blaster_shot"] = mix(sweep(1400, 280, 0.18, "square") * 0.32, sweep(2600, 500, 0.12) * 0.2, hp(noise(0.05), 1) * 0.3) * fade(int(SR * 0.18), 0.004, 0.06)
SOUNDS["dark_pulse"] = mix(tone(68, 0.8) * (0.7 + 0.3 * np.sin(2 * np.pi * 6 * tt(0.8))), lp(noise(0.8), 50) * 0.4) * fade(int(SR * 0.8), 0.02, 0.4)
SOUNDS["heavy_impact"] = boom(0.9, 52, 0.5)
SOUNDS["lightning_zap"] = mix(hp(noise(0.3), 1) * np.exp(-tt(0.3) * 9) * 0.8, sweep(3000, 700, 0.22, "square") * 0.25 * np.exp(-tt(0.22) * 8))
_d = 1.5
SOUNDS["saber_hum"] = loopify(mix(tone(110, _d, "saw") * 0.16, tone(220, _d) * 0.22, tone(331, _d) * 0.08) * (0.85 + 0.15 * np.sin(2 * np.pi * 2.2 * tt(_d))))
SOUNDS["slash_whoosh"] = whoosh(0.35, 3, 22)
_ts = mix(pluck([1560, 1170, 780], 1.2, [3.5, 4.5, 6.0], 0.5), pad(sweep(2400, 120, 1.1) * 0.18, 0.1))
SOUNDS["time_stop"] = _ts * fade(len(_ts), 0.01, 0.5)
SOUNDS["wind_gust"] = mix(lp(noise(1.2), 14) * np.sin(np.pi * tt(1.2) / 1.2) ** 1.4, lp(noise(1.2), 40) * 0.5) * fade(int(SR * 1.2), 0.2, 0.4)

# --- swings and hits ---
SOUNDS["swing_light"] = whoosh(0.24, 2, 30)
SOUNDS["swing_heavy"] = mix(whoosh(0.5, 8, 30), lp(noise(0.5), 55) * np.sin(np.pi * tt(0.5) / 0.5) * 0.6)
SOUNDS["swing_fan"] = whoosh(0.42, 4, 24) * (0.75 + 0.25 * np.sign(np.sin(2 * np.pi * 11 * tt(0.42))))
SOUNDS["hit_flesh"] = mix(tone(130, 0.2, "sin", -0.5) * np.exp(-tt(0.2) * 14), lp(noise(0.12), 3) * np.exp(-tt(0.12) * 22) * 0.8) * fade(int(SR * 0.2), 0.003, 0.08)
SOUNDS["hit_metal"] = mix(pluck([880, 1390, 2270, 3410], 0.3, [14, 17, 22, 28], 0.5), lp(noise(0.06), 2) * 0.5 * np.exp(-tt(0.06) * 30))
SOUNDS["crit_hit"] = mix(pluck([2350, 3520], 0.3, [10, 13], 0.5), pad(pluck([4700], 0.2, [12], 0.3), 0.05))
SOUNDS["mark_hit"] = mix(tone(466, 0.3) * np.exp(-tt(0.3) * 7) * 0.4, tone(440, 0.3) * np.exp(-tt(0.3) * 7) * 0.4, lp(noise(0.3), 30) * 0.3)

# --- charges, releases, beams ---
SOUNDS["charge_rise"] = mix(sweep(150, 1250, 1.0) * 0.3, sweep(300, 2500, 1.0) * 0.12, lp(noise(1.0), 4) * np.linspace(0.02, 0.16, SR)) * fade(SR, 0.06, 0.12)
_d = 2.0
SOUNDS["charge_hum"] = loopify(mix(tone(90, _d) * 0.32, tone(180, _d) * 0.16, tone(271, _d) * 0.06) * (0.88 + 0.12 * np.sin(2 * np.pi * 1.5 * tt(_d))))
SOUNDS["charge_full"] = mix(pluck([1318, 1975, 2637], 0.55, [7, 8, 10], 0.45), lp(noise(0.3), 2) * 0.12 * np.exp(-tt(0.3) * 8))
SOUNDS["charge_dark"] = mix(sweep(95, 480, 1.0, "saw") * 0.20, sweep(101, 505, 1.0, "saw") * 0.20, lp(noise(1.0), 60) * np.linspace(0.05, 0.3, SR)) * fade(SR, 0.08, 0.15)
SOUNDS["release_blast"] = mix(boom(0.6, 70, 0.45), whoosh(0.4, 3, 18) * 0.7)
SOUNDS["release_dark"] = mix(boom(0.7, 45, 0.3), lp(noise(0.7), 70) * np.exp(-tt(0.7) * 3) * 0.6)
SOUNDS["beam_charge"] = mix(sweep(100, 820, 1.2, "saw") * 0.22, sweep(1500, 3400, 1.2) * 0.10 * np.linspace(0.2, 1, int(SR * 1.2)), lp(noise(1.2), 3) * 0.14 * np.linspace(0, 1, int(SR * 1.2))) * fade(int(SR * 1.2), 0.05, 0.1)
SOUNDS["beam_fire"] = mix(tone(196, 0.8, "saw") * 0.24, tone(294, 0.8, "saw") * 0.16, lp(noise(0.8), 6) * 0.5, sweep(2400, 900, 0.8) * 0.10) * fade(int(SR * 0.8), 0.01, 0.2)

# --- booms, slams, ground ---
SOUNDS["boom_big"] = boom(1.6, 42, 0.5)
SOUNDS["boom_sonic"] = mix(np.sin(2 * np.pi * (120 - 90 * tt(1.8) / 1.8) * tt(1.8)) * np.exp(-tt(1.8) * 1.5) * 0.9, lp(noise(1.8), 80) * np.exp(-tt(1.8) * 1.8) * 0.5)
SOUNDS["boom_holy"] = mix(boom(1.2, 55, 0.3), pad(pluck([1567, 2093, 2637], 0.9, [4, 5, 6], 0.30), 0.08))
SOUNDS["slam_thud"] = mix(tone(56, 0.5, "sin", -0.35) * np.exp(-tt(0.5) * 7), lp(noise(0.3), 8) * np.exp(-tt(0.3) * 10) * 0.7)
SOUNDS["ground_crack"] = mix(*[pad(lp(noise(0.07), 2) * np.exp(-tt(0.07) * 26) * (0.8 - i * 0.13), 0.05 * i + 0.02 * RNG.random()) for i in range(5)], lp(noise(0.45), 45) * np.exp(-tt(0.45) * 5) * 0.4)

# --- thunder and zaps ---
_crack = hp(noise(0.16), 1) * np.exp(-tt(0.16) * 14) * 0.9
SOUNDS["thunder_crack"] = mix(_crack, pad(rumble(2.2, 40), 0.07))
SOUNDS["thunder_dark"] = mix(hp(noise(0.2), 1) * np.exp(-tt(0.2) * 11) * 0.7, pad(rumble(2.6, 30), 0.09))
SOUNDS["zap_dark"] = mix(hp(noise(0.3), 1) * np.exp(-tt(0.3) * 9) * 0.7, sweep(1900, 420, 0.24, "square") * 0.2 * np.exp(-tt(0.24) * 8), tone(98, 0.3, "saw") * 0.2 * np.exp(-tt(0.3) * 6))

# --- movement, warps, ambience ---
_rev = whoosh(0.35, 20, 3)
SOUNDS["teleport_whoosh"] = _rev * fade(len(_rev), 0.02, 0.1)
_d = 3.0
SOUNDS["rift_ambient"] = loopify(mix(tone(220, _d) * 0.14, tone(233, _d) * 0.14, tone(110, _d) * 0.12, lp(noise(_d), 30) * 0.10 * (0.7 + 0.3 * np.sin(2 * np.pi * 0.4 * tt(_d)))))
SOUNDS["rift_open"] = mix(sweep(180, 1400, 0.9) * 0.22, sweep(190, 1330, 0.9) * 0.22, lp(noise(0.9), 10) * np.sin(np.pi * tt(0.9) / 0.9) * 0.4) * fade(int(SR * 0.9), 0.05, 0.25)
_d = 2.5
SOUNDS["flight_wind"] = loopify(lp(noise(_d), 12) * (0.75 + 0.25 * np.sin(2 * np.pi * 0.8 * tt(_d))) * 0.8 + lp(noise(_d), 35) * 0.3)
_d = 3.0
SOUNDS["void_ambient"] = loopify(mix(tone(65, _d) * 0.3, tone(98, _d) * 0.18, tone(65 * 1.007, _d) * 0.3, lp(noise(_d), 60) * 0.14 * (0.6 + 0.4 * np.sin(2 * np.pi * 0.3 * tt(_d)))))

# --- fire and lava ---
SOUNDS["fire_shot"] = mix(lp(noise(0.35), 5) * np.exp(-tt(0.35) * 6) * 0.8, tone(160, 0.18, "sin", -0.5) * np.exp(-tt(0.18) * 12) * 0.5)
SOUNDS["fire_ignite"] = lp(noise(0.4), 4) * np.sin(np.pi * tt(0.4) / 0.4) ** 0.7 * 0.8
_bub = np.zeros(int(SR * 0.5))
for _i in range(7):
    _st = int(SR * (0.03 + 0.065 * _i))
    _ln = int(SR * 0.055)
    _seg = np.sin(2 * np.pi * (300 + 260 * RNG.random()) * tt(0.055)) * np.exp(-tt(0.055) * 30)
    _bub[_st:_st + _ln] += _seg[:max(0, len(_bub) - _st)]
SOUNDS["lava_bubble"] = lp(_bub, 6) * 1.4 * fade(len(_bub), 0.01, 0.1)
SOUNDS["extinguish"] = hp(noise(0.5), 6) * np.exp(-tt(0.5) * 5) * 0.55

# --- crystal, chains, trident, misc use cases ---
SOUNDS["crystal_chime"] = mix(pluck([2637, 3951, 5274], 0.6, [9, 11, 14], 0.4), pad(pluck([3951], 0.4, [10], 0.18), 0.07))
SOUNDS["glass_crack"] = mix(hp(noise(0.1), 1) * np.exp(-tt(0.1) * 26), pad(pluck([3400, 4600], 0.25, [18, 22], 0.3), 0.02), pad(hp(noise(0.08), 1) * np.exp(-tt(0.08) * 22) * 0.6, 0.09))
SOUNDS["chain_rattle"] = mix(*[pad(mix(lp(noise(0.05), 2) * np.exp(-tt(0.05) * 35), pluck([1900 + 500 * RNG.random()], 0.09, [26], 0.22)), 0.06 * i + 0.015 * RNG.random()) for i in range(6)])
SOUNDS["trident_throw"] = mix(whoosh(0.3, 2, 20), sweep(500, 1500, 0.25) * 0.10 * fade(int(SR * 0.25), 0.02, 0.1))
SOUNDS["trident_hit"] = mix(tone(140, 0.25, "sin", -0.4) * np.exp(-tt(0.25) * 12) * 0.8, pluck([720, 1180], 0.28, [13, 17], 0.35))
SOUNDS["cast_magic"] = mix(*[pad(pluck([f], 0.55, [5.5], 0.4), 0.09 * i) for i, f in enumerate([880, 1174, 1568, 2093])])
SOUNDS["cast_dark"] = mix(tone(110, 0.9) * 0.3, tone(130.8, 0.9) * 0.26, tone(164.8, 0.9) * 0.2, lp(noise(0.9), 50) * 0.25) * fade(int(SR * 0.9), 0.12, 0.35)
_gl = sweep(1400, 300, 0.9) * 0.22
SOUNDS["soul_drift"] = mix(_gl, lp(noise(0.9), 16) * np.sin(np.pi * tt(0.9) / 0.9) * 0.35) * fade(int(SR * 0.9), 0.1, 0.3)
SOUNDS["shield_clank"] = mix(pluck([1250, 1980, 2890], 0.22, [16, 20, 26], 0.45), lp(noise(0.05), 2) * 0.5 * np.exp(-tt(0.05) * 32))
SOUNDS["item_shatter"] = mix(hp(noise(0.09), 1) * np.exp(-tt(0.09) * 24), *[pad(lp(noise(0.05), 3) * np.exp(-tt(0.05) * 20) * (0.5 - i * 0.1), 0.1 + 0.06 * i) for i in range(3)])
SOUNDS["fizzle"] = mix(tone(190, 0.13, "square") * np.exp(-tt(0.13) * 10) * 0.22, lp(noise(0.04), 2) * 0.5)
SOUNDS["thunk"] = mix(tone(180, 0.18, "sin", -0.45) * np.exp(-tt(0.18) * 16), lp(noise(0.08), 4) * np.exp(-tt(0.08) * 24) * 0.7)
_hb = mix(tone(55, 0.16, "sin", -0.2) * np.exp(-tt(0.16) * 13), pad(tone(50, 0.2, "sin", -0.2) * np.exp(-tt(0.2) * 11) * 0.85, 0.28))
SOUNDS["heartbeat_dark"] = pad(_hb, 0.0, 0.95)

# --- scope 2 additions (appended after the first bank so the deterministic
# RNG stream for every earlier sound is unchanged) ---


def _voice(f0, f1, d, grit=0.22, vib=7.0):
    """Stylized creature voice: pitch-gliding saw through a soft lowpass with
    tremolo and a breath of noise. The mob voice sets are built from it."""
    x = sweep(f0, f1, d, "saw")
    trem = 0.72 + 0.28 * np.sin(2 * np.pi * vib * tt(d))
    return mix(lp(x, 4) * trem * 0.85, lp(noise(d), 26) * grit) * fade(int(SR * d), 0.03, 0.14)


# UI and progression
SOUNDS["ui_click"] = mix(lp(noise(0.035), 1) * np.exp(-tt(0.035) * 55) * 0.7,
                         tone(1900, 0.05) * np.exp(-tt(0.05) * 55) * 0.30)
SOUNDS["radar_blip"] = mix(tone(1568, 0.16) * np.exp(-tt(0.16) * 11) * 0.5,
                            tone(3136, 0.16) * np.exp(-tt(0.16) * 15) * 0.14)
SOUNDS["level_up"] = mix(*[pad(pluck([f], 1.1, [4.5], 0.42), 0.09 * i)
                           for i, f in enumerate([880.0, 1108.7, 1318.5, 1760.0])],
                         pad(hp(noise(0.5), 2) * np.exp(-tt(0.5) * 7) * 0.10, 0.3))
SOUNDS["equip"] = mix(lp(noise(0.22), 7) * np.sin(np.pi * tt(0.22) / 0.22) * 0.55,
                      pad(pluck([2350], 0.12, [20], 0.30), 0.10),
                      pad(tone(210, 0.09, "sin", -0.3) * np.exp(-tt(0.09) * 16) * 0.5, 0.12))
SOUNDS["repair"] = mix(*[pad(lp(noise(0.03), 2) * np.exp(-tt(0.03) * 40) * 0.6, 0.09 * i) for i in range(4)],
                       sweep(280, 660, 0.38) * 0.10 * fade(int(SR * 0.38), 0.05, 0.1),
                       pad(pluck([1500, 2100], 0.16, [18, 22], 0.30), 0.40))

# Dragons and the wish
SOUNDS["dragon_growl"] = mix(sweep(82, 52, 2.4, "saw") * (0.70 + 0.30 * np.sin(2 * np.pi * 8 * tt(2.4))) * 0.5,
                             lp(noise(2.4), 70) * 0.32, tone(39, 2.4) * 0.42) * fade(int(SR * 2.4), 0.15, 0.5)
SOUNDS["dragon_flap"] = mix(whoosh(0.55, 10, 30), pad(whoosh(0.55, 10, 30) * 0.85, 0.55),
                            pad(whoosh(0.55, 10, 30) * 0.68, 1.10))
SOUNDS["wish_granted"] = mix(*[pad(pluck([f], 1.7, [2.6], 0.40), 0.13 * i)
                               for i, f in enumerate([523.25, 659.25, 783.99, 1046.5])],
                             pad(hp(noise(1.0), 2) * np.exp(-tt(1.0) * 3.5) * 0.10, 0.45),
                             boom(1.5, 58, 0.14) * 0.55)

# Mob voices (D2): ambient, hurt, death per species
SOUNDS["soldier_ambient"] = mix(_voice(300, 215, 0.16), pad(_voice(275, 195, 0.15), 0.23))
SOUNDS["soldier_hurt"] = _voice(430, 235, 0.22, grit=0.3)
SOUNDS["soldier_death"] = mix(_voice(390, 65, 0.95, vib=5.0), lp(noise(0.95), 55) * np.exp(-tt(0.95) * 3) * 0.3)
SOUNDS["warrior_ambient"] = mix(_voice(135, 105, 0.38, vib=5.0), tone(68, 0.38) * 0.25 * fade(int(SR * 0.38), 0.05, 0.12))
SOUNDS["warrior_hurt"] = _voice(185, 115, 0.28, grit=0.28)
SOUNDS["warrior_death"] = _voice(205, 52, 1.15, vib=4.5)
SOUNDS["robot_ambient"] = mix(tone(1180, 0.06, "square") * np.exp(-tt(0.06) * 22) * 0.22,
                              pad(tone(880, 0.06, "square") * np.exp(-tt(0.06) * 22) * 0.22, 0.11),
                              lp(noise(0.2), 3) * 0.05)
SOUNDS["robot_hurt"] = mix(pluck([1240, 1870, 2600], 0.2, [16, 19, 24], 0.4),
                           sweep(920, 300, 0.2, "square") * 0.16 * np.exp(-tt(0.2) * 6))
SOUNDS["robot_death"] = mix(sweep(720, 48, 0.95, "square") * 0.26 * fade(int(SR * 0.95), 0.01, 0.25),
                            lp(noise(0.7), 20) * np.exp(-tt(0.7) * 4) * 0.25,
                            pad(pluck([980, 1470], 0.25, [14, 18], 0.35), 0.72))
SOUNDS["saibaman_ambient"] = mix(*[pad(sweep(1550, 2150, 0.055, "saw") * np.exp(-tt(0.055) * 6) * (0.20 - i * 0.02), 0.075 * i) for i in range(5)])
SOUNDS["saibaman_hurt"] = mix(sweep(1520, 780, 0.3, "saw") * 0.30, hp(noise(0.3), 2) * np.exp(-tt(0.3) * 10) * 0.25)
SOUNDS["saibaman_death"] = mix(sweep(1750, 280, 0.7, "saw") * 0.34,
                               pad(lp(noise(0.09), 2) * np.exp(-tt(0.09) * 24) * 0.7, 0.58))


def write_ogg(name, samples):
    x = np.clip(samples, -1, 1)
    peak = np.max(np.abs(x)) if len(x) else 0.0
    if peak > 0.92:
        x = x * (0.92 / peak)
    pcm = (x * 32767).astype(np.int16)
    wav_path = os.path.join(TMP, name + ".wav")
    with wave.open(wav_path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    ogg_path = os.path.join(SND_DIR, name + ".ogg")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav_path,
                    "-c:a", "libvorbis", "-q:a", "4", ogg_path], check=True)
    return ogg_path


def main():
    os.makedirs(SND_DIR, exist_ok=True)
    os.makedirs(TMP, exist_ok=True)
    names = sorted(SOUNDS)
    for name in names:
        write_ogg(name, SOUNDS[name])
        print("wrote", name)
    sj_path = os.path.join(ASSETS, "sounds.json")
    with open(sj_path) as fh:
        data = json.load(fh)
    for name in names:
        data[name] = {"sounds": [f"dragonblockarcanedba:{name}"]}
    with open(sj_path, "w") as fh:
        json.dump(dict(sorted(data.items())), fh, indent=2)
        fh.write("\n")
    print("sounds.json now has", len(data), "entries;", len(names), "from the bespoke bank")


if __name__ == "__main__":
    sys.exit(main())
