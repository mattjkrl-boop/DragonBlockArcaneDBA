# Complex Effects Report: Katana

Owner slot: Katana. Base HEAD 9178eac. Files edited: only the four owned
renderers. No commits. Themes, colors, damage, cooldowns, costs, timing,
mechanics, and synced fields are unchanged from HEAD. Existing material
emission is untouched and remains as the filler layer in every effect.

## Per effect

### KatanaAimGuideRenderer (class M)

- Pieces layer: ComplexGeometry.tendrilFan through
  ComplexGeometry.submitEnergy with ComplexTextures.TENDRIL. Five thin
  filaments (width 0.09), converged at the blade origin (the same local
  offset the first person guide origin uses), running along the forward
  aim path at a narrow 7 degree spread for 85 percent of the synced
  range. Root color 0x00D9FF, tip 0xFFFFFF, peak alpha 0.92, envelope is
  the existing lifecycle alpha (build, pulse, fade out). Vertex estimate
  at distScale 1.0: 5 tendrils x 384 = about 1920, inside the M budget
  of 2500.
- Complex particle layer: SPARK_STREAK through
  MaterialEmitters.Preset.fixed, drifting in along the path source.
  Continuous rate 24 per second, budget 32, inside the 40 per second
  continuous cap.
- Filler: unchanged (plasma sheath, white core, vapor, terminal and
  station plasma).

### KatanaChargeRenderer (class M)

- Pieces layer: ComplexGeometry.tendrilFan through submitEnergy with
  ComplexTextures.TENDRIL. Five lashes converged at the hilt position
  (right -0.28, forward -0.05, up 0.75, the same point the hilt emitters
  use), fanning up at 58 degrees. Length grows with the synced charge
  ratio (0.55 at zero charge to 1.70 at full) and width grows 0.09 to
  0.15, so the strands visibly build with the charge. Root 0xFFFFFF,
  tip 0x00D9FF, peak alpha 0.92, envelope is the existing alpha times
  pulse. Vertex estimate at distScale 1.0: about 1920, inside M 2500.
- Complex particle layer: SPARK_STREAK at the hilt (rate 20, budget
  28) and sparse WISP_CURL on the stance rim (rate 9, budget 14), both
  through Preset.fixed and inside the continuous cap.
- Filler: unchanged (gather plasma, rim vapor, hilt plasma, SPARK at
  the hilt, ground vapor).

### HeavenSplitterRenderer (class L)

- Pieces layer, two builders, both on the shader: (1)
  ComplexGeometry.tendril through submitEnergy with
  ComplexTextures.TENDRIL: one thick torn strand (width 0.55 times
  scale) along the split path at 52 percent of the wall height, control
  point bowed by a seeded offset from the entity seed, root 0xFFFFFF,
  tip 0x00D9FF, peak alpha 0.95, envelope is the existing lifecycle
  fade. (2) ComplexGeometry.crackFan through submitEnergy, centered by
  a pose translation at the path midpoint on the ground, radius 52
  percent of the dash length, hot 0xFFFFFF to cool 0x00D9FF, peak
  alpha 0.90. Vertex estimate at distScale 1.0: tendril 384 plus
  crackFan roughly 500 to 900, total well inside the L budget of 6000.
- Complex particle layer: SPARK_STREAK through Preset.fixed from the
  cut volume, gated by the existing strike flash (rate 30, budget 38),
  plus bursts at the existing strike tick: SHARD_CHIP 8 at each of the
  three strike points (24 total) and SPARK_STREAK 10 at the midpoint,
  34 complex sprites in the burst, inside the L burst budget of 60.
- Filler: unchanged (mantle, main, core plasma, vapor, SPARK, ground
  vapor, and the plasma and vapor strike bursts).

### SwiftCrescentRenderer (class M)

- Pieces layer: ComplexGeometry.tendril through submitEnergy with
  ComplexTextures.TENDRIL as the wave body: tip to tip across the cut
  basis (right -2.0 to +2.0 times scale, up 0.38), bowed forward 1.05
  and dipped -0.62 at the belly by the control point, width 0.36 times
  scale, root 0xFFFFFF to the variant lip color (0x00DBFF variant 0,
  0x8CCCFF variant 1), peak alpha 0.92, envelope is the existing
  lifecycle fade. The variant flipped forward is used, so variant 1
  bows the other way like its wake. Vertex estimate: 384, inside
  M 2500.
- Complex particle layer: SPARK_STREAK through Preset.fixed shed
  across the cut volume (rate 26, budget 34), plus a SPARK_STREAK
  burst of 12 at the existing strike tick, inside the M burst budget
  of 30.
- Filler: unchanged (silver plasma, white core, lip plasma, wake
  vapor, SPARK, and the strike bursts).

## Verification

Targeted javac with JDK 25 (~/workspace/tools/jdk-25.0.5+8/bin/javac),
sourcepath src/main/java:src/client/java, compiling only the four
owned files, output under /tmp/katana-262 and /tmp/katana-263.

- 26.2 classpath (cp262.txt): zero errors in owned files. The only
  errors in the closure are 4 baseline errors: DbaItems.java (Output
  has protected access in CreativeModeTab, x2) and DbaMenus.java
  (MenuType constructor and MenuSupplier private access).
- 26.3 classpath (cp263.txt): zero errors in owned files. The only
  errors are 10 baseline errors in DbaItems.java, DbaMenus.java, and
  entity files (DbaHostileEntity, FriezaSoldierEntity,
  NamekianWarriorEntity, PatrolRobotEntity).
- git diff --check on the owned files: clean. Added lines em dash
  scan: zero. Forbidden primitive calls (drawColoredBox, drawCylinder,
  drawDisc, drawSphere, drawHelix) in owned files: zero.
- One owned compile error was found and fixed during verification:
  KatanaAimGuide captured the reassigned alpha variable in the piece
  lambda; both AimGuide and Charge now capture a final envelope copy.

## Foundation change requests

1. tendrilFan radiates from one origin and cannot express true
   convergence. A gather variant whose strands start spread and end
   converged on a target point would serve KatanaAimGuide (filaments
   converging on the blade), KatanaCharge (inward lashes), and every
   other charge and gather effect in the audit exactly, instead of
   approximating convergence by placing the fan origin at the target.
2. crackFan is a radial fan with no orientation or elongation
   control. HeavenSplitter wants cracks running along a landing line;
   a line aligned crack runner (start, end, width) in the shared
   toolkit would fit it and the fissure class effects better than a
   radial fan centered on the line midpoint.
3. tendril curl amplitude is fixed at 0.055 of chord length with no
   caller control. On a long chord (the aim guide at up to about 13.6
   blocks of fan length) the mid spine wander reaches about 0.75
   blocks. A curl scale parameter would let long guide filaments stay
   tight while short lashes keep their full curl.

## Honest worse check

- KatanaAimGuide: moderate risk. Because of the fixed curl noted
  above, the filaments wander more than a calibration guide ideally
  would, and at full range they can read as loose plasma whips near
  the mid path. They converge at the blade, stay thin, and the
  original sheath and core emission still carries the readable guide
  line, so the composition reads better than the particles only
  version. Not shipped silently: if Carter wants a straighter guide,
  change request 3 is the fix, not a local workaround.
- KatanaCharge: low to moderate risk. The fan builder radiates, so
  the lashes grow outward and up from the hilt rather than collapsing
  into it; the inward read comes from their growth with charge plus
  the existing inward velocity gather emission. At full charge the
  fan could read as a small crown around the hilt. Length starts at
  0.55 so it never dominates the stance, and it is clearly better
  than round plasma alone. Change request 1 would make it exact.
- HeavenSplitter: low risk on the strand, moderate on the cracks.
  The crack fan is radial, so cracks also run perpendicular to and
  behind the landing line, reading as a burst web at the midpoint
  rather than a strict line. It sits at ground level under the plasma
  wall and the strand, so it supports rather than replaces the cut
  read. The single mid height strand is thinner than the full wall
  volume it crosses, which is intended: the wall stays plasma filler
  and the strand gives it a torn spine. Overall stronger than the
  particles only version.
- SwiftCrescent: low risk. The tendril is 0.36 times scale wide, so
  at large slash scales it reads as a bowed band rather than a thin
  crescent edge; the crossed ribbon pair, tendril canvas tearing,
  and the pinch windows keep it from reading as a clean ribbon, and
  the host lives only 10 ticks. It restores the crescent silhouette
  the particles only version had lost, so it reads better than both
  the particles only version and, in structure, the pre redo mesh.

No effect in this slot is judged worse than its current state overall;
the two moderate risks above are named with their shared toolkit fixes
rather than hidden.
