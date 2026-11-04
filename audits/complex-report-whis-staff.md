# Complex Effects Redo, Phase 2 Report: Whis Staff

Owner slot: Whis Staff. Base HEAD 9178eac. File edited, and only this
one: src/client/java/com/dragonblockarcanedba/client/render/TemporalRiftRenderer.java.
Item and entity files were not touched: item/WhisStaffItem.java and
entity/TemporalRiftEntity.java are unchanged, so spawn compatibility,
the synced seed, the synced radius and lifetime, and the d74f13a
destruction wiring (permanent conversions) are exactly as at HEAD.
TimeShatter and ErasureChargeOrb belong to other slots and were not
touched. Theme held throughout: temporal plasma (0x61C2FF field,
0xEAF8FF pale core), glass dust (0xDDF7FF, 0xBFEAFF), mist, soul
(0xCFF4FF), and the existing darkness core. No damage number,
cooldown, cost, timing, mechanic, or synced field changed. The
lifecycle envelope (build 15 percent, dissipate 35 percent), the
radius unfold, and every existing material stream rate and budget
are verbatim; pieces and the complex sprite stream were added,
never traded against the filler. No commits made.

## TemporalRift (class M)

- Pieces layer, solid: one ComplexGeometry.shardBurst call through
  ComplexGeometry.submitSolid, centered on the tear heart. The heart
  sits at height times 0.42, the same core lift band the existing
  core emission streams toward (its streams use core lifts of 0.22
  to 0.50), so the shards hang where the material already converges.
  Five shards in a 150 degree cone around the up axis, unit scaled
  with the unfolded radius (radius times 0.09, floor 0.5), giving
  the standard three size classes at about 0.36 to 1.92 blocks long
  at the full 12 block radius. Root color deep celestial blue
  0x2F6DB3 to tip pale temporal glass 0xDDF7FF, faceted through the
  plain ki type, alpha 0.95 times the lifecycle envelope. Five
  shards at 288 vertices is 1440 vertices, and the burst count of
  5 is inside the 16 mesh shardBurst cap.
- Pieces layer, energy: three ComplexGeometry.tendril strands
  through ComplexGeometry.submitEnergy with ComplexTextures.TENDRIL,
  streaming inward from the field rim (radius times 0.88, at three
  heights around the heart) to the heart itself. Each control point
  bows tangentially by rim radius times 0.20 plus a small lift, so
  the strands curl as they converge instead of running straight.
  Width 0.36, root celestial plasma 0x61C2FF to tip pale core
  0xEAF8FF, peak alpha 0.92 through the shader life remap. Rim
  angles use the golden angle step plus a seed derived offset, and
  all seeds derive from the entity synced seed (with the existing
  position and caster fallback untouched). The tendrils run at
  reduced detail scale (distance scale times 0.70, 17 spine samples,
  272 vertices each, 816 total). Combined pieces worst case is
  2256 vertices, inside the class M budget of 2500.
- Slow swirl: the whole pieces composition turns around the field
  axis at 0.4 degrees per tick of synced age (40 degrees over the
  100 tick rift life), applied to the pose for the pieces only.
  The emission, which has no angular structure, is unaffected.
- Complex particle layer: one continuous WISP_CURL emitter via
  MaterialEmitters.Preset.fixed(DbaParticles.WISP_CURL, Vec3.ZERO,
  0.008f), added to the existing RiftEmission bundle. Its source is
  the bundle inward volume sampler (strength 0.28, core lift 0.44),
  so the curls stream inward with the soul stream they ride beside.
  Rate 32 per second, budget 40, inside the 40 per second continuous
  cap for complex sprites. This renderer has no burst event moment
  (it is a continuous field), so no burst was added; the audit asks
  for the WISP_CURL layer, which the continuous stream provides for
  the whole life of the rift.
- PLASMA is no longer the body: the tear now has authored structure
  (glass shard cluster plus inward tendrils), and the plasma, soul,
  glass dust, mist, and darkness streams are filler around it,
  exactly as before in rate and color.

## Budgets and rules check

- Zero drawColoredBox, drawCylinder, drawDisc, drawSphere, or
  drawHelix calls in the owned file (grep verified).
- Every energy piece goes through submitEnergy with the TENDRIL
  canvas; the solid shard burst goes through submitSolid. No
  private geometry, emitter, or texture code was built.
- Peak alphas are honest: 0.95 on the solid shards (literal opacity
  on the ki type) and 0.92 on the shader tendrils. Faintness is not
  faked anywhere; the deep root color on the shards carries the
  darker read.
- Pieces are seeded from the entity synced state (state.seed, with
  its existing synced fallback) exactly as the emission is.
- Split source sets stay clean: the only edit is client side;
  DbaParticles (common) was only referenced, not modified.

## Verification

Targeted javac with JDK at ~/workspace/tools/jdk-25.0.5+8/bin/javac,
sourcepath src/main/java:src/client/java, compiling the one owned
file, output to private /tmp tags whis-staff-262 and whis-staff-263,
logs at /tmp/whis-staff-262.log and /tmp/whis-staff-263.log. Both
full logs were read before making these claims:

- 26.2 classpath (cp262.txt): zero errors in the owned file. The
  only errors in the closure are the known baseline kinds in
  DbaItems.java (CreativeModeTab Output protected access, 2) and
  DbaMenus.java (MenuType private access, 2), 4 errors total.
- 26.3 classpath (cp263.txt): zero errors in the owned file.
  Baseline errors only: the same DbaItems and DbaMenus kinds plus
  the known entity kinds in DbaHostileEntity, FriezaSoldierEntity,
  NamekianWarriorEntity, and PatrolRobotEntity, 10 errors total.
- The owned class and all three inner classes (RiftRenderState,
  FieldVolume, RiftEmission) were produced in both output trees.
  git diff --check on the owned file is clean, and an em dash scan
  of the owned file and this report is zero.

## Foundation change requests

No blocking gaps. Two optional, non-blocking notes for the merge:

1. ComplexGeometry.shardBurst fans shards in a cone around one
   axis only. A spherical distribution variant would serve rift
   heart clusters directly. Worked around here with a 150 degree
   cone around the up axis, which covers every direction except
   near straight down, so this is a convenience, not a need.
2. ComplexGeometry.tendrilFan fans outward from one origin.
   Inward streaming compositions (rim points converging on a
   heart) need a manual per tendril loop, as here and in
   DimensionalRiftRenderer. A converge variant (many origins, one
   target) would be the shared fix if the merge wants one.

## Honest worse check

- Placement risk, low: the tear heart is an inferred point, since
  the pre-redo rift is a volume field with no single tear mesh.
  I placed it at core lift 0.42 because the existing core, plasma,
  and darkness streams already converge on lifts between 0.22 and
  0.50 of the field height. The pieces therefore reinforce the
  convergence the filler already described, rather than inventing
  a new focal point somewhere the material does not go.
- Scale risk, low: at the full 12 block radius the shard cluster
  (under 2 blocks per shard) is small against a 24 block wide
  field when seen from outside. The tendrils carry the structure
  at that distance, since each spans rim to heart at about 10.6
  blocks, and the caster stands at the field center in normal use,
  where the cluster hangs about 5.3 blocks overhead at readable
  size. The particles only version had no structure at any
  distance, so this does not read worse.
- Cone weighting note: shardBurst samples its cone angle with a
  square root roll, so more shards point sideways or downward than
  straight up. For a burst at a tear that reads as radiating
  fragments rather than a fountain, which matches the fractured
  temporal glass identity. It is a look to confirm in game, not a
  defect I could improve inside the shipped builder.
- Tendril pinch watch item: the tendril builder collapses its
  width at two seeded pinch windows, and on strands this long a
  pinch could read as a break. Three strands at different rim
  heights, plus the unchanged inward plasma and soul filler running
  the same volume, keep the inward read continuous through any one
  pinch. First thing to check in game.
- Swirl note: the slow pose rotation (8 degrees per second) turns
  the shards with the tendrils. A fully frozen cluster would also
  have fit the time suspension theme, but the entity applies real
  vortex physics, and a gentle shared swirl reads as that vortex
  made visible. If Carter prefers frozen shards, dropping the pose
  rotation is a one line change in this file and nothing else
  depends on it.

Nothing in this slot reads worse than the particles only version or
the pre-redo version in a way I could not fix inside the standard,
so nothing was held back.
