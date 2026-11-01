# Weapon Audit, Group A (Nov 1, 2026)

Audit only, no code changed. Scope: KatanaItem, ZSwordItem, SaberItem, BraveSwordItem, GrandSwordItem, plus every entity, projectile, and effect helper class they reference. All damage values pass through `DbaEffectsConfig.capDamage` (config/DbaEffectsConfig.java:55-58), which clamps to the configured `maxWeaponDamage` when that value is above 0.

## Shared mechanics (all five weapons)

- Drain math (util/WeaponDrainHelper.java): per-tick drains take `percentPerMinute` of the max stat spread over 1200 ticks (WeaponDrainHelper.java:22-47 for Ki, 54-79 for Stamina). Discrete drains take `percentPerMinute x durationTicks / 1200` of the max stat (WeaponDrainHelper.java:125-146 Ki, 152-177 Stamina, 184-219 both). So a "30% per minute, 7 ticks" melee drain is 0.175% of max Ki per hit. `hasKi` / `hasStamina` only require the stat to be above 0.5 (WeaponDrainHelper.java:224-234).
- Left-click hold and release for all five weapons is routed server side through payloads in network/DbaNetwork.java (Z Sword :451-455, Saber :483-485, Grand Sword :499-503, Katana :522-524, Brave Sword :531). Right-click abilities use the vanilla `use` / `onUseTick` / `releaseUsing` path in each item class.
- Basic melee swing visual for every weapon comes from the shared profile table (client/fx/WeaponTrailProfiles.java:35-39): Katana white 0xFFFFFF reach 2.0 sweep 150 fade 5 no tip particle; Z Sword 0xBFE6FF reach 2.1 sweep 140 fade 6 SPARK tip; Saber 0x66CCFF reach 2.0 sweep 160 fade 4 SPARK tip; Brave Sword 0xFFD54A reach 2.1 sweep 145 fade 6 EMBER tip; Grand Sword 0xFFB347 reach 2.35 sweep 165 fade 8 alpha 1.05 EMBER tip. All five are Kind.ARC, so the basic swing is the same arc ribbon shape for all five, differing only in color, size, fade, and tip particle.
- `DbaImpactFx.heavy` (fx/DbaImpactFx.java:16-20) means: screen shake intensity 2.2 for 12 ticks with 3 ticks hitstop inside 8 blocks (28 block radius, distance falloff), a SPARK x40 plus WIND x16 particle burst scaled by effect density (fx/DbaImpactFx.java:23-29), and the DbaSounds.HEAVY_IMPACT sound. Weapons below list "ImpactFx.heavy" where the code calls it; that is the only screen shake any of these five weapons produce.
- No weapon in this group uses a post-processing shader directly. All ability visuals are physical 3D entities (GeckoLib-style models driven by synced data), particles, and sounds. Every visual entity class below is a pure data carrier server side: it syncs scale, tilt, lifetime, and caster id, self-discards at its lifetime, and does no damage unless stated.

---

## 1. Katana (item/KatanaItem.java, 440 lines)

Identity in code: "Supreme Speed Weapon" (KatanaItem.java:44-48).

### Base stats and passives
- Attack damage modifier +749, so 750 total base damage; attack speed modifier -1.0 (KatanaItem.java:60-77).
- Passive while held (mainhand): nameplate distance -48 (KatanaItem.java:81-83) and air drag x0.50 via ADD_MULTIPLIED_BASE -0.50 (KatanaItem.java:84-86). No tooltip or effect tells the player either exists.
- Melee hit cost: `drainKiDiscrete(player, 30.0, 7)` per hit (KatanaItem.java:95), equal to 0.175% of max Ki per hit.

### Ability 1: Flashdraw (left click, hold to charge, release to execute)
- Trigger: left-click hold ticks `onLeftClickChargeTick` (KatanaItem.java:103), release `onLeftClickRelease` (KatanaItem.java:134). Max charge constant 100 ticks / 5 s (KatanaItem.java:50).
- Cost: Ki drain 30% of max per minute while charging (KatanaItem.java:105). Running out of Ki force-releases the move (KatanaItem.java:105-108).
- Execution: finds the best look-direction target within a 16 block box (scaled by movement limiter), then chains to up to 4 more enemies within 12 blocks of each other, 5 targets max (KatanaItem.java:155-199). Player gains Blade Guard for 20 ticks (KatanaItem.java:202). Per target: a HollowAfterimageEntity is left at the player's current position, the player teleports 1.5 blocks past the target, target gets Silent Mark 20 ticks and Cinematic Tracking 25 ticks (KatanaItem.java:206-218).
- Damage: `(500 + Strength x 2.5) x streakBonus`, streakBonus = 1.0 + 0.2 per chain index (KatanaItem.java:217). Damage lands 6 ticks later via a scheduled server task, together with the visual (KatanaItem.java:222-240). With no target, it is a freeform 14 block teleport dash with no damage (KatanaItem.java:242-246).
- Cooldown: 30 ticks / 1.5 s on the item (KatanaItem.java:254). Blocked entirely while immobilized (KatanaItem.java:150-153).
- Visuals and audio: KatanaChargeEntity while charging (entity/KatanaChargeEntity.java:18-20: 3D iaido stance rings, hilt spark matrix, converging silver motes; follows the caster, self-discards 6 ticks after updates stop, entity/KatanaChargeEntity.java:62-94). ENCHANTMENT_TABLE_USE every 20 ticks, pitch 1.4 + 0.4 x charge (KatanaItem.java:126). Per delayed hit: SwiftCrescentEntity at the target (entity/SwiftCrescentEntity.java:16-19, volumetric crescent, tilt alternates +/-35 plus 15 per index, scale 1.0 + 0.15 per index, lifetime 10 ticks), PLAYER_ATTACK_CRIT and DbaSounds.BLADE_SHING at pitch 1.5 + 0.1 per index (KatanaItem.java:236-239). ANVIL_LAND sheathe ring at the end (KatanaItem.java:250-251).

### Ability 2: Iaijutsu, Heaven Splitter (right click, hold to charge, release)
- Trigger: `use` starts item use if the player has any Ki (KatanaItem.java:262-268); BLOCK use animation; charge in `onUseTick` (KatanaItem.java:281), release in `releaseUsing` (KatanaItem.java:305). Max charge 100 ticks (KatanaItem.java:51), charge ratio floored at 0.1 on release (KatanaItem.java:322).
- Cost: Ki drain 30% of max per minute while holding (KatanaItem.java:282). No discrete release cost.
- Execution: player is held nearly still while charging (KatanaItem.java:294). Dash distance 16 + 8 x chargeRatio blocks (16 to 24), hit box inflates 2.0 + 2.0 x chargeRatio (KatanaItem.java:326-336). Player teleports to the end point (KatanaItem.java:342). Every living entity in the box takes the damage immediately (no delay).
- Damage: `1000 + 1000 x chargeRatio + Strength x 4.0 x chargeRatio` (KatanaItem.java:378-380), so 1100 + 0.4 x Strength at minimum charge and 2000 + 4.0 x Strength at full charge.
- Cooldown: 80 ticks / 4 s (KatanaItem.java:428). Side rule: if any target dies to Heaven Splitter, the Flashdraw cooldown is removed (KatanaItem.java:401-404, removes the cooldown of the Katana item id, which is the shared item cooldown).
- Visuals and audio: KatanaAimGuideEntity while charging (entity/KatanaAimGuideEntity.java:17-20: 3D laser guide, reticle brackets, targeting beam, range synced as 24.0). BEACON_ACTIVATE plus DbaSounds.ABILITY_CHARGE every 20 ticks (KatanaItem.java:305-307). On release: HollowAfterimageEntity at the start point; a cascade of SwiftCrescentEntity slashes along the whole path, count = dashDist / 2.2 (at least 7), scale 1.35 + 0.55 x charge, zig-zag lateral offsets, lifetimes 14-17 (KatanaItem.java:346-370); two cross-cleave SwiftCrescentEntity per target at +/-40 degrees, scale x1.25, lifetime 14 (KatanaItem.java:388-399); one HeavenSplitterEntity spanning the path (entity/HeavenSplitterEntity.java:16-19, towering dimensional slash, lifetime 20, scale 1.0 + 0.5 x charge). Audio stack on release: PLAYER_ATTACK_SWEEP, PLAYER_ATTACK_CRIT, BLADE_SHING, LIGHTNING_BOLT_THUNDER, DbaSounds.LIGHTNING_ZAP, ANVIL_LAND (KatanaItem.java:413-424).
- HollowAfterimageEntity (shared with Saber): ghost player clone carrying the owner's skin and hair colors (entity/HollowAfterimageEntity.java:33-48), lives 100 ticks, and every 5 ticks applies Dark Faded for 60 ticks to enemies within 4 blocks (entity/HollowAfterimageEntity.java:95-108). So the "decoration" afterimage is also a debuff aura, which neither weapon's text mentions.

### Katana gaps
1. Flashdraw charge is cosmetic for power. Release damage, dash distance, and chain count never read chargeTicks except for the charge entity's visual ratio (KatanaItem.java:217 uses no charge term; freeform dash is a flat 14 blocks at :244). Holding 5 s and tapping do the same Flashdraw.
2. Flashdraw has no release sound of its own when the chain is empty: only the ANVIL_LAND sheathe plays (KatanaItem.java:250-251). No whoosh, no trail entity on the freeform dash path (Heaven Splitter gets a full crescent cascade; Flashdraw's no-target dash gets nothing).
3. No screen shake or hitstop anywhere in either ability, despite 2000+ damage single hits. ImpactFx is never called in KatanaItem.
4. The delayed Flashdraw damage (6 ticks) is scheduled with `level.getServer().execute` (KatanaItem.java:224), not a tick-scheduled task: if the target dies, logs, or changes dimension in that window the strike still fires on a stale reference, and there is no check at fire time.
5. Charge audio is a single repeated ENCHANTMENT_TABLE_USE / BEACON_ACTIVATE loop shared in feel with Brave Sword's charge (same two-sound pattern), not a Katana-specific rising sound.

---

## 2. Z Sword (item/ZSwordItem.java, 315 lines)

Identity in code: "Divine Heavy Weapon" (ZSwordItem.java:39-55).

### Base stats and passives
- Attack damage modifier +999, so 1000 total base damage; attack speed modifier -3.0, the slowest in the group (ZSwordItem.java:71-88).
- Passive while held: friction x1.80 via ADD_MULTIPLIED_BASE +0.80 (ZSwordItem.java:91-93).
- Melee hit cost: `drainBothDiscrete(player, 75.0, 50.0, 15)` (ZSwordItem.java:101), equal to 0.9375% of max Ki and 0.625% of max Stamina per hit.

### Ability 1: Z Shockwave (left click, hold to charge, release to fire)
- Trigger: `onLeftClickChargeTick` (ZSwordItem.java:109), `onLeftClickRelease` (ZSwordItem.java:142). Max charge 300 ticks / 15 s (ZSwordItem.java:58).
- Cost: Ki 75% per minute plus Stamina 50% per minute while charging (ZSwordItem.java:112). Running dry force-fires at the accumulated charge (ZSwordItem.java:112-116). While charging the player repeatedly gets Ancient Weight amp 0 for 10 ticks (ZSwordItem.java:124).
- Damage: `300 + 900 x chargeRatio + Strength x 3.5 x chargeRatio` (ZSwordItem.java:155-157), charge ratio floored at 0.1. Projectile speed 1.4 + 1.4 x charge (ZSwordItem.java:161-163).
- Cooldown: `20 + 40 x chargeRatio` ticks (24 to 60 ticks, 1.2 to 3 s) (ZSwordItem.java:179-180).
- Projectile: ZShockwaveEntity (entity/ZShockwaveEntity.java). Hitbox half width 3.0 + 5.0 x charge (up to 8, so 16 blocks wide at full charge), half height 1.6 (:128-131). Each entity hit once; knockback 1.8 + 2.2 x charge horizontal, 0.4 + 0.3 x charge up (:155-158). Dies on block contact unless piercing, hard lifetime 80 ticks (:171-181). At charge >= 0.90: pierces blocks and roots (constructor, :86-90); at tick 3 it spawns 2 trailing sub-waves at 0.7 x charge, 0.45 x damage, 0.65 x speed (:138-152); a max-charge direct hit teleports the target 2 blocks down, applies Movement Curse amp 9 for 80 ticks, and deals a flat extra 250 crush damage (:161-176).
- Visuals and audio: ZChargeEntity while charging (entity/ZChargeEntity.java:17-20: golden vortex, counter-rotating rings, sacred prisms, ground mandala). BEACON_AMBIENT every 20 ticks rising in pitch (ZSwordItem.java:135-137). On fire: PLAYER_ATTACK_SWEEP plus GENERIC_EXPLODE plus DbaImpactFx.heavy at the caster (ZSwordItem.java:170-176), which is screen shake 2.2 / hitstop 3 plus the SPARK and WIND burst plus HEAVY_IMPACT sound. Recoil: player launched backward 0.4 + 1.4 x charge with a small hop (ZSwordItem.java:166-168).

### Ability 2: Katchin Weight / Gravity Slam (right click, hold stance, release to slam)
- Trigger: `use` requires any Stamina (ZSwordItem.java:188-194); stance in `onUseTick` (ZSwordItem.java:206), slam in `releaseUsing` (ZSwordItem.java:233). Power ratio caps at 200 ticks / 10 s (ZSwordItem.java:215). Releasing before 10 ticks does nothing at all, no slam and no cooldown (ZSwordItem.java:245).
- Cost: Stamina 75% of max per minute while holding (ZSwordItem.java:208). No Ki cost.
- Stance effects: player rooted in place every tick (ZSwordItem.java:221), Ancient Weight amp 1 refreshed (ZSwordItem.java:224). The class header promises knockback immunity and heavy damage reduction (ZSwordItem.java:48-54); in the item code the only stance mechanics are the root and the Ancient Weight effect.
- Slam: radius 6 + 12 x powerRatio blocks (6 to 18) (ZSwordItem.java:247). Damage `400 + 800 x powerRatio + Strength x 3.0 x powerRatio` (ZSwordItem.java:257-259) to everything in the box. Targets get Earth Shatter 140 ticks, Cinematic Tracking 50 ticks, Movement Curse amp 9 for 40 ticks (ZSwordItem.java:271-277), then a DelayedLaunchEntity throws them up and outward after 40 ticks: horizontal 1.2 + 1.8 x powerRatio, vertical 1.0 + 0.8 x powerRatio, plus bounciness +0.90 and air drag -0.50 modifiers (entity/DelayedLaunchEntity.java:38-66).
- Cooldown: `60 + 80 x powerRatio` ticks (68 to 140 ticks, 3.4 to 7 s) (ZSwordItem.java:302).
- Visuals and audio: ZStanceAuraEntity while holding (entity/ZStanceAuraEntity.java:16-19: gravity suction funnel, Katchin monoliths, contracting event horizon rings). WARDEN_SONIC_CHARGE every 30 ticks (ZSwordItem.java:240-242). On release: ZGravitySlamEntity (entity/ZGravitySlamEntity.java:15-19: canyon fissure trenches, erupted monoliths, shockwave dome; lifetime 45-60 ticks), SlamDebris.burst with the Z_GRAVITY profile on its first tick (entity/ZGravitySlamEntity.java:60-64; fx/SlamDebris.java:24-62: real ground-block debris, POOF dust, SPARK accent, density scaled, never destroys blocks). GENERIC_EXPLODE plus ANVIL_LAND plus DbaImpactFx.heavy (ZSwordItem.java:293-297).

### Z Sword gaps
1. Both charge visuals are the same "rings around the player" family as Katana and Brave charge entities (rotating rings plus prisms/runes, differing mainly in color), so three of the five weapons read similarly while charging.
2. The stance's promised defense is not in the item: no damage-reduction or knockback code in `onUseTick` beyond rootInPlace and the Ancient Weight effect instance (ZSwordItem.java:221-224). Whatever protection exists lives entirely inside AncientWeightEffect, unverifiable from the weapon.
3. ZShockwaveEntity has no particle trail or sound of its own while traveling (entity/ZShockwaveEntity.java tick, :113-183): all audio is at the caster at fire time, so a 16 block wide wave crossing 80 ticks of travel is silent after launch.
4. Sub-waves reuse the identical entity and model at 0.7 x charge with no visual distinction (entity/ZShockwaveEntity.java:145-149), and the root-and-crush Tweak C has no dedicated effect: the target just teleports down 2 blocks (entity/ZShockwaveEntity.java:167-175).
5. Releasing the right click before 10 ticks silently wastes the stance drain with no feedback (ZSwordItem.java:245).

---

## 3. Saber (item/SaberItem.java, 964 lines)

Identity in code: "High-Speed Phasing & Blitz Chain Weapon" (SaberItem.java:49-53). By far the largest weapon class in the group.

### Base stats and passives
- Attack damage modifier +819, so 820 total base damage; attack speed modifier 0.0 under a dedicated id, because the Blitz speed buff rewrites it at runtime (SaberItem.java:113-130).
- Passives while held: friction x0.25 (modifier -0.75) and air drag x0.35 (modifier -0.65) (SaberItem.java:133-138).
- Melee hit cost: `drainStaminaDiscrete(player, 100.0, 5)` (SaberItem.java:142), equal to about 0.417% of max Stamina per hit.

### Ability 1: Blitz Flurry (left click, repeated press-and-release chain)
- Trigger: `onBlitzTick` while held (SaberItem.java:151), `onBlitzRelease` on release (SaberItem.java:415). The chain only continues if the player releases and re-presses: after each arrival the sequence waits for release (SaberItem.java:161-163, 296-299). A 20 tick continuation window after release keeps the chain alive; expiry, stamina exhaustion, switching items, or immobilization ends it and fires the finisher (SaberItem.java:169-175, 893-927).
- Cost: Stamina 100% of max per minute while blitzing (SaberItem.java:169). No per-hit discrete cost and no Ki cost.
- Targeting: first target needs a tight aim cone (look dot > 0.96, about 16 degrees) within 64 blocks with line of sight; chained targets are never re-targeted (SaberItem.java:439-475). On each launch, every living entity within 64 blocks gets Judgement Lock for 80 ticks (SaberItem.java:229-234).
- Travel and hit: the player is teleported along a parabolic arc over exactly 6 ticks to directly above the target's head, camera auto-pans toward the pre-computed next target (SaberItem.java:247-342). On arrival: velocity zeroed, target gets Movement Curse amp 9 and Cinematic Tracking with duration 999999 (effectively permanent until the finisher cleanup removes them, SaberItem.java:257-259), and takes `280 + Strength x 1.8` damage (SaberItem.java:264-265). The code comments state this damage is caught and delayed by DelayedDamageMixin because of the Movement Curse (SaberItem.java:262-263; mixin/DelayedDamageMixin.java exists and is the mechanism).
- Per-hit visuals and audio: PLAYER_ATTACK_CRIT plus BLADE_SHING (SaberItem.java:267-270); one or two SaberSlashEntity crescents at the target (entity/SaberSlashEntity.java:13-16, volumetric crescent; scale 1.0 normal / 1.4 strong, lifetime 10, second slash 0.85 / 1.3, lifetime 8) (SaberItem.java:280-294). An escalating attack speed buff is applied per hit (see gap 1). Launch cue: PLAYER_ATTACK_SWEEP at pitch 1.6 (SaberItem.java:236-237).
- Finisher, Best-Fit Line Snap: fires when the chain ends with at least one chained target. Damage `850 + Strength x 4.0` per target (SaberItem.java:552). Targets are floated over 12 ticks onto a best-fit 3D line through their positions (covariance plus power iteration, SaberItem.java:497-580, ticked in `tickServer` SaberItem.java:798-889), each gets Judgement Lock 30 ticks (SaberItem.java:575). At completion: SaberDimensionalLineSlashEntity along the line (entity/SaberDimensionalLineSlashEntity.java:15-18, reality cut beam, scale 1.2, lifetime 22), two SaberSlashEntity cross bursts per target (45 and -45 degrees), Movement Curse and Cinematic Tracking removed, finisher damage applied, bounciness +0.80 modifier (SaberItem.java:843-879). Audio at the centroid: ANVIL_LAND, PLAYER_ATTACK_CRIT, BLADE_SHING, LIGHTNING_BOLT_THUNDER, LIGHTNING_ZAP (SaberItem.java:886-895). Cooldown 100 ticks / 5 s, applied only when the finisher actually had targets (SaberItem.java:579-580).

### Ability 2: Flash Step (right click tap or hold)
- Trigger: `use` performs one step immediately (SaberItem.java:593-603); holding repeats a step every 7 ticks while charges remain (SaberItem.java:617-626). Use animation is NONE (SaberItem.java:612-614).
- Charges: 3, stored per player, 1 recharges every 60 ticks (charge deducted at SaberItem.java:652, default 3 at :761-763, recharge in tickServer at :914).
- Cost per step: `drainKiDiscrete(player, 75.0, 40)` (SaberItem.java:639), equal to 2.5% of max Ki, matching the comment at :638.
- Execution: if an enemy is in a dot > 0.82 cone within 16 blocks (scaled by movement limiter), the player teleports 1.5 blocks behind that enemy and copies its rotation (SaberItem.java:660-668, 695-699). Otherwise a 7 block directional dash that can phase 1.5 blocks past a thin wall if air is on the other side, else stops 0.5 short (SaberItem.java:669-686). Opens a 15 tick Perfect Dodge window (SaberItem.java:656).
- Path damage: every enemy crossed in the start-to-end box takes `400 + Strength x 2.0` and gets a SaberSlashEntity (SaberItem.java:725-735).
- Visuals and audio: HollowAfterimageEntity at the origin (shared with Katana, debuff aura included), SaberVoidTearEntity at origin (scale 1.0) and destination (scale 1.15, destination flag) (entity/SaberVoidTearEntity.java:14-17, void tear plus sonic rings, lifetime 12). Destination audio: ENDERMAN_TELEPORT, PLAYER_ATTACK_SWEEP, SLASH_WHOOSH (SaberItem.java:702-707). Action bar message shows remaining charges (SaberItem.java:737).
- Perfect Dodge (passive proc): if the player is hit during the 15 tick window, other systems call `triggerPerfectDodge` (SaberItem.java:786-805): charges reset to 3, SaberDodgeSparkEntity burst (entity/SaberDodgeSparkEntity.java:13-16, golden deflection shatter, scale 1.25, lifetime 14), SHIELD_BLOCK plus PLAYER_ATTACK_CRIT plus BLADE_SHING, action bar message. No damage component.

### Saber gaps
1. REAL BUG: the Blitz hit counter is never incremented. `seq.totalHits` is initialized to 0 (SaberItem.java:67) and read at :209, :273, :275, and :296, but no line in the file ever adds to it. Consequences: the "strong slash" threshold is always computed from 0, so strong slashes effectively only occur when the random threshold roll lands at its minimum pattern, and `applyEscalatingSpeedBuff` always receives totalHits + 1 = 1, so the attack speed bonus is permanently 6.6 instead of escalating toward the 24.0 cap (SaberItem.java:453-465). The escalating-speed identity of the weapon does not actually escalate.
2. Generic reuse: the afterimage is Katana's HollowAfterimageEntity, the finisher audio stack (ANVIL_LAND + CRIT + BLADE_SHING + thunder + zap) is the same recipe as Katana's Heaven Splitter release stack, and SaberSlashEntity is the same crescent family as Katana's SwiftCrescentEntity. Saber's distinct pieces are only the Void Tear and the Line Slash.
3. Blitz has no screen shake or hitstop at all, and the finisher that snaps up to a whole chain of enemies has no ImpactFx call; its feedback is sound plus the line entity only.
4. Immobilized check silently deletes the sequence without the finisher or a message (SaberItem.java:156-159), unlike the stamina-out path which messages the player (SaberItem.java:173).
5. Movement Curse and Cinematic Tracking are applied with duration 999999 on arrival (SaberItem.java:258-259) and only removed by finisher completion or the disconnect cleanup in tickServer (SaberItem.java:803-816). Any path where a sequence is dropped without the finisher running risks leaving a target rooted effectively forever; the code relies on every exit path remembering to call the finisher.
6. Flash Step's wall phase checks only two block positions (past the wall and the one above it, SaberItem.java:676-678), so stepping into a 1-block gap at feet level with a blocked head block teleports the player into a wall face position offset instead; minor, but it reads unfinished next to the rest of the polish.

---

## 4. Brave Sword (item/BraveSwordItem.java, 370 lines)

Identity in code: "Heroic Legendary Sword, Tapion's Sword" (BraveSwordItem.java:44-47).

### Base stats and passives
- Attack damage modifier +799, so 800 total base damage; attack speed modifier -1.8 (BraveSwordItem.java:62-79).
- Passive while held: air drag x0.50 (BraveSwordItem.java:82-84).
- Melee hit cost: `drainBothDiscrete(player, 25.0, 20.0, 10)` (BraveSwordItem.java:90), equal to about 0.208% of max Ki and 0.167% of max Stamina per hit.
- Combo state is stored on the item stack itself in CUSTOM_DATA "Combo" (BraveSwordItem.java:101-113). It never decays and is never reset except by firing the finisher; swapping to a second Brave Sword starts a fresh count, and the count survives indefinitely between fights.

### Ability 1: Brave Assault (left click hold, auto-striking combo)
- Trigger: `onLeftClickAssaultTick` per payload tick (BraveSwordItem.java:119). Strike interval `max(3, 8 - combo/2)` ticks, so strikes accelerate from every 8 ticks to every 3 ticks as combo rises (BraveSwordItem.java:129-131). Only the first living entity in a 3.5 block look box (inflated 1.5) is hit per strike (BraveSwordItem.java:133-143).
- Cost: Ki 25% per minute plus Stamina 20% per minute while held (BraveSwordItem.java:115). Note this drain runs per payload tick whether or not a strike lands.
- Standard hit: combo +1, damage `400 + Strength x 2.0` (BraveSwordItem.java:177-178).
- Every 5th hit: fires BraveSlashEntity at `hitDmg x 1.5` (so `600 + Strength x 3.0`), speed 1.8 along look (BraveSwordItem.java:181-188). The entity pierces, hits each entity once in a 3.0 x 1.2 inflated box, knockback 1.4, CRIT particles x10 per hit, lifetime 40 ticks (entity/BraveSlashEntity.java:44-95).
- Finisher at combo 10: player teleports 2.0 blocks past the target (BraveSwordItem.java:147-148), target takes `900 + Strength x 4.0` directly (BraveSwordItem.java:150-151), gets a bounciness +0.85 modifier (BraveSwordItem.java:154-161), and a BraveCrossSlashEntity spawns at its center with the same finisher damage value and scale 1.25 (BraveSwordItem.java:164-167). The cross slash then applies that same damage again to everything in radius 3.5 x 1.25 on its first 2 ticks (entity/BraveCrossSlashEntity.java:56-84), so the primary target is effectively hit twice for finisher damage (direct plus entity), and nearby enemies take a full finisher hit, not a reduced splash. Combo resets to 0 (BraveSwordItem.java:174). Audio: PLAYER_ATTACK_SWEEP, SLASH_WHOOSH, GENERIC_EXPLODE, plus DbaImpactFx.heavy at the target (shake, hitstop, burst, HEAVY_IMPACT) (BraveSwordItem.java:169-172). Action bar "BRAVE FINISHER!" (BraveSwordItem.java:175).
- Standard hit feedback: CRIT particles x8 at the target, PLAYER_ATTACK_SWEEP plus SLASH_WHOOSH with pitch rising 0.08 per combo, action bar combo counter every hit (BraveSwordItem.java:191-195).
- Cooldown: none. The assault has no item cooldown at any point; only the drains gate it.

### Ability 2: Heroic Dash (right click, hold to charge, release)
- Trigger: `use` requires any Ki (BraveSwordItem.java:204-209); charge in `onUseTick` (BraveSwordItem.java:219), release in `releaseUsing` (BraveSwordItem.java:245). Max charge 100 ticks (BraveSwordItem.java:50), ratio floored at 0.1 (BraveSwordItem.java:256).
- Cost: Ki 15% of max per minute while charging (BraveSwordItem.java:223). While charging, Heroic Focus amp 0 is refreshed every tick (BraveSwordItem.java:232).
- Execution: dash distance `(14 + 10 x chargeRatio) x ccMult` blocks (14 to 24), path box inflates 2.0 + 2.5 x chargeRatio (BraveSwordItem.java:268-280). Player teleports to the end (BraveSwordItem.java:297). Blocked while immobilized, with a message (BraveSwordItem.java:262-265).
- Damage: `600 + 1400 x chargeRatio + Strength x 4.0 x chargeRatio` to every target in the path box (BraveSwordItem.java:305-312), so 740 + 0.4 x Strength minimum, 2000 + 4.0 x Strength at full charge. Targets also get Cinematic Tracking 20 ticks, bounciness +0.80, air drag -0.40 modifiers (BraveSwordItem.java:311-330).
- Secondary shockwave: if any path target died OR charge >= 0.9, a radial burst at the destination hits all other enemies within 6 + 2.5 x chargeRatio blocks for 0.4 x the dash damage, and spawns BraveShockwaveEntity (BraveSwordItem.java:336-352; entity/BraveShockwaveEntity.java:13-16, beveled ring plus valor needles, lifetime 18, purely visual, the damage is applied directly by the item).
- Cooldown: 100 ticks / 5 s (BraveSwordItem.java:359).
- Visuals and audio: BraveChargeEntity while charging (entity/BraveChargeEntity.java:16-18: heroic runes, valor shards, core matrix; same ring-family charge visual as Katana and Z Sword). BEACON_ACTIVATE plus ABILITY_CHARGE every 20 ticks, the same charge sound pair as Katana's Heaven Splitter (BraveSwordItem.java:245-248). BraveRushTrailEntity spanning start to end (entity/BraveRushTrailEntity.java:15-18: Mach cone and streamlines, length synced to the dash, scale 1.0 + 0.5 x charge, lifetime 14). PLAYER_ATTACK_SWEEP plus SLASH_WHOOSH on release, GENERIC_EXPLODE plus ImpactFx.heavy when the shockwave triggers (BraveSwordItem.java:349-356).

### Brave Sword gaps
1. The finisher double-hits its primary target for full finisher damage (direct at BraveSwordItem.java:151 plus BraveCrossSlashEntity at entity/BraveCrossSlashEntity.java:77) and full-finisher-damages bystanders; nothing in the code marks this as intentional splash design, and the entity's own header says it "slices through the target and epicenter" (entity/BraveCrossSlashEntity.java:19-21). Looks like an unfinished balance pass, not a bug in the entity itself.
2. The 5th-hit crescent (BraveSlashEntity) and the assault's standard hits use only vanilla CRIT particles plus two sounds; the crescent is the same projectile model family the Grand Sword's release wave uses, recolored by its own renderer.
3. Combo never decays (BraveSwordItem.java:101-113 has no timer anywhere in the class), so a player can bank 9 hits, walk away, and open the next fight one hit from a finisher.
4. The charge visual and charge audio are the generic shared pattern (rings plus BEACON_ACTIVATE / ABILITY_CHARGE), identical in structure to Katana's right click; Heroic Focus is applied silently with no visual of its own in the item code.
5. The assault drain runs even when no target is in the box (BraveSwordItem.java:115 runs before target selection at :138), so holding left click into empty air burns Ki and Stamina at full rate with zero feedback.

---

## 5. Grand Sword (item/GrandSwordItem.java, 293 lines)

Identity in code: "Balanced legendary greatsword: AoE, defense, ranged energy, and team utility" (GrandSwordItem.java:40-51).

### Base stats and passives
- Attack damage modifier +899, so 900 total base damage; attack speed modifier -2.4 (GrandSwordItem.java:65-82).
- Passive while held: friction x1.75 (modifier +0.75) (GrandSwordItem.java:85-87).
- Melee hit cost: `drainKiDiscrete(player, 90.0, 10)` (GrandSwordItem.java:93), equal to 0.75% of max Ki per hit, the most expensive basic swing in the group.

### Ability 1: Grand Cyclone (left click hold to spin, release for finisher)
- Trigger: `onLeftClickSpinTick` (GrandSwordItem.java:99), `onLeftClickSpinRelease` (GrandSwordItem.java:179). Spin constant MAX_LEFT_SPIN_TICKS 140 / 7 s (GrandSwordItem.java:52), but nothing in the tick or release code reads it: there is no auto-release or power scaling at 140 ticks.
- Cost: Ki 90% of max per minute while spinning (GrandSwordItem.java:104). Running out force-releases (GrandSwordItem.java:104-108).
- Spin behavior: server rotates the player 15 degrees per tick accelerating to 35 degrees per tick by tick 100 (GrandSwordItem.java:117-122), forward drift 0.06 per tick scaled by movement limiter (GrandSwordItem.java:125-128).
- Blade shards: every 3 ticks a GrandBladeShardEntity is fired along the current facing with +/-0.08 spread at speed 1.5, damage `80 + Strength x 0.5` (GrandSwordItem.java:131-142). Shard behavior (entity/GrandBladeShardEntity.java): direct hit applies its damage plus Valor Stun amp stacking (60 ticks, amp +1 per shard, cap 5) plus Cinematic Tracking 60 ticks (entity/GrandBladeShardEntity.java:140-166); on block impact it embeds for up to 200 ticks as a ground trap that applies the same damage and stun stack to the first enemy stepping within about 0.85 blocks, then shatters (entity/GrandBladeShardEntity.java:88-137); flying shards live at most 60 ticks with gravity (entity/GrandBladeShardEntity.java:184-205). Shard audio: PLAYER_ATTACK_SWEEP plus SLASH_WHOOSH per shard volley from the item (GrandSwordItem.java:139-142), ARROW_HIT on embed, PLAYER_ATTACK_SWEEP plus ARMOR_STAND_HIT on trap trigger (entity/GrandBladeShardEntity.java:124-127, 162-163).
- Tracking: enemies within 14 blocks that already have Valor Stun get Cinematic Tracking refreshed (GrandSwordItem.java:146-157).
- Projectile defense: projectiles within 4 blocks not owned by the player (excluding its own shards and waves) have ownership transferred to the player and velocity reversed x -1.4, spawning GrandClashSparkEntity (entity/GrandClashSparkEntity.java:15-18, clash rings and golden sparks, lifetime 10) plus SHIELD_BLOCK sound (GrandSwordItem.java:160-177).
- Release finisher: frontal arc check (dot > 0.35) within 6 blocks, damage `450 + Strength x 2.0`, Cinematic Tracking 20 ticks, bounciness +0.85, knockback 1.4 forward and 0.3 up applied by direct setDeltaMovement (GrandSwordItem.java:184-220). Then fires GrandCrescentWaveEntity at `600 + Strength x 2.5`, speed 2.2 (GrandSwordItem.java:225-228): piercing wave, half width 3.2, half height 1.2, knockback 1.5, dies on block contact, 80 tick life (entity/GrandCrescentWaveEntity.java:86-131). Audio: PLAYER_ATTACK_SWEEP plus GENERIC_EXPLODE plus DbaImpactFx.heavy (GrandSwordItem.java:231-235). Cooldown 40 ticks / 2 s (GrandSwordItem.java:238).
- Doc mismatch inside the class header: the header promises the spin "damages enemies repeatedly (every 3 ticks), knocks them away" (GrandSwordItem.java:42-45). The tick code deals no direct spin damage and no knockback at all; the only spin damage is the shard projectiles. A player standing inside the spin next to the user takes nothing unless a shard happens to face them.

### Ability 2: Valor Field (right click hold)
- Trigger: `use` requires any Ki (GrandSwordItem.java:246-251); `onUseTick` just ensures a ValorFieldEntity exists (GrandSwordItem.java:266-276); release discards it and applies cooldown (GrandSwordItem.java:279-289). The field entity does all the work.
- Cost: Ki 90% of max per minute, drained inside the entity (entity/ValorFieldEntity.java:117-124). Running out stops the item use and drops the field. The item's own onUseTick does not drain, so the cost is invisible from the item class.
- Cooldown: 20 ticks / 1 s on release (GrandSwordItem.java:282).
- Field effects (entity/ValorFieldEntity.java, radius 9.0 at :41): follows the caster every tick (:129). Every 10 ticks: caster gets Valor amp 2 for 40 ticks; other players (all players count as allies, :270-275) get Valor amp 1; every other living entity in radius gets Valor Stun amp 1 for 40 ticks plus pressure damage `55 + Strength x 0.45` plus Cinematic Tracking 40 ticks (:134-165). That pressure applies every 10 ticks, so about `110 + Strength x 0.9` per second per enemy. BEACON_AMBIENT hum every 30 ticks (:168-171).
- Projectile stasis: enemy projectiles entering the 9 block field are frozen in place (velocity zeroed, no-gravity set), keeping their original velocity stored (entity/ValorFieldEntity.java:175-205). If the field moves away they resume flight (:214-222); if a living entity walks into one it takes the projectile's full damage (16 x speed for generic projectiles, the blast's own damage for KiBlastEntity) and the projectile detonates or breaks (:229-255); after 200 ticks suspended they break with an ITEM_BREAK sound (:258-263). The entity header says stasis lasts "up to 4s" (:35-38) but the code uses 200 ticks / 10 s (:258). Header also promises ally Strength II / Resistance II and enemy Slowness II (:31-34); the code instead applies the custom Valor and Valor Stun effects, so the header describes an older design.

### Grand Sword gaps
1. Biggest gap in the group: the Cyclone spin itself has no damage, no swing visual entity, and no particle ring in the item code. While spinning, the only effects are shards every 3 ticks and clash sparks on deflection. For a signature spin move, the blade arc that should sell it does not exist server side; whatever the client weapon animation shows is all there is.
2. MAX_LEFT_SPIN_TICKS (140) is dead code: never referenced by tick or release (GrandSwordItem.java:52). Spin power, shard damage, and the finisher do not scale with spin duration at all, so a 0.5 s spin and a 7 s spin release identically.
3. Valor Field is the only right-click in the group with no charge or release moment: it appears instantly at full 9 block power, has no spawn sound, no spawn visual burst in the item or entity code, and ends with a plain discard (GrandSwordItem.java:279-289). Its only audio is the BEACON_AMBIENT hum loop inside the entity.
4. Valor Field pressure damage has no per-hit effect at all: no particles, no sound, no hit flash call in the entity (:152-158), enemies inside the dome simply lose health twice a second under a stun effect.
5. Dead imports and leftovers signal unfinished cleanup: GrandSwordItem imports DustParticleOptions, ParticleTypes, and MobEffects and never uses any of them (GrandSwordItem.java:9-10, 17); ValorFieldEntity likewise imports DustParticleOptions, ParticleTypes, and MobEffects unused (entity/ValorFieldEntity.java:6-7, 14), and GrandCrescentWaveEntity imports DustParticleOptions and ParticleTypes unused (entity/GrandCrescentWaveEntity.java:3-4). GrandSwordItem also imports PlayerStats unused (:3).
6. The finisher knockback writes velocity directly with setDeltaMovement (GrandSwordItem.java:215-216) instead of MovementPhysics like the rest of the wave code, and the spin drift and rotation are also hand-rolled; inconsistent with the shared movement API the other four weapons use.

---

## Cross-group summary of effect gaps

- Charge visuals are one shared recipe (rotating 3D rings plus prisms/runes around the caster) for Katana left, Katana right (aim guide variant), Z Sword left, Z Sword stance, and Brave right. Only color and accessory shapes differ.
- Charge audio is two shared recipes: ENCHANTMENT_TABLE_USE / BEACON_ACTIVATE plus ABILITY_CHARGE loops (Katana, Brave) and BEACON_AMBIENT / WARDEN_SONIC_CHARGE (Z Sword, Valor Field hum).
- Screen shake and hitstop (DbaImpactFx.heavy) exist only on Z Sword (both abilities), Brave finisher and Brave shockwave, and Grand release finisher. Katana and Saber, the two fastest and flashiest weapons, have none.
- Saber has a confirmed dead escalation system (totalHits never incremented). Grand Sword has a confirmed dead spin cap constant and a header that promises spin damage the code never deals. Brave Sword's finisher double-applies its damage through its own cross slash entity.
- Every weapon's basic swing is the same ARC ribbon from one profile table; per-weapon identity lives in the abilities, not the swing.
