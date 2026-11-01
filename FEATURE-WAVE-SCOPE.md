# DBA Feature Wave Scope (Nov 1, 2026)

Source: Carter's Nov 1, 2026 direction for this chat. This wave is DBA ONLY (not the Mega Mod). Order below is the build order, grounded in the Nov 1 code audit at HEAD 80a2b98 (365 Java files). Carter declined the Windows compile check on Nov 1, 2026 ("just audit after and b4"), so Phase 0 is closed as waived: every phase is verified by the before and after audit method in BUILD-VERIFY.md instead of a build gate, and no audit is ever reported as a compile pass.

Rules that bind the whole wave:
- Version floor is Minecraft 26.2, target is 26.2 and up through the latest snapshot. Use only 26.2-native APIs (no mappings, ValueOutput/ValueInput patterns already in the code).
- Keep the split source sets clean: common code (src/main) never references client-only classes. Gameplay and physics logic lives in common or server-safe code and syncs by payload; rendering, shaders, particles, and shake live behind the client entrypoint.
- Visual work follows the race workflow: research the look, concept image first, then code to it, same quality across everything, render pictures committed with model work.
- Every system gets a config knob in DbaEffectsConfig style (density, scale, off switch) and respects effectDensity / screenShake.

## Audit: what already exists (do not rebuild from zero)

Weapons (19 GeckoLib weapons, including the Sickle of Sorrow):
- Items: Azure Dragon Sword, Bansho Fan, Blaster Gun, Brave Sword, Cursed Blade, Dabura Sword (Darkness), Devil Trident, Dimensional Sword, Evil Spear, Grand Sword, Hollows Edge, Katana, Ox Kings Ax, Power Pole, Saber, Sickle of Sorrow, Spirit Sword, Whis Staff, Z Sword. Each has a full Left and Right ability implementation, a GeckoLib geo model (geckolib/models/item/*.geo.json), a flat GUI texture plus a glowmask texture, and dedicated ability entities and renderers (83 client renderers total).
- Split personality problem: only 10 weapons also have a code-built procedural model in WeaponModelRegistry (dimensional, power pole, devil trident, sickle, spirit sword, bansho fan, hollows edge, whis staff, z sword, cursed blade), and only 4 of those are actually wired to the _3d swap in ItemModelResolverMixin (devil trident, power pole, dimensional sword, bansho fan). The other 9 weapons are GeckoLib only. In-hand look therefore differs by weapon family, which is exactly what a full overhaul should unify.
- SwingTrailRenderer already gives 15 bladed weapons a colored swing ribbon. Blaster, Bansho Fan, Whis Staff, and Power Pole have no trail entry.

Shaders (thin):
- Only post-process shaders exist: 3 fragment shaders (dba_eclipse, dba_grayscale, dba_shockwave) driven as stepped vanilla PostChains (eclipse_1..4, time_stop_1..4, shockwave_0..7) from DbaPostFx via GameRendererPostMixin. Uniforms are static per step file; there are no dynamic uniforms, no core/entity shaders, no aura or weapon shaders, and no built-in shader toggle or quality tiers.

Effects (wide but shallow):
- 83 ability renderers and the Ki render family (beam, blast, disk, explosion, laser, spiral) exist, plus AuraRenderer. Particles are minimal: 4 particle types (ember, spark, void_smoke, wind) plus one glow particle class. Most effects are geometry ribbons and beams, not particle systems.

Shaking (exists, basic):
- ScreenShakeHandler + CameraShakeMixin + ScreenShakePayload + hitstop already work: server triggers, client applies yaw/pitch jitter with decay, scale from config, and DbaPostFx fires a shockwave frame on intensity >= 1.2. Limits: yaw/pitch only (no roll, no positional shake, no FOV punch), random jitter only (no smooth trauma-based noise), one global shake (no per-source stacking beyond strongest-wins).

Movement (exists inside abilities, no physics layer):
- PlayerEntityMixin (1,051 lines) carries stats, flight and ability movement case by case. Dashes, rush trails, afterimages, and launches exist per weapon/technique. There is no shared movement physics system: no momentum model, air control, dash cooldown framework, wall or ground interaction, or landing impact system shared across abilities.

Mobs (gap):
- There are no custom hostile mobs to apply physics to yet. Entities are almost all ability projectiles, domains, and effects, plus Shenron, Porunga, and Flying Nimbus. Mob physics therefore means two things: (a) a knockback, launch, juggle, and ragdoll-lite response system for vanilla mobs and players hit by DBA abilities, and (b) groundwork so future DBA mobs inherit it.

## Proposed phases

### Phase 0: Compile verification (closed, waived by Carter Nov 1, 2026)
See BUILD-VERIFY.md. Carter will not build it; verification is the before and after audit on every change. The build has still never passed, and that fact stays stated plainly in every phase report.

### Phase 1: Weapon visual overhaul (all 19 weapons)
- One rendering path for every weapon: pick GeckoLib geo as the single standard (it already covers all 19 plus animations in dba_weapon.animation.json), retire or complete the procedural path so no weapon renders differently by family. Concept sheet per weapon first (front, side, in-hand, GUI), then model, texture, and glowmask rebuilt to it.
- Per-weapon identity pass: silhouette, palette, glowmask, swing trail color and width, charge visuals, and sheath/idle animation state (the Geo renderer already distinguishes idle, swinging, and using).
- Add missing trails (Blaster muzzle streak, Bansho wind arc, Whis Staff ring, Power Pole arc) and unify trail quality.
- Deliverable: render sheet per weapon committed, like the race sheets.

### Phase 2: True built-in shaders
- Dynamic post chain: replace stepped duplicate JSON files with parameterized passes where 26.2 allows it, keeping the stepped fallback. Add: chromatic aberration on heavy hits, vignette pulse on low Ki/health, heat shimmer for auras and beams, speed lines on high-velocity movement.
- World/entity shaders: aura shader (animated noise edge, form-colored), weapon energy shader (scrolling energy on glowmask areas), Ki beam core shader. All behind the client entrypoint, with off / low / high quality tiers and an Iris compatibility check (built-in effects must not fight an external shaderpack; disable world shaders gracefully when a shaderpack is active).
- Deliverable: before/after screenshots per effect.

### Phase 3: Particle and visual effects system
- One particle engine on top of the existing glow particle: pooled emitters, per-effect profiles (burst, trail, ambient, ground), density scaling from effectDensity.
- New particle sets: Ki sparks and embers per form color, slash sparks, impact debris, aura wisps, transformation pillar motes, dust and ground scorch on slams, rain of sparks on clashes (GrandClashSpark exists as a renderer; give it particles to match).
- Hit feedback kit: hit sparks, damage flash, clash flash, block/dodge sparks (SaberDodgeSpark pattern generalized).

### Phase 4: Screen and camera shake 2.0
- Trauma-based system: smooth noise instead of per-tick random jitter, roll added, small positional offset, FOV punch on the biggest hits, directional shake (away from impact), per-source trauma that adds and decays.
- Keep ScreenShakePayload as the server trigger; extend it with trauma, duration, and direction fields (version the payload, do not break old fields silently). Hitstop stays.
- Accessibility: existing screenShake scale plus a photosensitivity cap option.

### Phase 5: Movement physics
- Shared movement layer in common code: momentum-preserving dashes, air dash, double jump / Ki step, fast fall, landing impact (dust, shake, small crater scorch decal where allowed), wall cling/run-lite if it scopes cleanly, flight acceleration and banking model unified across races and forms.
- Ability migration: weapon rushes and Ki movement move onto the shared layer one at a time, keeping their damage and cooldown numbers unchanged.
- Server authoritative: movement state syncs by payload, server validates speed caps so it works on a dedicated server and in multiplayer.

### Phase 6: Mob physics
- Hit response system for LivingEntity: weight classes (light mobs launch, heavy mobs stagger, bosses resist), launch and juggle with gravity scaling, ground bounce and wall splat with impact damage, knockback that respects DbaEffectsConfig damage caps.
- Ragdoll-lite death: on death by a DBA ability, a short client-side tumble (spin, velocity, fade) instead of the vanilla tip-over, scaled by killing blow strength. Client visual only; server state stays vanilla.
- Debris: block and item debris bursts on slam impacts (OxFissure, ZGravitySlam, KingsSlam patterns generalized).

### Phase 7: More in the same spirit (proposed, Carter picks)
- Impact frames and slow-mo micro hitstop on finishers (client camera only, server unaffected).
- Sound-reactive visuals: aura hum intensity drives aura shader pulse.
- Weather/sky moments: eclipse and time-stop grades extended to form transformations (SSJ, God, Ultra Instinct each get a signature grade and burst).
- Damage direction indicator and clash camera (brief orbit nudge on beam clashes).
- Photo/dev preview: extend DevPreviewCamera into a render-sheet tool for weapons, matching the race sheet workflow.

## Risks and notes
- Loom snapshot (1.17-SNAPSHOT) plus Minecraft 26.2: pin nothing new without checking it resolves on 26.2 first.
- GeckoLib 5.5.3 AutoGlowingGeoLayer already handles glow; shader work must layer on it, not replace it blindly.
- todo.md and todo-simple.md in the repo are stale and are not a source for this scope.
- Grok also pushes to this repo from its own DBA chat: fetch before every push, merge pulls, never revert Grok commits silently.
