# Fresh Audit, Systems and Inventory (Nov 3, 2026)

Scope: the systems and inventory half of the fresh full project audit, at HEAD 94708b6. Clean slate method per Carter's order: nothing in this file comes from earlier audits or notes. Every claim was derived by reading the current code in this session. Audit only, no code changed.

## 1. Full inventory

### Java files by package

src/main/java (254 files), root package com.dragonblockarcanedba:

| Package | Files | What it is |
|---|---|---|
| entity | 101 | Ability entities, mob entities, dragons, the DbaEntities type registry, swarm and host base classes |
| item | 36 | Item classes: 22 weapons, armor, consumables, utility items, DbaItems registry, geo weapon interface package |
| effect | 29 | Mob effect classes and the DbaEffects registry (status effects such as Temporal Stasis and Storm of Darkness) |
| network | 26 | DbaNetwork plus 25 payload records (weapon input, movement, ki techniques, shake, stagger, tumble, broadcasts) |
| mixin | 10 | Common mixins, including the version-gated cloud texture mixins and the compat mixin plugin support |
| util | 9 | WeaponDrainHelper, DbaCompat version shims, MovementLimiterHelper, SwarmHelper, DbaPhysicsAttributes, trackers |
| block | 8 | DbaBlocks registry, dragon ball blocks, gravity training block, senzu plant, cloud block, block entities |
| registry | 7 | Race and form data classes and loaders, technique registry |
| ki | 4 | Ki technique handler and related player ki systems (spawns the Ki technique entities) |
| dimension | 3 | Custom dimension support (Otherworld) |
| config | 3 | DbaEffectsConfig, RaceConfig, RaceConfigManager |
| attribute | 3 | Player stat accessors and attributes |
| movement | 2 | MovementPhysics (players), MobPhysics (mobs) |
| inventory | 2 | Custom inventory and menu support |
| fx | 2 | DbaImpactFx, SlamDebris |
| compat | 2 | Version compatibility helpers |
| wish, tail, sound, skill, particle, command | 1 each | Dragon ball wish logic, tail logic, DbaSounds, skill support, DbaParticles, commands |
| (root) | 1 | DragonBlockArcaneDBA.java, the mod initializer |

src/client/java (182 files):

| Package | Files | What it is |
|---|---|---|
| client/render | 119 | 85 renderer classes at the render root, plus ki (8), layer (7), weapon (7), geo (4), model (3), animation (3), item (2) subpackages; KiRenderHelper lives in render/ki |
| client/gui | 17 | Screens and HUD overlays |
| client/mixin | 15 | Client mixins (render pipeline, camera, entity render dispatcher) |
| client/fx | 10 | DbaPostFx, ScreenShakeHandler, ChargeFx, ClashFx, DamageDirectionFx, MovementInput, StaggerFx, TumbleFx, SwingTrailRenderer, WeaponTrailProfiles |
| client/particle | 3 | DbaGlowParticle, DbaParticleProviders, OtherworldCloudParticleManager |
| client/hooks, client/compat, client/config, client/wish, client/dev | 10 | Client hooks, DbaPipelineCompat and cloud compat, client config, wish UI, dev tools |
| (client source root) | 7 | DragonBlockArcaneDBAClient.java (all entity renderer registrations) and client-set mixins |

Total: 436 Java files. DbaEntities.java declares 94 EntityType fields (entity/DbaEntities.java, 949 lines).

### Resources (brief)

- Particles: 5 particle JSONs in assets/dragonblockarcanedba/particles (spark, ember, void_smoke, wind, ki_mote), matching the 5 types in particle/DbaParticles.java:14-18.
- Post effects: 39 chain JSONs in assets/dragonblockarcanedba/post_effect (eclipse 1-4, shockwave 0-4, time_stop, ki_surge, danger, and 16 form grade chains), driven by client/fx/DbaPostFx.java.
- Models: 31 .geo.json files and 57 .animation.json files under assets; 531 PNG textures.
- Data: 77 JSONs under data/dragonblockarcanedba, including 11 race files (data/.../races) and 13 form files (data/.../forms), loaded by registry/RaceLoader.java and registry/FormLoader.java.

## 2. Weapon count and canonical list, from the code

Weapon count: 22. Source: the 22 weapon item fields in item/DbaItems.java (lines 274 to 497) and their ResourceKey registry ids (lines 21 to 240). Each weapon's ability entities were attributed by finding every construction site (new XEntity) in src/main/java, including fully qualified constructions inside item classes and secondary spawns from other entities. Renderer registrations are all in client/DragonBlockArcaneDBAClient.java (lines 60 to 422); in every case the renderer class is the entity class name plus Renderer, in client/render, except the Ki technique renderers in client/render/ki.

| # | Registry id | Item class (item/) | Ability entities (entity/) | Renderers (client/render/) |
|---|---|---|---|---|
| 1 | katana | KatanaItem.java | KatanaChargeEntity, KatanaAimGuideEntity, SwiftCrescentEntity, HeavenSplitterEntity, HollowAfterimageEntity (shared, also Saber and Hollow's Edge) | KatanaChargeRenderer, KatanaAimGuideRenderer, SwiftCrescentRenderer, HeavenSplitterRenderer, HollowAfterimageRenderer |
| 2 | z_sword | ZSwordItem.java | ZChargeEntity, ZShockwaveEntity, ZStanceAuraEntity, ZGravitySlamEntity, DelayedLaunchEntity (constructed at ZSwordItem.java:283) | ZChargeRenderer, ZShockwaveRenderer, ZStanceAuraRenderer, ZGravitySlamRenderer, DelayedLaunchRenderer |
| 3 | saber | SaberItem.java | SaberSlashEntity, SaberVoidTearEntity, SaberDodgeSparkEntity, SaberDimensionalLineSlashEntity, HollowAfterimageEntity (shared) | SaberSlashRenderer, SaberVoidTearRenderer, SaberDodgeSparkRenderer, SaberDimensionalLineSlashRenderer, HollowAfterimageRenderer |
| 4 | brave_sword | BraveSwordItem.java | BraveChargeEntity, BraveSlashEntity, BraveCrossSlashEntity, BraveRushTrailEntity, BraveShockwaveEntity | BraveChargeRenderer, BraveSlashRenderer, BraveCrossSlashRenderer, BraveRushTrailRenderer, BraveShockwaveRenderer |
| 5 | grand_sword | GrandSwordItem.java | GrandCrescentWaveEntity, GrandBladeShardEntity (GrandSwordItem.java:162-163), GrandClashSparkEntity (GrandSwordItem.java:198), ValorFieldEntity | GrandCrescentWaveRenderer, GrandBladeShardRenderer, GrandClashSparkRenderer, ValorFieldRenderer |
| 6 | azure_dragon_sword | AzureDragonSwordItem.java | AzureTempestChannelEntity, AzureStormEntity, AzureTornadoEntity (also spawned by AzureStormEntity), AzureLightningEntity (spawned by AzureStormEntity and AzureTornadoEntity), AzureRushTrailEntity, AzureSonicQuakeEntity | AzureTempestChannelRenderer, AzureStormRenderer, AzureTornadoRenderer, AzureLightningRenderer, AzureRushTrailRenderer, AzureSonicQuakeRenderer |
| 7 | dabura_sword | DaburaSwordItem.java | DarknessChargeEntity, DarknessWaveEntity, DarknessDomainEntity, DarknessBladeEntity, DarknessShatterEntity (also spawned by DarknessBladeEntity) | DarknessChargeRenderer, DarknessWaveRenderer, DarknessDomainRenderer, DarknessBladeRenderer, DarknessShatterRenderer |
| 8 | cursed_blade | CurseBladeItem.java | CurseChainEntity, AbyssalDomainEntity (CurseBladeItem.java:194), CurseTelegraphEntity (:252), CurseLightningEntity (:260, :298), SkyCracksEntity (:326), CurseGroundShatterEntity (:356) | CurseChainRenderer, AbyssalDomainRenderer, CurseTelegraphRenderer, CurseLightningRenderer, SkyCracksRenderer, CurseGroundShatterRenderer |
| 9 | hollows_edge | HollowsEdgeItem.java | HollowChargeEntity, HollowRushTrailEntity, VoidSlashEntity, VoidRiftEntity, HollowAfterimageEntity (shared) | HollowChargeRenderer, HollowRushTrailRenderer, VoidSlashRenderer, VoidRiftRenderer, HollowAfterimageRenderer |
| 10 | dimensional_sword | DimensionalSwordItem.java | DimensionalSlashEntity, DimensionalWarpRiftEntity | DimensionalSlashRenderer, DimensionalWarpRiftRenderer |
| 11 | spirit_sword | SpiritSwordItem.java | SpiritCleaveEntity, SpiritImpaleEntity (also spawned by SpiritCleaveEntity), SpiritCannonBeamEntity | SpiritCleaveRenderer, SpiritImpaleRenderer, SpiritCannonBeamRenderer |
| 12 | evil_spear | EvilSpearItem.java | EvilSpearChargeEntity, EvilSpearProjectileEntity, HellHuntImpactEntity (also spawned by EvilSpearProjectileEntity) | EvilSpearChargeRenderer, EvilSpearProjectileRenderer, HellHuntImpactRenderer |
| 13 | devil_trident | DevilTridentItem.java | TridentProngEntity (DevilTridentItem.java:149-150), TridentShardEntity, DevilSlamShockwaveEntity | TridentProngRenderer, TridentShardRenderer, DevilSlamShockwaveRenderer |
| 14 | sickle_of_sorrow | SickleOfSorrowItem.java | SorrowSlashEntity, DimensionalRiftEntity | SorrowSlashRenderer, DimensionalRiftRenderer |
| 15 | ox_kings_ax | OxKingsAxeItem.java | OxChargeEntity, OxShockwaveEntity, OxFissureEntity (spawned by OxShockwaveEntity), OxStanceAuraEntity, KingsSlamEntity | OxChargeRenderer, OxShockwaveRenderer, OxFissureRenderer, OxStanceAuraRenderer, KingsSlamRenderer |
| 16 | power_pole | PowerPoleItem.java | PowerPoleExtensionEntity, PowerPoleImpactEntity, PowerPoleWhirlwindEntity | PowerPoleExtensionRenderer, PowerPoleImpactRenderer, PowerPoleWhirlwindRenderer |
| 17 | whis_staff | WhisStaffItem.java | TimeShatterEntity, TemporalRiftEntity | TimeShatterRenderer, TemporalRiftRenderer |
| 18 | bansho_fan | BanshoFanItem.java | BanshoCycloneEntity, BanshoGaleEntity, BanshoWindProjectileEntity, BanshoShockwaveEntity (also constructed by BanshoWindProjectileEntity and by mixin/WindChargeMixin) | BanshoCycloneRenderer, BanshoGaleRenderer, BanshoWindProjectileRenderer, BanshoShockwaveRenderer |
| 19 | blaster_gun | BlasterGunItem.java | BlasterBoltEntity, ErasureChargeOrbEntity, ErasureCannonBeamEntity | BlasterBoltRenderer, ErasureChargeOrbRenderer, ErasureCannonBeamRenderer |
| 20 | lava_sword | LavaSwordItem.java | LavaCleaveEntity, LavaEruptionEntity, plus LavaSwordGround.java (a static ground-burn helper in entity/, not an entity) | LavaCleaveRenderer, LavaEruptionRenderer |
| 21 | spike_flail | SpikeFlailItem.java | SpikeFlailSwingEntity, SpikeFlailSpinEntity | SpikeFlailSwingRenderer, SpikeFlailSpinRenderer |
| 22 | war_mace | WarMaceItem.java | WarMaceSmashEntity, WarMaceRiseEntity | WarMaceSmashRenderer, WarMaceRiseRenderer |

Weapon ability entity total: 73 distinct entity classes (72 weapon-spawned plus the shared HollowAfterimageEntity counted once).

Not weapons, for completeness: the Ki technique entities (KiBlastEntity, KiBeamEntity, KiDiskEntity, KiLaserEntity, KiSpiralBeamEntity, KiExplosionEntity) are spawned by ki/KiTechniqueHandler.java (lines 119 to 312), not by any weapon item. In particular, Devil Trident does not spawn KiBlastEntity at this HEAD; its projectile is TridentProngEntity. Other non-weapon entities: KiBarrierEntity (spawned from effect/KiBarrier), the mobs SaibamanEntity, FriezaSoldierEntity, NamekianWarriorEntity, PatrolRobotEntity, the dragons ShenronEntity and PorungaEntity, FlyingNimbusEntity, OtherworldGuideEntity.

Coordination flags for the Phase 2 split: HollowAfterimageEntity and HollowAfterimageRenderer are shared by three weapons (Katana, Saber, Hollow's Edge). BanshoShockwaveEntity is also constructed by mixin/WindChargeMixin, outside the item and entity graph. AzureStormEntity, AzureTornadoEntity, DarknessBladeEntity, EvilSpearProjectileEntity, OxShockwaveEntity, SpiritCleaveEntity, and BanshoWindProjectileEntity each spawn a second weapon's entity, so those chains have two owners in a naive split.

## 3. Shared systems against the five spectacle points

The five points: (1) layered particle depth with distance scaling, dense core, mid layer, wispy outer; (2) large counts of individual tumbling debris on helical paths; (3) world interaction: ground scarring, block break or change, mob pull, lift, fling; (4) atmosphere: sky darkening, light dimming, fog or haze, lightning, weather control; (5) lifecycle envelopes: build-up, peak, slow dissipation.

### Particles: particle/DbaParticles.java, client/particle/DbaGlowParticle.java, client/particle/DbaParticleProviders.java

What it can do today: 5 custom particle types (DbaParticles.java:14-18), each a flat glow sprite with a fixed per-type style: two-stop color ramp, lifetime range, base size, growth, gravity, drag (DbaGlowParticle.java:16-23). Server code emits them with sendParticles; counts are scaled by the global effectDensity config. Four of five styles are additive.

Point gaps:
- Point 1: there is no layering system. No shared emitter builds core, mid, and outer bands; every effect hand-rolls its own sendParticles calls or hand-builds mesh geometry instead. There is no distance scaling anywhere: effectDensity (config/DbaEffectsConfig.java:54) is one global scalar from 0 to 4, not a function of viewer distance, and no particle or renderer code reduces or restructures output by range.
- Point 2: particles are single flat quads (DbaGlowParticle extends SingleQuadParticle, DbaGlowParticle.java:12). They cannot tumble as 3D pieces, and there is no helical-path emitter helper; orbit and helix motion exists only inside individual entity renderers' geometry, written per effect.
- Points 3, 4, 5: not applicable to the particle types themselves; see the systems below.

### Mesh rendering base: client/render/ki/KiRenderHelper.java

What it can do today: two render types over a flat white texture, normal translucent emissive (line 32) and additive eyes (line 27), full-bright vertex color, and shared primitives drawColoredBox (line 49), drawCylinder (line 106), drawDisc (line 131). This is why effects are colored geometry, not textured surfaces.

Point gaps: the helper offers primitives only. There is no shared layered-band, dome, funnel, or crescent builder; each of the 85 renderers at the render root re-implements its own rings, discs, and shells from these primitives, which is the structural reason effect styles drift per weapon. Nothing in the helper scales geometry or alpha by viewer distance (point 1).

### Impact feedback: fx/DbaImpactFx.java

What it can do today: server-side camera shake with quadratic distance falloff inside 28 blocks (lines 35-44), hitstop on close hits, one sound, and a fixed two-type particle burst: 40 SPARK plus 16 WIND, density scaled (lines 24-30).

Point gaps: fixed counts and fixed composition (no core, mid, outer structure, point 1); no debris (point 2); no block or mob interaction (point 3); no atmosphere (point 4); it is an instantaneous burst with no envelope (point 5).

### Debris: fx/SlamDebris.java

What it can do today: the only debris system in the project. On a slam it samples the real ground blocks under the impact and throws block-textured particles (BlockParticleOption), plus a POOF dust cloud and one accent type, with three profiles only: OX_FISSURE, Z_GRAVITY, KINGS_SLAM (line 22). Its own header states the limit plainly: "It never destroys blocks; block destruction stays with the abilities that already own it" (lines 11-14).

Point gaps:
- Point 2: this is particles, not debris pieces. Spawn points are capped at 140 (line 29) with 2 particles each, emitted once, with random short velocities. Nothing tumbles, nothing orbits, nothing follows a helical path, nothing rains back down. A codebase-wide search finds no FallingBlock usage anywhere in src/main/java or src/client/java: there is no falling-block or debris-entity machinery at all.
- Point 3: dust at the base, yes (POOF cloud, lines 47-48). Ground scarring, no: it reads the ground, it never changes it.
- Point 5: one-shot burst on the owning slam's first tick; no build-up, peak, or dissipation phases.

### Mob physics: movement/MobPhysics.java

What it can do today: hit-reaction physics for mobs struck by DBA damage. Weight classes LIGHT, HEAVY, BOSS by knockback resistance and health (lines 44-45, 149-154); launches on hits at damage 6 and up (launch, line 157) with a juggle fall-speed clamp, one ground bounce, and wall splat handled in the tracked tick (line 178); boss stagger on hits of 18 and up (line 126); ragdoll-lite death tumble delivered by payload.

Point gaps (point 3): this is impulse-on-hit only. There is no pull or attraction anywhere: no shared vortex, suction, or orbit helper exists in movement/ or util/ (a search for pull in both packages returns nothing). Nothing can draw mobs inward, hold them lifted, or fling them out on a delay; each of those would have to be built per ability or as a new shared system. Lift exists only as the launch impulse itself.

### Player movement: movement/MovementPhysics.java

What it can do today: a server-authoritative velocity API for ServerPlayer only: setVelocity, addVelocity, dampenHorizontal, rootInPlace, flight velocity blending, teleport, fall damage scaling (lines 114 to 205), driven by network/C2SMovementPayload (dash, ki step, fast fall) and client/fx/MovementInput.java.

Point gaps: players only; it does nothing for mobs, debris, or blocks, so it cannot carry points 2 or 3 for ability spectacle. It is not an effects system.

### Block modification

What exists today is per-ability, hand-rolled, in exactly three places in weapon code:
- entity/LavaSwordGround.java: temporary fire and lava placement on surfaces with a pending-restore tick system (the whole file; lava sources are budgeted per swing by the calling entities).
- entity/OxShockwaveEntity.java:263: level.destroyBlock(pos, true), weak terrain destruction at max charge only.
- item/CurseBladeItem.java:284: setBlockAndUpdate placing vanilla FIRE at strike sites.

Point gaps (point 3): there is no shared block-damage or ground-scar helper: no crater builder, no scar decals or cracked-ground persistence, no per-ability terrain budget system beyond the lava helper's restore list. Any new world interaction in Phase 2 either reuses LavaSwordGround's pattern (temporary, restored) or is new shared code. SlamDebris deliberately does not modify blocks (fx/SlamDebris.java:11-14).

### Atmosphere and weather

What exists today:
- Nothing controls weather. A search of both source sets finds no call to any weather setter (no setWeatherType, setRainLevel, or setThunderLevel) and no fog hook of any kind.
- Azure Storm is named a weather domain (entity/AzureStormEntity.java:22) but does not change level weather, rain, sky, or light. Its weather content is sound: thunder and zap sounds at AzureStormEntity.java:172-182, plus the AzureLightningEntity visual.
- Lightning visuals exist only as two per-weapon entities (AzureLightningEntity, CurseLightningEntity) with textured bolts. There is no shared lightning system other weapons can call.
- client/fx/DbaPostFx.java provides screen-space grades: an eclipse darkening chain while the local player has the Storm of Darkness effect (lines 44, 104), a grayscale time-stop grade, a ki surge grade, a danger vignette, form signature grades, and an impact shockwave ripple. These are per-player screen effects with stepped 4-level envelopes (lines 16-17, 93-95). They do not change world light, sky, or fog for anyone else, and they cannot be triggered as an atmosphere system by an ability at a location.
- A cloud layer system exists (client/compat/DbaCloudLayers263.java, mixin/CloudTextureMixin.java, mixin/CloudTextureMixin263.java, client/particle/OtherworldCloudParticleManager.java) but it is dimension decoration for Otherworld, gated by Minecraft version, not an ability atmosphere tool.

Point gaps (point 4): effectively total. Sky darkening, light dimming, haze, and weather change have no shared machinery; the only lever that exists is the per-player post chain in DbaPostFx, which is local, effect-driven, and capped at 4 static steps per chain because chain uniforms are baked into the JSON (DbaPostFx.java:16).

### Lifecycle envelopes (point 5)

There is no shared lifecycle or envelope system. Each ability entity hardcodes its own lifetime and each renderer computes its own fade math inside its submit method; charge and domain entities are event-driven with no fixed lifetime at all. The only stepped envelope machinery in the project is DbaPostFx's 4-step screen grades (client/fx/DbaPostFx.java:93-95), which is screen space, not world effects. Build-up, peak, and dissipation phases would have to be defined per ability or as new shared code in Phase 1's standard; nothing today enforces or assists them.

### Effects config: config/DbaEffectsConfig.java

Three tunables only (lines 21-25): effectDensity (particle count scalar, 0 to 4, line 54), screenShake (0 to 4), maxWeaponDamage (global per-hit cap, applied by capDamage, lines 57-62). There is no setting for debris counts, no distance-based scaling, no atmosphere toggle, and no per-effect quality tier. Any distance scaling or debris budget in the new standard needs config support added here, by the coordinating pass, since this file is shared.

### Networking: network/ (26 files)

Transport is complete for what exists: C2S payloads for weapon left clicks, movement, and ki techniques; S2C payloads for screen shake, boss stagger, death tumble, and state broadcasts. There is no payload for weather or atmosphere state, because there is no atmosphere system to sync. Particle spectacle needs no new networking (server sendParticles already replicates); debris entities and weather would.

### Ki techniques: ki/KiTechniqueHandler.java

Spawns the six Ki technique entities (lines 119 to 312) for the player technique system. Not weapon code, but it is a second consumer of the same render and particle base, so shared effect building blocks built in Phase 1 will affect it; the coordinating pass should treat it as a shared-code stakeholder.

## 4. Code health, shared and non-weapon code

- No TODO, FIXME, or XXX markers exist anywhere in src/main/java or src/client/java (full search, zero hits).
- No renderer is a no-op: a search for no-op or invisible-marker markers returns nothing, and every entity type in DbaEntities.java has a renderer registration in client/DragonBlockArcaneDBAClient.java (verified by pairing all registrations, lines 60 to 422, against the type list).
- Structural duplication: effect geometry is duplicated per renderer. 85 renderer classes at the client/render root each hand-build similar rings, discs, domes, and crescents from the same three KiRenderHelper primitives (client/render/ki/KiRenderHelper.java:49, 106, 131). There is no shared mesh library above those primitives. This is the main consistency risk for a multi-agent Phase 2 and the reason the shared building blocks have to come first.
- One spawner lives outside the item and entity graph: mixin/WindChargeMixin constructs BanshoShockwaveEntity. Any change to that entity's constructor or synced data must include the mixin.
- Secondary spawn chains concentrate risk: AzureStormEntity spawns two other ability entities (AzureTornadoEntity, AzureLightningEntity), and OxShockwaveEntity spawns OxFissureEntity. Constructor or data changes there cross weapon-internal boundaries even inside one weapon's file set.
- Version split is real and load-bearing: util/DbaCompat.java and the version-gated cloud mixins (mixin/CloudTextureMixin.java for 26.2, mixin/CloudTextureMixin263.java for 26.3) mean shared code cannot assume one Minecraft version's API surface. The build floor is 26.2 (gradle.properties: minecraft_version=26.2) and 26.3 runs the same jar through these shims.

## 5. Build reality check

From gradle.properties and build.gradle: Minecraft 26.2, Fabric loader 0.19.3, Loom 1.17-SNAPSHOT, Fabric API 0.154.2+26.2, GeckoLib 5.5.3, Java 25, Gradle wrapper 9.5.1.

From BUILD-VERIFY.md (the one standing-instructions file this audit was allowed to read): the mod has never had a passing build, and no build is possible on this machine. Six recorded sandbox attempts all failed before compilation at Gradle daemon connect (DaemonInitialConnectException), caused by the sandbox's non-HTTP loopback TCP restriction; the same restriction hit two other projects on this VM. Carter declined the Windows build on Nov 1, 2026 and set the standing verification instead: a written before audit and after audit on every change, full diff re-reads, JSON validation by parsing, import and reference resolution, split source set checks, and mixin target checks against in-repo usage. The file states the rule explicitly: "An audit is never reported as a compile."

What verification is actually possible here, then: code audits of exactly this kind, plus a javac pass against dependency jars fetched over HTTP where available (the audit classpath under ~/workspace/dba-audit-libs, with the real 26.2 and 26.3 Minecraft, Fabric API, loader, and GeckoLib jars), which compares javac error kinds before and after a change. That javac pass is an audit signal, not a Gradle build, and must never be reported as one. The real build can only happen on Carter's PC after the final push.
