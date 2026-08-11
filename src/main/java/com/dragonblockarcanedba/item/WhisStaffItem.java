package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.util.TimeTracker;
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

import java.util.List;

/**
 * Whis Staff — The Temporal God Rod. Endgame legendary weapon.
 * 
 * Left-click: 350 + (Dexterity × 2) damage.
 *   - Time reversal for 10 seconds (200 ticks)
 *   - Temporal Shatter: 1.5% of target's max HP as bonus magic damage
 *   - Slowness III for 5 seconds (time distortion)
 * 
 * Right-click: "Temporal Rift" — 12-block radius AOE.
 *   - All entities: Temporal Freeze (Slowness 127 = frozen) for 3 seconds, then 15-second reversal
 *   - Instant 400 + (Spirit × 1.5) magic damage to all entities in range
 *   - Self-Buff: Speed III + Resistance II for 10 seconds
 *   - Shimmering white/silver temporal sphere particles
 *   - 5-second cooldown
 * 
 * Passive: Auto-Dodge — 50% chance to negate incoming damage while held in mainhand.
 *   (Implemented via LivingEntityMixin)
 * 
 * Unbreakable legendary weapon. No durability.
 */
public class WhisStaffItem extends Item {
    private static final double AOE_RADIUS = 12.0;

    public WhisStaffItem(Properties properties) {
        super(properties.attributes(
            ItemAttributeModifiers.builder()
                .add(
                    Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                        BASE_ATTACK_DAMAGE_ID,
                        349.0, // +349 on top of base 1 = 350 total
                        AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
                .add(
                    Attributes.ATTACK_SPEED,
                    new AttributeModifier(
                        BASE_ATTACK_SPEED_ID,
                        -1.5, // Fast — befitting Whis's speed (effective 2.5 attacks/sec)
                        AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
                .build()
        ));
    }

    // --- Left Click: Damage + Time Reversal + Temporal Shatter + Slowness ---
    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Time reversal for 10 seconds (200 ticks)
        if (target instanceof TimeTracker timeTracker) {
            timeTracker.dba$startReversing(200);
        }

        // Slowness III for 5 seconds (100 ticks) — time distortion
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 2, false, true), attacker);

        if (attacker instanceof ServerPlayer serverPlayer) {
            ServerLevel serverLevel = (ServerLevel) serverPlayer.level();

            // Stat-scaled bonus damage: Dexterity × 2
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) serverPlayer;
            float dexBonus = (float) (accessor.dba$getDexterity() * 2.0);
            if (dexBonus > 0) {
                target.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(serverPlayer), dexBonus);
            }

            // Temporal Shatter: 1.5% of target's max HP as bonus magic damage
            float temporalDamage = target.getMaxHealth() * 0.015f;
            target.hurtServer(serverLevel, serverLevel.damageSources().magic(), temporalDamage);

            // White/silver impact particles — temporal distortion
            serverLevel.sendParticles(
                new DustParticleOptions(0xE0E0FF, 2.0F),
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                15, 0.4, 0.4, 0.4, 0.1
            );
            serverLevel.sendParticles(
                ParticleTypes.END_ROD,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                5, 0.3, 0.3, 0.3, 0.05
            );
        }
    }

    // --- Right Click: Temporal Rift AOE ---
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) level;
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

            // Calculate AOE burst damage: 400 + (Spirit × 1.5)
            float burstDamage = 400.0f + (float) (accessor.dba$getSpirit() * 1.5);

            // 12-block radius AOE — reverse all living entities
            AABB aoe = player.getBoundingBox().inflate(AOE_RADIUS);
            List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, aoe, e -> e != player && e.isAlive()
            );

            for (LivingEntity target : targets) {
                if (player.distanceTo(target) <= AOE_RADIUS) {
                    // Instant burst magic damage
                    target.hurtServer(serverLevel, serverLevel.damageSources().magic(), burstDamage);

                    // Temporal Freeze: Slowness 127 (amplifier 126) for 3 seconds (60 ticks)
                    // This effectively freezes the entity in place
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 126, false, false), player);

                    // Time reversal for 15 seconds (300 ticks) — starts after freeze ends
                    if (target instanceof TimeTracker tracker) {
                        tracker.dba$startReversing(300);
                    }

                    // Per-target temporal distortion particles
                    serverLevel.sendParticles(
                        ParticleTypes.END_ROD,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                        8, 0.3, 0.5, 0.3, 0.05
                    );
                }
            }

            // --- Self-Buff: Speed III + Resistance II for 10 seconds (200 ticks) ---
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 2, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 1, false, true));

            // --- Shimmering white/silver temporal sphere ---
            for (int i = 0; i < 150; i++) {
                double theta = Math.random() * Math.PI * 2;
                double phi = Math.random() * Math.PI;
                double r = AOE_RADIUS;
                double px = player.getX() + Math.sin(phi) * Math.cos(theta) * r;
                double py = player.getY() + 1.0 + Math.cos(phi) * r;
                double pz = player.getZ() + Math.sin(phi) * Math.sin(theta) * r;
                serverLevel.sendParticles(
                    new DustParticleOptions(0xE0E0FF, 1.8F),
                    px, py, pz,
                    1, 0.0, 0.0, 0.0, 0.0
                );
            }

            // Clock-like rotating particle ring at player's feet
            for (int i = 0; i < 72; i++) {
                double angle = Math.toRadians(i * 5);
                double ringR = 3.0;
                double px = player.getX() + Math.cos(angle) * ringR;
                double pz = player.getZ() + Math.sin(angle) * ringR;
                serverLevel.sendParticles(
                    ParticleTypes.END_ROD,
                    px, player.getY() + 0.2, pz,
                    1, 0.0, 0.1, 0.0, 0.0
                );
            }
        }

        // 5-second cooldown (100 ticks)
        player.getCooldowns().addCooldown(stack, 100);
        return InteractionResult.SUCCESS;
    }
}
