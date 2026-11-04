# No-Shapes Realism Pass Report: Katana

Scope: the four Katana effect renderers assigned in audits/no-shapes-audit.md: client/render/KatanaAimGuideRenderer.java, client/render/KatanaChargeRenderer.java, client/render/HeavenSplitterRenderer.java, client/render/SwiftCrescentRenderer.java. Base commit ad22b32. No item, entity, or shared file was edited. HollowAfterimageEntity and its renderer belong to Hollow's Edge and were not touched, even though the Katana spawns them. No damage number, cooldown, Ki cost, timing, duration, hitbox, mechanic, or synced data field changed anywhere; the conversion is presentation only, inside the four renderers.

## Before and after geometry counts

Counts follow the audit method: B is BespokeGeometry builder calls, L is LayeredRender geometry calls (drawVortexBands, drawAnchorGlow), V is direct vertex writing.

| Effect | B before | B after | L before | L after | V before | V after |
| --- | --- | --- | --- | --- | --- | --- |
| KatanaAimGuide | 13 | 0 | 0 | 0 | 0 | 0 |
| KatanaCharge | 10 | 0 | 1 | 0 | 0 | 0 |
| HeavenSplitter | 7 | 0 | 1 | 0 | 0 | 0 |
| SwiftCrescent | 6 | 0 | 0 | 0 | 0 | 0 |
| Owner total | 36 | 0 | 2 | 0 | 0 | 0 |

Grep verification after conversion, across the four files: zero BespokeGeometry references of any kind (builders and Rng), zero LayeredRender references, zero KiRenderHelper references, zero submitCustomGeometry calls, and zero addVertex calls. The renderers no longer draw anything; their submit methods only compute emission data and tick shared emitters, then call the empty super submit.

## Conversion method

Each renderer keeps a small per-entity emission anchor keyed by the entity UUID. The anchor holds MaterialEmitters.Emitter instances whose sources are lambdas that read the anchor's current position, orientation basis, scale, charge, and lifecycle fields on every sample, so the streams follow the entity as it moves or turns without rebuilding the emitters. Emission uses the toolkit wall-clock delta form of Emitter.tick(deltaSeconds, envelope), with the old lifecycle envelope and MaterialEmitters.distanceScale multiplied in as the envelope, so rates are frame-rate independent, distance-scaled, density-configured, and hard-budgeted per second per stream. Strike moments that used to be mesh flashes are one-shot MaterialEmitters.burst calls gated on the render age crossing the strike tick, once per entity. Anchors for entities that stop rendering are pruned after four seconds.

Orientation is carried as data, not as a rendered transform: each renderer derives its basis vectors from the synced rotations (yaw, pitch, and tilt where the old mesh used them) and samples particle positions in world space from those vectors.

## Materials used per effect

Theme throughout: ki plasma, air vapor, steel sparks, in the bespoke pass colors.

- SwiftCrescent (was bladeArc crescent, tornRibbon wake, shardFan belly spark): PLASMA in silver 0xDBF2FF as the main cut stream, PLASMA white as the core stream, PLASMA in the variant lip color (0x00DBFF cyan for variant 0, 0x8CCCFF silver-blue for variant 1), VAPOR in silver-cyan on the wake path behind the cut, and fixed DbaParticles.SPARK as steel sparks at the belly while the strike window is open. At the strike tick, white plasma, lip plasma, and SPARK bursts leave from the cut center. The crescent silhouette is deliberately not reproduced; the streams fill an oriented cut volume and trail backward instead.
- HeavenSplitter (was tornRibbon wall, bladeArc cross cleaves and shock rings, branch fracture ribs, drawAnchorGlow): PLASMA in mantle cyan 0x00BFFF, main cyan 0x00D9FF, and white, sampled through the whole cut volume between the path start and end up to the rising wall height; VAPOR in pale 0xBFEFFF through the same volume and as a low ground stream under the midpoint where the anchor glow sat; fixed SPARK sprayed from the cut volume while the strike window is open. The cross cleaves, ribs, and rings are not reproduced as forms; the strike burst at the start, midpoint, and end points carries that beat instead.
- KatanaCharge (was brokenVortexBand focus rings and halo, bladeArc ground accent, shardFan hilt sparks and motes, drawVortexBands stance bands): PLASMA in pale cyan 0x9FE8FF sampled through the stance volume with sample velocities pulled toward the sheathed hilt, PLASMA white at the hilt point, VAPOR in silver-cyan moving around the rim and as a ground stream at the feet, and fixed SPARK at the hilt. The old lifecycle alpha (build over four ticks, fade over FADE_TICKS) now scales emission density, and the charge ratio still tightens and strengthens the gathering through the source radii and the envelope.
- KatanaAimGuide (was tornRibbon beam sheets and flow bands, bladeArc reticle rings, shardFan brackets, crosses, and nodes): PLASMA in cyan 0x00D9FF sampled along the aim path with a small radial spread, PLASMA white in a tighter stream inside it, VAPOR in pale 0xBFEFFF around the path, PLASMA in terminal cyan 0x00F2FF gathered in a volume at the endpoint, and PLASMA in pale 0xD9FAFF gathered at the old six-block calibration distances. The first-person owner offset, hand side, zStart, and synced max range behavior are unchanged; they now move the emission origin instead of a mesh. The guide carries no steel sparks of its own; in this weapon the SPARK material sits at the Charge hilt, the SwiftCrescent belly, and the HeavenSplitter cut face, where steel contact actually happens.

## Verification

Targeted javac (JDK 25.0.5+8) of the four renderer files with -sourcepath src/main/java:src/client/java, once per audit classpath:

- 26.2 (cp262.txt): zero errors in any of the four Katana renderer files; all four renderer classes were emitted. The only errors in the run are the pre-existing baseline errors in item/DbaItems.java (CreativeModeTab Output access, two lines) and inventory/DbaMenus.java (MenuType access, two lines), pulled in only as sourcepath dependencies and unmodified by this pass.
- 26.3 (cp263.txt): zero errors in any of the four Katana renderer files; all four renderer classes were emitted. The only errors in the run are the same DbaItems and DbaMenus baseline errors plus the known 26.3 baseline kinds in entity/DbaHostileEntity.java, entity/FriezaSoldierEntity.java, entity/NamekianWarriorEntity.java, and entity/PatrolRobotEntity.java, none of them Katana files and none referencing a converted symbol.
- git diff --check on the four files is clean, and an em dash scan of the four files and this report is clean.
- This is a javac audit only. No Gradle build was run and no build pass is claimed. The in-game look is unverified from this environment, as with every prior pass.

## Shared change requests

None required for the conversion; the Phase 1 toolkit covered every Katana surface. One ergonomic note for the coordinating pass, not a gap: Emitter binds its preset and source at construction, so each renderer carries a small per-UUID anchor cache with dynamic source lambdas to keep streams attached to moving entities. If later owners repeat this pattern heavily, a shared per-entity emitter registry in the toolkit would remove the repeated boilerplate. No private particle system, texture style, or geometry helper was created here; the anchors only hold shared Emitter instances and sample data.

## Honest worse check

- Material lifetime tail: the material particles live on their own clocks (plasma about 10 to 22 ticks, vapor about 24 to 54 ticks), while SwiftCrescent's entity life is 10 ticks and KatanaCharge fades over 6 ticks. Converted effects therefore dissolve as lingering material after the entity ends instead of vanishing on the exact envelope tick the meshes used. Emission density still follows the old envelope, so nothing new spawns late, but a faint plasma or vapor tail outliving the effect is a real read change, most visible on SwiftCrescent and on a cancelled charge.
- SwiftCrescent readability: the cut no longer has a crescent silhouette at all. It reads as a concentrated, dissolving plasma cut with a vapor wake, which is the mandated direction, but the exact line of the cut is less legible than the bespoke arc was, especially at long range where distance scaling thins the streams.
- HeavenSplitter presence: the mesh wall read as a towering solid cut at any density setting. The particle volume reads softer, and at low effect density or far distance it is visibly thinner than the wall it replaced. The strike bursts at the three path points partially compensate at the moment the damage lands.
- Aim guide precision: the old reticles gave exact station and terminal marks. The material guide concentrates plasma at those points but offers no crisp reticle, so judging the exact dash endpoint is less precise than with the rings. The synced range and the actual dash are unchanged; only the readout is softer.
- Nothing was reverted: every trade above is the direct consequence of removing the visible forms, which is the pass rule, and no conversion made a mechanic, timing, damage value, or cost worse. The four entity-side particle emitters in the Katana entities (server-side DbaParticles and LayeredParticles emission) were already material-based and are untouched.
