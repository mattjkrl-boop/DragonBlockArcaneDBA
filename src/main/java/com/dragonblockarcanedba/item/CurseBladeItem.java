package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.CurseChainEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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

import java.util.Comparator;
import java.util.List;

/**
 * Curse Blade — Dark Curse & Battlefield Denial Weapon.
 * 
 * LEFT: Curse Chain (Continuous Projected Seeking Chains)
 * - Hold left click to continuously stream spectral cursed chains.
 * - Chains seek nearby enemies and apply Movement Curse (1 to 10 stacks).
 * - Physical/destructible chains (~60 HP); enemies can attack them to remove stacks.
 * - Stacks last 5s, hits reset countdown. Max 10 chains per target.
 * - Tweak A: Chains jump to nearby alternative enemies when target is maxed.
 * - Tweak B: 10 stacks completely prevents teleportation, dashing, and abilities.
 * - Tweak C: Heavily cursed enemies are pulled toward user; chains wrap body.
 * 
 * RIGHT: Abyssal Eclipse (Supernatural Corrupted Storm)
 * - Channel right click to expand a localized storm domain (10 to 30 blocks).
 * - Swirling wind tosses enemies and has a chance of disarming them (drop held item).
 * - Telegraphed dark red lightning strikes cursed targets (Tweak A).
 * - Applies Storm of Darkness effect (Darkness, Nausea, progressive slow).
 * - Tweak B: Localized corrupt weather zone.
 * - Tweak C: At max charge (15s+), triggers full root on heavily cursed enemies.
 */
public class CurseBladeItem extends Item {
    public static final int MAX_ECLIPSE_CHANNEL_TICKS = 300; // 15 seconds

    public CurseBladeItem(Properties properties) {
        super(properties.attributes(
            ItemAttributeModifiers.builder()
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
                )
                .build()
        ));
    }

    // --- LEFT CLICK: Curse Chain Streaming ---

    public static void streamCurseChain(Player player, ItemStack stack) {
        if (!player.level().isClientSide() && player.level() instanceof ServerLevel serverLevel) {
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
                    SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.9f, 0.7f + serverLevel.getRandom().nextFloat() * 0.4f);
            }
        }
    }

    // --- RIGHT CLICK: Abyssal Eclipse (Supernatural Corrupted Storm) ---

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
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
            ServerLevel serverLevel = (ServerLevel) level;
            int heldTicks = getUseDuration(stack, living) - remainingTicks;
            float stormRatio = Math.min(1.0f, heldTicks / (float) MAX_ECLIPSE_CHANNEL_TICKS);
            double domainRadius = 10.0 + (stormRatio * 20.0); // 10 to 30 blocks radius
            // 1. Tweak B: Localized supernatural storm atmosphere (Handled by ClientLevelWeatherMixin)

            // Dense swirling vortex wind particles around the domain
            for (int i = 0; i < (int) (8 + stormRatio * 16); i++) {
                double angle = serverLevel.getRandom().nextDouble() * Math.PI * 2;
                double r = serverLevel.getRandom().nextDouble() * domainRadius;
                double px = player.getX() + Math.cos(angle) * r;
                double pz = player.getZ() + Math.sin(angle) * r;
                double py = player.getY() + serverLevel.getRandom().nextDouble() * 6.0;

                // Swirling vortex wind & dark ash
                serverLevel.sendParticles(
                    ParticleTypes.SMOKE,
                    px, py, pz,
                    1, -Math.sin(angle) * 0.4, 0.05, Math.cos(angle) * 0.4, 0.05
                );
                serverLevel.sendParticles(
                    new DustParticleOptions(0x2E0854, 1.5F), // Cursed purple
                    px, py, pz,
                    1, 0.0, 0.1, 0.0, 0.02
                );
            }

            // 2. Wind Force & Disarm Mechanics on enemies inside domain
            AABB domainBox = player.getBoundingBox().inflate(domainRadius, 8.0, domainRadius);
            List<LivingEntity> enemiesInStorm = serverLevel.getEntitiesOfClass(
                LivingEntity.class, domainBox,
                e -> e.isAlive() && e != player && player.distanceTo(e) <= domainRadius
            );

            for (LivingEntity enemy : enemiesInStorm) {
                // Apply Storm of Darkness custom effect
                enemy.addEffect(new MobEffectInstance(DbaEffects.STORM_OF_DARKNESS_HOLDER, 60, (int) (stormRatio * 3), false, true), player);
                enemy.addEffect(new MobEffectInstance(DbaEffects.CINEMATIC_TRACKING_HOLDER, 60, 0, false, false, false), player);

                // Wind toss: Aggressive wind push
                if (heldTicks % 5 == 0 && serverLevel.getRandom().nextFloat() < 0.6f) {
                    double wx = (serverLevel.getRandom().nextDouble() - 0.5) * 1.5;
                    double wz = (serverLevel.getRandom().nextDouble() - 0.5) * 1.5;
                    enemy.setDeltaMovement(enemy.getDeltaMovement().add(wx, 0.6, wz));
                    enemy.hurtMarked = true;
                }

                // Disarm Chance: 5% chance per 20 ticks to knock held item from hand
                if (heldTicks % 20 == 0 && serverLevel.getRandom().nextFloat() < 0.05f) {
                    ItemStack held = enemy.getMainHandItem();
                    if (!held.isEmpty() && !(held.getItem() instanceof CurseBladeItem)) {
                        ItemStack dropped = held.copy();
                        enemy.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                        ItemEntity itemEntity = new ItemEntity(serverLevel, enemy.getX(), enemy.getY() + 0.5, enemy.getZ(), dropped);
                        itemEntity.setPickUpDelay(40);
                        serverLevel.addFreshEntity(itemEntity);

                        serverLevel.playSound(null, enemy.getX(), enemy.getY(), enemy.getZ(),
                            SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
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

                    // Telegraph warning right before strike
                    if (heldTicks % 40 >= 0 && heldTicks % 40 < 20) {
                        if (heldTicks % 5 == 0) {
                            // Red warning circle on ground
                            for (int i = 0; i < 20; i++) {
                                double angle = (i / 20.0) * Math.PI * 2;
                                double r = 1.5;
                                serverLevel.sendParticles(new DustParticleOptions(0xFF0000, 1.2F),
                                    targetPos.x + Math.cos(angle) * r, targetPos.y + 0.1, targetPos.z + Math.sin(angle) * r,
                                    1, 0, 0, 0, 0);
                            }
                        }
                    }

                    // Actual Strike
                    if (heldTicks % 40 == 20) {
                        com.dragonblockarcanedba.entity.CurseLightningEntity lightning = new com.dragonblockarcanedba.entity.CurseLightningEntity(
                            com.dragonblockarcanedba.entity.DbaEntities.CURSE_LIGHTNING, serverLevel
                        );
                        lightning.setPos(targetPos.x, targetPos.y, targetPos.z);
                        if (serverLevel.getRandom().nextFloat() < 0.15f) {
                            lightning.setRare(true);
                        }
                        serverLevel.addFreshEntity(lightning);

                        // Magic thunder damage
                        lightningTarget.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(player), 450.0f);
                        lightningTarget.igniteForSeconds(5);
                        serverLevel.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                            SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 3.0f, 0.7f);
                        serverLevel.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                            SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 2.0f, 0.6f);
                                
                        // Fire on ground
                        BlockPos firePos = BlockPos.containing(targetPos.x, targetPos.y, targetPos.z);
                        if (serverLevel.getBlockState(firePos).isAir() && serverLevel.getBlockState(firePos.below()).isSolidRender()) {
                            serverLevel.setBlockAndUpdate(firePos, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
                        }
                    }
                }
            }

            // Environmental ambient lightning (Dramatic, non-spammy pacing ~2-3 per second)
            if (serverLevel.getRandom().nextFloat() < 0.12f) {
                double rx = player.getX() + (serverLevel.getRandom().nextDouble() - 0.5) * 60;
                double rz = player.getZ() + (serverLevel.getRandom().nextDouble() - 0.5) * 60;
                double ry = serverLevel.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int)rx, (int)rz);
                
                com.dragonblockarcanedba.entity.CurseLightningEntity ambient = new com.dragonblockarcanedba.entity.CurseLightningEntity(
                    com.dragonblockarcanedba.entity.DbaEntities.CURSE_LIGHTNING, serverLevel
                );
                ambient.setPos(rx, ry, rz);
                if (serverLevel.getRandom().nextFloat() < 0.20f) {
                    ambient.setRare(true);
                }
                serverLevel.addFreshEntity(ambient);
                
                // Thunder sound
                serverLevel.playSound(null, rx, ry, rz,
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.2f, 0.6f + serverLevel.getRandom().nextFloat() * 0.4f);
                serverLevel.playSound(null, rx, ry, rz,
                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 0.9f, 0.5f + serverLevel.getRandom().nextFloat() * 0.2f);
                
                // Damage and fire
                AABB strikeBox = new AABB(rx - 3, ry - 3, rz - 3, rx + 3, ry + 3, rz + 3);
                for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, strikeBox, e -> e != player)) {
                    victim.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(player), 15.0f);
                    victim.igniteForSeconds(5);
                }
                
                // Set ground on fire
                BlockPos firePos = BlockPos.containing(rx, ry, rz);
                if (serverLevel.getBlockState(firePos).isAir() && serverLevel.getBlockState(firePos.below()).isSolidRender()) {
                    serverLevel.setBlockAndUpdate(firePos, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
                }
            }

            // Custom sky cracks
            if (heldTicks % 80 == 0) {
                com.dragonblockarcanedba.entity.SkyCracksEntity cracks = new com.dragonblockarcanedba.entity.SkyCracksEntity(
                    com.dragonblockarcanedba.entity.DbaEntities.SKY_CRACKS, serverLevel
                );
                cracks.setPos(player.getX(), 280, player.getZ()); // Render high in the sky
                serverLevel.addFreshEntity(cracks);
            }

            // 4. Tweak C: At maximum charge (15s = 300 ticks) and beyond, full root on heavily cursed enemies
            if (heldTicks >= MAX_ECLIPSE_CHANNEL_TICKS && heldTicks % 20 == 0) {
                for (LivingEntity enemy : enemiesInStorm) {
                    MobEffectInstance curse = enemy.getEffect(DbaEffects.MOVEMENT_CURSE_HOLDER);
                    if (curse != null && curse.getAmplifier() >= 4) { // 5+ stacks
                        // Violent Ground Slam (Soul Hook Refinement)
                        enemy.setDeltaMovement(0, -2.5, 0);
                        enemy.hurtMarked = true;
                        
                        // Magic impact damage
                        enemy.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(player), 150.0f);

                        // 3 seconds complete root (amp 9)
                        enemy.addEffect(new MobEffectInstance(DbaEffects.MOVEMENT_CURSE_HOLDER, 60, 9, false, true), player);
                        enemy.addEffect(new MobEffectInstance(DbaEffects.CINEMATIC_TRACKING_HOLDER, 60, 0, false, false, false), player);
                        serverLevel.sendParticles(
                            ParticleTypes.LARGE_SMOKE,
                            enemy.getX(), enemy.getY() + 0.5, enemy.getZ(),
                            10, 0.3, 0.5, 0.3, 0.05
                        );
                    }
                }
            }

            // Ambient storm sound
            if (heldTicks % 40 == 0) {
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 0.8f, 0.5f);
            }
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (!level.isClientSide() && living instanceof ServerPlayer player) {
            int heldTicks = getUseDuration(stack, living) - timeLeft;
            float stormRatio = Math.min(1.0f, heldTicks / (float) MAX_ECLIPSE_CHANNEL_TICKS);
            player.getCooldowns().addCooldown(stack, 40 + (int) (stormRatio * 60));
        }
        return true;
    }
}
