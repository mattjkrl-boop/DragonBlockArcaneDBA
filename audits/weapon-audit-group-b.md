# Weapon Audit Group B (Nov 1, 2026)

Audit only, no code changed. Scope: AzureDragonSwordItem, DaburaSwordItem, CurseBladeItem, HollowsEdgeItem, DimensionalSwordItem, plus every entity, effect, and helper class they reference.

Shared facts that apply to all five sections:

- Left-click abilities are not vanilla attack handling. They are dispatched server side from the C2SWeaponLeftClickPayload receiver in network/DbaNetwork.java:439-527 (charge tick while held, release on let go, default action on a plain click).
- Ki and stamina costs use util/WeaponDrainHelper.java. Per-tick drain is max stat times percent-per-minute divided by 1200 (WeaponDrainHelper.java:22-44). Discrete drain is max stat times percent-per-minute times durationTicks divided by 1200 (WeaponDrainHelper.java:108-128). Costs below are stated in those exact terms.
- Every damage number below passes through DbaEffectsConfig.capDamage, which clamps to the config value maxWeaponDamage when that is above 0 (config/DbaEffectsConfig.java:55-58).
- Basic swing visuals come from the one profile table in client/fx/WeaponTrailProfiles.java (the Phase 1 attack visuals rebuild). Each weapon section lists its own profile line.
- Screen shake and hitstop exist only where fx/DbaImpactFx.heavy is called. That helper sends shake intensity 2.2 for 12 ticks with hitstop 3 inside 8 blocks, a SPARK (40) plus WIND (16) particle burst, and the HEAVY_IMPACT sound (fx/DbaImpactFx.java:16-29). Only Azure (quake) and Dabura (wave release, eclipse) call it in this group. Curse, Hollows Edge, and Dimensional Sword have no shake or hitstop anywhere in their kits.
- No weapon specific post-processing shader is triggered by any of the five items. The DbaPostFx chains are form, charge, and low-health driven, not weapon driven.

---

## 1. Azure Dragon Sword

Source: item/AzureDragonSwordItem.java (361 lines). Theme: wind, flight, tempest.

### Base stats and passives

- Melee damage 800 (modifier 799.0, AzureDragonSwordItem.java:67), attack speed modifier -2.0 (swift).
- No hurtEnemy override: basic melee hits apply no on-hit effect and no drain. Plain damage only.
- Passives while held (AzureDragonSwordItem.java:83-88): AIR_DRAG -0.65 (ADD_MULTIPLIED_BASE), BOUNCINESS +0.30.
- Swing trail profile (WeaponTrailProfiles.java:43): color 0x3FA9FF, ARC, reach 2.15, sweep 150 degrees, fade 6 ticks, SPARK tip particles.

### Ability 1: Azure Dragon Rush (left click, held, in air)

- Trigger: left-click payload ticks while held; the release action stops the rush (network/DbaNetwork.java:469-475). Handler onDragonRushTick, AzureDragonSwordItem.java:98.
- Flight: forces fall-flying pose, velocity blended through MovementPhysics.applyFlightVelocity at blend 0.40 toward look direction. Speed 1.85 normal, 0.75 while sneaking, 3.7 on double rush (AzureDragonSwordItem.java:160-168). Double rush triggers when a new rush starts within 400 ms of the last one (AzureDragonSwordItem.java:108-115).
- Ki cost: smooth drain 20 percent of max Ki per minute, 40 percent per minute while double rushing (AzureDragonSwordItem.java:125-131). On Ki empty: rush stops and the item takes a 20 tick cooldown (AzureDragonSwordItem.java:120-123).
- Pass-through damage: enemies in a box inflated 2.5 blocks (4.0 on double rush) take 350 + Strength x 2.0, or 600 + Strength x 2.0 on double rush, plus a wind push of 1.5 (2.5 double) (AzureDragonSwordItem.java:171-183).
- Mini tornado: 40 percent chance per enemy passed (80 percent double rush) to spawn AzureTornadoEntity scale 1.0 at the enemy (AzureDragonSwordItem.java:186-189). Mini tornado lives 60 ticks, vortex radius 3.0, deals 25 damage every 10 ticks (entity/AzureTornadoEntity.java:27, 41, 88-95).
- Fall damage: fallDistance zeroed during and after the rush, so landing is free (AzureDragonSwordItem.java:169, 202-211).
- Entities spawned per tick: AzureRushTrailEntity at the player (AzureDragonSwordItem.java:172-175), a pure visual shell with a 12 tick life (entity/AzureRushTrailEntity.java:21).
- Audio: SoundEvents.ELYTRA_FLYING every 15 ticks, every 8 ticks on double rush at higher pitch (AzureDragonSwordItem.java:195-198). No sound, shake, or hitstop on the pass-through hits themselves.

### Ability 2: Sonic Quake (automatic finisher of the rush)

- Trigger: automatic when the rushing player touches ground or collides horizontally (AzureDragonSwordItem.java:187-193). Ends double rush.
- Damage: 450 + Strength x 2.5, radius 8.0, targets launched +1.4 vertical velocity and given a BOUNCINESS +0.85 modifier (AzureDragonSwordItem.java:217-235).
- Entity: AzureSonicQuakeEntity, an 18 tick visual shockwave shell (entity/AzureSonicQuakeEntity.java:19).
- Audio and FX: GENERIC_EXPLODE plus DbaImpactFx.heavy, so this is one of the few moves in the group with real screen shake and hitstop (AzureDragonSwordItem.java:237-240).

### Ability 3: Call of the Tempest (right click, charged channel)

- Trigger: right click hold (use, AzureDragonSwordItem.java:249), gated on hasKi. Use animation BOW, duration 72000.
- Ki cost: smooth drain 20 percent of max Ki per minute while channeling (AzureDragonSwordItem.java:272-276).
- Charge: up to MAX_TEMPEST_CHARGE_TICKS 300 ticks (15 seconds) (AzureDragonSwordItem.java:50). Channel visual is AzureTempestChannelEntity, a follow-the-caster shell whose synced charge ratio drives its renderer (entity/AzureTempestChannelEntity.java). Charge audio every 20 ticks: LIGHTNING_BOLT_THUNDER, DbaSounds.LIGHTNING_ZAP, and ELYTRA_FLYING, pitch rising with charge (AzureDragonSwordItem.java:298-305).
- Release (releaseUsing, AzureDragonSwordItem.java:307): needs at least 10 held ticks. Storm radius 10 + chargeRatio x 20 (10 to 30 blocks). Max charge at ratio 0.95 or above.
- Entity: AzureStormEntity at the player, or at the locked target if one exists (AzureDragonSwordItem.java:327-341). Lock-on is set in DragonBlockArcaneDBA.java:204-206 when the holder attacks or interacts with a living entity while holding the sword, and read back on release.
- Storm behavior (entity/AzureStormEntity.java): lifetime 220 ticks (11 seconds, line 30). Follows the player or the locked target. Wind shove on all enemies in radius every 4 ticks (lines 108-116). Every 25 ticks a lightning strike on a random enemy inside: AzureLightningEntity with damage 450, 20 percent chance of the rare variant (lines 118-133). AzureLightningEntity applies its damage in a 3.5 block box on its first tick and emits a 20 count SPARK burst (entity/AzureLightningEntity.java:58-80).
- Max charge bonus: the storm spawns one giant AzureTornadoEntity, scale 1.8 (entity/AzureStormEntity.java:100-105). Giant tornado: 200 tick life, radius 14.4, 60 damage every 10 ticks, and its own AzureLightningEntity strikes at 350 damage every 20 ticks (entity/AzureTornadoEntity.java:41, 88-110).
- Cooldown: 60 + chargeRatio x 60 ticks (60 to 120) on the sword (AzureDragonSwordItem.java:349).
- Audio on release: LIGHTNING_BOLT_IMPACT plus an action bar message with the radius (AzureDragonSwordItem.java:343-346).

### Gaps

- Basic melee is bare: no on-hit effect, no drain, nothing that says wind or dragon. Only the swing trail profile carries the identity.
- Rush pass-through hits are silent and weightless: up to 600 + Strength x 2.0 damage per enemy with no hit sound, shake, or particles at the victim.
- Lock-on has no feedback and goes stale: the target map is only written on attack or interact and only cleaned lazily on release or disconnect (AzureDragonSwordItem.java:351-356), so a storm can silently follow a target the player locked long ago instead of following the player.
- AzureStormEntity.isMaxCharged is a plain non-synced, non-persisted field (entity/AzureStormEntity.java:31-32), so a storm that survives a chunk unload or relog loses its max-charge flag state.
- The item file still imports DustParticleOptions, ParticleTypes, MobEffects, BlockPos, and PlayerStats without using them (AzureDragonSwordItem.java:3, 13-14, 21, 36 in the import block), leftovers from a removed particle implementation. The kit now leans almost entirely on entity renderers for visuals.
- Summoning a 30 block storm domain has no screen shake and no dedicated impact moment: one thunder sound and a chat line.

---

## 2. Dabura Sword (Darkness Sword)

Source: item/DaburaSwordItem.java (329 lines). Theme: abyssal darkness, petrification.

### Base stats and passives

- Melee damage 800 (modifier 799.0, DaburaSwordItem.java:59), attack speed modifier -2.4.
- hurtEnemy (DaburaSwordItem.java:88-92): drains the wielder's Ki discretely at 35 percent per minute over 10 ticks (about 0.29 percent of max Ki per hit). The drain result is ignored and nothing is applied to the target.
- Passives while held (DaburaSwordItem.java:74-79): NAME_PLATE_DIST -64 (nameplate stealth), FRICTION +0.50.
- Swing trail profile (WeaponTrailProfiles.java:44): color 0xC23BFF, ARC, reach 2.05, sweep 145 degrees, fade 6 ticks, VOID_SMOKE tip.

### Ability 1: Abyssal Slash (left click, charged)

- Trigger: left-click charge ticks then release (network/DbaNetwork.java, Dabura branch of the payload receiver). Charge handler onLeftClickChargeTick, DaburaSwordItem.java:96. Max charge MAX_LEFT_CHARGE_TICKS 160 ticks (8 seconds, DaburaSwordItem.java:46).
- Ki cost: smooth drain 35 percent of max Ki per minute while charging (DaburaSwordItem.java:99-103). Running dry force-releases the slash.
- While charging: movement dampened to 0.4 of horizontal velocity per tick (DaburaSwordItem.java:107), and DarknessChargeEntity is spawned and fed the charge ratio (DaburaSwordItem.java:114-121). The charge entity is a synced shell (entity/DarknessChargeEntity.java) whose renderer draws the dark aura, tendrils, and orbiting prisms. Charge audio: BEACON_AMBIENT every 20 ticks, pitch 0.4 + ratio x 0.5 (DaburaSwordItem.java:124-127).
- Release (onLeftClickRelease, DaburaSwordItem.java:129): damage = 400 + chargeRatio x 800 + Strength x 3.0 x chargeRatio, with chargeRatio floored at 0.1, so about 480 + Strength x 0.3 at a tap and 1200 + Strength x 3.0 at full charge (DaburaSwordItem.java:139-145).
- Projectile: DarknessWaveEntity at speed 1.6 + chargeRatio x 1.0 (DaburaSwordItem.java:149-155). The wave pierces everything, passes through terrain, lives 40 ticks, hitbox inflated 3.5 wide (entity/DarknessWaveEntity.java:104-110, 148-151). On hit it applies its damage once per entity, Petrification Curse 100 ticks amplifier 0, Dark Faded 100 ticks amplifier 0, AIR_DRAG +3.0 and FRICTION +2.5 modifiers, Cinematic Tracking 25 ticks, knockback 1.2, and a per-hit PLAYER_ATTACK_SWEEP sound (entity/DarknessWaveEntity.java:117-141).
- Full charge bonus (ratio 0.95 or above): two extra DarknessWaveEntity projectiles at 50 percent damage, flagged secondary (narrower 2.2 hitbox) (DaburaSwordItem.java:158-172).
- Cooldown: 20 + chargeRatio x 40 ticks (about 22 to 60) (DaburaSwordItem.java:176-177).
- Release audio and FX: PLAYER_ATTACK_SWEEP, GENERIC_EXPLODE, and DbaImpactFx.heavy (shake, hitstop, spark and wind burst) at the caster (DaburaSwordItem.java:189-195).

### Ability 2: World of Darkness (right click, channeled domain)

- Trigger: right click hold (use, DaburaSwordItem.java:182-188), gated on hasKi. Use animation BLOCK.
- Ki cost: smooth drain 35 percent of max Ki per minute (DaburaSwordItem.java:199-203).
- Channel: domain radius grows 8 + chargeRatio x 22 (8 to 30 blocks) over MAX_RIGHT_CHANNEL_TICKS 300 ticks (15 seconds, DaburaSwordItem.java:47, 212). DarknessDomainEntity is spawned and refreshed each tick (DaburaSwordItem.java:215-228); it is a synced radius shell (entity/DarknessDomainEntity.java) rendered as the void dome.
- While channeling: every enemy inside gets Petrification Curse 40 ticks amplifier 0 refreshed continuously (DaburaSwordItem.java:236-240). Petrification Curse itself is -80 percent movement speed, -50 percent attack damage and attack speed (effect/PetrificationCurseEffect.java). Hostile projectiles inside are slowed to 5 percent velocity (DaburaSwordItem.java:243-250).
- Audio while channeling: PORTAL_AMBIENT every 30 ticks, pitch rising with charge (DaburaSwordItem.java:253-256).

### Ability 3: Abyssal Eclipse (finisher, auto at full channel)

- Trigger: automatic at exactly 300 held ticks (DaburaSwordItem.java:261-264), then the channel stops. Handler triggerAbyssalEclipse, DaburaSwordItem.java:268.
- Burst damage: 1200 + Strength x 4.0 to every enemy currently in the domain list (DaburaSwordItem.java:272, 281). Enemies are popped +0.4 vertical, Petrification Curse amplifier 1 for 60 ticks, Cinematic Tracking 60 ticks.
- Blade rain: 10 DarknessBladeEntity spawn 18 to 23 blocks above random points in the domain at 350 damage each (DaburaSwordItem.java:285-296). Each blade falls at -1.8 per tick and detonates on ground contact or after 40 ticks: splash box inflated 4.0, spawns a DarknessShatterEntity of radius 2.8, GENERIC_EXPLODE sound (entity/DarknessBladeEntity.java:49-72).
- Ground shatter: one DarknessShatterEntity at the player with radius min(12, domain radius x 0.5) (DaburaSwordItem.java:292-296). The shatter entity is a 40 tick visual shell (entity/DarknessShatterEntity.java:22).
- Audio and FX: WARDEN_SONIC_BOOM, DbaSounds.WIND_GUST, and DbaImpactFx.heavy (DaburaSwordItem.java:298-303).
- Cooldown: 100 ticks after the eclipse (DaburaSwordItem.java:309). Releasing the channel early instead costs a 40 tick cooldown and gives nothing (DaburaSwordItem.java:313-320).

### Gaps

- The two full-charge bonus waves are not actually delayed. The code schedules them with level.getServer().execute (DaburaSwordItem.java:160-172), which runs on the next server tick for both; the 4 and 8 tick "delays" only shift the spawn position backward. In game the three waves leave almost together.
- Basic melee applies nothing to the target: no curse, no darkness, despite the weapon being the petrification sword. The curse only ever comes from the wave and the domain.
- The eclipse is all or nothing at exactly tick 300. Interrupt at 299 ticks and the entire 15 second channel pays out zero, with only a 40 tick cooldown as feedback.
- DarknessDomainEntity starts with lifetime 40 but refreshLifetime resets it to 10 (entity/DarknessDomainEntity.java:26, 67-69). Works, but the mismatch shows the class was copied and trimmed without cleanup. The same pattern is in Curse Blade's domain class.
- Release and eclipse impact feedback is the shared DbaImpactFx.heavy package, identical shake, particles, and sound to Azure's Sonic Quake and every other heavy impact in the mod. The signature finisher of the darkness sword lands with the generic impact.

---

## 3. Curse Blade (class CurseBladeItem)

Source: item/CurseBladeItem.java (369 lines). Theme: curse chains, corrupted storm.

### Base stats and passives

- Melee damage 750 (modifier 749.0, CurseBladeItem.java:48), attack speed modifier -2.0.
- hurtEnemy (CurseBladeItem.java:79-83): drains the wielder's Ki discretely at 30 percent per minute over 10 ticks (0.25 percent of max Ki per hit), result ignored, nothing applied to the target.
- Passives while held (CurseBladeItem.java:62-67): NAME_PLATE_DIST -64, MINI_NAME_PLATE_DIST -10.
- Swing trail profile (WeaponTrailProfiles.java:45): color 0x8A2BE2, ARC, reach 2.05, sweep 140 degrees, fade 7 ticks, VOID_SMOKE tip.

### Ability 1: Curse Chain Streaming (left click, per click)

- Trigger: each left-click payload fires one chain (network/DbaNetwork.java:458). Handler streamCurseChain, CurseBladeItem.java:87.
- Ki cost: discrete drain 30 percent per minute over 4 ticks, about 0.1 percent of max Ki per chain, checked (no Ki, no chain) (CurseBladeItem.java:90-92). No cooldown.
- Targeting: best target in the crosshair cone within 24 blocks (dot product above 0.3), otherwise the nearest living entity in the box (CurseBladeItem.java:97-124).
- Entity: CurseChainEntity with orbit index equal to the count of already attached chains modulo 10 (CurseBladeItem.java:127-137). Launch sound CHAIN_PLACE at a randomized pitch (CurseBladeItem.java:140-141).
- Chain behavior (entity/CurseChainEntity.java): homes at 1.6 blocks per tick (lines 234-237). It is attackable with 60 HP and pickable (lines 28, 98-100). On attach (lines 178-238):
  - Movement Curse +1 stack (amplifier cap 9, so 10 stacks), 100 tick duration. Each stack is -10 percent movement speed and jump; at 10 stacks the target is fully frozen (effect/MovementCurseEffect.java).
  - FRICTION and AIR_DRAG modifiers scaled 1.0 + amplifier x 0.8.
  - 35 magic damage on attach (line 212).
  - Lifesteal: heals the owner 2.0 + amplifier x 1.5, or +4 absorption if already at full health (lines 215-224).
  - At 5 or more stacks: pulls the target toward the owner and restores 10 Ki and 5 stamina to the owner (lines 227-238).
  - Attached chains orbit the target's body for 100 ticks (lines 241-260). If a target already has 10 chains, a new chain retargets to a nearby enemy or replaces the weakest chain (lines 152-176). Breaking a chain removes one curse stack (lines 106-124).

### Ability 2: Abyssal Eclipse (right click, channeled storm domain)

- Trigger: right click hold (use, CurseBladeItem.java:155-161), gated on hasKi. Use animation BOW. Header comment at CurseBladeItem.java:152.
- Ki cost: smooth drain 30 percent of max Ki per minute (CurseBladeItem.java:171-175).
- Channel: up to MAX_ECLIPSE_CHANNEL_TICKS 300 ticks (15 seconds, CurseBladeItem.java:40). Domain radius 10 + stormRatio x 20 (10 to 30 blocks) (CurseBladeItem.java:178-179). AbyssalDomainEntity spawned and refreshed per tick (CurseBladeItem.java:182-195).
- Domain effects per tick on enemies inside (CurseBladeItem.java:198-233):
  - Storm of Darkness 60 ticks, amplifier scaling 0 to 3 with storm ratio, plus Cinematic Tracking 60 ticks. Storm of Darkness reduces attack and movement speed (effect/StormOfDarknessEffect.java).
  - Wind toss every 5 ticks at 60 percent chance: random horizontal push plus +0.6 vertical.
  - Disarm: every 20 ticks, 5 percent chance per enemy to knock the main-hand item out as a dropped item with 40 tick pickup delay (Curse Blade itself is exempt). ITEM_BREAK sound.
- Targeted lightning (CurseBladeItem.java:236-281): targets the enemy with the most Movement Curse stacks. Every 40 ticks a CurseTelegraphEntity (radius 1.8, 20 ticks, entity/CurseTelegraphEntity.java) marks the spot; 20 ticks later a CurseLightningEntity strikes: flat 450 damage (no Strength scaling), ignite 5 seconds, and a real FIRE block placed on the ground. Sounds LIGHTNING_BOLT_THUNDER, DbaSounds.LIGHTNING_ZAP, LIGHTNING_BOLT_IMPACT. 15 percent chance of the rare visual variant.
- Ambient lightning (CurseBladeItem.java:285-316): 12 percent chance per tick, a CurseLightningEntity at a random point in a 60 by 60 block box around the player at surface height, regardless of the domain radius. Entities within 3 blocks take 15 damage and ignite 5 seconds; a FIRE block is placed. Thunder sounds at reduced volume. 20 percent rare variant.
- Sky cracks: SkyCracksEntity spawned every 80 ticks at fixed height y=280 above the player (CurseBladeItem.java:319-325); the entity is a 100 tick visual shell (entity/SkyCracksEntity.java:9).
- Max charge payoff (held 300+ ticks, every 20 ticks, CurseBladeItem.java:328-352): enemies with Movement Curse amplifier 4 or higher (5+ stacks) are slammed down at -2.5 vertical velocity, take flat 150 damage, are re-cursed at amplifier 9 for 60 ticks (full root), and get a CurseGroundShatterEntity of radius 3.5 at their feet (a 35 tick visual shell, entity/CurseGroundShatterEntity.java:20).
- Ambient audio: LIGHTNING_BOLT_IMPACT every 40 ticks (CurseBladeItem.java:355-358).
- Cooldown on release: 40 + stormRatio x 60 ticks (40 to 100) (CurseBladeItem.java:361-366).
- Important: CurseLightningEntity itself deals no damage. It is a 20 tick visual shell (entity/CurseLightningEntity.java:13-43); all strike damage is applied in the item code above.

### Gaps

- Name collision: this right click is also called "Abyssal Eclipse" (CurseBladeItem.java:152), the exact name of Dabura Sword's finisher, for a completely different mechanic. Two weapons, one signature move name.
- The domain visual is a recolor of Dabura's. AbyssalDomainEntity is a field-for-field copy of DarknessDomainEntity, and client AbyssalDomainRenderer is the same 313 lines as DarknessDomainRenderer with the red and blue channel emphasis swapped and slightly faster rotation bands (verified by diff). Curse Blade's signature domain reads as Dabura's domain with the colors flipped.
- Ambient lightning ignores the domain it belongs to: strikes land anywhere in a 60 block box even at minimum channel (10 block domain), and both ambient and targeted strikes place real FIRE blocks in the world, which can spread and grief builds.
- Flat numbers where the rest of the mod scales: targeted lightning is flat 450 and the max-charge slam is flat 150, neither scales with Strength, unlike Dabura's and Azure's formulas.
- No screen shake or hitstop anywhere in the kit. A 450 damage telegraphed lightning strike and a ground slam land with sound only; there is not one DbaImpactFx call in the item or any of its entities.
- Chain streaming is nearly free and cooldown-less (0.1 percent max Ki per chain), so the intended one-chain-at-a-time curse building can be click-spammed to 10 stacks almost instantly.

---

## 4. Hollow's Edge

Source: item/HollowsEdgeItem.java (391 lines). Theme: void assassin, blink dashes.

### Base stats and passives

- Melee damage 850 (modifier 849.0, HollowsEdgeItem.java:70), the highest base of the five. Attack speed modifier -1.8.
- No hurtEnemy override: basic melee applies no effect, no drain, nothing.
- Passives while held (HollowsEdgeItem.java:84-92): NAME_PLATE_DIST -64, MINI_NAME_PLATE_DIST -10, FRICTION -0.65 (glide).
- Swing trail profile (WeaponTrailProfiles.java:46): color 0x5A5A7A, ARC, reach 2.05, sweep 150 degrees, fade 6 ticks, tip NONE. It is the only one of the five with no tip particle.

### Ability 1: Hollow Rush charge (left click, charged)

- Trigger: left-click charge ticks (network/DbaNetwork.java:460-464). Handler onLeftClickChargeTick, HollowsEdgeItem.java:103. Max charge MAX_LEFT_CHARGE_TICKS 100 ticks (5 seconds, HollowsEdgeItem.java:50).
- Ki cost: smooth drain 30 percent of max Ki per minute while charging (HollowsEdgeItem.java:106-110). Running dry force-releases.
- While charging: movement dampened to 0.3 of horizontal velocity (HollowsEdgeItem.java:115), HollowChargeEntity spawned and fed the charge ratio (HollowsEdgeItem.java:121-128), a synced shell in the same pattern as the other charge entities (entity/HollowChargeEntity.java). Audio: ENDERMAN_AMBIENT every 20 ticks, pitch 0.5 + ratio x 0.6 (HollowsEdgeItem.java:130-133).
- Release (onLeftClickRelease, HollowsEdgeItem.java:136): opens the rush window for max(40, chargeRatio x 200) ticks, 2 to 10 seconds (HollowsEdgeItem.java:138-141), resets the dash counter, discards the charge entity, shows an action bar message, plays WARDEN_HEARTBEAT (HollowsEdgeItem.java:149-158). No cooldown is applied.

### Ability 2: Blink Dash (left click during the window, or any left click)

- Trigger: left-click default action (network/DbaNetwork.java:466). Handler onLeftClickDash, HollowsEdgeItem.java:161. Inside the window the blink is 5.0 blocks; outside the window it falls back to a free 3.0 block blink (HollowsEdgeItem.java:165-178). Holding backward dashes backward.
- Every 3rd dash inside the window is empowered (dash count modulo 3, HollowsEdgeItem.java:175-177).
- Cost and cooldown: none. performBlink (HollowsEdgeItem.java:181) contains no Ki drain and no cooldown call in either mode. Ki is only ever spent during the charge.
- Blink mechanics: raycast with a phase-through rule (if the 1.5 blocks past an obstacle are air, the dash passes through), ground snapping (HollowsEdgeItem.java:197-224), teleport via MovementPhysics.teleport (HollowsEdgeItem.java:232).
- Entities per blink: HollowAfterimageEntity at the start point (HollowsEdgeItem.java:226-228), which lives 100 ticks and pulses Dark Faded 60 ticks onto enemies within 4 blocks every 5 ticks (entity/HollowAfterimageEntity.java:94-108); and HollowRushTrailEntity spanning start to destination, a 14 tick visual shell that knows if it was a 3rd dash (entity/HollowRushTrailEntity.java:24, 40-47).
- On blink: the player gains HOLLOWED for 60 ticks (HollowsEdgeItem.java:235). Hollowed makes the player about 90 percent translucent, +60 percent movement speed, untargetable, with void smoke particles (effect/HollowedEffect.java).
- Path damage: enemies in the swept box take 450 + Strength x 2.5, multiplied by 1.8 on the 3rd dash, plus Dark Faded 80 ticks and Cinematic Tracking 80 ticks (HollowsEdgeItem.java:241-254).
- 3rd dash bonus: VoidSlashEntity fired at path damage x 1.5, speed 1.8 (HollowsEdgeItem.java:256-265). The slash pierces, lives 40 ticks, hitbox inflated 2.5, applies Dark Faded amplifier 1 for 80 ticks on hit (entity/VoidSlashEntity.java:66-89). Audio: PLAYER_ATTACK_SWEEP and DbaSounds.SLASH_WHOOSH.
- Blink audio at destination: ENDERMAN_TELEPORT and DbaSounds.DARK_PULSE (HollowsEdgeItem.java:267-270).

### Ability 3: Void Rift (right click, channeled)

- Trigger: right click hold (use, HollowsEdgeItem.java:276-303), gated on hasKi. Use animation BOW. On press, a VoidRiftEntity is spawned at the crosshair, 12 blocks out or at a block hit up to 32 blocks, with radius 2.5 and damage 550.
- Ki cost: smooth drain 30 percent of max Ki per minute while held (HollowsEdgeItem.java:318-322).
- While held (onUseTick, HollowsEdgeItem.java:313-350): the rift lerps toward the crosshair (factor 0.35 per tick) and its radius grows 2.5 + chargeRatio x 6.5, up to 9.0 blocks over 5 seconds.
- Rift behavior (entity/VoidRiftEntity.java): continuous gravity pull on entities and projectiles inside radius x 1.8, RIFTED 20 ticks refreshed on enemies, 25 damage every 10 ticks (lines 131-159). Rifted is a buff effect (speed, strength, jump, slow falling, effect/RiftedEffect.java), so enemies caught in the rift are technically being buffed while damaged. PORTAL_AMBIENT pulse every 25 ticks (lines 162-165). If never released, it auto-implodes after 400 ticks (lines 168-170).
- Release (releaseUsing, HollowsEdgeItem.java:356-376): if the player is looking at the rift (dot above 0.85) and within 40 blocks, the player teleports into the rift center and gains RIFTED 200 ticks. Then triggerImplosion runs: a 10 tick violent vacuum phase, then the burst (entity/VoidRiftEntity.java:84-128): flat 550 damage to everything in radius x 2.2, launch 2.2 outward plus 0.8 up, Dark Faded amplifier 1 for 100 ticks. Sounds GENERIC_EXPLODE, WARDEN_SONIC_BOOM, DbaSounds.WIND_GUST.
- Cooldown: 60 ticks (HollowsEdgeItem.java:374).

### Gaps

- The charge and window system is optional for mobility: the out-of-window fallback blink costs no Ki and has no cooldown, so a player can blink 3 blocks forever for free and the 5 second charge only buys distance and the 3rd dash slash.
- Rift damage never scales: the implosion is flat 550 and the pull ticks are flat 25 whether the rift was tapped or charged the full 5 seconds, and neither scales with Strength. Only the radius rewards charging.
- No screen shake, hitstop, or impact FX anywhere in the kit, including the "massive final implosion" (the item's own comment at HollowsEdgeItem.java:370). The biggest moment of the weapon is three sounds and a visual shell.
- Basic melee has the least identity of the five: highest base damage (850) but no on-hit effect at all, and the only swing profile with Tip.NONE.
- The afterimage looks like a damage threat but deals none: it only pulses Dark Faded.
- Rifted on enemies is a beneficial effect (effect/RiftedEffect.java bundles Speed, Strength, Jump Boost, Slow Falling). Enemies standing in the rift get buffed stats for 20 ticks at a time while taking 25 damage per 10 ticks, which reads as unfinished design rather than a debuff.

---

## 5. Dimensional Sword

Source: item/DimensionalSwordItem.java (177 lines, the smallest kit of the five). Theme: spacetime slashes and warps.

### Base stats and passives

- Melee damage 750 (modifier 749.0, DimensionalSwordItem.java:36), attack speed modifier -2.4.
- hurtEnemy (DimensionalSwordItem.java:72-79): drains the wielder's stamina discretely at 100 percent per minute over 13 ticks (about 1.08 percent of max stamina per hit), and applies MELTING 200 ticks amplifier 1 to the target. The drain's return value is ignored, so Melting applies even when the drain fails and zeroes stamina.
- Melting is a percent-of-max-health damage over time effect that also applies -20 percent movement and attack speed per level and -5 percent max health per level (effect/MeltingEffect.java, header lines 13-25).
- Passives while held (DimensionalSwordItem.java:51-59): NAME_PLATE_DIST -64, MINI_NAME_PLATE_DIST -10, AIR_DRAG -0.70.
- Swing trail profile (WeaponTrailProfiles.java:47): color 0x9B6BFF, ARC, reach 2.2, sweep 150 degrees, fade 7 ticks, VOID_SMOKE tip.

### Ability 1: Dimensional Slash (left click, in air)

- Trigger: left-click default action (network/DbaNetwork.java:476). Handler fireSlash, DimensionalSwordItem.java:82.
- Cost: stamina discrete drain 100 percent per minute over 13 ticks (about 1.08 percent of max stamina), and this one is checked: insufficient stamina means no slash (DimensionalSwordItem.java:85-87). No cooldown.
- Projectile: DimensionalSlashEntity with damage 750 at speed 2.5 along the look direction (DimensionalSwordItem.java:92-95). Successive slashes alternate tilt, stored as the "tiltRight" flag in the stack's CUSTOM_DATA (DimensionalSwordItem.java:89, 98-101).
- Slash behavior (entity/DimensionalSlashEntity.java): dies on any hit, block or entity (lines 138-143), 100 tick timeout (lines 102-104). On entity hit: 750 damage, MELTING 300 ticks amplifier 1, FRICTION -0.90 and AIR_DRAG -0.80 modifiers on the victim (lines 111-130).
- Audio: on its first tick, ILLUSIONER_CAST_SPELL plus RESPAWN_ANCHOR_SET_SPAWN (entity/DimensionalSlashEntity.java:85-90). On hit, RESPAWN_ANCHOR_DEPLETE (line 132-133). All vanilla respawn anchor and illusioner sounds, no custom sound.

### Ability 2: Dimensional Warp (right click)

- Trigger: right click (use, DimensionalSwordItem.java:105).
- Ki cost: discrete drain 100 percent per minute over 100 ticks, 8.33 percent of max Ki, checked up front (DimensionalSwordItem.java:108-110).
- Targeting: raycast up to 1024 blocks. A block hit warps the player to the hit face's cell; no block in view warps 128 blocks along the look direction instead (DimensionalSwordItem.java:116-141).
- Entities: two DimensionalWarpRiftEntity, radius 3.5, burst damage 750 each: one at the origin (isDestination false) and one at the destination (true) (DimensionalSwordItem.java:143-156).
- Self effects on warp: RIFTED 60 ticks and DARK_FADED 35 ticks (DimensionalSwordItem.java:159-160). Teleport through MovementPhysics.teleport (DimensionalSwordItem.java:167-169).
- Cooldown: 100 ticks (DimensionalSwordItem.java:172).
- Audio in the item: ENDERMAN_TELEPORT at both ends plus DbaSounds.DARK_PULSE at the destination (DimensionalSwordItem.java:162-165).
- Rift behavior (entity/DimensionalWarpRiftEntity.java): 24 tick life (line 27). Every enemy within 3.5 blocks of either rift takes 750 damage once, MELTING 200 ticks amplifier 1, FRICTION -0.90 and AIR_DRAG -0.80, and is pulled toward the origin rift or pushed away from the destination rift (lines 96-158). Each rift also plays its own four-sound spawn stack on tick 1 (lines 72-89): origin gets ENDERMAN_TELEPORT, DARK_PULSE, RESPAWN_ANCHOR_SET_SPAWN, PORTAL_TRAVEL; destination gets WARDEN_SONIC_BOOM, WIND_GUST, RESPAWN_ANCHOR_DEPLETE, GENERIC_EXPLODE.

### Gaps

- The origin rift deals the same 750 burst as the destination rift. Warping away nukes everyone you left behind for full slash damage. Nothing in the item text or comments frames the departure as an attack; it reads as the shared constructor argument (750.0f passed for both, DimensionalSwordItem.java:144, 154) rather than a designed second hit.
- Sound and camera disagree at the destination: the rift plays an explosion and sonic boom stack, but there is no screen shake, hitstop, or impact FX anywhere in the kit.
- Slash spam: fireSlash has no cooldown; the only throttle is about 1.08 percent stamina per shot, so a full stamina bar buys roughly 90 back-to-back 750 damage slashes.
- The advertised range is misleading in code: the raycast runs 1024 blocks, but aiming at open sky caps the warp at 128 blocks (DimensionalSwordItem.java:117, 136). Long warps only work with a visible block to land on.
- Smallest kit of the five by a wide margin: two active moves, no charged move, no channel, no finisher, no domain. Every other weapon in this group has three beats; this one has two.
- The slash's whole audio identity is borrowed vanilla sounds (illusioner cast, respawn anchor), the only projectile in the group with no custom sound and no impact feedback beyond the hit sound.

---

## Cross-weapon summary of the weakest points

1. Basic melee identity: Azure and Hollows Edge apply nothing on a basic hit; Dabura and Curse only drain their own Ki. Only Dimensional Sword puts an effect (Melting) on the victim.
2. Reused impact package: DbaImpactFx.heavy is the only shake and hitstop source, and only Azure and Dabura use it. Curse, Hollows Edge, and Dimensional Sword finishers land with no camera feedback at all.
3. Reused domain visual: Curse Blade's Abyssal Domain is Dabura's Darkness Domain renderer recolored, and the two finishers share the name Abyssal Eclipse.
4. Flat damage where siblings scale: Curse lightning 450, Curse slam 150, Hollow rift 550 and 25 per tick have no Strength term.
5. Free mobility: Hollow's Edge blink outside its rush window costs nothing and has no cooldown.
6. Unfinished-feeling mechanics: Dabura's "delayed" waves are not delayed in time; Dimensional's origin warp rift deals full burst damage; the Void Rift applies a beneficial effect (Rifted) to the enemies it is hurting.
