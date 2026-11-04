# Complex Effects Report: Evil Spear

Owner slot: Evil Spear. Base HEAD 9178eac. Files edited: only the three
owned renderers (EvilSpearChargeRenderer, EvilSpearProjectileRenderer,
HellHuntImpactRenderer). No commits. The Evil Spear Dread slot
(DreadGazeRenderer, EvilDreadEffect, DreadGazeEntity, MeltingOverlayMixin)
was not touched, and no entity code was touched. Themes, colors, damage,
cooldowns, costs, timing, mechanics, and synced fields are unchanged from
HEAD. Existing material emission is untouched and remains as the filler
layer in every effect; in all three effects the plasma emission is now
one filler among pieces and complex sprites, never the sole or dominant
layer. No plain primitive calls were added (zero drawColoredBox,
drawCylinder, drawDisc, drawSphere, drawHelix in the owned files).

## Per effect

### EvilSpearChargeRenderer (class M)

- Pieces layer, two builders:
  (1) Darkness strands inward: five one-count ComplexGeometry.tendrilFan
  calls through ComplexGeometry.submitVeil with ComplexTextures.TENDRIL,
  one per strand, rooted on the gathering ring (0.95 of the existing
  charge radius, heights 0.30 to 1.40, slowly rotating with age) and
  aimed at the core at spear height (local y 0.88, the same point the
  existing core emitter samples). Root color 0x26030B (the existing
  darkness color), tip 0xF20D33 (the existing crimson plasma color),
  peak alpha 0.92, envelope is the existing lifecycle (spawn smoothstep
  times release fade). Strand width grows with charge, 0.20 to 0.30.
  (2) ComplexGeometry.flameTongues through ComplexGeometry.submitEnergy
  with ComplexTextures.FLAME_TONGUE: four small tongues at the spear
  tip (base at the core point), radius 0.30 to 0.44 and height 0.55 to
  1.05, both growing with the synced charge ratio, hot base 0xFFF3D6 to
  crimson tip 0xF20D33, peak alpha 0.95, sway phase from age.
  Vertex estimate at distScale 1.0: five strands at reduced spine
  scale, about 256 each (1280), plus tongues about 192, total about
  1472, inside the M budget of 2500. Flame tongues stay inside their
  own cap (4 tongues of 6 segments, cap is 9 of 6).
- Complex particle layer: EMBER_FRAGMENT through
  MaterialEmitters.Preset.fixed, emitted from the existing ground ring
  source alongside the round ember filler. Continuous rate 28 per
  second, budget 36, inside the 40 per second continuous cap.
- Filler: unchanged (darkness column, embers, flame, crimson plasma
  core, soul vapor).

### EvilSpearProjectileRenderer (class M)

- Pieces layer, two builders. Direction comes from the real velocity,
  with a rotation based fallback copied from the shared projectile
  pattern, so the pieces track the actual flight line:
  (1) Tendril wake: two ComplexGeometry.tendril strands through
  submitVeil with ComplexTextures.TENDRIL, streaming behind the head.
  The main strand runs from 0.30 ahead of center to 1.95 behind, width
  0.34, bowed sideways by its control point; a thinner echo (width
  0.17) runs offset to 1.55 behind. Root 0xF20D33 at the head to tip
  0x26030B in the tail, peak alphas 0.92 and 0.88, envelope is the
  existing body fade times the first person owner factor, so the
  existing first person spawn softening applies to the pieces too.
  (2) ComplexGeometry.flameTongues through submitEnergy with
  ComplexTextures.FLAME_TONGUE at the head (base 0.38 along the flight
  direction): four tongues, radius 0.24, height 0.62, hot base
  0xFFF3D6 to crimson tip 0xF20D33, peak alpha 0.95, sway from age.
  Vertex estimate at distScale 1.0: two tendrils at 384 each (768)
  plus tongues about 192, total about 960, inside M 2500.
- Complex particle layer: EMBER_FRAGMENT trail through Preset.fixed,
  sampled along the existing previous to current flight segment source,
  the same segment the round ember filler uses. Continuous rate 30 per
  second, budget 38, inside the continuous cap.
- Filler: unchanged (crimson plasma cluster, darkness wake, embers,
  flame, soul vapor). No spear body was drawn: the projectile stays
  spectral, as the no-shapes pass settled.

### HellHuntImpactRenderer (class M)

- Pieces layer, two builders:
  (1) ComplexGeometry.crackFan through submitEnergy (white canvas):
  radius is the existing spread radius (state radius times 1.35 times
  the existing cubic growth), so the cracks grow with the ground
  emission, hot 0xFFFFFF at the center to cool 0xF20D33 at the tips,
  peak alpha 0.95, envelope is the existing fade and opening envelope
  without distance scaling.
  (2) TendrilFan burst lashes: four one-count ComplexGeometry.tendrilFan
  calls through submitVeil with ComplexTextures.TENDRIL, from the
  impact center outward and up at seeded cardinal angles, length 0.62
  of the spread radius (minimum 0.9), width 0.27, root 0xF20D33 to tip
  0x26030B, peak alpha 0.92, envelope scaled by the existing eruption
  pulse with a 0.35 floor so the lashes are present at the opening.
  Vertex estimate at distScale 1.0: crack fan roughly 520 to 610 plus
  four lashes at about 256 each (1024), total about 1550 to 1640,
  inside M 2500 and inside the crackFan builder range in the standard.
  The entity already spawns a DebrisSpawner field server side; these
  pieces are the fast near field structure only, as the audit directs.
  The d74f13a destruction wiring (permanent obsidian conversion and
  the final crater) is untouched.
- Complex particle layer: EMBER_FRAGMENT and SPARK_STREAK.
  EMBER_FRAGMENT through Preset.fixed: a burst at the existing opening
  beat (12, or 16 for a final impact, inside the M burst budget of 30)
  plus a continuous ground emitter at rate 26 per second, budget 34.
  SPARK_STREAK through Preset.fixed: continuous ground emitter at rate
  20 per second, budget 28, ticked on the same eruption pulse curve as
  the ember filler. Both continuous rates are inside the 40 per second
  per emitter cap.
- Filler: unchanged (darkness, embers, flame, crimson plasma, soul
  vapor, and the existing opening material bursts).

## Verification

Targeted javac with JDK 25 (~/workspace/tools/jdk-25.0.5+8/bin/javac),
sourcepath src/main/java:src/client/java, compiling only the three
owned files, output under /tmp/evil-spear-262 and /tmp/evil-spear-263.

- 26.2 classpath (cp262.txt): zero errors in owned files, and all three
  owned classes were produced. The only errors in the closure are the
  4 baseline errors: DbaItems.java (Output has protected access in
  CreativeModeTab, x2) and DbaMenus.java (MenuType constructor and
  MenuSupplier private access).
- 26.3 classpath (cp263.txt): zero errors in owned files, and all three
  owned classes were produced. The only errors are the 10 baseline
  errors in DbaItems.java, DbaMenus.java, and entity files
  (DbaHostileEntity, FriezaSoldierEntity, NamekianWarriorEntity,
  PatrolRobotEntity).
- git diff --check on the owned files: clean. Added lines em dash scan:
  zero. Forbidden primitive calls in owned files: zero. Pieces are
  seeded from the entity UUID hash already carried in each render
  state, the same synced seed family the material emission uses.

## Foundation change requests

1. flameTongues is world Y up only, with no orientation parameter. At
   the Evil Spear projectile head the tongues therefore rise in world
   Y whatever the flight direction is. A direction oriented variant
   (base plane and rise axis from a caller supplied direction) would
   let projectile and slam flames stream along the motion instead of
   always licking upward. The projectile use here is kept small for
   exactly this reason (see the worse check).
2. tendrilFan radiates from one origin and cannot express true
   convergence. The charge strands and the Hell Hunt lashes both use
   the one-count tendrilFan per strand workaround that other owners
   also adopted. A gather variant (spread starts, converged end) in
   the shared toolkit would serve every charge and gather effect
   directly.
3. tendrilFan scales its strand count with the same distScale value
   that scales spine samples, so passing a reduced scale to hold the
   vertex budget also rounds the strand count down (a count of 5 at
   scale 0.66 becomes 3). Separate count and spine scale parameters
   would let owners hold both the intended strand count and the
   vertex budget at range.
4. crackFan always draws at local y 0.03 with no height offset. On the
   final Hell Hunt impact the permanent crater bowl can sit below that
   plane at the center. A caller supplied y offset would let ground
   crack effects follow cratered terrain instead of floating or
   clipping at one fixed height.

## Honest worse check

- EvilSpearCharge: low risk. The veil strands are rooted in near black
  (0x26030B) against darkness smoke filler in the same palette, so the
  outer half of each strand is deliberately subtle; the read is carried
  by the crimson ends converging on the core and by the strands
  tightening as the ring radius curve already gathers. The flame
  tongues sit inside the existing plasma and flame core cluster, and
  at full charge their height (about 1.05) stays well under the column
  height, so they read as fire at the tip rather than a second core.
  The composition has real inward structure the particles only version
  lacked, so it reads better overall.
- EvilSpearProjectile: moderate risk, named plainly. The head flame
  tongues rise in world Y because of foundation gap 1 above: on a
  level or downward throw they read as a small flame crown licking up
  at the head, not as fire trailing the flight. They are kept short
  (0.62) and narrow so they cannot dominate, and the veil wake tendrils
  carry the flight read, giving the projectile the torn silhouette the
  particles only version had lost when the spear body was removed.
  Net: better than the particles only version, with the tongue
  orientation as the one piece that would improve again if the shared
  toolkit gains an oriented variant. The wake tendrils are attached in
  local space and do not lag behind the projectile history; at high
  speed they read as streamers fixed to the head, which matches how
  the material trail already behaves in this renderer.
- HellHuntImpact: low to moderate risk. Four lashes is a sparse burst
  compared with the dense material column, by budget design: more
  lashes at full spine samples would break the class M vertex budget
  alongside the crack fan. The lashes are wide (0.27) and long (0.62
  of spread) so they still register as burst structure. On a final
  impact, the central cracks can be partly hidden inside the permanent
  crater the destruction pass leaves (foundation gap 4); the rim
  cracks and the lashes stay visible above it, and the cracks still
  glow through the filler smoke at the center. Overall stronger than
  both the particles only version and the pre redo crack stars, which
  were clean straight lines this fan replaces with wandering, pinching,
  forking cracks.

No effect in this slot is judged worse than its current state overall.
The two named risks (projectile tongue orientation, final crater
occlusion of central cracks) are shared toolkit limits, recorded above
as change requests rather than worked around with private code.
