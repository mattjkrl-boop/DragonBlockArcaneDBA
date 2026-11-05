package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.ability.AbilityDef;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * The one generic modeled presentation entity (Models Pass). An
 * ability definition's "models" block names a GeckoLib asset set;
 * this entity renders it through the dynamic GeoModel on the
 * client and runs one of five presentation behaviors on the
 * server: descend (plant from the air), fall (rain and shatter),
 * hang (freeze mid-air, then drop), bolt (fly the cast line), and
 * escort (ride the caster). It is pure presentation: it deals no
 * damage, blocks nothing, cannot be picked or hurt, refuses to
 * save, and discards itself on its data lifetime. The synced
 * contract is the model name, the three clip names, the scale,
 * and the animation phase (0 spawn, 1 hold, 2 end) the server
 * derives from the behavior clock, so every observer plays the
 * same clip at the same tick.
 */
public class ModelDisplayEntity extends Entity implements GeoEntity {
    private static final EntityDataAccessor<String> MODEL =
            SynchedEntityData.defineId(ModelDisplayEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> ANIM_SPAWN =
            SynchedEntityData.defineId(ModelDisplayEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> ANIM_HOLD =
            SynchedEntityData.defineId(ModelDisplayEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> ANIM_END =
            SynchedEntityData.defineId(ModelDisplayEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> SCALE =
            SynchedEntityData.defineId(ModelDisplayEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> PHASE =
            SynchedEntityData.defineId(ModelDisplayEntity.class, EntityDataSerializers.BYTE);

    public static final byte PHASE_SPAWN = 0;
    public static final byte PHASE_HOLD = 1;
    public static final byte PHASE_END = 2;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    // Server-side behavior state (never synced directly; position,
    // rotation, and phase carry everything an observer needs).
    private String behavior = "descend";
    private UUID ownerUuid;
    private double targetY;
    private double startY;
    private int descendTicks = 10;
    private int hangTicks;
    private int lifeTicks;
    private double fallSpeed = 1.2;
    private double spinDeg;
    private double forward;
    private double up;
    private double trailSpacing = 2.0;
    private int index;
    private int setCount = 1;
    private double orbitRadius;
    /** cast-trigger sets: the def whose cast keeps this piece alive. */
    private net.minecraft.resources.Identifier castDef;
    private int castEndAge = -1;
    private double fallVelocity;
    private int groundedAge = -1;
    private Vec3 boltVelocity = Vec3.ZERO;

    // Client-side clip cache (controllers run on the client).
    private String cachedClipName;
    private RawAnimation cachedClip;
    private boolean cachedClipHold;

    public ModelDisplayEntity(EntityType<? extends ModelDisplayEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        // No invulnerability flag call: the flag setter was renamed
        // between 26.2 and 26.3, and hurtServer below already
        // refuses every damage source outright.
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MODEL, "");
        builder.define(ANIM_SPAWN, "");
        builder.define(ANIM_HOLD, "");
        builder.define(ANIM_END, "");
        builder.define(SCALE, 1.0f);
        builder.define(PHASE, PHASE_SPAWN);
    }

    /** Server-side setup before the entity is added to the level. */
    public void init(AbilityDef.ModelSpec spec, UUID ownerUuid, int index,
                     double targetY, Vec3 startPos, Vec3 boltVelocity) {
        this.entityData.set(MODEL, spec.model());
        this.entityData.set(ANIM_SPAWN, spec.animSpawn() == null ? "" : spec.animSpawn());
        this.entityData.set(ANIM_HOLD, spec.animHold() == null ? "" : spec.animHold());
        this.entityData.set(ANIM_END, spec.animEnd() == null ? "" : spec.animEnd());
        this.entityData.set(SCALE, spec.scale());
        this.behavior = spec.behavior();
        this.ownerUuid = ownerUuid;
        this.index = index;
        this.targetY = targetY;
        this.startY = startPos.y;
        this.descendTicks = Math.max(1, spec.descendTicks());
        this.hangTicks = spec.hangTicks();
        this.lifeTicks = spec.lifetimeTicks();
        this.fallSpeed = spec.fallSpeed();
        this.spinDeg = spec.spin();
        this.forward = spec.forward();
        this.up = spec.up();
        this.trailSpacing = Math.max(0.5, spec.radius());
        this.setCount = Math.max(1, spec.count());
        this.orbitRadius = spec.radius();
        this.castDef = null;
        this.boltVelocity = boltVelocity == null ? Vec3.ZERO : boltVelocity;
        this.setPos(startPos.x, startPos.y, startPos.z);
        if (this.boltVelocity.lengthSqr() > 1.0e-6) {
            faceVelocity(this.boltVelocity);
        }
    }

    public String getModelName() {
        return this.entityData.get(MODEL);
    }

    public float getDisplayScale() {
        return this.entityData.get(SCALE);
    }

    public byte getPhase() {
        return this.entityData.get(PHASE);
    }

    private void setPhase(byte phase) {
        if (this.entityData.get(PHASE) != phase) {
            this.entityData.set(PHASE, phase);
        }
    }

    private void faceVelocity(Vec3 vel) {
        double horiz = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        this.setYRot((float) (Math.atan2(vel.x, vel.z) * (180.0 / Math.PI)));
        this.setXRot((float) (-Math.atan2(vel.y, horiz) * (180.0 / Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        int age = this.tickCount;
        switch (this.behavior) {
            case "descend" -> tickDescend(age);
            case "fall" -> tickFall(age);
            case "hang" -> tickHang(age);
            case "bolt" -> tickBolt(age);
            case "escort" -> tickEscort(age);
            case "orbit" -> tickOrbit(age);
            case "ride" -> tickRide(age);
            case "burst" -> tickBurst(age);
            default -> {
                if (this.lifeTicks > 0 && age > this.lifeTicks) {
                    this.discard();
                }
            }
        }
    }

    private void tickDescend(int age) {
        if (age < this.descendTicks) {
            setPhase(PHASE_SPAWN);
            double t = (age + 1) / (double) this.descendTicks;
            double eased = 1.0 - (1.0 - t) * (1.0 - t);
            this.setPos(this.getX(), this.startY + (this.targetY - this.startY) * eased, this.getZ());
        } else if (this.lifeTicks > 0 && age > this.lifeTicks - 6) {
            setPhase(PHASE_END);
        } else {
            setPhase(PHASE_HOLD);
        }
        if (this.lifeTicks > 0 && age > this.lifeTicks) {
            this.discard();
        }
    }

    /** burst: fixed in the air where spawned; spawn clip, hold, end clip, gone. */
    private void tickBurst(int age) {
        if (age < this.descendTicks) {
            setPhase(PHASE_SPAWN);
        } else if (this.lifeTicks > 0 && age > this.lifeTicks - 6) {
            setPhase(PHASE_END);
        } else {
            setPhase(PHASE_HOLD);
        }
        if (this.lifeTicks > 0 && age > this.lifeTicks) {
            this.discard();
        }
    }

    private void tickFall(int age) {
        if (this.groundedAge < 0) {
            setPhase(PHASE_SPAWN);
            this.fallVelocity = Math.min(this.fallSpeed * 2.4, this.fallVelocity + 0.16);
            double y = this.getY() - this.fallVelocity;
            if (this.spinDeg != 0) {
                this.setYRot(this.getYRot() + (float) this.spinDeg);
                this.yRotO = this.getYRot();
            }
            if (y <= this.targetY) {
                y = this.targetY;
                this.groundedAge = age;
            }
            this.setPos(this.getX(), y, this.getZ());
        } else {
            setPhase(PHASE_END);
            if (age > this.groundedAge + 8) {
                this.discard();
            }
        }
    }

    private void tickHang(int age) {
        if (age < this.hangTicks) {
            setPhase(PHASE_SPAWN);
            return;
        }
        if (this.groundedAge < 0) {
            setPhase(PHASE_HOLD);
            this.fallVelocity = Math.min(1.6, this.fallVelocity + 0.14);
            double y = this.getY() - this.fallVelocity;
            if (y <= this.targetY) {
                y = this.targetY;
                this.groundedAge = age;
            }
            this.setPos(this.getX(), y, this.getZ());
        } else {
            setPhase(PHASE_END);
            if (age > this.groundedAge + 8) {
                this.discard();
            }
        }
        if (this.lifeTicks > 0 && age > this.lifeTicks + 40) {
            this.discard();
        }
    }

    private void tickBolt(int age) {
        setPhase(age < 2 ? PHASE_SPAWN : PHASE_HOLD);
        Vec3 next = this.position().add(this.boltVelocity);
        this.setPos(next.x, next.y, next.z);
        if (this.lifeTicks > 0 && age > this.lifeTicks) {
            this.discard();
            return;
        }
        if (this.level() instanceof ServerLevel serverLevel
                && !serverLevel.getBlockState(this.blockPosition()).isAir()
                && age > 2) {
            this.discard();
        }
    }

    /**
     * orbit: a ring of pieces circling the owner at models.radius,
     * models.up above its feet, advancing models.spin degrees per
     * tick, each piece facing along its path. Ends with the owner or
     * at the lifetime (last 6 ticks play the end clip).
     */
    private void tickOrbit(int age) {
        if (this.lifeTicks > 0 && age > this.lifeTicks) {
            this.discard();
            return;
        }
        setPhase(this.lifeTicks > 0 && age > this.lifeTicks - 6 ? PHASE_END
                : age < 6 ? PHASE_SPAWN : PHASE_HOLD);
        if (!(this.level() instanceof ServerLevel serverLevel) || this.ownerUuid == null) {
            return;
        }
        Entity owner = serverLevel.getEntity(this.ownerUuid);
        if (owner == null || !owner.isAlive()) {
            this.discard();
            return;
        }
        if (this.castDef != null && this.castEndAge < 0
                && !com.dragonblockarcanedba.ability.AbilityService.isCasting(this.ownerUuid, this.castDef)) {
            this.castEndAge = age;
        }
        if (this.castEndAge >= 0) {
            setPhase(PHASE_END);
            if (age > this.castEndAge + 6) {
                this.discard();
                return;
            }
        }
        double rad = Math.toRadians(this.index * 360.0 / this.setCount + age * this.spinDeg);
        double r = this.orbitRadius;
        this.setPos(owner.getX() + Math.cos(rad) * r, owner.getY() + this.up,
                owner.getZ() + Math.sin(rad) * r);
        // Face along the path: velocity direction of the circle is (-sin, cos).
        faceVelocity(new Vec3(-Math.sin(rad), 0, Math.cos(rad)));
    }

    /**
     * ride: glued to one host entity (the owner UUID names it, for
     * example a swarm shard), facing its motion. Discards with it.
     */
    private void tickRide(int age) {
        setPhase(age < 6 ? PHASE_SPAWN : PHASE_HOLD);
        if (!(this.level() instanceof ServerLevel serverLevel) || this.ownerUuid == null) {
            this.discard();
            return;
        }
        Entity host = serverLevel.getEntity(this.ownerUuid);
        if (host == null || host.isRemoved()) {
            this.discard();
            return;
        }
        Vec3 motion = host.position().subtract(this.position());
        this.setPos(host.getX(), host.getY(), host.getZ());
        if (motion.lengthSqr() > 1.0e-5) {
            faceVelocity(motion);
        }
    }

    private void tickEscort(int age) {
        setPhase(PHASE_HOLD);
        if (this.lifeTicks > 0 && age > this.lifeTicks) {
            this.discard();
            return;
        }
        if (!(this.level() instanceof ServerLevel serverLevel) || this.ownerUuid == null) {
            return;
        }
        Entity owner = serverLevel.getEntity(this.ownerUuid);
        if (owner == null || !owner.isAlive()) {
            this.discard();
            return;
        }
        Vec3 look = owner.getLookAngle();
        Vec3 back = new Vec3(-look.x, 0, -look.z);
        double backLen = back.length();
        back = backLen > 1.0e-4 ? back.scale(1.0 / backLen) : new Vec3(0, 0, 1);
        Vec3 at = owner.position()
                .add(look.scale(this.forward))
                .add(back.scale(this.index * this.trailSpacing))
                .add(0, this.up + owner.getBbHeight() * 0.4, 0);
        this.setPos(at.x, at.y, at.z);
        this.setYRot(owner.getYRot());
        this.setXRot(owner.getXRot());
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level,
                              net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putString("Model", getModelName());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        input.getString("Model").ifPresent(v -> this.entityData.set(MODEL, v));
    }

    // ------------------------------------------------------------------
    // GeckoLib
    // ------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<ModelDisplayEntity>("main", 2, this::animState));
    }

    private PlayState animState(AnimationTest<ModelDisplayEntity> test) {
        byte phase = getPhase();
        String clip = switch (phase) {
            case PHASE_SPAWN -> this.entityData.get(ANIM_SPAWN);
            case PHASE_END -> this.entityData.get(ANIM_END);
            default -> this.entityData.get(ANIM_HOLD);
        };
        if (clip == null || clip.isEmpty()) {
            return PlayState.STOP;
        }
        boolean loop = phase != PHASE_END;
        if (!clip.equals(this.cachedClipName) || loop != this.cachedClipHold || this.cachedClip == null) {
            this.cachedClip = loop ? RawAnimation.begin().thenLoop(clip)
                    : RawAnimation.begin().thenPlayAndHold(clip);
            this.cachedClipName = clip;
            this.cachedClipHold = loop;
        }
        return test.setAndContinue(this.cachedClip);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    /** Binds this piece to a cast: it plays its end clip and leaves when the cast ends. */
    public void bindToCast(net.minecraft.resources.Identifier defId) {
        this.castDef = defId;
    }
}
