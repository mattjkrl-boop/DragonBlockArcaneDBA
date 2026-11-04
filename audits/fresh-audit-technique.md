# Fresh Audit: Technique Weapons (Power Pole, Whis Staff, Bansho Fan, Blaster Gun, Spike Flail, War Mace)

Repo HEAD: 94708b6. Audit date: Nov 3, 2026. Method: clean slate. Every claim below was derived by reading the current code at this HEAD. No prior audit files were read. Paths are relative to the repo root. Item and entity files live under src/main/java/com/dragonblockarcanedba/. Renderers live under src/client/java/com/dragonblockarcanedba/client/render/.

## Existence check (from item/DbaItems.java and the item package)

All six weapons exist and are fully registered (registry key, item instance, creative tab output):

- Bansho Fan: key DbaItems.java:21-22, item DbaItems.java:274-275, class item/BanshoFanItem.java
- Whis Staff: key DbaItems.java:114-115, item DbaItems.java:365-366, class item/WhisStaffItem.java
- Power Pole: key DbaItems.java:192-193, item DbaItems.java:449-450, class item/PowerPoleItem.java
- War Mace: key DbaItems.java:219-220, item DbaItems.java:476-477, class item/WarMaceItem.java
- Spike Flail: key DbaItems.java:222-223, item DbaItems.java:479-480, class item/SpikeFlailItem.java
- Blaster Gun: key DbaItems.java:237-238, item DbaItems.java:494-495, class item/BlasterGunItem.java
- Blaster Ammo (support item for the gun): key DbaItems.java:240-241, item DbaItems.java:497-498, class item/BlasterAmmoItem.java

All six item classes implement item/geo/DbaGeoWeapon. GeckoLib models exist for all six under assets/dragonblockarcanedba/geckolib/models/item/ (power_pole, bansho_fan, blaster_gun, spike_flail, war_mace, whis_staff .geo.json), plus item model JSONs and *_geo_base.json files for all six.

Entity and renderer pairs per weapon:

- Power Pole: PowerPoleWhirlwind, PowerPoleExtension, PowerPoleImpact (entity/ and client/render/)
- Whis Staff: TimeShatter, TemporalRift
- Bansho Fan: BanshoCyclone, BanshoGale, BanshoShockwave, BanshoWindProjectile
- Blaster Gun: BlasterBolt, ErasureCannonBeam, ErasureChargeOrb
- Spike Flail: SpikeFlailSpin, SpikeFlailSwing
- War Mace: WarMaceRise, WarMaceSmash

Left-click routing for all six goes through network/DbaNetwork.java: Power Pole performWindSpin at line 480, Blaster onLeftClickBarrageTick at line 530, Bansho performGaleSwing at line 539, Whis performTemporalStrike at line 541, War Mace performSmash at line 543, Spike Flail performSwing at line 545.

## Cross-cutting findings (all six weapons)

1. No additive glow pass anywhere. All 16 technique renderers submit only on KiRenderHelper.kiRenderType() (entityTranslucentEmissive). A grep for kiAdditiveType() across all 16 renderer files returned zero matches. Against video point 1 (dense bright core), every effect in this group is a single translucent mesh stack with vertex alpha; nothing renders an additive core layer.
2. Particle counts are tiny everywhere, and zero for two weapons. Highest sustained emitter in the group is the Bansho cyclone at 2 WIND particles every 2 ticks (entity/BanshoCycloneEntity.java:125-137). Spike Flail and War Mace emit zero particles in all four of their entities combined. The video standard is layered rotating particle bands; no weapon here has a particle band system at all. Particles, where they exist, are single-type accents (WIND, SPARK, EMBER, KI_MOTE).
3. Debris count is zero everywhere. No weapon spawns individual tumbling debris pieces, block or otherwise. The closest things are baked mesh shards inside two renderers: 16 seeded diamond shards in PowerPoleImpactRenderer and 16 prism shards in TimeShatterRenderer. Both are fixed-count geometry drawn as part of one entity render, not simulated pieces, and neither follows a helical orbit.
4. Block interaction is zero everywhere. No weapon in this group breaks, moves, scars, cracks, or marks a block. Blocks only stop things: the Power Pole poke shortens against a block face (item/PowerPoleItem.java:229-235), the Blaster bolt and beam clip against blocks, the Bansho wind projectile clips against blocks. One exception in the other direction: the Bansho gale ignores blocks completely and flies through walls (see Bansho section).
5. Atmosphere is zero everywhere. No sky darkening, no light dimming, no haze, no lightning geometry in any of the six weapons. The Erasure beam plays the vanilla LIGHTNING_BOLT_THUNDER sound (entity/ErasureCannonBeamEntity.java:187-188); that is the only lightning-adjacent element in the group.
6. Lifecycles are short and fixed. Entity lifetimes in this group: 8, 10, 11, 12, 14, 16, 18, 25, 30, 50, 60 ticks. Most effects are at full strength almost immediately and end on a renderer fade. Only two mechanics have any build-up at all: the Blaster charge (100 ticks, orb grows, damage scales) and the Flail spin channel (32 ticks, but the payoff does not scale with channel time; see Flail section).
7. No finishers exist in this group. No weapon has a finisher trigger, a low-health execution, or a kill confirm effect in its item or entity code.
8. Damage numbers are very high and instant. Base melee totals: Power Pole 750, Whis 350, Bansho 400, Blaster 450, Flail 820, Mace 1300. Several abilities pay their full damage in a single tick with no wind-up, which compresses the spectacle window: the hit lands before any build-up could play.

## 1. Power Pole (item/PowerPoleItem.java)

Pieces: PowerPoleWhirlwindEntity (life 8 ticks, entity/PowerPoleWhirlwindEntity.java:25), PowerPoleExtensionEntity (life 12, entity/PowerPoleExtensionEntity.java:23), PowerPoleImpactEntity (life 14, entity/PowerPoleImpactEntity.java:22), renderers PowerPoleWhirlwindRenderer (402 lines), PowerPoleExtensionRenderer (355), PowerPoleImpactRenderer (325).

### Ability map

Melee (vanilla swing):
- Attribute damage 749 + 1 base = 750 total (item/PowerPoleItem.java:43-47). Attack speed modifier -2.0 (lines 50-57).
- hurtEnemy is an empty no-op by design (lines 74-78): a connecting vanilla swing deals only the attribute damage, no extra effect, no stamina.
- Passive while held: AIR_DRAG -0.40 ADD_MULTIPLIED_BASE (lines 64-66).

Left click hold, Wind Spin (performWindSpin, lines 84-175; routed from network/DbaNetwork.java:480; the handler calls it every 2 ticks while held):
- Cost gate: WeaponDrainHelper.drainStaminaDiscrete(player, 20.0, 10), 20 percent per minute prorated over 10 ticks (line 87).
- Range 25 blocks (line 93). Targets qualify inside a 35 degree cone (line 110) or within 4.5 blocks of the player in any direction (line 111).
- Per-target hit gate: 10 ticks, static SPIN_HIT_TIMES map (lines 82, 114-121), pruned only when the map grows past 512 entries.
- Damage: 750 / max(1, distance / 3), through DbaEffectsConfig.capDamage (line 126). That is 750 at 3 blocks or closer, 375 at 6, 250 at 9, about 90 at the 25 block edge.
- On hit: MovementLimiterHelper.applyPowerPoleGaleForce repulsion (line 130) and BOUNCINESS +0.90 modifier, id pole_spin_bounce (lines 133-140).
- Projectile deflection: enemy projectiles within 25 blocks and (within 4.5 or look dot above 0.75) are re-owned to the player and re-aimed at 2.2 speed (lines 144-157).
- Visual cadence: every 6 ticks (line 161) spawns one PowerPoleWhirlwindEntity (range 25, cone 35), sends 14 WIND particles (spread 2.2, 1.2, 2.2, speed 0.08, lines 167-168), plays PLAYER_ATTACK_SWEEP and DbaSounds.WIND_GUST (lines 170-173).

Right click, Extended Poke (use, lines 178-267):
- Cost gate: drainStaminaDiscrete(player, 20.0, 40) (line 182). Cooldown 40 ticks, 2 seconds (line 264).
- Reach 30 blocks (line 189). Single target: flat 750 damage through capDamage (line 206), POLE_STUN 100 ticks amplifier 0 (line 209), BOUNCINESS +0.95, id pole_poke_bounce (lines 212-219).
- 15 percent chance: POLE_STUN amplifier 1 for 100 + nextInt(41) ticks, 100 to 140 total (lines 225-228).
- Always spawns PowerPoleExtensionEntity with MAX_LENGTH set to the actual eye-to-hit distance (lines 241-243). Only when something was hit (entity or block) spawns PowerPoleImpactEntity at scale 2.4 and plays ANVIL_LAND plus HEAVY_CORE_HIT (lines 248-259). TRIDENT_THROW plays on every use (lines 261-262).

Entities:
- PowerPoleWhirlwindEntity: life 8 ticks (entity/PowerPoleWhirlwindEntity.java:25). Follows the owner eye position and rotation. Server emits WIND particles every 2 ticks at roughly 3 x effectDensity count, placed at look x (range x 0.4) ahead (line 115). No damage of its own.
- PowerPoleExtensionEntity: life 12 ticks (entity/PowerPoleExtensionEntity.java:23). Synced YAW, PITCH, MAX_LENGTH (default 30), CASTER_ID. No particles, no damage.
- PowerPoleImpactEntity: life 14 ticks (entity/PowerPoleImpactEntity.java:22). Synced YAW, PITCH, SCALE (default 2.2). Lifetime only; no particles, no damage.

Renderers:
- PowerPoleWhirlwindRenderer: one submitCustomGeometry on kiRenderType. Alpha envelope over the 8 tick life: smoothstep in below lifeT 0.25, out above 0.6. Parts: origin disc (28 segments, spin age x 65 degrees), cone sheath A (12 tiers x 28 segments at 0.65 x coneTan radius, 3-stop gradient, alpha 0.55 falling 45 percent), cone sheath B counter-twisting at 0.95 x coneTan (emerald gradient, alpha 0.40), 4 helical ribbons (24 steps, alpha (1 - along) x 0.7), 10 crescent blades looping down the cone (progress (b / 10 + age x 0.18) mod 1), 3 traveling shock rings plus 1 mouth ring.
- PowerPoleExtensionRenderer: one submit on kiRenderType. Extension is ease-out cubic over the first 3 ticks, hold, retract from tick 9 over 3 ticks with alpha falling only to 0.65, then the entity discards at 12. Shaft: 3 length bands (hot crimson at the hand 1.0/0.22/0.16, mid 0.85/0.08/0.08, deep tip 0.58/0.03/0.05) plus an inner core shaft at 0.55 radius and alpha 0.85, gold base and tip caps with bevel rings and a dome tip, 2 helical gold coil strands (segments min(length x 3.5, 70)), 3 sliding Mach cones, pulsing tip starburst. First person scale 0.55.
- PowerPoleImpactRenderer: one submit on kiRenderType, planar set drawn twice (flat and rotated 90 degrees around Y). Global fade 1 - smoothstep(age / 14). Gold gradient ring (36 segments), second rim ring, crimson ring (30), 8 starburst needles, compression dome (8 lat x 24 lon, alpha 0.35 falling with latitude), 16 UUID-seeded diamond shards flying out on an ease-out over 8 ticks, shrinking 30 percent over life.

### Gap analysis against the 5 points

- Point 1 (layered particle depth): FAIL. The whirlwind is two mesh cone sheaths plus ribbons. The only true particles are 14 WIND per 6 ticks from the item and about 3 per 2 ticks from the entity. There is no dense bright particle core, no mid particle band, no wispy particle edge, and no distance scaling. The mesh sheaths do have 3-stop color gradients, but they are surfaces, not particle layers.
- Point 2 (debris): FAIL. Zero debris. The impact's 16 mesh shards are the only flying pieces in the whole weapon, they are diamonds on fixed seeded paths, and there are 16 of them, not hundreds.
- Point 3 (world interaction): PARTIAL on mobs, FAIL on world. The spin genuinely repels mobs and deflects projectiles back at 2.2 speed, which is real interaction. But nothing touches blocks: no dust burst at any base, no ground mark, no scarring. The poke checks blocks only to shorten the pole.
- Point 4 (atmosphere): FAIL. Nothing.
- Point 5 (lifecycle): FAIL. The whirlwind entity lives 8 ticks and is respawned every 6 ticks while held, so the funnel envelope restarts constantly; it never builds, peaks, or dissipates as one event. The poke's extension retracts but its alpha stops at 0.65 and the entity simply discards at tick 12, a visible cut. Damage is instant on the first gate tick, no wind-up.

### Numbers holding the spectacle back

- Whirlwind entity life 8 ticks with a 6 tick respawn cadence: the effect can never develop a sustained funnel.
- Particle budget: 14 WIND per 6 ticks (item) plus about 3 per 2 ticks (entity). At full hold that is roughly 65 particles per second for a 25 block storm cone.
- Spin damage lands instantly at up to 750 per target per 10 ticks; targets die or are flung before the visual cone reads as a storm.

### Bugs, dead code, mismatches

- PowerPoleWhirlwindRenderer.java:392: drawTriangle is defined and never called anywhere in the file (grep count for the name in the file is 1, the definition). Dead code.
- PowerPoleExtensionRenderer: the shaft helper is named draw12SidedShaft but builds an 18-sided shaft. Name and behavior mismatch.
- SPIN_HIT_TIMES (item/PowerPoleItem.java:82) is keyed by entity id only, shared across all players on the server: two players spinning near the same mob share one 10 tick gate, so one player's spin can suppress the other's hit. Minor, but real.
- PowerPoleExtensionRenderer retract phase: alpha bottoms out at 0.65 and the entity discards at tick 12, so the pole vanishes at 65 percent opacity instead of fading out. Lifecycle mismatch with its own in-animation, which does ease properly.

## 2. Whis Staff (item/WhisStaffItem.java)

Pieces: TimeShatterEntity (life 18 ticks, entity/TimeShatterEntity.java:19), TemporalRiftEntity (life set from constructor, 60 from the item, entity/TemporalRiftEntity.java:24 and 38), renderers TimeShatterRenderer (321 lines), TemporalRiftRenderer (455).

### Ability map

Melee (vanilla swing plus hurtEnemy, lines 95-175):
- Attribute damage 349 + 1 = 350 total (lines 59-66). Attack speed modifier -2.4 (lines 67-74).
- Passives while held: FRICTION -0.80 and AIR_DRAG -0.80, both ADD_MULTIPLIED_BASE (lines 81-86).
- hurtEnemy runs only when the attacker is a Player (lines 99-101). On hit: target starts reversing for 100 ticks via TimeTracker.dba$startReversing(100) (lines 107-109); TEMPORAL_STASIS 100 ticks amplifier 0 (line 112); AIR_DRAG +4.0 modifier whis_stasis_drag (lines 114-120) and FRICTION +3.0 modifier whis_stasis_friction (lines 121-127), both ADD_MULTIPLIED_BASE.
- Server player only: bonus damage Dexterity x 2 as playerAttack through capDamage (lines 138-141); Temporal Shatter 1.5 percent of target max health as magic through capDamage (lines 144-146); spawns TimeShatterEntity scale 1.2 at target mid-height (lines 149-155); sounds AMETHYST_CLUSTER_BREAK, BEACON_ACTIVATE, DbaSounds.ABILITY_CHARGE (lines 158-172). No Ki drain in hurtEnemy by design (the network handler charges the swing).
- Header (lines 40-41) claims a passive Auto-Dodge, 50 percent chance to negate incoming damage while held, implemented via LivingEntityMixin. That mixin is outside the files in this audit slice; the claim is unverified here, neither confirmed nor denied.

Left click, Temporal Strike (performTemporalStrike, lines 180-213; routed from network/DbaNetwork.java:541):
- Cost: WeaponDrainHelper.drainKiDiscrete(player, 100.0, 13), 100 percent per minute over 13 ticks (line 181).
- Spawns 3 TimeShatterEntity along the look vector at 1.5, 3.0, 4.5 blocks with scales 0.75, 0.95, 1.15 (lines 186-189). Plays AMETHYST_CLUSTER_BREAK.
- Finds the nearest LivingEntity at distance 3.0 to 6.5 with look dot at least 0.84 (lines 195-210) and calls applyStaffHit with includeBase true (line 211). Targets closer than 3.0 blocks are deliberately skipped (melee reach belongs to hurtEnemy).
- applyStaffHit (lines 216-249): reversal 100 ticks, TEMPORAL_STASIS 100 ticks amplifier 0, same drag and friction modifiers; one damage call of Dexterity x 2 + 1.5 percent of target max health + 350 (the base, only when includeBase) through capDamage (lines 237-242); TimeShatterEntity scale 1.2; 2 sounds.

Right click, Temporal Rift (use, lines 252-327):
- Cost: drainKiDiscrete(player, 100.0, 100) checked before the server branch (line 256). Cooldown 100 ticks, 5 seconds (line 325).
- Every LivingEntity within 12.0 blocks (AOE_RADIUS, line 50; AABB inflated 12 then distance filtered, lines 267-272): burst damage 400 + Spirit x 1.5 as magic through capDamage (line 264, applied line 274); TEMPORAL_STASIS 60 ticks amplifier 0 (line 276); dba$startReversing(100) called immediately in the same loop (lines 280-282).
- Self buff: CELESTIAL_GRACE 200 ticks amplifier 0 (line 288).
- Spawns TemporalRiftEntity with radius 12 and lifetime 60 (lines 291-297). DbaImpactFx.medium at the player position (line 301). Sounds ABILITY_CHARGE, BEACON_POWER_SELECT, EVOKER_PREPARE_SUMMON (lines 304-321).

Entities:
- TimeShatterEntity: 63 lines. Only SCALE is synced (default 1.2). maxLifetime 18 ticks (entity/TimeShatterEntity.java:19), server-side countdown, discard at 0. No particles, no damage.
- TemporalRiftEntity: RADIUS (default 12) and CASTER_ID synced. Server tick plays AMETHYST_BLOCK_RESONATE every 20 ticks. KI_MOTE particles every 3 ticks: count max(1, round(2 x effectDensity)), at a random ring radius 0.3 to 0.9 of the rift radius, y + 0.3, spread (0.2, 1.2, 0.2), speed 0.02 (entity/TemporalRiftEntity.java:94). Recovers the owner from CASTER_ID when the owner reference is null.

Renderers:
- TimeShatterRenderer: two submitCustomGeometry calls, both on kiRenderType. Layer 1 is billboarded to the camera (pose multiplied by the conjugate of the camera orientation): a 36-segment splintering dial whose segments separate with progress, inner edge gradient halfway to white, plus 8 crack spokes with gradient. Layer 2 is world-space: 16 prism shards, each with an inner white copy at 0.40 scale and a spoke wake, travel on an ease-out cubic of progress (age / 18), shrinking 70 percent over life; octahedron core shrinking 70 percent. Fade starts only at progress 0.5, smoothstep over the second half. Scale grows 0.55 to 1.40 of the synced scale. Both lambdas build new Random(state.seed) from the entity UUID most significant bits, so the billboard pass and the shard pass draw different random sequences. The entity syncs no yaw (noted in the renderer's own comment).
- TemporalRiftRenderer: one submit on kiRenderType. Entrance: sine ease over the first 6 ticks multiplies the radius. Exit: last 10 ticks, smoothstep shrinks radius and fade = min(1, exit). Pulse 0.88 + 0.12 sin(age x 0.20). Dome: 14 lat x 40 lon, height radius x 1.05, latitude color ramp from deep cyan (0.38, 0.76, 1.0) at the base to silver (0.95, 0.98, 1.0) at the apex, alpha (0.13 + lat x 0.15) x pulse x fade. Inner counter-rotating core dome 8 x 28 at 0.72 radius, alpha 0.05 to 0.21. Ground astrolabe: outer ring 48 segments rotating age x 6 degrees; 12 hour steles (cardinals larger and brighter) via drawLash; clock hands reversing (hour hand -age x 15 degrees, minute hand -age x 45 degrees); center octahedral jewel; two tilted orbital rings at +42 and -42 degrees, 40 segments; 12 meridian streams of 16 steps; apex jewel at dome height + 0.1. drawQuad here is double-sided (reverse pass emitted).

### Gap analysis against the 5 points

- Point 1 (layered particle depth): FAIL. The rift dome is a two-shell mesh with a latitude gradient. Total particle output for the whole weapon is about 2 KI_MOTE every 3 ticks from the rift entity, and zero from the shatter. No core/mid/edge particle banding, no distance scaling.
- Point 2 (debris): FAIL. The shatter's 16 mesh prism shards are the only debris-like element; they are glass prisms on fixed seeded paths inside one render, not tumbling pieces in the world, and the count is 16.
- Point 3 (world interaction): FAIL on world, STRONG on mobs in a different way. The rift does not pull, lift, or fling anything; it freezes and rewinds. That is powerful control but it is the opposite motion profile from the video: nothing moves at all for 60 ticks. No block interaction, no dust, no ground effect beyond the astrolabe drawing itself.
- Point 4 (atmosphere): FAIL. A dome appears over a 12 block radius but nothing dims, hazes, or darkens; the dome alpha peaks at about 0.28 on the outer shell.
- Point 5 (lifecycle): PARTIAL. The rift renderer has a real entrance (6 ticks) and exit (10 ticks) and the shatter fades over its second half. But the shatter's first half has no fade-in at all (it appears at full alpha and only starts exiting at progress 0.5), and the whole rift is 60 ticks end to end: build, peak, and dissipation are compressed into 3 seconds with the peak being a static frozen dome.

### Numbers holding the spectacle back

- TimeShatterEntity life 18 ticks (0.9 seconds) for the weapon's signature visual.
- Rift particle rate: 2 x effectDensity KI_MOTE every 3 ticks over a 12 block radius dome. At density 1.0 that is about 13 particles per second for a 24 block wide effect.
- Rift cooldown 100 ticks against a 60 tick effect: the dome is down 40 percent of the time even when spammed.
- The strike's 3 decorative shatters sit at fixed 1.5 block steps with scales under 1.2; at 18 ticks each they read as ticks on a ruler, not an escalating crack.

### Bugs, dead code, mismatches

- TemporalRiftRenderer.java:442: drawTrenchSegment is defined and never called (grep count 1, the definition). Dead code.
- Doc and code tension in use(): the comment at item/WhisStaffItem.java:279 says the reversal "starts after freeze ends", but dba$startReversing(100) is called immediately in the same loop iteration as the stasis application (lines 276-282). Whether TimeTracker internally delays the rewind is not determinable from this slice; as written in the item, the call is immediate.
- TimeShatterRenderer: the billboard dial and the world shards use separate new Random(state.seed) instances with different draw counts, so shard positions do not correspond to the dial's crack pattern. Cosmetic mismatch between the two layers of the same effect.
- Header damage summary (lines 29-33) matches the code. Header Ki summary (line 46) matches. The only unverified header claim is Auto-Dodge (lines 40-41), which lives in a mixin outside this slice.


## 3. Bansho Fan (item/BanshoFanItem.java) - the wind weapon, audited hardest

Pieces: BanshoCycloneEntity (entity/BanshoCycloneEntity.java), BanshoGaleEntity, BanshoShockwaveEntity, BanshoWindProjectileEntity, renderers BanshoCycloneRenderer (371 lines), BanshoGaleRenderer (197), BanshoShockwaveRenderer (388), BanshoWindProjectileRenderer (351).

### Ability map

Melee (vanilla swing plus hurtEnemy, item/BanshoFanItem.java:100-175):
- Attribute damage 399 + 1 = 400 total (line 56). Attack speed modifier -2.0 (line 65).
- Passives while held: AIR_DRAG -0.60 (line 84) and FRICTION -0.75 (line 87), both ADD_MULTIPLIED_BASE.
- hurtEnemy, Gale Force: launch velocity, target position minus attacker position normalized and scaled 5.0, applied as (diff.x, 1.2, diff.z) added to the target velocity (lines 102-104). Gust burst at the target: 12 WIND particles (lines 108-111). BOUNCINESS +0.85, id bansho_gale_bounce (lines 113-119). AIR_DRAG -0.60 modifier bansho_gale_drag (lines 120-126). Bleeding II (amplifier 1) for 300 ticks (line 129).
- Server player only: bonus damage Strength x 1.5 as playerAttack through capDamage (lines 137-140).
- Cyclone Slash proc: 30 percent chance per landed melee hit (line 148) to spawn BanshoCycloneEntity at the target position with slashDamage = (400 + Strength bonus) x 0.5 (line 149).
- Every landed hit also spawns a radial BanshoShockwaveEntity, radius 3.2, at target mid-height (lines 158-167). Sounds PLAYER_ATTACK_SWEEP and DbaSounds.SLASH_WHOOSH (lines 170-173). No stamina in hurtEnemy by design.

Left click, Gale Swing (performGaleSwing, lines 181-195; routed from network/DbaNetwork.java:539):
- Cost: drainStaminaDiscrete(player, 45.0, 10), 45 percent per minute over 10 ticks (line 182).
- Spawns one BanshoGaleEntity with damage 400 + Strength x 1.5 (line 187) and velocity look x 1.35 (line 190). Sound DbaSounds.WIND_GUST. This fires even on a miss; bodies in melee reach are left to hurtEnemy by the entity's skip rule.

Right click, Tempest Barrage (use, lines 198-258):
- Cost: drainStaminaDiscrete(player, 45.0, 40) (line 202). Cooldown 40 ticks, 2 seconds (line 256).
- Spawns a directional cone BanshoShockwaveEntity, radius 3.5, at eye + look x 0.8 (lines 219-228). Purely visual.
- Fires 5 BanshoWindProjectileEntity in a fixed spread: yaw offsets -8, -4, 0, 4, 8 degrees; pitch offsets 2, -1, 0, -1, 2 (lines 237-238). Speed 4.5, inaccuracy 0 (lines 243-250). Projectile damage 300 + Spirit x 1 (lines 231-234).
- Sounds WIND_CHARGE_THROW and WIND_GUST (lines 206-211).

Entities:

BanshoGaleEntity (entity/BanshoGaleEntity.java):
- Life: discards when tickCount exceeds 10 (lines 58-61). Travels by manual setPos at 1.35 blocks per tick, so about 13.5 blocks maximum.
- Hit test each tick: bounding box expanded along motion and inflated (1.6, 0.8, 1.6) (line 63). Skips targets within 3.0 blocks of the owner (MELEE_SKIP, line 24) and targets with motion dot below 0.35 (lines 69-75). Each entity can be hit once (hitIds set).
- On hit: full swing damage through capDamage (line 79), Bleeding II 300 ticks (line 80), launch 1.6 along motion plus 0.55 up (lines 81-82).
- WIND particles: 6 every 2 ticks at the entity position (lines 85-88).
- There is no block collision anywhere in tick(): no clip call, no block check. The gale flies through solid walls for its full 10 ticks.

BanshoCycloneEntity (entity/BanshoCycloneEntity.java):
- Synced SCALE only (default 1.0, set to 1.0 by the item constructor path). maxLifetime 25 ticks, 1.25 seconds (line 28).
- Effect volume: radius = scale x 4.0, height = scale x 6.5, AABB from y - 0.5 to y + height (lines 62-69).
- Every tick, for every LivingEntity in the volume except the owner: tangential spin velocity from angle + 0.5 with speed 0.95 x max(0.2, 1 - dist / radius), inward suction 0.25 when dist above 0.6, lift vy 0.32 added per tick; horizontal components multiplied by 0.45 before being added (lines 76-99).
- Damage: every 5 ticks, slashDamage x 0.35 through capDamage (lines 102-108), plus Bleeding I (amplifier 0) for 100 ticks refreshed on each damage tick (line 109). Over the full 25 ticks a target that never leaves takes 5 damage ticks = 1.75 x slashDamage = 87.5 percent of the main hit's (400 + Strength bonus) figure.
- Sounds: ELTRA_FLYING and WIND_CHARGE_BURST at tick 1 and every 12 ticks (lines 114-119).
- Particles: every 2 ticks, WIND count max(1, round(2 x effectDensity)) at a random angle, radius 0.4 to 0.9 of the effect radius, height 0 to 0.8 of the funnel height, spread (0.15, 0.3, 0.15), speed 0.06 (lines 125-137). At density 1.0 that is about 25 particles across the cyclone's entire life.
- The entity does not move, does not grow, and does not interact with blocks.

BanshoShockwaveEntity (entity/BanshoShockwaveEntity.java):
- Purely visual. Synced YAW, PITCH, MAX_RADIUS (default 3.0), IS_CONE, ORIENT_TO_IMPACT. maxLifetime 14 ticks (line 22). tick() only counts down and discards (lines 92-100). No damage, no particles, no force.

BanshoWindProjectileEntity (entity/BanshoWindProjectileEntity.java):
- Damage is a plain field (default 300.0, line 33) set from the item; CASTER_ID synced. Spawn offset from the eye: look x 0.5, right x (sideSign x 0.35), up x -0.25, with main-arm and offhand detection (lines 44-63).
- tick(): manual raycast against blocks each tick (ClipContext COLLIDER, lines 124-133); on block hit it impacts and discards. Entity test: sweep box inflated 0.8, and only the FIRST target in the returned list is hit (targets.get(0), lines 136-147); the projectile then discards. It does not pierce.
- onHitImpact (lines 177-218): damage through capDamage (line 185); Bleeding III (amplifier 2) for 400 ticks (line 188), matching the item header; knockback 1.4 along motion plus 0.45 up (lines 191-193); spawns radial BanshoShockwaveEntity radius 2.6 on an entity hit or 2.0 on a block hit, with ORIENT_TO_IMPACT set true (lines 197-210); sounds WIND_CHARGE_BURST and ELYTRA_FLYING.
- Trail: WIND count max(1, round(2 x effectDensity)) every 2 ticks (lines 153-162). Timeout at 50 ticks: 10 WIND particles and a WIND_CHARGE_BURST sound, then discard (lines 164-173). At speed 4.5 the maximum range is about 225 blocks.
- No gravity, no drag: velocity is constant until impact or timeout.

Renderers:

BanshoCycloneRenderer: one submit on kiRenderType (line 51 and following). Lifecycle envelope: ease-in over the first 4 ticks (cubic), fade-out starting at age 17 over 8 ticks, alphaMult = inEase x (1 - smoothstep(outT)) (lines 66-69). Contents:
- Suction ring at the foot: 36 segments at radius baseRadius x 0.95 (baseRadius = scale x 3.5), rotating age x 28 degrees, plus a pale eye ring at 0.22 of that radius counter-rotating (lines 78-83).
- Funnel cone: 10 levels x 28 segments. Wall radius runs from 0.28 to 1.0 of (baseRadius x 0.38), so at scale 1.0 the visible funnel wall tops out at 1.33 blocks radius. Twist p x 1.4 up the height, rotation age x 30 degrees, breathing 1 + 0.03 sin(age x 0.9 + p x 5). Alpha 0.65 x (1 - p x 0.25). 3-stop gradient: deep emerald (0.0, 0.82, 0.42) at the base to emerald (0.15, 1.0, 0.65) mid to pale jade (0.55, 1.0, 0.85) at the crown (lines 92-137).
- 3 helical ribbons: 22 steps each, radius baseRadius x (0.35 + 0.65 t^2) so they reach 3.5 blocks at the top, twist t x 3.8 PI, rotation age x 22 degrees, band width 0.38 x scale x (0.6 + t x 0.7), alpha 0.60 (lines 140-180).
- 8 orbiting blades: fixed heights at progress x totalHeight x 0.85 + 0.4, radius baseRadius x (0.45 + 0.55 x (y / height)), per-blade rotation speed age x (32 + i x 3) degrees, drawn with drawLash, length 0.75 x scale, alpha 0.90 (lines 183-214).
- Spindle core: 6 levels x 8 segments, radius 0.20 x scale tapering 62 percent, rotation age x 50 degrees, plus an inner copy at 0.38 scale in near-white (lines 217-256).
- Crown outflow ring at the top: 30 segments at baseRadius x 1.12, counter-rotating age x -26 degrees, alpha 0.50 (lines 259-262).
- drawQuad in this renderer emits one winding only (lines 341-352): the funnel is single-sided. From inside the funnel or from below, the wall faces are culled and the funnel disappears.

BanshoGaleRenderer: one submit on kiRenderType. The fan opens over 6 ticks (ease-out cubic), then tips dissolve over the last 4 (lines 51-57). 9 ribs drawn with drawRib (8 steps each, gradient from (0.05, 0.42, 0.20) to (0.88, 1.0, 0.94)) plus a second inner copy at 0.38 width; 3 arcs (leading edge 28 steps near-white, fold 22 steps alpha 0.48, wake 22 steps alpha 0.26). Fan length 2.85, spread (0.20 + open x 1.05). Ribbons are drawn double-sided manually through KiRenderHelper.vertex pairs.

BanshoShockwaveRenderer: one submit on kiRenderType. Life 14 ticks, progress = age / 14, alpha holds until progress 0.55 then smoothstep out (lines 61-65). Cone mode: outer shell triangles in two greens, an inner shell at 0.42 scale, a mouth ring (28 segments), 10 forward needles via drawLash that retract after progress 0.40, and a muzzle flare ring that shrinks with progress (lines 72-129). Radial mode: beveled ring 40 segments with crest height 0.55 x (1 - progress x 0.8), 6 crescent blades sweeping 1.05 radians in 7 steps each, and a shrinking white-emerald core ring (lines 131-184). ORIENT_TO_IMPACT rotates the radial burst to the impact direction. drawQuad here is also single-winding per call, with geometry authored to face outward.

BanshoWindProjectileRenderer: one submit on kiRenderType. Envelope: in over 3 ticks (cubic), out from age 42 over 8 ticks (lines 76-79), matched to the 50 tick entity cap. Drill: 18 segment cone in two gradient bands plus an inner spindle, length 1.35, radius 0.32 with a 5 percent breathing sine, rotation age x 45 degrees; 4 orbiting blades via drawLash at orbit radius drillRadius x 1.35, counter-rotating age x -36 degrees; 2 helix wraps of 14 steps; 2 wake rings behind; a 4-band tapered nose with a white core (lines 84-190). First person scale 0.55.

### The funnel, specifically, against the video standard

The cyclone is the only tornado-like effect in the weapon, and it is a 30 percent proc on a landed melee hit. What it actually is in code:

- Duration: 25 ticks total. With the renderer's 4 tick ease-in and fade-out from tick 17, the funnel is at full presence for roughly ticks 5 to 17, about 0.6 seconds.
- Size: effect radius 4.0 blocks, height 6.5. The visible funnel wall is much smaller than the effect volume: the cone wall reaches only 1.33 blocks radius at the top (0.38 x baseRadius 3.5). The ribbons (to 3.5) and the crown ring (3.92) are the only parts that approach the pull radius. A mob standing at 3.5 blocks is being shredded by a funnel whose wall visually passes 2 blocks inside its position.
- Motion: mobs are genuinely pulled, spun, and lifted (suction 0.25, tangential up to 0.95 x 0.45, lift 0.32 per tick). This is the one place in the whole technique group where point 3's mob handling exists in vortex form. But targets are never flung out: when the entity discards at tick 25, whatever velocity they had is all they keep. There is no ejection phase.
- Particles: about 25 WIND particles over the entire 1.25 second life at default density. The funnel read comes almost entirely from the mesh cone. There are no rotating particle bands at different radii, no bright particle core (the spindle is mesh), no wispy particle edge, and no distance scaling: a viewer 30 blocks away gets the same 2 particles per 2 ticks, all near the funnel.
- Debris: zero. Nothing orbits except the 8 fixed mesh blades at fixed heights. No blocks, no chunks, no tumbling pieces of any size.
- Ground: the suction ring is a flat drawing at y + 0.05. There is no dust burst at the base, no debris ring on the ground, no scarring, and the AABB starts at y - 0.5 so the pull does reach slightly below the spawn point, but nothing visual marks it.
- Atmosphere: none. The sky does not change, no haze, no lightning.
- Lifecycle: 4 ticks in, about 13 ticks peak, 8 ticks out, then the entity is gone and every mob it held simply drops with its current velocity. No debris rain, no slow dissipation, and the proc can immediately re-trigger on the next melee hit, so cyclones pop in and out at melee cadence.

### Gap analysis against the 5 points (whole weapon)

- Point 1 (layered particle depth): FAIL. Every Bansho effect is mesh-first: cyclone cone and ribbons, gale fan, shockwave rings, drill. Particle totals per effect: cyclone about 25 per life, gale 6 per 2 ticks for 10 ticks (30), projectile trail 2 per 2 ticks, melee gust 12. No layered bands, no bright particle core, no distance scaling anywhere.
- Point 2 (debris): FAIL, zero pieces in all four entities and all four renderers.
- Point 3 (world interaction): PARTIAL, the best in this group for mobs. The cyclone pulls, lifts, and spins mobs; melee launches at 5.0 horizontal; the gale launches at 1.6; projectiles knock back at 1.4. But blocks are untouched: no dust at the cyclone base, no scarring, and the gale does not even collide with blocks, it passes through them, which reads as the wind ignoring the world rather than interacting with it.
- Point 4 (atmosphere): FAIL. Nothing.
- Point 5 (lifecycle): FAIL for the cyclone (25 ticks, pop-in cadence from a per-hit proc), PARTIAL for the smaller effects: the gale opens and dissolves over its 10 ticks, the projectile has a matched in/out envelope and a timeout puff, the shockwave holds then fades. Those are honest fades on very short lives.

### Numbers holding the spectacle back

- Cyclone lifetime 25 ticks and proc chance 30 percent: the storm is a brief accident of melee, not an event. At 25 ticks there is no time for a build-up phase even if the renderer had one.
- Cyclone damage ticks every 5 ticks at 35 percent of the proc figure: the shredding is back-loaded into at most 5 hits, and a target knocked out of the 4.0 radius early takes 1 hit of 17.5 percent of the main hit.
- Gale life 10 ticks at 1.35 speed: the fan's own swing visual (renderer length 2.85) is far shorter than the 13.5 block damage path.
- Right click cooldown 40 ticks with 5 projectiles at 300 + Spirit each: the barrage is strong, but each projectile's trail is 2 particles per 2 ticks, so a 5-drill spread reads as 5 bare meshes.

### Bugs, dead code, mismatches

- BanshoCycloneRenderer.java:361: drawTriangle is defined and never called (grep count 1). Dead code. (drawTriangle IS used in BanshoShockwaveRenderer and BanshoWindProjectileRenderer; the dead one is only the cyclone's.)
- BanshoGaleEntity has no block collision (entity/BanshoGaleEntity.java:57-91): it passes through solid blocks and can damage targets behind walls, while the fan's own wind projectile (entity/BanshoWindProjectileEntity.java:124-133) does clip against blocks. The two wind projectiles of the same weapon treat the world inconsistently.
- Item header (item/BanshoFanItem.java:36-37) describes the cyclone as dealing "50% damage"; the entity actually deals 35 percent of that 50 percent figure every 5 ticks, up to 175 percent of the figure (87.5 percent of the main hit) over a full stay. The header number is neither the per-tick nor the total number.
- Visual and effect radius mismatch on the cyclone: pull radius 4.0 (entity/BanshoCycloneEntity.java:62) versus funnel wall maximum 1.33 blocks (renderer cone at 0.38 x baseRadius). Targets at the edge of the volume are visibly outside the funnel that is hurting them.
- BanshoShockwaveEntity.ORIENT_TO_IMPACT defaults false and is only ever set true by the wind projectile; the melee and launch shockwaves rely on the flat or cone paths. Not a bug, but the field's default path means any future radial caller that forgets the setter gets a flat ring in mid-air.

## 4. Blaster Gun (item/BlasterGunItem.java)

Pieces: BlasterBoltEntity (entity/BlasterBoltEntity.java), ErasureCannonBeamEntity, ErasureChargeOrbEntity, renderers BlasterBoltRenderer (273 lines), ErasureCannonBeamRenderer (402), ErasureChargeOrbRenderer (344). Plus the ammo economy in the item itself (getAmmoCount line 78, consumeAmmo line 89) and heat stored in the stack's CUSTOM_DATA (getHeat line 106, setHeat line 115, clamped 0 to 100).

### Ability map

Melee (vanilla swing):
- Attribute damage 449 + 1 = 450 total (line 51). Attack speed modifier -2.0 (line 59). The item defines no hurtEnemy override: a melee swing is plain attribute damage with no effect.
- Passives while held: NAME_PLATE_DIST +64 (line 69) and MINI_NAME_PLATE_DIST +30 (line 72), both ADD_VALUE.

Left click hold, Blaster Barrage (onLeftClickBarrageTick, lines 127-181; routed from network/DbaNetwork.java:530 with chargeTicks):
- Fires only when chargeTicks % 3 == 0 (line 132): one bolt every 3 ticks while held, about 6.7 shots per second.
- Heat: each normal shot adds 8 heat (line 171), so heat reaches 100 on the 13th shot. Heat ratio = heat / 100 (line 151).
- Normal bolt damage: 250 + (Spirit x 1.5) x (1 + heatRatio x 0.5) (lines 152-154). Ammo cost 1 per shot (line 148).
- Overcharged state (heat at 100 when the shot fires): damage 700 + Spirit x 3.0 (line 153), ammo cost 3 (line 148), heat resets to 0 (line 163), item cooldown 20 ticks as a vent pause (line 167), GENERIC_EXPLODE sound and DbaImpactFx.heavy at the player (lines 168-169).
- Bolt speed 2.4 blocks per tick along the look vector (lines 157-160).
- Out of ammo: action-bar message "Out of Blaster Ammo!" and DISPENSER_FAIL sound, no shot (lines 139-145).
- Every shot updates an action-bar HUD: ammo count and heat percent (lines 177-180). No Ki or stamina cost anywhere on this weapon; the economy is ammo plus heat.

Right click, Erasure Cannon (use line 186, onUseTick line 207, releaseUsing line 236):
- use() requires at least 10 ammo (creative exempt) or returns InteractionResult.PASS with a chat message (lines 188-192). Starts using the item; getUseDuration is 72000 (line 197) and the use animation is BOW (line 202).
- Charge: MAX_RIGHT_CHARGE_TICKS 100, 5 seconds to full (line 40). chargeRatio = min(1, heldTicks / 100) (line 212).
- While charging, onUseTick refreshes ENERGY_OVERCHARGE amplifier 0 for 10 ticks on the player every tick (line 215), maintains one ErasureChargeOrbEntity per player in the static ACTIVE_CHARGE_MAP (lines 41, 218-224) and sets its charge ratio; charge sounds (BEACON_AMBIENT and DbaSounds.ABILITY_CHARGE) every 20 ticks with pitch rising with charge (lines 226-231).
- Release: the charge orb is removed from the map and discarded (lines 242-245). chargeRatio is floored at 0.1 even for an instant release (line 247). Ammo cost 10 + (int)(chargeRatio x 10), so 11 minimum in practice (floor 0.1 gives 10 + 1) and 20 at full charge; if the player cannot pay the full cost the shot does not fire (lines 252-260).
- Beam: 48 blocks from the eye along look, clipped against blocks to find the true length (lines 262-272). Damage 900 + chargeRatio x 1400 + Spirit x 4.0 (lines 275-277), so a full-charge shot is 2300 + Spirit x 4 before the cap. Spawns ErasureCannonBeamEntity with that damage, ratio, and length (line 280). DbaImpactFx.heavy at the impact point (lines 284-285). Cooldown 120 ticks, 6 seconds (line 287).
- onPlayerDisconnect discards a tracked charge orb (lines 291-295).

Entities:

BlasterBoltEntity (entity/BlasterBoltEntity.java):
- Synced: IS_OVERCHARGED, HEAT_RATIO, CASTER_ID, IMPACT_AGE (default -1). Damage is a plain field, default 250 (line 36). Spawn offset: eye + look x 0.5, right x (sideSign x 0.32), up x -0.25 with main-arm and offhand detection (lines 44-63).
- Flight: manual setPos along constant velocity; no gravity, no drag. Each tick it raycasts the step against blocks (lines 124-141); on a block hit it stops, plays FIRE_EXTINGUISH, sets IMPACT_AGE 0, and lingers: the renderer collapses it over 5 ticks, then it discards (lines 103-114).
- Entity hits: hitbox inflated by hitRadius = 1.5 if overcharged, else 0.6 + heatRatio x 0.4 (line 156). The bolt PIERCES entities: hit ids are remembered and it keeps flying (lines 160-200). Life cap: discards when tickCount exceeds 30 (lines 197-199), so maximum range is about 72 blocks at speed 2.4.
- On each entity hit: full bolt damage through capDamage, CINEMATIC_TRACKING 15 ticks amplifier 0 (lines 168-172), knockback 0.6 along motion plus 0.2 up (lines 176-178).
- Overcharged hits additionally: BOUNCINESS +0.75 and AIR_DRAG -0.30 modifiers on the target (lines 181-194), splash damage of 60 percent of bolt damage to other entities in the target's box inflated 3.0 (lines 186-192), 1 EXPLOSION particle (line 193), GENERIC_EXPLODE sound.
- The bolt entity emits no trail particles at all. Its entire flight visual is the renderer mesh.

ErasureChargeOrbEntity (entity/ErasureChargeOrbEntity.java):
- Synced CHARGE_RATIO and CASTER_ID. Follows the caster's muzzle (eye + look x 1.2) every tick and copies rotation (lines 79-86).
- Self-cleanup: discards if the caster is gone or dead; on the server, discards if the player is not holding a Blaster Gun or not using an item (lines 89-96); discards if setChargeRatio has not been called for more than 6 ticks (lines 117-120).
- Particles: every 2 ticks, SPARK count max(1, round((1 + charge x 3) x effectDensity)) at gather radius 0.9 - charge x 0.45 around the orb, vertical jitter 0.6, spread 0.05, speed 0.01 (lines 100-112). The gather radius shrinking as charge rises is the one inward-spiraling particle behavior in this weapon group.

ErasureCannonBeamEntity (entity/ErasureCannonBeamEntity.java):
- Synced CASTER_ID, BEAM_LENGTH (default 48), BEAM_Y_ROT, BEAM_X_ROT, CHARGE_RATIO. MAX_LIFETIME 16 ticks (line 34). The entity is static: it sits at the caster's eye position at fire time and does not follow the caster afterward.
- ALL damage happens on tick 1 (line 110). Candidate box: the beam AABB inflated by 1.5 + charge x 1.5 (line 124), then a real segment distance test: a target counts only if its center is within beamRadius (1.5 + charge x 1.5) plus half its width of the beam segment (lines 133-142).
- Per target on tick 1: CINEMATIC_TRACKING 25 ticks (lines 146-150), full totalDamage through capDamage (line 152), BOUNCINESS +0.85 (lines 155-161), AIR_DRAG -0.40 (lines 162-168), and a suction pull toward the beam centerline of 0.8 plus 0.2 up (lines 171-173).
- Max charge only (charge at least 0.9): endpoint explosion, 3 EXPLOSION_EMITTER particles and 50 percent of totalDamage to everything in a box 4 blocks each way around the endpoint, including targets already hit by the beam (lines 177-185).
- Sounds on tick 1: LIGHTNING_BOLT_THUNDER and DbaSounds.LIGHTNING_ZAP at volume 2.2 (lines 187-190).
- Particles: ticks 1 to 3 only. 10 SPARK (times effectDensity) placed at random points along the beam path, plus 6 EMBER at the terminus (lines 196-213). After tick 3 the beam burns for 13 more ticks with zero particles.
- Discards at tick 16 (lines 215-217).

Renderers:

BlasterBoltRenderer: one submit on kiRenderType. Color by state: overcharged (1.0, 0.12, 0.32); heat above 0.6 (1.0, 0.45, 0.05); else (1.0, 0.88 - heat x 0.30, 0.10). Pulse 1 + 0.08 sin(age x 2.5). Radius: overcharged 0.36, else 0.18 + heat x 0.12. Head length: overcharged 1.4, else 0.85 + heat x 0.35. Tail length: overcharged 3.4, else 2.0 + heat x 1.0. Three nested drawPlasmaBolt layers: outer sheath at 1.22 radius and alpha 0.22, body at alpha 0.88, white core at 0.38 radius counter-rotating (lines 106-108). 4 stabilizer fins via drawLash spanning 2.3 x radius (lines 115-131). On block impact the same bolt collapses: radius shrinks 62 percent, head 70 percent, over the 5 tick IMPACT_AGE, with a smoothstep alpha out. First person scale 0.60.

ErasureCannonBeamRenderer: one submit on kiRenderType. Envelope: scale-in over 3 ticks (cubic), exit starts at progress 0.58, smoothstep out by tick 16 (lines 94-98 of the renderer). Base radius (0.35 + charge x 0.45) x pulse; first person 0.50 scale with a muzzle anchor offset. Three drawFlowBeam layers: white core at 0.28 radius, cyan at 0.72 with alpha 0.78, darker lip at 0.86 with alpha 0.35; each is drawn in length bands (6 to 20 bands) with a traveling sine brightness wave and a taper toward the terminus (renderer lines 122-124 and the drawFlowBeam helper). 4 helical ribbons ride the beam (segments min(length x 3, 96), alternating white and cyan, flow-banded alpha). Muzzle emitter: one plane ring plus 4 converging lashes. Terminus: flared dome (radius (1.2 + charge x 0.8) x pulse, 16 segments) with a white core spike and 6 splash fins via drawLash. No aperture rings along the beam despite the class header promising them (see bugs).

ErasureChargeOrbRenderer: one submit on kiRenderType. Orb radius (0.22 + chargeEase x 0.95) x pulse, where chargeEase is the smoothstep of the charge ratio; alpha 0.10 + chargeEase x 0.90; past charge 0.75 a hot parameter shifts the shell color toward red-white (renderer lines 92-96). Contents: white octahedron core at 0.38 radius, two geodesic sphere shells (8 rings; outer 16 segments at 0.82 radius rotating -18 degrees per tick unit, inner 12 segments at 0.48 rotating +26) with a 3-stop latitude gradient, ONE plane ring at radius orbRadius x (1.48 - chargeEase x 0.28), and 8 intake spikes via drawLash spiraling inward from radius orbRadius x (2.3 - chargeEase x 0.95), speed 30 + charge x 60 degrees. First person scale 0.45 with a muzzle anchor offset.

### Gap analysis against the 5 points

- Point 1 (layered particle depth): FAIL on particles, PARTIAL on mesh layering. The beam renderer is genuinely layered (core, shroud, lip, ribbons, flow bands) and the bolt has 3 nested layers, but all of it is mesh. Beam particles exist only on ticks 1 to 3 (10 SPARK along the path, 6 EMBER at the end); the bolt has no trail particles at all, so a 72 block bolt flight is a bare mesh with nothing shedding off it. No distance scaling: the beam draws full band detail at any range.
- Point 2 (debris): FAIL. Zero debris. The overcharged splash and the endpoint explosion use 1 and 3 vanilla explosion particles respectively; nothing tumbles, nothing orbits.
- Point 3 (world interaction): PARTIAL. The beam clips against blocks and its endpoint explosion at charge 0.9+ is real area damage; the beam also sucks targets toward the centerline. But no block is ever damaged, cracked, or marked by any part of this weapon, the bolt stops at blocks with a FIRE_EXTINGUISH sound and a 5 tick collapse, and there is no dust or ground effect at any impact.
- Point 4 (atmosphere): FAIL, with the group's only near miss: the beam plays thunder and zap sounds, but there is no lightning visual, no light dimming, no haze. A 2300 damage beam fires in broad daylight conditions unchanged.
- Point 5 (lifecycle): PARTIAL, the best in the group on the front end. The charge is a real 100 tick build-up with a growing orb, gathering sparks, rising sound pitch, and a damage payoff that scales from about 1040 to 2300 plus Spirit. The back end is abrupt: all beam damage lands on tick 1, the beam visual ends at tick 16 on a fade that starts at 0.58, and the bolt's timeout death at tick 30 is a hard discard with no fade (only block impacts get the collapse treatment).

### Numbers holding the spectacle back

- Beam lifetime 16 ticks with all damage on tick 1: a 5 second charge buys a 0.8 second beam that is mechanically over in 1 tick.
- Beam particles stop after tick 3 of 16: 13 ticks of bare mesh.
- Bolt fire rate 1 per 3 ticks and bolt life 30 ticks: at most about 10 bolts alive at once, each with zero trail particles, so the barrage's density is capped by entity count, not by any particle system.
- Overcharge cycle: 13 shots to build heat, then a single overcharged shot and a 20 tick vent. The heat color shift on the bolt (renderer) is the only visible ramp; there is no growing muzzle glow, sound pitch rises only through the BLAZE_SHOOT pitch term (1.6 + heatRatio x 0.4).
- Charge floor 0.1 on release: an accidental click still fires an 11 ammo, roughly 1040 damage beam. There is no low-charge fizzle state.

### Bugs, dead code, mismatches

- ErasureChargeOrbRenderer.drawRotatingOctahedron (renderer lines 188-190): the method takes yawDeg and pitchDeg, converts both to radians into local variables yaw and pitch, and then never uses them. The octahedron vertices are drawn unrotated. The "rotating" core does not rotate; only the shells and ring rotate. Dead parameters and a misleading name.
- ErasureCannonBeamRenderer class header promises "Stationed hexagonal focusing aperture rings along the beam" and an "outer hex barrier"; submit() draws neither. The only ring in the file is the muzzle emitter ring inside drawMuzzleEmitter. Header and code mismatch.
- ErasureChargeOrbRenderer class header promises "3 concentric synchrotron accelerator rings", "Helical containment ribbons", and "overdrive corona discharge arcs"; submit() draws exactly one ring, no ribbons, no arcs. Header and code mismatch.
- BlasterGunItem.use() failure path returns InteractionResult.PASS (line 191) where the mod's other weapons return FAIL on a failed cost gate, and its "Need at least 10 Blaster Ammo" message is sent as a chat message (single-argument sendSystemMessage, line 189) while the barrage's out-of-ammo message uses the action bar. Minor inconsistencies.
- BlasterBoltEntity: the block-impact linger sets IMPACT_AGE on the server only (lines 136-139), and the renderer reads it from synced data, which is correct; but a bolt that hits an entity as its final act keeps flying, while a bolt that touches a wall stops dead. Behavior is intended piercing per the class comment, noted here only because the two stop conditions feel very different in play.
- ErasureCannonBeamEntity endpoint explosion (lines 177-185) does not exclude targets already damaged by the beam pass, so a target standing at the terminus takes 100 percent plus 50 percent on the same tick. That may be intended stacking for max charge; it is not documented in the item, whose text mentions no endpoint explosion at all.


## 5. Spike Flail (item/SpikeFlailItem.java)

Completeness verdict: COMPLETE, not a stub. Geo model exists, melee attribute exists, left click swing exists (network routed), right click channel exists with a two-phase entity (orbit, then release crack), both entities have real damage logic, both renderers are substantive, and onPlayerDisconnect cleanup exists (item/SpikeFlailItem.java:148-150). It is thinner than the older four weapons in exactly one dimension: spectacle. All numbers below.

Pieces: SpikeFlailSwingEntity (entity/SpikeFlailSwingEntity.java), SpikeFlailSpinEntity (entity/SpikeFlailSpinEntity.java), renderers SpikeFlailSwingRenderer (213 lines), SpikeFlailSpinRenderer (108).

### Ability map

Melee (vanilla swing):
- Attribute damage 819 + 1 = 820 total (item/SpikeFlailItem.java:44). Attack speed modifier -2.7 (line 49). No passives.
- hurtEnemy is an empty no-op by design (lines 57-59): the swing entity is the real left click.

Left click, Swing (performSwing, lines 61-87; routed from network/DbaNetwork.java:545):
- Guards: blocked while the item is on cooldown, while the player is using an item, or by a same-tick duplicate packet (static LAST_SWING_TICK map, lines 64-71); requires hasStamina, then drainStaminaDiscrete(player, 100.0, 20), 100 percent per minute over 20 ticks (line 75).
- Item cooldown 8 ticks (line 78).
- Spawns SpikeFlailSwingEntity with damage 720 + Strength x 2.0 (line 81). Sounds CHAIN_HIT and PLAYER_ATTACK_SWEEP.

Right click, Spin channel (use line 89, beginSpin line 106, onUseTick line 118, releaseUsing line 133):
- use() fails if the item is on cooldown or stamina is empty (lines 90-92); otherwise beginSpin spawns a SpikeFlailSpinEntity unless one is already alive within 3 blocks (lines 107-115) and the player starts using the item. Use duration 72000, animation BOW (lines 100-104).
- Channel: onUseTick drains stamina per tick at 150 percent per minute (line 122); if the drain fails the use stops. At held = CHANNEL_TICKS 32 (line 33) the use stops automatically (lines 126-129). Releasing early also ends the channel.
- releaseUsing adds item cooldown 36 ticks (line 136) and plays CHAIN_HIT. The actual crack damage is fired by the spin entity when it notices the player is no longer holding (see below), not by releaseUsing itself.

Entities:

SpikeFlailSwingEntity (entity/SpikeFlailSwingEntity.java):
- SWING_TICKS 11 (line 24). Synced YAW and DAMAGE (default 720). Facing is locked at spawn.
- Ball path (ballPos, lines 55-69): side sweeps -1 to +1 times 2.65 through a smoothstep ease, forward 1.85 + 0.95 sin(eased x PI), height owner y + 1.05 + 0.62 sin(eased x PI). The entity teleports to the ball position each tick.
- Hits: bounding box inflated 1.15, then a distance cap of 1.7 (lines 103-110). Each target once (hitIds). Full swing damage through capDamage (line 112), push 0.55 along ball-minus-owner plus 0.22 up (lines 113-114).
- No particles, no sounds of its own, no block interaction: the ball passes through walls and terrain freely.

SpikeFlailSpinEntity (entity/SpikeFlailSpinEntity.java):
- Synced ANGLE, RADIUS (starts 2.55, line 51), RELEASING, RELEASE_AGE. Follows the owner position each tick.
- Orbit phase: ANGLE advances 32 degrees per tick (line 131), a full revolution every 11.25 ticks. Ball position: (cos(angle) x radius, +1.15, sin(angle) x radius) (ballPos, lines 84-87).
- Contact damage: checked every 4 ticks (lines 133-135). Damage 160 + Strength x 0.35 (line 137) to targets within 1.25 of the ball (hitNearBall reach, line 138 call), per-target re-hit gap 8 ticks, push 0.35 horizontal plus 0.12 up (lines 141-159).
- Release phase: when the player is no longer using the flail, RELEASING is set (lines 118-120). RELEASE_AGE increments; RADIUS grows 2.55 + age x 0.48 per tick (lines 122-126), reaching about 6.4 at age 8. On the FIRST release tick the crack fires once (cracked flag, lines 127-130). Discards when release age exceeds 8 (lines 131-133).
- crack (lines 160-176): damage 780 + Strength x 2.0 through capDamage to every LivingEntity with horizontal distance between 0.4 and 5.4 of the player; knockback (0.85 / dist) along the radial direction plus 0.38 up. The crack takes no input from how long the channel ran.
- No particles anywhere in either flail entity. No block interaction.

Renderers:

SpikeFlailSwingRenderer: one submit on kiRenderType. Fade: full until age 8, smoothstep out over the last 3 ticks; open ease over the first 2 ticks scales the ball 0.75 to 1.0 (submit, lines 66-78). drawChain: 8 prism links (6 sides each) along a line from hand to ball with a 0.45 sine sag, alternating link radius 0.07 and 0.10, steel color (0.72, 0.74, 0.78). drawBall (shared with the spin renderer): outer sphere radius x (5 lat x 8 lon) in (0.78, 0.80, 0.84), inner sphere at 0.45 radius in near-white, and 6 spike cones (5 sides) on the cardinal axes, length about 0.3 beyond the ball surface.

SpikeFlailSpinRenderer: two submits on kiRenderType. While orbiting it draws the chain from (0.25, 1.0, 0.15) to the ball and the shared ball at radius 0.40, at the synced angle and radius, ball height 1.15. While releasing it additionally draws drawCrack: a flat bowed disc at y + 0.12, 20 segments around x 4 radial bands, from radius x 0.7 to the current radius, height bow 0.7 x fade with a sine profile, color near (1.0, 0.95, 0.8), alpha fade x (0.25 + 0.7 sin(t x PI)) (lines 79-99). The whole render fades with the release ease over 8 ticks.

### Gap analysis against the 5 points

- Point 1 (layered particle depth): FAIL, absolutely. Zero particles in the item, both entities, and both renderers. The spin is one ball mesh on a chain; there is no trail, no motion streak, no band of any kind around a ball moving at 32 degrees per tick.
- Point 2 (debris): FAIL. Zero pieces. The crack is a flat disc drawing.
- Point 3 (world interaction): PARTIAL on mobs only. The orbiting ball genuinely occupies space and knocks targets it touches, and the crack is a real radial knockback. But the swing ball passes through blocks, the crack ignores line of sight and height beyond the box (inflate 5.4, 1.4, 5.4), and nothing leaves dust, marks, or any trace on the ground.
- Point 4 (atmosphere): FAIL. Nothing.
- Point 5 (lifecycle): PARTIAL structure, FAIL payoff. The channel is a real build-up (up to 32 ticks of orbit), and the release has an 8 tick expanding fade. But the crack fires at full power on the first release tick whether the channel ran 1 tick or 32: the build-up buys nothing except more contact ticks. The swing itself is 11 ticks with a 3 tick fade, in and out in about half a second.

### Numbers holding the spectacle back

- CHANNEL_TICKS 32 at 150 percent per minute stamina, and the crack is a flat 780 + Strength x 2.0 regardless of channel time. There is no reason in the numbers to ever hold the full channel, so in practice the spin reads as a short orbit and an instant ring.
- Orbit contact damage 160 + Strength x 0.35 every 8 ticks per target: next to the 820 melee and 780 crack, the orbiting ball is a nuisance hit, so players have no damage reason to keep the ball spinning near targets either.
- Crack damage radius 5.4 fires while the visual radius is still 2.55 (the disc grows to about 6.4 over the following 8 ticks). Damage leads the visual by a full ring width.
- Swing cooldown 8 ticks plus an 11 tick swing entity: swings overlap, each a bare chain and ball with no trail.

### Bugs, dead code, mismatches

- Crack and channel are disconnected (entity/SpikeFlailSpinEntity.java:160-176 versus item/SpikeFlailItem.java:118-131): nothing passes the held tick count into the entity, so the release payoff cannot scale. Design gap, stated with the code locations.
- Damage and visual mismatch on release: crack applies at radius 5.4 on release tick 1; the rendered disc starts at radius 2.55 x 0.7 inner and grows 0.48 per tick. Targets at 5 blocks take full crack damage from a ring that has not visually reached them.
- beginSpin duplicate check (item lines 108-110) searches a 3 block box around the player for any live spin entity owned by the player; combined with the entity's own follow behavior this is fine, but if a spin entity ever survives its owner check (owner null path discards only when tickCount exceeds 2, entity lines 95-100), a stale entity near the player would block new spins. Edge case, low severity.
- SpikeFlailSwingEntity ball path is computed from the owner's CURRENT position each tick with the spawn-locked yaw (entity lines 96-98): if the player teleports or is knocked mid-swing, the ball jumps with them while keeping the old facing. Minor.

## 6. War Mace (item/WarMaceItem.java)

Completeness verdict: COMPLETE, not a stub. Geo model exists, the heaviest melee attribute in the group, left click smash with two variants (ground and falling heavy), right click rise, a relaunch combo loop between the smash entity and the item, both renderers substantive, and full disconnect cleanup for all five of its static maps (item/WarMaceItem.java:178-184). Like the Flail, it is complete as a weapon and thin as a spectacle: zero particles in both entities.

Pieces: WarMaceSmashEntity (entity/WarMaceSmashEntity.java), WarMaceRiseEntity (entity/WarMaceRiseEntity.java), renderers WarMaceSmashRenderer (111 lines), WarMaceRiseRenderer (95).

### Ability map

Melee (vanilla swing):
- Attribute damage 1299 + 1 = 1300 total (item/WarMaceItem.java:44). Attack speed modifier -3.4 (line 49), the slowest in the group.
- Passive while held: FRICTION +0.9 ADD_MULTIPLIED_BASE, id war_mace_weight (lines 54-56): the wielder is deliberately heavier to move.
- hurtEnemy is an empty no-op by design (lines 61-63).

Left click, Smash (performSmash, lines 65-105; routed from network/DbaNetwork.java:543):
- Stamina: STAMINA_PERCENT 125.0 for everything (line 28). Ground smash pays over 28 ticks, heavy over 48 ticks, via drainStaminaDiscrete (lines 84-87).
- Variant test: heavy when the player is airborne and falling with vertical velocity below -0.08 (line 76).
- Locks: separate static maps. Ground lock NEXT_GROUND = now + 10 ticks (line 91); heavy lock NEXT_HEAVY = now + 16 ticks (line 89); same-tick duplicate guard LAST_SMASH_TICK (lines 71-75). Item cooldown 5 ticks (line 93).
- Damage: ground 900 + Strength x 2.5; heavy 1600 + Strength x 4.0 (line 96). Heavy also resets the player's fall distance at cast (line 88).
- Spawns WarMaceSmashEntity with the damage and heavy flag (lines 97-98). Sounds ANVIL_LAND and GENERIC_EXPLODE, louder and lower when heavy (lines 99-102). DbaImpactFx.heavy at the player position (line 103).

Right click, Rise (use line 107, performRise lines 117-150):
- use() fails on cooldown or empty stamina (lines 108-110), else performRise: lock NEXT_RISE = now + 18 ticks (line 133), same-tick guard, drain 125 percent over 22 ticks (line 130), item cooldown 6 ticks (line 134), fall distance reset (line 135).
- The player launches: velocity += (look.x x 0.18, 1.08, look.z x 0.18), RISE_VERTICAL 1.08 and RISE_LOOK 0.18 (lines 30-32, applied line 137) via MovementPhysics.addVelocity.
- Spawns WarMaceRiseEntity with damage 480 + Strength x 1.5 (line 140). Sounds PLAYER_ATTACK_SWEEP and ANVIL_HIT.

Combo: relaunchFromSmash (lines 152-165), called by the smash entity on its first connect:
- Clears NEXT_HEAVY and NEXT_GROUND for the player, sets (not adds) vertical velocity to RISE_VERTICAL 1.08 plus the look nudge, resets fall distance. This is what makes rise, falling heavy smash, relaunch, falling heavy smash a loop.

Entities:

WarMaceSmashEntity (entity/WarMaceSmashEntity.java):
- Synced HEAVY and DAMAGE (default 900). Life: 10 ticks ground, 14 heavy (life(), lines 51-53).
- radiusAt (lines 55-60): starts 0.7, ease-out cubic to max 4.5 (ground) or 7.6 (heavy) over the life.
- Damage model: an expanding lip. Each tick it tests targets in a box spanning the current radius and y from -0.4 to +2.4 (lines 81-83). After tick 2, a target is hit only if its horizontal distance is between (previous radius - 0.8) and (current radius + 0.6): the hit travels with the ring edge (lines 92-95). On ticks 1 to 2 everything inside is hit. Each target once (hitIds).
- Damage is FLAT: the full smash damage at 0.7 blocks and at 7.6 blocks alike, no falloff (line 101).
- Knockback is deliberately tiny: 0.25 outward plus 0.08 up (line 108), with the code comment stating the mob has to stay under the next falling smash.
- First connect per entity relaunches the owner once via WarMaceItem.relaunchFromSmash (lines 110-113).
- Stationary at the cast position. No particles, no block interaction, no ground mark after the entity dies.

WarMaceRiseEntity (entity/WarMaceRiseEntity.java):
- Synced DAMAGE (default 480). Life 8 ticks (discard when tickCount exceeds 8, lines 48-51).
- Damage happens ONLY on tick 1 (line 53): box inflated (3.2, 1.6, 3.2), distance cap 3.4 (lines 58-63), full rise damage through capDamage, launch 0.45 outward plus 0.95 up (lines 72-73). Ticks 2 to 8 are purely the renderer crescent climbing.
- No particles, no block interaction.

Renderers:

WarMaceSmashRenderer: one submit on kiRenderType. Draws drawShell at the entity's current radiusAt(age): 22 segments around x 5 radial bands from radius x 0.62 to the full radius, bow height (heavy 1.35, ground 0.72) x (0.35 + 0.65 fade), a 3 percent ripple (1 + 0.08 sin(a x 3)), an extra bright mid band, colors bronze: heavy (0.72 to 1.0, 0.42 to 0.70, 0.16), ground (0.78 to 0.94, 0.58 to 0.76, 0.28 to 0.48). Fade is the smoothstep of (1 - age / life), so the shell is at full alpha at spawn: there is no build-in, the shell appears and then settles.

WarMaceRiseRenderer: one submit on kiRenderType. A single crescent: 10 segments, radius 1.15 x (0.45 + 0.55 open) where open eases over 3 ticks, thickness 0.85 x open, climbing lift age x 0.22 per tick, colors from bronze (0.85, 0.62, 0.28) to pale (1.0, 0.9, 0.62) with a bright top edge and a dark underside strip. Fade is smoothstep of (1 - age / 8). Total drawn life 8 ticks, total climb about 1.76 blocks.

### Gap analysis against the 5 points

- Point 1 (layered particle depth): FAIL. Zero particles. A 1600 to 1600+ damage ground slam produces one bowed mesh shell and a shared DbaImpactFx at the player's feet. No dust ring, no particle core, no debris band.
- Point 2 (debris): FAIL. Zero pieces, not even mesh shards. For a weapon whose whole identity is smashing the ground, there is nothing that flies off the impact.
- Point 3 (world interaction): FAIL on world, DELIBERATELY WEAK on mobs. The smash never touches a block: no crack decal, no scarring, no block displacement, and the shell is drawn at the cast height regardless of terrain slope. Mob knockback is 0.25 by design so targets stay in the combo. The rise launches the player 1.08, which is real self-interaction, and the relaunch loop is a genuine mechanic.
- Point 4 (atmosphere): FAIL. Nothing.
- Point 5 (lifecycle): FAIL. The smash shell appears at full alpha (fade only decreases), expands over 10 to 14 ticks, and discards. No build-up before the hit (damage starts tick 1), no lingering scar or settling dust after. The rise crescent lives 8 ticks while its damage is over on tick 1.

### Numbers holding the spectacle back

- Smash damage is instant and flat to 7.6 blocks: a heavy smash pays 1600 + Strength x 4.0 to everything the lip touches with no falloff, so fights end before the 14 tick shell finishes expanding.
- Heavy lock 16 ticks versus the relaunch loop: relaunchFromSmash clears the locks on connect, so a skilled loop is rise (18 tick lock), heavy smash, relaunch, heavy smash, with the shell visuals (14 ticks each) overlapping into a near-constant ring. The numbers encourage spam that the visuals cannot support.
- Rise entity: 1 damage tick out of an 8 tick life. Seven eighths of the effect is a fading crescent with nothing mechanical attached.
- Stamina pays are long (28 and 48 ticks at 125 percent per minute) but invisible: no visual anywhere shows the mace's cost draining.

### Bugs, dead code, mismatches

- WarMaceSmashEntity box (entity lines 81-83) spans y -0.4 to +2.4 around the CAST position and the entity never moves: a heavy smash cast mid-fall freezes its damage volume at the cast height while the player keeps falling, and the renderer shell likewise draws at that height, floating if the cast was high. The item comment frames the heavy smash as a falling blow, but nothing ties the shell to the landing point.
- The lip test (entity lines 92-95) skips the distance test entirely on ticks 1 to 2, so everything within the starting box takes the hit even if it is well inside where the lip has notionally not reached; combined with the flat damage this makes the first two ticks a plain radius blast and only later ticks a traveling ring. Behavior is mixed, not purely lip-based as the class comment ("Hits travel with the lip") claims.
- WarMaceRiseEntity: after its single tick-1 hit pass, the entity has no further server function but stays alive 7 more ticks purely as a render carrier; the DAMAGE synced field is likewise only read on tick 1. Not a bug, but the entity is a visual shell for 87 percent of its life.
- No dead helper methods were found in WarMaceRiseRenderer or WarMaceSmashRenderer; every private helper in both files is called from submit.

## Closing summary for the technique group

- All six weapons are real and complete as mechanics: geo models, both clicks, entities, renderers, cleanup. Spike Flail and War Mace are NOT stubs; they are finished weapons with thin effects.
- The group's shared ceiling against the video standard: mesh-only rendering (zero additive passes in 16 renderers), near-zero particles (and exactly zero for Flail and Mace), zero debris pieces, zero block interaction, zero atmosphere, and lifecycles measured in ticks rather than phases.
- The single strongest spectacle asset in the group is the Bansho cyclone's mob handling (real pull, spin, lift in a 4.0 radius, 6.5 high volume), and it is capped by a 25 tick life, a 30 percent proc gate, about 25 particles per life, a visible funnel wall (1.33 blocks) far smaller than its effect volume (4.0), and no ejection or dissipation phase.
- The single strongest build-up in the group is the Blaster's 100 tick Erasure charge; its payoff is a 16 tick beam that deals all of its damage on tick 1 and emits particles only on ticks 1 to 3.
