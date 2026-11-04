# Attack Upgrade Report: Blaster Gun (worst offender 3, Bolt rebuild)

Owner slot: Blaster Gun. Spec: audits/attack-complexity-audit.md
section 19, plus audits/attack-foundation-addendum.md (submit
discipline) and Carter's Nov 4 quality bar (shapes held to the
character and form standard: real form, real material, real weight;
fix form before count). HEAD ccfebf2, no commits made. Files
changed: exactly three renderers. No entity, item, damage, cooldown,
cost, timing, mechanic, or synced field was touched anywhere; bolt
speed, size, damage, and fire rate are unchanged, and the cannon
channel and beam path are unchanged.

## BlasterBoltRenderer (the rebuild)

Before: the pieces layer was two tendril ribbons in one
submitEnergy call (768 vertices) and the bolt's body was carried by
plasma sprites: a bare plasma ball with a wake. SPARK_STREAK
appeared only as a one-shot burst on the impact transition.

After, in flight:

- Solid core (new body): a casing of 4 interlocked plates,
  ComplexGeometry.shard pieces through submitSolidLit with the
  render state's lightCoords, in a gun palette (root 0x23262B,
  tip 0x565D66). The casing is physical gunmetal, so it is drawn
  lit like FragmentRenderer and the Grand Blade Shard; the casing
  body itself is never emissive. Form pass under Carter's quality
  bar: the plates are built like sabot petals on a real slug
  round, short and wide (length 0.78 of the head length, cross
  radius 0.66 of the bolt radius) instead of long needles, seated
  on a shell orbit of 0.46 of the bolt radius so only their edges
  overlap, shingle-staggered along the body with two alternating
  length classes (1.0 and 0.90), and nearly parallel with only a
  0.12 inward pinch, so they read as armor segments and not as a
  flower or drill tip. Each plate rolls its own seeded silhouette
  (seed salt 0xCA5E plus per-plate stride), and the whole casing
  spins slowly in flight (0.08 radians per tick of age).
  4 shards at 288 vertices: 1152 per submit.
- Hot core inset: one shard on submitEnergy at the center
  (288 vertices), heat palette root to near-white tip, shortened
  to 0.80 of the casing length and set forward so it reads as an
  inset core mostly hidden by the plate shell, glowing through
  the seams and just emerging at the nose. This is the only
  emissive part of the body.
- Wake: the two tendrils are replaced by a tendrilFan of 4
  strands through submitEnergy with the tendril canvas
  (4 tendrils at 384 vertices: 1536 per submit), rooted just
  behind the casing, spread 46 degrees, length 0.95 of the tail
  length, white-hot at the body grading to the heat color at the
  tail ends.
- SPARK_STREAK shed layer (new): a continuous emitter along the
  whole flight path at the standard continuous rate (30 per
  second, cap 44), joining the five existing filler channels
  (six channels total). The plasma filler is unchanged; it is
  filler around the casing now, not the body.

After, on impact (the existing collapse window, impactAge 0 to 5):

- A shardBurst of 5 casing fragments through submitSolidLit
  (5 shards at 288 vertices: 1440 per submit) in a 135 degree
  cone at the nose, same lit gunmetal as the casing, unit sized
  as chunky broken bits (1.28 of the bolt radius, growing 0.65
  to 1.25 across the collapse) beside the existing SPARK_STREAK
  impact burst, which is untouched. The casing, core, and wake
  keep collapsing under the existing boltDie envelope.

Piece totals: flight pieces 2976 vertices across three submits
(1152, 288, 1536); every individual submit is inside the class M
budget of 2500. Disclosed plainly: summed across submits, the
flight pieces run about 19 percent over the class M total of
2500 that some renderers in this mod apply as a whole-effect
sum. The audit mandates all three elements at once (4 plates, a
fan of 4, a core inset) and each element sits at its builder's
minimum cost, so the overage is a spec consequence, flagged here
for the merge rather than trimmed against the spec.

## ErasureChargeOrbRenderer

Before: one tendrilFan of 5 strands (1920 vertices) as the whole
pieces layer; nine emitter channels unchanged throughout.

After:

- Strands plus 2: a second tendrilFan call adds 2 shorter lashes
  (0.78 of the main length, 0.80 of the width) at an orbit phase
  offset of 2.1 radians from the main fan, in its own submit
  (768 vertices), so the orb carries 7 lashes and no single
  submit exceeds the class M budget (main fan stays 1920).
- Orbiting shard pair (new): 2 ComplexGeometry shards through
  submitSolid (576 vertices), erasure crystal palette (root
  0x37E6FF, tip 0xFFFFFF), circling the orb opposite each other
  in the right and up plane at 1.32 of the orb radius, each drawn
  along its own tangent so the pair reads as blades in orbit.
  Form pass: shortened to 0.78 of the orb radius and thickened
  to 0.34, so they read as crystal blades with weight, not
  needles. Their size tracks the orb radius as the charge grows.

Pieces total 3264 across three submits; largest submit 1920,
inside the class M budget per submit, with the same summed-total
disclosure as the bolt.

## ErasureCannonBeamRenderer (polish only)

- The ground-contact crack web is now two crackFan webs: the
  main fan at full plume radius (unchanged) plus a second,
  denser inner fan at 0.55 of that radius on its own seed at
  0.75 alpha, in its own submit (about 570 vertices by the
  emission loops). This is the audit's crackFan branches plus 2
  delivered the way section 1 of the audit delivers the same
  instruction (a second fan at reduced radius), because crackFan
  has no branch-count parameter. The beam body, chain, terminus
  fan, path, stations, and every emitter are byte-identical to
  before.
- Pieces total: about 5748 counted by the loops (was about
  5178), or about 6400 on the conservative 900-per-fan count the
  renderer comment used before (was 5508). The tendril submit is
  4608 of the class L budget of 6000; each crack web is its own
  submit far inside it. Disclosed for the merge.

## Verification

- Targeted javac (JDK 25 at ~/workspace/tools/jdk-25.0.5+8,
  sourcepath src/main/java:src/client/java) over the three owned
  files, full logs read, run twice (before and after the form
  pass, identical results):
  - cp262: 4 errors, all standing baseline closure kinds
    (DbaItems x2, DbaMenus x2). Zero in owned files.
  - cp263: 10 errors, exactly the documented baseline set (the
    26.2 four plus DbaHostileEntity x2, FriezaSoldierEntity,
    NamekianWarriorEntity, PatrolRobotEntity x2). Zero in owned
    files.
- git diff --check clean on the three owned files.
- Byte-level added-lines scan over the three files (160 added
  lines): zero em dashes (U+2014), zero en dashes (U+2013).
- No Gradle build run or claimed; the in-game look is
  unverified until Carter's PC test.

## Change requests

None against the foundation. One interpretation to note: audit
section 19 writes the casing submits as submitSolid, while the
foundation addendum's rule sends physical palettes through
submitSolidLit and names a physical piece on the emissive path a
merge-gate failure. Gunmetal casing is physical matter, so the
casing and its impact fragments use submitSolidLit (lit); the
hot core inset alone uses submitEnergy. The orb's shard pair
keeps the audit's literal submitSolid, as an erasure-energy
crystal in the same family as the SkyCracks glass exception the
addendum records.

## Honest worse check

- Realism review, per Carter's bar: the first casing cut was
  wrong in form (long thin petals on a tight orbit with a hard
  inward pinch: a flower bud, toy-like) and was rebuilt on the
  sabot-petal proportions above before this report was written.
  What remains short of the bar: a ComplexGeometry shard is a
  faceted spindle, so each plate is a rounded petal with a soft
  point, not a flat machined plate with beveled edges. The one
  flat-plate builder, fracturePlates, fans ground plates in a
  fixed plane and cannot make 4 plates or wrap a body axis, so
  a truly flat casing plate is a foundation gap, not filed as a
  change request because the sabot-petal read is a real-world
  form (APFSDS sabot petals are interlocked curved segments
  around a core) and the merge realism review can judge it in
  game. If it reads toy-like there, the fix is a foundation
  plate builder, not more pieces.
- The bolt's plasma filler is unchanged and still bright; with
  a dark lit casing inside it, the contrast is deliberate (hot
  core glow around dark metal), but if the filler washes the
  casing out at close range, the merge may want the body plasma
  rate trimmed. Not trimmed here: emitter rates were outside
  the spec and the filler stack is shared tuning.
- The wake fan's strands all root at one point just behind the
  casing, where the old two strands ran the full body length
  and read as a continuous comet body. The fan reads more as a
  burst wake. It is what the spec orders (tendrilFan of 4 as
  the wake), and the casing now carries the body read the
  strands used to fake, but it is the second thing to judge in
  game after the casing form.
- The impact fragment burst is renderer geometry, not
  entities: the five fragments hold their cone positions over
  the 5 tick collapse and fade with it, they do not fly out and
  land. That matches the audit spec (a shardBurst beside the
  existing burst) and the bolt's existing collapse language,
  but next to the real FragmentEntity conversions elsewhere in
  the mod it is the least physical piece in this slot.
- Nothing was reverted: every spec element is present, and no
  part of the old look was kept where the spec replaced it.
