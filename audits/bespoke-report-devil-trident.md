# Bespoke Report: Devil Trident

Phase 2 conversion of the Devil Trident at base commit 2ef870b, per
audits/BESPOKE-STANDARD.md. Files changed (the seven owned files only):

- item/DevilTridentItem.java
- entity/DevilSlamShockwaveEntity.java
- entity/TridentProngEntity.java
- entity/TridentShardEntity.java
- client/render/DevilSlamShockwaveRenderer.java
- client/render/TridentProngRenderer.java
- client/render/TridentShardRenderer.java

util/SwarmHelper.java was read only and is unchanged. No shared file was
edited. Nothing committed.

## Before and after counts

| Category | Before | After | Notes |
| --- | --- | --- | --- |
| Vanilla ParticleTypes references | 0 | 0 | Already clean at base: the files used DbaParticles (KI_MOTE, EMBER, SPARK) and LayeredParticles.dust only. Verified by grep, nothing to swap. |
| Vanilla DustParticleOptions references | 0 | 0 | Same as above. |
| Vanilla SoundEvents references | 6 | 0 | All six swapped to DbaSounds, plus one misassigned bespoke event corrected (see Sounds). |
| Vanilla damageSources() ability branches | 3 sites (6 lines) | 0 | Prong, slam, and shard ability damage now go through DbaDamage.hellfire. Plain melee keeps the vanilla item attack path per the standard's honest exception; these files never call a melee damage source explicitly, so there is nothing to convert there. |
| KiRenderHelper primitive calls (drawColoredBox, drawCylinder, drawDisc, drawSphere, drawHelix) | 0 | 0 | The spectacle pass had already moved this weapon off the primitives; the problem was local hand-rolled geometry instead. |
| Local draw helpers in the three renderers | 24 | 0 | Every private draw, quad, taper, tri, crystal, wake, and head helper deleted. The only remaining private method is TridentShardRenderer.submitBlade, a pose composition helper that calls BespokeGeometry.bladeArc and nothing else; TridentProngRenderer calls it too, so the crescent composition exists exactly once. |
| Math.cos / Math.sin lines in the three renderers | 57 (slam 36, shard 11, prong 10) | 0 | All regular segment loops are gone; every vertex now comes from a BespokeGeometry builder. |

## Geometry: what each element is now

Seeds: slam renderer already carried a UUID seed in its render state and
keeps it. Prong and shard render states now also carry a seed derived from
the entity UUID (most significant bits xor least significant bits), set in
extractRenderState from synced entity identity, identical on every client.
All builders receive that seed, the weapon colors, the lifecycle envelope,
and LayeredRender.distScale, matching the standard.

DevilSlamShockwaveRenderer (was 445 lines of local geometry, now
builder composition only):

- Seal disk body: glowFan (dark red pool at 0.55 of the seal radius).
- Fissure trenches and brimstone disk marks: crackStar twice, a dark red
  web at full radius and a hotter nested web at 0.8 radius, replacing the
  old trench boxes and the separate ring family.
- Erupting spikes: shardFan twice, dark shells at 0.98 radius and a hotter
  inner field at 0.6 radius, grown in with the seal via the envelope.
- Seal rim at the hit edge: brokenVortexBand, slowly twisting, replacing
  the stacked ground rings; the traveling follow echo is a second, fainter
  brokenVortexBand at the same follow radius math as before.
- Core pool: glowFan at the center, plus an additive echo in the second
  pass at the same alpha as before.
- Hellfire column: plume three times (dark outer gout, hotter inner stack,
  additive core), replacing the nested open cylinders and the helix. Pillar
  height, radius growth, and alpha curve math are unchanged.

TridentShardRenderer:

- Head: bladeArc crossed crescents via the shared submitBlade composition:
  a vertical crescent (shell radius 1.0, core 0.78, sweep 150 degrees) and
  a horizontal crescent at 0.92, each a dark shell arc with a hotter core
  arc. This replaces drawBarbedHead, headTaper, and headTri.
- Collar barbs and crystal: shardFan burst at the head base.
- Wake: tornRibbon twice (wide wispy outer, thin bright core) rotated to
  trail behind the blade, replacing drawHelicalWake and wakeSpan.
- Halo pass: the same crescents again on the additive type at the old halo
  alpha and pose scale, as before.
- Kept exactly: UUID size steps (0.80 to 1.16 in five steps), axial roll at
  22 degrees per tick, body pulse 0.93 plus 0.07 sin, spawn scale-in over
  5 ticks, overall scale 0.85 times size factor.

TridentProngRenderer:

- Each of the three needles is the same crossed crescent composition as
  the shards (radii 0.95, 1.25, 0.95 for the old side and center lengths,
  horizontal plane at 0.6 so the formation stays distinct), replacing the
  duplicated drawNeedle geometry.
- Barbs: shardFan at each needle base.
- Wake: tornRibbon twice per needle, envelope driven by the needle fade,
  so the trail still dies on impact exactly when the old wake did.
- Kept exactly: offsets -0.26, 0, 0.26, intro fade over 2 ticks, dissolve
  over 6 ticks after dieTick, orientation from synced yRot and xRot.

## Particles

Nothing to swap. All spawns already used DbaParticles (KI_MOTE, EMBER,
SPARK) and LayeredParticles.dust with DbaDustOptions. Verified zero
ParticleTypes and zero DustParticleOptions references before and after.

## Sounds

Six vanilla references swapped per the DbaSounds swap guide, call sites,
volumes, and pitches unchanged:

- DevilTridentItem swarm target mark: SoundEvents.ZOMBIE_VILLAGER_CONVERTED
  to DbaSounds.MARK_HIT (the audit's dark conversion fake; the guide maps
  dark marks to MARK_HIT).
- DevilTridentItem deploy cast: SoundEvents.GENERIC_EXPLODE to
  DbaSounds.BOOM_BIG.
- DevilSlamShockwaveEntity slam hit: SoundEvents.GENERIC_EXPLODE to
  DbaSounds.BOOM_BIG.
- TridentProngEntity block hit: SoundEvents.TRIDENT_HIT_GROUND to
  DbaSounds.TRIDENT_HIT (the bank has no separate ground event; the guide
  maps trident contact sounds to TRIDENT_HIT).
- TridentProngEntity entity hit: SoundEvents.TRIDENT_HIT to
  DbaSounds.TRIDENT_HIT.
- TridentShardEntity shard hit: SoundEvents.TRIDENT_HIT to
  DbaSounds.TRIDENT_HIT (HOSTILE source kept).

One correction beyond the vanilla count: DevilTridentItem.fireLasers played
DbaSounds.LIGHTNING_ZAP for the trident shot. That is bespoke but the wrong
identity for this weapon, so it now plays DbaSounds.TRIDENT_THROW at the
same volume and pitch. Total DbaSounds call sites in the seven files: 7.

## Damage

All ability damage now goes through DbaDamage.hellfire, which is this
weapon's family per the standard (hellfire red and black):

- TridentProngEntity pierce hits: hellfire(this, owner), replacing the
  playerAttack or magic branch. Pierce shares unchanged: 750, 495, 337.5
  across up to three targets, MARKED_BY_EVIL 60 ticks per hit.
- DevilSlamShockwaveEntity slam: hellfire(this, owner), replacing the
  playerAttack or magic branch. 750 damage at tick 8 of 35, radius 6.0,
  full to 3.0 with a 0.4 floor at the rim, fling, effects, GroundScar disk,
  and debris field all unchanged.
- TridentShardEntity shard hits: hellfire(this, this.getOwner()), replacing
  the mobProjectile or magic branch. 100 plus Strength times 0.5, hit
  spacing, and recoil unchanged.

The hellfire damage type JSON mirrors the vanilla shapes per the standard,
so amounts, scaling, and armor behavior are unchanged; only the type,
death message, and attribution identity are now the mod's own.

## Mechanics preserved (checked against the spectacle report)

Nothing but the swaps above changed on the common side; the git diff of
the four common files contains only the sound and damage lines. Shard
health budget 15000, swarm upkeep Ki 20 per minute, starvation wipe
cooldown 3600 ticks on every end path including settlement after 40
straight shard-free ticks, manual recall cooldown 600, deploy buffer
cooldown 20, orbit radii 2.2, 2.7, 3.2 with alternating directions and
staggered heights, ring spawn at the slam crater with emerge over 8 plus
2 times index ticks, shot cooldown 10 ticks, Ki drain 60 per 10 ticks,
prong dissolve after tick 28 over 6 ticks, slam scar chances 0.50, 0.38,
0.22 with restore at 240 ticks: all untouched.

## Toolkit gaps and change requests

None blocking. Two notes for the coordinating pass:

- TridentShardRenderer.submitBlade is the crescent composition shared by
  the shard and prong renderers. If a later weapon wants the same crossed
  crescent, promoting submitBlade into LayeredRender would be the shared
  home; it is composition only (pose operations plus bladeArc calls), so
  it is not urgent.
- The slam's old stacked seal rings are now expressed as glowFan plus
  brokenVortexBand. If the coordinating pass wants a true flat ring
  builder for other weapons, that is a BespokeGeometry change request,
  not something this weapon needs.

## Verification

Targeted javac (JDK 25) on the seven files with the full tree as
sourcepath:

- 26.2 classpath (cp262.txt): zero errors in any Devil Trident file. The
  compile's 7 total errors are in other files (DbaItems and DbaMenus
  baseline kinds, and BanshoCycloneEntity, another weapon's in-flight
  conversion).
- 26.3 classpath (cp263.txt): zero errors in any Devil Trident file. The
  17 total errors are all in other files (baseline kinds plus other
  agents' in-flight work).

Compile check is a javac audit only, per standing rules; no Gradle build
was run or claimed. No em dashes in any changed file (verified in the
diff).

## Honest worse check

Two places where the bespoke version reads differently from the spectacle
version, stated plainly rather than hidden:

- The prong and shard heads were barbed needles with a wrapped shaft
  texture read; they are now crossed crescent blades. The silhouette is
  bolder and more claw-like, the theme colors and hot core are the same,
  and the shard keeps its roll, pulse, and halo. This is a silhouette
  change the standard's builder set forces (there is no needle builder),
  and it reads as the same weapon family, not a worse one. If Carter
  prefers the needle read, the fix is a shared needle builder in
  BespokeGeometry, which is a coordinating-pass decision.
- The slam seal lost its many thin stacked rings; glowFan plus the crack
  webs plus the broken rim band carry the same dark body, hot edge, and
  hit-radius honesty with fewer, larger shapes. The trench read is
  stronger (real wandering cracks instead of tapered boxes), the ring
  read is weaker. Net: not worse overall, but the ring stack is the one
  element to eyeball first in game.

Everything else is a like-for-like swap: same numbers, same timings, same
colors, same envelopes, with the geometry, sounds, and damage now the
mod's own throughout.
