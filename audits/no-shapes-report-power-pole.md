# No-Shapes Report: Power Pole

Phase 2 conversion of the Power Pole to audits/NO-SHAPES-STANDARD.md, on top
of the Phase 1 foundation at ad22b32. Files owned and edited, exactly the
three rows assigned in audits/no-shapes-audit.md:

- client/render/PowerPoleExtensionRenderer.java
- client/render/PowerPoleImpactRenderer.java
- client/render/PowerPoleWhirlwindRenderer.java

No shared file was edited. The item, the three entities, and every server
side system (damage, vortex forces, debris, sounds, particles the entities
already emit) are untouched. No damage number, cooldown, Ki cost, timing,
duration, hitbox, mechanic, or synced data field changed anywhere. The only
render state addition is a client side entity id used to key each entity's
emitter rig; the material seeds derive from the same synced state hash the
bespoke renderer used (caster id, max length, range, cone angle, impact
scale, orientation).

## Before and after geometry counts

Counts are the audit rows (B is BespokeGeometry builder calls, L is
LayeredRender geometry calls, V is direct vertex drawing):

| Effect | B before | B after | L before | L after | V before | V after |
| --- | --- | --- | --- | --- | --- | --- |
| PowerPoleExtension | 11 | 0 | 0 | 0 | 0 | 0 |
| PowerPoleImpact | 8 | 0 | 0 | 0 | 0 | 0 |
| PowerPoleWhirlwind | 7 | 0 | 2 | 0 | 0 | 0 |
| Owner total | 26 | 0 | 2 | 0 | 0 | 0 |

Grep verification on the finished files: zero references to BespokeGeometry,
zero to LayeredRender (including the distScale utility; distance scaling now
comes from MaterialEmitters.distanceScale), zero to KiRenderHelper, zero
addVertex calls, and zero submitCustomGeometry calls in all three renderers.
All three submit methods now only feed MaterialEmitters and return.

## How the conversion works

Each renderer keeps a small per entity emission rig: a mutable frame (origin,
direction, and the effect's own dimensions) that the toolkit sources read at
sample time, plus MaterialEmitters.Emitter instances built on those sources.
The Emitter accumulator is driven by the render state's age delta, so
emission is frame rate independent; every tick call passes the lifecycle
envelope multiplied by the MaterialEmitters distance scale, and every
emitter carries a hard per second budget from the toolkit.

### PowerPoleExtension

The extending strike is now a rushing stream sampled along the pole path
with MaterialEmitters path segment sources:

- Vapor in the pale gold sheath color (0xF7D774, scale 1.45) along the whole
  current length: the displaced air the rush drags with it.
- Plasma in three heat bands matching the old shaft gradient (hot 0xFF3829
  at the hand, mid 0xD91414, deep 0x94141C at the tip) plus a thinner bright
  core stream (0xFF5940, scale 0.45).
- Dust (0xB09A72) and the existing SPARK type as steel sparks at the leading
  tip, emitted from the tip point with forward velocity.

The lifecycle is byte for byte the old one: ease out over 3 ticks, hold,
retract over the last 3 ticks with the envelope reaching zero at the tick
12 discard. The first person hand offset (side by handedness, lower, and
slightly forward, with the 0.55 first person scale applied to the tip
emission) is preserved in world space. The gold caps, collars, dragon
coils, dome tip, Mach plumes, wake ribbons, and tip glint are gone; they
were geometry ornaments and nothing in this renderer draws a replacement
silhouette. The pole item model in the hand stays a physical object,
untouched, as the standard requires.

### PowerPoleImpact

The impact is a short material event at the strike point:

- One opening burst on the first rendered frame: gold plasma and crimson
  plasma from a sphere volume at the focal point, pale vapor, impact dust
  (0xA78B5F) across the strike plane, and SPARK steel sparks.
- A brief continuous wash for the remaining life: vapor, dust, plasma, and
  sparks sampled in the strike plane, with the sample radius following the
  same ease out expansion over the first 8 ticks the old rings and dome
  used, and outward radial velocity rising with the same shock progress.
- The fade is the old smoothstep of age over the entity lifetime, applied
  as the emitter envelope.

The compression dome, the shock bands, the starburst fans, and the 16
flying shard fans are gone. Nothing physical was invented to replace the
shards; the audit theme for this row is air vapor, impact dust, and steel
sparks, and the burst is carried by exactly those.

### PowerPoleWhirlwind

The funnel is a sampled volume, not a drawn cone. A cone source samples
points inside the cone the entity already defines (position along the
axis, radius from the cone angle and the swell, angle advanced by the
same spin rate the old bands used), and returns a sample velocity with a
forward component, a tangential swirl component, and a small outward
component, so the material itself rotates:

- Vapor core in cyan-white (0xEFFFFF) at 0.62 of the cone radius.
- Vapor body in the whirlwind emerald (0x26FFA6, the entity's own theme
  color) at 0.95 of the cone radius.
- Plasma streaks (0x7DFFE8) at 0.84 of the radius with the fastest forward
  and swirl speeds, replacing the helical ribbons and the crescent
  cutters' motion read.
- Dust (0x9A8F7A) sampled around the caster's ground line under the near
  half of the cone, for the air meeting the ground.
- SPARK at a low rate in the outer flow, for contacts shed by the spin.

The alpha envelope formula is the same code path the bespoke renderer
used (build over BUILD_TICKS, decay from the synced DECAY_START over
DECAY_TICKS at the height the build had reached), and the swell factor
(0.55 plus 0.45 of the envelope) still drives the sampled radius. The
server side vortex forces, damage window, debris field, and gust sounds
are in the entity, which was not edited.

Peak emission at density 1 and near range is about 680 particles per
second across the five whirlwind emitters, about 765 per second for the
extension's seven emitters during its 12 tick life, and about 420 per
second plus the opening bursts for the impact. All rates pass through
the effects density config and the distance scale, and every emitter is
hard budgeted, so far or low density views step down instead of costing
full rate.

## Verification

Targeted javac audit only. No Gradle build was run and no in game look is
claimed.

- Full sourcepath targeted compile of the three renderers on the 26.2
  classpath (cp262.txt) completed: 4 errors, all pre-existing baseline
  kinds in files this pass did not touch (item/DbaItems.java CreativeModeTab
  access x2, inventory/DbaMenus.java MenuType access x2). Zero errors in
  any Power Pole file, and all three renderer classes plus their inner
  classes were emitted.
- On the 26.3 classpath (cp263.txt), the same full sourcepath closure run
  exceeded the sandbox time limit under heavy parallel load after emitting
  all three renderer classes and showing only the same baseline DbaItems
  errors; no error named a Power Pole file. A dependency resolved targeted
  compile of exactly the three renderers against the 26.3 classpath (mod
  dependencies taken from the compiled closure, Minecraft and Fabric from
  cp263.txt) then completed with exit 0 and zero errors, all classes
  emitted. The same dependency resolved compile on the 26.2 classpath
  also completed with exit 0 and zero errors.
- Em dash scan of the three edited files and this report: zero.

## Honest worse check

- The extension no longer reads as a solid pole flying out. It reads as a
  fast stream of vapor and plasma along the exact path, which is what the
  standard asks for, but the weapon's reach is less legible at long
  distance or low density settings than the old shaft was, and the gold
  fittings and dragon coil identity details are simply gone. If Carter
  wants those back, they have to return as a detailed physical model of
  the extending pole, not as renderer geometry.
- The whirlwind's orbiting crescent cutters are gone. The spin now reads
  through density, color layering, and real tangential particle motion.
  That is more realistic and less readable as ten separate blades; a
  cutter would likewise need to be a physical object.
- The impact loses its shard silhouettes and its dome. The burst is
  brighter at the center and dustier at the plane, but it has no hard
  focal star. That trade is inherent to the no-shapes rule for this row.
- Nothing was reverted: every loss above is a geometry read the standard
  explicitly removes, and the material replacements are in place for all
  three effects. Mechanics, timing, and identity colors are unchanged.

## Shared change requests

None. The Phase 1 toolkit (Preset, Source, burst, Emitter, distanceScale)
covered every conversion in this area. One optional note for the
coordinating pass, not a gap: if Carter asks for the extending pole or
the whirlwind cutters to be visible as objects again, that is a modeling
task (detailed physical models under the standard's physical object
exemption), not a foundation extension.
