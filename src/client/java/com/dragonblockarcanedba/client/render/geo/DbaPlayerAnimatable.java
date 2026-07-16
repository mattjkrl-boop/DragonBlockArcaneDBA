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

    private static final java.util.Map<String, RawAnimation> ANIMATION_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private static RawAnimation getAnimation(String racePrefix, String actionName, boolean loop) {
        String key = racePrefix + ":" + actionName + ":" + loop;
        return ANIMATION_CACHE.computeIfAbsent(key, k -> {
            String animPath = "animation." + racePrefix + "." + actionName;
            return loop ? RawAnimation.begin().thenLoop(animPath) : RawAnimation.begin().thenPlay(animPath);
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Movement controller — handles idle and walking loops
        controllers.add(new AnimationController<>( "movement", 5, state -> {
            net.minecraft.resources.Identifier raceId = state.getDataOrDefault(DbaGeoRenderer.RACE_ID_TICKET, net.minecraft.resources.Identifier.fromNamespaceAndPath("dragonblockarcanedba", "human"));
            String raceKey = raceId.getPath();

            // Check if a custom model exists on disk for this race
            boolean hasCustomModel = false;
            try {
                var modelId = DragonBlockArcaneDBA.id("geo/" + raceKey + ".geo.json");
                hasCustomModel = net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(modelId).isPresent();
            } catch (Exception ignored) {}

            String prefix = hasCustomModel ? raceKey : "dba";
            String action = state.isMoving() ? "walk" : "idle";

            state.setAnimation(getAnimation(prefix, action, true));
            return PlayState.CONTINUE;
        }));

        // Action controller — handles attack, transform and fly one-shots
        controllers.add(new AnimationController<DbaPlayerAnimatable>("actions", 5, state -> {
            net.minecraft.resources.Identifier raceId = state.getDataOrDefault(DbaGeoRenderer.RACE_ID_TICKET, net.minecraft.resources.Identifier.fromNamespaceAndPath("dragonblockarcanedba", "human"));
            String raceKey = raceId.getPath();

            boolean hasCustomModel = false;
            try {
                var modelId = DragonBlockArcaneDBA.id("geo/" + raceKey + ".geo.json");
                hasCustomModel = net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(modelId).isPresent();
            } catch (Exception ignored) {}

            String prefix = hasCustomModel ? raceKey : "dba";

            // Configure dynamic triggerable animations
            state.controller().triggerableAnim("attack", getAnimation(prefix, "attack", false));
            state.controller().triggerableAnim("transform", getAnimation(prefix, "transform", false));
            state.controller().triggerableAnim("fly", getAnimation(prefix, "fly", true));

            return PlayState.STOP;
        }).receiveTriggeredAnimations());
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
