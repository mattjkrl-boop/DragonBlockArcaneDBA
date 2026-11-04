# Spectacle Report: Dimensional Sword

Phase 2 weapon pass, from the fresh audit at audits/fresh-audit-dark.md section 4
and the shared standard at audits/SPECTACLE-STANDARD.md. Theme unchanged
throughout: space cutting, violet magenta tears, the 1024 block warp.

Files changed (only these):
- item/DimensionalSwordItem.java
- entity/DimensionalSlashEntity.java
- entity/DimensionalWarpRiftEntity.java
- client/render/DimensionalSlashRenderer.java
- client/render/DimensionalWarpRiftRenderer.java

## Before and after numbers

### Left click: Dimensional Slash

| Value | Before | After |
| --- | --- | --- |
| Damage | 750 flat, no stat scaling (the only projectile in the set with none) | 750 + Strength x 3 |
| Cooldown | 8 ticks | 16 ticks |
| Stamina drain | 100 percent per minute discrete over 13 ticks | unchanged |
| Projectile speed | 2.5 blocks per tick | unchanged |
| Max flight life | 100 ticks (about 250 blocks) | unchanged |
| On hit | entity discards the same tick, no burst, no fade | impact burst, 10 debris shards, 14 tick lingering tear fade |
| On block hit | discards silently | also scars the struck block (crying obsidian, restores after 140 ticks) |
| Melting on hit | 300 ticks, amplifier 1 | unchanged |
| Friction / air drag on hit | -0.90 / -0.80 modifiers | unchanged |

### Right click: Dimensional Warp

| Value | Before | After |
| --- | --- | --- |
| Ki cost | 100 percent per minute discrete over 100 ticks (about 8.33 percent of max Ki), charged BEFORE the raycast | same amount, charged only AFTER the warp target validates |
| Blocked or invalid target | full Ki drain and 100 tick cooldown paid anyway | warp refused: no charge, no cooldown, no rifts |
| Cooldown | 100 ticks | unchanged |
| Max range | 1024 block raycast, 128 block sky fallback | unchanged |
| Teleport timing | instant on click | NEW: fires at the origin rift burst tick, 10 ticks after the cast (build-up) |
| Origin rift damage | 250 | 400 + Strength x 2 |
| Origin rift radius | 3.5 | 3.0 |
| Origin rift life | 24 ticks | 34 ticks |
| Origin damage timing | first touch, from tick 1 | single burst at tick 10 |
| Origin force | 0.35 pull, once, only on first touch | VortexPhysics pull 0.32 per tick scaled by build-up, then 0.10 aftershock swirl; weight classes apply |
| Destination rift damage | 750 | 900 + Strength x 3 |
| Destination rift radius | 3.5 | 4.5 |
| Destination rift life | 24 ticks | 46 ticks |
| Destination damage timing | first touch, from tick 1 | single burst at tick 12 |
| Destination force | 0.5 push, once, only on first touch | build-up pull 0.20 per tick, then fling (power 0.95, up 0.5) at the burst |
| Melting on rift targets | 200 ticks, amplifier 1 | unchanged |
| Self effects | RIFTED 60 ticks and DARK_FADED 35 ticks on the warping player | RIFTED 60 ticks at the crossing tick; DARK_FADED on self removed |
| Destination debris | none | 24 ground-sampled pieces |
| Origin debris | none | 12 void palette pieces |
| Ground scar | none | origin disk radius 1 (200 ticks), destination disk radius 2 (240 ticks) |

Melee (750 base damage, attack speed -2.4, Melting 200 ticks amplifier 1,
stamina drain) is untouched.

## What was added, per point

Point 1, layered depth. The slash trail is now three layers emitted every
tick: a KI_MOTE core on the flight line, theme dust (LayeredParticles.dust,
0xCC26D9) as the mid layer hugging the crescent, and VOID_SMOKE wisps
shedding at the crescent tips; it replaces the flat two-type puff every 2
ticks. The warp rifts emit LayeredParticles.emitVortex for their whole life
(core, mid, outer scaled by the envelope). Renderers: the slash fade is now
FxEnvelope over the real flight life with distance-scaled segment counts;
the lingering tear draws LayeredRender.drawVortexBands; the rift renderer
adds LayeredRender.drawVortexBands and drawAnchorGlow around the tear.

Point 2, debris. Slash impact throws 10 DebrisEntity shards in a void
palette (obsidian, crying obsidian, amethyst block, three sizes, helical,
raining down). The origin burst throws 12 of the same. The destination burst
throws 24 pieces sampled from the actual ground it tears open, the
finisher-class count, matching the warp's place as this weapon's big move.

Point 3, world interaction. Both rifts now pull with VortexPhysics during
build-up (bosses barely move, players at 0.6), and the destination flings
its victims out at the burst. GroundScar: the slash scars the exact block it
strikes (140 ticks), the origin scars a radius 1 disk (200 ticks), the
destination a radius 2 disk (240 ticks), each following terrain column by
column so nothing floats, protected blocks refused inside GroundScar.
LayeredParticles.emitGroundBurst fires at both warp ends and at slash impact.

Point 4, atmosphere. The Dimensional Sword has no weather theme and gained
none, per the standard. Its atmosphere is light and particles: layered rift
columns, the anchor glow, and DbaImpactFx.medium camera weight, which used
to fire at cast time at an empty destination and now fires at the actual
arrival burst.

Point 5, lifecycle. Slash: the envelope runs over the real 100 tick life
(the old 70 to 100 fade window is now exactly the envelope decay), and a hit
no longer ends the entity: a synced FADE_START starts a 14 tick linger where
the crescent swells slightly, swirls in vortex bands, sheds an emitRain
spark drizzle, and fades shut. Rifts: build-up (envelope build 20 percent
plus the vortex drag), burst at the peak tick (origin 10 of 34, destination
12 of 46), dissipation over the envelope's last 35 percent with a fading
aftershock swirl and spark rain. No damage lands on tick 1 anywhere anymore.

## Bugs fixed

- Ki was charged before the raycast, so blocked warps and blind sky misclicks
  still cost the full drain and cooldown. The raycast and a landing
  validation (feet and head cells free, climbing up to 4 cells) now run
  first; failure costs nothing. The climb also fixes landings into buried
  face-adjacent cells (inside corners) that could suffocate the player.
- The warping player received DARK_FADED 35 ticks from their own ability.
  Removed. RIFTED 60 stays, applied at the crossing tick.
- DimensionalSlashRenderer's fade out window (ages 70 to 100) was
  unreachable on any hit because the entity discarded first. Entity life and
  renderer are reconciled: one shared envelope for flight, a synced linger
  fade for hits.
- Rift forces were inside the damagedTargets first-touch check, so the pull
  and push fired exactly once per target. Forces now run every tick through
  VortexPhysics; damage is one burst per rift.
- DimensionalWarpRiftRenderer's class doc promised fissure trenches, void
  slabs, gyroscopic rings, orbiting shards, and a singularity octahedron;
  the helper methods existed but were never called, so none of it rendered.
  All are now drawn, seeded per rift so tears differ without flickering.
- The 250 / 750 rift damage asymmetry was invisible because both rifts were
  radius 3.5 and rendered the same size. Radii are now 3.0 and 4.5 and the
  renderer scales from radius.

## New mechanics (reported as additions)

- Warp build-up: the teleport fires at the origin rift's burst tick instead
  of instantly, and the destination rift opens at the crossing moment. The
  10 tick delay is filled with the pull-in, and the arrival burst
  (damage, fling, debris, scar, camera weight) lands 12 ticks after that.
- Slash lingering tear and impact scarring, as above.

## Shared change requests

None. Everything was built from the existing shared blocks (FxEnvelope,
LayeredParticles, LayeredRender, DebrisSpawner, VortexPhysics, GroundScar).

## Verification

Targeted javac audit (JDK 25), the five files compiled with -sourcepath
against the 26.2 and 26.3 classpaths, pristine HEAD worktree versus the
current tree: zero error kinds located in the five weapon files on either
version, and zero new kinds versus baseline. One real error was caught and
fixed during verification (a lambda capturing the reassigned fade variable
in DimensionalSlashRenderer; it now captures a final snapshot). Full-tree
sweeps were attempted first but are not attributable here: the sandbox
restarted mid-run and the other Phase 2 agents are editing the shared tree
concurrently, so the targeted method is the honest isolation for this
weapon. The Gradle build remains waived per standing direction; the final
look judgment is Carter's on his PC.

## Worse check

- The warp now takes 0.5 seconds to fire instead of being instant. This is
  the build-up the task required, and the pull-in makes the delay do work,
  but it is a real feel change and is flagged as such.
- Slash sustained output at zero Strength is halved by the cooldown change
  (8 to 16 ticks). Deliberate: the audit named the spam rate as what capped
  the visual weight, and per-hit damage now scales with Strength.
- Rift damage is single-moment: a target that walks into a rift after its
  burst tick takes no damage from it, where before first touch at any time
  in the 24 ticks hurt. Accepted per standard point 5; the build-up vortex
  exists to hold targets for the burst.
- Nothing was reverted; no part of the pass read as a regression beyond the
  three deliberate trade-offs above.
