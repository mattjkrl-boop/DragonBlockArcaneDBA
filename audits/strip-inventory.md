# Weapon Attack Strip Inventory (pre-strip survey, HEAD 9834fb3)

Survey taken before any deletion, by full-tree reference analysis: every
candidate class was checked for referencers outside the weapon layer, and
each shared system was resolved before cutting. Counts: 204 files slated
for deletion (109 main source set, 95 client source set), 22 weapon item
classes slated for gutting to plain items, 17 surviving files slated for
surgical edits, 1 particle JSON slated for deletion.

## 1. Weapon items (22, gut to plain Item shells, registration kept)

KatanaItem, ZSwordItem, SaberItem, BraveSwordItem, GrandSwordItem,
AzureDragonSwordItem, DaburaSwordItem, CurseBladeItem (file name), 
HollowsEdgeItem, DimensionalSwordItem, SpiritSwordItem, EvilSpearItem,
DevilTridentItem, SickleOfSorrowItem, OxKingsAxeItem, PowerPoleItem,
WhisStaffItem, BanshoFanItem, BlasterGunItem, LavaSwordItem,
SpikeFlailItem, WarMaceItem. Each currently 300 to 480 lines of ability
logic (charges, stances, entity spawns, Ki drains, cooldowns). All take a
single Item.Properties constructor in DbaItems and implement
item/geo/DbaGeoWeapon, whose GeoItem methods are all interface defaults,
so a plain shell keeps registration, item model, and texture intact.
BlasterAmmoItem (12 lines) exists only as Blaster Gun ammunition:
delete class and registration.

## 2. Weapon ability entities (90 classes plus 1 interface, delete)

All entity/ classes serving weapon attacks: projectiles (BlasterBolt,
Bolt, TridentProng, EvilSpearProjectile, DimensionalSlash,
BanshoWindProjectile), fields and domains (ValorField, DarknessDomain,
AbyssalDomain, DimensionalRift, VoidRift, TemporalRift, AzureStorm),
waves and slams (GrandCrescentWave, ZShockwave, ZGravitySlam,
KingsSlam, WarMaceSmash, WarMaceRise, DevilSlamShockwave,
HellHuntImpact, OxShockwave, OxFissure, BanshoShockwave, CurseGroundShatter,
DarknessShatter, DelayedLaunch), charges and auras (KatanaCharge,
KatanaAimGuide, ZCharge, ZStanceAura, HollowCharge, BraveCharge,
DarknessCharge, ErasureChargeOrb, EvilSpearCharge, OxCharge,
OxStanceAura), trails and afterimages (HollowAfterimage,
HollowRushTrail, AzureRushTrail, BraveRushTrail), lava family
(LavaCleave, LavaEruption, LavaGeyser, LavaBlob, LavaMeteor, LavaField,
LavaSwordGround), misc (DebrisEntity, FragmentEntity, TridentShardEntity
plus ITrackedSwarmEntity, GrandBladeShard, GrandClashSpark,
HeavenSplitter, SwiftCrescent, SkyCracks, CurseChain, CurseLightning,
CurseTelegraph, AzureLightning, AzureTornado, AzureTempestChannel,
AzureSonicQuake, BanshoCyclone, BanshoGale, BraveSlash, BraveCrossSlash,
BraveShockwave, SorrowSlash, VoidSlash, SaberSlash, SaberVoidTear,
SaberDimensionalLineSlash, SaberDodgeSpark, DimensionalWarpRift,
DreadGaze, ErasureCannonBeam, SpiritCleave, SpiritImpale,
SpiritCannonBeam, PowerPoleExtension, PowerPoleImpact,
PowerPoleWhirlwind, SpikeFlailSpin, SpikeFlailSwing, TimeShatter).
Registrations live in entity/DbaEntities.java as self-contained field
blocks: removed by script. Kept entities: OtherworldGuide, FlyingNimbus,
Shenron, Porunga, Saibaman, FriezaSoldier, NamekianWarrior,
PatrolRobot, KiBlast, KiBeam, KiDisk, KiLaser, KiSpiralBeam,
KiExplosion, KiBarrier (all Ki technique or feature entities).

## 3. Weapon client renderers (91 files, delete)

Every renderer in client/render/ root except the keepers (AuraRenderer,
FlyingNimbusRenderer, OtherworldGuideRenderer, DynamicSkinManager,
AlphaModulatingVertexConsumer) renders a weapon ability entity.
Registrations sit in the client initializer: removed in its rewrite.
Also deleted: client/fx/ChargeFx, ClashFx, SwingTrailRenderer,
WeaponTrailProfiles (referenced only by the client initializer).
Kept in client/render: geo/ (weapon and dragon item models), item/
(procedural item models), layer/, model/, animation/, weapon/ (item
model registry), ki/ Ki technique renderers.

## 4. Shared render and particle infrastructure (KEEP, exceptions)

- client/render/ki/ComplexGeometry, ComplexTextures, DbaShaders,
  KiRenderHelper: used by AuraRenderer (form auras), all Ki technique
  renderers, KiBarrierRenderer, and client/wish/WishRitualVisuals.
  Kept whole, except ComplexGeometry loses its two weapon-only
  builders (rockWall, spikeEruption, added solely for the attack pass)
  and util/SpikePlacement is deleted with them.
- client/fx/MaterialEmitters and particle/DbaMaterialOptions with the
  eight material particle types: used by AtmosphereClient (weather),
  StaggerFx, every status effect's ambient particles, AuraRenderer,
  Ki renderers, wish visuals. Kept.
- particle/DbaParticles simple types (including the five complex-redo
  sprite types) and BLOCK_CHUNKS: used by effects, atmosphere,
  StaggerFx, wish visuals, and SaibamanEntity. Kept.
- THEME_DUST, particle/DbaDustOptions, client DbaDustParticle:
  CORRECTION recorded during the strip. The first sweep after deletion
  proved these are shared after all: entity/KiDiskEntity (Ki technique)
  and ki/KiBarrier emit DbaDustOptions particles, whose type is
  THEME_DUST. All three files and particles/theme_dust.json were
  restored, and the type stays registered. util/LayeredParticles was
  referenced only by weapon items (plus that javadoc mention) and
  stays deleted.

## 5. Terrain systems

- util/PermanentDestruction: KEEP (exception). KiTechniqueHandler and
  KiBlastEntity (surviving Ki technique system) call its crater,
  findSurface, and tier constants for Ki technique explosions. All
  weapon call sites die with the weapon code.
- util/DebrisSpawner, fx/SlamDebris, fx/DbaImpactFx: weapon only. Delete.
- block/MagmaCrustBlock, block/MiniVolcanoBlock,
  block/entity/VolcanoBlockEntity: DbaBlocks comment states both blocks
  exist only while LavaField holds their cells. Delete classes, the
  DbaBlocks fields and registrations, and the DbaBlockEntities entry.

## 6. Weapon hooks in shared systems (edit, keep the systems)

- DragonBlockArcaneDBA (main init): AttackBlockCallback (weapon mining
  block), UseItem and UseBlock sickle callbacks, Devil Trident swarm
  tick, Saber and Evil Spear tickServer calls, 14 weapon
  onPlayerDisconnect lines: removed. AttackEntityCallback keeps only
  its generic melee stamina drain, ungated from weapon identity;
  Devil Trident target marking and Azure lock-on removed.
- network/DbaNetwork: C2SWeaponLeftClickPayload registration and its
  whole dispatch receiver removed; payload class deleted.
- mixin/PlayerEntityMixin: the per-effect Ki and stamina upkeep drain
  block (seven weapon effect branches) and the sickle_of_sorrow
  Ki-depletion branch removed. Stats, races, forms, techniques stay.
- mixin/DelayedDamageMixin: the WeaponDrainHelper drain chain removed;
  the generic player-damage buffer, cinematic lock check, and deferred
  pop (shared with Ki Barrier) stay.
- mixin/LivingEntityMixin: Whis auto-dodge, Azure rush fall branch,
  Saber perfect dodge, Saber swing cancellers, Sickle hand override,
  and the whole Time Reversal section (util/TimeTracker deleted; its
  only consumers were TemporalRiftEntity and WhisStaffItem) removed.
  Effect inflictor tracking, tail, breathing, XP, Ki Barrier absorb,
  and mitigation stay.
- mixin/WindChargeMixin, mixin/client/SaberClientMixin (client set),
  client/mixin/MeltingOverlayMixin: weapon-only mixins. Files deleted
  and entries removed from both mixins JSON files.
- util/WeaponDrainHelper, util/SwarmHelper, util/VortexPhysics,
  util/BanshoWindChargeMarker, util/MovementLimiterHelper (only weapon
  items ever called it): deleted.
- skill/SkillDefinitions: the SICKLE technique definition and its
  tree.add removed (a weapon entry in the shared technique menu; its
  behaviors are removed from the mixins and init above).
- effect/EvilDreadEffect: its entire server behavior is maintaining
  DreadGazeEntity for the Evil Spear mark. Class deleted and its four
  DbaEffects registration pieces removed.
- The other 28 status effects STAY registered: three have live
  non-weapon appliers (ANCIENT_WEIGHT from GravityTrainingBlockEntity,
  EARTH_SHATTER from PlanetDimension, CELESTIAL_GRACE from
  PorungaEntity), and the shared combat mixins (DelayedDamageMixin,
  MovementLimiterHelper consumers aside, PlayerEntityMixin,
  EntityMixin, EntityTeleportMixin, LivingEntityMixin) treat the set
  as generic combat state. All weapon application sites are removed,
  so no weapon can apply any of them after the strip.

## 7. Verification plan

Dual javac sweeps against the clean 9834fb3 baseline (132 kinds on
26.2, 138 on 26.3, measured fresh before cutting): the stripped tree
must show no new error kinds on either classpath. Grep proof for the
headline deleted systems, JSON parse over surviving resources,
git diff --check, and git status confirming everything uncommitted
at HEAD 9834fb3.
