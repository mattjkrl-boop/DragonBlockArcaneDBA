package com.dragonblockarcanedba.item;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Spirit Sword — Levitation blade with a holdable spirit pulse.
 * 
 * Left-click: 6 damage, applies Levitation for 4 seconds, costs 1 durability.
 * Right-click (hold): Fires a continuous pulse beam forward (16 block range).
 *   Every 1 second while held, deals 1 damage to the first entity in line.
 *   Spawns cyan/white particles along the beam. Costs 1 durability per pulse.
 *   On release: 3-second cooldown before pulse can be used again.
 * 
 * Enchantable (value 15, same as iron). 1500 max durability.
 */
public class SpiritSwordItem extends Item {
    private static final double PULSE_RANGE = 16.0;

    public SpiritSwordItem(Properties properties) {
        super(properties.attributes(
            ItemAttributeModifiers.builder()
                .add(
                    Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                        BASE_ATTACK_DAMAGE_ID,
                        5.0, // +5 on top of base 1 = 6 total
                        AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
                .add(
                    Attributes.ATTACK_SPEED,
                    new AttributeModifier(
                        BASE_ATTACK_SPEED_ID,
                        -2.4, // Standard sword speed (effective 1.6 attacks/sec)
                        AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
                .build()
        ));
    }

    // --- Left Click: Levitation for 4 seconds ---
    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Apply Levitation (vanilla effect) for 4 seconds (80 ticks), passing attacker for XP tracking
        target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 80, 0, false, true), attacker);
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
    }

    // --- Right Click: Start holding to fire spirit pulse ---
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000; // Can hold essentially forever
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW; // Hold animation
    }

    // --- Continuous pulse beam while holding right-click ---
    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide() || !(livingEntity instanceof ServerPlayer player)) return;

        int ticksUsed = getUseDuration(stack, livingEntity) - remainingUseDuration;
        ServerLevel serverLevel = (ServerLevel) level;

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getViewVector(1.0f);
        Vec3 endPos = eyePos.add(lookVec.scale(PULSE_RANGE));

        // --- Continuous white particle beam every tick ---
        for (double d = 1.0; d <= PULSE_RANGE; d += 0.5) {
            Vec3 point = eyePos.add(lookVec.scale(d));
            serverLevel.sendParticles(
                new DustParticleOptions(0xFFFFFF, 0.8F), // White, smaller scale
                point.x, point.y, point.z,
                1, 0.02, 0.02, 0.02, 0.0
            );
        }

        // Fire pulse damage & levitation every 10 ticks (0.5 seconds)
        if (ticksUsed > 0 && ticksUsed % 10 == 0) {
            // --- Raycast to find the closest entity in the beam ---
            AABB searchArea = new AABB(eyePos, endPos).inflate(1.0);
            List<Entity> entities = level.getEntities(
                player, searchArea, e -> !e.isSpectator() && e.isPickable() && e.isAlive()
            );

            Entity closestHit = null;
            double closestDistSq = PULSE_RANGE * PULSE_RANGE;

            for (Entity entity : entities) {
                AABB entityBB = entity.getBoundingBox().inflate(0.3);
                Optional<Vec3> hitVec = entityBB.clip(eyePos, endPos);
                if (hitVec.isPresent()) {
                    double distSq = eyePos.distanceToSqr(hitVec.get());
                    if (distSq < closestDistSq) {
                        closestDistSq = distSq;
                        closestHit = entity;
                    }
                }
            }

            // Deal 1 damage and apply levitation to the hit entity
            if (closestHit instanceof LivingEntity livingTarget) {
                livingTarget.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(player), 1.0F);
                livingTarget.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 80, 0, false, true), player);

                // Impact particle burst at the hit entity
                serverLevel.sendParticles(
                    new DustParticleOptions(0xFFFFFF, 2.0F), // White burst
                    livingTarget.getX(), livingTarget.getY() + livingTarget.getBbHeight() * 0.5, livingTarget.getZ(),
                    8, 0.3, 0.3, 0.3, 0.05
                );
            }

            // Cost 1 durability per pulse hit (every 0.5s)
            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }
    }

    // --- On release: apply 3-second cooldown ---
    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player) {
            // 3-second cooldown after releasing the pulse (60 ticks)
            player.getCooldowns().addCooldown(stack, 60);
        }
        return true;
    }
}
