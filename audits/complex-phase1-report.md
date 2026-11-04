# Complex Effects Redo: Phase 1 Report

Phase 1 of the complex effects redo, on top of dba d74f13a, Nov 4, 2026. Carter's verdict on the no-shapes result was that the effects were just round glowing particles; he confirmed the layered target (complex pieces, complex shapes, complex particles) against a reference picture. This phase built the binding standard, the fresh work order, and the shared foundation. No effect was converted in this phase; Phase 2 does the thirty-one owner conversions against this foundation and is judged by the gates in audits/COMPLEX-STANDARD.md.

## What was built

### The standard and the work order

- audits/COMPLEX-STANDARD.md: the layered composition rules (pieces layer, complex particle layer, material filler layer), effect classes S, M, and L, the per-builder complexity bar, shader discipline, texture and sprite requirements, performance budgets, and the six merge gates for Phase 2. Where it conflicts with the no-shapes presentation rule it supersedes it; every other standing rule (themes, colors, mechanics, ownership, verification) still governs.
- audits/complex-audit.md: every weapon and system effect re-derived at d74f13a by counting the current MaterialEmitters mixes per renderer, with the pieces layer and complex particle layer each effect must gain, its class, and its Phase 2 owner. Thirty-one owner slots, the same structure as the no-shapes pass. Two deliberate exemptions are recorded in it: Bleeding and Melting status effects (no complex sprite silhouette matches fluid or corrosive effects) and the Otherworld cloud field (a background sky system, not an effect body).

### The pieces toolkit: client/render/ki/ComplexGeometry.java

Seeded builders (splitmix RNG, same family as the codebase's other seeded generators), all taking seed, size, color or gradient, and life inputs, all composing with FxEnvelope for distance scaling, all returning the vertices emitted:

- branchedLightning: iterative midpoint displacement on the trunk (3 to 5 levels by distance, amplitude 0.16 of length decaying 0.52 per level), 2 to 6 forks and 0 to 2 sub-branches per fork placed from the trunk and fork polylines, three branch orders with width factors 1.0, 0.5, and 0.27, per-segment width jitter and taper, sheath and core passes, crossed ribbon pairs per segment. Worst case 121 path segments and about 3,900 vertices inside the standard's 130 and 4,200 budgets.
- shard and shardBurst: closed irregular spindles, three control rings of 5, 6, and 5 vertices interpolated at 8 steps per band, per-vertex radial displacement 0.72 to 1.27, root-to-tip gradient, per-face facet brightness 0.68 to 1.18. One shard emits 48 triangles, 288 vertices double-sided. shardBurst mixes three size classes (about 55, 30, and 15 percent) with per-shard seeds, directions, and origin jitter.
- fracturePlates: 6 to 10 angular plates with jittered spans, about 15 percent skipped for gaps, independent corner heights with seeded drop-outs, raised or sunken centers, skirts on three sides, hot-to-dark radial gradient. About 480 vertices at the full count, inside the 1,600 budget.
- crackFan: 6 to 11 wandering cracks of 4 to 6 segments with seeded width pinches, 35 percent carrying one shorter fork, hot center cooling to the theme color. The fork origin is recomputed by deterministic replay of the walk's RNG consumption; that coupling is documented in the code and in this report's risks. About 790 vertices worst case, inside the 900 budget.
- tendril and tendrilFan: quadratic bezier spine at 12 to 24 samples, 3D curl offsets from two perpendicular sine and noise fields, width swell with two seeded pinch windows collapsing to 15 percent, crossed ribbon pair twisting 1.5 to 3.5 radians, sampling the tendril canvas texture with u along the spine. About 370 vertices per tendril.
- flameTongues: 3 to 9 tongues of 6 segments on alternating radial and tangential planes, caller-phase sway growing toward the tip, seeded lean, width swell and noise pinch, and a final segment pulled sideways with its width collapsed to 25 percent so the tip is torn in the mesh, sampling the flame canvas with v running base to tip. About 430 vertices at the full count.

Submit helpers so Phase 2 owners reach the shader in one call: submitEnergy and submitVeil (DbaShaders additive and alpha types, with texture overloads), and submitSolid (the plain ki type for faceted solid pieces). The shader-bound builders apply the toolkit's life remap internally (peak alpha times envelope, boosted 1.45 and clamped), so a piece submitted at an honest peak alpha reads as a body instead of dissolving at spawn. This pass is the first real adoption path for DbaShaders, which the no-shapes pass built and then used nowhere.

### The complex particle layer

Design decision, made from the code: the five complex sprite types are registered as separate SimpleParticleTypes in DbaParticles (SHARD_CHIP, SPARK_STREAK, WISP_CURL, EMBER_FRAGMENT, ASH_FLAKE) instead of extending DbaMaterialOptions. The material record is capped at eight material ids and carries a codec and stream codec; the complex sprites are physical-material sprites with fixed style colors that need no per-particle tinting, so separate types keep every synced field and the material codec untouched. Owners emit them through the existing toolkit with no new machinery: MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP, ...) or MaterialEmitters.burst, or addParticle for the common-side status effects.

- Styles live in DbaGlowParticle as per-type records and are registered in DbaParticleProviders: shard chips fall and tumble in stone gray; spark streaks are short additive slivers; wisp curls rise slowly, pale and additive; ember fragments glow orange-red with a dark crust read from the texture; ash flakes drift down in gray, alpha blended.
- Textures come from tools/gen_complex_assets.py (the generator of record): five sets of eight 128 px frames in textures/particle with their particles/*.json, plus the two geometry canvases flame_tongue.png and tendril.png in textures/entity/fx. No set is a radial-gradient disc; each set varies its silhouette per frame (jagged faceted polygons, rotated tapered streaks, spiral stamp curls, crust-and-core ember chunks, clustered ash stamps). The sets were checked on contact sheets during generation; two sets (ember fragments, ash flakes) and both canvases were revised once after the first sheet read too faint or too soft.

### Untouched, deliberately

- MaterialEmitters and DbaMaterialParticle: unchanged in behavior; they are the filler layer the standard now defines.
- The destruction wiring from d74f13a (PermanentDestruction, lava settling, retired GroundScar): not touched.
- BespokeGeometry and LayeredRender: still deleted, still unreferenced. ComplexGeometry is a new toolkit at a higher complexity bar, not a restoration.
- No damage number, cooldown, cost, timing, mechanic, theme, color, or synced data field changed anywhere. The only common-side edit is the five new particle type registrations in DbaParticles.

## Verification

- Targeted javac of the new and edited files first, on both classpaths: zero errors in ComplexGeometry, ComplexTextures, DbaParticles, DbaGlowParticle, and DbaParticleProviders on 26.2 and on 26.3. The only errors in those closures are the documented baseline kinds in DbaItems, DbaMenus, and (26.3) the hostile entity files.
- Dual full sweeps with the javac audit harness: baseline worktree at d74f13a measured fresh at 132 error kinds (26.2) and 138 (26.3); the Phase 1 tree measured 132 and 138; comm -13 printed nothing on both versions, and comm -23 also printed nothing, so the kind sets are identical to baseline.
- Resources: all 566 resource JSONs parse, including the five new particle JSONs; every particle texture reference in the tree resolves to a real file, including all forty new sprite frames; both canvas textures exist at the paths ComplexTextures declares.
- Hygiene: git diff --check clean; added-lines em dash scan zero (checked over the diff and every new text file, byte-level).

## Honest gaps and risks for Phase 2

- In-game look is unverified. No Gradle build can run in this VM, and no effect has been seen in game with the new toolkit. The foundation is compile-verified and budget-verified only. Carter's PC test is the first real look, as with every pass.
- The canvas textures are the softest part of the foundation. On contact sheets the flame tongue still has soft edges; the design leans on the mesh tearing its own tip and on the shader's fbm erosion to cut it further. If Phase 2 owners find the flame reads soft in context, the fix is a generator revision (harder threshold, deeper tears), recorded per owner report.
- DbaGlowParticle limits apply to the complex sprites: particles have no ground collision (shard chips fall through the floor plane while fading), and roll advances at one fixed rate for every style, so chips do not tumble at varied rates and spark streaks do not align to their velocity. They are small sprites by design; the pieces layer carries the structured read. If Carter wants velocity-aligned streaks, that is a particle class change, out of this phase.
- crackFan's fork placement replays the walk RNG to recover the fork origin. If a future edit changes crackWalk's RNG consumption order, the replay must change with it or forks will detach from their cracks. The coupling is commented at both sites.
- Budget honesty: the shard's real vertex count (288) forced one correction while writing the standard; the budget table now carries the measured number and a 12-shard guidance for combined class L compositions. Owners should trust the builders' returned vertex counts over estimates when composing.
- The Bolt conversion in Phase 2 (Shared weapon systems owner) is the flagship and the riskiest single conversion: BoltRenderer currently turns its seeded path into emission; drawing branchedLightning from that path must keep the strike's timing, colors, and ground behavior identical, with emission demoted to filler.
