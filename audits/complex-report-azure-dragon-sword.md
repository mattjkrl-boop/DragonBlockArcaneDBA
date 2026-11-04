# Complex Effects Redo, Phase 2 Report: Azure Dragon Sword

Owner slot: Azure Dragon Sword (theme: tempest plasma, rain, mist, lightning plasma).
Base: HEAD 9178eac. Files edited: only the six assigned renderers under
src/client/java/com/dragonblockarcanedba/client/render/. No commits.
Themes, colors, damage numbers, cooldowns, costs, timing, mechanics, and
synced data fields are identical to HEAD. The d74f13a destruction wiring
was not touched. BoltRenderer and BoltEntity were not touched. No plain
primitives were added: zero drawColoredBox, drawCylinder, drawDisc,
drawSphere, or drawHelix calls in the slot. Every energy piece submits
through ComplexGeometry.submitEnergy with the TENDRIL canvas where the
builder samples one, and the one solid piece submits through
ComplexGeometry.submitSolid. Peak alphas on effect bodies are 0.85 to
0.95; faintness is never faked with a low constant alpha on a shader
piece. All pieces are seeded from the entity's synced render seed (or,
for the rush trail, the synced caster id key the wake maps already use).

## Per effect

### AzureLightningRenderer (class M, telegraph for the shared BoltEntity strike)

- Pieces layer: crackFan electric web in the warned disc through
  submitEnergy, plus tendrilFan of 3 small arcs through submitEnergy
  with ComplexTextures.TENDRIL during the telegraph.
- Web behavior: the crack radius crawls from 45 percent to 100 percent
  of its cap across the telegraph smoothstep, so the web spreads as the
  strike nears. The cap is 0.78 of the warned disc radius (see the worse
  check for why). The web stays drawn at full crawl through the
  afterglow at alpha 0.85, cooling with the disc fade.
- Arc behavior: arcs rise from the disc center at 65 degree spread and
  0.55 of the disc radius in length, reseeded every two ticks from the
  synced seed plus the tick bucket, so every client draws the same arc
  in the same window and the arcs jump like discharge.
- Complex particle layer: SPARK_STREAK through
  MaterialEmitters.Preset.fixed, rising velocity 0.28, continuous rate
  26 per second, emitter budget 40 per second, ticked only in the
  telegraph and scaled by telegraph progress.
- Filler: the existing warning, converge, column, and vapor plasma and
  vapor emitters are unchanged.
- Vertex estimate at distScale 1.0: crackFan about 950 worst case,
  tendrilFan 3 tendrils at 384 each is 1152, combined about 2100 of the
  class M 2500 budget.

### AzureRushTrailRenderer (class M)

- Pieces layer: a chain of three tendril calls in one submitEnergy
  with the TENDRIL canvas, laid along the wake centerline stations in
  local space (stations 0 to 5, 5 to 10, 10 to 15, each a bezier
  through its middle station). Width 0.42 times the trail scale,
  tapering 1.0, 0.92, 0.80 head to tail, halved in first person to
  match the emission narrowing. Colors keep the wake identity: near
  white cyan root, azure tip, green tip on a double rush.
- Complex particle layer: WISP_CURL through Preset.fixed, shed through
  the existing shell volume source, rate 26 per second, budget 40.
- Filler: the plasma core, vapor shell, rain, and shock lip plasma
  emitters are unchanged, and the new emitter ticks in the same loop
  with the same envelope.
- Vertex estimate: 3 tendrils at 384 each is 1152 of 2500.

### AzureSonicQuakeRenderer (class L)

- Pieces layer, two builders: fracturePlates through submitSolid
  (azure tinted stone gradient, hot 0xA8C8D2 to dark 0x46545C, alpha
  0.95) plus crackFan through submitEnergy (white hot center to
  tempest plasma 0x4DF2FF tips, alpha 0.90). Both radii grow with the
  current wave radius (plates at the wave radius, cracks at 0.92 of
  it), so nothing renders ahead of the crest. Plate lift is 0.30,
  easing down as the wave progresses.
- Complex particle layer: SHARD_CHIP in two forms. A one-shot
  MaterialEmitters.burst of 30 at the epicenter, guarded by a per
  emission flag so it fires once per quake (class L burst budget 60),
  plus a continuous crest emitter through Preset.fixed at rate 20 per
  second, budget 40.
- Filler: the crest dust, vapor, plasma, ground block chunks, crit
  sparks, and epicenter dust emitters are unchanged. The entity's
  server side debris field is untouched; these pieces are the fast
  near-field fragments.
- Vertex estimate: plates about 480, crackFan about 950, combined
  about 1450 of the class L 6000 budget. The two builder rule is met;
  neither builder is run past its shipped count budget.

### AzureStormRenderer (class M)

- Pieces layer: two tendrilFan calls in one submitEnergy with the
  TENDRIL canvas, 2 strands each, standing on opposite sides of the
  eye (origins at plus 0.34 and minus 0.30 of the radius), tilted with
  the swirl, length 0.62 of the storm radius, width 0.34, vapor cyan
  root to pale cyan tip, alpha 0.88. The seed bucket rolls every eight
  ticks so the lashes re-lash instead of hanging frozen.
- Complex particle layer: ASH_FLAKE through Preset.fixed at rate 24
  per second and WISP_CURL through Preset.fixed at rate 20 per second,
  both on the existing cylinder swirl sources, budgets 40 each.
- Filler: mist, vapor, dust, eye vapor and plasma, and the two arc
  plasma emitters are unchanged. The storm's bolts stay BoltEntity
  spawns from the shared system; this renderer presents the storm
  volume only.
- Vertex estimate: 4 tendrils at 384 each is 1536 of 2500.

### AzureTempestChannelRenderer (class M)

- Pieces layer: three tendril calls in one submitEnergy with the
  TENDRIL canvas, with helical control points as assigned. Each strand
  starts at the tunnel rim (1.12 of the tunnel radius, y 0.15), its
  control point steps about 60 degrees around the axis at 0.55 of the
  radius and 0.52 of the height, and it ends near the axis (0.16 of
  the radius) at 0.94 of the height, so the bezier itself spirals
  inward. The triad rotates with channel age at 0.06 radians per tick,
  the same direction as the filler swirl velocities. Width grows with
  charge, 0.26 plus 0.18 times charge. Root tempest plasma 0x4DF2FF to
  white tip, alpha 0.90.
- Complex particle layer: SPARK_STREAK through Preset.fixed, rising
  through the channel volume on the existing rising source, rate 26
  per second, budget 40, ticked with the main emitter group so it
  follows the same intro, outro, and charge envelope.
- Filler: vapor, mist, plasma, ground dust, and the two charge gated
  arc plasma emitters are unchanged.
- Vertex estimate: 3 tendrils at 384 each is 1152 of 2500.

### AzureTornadoRenderer (class L)

- Pieces layer: tendrilFan at full fan budget through submitEnergy
  with the TENDRIL canvas: 6 strands, the fan maximum, each at the
  full 24 spine samples at distScale 1.0. One fan call, origin at the
  local base center, axis straight up, spread 30 degrees. The spread
  matches the funnel profile ratio (top radius over height is the
  same for giant and mini), and the strand length is 1.12 of the
  total height, the funnel diagonal, so strands hug the column and
  reach the widening top. Width 0.60 giant, 0.36 mini, scaled by the
  entity scale clamped to 0.5 to 1.5. Vapor root 0xBFE3F2 to tempest
  plasma tip 0x4DF2FF, alpha 0.90, shader treatment on the energy
  pieces as class L requires.
- Rotation: the fan is rotated around the local Y axis by age times
  3 degrees inside the piece callback, on a copy of the submitted
  pose built the same way the existing layer renderers copy a pose,
  so the strands swirl with the flow. The rotation uses a Matrix4f
  wrapping the Axis rotation, the DebrisRenderer pattern, because
  PoseStack.mulPose(Quaternionf) does not exist on the 26.3 classpath
  (see verification).
- Complex particle layer: SHARD_CHIP through Preset.fixed orbiting in
  the lower funnel source (rate 28 giant, 20 mini, budget 40) and
  ASH_FLAKE through Preset.fixed through the whole funnel volume
  (rate 24, budget 40).
- Filler: vapor, mist, rain, ground dust, ground block chunks, and
  plasma emitters are unchanged.
- Vertex estimate: 6 tendrils at 384 each is 2304 of 6000. The fan is
  at its shipped strand and sample maximum; the class L full budget
  reading for this slot is the single builder run at full count, as
  assigned.

## Budgets summary

- No submit exceeds its class vertex budget (largest is the tornado
  at about 2304 of 6000; the tightest is the lightning at about 2100
  of 2500).
- Every complex sprite emitter runs at 20 to 28 per second with its
  budget capped at 40 per second, the standard continuous cap. The
  only burst in the slot is the sonic quake chip burst of 30, under
  the class L burst budget of 60.
- No class M or L effect in the slot is particles only, and none
  relies on PLASMA material emission as its body: each now has a
  structured pieces body with the material emission kept as filler
  around it, at its original rates, per the audit rule that filler is
  never traded against the new layers.

## Verification

Targeted javac with JDK ~/workspace/tools/jdk-25.0.5+8/bin/javac,
sourcepath src/main/java:src/client/java, compiling the six owned
files, private output dirs /tmp/azure-slot-262 and /tmp/azure-slot-263.

- Baseline before edits (same command): 26.2 classpath 4 errors, all
  outside the slot (DbaItems CreativeModeTab Output protected access
  times 2, DbaMenus MenuType private access times 2). 26.3 classpath
  10 errors, all outside the slot (the same 4 plus 5 cannot find
  symbol and 1 swing method error across DbaHostileEntity,
  FriezaSoldierEntity, NamekianWarriorEntity, and PatrolRobotEntity).
- After edits, 26.2 classpath (cp262.txt): 4 errors, the exact
  baseline set, zero errors in owned files.
- After edits, 26.3 classpath (cp263.txt): 10 errors, the exact
  baseline set, zero errors in owned files. One owned error appeared
  on the first 26.3 run (AzureTornadoRenderer mulPose(Quaternionf))
  and was fixed with the Matrix4f rotation pattern above; the re-run
  is clean.
- git diff --check on the six owned files: clean. Added lines em dash
  scan across the working tree diff: zero.

## Foundation change requests

No private toolkit was built and no foundation file was edited. Three
requests for the shared toolkit, none blocking:

1. crackFan radius is a walk length budget, not a hard disc bound.
   Each step is 0.60 to 1.35 times the nominal segment length, so a
   crack can overshoot the nominal radius, and a fork adds more. For
   a telegraph whose drawn size must equal the hit size, I scaled the
   fan to 0.78 of the disc in the owner file. Request: document the
   overshoot factor in the builder javadoc, or add a clamped variant
   for warning discs.
2. tendril takes a single quadratic bezier, so a polyline path (the
   rush wake has 16 stations) must be chained from several tendrils
   that share endpoints. Request: a tendrilPath builder that takes a
   point list and builds one continuous strand, so chained joints and
   their pinch windows do not segment the strand.
3. There is no azimuth or rotation parameter on tendrilFan, so a fan
   that must swirl (the tornado) needs the pose copy and rotate
   pattern inside the piece callback. It works, but every owner who
   needs it will reinvent it. Request: document the pattern in the
   ComplexGeometry javadoc, or accept an optional fan azimuth.

## Worse check (honest)

Overall: no effect in this slot reads worse than its particles only
version in my judgment, and none reads worse than the pre-redo
version. Each effect previously had no structured body at all; each
now has one in its own theme colors, with the old emission intact
around it. The specific risks I cannot fully settle without an in
game look, stated plainly:

- AzureLightning telegraph boundary (the known sensitive point). The
  particle warning disc still marks the exact hit radius, and the web
  is capped well inside it: typical crack endpoints land near 0.76 of
  the disc radius. The worst case seed roll (every step at the 1.35
  maximum, near straight, plus a long fork) can poke a crack tip a
  few percent past the disc edge. I judged a rare few percent tip
  overshoot acceptable against shrinking the web further, which would
  make the web read as a small inner scribble unrelated to the warned
  area, but the merge should know the trade was made knowingly. The
  geometry blink is deliberately gentler than the emission blink
  (0.88 floor instead of 0.72) so the boundary never strobes hard.
  The center arc fan is the other watch point: three arcs from one
  center point can read as a small fountain at the disc center. Their
  spread and short length keep them low, and they reseed like
  discharge, but if they read wrong in game the fix inside the
  standard is fewer, shorter arcs, not removing the web.
- AzureRushTrail: the three chained tendrils share endpoints, and the
  builder's pinch windows can land near a joint, so the wake strand
  may read as three linked strands instead of one continuous strand.
  The tapering widths make the joints read as strand breaks, which
  suits a torn wake, but it is a segmentation risk (see change
  request 2).
- AzureSonicQuake: the plates and cracks grow by scaling a fixed seed
  pattern with the wave radius, so early frames show the same pattern
  small rather than new breaks appearing at the crest. The alternative
  (full size plates from tick zero) would draw broken ground ahead of
  the wave the damage follows, which reads worse and lies about the
  hit area, so the growing pattern stays.
- AzureStorm: the lash seed rolls every eight ticks, so strands change
  shape in steps rather than morphing. At 0.4 second steps this reads
  as lashing, but it is a step, not a glide. The two fixed fan bases
  also cluster the strands at two spots in a large volume; the filler
  weather carries the rest of the volume, as it did before.
- AzureTempestChannel: at zero charge the tendrils are at their
  thinnest and the shared envelope is at its lowest, so the pieces are
  faint at channel start by design, matching the filler fade-in. No
  separate risk beyond that.
- AzureTornado: the strands rotate at 3 degrees per tick (one
  revolution in six seconds), slower than the tangential particle
  velocities imply, so the pieces lag the filler swirl while turning
  the same direction. All six strands also share the base center
  point, so the ground center has a knot where they cross; the funnel
  base and the ground dust sit on the same spot and mask most of it.

If the merge's in game check finds any one of these reading worse
than the particles only state, the lightning arc fan and the storm
lash step are the two I would revert first; the rest I would keep.
