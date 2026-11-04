# Bespoke Pass Phase 2 Report: Grand Sword

Scope: GrandSwordItem, GrandBladeShardEntity, GrandClashSparkEntity,
GrandCrescentWaveEntity, ValorFieldEntity, and the four renderers
(GrandBladeShardRenderer, GrandClashSparkRenderer,
GrandCrescentWaveRenderer, ValorFieldRenderer). Converted at working tree
state on top of commit 2ef870b. Not committed, per instructions.
ValorEffect and all other effect/ files were not touched (effects agent).

## Before and after counts (grep over the 9 owned files)

| Category | Before | After | Notes |
| --- | --- | --- | --- |
| Vanilla ParticleTypes refs | 2 | 0 | Both in GrandBladeShardEntity |
| DustParticleOptions refs | 0 | 0 | None existed in these files |
| Vanilla SoundEvents refs | 15 | 0 | Item 4, shard 4, wave 1, field 6 |
| KiRenderHelper primitive calls | 0 | 0 | None existed in these files |
| Ability damage built from vanilla damageSources() | 6 sites | 0 sites | One explained exception below |
| Math.cos / Math.sin lines in renderers | 84 | 6 | Remaining 6 are alpha pulses and glyph placement, no geometry |
| Local geometry draw helpers in renderers | 22 | 0 | 5 pose-glue methods added, they emit no vertices |

The 6 remaining trig lines: two dome and shard alpha pulses (Math.sin on
age), the wave strike curve (Math.sin, pre-existing), and three lines that
position the wave's glyph glow fans on the arc band (placement of builder
calls, not geometry generation). Every vertex in the four renderers now
comes out of a BespokeGeometry builder or a shared LayeredRender block.

The 5 pose-glue methods (drawBespokeBlade, drawWake, drawArcLayer,
drawWing, drawOrbital) only copy a PoseStack pose, apply rotations and
translations, and call builders. They contain no vertex emission and no
shape math, so they are orchestration, not private geometry builders.

## Geometry conversions (builders used)

GrandCrescentWaveRenderer (was drawVolumetricCrescent, drawWingRibbon,
drawTrailingArc, drawValorGlyphs):
- bladeArc x4: three nested layers (radii span, 0.80 span, 0.60 span;
  sweeps 150, 142, 132 degrees; gold canopy alpha 0.45, white-gold mid
  0.65, white core 0.85) plus a trailing wake arc stepped 0.55 back.
  Pose glue rotates the flat arc so the belly passes through the origin
  and the tips trail toward -Z, matching the old crescent's orientation.
- tornRibbon x2: wing wisps toward the tips at y +0.18 and -0.18.
- glowFan x7: valor glyphs placed on the band at computed arc positions,
  alpha flow formula unchanged (0.70 + 0.30 sin(age x 0.6 - g x 0.9)).
- shardFan x1: strike flash at the belly on the opening frames.
- Seed: entity UUID (both halves XORed), stored in the render state.

ValorFieldRenderer (was a 14 x 36 hand-built dome mesh plus drawQuad,
drawRotatingRing, drawRing3D, drawTiltedOrbitalRing, drawMeridianRib,
drawStasisCage, drawTrenchSegment):
- displacedDome x2: main shell (valor gold, alpha 0.30, envelope pulse)
  and a pale inner shell (radius x 0.97, alpha 0.13) standing in for the
  old three-stop gradient. The pose is Y-squashed by the exact spectacle
  factor 1.05 x (0.22 + 0.78 x rise), so the spawn rise and dome height
  are unchanged.
- LayeredRender.drawVortexBands: interior energy banding, replacing the
  eight meridian ribs.
- LayeredRender.drawAnchorGlow: kept as before.
- brokenVortexBand x5: outer ground ring (radius x 0.98, twist age x 0.10,
  matching the old 6 degrees per tick), inner ring (radius x 0.62, twist
  age x -0.20, matching -12 degrees per tick), lip band at the same
  lipY = 0.1 + domeHeight x 0.45, and the two tilted orbital rings
  (plus and minus 35 degrees, precession age x 0.31 rad, matching 18
  degrees per tick) via pose glue.
- crackStar x1: the eight ground rays.
- shardFan x2 per suspended projectile: crossed fans (one flipped 180
  degrees) as the stasis cage, seeded from the synced relative position.
- Seeds: fixed theme constants. FIELD_RADIUS is a compile time constant,
  so a constant seed keeps the silhouette stable and identical on every
  client, the same convention LayeredRender uses (seeds from dimensions).

GrandBladeShardRenderer (was drawPhysicalBlade, drawHelicalWake,
drawBeaconPlume, drawCrackLine, drawEdgeLine, drawGroundRing,
drawTriangle, drawQuad3D):
- tornRibbon x3 for the blade body: a flat gold ribbon (length 1.40,
  width 0.44), a vertical steel-gold ribbon crossed on the same spine,
  and a narrow white-gold core ribbon. Pose glue turns the ribbons along
  +Z with the hilt at -0.30, the old blade's exact footprint (length
  1.10 + tip, half width 0.22). Used in both flying and embedded states,
  with the old embedded tilt (max(35, |xRot|)) and 0.85 scale kept.
- tornRibbon x2: crossed wake ribbons trailing to -Z, length 2.05,
  replacing the 12 step helical line wake.
- crackStar x2: embedded fracture mandala (radii 1.10 and 0.75),
  replacing the 6 hand-drawn crack rays.
- glowFan x1: embedded warning ring at radius 0.85.
- plume x1: embedded hazard beacon, height 1.6 x pulse as before.
- Seed: entity UUID (both halves XORed).

GrandClashSparkRenderer (was drawClashRing, drawSpikeQuad,
drawDiamondSpark, plus a per-frame java.util.Random):
- plume x2: white-gold core flash and the additive halo copy.
- shardFan x3: flat spike star (radius 2.2 x scale), a standing star via
  pose rotation (radius 1.9 x scale), and the outer flying fan whose
  radius grows with progress ((1.3 + progress x 2.2) x scale, scale
  breathing only).
- brokenVortexBand x3: two counter-twisting rings (twists age x 0.5 and
  age x -0.8) and the additive halo ring.
- The double submission (flat plus 90 degrees) is kept, now rotated
  around X because the builders are XZ-planar.
- The java.util.Random is deleted. Seed: entity UUID most significant
  bits, as the render state already carried.

## Particle conversions

- GrandBladeShardEntity: ParticleTypes.CRIT to DbaParticles.CRIT_SPARK
  at both burst sites (embedded shatter, flying hit). Counts, spreads,
  and speeds unchanged.
- Every other particle call in the owned files already used DbaParticles
  (SPARK, KI_MOTE) and was left untouched.

## Sound conversions (all 15 sites, volumes and pitches unchanged)

GrandSwordItem:
- PLAYER_ATTACK_SWEEP to DbaSounds.SWING_HEAVY (shard launch; the
  bespoke SLASH_WHOOSH that already played alongside stays)
- SHIELD_BLOCK to DbaSounds.SHIELD_CLANK (projectile deflect)
- PLAYER_ATTACK_SWEEP to DbaSounds.SWING_HEAVY (finisher release)
- GENERIC_EXPLODE to DbaSounds.BOOM_HOLY (finisher release, holy boom
  per the DbaSounds guide)

GrandBladeShardEntity:
- PLAYER_ATTACK_SWEEP to SWING_HEAVY (embedded hazard hit, flying hit)
- ARMOR_STAND_HIT to HIT_METAL (embedded hit)
- ARROW_HIT to THUNK (block embed)

GrandCrescentWaveEntity:
- GENERIC_EXPLODE to RELEASE_BLAST (block hit)

ValorFieldEntity:
- BEACON_ACTIVATE to CHARGE_RISE (field spawn)
- BEACON_AMBIENT to CHARGE_HUM (30 tick hum)
- ENCHANTMENT_TABLE_USE to CAST_MAGIC (projectile suspended)
- GENERIC_EXPLODE to BOOM_BIG (suspended Ki blast walked into)
- ARROW_HIT to THUNK (suspended projectile walked into)
- ITEM_BREAK to ITEM_SHATTER (200 tick stasis expiry)

## Damage conversions

All through DbaDamage.valor, amounts and formulas untouched:
- GrandSwordItem spin contact damage: valor(player)
- GrandSwordItem finisher slash: valor(player)
- GrandBladeShardEntity embedded hazard hit and flying hit:
  valor(this shard, owner) via a small valorSource() helper; the anchor
  falls back to the shard itself when the owner is gone, replacing the
  old playerAttack / mobAttack / generic ternary
- GrandCrescentWaveEntity piercing damage: valor(this wave, owner),
  replacing the playerAttack / mobProjectile / generic ternary
- ValorFieldEntity field pressure ticks: valor(caster)

Explained exception (2 damageSources() lines remain): the Valor Field's
suspended projectile collision. That hit is the suspended projectile's
own damage, dealt for its original owner when something walks into it
(KiBlastEntity.getDamage(), or 16 x its original speed). The field deals
no damage there; re-typing it to valor would misattribute the kill to
the field's caster and change Ki blast behavior, which the task's
"keep every mechanic identical" rule forbids. It keeps indirectMagic /
mobProjectile / generic exactly as the spectacle pass had it, and the
reason is written in a comment at the site. The field's own ability
damage (pressure) is valor, as required.

Melee: untouched. GrandSwordItem.hurtEnemy never dealt damage (feedback
only, already DbaParticles), and plain melee still resolves through the
vanilla player attack path.

## Mechanics preserved (verified by git diff on the 5 main files)

The diff on GrandSwordItem, GrandBladeShardEntity,
GrandCrescentWaveEntity, and ValorFieldEntity contains only import
lines, sound event names, damage source expressions, and one comment.
GrandClashSparkEntity is unchanged. Specifically intact: spin timing
(140 tick max, timed bounciness at 100 ticks, contact every 3 ticks,
radius 2.6 to 3.5, pull 0.45 / 0.12, ramp 0.4 to 1.0), spin multiplier
0.8 to 2.4, finisher cooldown 160 ticks, wave damage (600 + Str x 2.5),
shard damage (80 + Str x 0.5), shard flight 60 ticks and embedded 200
ticks on separate counters, Valor Stun stacking (amp +1, cap 5, 60
ticks), wave life 80 ticks and 12 tick dissipate, field radius 9.0,
field pressure (55 + Str x 0.45) every 10 ticks, all buff tiers (caster
amp 2, allies amp 1, enemies Valor Stun plus Slowness amp 1, 40 tick
refresh), the ally rule (real team alliance only, caster.isAlliedTo),
Ki drain 90 percent per minute on spin and field, field cooldown 60
ticks, stasis rules (freeze, resume, 200 tick break, collision
detonation), and every renderer fade timing (shard build 3 ticks and
flight fade 10, embedded fade 30, clash envelope 0.20 / 0.45, wave
envelope 0.04 / 0.35 plus the 12 tick hit fade, field rise 10 ticks and
FADE_OUT_TICKS fade).

One compile fix during verification: the wave renderer lambda captured
the reassigned fade variable, which Java rejects. The lambda now
captures a final snapshot (fadeFinal) taken after the hit fade is
applied. No behavior change.

## Verification

Grep over the 9 owned files after conversion: ParticleTypes 0,
DustParticleOptions 0, SoundEvents 0, KiRenderHelper primitives 0,
damageSources() 2 lines (the explained exception above).

Targeted javac (JDK 25, sourcepath over src/main/java and
src/client/java, owned files only) on both audit classpaths:
- cp262 main: 0 errors in owned files. 4 errors total, all in non-owned
  DbaItems.java and DbaMenus.java pulled in as dependencies.
- cp263 main: 0 errors in owned files. All errors in non-owned files
  (DbaItems, DbaMenus, DbaHostileEntity, FriezaSoldierEntity,
  NamekianWarriorEntity, PatrolRobotEntity).
- cp262 client: 0 errors in owned files. 4 errors total, same two
  non-owned dependency files.
- cp263 client: 0 errors in owned files. 11 errors total, all in the
  same non-owned dependency files.
No error references a Grand Sword symbol. Whole-tree sweep totals were
not used because the other Phase 2 agents are editing the tree
concurrently, per the task instructions.

## Toolkit gaps (shared change requests)

1. No containment cage builder. Stasis cages are approximated with
   crossed shardFans. Request: a seeded cage builder in BespokeGeometry
   (octahedron or twin pyramid wireframe) for stasis and prison effects.
2. displacedDome takes a single color. The spectacle dome's three-stop
   vertical gradient cannot be expressed; approximated with a pale
   inner shell. Request: an optional zenith color on displacedDome.
3. No glyph or runestone builder. The wave's valor glyphs are glowFan
   discs placed on the arc. Request: a small seeded glyph builder
   (diamond or rune cluster) for inscribed effects.
4. No solid blade builder. Sword-type shards are crossed tornRibbons.
   Request: a seeded faceted blade builder (length, width, spine
   height) so blade projectiles keep a hard physical edge read.
5. Rings are Y-axis only. Tilted orbitals and standing stars need pose
   glue copies (works, used here). A band variant that takes a tilt
   would remove that glue in several renderers.

## Honest worse check

- Valor dome gradient: the smooth three-stop gradient (deep gold base,
  valor gold mid, near-white zenith) is gone. Two displacedDome shells
  approximate it, but the blend is visibly simpler. This is a real
  downgrade caused by toolkit gap 2, not by choice.
- Blade shard body: the old shard was a faceted steel-and-gold sword
  fragment with hard faces. Crossed torn ribbons read as an energy
  blade: same footprint, same spin, softer material read. If Carter
  wants the physical sword back, it needs change request 4.
- Crescent wave thickness: bladeArc's band (inner radius 0.5R) is
  chunkier than the old thin beveled crescent. The three-layer alpha
  stack keeps the bright core, but the silhouette is heavier.
- Valor glyphs: glow discs instead of diamond runes. Smaller loss,
  they still sit on the arc and pulse in sequence.
- Clash spikes: seeded fan shards instead of 8 uniform bowed spikes.
  The burst reads more organic and less mechanical, and the old
  per-frame random shard jitter is gone by design (seed stability).
- Stasis cages: spiked fan seals instead of octahedra. The containment
  read is weaker until change request 1 lands.
- Nothing else regressed: damage, cooldowns, radii, timers, buff
  tiers, ally rules, drains, and fade timings are identical, and the
  sounds are same-site swaps per the DbaSounds guide.
