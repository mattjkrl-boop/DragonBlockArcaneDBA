# No-Shapes Report: TimeShatter

Owner: TimeShatter system owner, no-shapes realism pass Phase 2.
Files owned: entity/TimeShatterEntity.java and
client/render/TimeShatterRenderer.java. Only the renderer changed.
The entity needed no edit: the Scope 2 pass already gave it the 36
tick life, the synced SEED field, and the unchanged constructor, and
this conversion consumes exactly that state. Nothing committed.
Measured at HEAD ad22b32 plus the other Phase 2 owners' in-flight
work already in the tree, which this report does not touch.

## Before and after geometry counts (renderer, grep verified)

- BespokeGeometry builder call sites: 11 to 0
  (2 brokenVortexBand, 2 branch, 1 tornRibbon, 2 shardFan,
  4 displacedDome)
- BespokeGeometry references of any kind, including the 2 Rng
  stream constructions: 13 to 0
- LayeredRender geometry calls: 0 to 0
- LayeredRender references (the distScale utility call): 1 to 0;
  distance scaling now comes from MaterialEmitters.distanceScale
- KiRenderHelper references (the shared render type): 1 to 0
- KiRenderHelper.vertex calls: 0 to 0
- submitCustomGeometry calls: 4 to 0
- Direct addVertex calls: 0 to 0
- Local vertex-emitting helpers: 0 to 0

The renderer now submits no geometry at all. Its submit method only
drives material emission and calls super.

## Conversion design

The dial, spokes, shards, and core stopped being drawn constructs.
Their placement math survives as emission data, sampled through the
shared MaterialEmitters toolkit (Preset, Source, burst) and nothing
else. No private particle system, texture style, or geometry helper
was added; the four private methods in the renderer are Source
factories for the shared emitter, returning points and velocities
as data.

Emission runs once per whole tick per entity (a tick guard keyed by
level, entity id, and seed), so frame rate cannot multiply the
burst, and a tick missed while off screen is never caught up in a
spike. Counts scale by the existing FxEnvelope over the 36 tick
life (0.14 build, 0.50 decay, carried over from Scope 2) and by
MaterialEmitters.distanceScale. Every position stream is seeded
from the entity's synced SEED, so all clients sample the same
field. Base rate is 34 particles per tick before envelope, density
config, and distance scaling.

The streams, in the order they emit each tick:

- Suspended glass dust, three color streams (pale cyan, white,
  lilac) sampled inside a softly flattened volume at the old burst
  radius. Each sample's velocity points back toward the center, so
  the dust drifts against the outward shatter flow instead of
  riding it. This is the main body of the effect now.
- Crack character: glass dust clustered along the eight old spoke
  lines in the camera plane. The spoke angles, lengths, and depth
  jitter are rebuilt from the same seed stream the Scope 2 spokes
  used (seed xor 0x5907E), with per-sample jitter across the line.
  The lines are sampling data only; nothing connects them, and the
  irregular clustering plus the glass dust flipbook texture carry
  the fractured read.
- Shard trajectories: the sixteen old flight paths, rebuilt from
  the old shard seed stream (angle, speed, y offset, z factor, with
  the same per-shard stream consumption), sampled at the current
  travel distance with a small cluster radius. The palette cycle is
  preserved by splitting the paths into their mod-3 thirds: white,
  cyan, lilac. Dust velocity points inward along the path, against
  the flight, reinforcing the time-reversal feel.
- Core accents: pale cyan PLASMA and lilac SOUL motes in a tight
  volume at the center, where the old radiant core sat. Plasma
  carries the hot center; soul carries the softer temporal tone.
- Color-split ghosting: two glass dust streams sampled around the
  old dial radius in the camera plane, offset to opposite sides of
  the center by 0.055 times scale, one in the dial's cyan and one
  in near-white. The split is carried by the offset emission
  itself. Local world-space distortion is not faked: the Phase 1
  shader finding rules it out in this codebase, and no translucent
  construct stands in for it.

Colors are the old palette throughout: dial cyan (0.60, 0.90, 1.0
family), near-white, spoke cyan (0.50, 0.85, 1.0), and the shard
cycle of diamond-white, cyan, and lilac. The overall scale curve
(0.55 plus travel times 0.85) and the cubic ease-out travel are the
Scope 2 renderer's, unchanged.

## What did not change

- TimeShatterEntity: byte-identical. Lifetime 36, maxLifetime 36,
  synced SCALE and SEED, both constructors, save and load.
- WhisStaffItem: read only here. Its three TimeShatter spawn sites
  and the (Level, LivingEntity, Vec3, float) constructor they call
  are untouched, and the item compiles in the verification closure
  below on both classpaths.
- Damage, reversal mechanics, lifetime, and cooldowns: the entity
  still deals no damage and drives no mechanics of its own; the
  Whis Staff reversal timing lives in the item and is untouched.

## Verification

Targeted javac, JDK 25, sourcepath over both source sets,
compiling TimeShatterEntity, TimeShatterRenderer, and
WhisStaffItem together so the caller closure is proven:

- 26.2 (cp262.txt): 4 errors total, all pre-existing baseline
  kinds in files this task never opened: DbaItems line 535
  (CreativeModeTab Output protected access, 2) and DbaMenus
  line 16 (MenuType private access, 2). Zero errors in
  TimeShatterEntity, TimeShatterRenderer, or WhisStaffItem;
  class files emitted for all three.
- 26.3 (cp263.txt): 10 errors total, all in the known 26.3
  baseline files: DbaItems (2), DbaMenus (2), DbaHostileEntity
  (2), PatrolRobotEntity (2), FriezaSoldierEntity (1),
  NamekianWarriorEntity (1). Zero errors in the two owned files
  or WhisStaffItem; class files emitted for all three.
- Grep proof on the converted renderer: BespokeGeometry 0,
  LayeredRender 0, KiRenderHelper 0, submitCustomGeometry 0,
  addVertex 0.
- Em dash scan of the renderer and this report: 0.

This is the javac audit method. No Gradle build was run or is
claimed, and no in-game look is claimed from this environment.

## Honest worse check

Nothing reverted, but the trades are real:

- The burst is less legible as a time shatter at a glance. The
  geometry version announced itself with a clear dial, spokes, and
  flying shards; the material version asks the viewer to read
  clustering, drift, and color split. Up close it should read as
  suspended fractured glass. At far distance, where distance
  scaling thins the counts, it may read as a generic pale sparkle
  cloud. That is the known cost of the no-shapes rule on this
  effect, stated plainly for Carter's PC judgment.
- Suspended is approximate. The foundation GLASS_DUST profile
  falls fast and bounces on ground contact; the suspension read
  is carried by continuous re-emission across the full 36 ticks
  and by the inward drift velocity, not by hovering physics.
  Some motes visibly rain out of the volume, especially in the
  back half of the life. Fixing that properly is a foundation
  profile change, listed below as a change request rather than
  faked locally.
- The color split is subtle. A 0.055 scale offset between two
  dust streams is a shimmer, not a dramatic chromatic tear. It
  was kept small so the ghosting reads as one event with split
  edges rather than two separate clouds.
- The crack clustering is deliberately sparse (9 base particles
  per tick across eight lines). On a busy background the crack
  read may vanish even though the volume dust stays visible.

## Shared change requests

One, for the coordinating pass: consider a lower gravity variant
of the GLASS_DUST profile (or a per-material gravity scale in
DbaMaterialOptions) for effects that need genuinely suspended
glass, TimeShatter first among them. The current profile's fast
fall is right for impact glass spray and wrong for a temporal
suspension field. Not implemented here; DbaMaterialParticle and
DbaMaterialOptions are shared and read-only for this owner.
