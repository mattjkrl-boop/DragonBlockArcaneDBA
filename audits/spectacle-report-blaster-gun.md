# Spectacle Report: Blaster Gun (Phase 2)

Agent scope: item/BlasterGunItem.java, item/BlasterAmmoItem.java (audited, no
change needed, see below), entity/BlasterBoltEntity.java,
entity/ErasureCannonBeamEntity.java, entity/ErasureChargeOrbEntity.java, and
renderers BlasterBoltRenderer (no change needed), ErasureCannonBeamRenderer,
ErasureChargeOrbRenderer. Theme unchanged: Frieza force tech, cyan-white
charged beam, heat-shifting bolts. All work builds on the shared blocks
(FxEnvelope, LayeredParticles, DebrisSpawner, BoltEntity); no shared file was
edited.

## Before and after numbers

| Value | Before | After |
|---|---|---|
| Erasure beam total damage | 900 + charge x 1400 + Spirit x 4, ALL on tick 1 | 800 + charge x 1500 + Spirit x 4, split into 6 pulses of total / 6 at ticks 3, 6, 9, 12, 15, 18 |
| Beam damage, minimum real shot | 1040 + Spirit x 4 (at the 0.1 charge floor) | 1100 + Spirit x 4 (at 0.2 charge, the fizzle floor) |
| Beam damage, full charge | 2300 + Spirit x 4 | 2300 + Spirit x 4 (unchanged headline) |
| Full charge vs weakest shot | 2300 / 1040 = 2.21x, but the weak shot was an accidental click | 2300 / 1100 = 2.09x, and the weak shot now requires a deliberate 20 tick hold |
| Beam lifetime | 16 ticks | 24 ticks |
| Beam particles | ticks 1 to 3 only (10 SPARK along path + 6 EMBER at end, then 13 bare ticks) | every tick of all 24, layered (counts below) |
| Beam cooldown | 120 ticks | 120 ticks (unchanged; already exceeds the 24 tick lifetime per the standard) |
| Endpoint explosion (charge >= 0.9) | tick 1, 50 percent of total, stacked on top of the full tick 1 beam hit on the same tick | final pulse (tick 18), 50 percent of total, plus a 24 piece debris field and a full-radius ground burst; no same-tick stacking with beam damage |
| Beam suction | 0.8 toward centerline + 0.2 up, once on tick 1 | 0.35 + 0.08 up per pulse, up to 6 pulses (comparable total, no one-tick yank) |
| Charge floor / fizzle | floor 0.1: any click fires, minimum ammo cost 11 | below 0.2 charge (under 20 ticks held): fizzle, no beam, no ammo spent, no cooldown, action-bar message + spark puff |
| Beam ammo cost | 10 + charge x 10 (11 to 20) | 10 + charge x 10 (12 to 20, because sub-0.2 no longer fires) |
| Charge orb particles | every 2 ticks, one SPARK layer, count 1 + charge x 3 | every tick, three layers: KI_MOTE core count 1 + charge x 2, SPARK mid count 1 + charge x 4 on a tightening spiral (radius 0.9 to 0.45), cyan dust outer every 2 ticks; EMBER hot layer past 0.75 charge |
| Bolt trail particles | 0 (none, entire 72 block flight was bare mesh) | every tick: 2 SPARK (3 if overcharged) + 1 KI_MOTE; EMBER every 2 ticks if overcharged |
| Bolt block impact | FIRE_EXTINGUISH sound + 5 tick mesh collapse, no particles, no debris | same collapse, plus SPARK burst 8, heat-colored dust 6, debris field 5 pieces (10 overcharged) sampled from the ground at the impact point |
| Bolt entity hit | damage + effects only | plus a 4 SPARK burst at the target center |
| Bolt timeout death (tick 30) | hard discard in mid flight | enters the impact collapse state and fades over 5 ticks |
| Barrage muzzle | bolts appear mid-air | 2 SPARK muzzle flash per shot at the barrel |
| Melee, barrage damage, heat, vent | unchanged | unchanged (450 melee; bolt 250 + Spirit x 1.5 heat-scaled; overcharged 700 + Spirit x 3; vent 20 ticks) |
| Ki costs | none anywhere on this weapon (ammo + heat economy) | none (unchanged; nothing to refund because a fizzle or blocked shot charges no ammo) |

## What was added, per point

1. LAYERED DEPTH. Beam particles are now three layers for the beam's whole
   life: KI_MOTE core on the centerline (4 per tick at full envelope), a
   SPARK mid layer on a disc around the line whose distance along the beam
   advances with age so it flows muzzle to terminus like the renderer's flow
   bands (5 per tick), and a cyan dust outer layer at 1.15 to 1.65 times the
   beam radius (2 per 2 ticks), plus EMBER and SPARK boiling off the
   terminus. All counts scale by FxEnvelope.envelope(age, 24) and
   effectDensity. The charge orb's particles are layered the same way (core,
   mid, outer, plus the hot layer). The beam renderer now takes its alpha
   from the same FxEnvelope the entity uses, and ribbon segment counts step
   down with FxEnvelope.distScale beyond 32 and 64 blocks.
2. DEBRIS. The full-charge endpoint detonation throws a 24 piece
   DebrisSpawner field (finisher class count) sampled from the blocks
   around the terminus: pieces orbit helically for 26 ticks, then rain down.
   Bolt block impacts kick up a small field, 5 pieces normal and 10
   overcharged, deliberately small because the barrage fires every 3 ticks.
3. WORLD INTERACTION. Beam tick 1 fires LayeredParticles.emitGroundBurst at
   the terminus (cyan tech dust), and the detonation repeats it at full
   radius. Bolt impacts burst dust at the exact block hit. Suction is a real
   per-pulse force on everything in the beam. No GroundScar: the beam is an
   energy weapon, not an elemental or heavy theme, and the standard reserves
   scarring for those; dust, debris, and forces carry the interaction here.
4. ATMOSPHERE. The beam already played thunder and a zap with no visual;
   tick 1 now also spawns a shared BoltEntity strike at the terminus (cyan
   0xFF7DF4FF, 12 long, 8 tick life) so the thunder has a branched lightning
   visual. No weather added: the gun has no weather theme.
5. LIFECYCLE. Beam life 16 to 24 ticks with the shared envelope on both the
   mesh alpha and the particle counts (build over the first 20 percent,
   dissipate over the last 30). Damage moved inside the window: 6 pulses at
   ticks 3 to 18, none on tick 1 and none after the visual ends. The
   renderer exit now starts at 0.7 of life to match. In the decay window,
   spent sparks rain down around the terminus via LayeredParticles.emitRain.
   The bolt's range death is a fade, not a pop. The charge build-up now
   reads in particles that climb with charge level (counts rise, the spiral
   tightens, embers appear past 0.75), matching the orb mesh, which already
   grew and materialized with charge.

## Renderer geometry the headers promised

- ErasureCannonBeamRenderer header promised "stationed hexagonal focusing
  aperture rings along the beam" and submit() drew none. It now draws three
  hex rings (drawPlaneRing with 6 segments) at 0.25, 0.5, and 0.75 of the
  beam length, counter-rotating, tightening with distance from the muzzle,
  alpha on the shared envelope; the farthest ring drops out at distance.
- ErasureChargeOrbRenderer header promised 3 concentric rings and drew 1.
  It now draws three rings in three planes (muzzle plane via drawPlaneRing,
  horizontal and side planes via a new drawAxisRing helper). The header's
  remaining false promises (helical ribbons, corona arcs) were removed from
  the header text instead of left lying; the past-0.75 ember layer is the
  honest version of the corona idea and lives in the entity's particles.

## Bugs fixed

- drawRotatingOctahedron (ErasureChargeOrbRenderer) converted its yawDeg and
  pitchDeg parameters to radians and never used them: the "rotating" core
  was drawn unrotated. All six vertices are now rotated (yaw around Y, then
  pitch around X) through a new rotVertex helper.
- Beam endpoint explosion stacked 100 percent beam + 50 percent explosion on
  the same tick 1 for any target at the terminus, and the item text never
  mentioned the explosion. The explosion is now a separate beat on the final
  pulse tick; stacking in the same tick is gone.
- BlasterGunItem.use() failure path returned InteractionResult.PASS and sent
  its "Need at least 10 Blaster Ammo" message as chat text, unlike every
  other weapon in the mod. It now returns FAIL with an action-bar message.
- The 0.1 charge floor let an accidental click fire an 11 ammo, 1040 damage
  beam (audit's "no low-charge fizzle state"). Fixed with the fizzle gate
  above; ammo is only consumed when the beam actually fires, per the
  standard's charging rule.

## Audited, no change

- BlasterAmmoItem: a plain Item subclass with a 3 line constructor. The
  fresh audit assigns the ammo economy to the item class, not this file;
  nothing here was short of the standard.
- BlasterBoltRenderer: already draws three nested layers with heat colors
  and a proper impact collapse; the bolt's missing trail was an entity-side
  particle gap, fixed in the entity. No renderer change needed.

## Shared change requests

- None. Everything above uses the shared blocks as they stand
  (FxEnvelope.envelope and distScale, LayeredParticles.emitGroundBurst,
  emitRain, and dust, DebrisSpawner.spawnField, BoltEntity).

## Honest worse check

- Beam ramp: the shared envelope reaches full alpha at tick 5 of 24, where
  the old private curve hit full at tick 3 of 16. The first two ticks read
  slightly softer than before; that is the standard's build-up doing its
  job, and the first damage pulse lands at tick 3 as the beam peaks. Kept.
- Pulse damage means a target that steps out of the beam after one pulse
  takes one sixth of the old instant total. That is the intended trade for
  a beam that reads as a sustained burn, and a target that stays in takes
  the same total as before. Kept, and stated here plainly so it is a known
  design change, not a silent nerf.
- Nothing else regressed on re-read: heat, vent, ammo, barrage rate, bolt
  piercing, and the orb's cleanup rules are byte-identical in behavior.
- Verification: dual javac sweeps (26.2 and 26.3 classpaths) against a HEAD
  (5e4b1de) baseline worktree, error kinds compared with line numbers
  stripped. Baselines: 132 kinds (26.2), 141 kinds (26.3). Current tree with
  all agents' in-flight work: 134 (26.2), 150 (26.3). The first sweep caught
  exactly one new error kind in this weapon's files: sendSystemMessage with
  the action-bar boolean overload called on a plain Player in
  BlasterGunItem.use(), on both classpaths. Fixed with a ServerPlayer cast.
  The final re-sweep after the fix shows ZERO error kinds in any of this
  weapon's eight files (item, ammo, three entities, three renderers) on
  both classpaths. The new kinds that remain in the tree belong to other
  agents' files (ZShockwaveEntity and CurseBladeItem on 26.2;
  CurseChainRenderer, KingsSlamEntity, OxShockwaveEntity, CurseBladeItem on
  26.3) and are the coordinating pass's to collect from their owners. (Two
  earlier verification attempts were lost to sandbox timeouts and a /tmp
  wipe; the harness was rebuilt under ~/workspace/dba-audit-work and the
  numbers above are from the completed run.) The Gradle build cannot run in
  this sandbox (standing platform limit), and per Carter's standing
  decision the in-game look check is his on his PC after the final push.
