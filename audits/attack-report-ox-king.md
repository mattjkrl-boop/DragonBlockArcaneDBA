# Attack Upgrade Pass, Phase 2 owner report: Ox King's Ax (flagship wall)

Spec: audits/attack-complexity-audit.md section 15, plus the builder
contract in audits/attack-foundation-addendum.md (worked example 1 is
this slot's fissure wall). Steering applied during the work: the wall
must read as a rocky mountain heaving out of the ground, jagged and
clustered, never a neat row of even slabs, and both the wall and the
spikes are held to a physical believability bar (form, material,
weight) over game-looking shapes. HEAD ccfebf2, no commits made.
Files changed, all Ox King's Ax only: OxFissureRenderer,
OxShockwaveRenderer, KingsSlamRenderer, OxChargeRenderer,
OxFissureEntity. No other file touched; the rest of the working tree
belongs to the other Phase 2 owners.

## OxFissureRenderer: the flagship wall

Before: pieces layer was 1 fracturePlates call plus 2 crackFan calls
(the trench web); 6 continuous material emitters; no wall, no spikes.
After: the fissure has a rock wall body, built from TWO overlapping
rockWall calls, both through submitSolidLit with the render state's
lightCoords (physical stone on the lit path; nothing new was added
on the emissive path):

- Main mass: length 10.0, height 3.0 (top of the audit's 2.2 to 3.0
  band), arc 0.15, centered at z -0.6 along the line. 9 slabs at
  distance scale 1.0 (10 / 1.15), about 648 vertices.
- Shoulder cluster: length 6.0, height 2.2 (bottom of the band),
  arc -0.12, centered at z +2.4, pose rotated 5 degrees across the
  main mass so the two ridges overlap at a slight angle instead of
  continuing one straight line. 6 slabs (the builder's minimum),
  about 432 vertices.
- Combined span about 11 blocks of clustered ridge; total about
  1080 wall vertices at distance scale 1.0, well inside the class M
  budget and the addendum's measured wall costs (72 per slab).

Composition choices, per the steering: fewer, bigger, deliberate
pieces over many small ones (15 slabs total across two masses, each
about 1.0 to 1.1 blocks wide and up to 3 blocks tall); uneven
heights, lengths, arcs, and angles; the shoulder overlaps the main
mass's span so the silhouette is a clustered ridge line, not a
fence. Palette: the fissure plates' earth family pulled grayer and
darker (main root 0x2B241E to tip 0x77644C; shoulder root 0x262019
to tip 0x685741), because saturated warm browns read as toy rock at
wall scale; real broken stone is low-chroma with a dark buried base
and a weathered top. Form depth beyond this (per-slab silhouette
irregularity) rides on the merge's planned foundation irregularity
upgrade, as the steering directed; this composition is built to the
spec as written and leans clustered wherever the owner has
discretion.

Beats, on the fissure's existing timing with its damage beat
untouched underneath: RISE staggered slab by slab (each mass rides
the builder's uniform-drop contract, so taller slabs break the
surface first; main mass rise runs 7 ticks, shoulder 8 ticks, the
slower end of the band for weight), HOLD 4 ticks at full height
(both factors parked at rise 1, sink 0 through tick 12), SLAM flat
over 3 ticks from tick 12 (sinkFactor, ease-in inside the builder).
At the slam the renderer adds a full-length crackFan (radius 5.2,
the trench's own hot palette, submitEnergy) that then lives under
the fissure's normal fade like the trench's web. Crack fan calls:
2 before, 3 after.

Orientation note: the addendum's worked example passes the fissure
direction as the wall facing while stating the wall line runs along
the fissure path. In this renderer's local frame those two cannot
both hold (rockWall's line runs along facing's right), so the
stated outcome governs: facing points across the trench (local
+X), which puts the line along local +Z, the fissure path. Flagged
here so the merge sees the reading, not just the code.

DUST and debris accompaniment: two one-shot MaterialEmitters dust
bursts on the renderer's existing crack-line source (36 shed along
the line during the rise window, 30 at the slam tick). The real
DebrisSpawner chunks are server side by construction, so they live
in OxFissureEntity on the same beats (below).

## OxFissureEntity: DebrisSpawner beats (visual only, additions only)

The entity diff is 29 inserted lines and zero removed lines. Two
visual-only spawn blocks were added and nothing else in the file
moved: the damage block (tickCount % 10), the lifetime, the ember
and rain layers, and the SlamDebris tick 1 burst are byte-identical
to HEAD.

- Tick 2 (the King's Slam pattern: SlamDebris on tick 1, the field
  on tick 2): DebrisSpawner.spawnField, 10 chunks at density 1.0,
  radius 2.0, rise height 1.4, orbit 14, ground-sampled palette:
  the rise debris at the wall bases.
- Tick 12 (the renderer's slam tick): DebrisSpawner.spawnField,
  14 chunks (inside the spec's 12 to 16), radius 2.2, rise height
  1.8, orbit 16.
Both counts scale by DbaEffectsConfig.effectDensity like every
other debris call site, and both pass the fissure's owner (or null)
exactly as KingsSlamEntity does. DebrisEntity pieces deal no
damage and destroy nothing; PermanentDestruction code, tiers,
caps, and protected-block rules were not touched anywhere in this
slot (no destruction file was opened for editing).

## OxShockwaveRenderer: inner plate ring

Before: 1 fracturePlates call (outer fan) plus 1 crackFan.
After: 2 fracturePlates calls. The added inner ring runs at 0.45
of the plate radius and 0.75 of the plate lift with a sibling seed
and the same palette, through the same submitSolid path as the
existing plates (a grounded plate family that predates the lit
path and was left on it by the Phase 1 glow fix; the new ring
matches its own family rather than splitting the pair across two
lighting paths). About 480 added vertices at distance scale 1.0,
inside the class L budget. Wave timing, radius growth, and the
emission envelope are unchanged.

## KingsSlamRenderer: inner crack fan and rim spikes

Before: 1 fracturePlates call plus 1 crackFan call; no spikes.
After: crackFan calls 1 to 2, plus 6 rock spikes.

- Inner crack fan at 0.55 of the disc radius, sibling seed, same
  hot palette, submitEnergy. Interpretation note: the spec says
  crackFan branches plus 3, and crackFan has no branch or crack
  count parameter (its count is foundation-internal), so the extra
  branches are delivered as this layered inner fan, the same
  technique the fissure renderer already uses to lengthen its web.
  Change request 1 below covers the exact-count version.
- Rim spikes: one submitSolidLit whose draw loops 6 spikeEruption
  calls, each a SCATTER of count 1 centered on a seeded rim point
  at 0.93 of the live disc radius (angle jitter plus or minus 0.35
  radians), so the six short spikes stand scattered AT the rim
  instead of filling the disc. sizeScale 0.55 (lengths about 0.66
  to 1.76 blocks: short, as specced). 6 times 384 = 2304 vertices;
  the submit's total (plates about 480, two fans about 1800,
  spikes 2304) stays under the class L 6000 figure. Palette is the
  fissure wall's low-chroma stone pair (root 0x2B241E, tip
  0x77644C), lit, no glow. Rise is keyed to the slam's own growth:
  riseFactor runs life 0.38 to 0.58, so the spikes erupt just as
  the expanding front reaches the rim and stand at full height
  just after it passes; sinkFactor stays 0 and they crack away
  through the slam's fade envelope, the addendum's sanctioned
  spike exit. Radii, growth curve, lifetime, and rim damage are
  unchanged.

## OxChargeRenderer: strands plus 2, ember layer doubled

Before: tendrilFan strands 6 (3 clusters of 2); 6 emitters, the
ember presence being one soft EMBER emitter (rate 55, cap 90,
gated above 25 percent charge).
After: tendrilFan strands 8; 7 emitters.

- Strands: delivered as a fourth 2-strand cluster (loop 3 to 4,
  ring divisor 3.0 to 4.0), so every cluster keeps its uniform
  two-strand fan and the total is exactly plus 2. To hold the same
  vertical band with four clusters, the cluster height step
  compresses from 0.26 to 0.20 (origins now at 0.20, 0.40, 0.60,
  0.80 instead of 0.20, 0.46, 0.72). Charge radius growth, fade in,
  and the charge envelope are unchanged.
- Ember: the audit says EMBER_FRAGMENT layer doubled, but this
  renderer had no EMBER_FRAGMENT layer at HEAD, only the soft
  EMBER material emitter. Rather than doubling the soft sprite
  count (against the pass's complex-particles direction), a chunky
  EMBER_FRAGMENT emitter was added at full parity with the ember
  layer (same disc source, same velocity and spread, rate 55, cap
  90, same 25 percent charge gate), so total ember-class emission
  doubles and half of it is now solid fragments. Flagged as a spec
  discrepancy for the merge: if the intent was literally 2x on a
  fragment layer that exists in some other build, the parity add
  still lands within one emitter of it.

## Verification

Targeted javac (JDK 25 at ~/workspace/tools/jdk-25.0.5+8,
sourcepath src/main/java:src/client/java) over the five owned
files, FULL logs read on both classpaths:

- cp262 (~/workspace/dba-audit-libs/cp262.txt): 4 errors, all the
  documented baseline set (DbaItems x2, DbaMenus x2). Zero errors
  in owned files.
- cp263 (~/workspace/dba-audit-libs/cp263.txt): 10 errors, exactly
  the documented baseline set (DbaItems x2, DbaMenus x2,
  DbaHostileEntity x2, PatrolRobotEntity x2, FriezaSoldierEntity
  x1, NamekianWarriorEntity x1). Zero errors in owned files.

git diff --check clean on the five owned files. Byte-level scan of
all 183 added lines: em dash (E2 80 94) count 0, en dash (E2 80
93) count 0. HEAD remains ccfebf2; no commits. No Gradle build was
run or is claimed; no build exists in this VM, and the in-game
look is unverified until Carter's PC test.

## Change requests (foundation is read-only for owners)

1. crackFan branch control: crackFan's crack count and fork
   chance are foundation-internal, so every "branches plus N" spec
   in this pass can only be delivered as an added fan. A crack
   count or branch-count parameter on crackFan (or a small-fan
   variant) would let owners hit exact branch counts.
2. spikeEruption girth: girth is builder-internal at 0.15 to 0.23
   of length, which reads slender for erupted rock. A chunkier
   base class (or a girthScale parameter) would serve the
   believability bar for stone spikes; noted for the merge's
   planned foundation irregularity upgrade rather than worked
   around locally.

## Honest worse check

- The wall is large, deliberately: two masses spanning about 11
  blocks of ridge on a fissure whose damage trench is 4.2 long,
  and a max-charge Groundbreaker spawns six fissures, so a full
  slam now raises six clustered ridges around the wave at once.
  That is the flagship read the audit and the steering asked for,
  and the masses sit on the fissure's own yaw so they radiate with
  the pattern, but it is the first thing to judge in game: if the
  ridges crowd the shockwave's own read, the shoulder mass is the
  cheapest dial to shrink or drop, and no other part depends on it.
- The wall lives only for the opening 15 ticks of a 100 tick
  fissure (rise 7 to 8, hold to 12, slam 3), then the trench burns
  on without it. That is the specced beat (the wall is the
  fissure's opening body and its slam leaves the crack web), but
  if the ridge should persist, that is a spec change at the
  merge, not something improvised here.
- The Kings Slam rim spikes stand at full height only briefly
  (rise completes at life 0.58, the fade starts at 0.62) before
  cracking away with the envelope. They read as a crest flourish
  thrown up by the passing front, which matches the addendum's
  crack-away recipe, but they are not standing rocks for the
  slam's remaining life; a retract-through-sinkFactor exit would
  need the fade window moved, a host timing judgment left to the
  merge.
- Ox Charge clusters 2 and 3 sit slightly lower than before
  (0.26 to 0.20 height step) to make room for the fourth cluster
  in the same band. A minor composition shift in an existing
  layer, accepted to keep every cluster's fan uniform; visible
  only on a side-by-side comparison.
- Nothing was reverted: no part of this slot reads worse on the
  code evidence available. The two judgment calls most likely to
  be revisited after the PC test are the wall's total span across
  six simultaneous fissures and the rim spikes' short stand.
