package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.item.DbaItems;
import com.dragonblockarcanedba.sound.DbaSounds;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Deployable ally. It stays near the player who placed it, hits hostile mobs, and
 * comes back as an item when that player recalls it or it is destroyed.
 */
public class PatrolRobotEntity extends net.minecraft.world.entity.PathfinderMob implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private UUID ownerId;
    private boolean returnsItem;

    public PatrolRobotEntity(EntityType<? extends PatrolRobotEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCustomName(Component.translatable("entity.dragonblockarcanedba.patrol_robot"));
        this.setBase(Attributes.MAX_HEALTH, 36.0);
        this.setBase(Attributes.MOVEMENT_SPEED, 0.28);
        this.setBase(Attributes.ARMOR, 8.0);
        this.setBase(Attributes.FOLLOW_RANGE, 24.0);
        this.setHealth(36.0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.ATTACK_DAMAGE, 6.0);
    }

    public void assignOwner(Player player, boolean returnsItem) {
        this.ownerId = player.getUUID();
        this.returnsItem = returnsItem;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
        this.goalSelector.addGoal(6, new StayNearOwnerGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    @Override
    public void aiStep() {
        if (!this.level().isClientSide()) {
            this.assistOwner();
        }
        super.aiStep();
    }

    private void assistOwner() {
        Player owner = this.owner();
        if (owner == null) {
            return;
        }
        if (this.getTarget() == owner) {
            this.setTarget(null);
        }
        LivingEntity attacker = owner.getLastHurtByMob();
        if (attacker == null || attacker == this || attacker == owner || !attacker.isAlive()) {
            return;
        }
        if (owner.tickCount - owner.getLastHurtByMobTimestamp() > 160) {
            return;
        }
        if (this.distanceToSqr(attacker) < 576.0) {
            this.setTarget(attacker);
        }
    }

    private Player owner() {
        if (this.ownerId == null) {
            return null;
        }
        return this.level().getPlayerByUUID(this.ownerId);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && this.ownerId != null && player.getUUID().equals(this.ownerId)) {
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        boolean ownerHere = this.ownerId == null || player.getUUID().equals(this.ownerId);
        if (!ownerHere && !player.getAbilities().instabuild) {
            return InteractionResult.FAIL;
        }
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (this.returnsItem) {
            player.addItem(new net.minecraft.world.item.ItemStack(DbaItems.PATROL_ROBOT));
        }
        this.returnsItem = false;
        this.discard();
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                DbaSounds.REPAIR, net.minecraft.sounds.SoundSource.NEUTRAL, 0.7f, 1.4f);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (this.returnsItem) {
            this.spawnAtLocation(level, DbaItems.PATROL_ROBOT);
            this.returnsItem = false;
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return DbaSounds.ROBOT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return DbaSounds.ROBOT_DEATH;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<PatrolRobotEntity>("main", 4, this::animate));
    }

    private PlayState animate(AnimationTest<PatrolRobotEntity> test) {
        if (com.dragonblockarcanedba.util.DbaCompat.isSwinging(this)
                || com.dragonblockarcanedba.util.DbaCompat.attackAnim(this, 1.0f) > 0.0f) {
            return test.setAndContinue(ATTACK);
        }
        if (this.getDeltaMovement().horizontalDistanceSqr() > 2.5E-3) {
            return test.setAndContinue(WALK);
        }
        return test.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("ReturnsItem", this.returnsItem);
        if (this.ownerId != null) {
            output.putString("Owner", this.ownerId.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.returnsItem = input.getBooleanOr("ReturnsItem", false);
        input.getString("Owner").ifPresent(this::readOwner);
    }

    private void readOwner(String value) {
        try {
            this.ownerId = UUID.fromString(value);
        } catch (IllegalArgumentException bad) {
            this.ownerId = null;
        }
    }

    private void setBase(Holder<Attribute> attribute, double value) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private static final class StayNearOwnerGoal extends Goal {
        private final PatrolRobotEntity robot;

        StayNearOwnerGoal(PatrolRobotEntity robot) {
            this.robot = robot;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Player owner = this.robot.owner();
            return owner != null && this.robot.getTarget() == null && this.robot.distanceToSqr(owner) > 36.0;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Player owner = this.robot.owner();
            if (owner == null) {
                return;
            }
            if (this.robot.distanceToSqr(owner) > 576.0) {
                this.robot.teleportTo(owner.getX() + 1.0, owner.getY(), owner.getZ());
                this.robot.getNavigation().stop();
                return;
            }
            this.robot.getNavigation().moveTo(owner, 1.15);
        }
    }
}
