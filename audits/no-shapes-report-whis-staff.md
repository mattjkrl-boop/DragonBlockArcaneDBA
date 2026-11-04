# No-Shapes Report: Whis Staff

Phase 2 no-shapes conversion, written against audits/NO-SHAPES-STANDARD.md
and the Whis Staff section of audits/no-shapes-audit.md. Files edited, only
these three: item/WhisStaffItem.java, entity/TemporalRiftEntity.java,
client/render/TemporalRiftRenderer.java. TimeShatterEntity,
TimeShatterRenderer, and ErasureChargeOrb were not touched. No commits.

The Whis Staff has exactly one geometry effect surface: the Temporal Rift.
The item and entity edits are comment text only (the rift is no longer
described as a dome or astrolabe, and the synced seed is described as a
material seed). No damage number, cooldown, Ki cost, timing, mechanic,
or synced data field changed anywhere: the rift still deals its split
400 + Spirit x 1.5 in two halves (cast, then tick 15), still freezes for
60 ticks, still starts the 100 tick reversal at tick 60, still lives
100 ticks on a 140 tick cooldown, and the entity's server side layered
particles, vortex drift, debris field, sounds, and damage calls are
byte-identical.

## Before and after geometry counts (TemporalRiftRenderer)

| Check | Before | After |
|---|---|---|
| BespokeGeometry builder call sites | 14 | 0 |
| submitCustomGeometry blocks | 6 | 0 |
| LayeredRender geometry calls (drawVortexBands, drawAnchorGlow) | 0 | 0 |
| LayeredRender references of any kind | 5 | 0 |
| KiRenderHelper references (import and kiRenderType, no vertex calls) | 2 | 0 |
| Direct addVertex or KiRenderHelper.vertex calls | 0 | 0 |
| Local geometry helpers | 0 | 0 |

The 14 deleted builder calls were: displacedDome x4 (outer shell, inner
shell, center cap, zenith cap), brokenVortexBand x5 (three time currents,
two orbital rings), glowFan x1 (ground anchor), shardFan x1 (hour
steles), branch x1 call site in the twelve-stream loop (meridian
streams), and tornRibbon x2 (clock hands). The renderer now contains no
reference to BespokeGeometry, LayeredRender, or KiRenderHelper at all.

## What the rift is made of now

The renderer draws nothing. Each rendered rift owns one bundle of seven
MaterialEmitters.Emitter streams, keyed by entity id, built from the
synced seed and the entity's world position. All streams sample points
inside the field volume as data and give them inward velocities, so the
material streams into the rift instead of sitting on a shell:

| Stream | Material (DbaMaterialOptions) | Color | Rate per sec | Budget per sec | Role |
|---|---|---|---|---|---|
| Plasma core | PLASMA | 0xEAF8FF pale silver white | 90 | 130 | Bright core where the inner shell and center jewel sat |
| Plasma field | PLASMA | 0x61C2FF celestial cyan | 150 | 220 | Main field body, replaces both dome shells |
| Soul | SOUL | 0xCFF4FF pale soul cyan | 105 | 160 | Inward streaming currents, replaces the vortex bands and meridian streams |
| Glass dust, volume | GLASS_DUST | 0xDDF7FF | 75 | 115 | Suspended fractured temporal dust through the field |
| Glass dust, ground | GLASS_DUST | 0xBFEAFF | 42 | 70 | Ground residue drifting inward, replaces the glow fan anchor |
| Mist | MIST | 0xA9D8F2 | 26 | 44 | Sparse large boundary sprites suggesting the field edge, no shell drawn |
| Darkness core | DARKNESS | 0x0D1526 deep indigo black | 34 | 54 | Dark core at the rift center, per the standard's rift method |

The glass dust streams carry the irregular crack read: the GLASS_DUST
flipbook set is the foundation's cellular breakup texture, concentrated
in the volume and across the ground footprint. The clock identity
(hands, steles, orbital rings, jewels) is not recreated in any form; the
temporal read now comes from the inward motion, the glass dust, and the
dark core.

Lifecycle and scaling carry over:

- FxEnvelope with the same fractions as before (build 0.15, decay 0.35)
  scales every stream, so material density, the entity's server
  particles, and the tick 15 damage peak all agree.
- The build phase radius unfold is preserved: the sampled emission
  volume grows from 0.35 to the full radius across the build phase
  instead of a mesh growing.
- Distance scaling uses MaterialEmitters.distanceScale, multiplied into
  the envelope passed to each Emitter. The toolkit Emitter applies the
  effects density config and its hard per-second budget on top.
- Emission delta is derived from the synced render age (game time), so
  emission pauses with the game and the accumulator keeps the rate
  frame-rate independent. Bundles for rifts that stop rendering are
  pruned after five seconds; an age reset or level change rebuilds the
  bundle.

## Verification

Targeted javac audit (the standing method; no Gradle run or claimed):
the three owned files compiled as roots with the full tree on the
sourcepath, against both audit classpaths.

- 26.2: zero error lines naming WhisStaffItem, TemporalRiftEntity, or
  TemporalRiftRenderer. Class files emitted for all three files,
  including the renderer's new inner classes. The run's only residual
  errors are 4 lines in files this owner does not own (DbaItems x2,
  DbaMenus x2), the known baseline kinds.
- 26.3: zero error lines naming any owned file; class files emitted for
  all three. Residual errors are 10 lines in non-owned files (the same
  DbaItems and DbaMenus kinds, plus the known 26.3 mob entity kinds in
  DbaHostileEntity, PatrolRobotEntity, FriezaSoldierEntity, and
  NamekianWarriorEntity).
- Grep counts after conversion, across all three owned files:
  BespokeGeometry 0, LayeredRender 0, KiRenderHelper 0, addVertex 0.
- git diff --check on the three owned files is clean.
- Item and entity diffs are comment text only, verified by reading the
  diff: no code line changed in either file.

## Shared change requests

One, recorded rather than worked around: the foundation has no ground
or wall decal surface. The standard asks rifts for irregular crack
textures as decals where the owning effect already marks a surface;
the Temporal Rift never marked a surface (its old ground element was a
glow fan, now ground glass dust), so no private decal system was
invented. If the coordinating pass later adds a shared decal surface,
the rift's ground footprint is the natural place to lay a temporal
crack decal under the glass dust.

## Worse check (honest)

- The astrolabe identity is gone by design: no dome edge, clock hands,
  steles, rings, or jewels. The cost is real: the exact 12 block field
  boundary is less sharply readable than the dome shell was. The
  gameplay radius, freeze, and damage area are unchanged, but a player
  judging the edge by eye now reads mist and density falloff instead of
  a drawn shell. Accepted under the standard, flagged here.
- Material visibility leans on the effects density config and distance
  scaling more than geometry did. At low density settings or far
  distances the field reads thinner than the old mesh did. The rates
  and budgets above are sized to keep it present at default settings,
  but the trade exists and only an in-game look settles it.
- Glass dust in the volume falls and bounces under its material
  profile, so some of it settles downward instead of hanging. The
  entity's server side debris field still hangs in suspension, so the
  suspended read is not lost overall.
- Nothing was reverted. No file outside my list was edited by me.
