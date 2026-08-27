package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.DimensionalSlashEntity;
import com.dragonblockarcanedba.entity.DimensionalWarpRiftEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.dragonblockarcanedba.util.WeaponDrainHelper;

public class DimensionalSwordItem extends Item {
    public DimensionalSwordItem(Properties properties) {
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
                    -2.4, // Standard sword speed
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            );

        // MC 26.2 Stealth & Physics: Dimensional Phasing & Spacetime Glide
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.NAME_PLATE_DIST_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("dimensional_nameplate_stealth"), -64.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
        );
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.MINI_NAME_PLATE_DIST_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("dimensional_mini_nameplate_stealth"), -10.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
        );
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("dimensional_air_drag"), -0.70, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
        );

        return builder.build();
    }

    // This handles the melee hit. 
    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player && !player.level().isClientSide()) {
            // Drain Stamina: 100% per minute (~13 ticks cadence = ~1.08% Max Stamina)
            WeaponDrainHelper.drainStaminaDiscrete(player, 100.0, 13);

            // Apply Melting to the target
            target.addEffect(new MobEffectInstance(DbaEffects.MELTING_HOLDER, 200, 1, false, true), attacker);
        }
    }

    public static void fireSlash(Player player, ItemStack stack) {
        if (!player.level().isClientSide()) {
            // Drain Stamina: 100% per minute (~13 ticks cadence)
            if (!WeaponDrainHelper.drainStaminaDiscrete(player, 100.0, 13)) {
                return;
            }

            // Check NBT for tilt state to alternate left and right
            boolean tiltRight = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getBoolean("tiltRight").orElse(false);
            
            DimensionalSlashEntity slash = new DimensionalSlashEntity(player.level(), player, tiltRight, 750.0f);
            Vec3 look = player.getLookAngle();
            slash.setDeltaMovement(look.x * 2.5, look.y * 2.5, look.z * 2.5); // Fast projectile
            player.level().addFreshEntity(slash);

            // Toggle tilt for next swing
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            tag.putBoolean("tiltRight", !tiltRight);
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        // Drain Ki: 100% per minute for 100-tick (5-second) cooldown cycle (8.33% Max Ki)
        if (!WeaponDrainHelper.drainKiDiscrete(player, 100.0, 100)) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) level;
            
            // Raycast to find destination - any distance you can see (up to 1024 blocks)
            double maxDistance = 1024.0;
            Vec3 eyePos = player.getEyePosition();
            Vec3 lookDir = player.getLookAngle();
            Vec3 traceEnd = eyePos.add(lookDir.scale(maxDistance));
            ClipContext context = new ClipContext(eyePos, traceEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
            HitResult hitResult = level.clip(context);
            
            Vec3 dest;
            double targetX, targetY, targetZ;

            if (hitResult.getType() == HitResult.Type.BLOCK && hitResult instanceof BlockHitResult blockHit) {
                Direction face = blockHit.getDirection();
                BlockPos targetBlock = blockHit.getBlockPos().relative(face);
                dest = blockHit.getLocation();
                targetX = targetBlock.getX() + 0.5;
                targetY = targetBlock.getY();
                targetZ = targetBlock.getZ() + 0.5;
            } else {
                // If aiming into open sky/void with no block in sight, warp forward along view line
                dest = eyePos.add(lookDir.scale(128.0));
                targetX = dest.x;
                targetY = dest.y;
                targetZ = dest.z;
            }
                
            // Phase 1: Physical 3D Departure Origin Rift
            DimensionalWarpRiftEntity originRift = new DimensionalWarpRiftEntity(
                serverLevel, player, player.position(), 3.5f, false, 750.0f
            );
            serverLevel.addFreshEntity(originRift);
            
            // Phase 2: Spatial Rift Energy Surge & Momentary Darkness Disorientation
            player.addEffect(new MobEffectInstance(DbaEffects.RIFTED_HOLDER, 60, 0, false, false));
            player.addEffect(new MobEffectInstance(DbaEffects.DARK_FADED_HOLDER, 35, 0, false, false));
            
            // Phase 3: Physical 3D Arrival Destination Rift
            DimensionalWarpRiftEntity destRift = new DimensionalWarpRiftEntity(
                serverLevel, player, dest, 3.5f, true, 750.0f
            );
            serverLevel.addFreshEntity(destRift);

            // Audio cues
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 0.8f);
            serverLevel.playSound(null, targetX, targetY, targetZ,
                net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 0.8f);
            
            // Phase 4: Teleport
            player.teleportTo(targetX, targetY, targetZ);
            player.resetFallDistance();
            
            // 5-second cooldown
            player.getCooldowns().addCooldown(stack, 100);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }
}
