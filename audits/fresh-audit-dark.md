# Fresh Audit: Dark and Void Weapons

Scope: Dabura Sword, Curse Blade, Hollow's Edge, Dimensional Sword, Sickle of Sorrow.
Repo state: HEAD 94708b6, clean tree. Method: clean slate. Every claim below was derived by reading the current item classes, every entity they spawn, and every renderer for those entities at this HEAD. No prior audit files were read. Audit only, no code was changed.

Paths below are relative to the repo root. Item and entity code lives under src/main/java/com/dragonblockarcanedba/, renderers under src/client/java/com/dragonblockarcanedba/client/render/.

## Pieces confirmed from the code

All five weapons exist and are registered in item/DbaItems.java:

- Sickle of Sorrow: item/SickleOfSorrowItem.java, registered at DbaItems.java:347 (key at DbaItems.java:96). Entities: entity/SorrowSlashEntity.java, entity/DimensionalRiftEntity.java. Renderers: SorrowSlashRenderer.java, DimensionalRiftRenderer.java.
- Hollow's Edge: item/HollowsEdgeItem.java, registered at DbaItems.java:443 (key at DbaItems.java:186). Entities: entity/HollowChargeEntity.java, entity/HollowAfterimageEntity.java, entity/HollowRushTrailEntity.java, entity/VoidSlashEntity.java, entity/VoidRiftEntity.java. Renderers: HollowChargeRenderer.java, HollowAfterimageRenderer.java, HollowRushTrailRenderer.java, VoidSlashRenderer.java, VoidRiftRenderer.java.
- Dimensional Sword: item/DimensionalSwordItem.java, registered at DbaItems.java:446 (key at DbaItems.java:189). Entities: entity/DimensionalSlashEntity.java, entity/DimensionalWarpRiftEntity.java. Renderers: DimensionalSlashRenderer.java, DimensionalWarpRiftRenderer.java.
- Curse Blade: item/CurseBladeItem.java, registered at DbaItems.java:458 (key at DbaItems.java:201). Entities: entity/CurseChainEntity.java, entity/AbyssalDomainEntity.java, entity/CurseTelegraphEntity.java, entity/CurseLightningEntity.java, entity/SkyCracksEntity.java, entity/CurseGroundShatterEntity.java. Renderers: CurseChainRenderer.java, AbyssalDomainRenderer.java, CurseTelegraphRenderer.java, CurseLightningRenderer.java, SkyCracksRenderer.java, CurseGroundShatterRenderer.java.
- Dabura Sword: item/DaburaSwordItem.java, registered at DbaItems.java:470 (key at DbaItems.java:213). Entities: entity/DarknessChargeEntity.java, entity/DarknessWaveEntity.java, entity/DarknessDomainEntity.java, entity/DarknessBladeEntity.java, entity/DarknessShatterEntity.java. Renderers: DarknessChargeRenderer.java, DarknessWaveRenderer.java, DarknessDomainRenderer.java, DarknessBladeRenderer.java, DarknessShatterRenderer.java.

Nothing is missing and nothing extra was found, with one exception: Curse Blade's SkyCracksEntity and SkyCracksRenderer exist and are spawned in game, but the renderer draws nothing at all (see Curse Blade, bugs).

## Cross-cutting findings (all five weapons)

These were verified once for the whole set and apply to every weapon below.

1. There is no weather, time, or fog control anywhere. A grep for setWeatherParameters, setDayTime, setRainLevel, setThunderLevel, getRainLevel, isThundering, isRaining, and fog APIs across all five item classes and every entity in the set returned zero matches. No ability in this set changes vanilla weather state, time of day, rain or thunder level, fog density, or world light. All atmosphere is faked with translucent mesh domes, custom entities, particles, and WEATHER category sound effects. A player standing outside a domain sees an unchanged sky.

2. The visuals are mesh geometry, not particle systems. Every renderer in the set draws custom vertex geometry through KiRenderHelper.kiRenderType() plus one additive pass through KiRenderHelper.kiAdditiveType(). The geometry is layered and often well faded, but it is rigid: rings, domes, crescents, and shards whose vertices are computed from age each frame. Against point 1 of the standard (layered rotating particle bands with a dense bright core, a mid layer, and wispy edges), the set has layered meshes and almost no particles.

3. Real particle counts are tiny. Server side particle emission at effect density 1.0, counted from the sendParticles calls:
   - DarknessDomainEntity: 5 VOID_SMOKE every 4 ticks plus 2 SPARK every 12 ticks (entity/DarknessDomainEntity.java:83 and :93), about 28 particles per second spread over a domain of radius up to 30.
   - AbyssalDomainEntity: same rates, with KI_MOTE in place of SPARK (entity/AbyssalDomainEntity.java:83 and :93).
   - DarknessWaveEntity: 3 VOID_SMOKE every 2 ticks (entity/DarknessWaveEntity.java:153), about 30 per second along the flight path.
   - VoidRiftEntity: 3 VOID_SMOKE every 3 ticks plus 2 SPARK every 6 ticks (entity/VoidRiftEntity.java:97 and :107), about 27 per second, emitted only at the rift center even when the radius is 9.
   - DimensionalRiftEntity: 4 SPARK plus 2 VOID_SMOKE every 8 ticks (entity/DimensionalRiftEntity.java:93 and :96), about 15 per second across a radius that grows to 15, plus one collapse burst of 14 smoke and 10 spark at tick 50.
   The reference storm carries hundreds of simultaneous moving pieces. Every effect here runs under 30 particles per second and leans on mesh for its body.

4. There is no distance scaling anywhere. A grep for distance based logic across all 20 renderers in the set returned zero matches. Segment counts, shard counts, and alphas are constants. A 30 radius domain costs the same vertex count at 200 blocks away as at point blank, and particles do not thicken or thin with range. DbaEffectsConfig.effectDensity() scales only the server particle counts above.

5. Almost nothing touches blocks. Across all five weapons, the only block ever written is one FIRE block placed by Curse Blade's telegraphed lightning strike when the ground cell is air over a solid block (item/CurseBladeItem.java, strike branch). Nothing breaks blocks, lifts blocks, scars ground persistently, or rains debris back down. Every ground effect (fissures, shatter rings, ground seals) is a renderer decal on a short lived entity and leaves the world untouched when it ends.

6. Debris, in the video sense, does not exist. The closest pieces are fixed count mesh shards: 12 rising slabs in DarknessShatterRenderer and its CurseGroundShatterRenderer clone, 16 orbiting spires in each domain renderer, 18 orbiting spikes in DimensionalRiftRenderer, 8 to 14 converging shards in each charge renderer. Counts run 8 to 18, sizes are near uniform, motion is a ring rotation or a rise in place. None of them tumble, none follow helical paths, none rain down at the end.

7. Domain lifecycle pops. Both domain renderers (DarknessDomainRenderer, AbyssalDomainRenderer) use constant alphas with no fade in or fade out. Both domain entities stay alive only because the item channel refreshes a 10 tick lifetime field every tick. The dome appears at full alpha on the first frame and vanishes the same tick the channel ends or is interrupted. This is the largest single violation of point 5 in the set.

---

## 1. Dabura Sword

Files: item/DaburaSwordItem.java (365 lines). Entities: DarknessChargeEntity, DarknessWaveEntity, DarknessDomainEntity, DarknessBladeEntity, DarknessShatterEntity.

### Ability map

Melee (DaburaSwordItem.java createModifiers, :61):
- Base damage 800 (1 + 799), attack speed modifier -2.4.
- hurtEnemy: Ki drain discrete (35.0 over 10 ticks) and Petrification Curse for 40 ticks, amplifier 0, on the target.

Left click, Abyssal Slash (charge and release):
- Charge cap MAX_LEFT_CHARGE_TICKS = 160 ticks, 8 seconds (DaburaSwordItem.java:46). While charging: Ki drain 35 percent per minute per tick, movement dampened by factor 0.4, DarknessChargeEntity follows the player and mirrors the charge ratio.
- Release damage: 400 + chargeRatio * 800 + Strength * 3 * chargeRatio (DaburaSwordItem.java:143 to 144). Full charge, zero Strength: 1200.
- Wave speed: 1.6 + chargeRatio * 1.0 blocks per tick.
- At chargeRatio 0.95 or higher, two extra waves fire at 0.5 times damage with speed factors 0.78 and 0.58.
- Cooldown: 20 + chargeRatio * 40 ticks, so 20 to 60 ticks (DaburaSwordItem.java:179 to 180).

Right click, World of Darkness (channel) and Abyssal Eclipse (finisher):
- Channel cap MAX_RIGHT_CHANNEL_TICKS = 300 ticks, 15 seconds (DaburaSwordItem.java:47). DarknessDomainEntity follows the owner, radius grows 8 to 30.
- Inside the domain: enemies get Petrification Curse 40 ticks reapplied continuously, and hostile projectiles have their velocity scaled to 0.05.
- Full eclipse fires only at exactly heldTicks == 300 (DaburaSwordItem.java:264): burst damage 1200 + Strength * 4 (DaburaSwordItem.java:275) to every enemy in the domain, their velocity is set to (0, 0.4, 0) so they hang suspended, Petrification Curse 60 ticks amplifier 1, Cinematic Tracking 60 ticks, 10 DarknessBladeEntity spawn 18 to 23 blocks above targets and fall at -1.8 per tick dealing 350 damage each, plus a DarknessShatterEntity of radius min(12, radius * 0.5). Cooldown 100 ticks (DaburaSwordItem.java:312).
- Partial eclipse on release at 200 to 299 ticks: same burst at 0.6 * chargeRatio scaling (DaburaSwordItem.java:320), cooldown 80 ticks (DaburaSwordItem.java:351).
- Release under 200 ticks: no effect, cooldown 40 ticks (DaburaSwordItem.java:353).

DarknessWaveEntity (entity/DarknessWaveEntity.java):
- noPhysics, pierces terrain, discards when tickCount exceeds 40, so maximum range is about 40 ticks times speed (up to roughly 104 blocks at full charge speed 2.6).
- Hitbox inflate 3.5, or 2.2 for the secondary waves. On hit: the wave damage, Petrification Curse, Dark Faded 100 ticks, friction and air drag attribute modifiers, knockback 1.2.
- Particles: 3 VOID_SMOKE every 2 ticks (DarknessWaveEntity.java:153).

DarknessBladeEntity (entity/DarknessBladeEntity.java):
- Falls at the velocity set by the item, checks impact by onGround() or a non air block at its own position or tickCount over 40, then deals its damage in a splash inflated 4.0 by 2.0 and spawns a DarknessShatterEntity of radius 2.8.
- Particles: VOID_SMOKE 3 and SPARK 1 every 2 ticks.

DarknessDomainEntity (entity/DarknessDomainEntity.java):
- Lifetime field is 10 ticks, refreshed by the item each tick while the channel holds. Follows the owner.
- Particles: 5 VOID_SMOKE every 4 ticks in a radius * 0.6 box, 2 SPARK every 12 ticks (DarknessDomainEntity.java:83, :93).
- The entity itself applies no effects; all domain mechanics live in the item's onUseTick.

DarknessShatterEntity (entity/DarknessShatterEntity.java):
- Lifetime 40 ticks. Particles only while tickCount is 8 or less, then particle silent for the remaining 32 ticks while its renderer keeps drawing.

Renderers:
- DarknessChargeRenderer: 3 counter rotating ground rings, 8 to 14 inward converging shards (count scales with charge), 2 to 3 nested octahedra, twin helical ribbons of 12 steps, plus an additive halo pass. All mesh. Scales honestly with charge ratio.
- DarknessWaveRenderer: 4 nested volumetric crescent layers plus an additive shell, 8 leading fangs, 5 trailing shard triangles. The wave opens over its first 5 ticks and fades from tick 30 to 40, matching the entity's 40 tick life. This is the best lifecycle match in the weapon.
- DarknessDomainRenderer: 4 counter rotating ground whirlpool bands (48 segments), an outer dome of 12 latitudes by 36 longitudes, an inner dome of 6 by 24 counter rotating, 16 orbiting spires, an 8 level central vortex column, and renderer side lightning: 4 arcs of 5 ribbon segments each, reseeded every 3 ticks (DarknessDomainRenderer.java:241 onward). Dome height is capped at min(26, radius * 1.35). Alphas are constants: no fade in, no fade out.
- DarknessBladeRenderer: a layered greatsword mesh (aura shell, obsidian core, pulsing fuller, crossguard, hilt, plunge wake cones), fade in over 3 ticks.
- DarknessShatterRenderer: 3 expanding shock rings, 8 fissure trenches of 5 segments, 12 debris slabs, a central eruption column, additive halo. Fade is computed as 1 - age / lifetime and correctly reaches 0 at tick 40.

### Gap analysis against the 5 points

Point 1, layered particle depth: FAIL on particles, PARTIAL on layering. The dome, wave, and charge are genuinely multi layer meshes. But the domain that covers a 30 block radius runs about 28 particles per second total, and they are emitted in a flat box at y + 1.0 and y + 0.6, not in rotating bands at core, mid, and edge heights. There is no dense bright core of particles anywhere; brightness lives in mesh vertex colors. No distance scaling.

Point 2, debris: FAIL. The only debris is the shatter renderer's 12 slabs. They rise in place to a seeded height, lean outward, and fade. They do not orbit, tumble, or fall. The eclipse drops 10 swords, which read as projectiles, not debris, and they vanish on impact leaving only a 2.8 radius shatter decal.

Point 3, world interaction: FAIL. No block is ever broken, placed, or moved by any Dabura ability. Mobs are petrified and, at the eclipse, frozen mid air with velocity zeroed; nothing is pulled toward the domain center, lifted by the vortex column that the renderer draws, or flung out at the end. The vortex is a picture with no force behind it. There are no dust bursts at the dome base; the ground whirlpool bands are decals at fixed heights (y 0.08 to 0.23 in the renderer) that do not react to terrain.

Point 4, atmosphere: FAIL. No weather, time, fog, or light calls (cross-cutting finding 1). The sky does not darken; the dome is a translucent shell drawn around the player and world light inside it is unchanged. The renderer's 4 lightning arcs are short (5 segments, starting 6 to 16 blocks up, inside the dome), never branch, never strike the ground, and have no sound or gameplay effect. The entity class comment promises atmospheric dark lightning that the entity never spawns; the arcs exist only in the renderer.

Point 5, lifecycle: MIXED. The charge builds honestly over 160 ticks and the charge renderer scales with it. The wave opens and fades on schedule. The shatter expands and fades to zero. But the headline effect, the domain, pops in at full alpha and pops out the tick the channel ends, and the eclipse is all or nothing at exactly tick 300: release at 299 and the burst, blades, and suspension are replaced by a 0.6 scaled burst with no blades at all.

### Numbers holding the spectacle back

- Domain particle emission: 5 smoke per 4 ticks over radius up to 30. A storm of that footprint needs orders of magnitude more, or a real particle band system in the renderer.
- Eclipse blade count is a flat 10 whether the domain radius is 8 or 30, so a full size domain looks emptier than a small one.
- The full finisher demands the entire 300 tick channel; the 200 to 299 partial window delivers 0.6 * chargeRatio of the burst (at 200 ticks that is 0.4 of full) and none of the blade rain that sells the move.
- Wave lifetime is capped at 40 ticks while the fade starts at tick 30, so a full charge wave spends a quarter of its visible life already dying.
- DarknessShatterEntity stops emitting particles after tick 8 of its 40 tick life, so impacts go quiet while the decal is still large.

### Bugs, dead code, mismatches

- entity/DarknessBladeEntity.java: the damage field defaults to 400.0f but the item always passes 350.0f. The default is dead and disagrees with the only call site.
- entity/DarknessBladeEntity.java: impact detection calls onGround() on an entity with noPhysics set, which is unreliable; the block at position check does the real work.
- entity/DarknessWaveEntity.java and entity/DarknessBladeEntity.java: unused imports (DustParticleOptions, ParticleTypes, MobEffects in the wave; Dust and ParticleTypes in the blade).
- entity/DarknessDomainEntity.java: class comment advertises atmospheric dark lightning; the entity spawns none.
- The domain has no teardown path: if the channel is interrupted by anything other than a clean release, the entity simply stops being refreshed and the dome vanishes mid frame (see cross-cutting finding 7).

---

## 2. Curse Blade

Files: item/CurseBladeItem.java (381 lines). Entities: CurseChainEntity, AbyssalDomainEntity, CurseTelegraphEntity, CurseLightningEntity, SkyCracksEntity, CurseGroundShatterEntity.

### Ability map

Melee:
- Base damage 750, attack speed modifier -2.0.
- hurtEnemy is deliberately empty. A code comment states the curse chain and the domain are the payoff, so a plain melee hit does base damage and nothing else: no curse, no drain, no effect.

Left click, curse chain stream (static streamCurseChain):
- Cooldown 5 ticks (CurseBladeItem.java:97). Ki drain discrete (30.0 over 4 ticks) per shot.
- Targeting: search radius 24 blocks, prefers a target in a cone of dot product above 0.3, else the nearest.
- CurseChainEntity (entity/CurseChainEntity.java): health 60 and isPickable true, so enemies can destroy chains. Homes toward the target in steps of 1.6 blocks per tick. Attaches when the squared distance is under 1.8. Attach timer 100 ticks. On attach: 35 damage, and Movement Curse amplifier increments by 1 per attached chain, capped at amplifier 9 (10 stacks). The owner heals 2.0 + amplifier * 1.5 per attach, or gains 4 absorption instead if at full health. At amplifier 4 or higher, each attach also pulls the target 0.7 toward the owner and restores 10 Ki and 5 Stamina to the owner. When 10 chains are attached to one target, new chains retarget.

Right click, Cursed Tempest (channel):
- Channel cap MAX_ECLIPSE_CHANNEL_TICKS = 300 ticks, 15 seconds (CurseBladeItem.java:40). AbyssalDomainEntity follows the owner, radius grows 10 to 30.
- Enemies inside: Storm of Darkness for 60 ticks at amplifier (int)(stormRatio * 3), and Cinematic Tracking 60 ticks.
- Wind toss: every 5 ticks, each enemy has a 0.6 probability of a random horizontal push of up to plus or minus 0.75 and an upward push of 0.6.
- Disarm: every 20 ticks, each enemy has a 5 percent chance to drop its main hand item, with pickup delay 40 ticks.
- Telegraphed lightning: every 40 ticks (CurseBladeItem.java:251) a CurseTelegraphEntity (radius 1.8, lifetime 20 ticks) is placed on the enemy with the highest Movement Curse. At heldTicks % 40 == 20 (CurseBladeItem.java:259) a CurseLightningEntity strikes that spot: damage 450 + Strength * 1.5 (CurseBladeItem.java:270), target ignited for 5 seconds, and a real FIRE block is set on the ground if the cell is air over a solid block. The strike is telegraphed as a rare bolt 15 percent of the time.
- Ambient lightning: every tick, a 0.12 probability strike anywhere in the domain at MOTION_BLOCKING height (CurseBladeItem.java:293), 20 percent rare. It deals only 15 damage in a 3 block box, ignites for 5 seconds, and deliberately sets no block fire (per its code comment, to avoid uncontrolled spread).
- Sky cracks: every 80 ticks a SkyCracksEntity is spawned at fixed y = 280.
- Slam phase: once heldTicks reaches 300, every 20 ticks (CurseBladeItem.java:334) every enemy with Movement Curse amplifier 4 or higher is slammed: vertical velocity -2.5, damage 150 + Strength * 0.8 (CurseBladeItem.java:336), enemies at amplifier 9 are rooted for 60 ticks, and a CurseGroundShatterEntity of radius 3.5 and lifetime 35 ticks spawns at each slam.
- Release cooldown: 40 + stormRatio * 60 ticks, so 40 to 100 (CurseBladeItem.java:377). There is no partial finisher on early release: the tempest just ends.

CurseLightningEntity (entity/CurseLightningEntity.java): lifetime 20 ticks, one synced boolean IS_RARE. It deals no damage itself; all strike damage is applied by the item at spawn time.

CurseTelegraphEntity (entity/CurseTelegraphEntity.java): lifetime 20 ticks, radius 1.8, exposes a progress value the renderer uses for its shrinking indicator.

AbyssalDomainEntity (entity/AbyssalDomainEntity.java): a field for field clone of DarknessDomainEntity, including the 10 tick refreshed lifetime. Particles: 5 VOID_SMOKE every 4 ticks, 2 KI_MOTE every 12 ticks.

SkyCracksEntity (entity/SkyCracksEntity.java): tick counts to 100 and discards (SkyCracksEntity.java:21 to 26). It has no mechanics, no particles, and an empty defineSynchedData.

Renderers:
- AbyssalDomainRenderer: a near exact copy of DarknessDomainRenderer with crimson and violet colors swapped in, slightly faster band rotation, and dome height capped at min(24, radius * 1.30) instead of 26. Same 4 ground bands, same double dome, same 16 spires, same vortex column, same style of short interior lightning arcs. Same constant alphas, same pop in and pop out.
- CurseChainRenderer: physical 3D chain links (12 segments per link profile) drawn along a spine between owner and target, with a collar and an octahedron curse mark at the attach point. The chain is the most physically grounded visual in the set.
- CurseTelegraphRenderer: dual counter rotating pentagram rings, an indicator ring that shrinks to zero as progress reaches 1, rising beacon pillars, a flashing central hex eye, and an additive glow pass. A genuinely good telegraph.
- CurseLightningRenderer: a 16 segment jagged trunk with a white core and a fading wake, 4 forks of 3 segments each branching off the trunk, and an expanding ground ring. The bolt starts at a fixed local height of 16.0 blocks (the cloud constant in the renderer) regardless of where it strikes. The strike draws over 2.5 ticks and fades out by about tick 9.5; the renderer returns early once flashTime exceeds 12, while the entity lives 20 ticks.
- CurseGroundShatterRenderer: a recolored copy of DarknessShatterRenderer (crimson instead of violet), same 3 rings, 8 trenches, 12 slabs, same corrected fade.
- SkyCracksRenderer: submit() calls only super.submit and draws nothing (SkyCracksRenderer.java:48 to 50). Its own class comment says the sky picture is not drawn and that the bolts live on the dome and the strike.

### Weather and atmosphere audit (audited hardest, per assignment)

What exists today, in full:
- Vanilla weather state: untouched. No rain level, thunder level, weather timer, or time calls exist in the item or any entity (grep verified, cross-cutting finding 1).
- Lightning: a custom mesh entity (CurseLightningEntity plus CurseLightningRenderer), not vanilla lightning. It produces no vanilla thunder, no light flash in the world lighting engine, no sky brightening, and no fire except the one block the item sets for telegraphed strikes.
- Sound: thunder is imitated with WEATHER category sound effects at strike time.
- Sky: SkyCracksEntity is spawned at y = 280 every 80 ticks and renders nothing. There is no sky darkening, no cloud layer, no haze, and no fog change at any point in the channel.
- The dome mesh is the entire atmosphere. Inside a 30 radius dome the world stays at full daylight brightness, because nothing changes light levels and the dome is translucent geometry, not a lighting effect.

### Gap analysis against the 5 points

Point 1: FAIL on particles, PARTIAL on layering. Same mesh dome family as Dabura. The tempest, supposedly a storm, emits the same roughly 28 particles per second as the darkness domain. A storm with no rain particles, no wind streaks, and no layered bands of moving particles is a dome with lightning pictures.

Point 2: FAIL. The tempest has no debris at all. Nothing is picked up, nothing orbits, nothing rains down. The only ground debris in the weapon is the slam shatter's 12 static slabs, borrowed from the Dabura shatter renderer.

Point 3: PARTIAL, the best of the domain weapons. The wind toss does move mobs (random pushes every 5 ticks), chains physically pull cursed targets, and slams drive targets into the ground. But the motion is random jitter, not a storm pattern: nothing spirals inward, nothing is lifted into the air column the renderer draws, and blocks are never affected apart from the single FIRE block.

Point 4: FAIL, and this is the weapon whose theme most demands it. The sky never darkens, there is no real thunder or lightning in the engine sense, ambient bolts are 16 blocks tall starting from a fixed local height rather than from the dome top or the sky, and the one entity built to sell the sky (SkyCracksEntity) is a no-op that renders nothing. The atmosphere is a sound effect and a translucent dome.

Point 5: MIXED. The telegraph to strike rhythm (20 tick warning, strike, repeat every 40 ticks) is a real build and release beat, and the telegraph renderer sells it. But the tempest itself has no build up beyond the dome radius growing, no peak phase change other than slams unlocking at tick 300, and no dissipation: release the channel and the storm vanishes the same tick. An early release buys nothing at all, not even a scaled finisher.

### Numbers holding the spectacle back

- Ambient lightning damage is 15 against the telegraphed strike's 450 + Strength * 1.5, a 30 to 1 ratio. The frequent bolts that make the storm look alive are mechanically invisible, so players learn to ignore the storm's main visual.
- Ambient strike probability 0.12 per tick averages about 2.4 strikes per second across the whole domain, which sounds busy, but each bolt is only 16 blocks tall and lands at a random point, so most strikes happen far from any viewer focus.
- The slam phase, the tempest's only finisher, requires the full 300 tick channel and then ticks every 20 ticks for 150 + Strength * 0.8, less than a third of one telegraphed strike.
- Disarm at 5 percent per 20 ticks per enemy is a roughly once per 20 seconds event per enemy: too rare to read as a storm stripping weapons.
- Chain attach damage 35 against a 750 base melee and 450 strike damage makes the left click read as a debuff applicator only; its heal (2.0 + amplifier * 1.5, at most 15.5) is small next to the damage numbers flying around it.

### Bugs, dead code, mismatches

- SkyCracksEntity plus SkyCracksRenderer are dead weight in game: the item spawns the entity every 80 ticks at y = 280, it renders nothing, does nothing, and discards after 100 ticks. Either the sky visual needs to exist or the spawn should not happen. The entity also carries an unused CompoundTag import and an empty defineSynchedData.
- CurseLightningRenderer stops drawing after flashTime 12 while CurseLightningEntity lives 20 ticks: the last 8 ticks of every strike are invisible. Harmless, but the two lifetimes disagree.
- AbyssalDomainEntity and AbyssalDomainRenderer duplicate DarknessDomainEntity and DarknessDomainRenderer almost line for line (the renderer even keeps the same structure and helper code, with colors, two rotation speeds, and the dome cap changed). Any fix to one domain, including the pop in and pop out problem, has to be made twice or the two weapons drift apart.
- Fire behavior is inconsistent between the two strike types in the same ability: the telegraphed strike sets a FIRE block, the ambient strike is coded not to. A player sees one bolt start a fire and the next identical looking bolt not.
- Disarmed weapons drop with pickup delay 40 ticks and nothing prevents the same enemy from picking the weapon straight back up, so the disarm often undoes itself.

---

## 3. Hollow's Edge

Files: item/HollowsEdgeItem.java (415 lines). Entities: HollowChargeEntity, HollowAfterimageEntity, HollowRushTrailEntity, VoidSlashEntity, VoidRiftEntity.

### Ability map

Melee:
- Base damage 850 (1 + 849), attack speed modifier -1.8, the hardest hitting melee in this set.
- hurtEnemy: Ki drain discrete (20.0 over 7 ticks), Dark Faded 40 ticks on the target.

Left click, Hollow Rush (charge, then dash window):
- Charge cap MAX_LEFT_CHARGE_TICKS = 100 ticks, 5 seconds (HollowsEdgeItem.java:50). While charging: Ki drain 30 percent per minute, movement dampen 0.3, HollowChargeEntity mirrors the charge.
- Release opens a rush window of max(40, chargeRatio * 200) ticks, so 40 to 200 ticks (HollowsEdgeItem.java:141).
- Dash outside the window: a fallback 3 block blink, Ki drain discrete (30.0 over 20 ticks), cooldown 10 ticks (HollowsEdgeItem.java:188).
- Dash inside the window: free, 5 blocks. Path damage 450 + Strength * 2.5 (HollowsEdgeItem.java:264), multiplied by 1.8 on every third dash (HollowsEdgeItem.java:265). Every dash spawns a HollowAfterimageEntity and a HollowRushTrailEntity, and the dasher gains HOLLOWED for 60 ticks.
- Every third dash also fires a VoidSlashEntity at baseDmg * 1.5, with velocity set from the look vector scaled by 1.8 (HollowsEdgeItem.java:279). A third dash slash therefore carries (450 + Strength * 2.5) * 1.8 * 1.5 before the entity's own multipliers.
- HollowAfterimageEntity: lifetime 100 ticks, applies Dark Faded 60 ticks to enemies within 4 blocks every 5 ticks, emits VOID_SMOKE every 4 ticks.
- HollowRushTrailEntity: maximum lifetime 14 ticks, purely visual.
- VoidSlashEntity: lifetime 40 ticks, hitbox inflate 2.5 by 1.2, Dark Faded 80 ticks amplifier 1 on hit, VOID_SMOKE every 2 ticks.

Right click, Void Rift (channel, teleport, implode):
- use() spawns a VoidRiftEntity at the crosshair point (eye plus look * 12, block clip out to 32 blocks, raised 0.5 on a block hit) with starting radius 2.5 and damage 550 (HollowsEdgeItem.java:315).
- onUseTick: the rift follows the crosshair (clip 48, lerp 0.35), radius grows 2.5 to 9.0, and damage is recomputed every tick as (550 + Strength * 2) * (0.7 + 0.6 * chargeRatio) (HollowsEdgeItem.java:367).
- Channel phase mechanics (entity/VoidRiftEntity.java): everything within radius * 1.8 is pulled toward the center; every 10 ticks, any LivingEntity inside the entity's axis aligned bounding box takes 5 percent of the rift damage and has Dark Faded 20 ticks refreshed. If the channel runs past 400 ticks the rift auto implodes.
- releaseUsing: if the player is aiming at the rift (dot product above 0.85, distance 40 or less) the player teleports into it and gains RIFTED for 200 ticks. Then triggerImplosion() starts a 10 tick suction phase in which the velocity of all entities within radius * 2.5, including items and projectiles, is set to 1.2 toward the center, followed by a burst dealing the full rift damage to LivingEntities in a radius * 2.2 box with an outward launch of 2.2 plus 0.8 upward and Dark Faded 100 ticks amplifier 1.
- Cooldown on release: 60 ticks (HollowsEdgeItem.java:398). Radius is clamped between 1.5 and 12 by setRadius.

Renderers:
- HollowChargeRenderer: structurally the DarknessChargeRenderer with a larger base radius (1.4 + charge * 1.6) and a different palette: same 3 ground rings, same 8 to 14 converging shards, same nested octahedra, same twin helical ribbons.
- HollowAfterimageRenderer: a ghost body in layered passes (outer halo, skin tinted spectral body, hair pass, a central eye slit, scanline slices, speed ribbons, a footstep ripple ring, and rising polygon motes). Alpha decays with progress squared over the 100 tick life. The most characterful renderer in the set, and its lifecycle is honest.
- HollowRushTrailRenderer: void arcs and closing rings over the 14 tick life, alpha eased by progress, with a third dash variant at higher alpha.
- VoidSlashRenderer: 4 concentric void rings with edge treatment, opening on spawn and closing over the last 30 percent of the 40 tick life.
- VoidRiftRenderer: a camera facing tear (the pose is rotated by the camera orientation quaternion) built from layered cuts: throat, inner wall, outer lip, flare, wake, and a white seam, plus shard geometry. The collapse is choreographed inside the synced 10 tick implode window, with the body, rim, seam, and shards fading on staggered schedules. There is no fade in: when not imploding, body alpha is a flat 1.0 from the first frame, softened only by the server side radius growing from 2.5.

### Gap analysis against the 5 points

Point 1: PARTIAL. The charge and slash visuals are layered meshes, and the afterimage is genuinely multi pass. But the rift, the weapon's biggest object at radius up to 9, emits about 27 particles per second from its center point only. The rim, where a viewer reads the effect's size, is particle silent. No rotating particle bands exist anywhere in the weapon.

Point 2: FAIL. No debris of any kind. The rift pulls real entities but spawns no orbiting pieces, and the implosion throws mobs out without a single block fragment or shard entity following them.

Point 3: PARTIAL, the strongest in the set for mobs. Suction at velocity 1.2 applied to everything in radius * 2.5, a channel pull at radius * 1.8, and a 2.2 outward launch are real forces. But blocks are completely exempt: a max size rift channeled inside a structure moves no block, and the ground under it shows no scar when it ends. Dust bursts do not exist; the base of the rift has no ground interaction visual at all, since the tear is camera facing and floats at the crosshair point.

Point 4: FAIL. No atmosphere calls of any kind. A 9 radius tear in space does not dim light, darken the sky, or add haze; outside the tear mesh the world is unchanged.

Point 5: MIXED. The dash kit is the best micro lifecycle work in the set: afterimages decay over 100 ticks, trails close over 14, slashes close over their last 30 percent. The rift is the opposite: it pops in at full alpha, can sit in channel phase for up to 400 ticks (20 seconds, four times the 100 tick charge that feeds the other ability), and its entire dissipation is compressed into the 10 tick implode, which is fast enough to read as a snap rather than a collapse, the renderer choreography notwithstanding.

### Numbers holding the spectacle back

- Channel phase tick damage is 5 percent of rift damage every 10 ticks. At base that is 27.5 damage per half second from an ability whose burst is 550 plus scaling: the long channel feels weak next to its own ending, which pushes players to release instantly and skip the spectacle entirely.
- Rift radius caps at 9.0 from the item (setRadius allows 12), while its pull reaches radius * 2.5, about 22 blocks: the visible tear is much smaller than the force it exerts, so mobs slide toward empty air well outside the visual.
- The implode suction window is 10 ticks for everything the rift spent up to 400 ticks gathering; there is no staged collapse.
- Afterimage Dark Faded (60 ticks, reapplied every 5 ticks within 4 blocks for 100 ticks) outlasts and outreaches the dash that made it, so the faint visual carries a longer debuff than the weapon's own melee applies (40 ticks).

### Bugs, dead code, mismatches

- entity/VoidRiftEntity.java: channel phase damage is tested against the axis aligned bounding box with no distance check, so a target at a box corner, about 1.41 times the radius away diagonally and visibly outside the tear, takes the same tick damage as a target at the center.
- entity/VoidSlashEntity.java: the damage field defaults to 600.0f, but the item always sets damage from its own formula. The default is dead and matches nothing.
- entity/VoidRiftEntity.java: both the channel pull and the implode suction apply to all entities with no exclusions, so the owner's own dropped items, projectiles in flight, and loot from kills are dragged in and then launched by the burst.
- HollowChargeRenderer duplicates DarknessChargeRenderer's geometry code with palette and radius changes, the same two copies to maintain problem as the domain pair.

---

## 4. Dimensional Sword

Files: item/DimensionalSwordItem.java (189 lines, the smallest item class in the set). Entities: DimensionalSlashEntity, DimensionalWarpRiftEntity.

### Ability map

Melee:
- Base damage 750 (1 + 749) (DimensionalSwordItem.java:41), attack speed modifier -2.4.
- hurtEnemy: stamina drain discrete (100.0 over 13 ticks), Melting 200 ticks amplifier 1.

Left click, dimensional slash (static fireSlash):
- Cooldown 8 ticks (DimensionalSwordItem.java:93), stamina drain discrete (100.0 over 13 ticks) per shot.
- Damage is a flat 750.0f (DimensionalSwordItem.java:98). No Strength, Spirit, or any other stat scaling, the only projectile in the set with none.
- Slash speed 2.5 blocks per tick. Tilt alternates left and right using a "tiltRight" flag stored in the stack's CUSTOM_DATA.
- DimensionalSlashEntity (entity/DimensionalSlashEntity.java): hit detection via ProjectileUtil.getHitResultOnMoveVector; discards on any hit, entity or block; times out when tickCount exceeds 100, giving a maximum range of about 250 blocks. On hit: Melting 300 ticks amplifier 1, plus friction -0.90 and air drag -0.80 attribute modifiers. Particles: VOID_SMOKE and SPARK every 2 ticks.

Right click, Dimensional Warp:
- Cost is paid before aiming resolves: Ki drain discrete (100.0 over 100 ticks) is charged first, then the raycast runs (item/DimensionalSwordItem.java, use flow). Cooldown 100 ticks (DimensionalSwordItem.java:184).
- Block raycast up to 1024 blocks (DimensionalSwordItem.java:126). If the ray hits nothing (aiming at sky), the destination is 128 blocks forward along the look vector (DimensionalSwordItem.java:145).
- Two DimensionalWarpRiftEntity spawn: the origin rift bursts for 250 damage (DimensionalSwordItem.java:154) and the destination rift for 750 (DimensionalSwordItem.java:164), both radius 3.5.
- DimensionalWarpRiftEntity: maximum lifetime 24 ticks; each target is damaged once (a damagedTargets set); Melting 200 ticks amplifier 1; the origin rift pulls targets at 0.35, the destination pushes at 0.5.
- The warping player receives RIFTED 60 ticks and DARK_FADED 35 ticks on themselves.

Passives, charges, finishers: none. This is the only weapon in the set with no charge mechanic, no channel, and no finisher.

Renderers:
- DimensionalSlashRenderer: a layered crescent in the DarknessWaveRenderer family (outer shell, core, bright edge, tip spurs, trailing ribbons). Fade in over 5 ticks; fade out only from age 70 to 100.
- DimensionalWarpRiftRenderer: a vertical tear slit with a wake, scaled by a sin(progress * PI) fade over the 24 tick life, opening and then shutting. Clean lifecycle, the best behaved small effect in the set.

### Gap analysis against the 5 points

Point 1: PARTIAL to FAIL. The slash is a layered mesh crescent with two particle types every 2 ticks from the entity. There are no bands, no core to edge particle gradient, and at speed 2.5 the slash crosses most fights in a few ticks, so even its mesh layering barely registers.

Point 2: FAIL. No debris. The warp tears open and shut and spawn nothing physical; a sword whose theme is cutting space leaves no fragments, no shards, no residue.

Point 3: FAIL. The warp's origin pull (0.35) and destination push (0.5) are the weakest forces in the set and apply only inside radius 3.5 for 24 ticks. The slash flies up to 250 blocks and through the world without cutting, marking, or scarring a single block; it discards silently on the first block it touches.

Point 4: FAIL. No atmosphere of any kind. A 1024 block teleport and a space cutting slash produce no light, sky, or fog change at either end.

Point 5: MIXED. The warp rift's 24 tick sin fade is a proper build and dissipate in miniature. The slash is the reverse: it discards the instant it hits anything, with no impact burst, no scar, and no fade, and its renderer fade out window (age 70 to 100) can only ever be seen on a complete miss, because any hit ends the entity first.

### Numbers holding the spectacle back

- Flat 750 damage with an 8 tick cooldown is the highest sustained ranged output in the set (about 93 damage per tick of cooldown before melee), while its siblings scale with stats: Dabura's wave reaches 1200 + Strength * 3, Hollow's dash deals 450 + Strength * 2.5, the Sickle deals 750 + Strength * 3. The Dimensional slash starts equal and falls behind every level, and its spam rate caps how heavy its visual is allowed to feel.
- Warp asymmetry (250 origin, 750 destination) is invisible to the player: both rifts render at the same size and the origin burst, the one at the player's own feet, is the weak one.
- The 100 Ki cost is charged before the raycast, so a misclick into the sky still costs the full drain and the 100 tick cooldown buys a 128 block blind warp.

### Bugs, dead code, mismatches

- The warping player is given DARK_FADED 35 ticks by their own ability (DimensionalSwordItem.java, warp application). Every other use of Dark Faded in the set is an offensive debuff applied to enemies; here the user debuffs themselves for teleporting.
- DimensionalSlashRenderer prepares a fade out for ages 70 to 100 that is unreachable in normal play because DimensionalSlashEntity discards on first hit; only a shot fired into open sky ever shows it.
- The slash stores its alternating tilt in the item stack's CUSTOM_DATA, so two Dimensional Swords in the same inventory share nothing but also read as per stack state on an item that stacks to 1 and is normally used from one hand; harmless, but it is state kept in an unusual place compared to the rest of the set, which keeps ability state on the player or the entity.

---

## 5. Sickle of Sorrow

Files: item/SickleOfSorrowItem.java (247 lines). Entities: SorrowSlashEntity, DimensionalRiftEntity.

### Ability map

Item passives (SickleOfSorrowItem.java createModifiers):
- A nameplate stealth modifier of -60.0 (SickleOfSorrowItem.java:90).
- An ethereal drag modifier of -0.60 applied as ADD_MULTIPLIED_BASE (SickleOfSorrowItem.java:96).

Melee (executeTechniqueMelee):
- Base damage 750 (1 + 749) (SickleOfSorrowItem.java:73). Total melee damage 750 + Strength * 3, with the Strength portion applied as a separate hurt call (SickleOfSorrowItem.java:137).
- On hit: Melting amplifier 2 (Melting III) for 300 ticks, Sorrow Rift for 80 ticks, Ki drain set by PlayerStats.getSickleBaseActionDrain(techniqueLevel) spread over 13 ticks.
- Soul Rend: the owner heals 3 percent of the total damage dealt.
- A SorrowSlashEntity impact burst is spawned at the target with its damage set to 0.0f: a visual only entity (see bugs).

Air swing (performTechniqueAirSwing):
- Ki drain spread over 8 ticks. Damage 750 + Strength * 3 (SickleOfSorrowItem.java:173). Wave speed 1.6.
- SorrowSlashEntity as a wave: lifetime 30 ticks (16 in impact burst mode), dies on the first block clip, hitbox inflate 3.8 by 1.6. Per target hit: Melting III 300 ticks, Sorrow Rift, and a 3 percent Soul Rend heal to the owner per target.

Right click, Dimensional Rift:
- Two cooldown gates run at once: the vanilla item cooldown of 60 ticks (SickleOfSorrowItem.java:204) and a static RIFT_COOLDOWNS map keyed by player UUID (SickleOfSorrowItem.java:210), pruned at the top of each call (SickleOfSorrowItem.java:218) and checked at SickleOfSorrowItem.java:220. A code comment explains the map exists because technique callbacks bypass use(). Ki drain is charged at SickleOfSorrowItem.java:228.
- Burst damage 500 + Spirit * 2 (SickleOfSorrowItem.java:235). Note this is the only ability in the set that scales with Spirit; everything else uses Strength.
- DimensionalRiftEntity (entity/DimensionalRiftEntity.java): MAX_LIFETIME 60 ticks. Radius expands 3 to 15 over the first 40 ticks. IS_COLLAPSING is set at tick 50. The burst hits each target once (with a 1 percent heal to the owner per target). Tick damage is 35 every 10 ticks to targets in range. Gravity pull is max(0.3, 1.4 * (1 - distance / 15)) toward the center plus 0.15 upward. Melting amplifier scales with closeness up to amplifier 9, with duration 30 + amplifier * 3 seconds, so up to 57 seconds. Sorrow Rift 200 ticks is refreshed on targets in range.
- Particles: 4 SPARK plus 2 VOID_SMOKE every 8 ticks, and at tick 50 (MAX_LIFETIME - 10) a collapse burst of 14 VOID_SMOKE and 10 SPARK (DimensionalRiftEntity.java:93 to 104).

Renderers:
- SorrowSlashRenderer: 4 layered crescents (outer silhouette, magenta crest, bright edge, inner core) with an intro ease and a smooth fade over the entity's life. Consistent with the wave's 30 tick life.
- DimensionalRiftRenderer: ground accretion rings, 3 crossed vertical tear planes up to 8 blocks high at 60 degree offsets, a singularity core drawn as a dark octahedron with a bright horizon ring and a white heart, and 18 orbiting crystalline spikes. The spikes spiral inward over the rift's life (orbit radius from 0.90 down to 0.32 of the rift radius) and each bobs on its own phase. Fade in over 6 ticks, and a separate collapse fade when IS_COLLAPSING is set.

### Gap analysis against the 5 points

Point 1: PARTIAL. The rift renderer layers rings, tears, spikes, and a core, and the slash is a proper 4 layer crescent. Particles are the sparsest in the set relative to size: 6 particles every 8 ticks across a disc that reaches radius 15 is about 15 per second over roughly 700 square blocks. The core has no particle density at all between the 8 tick emissions.

Point 2: PARTIAL, the closest in the set. The 18 orbiting spikes do orbit and do spiral inward, which is real orbital motion. But they are 18 identical pyramids of one size (height 1.4, thickness 0.22) on a single ring at a single height band (bob range about 0.06 to 0.22). The standard asks for hundreds of varied pieces tumbling on helical paths; this is one neat ring of matching spikes that never tumbles and never falls.

Point 3: PARTIAL. The gravity pull is real and distance scaled (up to 1.4 velocity at the center), targets are damaged, melted, and held by refreshed Sorrow Rift. But nothing is ever flung out: when the rift collapses at tick 60, pulled targets simply stop being pulled wherever they are. No blocks are affected, and there is no ground scar under a 15 radius rift.

Point 4: FAIL. No atmosphere calls. A black hole style singularity with an 8 block tear does not darken the sky, dim light, or add haze.

Point 5: PARTIAL, the best structured lifecycle in the set after the Hollow dash kit: 40 ticks of expansion, collapse flagged at 50, a particle inrush burst at 50, and a renderer collapse fade. The problem is scale: the whole life is 60 ticks, 3 seconds, for the weapon's signature right click, against 300 tick (15 second) channels on Dabura and Curse Blade. It builds and dies before it can peak.

### Numbers holding the spectacle back

- Total rift life 60 ticks. The expansion alone takes 40, leaving 10 ticks at full size before collapse starts. There is no sustained peak phase at all.
- Tick damage 35 every 10 ticks (3.5 per tick) against a 500 + Spirit * 2 burst: standing in the rift for its whole life costs a target about 210 damage in ticks, less than half the entry burst, so the rift reads as a one hit effect with a 3 second tail.
- Melting at amplifier up to 9 with duration up to 57 seconds (30 + 9 * 3) is wildly out of family with the rest of the set, where the longest effect is 300 ticks (15 seconds). The rift's visual is gone in 3 seconds but its debuff outlasts every other effect in the audit by nearly 4 times.
- Burst scaling uses Spirit * 2 while the same weapon's melee and air swing use Strength * 3, so building for the weapon's own melee stat does nothing for its right click.

### Bugs, dead code, mismatches

- The melee impact burst spawns a SorrowSlashEntity with damage 0.0f, and SorrowSlashEntity.tick skips all of its hit and effect logic when damage is 0 or less (the guard is commented in the entity). The melee therefore spawns an entity whose only job is to be rendered; that works, but the damage 0 convention is invisible to anyone reading the item alone.
- The dual cooldown (item cooldown at SickleOfSorrowItem.java:204 plus the static map at :210) can disagree: the map is keyed by player UUID and pruned lazily only when the ability is attempted, and any path that sets one gate without the other leaves the weapon either double locked or unlocked early.
- RIFT_COOLDOWNS is a static map on the item class holding server state across players; it survives in memory for the life of the server process and is only pruned on ability use, so entries for players who never use the ability again linger.

---

## Set-wide summary for the coordinating pass

- Strongest today: Hollow's Edge dash kit (lifecycle and mob forces), Sickle's rift structure (build, collapse, orbiting spikes), Curse Blade's telegraph and chain visuals.
- Weakest today: atmosphere across the board (zero weather, time, fog, or light control in five weapons, two of which are literally storms), particle density (under 30 per second everywhere), debris (8 to 18 static mesh pieces maximum, no tumbling, no helical paths, no rain down), and block interaction (one FIRE block in the entire set).
- Structural debt that will fight any quality pass: three renderer families are duplicated in near identical pairs (DarknessChargeRenderer and HollowChargeRenderer, DarknessDomainRenderer and AbyssalDomainRenderer including their entities, DarknessShatterRenderer and CurseGroundShatterRenderer), so shared improvements must be applied two or three times or the copies consolidated first.
- Dead weight to resolve in Phase 2: SkyCracksEntity and SkyCracksRenderer (spawned, invisible, functionless), DarknessBladeEntity's 400.0f default damage, VoidSlashEntity's 600.0f default damage, DimensionalSlashRenderer's unreachable fade out window.
