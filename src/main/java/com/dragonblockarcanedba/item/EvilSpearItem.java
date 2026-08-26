package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.EvilSpearChargeEntity;
import com.dragonblockarcanedba.entity.EvilSpearProjectileEntity;
import com.dragonblockarcanedba.entity.HellHuntImpactEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Evil Spear — Cursed Hunter Weapon.
 * Target marking, supernatural spear throws, enemy impalement, and chained executions.
 */
public class EvilSpearItem extends Item {
    public static final int MAX_LEFT_CHARGE_TICKS = 120; // 6 seconds

    // Track active charging EvilSpearChargeEntity per player
    public static final Map<UUID, EvilSpearChargeEntity> ACTIVE_CHARGE_MAP = new ConcurrentHashMap<>();

    public EvilSpearItem(Properties properties) {
        super(properties.attributes(createModifiers()));
    }

    private static ItemAttributeModifiers createModifiers() {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                    BASE_ATTACK_DAMAGE_ID,
                    799.0, // 1 + 799 = 800 base damage
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            )
            .add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                    BASE_ATTACK_SPEED_ID,
                    -1.6, // Rapid thrusts
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            );

        // MC 26.2 Stealth & Physics: Cursed Hunter Stealth & Throw Aerodynamics
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.NAME_PLATE_DIST_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("evil_spear_nameplate_stealth"), -48.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
        );
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.MINI_NAME_PLATE_DIST_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("evil_spear_mini_nameplate_stealth"), -8.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
        );
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("evil_spear_air_drag"), -0.40, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
        );

        return builder.build();
    }

    // --- LEFT CLICK: Evil Impale (Charged Spectral Spear Throw) ---

    public static void onLeftClickChargeTick(ServerPlayer player, ItemStack stack, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        // Drain Ki: ~2.5% max Ki per second
        double maxKi = PlayerStats.getMaxKi(player);
        double drainPerTick = (maxKi * 0.025) / 20.0;
        double currentKi = accessor.dba$getCurrentKi();

        if (currentKi >= drainPerTick) {
            accessor.dba$addKi(-drainPerTick);
            if (chargeTicks % 5 == 0) {
                accessor.dba$syncStats();
            }
        } else {
            onLeftClickRelease(player, stack, chargeTicks);
            return;
        }

        Vec3 vel = player.getDeltaMovement();
        player.setDeltaMovement(vel.x * 0.5, vel.y, vel.z * 0.5);
        player.hurtMarked = true;

        float chargeRatio = Math.min(1.0f, chargeTicks / (float) MAX_LEFT_CHARGE_TICKS);

        // Spawn / update physical 3D Evil Spear Charge Entity (no particle spam)
        EvilSpearChargeEntity chargeEntity = ACTIVE_CHARGE_MAP.get(player.getUUID());
        if (chargeEntity == null || !chargeEntity.isAlive()) {
            chargeEntity = new EvilSpearChargeEntity(level, player);
            level.addFreshEntity(chargeEntity);
            ACTIVE_CHARGE_MAP.put(player.getUUID(), chargeEntity);
        }
        chargeEntity.setChargeRatio(chargeRatio);

        if (chargeTicks % 20 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 0.6f + chargeRatio * 0.6f);
        }
    }

    public static void onLeftClickRelease(ServerPlayer player, ItemStack stack, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        // Discard active charge entity
        EvilSpearChargeEntity chargeEntity = ACTIVE_CHARGE_MAP.remove(player.getUUID());
        if (chargeEntity != null && chargeEntity.isAlive()) {
            chargeEntity.discard();
        }

        float chargeRatio = Math.max(0.1f, Math.min(1.0f, chargeTicks / (float) MAX_LEFT_CHARGE_TICKS));

        // Damage: 450.0 + (chargeRatio * 700.0) + (Strength * 3.0 * chargeRatio)
        float baseDmg = 450.0f + (chargeRatio * 700.0f);
        float strBonus = (float) (accessor.dba$getStrength() * 3.0f * chargeRatio);
        float totalDamage = baseDmg + strBonus;

        Vec3 look = player.getLookAngle();
        double speed = 2.0 + (chargeRatio * 1.2);

        EvilSpearProjectileEntity spear = new EvilSpearProjectileEntity(level, player, totalDamage);
        spear.setDeltaMovement(look.x * speed, look.y * speed, look.z * speed);
        level.addFreshEntity(spear);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.8f, 0.7f);

        int cooldown = 20 + (int) (chargeRatio * 30);
        player.getCooldowns().addCooldown(stack, cooldown);
    }

    // --- RIGHT CLICK: Hell Hunt (Lock-on Chain Execution) ---

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.getCooldowns().isOnCooldown(stack)) {
                return InteractionResult.PASS;
            }

            if (com.dragonblockarcanedba.util.MovementLimiterHelper.isMovementImmobilized(serverPlayer)) {
                serverPlayer.sendSystemMessage(Component.literal("§cImmobilized! Cannot rush."), true);
                return InteractionResult.FAIL;
            }

            double ccMult = com.dragonblockarcanedba.util.MovementLimiterHelper.getMovementMultiplier(serverPlayer);
            ServerLevel serverLevel = (ServerLevel) level;
            // 32-meter lock-on search
            LivingEntity target = findLockOnTarget(serverPlayer, 32.0);
            if (target != null) {
                performHellHunt(serverPlayer, stack, target);
                return InteractionResult.SUCCESS;
            } else {
                // If no direct target, perform forward high-speed lunge (scaled by CC / movement limits)
                Vec3 look = serverPlayer.getLookAngle();
                serverPlayer.setDeltaMovement(look.x * 2.0 * ccMult, 0.4 * ccMult, look.z * 2.0 * ccMult);
                serverPlayer.hurtMarked = true;
                serverLevel.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5f, 0.5f);
                serverPlayer.getCooldowns().addCooldown(stack, 40);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.CONSUME;
    }

    private static LivingEntity findLockOnTarget(ServerPlayer player, double maxDist) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        AABB searchBox = player.getBoundingBox().inflate(maxDist);
        List<LivingEntity> list = level.getEntitiesOfClass(
            LivingEntity.class, searchBox,
            e -> e.isAlive() && e != player
        );

        LivingEntity best = null;
        double bestScore = 0.5; // Cosine threshold (roughly 60 degrees cone)

        for (LivingEntity e : list) {
            Vec3 toEntity = e.getEyePosition().subtract(eye).normalize();
            double dot = look.dot(toEntity);
            if (dot > bestScore && player.distanceTo(e) <= maxDist) {
                bestScore = dot;
                best = e;
            }
        }
        return best;
    }

    private static void performHellHunt(ServerPlayer player, ItemStack stack, LivingEntity initialTarget) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        Set<Integer> visited = new HashSet<>();
        List<LivingEntity> chainTargets = new ArrayList<>();
        chainTargets.add(initialTarget);
        visited.add(initialTarget.getId());

        // Find up to 7 additional nearby Marked enemies (up to 8 total, Tweak A)
        LivingEntity current = initialTarget;
        for (int i = 0; i < 7; i++) {
            AABB searchBox = current.getBoundingBox().inflate(16.0);
            List<LivingEntity> nearbyMarked = level.getEntitiesOfClass(
                LivingEntity.class, searchBox,
                e -> e.isAlive() && e != player && !visited.contains(e.getId()) &&
                     e.hasEffect(com.dragonblockarcanedba.effect.DbaEffects.MARKED_BY_EVIL_HOLDER)
            );
            if (nearbyMarked.isEmpty()) break;

            LivingEntity next = nearbyMarked.get(0);
            chainTargets.add(next);
            visited.add(next.getId());
            current = next;
        }

        // Teleport/Rush through each target in chain sequence
        int chains = chainTargets.size();
        for (int i = 0; i < chains; i++) {
            LivingEntity t = chainTargets.get(i);
            boolean isFinal = (i == chains - 1);

            // Calculate damage: each chain adds +15% damage (Tweak C)
            float multiplier = 1.0f + (i * 0.15f);
            float baseDmg = isFinal
                ? (400.0f + (chains * 150.0f) + (float) (accessor.dba$getStrength() * 3.5f))
                : (350.0f + (float) (accessor.dba$getStrength() * 2.0f));
            float finalDmg = baseDmg * multiplier;

            // Teleport next to target
            player.teleportTo(level, t.getX(), t.getY(), t.getZ(), java.util.Collections.emptySet(), player.getYRot(), player.getXRot(), false);

            // Drive into ground with Movement Curse root (100 ticks = 5s)
            t.addEffect(new MobEffectInstance(com.dragonblockarcanedba.effect.DbaEffects.MOVEMENT_CURSE_HOLDER, 100, 0, false, true));
            t.addEffect(new MobEffectInstance(com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 100, 0, false, false, false), player);
            
            // MC 26.2 Physics: Ground impalement pin friction & violent shockwave bounce
            com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                t,
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.FRICTION_ID,
                com.dragonblockarcanedba.DragonBlockArcaneDBA.id("evil_hunt_friction"),
                4.0,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                t,
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID,
                com.dragonblockarcanedba.DragonBlockArcaneDBA.id("evil_hunt_bounce"),
                0.80,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
            );

            t.hurtServer(level, level.damageSources().playerAttack(player), finalDmg);

            // Spawn Physical 3D Hell Hunt Impact Entity (Tectonic Fissures, Obsidian Spikes, Execution Pillar)
            HellHuntImpactEntity impact = new HellHuntImpactEntity(
                level, player, t.position().add(0, 0.05, 0), isFinal ? 4.5f : 3.0f, isFinal
            );
            level.addFreshEntity(impact);

            level.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.8f, 0.5f);
        }

        // Demon Surge: Demonic rush speed, attack frenzy, and jump boost for 40 ticks
        player.addEffect(new MobEffectInstance(DbaEffects.DEMON_SURGE_HOLDER, 40, 0, false, false));

        // 5-second cooldown
        player.getCooldowns().addCooldown(stack, 100);
    }
}
