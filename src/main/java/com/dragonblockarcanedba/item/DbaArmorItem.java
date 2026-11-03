package com.dragonblockarcanedba.item;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.world.item.Item;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Worn armor that keeps the vanilla equipment stats and slot, and supplies a GeckoLib
 * model on the client. The render provider is installed by the client so this class
 * stays free of client imports.
 */
public class DbaArmorItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String modelName;

    public DbaArmorItem(Properties properties, String modelName) {
        super(properties);
        this.modelName = modelName;
    }

    /** Shared geo model id. Pieces of one set reuse the same file and render their own bones. */
    public String modelName() {
        return this.modelName;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("idle", 0, test -> PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public Object getRenderProvider() {
        Object provider = Hooks.PROVIDERS.get(this);
        if (provider == null) {
            provider = Hooks.PROVIDER.apply(this);
            if (provider != null) {
                Hooks.PROVIDERS.put(this, provider);
            }
        }
        return provider;
    }

    public static final class Hooks {
        /** Client installs this. Servers leave the default, which returns null. */
        public static Function<DbaArmorItem, Object> PROVIDER = item -> null;
        static final Map<DbaArmorItem, Object> PROVIDERS = new ConcurrentHashMap<>();

        private Hooks() {}
    }
}
