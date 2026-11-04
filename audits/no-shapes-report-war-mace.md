# No-Shapes Report: War Mace

Phase 2 no-shapes conversion, built to audits/NO-SHAPES-STANDARD.md from
the audit rows in audits/no-shapes-audit.md and the identity in
audits/bespoke-report-war-mace.md. This agent is the replacement for the
first War Mace owner, which died to an infrastructure rate limit almost
immediately; git status at takeover showed no partial War Mace changes,
so the conversion was done fresh from the bespoke state.

Theme unchanged: brutal weight, bronze, stone dust. Every damage number,
falloff, lock, cooldown, stamina cost, knockback value, and the relaunch
loop are identical; the entities and the item were not touched at all.
Only the two renderers changed, and both now draw nothing.

Files changed (mine only):
- client/render/WarMaceSmashRenderer.java (rebuilt on MaterialEmitters)
- client/render/WarMaceRiseRenderer.java (rebuilt on MaterialEmitters)

## Before and after counts

| Measure (my two renderer files) | Before | After |
|---|---|---|
| BespokeGeometry builder calls | 11 (Smash 9, Rise 2) | 0 |
| LayeredRender geometry calls | 0 (distScale utility only) | 0 (no LayeredRender reference at all) |
| Direct addVertex / KiRenderHelper.vertex calls | 0 | 0 |
| KiRenderHelper references of any kind | present (render type) | 0 |
| submitCustomGeometry calls | 3 | 0 |
| Local draw helpers (drawArcLayer) | 1 | 0 |

The audit's per-file counts match the committed code: Smash 9 builder
call sites (glowFan, brokenVortexBand x3, crackStar, plume, and
displacedDome x3 counting the heavy and normal branches separately),
Rise 2 builder call sites (bladeArc inside drawArcLayer, called four
times per frame, plus the base glowFan). Both are zero now, verified by
grep.

## What each piece became

Both renderers follow the pass pattern set by the converted slam and
rift renderers: a per-entity emission bundle keyed by entity id, sources
that read a mutable frame updated every submit, the toolkit Emitter
accumulator ticked with a game-time delta (age difference over 20), the
shared FxEnvelope multiplied by MaterialEmitters.distanceScale, bundle
rebuild when the level, seed, or variant changes or age runs backwards,
and pruning of bundles unseen for five seconds. The synced SEED, the
envelope fractions, and every color carry over.

WarMaceSmashRenderer (life 14 heavy / 10 normal, envelope build 0.15,
decay 0.35, unchanged):
- glowFan ground splash -> bronze DUST across the impact disc plus a
  sparse SPLASH-bronze PLASMA glint layer on the same disc.
- brokenVortexBand x3 -> three rim swirl layers of DUST and VAPOR whose
  samples carry tangential velocities: the swirl is particle motion in
  the old bands' radii (0.82 / 0.88 / 0.95 of the shell radius), height
  bands, counter rotation directions, and falling density order (the
  per-layer envelope multipliers 1.0 / 0.6 / 0.35 track the old band
  alphas 0.85 / 0.45 / 0.18 against the shell).
- displacedDome x2 (the shell) -> DUST and shell-bronze VAPOR sampled
  through the squashed volume the shell occupied, height following the
  same bow profile (heavy 1.35, normal 0.72, envelope scaled), plus hot
  core PLASMA (0xFFE69E) sampled through the inner 0.62 volume. No dome
  surface remains; the standard's fields method, carried by motes.
- crackStar -> dark fracture-bronze DUST and sparse bronze PLASMA along
  seven seeded fracture arms on the ground (angles and lengths from the
  same seed salt 0xCA01). The entity's real GroundScar fracture disk is
  untouched and still does the physical fracturing.
- plume -> pale DUST and EMBER column over the center, ticked with the
  same plume envelope (fades out over the first 60 percent of life).
- Added physical impact material the geometry build did not have:
  BLOCK_CHUNKS debris of the actual ground block under the smash and
  SPARK steel sparks across the disc, both at modest rates. The entity
  already throws a real DebrisSpawner field server side; these are the
  client-local near-camera complement the converted KingsSlam used.

WarMaceRiseRenderer (life 8, envelope build 0.12, decay 0.30, unchanged):
- bladeArc x4 layers -> PLASMA emitted along the arc path as data. The
  source samples the same 250 degree sweep in the same facing plane
  (right, up, forward from yRot, 0.35 forward offset) at the same radii
  (x1.3 wisp, x1.0 main, x0.96 and x0.92 ghosts) and heights (lift
  +0.06 / 0 / -0.28 / -0.56), climbing at the same 0.22 per tick and
  opening with the same smoothstep ease over 3 ticks. Colors are the
  exact old layer colors as ARGB. Layer strengths map to emission
  density multipliers (main 1.0, mid ghost 0.55, wisp 0.45, low ghost
  0.35) in place of the old per-layer envelope scaling.
- The crescent also sheds a pale VAPOR wash and sparse SPARK steel
  sparks along the main arc, the slash method's contact material.
- glowFan launch ring -> DUST and VAPOR on an expanding ground ring
  (radius 0.8 + age x 0.55, the old fan's growth) with outward sample
  velocities, gated to the same first 3.5 ticks with the same
  (1 - age / 3.5) x 0.6 strength curve.

## Materials per effect

- WarMaceSmash: stone dust (DUST, theme bronze), shock vapor (VAPOR,
  pale and shell bronze), physical debris (BLOCK_CHUNKS of the real
  ground block), steel sparks (SPARK), impact embers (EMBER), hot
  plasma glints (PLASMA, splash and core bronze).
- WarMaceRise: bronze plasma (PLASMA, four layer colors), shock vapor
  (VAPOR), steel sparks (SPARK), ground dust (DUST).

This matches the audit's named material for both rows: compressed ground
dust, vapor, physical debris, and impact embers or sparks; theme stone
dust, physical debris, steel sparks, shock vapor.

## Verification (javac audit, not a Gradle build)

Targeted javac compiles of my two files with sourcepath over the whole
tree, baseline (files as committed at ad22b32) versus converted, on both
audit classpaths with absolute @argfile paths:
- 26.2: baseline 4 error lines, converted 4 error lines; the sorted
  error sets are byte-identical (DbaItems CreativeModeTab x2, DbaMenus
  MenuType x2, the standing baseline artifacts). Zero error lines name
  any War Mace file. All 8 class files for my two renderers and their
  nested types were emitted.
- 26.3: baseline 10 error lines, converted 10 error lines; the sorted
  error sets are byte-identical (the same 4 plus the known 26.3 mob
  entity kinds in DbaHostileEntity, FriezaSoldierEntity,
  NamekianWarriorEntity, PatrolRobotEntity). Zero error lines name any
  War Mace file. All 8 class files emitted.
- Grep proof: BespokeGeometry 0, LayeredRender 0, addVertex 0,
  KiRenderHelper 0, submitCustomGeometry 0 in both files, and no em
  dash bytes in either file or this report.
- No Gradle build was run or claimed (the sandbox cannot; Carter builds
  on his PC, standing waiver).
- Other Phase 2 owners were editing their own files in the same tree
  throughout; no file outside my two was touched by me. Nothing was
  committed.

## Shared change requests

None. MaterialEmitters (Preset, Source, burst, Emitter), the DUST,
VAPOR, and PLASMA materials, and the fixed SPARK, EMBER, and BLOCK_CHUNKS
types covered every layer. One small note for the coordinating pass, not
a request: Emitter rates are fixed at construction, so effects whose
strength layers change over life (the plume, the launch ring) are driven
by passing different envelope values per emitter, which works cleanly;
no toolkit change is needed.

## Worse check

Nothing reverted. Four honest notes:

- The shell is the big read change, again. The bespoke dome read as a
  filled bronze bell; the conversion reads as a dense bronze dust and
  vapor volume with a hot core. That is what the standard orders for a
  field, and the bow profile, footprint, and envelope are the same, but
  the hard silhouette is gone by design, so the smash's edge is softer
  and its peak density moment is less sculpted than the dome was.
- Particle density now carries what alpha carried. At the far distance
  step (distScale 0.35) plus the per-second budgets, a heavy smash at
  long range will read thinner than the mesh did, because the mesh drew
  at full coverage for free. Rates were set generously inside the
  budgets (rim layer 200/s, shell dust 165/s at normal size, scaled up
  1.25x for heavy), but this is the known cost of the material method.
- The rise crescent's arc is sampled, not drawn, so individual frames
  show a beaded plasma arc rather than a continuous blade of light. At
  the main layer's 300/s rate and the plasma profile's size, the beads
  overlap into a band in motion; a still screenshot will show the
  sampling. The ghosts are deliberately sparser and will read as
  trailing motes, which suits a ghost layer.
- The rim swirl layers give the dust real tangential motion the bands
  only faked by rotating a texture-like mesh; if anything reads better
  than the geometry it replaced, it is this and the crack arms, whose
  plasma now sits in the real scarred fractures instead of on a drawn
  star.
