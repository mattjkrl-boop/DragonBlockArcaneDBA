# Weapon Audit Group C (Nov 1, 2026)

Audit only, no code changed. Scope: SpiritSwordItem, EvilSpearItem, DevilTridentItem, SickleOfSorrowItem, OxKingsAxeItem, plus every entity, projectile, and effect class they reference. All damage numbers pass through `DbaEffectsConfig.capDamage(...)` at the call site (noted once here, not repeated per hit).

Shared drain math (`util/WeaponDrainHelper.java:16,22-46,89-113`): per-tick drain = maxKi (or maxStamina) x (percentPerMinute / 100) / 1200 per tick. Discrete drain = max resource x (percentPerMinute / 100) x (durationTicks / 1200). Percentages below are of MAX Ki/Stamina.

Basic attack swing trails for all five come from the shared profile table (`client/fx/WeaponTrailProfiles.java:45-50`), cited per weapon. All 14 entity types referenced below have a client renderer registered (`client/DragonBlockArcaneDBAClient.java:62,106,170,178,182,186,222,250,254,270,274,294,298,302`), so nothing here is invisible.

---

## 1. Spirit Sword (`item/SpiritSwordItem.java`, 258 lines)

Base stats (`SpiritSwordItem.java:45-60`): attack damage +499 (500 total), attack speed -1.0 (comment: 3.0 attacks/sec). Passive attribute riders while held (`:66-71`): nameplate distance +40 ("Divine Presence beacon"), air drag -0.55 multiplied base ("Celestial Glide"). No finisher mechanic exists for this weapon.

### Abilities

**Melee hit (left click, on-hit)** `hurtEnemy` `:83-125`
- Damage: 500 base attribute hit, plus Spirit Cleave = 2% of target max HP as magic damage (`:104-105`), plus Strength x 2.0 as player-attack damage (`:108-112`).
- Effects on target: SPIRIT_IMPALE 120 ticks (`:85`), CINEMATIC_TRACKING 120 ticks (`:86`), BOUNCINESS attribute modifier +0.85 (`:89-96`).
- Disarm: 20% chance per hit to drop the target's main-hand item and clear the slot (`:115-121`).
- Ki cost: discrete drain 55.0% per minute rate x 7 ticks = 0.32% of max Ki per hit (`:101`). No cooldown (attack speed only).
- Spawns: `SpiritImpaleEntity` at scale 1.0 on every hit (`:124`).
- Audio (`:128-134`): SoundEvents.TRIDENT_THUNDER (vol 1.3, pitch 1.15), BEACON_ACTIVATE (1.1, 1.35), DbaSounds.ABILITY_CHARGE (1.1, 1.35), all at the target.

**Spirit Cannon (right click, hold to channel)** `use` `:138-144`, `onUseTick` `:156-230`, `releaseUsing` `:234-257`
- Trigger: right click starts using (BOW use animation, max duration 72000, `:147-155`); requires `WeaponDrainHelper.hasKi` or the use fails (`:139-141`).
- Ki cost: smooth drain 55.0% of max Ki per minute while held; channel stops when Ki runs out (`:160-164`).
- Damage: pulse every 10 ticks (0.5s) along a 32.0 block ray (`PULSE_RANGE`, `:38`; pulse `:181-227`): 200.0 + Spirit x 1.5 player-attack damage, plus 2% of target max HP as magic damage, piercing ALL living entities whose inflated bounding box clips the ray (`:185-191`). Each hit also applies SPIRIT_IMPALE 100 ticks (`:203`), BOUNCINESS +0.80 (`:206-213`), and spawns a `SpiritImpaleEntity` at scale 0.75 (`:216`).
- Cooldown: 20 ticks (1s) applied on release (`:250`).
- Spawns: one persistent `SpiritCannonBeamEntity` maintained while channeling (`:168-179`).

### Referenced entities

**SpiritCannonBeamEntity** (`entity/SpiritCannonBeamEntity.java`, 125 lines): noPhysics Projectile that pins itself to the caster's eye, syncs yaw/pitch/length; server raycasts blocks each tick and clamps beam length to the block hit (max 32.0, `:27,96-113`); self-discards if the caster stops holding a Spirit Sword or stops using the item (`:115-121`). Visual per class doc (`:20-23`): 3D geometric beam with rotating helical drill spirals, orbital Ki ring nodes, muzzle/terminus geometry (client renderer).

**SpiritImpaleEntity** (`entity/SpiritImpaleEntity.java`, 99 lines): noPhysics Projectile, 28-tick lifetime (`:20-21,87-94`), follows its target entity each tick (`:79-85`). Visual per class doc (`:14-17`): 6 celestial swords, rotating runic judgement arrays, 3 ground seals, ascending divine pillar, crystalline shockwave spikes (client renderer).

### Current effects summary
- Swing trail: ARC, color 0x7DF9FF, reach 2.4, sweep 155 deg, fade 7 ticks, alpha 1.1, SPARK tip particles (`WeaponTrailProfiles.java:45`).
- Particles: none spawned in the item or either entity class server-side; all visuals are the two geometry entities plus the swing trail.
- Audio: beacon/trident themed set above; per-pulse BEACON_ACTIVATE at caster (`:226-227`), per-hit BEACON_POWER_SELECT (1.2, 1.5) (`:220-221`).
- Screen shake / hitstop: none anywhere for this weapon (no DbaImpactFx call).
- Shaders: none referenced.

### Gaps
1. No finisher and no charged move: the kit is melee + one channel beam only, the thinnest moveset of the five.
2. The beam is instant full power from the first tick: no charge-up, no ramp, no terminus impact particles or shake where it lands; block contact only shortens the geometry.
3. A full `SpiritImpaleEntity` (28-tick life) spawns on EVERY melee hit at 3.0 attacks/sec, and another per beam pulse per target: heavy overlapping entity churn for a single-target visual.
4. Disarm (20%) applies to any LivingEntity with no boss or player guard: it can strip a boss's or another player's held weapon outright.
5. Melee audio stack (TRIDENT_THUNDER + BEACON_ACTIVATE + ABILITY_CHARGE) is identical on every hit with fixed pitch: repetitive, and the trident/beacon sounds are vanilla reuse rather than a signature sword sound.

---

## 2. Evil Spear (`item/EvilSpearItem.java`, 312 lines)

Base stats (`EvilSpearItem.java:53-68`): attack damage +749 (750 total), attack speed -1.5. Passive riders (`:72-77`): nameplate distance -48 (stealth), air drag -0.40. Ki drain identity: 45.0% per minute.

### Abilities

**Melee hit (left click, on-hit)** `hurtEnemy` `:93-97`
- Damage: 750 base attribute hit only. No bonus damage, no effects, no mark.
- Ki cost: discrete drain 45.0 x 8 ticks = 0.30% of max Ki per hit. That is all a basic melee hit does.

**Evil Impale (left click, charged throw)** `onLeftClickChargeTick` `:103-129`, `onLeftClickRelease` `:131-160`; routed from `network/DbaNetwork.java:515-522` (cooldown-checked).
- Charge: hold left click up to MAX_LEFT_CHARGE_TICKS = 120 ticks / 6s (`:45`). While charging: smooth Ki drain 45.0%/min (= 0.0375%/tick, 4.5% of max Ki for a full charge; running out forces release, `:107-110`), horizontal velocity dampened to 0.5 each tick (`:116`), one `EvilSpearChargeEntity` maintained with synced charge ratio (`:120-127`).
- Release damage: 450.0 + (chargeRatio x 700.0) + (Strength x 3.0 x chargeRatio); chargeRatio is floored at 0.1 (`:142-146`). Projectile speed 1.8 + chargeRatio x 1.8 (`:149-152`).
- Cooldown: 20 + (int)(chargeRatio x 30) ticks = 20 to 50 ticks (`:157-158`).
- Audio: charge loop ENCHANTMENT_TABLE_USE every 20 ticks, pitch 0.6 + chargeRatio x 0.6 (`:124-127`); release TRIDENT_THROW (1.8, 0.7) (`:155-156`).
- Spawns: `EvilSpearChargeEntity` (charge), `EvilSpearProjectileEntity` (release).

**Hell Hunt (right click, lock-on chain)** `use` `:164-198`, `findLockOnTarget` `:200-224`, `performHellHunt` `:226-296`
- Trigger: right click; blocked while on cooldown or movement-immobilized (`:168-180`). Ki cost: discrete drain 45.0 x 40 ticks = 1.5% of max Ki, charged up front (`:174`).
- Lock-on: best look-dot target within 32.0 blocks, cone threshold dot > 0.5 (`:207-223`). Chain: initial target plus up to 7 more MARKED_BY_EVIL targets within 16 blocks of the previous one, 8 total (`:236-254`).
- Damage per chain step i: non-final 350.0 + Strength x 2.0; final 400.0 + (chain count x 150.0) + Strength x 3.5; each step multiplied by (1.0 + i x 0.15) (`:262-268`).
- Per target: teleport player onto the target via MovementPhysics.teleport (`:271`), MOVEMENT_CURSE 100 ticks (`:274`), CINEMATIC_TRACKING 100 ticks (`:275`), FRICTION x4.0 and BOUNCINESS +0.80 attribute modifiers (`:278-293`), spawn `HellHuntImpactEntity` radius 3.0 (4.5 and final flag on the last target) (`:298-301`).
- After the chain: DEMON_SURGE 40 ticks on the player (`:309`). Cooldown 100 ticks / 5s (`:312`).
- No-target fallback: forward lunge, velocity (look x 2.0, 0.4, look z 2.0) scaled by the movement limiter multiplier (`:189-195`), cooldown 40 ticks. Sounds PLAYER_ATTACK_SWEEP (1.5, 0.5) + DbaSounds.SLASH_WHOOSH (1.5, 0.5) (`:191-194`).
- Audio per chain hit: PLAYER_ATTACK_CRIT (1.8, 0.5) + DbaSounds.BLADE_SHING (1.8, 0.5) (`:304-305`).

### Referenced entities

**EvilSpearChargeEntity** (`entity/EvilSpearChargeEntity.java`, 92 lines): follows the caster, carries CHARGE_RATIO; self-discards after 6 ticks without an update or if the spear leaves the hand (`:76-90`). Visual per class doc (`:15-18`): demonic crimson ground seals, contracting energy torus rings, orbiting blood shards, pulsating core matrix.

**EvilSpearProjectileEntity** (`entity/EvilSpearProjectileEntity.java`, 165 lines): manual-position piercing projectile, 40-tick max life (`:156-158`), hitbox inflated 1.2 (`:92`). On each first hit per entity (`:96-154`): full stored damage (+30% if the target already has MARKED_BY_EVIL, `:100-102`), applies MARKED_BY_EVIL 160 ticks (`:108-110`), FRICTION x3.0 and AIR_DRAG x2.0 modifiers (`:114-127`), CINEMATIC_TRACKING 25 ticks (`:133-137`), inward vacuum pull 0.8 toward the spear (`:141-143`), a 3.5-block shockwave that pushes nearby enemies outward at 1.2 with no damage (`:146-152`), spawns `HellHuntImpactEntity` radius 2.6 non-final (`:149-152`), sound TRIDENT_HIT (1.8, 0.6) (`:154`).

**HellHuntImpactEntity** (`entity/HellHuntImpactEntity.java`, 73 lines): pure visual marker, 35-tick lifetime (`:20,64-71`), synced RADIUS and IS_FINAL. Visual per class doc (`:13-16`): ground-shatter fissure trenches, erupting blood/obsidian spikes, expanding shockwave seals, execution pillar (bigger on final). Shared by Hell Hunt and by spear projectile impacts.

### Current effects summary
- Swing trail: THRUST kind, color 0xB0003A, reach 2.7, fade 5 ticks, no tip particles (`WeaponTrailProfiles.java:47`).
- Particles: none in the item or its entities; visuals are geometry entities plus the thrust trail.
- Screen shake / hitstop / shaders: none referenced.

### Gaps
1. Basic melee is a dead move: 750 flat attribute damage and a Ki drain, nothing else. It does not even apply MARKED_BY_EVIL, so the spear cannot set up its own Hell Hunt chain in melee; marks come only from the thrown projectile (or from Devil Trident targeting, `DevilTridentItem.java:107`).
2. The projectile has NO block collision: noPhysics with manual position stepping (`EvilSpearProjectileEntity.java:76-84`) and no block clip anywhere, so it flies through walls for its full 40 ticks and can mark and damage targets through solid terrain.
3. Hell Hunt executes the entire chain in a single tick (loop at `EvilSpearItem.java:258-307`): every teleport, hit, and impact entity lands simultaneously, so there is no per-jump travel visual or sound stagger; the "chain execution" reads as one frame.
4. Chain order is arbitrary: the next marked target is `nearbyMarked.get(0)` from an unordered entity query (`:253`), and lock-on ties are first-found; no nearest-first or weakest-first logic.
5. The no-target lunge has no end state: no impact, no damage, no landing effect if it connects with an enemy mid-flight.
6. Charge audio is a single vanilla ENCHANTMENT_TABLE_USE loop; release and all impacts lean on vanilla trident sounds (TRIDENT_THROW, TRIDENT_HIT) shared with the Spirit Sword's melee stack.

---

## 3. Devil Trident (`item/DevilTridentItem.java`, 323 lines)

Base stats (`DevilTridentItem.java:27-42`): attack damage +749 (750 total), attack speed -2.2. Passive rider (`:46-48`): FRICTION x0.60 while held. State lives in the stack's CUSTOM_DATA: `isDeployed` boolean and `swarmTarget` UUID, set via `util/SwarmHelper.java:25-27` (deploy) and `:104-106` (recall).

### Abilities

**Three-Pronged Lasers (left click, not deployed)** `hurtEnemy` `:58-64` and `performLeftClickTargeting` `:66-119` route to `fireLasers` `:121-148`; air clicks routed from `network/DbaNetwork.java:479-481`.
- Damage: 3 x `KiBlastEntity`, each setDamage(250.0) = 750 total if all connect (`:132-133`), color 0xFF0000 (red), speed 2.5, spawned 0.25 blocks apart across the hand (`:129-144`).
- Ki cost: NONE. No WeaponDrainHelper call in the laser path. Cooldown: NONE; the DbaNetwork branch for DevilTridentItem (`DbaNetwork.java:479-481`) has no `isOnCooldown` check, unlike the Z Sword, Ox, Evil Spear, and most other branches in the same handler.
- Audio: none on firing. Impact audio comes from KiBlastEntity itself (below).

**Targeting (left click, deployed)** `performLeftClickTargeting` `:66-119`
- Raycast fallback: best target in a 64-block inflated box with look dot > 0.90 (`:77-101`); a physical melee hit supplies the target directly (`:60-62`).
- Applies MARKED_BY_EVIL 60 ticks (`:107`), stores the UUID as `swarmTarget` (`:105-106`). Audio: SoundEvents.ZOMBIE_VILLAGER_CONVERTED (0.5, 2.0) (`:109-110`). No Ki cost, no cooldown.

**Deploy (right click, not deployed)** `use` `:167-226` (deploy branch `:180-220`)
- Spawns `DevilSlamShockwaveEntity` radius 5.0 at the player (`:183-185`) and 10 x `TridentShardEntity` at player +2.0 Y (`:210-214`); `SwarmHelper.deploySwarm(stack, 10, 15000.0f)` sets isDeployed and per-shard health 15000 (`:216`).
- Slam damage: flat 750.0 to every LivingEntity in the player's bounding box inflated 5.0 (a box test, no distance falloff, `:188-208`), plus DEVILS_HANDS amplifier 2 for 300 ticks (`:193`), CINEMATIC_TRACKING 300 ticks (`:194`), BOUNCINESS +0.85 and AIR_DRAG -0.40 modifiers (`:197-208`), ground pull velocity -2.0 Y (`:211`).
- Audio: GENERIC_EXPLODE (1.2, 0.7) (`:187-189`). Cooldown: 20 ticks, described in code as a buffer before recall (`:218`). Ki cost: none up front; the swarm drains while deployed (below).

**Recall (right click, deployed)** `recallShards` `:150-157`
- Every owned shard in 64 blocks is set recalling, swarm state cleared, cooldown 3600 ticks = 3 minutes (`:156`). The same 3600-tick cooldown is applied on forced recall when Ki runs out (`:242-244` in manageShardSwarm).

**Shard swarm (passive, while deployed)** `manageShardSwarm` `:228-322`, ticked for every player from `DragonBlockArcaneDBA.java:249`
- Ki cost: smooth drain 20.0% of max Ki per minute while deployed; at zero Ki all shards are discarded, the swarm is recalled, and the 3600-tick cooldown is applied (`:236-247`).
- Defense: shards are reassigned to intercept any non-owned projectile within 12 blocks (`:262-271`).
- Offense: if a `swarmTarget` is set, ALL shards attack it (`:287-293`); otherwise shards are dealt out to every LivingEntity within 40 blocks, ceil(enemyHealth / 100) shards per enemy (`:296-311`). Leftover shards orbit (`:314-320`).
- Respawn: missing shards are recreated with their stored health (`:276-285`).

### Referenced entities

**KiBlastEntity** (`entity/KiBlastEntity.java`, 77 lines): the mod's generic shared ThrowableProjectile with synced COLOR and DAMAGE. On entity hit: indirectMagic damage, then explode (`:49-58`); on block hit: explode (`:61-66`). Explode audio: GENERIC_EXPLODE (0.8, 1.4) + LIGHTNING_BOLT_IMPACT (0.5, 1.6) (`:68-76`). No particles, no AoE damage: the "explosion" is sound only and the blast discards on first contact.

**TridentShardEntity** (`entity/TridentShardEntity.java`, 189 lines): noPhysics swarm shard, health 15000 (`:22`). Attack: homes at speed 2.5 (`:152-160`); within 2 blocks deals 100.0 flat damage (mobProjectile source) and applies DEVILS_HANDS 60 ticks, and loses 100 health per hit dealt (`:163-178`). Intercept: discards the incoming projectile and loses 50 health (`:74-81`). Damageable via hurtServer, dies at 0 health (`:88-99`). Recall: flies back at 1.8/tick and discards within 1 block (`:128-141`). Idle: orbits the owner at radius 2.2 (`:180-197`). Despawn if owner is dead, offline, or over 64 blocks away (`:120-126`). No sounds and no particles anywhere in the class.

**DevilSlamShockwaveEntity** (`entity/DevilSlamShockwaveEntity.java`, 63 lines): pure visual marker, 35-tick lifetime (`:20,55-62`), synced RADIUS. Visual per class doc (`:11-14`): demonic ground-shatter fissure trenches, jagged brimstone/obsidian spikes, expanding impact seals, vertical hellfire eruption column.

### Current effects summary
- Swing trail: THRUST kind, color 0xFF2244, reach 2.7, fade 5 ticks, no tip particles (`WeaponTrailProfiles.java:48`).
- The "lasers" are visually the generic Ki blast projectile tinted red; the only trident-specific geometry is the slam shockwave and the shard models.
- Screen shake / hitstop / shaders: none referenced, including on the 750-damage deploy slam.

### Gaps
1. BIGGEST GAP IN THE GROUP: the lasers are completely free and uncooled. 750 damage per click with no Ki cost and no cooldown check in either the item or the network handler (`DevilTridentItem.java:121-148`, `DbaNetwork.java:479-481`) makes every costed weapon move in this group look broken by comparison; this reads as unfinished balancing, not a design choice.
2. "Three-Pronged Lasers" are three parallel generic KiBlastEntity projectiles offset 0.25 blocks (`:129-144`): no beam, no trident-specific projectile model, no firing sound, and the shared explode audio (explosion + lightning) does not match a laser.
3. Shard damage is a flat 100.0 with no stat scaling (`TridentShardEntity.java:167`), while every other damage number in this group scales with Strength or Spirit; shard offense also targets ALL LivingEntities within 40 blocks including passive and friendly mobs (`DevilTridentItem.java:297-311`).
4. Deploy slam uses an inflated-box test with flat 750 and no falloff (`:188-191`), inconsistent with the radius-checked slams in OxKingsAxeItem.
5. Manual recall costs the same 3-minute cooldown as a forced Ki-starved recall (`:156` vs `:244`), punishing clean play identically to failure.
6. Targeting feedback is one borrowed vanilla sound (ZOMBIE_VILLAGER_CONVERTED at pitch 2.0); shards are completely silent when they hit, intercept, or die.

---

## 4. Sickle of Sorrow (`item/SickleOfSorrowItem.java`, 221 lines)

Base stats (`SickleOfSorrowItem.java:56-71`): attack damage +749 (750 total), attack speed -2.4. Passive riders (`:75-83`): nameplate distance -60, mini nameplate -10, air drag -0.60. Ki cost is technique-level based: `PlayerStats.getSickleBaseActionDrain(level)` = max(6.0, 25.0 - (level-1) x 2.0) % per minute, levels 1-10, so 25.0 at level 1 down to 7.0 at level 10 (`attribute/PlayerStats.java:370-373`). Below, costs are quoted at level 1 / level 10.

### Abilities

**Melee hit (left click, on-hit)** `hurtEnemy` `:97-101` -> `executeTechniqueMelee` `:103-134`
- Damage: 750 base attribute hit plus Strength x 3.0 bonus player-attack damage (`:121-125`).
- Effects: MELTING amplifier 2 (Melting III) 300 ticks / 15s (`:107`), SORROW_RIFT 80 ticks / 4s (`:110`).
- Soul Rend: heals the attacker 3% of (750 + Strength x 3) (`:128-130`).
- Ki cost: discrete drain baseDrain x 13 ticks = 0.27% of max Ki at level 1, 0.08% at level 10 (`:117`). Result of the drain is not checked (unlike the air swing).
- Spawns: `SorrowSlashEntity` impact burst ON the target with damage parameter 0.0 (damage already applied by the melee itself), alternating tilt (`:132-147`).
- Audio: none in the melee path itself.

**Sorrow Slash wave (left click, air swing)** `performSorrowSlash` `:150-152` -> `performTechniqueAirSwing` `:154-181`; routed from `network/DbaNetwork.java:443-446` (when the sickle technique is active) and `:532-534`.
- Damage: 750.0 + Strength x 3.0 carried by the wave (`:165-166`).
- Ki cost: discrete drain baseDrain x 8 ticks = 0.17% at level 1, 0.06% at level 10; swing aborts if the drain fails (`:160-162`). No item cooldown; the DbaNetwork branches have no isOnCooldown check, so swing rate is bounded by the client click payloads plus Ki.
- Spawns: `SorrowSlashEntity` flying wave at eye + look x 1.2, velocity look x 1.6 (`:168-180`).

**Dimensional Rift (right click)** `use` `:184-196`, `performTechniqueDimensionalRift` `:200-220`
- Damage: burst 500.0 + Spirit x 2.0 on first contact per target, plus the rift's own tick damage (entity below).
- Ki cost: discrete drain baseDrain x 60 ticks = 1.25% of max Ki at level 1, 0.35% at level 10 (`:210-212`).
- Cooldown: 60 ticks (3s) item cooldown (`use` `:191`) AND a second, redundant 60-tick gate in the static RIFT_COOLDOWNS map (`:198,202-206`).
- Spawns: `DimensionalRiftEntity` at the player's feet (`:214-219`).

### Referenced entities

**SorrowSlashEntity** (`entity/SorrowSlashEntity.java`, 142 lines): noPhysics crescent, synced TILT_RIGHT and IS_IMPACT_BURST. Lifetime 16 ticks (impact burst) / 30 ticks (wave) (`:137-140`); hitbox inflate 2.5 x 2.0 (burst) / 3.8 x 1.6 (wave) (`:102-104`). On hit (`:106-135`): stored damage, MELTING III 300 ticks, SORROW_RIFT 80 ticks, FRICTION x-0.60 modifier, Soul Rend heal of 3% of damage to the owner, sounds PLAYER_ATTACK_SWEEP (1.6, 0.5) + SOUL_ESCAPE (1.2, 0.7). Each entity hits each target once (hitEntityIds).

**DimensionalRiftEntity** (`entity/DimensionalRiftEntity.java`, 179 lines): 60-tick lifetime (`:29`); radius expands 3.0 -> 15.0 over 40 ticks, collapsing flag in the last 10 ticks (`:78-84`). Per target in radius (`:92-149`): gravity pull toward the center, strength max(0.3, 1.4 x (1 - dist/15)) with +0.15 Y (`:99-106`); MELTING with distance-based amplifier up to 9 (Melting X at point blank), duration 30 + amplifier x 3 seconds (`:109-111`); SORROW_RIFT 200 ticks and CINEMATIC_TRACKING 40 ticks (`:114-115`); FRICTION x-0.80 and AIR_DRAG x3.0 modifiers (`:118-131`); one-time burst damage (the item's 500 + Spirit x 2) with a 1%-of-burst Soul Rend heal per target (`:134-146`); plus 35.0 tick damage every 10 ticks (`:149-154`). Audio: tick 1 WARDEN_SONIC_BOOM (2.5, 0.6) + DbaSounds.WIND_GUST (2.5, 0.6) + RESPAWN_ANCHOR_SET_SPAWN (2.0, 0.5) (`:158-165`); PORTAL_AMBIENT every 20 ticks with rising pitch (`:167-170`); RESPAWN_ANCHOR_DEPLETE (2.0, 0.7) at expiry (`:172-175`). Visual per class doc (`:20-23`): concentric ground accretion rings, vertical spatial tears, 18 orbital void shards, singularity core.

### Current effects summary
- Swing trail: ARC, color 0x8B0000, reach 2.3, sweep 185 deg (widest in the group), fade 7 ticks, no tip particles (`WeaponTrailProfiles.java:49`).
- Particles: none. `SorrowSlashEntity.java:6` imports ParticleTypes and never uses it; the rift entity also spawns no particles. All visuals are geometry.
- Screen shake / hitstop / shaders: none referenced, even for the 15-block rift.

### Gaps
1. The melee impact slash is spawned with damage 0.0 (`SickleOfSorrowItem.java:141`), but SorrowSlashEntity still applies its full on-hit package to any OTHER entity in its 2.5-block hitbox: free Melting III + Sorrow Rift + friction distortion on bystanders, with a 0-damage Soul Rend heal. The burst is not purely cosmetic as intended.
2. `alternatingTilt` is a single static boolean shared by every player and both spawn paths (`SickleOfSorrowItem.java:44,132,168`): in multiplayer, one player's swing flips the next player's slash tilt. It should be per-player or per-swing random.
3. Neither the wave nor the melee path makes any sound at swing time; the only slash audio (PLAYER_ATTACK_SWEEP + SOUL_ESCAPE) plays inside the entity on a hit, so a missed air swing is silent, and the melee target hears the entity's hit sounds, not a weapon sound.
4. The wave has no block collision (noPhysics, manual stepping, `SorrowSlashEntity.java:91-93`): it passes through walls for 30 ticks x 1.6 speed (about 48 blocks), same wall-pierce issue as the Evil Spear projectile.
5. Double cooldown bookkeeping for the rift (item cooldown + static map) and the static RIFT_COOLDOWNS map is never cleaned up (`:198`): one stale entry per player UUID forever.
6. The rift's headline numbers are invisible to the player: Melting up to amplifier 9 for up to 57 seconds is applied by an entity whose burst already hit, with no per-target feedback sound or particle to show the tier.

---

## 5. Ox King's Axe (`item/OxKingsAxeItem.java`, 421 lines)

Base stats (`OxKingsAxeItem.java:70-85`): attack damage +949 (950 total, highest in the group), attack speed -2.8. Passive rider (`:89-91`): FRICTION x1.0 while held. This weapon drains STAMINA, not Ki: 125.0% per minute across all moves. A basic melee hit only drains stamina discretely (125.0 x 15 ticks = 1.56% of max stamina, `hurtEnemy` `:96-100`); all real moves are below.

### Abilities

**Groundbreaker (left click, charged)** `onLeftClickChargeTick` `:106-145`, `onLeftClickRelease` `:147-180`; routed from `network/DbaNetwork.java:486-497` (cooldown-checked).
- Charge: hold up to MAX_LEFT_CHARGE_TICKS = 200 ticks / 10s (`:59`). Smooth stamina drain 125.0%/min while charging (0.104%/tick, about 20.8% of max stamina for a full charge); running out forces release (`:111-115`).
- Rooting while charging: OX_BRACE amplifier 0 under 40 ticks, amplifier 1 from 40-99, and from 100 ticks full root via MovementPhysics.rootInPlace plus OX_BRACE amplifier 2 (`:121-129`). One `OxChargeEntity` is maintained with synced charge ratio (`:132-140`).
- Release damage: 350.0 + (chargeRatio x 850.0) + (Strength x 3.5 x chargeRatio), chargeRatio floored at 0.05 (`:160-163`), delivered by the shockwave entity.
- Cooldown: 20 + (int)(chargeRatio x 40) ticks = 20 to 60 ticks (`:177-178`). The network fallback path releases with a fixed 10 ticks of charge on a plain click (`DbaNetwork.java:495`), so there is no minimum-charge whiff state.
- Audio on release: PLAYER_ATTACK_SWEEP (2.0, 0.5), ANVIL_LAND (1.8, 0.6), GENERIC_EXPLODE (1.5, 0.8), plus DbaImpactFx.heavy (`:177-182`). Charge audio: BEACON_AMBIENT every 20 ticks, pitch 0.4 + chargeRatio x 0.4 (`:142-145`).

**Colossal Stance (right click, hold)** `use` `:184-190` (BLOCK animation, 72000 duration `:192-200`), `onUseTick` `:202-301`, `releaseUsing` `:303-416`
- Hold state: rooted in place every tick (`:213`), OX_BRACE amplifier 1 refreshed (`:216`), stamina drain 125.0%/min (`:207-210`).
- King's Force aura, 12 blocks (`:219-251`): every enemy gets FISSURE_STUN 20 ticks refreshed; every 5 ticks, pressure damage 45.0 + Strength x 0.4 and CINEMATIC_TRACKING 25 ticks; constant outward push 0.35 horizontal + 0.05 Y.
- Self pressure: from held tick 200 (10s), the holder takes 3% of CURRENT HP per second (applied per tick as health x 0.03 / 20, generic source) (`:266-269`).
- One `OxStanceAuraEntity` is maintained with held ticks and peak flag (`:254-263`).
- Peak window audio: WARDEN_SONIC_CHARGE (1.2, 1.8) every 5 ticks while held 280-300 ticks (`:272-278`).

**Normal Slam (release at 10-279 ticks, or 0.5s-13.95s)** `releaseUsing` `:379-414`
- Damage: 400.0 + (holdRatio x 900.0) + (Strength x 3.0 x holdRatio), holdRatio = min(1, heldTicks/200); radius 10 blocks with a real distance check (`:381-387,395-397`).
- Per target: FISSURE_STUN amplifier 0 for 80 ticks, CINEMATIC_TRACKING 40 ticks, MOVEMENT_CURSE amplifier 1 for 40 ticks, knockback 1.5 horizontal + 0.5 Y (`:398-408`).
- Spawns `KingsSlamEntity` radius 10, non-flawless (`:411-412`). Audio: GENERIC_EXPLODE (2.0, 0.7) + DbaImpactFx.heavy + ANVIL_LAND (1.5, 0.6) (`:402-405`). Cooldown 40 + (int)(holdRatio x 60) ticks = 40 to 100 ticks.

**Flawless King's Slam (release at 280-300 ticks, the 14.0s-15.0s peak window)** `releaseUsing` `:343-377`
- Damage: 2000.0 + Strength x 5.0 flat, radius 20 blocks (`:346-347`).
- Per target: FISSURE_STUN amplifier 1 for 140 ticks, CINEMATIC_TRACKING 60 ticks, MOVEMENT_CURSE amplifier 2 for 60 ticks, knockback 2.5 horizontal + 0.9 Y (`:358-367`).
- Spawns `KingsSlamEntity` radius 20, flawless (`:370`). Particles: EXPLOSION_EMITTER x3 (`:372`). Audio: GENERIC_EXPLODE (3.0, 0.5), WARDEN_SONIC_BOOM (2.5, 0.6), DbaSounds.WIND_GUST (2.5, 0.6) (`:360-366`). Cooldown 100 ticks / 5s (`:368`).

**Failure state (held past 300 ticks)** `onUseTick` `:281-300`
- Stance collapses: self recoil damage = 25% of the Flawless damage (2000.0 + Strength x 5.0) x 0.25, generic source (`:288-292`); stance entity discarded, use stopped.
- Audio: SHIELD_BREAK (2.5, 0.5) + GENERIC_EXPLODE (2.0, 0.6) + DbaImpactFx.heavy; particles EXPLOSION_EMITTER x2 (`:301-303`). Cooldown 200 ticks / 10s (`:306`).

### Referenced entities

**OxChargeEntity** (`entity/OxChargeEntity.java`, 92 lines): follows the caster, synced CHARGE_RATIO, self-discards after 6 ticks without update or if the axe leaves the hand (`:76-90`). Visual per class doc (`:15-17`): ground-shatter decal, branching magma fissure trenches, levitating vibrating rock debris, molten compression rings.

**OxShockwaveEntity** (`entity/OxShockwaveEntity.java`, 261 lines): the Groundbreaker payload. Max radius 4.0 + chargeRatio x 20.0 (up to 24 blocks; sub-waves x0.75), expansion about 0.8 blocks/tick (`:57-67`). Ring hit test (annulus currentRadius +/- 1.8, each entity once) deals the stored damage plus CINEMATIC_TRACKING 30 and MOVEMENT_CURSE amplifier 1 for 30 ticks, BOUNCINESS +0.90 and AIR_DRAG -0.50 modifiers, radial knockback 1.4 + charge x 1.8 horizontal and 0.6 + charge x 0.7 vertical, EXPLOSION particles x2 per target (`:177-241`). At charge >= 0.50 it spawns two echo sub-waves at ticks 6 and 12 dealing 40% damage (`:141-154`). At charge >= 0.95: EXPLOSION_EMITTER burst, destroys blocks with blast resistance <= 2.0 along the ring (drops enabled, BLOCK particles), and spawns 6 x `OxFissureEntity` at 50.0 damage/sec (`:122-139,157-169`). OxFissureEntity itself: 100-tick life, 25.0 damage every 10 ticks to enemies standing on it (`entity/OxFissureEntity.java:20-24,67-89`), plus the shared SlamDebris ground burst on its first tick.

**OxStanceAuraEntity** (`entity/OxStanceAuraEntity.java`, 100 lines): follows the caster, synced HELD_TICKS and IS_PEAK, self-discards when the caster stops using the axe or after 6 ticks without update (`:76-97`). Visual per class doc (`:15-17`): translucent titan aegis avatar, 12-block repulsion boundary disc, swirling heat dome.

**KingsSlamEntity** (`entity/KingsSlamEntity.java`, 73 lines): pure visual marker, lifetime 40 ticks (normal) / 55 ticks (flawless) (`:40-44`); on tick 1 it fires the shared `fx/SlamDebris.burst` with the KINGS_SLAM profile (real ground-block debris, dust, SPARK/EMBER accents) (`:64-67`). Visual per class doc (`:13-16`): ground shatter, radiating canyon trenches, basalt monoliths, volcanic shockwave dome; flawless adds magma geysers per the item doc.

**DbaImpactFx.heavy** (`fx/DbaImpactFx.java:16-20`): the group's only screen shake source: ScreenShakePayload intensity 2.2, 12 ticks, hitstop 3 for players within 8 blocks (28-block falloff radius), DbaSounds.HEAVY_IMPACT, plus a burst of 40 SPARK and 16 WIND particles scaled by effect density (`:23-29`).

### Current effects summary
- Swing trail: ARC, color 0xFF6A2B, reach 2.4, sweep 150 deg, fade 8 ticks, alpha 1.05, EMBER tip particles (`WeaponTrailProfiles.java:50`).
- This is the best-covered weapon in the group: geometry entities for charge, shockwave, stance, and slam; the only screen shake and hitstop (`DbaImpactFx.heavy` on Groundbreaker release, both slams, and failure); real particle use (EXPLOSION, EXPLOSION_EMITTER, BLOCK debris, SlamDebris).
- Shaders: none referenced.

### Gaps
1. `DbaImpactFx.heavy(...)` is called TWICE in the Flawless slam branch (`OxKingsAxeItem.java:361` and `:366`, interleaved between the sound calls): players get a doubled shake payload, doubled HEAVY_IMPACT sound, and doubled particle burst. This looks like a copy-paste slip, not a deliberate double impact; the Normal slam and failure branches call it once each (`:403`, `:303`).
2. Doc vs code drift in the class header (`:36-57`): the header promises Resistance IV and Slowness IV in stance; the code applies the custom OX_BRACE and FISSURE_STUN effects instead. Anyone tuning from the header will look for effects that are never applied.
3. The stance aura and both slams hit EVERY LivingEntity in range, including passive and friendly mobs (aura `:222-225`, slams `:352-355,389-392`): holding stance in a village or pen is a massacre, and there is no ally/pet exclusion anywhere in the weapon.
4. Charge audio is one vanilla BEACON_AMBIENT loop shared in spirit with the Spirit Sword's beacon-heavy stack; for a 10-second ground-shattering charge there is no rising rumble layer, cracking sounds, or a full-charge "ready" cue: the only peak feedback in the whole weapon is WARDEN_SONIC_CHARGE inside the stance window.
5. Groundbreaker has no caster feedback at release tiers: a 0.05-charge release and a 0.95-charge release play the same three sounds at the same volumes; only the shockwave entity's charge ratio differentiates them visually.

---

## Cross-cutting notes

- Damage capping: every damage call in all five weapons routes through `DbaEffectsConfig.capDamage`, so headline numbers above are pre-cap.
- Screen shake and hitstop exist ONLY in OxKingsAxeItem (via DbaImpactFx.heavy). Spirit Sword, Evil Spear, Devil Trident, and Sickle of Sorrow have none in their item or entity code, despite several 750-plus damage hits and a 15-block rift.
- No weapon in this group references the Phase 2/7 post shader chains directly; all "shader" feel is geometry entities, particles, and swing trails.
- Vanilla sound reuse is heavy across the group: GENERIC_EXPLODE appears in Devil Trident (deploy + Ki blast), Ox (release, slams, failure); BEACON sounds carry both Spirit Sword identity and Ox charge ambience; TRIDENT sounds carry both Spirit Sword melee and Evil Spear throw/impact.
- Wall-piercing projectiles: EvilSpearProjectileEntity and SorrowSlashEntity both step manually with noPhysics and no block collision, so both damage through walls for their full lifetimes.
