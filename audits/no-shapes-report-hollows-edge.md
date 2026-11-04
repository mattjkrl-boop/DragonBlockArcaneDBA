# No-Shapes Realism Pass Report: Hollow's Edge

Base: commit ad22b32 (No-Shapes Phase 1 foundation). Standard: audits/NO-SHAPES-STANDARD.md.
Scope: the five renderers assigned in audits/no-shapes-audit.md (HollowAfterimageRenderer,
HollowChargeRenderer, HollowRushTrailRenderer, VoidRiftRenderer, VoidSlashRenderer).
No other files were edited. The item, all five entities, and HollowAfterimageEntity in
particular are byte-identical to the base (verified by git diff: zero diff lines), so every
constructor, synced data field, damage number, cooldown, cost, timing, and mechanic is
unchanged, including the Dark Faded application HollowAfterimageEntity performs for the
afterimages Katana and Saber also spawn through the same (Level, Player) constructor.
No commit was made; the coordinating pass merges.

## Before and after geometry counts (owned files only, grep verified)

| Surface | Before | After |
| --- | --- | --- |
| BespokeGeometry builder calls | 27 (Afterimage 10, VoidRift 10, VoidSlash 7) | 0 |
| LayeredRender geometry calls | 0 | 0 |
| Direct VertexConsumer.addVertex calls | 40 (HollowCharge 24, HollowRushTrail 16) | 0 |
| KiRenderHelper.vertex calls | 0 | 0 |
| submitCustomGeometry calls | 21 | 0 |
| LayeredRender.distScale references (utility, replaced by MaterialEmitters.distanceScale) | 3 | 0 |

Every renderer now presents its effect only by emitting DbaMaterialOptions material
particles through client/fx/MaterialEmitters. Each renderer holds per-entity Emitter
streams keyed by the entity seed (the UUID most significant bits, the same seed source
the geometry used), ticked once per submit with a wall-clock delta, the lifecycle
envelope, and MaterialEmitters.distanceScale, under the Emitter per-second budgets.
Emission points, paths, planes, and volumes are data only; nothing draws a mesh,
ribbon, shell, or silhouette. KiRenderHelper remains referenced in
HollowAfterimageRenderer only for its red, green, and blue color channel extractors,
which draw nothing.

## Materials per effect

Theme throughout, unchanged from the bespoke pass: soul wisps, void plasma, and
darkness smoke, in the deep void violet, vibrant purple, cyan core, and white edge
colors the geometry used.

- HollowAfterimage: a dissolving humanoid presence emitted inside the body's volume.
  DARKNESS in the caster's skin-tinted body color carries the body mass, SOUL in cyan
  carries the afterglow wisps, SOUL in the synced hair color is concentrated at head
  height as the hair cap, a small PLASMA cluster in near-white cyan sits at the old
  eye slit position on the face, VAPOR bands at the six old scanline heights carry the
  scanline character, PLASMA shed behind the body carries the speed wake, and VAPOR at
  ground level carries the old footstep ripple. The emission volume widens as the
  100 tick fade progresses, so the presence dissolves outward on the old curve.
- HollowCharge: PLASMA sampled through the charge volume with a per-sample velocity
  toward the core point, so the gathering stream visibly converges as charge grows;
  DARKNESS smoke thickens around the caster; SOUL vapor spirals up the two helical
  paths the ribbons traced; a PLASMA core cluster (cyan, with the white heart gated
  to charge 0.5 and up, as the old nested octahedron was) sits at the singularity
  point; DUST in void violet crawls across the ground area the rings anchored.
- HollowRushTrail: PLASMA fills the corridor volume the tube occupied, a cyan PLASMA
  stream runs the center line, VAPOR sheds along the two helical streamline paths,
  and DARKNESS pools at the origin and destination contacts where the puncture rings
  sat. On the third dash, PLASMA sampled across the old arc region forms the cut,
  with a white PLASMA core and VAPOR shed, and no arc mesh under it. The first-person
  narrowing now scales the emission radii, on the same 0.50 factor.
- VoidSlash: dense PLASMA along the arc the bladeArc layers traced (violet body,
  cyan core inside it), a thin white SOUL edge, and dark VAPOR shed behind the cut
  where the wake smears trailed. The synced tilt rotates the emission plane, as it
  rotated the crescent. Open, close, fade, and radius math are unchanged.
- VoidRift: PLASMA streams inward across the billboard mouth plane with a velocity
  toward the core, DARKNESS fills the throat volume, SOUL rides the rim with a
  tangential accretion drift, sparse VAPOR suggests the old back halo, the seven lip
  crystal anchors (the old shard layout math, kept as placement data) shed SOUL as
  they are sucked toward the throat, and a white PLASMA seam burns down the center
  column at peak collapse. The implosion alpha choreography (body dies first, rim
  outlives it, seam last, shards collapse earliest) drives the stream envelopes on
  the same windows, and the inward stream speed rises with the collapse.

## Verification (javac audit only, no Gradle build run or claimed)

Targeted compile of the 5 owned renderers with the sourcepath over the full tree,
JDK 25, run on both Phase 1 classpaths:

- 26.2 (cp262.txt): zero errors in all 5 owned files, classes emitted for all five.
  The only errors in the pulled graph are the 4 pre-existing baseline lines in
  DbaItems.java and DbaMenus.java.
- 26.3 (cp263.txt): zero errors in all 5 owned files, classes emitted for all five.
  The only errors in the pulled graph are the 10 pre-existing baseline lines in
  DbaItems.java, DbaMenus.java, PatrolRobotEntity.java, DbaHostileEntity.java,
  NamekianWarriorEntity.java, and FriezaSoldierEntity.java.

One compile fix was needed during the pass: the per-entity stream lookup variable is
reassigned when a stream set is created, so it is bound into a final local before the
stream-pruning lambda captures it. No behavior change.

Grep proof over the 5 owned files after conversion: BespokeGeometry 0, LayeredRender 0,
addVertex 0, KiRenderHelper.vertex 0, submitCustomGeometry 0. Em dash scan of the 5
files and this report: zero.

## Change requests

None blocking. Two toolkit notes for the coordinating pass, both worked within rather
than around:

- MaterialEmitters sources return position only for the built-in sources, so inward
  streaming (charge gather, rift inward flow) is done with custom Source
  implementations that return a velocity toward the core in the Sample. That is the
  toolkit's own interface used as designed; a built-in inward or shell source would
  save every rift, charge, and portal owner the same few lines if the foundation is
  ever extended.
- Preset color and scale are fixed when an Emitter is created, so per-entity colors
  (the afterimage's skin and hair tints) are captured when its stream set is created.
  That is correct for one entity's lifetime; stream sets are keyed by entity seed and
  pruned after 4 seconds unseen, so a stale set cannot outlive its entity by more
  than that window.

## Honest worse check

The client cannot run in this sandbox, so the final look is judged in game on Carter's
PC. Nothing was reverted, but the trades should be named plainly:

1. Afterimage silhouette: this is the hard case the brief named. The presence is now
   particles inside a body volume, so it reads as a dissolving figure of smoke and
   soul light rather than a solid ghost. The hair cap, eye glow, and scanlines are
   carried by emission placement and color, not by a drawn figure, so all three are
   softer than their geometry versions, and at low effects-density settings the
   figure will read faint. That softness is the standard's price for no drawn
   figure; the alternative was the plume column, which was still a shape.
2. Charge core and ground: the singularity no longer has a crisp rotating core
   object, and the ground containment rings are gone entirely, replaced by dust
   drift, so the anchored, ritual-circle read is weaker. The convergence motion is
   stronger than the geometry version: the shards orbited, these particles actually
   fall inward.
3. Slash and corridor solidity: both are particle volumes now. The slash's leading
   edge is softer than the drawn crescent, and because material particles live
   10 to 22 ticks while the slash lives 40, the cut reads as a dense, continuously
   replenished arc rather than one solid object. The corridor likewise reads as a
   filled, moving volume instead of a tube with walls.
4. Rift lip: the mouth has no hard lip edge anymore. It reads through density
   contrast, darkness in the throat against plasma streaming inward, which is the
   intended material read, but anyone who liked the crisp tear outline will find
   this version softer, especially at long range where distance scaling thins the
   streams.
5. Emission is renderer-driven, so an entity that is not being rendered (off
   screen) emits nothing that frame, exactly as its geometry was not drawn off
   screen. Particles already in flight continue normally.
