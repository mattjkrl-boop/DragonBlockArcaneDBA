# Bespoke Pass Scope 2 Audit (Phase 1, coordinating pass)

Scope expansion audit at HEAD dc2cd1f, measured fresh from the code by
grep (clean slate: no number below is copied from an earlier audit).
Carter directed that the areas the first bespoke pass left out of
scope now be converted too: Ki techniques, the dragon wish ritual,
mobs, menus and screens, dimension ambience, and the orphaned
TimeShatter pair, plus a tree-wide sweep for anything vanilla hiding
in an area nobody named. The binding rules are
audits/BESPOKE-STANDARD.md, inherited unchanged: geometry only via
BespokeGeometry with synced seeds, particles only via DbaParticles,
sounds only via DbaSounds, ability damage only via DbaDamage, split
source sets clean, dual-version javac bar (zero new error kinds vs
the 132 on 26.2 and 141 on 26.3 baseline), agents edit only their own
files, shared changes through the coordinating pass.

This document is audit and plan only. No code was changed.

Concurrency note: a Lava Sword redesign agent is editing Lava Sword
files in the same tree while this audit runs. Nothing below touches
Lava scope (already converted in the first pass), and all counts were
taken at dc2cd1f. If that work lands mid-audit, tree totals in
section 7 may shift by its files only.

New standing permission (Carter, Nov 3, 2026), folded into the plan:
when converting an ability, Muse may create NEW BLOCKS and NEW
ENTITIES to make the ability work, for any weapon or system in the
mod. Section 9 lists where that permission honestly applies in this
scope, with registration costs, and where it does not help.

## Tree-wide totals outside the converted weapon scope (measured)

- Vanilla SoundEvents references: 98 lines in 25 files.
- Vanilla damageSources() lines: 18 tree-wide. Two are the first
  pass's documented ValorFieldEntity exception (weapon scope,
  stands). Sixteen sit outside weapon scope, inventoried below.
- Vanilla ParticleTypes call lines: 15 real call lines in 10 files,
  plus 17 false-positive lines in particle/DbaParticles.java (the
  FabricParticleTypes factory API and javadoc, not vanilla usage).
- DustParticleOptions: 5 real constructions in 3 files, plus 3
  javadoc-only mentions (DbaParticles, DbaDustOptions).
- KiRenderHelper primitive call sites: 24 (Ki renderers 16, wish
  visuals 8). The 5 primitive method definitions themselves live in
  client/render/ki/KiRenderHelper.java.
- Hand-rolled local geometry helpers outside weapon scope: 13
  vertex-emitting or primitive-composing helpers (Ki renderers 5,
  WishRitualVisuals 4, TimeShatterRenderer 4).

## Area 1: Ki techniques

Files (18): ki/KiTechniqueHandler.java (344 lines), ki/KiTechnique.java
(117), ki/KiTechniqueType.java (41), ki/KiBarrier.java (252);
entity/KiBeamEntity, KiBlastEntity, KiDiskEntity, KiExplosionEntity,
KiLaserEntity, KiSpiralBeamEntity, KiBarrierEntity;
client/render/ki/KiBeamRenderer, KiBlastRenderer, KiDiskRenderer,
KiExplosionRenderer, KiLaserRenderer, KiSpiralBeamRenderer,
KiBarrierRenderer. The seven technique types (KiTechniqueType):
BLAST, SPIRAL_BEAM, DISK, BEAM, LASER, EXPLOSION, BARRIER, mapping
one to one onto the seven entities and renderers.

Before-counts:
- Particles: 5 DustParticleOptions constructions (KiBarrier.java 2,
  at lines 108 and 167, fully qualified; KiDiskEntity 2 plus its
  import). Zero vanilla ParticleTypes lines. The only particle
  emissions in Ki common code are those five dust bursts: the beam,
  explosion, laser, spiral, and barrier entities emit no particles
  server side; their look is renderer geometry plus handler sounds.
- Sounds: 23 vanilla SoundEvents lines. KiTechniqueHandler 14:
  BLAZE_SHOOT x2, GENERIC_EXPLODE x3, FIREWORK_ROCKET_BLAST,
  BEACON_ACTIVATE, WARDEN_SONIC_BOOM x2, TRIDENT_THROW,
  ELYTRA_FLYING, BEACON_DEACTIVATE, GUARDIAN_ATTACK,
  RESPAWN_ANCHOR_DEPLETE. KiBarrier 3: BEACON_ACTIVATE,
  BEACON_DEACTIVATE x2. KiDiskEntity 4 (fully qualified):
  PLAYER_ATTACK_SWEEP, ANVIL_LAND, GENERIC_EXPLODE, ITEM_BREAK.
  KiBlastEntity 2: GENERIC_EXPLODE, LIGHTNING_BOLT_IMPACT.
- Damage: 6 vanilla damageSources() lines, all indirectMagic or
  magic: KiTechniqueHandler 4 (three indirectMagic(player, player)
  ability hits at lines 184, 256, 303, and one magic() self-damage
  at line 309), KiDiskEntity 1 (indirectMagic(this, owner), line
  87), KiBlastEntity 1 (indirectMagic(this, owner), line 102).
- Primitives: 16 KiRenderHelper calls. KiBeamRenderer 2 (both
  drawCylinder, sheath and core), KiLaserRenderer 2 (drawCylinder
  x2), KiDiskRenderer 2 (drawDisc x2, hole and face),
  KiExplosionRenderer 3 (drawSphere x2, drawDisc x1),
  KiSpiralBeamRenderer 3 (drawCylinder x1, drawHelix x2),
  KiBarrierRenderer 4 (drawSphere x2, drawDisc x2). KiBlastRenderer
  calls no primitives but hand-rolls its geometry.
- Local helpers: 5 vertex-emitting helpers: KiBlastRenderer 4
  (drawLaserBoltBody, drawHelicalVortex, drawRibbonSegment,
  drawResonanceRing) and KiBarrierRenderer 1 (drawInvertedSphere).
  Direct addVertex lines in Ki renderers: 16, all in
  KiBlastRenderer's helpers. Trig lines across the 7 renderers: 28.
- Seeds: NO Ki entity carries a synced SEED field. Synced accessors
  today are COLOR, DAMAGE, SCALE, OWNER_ID, LENGTH, USED_PERCENT
  only. Every converted renderer needs a synced seed per the
  BoltEntity pattern the weapon agents used.
- Textures: none vanilla. Ki geometry draws through the mod's ki
  canvas via KiRenderHelper render types, same as weapons.

What conversion means here: exactly the weapon treatment. The 16
primitive calls and 5 helpers are deleted; beams and the laser
rebuild as layered tornRibbon crosses (the Spirit Cannon and Erasure
beam conversions are the in-repo precedent), the disk as bladeArc
plus shardFan, the explosion as displacedDome shells plus plume, the
spiral as tornRibbon strands with a brokenVortexBand collar, the
barrier as displacedDome pairs (its drawInvertedSphere is the
regular lat-long ball the toolkit exists to replace). Dust bursts
become THEME_DUST with DbaDustOptions at identical colors and
scales. Sounds swap by use case onto the existing bank; the bank
already registers KAMEHAMEHA_CHARGE and KAMEHAMEHA_FIRE with ZERO
call sites anywhere in the tree (verified by grep), so the beam
technique's charge and fire sounds wire to events that already
exist. GUARDIAN_ATTACK (laser) maps to LIGHTNING_ZAP;
FIREWORK_ROCKET_BLAST (explosion technique) maps to BOOM_BIG;
TRIDENT_THROW in the handler is a disk throw and maps to
DbaSounds.TRIDENT_THROW or a plain throw whoosh, agent's judgment
call to report. Damage: all 6 sites to DbaDamage.ki (decision D1).

Risks and ordering: KiTechniqueHandler is one shared file driving
all seven techniques, so the whole area goes to ONE agent; splitting
the handler across agents would collide. No foundation gaps block
this area: it can start as soon as Phase 2 opens.

## Area 2: Dragon wish ritual

Files: wish/DragonWishRitual.java (278), entity/ShenronEntity.java
(126), entity/PorungaEntity.java (136), entity/DbaGeoDragon.java
(44, the GeckoLib base both dragons share),
client/wish/WishRitualVisuals.java (313),
client/render/geo/DbaDragonRenderers.java (wiring: GeoEntityRenderer
creation plus the wish-linger hook), client/hooks/WishClient.java,
client/gui/WishScreen.java (164), network/WishRitualStartPayload,
network/WishMenuOpenPayload, network/C2SMakeWishPayload,
block/EarthDragonBallBlock, block/NamekianDragonBallBlock.

Before-counts:
- Sounds: 12 vanilla SoundEvents lines. DragonWishRitual 3:
  BEACON_ACTIVATE (line 112), PORTAL_TRAVEL (191),
  ENDER_DRAGON_GROWL (237). ShenronEntity 4: BEACON_ACTIVATE,
  UI_TOAST_CHALLENGE_COMPLETE, ENDER_DRAGON_FLAP, PORTAL_TRAVEL
  (lines 94 to 100). PorungaEntity 5: BEACON_ACTIVATE,
  UI_TOAST_CHALLENGE_COMPLETE, ENDER_DRAGON_FLAP, PORTAL_TRAVEL,
  ENDER_DRAGON_GROWL (lines 95 to 111).
- Geometry: WishRitualVisuals holds all 8 remaining non-Ki primitive
  calls: drawSphere x4, drawCylinder x2, drawHelix x2, composed by 4
  local helpers: drawBall (the seven dragon balls, spheres with
  star pips), drawColumn (the summoning light column, cylinders
  plus twin helixes), drawStandingRing (sphere studs on a ring),
  drawTorus (a hand-rolled equal-segment torus, no primitive call
  but regular geometry by construction). Trig lines in wish client
  files: 14.
- Particles: 0. Damage: 0.
- Textures and models: already bespoke. Both dragons are GeckoLib
  entities with the mod's own geo models, animations, and textures
  (textures/entity/shenron.png, porunga.png); dragon ball blocks and
  items use the mod's own textures. WishScreen, WishClient, the
  three payloads, and both dragon ball blocks carry zero vanilla
  references in every category (verified): they are plumbing.

What conversion means here: WishRitualVisuals rebuilt on
BespokeGeometry: balls to displacedDome pairs with shardFan star
pips (the Blaster charge orb conversion is the precedent), the
column to a plume core with brokenVortexBand shells and branch
ribbons standing in for the helix pair, the standing ring to a
brokenVortexBand with glowFan studs, the torus to a
brokenVortexBand pair. Sounds: BEACON_ACTIVATE maps to CHARGE_RISE,
PORTAL_TRAVEL to TELEPORT_WHOOSH or RIFT_OPEN (both exist). Three
events have NO bank analogue and need new synthesized entries from
the coordinating pass (Phase 1 work, tools/gen_bespoke_sounds.py):
a dragon growl, a dragon wing flap, and a wish-granted fanfare for
UI_TOAST_CHALLENGE_COMPLETE (decision D7).

Risks and ordering: blocked on the three new bank events. The
visuals conversion itself is self-contained (WishRitualVisuals is
client only, driven by ritual state). DbaDragonRenderers is shared
with the mob registrations (MobClient calls its create()); it needs
no changes and is NOT in this agent's file list.

## Area 3: Mobs

Files: entity/DbaHostileEntity.java (92, abstract GeckoLib Monster
base), entity/FriezaSoldierEntity.java (75),
entity/NamekianWarriorEntity.java (58),
entity/PatrolRobotEntity.java (264), entity/SaibamanEntity.java
(94), entity/OtherworldGuideEntity.java (45),
entity/DbaHostileSpawns.java, client/render/OtherworldGuideRenderer,
plus read-only wiring: client/hooks/MobClient.java and
client/hooks/GearClient.java (renderer registration),
client/render/geo/DbaDragonRenderers.java (shared create()).

Before-counts:
- Sounds: 17 vanilla SoundEvents lines, and most of them are mob
  VOICES delivered through getAmbientSound, getHurtSound, and
  getDeathSound overrides. FriezaSoldier 5: BLAZE_SHOOT (its ranged
  ki shot, line 52), PLAYER_ATTACK_WEAK (melee swing), and
  PILLAGER_AMBIENT, PILLAGER_HURT, PILLAGER_HURT as ambient, hurt,
  and death. NamekianWarrior 4: PLAYER_ATTACK_WEAK and
  VINDICATOR_AMBIENT, VINDICATOR_HURT, VINDICATOR_HURT.
  PatrolRobot 3: IRON_GOLEM_REPAIR (line 147, a repair action),
  IRON_GOLEM_HURT, IRON_GOLEM_DEATH. Saibaman 5:
  GENERIC_EXPLODE (self-destruct, line 66), SLIME_ATTACK,
  SLIME_SQUISH_SMALL (ambient), SLIME_HURT, SLIME_HURT.
  DbaHostileEntity, OtherworldGuideEntity, and DbaHostileSpawns:
  zero in every category.
- Particles: 2 lines. FriezaSoldier line 53: ParticleTypes.CRIT on
  its ranged hit. Saibaman line 67: ParticleTypes.ITEM_SLIME
  (fully qualified) on self-destruct.
- Damage: 2 lines, both damageSources().mobAttack(this):
  FriezaSoldier line 51 (the ranged shot lands as a 5.0 mobAttack
  hit at range, with the CRIT particles above) and Saibaman line 61
  (self-destruct area damage). Mob MELEE contact damage does not
  appear here at all: it runs through vanilla Mob attack plumbing
  with no damageSources() call in mod code, the same status as the
  player's plain melee hit under rule 4.
- Geometry: 0 primitives, 0 local helpers, 0 addVertex. The four
  hostile mobs render through GeckoLib DefaultedEntityGeoModel with
  the mod's own models and textures (saibaman, frieza_soldier,
  namekian_warrior, patrol_robot), registered in MobClient and
  GearClient. Already bespoke.
- Vanilla textures: exactly 1, and it is the only vanilla texture
  reference under client/render in the whole tree:
  OtherworldGuideRenderer line 12 sets its texture to
  minecraft:textures/entity/villager/villager.png. The guide
  renderer is a vanilla-style Villager renderer for a guide NPC.
  Fixing it needs a mod-authored guide texture (art work for the
  coordinating pass or Carter); the renderer swap itself is one
  line once the texture exists (decision D12).

What conversion means here: attack and action sounds swap by use
case (BLAZE_SHOOT to FIRE_SHOT, GENERIC_EXPLODE to BOOM_BIG, melee
swings to SWING_LIGHT or HIT_FLESH, repair to a mechanical event).
The voices are the real work: full conversion needs NEW bank
entries per mob family, ambient plus hurt plus death for soldier,
warrior, robot, and saibaman (up to 12 synthesized events), because
the bank has no mob voice events at all. That is coordinating-pass
Phase 1 synthesis work and the highest-risk audio in this scope:
synthesized creature voices can easily read worse than the vanilla
pillager, vindicator, golem, and slime voices they replace
(decision D2, Carter's call). Particles swap by the standard map:
CRIT to CRIT_SPARK; ITEM_SLIME has no analogue (nearest is
DUST_PUFF tinted, or a new slime-chunk type on BLOCK_CHUNKS with a
slime block state; agent reports the choice). Damage: the two
mobAttack lines are ABILITY damage (a ranged ki shot and a
self-destruct), not plain hits; recommendation is DbaDamage.ki for
both, with mob contact melee left on the vanilla pipeline exactly
like player melee (decision D3).

Risks and ordering: blocked on the voice bank decision (D2). The
Frieza ranged attack is an instant hit at range with particles at
the target; see section 9 for the projectile-entity option
(decision D4).

## Area 4: Menus and screens

Files: client/gui (17 files: DbaMenuScreen, DbaSettingsScreen,
DbaHudOverlay, DbaUi, EmoteCatalog, EmoteSelectionScreen, EmotesTab,
FormsTab, GravityTrainingScreen, KiCustomizerTab, MenuTab,
RaceSelectionScreen, ReviveScreen, SpacePodScreen, StatsTab,
TechniquesTab, WishScreen), inventory/DbaMenus.java,
inventory/GravityTrainingMenu.java, and the screen mixins
client/mixin/AbstractContainerScreenMixin, OptionsScreenMixin,
PauseScreenMixin, TitleScreenMixin.

Before-counts: this area is already bespoke in everything but one
sound. Particles 0, damage 0, primitives 0, DustParticleOptions 0,
across all 21 files. Sounds: exactly ONE vanilla line in the entire
GUI tree: client/gui/DbaUi.java line 225, inside the shared
playClick() helper, plays SoundEvents.UI_BUTTON_CLICK through
SimpleSoundInstance.forUI. DbaUi is the shared UI kit every DBA
screen builds on, so every screen's clicks funnel through this one
method. The 6 draw-prefixed helpers in gui (drawDamageDirection,
drawSlot, drawPreview, drawRaceGrid, drawColors, drawInspector) are
GuiGraphicsExtractor 2D screen drawing: HUD arcs, slot frames, race
previews, panels. They are not effect geometry and rule 1 does not
apply to them; there is nothing to convert. Menus server side
(DbaMenus, GravityTrainingMenu) are MenuType and container plumbing
with zero references in every category: game infrastructure, not
presentation assets. The screen mixins restyle vanilla screens and
carry no vanilla assets.

What conversion means here, concretely: ONE new bank event
(ui_click, synthesized with the rest of the bank) and a one-line
swap inside DbaUi.playClick(). That single change bespokes every
DBA screen's click at once. network/DbaNetwork also plays
UI_BUTTON_CLICK x2 server side for form toggles (section 7); the
same event covers it. Nothing else in this area is a conversion
target. Adding hover or select sounds would be NEW design (no such
sounds exist today), not conversion; it is listed as optional
decision D8 and should not be smuggled into a conversion agent's
brief. Because the whole area is one line plus the bank event, the
recommendation is that the coordinating pass does it centrally with
the Phase 1 bank work; no dedicated agent is proposed.

## Area 5: Dimension ambience

The mod's dimensions: Namek, Otherworld, Vegeta, Yardrat (dimension
and dimension_type JSONs under data/dragonblockarcanedba). Files:
dimension/DimensionTravel.java (289), dimension/PlanetDimension.java
(50), dimension/OtherworldStationGenerator.java (361, zero
references in every category: worldgen plumbing),
client/particle/OtherworldCloudParticleManager.java (195),
block/YellowCloudBlock.java, the four dimension_type JSONs, and the
already-bespoke cloud rendering stack (mixin/CloudTextureMixin,
mixin/CloudTextureMixin263, mixin/CloudColorMixin,
client/compat/DbaCloudLayers263) from the 26.3 port.

Before-counts, the vanilla dependencies in order of depth:
1. Dimension type effects: ALL FOUR dimension_type JSONs declare
   "effects": "minecraft:overworld". The dimensions borrow the
   overworld's sky color, fog color and density, and cloud behavior
   wholesale; only ambient_light and fixed_time are their own. This
   is the deepest vanilla dependency in the area: the moment-to-
   moment sky and fog of four dimensions are vanilla presentation.
   True conversion means registering custom DimensionSpecialEffects
   for each dimension on the client and pointing the JSONs at mod
   effect ids. That is real client work plus a data change, and
   the registration API must be verified against both the 26.2 and
   26.3 jars before promising it (the multi-layer cloud port is the
   precedent for this kind of dual-version check). Decision D6,
   Carter's call.
2. Cloud particles: OtherworldCloudParticleManager spawns vanilla
   ParticleTypes.CLOUD at 2 call sites (lines 168 and 189) to build
   its five-tier drifting Otherworld cloud formations. The bank of
   particle types has no cloud puff: DUST_PUFF is a small short
   ground puff with the wrong scale and life for a sky cloud.
   Honest options in section 9; recommendation is a new CLOUD_PUFF
   particle type (decision D5).
3. block/YellowCloudBlock.java line 116: ParticleTypes.CLOUD x1,
   ambient puffs on the nimbus cloud block. Same new type as item 2.
4. Sounds: DimensionTravel 4 lines, fully qualified:
   FIREWORK_ROCKET_LAUNCH plus GENERIC_EXPLODE (space pod launch,
   lines 87 and 89) and PORTAL_TRAVEL plus BEACON_ACTIVATE (portal
   travel, lines 166 and 168). The bank already registers
   SPACE_POD_LAUNCH with zero call sites: the launch pair wires to
   it (plus BOOM_BIG if a two-layer launch is wanted). Portal maps
   to TELEPORT_WHOOSH or RIFT_OPEN.
5. Damage: PlanetDimension line 44, one line:
   damageSources().drown() for 4.0. This is an environmental hazard
   tick, not ability presentation; re-typing would change only its
   death message. Recommendation: leave vanilla and list it, or
   convert if Carter wants absolute zero; flagged as part of
   decision D11's family (environmental damage lines).
6. Data side: biome JSONs under worldgen/biome carry NO
   ambient_sound entries (checked): there are no data-side ambient
   sound dependencies. The cloud LAYERS themselves are already the
   mod's own rendering (the mixin stack above); only the particle
   clouds in item 2 are vanilla-fed.

What conversion means here: items 2 to 4 are ordinary swaps once
the CLOUD_PUFF type exists. Item 1 is the only structural work in
the area and the only one that changes what the dimensions look
like; it should be its own decision, not bundled silently into a
swap agent's brief.

## Area 6: TimeShatter (Whis family, owned by no agent in either pass)

Files: entity/TimeShatterEntity.java (63 lines) and
client/render/TimeShatterRenderer.java. Spawned by
item/WhisStaffItem.java at three sites (lines 155, 199, 260) on
Temporal Shatter impacts. The Whis agent's brief excluded this pair
in the spectacle pass and again in the bespoke pass; it is the last
unconverted weapon-family effect in the tree.

Before-counts: particles 0, sounds 0, damage 0 (the entity deals
no damage and plays no sounds; it is a pure visual burst),
primitives 0, DustParticleOptions 0. Geometry: 4 local
vertex-emitting helpers (drawSplinteringDial, drawSpoke,
drawGlassPrismShard, drawOctahedralCore), 32 addVertex lines, 20
trig lines, and equal-segment construction throughout (the dial is
36 uniform segments). The entity's only synced field is SCALE;
there is no synced seed.

The three known defects, precisely from the code:
1. Eighteen tick life. TimeShatterEntity lines 20 and 21:
   maxLifetime and lifetime are both the plain int 18, decremented
   server side per tick with discard at zero. The renderer reads
   getMaxLifetime() for its progress curve, so the whole burst,
   including its fade-out (which only starts at progress 0.5), is
   compressed into 18 ticks, 0.9 seconds, where converted burst
   effects run 30 to 40. Fix: raise both fields together (36
   suggested; the exact value is a feel decision for the
   coordinating pass, since it lengthens a Whis Staff impact
   effect). No other file needs to change for this fix: the
   renderer takes the value from the entity.
2. No fade-in. TimeShatterRenderer submit(): the fade term is
   exit-only. exitT stays 0 until progress passes 0.5, so alpha is
   at full strength from the first frame while only the scale
   eases (travel curve). Every converted renderer in the first pass
   multiplies by an intro smoothstep (the FxEnvelope pattern, first
   4 to 6 ticks). Fix: multiply the fade by that intro term; a
   renderer-only change.
3. Mismatched random seeds between the dial and shard layers.
   submit() issues TWO submitCustomGeometry lambdas, and EACH
   constructs its own new Random(state.seed) (renderer lines 80
   and 108). The first lambda consumes 24 draws for its 8 spokes.
   The second lambda restarts the identical stream from the top,
   so the 16 shards' angles, speeds, sizes, and rotations reuse the
   spokes' opening random values in order: shard parameters are
   exactly correlated with spoke parameters instead of being
   independent draws, and editing the spoke loop silently
   reshuffles every shard. On top of that, state.seed is the entity
   UUID's most significant bits, a hash of synced state (legal
   under the standard) rather than a synced seed field. Fix: add a
   synced SEED field to the entity on the BoltEntity pattern, set
   in the spawn constructor (WhisStaffItem's three construction
   sites keep compiling untouched, because the public constructor
   signature does not change), and derive a separate builder seed
   per layer from it.

What conversion means here: the three fixes above plus the full
bespoke rebuild of the renderer on BespokeGeometry: dial to
brokenVortexBand, spokes and shard wakes to branch and tornRibbon,
shards to shardFan, the octahedral core to a displacedDome pair
(the first pass's deferred octahedron builder gap applies here
too; the dome-pair workaround is the shipped precedent). One agent
owns both files. WhisStaffItem is NOT in its file list.

## Area 7: Tree-wide sweep, everything else

Grouped by directory, real targets separated from false positives.
SoundEvents total reconciles: 23 (Ki) + 12 (wish) + 17 (mobs) + 17
(network) + 4 (dimension) + 3 (tail) + 3 (movement) + 17 (utility
items and nimbus) + 1 (main class) + 1 (gui) = 98.

Real targets not covered by areas 1 to 6:

- network/DbaNetwork.java: 17 SoundEvents lines plus 1
  ParticleTypes.CRIT (line 262). This is the player progression
  and forms system, the largest single sound file outside weapon
  scope: PLAYER_LEVELUP x3, EXPERIENCE_ORB_PICKUP, BEACON_ACTIVATE,
  BEACON_DEACTIVATE x2, FIRE_EXTINGUISH x2, GENERIC_EXPLODE,
  LIGHTNING_BOLT_THUNDER played in SoundSource.WEATHER,
  WITHER_SPAWN, RESPAWN_ANCHOR_DEPLETE, ENCHANTMENT_TABLE_USE,
  ARMOR_EQUIP_GENERIC, UI_BUTTON_CLICK x2. The bank already
  registers TRANSFORM_GENERIC with zero call sites: form
  activation is its obvious home. Most of the rest map onto
  existing bank events by use case (charge and release family,
  EXTINGUISH, CAST_MAGIC); a dedicated level-up chime is the one
  plausible new entry (decision D13's sibling: reuse CRYSTAL_CHIME
  or synthesize one). The CRIT line swaps to CRIT_SPARK.
- fx/SlamDebris.java: 2 lines, line 42 a vanilla
  BlockParticleOption(ParticleTypes.BLOCK, state) and line 48
  ParticleTypes.POOF. Shared weapon fx that the first pass's
  agents held read-only, so it kept vanilla particles while every
  caller converted. The standard map applies directly:
  BLOCK_CHUNKS and DUST_PUFF. Coordinating-pass fix.
- entity/DebrisEntity.java line 118: ParticleTypes.POOF (fully
  qualified) for landing dust. This is the first pass's OWN Phase 1
  entity, shipped with a vanilla particle in it. Swap to
  DUST_PUFF. Coordinating-pass fix.
- movement/MobPhysics.java and movement/MovementPhysics.java:
  POOF x1 each (landing dust), PLAYER_SMALL_FALL, SHIELD_BLOCK,
  PLAYER_BIG_FALL. Shared physics feedback under every weapon and
  mob. Particles swap to DUST_PUFF; the fall sounds have no exact
  bank analogue (THUNK and SLAM_THUD are the nearest; agent or
  coordinating pass reports the pick). Damage in MobPhysics:
  generic() x2 (lines 194 and 205), fall and stagger physics
  damage. That is the physics engine's own fall-damage analogue,
  game-system damage rather than ability presentation;
  recommendation: leave as generic and carry it on the
  still-vanilla list with that reason.
- tail/TailHelper.java: 4 ParticleTypes lines (CRIT,
  DAMAGE_INDICATOR, SMOKE, HAPPY_VILLAGER, lines 119 to 121 and
  163) plus 3 SoundEvents (ITEM_BREAK, PLAYER_ATTACK_CRIT,
  EXPERIENCE_ORB_PICKUP): tail sever and tail heal feedback. CRIT
  and SMOKE swap by the map (CRIT_SPARK, DUST_PUFF); ITEM_BREAK
  to ITEM_SHATTER, PLAYER_ATTACK_CRIT to CRIT_HIT.
  DAMAGE_INDICATOR and HAPPY_VILLAGER are state-feedback glyphs
  (damage pips and heal hearts), not world effects, and have no
  DbaParticles analogue: decision D10.
- Utility items and the nimbus (17 SoundEvents lines):
  item/SenzuBeanItem (PLAYER_BURP, PLAYER_LEVELUP,
  BEACON_POWER_SELECT), item/RecoveryCapsuleItem (PLAYER_BURP,
  BOTTLE_EMPTY), item/DragonRadarItem (NOTE_BLOCK_BASS,
  NOTE_BLOCK_BIT: the radar's distance beeps; a new radar_blip
  bank event is the honest bespoke answer, decision D13),
  item/PatrolRobotItem (IRON_GOLEM_REPAIR; pairs with the mob
  area's robot repair), item/FlyingNimbusItem (WOOL_PLACE x2,
  ELYTRA_FLYING x2), entity/FlyingNimbusEntity (WOOL_PLACE,
  WOOL_BREAK, ITEM_PICKUP), item/DbaArmorMaterials
  (ARMOR_EQUIP_IRON and ARMOR_EQUIP_LEATHER: these are the equip
  sound SUPPLIERS inside the armor material definitions, so the
  swap changes what every DBA armor piece sounds like on equip;
  one new equip event or reuse ARMOR-style bank entry needed).
  DragonBlockArcaneDBA.java line 224: one BEACON_ACTIVATE in the
  main mod class (a shared file; the coordinating pass should make
  this one swap centrally rather than handing the main class to an
  agent).
- mixin/WindChargeMixin.java line 50: damageSources().
  playerAttack(owner) for the Bansho Fan wind charge burst. This
  is weapon ability damage that lives in a mixin, outside the
  Bansho agent's ownership in the first pass. Swap to
  DbaDamage.tempest. One line, but the file is a mixin other work
  depends on (the BanshoShockwaveEntity constructor contract), so
  it goes to the coordinating pass or the shared-systems agent
  with care.
- mixin/LivingEntityMixin.java line 99: a DustParticleOptions
  white burst for the WHIS STAFF AUTO-DODGE (the code comment says
  so; the dodge sound beside it is already DbaSounds.WIND_GUST).
  A weapon-scope straggler the Whis agent could not own because it
  lives in a mixin. Swap to THEME_DUST with DbaDustOptions, white,
  scale 1.5, identical numbers. Coordinating-pass fix.
- block/entity/GravityTrainingBlockEntity.java line 110:
  generic() gravity training damage to the training player.
  Environmental and training damage; crush fits thematically if
  Carter wants it typed (decision D11).

False positives, not conversion targets:

- particle/DbaParticles.java, 17 ParticleTypes-matching lines:
  the FabricParticleTypes factory API used to register the mod's
  own types, plus javadoc. Not vanilla usage. The 3
  DustParticleOptions matches across DbaParticles and
  particle/DbaDustOptions.java are likewise javadoc only.
- entity/ValorFieldEntity.java, 2 damageSources() lines: the
  first pass's documented exception (suspended projectile damage
  belongs to the projectile's owner). Stands unchanged.
- mixin/DelayedDamageMixin.java lines 166 and 169: NOT a swap
  target, and worth stating plainly. This mixin accumulates ANY
  player-attributed damage landing on a LivingEntity and
  re-applies the total later under a REBUILT source
  (playerAttack(attacker), or generic() when the attacker is
  gone). By construction it discards the original damage type on
  every delayed hit, including the first pass's bespoke types.
  Making delayed damage preserve bespoke types means storing the
  original DamageSource in the mixin and re-applying with it: a
  behavior change to a shared combat system, not a line swap.
  Decision D9 for Carter and the coordinating pass; no Phase 2
  agent should touch it on a conversion brief.
- dimension/PlanetDimension.java drown(): environmental hazard,
  area 5 item 5.
- command/DbaCommand.java line 306: passes generic() as an
  argument into TailHelper.severTail from an admin command. The
  gameplay sever path supplies its own source. Not presentation;
  leave.
- Mob melee contact damage: no damageSources() call exists in mod
  code for it (vanilla Mob plumbing), same exemption logic as
  player plain melee. Not counted anywhere above.
- The 6 gui draw helpers: 2D screen drawing, area 4.

## Decisions for the coordinating pass or Carter

- D1. Ki technique damage family: all 6 Ki damage sites to
  DbaDamage.ki, no new family. Recommendation: yes.
- D2. Mob voices: synthesize ambient, hurt, and death events for
  the soldier, warrior, robot, and saibaman families (up to 12 new
  bank events), or scope mob voices out as game infrastructure.
  Recommendation: convert, it is the only path to zero in mob
  scope, but Carter should know synthesized voices are the
  highest-risk audio in the pass and may read worse than vanilla.
- D3. Mob ability damage: Frieza ranged shot and Saibaman
  self-destruct from mobAttack to DbaDamage.ki; contact melee
  stays on the vanilla pipeline. Recommendation: yes.
- D4. Frieza ranged attack shape: it is an instant hit today.
  Under the new-entities permission it could fire a real
  projectile (section 9). Carter's call; conversion does not
  require it.
- D5. Otherworld cloud particles: new CLOUD_PUFF particle type
  (registration, provider style, generated texture) vs a cloud
  entity vs DUST_PUFF reuse. Recommendation: the particle type.
- D6. Dimension effects: register custom DimensionSpecialEffects
  for Namek, Otherworld, Vegeta, and Yardrat and repoint the four
  dimension_type JSONs, vs keep borrowing "minecraft:overworld"
  sky and fog (cloud layers are already custom). Carter's call;
  this is the only item that changes how the dimensions look.
- D7. Wish bank entries: dragon growl, dragon flap, wish-granted
  fanfare. Needed for zero vanilla in the ritual; coordinating
  pass synthesizes them in Phase 1.
- D8. Menu sounds beyond the click: none exist today. Adding
  hover or select sounds is new design, not conversion. Skip
  unless Carter asks.
- D9. DelayedDamageMixin: preserve the original DamageSource
  through the delay (shared combat behavior change) or accept
  that delayed hits land as player attacks (status quo). Not a
  conversion swap either way.
- D10. Tail glyph particles (DAMAGE_INDICATOR, HAPPY_VILLAGER):
  new bespoke glyph types vs leave as state feedback.
  Recommendation: leave and list, unless Carter wants absolute
  zero.
- D11. Environmental damage lines (PlanetDimension drown,
  GravityTrainingBlockEntity generic): type them (crush for
  gravity; drown has no fitting family) or leave and list.
  Recommendation: leave both, list both.
- D12. OtherworldGuide texture: a mod-authored texture must be
  created before the one-line renderer swap can happen. Art task,
  coordinating pass or Carter.
- D13. Small new bank entries: radar_blip for the Dragon Radar,
  a level-up chime for progression, an armor equip event, a
  robot repair event. Each is one synthesized event; the
  coordinating pass can batch them into the Phase 1 bank update.
- D14 (context, not a scope 2 decision): the first pass's damage
  TYPE TAGS were approved by Carter separately and are
  coordinating-pass work running in parallel; they are not part
  of this fan-out.

## Section 9: New blocks and entities, where the permission applies

- Frieza Soldier ranged attack (D4): today an instant 5.0 hit at
  range. Option A, zero registration cost: spawn the existing
  KiBlastEntity as the soldier's shot (it already has synced
  color, damage, scale, a renderer, and an indirectMagic hit that
  this pass converts to DbaDamage.ki). Option B, full bespoke: a
  dedicated soldier blaster projectile entity. Registration cost
  either way beyond Option A: one EntityType in DbaEntities, one
  renderer registration in MobClient, spawn code in
  FriezaSoldierEntity. Recommendation: Option A unless Carter
  wants the soldier shot to look distinct from player ki blasts.
- Otherworld clouds (D5): a dedicated cloud ENTITY is possible
  under the permission (entity type, renderer, spawn and cull
  logic, sync), but the cloud manager is already a particle
  system doing this job well; the bespoke gap is only the
  particle TYPE it feeds on. Recommendation: new particle type,
  not an entity. The entity route is costed here so the choice is
  informed: roughly one entity class, one renderer, registration
  in DbaEntities and the client init, and a rewrite of the
  manager's spawn logic.
- Wish ritual: NO new entity recommended. WishRitualVisuals
  already renders the ritual client side from ritual state; a
  ritual column entity would add sync surface (entity type,
  tracked data, spawn and despawn plumbing in DragonWishRitual)
  for no visual gain over converting the visuals in place.
- Ki techniques: no new entities or blocks needed. All seven
  technique entities exist and take conversions in place; the
  barrier's synced seed and the other entities' seeds are data
  fields, not new types.
- TimeShatter: no new entity needed. The fix is two int fields, a
  synced SEED data field on the existing entity, and the renderer
  rebuild.
- Menus: no new block is relevant. The gravity training damage
  line lives in an existing block entity; nothing in menu scope
  is served by a new block.
- Mobs generally: no new mob entities proposed. The four hostile
  mobs and the guide convert in place.

## Proposed Phase 2 fan-out

Phase 1 (coordinating pass) lands FIRST, because five areas are
blocked on foundation pieces:
- Bank additions via tools/gen_bespoke_sounds.py: ui_click,
  dragon_growl, dragon_flap, wish_granted, the D2 mob voice sets
  if approved, and the D13 small entries (radar_blip, level-up
  chime, armor equip, robot repair) as decided.
- CLOUD_PUFF particle type if D5 approves (registration in
  DbaParticles, provider style in DbaParticleProviders, texture
  from tools/gen_bespoke_textures.py).
- OtherworldGuide texture if D12 is actioned.
- DimensionSpecialEffects registrations and the four JSON edits
  if D6 approves (with the dual-version API check first).
- Already in the bank and only needing call sites, do NOT
  regenerate: KAMEHAMEHA_CHARGE, KAMEHAMEHA_FIRE,
  TRANSFORM_GENERIC, SPACE_POD_LAUNCH.

Phase 2 agents (one per area, own files only, no commits, report
per area in the first pass's format):

- A1 Ki Techniques: ki/KiTechniqueHandler.java, ki/KiTechnique.java
  (zero references today; owned for completeness), ki/KiBarrier.java,
  ki/KiTechniqueType.java (zero references; owned for completeness),
  entity/KiBeamEntity.java, entity/KiBlastEntity.java,
  entity/KiDiskEntity.java, entity/KiExplosionEntity.java,
  entity/KiLaserEntity.java, entity/KiSpiralBeamEntity.java,
  entity/KiBarrierEntity.java, client/render/ki/KiBeamRenderer.java,
  client/render/ki/KiBlastRenderer.java,
  client/render/ki/KiDiskRenderer.java,
  client/render/ki/KiExplosionRenderer.java,
  client/render/ki/KiLaserRenderer.java,
  client/render/ki/KiSpiralBeamRenderer.java,
  client/render/ki/KiBarrierRenderer.java. NOT owned:
  client/render/ki/KiRenderHelper.java, BespokeGeometry.java,
  LayeredRender.java (shared). After A1 and A2 both verify zero
  primitive callers, the coordinating pass decides whether
  KiRenderHelper's 5 primitive methods are deleted or left as
  dead code; no agent deletes them unilaterally.
- A2 Wish Ritual: wish/DragonWishRitual.java,
  entity/ShenronEntity.java, entity/PorungaEntity.java,
  entity/DbaGeoDragon.java, client/wish/WishRitualVisuals.java,
  client/gui/WishScreen.java (zero references; owned so the ritual
  surface has one owner), client/hooks/WishClient.java (zero
  references; owned likewise). NOT owned:
  client/render/geo/DbaDragonRenderers.java (shared with mob
  registration), the dragon ball blocks and payloads (zero
  references; left unowned deliberately, any surprise found there
  is reported, not edited).
- A3 Mobs: entity/DbaHostileEntity.java,
  entity/FriezaSoldierEntity.java, entity/NamekianWarriorEntity.java,
  entity/PatrolRobotEntity.java, entity/SaibamanEntity.java,
  entity/OtherworldGuideEntity.java, entity/DbaHostileSpawns.java
  (zero references; owned as the family's spawn table),
  client/render/OtherworldGuideRenderer.java. NOT owned:
  client/hooks/MobClient.java, client/hooks/GearClient.java,
  client/render/geo/DbaDragonRenderers.java (registration wiring,
  no changes needed).
- A4 Progression and Forms: network/DbaNetwork.java,
  tail/TailHelper.java. (DragonBlockArcaneDBA.java is shared: its
  single line 224 swap is a coordinating-pass fix, not agent work.)
- A5 Utility Items and Nimbus: item/SenzuBeanItem.java,
  item/RecoveryCapsuleItem.java, item/DragonRadarItem.java,
  item/PatrolRobotItem.java, item/FlyingNimbusItem.java,
  item/DbaArmorMaterials.java, entity/FlyingNimbusEntity.java.
  (client/render/FlyingNimbusRenderer.java carries zero vanilla
  references and is excluded.)
- A6 Dimension Ambience: dimension/DimensionTravel.java,
  dimension/PlanetDimension.java,
  dimension/OtherworldStationGenerator.java (zero references;
  owned as the dimension package's remainder),
  client/particle/OtherworldCloudParticleManager.java,
  block/YellowCloudBlock.java, and, only if D6 approves, the four
  dimension_type JSONs and the new effects registration class the
  coordinating pass creates for it.
- A7 TimeShatter: entity/TimeShatterEntity.java,
  client/render/TimeShatterRenderer.java. NOT owned:
  item/WhisStaffItem.java (converted in the first pass; the fix
  preserves its constructor calls).
- Menus and UI: no agent. The coordinating pass makes the
  DbaUi.playClick() swap itself when it lands the ui_click bank
  event (area 4).

Coordinating-pass micro-fixes (shared files, done centrally at
merge, none delegated): fx/SlamDebris.java (2 particle lines),
entity/DebrisEntity.java (1 particle line),
mixin/WindChargeMixin.java (1 damage line),
mixin/LivingEntityMixin.java (1 dust construction),
DragonBlockArcaneDBA.java (1 sound line),
block/entity/GravityTrainingBlockEntity.java (1 damage line, only
if D11 decides to type it).

Ordering constraints:
1. Phase 1 foundation first: A2 waits on the dragon bank events,
   A3 on the D2 voice decision and events, A4 on the level-up
   decision, A6 on CLOUD_PUFF, the menu swap on ui_click.
2. A1 and A7 are unblocked now: every sound they need already
   exists in the bank.
3. KiRenderHelper primitive deletion, if chosen, comes after A1
   and A2 both report zero callers, at merge.
4. Every agent verifies on both classpaths against the standing
   baseline (132 kinds on 26.2, 141 on 26.3), zero new kinds.

Risks, stated plainly:
- Mob voice synthesis (D2) is the likeliest place for a conversion
  to read worse than vanilla; the revert-on-worse rule from the
  standard applies per family.
- DimensionSpecialEffects (D6) is the only item whose API must be
  proven on both versions before work starts; if the registration
  differs, it becomes its own dual-version shim task.
- DbaNetwork (A4) is a large shared networking file: the agent
  swaps sound and particle constants only, never payload shapes or
  handler logic.
- The TimeShatter life raise changes how long a Whis Staff impact
  lingers on screen. It is the assigned defect fix, but the exact
  new lifetime should be confirmed by the coordinating pass before
  A7 starts, not improvised by the agent.
- DelayedDamageMixin (D9) sits underneath ALL player damage,
  including everything this scope converts. Until Carter decides,
  delayed hits will keep landing as player attacks no matter what
  any agent does; the final audit of this scope must say so if it
  is still true.
