# Lava Sword Full Redesign Report

Scope: complete redesign of the Lava Sword's ground system, all six of
Carter's ideas built as one system, with new blocks and new entities.
Baseline HEAD at audit time: dc2cd1f. Nothing in this pass is committed;
the tree also carries other agents' work (damage tags already landed as
8058771; the Dread and Evil Spear files are not part of this pass and
were not touched).

## The design: one material, one engine, one play pattern

The old system left two different things on the ground: temporary fire
cells and short-lived no-flow lava sources, both run by
LavaSwordGround. The redesign gives the sword its own material and one
engine that runs everything:

- The material is magma crust: one custom block with three cooling
  stages (molten, cooling, dark). It is what pools, channels, waves,
  splats, and craters are all made of, so every mark the sword leaves
  reads as the same substance cooling at different speeds.
- The engine is entity/LavaField.java: placement validation, stage
  schedules, caps, and crash-safe SavedData persistence for every cell
  and every volcano site, in one place. Every feature below is a
  different way of asking LavaField to place that material.
- The legacy LavaSwordGround system still exists for fire cells (the
  eruption ring and the meteor scorch ring), with its own persistence,
  unchanged except for one floor test fix noted below.

The play pattern that falls out of the six ideas: the cleave is now a
wave that raises crust as it travels and carves a channel that runs
downhill when it lands (ideas 4 and 1). The eruption is a zoning burst:
pools along its lip, geysers under whatever it hits (ideas 3 and 2).
Geysers and the volcano rain blobs, and every blob splats into a pool,
so heavy plays keep feeding the same crust field. The meteor finisher
(idea 5) is the ranged version of the same pattern: craters, pools, and
a scorch ring from the sky. The volcano (idea 6) is the stationary
version: plant it and it works an area for 30 seconds. One substance,
three ranges: carried (cleave), burst (eruption, meteors), planted
(volcano).

Crust profiles (ticks to cooling stage, to dark stage, to restore):
POOL 60 / 120 / 200, CHANNEL 40 / 80 / 140, WAVE 14 / 30 / 54. Stage 0
(molten) deals the cell's full touch damage and 50 fire ticks, stage 1
(cooling) deals 40 percent and 20 fire ticks, stage 2 (dark) deals
nothing and is a walkable slab 6/16 high. Stages 0 and 1 have no
collision: you wade through hot crust and it burns you.

## New blocks and entities, with registration points

Blocks (registered block-only, no BlockItems, in block/DbaBlocks.java
next to the existing block-only registrations; ability constructs, not
obtainable items):

- MagmaCrustBlock (block/MagmaCrustBlock.java): IntegerProperty STAGE
  0 to 2, light level 15 / 9 / 2 by stage, SoundType.BASALT. Contact
  routes to LavaField.onCrustTouch on the server. Blockstate
  blockstates/magma_crust.json maps stage to three models
  (models/block/magma_crust_stage0/1/2.json), slab elements 0..16 by
  0..6, top and side textures per stage.
- MiniVolcanoBlock (block/MiniVolcanoBlock.java): BaseEntityBlock,
  bedrock-grade strength so it cannot be broken while standing,
  light level 13. Blockstate blockstates/mini_volcano.json, model
  models/block/mini_volcano.json (four stacked rock boxes narrowing to
  a crater whose top face uses the molten crust texture).

Block entity (block/entity/DbaBlockEntities.java, same registration
pattern as the gravity training block entity):

- VolcanoBlockEntity (block/entity/VolcanoBlockEntity.java): drives
  one planted volcano. Persisted owner, expiry, next eruption, and
  blob damage through the 26.2 ValueOutput / ValueInput API. Erupts
  every 36 to 54 ticks: lobs 3 to 5 LavaBlobEntity arcs to a ring
  3.5 to 7.5 blocks out, applies a heat pulse (50 percent of blob
  damage, 40 fire ticks, push) around the cone, and has a 1 in 3
  chance per eruption to crust one pool cell near its base. At expiry
  it hands the site back to LavaField.endVolcano, which removes the
  block, plays the crumble, and clears the field entry.

Entities (entity/DbaEntities.java, after the existing lava
registrations; renderers in DragonBlockArcaneDBAClient in the same
block of registrations):

- LavaGeyserEntity (entity/LavaGeyserEntity.java, sized 0.8 x 0.8,
  tracking 48; renderer LavaGeyserRenderer): stationary pillar, life
  18 ticks. Column hit once at tick 2 (radius 1.3, height 4.5):
  geyser damage, 60 fire ticks, launched 0.95 straight up. At tick 3
  it rains 3 blobs at 35 percent of its damage, angles and distances
  derived from the synced seed. Renderer: two BespokeGeometry plumes,
  a brokenVortexBand wrap, crackStar and glowFan at the base.
- LavaBlobEntity (entity/LavaBlobEntity.java, sized 0.5 x 0.5,
  tracking 48; renderer LavaBlobRenderer): ballistic projectile under
  manual gravity 0.06 (DebrisEntity pattern), with a static launch
  helper that solves the discrete arc so a lob lands on its target
  after a chosen flight time. Direct hit: full blob damage and 60
  fire ticks. Landing or strike: splash at 60 percent in radius 1.3,
  40 fire ticks, ground burst, and one POOL crust cell with touch
  damage at 50 percent of blob damage. Renderer: two displaced domes
  back to back tumbling off age and seed, plus a small plume trail.
- LavaMeteorEntity (entity/LavaMeteorEntity.java, sized 1.2 x 1.2,
  tracking 64; renderer LavaMeteorRenderer): falls at -1.9 per tick
  with a small seeded drift. Impact: full meteor damage in radius
  3.2, 80 fire ticks, outward push, BOOM_BIG and GROUND_CRACK, ground
  burst, 10 debris pieces, a fire-only scorch ring (LavaSwordGround
  scorchDisk, budget 0, radius 3.0), and up to 5 POOL crust cells
  (center first, then a seeded ring) with touch damage at 12 percent
  of impact damage. Renderer: displaced dome rock with a spinning
  brokenVortexBand and a two-layer plume trail.

Textures: tools/gen_lava_textures.py (deterministic seeds, numpy and
PIL, following tools/gen_bespoke_textures.py) generates seven 32x32
RGBA textures into textures/block/: molten, cooling, and dark crust
tops and sides with glowing crack work, plus mini_volcano_rock. No
new sound files: the existing DbaSounds bank covers every call
(LAVA_BUBBLE, FIRE_SHOT, FIRE_IGNITE, RELEASE_BLAST, GROUND_CRACK,
BOOM_BIG, EXTINGUISH, CHARGE_HUM, CHARGE_FULL, FIZZLE, SLASH_WHOOSH).

Lang (lang/en_us.json): block.dragonblockarcanedba.magma_crust,
block.dragonblockarcanedba.mini_volcano,
entity.dragonblockarcanedba.lava_geyser,
entity.dragonblockarcanedba.lava_blob,
entity.dragonblockarcanedba.lava_meteor.

## Every trigger, cost, and cooldown

Unchanged moves keep their exact old numbers:

- Melee hit: 860 attribute damage (unchanged), 60 tick burn and the
  FLAME particle spit on the target (unchanged).
- Cleave (left click): Ki drainKiDiscrete(90, 14), item cooldown 8,
  damage 740 + Strength x 2.0 (all unchanged). New ground work: a
  magma wave under the flight path, 3 cells wide, budget 44 WAVE
  cells per cleave, touch damage 15 percent of cleave damage; on
  flight end (block hit or expiry) the channel is carved back along
  the centerline and flows downhill up to 6 more cells at 2 ticks
  apart, CHANNEL profile, same touch damage; on a block landing a
  mini geyser erupts at 45 percent of cleave damage.
- Eruption (right click): Ki drainKiDiscrete(90, 40), item cooldown
  40, damage 880 + Strength x 2.2, cone and reach unchanged. New:
  the ground ring is fire only now (scorchArc budget 0, was 8 lava
  sources); up to 8 POOL crust cells along the lip, touch damage 12
  percent of eruption damage; up to 4 geysers at 36 percent of
  eruption damage (at most 2 under hit targets, plus 2 at the lip
  corners at tick 6).

New moves:

- Meteor finisher: sneak and hold right click. Charge feedback from
  onUseTick (CHARGE_HUM climbing every 20 ticks, CHARGE_FULL at 50,
  FLAME particles at the hand). Release at 50 or more held ticks:
  Ki drainKiDiscrete(90, 80), 7 meteors on the looked-at point out
  to 64 blocks (spawn heights 26 plus 4 per meteor, ring offsets up
  to 3.5 blocks), each dealing 520 + Strength x 1.6 on impact.
  Readiness 400 ticks (20 seconds), kept in a timestamp map, not the
  item cooldown, so it never blocks cleave or eruption. Release
  under 50 ticks, or with the Ki drain failing: fizzle (FIZZLE sound,
  ember puff, actionbar note), no cost, no readiness.
- Volcano ultimate: sneak and left click (a branch inside
  performCleave after its cooldown and same-tick guards, so the
  packet path is the one cleaves already use). Target: the ground
  block under the crosshair within 7 blocks. Ki
  drainKiDiscrete(90, 200), the heaviest drain the sword has.
  Readiness 1200 ticks (60 seconds), timestamp map, separate from
  the item cooldown. One active volcano per player. Life 600 ticks
  (30 seconds). Blob damage 100 + Strength x 0.6 per blob, heat
  pulse at 50 percent of that. Failures (not ready, no ground in
  reach, protected or occupied site, one already standing) report
  on the actionbar and cost nothing.

All ability damage in the redesign goes through DbaDamage.lava,
capped by DbaEffectsConfig.capDamage exactly where the existing
weapon code caps it.

## Safety implementation (the contract, carried over and extended)

- Temporary and restores: every crust cell, channel cell, and volcano
  site is a LavaField entry with a restore tick and the cell's prior
  state (always air for crust, by the placement rule). Stages advance
  on the level tick; at the restore tick the prior state goes back.
- Crash-safe persistence: every entry is mirrored into the level's
  SavedData (id "lava_field", codec based, same style as
  LavaSwordGround's data) at the moment it is created, updated in
  place when a wave cell upgrades to a channel cell, and removed when
  the entry resolves. On the first tick of a server run, persisted
  entries are adopted: overdue ones restore at once, live ones resume
  their schedules, entries whose cell no longer holds field material
  are dropped, and channel fronts that had not arrived still arrive.
  SERVER_STOPPING restores everything; SERVER_STOPPED clears the
  adopted set. The volcano block entity also persists its own state,
  and the field entry is the backstop if the entity is lost: the
  site still ends and restores.
- Protected blocks refused: placement only ever fills an air cell
  above sturdy ground, and the ground under a cell must pass both
  refusal lists: GroundScar.isProtected (any block entity, bedrock,
  barrier, spawners, portals, command blocks, structure blocks,
  jigsaw) and the sword's own list, now exposed as
  LavaSwordGround.isProtectedGround (bedrock, spawner, trial
  spawner, chest, trapped chest, ender chest, barrel). Volcano
  planting runs the same canOccupy test. Nothing in the redesign
  breaks or replaces a non-air block, ever.
- Hard caps and budgets: 800 field cells per level and 240 per owner
  across all features; per-move budgets on top (wave 44 per cleave,
  pools 8 per eruption, pools 5 per meteor, geysers 4 per eruption,
  blobs 3 per geyser and 3 to 5 per volcano eruption, meteors 7 per
  cast, volcanoes 1 per player). Cooldowns and Ki costs above keep
  the cast rate itself bounded.
- Pools burn everyone, the planter included, exactly as the real
  lava sources they replace did. That is deliberate zoning, stated
  here so it is a decision on record, not a surprise.

## One fix inside the legacy system

LavaSwordGround.scorchColumn's floor test used blocksMotion(), which
was removed in 26.3 and was the file's one pre-existing 26.3 baseline
error kind. The test is now isFaceSturdy only, the same form
LavaField.findSurface and the SpikeFlailSpinEntity precedent use.
Behavior on 26.2 is unchanged (the sturdy-face test was already the
stricter half of the pair). The 26.3 error kind is gone (verified
below).

## Before and after counts

- Ground systems: 1 (LavaSwordGround) before; 2 after (plus
  LavaField, 774 lines).
- Custom blocks owned by the sword: 0 before; 2 after (magma crust,
  mini volcano), plus 1 block entity type.
- Ability entity types owned by the sword: 2 before (cleave,
  eruption); 5 after (plus geyser, blob, meteor), with 3 new
  renderers (5 total).
- Ground material placed: real lava sources (budgets 4 per cleave,
  8 per eruption, 80 tick life) and fire cells before; fire cells
  only from the legacy system after, plus crust in 3 profiles with
  lives of 54, 140, and 200 ticks.
- Textures for the sword's ground system: 0 before; 7 after.
  Resource files: 2 blockstates and 4 models added; 5 lang entries
  added.
- Moves: 3 before (melee, cleave, eruption); 5 after (plus meteor
  finisher and volcano ultimate).
- New sound files: 0. New damage types: 0 (DbaDamage.lava
  throughout).

## Verification

Javac audit only, per the standing rule for this repo (no Gradle
claim; Gradle cannot run in this VM). Full-tree sweeps with
~/workspace/dba-audit-libs/sweep.sh on both audit classpaths:

- 26.2 (cp262.txt): 132 unique error kinds, equal to the pre-pass
  baseline of 132.
- 26.3 (cp263.txt): 139 unique error kinds, one fewer than the 140
  at dc2cd1f. The removed kind is the LavaSwordGround blocksMotion
  kind; grep of the 26.3 kinds file for blocksMotion returns 0.
- Error kinds in redesign files: zero on both classpaths. The grep
  for Lava, Magma, and Volcano across both kinds files is empty.
  (Two mulPose(Quaternionf) kinds in the new blob and meteor
  renderers appeared in the first sweep and were fixed to the
  Matrix4f rotation form the EruptionRenderer already uses for dual
  version support; the second sweep is the clean one reported here.)
- JSON: all 7 new or edited JSON files parse (2 blockstates, 4
  models, lang/en_us.json in full).
- Textures: all 7 generated files are valid 32x32 RGBA PNGs.
- Bespoke standard greps across the 14 owned Java files: vanilla
  ParticleTypes references 0, vanilla SoundEvents references 0.
  All geometry in the new renderers is BespokeGeometry builders off
  synced entity seeds; all particles are DbaParticles types; all
  sounds are DbaSounds entries.
- Em dash scan across all owned files and this report: clean.
- Split source sets: the new renderers live under src/client with
  the other weapon renderers; common code references no client-only
  classes (the sweeps compile both source sets together and report
  no new kinds).
- In-game look: unverified, by construction. Carter's PC build and
  test is the first real look at the wave, the geysers, the meteor
  volley, and the volcano.

## Honest worse check

What the old system did that this one does not, stated plainly:

- Real lava is gone from the sword's ground. The old cleave and
  eruption placed actual lava source blocks; the new pools are the
  mod's own crust block. Crust does not flow like lava, cannot be
  scooped with a bucket, does not set neighboring flammables alight
  the way a lava source does, and water meeting it makes no obsidian
  or cobblestone. If any of those interactions were load-bearing for
  a play pattern, they are lost.
- The eruption's ground ring is weaker as raw burn than before: it
  used to place up to 8 real lava sources; now it places fire cells
  plus up to 8 crust pools whose touch damage (12 percent of
  eruption damage) is far below standing in real lava. The pools
  zone for longer (200 ticks against 80), but the instant ground
  threat is lower.
- The cleave's on-top lava pile is replaced, not kept. The ground
  fix at 94708b6 made a landing cleave put its pile on top of the
  block it hit; the redesign's landing raises a geyser and crust
  instead, so that specific behavior no longer exists.
- The battlefield stays marked longer. Pool crust lingers up to 200
  ticks (60 of them harmless dark slab) where the old ground fully
  cleared at 80 ticks. That is the design (idea 3 wants the mark to
  stay while it cools), but it is visual clutter the old system did
  not leave, and stage 2 crust is a walkable half slab that changes
  footing briefly.
- Sneak plus left click no longer cleaves. A player who sneaks and
  swings now plants (or tries to plant) the volcano ultimate
  instead of throwing a cleave. This is the standard sneak-combo
  trade, but it is a real change to an existing input.
- The volcano is unbreakable while it stands, including by its own
  planter. A misplaced volcano costs up to 30 seconds of waiting
  (or leaving the area) instead of one pickaxe swing.
