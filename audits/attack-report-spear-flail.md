# Attack Upgrade Pass, Phase 2 owner report: Evil Spear and Spike Flail (spike specs)

Owner slot: Evil Spear (audit section 12) and Spike Flail (audit
section 21). HEAD ccfebf2, no commits made. Files edited, all client
renderers: HellHuntImpactRenderer, EvilSpearChargeRenderer,
EvilSpearProjectileRenderer, SpikeFlailSpinRenderer,
SpikeFlailSwingRenderer. No entity, item, or foundation file touched.
No damage, cooldown, cost, timing, mechanic, or synced field changed
anywhere in this slot: every addition reads existing renderer state
(charge ratio, release ticks, spin charge, release age, swing age)
and draws presentation only.

Steering applied (Carter, Nov 4, 15:10 and 15:11 CT): no spike set in
this slot is one even ring of identical spikes. Every set is composed
as overlapping calls at jittered radii with split size scales, so
spacing, leans, and heights land clustered and uneven, favoring
fewer, bigger, deliberate spikes. The merge's planned builder-side
irregularity upgrade will deepen this further; the compositions here
are built to the spec as written and leave room for it.

## Evil Spear

### HellHuntImpactRenderer (Hell Hunt impact)

Before: crackFan (1 call, energy) plus 4 tendrilFan lashes (veil).
Zero physical pieces.
After: the same crackFan and lashes, plus a clustered iron spike
eruption in one new submitSolidLit submit holding two spikeEruption
RING calls: 6 big spikes at full builder scale (sizeScale 1.0,
lengths 1.2 to 3.2) on radius 0.68 of the crack fan's spread radius,
and 4 shorter spikes (sizeScale 0.55, lengths 0.66 to 1.76) clustered
on radius 0.42 of it, different seed. Total 10 spikes (spec 8 to 10),
palette blackened steel (root 0xFF141519, tip 0xFF4A5058), inside the
existing crackFan as specced. Beat: RISE over age 0 to 5 ticks,
HOLD 4, retract over age 9 to 14, far inside the impact's existing
life (35 normal, 50 final). Spike vertices: 10 at 384 is 3840 at
distance scale 1.0 in the new submit; the crackFan (about 500 to 900)
and lash (about 1024 at 0.66 distance scale) submits are unchanged.

### EvilSpearChargeRenderer (charge)

Before: 5 tendrilFan strands (veil) plus 4 flame tongues (energy).
Zero physical pieces.
After: the same strands and tongues, plus Carter's body-space beat:
6 dark iron spikes in one new submitSolidLit submit holding two
spikeEruption BODY calls at body center height (local y 1.0): a main
burst of 4 at offset 0.62 from the body axis, sizeScale 0.56
(lengths 0.67 to 1.79, longest class capped at the spec's 1.8
blocks), and a shorter offset pair of 2 at offset 0.85, sizeScale
0.42 (lengths 0.50 to 1.34), phase turned 1.05 radians off the main
burst so the six cluster in two uneven groups around the body,
angled outward and up like spearheads. The rise factor is the
smoothed charge ratio itself: the spikes emerge as the charge
builds, finish emerging exactly at full charge, hold while the
charge holds, and retract along their own axes across the existing
6 tick release fade (sink keyed to releaseTicks over
RELEASE_FADE_TICKS). Charge timing and release rules are only read,
never written. Spike vertices: 6 at 384 is 2304 at distance scale
1.0 in the new submit; existing pieces about 1470 at 0.66 distance
scale, unchanged.

### EvilSpearProjectileRenderer (projectile)

Before: 2 wake tendrils (veil, about 768 vertices) plus 4 flame
tongues (energy, about 192). Total about 960.
After: flame tongues 4 to 5 (rows plus 1, about 240 vertices), plus
a shardBurst escort of 3 iron shards (root 0xFF1D2024, tip
0xFF7C848C) in a new submitSolidLit submit: cone of 52 degrees
around the real flight direction, unit 0.55 (shard lengths 0.17 to
0.88), origin at the head point, envelope identical to the tongue
cluster's body envelope, so the escort fades in at launch and out at
the end of flight with the rest of the body. Escort vertices: 3 at
about 288 is about 864. New total about 1872, inside the class M
budget of 2500 the renderer documents. Projectile speed, pierce,
and impact behavior untouched.

## Spike Flail

### SpikeFlailSpinRenderer (spin)

Before: 2 tendril arcs (energy), zero physical pieces, and the
sprite-heaviest continuous load in the set: six orbit-phase emitters
at rates 130, 58, 26, 48, 38, 15 with caps 190, 90, 34, 80, 65, 22.
After: tendrils still exactly 2 (spin strobing caution obeyed; no
spin visual multiplied), plus an iron spike eruption at the
full-speed beat in one new submitSolidLit submit holding two
spikeEruption RING calls: 7 big spikes (sizeScale 0.95, lengths
1.14 to 3.04) on the spin's own radius, and a shorter cluster of 3
(sizeScale 0.60, lengths 0.72 to 1.92) at 0.80 of that radius,
different seed. Total 10 spikes (spec 9 to 11), palette dark iron
(root 0xFF17191D, tip 0xFF565E66). Beat, all keyed to state the
renderer already had: rise rides the last eighth of the synced
charge ramp (charge 0.875 to 1.0), which is the final 4 ticks of the
32 tick channel, RISE 4; HOLD while charge stays pinned at 1;
retract over the first 4 release ticks when the spin lets go. A
tapped channel that never reaches full speed never raises the
spikes. Spike vertices: 10 at 384 is 3840 at distance scale 1.0.
Sprite trim, about a quarter, applied to the six orbit-phase
emitters only (rates 98, 44, 20, 36, 29, 11; caps 143, 68, 26, 60,
49, 17): those are the layers that play while the ring stands. The
release-phase edge emitters, settle dust, and the crack burst are
untouched, because they play after the spikes have retracted and
never compete with the ring's read. Spin radius, speed curve,
parry, and damage untouched.

### SpikeFlailSwingRenderer (swing)

Before: 3 tendrils (two arc strands plus echo), 27 sprite emit
calls, zero physical pieces.
After: 5 tendrils: the original 3 arc strands unchanged, plus a
tendril pair on the chain line (widths 0.16 and 0.10, hand point to
ball, control point the midpoint dropped 0.05 for a swung chain's
sag), drawn in the same tendril submit. Plus a shardBurst of 4 iron
shards (root 0xFF1D2024, tip 0xFF7C848C) in a new submitSolidLit
submit at the swing's apex: origin at the ball sample, cone of 58
degrees around the ball's sampled travel direction (last two trail
samples), unit 0.55, envelope weighted by sine of swing progress,
peaked at half progress and zero at both ends. The height apex is
exact, not approximate: ballPos height rides sin of the eased
progress, and the entity's ease is smoothstep, which crosses 0.5 at
raw progress 0.5, so the burst peaks on the same frame the arc
peaks. Shard vertices: 4 at about 288 is about 1150 at distance
scale 1.0, in their own submit. Swing timing and damage untouched.

## No-glow gate

Every new physical piece in this slot (30 spikes, 7 shards) draws
through ComplexGeometry.submitSolidLit with the render state's
lightCoords. Verified by grep over the five files: the only
submitSolidLit calls are the five new ones, and every spikeEruption
and new shardBurst call sits inside one. The pre-existing
submitEnergy and submitVeil calls carry only energy pieces
(crackFan, flame tongues, tendrils), unchanged in kind.

## Verification

Targeted javac, JDK 25 at ~/workspace/tools/jdk-25.0.5+8, sourcepath
src/main/java:src/client/java, over the five owned files, full logs
read on both runs (the composition rework was re-verified after the
steering arrived, so both implementations passed):
- cp262: 4 errors, all standing baseline kinds (DbaItems 2,
  DbaMenus 2). Zero errors in owned files.
- cp263: 10 errors, exactly the documented baseline set (DbaItems 2,
  DbaMenus 2, DbaHostileEntity 2, PatrolRobotEntity 2,
  FriezaSoldierEntity 1, NamekianWarriorEntity 1). Zero errors in
  owned files.
git diff --check clean on the five files. Byte-level em dash and en
dash scan over added lines in the five files: zero. No commits. No
Gradle build run or claimed; the in-game look is unverified until
Carter's PC test.

## Change requests (foundation is read-only for this slot)

1. BODY band fit: the charge spec asks for spikes of 1.2 to 1.8
   blocks, but the builder's length classes span 1.2 to 3.2 times
   sizeScale (a 2.67 spread ratio) while the band's ratio is 1.5,
   so no single sizeScale puts all three classes inside the band.
   Resolved owner-side by capping the longest class at the band
   (0.56 gives 1.79 max) and accepting a shorter small class (0.67
   to 1.06). If the band should hold for every spike, the builder
   needs a tighter class spread option for BODY layouts.
2. Support for the merge's announced irregularity upgrade: RING
   placement jitter in the current builder is plus or minus 0.15
   radians with leans of 0.15 to 0.42, so owner-side clustering via
   split calls and split radii does real work but cannot produce
   tight grouped bursts on one radius. Recorded so the merge upgrade
   knows the owner compositions are ready to inherit deeper
   clustering without rework.
3. No other foundation gap blocked any spec item in this slot.

## Honest worse check

- Visual intersection, Spike Flail spin: the ball orbits at the
  spin's radius at height 1.15, and the main spikes stand on that
  same radius leaning outward. The lean moves most spike mass out
  of the orbit cylinder by ball height (about 0.17 to 0.47 blocks
  out at 1.15 high), but the tallest, least-leaned spikes will
  still cross the ball's path briefly each orbit. The audit specced
  the ring at the spin's radius, and a flail smashing past its own
  spike ring may read as violent rather than wrong, but this is the
  first thing to judge on the PC test. Named, not hidden.
- Hell Hunt spike height: the outer set runs at full builder scale
  per the fewer-and-bigger steering, so its top class stands 3.2
  blocks around a radius 3 impact, taller than the tendril lashes
  (about 2.5 long). It reads as a crown of iron around the scar,
  which is the intent, but if any set in this slot reads oversized
  in game, it is this one.
- Regularity remainder: clustering here is composition-level (split
  calls, radii, scales, seeds) on top of the builder's modest
  built-in jitter. Until the merge's builder irregularity upgrade
  lands, a frozen frame of any ring in this slot will still show
  its ring structure on inspection. The steering's full clustered
  look depends on that upgrade arriving; this slot does not claim
  it early.
- Charge pair at range: distance scaling floors the offset pair at
  2 spikes (the builder's minimum is min(3, count)) while the main
  four can thin to 3, so at long range the composition leans
  slightly more on the pair than at close range. Subtle, and the
  pair is the shorter set, so the far read stays quieter overall.
- Projectile escort formation: the three escort shards hold a
  fixed seeded formation around the flight axis (renderer drawn,
  as the audit specced a shardBurst), so they fly in formation
  rather than tumbling like loose debris. If Carter wants them
  tumbling free, they would need to become real entities, which is
  a different and bigger change than this spec.
- Sprite trim tradeoff: while the flail ring stands, the orbit's
  dust and vapor air read is about a quarter thinner and leans on
  the two tendrils and the ring itself. The release crack, which
  is the spin's payoff, is at full strength as before.
- Nothing in this slot was reverted: no change made a weapon look
  or play worse by the code evidence available here, and the two
  judgment calls above (spin intersection, Hell Hunt crown height)
  are presentation risks for the PC test, not known regressions.
