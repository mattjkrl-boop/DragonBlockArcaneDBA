# No-Shapes Realism Pass Report: Evil Spear

Owner scope completed at HEAD ad22b32, with no commit made. Only these
three renderer files were edited:

- `src/client/java/com/dragonblockarcanedba/client/render/EvilSpearChargeRenderer.java`
- `src/client/java/com/dragonblockarcanedba/client/render/EvilSpearProjectileRenderer.java`
- `src/client/java/com/dragonblockarcanedba/client/render/HellHuntImpactRenderer.java`

No Evil Spear item or entity file was edited. No dread system file was
edited: `effect/EvilDreadEffect.java`, `entity/DreadGazeEntity.java`,
`client/render/DreadGazeRenderer.java`, and the MeltingOverlayMixin stay
with the Evil Spear Dread owner.

## Before and after geometry counts

Counts use the audit definitions: B is BespokeGeometry builder calls, L is
LayeredRender geometry calls (`drawVortexBands` and `drawAnchorGlow`), and
V is direct vertex writing in the renderer.

| Effect | B before | B after | L before | L after | V before | V after |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| EvilSpearCharge | 12 | 0 | 1 | 0 | 0 | 0 |
| EvilSpearProjectile | 4 | 0 | 0 | 0 | 0 | 0 |
| HellHuntImpact | 10 | 0 | 2 | 0 | 0 | 0 |
| **Owner total** | **26** | **0** | **3** | **0** | **0** | **0** |

Final grep checks on the three edited renderers also found:

- `BespokeGeometry`: 0 references
- `LayeredRender`: 0 references
- `KiRenderHelper`: 0 references
- `addVertex`: 0 calls
- `submitCustomGeometry`: 0 calls

The renderers now submit no custom geometry at all. Their remaining work
is lifecycle calculation and shared material emission.

## Conversion method

Each renderer keeps a small per-entity emission rig keyed by the existing
UUID most-significant-bits seed already carried in its render state. A rig
only stores current source data and shared `MaterialEmitters.Emitter`
accumulators. It is not a private particle system: particle types, seed
progression, density scaling, budgets, and spawning all stay inside
`MaterialEmitters` and `DbaMaterialParticle`.

The rigs call the deterministic Emitter form with a render-state age
delta, converted from ticks to seconds and capped at 0.05 seconds, and
the effect envelope as the second argument. Emission therefore follows
game time rather than wall-clock render time.
`MaterialEmitters.distanceScale` supplies the distance factor. Stale rigs
are pruned after five seconds without a render submission. Sources sample
points, paths, ground areas, and volumes as data; no source draws a mesh,
band, shell, or silhouette.

### EvilSpearCharge

Materials, in the audit theme of darkness smoke, hellfire embers, and soul
vapor:

- DARKNESS at color 0x26030B, scale 2.7, sampled through the charge column
- EMBER, sampled across the ground area
- FLAME, sampled at the spear-height core
- PLASMA at color 0xF20D33, scale 1.30, sampled at the spear-height core
- SOUL at color 0xFFD9E6, scale 1.15, sampled through the charge column

Configured stream rates before lifecycle, distance, and density scaling
are 64, 52, 36, 66, and 30 particles per second, with per-second budgets
of 84, 76, 56, 88, and 48. The old six-tick build-in, charge brightening,
release expansion, and six-tick release fade still control the envelope
and sampled radius. The entity's existing server side KI_MOTE, EMBER, and
VOID_SMOKE emission is unchanged.

### EvilSpearProjectile

Materials, in the audit theme of darkness smoke, hellfire embers, and soul
vapor:

- PLASMA at color 0xF20D33, scale 1.35, sampled in the moving cluster
- DARKNESS at color 0x26030B, scale 2.05, sampled in the wake
- EMBER, sampled along the segment from the previous rendered position to
  the current position
- FLAME, sampled in the moving cluster
- SOUL at color 0xFFD9E6, scale 1.05, sampled in the moving cluster

Configured stream rates before lifecycle, distance, and density scaling
are 86, 48, 44, 28, and 26 particles per second, with per-second budgets
of 112, 68, 64, 44, and 42. The old three-tick build-in and the fade after
tick 32 still control the envelope. The render state gained only a client
side velocity snapshot, taken from the entity's existing movement, so the
cluster and trail sources can follow the real flight path. No synced data
accessor was added or changed.

The entity's own server side layered flight trail, pierce behavior, first
hit vortex, wall handling, impact entity spawning, and sounds are
unchanged.

### HellHuntImpact

Materials, in the audit theme of darkness smoke, hellfire embers, and soul
vapor:

- DARKNESS at color 0x20030A, scale 3.0, sampled across the growing ground
  area
- EMBER, sampled across the growing ground area
- FLAME, sampled in the short execution column
- PLASMA at color 0xF20D33, scale 1.45, sampled across the growing ground
  area
- SOUL at color 0xFFD9E6, scale 1.20, sampled in the short execution column

Configured continuous stream rates before lifecycle, distance, and
density scaling are 72, 68, 58, 62, and 36 particles per second, with
per-second budgets of 96, 92, 78, 84, and 54. A one-time opening burst
fires when an impact rig is first created: FLAME 14 normal or 22 final,
EMBER 20 normal or 30 final, DARKNESS 12 normal or 18 final, and PLASMA
10 normal or 16 final, before the burst helper's own density scaling.

The old growth curve over the first ten ticks still expands the sampled
ground area. The old rise and sink timing now modulates the ember and
ground emission pulse instead of a spike silhouette. The old normal and
final pillar windows, 12 and 18 ticks, still control how long the flame
and soul column streams stay at full strength. The entity's real obsidian
scar, debris field, layered vortex particles, ember rain, lifetime, and
restore behavior are unchanged.

## Mechanics preservation

Because only the three renderer files changed, every Evil Spear mechanic
is preserved by construction:

- Damage values and formulas are unchanged.
- The hellfire damage conversions from the bespoke pass are unchanged.
- Cooldowns and Ki costs are unchanged.
- Charge time, throw speed, hunt chain size, hunt spacing, lunge timing,
  and impact lifetimes are unchanged.
- All three mark application sites are unchanged, including the melee
  mark, the lunge landing mark, and the projectile pierce mark.
- The dread applications paired with those marks are unchanged.
- The Hell Hunt lock-on still requires line of sight, still uses the
  32 block range, and still uses the same look dot threshold.
- No synced data field was added, removed, or reinterpreted.

## Verification

Targeted javac audit only, using JDK 25 and sourcepath
`src/main/java:src/client/java`. The three edited renderers were compiled
as roots on both audit classpaths.

- 26.2 (`cp262.txt`): zero error lines naming any owned renderer. Class
  files were emitted for all three renderers and their rig classes. The
  run's only errors were the four known baseline lines in
  `item/DbaItems.java` and `inventory/DbaMenus.java`.
- 26.3 (`cp263.txt`): zero error lines naming any owned renderer. Class
  files were emitted for all three renderers and their rig classes. The
  run's only errors were ten known baseline lines in `item/DbaItems.java`,
  `inventory/DbaMenus.java`, `entity/DbaHostileEntity.java`,
  `entity/FriezaSoldierEntity.java`, `entity/NamekianWarriorEntity.java`,
  and `entity/PatrolRobotEntity.java`.

Both javac runs exit nonzero because of those pre-existing closure
errors, not because of the Evil Spear files. `git diff --check` on the
three renderers is clean, and the added-line em dash scan is clean.

No Gradle build was run, and no Gradle pass or in-game look is claimed.

## Honest worse check

Nothing was reverted, because every old visible construct in these files
was geometry that this pass is required to remove. Three identity losses
need Carter's in-game judgment:

1. The projectile loses its solid spear silhouette entirely: shaft, head,
   barbs, Mach sheath, and wake are now a dense spectral material cluster
   and trail. That follows the audit's energy projectile disposition and
   is reinforced by the entity's existing server side trail, but it is
   the largest read change in this weapon. If Carter wants a visible
   solid spear in flight, the honest fix is a detailed physical projectile
   model owned as a model and registration change, not a return to drawn
   geometry in this renderer.
2. The charge loses its readable seal, orbiting shards, and nested core.
   Charge ratio still drives sampled radius and stream density, and the
   entity's own server particles still gather at the caster, but the new
   material cloud is less structured than the old emblem.
3. The impact loses its fissure, spike, and pillar silhouettes. The real
   ground scar, physical debris, and server side vortex and rain particles
   still carry the impact, and the flame and soul column keeps a vertical
   read for the old pillar window, but it will not tower as a solid
   execution pillar the way the old plume did.

## Shared change requests

- Add dynamic source factories to `MaterialEmitters` for a moving center,
  a moving path segment, and a resizing ground area. The toolkit's static
  `point`, `path`, `volume`, and `groundArea` factories snapshot their
  positions at construction, so moving or growing effects currently need
  local `MaterialEmitters.Source` lambdas that read current render state
  each sample. The Evil Spear conversions use only the shared Source
  interface and shared Emitter spawning, but a central dynamic source
  factory would make the pattern cleaner for every Phase 2 owner.
- No other toolkit gap blocked this weapon.
