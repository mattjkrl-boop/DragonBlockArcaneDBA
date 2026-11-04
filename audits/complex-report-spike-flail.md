# Complex Effects Report: Spike Flail

Owner slot: Spike Flail. Base HEAD 9178eac. Files edited: only the two
owned renderers. No commits. Themes, colors, damage, cooldowns, costs,
timing, mechanics, and synced fields are unchanged from HEAD. Existing
material emission is untouched and remains as the filler layer in both
effects. The entity side destruction wiring from d74f13a is untouched,
and the physical flail head stays a model. No plain primitives were
added: zero drawColoredBox, drawCylinder, drawDisc, drawSphere, or
drawHelix calls in either file.

## Per effect

### SpikeFlailSpinRenderer (class M)

- Pieces layer: ComplexGeometry.tendril through
  ComplexGeometry.submitEnergy with ComplexTextures.TENDRIL, two
  strands in one submit. Both follow the sampled head path exactly:
  endpoints are the previous ball sample and the current ball sample
  from extractRenderState (angles at the synced angle minus 32
  degrees and at the synced angle, at the synced orbit radius),
  converted to local space by subtracting the render origin. The
  control point sits on the orbit arc midway between the samples, at
  the synced radius and the ball height, so the bezier bows along the
  real orbit instead of cutting its chord. No new path is invented.
  Main strand width 0.32, root color THEME_DUST 0xD8C9A3, tip
  0xFFF2CC, peak alpha 0.92. Echo strand width 0.17, root 0xEDE8DA,
  tip 0xF5EFE0, peak alpha 0.88. Both seeds derive from the entity
  seed (state.seed xor 0xA11C and 0xA22C). The geometry envelope
  mirrors the emission orbit envelope in each phase, without the
  distance scale (distScale is passed to the builder separately):
  channel envelope times fade while orbiting, fade times 0.45 during
  the release expansion and at the hit tick, and fade times 0.225 in
  the settle phase. Vertex estimate at distScale 1.0: 2 tendrils x
  384 = 768, inside the M budget of 2500.
- Complex particle layer: SPARK_STREAK through
  MaterialEmitters.Preset.fixed in three continuous emitters plus one
  burst, all at contacts as the audit names. Along the sampled head
  path (same ballPath source as the vapor and SPARK fillers): rate
  26 per second, budget 34. At the ground contact under the ball
  (same groundBall source as the ground dust): rate 15 per second,
  budget 22. At the release crack edge (same edge source as the edge
  dust, vapor, and plasma): rate 27 per second, budget 35, ticked
  with the crack envelope. Every continuous rate is inside the 40
  per second per emitter cap. In fireCrackBurst, a SPARK_STREAK burst
  over the same ground area as the existing SPARK burst: count
  round((12 + 8 x charge) x distScale), at most 20, inside the M
  burst budget of 30.
- Filler: unchanged (orbit vapor, SPARK, chain vapor, ground dust,
  edge dust, edge vapor, edge plasma, settle dust, and the crack
  burst of dust, vapor, SPARK, and physical block chunks). PLASMA is
  one edge filler among several and is not the dominant layer; the
  tendril arcs are the body of the orbit read.

### SpikeFlailSwingRenderer (class M)

- Pieces layer: ComplexGeometry.tendril through
  ComplexGeometry.submitEnergy with ComplexTextures.TENDRIL, three
  strands in one submit, chained along the swing arc exactly as
  sampled. The five trail samples come from the existing extraction
  (SpikeFlailSwingEntity.ballPos at the eased swing progress steps),
  converted to local space by subtracting the render state origin.
  Strand one runs sample 0 to sample 2 with sample 1 as control,
  width 0.30, root THEME_DUST 0xD8C9A3, tip 0xEDE8DA, peak alpha
  0.92. Strand two runs sample 2 to sample 4 (the ball) with sample
  3 as control, width 0.28, root 0xEDE8DA, tip 0xFFF6D8, peak alpha
  0.92, so its root color matches strand one at the shared middle
  sample. A thinner echo runs the whole arc, sample 0 to sample 4
  with sample 2 as control, width 0.15, root 0xEDE8DA, tip
  0xFFF6D8, peak alpha 0.88. Seeds derive from the entity seed
  (state.seed xor 0x5A11, 0x5B22, 0x5C33). The geometry envelope is
  the existing swing envelope without the distance scale: fade times
  (0.35 + 0.65 x open), with distScale passed to the builder
  separately. Vertex estimate at distScale 1.0: 3 tendrils x 384 =
  1152, inside the M budget of 2500.
- Complex particle layer: SPARK_STREAK through
  MaterialEmitters.Preset.fixed shed along the same trail path
  source as the vapor and SPARK fillers: rate 26 per second, budget
  34, inside the continuous cap.
- Filler: unchanged (trail vapor, SPARK, core plasma at the head,
  chain vapor, ground dust at the low arc points). The core plasma
  remains a small glint at the moving head only; the tendril body is
  the swing arc read, so PLASMA is not the sole or dominant layer.

## Verification

Targeted javac with JDK ~/workspace/tools/jdk-25.0.5+8/bin/javac,
sourcepath src/main/java:src/client/java, compiling both owned
files, private output tags /tmp/sf262 and /tmp/sf263, full logs read
at /tmp/javac-spike-flail-262.log and /tmp/javac-spike-flail-263.log.

- 26.2 classpath (cp262.txt): 4 errors total, zero in owned files.
  All four are the known baseline kinds elsewhere in the closure:
  DbaItems.java CreativeModeTab Output protected access (2) and
  DbaMenus.java MenuType private access (2). Class files for both
  owned renderers and their inner classes were produced.
- 26.3 classpath (cp263.txt): 10 errors total, zero in owned files.
  All ten are baseline kinds elsewhere in the closure: the same
  DbaItems (2) and DbaMenus (2) errors, plus DbaHostileEntity,
  PatrolRobotEntity, FriezaSoldierEntity, and NamekianWarriorEntity
  symbol and signature errors (6). Class files for both owned
  renderers and their inner classes were produced.
- git diff --check on both owned files: clean.
- Added lines em dash scan: zero.
- Split source sets stay clean: both edited files are client side
  only, and all new imports are client side (ComplexGeometry,
  ComplexTextures) or already used there.

## Foundation change requests

None. No foundation gaps were hit. SPARK_STREAK emission uses
MaterialEmitters.Preset.fixed as directed; no Preset.spark factory
is needed, and no private toolkit, emitter, or texture was built.

## Worse check

Spin: the honest risk is the short arc segment. The tendril spans
only the 32 degree sample behind the head, so at the starting orbit
radius its chord is about 1.5 blocks, and a 0.32 wide strand could
in principle read as a chunky ribbon stub attached to the ball
rather than as torn air. Three things keep it from reading worse
than the particles only version: the builder curl, pinch windows,
and tendril canvas dissolve break the silhouette up along that
short length; the strand gradient runs from the muted dust color at
the tail to near white only at the head, so the eye still lands on
the ball model; and the pre-existing vapor and spark emission along
the same path sits over and around the strand, so the piece reads
as structure inside the vapor, not as a replacement for it. The
orbit also repeats every frame with the head moving fast, so the
arc is perceived in motion as a trailing wake, which is the read
the no-shapes version lacked. Verdict: not worse.

Swing: the honest risk is doubling at the middle sample, where
strand one ends, strand two begins, and the echo passes nearby, so
the middle of the arc could read slightly denser than the ends.
That density peak sits at the arc belly, which is where a swing
reads fastest and where the existing emission already concentrates
its vapor, and the matching root and tip colors at the joint keep
it one continuous body rather than two pieces. Early in the swing
several trail samples collapse to the same start point; the builder
returns zero for chords under 0.05, so no degenerate geometry is
drawn in that window, and the strands grow in with the arc exactly
as the emission does. Verdict: not worse. Neither rebuild was found
to read worse than the particles only version or the pre-redo
version, so nothing is held back under merge gate 6.
