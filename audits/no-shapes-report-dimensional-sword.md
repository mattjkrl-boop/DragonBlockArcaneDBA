# No-Shapes Report: Dimensional Sword

Phase 2 conversion of the Dimensional Sword to the No-Shapes Realism
Standard (audits/NO-SHAPES-STANDARD.md), on top of Phase 1 commit ad22b32.
Files owned and touched: client/render/DimensionalSlashRenderer.java and
client/render/DimensionalWarpRiftRenderer.java, exactly the two files the
audit assigns to this owner. The entity files, the item, and the effect/
status files were not touched. Not committed.

## Before and after geometry counts

Counts are call sites in the owned renderers, matching the audit rows
(B is BespokeGeometry builder calls, L is LayeredRender geometry calls,
V is direct vertex draws).

| Renderer | B before | B after | L before | L after | V before | V after |
|---|---|---|---|---|---|---|
| DimensionalSlashRenderer | 12 | 0 | 1 | 0 | 0 | 0 |
| DimensionalWarpRiftRenderer | 13 | 0 | 2 | 0 | 0 | 0 |
| Owner total | 25 | 0 | 3 | 0 | 0 | 0 |

Also verified zero after conversion, in both files: BespokeGeometry
references of any kind (including Rng), LayeredRender references,
KiRenderHelper references, submitCustomGeometry calls, and addVertex
calls. Both renderers now draw nothing; their submit methods only emit
particles through client/fx/MaterialEmitters and call super.submit.

## How the conversion works

Render states in this version are transient per frame, so each submit
builds its MaterialEmitters.Emitter set from the synced render state and
ticks it with the deterministic form, tick(1/60, envelope), where the
envelope already carries the lifecycle fade multiplied by the
FxEnvelope distance scale. Every emitter is constructed with a hard
per-second budget at or slightly above its rate. Emission points are
data only: cut-plane samples, sphere volumes, ground areas, and one
inward-stream source, all per the standard.

### DimensionalSlash: swing-driven noisy plasma trail

The audit assignment: swing-driven noisy plasma trail, vapor, sparks, and
dissolving material; theme void plasma, spatial glass dust, darkness
vapor.

- The cut body is three plasma stops sampled across the actual cut
  plane (the entity's synced yaw, pitch, and plus or minus 35 degree
  tilt, with the old sin(age x 0.4) 6 degree spin wobble): abyssal violet
  0xA60DF2 at rate 300, neon blood-magenta 0xFF1A59 at 240, white tear
  0xFFFFFF at 170. Sample velocities stream backward along the flight
  path and slightly outward at the tips, so the plasma sheds off the cut.
- The singularity pearl is now a dense plasma cluster at the same spot:
  white at 130 inside radius 0.30 and magenta 0xFF26D9 at 120 inside
  radius 0.48, both pulsing on the old 0.90 plus 0.10 sin(age x 0.6)
  curve.
- Darkness (0x0D0018, rate 110) and vapor (0x5E0B99, rate 80) wrap the
  cut from a sphere volume source and drift backward.
- Spatial glass dust (0xE8CCFF, rate 80 per tip) frays off the two
  pinched tips where the shard fans used to sit.
- Lifecycle is unchanged: envelope (age, MAX_LIFE_TICKS, 0.05, 0.30),
  first-person scale-in under age 5, and the linger fade with its 1.0
  plus 0.22 swell. During the linger the plasma envelope is damped to
  45 percent while glass dust and darkness take over, so the tear reads
  as residue closing rather than a shape shrinking.

### DimensionalWarpRift: the rift method

The audit assignment: inward-streaming plasma, darkness smoke, irregular
crack decals, and distortion particles; theme void plasma, spatial
glass dust, darkness vapor. The standard's rift method is followed:

- Inward streaming: a cylindrical source samples the rim (0.45 to 1.0
  of the current radius, full height) and gives every sample a velocity
  toward the tear's heart. Void plasma 0x990DE6 streams at rate 280
  times the radius factor; darkness 0x05000D streams at 190 times the
  radius factor. This is the visual half of the pull the entity's
  VortexPhysics already applies.
- The tear seam: white plasma (rate 120) and magenta plasma 0xFF26D9
  (rate 150) sampled inside the torn volume, whose width and height use
  the exact tearW and tearH shaping from the geometry version
  (entrance ease, origin versus destination expansion, shut shaping
  from progress 0.55), and whose envelope is multiplied by openTear.
- The singularity heart is a white (140) and magenta (120) plasma
  cluster at radius 0.30 and 0.46, on the old 0.88 plus 0.12
  sin(age x 0.5) pulse.
- Boundary: sparse large darkness vapor (0x3D0A66, scale 2.40, rate 90
  times the radius factor) drifts through the torn volume, suggesting
  the film without drawing a shell.
- Ground cracks: spatial glass dust 0xD9B3FF (rate 130 times the radius
  factor) and violet plasma flecks 0x8C1AFF (rate 90 times the radius
  factor) emitted across the ground disk at the same radii the fissure
  work covered. The crack mark itself remains the entity's real
  crying obsidian GroundScar disk, which the geometry version also
  relied on.
- Distortion: unchanged and untouched. It lives in the entity's own
  DbaImpactFx.medium call at the destination burst, the existing
  screen-space post chain. The renderer adds no fake local refraction.

## What did not change

Every damage number, cooldown, Ki cost, timing, duration, hitbox,
movement rule, and synced data field is identical. No entity, item, or
effect file was edited. The server-side particle emissions in the two
entity classes (LayeredParticles trails and bursts) were already
material particles and are untouched. The ease() lifecycle shaping
helper in the rift renderer stays; it is lifecycle math, not geometry.

## Verification

Targeted javac (JDK 25, full sourcepath closure over the two owned
renderers):

- cp262.txt: zero errors naming either owned file; both renderer
  classes and their render state classes were emitted. The closure's
  only errors are the 4 known baseline errors in item/DbaItems.java
  and inventory/DbaMenus.java.
- cp263.txt: zero errors naming either owned file; both classes
  emitted. The closure shows 10 errors, all in the known baseline
  files (DbaItems, DbaMenus, DbaHostileEntity, FriezaSoldierEntity,
  NamekianWarriorEntity, PatrolRobotEntity).
- Em dash scan of both edited files and this report: zero.
- Javac audit only. No Gradle build was run and no in-game look is
  claimed.

## Honest worse check

- The slash no longer has one continuous solid silhouette. At a glance
  it reads as a dense plasma stream along the cut plane rather than a
  drawn crescent. That is the standard's intent, and the three-stop
  color read, the belly bow in the sampling, and the tip glass dust
  keep the identity, but the instant clean-crescent readability of the
  bespoke version is reduced. Not reverted: a solid silhouette is
  exactly what this pass exists to remove.
- The rift's gyroscopic bands and orbiting shards are gone entirely;
  their motion is replaced by the inward stream and the boundary
  vapor. The rift reads less mechanical and more like a feeding tear,
  which fits the warp's own vortex pull, but anyone attached to the
  astrolabe read will notice its absence.
- Frame-rate caveat: because render states are transient, the emitter
  accumulator is stepped deterministically at 1/60 per submit. At
  render rates well above 60 frames per second the per-second emission
  rises proportionally; particle lifetimes and budgets keep the counts
  bounded, but a persistent per-entity emitter handle in the toolkit
  would remove the caveat. Recorded as a change request below.
- Nothing was reverted. No piece reads as broken or empty relative to
  the geometry version; the two identity trades above are stated
  plainly instead.

## Change requests (foundation gaps, not worked around)

1. A ground decal render type: the foundation ships no decal system,
   so the rift's crack presentation is carried by ground-sampled glass
   dust and plasma flecks over the entity's real GroundScar disk
   rather than a literal irregular crack decal texture. If Carter
   wants true decals, the coordinating pass should add one decal
   surface to the foundation and this owner can adopt it in the two
   ground emitters.
2. A persistent per-entity emitter handle in MaterialEmitters (keyed
   by entity id, cleaned up when the entity leaves the client level)
   would let continuous renderer emitters keep a true wall-clock
   accumulator across frames instead of the per-submit deterministic
   step used here.
