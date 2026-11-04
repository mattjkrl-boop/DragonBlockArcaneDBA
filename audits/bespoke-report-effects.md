# Bespoke Pass Phase 2 Report: Status Effects (effect/)

Scope: every class under src/main/java/com/dragonblockarcanedba/effect/ that
referenced vanilla ParticleTypes, DustParticleOptions, or vanilla SoundEvents.
Only particle and sound construction changed. No durations, amplifiers, tick
logic, colors, or damage were touched. Colors carried on dust are byte-identical
to before; only the carrier changed, from vanilla DustParticleOptions to the
mod's DbaParticles.THEME_DUST with DbaDustOptions, per BESPOKE-STANDARD Rule 2.
HeroicFocusEffect keeps its spectacle-merge gold 0xFFD700.

## Before and after counts (effect/ directory)

| Reference | Before | After |
|---|---|---|
| DustParticleOptions (imports + constructions) | 54 lines (25 imports, 29 constructions) | 0 |
| ParticleTypes references | 4 (BLOCK x1, PORTAL x2, SMOKE x1) | 0 |
| ParticleTypes / DustParticleOptions imports | 33 | 0 |
| Vanilla SoundEvents references | 0 (none existed) | 0 |
| DbaDustOptions constructions | 0 | 29 |
| DbaParticles references (BLOCK_CHUNKS, VOID_SMOKE, DUST_PUFF) | 2 (pre-existing VOID_SMOKE in two constructors) | 6 |

Verification grep after the swaps: zero matches for "ParticleTypes" or
"DustParticleOptions" anywhere under effect/.

## Files changed (27, all under effect/)

Dust to THEME_DUST (import plus every construction swapped, color and scale
unchanged): AncientWeightEffect, BladeGuardEffect, BleedingEffect,
CelestialGraceEffect, DemonSurgeEffect, DevilsHandsEffect,
EnergyOverchargeEffect, HeroicFocusEffect, JudgementLockEffect,
MarkedByEvilEffect, MeltingEffect, MovementCurseEffect, OxBraceEffect,
PetrificationCurseEffect, PoleStunEffect, SilentMarkEffect,
SpiritImpaleEffect, StormOfDarknessEffect, TemporalStasisEffect,
ValorEffect, ValorStunEffect.

Dust plus a ParticleTypes swap: RiftedEffect (dust 0x9400D3 kept;
ParticleTypes.PORTAL to DbaParticles.VOID_SMOKE), SorrowRiftEffect
(dust 0x8A2BE2 kept; ParticleTypes.PORTAL to DbaParticles.VOID_SMOKE).

ParticleTypes swap only: EarthShatterEffect (BlockParticleOption now
carries DbaParticles.BLOCK_CHUNKS with the same dirt BlockState),
FissureStunEffect (ParticleTypes.SMOKE to DbaParticles.DUST_PUFF).

Dead import removal only: DarkFadedEffect and HollowedEffect imported
DustParticleOptions and ParticleTypes but never used either (their ambient
particle was already DbaParticles.VOID_SMOKE, fully qualified); both imports
removed. MovementCurseEffect and StormOfDarknessEffect also carried an unused
ParticleTypes import; removed alongside their dust swaps.

## Swap choices for types the standard's map does not name

- PORTAL (Rifted, SorrowRift) to VOID_SMOKE: the standard's Rule 2 table
  covers POOF, CRIT, FLAME, EXPLOSION, BLOCK, and dust only. Rifted and
  SorrowRift are void and rift family effects, and VOID_SMOKE is the mod's
  dark violet drifting style (base color 0.35, 0.10, 0.50), already the
  ambient particle of the sibling void effects DarkFaded and Hollowed.
  Emission counts, positions, and velocities at both call sites are
  unchanged; only the type token changed.
- SMOKE (FissureStun) to DUST_PUFF: the soft off-white expanding puff is
  the closest bespoke match for a dust puff at a stunned target's feet,
  and it is the standard's designated puff family. Count and spread
  unchanged.

## Out of scope, left alone for the coordinating pass

Five effect files deal damage with vanilla damage sources as part of the
effect itself (not a weapon). Per my brief these were NOT re-typed:
BleedingEffect (magic), DarkFadedEffect (magic), MeltingEffect (magic),
SorrowRiftEffect (magic), DevilsHandsEffect (onFire). If the coordinating
pass wants effect-internal damage on DbaDamage families, these five call
sites are the complete list.

No effect file referenced vanilla SoundEvents, so Rule 3 needed no swaps
in this directory.

## Verification

Targeted javac of all effect sources with the full sourcepath closure,
JDK 25, on both audit classpaths: zero errors in any effect/ file on
26.2 (cp262) and zero on 26.3 (cp263). The only errors in those runs sit
in unrelated dependency files pulled in by the closure (DbaItems,
DbaMenus, mob entities, LavaSwordGround, and similar), which are
pre-existing baseline kinds from the 57902e1 baseline and are in files
this task never opened. This is a javac audit, not a Gradle build; no
build pass is claimed.

Diff discipline check: filtering the git diff of the 27 files for any
changed line that is not a particle construction, a particle type token,
or an import returns nothing. Every color value, scale, count, velocity,
and spread argument is identical to the pre-swap code.

## Honest worse check

Nothing reverted. Two honest differences, both consequences of the
standard rather than accidents: (1) THEME_DUST renders through the mod's
own soft additive glow style instead of vanilla's dust sprite, so the
same colors will read slightly brighter and softer in game; that is the
pass working as intended, and the dust color values themselves are
unchanged. (2) VOID_SMOKE drifts darker and slower than vanilla PORTAL
specks at the two rift call sites; it matches the void family the
effects belong to, but if Carter wants the brighter vanilla portal
sparkle back, the coordinating pass can add a dedicated rift sparkle
type to DbaParticles instead. No behavior, timing, or numbers changed
anywhere in this directory.
