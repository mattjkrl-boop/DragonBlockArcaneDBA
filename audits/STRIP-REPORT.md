# Weapon Attack Strip Report (Nov 4, 2026)

Carter's fresh start, executed in the working tree only. HEAD is still
9834fb3, origin/main is still 9834fb3, and nothing was committed or
pushed: the full old implementation remains recoverable from git
history, and this strip exists only as uncommitted changes (202 deleted
files, 41 modified files, plus audits/strip-inventory.md and this
report untracked). The attack designs survive outside the code at
brain mods/DBA-ATTACK-DESIGNS.md; no implementation of them remains.

## What a weapon is now

Each of the 22 weapons (katana, z_sword, saber, brave_sword,
grand_sword, azure_dragon_sword, dabura_sword, cursed_blade,
hollows_edge, dimensional_sword, spirit_sword, evil_spear,
devil_trident, sickle_of_sorrow, ox_kings_ax, power_pole, whis_staff,
bansho_fan, blaster_gun, lava_sword, spike_flail, war_mace) is a plain
item: a single-properties-constructor class extending Item and
implementing item/geo/DbaGeoWeapon (all of whose GeoItem methods are
interface defaults), about 17 lines each, down from 300 to 480 lines
of ability logic. Verified by grep: all 22 constructions are still in
DbaItems, so every weapon stays registered with its item model,
texture, geo model, and creative menu presence. No use() abilities, no
charge or stance logic, no custom melee effects, no ability damage,
no Ki costs, no cooldowns, no ability tooltips. BlasterAmmoItem, which
existed only as Blaster Gun ammunition, was deleted with its
registration, creative tab entry, and register call; its item model
and texture files were left in resources (harmless, and reusable if a
rebuild wants ammunition back).

## Deleted: 202 files

- 90 files in entity/: 89 weapon ability entity classes (every
  projectile, field, domain, rift, wave, slam, shard, trail, charge,
  and aura entity serving the 22 weapons, including DebrisEntity,
  FragmentEntity, TridentShardEntity, and the whole lava family with
  LavaField and LavaSwordGround) plus the ITrackedSwarmEntity
  interface. Their 87 field registrations were removed from
  DbaEntities by script; the register() method's kept-entity attribute
  registrations were untouched.
- 88 weapon entity renderers in client/render/ root, plus their
  registrations in the rewritten client initializer.
- 4 client fx files: ChargeFx, ClashFx, SwingTrailRenderer,
  WeaponTrailProfiles (referenced only by the old client initializer).
- 9 util files: DebrisSpawner, SpikePlacement, SwarmHelper,
  WeaponDrainHelper, BanshoWindChargeMarker, VortexPhysics,
  TimeTracker, LayeredParticles, MovementLimiterHelper.
- 3 weapon-only mixins: WindChargeMixin, SaberClientMixin,
  MeltingOverlayMixin, with their entries removed from both mixins
  JSON files (both files revalidated as JSON).
- 2 terrain blocks (MagmaCrustBlock, MiniVolcanoBlock) plus
  VolcanoBlockEntity, with the DbaBlocks fields and registrations and
  the DbaBlockEntities type removed. The DbaBlocks comment confirmed
  both blocks existed only as LavaField constructs.
- 2 main fx files: DbaImpactFx, SlamDebris.
- 1 network payload: C2SWeaponLeftClickPayload, with its registration
  and its entire dispatch receiver removed from DbaNetwork, and the
  whole client side weapon input state machine (13 per-weapon charge
  trackers sending that payload) removed from the client initializer.
- 1 effect: EvilDreadEffect, whose entire server behavior was
  maintaining DreadGazeEntity for the Evil Spear mark, with its four
  DbaEffects registration pieces removed.
- ComplexGeometry (kept file, edited): the rockWall and spikeEruption
  builders, added solely for the attack pass, were cut with their
  exclusive helpers; the class javadoc was updated to match.

## Edited: 41 files

The 22 weapon item shells, plus: DbaItems (ammo), DbaEntities,
DbaBlocks, DbaBlockEntities, DbaEffects (evil dread), DbaNetwork,
DbaParticles and DbaParticleProviders (theme dust out, then back in,
see judgments), SkillDefinitions (the Sickle of Sorrow technique
definition and its tree entry removed from the shared technique menu),
DragonBlockArcaneDBA (main init: weapon AttackBlockCallback, both
sickle UseItem/UseBlock callbacks, trident swarm and Saber and Evil
Spear server ticks, and 14 weapon disconnect lines removed; the
AttackEntityCallback now runs only its generic melee stamina drain,
ungated from weapon identity, with Devil Trident target marking and
Azure lock-on removed), DragonBlockArcaneDBAClient (rewritten: only
non-weapon registrations remain), PlayerEntityMixin (the seven-branch
per-effect upkeep drain block and the sickle Ki-depletion branch
removed), DelayedDamageMixin (the WeaponDrainHelper drain chain
removed; buffer, lock check, and deferred pop kept), LivingEntityMixin
(Whis auto-dodge, Azure rush fall branch, Saber perfect dodge, Saber
swing cancellers, Sickle hand override, and the whole Time Reversal
section removed), ComplexGeometry, both mixins JSONs, and the two
restored theme dust files (comment wording only).

## Shared infrastructure kept (exceptions, with reasons)

- util/PermanentDestruction: the Ki technique system
  (KiTechniqueHandler, KiBlastEntity) calls its crater, findSurface,
  and tier constants for Ki technique explosions. Post-strip grep
  confirms its only referencers are those two files and itself. All
  weapon call sites died with the weapon code.
- client/render/ki/ComplexGeometry, ComplexTextures, DbaShaders,
  KiRenderHelper: form auras (AuraRenderer), every Ki technique
  renderer, KiBarrierRenderer, and the wish ritual visuals
  (WishRitualVisuals) are built on them. Only the two weapon-only
  builders were cut from ComplexGeometry.
- client/fx/MaterialEmitters, particle/DbaMaterialOptions and the
  eight material particle types, util/FxEnvelope, util/SeedHash:
  used by AtmosphereClient weather, StaggerFx, status effect ambient
  particles, auras, Ki renderers, and wish visuals.
- All DbaParticles simple types and BLOCK_CHUNKS: used by effects,
  atmosphere, StaggerFx, wish visuals, and SaibamanEntity.
- THEME_DUST with DbaDustOptions and DbaDustParticle: restored after
  the first post-strip sweep proved KiDiskEntity and KiBarrier emit
  them. Recorded here and in the inventory as a classification
  correction, not a quiet reversal.
- The other 28 status effects stay registered in DbaEffects. Three
  have live non-weapon appliers (ANCIENT_WEIGHT from gravity training,
  EARTH_SHATTER from PlanetDimension, CELESTIAL_GRACE from Porunga),
  and the shared combat mixins treat the set as generic combat state.
  Every weapon application site is gone, so no weapon can apply any
  effect now. DelayedDamageMixin and WeaponDrainHelper's removal is
  scoped accordingly: the drain helper is deleted and both mixins'
  drain branches are removed, while the generic damage buffer stays.
- DbaPostFx stays untouched: its form grades, danger vignette,
  shockwave, and atmosphere gloom are non-weapon; its Temporal Stasis
  and Storm of Darkness triggers simply have no appliers left.
- Ki entities and their renderers, Ki Barrier, races, forms, menus,
  HUD, mobs, armor, Dragon Radar, Recovery Capsule, the wish ritual,
  and the movement system were not touched, beyond the hook removals
  listed above.

## Judgment calls

- BlasterAmmoItem deleted rather than kept as a plain item: it is not
  one of the 22 weapons and existed only to feed the Blaster Gun.
- The Sickle of Sorrow technique definition was removed from
  SkillDefinitions because every behavior behind it (hand override,
  rift routing, technique melee, Ki upkeep) was weapon plumbing;
  PlayerStats and PlayerStatsAccessor keep their generic technique
  maps and the now-inert sickle accessors, since the stats framework
  is shared.
- Removing PlayerEntityMixin's upkeep drain block means Celestial
  Grace (Porunga) and Ancient Weight (gravity training) no longer
  drain Ki per tick while active; they now simply expire by duration.
  Those drains were keyed to weapon names and priced weapon effects;
  keeping them would have kept weapon Ki drains running.
- MovementLimiterHelper was deleted: its only callers in the whole
  tree were weapon items. Its kept-effect branches (Ancient Weight,
  Earth Shatter) had no other caller, so nothing else loses behavior.
- Item model and texture resources for all 22 weapons stay, as do
  weapon geo models and animations (item appearance). Lang entries
  for removed things (blaster ammo, evil dread) are inert text and
  were left alone.

## Verification

- Baselines measured fresh on the clean tree before cutting: 132
  unique javac error kinds on the 26.2 classpath, 138 on 26.3.
- Stripped tree: 120 kinds on 26.2, 126 on 26.3. comm -13 of baseline
  against stripped prints NOTHING on both classpaths: every remaining
  kind already existed at 9834fb3, and 12 baseline kinds per classpath
  disappeared with the weapon closures. No ability code was stubbed
  back in to achieve this; the one restoration (theme dust) is shared
  infrastructure with live non-weapon users, documented above.
- Reference analysis run against the final deletion set: 0 surviving
  files reference any deleted class (the analyzer's only hit during
  the pass was the theme dust javadoc, since cleaned).
- Grep proof, zero surviving files for: FragmentEntity, DebrisEntity
  (via analyzer), spikeEruption, rockWall, DebrisSpawner,
  WeaponDrainHelper, TimeTracker, C2SWeaponLeftClickPayload,
  LayeredParticles, SwarmHelper, VortexPhysics, MovementLimiterHelper,
  MagmaCrustBlock, MiniVolcanoBlock, BlasterAmmoItem, EvilDreadEffect,
  DbaImpactFx, SlamDebris, SpikePlacement, ChargeFx, ClashFx,
  SwingTrailRenderer, WeaponTrailProfiles, WindChargeMixin,
  MeltingOverlayMixin, BanshoWindChargeMarker, DreadGazeEntity,
  LavaField.
- All 567 surviving resource JSONs parse. git diff --check is clean.
- git status: HEAD 9834fb3, origin/main 9834fb3, 202 deleted and 41
  modified files uncommitted, audits/strip-inventory.md and this
  report untracked. No commit, no push, no checkout, no reset.

## Honest line

No Gradle build exists in this VM and none was run or is claimed.
Verification is the dual javac audit sweep against the 9834fb3
baseline, as with every prior pass. The stripped tree's in-game state
is unverified until Carter builds on his PC.
