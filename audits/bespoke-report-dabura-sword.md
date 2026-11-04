# Bespoke Pass Phase 2 Report: Dabura Sword

Weapon: Dabura Sword (item/DaburaSwordItem.java)
Entities: DarknessDomainEntity, DarknessBladeEntity, DarknessWaveEntity, DarknessShatterEntity, DarknessChargeEntity
Renderers: DarknessDomainRenderer, DarknessBladeRenderer, DarknessWaveRenderer, DarknessChargeRenderer, DarknessShatterRenderer
Base commit: 2ef870b (Bespoke pass Phase 1: shared foundation)
Standard: audits/BESPOKE-STANDARD.md

Every damage number, cooldown, timing, radius formula, and mechanic from the spectacle pass is unchanged. Only the presentation layer (geometry, sounds) and the damage source types moved.

## Before and after counts

| Violation class | Before | After |
| --- | --- | --- |
| Local geometry helper methods in the five renderers | 22 | 0 |
| Direct vertex emission (addVertex) in owned renderers | all geometry | 0 |
| KiRenderHelper primitive draws | 0 | 0 |
| Vanilla SoundEvents references | 8 | 0 |
| Vanilla damage source expressions for abilities | 4 | 0 |
| Vanilla ParticleTypes references | 0 | 0 |
| Vanilla DustParticleOptions references | 0 | 0 |
| java.util.Random in renderers | 1 (Shatter) | 0 |
| Vanilla weather calls for the domain storm | 0 | 0 |

Helper breakdown before: Domain 3 (drawRotatingRing, draw3DShardPyramid, drawQuad), Wave 4 (draw3DVolumetricCrescent, drawLeadingFangs, drawTrailingShards, waveTri), Blade 7 (draw3DGreatsword, drawCrossguard, drawGuardHorn, drawHilt, drawPlungeCone, consumerQuad, fadeQuad), Charge 4 (drawRotatingRing, draw3DShard, drawOctahedron, drawQuad), Shatter 4 (drawTrenchSegment, drawRockSlab, drawGroundRing, drawQuad). All deleted, none copied into shared code.

Particles needed no work: all emission in the owned files already went through DbaParticles types or LayeredParticles (whose dust() returns DbaDustOptions). Verified zero vanilla references before and after.

The domain storm needed no caller change: AtmosphereServer.begin keeps its Phase 1 signature and the system is the custom AtmosphereServer/AtmosphereClient volume, never vanilla weather. One stale spectacle-era comment in the item claimed the old system saved and restored prior weather; corrected, since the Phase 1 system does not do that.

## Sound swaps (8 vanilla references to 0)

| Site | Before | After |
| --- | --- | --- |
| Slash charge hum (item) | SoundEvents.BEACON_AMBIENT | DbaSounds.CHARGE_HUM |
| Slash release swing (item) | SoundEvents.PLAYER_ATTACK_SWEEP | DbaSounds.SWING_HEAVY |
| Slash release burst (item) | SoundEvents.GENERIC_EXPLODE | DbaSounds.RELEASE_DARK |
| Domain channel cue (item) | SoundEvents.PORTAL_AMBIENT | DbaSounds.RIFT_AMBIENT |
| Full eclipse boom (item) | SoundEvents.WARDEN_SONIC_BOOM | DbaSounds.BOOM_SONIC |
| Partial eclipse boom (item) | SoundEvents.WARDEN_SONIC_BOOM | DbaSounds.BOOM_SONIC |
| Blade impact (DarknessBladeEntity) | SoundEvents.GENERIC_EXPLODE | DbaSounds.BOOM_BIG |
| Wave hit (DarknessWaveEntity) | SoundEvents.PLAYER_ATTACK_SWEEP | DbaSounds.HIT_FLESH |

Volumes and pitches unchanged at every site. The pre-existing DbaSounds.WIND_GUST call in the full eclipse was already compliant and stayed.

Dark variants note: the task named charge_dark, thunder_dark, and zap_dark as candidates. Dabura's files have no thunder or zap call sites (the domain's lightning is real BoltEntity strikes owned by shared code, and its sound lives there), and the one charge site is the looping charge hum, which the swap guide maps explicitly from BEACON_AMBIENT to CHARGE_HUM. RELEASE_DARK covers the dark release burst. No site was left vanilla to force a dark variant in.

## Damage conversions (4 expressions to 0)

| Site | Before | After |
| --- | --- | --- |
| Full eclipse burst (item) | damageSources().playerAttack(player) | DbaDamage.darkness(player) |
| Partial eclipse burst (item) | damageSources().playerAttack(player) | DbaDamage.darkness(player) |
| Wave hit (DarknessWaveEntity) | playerAttack / mobProjectile / generic branch | DbaDamage.darkness(this, living owner or null) |
| Blade splash (DarknessBladeEntity) | playerAttack / generic branch | DbaDamage.darkness(this, owner) |

The wave and blade keep attacker attribution whenever a living owner exists; when the owner is gone, the ability entity itself anchors the source, so DbaDamage never receives two nulls. Plain melee (hurtEnemy, Ki drain, Petrification Curse on hit) is untouched and still resolves as a normal player attack, per the standard.

## Geometry conversion, builders used

All five render states now carry a seed taken from the entity UUID most significant bits (Domain and Shatter already had it; Wave, Blade, and Charge gained it). No client-side random and no wall-clock seeds anywhere. Shatter slab placement uses BespokeGeometry.Rng on that seed.

DarknessWaveRenderer:
- bladeArc x4 layered (obsidian shell, violet main, crimson core, white-hot heart) plus one additive bladeArc, posed upright with XP -90 and a wide X stretch, keeping the open/fade/pulse lifecycle math.
- tornRibbon x4: two trailing lashes streaming backward, one leading tongue plus a shorter violet lash where the fang row used to be.

DarknessBladeRenderer:
- bladeArc x4 layered (two obsidian, pulsed crimson fuller, white core) plus one additive bladeArc, wide sweeps standing in for the greatsword, same XP 90 plunge pose, ZP roll spin, and 3 tick fade-in.
- tornRibbon x2 plunge wake trails replacing the smooth plunge cones.

DarknessDomainRenderer:
- displacedDome x2: outer violet shell at y 1 rotating with age, inner crimson shell at y 2 counter-rotating, replacing the hand-rolled lat/long meshes.
- brokenVortexBand x9: four ground whirlpool bands (per-band rotated poses at the old speeds and directions), the two-layer vortex column (violet outer, crimson core, counter-rotating), two rim bands, and the additive halo band.
- branch x16: the orbiting spires, same orbit radius, bob, height, and inward lean math as the old shard pyramids.
- LayeredRender.drawVortexBands and LayeredRender.drawAnchorGlow: shared compliant helpers, the vortex bands call kept from the spectacle pass, the anchor glow replacing the hand-rolled glow disc.

DarknessChargeRenderer:
- brokenVortexBand x5: three ground seal rings turning at the old -14, +22, -32 degrees per tick, plus the twin helical strands as tall high-twist bands a half turn apart.
- branch x8 to 14: converging shards on the unchanged orbit math (count, radius, bob, inward tip direction, length all preserved).
- displacedDome x6 to 8: the nested core as dome pairs (dome plus flipped twin) at the old three sizes, plus the additive halo pair.
- LayeredRender.drawAnchorGlow for the ground glow.

DarknessShatterRenderer:
- glowFan x5: the tiered shockwave as stacked discs (violet, crimson, obsidian) plus the two additive halo discs, same expansion rate and radii ratios.
- crackStar x2: wide violet fissures with a brighter narrow core star.
- branch x12: erupting slabs leaning outward, placements from BespokeGeometry.Rng on the old distributions.
- plume x1: the central eruption column, spinning, with the old growth and thinning formulas.

## Verification

Targeted javac of exactly the 11 owned files, JDK 25.0.5+8, -sourcepath over src/main/java and src/client/java, -Xmaxerrs 10000:

- 26.2 classpath (cp262.txt): zero errors in any Dabura or Darkness file; all 11 files produced class output. The only errors in the run are 4 pre-existing baseline errors in shared files outside this task's scope (item/DbaItems.java, inventory/DbaMenus.java).
- 26.3 classpath (cp263.txt): zero errors in any Dabura or Darkness file; all 11 files produced class output. The only errors are 11 pre-existing baseline errors in shared files outside scope (DbaItems, DbaMenus, DbaHostileEntity, FriezaSoldierEntity, NamekianWarriorEntity, PatrolRobotEntity, LavaSwordGround, the last being the known 26.3 baseline entry from the spectacle audit).

No new error kinds were introduced on either classpath. No Gradle build was run or claimed; per the standing rule the compile check here is javac only and the in-game check is Carter's PC build.

## Toolkit gaps (change requests, not worked around privately)

1. No octahedron or diamond builder. The Charge renderer's nested octahedron core is approximated with displacedDome pairs (a dome and its flipped twin). It reads as a lumpy rounded core, less crystalline than the old octahedra. Request: a displaced octahedron builder in BespokeGeometry.
2. No helical ribbon builder. The Charge helices are high-twist brokenVortexBand tubes; they wind and rise correctly but are broken bands with gaps rather than continuous twin ribbons. Request: a helix ribbon builder (radius, height, turns, width).
3. No flared vortex tube. The Domain vortex column used to widen from 0.6x to 1.1x of its base radius over its height; brokenVortexBand holds one radius, and plume narrows upward instead. The column is now two constant-radius counter-rotating bands. Request: radius interpolation (r0 to r1) on brokenVortexBand or a dedicated funnel builder.
4. branch() takes a single color with a baked brightness profile (dimmer ends, full mid). The old spires, slabs, and charge shards had a two-tone read (dark base, bright tip). The conversion passes the bright color, so the ends land darker through the baked profile, which approximates it, but a from/to color gradient variant of branch would restore it exactly.
5. No vertical blade builder. The falling blade is a wide-sweep bladeArc stood upright, and the wave crescent needs XP -90 plus a 5.2x non-uniform X stretch to reach its wide flat silhouette. Both work, but a dedicated vertical blade/crescent builder with width and height parameters would be cleaner and less fragile.

## Honest worse check

Nothing was reverted; every item below is the standard's mandated trade (seeded bespoke geometry over hand-rolled regular geometry), and no mechanic, number, timing, or color family changed. Stated plainly, the visual losses are:

- Wave: the silhouette is narrower than the old crescent (about 5.8 blocks wide at full size against the old 9.7), though that also sits closer to the wave's 3.5 block hitbox, which the old visual overshot almost 3x. The fang row is gone; two jagged lashes suggest it rather than replicate it. Edges are torn and nicked instead of smooth beveled.
- Blade: the greatsword's separate parts (horned crossguard, hilt, pommel) are no longer individually readable. It reads as one huge flared torn blade. Overall size, spin, fade-in, and the pulsing crimson fuller are preserved.
- Charge: the core is rounder and lumpier than the faceted octahedra were (gap 1 above), and the helices have band gaps the old continuous ribbons did not (gap 2).
- Shatter: the shockwave rings are soft glow discs instead of hard-edged annuli, and the fissures are crackStar arms (6 to 9 arms of seeded lengths) instead of exactly 8 uniform wandering trenches.
- Domain: the dome is now a true hemisphere (height equals radius) instead of the old stretched profile (radius x 1.35, capped at 26), so small domains sit a little lower and the largest sit a little higher than before. The spires are branch strips rather than solid pyramids. The displaced, gapped surface reads as a churning cloud dome rather than a smooth sphere, which fits the theme but is a real silhouette change.

Everything else (alphas, fades, rotation speeds, orbit math, particle emission, Ki costs, cooldowns, damage values) is the spectacle pass state, untouched.
