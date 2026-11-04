# Complex Effects Redo, Phase 2 Report: Evil Spear Dread

Owner slot: Evil Spear Dread. File edited: only
`src/client/java/com/dragonblockarcanedba/client/render/DreadGazeRenderer.java`.
No commits. No other file touched by this slot: `effect/EvilDreadEffect.java`,
`entity/DreadGazeEntity.java`, the MeltingOverlayMixin, and the Evil Spear
weapon renderers are untouched, and the d74f13a destruction wiring is untouched.

## DreadGaze (class M)

Layers added, on top of the existing presentation, which is kept whole:

- Pieces layer: five tendril wisps through one
  `ComplexGeometry.submitVeil` call sampling `ComplexTextures.TENDRIL`,
  each strand built with `ComplexGeometry.tendril`. Each strand is seeded
  from the victim seed (the same synced seed source as the existing
  emission and the eye ring, `state.seed ^ 0xD3AD5`, with a separate
  `ComplexGeometry.Rng` stream, so the eye ring layout, delays, periods,
  and phases do not move). A strand starts low on the outer shroud shell
  (radius factor 1.28, y 0.04), curls inward and upward through the shroud
  (control point at radius factor 0.82, about half the shroud height, and
  about a third of a turn around the victim in total), and ends just past
  the top of the shroud (radius factor 1.08, height factor 1.06), drifting
  slowly around the ring with age (0.008 radians per tick). Strand width
  is 0.24. Vertex colors are near-black, as assigned: root `0x0D0017`,
  which is exactly the existing darkness material color, grading to tip
  `0x2E0A16` (dark brimstone maroon) on even strands and `0x1E0533` (dark
  violet) on odd strands. Peak vertex alpha is an honest 0.95, inside the
  assigned 0.85 to 1.0 band: faintness and darkness come only from the
  near-black colors, never from a low alpha fed to the shader, and the
  fade rides the existing envelope (fade in over the anchor's first 16
  ticks, fade out over the effect's last 25), which is passed as the
  builder envelope.
- Vertex budget: at full detail one tendril is 24 spine samples at 16
  vertices per segment (two crossed quads per segment, double sided), so
  384 vertices per strand and 5 times 384 = 1920 vertices per submit,
  inside the class M budget of 2500. The builder scales the sample count
  down with `FxEnvelope.distScale` from the render state's camera
  distance, so distant submits cost less.
- Complex particle layer: `ASH_FLAKE` drift, one new persistent emitter
  in the existing per-anchor `ShroudEmitters`, using
  `MaterialEmitters.Preset.fixed(DbaParticles.ASH_FLAKE, ...)` sampled on
  the existing shell source at radius factor 1.10 with a slow tangential
  drift (0.010) and a slight downward preset velocity (minus 0.006 y).
  Rate 20 per second, budget 40 per second: both under the standard
  continuous cap of 40 per second per emitter. Ticked at envelope times
  0.90 alongside the existing emitters.
- Filler layer, kept exactly as it was: DARKNESS material emission
  (rate 20, shell factor 1.0) and SMOKE material emission (rate 11, shell
  factor 1.22), same colors, scales, seeds, velocities, and tick
  envelopes as at HEAD.

Untouched inside the file, by the hard scope lines: the eye sprites stay
exactly as they are (detailed textured sprites on their existing quad
particle path, halo pass plus core pass), and the blink behavior,
including the openness curve and the previous pass's alpha and scale
conversion of the blink, is byte for byte unchanged. The new pieces code
runs on its own seed stream and does not consume the eye ring's
deterministic stream.

No plain primitives: zero calls to `drawColoredBox`, `drawCylinder`,
`drawDisc`, `drawSphere`, or `drawHelix` in the file (checked by grep on
the final file and on the added diff lines). Themes, colors, and every
damage number, cooldown, cost, timing, mechanic, and synced data field
are identical to HEAD; the only new colors are the near-black tendril
root and tip colors above, which stay inside the existing darkness and
brimstone smoke palette.

## Verification

Targeted javac with JDK `~/workspace/tools/jdk-25.0.5+8/bin/javac`,
run from the repo root with `-sourcepath src/main/java:src/client/java`,
compiling only `DreadGazeRenderer.java`, full logs read before reporting:

- 26.2 (`-cp` from `~/workspace/dba-audit-libs/cp262.txt`, output under
  `/tmp/dba-dread2-262`): zero errors in the owned file. The log holds
  4 errors, all baseline elsewhere in the closure: `DbaItems.java` (2,
  protected access on CreativeModeTab Output) and `DbaMenus.java` (2,
  private access on MenuType). The `DreadGazeRenderer` classes were
  emitted.
- 26.3 (`-cp` from `~/workspace/dba-audit-libs/cp263.txt`, output under
  `/tmp/dba-dread2-263`): zero errors in the owned file. The log holds
  10 errors, all baseline elsewhere in the closure: the same `DbaItems`
  (2) and `DbaMenus` (2), plus `DbaHostileEntity.java` (2),
  `FriezaSoldierEntity.java` (1), `NamekianWarriorEntity.java` (1), and
  `PatrolRobotEntity.java` (2), all of the known 26.3 vanilla signature
  kind. The `DreadGazeRenderer` classes were emitted.
- `git diff --check` on the owned file: clean. Added lines contain zero
  em dashes (checked programmatically) and zero plain primitive calls.

Note on method: a first javac attempt used an absolute sourcepath whose
second entry did not expand, which hid the whole client source set and
produced spurious package errors, including on the pre-existing
MaterialEmitters import. That run was discarded; the results above are
from the specified relative sourcepath form, matching the other owners.

## Foundation change requests

None. The shipped foundation covered this slot completely: the
`submitVeil` texture overload, the `tendril` builder with its pinch and
twist profile, `ComplexGeometry.Rng`, `ComplexTextures.TENDRIL`, and
`MaterialEmitters.Preset.fixed` with `DbaParticles.ASH_FLAKE`. No private
toolkit, emitter, geometry, or texture was built in the owned file.

## Honest worse check

The named risk is real, and it is judged plainly here: near-black pieces
against a near-black shroud have low contrast where the shroud is at its
densest and the background behind it is also dark (night, caves, dark
rooms). In those exact conditions the tendril layer will be subtle to
nearly invisible, and most of the visible gain there comes from the
ASH_FLAKE layer, whose gray flakes are lighter than the shroud and read
clearly as drifting ash. That limit is inherent to the assigned
near-black palette; the only fix would be brighter strand colors, which
would break the assigned theme, so it was not done.

Why this still does not read worse than either earlier version:

- Nothing was traded away. The darkness and smoke filler rates, colors,
  and envelopes are unchanged, the eye sprites and blink are unchanged,
  and the tendrils and ash are purely additional layers on top.
- Where any lighter pixel sits behind or inside the shroud (daylight,
  lit terrain, the victim's body and armor, the red eyes at their wider
  radius), the strands do read: they are alpha blended dark curls with
  torn TENDRIL canvas edges, honest 0.95 peak alpha (so the shader life
  remap keeps them solid through build and hold), a width of 0.24, two
  alternating near-black hues for strand to strand separation, and a
  slow swirl plus the builder's own 3D curl, so they move as structured
  pieces rather than as static darkening. Moving dark strands against a
  lit background is exactly the brimstone smoke read the slot calls for.
- The strands cannot bury the effect's focal point: they hug the shell
  at roughly 0.7 to 1.1 blocks from the victim center, while the eye
  ring floats at 2.3 to 4.6 blocks, so no tendril ever crosses an eye.

Verdict: the conversion is a net gain in lit and mid-tone scenes and a
neutral, not worse, read in fully dark scenes. It is shipped on that
basis, with the dark-scene subtlety stated openly rather than hidden.
