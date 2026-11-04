# Spectacle Report: Devil Trident (Phase 2)

Agent scope: item/DevilTridentItem.java, entity/TridentProngEntity.java,
entity/TridentShardEntity.java, entity/DevilSlamShockwaveEntity.java, and the
renderers TridentShardRenderer.java and DevilSlamShockwaveRenderer.java.
TridentProngRenderer.java needed no change (its helical wake plus the new
server trail covers the shot). util/SwarmHelper.java was read only. Theme kept:
crimson demonic, devil's hands, the shard swarm, the deploy slam.

## Before and after numbers

| Value | Before | After |
|---|---|---|
| Prong damage | 750 flat to the first target only, shot dies | 750 / 495 / 337.5 across up to 3 pierced targets (shares 1.0, 0.66, 0.45) |
| Prong on-hit effect | none | MARKED_BY_EVIL 60 ticks on each pierced target |
| Prong life and dissolve | dies after tick 28, 6 tick dissolve | unchanged |
| Prong Ki and cooldown | 60 Ki per 10 ticks, 10 tick cooldown | unchanged |
| Deploy slam damage timing | full AoE on the cast tick | lands at slam tick 8 of 35, at the visual's peak |
| Deploy slam radius | 5.0 damage and visual, seal art reaching about 6.75 | 6.0 damage and visual, seal edge drawn at radius x 1.05 |
| Deploy slam damage | 750 x falloff (full to 2.5, floor 0.4 at rim) | 750 x falloff (full to 3.0, floor 0.4 at rim), same curve scaled |
| Deploy target motion | velocity add y -2.0, a downward pin | VortexPhysics.fling: 0.85 outward, up to 1.05 up, weight scaled (boss 0.12, heavy 0.45, light 1.0) |
| Deploy ground changes | zero blocks | GroundScar disk radius 6: magma chance 0.50 at dist 1.2 to 3.0, blackstone 0.38 at 3.0 to 4.6, soul soil 0.22 at 4.6 to 6.0, restored after 240 ticks; the caster's own cell is never scarred |
| Deploy debris | none | DebrisSpawner field: 30 pieces, radius 5.1, rise 3.4, 26 orbit ticks, palette sampled from the ground plus magma and blackstone |
| Deploy status effects | DEVILS_HANDS 300 ticks amp 2, CINEMATIC_TRACKING 300, on cast tick | same values, applied by the slam entity at hit tick 8 |
| Shard hit cadence | a hit attempt every tick inside 2 blocks (20 per second design rate) | at most 1 hit per 6 ticks per shard, with a 4 tick recoil out between dives |
| Shard damage per hit | 100 + Strength x 0.5 | unchanged |
| Shard health budget | 15000 per shard, spent per hit dealt | unchanged (about 150 hits per shard, now spread over real dive cycles) |
| Swarm upkeep | Ki drain 20 per minute | unchanged |
| Starvation cooldown | 3600 ticks only if the last shard physically arrived home; 0 if shards were discarded by distance, logoff, or unload | 3600 ticks on every end path: arrival, or settlement after 40 straight ticks with zero shards while a wipe is pending |
| Manual recall cooldown | 600 ticks | unchanged |
| Deploy buffer cooldown | 20 ticks | unchanged |
| Shard orbit | flat ring radius 2.2, one direction, one height band | radii 2.2 / 2.7 / 3.2 by index, alternating direction (x1.0 and x-0.85), staggered heights |
| Shard scale | uniform 0.85 | 0.85 x per-shard factor 0.80 to 1.16 (five steps from the entity UUID) |
| Shard spawn | all 10 at the player's position, y + 2.0, same tick | ring at the slam crater (radius 0.9, y + 0.5), emerge flight of 8 + 2 x index ticks (8 to 26) |
| Slam entity particles | SPARK about 10 on tick 1, EMBER about 3 per tick for 12 ticks | LayeredParticles.emitVortex every tick of the 35 tick life, emitGroundBurst on tick 1, emitRain over the last 12 ticks |
| Shard particles | EMBER 1 per shard per 3 ticks (about 67 per second for the swarm) | KI_MOTE 1 to 2 plus crimson dust 1 per shard per tick, EMBER 1 per 3 ticks (about 540 per second at density 1.0, density gated, halved below 0.5 density) |
| Slam renderer fade basis | getLifetime() returned ticks REMAINING, so the fade denominator shrank each tick and the visual effectively died near tick 18 | getLifetime() returns the full 35; the fade spans the whole life |

## What was added, per point

1. Layered depth. The prong shot now emits a three layer trail every tick
   (KI_MOTE core, crimson dust mid via LayeredParticles.dust, EMBER wisps) and
   sheds embers through its 6 tick dissolve. Shards emit the same three layers,
   doubling motes while diving. The slam's hellfire column is now backed by
   LayeredParticles.emitVortex (core motes, crimson helical mid band, ember
   outer wisps) for its whole life, on top of the renderer's existing layered
   geometry. The slam seal's ring segment counts now scale with camera distance
   through LayeredRender.distScale and FxEnvelope.segments.
2. Debris. The deploy slam throws a 30 piece DebrisSpawner field sampled from
   the ground it lands on, with magma and blackstone mixed in: three sizes,
   helical orbits, then the pieces rain back down (DebrisEntity behavior). The
   swarm itself gained the point 2 variety it lacked: per-shard sizes, three
   orbit shells, alternating orbit directions, staggered heights.
3. World interaction. The slam tears a real brimstone scar disk through
   GroundScar (temporary, restored, crash safe, protected blocks refused by
   GroundScar itself), bursts base dust through LayeredParticles.emitGroundBurst,
   and launches targets up and out through VortexPhysics.fling instead of
   pinning them down. The prong now bursts on the block face it strikes instead
   of dissolving in silence.
4. Atmosphere. The trident has no weather theme and gained none, per the
   standard. Its atmosphere is light and particles: the layered hellfire column,
   the crimson trail system, and ember rain while the slam dissipates.
5. Lifecycle. Deploy damage moved from the cast tick into the visual's active
   window (tick 8: seal grown in, spikes standing), and the seal's drawn edge
   now equals the hit radius. The scar tears at tick 3 as the trenches open.
   Shards build up on deploy instead of popping in: they burst from the crater
   ring and fly out to their places over 8 to 26 ticks, staggered by index. The
   slam ends in ember rain; the prong dissolves over 6 ticks shedding embers;
   the renderer fade now spans the full 35 ticks (see the getLifetime fix).

## Bugs fixed

- TridentProngEntity.hitIds was dead bookkeeping: the hit loop returned after
  the first hit, so the set could never hold more than the hit that ended the
  shot. Piercing is now real: the set is the pierce ledger, one entry per body,
  no target billed twice, shot dies on the third body.
- Starvation cooldown bypass closed. The 3600 tick cooldown only fired from
  finishReturn, so shards discarded by distance, logoff, or chunk unload ended
  the swarm for free. Starvation now marks a wipe debt (swarmWipePending) on
  the stack; if every shard is gone for 40 straight ticks with a wipe pending,
  manageShardSwarm settles it with the same recall and the same 3600 tick
  cooldown. The 40 tick delay keeps a shard in a briefly unloaded chunk from
  settling the wipe early. The normal arrival path is unchanged and clears the
  debt.
- fireLasers stack resolution: with no trident in either hand, the old code
  took the offhand item unconditionally, drained Ki, and silently skipped the
  cooldown. It now resolves main hand, then offhand, and returns before any
  charge if neither holds the trident.
- DevilSlamShockwaveEntity.getLifetime returned the ticks remaining, and the
  renderer divides age by it, so the fade accelerated and the slam visual
  effectively ended near tick 18 of its 35 tick life. It now returns the full
  life constant.

## Shared change requests

None required. Everything was built by calling the shared blocks as documented
(GroundScar.scar, DebrisSpawner.spawnField, LayeredParticles emitVortex,
emitGroundBurst, emitRain and dust, VortexPhysics.fling, FxEnvelope,
LayeredRender.distScale). One watch item for the coordinating pass, not a
change request: SwarmHelper.getMissingEntities counts a shard as missing when
it is absent from the manager's active list, and that list excludes recalling
shards. Today every recall path clears the health records in the same call, so
it is safe; a future caller that recalls shards without clearing records in
the same tick would respawn duplicates.

## Honest worse check

- Seal footprint: the seal draw factor went from radius x 1.35 to x 1.05 for
  telegraph honesty. Combined with radius 5 to 6, the drawn outer edge is 6.3
  blocks versus 6.75 before: the picture is a touch narrower while the real
  hit is bigger. Deliberate, kept, and flagged here for Carter's eyes.
- Shard DPS: capping each shard at one hit per 6 ticks lowers the swarm's
  theoretical single target DPS against the old every-tick blender (target
  invulnerability frames already thinned the old rate in practice). The health
  budget per shard is unchanged, so the swarm lasts proportionally longer and
  reads as swooping dives. This was one of the audit's named number problems;
  not reverted.
- Particle cost: shard trails raise swarm output from about 67 to about 540
  particles per second at density 1.0. It is gated by
  DbaEffectsConfig.effectDensity and halved under 0.5 density. If Carter's PC
  test shows frame cost, the cheap dial is the dust layer's tick rate.
- Deploy delay: the 8 tick telegraph means a fast target can step out of the
  seal before the hit lands. That is the standard's honesty rule working as
  designed, and it is a real play cost versus the old instant slam. Not
  reverted.
- Nothing was reverted outright. The Minecraft client cannot run in this
  sandbox, so these judgments are from the code; the final look call is
  Carter's PC test.

## Verification

Targeted javac compiles of the 7 weapon files (sourcepath over the full tree)
on both audit classpaths: zero errors in any Devil Trident file on 26.2 and on
26.3. The only errors present are in files owned by other agents or the
registry (DbaItems, DbaMenus, ZShockwaveEntity, OxShockwaveEntity,
KingsSlamEntity, CurseBladeItem, LavaSwordGround, mob entities), all outside
this weapon's scope and present independent of this diff. A full-tree sweep's
error kinds likewise contain no Devil Trident entries. No Gradle build was run
(sandbox daemon restriction; per project rule the build is Carter's PC step).
No em dashes in any added code, comment, or this report.

Left untouched on purpose: the audit's melee-doubles-as-laser mismatch
(hurtEnemy routing into the laser when not deployed). It was not in this
weapon's assigned fix list and changing melee behavior is a design call for
the coordinating pass.
