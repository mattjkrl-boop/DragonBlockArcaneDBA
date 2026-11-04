# No-Shapes Realism Pass Report: Dabura's Sword

Owner scope: DarknessBladeRenderer, DarknessChargeRenderer, DarknessDomainRenderer, DarknessShatterRenderer, and DarknessWaveRenderer.

Standard: audits/NO-SHAPES-STANDARD.md. Identity source: audits/bespoke-report-dabura-sword.md. Audit rows: audits/no-shapes-audit.md, Dabura's Sword section.

Only the five assigned renderer files were edited for the conversion. No item, entity, shared foundation, particle registry, or shared renderer file was changed. No damage number, damage type, cooldown, Ki cost, timing, duration, hitbox, movement value, mechanic, or synced data field was changed.

## Before and after geometry counts

Counts use the audit definitions: B is BespokeGeometry builder calls, L is LayeredRender geometry calls, and V is direct vertex drawing.

| Effect | Before B | Before L | Before V | After B | After L | After V |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| DarknessBlade | 6 | 0 | 0 | 0 | 0 | 0 |
| DarknessCharge | 11 | 1 | 0 | 0 | 0 | 0 |
| DarknessDomain | 9 | 2 | 0 | 0 | 0 | 0 |
| DarknessShatter | 9 | 0 | 0 | 0 | 0 | 0 |
| DarknessWave | 8 | 0 | 0 | 0 | 0 | 0 |
| Owner total | 43 | 3 | 0 | 0 | 0 | 0 |

Final grep over the five owned renderers also found zero BespokeGeometry references of any kind, including Rng, zero LayeredRender references, zero KiRenderHelper references, zero submitCustomGeometry calls, and zero addVertex calls. The renderers submit no custom geometry at all.

## Foundation use

Each renderer stores its MaterialEmitters.Emitter set in its render state, keyed by the existing entity UUID seed and the current client level. The sources read the current render state on every sample, so moving entities carry their emission volume with them. Emission uses the deterministic Emitter tick form with a delta derived from the render state age, the toolkit per-second budgets, the effects density config inside the toolkit, and MaterialEmitters.distanceScale for distance falloff. Lifecycle fades from the old renderers became emission envelopes. No private particle system, texture, or geometry helper was added.

All materials are DbaMaterialOptions values passed through MaterialEmitters.Preset.material: DARKNESS, PLASMA, SOUL, VAPOR, and DUST where the audit row calls for dust.

## Conversion by effect

### DarknessBlade

Materials: PLASMA in violet, crimson, white, and pale red; DARKNESS; VAPOR; SOUL.

The layered blade arcs and wake ribbons are gone. Seven emitters shed material along the falling path: a violet plasma body, a crimson inner stream, a narrow white hot stream, a darkness sheath, a wider vapor wake, soul vapor, and bright plasma sparks sampled at the leading end. The samples rotate with the same yRot plus age roll term the old renderer used, and the old 3 tick fade-in is now the emission envelope. The entity's own server-side void smoke and spark trail were already present and are unchanged.

### DarknessCharge

Materials: DARKNESS, PLASMA, SOUL, and VAPOR.

The ground seal bands, converging branch shards, helical bands, nested dome core, and anchor glow are gone. Ground darkness and plasma are sampled on rings around the caster with inward sample velocity, soul and darkness are sampled in a volume around the core with inward drift, plasma fills the shrinking core volume, and vapor is sampled on the two old helix paths with tangential and rising velocity. The paths are emission data only; nothing is drawn along them. Base radius still grows from 1.3 to 2.8 with charge, the core still sits at y 0.85, the pulse formula is preserved, and emission density rises with the charge ratio.

### DarknessDomain

Materials: DARKNESS, SOUL, VAPOR, and PLASMA.

The displaced dome shells, ground vortex bands, orbiting branch spires, vortex tube bands, rim bands, halo band, anchor glow, and shared LayeredRender vortex bands are gone. In their place:

- DARKNESS and SOUL fill the volume under the old dome profile, sampled by horizontal radius and the dome height curve, with tangential drift.
- DARKNESS and large VAPOR sprites are sampled sparsely on the dome boundary itself, suggesting the film without drawing a shell.
- DARKNESS and PLASMA swirl across the ground area.
- PLASMA in violet and crimson rises through the central column volume.

The domain becomes darkness motes and drifting vapor rather than any dome surface, as assigned. Radius still comes from the synced entity field, dome height still follows min(26, radius x 1.35), the synced lifecycle fade still gates the whole presentation, and the interior and ground emission rates scale mildly with radius inside fixed per-emitter budgets. The domain entity's own layered particles, debris fields, and real BoltEntity strikes are untouched.

### DarknessShatter

Materials: DARKNESS, DUST, PLASMA, and SOUL.

The glow fan shock discs, crack stars, branch slabs, plume column, and additive halo discs are gone. Darkness and dust move outward across the expanding ground area at the old shock expansion rate, violet and crimson plasma are sampled along eight seeded radial crack paths as emission data, and darkness, soul vapor, and dust rise through the central eruption volume. The expansion radius, column height growth, column thinning, fixed 40 tick life, and corrected fade are preserved as source and envelope math.

### DarknessWave

Materials: DARKNESS, PLASMA, VAPOR, and SOUL.

The blade arc crescent, additive arc, leading torn ribbons, and trailing lashes are gone. The wave is now a rolling front of darkness and violet plasma with a crimson core, a trailing vapor stream behind the front, and soul wisps through the body. Samples are placed in a volume built from the entity's real forward, right, and up vectors, using the old span and height values and the old first-person opening scale, so the material front matches the wave hitbox instead of the old oversized crescent silhouette. The open-over-5-ticks, full-to-45, gone-at-60 lifecycle is preserved as the emission envelope.

## Verification

Targeted javac, JDK 25.0.5+8, with the five owned renderer files as roots and a sourcepath over src/main/java and src/client/java:

- 26.2 classpath (cp262.txt): zero errors in any owned renderer file. Class output was produced for all five renderers and all five render states. The only errors in the run were 4 pre-existing baseline error lines in item/DbaItems.java and inventory/DbaMenus.java, outside this owner scope.
- 26.3 classpath (cp263.txt): zero errors in any owned renderer file. Class output was produced for all five renderers and all five render states. The only errors in the run were 10 pre-existing baseline error lines in item/DbaItems.java, inventory/DbaMenus.java, entity/DbaHostileEntity.java, entity/FriezaSoldierEntity.java, entity/NamekianWarriorEntity.java, and entity/PatrolRobotEntity.java, outside this owner scope.

git diff --check over the five owned files is clean, and the added lines contain no em dashes.

Javac audit only. No Gradle build was run or claimed, and no in-game look is claimed from this environment.

## Change requests

1. Ground crack decals have no shared foundation support. Phase 1 provides no decal render layer, decal source, or crack decal texture route that a renderer owner can call, and creating one privately would violate the shared foundation rule. DarknessShatter therefore carries its cracks as plasma sampled along radial emission paths plus dust and darkness, with no persistent crack decal left on the ground. Request: a coordinating-pass ground decal system, with irregular crack textures, that ground owners can stamp at an impact point.
2. No other toolkit gaps were hit. The Emitter, Preset, and Source set covered the blade, charge, domain, and wave conversions, including per-sample source velocity for inward gathering, tangential swirl, and the wave's backward vapor stream.

## Honest worse check

Nothing was reverted, and no mechanic changed. The real visual trades, stated plainly:

- DarknessBlade no longer reads as a giant sword object. It reads as a dense falling column of plasma, darkness, and vapor with sparks at the leading end. That is the audit's material assignment for this effect, but the readable sword silhouette is a genuine loss. Restoring it would require a detailed physical falling-sword model, which is outside this renderer's assigned material conversion.
- DarknessDomain has no hard edge anymore. The boundary is inferred from motes and sparse vapor film, so the exact domain radius is less legible than the old dome shell, and at the largest radii the budgeted volume can look thinner than the shell did.
- DarknessWave has no sharp leading silhouette. It reads as a rolling material front, which matches the hitbox more honestly, but at long range the distance-scaled emission can look faint compared with the old crescent.
- DarknessCharge loses its readable seal rings and shard objects. Charge level now reads through density, inward motion, and the still-growing sample radius instead of constructed ring and core shapes.
- DarknessShatter loses the erupting slab objects and any persistent crack mark. Its crack read lasts only as long as the plasma and dust particles live, pending the decal change request above.
- Particle counts are budgeted per emitter, and simultaneous copies of the same effect stack linearly. The domain is the heaviest of the five; its rates are capped, but several full-radius domains at once will cost more than the old mesh draw did.
