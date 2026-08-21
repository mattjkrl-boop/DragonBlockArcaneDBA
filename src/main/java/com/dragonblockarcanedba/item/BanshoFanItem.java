package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.List;

/**
 * Bansho Fan — The Storm Emperor. Endgame legendary weapon.
 * 
 * Left-click: 400 + (Strength × 1.5) damage.
 *   - Gale Force: Launches target 10 blocks with strong upward velocity
 *   - Applies Bleeding II for 15 seconds (wind cuts)
 *   - Cyclone Slash: 30% chance to AOE hit all entities within 3 blocks of target
 *     for 50% of main hit's damage
 * 
 * Right-click: "Tempest Barrage"
 *   - Fires 5 enhanced wind charges in a spread pattern
 *   - Each deals 300 + (Spirit × 1) damage on impact (via WindChargeMixin)
 *   - Wind charges apply Bleeding III for 20 seconds
 *   - 3× velocity (4.5F), perfect accuracy
 *   - 2-second cooldown
 * 
 * Unbreakable legendary weapon. No durability.
 */
public class BanshoFanItem extends Item {
    public BanshoFanItem(Properties properties) {
        super(properties.attributes(createModifiers()));
    }

    private static ItemAttributeModifiers createModifiers() {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                    BASE_ATTACK_DAMAGE_ID,
                    399.0, // +399 on top of base 1 = 400 total
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            )
            .add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                    BASE_ATTACK_SPEED_ID,
                    -1.0, // Fastest weapon — swift fan swipes (effective 3.0 attacks/sec)
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            );

        // MC 26.2 Physics: Storm Emperor aerial glide (drastically reduced mid-air drag)
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("bansho_wind_drag"), -0.70, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
        );

        return builder.build();
    }

    // --- Left Click: Massive knockback + Bleeding II + Cyclone Slash ---
    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Gale Force: Launch target 10 blocks with strong upward velocity & MC 26.2 catapult physics
        Vec3 diff = target.position().subtract(attacker.position()).normalize().scale(5.0);
        target.setDeltaMovement(target.getDeltaMovement().add(diff.x, 1.2, diff.z));
        target.hurtMarked = true; // Force velocity sync to client

        com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
            target,
            com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID,
            com.dragonblockarcanedba.DragonBlockArcaneDBA.id("bansho_gale_bounce"),
            0.85,
            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
        );
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
            target,
            com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID,
            com.dragonblockarcanedba.DragonBlockArcaneDBA.id("bansho_gale_drag"),
            -0.60,
            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );

        // Apply Bleeding II (amplifier 1) for 15 seconds (300 ticks)
        target.addEffect(new MobEffectInstance(DbaEffects.BLEEDING_HOLDER, 300, 1, false, true), attacker);

        if (attacker instanceof ServerPlayer serverPlayer) {
            ServerLevel serverLevel = (ServerLevel) serverPlayer.level();

            // Stat-scaled bonus damage: Strength × 1.5
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) serverPlayer;
            float strengthBonus = (float) (accessor.dba$getStrength() * 1.5);
            if (strengthBonus > 0) {
                target.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(serverPlayer), strengthBonus);
            }

            // Cyclone Slash: 30% chance to AOE all entities within 3 blocks of target
            if (serverLevel.getRandom().nextFloat() < 0.30f) {
                float cycloneDamage = (400.0f + strengthBonus) * 0.5f; // 50% of main hit
                AABB cycloneArea = target.getBoundingBox().inflate(3.0);
                List<LivingEntity> nearbyTargets = serverLevel.getEntitiesOfClass(
                    LivingEntity.class, cycloneArea,
                    e -> e != attacker && e != target && e.isAlive()
                );

                for (LivingEntity nearby : nearbyTargets) {
                    nearby.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(serverPlayer), cycloneDamage);
                    // Knockback nearby targets too
                    Vec3 knockback = nearby.position().subtract(target.position()).normalize().scale(2.0);
                    nearby.setDeltaMovement(nearby.getDeltaMovement().add(knockback.x, 0.5, knockback.z));
                    nearby.hurtMarked = true;

                    // Apply Bleeding I to cyclone targets
                    nearby.addEffect(new MobEffectInstance(DbaEffects.BLEEDING_HOLDER, 200, 0, false, true), serverPlayer);
                }

                // Cyclone particle effect — multi-layered emerald & jade swirling vortex around target
                for (int i = 0; i < 90; i++) {
                    double angle = Math.toRadians(i * 8);
                    double r = 3.5 * (1.0 - (i / 90.0));
                    double height = i * 0.06;
                    double px = target.getX() + Math.cos(angle) * r;
                    double pz = target.getZ() + Math.sin(angle) * r;
                    serverLevel.sendParticles(
                        new DustParticleOptions(i % 2 == 0 ? 0x00FF88 : 0x88FFCC, 1.8F),
                        px, target.getY() + height, pz,
                        1, -Math.sin(angle) * 0.2, 0.08, Math.cos(angle) * 0.2, 0.02
                    );
                }
            }

            // Standard impact wind burst & sweep particles
            serverLevel.sendParticles(
                new DustParticleOptions(0x00FF99, 2.2F),
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                20, 0.6, 0.4, 0.6, 0.18
            );
            serverLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                3, 0.2, 0.2, 0.2, 0.0
            );
        }
    }

    // --- Right Click: Tempest Barrage — 5 wind charges in a spread ---
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        // Play wind charge sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.WIND_CHARGE_THROW, SoundSource.NEUTRAL,
            0.9F, 0.5F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            // Emerald launch burst at player eye position
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            for (int i = 0; i < 25; i++) {
                serverLevel.sendParticles(
                    new DustParticleOptions(0x00FFAA, 1.6F),
                    eye.x + look.x * 0.8 + (serverLevel.getRandom().nextDouble() - 0.5) * 0.6,
                    eye.y + look.y * 0.8 + (serverLevel.getRandom().nextDouble() - 0.5) * 0.6,
                    eye.z + look.z * 0.8 + (serverLevel.getRandom().nextDouble() - 0.5) * 0.6,
                    1, look.x * 0.4, look.y * 0.4, look.z * 0.4, 0.08
                );
            }

            // Fire 5 wind charges in a spread pattern
            float[] yawOffsets = { -8.0f, -4.0f, 0.0f, 4.0f, 8.0f };
            float[] pitchOffsets = { 2.0f, -1.0f, 0.0f, -1.0f, 2.0f };

            for (int i = 0; i < 5; i++) {
                WindCharge windCharge = new WindCharge(player, level,
                    player.getX(), player.getEyeY(), player.getZ());
                windCharge.shootFromRotation(player,
                    player.getXRot() + pitchOffsets[i],
                    player.getYRot() + yawOffsets[i],
                    0.0F, 4.5F, 0.0F); // 3× velocity, perfect accuracy
                if (windCharge instanceof com.dragonblockarcanedba.util.BanshoWindChargeMarker marker) {
                    marker.dba$setFromBanshoFan(true);
                }
                level.addFreshEntity(windCharge);
            }
        }

        // 2-second cooldown (40 ticks)
        player.getCooldowns().addCooldown(itemStack, 40);
        return InteractionResult.SUCCESS;
    }
}
