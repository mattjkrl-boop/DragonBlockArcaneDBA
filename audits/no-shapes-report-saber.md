# No-Shapes Realism Pass Report: Saber

Base commit: ad22b32 (Phase 1 foundation). Worked, not committed, per the pass rules.
Owned files converted: the four renderers assigned in audits/no-shapes-audit.md
(client/render/SaberSlashRenderer.java, SaberVoidTearRenderer.java,
SaberDodgeSparkRenderer.java, SaberDimensionalLineSlashRenderer.java).
Nothing else was edited by this owner. SaberItem.java and all four Saber
entities are byte-identical to the base commit, so the Blitz escalation,
the finisher splits and timing, every damage number, Ki cost, cooldown,
and synced data field are untouched by construction. HollowAfterimageEntity
and its renderer were not touched by this owner; they belong to Hollow's Edge.

## Before and after geometry counts

Counts follow the audit method: B is BespokeGeometry builder call sites,
L is LayeredRender geometry calls, V is direct vertex writing.

| Effect | B before | L before | V before | B after | L after | V after |
| --- | --- | --- | --- | --- | --- | --- |
| SaberSlash | 10 | 0 | 0 | 0 | 0 | 0 |
| SaberVoidTear | 9 | 0 | 0 | 0 | 0 | 0 |
| SaberDodgeSpark | 9 | 0 | 0 | 0 | 0 | 0 |
| SaberDimensionalLineSlash | 7 | 0 | 0 | 0 | 0 | 0 |
| Owner total | 35 | 0 | 0 | 0 | 0 | 0 |

Also to zero in all four files: BespokeGeometry.Rng references (1 in
DodgeSpark before), KiRenderHelper references, LayeredRender references
including distScale, submitCustomGeometry calls, and PoseStack geometry
transforms. Distance scaling now comes from MaterialEmitters.distanceScale.
A grep over the four files for BespokeGeometry, LayeredRender, KiRenderHelper,
addVertex, and submitCustomGeometry returns no matches.

## Method

Each renderer now only emits particles through client/fx/MaterialEmitters.
Render states in this version are recreated on every extraction, so each
renderer keeps its Emitter set in a small per-instance map keyed by the
effect's existing synced seed (entity id hash for Slash, VoidTear, and
LineSlash; the synced UUID for DodgeSpark). The map entry refreshes its
origin, orientation, and animated scale fields from the current render
state on every submit, computes the emission delta from the state age in
game ticks, and ticks every emitter with the lifecycle envelope multiplied
by the distance scale. Entries are removed when the effect completes and
pruned after five seconds without a submit. Every emitter carries an
explicit per-second rate and budget from the toolkit; no private particle
system, texture, or geometry helper was written. Sampling paths, volumes,
and rib stations are data only, per the standard.

## Materials per effect

Core materials for the Saber are dimensional void plasma and darkness,
with spatial glass dust, per the audit rows.

- SaberSlash: cyan void plasma (0x4DD8FF) and a white plasma core sampled
  along the slash path as it opens; spatial glass dust (0xBFEFFF) at the
  two tips and the strike center; darkness vapor (0x3D1A78) around the
  cut. The old strike window (progress 0.22 to 0.55) fires one plasma and
  glass burst at the crescent center on its first frame.
- SaberVoidTear: darkness vapor (0x260052) filling the tear volume and
  drifting inward; cyan void plasma streaming inward along the slit; a
  white plasma core on the same slit path; spatial glass dust thrown
  outward radially where the spikes used to read. The strike pulse fires
  one glass and plasma burst at the center. The destination tear keeps
  its 1.30 width boost through the slit sampling width.
- SaberDodgeSpark: gold plasma (0xFFD75E) and white plasma throughout the
  burst volume; spatial glass dust (0xFFF3B0) on the same volume; a thin
  darkness vapor (0x3D1A78) rim at low rate. The opening frame fires one
  gold plasma and glass burst for the clash contact, and the continuous
  emitters ride the old build-in, growth, and quadratic fade curves.
- SaberDimensionalLineSlash: cyan void plasma along the full cut path
  with a tighter white plasma core on the same path; spatial glass dust
  biased to the five old rib stations (0.12, 0.31, 0.50, 0.69, 0.88 of
  the cut) plus the strike burst; darkness vapor hugging the path and
  drifting inward. Emission rate scales with the synced line length
  (320 to 720 plasma per second before envelope and density scaling).
  The strike window (progress 0.32 to 0.62) fires one white plasma and
  glass burst at the snap center.

## Numbers and mechanics preserved

No item, entity, or shared file was edited, so nothing about damage,
cooldowns, costs, timing, hitboxes, or synced data can have changed.
Renderer-side lifecycle math (progress, open, envelope, fade, strike
windows, growth curves, destination boost) was carried over line for line
from the bespoke versions; only the drawing calls were replaced.

## Verification

Targeted javac of the four owned renderers with -sourcepath over both
source sets, on both audit classpaths
(~/workspace/dba-audit-libs/cp262.txt and cp263.txt), JDK 25:

- 26.2: zero error lines in any Saber file; class files emitted for all
  four renderers and their emitter classes. Residual closure errors are
  only the known baseline kinds in DbaItems and DbaMenus.
- 26.3: zero error lines in any Saber file; class files emitted for all
  four renderers. Residual closure errors are only the known baseline
  kinds in DbaItems, DbaMenus, DbaHostileEntity, FriezaSoldierEntity,
  NamekianWarriorEntity, and PatrolRobotEntity.

Em dash scan of the four converted files: zero. Javac audit only. Gradle
was not run and no in-game look is claimed.

## Honest worse check

Nothing was reverted, but four trades are real:

1. Material particles live on their own profiles, not on the host
   effect's lifetime. Darkness vapor (30 to 62 tick profile) and glass
   dust can linger briefly after a 10 to 20 tick Saber effect ends, so
   the Saber now leaves a faint void haze tail that the geometry
   versions did not. Rates are kept low, but the tail exists.
2. The SaberSlash cut is now a sampled particle path. At the lowest
   effects density settings it can read as separated motes rather than
   one continuous cut. That is the standard's intended trade, but
   continuity now depends on particle density in a way the mesh did not.
3. SaberDodgeSpark lost its planar crossed clash read. The volume burst
   reads equally from every angle, but the sharp directional spike
   silhouette is gone.
4. The VoidTear destination core used to burn at full alpha against the
   origin tear's 0.8. That alpha difference is not reproduced; the
   destination tear now reads wider and denser instead of brighter.

## Change requests for the coordinating pass

One optional foundation gap, not a blocker: DbaMaterialOptions carries
no lifetime scale, so a short-lived host cannot shorten a material
profile's linger (see worse check item 1). If several owners hit the
same tail, a lifetime multiplier on DbaMaterialOptions, honored by
DbaMaterialParticle, would be the shared fix. No private workaround was
written for it.
