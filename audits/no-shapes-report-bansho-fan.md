# No-Shapes Pass Phase 2 Report: Bansho Fan

Owner scope: the four Bansho Fan renderers named in audits/no-shapes-audit.md
(BanshoCycloneRenderer, BanshoGaleRenderer, BanshoShockwaveRenderer,
BanshoWindProjectileRenderer). Base: HEAD ad22b32.

## Replacement-agent note: the partial diff

This slot was finished by a replacement agent after the previous Bansho Fan
agent died to an infrastructure error mid-work. The partial diff it left in
the tree covered all four renderers and no report. The replacement work was
an audit of that diff, file by file, against HEAD, the binding standard, and
the MaterialEmitters foundation source, plus full verification.

Finding: the partial diff was complete and coherent. No half-converted file
was found, and no code fix was required in any of the four files. The
replacement agent made zero code changes; every claim below was re-derived
from the tree as found, not taken from the dead agent. Nothing was reset or
reverted, and no other owner's files were touched.

## Before and after geometry counts

Counts are call sites in the four renderer files, HEAD (ad22b32) against the
current tree, matching the audit table (28 BespokeGeometry, 2 LayeredRender
geometry calls, 0 direct vertex draws):

| File | BespokeGeometry before | after | LayeredRender geometry before | after | addVertex / KiRenderHelper.vertex before | after |
|---|---:|---:|---:|---:|---:|---:|
| BanshoCycloneRenderer | 6 | 0 | 2 | 0 | 0 | 0 |
| BanshoGaleRenderer | 3 | 0 | 0 | 0 | 0 | 0 |
| BanshoShockwaveRenderer | 10 | 0 | 0 | 0 | 0 | 0 |
| BanshoWindProjectileRenderer | 9 | 0 | 0 | 0 | 0 | 0 |
| Owner total | 28 | 0 | 2 | 0 | 0 | 0 |

Grep over the four files in the current tree: BespokeGeometry 0,
LayeredRender 0, addVertex 0, KiRenderHelper 0. The renderers draw no
geometry at all; each holds a per-render-state Emission holder of
MaterialEmitters.Emitter instances and emits material particles from the
submit method, using the toolkit Emitter accumulator (frame-rate
independent, density-scaled), MaterialEmitters.distanceScale for distance
falloff, and per-second budgets on every emitter.

## Materials per effect

Theme throughout is the fan's wind identity: emerald vapor, pale mist and
plasma, muted airborne dust. The emerald 0x26FFA6 is exactly HEAD's cyclone
band color (0.15, 1.0, 0.65); the pale tones 0xD8FFF2, 0xCFFFEF, 0x8FFFD8,
and 0x9FFFE0 carry HEAD's pale eye, core, and wake colors.

- Cyclone: emerald VAPOR sampled in a rotating annulus volume shaped by the
  current radius and height (the funnel is now moving material, not bands),
  pale MIST at the outer upper wall, muted green-gray DUST across the ground
  disc with tangential drift (the old foot suction skirt), and a bright pale
  VAPOR core column (the old eye and helical ribbons). The audit row also
  names rain: rain for this storm lives in the AtmosphereServer storm volume
  driven by the entity, which is unchanged common code; the bespoke renderer
  drew no rain either, so nothing was dropped here.
- Gale: emerald VAPOR shed across the opening fan surface, pale PLASMA
  following the nine rib paths (sampler picks one of the 9 discrete rib
  sides, exactly the old rib set), airborne DUST trailing from the tip half,
  and MIST gathered near the origin. fanPoint survives only as sampling
  path data, which the standard explicitly allows.
- Shockwave, cone mode: emerald VAPOR front sampled through the cone volume
  with forward rush velocity, a pale PLASMA core at 0.36 of the cone radius,
  launch DUST at the mouth, and MIST at the origin. Radial mode: emerald
  VAPOR sampled on the expanding front ring, a pale VAPOR center wash, ground
  DUST inside the ring, and real BLOCK_CHUNKS debris (Preset.fixed over a
  BlockParticleOption carrying the captured ground block state) when the
  burst sits within 1.6 blocks of a ground block. Debris is skipped for
  orientToImpact waves, whose plane is not the ground.
- Wind projectile: a dense emerald VAPOR cluster sampled along the flight
  axis with a helical tangent velocity (the drill read is now motion in the
  material, not a plume mesh), a pale PLASMA core on the axis, airborne DUST
  trailing behind, and a MIST wake volume. No shell mesh remains; the
  projectile is pure energy, so per the standard it has no model.

## Behavior preservation, checked against HEAD line by line

- Cyclone: the FxEnvelope envelope with the entity's BUILD_FRAC and
  DECAY_FRAC, growth smoothstep, radius scale x 4.0 x (0.45 + 0.55 x
  growth), and height scale x 6.5 x (0.35 + 0.65 x growth) are the HEAD
  formulas unchanged. Seed is the entity id hash, as in HEAD and the bespoke
  pass.
- Gale: open over 6 ticks with the cubic ease-out, follow over the next 4,
  smoothstep fade, spread (0.20 + open x 1.05) x (1 + follow x 0.06), lift
  0.10 x open x (1 - follow x 0.45), and fanPoint with len 2.85 and near
  0.14 are byte-identical math to HEAD.
- Shockwave: 14 tick life, progress clamp, exit fade starting at progress
  0.55 with the smoothstep, alpha 0.95, cone length maxRadius x (0.4 + 0.8 x
  expansion), cone radius maxRadius x (0.3 + 0.9 x expansion), radial radius
  max(0.4, ease x maxRadius), and crest height 0.55 x (1 - progress x 0.8)
  are the HEAD formulas unchanged.
- Wind projectile: age envelope in over 3 ticks, out from tick 42 to 50,
  drill radius 0.32 x (1 + 0.05 x sin(age x 1.5)), and the 0.55 first-person
  owner scale are the HEAD numbers unchanged.
- No damage number, cooldown, cost, timing, mechanic, entity constructor, or
  synced data field changed anywhere. BanshoShockwaveEntity and
  WindChargeMixin are untouched in the tree (git status clean for both), so
  the WindChargeMixin constructor contract holds by construction.

## Verification

Targeted javac (JDK 25) of the four renderer files via sourcepath, on both
audit classpaths:

- 26.2 (cp262.txt): all four renderers and their inner classes compiled and
  emitted. Zero errors in any Bansho file. The only errors in the run are
  the documented baseline kinds in DbaItems (CreativeModeTab protected
  access) and DbaMenus (MenuType private access).
- 26.3 (cp263.txt): all four renderers and their inner classes compiled and
  emitted. Zero errors in any Bansho file. The only errors are the
  documented 26.3 baseline kinds: the two above plus DbaHostileEntity,
  FriezaSoldierEntity, NamekianWarriorEntity, and PatrolRobotEntity.
- git diff --check on the four files: clean. Em dash scan of the four files
  and of all added diff lines: zero.
- Every MaterialEmitters call was checked against the foundation source:
  Preset.vapor, Preset.mist, Preset.dust, Preset.plasma, and Preset.fixed
  signatures; the Emitter constructor and tick(deltaSeconds, envelope);
  the Source functional interface and Sample record; sphereVolume and
  distanceScale. All usages match. No private particle system, geometry
  helper, or texture style was introduced.

Javac audit only. No Gradle build was run and no in-game look is claimed.

## Toolkit change requests

None. The Phase 1 foundation covered everything this weapon needed,
including the fixed-options preset for real block chunk debris.

## Honest worse check

Two real trades, both inherent to the standard rather than defects, neither
reverted because reverting would restore banned geometry:

- The cyclone's crisp rotating funnel wall and its eight discrete orbiting
  blade silhouettes are gone as silhouettes. The storm now reads as a dense
  rotating volume of vapor, mist, and dust with a bright core. The volume
  approximates the funnel read; it does not reproduce the clean wall. This
  is the largest identity shift in the weapon and the one Carter should
  judge first on his PC test.
- The gale's solid fan surface is now density along the fan paths. At low
  effects density settings the fan will read thinner than the old ribbons
  did. Motion, spread, and timing are identical; only the surface solidity
  changed.

One addition, not a loss: the radial shockwave previously had no debris at
all (geometry only); it now throws real chunks of the ground block it
bursts over, at the audit's direction (physical debris).

Emission runs from render submit, so an entity outside the frustum emits
nothing, the same practical behavior as the old mesh renderers, which also
only drew when submitted.
