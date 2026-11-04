# Bespoke Report: Ox King's Axe

Weapon: Ox King's Axe (item class OxKingsAxeItem).
Base commit: 2ef870b (Bespoke Phase 1 foundation). Spectacle numbers and
mechanics are unchanged; only the materials changed: geometry, particles,
sounds, and damage types are now the mod's own.

## Files converted

- item/OxKingsAxeItem.java
- entity/OxShockwaveEntity.java (Groundbreaker wave and echoes)
- entity/KingsSlamEntity.java (King's Slam)
- entity/OxFissureEntity.java (ground fissures)
- entity/OxChargeEntity.java and entity/OxStanceAuraEntity.java: audited,
  no vanilla particle, sound, or damage references existed; unchanged.
- client/render/OxShockwaveRenderer.java
- client/render/KingsSlamRenderer.java
- client/render/OxChargeRenderer.java
- client/render/OxFissureRenderer.java
- client/render/OxStanceAuraRenderer.java
- fx/SlamDebris.java: read only, untouched.

## Before and after counts

| Measure | Before | After |
| --- | --- | --- |
| Local geometry helper methods in the 5 renderers | 25 | 0 |
| Hand written addVertex calls in the 5 renderers | 120 | 0 |
| KiRenderHelper primitive calls | 0 | 0 |
| ParticleTypes references (item + entities) | 8 | 0 |
| SoundEvents references (all 11 files) | 18 | 0 |
| damageSources() constructions (item + entities) | 9 lines | 0 |
| BespokeGeometry builder calls in the 5 renderers | 0 | 50 |
| DbaDamage.crush damage sources | 0 | 6 |

The 25 deleted helpers: drawDustBand, drawFacetedCrag (shockwave);
drawTrenchSegment, drawGroundRing, drawShockwaveDome, drawRockMonolith,
drawVerticalGeyser (slam); drawLash, lashSpan, drawTrenchSegment,
drawRotatingRing, drawRockChunk, drawAscendingTendril (charge; the last
was dead code); drawTrenchSegment, drawRockTooth, toothSpan (fissure);
drawRotatingRing, drawLash, lashSpan, drawSegment (also dead),
drawShoulderPauldron, drawTitanHorn, drawAegisPlate, drawHeatDome,
drawRibbonSegment (stance).

KiRenderHelper is still referenced only for the shared render types
(kiRenderType, kiAdditiveType), the same way BespokeGeometry itself uses
it for vertex output. No primitive draw calls existed in this family
before or after; the borrowed geometry was the local helpers.

One stray import removed along the way: OxShockwaveEntity imported
DustParticleOptions but never constructed it (its dust already came from
LayeredParticles). OxFissureEntity imported ParticleTypes without using
it. Both imports are gone.

Two pre-existing em dashes in the class javadoc headers of
OxKingsAxeItem and OxShockwaveEntity (present at 2ef870b) were replaced
with colons so the converted files carry none; every other converted
file and this report were written without any. That edit is comment
only and postdates the verification run below, so it does not affect
the compile comparison.

## Geometry: builders used per renderer

Placement in the shockwave, slam, and charge renderers still follows the
spectacle pass's seeded Random stream (same entity seed, same consumption
order where the stream existed), so positions, counts, heights, and leans
match the previous build. The shapes drawn at those positions are now
toolkit builders, each seeded from the render state's entity UUID seed.
OxStanceAuraRenderer's render state gained a seed field (it had none) so
its builders are seeded like the others.

OxShockwaveRenderer:
- Anchor glow: LayeredRender.drawAnchorGlow (unchanged, already bespoke).
- Dust walls: brokenVortexBand x2 at 0.94x and 1.07x radius, heights and
  counter rotation as before, mid and outer band alphas as before.
- Perimeter crags (28 to 36): displacedDome per crag, posed at the ring
  position, leaned outward by the spectacle tilt, Y stretched from base
  width to full crag height, basalt colors.
- Molten veins: branch per crag, base to 0.88x tip, hot yellow.
- Leading edge teeth (26) and inner shards (14): branch spikes with the
  spectacle heights, widths, and outward leans.

KingsSlamRenderer:
- Anchor glow: LayeredRender.drawAnchorGlow (unchanged).
- Crest ring: brokenVortexBand, low band at 0.92x shock radius.
- Caldera bed: glowFan at 0.65x shock radius.
- Dome walls: displacedDome x2 (full radius and 0.52x), each posed with a
  Y scale so the dome height equals the spectacle dome height exactly
  (domeH and domeH x 0.70).
- Trench system: crackStar x3 layered on one seed (theme base, hot mid,
  white core), spreading with the growth curve.
- Monoliths (16 or 28): displacedDome per monolith, leaned outward, plus
  a branch vein per monolith.
- Flawless geysers (8): plume per geyser, same positions, heights, and
  flicker as the spectacle cylinders.

OxChargeRenderer:
- Ground shatter: crackStar x2 layered (magma base, hot core).
- Compression rings: brokenVortexBand x2, low bands at the spectacle ring
  radii, twisting at the spectacle rate.
- Brace heart column: LayeredRender.drawVortexBands (shared bespoke
  helper), height and colors as before.
- Levitating chunks (10 to 18): displacedDome per chunk, tumbling on the
  spectacle spin, with a branch heat core through each chunk.
- Tendrils (above 25 percent charge): branch x2 per tendril (edge and
  hot core), same endpoints as the spectacle lashes.

OxFissureRenderer:
- Anchor glow: LayeredRender.drawAnchorGlow (unchanged).
- Trench: crackStar x4 layered on one seed (dark underlay, magma base,
  heat core, white center) inside a pose squashed 0.55 on X within the
  existing yaw pose, so the star spans about 3.9 along the trench axis
  and 2.1 across, matching the entity's damage rectangle (4.2 x 2.0).
  Two branch cracks from the old build are subsumed: crackStar arms and
  their wander cover the same ground.
- Teeth (4): branch x2 per tooth (basalt body, hot core) along the long
  axis, alternating sides, spectacle heights.
- Additive pass: the heat core crackStar layer redrawn on
  kiAdditiveType at 0.35 alpha, same seed, so the glow follows the crack.

OxStanceAuraRenderer:
- Anchor glow and vortex bands: LayeredRender (unchanged, already
  bespoke).
- Boundary rings and repulsion ripple: brokenVortexBand x3 at the
  spectacle radii, heights, twist, and ripple timing.
- Pressure nodes (12): branch x2 per node (edge and core), low arcs on
  the boundary at the spectacle node angles.
- Titan horns: branch x2 per horn (body and core), gnarled in place of
  the old smooth lash horns.
- Aegis plates (2, plus cores): tornRibbon posed upright (rotate Y then
  Z) at the spectacle plate positions, bowed outward, with a smaller
  hotter tornRibbon core in the same pose.
- Pauldrons (2): displacedDome, squashed, at the spectacle shoulder
  positions.
- Heat shells (2): displacedDome, Y scaled to the spectacle dome
  heights.
- Helix ribbons (2): tornRibbon posed upright at the helix base angle,
  bending tangentially, with a thin hot core ribbon.
- Peak spikes (12, peak window only): branch x2 per spike, same angles,
  lengths, and heights as the spectacle lashes.

## Particles: swaps

All through DbaParticles; counts, spreads, and speeds unchanged.

| Site | Before | After |
| --- | --- | --- |
| Item, overload failure burst | ParticleTypes.EXPLOSION_EMITTER | DbaParticles.BLAST |
| Item, flawless slam burst | ParticleTypes.EXPLOSION_EMITTER | DbaParticles.BLAST |
| Shockwave, full charge opening burst | ParticleTypes.EXPLOSION_EMITTER | DbaParticles.BLAST |
| Shockwave, per target hit burst | ParticleTypes.EXPLOSION | DbaParticles.BLAST |
| Shockwave, ring block destruction | BlockParticleOption(ParticleTypes.BLOCK, state) | BlockParticleOption(DbaParticles.BLOCK_CHUNKS, state) |

Everything else in the family already used DbaParticles (EMBER crest and
fissure embers, THEME_DUST through LayeredParticles.dust). The ring block
destruction rules are untouched: charge at least 0.95, main wave only,
both heights, blast resistance at most 2.0, bedrock and barrier excluded,
GroundScar protection respected, 6 block particles per destroyed block
with the pre destruction state.

## Sounds: swaps

All through DbaSounds; call sites, volumes, and pitches unchanged.

| Site | Before | After |
| --- | --- | --- |
| Item, melee connect thud | SoundEvents.ANVIL_LAND | DbaSounds.THUNK |
| Item, charge rumble (every 20 ticks) | SoundEvents.BEACON_AMBIENT | DbaSounds.CHARGE_HUM |
| Item, left click charge milestone | SoundEvents.WARDEN_SONIC_CHARGE | DbaSounds.CHARGE_FULL |
| Item, stance peak reached | SoundEvents.WARDEN_SONIC_CHARGE | DbaSounds.CHARGE_FULL |
| Item, Groundbreaker release sweep | SoundEvents.PLAYER_ATTACK_SWEEP | DbaSounds.SWING_HEAVY |
| Item, Groundbreaker release thud | SoundEvents.ANVIL_LAND | DbaSounds.SLAM_THUD |
| Item, Groundbreaker release blast | SoundEvents.GENERIC_EXPLODE | DbaSounds.RELEASE_BLAST |
| Item, overload recoil crack | SoundEvents.SHIELD_BREAK | DbaSounds.ITEM_SHATTER |
| Item, overload failure boom | SoundEvents.GENERIC_EXPLODE | DbaSounds.BOOM_BIG |
| Item, flawless slam boom | SoundEvents.GENERIC_EXPLODE | DbaSounds.BOOM_BIG |
| Item, flawless slam sonic layer | SoundEvents.WARDEN_SONIC_BOOM | DbaSounds.BOOM_SONIC |
| Item, normal slam blast | SoundEvents.GENERIC_EXPLODE | DbaSounds.RELEASE_BLAST |
| Item, normal slam thud | SoundEvents.ANVIL_LAND | DbaSounds.SLAM_THUD |
| Shockwave, full charge opening boom | SoundEvents.GENERIC_EXPLODE | DbaSounds.BOOM_BIG |
| Fissure, magma sizzle | SoundEvents.LAVA_POP | DbaSounds.LAVA_BUBBLE |

The flawless slam's WIND_GUST was already DbaSounds and stays.

## Damage: swaps

Ability damage now goes through DbaDamage.crush. Plain melee is
untouched: the item's attack damage attribute and hurtEnemy (weight
scaled shove, Fissure Stun, embers, thud) never constructed a damage
source and still do not.

| Site | Before | After |
| --- | --- | --- |
| Shockwave wave and echo hits | playerAttack / mobAttack / generic ternary | DbaDamage.crush(this, owner) |
| King's Slam rim hits | playerAttack / mobAttack / generic ternary | DbaDamage.crush(this, owner) |
| Fissure standing damage | indirectMagic(this, owner) / hotFloor() | DbaDamage.crush(this, owner) |
| Stance aura pressure on enemies | playerAttack(player) | DbaDamage.crush(player) |
| Stance self pressure (overload ramp) | generic() | DbaDamage.crush(player) |
| Overload recoil self damage | generic() | DbaDamage.crush(player) |

Notes on the two judgment calls:
- The fissure's old fallback was hotFloor and its owner path was
  indirectMagic. The task brief assigns fissures to crush explicitly
  (the fissure is a ground crack from a physical weapon), so both paths
  are crush now; the ignite on hit is unchanged.
- The stance self pressure and the overload recoil are ability side
  effects of this physical weapon, not environmental damage, so they
  route through crush(player) as self inflicted crush damage instead of
  vanilla generic. Amounts unchanged.

crush(this, owner) keeps the ability entity as the direct source and
the owner as the attacker, matching the standard's replacement for
mobAttack and mobProjectile. When the owner is gone, DbaDamage anchors
on the ability entity, so ownerless waves, slams, and fissures still
deal their damage instead of falling back to generic.

## Verification

Targeted javac through the repo sweep harness (javac -Xmaxerrs 10000
over src/main/java and src/client/java), run on the converted tree and
on a pristine checkout of 2ef870b, error kinds compared as unique
(file, message) pairs with line numbers stripped.

- 26.2 classpath: baseline 132 kinds, converted tree 132 kinds, zero
  new kinds anywhere in the tree, zero kinds in any Ox file in either
  tree.
- 26.3 classpath: baseline 141 kinds, converted tree 140 kinds, zero
  new kinds anywhere in the tree, zero kinds in any Ox file in either
  tree.
- One real catch during verification: the first converted build used
  PoseStack.mulPose(Quaternionf), which exists on 26.2 but not 26.3
  (4 new kinds, one per renderer that rotates a pose). All ten call
  sites were switched to the cross version form the fissure renderer
  already used, mulPose(new Matrix4f().rotation(Axis...)), and the
  re-run is the clean result above. The same mulPose(Quaternionf) kind
  appeared transiently in other families' renderers during the parallel
  pass; it is gone from the final tree wide comparison as well.
- Tree totals moved between runs (149 down to 140 on 26.3) because the
  other Phase 2 conversions were editing the shared tree concurrently;
  the comparisons above are against the pristine baseline kinds list,
  which is the stable reference.

Gradle cannot run in this VM (sandbox loopback restriction) and the
compile check is waived by Carter; no Gradle pass is claimed.

## Honest worse check

What got better: every silhouette is now irregular and seeded (torn,
jittered, broken bands and noise displaced domes instead of ruled rings,
fans, and pyramids), the fissure visual finally matches its damage
rectangle in both axes, and all sounds and particles are the mod's own
bank, so the family no longer borrows vanilla material.

What is different, stated plainly:
- Crags and monoliths are elongated domes: rocky and volumetric, but
  their tips are rounded where the spectacle pyramids were sharp. The
  toolkit has no upright shard or spike builder; that is the single
  biggest silhouette compromise in this family (see change request).
- Branch based teeth, shards, veins, and spikes are jagged ribbons.
  They read as splintered, energized rock rather than smooth tapered
  spikes. From edge on they are thinner than the old solid teeth.
- The stance avatar is the loosest conversion: the pauldron was a curved
  shell band and is now a squashed dome cap; the aegis plates were bowed
  quads and are now upright torn ribbons (same position, size, bow, and
  colors); the horns were smooth lashes and are now gnarled branches;
  the helix was a true spiral of segments and is now one bent torn
  ribbon per strand, so it sweeps rather than corkscrews.
- The slam dome is a true hemisphere squashed to height, where the old
  wall narrowed to 0.4x radius at the top and faded to zero; the dome's
  rim alpha is 0.35x at the base instead of the wall's flat alpha. The
  profile is rounder. Numbers (radius growth, height, timing) are the
  spectacle ones.
- Vertex counts rise versus the old helpers (the builders draw richer
  geometry), stepped down by the same distScale the spectacle pass used;
  every builder receives it. Per crag and per monolith submissions also
  replace single batched draws, so a full charge wave now submits about
  37 nodes instead of 1. This is a real cost increase on the client,
  bounded by the short lifetimes (wave about 33 ticks, slam 40 to 55).

No mechanic, number, cooldown, damage value, radius, scar rule, or
destruction rule changed. If Carter judges any converted silhouette
worse in game, the spectacle versions of these five renderers are one
git revert of this uncommitted work away, and the particle, sound, and
damage conversions stand on their own.

## Shared change requests (toolkit gaps, not worked around locally)

1. An upright shard or spike builder (a seeded, irregular tapered spike
   with volume, not a flat fan) would serve the wave crags and teeth,
   the slam monoliths, the charge chunks, and the fissure teeth in this
   family alone, and likely every earth and rock weapon in the pass.
   Today those use elongated displacedDome (volume, rounded tips) or
   branch (sharp, flat).
2. A shell band builder (a curved arc strip, for pauldrons, collars,
   and crowns) would restore the stance pauldron's arc silhouette; the
   squashed dome is the closest current shape.
