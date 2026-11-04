package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
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

public class DimensionalSwordItem extends Item implements com.dragonblockarcanedba.item.geo.DbaGeoWeapon {
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
            // Slashes had no cooldown at all: a full stamina bar bought roughly 90
            // back-to-back 750 damage projectiles. A 16 tick cadence keeps it fast
            // but paced, and heavy enough per shot for the trail and the lingering
            // tear on impact to actually read.
            if (player.getCooldowns().isOnCooldown(stack)) {
                return;
            }
            // Drain Stamina: 100% per minute (~13 ticks cadence)
            if (!WeaponDrainHelper.drainStaminaDiscrete(player, 100.0, 13)) {
                return;
            }
            player.getCooldowns().addCooldown(stack, 16);

            // Check NBT for tilt state to alternate left and right
            boolean tiltRight = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getBoolean("tiltRight").orElse(false);

            // Stat scaling like every sibling weapon: 750 + Strength x 3 (the
            // slash used to be a flat 750 and fell behind every level).
            float strength = player instanceof PlayerStatsAccessor accessor ? accessor.dba$getStrength() : 0;
            float slashDamage = 750.0f + strength * 3.0f;
            DimensionalSlashEntity slash = new DimensionalSlashEntity(player.level(), player, tiltRight, slashDamage);
            Vec3 look = player.getLookAngle();
            slash.setDeltaMovement(look.x * 2.5, look.y * 2.5, look.z * 2.5); // Fast projectile
            player.level().addFreshEntity(slash);
            // The slash's audio identity was entirely borrowed vanilla sounds; give it the blade ring.
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                com.dragonblockarcanedba.sound.DbaSounds.BLADE_SHING, net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 1.3f);

            // Toggle tilt for next swing
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            tag.putBoolean("tiltRight", !tiltRight);
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        }
    }

    /**
     * Find a landing cell at or above start whose feet and head cells are both
     * free of collision, climbing up to maxClimb cells. Returns null when no
     * free cell exists, which refuses the warp before any Ki is charged.
     */
    private static BlockPos findFreeLanding(ServerLevel level, BlockPos start, int maxClimb) {
        for (int dy = 0; dy <= maxClimb; dy++) {
            BlockPos feet = start.above(dy);
            if (level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
                return feet;
            }
        }
        return null;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerLevel serverLevel = (ServerLevel) level;

        // Step 1: aim and validate FIRST. Ki used to be charged before the
        // raycast ran, so a blocked warp or a blind sky misclick still cost the
        // full drain. Now the target has to prove it is real before anything
        // is paid.
        double maxDistance = 1024.0;
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookDir = player.getLookAngle();
        Vec3 traceEnd = eyePos.add(lookDir.scale(maxDistance));
        ClipContext context = new ClipContext(eyePos, traceEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        HitResult hitResult = level.clip(context);

        Vec3 riftPos;
        Vec3 landing;

        if (hitResult.getType() == HitResult.Type.BLOCK && hitResult instanceof BlockHitResult blockHit) {
            Direction face = blockHit.getDirection();
            BlockPos targetBlock = blockHit.getBlockPos().relative(face);
            BlockPos free = findFreeLanding(serverLevel, targetBlock, 4);
            if (free == null) {
                return InteractionResult.FAIL;
            }
            riftPos = blockHit.getLocation();
            landing = new Vec3(free.getX() + 0.5, free.getY(), free.getZ() + 0.5);
        } else {
            // Aiming into open sky with no block in sight: warp 128 blocks
            // forward along the view line, validated the same way.
            Vec3 dest = eyePos.add(lookDir.scale(128.0));
            BlockPos cell = BlockPos.containing(dest.x, dest.y, dest.z);
            BlockPos free = findFreeLanding(serverLevel, cell, 4);
            if (free == null) {
                return InteractionResult.FAIL;
            }
            riftPos = dest;
            landing = free.equals(cell)
                ? dest
                : new Vec3(free.getX() + 0.5, free.getY(), free.getZ() + 0.5);
        }

        // Step 2: the target is real, so charge. Drain Ki: 100% per minute for
        // the 100-tick (5-second) cooldown cycle (8.33% Max Ki). If the drain
        // fails, nothing was spawned and no cooldown is set.
        if (!WeaponDrainHelper.drainKiDiscrete(player, 100.0, 100)) {
            return InteractionResult.FAIL;
        }

        // Step 3: open the origin rift and arm it with the warp. The rift
        // builds up at the player's feet, pulling the world in, and fires the
        // teleport at its burst tick: the player lands exactly when the
        // destination rift opens, and the destination burst (damage, fling,
        // debris, scar, camera weight) happens there. The origin is the tear
        // you leave; the destination is the heavier end, and it now renders
        // bigger so the asymmetry is visible: origin 400 + Strength x 2 at
        // radius 3.0, destination 900 + Strength x 3 at radius 4.5.
        float strength = player instanceof PlayerStatsAccessor accessor ? accessor.dba$getStrength() : 0;
        DimensionalWarpRiftEntity originRift = new DimensionalWarpRiftEntity(
            serverLevel, player, player.position(), 3.0f, false, 400.0f + strength * 2.0f
        );
        originRift.armWarp(landing, riftPos, 900.0f + strength * 3.0f, 4.5f);
        serverLevel.addFreshEntity(originRift);

        // 5-second cooldown, longer than either rift's life.
        player.getCooldowns().addCooldown(stack, 100);
        return InteractionResult.SUCCESS;
    }
}
