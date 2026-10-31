package com.dragonblockarcanedba.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import net.minecraft.world.entity.Entity;

import java.util.function.Predicate;

/**
 * GeckoLib animation setup shared by the eternal dragons. The body controller loops the
 * serpent motion; the action controller roars on arrival, plays the wish loop while a
 * wish is being made and otherwise idles.
 */
public interface DbaGeoDragon extends GeoEntity {
    RawAnimation SERPENT = RawAnimation.begin().thenLoop("serpent");
    RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    RawAnimation ROAR = RawAnimation.begin().thenPlay("roar");
    RawAnimation WISH = RawAnimation.begin().thenLoop("wish");
    int ROAR_TICKS = 60;

    final class Hooks {
        /** True while the local player is making a wish with this dragon. Set by the client. */
        public static Predicate<Entity> WISHING = e -> false;

        private Hooks() {}
    }

    @Override
    default void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<DbaGeoDragon>("body", 0, test -> test.setAndContinue(SERPENT)));
        controllers.add(new AnimationController<DbaGeoDragon>("action", 8, this::dbaAction));
    }

    private PlayState dbaAction(AnimationTest<DbaGeoDragon> test) {
        Entity self = (Entity) this;
        if (Hooks.WISHING.test(self)) return test.setAndContinue(WISH);
        if (self.tickCount < ROAR_TICKS) return test.setAndContinue(ROAR);
        return test.setAndContinue(IDLE);
    }
}
