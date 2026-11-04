# No-Shapes Report: Spike Flail

Owner scope: client/render/SpikeFlailSpinRenderer.java and
client/render/SpikeFlailSwingRenderer.java, exactly the two files assigned
to Spike Flail in audits/no-shapes-audit.md. No item, entity, or shared
file was edited. Theme kept: steel chain and spiked ball, warm white crack,
warm steel dust 0xD8C9A3. Every damage number, cooldown, stamina value,
lifetime, mechanic, and synced data field is unchanged, because the entity
and item files were not touched and the renderers only changed how the
same synced state is presented.

## Before and after geometry counts

Counts are the audit's own rows for this owner (B is BespokeGeometry
builder calls, L is LayeredRender geometry calls, V is direct vertex
writing):

| Effect | B before | B after | L before | L after | V before | V after |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| SpikeFlailSpin | 5 | 0 | 2 | 0 | 0 | 0 |
| SpikeFlailSwing | 6 | 0 | 0 | 0 | 0 | 0 |
| Owner total | 11 | 0 | 2 | 0 | 0 | 0 |

The shared submitSpan, submitChain, and submitBall helpers that lived in
SpikeFlailSwingRenderer are deleted, so the Spin renderer's indirect use
of them is gone as well. Both renderers now submit no effect geometry at
all: submit only drives MaterialEmitters emission from the render state,
then calls super.submit. Post-edit grep over both files finds no
BespokeGeometry reference, no LayeredRender reference, no KiRenderHelper
reference, no builder call, and no direct vertex call.

## Materials per effect

All emission goes through client/fx/MaterialEmitters: per-entity Emitter
sets with frame deltas derived from the render age, FxEnvelope distance
scaling through MaterialEmitters.distanceScale, density scaling inside
the toolkit, and hard per-second budgets on every stream.

### SpikeFlailSpin, orbit phase

- Air vapor (Preset.vapor, 0xEDE8DA, scale 1.50, rate 130/s, budget 190)
  sampled along the ball's real movement path from the previous tick
  position to the current position.
- Steel sparks (Preset.fixed on DbaParticles.SPARK, rate 58/s, budget 90)
  along the same path.
- Air vapor (0xDAD4C6, scale 1.00, rate 48/s, budget 80) along the chain
  path from the holder-side anchor to the ball.
- Ground dust (Preset.dust, theme 0xD8C9A3, scale 1.15, rate 38/s,
  budget 65) across a 0.65 radius ground area under the ball.
- Envelope is the same FxEnvelope.envelope(age, CHANNEL_TICKS) the old
  bands used, times distance scale.

### SpikeFlailSpin, release and crack

- While the crack expands (releaseAge below RELEASE_TICKS), the
  expanding edge carries ground dust (scale 1.50, rate 160/s, budget
  230), air vapor (0xF5EFE0, scale 2.00, rate 58/s, budget 95), and warm
  plasma (Preset.plasma, 0xFFF2CC, scale 0.80, rate 72/s, budget 115),
  sampled on the synced expanding radius with outward velocity. The
  envelope keeps the old crack alpha shape: fade times (0.55 + 0.45 x
  charge), times distance scale.
- On the exact tick the damage lands (releaseAge equals RELEASE_TICKS,
  guarded to fire once per release), a one-time burst fires: ground dust
  (34 + 26 x charge), vapor in a sphere volume over the center
  (16 + 10 x charge), steel sparks (18 + 12 x charge), and physical
  debris through Preset.fixed on DbaParticles.BLOCK_CHUNKS carrying the
  actual ground block state at the center, falling back to gravel when
  the sampled cell is air, which matches the gravel scar the entity
  leaves (14 + 10 x charge). All counts are distance scaled.
- During the settle window, ground dust (scale 1.05, rate 38/s, budget
  65) settles across the crack disc under the same settle fade the old
  crack marks used.

### SpikeFlailSwing

- Air vapor (0xEDE8DA, scale 1.50, rate 150/s, budget 220) sampled along
  the real arc: the same five ballPos samples at 0.09 progress steps the
  old trail used, kept as a polyline emission source, never drawn.
- Steel sparks (DbaParticles.SPARK, rate 65/s, budget 100) along the same
  arc path.
- Warm plasma glints (Preset.plasma, 0xFFF6D8, scale 0.65, rate 70/s,
  budget 110) at the moving ball position.
- Air vapor (0xDAD4C6, scale 1.05, rate 55/s, budget 90) along the
  hand-to-ball chain path.
- Ground dust (theme 0xD8C9A3, scale 1.10, rate 30/s, budget 55) under
  the ball, at full envelope only while the arc is low (ball within 1.25
  of the holder's ground level) and at 30 percent otherwise.
- The old fade and open easing are preserved exactly: open eases over
  the first 2 ticks of age, fade holds until age 8 and eases out by
  age 11.

## Physical object note

The Spike Flail item's own GeckoLib model, rendered in the holder's hand
by the weapon system, is untouched and remains the physical modeled
object. The entity renderers no longer fabricate a second chain and head
out of effect geometry; the deployed swing and orbit presentation is the
material emission above, layered over the entity's existing server-sent
wind, dust, spark, debris, and ground scar effects, which were not
changed.

## Verification

Targeted javac audit only (the project's standing method; no Gradle
build was run and none is claimed). Both renderer files were compiled as
roots with the full sourcepath on both audit classpaths:

- cp262: zero error lines naming any Spike Flail file; both renderer
  classes and their inner classes were emitted. The only errors in the
  run are the 4 known baseline lines in DbaItems and DbaMenus.
- cp263: zero error lines naming any Spike Flail file; both renderer
  classes and their inner classes were emitted. The only errors in the
  run are the 10 known baseline lines in DbaItems, DbaMenus, and the
  hostile mob entities.
- git diff --check on the two renderer files: clean.
- Em dash scan of the two renderer files and this report: zero.

## Change requests

None blocking. One note for the coordinating pass: steel sparks ride the
existing DbaParticles.SPARK type through Preset.fixed, because the
DbaMaterialOptions family has no dedicated spark material and SPARK is
the established steel spark type used by the entity's own server-side
impact effects. If the merge wants every spark on a material preset, a
Preset.spark factory or a spark material in the foundation is the one
central change that would do it.

## Worse check (honest)

- The deployed chain and head are no longer a solid body in the entity
  renderer. The held weapon model remains, but the orbiting and swinging
  head at range now reads from dense vapor, sparks, and plasma glints
  instead of a continuous iron ball and chain. The chain is the weakest
  part: vapor along its path only suggests it. This follows the audit's
  binding material assignment for this owner (motion-driven vapor and
  plasma, dust at contacts), and I did not fake a body with geometry,
  but if it reads worse in game the honest remedy is a real modeled head
  and chain asset for the entity, which is outside this owner's two-file
  scope. Flagged, not hidden.
- The crack telegraph lost its glowing crack lines. The expanding radius
  is still honest, because the dust, vapor, and plasma ride the synced
  expanding edge and the damage still lands exactly on release tick 7,
  but the radius is carried by moving material rather than drawn lines,
  so it is less explicit at a glance.
- Client emission stacks on top of the entity's existing server-sent
  wind, dust, spark, debris, and scar effects. At close range the orbit
  may read busy. Distance scaling, the toolkit density config, and the
  per-stream budgets are the mitigations already in place; the server
  effects themselves were out of scope and unchanged.
- Nothing was reverted. No mechanic, number, timing, or synced field
  changed anywhere in this conversion.
