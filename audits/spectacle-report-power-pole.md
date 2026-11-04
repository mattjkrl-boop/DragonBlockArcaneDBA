# Spectacle Report: Power Pole (Phase 2)

Agent scope: item/PowerPoleItem.java, entity/PowerPoleWhirlwindEntity.java,
entity/PowerPoleExtensionEntity.java, entity/PowerPoleImpactEntity.java,
client/render/PowerPoleWhirlwindRenderer.java,
client/render/PowerPoleExtensionRenderer.java,
client/render/PowerPoleImpactRenderer.java. No shared files were edited.
Base commit: 5e4b1de. Theme kept: crimson and gold extending staff, emerald
and cyan-white wind cone, gold and crimson impact burst. Nothing was
recolored or re-identified.

## Before and after numbers

| Value | Before | After |
| --- | --- | --- |
| Whirlwind entity | New entity every 6 ticks, life 8 ticks, envelope restarting forever | ONE sustained entity per spin: build 10 ticks, sustain while held (refresh grace 6 ticks), dissipate 24 ticks. Minimum life about 40 ticks on a tap, unbounded while held |
| Spin damage window | Damage and deflection from the first call, every call | Damage and deflection only inside the active window: after the 10 tick build, while envelope is above 0.25 |
| Spin damage per hit | 750 / max(1, distance / 3), per target gate 10 ticks | Unchanged formula and gate. Gate key fixed (see bugs) |
| Spin hit gate key | Target entity id only, shared across all players | Packed (player id, target id) pair |
| Spin particles (density 1.0) | About 65 per second: 14 WIND per 6 ticks from the item, about 3 WIND per 2 ticks from the entity | About 330 per second at peak: LayeredParticles.emitVortex at the wielder every tick (core 3 + mid 5 + outer 3) plus the same at the cone midpoint every 2 ticks, all scaled by envelope and effectDensity |
| Spin mob forces | On-hit gale repulsion only | VortexPhysics.applyVortex every tick, two zones: wielder radius 4.5 (strength 0.16, swirl 1.6, lift 0.30), cone midpoint radius 5.0 (strength 0.10, swirl 1.2, lift 0.18), envelope and weight class scaled. On-hit gale repulsion kept |
| Spin end beat | Entity discarded at 8 ticks, mobs keep whatever velocity they had | On dissipation start: VortexPhysics.fling on everything held within radius 5 (power 0.9, up 0.5), then the funnel fades over 24 ticks |
| Debris, spin | 0 pieces | 8 pieces every 20 ticks while at peak: ground sampled palette, 3 sizes, orbit radius up to 3.0, rise 3.5, orbit 26 ticks, then fall and fade on landing |
| Debris, poke | 0 pieces | 24 pieces per landed poke: palette sampled at the hit point, orbit radius up to 2.2, rise 2.5, orbit 18 ticks |
| Poke block reach | Blocks checked only inside the player's short POV reach (about 5 blocks); past that the pole passed through walls and the entity hit always won | Block clip over the full 30 blocks; the nearer of block or entity wins, and the extension length stops at the winner |
| Poke damage | 750 flat through capDamage | Unchanged |
| Poke stun | POLE_STUN 100 ticks amp 0; 15 percent amp 1 for 100 to 140 ticks | Unchanged |
| Poke cooldown | 40 ticks | Unchanged |
| Stamina (this weapon has no Ki cost) | Spin: drainStaminaDiscrete 20 percent per minute prorated over 10 ticks, per call. Poke: 20 percent per minute prorated over 40 ticks | Unchanged, both |
| Impact entity life | 14 ticks | 16 ticks (renderer fade is keyed to it) |
| Impact particles | 0 | Tick 1: SPARK 10 + KI_MOTE 6 at density 1.0. Tick 8: WIND 6. Plus item-side ground burst: 14 gold dust + POOF puffs at density 1.0 |
| Extension particles | 0 | Per tick: 2 WIND streaming along the shaft + 2 KI_MOTE at the tip, at density 1.0, following the renderer's extension fraction |
| Extension retract alpha | Bottomed out at 0.65, then the entity discarded: visible pop | Fades fully to 0.0 across the retract, reaching zero exactly at the tick 12 discard |
| Ground scar | None | Poke landing on grass from above: grass to dirt over a 3 by 3 area at 70 percent chance per cell, restored after 120 ticks by GroundScar |
| Renderer segments, whirlwind and impact | Fixed 28 (cone, rings), 24 (ribbons, dome) | Distance scaled via LayeredRender.distScale and FxEnvelope.segments: full detail near, stepping down to minimums of 12 / 10 / 14 beyond about 96 blocks |
| Gust audio | Sweep + gust every 6 ticks with each respawn | Sweep + gust once on spawn from the item, then sweep + gust every 16 ticks from the entity while sustaining |

Sustained spin damage total, stated per the standard: a target that stays in
the cone takes 750 / max(1, distance / 3) every 10 ticks for as long as the
spin is held and the stamina drain (20 percent per minute) can be paid. At
3 blocks or closer that is 750 per hit, 1500 per second, same as before;
the change is that the first 10 ticks now deal nothing while the funnel
visibly builds, and targets are swirled and held by the vortex instead of
only being repelled on hit ticks.

## What was added, per point

1. Layered depth. The whirlwind now draws the shared
   LayeredRender.drawVortexBands along the cone axis (bright core, emerald
   mid, wispy outer, counter rotating, envelope and distance scaled), with
   the band frame turned so the wide bright base sits at the cone mouth and
   the column tightens back toward the staff. LayeredParticles.emitVortex
   feeds two particle volumes (wielder column, cone midpoint) in the three
   shared layers: KI_MOTE core, emerald dust mid band, WIND outer wisps.
   The existing gradient cone sheaths, ribbons, blades, and rings were kept
   and now swell with the envelope (0.55 to 1.0) and step down with
   distance. The extension gained an outer pale gold glow sheath around its
   3 band crimson shaft and inner core, a translucent tip wake cone, and a
   real particle trail. The impact gained SPARK, KI_MOTE, and WIND particle
   layers under its rings and dome.
2. Debris. DebrisSpawner fields with ground sampled palettes and three
   piece sizes: a fresh handful every second during the spin, 24 pieces on
   every landed poke. Pieces orbit helically, release, fall, land, and
   crumble (DebrisEntity's own phases), so nothing vanishes mid-air.
3. World interaction. VortexPhysics swirls, lifts, and holds mobs in two
   zones for the spin's whole life, with a fling ejection as it dies. The
   poke bursts dust where it lands (LayeredParticles.emitGroundBurst),
   throws real debris, and tears turf (grass to dirt, GroundScar restored)
   when it comes down on grass from above. The poke also stops at walls
   over its full reach now instead of passing through them.
4. Atmosphere. The Power Pole has no weather or darkness theme, so per the
   standard it gained none. Its atmosphere is light and motion: layered
   glow bands, particle volume that breathes with the envelope, and gust
   audio under the sustain. No AtmosphereServer call was added on purpose.
5. Lifecycle. The whirlwind is one event with one envelope: build over 10
   ticks, sustain, dissipate over 24 ticks, evaluated by identical phase
   math in the entity (particle counts, damage window, vortex strength)
   and the renderer (alpha, swell), keyed on age plus the synced
   DECAY_START. Damage lands inside the visual's active window only.
   Decay falls from whatever height the build had reached, so an
   interrupted spin never jumps to full alpha before fading. The
   extension's retract now fades to zero at its discard tick. The impact
   ring that used to key off the dead 8 tick lifetime now loops its travel
   for the effect's real life. Re-pressing during dissipation discards the
   dying funnel and starts a fresh one.

## Bugs fixed

- SPIN_HIT_TIMES was keyed by target entity id only and shared across all
  players: two players spinning near the same mob shared one 10 tick gate
  and one player's spin suppressed the other's hit. Now keyed by the
  (player, target) pair.
- Dead drawTriangle in PowerPoleWhirlwindRenderer removed (defined, never
  called; verified by grep before removal).
- PowerPoleExtensionRenderer retract alpha bottomed at 0.65 and the entity
  discarded at tick 12, a visible cut. Alpha now reaches 0.0 at discard.
- The whirlwind's 8 tick life with a 6 tick respawn cadence restarted its
  fade envelope forever; replaced by the sustained entity above.
- Poke block clipping used the player's short POV reach helper while the
  entity test used the full 30 blocks, so the pole visually pierced walls
  beyond about 5 blocks and could hit targets behind them. Full range
  clip, nearer hit wins.
- Found during self audit and fixed before finishing: the first rewrite of
  the entity tick dropped the client side eye lock (the old entity
  followed the owner on the client too); restored, freezing when decay
  starts, matching the server.
- Found during self audit and fixed: the decay envelope initially assumed
  a full build peak, so a spin interrupted mid-build would have jumped up
  in alpha at dissipation start; it now decays from the reached height,
  identically on both sides.

Noted from the fresh audit, deliberately not changed: the extension
renderer's helper is named draw12SidedShaft but builds an 18 sided shaft.
Cosmetic name mismatch only; renaming was left out to keep this diff
focused on the spectacle work.

## Shared change requests

None. Every shared block the standard assigns (FxEnvelope, LayeredParticles,
DebrisSpawner, VortexPhysics, GroundScar, LayeredRender) already exposed
what this weapon needed, and no shared file was edited.

One observation for the coordinating pass, from the verification sweep: the
shared working tree currently carries 3 new javac error kinds on 26.2 and
5 on 26.3 in OTHER weapons' in-progress files (KatanaChargeRenderer,
ZShockwaveEntity, CurseBladeItem, KingsSlamEntity, OxShockwaveEntity).
Those are sibling agents mid-flight, not Power Pole code; flagging them
here so the final merge audit expects them.

## Verification

Dual javac sweeps with the standing audit harness (javac against the 26.2
and 26.3 classpaths, error kinds compared with line numbers stripped).
Baseline at 5e4b1de, measured twice (the first run's /tmp outputs were
lost to a VM /tmp wipe and the baseline was reproduced): 132 unique error
kinds on 26.2, 141 on 26.3. Current shared tree (all Phase 2 agents
mid-pass): 135 on 26.2, 146 on 26.3. New kinds attributable to Power Pole:
ZERO on both versions; a grep for PowerPole across both full error logs
returns 0 lines. The only new kinds are the sibling files listed above.
Gradle was not run: the sandbox cannot connect to the Gradle daemon (the
standing, previously recorded limitation), so this audit is the
verification, not a build pass. The Minecraft client cannot run in this
sandbox, so the final in-game look of the bands along the cone axis is
Carter's call on his PC; the transform math was checked by hand against
the renderer's existing cone frame.

## Worse check

Nothing was reverted. Damage numbers, cooldowns, stun, and stamina drains
are byte-identical to before, so the weapon cannot play weaker. The one
deliberate play-feel tradeoff: spin damage now waits for the 10 tick
build, per the standard's telegraph rule, while the stamina drain still
runs during the build (it runs per call, unchanged from before). In
exchange the funnel is a persistent vortex that holds and spins targets
instead of a strobe of 8 tick cones. If Carter judges the build delay too
long in game, BUILD_TICKS in PowerPoleWhirlwindEntity is the single knob
and the renderer follows it automatically.
