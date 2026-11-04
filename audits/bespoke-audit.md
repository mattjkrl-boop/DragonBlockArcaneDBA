# Bespoke Pass Audit (Phase 1, coordinating pass)

Inventory at HEAD 57902e1, taken from the code. Weapon scope means: the 22
weapon item classes, their ability entities (entity/ minus mobs, dragons,
the nimbus vehicle, and the Ki technique entities), their renderers
(client/render minus mob, guide, nimbus, aura, and skin files), the shared
fx/ and util/ systems the weapons use, client/fx weapon systems
(ChargeFx, SwingTrailRenderer, WeaponTrailProfiles, DbaPostFx,
AtmosphereClient), and client/particle. Out of scope and not counted:
menus, races and forms, mobs, Ki techniques (ki/ package and its entities
and renderers), the wish ritual, UI sounds, dimension ambience.

## 1. Geometry: primitives and hand-rolled regular geometry

KiRenderHelper (client/render/ki/KiRenderHelper.java) defines five
primitive builders: drawColoredBox (axis-aligned box), drawCylinder
(equal-segment tube), drawDisc (equal-segment ring fan), drawSphere
(latitude-longitude ball), drawHelix (uniform helix ribbon). Call sites
across the whole source tree: 35 usage lines. Inside weapon scope only 11:
HollowAfterimageRenderer 9 (all drawColoredBox) and SpikeFlailSwingRenderer
2 (drawSphere). The rest belong to Ki technique renderers (16) and the wish
ritual visuals (8), both out of scope.

The primitives are NOT the main problem. The main problem is hand-rolled
regular geometry inside the weapon renderers themselves:

- 84 renderer files in weapon scope; 82 of them contain Math.cos / Math.sin
  segment loops, about 1,821 trig call lines in total. Rings, fans, cones,
  domes, discs, and cylinders are rebuilt per renderer from equal segments.
- 73 of the 84 emit vertices directly through VertexConsumer.addVertex.
- About 120 local draw helpers are defined across those files, with heavy
  duplication of the same regular shapes: drawQuad defined about 30 times,
  drawRotatingRing about 19, drawTriangle about 15, drawGroundRing about
  11, drawTrenchSegment 9, drawLash 9, plus octahedra, shock rings, domes,
  crescents, spikes, slabs, and ribbons. Call sites for the biggest:
  drawQuad 149, drawRotatingRing 82, drawTriangle 60, drawGroundRing 51.
- The shared LayeredRender (client/render/ki/LayeredRender.java) is also
  regular: drawVortexBands stacks 6 equal rings of 28 equal segments
  (18 call sites), drawAnchorGlow is an equal-segment fan disc
  (26 call sites). LayeredRender.distScale has 45 call sites and must
  keep working unchanged.

Verdict: geometry in weapon scope is regular and duplicated. The fix is
one seeded procedural toolkit (BespokeGeometry) plus a bespoke rework of
the two LayeredRender builders, both delivered in this Phase 1.

## 2. Particles: vanilla types in weapon scope

23 references to vanilla ParticleTypes in 16 weapon-scope files, plus the
vanilla DustParticleOptions used as the theme dust (7 references in 2
files, one of them LayeredParticles itself, which fans out everywhere):

| Vanilla type | Refs | Files | Use |
|---|---|---|---|
| POOF | 5 | ZShockwaveEntity, WarMaceSmashEntity, DebrisEntity, fx/SlamDebris, util/LayeredParticles | ground burst puffs, dissipation, debris landing dust |
| CRIT | 4 | BraveSwordItem, BraveSlashEntity, GrandBladeShardEntity, (+1 entity) | hit sparkle on strikes |
| FLAME | 4 | LavaSwordItem, LavaCleaveEntity, LavaEruptionEntity | fire trail and ambient flame |
| EXPLOSION_EMITTER | 4 | OxKingsAxeItem, OxShockwaveEntity, ErasureCannonBeamEntity | large explosion burst |
| EXPLOSION | 2 | BlasterBoltEntity, OxShockwaveEntity | impact burst |
| BLOCK | 2 | OxShockwaveEntity, fx/SlamDebris | block crumble chunks (BlockParticleOption with a real block state) |
| DustParticleOptions | 7 | util/LayeredParticles, +1 | the theme-colored mid layer of every layered emission |

Outside weapon scope and excluded: ParticleTypes.CLOUD x2 in
client/particle/OtherworldCloudParticleManager (dimension ambience, not a
weapon effect).

The mod's own particle system today: DbaParticles registers 5
SimpleParticleTypes (SPARK, EMBER, VOID_SMOKE, WIND, KI_MOTE), rendered by
client/particle/DbaGlowParticle with per-type Style records (color ramp,
life, size, growth, gravity, drag, additive flag), 4-frame textures each
under textures/particle/, JSON definitions under assets/particles/.
Providers are registered in client/particle/DbaParticleProviders.

Gap: no custom equivalent exists for a soft dust puff (POOF), a crit
sparkle (CRIT), a flame tongue (FLAME), a large blast puff (EXPLOSION
family), a block-state chunk (BLOCK), a colored theme dust
(DustParticleOptions), ash or wisp variants for the dark and spirit
themes, or rain for the storm rework (section 5).

## 3. Sounds: vanilla SoundEvents in weapon scope

185 references, 47 distinct vanilla events, 55 files. The existing
DbaSounds bank has 10 ability sounds (ability_charge, blade_shing,
blaster_shot, dark_pulse, heavy_impact, lightning_zap, saber_hum,
slash_whoosh, time_stop, wind_gust) next to 7 non-weapon sounds; weapons
use a couple of them and fake everything else with vanilla events:

| Use case | Vanilla events faking it | Refs |
|---|---|---|
| Swing whoosh | PLAYER_ATTACK_SWEEP | 29 |
| Crit / hard hit | PLAYER_ATTACK_CRIT | 7 |
| Explosion | GENERIC_EXPLODE | 24 |
| Sonic boom (beams, slams) | WARDEN_SONIC_BOOM | 6 |
| Heavy slam thud | ANVIL_LAND 14, ANVIL_HIT 1, HEAVY_CORE_HIT 1 | 16 |
| Thunder | LIGHTNING_BOLT_THUNDER | 8 |
| Lightning zap / impact | LIGHTNING_BOLT_IMPACT 6, TRIDENT_THUNDER 1 | 7 |
| Charge start / activate | BEACON_ACTIVATE 6, RESPAWN_ANCHOR_SET_SPAWN 3 | 9 |
| Charge hum (sustained) | BEACON_AMBIENT | 5 |
| Charge milestone / full | BEACON_POWER_SELECT 2, WARDEN_SONIC_CHARGE 3, RESPAWN_ANCHOR_DEPLETE 4 | 9 |
| Flight / rush loop | ELYTRA_FLYING | 5 |
| Wind burst / throw | WIND_CHARGE_BURST 5, WIND_CHARGE_THROW 1 | 6 |
| Teleport / warp | ENDERMAN_TELEPORT 4, PORTAL_TRAVEL 1 | 5 |
| Rift / portal ambience | PORTAL_AMBIENT | 3 |
| Fire shot / ignite | BLAZE_SHOOT 3, FIRECHARGE_USE 1 | 4 |
| Lava | LAVA_POP 2, FIRE_EXTINGUISH 2 | 4 |
| Crystal / chime | AMETHYST_CLUSTER_BREAK 4, AMETHYST_BLOCK_RESONATE 1, GLASS_BREAK 1 | 6 |
| Chains | CHAIN_PLACE 2, CHAIN_HIT 2 | 4 |
| Trident | TRIDENT_HIT 4, TRIDENT_THROW 2, TRIDENT_HIT_GROUND 1 | 7 |
| Magic cast shimmer | ENCHANTMENT_TABLE_USE 3, EVOKER_PREPARE_SUMMON 1, ILLUSIONER_CAST_SPELL 1 | 5 |
| Dark conversion / curse | ZOMBIE_VILLAGER_CONVERTED 1, WARDEN_HEARTBEAT 1, SOUL_ESCAPE 2 | 4 |
| Block / parry | SHIELD_BLOCK 2, SHIELD_BREAK 1, ARMOR_STAND_HIT 1 | 4 |
| Break / fizzle / misc | ITEM_BREAK 3, DISPENSER_FAIL 3, ARROW_HIT 2, ENDERMAN_AMBIENT 1 | 9 |

## 4. Damage: how ability damage builds its DamageSource today

149 damageSources() references across weapon item, entity, util, and fx
files. Every one uses a vanilla damage type:

| Vanilla source | Refs | Notes |
|---|---|---|
| playerAttack | 63 | the majority of ability damage, not just melee |
| generic | 22 | includes MobPhysics fall/stagger damage x2 |
| magic | 21 | spirit, temporal, percent-health style hits |
| mobAttack | 18 | ability entities posing as mobs |
| mobProjectile | 15 | thrown and shot abilities |
| indirectMagic | 7 | rift and beam style indirect hits |
| lightningBolt | 1 | Azure lightning |
| inWall | 1 | crush-in-wall case |
| hotFloor | 1 | standing in ground fire case |

There are no mod damage types today (no data/dragonblockarcanedba/
damage_type/ directory) and no death.attack entries in the lang file.
Melee weapon hits themselves go through the vanilla item attack path,
which is the game's own player attack; that stays (see the standard).

## 5. Storms and debris: the two vanilla render paths

AtmosphereServer (util/AtmosphereServer.java) writes REAL vanilla
weather: it saves the level WeatherData on the first storm, forces
setRaining(true) with rain and thunder timers of 20 minutes, refreshes
the timers every tick, and restores the saved weather when the last
storm expires. AtmosphereClient only lerps the gloom grade (read by
DbaPostFx) from AtmospherePayload(x, y, z, radius, gloom, ticks); the
rain and thunder the player sees and hears are vanilla weather effects.
Callers: the Azure storm, Dabura darkness domain, Curse tempest, and
Bansho cyclone systems, via AtmosphereServer.begin(level, center,
radius, gloom, Kind, ticks) with Kind RAIN or THUNDER.

DebrisRenderer (client/render/DebrisRenderer.java) renders DebrisEntity
through net.minecraft.client.renderer.block.MovingBlockRenderState and
SubmitNodeCollector.submitMovingBlock, mirroring vanilla
FallingBlockRenderer: the state carries randomSeedPos, blockPos,
blockState, biome, cardinalLighting, and lightEngine, so tint and
lighting match the world exactly. FlyingNimbusRenderer uses the same
path (vehicle, out of scope).

Rule 6 API check (javap against BOTH audit jars, 26.2 client jar and
26.3 mc263.jar): a clean alternative exists with IDENTICAL signatures
on both versions:

- net.minecraft.client.renderer.block.BlockModelRenderState:
  submit(PoseStack, SubmitNodeCollector, int, int, int) and isEmpty(),
  same on 26.2 and 26.3.
- net.minecraft.client.renderer.block.BlockModelResolver:
  update(BlockModelRenderState, BlockState, BlockDisplayContext),
  same on both; instances come from
  EntityRendererProvider.Context.getBlockModelResolver().
- net.minecraft.client.renderer.block.model.BlockDisplayContext.create()
  is the context vanilla uses (EndermanRenderer carries a static
  BLOCK_DISPLAY_CONTEXT built exactly this way for its carried block,
  the closest vanilla analogue to debris: a block model drawn inside an
  entity renderer with a custom pose).
- The submit argument triple is (packed light coords,
  OverlayTexture.NO_OVERLAY, outline color), copied from
  CarriedBlockLayer's call in both jars.

So DebrisRenderer can leave MovingBlockRenderState behind with no
DbaCompat shim. The one honest trade: MovingBlockRenderState samples
the light engine and cardinal lighting at the block position for a
per-quad world look; the BlockModelRenderState path takes the entity's
packed light coords and the model's baked tints, the same trade vanilla
itself makes for carried blocks and block displays.

Also present on both versions with identical signatures:
OrderedSubmitNodeCollector.submitBlockModel(PoseStack, RenderType,
List<BlockStateModelPart>, int[], int, int, int) and
submitCustomGeometry (which the weapon renderers already use).

## 6. Textures

Weapon effect geometry draws through the mod's own white canvas
(textures/entity/ki_white.png) via KiRenderHelper render types, colored
by vertex color. Particle textures are the mod's own (5 types x 4
frames). The Azure and Curse lightning textures
(textures/entity/azure_lightning*.png, curse_lightning*.png) are the
mod's own files. The ONLY vanilla texture reference under client/render
is OtherworldGuideRenderer's villager.png, a mob renderer, out of
scope. Verdict: weapon effect textures are already the mod's own; new
particle textures must stay that way (procedural generation, section 7
of the standard).

## 7. Foundation gaps this Phase 1 must close

1. BespokeGeometry: one seeded procedural toolkit covering torn ribbon,
   shard fan, blade arc, displaced dome, broken vortex band, branch,
   ground crack star, plume.
2. LayeredRender: drawVortexBands and drawAnchorGlow rebuilt bespoke
   on the toolkit, same signatures.
3. Particles: custom types for dust puff, crit sparkle, flame, blast,
   ash, wisp, rain; a colored theme dust options type replacing
   DustParticleOptions; a block-state chunk options type replacing
   ParticleTypes.BLOCK; LayeredParticles switched to all-custom types.
4. Sounds: DbaSounds extended to cover every use case in section 3,
   procedurally synthesized, organized by use with theme variants.
5. Damage: 12 theme-family damage types with death messages and one
   DbaDamage factory class.
6. Atmosphere: server stops writing WeatherData; payload gains the
   storm kind; client renders rain, plays thunder, keeps gloom.
7. DebrisRenderer: moved to BlockModelRenderState per section 5.
