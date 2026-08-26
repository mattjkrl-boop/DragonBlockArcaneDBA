package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.PowerPoleExtensionEntity;
import com.dragonblockarcanedba.entity.PowerPoleImpactEntity;
import com.dragonblockarcanedba.entity.PowerPoleWhirlwindEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class PowerPoleItem extends Item {

    public PowerPoleItem(Properties properties) {
        super(properties.attributes(createModifiers()));
    }

    private static ItemAttributeModifiers createModifiers() {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
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
            );

        // MC 26.2 Physics: Acrobatic Pole Vaulting Spring & Aerodynamic Glide
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("power_pole_spring_bounce"), 0.50, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
        );
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("power_pole_air_drag"), -0.40, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
        );

        return builder.build();
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
                
                Vec3 toTargetDir = toTarget.normalize();
                double dotProduct = Math.max(-1.0, Math.min(1.0, lookDir.dot(toTargetDir)));
                double angleToTarget = Math.toDegrees(Math.acos(dotProduct));

                boolean inCone = angleToTarget <= 35.0;
                boolean inClosePerimeter = distance <= 4.5; // Radial barrier around the spinner

                if (inCone || inClosePerimeter) {
                    // Inverse area damage scaling
                    float damage = 750.0f / (float) Math.max(1.0, distance / 3.0);
                    t.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(player), damage);
                    
                    // Counter incoming rush velocity and apply overwhelming gale repulsion
                    com.dragonblockarcanedba.util.MovementLimiterHelper.applyPowerPoleGaleForce(t, eyePos, distance);

                    // MC 26.2 Physics: Apply bounciness so enemies ricochet off terrain
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                        t,
                        com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID,
                        com.dragonblockarcanedba.DragonBlockArcaneDBA.id("pole_spin_bounce"),
                        0.90,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
                    );
                }
            }

            // Deflect incoming projectiles & Ki Blasts
            List<net.minecraft.world.entity.projectile.Projectile> projectiles = serverLevel.getEntitiesOfClass(
                net.minecraft.world.entity.projectile.Projectile.class, aoe, p -> p.isAlive() && p.getOwner() != player
            );
            for (net.minecraft.world.entity.projectile.Projectile p : projectiles) {
                Vec3 pPos = p.position();
                Vec3 toProj = pPos.subtract(eyePos);
                double pDist = toProj.length();
                if (pDist <= maxRange && (pDist <= 4.5 || lookDir.dot(toProj.normalize()) > 0.75)) {
                    p.setOwner(player);
                    p.setDeltaMovement(toProj.normalize().scale(2.2));
                    p.hurtMarked = true;
                }
            }

            // Spawn Physical 3D Conical Aerodynamic Hurricane Vortex Entity
            PowerPoleWhirlwindEntity whirlwind = new PowerPoleWhirlwindEntity(
                serverLevel, player, eyePos, player.getYRot(), player.getXRot(), (float) maxRange, 35.0f
            );
            serverLevel.addFreshEntity(whirlwind);

            // Auditory feedback
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.4f, 1.3f);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WIND_CHARGE_THROW, SoundSource.PLAYERS, 1.0f, 1.1f);
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
                
                // Pole Stun
                living.addEffect(new MobEffectInstance(DbaEffects.POLE_STUN_HOLDER, 100, 0, false, false));

                // MC 26.2 Physics: Apply high bounciness upon heavy impact
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                    living,
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID,
                    com.dragonblockarcanedba.DragonBlockArcaneDBA.id("pole_poke_bounce"),
                    0.95,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
                );
                
                // 15% Heavy Concussion Stun
                if (serverLevel.getRandom().nextFloat() < 0.15f) {
                    int stunDuration = 60 + serverLevel.getRandom().nextInt(41); // 3-5 seconds
                    living.addEffect(new MobEffectInstance(DbaEffects.POLE_STUN_HOLDER, stunDuration, 1, false, false));
                }
            } else {
                HitResult blockHit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
                if (blockHit.getType() != HitResult.Type.MISS) {
                    hitVec = blockHit.getLocation();
                }
            }
            
            double dist = eyePos.distanceTo(hitVec);

            // Spawn Physical 3D Stretching Power Pole Model
            PowerPoleExtensionEntity extension = new PowerPoleExtensionEntity(
                serverLevel, player, eyePos, player.getYRot(), player.getXRot(), (float) dist
            );
            serverLevel.addFreshEntity(extension);

            // Spawn Physical 3D Kinetic Impact Shockwave & Shatter Entity
            PowerPoleImpactEntity impact = new PowerPoleImpactEntity(
                serverLevel, player, hitVec, player.getYRot(), player.getXRot(), 2.4f
            );
            serverLevel.addFreshEntity(impact);

            // High-impact auditory feedback
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.4f, 1.4f);
            serverLevel.playSound(null, hitVec.x, hitVec.y, hitVec.z,
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.2f, 1.6f);
            serverLevel.playSound(null, hitVec.x, hitVec.y, hitVec.z,
                SoundEvents.HEAVY_CORE_HIT, SoundSource.PLAYERS, 1.3f, 1.2f);
            
            player.getCooldowns().addCooldown(stack, 40); // 2 second cooldown
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
