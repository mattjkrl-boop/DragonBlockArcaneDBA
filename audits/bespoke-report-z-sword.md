# Bespoke Pass Report: Z Sword

Scope: item/ZSwordItem.java; entities ZChargeEntity, ZShockwaveEntity, ZStanceAuraEntity, ZGravitySlamEntity, DelayedLaunchEntity; renderers ZShockwaveRenderer, ZGravitySlamRenderer, ZChargeRenderer, ZStanceAuraRenderer, DelayedLaunchRenderer. Base commit 2ef870b. No shared files were edited. Not committed, per instructions.

## Before and after counts

| Category | Before | After | Notes |
| --- | --- | --- | --- |
| Local regular-geometry draw helpers in renderers | 20 | 0 | Charge 6, Slam 5, Shockwave 3, Stance 4 (plus its inline funnel quad loop), DelayedLaunch 2. All deleted. |
| KiRenderHelper primitive draws (box, cylinder, disc, sphere, helix) | 0 | 0 | The spectacle renderers drew raw vertices; the violation was the local helper layer, now gone. |
| BespokeGeometry builder call sites in renderers | 0 | 45 | Wave 4, Slam 10, Charge 13, Stance 13, DelayedLaunch 5 (grep count of BespokeGeometry references, including the Rng use in the slam renderer). |
| Vanilla SoundEvents call sites | 12 | 0 | Item 7, ZShockwaveEntity 3, ZGravitySlamEntity 1, DelayedLaunchEntity 1. All volumes and pitches unchanged. |
| Vanilla ParticleTypes call sites | 1 | 0 | ZShockwaveEntity wall impact POOF became DbaParticles.DUST_PUFF, same count, spread and speed. No ParticleTypes.BLOCK existed in Z files, so no BLOCK_CHUNKS swap was needed (before count for block particles: 0). No raw DustParticleOptions existed either: charge and stance dust already routes through LayeredParticles.dust, which returns the THEME_DUST options type since Phase 1. |
| Vanilla damage sources for ability damage | 3 sites, 6 vanilla sources | 0 | Wave mobProjectile; crush indirectMagic and inWall; slam playerAttack, mobAttack and generic branch. All replaced by DbaDamage factories (see below). Plain melee in ZSwordItem.hurtEnemy never dealt damage (particles only) and is unchanged; no melee damage source existed to preserve. |
| Renderer seed source | Entity UUID most significant bits (4 renderers; DelayedLaunch unseeded) | Synced entity SEED | All 5 entities now carry a synced INT SEED in the BoltEntity house pattern, set from level random in the spawning constructor. |

## Sounds (swap guide applied, call sites and volumes and pitches identical)

- ZSwordItem charge hum: BEACON_AMBIENT to DbaSounds.CHARGE_HUM.
- ZSwordItem wave release sweep: PLAYER_ATTACK_SWEEP to DbaSounds.SWING_HEAVY (heavy blade).
- ZSwordItem wave fire: GENERIC_EXPLODE to DbaSounds.RELEASE_BLAST.
- ZSwordItem stance milestone pulse: WARDEN_SONIC_CHARGE to DbaSounds.CHARGE_FULL (the guide's milestone target).
- ZSwordItem slam release: GENERIC_EXPLODE to DbaSounds.BOOM_BIG, and ANVIL_LAND to DbaSounds.SLAM_THUD.
- ZSwordItem too-early release tick: ANVIL_LAND to DbaSounds.GROUND_CRACK (a fizzle, not a slam).
- ZShockwaveEntity travel loop (every 15 ticks): PLAYER_ATTACK_SWEEP to DbaSounds.SWING_HEAVY.
- ZShockwaveEntity root crush: ANVIL_LAND to DbaSounds.GROUND_CRACK (the head-in-blocks crunch).
- ZShockwaveEntity wall impact: ANVIL_LAND to DbaSounds.SLAM_THUD.
- ZGravitySlamEntity strike: ANVIL_LAND to DbaSounds.SLAM_THUD.
- DelayedLaunchEntity launch: GENERIC_EXPLODE to DbaSounds.RELEASE_BLAST.

## Damage

- Wave hit: DbaDamage.gravity(this, owner if living else null). The gravity type is the Z Sword's theme type per the task; the mobProjectile source had no armor bypass, and gravity mirrors a plain physical shape, so effective damage is unchanged.
- Root crush 250: DbaDamage.crush(this, livingOwner or null). Per standard rule 4, inWall maps to crush; the old branch picked indirectMagic when an owner existed and inWall otherwise, and crush covers both with the direct entity anchored.
- Slam strike: the playerAttack / mobAttack / generic three-way branch collapsed to DbaDamage.gravity(this, livingOwner or null). Numbers, falloff (full inside 45 percent radius, 30 percent at rim) and the strike tick are untouched.

## Geometry (builders used, per renderer)

All pose math, envelopes (fade, grow, impact, decay, pulse), colors and radii from the spectacle pass are preserved verbatim; only the shape source changed.

- ZShockwaveRenderer: bladeArc x3 (outer gold crescent, near-white core at 0.30 width and 0.78 height scale, dimmer wake arc trailing 0.85 behind), shardFan x1 (apex solar core, facing forward at the chord). bladeArc is flat, so the arc is stood upright with two pose rotations (arc +X becomes up, +Z becomes span), the radius is solved from the span (about 0.61 of radius sideways at a 150 degree sweep), Y is squashed so the band is exactly 2 x height tall, and the band sits at 60 percent of the chord so it stays inside the 1.4 block damage band tolerance.
- ZGravitySlamRenderer: displacedDome x1 (Y-squashed by pose scale to the old domeH), brokenVortexBand x1 (rim), crackStar x3 (inner ground mark, violet trench star, gold core star at 0.8 reach), tornRibbon x2 per monolith (violet body, brighter core seam) stood upright and leaned radially outward with the old placement, height, width and tilt math, plume x2 per geyser (gold core, violet outer) at 5 endpoints derived from BespokeGeometry.Rng on the entity seed. The LayeredRender.drawAnchorGlow pass is kept as is.
- ZChargeRenderer: glowFan x2 (mandala bed, additive halo), crackStar x1 (rotating sacred spokes, pose-rotated at the old 8 degrees per tick), brokenVortexBand x7 (mandala inner ring, 3 stacked funnel bands narrowing into the sword, 2 tilted celestial rings, 2 flat compression rings, 1 additive halo band), shardFan x2 per prism (tumbling crystal fan), branch x1 per overdrive flare (6 to 12 jagged rays from the core).
- ZStanceAuraRenderer: glowFan x4 (ground disc, singularity eye, 2 additive halo), brokenVortexBand x9 (ground ring, eye rim, 4 contracting event horizon rings on the exact old phase math, 4 stacked well bands, 2 strongly twisted gold stream bands), tornRibbon x2 per monolith (dark body, violet core) with the old placement, height, width and lean math, shardFan x1 per monolith (gold rune above the anchor).
- DelayedLaunchRenderer: crackStar x1 (full footprint marker at the true slam radius, slowly rotating), brokenVortexBand x2 (contracting violet band, tighter gold band), shardFan x4 (rune ticks at the rotating cardinal points), plus the kept LayeredRender.drawAnchorGlow footprint pass and a glowFan halo.

## Seeds

Synced INT SEED added to all 5 entities, defined in defineSynchedData, set in the spawning constructor, exposed via getSeed(), and read by the renderers through the render state. Persisted in save and load for ZShockwaveEntity, ZGravitySlamEntity and DelayedLaunchEntity (they already had save and load). ZChargeEntity and ZStanceAuraEntity have no save and load overrides in the spectacle code and none were added: their seed is synced only, so a chunk reload restarts their pattern from seed 0. Flagged here as a known cosmetic gap, not hidden.

## Verification

Targeted javac per the Phase 1 audit method: the 11 owned files compiled with JDK 25 against the audit classpaths (cp262.txt and cp263.txt in ~/workspace/dba-audit-libs), sourcepath src/main/java:src/client/java, which pulls the dependency closure.

- 26.2 classpath: 0 errors in owned files. 4 errors total, all in untouched files (DbaItems, DbaMenus), all kinds present in the recorded baseline sweeps (errors262b and errors262c).
- 26.3 classpath: 0 errors in owned files. 11 errors total, all in untouched files (DbaItems, DbaMenus, DbaHostileEntity, FriezaSoldierEntity, NamekianWarriorEntity, PatrolRobotEntity), all kinds present in the recorded baseline sweeps (errors263b and errors263c). FriezaSoldierEntity references no Z file, so these cannot be caused by this conversion.
- Baseline reference: full tree sweeps at 57902e1 recorded 132 error kinds on 26.2 and 141 on 26.3. This conversion introduces no new kind in any file it touched.
- Post-conversion greps over the 11 owned files: SoundEvents 0, ParticleTypes 0, damageSources() 0, local draw helpers 0, KiRenderHelper primitive draws 0.
- Gradle cannot run in this VM (standing sandbox loopback restriction), so no Gradle pass is claimed, same as every prior pass.
- Em dashes: none added. Five pre-existing em dashes remain in the class header comments of ZSwordItem and the four main entities; per Carter's standing rule, past text is not gone back and changed, so they were left exactly as they were at 2ef870b.

## Numbers and mechanics preserved

Untouched by this pass: melee 1000 and attack speed; wave damage 150 + 1050 x charge + Strength x 3.5 x charge, speed, cooldown 40 + 80c, hit band (span 4.8 + 6.2c, chord 1.6 + 1.4c, height 1.5 + 1.8c, tolerance 1.4), sub-waves at tick 3, pierce and root at charge 0.90 and up, crush 250 with the both-cells-solid bury check, MOVEMENT_CURSE 80 ticks amp 9; charge drains (Ki 75 percent per min, stamina 50 percent per min, auto-fire at 300 ticks); stance drain 75 percent per min, hard stop 200 ticks, VortexPhysics well radius 3 + 5p and strength 0.06 + 0.10p; slam radius 6 + 12p, damage 400 + 800p + Strength x 3.0p at strikeTick max(6, 30 percent of life), life 80 + 40p, falloff, cooldown 100 + 100p, debris 26 + 36p, GroundScar trenches, launch delay 40 + 40p, fling 1.2 + 1.8p horizontal and 1.0 + 0.8p up, timed bounce and air drag modifiers at 200 ticks, launch debris 16 + 16p. Entity logic diffs are limited to the seed field, the sound tokens, the one particle token, and the damage source factories.

## Toolkit change requests (gaps found, not worked around in shared code)

1. No 3D crystal or slab builder exists. The charge prisms (octahedrons) and both monolith sets (leaning slabs) had to become flat builders (shardFan, tornRibbon). A seeded displaced octahedron or beveled slab builder would restore their volume.
2. bladeArc is flat only. A vertical crescent needs two pose rotations plus an independent Y squash at every call site. A height parameter or a vertical variant of bladeArc would make wave-style arcs one call.
3. displacedDome has a fixed hemisphere height ratio. Matching the slam's domeH required a pose Y scale. A height parameter on displacedDome would be cleaner.
4. crackStar draws at a fixed local y (0.03) with no y parameter, unlike glowFan which takes y. Elevated ground marks need pose translates. Add a y parameter to crackStar.
5. brokenVortexBand is the only builder without an envelope parameter; callers must pre-multiply alpha. Adding envelope would make the API uniform.

## Honest worse check

Not a clean sweep; three real downgrades, all from toolkit gap 1 and 2:

- Monoliths (slam and stance) are the biggest loss. The old slabs were 3D solids with a dark tip, a bright core strip and a vertical color gradient. The torn ribbon version is a flat sheet: strong from the front and back, nearly invisible edge-on, and the dark tip gradient is gone (a brighter core ribbon replaces the core strip, but there is no tip treatment). Placement, heights, lean and counts are identical, so the field reads the same from most angles, but a player circling the slam will see the slabs thin out. This should be revisited if a slab builder lands.
- The wave crescent lost its closed volume. The old mesh had thickness, bevels and a forward parabolic bulge that matched the damage curve exactly. The new layered arcs read sharper and more torn, and the band placement keeps the visual inside the damage tolerance (worst gap 1.14 blocks at the center front at full charge, against a 1.4 tolerance), but from directly above or below the crescent is now a line, not a blade with a keel.
- Charge prisms lost the solid gem look. The tumbling shard fans read as crystal bursts and keep the orbit, float, size and tumble math, but they are flat fans, not octahedrons.

Lesser changes, judged neutral to better: the slam dome is now a closed displaced hemisphere instead of an open cone wall, so its top has a soft cap instead of dissolving to zero alpha (silhouette rounder, displacement makes it read less regular than the old perfect cone, which fits the bespoke goal); trenches are wandering forked crack stars instead of straight segmented rectangles (coverage at the rim is slightly less uniform, fracture read is better); stance streams are two strongly twisted gold bands woven through the well instead of three distinct spiral ribbons (less distinct as separate streams); stance funnel and charge funnel bands are fragmented by design, so both read more broken and less smooth than the spectacle versions, which is the intended bespoke trade.

Nothing else regressed: every alpha envelope, radius, count and timing in the renderers matches the spectacle pass formulas, and all gameplay numbers are byte-identical in effect.
