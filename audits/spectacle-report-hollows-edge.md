# Spectacle Report: Hollow's Edge

Phase 2 weapon report, written against audits/SPECTACLE-STANDARD.md and the
Hollow's Edge section of audits/fresh-audit-dark.md (the before picture).
Theme unchanged: void rush, afterimages, the imploding rift. Same violet
palette (lip color 0.50 / 0.05 / 0.88, theme ARGB 0x800DE0), same identity.

Files changed (mine only):
- item/HollowsEdgeItem.java
- entity/VoidRiftEntity.java
- entity/VoidSlashEntity.java
- entity/HollowAfterimageEntity.java (shared entity, see the Katana and Saber note)
- client/render/VoidRiftRenderer.java

No shared or common files were edited. No commit made.

## Before and after numbers

All damage values pass through DbaEffectsConfig.capDamage, before and after.
Strength is abbreviated Str. Rift burst damage formula is unchanged:
(550 + Str x 2) x (0.7 + 0.6 x chargeRatio), so 385 + 1.4 Str on a tap and
715 + 2.6 Str at full charge (Strength 0 numbers used below).

| Value | Before | After |
| --- | --- | --- |
| Rift channel tick damage | 5 percent of burst damage every 10 ticks, flat, tested against the bounding box out to radius x 1.8 (35.75 per pulse at full charge, anywhere in the box, corners included) | 8 percent at the core every 10 ticks, falling off with distance to 2.8 percent at the rim, true distance check inside radius only (57.2 at core, 38.6 at half radius, 20.0 at rim, full charge) |
| Rift channel tick total, target standing at core for the whole channel | 40 pulses x 35.75 = 1430 over the 400 tick cap | 24 pulses x 57.2 = 1372.8 over the 240 tick cap (similar total, delivered in 12 seconds instead of 20, and only at the core) |
| Rift burst damage | Full burst damage (715) to everything in a radius x 2.2 box, corners about 3.1x radius out | Burst damage x falloff (1.0 at core to 0.6 at edge) inside a radius x 1.5 sphere (715 at core, 429 at edge) |
| Rift burst radius | radius x 2.2 box | radius x 1.5 sphere, distance checked |
| Implode suction window | 10 ticks | 30 ticks, staged (strength scales 0.45 to 1.0 of full across the window) |
| Implode suction force | Velocity set to 1.2 toward center, radius x 2.5, all entities, no weight classes | VortexPhysics add based pull 0.5 x stage at radius x 1.6 for living targets, weight classed; loose entities add 0.7 x stage |
| Channel pull | Velocity add up to 0.6 (y halved), radius x 1.8 box, all entities, no weight classes, no swirl, no lift | VortexPhysics: strength 0.30, swirl 0.6, lift 0.10, radius x 1.35, weight classed (boss 0.12, heavy 0.45, players x 0.6); loose items and projectiles add 0.25 |
| Channel max length (auto implode) | 400 ticks (20 s) | 240 ticks (12 s) |
| Rift cooldown on release | 60 ticks flat | 60 + 60 x chargeRatio (60 to 120 ticks); always exceeds the 30 tick collapse, so rifts cannot stack |
| Burst fling | Velocity add 2.2 out + 0.8 up, unscaled for every target | VortexPhysics.fling 2.2 out / 0.8 up, weight classed (boss 0.12x, heavy 0.45x, light 1.0x) |
| Channel Dark Faded | 20 ticks, refreshed every tick inside the box | 30 ticks, refreshed every 5 ticks, distance checked inside radius x 1.35 |
| Afterimage Dark Faded | 60 ticks, box radius 4.0, no distance check | 40 ticks (matches the weapon's own melee), true radius about 3 (box 3.0 prefilter plus 3.25 distance check) |
| Void Slash default damage | 600.0f field default (dead, matched no formula) | 0.0f, and an unset slash deals no damage; the item always sets it |
| Void Slash damage when fired (unchanged) | (450 + Str x 2.5) x 1.8 (third dash) x 1.5 = 1215 + Str x 6.75 | Same |
| Dash path damage (unchanged) | 450 + Str x 2.5, x 1.8 on every third dash | Same |
| Melee (unchanged) | 850 base, Dark Faded 40 ticks on hit | Same |
| Ki costs (all unchanged) | Charge drain 30 percent per minute, rift channel drain 30 percent per minute, fallback blink 30 discrete over 20 ticks, melee drain 20 over 7 ticks | Same |
| Rift particles | About 27 per second from the center point only (void smoke every 3 ticks, sparks every 6) | LayeredParticles.emitVortex every 2 ticks: KI_MOTE core, violet dust helical mid band at 0.55 to 0.8 radius, VOID_SMOKE wisps at 0.9 to 1.2 radius, all envelope and density scaled |
| Rift renderer fade in | None: body alpha flat 1.0 from the first frame | 20 tick envelope fade in on body, rim, seam, shards, halo, and bands |
| Rift collapse choreography window (renderer) | 10 ticks | 30 ticks, read from VoidRiftEntity.IMPLOSION_TICKS so both sides agree |
| Tear segment count (renderer) | 16 flat | 10 to 16 by camera distance (FxEnvelope.segments); accretion bands 22 down to 8 by distance |
| Debris | None anywhere in the weapon | Implosion wave: 60 pieces (density scaled), 3 sizes (0.45 / 0.7 / 1.0 from DebrisSpawner), ground sampled palette, 70 tick helical orbit up into the tear, then release and rain down. Channel trickle: 12 pieces every 40 ticks while radius is at least 4, 45 tick orbit |
| Ground interaction | None: no dust, no scar, blocks fully exempt | Dust burst at the ground under the tear on open and at the burst; obsidian GroundScar disk at the burst, radius 2 to 6 by rift size, 45 percent chance per cell, restored after 240 ticks, protected blocks refused by GroundScar itself |

Charge pays, per the standard: full charge versus tap is 3.6x on radius
(2.5 to 9.0) and 1.86x on burst damage (0.7x to 1.3x multiplier).

## What was added, per point

1. LAYERED DEPTH. The rift now emits LayeredParticles.emitVortex every 2
   ticks in channel and collapse: bright KI_MOTE core, a violet helical mid
   band at the rim where the tear's size reads (the audit found the rim
   particle silent; it is now the loudest layer), and VOID_SMOKE wisps
   outside it. The renderer gained a halo disc of void light behind the
   throat and three accretion bands circling the tear in its own plane at
   radii 0.90 / 1.06 / 1.24 x radius, rotating at different speeds and
   directions with tapered ends, at the standard band alphas. Tear and band
   segment counts and alpha step down with camera distance through
   LayeredRender.distScale and FxEnvelope.segments. The left click charge
   also emits the shared vortex at body scale around the player, breathing
   with the charge envelope. The Void Slash trail gained a KI_MOTE core
   under its existing VOID_SMOKE.
2. DEBRIS. DebrisSpawner fields of real ground blocks, sampled from the
   ground under the tear: a 60 piece storm class wave when the collapse
   starts, orbiting up into the rift on helical paths in 3 sizes, released
   at orbit end to tumble and rain back down (DebrisEntity phases), plus a
   12 piece trickle every 40 ticks while a mature rift channels. Nothing
   vanishes mid air; pieces land, fade, and crumble per DebrisEntity.
3. WORLD INTERACTION. The pull, lift, and fling all run on VortexPhysics
   (see numbers table), with MobPhysics weight classes, so the rift moves
   the world in the shared language. Ground dust bursts where the tear
   opens and where the burst lands. The burst leaves a temporary obsidian
   scar through GroundScar (restored, crash safe, protected blocks refused).
   Loose items and projectiles are still eaten; the owner's own projectiles
   are now exempt.
4. ATMOSPHERE. No weather added: the rift has no weather theme, and
   AtmosphereServer only offers RAIN and THUNDER kinds that set real
   weather, which would be wrong for a void tear. Its atmosphere is light
   and particles per the standard: the halo disc, the layered particle
   field, and the existing sound pulses. See shared change request 2.
5. LIFECYCLE. The rift fades in over 20 ticks on the envelope (it popped in
   at full alpha before). Channel particles and pull strength ride
   FxEnvelope over the 240 tick maximum channel and thin in the last third,
   telegraphing the forced implosion. The collapse is a staged 30 tick
   suction with rising strength and a debris wave, ending in the burst, and
   the renderer choreography runs on the same 30 tick window from the
   synced implode start tick. Damage sits inside the visual: channel ticks
   during the channel, burst at the collapse's final tick. Telegraph
   honesty: pull radius (1.35x) and burst radius (1.5x) now sit close to
   the visible tear instead of reaching 1.8x to 2.5x past it.

## Bugs fixed

- Channel tick damage used the axis aligned bounding box with no distance
  check (audit, VoidRiftEntity): corner targets at about 1.41x the radius
  took full damage. Now distance checked with falloff, in the channel and
  in the burst.
- VoidSlashEntity's dead 600 damage default (audit): removed. The field
  defaults to 0, the item always sets damage from its formula, and an
  unset slash now deals no damage instead of phantom 600.
- No exclusions on rift forces (audit): the channel pull and implode
  suction dragged the owner's own projectiles and loot indiscriminately.
  The owner's projectiles are now exempt from the loose entity pull, and
  DebrisEntity pieces are exempt so the debris field keeps its helical
  motion. Dropped items are still pulled in on purpose: the rift eating
  the world is its identity.
- Afterimage debuff outlasted and outreached the dash that made it
  (audit): 60 ticks at box radius 4, against the melee's own 40 ticks.
  Now 40 ticks at a true radius of about 3.

## Katana and Saber dependency note (HollowAfterimageEntity)

I own HollowAfterimage per the standard. Its public constructor
(Level, Player) is unchanged, all five synced data fields and accessors are
unchanged, and Katana's and Saber's spawn sites keep working exactly as
before. One behavior tune inside the entity affects their ghosts too: the
Dark Faded aura is now 40 ticks within about 3 blocks instead of 60 ticks
within a 4 block box. If those weapons want their old numbers back, see
shared change request 3.

## Shared change requests (not made, per coordination rules)

1. LayeredParticles.emitVortex takes age and life and applies the standard
   envelope internally. A channeled effect with a player controlled length
   has to pass a guessed maximum life (the rift passes its real 240 tick
   cap, which works). An overload that accepts a precomputed envelope
   float would let channeled effects hold an exact steady state.
2. AtmosphereServer.Kind is only RAIN and THUNDER, and both set real
   server weather. A gloom only kind (client grade broadcast, no weather
   change) would let void and darkness weapons answer point 4 properly.
   The rift deliberately does not fake rain in the meantime.
3. HollowAfterimageEntity's debuff numbers are global to all three
   weapons that spawn it. If Katana or Saber want per weapon ghost
   numbers, the entity needs optional synced overrides (duration and
   radius) defaulting to the current values.

## Honest worse check

- Burst reach went down: radius x 2.2 box to radius x 1.5 sphere, and the
  burst edge pays 60 percent instead of full. This is the audit's own bug
  (full damage at box corners, about 3.1x radius out, far outside the
  visual) traded for an honest telegraph, with core damage untouched and
  channel core damage per second roughly doubled (3.6 to 5.7 per tick at
  full charge) to pay for time on target. Not reverted: it is what the
  standard asks for, but it is a real reach nerf and is stated as one.
- Bosses and heavy mobs now barely move under the pull and fling
  (0.12x / 0.45x weight scale) where the old code set velocities unscaled
  and could drag anything. That is the shared VortexPhysics language the
  task required; the rift no longer ragdolls bosses. Not reverted.
- Raw inward pull at the core is lower per tick than the old code
  (0.30 add, versus up to 0.6 add), in exchange for swirl, lift, and
  weight classes. It still accumulates every tick and the staged implosion
  gathers at radius x 1.6 with rising strength. One constant
  (VortexPhysics strength in VoidRiftEntity) restores the old raw pull if
  playtesting says the gather feels weak. Flagged, not reverted.
- Renderer overdraw grew: the halo disc spans 1.55x the tear radius and
  three bands circle it. Alphas are low (halo 0.30 at center fading to 0,
  bands at the standard 0.45 / 0.18 band alphas or below) and distance
  scaling cuts segments beyond 32 blocks. Judged acceptable; nothing in
  the pass was reverted.

## Self audit and verification

- Diff scope: exactly the five files listed at the top. No util, fx,
  network, movement, config, registry, or other weapon files touched.
- Javac audit sweeps (the compile check for this sandbox; Gradle cannot
  run here): baseline measured fresh from a worktree at HEAD 5e4b1de,
  132 unique error kinds on 26.2 and 125 on 26.3. Current tree: 140 on
  26.2 and 147 on 26.3, and every new kind sits in another weapon agent's
  in flight files (KatanaChargeRenderer, LavaEruptionEntity,
  SpiritImpaleEntity, ZShockwaveEntity, BlasterGunItem, CurseBladeItem,
  SpiritSwordItem, KingsSlamEntity, OxShockwaveEntity,
  SpikeFlailSpinEntity, and a transient mixin package block on 26.3).
  Zero new error kinds in any Hollow's Edge file on either version. The
  one pre existing 26.3 kind in VoidRiftRenderer (mulPose with a
  Quaternionf, the known dual version artifact) is identical before and
  after.
- No em dashes in any line added by this pass. Four em dashes remain in
  pre existing untouched lines (three class javadocs and one player
  message string), left exactly as written.
- The Minecraft client cannot run in this sandbox, so the final look is
  judged in game on Carter's PC after the coordinating pass pushes.
