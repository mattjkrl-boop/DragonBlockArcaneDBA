# Spectacle Report: Bansho Fan (Phase 2)

Agent scope: item/BanshoFanItem.java, entity/BanshoCycloneEntity.java,
entity/BanshoGaleEntity.java, entity/BanshoShockwaveEntity.java,
entity/BanshoWindProjectileEntity.java, and
client/render/BanshoCycloneRenderer.java. No shared files edited.
Base: HEAD 5e4b1de. Theme kept: emerald and jade wind, the great cyclone.

## The big change: the cyclone is a cast now, not a proc

Before: a landed melee hit had a 30 percent chance to spawn a 25 tick
cyclone. The storm was a brief accident of melee, too short for any
lifecycle, and it could re-trigger at melee cadence.

After: every landed melee hit casts the cyclone at the target, gated by a
per-player storm cooldown of 120 ticks stored in the item, and blocked
while one of the caster's own cyclones is already standing within 24
blocks. The cooldown (120) exceeds the cyclone's life (100), so two
cyclones from one fan can never stack, per the standard's numbers
guidance. No charge mechanic exists for this weapon (its left click
routes a plain swing in DbaNetwork, no charge ticks), so none was
invented; the cast gate is the honest replacement for the proc.

## Before and after numbers

### Great Cyclone (was Cyclone Slash proc)

| Value | Before | After |
|---|---|---|
| Trigger | 30% chance per landed melee hit | Guaranteed cast on landed hit, 120 tick per-player cooldown, one active storm per caster |
| Lifetime | 25 ticks | 100 ticks (20 build / peak / 35% dissipate) |
| Effect radius | 4.0 blocks | 4.0 blocks (unchanged) |
| Visible funnel wall | 1.33 blocks max (0.38 x 3.5 cone) | Outer band exactly 4.0 blocks, equals the effect radius |
| Height | 6.5 blocks | 6.5 blocks (unchanged) |
| Damage figure | (400 + Strength x 1.5) x 0.5 | Same figure |
| Damage ticks | Every 5 ticks, all 25 ticks, 5 ticks max | Every 4 ticks inside the active window only (ticks 20 to 80), 16 ticks |
| Damage per tick | 35% of figure (70 + scale) | 11% of figure |
| Full-stay total | 175% of figure = 87.5% of main hit | 176% of figure = 88% of main hit |
| Bleeding | I, 100 ticks, per damage tick | I, 100 ticks, per damage tick (unchanged) |
| Mob forces | Hand-rolled spin/suction/lift every tick | VortexPhysics.applyVortex every tick until ejection: pull 0.30, swirl 1.6, lift 0.55, envelope ramped, weight classes and player 0.6 factor from the shared block |
| Ejection | None; held mobs dropped at discard | At tick 82 every held target is flung out (power 1.15, up 0.9, weight scaled) |
| Particles | About 25 WIND per whole life | LayeredParticles.emitVortex every tick (core KI_MOTE + emerald dust mid band + WIND outer wisps, envelope and density scaled), plus emitRain WIND drizzle through the decay |
| Debris | 0 | 64 pieces (density scaled) of sampled ground blocks, 3 sizes, helical orbit, release and rain down during the decay |
| Ground | Nothing | Base dust burst on cast; scouring every 10 ticks in the active window: grass to dirt, dirt/podzol/mycelium to coarse dirt, radius 3 disk, 60% per cell, GroundScar restore 240 ticks |
| Atmosphere | None | AtmosphereServer Kind.RAIN, radius 24, gloom 0.45, for the cyclone's 100 tick life |
| Stamina | None (hurtEnemy by design) | Unchanged |

### Gale Swing (left click)

| Value | Before | After |
|---|---|---|
| Damage | 400 + Strength x 1.5 | Unchanged |
| Stamina | drainStaminaDiscrete 45.0 over 10 | Unchanged |
| Gale life / speed | 10 ticks at 1.35 per tick | Unchanged |
| Block collision | None: flew through walls, hit targets behind them | Clips each step like the wind projectile; on a block face it bursts there: radial shockwave 2.2 with ORIENT_TO_IMPACT, ground dust burst, burst sound, discard |
| Trail | 6 WIND per 2 ticks | Unchanged |

### Tempest Barrage (right click)

| Value | Before | After |
|---|---|---|
| Projectiles | 5, spread as before | Unchanged |
| Projectile damage | 300 + Spirit x 1 each | Unchanged |
| Cooldown | 40 ticks | Unchanged |
| Stamina | drainStaminaDiscrete 45.0 over 40 | Unchanged |
| Trail | 2 WIND per 2 ticks | Every tick: 1 KI_MOTE core mote + 2 WIND wisps (density scaled) |
| Block hit | Shockwave 2.0 only | Plus LayeredParticles ground dust burst at the strike point |

### Melee (hurtEnemy) and shockwaves

Melee numbers unchanged (400 attribute, Gale Force launch, Bleeding II
300 ticks, Strength x 1.5 bonus, radial shockwave 3.2 on hit).
BanshoShockwaveEntity is still purely visual with signature-compatible
constructors (the WindChargeMixin caller is untouched), but radial
shockwaves now emit WIND wisps off the expanding rim every 2 ticks
(oriented bursts emit a small puff instead), so the rings are no longer
mesh-only.

## Per point: what was added

1. LAYERED DEPTH. The cyclone funnel is now LayeredRender.drawVortexBands:
   bright core band (0.38x), emerald mid band, wispy outer band (1.45x)
   counter-rotating at three speeds, standard band alphas, distance
   scaling via LayeredRender.distScale, all multiplied by the shared
   FxEnvelope (20 percent build, 35 percent decay, the same fractions the
   entity uses). The mid band radius is set so the outer band lands
   exactly on the 4.0 effect radius: the visible wall equals the hit
   wall. LayeredRender.drawAnchorGlow plants the storm on the ground.
   The suction ring, eye ring, triple jade ribbons, 8 orbiting blades,
   and crown ring are kept as the weapon's identity, re-based on the
   growing funnel radius and the same envelope. Server side,
   LayeredParticles.emitVortex feeds the funnel every tick. The wind
   projectile trail and the shockwave rims gained particle layers too.
2. DEBRIS. DebrisSpawner.spawnField at the build peak (tick 20): 64
   pieces sampled from the actual ground under the storm, three sizes,
   helical orbits, released with tangential velocity so they rain back
   down through the dissipation. Storm class count (60+) met.
3. WORLD INTERACTION. Base dust burst on the cast tick. Ground scouring
   through GroundScar (soft ground only, protected blocks refused inside
   GroundScar, crash safe restore). VortexPhysics does the pull, swirl,
   and lift with boss/heavy/light weight classes. The gale now collides
   with blocks instead of flying through walls.
4. ATMOSPHERE. The cyclone cast calls AtmosphereServer.begin with
   Kind.RAIN (the standard names Bansho wind as a rain kind), radius 24,
   gloom 0.45, 100 ticks: real server rain plus client gloom for the
   storm's life, prior weather restored by the shared system. The
   barrage and gale gained no weather; they are not storm casts.
5. LIFECYCLE. 100 tick life on one envelope shared by renderer alpha and
   scale, particle counts, vortex strength, and the damage window.
   Build-up: the funnel grows from 45 percent radius and 35 percent
   height while forces ramp. Peak: full density, debris orbiting,
   damage ticking every 4 ticks. Dissipation: forces stop at the
   ejection beat (tick 82), held mobs are flung out, debris rains,
   emitRain drizzles wind wisps, alpha falls to zero before discard.
   Nothing pops in and nothing vanishes at visible alpha.

## Bugs fixed

- Visual/effect radius mismatch (audit): funnel wall 1.33 vs effect
  radius 4.0. Now the outer band sits at 4.0.
- Gale through walls (audit): BanshoGaleEntity had no block collision
  while the fan's own projectile clipped. Now both clip.
- Item header lied (audit): it promised "50% damage" while the entity
  dealt 35% of that figure per tick. The header now states the real
  per-tick number, tick count, and full-stay total.
- Dead helper (audit): BanshoCycloneRenderer.drawTriangle was defined
  and never called. Removed. (The task brief said drawQuad overload;
  in the file drawQuad is live and used by the rings and ribbons, and
  drawTriangle is the dead one. drawQuad was kept and made double
  sided.)
- Single-sided funnel (audit): the cyclone's drawQuad emitted one
  winding, so the funnel vanished from inside. The shared bands draw
  both windings, and this renderer's drawQuad now emits both windings
  too.
- Cyclone proc stacking and pop cadence (audit): replaced by the gated
  cast above.
- The old cyclone's hand-rolled forces ignored weight classes entirely
  (a boss moved like a chicken). VortexPhysics applies the shared
  weight scale now.

## Cyclone renderer: what was removed and why

The old 10 level cone mesh and the spindle core are gone: the layered
bands replace them (core band is the bright spine, mid band is the
wall, outer band is the wispy edge the cone never had). The ribbons,
blades, rings, and crown stayed because they are the weapon's
signature shapes and the audit did not fault them.

## Shared change requests

None. Every shared block needed already existed with the right
signatures (FxEnvelope, LayeredParticles, VortexPhysics, DebrisSpawner,
GroundScar, AtmosphereServer, LayeredRender).

## Verification

Dual javac sweeps (audit harness, -Xmaxerrs 10000, error kinds compared
with line numbers stripped) against a pristine git archive of HEAD
5e4b1de. Baselines: 132 error kinds on 26.2, 141 on 26.3.

Bansho scoped result, the one that decides this weapon: the raw javac
output contains ZERO error lines mentioning any Bansho file, on 26.2
and on 26.3, both at baseline and after these changes. All six Bansho
files compiled clean in the audit environment before the pass and
still compile clean after it.

Whole tree note: the current tree shows 139 kinds on 26.2; the 7 kinds
not in the baseline are all in other weapons' files (KatanaCharge,
LavaEruption, SpiritImpale, ZShockwave, BlasterGun, CurseBlade,
SpiritSword), which sibling agents were editing at the same time, so
the tree was moving during the sweep. The 26.3 whole-tree kind list
could not be extracted cleanly for the same reason (a sibling file's
non-UTF8 bytes made javac's output read as binary to the extractor).
The coordinating pass's final audit on a settled tree owns the whole
project verdict; the Bansho scoped verdict above is complete.

## Honest worse check

- Cyclone availability: a guaranteed storm every 120 ticks is stronger
  than a 30 percent proc on paper, but the old proc could also fire
  several times inside 120 ticks of melee spam, each time restarting
  the shred. The new storm is one longer, telegraphed event with the
  same full-stay damage total (88 percent vs 87.5 percent of the main
  hit). Judged better, not worse: it trades spam for an event, which
  is the point of the pass.
- Gale wall burst: gale swings into a wall now stop at the wall. That
  is a real play change (no more hitting through walls), intended and
  listed by the audit as a bug.
- The funnel no longer has the old solid cone mesh; at very low effect
  density settings the particle layers thin out and the read leans on
  the bands, ribbons, and blades. The bands are mesh, not particles,
  so the funnel still reads at density 0.
- Nothing was reverted. No file outside the assigned set was touched.
- The Minecraft client cannot run in this sandbox; the final in-game
  look judgment is Carter's on his PC, same as every prior pass.
