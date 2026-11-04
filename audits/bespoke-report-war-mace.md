# Bespoke Report: War Mace

Phase 2 bespoke conversion, built to audits/BESPOKE-STANDARD.md from the
before picture in audits/bespoke-audit.md and the spectacle state in
audits/spectacle-report-war-mace.md. Theme unchanged: brutal weight, the
bronze shell and crescent. Every number, falloff, lock, stamina cost, and
the relaunch loop from the spectacle pass are identical; only what the
effects are made of changed.

Files changed (mine only):
- item/WarMaceItem.java (sound swaps only)
- entity/WarMaceSmashEntity.java (particle swap, damage swap, synced seed)
- entity/WarMaceRiseEntity.java (damage swap, synced seed)
- client/render/WarMaceSmashRenderer.java (rebuilt on BespokeGeometry)
- client/render/WarMaceRiseRenderer.java (rebuilt on BespokeGeometry)

## Before and after counts

| Measure (my five files) | Before | After |
|---|---|---|
| Vanilla ParticleTypes references | 1 (POOF outer in the smash emitVortex) | 0 |
| Vanilla SoundEvents references | 4 (ANVIL_LAND, GENERIC_EXPLODE, PLAYER_ATTACK_SWEEP, ANVIL_HIT, all in the item) | 0 |
| Vanilla damageSources() uses for ability damage | 4 lines (playerAttack / magic ternary in each entity) | 0 |
| KiRenderHelper primitive draws | 0 | 0 |
| Local draw helpers in the renderers | 3 (drawShell, quad, drawCrescent) | 0 |
| Hand-rolled regular geometry builders | 2 (equal-segment shell grid, equal-segment crescent) | 0 |
| LayeredRender geometry calls (drawVortexBands, drawAnchorGlow) | 3 call sites | 0 (toolkit builders instead; LayeredRender.distScale still used) |

## Geometry: builders used

Both entities gained a synced SEED field (INT, DebrisEntity pattern, set
at spawn from owner UUID, game time, and damage, and saved and loaded
with the entity), so every builder below draws the same silhouette on
every client. Per-layer seed salts keep layers from sharing a pattern.

WarMaceSmashRenderer (drawShell and its quad helper deleted):
- glowFan: the anchor glow, jittered rim instead of the equal-segment fan.
- brokenVortexBand x3: the dust ring at the shell edge, standard band
  alphas (0.85 / 0.45 / 0.18 times the envelope), counter rotating twists
  (age x +0.45 / -0.6 / +0.3), replacing drawVortexBands.
- crackStar: new fracture marks radiating on the ground under the shell,
  dark bronze (0.16, 0.10, 0.05), matching the entity's GroundScar disk.
- plume: the impact dust, early life only (envelope cut by age over the
  first 60 percent of life), pale bronze (0.92, 0.76, 0.50).
- displacedDome x2: the bronze shell itself, drawn in a Y-squashed pose
  so the dome keeps the old bow height (heavy 1.35, normal 0.72, both
  envelope scaled); outer dome in the theme bronze (heavy 0.86, 0.56,
  0.20; normal 0.85, 0.64, 0.32), inner dome at 0.62 radius in the hot
  core color (1.0, 0.90, 0.62) where the old shell had its bright mid band.

WarMaceRiseRenderer (drawCrescent deleted):
- bladeArc x4: the crescent layers. The toolkit arc lies in the ground
  plane, so each layer's pose tips it upright (X rotation -90 degrees)
  into the facing plane, centered on the facing direction, sweep 250
  degrees. Two ghost arcs below (radius x0.92 and x0.96, envelope x0.20
  and x0.40, offsets -0.56 and -0.28), one wide wisp (radius x1.3,
  envelope x0.30, offset +0.06), and the main arc (envelope x1.0) in the
  bright bronze (0.97, 0.82, 0.50). The arc's seeded nicks are the
  crescent's new signature, per the task.
- glowFan: the expanding launch ring at the base, first 3.5 ticks,
  replacing drawAnchorGlow.

The FxEnvelope usage is unchanged (smash build 15 percent, decay 35
percent; rise build 12 percent, decay 30 percent), and segment counts
still step down through the builders' distScale parameter.

## Particles

- WarMaceSmashEntity: the emitVortex outer layer was ParticleTypes.POOF;
  it is now DbaParticles.DUST_PUFF, the standard's like-for-like map.
  Nothing else in my files named a vanilla type: the rise already used
  DbaParticles.SPARK and KI_MOTE, and LayeredParticles was already
  all-custom after Phase 1.

## Sounds

All four swaps are in WarMaceItem, same call sites, volumes, and pitches:
- ANVIL_LAND -> DbaSounds.SLAM_THUD (the guide's slam swap).
- GENERIC_EXPLODE -> DbaSounds.BOOM_BIG (the guide's explosion swap).
- PLAYER_ATTACK_SWEEP -> DbaSounds.SWING_HEAVY (the guide's sweep swap,
  heavy variant: this is the heaviest weapon in the set).
- ANVIL_HIT -> DbaSounds.HIT_METAL (the anvil family's lighter metallic
  hit in the bank; it accents the rise, where a full slam thud would be
  wrong). Judgment call, flagged here per the guide's variant rule.

## Damage

- WarMaceSmashEntity and WarMaceRiseEntity: the playerAttack / magic
  ternary is gone. Ability damage now builds through
  DbaDamage.crush(this, owner): the crush family, with the smash or rise
  entity as the direct source and the owner as the attacker. The crush
  type JSON mirrors player attack, so armor and difficulty behavior are
  what they were; only the type and death message changed.
- The plain melee hit is untouched: WarMaceItem.hurtEnemy stays the
  vanilla player attack path, the standard's stated exception.
- Damage values, the center-to-rim falloff (1.0 to 0.35), the climbing
  rise hit volume, locks (ground 10, heavy 16, rise 18), item cooldowns
  (5 and 6), stamina pays (28 / 48 / 22 ticks at 125 percent per minute),
  knockback, and relaunchFromSmash are all byte-for-byte the spectacle
  behavior.

## Verification (javac audit, not a Gradle build)

Targeted javac compiles of my five files with sourcepath over the whole
tree, baseline worktree at HEAD 2ef870b versus the current tree, on both
audit classpaths (absolute paths in the @argfiles; one earlier run with
a tilde path failed to load the classpaths at all and was discarded):
- 26.2: baseline 4 error lines in the sourcepath closure, current 4.
- 26.3: baseline 11 error lines, current 11.
- The normalized error kind sets are IDENTICAL between baseline and
  current on both versions, and zero error lines name any War Mace file
  in any of the four dumps. The closure errors live in pre-existing
  files (DbaMenus, DbaItems, and on 26.3 the mob entities and
  LavaSwordGround), the same artifacts the spectacle pass recorded.
- Grep proof for the counts table: ParticleTypes 0, SoundEvents 0,
  damageSources() 0, primitive draws 0, local draw helpers 0, and no em
  dash bytes in any of my five files.
- No Gradle build was run or claimed (the sandbox cannot; Carter builds
  on his PC, standing waiver).
- The baseline worktree was removed after the comparison. Other Phase 2
  agents were editing their own files in the same tree throughout; no
  War Mace file outside my five was touched by me.

## Shared change requests

None. Every builder, particle, sound, and damage factory I needed
already existed with the signature I called.

## Worse check

Nothing reverted. Four honest notes:

- The shell silhouette changed the most: the old shell was an open ring
  band from 0.62 to 1.0 of the radius; the displaced dome covers the
  full footprint, squashed to the same bow height, so the smash reads
  as a filled, lumpy dome rather than a hollow lip. Colors, bow, radius
  growth, and envelope are unchanged, and the standard's rule 1 mandates
  the irregular geometry, but if any single swap gets reversed on
  Carter's PC look, it is this one.
- The rise crescent is a 250 degree bladeArc where the old parametric
  arc spanned about 306 degrees; with the toolkit's pinched, faded ends
  the crescent reads slightly narrower. Radius, climb rate, and layer
  alphas are unchanged.
- brokenVortexBand breaks about 16 percent of its columns by seed, so
  the dust ring is deliberately gappy now; at far distances the segment
  step-down plus gaps could read sparse. Three counter rotating layers
  at different radii are the mitigation.
- HIT_METAL for the old ANVIL_HIT is the one sound swap that is a
  judgment call rather than a named pair in the guide; SLAM_THUD at a
  lower volume is the fallback if it reads too light in game.
