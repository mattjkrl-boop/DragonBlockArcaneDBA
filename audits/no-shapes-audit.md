# No-Shapes Realism Pass: Fresh Audit

Measured fresh from the code at dba d9f8a90 on Nov 4, 2026. Old audits were not used as a source of counts. This inventory is the Phase 2 work order for the no-shapes realism pass.

## Method

- B counts calls to the nine BespokeGeometry builders: tornRibbon, shardFan, bladeArc, displacedDome, brokenVortexBand, glowFan, branch, crackStar, and plume.
- L counts LayeredRender geometry calls only: drawVortexBands and drawAnchorGlow. LayeredRender.distScale is a distance utility, not a drawing call, and is counted separately below.
- V counts direct vertex writing in effect code: VertexConsumer.addVertex calls in the two Hollow renderers, plus KiRenderHelper.vertex calls in SwingTrailRenderer, BoltRenderer, and DreadGazeRenderer.
- BespokeGeometry.Rng references and other non-drawing references are not builder calls. They still have to leave effect renderers when the geometry does, because the standard forbids the toolkit in effect visuals.
- Physical models are not counted as effect geometry: weapon models, race models, mob models, the Flying Nimbus model, and DebrisRenderer block models. Their energy, fire, smoke, and light remain in scope wherever a separate effect renderer draws them.

## Totals at d9f8a90

| Surface | Count | Files |
|---|---:|---:|
| BespokeGeometry builder calls in effect code | 684 | 90 |
| BespokeGeometry builder calls inside LayeredRender itself | 3 | 1 |
| LayeredRender geometry calls | 44 | 29 |
| LayeredRender.distScale references (utility, not geometry) | 115 | 85 |
| Direct addVertex calls in effect renderers | 40 | 2 |
| KiRenderHelper.vertex calls in effect code outside the geometry toolkit | 30 | 3 |
| Distinct files with visible effect geometry | 94 | 94 |
| KiRenderHelper primitive methods with zero call sites | 5 | 1 |

The five uncalled KiRenderHelper primitives are drawColoredBox, drawCylinder, drawDisc, drawSphere, and drawHelix. They draw nothing today, but they are the old shape toolkit and stay deprecated under the standard.

## Work order by owner

Every row is one effect surface. B, L, and V are the counts defined above. The material column is binding for Phase 2: the converted effect must be made of that material, with the owner theme and colors preserved.

### Katana

| Effect | File | B | L | V | Must become |
|---|---|---:|---:|---:|---|
| KatanaAimGuide | `src/client/java/com/dragonblockarcanedba/client/render/KatanaAimGuideRenderer.java` | 13 | 0 | 0 | gathering plasma motes, vapor, and material drift pulled toward the weapon or caster; theme: ki plasma, air vapor, steel sparks |
| KatanaCharge | `src/client/java/com/dragonblockarcanedba/client/render/KatanaChargeRenderer.java` | 10 | 1 | 0 | gathering plasma motes, vapor, and material drift pulled toward the weapon or caster; theme: ki plasma, air vapor, steel sparks |
| HeavenSplitter | `src/client/java/com/dragonblockarcanedba/client/render/HeavenSplitterRenderer.java` | 7 | 1 | 0 | material particles and vapor in the owner theme; theme: ki plasma, air vapor, steel sparks |
| SwiftCrescent | `src/client/java/com/dragonblockarcanedba/client/render/SwiftCrescentRenderer.java` | 6 | 0 | 0 | swing-driven noisy plasma trail, vapor, sparks, and dissolving material; theme: ki plasma, air vapor, steel sparks |
| **Owner total** |  | **36** | **2** | **0** |  |

### Z Sword

| Effect | File | B | L | V | Must become |
|---|---|---:|---:|---:|---|
| ZCharge | `src/client/java/com/dragonblockarcanedba/client/render/ZChargeRenderer.java` | 13 | 0 | 0 | gathering plasma motes, vapor, and material drift pulled toward the weapon or caster; theme: gravity plasma, stone dust, steel sparks |
| ZGravitySlam | `src/client/java/com/dragonblockarcanedba/client/render/ZGravitySlamRenderer.java` | 9 | 1 | 0 | compressed ground dust, vapor, physical debris, and impact embers or sparks; theme: gravity plasma, stone dust, steel sparks |
| ZShockwave | `src/client/java/com/dragonblockarcanedba/client/render/ZShockwaveRenderer.java` | 4 | 0 | 0 | compressed ground dust, vapor, physical debris, and impact embers or sparks; theme: gravity plasma, stone dust, steel sparks |
| ZStanceAura | `src/client/java/com/dragonblockarcanedba/client/render/ZStanceAuraRenderer.java` | 13 | 0 | 0 | gathering plasma motes, vapor, and material drift pulled toward the weapon or caster; theme: gravity plasma, stone dust, steel sparks |
| DelayedLaunch | `src/client/java/com/dragonblockarcanedba/client/render/DelayedLaunchRenderer.java` | 5 | 1 | 0 | material particles and vapor in the owner theme; theme: gravity plasma, stone dust, steel sparks |
| **Owner total** |  | **44** | **2** | **0** |  |

### Saber

| Effect | File | B | L | V | Must become |
|---|---|---:|---:|---:|---|
| SaberDimensionalLineSlash | `src/client/java/com/dragonblockarcanedba/client/render/SaberDimensionalLineSlashRenderer.java` | 7 | 0 | 0 | swing-driven noisy plasma trail, vapor, sparks, and dissolving material; theme: void plasma, spatial glass dust, darkness vapor |
| SaberDodgeSpark | `src/client/java/com/dragonblockarcanedba/client/render/SaberDodgeSparkRenderer.java` | 9 | 0 | 0 | material particles and vapor in the owner theme; theme: void plasma, spatial glass dust, darkness vapor |
| SaberSlash | `src/client/java/com/dragonblockarcanedba/client/render/SaberSlashRenderer.java` | 10 | 0 | 0 | swing-driven noisy plasma trail, vapor, sparks, and dissolving material; theme: void plasma, spatial glass dust, darkness vapor |
| SaberVoidTear | `src/client/java/com/dragonblockarcanedba/client/render/SaberVoidTearRenderer.java` | 9 | 0 | 0 | material particles and vapor in the owner theme; theme: void plasma, spatial glass dust, darkness vapor |
| **Owner total** |  | **35** | **0** | **0** |  |

### Brave Sword

| Effect | File | B | L | V | Must become |
|---|---|---:|---:|---:|---|
| BraveCharge | `src/client/java/com/dragonblockarcanedba/client/render/BraveChargeRenderer.java` | 12 | 1 | 0 | gathering plasma motes, vapor, and material drift pulled toward the weapon or caster; theme: valor plasma, golden motes, impact dust |
| BraveCrossSlash | `src/client/java/com/dragonblockarcanedba/client/render/BraveCrossSlashRenderer.java` | 6 | 0 | 0 | swing-driven noisy plasma trail, vapor, sparks, and dissolving material; theme: valor plasma, golden motes, impact dust |
| BraveRushTrail | `src/client/java/com/dragonblockarcanedba/client/render/BraveRushTrailRenderer.java` | 9 | 0 | 0 | motion-driven vapor and plasma particles shed along the real movement path, plus dust at contacts; theme: valor plasma, golden motes, impact dust |
| BraveShockwave | `src/client/java/com/dragonblockarcanedba/client/render/BraveShockwaveRenderer.java` | 9 | 1 | 0 | compressed ground dust, vapor, physical debris, and impact embers or sparks; theme: valor plasma, golden motes, impact dust |
| BraveSlash | `src/client/java/com/dragonblockarcanedba/client/render/BraveSlashRenderer.java` | 5 | 0 | 0 | swing-driven noisy plasma trail, vapor, sparks, and dissolving material; theme: valor plasma, golden motes, impact dust |
| **Owner total** |  | **41** | **2** | **0** |  |

### Grand Sword

| Effect | File | B | L | V | Must become |
|---|---|---:|---:|---:|---|
| GrandBladeShard | `src/client/java/com/dragonblockarcanedba/client/render/GrandBladeShardRenderer.java` | 8 | 0 | 0 | physical fragment material where a real fragment exists, otherwise dust, sparks, and plasma; no shard mesh; theme: valor plasma, steel sparks, stone dust |
| GrandClashSpark | `src/client/java/com/dragonblockarcanedba/client/render/GrandClashSparkRenderer.java` | 8 | 0 | 0 | material particles and vapor in the owner theme; theme: valor plasma, steel sparks, stone dust |
| GrandCrescentWave | `src/client/java/com/dragonblockarcanedba/client/render/GrandCrescentWaveRenderer.java` | 4 | 0 | 0 | swing-driven noisy plasma trail, vapor, sparks, and dissolving material; theme: valor plasma, steel sparks, stone dust |
| ValorField | `src/client/java/com/dragonblockarcanedba/client/render/ValorFieldRenderer.java` | 9 | 2 | 0 | material mote volume, noise-eroded film, and hit ripple particle bursts; theme: valor plasma, steel sparks, stone dust |
| **Owner total** |  | **29** | **2** | **0** |  |

### Azure Dragon Sword

| Effect | File | B | L | V | Must become |
|---|---|---:|---:|---:|---|
| AzureLightning | `src/client/java/com/dragonblockarcanedba/client/render/AzureLightningRenderer.java` | 4 | 2 | 0 | lightning plasma layered along the seeded branch path, impact sparks, brief plasma afterglow; theme: tempest plasma, rain, mist, lightning plasma |
| AzureRushTrail | `src/client/java/com/dragonblockarcanedba/client/render/AzureRushTrailRenderer.java` | 4 | 0 | 0 | motion-driven vapor and plasma particles shed along the real movement path, plus dust at contacts; theme: tempest plasma, rain, mist, lightning plasma |
| AzureSonicQuake | `src/client/java/com/dragonblockarcanedba/client/render/AzureSonicQuakeRenderer.java` | 6 | 0 | 0 | compressed ground dust, vapor, physical debris, and impact embers or sparks; theme: tempest plasma, rain, mist, lightning plasma |
| AzureStorm | `src/client/java/com/dragonblockarcanedba/client/render/AzureStormRenderer.java` | 7 | 1 | 0 | wind-driven rain, mist, vapor, and suspended dust moved by particle forces; theme: tempest plasma, rain, mist, lightning plasma |
| AzureTempestChannel | `src/client/java/com/dragonblockarcanedba/client/render/AzureTempestChannelRenderer.java` | 5 | 1 | 0 | material particles and vapor in the owner theme; theme: tempest plasma, rain, mist, lightning plasma |
| AzureTornado | `src/client/java/com/dragonblockarcanedba/client/render/AzureTornadoRenderer.java` | 5 | 3 | 0 | wind-driven rain, mist, vapor, and suspended dust moved by particle forces; theme: tempest plasma, rain, mist, lightning plasma |
| **Owner total** |  | **31** | **7** | **0** |  |

### Dabura's Sword

| Effect | File | B | L | V | Must become |
|---|---|---:|---:|---:|---|
| DarknessBlade | `src/client/java/com/dragonblockarcanedba/client/render/DarknessBladeRenderer.java` | 6 | 0 | 0 | plasma and vapor shed from the real weapon model, impact sparks, and smoke; theme: darkness smoke, hellish plasma, soul vapor |
| DarknessCharge | `src/client/java/com/dragonblockarcanedba/client/render/DarknessChargeRenderer.java` | 11 | 1 | 0 | gathering plasma motes, vapor, and material drift pulled toward the weapon or caster; theme: darkness smoke, hellish plasma, soul vapor |
| DarknessDomain | `src/client/java/com/dragonblockarcanedba/client/render/DarknessDomainRenderer.java` | 9 | 2 | 0 | material mote volume, noise-eroded film, and hit ripple particle bursts; theme: darkness smoke, hellish plasma, soul vapor |
| DarknessShatter | `src/client/java/com/dragonblockarcanedba/client/render/DarknessShatterRenderer.java` | 9 | 0 | 0 | ground crack decals, dust, physical debris where present, and plasma carried in the cracks; theme: darkness smoke, hellish plasma, soul vapor |
| DarknessWave | `src/client/java/com/dragonblockarcanedba/client/render/DarknessWaveRenderer.java` | 8 | 0 | 0 | swing-driven noisy plasma trail, vapor, sparks, and dissolving material; theme: darkness smoke, hellish plasma, soul vapor |
| **Owner total** |  | **43** | **3** | **0** |  |

### Cursed Blade

| Effect | File | B | L | V | Must become |
|---|---|---:|---:|---:|---|
| AbyssalDomain | `src/client/java/com/dragonblockarcanedba/client/render/AbyssalDomainRenderer.java` | 14 | 3 | 0 | material mote volume, noise-eroded film, and hit ripple particle bursts; theme: curse plasma, darkness smoke, lightning plasma, ground dust |
| CurseChain | `src/client/java/com/dragonblockarcanedba/client/render/CurseChainRenderer.java` | 8 | 0 | 0 | curse plasma links carried by dense particles, darkness smoke, and impact sparks; theme: curse plasma, darkness smoke, lightning plasma, ground dust |
| CurseGroundShatter | `src/client/java/com/dragonblockarcanedba/client/render/CurseGroundShatterRenderer.java` | 8 | 0 | 0 | ground crack decals, dust, physical debris where present, and plasma carried in the cracks; theme: curse plasma, darkness smoke, lightning plasma, ground dust |
| CurseLightning | `src/client/java/com/dragonblockarcanedba/client/render/CurseLightningRenderer.java` | 5 | 0 | 0 | lightning plasma layered along the seeded branch path, impact sparks, brief plasma afterglow; theme: curse plasma, darkness smoke, lightning plasma, ground dust |
| CurseTelegraph | `src/client/java/com/dragonblockarcanedba/client/render/CurseTelegraphRenderer.java` | 8 | 0 | 0 | gathering plasma motes, vapor, and material drift pulled toward the weapon or caster; theme: curse plasma, darkness smoke, lightning plasma, ground dust |
| SkyCracks | `src/client/java/com/dragonblockarcanedba/client/render/SkyCracksRenderer.java` | 5 | 0 | 0 | ground crack decals, dust, physical debris where present, and plasma carried in the cracks; theme: curse plasma, darkness smoke, lightning plasma, ground dust |
| **Owner total** |  | **48** | **3** | **0** |  |

### Hollow's Edge

| Effect | File | B | L | V | Must become |
|---|---|---:|---:|---:|---|
| HollowAfterimage | `src/client/java/com/dragonblockarcanedba/client/render/HollowAfterimageRenderer.java` | 10 | 0 | 0 | soul vapor, darkness smoke, and dissolving plasma afterglow in the caster colors; theme: soul wisps, void plasma, darkness smoke |
| HollowCharge | `src/client/java/com/dragonblockarcanedba/client/render/HollowChargeRenderer.java` | 0 | 0 | 24 | gathering plasma motes, vapor, and material drift pulled toward the weapon or caster; theme: soul wisps, void plasma, darkness smoke |
| HollowRushTrail | `src/client/java/com/dragonblockarcanedba/client/render/HollowRushTrailRenderer.java` | 0 | 0 | 16 | motion-driven vapor and plasma particles shed along the real movement path, plus dust at contacts; theme: soul wisps, void plasma, darkness smoke |
| VoidRift | `src/client/java/com/dragonblockarcanedba/client/render/VoidRiftRenderer.java` | 10 | 0 | 0 | inward-streaming plasma, darkness smoke, irregular crack decals, and distortion particles; theme: soul wisps, void plasma, darkness smoke |
| VoidSlash | `src/client/java/com/dragonblockarcanedba/client/render/VoidSlashRenderer.java` | 7 | 0 | 0 | swing-driven noisy plasma trail, vapor, sparks, and dissolving material; theme: soul wisps, void plasma, darkness smoke |
| **Owner total** |  | **27** | **0** | **40** |  |

### Dimensional Sword

| Effect | File | B | L | V | Must become |
|---|---|---:|---:|---:|---|
| DimensionalSlash | `src/client/java/com/dragonblockarcanedba/client/render/DimensionalSlashRenderer.java` | 12 | 1 | 0 | swing-driven noisy plasma trail, vapor, sparks, and dissolving material; theme: void plasma, spatial glass dust, darkness vapor |
| DimensionalWarpRift | `src/client/java/com/dragonblockarcanedba/client/render/DimensionalWarpRiftRenderer.java` | 13 | 2 | 0 | inward-streaming plasma, darkness smoke, irregular crack decals, and distortion particles; theme: void plasma, spatial glass dust, darkness vapor |
| **Owner total** |  | **25** | **3** | **0** |  |

### Spirit Sword

| Effect | File | B | L | V | Must become |
|---|---|---:|---:|---:|---|
| SpiritCannonBeam | `src/client/java/com/dragonblockarcanedba/client/render/SpiritCannonBeamRenderer.java` | 8 | 0 | 0 | dense plasma emission along the path, impact plume, smoke, and vapor; theme: soul plasma, spirit wisps, vapor |
| SpiritCleave | `src/client/java/com/dragonblockarcanedba/client/render/SpiritCleaveRenderer.java` | 8 | 0 | 0 | swing-driven noisy plasma trail, vapor, sparks, and dissolving material; theme: soul plasma, spirit wisps, vapor |
| SpiritImpale | `src/client/java/com/dragonblockarcanedba/client/render/SpiritImpaleRenderer.java` | 19 | 2 | 0 | dense plasma particle cluster with a material trail and impact plume; physical projectile models, where one exists, stay detailed models; theme: soul plasma, spirit wisps, vapor |
| **Owner total** |  | **35** | **2** | **0** |  |

### Evil Spear

| Effect | File | B | L | V | Must become |
|---|---|---:|---:|---:|---|
| EvilSpearCharge | `src/client/java/com/dragonblockarcanedba/client/render/EvilSpearChargeRenderer.java` | 12 | 1 | 0 | gathering plasma motes, vapor, and material drift pulled toward the weapon or caster; theme: darkness smoke, hellfire embers, soul vapor |
| EvilSpearProjectile | `src/client/java/com/dragonblockarcanedba/client/render/EvilSpearProjectileRenderer.java` | 4 | 0 | 0 | dense plasma particle cluster with a material trail and impact plume; physical projectile models, where one exists, stay detailed models; theme: darkness smoke, hellfire embers, soul vapor |
| HellHuntImpact | `src/client/java/com/dragonblockarcanedba/client/render/HellHuntImpactRenderer.java` | 10 | 2 | 0 | material particles and vapor in the owner theme; theme: darkness smoke, hellfire embers, soul vapor |
| **Owner total** |  | **26** | **3** | **0** |  |

### Devil Trident

| Effect | File | B | L | V | Must become |
|---|---|---:|---:|---:|---|
| DevilSlamShockwave | `src/client/java/com/dragonblockarcanedba/client/render/DevilSlamShockwaveRenderer.java` | 13 | 0 | 0 | compressed ground dust, vapor, physical debris, and impact embers or sparks; theme: hellfire flame, magma embers, brimstone smoke, ground dust |
| TridentProng | `src/client/java/com/dragonblockarcanedba/client/render/TridentProngRenderer.java` | 3 | 0 | 0 | dense plasma particle cluster with a material trail and impact plume; physical projectile models, where one exists, stay detailed models; theme: hellfire flame, magma embers, brimstone smoke, ground dust |
| TridentShard | `src/client/java/com/dragonblockarcanedba/client/render/TridentShardRenderer.java` | 8 | 0 | 0 | physical fragment material where a real fragment exists, otherwise dust, sparks, and plasma; no shard mesh; theme: hellfire flame, magma embers, brimstone smoke, ground dust |
| **Owner total** |  | **24** | **0** | **0** |  |

### Sickle of Sorrow

| Effect | File | B | L | V | Must become |
|---|---|---:|---:|---:|---|
| DimensionalRift | `src/client/java/com/dragonblockarcanedba/client/render/DimensionalRiftRenderer.java` | 13 | 2 | 0 | inward-streaming plasma, darkness smoke, irregular crack decals, and distortion particles; theme: soul wisps, void plasma, darkness smoke |
| SorrowSlash | `src/client/java/com/dragonblockarcanedba/client/render/SorrowSlashRenderer.java` | 1 | 0 | 0 | swing-driven noisy plasma trail, vapor, sparks, and dissolving material; theme: soul wisps, void plasma, darkness smoke |
| **Owner total** |  | **14** | **2** | **0** |  |

### Ox King's Ax

| Effect | File | B | L | V | Must become |
|---|---|---:|---:|---:|---|
| KingsSlam | `src/client/java/com/dragonblockarcanedba/client/render/KingsSlamRenderer.java` | 10 | 1 | 0 | compressed ground dust, vapor, physical debris, and impact embers or sparks; theme: ground dust, stone debris, shock vapor, embers |
| OxCharge | `src/client/java/com/dragonblockarcanedba/client/render/OxChargeRenderer.java` | 8 | 1 | 0 | gathering plasma motes, vapor, and material drift pulled toward the weapon or caster; theme: ground dust, stone debris, shock vapor, embers |
| OxFissure | `src/client/java/com/dragonblockarcanedba/client/render/OxFissureRenderer.java` | 7 | 1 | 0 | ground crack decals, dust, physical debris where present, and plasma carried in the cracks; theme: ground dust, stone debris, shock vapor, embers |
| OxShockwave | `src/client/java/com/dragonblockarcanedba/client/render/OxShockwaveRenderer.java` | 6 | 1 | 0 | compressed ground dust, vapor, physical debris, and impact embers or sparks; theme: ground dust, stone debris, shock vapor, embers |
| OxStanceAura | `src/client/java/com/dragonblockarcanedba/client/render/OxStanceAuraRenderer.java` | 16 | 2 | 0 | gathering plasma motes, vapor, and material drift pulled toward the weapon or caster; theme: ground dust, stone debris, shock vapor, embers |
| **Owner total** |  | **47** | **6** | **0** |  |

### Power Pole

| Effect | File | B | L | V | Must become |
|---|---|---:|---:|---:|---|
| PowerPoleExtension | `src/client/java/com/dragonblockarcanedba/client/render/PowerPoleExtensionRenderer.java` | 11 | 0 | 0 | dense plasma particle cluster with a material trail and impact plume; physical projectile models, where one exists, stay detailed models; theme: air vapor, impact dust, steel sparks |
| PowerPoleImpact | `src/client/java/com/dragonblockarcanedba/client/render/PowerPoleImpactRenderer.java` | 8 | 0 | 0 | material particles and vapor in the owner theme; theme: air vapor, impact dust, steel sparks |
| PowerPoleWhirlwind | `src/client/java/com/dragonblockarcanedba/client/render/PowerPoleWhirlwindRenderer.java` | 7 | 2 | 0 | motion-driven vapor and plasma particles shed along the real movement path, plus dust at contacts; theme: air vapor, impact dust, steel sparks |
| **Owner total** |  | **26** | **2** | **0** |  |

### Whis Staff

| Effect | File | B | L | V | Must become |
|---|---|---:|---:|---:|---|
| TemporalRift | `src/client/java/com/dragonblockarcanedba/client/render/TemporalRiftRenderer.java` | 14 | 0 | 0 | inward-streaming plasma, darkness smoke, irregular crack decals, and distortion particles; theme: temporal plasma, glass dust, mist |
| **Owner total** |  | **14** | **0** | **0** |  |

### Bansho Fan

| Effect | File | B | L | V | Must become |
|---|---|---:|---:|---:|---|
| BanshoCyclone | `src/client/java/com/dragonblockarcanedba/client/render/BanshoCycloneRenderer.java` | 6 | 2 | 0 | wind-driven rain, mist, vapor, and suspended dust moved by particle forces; theme: wind vapor, airborne dust, mist |
| BanshoGale | `src/client/java/com/dragonblockarcanedba/client/render/BanshoGaleRenderer.java` | 3 | 0 | 0 | motion-driven vapor and plasma particles shed along the real movement path, plus dust at contacts; theme: wind vapor, airborne dust, mist |
| BanshoShockwave | `src/client/java/com/dragonblockarcanedba/client/render/BanshoShockwaveRenderer.java` | 10 | 0 | 0 | compressed ground dust, vapor, physical debris, and impact embers or sparks; theme: wind vapor, airborne dust, mist |
| BanshoWindProjectile | `src/client/java/com/dragonblockarcanedba/client/render/BanshoWindProjectileRenderer.java` | 9 | 0 | 0 | dense plasma particle cluster with a material trail and impact plume; physical projectile models, where one exists, stay detailed models; theme: wind vapor, airborne dust, mist |
| **Owner total** |  | **28** | **2** | **0** |  |

### Blaster Gun

| Effect | File | B | L | V | Must become |
|---|---|---:|---:|---:|---|
| BlasterBolt | `src/client/java/com/dragonblockarcanedba/client/render/BlasterBoltRenderer.java` | 7 | 0 | 0 | dense plasma particle cluster with a material trail and impact plume; physical projectile models, where one exists, stay detailed models; theme: ki plasma, steel sparks, smoke, vapor |
| ErasureCannonBeam | `src/client/java/com/dragonblockarcanedba/client/render/ErasureCannonBeamRenderer.java` | 8 | 0 | 0 | dense plasma emission along the path, impact plume, smoke, and vapor; theme: ki plasma, steel sparks, smoke, vapor |
| ErasureChargeOrb | `src/client/java/com/dragonblockarcanedba/client/render/ErasureChargeOrbRenderer.java` | 7 | 0 | 0 | gathering plasma motes, vapor, and material drift pulled toward the weapon or caster; theme: ki plasma, steel sparks, smoke, vapor |
| **Owner total** |  | **22** | **0** | **0** |  |

### Lava Sword

| Effect | File | B | L | V | Must become |
|---|---|---:|---:|---:|---|
| LavaBlob | `src/client/java/com/dragonblockarcanedba/client/render/LavaBlobRenderer.java` | 3 | 0 | 0 | flame flipbooks, magma embers, smoke, ash, vapor, and real crust or debris material; theme: flame, magma embers, smoke, ash, vapor |
| LavaCleave | `src/client/java/com/dragonblockarcanedba/client/render/LavaCleaveRenderer.java` | 4 | 0 | 0 | swing-driven noisy plasma trail, vapor, sparks, and dissolving material; theme: flame, magma embers, smoke, ash, vapor |
| LavaEruption | `src/client/java/com/dragonblockarcanedba/client/render/LavaEruptionRenderer.java` | 3 | 1 | 0 | flame flipbooks, magma embers, smoke, ash, vapor, and real crust or debris material; theme: flame, magma embers, smoke, ash, vapor |
| LavaGeyser | `src/client/java/com/dragonblockarcanedba/client/render/LavaGeyserRenderer.java` | 5 | 0 | 0 | flame flipbooks, magma embers, smoke, ash, vapor, and real crust or debris material; theme: flame, magma embers, smoke, ash, vapor |
| LavaMeteor | `src/client/java/com/dragonblockarcanedba/client/render/LavaMeteorRenderer.java` | 5 | 0 | 0 | flame flipbooks, magma embers, smoke, ash, vapor, and real crust or debris material; theme: flame, magma embers, smoke, ash, vapor |
| **Owner total** |  | **20** | **1** | **0** |  |

### Spike Flail

| Effect | File | B | L | V | Must become |
|---|---|---:|---:|---:|---|
| SpikeFlailSpin | `src/client/java/com/dragonblockarcanedba/client/render/SpikeFlailSpinRenderer.java` | 5 | 2 | 0 | motion-driven vapor and plasma particles shed along the real movement path, plus dust at contacts; theme: air vapor, steel sparks, ground dust |
| SpikeFlailSwing | `src/client/java/com/dragonblockarcanedba/client/render/SpikeFlailSwingRenderer.java` | 6 | 0 | 0 | motion-driven vapor and plasma particles shed along the real movement path, plus dust at contacts; theme: air vapor, steel sparks, ground dust |
| **Owner total** |  | **11** | **2** | **0** |  |

### War Mace

| Effect | File | B | L | V | Must become |
|---|---|---:|---:|---:|---|
| WarMaceRise | `src/client/java/com/dragonblockarcanedba/client/render/WarMaceRiseRenderer.java` | 2 | 0 | 0 | compressed ground dust, vapor, physical debris, and impact embers or sparks; theme: stone dust, physical debris, steel sparks, shock vapor |
| WarMaceSmash | `src/client/java/com/dragonblockarcanedba/client/render/WarMaceSmashRenderer.java` | 9 | 0 | 0 | compressed ground dust, vapor, physical debris, and impact embers or sparks; theme: stone dust, physical debris, steel sparks, shock vapor |
| **Owner total** |  | **11** | **0** | **0** |  |

### Shared weapon systems

| Effect | File | B | L | V | Must become |
|---|---|---:|---:|---:|---|
| SwingTrail | `src/client/java/com/dragonblockarcanedba/client/fx/SwingTrailRenderer.java` | 0 | 0 | 8 | motion-driven vapor and plasma particles shed along the real movement path, plus dust at contacts; theme: themed plasma, vapor, sparks, or lightning plasma from the calling weapon |
| Bolt | `src/client/java/com/dragonblockarcanedba/client/render/BoltRenderer.java` | 0 | 0 | 14 | lightning plasma layered along the seeded branch path, impact sparks, brief plasma afterglow; theme: themed plasma, vapor, sparks, or lightning plasma from the calling weapon |
| **Owner total** |  | **0** | **0** | **22** |  |

### Ki Techniques

| Effect | File | B | L | V | Must become |
|---|---|---:|---:|---:|---|
| KiBarrier | `src/client/java/com/dragonblockarcanedba/client/render/ki/KiBarrierRenderer.java` | 6 | 0 | 0 | material mote volume, noise-eroded film, and hit ripple particle bursts; theme: technique-colored plasma, vapor, smoke, and impact dust |
| KiBeam | `src/client/java/com/dragonblockarcanedba/client/render/ki/KiBeamRenderer.java` | 1 | 0 | 0 | dense plasma emission along the path, impact plume, smoke, and vapor; theme: technique-colored plasma, vapor, smoke, and impact dust |
| KiBlast | `src/client/java/com/dragonblockarcanedba/client/render/ki/KiBlastRenderer.java` | 4 | 0 | 0 | dense plasma particle cluster with a material trail and impact plume; physical projectile models, where one exists, stay detailed models; theme: technique-colored plasma, vapor, smoke, and impact dust |
| KiDisk | `src/client/java/com/dragonblockarcanedba/client/render/ki/KiDiskRenderer.java` | 2 | 0 | 0 | dense plasma particle cluster with a material trail and impact plume; physical projectile models, where one exists, stay detailed models; theme: technique-colored plasma, vapor, smoke, and impact dust |
| KiExplosion | `src/client/java/com/dragonblockarcanedba/client/render/ki/KiExplosionRenderer.java` | 6 | 0 | 0 | material particles and vapor in the owner theme; theme: technique-colored plasma, vapor, smoke, and impact dust |
| KiLaser | `src/client/java/com/dragonblockarcanedba/client/render/ki/KiLaserRenderer.java` | 1 | 0 | 0 | dense plasma particle cluster with a material trail and impact plume; physical projectile models, where one exists, stay detailed models; theme: technique-colored plasma, vapor, smoke, and impact dust |
| KiSpiralBeam | `src/client/java/com/dragonblockarcanedba/client/render/ki/KiSpiralBeamRenderer.java` | 2 | 0 | 0 | dense plasma emission along the path, impact plume, smoke, and vapor; theme: technique-colored plasma, vapor, smoke, and impact dust |
| **Owner total** |  | **22** | **0** | **0** |  |

### Wish Ritual

| Effect | File | B | L | V | Must become |
|---|---|---:|---:|---:|---|
| WishRitualVisuals | `src/client/java/com/dragonblockarcanedba/client/wish/WishRitualVisuals.java` | 12 | 0 | 0 | golden plasma motes, soul wisps, mist, and dragon aura vapor; dragon balls remain physical objects, their glow becomes particles |
| **Owner total** |  | **12** | **0** | **0** |  |

### TimeShatter

| Effect | File | B | L | V | Must become |
|---|---|---:|---:|---:|---|
| TimeShatter | `src/client/java/com/dragonblockarcanedba/client/render/TimeShatterRenderer.java` | 11 | 0 | 0 | ground crack decals, dust, physical debris where present, and plasma carried in the cracks; theme: glass dust, temporal plasma, chromatic distortion |
| **Owner total** |  | **11** | **0** | **0** |  |

### Evil Spear Dread

| Effect | File | B | L | V | Must become |
|---|---|---:|---:|---:|---|
| DreadGaze | `src/client/java/com/dragonblockarcanedba/client/render/DreadGazeRenderer.java` | 2 | 0 | 8 | darkness smoke and vapor for the shroud; the detailed eye texture remains a detailed sprite, not a shape effect; theme: darkness smoke, vapor, detailed eye sprites |
| **Owner total** |  | **2** | **0** | **8** |  |

### Status Effects

Fresh scan finding: zero BespokeGeometry, LayeredRender geometry, or direct vertex calls exist in src/main/java/com/dragonblockarcanedba/effect. The status effect surface is particle presentation only. Twenty-eight effect classes were inventoried; EvilDreadEffect is cross-listed under Evil Spear Dread, leaving twenty-seven rows here. Each row must move from generic theme dust or VOID_SMOKE to its named material using the Phase 1 material family, at identical durations, amplifiers, and tick rates.

| Effect | File | Must become |
|---|---|---|
| AncientWeight | `src/main/java/com/dragonblockarcanedba/effect/AncientWeightEffect.java` | settling stone dust and earth vapor |
| BladeGuard | `src/main/java/com/dragonblockarcanedba/effect/BladeGuardEffect.java` | steel sparks and plasma motes shed on guard contact |
| Bleeding | `src/main/java/com/dragonblockarcanedba/effect/BleedingEffect.java` | crimson fluid droplets and red mist |
| CelestialGrace | `src/main/java/com/dragonblockarcanedba/effect/CelestialGraceEffect.java` | soul plasma and golden vapor |
| DarkFaded | `src/main/java/com/dragonblockarcanedba/effect/DarkFadedEffect.java` | darkness smoke and void plasma |
| DemonSurge | `src/main/java/com/dragonblockarcanedba/effect/DemonSurgeEffect.java` | hellfire embers and crimson smoke |
| DevilsHands | `src/main/java/com/dragonblockarcanedba/effect/DevilsHandsEffect.java` | hellfire flame and darkness smoke |
| EarthShatter | `src/main/java/com/dragonblockarcanedba/effect/EarthShatterEffect.java` | real block chunks and ground dust |
| EnergyOvercharge | `src/main/java/com/dragonblockarcanedba/effect/EnergyOverchargeEffect.java` | plasma and electric sparks |
| FissureStun | `src/main/java/com/dragonblockarcanedba/effect/FissureStunEffect.java` | ground dust and stone debris |
| HeroicFocus | `src/main/java/com/dragonblockarcanedba/effect/HeroicFocusEffect.java` | golden plasma motes |
| Hollowed | `src/main/java/com/dragonblockarcanedba/effect/HollowedEffect.java` | void plasma and darkness vapor |
| JudgementLock | `src/main/java/com/dragonblockarcanedba/effect/JudgementLockEffect.java` | temporal plasma and glass dust |
| MarkedByEvil | `src/main/java/com/dragonblockarcanedba/effect/MarkedByEvilEffect.java` | darkness smoke and crimson plasma |
| Melting | `src/main/java/com/dragonblockarcanedba/effect/MeltingEffect.java` | corrosive violet vapor, plasma, and smoke |
| MovementCurse | `src/main/java/com/dragonblockarcanedba/effect/MovementCurseEffect.java` | darkness smoke and curse plasma |
| OxBrace | `src/main/java/com/dragonblockarcanedba/effect/OxBraceEffect.java` | ground dust and ember sparks |
| PetrificationCurse | `src/main/java/com/dragonblockarcanedba/effect/PetrificationCurseEffect.java` | stone dust and gray vapor |
| PoleStun | `src/main/java/com/dragonblockarcanedba/effect/PoleStunEffect.java` | golden sparks and dust |
| Rifted | `src/main/java/com/dragonblockarcanedba/effect/RiftedEffect.java` | void plasma and darkness smoke |
| SilentMark | `src/main/java/com/dragonblockarcanedba/effect/SilentMarkEffect.java` | pale soul vapor |
| SorrowRift | `src/main/java/com/dragonblockarcanedba/effect/SorrowRiftEffect.java` | soul wisps and void plasma |
| SpiritImpale | `src/main/java/com/dragonblockarcanedba/effect/SpiritImpaleEffect.java` | soul plasma and golden wisps |
| StormOfDarkness | `src/main/java/com/dragonblockarcanedba/effect/StormOfDarknessEffect.java` | darkness smoke, ash, and storm vapor |
| TemporalStasis | `src/main/java/com/dragonblockarcanedba/effect/TemporalStasisEffect.java` | temporal plasma, glass dust, and mist |
| Valor | `src/main/java/com/dragonblockarcanedba/effect/ValorEffect.java` | valor plasma and golden motes |
| ValorStun | `src/main/java/com/dragonblockarcanedba/effect/ValorStunEffect.java` | valor plasma and impact dust |

### Mob Ability Effects

Fresh scan finding: zero visible geometry calls in the mob entities or their renderers. Mob bodies are physical GeckoLib models and are out of the geometry gate. The ability presentation rows are:

| Effect | File | Must become |
|---|---|---|
| Frieza Soldier ranged hit feedback | `src/main/java/com/dragonblockarcanedba/entity/FriezaSoldierEntity.java` | plasma sparks and a brief vapor plume at the hit point; the hit itself stays instant under the standing D4 decision |
| Saibaman self-destruct burst | `src/main/java/com/dragonblockarcanedba/entity/SaibamanEntity.java` | physical slime chunks stay physical; add green vapor and dust so the burst is not only chunks |
| Hostile boss stagger feedback | `src/client/java/com/dragonblockarcanedba/client/fx/StaggerFx.java` | impact dust and plasma in the attacker theme; no geometry exists here today |

### Storms and AtmosphereClient

Azure storm entities are owned by Azure Dragon Sword above. The shared client storm system has zero geometry calls:

| Surface | File | Must become |
|---|---|---|
| Storm rain and gloom | `src/client/java/com/dragonblockarcanedba/client/fx/AtmosphereClient.java` | detailed rain streaks, mist, and darkness vapor inside the storm volume; thunder light comes from the shared Bolt owner and thunder sound stays as is |
| Otherworld cloud field | `src/client/java/com/dragonblockarcanedba/client/particle/OtherworldCloudParticleManager.java` | mist and vapor flipbooks at cloud scale; no geometry exists here today |

### Forms and Player Auras

| Surface | File | Must become |
|---|---|---|
| Form aura emission | `src/client/java/com/dragonblockarcanedba/client/render/AuraRenderer.java` | PLASMA material particles tinted by the active form color, with vapor at the rim; zero geometry exists here today |

## Surfaces checked and not counted

- DebrisRenderer: real block models for physical debris. Exempt as a physical object renderer; the dust around debris impacts is owned by the calling weapon rows.
- DbaWeaponRenderer and ProceduralWeaponRenderer: detailed weapon models. Exempt as physical objects.
- Race, hair, tail, and mob model renderers: physical creature models. Exempt.
- FlyingNimbusRenderer: a physical vehicle model. Exempt from the effect geometry gate.
- DbaPostFx: screen-space post chains. Not world geometry; it is the existing honest home for shockwave and chromatic distortion support.

## Phase 2 owner list

The fan-out owners are: the twenty-two weapon owners named above, Shared weapon systems (SwingTrail and Bolt), Status Effects, Ki Techniques, Wish Ritual, Mob Ability Effects, Storms and AtmosphereClient, TimeShatter, Evil Spear Dread, and Forms and Player Auras. AuraRenderer has no geometry, so the Forms owner is a material conversion only. Every weapon owner also owns the LayeredRender.distScale references in its files if it removes the LayeredRender import entirely; distScale itself may move to FxEnvelope during the merge if that is cleaner.
