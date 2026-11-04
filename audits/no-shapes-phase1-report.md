# No-Shapes Realism Pass: Phase 1 Report

Coordinating Phase 1, measured and built at dba d9f8a90, Nov 4, 2026. Deliverables: audits/no-shapes-audit.md (the Phase 2 work order), audits/NO-SHAPES-STANDARD.md (binding), and the shared foundation below. Phase 1 changed no damage, cooldown, cost, timing, duration, or mechanic anywhere. BespokeGeometry and LayeredRender were not deleted; Phase 2 still calls them until each owner converts, and the standard marks both deprecated for effect visuals.

## Audit counts (fresh at d9f8a90)

| Surface | Count | Files |
|---|---:|---:|
| BespokeGeometry builder calls in effect code | 684 | 90 |
| BespokeGeometry builder calls inside LayeredRender itself | 3 | 1 |
| LayeredRender geometry calls (drawVortexBands, drawAnchorGlow) | 44 | 29 |
| LayeredRender.distScale references (utility, not geometry) | 115 | 85 |
| Direct addVertex calls in effect renderers (HollowCharge, HollowRushTrail) | 40 | 2 |
| KiRenderHelper.vertex calls in effect code (SwingTrail, Bolt, DreadGaze) | 30 | 3 |
| Distinct files with visible effect geometry | 94 | 94 |

Per-builder calls in effect code: brokenVortexBand 161, tornRibbon 100, displacedDome 84, shardFan 78, bladeArc 70, branch 60, glowFan 48, crackStar 45, plume 38.

Also inventoried with zero geometry: 28 status effect classes (particle presentation only; EvilDread cross-listed under Dread), the mob ability presentation rows (Frieza hit feedback, Saibaman burst, boss stagger), AtmosphereClient storm rain and gloom, the Otherworld cloud field, and the form aura emitter. Checked and exempt as physical objects: DebrisRenderer block models, weapon models, race and mob models, the Flying Nimbus model. KiRenderHelper still defines five primitive methods (box, cylinder, disc, sphere, helix) with zero call sites; they draw nothing and stay deprecated.

## Material inventory summary

Every one of the 94 geometry files has a row in the audit with a binding material assignment, grouped by owner. The pattern by theme: Azure becomes tempest plasma, rain, mist, and lightning plasma; Dabura and the dread shroud become darkness smoke and soul vapor; Cursed Blade becomes curse plasma, darkness smoke, lightning plasma, and ground dust; Hollow's Edge and Sickle become soul wisps, void plasma, and darkness smoke; Lava becomes flame flipbooks, magma embers, smoke, ash, and vapor over its real crust blocks; Ox and War Mace become ground dust, physical debris, and shock vapor; Ki techniques become technique-colored plasma with vapor and impact dust; the wish ritual becomes golden plasma motes, soul wisps, and mist; TimeShatter becomes glass dust, temporal plasma, and chromatic distortion. Beams, barriers, rifts, storms, ground effects, and projectiles each follow the replacement method for their class in the standard.

## Foundation: textures

New generator: tools/gen_no_shapes_textures.py. It writes 128 px, eight-frame sets built from fractal noise, cellular breakup, ridged filaments, tendrils, domain wobble, and eroded alpha, with a stable per-set seed so regeneration is reproducible.

- New sets: plasma, smoke, vapor, dust, mist, soul, darkness, glass_dust.
- Upgraded in place (same registry names, same call sites): flame, ember, ash, dust_puff, from 64 px four-frame soft sprites to the 128 px eight-frame detailed sets.
- Every set has its particle JSON written by the same generator. Spot checks by eye: plasma reads as a bright filamented burst, flame as a warped licking plume, glass dust as fine suspended flecks after a density fix in the generator (an early draft read as snow and was corrected before landing).

## Foundation: particle types and behaviors

New common-side types in DbaParticles, all carrying the new DbaMaterialOptions record (material, color, scale, seed), registered as plasma, smoke, vapor, dust, mist, soul, darkness, and glass_dust. DbaMaterialOptions resolves its own registered type from the material id, so server-sent and client-spawned particles route correctly.

New client behavior in DbaMaterialParticle, one profile per material:

- Turbulence: seeded sinusoidal drift on all three axes, rate and amplitude per profile.
- Drag and buoyancy or gravity per profile: smoke, vapor, and souls rise; dust settles; glass dust falls fast.
- Spin per profile, fastest on glass dust.
- Density fade: smoothstep fade-in and a powered fade-out against a per-material alpha ceiling (mist stays faint, darkness stays dense).
- Ground response using the engine collision flag: smoke, vapor, dust, and darkness settle or creep on ground contact; glass dust bounces once at a third of its impact speed; mist hugs the ground.
- Velocity-responsive scale: profiles scale the sprite with particle speed, strongest on plasma. Honest limit: SingleQuadParticle draws camera-facing quads, so this is responsive scale, not true anisotropic stretching of the sprite along its velocity. Streak-shaped flipbooks (the existing rain set is the pattern) remain the tool where a literal streak is needed.
- Lighting: emissive materials (plasma, soul, glass dust) render full bright; smoke, vapor, dust, mist, and darkness take world light, so smoke no longer glows in the dark.

Providers are registered in DbaParticleProviders alongside the existing ones. Every pre-existing particle type, sound, and system is untouched in behavior; only the four texture sets named above changed pixels.

## Foundation: emission toolkit

New client class client/fx/MaterialEmitters:

- Preset: a material or fixed particle look with base velocity and spread. Factories exist for all eight materials plus flame, ember, and ash.
- Source: emission points as data. point, path (straight and polyline, with path-following sample velocity), volume, sphereVolume, and groundArea.
- burst: one-shot emission for impacts and casts, density-scaled.
- Emitter: continuous emission with a wall-clock accumulator, so the rate is frame-rate independent; a hard per-second budget; no emission debt after a capped frame; per-particle seed progression so material phases vary inside one stream.
- distanceScale helper reads the local player distance through FxEnvelope, matching the existing distScale pattern.

## Shader findings (stated plainly)

- No custom world-space shader or RenderPipeline registration pattern exists in this code. World rendering uses vanilla RenderTypes factories; the one custom pipeline (additive particles) is reflective compat code because the pipeline classes moved between 26.2 and 26.3.
- An animated noise-eroded world render type was therefore NOT built. Erosion is delivered through flipbook animation plus density fade. Claiming a shader here would be faking it.
- Local world-space distortion or refraction was likewise NOT built and is not feasible in the existing pattern. Distortion support in this pass is the existing screen-space post shockwave chain in DbaPostFx, which Phase 2 may trigger for heavy impacts under the standard.
- Both items are recorded in NO-SHAPES-STANDARD.md so no Phase 2 owner promises them.

## Verification

- Baseline worktree at d9f8a90, private sweep tags noshapes991-base262 and noshapes991-base263: 132 unique error kinds on 26.2, 138 on 26.3.
- Final tree, tags noshapes991-final262 and noshapes991-final263: 132 kinds on 26.2, 138 on 26.3. comm -13 against baseline is EMPTY on both versions: zero new error kinds.
- Targeted javac of the five new or edited Java files on the 26.2 classpath before the full sweeps showed only the known baseline DbaItems and DbaMenus error kinds, none in foundation files.
- All 561 JSON files under src/main/resources parse. All 144 particle texture references across all particle JSONs resolve to files on disk. All twelve generated sets verify at eight frames of 128 by 128.
- Em dash scan of the Phase 1 added lines: zero (checked at commit time over the staged diff).
- The baseline worktree was removed and pruned after the sweeps.
- Javac audit only. No Gradle build was run and no in-game look is claimed; the first visual judgment is Carter's PC test after Phase 2.

## Phase 2 owner list (from the audit)

Twenty-two weapon owners: Katana, Z Sword, Saber, Brave Sword, Grand Sword, Azure Dragon Sword, Dabura's Sword, Cursed Blade, Hollow's Edge, Dimensional Sword, Spirit Sword, Evil Spear, Devil Trident, Sickle of Sorrow, Ox King's Ax, Power Pole, Whis Staff, Bansho Fan, Blaster Gun, Lava Sword, Spike Flail, War Mace.

Plus nine system owners: Shared weapon systems (SwingTrail and Bolt), Status Effects, Ki Techniques, Wish Ritual, Mob Ability Effects, Storms and AtmosphereClient, TimeShatter, Evil Spear Dread, and Forms and Player Auras.

Thirty-one owner slots in total. Each converts only its audited files, builds only on this foundation, and reports per-effect before and after counts against the audit rows.
