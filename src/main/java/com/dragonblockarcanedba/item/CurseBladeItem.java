package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.CurseChainEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.dragonblockarcanedba.util.WeaponDrainHelper;

import java.util.Comparator;
import java.util.List;

/**
 * Curse Blade: Dark Curse & Battlefield Denial Weapon.
 * 
 * Ki Drain: 30% per minute (smooth).
 */
public class CurseBladeItem extends Item implements com.dragonblockarcanedba.item.geo.DbaGeoWeapon {
    public static final int MAX_ECLIPSE_CHANNEL_TICKS = 300; // 15 seconds

    // Tempest bolt colors: cursed crimson, violet for rare and heavily cursed strikes.
    public static final int BOLT_CRIMSON = 0xFFF20D33;
    public static final int BOLT_VIOLET = 0xFF9B30FF;

    public CurseBladeItem(Properties properties) {
        super(properties.attributes(createModifiers()));
    }

    private static ItemAttributeModifiers createModifiers() {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                    BASE_ATTACK_DAMAGE_ID,
                    749.0, // 1 + 749 = 750 base damage
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            )
            .add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                    BASE_ATTACK_SPEED_ID,
                    -2.0, // Fast fluid dark swings
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            );

        // MC 26.2 Stealth: Shadow Stalker Concealment (hides nameplates from afar)
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.NAME_PLATE_DIST_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("curse_blade_nameplate_stealth"), -64.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
        );
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.MINI_NAME_PLATE_DIST_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("curse_blade_mini_nameplate_stealth"), -10.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
        );

        return builder.build();
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // The curse chain and the domain already pay. A vanilla connect used to
        // spend ki again and add no curse of its own.
    }

    // --- LEFT CLICK: Curse Chain Streaming ---

    public static void streamCurseChain(Player player, ItemStack stack) {
        if (!player.level().isClientSide() && player.level() instanceof ServerLevel serverLevel) {
            // Chains were nearly free and cooldown-less, so 10 curse stacks could be
            // click-spammed almost instantly. A 5 tick cadence keeps chains fast but paced.
            if (player.getCooldowns().isOnCooldown(stack)) {
                return;
            }
            // Drain Ki: 30% per minute (~4 ticks cadence = 0.1% Max Ki)
            if (!WeaponDrainHelper.drainKiDiscrete(player, 30.0, 4)) {
                return;
            }
            player.getCooldowns().addCooldown(stack, 5);

            Vec3 eyePos = player.getEyePosition();
            Vec3 look = player.getLookAngle();

            // Find best target near crosshair or within 24 blocks
            AABB searchBox = player.getBoundingBox().inflate(24.0);
            List<LivingEntity> potentialTargets = serverLevel.getEntitiesOfClass(
                LivingEntity.class, searchBox,
                e -> e.isAlive() && e != player && !e.isSpectator()
            );

            LivingEntity target = null;
            double bestScore = Double.MAX_VALUE;

            for (LivingEntity e : potentialTargets) {
                Vec3 toTarget = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eyePos);
                double distance = toTarget.length();
                double dot = look.dot(toTarget.normalize());

                if (dot > 0.3) { // In front of player
                    double score = distance * (2.0 - dot);
                    if (score < bestScore) {
                        bestScore = score;
                        target = e;
                    }
                }
            }

            // If no one in crosshair cone, pick closest enemy
            if (target == null && !potentialTargets.isEmpty()) {
                potentialTargets.sort(Comparator.comparingDouble(player::distanceToSqr));
                target = potentialTargets.get(0);
            }

            if (target != null) {
                // Determine orbit index based on existing chains on target
                List<CurseChainEntity> existing = serverLevel.getEntitiesOfClass(
                    CurseChainEntity.class, target.getBoundingBox().inflate(3.0),
                    c -> c.isAttached()
                );
                int orbitIndex = existing.size() % 10;

                CurseChainEntity chain = new CurseChainEntity(serverLevel, player, target, orbitIndex);
                chain.setDeltaMovement(look.scale(1.5));
                serverLevel.addFreshEntity(chain);

                // Sound
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    com.dragonblockarcanedba.sound.DbaSounds.CHAIN_RATTLE, SoundSource.PLAYERS, 0.9f, 0.7f + serverLevel.getRandom().nextFloat() * 0.4f);
            }
        }
    }

    // --- RIGHT CLICK: Cursed Tempest (Supernatural Corrupted Storm) ---

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!WeaponDrainHelper.hasKi(player)) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingTicks) {
        if (!level.isClientSide() && living instanceof ServerPlayer player) {
            // Drain Ki: 30% per minute
            if (!WeaponDrainHelper.drainKiPerTick(player, 30.0)) {
                player.stopUsingItem();
                return;
            }

            ServerLevel serverLevel = (ServerLevel) level;
            int heldTicks = getUseDuration(stack, living) - remainingTicks;
            float stormRatio = Math.min(1.0f, heldTicks / (float) MAX_ECLIPSE_CHANNEL_TICKS);
            double domainRadius = 10.0 + (stormRatio * 20.0); // 10 to 30 blocks radius

            // 1. Manage Physical 3D Abyssal Domain Void Storm Entity (Atmosphere & Void Dome)
            List<com.dragonblockarcanedba.entity.AbyssalDomainEntity> existingDomains = serverLevel.getEntitiesOfClass(
                com.dragonblockarcanedba.entity.AbyssalDomainEntity.class,
                player.getBoundingBox().inflate(6.0),
                d -> d.getCasterId() == player.getId() && d.isAlive()
            );
            com.dragonblockarcanedba.entity.AbyssalDomainEntity domain;
            if (existingDomains.isEmpty()) {
                domain = new com.dragonblockarcanedba.entity.AbyssalDomainEntity(serverLevel, player, player.position(), (float) domainRadius);
                serverLevel.addFreshEntity(domain);
            } else {
                domain = existingDomains.get(0);
                domain.setRadius((float) domainRadius);
                domain.refreshLifetime();
            }

            // 1b. Storm atmosphere (point 4): the tempest registers its
            // tracked storm volume with AtmosphereServer for its whole life.
            // The storm is fully custom (client rain, gloom grade, and the
            // bank's dark thunder); world weather is never touched, and the
            // gloom lerps out when the storm ends, so the dissipation needs
            // no hard cut here.
            if (heldTicks % 20 == 0) {
                com.dragonblockarcanedba.util.AtmosphereServer.begin(serverLevel, player.position(),
                    (float) domainRadius + 24.0f, 0.45f + 0.45f * stormRatio,
                    com.dragonblockarcanedba.util.AtmosphereServer.Kind.THUNDER, 70);
            }

            // Cast beat (point 3): dust bursts off the ground as the storm opens.
            if (heldTicks <= 1) {
                com.dragonblockarcanedba.util.LayeredParticles.emitGroundBurst(serverLevel, player.position(), 3.0f, 0xFFE61230);
            }

            // Peak beat (point 2): the moment the tempest reaches full charge
            // it tears a full debris field out of the ground it stands on.
            if (heldTicks == MAX_ECLIPSE_CHANNEL_TICKS) {
                com.dragonblockarcanedba.util.DebrisSpawner.spawnField(serverLevel, player, player.position(),
                    60, domainRadius * 0.7, 10.0, 80);
                com.dragonblockarcanedba.util.LayeredParticles.emitGroundBurst(serverLevel, player.position(), (float) domainRadius * 0.6f, 0xFFE61230);
            }

            // 2. Wind Force & Disarm Mechanics on enemies inside domain
            AABB domainBox = player.getBoundingBox().inflate(domainRadius, 8.0, domainRadius);
            List<LivingEntity> enemiesInStorm = serverLevel.getEntitiesOfClass(
                LivingEntity.class, domainBox,
                e -> e.isAlive() && e != player && player.distanceTo(e) <= domainRadius
            );

            // 2b. Storm wind (point 3): a real vortex pattern instead of the
            // old random jitter pushes. Everything inside spirals inward,
            // swirls around the caster, and lifts near the core, with the
            // strength carried by the channel build up envelope.
            float stormEnv = com.dragonblockarcanedba.util.FxEnvelope.smoothstep(Math.min(1.0f, heldTicks / 60.0f));
            double domeHeight = Math.min(24.0, domainRadius * 1.30);
            com.dragonblockarcanedba.util.VortexPhysics.applyVortex(serverLevel, player.position(),
                domainRadius, domeHeight, player, 0.055, 1.5, 0.06, stormEnv);

            for (LivingEntity enemy : enemiesInStorm) {
                // Apply Storm of Darkness custom effect
                enemy.addEffect(new MobEffectInstance(DbaEffects.STORM_OF_DARKNESS_HOLDER, 60, (int) (stormRatio * 3), false, true), player);
                enemy.addEffect(new MobEffectInstance(DbaEffects.CINEMATIC_TRACKING_HOLDER, 60, 0, false, false, false), player);

                // Disarm Chance: 10% chance per 20 ticks to knock held item from hand.
                // Was 5% with a 40 tick pickup delay, so the storm stripped a
                // weapon about once per 20 seconds per enemy and the enemy
                // usually picked it straight back up; both numbers now read.
                if (heldTicks % 20 == 0 && serverLevel.getRandom().nextFloat() < 0.10f) {
                    ItemStack held = enemy.getMainHandItem();
                    if (!held.isEmpty() && !(held.getItem() instanceof CurseBladeItem)) {
                        ItemStack dropped = held.copy();
                        enemy.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                        ItemEntity itemEntity = new ItemEntity(serverLevel, enemy.getX(), enemy.getY() + 0.5, enemy.getZ(), dropped);
                        itemEntity.setPickUpDelay(100);
                        serverLevel.addFreshEntity(itemEntity);

                        serverLevel.playSound(null, enemy.getX(), enemy.getY(), enemy.getZ(),
                            com.dragonblockarcanedba.sound.DbaSounds.ITEM_SHATTER, SoundSource.PLAYERS, 1.0f, 1.0f);
                    }
                }
            }

            // 3. Telegraphed Dark Red Lightning (Tweak A: Targets cursed enemies)
            if (!enemiesInStorm.isEmpty()) {
                LivingEntity lightningTarget = enemiesInStorm.stream()
                    .max(Comparator.comparingInt(e -> {
                        MobEffectInstance eff = e.getEffect(DbaEffects.MOVEMENT_CURSE_HOLDER);
                        return eff != null ? eff.getAmplifier() + 1 : 0;
                    }))
                    .orElse(enemiesInStorm.get(0));

                if (lightningTarget != null) {
                    Vec3 targetPos = lightningTarget.position();

                    // Telegraph warning right before strike (Spawns 3D Runic Decal 20 ticks before detonation)
                    if (heldTicks % 40 == 0) {
                        com.dragonblockarcanedba.entity.CurseTelegraphEntity telegraph = new com.dragonblockarcanedba.entity.CurseTelegraphEntity(
                            serverLevel, player, targetPos.add(0, 0.05, 0), 1.8f, 20
                        );
                        serverLevel.addFreshEntity(telegraph);
                    }

                    // Actual Strike: a real BoltEntity strike now, branched
                    // and seeded, reaching from the ground to the top of the
                    // dome instead of a fixed 16 block picture.
                    if (heldTicks % 40 == 20) {
                        boolean rareBolt = serverLevel.getRandom().nextFloat() < 0.15f;
                        float boltLength = (float) (Math.min(24.0, domainRadius * 1.30) + 6.0);
                        com.dragonblockarcanedba.entity.BoltEntity bolt = new com.dragonblockarcanedba.entity.BoltEntity(
                            serverLevel, new Vec3(targetPos.x, targetPos.y, targetPos.z),
                            serverLevel.getRandom().nextInt(), rareBolt ? BOLT_VIOLET : BOLT_CRIMSON, boltLength, 8
                        );
                        serverLevel.addFreshEntity(bolt);

                        // Magic thunder damage (scales with Strength like its sibling weapons)
                        float strikeDamage = 450.0f + (float) (((PlayerStatsAccessor) player).dba$getStrength() * 1.5f);
                        lightningTarget.hurtServer(serverLevel, com.dragonblockarcanedba.util.DbaDamage.curse(player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (strikeDamage)));
                        com.dragonblockarcanedba.fx.DbaImpactFx.medium(serverLevel, targetPos.x, targetPos.y, targetPos.z);
                        lightningTarget.igniteForSeconds(5);
                        serverLevel.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                            com.dragonblockarcanedba.sound.DbaSounds.THUNDER_DARK, SoundSource.WEATHER, 3.0f, 0.7f);
                        serverLevel.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                            com.dragonblockarcanedba.sound.DbaSounds.LIGHTNING_ZAP, SoundSource.WEATHER, 3.0f, 0.7f);
                        serverLevel.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                            com.dragonblockarcanedba.sound.DbaSounds.ZAP_DARK, SoundSource.WEATHER, 2.0f, 0.6f);
                                
                        // Fire on ground
                        BlockPos firePos = BlockPos.containing(targetPos.x, targetPos.y, targetPos.z);
                        if (serverLevel.getBlockState(firePos).isAir() && serverLevel.getBlockState(firePos.below()).isSolidRender()) {
                            serverLevel.setBlockAndUpdate(firePos, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
                        }

                        // Scorched ground (point 3): the strike blackens the
                        // earth around the hit for a while, then GroundScar
                        // restores it. Torn up blocks orbit the hit briefly.
                        scorchGround(serverLevel, targetPos.x, targetPos.z, 1, 0.7f, 160);
                        com.dragonblockarcanedba.util.DebrisSpawner.spawnField(serverLevel, player,
                            new Vec3(targetPos.x, targetPos.y, targetPos.z), 12, 2.5, 3.0, 25);
                        com.dragonblockarcanedba.util.LayeredParticles.emitGroundBurst(serverLevel,
                            new Vec3(targetPos.x, targetPos.y, targetPos.z), 1.8f, 0xFFE61230);
                    }
                }
            }

            // Environmental ambient lightning. These bolts used to deal 15
            // damage against the telegraphed strike's 450 plus scaling, a
            // 30 to 1 gap that taught players to ignore the storm's main
            // visual. They now carry real weight: 120 + Strength * 0.35 in a
            // 3.5 block box, and just under half of them land near an enemy
            // inside the storm so the tempest reads as hunting, not random.
            if (serverLevel.getRandom().nextFloat() < 0.12f) {
                double rx, rz;
                if (!enemiesInStorm.isEmpty() && serverLevel.getRandom().nextFloat() < 0.45f) {
                    LivingEntity near = enemiesInStorm.get(serverLevel.getRandom().nextInt(enemiesInStorm.size()));
                    rx = near.getX() + (serverLevel.getRandom().nextDouble() - 0.5) * 4.0;
                    rz = near.getZ() + (serverLevel.getRandom().nextDouble() - 0.5) * 4.0;
                } else {
                    rx = player.getX() + (serverLevel.getRandom().nextDouble() - 0.5) * 2.0 * domainRadius;
                    rz = player.getZ() + (serverLevel.getRandom().nextDouble() - 0.5) * 2.0 * domainRadius;
                }
                double ry = serverLevel.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) rx, (int) rz);

                boolean rareBolt = serverLevel.getRandom().nextFloat() < 0.20f;
                float boltLength = (float) (Math.min(24.0, domainRadius * 1.30) + 6.0);
                com.dragonblockarcanedba.entity.BoltEntity ambientBolt = new com.dragonblockarcanedba.entity.BoltEntity(
                    serverLevel, new Vec3(rx, ry, rz),
                    serverLevel.getRandom().nextInt(), rareBolt ? BOLT_VIOLET : BOLT_CRIMSON, boltLength, 8
                );
                serverLevel.addFreshEntity(ambientBolt);

                // Thunder sound
                serverLevel.playSound(null, rx, ry, rz,
                    com.dragonblockarcanedba.sound.DbaSounds.THUNDER_DARK, SoundSource.WEATHER, 1.2f, 0.6f + serverLevel.getRandom().nextFloat() * 0.4f);
                serverLevel.playSound(null, rx, ry, rz,
                    com.dragonblockarcanedba.sound.DbaSounds.ZAP_DARK, SoundSource.WEATHER, 0.9f, 0.5f + serverLevel.getRandom().nextFloat() * 0.2f);

                // Damage, scorch, and a dust burst where the bolt lands. Both
                // strike types now scorch the same way, so identical looking
                // bolts behave alike; loose block fire stays exclusive to the
                // telegraphed strike, which is aimed and warned in advance.
                float ambientDamage = 120.0f + (float) (((PlayerStatsAccessor) player).dba$getStrength() * 0.35f);
                AABB strikeBox = new AABB(rx - 3.5, ry - 3, rz - 3.5, rx + 3.5, ry + 3, rz + 3.5);
                for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, strikeBox, e -> e != player)) {
                    victim.hurtServer(serverLevel, com.dragonblockarcanedba.util.DbaDamage.curse(player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(ambientDamage));
                    victim.igniteForSeconds(3);
                }
                scorchGround(serverLevel, rx, rz, 0, 1.0f, 120);
                com.dragonblockarcanedba.util.LayeredParticles.emitGroundBurst(serverLevel, new Vec3(rx, ry, rz), 1.4f, 0xFFE61230);
            }

            // Custom sky cracks
            if (heldTicks % 80 == 0) {
                com.dragonblockarcanedba.entity.SkyCracksEntity cracks = new com.dragonblockarcanedba.entity.SkyCracksEntity(
                    com.dragonblockarcanedba.entity.DbaEntities.SKY_CRACKS, serverLevel
                );
                cracks.setPos(player.getX(), Math.min(280.0, player.getY() + 55.0), player.getZ()); // Above the storm, still reachable in low ceiling dimensions
                cracks.setOwner(player);
                serverLevel.addFreshEntity(cracks);
            }

            // 4. Tweak C: At maximum charge (15s = 300 ticks) and beyond, full root on heavily cursed enemies
            if (heldTicks >= MAX_ECLIPSE_CHANNEL_TICKS && heldTicks % 20 == 0) {
                boolean slamImpactPlayed = false;
                float slamDamage = 220.0f + (float) (((PlayerStatsAccessor) player).dba$getStrength() * 1.0f);
                for (LivingEntity enemy : enemiesInStorm) {
                    MobEffectInstance curse = enemy.getEffect(DbaEffects.MOVEMENT_CURSE_HOLDER);
                    if (curse != null && curse.getAmplifier() >= 4) { // 5+ stacks
                        // Violent Ground Slam (Soul Hook Refinement)
                        enemy.setDeltaMovement(0, -2.5, 0);
                        com.dragonblockarcanedba.util.DbaCompat.markHurt(enemy);

                        // Magic impact damage (scales with Strength)
                        enemy.hurtServer(serverLevel, com.dragonblockarcanedba.util.DbaDamage.curse(player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (slamDamage)));
                        if (!slamImpactPlayed) {
                            slamImpactPlayed = true;
                            com.dragonblockarcanedba.fx.DbaImpactFx.heavy(serverLevel, enemy.getX(), enemy.getY(), enemy.getZ());
                        }

                        // 3 seconds complete root (amp 9)
                        enemy.addEffect(new MobEffectInstance(DbaEffects.MOVEMENT_CURSE_HOLDER, 60, 9, false, true), player);
                        enemy.addEffect(new MobEffectInstance(DbaEffects.CINEMATIC_TRACKING_HOLDER, 60, 0, false, false, false), player);

                        // Physical 3D Tectonic Fissure Trenches & Shattered Obsidian Slabs
                        com.dragonblockarcanedba.entity.CurseGroundShatterEntity shatter = new com.dragonblockarcanedba.entity.CurseGroundShatterEntity(
                            serverLevel, player, enemy.position().add(0, 0.05, 0), 3.5f
                        );
                        serverLevel.addFreshEntity(shatter);

                        // The slam tears the ground up for real now (points 2
                        // and 3): orbiting debris pieces, a scorched patch
                        // under the victim, and a dust burst at the impact.
                        com.dragonblockarcanedba.util.DebrisSpawner.spawnField(serverLevel, player, enemy.position(), 12, 3.0, 4.0, 30);
                        scorchGround(serverLevel, enemy.getX(), enemy.getZ(), 2, 0.5f, 200);
                        com.dragonblockarcanedba.util.LayeredParticles.emitGroundBurst(serverLevel, enemy.position(), 2.2f, 0xFFE61230);
                    }
                }
            }

            // Ambient storm sound
            if (heldTicks % 40 == 0) {
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    com.dragonblockarcanedba.sound.DbaSounds.THUNDER_DARK, SoundSource.WEATHER, 0.8f, 0.5f);
            }
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (!level.isClientSide() && living instanceof ServerPlayer player && level instanceof ServerLevel serverLevel) {
            int heldTicks = getUseDuration(stack, living) - timeLeft;
            float stormRatio = Math.min(1.0f, heldTicks / (float) MAX_ECLIPSE_CHANNEL_TICKS);
            // Cooldown raised: a storm class move should not restart the
            // moment its weather fades. Was 40 + ratio * 60 (40 to 100).
            player.getCooldowns().addCooldown(stack, 100 + (int) (stormRatio * 100));

            // Partial finisher (point 5): releasing early used to buy nothing
            // at all, the tempest just ended. From 2 seconds in, release now
            // detonates the Tempest Verdict, scaled by the charge gathered:
            // damage, curse stacks, fling, and debris all grow monotonically
            // with stormRatio, and a full charge is worth well over twice a
            // minimum release on the headline damage.
            if (heldTicks >= 40) {
                double domainRadius = 10.0 + (stormRatio * 20.0);
                float verdictDamage = 150.0f + 650.0f * stormRatio
                    + (float) (((PlayerStatsAccessor) player).dba$getStrength() * (0.8f + 1.7f * stormRatio));
                AABB verdictBox = player.getBoundingBox().inflate(domainRadius, 8.0, domainRadius);
                List<LivingEntity> caught = serverLevel.getEntitiesOfClass(
                    LivingEntity.class, verdictBox,
                    e -> e.isAlive() && e != player && player.distanceTo(e) <= domainRadius
                );
                int bolts = 0;
                for (LivingEntity enemy : caught) {
                    MobEffectInstance curse = enemy.getEffect(DbaEffects.MOVEMENT_CURSE_HOLDER);
                    enemy.hurtServer(serverLevel, com.dragonblockarcanedba.util.DbaDamage.curse(player),
                        com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(verdictDamage));
                    enemy.igniteForSeconds(4);
                    int amp = Math.min(9, (curse != null ? curse.getAmplifier() + 1 : 0) + 2);
                    enemy.addEffect(new MobEffectInstance(DbaEffects.MOVEMENT_CURSE_HOLDER, 60, amp, false, true), player);
                    com.dragonblockarcanedba.util.VortexPhysics.fling(enemy, player.position(), 0.6 + 1.2 * stormRatio, 0.5 + 0.5 * stormRatio);
                    if (bolts < 6) {
                        bolts++;
                        com.dragonblockarcanedba.entity.BoltEntity bolt = new com.dragonblockarcanedba.entity.BoltEntity(
                            serverLevel, enemy.position(), serverLevel.getRandom().nextInt(),
                            curse != null ? BOLT_VIOLET : BOLT_CRIMSON, 26.0f, 8);
                        serverLevel.addFreshEntity(bolt);
                    }
                }
                // The dying storm throws its debris field outward and leaves
                // a scorched patch where it stood (points 2, 3, and 5).
                com.dragonblockarcanedba.util.DebrisSpawner.spawnField(serverLevel, player, player.position(),
                    24 + (int) (36 * stormRatio), domainRadius * 0.8, 6.0 + 6.0 * stormRatio, 50);
                scorchGround(serverLevel, player.getX(), player.getZ(), Math.min(8, (int) (domainRadius * 0.5)), 0.45f, 240);
                com.dragonblockarcanedba.util.LayeredParticles.emitGroundBurst(serverLevel, player.position(), (float) domainRadius * 0.5f, 0xFFE61230);
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    com.dragonblockarcanedba.sound.DbaSounds.THUNDER_DARK, SoundSource.WEATHER, 3.0f, 0.6f);
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    com.dragonblockarcanedba.sound.DbaSounds.ZAP_DARK, SoundSource.WEATHER, 2.0f, 0.5f);
            }
        }
        return true;
    }

    /**
     * Scorch the real top block of each column around a point through
     * GroundScar, so the blackened ground sits on the terrain on any slope
     * and is restored on schedule. Air columns (cliff edges) are skipped.
     */
    private static void scorchGround(ServerLevel serverLevel, double cx, double cz, int radius, float chance, int restoreTicks) {
        int baseX = (int) Math.floor(cx);
        int baseZ = (int) Math.floor(cz);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                if (serverLevel.getRandom().nextFloat() > chance) continue;
                int topY = serverLevel.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, baseX + dx, baseZ + dz) - 1;
                BlockPos pos = new BlockPos(baseX + dx, topY, baseZ + dz);
                if (serverLevel.getBlockState(pos).isAir()) continue;
                com.dragonblockarcanedba.util.GroundScar.scar(serverLevel, pos,
                    net.minecraft.world.level.block.Blocks.BLACKSTONE.defaultBlockState(), restoreTicks);
            }
        }
    }
}
