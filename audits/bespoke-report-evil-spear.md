# Bespoke Pass Report: Evil Spear

Scope: `item/EvilSpearItem.java`, `entity/EvilSpearChargeEntity.java`,
`entity/EvilSpearProjectileEntity.java`, `entity/HellHuntImpactEntity.java`,
and the three renderers (charge, projectile, impact). Base: commit `2ef870b`.
No shared files were edited. No commit was made.

## Before and after counts (owned files only)

| Check | Before | After |
| --- | --- | --- |
| Vanilla `ParticleTypes` references | 0 | 0 |
| Vanilla `DustParticleOptions` references | 0 | 0 |
| Vanilla `SoundEvents` lines | 9 (2 imports, 7 call sites) | 0 |
| Ability `damageSources()` lines | 4 lines across 3 sites | 0 |
| Local geometry helper methods in renderers | 16 | 0 |
| `KiRenderHelper` use in renderers | render type plus 76 brightness constant references inside hand-built vertex code | `kiRenderType()` only, 1 per renderer |
| Client side random in renderers | 1 (`HellHuntImpactRenderer`) | 0 |
| Em dash characters in owned files | 4 (header comments) | 0 |

Particles needed no work: the spectacle pass already ran the Evil Spear
entirely on `DbaParticles` types and `LayeredParticles` theme dust, and that
is unchanged.

Three small orchestration wrappers remain in the renderers
(`drawCoreLayer`, `ribbon`, `barb`). They contain no vertex code and no
geometry math: they only apply pose transforms and submit calls into
`BespokeGeometry`. Every vertex in the weapon now comes from
`BespokeGeometry` or `LayeredRender`.

## Sounds: 7 swaps, volumes, pitches, and sources unchanged

| Site | Before | After |
| --- | --- | --- |
| Item, charge tick (every 20 ticks) | `ENCHANTMENT_TABLE_USE` | `DbaSounds.CAST_DARK` |
| Item, throw release | `TRIDENT_THROW` | `DbaSounds.TRIDENT_THROW` |
| Item, lunge fallback | `PLAYER_ATTACK_SWEEP` | `DbaSounds.SWING_LIGHT` (the paired `SLASH_WHOOSH` line was already bespoke and stays) |
| Item, lunge landing crit | `PLAYER_ATTACK_CRIT` | `DbaSounds.CRIT_HIT` |
| Item, hunt step crit | `PLAYER_ATTACK_CRIT` | `DbaSounds.CRIT_HIT` (the paired `BLADE_SHING` line was already bespoke and stays) |
| Projectile, wall hit | `TRIDENT_HIT` | `DbaSounds.TRIDENT_HIT` |
| Projectile, entity hit | `TRIDENT_HIT` | `DbaSounds.TRIDENT_HIT` |

## Damage: ability damage on DbaDamage.hellfire, melee untouched

- Throw pierce (`EvilSpearProjectileEntity`): the old source ternary
  (player attack, mob projectile, generic) is now
  `DbaDamage.hellfire(this, owner)` for a living owner and
  `DbaDamage.hellfire(this, null)` otherwise, with the spear as the direct
  entity. Damage value, the 1.3x marked multiplier, and `capDamage` are
  unchanged.
- Hunt steps (`EvilSpearItem.tickHuntStep`): `playerAttack(player)` is now
  `DbaDamage.hellfire(player)`. Step multipliers and formulas unchanged.
- Lunge landing (`EvilSpearItem`): `playerAttack(player)` is now
  `DbaDamage.hellfire(player)`. Damage formula and the mark application
  unchanged.
- Plain melee stays vanilla: `hurtEnemy` never dealt the melee hit itself;
  it drains Ki (`drainKiDiscrete(player, 45.0, 8)`) and applies
  `MARKED_BY_EVIL` for 120 ticks. Both are untouched, so the mark still
  applies exactly as before.

No mechanic or number changed anywhere: charge time, throw damage and
speed formulas, cooldowns (throw 44 to 60, hunt 160, lunge 40), the hunt
chain rules (8 targets, 16 block steps, 6 tick spacing), the lock-on
(line of sight required, 32 block range, dot above 0.5), vortex numbers,
impact lifetimes (35 normal, 50 final), debris, and the ground scar are
all byte-identical logic. The only other edits in the two entity files are
header comment punctuation.

## Geometry: builders used per renderer

Seeds: every shape is seeded from the entity UUID most significant bits,
which the server assigns and the client receives, so all clients draw the
same silhouette. The charge and projectile render states gained a `seed`
field for this; the impact state already carried one. No new synced data
accessors were needed.

Charge renderer (helpers deleted: `drawRotatingRing`, `draw3DShard`,
`crystal`, `drawOctahedron`, `drawQuad`):
- Ground seals and the tightening filament: `brokenVortexBand` at each old
  ring's mid radius, height, color, alpha, and spin speed (outer 1.09x,
  mid 0.76x, core 0.42x of base radius; spins -18, +28, -42 degrees per
  tick).
- Gathering rings: four `brokenVortexBand` calls on the unchanged ring
  phase, contraction, climb, color ramp, and spin math.
- Plasma strands: two tall `brokenVortexBand` calls (radii 0.65x and 0.62x
  of base radius, height 2.4, twist offset half a turn apart).
- Orbiting shards: two `shardFan` layers in the needle colors (dark
  (0.75, 0, 0.12), pale (1.0, 0.75, 0.85)) at the old orbit radius and
  speed, one flat and one vertical and counter rotating.
- Core: three nested layers of paired `displacedDome` calls (one up, one
  flipped) at the old octahedron sizes (coreSize, 0.58x, 0.26x), spins
  (+24, -36, +60 degrees per tick), colors, and the charge brightened
  heart.
- `LayeredRender.drawVortexBands` column kept as it was.

Projectile renderer (helpers deleted: `drawTaper`, `drawSpearHead`,
`drawFin`, `drawTriangle`, `drawQuad`, `drawRing`):
- Shaft: crossed `tornRibbon` pairs (dark sheath, bright core) laid along
  the spear axis with pose rotations, wide at the neck (z 1.35) and
  narrowing to the butt (z -2.0), matching the old taper spans and widths.
- Head: crossed wide `tornRibbon` pairs, crimson outside and pale inside,
  z 1.2 to tip 2.85, inner blade z 1.55 to 2.68, same widths and colors.
- Barbs: angled `tornRibbon` calls reproducing the old fin endpoints
  (crossguard reach 0.55 out, z -2.0 to -0.9 on all four sides; head
  flukes reach 0.35, z 1.2 to 0.7).
- Mach sheath: `plume` (radius 0.70, height 2.55 from z 0.3, alpha 0.42 on
  the body fade) with a `brokenVortexBand` rim at radius 0.92 in the old
  rim color.
- Trail: three `tornRibbon` strands clocked at 0, 90, and 180 degrees
  around the spear axis, length 4.2 behind the butt, the third wider and
  faint (alpha factor 0.32), all on the body fade.
- Wake: one `brokenVortexBand` easing back on the unchanged wake curve
  (radius 0.28 to 0.98, alpha sine 0.5 on the body fade).

Impact renderer (helpers deleted: `drawGroundRing`, `drawTrenchRibbon`,
`drawSpike`, `face`, `drawConePillar`):
- Shock seals: `brokenVortexBand` at each old ring's mid radius (0.91x,
  0.52x, 0.17x of shock radius, plus the 1.05x final halo), same heights,
  colors, alphas, and spin speeds, growing on the unchanged shock curve.
- Fissures: two layered `crackStar` calls in the old trench colors (dark
  (0.45, 0, 0.08) at 1.15x radius, bright (1.0, 0.35, 0.40) at 0.8x),
  growing with the shock.
- Spike ring: two `shardFan` layers (dark sheath, blood core) whose
  radius carries the old spike rise and sink factor unchanged.
- Execution pillar: two `plume` layers (crimson sheath, pale core) on the
  unchanged pillar timing, heights (5.5 normal, 8.5 final), radii, and
  alpha curve.
- `LayeredRender.drawAnchorGlow` and `drawVortexBands` kept with their
  exact spectacle pass arguments.

## Verification: targeted javac audit (not a Gradle build)

Compiled the seven owned files with JDK 25 (`~/workspace/tools/jdk-25.0.5+8`),
`-sourcepath src/main/java:src/client/java`, once per classpath:

- 26.2 (`cp262.txt`): zero error lines naming any owned file. The compile
  produced 4 errors, all in shared files outside this scope
  (`item/DbaItems.java`, `inventory/DbaMenus.java`), matching the known
  baseline kinds.
- 26.3 (`cp263.txt`): zero error lines naming any owned file. The compile
  produced 11 errors, all in shared or foreign files (`DbaItems`,
  `DbaMenus`, the hostile entity classes, and a sibling weapon file that
  was mid-edit by its own agent).

Gradle cannot run in this sandbox; this is a javac audit only and is not
claimed as a Gradle pass.

## Honest worse check

The conversion holds the spectacle pass identity, colors, timing, and
sizes throughout, but four silhouettes are approximations because the
toolkit has no matching builder. In rough order of how much they change
the read:

1. Impact spikes: the old spikes were tall vertical blades (up to about
   2 blocks). `shardFan` is flat, so the eruption now reads as jagged
   ground shatter radiating from the scar; the vertical read is carried
   by the plume pillar and the vortex column. This is the clearest loss.
2. Projectile body: the 12-sided taper tube and the faceted diamond head
   become crossed ribbon pairs. Along the ribbon planes the silhouette
   matches; edge-on to both planes it reads thinner than the old solid.
3. Charge needles: the individually aimed, bobbing crystal needles become
   two orbiting shard fans. The orbit radius, speed, and two-tone colors
   are kept; the per-needle aim at the core and the height bob are gone.
4. Trail and cone: the helical trail wrap becomes bent torn ribbons (the
   whole spear assembly still spins, so the trail still whirls), and the
   smooth Mach cone becomes a turbulent plume, which reads less like a
   clean shock cone and more like a vapor sheath.

None of these read as broken or off-theme, and the pass's goal is
irregular silhouettes over regular primitives, so nothing was reverted.
Items 1 and 2 are the ones to judge first in game.

## Shared change requests

- Add a vertical shard or blade builder to `BespokeGeometry` (a seeded fan
  of upright, uneven blades around a center, parameterized like
  `shardFan`). It would restore the impact spike eruption and give the
  charge needles their aim back. Until then the flat fan compositions
  above stand.
- No other toolkit gaps blocked this weapon.
