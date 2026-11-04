# No-Shapes Realism Standard

Binding for the DBA no-shapes realism pass. Measured and written at dba d9f8a90, Nov 4, 2026. This standard supersedes the bespoke pass geometry rules for effect visuals. BESPOKE-STANDARD.md and SPECTACLE-STANDARD.md still govern themes, colors, damage, cooldowns, costs, timing, mechanics, ownership, and verification wherever they do not conflict with this document.

## The rule

Removing basic primitives is not enough. No visible shape may be the basis of an effect. Effects must look realistic and must not read as plain 2D or 3D shapes.

The bespoke pass geometry toolkit (tornRibbon, shardFan, bladeArc, displacedDome, brokenVortexBand, glowFan, branch, crackStar, plume) is deprecated for effect visuals, including its irregular and seeded forms. An irregular ribbon is still a ribbon. A noisy dome is still a dome. Making a shape rougher does not convert it.

The replacement is material, texture, particle, shader, and simulation driven presentation:

- An effect is made of a material: plasma, flame, smoke, vapor, dust, embers, ash, mist, souls, darkness, glass dust, lightning plasma, rain, or a physical material such as stone, slime, or magma crust.
- The material is carried by detailed flipbook sprites, particle behavior (turbulence, drag, buoyancy, gravity, spin, density fade, ground response), light and post effects, sounds, and real blocks or physical models where a real object exists.
- Emission points, paths, and volumes are data. Sampling particles along a beam path or inside a barrier volume is required and is not drawing geometry, as long as no mesh, ribbon, shell, or silhouette is rendered from that data.

## Scope

In scope: every effect surface inventoried in audits/no-shapes-audit.md. That is all 22 weapons, shared weapon trails and lightning, status effects, Ki techniques, the wish ritual, mob ability effects, storms and AtmosphereClient, TimeShatter, the Evil Spear dread shroud, and form auras.

Out of the geometry gate, as physical objects: weapon models, race and mob models, detailed projectile models, debris block models, dragon balls, the Flying Nimbus model, blocks, and the detailed dread eye sprites. Their energy, fire, smoke, light, and magic are in scope wherever a separate effect surface draws them. A physical fragment effect (a blade shard, a trident shard) must be a detailed physical model or actual material chunks if it remains visible as an object; if it was only a glowing mesh, it becomes particles.

Presentation layer only. Damage numbers, damage types, cooldowns, Ki costs, timing, durations, hitboxes, movement, and mechanics do not change in this pass. If a conversion truly cannot be done without a mechanic change, the owner stops on that piece and reports it instead of changing the mechanic.

## Foundation (Phase 1)

Phase 2 builds only on the shared foundation. No owner invents a private particle system, a private texture style, or a private emitter.

- Materials: DbaParticles PLASMA, SMOKE, VAPOR, DUST, MIST, SOUL, DARKNESS, and GLASS_DUST, all carrying DbaMaterialOptions (material, color, scale, seed). Their client behavior lives in DbaMaterialParticle profiles: turbulence, drag, buoyancy or gravity, spin, velocity-responsive scale, density fade, and ground response.
- Existing types stay live: FLAME, EMBER, and ASH keep their registry names and call sites, with new 128 px eight-frame detailed flipbooks. DUST_PUFF, KI_MOTE, WISP, VOID_SMOKE, WIND, RAIN, THEME_DUST, BLOCK_CHUNKS, and the rest are unchanged except where an audit row names them.
- Textures: tools/gen_no_shapes_textures.py is the generator of record for the material sets. Sprites are 128 px, eight frames, built from fractal noise, cellular breakup, tendrils, and eroded alpha. Soft round blobs are not a material texture.
- Emission: client/fx/MaterialEmitters provides Preset, Source, burst, and Emitter. Sources supply points along paths, inside volumes, and across ground areas as data. Continuous emitters are frame-rate independent, density-scaled, distance-scalable, and hard-budgeted per second.
- Split source sets: DbaMaterialOptions and DbaParticles live in the common source set and reference no client classes. DbaMaterialParticle, DbaParticleProviders, and MaterialEmitters live in the client source set. Server code may send the simple and complex particle types it already sends; it may not reference the client toolkit.

## Replacement method by effect class

- Beams: dense particle emission along the path from MaterialEmitters.path, a brighter plasma core near the source, an impact plume of plasma, smoke, and sparks at the terminus, and post or particle distortion at heavy impacts. No tube, ribbon, helix, or crossed plane.
- Slashes and waves: swing-driven noisy trails. Emission follows the real weapon or entity movement path and dies fast; add vapor, sparks at contacts, and dissolving plasma. No bladeArc, crescent mesh, or ribbon.
- Explosions: animated flame and smoke flipbooks, embers, physical debris, ground dust, a light flash through existing glow particles or post shockwave, and lingering smoke. No dome, sphere, blast ring mesh, or glow fan.
- Barriers, fields, and domains: a volume of drifting material motes sampled from MaterialEmitters.sphereVolume or volume, a noise-eroded film suggested by sparse large vapor or plasma sprites at the boundary, and hit ripples as particle bursts at the hit point. No displaced dome, shell mesh, or band stack.
- Storms and vortexes: particle forces and emitters. Rain, mist, vapor, and dust move around the center; lightning comes from the Bolt conversion. No vortex bands, rings, or column meshes.
- Rifts and portals: inward-streaming particles from a volume source, darkness or void plasma, irregular crack textures as ground or wall decals where the owning effect already marks a surface, and distortion through the existing post shockwave where a screen effect honestly fits. No shard fans, crack stars, or torn ribbons.
- Lightning: the seeded branch path stays as data. Render it as layered glow sprites and spark particles emitted along the path, with a flash and impact sparks. No branch ribbons or polyline strips.
- Ground effects: decals plus particles plus real blocks. Crack and scorch decals use irregular generated textures on the ground surface; dust, embers, and debris carry the motion; temporary blocks carry anything the player can stand in or on. No glow fans, discs, rings, or crack stars.
- Energy projectiles: dense particle clusters with material trails and impact plumes. A projectile that is a physical object keeps or gains a detailed model; a projectile that is pure energy has no shell mesh.
- Slams and shockwaves: ground dust, vapor, physical debris, crack decals, and the existing post shockwave for heavy hits. No shockwave dome, ring, or plume mesh.
- Charges, stances, and auras: material motes pulled toward or rising from the caster, vapor at the rim, and density tied to the existing lifecycle envelope. No aura shells, bands, or fans.
- TimeShatter: glass dust particles suspended and drifting against the flow, irregular crack textures, temporal plasma, and chromatic distortion from the existing post chain family. No dial, spoke, shard, or core geometry.
- Dread shroud: darkness smoke and vapor around the victim from a volume source. The detailed eye sprites stay detailed sprites; the shroud bands do not.

## Shader findings

The mod's shader surface today is screen-space post chains (DbaPostFx with six fragment shaders) plus vanilla RenderTypes factories for world rendering. There is no custom world-space RenderPipeline or shader registration pattern in the code, and the particle pipeline that does exist (the additive particle layer) is built through DbaPipelineCompat precisely because the pipeline classes moved between 26.2 and 26.3.

Therefore:

- Animated noise erosion is delivered through flipbook sprite animation and density fade, which is honest and version-safe. No owner may claim a noise-eroded shader render type.
- Distortion is delivered through the existing post shockwave chain for heavy screen-space impacts. Local world-space refraction is not feasible in the existing pattern and is not part of this pass. No owner may fake it with a translucent shape.
- If a future pass registers a real custom render pipeline on both versions, this section can be revisited. Until then, materials and particles carry the realism.

## Hard completion gates

The pass is not done until all of these hold in the integrated tree:

1. Zero BespokeGeometry calls in effect renderers, including builder calls and Rng references. BespokeGeometry itself may remain only if some non-effect caller remains; the merge decides deletion from the final grep, not from preference.
2. Zero LayeredRender geometry calls (drawVortexBands, drawAnchorGlow) in effect renderers.
3. Zero direct addVertex effect drawing and zero KiRenderHelper.vertex calls in effect code outside the deprecated toolkits themselves.
4. Every row of audits/no-shapes-audit.md is converted to its named material, or is explicitly carried in the final audit as not converted with a reason Carter can judge. Silence is not a disposition.
5. No new private geometry helper, private particle system, or private texture style appears in any owner's files.
6. Dual javac audit sweeps show zero new error kinds against the pass baseline on both 26.2 and 26.3.
7. All new and changed JSON parses; every particle JSON texture reference resolves to a file on disk.

## Working rules

- One owner per weapon or system, exactly as listed in the audit. Owners edit only their files. Shared foundation gaps go in the owner's report as change requests; the coordinating pass extends the foundation once, centrally.
- Read the effect before converting it. Theme, colors, lifecycle envelope, synced seeds, and distance behavior carry over. The material changes; the identity does not.
- Revert on worse: if a conversion reads worse than the geometry it replaced and cannot be fixed within the owner's scope, revert that piece and say so in the report. A missing effect is a reportable outcome; a worse effect shipped silently is a failure.
- Performance is part of realism: use the Emitter budgets, FxEnvelope distance scaling, and the effects density config. An effect that only works at unlimited particle counts is not converted, it is broken.
- No em dashes in code comments, reports, audits, or commit messages.
- Verification is the javac audit sweep. Nobody claims a Gradle build or an in-game look from this environment.
