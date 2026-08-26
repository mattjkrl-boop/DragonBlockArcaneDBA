package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.DimensionalRiftEntity;
import com.dragonblockarcanedba.entity.SorrowSlashEntity;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Sickle of Sorrow — The Dimensional Reaper. Endgame legendary weapon.
 * 
 * Left-click: 750 + (Strength × 3) damage.
 *   - Spawns physical 3D Sorrow Slash geometry on melee hits and air swings.
 *   - Applies Melting III for 15 seconds.
 *   - Soul Rend: Steals 3% of damage dealt as healing (lifesteal).
 *   - Applies Sorrow Rift for 4 seconds (weeping shadow corruption).
 * 
 * Right-click: "Dimensional Rift" — 15-block radius 3D Gravitational Vortex Domain.
 *   - Spawns persistent 3D Dimensional Rift entity (replaces particle spam).
 *   - Concentric 3D ground accretion rings, towering vertical spatial tears, 18 orbital void shards, and singularity core.
 *   - Distance-based Melting tiers up to Level 10 at point-blank.
 *   - Instant burst of 500 + (Spirit × 2) magic damage to all targets caught.
 *   - Heavy gravity well pulling enemies toward the singularity.
 *   - 3-second cooldown.
 * 
 * Unbreakable legendary weapon. No durability.
 */
public class SickleOfSorrowItem extends Item {
    private static boolean alternatingTilt = false;

    public SickleOfSorrowItem(Properties properties) {
        super(properties.attributes(createModifiers()));
    }

    private static ItemAttributeModifiers createModifiers() {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                    BASE_ATTACK_DAMAGE_ID,
                    749.0, // +749 on top of base 1 = 750 total
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            )
            .add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                    BASE_ATTACK_SPEED_ID,
                    -2.4, // Heavy, deliberate reaper swing speed
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            );

        // MC 26.2 Stealth & Physics: Grim Reaper Void Shroud & Ethereal Glide
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.NAME_PLATE_DIST_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("sickle_nameplate_stealth"), -60.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
        );
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.MINI_NAME_PLATE_DIST_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("sickle_mini_nameplate_stealth"), -10.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
        );
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("sickle_ethereal_drag"), -0.60, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
        );

        return builder.build();
    }

    // --- Left Click: Direct Melee Impact ---
    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Apply Melting III (amplifier 2) for 15 seconds (300 ticks)
        target.addEffect(new MobEffectInstance(DbaEffects.MELTING_HOLDER, 300, 2, false, true), attacker);

        // Apply Sorrow Rift for 4 seconds (80 ticks) — weeping shadow corruption
        target.addEffect(new MobEffectInstance(DbaEffects.SORROW_RIFT_HOLDER, 80, 0, false, true), attacker);

        if (attacker instanceof ServerPlayer serverPlayer) {
            ServerLevel serverLevel = (ServerLevel) serverPlayer.level();

            // Stat-scaled bonus damage: Strength × 3
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) serverPlayer;
            float strengthBonus = (float) (accessor.dba$getStrength() * 3.0);
            if (strengthBonus > 0) {
                target.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(serverPlayer), strengthBonus);
            }

            // Soul Rend: Steal 3% of total damage dealt as healing
            float totalDamage = 750.0f + strengthBonus;
            float healAmount = totalDamage * 0.03f;
            serverPlayer.heal(healAmount);

            // Spawn 3D Physical Sorrow Slash Impact Geometry directly on target
            alternatingTilt = !alternatingTilt;
            Vec3 targetCenter = target.position().add(0, target.getBbHeight() * 0.5, 0);
            SorrowSlashEntity impactSlash = new SorrowSlashEntity(
                serverLevel,
                serverPlayer,
                targetCenter,
                serverPlayer.getYRot(),
                serverPlayer.getXRot(),
                0.0f, // Damage already applied above in melee hit
                alternatingTilt,
                true  // isImpactBurst
            );
            serverLevel.addFreshEntity(impactSlash);
        }
    }

    // --- Left Click: Air Swing Physical Crescent Slash ---
    public static void performSorrowSlash(Player player, ItemStack stack) {
        if (!player.level().isClientSide() && player.level() instanceof ServerLevel serverLevel) {
            if (player.getCooldowns().isOnCooldown(stack)) return;

            PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
            float strengthBonus = (float) (accessor.dba$getStrength() * 3.0);
            float totalDamage = 750.0f + strengthBonus;

            alternatingTilt = !alternatingTilt;
            Vec3 look = player.getLookAngle();
            Vec3 spawnPos = player.getEyePosition().add(look.scale(1.2));

            SorrowSlashEntity slashWave = new SorrowSlashEntity(
                serverLevel,
                player,
                spawnPos,
                player.getYRot(),
                player.getXRot(),
                totalDamage,
                alternatingTilt,
                false // isImpactBurst = false (flying wave)
            );
            slashWave.setDeltaMovement(look.scale(1.6));
            serverLevel.addFreshEntity(slashWave);

            // Brief 8-tick swing cadence
            player.getCooldowns().addCooldown(stack, 8);
        }
    }

    // --- Right Click: Dimensional Rift 15-Block 3D Domain ---
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) level;
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

            // Calculate AOE burst damage: 500 + (Spirit × 2)
            float burstDamage = 500.0f + (float) (accessor.dba$getSpirit() * 2.0);

            // Spawn 3D Physical Dimensional Rift Domain Entity
            DimensionalRiftEntity rift = new DimensionalRiftEntity(
                serverLevel,
                player,
                player.position(),
                burstDamage
            );
            serverLevel.addFreshEntity(rift);
        }

        // 3-second cooldown (60 ticks)
        player.getCooldowns().addCooldown(stack, 60);
        return InteractionResult.SUCCESS;
    }
}
