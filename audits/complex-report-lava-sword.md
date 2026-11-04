# Complex Effects Redo, Phase 2 Report: Lava Sword

Owner slot: Lava Sword (theme: flame, magma embers, smoke, ash, vapor).
Base: HEAD 9178eac. Files edited, and only these five:
LavaBlobRenderer.java, LavaCleaveRenderer.java, LavaEruptionRenderer.java,
LavaGeyserRenderer.java, LavaMeteorRenderer.java, all under
src/client/java/com/dragonblockarcanedba/client/render/.
No commits. No entity, block, blockstate, model, or texture files touched.
Themes, colors, damage numbers, cooldowns, costs, timing, mechanics, and
synced fields are identical to HEAD: every piece is seeded from the same
synced seed the emission already used, and every envelope, radius, height,
and reach value is the value the existing submit code already computed.
The existing material emission is unchanged in every file and now reads as
the filler layer only. Zero drawColoredBox, drawCylinder, drawDisc,
drawSphere, or drawHelix calls (grep verified). All fire and energy pieces
go through ComplexGeometry.submitEnergy with peak alphas 0.92 to 0.95;
the one solid piece goes through submitSolid at 0.96. No low constant
alpha is used to fake faintness anywhere.

## LavaBlob (class M)

- Pieces layer: ComplexGeometry.flameTongues through submitEnergy with
  ComplexTextures.FLAME_TONGUE. Five tongues, six segments, base at local
  (0, 0.16, 0) on the blob crown, radius 0.30, height 0.78, hot 0xFFF2B0
  to theme 0xFF5A00, alpha 0.95, sway phase age times 0.55, envelope the
  same floored envelope the emission uses. About 240 vertices, budget
  M 2500.
- Complex particle layer: EMBER_FRAGMENT through
  MaterialEmitters.Preset.fixed as a continuous emitter on the existing
  trail source, rate 30 per second, budget 38, at the standard
  continuous cap.
- Filler: unchanged flame, ember, ash, smoke, vapor cluster.

## LavaCleave (class M)

- Pieces layer, two builders: ComplexGeometry.tendril through
  submitEnergy with ComplexTextures.TENDRIL as the wave body, from 1.35
  behind the center to 0.55 ahead along the real velocity direction
  (rotation fallback, same frame the emission sweeps), control point
  bowed 0.35 along the diameter axis, width 1.05, hot 0xFFF2B0 to
  0xFF5A00, alpha 0.95. Plus flameTongues at the leading contact point
  (travel times 0.45): four tongues, radius 0.55, height 0.85, hot
  0xFFF2B0 to deep 0xE63A00, alpha 0.92. Combined about 580 vertices,
  budget M 2500. Piece envelope is max of the lifecycle envelope and
  open times trail, so the body is present for exactly the swept life.
- Complex particle layer: EMBER_FRAGMENT continuous emitter on the
  existing swept source, rate 30, budget 38.
- Filler: unchanged flame, ember, ash, smoke, vapor swept volume.

## LavaEruption (class L)

- Pieces layer, two builders, shader treatment on every fire piece:
  flameTongues crown at full budget (nine tongues, the builder maximum)
  placed at the wave front, facing times reach times 0.52, radius reach
  times 0.42, height the existing eruption height times 1.25 plus 0.35,
  hot 0xFFF2B0 to 0xFF5A00, alpha 0.95, through submitEnergy with the
  flame canvas. Plus crackFan at the base through submitEnergy, centered
  forward at facing times reach times 0.30 via a pose translation, with
  radius reach times 0.62 so crack tips stay inside the cone reach the
  damage uses. Combined about 1100 vertices, budget L 6000.
- Complex particle layer: EMBER_FRAGMENT continuous through the existing
  cone source at rate 36, budget 40, and ASH_FLAKE continuous on the
  ground source at rate 22, budget 30.
- Filler: unchanged flame, ember, ash, smoke, vapor. The entity's
  DebrisSpawner field already covers big physical chunks server side.

## LavaGeyser (class M)

- Pieces layer: flameTongues column crown through submitEnergy with the
  flame canvas. Seven tongues, radius 0.85, height 1.15, base at local
  (0, height, 0) where height is the same 4.4 times build curve the
  emission samples, so the crown rides the top of the pillar as it
  grows. Hot 0xFFF2B0 to 0xFF5A00, alpha 0.95, sway age times 0.60.
  About 336 vertices, budget M 2500.
- Complex particle layer: EMBER_FRAGMENT continuous emitter on the
  existing column source, rate 34, budget 40.
- Filler: unchanged flame, ember, ash, smoke, vapor column.

## LavaMeteor (class L)

- Pieces layer, three builders: flameTongues tail cluster in flight
  through submitEnergy with the flame canvas (seven tongues, base at
  local (0, 0.30, 0), radius 0.55, height 1.45, hot 0xFFF2B0 to
  0xFF5A00, alpha 0.95, swaying against the fall). At impact:
  fracturePlates through submitSolid (radius 2.6, lift 0.38, hot crust
  0xFF7A1A to dark 0x2E0F06, alpha 0.96) plus crackFan through
  submitEnergy (radius 2.9, hot 0xFFF2B0 to 0xFF5A00, alpha 0.95). The
  impact pair is placed at the real surface under the meteor from the
  client heightmap (MOTION_BLOCKING at the meteor x, z), translated in
  the pose to groundTop minus entity Y, and ramps in with a smoothstep
  of proximity over the last five blocks of the fall. Combined about
  1400 vertices, budget L 6000.
- Complex particle layer, heavy as assigned: EMBER_FRAGMENT continuous
  on the existing trail source at rate 40, budget 40 (the standard
  continuous cap), plus a one shot burst of 48 EMBER_FRAGMENT at the
  touchdown point, guarded to fire once per emission instance, inside
  the class L burst budget of 60.
- Filler: unchanged flame head and trail, ember, magma block chunks,
  ash, smoke, vapor.

## Verification

Targeted javac with JDK ~/workspace/tools/jdk-25.0.5+8/bin/javac,
sourcepath src/main/java:src/client/java, compiling the five owned
files, output under private /tmp tags lava-sword-262 and lava-sword-263,
logs at /tmp/lava-sword-262.log and /tmp/lava-sword-263.log, both read
in full.

- 26.2 classpath (cp262.txt): 4 errors, zero in owned files. All four
  are the pre-existing closure baseline: DbaItems.java line 535
  (CreativeModeTab Output protected access, twice) and DbaMenus.java
  line 16 (MenuType constructor and MenuSupplier private access).
- 26.3 classpath (cp263.txt): 10 errors, zero in owned files. The same
  DbaItems and DbaMenus baseline plus DbaHostileEntity.java line 79,
  FriezaSoldierEntity.java line 51, NamekianWarriorEntity.java line 33,
  and PatrolRobotEntity.java line 181 (26.3 API surface differences in
  main source files). None of these files are in this slot, and none of
  the error kinds touch the render closure the lava files use.
- Added lines em dash scan: zero (checked by character count on all
  five files). Primitive scan: zero banned calls. Diff stat: only the
  five owned files changed, 269 insertions, 16 deletions (the deletions
  are the replaced class javadoc blocks).

## Foundation change requests

None. Every builder, texture, submit helper, and sprite type this slot
needed already exists in the shared foundation as shipped. One
observation for the merge, not a request: ComplexGeometry.flameTongues
and crackFan take no origin offset (flameTongues takes a base point,
crackFan and fracturePlates are origin locked), so offsetting a crack
fan requires a poseStack push, translate, submit, pop around the submit
call, as done in LavaEruptionRenderer and LavaMeteorRenderer. That
pattern works and needs no toolkit change.

## Honest worse check

- LavaBlob, LavaCleave, LavaGeyser: no. Each gains a structured body
  (tongues on the crown, a torn tendril as the wave body, a crown riding
  the pillar top) where the particles-only version had only a soft
  cluster or column. The pieces are small relative to the filler volume,
  seeded, and envelope matched, so they add silhouette without hiding
  the material read. The cleave tendril is the one judgment call: it
  runs along the travel axis rather than across the diameter, so it
  reads as a streaking flame core inside the swept volume instead of a
  crescent edge. That matches the audit wording (tendril body) and the
  contact tongues mark the leading edge; it does not read worse than
  the emission-only version, which had no body at all.
- LavaEruption: no, with one caveat. The crown sits at the wave front
  and grows with reach and height, and the crack web stays inside the
  damage cone reach. The caveat: crackFan is a full radial fan, so even
  centered forward, some cracks run sideways and slightly behind the
  offset center. At 0.30 reach forward with 0.62 reach radius the rear
  tips reach about 0.32 reach behind the origin at ground level, which
  reads as base cracking around the caster rather than damage area
  overreach, and the filler cone already dominates the front. Acceptable
  inside the standard; a cone-clipped crack variant would be a shared
  toolkit extension, not a private fix, and is not needed to ship.
- LavaMeteor: partially, and this is the honest weak point of the slot.
  The in-flight tail tongues and the heavy fragment trail are a clear
  gain. The impact plates and crack web, however, can only ramp in over
  the last five blocks of the fall (a few rendered frames at meteor
  speed) because the meteor entity is discarded at impact and this
  renderer dies with it; there is no synced impact field and no
  persistent impact entity in this slot's scope to carry a lingering
  crater composition. So the impact pieces read as a brief ground break
  flash at touchdown, backed by the fragment burst, while the lasting
  impact read remains the entity's server side debris, crater pools,
  and lava settling, which are out of scope and untouched. This is
  still not worse than the pre-redo or particles-only state, which had
  no ground structure at all, but if Carter wants the meteor crater to
  linger as pieces, that needs a data or entity change (a short lived
  impact entity or a synced impact tick), which is outside this pass
  and is flagged here rather than faked.
