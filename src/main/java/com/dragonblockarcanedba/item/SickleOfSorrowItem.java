package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.effect.DbaEffects;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
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
 * Purple Scythe — The Dimensional Rift weapon.
 * 
 * Left-click: 7 damage, applies Melting I for 10 seconds, costs 1 durability.
 * Right-click: "Dimensional Rift" — 5-block radius 360° AOE applying Melting
 *   at tiered levels based on distance (closer = higher level + longer duration).
 *   5-second cooldown, costs 3 durability.
 * 
 * Enchantable (enchantment value 15, same as iron).
 * 1500 max durability.
 */
public class SickleOfSorrowItem extends Item {

    public SickleOfSorrowItem(Properties properties) {
        super(properties.attributes(
            ItemAttributeModifiers.builder()
                .add(
                    Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                        BASE_ATTACK_DAMAGE_ID,
                        6.0, // +6 on top of base 1 = 7 total
                        AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
                .add(
                    Attributes.ATTACK_SPEED,
                    new AttributeModifier(
                        BASE_ATTACK_SPEED_ID,
                        -2.8, // Slow like an axe (effective 1.2 attacks/sec)
                        AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
                .build()
        ));
    }

    // --- Left Click: Melting I for 10 seconds ---
    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Apply Melting Level 1 (amplifier 0) for 10 seconds (200 ticks), passing attacker for XP tracking
        target.addEffect(new MobEffectInstance(DbaEffects.MELTING_HOLDER, 200, 0, false, true), attacker);
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
    }

    // --- Right Click: Dimensional Rift AOE ---
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) level;

            // Scan 5-block radius for all living entities (excluding the user)
            AABB aoe = player.getBoundingBox().inflate(5.0);
            List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, aoe, e -> e != player && e.isAlive()
            );

            for (LivingEntity target : targets) {
                double distance = player.distanceTo(target);
                if (distance <= 5.0) {
                    // Distance-based level: closer = higher level
                    // 0-1 blocks: Lvl 5 (amp 4), 1-2: Lvl 4 (amp 3), etc.
                    int amplifier = Math.max(0, 4 - (int) Math.floor(distance));

                    // Duration scales with level: 30s base + 5s per level above 1
                    int durationSec = 30 + (amplifier * 5);
                    int durationTicks = durationSec * 20;

                    target.addEffect(new MobEffectInstance(
                        DbaEffects.MELTING_HOLDER, durationTicks, amplifier, false, true
                    ), player);
                }
            }

            // --- Purple particle ring effect (360°) ---
            for (int i = 0; i < 72; i++) {
                double angle = Math.toRadians(i * 5);
                for (double r = 1.0; r <= 5.0; r += 1.0) {
                    double px = player.getX() + Math.cos(angle) * r;
                    double pz = player.getZ() + Math.sin(angle) * r;
                    serverLevel.sendParticles(
                        new DustParticleOptions(0x9900CC, 1.8F),
                        px, player.getY() + 0.5, pz,
                        1, 0.0, 0.3, 0.0, 0.0
                    );
                }
            }

            stack.hurtAndBreak(3, player, EquipmentSlot.MAINHAND);
        }

        // 5-second cooldown (100 ticks)
        player.getCooldowns().addCooldown(stack, 100);
        return InteractionResult.SUCCESS;
    }
}
