# Complex Effects Redo, Phase 2 Report: Sickle of Sorrow

Slot: Sickle of Sorrow (theme: soul wisps, void plasma, darkness smoke).
Base: HEAD 9178eac. Files edited, only these two:
src/client/java/com/dragonblockarcanedba/client/render/DimensionalRiftRenderer.java
and src/client/java/com/dragonblockarcanedba/client/render/SorrowSlashRenderer.java.
No commits. No damage number, cooldown, cost, timing, mechanic, or synced
data field changed in either file. Seeds still come from the entity's
synced state (entity id and position hash for the rift, entity id plus
tilt and impact flags for the slash), exactly as the emission used them.
The d74f13a destruction wiring is untouched. Zero drawColoredBox,
drawCylinder, drawDisc, drawSphere, and drawHelix calls in either file.

## DimensionalRift (class M)

Pieces layer, added in submit, in entity local space with the tear heart
at mid fissure height, following the same lifecycle fade and orbit phase
as the existing emission:

- ComplexGeometry.shardBurst through submitSolid (solid discipline,
  faceted vertex shading): 5 shards, unit 0.80, spread 100 degrees around
  the up axis, root color 0x24053D to tip color 0xB84DFF, peak alpha
  0.95. That is 5 times 288 vertices, 1440.
- ComplexGeometry.tendril, three strands, through submitEnergy with
  ComplexTextures.TENDRIL (energy discipline on the dissolve shader):
  each strand runs from a rim point at radius times 0.88, at the rig's
  current orbit angle plus the golden angle step the orbit clusters use,
  to the tear heart, with the control point pushed tangentially so the
  strands visibly stream and curl inward with the pull. Root color
  0x9900E6 (the rift's soul violet) to tip color 0xE633FF, peak alpha
  0.92. Tendrils run at 0.70 of the distance scale, 17 spine samples,
  272 vertices each, 816 total.
- Worst case per-submit total: 2256 vertices, inside the class M budget
  of 2500 and the shardBurst cap of 16 meshes.

Complex particle layer:

- WISP_CURL through MaterialEmitters.Preset.fixed, a new continuous
  Emitter in the RiftRig on the existing inward soul stream sampler
  (volumeSample with stream true), rate 30 per second, budget 40 per
  second, inside the continuous budget of 40 per second per emitter.

Filler layer: all seven pre-existing material emitters (darkness, soul,
two plasma, soul and darkness orbit clusters, glass dust) are unchanged
and now read as filler around the shards and tendrils. PLASMA is no
longer the dominant layer: the solid shard burst and the three tendril
bodies carry the silhouette.

## SorrowSlash (class M)

Pieces layer, added in submit, in entity local space on the same synced
right, up, and forward basis the emission sampler uses, so the strands
inherit the reap tilt and the impact versus flying scale:

- ComplexGeometry.tendril, two strands, through submitEnergy with
  ComplexTextures.TENDRIL:
  - Crest strand: end to end across the span (right times plus and minus
    halfSpan times 0.95, dropped by halfHeight times 0.35) with the
    control pushed up the arc (up times halfHeight times 0.95), width
    0.50 times scale, root color 0xCC0DD9 to tip color 0xE6FAFF, peak
    alpha 0.95.
  - Soul strand: the same arc offset backward along the travel direction
    by 0.40 times scale, width 0.28 times scale, root color 0xB84DFF to
    tip color 0xCC0DD9, peak alpha 0.90.
- Worst case per-submit total: 2 tendrils at full 24 spine samples, 768
  vertices, inside the class M budget of 2500.

Complex particle layer:

- WISP_CURL through MaterialEmitters.Preset.fixed, a new continuous
  Emitter in the SlashRig on the soul band sampler, rate 30 per second,
  budget 40 per second, inside the continuous budget.
- WISP_CURL burst at the impact pop, through MaterialEmitters.burst with
  Preset.fixed: 12 times envelope, inside the class M burst budget of 30
  complex sprites per burst event, added alongside the unchanged
  material bursts.

Filler layer: all five pre-existing material emitters (two plasma, soul,
darkness, vapor) and the three material impact bursts are unchanged.

## Verification

Targeted javac with JDK ~/workspace/tools/jdk-25.0.5+8/bin/javac,
sourcepath src/main/java:src/client/java, compiling both owned files,
private output tags /tmp/sickle262 and /tmp/sickle263, logs read in full
at /tmp/javac-sickle-262.log and /tmp/javac-sickle-263.log.

- 26.2 classpath (cp262.txt): zero errors in either owned file. Both
  renderer class files and their inner classes were produced. The 4
  errors in the log are baseline kinds in the closure, in
  item/DbaItems.java (Output has protected access in CreativeModeTab)
  and inventory/DbaMenus.java (MenuType constructor and MenuSupplier
  private access), neither of which this slot owns or touched.
- 26.3 classpath (cp263.txt): zero errors in either owned file. Both
  renderer class files were produced. The 10 errors in the log are
  baseline kinds in the closure: the same DbaItems and DbaMenus errors
  plus entity/DbaHostileEntity.java, entity/PatrolRobotEntity.java,
  entity/FriezaSoldierEntity.java, and entity/NamekianWarriorEntity.java
  (missing swinging, attackAnim, and hurtMarked symbols and the changed
  swing signature). None are in owned files.
- git diff --check on both owned files: clean. Em dash scan on both
  owned files and on this report: zero.

## Foundation change requests

None. ComplexGeometry.shardBurst, tendril, submitSolid, submitEnergy,
ComplexTextures.TENDRIL, and MaterialEmitters.Preset.fixed with
DbaParticles.WISP_CURL covered everything this slot needed. No private
toolkit was built.

## Honest worse check

DimensionalRift: the main risk is crowding at the tear. The rift already
fills its volume with darkness and soul emission, and adding 5 solid
shards plus 3 bright tendrils at the center could have turned the core
into a knot. It does not read worse, for two reasons: the shards sit at
mid fissure where the white core plasma already concentrated the eye,
so they give that glow a fractured silhouette instead of competing with
it, and the tendrils end at the same heart the soul stream already flows
to, so they reinforce the inward pull read rather than adding a new
direction. At large radii (the rift expands toward 15) the tendrils get
long and thin relative to the volume; they still read as stream lines,
and the shard burst stays at the tear where it belongs. One honest
caveat: shardBurst throws its cone around the up axis, so the shards
read as a fountain fan at the tear, not a spherical explosion. That
matches the tall fissure identity of this rift, so it is a fit, not a
defect.

SorrowSlash: the risk was a tendril arc fighting the sampled material
band, which spreads across the whole swept volume. The strands sit on
the crest arc and just behind it, inside the band, so the composition
reads as a structured crescent with a dissolving wake, which is
strictly more structured than the particles-only version and restores
the body the no-shapes pass removed. The arc control pushes the strand
midpoint up by halfHeight times 0.95 while the band is a flat swept
box, so at extreme reap tilt the crest strand can sit slightly above
the densest part of the band; the soul strand and the wisp layer bridge
that gap, and the fade is shared, so it does not detach visually in the
math. Neither rebuild reads worse than the particles-only version, and
neither is shipped with a known regression.
