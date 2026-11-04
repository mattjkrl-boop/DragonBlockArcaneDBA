package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.util.DebrisSpawner;
import com.dragonblockarcanedba.util.GroundScar;
import com.dragonblockarcanedba.util.LayeredParticles;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class DimensionalSlashEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> TILT = SynchedEntityData.defineId(DimensionalSlashEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(DimensionalSlashEntity.class, EntityDataSerializers.INT);
    /** Tick at which the slash struck and stopped flying; -1 while in flight. Synced so the renderer can run the tear fade. */
    private static final EntityDataAccessor<Integer> FADE_START = SynchedEntityData.defineId(DimensionalSlashEntity.class, EntityDataSerializers.INT);

    /** Hard flight cap: at speed 2.5 this is about 250 blocks of range. The renderer's envelope fades the last 30 ticks of it. */
    public static final int MAX_LIFE_TICKS = 100;
    /** How long the torn crescent hangs in the cut it made before the renderer has faded it shut. */
    public static final int LINGER_TICKS = 14;

    /** Theme dust color for the layered trail and impact burst: the crescent's violet magenta. */
    public static final int THEME_ARGB = 0xCC26D9;

    /** Shards of cut space thrown on impact: dark void stone and crystal. */
    private static final List<BlockState> VOID_PALETTE = List.of(
        Blocks.OBSIDIAN.defaultBlockState(),
        Blocks.CRYING_OBSIDIAN.defaultBlockState(),
        Blocks.AMETHYST_BLOCK.defaultBlockState()
    );

    private float damage = 750.0f;

    public DimensionalSlashEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public DimensionalSlashEntity(Level level, LivingEntity owner, boolean tiltRight, float damage) {
        super(DbaEntities.DIMENSIONAL_SLASH, level);
        this.setOwner(owner);
        if (owner != null) {
            Vec3 eye = owner.getEyePosition();
            Vec3 look = owner.getLookAngle();
            Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
            if (right.lengthSqr() < 0.001) right = new Vec3(1, 0, 0);
            Vec3 up = right.cross(look).normalize();

            boolean isRightHanded = (owner.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            boolean isOffhand = (owner.getOffhandItem().getItem() instanceof com.dragonblockarcanedba.item.DimensionalSwordItem &&
                !(owner.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.DimensionalSwordItem));
            boolean onRight = isRightHanded ? !isOffhand : isOffhand;
            float sideSign = onRight ? 1.0f : -1.0f;

            Vec3 spawnPos = eye.add(look.scale(0.8)).add(right.scale(sideSign * 0.35)).add(up.scale(-0.25));
            this.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            this.setYRot(owner.getYRot());
            this.setXRot(owner.getXRot());
            this.entityData.set(CASTER_ID, owner.getId());
        }
        this.entityData.set(TILT, tiltRight);
        this.damage = damage;
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TILT, false);
        builder.define(CASTER_ID, -1);
        builder.define(FADE_START, -1);
    }

    public int getCasterId() {
        return this.entityData.get(CASTER_ID);
    }

    public boolean getTilt() {
        return this.entityData.get(TILT);
    }

    /** Tick the slash struck and began its lingering tear fade, or -1 while it is still flying. */
    public int getFadeStart() {
        return this.entityData.get(FADE_START);
    }

    public float getDamage() {
        return this.damage;
    }

    @Override
    public void tick() {
        super.tick();

        // Lingering tear: the slash has struck and stopped. It hangs in the cut
        // while the renderer fades it shut over LINGER_TICKS instead of popping
        // out of existence on the hit tick, shedding dissipation sparks.
        int fadeStart = this.getFadeStart();
        if (fadeStart >= 0) {
            this.setDeltaMovement(Vec3.ZERO);
            if (this.level() instanceof ServerLevel serverLevel) {
                float lingerAge = this.tickCount - fadeStart;
                LayeredParticles.emitRain(serverLevel, this.position(), 1.1f, 1.4f,
                    lingerAge, LINGER_TICKS, DbaParticles.SPARK);
                if (lingerAge >= LINGER_TICKS) {
                    this.discard();
                }
            }
            return;
        }

        if (!this.level().isClientSide() && this.tickCount == 1) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.8f, 1.3f);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 1.2f, 1.6f);
        }

        // Layered trail (point 1): a bright mote core on the flight line, a
        // theme dust mid layer hugging the crescent, and void smoke wisps
        // shedding off the edges. Replaces the flat two-type puff every 2 ticks.
        if (this.level() instanceof ServerLevel serverLevel) {
            float density = com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity();
            if (density > 0f) {
                Vec3 vel = this.getDeltaMovement();
                Vec3 side = vel.cross(new Vec3(0, 1, 0));
                if (side.lengthSqr() < 0.001) side = new Vec3(1, 0, 0);
                side = side.normalize();
                Vec3 pos = this.position();
                serverLevel.sendParticles(DbaParticles.KI_MOTE,
                    pos.x, pos.y, pos.z,
                    Math.max(1, Math.round(1f * density)), 0.05, 0.05, 0.05, 0.01);
                serverLevel.sendParticles(LayeredParticles.dust(THEME_ARGB, 1.3f),
                    pos.x, pos.y, pos.z,
                    Math.max(1, Math.round(2f * density)), 0.55, 0.28, 0.55, 0.02);
                if (this.tickCount % 2 == 0) {
                    double off = 1.1 + serverLevel.getRandom().nextDouble() * 0.5;
                    double sign = serverLevel.getRandom().nextBoolean() ? 1.0 : -1.0;
                    serverLevel.sendParticles(DbaParticles.VOID_SMOKE,
                        pos.x + side.x * off * sign, pos.y + (serverLevel.getRandom().nextDouble() - 0.5) * 0.4, pos.z + side.z * off * sign,
                        1, 0.10, 0.10, 0.10, 0.02);
                }
            }
        }

        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitResult.getType() != HitResult.Type.MISS) {
            this.onHit(hitResult);
            if (this.getFadeStart() >= 0) {
                return; // Struck this tick: the tear hangs here, no more travel.
            }
        }

        Vec3 vec3 = this.getDeltaMovement();
        double d = this.getX() + vec3.x;
        double e = this.getY() + vec3.y;
        double f = this.getZ() + vec3.z;

        this.setPos(d, e, f);

        if (this.tickCount > MAX_LIFE_TICKS && !this.level().isClientSide()) {
            this.discard(); // Time out after 5 seconds, at zero alpha by the renderer envelope
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            Entity hitEntity = result.getEntity();
            Entity owner = this.getOwner();
            if (hitEntity instanceof LivingEntity living) {
                DamageSource source = owner instanceof Player playerOwner
                    ? serverLevel.damageSources().playerAttack(playerOwner)
                    : (owner instanceof LivingEntity livingOwner ? serverLevel.damageSources().mobProjectile(this, livingOwner) : serverLevel.damageSources().magic());

                living.hurtServer(serverLevel, source, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (damage)));
                living.addEffect(new MobEffectInstance(DbaEffects.MELTING_HOLDER, 300, 1, false, true), owner);

                // MC 26.2 Physics: Spatial rift warp friction & drag distortion
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                    living,
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.FRICTION_ID,
                    com.dragonblockarcanedba.DragonBlockArcaneDBA.id("dimensional_slash_friction"),
                    -0.90,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                );
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                    living,
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID,
                    com.dragonblockarcanedba.DragonBlockArcaneDBA.id("dimensional_slash_drag"),
                    -0.80,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                );

                serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.PLAYERS, 1.8f, 1.2f);
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            if (this.getFadeStart() < 0) {
                beginImpact(serverLevel, result);
            }
        }
    }

    /**
     * The strike, shared by entity and block hits: the crescent stops, bursts,
     * throws shards of cut space, and (on a block) cuts a temporary scar into
     * the exact block it struck, which the world heals on GroundScar's clock.
     * The entity then lingers for the renderer's tear fade instead of dying here.
     */
    private void beginImpact(ServerLevel serverLevel, HitResult result) {
        this.entityData.set(FADE_START, this.tickCount);
        this.setDeltaMovement(Vec3.ZERO);

        Vec3 at = result.getLocation();
        LayeredParticles.emitGroundBurst(serverLevel, at, 1.4f, THEME_ARGB);
        Entity owner = this.getOwner();
        DebrisSpawner.spawnField(serverLevel, owner instanceof LivingEntity living ? living : null,
            at, 10, 1.6, 2.2, 24, VOID_PALETTE);

        if (result instanceof BlockHitResult blockHit) {
            GroundScar.scar(serverLevel, blockHit.getBlockPos(),
                Blocks.CRYING_OBSIDIAN.defaultBlockState(), 140);
            serverLevel.playSound(null, at.x, at.y, at.z,
                SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.PLAYERS, 1.4f, 1.5f);
        }
    }
}
