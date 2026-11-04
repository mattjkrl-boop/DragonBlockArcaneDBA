# Complex Effects Redo: Fresh Audit

Measured fresh from the code at dba d74f13a on Nov 4, 2026. This inventory is the Phase 2 work order for the complex effects redo, under audits/COMPLEX-STANDARD.md.

## Method and the current state

- "Now" is what the no-shapes pass left: every effect below is material particle emission only (DbaMaterialOptions materials through MaterialEmitters, or direct DbaMaterialOptions emission for status effects), with zero geometry in any effect renderer. The material mix per file was measured by counting MaterialEmitters.Preset calls in the current tree.
- Preset.fixed entries in the current renderers wrap physical or glyph options (BLOCK_CHUNKS, SPARK, RAIN, and similar), counted as filler, not as a complex layer.
- "Pieces" names the ComplexGeometry builder the effect must gain. "Complex particles" names the complex sprite types it must emit (SHARD_CHIP, SPARK_STREAK, WISP_CURL, EMBER_FRAGMENT, ASH_FLAKE). The existing material emission stays as the filler layer in every row.
- Classes are the standard's: S small, M standard ability, L signature. Class M and L rows require the pieces layer; class S rows require the complex particle layer.
- Debris note: where the owning entity already calls DebrisSpawner.spawnField, the big physical chunks already exist server side; the pieces layer there is for the fast near-field fragments and energy structure debris cannot time. Measured DebrisSpawner users include DevilSlamShockwave, KingsSlam, ZGravitySlam, OxShockwave, WarMaceSmash, LavaMeteor, LavaEruption, LavaCleave, AzureSonicQuake, AzureLightning, AzureTornado, BanshoCyclone, DimensionalSlash, DimensionalWarpRift, DimensionalRift, VoidRift, TemporalRift, SpiritImpale, GrandBladeShard, HeavenSplitter, HellHuntImpact, ErasureCannonBeam, BlasterBolt, BraveShockwave, PowerPoleWhirlwind, SpikeFlailSpin, DarknessDomain, and DelayedLaunch entities, plus several item-side calls.

## Work order by owner

### Katana (theme: ki plasma, air vapor, steel sparks)

| Effect | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| KatanaAimGuide | `client/render/KatanaAimGuideRenderer.java` | plasma 4, vapor 1 gather emission | tendrilFan: thin gathering filaments converging on the blade (M) | SPARK_STREAK drifting in |
| KatanaCharge | `client/render/KatanaChargeRenderer.java` | plasma 2, vapor 2, fixed 1 | tendrilFan inward lashes that grow with charge (M) | SPARK_STREAK, sparse WISP_CURL |
| HeavenSplitter | `client/render/HeavenSplitterRenderer.java` | plasma 4, vapor 3, fixed 1 | tendril: one thick torn strand along the split path; crackFan at the landing line (L) | SPARK_STREAK at contact, few SHARD_CHIP |
| SwiftCrescent | `client/render/SwiftCrescentRenderer.java` | plasma 5, vapor 1, fixed 2 | tendril along the crescent arc as the wave body (M) | SPARK_STREAK shed at the rim |

### Z Sword (theme: gravity plasma, stone dust, steel sparks)

| Effect | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| ZCharge | `client/render/ZChargeRenderer.java` | plasma 2, dust 1, vapor 1 | tendrilFan inward (M) | SPARK_STREAK |
| ZGravitySlam | `client/render/ZGravitySlamRenderer.java` | dust 2, plasma 2, vapor 1, fixed 1 | fracturePlates plus crackFan at the slam point; entity already spawns a DebrisSpawner field (L) | SHARD_CHIP burst, SPARK_STREAK |
| ZShockwave | `client/render/ZShockwaveRenderer.java` | plasma 3, dust 1, vapor 1 | crackFan at the origin, fracturePlates small (M) | SHARD_CHIP |
| ZStanceAura | `client/render/ZStanceAuraRenderer.java` | plasma 4, dust 2, vapor 1 | tendrilFan rising wisps (M) | WISP_CURL |
| DelayedLaunch | `client/render/DelayedLaunchRenderer.java` | plasma 2, dust 1, vapor 1 | crackFan telegraph web on the ground; tendril lash at the launch tick (M) | SPARK_STREAK at launch |

### Saber (theme: void plasma, spatial glass dust, darkness vapor)

| Effect | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| SaberSlash | `client/render/SaberSlashRenderer.java` | plasma 3, glassDust 2, darkness 1 | tendril body along the slash (M) | SHARD_CHIP spatial fragments, SPARK_STREAK |
| SaberDimensionalLineSlash | `client/render/SaberDimensionalLineSlashRenderer.java` | plasma 3, glassDust 2, darkness 1 | tendril body along the line (M) | SHARD_CHIP, SPARK_STREAK |
| SaberVoidTear | `client/render/SaberVoidTearRenderer.java` | plasma 3, glassDust 2, darkness 1 | shardBurst flat fan of spatial shards around the tear, tendrils streaming inward (M) | WISP_CURL, ASH_FLAKE |
| SaberDodgeSpark | `client/render/SaberDodgeSparkRenderer.java` | plasma 3, glassDust 2, darkness 1 | none (class S) | SPARK_STREAK, SHARD_CHIP |

### Brave Sword (theme: valor plasma, golden motes, impact dust)

| Effect | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| BraveCharge | `client/render/BraveChargeRenderer.java` | plasma 1, soul 1, dust 1, vapor 1 | tendrilFan gathering (M) | WISP_CURL |
| BraveCrossSlash | `client/render/BraveCrossSlashRenderer.java` | plasma 4, soul 2, vapor 1 | two crossed tendrils as the cross body (M) | SPARK_STREAK |
| BraveRushTrail | `client/render/BraveRushTrailRenderer.java` | plasma 3, vapor 2, dust 1 | tendril segments along the real rush path (M) | WISP_CURL shed |
| BraveShockwave | `client/render/BraveShockwaveRenderer.java` | plasma 3, dust 2, soul 1 | fracturePlates plus crackFan; entity already spawns debris (L) | SHARD_CHIP |
| BraveSlash | `client/render/BraveSlashRenderer.java` | plasma 4, vapor 1 | tendril body (M) | SPARK_STREAK |

### Grand Sword (theme: valor plasma, steel sparks, stone dust)

| Effect | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| GrandBladeShard | `client/render/GrandBladeShardRenderer.java` | plasma 2, dust 2, vapor 1, fixed 1 | shard mesh as the blade shard body (steel gradient), tendril wake (M) | SPARK_STREAK on shatter, SHARD_CHIP |
| GrandClashSpark | `client/render/GrandClashSparkRenderer.java` | plasma 1, dust 1, vapor 1, fixed 1 | tendrilFan, three short lashes at the clash point (M) | SPARK_STREAK burst |
| GrandCrescentWave | `client/render/GrandCrescentWaveRenderer.java` | plasma 2, vapor 1, fixed 1 | tendril arc body (M) | SPARK_STREAK at the rim |
| ValorField | `client/render/ValorFieldRenderer.java` | plasma 2, dust 1, vapor 1, fixed 1 | vertical tendrils lashing at the field rim; two to four slow shard meshes suspended in the volume, matching the field's projectile stasis identity (L) | WISP_CURL in the volume, SPARK_STREAK on suspend events |

### Azure Dragon Sword (theme: tempest plasma, rain, mist, lightning plasma)

| Effect | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| AzureLightning | `client/render/AzureLightningRenderer.java` | plasma 3, vapor 1; the strike itself is the shared BoltEntity | crackFan electric web crawling in the warned disc; tendrilFan small arcs during the telegraph (M) | SPARK_STREAK rising in the telegraph |
| AzureRushTrail | `client/render/AzureRushTrailRenderer.java` | plasma 2, vapor 1, fixed 1 | tendril along the rush path (M) | WISP_CURL |
| AzureSonicQuake | `client/render/AzureSonicQuakeRenderer.java` | dust 2, vapor 1, plasma 1, fixed 2 | fracturePlates plus crackFan; entity already spawns debris (L) | SHARD_CHIP |
| AzureStorm | `client/render/AzureStormRenderer.java` | plasma 3, vapor 2, dust 1, mist 1; bolts are BoltEntity spawns | tendrilFan wind lashes in the storm volume (M) | ASH_FLAKE, WISP_CURL wind-borne |
| AzureTempestChannel | `client/render/AzureTempestChannelRenderer.java` | plasma 3, dust 1, mist 1, vapor 1 | three tendrils with helical control points spiraling inward (M) | SPARK_STREAK |
| AzureTornado | `client/render/AzureTornadoRenderer.java` | vapor 1, mist 1, dust 1, plasma 1, fixed 2 | tendrilFan: four to six tall lashing strands around the column (L) | SHARD_CHIP orbiting debris, ASH_FLAKE |

### Dabura's Sword (theme: darkness smoke, hellish plasma, soul vapor)

| Effect | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| DarknessBlade | `client/render/DarknessBladeRenderer.java` | material 7 | tendril wake behind the falling blade (M) | EMBER_FRAGMENT, SPARK_STREAK at impacts |
| DarknessCharge | `client/render/DarknessChargeRenderer.java` | material 7 | tendrilFan inward darkness strands through submitVeil (M) | WISP_CURL |
| DarknessDomain | `client/render/DarknessDomainRenderer.java` | material 8 | rim tendrils through submitVeil; crackFan at the domain base (L) | ASH_FLAKE falling inside |
| DarknessShatter | `client/render/DarknessShatterRenderer.java` | material 7 | crackFan as the shatter web itself, fracturePlates dark (M) | SHARD_CHIP |
| DarknessWave | `client/render/DarknessWaveRenderer.java` | material 5 | tendril body along the wave (M) | EMBER_FRAGMENT shed |

### Cursed Blade (theme: curse plasma, darkness smoke, lightning plasma, ground dust)

| Effect | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| AbyssalDomain | `client/render/AbyssalDomainRenderer.java` | plasma 6, darkness 2, dust 1, smoke 1, vapor 1 | rim tendrils plus crackFan base web (L) | ASH_FLAKE |
| CurseChain | `client/render/CurseChainRenderer.java` | plasma 5, soul 3, darkness 2 | one taut low-curl tendril per chain path, caster to target (M) | SPARK_STREAK at the links |
| CurseGroundShatter | `client/render/CurseGroundShatterRenderer.java` | dust 3, plasma 3, darkness 2 | crackFan plus fracturePlates (M) | SHARD_CHIP |
| CurseLightning | `client/render/CurseLightningRenderer.java` | plasma 5, darkness 1, dust 1 | branchedLightning drawn in the renderer in curse colors, alongside the emission (M) | SPARK_STREAK |
| CurseTelegraph | `client/render/CurseTelegraphRenderer.java` | plasma 7, darkness 2, soul 1, dust 1 | crackFan warning web; tendrilFan gather (M) | SPARK_STREAK |
| SkyCracks | `client/render/SkyCracksRenderer.java` | plasma 3, soul 1, darkness 1 | crackFan on the rift plane; shardBurst of dark shards falling from the cracks (M) | WISP_CURL |

### Hollow's Edge (theme: soul wisps, void plasma, darkness smoke)

| Effect | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| HollowAfterimage | `client/render/HollowAfterimageRenderer.java` | soul 2, plasma 2, vapor 2, darkness 1 | tendrilFan wisps shaped around the silhouette, veil tendrils (M) | WISP_CURL dense |
| HollowCharge | `client/render/HollowChargeRenderer.java` | plasma 3, darkness 1, dust 1, soul 1 | tendrilFan inward (M) | WISP_CURL |
| HollowRushTrail | `client/render/HollowRushTrailRenderer.java` | plasma 4, vapor 2, darkness 1 | tendril along the rush path (M) | WISP_CURL |
| VoidRift | `client/render/VoidRiftRenderer.java` | soul 2, plasma 2, darkness 1, vapor 1 | shardBurst ring of void shards; tendrils streaming inward (M) | ASH_FLAKE |
| VoidSlash | `client/render/VoidSlashRenderer.java` | plasma 2, soul 1, vapor 1 | tendril body (M) | WISP_CURL shed |

### Dimensional Sword (theme: void plasma, spatial glass dust, darkness vapor)

| Effect | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| DimensionalSlash | `client/render/DimensionalSlashRenderer.java` | plasma 5, darkness 1, glassDust 1, vapor 1 | tendril body: one thick strand with torn pinches for the flying slash (L) | SHARD_CHIP spatial fragments at the rim |
| DimensionalWarpRift | `client/render/DimensionalWarpRiftRenderer.java` | plasma 6, darkness 1, glassDust 1, vapor 1 | shardBurst fan at the tear, inward tendrils, crackFan on the rift plane (M) | WISP_CURL |

### Spirit Sword (theme: soul plasma, spirit wisps, vapor)

| Effect | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| SpiritCannonBeam | `client/render/SpiritCannonBeamRenderer.java` | soul 4, vapor 2, plasma 1, fixed 1 | tendril core along the beam; tendrilFan impact lashes (L) | SPARK_STREAK at the terminus |
| SpiritCleave | `client/render/SpiritCleaveRenderer.java` | soul 3, vapor 1, fixed 1 | tendril body (M) | WISP_CURL |
| SpiritImpale | `client/render/SpiritImpaleRenderer.java` | soul 3, vapor 2, plasma 1 | shard mesh as the spirit spike body (soul gradient), tendril wake (M) | WISP_CURL |

### Evil Spear (theme: darkness smoke, hellfire embers, soul vapor)

| Effect | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| EvilSpearCharge | `client/render/EvilSpearChargeRenderer.java` | darkness 1, ember 1, flame 1, plasma 1, soul 1 | tendrilFan inward; flameTongues small at the spear tip (M) | EMBER_FRAGMENT |
| EvilSpearProjectile | `client/render/EvilSpearProjectileRenderer.java` | darkness 1, ember 1, flame 1, plasma 1, soul 1 | tendril wake; flameTongues at the head (M) | EMBER_FRAGMENT trail |
| HellHuntImpact | `client/render/HellHuntImpactRenderer.java` | darkness 1, ember 1, flame 1, plasma 1, soul 1 | crackFan plus tendrilFan burst lashes (M) | EMBER_FRAGMENT, SPARK_STREAK |

### Devil Trident (theme: hellfire flame, magma embers, brimstone smoke, ground dust)

This is the reference picture's weapon: the slam composition in the picture is the literal target for DevilSlamShockwave.

| Effect | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| DevilSlamShockwave | `client/render/DevilSlamShockwaveRenderer.java` | plasma 4, dust 2, flame 2, ember 1, smoke 1 | fracturePlates brimstone fan plus crackFan magma web plus flameTongues rising from the crater ring; entity already spawns a DebrisSpawner field (L) | EMBER_FRAGMENT and SHARD_CHIP bursts, ASH_FLAKE after |
| TridentProng | `client/render/TridentProngRenderer.java` | plasma 2, ember 1, flame 1 | shard meshes as the prong heads (hellfire gradient, submitSolid), flameTongues at the base, tendril wake (M) | EMBER_FRAGMENT |
| TridentShard | `client/render/TridentShardRenderer.java` | plasma 2, ember 1, flame 1 | shard mesh as the shard body: a jagged hellfire fragment, restoring the silhouette the no-shapes report flagged as lost, at complex piece quality rather than the old clean crescent; tendril wake (M) | EMBER_FRAGMENT shed |

### Sickle of Sorrow (theme: soul wisps, void plasma, darkness smoke)

| Effect | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| DimensionalRift | `client/render/DimensionalRiftRenderer.java` | darkness 2, soul 2, plasma 2, glassDust 1 | shardBurst at the tear; tendrils streaming inward (M) | WISP_CURL |
| SorrowSlash | `client/render/SorrowSlashRenderer.java` | plasma 3, darkness 2, soul 2, vapor 1 | tendril body (M) | WISP_CURL |

### Ox King's Ax (theme: ground dust, stone debris, shock vapor, embers)

| Effect | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| KingsSlam | `client/render/KingsSlamRenderer.java` | dust 1, plasma 2, vapor 2, ember 1, fixed 1 | fracturePlates plus crackFan; entity already spawns a DebrisSpawner field (L) | SHARD_CHIP burst, large |
| OxCharge | `client/render/OxChargeRenderer.java` | dust 1, ember 1, plasma 1, vapor 1, fixed 1 | tendrilFan dust strands pulled inward (M) | SHARD_CHIP gathering chips |
| OxFissure | `client/render/OxFissureRenderer.java` | plasma 2, dust 1, ember 1, fixed 1 | crackFan along the fissure line as its glowing core; fracturePlates at the origin (M) | SHARD_CHIP |
| OxShockwave | `client/render/OxShockwaveRenderer.java` | dust 1, ember 1, plasma 1, vapor 1, fixed 1 | fracturePlates at the origin plus crackFan; entity already spawns debris (L) | SHARD_CHIP |
| OxStanceAura | `client/render/OxStanceAuraRenderer.java` | plasma 3, dust 1, ember 1, vapor 1 | tendrilFan rising dust strands (M) | ASH_FLAKE |

### Power Pole (theme: air vapor, impact dust, steel sparks)

| Effect | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| PowerPoleExtension | `client/render/PowerPoleExtensionRenderer.java` | plasma 4, dust 1, vapor 1, fixed 1 | tendril wind strand along the extension path (M) | SPARK_STREAK at the tip |
| PowerPoleImpact | `client/render/PowerPoleImpactRenderer.java` | dust 2, plasma 3, vapor 2, fixed 2 | crackFan small at the impact (M) | SHARD_CHIP burst |
| PowerPoleWhirlwind | `client/render/PowerPoleWhirlwindRenderer.java` | vapor 2, dust 1, plasma 1, fixed 1 | tendrilFan circling strands (M) | ASH_FLAKE airborne |

### Whis Staff (theme: temporal plasma, glass dust, mist)

| Effect | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| TemporalRift | `client/render/TemporalRiftRenderer.java` | glassDust 2, plasma 2, darkness 1, mist 1, soul 1 | shardBurst of temporal glass shards at the tear; tendrils streaming inward (M) | WISP_CURL |

### Bansho Fan (theme: wind vapor, airborne dust, mist)

| Effect | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| BanshoCyclone | `client/render/BanshoCycloneRenderer.java` | vapor 2, dust 1, mist 1 | tendrilFan tall wind strands around the column (L) | ASH_FLAKE, SHARD_CHIP orbiting |
| BanshoGale | `client/render/BanshoGaleRenderer.java` | vapor 1, plasma 1, dust 1, mist 1 | tendril along the gale path (M) | WISP_CURL |
| BanshoShockwave | `client/render/BanshoShockwaveRenderer.java` | vapor 3, dust 2, plasma 1, mist 1, fixed 1 | crackFan plus light fracturePlates (wind scour) (M) | SHARD_CHIP, few |
| BanshoWindProjectile | `client/render/BanshoWindProjectileRenderer.java` | vapor 1, dust 1, mist 1, plasma 1 | double tendril spiral wake (M) | WISP_CURL |

### Blaster Gun (theme: ki plasma, steel sparks, smoke, vapor)

| Effect | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| BlasterBolt | `client/render/BlasterBoltRenderer.java` | plasma 3, vapor 1, fixed 1 | short tendril wake (M) | SPARK_STREAK at impact |
| ErasureCannonBeam | `client/render/ErasureCannonBeamRenderer.java` | plasma 7, vapor 2, smoke 1, fixed 1 | tendril core, thick and white-hot at the root; tendrilFan terminus lashes; crackFan at ground contact (L) | SPARK_STREAK heavy at the terminus |
| ErasureChargeOrb | `client/render/ErasureChargeOrbRenderer.java` | plasma 4, vapor 1, ember 1, fixed 1 | tendrilFan lashes orbiting the orb (M) | SPARK_STREAK, WISP_CURL |

### Lava Sword (theme: flame, magma embers, smoke, ash, vapor)

| Effect | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| LavaBlob | `client/render/LavaBlobRenderer.java` | flame 1, ember 1, smoke 1, ash 1, vapor 1 | flameTongues on the blob crown (M) | EMBER_FRAGMENT trail |
| LavaCleave | `client/render/LavaCleaveRenderer.java` | flame 1, ember 1, smoke 1, ash 1, vapor 1 | tendril body in flame gradient; flameTongues at contact (M) | EMBER_FRAGMENT |
| LavaEruption | `client/render/LavaEruptionRenderer.java` | flame 1, ember 1, smoke 1, ash 1, vapor 1 | flameTongues crown at full height; crackFan at the base (L) | EMBER_FRAGMENT, ASH_FLAKE |
| LavaGeyser | `client/render/LavaGeyserRenderer.java` | flame 1, ember 1, smoke 1, ash 1, vapor 1 | flameTongues column crown (M) | EMBER_FRAGMENT |
| LavaMeteor | `client/render/LavaMeteorRenderer.java` | flame 2, ember 1, smoke 1, ash 1, vapor 1, fixed 1 | flameTongues tail cluster; fracturePlates plus crackFan at impact (L) | EMBER_FRAGMENT heavy |

### Spike Flail (theme: air vapor, steel sparks, ground dust)

| Effect | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| SpikeFlailSpin | `client/render/SpikeFlailSpinRenderer.java` | dust 4, vapor 4, plasma 1, fixed 3 | tendril arcs following the sampled head path (M) | SPARK_STREAK at contacts |
| SpikeFlailSwing | `client/render/SpikeFlailSwingRenderer.java` | vapor 2, dust 1, plasma 1, fixed 1 | tendril body along the swing arc (M) | SPARK_STREAK |

### War Mace (theme: stone dust, physical debris, steel sparks, shock vapor)

| Effect | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| WarMaceRise | `client/render/WarMaceRiseRenderer.java` | plasma 4, vapor 2, dust 1, fixed 1 | crackFan plus small fracturePlates at the rise point (M) | SHARD_CHIP |
| WarMaceSmash | `client/render/WarMaceSmashRenderer.java` | dust 6, plasma 3, vapor 2, ember 1, fixed 2 | fracturePlates at the full plate budget (the crater flagship) plus crackFan; entity already spawns a DebrisSpawner field (L) | SHARD_CHIP burst |

### Shared weapon systems

| Effect | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| SwingTrail | `client/fx/SwingTrailRenderer.java` | swing-driven material emission along the sampled swing path, per-weapon profiles | a tendril following the sampled swing path (path points become bezier controls), torn pinches at the arc ends, colors from WeaponTrailProfiles (L, it serves every weapon) | SPARK_STREAK at the tip for steel themes |
| Bolt | `client/render/BoltRenderer.java` | the seeded branch path exists as data; the strike is layered plasma emission along it, plus SPARK fixed preset and ground glow | branchedLightning drawn on submitEnergy from the same seeded path data: trunk, forks, and sub-branches as shader pieces, with the existing emission kept as filler (L, flagship conversion) | existing SPARK emission stays; add SPARK_STREAK at the strike point |

### Ki Techniques (theme: technique-colored plasma, vapor, smoke, impact dust)

| Effect | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| KiBarrier | `client/render/ki/KiBarrierRenderer.java` | plasma 4, vapor 1 | vertical tendril lashes standing on the barrier boundary (L) | WISP_CURL in the volume |
| KiBeam | `client/render/ki/KiBeamRenderer.java` | plasma 3, smoke 1, vapor 1 | tendril core along the beam; tendrilFan impact lashes (L) | SPARK_STREAK at the terminus |
| KiBlast | `client/render/ki/KiBlastRenderer.java` | plasma 3, vapor 1 | tendril wake (M) | SPARK_STREAK at impact |
| KiDisk | `client/render/ki/KiDiskRenderer.java` | plasma 3, ember 1, vapor 1 | tendrilFan: three short lashes spinning at the rim as the cutting wake (M) | SPARK_STREAK shed at the rim |
| KiExplosion | `client/render/ki/KiExplosionRenderer.java` | plasma 2, dust 1, smoke 1, vapor 1 | fracturePlates plus crackFan at the blast center; short flameTongues in technique colors (L) | SHARD_CHIP |
| KiLaser | `client/render/ki/KiLaserRenderer.java` | plasma 3, vapor 1 | thin tendril core (M) | SPARK_STREAK |
| KiSpiralBeam | `client/render/ki/KiSpiralBeamRenderer.java` | plasma 4, smoke 1, vapor 1 | two tendrils with opposed helical controls as the spiral body; tendrilFan at impact (L) | SPARK_STREAK |

KiBarrier hit ripples remain blocked on the entity having no synced hit field; that is a data change, out of this pass, and stays Carter's call exactly as the no-shapes merge recorded it.

### Wish Ritual

| Effect | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| WishRitualVisuals | `client/wish/WishRitualVisuals.java` | plasma 6, mist 4, soul 3, vapor 1 | tendrilFan golden strands rising around the dragon balls; small flameTongues in gold at the ritual peak (M) | WISP_CURL |

### TimeShatter (theme: glass dust, temporal plasma, chromatic distortion)

| Effect | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| TimeShatter | `client/render/TimeShatterRenderer.java` | glassDust 8 (gravity scale 0.1 suspension from the no-shapes merge), plasma 1, soul 1 | shardBurst: a frozen fan of pale temporal glass shards; crackFan on the shatter plane (M) | SHARD_CHIP as the glass chip layer |

### Evil Spear Dread

| Effect | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| DreadGaze | `client/render/DreadGazeRenderer.java` | darkness 1, smoke 1 shroud emission; eyes are detailed textured sprites on a quad path | tendril wisps through submitVeil with the TENDRIL canvas in near-black colors, curling through the shroud (M) | ASH_FLAKE drift |

The eye sprites stay as they are: detailed physical sprites, out of the pieces gate, as in every prior pass.

### Status Effects (class S; files in `src/main/java/com/dragonblockarcanedba/effect/`)

All twenty-seven classes currently emit DbaMaterialOptions materials directly at their existing tick points. Each gains one complex sprite layer at the same tick points and rates, matched to its material family. No geometry: these are per-entity auras at class S size.

| Family | Effects | Complex layer |
|---|---|---|
| Hellfire | DemonSurgeEffect, DevilsHandsEffect | EMBER_FRAGMENT |
| Earth and stone | AncientWeightEffect, EarthShatterEffect, FissureStunEffect, OxBraceEffect, PetrificationCurseEffect | SHARD_CHIP |
| Temporal glass | JudgementLockEffect, TemporalStasisEffect | SHARD_CHIP (glass read) |
| Soul and valor | CelestialGraceEffect, HeroicFocusEffect, SilentMarkEffect, SorrowRiftEffect, SpiritImpaleEffect, ValorEffect, ValorStunEffect | WISP_CURL |
| Electric and guard | BladeGuardEffect, EnergyOverchargeEffect, PoleStunEffect | SPARK_STREAK |
| Darkness and void | DarkFadedEffect, HollowedEffect, MarkedByEvilEffect, MovementCurseEffect, RiftedEffect, StormOfDarknessEffect | ASH_FLAKE |
| Fluid and corrosive | BleedingEffect, MeltingEffect | none: no complex sprite silhouette matches fluid droplets or corrosive vapor, and forcing a fragment sprite here would read wrong. Recorded as a deliberate class S exemption; revisit only if a droplet sprite set is ever generated. |

### Mob Ability Effects (class S)

| Effect | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| Frieza Soldier ranged hit feedback | `entity/FriezaSoldierEntity.java` | material sparks at the hit point | none | SPARK_STREAK at the hit point |
| Saibaman self-destruct burst | `entity/SaibamanEntity.java` | physical slime chunks plus material burst | none; chunks stay physical | WISP_CURL, ASH_FLAKE |
| Hostile boss stagger feedback | `client/fx/StaggerFx.java` | dust 1, plasma 1 | none | SPARK_STREAK, SHARD_CHIP burst |

### Storms and AtmosphereClient

| Surface | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| Storm rain and gloom | `client/fx/AtmosphereClient.java` | material 4 (rain, mist, darkness, vapor inside the storm volume) | none in the client system itself; thunder is the shared Bolt owner's geometry (M system) | WISP_CURL wind wisps, ASH_FLAKE airborne grit inside the volume |
| Otherworld cloud field | `client/particle/OtherworldCloudParticleManager.java` | CLOUD_PUFF flipbook field | none: a background sky field, not an effect body; recorded as a deliberate exemption | none |

### Forms and Player Auras

| Surface | File | Now | Pieces to add | Complex particles |
|---|---|---|---|---|
| Form aura emission | `client/render/AuraRenderer.java` | plasma 1, soul 1, vapor 1 in form colors | flameTongues aura lashes in the active form colors rising around the player (the aura read from the reference picture), plus tendrilFan wisps at high form grades (L) | WISP_CURL shed |

## Phase 2 owner list

The fan-out owners are the same thirty-one slots as the no-shapes pass: the twenty-two weapon owners named above, Shared weapon systems (SwingTrail and Bolt), Ki Techniques, Wish Ritual, TimeShatter, Evil Spear Dread, Status Effects, Mob Ability Effects, Storms and AtmosphereClient, and Forms and Player Auras. Every owner also owns the filler emission already in its files: the pieces and complex particle layers are added to it, never traded against it, and themes, colors, damage, cooldowns, costs, timing, mechanics, and synced fields stay identical.
