# Complex Effects Redo, Phase 2 Report: War Mace

Owner slot: War Mace. Files edited (only these two):
`src/client/java/com/dragonblockarcanedba/client/render/WarMaceRiseRenderer.java` and
`src/client/java/com/dragonblockarcanedba/client/render/WarMaceSmashRenderer.java`.
No entity code touched, no commits, no foundation files changed. Themes, colors,
damage, cooldowns, costs, timing, mechanics, and synced fields are identical to
HEAD 9178eac; every existing material emission is kept unchanged as the filler
layer in both renderers.

## WarMaceRise (class M)

Pieces layer added, drawn every submit at the rise point in the local ground
frame, seeded from the entity's synced seed:

- `ComplexGeometry.fracturePlates` through `ComplexGeometry.submitSolid`:
  small plates, radius 1.05, lift 0.16, hot color GHOST_LOW bronze 0xD99E47,
  dark color 0x4A3826, peak alpha 0.92, envelope is the existing FxEnvelope
  value. About 480 vertices.
- `ComplexGeometry.crackFan` through `ComplexGeometry.submitEnergy`:
  radius 1.35 (cracks run through and past the plates), hot WISP 0xFFEBB3 to
  cool GHOST_LOW 0xD99E47, peak alpha 0.95. Under 900 vertices.

Combined piece vertices are about 1100 to 1400, inside the class M budget of
2500, and each builder is inside its own cap (plates at most 1600, cracks at
most 900).

Complex particle layer added:

- One shot `MaterialEmitters.burst` of SHARD_CHIP, count 20 (class M burst
  budget 30), fired once per emission bundle on its first frame, off the
  existing base ring source, velocity up 0.32, spread 0.18.
- Continuous SHARD_CHIP emitter (`Preset.fixed`, up 0.28, spread 0.16) off the
  same base ring source, rate 26 per second (continuous cap 40), ticked only
  while the existing launch ring envelope is active, matching the base dust
  and vapor.

Filler unchanged: main, wisp, and ghost plasma arcs, arc vapor, SPARK steel
sparks, base ring dust and vapor. Plasma is no longer the only layer; the
ground structure at the launch point is now solid plates plus shader cracks
plus stone chips.

## WarMaceSmash (class L, crater flagship)

Pieces layer added, two builders, drawn in the local ground frame, seeded
from the entity's synced seed:

- `ComplexGeometry.fracturePlates` through `ComplexGeometry.submitSolid` at
  the builder's full plate budget (the builder draws its full plate count at
  distScale 1.0; no reduced count was requested). Radius follows the growing
  shock radius but is capped at the footprint the entity really breaks:
  4.0 normal (crater radius 3 plus the permanent fracture ring one wider)
  and 5.0 heavy (crater radius 4 plus the ring). Lift 0.30 normal, 0.42
  heavy. Hot color SHELL_NORMAL 0xD9A352 or SHELL_HEAVY 0xDB8F33, dark
  0x4A3826, peak alpha 0.96. About 480 vertices.
- `ComplexGeometry.crackFan` through `ComplexGeometry.submitEnergy`: radius
  plate radius times 1.08, hot HOT_CORE 0xFFE69E to cool BRONZE 0xD99E47,
  peak alpha 0.95. Under 900 vertices.

Combined piece vertices are about 1100 to 1400, inside the class L budget of
6000, with each builder inside its own cap.

Ground gate: the entity code (untouched) plants its crater only when the
smash reaches the ground; a heavy smash rides its falling owner down first.
There is no synced impacted field the renderer can read, so the pieces layer
is gated on a renderer side ground check, `groundNear`: solid ground within
about three blocks under the center or four ring sample points. That range
reaches the crater bowl floor after the crater is dug (about two blocks at
the center) and finds only air while a heavy smash is still high in its
fall, so plates never float in mid air with no crater under them.

Complex particle layer added:

- One shot `MaterialEmitters.burst` of SHARD_CHIP, count 48 (class L burst
  budget 60), fired once per emission bundle on its first frame, off the
  impact disc source, velocity up 0.38, spread 0.22.
- Continuous SHARD_CHIP emitter (`Preset.fixed`, up 0.30, spread 0.15) off
  the same disc source, rate 36 per second (continuous cap 40), ticked with
  the main envelope.

Filler unchanged: disc dust, splash plasma, three rim swirl layers, shell
dust and vapor, core plasma, crack dust and plasma, real ground block debris
chunks, SPARK steel sparks, plume dust and embers. The entity's server side
DebrisSpawner field is untouched and still carries the big physical chunks;
PLASMA emission is filler only now, never the body.

## Budgets and gates

- Zero `drawColoredBox`, `drawCylinder`, `drawDisc`, `drawSphere`, or
  `drawHelix` calls in either file (grep verified).
- Energy pieces (both crackFans) go through `submitEnergy`; solid pieces
  (both fracturePlates) go through `submitSolid`. Peak alphas are 0.92 to
  0.96, honest body alphas; no low constant alpha is used to fake faintness.
- No private geometry, particle, or texture toolkit was built; everything
  uses ComplexGeometry and MaterialEmitters as shipped.
- Split source sets stay clean: both files were already client only and the
  new imports are client side classes only.

## Verification

Targeted javac with JDK at ~/workspace/tools/jdk-25.0.5+8/bin/javac,
`-sourcepath src/main/java:src/client/java`, compiling both owned files,
output to private /tmp tags (warmace-262, warmace-263). Both full logs were
read.

- 26.2 classpath (cp262.txt): 4 errors, all pre existing baseline kinds in
  files outside this slot (DbaItems CreativeModeTab Output protected access,
  DbaMenus MenuType private access). Zero errors in either owned file; both
  owned class sets were produced in the output directory.
- 26.3 classpath (cp263.txt): 10 errors, all pre existing baseline kinds in
  files outside this slot (the same DbaItems and DbaMenus errors, plus
  DbaHostileEntity, FriezaSoldierEntity, NamekianWarriorEntity, and
  PatrolRobotEntity symbol and swing signature errors). Zero errors in
  either owned file; both owned class sets were produced.
- `git diff --check` on both owned files: clean.
- Added lines em dash scan: zero.

## Foundation change requests

None required. Both assigned builders and the SHARD_CHIP sprite were
sufficient as shipped. One observation for the merge, not a blocker: the
fracturePlates builder internally draws 10 plates at distScale 1.0 while the
standard's budget table names 12 plates as the cap; this slot used the
builder exactly as shipped, which the other slam owners (KingsSlam,
ZGravitySlam, DevilSlamShockwave, OxShockwave) also did, so "full plate
budget" here means the builder's full output. If the merge wants a literal
12 plates, that is a shared ComplexGeometry change for all slam owners at
once, not a per owner variant.

## Worse check

WarMaceRise: low risk of reading worse. The plates are small (radius 1.05,
lift 0.16) and sit at the launch point under the existing base ring dust, so
they add ground structure without competing with the rising crescent, which
is the effect's identity and is unchanged. One honest caveat: the rise
entity's origin is the caster's feet, so if a rise is ever triggered mid
air, the plates would draw at feet level in the air for its 8 tick life.
The pre redo and particles only versions had the same base ring behavior at
that origin, and the rise is a ground launch in normal play, so this is a
bounded, pre existing framing rather than a new defect.

WarMaceSmash: this should read strictly better than the particles only
version. Before, the crater the entity really digs had no structured
renderer geometry over it at all; now lifted plates and glowing cracks sit
exactly on the crater and fracture ring footprint while the debris field,
block chunks, and chips carry the physical read. Two honest caveats.
First, the ground gate is a client side sample check, not the entity's true
impacted flag (which is not synced): on unusual terrain (deep crater bowls
on steep slopes, or ground more than about three blocks below the impact
point at some sample points) the plates could in principle appear a tick
early or late relative to the server crater. The window is small because
the check requires ground near, and the filler emission has always drawn
for the whole life regardless. Second, plate radius is capped at the crater
plus ring footprint while the dust shockwave keeps expanding to radius 4.5
or 7.6, so late in the effect the plates read as the broken core inside a
wider dust front. That is deliberate: plates beyond the ring would float
over ground the entity never broke, which would read worse. Neither caveat
makes the result worse than the particles only version, and neither needs a
fix outside the standard; if Carter wants the impacted moment exact, the
fix is a synced impacted field on the entity, which is a data change
outside this pass and outside this slot.
