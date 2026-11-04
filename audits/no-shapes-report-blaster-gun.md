# No-Shapes Realism Pass Report: Blaster Gun

Scope: the three renderers named in audits/no-shapes-audit.md for this
owner: client/render/BlasterBoltRenderer.java,
client/render/ErasureCannonBeamRenderer.java, and
client/render/ErasureChargeOrbRenderer.java. Base: commit ad22b32
(Phase 1 foundation). No commit made by this agent. Only these three
files were edited; the item, the ammo item, and the three entities are
unmodified, so every damage number, cooldown, Ki cost, timing,
hitbox, heat rule, and synced data field is byte-identical. The
entities' own server-side particles and sounds are untouched.

## Before and after geometry counts

Counts are BespokeGeometry builder calls in the owned renderers, per
the audit's B column, plus the related geometry surfaces.

| Category | Before | After |
| --- | --- | --- |
| BespokeGeometry calls, BlasterBoltRenderer | 7 | 0 |
| BespokeGeometry calls, ErasureCannonBeamRenderer | 8 | 0 |
| BespokeGeometry calls, ErasureChargeOrbRenderer | 7 | 0 |
| BespokeGeometry calls, owner total | 22 | 0 |
| LayeredRender geometry calls | 0 | 0 |
| Direct addVertex calls | 0 | 0 |
| KiRenderHelper references (kiRenderType) | 3 | 0 |
| submitCustomGeometry call sites | 12 | 0 |
| Local geometry helper methods | 7 | 0 |

Deleted helpers: bolt ribbon and axisFan; beam ribbonPlane and
beamPlaneFan; orb domeShell, shardCluster, and ringBand. All three
renderers now draw nothing in submit. The PoseStack and
SubmitNodeCollector parameters are unused beyond the super call.

## Method

Each renderer captures its world frame in extractRenderState (origin
from the entity's interpolated position, direction from its synced
rotations, and a right and up basis for the beam and orb so the old
first-person muzzle offsets land in the same place). In submit, the
renderer feeds that frame to persistent per-entity
MaterialEmitters.Emitter channels keyed by entity id. The Emitter
accumulator and per-second budgets therefore survive across frames;
channels rebuild only when a quantized signature changes (heat color
and radius bucket for the bolt, charge and size buckets for the beam,
charge bucket for the orb). Sources are toolkit sources (path,
sphereVolume) wrapped in lambdas that read the live frame, so a
channel follows its entity without rebuilding. Emission deltas come
from the entity's synced age, so rates are frame-rate independent.
Distance scaling comes from MaterialEmitters.distanceScale, density
from the effects config inside the toolkit, and the synced entity
seed feeds the DbaMaterialOptions seed variation of every preset.

## Materials per effect

BlasterBolt (theme: ki plasma, steel sparks, smoke, vapor):
- Body: PLASMA in the bolt's heat palette sampled along the real
  flight segment from tail to head (260 per second, 340 overcharged).
- Core: PLASMA in near-white along the middle of the segment.
- Nose: PLASMA bunched in a small sphere volume at the head, plus
  fixed SPARK particles shed from the same volume.
- Tail wake: VAPOR in the heat palette along the rear segment.
- The impact collapse math is unchanged and now drives the emission
  envelope and particle scale instead of mesh size.

ErasureCannonBeam (theme: ki plasma, steel sparks, smoke, vapor):
- Body: three PLASMA runs along MaterialEmitters.path from muzzle to
  terminus: a white core (430 per second), a cyan body run (350), and
  a darker, larger, sparser outer run (190), plus a VAPOR run (95).
- Focusing stations: loose PLASMA clusters in sphere volumes at the
  old 0.25, 0.50, and 0.75 ring positions, tightening with distance
  from the muzzle as the rings did; far views drop the last station,
  as before.
- Muzzle: a PLASMA cluster at the barrel point, including the
  first-person hand offset.
- Terminus plume: PLASMA in cyan and white, SMOKE, VAPOR, and fixed
  SPARK particles in a sphere volume at the hit point, scaled by the
  old impact pulse. The beam's FxEnvelope drives emission density so
  it builds and dissipates with the beam's 24 tick life.

ErasureChargeOrb (theme: ki plasma, steel sparks, smoke, vapor):
- Core: white PLASMA churning in a sphere volume at 0.85 of the orb
  radius, rate rising with charge (210 to 550 per second).
- Body: cyan PLASMA at 1.05 of the radius. Past 0.75 charge a
  red-white hot PLASMA run joins, carrying the old shell color
  journey.
- Boundary: sparse large VAPOR sprites at 1.28 of the radius suggest
  the containment surface; no shell is drawn.
- Intake: PLASMA motes spawn at the old intake radius (2.3 of the
  orb radius, tightening to 1.35 with charge ease), jittered by up to
  30 percent so no clean shell forms, with sample velocities aimed
  at the center; the pull strengthens with charge. This replaces the
  displaced dome shells, the shard cage, the gyroscope bands, and
  the intake branch spikes with one gathering stream.
- Shedding: fixed EMBER particles boil off the body volume (35 per
  second, 130 once charge passes 0.75) and fixed SPARK particles
  escape at every charge level.
- Position, pulse, radius growth (0.22 plus charge ease times 0.95),
  and the first-person scale and muzzle offset are unchanged.

## Verification

Targeted javac (JDK 25, sourcepath over both source sets) of the
three owned renderers against both audit classpaths:
- cp262.txt (26.2): zero errors naming any owned file; class files
  emitted for all three renderers and their state and emission
  classes. The run's only 4 error lines are the known baseline kinds
  in DbaItems (CreativeModeTab Output protected access) and DbaMenus
  (MenuType private access).
- cp263.txt (26.3): zero errors naming any owned file; class files
  emitted for all three renderers. The run's only 10 error lines are
  the known baseline kinds: the same DbaItems and DbaMenus lines,
  plus the 26.3 API-removal kinds in DbaHostileEntity,
  FriezaSoldierEntity, NamekianWarriorEntity, and PatrolRobotEntity.
  None of those files were touched by this agent.
Grep verification on the owned files after conversion:
BespokeGeometry 0, LayeredRender 0, addVertex 0, KiRenderHelper 0,
submitCustomGeometry 0. Scoped git diff --check is clean. Em dash
scan of added lines in the owned files and this report: 0.
Javac audit only; no Gradle build is claimed.

## Honest worse check

- Beam solidity: the beam is now particle density along a path, not
  a continuous surface. At the lowest effects density setting or at
  long distance it can read as a stream of bright motes rather than
  a solid energy column. That is the standard's accepted trade for
  beams, and rates were set high (about 1,900 per second across all
  beam channels at full envelope) to keep the column dense, but it
  is the piece most likely to be judged in game.
- Bolt identity: the stabilizer fins are gone entirely. The bolt
  reads as a pure plasma slug with sparks and a vapor wake, which is
  what the audit ordered (dense plasma particle cluster), but the
  manufactured projectile character the fins gave it is a real loss.
- Orb at low charge: with no cage or rings, the early charge read
  rests on the intake streams and a faint core (the envelope starts
  at 0.25). The first fraction of a charge is quieter than the old
  orb. Density and scale take over as charge rises, and nothing was
  reverted, but the low-charge read is the orb's weakest moment.
- Orb containment: the sphere volumes and intake shell are emission
  data, not rendered surfaces, per the standard. If the particle
  counts read sparse in game, the cluster could feel less contained
  than the old cage. The vapor boundary run is the mitigation.
- In-game look is unverified by construction, as with the whole
  pass: no Gradle build and no client run in this environment, and
  Carter's PC test is the first real look.

## Shared change requests

None blocking. One toolkit note for the coordinating pass:
MaterialEmitters.path and sphereVolume sample from fixed points
captured at construction, so a persistent Emitter that must follow
a moving entity needs its source wrapped in a lambda that rebuilds
the toolkit source from a live frame on each sample, as done here.
A Source variant that reads a supplier would remove that boilerplate
from every owner with a moving effect.
