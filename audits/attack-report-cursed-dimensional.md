# Attack Upgrade Pass, Phase 2 owner report: Cursed Blade and Dimensional Sword

Owner slot: Cursed Blade and Dimensional Sword. Specs:
audits/attack-complexity-audit.md sections 8 and 10, plus
audits/attack-foundation-addendum.md. HEAD ccfebf2, no commits made.
Six renderer files changed, all client side. No entity file, item
file, damage number, cooldown, cost, timing, mechanic, or synced
field was touched anywhere in this slot.

## Cursed Blade

### CurseGroundShatterRenderer: obsidian spikes (new), plates to rim

Before: pieces were a crackFan (9 to 11 cracks) plus fracturePlates
(10 plates at the shatter radius), both unchanged in kind.
After:

- spikeEruption, SCATTER layout, 8 spikes at sizeScale 1.25,
  scatter radius 0.80 of the shatter radius, through submitSolidLit
  with the render state's lightCoords. Composition follows Carter's
  steering (Nov 4, about 15:10 CT): few and big on purpose, bunched
  on a tight inner disc so they lean together like an eruption
  instead of ringing the shatter evenly, and the merge's builder
  upgrade deepens the clustering from there. Palette is real black
  glass per the quality bar: root 0xFF070409 (near black), tip
  0xFF241832 (a faint violet sheen only), so the lit facets carry
  the read instead of a purple crystal gradient. Beat: RISE over
  4 ticks (smoothstep of age / 4), no hold, then a crack-and-fade
  settle: sinkFactor runs 0 to 1 over ticks 4 to 12 while the spike
  envelope falls over ticks 4 to 13, so the cluster is gone by
  about tick 13 of the 35 tick life.
- fracturePlates now draws at 1.2 times the radius, pushing the
  plate bodies into the outer band so the spikes own the disc.
  Plate count, lift, and colors are unchanged.
- Vertex cost of the new call: 8 spikes at 384 vertices is 3072
  worst case before distance scaling, on its own submitSolidLit
  submit; plates and crackFan keep their own submits as before.

### SkyCracksRenderer: heart refill and branchier fans

Before: two crackFans (outer at detail ds, inner at 0.7 ds) and no
drawn shards at all; the falling shards are FragmentEntity rain and
stay that way (the entity file and its counts and cadence are
untouched, per spec).
After:

- Hanging shardBurst at the tear heart: 5 shards, unit 1.0, spread
  45 degrees, axis straight down in the plain entity pose (the pose
  the drawn clusters used before the rain became entities), through
  submitSolid in curse violet glass (root 0x2A0A4A, tip 0x8C1AF2;
  an energy material, so the emissive path stays correct per the
  audit). Its envelope pulses on the rain's own shed beat: a cosine
  swell peaking at age % 20 == 10, the server side shed tick, over
  a 0.65 floor, so the heart breathes in step with each real rain
  batch instead of competing with it. Cost: 5 shards at 288
  vertices is 1440 worst case.
- crackFan branches plus 2 per fan: outer detail ds + 2/9 (base
  cracks 9 to 11 at close range), inner 0.7 ds + 2/9 (base cracks
  6 to 8 at close range). Mechanism and side effect are in change
  request 1 below.
- CurseLightningRenderer: not edited. The bolt path is byte
  identical, verified by an empty git diff on the file.

### CurseChainRenderer: paired chain and anchor shards

Before: one tendril along the chain path, no solid pieces.
After:

- The tendril is doubled into a paired chain: a second strand on
  the same bezier endpoints shifted sideways by a horizontal
  perpendicular (0.10 at the ends, 0.17 at the control, control
  also dropped 0.05), at 0.72 of the main width and 0.85 of its
  alpha, same violet to crimson gradient, its own seed salt. Both
  strands draw in the same submitEnergy call under the unchanged
  pose transform, in flight and bound alike.
- shardBurst of 3 at the anchor point (local origin, axis up,
  spread 70, unit 0.55 in chain local units, root 0x380852, tip
  0xBF0DFF) through submitSolid, drawn only while the chain is
  attached: in flight there is no anchor yet, so the burst waits
  for the bind. Chain timing, attach behavior, and the fade math
  are untouched. Cost: 3 shards at 288 is 864 worst case.

### AbyssalDomainRenderer: domain fan branches plus 3

Before: one crackFan at detail ds (base cracks 9 at close range).
After: detail ds + 1/3 (base cracks 12 at close range). Radius,
colors, the ten rim tendrils, and every emission stream are
unchanged. Domain build, fade, and behavior are untouched.

## Dimensional Sword

### DimensionalSlashRenderer: five strands, in-plane crack, end shards

Before: 3 tendrils, no crack geometry, no solid pieces.
After:

- Tendrils 3 to 5. The three existing strands are byte identical
  in placement, color, and width. Two strands are added: a high
  strand cresting above the bow (control at 1.34 of scale, magenta
  to violet, width 0.20) and a low strand sagging under it
  (control at 0.62 of scale pushed back along the flight path,
  violet to white, width 0.22). Each carries a seeded phase offset
  on the shared 0.6 per tick swell rate (phases rolled from the
  slash seed), so the five strands never pulse in sync.
- crackFan at the cut's center, in the slash plane: the pose is
  multiplied by a basis built from the renderer's own right,
  forward, and up vectors (fan local X along the cut, local Z
  across it, local Y out along the flight direction), radius 0.80
  of the cut half width, hot white to warp violet, through
  submitEnergy. At the capped detail of 0.62 the fan lands at 6 to
  8 branches; the spec's exact 5 is below the builder's floor, see
  change request 2.
- shardBurst of 4 crystal shards at the cut's ends: two per end at
  0.95 of the half width, axes pointing outward along the cut,
  spread 55, unit 0.50 of scale, warp violet glass (root 0x2A0A4A,
  tip 0xE8CCFF) through submitSolid. Cost: 4 at 288 is 1152.
- Pieces total is now roughly 3300 vertices worst case, inside
  the class L budget of 6000 the file already claimed. Lifecycle,
  linger, first-person scale-in, and all emission are unchanged.

### DimensionalWarpRiftRenderer: branchier fan, mirrored arrival

Before: 2 tendrils and one crackFan at detail 0.7 ds (base cracks
6 at close range), drawn the same for origin and destination.
After:

- crackFan branches plus 2: detail 0.7 ds + 2/9 (base cracks 8 at
  close range). Same seed, radius, and colors.
- Tendrils: the spec's end state is 2 and the code at HEAD already
  draws 2 (the audit's today line counted builder types, not
  strands), so no tendril change was needed or made.
- Destination rifts gain a mirrored crackFan: the same fan (same
  seed, radius, colors, detail) drawn a second time under a pose
  mirrored across local X, destination only, so arrival tears the
  ground as violently as departure. Cost: about 600 more vertices
  on destination rifts; worst case total near 1970, inside the
  class M budget of 2500 the file documents.
- Fragment spawns (4 per rift) are the entity's business and were
  not touched; warp distances and rift lifetimes are unchanged.

## Verification

Targeted javac (JDK 25 at ~/workspace/tools/jdk-25.0.5+8,
sourcepath src/main/java:src/client/java) over exactly the six
owned files, full logs read end to end, never summaries:

- cp262 (classpaths in ~/workspace/dba-audit-libs/cp262.txt):
  4 errors, all standing baseline closure kinds (DbaItems x2,
  DbaMenus x2). Zero errors in owned files.
- cp263: 10 errors, exactly the documented baseline set (the 26.2
  four plus DbaHostileEntity x2, PatrolRobotEntity x2,
  FriezaSoldierEntity x1, NamekianWarriorEntity x1). Zero errors
  in owned files.

git diff --check clean on the six files. Byte-level em dash and
en dash scan over added lines in the six files: zero. HEAD is
still ccfebf2; no commits. No Gradle build was run or is claimed;
the in-game look is unverified until Carter's PC test. One working
tree note: the tree also carries the other Phase 2 owners' edits;
every check above was scoped to this slot's six files.

## Change requests (foundation was read-only)

1. crackFan has no explicit branch count parameter. Its crack
   count derives from segments(9, distScale, 6) plus a seeded 0 to
   2, so every "branches plus N" spec in this pass (SkyCracks x2,
   Abyssal Domain, Warp Rift) was implemented by raising the
   detail argument by N/9. That adds exactly N base cracks above
   the detail floor, but it also raises segments per crack
   (segments(6, detail, 4)): SkyCracks outer 6 to 7 and inner 4
   to 6 at close range, Abyssal Domain 6 to 8, Warp Rift 5 to 6.
   The fans are denser per crack as well as branchier. An explicit
   branches parameter would make the spec exact.
2. crackFan cannot draw fewer than 6 base cracks (the segments
   floor of 6, plus the seeded 0 to 2), so the Dimensional Slash
   spec of a 5 branch fan lands as 6 to 8 branches at the capped
   detail of 0.62. Closing this needs the same explicit branch
   parameter with a lower floor.
3. fracturePlates has no inner radius or rim band parameter.
   "Plates pushed to the rim" was done by scaling the fan radius
   to 1.2 times, which moves the plate band outward but also grows
   the plates themselves (they are proportional to the radius) and
   lets outer edges reach about 1.32 times the nominal shatter
   radius. A rim band parameter (inner radius fraction) would push
   the plates out without enlarging them.
4. spikeEruption SCATTER placement is a uniform seeded disc
   scatter. Carter's steering asks for clustered, uneven placement
   like a real eruption; per that steering this lands as the
   merge's signature-compatible builder upgrade. Owner side, the
   composition already favors clustering (8 large spikes on a 0.80
   radius disc), but true cluster centers are a builder change.

## Honest worse check

- Ground Shatter plates at 1.2 times radius: outer plate edges
  can reach about 1.32 times the nominal shatter radius, past the
  crackFan's nominal 1.02 disc but inside the crack web's worst
  case reach (about 1.35 times) and the shock dust front (1.3
  times). If the PC test shows plates floating past the visible
  shatter, the dial is 1.1 times, not a revert. The spikes at
  1.25 size classes run about 1.5 to 4.0 blocks long; the tallest
  are dramatic against the 3.5 radius disc, and they are gone by
  about tick 13, so they cannot overstay. The near-black palette
  is deliberate (real black glass); in dark scenes the spikes will
  read as silhouettes against the crimson cracks. If they read as
  invisible rather than black, the tip sheen 0xFF241832 is the
  dial, not the count and not the form.
- SkyCracks fans are denser per crack as a side effect of the
  branch mechanism (change request 1); with the heart refill the
  pieces total rises to roughly 3700 vertices across three
  submits, up from about 1300, still inside class L. The refill
  shards are drawn decoration at the heart, pulsing rather than
  falling, small against the 21 radius tear; they do not
  impersonate the entity rain. The pulse phase uses client age,
  which tracks the synced tickCount, so worst case is a sub-tick
  offset from the server shed, not a wrong beat.
- CurseChain paired strand: the perpendicular is computed from
  the from-to chord, which in flight runs along local -Z, so the
  pair sits side by side across the trail; on the bind arc it
  shifts around the arc. Both read as one doubled chain. The
  anchor burst being attached-only means a flying chain shows no
  shards, which is the intent (no anchor exists yet).
- Dimensional Slash is the busiest change in this slot: at full
  pulse the top of the cut stacks three bows (high strand, rim
  strand, main strand). That is the spec's five strands doing
  their job, and it is the first thing to judge on the PC test;
  if it reads as clutter, the high strand is the one to drop and
  the low strand keeps the count honest. The end shard bursts
  distance-scale down to 1 shard per end at long range, which is
  acceptable falloff, not a bug.
- Warp Rift mirrored fan: the mirror is a pose mirror of the same
  seeded fan, so the destination web is the exact chirality flip
  of its own base web. The builders emit double-sided triangles
  and quads and the fan rides the additive shader, so the flipped
  winding renders in kind; the PC test is still the first real
  look at it. Destination vertex cost roughly doubles for the fan
  only, already accounted above.
- Nothing was reverted. Every change in this slot is additive
  presentation inside renderer submit methods; the one placement
  change (Ground Shatter plates at 1.2 times radius) is flagged
  above with its dial.
