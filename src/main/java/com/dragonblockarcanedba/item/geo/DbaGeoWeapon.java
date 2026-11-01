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

/**
 * Shared GeckoLib setup for 3D weapon models. Implementing items get an animated
 * geo model (idle, swing and charge loops) and an emissive glow layer on the client.
 * Client only hooks are injected through the static fields so this class never
 * touches client classes.
 *
 * Animation state is per holder and per display context: the client renderer stamps
 * DbaWeaponTickets from the entity actually holding the rendered stack, the predicate
 * below reads only those tickets, and isPerspectiveAware gives every display context
 * (first person, third person, GUI, and so on) its own animation manager per stack.
 * Stack ids are assigned on the server by WeaponAnimatableIds so the same stack keys
 * to the same animation manager on every client.
 */
public interface DbaGeoWeapon extends GeoItem {
    RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    RawAnimation SWING = RawAnimation.begin().thenLoop("swing");
    RawAnimation CHARGE = RawAnimation.begin().thenLoop("charge");

    Map<Item, AnimatableInstanceCache> CACHES = new ConcurrentHashMap<>();

    final class Hooks {
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

    /** First person and third person copies of the same stack animate on separate timelines. */
    @Override
    default boolean isPerspectiveAware() {
        return true;
    }

    @Override
    default void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<DbaGeoWeapon>("main", 3, this::dbaAnimState));
    }

    private PlayState dbaAnimState(AnimationTest<DbaGeoWeapon> test) {
        float useRatio = test.getDataOrDefault(DbaWeaponTickets.USE_RATIO, -1f);
        if (useRatio >= 0f) return test.setAndContinue(CHARGE);
        if (test.getDataOrDefault(DbaWeaponTickets.HOLDER_SWINGING, false)) return test.setAndContinue(SWING);
        return test.setAndContinue(IDLE);
    }

    @Override
    default AnimatableInstanceCache getAnimatableInstanceCache() {
        return CACHES.computeIfAbsent((Item) this, i -> GeckoLibUtil.createInstanceCache(this));
    }
}
