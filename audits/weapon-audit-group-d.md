# Weapon Audit Group D (Nov 1, 2026)

Audit only, no code changed. Scope: PowerPoleItem, WhisStaffItem, BanshoFanItem, BlasterGunItem, plus every entity, effect, and helper class they reference. Line numbers are from the current main branch files.

Shared facts that apply to all four weapons:

- Every ability damage number below passes through `DbaEffectsConfig.capDamage` (`config/DbaEffectsConfig.java:55-58`). The cap config `maxWeaponDamage` defaults to 0, which means uncapped (`config/DbaEffectsConfig.java:15,23`).
- All Ki and stamina costs use `WeaponDrainHelper` discrete drains: cost = max stat x (percentPerMinute / 100) x (durationTicks / 1200) (`util/WeaponDrainHelper.java:134-158` Ki, `163-188` stamina). The percents below are converted to real per-use costs using that formula.
- Basic swing visuals for all four come from the shared table `client/fx/WeaponTrailProfiles.java`; per-weapon profiles are cited in each section.
- None of the four weapons uses a DBA post-processing shader for any ability. Screen shake appears exactly once across the whole group (Blaster Gun overcharged shot, via `fx/DbaImpactFx.java`). All ability visuals are GeckoLib "physical 3D" entities (renderers registered in `client/DragonBlockArcaneDBAClient.java:227-347`) plus dust particles emitted by the status effect classes.

---

## 1. Power Pole (item/PowerPoleItem.java)

Base stats: attack damage modifier +749.0, so 750 total (`createModifiers`, ~L40-48); attack speed -2.0 (~L49-57); while held in mainhand, AIR_DRAG -0.40 multiplied base (~L60-63). No durability logic in the class.

### Ability 1: plain melee hit (passive on hit)
- Trigger: vanilla melee hit landing; `hurtEnemy` (~L67-74).
- Effect: stamina drain only. `drainStaminaDiscrete(player, 20.0, 10)` = 0.167% of max stamina per hit, server side, player attackers only.
- Damage: the 750 base attribute damage; no bonus hit, no status, no knockback code.
- Visuals and audio: none in the item. Swing trail from the shared profile: ARC, color 0xFF5040, reach 2.9, sweep 130 deg, fade 5 ticks, alpha 0.9, no tip particle (`WeaponTrailProfiles.java:53`).

### Ability 2: Whirlwind Staff (left-click hold, `performWindSpin` ~L77-160)
- Trigger: hold left click. The client sends `C2SWeaponLeftClickPayload` ACTION_CLICK every 2 ticks while the attack key is held for "rapid" weapons including the Power Pole (`client/DragonBlockArcaneDBAClient.java` ~L578-593, weaponUseTimer = 2, commented "10 attacks per second"); the server receiver dispatches straight to `performWindSpin` with no item-cooldown check (`network/DbaNetwork.java:477-479`).
- Cost: stamina `drainStaminaDiscrete(player, 20.0, 10)` per call = 0.167% of max stamina; the call aborts if stamina is insufficient (~L80-83). At the 2-tick call rate that is about 100% of max stamina per minute of holding. No item cooldown anywhere on this ability.
- Damage: every living entity in a 35 degree cone out to 25 blocks, or within 4.5 blocks of the player in any direction (~L88,105-106): `750.0 / max(1.0, distance / 3.0)`, capped (~L110-111). That is 750 at 3 blocks or closer, about 250 at 9 blocks, 90 at the 25-block edge. Applied per call, so a held spin re-damages everything in the cone every 2 ticks.
- Secondary effects per hit: `MovementLimiterHelper.applyPowerPoleGaleForce` cancels inward rush momentum and repels the target (base force max(0.6, 2.2 / max(1, distance/4)) plus an upward 0.2-0.25) (`util/MovementLimiterHelper.java:90-120`); BOUNCINESS +0.90 attribute modifier (~L114-121).
- Projectile deflection: enemy projectiles within 25 blocks that are inside 4.5 blocks or in front (look dot > 0.75) change owner to the player and are redirected away at speed 2.2 (~L132-145).
- Entities spawned: `PowerPoleWhirlwindEntity` at the eye position, range 25.0, cone 35.0 (~L148-151). The entity is purely visual: lifetime 8 ticks, it follows the owner's eye position and rotation each tick and does no damage itself (`entity/PowerPoleWhirlwindEntity.java:30,92-112`).
- Audio: `SoundEvents.PLAYER_ATTACK_SWEEP` (1.4 vol, 1.3 pitch) and `SoundEvents.WIND_CHARGE_THROW` (1.0, 1.1) at the player (~L154-157).
- Visuals: the whirlwind entity only. Zero particles, no screen shake, no trail beyond the basic swing profile.

### Ability 3: Extended Reach poke (right click, `use` ~L162-232)
- Trigger: right click. Cost: stamina `drainStaminaDiscrete(player, 20.0, 40)` = 0.667% of max stamina, returns FAIL if insufficient (~L168-171; note the drain check runs before the server-only block, so it is also evaluated on the client).
- Cooldown: 40 ticks (2 seconds), end of `use`.
- Targeting: entity raycast to 30.0 blocks, falling back to the block POV hit, falling back to the full 30-block end point (~L176-215).
- On a living hit: 750.0 damage, capped (~L192); POLE_STUN amplifier 0 for 100 ticks (~L195); BOUNCINESS +0.95 (~L198-205); plus a 15% chance of a second POLE_STUN at amplifier 1 for 60-100 ticks (~L208-211). POLE_STUN itself is -80% movement speed and -60% attack speed with rotating gold dust stars over the head (`effect/PoleStunEffect.java`).
- Entities spawned on every use, hit or miss: `PowerPoleExtensionEntity` from the eye, length = distance to hit point (~L217-220; visual only, lifetime 12 ticks, `entity/PowerPoleExtensionEntity.java:22,88-103`), and `PowerPoleImpactEntity` at the hit point, scale 2.4 (~L223-226; visual only, lifetime 14 ticks, `entity/PowerPoleImpactEntity.java:21,74-85`).
- Audio: `SoundEvents.TRIDENT_THROW` at the player; `SoundEvents.ANVIL_LAND` and `SoundEvents.HEAVY_CORE_HIT` at the hit point (~L229-235).

### Gaps: Power Pole
1. Whirlwind Staff has no cooldown and no per-target gating: full cone damage plus a fresh whirlwind entity every 2 ticks for as long as the key is held. The only throttle is the stamina drain.
2. The `hurtEnemy` comment (~L67) calls the melee hit "Whirlwind Staff (AoE Knockback and Damage)"; the body does none of that, it only drains stamina. The AoE lives entirely in `performWindSpin`.
3. Extended Reach plays the full impact package (impact entity plus ANVIL_LAND and HEAVY_CORE_HIT) even on a total miss, at the empty 30-block end point in mid-air.
4. Whirlwind's only feedback is an 8-tick visual entity and two vanilla sounds, one of which (PLAYER_ATTACK_SWEEP) is the generic vanilla sweep reused by other weapons. No particles, no shake, no DBA sound.
5. The poke stacks two POLE_STUN instances (amp 0 for 100 ticks always, amp 1 for 60-100 ticks on the 15% roll), so the "heavy concussion" roll can produce a shorter total stun than the guaranteed base application.

---

## 2. Whis Staff (item/WhisStaffItem.java)

Base stats: attack damage modifier +349.0, so 350 total (~L62-70); attack speed -2.4 (~L71-79); while held, FRICTION -0.80 and AIR_DRAG -0.80 multiplied base (~L82-87). Class doc comment (~L36-56) describes the kit; parts of it are stale (see gaps).

### Passive: Auto-Dodge (not in the item class)
- Implementation: `mixin/LivingEntityMixin.java:95-110`. While the staff is in the mainhand, every incoming damage event has a 50% chance to be fully negated; on a dodge the mixin spawns 10 white `DustParticleOptions` (0xFFFFFF, size 1.5) at the player's mid-body and sets `invulnerableTime` to at least 10 ticks. No sound, no cooldown beyond the invulnerability frames, no cost.

### Ability 1: Temporal Shatter hit (left click melee, `hurtEnemy` ~L92-160)
- Trigger: melee hit landing. No item cooldown (attack speed governs). The client also sends a left-click payload per swing (generic swing branch, `client/DragonBlockArcaneDBAClient.java` ~L832-848), and the server receiver drains Ki again for Whis Staff swings (`network/DbaNetwork.java`, Whis Staff branch at the end of the left-click receiver).
- Applied to the target regardless of attacker type (~L95-114, before the ServerPlayer check):
  - Time reversal: `TimeTracker.dba$startReversing(200)` (200 ticks) if the target implements TimeTracker (~L95-97). Implementation in `mixin/LivingEntityMixin.java:299-313` replays the recorded position history with noPhysics.
  - TEMPORAL_STASIS amplifier 0 for 100 ticks (~L100). The effect is -100% movement speed and -100% attack speed, zeroes horizontal velocity every tick, and orbits blue dust particles around the victim (`effect/TemporalStasisEffect.java`).
  - AIR_DRAG +4.0 and FRICTION +3.0 multiplied-base attribute modifiers (~L102-114).
- Player attacker only (~L116-160):
  - Ki cost: `drainKiDiscrete(serverPlayer, 100.0, 13)` = 1.083% of max Ki per hit. The return value is ignored, so the hit fully applies even when Ki runs out. (Plus the separate payload drain noted above.)
  - Bonus damage 1: Dexterity x 2.0 as `playerAttack` damage, capped (~L124-128), on top of the 350 base attribute hit.
  - Bonus damage 2 (Temporal Shatter): 1.5% of the target's max health as `magic` damage, capped (~L131-132).
  - Entity spawned: `TimeShatterEntity` at the target's mid-height, scale 1.2 (~L135-141). Visual only, lifetime 18 ticks (`entity/TimeShatterEntity.java:20-21,52-62`).
  - Audio at the target: `SoundEvents.AMETHYST_CLUSTER_BREAK` (1.2, 1.6), `SoundEvents.BEACON_ACTIVATE` (0.9, 1.9), and the DBA sound `DbaSounds.ABILITY_CHARGE` (0.9, 1.9) (~L144-160).
- Visuals: shatter entity plus the stasis effect's blue dust orbit. No particles in the item class itself, no shake, no trail beyond the shared profile: ARC, 0xFFE9A8, reach 2.6, sweep 120 deg, fade 6, alpha 0.9, tip SPARK (`WeaponTrailProfiles.java:55`).

### Ability 2: Temporal Rift (right click, `use` ~L162-228)
- Trigger: right click.
- Ki cost: `drainKiDiscrete(player, 100.0, 100)` = 8.33% of max Ki; returns FAIL if insufficient (~L167-170).
- Cooldown: 100 ticks (5 seconds), end of `use`.
- Area: 12.0 block radius (`AOE_RADIUS`, ~L59). Every living entity except the caster within range takes, instantly:
  - Magic damage `400.0 + Spirit x 1.5`, capped (~L176,188).
  - TEMPORAL_STASIS amplifier 0 for 60 ticks (~L191).
  - Time reversal `dba$startReversing(300)` (300 ticks) (~L194-196).
- Self-buff: CELESTIAL_GRACE amplifier 0 for 200 ticks (~L201): +50% movement speed, +0.6 jump strength, +6 armor, +4 armor toughness, +10 safe fall distance, with cyan dust particles (`effect/CelestialGraceEffect.java`).
- Entity spawned: `TemporalRiftEntity` at the player's feet, radius 12.0, lifetime 60 ticks (~L204-211). Visual only; it plays an ambient `SoundEvents.AMETHYST_BLOCK_RESONATE` hum (0.8, 1.2) every 20 ticks while alive (`entity/TemporalRiftEntity.java:72-80`).
- Audio on cast: `SoundEvents.BEACON_POWER_SELECT` (1.4, 1.2) and `SoundEvents.EVOKER_PREPARE_SUMMON` (1.0, 1.6) (~L214-225).
- Visuals: rift dome entity plus effect dust. No screen shake, no shader, no DBA sound.

### Gaps: Whis Staff
1. Reversal duration mismatch: the staff requests 200-tick (left click) and 300-tick (rift) reversals, but the position history buffer holds a maximum of 100 entries, documented as 5 seconds (`util/TimeTracker.java:11-14`). The rewind therefore runs out of recorded positions long before the requested duration; the longer numbers in the item are not achievable as written.
2. Stale class doc: the header (~L38-47) promises "Slowness III for 5 seconds" on left click; the code applies the custom TEMPORAL_STASIS at amplifier 0 for 100 ticks. It also frames the left click as "350 + (Dexterity x 2)" and omits the 1.5% max-health shatter hit that the code adds separately.
3. Ki is charged twice for one swing: once in `hurtEnemy` (100 x 13 ticks) and once in the `DbaNetwork` left-click receiver branch for Whis Staff (100 x 13 ticks). The Bansho Fan has the same double-charge pattern (below); the Power Pole's payload path does not double-charge its melee.
4. `hurtEnemy` applies reversal, stasis, and the physics modifiers even when the attacker is not a player (no gate on ~L95-114), while the Ki drain and both bonus damages are player-only. A mob somehow swinging the staff gets the control effects for free.
5. Temporal Rift, a 12-block time freeze with a 2300-plus-scale sibling in this audit's damage range, has no screen shake, no hitstop, and no unique audio: both cast sounds are vanilla, and the rift's ambient hum is a vanilla amethyst sound.
6. Auto-Dodge's only feedback is a generic white dust burst. No sound, no afterimage, no animation cue for a 50% full-damage negation passive.

---

## 3. Bansho Fan (item/BanshoFanItem.java)

Base stats: attack damage modifier +399.0, so 400 total (~L57-65); attack speed -2.0 (~L66-74); while held, AIR_DRAG -0.60 and FRICTION -0.75 multiplied base (~L77-82). Class doc (~L28-47) says "Stamina Drain: 45% per minute (smooth)"; the implementation is discrete per-action drains, not a smooth drain.

### Ability 1: Gale Force hit (left click melee, `hurtEnemy` ~L92-180)
- Trigger: melee hit landing. No item cooldown.
- On every hit, any attacker:
  - Launch: target velocity increases by a horizontal vector of length 5.0 away from the attacker plus +1.2 vertical (~L95-97). The comment says "launches target 10 blocks"; the code sets velocity 5.0, not a fixed 10-block displacement.
  - BOUNCINESS +0.85 and AIR_DRAG -0.60 attribute modifiers (~L99-112).
  - BLEEDING amplifier 1 (Bleeding II) for 300 ticks (~L115). BLEEDING deals 0.5% of max health as magic damage every `40 >> amplifier` ticks (amplifier 1 = every 20 ticks), minimum 2.0, plus -10% armor per level, blocks natural regen, and drips dark red dust particles (`effect/BleedingEffect.java`).
- Player attacker only (~L118-180):
  - Stamina cost: `drainStaminaDiscrete(serverPlayer, 45.0, 10)` = 0.375% of max stamina per hit; return value ignored. The server left-click receiver separately drains the same 45 x 10 for every Bansho Fan swing payload (`network/DbaNetwork.java`, Bansho Fan branch at the end of the receiver), so a landed hit is charged twice.
  - Bonus damage: Strength x 1.5 as `playerAttack`, capped (~L125-129), on top of the 400 base.
  - Cyclone Slash proc, 30% chance (~L132): cycloneDamage = (400 + Strength bonus) x 0.5 (~L133).
    - Spawns `BanshoCycloneEntity` at the target (~L136-141).
    - Immediately, in the item class, every other living entity within 3.0 blocks of the target takes the full cycloneDamage, knockback 2.0 horizontal / 0.5 up, and BLEEDING amplifier 0 for 200 ticks (~L143-160).
    - The cyclone entity then keeps working for 25 ticks: radius 4.0, height 6.5, it pulls, lifts (vy 0.32), and spins every living entity inside, deals slashDamage x 0.35 every 5 ticks (up to 5 damage ticks), and applies BLEEDING amplifier 0 for 100 ticks, with `ELYTRA_FLYING` and `WIND_CHARGE_BURST` sounds on tick 1 and every 12 ticks (`entity/BanshoCycloneEntity.java:52-122`).
  - Entity spawned on every hit: `BanshoShockwaveEntity`, radial mode, scale 3.2, at the target's mid-height (~L164-173). Visual only, lifetime 14 ticks (`entity/BanshoShockwaveEntity.java:23,82-91`).
  - Audio at the target: `SoundEvents.PLAYER_ATTACK_SWEEP` (1.2, 1.2) and the DBA sound `DbaSounds.SLASH_WHOOSH` (1.2, 1.2) (~L175-178).
- Basic swing profile: FAN kind, 0xCFF5EE, reach 2.6, sweep 200 deg, fade 8, alpha 0.7, tip WIND (`WeaponTrailProfiles.java:56`).

### Ability 2: Tempest Barrage (right click, `use` ~L184-244)
- Trigger: right click.
- Stamina cost: `drainStaminaDiscrete(player, 45.0, 40)` = 1.5% of max stamina; returns FAIL if insufficient (~L189-192).
- Cooldown: 40 ticks (2 seconds) (~L242).
- Audio first, on both logical sides because `level.playSound` is called before the server check: `SoundEvents.WIND_CHARGE_THROW` with SoundSource.NEUTRAL (1.2, 0.9) and `SoundEvents.ELYTRA_FLYING` with PLAYERS (1.0, 1.5) (~L195-200).
- Server side: spawns `BanshoShockwaveEntity` in cone mode, scale 3.5, at eye + look x 0.8 (~L207-216), then fires 5 `BanshoWindProjectileEntity`:
  - Damage each: 300 + Spirit x 1 (~L219-222).
  - Spread: yaw offsets -8, -4, 0, 4, 8 degrees; pitch offsets 2, -1, 0, -1, 2; speed 4.5; inaccuracy 0.0 (~L225-241).
  - Projectile behavior (`entity/BanshoWindProjectileEntity.java:97-185`): max life 50 ticks; hits only the first living entity in its swept box (or a block); on an entity hit it deals its full damage, applies BLEEDING amplifier 2 (Bleeding III) for 400 ticks, knockback 1.4 horizontal / 0.45 up, spawns a radial `BanshoShockwaveEntity` scale 2.6 (2.0 on a block hit), and plays `WIND_CHARGE_BURST` plus `ELYTRA_FLYING` at the impact point.

### Gaps: Bansho Fan
1. Cyclone Slash double-dips: the item class instantly applies the full cycloneDamage in a 3-block AoE and the spawned cyclone entity then applies a further slashDamage x 0.35 every 5 ticks for 25 ticks to everything it holds. The class doc (~L38-40) describes the cyclone as dealing "50% damage"; the combined total is well beyond that.
2. The headline Gale Force launch has no visual of its own: no gust particles at the launch moment. Feedback is the shared shockwave entity (which also fires on non-launch hits) and bleeding dust.
3. `use` plays its two sounds through `level.playSound` before the `isClientSide` split, so they can play on both sides; it also mixes SoundSource.NEUTRAL and PLAYERS for the same cast, unlike the other weapons in this group.
4. Stamina double-charge on landed melee hits (receiver branch plus `hurtEnemy`), same pattern as the Whis Staff.
5. No screen shake, no shader, no DBA sound in Tempest Barrage: a 5-projectile endgame barrage is carried entirely by two entities and vanilla wind sounds.

---

## 4. Blaster Gun (item/BlasterGunItem.java)

Base stats: attack damage modifier +449.0, so 450 total (~L46-54); attack speed -2.0 (~L55-63); while held, NAME_PLATE_DIST +64.0 and MINI_NAME_PLATE_DIST +30.0 (~L66-71). There is no `hurtEnemy` override: a melee swing is a plain 450 attribute hit. This weapon has no Ki or stamina cost anywhere; its economy is ammunition and heat.

Ammunition and heat utilities (~L78-121): ammo is `BlasterAmmoItem` stacks counted across the whole inventory (`getAmmoCount`, `consumeAmmo`; creative mode bypasses consumption). Heat is stored per stack in the CUSTOM_DATA component under the key "Heat", clamped 0-100 (`getHeat`, `setHeat`).

### Ability 1: Blaster Barrage (left-click hold, `onLeftClickBarrageTick` ~L124-176)
- Trigger: hold left click. The client increments `blasterBarrageTicks` and sends an ACTION_CHARGE_TICK payload every tick (`client/DragonBlockArcaneDBAClient.java` ~L793-808); the server receiver checks the item cooldown (none is ever set by the barrage itself) and calls `onLeftClickBarrageTick` (`network/DbaNetwork.java:525-529`). The method fires only when `chargeTicks % 3 == 0`, so one bolt every 3 ticks (~L128-129).
- Cost per shot: 1 ammo, or 3 ammo for an overcharged shot (heat >= 100) (~L135-139). If ammo is short (and not creative): actionbar message "Out of Blaster Ammo!", `SoundEvents.DISPENSER_FAIL` (1.0, 1.8), no shot (~L136-141).
- Damage per bolt:
  - Normal: `250 + (Spirit x 1.5) x (1 + heatRatio x 0.5)` where heatRatio = heat / 100, so the heat bonus multiplies the Spirit scaling by up to x1.5 at 99 heat (~L147-149).
  - Overcharged (heat >= 100): `700 + Spirit x 3.0` (~L147-148).
- Projectile: `BlasterBoltEntity`, velocity = look x 2.4 per tick (~L152-155). Bolt behavior (`entity/BlasterBoltEntity.java`):
  - Piercing: it keeps a hit set and is never discarded on hit; lifetime 30 ticks (about 72 blocks at full speed) (~L122-162).
  - Hit radius: bounding box inflated by 0.6 + heatRatio x 0.4, or 1.5 when overcharged (~L118-119).
  - On hit: full bolt damage (capped), CINEMATIC_TRACKING amplifier 0 for 15 ticks (an invisible internal effect with no particles or attributes, `effect/CinematicTrackingEffect.java`), knockback 0.6 horizontal (1.5 overcharged) plus 0.2 up (~L130-141).
  - Overcharged extra: BOUNCINESS +0.75 and AIR_DRAG -0.30 on the victim, 60% bolt damage splashed to other living entities within 3.0 blocks, exactly 1 vanilla `ParticleTypes.EXPLOSION` particle, and `GENERIC_EXPLODE` sound (~L143-160).
  - The bolt performs no block raycast at all: it advances by `setPos` and only tests entity boxes, so bolts pass through solid blocks for their entire 30-tick life.
- Heat and audio per shot: normal shot adds +8 heat and plays `SoundEvents.BLAZE_SHOOT` (1.0, pitch 1.6 + heatRatio x 0.4) (~L165-168). Overcharged shot resets heat to 0, plays `GENERIC_EXPLODE` (1.5, 1.2), and calls `DbaImpactFx.heavy` at the shooter's own position (~L160-164): that sends a `ScreenShakePayload` (intensity 2.2, 12 ticks, hitstop 3 inside 8 blocks) to players within 28 blocks with distance falloff, spawns 40 DBA SPARK and 16 DBA WIND particles, and plays `DbaSounds.HEAVY_IMPACT` (`fx/DbaImpactFx.java:16-33`).
- HUD: after every shot the shooter gets an actionbar message with current ammo and heat percent (~L171-175). This is the only heat feedback besides the actionbar text.
- Basic swing profile (melee swings): FLASH kind, 0xFFB347, reach 8.0, fade 3, tip SPARK (`WeaponTrailProfiles.java:57`).

### Ability 2: Erasure Cannon (right click hold and release; `use` ~L182-190, `onUseTick` ~L202-228, `releaseUsing` ~L231-282)
- Trigger: hold right click (`startUsingItem`, use animation BOW, use duration 72000), release to fire. Gate: requires at least 10 ammo in inventory or the use returns PASS with a chat message (~L184-189).
- Charge: maximum charge at `MAX_RIGHT_CHARGE_TICKS` = 100 ticks (5 seconds) (~L40). While holding, server side each tick:
  - The player receives ENERGY_OVERCHARGE amplifier 0 refreshed for 10 ticks (~L211): -50% movement speed, +0.8 knockback resistance, green dust spark particles (`effect/EnergyOverchargeEffect.java`).
  - An `ErasureChargeOrbEntity` is spawned once per player and tracked in the static `ACTIVE_CHARGE_MAP`; its charge ratio is set to heldTicks / 100 each tick (~L214-222). The orb follows the muzzle (eye + look x 1.2) every tick and self-discards if the player stops using the item or after 6 ticks without an update (`entity/ErasureChargeOrbEntity.java:70-103`).
  - `SoundEvents.BEACON_AMBIENT` every 20 ticks, pitch 0.6 + chargeRatio x 0.8 (~L224-227).
- On release (`releaseUsing` ~L231-282):
  - The charge orb is removed and discarded (~L238-241).
  - chargeRatio is floored at 0.1, so even a 1-tick tap fires at 10% charge (~L244).
  - Ammo cost: 10 + (int)(chargeRatio x 10), i.e. 11 to 20. If the player has less than the cost (and is not creative), the cost is silently clamped to whatever ammo remains and the shot still fires at full computed damage (~L247-251).
  - Beam geometry: block raycast from the eyes along look, maximum length 48.0, shortened to the first block hit (~L254-265).
  - Damage: `900 + chargeRatio x 1400 + Spirit x 4.0` (~L267-269). That is 1040 + Spirit x 4 at the 0.1 floor and 2300 + Spirit x 4 at full charge.
  - Entity spawned: `ErasureCannonBeamEntity` with the total damage, charge ratio, and beam length (~L272).
  - Cooldown: 120 ticks (6 seconds) (~L274).
- Beam behavior (`entity/ErasureCannonBeamEntity.java`): lifetime 16 ticks (MAX_LIFETIME, L37), but all damage lands once on tick 1 (~L113-176):
  - Hit test is an AABB built from the beam start and end points, inflated by 1.5 + charge x 1.5 (~L126-131). There is no per-target distance-to-beam-line check, so anything inside the fat diagonal box takes the hit, including targets well off the visual beam line.
  - Each target: full totalDamage once (capped), CINEMATIC_TRACKING 25 ticks, BOUNCINESS +0.85, AIR_DRAG -0.40, and a suction pull of 0.8 toward the beam centerline plus 0.2 up (~L133-157).
  - At charge >= 0.9 only: 3 vanilla `ParticleTypes.EXPLOSION_EMITTER` particles at the endpoint and 50% totalDamage to living entities in a 4-block box at the endpoint (~L159-167).
  - Audio on the damage tick: `SoundEvents.LIGHTNING_BOLT_THUNDER` (2.2, 0.6) and `DbaSounds.LIGHTNING_ZAP` (2.2, 0.6) at the beam origin (~L169-174).
  - No screen shake and no hitstop for the beam itself.
- Cleanup: `onPlayerDisconnect` discards any tracked charge orb (~L278-285).

### Gaps: Blaster Gun
1. Bolts ignore blocks completely (no clip test in `BlasterBoltEntity.tick`): barrage fire pierces walls, floors, and terrain for its full roughly 72-block range. This reads as unfinished rather than a design choice, because the Erasure Cannon beam in the same weapon does raycast blocks.
2. The Erasure Cannon, the biggest single hit in this group (up to 2300 + Spirit x 4), has no screen shake and no hitstop. The group's only shake is attached to the overcharged barrage shot and is centered on the shooter, not the impact.
3. Charge floor plus ammo clamp: a 1-tick tap fires a 1040 + Spirit x 4 beam, and a player short on ammo pays only what they have and still gets full damage. The 10-ammo gate in `use` is the only real floor.
4. Heat has no downside: it only buffs normal shots (up to x1.5 on the Spirit scaling) and at 100 it converts the next shot into the strictly stronger overcharged shot. The only costs are the 3-ammo price and the heat reset.
5. Beam hit detection is a fat AABB, not a line test: targets behind cover near the beam's bounding box, or several blocks off the beam line, take full beam damage.
6. Audio is mostly generic vanilla (BLAZE_SHOOT per bolt, BEACON_AMBIENT while charging, GENERIC_EXPLODE, LIGHTNING_BOLT_THUNDER). Only LIGHTNING_ZAP and the HEAVY_IMPACT inside `DbaImpactFx.heavy` are DBA sounds, and the charge loop has no rising DBA charge sound of its own.
7. `ACTIVE_CHARGE_MAP` is a static map keyed by player UUID; cleanup depends on `releaseUsing`, the orb's own `isUsingItem` watchdog, and the disconnect hook. Those three paths cover the known exits, but the map itself is never pruned for players whose orb died by the watchdog (the stale entry is overwritten on the next charge, and `releaseUsing` removes it; low risk, noted for completeness).

---

## Cross-cutting gaps for the group

1. Double resource charging: Whis Staff and Bansho Fan melee hits are charged once in `hurtEnemy` and once in the `DbaNetwork` left-click receiver branch for the same swing. Power Pole and Blaster Gun do not have this overlap.
2. Vanilla sound reuse is heavy across the group: PLAYER_ATTACK_SWEEP (Power Pole spin, Bansho hit), BEACON family sounds (Whis hit and rift), ELYTRA_FLYING (Bansho projectile, cyclone, barrage cast), GENERIC_EXPLODE and BLAZE_SHOOT (Blaster). DBA-owned sounds appear only in Bansho's SLASH_WHOOSH, Whis's ABILITY_CHARGE, and Blaster's LIGHTNING_ZAP and HEAVY_IMPACT.
3. No ability in the group uses a post-processing shader, and only one uses screen shake (overcharged bolt, at the shooter). Big moments (Temporal Rift freeze, Erasure Cannon, Tempest Barrage) have no shake or hitstop.
4. Several class doc comments no longer match the code (Whis "Slowness III", Bansho cyclone "50% damage", Power Pole `hurtEnemy` "AoE", Bansho launch "10 blocks"). The code numbers in this audit are the live ones.
5. Every ability visual is delegated to a spawned entity or an effect-class dust emitter; the four item classes themselves spawn no particles at all except through `DbaImpactFx` (Blaster overcharged shot).
