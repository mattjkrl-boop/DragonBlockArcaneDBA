# Complex Effects Redo, Phase 2 Report: Dimensional Sword

Owner slot: Dimensional Sword. Files edited, only these two:
`src/client/java/com/dragonblockarcanedba/client/render/DimensionalSlashRenderer.java` and
`src/client/java/com/dragonblockarcanedba/client/render/DimensionalWarpRiftRenderer.java`.
No commits. Base HEAD 9178eac. Themes, colors, damage numbers, cooldowns, costs, timing,
mechanics, and synced data fields are identical to HEAD: every pre-existing emission line
in both files is untouched, and the new layers only add on top of it. The d74f13a
destruction wiring (the Dimensional Slash and Warp Rift permanent conversions) is not
touched. No plain primitives anywhere: zero drawColoredBox, drawCylinder, drawDisc,
drawSphere, or drawHelix calls in either file.

## DimensionalSlash (class L)

Pieces layer, one builder type at full strand count, as the task allows for this slot:
- `ComplexGeometry.tendril`, three strands in one `submitEnergy` call sampling
  `ComplexTextures.TENDRIL`, drawn in entity local space along the same right, up, and
  forward basis the existing cut plane emission samples, seeded from the entity's synced
  seed (`seed ^ 0xD51A`, `0xD52B`, `0xD53C`).
- Main strand: one thick strand, tip to tip across the full cut width (halfWidth 2.20
  times scale), bezier control bowed along up at twice the sampled belly so the strand
  crests on the same bowed line as the crescent, width 0.85 times scale times the
  existing pulse, gradient abyssal violet 0xA60DF2 to blood magenta 0xFF1A59, peak
  alpha 0.95.
- White core strand: 0.80 of the span, offset forward of the belly, width 0.32, gradient
  white 0xFFFFFF to magenta 0xFF26D9, peak alpha 0.95.
- Rim strand: 0.97 of the span, offset behind the belly, width 0.28, reversed gradient
  0xFF1A59 to 0xA60DF2, peak alpha 0.90.
- Vertex accounting: each tendril is at most 24 spine samples times 16 vertices (384);
  three strands stay near 1150 vertices, inside the class L budget of 6000. The piece
  envelope follows the same fade the plasma uses, including the linger decay factor
  (1.0 minus 0.55 times lingerT), so the strands dissolve with the cut while the residue
  layers take over during the linger, exactly as the plasma already did.

Complex particle layer:
- `DbaParticles.SHARD_CHIP` through `MaterialEmitters.Preset.fixed`, one emitter per
  pinched tip, sourced from the same tip sphere volumes and with the same backward and
  outward velocity as the existing spatial glass dust at those tips, rate 28 per second
  with budget 40, inside the 40 per second continuous cap per emitter. No burst event,
  so the class L burst budget of 60 is untouched.

Filler layer: unchanged. The three plasma stops across the cut plane, the white and
magenta singularity core cluster, the darkness vapor wrap, the violet vapor, and the
tip glass dust all tick with their original rates, budgets, colors, and envelopes.
PLASMA is no longer the body of the effect; the tendril strands are.

## DimensionalWarpRift (class M)

Pieces layer, all three assigned builders:
- `ComplexGeometry.shardBurst` through `submitSolid`: a fan of 4 spatial shards at the
  tear heart (local y 1.4, where the singularity cluster sits), cone axis up, spread
  68 degrees, unit 0.70, root dark void 0x1A0533 to tip spatial glass 0xE8CCFF, alpha
  0.95. Its envelope is the tear envelope (shared lifecycle envelope times openTear),
  so the fan follows the tear open and shut shaping. 4 shards is inside the shardBurst
  cap of 16 meshes.
- `ComplexGeometry.tendril`, two strands through `submitEnergy` sampling
  `ComplexTextures.TENDRIL`: streaming inward from rim points at radius times 0.85 to
  the heart, with a tangential curl offset on the control point, width 0.20, gradient
  void violet 0x990DE6 to magenta 0xFF26D9 (the inward stream's own colors), peak alpha
  0.92, seeded from the synced seed. Angles carry a seed derived offset
  (`(seed & 7) * 0.13`), so every client draws the same strands.
- `ComplexGeometry.crackFan` through `submitEnergy`: on the rift's ground plane at
  radius times 1.02, the same radius as the existing ground crack emission and the
  permanent conversion disk, hot white center to violet 0x8C1AFF tips, peak alpha 0.90.
  It runs at detail scale distScale times 0.70 as the third builder (see the budget
  accounting below).
- Vertex accounting at full detail, worst case: shard fan 4 times 288 = 1152, inward
  tendrils 2 times at most 384 = 768, crack fan at the reduced detail scale at most
  about 450 (at most 8 cracks, 4 segments each, forks included). Worst case total near
  2370, inside the class M budget of 2500.

Complex particle layer:
- `DbaParticles.WISP_CURL` through `MaterialEmitters.Preset.fixed`, sourced from the
  same inward stream source as the plasma and darkness (rim points with velocity
  toward the heart), rate 30 per second with budget 40, inside the continuous cap.

Filler layer: unchanged, including the envelope behavior the task called out. The
shared `FxEnvelope.envelope` lifecycle (build 0.20, decay 0.35), the entrance ease, the
expansion split by destination flag, the tear open and shut shaping, and the sine pulse
(0.88 plus 0.12 times sin of age times 0.5) on the singularity heart layers are all
exactly as at HEAD. The darkness inward stream, the white and magenta tear seam, the
heart cluster, the boundary vapor, and the ground glass dust and plasma flecks tick
with their original rates, budgets, colors, and envelopes. No existing line was
removed or retuned.

## Verification

Targeted javac with JDK at ~/workspace/tools/jdk-25.0.5+8/bin/javac, sourcepath
src/main/java:src/client/java, compiling only the two owned files, private output
tags /tmp/dimsword-262 and /tmp/dimsword-263.

- Classpath cp262.txt: 4 errors total, all baseline kinds in files outside this slot
  (DbaItems.java line 535, protected access on CreativeModeTab Output, twice;
  DbaMenus.java line 16, private access on MenuType constructor and MenuSupplier,
  twice). Zero errors in either owned file, and both owned classes plus their render
  state classes were produced in the output directory.
- Classpath cp263.txt: 10 errors total, all baseline kinds in files outside this slot
  (DbaItems.java line 535 as above; DbaMenus.java line 16 as above; DbaHostileEntity,
  FriezaSoldierEntity, NamekianWarriorEntity, and PatrolRobotEntity symbol and swing
  signature errors from the 26.3 API shift). Zero errors in either owned file, and
  both owned classes were produced.
- `git diff --check` on both owned files: clean.
- Added lines em dash scan: zero.

## Foundation change requests

No blockers. Two non-blocking notes for the merge, filed instead of building anything
private:

1. crackFan orientation: `ComplexGeometry.crackFan` draws only in the XZ plane at
   local y 0.03. A crack fan on a vertical tear plane needs either a pose copy
   rotation by the caller (the pattern SkyCracks used with a camera billboard) or a
   toolkit variant that takes a plane basis. This rift has no synced facing field, so
   a vertical fan would have to billboard to the camera; the ground plane placement
   was chosen instead because it matches this renderer's existing ground crack
   emission and the real permanent disk. If Carter wants cracks on the vertical tear
   plane itself, that is a shared toolkit extension request, not an owner workaround.
2. Tendril gradient shape: `tendril` gradients run root to tip in one linear sweep.
   A center hot gradient (theme color at both tips, white at the crest) would suit
   slash bodies whose color stops are symmetric. The current build works around it
   honestly with three strands (main, white core, reversed rim), so this is a polish
   request only.

## Honest worse check

DimensionalSlash: this does not read worse than the particles only version. The
particles only version had no silhouette at all in flight; the strands give the cut a
torn, structured body that follows the exact plane, tilt, wobble, first person scale
in, and linger swelling the emission already used. Two real risks, both bounded: the
main strand gradient runs violet at one tip to magenta at the other, so the strand
alone is asymmetric, but the reversed rim strand and the unchanged symmetric three
stop plasma sampling keep the whole composition balanced; and the thick strand can
partly cover the white core plasma cluster at the belly, which is mitigated by the
core strand and the core cluster both sitting forward of the belly (forward offsets
0.22 and 0.30), so the cluster still reads in front of the main strand.

DimensionalWarpRift: this does not read worse than the particles only version either,
with one honest caveat. The shard fan is static geometry: the shards hold their
seeded directions for the rift's 24 tick life and only their alpha follows the tear
shaping. At 1.2 seconds of life, with the entrance ease and the shut shaping running,
that reads as spatial glass caught in the tear rather than as a frozen prop, and the
inward tendrils plus the unchanged inward streaming emission keep the motion read.
If the rift lifetime were ever lengthened, static shards would start to read worse
and would want the cycling treatment other owners gave falling shards; at the
current lifetime it is not a regression. The crack fan at reduced detail scale shows
6 to 8 cracks instead of the builder's nominal 9 to 11, a deliberate trade to keep
the three builder composition inside the class M vertex budget; on the marked disk
at radius up to about 4.5 the fan still reads as a glowing crack web, and the ground
glass dust and plasma flecks over the same disk are unchanged.

Neither effect keeps a round glow sprite as its body, and in neither file is PLASMA
emission the sole or dominant layer anymore: the slash body is the tendril strands,
and the rift reads from its shard fan, inward tendrils, and crack fan, with plasma
left as filler around them.
