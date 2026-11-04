# Complex Effects Redo, Phase 2 Report: TimeShatter

Owner slot: TimeShatter. Base HEAD 9178eac. File edited, and only
this one: src/client/java/com/dragonblockarcanedba/client/render/TimeShatterRenderer.java.
No entity, item, or shared foundation file was touched, so the
TimeShatterEntity, its 36 tick life, the synced SEED, the GLASS_DUST
gravity scale 0.1 suspension wiring from the no-shapes merge, every
WhisStaffItem spawn path, and the d74f13a destruction wiring are
exactly as at HEAD. Theme held throughout: glass dust, temporal
plasma, chromatic distortion, in the existing palette only. No damage
number, cooldown, cost, timing, mechanic, or synced field changed.
The pieces and the complex sprite stream were added to the existing
filler, never traded against it. No commits made.

## TimeShatter (class M)

All three layers sit on one shatter plane: the camera plane the
existing crack spoke dust and dial ghost streams already use. The
pose is billboarded to the camera inside submitPieces, following the
pattern already shipped in SkyCracksRenderer.

- Pieces layer, solid: a frozen fan of temporal glass shards through
  ComplexGeometry.submitSolid. Five ComplexGeometry.shardBurst calls
  inside one submit, one shard per spoke (count 1 each, 5 meshes in
  total, inside the 16 mesh shardBurst cap), each aimed along its own
  seeded in-plane direction with a 14 degree cone jitter, unit
  0.90 times the travel-scaled effect scale, so individual shards run
  about 0.27 to 1.44 times that scale across the standard three size
  classes. Root 0xFFFFFF to tip 0x65D8FF, faceted through the plain
  ki type, alpha 0.95 times the existing FxEnvelope (build 0.14,
  decay 0.50 over the 36 tick life).
- Frozen posing, deliberate: the five spoke angles come from a Random
  seeded by the entity synced seed (seed xor 0xFA17), the origins are
  all the effect center, and no posing input reads age. There is no
  rotation, no sway, no drift, and no per-frame re-roll. The only
  time variation is the envelope alpha and the size, which follows
  the same cubic travel curve the existing filler already used, and
  that curve saturates, so after the unfold the fan hangs still for
  the rest of the life. That is the time-stop read the task asked
  for: stillness over motion.
- Pieces layer, energy: one ComplexGeometry.crackFan call through
  ComplexGeometry.submitEnergy, on a copy of the billboarded pose
  rotated minus 90 degrees about X so the builder XZ plane becomes
  the shatter plane, exactly as SkyCracksRenderer orients its crack
  plane. Radius 1.80 times the travel-scaled scale, which is the old
  dial radius the ghost ring streams already sample. Hot 0xFFFFFF at
  the center to cool 0x65D8FF at the tips, peak alpha 0.92 through
  the shader life remap.
- Complex particle layer: SHARD_CHIP as the glass chip layer, via
  MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP, Vec3.ZERO,
  0.003f) through the renderer existing tick-guarded burst helper.
  The source is the existing crackSource, so chips spawn along the
  same eight crack spoke lines as the crack dust, in the same camera
  plane as the pieces, and ride that source slow inward velocity, so
  they hang with the suspended dust instead of spraying outward.
  Rate: 2 per emission tick, which is 2 per burst event (class M
  burst budget 30) and at most 40 per second at the 20 tick emission
  rate (the continuous cap), before the fade and distance scale
  reduce it further. Chip particle lifetime is 14 to 30 ticks per
  the shared SHARD_CHIP style, so the layer accumulates to a visible
  suspended scatter over the effect life rather than flashing per
  tick.
- Filler layer, unchanged: all ten existing material streams are
  verbatim, including every GLASS_DUST preset carrying
  withGravityScale(0.1f), the plasma and soul core accents, and the
  two offset ghost streams that carry the chromatic split. PLASMA
  was never the body here and is even less so now: the body is the
  shard fan plus the crack web, plasma and soul are a 3 and 2 count
  core accent inside a much larger glass composition.

## Budgets and rules check

- Vertex accounting at full detail: the fan is 5 shards at 288
  vertices each (1440). The crackFan is at most 11 cracks: main
  walks at most 528 vertices (11 cracks times 6 segments times 8)
  plus forks at most 264 (11 forks times 3 segments times 8), so
  792 worst case even if every crack forked. Combined worst case
  2232, inside the class M per-submit budget of 2500. Typical
  crackFan output is near 620, for a typical total near 2060.
- Zero drawColoredBox, drawCylinder, drawDisc, drawSphere, or
  drawHelix calls in the owned file (grep verified).
- Every solid piece goes through submitSolid; the crackFan energy
  piece goes through submitEnergy. No private geometry, emitter, or
  texture code was built.
- Alpha as life respected: 0.95 literal on the solid shards and
  0.92 peak on the shader cracks. Faintness is carried by color,
  never by a low constant alpha fed to a shader piece.
- Pieces and chips are seeded from the entity synced seed
  (state.seed, with its existing UUID fallback untouched), exactly
  as the existing emission is.
- Split source sets stay clean: the only edit is client side;
  DbaParticles (common) was only referenced, not modified.

## Verification

Targeted javac with JDK at ~/workspace/tools/jdk-25.0.5+8/bin/javac,
sourcepath src/main/java:src/client/java, compiling the one owned
file, output to private /tmp tags timeshatter-262 and
timeshatter-263, logs at /tmp/timeshatter-262.log and
/tmp/timeshatter-263.log. Both full logs were read before making
these claims:

- 26.2 classpath (cp262.txt): zero errors in the owned file. The
  only errors in the closure are the known baseline kinds in
  DbaItems.java (CreativeModeTab Output protected access, 2) and
  DbaMenus.java (MenuType private access, 2), 4 errors total.
- 26.3 classpath (cp263.txt): zero errors in the owned file.
  Baseline errors only: the same DbaItems and DbaMenus kinds plus
  the known entity kinds in DbaHostileEntity, FriezaSoldierEntity,
  NamekianWarriorEntity, and PatrolRobotEntity, 10 errors total.
- The owned class and both inner classes (ShatterRenderState,
  EmissionKey) were produced in both output trees. git diff --check
  on the owned file is clean, and an em dash scan of the owned file
  and this report is zero.

## Foundation change requests

No blocking gaps. Two optional, non-blocking notes for the merge:

1. ComplexGeometry.shardBurst fans shards in a cone around one axis
   only. A planar fan variant (spokes distributed in a caller plane)
   would serve shatter fans directly. Worked around here with one
   count 1 burst per spoke, which also keeps each spoke size class
   independently seeded, so this is a convenience, not a need.
2. ComplexGeometry.crackFan draws only in its local XZ plane.
   Billboard-plane effects need the pose copy plus minus 90 degree
   X rotation used here and in SkyCracksRenderer. A plane-parameter
   variant would be the shared fix if the merge wants one.

## Honest worse check

- Color split, healed by the pieces, not worsened: the previous
  pass noted a subtle color split in this effect, carried by the
  two offset ghost streams (cyan 0x65D8FF one way, near-white
  0xF4FEFF the other). The pieces add no new hue at all. Both
  builders run on one shared radial gradient using the split own
  two tones: near-white 0xFFFFFF at the heart to the ghost cyan
  0x65D8FF at the tips, in the shards (root to tip) and in the
  cracks (hot center to cool tips) alike. So where the filler
  presents the split as two separated offset copies, the pieces
  present the same two colors as one continuous gradient inside
  every single piece, which reads as chromatic distortion resolving
  into the glass itself rather than as two mismatched effects. The
  lilac tones (0xD8BFFF, 0xD9C2FF) stay only where they already
  were, in the filler dust and soul streams; keeping them out of
  the pieces is what stops the pieces from widening the split into
  a three-way one. The ghost streams themselves are unchanged, so
  the intentional split read is not erased, it is now anchored by
  a unified body.
- Plane divergence risk, low and pre-existing: the filler shard
  trajectory streams sample a world XY fan, while the pieces and
  the crack and ghost filler sit on the camera plane. At oblique
  viewing angles the world XY dust fan and the billboarded pieces
  can separate. That divergence exists at HEAD between the
  trajectory streams and the crack and ghost streams; the pieces
  join the majority plane (cracks plus ghost plus chips), so the
  net alignment is better than at HEAD, not worse.
- Sparse fan risk, low: five spokes is a fan, not a spray, and the
  per-spoke size class roll means one spoke can land in the chip
  class and read short. The rolls are seeded, so a given cast is
  consistent on every client and across frames, and the mixed
  lengths are the fractured read the builder is built for. The
  crackFan web and the chip scatter fill the gaps between spokes.
- Center cluster risk, low: all five shard roots sit at the effect
  center, over the plasma and soul core accents. Shard roots are
  the narrow end of the spindle (root ring at 0.55 girth, root
  point pulled back only 6 percent of length), so the overlap is a
  tight bright heart, not a blob, and the white root color matches
  the hot crack center beneath it.
- Unfold note: the fan size follows the travel curve, so during
  roughly the first third of the life the fan grows outward with
  the rest of the shatter. That is the one motion the pieces have,
  it is inherited from the filler own travel math rather than added
  for the pieces, and it saturates; a version frozen at full size
  from tick zero would pop in against a filler that visibly
  travels outward. First thing to check in game if Carter wants
  the fan fully static from the first frame; dropping the travel
  factor in submitPieces only is a one line change.

Nothing in this slot reads worse than the particles only version
or the pre-redo version in a way I could not fix inside the
standard, so nothing was held back.
