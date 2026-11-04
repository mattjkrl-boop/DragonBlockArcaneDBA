# Attack Upgrade Pass, Phase 2 Owner Report: Whis Staff and Bansho Fan

Owner slot: Whis Staff (Temporal Rift, Time Shatter) and Bansho Fan
(Shockwave, Gale, Wind Projectile, Cyclone). Specs:
audits/attack-complexity-audit.md sections 17 and 18, builder contract
audits/attack-foundation-addendum.md. HEAD ccfebf2, no commits made.
Six files edited, 330 insertions, 31 deletions:

- src/client/java/com/dragonblockarcanedba/client/render/TemporalRiftRenderer.java
- src/main/java/com/dragonblockarcanedba/entity/TemporalRiftEntity.java
- src/client/java/com/dragonblockarcanedba/client/render/TimeShatterRenderer.java
- src/client/java/com/dragonblockarcanedba/client/render/BanshoShockwaveRenderer.java
- src/client/java/com/dragonblockarcanedba/client/render/BanshoGaleRenderer.java
- src/client/java/com/dragonblockarcanedba/client/render/BanshoWindProjectileRenderer.java

BanshoCycloneRenderer.java is untouched (git diff is zero lines), as
specced. No foundation file was edited. No damage, cooldown, cost,
timing, mechanic, or synced field changed anywhere in this slot; the
only entity edit is presentation-only fragment spawning on a beat the
entity already owned.

## Whis Staff: Temporal Rift glass spike ring

TemporalRiftRenderer gained the temporal glass spike ring, drawn in
the unrotated world frame (the existing slow pose rotation still
serves the tendrils alone, because spikes stand on the ground).
Composition follows Carter's clustering steering (Nov 4, about 15:10
CT): two uneven rings instead of one even ring of 7 to 9.

- Main ring: spikeEruption RING, count 5, radius 0.82 of the rift's
  full radius, sizeScale 0.90, seed family rift seed ^ 0x7E51.
- Outer cluster: spikeEruption RING, count 3, radius 0.97 of the full
  radius, sizeScale 0.55, seed rift seed ^ 0x7E67.
- Total 8 spikes, inside the specced 7 to 9. The two rings sit at
  different radii, on different seeds and size classes, with coprime
  counts (5 and 3), so spikes pair up into clusters at some angles
  and stand alone at others instead of marching as a uniform bar.
- Palette is the rift fragments' own: root 0x2F6DB3, tip 0xDDF7FF.
  Both calls draw through submitSolidLit with the render state's
  lightCoords. Standing glass is physical matter, so no part of the
  ring is on the emissive path.
- Animation on the host's existing beats only: riseFactor runs over
  the first 20 ticks (the spec's slow RISE matched to the rift's
  build; the builder's per-spike stagger spreads it), HOLD at factor
  1 until the reversal tick, then sinkFactor runs 4 ticks from
  TemporalRiftEntity.REVERSAL_TICK (60). The renderer's lifecycle
  envelope (build 15 percent, dissipate 35 percent of the 100 tick
  life) is untouched, and dissipation only starts at tick 65, after
  the sink completes.
- Size note: the addendum's worked guidance puts the Whis ring at
  sizeScale about 0.7. The main ring runs 0.90 and the outer cluster
  0.55, a deliberate trade under Carter's fewer, bigger, deliberate
  pieces steering: five big spikes carry the read, three small ones
  break the outline. Combined silhouette weight is near the guidance.

TemporalRiftEntity gained the drop beat, presentation only, inside
the existing REVERSAL_TICK block after the reversal target loop:

- New private helper dropSpikeRing spawns one FragmentEntity.burst
  per spike position: 5 bursts at radius fraction 0.82 (unit 1.0)
  and 3 at 0.97 (unit 0.7), matching the renderer's two rings.
- Each fragment spawns at spike mid height (center plus 0.85),
  tips outward (axis radial 0.55, down 0.45), spread 38 degrees,
  speed 0.06 to 0.18, and reuses the tear heart burst's exact rules:
  palette 0x2F6DB3 to 0xDDF7FF, gravityScale 0.3, lifeTicks 110,
  restTicks 10. Seed family is the heart burst's neighbour salts
  (0x7E44 and 0x7E58, plus i times 131 per spike).
- Untouched, verified in the diff: PEAK_TICK damage and its sound,
  the reversal target loop, the tick 2 debris field and heart burst
  (still 5 fragments, seed ^ 0x7E42), all synced fields, lifetime.

Piece counts, Temporal Rift renderer: tendrils 3 before and after
(816 vertices). Spike calls 0 before, 2 after; spikes 0 before, 8
after; spike vertices 0 before, at most 3072 after, split as two
submitSolidLit submits of 1920 (main ring) and 1152 (outer cluster),
each inside the class M budget of 2500. FragmentEntity bursts per
rift: 1 call of 5 (heart) before; the same plus 8 calls of 1 (ring
drop) after.

## Whis Staff: Time Shatter second crackFan

TimeShatterRenderer.submitPieces now draws two crackFans in the same
submitEnergy call on the billboarded shatter plane:

- Main fan unchanged: seed pieceSeed ^ 0xC4ACE, radius scale times
  1.80, 0xFFFFFF to 0x65D8FF, alpha 0.92.
- Second fan added: seed pieceSeed ^ 0xC4B0 (the same second-fan salt
  pattern OxFissureRenderer uses), radius 0.62 of the main fan,
  alpha 0.85, and a counter-rotation of minus 0.35 radians applied
  in the fan's own plane (a Y rotation on the copied pose before the
  lay-flat X rotation), about half a crack spacing, so its branches
  interleave the main fan's instead of tracing over them.
- The FragmentEntity fan is byte-untouched, as specced; shatter
  timing stands (RELEASE_AT 36 lives in WhisStaffItem, not edited).

Piece counts: crackFan calls 1 before, 2 after. Worst case piece
vertices 792 before, 1584 after, inside the class M budget of 2500.

## Bansho Fan: Shockwave push wall

BanshoShockwaveRenderer gained the PUSH WALL in the pieces block,
after the existing fracturePlates and crackFan submits, inside the
same pose those pieces already use. Form follows the clustering
steering: not a neat row, but a front row of big slabs with a lower,
heaped backing ridge row behind it, and the backing row crumbles
first as the wave dies.

Palette (both modes): sampled per frame from the ground block under
the wave origin (the block at origin minus 0.5 y), taking its map
color, blending 45 percent toward soil 0x6B5138 so heaved earth
reads as earth rather than a glowing copy of the surface block,
then root at 0.50 brightness and tip lightened 30 percent toward
white. Fallback when there is no sampleable ground: root 0x4A4238,
tip 0x8A7B68 (weathered stone). All wall bodies draw through
submitSolidLit with the render state's lightCoords, per the
no-glow rule for physical pieces.

Cone mode (drawConeWall): one wall across the wavefront in the
rotated cone frame, local z as travel direction.

- Front row: rockWall at z equals coneLength times 0.88, facing
  local plus z, length clamp(coneRadius times 2, 4.8, 9.8), height
  1.25, arc minus 0.45 (center leading, a plow rather than a scoop),
  seed geoSeed ^ 0x5C19.
- Backing row: same call shape at front z minus 0.55, length times
  0.8, height 0.72, seed geoSeed ^ 0x5C23.
- The whole wall pose leans 0.16 radians into the travel direction.
- Beats on the wave's existing 14 tick progress: rise over the
  first 3 ticks, front row rides coneLength as it expands, backing
  row sinks from progress 0.50, front row from 0.62.

Radial mode (drawRingWall): three arcs around the expanding ring,
one centered on the wave's facing (world facing in the plain frame,
local plus z in the orient-to-impact frame), the others 120 degrees
apart, each with its own height (1.30, 1.48, 1.22) so the ridge line
is uneven around the ring, and a backing arc behind each at radius
minus 0.55 and 0.55 of the front height.

- Per arc: a pose translated to the ring at the current radius and
  rotated to face outward, leaning 0.16 radians outward. Front row
  length clamp(currentRadius times 1.5, 4.8, 9.8); the arc parameter
  is minus length over currentRadius, so each arc follows the ring's
  own circle exactly at every size and rides the existing expansion
  (the renderer's sine-eased currentRadius, the same front the
  plates, cracks, and vapor already ride).
- Beats: rise from age 1.5 over 3 ticks, backing arcs sink from
  progress 0.48, front arcs from 0.60.
- The crumble's chunk read rides the renderer's existing
  BLOCK_CHUNKS debris stream, which already sprays ground chunks
  across the disc under the wall through the wave's exit. No entity
  edit was made for Bansho, per the brief's renderer-only scope.

Piece counts: rockWall calls 0 before. After: cone mode runs 2 calls
in one lit submit (front row plus backing row, at most 16 slabs,
1152 vertices). Radial mode runs 6 calls in 6 lit submits (3 arcs
times front and backing rows); per submit at most 9 slabs, 648
vertices. Slab counts: front rows run 7 to 9 slabs once the front
is developed (coneRadius at least about 3.75, ring radius at least
5); before that the builder's 6 slab floor applies, because a
full-length wall on a young front would wrap past its neighbours.
Existing plates (about 480 vertices) and crackFan (under 900) are
unchanged. Shockwave expansion speed and radius, entity lifetime,
and every damage and knockback line are untouched; the entity file
was not edited.

## Bansho Fan: Gale tendrils 3 to 5

BanshoGaleRenderer: the pieces lambda gains the inner rib pair at
side plus and minus 1.15 (fanVec points at t 0, 0.5, 1.0, the same
rib curve builder the existing strands use), width 0.26, palette
0x9FFFE0 to 0x26FFA6, alpha 0.92, seeds geoSeed ^ 0x6A44 (left) and
^ 0x6A55 (right), drawn between the center strand and the outer
pair. The fan now reads as one torn sheet instead of three separate
strands.

Piece counts: tendrils 3 before, 5 after; vertices 1152 before,
1920 after, inside the class M budget of 2500.

## Bansho Fan: Wind Projectile tendrils 2 to 3 plus core shards

BanshoWindProjectileRenderer: the wake's control points are now
computed by a small spiralControl helper and re-spaced evenly at
120 degrees (phases spin, spin plus 2.094, spin plus 4.189, radius
0.30, z minus 0.55, all times ownerScale, as before). A third
tendril joins on seed geoSeed ^ 0xD433. Plus the solid core: one
shardBurst call through submitSolid (the brief's specced path for
this wind-glass, the same plain ki type every shardBurst in the mod
uses), 3 shards, origin z 0.42 times ownerScale at the head, axis
forward, spread 55 degrees, unit 0.36 times ownerScale, palette
0x9FFFE0 to 0xFFFFFF, alpha 0.95. The burst is seed-static in the
flight frame, so the cluster rides as one rigid glass head; the
wake supplies the motion around it.

Piece counts: tendrils 2 before, 3 after (768 to 1152 vertices);
shardBurst calls 0 before, 1 after (3 shards, 864 vertices, its own
submit). Projectile pathing, envelope, and first-person owner scale
are untouched.

## Bansho Fan: Cyclone

No change, per spec. BanshoCycloneRenderer.java and
BanshoCycloneEntity.java are both untouched; the cyclone's debris
counts stand exactly as they were.

## Verification

Targeted javac, JDK 25 at ~/workspace/tools/jdk-25.0.5+8, sourcepath
src/main/java:src/client/java, over the six edited files, full logs
read line by line (never summaries):

- cp262 (~/workspace/dba-audit-libs/cp262.txt): 4 errors, all the
  standing baseline closure kinds: DbaItems x2 (Output has protected
  access in CreativeModeTab), DbaMenus x2 (MenuType constructor and
  MenuSupplier private access). Zero errors in owned files.
- cp263 (~/workspace/dba-audit-libs/cp263.txt): 10 errors, exactly
  the documented baseline set: the cp262 four plus DbaHostileEntity
  x2, PatrolRobotEntity x2, FriezaSoldierEntity x1,
  NamekianWarriorEntity x1. Zero errors in owned files.

git diff --check on the six files: clean. Byte-level em dash scan
over all added lines in the six files (330 added lines): zero
matches for U+2014 and U+2013. No commits; HEAD remains ccfebf2.
No Gradle build was run or is claimed; the in-game look is
unverified until Carter's PC test.

## Change requests (foundation gaps, filed not fixed)

1. crackFan has no branch count knob. The audit spec for Time
   Shatter says crackFan branches plus 2, but crackFan derives its
   crack count internally (FxEnvelope.segments(9, distScale, 6)
   plus a seeded 0 to 2) and exposes no count, fork, or branch
   parameter, and there is no single-branch crack builder to add
   branches with. Request: an overload or parameter for the base
   crack count so owners can spec exact branch counts. What shipped
   instead: the second counter-rotated fan, which adds a whole
   second fan of branches; the visible branch density more than
   covers the spec's intent, but the per-fan branch count itself did
   not move, and I am naming that plainly rather than calling the
   substitution exact.
2. spikeEruption RING placement is builder-internal. The drawn
   spikes' exact angles and positions come out of the builder's own
   RNG stream, which server code cannot reproduce, so the reversal
   drop positions in TemporalRiftEntity are an independent seeded
   take on the same two rings (same counts, radii, palette, size
   classes), not a per-spike match. Request: a placement phase
   parameter on spikeEruption, or a shared position helper both
   sides can call, so entity drops can land exactly where drawn
   spikes stood. This matters more after the merge's planned
   irregularity upgrade, which will move drawn positions again;
   without an export, the gap widens.
3. Observation only, no request: rockWall has no lean parameter, so
   the wall's forward lean is applied at the pose (a 0.16 radian X
   rotation), which worked cleanly in both shockwave frames.

## Honest worse check

Whis Staff:

- The reversal handoff is the soft spot. Drawn spikes sink over 4
  ticks while fragments fall from independently computed ring
  positions, so an individual spike can read as stepping sideways
  as it turns into falling glass. Mitigations shipped: identical
  ring radii and counts, same palette and size classes, fragments
  spawn at spike mid height so the fall starts where the spike
  stood, and the 4 tick sink overlaps the fall. Change request 2 is
  the real fix. First thing to judge on the PC test.
- The spike ring stands at the FULL radius from tick 0 while the
  field's visible edge unfolds over 15 ticks, so early spikes rise
  at a rim the field has not reached yet. The 20 tick rise softens
  it. Keying the ring radius to the unfolded radius would fix the
  early read but break agreement with the server drop, which uses
  the full radius; I kept the agreement and named the cost.
- Time Shatter's plane is busier: two fans plus the fragment fan
  plus the crack spoke chips. The second fan is subordinated (0.62
  radius, alpha 0.85 against 0.92), but if the shatter reads
  crowded at point blank, the second fan is the piece to cut, and
  the literal branches-plus-2 spec line stays unmet until change
  request 1 lands. Nothing was reverted; the density is the spec's
  own ask.

Bansho Fan:

- The wind wake's two original strands moved. Re-spacing from an
  opposite pair (180 degrees) to an even triple (120 degrees) is
  what tendrils 2 to 3 means geometrically, and the drill read is
  better distributed, but the old pair's exact twist character is
  gone. If Carter preferred it, reverting is one deleted tendril
  call plus restoring the opposite control point.
- The glass core cluster is rigid in the flight frame while the
  wake spins around it. That contrast is deliberate (a solid head
  inside a spinning wake), but if it reads as frozen on the PC
  test, the follow-up is a slow spin on the shard pose, not more
  shards.
- The wall rides the renderer's sine-eased front, like every other
  visual in this renderer, while the entity applies its rim
  effects on a linear radius; mid-wave the drawn wall runs ahead
  of the damaging rim. This divergence predates the wall (plates,
  cracks, and vapor all ride the same eased front) and the wall
  inherits the visual front on purpose; no damage moved.
- Radial arcs wear the origin's palette even where the ring
  crosses different terrain. Sampling per arc would fix it at
  three times the sampling cost; named, not done.
- On steep cone pitches the wall stands in the cone frame rather
  than on the terrain, so a hard downward cast can leave it
  floating or buried relative to the ground. The existing cone
  pieces share this frame behavior; the wall inherits it.
- Gale at 1920 vertices sits near the class M ceiling at point
  blank. Distance scaling cuts tendril samples with range, as it
  did at 1152; if the fan reads busy up close, the inner pair is
  the first thing to thin.

Nothing in this slot was reverted. Every regression risk above is a
judgment call for the PC test, with the cause attached.
