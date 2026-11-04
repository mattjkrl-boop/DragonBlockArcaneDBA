# Attack Upgrade Pass: Final Audit

Scope: the attack upgrade pass on top of the entity objects pass
(8594bf8). Three commits land together: 1926d34 (Phase 1: the glow
fix plus the attack complexity audit), ccfebf2 (Phase 2 foundation:
rockWall and spikeEruption), and this merge commit (15 owner
conversions, the merge coordinator's central foundation work, all 15
owner reports, and this audit). Carter's directives for the pass:
launched blocks must not glow, attacks must use more and more
complex shapes (walls heaving out of the ground, spikes erupting,
nothing boring), spikes must not read as a uniform spike bar, walls
must read as a rocky mountain heaving out, not a neat slab row,
everything realistic at movie quality, and one small thing must not
be built from a thousand tiny shapes. Carter approved the 22 concept
pictures at ~/workspace/dba-attack-concepts/ ("All good", Nov 4,
2026); those pictures are the locked visual basis checked in the
picture gate below.

Verification standing line, stated plainly: there is no Gradle build
in this VM (sandbox loopback restriction, pre-existing), and none is
claimed. Verification is the dual javac audit sweep against a fresh
baseline, targeted javac on every merge-touched file, a vertex
counting harness, and code reading. The in-game look is unverified
until Carter's PC test.

## Phase 1: the glow fix (1926d34)

Carter's report: "I dont want the glowing applied to the blocks
launching and that happens I bit too much." Root cause, verified
against the 26.2 client jar (full record in
audits/attack-glow-fix-report.md):

1. DebrisRenderer submitted the real block model through
   BlockModelRenderState.submit with the entity render state's single
   packed lightCoords. BlockModelFeatureRenderer stamps that one
   light value on every quad: no light-engine sampling, no ambient
   occlusion, no per-face shading, so launched chunks wore full
   outdoor brightness uniformly and read as glowing. The path was a
   Bespoke pass trade; the original falling-block terrain path was
   correct. DebrisSpawner.spawnField has 20+ call sites, which is
   why it "happens a bit too much".
2. FragmentRenderer drew every fragment through the emissive solid
   path with vertices stamped FULL_BRIGHT, ignoring world lighting
   even after landing. GrandBladeShardRenderer had the same defect.

Fix: DebrisRenderer is back on the moving block terrain path; a new
world-lit path (ComplexGeometry.submitSolidLit plus KiRenderHelper
light plumbing) carries physical shards, which now draw lit. One
rule everywhere after this pass: physical matter uses world
lighting. Glow accompaniment at launch sites was checked and
cleared: SlamDebris block chunks and DUST_PUFF are world-lit terrain
particles, LavaField crust launches are world-lit, and the plasma at
launch centers is each weapon's own burst, which stays.

## Gates

### Ownership gate: PASS

Every changed file belongs to exactly one of the 15 owner reports
(verified programmatically against all 15 reports; the two apparent
double mentions are prose cross-references, not edits). Foundation
files ComplexGeometry and KiRenderHelper show zero owner
modifications. The coordinator's own changes are listed under
Central foundation work below and touch only: ComplexGeometry, the
addendum, two renderer call sites (crackFan retrofits), two new
common utility classes, and TemporalRiftEntity's drop placement.

### No-mechanics gate: PASS

The only main-source edits in the whole pass are the four entity
files the owners documented, each read in full at merge:
OxFissureEntity (two DebrisSpawner.spawnField beats, additions
only), WarMaceSmashEntity (one armed DebrisSpawner.spawnField of 12
chunks at the slam tick, additions only), DimensionalRiftEntity
(one FragmentEntity.burst of 4 at the peak tear tick, additions
only), and TemporalRiftEntity (the spike drop bursts). No damage
value, cooldown, cost, timing, mechanic, or synced field changed
anywhere. PermanentDestruction and all destruction code are
byte-identical to ccfebf2 (absent from the diff). The coordinator's
TemporalRiftEntity edit changes only the drop bursts' placement
math and their seed salts (now the renderer's own salts); burst
counts, speeds, gravity, lifetimes, and the reversal tick are
untouched.

### No-glow gate: PASS, with two recorded judgments

Every rockWall call added in this pass (Grand, Bansho cone front and
backing, Bansho radial arcs, War Mace rim arcs, Z curtain in both
renderers, Ox fissure masses, the new Lava field wall) draws through
submitSolidLit. Every spikeEruption call draws through
submitSolidLit, with one deliberate exception: Dabura's eclipse rim
carries an aligned submitVeil shell duplicate at alpha 0.22 over the
lit body, the audit-specced violet edge seam, never the body. Every
new shard or shardBurst in a physical palette (Blaster casing
petals, Grand valor shards, Z charge shards, Katana and Brave steel,
Saber slivers, Power Pole stone chips, Spirit Cleave soul crystal
spikes and shards, Lava basalt shards, Spike Flail iron, Evil Spear
iron escort, Hell Hunt and spin spikes in blackened steel) draws
through submitSolidLit. The shardBursts that stay on the emissive
solid path are the audit-sanctioned energy materials: Bansho
wind-glass, CurseChain and SkyCracks curse glass, Dimensional Slash
warp glass, Erasure orb crystal, Sorrow Slash violet crystal, Void
Slash void glass, and the Blaster bolt's single emissive hot core
inset inside its lit casing.

Two judgments, recorded plainly:

1. OxShockwaveRenderer's new inner fracturePlates ring and
   WarMaceRiseRenderer's new outer plate ring sit on their families'
   pre-existing submitSolid path, not submitSolidLit. Decision:
   KEEP, by the coordinator. Reasons: the audit, which names
   submitSolidLit explicitly wherever it mandates the lit path,
   specs these two as count additions inside existing families
   ("plates count plus one inner ring"); the Phase 1 glow fix
   deliberately scoped the lit conversion to world objects (debris,
   fragments, blade shards) and left the bespoke plate families on
   the solid path; splitting one plate composition across two light
   models is the worse outcome; and War Mace Rise's palette is the
   ghost palette (a spectral composition, inside the gate's energy
   exemption). The War Mace owner report's blanket sentence "every
   new physical piece draws through submitSolidLit" is inaccurate
   for its rise ring and is corrected by this note. If Carter's PC
   test says these plates glow, the relight is a two-call change per
   renderer, following the Z Shockwave precedent (move the whole
   family at once).
2. Spirit Impale's shards stay on submitSolid. Decided per the
   owner's soul-energy reasoning, which stands: the impale ring is a
   soul composition, not physical crystal.

### Picture-check realism gate: 22 of 22 MATCH

Each shipped composition was compared against its approved concept
picture (all 22 pictures read at merge). MATCH means the picture's
signature elements are present in the build.

| Weapon | Verdict | Signature elements checked |
| --- | --- | --- |
| Katana | MATCH | Dash crescents with steel shard clusters and a ground crack fan; Heaven Splitter's towering split with wall-face fan and crystal crown |
| Z Sword | MATCH | Gravity curtain wall on the slam's front edge; dual plate rings and full-radius fan on the shockwave; suspended charge shards that drop and shatter |
| Saber | MATCH | Dimensional line slash crossing targets with an in-air void crack fan (now exactly 4 branches after the merge retrofit); composed crown-and-flanks Void Tear shards |
| Brave Sword | MATCH | Cross slash shard bursts on both diagonal planes; flame tongue ring on the shockwave; rush trail shard shed |
| Grand Sword | MATCH | Crescent Wave's bowed rock wall front with orbiting valor shards and top-edge flame tongues; Valor dome with suspended projectiles (picture's frozen arrows); clash spark fan and burst |
| Azure Dragon Sword | MATCH | Tempest channel storm-glass ring; storm fans with rim shard shed; mirrored dual quake fans; densified lightning telegraph |
| Dabura's Sword | MATCH | Eclipse rim of black spikes (clustered after the merge upgrade) with the violet seam; darkness wave fan converging to one origin; shatter's second plate ring |
| Cursed Blade | MATCH | Ground Shatter obsidian spike scatter; SkyCracks heart refilled with a hanging burst over the fragment rain; chains' paired tendrils with anchor burst. The picture's pillar stand-in was approved as-is |
| Hollow's Edge | MATCH | Void Slash tendrils and void shard burst; afterimage fan; the rift's real fragment ring at the rim |
| Dimensional Sword | MATCH | Slash's five tendrils, in-plane fan, and crystal end shards; mirrored arrival fan on warp rifts |
| Spirit Sword | MATCH | Cleave rebuilt as a woven blade with leading-edge shards and a soul crystal spike fan at the hit point (the picture's crystal burst at the arc's foot); Impale at six shards; cannon terminus fan. The montage framing was approved as-is |
| Evil Spear | MATCH | Hell Hunt's two-tier spike ring on impact; body-space spearhead spikes bursting around the caster at full charge (Carter's own steering, matched exactly); projectile flame tongues and iron escort |
| Devil Trident | MATCH | Deploy slam's brimstone plates and magma web kept at their deliberate radii under a clustered ring of 11 blackstone spikes; the shard swarm (the lore) untouched; prong tongues up to four per cluster |
| Sickle of Sorrow | MATCH | Sorrow Slash tendrils, crystal burst, and ground-gated fan; the rift's second real fragment burst at the peak tear (nine fragments per rift) |
| Ox King's Ax | MATCH | Fissure wall as two overlapping rock masses over the trench with debris beats at rise and slam; Kings Slam rim spikes; shockwave's second plate tier. The seal glyphs were approved as-is |
| Power Pole | MATCH | Whirlwind fan at seven strands; impact rebuilt with dual fans, a low plate ring, and stone chips at the tip burst |
| Whis Staff | MATCH | Temporal Rift glass spike ring (two uneven clustered rings) that drops as real fragments on reversal, now at the drawn positions; Time Shatter's second counter-rotated fan |
| Bansho Fan | MATCH | Shockwave push wall: leading rock row with a crumbling backing ridge in cone mode, three uneven arcs in radial mode; wind projectile glass head; gale tendrils at five |
| Blaster Gun | MATCH | The bolt is a sabot casing of four lit gunmetal petals around an emissive core (the picture's cased shells with glowing cores); impact casing burst; cannon's second fan; charge orb's orbiting crystal pair |
| Lava Sword | MATCH | Basalt slab wall standing at the eruption field's edge with a magma vein; cleave's second flame row and basalt shards; eruption's layered dual fans |
| Spike Flail | MATCH | Spin ring as two clustered spike tiers at the orbit (the picture's ground tufts); swing tendrils and apex burst |
| War Mace | MATCH | Crater rim wall as two clustered rock arcs covering about 167 degrees, not a full ring (the picture's one-sided slab rim); debris burst armed at the slam tick; rise's second heave ring |

No GAPs. Nothing toy-like, even, or fence-like remains by
construction after the irregularity upgrade below; whether it reads
that way in game is Carter's PC test.

## Central foundation work (coordinator)

### 3a. Irregularity upgrade: DONE

Inside ComplexGeometry, signature-compatible, so every host
upgraded at once:

- spikeEruption placement is clustered in LINE, RING, SCATTER, and
  FAN. RING angles and radius fractions come from a new common
  helper, util/SpikePlacement: contiguous tufts of two to four
  spikes (cluster count 1 to 4 by ring size), jittered cluster
  centers, in-cluster spacing of 0.13 to 0.24 radians, per-spike
  micro jitter, and radius variance of plus or minus 7 percent.
  LINE compresses contiguous groups toward jittered centers;
  SCATTER pulls each spike to a third of its rolled offset from one
  of two to four seeded cluster centers; FAN bursts as two or three
  sub-wedges. Ground layouts also multiply lengths by a seeded 0.84
  to 1.18, so no two neighbors stand alike. BODY is unchanged: its
  host caps the longest class against a fitted band, and its owner
  already splits the call for clustering. The Devil Trident ring
  (11 spikes) and the Dabura rim (10) were even rings; both now draw
  as three to four tufts with bare rim between.
- rockWall slab heights ride a grouped ridge profile: three seeded
  peak clusters plus rolling noise set each slab's height factor
  (about 0.60 to 1.24 of nominal, clustered, instead of independent
  0.70 to 1.16 coin flips), and per-slab tilt, lean, face offset,
  and bulge ranges widened. The wall reads as a heaved ridge line,
  not a slab row.
- Determinism preserved (same seed, same output; the counting
  harness's determinism probe agrees). Vertex counts per piece are
  UNCHANGED, machine-verified: a counting VertexConsumer harness ran
  both builders on the ccfebf2 baseline tree and on the integrated
  tree across sampled owner calls and every layout and count; the
  outputs are byte-identical (72 vertices per slab at 6, 9, 7, 10,
  6, and 14 slabs; 384 per spike at counts 3, 5, 8, 11, 14 in all
  five layouts). Only placement and profile math moved.
- The RING stream change is recorded: the angle's old rng.signed()
  draw is gone (placement is hash-pure now), so length and girth
  rolls inside RING calls re-roll to distribution-identical values.

Supporting refactor: ComplexGeometry's private mix, hash1, and
noise1 now delegate to a new common util/SeedHash holding the exact
same splitmix math, so server code reproduces client hash values
bit for bit. Every existing hash consumer's values are unchanged by
construction (identical formulas).

### 3b. Arc sign reconciliation: DONE, zero call flips

The Grand Sword owner proved from the placement math that
rockWall's shipped convention is the opposite of its javadoc: the
wall's center stays at origin and the ends swing, so a POSITIVE arc
swings the ends toward facing and leaves the center recessed
(concave from the facing side); NEGATIVE bows the center toward
facing (convex). The math was NOT flipped (owners compensated in
their calls). The javadoc in ComplexGeometry and the arc paragraph
in audits/attack-foundation-addendum.md now state the shipped
convention plainly. Every owner's arc usage checked against its
stated intent, all in the shipped code:

- Grand Crescent Wave: effective -0.8, convex bow toward the wave's
  travel, matching the skin's own bow. Correct, no flip.
- Bansho cone front and backing rows: -0.45 each, center leading (a
  plow). Correct, no flip.
- Bansho radial arcs: minus length over current radius with poses
  facing outward, so each arc hugs the ring's circle. Correct, no
  flip.
- War Mace rim arcs: plus length over radius with slabs facing the
  crater center, which wraps the wall exactly onto the rim circle
  (the builder's arc circle centers on the crater). Correct, no
  flip.
- Lava field wall: plus length over radius (about 1.31) with facing
  back toward the eruption origin; same wrap geometry, the wall
  lies on the pool lip circle. Correct, no flip.
- Ox fissure masses: main +0.15 and shoulder -0.12, deliberate
  slight opposing bows forming the clustered ridge; neither carries
  a wrap or convex claim to contradict. No flip.
- Z gravity curtain: +0.45; the stated intent is a bowed clustered
  front, which either sign satisfies, and the facing is position
  hash seeded by design. No flip.

### 3c. crackFan branch-count overload: DONE, two retrofits

crackFan gained an explicit base-count overload following the
existing deterministic fork replay contract (each crack's walk and
fork seeds derive from the fan seed and crack index alone; the
standard overload's behavior is byte-identical, now delegating to a
shared impl with its original base of 9 and its density jitter).
Retrofitted, per the merge brief, only the two sites whose specs
were numerically explicit:

- SaberDimensionalLineSlashRenderer: specced 4 short branches, was
  drawing the internal 9 to 11; now base 4.
- SwiftCrescentRenderer (Katana): specced 3 short branches, was
  drawing 6 to 11 by radius; now base 3.

All other densify workarounds stay as shipped (detail-ratio boosts
in SkyCracks, Abyssal Domain, Warp Rift, Azure Lightning, War Mace
Rise; layered second fans in Lava Eruption, Blaster Cannon,
Dimensional Slash, Z Shockwave; mirrored poses).

### 3d. Temporal Rift drop alignment: DONE at the root

The mismatch: drawn spike positions came from builder-internal RNG
while TemporalRiftEntity's reversal drop used an independent seeded
take (even angles, phase (salt and 7) times 0.35), so fragments
could land up to about a radian from the drawn spike at rim radius.
Fix: RING placement now lives in the shared util/SpikePlacement
(pure functions of seed, index, count), the client builder consumes
it, and dropSpikeRing consumes it with the renderer's own seed
salts (0x7E51 main ring, 0x7E67 outer cluster, replacing the
entity's private 0x7E44 and 0x7E58), so each fragment drops from the
base of the spike the player was looking at. Machine-verified: a
recording-consumer harness rendered the ring at four seeds and
counts (5, 11, 3, 10) and compared every spike's centroid angle
against SpikePlacement.ringAngle for the same seed, index, and
count; all agreed within 0.05 radians (worst seen 0.0032). The one
remainder, recorded as a PC-test watch item: at reduced client
distance detail the builder draws the ring regrouped around fewer
spikes, while the drop always uses the full count's positions,
which are the persistent truth.

### 3e. Budget and read judgments: all KEEP

- Dabura eclipse rim: 3840 lit verts plus the 3840-vert veil shell
  (about 7680 total at distance scale 1.0), each submit inside the
  class L budget of 6000. Keep: the shell is the picture's violet
  edge seam; thinning it means fewer spikes or a smaller shell,
  both of which change the approved read.
- VoidSlash and HollowAfterimage energy submits at about 3072 verts
  at distance scale 1.0: inside budget beyond close range. Keep.
- PowerPoleWhirlwind's seven strands at about 2688: keep.
- Blaster whole-effect sum about 19 percent over a whole-effect
  reading of class M, with every per-submit count inside: keep (the
  audit's own specced consequence, disclosed in the owner report).
- Devil Trident slam at about 5630 of 6000: keep, margin noted in
  the flags below.

## Per-weapon outcomes (owner work, piece counts)

- Devil Trident: slam pieces about 1400 to about 5630 of 6000
  (brimstone plates at 0.90 and crack web at 0.95 of seal radius
  untouched, plus the new 11-spike ring). Prong flame tongues 3 to 4
  per needle cluster. Swarm untouched.
- Grand Sword: Crescent Wave about 770 to about 4090 verts (6-slab
  rock front, 2 seam tendrils, 6 orbiting valor shards, 5 flame
  tongue clusters, chip and dust shed). Clash Spark about 1150 to
  about 3750. Blade Shard wake doubled.
- Blaster Gun: bolt flight 2976 verts across three submits (4
  casing petals, 1 core shard, 4-strand tendril fan wake); impact
  casing burst of 5; charge orb fan 5 to 7 plus orbiting pair;
  cannon second fan.
- Ox King's Ax: fissure wall 15 slabs across two masses (9 main, 6
  shoulder), debris beats of about 10 chunks at tick 2 and 14 at
  tick 12; Kings Slam 6 rim spikes (2304 verts) plus inner fan;
  charge fan 6 to 8 strands with the ember layer at parity.
- War Mace: rim wall 12 slabs in two clustered arcs; armed debris
  burst of 12 chunks at the slam tick; rise second plate ring and
  fan branches 9 to 11 at full detail; sprite trims of about 10
  percent on the six largest smash emitters.
- Z Sword: gravity curtain 8 to 10 slabs drawn by two renderers
  with a seamless handoff at the launch tick; shockwave dual plate
  rings (family moved to lit by its owner) plus second fan; charge
  4 suspended stone shards, about 3000 verts at distance scale 1.0.
- Katana: SwiftCrescent second arc, 6 steel shards in 3 clusters,
  ground-gated fan (now exactly 3 branches). Heaven Splitter crown
  of 6 crystal shards and a wall-face fan. Charge and aim guides
  carry 3 de-ringed orbiting shards each.
- Brave Sword: CrossSlash 8 shards across two planes; Slash
  tendrils 2 to 3 plus apex burst; Rush trail shed of 2 shards
  every 6 ticks; Shockwave 9-tongue flame ring; Charge fan 5 to 7.
- Saber: Slash 5 authored steel slivers (a form fix: burst rolls at
  the same count read as pebbles); line slash tendrils 2 to 4 plus
  the fan retrofit above; Void Tear composed 1 crown plus 2 flanks.
- Hollow's Edge: Void Slash tendrils 2 to 4 plus burst of 4;
  Rush trail chip shed pair; Afterimage fan 3 to 5; rift rim
  tendrils 2 to 3, with the optional drawn fragment ring
  deliberately not added over the 5 real fragments.
- Azure Dragon Sword: channel fan 3 to 5 plus 6 orbiting
  storm-glass shards; storm fans 2 to 4 with a 4-shard rim shed;
  quake dual fans via mirrored pose; rush trail streak layer added
  at 52 per second; telegraph density up by 1.22.
- Dabura's Sword: eclipse rim 10 spikes in two submits (lit body
  plus veil seam); darkness wave fan 3 to 4; blade 3 dark steel
  shards on the roll arc; shatter second plate ring on lit.
- Cursed Blade: Ground Shatter 8 obsidian spikes (scatter, size
  scale 1.25) plus plates at 1.2 radius; SkyCracks heart burst of 5
  pulsing on the rain's shed phase; chain paired tendrils plus
  anchor burst; domain fan density up.
- Dimensional Sword: slash tendrils 3 to 5, in-plane fan, 4 crystal
  end shards (about 3300 verts, class L); warp rift fan density up
  plus a mirrored arrival fan; fragment spawns untouched.
- Evil Spear: Hell Hunt 10 spikes in two tiers (3840 verts);
  body-space spikes 4 plus 2 offset at full charge, emerging on the
  smoothed charge ratio; projectile tongues 4 to 5 plus 3-shard
  iron escort.
- Spike Flail: spin ring 10 spikes in two tiers rising over the
  last eighth of the charge ramp; swing tendrils 3 to 5 plus apex
  burst of 4; orbit-phase sprite trims of about a quarter.
- Sickle of Sorrow: Sorrow Slash tendrils 2 to 4, burst of 5,
  ground-gated fan; rift second fragment burst of 4 at tick 20
  (nine real fragments per rift); rift tendrils 3 to 4.
- Power Pole: impact rebuilt (original fan, mirrored half fan,
  plate ring of 6 to 8 at low lift, burst of 5 stone chips);
  extension tendrils 2 to 3 plus streak shed; whirlwind fan 5 to 7.
- Spirit Sword: Cleave rebuilt (5-strand woven fan, 4 leading-edge
  shards, 5 soul crystal spikes at the hit point; 4992 verts across
  three submits, each inside class M; soul streams doubled);
  Impale at 6 shards with the main shard a size class up; cannon
  terminus fan 6 to 7 (5760 of 6000).
- Whis Staff: Temporal Rift ring 8 glass spikes in two uneven
  rings (drawn 1920 plus 1152 verts), dropping as exactly placed
  fragments after 3d; Time Shatter second counter-rotated fan;
  Bansho work in its own rows below.
- Bansho Fan: shockwave push wall (cone: leading row plus backing
  ridge; radial: 3 uneven arcs plus backing arcs), whole wall
  leaning 0.16 radians into travel, palette sampled from the ground
  under the wave; gale tendrils 3 to 5 (1920 verts); wind
  projectile tendrils 2 to 3 plus a 3-shard glass head; cyclone
  untouched per spec.
- Lava Sword: new LavaFieldWallRenderer (7 basalt slabs, 504 verts,
  plus one magma vein tendril) standing at pool-stage fields on the
  field's 200-tick clock; eruption fans 1 to 2; cleave flame rows
  1 to 2 plus 4 basalt shards. All main-source lava files
  untouched; the is_fire design question is untouched with them.

## Verification

- Dual javac sweeps, fresh baseline worktree at ccfebf2 vs the
  integrated tree, unique (file, message) error kinds: 26.2 gives
  132 kinds on both trees; 26.3 gives 138 on both; comm -13 prints
  nothing on either classpath. Zero new error kinds.
- Targeted javac on every coordinator-touched file, both
  classpaths, full logs read: exactly the known baseline closure
  kinds (4 on 26.2, 10 on 26.3), zero errors in any touched file.
- Counting harness: vertex counts identical pre and post the
  irregularity upgrade (72 per slab, 384 per spike); determinism
  probe identical; placement harness: drawn ring angles match the
  shared helper within 0.05 radians at four seeds and counts.
- Resources: 567 JSON files parse, zero failures.
- git diff --check: clean.
- Em dash scan (byte level): zero across the full diff, all 15
  owner reports, the addendum, the new files, and this audit.

## Deferred items, with reasons

- Converging tendrilFan variant, tendrilChain, tendril phase
  parameter, crackFan plane basis and fork-sign parameter,
  shardBurst distribution variants, flameTongues direction
  alignment: complex-pass deferrals, unchanged by this pass.
- rockWall tilt toward a host up vector and an orbit or escort
  helper (Grand): builder features, not needed by any shipped
  composition.
- A flat plate builder (Blaster casing is built from shard plates
  because fracturePlates cannot wrap a body axis): foundation gap,
  named for a future foundation step.
- tendrilFan per-strand origin offsets (Azure and Dabura fans
  converge at a single origin by design today).
- fracturePlates rim band and plate-count parameters; spikeEruption
  chunkier girth class and a BODY band spread option; per-slab
  ground following and slab boundary exposure for rockWall (Lava
  seam placement); a server hook for real DebrisEntity chunks at
  the Lava wall settle (today's settle crumble is client block
  chunks).
- Power Pole whirlwind spin halving: its owner gated it on a
  playtest judgment; not applied.

## Owner flags carried (unedited in substance)

- Z Sword: empty-ground slams (no launch targets) have no
  DelayedLaunchEntity, so the slam renderer drops a fully risen
  curtain with a pop-in at full height for 4 ticks; curtain facing
  is position hash seeded, not caster facing.
- War Mace: the rim wall covers about 167 degrees by design, not a
  full ring; no pre-impact telegraph entity exists for the ground
  smash (impact resolves server side at tick 1), so the rise keys
  to the renderer composition's own build; rockWall sink is
  vertical only.
- Ox King's Ax: ridge crowding across six simultaneous max-charge
  fissures is the first PC-test judgment; the shoulder mass is the
  cheapest dial. The fissure wall lives only the opening 15 of the
  fissure's 100 ticks, per spec.
- Devil Trident: tallest spike tips can lean about 1.3 blocks out
  near the seal edge; the slam's budget margin is the slimmest in
  the kit (about 5630 of 6000), and distance scaling floors the
  ring count at 3.
- Spike Flail: the orbiting ball crosses the tallest, least-leaned
  spikes briefly each orbit (specced radius; first PC-test
  judgment).
- Temporal Rift: the drawn ring stands at full radius from tick 0
  while the field edge unfolds over 15 ticks (kept to preserve
  agreement with the server drop); the reversal handoff remainder
  is the distance-detail regrouping noted in 3d.
- Lava Sword: a field wall forms on a client only if that client
  rendered the eruption once (sighting dependency); the settle
  crumble is client block chunks over about a 12 tick window, not a
  server DebrisSpawner burst.
- Blaster Gun: the casing's first cut read as a flower bud and was
  rebuilt on sabot proportions; the flat-plate foundation gap
  stands (see Deferred).
- Grand Sword: pitched shots can peel the wall front off the tilted
  skin; low waves plow slab skirts through the ground; the big
  escort shards cross the skin plane twice per orbit.
- Spirit Sword: on full flights the cleave's spikes begin rising
  during the last 4 ticks of travel (telegraphing), the cost of
  fitting the beat inside the fixed 19-tick host life; early wall
  hits restart the rise at the true hit point with only the fade
  ticks remaining.
- Saber: Void Tear's count went 5 to 3 by composition (the audit
  counted the starting point as 1 shard; the code drew a 5-shard
  fan), and the continuous ASH_FLAKE rate was not doubled (the
  complex standard caps continuous complex sprite emission at 40
  per second per emitter; the burst at the mouth was doubled
  instead).
- Dimensional Sword: the slash's in-plane fan lands at 6 to 8
  branches because the standard builder's floor is 6 (the explicit
  overload now exists if a future spec wants exactly 5).
- Cursed Blade: denser SkyCracks and Abyssal fans also lengthen
  per-crack walks slightly (segments rise with detail); Ground
  Shatter's plates at 1.2 radius grow proportionally, outer edges
  reaching about 1.32 nominal (the dial is 1.1 if the PC test shows
  floating).
- War Mace Rise: the crackFan boost lengthens per-crack walks at
  full detail (segments 6 to 7).
- Bansho Fan: the wind projectile's tendril control points were
  re-spaced from an opposite pair to an even triple, so the old
  pair's exact twist character is gone; drawn wall rides the
  renderer's sine-eased front while the damaging rim is linear
  (pre-existing divergence, inherited on purpose); radial arcs wear
  the origin's palette across mixed terrain; steep cone pitches can
  float or bury the wall.
- Dabura's Sword: the veil seam shell carries an outline risk at
  some angles, and stacked domains at close range are the load
  risk.
- Power Pole: mid-air impact hits show the solid plate disc
  floating for 14 ticks; plate count is distance driven (10 at
  point blank).
- Sickle of Sorrow: the audit's "4 more bursts" for the rift was
  read as one burst call of 4 fragments; flagged in its report.

## Honest lines

- No Gradle build exists in this VM and none is claimed; the
  compile evidence is the javac audit above.
- The in-game look of every composition in this pass is unverified
  until Carter's PC test. The PC-test watch list: the two plate
  families kept on the solid path (Ox Shockwave, War Mace Rise),
  Ox ridge crowding, Devil Trident spike tip reach and budget
  margin, Spike Flail ball crossing, Temporal Rift reversal at
  reduced distance detail, Grand pitched-shot peel, Z empty-ground
  curtain pop-in, and Dabura stacked domains.
