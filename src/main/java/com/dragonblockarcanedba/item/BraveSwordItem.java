package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.BraveChargeEntity;
import com.dragonblockarcanedba.entity.BraveCrossSlashEntity;
import com.dragonblockarcanedba.entity.BraveRushTrailEntity;
import com.dragonblockarcanedba.entity.BraveShockwaveEntity;
import com.dragonblockarcanedba.entity.BraveSlashEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brave Sword — Heroic Legendary Sword (Tapion's Sword).
 * Relentless sword assault, escalating Brave Power, physical 3D cruciform cross slashes,
 * supersonic heroic rush flight trails, and geometric valor shockwaves.
 */
public class BraveSwordItem extends Item {
    public static final int MAX_RIGHT_CHARGE_TICKS = 160; // 8 seconds

    // Track active charging BraveChargeEntity per player
    public static final Map<UUID, BraveChargeEntity> ACTIVE_CHARGE_MAP = new ConcurrentHashMap<>();

    public BraveSwordItem(Properties properties) {
        super(properties.attributes(createModifiers()));
    }

    private static ItemAttributeModifiers createModifiers() {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                    BASE_ATTACK_DAMAGE_ID,
                    899.0, // 1 + 899 = 900 base damage
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            )
            .add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                    BASE_ATTACK_SPEED_ID,
                    -1.4, // Swift heroic swordplay
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            );

        // MC 26.2 Physics: Hero's Swift Stride & Acrobatic Glide
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.FRICTION_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("brave_hero_friction"), -0.60, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
        );
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("brave_hero_drag"), -0.50, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
        );

        return builder.build();
    }

    // --- Combo Tracking ---

    public static int getCombo(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            return customData.copyTag().getIntOr("Combo", 0);
        }
        return 0;
    }

    public static void setCombo(ItemStack stack, int combo) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        tag.putInt("Combo", combo);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    // --- LEFT CLICK: Brave Sword Assault (Combo & Finisher) ---

    public static void onLeftClickAssaultTick(ServerPlayer player, ItemStack stack, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        int combo = getCombo(stack);
        // Attack rate scales with combo (Tweak A: higher combo = faster strikes)
        int interval = Math.max(3, 8 - (combo / 2));
        if (chargeTicks % interval != 0) return;

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 strikeTarget = eye.add(look.scale(3.5));

        AABB hitBox = new AABB(eye, strikeTarget).inflate(1.5);
        List<LivingEntity> targets = level.getEntitiesOfClass(
            LivingEntity.class, hitBox,
            e -> e.isAlive() && e != player
        );

        if (!targets.isEmpty()) {
            LivingEntity target = targets.get(0);
            combo++;
            setCombo(stack, combo);

            float strength = accessor.dba$getStrength();

            if (combo >= 10) {
                // Brave Finisher (Tweak C): Dash forward & unleash massive physical 3D cross-slash
                Vec3 targetCenter = target.position().add(0, target.getBbHeight() * 0.5, 0);
                Vec3 past = target.position().add(look.scale(2.0));
                player.teleportTo(level, past.x, past.y, past.z, java.util.Collections.emptySet(), player.getYRot(), player.getXRot(), false);

                float finisherDmg = 900.0f + (strength * 4.0f);
                target.hurtServer(level, level.damageSources().playerAttack(player), finisherDmg);

                // MC 26.2 Physics: Finisher ricochet bounce
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                    target,
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID,
                    com.dragonblockarcanedba.DragonBlockArcaneDBA.id("brave_finisher_bounce"),
                    0.85,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
                );

                // Spawn physical 3D volumetric golden-cyan cross slash entity
                BraveCrossSlashEntity crossSlash = new BraveCrossSlashEntity(
                    level, player, targetCenter, player.getYRot(), player.getXRot(), finisherDmg, 1.25f
                );
                level.addFreshEntity(crossSlash);

                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 2.0f, 0.6f);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.8f, 1.2f);

                setCombo(stack, 0); // Reset combo
                player.sendSystemMessage(Component.literal("§6★ BRAVE FINISHER! ★"), true);
            } else {
                // Standard combo strike
                float hitDmg = 400.0f + (strength * 2.0f);
                target.hurtServer(level, level.damageSources().playerAttack(player), hitDmg);

                // Tweak B: Every 5th hit releases bonus golden energy crescent wave
                if (combo % 5 == 0) {
                    BraveSlashEntity crescent = new BraveSlashEntity(level, player, hitDmg * 1.5f);
                    crescent.setDeltaMovement(look.x * 1.8, look.y * 1.8, look.z * 1.8);
                    level.addFreshEntity(crescent);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5f, 1.4f);
                }

                // Combo particle feedback & sound
                level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 1.0f + combo * 0.08f);
                player.sendSystemMessage(Component.literal(String.format("§6Brave Power: §e%d Hits", combo)), true);
            }
        }
    }

    // --- RIGHT CLICK: Brave Sword Attack (Charged Piercing Heroic Dash) ---

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BLOCK;
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingTicks) {
        if (!level.isClientSide() && living instanceof ServerPlayer player) {
            ServerLevel serverLevel = (ServerLevel) level;
            int heldTicks = getUseDuration(stack, living) - remainingTicks;
            float chargeRatio = Math.min(1.0f, heldTicks / (float) MAX_RIGHT_CHARGE_TICKS);

            // Heroic Focus: courageous stance, armor & jump power
            player.addEffect(new MobEffectInstance(DbaEffects.HEROIC_FOCUS_HOLDER, 10, 0, false, false));

            // Manage physical 3D Brave Charge Entity
            BraveChargeEntity chargeEntity = ACTIVE_CHARGE_MAP.get(player.getUUID());
            if (chargeEntity == null || !chargeEntity.isAlive()) {
                chargeEntity = new BraveChargeEntity(serverLevel, player);
                serverLevel.addFreshEntity(chargeEntity);
                ACTIVE_CHARGE_MAP.put(player.getUUID(), chargeEntity);
            }
            chargeEntity.setChargeRatio(chargeRatio);

            if (heldTicks % 20 == 0) {
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9f, 0.8f + chargeRatio * 0.5f);
            }
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (!level.isClientSide() && living instanceof ServerPlayer player) {
            ServerLevel serverLevel = (ServerLevel) level;
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

            int heldTicks = getUseDuration(stack, living) - timeLeft;
            float chargeRatio = Math.max(0.1f, Math.min(1.0f, heldTicks / (float) MAX_RIGHT_CHARGE_TICKS));

            // Clean up 3D charge entity
            BraveChargeEntity chargeEntity = ACTIVE_CHARGE_MAP.remove(player.getUUID());
            if (chargeEntity != null && chargeEntity.isAlive()) {
                chargeEntity.discard();
            }

            if (com.dragonblockarcanedba.util.MovementLimiterHelper.isMovementImmobilized(player)) {
                player.sendSystemMessage(Component.literal("§cImmobilized! Cannot dash."), true);
                return false;
            }

            double ccMult = com.dragonblockarcanedba.util.MovementLimiterHelper.getMovementMultiplier(player);

            // High-speed piercing sword dash up to 24 blocks (scaled by CC / movement limiter)
            Vec3 start = player.position();
            Vec3 look = player.getLookAngle();
            double dashDist = (14.0 + (chargeRatio * 10.0)) * ccMult;
            Vec3 end = start.add(look.scale(dashDist));

            // Hit targets along the dash path
            AABB pathBox = new AABB(start, end).inflate(2.0 + chargeRatio * 2.5); // Tweak A: wider with charge
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                LivingEntity.class, pathBox,
                e -> e.isAlive() && e != player
            );

            // Spawn physical 3D supersonic rush flight trail connecting start to end
            Vec3 dashVec = end.subtract(start);
            float trailLen = (float) dashVec.length();
            if (trailLen > 0.1f) {
                double dx = dashVec.x, dy = dashVec.y, dz = dashVec.z;
                float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
                float pitch = (float) (-(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180.0 / Math.PI)));
                BraveRushTrailEntity trail = new BraveRushTrailEntity(
                    serverLevel, player, start.add(0, 0.8, 0), yaw, pitch, trailLen, 1.0f + chargeRatio * 0.5f
                );
                serverLevel.addFreshEntity(trail);
            }

            // Teleport player
            player.teleportTo(serverLevel, end.x, end.y, end.z, java.util.Collections.emptySet(), player.getYRot(), player.getXRot(), false);

            // Delayed heroic finishing slash: 600.0 + (chargeRatio * 1400.0) + (Strength * 4.0 * chargeRatio)
            float baseDamage = 600.0f + (chargeRatio * 1400.0f);
            float strDamage = (float) (accessor.dba$getStrength() * 4.0f * chargeRatio);
            float totalDamage = baseDamage + strDamage;

            boolean anyKilled = false;
            for (LivingEntity t : targets) {
                t.addEffect(new MobEffectInstance(com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 20, 0, false, false, false), player);
                t.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(player), totalDamage);

                // MC 26.2 Physics: Piercing dash launch and bounce
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                    t,
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID,
                    com.dragonblockarcanedba.DragonBlockArcaneDBA.id("brave_dash_bounce"),
                    0.80,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
                );
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                    t,
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID,
                    com.dragonblockarcanedba.DragonBlockArcaneDBA.id("brave_dash_drag"),
                    -0.40,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                );

                if (!t.isAlive()) {
                    anyKilled = true;
                }
            }

            // Tweak C: If any enemy killed, or max charge, trigger secondary physical radial heroic shockwave
            if (anyKilled || chargeRatio >= 0.9f) {
                float shockRadius = (float) (6.0 + chargeRatio * 2.5);
                AABB shockAoe = player.getBoundingBox().inflate(shockRadius);
                List<LivingEntity> shockTargets = serverLevel.getEntitiesOfClass(
                    LivingEntity.class, shockAoe, e -> e.isAlive() && e != player && !targets.contains(e)
                );
                for (LivingEntity st : shockTargets) {
                    st.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(player), totalDamage * 0.4f);
                }

                BraveShockwaveEntity shockwave = new BraveShockwaveEntity(serverLevel, player, player.position().add(0, 0.05, 0), shockRadius);
                serverLevel.addFreshEntity(shockwave);

                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0f, 0.9f);
            }

            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 2.0f, 0.7f);

            player.getCooldowns().addCooldown(stack, 100); // 5-second cooldown
        }
        return true;
    }

    public static void onPlayerDisconnect(UUID playerUuid) {
        BraveChargeEntity chargeEntity = ACTIVE_CHARGE_MAP.remove(playerUuid);
        if (chargeEntity != null && chargeEntity.isAlive()) {
            chargeEntity.discard();
        }
    }
}
