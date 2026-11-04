# Fresh Audit: Heavy and Elemental Weapons

Scope: Spirit Sword, Evil Spear, Devil Trident, Ox King's Axe, Lava Sword.
Repo state: HEAD 94708b6. Method: clean slate. Every claim below was derived by reading the current code (item classes, every entity and renderer each weapon spawns, and the shared helpers they call). No prior audit files were read.

## Confirmation: all five weapons exist

Registry (item/DbaItems.java):

- SPIRIT_SWORD line 320: SpiritSwordItem. Keys at lines 69 to 70.
- EVIL_SPEAR line 485: EvilSpearItem. Keys at lines 228 to 229.
- DEVIL_TRIDENT line 452: DevilTridentItem. Keys at lines 195 to 196.
- OX_KINGS_AX line 464: OxKingsAxeItem. Keys at lines 207 to 208.
- LAVA_SWORD line 482: LavaSwordItem. Keys at lines 225 to 226.

Entity types (entity/DbaEntities.java): SPIRIT_CLEAVE 381, SPIRIT_IMPALE 571, SPIRIT_CANNON_BEAM 581, EVIL_SPEAR_PROJECTILE 351, EVIL_SPEAR_CHARGE 501, HELL_HUNT_IMPACT 511, TRIDENT_SHARD 37, TRIDENT_PRONG 371, DEVIL_SLAM_SHOCKWAVE 561, OX_SHOCKWAVE 281, OX_FISSURE 291, OX_CHARGE 781, OX_STANCE_AURA 791, KINGS_SLAM 801, LAVA_CLEAVE 917, LAVA_ERUPTION 927.

Pieces per weapon:

- Spirit Sword: item SpiritSwordItem.java (298 lines); entities SpiritCleaveEntity, SpiritImpaleEntity, SpiritCannonBeamEntity; renderers SpiritCleaveRenderer, SpiritImpaleRenderer, SpiritCannonBeamRenderer.
- Evil Spear: item EvilSpearItem.java (402 lines); entities EvilSpearChargeEntity, EvilSpearProjectileEntity, HellHuntImpactEntity; renderers EvilSpearChargeRenderer, EvilSpearProjectileRenderer, HellHuntImpactRenderer.
- Devil Trident: item DevilTridentItem.java (367 lines); entities TridentProngEntity, TridentShardEntity, DevilSlamShockwaveEntity; renderers TridentProngRenderer, TridentShardRenderer, DevilSlamShockwaveRenderer. Swarm bookkeeping lives in util/SwarmHelper.java. The swarm manager runs every server tick from DragonBlockArcaneDBA.java line 267.
- Ox King's Axe: item OxKingsAxeItem.java (430 lines); entities OxChargeEntity, OxShockwaveEntity, OxFissureEntity, OxStanceAuraEntity, KingsSlamEntity; renderers OxChargeRenderer, OxShockwaveRenderer, OxFissureRenderer, OxStanceAuraRenderer, KingsSlamRenderer; plus shared fx/SlamDebris.java.
- Lava Sword: item LavaSwordItem.java (133 lines); entities LavaCleaveEntity, LavaEruptionEntity; helper entity/LavaSwordGround.java (389 lines, not an entity: a static block placement and restore system); renderers LavaCleaveRenderer, LavaEruptionRenderer.

## Shared building blocks available in the code

- particle/DbaParticles.java lines 13 to 17: exactly five custom particle types exist: SPARK, EMBER, VOID_SMOKE, WIND, KI_MOTE. Everything in this weapon set uses only SPARK, EMBER, and KI_MOTE, plus vanilla EXPLOSION, EXPLOSION_EMITTER, POOF, and BLOCK particles.
- KiRenderHelper offers two render layers, kiRenderType (translucent emissive) and kiAdditiveType (additive eyes type). A mesh pass plus an additive halo pass is the established two-layer pattern in this codebase. Several renderers below use only one pass.
- fx/SlamDebris.java is the one shared debris system: a single burst call, used by OxFissureEntity, KingsSlamEntity, and ZGravitySlamEntity (a non-audited weapon). Count formula at line 31: clamp(8, 140, (30 + radius x 5) x effect density) spawn points, each emitting 2 BLOCK particles textured from the real ground block under the point, plus POOF dust at count / 3 and one accent type.
- No renderer in this set scales detail, alpha, or particle count by camera distance. The only scaling input anywhere is the user effects density setting. Point 1's "distance scaling" is absent from all five weapons.
- Every renderer in this set returns true from both shouldRender overloads, so none of these effects is ever frustum culled.

## Headline finding on point 4 (atmosphere)

None of the five weapons implements any part of point 4. No code in any item, entity, or renderer in this set touches time of day, weather, world or block light levels, fog or haze, or spawns a lightning entity. The only lightning-adjacent items are sound events: TRIDENT_THUNDER in Spirit Sword melee and hell hunt audio, WARDEN_SONIC_BOOM on the Ox flawless slam, LIGHTNING_ZAP on the Devil Trident laser. The single real light change in the whole set is incidental: the Lava Sword's real lava blocks emit vanilla lava light (level 15) for the 80 ticks they exist, which brightens the area, the opposite of the reference video's darkening.

---

## 1. Spirit Sword

Theme: cyan and gold spirit energy. The cannon beam is the visual flagship; the cleave and impale are smaller.

### Ability map

Melee (SpiritSwordItem.java):

- Attribute attack damage 499, plus the base 1, total 500 (line 53). Attack speed modifier -1.0, so 3.0 attacks per second.
- Passives while held: NAME_PLATE_DIST +40 and AIR_DRAG -0.55 as attribute holders.
- hurtEnemy on hit: SPIRIT_IMPALE for 120 ticks (line 83), CINEMATIC_TRACKING 120 ticks, BOUNCINESS attribute set to 0.85 on the target, bonus magic damage of 2 percent of the target's max HP, plus Strength x 2 through playerAttack. A 20 percent disarm roll (line 113) drops the target's main hand item, excluding players and excluding the BOSS class (knockback resistance at least 0.9 or max health at least 400). Spawns a SpiritImpaleEntity at scale 1.0, deduped per target (line 129).
- Cost: none. No Ki drain and no cooldown on melee.

Left click, Spirit Swing (performSpiritSwing, called from the swing payload at network/DbaNetwork.java line 537):

- Ki: drainKiDiscrete 55.0 per 7 ticks (SpiritSwordItem.java line 149).
- Spawns SpiritCleaveEntity: damage 500 + Strength x 2, velocity look x 1.7 blocks per tick, BLADE_SHING sound.
- SpiritCleaveEntity: MELEE_SKIP 3.0 (SpiritCleaveEntity.java line 27), so targets closer than 3.0 blocks to the owner are skipped (applied line 77). Server discards the entity when tickCount exceeds 8 (line 60), so at speed 1.7 the maximum travel is about 15.3 blocks. It clips against blocks every tick and discards instantly on a block hit. Hit box is the bounding box expanded along motion and inflated (0.7, 0.5, 0.7); each target is hit once. Damage is this.damage plus 2 percent of the target's max HP (line 85). Applies SPIRIT_IMPALE 120 ticks (line 86) and spawns a SpiritImpaleEntity at scale 0.85, deduped.
- The entity emits no particles at all.

Right click, Spirit Cannon (use, onUseTick, releaseUsing):

- use() requires hasKi and starts using the item; getUseDuration is 72000; use animation is BOW.
- onUseTick drains Ki per tick at the 55.0 per minute rate and stops using the item when a drain fails. Damage pulses every 10 ticks after tick 0.
- PULSE_RANGE is 32.0 (SpiritSwordItem.java line 41). Beam damage per pulse is (200 + Spirit x 1.5) x ramp (line 222), where ramp is 0.6 below 20 ticks used, 0.85 below 40, and 1.0 from 40 on.
- The pulse hits every LivingEntity whose bounding box inflated by 0.3 intersects the eye to end line (search box inflated 1.0). It pierces: every target on the line takes the pulse, plus 2 percent of max HP as magic damage, SPIRIT_IMPALE 100 ticks (line 242), BOUNCINESS 0.80, and a SpiritImpaleEntity at scale 0.75, deduped. SPARK particles, count 6, spawn at each hit target.
- releaseUsing discards the player's beam entities and adds a 20 tick item cooldown (line 294).

SpiritCannonBeamEntity (the visual): MAX_BEAM_RANGE 32.0 (SpiritCannonBeamEntity.java line 28). It follows the caster's eye and rotations, raycasts blocks each tick with ClipContext COLLIDER and sets the synced LENGTH to the block hit distance or 32 (lines 100 to 105). SPARK at the impact point every 2 ticks (count about 3 x density) and KI_MOTE at the muzzle every 6 ticks (count about 2 x density). It discards when the caster dies, stops holding the sword in either hand, or (after tick 3) stops using the item. This entity deals no damage; all cannon damage is in the item's onUseTick.

SpiritImpaleEntity: lifetime 28 ticks (SpiritImpaleEntity.java lines 21 to 22). It follows its target's position every tick, emits KI_MOTE every 2 ticks at count about 3 x density (line 99 area), and is discarded at the end of its lifetime with no closing burst.

Renderers:

- SpiritCleaveRenderer: one custom geometry pass on kiRenderType only. A 7 segment tapered blade drawn twice (colored shell and white core), a 4 segment wake, guard horns, and a 6 petal tip burst gated on a tip flash. Open is smoothstep(age / 3), fade is smoothstep(1 - age / 8), blade length 2.25 x (0.30 + 0.70 x open). No additive pass, no particles.
- SpiritImpaleRenderer: mesh pass plus one additive halo pass. Mesh: 3 counter-rotating ground rings (32, 24, 16 segments, radii to 2.4 x scale), 2 cylinder pillars (12 and 8 sides, height 6.0 x slam), 2 ascending helices of 14 steps, 6 plunging 3D spirit swords, 8 crystal spikes with per-entity seeded random. Additive pass redraws outer and middle rings and the pillar at alphas 0.20, 0.16, 0.14 x fade. Fade holds 1.0 until age 20, then (maxLifetime - age) / 8. Pulse 0.85 + 0.15 x sin(age x 0.4).
- SpiritCannonBeamRenderer: the most layered geometry in this weapon. 3 prism layers (inner 8 sides radius 0.13, mid 8 sides radius 0.26 counter-rotating, outer 6 sides radius 0.42 drawn as 12 length segments with a traveling brightness band), 4 helical ribbons of up to 72 segments in alternating cyan and gold counter-rotating pairs, a muzzle emitter (2 rings plus 4 curved needles), and a terminus impact (16 segment flared dome, white flash, 4 needles). Fade in is smoothstep over the first 5 ticks. There is no fade out.

### Gap analysis against the 5 points

1. Layered particle depth: FAIL for the cleave (single mesh pass, zero particles emitted by the entity, no additive layer). PARTIAL for the impale (mesh plus one additive halo pass is genuine 2 layer work, but there are no particle bands and no wispy outer layer). PARTIAL for the cannon (3 prism layers plus ribbons read as core, mid, and outer geometry, but it is all mesh: the only particles are SPARK and KI_MOTE trickles of about 3 and 2 per emission, and nothing scales with distance).
2. Debris: FAIL. No individual debris pieces exist anywhere in this weapon. The impale's 6 swords and 8 spikes are fixed seeded geometry that plunges once; nothing tumbles, orbits, or rains.
3. World interaction: FAIL. The cleave dies on the first block it touches with no burst, scorch, or mark. The cannon's damage ignores blocks entirely (see mismatch below). No mob is pulled, lifted, or flung by any ability: BOUNCINESS changes how targets bounce off surfaces afterward, but no ability applies velocity. No ground scarring of any kind.
4. Atmosphere: FAIL. Sound only (TRIDENT_THUNDER on melee). No darkening, haze, or lightning.
5. Lifecycle: FAIL for the cleave (discarded mid-flight at tick 8 or instantly on a wall, no dissipation phase; the renderer's fade is tied to the same 8 ticks, so the fade and the flight end together). PARTIAL for the impale (clean final 8 tick fade, but no closing burst and the entity simply stops). FAIL for the cannon (fades in over 5 ticks, then the full 32 block beam vanishes in one tick when the player releases, because releaseUsing discards the entity and the renderer has no fade out path).

### Numbers holding the spectacle back

- Cleave life: 8 ticks at 1.7 blocks per tick is about 15 blocks and 0.4 seconds of screen time. There is no temporal room for a trail, a debris phase, or a dissipation.
- Cannon damage pulses every 10 ticks while the visual beam is continuous, so hits land in half second steps under a beam that looks constant.
- Impale lifetime 28 ticks (1.4 seconds) with KI_MOTE only every 2 ticks at count about 3: the signature on-hit effect is gone before it can read as a storm element.

### Bugs, dead code, mismatches

- MISMATCH (damage through walls): the item's cannon pulse tests entity boxes along the full 32 block line with no block occlusion check, while SpiritCannonBeamEntity raycasts blocks and truncates the visual beam at the first block (SpiritCannonBeamEntity.java lines 100 to 105). The picture stops at a wall; the damage does not. Targets behind walls take full pulsing damage from a beam they cannot see reaching them.
- DEAD CODE: SpiritImpaleEntity has a third constructor (Level, owner, Vec3 pos, float scale) with no callers. All three spawn sites use the target constructor: SpiritSwordItem.java lines 129 and 257, SpiritCleaveEntity.java line 90 (verified by grep).
- DOC DRIFT: SpiritCannonBeamRenderer's class javadoc claims orbital Ki ring nodes and pulsing energy reticles along the beam; submit draws no rings along the beam length. The section comment numbering inside submit jumps straight to a comment numbered 5 for the terminus.
- The cleave's damage field is a plain server-side field, not synced. Harmless today because the renderer does not use it, but it means no client-side visual can ever scale with cleave damage without a code change.

---

## 2. Evil Spear

Theme: crimson and black demonic. A charge-up piercing spear throw, and a teleporting chain hunt.

### Ability map

Melee (EvilSpearItem.java):

- Attribute attack damage 749, plus base 1, total 750 (line 61). Attack speed modifier -1.5, so 2.5 attacks per second.
- Passives while held: NAME_PLATE_DIST -48 and AIR_DRAG -0.40.
- hurtEnemy: drainKiDiscrete 45.0 per 8 ticks, and MARKED_BY_EVIL for 120 ticks on the target.

Left click, Evil Impale (charge and release):

- MAX_LEFT_CHARGE_TICKS is 120, 6 seconds (line 46). onLeftClickChargeTick drains Ki per tick at the 45.0 per minute rate and auto-releases when a drain fails; syncStats every 5 ticks; MovementPhysics.dampenHorizontal 0.5 while charging; charge sound every 20 ticks with rising pitch.
- EvilSpearChargeEntity follows the caster and is discarded if charge updates lapse for more than 6 ticks (EvilSpearChargeEntity.java line 101). It emits EMBER every 2 ticks at count (2 + 4 x chargeRatio) x density (line 79 area).
- onLeftClickRelease: chargeRatio is floored at 0.1 (line 140). Damage is 450 + 700 x ratio + Strength x 3 x ratio (lines 142 to 143): 520 + 0.3 x Strength at minimum tap, 1150 + 3 x Strength at full charge, before the damage cap. Projectile speed is 1.8 + 1.8 x ratio blocks per tick (1.98 to 3.6). Cooldown is 20 + 30 x ratio ticks (line 158), 23 to 50 ticks.

EvilSpearProjectileEntity:

- Piercing: each target is hit once (hit id set). Movement is manual with noPhysics. It clips against blocks each tick; on a block hit it moves to the hit position, plays TRIDENT_HIT, and discards. Lifetime discard when tickCount exceeds 40 (line 191), so maximum range is about 79 blocks at minimum charge speed and about 144 at full charge. Hit box inflated by 1.2. EMBER trail every 2 ticks at count 2 x density.
- On hit: damage is multiplied by 1.3 if the target already has MARKED_BY_EVIL (lines 131 to 132); applies MARKED_BY_EVIL 160 ticks (line 140), FRICTION x 3.0 and AIR_DRAG x 2.0 as multiplied base modifiers, CINEMATIC_TRACKING 25 ticks. A vacuum pull drags the target toward the spear position (0.8 horizontal, +0.2 vertical) with markHurt. A shockwave pushes every entity in the target's box inflated by 3.5 (line 172) away at 1.2 horizontal and +0.2 vertical, with no damage. A HellHuntImpactEntity of radius 2.6 spawns at the target's feet on every hit (line 183). TRIDENT_HIT sound per hit (line 188).

Right click, Hell Hunt (performHellHunt and tickServer, ticked from DragonBlockArcaneDBA.java line 275):

- use() returns PASS while on cooldown. Cost: drainKiDiscrete 45.0 per 40 ticks. The hunt is blocked while the player is movement-immobilized, with an action bar message.
- Lock-on: findLockOnTarget at range 32.0 (called line 184, method line 204), cone dot above 0.5 (about a 60 degree half-angle), best dot score wins. There is no line of sight or block check.
- Chain: the initial target plus up to 7 more MARKED_BY_EVIL targets found within the current target's box inflated by 16.0, nearest first, 8 targets maximum. Steps run every 3 ticks through the static tickServer, the first step immediate. Cooldown 100 ticks is set at hunt start (line 269).
- executeHuntStep: damage multiplier 1 + 0.15 x stepIndex. Normal step damage is 350 + Strength x 2; the final step is 400 + chainCount x 150 + Strength x 3.5 (lines 357 to 358). Each step teleports the PLAYER to the target's exact position; the target gets MOVEMENT_CURSE 100 ticks, CINEMATIC_TRACKING 100 ticks, FRICTION x 4.0, BOUNCINESS 0.80, and a HellHuntImpactEntity of radius 3.0, or 4.5 on the final step. After the last step the player gets DEMON_SURGE for 40 ticks (line 299 area).
- No-target fallback lunge: velocity look x 2.0 horizontal and y 0.4, scaled by the movement multiplier; cooldown 40 ticks (line 196). It lands 8 ticks later via PENDING_LUNGES (line 197): 200 + Strength x 1 to everything in a box inflated by 3.0, MARKED_BY_EVIL 120 ticks, and a HellHuntImpactEntity of radius 2.6 only if at least one victim was hit.

HellHuntImpactEntity: lifetime 35 ticks (HellHuntImpactEntity.java line 20). Synced RADIUS and IS_FINAL. It emits EMBER only while tickCount is at most 14 and only every 2 ticks (lines 69 to 73), at count about 3 x density with spread radius x 0.5. It has no block interaction of any kind: no breaking, no scorch, no displacement. It discards silently at 35 ticks.

Renderers:

- EvilSpearChargeRenderer: one pass on kiRenderType, no additive pass. 3 ground rings at 40 segments counter-rotating, radii scaling with baseRadius 1.35 + 1.55 x charge; 4 gathering torus rings of 28 segments that contract and rise; 8 to 14 orbiting crystal shards, the count scaling with charge; 3 nested octahedra whose heart brightens with charge; a tightening filament ring of 36 segments; 2 helical ribbons of 12 steps. Alpha is 0.65 + 0.35 x charge. No spawn fade and no release fade.
- EvilSpearProjectileRenderer: one pass on kiRenderType, no additive pass. Axial spin of age x 30 degrees per tick; tapered shaft (12 and 8 sides); 2 nested diamond spearheads; 6 fins; a Mach cone of 28 segments with a 24 segment rim ring; 2 helical trail ribbons of 14 steps; one wake ring cycling on (age x 0.12) mod 1. First-person scale-in over the first 5 ticks. End fade is a smoothstep from age 32 to 40, matching the entity discard at tick 40.
- HellHuntImpactRenderer: one pass on kiRenderType, no additive pass. 3 counter-rotating ground rings (32, 24, 20 segments), an extra ring on the final impact; 8 radiating fissure trench ribbons of 5 steps (10 on final); 12 jagged spikes (18 on final) that rise over 8 ticks and sink after 55 percent of life; a cone execution pillar lasting 12 ticks (18 on final), height 5.5 (8.5 final). Whole-effect fade is a smoothstep of age over the 35 tick lifetime.

### Gap analysis against the 5 points

1. Layered particle depth: FAIL. All three renderers are single-pass mesh with no additive layer. The charge effect is rich geometry (rings, shards, octahedra, helices) but geometry is not particle layering: there is no dense core, mid band, and wispy edge particle structure, and the only real particles are the entity-side EMBER trickles (charge: up to 6 every 2 ticks; projectile: 2 every 2 ticks; impact: about 3 every 2 ticks for its first 14 ticks only). No distance scaling.
2. Debris: FAIL. Nothing in this weapon is debris. The charge renderer's 8 to 14 shards are fixed orbit geometry whose count scales with charge; they do not tumble on independent paths, are never released, and vanish with the charge entity.
3. World interaction: WEAK. The projectile's vacuum pull (0.8 inward, +0.2 up) and its 3.5 box push (1.2 out, +0.2 up) are the only mob physics, both small and instant with no lift arc and no fling. Hell Hunt moves the player, not the mobs. The impact effect draws fissure trenches and impalement spikes but changes zero blocks and leaves zero scar (HellHuntImpactEntity contains no block code at all). The projectile dies on walls with a sound and nothing else.
4. Atmosphere: FAIL. None.
5. Lifecycle: MIXED. The projectile is the best in this weapon: scale-in, a matched end fade over its last 8 ticks. The charge visual pops in at alpha at least 0.65 on frame one and is discarded the moment the throw releases, so a 6 second build-up ends in a one-frame cut. The impact builds well (rings grow over 10 ticks, spikes rise and sink) and fades over its full 35 ticks, but ends by discard with no residue, no settling debris, and no scar.

### Numbers holding the spectacle back

- Hell Hunt step spacing is 3 ticks, so a full 8 target chain resolves in 21 ticks (about 1 second) while each impact visual lives 35 ticks: at the end of a chain, up to 8 impacts are alive at once and overlap into one undifferentiated field.
- Hell Hunt cooldown 100 ticks (5 seconds) plus the 45 Ki discrete cost makes the weapon's biggest effect its rarest.
- Projectile trail density: EMBER count 2 every 2 ticks is about 20 particles across an entire 40 tick flight at density 1.0.
- Charge time to spectacle ratio: up to 120 ticks of holding buys a projectile that lives at most 40 ticks.

### Bugs, dead code, mismatches

- DEAD CODE: EvilSpearProjectileRenderer.drawBeam (line 305) is defined and never called. Verified by grep: the only occurrence in the file is the definition. submit never references it.
- DEAD FIELD: PendingHunt.stack is written in performHellHunt and never read anywhere. A local PlayerStatsAccessor declared in performHellHunt is also unused.
- ASYMMETRY: EvilSpearItem.onPlayerDisconnect clears ACTIVE_CHARGE_MAP but not PENDING_HUNTS or PENDING_LUNGES. Those two self-clean on the next tickServer pass through a null player check, so the exposure is one tick, but the cleanup contract is inconsistent with the other weapons in this set (Ox and Lava clean all of their static maps).
- MISMATCH (lock-on through walls): findLockOnTarget performs no block occlusion test, so Hell Hunt locks onto, and then teleports the player onto, targets behind walls, while every visual in the chain is a ground strike at the target's feet.
- MISMATCH (visual ground break, real ground untouched): HellHuntImpactRenderer draws shattering trenches and erupting spikes for 35 ticks; HellHuntImpactEntity never touches a block. When the effect ends the ground is exactly as it was, with no scar, no scorch, and no displaced block.

---

## 3. Devil Trident

Theme: crimson demonic. A spammable 3-prong laser shot, and a deployable 10 shard swarm with its own health economy, Ki upkeep, and recall cooldowns. The swarm is the weapon.

### Ability map

Melee (DevilTridentItem.java):

- Attribute attack damage 749, plus base 1, total 750 (line 38). Attack speed modifier -2.2, so 1.8 attacks per second.
- Passive while held: FRICTION +0.60 as ADD_MULTIPLIED_BASE.
- hurtEnemy does no direct effect work. It calls performLeftClickTargeting with the struck target as the fallback, so a melee connect also runs the left-click behavior: mark a swarm target if deployed, or fire the laser if not (the fireLasers branch is at line 112).

Left click, not deployed: the laser (fireLasers, line 126; also reached from the air-swing payload at network/DbaNetwork.java line 482):

- Ki: drainKiDiscrete 60.0 per 10 ticks (line 135). Item cooldown 10 ticks (line 142). LIGHTNING_ZAP sound.
- Spawns one TridentProngEntity with damage 750.0 flat (TridentProngEntity.java line 30; the item passes 750.0f). No Strength scaling.
- TridentProngEntity: speed 2.5 blocks per tick (line 67); begins dying when tickCount exceeds 28 (line 89), so maximum range is about 70 blocks. A block clip triggers beginDying (line 98). The first entity hit takes capDamage(750) and triggers beginDying (line 111). Dying is a 6 tick dissolve, then discard. The hit applies no effects: no MARKED_BY_EVIL, no DEVILS_HANDS, nothing.
- The entity emits no particles.

Left click, deployed: swarm targeting (performLeftClickTargeting):

- Target search in the player's box inflated by 64.0; cone dot above 0.90; score is distance x (2 - dot), lowest wins; a melee fallback target is used directly if supplied.
- On a found target: the stack's CUSTOM_DATA gets swarmTarget set to the target UUID, the target gets MARKED_BY_EVIL for 60 ticks, and a ZOMBIE_VILLAGER_CONVERTED sound plays at pitch 2.0.
- Cost: none. No Ki, no cooldown.

Right click, deploy (use, when not deployed):

- Spawns DevilSlamShockwaveEntity at the player's position with radius 5.0 (line 180). GENERIC_EXPLODE sound and a DbaImpactFx.heavy burst.
- AoE: every LivingEntity in the player's box inflated by 5.0 takes 750 x falloff (line 195), falloff 1.0 at distance 2.5 or less, falling linearly to a floor of 0.4 at the rim. Each target also gets DEVILS_HANDS 300 ticks at amplifier 2, CINEMATIC_TRACKING 300 ticks, BOUNCINESS 0.85, AIR_DRAG -0.40 multiplied base, and a downward velocity add of y -2.0 with markHurt.
- Spawns 10 TridentShardEntity at the player's position, y + 2.0. SwarmHelper.deploySwarm(stack, 10, 15000.0f) (line 227).
- Cooldown 20 ticks, a buffer before recall is allowed (line 229).
- Ki cost to deploy: none. The cost is the upkeep drain below.

Right click, recall (use, when deployed): recallShards recalls every shard owned by the player within inflate 64.0, runs SwarmHelper.recallSwarm, and adds a 600 tick (30 second) item cooldown (line 163).

Swarm upkeep (manageShardSwarm, called every server tick from DragonBlockArcaneDBA.java line 267):

- Only runs while the player holds a Devil Trident in either hand.
- Missing shards respawn from SwarmHelper health records at the player's position, y + 2.0.
- While deployed: drainKiPerTick at 20.0 per minute (line 281). If a drain fails, every shard returnHome; when the last shard arrives, onSwarmArrivedHome runs SwarmHelper.recallSwarm and applies a 3600 tick (3 minute) cooldown (line 365).
- Protection: enemy projectiles within the player's box inflated by 12.0 are assigned to shards, which discard the projectile and lose 50 health (TridentShardEntity.intercept).
- Offense: if swarmTarget is set and alive, all remaining shards attack it. Otherwise shards auto-assign to Enemy mobs within inflate 40.0, sending ceil(enemy health / per-shard damage) shards per enemy. Leftover shards idle-orbit.
- A dead or unloaded swarmTarget is cleared from the stack data.

TridentShardEntity:

- Health 15000.0 per shard (TridentShardEntity.java line 23). The swarm health budget is spent in damage dealt and in damage taken (hurtServer subtracts incoming damage; intercept costs 50).
- Damage per hit: 100 + Strength x 0.5 (DevilTridentItem.shardDamage).
- Attack: moves 2.5 blocks per tick toward the target's center (line 236). Damage lands when distanceToSqr to the target is below 4.0, i.e. within 2 blocks (line 242), and repeats every tick with no per-shard or per-target hit cooldown in this code: capDamage(100 + Strength x 0.5), DEVILS_HANDS 60 ticks, TRIDENT_HIT sound, and the shard's health drops by the damage dealt. At 15000 health and about 100 damage per hit, one shard's budget is about 150 hits.
- Idle: orbits the owner at radius 2.2 (line 264) with angle advancing 0.4 radians per tick and a vertical sine wave of amplitude 0.75.
- Particles: EMBER count 1 every 3 ticks per shard (line 209 area), about 67 particles per second for a full 10 shard swarm at density 1.0.
- Recall flight: speed min(1.8, max(0.35, distance x 0.48)) with a slight sinusoidal bow; discarded within 0.42 blocks of the owner's hand point. The entity also discards outright if the owner is dead or more than 64 blocks away.

DevilSlamShockwaveEntity: lifetime 35 ticks (DevilSlamShockwaveEntity.java line 20), synced RADIUS. Server particles: a SPARK burst on tick 1 (count about 10 x density, spread radius x 0.5) and EMBER for the first 12 ticks (count about 3 x density). It deals no damage and touches no blocks; all deploy damage is in the item.

Renderers:

- TridentShardRenderer: mesh pass plus an additive halo pass that redraws the same head silhouette at 1.14 scale and alpha 0.2 x pulse. The head is a 6 slice curved blade with shell and core layers, barbs, and a collar crystal. A 3 strand helical wake of 10 steps trails it. The whole blade rolls at age x 22 degrees per tick and pulses; spawn scale-in runs over the first 5 ticks. No fade out: shards are discarded on arrival home.
- TridentProngRenderer: one pass on kiRenderType, no additive pass. 3 needles at offsets -0.26, 0, +0.26 with lengths 1.25, 1.65, 1.25, each the same barbed head construction as the shard at 0.42 width, plus 3 strand helical wakes of 8 steps. Fade in over 2 ticks; the dying dissolve fades over 6 ticks synced to the entity's die tick.
- DevilSlamShockwaveRenderer: the richest renderer in this weapon. Main pass: 5 seal rings (48, 56, 36, 20, 12 segments) including a traveling follow ring that expands after age 5; 12 barbed teeth; 9 curved fissure trench volumes of 7 steps plus 3 curling tail segments each; 14 crystal spikes that rise over 8 ticks and sink after 50 percent of life; a 3 layer hellfire pillar (12, 8, 6 sides, height 6.7) with a 2 strand column helix. Additive pass: a core ring, the follow ring, and a pillar core. Fade is a smoothstep over the full 35 tick life. All of it is geometry; the entity-side particles are the tick 1 spark burst and 12 ticks of embers.

### Gap analysis against the 5 points

1. Layered particle depth: PARTIAL on geometry, FAIL on particles. The slam renderer has genuine multi-layer structure (seal, trenches, spikes, 3 layer pillar, helix, plus an additive pass), and the shard has a mesh plus halo silhouette. But none of it is particle banding: no dense particle core, no mid particle layer, no wispy particle edge, and no distance scaling. The prong shot, the weapon's most-used effect, is a single-pass mesh whose entity emits zero particles.
2. Debris: PARTIAL, the closest in this weapon set. The swarm is 10 individual physical pieces, which is the right shape for point 2. But they fly a fixed radius 2.2 orbit with a sine bob, all at one uniform 0.85 scale; the renderer rolls each blade in place, which reads as spin, not tumbling; there are no helical storm paths around a center, no size variety, and no debris field. When the swarm ends, shards fly home and are discarded at the hand: nothing rains down.
3. World interaction: FAIL. The deploy slam draws fissure trenches, spikes, and a hellfire pillar for 35 ticks and changes zero blocks; nothing is scarred during or after. Mob physics is a single downward velocity add of -2.0: targets are pinned down, the exact opposite of the reference's pull in, lift, and fling. The prong dies on the first block with a 6 tick dissolve and no impact effect, burst, or mark.
4. Atmosphere: FAIL. LIGHTNING_ZAP is a sound. Nothing else.
5. Lifecycle: PARTIAL. The slam visual grows its seal over 10 ticks and fades across its whole life; shards scale in over 5 ticks; the prong has a 2 tick fade in and a 6 tick dissolve. Against that: the deploy is instant in effect (full damage and full downward pull on the use tick, under a visual that is still growing), and all 10 shards appear at once at y + 2.0 with no staggered build-up; the swarm's end is clean but abrupt per shard (discard at 0.42 blocks from the hand).

### Numbers holding the spectacle back

- The laser is the loudest spammable number and the plainest effect: 750 flat damage every 10 ticks (0.5 seconds) riding on one 3-needle mesh with no trail particles, no impact effect, and no on-hit status.
- Swarm upkeep is cheap (20 percent of Ki per minute), so cost is not the limiter; the fixed counts are: 10 shards, orbit radius 2.2, EMBER 1 per shard per 3 ticks.
- Deploy slam radius is fixed at 5.0 for both damage and visuals, while the renderer's trench art reaches radius x 1.25 plus curling tails: the picture promises about 6.5 blocks, the effect delivers 5.
- Shard damage cadence: a shard parked inside the 2 block trigger attempts a hit every tick. Even where the target's own invulnerability rules thin that out, the design number is 20 attempts per second per shard, and the 15000 health budget is sized for it (about 150 hits at base damage).

### Bugs, dead code, mismatches

- DEAD BOOKKEEPING: TridentProngEntity.hitIds is a maintained set, but the hit loop returns after the first hit and begins dying, so the set can never hold more than the one hit that ends the shot. The code reads as a piercing projectile; the behavior is strictly single target.
- MISMATCH (melee doubles as the laser): hurtEnemy routes into performLeftClickTargeting, whose not-deployed branch fires the laser. One melee connect can therefore deal 750 melee damage plus a 750 prong if the shot connects, for a single 60 Ki charge. The method comment frames hurtEnemy as a fallback targeting path; nothing in the naming or the item's other text tells the player melee also fires.
- MISMATCH (ground slam that breaks no ground): the deploy is named and drawn as a ground slam with fissure trenches (DevilSlamShockwaveEntity and its renderer), and neither the item's use code nor the entity modifies, displaces, or scars a single block.
- EDGE CASE: onSwarmArrivedHome scans the player's entire inventory for a stack whose CUSTOM_DATA says deployed. If that stack was moved, split, or its data changed mid-return, the 3 minute cooldown and the recall bookkeeping land on the wrong stack or on none.
- EDGE CASE: fireLasers picks the trident stack as the main hand item if it is a Devil Trident and otherwise takes the offhand item unconditionally. If neither hand holds the trident (reachable only through unusual call paths, since the normal callers hold it), the cooldown branch silently skips while the Ki drain still gates the shot. The failure mode is a missing cooldown, not a free shot.
- The 3 minute starvation cooldown (3600 ticks) is applied only when the last shard physically arrives home (finishReturn path). A shard whose owner logs off or moves more than 64 blocks away discards outright in tick, bypassing finishReturn, so a swarm can end with no cooldown at all if its shards are discarded by distance rather than arriving.

---

## 4. Ox King's Axe

Theme: magma, basalt, gold. The heavyweight: a 10 second charge into a terrain-breaking shockwave, and a stance that ends in a slam. This weapon owns the set's only real block destruction.

### Ability map

Melee (OxKingsAxeItem.java):

- Attribute attack damage 949, plus base 1, total 950 (line 79). Attack speed modifier -2.8, so 1.2 attacks per second.
- Passive while held: FRICTION x 2.0 (ADD_MULTIPLIED_BASE 1.0).
- hurtEnemy is an empty no-op, by design per its comment: the abilities already pay. A plain melee hit is vanilla attribute damage with no effect, no cost, and no visual of its own.

Left click, Groundbreaker charge (onLeftClickChargeTick, driven by payloads at network/DbaNetwork.java line 492):

- MAX_LEFT_CHARGE_TICKS is 200, 10 seconds (line 64).
- Stamina: drainStaminaPerTick at 125 percent per minute (line 115). If a drain fails, the charge force-releases at its current value.
- Rooting tiers: below 40 ticks, OX_BRACE amplifier 0 refreshed for 10 ticks; 40 to 99 ticks, amplifier 1; at 100 ticks and above, MovementPhysics.rootInPlace plus amplifier 2.
- Full charge cue at tick 190 (MAX minus 10): WARDEN_SONIC_CHARGE sound and an action bar message. Charge rumble sound every 20 ticks with pitch rising with charge.
- OxChargeEntity follows the player with the synced charge ratio; it discards itself after more than 6 ticks without an update. It emits EMBER every 2 ticks at count (2 + 4 x charge) x density.

Left click release (onLeftClickRelease):

- chargeRatio is floored at 0.05. Damage is 350 + 850 x ratio + Strength x 3.5 x ratio (line 173): 392.5 at a tap, 1200 + 3.5 x Strength at full charge, before the cap.
- Cooldown is 20 + 40 x ratio ticks, 21 to 60 ticks (line 193 area).
- Spawns OxShockwaveEntity and plays sweep, anvil, and explode sounds scaled by charge, plus a DbaImpactFx.heavy burst.

OxShockwaveEntity:

- maxRadius is 4 + 20 x charge (line 69): 4.2 blocks at tap, 24 at full charge. Sub waves multiply radius by 0.75. maxTicks is max(10, radius x 1.3) (line 74), up to 31 ticks at full charge. Expansion uses ease-out cubic progress: the wave is fastest at the start and slowest at the end.
- Hit detection is an annulus: targets whose horizontal distance falls between currentRadius - 1.8 and currentRadius + 1.8 (inner radius at line 163), inside a box spanning y -2 to +3. Each entity is hit once per wave.
- Damage is the wave's full damage with no distance falloff. On hit: CINEMATIC_TRACKING 30 ticks, MOVEMENT_CURSE 30 ticks at amplifier 1, BOUNCINESS 0.90, AIR_DRAG -0.50 multiplied base.
- Knockback: horizontal 1.4 + 1.8 x charge (line 223), 1.49 at tap and 3.2 at full charge; vertical 0.6 + 0.7 x charge (line 224), 0.64 to 1.3, added to current velocity with the vertical component floored through max(current y, 0.2) first. EXPLOSION particles, count 2, at each hit target.
- Echo waves, at charge 0.50 and above: a second wave spawns at tick 6 with charge x 0.8 and damage x 0.40 (line 136), and a third at tick 12 with charge x 0.6 and damage x 0.40 (line 142). Each echo recomputes its radius from its scaled charge and then applies the 0.75 sub wave factor, so at full charge echo 1 has radius (4 + 16) x 0.75 = 15 blocks. Each wave keeps its own hit set, so one target can be hit by all three waves: total full-charge damage to a single target is about 1.8 x the headline wave damage.
- Block destruction, main wave only, at charge 0.95 and above: see the block section below.
- Fissures, at charge 0.95 and above, on tick 1: 6 OxFissureEntity at random angles and distances of 3 to 3 + 0.6 x maxRadius, yaw set radially outward.

OxFissureEntity:

- MAX_LIFETIME is 100 ticks, 5 seconds (OxFissureEntity.java line 24). damagePerSec is 50 (line 23), applied as 25 damage every 10 ticks to every LivingEntity in the entity's box inflated by (2.5, 1.5, 2.5) (line 73), plus igniteForSeconds 2.0 per hit (line 86).
- EMBER every 4 ticks, LAVA_POP sound every 10 ticks, one SlamDebris burst on tick 1 with profile OX_FISSURE at radius 2.5.
- It changes no blocks. When it expires, the ground is untouched.

Right click, Colossal Stance (use, onUseTick, releaseUsing):

- use() fails with no stamina, otherwise starts using; getUseDuration 72000; animation BLOCK.
- Stamina drain 125 percent per minute (line 221). MovementPhysics.rootInPlace every tick. OX_BRACE amplifier 1 refreshed every tick.
- King's Force aura, radius 12.0 (line 236): every tick, each enemy in range (tamed pets are exempt) gets FISSURE_STUN 20 ticks refreshed and an outward push of 0.35 horizontal and +0.05 vertical with markHurt. Every 5 ticks, each also takes 45 + Strength x 0.4 (line 255) plus CINEMATIC_TRACKING 25 ticks. Sustained, that is about 180 + 1.6 x Strength damage per second for standing inside the aura.
- Self pressure damage from heldTicks 200 on: 3 percent of current HP per second, applied per tick as health x 0.03 / 20 with a floor of 1.0 (line 282 area).
- OxStanceAuraEntity mirrors held ticks and the peak flag to the renderer; discards after more than 6 ticks without an update; EMBER every 2 ticks scaling with held time, SPARK every 4 ticks during the peak window.
- Peak window: heldTicks 280 to 300 (14 to 15 seconds), with a WARDEN_SONIC_CHARGE cue every 5 ticks.
- Failure state, heldTicks above 300: recoil self damage of 25 percent of (2000 + Strength x 5), stopUsingItem, SHIELD_BREAK and explode sounds, EXPLOSION_EMITTER x 2, and a 200 tick (10 second) cooldown (line 318).

Normal slam (releaseUsing, held 10 to 300 ticks, outside the peak window):

- holdRatio is min(1, heldTicks / 200). Damage is 400 + 900 x holdRatio + Strength x 3 x holdRatio (line 381). Radius is a fixed 10.0 at every hold length (line 384).
- Damage is instant at the release tick to everything within the radius, with no distance falloff: FISSURE_STUN 80 ticks, CINEMATIC_TRACKING 40, MOVEMENT_CURSE 40 at amplifier 1, launch 1.5 horizontal and +0.5 vertical.
- Spawns KingsSlamEntity with radius 10 and lifetime 40. Cooldown 40 + 60 x holdRatio ticks, 40 to 100.

Flawless King's Slam (releaseUsing, held 280 to 300 ticks):

- Damage 2000 + Strength x 5 (line 340), radius 20.0 (line 341), flat with no falloff. FISSURE_STUN 140 ticks at amplifier 1, CINEMATIC_TRACKING 60, MOVEMENT_CURSE 60 at amplifier 2. Launch 2.5 horizontal and +0.9 vertical.
- Spawns KingsSlamEntity with radius 20, flawless flag, lifetime 55. EXPLOSION_EMITTER x 3, explode and WARDEN_SONIC_BOOM and WIND_GUST sounds. Cooldown 100 ticks (line 377).

KingsSlamEntity: purely visual plus particles. Lifetime 40 normal, 55 flawless (KingsSlamEntity.java line 34). One SlamDebris burst on tick 1 with profile KINGS_SLAM at the slam radius (line 66). Crest embers while tickCount is at most 21: every 3 ticks, about 8 x density EMBER placed around the expanding front. All slam damage lives in the item's releaseUsing.

Renderers:

- OxChargeRenderer: one pass on kiRenderType, no additive pass. 8 to 14 branching magma trench ribbons (count scales with charge), each drawn as a base segment plus a molten core segment; 2 compression rings at 40 segments; a tapered central column up to about 2.75 blocks tall at full charge; 10 to 18 levitating rock chunks that bob on a sine and tilt with charge; ascending heat tendrils from charge 0.25 up, 6 to 12 of them. No fade in or out: the entity is discarded on release.
- OxShockwaveRenderer: one pass on kiRenderType, no additive pass. 28 to 36 faceted crags on the wavefront ring (24 on sub waves), each drawn twice (body plus molten vein), 26 molten teeth, and 14 inner shards, all riding the synced current radius. The renderer recomputes the wave's real lifetime from the synced charge ratio and fades fully to zero across the final 30 percent of life; crag height ramps in over the first 22 percent.
- OxFissureRenderer: mesh pass plus an additive halo pass along the trench core (the halo rebuilds the same seeded path). The trench is 3.6 blocks long: an 8 segment main crack with 3 width layers (base, core, white-hot center), stone teeth on alternating segments, and 2 branch cracks. Fade in over 4 ticks, fade out across the final 30 percent of the 100 tick life.
- OxStanceAuraRenderer: one pass on kiRenderType, no additive pass. A 12 block boundary ring pair at 48 segments with 12 radial pressure nodes and a repeating expanding ripple; a titan avatar (2 shoulder pauldrons, 2 horns, front and back aegis plates with hotter inner core plates); 2 counter-rotating heat domes; 2 helical ribbons of 20 steps; at peak only, 12 radiating energy lashes. Alpha is 0.70 + intensity x 0.25 from the first frame; there is no fade in or out.
- KingsSlamRenderer: one pass on kiRenderType, no additive pass. 2 ground rings (48 and 32 segments), 2 hemispherical dome walls, 12 radiating canyon trenches of 6 steps (18 on flawless) with 3 width layers, 16 erupted monoliths (28 flawless) which are single triangles drawn front and back, and on flawless only, 8 magma geysers of 6 to 8.5 blocks with flickering heights. Growth runs over the first 55 percent of life; fade out runs across the final 38 percent.

### Gap analysis against the 5 points

1. Layered particle depth: FAIL on particles, PARTIAL on geometry. The charge, wave, stance, and slam renderers are all single-pass mesh with no additive layer (only the fissure has a halo pass). The wave renderer puts everything on one ring: 28 to 36 crags plus 26 teeth plus 14 shards at the crest, with no dust wall behind the crest, no inner debris band, and no wispy trailing edge. The only true particles are EMBER and SPARK trickles and the SlamDebris burst. No distance scaling anywhere.
2. Debris: PARTIAL, the best in this set, and still one burst deep. SlamDebris throws real ground-textured block particles (up to 140 spawn points x 2 particles at large radii, plus POOF dust), which is the only genuine debris any of these five weapons produces. But it fires exactly once, on tick 1: no tumbling individual pieces, no helical orbits, no sustained debris during the 40 to 55 tick slam, and no rain-down phase as it ends. The charge renderer's 10 to 18 rock chunks levitate and bob but never rotate, and they vanish the frame the charge is released.
3. World interaction: STRONGEST in the set, with a hole in the middle. Mobs: the wave launch (up to 3.2 horizontal, 1.3 vertical, BOUNCINESS 0.90, halved air drag) is a real lift-and-fling; slams launch; the stance pushes. But every force points outward or up: nothing in this weapon pulls mobs in toward a center the way the reference storm does. Ground: real destruction exists (below), but slams and fissures scar nothing: the fissure's drawn crack is a 3.6 block trench visual over a damage zone that changes no blocks and leaves nothing after 5 seconds.
4. Atmosphere: FAIL. No darkening, haze, or lightning. The flawless slam's WARDEN_SONIC_BOOM is the biggest cue.
5. Lifecycle: MIXED, with the set's best and worst in one weapon. Best: the shockwave's full fade across its final 30 percent, the fissure's in and out fades, and KingsSlam's grow-then-fade envelope. Worst: the charge visual pops out instantly on release (trenches, rings, and levitating rocks at full alpha one frame, gone the next; only the spawned wave covers the cut), the stance aura pops in at a minimum alpha of 0.70 on frame one and pops out on release, and slam damage is instant at the release tick while the KingsSlam visual needs 22 to 30 ticks to grow to full radius, so rim targets are launched by a dome that has not visually reached them yet.

### Numbers holding the spectacle back

- The destruction gate is charge 0.95 and above: a hold of about 9.4 seconds is the minimum for any block interaction at all. A 0.94 charge swing of the same 10 second class destroys nothing.
- Ring sampling density: destroy points are max(8, currentRadius x 2), about 48 points around a 24 block ring, a spacing of about 3.1 blocks. Roughly two of every three blocks on the ring line are never tested. Destruction reads as scattered holes, not a broken ring.
- Normal slam radius is frozen at 10.0 while its damage scales with hold time: a 0.5 second hold and a capped-ratio hold cover identical ground.
- Fissure damage is 50 per second, about 4 percent per second of the full-charge wave hit that creates it. The lingering hazard is numerically trivial next to its own cause.
- The stance asks for up to 15 seconds of standing perfectly still; its spectacle during that time is one aura entity, EMBER trickles, and a push of 0.35 per tick.

### Bugs, dead code, mismatches

- DEAD CODE: OxChargeRenderer.drawAscendingTendril (line 316) is defined and never called; submit uses drawLash. Verified by grep: one occurrence, the definition. OxStanceAuraRenderer.drawSegment is likewise defined and never called (grep count 1).
- CLIENT STATE MISMATCH RISK: OxShockwaveEntity.maxRadius and maxTicks are plain fields set only in the gameplay constructor. A client copy built through the EntityType constructor keeps the defaults (maxRadius 12.0, maxTicks 20), and tick() runs on the client too, recomputing the synced CURRENT_RADIUS from those wrong values each client tick between server updates. OxShockwaveRenderer works around the lifetime half of this by recomputing maxTicks from the synced charge ratio, but the radius value it reads can still be locally overwritten.
- MISMATCH (damage timing vs visual timing): slam damage is applied instantly in releaseUsing, while KingsSlamEntity's visual grows across 55 percent of a 40 or 55 tick life. At radius 20, the dome needs about 30 ticks to arrive; the damage arrived at tick 0.
- MISMATCH (fissure shape): OxFissureRenderer draws a 3.6 block long trench; OxFissureEntity damages a box inflated by (2.5, 1.5, 2.5) around a near-zero-size entity box, roughly a 5 x 3 x 5 block square centered on the spawn point. The drawn crack and the damaging area do not share a shape or an orientation.
- INCONSISTENCY: running out of stamina mid-stance routes through stopUsingItem into releaseUsing and still pays out a scaled normal slam, while holding past 300 ticks is punished with recoil and no slam. The class javadoc frames failure only as the over-15-second collapse; the stamina path is a second, unlabelled failure mode that behaves like a success.
- NOTE: the failure path calls stopUsingItem after manually discarding the stance entity; releaseUsing then removes and discards from ACTIVE_STANCE_MAP a second time. Harmless, but the two exit paths were clearly not unified.

---

## 5. Lava Sword

Theme: fire and real lava. The newest and smallest weapon in the set (one 133 line item class), and the only one that changes blocks temporarily instead of destroying them or ignoring them.

### Ability map

Melee (LavaSwordItem.java):

- Attribute attack damage 859, plus base 1, total 860 (line 46). Attack speed modifier -2.1, so 1.9 attacks per second.
- hurtEnemy is an empty no-op, per its comment: the cleave and the eruption already pay. A plain melee hit from the Lava Sword applies no burn, no effect, and no ground scorch.
- No passives of any kind.

Left click, cleave (performCleave, from the swing payload at network/DbaNetwork.java line 547):

- Guards: returns early if the item is on cooldown, and a static LAST_CLEAVE_TICK map swallows a second call in the same game tick (the swing packet and the melee path can both fire for one press).
- Ki: drainKiDiscrete 90.0 per 14 ticks (line 73). Item cooldown 8 ticks (line 76).
- Damage 740 + Strength x 2 (line 79). BLAZE_SHOOT and PLAYER_ATTACK_SWEEP sounds.
- LavaCleaveEntity: velocity look x 1.35 blocks per tick; discards when tickCount exceeds 10 (LavaCleaveEntity.java line 62), so maximum travel is about 13.5 blocks. Piercing, each target once, hit box inflated by (1.3, 0.8, 1.3). On hit: capDamage(damage) and setRemainingFireTicks raised to at least 60 (line 83). No knockback at all.
- Ground burn: every tick, LavaSwordGround.scorchSegment over the segment flown that tick, at radius 1.35, spending from a per-cleave lava budget of 3 sources (lavaLeft field, line 27; spend at line 85).
- On a block hit: LavaSwordGround.scorchLanding on the hit block (line 90), then the entity discards. The landing call guarantees the pile sits on top of the block the cleave struck, if budget remains.

Right click, eruption (use routing into performEruption):

- use() fails on cooldown or with no Ki. Guards mirror the cleave (LAST_ERUPT_TICK same-tick swallow).
- Ki: drainKiDiscrete 90.0 per 40 ticks (line 113). Item cooldown 40 ticks (line 116).
- Damage 880 + Strength x 2.2 (line 119). LAVA_POP, BLAZE_SHOOT, and FIRECHARGE_USE sounds.
- LavaEruptionEntity: life() is 12 ticks (LavaEruptionEntity.java line 46). reachAt grows the wave from 1.1 to 6.5 blocks with quadratic ease-out (line 50). Each tick, targets are hit if they fall in the annulus between the previous reach minus 0.7 and the current reach plus 0.4, and inside a frontal cone of dot at least 0.15 (line 91), an 81 degree half-angle, about 162 degrees in total. Each target is hit once. On hit: capDamage(damage), burn raised to at least 80 ticks (line 99), and a push of 0.45 forward and +0.28 vertical (line 100).
- Ground burn: every tick, LavaSwordGround.scorchArc over the same annulus and cone the damage uses, spending from a per-eruption lava budget of 5 sources (line 23).
- Shared cooldown: both abilities put the cooldown on the same item stack, so an eruption locks out the cleave for 40 ticks and a cleave locks out the eruption for 8.

LavaSwordGround (entity/LavaSwordGround.java), the burn system:

- Surface finding (scorchColumn): starting from a height hint, it climbs up to 4 cells if the hint cell is inside a block, then searches downward up to 6 cells for an air cell sitting on a block whose top face is sturdy. That cell is the surface.
- Placement (igniteSurface): FIRE goes on every eligible surface cell. Real LAVA SOURCE blocks (LiquidBlock LEVEL 0) go on the first lavaBudget eligible cells in scan order (dx from -reach to +reach, dz likewise). Cells adjacent to water fall back to fire instead of lava. Cells occupied by a living player are skipped. alreadyPending cells are skipped, so overlapping swings do not double-book a cell.
- Placement flags are Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SKIP_ON_PLACE (line 43), so the lava does not run its on-place logic: it does not schedule flow and does not cook neighboring water into cobblestone, stone, or obsidian on placement. Block ticks and fluid ticks for the cell are cleared every tick while it is pending.
- Lifetime: LIFE_TICKS is 80, 4 seconds (line 36). A server tick hook restores each cell's previous block state (always air, since placement requires air) when its timer expires.
- Neighbor watch: for each lava cell, all 26 neighbor states are snapshotted at placement. Every tick, hold() reverts any neighbor that flowed into lava, converted to cobblestone, stone, obsidian, or basalt, or caught stray fire, unless the neighbor's original state is on the protected list.
- Server stop: a SERVER_STOPPING hook restores every pending cell belonging to that server.
- Mob interaction is indirect but real: the placed fire and lava are genuine blocks, so anything standing in them takes vanilla fire or lava damage and burns under vanilla rules for as long as the cells exist.

Renderers:

- LavaCleaveRenderer: one pass on kiRenderType, no additive pass. A main crescent (12 segments, radius up to 1.35, opening over 2.5 ticks), a smaller trailing crescent at 0.45 alpha, and 4 drip beads hanging under the lip that sink as age advances. Fade is smoothstep(1 - age / 10), reaching zero exactly when the entity discards.
- LavaEruptionRenderer: one pass on kiRenderType, no additive pass. A single wave sheet on a 14 x 5 grid whose reach is read live from the entity's reachAt, bow height peaking mid-wave, colors ramping to a bright edge on the outer band. Fade is smoothstep(1 - age / 12), matched to the entity's 12 tick life.
- Neither entity emits any particle of any type. The only lasting visual is the real fire and lava blocks on the ground.

### Gap analysis against the 5 points

1. Layered particle depth: FAIL, the weakest visuals in the set. Each effect is a single mesh sheet with no additive pass, no core/mid/outer structure, and zero emitted particles. The ground burn (real fire and lava blocks with vanilla animation) carries more visual depth than the projectiles do.
2. Debris: FAIL. No debris, no sparks, no ember fall, nothing individual. A lava weapon that sheds nothing.
3. World interaction: STRONGEST in the set for blocks, WEAKEST for mobs. This is the only real block-changing system among the five weapons: genuine lava sources and genuine fire, placed on the actual surface, held against flow for 80 ticks, then restored. Mobs in those cells burn under vanilla rules. But the abilities themselves move mobs almost not at all: the cleave applies no knockback, the eruption's push is 0.45 and +0.28, and nothing pulls, lifts, or flings. Scarring is temporary by design: after 4 seconds per cell, the ground is byte-identical to before the swing.
4. Atmosphere: FAIL, with the set's one irony: the placed lava emits vanilla light level 15, so the Lava Sword briefly brightens its area. Nothing darkens, hazes, or flashes.
5. Lifecycle: PARTIAL. The meshes fade across their full (short) lives with fades matched to the entity discards, and the ground burn expires cell by cell in placement order, oldest first, which reads as a trail burning out behind the swing. Against that: the whole weapon lives at short timescales (10 tick cleave, 12 tick eruption, 80 tick burn), there is no build-up phase anywhere, and the burn's end is a hard per-cell restore, visible as individual cells snapping back to air on their own timers rather than a unified cool-down.

### Numbers holding the spectacle back

- Lava budgets: 3 sources per cleave and 5 per eruption, against a cleave scorch disk of radius 1.35 (about 9 cells per tick of flight, roughly 90 cells over a full flight) and an eruption annulus covering dozens of cells. Over 90 percent of the burn is fire; the lava reads as a few puddles.
- Budget spend order: scorchDisk scans dx then dz from the negative corner, and the cleave re-runs it every tick at the current segment, so the budget tends to be spent on the earliest, nearest, and north-westernmost eligible cells rather than at the impact point. The landing call fixes this only for the final block and only if budget remains.
- Cleave reach: 10 ticks at 1.35 blocks per tick is about 13.5 blocks for a 740 + 2 x Strength hit. The entire effect, flight, trail, and fade, is over in half a second.
- Eruption reach caps at 6.5 blocks after 12 ticks: the "eruption" is a short frontal fan, smaller than the Ox normal slam's fixed radius of 10.
- Target burn times (60 ticks cleave, 80 ticks eruption) are at or below the ground burn's own 80 tick life, so victims stop burning before the ground does.

### Bugs, dead code, mismatches

- PERSISTENCE GAP (the serious one): PENDING is an in-memory static ArrayList (line 46) with no on-disk record. The restore guarantee holds only while the server keeps ticking or stops cleanly through the SERVER_STOPPING hook. If the server crashes, is killed, or a chunk unloads in a way that skips the hooks, the placed lava sources and fire are real saved blocks with no record anywhere: they persist permanently, and after a reload nothing clears their fluid ticks, so the lava flows like natural lava. A temporary cosmetic burn can become permanent terrain damage through a crash.
- DEAD CHECK: protectedSolid (line 244: bedrock, spawner, trial spawner, chest, trapped chest, ender chest, barrel) is consulted in igniteSurface against the block being replaced and in restore() against the restore state. Placement already requires that cell to be air, so both checks can never fire. The list does real work only inside hold(), guarding neighbor reverts. As written, it looks like protection for the placed cell and is not.
- MISMATCH (eruption cone vs drawn arc): the damage cone accepts dot at least 0.15, about 162 degrees total, while LavaEruptionRenderer draws its wave across an arc of 1.15 x 2 radians, about 132 degrees. Targets near the side edges take 880 + 2.2 x Strength from a wave that never visually reaches them. (The scorch arc uses the same 0.15 dot as the damage, so the ground burn matches the damage; it is the mesh that is narrower.)
- MISMATCH (identity): the Lava Sword's plain melee hit, the hit it lands most often, applies no burn and no effect at all (empty hurtEnemy). The weapon's name and both abilities are built on fire; its basic attack has none.
- BEHAVIOR NOTE: scorchColumn's downward search of up to 6 cells means that over a ledge or cliff edge, the burn can land on ground up to 6 blocks below the cleave's flight path. The trail can appear on a lower floor the crescent never touched. That may be desirable as dripping, but it is not documented in the item or the helper's javadoc.
- The cleave runs its scorch even on the tick it spawns at the player's eye, and occupiedByPlayer only skips the exact cell a living player occupies: a cleave fired at point blank scorches the ground around and under the fight, including under the target's feet, which is presumably intended, but the same skip does not extend to other entities, so mounts and pets get lava placed under them freely.

---

## Block interaction deep dive (all five weapons)

This set was expected to carry the mod's ground destruction. What the code actually does, weapon by weapon:

### What breaks blocks

- Ox King's Axe, Groundbreaker release, ONLY at charge 0.95 and above, ONLY in OxShockwaveEntity, ONLY on the main wave (echo waves do not destroy). Each tick, the wave tests destroyPoints = max(8, currentRadius x 2) points evenly spaced on the ring at its current radius, at two heights: the block at the wave center's y and the block above it. checkAndDestroyWeakBlock (OxShockwaveEntity.java lines 245 to 265) destroys any block whose explosion resistance is at most 2.0 (line 257), except bedrock and barrier, using destroyBlock(pos, true) (line 263), so destroyed blocks drop as items. The code's own comment lists the intended class: dirt, grass, flowers, leaves, glass, ice, snow. Six BLOCK particles per destroyed block.
- Nothing else in the five weapons breaks a block. Not the King's Slam at 2000 + Strength x 5, not the Devil slam, not Hell Hunt impacts, not the Spirit Cannon, not the Lava Sword.

### What changes blocks temporarily

- Lava Sword only, through LavaSwordGround: real fire on every eligible surface cell along the cleave path and eruption arc, and real lava source blocks up to the per-swing budget (3 cleave, 5 eruption), each restored to its previous state after 80 ticks, with flow, conversion, and fire spread actively reverted every tick in the 26 neighbor cells of each lava source. Subject to the crash persistence gap documented in the Lava Sword section.

### What only looks like it changes blocks

- Devil Trident deploy slam: trench, spike, and pillar visuals for 35 ticks over radius 5. Zero block changes.
- Ox King's Slam (KingsSlamEntity, normal and flawless): canyon trench and monolith visuals for 40 or 55 ticks over radius 10 or 20. Zero block changes; the entity contains no block code.
- Ox fissures: a 3.6 block trench visual and a damage zone for 100 ticks. Zero block changes.
- Evil Spear Hell Hunt impacts: trench and spike visuals for 35 ticks, up to 8 chained. Zero block changes.
- Spirit Sword cleave: the only projectile in the set that even acknowledges blocks, by dying on contact with no effect.

### What scars remain

- After a full-charge Ox Groundbreaker: permanently missing weak blocks along a sparsely sampled ring (about 48 of roughly 150 ring positions tested at full radius), plus their item drops on the ground. This is the only permanent trace any of the five weapons leaves in the world.
- After a Lava Sword fight: nothing, provided the server never crashed mid-burn. Cells restore individually, so during the 80 tick window the scar is a shrinking patchwork, oldest cells first.
- After everything else: nothing at all. Every other effect in the set is visual-only with lifetimes between 8 and 100 ticks.

### What happens to mobs physically, ranked

1. Ox Groundbreaker wave: launch of 1.49 to 3.2 horizontal and 0.64 to 1.3 vertical by charge, with BOUNCINESS 0.90 and AIR_DRAG halved, so targets bounce and carry. The strongest physical effect in the set.
2. Ox Flawless King's Slam: 2.5 horizontal, +0.9 vertical, instant at release across radius 20.
3. Ox normal slam: 1.5 horizontal, +0.5 vertical, instant across radius 10.
4. Ox stance aura: continuous outward push of 0.35 horizontal and +0.05 vertical per tick inside radius 12, for as long as the stance holds.
5. Evil Spear Impale projectile: vacuum pull of 0.8 toward the spear and +0.2 up on the pierced target, plus a 1.2 horizontal and +0.2 push on everything in the target's box inflated by 3.5.
6. Lava eruption: 0.45 forward, +0.28 up, once per target.
7. Devil Trident deploy slam: a downward add of -2.0 vertical. Targets are slammed into the ground, not lifted.
8. Evil Spear Hell Hunt: no mob movement at all. The ability moves the player between targets.
9. Spirit Sword (all abilities) and Lava cleave: no velocity applied to anything, ever. Spirit Sword's BOUNCINESS attributes change how targets interact with surfaces afterward, but nothing is pushed, pulled, lifted, or thrown at the moment of the hit.

No ability in any of the five weapons pulls mobs inward toward a gathering center and then lifts them, which is the reference video's signature mob behavior. The two inward forces that exist (the Evil Spear vacuum at 0.8, and nothing else) are single-target, instant, and immediately followed by an outward push.

---

## Cross-cutting summary

- Point 1 (layered particle depth): not met by any weapon. The set's effects are mesh-first: of the 16 renderers audited, only 4 use the additive second pass at all (SpiritImpale, TridentShard, DevilSlamShockwave, OxFissure). Particle emission everywhere is a trickle: the largest sustained emitter in the set is the Trident shard swarm at about 67 EMBER per second spread across 10 entities, and several flagship effects (Spirit cleave, Trident prong, both Lava effects, KingsSlam apart from its tick 1 burst) emit few or zero particles. No distance scaling exists anywhere.
- Point 2 (debris): one real system, SlamDebris, firing one burst on tick 1 of the Ox fissure and King's Slam only, capped at 140 spawn points. Everything else that looks like debris (Ox charge rocks, Evil Spear charge shards, Trident shards) is fixed formation geometry.
- Point 3 (world interaction): real but narrow. One destruction path (Ox wave at 95 percent charge, weak blocks, sparse ring sampling, permanent), one temporary change system (Lava burn, 80 ticks, crash-fragile), and a long list of ground-breaking visuals over untouched ground. Mob physics is launch-only; there is no gather, lift, and fling cycle anywhere.
- Point 4 (atmosphere): absent from all five weapons, entirely. No light, sky, haze, weather, or lightning code exists in this set.
- Point 5 (lifecycle): the discipline is uneven but present in places: OxShockwave and OxFissure fades, KingsSlam grow and fade, the Evil Spear projectile's matched end fade, and the Lava burn's cell-by-cell expiry are real build-up and dissipation work. The failures are at the seams: the Spirit Cannon and the Ox charge and stance visuals cut in or out in a single tick, and nothing in the set leaves a settling phase (debris rain, cooling scar, dying embers) after its main effect ends, except the Lava burn's 80 tick tail.
