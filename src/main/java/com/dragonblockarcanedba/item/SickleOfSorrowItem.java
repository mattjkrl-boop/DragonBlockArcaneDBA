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
 * Sickle of Sorrow: The Dimensional Reaper. Endgame legendary weapon.
 * 
 * Left-click: 750 + (Strength × 3) damage.
 *   - Spawns physical 3D Sorrow Slash geometry on melee hits and air swings.
 *   - Applies Melting III for 15 seconds.
 *   - Soul Rend: Steals 3% of damage dealt as healing (lifesteal).
 *   - Applies Sorrow Rift for 4 seconds (weeping shadow corruption).
 * 
 * Right-click: "Dimensional Rift": 15-block radius 3D Gravitational Vortex Domain.
 *   - Spawns persistent 3D Dimensional Rift entity (replaces particle spam).
 *   - Concentric 3D ground accretion rings, towering vertical spatial tears, 18 orbital void shards, and singularity core.
 *   - Distance-based Melting tiers up to Melting IV at point-blank, 10 seconds.
 *   - Burst of 500 + (Strength × 3) damage at the expansion peak (tick 20) to all targets caught.
 *   - Heavy gravity well pulling enemies toward the singularity, then an implosion fling at collapse.
 *   - 6-second cooldown (single vanilla item cooldown, see performTechniqueDimensionalRift).
 * 
 * Unbreakable legendary weapon. No durability.
 */
import com.dragonblockarcanedba.util.WeaponDrainHelper;

public class SickleOfSorrowItem extends Item implements com.dragonblockarcanedba.item.geo.DbaGeoWeapon {

    /**
     * Slash tilt alternation is per stack, not one static shared by every player
     * (two reapers swinging at once used to flip each other's tilt). Returns the
     * tilt for this swing and stores the opposite for the next one, the same
     * pattern DimensionalSwordItem uses.
     */
    private static boolean nextTilt(Player player) {
        ItemStack stack = player.getMainHandItem().getItem() instanceof SickleOfSorrowItem
            ? player.getMainHandItem() : player.getOffhandItem();
        if (!(stack.getItem() instanceof SickleOfSorrowItem)) return false;
        net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        boolean tiltRight = tag.getBoolean("tiltRight").orElse(false);
        tag.putBoolean("tiltRight", !tiltRight);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        return tiltRight;
    }

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
        if (attacker instanceof ServerPlayer serverPlayer) {
            executeTechniqueMelee(serverPlayer, target);
        }
    }

    public static void executeTechniqueMelee(ServerPlayer serverPlayer, LivingEntity target) {
        // Swing sound at swing time; before, only entity hit sounds existed downstream.
        serverPlayer.level().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
            com.dragonblockarcanedba.sound.DbaSounds.BLADE_SHING, net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 0.8f);

        // Apply Melting III (amplifier 2) for 15 seconds (300 ticks)
        target.addEffect(new MobEffectInstance(DbaEffects.MELTING_HOLDER, 300, 2, false, true), serverPlayer);

        // Apply Sorrow Rift for 4 seconds (80 ticks), weeping shadow corruption
        target.addEffect(new MobEffectInstance(DbaEffects.SORROW_RIFT_HOLDER, 80, 0, false, true), serverPlayer);

        PlayerStatsAccessor accessor = (PlayerStatsAccessor) serverPlayer;
        int level = accessor.dba$getTechniqueLevel("sickle_of_sorrow");
        double baseDrain = com.dragonblockarcanedba.attribute.PlayerStats.getSickleBaseActionDrain(level);

        // Drain Ki for swing cadence (~13 ticks at 1.6 attacks/sec)
        WeaponDrainHelper.drainKiDiscrete(serverPlayer, baseDrain, 13);

        ServerLevel serverLevel = (ServerLevel) serverPlayer.level();

        // Stat-scaled bonus damage: Strength × 3. This is an ability hit layered
        // on the melee swing, so it uses the mod's void damage type (bespoke
        // rule 4); the plain weapon hit itself stays the vanilla player attack.
        float strengthBonus = (float) (accessor.dba$getStrength() * 3.0);
        if (strengthBonus > 0) {
            target.hurtServer(serverLevel, com.dragonblockarcanedba.util.DbaDamage.voidDmg(serverPlayer), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (strengthBonus)));
        }

        // Soul Rend: Steal 3% of total damage dealt as healing
        float totalDamage = 750.0f + strengthBonus;
        float healAmount = totalDamage * 0.03f;
        serverPlayer.heal(healAmount);

        // Spawn 3D Physical Sorrow Slash Impact Geometry directly on target.
        // Damage 0.0f is deliberate: the impact burst is a pure visual. The melee
        // hit above already applied all damage and effects, and SorrowSlashEntity's
        // impact burst mode never runs hit logic at all (see its tick()).
        Vec3 targetCenter = target.position().add(0, target.getBbHeight() * 0.5, 0);
        SorrowSlashEntity impactSlash = new SorrowSlashEntity(
            serverLevel,
            serverPlayer,
            targetCenter,
            serverPlayer.getYRot(),
            serverPlayer.getXRot(),
            0.0f, // Pure visual: melee damage already applied above
            nextTilt(serverPlayer),
            true  // isImpactBurst
        );
        serverLevel.addFreshEntity(impactSlash);
    }

    // --- Left Click: Air Swing Physical Crescent Slash ---
    public static void performSorrowSlash(Player player, ItemStack stack) {
        performTechniqueAirSwing(player);
    }

    public static void performTechniqueAirSwing(Player player) {
        if (!player.level().isClientSide() && player.level() instanceof ServerLevel serverLevel) {
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
            int level = accessor.dba$getTechniqueLevel("sickle_of_sorrow");
            double baseDrain = com.dragonblockarcanedba.attribute.PlayerStats.getSickleBaseActionDrain(level);

            // Drain Ki for 8-tick swing cadence
            if (!WeaponDrainHelper.drainKiDiscrete(player, baseDrain, 8)) {
                return;
            }

            float strengthBonus = (float) (accessor.dba$getStrength() * 3.0);
            float totalDamage = 750.0f + strengthBonus;

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                com.dragonblockarcanedba.sound.DbaSounds.BLADE_SHING, net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 0.8f);

            Vec3 look = player.getLookAngle();
            Vec3 spawnPos = player.getEyePosition().add(look.scale(1.2));

            SorrowSlashEntity slashWave = new SorrowSlashEntity(
                serverLevel,
                player,
                spawnPos,
                player.getYRot(),
                player.getXRot(),
                totalDamage,
                nextTilt(player),
                false // isImpactBurst = false (flying wave)
            );
            slashWave.setDeltaMovement(look.scale(1.6));
            serverLevel.addFreshEntity(slashWave);
        }
    }

    // --- Right Click: Dimensional Rift 15-Block 3D Domain ---

    /**
     * Rift cooldown in ticks: 120 (6 seconds), longer than the rift's 60 tick
     * life so two rifts can never stack. The vanilla item cooldown on the held
     * stack is the ONE source of truth: use() and the technique callbacks in
     * DragonBlockArcaneDBA both funnel through performTechniqueDimensionalRift,
     * which checks and sets it here. (The old static UUID map that ran beside
     * the item cooldown could desync from it and is gone.)
     */
    public static final int RIFT_COOLDOWN_TICKS = 120;

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return performTechniqueDimensionalRift(player) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    /**
     * The stack whose cooldown group gates the rift: the held Sickle of Sorrow
     * (main hand first) when one is in hand. The technique callbacks in
     * DragonBlockArcaneDBA can fire while the sickle technique is active but a
     * different item is held; cooldowns are tracked per item group, not per
     * stack instance, so a bare stack of the registered item addresses the
     * same single cooldown for that path.
     */
    private static ItemStack cooldownStack(Player player) {
        if (player.getMainHandItem().getItem() instanceof SickleOfSorrowItem) return player.getMainHandItem();
        if (player.getOffhandItem().getItem() instanceof SickleOfSorrowItem) return player.getOffhandItem();
        return new ItemStack(DbaItems.SICKLE_OF_SORROW);
    }

    public static boolean performTechniqueDimensionalRift(Player player) {
        ItemStack stack = cooldownStack(player);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return false;
        }

        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
        int level = accessor.dba$getTechniqueLevel("sickle_of_sorrow");
        double baseDrain = com.dragonblockarcanedba.attribute.PlayerStats.getSickleBaseActionDrain(level);

        // Ki is charged only when the move actually fires: a failed drain
        // returns before the cooldown is set, so nothing is locked out.
        if (!WeaponDrainHelper.drainKiDiscrete(player, baseDrain, 60)) {
            return false;
        }
        player.getCooldowns().addCooldown(stack, RIFT_COOLDOWN_TICKS);

        if (!player.level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) player.level();
            // Strength scaling, same as this weapon's melee and air swing, so
            // building for the weapon's own stat feeds its right click too.
            float burstDamage = 500.0f + (float) (accessor.dba$getStrength() * 3.0);

            DimensionalRiftEntity rift = new DimensionalRiftEntity(
                serverLevel,
                player,
                player.position(),
                burstDamage
            );
            serverLevel.addFreshEntity(rift);
        }
        return true;
    }
}
