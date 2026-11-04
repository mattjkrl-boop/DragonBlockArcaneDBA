# Spectacle Report: Ox King's Axe (Phase 2)

Weapon: Ox King's Axe, the groundbreaker. Theme kept throughout: magma, basalt,
gold, royal weight. Files changed: item/OxKingsAxeItem.java,
entity/OxShockwaveEntity.java, entity/OxFissureEntity.java,
entity/KingsSlamEntity.java, entity/OxChargeEntity.java (unchanged),
entity/OxStanceAuraEntity.java (unchanged), and the five renderers
OxChargeRenderer, OxShockwaveRenderer, OxFissureRenderer,
OxStanceAuraRenderer, KingsSlamRenderer. fx/SlamDebris.java untouched, as
required. No shared files edited. No commit made.

## Before and after numbers

| Value | Before | After |
| --- | --- | --- |
| Melee attack damage (attribute) | 950 total (949 + 1) | unchanged |
| Melee hurtEnemy | bare vanilla hit, no effect, no visual | weight-scaled shove (0.85 horizontal, up to 0.42 up, target velocity x 0.3 kept), Fissure Stun 12 ticks, 6 EMBER particles (density scaled), anvil thud; still no stamina or Ki cost |
| Groundbreaker damage | 350 + 850 x charge + Strength x 3.5 x charge (392.5 tap, 1200 + 3.5 x Str full) | unchanged |
| Groundbreaker cooldown | 20 + 40 x charge ticks (21 to 60) | unchanged |
| Groundbreaker charge time | 200 ticks max, stamina 125 percent per minute | unchanged |
| Shockwave max radius | 4 + 20 x charge (24 full), echoes x 0.75 | unchanged |
| Shockwave ring destruction coverage | max(8, radius x 2) sample points, about 48 points on a 24 block ring, spacing about 3.1 blocks, roughly 1 in 3 ring blocks tested | every distinct ring cell: arc stepped at half a block, about 150 cells on a 24 block ring, 100 percent of ring cells tested at both heights |
| Destruction gate and class | charge >= 0.95, main wave only, blast resistance <= 2.0, bedrock and barrier excluded, drops on | same gate and class, plus GroundScar.isProtected check (spawners, chests, barrels, block entities and friends can never be destroyed) |
| Ground scarring | none below 0.95 charge; nothing at all from slams or stance | wave: from charge >= 0.50, ring surface cells scarred (55 percent chance per cell, 40 percent magma block, 60 percent basalt), restore 240 ticks, 300 ticks at destruction charges. King's Slam: disk radius 5 (normal) or 7 (flawless) scarred at the rim tick, 70 percent chance per cell, same magma and basalt mix, restore 300 ticks |
| Debris | one SlamDebris burst on tick 1 of fissures and slams only | wave: DebrisSpawner field every 3 ticks of the main wave, count round((2 + 5 x charge) x density) per spawn (up to 7, about 70 pieces over a full charge wave), ground sampled palette, rise 2.2 + 1.6 x charge, orbit 12 to 20 ticks then rain down. King's Slam: DebrisSpawner field on tick 2, 30 pieces normal, 48 flawless (density scaled), rise 3.5 or 5.0, orbit 26 ticks. SlamDebris bursts kept |
| Fissure damage | 50 per second (25 per 10 tick hit), damage zone an unrotated box about 5 x 3 x 5 | 150 per second (75 per 10 tick hit), damage zone a rotated rectangle aligned to the drawn trench (about 4.2 long, 2.0 wide, y window -1.5 to +2.0) |
| Stance aura push | flat 0.35 horizontal, +0.05 vertical per tick at any distance inside 12 | falls off with distance: scale 0.30 + 0.70 x (1 - dist / 12), so 0.35 at the center to 0.105 at the rim, vertical scaled the same way |
| Stance aura pressure damage | 45 + Strength x 0.4 every 5 ticks (about 180 + 1.6 x Str per second), flat | same base, multiplied by (1 - 0.5 x dist / 12): full at the center, half at the rim (about 90 + 0.8 x Str per second at the rim) |
| Stance aura stun | Fissure Stun 20 ticks refreshed | unchanged (the denial stays full strength) |
| Normal slam damage | 400 + 900 x holdRatio + Strength x 3 x holdRatio, instant at the release tick | same total, applied by KingsSlamEntity as the dome rim reaches each target; final rim target lands at tick 22 of 40 |
| Normal slam radius | frozen at 10.0 at every hold length | 6.0 + 8.0 x holdRatio (6.0 at minimum hold, 14.0 at capped ratio) |
| Normal slam cooldown | 40 + 60 x holdRatio ticks (40 to 100) | unchanged |
| Flawless slam damage | 2000 + Strength x 5, instant at the release tick across radius 20 | same total, applied by KingsSlamEntity as the dome rim reaches each target; final rim target lands at tick 31 of 55 |
| Flawless slam cooldown | 100 ticks | unchanged |
| Slam effect profile on hit | Fissure Stun 80 (normal) or 140 amp 1 (flawless), Cinematic Tracking 40 or 60, Movement Curse 40 amp 1 or 60 amp 2, launch 1.5 and +0.5 or 2.5 and +0.9 | unchanged values, now applied by KingsSlamEntity.applyRimDamage at the rim tick for each target |
| Ki costs | none on either ability (stamina only) | unchanged |

Charge scaling check (standard: full charge worth at least 2x a tap): wave
damage 392.5 tap to 1200 + 3.5 x Str full (about 3.1x), radius 4.2 to 24
(5.7x), destruction and fissures gated to full charge. Normal slam damage
400 (minimum hold ratio near 0.05: about 445) to 1300 + 3 x Str, radius now
6.3 to 14. Cooldowns: Groundbreaker 60 at full charge against a wave plus
echoes ending about tick 35; flawless slam 100 against a 55 tick visual;
normal slam 40 to 100 against a 40 tick visual. All satisfy cooldown >=
visual lifetime.

## Per point, what was added

1. LAYERED DEPTH. Shockwave: the renderer gained a ground anchor glow
   (LayeredRender.drawAnchorGlow at the standard mid alpha), a mid dust wall
   band rolling behind the crest at 0.94 x radius, and a wispy outer band at
   1.07 x radius turning the other way, on top of the existing crags, teeth,
   and shards (core). The entity emits three matching particle layers per
   tick: KI_MOTE on the crest line, magma dust (0xFF7A1A) at 0.88 x radius,
   EMBER wisps trailing at 0.55 to 0.8 x radius, all scaled by FxEnvelope.
   Stance aura: anchor glow under the 12 block boundary plus
   LayeredRender.drawVortexBands (three counter-rotating bands) around the
   titan. King's Slam: anchor glow pool under the dome. Fissure: anchor glow
   pool under the trench. Distance scaling (FxEnvelope.distScale from the
   render state's distanceToCameraSq) now steps down segments and counts in
   the shockwave, stance, slam, and charge renderers.
2. DEBRIS. DebrisSpawner fields ride the Groundbreaker wave (see numbers
   table) and erupt from the King's Slam on tick 2 (30 or 48 pieces, three
   sizes, real ground blocks sampled on site, helical orbit, then rain
   down). The charge renderer's levitating rocks now tumble (footprint and
   lean rotate with age on per-rock phases) instead of only bobbing.
3. WORLD INTERACTION. Ring destruction is now honest at full charge (every
   ring cell tested, both heights, weak blocks only, drops, protected blocks
   refused through GroundScar.isProtected). GroundScar scarring added at
   charge >= 0.50 on the wave ring and under the King's Slam at its rim
   tick; all scars restore (240 to 300 ticks) and are crash safe through
   GroundScar's SavedData. Base dust bursts (LayeredParticles.emitGroundBurst)
   on the wave's and the slam's first tick. Mobs: the stance push and
   pressure damage now fall off with distance instead of hitting flat across
   12 blocks; melee connects now physically shove by weight class.
4. ATMOSPHERE. No weather theme belongs to this weapon and none was added,
   per the standard. Atmosphere is light: the new anchor glows put a molten
   pool under the wave, the slam, the fissure, and the stance boundary, and
   the flawless slam keeps its existing sonic boom cue.
5. LIFECYCLE. Slam damage timing is now honest: the telegraph (stance,
   then the growing dome) comes first and damage lands as the rim reaches
   each target (ticks spread 1 to 22 normal, 1 to 31 flawless), never at
   the release tick and never after the visual ends. The charge visual
   fades in over 6 ticks and the stance aura over 8 ticks (both popped in
   at high alpha before). Wave crest particles follow FxEnvelope, so they
   build and dissipate with the wave. Fissures gain an ember rain
   (LayeredParticles.emitRain) across their final 30 ticks. Shockwave and
   fissure fades from the earlier passes were already envelope-correct and
   are unchanged.

## Bugs fixed

- OxShockwaveEntity client state mismatch (from the audit): maxRadius and
  maxTicks were plain fields set only in the gameplay constructor, so
  client copies kept the defaults (12.0 and 20) and tick() recomputed the
  synced CURRENT_RADIUS from those wrong values between server updates.
  Both are now derived from the synced charge ratio and sub wave flag via
  computeMaxRadius() and computeMaxTicks() on both sides; the plain fields
  are gone. The renderer already recomputed the same way, so all three
  parties now agree.
- Slam damage vs visual mismatch (from the audit): damage was instant at
  the release tick while the KingsSlam dome needs 22 (normal) or about 30
  (flawless) ticks to grow to the rim. Damage now lives in KingsSlamEntity
  and lands per target at its rim tick, using the renderer's exact growth
  curve (ease-out cubic over the first 55 percent of life).
- Fissure shape mismatch (from the audit): the renderer draws a 3.6 block
  trench along the entity yaw; the damage zone was an unrotated 5 x 3 x 5
  box. The hit test now runs in the fissure's local frame (4.2 long, 2.0
  wide), aligned to the same yaw the spawner sets and the renderer draws.
- Melee identity gap: hurtEnemy was an empty no-op, so the mod's heaviest
  weapon had a bare vanilla basic attack. It now has a weight-scaled shove,
  a 12 tick fissure stun tremor, embers, and a thud.
- Normal slam radius frozen at 10.0 while its damage scaled: radius now
  scales 6.0 to 14.0 with hold ratio.
- Stance stamina exit note (from the audit, not changed): running out of
  stamina mid-stance still routes through releaseUsing and pays a scaled
  normal slam. It is now at least consistent with the telegraphed slam
  system, and the behavior is a design choice about a failed stance, so it
  is flagged here rather than silently changed.

Not fixed, deliberately: OxChargeRenderer.drawAscendingTendril and
OxStanceAuraRenderer.drawSegment are dead code per the audit, but both are
private helpers whose removal changes nothing visually; left in place to
keep this diff focused (coordinating pass may delete them in cleanup).

## Shared change requests

- None required. All work was done by calling the shared blocks
  (FxEnvelope, LayeredParticles, DebrisSpawner, GroundScar, LayeredRender)
  from this weapon's own files.
- Optional cleanup for the coordinating pass: delete the two dead private
  renderer helpers named above.

## Worse check

Self-audited every change against the standard and the audit. Nothing was
reverted. Honest risks, stated plainly:

- Slam damage now uses horizontal distance to the rim; the old item code
  used 3D distance to the player. A target directly overhead at the rim
  height edge could be hit slightly earlier than before. The dome is a
  ground effect, so horizontal is the honest measure, and the y window in
  the entity query (center y -2 to +3) matches the shockwave's.
- The rim damage means a target that walks out of the dome mid-growth now
  takes no slam damage where the old instant code would have hit it at
  release. That is the telegraph working as intended (the warning's size
  equals the hit's size), and targets near the center are still hit within
  the first ticks.
- Wave scarring runs GroundScar.scar per ring cell per tick; GroundScar
  scans its entry list linearly per call. At full radius that is about 150
  cells per tick for up to 31 ticks with a 55 percent chance filter. Scar
  entries live at most 300 ticks, so the concurrent list stays bounded,
  but a server running many simultaneous full-charge Groundbreakers will
  feel this more than the old code, which did no scarring at all.
- Debris counts at full density: about 70 DebrisEntity pieces over a full
  charge wave plus 30 to 48 per slam. Within the standard's field sizes,
  but they are real entities; low-end servers may want density turned down
  (DbaEffectsConfig.effectDensity already scales every count added here).

## Verification

Method: javac audit sweeps (the sandbox cannot run the Gradle build; the
compile check for this project is waived by Carter, audit only). The whole
project (449 Java files) was swept against both the 26.2 and 26.3
classpaths, baseline at commit 5e4b1de versus the edited tree, comparing
unique (file, message) error kinds with line numbers stripped.

- Baseline at 5e4b1de: 132 error kinds on 26.2, 141 on 26.3.
- Edited tree: 132 kinds on 26.2, 141 on 26.3.
- New kinds in this weapon's files: zero on 26.2, zero on 26.3.

One real catch during verification: the first sweep round showed two new
"cannot find symbol" kinds on 26.3 only, in KingsSlamEntity and
OxShockwaveEntity, for BlockState.blocksMotion(), which exists on 26.2 but
was removed on 26.3 (the pre-existing LavaSwordGround carries that same
baseline kind). Both calls were switched to
isFaceSturdy(level, pos, Direction.UP), which exists on both versions, and
the re-sweep confirmed zero new kinds. No Gradle build was run or claimed.
