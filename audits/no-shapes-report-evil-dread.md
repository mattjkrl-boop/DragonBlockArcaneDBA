# No-Shapes Report: Evil Spear Dread

Owner: Evil Spear Dread system. Tree: dba ad22b32 (Phase 1 foundation).
Files owned: effect/EvilDreadEffect.java, entity/DreadGazeEntity.java,
client/render/DreadGazeRenderer.java. Only the renderer needed edits;
the effect and the anchor entity were inspected and left byte-identical
because both assigned conversions live in the renderer. No commits.
Verification is the javac audit only; no Gradle claim and no in-game
look claim.

## Before and after counts (DreadGazeRenderer)

| Surface | Before | After |
|---|---:|---:|
| BespokeGeometry builder calls (brokenVortexBand shroud) | 2 | 0 |
| BespokeGeometry.Rng references | 1 | 0 |
| KiRenderHelper.vertex calls (eye quads) | 8 | 0 |
| LayeredRender geometry calls | 0 | 0 |
| LayeredRender.distScale references (utility, not geometry) | 1 | 0 |
| Direct addVertex calls | 0 | 0 |

Grep proof after the conversion: zero BespokeGeometry, KiRenderHelper,
and LayeredRender references remain in any of the three owned files.
No new private geometry helper, particle system, or texture style was
created. The one local helper added is a splitmix random stream (see
Eyes), which draws nothing.

## Conversion 1: the shroud is material now

Before: DreadGazeRenderer wrapped the victim in two counter-turning
BespokeGeometry.brokenVortexBand layers of near-black crimson smoke
(radii victimWidth / 2 + 0.5 and 1.25x that, heights 0.95 and 0.8 of
victim height, alphas 0.34 and 0.26 of the lifecycle envelope).

After: the bands are gone. The renderer runs two persistent
per-anchor MaterialEmitters on the shared toolkit:

- DARKNESS preset, color 0x0D0017 (the inner band color), scale 2.7,
  base velocity up 0.020, spread 0.010, rate 20 per second, budget 56.
- SMOKE preset, color 0x1A000A (the outer band color), scale 3.1,
  base velocity up 0.014, spread 0.014, rate 11 per second, budget 32,
  ticked at 0.85 of the darkness envelope.

Both sources sample a shell around the victim at the old shroud
radius and height (radius factor 1.0 for darkness, 1.22 for smoke,
heights up to the old shroud top), with a slow tangential drift in
opposite directions per material (plus 0.022 and minus 0.016), which
carries the old counter-turning character as motion instead of mesh.
Emission is keyed by anchor UUID in the renderer, created once per
gaze, updated from the render state each frame, ticked with an
age-derived delta so the rate is frame-rate independent, and pruned
two seconds after an anchor stops rendering. The lifecycle envelope
is unchanged (fade in over the anchor's first 16 ticks, fade out over
the effect's last 25 ticks), distance scaling now comes from
MaterialEmitters.distanceScale, and the shared Emitter applies the
effects density config on top, as the foundation specifies.

The effect's own ambient VOID_SMOKE wisps in EvilDreadEffect are
server-side effect particles and were not part of the assigned shroud
conversion; they are unchanged.

## Conversion 2: the eyes are textured sprites now

Before: each eye was a hand-written quad pair (halo at 1.7x, then the
core) written through KiRenderHelper.vertex with both windings on
RenderTypes.entityTranslucentEmissive.

After: the eyes are submitted as a QuadParticleRenderState group on a
SingleQuadParticle.Layer bound directly to the detailed eye texture
(translucent, using the shared translucent particle pipeline). Each
sprite is added camera-relative with the camera orientation passed
straight through, which is the quad particle system's own billboard
contract, so every pair still stares at whoever looks at it. The
lateral pair offset is applied along the camera right vector derived
from the same orientation, matching the old pose-space offset. Halo
sprite first at 1.7x size with the old halo tint and 0.16 alpha
factor, then the core sprite at full texture alpha factor 0.95, both
at full-bright light, exactly the old pass order and values.

Seeded behavior is preserved in full. The renderer now carries a
local splitmix stream with the same sequence, the same seed
(victim UUID most significant bits, xor 0xE7E50), and the same
fixed consumption order per pair as the deprecated toolkit stream it
replaces, so angles, radii (2.3 to 4.6), heights (0.35 to 2.6), pair
widths (0.55 to 0.9), appear delays, blink periods (53 to 120 ticks)
and phases, drift directions, and pulse phases land on the same
values as before. Pair count scaling by density (minimum 3, none at
density zero), the 12 tick staggered fade-in per pair, the slow
angular drift, the bob, and the 0.8 to 1.0 glow pulse are unchanged.

## What did not change

- EvilDreadEffect.java and DreadGazeEntity.java: no edits. Durations
  still ride the spear's mark sites (melee 120, lunge 120, projectile
  pierce 160 ticks, in the spear owner's files, untouched), the
  heartbeat and onset sounds are the same bank events, and the anchor
  spawn, follow, dedup, and ten tick self-discard behavior is
  identical.
- The MeltingOverlayMixin vignette is a screen effect, not world
  geometry, and was left exactly as it is, per the assignment.
- No Evil Spear item, projectile, damage, mark, multiplier, hunt, or
  cooldown code was touched; those files belong to the weapon owner.

## Verification

- Targeted javac of the three owned files as roots with the full
  sourcepath closure (src/main/java plus src/client/java), JDK 25,
  on both audit classpaths (cp262.txt and cp263.txt): zero error
  lines naming any owned file on either version, and class files
  emitted for all three owned files on both versions (including the
  renderer's ShroudEmitters and DeterministicRng inner classes).
- Residual errors in the runs are only the known baseline kinds in
  closure files: on 26.2, DbaItems CreativeModeTab Output protected
  access and DbaMenus MenuType private access; on 26.3, the same
  plus the hostile mob kinds (DbaHostileEntity, FriezaSoldierEntity
  swing signature, NamekianWarriorEntity, PatrolRobotEntity). None
  reference a dread symbol.
- The mixin stub approach from the original dread report was not
  needed: the owned closure contains no mixin file, and
  MeltingOverlayMixin was not touched.
- git diff --check on the renderer is clean; the em dash scan of
  added lines and of this report is zero.

## Honest worse check

- The shroud now reads as drifting dark smoke with the material
  profiles' own lifetimes instead of two continuous counter-turning
  bands. Coverage near the victim is comparable at close range, but
  particle budgets and distance scaling thin it sooner at range than
  the geometry thinned. That is the assigned material trade; whether
  it reads as dread or as ordinary smoke is an in-game judgment this
  environment cannot make.
- Quad particle sprites are square and cannot squash on one axis, so
  the blink's eyelid squash is the one visual behavior that changed
  form: the identical openness curve (close 2 ticks, hold 1, reopen
  3, same periods and phases) now also drives alpha (0.10 to 1.0 of
  the pair alpha) and a slight scale (0.72 to 1.0). A fully closed
  eye reads as a small faint eye instead of a thin slit. Timing is
  unchanged; the silhouette at the bottom of the blink is not.
- The eye layer runs on the translucent particle pipeline instead of
  the old emissive entity render type. Full-bright light is passed
  for every sprite, but fog handling on that pipeline can differ at
  range; the eyes sit 2.3 to 4.6 blocks from the victim, so any
  difference should be negligible, and it is unverified either way
  until the PC test.
- Nothing was reverted. No mechanic, duration, sound, or number
  changed anywhere in the system.

## Change requests

None. The Phase 1 foundation (MaterialEmitters presets, sources,
and budgeted emitters, plus the quad particle sprite path) covered
both conversions without shared edits.
