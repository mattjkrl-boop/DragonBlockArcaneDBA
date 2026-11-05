# DBA Rebuild Final Audit (Phase 6 close)

Date: Wed Nov 4, 2026 (late evening CT). Scope: the weapon rebuild
that began with the strip is complete. All 22 weapons and all 44
attacks are authored as data on the Phase 1 to 5 system plus the
Phase 6 foundation. This audit is the close-out record: what was
built, how every attack rates against its approved reference
picture, what could not be expressed, and the standing honest
lines. Companion docs: rebuild-phase6-plan.md (the roster and
schema contract), REBUILD-ARCHITECTURE.md (the living system
contract, sections 10 and 11 cover Phase 6).

## Verification (all gates run at the final tree)

- Roster harness (the real AbilityDef parser plus the real
  AbilityRegistry, standalone): 54 files, 54 parsed, 44 bound,
  10 unbound, 22 weapons each holding exactly a left and a right
  binding, zero duplicate bindings, zero unresolved references
  (summons, sneak and air variants, critical-window summons all
  resolve inside the set). Failures: 0.
- Effect references: every effect id used (melting,
  petrification_curse, pole_stun, ox_brace) is registered in
  DbaEffects.java. Sound references: 45 distinct ids used, all
  present in sounds.json.
- Sweeps (javac audit, 26.2 and 26.3 classpaths): 124 and 130
  error kinds against rebuild baselines of 120 and 126. The
  literal comm -13 prints ONLY the 4 FogRendererMixin lines,
  the Phase 5 precedent: that file predates Phase 6 (the
  baselines were measured at the strip base, before Phase 5
  added it), it is untouched by Phase 6, and its 4 messages are
  the missing-Mixin-package messages every pre-existing mixin
  file contributes because the sweep classpath has no Mixin
  jars. The message-level comparison (file prefixes stripped)
  is EMPTY on both classpaths: Phase 6 introduces zero new
  error messages. No new mixin was added by Phase 6.
- Schema harness from Foundation A: 26 checks passed, 0 failed
  (both proofs parse unchanged; 12 valid synthetics covering
  every new block parse; 12 malformed defs rejected naming the
  expected field).
- SpikeCluster kit harness: 44 vertices per spike, exact at
  counts 0, 1, 6, 24 across all three layouts, hard cap 24,
  deterministic per seed.
- All resource JSONs parse (571 files). git diff --check
  clean. Em dash scan zero on every changed or new file (the
  two legacy hits in DbaHudOverlay.java stay untouched, per
  the standing rule that past text is not gone back and
  changed).
- Particle worst case: the plausible worst scene (Eclipse,
  Call of the Tempest, Cursed Tempest, Valor Field, Bansho
  Cyclone, Grand Cyclone at full charge shed, and a Z
  Shockwave in flight at once) spawns about 1376 particles per
  second of continuous recipes in total. At the particle
  lifetimes in use (roughly 0.5 to 2 seconds) that is roughly
  700 to 2700 alive, under the roughly 4000 shared cap from
  the visual research, and the Phase 3 budget system enforces
  each def's own per-second cap (240 to 450) regardless. This
  is an estimate from the data, not a measured in-game count.

## What Phase 6 built (foundation, all generic, no per-weapon Java)

Schema (AbilityDef, validated by name like every earlier block):
Carrier gains BEAM, SWARM, NONE. New blocks: move (dash, chain,
step, warp, launch, slam, rush), melee, onHit, swing, summons,
aura, sustain, autoFire, combo, criticalWindow, volley,
terrain, beam params, swarm params; cloud gains follow, anchor
entity, pull, vortex, stasis, insideEffect,
insideDamageInterval, chains, freezeTicks, rewindTicks,
onSpawnBurst, finale; wave gains knockback, deflect,
selfLaunch; geometry gains the ground block (wall, cracks,
spikes). Top level gains bind, sneakVariant, airVariant,
rooted, charges, chargeRegenTicks. Cross-reference resolution
is validated after load with loud by-name logs (AbilityLoader).

Runtime (AbilityService): one per-player runtime holding move
state, channel state (sustain and autoFire with heat and
overcharge), combo counting, charge pools, swarm watcher,
delayed volley spawns. Fire order, summons executor (fire,
fullPower, critical release, meleeHit, carrierHit; no
recursion), melee cone resolution, aura ticking, rooted holds.
AbilityDamage routes every carrier and melee hit through the
one path, applying onHit effects, lifesteal, and combo reports.
AbilityTerrain drives PermanentDestruction for crater, disk,
trail, field, and crust events under the hard caps.
RewindService keeps a global position history (living entities
within 48 blocks of a player, sampled every 4 ticks, 120 ticks
kept). AbilitySwingHook is a second additive
AttackEntityCallback for passive swing effects.

New carriers: BeamCarrierEntity (rides the owner's eye and
look, segment damage on the data interval, end burst) and the
Swarm pair (SwarmHostEntity with the shared budget, Ki drain,
target assignment, intercept bookkeeping, and its EndReason;
SwarmShardEntity with orbit, seek, intercept, return states).

Client: SpikeCluster kit builder (seeded tapered prisms, ring,
scatter, fan layouts, world-lit); ground draws on the cloud
renderer (TornWall wall, CrackDecal cracks, SpikeCluster
spikes, all anchored at the cloud's current radius) and on the
wave renderer (wall across the travel path, cracks at the
anchor); ChainRun links drawn by the cloud renderer when a def
has chains; BeamCarrierRenderer (energy BeamDrawer form, solid
lit cylinder plus tip disc form); SwarmHostRenderer (quiet
planted seal: ground ring, cracks, orbit ring, low dome);
SwarmShardRenderer (lit shard along its real motion with an
inset energy core); data-driven tooltips (ItemTooltipCallback:
Left Click and Right Click lines from the synced registry, so
datapack renames flow through).

One behavior added during the close-out pass: a charged dash
now scales its travel distance by cast power between the
minPower fraction and full distance (Heroic Dash travels 8 to
24 blocks as designed; the fixed-distance encoding the data
author had to use is superseded).

## Roster (44 bound attacks)

Ratings follow Carter's relaxed picture rule: general following
of the approved concept (plus the cool-bar card where one
exists), best effort, gaps named. STRONG means the composition,
scale, and choreography read as the picture. PARTIAL-STRONG
means the hero element lands with one named approximation.
PARTIAL means the mechanic is fully real but the picture's
signature visual is approximated by recipes or palette.

| # | Weapon | Attack | Carrier and modules | Rating | Gap |
|---|--------|--------|--------------------|--------|-----|
| 1 | Katana | Flashdraw | none; move chain 5, melee | PARTIAL-STRONG | afterimages are fx, no model |
| 2 | Katana | Heaven Splitter | wave slow; ground wall and cracks | STRONG | wall now lingers 100 ticks where the wave dies (carrierEnd summon, katana_heaven_wall); unverified in game |
| 3 | Z Sword | Z Shockwave | wave; raised charge ring (proof) | STRONG | none open |
| 4 | Z Sword | Katchin Weight | cloud anchor target; pull, finale slam, crater, cracks | PARTIAL-STRONG | the concept's sword rain is picture dressing, not the design |
| 5 | Saber | Blitz Flurry | move chain into wave | PARTIAL | the picture's line-up finisher reads thin |
| 6 | Saber | Flash Step | move step, charges 3; echoed cloud | PARTIAL | portals are low domes, not tall rift shapes (movement tool by design) |
| 7 | Brave Sword | Assault | melee; combo 10 into ring wave | PARTIAL | the cross slash shape is renderer-side; ring and palette carry it |
| 8 | Brave Sword | Heroic Dash | move dash (power-scaled 8 to 24); ring wave on arrival | PARTIAL-STRONG | trail is the ring's shed recipes, no dash-trail carrier |
| 9 | Grand Sword | Grand Cyclone | aura spin (pull, deflect claim, chip damage); wave on release | STRONG | spin shards are recipes, not projectile entities (plan limit) |
| 10 | Grand Sword | Valor Field | cloud follow caster; stasis freeze | PARTIAL-STRONG | hex-cell dome pattern has no kit equivalent |
| 11 | Azure Dragon Sword | Dragon Rush | move rush; ring wave on landing | PARTIAL-STRONG | no dragon-head form in the kit; trail and ring carry it |
| 12 | Azure Dragon Sword | Call of the Tempest | cloud; strikes, vortex | STRONG | debris is recipe particles, not flying blocks |
| 13 | Dabura Sword | Abyssal Slash | chargeRelease wave; onHit petrification | PARTIAL-STRONG | picture's second cross arc not fired |
| 14 | Dabura Sword | Eclipse Domain | cloud follow caster; pull, stasis slow, petrify inside, finale strikes, obsidian finale | PARTIAL-STRONG | finale sky blades read as strikes and bursts, no blade model |
| 15 | Cursed Blade | Curse Chain | cloud anchor entity; follow entity, chains 10, inside damage | PARTIAL | drawn chain set is a client approximation of the server latch |
| 16 | Cursed Blade | Cursed Tempest | cloud; strikes, 2-layer shell (proof) | PARTIAL-STRONG | held from Phase 4 |
| 17 | Hollow's Edge | Hollow Rush | none; move chain 4 | PARTIAL | afterimages are fx only |
| 18 | Hollow's Edge | Void Rift | cloud anchor target; pull 2.2, vortex, finale implosion | PARTIAL | spike ring read via shard recipes; ground spikes do not apply to an anchored rift |
| 19 | Dimensional Sword | Dimensional Slash | projectile; swing melting | PARTIAL | per-shot tilt alternation is not a data parameter |
| 20 | Dimensional Sword | Warp Rift | move warp 1024 (sky fallback 128 in code); destination cloud, onSpawnBurst | PARTIAL | rift ring visual is disc and ring recipes |
| 21 | Spirit Sword | Spirit Swing | projectile plus melee; meleeHit summon | PARTIAL | the picture's second half is the summon below |
| 22 | Spirit Sword | Spirit Cannon | beam; sustain 5 percent per second | STRONG | braided multi-stream beam texture is renderer-side |
| 23 | Evil Spear | Evil Impale | chargeRelease projectile; fullPower summon | PARTIAL-STRONG | thrown spear is an energy-cased shard, not a spear model |
| 24 | Evil Spear | Hell Hunt | cloud anchor entity; chains 3, finale 2.2 | PARTIAL | chain links drawn to a client-approximated set |
| 25 | Devil Trident | Deploy | swarm carrier, all design numbers verbatim | STRONG | shard look depends on the swarm renderer (lit shard plus energy core) |
| 26 | Devil Trident | Prong Shot | projectile; 750 flat, Ki 60 drained over 10 ticks, cooldown 10 (design) | PARTIAL | a plain fast projectile; the golden streak reads via palette |
| 27 | Sickle of Sorrow | Sorrow Slash | melee; onHit melting plus 15 percent lifesteal | PARTIAL | slash trail read via fx; design numbers verbatim (750 plus 3.0 Strength) |
| 28 | Sickle of Sorrow | Dimensional Rift | cloud; vortex, inside damage, finale implosion | PARTIAL-STRONG | crystal shards are recipes, no crystal geometry |
| 29 | Ox King's Ax | Ox King's Strike | rooted chargeRelease 200; ring wave radius 24; fullPower summon | STRONG | none material |
| 30 | Ox King's Ax | Ox Brace / King's Slam | aura repulsion stance; critical window 280 to 300 at 1.5 power plus crag summon; ring wave on release | PARTIAL | titan scale is data; the visual is aura plus wave plus crags |
| 31 | Power Pole | Wind Spin | wave radius 25; deflect 2.2 no claim, knockback | PARTIAL | reach is travel plus radius, an approximation (named in the plan) |
| 32 | Power Pole | Extension | beam solid form; onHit pole_stun | PARTIAL-STRONG | one-application jab reads via the lit cylinder and tip disc |
| 33 | Whis Staff | Time Shatter (melee) | melee 350 plus 2.0 Dexterity (design); meleeHit summon | PARTIAL | the burst below carries the picture |
| 34 | Whis Staff | Temporal Rift | cloud; freeze 60, rewind 100, damage 0 | PARTIAL-STRONG | drawn glass ring has no kit equivalent; freeze and rewind are real |
| 35 | Bansho Fan | Gale Force | wave; knockback, carrierHit cyclone summon | PARTIAL-STRONG | dust trails ride the cyclone def's palette |
| 36 | Bansho Fan | Tempest Barrage | projectile volley 5; fire shockwave summon | PARTIAL | bolts use the tempest palette, the picture's dust grey was traded for coherence |
| 37 | Blaster Gun | Barrage | autoFire channel; heat to overcharged shot | PARTIAL | energy bolts, not the picture's shell casings (plan mandates energy casing) |
| 38 | Blaster Gun | Erasure Cannon | chargeRelease beam 48 | STRONG | none material |
| 39 | Lava Sword | Lava Cleave | wave; terrain trail lava cap 120; sneak variant volcano | PARTIAL | lava fountains behind the caster have no recipe equivalent |
| 40 | Lava Sword | Lava Eruption | cloud; terrain field lava cap 260, basalt crust; sneak variant meteor | PARTIAL-STRONG | magma channel lines are the field conversion, not drawn channels |
| 41 | Spike Flail | Wide Swing | melee arc 150; rooted 6-tick startup | PARTIAL | the chain itself is sound and charge geometry, not drawn links |
| 42 | Spike Flail | Spin Disc | chargeRelease 32; ring wave radius 9 | PARTIAL-STRONG | ground spikes in the picture's disc edge belong to the swing scene |
| 43 | War Mace | Crater Smash | ring wave; selfLaunch, terrain crater; air variant falling smash | STRONG | picture crater is wider; scale is design-bound |
| 44 | War Mace | Rising Blow | melee plus move launch | PARTIAL | forward push now data (move.forward 0.6, tuning value, not a design number); launch velocity now synced to client |

Unbound defs (10, all bind false, summoned or variant-only):
spirit_impale (PARTIAL: the six celestial swords have no kit
model; dome, disc seals, and the strike-in ring approximate
them), evil_spikes (PARTIAL: the design's body-space burst
reads as a tight ground spike ring), time_shatter (PARTIAL:
freeze is real, glass is shard fx), ox_fissures and ox_crags
(PARTIAL-STRONG: ground cracks and spikes plus real magma
fields), bansho_cyclone (PARTIAL-STRONG: dust tornado, tuned
height 10 against a taller picture), bansho_shockwave
(STRONG for its one job), volcano (PARTIAL: fountain jets are
recipes), meteor (PARTIAL-STRONG: lit plate casing and crater
land; no glowing crack texture on the casing), falling_smash
(STRONG: slam, bigger crater, relaunch all real).

No attack rates WEAK: every design's mechanic is implemented
and functional; every gap above is presentation fidelity, not
missing behavior.

## Designs not fully expressed, and what shipped instead

- Temporal Rift (follow-up): rewind now restores position,
  rotation, health, fire, air, and status effects (stasis
  excluded). Inventory is still not rewound, deliberately
  (duplication risk). Freeze now also pauses mob AI via NoAI
  for the freeze window, restored at window end or cloud
  removal; mobs already NoAI are untouched. Players are only
  velocity-zeroed plus stasis. Known edge: a chunk unloading
  mid-freeze can save a mob with NoAI set. Unverified in game.
- Spirit Impale's six swords, Eclipse's sky blades, Katchin's
  sword rain: no sword or blade model exists in the kit.
  Shipped as dome and disc volumes, strike scheduling, and
  burst recipes.
- Prong Shot Ki pricing: CLOSED Nov 5, 2026 (Grok). Restored to
  the design: generic kiDrainTicks field, the full 60 must be on
  hand at fire and leaves as 6 per tick over 10 ticks.
- Blaster Barrage overcharge: the design's overcharged AoE of
  2.2 is reached as base aoeRadius 1.1 times overchargeAoeScale
  2, because the channel multiplies the base radius. Normal
  bolts therefore carry a small 1.1 splash the plan's "aoe 0"
  line did not intend; the design number won. Named here as a
  deliberate deviation.
- Rising Blow's launch: distance 18 yields the design's 1.6
  upward velocity. Follow-up: move.forward (blocks per tick
  along the horizontal aim) now carries the forward push, set
  to 0.6 as tuning (the design record gave no number). The
  launch was a bare server setDeltaMovement that never synced
  to the client; it now goes through MovementPhysics. Unverified
  in game.
- Heaven Splitter (follow-up): new summon trigger carrierEnd;
  when the wave expires naturally it leaves katana_heaven_wall,
  an unbound zero-damage cloud carrying the same ground wall
  and cracks, facing the wave's yaw, for 100 ticks (tuning, no
  design number), sinking with the cloud envelope and shedding
  chips at its end. Still a drawn wall: no collision. Cloud
  walls now face the carrier yaw instead of a seeded angle
  (only this def uses a cloud wall). Ox dressing unchanged.
- Wind Spin reach is wave travel plus radius (about 21 blocks
  of travel plus the 25 radius), the plan's named
  approximation of the design's reach line.
- Swarm assignment approximates "up to 3 shards per enemy"
  with the global seeking count (marked target exempt), and
  chain hop damage is gated on the chain def's damageOnPass
  flag. Both from Foundation A's report, accepted.
- FX density: authoring produced two density families; the
  close-out pass scaled the sparse files' continuous recipe
  rates up to the proof family's density, per file, under each
  def's own cap. All such numbers remain data tuning.

## Deferred items

- Hit-stop remains deferred on the Phase 5 terms: the carrier
  presentation clock cannot be slowed per viewer without
  desyncing visuals from server damage, and no roster
  attack's picture was judged to demand the pose-only
  compromise (a caster-only freeze of the follow-through
  clock). The designed schema slot (hitStopTicks, cap 4, in
  the feedback block) is recorded in the Phase 5 report.
- In-game look of everything in this phase is unverified
  until Carter's PC test, same as every prior phase. The PC
  test watch list from the attack upgrade pass still stands,
  and this phase adds: beam and swarm first looks, the dash
  distance feel, Eclipse and Temporal Rift presence, and
  whether the scaled fx densities read right at the caps.

## Process notes (for the record)

- The authoring fan-out initially carried a stale roster
  mapping for one weapon cluster. The plan's numbered roster
  is the authority; the affected files were reconciled
  centrally: Heaven Splitter to Katana R, Katchin Weight to
  Z Sword R, Abyssal Slash to Dabura L (type darkness),
  Eclipse Domain to Dabura R, Hollow Rush re-slotted to
  left, Curse Chain and Void Rift authored centrally, and
  two invented files (a katana rush-ring and a Dabura rim
  spikes field with no plan entry) deleted. The roster
  harness above proves the final binding state.
- Commits: foundation A (82dcacf), foundation B (09ac61d),
  then this close-out (dash power scaling, the 52 new ability
  files, this audit). Push follows the gates above.

## Standing honest lines

- No Gradle build ran in this VM (the sandbox loopback
  restriction stands); verification is the javac audit sweep
  plus the standalone harnesses, exactly as every prior phase.
  The compile check remains waived by Carter, never passed.
- The in-game look is unverified until Carter tests on his
  PC. Nothing in this audit claims more than code, data, and
  harnesses can prove.
- Damage, cost, and cooldown numbers are data. Where the
  design record gave numbers they are verbatim (the Trident
  set, Sickle and Whis melee, Prong Shot, the Ox critical
  window); everything else is tuning inside the mod's existing
  scale and can be rebalanced by editing one JSON per attack.
