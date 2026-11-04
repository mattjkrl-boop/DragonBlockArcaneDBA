# Bespoke Pass Phase 2 Report: Saber

Base commit: 2ef870b (Phase 1 foundation). Worked, not committed, per the pass rules.
Owned files converted: item/SaberItem.java and the four renderers
(client/render/SaberSlashRenderer.java, SaberVoidTearRenderer.java,
SaberDodgeSparkRenderer.java, SaberDimensionalLineSlashRenderer.java).
The four entities (SaberSlashEntity, SaberVoidTearEntity, SaberDodgeSparkEntity,
SaberDimensionalLineSlashEntity) needed no edits: they already emitted only
through LayeredParticles on DbaParticles types and held no sounds, no vanilla
particles and no damage calls. HollowAfterimage untouched (Hollow's Edge owns it).

## Before and after counts (all 9 owned files)

| Check | Before | After |
| --- | --- | --- |
| Vanilla ParticleTypes references | 0 | 0 |
| DustParticleOptions references | 0 | 0 |
| Vanilla SoundEvents references | 9 (all in SaberItem) | 0 |
| damageSources() ability damage sites | 7 lines in SaberItem | 0 |
| KiRenderHelper primitive draws (box, cylinder, disc, sphere, helix) | 0 | 0 |
| Local draw helper methods in renderers | 14 | 0 |
| Hand-rolled addVertex lines in renderers | 108 | 0 |
| java.util.Random in renderers | 1 (DodgeSpark) | 0 |
| BespokeGeometry builder call sites | 0 | 36 across the 4 renderers |

Particles were already compliant before this pass (LayeredParticles with
DbaParticles KI_MOTE, WIND, VOID_SMOKE, SPARK only), so particle work was
verify-only. Nothing about the entity particle behavior changed.

## Sounds: 9 swaps in SaberItem, volumes and pitches unchanged

| Site | Before | After (per the DbaSounds guide) |
| --- | --- | --- |
| Blitz chain lock cue | PLAYER_ATTACK_SWEEP | DbaSounds.SWING_LIGHT |
| Blitz arrival | PLAYER_ATTACK_CRIT | DbaSounds.CRIT_HIT |
| Flash Step destination (teleport) | ENDERMAN_TELEPORT | DbaSounds.TELEPORT_WHOOSH |
| Flash Step destination (sweep) | PLAYER_ATTACK_SWEEP | DbaSounds.SWING_LIGHT |
| Perfect Dodge (block) | SHIELD_BLOCK | DbaSounds.SHIELD_CLANK |
| Perfect Dodge (crit) | PLAYER_ATTACK_CRIT | DbaSounds.CRIT_HIT |
| Finisher ignition (slam) | ANVIL_LAND | DbaSounds.SLAM_THUD |
| Finisher ignition (crit) | PLAYER_ATTACK_CRIT | DbaSounds.CRIT_HIT |
| Finisher ignition (thunder) | LIGHTNING_BOLT_THUNDER | DbaSounds.THUNDER_CRACK |

Decision on the thunder line: Rule 5 puts storm thunder in AtmosphereClient,
but the Saber finisher is not an AtmosphereServer storm, it is a one-shot
weapon sound at a fixed point, so the guide's thunder mapping applies and the
sound stays at the call site as DbaSounds.THUNDER_CRACK (SoundSource.WEATHER,
volume 1.5, pitch 1.4, all unchanged). The unused SoundEvents import was removed.

## Damage: ability damage through DbaDamage, melee untouched

| Site | Before | After |
| --- | --- | --- |
| Fencing lunge (200 + Str x 1.4) | playerAttack | DbaDamage.ki(player) |
| Blitz arrival 60 percent slice | playerAttack | DbaDamage.voidDmg(player) |
| Flash Step path 60 percent slice | playerAttack | DbaDamage.voidDmg(player) |
| PendingHit delayed 40 percent slices (blitz and Flash Step) | playerAttack, generic fallback | DbaDamage.voidDmg(player), DbaDamage.voidDmg(target, null) fallback |
| Finisher slices (50 / 30 / 20) | playerAttack, generic fallback | DbaDamage.voidDmg(player), DbaDamage.voidDmg(target, null) fallback |

Mapping rationale, per the brief: the lunge is a plain energy thrust, so it
takes the ki factory; everything dimensional (blitz cuts through space, the
Flash Step path cut, the finisher's reality line) takes voidDmg.
Every delayed slice flows through the PendingHit queue, so converting the
queue's source construction converts both splits at once. Amounts, splits,
cooldowns and costs are byte-identical to the spectacle pass.

Explained fallback change: when the attacking player is gone (removed or
dead), the old code fell back to damageSources().generic(). DbaDamage
factories throw if both entities are null, and PendingHit stores no direct
entity other than the target, so the fallback is now
DbaDamage.voidDmg(target, null): the registry holder is resolved from the
target's level and the hit keeps the void type with no attacker, which is
the closest honest match to generic's no-attacker semantics. Amounts on
this edge path are unchanged. The plain melee weapon hit never went through
these lines and still uses the vanilla player attack path, per the standard's
honest exception.

## Geometry: builders used, per renderer

Seeds are synced and frame stable. Slash, VoidTear and LineSlash render
states now extract a seed built from the server-assigned entity id hashed
with the entity's synced fields (tilt and scale; scale and destination flag;
direction and line length). DodgeSpark keeps its existing synced UUID seed,
now fed to the toolkit instead of java.util.Random.

SaberSlashRenderer (was: volumetric parabola crescent, curved tip spurs,
diamond spark):
- bladeArc x3 for the crescent layers (outer at ALPHA_OUTER, cyan mid at
  0.86, white core), frame rotated so the arc bulge faces the old chord
  direction; radius = span x 1.5 so the arc width tracks the old span.
- bladeArc x1 flipped 180 degrees for the strong variant's second crescent.
- branch x4 for the tip spurs (cyan body plus white core streak, both tips).
- crackStar + shardFan for the strike flash, seated mid-claw.

SaberVoidTearRenderer (was: diamond slit, curved spikes, 16 segment cone,
two equal segment rings):
- tornRibbon x4 for the slit: dark violet halo (0.15, 0, 0.32) at fade x
  0.55, cyan layer at 0.95, and the white core woven from two ribbons run
  in opposite directions so both ends of the slit burn. Ribbons are stood
  vertical with translate plus Z and X rotations; the destination boost
  (width x1.30, core alpha 1.0 vs 0.8) is preserved.
- branch x12 for the 6 rotating spikes (cyan body plus white core each).
- displacedDome x1 for the sonic cone (white, same post-40-percent fade-out).
- brokenVortexBand x2 for the counter-rotating shock rings, twist carrying
  the old spin rates (age x 40 degrees and age x -55 degrees), radii and
  alphas as before.

SaberDodgeSparkRenderer (was: 3 clash rings, 8 bowed spike quads, 12 flying
diamond shards, diamond core flash, additive halo):
- brokenVortexBand x3 for the clash rings (radii are the means of the old
  inner and outer radii; twists carry age x 35, age x -50, age x 22), drawn
  in two orientations like the old double submit.
- branch x16 for the 8 spikes (gold body plus white core).
- branch x12 for the flying shards, positions from BespokeGeometry.Rng on
  the same synced UUID seed, consumed in the same order as the old Random.
- plume x1 for the core flash, plume x1 oversized plus brokenVortexBand x1
  for the additive halo pass.

SaberDimensionalLineSlashRenderer (was: tapered tube beam, 3 traveling
rings, impact ring, 5 diamond ribs, inline helix strip):
- tornRibbon x4 for the beam: cyan sheath plus white core in a flat pair
  and an upright pair (crossed, so the cut has volume), spanning
  -halfLen to +halfLen with the same cut growth.
- brokenVortexBand x3 traveling bands (same travel speed and fade profile)
  plus x1 impact ring on the same 0.32 to 0.62 strike window.
- shardFan x5 for the transversal ribs, same spacing and sine brightness.
- The hand-rolled helix is deleted with no builder substitute: the
  ribbons' torn, jittered edges replace it, which is the toolkit's intent.
- The finisher's lightning bolt is drawn by the shared BoltEntity and
  BoltRenderer, not locally, so the brief's branch-for-bolt item did not
  apply here.

The local smooth() helpers were replaced by FxEnvelope.smoothstep (the
identical curve). strike() pulse helpers stay: they are alpha curves, not
geometry. All pose rotations, envelope fractions, colors, layer alphas,
lifetimes and distance scaling are unchanged from the spectacle pass.

## Numbers and mechanics preserved

Verified by diffing the item and entities against the spectacle report:
blitz 280 + Str x 1.8 split 60/40 at 5 ticks, travel 6 to 16 ticks, lunge
200 + Str x 1.4, Flash Step 320 + Str x 1.6 split 60/40, Ki cost 2.5 percent
charged only after the immobilized check, 3 charges at 60 ticks, Perfect
Dodge window 15 ticks, finisher 1000 + Str x 4.5 split 50/30/20 at beam
ticks 0/8/16, snap 18 ticks, cooldown 100 ticks, debris 28 pieces (min 8),
one finisher bolt 0xFFBFEFFF length 14 life 8, and all entity lifetimes
(16/14/14/18/16/20/20/34) are untouched. No damage number, split, cost or
cooldown changed anywhere in this conversion.

## Verification

Targeted javac of the 9 owned files with -sourcepath over both source sets,
the spectacle pass method, on both audit classpaths
(~/workspace/dba-audit-libs/cp262.txt and cp263.txt), JDK 25:
- 26.2: zero error lines in Saber-owned files.
- 26.3: zero error lines in Saber-owned files.
The full compiles still report the known pre-existing baseline error kinds
in files this conversion does not own (DbaItems, DbaMenus, the hostile
entity set, LavaSwordGround on 26.3); none are new and none are in Saber
files. Gradle was not run: the compile check stays waived per the standing
rule for this VM, and this report does not claim a Gradle build.

## Honest worse check

No conversion was reverted; nothing reads clearly worse overall, but four
silhouette changes are real and worth knowing:
1. The slash crescent is now a bladeArc claw: deeper than the old flat
   crescent (depth roughly equals width instead of a 0.36 depth ratio).
   This is the toolkit's designated slash shape and the biggest single
   look change in this conversion. If it reads worse in game, the radius
   and sweep are two numbers in one submit block.
2. The line slash beam tapers toward one end (tornRibbon's built-in
   taper) instead of pinching at both ends like the old tube, and it is a
   crossed pair of flat ribbons rather than a round tube. The bright end
   sits at the snap start, which suits the cut reading.
3. The void tear's sonic cone is now a dome shell: same radius and fade
   window, shorter forward reach (1.4 x scale vs the cone's 2.2 x open x
   scale length).
4. Dodge spark spikes and void spikes are branch streaks: jagged and
   crack-like with uniform alpha, where the old strips were smooth, bowed
   and gradient faded. For a deflection clash and a void tear the jagged
   read fits the themes, but it is a texture change, not a pure upgrade.

## Change requests for the coordinating pass

One optional toolkit gap, not a blocker: bladeArc's inner radius is fixed
at radius x 0.5, so slash weapons cannot reproduce a thin crescent
footprint with it; the Saber accepted the deeper claw. If other slash
weapons (Katana, Spirit Sword) hit the same wall, a thinness parameter on
bladeArc would be the shared fix. No private builder was written for it.
