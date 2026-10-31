package com.dragonblockarcanedba.item.geo;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.world.item.Item;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Shared GeckoLib setup for 3D weapon models. Implementing items get an animated
 * geo model (idle, swing and charge loops) and an emissive glow layer on the client.
 * Client only hooks are injected through the static fields so this class never
 * touches client classes.
 */
public interface DbaGeoWeapon extends GeoItem {
    RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    RawAnimation SWING = RawAnimation.begin().thenLoop("swing");
    RawAnimation CHARGE = RawAnimation.begin().thenLoop("charge");

    Map<Item, AnimatableInstanceCache> CACHES = new ConcurrentHashMap<>();

    final class Hooks {
        /** Returns 0 idle, 1 swing, 2 charge for the given item. Set by the client. */
        public static ToIntFunction<Item> STATE = item -> 0;
        /** Builds the GeoRenderProvider for an item. Set by the client, null on servers. */
        public static Function<Item, Object> PROVIDER = item -> null;
        static final Map<Item, Object> PROVIDERS = new ConcurrentHashMap<>();

        private Hooks() {}
    }

    @Override
    default Object getRenderProvider() {
        Object p = Hooks.PROVIDERS.get((Item) this);
        if (p == null) {
            p = Hooks.PROVIDER.apply((Item) this);
            if (p != null) Hooks.PROVIDERS.put((Item) this, p);
        }
        return p;
    }

    @Override
    default void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<DbaGeoWeapon>("main", 3, this::dbaAnimState));
    }

    private PlayState dbaAnimState(AnimationTest<DbaGeoWeapon> test) {
        int state = Hooks.STATE.applyAsInt((Item) this);
        return test.setAndContinue(state == 1 ? SWING : state == 2 ? CHARGE : IDLE);
    }

    @Override
    default AnimatableInstanceCache getAnimatableInstanceCache() {
        return CACHES.computeIfAbsent((Item) this, i -> GeckoLibUtil.createInstanceCache(this));
    }
}
