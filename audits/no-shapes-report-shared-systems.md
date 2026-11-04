# No-Shapes Realism Pass: Shared Weapon Systems Report

Owner: Shared weapon systems (SwingTrail and Bolt), Phase 2. Measured and converted at dba ad22b32. Files edited, and only these: src/client/java/com/dragonblockarcanedba/client/fx/SwingTrailRenderer.java and src/client/java/com/dragonblockarcanedba/client/render/BoltRenderer.java. No commits. No shared toolkit file was edited, and no weapon file was touched.

## Before and after counts

Counts follow the audit definition: V is direct KiRenderHelper.vertex calls in effect code.

| File | V before | V after |
|---|---:|---:|
| SwingTrailRenderer.java | 8 | 0 |
| BoltRenderer.java | 14 | 0 |
| Owner total | 22 | 0 |

Grep proof on the converted files: zero KiRenderHelper references of any kind, zero BespokeGeometry references, zero LayeredRender references, zero addVertex calls, and zero submitCustomGeometry calls in either file. Neither file draws anything anymore; both only emit particles through client/fx/MaterialEmitters.

## SwingTrailRenderer conversion

What it was: a LevelRenderEvents custom geometry callback that connected the recorded swing samples into two ribbons per segment, a theme colored blade surface and a near white core along the leading half, alpha driven by the profile fade formula.

What it is now:

- The LevelRenderEvents registration and the ribbon code are deleted. register() now registers only the existing client tick.
- The sampling half is byte-for-byte the old behavior: same basis math, same SUBSTEPS of 4, same ARC, FAN, and THRUST sample math from the profile, same sample cap and pruning, same fade window. The samples are now emission data only.
- Each trail owns three MaterialEmitters.Emitter accumulators, created per profile and per client level: theme plasma across the whole blade, near white plasma along the leading half (blade fraction 0.5 to 1.0), and theme vapor across the whole blade. Particle colors come from the existing profile record: the plasma and vapor use the profile color, and the core uses the old core formula, the profile color pushed 70 percent toward white. Particle scale derives from the profile reach.
- The emission source samples a segment between two recorded swing samples and a point across the blade between hilt and tip, and reports the segment motion as a small source velocity, so the material keeps moving the way the swing moved.
- The emission envelope is the old ribbon alpha: the strongest surviving sample alpha under the unchanged smoothstep fade formula, times the profile alpha, distance-scaled through MaterialEmitters.distanceScale. Emission stops when the samples prune, exactly when the ribbon used to stop.
- FLASH profiles: the muzzle spark burst now goes through MaterialEmitters.burst with the same count of 5, the same forward velocity of 0.25, and the same spread of 0.06, and the recorded muzzle to end sample feeds the same trail emitters as a single hilt to tip line.
- Tip particles: the profile Tip switch now builds a MaterialEmitters.Preset per kind (SPARK and VOID_SMOKE and WIND as fixed presets on the existing types, EMBER through the toolkit ember preset) and fires a one count burst at the same tip position, in the same attack animation window of 0.3 to 0.8, with the same upward drift of 0.03.

Unchanged: WeaponTrailProfiles itself (read-only for this owner), the profile table contents, all colors, reach, sweep, fade ticks, alpha, kinds, and tip assignments. The renderer public API is unchanged: register() has the same signature, so the client init call site compiles as before.

## BoltRenderer conversion

What it was: crossed ribbon geometry per branch segment in two passes (theme sheath, near white core) plus two flat ground discs, all drawn through KiRenderHelper.vertex in a submitCustomGeometry callback, alpha from FxEnvelope times a per tick seeded flicker.

What it is now:

- The geometry callback, the ribbon and quad helpers, and the disc helper are deleted. submit() now emits particles and then calls super.submit as before.
- The branch path is built by the exact old random walk, consuming the synced seed in the same order: the 12 step jittered trunk climb, then 2 to 4 forks leaving the trunk and diving outward, with the same taper values. The path is data only; BoltEntity and its synced seed, color, length, and life are untouched, so every client emits along the same trunk and forks and the strike behavior is identical.
- Segments are grouped into two taper bands, strong (taper at least 0.72: most of the trunk) and weak (the trunk top and all forks). Each band gets a length-weighted segment source and two plasma layers: a theme colored sheath (scale 1.55 strong, 1.12 weak) and a near white core (scale 0.85 strong, 0.58 weak; the core color is the old formula, the theme color pushed 75 percent toward white). Rates scale with total path length relative to the bolt length, and the per-particle seed offsets differ per layer so the layers do not move in lockstep.
- The ground discs are replaced by plasma sprites from MaterialEmitters.groundArea at the same two radii, 1.6 theme colored and 0.7 white, at the same height of 0.05 above the strike point.
- Impact sparks are DbaParticles.SPARK through a fixed preset at the strike point, with an upward bias and wide spread.
- Timing: the strength fed to every emitter is the old alpha product, FxEnvelope.envelope(age, life, 0.12, 0.45) times the same per tick flicker (a Random reseeded from seed times 31 plus the whole tick, 0.65 to 1.0), times FxEnvelope distance scaling.
- Emission plumbing: render states are rebuilt every frame, so the toolkit emitters live in a per-renderer map keyed by entity id. Each entry is validated against the synced seed, color, length, life, strike position, and last seen age; a mismatch rebuilds it. The first submit of a strike fires one burst along the whole path so the bolt is present at once, later submits feed the emitters by the age delta, so emission is frame-rate independent. Entries that stop being submitted are dropped after two seconds, and an entry is removed when its bolt reaches its life.

Unchanged: the renderer constructor, both shouldRender overloads, createRenderState, extractRenderState, and submit signatures. BoltState dropped the r, g, b floats and gained entityId and a raw color int; a tree-wide grep confirms nothing outside this file ever referenced BoltState or its fields.

## Verification

Javac audit only, no Gradle claim. Targeted javac with the full sourcepath closure (src/main/java plus src/client/java) on both audit classpaths, with the two owned files as roots:

- 26.2 (cp262.txt): zero errors naming either owned file; both classes emitted. The only closure errors are the 4 known baseline lines in DbaItems and DbaMenus.
- 26.3 (cp263.txt): zero errors naming either owned file; both classes emitted. The only closure errors are the 10 known baseline lines: DbaItems 2, DbaMenus 2, DbaHostileEntity 2, PatrolRobotEntity 2, FriezaSoldierEntity 1, NamekianWarriorEntity 1.

Call-site compatibility sample: the same compile was rerun with AzureLightningRenderer added as a third root (the weapon renderer whose entity strikes through the shared BoltEntity). On both classpaths it produced zero errors in the sample file and in both owned files, and all three classes were emitted, with only the same baseline closure errors above.

Also checked: git diff --check is clean on both files, and the added-lines em dash scan is zero.

## Honest worse check

Nothing was reverted, but these trades are real and are Carter's to judge on the PC test:

- Swing trail surface continuity: the ribbon was one continuous surface; the trail is now dense particles sampled across the same blade area. At default density the blade reads covered, but at reduced density settings or long distance it can read as separate motes where the ribbon stayed unbroken.
- Swing trail timing tail: emission follows the old fade window exactly, but the material sprites dissolve on their own profiles after emission stops (plasma 10 to 22 ticks, vapor 24 to 54), so the last wisps outlast the old fadeTicks cutoff, which for the katana profile was 5 ticks. The trail reads softer and slightly longer than the ribbon did.
- Tip and flash counts at reduced density: the old code spawned its fixed counts whenever density was above zero; the toolkit burst density-scales them, so low density settings now get lighter tip and muzzle feedback than before. At default density the counts match.
- Bolt continuity: same point as the trail. The core emitter rate is set so the seeded path reads continuous at default settings, but the bolt is sprites along a path, not an unbroken surface, and gaps can show at distance.
- Bolt taper is stepped, not continuous: the old width tapered per segment; the conversion has two taper bands with two sprite scales, so the falloff from trunk to fork tips happens in one step instead of smoothly.
- Bolt afterglow: plasma sprites live 10 to 22 ticks, so a strike leaves a brief fading afterglow after the bolt entity dies, where the old envelope ended exactly with the entity. The standard names a brief plasma afterglow as the goal for lightning, but it is a visible change from the old timing.
- Cost: a bolt now costs a few hundred short-lived particles over its life instead of two ribbon passes, and a swing costs a few dozen. Budgets in the toolkit emitters and distance scaling bound this, but simultaneous storm bolts are the most expensive case and the one to watch in game.

## Change requests

None. Both public surfaces (SwingTrailRenderer.register and the BoltRenderer constructor and state flow) kept their signatures, so no weapon or call-site file needs an edit to compile against the converted renderers.
