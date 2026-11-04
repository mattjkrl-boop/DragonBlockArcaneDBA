# Complex Effects Redo, Phase 2 Report: Power Pole

Owner slot: Power Pole. Theme: air vapor, impact dust, steel sparks.
Files edited, and only these: PowerPoleExtensionRenderer.java,
PowerPoleImpactRenderer.java, PowerPoleWhirlwindRenderer.java, all under
src/client/java/com/dragonblockarcanedba/client/render/.
No commits. No entity, item, mechanic, damage, cooldown, cost, timing, or
synced field changes. The projectile deflection behavior and the d74f13a
destruction wiring are untouched. The pole itself stays a physical model.
All existing material emission stays as the filler layer, unchanged in
colors, rates, and sources, except where a new emitter was added below.

## PowerPoleExtension (class M)

Pieces layer added:
- ComplexGeometry.tendril through ComplexGeometry.submitEnergy with
  ComplexTextures.TENDRIL, seeded from the entity synced seed (seed xor
  0x7E57). One wind strand in local space from the local start offset
  (including the first person hand offset) to the tip at the live
  extension length, control point bowed to one side of the path by 5
  percent of the current length. Width 0.30, root color 0xF7D774 (the
  existing vapor gold) running to tip color 0xFF3829 (the existing hot
  plasma band), peak alpha 0.92, envelope is the existing lifecycle
  alphaMult, so the strand extends, holds, and retracts with the pole.
- A second thinner echo tendril (seed xor 0xE11D), offset 0.10 to the
  side and bowed to the opposite side of the path, width 0.15, root
  0xFF5940 to tip 0x94141C (the existing core and tip plasma bands),
  peak alpha 0.90. Two strands wrapping opposite sides read as air
  coiling around the pole rather than one centered line.
- Vertex estimate: two tendrils at 24 spine samples, about 384 vertices
  each at distScale 1.0, about 768 total, inside the class M budget
  of 2500.

Complex particle layer added:
- SPARK_STREAK through MaterialEmitters.Preset.fixed as a new continuous
  emitter at the existing tip source, rate 28 per second, budget 40 per
  second, spread 0.18. It ticks with the same envelope as the existing
  tip dust and tip sparks.

## PowerPoleImpact (class M)

Pieces layer added:
- ComplexGeometry.crackFan through ComplexGeometry.submitEnergy, seeded
  from the entity synced seed (seed xor 0xC9AC). The builder draws in its
  local XZ plane, so the piece rotates a copy of the submitted pose with
  a quaternion carrying the local Y axis onto the strike direction,
  which puts the fan exactly on the strike plane the filler wash already
  uses. Radius is the impact scale times (0.70 plus 0.60 times the
  existing shock expansion progress), so the web grows with the same
  easing as the filler. Hot color 0xFFF7D6 (near the existing pale vapor
  0xFFF3C4) at the center running to cool color 0xE32636 (the existing
  crimson burst plasma) at the tips, peak alpha 0.95, envelope is the
  existing lifetime fade.
- Vertex estimate: crackFan ships at most about 900 vertices at full
  budget, inside the class M budget of 2500.

Complex particle layer added:
- SHARD_CHIP burst through MaterialEmitters.burst with
  MaterialEmitters.Preset.fixed, added to the existing one shot burst:
  count round(18 times distScale), inside the class M burst budget of
  30, from the same small sphere volume at the strike point as the
  existing spark burst, velocity along the strike direction with
  spread 0.38.

## PowerPoleWhirlwind (class M)

Pieces layer added:
- ComplexGeometry.tendrilFan through ComplexGeometry.submitEnergy with
  ComplexTextures.TENDRIL, seeded from the entity synced seed (seed xor
  0xF4A1). Five strands fanned at the entity cone angle around the cone
  axis (the forward direction), length is range times 0.92 times the
  existing swell factor, width 0.34, root color 0xEFFFFF (the existing
  core vapor) running to tip color 0x26FFA6 (the existing emerald
  vapor), peak alpha 0.92, envelope is the existing build, hold, and
  decay alphaMult. Inside the piece, a copy of the submitted pose is
  rotated around the cone axis at the same spin rate the filler cone
  sources use (age times 65 degrees), so the strands circle with the
  flow instead of hanging static. The rotation uses a Matrix4f form
  because PoseStack.mulPose(Quaternionf) does not exist on 26.3.
- Vertex estimate: five tendrils at 24 spine samples, about 384
  vertices each at distScale 1.0, about 1920 total, inside the class M
  budget of 2500.
- The entity already spawns a DebrisSpawner field server side; that is
  untouched, and these strands are the fast near-field structure the
  audit assigns to this layer.

Complex particle layer added:
- ASH_FLAKE through MaterialEmitters.Preset.fixed as a new continuous
  emitter sampled from the existing cone source (radius factor 0.95,
  the emerald vapor sampling), rate 24 per second, budget 40 per
  second, spread 0.03, ticking with the same envelope as the other
  cone emitters. Airborne grit through the whole cone volume.

## Compliance checks

- Every assigned effect is class M and now has both a pieces layer (at
  least one ComplexGeometry builder) and a complex particle layer
  (SPARK_STREAK, SHARD_CHIP, ASH_FLAKE respectively), plus its existing
  filler. No effect is particles only.
- PLASMA filler is no longer the dominant layer of any of the three:
  the extension body is the twin tendrils, the impact body is the
  crack web on the strike plane, and the whirlwind body is the
  circling strand fan.
- Zero calls to drawColoredBox, drawCylinder, drawDisc, drawSphere, or
  drawHelix in the three files (grep verified). No private geometry,
  particle system, or texture style was built; all pieces go through
  the shared ComplexGeometry submit helpers and all complex sprites
  through MaterialEmitters.
- Energy pieces go through submitEnergy with honest peak alphas (0.90
  to 0.95); faintness is carried by color, never by a low constant
  alpha fed to the shader.
- Sprite budgets: continuous complex emitters run at 28 and 24 per
  second with a 40 per second budget cap, inside the standard cap of
  40 per second per emitter. The one burst is 18 at distScale 1.0,
  inside the class M burst budget of 30.
- Added lines em dash scan: zero. git diff --check on the three files:
  clean.

## Verification

Targeted javac with JDK ~/workspace/tools/jdk-25.0.5+8/bin/javac,
sourcepath src/main/java:src/client/java, compiling only the three
owned files, output to private /tmp tags (powerpole-262, powerpole-263).
Both full logs were read before this claim.

- 26.2 classpath (cp262.txt): 4 errors total, zero in owned files. All
  4 are the pre-existing baseline kinds elsewhere in the closure:
  DbaItems.java CreativeModeTab Output protected access (2) and
  DbaMenus.java MenuType private access (2). Class files for all three
  owned renderers were produced.
- 26.3 classpath (cp263.txt): 10 errors total, zero in owned files.
  The same DbaItems and DbaMenus errors, plus the pre-existing 26.3
  baseline kinds in DbaHostileEntity, FriezaSoldierEntity,
  NamekianWarriorEntity, and PatrolRobotEntity. Class files for all
  three owned renderers were produced.
- One owned error was found and fixed during verification: the first
  whirlwind rotation used PoseStack.mulPose(Quaternionf), which 26.3
  rejects. It was replaced with the Matrix4f rotation form that other
  owners already use on both versions, and both classpaths were
  recompiled after the fix with the results above.

## Foundation change requests

None. Every builder, texture, and sprite type this slot needed was
already shipped in the Phase 1 foundation (tendril, tendrilFan,
crackFan, TENDRIL canvas, SPARK_STREAK, SHARD_CHIP, ASH_FLAKE), and
the pose copy rotation pattern used by the Azure Tornado and Sky
Cracks owners covered the two orientation problems here (strike plane
fan, cone axis spin) without any toolkit change.

## Honest worse check

Extension: low risk of reading worse. The risk is the twin tendrils
reading as a glowing rope alongside the physical pole model if the
tendril canvas dissolves less than expected in game, since the strands
run the full pole length. Mitigation already in the build: widths are
thin (0.30 and 0.15) against lengths up to 30, the builder pinch
windows tear both strands twice along their length, and the colors
are the existing vapor to plasma gradient rather than a new bright
hue. It cannot read worse than the particles only version in
structure terms, because that version had no path structure at all;
the filler stream that carried it is still there underneath.

Impact: one real orientation caveat. The crackFan is planar, and this
build puts that plane on the strike plane, which is correct for the
way the filler wash is already framed and for strikes into walls and
targets. For a strike that lands on open ground while aimed mostly
horizontally, the web stands on the strike plane rather than lying on
the ground, so it reads as a fracture web bursting off the hit point
instead of cracks in the floor. That still reads as impact structure,
and it matches the existing strike plane framing of the whole effect,
but it is not ground cracks in that one case. This is a builder limit
(crackFan is XZ planar by design), not something fixable inside this
slot without a shared toolkit variant, and it is not bad enough to
withhold: the pre-redo and particles only versions had no web at all.

Whirlwind: moderate confidence, one watch item. The five strand fan
rotates at the filler spin rate (age times 65 degrees, about one turn
per 5.5 ticks), which is fast; if it strobes in game it will read as
flicker rather than circling strands. The strands themselves are
correctly placed (cone angle, cone length, swell matched), the filler
cone particles already spin at exactly this rate and read as a
whirlwind, and the tendril pinch and curl keep each strand broken up
rather than a clean sweeping line, so the expectation is that it
reads as fast circling air. If the merge playtest shows strobing, the
one line fix is to halve the geometry spin rate relative to the
filler spin; that is a presentation tweak inside this file, not a
foundation change. The ash flakes are near neutral gray against an
emerald and cyan cone, so they read as grit, which is the intent;
they are sparse (24 per second) and cannot dominate the volume.

Overall: no effect in this slot is shipped in a state believed to
read worse than the particles only version or the pre-redo version.
The impact ground case and the whirlwind spin rate are the two items
to look at first in the merge playtest.
