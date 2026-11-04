# Complex Effects Redo, Phase 2 Report: Saber

Owner slot: Saber. Base HEAD 9178eac. Files edited, and only these:
SaberSlashRenderer.java, SaberDimensionalLineSlashRenderer.java,
SaberVoidTearRenderer.java, SaberDodgeSparkRenderer.java, all under
src/client/java/com/dragonblockarcanedba/client/render/.
Theme held throughout: void plasma (0x4DD8FF, white core), spatial
glass dust (0xBFEFFF), darkness vapor (0x3D1A78 and 0x260052), gold
only where the dodge spark already had it (0xFFD75E, 0xFFF3B0).
No damage number, cooldown, cost, timing, mechanic, or synced field
changed. All existing material emission is kept verbatim as the
filler layer; pieces and complex sprites were added, never traded
against it. No commits made.

## SaberSlash (class M)

- Pieces layer: three ComplexGeometry.tendril strands through
  ComplexGeometry.submitEnergy with ComplexTextures.TENDRIL. The
  main strand follows the exact parabolic arc the trail emission
  already sampled (x across emission.span, z bulging to
  emission.chord at the middle, quadratic bezier control at twice
  the chord), white root to void cyan tip, width 0.40 times scale,
  peak alpha 0.95. Two thinner offset strands (void cyan to dark
  violet, void cyan to pale glass) sit above and below the main
  strand. The pose carries the same synced rotation matrix the
  emission uses (yRot, xRot, tiltAngle), and all seeds derive from
  the synced state seed. Vertex estimate: three tendrils at at
  most 24 samples each, near 1150 vertices, inside the class M
  budget of 2500.
- Complex particle layer: continuous SHARD_CHIP emitter riding the
  trail source at 34 per second (budget 40) and continuous
  SPARK_STREAK emitter on the tip glass source at 30 per second,
  both via MaterialEmitters.Preset.fixed. At the existing strike
  flash moment, added bursts of SHARD_CHIP (13 at full density)
  and SPARK_STREAK (11), a complex burst total of 24, inside the
  class M burst budget of 30.
- PLASMA is no longer the body: the tendril strands are the slash
  body and plasma is filler around them.

## SaberDimensionalLineSlash (class M)

- Pieces layer: two ComplexGeometry.tendril strands through
  submitEnergy with ComplexTextures.TENDRIL, running the full
  synced line (from dir times minus halfLen to dir times plus
  halfLen, in the world aligned pose frame at the entity origin).
  Main strand white root to void cyan tip, width 0.34 times scale,
  peak alpha 0.95, with a slight bow control offset; second strand
  offset upward, void cyan to dark violet, width 0.18. Vertex
  estimate near 770, inside the class M budget of 2500.
- Complex particle layer: continuous SHARD_CHIP emitter on the rib
  station glass source at 34 per second and continuous
  SPARK_STREAK emitter on the core source at 30 per second, both
  Preset.fixed. At the existing strike flash, added bursts of
  SHARD_CHIP (13) and SPARK_STREAK (12), total 25, inside the
  class M burst budget of 30.

## SaberVoidTear (class M)

- Pieces layer, solid: five ComplexGeometry.shard meshes through
  ComplexGeometry.submitSolid, placed in the tear plane as a flat
  fan around the slit. Angles are seeded from the synced seed with
  up to 0.35 radians of jitter, lengths vary 0.8 to 1.25, root
  color dark violet 0x3D1A78 to tip pale glass 0xBFEFFF, alpha
  0.95 times the piece envelope. Five shards at 288 vertices each
  is 1440 vertices.
- Pieces layer, energy: two ComplexGeometry.tendril strands
  through submitEnergy with ComplexTextures.TENDRIL, streaming
  inward from 1.05 times slitH to 0.10 times slitH with a
  tangential control offset, void cyan root to white tip, peak
  alpha 0.92. Near 770 vertices. Combined total near 2210, inside
  the class M budget of 2500.
- Complex particle layer: continuous WISP_CURL emitter on the
  darkness volume source at 32 per second and continuous
  ASH_FLAKE emitter on the outward glass source at 28 per second,
  both Preset.fixed. At the existing flash moment, added bursts
  of WISP_CURL (10) and ASH_FLAKE (8), total 18, inside the class
  M burst budget of 30.
- Wall occlusion: unchanged. Pieces submit through the normal
  depth tested collector path (submitSolid and submitEnergy) in
  the tear plane under the same synced rotation as before;
  shouldRender, extraction, and all timing are untouched, and no
  render type or depth behavior was altered.

## SaberDodgeSpark (class S)

- No pieces layer, as the audit specifies for this class S burst.
- Complex particle layer: continuous SPARK_STREAK emitter in the
  burst volume at 36 per second and continuous SHARD_CHIP emitter
  at 30 per second, both Preset.fixed and inside the 40 per
  second budget. The existing opening burst gains SPARK_STREAK
  (6) and SHARD_CHIP (4), a complex burst total of 10, inside
  the class S burst budget of 12.

## Budgets and rules check

- Zero drawColoredBox, drawCylinder, drawDisc, drawSphere, or
  drawHelix calls in the four files (grep verified).
- Every energy piece goes through submitEnergy with the TENDRIL
  canvas; every solid piece through submitSolid. No private
  geometry, emitter, or texture code was built.
- Peak alphas on pieces are 0.90 to 0.95; faintness is carried by
  color choice, never by a low constant alpha. Envelopes reuse
  each effect's existing open, cut, and fade values, so pieces
  live and die with the effect.
- Continuous complex sprite rates are 28 to 36 per second per
  emitter, at or under the 40 per second cap. Complex burst
  totals: Slash 24, LineSlash 25, VoidTear 18, DodgeSpark 10,
  all inside their class budgets.
- The d74f13a destruction wiring was not touched. Split source
  sets stay clean: all edits are client side; DbaParticles (common)
  was only referenced, not modified.

## Verification

Targeted javac with JDK at ~/workspace/tools/jdk-25.0.5+8/bin/javac,
sourcepath src/main/java:src/client/java, compiling the four owned
files, output to private /tmp tags saber-complex-262 and
saber-complex-263:

- 26.2 classpath (cp262.txt): zero errors in owned files. The only
  errors in the closure are the known baseline kinds in
  DbaItems.java (CreativeModeTab Output protected access, 2) and
  DbaMenus.java (MenuType private access, 2), 4 errors total.
- 26.3 classpath (cp263.txt): zero errors in owned files. Baseline
  errors only: the same DbaItems and DbaMenus kinds plus the known
  entity kinds in DbaHostileEntity, FriezaSoldierEntity,
  NamekianWarriorEntity, and PatrolRobotEntity, 10 errors total.
- All four owned classes and their inner classes were produced in
  both output trees. git diff --check on the four files is clean,
  and an em dash scan of the four files and this report is zero.

## Foundation change requests

No blocking gaps. Two optional, non-blocking notes for the merge:

1. ComplexGeometry.shardBurst only fans shards in a cone around
   one axis. A flat fan variant (shards spread inside a plane)
   would serve tear and rift effects directly. Worked around here
   with individual seeded shard calls in the tear plane, which
   stays inside budget, so this is a convenience, not a need.
2. ComplexGeometry.tendril has no zero curl option for a perfectly
   straight cut. The line slash worked around it with a small
   bezier bow, which reads as a slight dimensional waver. If the
   merge wants a razor straight option, a curl scale parameter on
   tendril would be the shared fix.

## Honest worse check

- SaberSlash: low risk of reading worse. The tendril arc is the
  same path the emission already drew, so the body reinforces the
  existing read. The one watch item is the tendril pinch windows:
  at the two seeded pinches the strand narrows sharply, which
  could read as segmentation on a very short 10 tick slash. The
  two offset strands and the unchanged filler keep the arc
  continuous through the pinches, so I judge it net better, but
  it is the first thing to check in game.
- SaberDimensionalLineSlash: the honest risk in this slot. A
  dimensional line slash wants to read razor straight, and the
  tendril builder adds mid-spine curl plus my small bow control,
  so the body wavers slightly where the old emission read as a
  clean line of plasma. The waver is small (bow 0.16 times scale
  over a line at least 10 long, curl amplitude 5.5 percent of
  chord), the white core emission still runs the exact straight
  path underneath, and the standard mandates a tendril body here,
  so I shipped it. If Carter finds the waver wrong in game, the
  fix is foundation request 2 above, not a local workaround.
- SaberVoidTear: moderate watch item. Five shards at seeded
  angles can still hint at a regular fan on a given seed, since
  the base angles are evenly spaced before jitter. Length and
  angle jitter plus the faceted silhouettes keep any two tears
  from matching, and the shards sit in the tear plane where the
  glass dust already flew, so the composition reads as spatial
  fragments rather than a star. It does not read worse than the
  particles only version, which had no structure at the tear rim
  at all.
- SaberDodgeSpark: one color note. SHARD_CHIP sprites carry the
  fixed stone gray style color from the shared sprite set, which
  sits slightly cooler than this burst's gold theme. The audit
  assigns SHARD_CHIP here and the standard states theme color
  comes from the pieces and filler layers, which remain gold.
  At 4 burst chips and 30 per second inside a dense gold volume,
  the gray reads as physical debris, not as a theme break.

Nothing in this slot reads worse than the pre-redo version in a
way I could not fix inside the standard, so nothing was held back.
