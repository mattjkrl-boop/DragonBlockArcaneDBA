package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
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
 * Spirit Sword — The Annihilator Blade. Endgame legendary weapon.
 * 
 * Left-click: 500 + (Strength × 2) damage.
 *   - Applies Levitation III for 6 seconds
 *   - Deals bonus 2% of target's max HP as magic damage (Spirit Cleave)
 *   - Applies Glowing for 10 seconds (target can't hide)
 *   - 20% chance to Disarm (target drops held item)
 * 
 * Right-click (hold): Spirit Cannon — continuous beam (32-block range).
 *   - Pierces through ALL entities in line
 *   - 200 + (Spirit × 1.5) damage per pulse (every 10 ticks)
 *   - Each pulse also deals 2% of target's max HP as magic damage
 *   - Applies Weakness II for 5 seconds
 *   - Alternating cyan/white particle beam with impact explosions
 *   - On release: 1-second cooldown
 * 
 * Unbreakable legendary weapon. No durability.
 */
public class SpiritSwordItem extends Item {
    private static final double PULSE_RANGE = 32.0;

    public SpiritSwordItem(Properties properties) {
        super(properties.attributes(
            ItemAttributeModifiers.builder()
                .add(
                    Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                        BASE_ATTACK_DAMAGE_ID,
                        499.0, // +499 on top of base 1 = 500 total
                        AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
                .add(
                    Attributes.ATTACK_SPEED,
                    new AttributeModifier(
                        BASE_ATTACK_SPEED_ID,
                        -1.0, // Fast legendary blade (effective 3.0 attacks/sec)
                        AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
                .build()
        ));
    }

    // --- Left Click: Massive damage + Levitation III + Spirit Cleave + Disarm ---
    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Apply Spirit Impale (mid-air suspension + damage weakness + divine Ki radiance)
        target.addEffect(new MobEffectInstance(DbaEffects.SPIRIT_IMPALE_HOLDER, 120, 0, false, true), attacker);
        target.addEffect(new MobEffectInstance(DbaEffects.CINEMATIC_TRACKING_HOLDER, 120, 0, false, false, false), attacker);

        // Spirit Cleave: 2% of target's max HP as bonus magic damage
        if (attacker instanceof ServerPlayer serverPlayer) {
            ServerLevel serverLevel = (ServerLevel) serverPlayer.level();
            float spiritCleave = target.getMaxHealth() * 0.02f;
            target.hurtServer(serverLevel, serverLevel.damageSources().magic(), spiritCleave);

            // Stat-scaled bonus damage: Strength × 2
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) serverPlayer;
            float strengthBonus = (float) (accessor.dba$getStrength() * 2.0);
            if (strengthBonus > 0) {
                target.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(serverPlayer), strengthBonus);
            }

            // 20% chance to Disarm (drop held item)
            if (serverLevel.getRandom().nextFloat() < 0.20f && target instanceof LivingEntity livingTarget) {
                ItemStack heldItem = livingTarget.getMainHandItem();
                if (!heldItem.isEmpty()) {
                    livingTarget.spawnAtLocation(serverLevel, heldItem.copy());
                    livingTarget.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                }
            }

            // Impact particles — cyan burst at target
            serverLevel.sendParticles(
                new DustParticleOptions(0x00FFFF, 2.5F),
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                15, 0.4, 0.4, 0.4, 0.1
            );
        }
    }

    // --- Right Click: Start holding to fire Spirit Cannon ---
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

    // --- Continuous Spirit Cannon beam while holding right-click ---
    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide() || !(livingEntity instanceof ServerPlayer player)) return;

        int ticksUsed = getUseDuration(stack, livingEntity) - remainingUseDuration;
        ServerLevel serverLevel = (ServerLevel) level;

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getViewVector(1.0f);

        // --- Continuous dense core beam + outer spiraling energy drill rings every tick ---
        boolean useCyan = (ticksUsed % 2 == 0);
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = lookVec.cross(up).normalize();
        if (right.lengthSqr() < 0.001) right = new Vec3(1, 0, 0);
        Vec3 orthoUp = right.cross(lookVec).normalize();

        for (double d = 1.0; d <= PULSE_RANGE; d += 0.5) {
            Vec3 point = eyePos.add(lookVec.scale(d));

            // Dense Core
            serverLevel.sendParticles(
                new DustParticleOptions(useCyan ? 0x00FFFF : 0xFFFFFF, 1.6F),
                point.x, point.y, point.z,
                1, 0.01, 0.01, 0.01, 0.0
            );

            // Double spiraling energy drill helix
            double spiralAngle1 = (ticksUsed * 0.4) + (d * 0.8);
            double spiralAngle2 = spiralAngle1 + Math.PI;
            double spiralRadius = 0.45;

            Vec3 offset1 = right.scale(Math.cos(spiralAngle1) * spiralRadius).add(orthoUp.scale(Math.sin(spiralAngle1) * spiralRadius));
            Vec3 offset2 = right.scale(Math.cos(spiralAngle2) * spiralRadius).add(orthoUp.scale(Math.sin(spiralAngle2) * spiralRadius));

            serverLevel.sendParticles(
                new DustParticleOptions(0x00E5FF, 1.2F),
                point.x + offset1.x, point.y + offset1.y, point.z + offset1.z,
                1, 0.0, 0.0, 0.0, 0.0
            );
            serverLevel.sendParticles(
                new DustParticleOptions(0xFFFFFF, 1.0F),
                point.x + offset2.x, point.y + offset2.y, point.z + offset2.z,
                1, 0.0, 0.0, 0.0, 0.0
            );
        }

        // Fire pulse damage every 10 ticks (0.5 seconds)
        if (ticksUsed > 0 && ticksUsed % 10 == 0) {
            Vec3 endPos = eyePos.add(lookVec.scale(PULSE_RANGE));

            // Calculate stat-scaled beam damage: 200 + (Spirit × 1.5)
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
            float beamDamage = 200.0f + (float) (accessor.dba$getSpirit() * 1.5);

            // --- Piercing beam: hit ALL entities in the line ---
            AABB searchArea = new AABB(eyePos, endPos).inflate(1.0);
            List<Entity> entities = level.getEntities(
                player, searchArea, e -> !e.isSpectator() && e.isPickable() && e.isAlive()
            );

            for (Entity entity : entities) {
                AABB entityBB = entity.getBoundingBox().inflate(0.3);
                Optional<Vec3> hitVec = entityBB.clip(eyePos, endPos);
                if (hitVec.isPresent() && entity instanceof LivingEntity livingTarget) {
                    // Main beam damage
                    livingTarget.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(player), beamDamage);

                    // 2% of target's max HP as bonus magic damage
                    float percentDamage = livingTarget.getMaxHealth() * 0.02f;
                    livingTarget.hurtServer(serverLevel, serverLevel.damageSources().magic(), percentDamage);

                    // Apply Spirit Impale for 5 seconds (100 ticks)
                    livingTarget.addEffect(new MobEffectInstance(DbaEffects.SPIRIT_IMPALE_HOLDER, 100, 0, false, true), player);

                    // Impact particle burst at hit entity — big cyan/white explosion
                    serverLevel.sendParticles(
                        new DustParticleOptions(0x00FFFF, 2.5F),
                        livingTarget.getX(), livingTarget.getY() + livingTarget.getBbHeight() * 0.5, livingTarget.getZ(),
                        12, 0.4, 0.4, 0.4, 0.08
                    );
                    serverLevel.sendParticles(
                        new DustParticleOptions(0xFFFFFF, 3.0F),
                        livingTarget.getX(), livingTarget.getY() + livingTarget.getBbHeight() * 0.5, livingTarget.getZ(),
                        3, 0.1, 0.1, 0.1, 0.0
                    );
                }
            }
        }
    }

    // --- On release: apply 1-second cooldown ---
    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player) {
            // 1-second cooldown after releasing the cannon (20 ticks)
            player.getCooldowns().addCooldown(stack, 20);
        }
        return true;
    }
}
