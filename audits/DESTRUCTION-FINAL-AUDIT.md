# Destruction Wiring Final Audit

Pass: wire util/PermanentDestruction into every weapon terrain system and
retire the restore systems, per audits/destruction-expansion-plan.md.
Base: 4b3fdf8 (the no-shapes merge). Ruling behind the pass (Carter,
Nov 4, 2026): weapons may destroy more blocks, destruction does not need
to restore itself, and terrain results may be real permanent blocks.

No Gradle build was run or claimed (the standing VM restriction; the
compile check for this repo is waived, not passed). Verification was the
dual javac audit sweep described at the end. The in-game look is
unverified until Carter's PC test.

## Inventory verification (drift check)

The plan's measured inventory was re-verified against the tree at
4b3fdf8 before editing: exactly 26 GroundScar scar/scarDisk call sites in
the same 23 caller files, the register() call at
DragonBlockArcaneDBA.java line 92, and LavaField calling
GroundScar.isProtected in canOccupy. No drift from the plan.

## Per-step outcomes

Step 1 (lava block art gate): already satisfied by the no-shapes merge
(rebuilt textures and models shipped in 4b3fdf8). Not touched.

Step 2 (pure conversions): every GroundScar scar/scarDisk call in weapon
and item code became PermanentDestruction.convert/convertDisk with
identical sizes, chances, and theme blocks. All 26 call sites migrated.
Where one method also held that weapon's Step 3 crater or fissure (Devil
Slam, King's Slam, Z Slam, Dabura, Curse Blade, Power Pole, Grand Sword,
Hell Hunt, Dimensional Rift, Warp Rift, Azure Quake, Bansho Cyclone,
Spike Flail), the Step 3 work landed in the same edit, because splitting
one method across two steps would have meant editing it twice for no
gain. This is flagged here honestly: steps 2 and 3 are interleaved at
method granularity, sequential at file granularity.

Step 3 (new destruction): Ox scaling, War Mace crater and ring, slam
craters (Devil, Z, King, Brave, Grand spin), Grand Crescent traveling
fissure, Azure quake ring fissures, Blaster trench and endpoint crater,
Ki Explosion and Ki Blast craters, Katana and Evil Spear landing
fissures, Warp Rift and Dimensional Rift mouth craters, Debris monolith
landing crater. Details per weapon below.

Step 4 (lava migration): LavaSwordGround lava is real flowing lava now
(full update flags, never recorded, never restored, neighbor watch and
hold() deleted); fire keeps its exact 80 tick life and its SavedData
crash mirror. LavaField crust settles instead of restoring (POOL to
basalt, CHANNEL to basalt with a 25 percent magma block centerline, WAVE
still vanishes); the volcano ends as a permanent dormant block. Legacy
budgets: LavaMeteor impact scorch budget 0 to 3; LavaEruption gained a
lavaLeft field of 8 spent across its per-tick fire arc. The lava sword's
own swing budgets are unchanged in size; only the lava's behavior
changed (it flows and stays).

Step 5 (retire GroundScar): util/GroundScar.java deleted, its SavedData
type and the register() call removed, and isProtected moved verbatim
into PermanentDestruction. LavaField.canOccupy now calls
PermanentDestruction.isProtected. A grep of src for GroundScar returns
zero references.

Step 6 (verification): dual sweeps at every step boundary and a final
one; numbers below.

## Every weapon's final terrain behavior

Lava Sword (and its field systems): placed lava sources are real lava
that flows and never restores. Fire cells still expire at 80 ticks (a
burn, not terrain). Magma crust POOL cells settle to basalt permanently
at 200 ticks; CHANNEL cells settle to basalt, with 25 percent of the
flown centerline cells settling to magma block (centerline tracked per
cell, saved through the Entry codec as an optional field so old saves
decode); WAVE cells still vanish at 54 ticks (see remainder). The mini
volcano erupts for 600 ticks, then goes dormant and stands permanently;
its field entry closes 25 ticks later. endVolcano remains as the manual
removal path.

Ox King's Ax: the ring destroys as it expands from half charge up (main
wave only, as before). Resistance cap scales with charge,
2.0 + 4.0 * charge (4.0 at half charge, 6.0 at full). At full charge,
inside half radius, the cut is 3 blocks deep instead of 2. Per-wave cap
700 destroyed blocks, drops on, BLOCK_CHUNKS burst per break kept. The
old scar ring is a permanent conversion: 55 percent per-column
application chance, 40 percent magma block and 60 percent basalt when it
applies, both numbers unchanged from the scar version.

War Mace: flagship crater on grounded impacts, radius 3 (4 on the heavy
smash), depthScale 0.6, STANDARD tier, cap 220, no drops. The fracture
ring converts permanently one ring wider than the crater (radius 4, or 5
heavy), with the same fracture mapping and chance falloff as before.

Devil Trident (Devil Slam): crater radius 3, STANDARD, cap 160, punched
first; the zoned brimstone conversions (magma heart, blackstone mid,
soul soil rim) then pave the crater floor and rim, permanently. Theme
flag: the plan's Devil line said "basalt with magma block at 40
percent", which conflicts with the file's own zoned theme; the plan also
says every entry keeps the weapon's current theme block, so the zoned
theme was kept and only the crater was added.

Z Sword (Z Gravity Slam): trench conversions to magma are permanent.
At the strike tick, a crater radius 2.5, STANDARD, cap 120 at the slam
center.

King's Sword (King's Slam): crater radius 2, STANDARD, cap 100 first,
then the permanent magma and basalt disk (radius 5, or 7 flawless)
converts over the crater floor and rim.

Brave Sword (dive shockwave): magma disk conversion permanent, plus a
crater radius 2, WEAK, cap 60 at the contact point.

Grand Sword: spin conversion to coarse dirt permanent, plus a crater
radius 2, WEAK, cap 60 under the spin. Grand Crescent wave: contact
conversion permanent, plus a traveling fissure carved along its ground
track while it flies in ground contact (every 2 ticks, width 1, depth 1,
WEAK, cap 90 per wave).

Blaster Gun (Erasure Cannon): while the beam holds, each damage pulse
carves a trench at the beam's ground contact (fissure length 3 per
pulse, width 1, depth 1, STANDARD, cap 120 per beam). At charge 0.9 and
above, the endpoint detonation adds a crater radius 2.5, STANDARD, cap
120 where the terminus rests on ground.

Ki techniques: Ki Explosion punches a crater under the caster, radius
2.0 at small blasts scaling to 4.5 at the top of the technique scale,
depthScale 0.55, STANDARD tier (HEAVY once the crater reaches full
size), cap 260, no drops; an airborne cast craters nothing. Ki Blast:
only charged blasts crater (see flag below), on block impact, radius
1.5, WEAK, cap 40.

Dabura's Sword: eclipse disks convert permanently to obsidian (full
eclipse disk capped at radius 8, wave disk at radius 6; see flag), plus
a center crater radius 2, WEAK, cap 60 at both ground sites.

Curse Blade: scorch conversions to blackstone are permanent; the
charged strike's scorch radius grew 1 to 2 as the plan specifies; the
ground shatter slam adds a crater radius 2, WEAK, cap 60.

Power Pole: the 3x3 turf tear converts permanently; the descending
strike adds a crater radius 2, WEAK, cap 50 at the landed block.

Katana: Heaven Splitter's landing cuts a fissure forward along the look
direction (length 6, width 1, depth 1, WEAK, cap 30). Flag: Flashdraw
was deliberately not wired; Heaven Splitter is the signature charged
slash with one defined landing, and it is now the one place the katana
touches terrain.

Evil Spear: the lunge landing cuts a fissure (length 4, width 1, depth
1, WEAK, cap 24), whether or not the lunge found a victim. Hell Hunt
impacts convert to obsidian permanently, and the final form adds a
crater radius 2, WEAK, cap 60.

Spirit Sword and Spirit Impale: glass crystal conversions permanent.

Darkness Blade: obsidian impact disk permanent.

Dimensional weapons: Dimensional Slash crying obsidian permanent; Void
Rift obsidian disk permanent and deliberately crater-free (a void rift
consumes, it does not blast); Dimensional Rift soul soil disk permanent
plus a mouth crater radius 2, WEAK, cap 50; Dimensional Warp Rift crying
obsidian disks permanent (arrival radius 2, departure radius 1), plus an
arrival crater radius 2, WEAK, cap 50 (conversion first, then the crater
at its heart).

Azure weapons: Sonic Quake grass conversion permanent, plus ring
fissures at radius 3 and 5 (eight tangential segments each, depth 1,
WEAK, shared budget 80). Tornado: the grass conversion was replaced by
the plan's strip, the top block of each column in the inner disk
destroyed at WEAK tier with drops, cap 200 per tornado life.

Bansho Fan (cyclone): scour conversion to coarse dirt permanent, plus a
surface tear-off: leaves, flowers, snow, and ground plants within 3
blocks above the soil line are destroyed at WEAK tier with drops, cap
150 per cyclone.

Spike Flail: the spin tears soft surface blocks up outright (WEAK,
drops, cap 60 per spin) and fractures harder floors to gravel
permanently.

Shared debris (DebrisEntity): monolith-class pieces dent the ground on
landing (crater radius 2, WEAK, cap 60, no drops). Flag: no monolith
entity exists in the code; monoliths are renderer visuals in King's Slam
and Z Slam, and DebrisEntity is the only falling physical block system.
The crater is gated to the largest debris size class (scale 1.0 pieces)
only; ungated, every debris field in the mod would out-destroy the
slams themselves.

Saber and Hollow's Edge: no terrain calls, unchanged, terrain-free by
identity as the plan requires.

## What still restores or vanishes, and why

Only two things. LavaSwordGround fire cells restore to air at 80 ticks:
fire is a short burn visual, not a terrain result, and the ruling covers
terrain. LavaField WAVE crust cells vanish at the end of their 54 tick
life: a permanent basalt stripe under every cleave path would paint the
world; cells upgraded to CHANNEL by the carve settle permanently
instead, as the plan specifies. Nothing else in the mod restores terrain
anymore.

## Lava settling confirmation

POOL crust settles to basalt. CHANNEL crust settles to basalt with a 25
percent magma block centerline. WAVE crust vanishes. Legacy
LavaSwordGround lava is real flowing lava, never restored; saved lava
records from before the expansion are dropped at adoption without
touching the world. The volcano ends dormant and permanent. Shutdown
and crash semantics: both lava systems now settle only overdue entries
at shutdown and leave live schedules recorded in SavedData for adoption
on the next start, so stage and expiry timing stays exact across
restarts instead of being torn out early. This replaces the old
restore-everything shutdown, which permanence makes meaningless.

## Interpretation flags (all of them)

1. Steps 2 and 3 interleaved at method granularity (see Step 2 above).
2. Devil Slam kept its zoned brimstone theme over the plan's basalt
   phrasing (see Devil Trident above).
3. Dabura's reduced radii (domain 8, wave 6) were applied as caps on the
   existing radius formulas, preserving the small-domain scaling instead
   of forcing flat radii.
4. Ox "convertDisk at 40 percent" was read as the existing magma share
   of the ring conversion; the 55 percent application chance and 40
   percent magma share are both unchanged, as Step 2 requires identical
   sizes and chances.
5. Ox destruction scaling applies inside the existing ground-work gate
   (charge at least 0.50, main wave only); the plan's formula does not
   name a new gate, and the gate is a mechanic.
6. Ki Blast "charged" is gated on entity scale above 1.0 (the default
   technique size): KiBlastEntity carries no charge flag, and scale is
   the only charge signal it has.
7. Debris monolith crater gated to scale 1.0 pieces (see Shared debris).
8. Katana wired on Heaven Splitter only; Flashdraw left terrain-free.
9. Warp Rift arrival: conversion disk first, crater second (its
   conversion seeks each column's own surface, so the crater at the
   heart is not refilled either way).

## Verification

Baseline measured fresh at 4b3fdf8 in a worktree: 132 unique error kinds
(26.2 classpath), 138 (26.3 classpath). Dual sweeps after each step
boundary and a final sweep: 132 and 138 at every checkpoint, with
comm -13 empty on both classpaths every time (zero new error kinds; the
kind sets are identical to baseline). git diff --check clean. Added
lines em dash scan: zero. No JSON or resource files were changed in
this pass, so the JSON parse gate has nothing to check. No Gradle build
was run. In-game look and feel are unverified until Carter's PC test.

## Honest remainder

- Existing worlds keep an orphaned ground_scar SavedData record with no
  reader left; it is inert data in the world file, nothing reads or
  writes it anymore.
- Old lava_field saves decode through the optional centerline field and
  settle as non-centerline cells; old lava_sword_ground saves keep
  decoding, with lava entries dropped at adoption and fire entries
  resuming exactly.
- The lava damage family's lack of an is_fire tag is untouched; it
  remains Carter's design call from the earlier pass, not something this
  pass was asked to change.
- Destruction amounts are the plan's numbers, applied as written; if
  any weapon's terrain now reads too strong or too weak in game, that is
  a tuning call for Carter after the PC test, not something this pass
  pre-judged.
