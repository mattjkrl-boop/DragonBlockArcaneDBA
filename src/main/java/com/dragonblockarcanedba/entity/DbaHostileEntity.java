package com.dragonblockarcanedba.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Hostile GeckoLib mob. Combat stats are applied on the instance because the living
 * attribute builder already contains health, speed, and follow range.
 */
public abstract class DbaHostileEntity extends Monster implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    protected DbaHostileEntity(EntityType<? extends DbaHostileEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createHostileAttributes() {
        return Monster.createMonsterAttributes();
    }

    protected final void applyCombatStats(double health, double speed, double attack, double followRange) {
        this.setBase(Attributes.MAX_HEALTH, health);
        this.setBase(Attributes.MOVEMENT_SPEED, speed);
        this.setBase(Attributes.ATTACK_DAMAGE, attack);
        this.setBase(Attributes.FOLLOW_RANGE, followRange);
        this.setHealth((float) health);
    }

    private void setBase(Holder<Attribute> attribute, double value) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.85));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<DbaHostileEntity>("main", 4, this::animate));
    }

    private PlayState animate(AnimationTest<DbaHostileEntity> test) {
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
}
