package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.util.TimeTracker;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
 * Whis Staff — Time Reversal weapon.
 * 
 * Left-click: 4 damage, reverses the target entity's movement for 3 seconds
 *   (entity retraces its recorded position history, phasing through blocks).
 *   Costs 1 durability.
 * 
 * Right-click: "Temporal Rift" — 5-block radius AOE that reverses ALL entities
 *   in range for 5 seconds. Spawns white particle sphere to show the area.
 *   10-second cooldown, costs 3 durability.
 * 
 * Enchantable (enchantment value 15, same as iron).
 * 1500 max durability.
 */
public class WhisStaffItem extends Item {

    public WhisStaffItem(Properties properties) {
        super(properties.attributes(
            ItemAttributeModifiers.builder()
                .add(
                    Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                        BASE_ATTACK_DAMAGE_ID,
                        3.0, // +3 on top of base 1 = 4 total
                        AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
                .add(
                    Attributes.ATTACK_SPEED,
                    new AttributeModifier(
                        BASE_ATTACK_SPEED_ID,
                        -3.0, // Slow (effective 1.0 attacks/sec)
                        AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
                .build()
        ));
    }

    // --- Left Click: 4 damage + reverse target for 8 seconds ---
    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target instanceof TimeTracker timeTracker) {
            timeTracker.dba$startReversing(160);
        }
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
    }

    // --- Right Click: Temporal Rift AOE ---
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) level;

            // 5-block radius AOE — reverse all living entities
            AABB aoe = player.getBoundingBox().inflate(5.0);
            List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, aoe, e -> e != player && e.isAlive()
            );

            for (LivingEntity target : targets) {
                if (player.distanceTo(target) <= 5.0 && target instanceof TimeTracker tracker) {
                    // Reverse for 20 seconds (400 ticks)
                    tracker.dba$startReversing(400);
                }
            }

            // --- White particle sphere to show the affected area ---
            for (int i = 0; i < 100; i++) {
                double theta = Math.random() * Math.PI * 2;
                double phi = Math.random() * Math.PI;
                double r = 5.0;
                double px = player.getX() + Math.sin(phi) * Math.cos(theta) * r;
                double py = player.getY() + 1.0 + Math.cos(phi) * r;
                double pz = player.getZ() + Math.sin(phi) * Math.sin(theta) * r;
                serverLevel.sendParticles(
                    new DustParticleOptions(0xFFFFFF, 1.5F),
                    px, py, pz,
                    1, 0.0, 0.0, 0.0, 0.0
                );
            }

            stack.hurtAndBreak(3, player, EquipmentSlot.MAINHAND);
        }

        // 10-second cooldown (200 ticks)
        player.getCooldowns().addCooldown(stack, 200);
        return InteractionResult.SUCCESS;
    }
}
