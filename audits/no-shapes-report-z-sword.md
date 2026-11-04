# No-Shapes Realism Report: Z Sword

Scope: the five renderer files assigned to the Z Sword in audits/no-shapes-audit.md: ZChargeRenderer, ZGravitySlamRenderer, ZShockwaveRenderer, ZStanceAuraRenderer, DelayedLaunchRenderer. Base HEAD ad22b32. No shared files were edited, no entity, item, or mechanic file was touched, and nothing was committed.

## Before and after geometry counts

Counts follow the audit method: B is BespokeGeometry builder calls, L is LayeredRender geometry calls (drawVortexBands, drawAnchorGlow), V is direct vertex drawing.

| Effect | B before | B after | L before | L after | V before | V after |
| --- | --- | --- | --- | --- | --- | --- |
| ZCharge | 13 | 0 | 0 | 0 | 0 | 0 |
| ZGravitySlam | 9 | 0 | 1 | 0 | 0 | 0 |
| ZShockwave | 4 | 0 | 0 | 0 | 0 | 0 |
| ZStanceAura | 13 | 0 | 0 | 0 | 0 | 0 |
| DelayedLaunch | 5 | 0 | 1 | 0 | 0 | 0 |
| **Owner total** | **44** | **0** | **2** | **0** | **0** | **0** |

Post-conversion greps over the five owned files: BespokeGeometry 0 references, LayeredRender 0 references, KiRenderHelper 0 references, addVertex 0, submitCustomGeometry 0. Every renderer now only extracts state and emits particles in submit; none of them submits geometry.

## Conversion method

Each renderer keeps a small per-entity emission record keyed by entity id and synced seed. The record holds shared MaterialEmitters.Emitter instances (frame-rate independent accumulator, hard per-second budgets, density config applied inside the toolkit) plus a mutable frame that the renderer updates each submit with the current center, radii, envelopes, and orientation. Sources are MaterialEmitters.Source data samplers: they supply points and velocities, and draw nothing. Distance scaling uses MaterialEmitters.distanceScale, and every emitter tick is multiplied by the effect's existing lifecycle envelope times that distance scale, matching the role distScale played in the geometry versions.

## Materials per effect

Theme throughout: gravity plasma, stone dust, and steel spark accents, with the Z Sword gold and violet colors preserved from the bespoke renderers. Spark accents are carried by small bright plasma (charge core sparks, the shockwave core stream and apex concentration, stance anchor glints), since this owner's brief builds on the DbaMaterialOptions material family.

- **ZCharge:** gold PLASMA and pale gold VAPOR sampled through the old funnel volume, always moving inward toward the sword core with a slight swirl; stone DUST sampled across the ground seal area and pulled inward; a bright gold PLASMA core emitter at sword height whose rate steps up at the old overdrive threshold (charge 0.4). The levitating prisms are not rebuilt in any form: their positions are inside the same inward volume stream, where dense bright plasma glints replace them. Fade-in, release fade over FADE_LENGTH, base radius, and funnel height formulas are unchanged.
- **ZGravitySlam:** stone DUST and pale VAPOR driven outward across the growing footprint; violet PLASMA rising through the trench area; physical debris through a fixed preset carrying BlockParticleOption on DbaParticles.BLOCK_CHUNKS with the actual block state under the slam (stone fallback), launched from the column bases on the strike envelope; gold PLASMA geyser eruptions gated by the old strike envelope. The monoliths are the hard case and are handled as ordered below.
- **ZShockwave:** the span, chord, and height survive only as emission data in the entity orientation frame (the same yaw and pitch basis the geometry pose used). Dense gold PLASMA carries the front, a narrower near-white PLASMA stream carries the hot center, pale gold VAPOR trails the wake, stone DUST rides the lower edge, and a PLASMA concentration at the chord point replaces the apex burst on the strike envelope. Fade-in over 6 ticks, fade-out over the final 25 ticks, growth from 80 percent, and the first-person owner scaling are unchanged.
- **ZStanceAura:** violet PLASMA and pale violet VAPOR fill the well volume, pulled toward the axis, swirling, and sinking; stone DUST gathers inward across the ground; a tight bright PLASMA concentration at the center replaces the singularity eye. The monolith anchors and rune positions are handled as ordered below.
- **DelayedLaunch:** stone DUST drifts across the full true slam radius for the whole countdown; violet PLASMA and pale violet VAPOR are sampled inside the contracting radius and pulled inward, faster as progress rises; gold PLASMA concentrations at the four rotating cardinal data points replace the rune ticks. True radius, true delay, pulse, and rotation speed formulas are unchanged.

## The gravity monoliths

Both monolith sets (gravity slam, stance anchors) are converted with no slab, ribbon, or silhouette of any kind. Their seeded placement data survives: the slam recomputes the same Random(seed) column sequence (angle, distance fraction, height, width, tilt) the ribbon version consumed, and the stance recomputes the same rotating anchor ring (count, distance, height, width, pulse). At each anchor the renderer emits a dense falling column of stone DUST and heavy gravity PLASMA, with physical block chunks at the slam column bases and gold plasma glints above the stance anchors. The columns grow, decay, and sink on the same grow and decay envelopes the slabs used.

## Numbers and mechanics preserved

Only renderer files changed, so every damage number, damage type, cooldown, Ki cost, timing, duration, hitbox, and synced data field is untouched by construction. Within the renderers, all lifecycle formulas were carried over before the draw calls were replaced: charge fade-in and release fade, stance fade-in and release fade, slam progress, grow, strike, and decay curves, shockwave fade, growth, and first-person scaling, and the delayed launch contraction from the true radius to 35 percent over the true delay.

## Verification

Targeted javac with JDK 25, sourcepath src/main/java:src/client/java, the five owned renderers as roots:

- 26.2 classpath (cp262.txt): zero errors in any owned file; 20 owned class files emitted. The only errors in the run are 4 pre-existing baseline errors in untouched files (DbaItems CreativeModeTab access x2, DbaMenus MenuType access x2).
- 26.3 classpath (cp263.txt): zero errors in any owned file; 20 owned class files emitted. The only errors in the run are 10 pre-existing baseline errors in untouched files (DbaItems x2, DbaMenus x2, DbaHostileEntity swinging and attackAnim x2, FriezaSoldierEntity swing signature, NamekianWarriorEntity hurtMarked, PatrolRobotEntity swinging and attackAnim x2).
- Em dash scan of added lines in the five renderers and this report: clean.
- Javac audit only. No Gradle build was run and no in-game look is claimed.

## Toolkit change requests

1. Ground decals: the standard's ground method calls for crack and scorch decals, but the Phase 1 foundation ships no decal API, only particles. The slam trench pattern and the delayed launch footprint are therefore carried by dust, vapor, and plasma alone. A shared decal layer (irregular crack textures projected at a world position with a lifetime envelope) would restore a persistent ground mark for slam and telegraph effects across owners.
2. Dynamic emitter bindings: MaterialEmitters.Emitter fixes its preset and source at construction, so any effect whose center, radius, or orientation moves needs a mutable frame holder of the kind used here. A toolkit-level updateable source wrapper would make this pattern one shared class instead of a per-renderer holder.

## Honest worse check

Not a clean sweep; the losses are the ones the standard accepts, stated plainly:

- The monoliths have no solid presence at all now. At low density settings or long distance, an anchor reads as falling dust and plasma rather than as a massive stone weight. The sense of huge solid Katchin blocks is weaker than both the spectacle slabs and the bespoke ribbons. This is the biggest loss in the conversion.
- The charge prisms and overdrive flares are gone as distinct objects. They read as bright glints inside the inward stream, which is less legible than tumbling crystals, especially early in the charge before the core emitter steps up.
- The shockwave has no continuous front edge. At minimum density it can read as a spray of plasma and dust instead of one coherent wave, even though the emission frame matches the damage band exactly.
- The slam lost its drawn trench pattern and the delayed launch lost its precise contracting ring. The areas are still marked for their full true sizes, but the boundary is a particle density falloff, not a line a player can read to the block. Change request 1 is aimed at this.
- Emission starts from the render submit path, so the first frame of each effect emits one nominal 60 fps step before the accumulator measures a real frame delta. This is invisible in practice but is a difference from the geometry version, which was at full envelope on frame one (both charge and stance keep their multi-tick fade-ins, which cover it).

Nothing else regressed: colors, envelopes, radii, counts of anchor positions, and all gameplay behavior match the bespoke versions, and no piece had to be reverted.
