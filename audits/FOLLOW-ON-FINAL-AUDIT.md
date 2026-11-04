# DBA Follow-On Final Audit

This is the final integration audit for the follow-on batch that started
after the bespoke merge at dc2cd1f. It covers the damage type tags, the
Scope 2 foundation, the seven Scope 2 conversion areas, the Lava Sword
full redesign, and the Evil Spear dread effect.

Commit chain covered by this audit:

- 8058771: damage type tags, already pushed before this integration.
- ef5ad02: Scope 2 foundation, committed locally before the area work.
- The final integration commit containing the files reviewed here sits
  directly on top of ef5ad02.

Verification in this audit is the established javac audit method only.
No Gradle build was run, and no Gradle pass is claimed. No in-game look
was verified in this VM. Carter's PC build and test remain the first
real look and listen for the converted visuals and synthesized sounds.

## Executive result

The batch is integrated as one tree with no ownership violations found.
The final tree has:

- Zero vanilla ParticleTypes call sites or imports in mod code.
- Zero DustParticleOptions constructions.
- Zero vanilla SoundEvents references.
- Zero KiRenderHelper primitive call sites.
- Ability damage in the converted areas on DbaDamage families, with
  the documented game-system exceptions listed at the end of this
  audit.
- A complete Lava Sword ground system built from one custom material
  and one persistent field engine, combining all six approved ideas.
- A separate Evil Spear dread effect that adds darkness and red eyes
  without changing any spear damage, mark, hunt, or cooldown behavior.
- Dual javac sweeps with no new error kinds on either Minecraft
  version. The 26.3 final tree has two fewer baseline error kinds than
  the foundation baseline.

## Damage type tags already pushed as 8058771

The bespoke pass created 12 custom damage families but initially gave
them no vanilla damage_type tag memberships. Commit 8058771 closed
that parity gap with six data files under
data/minecraft/tags/damage_type, all using "replace": false. No Java
changed in that commit.

Tag memberships added:

- bypasses_armor: hellfire, spirit, tempest, temporal, void.
- no_knockback: hellfire, spirit, tempest, temporal, void.
- witch_resistant_to: spirit, tempest, temporal, void.
- bypasses_wolf_armor: spirit, tempest, temporal, void.
- avoids_guardian_thorns: spirit, temporal, void.
- is_fire: hellfire.

Per-family outcome:

- Void, spirit, and temporal received the full magic combat profile.
  Their pure or dominant replaced sites were magic, including the
  recurring effect damage that had the clearest parity claim.
- Tempest received bypasses_armor, no_knockback, witch_resistant_to,
  and bypasses_wolf_armor. Its signature Azure hits used
  indirectMagic as a primary branch. The is_lightning tag was not
  restored because lightningBolt was only one fallback branch of one
  site and most tempest damage is wind.
- Hellfire received is_fire, bypasses_armor, and no_knockback,
  anchored on DevilsHandsEffect as the pass's only pure onFire
  conversion. The accepted compromise is that player-owned spear and
  trident hits that used playerAttack before conversion now count as
  fire, so fire-immune mobs can resist them.
- Ki, valor, darkness, curse, and gravity received no tags because
  their replaced sources were player-attack class or projectile and
  mob fallbacks with no magic-class primary to restore.
- Crush received no tags. A family-wide armor bypass would have made
  every flail, mace, ox, and pole crush hit ignore armor. Three
  minority losses were accepted and named in
  audits/bespoke-damage-tags.md: the OxFissure indirectMagic site,
  the ZShockwave indirectMagic and inWall site, and Ox generic
  self-damage.
- Lava received no tags and deliberately did not receive is_fire.
  No fire-type vanilla source was ever replaced by the lava family,
  so adding is_fire would have been a new behavior change rather
  than parity. It remains a design choice Carter can request later.

The full archaeology, source counts, and excluded-tag reasoning are
in audits/bespoke-damage-tags.md and the addendum in
audits/BESPOKE-FINAL-AUDIT.md.

## Scope 2 foundation at ef5ad02

The foundation commit contains 56 files. It built the shared pieces
the seven area conversions needed, applied the named central fixes,
and recorded all D1 to D14 dispositions in
audits/scope2-foundation-report.md.

### Foundation additions

Sound bank:

- Twenty-one new synthesized events were appended through
  tools/gen_bespoke_sounds.py and wired in DbaSounds: ui_click,
  dragon_growl, dragon_flap, wish_granted, radar_blip, level_up,
  equip, repair, and twelve mob voice events for soldier, warrior,
  robot, and saibaman.
- The 55 older ogg files were not changed in substance. Regeneration
  changed only ogg stream serials while decoded PCM stayed identical
  on the sampled files, so the older files were restored and only
  the 21 new ogg files entered the foundation commit.
- sounds.json reached 82 entries.

Particles:

- CLOUD_PUFF was registered and styled for Otherworld cloud systems.
- DAMAGE_SPARK was registered for tail damage feedback.
- HAPPY_SPARKLE was registered for tail heal feedback.
- Each of the three types has a particle JSON and four generated
  texture frames.

Other foundation assets and data:

- textures/entity/otherworld_guide.png was generated on the exact
  VillagerModel UV layout from the 26.2 jar. The guide wears an
  indigo robe with gold trim and a circlet; the hat regions are
  transparent.
- The four dimension_type JSON files for Namek, Otherworld, Vegeta,
  and Yardrat stopped carrying the dead overworld effects value and
  gained mod-authored visual attributes. Namek is green with teal
  water fog, Otherworld is yellow with pink-tinted clouds, Vegeta is
  harsh pale grey-blue, and Yardrat is soft blue-lavender. Gameplay
  fields stayed byte-identical and cloud_height was not set.

### D1 to D14 dispositions

- D1, Ki damage family: approved and completed in A1. All six Ki
  ability damage sites use DbaDamage.ki. No new family was created.
- D2, mob voices: approved and built. Twelve synthesized voice events
  cover soldier, warrior, robot, and saibaman ambient, hurt, and
  death roles. A3 wired every existing call site. ROBOT_AMBIENT is
  registered but intentionally has no call site because
  PatrolRobotEntity never had an ambient sound override and vanilla
  iron golems have no ambient sound; adding one would be new
  behavior, not conversion. Synthesized voices remain the highest
  listen-only risk in Scope 2.
- D3, mob ability damage: approved and completed in A3. The Frieza
  soldier ranged shot and the Saibaman self-destruct use
  DbaDamage.ki. Mob contact melee remains vanilla Mob plumbing,
  matching the player plain melee exemption.
- D4, Frieza ranged shape: conversion only. The shot remains an
  instant hit. No KiBlastEntity reuse or new projectile was added.
- D5, Otherworld clouds: approved and completed. CLOUD_PUFF was
  built by the foundation and wired by A6 at all three call sites.
  No cloud entity was created because the existing manager is
  already a particle system.
- D6, dimension looks: approved, with a required mechanism
  correction. DimensionSpecialEffects does not exist in either the
  26.2 or 26.3 jar, DimensionType has no effects component, and the
  old JSON effects line was dead data. The intended result was
  implemented through the version-correct attributes map instead.
  No client effects class or registration exists to add. A6 also
  aligned the Otherworld biome cloud color to the dimension's pink.
- D7, wish sounds: approved and completed. Dragon growl, dragon
  flap, and wish-granted fanfare were synthesized by the foundation
  and wired by A2.
- D8, extra menu sounds: skipped. Hover and select sounds did not
  exist, so adding them would have been new design rather than
  conversion. The existing click was converted.
- D9, DelayedDamageMixin: approved and implemented as a combat
  behavior fix, detailed in its own section below.
- D10, tail glyphs: approved and completed. DAMAGE_SPARK and
  HAPPY_SPARKLE replaced the vanilla damage and happy villager
  particle glyphs in A4.
- D11, environmental damage: left vanilla and listed. PlanetDimension
  drown damage and GravityTrainingBlockEntity generic damage are
  game-system semantics, not ability presentation.
- D12, guide texture: approved and completed. The foundation made
  the texture and A3 changed the renderer to use it.
- D13, small bank entries: approved and completed. Radar blip,
  level up, equip, and repair were synthesized. TRANSFORM_GENERIC
  already existed and A4 wired it at form activation.
- D14, damage tags: context only for Scope 2. The tags were approved
  separately and already pushed as 8058771.

### Foundation central fixes

The foundation applied these shared fixes centrally:

1. DbaUi.playClick now uses DbaSounds.UI_CLICK. That one method is
   the click path for the DBA screens, so the menus and screens area
   needed no separate agent.
2. SlamDebris block particles now use DbaParticles.BLOCK_CHUNKS and
   its poof now uses DbaParticles.DUST_PUFF.
3. DebrisEntity landing dust now uses DbaParticles.DUST_PUFF.
4. WindChargeMixin's Bansho wind charge hit now uses
   DbaDamage.tempest instead of a vanilla playerAttack source.
5. LivingEntityMixin's Whis Staff auto-dodge burst now uses
   DbaDustOptions on DbaParticles.THEME_DUST at the same color and
   scale.
6. The main mod class Azure lock-on sound now uses
   DbaSounds.CHARGE_RISE at the same volume and pitch.

GravityTrainingBlockEntity was deliberately not changed under D11.

### Final merge central fixes

The final fresh grep found one real presentation area that no Scope
2 area agent owned: movement landing and impact feedback. The merge
made only token swaps at identical counts, volumes, pitches, and
sound sources:

- MovementPhysics landing dust: ParticleTypes.POOF to
  DbaParticles.DUST_PUFF.
- MovementPhysics big landing sound: SoundEvents.PLAYER_BIG_FALL to
  DbaSounds.SLAM_THUD.
- MobPhysics ground-bounce dust: ParticleTypes.POOF to
  DbaParticles.DUST_PUFF.
- MobPhysics ground-bounce sound: SoundEvents.PLAYER_SMALL_FALL to
  DbaSounds.THUNK.
- MobPhysics wall-splat sound: SoundEvents.SHIELD_BLOCK to
  DbaSounds.SHIELD_CLANK.

No movement physics, damage amount, launch, bounce, splat, or timing
logic changed in those fixes. MobPhysics generic impact damage stays
on the intentionally-left list because it is physics-system damage.

## D9 DelayedDamageMixin behavior fix

DelayedDamageMixin is shared combat infrastructure, so this change
is recorded as a behavior fix rather than a cosmetic swap.

Before the foundation, every buffered player-attributed hit was
rebuilt at the pop as playerAttack, or generic when no attacker was
available. That silently discarded the original bespoke damage type,
its death message, its armor behavior, and the tag parity restored
by 8058771.

The mixin now stores the DamageSource of the largest buffered hit
and applies the accumulated total under that stored source when its
causing entity is still present and alive. Only player-attributed
hits are buffered, so the stored causing entity is a player by
construction. The old playerAttack and generic rebuild remains as a
fallback in the same order when the stored source cannot be used.
Accumulation, the ten-tick window, cinematic-lock refresh, drain
branches, hurt animation, markHurt handling, and KiBarrier deferred
damage flags are unchanged.

Accepted subtlety: a stored source's direct entity may have
despawned by pop time, such as a spent Ki blast. The damage type,
armor behavior, and tags still come from the stored source, which is
the purpose of the fix.

## Scope 2 area outcomes

### A1 Ki Techniques

Report: audits/scope2-report-ki-techniques.md.

- Vanilla sound call sites went from 23 to 0.
- Vanilla damage lines went from 6 to 0. Beam, laser, explosion
  area, disk, and blast hits use DbaDamage.ki. Explosion self-damage
  uses the no-attacker form of DbaDamage.ki, preserving the old
  magic self-damage shape.
- The area's four real DustParticleOptions constructions became
  DbaDustOptions on THEME_DUST at identical colors and scales. The
  audit's fifth tree-wide construction was in LivingEntityMixin and
  the foundation had already converted it.
- Sixteen KiRenderHelper primitive calls and five local
  vertex-emitting helpers went to 0.
- All seven Ki visual entities gained a synced SEED field using the
  BoltEntity pattern. Constructor signatures did not change.
- KAMEHAMEHA_CHARGE and KAMEHAMEHA_FIRE, previously registered with
  zero call sites, now carry the beam technique.
- Beam and laser geometry uses crossed torn ribbons; spiral uses a
  torn ribbon core plus broken vortex band segments; disk uses
  blade arcs and a shard fan; explosion uses displaced dome pairs,
  a plume, and a broken vortex band; blast uses torn ribbons, a
  shard fan, and bands; barrier uses displaced dome pairs and
  broken vortex bands.
- Damage amounts, costs, cooldowns, radii, lifetimes, and hit logic
  did not change.

Honest visual trades from A1: beam and laser tips taper instead of
ending uniformly; the spiral's two separate helix strands read as
one broken sheath; the disk loses its clean center hole and perfect
annulus; the barrier loses the old separately wound inner shell
shimmer; the blast loses its faceted diamond body and explicit nose
cone gradient.

### A2 Wish Ritual

Report: audits/scope2-report-wish-ritual.md.

- Twelve vanilla sound call sites went to 0 at identical volumes,
  pitches, and sources.
- Dragon arrival uses DRAGON_GROWL, departures use DRAGON_FLAP and
  TELEPORT_WHOOSH, grants use WISH_GRANTED, and ritual and grant
  charge beats use CHARGE_RISE.
- WishRitualVisuals went from eight KiRenderHelper primitive calls
  and four local geometry helpers to zero of each.
- Dragon balls are displaced dome pairs with shard fan star pips at
  the old pip positions. The summoning column is a plume core with
  broken vortex band shells and branch ribbons. Every ring is a
  broken vortex band pair. Motes are plume blobs and the arrival
  flash is a displaced dome pair.
- The client ritual seed is mixed only from synced payload state:
  game time, the Namek flag, and the center derived from synced
  dragon ball positions. No new network field was added.
- Five mulPose(Quaternionf) sites in WishRitualVisuals were changed
  to the dual-version Matrix4f rotation form. Three were introduced
  during conversion and two were pre-existing. This removed one
  26.3 baseline error kind.
- DbaGeoDragon, WishScreen, and WishClient were checked and left
  unchanged because they had zero vanilla references.

Honest visual trades from A2: dragon balls are lumpy and have a
faint equator seam; pips are flat shard bursts rather than round
beads; rings are ribbons with seeded gaps rather than continuous
tubes; the column core reads as stacked light beads at full height;
the branch ribbons wrap about 1.2 turns instead of the old four-turn
coils; the column shells now turn slowly where the old cylinder was
static.

### A3 Mobs

Report: audits/scope2-report-mobs.md.

- Vanilla SoundEvents references went from 17 to 0.
- Vanilla ParticleTypes references went from 2 to 0.
- Vanilla damage lines went from 2 to 0.
- The tree's only vanilla renderer texture reference went from 1
  to 0.
- Frieza Soldier, Namekian Warrior, Patrol Robot, and Saibaman use
  their own synthesized ambient, hurt, and death voices where call
  sites exist. Distinct death events replace the old pattern of
  reusing the hurt sound for death.
- Frieza's ranged hit particles use CRIT_SPARK. Saibaman's burst
  uses BLOCK_CHUNKS with a slime block state, preserving the green
  chunk character better than a neutral dust puff.
- Frieza's ranged shot and Saibaman's self-destruct use
  DbaDamage.ki(this, this), matching the direct-plus-attacker shape
  of the old mobAttack sources at unchanged amounts.
- OtherworldGuideRenderer now uses the mod-authored guide texture.
  Its VillagerModel and render state remain the platform model for
  that NPC.
- Mob stats, behavior, AI, spawn rules, and the Frieza instant-hit
  shape did not change.

Honest trades from A3: synthesized voices and distinct death cries
are the most audible change in the area; Saibaman burst chunks are
more cubic than slimeball sprites; the guide's appearance changes
by design.

### A4 Progression, Forms, and Tail

Report: audits/scope2-report-progression.md.

- DbaNetwork went from 17 vanilla sound call sites and one vanilla
  particle call site to zero of each.
- TailHelper went from four vanilla particle call sites and three
  vanilla sound call sites to zero of each.
- Form activation now uses TRANSFORM_GENERIC with the existing
  charge, boom, thunder, and zap layers. Level-up beats use
  LEVEL_UP, form toggles use UI_CLICK, technique equip uses EQUIP,
  and technique save uses CAST_MAGIC.
- Tail sever uses CRIT_SPARK, DAMAGE_SPARK, and DUST_PUFF. Tail
  regrow uses HAPPY_SPARKLE. Tail break and crit sounds use
  ITEM_SHATTER and CRIT_HIT.
- Packet shapes, progression math, form requirements, tail physics,
  volumes, pitches, particle counts, spreads, and speeds did not
  change.

Honest trades from A4: form activation has a new lead timbre;
detransformation reads darker through RELEASE_DARK and EXTINGUISH;
the tail glyphs carry the same color and motion meaning but no
longer use heart-shaped vanilla glyphs.

### A5 Utility Items and Nimbus

Report: audits/scope2-report-utility-items.md.

- Seventeen vanilla sound lines across seven files went to 0.
- There were no vanilla particles, dust constructions, or damage
  lines in this area before or after; sounds were the whole task.
- Dragon Radar distance beeps use RADAR_BLIP on one event, with the
  old pitch ladder and volume split preserving found versus
  not-found feedback.
- Armor equip suppliers use EQUIP for every DBA armor piece. The
  holder construction was checked against the 26.2 jar and the
  registration order in the main class.
- Senzu and capsule milestone, robot repair, and nimbus flight
  sounds use LEVEL_UP, CHARGE_FULL, REPAIR, and FLIGHT_WIND.
- Item behavior, radar logic, armor stats, and nimbus flight did
  not change.

Judgment calls recorded by A5: the two vanilla burp sites use THUNK,
the capsule drain uses FIZZLE, nimbus materialize and break sounds
use WIND_GUST, and the nimbus return uses CRYSTAL_CHIME. The burp
loses its comedic gulp character; if Carter's listen rejects it, a
dedicated synthesized gulp belongs in the bank rather than a local
workaround.

### A6 Dimension Ambience

Report: audits/scope2-report-dimension-ambience.md.

- All three vanilla CLOUD particle call sites now use CLOUD_PUFF:
  two in OtherworldCloudParticleManager and one in
  YellowCloudBlock. Counts, spreads, velocities, and spawn
  conditions are unchanged.
- DimensionTravel's four vanilla sounds went to 0. Launch uses
  SPACE_POD_LAUNCH plus BOOM_BIG. Arrival uses RIFT_OPEN plus
  CHARGE_RISE beside the pre-existing custom charge layer.
- The Otherworld biome cloud color changed from a yellow value to
  the dimension definition's pink value, exactly
  "#e6f2a7c3". This fixes a biome-level override that made clouds
  yellow against the yellow Otherworld sky. Cloud height and all
  gameplay biome fields are unchanged.
- PlanetDimension was not edited. Its drown line remains under D11.
- OtherworldStationGenerator had no vanilla references and was not
  edited.

Honest trade from A6: CLOUD_PUFF is larger and longer-lived than
vanilla CLOUD, so at unchanged spawn counts the Otherworld cloud
formations will read fuller and softer. If they read crowded in
game, the future fix is a manager spawn-count trim, not a particle
type change.

### A7 TimeShatter

Report: audits/scope2-report-timeshatter.md.

All three documented defects were fixed:

1. Lifetime rose from 18 ticks to 36 ticks in both the maximum and
   current lifetime fields.
2. Alpha now uses FxEnvelope with a five-tick smoothstep build, a
   hold, and the old exit curve. The burst no longer appears at full
   alpha on frame one.
3. The entity gained a synced and persisted SEED field. The
   renderer derives separate streams for dial, spokes, shards, and
   core, so shard parameters no longer restart and correlate with
   spoke parameters.

Geometry conversion:

- Four local vertex-emitting helpers and 32 direct addVertex lines
  went to 0.
- The dial is two broken vortex bands, spokes are paired branch
  ribbons, shards are shard fan clusters with torn ribbon wakes,
  and the core is a displaced dome pair with a brighter nested pair.
- WhisStaffItem's three spawn sites were read and compiled in the
  verification closure but not edited. Constructor signatures are
  unchanged.
- The entity still deals no damage. Whis Staff reversal mechanics
  and timing in the item are unchanged.

Honest trades from A7: the core is rounder than the old octahedron;
shards read as glass spray clusters instead of single clean prisms;
spokes lose their smooth bow and color gradient; dial gaps are
seeded at spawn rather than prying open with progress; the burst
now blooms over 36 ticks rather than snapping in 18 ticks.

## Lava Sword full redesign

Report: audits/lava-redesign-report.md.

The redesign replaces the old split of temporary fire cells and
short-lived real lava sources with one custom material and one
engine, while retaining the legacy fire system for scorch rings.

### One material and one engine

Magma crust is the material for pools, channels, waves, blob splats,
and meteor craters. It has three cooling stages:

- Stage 0, molten: light 15, full cell touch damage, and 50 fire
  ticks on contact.
- Stage 1, cooling: light 9, 40 percent touch damage, and 20 fire
  ticks on contact.
- Stage 2, dark: light 2, no touch damage, and a walkable slab six
  sixteenths high.

Stages 0 and 1 have no collision, so a target wades through hot
crust while it burns. Stage schedules by profile are:

- POOL: cooling at 60 ticks, dark at 120, restore at 200.
- CHANNEL: cooling at 40 ticks, dark at 80, restore at 140.
- WAVE: cooling at 14 ticks, dark at 30, restore at 54.

LavaField is the engine. It validates placement, owns stage
schedules and budgets, handles touch routing, adopts persisted
entries after a restart, restores entries on schedule, restores all
entries on server stopping, and clears its adopted set after server
stopped. Its SavedData id is "lava_field".

### New blocks and block entity

- MagmaCrustBlock: registered without a BlockItem. It has a STAGE
  property from 0 to 2, stage-based light, three block models, and
  six generated crust textures: a top and a side for each stage.
- MiniVolcanoBlock: registered without a BlockItem. It is
  unbreakable while standing, has light 13, one block model, and a
  generated volcano rock texture plus the molten crust crater top.
- VolcanoBlockEntity: registered for MiniVolcanoBlock. It persists
  owner, expiry, next eruption, and blob damage, drives eruptions,
  and hands expiry back to LavaField.

### New entities and renderers

- LavaGeyserEntity: an 18-tick stationary pillar. At tick 2 it hits
  a radius 1.3 by height 4.5 column, applies its damage, 60 fire
  ticks, and a 0.95 upward launch. At tick 3 it rains three blobs at
  35 percent of its damage.
- LavaBlobEntity: the shared ballistic blob. It uses manual gravity
  0.06 and a discrete arc launch helper. A direct hit deals full
  blob damage and 60 fire ticks. A landing deals 60 percent splash
  in radius 1.3, 40 fire ticks, and one POOL crust cell whose touch
  damage is 50 percent of blob damage.
- LavaMeteorEntity: a falling meteor with seeded drift. Impact
  deals full meteor damage in radius 3.2, applies 80 fire ticks and
  outward push, plays custom boom and crack sounds, spawns debris,
  places a fire-only scorch ring, and places up to five POOL crust
  cells with touch damage at 12 percent of impact damage.

All three entities are registered in DbaEntities and all three
renderers are registered in the client entrypoint. Their renderers
use BespokeGeometry builders from synced seeds.

### How the six ideas combine

1. Flowing channels: when a cleave flight ends, LavaField carves a
   channel back along the recorded centerline and then follows a
   greedy downhill path for up to six more cells, two ticks apart.
2. Eruption pillars and raining blobs: eruptions raise geysers, and
   geysers rain LavaBlobEntity projectiles that splat into pools.
   A cleave block landing also raises a mini geyser.
3. Cooling crust: every pool, channel, wave, splat, and crater uses
   the same three-stage material and only burns while hot.
4. Magma wave: the cleave lays a three-cell-wide wave under its
   flight path, with a budget of 44 WAVE cells per cleave and touch
   damage at 15 percent of cleave damage.
5. Meteor finisher: a full sneak charge calls seven meteors onto
   the looked-at point, and each impact creates pools and a scorch
   ring.
6. Volcano ultimate: a planted MiniVolcanoBlock keeps erupting for
   600 ticks, lobbing blobs and applying heat pulses around its
   cone.

### Triggers, costs, cooldowns, and unchanged numbers

Existing moves kept their numbers:

- Melee: 860 attribute damage and a 60-tick burn remain unchanged.
- Cleave, left click: damage 740 plus Strength times 2.0, Ki cost
  drainKiDiscrete(90, 14), and item cooldown 8 remain unchanged.
  Its wave touch damage is 15 percent of cleave damage. A block
  landing raises a mini geyser at 45 percent of cleave damage.
- Eruption, right click: damage 880 plus Strength times 2.2, Ki
  cost drainKiDiscrete(90, 40), and item cooldown 40 remain
  unchanged. Its ring is fire only now. It can place up to eight
  POOL cells at 12 percent touch damage and up to four geysers at
  36 percent of eruption damage, with at most two geysers under hit
  targets and two more at the lip at tick 6.

New moves:

- Meteor finisher: sneak and hold right click for 50 ticks. Charge
  uses CHARGE_HUM and CHARGE_FULL feedback. Release at full charge
  costs drainKiDiscrete(90, 80), targets a point up to 64 blocks
  away, and calls seven meteors. Each impact deals 520 plus
  Strength times 1.6. Readiness is 400 ticks in a timestamp map, so
  it does not block cleave or eruption. An undercharged release or
  failed Ki drain fizzles with no cost and no readiness.
- Volcano ultimate: sneak and left click through the existing
  cleave packet path. It targets ground under the crosshair within
  seven blocks. It costs drainKiDiscrete(90, 200), has a 1200-tick
  readiness, allows one active volcano per player, and lasts 600
  ticks. It erupts every 36 to 54 ticks, lobbing three to five
  blobs at 100 plus Strength times 0.6. A heat pulse deals 50
  percent of blob damage with 40 fire ticks and push. Failed
  readiness, target, protection, occupancy, or duplicate checks
  cost nothing and report on the actionbar.

All redesigned ability damage uses DbaDamage.lava and the existing
capDamage points.

### Safety contract

- Every crust cell, channel cell, and volcano site is temporary and
  restores.
- Every field entry is written to SavedData when created, updated
  when a wave cell becomes a channel cell, and removed when it
  resolves.
- Persisted entries are adopted on the first tick of a server run.
  Overdue entries restore, live entries resume, entries whose cells
  no longer hold field material are dropped, and unfinished channel
  fronts still arrive.
- Placement only fills an air cell above sturdy ground.
- Ground must pass both GroundScar.isProtected and
  LavaSwordGround.isProtectedGround. Protected blocks and block
  entity sites are refused.
- Nothing in the redesign breaks or replaces a non-air block.
- Caps are 800 field cells per level and 240 per owner, with
  per-move budgets for wave cells, pools, geysers, blobs, meteors,
  and volcanoes.
- Pools burn everyone, including the planter, as the real lava
  sources they replaced did. That zoning behavior is deliberate.

### Legacy system and one dual-version fix

LavaSwordGround remains for fire cells used by the eruption ring
and meteor scorch ring. Those rings place vanilla fire states
through BaseFireBlock; they no longer place real lava sources.

LavaSwordGround.scorchColumn no longer calls blocksMotion, which
was removed in 26.3. It uses the sturdy-face floor test only. This
removed the file's pre-existing 26.3 baseline error kind and did
not change its stricter 26.2 behavior.

### Honest Lava trades

- Real lava sources are gone from the sword's ground system. Crust
  does not flow, cannot be bucketed, does not ignite neighbors like
  a lava source, and makes no obsidian or cobblestone with water.
- The eruption's instant ground burn is weaker than up to eight old
  lava sources. Its pools last longer but deal only 12 percent of
  eruption damage on touch.
- The old landing pile behavior from the earlier ground fix is
  replaced by a landing geyser and crust.
- Pool crust can remain for 200 ticks, including a harmless dark
  slab stage, where the old ground cleared at 80 ticks.
- Sneak plus left click no longer cleaves; it tries to plant the
  volcano.
- A planted volcano cannot be broken early, including by its own
  planter.

## Evil Spear dread effect

Report: audits/evil-spear-dread-report.md.

### Host choice

The dread is a new status effect, evil_dread, rather than a change
to MarkedByEvilEffect. The mark is shared with Devil Trident, is
read by EvilSpearProjectileEntity for its 1.3 times marked-target
multiplier, and is read by DelayedDamageMixin. A separate effect
keeps the trident and every mark mechanic untouched.

The spear applies evil_dread beside its own mark at the same three
sites and durations:

- Melee hit: 120 ticks.
- Lunge landing: 120 ticks.
- Projectile pierce: 160 ticks.

Hell Hunt steps do not reapply the mark in the existing code, so
they do not reapply dread. Reapplication refreshes dread with the
mark. The effect deals no damage and changes no spear number,
cooldown, chain, or multiplier.

### Darkness

No vanilla Blindness or Darkness effect is used and no fog hook was
invented. Darkness has three mod-owned layers:

1. A private client vignette branch in MeltingOverlayMixin closes
   in over about 20 ticks and leaves over about 15 ticks. A
   near-black murk and seven stepped edge bands make the rim fully
   black and reduce sight to a dim tunnel. Center alpha is capped
   so the emissive eyes remain readable; that cap is one constant
   if Carter wants the center darker after his PC test.
2. A world-side shroud of two counter-turning broken vortex bands
   wraps the victim in near-black crimson smoke.
3. The effect uses DbaParticles.VOID_SMOKE as its effect particle
   type, following the Dark Faded pattern.

The client visuals respect the effects density config. Density zero
turns the dread visuals off.

### Red eyes

EvilDreadEffect maintains one invisible DreadGazeEntity anchor per
victim. The anchor follows the victim, does not collide, deals no
damage, and discards itself after ten ticks without a live afflicted
victim, covering expiry, milk cures, death, and dimension changes.
If the anchor is lost while the effect remains, the effect respawns
it.

DreadGazeRenderer draws up to seven eye pairs, scaled by effect
density with a minimum of three when visuals are enabled. Pair
placement is seeded from the victim UUID and is stable across
clients and anchor respawns:

- Radius: 2.3 to 4.6 blocks.
- Height: 0.35 to 2.6 blocks.
- Pair width: 0.55 to 0.9 blocks.
- Fade-in: staggered by pair over 12 ticks after seeded delays.
- Blink periods: 53 to 120 ticks with seeded phases, so the ring
  never blinks in unison.
- Blink shape: a six-tick vertical squash, closing in two ticks,
  shut for one, and opening in three.
- Each pair also has slow angular drift, a gentle bob, and a glow
  pulse.

The eye texture is procedurally generated at 128 by 128 pixels: a
dark bloodshot almond sclera with veins, a striated crimson iris,
a vertical slit pupil, specular glints, and baked red glow. Each
eye is drawn as a halo pass and a core pass on camera-facing
billboards, so every pair turns to stare at whoever views it.

Everyone nearby sees the eyes and shroud around any victim, player
or mob. The victim also receives the private screen darkness. In
first person, the ring sits at least 2.3 blocks away and does not
block input, attacks, collision, or line of sight.

### Dread sounds and registrations

No new sound was synthesized. Onset uses DbaSounds.VOID_AMBIENT at
0.7 volume and 0.65 pitch. HEARTBEAT_DARK plays at 0.65 volume and
0.8 pitch every 28 ticks of remaining duration.

Registrations are minimal and in existing style: EVIL_DREAD in
DbaEffects, DREAD_GAZE in DbaEntities, the renderer in the client
entrypoint, the effect icon at textures/mob_effect/evil_dread.png,
and English names for the effect and anchor entity.

## Ownership and scope review

The integration reviewed the full uncommitted surface before
staging: 57 tracked modified files and 40 component-created files,
followed by this audit as the ninety-eighth file in the final
integration commit. The foundation commit separately contains 56
files.

Ownership result:

- A1 changed only its Ki technique files.
- A2 changed only DragonWishRitual, ShenronEntity, PorungaEntity,
  and WishRitualVisuals among its seven owned files; the other
  three were verified clean and left alone.
- A3 changed only its mob files and the guide renderer.
- A4 changed only DbaNetwork and TailHelper.
- A5 changed only its seven utility and nimbus files.
- A6 changed only DimensionTravel, the cloud manager,
  YellowCloudBlock, and the Otherworld biome JSON.
- A7 changed only TimeShatterEntity and TimeShatterRenderer.
- Lava changed its sword item, cleave, eruption, ground, new field,
  blocks, block entity, entities, renderers, resources, and the
  minimal shared registrations for those pieces.
- Dread changed its effect, anchor entity, renderer, spear item,
  spear projectile, overlay mixin, and the minimal shared
  registrations for those pieces.
- Shared DbaEntities and the client entrypoint contain both the
  Lava and Dread registrations cleanly, with no overlap or lost
  registration.
- Shared DbaBlocks, DbaBlockEntities, DbaEffects, and en_us.json
  contain only the registrations and names required by the Lava
  and Dread work.
- The only merge-level code edits were the five movement feedback
  token swaps listed under final merge central fixes. One trailing
  whitespace character was also removed from
  audits/bespoke-scope2-audit.md so the staged diff check passes.

No unrelated file or generated junk was found in the integration
surface.

## Fresh goal checks

Counts below were re-derived from the integrated tree at final
merge, not copied from component reports.

### Particles

- Vanilla ParticleTypes call sites: 0.
- Vanilla ParticleTypes imports: 0.
- DustParticleOptions constructions: 0.
- Remaining lexical matches for ParticleTypes are the
  FabricParticleTypes registration API and explanatory javadoc in
  particle/DbaParticles.java. They define the mod's own types and
  are not vanilla particle use.
- Remaining lexical matches for DustParticleOptions are javadoc in
  DbaParticles and DbaDustOptions. They are not constructions.

### Sounds

- Vanilla SoundEvents references: 0.
- The movement feedback sites that remained after the area fan-out
  were converted centrally at final merge, as listed above.

### Damage source lines

Nine damageSources() lines remain, containing ten calls. Every one
is classified here:

- GravityTrainingBlockEntity, one generic call: D11 training-system
  pressure damage, left by decision.
- DbaCommand, one generic call passed as the source argument for an
  admin tail sever command: command plumbing, not ability
  presentation. The gameplay sever path supplies its own source.
- PlanetDimension, one drown call: D11 environmental hazard, left
  by decision.
- ValorFieldEntity, two lines and three calls: the documented
  suspended-projectile exception. A suspended projectile keeps
  indirectMagic, mobProjectile, or generic according to its own
  original owner and projectile type. Re-typing those hits to valor
  would misattribute another owner's damage.
- DelayedDamageMixin, two fallback calls: playerAttack and generic
  are used only after the D9 stored bespoke source path cannot be
  used.
- MobPhysics, two generic calls: ground-bounce and wall-splat
  physics-system damage, not an ability source.

No other damageSources() lines remain.

### KiRenderHelper primitives

- Primitive call sites outside KiRenderHelper: 0.
- The five primitive method definitions, drawColoredBox,
  drawCylinder, drawDisc, drawSphere, and drawHelix, remain inside
  KiRenderHelper as unused shared helper code. No converted effect
  calls them and no rendered geometry in this batch comes from
  them.

### Textures

- Java references to a vanilla entity texture: 0.
- The Otherworld guide uses the mod-authored texture while keeping
  the VillagerModel as its platform model.

## Resource validation

- All 553 JSON files under src/main/resources parse successfully.
- All eight JSON files in the final integration surface parse:
  two blockstates, four block models, en_us.json, and the
  Otherworld biome JSON.
- sounds.json has 82 entries. Every sound reference in it resolves
  to an ogg file on disk. Every DbaSounds sfx event has a sounds
  entry and an ogg file.
- Sixteen sprite-based particle JSON files contain 64 texture
  references, and every reference resolves under
  textures/particle. CLOUD_PUFF, DAMAGE_SPARK, and HAPPY_SPARKLE
  each have their JSON and all four frames.
- BLOCK_CHUNKS has no sprite JSON by design; it renders block model
  chunks through its block particle option rather than a particle
  sprite set.
- Both new blockstates resolve to existing block models. All lava
  model texture references resolve to existing PNG files.
- All nine new PNG files in the final integration surface are valid
  PNG files: seven 32 by 32 lava block textures, the 128 by 128
  dread eye, and the 64 by 64 evil dread effect icon.
- The foundation guide texture is a valid 64 by 64 PNG.
- DbaEntities contains DREAD_GAZE, LAVA_GEYSER, LAVA_BLOB, and
  LAVA_METEOR. The client entrypoint registers a renderer for each.
- DbaBlocks contains MAGMA_CRUST and MINI_VOLCANO. DbaBlockEntities
  contains VOLCANO_BLOCK_ENTITY. DbaEffects contains EVIL_DREAD.
- git diff --check is clean for the integrated code and resource
  diff.
- The em dash scan over added lines for the full batch since
  8058771, all untracked batch files, and this audit found zero
  em dashes.

## Javac audit sweeps

Method: ~/workspace/dba-audit-libs/sweep.sh, full main and client
source sets, private sweep tags for this merge, baseline worktree
at ef5ad02. The baseline worktree was removed and pruned after the
runs.

Results:

- Minecraft 26.2 baseline at ef5ad02: 132 unique error kinds.
- Minecraft 26.2 final tree: 132 unique error kinds.
- Minecraft 26.2 comm -13 baseline to final: empty. Zero new kinds.
- Minecraft 26.3 baseline at ef5ad02: 140 unique error kinds.
- Minecraft 26.3 final tree: 138 unique error kinds.
- Minecraft 26.3 comm -13 baseline to final: empty. Zero new kinds.
- Removed on 26.3: WishRitualVisuals mulPose(Quaternionf), and
  LavaSwordGround cannot find symbol for the removed blocksMotion
  API.
- Removed on 26.2: none.

These are javac audit results only. They are not a Gradle build.

## Complete still-vanilla and intentionally-left list

The following list is complete for the presentation and damage
categories audited in this batch:

1. Plain player melee remains the vanilla player attack. That is
   what a weapon hit is in Minecraft, and re-typing it would break
   the weapon pipeline. Mob contact melee likewise remains vanilla
   Mob attack plumbing with no damageSources() call in mod code.
2. All damage, including every custom DbaDamage family, still
   resolves through Minecraft's health, armor, resistance, and
   invulnerability pipeline. The custom part is the damage type,
   death messages, tags, geometry, particles, and sounds.
3. ValorFieldEntity keeps its two documented suspended-projectile
   lines so a suspended projectile's damage remains attributed to
   its original owner and projectile type.
4. DelayedDamageMixin keeps playerAttack and generic as fallback
   sources only. Its primary pop path now preserves the stored
   bespoke source under D9.
5. PlanetDimension drown damage stays vanilla under D11.
6. GravityTrainingBlockEntity generic damage stays vanilla under
   D11.
7. MobPhysics ground-bounce and wall-splat generic damage stays
   vanilla because it is physics-system impact damage.
8. DbaCommand keeps one generic source argument for an admin tail
   sever command. It is command plumbing, not a converted ability.
9. KiRenderHelper keeps five unused primitive method definitions.
   There are zero call sites, so they produce no rendered geometry.
10. The Lava Sword's legacy scorch rings still place vanilla fire
    states through BaseFireBlock. Real lava sources are gone; the
    custom crust system carries pools, channels, waves, splats,
    and craters.
11. The new lava blocks use the vanilla BASALT block SoundType as
    material infrastructure, in the same way existing DBA blocks
    use vanilla block sound types. All ability-triggered sounds in
    the redesign use DbaSounds.
12. The Otherworld guide keeps the vanilla VillagerModel and
    VillagerRenderState as platform model plumbing, with the
    mod-authored texture replacing the vanilla texture.
13. Frieza's ranged attack remains an instant hit under D4. Its
    particles and damage type are custom; no projectile entity was
    added.
14. ROBOT_AMBIENT exists in the custom bank but has no call site
    because the robot never had an ambient sound. It is unused
    custom content, not a vanilla remainder.
15. Minecraft remains the platform for registries, networking,
    entity physics, rendering submission, fonts, block behavior,
    and world saving. Those systems are infrastructure, not
    converted presentation assets.

## Final state and remaining judgment

The integrated batch preserves the component reports beside this
audit:

- audits/bespoke-scope2-audit.md
- audits/scope2-foundation-report.md
- audits/scope2-report-ki-techniques.md
- audits/scope2-report-wish-ritual.md
- audits/scope2-report-mobs.md
- audits/scope2-report-progression.md
- audits/scope2-report-utility-items.md
- audits/scope2-report-dimension-ambience.md
- audits/scope2-report-timeshatter.md
- audits/lava-redesign-report.md
- audits/evil-spear-dread-report.md

The remaining judgment is Carter's PC test. Synthesized mob voices,
the movement impact sound picks, the Otherworld cloud density, the
Lava Sword's new ground reads, the TimeShatter bloom, and the Evil
Dread center darkness can only be judged in game. No conversion in
this batch was reverted during integration.
