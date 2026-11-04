# Bespoke Pass Phase 2 Report: Katana

Scope: item/KatanaItem.java; entity/KatanaChargeEntity.java, entity/KatanaAimGuideEntity.java, entity/SwiftCrescentEntity.java, entity/HeavenSplitterEntity.java; client/render/KatanaChargeRenderer.java, client/render/KatanaAimGuideRenderer.java, client/render/SwiftCrescentRenderer.java, client/render/HeavenSplitterRenderer.java. Base commit 2ef870b. HollowAfterimageEntity and its renderer were not touched (Hollow's Edge owns them); the item still spawns the afterimage exactly as before. No damage number, cooldown, Ki cost, range, or mechanic changed anywhere in this pass.

## Before and after counts (in the nine files above)

| Measure | Before | After |
| --- | --- | --- |
| Local regular-geometry helper definitions | 14 | 0 |
| Local helper call sites (textual) | 34 | 0 |
| Direct addVertex call sites in the renderers | 124 | 0 |
| Vanilla ParticleTypes references | 0 | 0 |
| Vanilla DustParticleOptions references | 0 | 0 |
| Vanilla SoundEvents references | 10 | 0 |
| Vanilla ability-damage references (damageSources().playerAttack) | 2 | 0 |
| java.util.Random uses in renderers | 2 | 0 |

Helpers deleted, by file: SwiftCrescentRenderer 2 (drawSilverCrescent, drawDiamondSpark, 3 call sites); HeavenSplitterRenderer 4 (drawArcingSlashWall, drawAngledCrossCleave, drawTransversalFractureRib, drawShockRingZ, 8 call sites); KatanaChargeRenderer 3 (drawRotatingRing, drawDiamondSpark, drawQuad, 11 call sites); KatanaAimGuideRenderer 5 (drawLaserBeam, drawRingZ, drawDiamondBracket, drawCrosshairSpokes, drawDiamondSparkZ, 12 call sites). The two strike() methods that remain in SwiftCrescentRenderer and HeavenSplitterRenderer are envelope timing math, not geometry. Particles needed no conversion: the katana entities already emitted only DbaParticles types and LayeredParticles.dust (which returns DbaDustOptions for THEME_DUST), and they still do.

## Geometry: what was rebuilt on which builder

Every builder call is seeded from the entity UUID's most significant bits, which is synced identity, so every client draws the same silhouette. SwiftCrescentRenderer and KatanaAimGuideRenderer gained a seed field on their render states to carry it; the other two renderers already had one.

SwiftCrescentRenderer (Flashdraw crescents):
- Crescent body: three nested BespokeGeometry.bladeArc calls in one pose (silver body radius 2.3 sweep 100, white core radius 1.93, colored lip radius 2.42), the pose yawed -90 degrees so the arc bulge faces +Z like the old strip's bow. Variant 1 mirrors local Z and swaps the lip to silver-blue, as before.
- Wake: one tornRibbon trailing toward -Z behind the belly, length driven by the same trail envelope value as the old wake.
- Belly spark: one shardFan in the crescent plane, alpha set to the strike impact value and envelope 1.0, matching the old spark which ignored the lifecycle fade and was gated by the strike window alone.
- Halo: the same bladeArc at 1.3x scale on the additive type with envelope fade x 0.18, as before.

HeavenSplitterRenderer (the wall):
- Wall: three tornRibbon layers (cyan mantle at 0.92 height, main wall, white core at 0.58 height) sharing one pose that stands the ribbon width vertical and runs the length from the far end of the path back to the origin, so the ribbon's bright start lands at the far end, matching the direction of the old wall's rising lead alpha, and its torn taper dies at the player's feet.
- Cross cleaves: one bladeArc (sweep 120) per cleave, stood into the wall plane and tilted +36 or -36 degrees in plane, same count, spacing, span formula, and color as before.
- Fracture ribs: pairs of BespokeGeometry.branch ribbons meeting at an apex on the wall face (two calls per rib, one submit for all ribs). Placement uses BespokeGeometry.Rng(state.seed) with the same ranges as the old java.util.Random placement.
- Shock rings: one bladeArc at sweep 360 per ring, stood into the wall plane at the path start, midpoint, and end, gated on the strike impact exactly as before, colors and alpha factors unchanged. Ring radius is scaled by 1.25 so the arc band (which spans 0.5R to 1.1R) centers where the old ring band sat.
- Anchor glow: unchanged (LayeredRender.drawAnchorGlow, already bespoke and compliant).

KatanaChargeRenderer (Flashdraw charge stance):
- Focus rings: three BespokeGeometry.brokenVortexBand calls lying near flat (y bands 0.02 to 0.10, 0.04 to 0.12, 0.06 to 0.16) with the age rotation fed through the builder's twist parameter at the old speeds (+25, -35, +48 degrees per tick factor), same radii, colors, and alpha products as the old rotating rings.
- Ground arc accent: two nested bladeArc calls (silver radius 1.3, white radius 1.12, sweep 80), bulge turned to +Z and offset so the band sits over the old parabola strip's footprint.
- Hilt sparks: two shardFan calls stood into the XY plane at the hilt position (white over cyan), sizes tracking the old spark size.
- Converging motes: the same golden angle spiral, counts, drift, and alpha curve as before, but each mote is now its own shardFan submit; the height jitter comes from BespokeGeometry.Rng(state.seed) instead of java.util.Random.
- Vortex bands: unchanged (LayeredRender.drawVortexBands, already compliant).
- Halo: one brokenVortexBand at 1.15x radius plus one shardFan at the hilt, additive, same alphas as before.

KatanaAimGuideRenderer (Heaven Splitter aim guide):
- Beam: crossed tornRibbon sheet pairs. One pose runs the ribbon length down +Z with width horizontal, a second pose adds a 90 degree roll around the beam axis so the width stands vertical. Sheath sheets (cyan, alpha factor 0.65) and core sheets (white, 0.95) in both poses, widths scaled from the old tube radii.
- Flow bands: one tornRibbon segment per traveling band, same band count, speed, length, and alpha falloff as before.
- Start ring, station rings, terminal ring: bladeArc at sweep 360 stood into the XY plane with the old rotation speeds applied as in-plane Z rotations; radius scaled by 1.25 so the band centers on the old ring radius. Station brackets and the terminal bracket-plus-spokes are shardFan calls in the same pose as their ring.
- Traveling nodes: one shardFan per node lying in the XZ plane at the node's z, same count, speed, size, and alpha falloff as before.
- Halo: terminal bladeArc at 1.25x plus a wide tornRibbon sheath sheet, additive, same alphas as before.

## Sounds: swap table (all call sites, volumes, and pitches unchanged)

KatanaItem:
- ENCHANTMENT_TABLE_USE (Flashdraw charge shimmer tick) to DbaSounds.CAST_MAGIC.
- FIRE_EXTINGUISH (Ki exhaustion cancel) to DbaSounds.FIZZLE.
- PLAYER_ATTACK_SWEEP (freeform dash) to DbaSounds.SLASH_WHOOSH.
- PLAYER_ATTACK_SWEEP (Heaven Splitter release) to DbaSounds.SLASH_WHOOSH.
- PLAYER_ATTACK_CRIT (Heaven Splitter release) to DbaSounds.CRIT_HIT.
- BEACON_ACTIVATE (Heaven Splitter charge tick) to DbaSounds.CHARGE_RISE; the DbaSounds.ABILITY_CHARGE layer that already played at the same tick is untouched.
- ANVIL_LAND (Flashdraw sheathing ring) to DbaSounds.BLADE_SHING.
- ANVIL_LAND (Heaven Splitter release) to DbaSounds.HIT_METAL.

SwiftCrescentEntity:
- PLAYER_ATTACK_CRIT (carried strike) to DbaSounds.CRIT_HIT; the DbaSounds.BLADE_SHING layer at the same call site is untouched.

HeavenSplitterEntity:
- LIGHTNING_BOLT_THUNDER (wall strike tick) to DbaSounds.THUNDER_CRACK; the DbaSounds.LIGHTNING_ZAP layer at the same call site is untouched. This is the weapon's own strike thunder, not a storm system, so it stays in the entity per the swap guide's thunder row.

One judgment call to flag: the DbaSounds guide's generic ANVIL_LAND row points at slam sounds (SLAM_THUD, GROUND_CRACK). Both katana ANVIL_LAND sites are blade sheathing rings, not slams; the Flashdraw site's own comment says "Blade sheathing ring sound". They were therefore mapped by use case to the bank's blade and metal sounds: BLADE_SHING for the Flashdraw ring (the same sound the splitter release already layered for its sheathe), and HIT_METAL for the splitter release ring, where BLADE_SHING already plays at the same tick and stacking the identical sample would only add volume. If the literal guide mapping is preferred, it is a two-line change.

## Damage

- SwiftCrescentEntity strike: level.damageSources().playerAttack(player) to DbaDamage.ki(this, player). The crescent entity is the direct entity carrying the strike; the owning player is the attacker.
- HeavenSplitterEntity wall strike: same conversion, DbaDamage.ki(this, player), falloff and capDamage calls untouched.
- The ki family is used for both because both abilities are Ki techniques (they drain Ki to charge); the bespoke audit maps no katana damage to void. Plain melee is untouched: KatanaItem has no melee damage code of its own, and the item's attack remains the vanilla attribute based player attack, as the standard requires. A git diff of the item and the two strike entities confirms the only changed lines are the sound event tokens, the two damage source expressions, and three removed SoundEvents imports; every number is byte-identical.

## Verification

Targeted javac (JDK 25.0.5+8) of the nine files with -sourcepath src/main/java:src/client/java, once per audit classpath:
- 26.2 (cp262.txt): zero errors in the nine Katana files. The only errors in the run are 4 pre-existing baseline errors in item/DbaItems.java and inventory/DbaMenus.java, which are not Katana files, are unmodified by this pass, and are pulled in only as sourcepath dependencies.
- 26.3 (cp263.txt): zero errors in the nine Katana files. The only errors in the run are in other files (DbaItems, DbaMenus, and several entity files belonging to other weapons whose Phase 2 conversions were in flight during the run).
- All four renderer classes, the item class, and all four entity classes were emitted to the output directories on both classpaths.
- Two real defects were caught by the first verification round and fixed before the clean run: (1) PoseStack.mulPose(Quaternionf) from Axis.rotationDegrees does not exist on the 26.3 classpath, so all added rotations use the codebase's cross-version form, mulPose(new Matrix4f().rotation(...)); (2) the lifecycle alpha in the two charge-state renderers is reassigned by the fade-out block, so lambdas capture a final alias (alphaF) instead. Neither fix changes any visual value.
- Grep verification after conversion: zero SoundEvents, zero ParticleTypes, zero DustParticleOptions, zero damageSources().playerAttack, zero addVertex, zero KiRenderHelper primitive draws, and zero local draw helper definitions across the nine files. No em dashes in any touched file or in this report.
- This is a javac audit, not a Gradle build; per the standing rule for this repo, no build pass is claimed.

## Shared change requests (toolkit gaps, not worked around privately)

1. No beam builder. The aim guide's beam was an 8-sided tube; BespokeGeometry has no straight beam or tube builder, so the beam is now crossed tornRibbon sheets (the standard flat-beam read). A dedicated seeded beam builder with end taper and edge jitter would serve the aim guide and any future guide or beam weapon better than re-deriving crossed sheets per renderer.
2. No flat ground ring builder. bladeArc at sweep 360 makes a thick band whose inner radius is half the outer, and brokenVortexBand is a vertical wall. The charge stance rings use brokenVortexBand squashed to 0.08 to 0.10 height. A dedicated flat broken ring (a ring band version of glowFan's jittered rim, not center filled) would fit focus rings and telegraph circles across weapons.
3. shardFan centers at the pose origin only. A multi-spark field (the charge stance's converging motes, up to about 18 motes) therefore needs one submit per mote. A scatter variant that takes precomputed offsets, or a dedicated mote field builder, would cut submit counts for every charge stance effect.

## Honest worse check

- Crescent silhouette: this is the largest visual change. The old crescent was a thin bowed strip (0.24 thick at the belly) with a fading wake; the new one is a fuller C-shell bladeArc band around the target (band spans roughly 1.15 to 2.5 blocks at base scale). It reads chunkier and less like a hairline cut. The wake ribbon, the white core, the colored lip, the variant mirror, and the strike-timed belly spark all survive, and damage, timing, and colors are unchanged. Not reverted: the thin strip is precisely the hand-rolled regular geometry this pass exists to remove, and bladeArc is the builder the task specifies for crescents. Flagged here so the call is visible.
- Splitter wall: tornRibbon has no bottom-to-top alpha gradient, so the wall's ground edge is harder than the old sheet, which faded in from alpha 0 at the ground. The mantle and core layering, the torn edges, and the midpoint anchor glow soften it, but at close range the bottom edge is a visible line. The white core is also vertically centered now where the old core was bottom aligned; it reads as a bright spine either way. The fracture ribs are jagged branch bolts instead of clean triangles: more like cracked reality, less tidy. The along-length alpha gradient direction matches the old wall (bright at the far end) by construction.
- Charge rings: a squashed brokenVortexBand reads as a broken rim of light segments rather than a clean thin ring. That break-up is the point of the bespoke pass, but it is visibly chunkier at close range. The mote shard fans are single-plane (XY) where the old diamond sparks had a second crossed plane, so a mote seen edge-on nearly vanishes for a frame; the spiral motion makes this brief.
- Aim guide beam: crossed flat sheets replace a true tube. From most angles the guide reads the same or better, since one sheet always faces the viewer, but looking exactly down the beam axis the line nearly vanishes (two thin sheet edges) where the tube still showed a ring of faces. The start ring at the beam origin partially covers the first person case.
- Sounds: every swap is a same-role bespoke sample at the identical volume and pitch formula as the vanilla original, so no call gets louder, quieter, or mistimed. The ANVIL_LAND judgment call above is the only swap that deviates from the guide's generic row, and it is flagged there.
- Nothing was reverted wholesale: no conversion made a mechanic, a timing, or a damage value worse, and the visual regressions above are silhouette changes inherent to the mandated builders, recorded here rather than hidden.
