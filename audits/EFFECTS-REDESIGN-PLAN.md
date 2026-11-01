# Effects Redesign Plan (Nov 1, 2026)

Carter's direction, verbatim: "look at how the effects look before changing
them so they look similar but 100 times better."

So this pass is a redesign, not a rebuild and not a polish. Every weapon
ability effect keeps its identity and gets dramatically better execution.

## The identity lock (hard rule)

For every effect, before editing, read its entry in the group visual audit
(entity-visual-audit-group-a.md through group-d.md) and its full renderer
source. The audit is the record of how the effect looks now. The redesign
must preserve:

- The dominant palette (same color family per weapon, listed per group
  below).
- The signature shapes (the storm is still a dome with an eye column, the
  tornado is still a funnel with ribbons, the fissure is still a ground
  trench, the afterimage is still a ghost body).
- The lifetime, role, and trigger. No damage, behavior, cooldown, or
  balance changes anywhere in this pass.

A player who knows the current mod should recognize every effect instantly
and just see it done far better.

## What "100 times better" means here (the design system)

Apply these techniques per effect where they fit. Signature, large, and
long lived effects get the full treatment; small 8 to 16 tick bursts get a
light touch (T1, T2, T4). Do not apply every technique mechanically to
every effect.

- T1 Additive glow halo. No renderer in scope uses
  KiRenderHelper.kiAdditiveType() (RenderTypes.eyes) today; everything is
  normal blending, which is why effects read flat and matte. Add an
  additive pass of the effect's signature shape at slightly larger scale
  and low alpha (roughly 0.10 to 0.30) so cores bloom and edges glow.
  At most one extra submit per effect part group.
- T2 Three-stop vertex gradients. Where a part is one flat color today,
  give it a depth ramp: near-white hot tint at the core or leading edge,
  the main color mid, a deeper shade of the same hue at the base or trail.
  Flat single-color shells (Erasure orb shells, Azure funnel layers,
  Trident shard body) are the top targets.
- T3 Tessellation step-up. Raise segment counts about 1.5x on rings,
  discs, domes, and funnels (16 to 24 or 32, dome grids one step denser)
  so silhouettes are smooth curves instead of visible polygons.
- T4 Eased motion and secondary motion. Replace linear ramps with eased
  curves where a ramp reads mechanical, and add life to effects that sit
  static while alive: counter-rotating layers, breathing pulse, slow roll,
  orbiting accents. If an effect already moves richly, leave its motion
  alone.
- T5 Theme particles. Most entities emit no particles of their own. Add
  tasteful emission from the entity tick (server side, ServerLevel
  sendParticles, scaled by DbaEffectsConfig effect density exactly like
  ZShockwaveEntity already does) using the weapon's theme particle:
  SPARK, EMBER, VOID_SMOKE, WIND, or KI_MOTE. Signature and long lived
  effects first; do not spam tiny bursts.
- T6 Ground anchor glow. Standing and floating effects that hover over the
  world get a soft additive ground disc or ring in their color so they sit
  in the scene instead of floating over it.
- T7 Energy flow. Beams, waves, and bolts get traveling brightness bands
  (phase shifted sine along their length) so energy visibly flows instead
  of sitting uniform. The Erasure beam's uniform 48 block tube is the top
  target.

## Preserve the previous pass

The working tree already contains the entity and effects visual pass
(ENTITY-VISUALS-AFTER.md, commit 30e1afd): fade envelopes, the broken fade
formula fixes, the DelayedLaunch telegraph, billboarded Time Shatter dial,
crossed plane bursts, oriented fissures and Bansho bursts, Void Rift
implosion phasing, and the particle additions it made. All of that stays.
Build on it; do not regress any fade, lifetime, or orientation fix.

## Per-group identity locks

- Group A. Katana: electric cyan and silver-white crescents, reticle beam.
  Z Sword: gold and violet, mandala charge, gravity trenches and
  monoliths. Saber: cyan with gold clash sparks, void tear slit. Brave
  Sword: gold and cyan valor, cross slash X, valor prisms. Grand Sword:
  gold and white-gold crescents, steel blue blade shards, valor dome.
- Group B. Azure: cyan and teal storm, funnel tornado, textured lightning
  (the azure_lightning textures stay). Dabura: violet and crimson
  darkness, falling greatswords, shatter. Curse: red and violet, textured
  lightning and sky cracks (textures stay), chains and domains. Hollow's
  Edge: violet and cyan void, ghost afterimage, accretion disk rift.
  Dimensional: violet and magenta, tear slit warp rifts.
- Group C. Spirit Sword: cyan and gold, impale swords, cannon beam. Evil
  Spear: crimson and blood red, spear projectile, hell hunt ground
  impacts. Devil Trident: dark crimson, shard swarm, deploy slam. Sickle:
  void black, magenta, cyan edge, crescents and dimensional rift. Ox:
  molten orange and dark basalt, fissures, crags, stance avatar, king
  slam (Flawless turns pale gold, keep that switch).
- Group D. Power Pole: crimson and gold pole, emerald and cyan
  whirlwind. Whis: cyan, silver, and lilac time, astrolabe and clock
  hands. Bansho: emerald wind, cyclone funnel, drill projectile. Blaster:
  heat-reactive bolts (yellow-white to orange to magenta-red, keep the
  heat palette logic) and cyan erasure orb and beam.

## Verification

No Gradle build (waived by Carter). After all groups land, dual javac
audit sweeps on the 26.2 and 26.3 classpaths must show zero new error
kinds against the pre-pass baseline, and every changed file gets a diff
review against this plan and its audit entry.
