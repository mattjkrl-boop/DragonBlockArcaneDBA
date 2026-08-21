package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.HollowAfterimageEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Saber — Fastest pure-combat weapon: precision, mobility, counters, and rapid target-to-target chaining.
 *
 * LEFT: Blitz Flurry (Hold Left Click)
 * - Holds left click to continuously perform rapid saber strikes.
 * - Automatically acquires and chains between enemies within 3.0 blocks.
 * - Consumes 5% max Ki per successful target transition.
 * - Strikes automatically without needing manual blade model contact.
 * - Pierces through targets; targets become ~90% transparent client-side for the Saber user.
 * - Inflicts Movement Curse (level 10 root + Darkness) on chained enemies.
 * - Every 5–10 hits delivers a Stronger Slash with critical damage burst.
 * - Escalating Speed (Tweak A): Buffs attack speed progressively; grants temporary attack speed boost (2s) for ANY weapon.
 * - Releasing left click carries slightly past target and opens a 0.5s continuation buffer.
 * - Re-holding left click continues the combo to next target.
 * - Ending the combo triggers Best-Fit 3D Line Snap Finisher: snaps all chained enemies onto the straightest fit line,
 *   dealing massive finishing burst damage with neck-snap crunch, and applies a 5-second cooldown.
 *
 * RIGHT: Flash Step (Right Click / Hold Right Click)
 * - Instantly dashes horizontally or vertically in look direction (works airborne).
 * - Spawns Hollow Afterimage clone at starting position.
 * - Damages enemies along the dash path.
 * - Tweak A: Dashes behind targeted enemy if an enemy is aimed at within 16 blocks.
 * - Tweak B: 3 charges stored with 3s recharge per charge; holding repeatedly performs short dashes.
 * - Tweak C: Perfect Dodge — dodging an incoming attack within 0.75s negates damage and resets cooldown/charges.
 */
public class SaberItem extends Item {

    public static final Identifier SABER_ATTACK_SPEED_ID = DragonBlockArcaneDBA.id("saber_escalating_speed");

    // Client-side tracked entity ID currently being phased/slashed by the local player
    public static int clientPhasedEntityId = -1;

    // --- State Tracking Classes ---

    public static class BlitzSequence {
        public final List<UUID> chainedEntityUuids = new ArrayList<>();
        public final Map<UUID, Vec3> chainedLastPositions = new LinkedHashMap<>();
        public UUID currentTargetUuid = null;
        public UUID nextTargetUuid = null;
        public int totalHits = 0;
        public int nextStrongSlashThreshold = 6;
        public long lastAttackGameTime = 0;
        public long releaseGameTime = 0;
        public boolean inContinuationWindow = false;
        public Vec3 startPlayerPos = null;
        public float startYaw = 0;
        public float startPitch = 0;
        public boolean hasArrived = false;
        public Vec3 arrivedPos = null;
        public boolean waitingForRelease = false;
        public int launchTicks = 0;
    }

    public static class BlitzFinisherAnim {
        public ServerPlayer player;
        public ServerLevel level;
        public List<LivingEntity> targets = new ArrayList<>();
        public Map<LivingEntity, Vec3> startPositions = new HashMap<>();
        public Map<LivingEntity, Vec3> endPositions = new HashMap<>();
        public Vec3 centroid;
        public Vec3 direction;
        public double minT;
        public double maxT;
        public float damage;
        public int ticks = 0;
        public final int maxTicks = 10; // 0.5s float duration
    }

    public static final Map<UUID, BlitzSequence> ACTIVE_BLITZ_MAP = new ConcurrentHashMap<>();
    public static final List<BlitzFinisherAnim> ACTIVE_FINISHERS = new ArrayList<>();
    public static final Map<UUID, Long> SPEED_BUFF_EXPIRE_MAP = new ConcurrentHashMap<>();
    public static final Map<UUID, Integer> FLASH_STEP_CHARGES = new ConcurrentHashMap<>();
    public static final Map<UUID, Long> LAST_RECHARGE_TIME = new ConcurrentHashMap<>();
    public static final Map<UUID, Long> PERFECT_DODGE_EXPIRE_TIME = new ConcurrentHashMap<>();

    public SaberItem(Properties properties) {
        super(properties.attributes(createModifiers()));
    }

    private static ItemAttributeModifiers createModifiers() {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                    BASE_ATTACK_DAMAGE_ID,
                    819.0, // 1 + 819 = 820 base damage
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            )
            .add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                    BASE_ATTACK_SPEED_ID,
                    0.0, // Rapid swift strikes
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            );

        // MC 26.2 Physics: Hyper-Agile Footwork & Zero-Drag Blitz Mobility
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.FRICTION_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("saber_blitz_friction"), -0.75, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
        );
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("saber_blitz_drag"), -0.65, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
        );

        return builder.build();
    }

    // --- LEFT CLICK: Blitz Flurry ---

    public static void onBlitzTick(ServerPlayer player, ItemStack stack, int chargeTicks) {
        if (player.getCooldowns().isOnCooldown(stack)) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        if (com.dragonblockarcanedba.util.MovementLimiterHelper.isMovementImmobilized(player)) {
            ACTIVE_BLITZ_MAP.remove(player.getUUID());
            return;
        }

        UUID playerUuid = player.getUUID();
        long now = level.getGameTime();

        BlitzSequence seq = ACTIVE_BLITZ_MAP.computeIfAbsent(playerUuid, k -> new BlitzSequence());

        if (seq.waitingForRelease) {
            return; // Must release click before chaining to next!
        }

        boolean resumedFromWindow = false;
        // If player re-pressed/held during continuation window, cancel continuation timeout and continue combo!
        if (seq.inContinuationWindow) {
            resumedFromWindow = true;
            seq.inContinuationWindow = false;
            seq.hasArrived = false;
            seq.currentTargetUuid = null; // CLEAR old target to force acquiring a new one
        }

        LivingEntity currentTarget = null;
        if (seq.currentTargetUuid != null) {
            var entity = level.getEntity(seq.currentTargetUuid);
            if (entity instanceof LivingEntity living && living.isAlive()) {
                currentTarget = living;
            }
        }

        // ONLY acquire a new target if this is a fresh click (chargeTicks <= 2) or we are resuming from a continuation window!
        if (currentTarget == null && (chargeTicks <= 2 || resumedFromWindow)) {
            currentTarget = acquireTarget(player, level, seq.chainedEntityUuids, 64.0, chargeTicks <= 2);
            if (currentTarget != null) {
                // New target transition -> Requires 5% mana (Ki)
                double maxKi = PlayerStats.getMaxKi(player);
                double kiCost = maxKi * 0.05;
                com.dragonblockarcanedba.attribute.PlayerStatsAccessor accessor = (com.dragonblockarcanedba.attribute.PlayerStatsAccessor) player;
                double currentKi = accessor.dba$getCurrentKi();

                if (currentKi < kiCost) {
                    player.sendSystemMessage(Component.literal("§c✦ Not Enough Ki (5% Required to Chain)! ✦"), true);
                    return;
                }

                // Deduct 5% Ki
                accessor.dba$addKi(-kiCost);
                accessor.dba$syncStats();

                // Register to sequence
                seq.currentTargetUuid = currentTarget.getUUID();
                if (!seq.chainedEntityUuids.contains(currentTarget.getUUID())) {
                    seq.chainedEntityUuids.add(currentTarget.getUUID());
                }
                seq.chainedLastPositions.put(currentTarget.getUUID(), currentTarget.position());
                seq.nextStrongSlashThreshold = seq.totalHits + 5 + level.getRandom().nextInt(6);
                
                seq.startPlayerPos = player.position();
                seq.startYaw = player.getYRot();
                seq.startPitch = player.getXRot();
                seq.hasArrived = false;
                seq.launchTicks = 0;
                
                // Pre-calculate next target for camera pan
                LivingEntity nextTarget = findNextTarget(player, level, currentTarget, seq.chainedEntityUuids);
                if (nextTarget != null) {
                    seq.nextTargetUuid = nextTarget.getUUID();
                } else {
                    seq.nextTargetUuid = null;
                }

                // Apply localized slow-motion to all entities within 64 blocks ONCE per launch
                net.minecraft.world.phys.AABB slowBox = player.getBoundingBox().inflate(64.0);
                List<LivingEntity> slowEntities = level.getEntitiesOfClass(LivingEntity.class, slowBox, e -> e != player && e.isAlive());
                for (LivingEntity e : slowEntities) {
                    e.addEffect(new MobEffectInstance(DbaEffects.JUDGEMENT_LOCK_HOLDER, 80, 0, false, false, false));
                }

                // Sound cue for target chain lock
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.4f, 1.6f);
            }
        }

        // If we have an active target
        if (currentTarget != null && currentTarget.isAlive()) {
            seq.chainedLastPositions.put(currentTarget.getUUID(), currentTarget.position());
            
            // Final target position is directly above their head!
            Vec3 targetPos = currentTarget.position().add(0, currentTarget.getBbHeight() + 0.1, 0); 
            
            com.dragonblockarcanedba.attribute.PlayerStatsAccessor accessor = (com.dragonblockarcanedba.attribute.PlayerStatsAccessor) player;

            if (!seq.hasArrived) {
                seq.launchTicks++;
                float progress = (float) seq.launchTicks / 6.0f; // 6 ticks = exact 0.3s travel time
                progress = net.minecraft.util.Mth.clamp(progress, 0.0f, 1.0f);

                if (progress >= 1.0f) {
                    // ARRIVED!
                    seq.hasArrived = true;
                    seq.arrivedPos = targetPos;
                    player.teleportTo(targetPos.x, targetPos.y, targetPos.z);
                    player.setDeltaMovement(0, 0, 0);
                    
                    // Crowd Control: Apply Movement Curse amplifier 9 (full root + darkness) forever until snap!
                    currentTarget.addEffect(new MobEffectInstance(DbaEffects.MOVEMENT_CURSE_HOLDER, 999999, 9, false, true, false));
                    currentTarget.addEffect(new MobEffectInstance(DbaEffects.CINEMATIC_TRACKING_HOLDER, 999999, 0, false, false, false), player);
                    
                    // Damage & Slash Impact (This is caught and delayed by DelayedDamageMixin because of the Movement Curse!)
                    float baseDamage = 280.0f + (float) (accessor.dba$getStrength() * 1.8);
                    currentTarget.hurtServer(level, level.damageSources().playerAttack(player), baseDamage);
                    
                    level.playSound(null, currentTarget.getX(), currentTarget.getY(), currentTarget.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.5f);
                    
                    // Cross slash particles at arrival point
                    level.sendParticles(
                        ParticleTypes.SWEEP_ATTACK,
                        currentTarget.getX(), currentTarget.getY() + 1.0, currentTarget.getZ(), 
                        30, 0.5, 0.5, 0.5, 0.2);
                        
                    // Silver flurry particles
                    for (int i = 0; i < 8; i++) {
                        double ox = (level.getRandom().nextDouble() - 0.5) * 0.8;
                        double oy = level.getRandom().nextDouble() * currentTarget.getBbHeight();
                        double oz = (level.getRandom().nextDouble() - 0.5) * 0.8;
                        level.sendParticles(
                            new DustParticleOptions(0xFFFFFF, 1.4F),
                            currentTarget.getX() + ox, currentTarget.getY() + oy, currentTarget.getZ() + oz,
                            1, 0.0, 0.05, 0.0, 0.02
                        );
                    }
                    
                    applyEscalatingSpeedBuff(player, seq.totalHits + 1);
                    
                    seq.currentTargetUuid = null;
                    seq.inContinuationWindow = true;
                    seq.releaseGameTime = now;
                    seq.waitingForRelease = true;
                } else {
                    // LAUNCHING IN PARABOLIC ARCH
                    double startX = seq.startPlayerPos.x;
                    double startY = seq.startPlayerPos.y;
                    double startZ = seq.startPlayerPos.z;

                    double currentX = net.minecraft.util.Mth.lerp(progress, startX, targetPos.x);
                    double currentZ = net.minecraft.util.Mth.lerp(progress, startZ, targetPos.z);
                    double linearY = net.minecraft.util.Mth.lerp(progress, startY, targetPos.y);

                    double horizontalDist = Math.sqrt(Math.pow(targetPos.x - startX, 2) + Math.pow(targetPos.z - startZ, 2));
                    double heightDiff = targetPos.y - startY;
                    
                    // Calculate arch height: wider distances = wider arch. Higher target = higher peak to clear ledges.
                    double archHeight = (horizontalDist * 0.08) + (Math.max(0, heightDiff) * 0.3) + 0.5;
                    double currentY = linearY + (Math.sin(progress * Math.PI) * archHeight);
                    
                    if (!player.isSpectator()) {
                        player.noPhysics = true;
                    }
                    
                    // Forcefully teleport the player along the exact mathematical curve
                    player.teleportTo(currentX, currentY, currentZ);

                    // Camera Auto-Pan logic
                    if (seq.nextTargetUuid != null) {
                        net.minecraft.world.entity.Entity nextEntity = level.getEntity(seq.nextTargetUuid);
                        if (nextEntity != null && nextEntity.isAlive()) {
                            Vec3 lookAtPos = nextEntity.getEyePosition();
                            double dX = lookAtPos.x - currentX;
                            double dY = lookAtPos.y - currentY;
                            double dZ = lookAtPos.z - currentZ;
                            double horiz = Math.sqrt(dX * dX + dZ * dZ);
                            float targetYaw = (float) (Math.atan2(dZ, dX) * (180.0 / Math.PI)) - 90.0f;
                            float targetPitch = (float) -(Math.atan2(dY, horiz) * (180.0 / Math.PI));

                            float newYaw = net.minecraft.util.Mth.rotLerp(progress, seq.startYaw, targetYaw);
                            float newPitch = net.minecraft.util.Mth.lerp(progress, seq.startPitch, targetPitch);

                            // Sync Rotation without overriding position/velocity using PlayerLookAtPacket
                            net.minecraft.world.phys.Vec3 dir = net.minecraft.world.phys.Vec3.directionFromRotation(newPitch, newYaw);
                            net.minecraft.world.phys.Vec3 lookPos = player.getEyePosition().add(dir.scale(10.0));
                            player.connection.send(new net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket(
                                net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, lookPos.x, lookPos.y, lookPos.z
                            ));
                        }
                    }
                }
            }
        } else if (seq.inContinuationWindow && seq.arrivedPos != null) {
            // WE HAVE ARRIVED AND ARE WAITING TO CHAIN
            // Forcefully freeze the player in mid-air at the exact spot they hit the mob
            if (!player.isSpectator()) player.noPhysics = true;
            player.setDeltaMovement(0, 0, 0);
            player.teleportTo(seq.arrivedPos.x, seq.arrivedPos.y, seq.arrivedPos.z);
            
            // Keep looking at the next target if it exists
            if (seq.nextTargetUuid != null) {
                if (level.getEntity(seq.nextTargetUuid) instanceof LivingEntity nextEntity && nextEntity.isAlive()) {
                    Vec3 lookAtPos = nextEntity.getEyePosition();
                    player.connection.send(new net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket(
                        net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, lookAtPos.x, lookAtPos.y, lookAtPos.z
                    ));
                }
            }
        }
    }

    public static void onBlitzRelease(ServerPlayer player, ItemStack stack, int chargeTicks) {
        UUID playerUuid = player.getUUID();
        BlitzSequence seq = ACTIVE_BLITZ_MAP.get(playerUuid);
        if (seq == null) {
            return;
        }

        seq.waitingForRelease = false; // Allow next click to chain!

        ServerLevel level = (ServerLevel) player.level();
        
        // Only push if they didn't just arrive at a target
        if (seq.currentTargetUuid != null && !seq.hasArrived) {
            Vec3 look = player.getLookAngle();
            player.setDeltaMovement(look.scale(0.7).add(0, 0.1, 0));
            player.hurtMarked = true;
            seq.currentTargetUuid = null; // Clear so next hold can acquire
        }
        
        if (!seq.inContinuationWindow) {
            seq.inContinuationWindow = true;
            seq.releaseGameTime = level.getGameTime();
        }
    }

    private static LivingEntity acquireTarget(ServerPlayer player, ServerLevel level, List<UUID> alreadyChained, double radius, boolean strictAim) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        AABB searchBox = player.getBoundingBox().inflate(radius + 1.0);

        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> {
            if (!e.isAlive() || e == player || e.isSpectator() || e.isInvulnerable() || player.distanceTo(e) > radius + 1.2 || !player.hasLineOfSight(e)) {
                return false;
            }
            if (alreadyChained.contains(e.getUUID())) {
                return false; // NEVER target someone already chained
            }
            if (strictAim) {
                Vec3 toEntity = e.getEyePosition().subtract(eyePos).normalize();
                return look.dot(toEntity) > 0.96; // tight cone (approx 16 degrees)
            }
            return true;
        });

        if (candidates.isEmpty()) return null;

        // Sort candidates:
        // Priority 1: Unchained targets first
        // Priority 2: Alignment with player's camera look vector (dot product)
        // Priority 3: Distance
        candidates.sort((a, b) -> {
            boolean aChained = alreadyChained.contains(a.getUUID());
            boolean bChained = alreadyChained.contains(b.getUUID());
            if (aChained != bChained) {
                return aChained ? 1 : -1; // unchained first
            }

            Vec3 toA = a.getBoundingBox().getCenter().subtract(eyePos).normalize();
            Vec3 toB = b.getBoundingBox().getCenter().subtract(eyePos).normalize();
            double dotA = look.dot(toA);
            double dotB = look.dot(toB);

            if (Math.abs(dotA - dotB) > 0.05) {
                return Double.compare(dotB, dotA); // highest dot product first
            }

            return Double.compare(player.distanceToSqr(a), player.distanceToSqr(b));
        });

        return candidates.get(0);
    }

    private static LivingEntity findNextTarget(ServerPlayer player, ServerLevel level, LivingEntity currentTarget, List<UUID> alreadyChained) {
        AABB searchBox = currentTarget.getBoundingBox().inflate(64.0);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> {
            return e.isAlive() && e != player && !e.isSpectator() && !e.isInvulnerable() && e != currentTarget && !alreadyChained.contains(e.getUUID());
        });
        
        if (candidates.isEmpty()) return null;
        
        candidates.sort(Comparator.comparingDouble(a -> a.distanceToSqr(currentTarget)));
        return candidates.get(0);
    }

    private static void applyEscalatingSpeedBuff(ServerPlayer player, int totalHits) {
        AttributeInstance speedAttr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (speedAttr != null) {
            double bonus = Math.min(24.0, 6.0 + (totalHits * 0.6));
            speedAttr.removeModifier(SABER_ATTACK_SPEED_ID);
            speedAttr.addTransientModifier(new AttributeModifier(
                SABER_ATTACK_SPEED_ID,
                bonus,
                AttributeModifier.Operation.ADD_VALUE
            ));

            SPEED_BUFF_EXPIRE_MAP.put(player.getUUID(), player.level().getGameTime() + 40); // 2.0s duration
        }
    }

    private static void clearEscalatingSpeedBuff(ServerPlayer player) {
        AttributeInstance speedAttr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(SABER_ATTACK_SPEED_ID);
        }
        SPEED_BUFF_EXPIRE_MAP.remove(player.getUUID());
    }

    // --- FINISHER: Best-Fit 3D Line Snap ---

    public static void executeBestFitLineFinisher(ServerPlayer player, ServerLevel level, BlitzSequence seq, ItemStack stack) {
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
        List<LivingEntity> chainedEntities = new ArrayList<>();

        for (UUID uuid : seq.chainedEntityUuids) {
            var entity = level.getEntity(uuid);
            if (entity instanceof LivingEntity living && living.isAlive()) {
                chainedEntities.add(living);
            }
        }

        if (!chainedEntities.isEmpty()) {
            List<Vec3> positions = new ArrayList<>();
            for (LivingEntity e : chainedEntities) {
                positions.add(e.position());
            }

            Vec3 centroid;
            Vec3 direction;

            if (positions.size() == 1) {
                centroid = positions.get(0);
                direction = player.getLookAngle();
            } else if (positions.size() == 2) {
                centroid = positions.get(0).add(positions.get(1)).scale(0.5);
                direction = positions.get(1).subtract(positions.get(0)).normalize();
            } else {
                // Best-Fit 3D Line using Covariance Matrix & Power Iteration for Principal Eigenvector
                double sumX = 0, sumY = 0, sumZ = 0;
                for (Vec3 p : positions) {
                    sumX += p.x;
                    sumY += p.y;
                    sumZ += p.z;
                }
                centroid = new Vec3(sumX / positions.size(), sumY / positions.size(), sumZ / positions.size());

                // Build 3x3 covariance matrix
                double xx = 0, xy = 0, xz = 0;
                double yy = 0, yz = 0, zz = 0;
                for (Vec3 p : positions) {
                    double dx = p.x - centroid.x;
                    double dy = p.y - centroid.y;
                    double dz = p.z - centroid.z;
                    xx += dx * dx;
                    xy += dx * dy;
                    xz += dx * dz;
                    yy += dy * dy;
                    yz += dy * dz;
                    zz += dz * dz;
                }

                // Power iteration to find dominant eigenvector
                Vec3 v = positions.get(positions.size() - 1).subtract(positions.get(0));
                if (v.lengthSqr() < 1e-4) v = new Vec3(1, 0, 0);
                v = v.normalize();

                for (int iter = 0; iter < 12; iter++) {
                    double nx = xx * v.x + xy * v.y + xz * v.z;
                    double ny = xy * v.x + yy * v.y + yz * v.z;
                    double nz = xz * v.x + yz * v.y + zz * v.z;
                    Vec3 nv = new Vec3(nx, ny, nz);
                    if (nv.lengthSqr() > 1e-6) {
                        v = nv.normalize();
                    }
                }
                direction = v;
            }

            // Prepare Finisher Animation
            BlitzFinisherAnim anim = new BlitzFinisherAnim();
            anim.player = player;
            anim.level = level;
            anim.centroid = centroid;
            anim.direction = direction;
            anim.damage = 850.0f + (float) (accessor.dba$getStrength() * 4.0);

            double minT = Double.MAX_VALUE;
            double maxT = -Double.MAX_VALUE;

            for (LivingEntity e : chainedEntities) {
                Vec3 toEntity = e.position().subtract(centroid);
                double t = toEntity.dot(direction);
                minT = Math.min(minT, t);
                maxT = Math.max(maxT, t);

                Vec3 snapTarget = centroid.add(direction.scale(t));
                
                anim.targets.add(e);
                anim.startPositions.put(e, e.position());
                anim.endPositions.put(e, snapTarget);
                
                // Judgement Lock stasis during the finisher dimensional slash
                e.addEffect(new MobEffectInstance(DbaEffects.JUDGEMENT_LOCK_HOLDER, 30, 0, false, false));
            }

            if (minT > maxT) { minT = -2.0; maxT = 2.0; }
            anim.minT = minT;
            anim.maxT = maxT;
            
            ACTIVE_FINISHERS.add(anim);

            // Apply 5-second weapon cooldown (100 ticks) ONLY if we actually hit something!
            player.getCooldowns().addCooldown(stack, 100);
        }

        // Restore global tick rate (legacy safety)
        if (level.getServer() != null) {
            level.getServer().tickRateManager().setTickRate(20.0f);
        }
    }

    // --- RIGHT CLICK: Flash Step ---

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            performFlashStep(serverPlayer, stack);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingTicks) {
        if (!level.isClientSide() && living instanceof ServerPlayer player) {
            int usedTicks = getUseDuration(stack, living) - remainingTicks;
            // Holding right click repeatedly performs short Flash Steps every 7 ticks while charges remain
            if (usedTicks > 0 && usedTicks % 7 == 0) {
                int charges = getFlashStepCharges(player);
                if (charges > 0) {
                    performFlashStep(player, stack);
                }
            }
        }
    }

    public static void performFlashStep(ServerPlayer player, ItemStack stack) {
        ServerLevel level = (ServerLevel) player.level();
        UUID playerUuid = player.getUUID();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

        int charges = getFlashStepCharges(player);
        if (charges <= 0) {
            player.sendSystemMessage(Component.literal("§c✦ Flash Step: Recharging! ✦"), true);
            return;
        }

        // Drain small Ki / Stamina
        double currentKi = accessor.dba$getCurrentKi();
        if (currentKi >= 10.0) {
            accessor.dba$addKi(-10.0);
            accessor.dba$syncStats();
        } else {
            double currentStamina = accessor.dba$getCurrentStamina();
            if (currentStamina >= 10.0) {
                accessor.dba$addStamina(-10.0);
                accessor.dba$syncStats();
            }
        }

        if (com.dragonblockarcanedba.util.MovementLimiterHelper.isMovementImmobilized(player)) {
            player.sendSystemMessage(Component.literal("§cImmobilized! Cannot flash step."), true);
            return;
        }

        double ccMult = com.dragonblockarcanedba.util.MovementLimiterHelper.getMovementMultiplier(player);

        // Deduct 1 charge
        FLASH_STEP_CHARGES.put(playerUuid, charges - 1);
        LAST_RECHARGE_TIME.putIfAbsent(playerUuid, level.getGameTime());

        // Set Perfect Dodge window (15 ticks / 0.75s)
        PERFECT_DODGE_EXPIRE_TIME.put(playerUuid, level.getGameTime() + 15);

        Vec3 startPos = player.position();
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        // Tweak A: Check if an enemy is targeted within 16 blocks to dash directly behind them
        LivingEntity targetedEnemy = findFlashStepTarget(player, level, 16.0 * ccMult);
        Vec3 destination;

        if (targetedEnemy != null) {
            // Dash behind the enemy facing the enemy's back
            Vec3 enemyLook = targetedEnemy.getLookAngle();
            destination = targetedEnemy.position().subtract(enemyLook.scale(1.5));
        } else {
            // Directional dash (works horizontally & vertically, scaled by CC / movement limiter)
            double dashDistance = 7.0 * ccMult;
            Vec3 rayEnd = eyePos.add(look.scale(dashDistance + 1.0));
            BlockHitResult hit = level.clip(new ClipContext(eyePos, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

            if (hit.getType() == HitResult.Type.BLOCK) {
                // Check phasing gap past wall
                Vec3 pastWall = hit.getLocation().add(look.scale(1.5));
                BlockPos pastPos = BlockPos.containing(pastWall);
                if (level.getBlockState(pastPos).isAir() && level.getBlockState(pastPos.above()).isAir()) {
                    destination = pastWall;
                } else {
                    destination = hit.getLocation().subtract(look.scale(0.5));
                }
            } else {
                destination = startPos.add(look.scale(dashDistance));
            }
        }

        // Spawn Hollow Afterimage clone at starting position
        HollowAfterimageEntity afterimage = new HollowAfterimageEntity(level, player);
        level.addFreshEntity(afterimage);

        // Origin particles
        level.sendParticles(
            ParticleTypes.PORTAL,
            startPos.x, startPos.y + 1.0, startPos.z,
            15, 0.3, 0.5, 0.3, 0.05
        );

        // Perform instant teleport
        player.teleportTo(destination.x, destination.y, destination.z);
        if (targetedEnemy != null) {
            player.setYRot(targetedEnemy.getYRot());
            player.setXRot(targetedEnemy.getXRot());
        }

        // Destination particles & sound
        level.playSound(null, destination.x, destination.y, destination.z,
            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.2f, 1.4f);
        level.playSound(null, destination.x, destination.y, destination.z,
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5f, 1.8f);

        level.sendParticles(
            new DustParticleOptions(0x00E5FF, 1.8F),
            destination.x, destination.y + 1.0, destination.z,
            20, 0.3, 0.5, 0.3, 0.05
        );

        // Path Damage to enemies crossed during dash
        AABB pathBox = new AABB(startPos, destination).inflate(1.2);
        List<LivingEntity> crossedEnemies = level.getEntitiesOfClass(LivingEntity.class, pathBox, e -> e.isAlive() && e != player);

        float pathDamage = 400.0f + (float) (accessor.dba$getStrength() * 2.0);
        for (LivingEntity enemy : crossedEnemies) {
            enemy.hurtServer(level, level.damageSources().playerAttack(player), pathDamage);
            level.sendParticles(
                ParticleTypes.SWEEP_ATTACK,
                enemy.getX(), enemy.getY() + enemy.getBbHeight() * 0.5, enemy.getZ(),
                1, 0.0, 0.0, 0.0, 0.0
            );
        }

        player.sendSystemMessage(Component.literal("§b✦ Flash Step [" + (charges - 1) + "/3 Charges] ✦"), true);
    }

    private static LivingEntity findFlashStepTarget(ServerPlayer player, ServerLevel level, double range) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        AABB searchBox = player.getBoundingBox().inflate(range);

        List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, searchBox, e ->
            e.isAlive() && e != player && !e.isSpectator() && !e.isInvulnerable() && player.distanceTo(e) <= range
        );

        LivingEntity bestTarget = null;
        double bestDot = 0.82; // Cone threshold

        for (LivingEntity e : enemies) {
            Vec3 toEntity = e.getBoundingBox().getCenter().subtract(eyePos).normalize();
            double dot = look.dot(toEntity);
            if (dot > bestDot) {
                bestDot = dot;
                bestTarget = e;
            }
        }
        return bestTarget;
    }

    public static int getFlashStepCharges(Player player) {
        return FLASH_STEP_CHARGES.getOrDefault(player.getUUID(), 3);
    }

    public static void resetCharges(Player player) {
        FLASH_STEP_CHARGES.put(player.getUUID(), 3);
        PERFECT_DODGE_EXPIRE_TIME.remove(player.getUUID());
    }

    public static boolean isPerfectDodgeActive(Player player) {
        Long expire = PERFECT_DODGE_EXPIRE_TIME.get(player.getUUID());
        return expire != null && player.level().getGameTime() <= expire;
    }

    public static void triggerPerfectDodge(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        resetCharges(player);

        // Counter sound & particles
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 2.0f, 1.5f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 2.0f, 1.8f);

        for (int i = 0; i < 15; i++) {
            double ox = (level.getRandom().nextDouble() - 0.5) * 1.5;
            double oy = level.getRandom().nextDouble() * player.getBbHeight();
            double oz = (level.getRandom().nextDouble() - 0.5) * 1.5;
            level.sendParticles(
                new DustParticleOptions(0xFFD700, 2.0F), // Gold flash
                player.getX() + ox, player.getY() + oy, player.getZ() + oz,
                1, 0.0, 0.1, 0.0, 0.05
            );
        }

        player.sendSystemMessage(Component.literal("§6✦ PERFECT DODGE! Flash Step Charges Restored! ✦"), true);
    }

    // --- Server Tick Loop for Managing Blitz Sequences & Charge Recharging ---

    public static void tickServer(MinecraftServer server) {
        // Process Active Finishers (Floating to best fit line)
        Iterator<BlitzFinisherAnim> it = ACTIVE_FINISHERS.iterator();
        while (it.hasNext()) {
            BlitzFinisherAnim anim = it.next();
            
            // Disconnect safety: if player disconnected during finisher, restore entities and remove anim
            if (anim.player == null || anim.player.isRemoved() || !anim.player.isAlive()) {
                for (LivingEntity e : anim.targets) {
                    if (e != null && e.isAlive()) {
                        e.noPhysics = false;
                        e.removeEffect(com.dragonblockarcanedba.effect.DbaEffects.MOVEMENT_CURSE_HOLDER);
                        e.removeEffect(com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER);
                    }
                }
                it.remove();
                continue;
            }

            anim.ticks++;
            
            float progress = (float) anim.ticks / (float) anim.maxTicks;
            progress = net.minecraft.util.Mth.clamp(progress, 0.0f, 1.0f);
            
            for (LivingEntity e : anim.targets) {
                if (e == null || !e.isAlive() || e.isRemoved()) continue;
                
                Vec3 start = anim.startPositions.get(e);
                Vec3 end = anim.endPositions.get(e);
                
                if (start != null && end != null) {
                    Vec3 current = start.lerp(end, progress);
                    e.noPhysics = true;
                    e.teleportTo(current.x, current.y, current.z);
                    e.setDeltaMovement(0, 0, 0);
                    e.hurtMarked = true;
                }
            }
            
            if (anim.ticks >= anim.maxTicks) {
                // Animation finished: Trigger explosion, damage, and visual slash beam!
                ServerLevel level = anim.level;
                
                for (LivingEntity e : anim.targets) {
                    if (e == null || !e.isAlive() || e.isRemoved()) continue;
                    e.noPhysics = false;
                    
                    // Violent fracture particles at each snapped mob
                    level.sendParticles(
                        ParticleTypes.CRIT,
                        e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(),
                        20, 0.4, 0.4, 0.4, 0.2
                    );
                    
                    // Remove the crowd control effect to initiate the Cinematic Damage pop (0.5s delay)
                    e.removeEffect(com.dragonblockarcanedba.effect.DbaEffects.MOVEMENT_CURSE_HOLDER);
                    e.removeEffect(com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER);
                    
                    // Burst Finishing Damage (Neck-Snap Finisher)
                    DamageSource dmgSource = (!anim.player.isRemoved() && anim.player.isAlive())
                        ? level.damageSources().playerAttack(anim.player)
                        : level.damageSources().generic();
                    e.hurtServer(level, dmgSource, anim.damage);

                    // MC 26.2 Physics: Best-fit line snap shockwave bounce
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                        e,
                        com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID,
                        com.dragonblockarcanedba.DragonBlockArcaneDBA.id("saber_finisher_bounce"),
                        0.80,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
                    );
                }
                
                // Straight-line visual slash beam along the entire calculated best-fit line
                double lineLength = anim.maxT - anim.minT + 2.0;
                int particleSteps = Math.max(10, (int) (lineLength * 6));

                for (int i = 0; i <= particleSteps; i++) {
                    double t = (anim.minT - 1.0) + (lineLength * i / (double) particleSteps);
                    Vec3 point = anim.centroid.add(anim.direction.scale(t));
                    level.sendParticles(
                        new DustParticleOptions(0x00E5FF, 2.0F),
                        point.x, point.y + 0.5, point.z,
                        1, 0.0, 0.0, 0.0, 0.0
                    );
                    level.sendParticles(
                        new DustParticleOptions(0xFFFFFF, 1.6F),
                        point.x, point.y + 0.5, point.z,
                        1, 0.0, 0.0, 0.0, 0.0
                    );
                }

                // Violent sounds: neck snap crunch + thunder clap + sweep
                level.playSound(null, anim.centroid.x, anim.centroid.y, anim.centroid.z,
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 2.0f, 1.8f);
                level.playSound(null, anim.centroid.x, anim.centroid.y, anim.centroid.z,
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 2.5f, 0.6f);
                level.playSound(null, anim.centroid.x, anim.centroid.y, anim.centroid.z,
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.5f, 1.4f);

                if (!anim.player.isRemoved()) {
                    anim.player.sendSystemMessage(Component.literal("§b✦ BLITZ FINISHER: " + anim.targets.size() + " Enemies Snapped! ✦"), true);
                }
                
                it.remove();
            }
        }

        for (ServerLevel level : server.getAllLevels()) {
            long now = level.getGameTime();

            for (ServerPlayer player : level.players()) {
                UUID playerUuid = player.getUUID();

                // 1. Manage Flash Step Charge Recharging (1 charge every 60 ticks / 3s)
                int currentCharges = FLASH_STEP_CHARGES.getOrDefault(playerUuid, 3);
                if (currentCharges < 3) {
                    long lastRecharge = LAST_RECHARGE_TIME.getOrDefault(playerUuid, now);
                    if (now - lastRecharge >= 60) {
                        FLASH_STEP_CHARGES.put(playerUuid, currentCharges + 1);
                        LAST_RECHARGE_TIME.put(playerUuid, now);
                    }
                }

                // 2. Clear expired Escalating Speed Buffs
                Long speedExpire = SPEED_BUFF_EXPIRE_MAP.get(playerUuid);
                if (speedExpire != null && now > speedExpire) {
                    clearEscalatingSpeedBuff(player);
                }

                // 3. Manage Blitz Flurry continuation expiration & Finisher trigger
                BlitzSequence seq = ACTIVE_BLITZ_MAP.get(playerUuid);
                if (seq != null) {
                    if (!(player.getMainHandItem().getItem() instanceof SaberItem)) {
                        ItemStack mainStack = player.getMainHandItem();
                        executeBestFitLineFinisher(player, level, seq, mainStack);
                        ACTIVE_BLITZ_MAP.remove(playerUuid);
                        if (!player.isSpectator() && player.noPhysics) {
                            player.noPhysics = false;
                        }
                    } else if (seq.inContinuationWindow) {
                        if (now - seq.releaseGameTime > 20) {
                            // Continuation buffer (20 ticks / 1.0s at normal speed) expired without re-holding!
                            ItemStack mainStack = player.getMainHandItem();
                            executeBestFitLineFinisher(player, level, seq, mainStack);
                            ACTIVE_BLITZ_MAP.remove(playerUuid);
                            if (!player.isSpectator() && player.noPhysics) {
                                player.noPhysics = false;
                            }
                        }
                    }
                } else {
                    if (!player.isSpectator() && player.noPhysics) {
                        player.noPhysics = false;
                    }
                }
            }
        }
    }

    public static void onPlayerDisconnect(UUID playerUuid) {
        ACTIVE_BLITZ_MAP.remove(playerUuid);
        SPEED_BUFF_EXPIRE_MAP.remove(playerUuid);
        FLASH_STEP_CHARGES.remove(playerUuid);
        LAST_RECHARGE_TIME.remove(playerUuid);
        PERFECT_DODGE_EXPIRE_TIME.remove(playerUuid);
        ACTIVE_FINISHERS.removeIf(anim -> anim.player != null && anim.player.getUUID().equals(playerUuid));
    }
}
