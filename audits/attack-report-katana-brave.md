# Attack Upgrade Pass, Phase 2 Owner Report: Katana and Brave Sword

Owner slot: Katana and Brave Sword (piece multiplication specs, no
walls or spikes). Specs: audits/attack-complexity-audit.md sections
1 (Katana) and 4 (Brave Sword), plus audits/attack-foundation-addendum.md.
HEAD ccfebf2, no commits made. Nine renderer files edited, all in
src/client/java/com/dragonblockarcanedba/client/render. Every change
is presentation inside submit methods: no damage, cooldown, cost,
timing, mechanic, or synced field changed anywhere, and no entity or
item file was touched. Every new physical piece (steel, crystal)
draws through ComplexGeometry.submitSolidLit with the render state's
lightCoords; energy additions (a tendril, a crackFan, flame tongues)
stay on submitEnergy. Foundation files were not edited.

Piece vertex convention for the budgets below: one shard is 48
triangles, 288 vertices in the addendum's double-sided counting; one
tendril at full segments is about 384 vertices.

## Katana

### SwiftCrescentRenderer

- Tendril arcs: 1 before, 2 after. The new second arc shares the
  main arc's exact tips and bows off the opposite lip (control point
  mirrored in forward and up, slightly shallower), width 0.30 times
  baseScale against the main arc's 0.36, alpha 0.55, which is 60
  percent of the main arc's 0.92, seeded apart (seed xor 0x5A17
  against the main arc's 0xC3E5). Same TENDRIL canvas and variant
  lip color, so the cut reads as one crescent with two edges.
- Solid shards: 0 before, 6 after. Three clusters of 2 along the
  arc's leading edge at bezier parameters 0.32, 0.5, and 0.68 (six
  shards, size class S: unit 0.34 times baseScale, lengths about
  0.10 to 0.54 blocks). Each cluster sheds along the arc's own
  tangent at its point on the curve, the way fragments leave a
  moving edge. Silver gradient 0xFF8E9DAE root to 0xFFF2F8FF tip,
  submitSolidLit, gated to the swing's peak window (envelope is
  fade times the existing strike impact factor).
- crackFan: 0 before, 1 after, conditional. When the swing is aimed
  low (xRot at least 20 degrees) and the entity runs within reach
  of the ground (heightmap drop above 0 and at most 2.2 times
  baseScale plus 0.5), a short crackFan (radius 0.85 times
  baseScale, white to variant lip color) sits at the ground contact
  under the arc belly, weighted 0.25 plus 0.75 times the strike
  impact so it flashes hardest at the strike tick instead of
  sitting as a static decal. The branch count is the builder's own;
  see change request 1.
- Peak added vertices: about 384 (second tendril) plus 1728
  (shards, peak window only) plus roughly 500 to 900 (ground fan,
  only when aimed low over ground).

### HeavenSplitterRenderer

- Split geometry positions: untouched. The strand, the ground
  crack fan, the wall height math, and all emission are byte
  identical to HEAD.
- crackFan: 1 before, 2 after. The second fan is mirrored onto the
  split wall's own face at the split point (path midpoint, raised
  to 0.52 of wall height where the strand runs), radius
  dashLength times 0.26, exactly half the ground fan's 0.52. A
  basis matrix maps the fan plane (local XZ, normal +Y) onto the
  wall plane: local X to minus right, local Y to forward, local Z
  to up, which also floats the fan 0.03 toward the front face.
  Same white to cyan palette, seed xor 0xCAFE apart from the
  ground fan's 0xC4AC.
- Solid shards: 0 before, 6 after. A crown of six crystal shards
  above the split point at 0.55 of wall height, placed one by one
  in the wall plane along a shallow arc over the split point
  (offsets along right from minus 1.15 to plus 1.15 times scale,
  heights following 0.42 minus 0.30 times frac squared), each at
  its own length (0.45, 0.58, or 0.71 times scale) and hanging
  tilt, with a slight alternating out of plane lean. Crystal
  gradient 0xFF8FD8E8 root to white tip, submitSolidLit. The
  placement method is a deliberate form choice; see the worse
  check.
- Added vertices: 1728 (crown) plus roughly 500 to 900 (mirrored
  fan).

### KatanaChargeRenderer

- tendrilFan: 5 strands, unchanged.
- Orbiting shards: 0 before, 3 after, added to the charge fan.
  Three shard() singles circling the stance at hilt height (local
  y 0.75), orbit radius 0.50 plus charge times 0.25, drawn lit,
  envelope following the existing lash envelope so they build and
  fade with the charge. Form is deliberately un-ringlike: per
  shard radius factors 0.86, 0.99, 1.12, per shard angular
  speeds, per shard bob amplitude and phase, lengths 0.30, 0.36,
  0.42, and flight tipped inward and off plane, so the three read
  as loose steel swirling in, not a rigid ring. Steel gradient
  0xFF9FB8C8 root to white tip.

### KatanaAimGuideRenderer

- tendrilFan: 5 strands, unchanged. The guide path, its station
  emission, and the terminal point are untouched, so the
  telegraph read stays exactly as it was.
- Orbiting shards: 0 before, 3 after, at the guide origin only.
  Three shard() singles in the plane across the aim, radii 0.30,
  0.345, 0.39, lengths 0.26, 0.30, 0.34, per shard speeds, each
  tipped out of plane by its own amount, envelope following the
  guide alpha including its fade in and out. Steel gradient
  0xFF8FB6C9 root to white tip, submitSolidLit.

## Brave Sword

### BraveCrossSlashRenderer

- Tendrils: 2, unchanged. Cross slash timing untouched.
- Solid shards: 0 before, 8 after. Four shards along each slash
  plane at the crossing tick: each plane's four are composed as
  two half bursts of 2, one per arm of the diagonal, so the
  shards stay on the diagonal line instead of coning off it.
  Unit 0.50 times scale, spread 26 degrees, bright steel
  0xFFE8F1F8 root to gold 0xFFFFD700 tips, submitSolidLit.
  Envelope is the cross envelope times the existing strike
  window factor (progress 0.28 to 0.62), so the solid heart
  flashes in and out with the strike sine and never pops.
- Peak added vertices: 2304, inside the crossing window only.

### BraveSlashRenderer

- Tendrils: 2 before, 3 after. The third strand is a thin leading
  echo: span 0.30 of gspan each way, control forward 0.70 (ahead
  of the main strand's 0.62 crest), width 0.11, pale gold
  0xFFF2C0 to gold 0xFFD700, seeded apart (seed xor 0x2B992DDF).
- Solid shards: 0 before, 3 after. One shardBurst of 3 at the arc
  apex, placed at the main strand's exact bezier crest (forward
  0.36 in local space), axis forward, spread 40 degrees, unit
  0.45, bright steel to gold tips, submitSolidLit.

### BraveRushTrailRenderer

- Tendrils: 3, unchanged. Rush trail lifetime untouched (the host
  still lives its synced 30 ticks).
- Shed shards: 0 before, up to 10 after. A trailing shard shed of
  2 shards every 6 ticks: five sets over the 30 tick life, set s
  appearing at tick 6s with a 2 tick envelope ramp, stationed
  along the path from tip toward tail at fraction (s plus 1) over
  6, shed backward along minus forward at 50 degrees spread,
  small class. Placement and size are seeded per set (along path
  jitter plus or minus 0.25 times gscale, cross jitter plus or
  minus 0.15, unit 0.26 to 0.35 times gscale), so the shed steel
  lands as scattered debris, not beads on a string. Bright steel
  to gold tips, submitSolidLit, first person gscale applies to
  stations and sizes exactly as it does to the strands.

### BraveShockwaveRenderer

- fracturePlates: 1 call, unchanged. crackFan: 1 call, unchanged.
  Shockwave radius, expansion ease, and envelope untouched.
- flameTongues: 0 before, 1 call after (9 tongues at distance
  scale 1). One ring in brave gold at the plate rim, on the rim
  convention the slam family already uses (tongue disc radius
  just past the plate radius, here currentRadius times 1.05, the
  same factor DevilSlamShockwaveRenderer uses), base at local
  (0, 0.06, 0), height 1.05, palette pale gold 0xFFF2C0 at the
  base to brave gold 0xFFD700 at the tips, sway age times 0.45,
  submitted with the FLAME_TONGUE canvas. The ring follows the
  same expanding currentRadius as the plates and crack fan.

### BraveChargeRenderer

- tendrilFan strands: 5 before, 7 after (the spec's plus 2).
  Fan origin, direction, spread, length, width, and palette are
  unchanged, and the inline comment was updated to match.

## Verification

- Targeted javac, JDK 25 at ~/workspace/tools/jdk-25.0.5+8,
  sourcepath src/main/java:src/client/java, over the nine owned
  files, full logs read (never summaries):
  - cp262: 4 errors, all standing baseline closure kinds
    (DbaItems 2, protected access in CreativeModeTab; DbaMenus 2,
    private access in MenuType). Zero errors in owned files.
  - cp263: 10 errors, exactly the documented baseline set (the
    cp262 four plus DbaHostileEntity 2, PatrolRobotEntity 2,
    FriezaSoldierEntity 1, NamekianWarriorEntity 1). Zero errors
    in owned files.
- Two real owned-file errors were caught by the first compile and
  fixed before the clean runs above: HeavenSplitterRenderer
  captured the reassigned local right in the crown lambda (fixed
  with final aliases), and KatanaAimGuideRenderer captured alpha,
  which the fade block reassigns (fixed with a final alias). The
  clean results are from recompiles after those fixes.
- git diff --check: clean on all nine files.
- Byte level em dash and en dash scan over all 245 added lines:
  zero of either.
- No commits. The tree also holds other Phase 2 owners'
  uncommitted work; this slot touched only the nine files above.
- No Gradle build was run or is claimed; Gradle cannot run in
  this VM. The in-game look is unverified until Carter's PC test.

## Change requests (foundation gaps, filed not fixed)

1. crackFan branch count. ComplexGeometry.crackFan derives its
   crack count internally (segments of 9 at minimum 6, plus 0 to
   2, so 6 to 11 cracks) and takes no count parameter. The
   Katana spec asked for a short crackFan of 3 branches at the
   Swift Crescent ground contact. I implemented the short fan by
   radius instead (0.85 times baseScale); it renders 6 to 11
   short branches, more and finer than specced. Request: a
   crackFan overload with an explicit crack count so short fans
   can be authored at exactly 3 branches.
2. flameTongues radial band. flameTongues places tongues at
   0.15 to 0.80 of the base disc radius, so a true rim ring is
   only approximated by passing a disc radius just past the
   plate radius (the established slam family convention). If a
   true rim ring is wanted later, request an optional radial
   band parameter. Not blocking; the Brave ring follows the
   convention the merge already accepted for the Devil slam.

## Honest worse check and realism review

Carter's quality bar (Nov 4) was applied as a form review of
every new piece before shipping: authored, physically believable
shapes over game-looking ones, form fixed before count.

- Every new solid piece is the toolkit's authored shard form
  (seeded noise displacement, closed spindle, per face facet
  brightness steps) drawn through the world lit path. No smooth
  primitives, no billboards, and no new emissive physical pieces
  were added anywhere in this slot.
- Katana orbiting shards: the first version of both orbits was a
  perfect ring (equal radii, equal speeds, equal sizes, pure
  tangent flight). That read as a game construct, so both were
  rebuilt with per shard radius, speed, bob, size, and tipped
  flight before shipping. This is the clearest form fix in the
  slot and the one most worth the merge's eye.
- HeavenSplitter crown: implemented as six individually placed
  shards instead of one shardBurst cone. A cone spray from a
  single point reads as a firework, which is toy-like against
  the form bar; placed splinters hanging in the wall plane read
  as fractured glass caught in the cut. Count (6), position
  (above the split point), palette, and the lit path all follow
  the spec; only the placement method differs. Flagged for the
  merge in case it wants the literal burst call.
- HeavenSplitter mirrored crackFan: the audit text says "a
  second crackFan mirrored at half radius" without naming the
  mirror plane. I mirrored across the ground line onto the split
  wall's own face, the only reading that adds new information; a
  same spot half radius fan would only densify the ground web.
  If the merge intended the densify reading, this is the one
  change to swap. On flat ground the wall fan stays on the face
  (centered at 0.52 of wall height, radius about 4.2 blocks at
  the default 16 block dash against a 7.2 block wall height).
- SwiftCrescent ground crackFan: gated on both pitch and real
  heightmap proximity, so it cannot appear under mid air
  slashes. On slopes the single point heightmap sample can sit
  slightly off the visual ground; at 0.85 times scale radius
  that reads as cracking at the contact. Kept.
- SwiftCrescent second lip: thinner and at 60 percent alpha, so
  it stays subordinate to the main arc; with strong tilt angles
  the two lips plus the plasma volume are the busiest moment in
  this slot, but the lip colors and tips are shared, so it reads
  as one crescent. Kept.
- BraveCrossSlash shards exist only inside the crossing window
  (about 14 of the 40 ticks) and ramp on the strike sine itself,
  so there is no pop at the window edges. Kept.
- BraveRushTrail shed: the last set appears at tick 24 of 30, so
  it gets about 6 ticks at full envelope. That is short but the
  shed is a trail effect; earlier sets carry the read. Kept.
- BraveShockwave flame ring: tongues rise up to about 1.3
  blocks at full radius against plates that lift 0.34. The
  proportions match the KiExplosion and Devil slam family, and
  the tongues are torn mesh strips, not cones. This is the
  tallest new silhouette in the slot and the one I would look
  at first on the PC test. Kept, flagged.
- Nothing was reverted. No part of the spec made a weapon look
  or play worse that I can find, and since no timing, damage,
  cooldown, cost, or synced field was touched, nothing plays
  differently at all.
