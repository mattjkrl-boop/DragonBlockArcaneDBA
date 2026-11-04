package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.effect.DbaEffects;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;

import java.util.Comparator;
import java.util.List;

/**
 * Curse Chain Entity: Physical spectral chain projected by Curse Blade.
 * 
 * - Homes in on target enemies.
 * - Attackable and destructible by victim or allies before or after attaching (~60 HP).
 * - Applies and increments Movement Curse (10 max stacks).
 * - Stacks last 5s and reset on hit; breaking a chain removes a stack.
 * - Tweak A: Jumps to nearby targets if current target is maxed (10 chains).
 * - Tweak C: Pulls heavily cursed enemies toward user and tightly binds their body.
 */
public class CurseChainEntity extends Projectile implements ITrackedSwarmEntity {
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(CurseChainEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_ATTACHED = SynchedEntityData.defineId(CurseChainEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ORBIT_INDEX = SynchedEntityData.defineId(CurseChainEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ATTACH_REMAINING = SynchedEntityData.defineId(CurseChainEntity.class, EntityDataSerializers.INT);

    /** How long an attached chain binds its target, in ticks. */
    public static final int ATTACH_LIFE_TICKS = 100;

    private float health = 60.0f;
    private LivingEntity target = null;
    private int orbitIndex = 0;
    private int attachTimer = 0;

    public CurseChainEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public CurseChainEntity(Level level, LivingEntity owner, LivingEntity target, int orbitIndex) {
        super(DbaEntities.CURSE_CHAIN, level);
        this.setOwner(owner);
        this.target = target;
        this.orbitIndex = orbitIndex;
        this.noPhysics = true;

        Vec3 start = owner.getEyePosition();
        this.setPos(start.x, start.y - 0.2, start.z);

        this.entityData.set(TARGET_ID, target != null ? target.getId() : -1);
        this.entityData.set(IS_ATTACHED, false);
        this.entityData.set(ORBIT_INDEX, orbitIndex);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TARGET_ID, -1);
        builder.define(IS_ATTACHED, false);
        builder.define(ORBIT_INDEX, 0);
        builder.define(ATTACH_REMAINING, -1);
    }

    /** Ticks of bind left, or -1 while the chain is still flying. Synced so
     *  the renderer can fade the chain out instead of popping at discard. */
    public int getAttachRemaining() {
        return this.entityData.get(ATTACH_REMAINING);
    }

    /** (Re)applies the chain's friction and air drag weight for a curse
     *  amplifier. DbaPhysicsAttributes.applyModifier is a 20 tick timed
     *  modifier, so this must be refreshed while the chain holds or the
     *  grounding silently stops 20 ticks into a 100 tick bind. */
    private void applyChainWeight(LivingEntity target, int amp) {
        double chainWeight = 1.0 + (amp * 0.8);
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
            target,
            com.dragonblockarcanedba.util.DbaPhysicsAttributes.FRICTION_ID,
            com.dragonblockarcanedba.DragonBlockArcaneDBA.id("curse_chain_friction"),
            chainWeight,
            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
            target,
            com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID,
            com.dragonblockarcanedba.DragonBlockArcaneDBA.id("curse_chain_drag"),
            chainWeight,
            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );
    }

    public boolean isAttached() {
        return this.entityData.get(IS_ATTACHED);
    }

    public int getTargetId() {
        return this.entityData.get(TARGET_ID);
    }

    public int getOrbitIndex() {
        return this.entityData.get(ORBIT_INDEX);
    }

    @Override
    public int getSwarmIndex() {
        return this.orbitIndex;
    }

    @Override
    public float getSwarmHealth() {
        return this.health;
    }

    @Override
    public void setSwarmHealth(float health) {
        this.health = health;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!this.isRemoved()) {
            this.health -= amount;

            if (this.health <= 0) {
                // Remove one stack from the target if attached
                if (isAttached() && target != null && target.isAlive()) {
                    MobEffectInstance cur = target.getEffect(DbaEffects.MOVEMENT_CURSE_HOLDER);
                    if (cur != null) {
                        int amp = cur.getAmplifier();
                        target.removeEffect(DbaEffects.MOVEMENT_CURSE_HOLDER);
                        if (amp > 0) {
                            target.addEffect(new MobEffectInstance(DbaEffects.MOVEMENT_CURSE_HOLDER, cur.getDuration(), amp - 1, false, true));
                        }
                    }
                }
                this.discard();
            }
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) return;

        ServerLevel serverLevel = (ServerLevel) this.level();
        Entity owner = this.getOwner();

        if (owner == null || !owner.isAlive()) {
            this.discard();
            return;
        }

        // Theme particles (T5): void smoke wisps shedding off the chain while
        // it flies and while it holds its target.
        if (this.tickCount % 4 == 0) {
            float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
            if (density > 0f) {
                serverLevel.sendParticles(
                    com.dragonblockarcanedba.particle.DbaParticles.VOID_SMOKE,
                    this.getX(), this.getY() + 0.3, this.getZ(),
                    Math.max(1, Math.round(2f * density)),
                    0.4, 0.4, 0.4, 0.02);
            }
        }

        // Resolve target if null
        if (target == null && getTargetId() != -1) {
            Entity e = serverLevel.getEntity(getTargetId());
            if (e instanceof LivingEntity le && le.isAlive()) {
                target = le;
            }
        }

        if (target == null || !target.isAlive() || this.distanceToSqr(target) > 64 * 64) {
            this.discard();
            return;
        }

        boolean attached = isAttached();

        if (!attached) {
            // Tweak A: If target already has 10 chains, jump to another nearby enemy
            List<CurseChainEntity> existingChains = serverLevel.getEntitiesOfClass(
                CurseChainEntity.class, target.getBoundingBox().inflate(3.0),
                c -> c.isAttached() && c.target == target
            );

            if (existingChains.size() >= 10) {
                // Find nearby alternative enemy within 10 blocks
                List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(
                    LivingEntity.class, target.getBoundingBox().inflate(10.0),
                    e -> e.isAlive() && e != owner && e != target
                );
                if (!nearby.isEmpty()) {
                    target = nearby.get(0);
                    this.entityData.set(TARGET_ID, target.getId());
                } else {
                    // Replace lowest health chain on current target
                    existingChains.sort(Comparator.comparingDouble(CurseChainEntity::getSwarmHealth));
                    CurseChainEntity lowest = existingChains.get(0);
                    lowest.discard();
                }
            }

            // Fly towards target
            Vec3 targetCenter = target.position().add(0, target.getBbHeight() * 0.5, 0);
            Vec3 currentPos = this.position();
            Vec3 dir = targetCenter.subtract(currentPos);

            if (dir.lengthSqr() < 1.8) {
                // Attach to target!
                this.entityData.set(IS_ATTACHED, true);
                this.attachTimer = ATTACH_LIFE_TICKS; // 5 seconds duration
                this.entityData.set(ATTACH_REMAINING, ATTACH_LIFE_TICKS);

                // Apply / increment Movement Curse
                MobEffectInstance existingEffect = target.getEffect(DbaEffects.MOVEMENT_CURSE_HOLDER);
                int newAmp = (existingEffect != null) ? Math.min(9, existingEffect.getAmplifier() + 1) : 0;
                target.addEffect(new MobEffectInstance(DbaEffects.MOVEMENT_CURSE_HOLDER, 100, newAmp, false, true), owner);

                // MC 26.2 Physics: Heavy spectral iron chains ground the target and increase drag
                applyChainWeight(target, newAmp);

                // Small magic hit damage. Was 35, which read as nothing next
                // to the blade's 750 melee and the tempest's strikes; 75 makes
                // the attach itself felt without making chains the damage.
                target.hurtServer(serverLevel, com.dragonblockarcanedba.util.DbaDamage.curse(this, (LivingEntity) owner), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (75.0f)));

                // Attach burst: sparks off the collar as the chain bites in.
                float pdensity = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
                if (pdensity > 0f) {
                    serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                        Math.max(2, Math.round(8f * pdensity)), 0.5, 0.6, 0.5, 0.06);
                }

                // Lifesteal & Blood Shield (Soul Rend Refinement)
                if (owner instanceof Player player) {
                    float healAmount = 4.0f + (newAmp * 2.5f);
                    if (player.getHealth() < player.getMaxHealth()) {
                        player.heal(healAmount);
                    } else {
                        // Max health -> Blood Shield (Direct absorption hearts)
                        player.setAbsorptionAmount(Math.min(player.getMaxHealth(), player.getAbsorptionAmount() + 4.0f));
                    }
                }

                // Tweak C: Pull target toward owner if heavily cursed (5+ stacks)
                if (newAmp >= 4) {
                    Vec3 pull = owner.position().subtract(target.position()).normalize().scale(0.7);
                    target.setDeltaMovement(target.getDeltaMovement().add(pull.x, 0.15, pull.z));
                    com.dragonblockarcanedba.util.DbaCompat.markHurt(target);

                    // Stamina / Ki Restoration (Soul Rend Refinement)
                    if (owner instanceof Player player) {
                        PlayerStatsAccessor stats = (PlayerStatsAccessor) player;
                        stats.dba$addKi(10.0);
                        stats.dba$addStamina(5.0);
                        stats.dba$syncStats();
                    }
                }
            } else {
                Vec3 step = dir.normalize().scale(1.6);
                this.setPos(currentPos.x + step.x, currentPos.y + step.y, currentPos.z + step.z);
                this.setYRot((float) (Math.atan2(step.z, step.x) * (180.0 / Math.PI)) - 90.0f);
                this.setXRot((float) (-(Math.atan2(step.y, Math.sqrt(step.x * step.x + step.z * step.z)) * (180.0 / Math.PI))));
            }
        } else {
            // Attached state: orbit/wrap tightly around target's torso/limbs
            this.attachTimer--;
            this.entityData.set(ATTACH_REMAINING, Math.max(0, this.attachTimer));
            if (this.attachTimer <= 0 || !target.hasEffect(DbaEffects.MOVEMENT_CURSE_HOLDER)) {
                this.discard();
                return;
            }

            MobEffectInstance curseNow = target.getEffect(DbaEffects.MOVEMENT_CURSE_HOLDER);

            // The friction and drag weight is a 20 tick timed modifier; refresh
            // it while the bind lasts so the grounding the chain shows is the
            // grounding the target actually feels for the full 100 ticks.
            if (curseNow != null && this.attachTimer % 10 == 0) {
                applyChainWeight(target, curseNow.getAmplifier());
            }

            // Reel-in (Tweak C, sustained): at 5+ stacks the chain keeps
            // dragging the victim toward the owner for the whole bind, not
            // only on the attach tick. Capped, so it reads as a drag, and
            // scaled by the shared weight classes so bosses barely move.
            if (curseNow != null && curseNow.getAmplifier() >= 4) {
                Vec3 toOwner = new Vec3(owner.getX() - target.getX(), 0, owner.getZ() - target.getZ());
                double dist = toOwner.length();
                if (dist > 1.5) {
                    Vec3 dirToOwner = toOwner.scale(1.0 / dist);
                    Vec3 vel = target.getDeltaMovement();
                    double toward = vel.x * dirToOwner.x + vel.z * dirToOwner.z;
                    double step = 0.05 * com.dragonblockarcanedba.util.VortexPhysics.weightScale(target);
                    if (toward < 0.30) {
                        target.setDeltaMovement(vel.add(dirToOwner.x * step, 0, dirToOwner.z * step));
                        com.dragonblockarcanedba.util.DbaCompat.markHurt(target);
                    }
                }
            }

            // Constrict tightly around target body
            double angle = (this.tickCount * 0.15) + (orbitIndex * (Math.PI * 2.0 / 10.0));
            double radius = 0.45 + (target.getBbWidth() * 0.5);
            double heightOffset = (target.getBbHeight() * 0.2) + ((orbitIndex % 5) * (target.getBbHeight() * 0.15));

            double ox = Math.cos(angle) * radius;
            double oz = Math.sin(angle) * radius;

            this.setPos(target.getX() + ox, target.getY() + heightOffset, target.getZ() + oz);
            this.setYRot((float) Math.toDegrees(angle));
            this.setXRot(0.0f);
        }
    }
}
