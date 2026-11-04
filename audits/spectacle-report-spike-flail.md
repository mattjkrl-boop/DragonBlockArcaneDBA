# Spectacle Report: Spike Flail (Phase 2)

Agent scope: item/SpikeFlailItem.java, entity/SpikeFlailSpinEntity.java,
entity/SpikeFlailSwingEntity.java, client/render/SpikeFlailSpinRenderer.java.
SpikeFlailSwingRenderer.java was read but not edited (see Point 1 note).
Shared blocks were only called, never edited. Base: commit 5e4b1de.
Theme kept: steel chain and spiked ball, warm white crack. No color or
identity changes anywhere.

## Before and after numbers

| Value | Before | After |
| --- | --- | --- |
| Melee attribute damage | 819 + 1 = 820 | unchanged |
| Melee attack speed | -2.7 | unchanged |
| Swing (left click) damage | 720 + Strength x 2.0 | unchanged |
| Swing cooldown | 8 ticks | unchanged |
| Swing stamina | drainStaminaDiscrete 100%/min over 20 ticks | unchanged |
| Swing entity life | 11 ticks | unchanged |
| Orbit contact damage | 160 + Strength x 0.35 | 200 + Strength x 0.5 |
| Orbit contact check / re-hit gap | every 4 ticks / 8 ticks per target | unchanged |
| Orbit contact push | 0.35 horizontal, 0.12 up | unchanged |
| Channel length | CHANNEL_TICKS 32 | unchanged |
| Channel stamina | 150%/min per tick | unchanged |
| Release (item) cooldown | 36 ticks | unchanged |
| Crack damage | 780 + Strength x 2.0, flat, ignores channel time | (480 + 600 x charge) + Strength x (1.0 + 1.0 x charge): tap 480 + 1.0S, full channel 1080 + 2.0S |
| Crack radius | 5.4 flat | 2.7 + 2.7 x charge: 2.7 tap, 5.4 full channel |
| Crack timing | fires on release tick 1 at full radius | fires on release tick 7, the tick the expanding visual disc reaches the crack radius |
| Crack knockback | radial speed 0.85, up 0.38 | radial 0.55 + 0.45 x charge (0.55 to 1.0), up 0.30 + 0.16 x charge (0.30 to 0.46) |
| Release visual life | discards at release age over 8; disc grows 0.48/tick to about 6.4 (overshoots the 5.4 damage radius) | 7 expansion ticks with smoothstep growth ending exactly at the crack radius, plus 4 settle ticks; discards at age over 11 |
| Debris pieces on crack | 0 | 24 + 8 x charge (24 tap, 32 full), ground-sampled palette, 3 sizes from DebrisSpawner |
| Ground scar on crack | none | per-column scar disk at the real crack radius, gravel, chance 1.0 inside 1.5 blocks and 0.6 beyond, restores after 100 ticks |
| Particles, all four files | 0 | see Points 1, 3, 5 |
| Ki costs | none (this weapon runs on stamina, not Ki) | none; no Ki or stamina value changed |

Charge is the channel fraction: charge = min(1, held ticks / 32), captured
server side by the spin entity at the release tick and synced (new CHARGE
synced field) so the renderer scales with the same number the damage uses.
Full channel pays 2.25x the tap crack damage and 2x the tap radius, which
meets the standard's charge guidance.

## What was added, per point

Point 1, layered depth:
- Spin channel: LayeredParticles.emitVortex every orbit tick (dense KI_MOTE
  core, warm steel dust mid band, WIND outer wisps), counts scaled by
  FxEnvelope over the 32 tick channel, so the spin's presence builds with
  the channel instead of starting flat out.
- Spin renderer: LayeredRender.drawVortexBands at the orbit radius (mid band
  at 0.70 x radius, wispy outer band at about 1.0 x radius, riding the ball's
  path) plus LayeredRender.drawAnchorGlow under the spin, both scaled by the
  same FxEnvelope the particles use, with LayeredRender distance scaling.
- Swing: SPARK and warm dust trail off the ball every tick, counts scaled by
  FxEnvelope over the 11 tick swing and by effectDensity. The swing renderer
  already had an open ease and a fade, so it was left as is.
- Crack disc: alpha now scales with charge (0.55 + 0.45 x charge) so a tap
  crack reads thinner than a full channel crack, and an anchor glow sits
  under the disc.

Point 2, debris:
- On the crack impact tick, DebrisSpawner.spawnField throws 24 to 32 pieces
  sampled from the actual ground under the player, orbiting helically out to
  the crack radius (rise 2.4, 16 orbit ticks plus per-piece jitter), then
  raining back down inside DebrisEntity. Meets the 24 piece finisher floor
  at any charge.

Point 3, world interaction:
- Base dust burst (LayeredParticles.emitGroundBurst) at the crack center on
  the impact tick, radius 0.75 x crack radius.
- GroundScar tears the surface inside the real crack radius into gravel:
  per-column floor search near the player's level (isFaceSturdy), full
  coverage inside 1.5 blocks, 0.6 chance beyond, restore after 100 ticks,
  protected blocks refused inside GroundScar itself.
- Dust kicked at the ball's low point: emitGroundBurst at the ground cell
  under the orbiting ball every 4 ticks.
- Dust rides the expanding disc edge during the 7 release ticks, and an
  ANVIL_LAND thud plays on the impact tick.
- Mob forces were already real (orbit contact push, radial crack knockback);
  the crack knockback now scales with charge as tabled above.

Point 4, atmosphere:
- No weather added: the flail has no weather theme, and the standard says
  weapons without one do not gain random weather. Its atmosphere is the
  layered dust, sparks, ground glow, and the impact thud.

Point 5, lifecycle:
- Channel: particles and renderer bands build over the first fifth of the
  32 tick channel via the shared envelope, peak, and settle into the release.
- Release: 7 ticks of smoothstep disc expansion (the telegraph), damage,
  scar, debris, and burst all landing together on tick 7 when the disc
  reaches the damage radius, then 4 settle ticks where the renderer fades
  out over FxEnvelope.smoothstep, LayeredParticles.emitRain drizzles theme
  dust, and the debris falls and lands. Nothing pops in at full alpha and
  nothing discards at visible alpha: the old code discarded at release age
  9 while its fade was still mid-curve and its disc had overshot the hit.

## Bugs fixed

1. Crack and channel disconnected (fresh audit): nothing passed the held
   tick count to the release, so 1 tick and 32 ticks paid the same 780.
   Charge now scales damage, radius, knockback, debris count, and scar size.
2. Damage and visual mismatch (fresh audit): damage fired at radius 5.4 on
   release tick 1 while the visual disc was still at 2.55, and the disc then
   grew past the damage radius to about 6.4. Now the disc ends exactly at
   the damage radius and the hit lands on the tick it gets there.
3. Stale spin entity blocks new spins (fresh audit edge case): beginSpin's
   duplicate check counted entities that were already releasing. It now
   ignores releasing entities, so a leftover release visual near the player
   cannot block the next spin.
4. Found during this pass, in my own first draft: the scar floor search used
   BlockState.blocksMotion(), which was removed in 26.3 (the 26.3 javac
   sweep caught it). Replaced with isFaceSturdy(level, pos, Direction.UP),
   present on both 26.2 and 26.3. Observation for the coordinator: the same
   removed call exists at entity/LavaSwordGround.java:212 and shows as a
   26.3 baseline error kind there; that file belongs to the Lava Sword
   agent, so I did not touch it.

## Shared change requests

None. Every shared block I needed already existed with the right signature
(FxEnvelope, LayeredParticles, GroundScar, DebrisSpawner, LayeredRender).

## Worse check (honest)

- A tap release is deliberately weaker than the old flat crack (480 + 1.0S
  at radius 2.7, versus 780 + 2.0S at 5.4). That is the standard's rule that
  charge must pay, not a regression: a full channel now beats the old crack
  on damage (1080 + 2.0S) at the same 5.4 radius, with debris and scarring
  the old crack never had.
- The release disc's max visual radius dropped from about 6.4 to 5.4. The
  old 6.4 was overshoot past its own damage radius; 5.4 is the honest edge.
- The settle drizzle is thin (about one dust particle per tick) because
  LayeredParticles.emitRain inverts the envelope; the dissipation read is
  carried mainly by the falling debris and the renderer fade. Noted, not
  hidden.
- Nothing was reverted. No part of the rebuild reads worse than before:
  every before state was zero particles, zero debris, zero scar, and a
  damage timing that lied about its radius.

## Verification

Javac audit sweeps (the project's standing method; Gradle cannot run in
this sandbox): full tree compiled against the 26.2 and 26.3 audit
classpaths after the final edits. Zero error kinds in all four Spike Flail
files on both versions. Tree-wide kind counts during this pass (135 on
26.2, 146 on 26.3 at last sweep) move as sibling agents edit concurrently,
so the coordinator's baseline diff at merge is the authoritative check.
The first 26.3 sweep of my draft did catch the blocksMotion error above;
it is fixed and the re-sweep is clean. No commit made, per instructions.
