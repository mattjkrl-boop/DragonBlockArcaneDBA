# Scope 2 Report: TimeShatter (Agent A7)

Files owned and edited: entity/TimeShatterEntity.java and
client/render/TimeShatterRenderer.java. Nothing else touched, nothing
committed. item/WhisStaffItem.java was read only, as a caller.
Measured at HEAD ef5ad02 plus the other scope agents' uncommitted
work already in the tree (Lava Sword, Evil Spear dread), which this
report does not touch.

## The three defects and their fixes

1. Eighteen tick life. TimeShatterEntity held maxLifetime and
   lifetime as plain ints of 18, so the whole burst, including a
   fade-out that only started at progress 0.5, was compressed into
   0.9 seconds. Fix: both fields raised together to 36, the value
   the coordinating pass settled on. The renderer already read
   getMaxLifetime() for its progress curve, so no other file needed
   to change. WhisStaffItem's three spawn sites (lines 155, 199,
   260) all call the unchanged (Level, LivingEntity, Vec3, float)
   constructor and compile untouched (proven in the closure below).
   The entity still deals no damage and drives no mechanics of its
   own; the Whis Staff reversal timing lives in the item and is
   byte-identical.

2. No fade-in. The old renderer's fade term was exit-only: exitT
   stayed 0 until progress passed 0.5, so alpha sat at full strength
   from the first frame and only the scale eased. Fix: alpha now
   runs on the shared envelope,
   FxEnvelope.envelope(age, maxLifetime, 0.14f, 0.50f). The 0.14
   build fraction is a smoothstep over the first 5 ticks of the 36
   tick life; the 0.50 decay fraction reproduces the old exit curve
   exactly (smoothstep symmetry makes the old 1 minus smoothstep
   exit identical to the envelope's out term). Hold in between.
   Nothing pops in and nothing vanishes at visible alpha.

3. Correlated random streams between layers. The old submit() ran
   two submitCustomGeometry lambdas and each constructed its own
   new Random(state.seed), so the shard layer restarted the exact
   stream the spoke layer had consumed: shard angles, speeds, sizes,
   and rotations reused the spokes' opening draws in order, and
   state.seed itself was only the entity UUID's most significant
   bits. Fix: the entity gained a synced SEED field on the
   BoltEntity pattern (EntityDataAccessor<Integer>, defined with
   default 0, set from level.getRandom().nextInt() inside the spawn
   constructor so no constructor signature changed, getSeed()
   accessor, and Bolt-style Seed persistence in
   addAdditionalSaveData and readAdditionalSaveData). The renderer
   extracts it into the render state, with a fallback for the frames
   before the synced value lands: the entity UUID hash, itself
   synced state, never a local random. Each layer now derives an
   independent stream from that one seed: dial bands at seed xor
   0xD1A1 and 0xD1A2, spoke placement from a BespokeGeometry.Rng at
   seed xor 0x5907E with per-spoke builder seeds at seed + 101 and
   seed + 202 plus 7919 per index, shard placement from an Rng at
   seed xor 0x51A9D with per-shard builder seeds at seed + 303, 404,
   and 505 plus 7919 per index, core domes at seed xor 0xC09E
   through 0xC0A1. Layers are decorrelated, frame-stable, and
   identical on every client.

## Geometry conversion

The renderer's 4 local vertex-emitting helpers are deleted and the
composition is rebuilt only on BespokeGeometry, identity kept
(time dial plus shattering shards, same colors, same placement
math, same rotation rates):

- Dial: two brokenVortexBand calls on a pose turned 90 degrees
  about X inside the existing camera-billboard block, laying the
  band plane into the billboard's XY plane. Bright band
  (0.95, 0.98, 1.0) at the old alpha 0.85 carries the dial color;
  a narrower, dimmer band (0.60, 0.90, 1.0) at 0.93 radius carries
  the old alternating segments' cooler tone. Twist is negative
  radians of age times 25 because the X turn mirrors the band's
  angle direction; the dial keeps spinning the old way. The bands'
  seeded column gaps are the splintering.
- Spokes: 8 branch ribbons from the center, tips placed with the
  old math (even spacing plus up to 0.2 radian jitter, length
  1.5 to 2.5 times scale, z jitter 0.4 times scale, width shrinking
  with progress as before). Two passes per spoke: cyan body
  (0.50, 0.85, 1.0) at alpha 0.70 and a thinner near-white core
  (0.95, 0.98, 1.0) at alpha 0.90, standing in for the old
  two-tone lerp and white core strip.
- Shards: 16, placed with the old math verbatim (angle, speed
  1.4 to 3.2, travel times speed times 2.4 times scale, y and z
  jitter, size 0.22 to 0.40 shrinking 70 percent over life, tumble
  30 to 80 degrees per tick, the same mod-3 iridescent palette).
  Each shard is a shardFan cluster at radius 1.8 times shardSize
  in its theme color plus a white core fan at 0.75 times, on pose
  copies tumbled by the shard's rotation and a per-shard tilt.
  The wake is a tornRibbon rooted at the shard and aimed back
  toward the center with a quaternion rotationTo, so the ribbon's
  bright root sits at the shard and dies inward, the old wake's
  gradient direction, at alpha 0.50.
- Core: a displacedDome pair, one call flipped 180 degrees about
  X, at 0.9 times the old core size in a blended celestial tone
  (0.80, 0.93, 1.0), plus a nested brighter pair at half size in
  (0.98, 0.98, 1.0) as the hot heart. The whole core spins at the
  old 40 degrees per tick on a Y-rotated pose. This is the first
  pass's shipped dome-pair workaround for the deferred octahedron
  builder, as the scope 2 audit prescribed.
- Travel (cubic ease-out), the scale curve (0.55 plus travel times
  0.85), the billboard treatment of the dial layer, the dual
  shouldRender overloads, and the render state shape are carried
  over. Segment counts step down with LayeredRender.distScale,
  which the old renderer did not use at all.

## Before and after counts

Entity:
- maxLifetime and lifetime: 18 to 36 (both fields, together)
- Synced data fields: 1 (SCALE) to 2 (SCALE, SEED)
- Constructor signatures changed: 0

Renderer (grep verified against HEAD):
- Local vertex-emitting helpers: 4 to 0 (drawSplinteringDial,
  drawSpoke, drawGlassPrismShard, drawOctahedralCore)
- Direct addVertex lines: 32 to 0
- Trig lines: 20 to 4 (the 4 remaining are placement math only:
  spoke tip and shard position cos and sin)
- java.util.Random constructions: 2 to 0
- BespokeGeometry call lines: 0 to 13 (11 builder calls plus the
  2 per-layer Rng streams)
- Vanilla ParticleTypes, DustParticleOptions, SoundEvents in both
  files: 0 before, 0 after (the pair was already clean on
  particles, sounds, and damage; it is a pure visual burst)

## Verification

Targeted javac, JDK 25, sourcepath over both source sets,
compiling TimeShatterEntity, TimeShatterRenderer, and
WhisStaffItem together so the caller closure is proven:

- 26.2 (cp262.txt): 4 errors total, all pre-existing baseline
  kinds in files this task never opened: DbaItems line 535
  (CreativeModeTab Output protected access, 2) and DbaMenus
  line 16 (MenuType private access, 2). Zero errors in
  TimeShatterEntity, TimeShatterRenderer, or WhisStaffItem;
  class files emitted for all three.
- 26.3 (cp263.txt): 10 errors total, all in the known 26.3
  baseline files: DbaItems (2), DbaMenus (2), DbaHostileEntity
  (2), PatrolRobotEntity (2), FriezaSoldierEntity (1),
  NamekianWarriorEntity (1). Zero errors in the two owned files
  or WhisStaffItem; class files emitted for all three.
- Em dash scan of both owned files: 0.

This is the javac audit method. No Gradle build was run or is
claimed.

## Honest worse check

Nothing reverted. The real trades, stated plainly:

- The core is rounder. A noise-displaced dome pair has no flat
  facets, so the diamond read of the old octahedron softens. This
  is the standing deferred octahedron builder gap; the dome pair
  is the workaround every converted effect in the first pass
  shipped (Whis Staff jewels included).
- Each shard is now a small fan of 5 to 9 mini shards instead of
  one clean prism triangle with a white core. It reads as a glass
  spray cluster at each of the 16 flight positions rather than 16
  distinct tumbling prisms, and it emits more vertices per frame
  than the old draw (distance scaling steps the fans down).
- The spokes lost their smooth bow and their along-length color
  lerp. Branch is jagged by design and single-color per call; the
  two-pass cyan and white approximation keeps the two-tone read
  but not the gradient.
- The dial's splinter gaps no longer pry open as progress grows
  (the old gap term scaled with progress). The broken bands'
  seeded gaps are fixed at spawn; the splinter read is carried by
  the gaps, the radius jitter, and the shrinking band width.
- The wake's inner end dies to about 45 percent of its root alpha
  (tornRibbon's fixed profile) instead of fully to zero as the old
  spoke-drawn wake did. At the wake's 0.50 alpha this is a faint
  residue, not a hard edge.
- The burst now lingers twice as long on screen and the shards
  take twice as long to reach full spread, since travel still
  keys off progress. That is the assigned lifetime fix working as
  directed, but it changes the feel of a Temporal Shatter impact
  from a snap to a bloom; the coordinating pass picked 36 with
  that known.

## Shared change requests

None new. The conversion used only the existing toolkit; the one
standing gap it leans on, a true octahedron or gem builder, is
already on the coordinating pass's deferred list from the first
pass, and the dome pair stands in here as it did there.
