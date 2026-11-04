package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.EvilSpearChargeEntity;
import com.dragonblockarcanedba.entity.EvilSpearProjectileEntity;
import com.dragonblockarcanedba.entity.HellHuntImpactEntity;
import com.dragonblockarcanedba.movement.MovementPhysics;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

import com.dragonblockarcanedba.util.DbaDamage;
import com.dragonblockarcanedba.util.WeaponDrainHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Evil Spear: Cursed Hunter Weapon.
 * 
 * Ki Drain: 45% per minute (smooth).
 */
public class EvilSpearItem extends Item implements com.dragonblockarcanedba.item.geo.DbaGeoWeapon {
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
                    749.0, // 1 + 749 = 750 base damage
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            )
            .add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                    BASE_ATTACK_SPEED_ID,
                    -1.5, // Rapid piercing spear thrusts
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            );

        // MC 26.2 Stealth: Predatory Camouflage
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.NAME_PLATE_DIST_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("evil_spear_nameplate_stealth"), -48.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
        );
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("evil_spear_air_drag"), -0.40, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
        );

        return builder.build();
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player && !player.level().isClientSide()) {
            WeaponDrainHelper.drainKiDiscrete(player, 45.0, 8);
            // Melee did not even mark: the spear could not set up its own Hell Hunt chain.
            target.addEffect(new MobEffectInstance(com.dragonblockarcanedba.effect.DbaEffects.MARKED_BY_EVIL_HOLDER, 120, 0, false, true), player);
            // The dread rides the mark at the same duration: darkness and
            // the watching eyes for exactly as long as the mark lasts.
            target.addEffect(new MobEffectInstance(com.dragonblockarcanedba.effect.DbaEffects.EVIL_DREAD_HOLDER, 120, 0, false, true), player);
        }
    }

    // --- LEFT CLICK: Evil Impale (Charged Spectral Spear Throw) ---

    public static void onLeftClickChargeTick(ServerPlayer player, ItemStack stack, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        // Drain Ki: 45% per minute (smooth)
        if (!WeaponDrainHelper.drainKiPerTick(player, 45.0)) {
            onLeftClickRelease(player, stack, chargeTicks);
            return;
        }
        if (chargeTicks % 5 == 0) {
            accessor.dba$syncStats();
        }

        MovementPhysics.dampenHorizontal(player, 0.5);

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
                com.dragonblockarcanedba.sound.DbaSounds.CAST_DARK, SoundSource.PLAYERS, 0.8f, 0.6f + chargeRatio * 0.6f);
        }
    }

    public static void onLeftClickRelease(ServerPlayer player, ItemStack stack, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        // Release the charge entity into its dissipation window instead of
        // discarding it the frame the throw leaves (standard point 5).
        EvilSpearChargeEntity chargeEntity = ACTIVE_CHARGE_MAP.remove(player.getUUID());
        if (chargeEntity != null && chargeEntity.isAlive()) {
            chargeEntity.beginRelease();
        }
        float chargeRatio = Math.max(0.1f, Math.min(1.0f, chargeTicks / (float) MAX_LEFT_CHARGE_TICKS));

        // The build-up ends in a base burst at the caster's feet, so six
        // seconds of gathering resolves into the throw instead of a cut.
        com.dragonblockarcanedba.util.LayeredParticles.emitGroundBurst(level, player.position(),
            1.2f + chargeRatio * 1.3f, HellHuntImpactEntity.THEME_ARGB);

        // Damage: 450.0 + (chargeRatio * 700.0) + (Strength * 3.0 * chargeRatio)
        float baseDmg = 450.0f + (chargeRatio * 700.0f);
        float strBonus = (float) (accessor.dba$getStrength() * 3.0f * chargeRatio);
        float totalDamage = baseDmg + strBonus;

        Vec3 look = player.getLookAngle();
        double speed = 1.8 + (chargeRatio * 1.8);

        EvilSpearProjectileEntity spear = new EvilSpearProjectileEntity(level, player, totalDamage);
        spear.setDeltaMovement(look.scale(speed));
        level.addFreshEntity(spear);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            com.dragonblockarcanedba.sound.DbaSounds.TRIDENT_THROW, SoundSource.PLAYERS, 1.8f, 0.7f);

        // Cooldown floor is the projectile's 40 tick visual lifetime
        // (standard numbers guidance), scaling up with the charge.
        int cooldown = 40 + (int) (chargeRatio * 20);
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

            // The immobilized block used to sit after the Ki drain, so a
            // blocked hunt still charged the player. Ki is now only charged
            // once the move is known to fire (standard numbers guidance).
            if (com.dragonblockarcanedba.util.MovementLimiterHelper.isMovementImmobilized(serverPlayer)) {
                serverPlayer.sendSystemMessage(Component.literal("§cImmobilized! Cannot rush."), true);
                return InteractionResult.FAIL;
            }

            // Drain Ki: 45% per minute for 40-tick cooldown cycle (1.5% Max Ki)
            if (!WeaponDrainHelper.drainKiDiscrete(serverPlayer, 45.0, 40)) {
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
                MovementPhysics.setVelocity(serverPlayer, new Vec3(look.x * 2.0 * ccMult, 0.4 * ccMult, look.z * 2.0 * ccMult));
                serverLevel.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    com.dragonblockarcanedba.sound.DbaSounds.SWING_LIGHT, SoundSource.PLAYERS, 1.5f, 0.5f);
                serverLevel.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    com.dragonblockarcanedba.sound.DbaSounds.SLASH_WHOOSH, SoundSource.PLAYERS, 1.5f, 0.5f);
                serverPlayer.getCooldowns().addCooldown(stack, 40);
                PENDING_LUNGES.put(serverPlayer.getUUID(), serverLevel.getGameTime() + 8);
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
            // Line of sight is required: the hunt used to lock on through
            // walls and then teleport the player onto a target it could not
            // see, striking the ground on the far side of solid terrain.
            if (dot > bestScore && player.distanceTo(e) <= maxDist && player.hasLineOfSight(e)) {
                bestScore = dot;
                best = e;
            }
        }
        return best;
    }

    private static void performHellHunt(ServerPlayer player, ItemStack stack, LivingEntity initialTarget) {
        ServerLevel level = (ServerLevel) player.level();

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

            // Nearest first: the chain used to take whatever the entity query returned first.
            final LivingEntity chainFrom = current;
            nearbyMarked.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(chainFrom)));
            LivingEntity next = nearbyMarked.get(0);
            chainTargets.add(next);
            visited.add(next.getId());
            current = next;
        }

        // The whole chain used to execute in a single tick: every teleport, hit, and
        // impact landed in one frame. Steps land every 6 ticks (first step immediate),
        // processed by tickServer, so each impact's 35 tick visual gets room to
        // read before the next strike lands instead of stacking into one field.
        PendingHunt hunt = new PendingHunt();
        for (LivingEntity t : chainTargets) {
            hunt.targetUuids.add(t.getUUID());
        }
        hunt.nextStepTick = level.getGameTime();
        PENDING_HUNTS.put(player.getUUID(), hunt);
        // Cooldown starts with the hunt so a second hunt cannot overwrite the pending one.
        // 160 ticks covers the full 8 step chain (42 ticks) plus the final
        // impact's 50 tick life with room to spare, so hunts never overlap.
        player.getCooldowns().addCooldown(stack, 160);
    }

    private static class PendingHunt {
        final java.util.List<UUID> targetUuids = new java.util.ArrayList<>();
        int index = 0;
        long nextStepTick = 0;
    }

    private static final java.util.Map<UUID, PendingHunt> PENDING_HUNTS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<UUID, Long> PENDING_LUNGES = new java.util.concurrent.ConcurrentHashMap<>();

    /** Advances staggered Hell Hunt chains and lunge landings. Called from the main server tick. */
    public static void tickServer(net.minecraft.server.MinecraftServer server) {
        if (!PENDING_HUNTS.isEmpty()) {
            java.util.Iterator<java.util.Map.Entry<UUID, PendingHunt>> it = PENDING_HUNTS.entrySet().iterator();
            while (it.hasNext()) {
                java.util.Map.Entry<UUID, PendingHunt> entry = it.next();
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player == null) {
                    it.remove();
                    continue;
                }
                ServerLevel level = (ServerLevel) player.level();
                PendingHunt hunt = entry.getValue();
                if (level.getGameTime() < hunt.nextStepTick) continue;

                if (hunt.index >= hunt.targetUuids.size()) {
                    // Chain complete: Demon Surge lands at the end (cooldown started with the hunt).
                    player.addEffect(new MobEffectInstance(DbaEffects.DEMON_SURGE_HOLDER, 40, 0, false, false));
                    it.remove();
                    continue;
                }

                int i = hunt.index++;
                hunt.nextStepTick = level.getGameTime() + 6;
                var entity = level.getEntity(hunt.targetUuids.get(i));
                if (entity instanceof LivingEntity t && t.isAlive()) {
                    executeHuntStep(player, level, t, i, hunt.targetUuids.size());
                }
            }
        }

        if (!PENDING_LUNGES.isEmpty()) {
            java.util.Iterator<java.util.Map.Entry<UUID, Long>> it = PENDING_LUNGES.entrySet().iterator();
            while (it.hasNext()) {
                java.util.Map.Entry<UUID, Long> entry = it.next();
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player == null) {
                    it.remove();
                    continue;
                }
                ServerLevel level = (ServerLevel) player.level();
                if (level.getGameTime() < entry.getValue()) continue;
                it.remove();

                // The no-target lunge used to have no end state at all. It now lands:
                // enemies at the landing point take a reduced hunt hit and an impact marker.
                PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
                float lungeDamage = 200.0f + (float) (accessor.dba$getStrength() * 1.0f);
                java.util.List<LivingEntity> landed = level.getEntitiesOfClass(
                    LivingEntity.class, player.getBoundingBox().inflate(3.0),
                    e -> e.isAlive() && e != player
                );
                for (LivingEntity victim : landed) {
                    victim.hurtServer(level, DbaDamage.hellfire(player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (lungeDamage)));
                    victim.addEffect(new MobEffectInstance(com.dragonblockarcanedba.effect.DbaEffects.MARKED_BY_EVIL_HOLDER, 120, 0, false, true), player);
                    victim.addEffect(new MobEffectInstance(com.dragonblockarcanedba.effect.DbaEffects.EVIL_DREAD_HOLDER, 120, 0, false, true), player);
                }
                if (!landed.isEmpty()) {
                    HellHuntImpactEntity impact = new HellHuntImpactEntity(
                        level, player, player.position().add(0, 0.05, 0), 2.6f, false
                    );
                    level.addFreshEntity(impact);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), com.dragonblockarcanedba.sound.DbaSounds.CRIT_HIT, SoundSource.PLAYERS, 1.6f, 0.5f);
                }
                // The landing itself cuts the ground: a short fissure along
                // the lunge line (length 4, one deep, WEAK), whether or not
                // the lunge found a victim.
                net.minecraft.core.BlockPos lungeAir = com.dragonblockarcanedba.util.PermanentDestruction.findSurface(
                    level, (int) Math.floor(player.getX()), (int) Math.floor(player.getZ()), (int) Math.floor(player.getY()));
                if (lungeAir != null) {
                    com.dragonblockarcanedba.util.PermanentDestruction.fissure(level, lungeAir, player.getLookAngle(),
                        4.0, 1, 1, false,
                        com.dragonblockarcanedba.util.PermanentDestruction.TIER_WEAK, 24);
                }
            }
        }
    }

    private static void executeHuntStep(ServerPlayer player, ServerLevel level, LivingEntity t, int i, int chains) {
        {
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
            boolean isFinal = (i == chains - 1);

            // Calculate damage: each chain adds +15% damage (Tweak C)
            float multiplier = 1.0f + (i * 0.15f);
            float baseDmg = isFinal
                ? (400.0f + (chains * 150.0f) + (float) (accessor.dba$getStrength() * 3.5f))
                : (350.0f + (float) (accessor.dba$getStrength() * 2.0f));
            float finalDmg = baseDmg * multiplier;

            // Teleport next to target
            MovementPhysics.teleport(player, t.position());

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

            t.hurtServer(level, DbaDamage.hellfire(player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (finalDmg)));

            // Spawn Physical 3D Hell Hunt Impact Entity (Tectonic Fissures, Obsidian Spikes, Execution Pillar)
            HellHuntImpactEntity impact = new HellHuntImpactEntity(
                level, player, t.position().add(0, 0.05, 0), isFinal ? 4.5f : 3.0f, isFinal
            );
            level.addFreshEntity(impact);

            level.playSound(null, t.getX(), t.getY(), t.getZ(), com.dragonblockarcanedba.sound.DbaSounds.CRIT_HIT, SoundSource.PLAYERS, 1.8f, 0.5f);
            level.playSound(null, t.getX(), t.getY(), t.getZ(), com.dragonblockarcanedba.sound.DbaSounds.BLADE_SHING, SoundSource.PLAYERS, 1.8f, 0.5f);
        }
    }

    /** Discards any charge or stance entities still tracked for a player who logged out. */
    public static void onPlayerDisconnect(UUID playerUuid) {
        net.minecraft.world.entity.Entity e0 = ACTIVE_CHARGE_MAP.remove(playerUuid);
        if (e0 != null && e0.isAlive()) e0.discard();
        // Hunts and lunges used to survive disconnect cleanup while the
        // charge map did not; all three static maps now clear together.
        PENDING_HUNTS.remove(playerUuid);
        PENDING_LUNGES.remove(playerUuid);
    }
}
