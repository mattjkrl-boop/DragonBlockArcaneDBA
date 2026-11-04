# Spectacle Report: Grand Sword

Phase 2 weapon agent report. Before picture: audits/fresh-audit-blades.md,
Grand Sword section (8 abilities, 17 issues), at commit 5e4b1de. Standard:
audits/SPECTACLE-STANDARD.md. Files touched, and only these:
item/GrandSwordItem.java, entity/GrandBladeShardEntity.java,
entity/GrandClashSparkEntity.java, entity/GrandCrescentWaveEntity.java,
entity/ValorFieldEntity.java, and the four matching renderers
(GrandBladeShardRenderer, GrandClashSparkRenderer, GrandCrescentWaveRenderer,
ValorFieldRenderer). Theme unchanged throughout: colossal golden blade,
valor field, clashing sparks.

## Before and after numbers

| Value | Before | After |
| --- | --- | --- |
| Spin charge multiplier (finisher and wave) | 0.7 + 0.6 x ratio: tap 0.7, full 140 tick spin 1.3 (1.86x) | 0.8 + 1.6 x ratio: tap 0.8, full spin 2.4 (3.0x) |
| Finisher slash damage | (450 + 2.0 x Strength) x 0.7 to 1.3 | (450 + 2.0 x Strength) x 0.8 to 2.4 |
| Crescent Wave damage | (600 + 2.5 x Strength) x 0.7 to 1.3 | (600 + 2.5 x Strength) x 0.8 to 2.4 |
| Finisher cooldown | 40 ticks (2 s), shorter than the 140 tick charge feeding it | 160 ticks (8 s), longer than the charge and longer than the wave's 80 tick life |
| Spin contact damage | 100 + 0.5 x Strength, full value from the first tick | Same base x ramp 0.4 rising to 1.0 over the first 60 spin ticks |
| Spin contact radius | 2.6 blocks, flat | 2.6 growing to 3.5 with spin progress (the header's promised range growth, now real) |
| Spin contact knockback | Away 0.75 horizontal, 0.25 up | Pull in 0.45 horizontal, 0.12 up (cyclone holds victims in the arc; the finisher flings them out) |
| Blade Shard speed | 1.5, flat | 1.5 growing to 1.9 with spin progress |
| Finisher knockback | Look x 1.4, 0.3 up | Look x 1.4 plus radial 0.6 out of the cyclone, 0.35 up |
| Finisher bounciness modifier | Permanent attribute modifier, no removal anywhere | Timed modifier, expires after 100 ticks |
| Clash Spark life | 10 ticks (0.5 s), full alpha on tick 1 | 24 ticks under FxEnvelope (build 20 percent, dissipate 45 percent) |
| Valor Field release cooldown | 20 ticks (1 s) | 60 ticks (3 s) of real downtime |
| Valor Field removal | Instant discard on every path, dome vanishes in one tick | 12 tick fade out on every path (release, Ki out, caster death, not holding, disconnect) |
| Valor Field buffs | VALOR holder only; header promised vanilla buffs that were never applied | VALOR plus the promised vanilla effects: caster Strength III, Resistance III, Haste II; allies Strength II, Resistance II; enemies Slowness II (all 40 ticks, refreshed every 10) |
| Wave block contact | Instant discard at full brightness | Debris field of 26 pieces, ground burst, coarse dirt scar (200 tick restore), then 12 tick dissipate |
| Shard flight / embedded life | One shared tickCount drove both lifetimes | Separate counters: flight 60 ticks, embedded 200 ticks |
| Ki drain | 90 percent of max Ki per minute on spin and field (unchanged) | Same numbers; the item header's false "4 percent per second" line corrected to 90 percent per minute (1.5 percent per second) |

Ki costs were already charged only while the moves actually run (per tick
drain, release on drain failure), and remain so; no new discrete Ki charges
were added and none are charged on a blocked cast.

## What was added, per point

1. LAYERED DEPTH. The spin's single flat ring of 10 SPARK points is replaced
   by the shared LayeredParticles.emitVortex around the player (bright
   KI_MOTE core, gold dust mid band, wispy SPARK outer layer), called every
   2 ticks with the envelope held at peak. The Valor Field's near empty
   interior (about 4 SPARK every 4 ticks across a 9 block radius) is filled
   by the same emitVortex at field scale, and its renderer gains a shared
   LayeredRender.drawAnchorGlow disc at the dome base. The flying shard
   trail gains a KI_MOTE layer around its SPARK core. The wave renderer now
   distance scales its crescent segments through LayeredRender.distScale
   and FxEnvelope.segments.
2. DEBRIS. The finisher release spawns a DebrisSpawner field of 28 ground
   pieces at the player (finisher class, above the 24 piece floor) that
   orbit, rise, and rain back down. The wave's block strike spawns 26
   pieces at the impact point. Shard impacts spawn small fields: 8 on block
   embed, 6 on a direct hit, 8 on the embedded hazard shatter.
3. WORLD INTERACTION. The spin pulls enemies into the blade arc and holds
   them there instead of knocking them away, and the finisher flings them
   radially out. Ground bursts fire at the spin start and every 30 ticks
   while spinning, at the finisher release, and under the wave when it
   flies low over solid ground. GroundScar leaves a temporary coarse dirt
   scar under the finisher (radius 1, or 2 past two thirds charge, 240 tick
   restore) and at the wave's impact point (radius 1, 200 tick restore).
4. ATMOSPHERE. The Grand Sword has no weather theme and gains none, per the
   standard. Its atmosphere is light and particles: the clash spark now
   emits envelope scaled SPARK and KI_MOTE motes at the clash point, the
   melee connect bursts gold sparks on the target, and the field's spawn
   gains a ground burst under the beacon sound.
5. LIFECYCLE. FxEnvelope or the same smoothstep shape now drives every
   effect: clash spark builds and dissipates over 24 ticks in both the
   entity's particles and the renderer's alpha and growth; flying shards
   fade in over 3 ticks and out over their last 10 flight ticks; embedded
   shards fade out over their last 30 ticks; the wave fades on the shared
   envelope and, on a block strike, hangs for 12 ticks fading from the hit
   tick instead of discarding; the Valor Field fades out over 12 ticks on
   every removal path with dissipation rain, and its suspended projectiles
   resume at fade start instead of snapping back the tick the dome
   vanishes. Spin contact damage ramps up with the spin instead of landing
   at full value on tick 3.

## Bugs fixed (audit issue numbers)

1. Item header Ki contradiction: corrected to the real 90 percent per
   minute (1.5 percent per second). The Valor Field header now also states
   the same drain.
2. Header promised spin range growth that did not exist: contact radius
   and shard speed now actually grow with spin progress.
3. Header said projectiles are deflected or destroyed: header now says
   deflected (reversed), which is what the code does.
4. Valor Field promised buffs now applied (see numbers table).
5. Valor Field header stasis duration corrected from 4 s to the real
   10 s (200 ticks).
6. Blade Shard header claimed vanilla Slowness stacking and a Movement
   Curse: corrected to describe the Valor Stun stacking the code applies.
7. Unused imports removed from GrandBladeShardEntity (DustParticleOptions,
   MobEffects).
8. Dead drawFlyingSword method (about 7,300 characters, never called)
   removed from GrandCrescentWaveRenderer.
9. Valor Field renderer comment said fade in over 6 ticks; code divides
   by 10. Comment corrected.
10. Valor Field fade out path added (entity beginFadeOut plus a synced
    fade start the renderer consumes); every removal path routes through
    it, including releaseUsing and disconnect in the item.
11. Clash Spark duplicated lifetime state (maxLifetime and lifetime
    fields that only agreed by accident): single MAX_LIFETIME constant of
    24, one counter.
12. Shard flight and embedded lifetimes decoupled into separate counters.
13. Stasis cage false positives: the field now syncs the network ids of
    the projectiles in its own suspended map (a synced string, updated
    only when the set changes), and the renderer cages only those ids. It
    no longer cages any slow projectile that wanders into the radius.
15. Finisher bounciness is a timed modifier now (100 ticks) instead of a
    permanent one with no removal.
16. isAlly no longer treats every player as an ally: it uses real team
    alliance only, so an opposing player in the field is debuffed and
    pressure damaged instead of buffed.
17. Melee hurtEnemy is no longer an empty stub: a connect now bursts gold
    sparks and motes on the target. It still costs no Ki and adds no
    damage, by the original design note.

Not fixed, deliberately: audit issue 14, the single target caps inside the
shard hazard, the flying shard hit, and the suspended projectile collision.
Those read as designed single target resolutions of small effects, and
widening them would change the weapon's balance beyond the spectacle brief.
Flagging here so the coordinating pass can overrule.

## Shared change requests (not made, per coordination rules)

- effect/ValorEffect.java's class doc says the Valor effect deliberately
  avoids vanilla Strength, Resistance, and Haste. The field now applies
  those vanilla effects alongside VALOR because the field's own design
  promises them. If the coordinating pass prefers one source of truth, the
  ValorEffect doc should be updated to match this decision.
- The standard names LayeredRender.drawLayeredDome, but LayeredRender only
  ships drawVortexBands and drawAnchorGlow. The Valor Field dome keeps its
  own proven mesh plus the anchor glow. If a shared dome helper lands, the
  field renderer can adopt it in a later pass.
- LayeredParticles.emitRain takes an age and life and inverts the envelope
  internally, so a standalone dissipation call has to pass a synthetic age
  (19 of 20) to get full strength rain. A direct strength based overload
  would be cleaner for release and fade out calls like the finisher's.
- GroundScar.scarDisk writes at one fixed Y level. The finisher and the
  wave each pick the ground block Y themselves; a surface following
  variant would handle slopes better.

## Worse check

Re-read every changed hunk after editing. Nothing was reverted. Honest
notes:

- The one real feel change is the spin: it now pulls enemies in and holds
  them instead of knocking them away, and its contact damage starts at
  0.4x while the spin builds. The first seconds of a spin are weaker than
  before; a spun up cyclone is stronger and the release now flings what it
  held. That trade is the standard's cyclone reading (pull, lift, fling),
  applied on purpose, and it is the change to reverse first if Carter
  dislikes the feel.
- The clash spark starts smaller (scale 0.55 growing to 1.8) instead of
  popping in at 0.85. That is the build phase working as intended, not a
  dimming: its peak exceeds the old peak and it lives 2.4x longer.
- While a Valor Field fades (12 ticks), onUseTick will not spawn a
  replacement dome until the old one is removed. Deliberate: it prevents
  two overlapping domes, and the new 60 tick release cooldown covers the
  window anyway.
- Verification is the javac audit only, per the standing sandbox rule
  (Gradle cannot run here; the build waiver is Carter's). Dual sweeps of
  the whole tree against the 26.2 and 26.3 audit classpaths show zero
  error lines in any of the nine Grand Sword files on either version
  (643 ambient error lines on 26.2 and 677 on 26.3, all in other files).
  During the sweeps other Phase 2 agents were editing their own weapons
  in the same tree; every new error kind that appeared belongs to those
  files, none to this weapon. The final in game look is Carter's to judge
  on his PC.
