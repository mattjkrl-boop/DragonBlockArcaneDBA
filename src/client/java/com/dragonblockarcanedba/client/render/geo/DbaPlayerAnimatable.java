package com.dragonblockarcanedba.client.render.geo;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import com.geckolib.animatable.GeoReplacedEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.world.entity.EntityType;

/**
 * GeckoLib animatable wrapper for the Player entity.
 * <p>
 * GeckoLib's replaced entity system requires a standalone GeoReplacedEntity
 * to define animation controllers. This class:
 * 1. Registers a "movement" controller (idle/walk loops)
 * 2. Registers an "actions" controller (attack/transform one-shots)
 * 3. Manages the animation instance cache
 * <p>
 * Animation names reference strings in the .animation.json files exported
 * from Blockbench, following the convention: "animation.dba.{action}"
 * <p>
 * Model height follows standard Minecraft player proportions (1.8 blocks / ~30px).
 */
public class DbaPlayerAnimatable implements GeoReplacedEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Common animation references (matching Blockbench export names)
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.dba.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.dba.walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.dba.attack");
    private static final RawAnimation TRANSFORM = RawAnimation.begin().thenPlay("animation.dba.transform");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.dba.fly");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Movement controller — handles idle and walking loops
        controllers.add(new AnimationController<>( "movement", 5, state -> {
            if (state.isMoving()) {
                state.setAnimation(WALK);
            } else {
                state.setAnimation(IDLE);
            }
            return PlayState.CONTINUE;
        }));

        // Action controller — handles attack and transformation one-shots via triggerableAnim
        controllers.add(new AnimationController<DbaPlayerAnimatable>("actions", 5, state -> PlayState.STOP)
            .triggerableAnim("attack", ATTACK)
            .triggerableAnim("transform", TRANSFORM)
            .triggerableAnim("fly", FLY)
            .receiveTriggeredAnimations());
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
