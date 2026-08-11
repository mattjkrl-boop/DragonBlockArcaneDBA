package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Sickle of Sorrow — The Dimensional Reaper. Endgame legendary weapon.
 * 
 * Left-click: 750 + (Strength × 3) damage.
 *   - Applies Melting III for 15 seconds
 *   - Soul Rend: Steals 3% of damage dealt as healing (lifesteal)
 *   - Applies Darkness for 4 seconds (blinds target)
 * 
 * Right-click: "Dimensional Rift" — 15-block radius 360° AOE.
 *   - Distance-based Melting tiers up to Level 10 at point-blank
 *   - Instant burst of 500 + (Spirit × 2) magic damage to all targets
 *   - Slowness IV for 10 seconds
 *   - Gravity Well: Pulls all hit entities toward the player
 *   - Expanding purple/black void rings with rising dark particles
 *   - 3-second cooldown
 * 
 * Unbreakable legendary weapon. No durability.
 */
public class SickleOfSorrowItem extends Item {
    private static final double AOE_RADIUS = 15.0;

    public SickleOfSorrowItem(Properties properties) {
        super(properties.attributes(
            ItemAttributeModifiers.builder()
                .add(
                    Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                        BASE_ATTACK_DAMAGE_ID,
                        749.0, // +749 on top of base 1 = 750 total
                        AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
                .add(
                    Attributes.ATTACK_SPEED,
                    new AttributeModifier(
                        BASE_ATTACK_SPEED_ID,
                        -2.4, // Slightly faster than before (-2.8), still heavy
                        AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
                .build()
        ));
    }

    // --- Left Click: Massive damage + Melting III + Soul Rend lifesteal + Darkness ---
    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Apply Melting III (amplifier 2) for 15 seconds (300 ticks)
        target.addEffect(new MobEffectInstance(DbaEffects.MELTING_HOLDER, 300, 2, false, true), attacker);

        // Apply Darkness for 4 seconds (80 ticks) — blinds target
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, true), attacker);

        if (attacker instanceof ServerPlayer serverPlayer) {
            ServerLevel serverLevel = (ServerLevel) serverPlayer.level();

            // Stat-scaled bonus damage: Strength × 3
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) serverPlayer;
            float strengthBonus = (float) (accessor.dba$getStrength() * 3.0);
            if (strengthBonus > 0) {
                target.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(serverPlayer), strengthBonus);
            }

            // Soul Rend: Steal 3% of total damage dealt as healing
            float totalDamage = 750.0f + strengthBonus;
            float healAmount = totalDamage * 0.03f;
            serverPlayer.heal(healAmount);

            // Purple/dark impact particles
            serverLevel.sendParticles(
                new DustParticleOptions(0x660033, 2.5F),
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                20, 0.5, 0.5, 0.5, 0.1
            );
        }
    }

    // --- Right Click: Dimensional Rift AOE ---
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) level;
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

            // Calculate AOE burst damage: 500 + (Spirit × 2)
            float burstDamage = 500.0f + (float) (accessor.dba$getSpirit() * 2.0);

            // Scan 15-block radius for all living entities (excluding the user)
            AABB aoe = player.getBoundingBox().inflate(AOE_RADIUS);
            List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, aoe, e -> e != player && e.isAlive()
            );

            for (LivingEntity target : targets) {
                double distance = player.distanceTo(target);
                if (distance <= AOE_RADIUS) {
                    // Distance-based Melting level: closer = higher level
                    // 0-1.5 blocks: Level 10 (amp 9), scaling down to Level 1 (amp 0) at 15 blocks
                    int amplifier = Math.max(0, 9 - (int) Math.floor(distance * 9.0 / AOE_RADIUS));

                    // Duration scales with level: 30s base + 3s per level above 1
                    int durationSec = 30 + (amplifier * 3);
                    int durationTicks = durationSec * 20;

                    target.addEffect(new MobEffectInstance(
                        DbaEffects.MELTING_HOLDER, durationTicks, amplifier, false, true
                    ), player);

                    // Instant burst magic damage
                    target.hurtServer(serverLevel, serverLevel.damageSources().magic(), burstDamage);

                    // Slowness IV for 10 seconds (200 ticks)
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 3, false, true), player);

                    // Gravity Well: Pull target toward player
                    Vec3 pullDir = player.position().subtract(target.position()).normalize().scale(1.5);
                    target.setDeltaMovement(target.getDeltaMovement().add(pullDir.x, 0.3, pullDir.z));
                    target.hurtMarked = true; // Force velocity sync
                }
            }

            // --- Expanding purple/black void rings (3 rings at different radii) ---
            for (double ringRadius = 3.0; ringRadius <= AOE_RADIUS; ringRadius += 4.0) {
                for (int i = 0; i < 90; i++) {
                    double angle = Math.toRadians(i * 4);
                    double px = player.getX() + Math.cos(angle) * ringRadius;
                    double pz = player.getZ() + Math.sin(angle) * ringRadius;
                    serverLevel.sendParticles(
                        new DustParticleOptions(0x9900CC, 2.0F),
                        px, player.getY() + 0.5, pz,
                        1, 0.0, 0.4, 0.0, 0.0
                    );
                }
            }

            // Dark rising particles in the center
            for (int i = 0; i < 30; i++) {
                double offsetX = (serverLevel.getRandom().nextFloat() - 0.5) * 4.0;
                double offsetZ = (serverLevel.getRandom().nextFloat() - 0.5) * 4.0;
                serverLevel.sendParticles(
                    ParticleTypes.SMOKE,
                    player.getX() + offsetX, player.getY() + 0.2, player.getZ() + offsetZ,
                    3, 0.1, 0.8, 0.1, 0.05
                );
            }

            // Soul Rend heal from AOE: 1% of total burst damage per target hit
            float aoeHeal = burstDamage * 0.01f * targets.size();
            player.heal(aoeHeal);
        }

        // 3-second cooldown (60 ticks)
        player.getCooldowns().addCooldown(stack, 60);
        return InteractionResult.SUCCESS;
    }
}
