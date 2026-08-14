package com.dragonblockarcanedba.item;




import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;

import java.util.List;

public class PowerPoleItem extends Item {

    public PowerPoleItem(Properties properties) {
        super(properties.attributes(
            ItemAttributeModifiers.builder()
                .add(
                    Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                        BASE_ATTACK_DAMAGE_ID,
                        749.0, // Late game damage (1 + 749 = 750)
                        AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
                .add(
                    Attributes.ATTACK_SPEED,
                    new AttributeModifier(
                        BASE_ATTACK_SPEED_ID,
                        -2.0, // Faster than normal sword
                        AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
                .build()
        ));
    }
    // Left Click: Whirlwind Staff (AoE Knockback and Damage)
    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Wind spin logic is now handled by the left-click swing packet
    }

    public static void performWindSpin(Player player, ItemStack stack) {
        if (!player.level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) player.level();
            
            double maxRange = 25.0; // 25 blocks max range
            Vec3 eyePos = player.getEyePosition();
            Vec3 lookDir = player.getLookAngle();
            
            // Inflate AABB to cover the max range
            AABB aoe = player.getBoundingBox().inflate(maxRange);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, aoe, e -> e != player && e.isAlive());
            
            for (LivingEntity t : targets) {
                Vec3 targetPos = t.position().add(0, t.getBbHeight() / 2, 0);
                Vec3 toTarget = targetPos.subtract(eyePos);
                double distance = toTarget.length();
                
                if (distance > maxRange || distance < 0.1) continue;
                
                // Cone angle check - getting wider further out
                // Let's say cone is 60 degrees total (30 degrees each side)
                Vec3 toTargetDir = toTarget.normalize();
                double dotProduct = lookDir.dot(toTargetDir);
                double angleToTarget = Math.toDegrees(Math.acos(dotProduct));
                
                if (angleToTarget <= 30.0) {
                    // Inverse area damage scaling
                    // Wider it goes, the more split the damage is.
                    // Damage = 750 / max(1.0, distance / 3.0)
                    float damage = 750.0f / (float) Math.max(1.0, distance / 3.0);
                    
                    t.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(player), damage);
                    
                    // Knockback outward from player
                    Vec3 knockback = toTargetDir.scale(1.5 / Math.max(1.0, distance / 5.0));
                    t.setDeltaMovement(t.getDeltaMovement().add(knockback.x, 0.2, knockback.z));
                    t.hurtMarked = true;
                }
            }

            // Wind Particles in a Cone
            // Fire multiple rings of particles expanding outward
            for (double d = 1.0; d <= maxRange; d += 3.0) {
                Vec3 ringCenter = eyePos.add(lookDir.scale(d));
                double ringRadius = d * Math.tan(Math.toRadians(30.0)); // Match the 30 degree cone
                
                for (int i = 0; i < 360; i += 45) { // Less dense further out is fine, or keep it dense
                    double angle = Math.toRadians(i);
                    // Generate orthogonal vectors to lookDir for the ring
                    Vec3 up = new Vec3(0, 1, 0);
                    if (Math.abs(lookDir.y) > 0.9) up = new Vec3(1, 0, 0);
                    Vec3 right = lookDir.cross(up).normalize();
                    Vec3 upOrthogonal = right.cross(lookDir).normalize();
                    
                    Vec3 particlePos = ringCenter
                            .add(right.scale(Math.cos(angle) * ringRadius))
                            .add(upOrthogonal.scale(Math.sin(angle) * ringRadius));
                            
                    serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.CLOUD,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.1, 0.1, 0.1, 0.05
                    );
                }
            }
        }
    }

    // Right Click: Extended Reach
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) level;
            double reach = 30.0;
            
            Vec3 eyePos = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            Vec3 endPos = eyePos.add(look.scale(reach));
            
            AABB hitBox = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0D);
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eyePos, endPos, hitBox, e -> !e.isSpectator() && e.isPickable(), reach * reach);
            
            Vec3 hitVec = endPos;
            if (entityHit != null && entityHit.getEntity() instanceof LivingEntity living) {
                hitVec = entityHit.getLocation();
                
                // Damage
                living.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(player), 750.0f);
                
                // Slowness
                living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 3, false, false));
                
                // 15% Blackout Stun
                if (serverLevel.getRandom().nextFloat() < 0.15f) {
                    int stunDuration = 60 + serverLevel.getRandom().nextInt(41); // 3-5 seconds
                    living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, stunDuration, 0, false, false));
                    living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, stunDuration, 10, false, false)); // Full freeze
                }
            } else {
                HitResult blockHit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
                if (blockHit.getType() != HitResult.Type.MISS) {
                    hitVec = blockHit.getLocation();
                }
            }
            
            // Visual Extension using particles (Red/Orange line)
            double dist = eyePos.distanceTo(hitVec);
            for (double d = 0; d < dist; d += 0.5) {
                Vec3 p = eyePos.add(look.scale(d));
                serverLevel.sendParticles(
                    new DustParticleOptions(0xFF4500, 1.5F), // Orange-Red
                    p.x, p.y, p.z,
                    1, 0.0, 0.0, 0.0, 0.0
                );
            }
            
            player.getCooldowns().addCooldown(stack, 40); // 2 second cooldown
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
