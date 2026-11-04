# Complex Effects Redo, Phase 2 Report: Ki Techniques

Owner slot: Ki Techniques. Base: HEAD 9178eac (Phase 1 foundation). Files edited: only the seven assigned renderers under src/client/java/com/dragonblockarcanedba/client/render/ki/. No commits. No entity code touched. No synced field, damage number, cooldown, cost, timing, or mechanic changed. All colors come from the same synced technique color (entity getColor and getSeed) the existing emission already used; nothing is hardcoded to one technique. All pre-existing material emission is kept unchanged as the filler layer in every file.

Shared pattern: pieces are drawn in submit() in entity local space (pose origin at the entity origin, the same convention the other Phase 2 owners use), seeded from the synced seed, with peak alphas 0.88 to 0.97. Energy pieces go through ComplexGeometry.submitEnergy with ComplexTextures.TENDRIL or ComplexTextures.FLAME_TONGUE; solid pieces go through ComplexGeometry.submitSolid. Complex sprites are added as extra MaterialEmitters.Emitter channels with MaterialEmitters.Preset.fixed inside the existing per-entity emission sets, so they inherit the existing accumulator, density, and distance envelope handling. Every complex sprite channel runs at 20 to 34 per second with a 40 per second budget cap, inside the standard continuous budget of 40 per second per emitter. A tendril at distScale 1.0 emits 384 vertices (24 spine samples, crossed ribbon pairs).

## Per effect

### KiBarrierRenderer (class L)

- Pieces: six vertical ComplexGeometry.tendril lashes standing on the barrier boundary, one builder at its full segment and count budget (24 samples, 6 strands, 2304 vertices of the 6000 class L budget). Each lash runs from the lower boundary through a control point bowed out to 1.12 times the radius at the equator to the upper boundary, turns at the standing rim rate (spin times 1.7, the same rate the filler vertical rim uses), and follows the filler frame radius math exactly (breathe plus used percent growth). Root color is the synced technique color, tip color is the existing glow tint of that color. Submit: submitEnergy with the tendril canvas.
- Complex particles: WISP_CURL, continuous in the barrier volume (moteVolume source), 24 per second, budget 40.
- Filler: unchanged (plasma motes, boundary vapor and plasma, flat and vertical rim plasma).
- Ripple block, restated exactly as the audit records it: KiBarrier hit ripples remain blocked on the entity having no synced hit field; that is a data change, out of this pass, and stays Carter's call exactly as the no-shapes merge recorded it. No synced field or entity change was added in this pass; the barrier work here is the boundary lashes and the volume wisps only.

### KiBeamRenderer (class L)

- Pieces: two builder uses in one submitEnergy call with the tendril canvas. A chained tendril core along the beam: two strands (a white to technique main strand and a thinner technique to white offset strand), segmented 2 to 4 by beam length so the bezier curl stays tight over long beams, plus a tendrilFan of six impact lashes at the terminus (56 degree spread). Vertex count at distScale 1.0: at most 4 segments times 2 strands times 384 (3072) plus the fan at 6 times 384 (2304), for 5376 of 6000.
- Complex particles: SPARK_STREAK at the terminus, continuous, 30 per second, budget 40, born moving along the beam direction.
- Filler: unchanged (path plasma, white root plasma, vapor, impact plasma and smoke).

### KiBlastRenderer (class M)

- Pieces: a tendril wake behind the bolt: a main tendril from the tail (same 1.6 times scale tail length the filler trail uses) to a white tip at the nose, plus a thinner offset tendril as its torn edge. Two tendrils, 768 vertices of the 2500 class M budget. Submit: submitEnergy with the tendril canvas, root color the synced technique color.
- Complex particles: SPARK_STREAK shed at the head cluster, continuous, 20 per second, budget 40. Note: the audit row says SPARK_STREAK at impact, but the blast render state carries no synced impact field and this pass may not add one, so the streaks stream at the nose in flight instead of bursting at a client side impact moment. The entity's own server burst remains the impact event, unchanged.
- Filler: unchanged (plasma cluster, white heart, plasma and vapor trail).

### KiDiskRenderer (class M)

- Pieces: three short tendril lashes anchored at the rim, spinning at the old 24 degrees per tick (0.4189 radians per tick on the age), trailing tangentially and slightly outward as the cutting wake. Three tendrils, 1152 vertices of 2500. Submit: submitEnergy with the tendril canvas, technique color to a white tip.
- Complex particles: SPARK_STREAK shed at the rim source, continuous, 26 per second, budget 40.
- Filler: unchanged (disc plane plasma, white rim plasma, vapor, rim ember and plasma).

### KiExplosionRenderer (class L)

- Pieces: three builder types. fracturePlates at 0.90 of the eased blast radius through submitSolid, hot face the glow tint of the synced technique color and rim the same color darkened to 30 percent. crackFan at 0.95 of the eased radius through submitEnergy, white hot center to the technique color at the tips. flameTongues, seven short tongues at 0.55 of the eased radius and 0.70 of it tall, through submitEnergy with the flame tongue canvas, glow tint base to technique color tip, sway phase from age. Vertex estimate at distScale 1.0: plates about 480, cracks about 700, tongues 336, for about 1500 of 6000. Piece sizes follow the eased radius and the fade envelope, so the structure opens and dissolves with the existing 8 tick open and 36 tick squared fade lifecycle.
- Complex particles: SHARD_CHIP blown through the blast volume source, continuous, 34 per second, budget 40. It was inserted before the dust ring emitter so the ring emitter stays last and keeps its ring envelope in the tick loop.
- Filler: unchanged (volume plasma, white core plasma, boundary vapor, smoke, ground ring dust). The crater stays the d74f13a entity destruction code; nothing here touches it.

### KiLaserRenderer (class M)

- Pieces: a thin chained tendril core along the laser path, 2 to 3 segments by length, width 0.17 times scale, technique color to a white tip. At most 1152 vertices of 2500. Submit: submitEnergy with the tendril canvas.
- Complex particles: SPARK_STREAK at the terminus, continuous, 24 per second, budget 40.
- Filler: unchanged (path plasma, white root plasma, vapor, terminus plasma).

### KiSpiralBeamRenderer (class L)

- Pieces: two builder uses in one submitEnergy call with the tendril canvas. The spiral body is two tendrils with opposed helical controls: chained segments (3 to 4 by length) whose start, control, and end points all ride the same winding the filler helix source uses (3.5 turns over the length, phase 0.35 per tick of age, radius 0.28 times scale), one strand technique color to white and the opposed strand white to technique color, so the pair reads as the two intertwined arms of the spiral rather than a rendered helix primitive. Plus a tendrilFan of six impact lashes at the terminus. Vertex count at distScale 1.0: at most 4 segments times 2 strands times 384 (3072) plus 2304, for 5376 of 6000.
- Complex particles: SPARK_STREAK at the terminus, continuous, 30 per second, budget 40.
- Filler: unchanged (white and technique path plasma, helix plasma and vapor, terminus plasma and smoke).

## Budget and gate checks

- Vertex budgets: every effect is inside its class budget at distScale 1.0 (M cap 2500: blast 768, disk 1152, laser at most 1152. L cap 6000: barrier 2304, beam at most 5376, spiral at most 5376, explosion about 1500). All geometry scales down through the builders' own distScale handling.
- Sprite budgets: no bursts are used in this slot; every complex sprite channel is continuous at 34 per second or less against the 40 per second per emitter cap.
- Plain primitives: zero calls to drawColoredBox, drawCylinder, drawDisc, drawSphere, or drawHelix in the seven owned files (grep verified; the only matches in the ki package are the method definitions inside KiRenderHelper.java itself, a shared foundation file this slot did not touch, with zero call sites added).
- PLASMA is never the sole or dominant layer in any class M or L effect here: every effect now leads with its pieces layer and carries a complex sprite layer, with the plasma emission kept as filler at its previous rates.
- git diff --check on the seven files: clean. Added lines scanned for em dashes and en dashes: zero.
- Diff scope: exactly the seven assigned files changed, 401 insertions and 42 deletions, all of the deletions being the replaced class javadoc headers.

## Verification

Targeted javac with JDK at ~/workspace/tools/jdk-25.0.5+8/bin/javac, sourcepath src/main/java:src/client/java, compiling the seven owned files, private output dirs /tmp/ki-techniques-out262 and /tmp/ki-techniques-out263, logs at /tmp/ki-techniques-javac262.log and /tmp/ki-techniques-javac263.log. Both full logs were read.

- 26.2 classpath (cp262.txt): 4 errors total, all baseline and outside this slot: DbaItems.java line 535 (CreativeModeTab Output protected access, twice) and DbaMenus.java line 16 (MenuType constructor and MenuSupplier private access, twice). Zero errors in any owned file; zero log lines mention any file under render/ki. All seven renderers and their inner classes produced class files in the output dir.
- 26.3 classpath (cp263.txt): 10 errors total, all baseline and outside this slot: the same DbaItems and DbaMenus errors, plus DbaHostileEntity.java line 79 (swinging and attackAnim symbols), FriezaSoldierEntity.java line 51 (swing signature), NamekianWarriorEntity.java line 33 (hurtMarked), and PatrolRobotEntity.java line 181 (swinging and attackAnim). Zero errors in any owned file; all seven renderers produced class files in the output dir.
- No new error kinds were introduced by this slot on either classpath. The javac exit code of 1 on both runs comes entirely from the baseline errors above.

## Foundation change requests

None. The shipped toolkit covered every assigned row without a private variant: tendril, tendrilFan, fracturePlates, crackFan, and flameTongues builders, submitEnergy and submitSolid, the tendril and flame tongue canvases, and Preset.fixed sprite emission were sufficient. No shared foundation file was edited.

One data limitation recorded, not a foundation gap: KiBlast and KiBarrier both lack a synced event field (impact moment for the blast, hit moment for the barrier). The blast streaks therefore stream continuously at the nose, and the barrier ripple stays blocked as recorded above. Both would need a synced field, which is an entity change outside this pass and stays Carter's call.

## Honest worse check

- KiBarrier: the lashes are the riskiest read in this slot. Six strands on a small sphere (radius about 1.3) could read as a cage or a sea creature if they sit too proud of the boundary film. They are kept thin (0.16 width) and bowed only 12 percent past the radius so they hug the surface the filler boundary sprites already mark, and their tips fade through the shader life remap. I judge this better than the particles only version, which had no structure at all on the boundary, but it is the one effect here I would most want Carter's eyes on in game.
- KiBeam and KiSpiralBeam: the tendril core is deliberately thinner than the filler plasma cloud around it, so at long range the beam still reads as a beam and the core adds structure up close. The risk is the opposite: at far distance the core may be nearly invisible inside the plasma. That is acceptable; nothing was traded away, the filler is untouched, and the terminus fans and streaks still land at range.
- KiBlast: two wake strands behind a small fast projectile could read as tails or antennae if the pulse makes the cluster much smaller than the wake. The wake length follows the same tail length the filler trail already used, and its width scales with the bolt scale, so the proportions match the pre-existing trail read. I do not think it reads worse.
- KiDisk: the rim lashes extend the tangential shed the rim emitters already drew, now with a visible spinning structure. The anchor spin uses the same 24 degrees per tick the filler was born with, so lashes and shed streaks wheel together. No worse risk beyond normal tuning.
- KiExplosion: plates and cracks lie in the plane at the blast center height, the same plane the filler ground ring already used. For a mid air blast that plane floats, but it floated for the ring before this pass too, so the pieces do not introduce a new wrongness; over the real crater the destruction pass leaves, the plates read as lifted ground as intended.
- KiLaser: a 0.17 width strand is close to the thinnest readable tendril. If it reads as a scratch line on some technique colors, the fix inside the standard is a small width raise, not a structural change. The white tip gradient keeps it from reading as a flat line.

No effect in this slot reads worse than its particles only version in a way I could identify from the code, and none was shipped silently against a known regression. The barrier lash density is the single judgment call flagged above for in game review.
