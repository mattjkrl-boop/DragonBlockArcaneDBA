# Attack Upgrade Pass, Phase 2 Owner Report: Sickle of Sorrow and Power Pole

Owner slot: Sickle of Sorrow (audit section 14) and Power Pole (audit
section 16). Base HEAD ccfebf2, no commits made. Specs implemented from
audits/attack-complexity-audit.md and audits/attack-foundation-addendum.md.
Files changed, exactly six: SorrowSlashRenderer, DimensionalRiftRenderer,
DimensionalRiftEntity, PowerPoleImpactRenderer, PowerPoleExtensionRenderer,
PowerPoleWhirlwindRenderer. Foundation files untouched. No damage, cooldown,
cost, timing, mechanic, or synced field changed anywhere; the one entity
edit adds a presentation spawn on an existing beat.

## Sickle of Sorrow

### SorrowSlashRenderer: tendrils 2 to 4, shards 0 to 5, grounded crack fan 0 to 1

- Tendrils: the crest strand (0.50 width, violet to cyan-white) and the
  soul strand (0.28, trailing 0.40 behind) are unchanged. Added a second
  pair inside the same submitEnergy call: a forward echo of the crest
  pushed 0.38 of scale toward the travel direction (0.38 width, violet
  0x9900E6 to 0xB84DFF) and a deep trailing strand 0.78 of scale behind
  (0.20 width, 0x9900E6 to deep violet 0x3D0A66). Each new strand has its
  own seed salt (0xD33, 0xD44 against the pair's 0xD11, 0xD22), so the
  builder's seeded curl phases sit apart, as specced. Piece count:
  tendrils 2 to 4; worst case vertices in this submit 768 to 1536, inside
  the class M budget of 2500.
- Shard burst: one new submitSolid call, ComplexGeometry.shardBurst of 5
  sorrow shards fanned in the arc plane (cone axis is the travel
  direction, spread 150 degrees, unit 0.55 of scale, origin lifted 0.15
  of the arc half height). Palette is violet crystal, root 0x3D0A66 to
  tip 0xB84DFF, per the audit spec's submitSolid call: energy crystal,
  the same family treatment as the curse glass elsewhere. One shard is
  288 vertices; the burst is 1440 worst case in its own submit.
- Crack fan: one new submitEnergy crackFan at the arc's low point, only
  when grounded. A read-only client check (groundCrackPoint) scans down
  up to 4 blocks from the low point (center minus 0.9 of the arc half
  height along the synced up axis) for the first solid top and accepts
  it when the low point sits between 0.25 sunk and 2.5 blocks above it;
  the fan then draws on a copy of the pose moved to that ground point,
  radius 1.15 of scale, pale violet-white 0xF5EEFF to violet 0x9900E6.
  A slash flying high draws no fan. Worst case about 550 vertices in its
  own submit. See change request 1 for the branch count caveat.
- Emission rig, lifecycle fade, tilt math, and the impact burst are
  byte-identical to before.

### DimensionalRiftEntity: second authored fragment beat at peak

- At tick 20 (EXPAND_TICKS, expansion complete, radius at the full
  15.0), the entity fires one more FragmentEntity.burst at the peak
  tear height: tearY by the tick 2 formula at the current radius,
  min(8, 2.5 + 15 x 0.4) x 0.5 = 4.0. The call follows the tick 2 call's
  exact pattern: same helper, owner (the rift's caster), axis up,
  spread 100 degrees, unit 0.80, palette root 0x24053D to tip 0xB84DFF,
  speeds 0.10 to 0.30, gravity scale 0.5, life 70 ticks, rest 8 ticks.
  Count is 4 fragments. The seed is the tick 2 burst seed family plus a
  beat salt (0xBEA7), so the four silhouettes are fresh draws, not clones
  of the first beat's five.
- Fragments per rift: 5 to 9 total across the two beats, inside the
  foundation caps (64 per call, 256 alive in the area box). Rift growth
  curve (currentRadius and the EXPAND_TICKS and COLLAPSE_TICK beats),
  the debris field (spawnDebrisField untouched, its density-scaled count
  unchanged), fragment lifetimes, damage beats, pull, and sounds are all
  unchanged. See change request 3 for the wording interpretation.

### DimensionalRiftRenderer: tendrils 3 to 4

- The inward tendril loop runs 4 strands instead of 3; the fourth uses
  the same orbit phase formula (orbit plus 3 golden-angle steps, rim
  height 0.40 plus 3 x 0.24 of height), the same heart target, widths,
  and palette. Vertex accounting at the renderer's reduced detail scale:
  816 to 1088, inside the class M budget of 2500. Emission rigs
  untouched.

## Power Pole

### PowerPoleImpactRenderer: rebuilt pieces layer

Before: one crackFan carried the whole hit (about 550 to 620 vertices),
plus the unchanged sprite emission (33 emit calls in the rig and burst).

After, sprite counts untouched, pieces in three submits:

- Crack fans: the original fan is unchanged (strike plane pose, radius
  scale x (0.70 plus 0.60 shock progress), 0xFFF7D6 to 0xE32636). A
  second fan joins it in the same submitEnergy call at half radius on a
  pose mirrored across an in-plane axis (strike rotation, then a PI
  rotation about the local X), so its forks lean against the first
  fan's forks instead of echoing them, at 0.90 alpha with its own seed.
  See change request 4.
- Fracture plates: one new submitSolidLit call, fracturePlates at the
  same hit radius on the same strike plane rotation, lit by the render
  state's packed lightCoords. Form pass under Carter's realism bar:
  lift is low (0.06 of scale, about 0.13 blocks at the default scale
  2.2) so the plates hug the surface and tilt like slabs shifted by the
  blow instead of popping up like tiles, and the palette is a muted,
  close-toned gray-brown earth (0xA79B84 center to 0x4E463A rim),
  because real broken ground does not shade like a designed gradient.
  The builder emits 6 to 10 plates by distance (about 290 to 480
  vertices). See change request 2 for the plate count caveat.
- Stone chips: one new submitSolidLit call, shardBurst of 5 chips at
  the contact point, cone along the strike direction (spread 70
  degrees, unit 0.28 of scale), lit path, tight stone palette (root
  0x7A6C52 to tip 0x96876A, kept close so the chips do not shade like
  crystals). 1440 vertices worst case in its own submit.
- No new physical piece sits on the emissive path: plates and chips are
  submitSolidLit only; the fans stay energy, as before.

### PowerPoleExtensionRenderer: tendrils 2 to 3 braided, shaft streak shed

- Third tendril in the same submitEnergy call: starts left and high of
  the shaft (right minus 0.10, up plus 0.10, first-person scaled), ends
  right and low (right plus 0.06, up minus 0.10), control pushed left
  (right minus 0.16) and slightly up, so it crosses the shaft diagonally
  against the first strand's up bow and the second strand's right bow;
  width 0.22, gold 0xFFC46B to crimson 0xD91414, own seed salt. With the
  builder's per-seed curl phases, the three strands wrap the shaft as a
  braid instead of lying in one plane. Tendril vertices 768 to 1152
  worst case.
- SPARK_STREAK shed: a second streak emitter joins the rig, sampling
  the shaft segment (t 0.05 to 1.0) at stream speed 0.55, the same
  forward speed the plasma stream uses, at 26 per second with a budget
  of 40. The tip streak emitter is unchanged. Extension length, timing
  (ease out 3, hold, retract from tick 9, gone at 12), and lifecycle
  math are untouched.

### PowerPoleWhirlwindRenderer: tendrilFan strands 5 to 7

- The tendrilFan count goes 5 to 7; seed, spread (cone angle), length,
  width 0.34, palette, envelope, and distScale are all identical, and
  the spin (age x 65 degrees, applied identically to the fan pose and
  the filler cone phase) is untouched, per spec: no spin speed was
  changed. Worst case vertices in this submit rise from about 1920 to
  about 2688 at distScale 1. See the worse check.

## Verification

Targeted javac, JDK 25, sourcepath src/main/java:src/client/java, over
the six owned files, FULL logs read (no summaries):

- cp262 (final code): 4 errors, all standing baseline closure kinds in
  files this slot does not own (DbaItems x2, DbaMenus x2). Zero errors
  in owned files.
- cp263 (final code): 10 errors, all standing baseline closure kinds in
  files this slot does not own (DbaItems x2, DbaMenus x2,
  DbaHostileEntity x2, PatrolRobotEntity x2, FriezaSoldierEntity x1,
  NamekianWarriorEntity x1). Zero errors in owned files.
- git diff --check clean on the six owned files. Byte-level em dash
  scan on added lines in the six owned files: zero. Diff scope is
  exactly the six owned files (209 insertions, 33 deletions, the
  deletions being comment rewrites). No commits. No Gradle build run
  or claimed; the in-game look is unverified until Carter's PC test.

## Change requests (foundation is read-only for this slot)

1. crackFan branch count: the audit specs a crackFan of 3 short
   branches for the Sorrow Slash, but crackFan has no count parameter;
   it always walks 6 to 11 cracks (segments of 9 by distance, plus a
   seeded 0 to 2). Implemented as one crackFan call at a short radius
   (1.15 of scale), which keeps the fan small and short, but the crack
   count is the builder's, not 3. Foundation fix if the merge wants the
   exact read: a crack-count overload on crackFan.
2. fracturePlates plate count: the audit specces a ring of 6 to 8
   plates for the Power Pole impact, but the builder's plate count is
   distance driven (segments of 10, minimum 6), so it emits 6 to 8
   plates at normal viewing distances and 10 at point blank. No count
   parameter exists. Foundation fix if wanted: a plate count argument
   or a cap on fracturePlates.
3. Dimensional Rift beat wording: the audit says "4 more FragmentEntity
   bursts at tearY peak". Read as one more burst call carrying 4 more
   fragments (the entity pass flagged "a second authored burst beat",
   singular), implemented exactly in the tick 2 call's pattern. If the
   merge reads it as four separate burst calls, that is a one-line
   count and call change in DimensionalRiftEntity; flagged rather than
   silently multiplied to 20 fragments.
4. crackFan fork direction: "reversed forks" has no builder parameter.
   Implemented as a mirrored pose for the second impact fan (strike
   rotation plus a PI rotation about an in-plane axis), which reverses
   the fork lean relative to the first fan. The mirrored fan sits 0.06
   blocks across the strike plane from the first (its 0.03 local lift
   flips sign); the quads are double-sided and the planes do not
   z-fight. Noted so the merge judges it knowingly.

## Honest worse check

- Whirlwind vertex budget: seven strands at full sampling are about
  2688 vertices in one submit at distScale 1, over the nominal class M
  figure of 2500, back inside it by distScale 0.93. The count is
  implemented exactly as specced rather than silently trimmed, and the
  overage is point-blank only, but it is an overage and the merge
  should know. Separately, seven strands at the untouched spin rate
  will read busier than five; the audit accepted that trade and left
  strobing to the PC test, and no spin value was touched here.
- Impact plates away from the ground: the impact's pieces have always
  lived on the strike plane, including the pre-existing crackFan, so a
  hit landed on a wall or a mid-air target already drew a flat fan
  there. The new plates and chips are solid and lit, so on a mid-air
  hit they will read as a floating fractured disc for the impact's 14
  ticks where the old fan read as flat energy. That is more visible,
  not less. Ground strikes, the common case and the one Carter's
  realism bar describes, are the read the form was tuned for (low lift,
  muted earth). Nothing reverted on this; flagged for the PC test.
- Impact plate presence: the lift was deliberately lowered for
  physical believability after Carter's quality note (0.10 to 0.06 of
  scale). If the hit reads too quiet in game, lift is a one-value call
  for the merge, not a rebuild.
- Sorrow Slash crack fan: it appears under flying slashes that skim
  within 2.5 blocks of solid ground, not only landed ones. A slash
  reaping along a rooftop or a cliff face edge will score cracks that
  track it. That follows the spec's "when grounded" wording, and the
  fan is short (1.15 of scale), but the cadence in game is a PC-test
  judgment.
- Dimensional Rift: the peak beat's four fragments spawn while the
  first beat's five may still be alive (life 70 from tick 2), so a rift
  briefly carries 9 fragments. Inside caps, and the grown tear reads
  fuller, which is the point; no fragment from either beat tracks the
  tear as it collapses, the same accepted trade as the entity pass.
- Extension braid: the crossing strand's braid read depends on the
  builder's seeded curl adding wrap on top of the crossed control
  points; from the code alone it is a diagonal cross, and whether it
  reads as a true braid in motion is unverified until the PC test.
- Nothing was reverted. Every change either matched its spec exactly
  or is flagged above with the reason.
