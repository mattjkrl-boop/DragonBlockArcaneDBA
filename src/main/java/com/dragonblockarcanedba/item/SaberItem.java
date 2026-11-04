package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.BoltEntity;
import com.dragonblockarcanedba.entity.HollowAfterimageEntity;
import com.dragonblockarcanedba.entity.SaberDodgeSparkEntity;
import com.dragonblockarcanedba.entity.SaberDimensionalLineSlashEntity;
import com.dragonblockarcanedba.entity.SaberSlashEntity;
import com.dragonblockarcanedba.entity.SaberVoidTearEntity;
import com.dragonblockarcanedba.movement.MovementPhysics;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.util.DebrisSpawner;
import com.dragonblockarcanedba.util.LayeredParticles;
import com.dragonblockarcanedba.util.WeaponDrainHelper;
import net.minecraft.core.BlockPos;
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
 * Saber: High-Speed Phasing & Blitz Chain Weapon.
 * 
 * Drain: Left click Stamina 100% per minute, Right click Ki 75% per minute.
 */
public class SaberItem extends Item implements com.dragonblockarcanedba.item.geo.DbaGeoWeapon {

    public static final Identifier SABER_ATTACK_SPEED_ID = DragonBlockArcaneDBA.id("saber_escalating_speed");

    // Client-side tracked entity ID currently being phased/slashed by the local player
    public static int clientPhasedEntityId = -1;

    // --- State Tracking Classes ---

    /** Saber theme color for the shared layered particle blocks: electric cyan. */
    public static final int SABER_CYAN = 0xFF4DD8FF;

    public static class BlitzSequence {
        public final List<UUID> chainedEntityUuids = new ArrayList<>();
        public final Map<UUID, Vec3> chainedLastPositions = new LinkedHashMap<>();
        public UUID currentTargetUuid = null;
        public UUID nextTargetUuid = null;
        public int totalHits = 0;
        public int nextStrongSlashThreshold = 6;
        public ServerLevel level = null;
        public int travelTicks = 6;
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
        public final int maxTicks = 18; // 0.9s float duration: the snap is the finisher telegraph
    }

    /**
     * One delayed slice of a split damage event. Big saber hits no longer land
     * in a single tick while their visual decorates: the first slice lands on
     * the hit tick and the rest land inside the visual's active window.
     */
    public static class PendingHit {
        public ServerPlayer player;
        public ServerLevel level;
        public LivingEntity target;
        public float damage;
        public long dueGameTime;
    }

    public static final Map<UUID, BlitzSequence> ACTIVE_BLITZ_MAP = new ConcurrentHashMap<>();
    public static final List<BlitzFinisherAnim> ACTIVE_FINISHERS = new ArrayList<>();
    public static final List<PendingHit> PENDING_HITS = new ArrayList<>();
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

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // The blitz and the lunge already pay their own stamina. A vanilla connect
        // used to spend another chunk and show nothing of its own.
    }

    // --- LEFT CLICK: Blitz Flurry ---

    public static void onBlitzTick(ServerPlayer player, ItemStack stack, int chargeTicks) {
        if (player.getCooldowns().isOnCooldown(stack)) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        if (com.dragonblockarcanedba.util.MovementLimiterHelper.isMovementImmobilized(player)) {
            // Clean up before dropping the sequence: chained targets hold near-permanent
            // Movement Curse and Cinematic Tracking that only the finisher used to remove.
            BlitzSequence dropped = ACTIVE_BLITZ_MAP.remove(player.getUUID());
            if (dropped != null) {
                releaseChainedTargets(level, dropped);
                clearEscalatingSpeedBuff(player);
                if (!player.isSpectator()) {
                    player.noPhysics = false;
                }
            }
            player.sendSystemMessage(Component.literal("§c✦ Immobilized! Blitz Ended. ✦"), true);
            return;
        }

        UUID playerUuid = player.getUUID();
        long now = level.getGameTime();

        BlitzSequence seq = ACTIVE_BLITZ_MAP.computeIfAbsent(playerUuid, k -> new BlitzSequence());
        seq.level = level;

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
                if (!WeaponDrainHelper.hasStamina(player)) {
                    player.sendSystemMessage(Component.literal("§c✦ Not Enough Stamina to Chain! ✦"), true);
                    return;
                }

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
                // Travel time scales with hop distance: a 2 block hop stays a
                // snap, a 64 block hop becomes a real flight instead of a cut.
                double hopDist = player.position().distanceTo(currentTarget.position());
                seq.travelTicks = Math.max(6, Math.min(16, 6 + (int) (hopDist * 0.12)));
                
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

        // No lock: a plain fencing lunge. Holding the button with nobody in front
        // used to drain stamina every tick and draw nothing.
        if (currentTarget == null && !seq.inContinuationWindow) {
            if (chargeTicks % 6 == 0) {
                if (!WeaponDrainHelper.drainStaminaDiscrete(player, 100.0, 6)) {
                    return;
                }
                com.dragonblockarcanedba.attribute.PlayerStatsAccessor lungeStats =
                    (com.dragonblockarcanedba.attribute.PlayerStatsAccessor) player;
                // The no lock fallback is deliberately weaker than a chained blitz
                // hit (280 + Strength x 1.8): a plain fencing thrust, not a chain hit.
                float lungeDamage = 200.0f + (float) (lungeStats.dba$getStrength() * 1.4);
                Vec3 eye = player.getEyePosition();
                Vec3 look = player.getLookAngle();
                AABB reach = player.getBoundingBox().expandTowards(look.scale(5.5)).inflate(0.9);
                LivingEntity struck = null;
                double best = 5.5;
                for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, reach, e -> e != player && e.isAlive())) {
                    Vec3 to = candidate.getBoundingBox().getCenter().subtract(eye);
                    double dist = to.length();
                    if (dist > 5.5 || dist < 0.2 || to.lengthSqr() < 1.0E-4) {
                        continue;
                    }
                    if (look.dot(to.normalize()) < 0.82) {
                        continue;
                    }
                    if (dist < best) {
                        best = dist;
                        struck = candidate;
                    }
                }
                if (struck != null) {
                    struck.hurtServer(level, level.damageSources().playerAttack(player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(lungeDamage));
                }
                SaberSlashEntity lunge = new SaberSlashEntity(
                    level, player, eye.add(look.scale(0.6)), player.getYRot(), player.getXRot(), 0.0f, 1.15f, false, 12
                );
                // 0.46 blocks per tick over the 12 tick life travels 5.5 blocks,
                // exactly the damage reach: the mesh no longer flies through
                // entities it cannot hurt.
                lunge.setDeltaMovement(look.scale(0.46));
                level.addFreshEntity(lunge);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    com.dragonblockarcanedba.sound.DbaSounds.BLADE_SHING, SoundSource.PLAYERS, 0.9f, 1.6f);
            }
            return;
        }

        // Drain Stamina smoothly: 100% per minute while a blitz is actually running.
        if (!WeaponDrainHelper.drainStaminaPerTick(player, 100.0)) {
            executeBestFitLineFinisher(player, level, seq, stack);
            clearEscalatingSpeedBuff(player);
            ACTIVE_BLITZ_MAP.remove(playerUuid);
            player.sendSystemMessage(Component.literal("§c✦ Out of Stamina! Blitz Ended. ✦"), true);
            return;
        }

        // If we have an active target
        if (currentTarget != null && currentTarget.isAlive()) {
            seq.chainedLastPositions.put(currentTarget.getUUID(), currentTarget.position());
            
            // Final target position is directly above their head!
            Vec3 targetPos = currentTarget.position().add(0, currentTarget.getBbHeight() + 0.1, 0); 
            
            com.dragonblockarcanedba.attribute.PlayerStatsAccessor accessor = (com.dragonblockarcanedba.attribute.PlayerStatsAccessor) player;

            if (!seq.hasArrived) {
                seq.launchTicks++;
                float progress = (float) seq.launchTicks / (float) seq.travelTicks;
                progress = net.minecraft.util.Mth.clamp(progress, 0.0f, 1.0f);

                if (progress >= 1.0f) {
                    // ARRIVED! Count the hit: the escalating speed buff and the strong-slash
                    // pattern both read totalHits, and nothing ever incremented it.
                    seq.totalHits++;
                    seq.hasArrived = true;
                    seq.arrivedPos = targetPos;
                    MovementPhysics.teleport(player, targetPos);
                    MovementPhysics.setVelocity(player, Vec3.ZERO);
                    
                    // Crowd Control: Apply Movement Curse amplifier 9 (full root + darkness) forever until snap!
                    currentTarget.addEffect(new MobEffectInstance(DbaEffects.MOVEMENT_CURSE_HOLDER, 999999, 9, false, true, false));
                    currentTarget.addEffect(new MobEffectInstance(DbaEffects.CINEMATIC_TRACKING_HOLDER, 999999, 0, false, false, false), player);

                    // Dust bursts at the target's feet where the blitz lands.
                    LayeredParticles.emitGroundBurst(level, currentTarget.position(), 1.6f, SABER_CYAN);

                    // Damage lands inside the slash visual's active window, not in
                    // one tick: 60 percent on the arrival tick, 40 percent 5 ticks
                    // later while the 16 tick crescent is at peak.
                    float baseDamage = 280.0f + (float) (accessor.dba$getStrength() * 1.8);
                    currentTarget.hurtServer(level, level.damageSources().playerAttack(player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(baseDamage * 0.6f));
                    queuePendingHit(player, level, currentTarget, baseDamage * 0.4f, 5);
                    
                    level.playSound(null, currentTarget.getX(), currentTarget.getY(), currentTarget.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.5f);
                    level.playSound(null, currentTarget.getX(), currentTarget.getY(), currentTarget.getZ(),
                        com.dragonblockarcanedba.sound.DbaSounds.BLADE_SHING, SoundSource.PLAYERS, 1.0f, 1.5f);
                    
                    // Spawn Physical 3D Saber Crescent Slashes
                    boolean isStrong = seq.totalHits >= seq.nextStrongSlashThreshold;
                    if (isStrong) {
                        seq.nextStrongSlashThreshold = seq.totalHits + 6 + level.getRandom().nextInt(6);
                    }

                    float hitYaw = player.getYRot() + (level.getRandom().nextFloat() - 0.5f) * 20.0f;
                    float hitPitch = player.getXRot();
                    float tilt1 = (level.getRandom().nextBoolean() ? 40.0f : -40.0f) + (level.getRandom().nextFloat() - 0.5f) * 15.0f;
                    Vec3 slashPos = currentTarget.position().add(0, currentTarget.getBbHeight() * 0.55, 0);

                    SaberSlashEntity slash1 = new SaberSlashEntity(
                        level, player, slashPos, hitYaw, hitPitch, tilt1, isStrong ? 1.4f : 1.0f, isStrong, 16
                    );
                    level.addFreshEntity(slash1);

                    if (isStrong || level.getRandom().nextBoolean()) {
                        float tilt2 = -tilt1;
                        SaberSlashEntity slash2 = new SaberSlashEntity(
                            level, player, slashPos, hitYaw, hitPitch, tilt2, isStrong ? 1.3f : 0.85f, false, 14
                        );
                        level.addFreshEntity(slash2);
                    }
                    
                    applyEscalatingSpeedBuff(player, seq.totalHits);
                    
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
                    MovementPhysics.teleport(player, new Vec3(currentX, currentY, currentZ));

                    // Layered wake along the flight path: because the player moves
                    // every tick, the per tick vortex emission paints a trail
                    // instead of the hop reading as a teleport cut.
                    LayeredParticles.emitVortex(level, player.position().add(0, 0.2, 0), 1.1f, 1.8f,
                        seq.launchTicks, seq.travelTicks, SABER_CYAN, DbaParticles.WIND);

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
            MovementPhysics.setVelocity(player, Vec3.ZERO);
            MovementPhysics.teleport(player, seq.arrivedPos);
            
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
            MovementPhysics.setVelocity(player, look.scale(0.7).add(0, 0.1, 0));
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

        // Sort candidates (already chained targets were filtered out above, so
        // every survivor is unchained and no chained priority branch is needed):
        // Priority 1: Alignment with player's camera look vector (dot product)
        // Priority 2: Distance
        candidates.sort((a, b) -> {
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

    /** Queue one delayed slice of a split damage event, due in delayTicks. */
    private static void queuePendingHit(ServerPlayer player, ServerLevel level, LivingEntity target, float damage, int delayTicks) {
        if (target == null || damage <= 0f) {
            return;
        }
        PendingHit hit = new PendingHit();
        hit.player = player;
        hit.level = level;
        hit.target = target;
        hit.damage = damage;
        hit.dueGameTime = level.getGameTime() + delayTicks;
        PENDING_HITS.add(hit);
    }

    /**
     * Strip the blitz crowd control from every chained target. The single
     * cleanup path for a blitz that ends without a finisher: immobilized
     * cleanup and disconnect both route through here.
     */
    private static void releaseChainedTargets(ServerLevel level, BlitzSequence seq) {
        if (level == null || seq == null) {
            return;
        }
        for (UUID chainedUuid : seq.chainedEntityUuids) {
            var chainedEntity = level.getEntity(chainedUuid);
            if (chainedEntity instanceof LivingEntity chainedLiving) {
                chainedLiving.removeEffect(DbaEffects.MOVEMENT_CURSE_HOLDER);
                chainedLiving.removeEffect(DbaEffects.CINEMATIC_TRACKING_HOLDER);
            }
        }
    }

    /**
     * The finisher's own restore path: unfreeze every snapped target and strip
     * its crowd control. The tick loop's disconnect safety and the disconnect
     * handler both call this, so no target is ever left frozen with noPhysics
     * set and no code path restoring it.
     */
    private static void restoreFinisherTargets(BlitzFinisherAnim anim) {
        for (LivingEntity e : anim.targets) {
            if (e != null && e.isAlive()) {
                e.noPhysics = false;
                e.removeEffect(DbaEffects.MOVEMENT_CURSE_HOLDER);
                e.removeEffect(DbaEffects.CINEMATIC_TRACKING_HOLDER);
            }
        }
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
            anim.damage = 1000.0f + (float) (accessor.dba$getStrength() * 4.5);

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
            // The cooldown keys by item, so the stack handed in must be a saber
            // stack: callers that end a blitz by swapping weapons pass one.
            player.getCooldowns().addCooldown(stack, 100);
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

        // The immobilized check runs BEFORE any Ki is drained: a blocked step
        // never charges. Ki is only spent when the step actually fires.
        if (com.dragonblockarcanedba.util.MovementLimiterHelper.isMovementImmobilized(player)) {
            player.sendSystemMessage(Component.literal("§cImmobilized! Cannot flash step."), true);
            return;
        }

        // Drain Ki: 75% per minute (smooth per charge cooldown cycle = 40 ticks = 2.5% Max Ki)
        if (!WeaponDrainHelper.drainKiDiscrete(player, 75.0, 40)) {
            player.sendSystemMessage(Component.literal("§c✦ Not Enough Ki for Flash Step! ✦"), true);
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

        // Spawn physical 3D void tear & sonic shock rings at origin
        SaberVoidTearEntity originTear = new SaberVoidTearEntity(
            level, player, startPos.add(0, 1.0, 0), player.getYRot(), player.getXRot(), 1.0f, false, 20
        );
        level.addFreshEntity(originTear);
        LayeredParticles.emitGroundBurst(level, startPos, 1.2f, SABER_CYAN);

        // Perform instant teleport
        MovementPhysics.teleport(player, destination);
        if (targetedEnemy != null) {
            player.setYRot(targetedEnemy.getYRot());
            player.setXRot(targetedEnemy.getXRot());
        }

        // Spawn physical 3D void tear & sonic shock rings at destination
        SaberVoidTearEntity destTear = new SaberVoidTearEntity(
            level, player, destination.add(0, 1.0, 0), player.getYRot(), player.getXRot(), 1.15f, true, 20
        );
        level.addFreshEntity(destTear);
        LayeredParticles.emitGroundBurst(level, destination, 1.2f, SABER_CYAN);

        // Destination sound
        level.playSound(null, destination.x, destination.y, destination.z,
            com.dragonblockarcanedba.sound.DbaSounds.SABER_HUM, SoundSource.PLAYERS, 0.7f, 1.3f);
        level.playSound(null, destination.x, destination.y, destination.z,
            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.2f, 1.4f);
        level.playSound(null, destination.x, destination.y, destination.z,
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5f, 1.8f);
        level.playSound(null, destination.x, destination.y, destination.z,
            com.dragonblockarcanedba.sound.DbaSounds.SLASH_WHOOSH, SoundSource.PLAYERS, 1.5f, 1.8f);

        // Path Damage to enemies crossed during dash
        AABB pathBox = new AABB(startPos, destination).inflate(1.2);
        List<LivingEntity> crossedEnemies = level.getEntitiesOfClass(LivingEntity.class, pathBox, e -> e.isAlive() && e != player);

        // A movement side effect now hits softer than a chained blitz hit, and
        // it lands inside the 20 tick tear window: 60 percent on the step tick,
        // 40 percent 5 ticks later as the tears dissipate.
        float pathDamage = 320.0f + (float) (accessor.dba$getStrength() * 1.6);
        for (LivingEntity enemy : crossedEnemies) {
            enemy.hurtServer(level, level.damageSources().playerAttack(player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(pathDamage * 0.6f));
            queuePendingHit(player, level, enemy, pathDamage * 0.4f, 5);
            Vec3 enemySlashPos = enemy.position().add(0, enemy.getBbHeight() * 0.55, 0);
            SaberSlashEntity pathSlash = new SaberSlashEntity(
                level, player, enemySlashPos, player.getYRot(), player.getXRot(), 25.0f, 1.1f, false, 14
            );
            level.addFreshEntity(pathSlash);
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
        // Strict comparison: the window is set to now + 15, so <= would read
        // active for 16 distinct game time values instead of 15.
        return expire != null && player.level().getGameTime() < expire;
    }

    public static void triggerPerfectDodge(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        resetCharges(player);

        // Counter sound & 3D geometric spark burst
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 2.0f, 1.5f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 2.0f, 1.8f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            com.dragonblockarcanedba.sound.DbaSounds.BLADE_SHING, SoundSource.PLAYERS, 2.0f, 1.8f);

        Vec3 sparkPos = player.position().add(0, player.getBbHeight() * 0.55, 0);
        SaberDodgeSparkEntity dodgeSpark = new SaberDodgeSparkEntity(
            level, player, sparkPos, 1.25f, 20
        );
        level.addFreshEntity(dodgeSpark);

        player.sendSystemMessage(Component.literal("§6✦ PERFECT DODGE! Flash Step Charges Restored! ✦"), true);
    }

    // --- Server Tick Loop for Managing Blitz Sequences & Charge Recharging ---

    public static void tickServer(MinecraftServer server) {
        // Process due slices of split damage events first: each lands inside
        // its visual's active window, after the hit tick that opened it.
        Iterator<PendingHit> hitIt = PENDING_HITS.iterator();
        while (hitIt.hasNext()) {
            PendingHit hit = hitIt.next();
            if (hit.level.getGameTime() >= hit.dueGameTime) {
                if (hit.target != null && hit.target.isAlive() && !hit.target.isRemoved()) {
                    DamageSource src = (hit.player != null && !hit.player.isRemoved() && hit.player.isAlive())
                        ? hit.level.damageSources().playerAttack(hit.player)
                        : hit.level.damageSources().generic();
                    hit.target.hurtServer(hit.level, src, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(hit.damage));
                }
                hitIt.remove();
            }
        }

        // Process Active Finishers (Floating to best fit line)
        Iterator<BlitzFinisherAnim> it = ACTIVE_FINISHERS.iterator();
        while (it.hasNext()) {
            BlitzFinisherAnim anim = it.next();

            // Disconnect safety: if player disconnected during finisher, restore entities and remove anim
            if (anim.player == null || anim.player.isRemoved() || !anim.player.isAlive()) {
                restoreFinisherTargets(anim);
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
                    com.dragonblockarcanedba.util.DbaCompat.markHurt(e);
                }
            }
            
            if (anim.ticks >= anim.maxTicks) {
                // Animation finished: Trigger explosion, damage, and visual slash beam!
                ServerLevel level = anim.level;
                
                // Spawn physical 3D dimensional line cut entity along calculated best-fit line!
                double lineLength = Math.max(8.0, (anim.maxT - anim.minT) + 4.0);
                SaberDimensionalLineSlashEntity lineSlash = new SaberDimensionalLineSlashEntity(
                    level, anim.player, anim.centroid, anim.direction, (float) lineLength, 1.2f, 34
                );
                level.addFreshEntity(lineSlash);

                // World interaction at the payoff: a branched bolt where the
                // thunder sounds already promised one, a helical debris field
                // of the ground the line is standing on, and base dust.
                Vec3 feet = new Vec3(anim.centroid.x, Math.floor(anim.centroid.y), anim.centroid.z);
                level.addFreshEntity(new BoltEntity(level, feet, level.getRandom().nextInt(), 0xFFBFEFFF, 14.0f, 8));
                int debrisCount = Math.max(8, Math.round(28f * com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity()));
                DebrisSpawner.spawnField(level, anim.player, feet, debrisCount,
                    Math.min(6.0, lineLength * 0.5), 3.5, 26);
                LayeredParticles.emitGroundBurst(level, feet, 2.2f, SABER_CYAN);

                for (LivingEntity e : anim.targets) {
                    if (e == null || !e.isAlive() || e.isRemoved()) continue;
                    e.noPhysics = false;

                    // Spawn physical 3D micro cross-burst slash on each snapped mob
                    Vec3 mobSlashPos = e.position().add(0, e.getBbHeight() * 0.5, 0);
                    SaberSlashEntity mobSlash1 = new SaberSlashEntity(
                        level, anim.player, mobSlashPos, 0, 0, 45.0f, 1.3f, true, 18
                    );
                    level.addFreshEntity(mobSlash1);

                    SaberSlashEntity mobSlash2 = new SaberSlashEntity(
                        level, anim.player, mobSlashPos, 0, 0, -45.0f, 1.1f, false, 16
                    );
                    level.addFreshEntity(mobSlash2);

                    // Remove the crowd control effect to initiate the Cinematic Damage pop (0.5s delay)
                    e.removeEffect(com.dragonblockarcanedba.effect.DbaEffects.MOVEMENT_CURSE_HOLDER);
                    e.removeEffect(com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER);

                    // Finisher damage spreads across the beam's 34 tick active
                    // window: 50 percent as the beam ignites (the 18 tick snap
                    // was the telegraph), 30 percent at beam tick 8, 20 percent
                    // at beam tick 16.
                    DamageSource dmgSource = (!anim.player.isRemoved() && anim.player.isAlive())
                        ? level.damageSources().playerAttack(anim.player)
                        : level.damageSources().generic();
                    e.hurtServer(level, dmgSource, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(anim.damage * 0.5f));
                    queuePendingHit(anim.player, level, e, anim.damage * 0.3f, 8);
                    queuePendingHit(anim.player, level, e, anim.damage * 0.2f, 16);

                    // MC 26.2 Physics: Best-fit line snap shockwave bounce. The
                    // request now matches the helper's clamp exactly: the timed
                    // modifier caps bounciness at 0.30, so 0.30 is what is asked
                    // for and 0.30 is what the target gets, for 20 ticks.
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                        e,
                        com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID,
                        com.dragonblockarcanedba.DragonBlockArcaneDBA.id("saber_finisher_bounce"),
                        0.30,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
                    );
                }

                com.dragonblockarcanedba.fx.DbaImpactFx.heavy(level, anim.centroid.x, anim.centroid.y, anim.centroid.z);

                // Violent sounds: neck snap crunch + thunder clap + sweep
                level.playSound(null, anim.centroid.x, anim.centroid.y, anim.centroid.z,
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 2.0f, 1.8f);
                level.playSound(null, anim.centroid.x, anim.centroid.y, anim.centroid.z,
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 2.5f, 0.6f);
                level.playSound(null, anim.centroid.x, anim.centroid.y, anim.centroid.z,
                    com.dragonblockarcanedba.sound.DbaSounds.BLADE_SHING, SoundSource.PLAYERS, 2.5f, 0.6f);
                level.playSound(null, anim.centroid.x, anim.centroid.y, anim.centroid.z,
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.5f, 1.4f);
                level.playSound(null, anim.centroid.x, anim.centroid.y, anim.centroid.z,
                    com.dragonblockarcanedba.sound.DbaSounds.LIGHTNING_ZAP, SoundSource.WEATHER, 1.5f, 1.4f);

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
                        // Ending a blitz by swapping weapons: the finisher
                        // cooldown belongs on the saber, not on the swapped to
                        // item. Hand the finisher a saber stack (the offhand if
                        // the saber is there, otherwise a bare saber stack; the
                        // cooldown keys by item, not by stack instance).
                        ItemStack saberStack = player.getOffhandItem().getItem() instanceof SaberItem
                            ? player.getOffhandItem()
                            : new ItemStack(com.dragonblockarcanedba.item.DbaItems.SABER);
                        executeBestFitLineFinisher(player, level, seq, saberStack);
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
        // A disconnect mid blitz frees the chained targets through the same
        // cleanup path as an immobilized end, instead of leaving the near
        // permanent Movement Curse and Cinematic Tracking on them forever.
        BlitzSequence dropped = ACTIVE_BLITZ_MAP.remove(playerUuid);
        if (dropped != null) {
            releaseChainedTargets(dropped.level, dropped);
        }
        SPEED_BUFF_EXPIRE_MAP.remove(playerUuid);
        FLASH_STEP_CHARGES.remove(playerUuid);
        LAST_RECHARGE_TIME.remove(playerUuid);
        PERFECT_DODGE_EXPIRE_TIME.remove(playerUuid);
        PENDING_HITS.removeIf(hit -> hit.player != null && hit.player.getUUID().equals(playerUuid));
        // A disconnect mid finisher restores the snapped targets through the
        // finisher's own restore path (unfreeze, strip crowd control) before
        // the animation is dropped, so nothing is left frozen with noPhysics.
        Iterator<BlitzFinisherAnim> finIt = ACTIVE_FINISHERS.iterator();
        while (finIt.hasNext()) {
            BlitzFinisherAnim anim = finIt.next();
            if (anim.player != null && anim.player.getUUID().equals(playerUuid)) {
                restoreFinisherTargets(anim);
                finIt.remove();
            }
        }
    }
}
