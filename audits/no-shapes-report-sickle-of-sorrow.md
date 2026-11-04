# No-Shapes Report: Sickle of Sorrow

Phase 2 weapon report for the no-shapes realism pass. Files owned and touched:
client/render/DimensionalRiftRenderer.java and
client/render/SorrowSlashRenderer.java. Those are the two files the audit
assigns to this owner. The item, both entities, and every other Sickle file
are untouched, so every damage number, cooldown, Ki cost, timing, mechanic,
and synced data field is identical, including the spectacle pass Melting
strength in DimensionalRiftEntity (distance-based amplifier, capped at
amplifier 3 for 200 ticks), which lives server side and was not edited.
TimeShatterEntity and its renderer belong to the TimeShatter system owner and
were not touched, even though this weapon family uses time effects.

Both renderers now draw no geometry at all. Their submit methods only feed
the shared material system (client/fx/MaterialEmitters) and call the vanilla
super submit. Theme and colors are the bespoke pass palette throughout:
violet and magenta void plasma, soul wisps, near-black darkness smoke, and
pale cyan glass dust at the rift core.

## Before and after geometry counts

Counts are source call sites, matching the audit's B, L, and V columns.

| File | Category | Before | After |
| --- | --- | --- | --- |
| DimensionalRiftRenderer | BespokeGeometry builder calls (brokenVortexBand 5, displacedDome 4, shardFan 2, tornRibbon 2) | 13 | 0 |
| DimensionalRiftRenderer | LayeredRender geometry calls (drawAnchorGlow, drawVortexBands) | 2 | 0 |
| DimensionalRiftRenderer | submitCustomGeometry blocks | 5 | 0 |
| DimensionalRiftRenderer | KiRenderHelper references (kiRenderType) | 1 | 0 |
| DimensionalRiftRenderer | Direct addVertex calls | 0 | 0 |
| SorrowSlashRenderer | BespokeGeometry builder calls (bladeArc, inside the submitArc helper that ran six crescent layers per frame) | 1 | 0 |
| SorrowSlashRenderer | LayeredRender geometry calls | 0 | 0 |
| SorrowSlashRenderer | submitCustomGeometry blocks | 1 | 0 |
| SorrowSlashRenderer | KiRenderHelper references (kiRenderType) | 1 | 0 |
| SorrowSlashRenderer | Direct addVertex calls | 0 | 0 |
| Owner total per the audit | B / L / V | 14 / 2 / 0 | 0 / 0 / 0 |

The submitArc helper in the slash renderer and every pose-stack transform in
both files are deleted. Distance scaling no longer comes from
LayeredRender.distScale; it comes from MaterialEmitters.distanceScale.

## How emission is wired

Each renderer keeps a small per-entity rig keyed by the render state's seed,
which is derived from synced entity state exactly as before (entity id,
position, tilt, and impact flags), so every client seeds the same material
variation. A rig holds shared MaterialEmitters.Emitter instances whose
MaterialEmitters.Source samplers read the rig's current center, size, and
phase fields; submit refreshes those fields every frame and ticks the
emitters with a wall clock delta capped at 0.05 seconds, so emission is
frame-rate independent and runs through the toolkit's accumulator and
per-second budgets. The tick envelope is the effect's old lifecycle fade
multiplied by MaterialEmitters.distanceScale. Rigs are pruned after five
seconds unseen and reset on level change, age reset, or (for the slash) an
impact flag change or a position jump over 30 blocks. No private particle
system, texture, or geometry helper was created; the sources are samplers
fed to the shared Emitter, as the standard prescribes.

## Dimensional Rift: materials per element

Seven continuous emitter channels, 335 particles per second in total at full
envelope before density config and distance scaling:

- Accretion volume: DARKNESS (0x0A0012, scale 2.6) sampled through the rift
  volume with a slow swirl, 55/s. This replaces the outer broken vortex band,
  the middle and inner bands, the shared vortex bands, and the anchor glow.
- Inward soul stream: SOUL (0x9900E6, the entity's own THEME_ARGB, scale 2.0)
  sampled through the same volume with inward velocity driven by the pull
  value (0.22 + pull x 0.30) plus a tangential swirl, 85/s. The stream
  strengthens exactly when the vortex pull strengthens, including the
  collapse boost.
- Vertical fissures: PLASMA (0xCC0DF2, the old tear color, scale 3.0) sampled
  in the tall central column the three torn ribbons stood in, easing toward
  the axis, 65/s.
- Singularity core: PLASMA (0xF5EEFF, scale 2.6) sampled inside the old core
  volume with inward drift, 45/s, plus GLASS_DUST (0xCCF2FF, scale 1.4) in
  the same volume, 20/s, carrying the fracture read the singularity's hard
  body used to give. Core size keeps the old formula: max(0.55, 1.15 x pulse)
  x (1 - pull x 0.22), sampled at 1.45x that radius.
- Orbiting clusters: the 18 floating pyramids of the spectacle pass, which
  the bespoke pass flattened into two shard fans, are now nine seeded
  cluster centers of SOUL (0xE633FF, scale 2.2, 40/s) and DARKNESS (0x0D001A,
  the old void strip color, scale 2.2, 25/s). The clusters ride the two old
  orbit bands (0.90 and 0.68 of the old orbit radius), keep the old inward
  easing as the pull grows (0.90 - 0.58 x pull) and the old 0.8x orbit
  speed, and each point is sampled with up to 0.55 blocks of jitter plus a
  bob, with tangential and inward velocity. They are loose knots of
  material, never solids.

Carried over unchanged: the FxEnvelope build and decay fractions from the
entity constants, the pulse (0.85 + 0.15 x sin(age x 0.25)), the fissure
height formula (min(8, 2.5 + radius x 0.4) x pulse), and the collapse timing.
The entity's server-side LayeredParticles vortex, collapse rain, debris
field, and GroundScar soul soil scar are untouched.

One audit phrase is not literally implemented: the rift row names irregular
crack decals. The foundation provides no decal system, and drawing a decal
would be geometry, which this pass forbids. The rift already marks its
surface through the entity's server-side soul soil scar, which stays, and
GLASS_DUST carries the fracture read in the air. Recorded here as a
disposition, not silently dropped.

## Sorrow Slash: materials per element

Five continuous emitter channels, 330 particles per second in total at full
envelope, all sampled across the swept volume the crescent used to occupy.
The volume basis is the entity's real look direction, with the old reap tilt
applied as a rotation of the sampling axes (42 degrees, plus the flying
wave's settle roll of 14 degrees through the same exp decay), and the old
open scale (0.72 + ease x 0.48 flying, 0.85 + ease x 0.45 impact) driving
the volume size. Sampled points get a trailing velocity back along the
travel direction, so the material reads as a wake.

- Crest: PLASMA (0xCC0DD9, the old magenta crest color, scale 2.6) across
  the full swept volume, 130/s.
- Edge: PLASMA (0xE6FAFF, the old cyan-white edge color, scale 1.9) in the
  central 45 percent band, 85/s.
- Souls: SOUL (0xB84DFF, scale 1.8) across a slightly larger, deeper volume,
  55/s.
- Void wake: DARKNESS (0x140026, the old silhouette color, scale 2.4) in
  the largest, deepest volume, 35/s.
- Vapor wash: VAPOR (0xBFEFFF, scale 2.2), 25/s.

The impact burst keeps its opening pop as material bursts instead of the
old crossing crescent: 24 PLASMA, 14 SOUL, and 9 DARKNESS at full envelope,
scaled by the same fade and distance envelope, fired once per burst entity.

Carried over unchanged: the lifecycle fade (smoothstep ease over 30 ticks
flying and 16 ticks impact, with the flying wave's four tick intro fade),
and the entity's own server trail and impact particles.

## Verification

Targeted javac audit (the sandbox cannot run Gradle; this is the repo's
audit method, not a build). Both renderers were compiled with the full
sourcepath against the 26.2 and 26.3 audit classpaths, before edits and
after.

- Before: 4 closure errors on 26.2 and 10 on 26.3, zero naming either owned
  file.
- After: 4 closure errors on 26.2 and 10 on 26.3, zero naming either owned
  file. The error sets are the known pre-existing baseline kinds only
  (DbaItems CreativeModeTab access and DbaMenus MenuType access on both
  versions, plus the 26.3 mob entity kinds in DbaHostileEntity,
  FriezaSoldierEntity, NamekianWarriorEntity, and PatrolRobotEntity).
- Class files for both renderers, both render states, and both emitter rigs
  were emitted on both classpaths.
- Final greps on the two owned files: BespokeGeometry 0, LayeredRender 0,
  addVertex 0, KiRenderHelper 0, submitCustomGeometry 0, em dashes 0.
- Not committed, per the coordination rules.

## Shared change requests

None. The Phase 1 toolkit covered both effects: the Emitter accumulator,
the volume, sphereVolume, and burst entry points, and the PLASMA, SOUL,
DARKNESS, VAPOR, and GLASS_DUST presets were sufficient. The bespoke pass
change request for a seeded shard orbit builder is superseded by this
conversion: orbiting solids are exactly what the no-shapes rule removes,
and the cluster sampler above replaces them within the shared toolkit.

## Worse check

Honest trades, none judged a fail, nothing reverted:

- The slash has no crescent silhouette at all now. That is the rule working
  as directed, but the reap's width reads from a moving particle wake, so
  at far distances or low effects density it will read thinner than the old
  moon did. The swept volume keeps the old span and scale, so the near read
  is full width.
- SOUL and DARKNESS particles live up to about 3.5 seconds by material
  profile, so the slash leaves a dissolving sorrow haze along its path
  after the wave entity dies at 30 ticks, where the old crescent vanished
  with the entity. It fits the theme and the entity already trailed void
  smoke server side, but it is a real linger change.
- The rift's singularity no longer has a dark body. The black hole read now
  depends on the surrounding darkness volume and the inward soul stream
  converging on a bright plasma and glass dust knot. If Carter judges the
  core too bright in game, the fix is a color and rate change on two
  emitter presets in this file, not a geometry return.
- Orbit clusters are statistical: at low density a given cluster can be
  nearly empty for a second. The nine cluster centers keep the orbit
  populated overall, but no single cluster is guaranteed a minimum count.
- The continuous emitters emit nothing on an entity's first rendered frame
  (the accumulator delta starts at zero). The slash impact burst covers
  this for bursts, and the rift builds over 20 ticks, so in practice the
  gap is not visible; it is recorded because it is real.
- In-game look is unverified, as with every pass in this environment.
  Carter's PC test is the first real read.
