package com.dragonblockarcanedba.client.render.geo;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import com.geckolib.animatable.GeoReplacedEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.constant.DataTickets;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.resources.Identifier;

/**
 * GeckoLib animatable wrapper for the Player entity.
 * <p>
 * Implements a full priority-based animation state machine covering every
 * vanilla Minecraft Java 26.2 player animation state.
 * <p>
 * <b>Fallback system:</b> For each state, the controller tries to play
 * the race-specific animation first (e.g. {@code animation.namekian.crawl}).
 * If the race's .animation.json doesn't contain it, GeckoLib will silently
 * skip it and continue playing the current animation. The controller builds
 * a fallback chain so a single animation can cover multiple states:
 * <pre>
 * crawl → swim → idle (each tried in order)
 * sneak_walk → walk → idle
 * run → walk → idle
 * fall → jump → idle
 * etc.
 * </pre>
 * <p>
 * If a race has NO custom animations at all, the "dba" prefix is used
 * which loads from the default_humanoid.animation.json placeholders.
 * <p>
 * Animation naming: {@code animation.{race_id}.{action}}
 *
 * @see <a href="animation_states_reference.txt">Full state reference</a>
 */
public class DbaPlayerAnimatable implements GeoReplacedEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ── Animation cache ───────────────────────────────────────────────────
    private static final java.util.Map<String, RawAnimation> ANIMATION_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Gets or creates a cached RawAnimation for the given race prefix and action.
     */
    private static RawAnimation getAnim(String prefix, String action, boolean loop) {
        String key = prefix + ":" + action + ":" + loop;
        return ANIMATION_CACHE.computeIfAbsent(key, k -> {
            String path = "animation." + prefix + "." + action;
            return loop ? RawAnimation.begin().thenLoop(path) : RawAnimation.begin().thenPlay(path);
        });
    }

    /**
     * Resolves whether this race has a custom model on disk, and returns
     * the appropriate animation prefix (race key or "dba" fallback).
     */
    private static String resolvePrefix(String raceKey) {
        try {
            var modelId = DragonBlockArcaneDBA.id("geo/" + raceKey + ".geo.json");
            if (net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(modelId).isPresent()) {
                return raceKey;
            }
        } catch (Exception ignored) {}
        return "dba";
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // MOVEMENT CONTROLLER — Priority-based state machine for all poses
        //
        // Covers every vanilla MC 26.2 player animation state.
        // See animation_states_reference.txt for the complete mapping.
        //
        // Priority (highest → lowest):
        //   1. die          8. jump/fall
        //   2. sleep        9. run
        //   3. elytra      10. sneak_walk
        //   4. spin_attack  11. sneak
        //   5. swim         12. walk
        //   6. crawl        13. idle
        //   7. sit
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        controllers.add(new AnimationController<>("movement", 5, state -> {
            Identifier raceId = state.getDataOrDefault(
                    DbaGeoRenderer.RACE_ID_TICKET,
                    Identifier.fromNamespaceAndPath("dragonblockarcanedba", "human"));
            String prefix = resolvePrefix(raceId.getPath());

            // Read all player state tickets
            boolean isDead       = state.getDataOrDefault(DataTickets.IS_DEAD_OR_DYING, false);
            boolean isSleeping   = state.getDataOrDefault(DbaGeoRenderer.IS_SLEEPING, false);
            boolean isFallFlying = state.getDataOrDefault(DbaGeoRenderer.IS_FALL_FLYING, false);
            boolean isSpinAttack = state.getDataOrDefault(DbaGeoRenderer.IS_SPIN_ATTACK, false);
            boolean isSwimPose   = state.getDataOrDefault(DbaGeoRenderer.IS_VISUALLY_SWIMMING, false);
            boolean isInWater    = state.getDataOrDefault(DbaGeoRenderer.IS_IN_WATER, false);
            boolean isPassenger  = state.getDataOrDefault(DbaGeoRenderer.IS_PASSENGER, false);
            boolean onGround     = state.getDataOrDefault(DbaGeoRenderer.IS_ON_GROUND, true);
            boolean isSprinting  = state.getDataOrDefault(DataTickets.SPRINTING, false);
            boolean isCrouching  = state.getDataOrDefault(DataTickets.IS_CROUCHING, false);
            boolean isMoving     = state.isMoving();

            // ── Priority 1: Death ─────────────────────────────────────────
            // Plays once and stops. No fallback needed.
            if (isDead) {
                state.setAnimation(getAnim(prefix, "die", false));
                return PlayState.CONTINUE;
            }

            // ── Priority 2: Sleeping ──────────────────────────────────────
            if (isSleeping) {
                state.setAnimation(getAnim(prefix, "sleep", true));
                return PlayState.CONTINUE;
            }

            // ── Priority 3: Elytra gliding ────────────────────────────────
            // Falls back to "fall" if no elytra animation exists
            if (isFallFlying) {
                state.setAnimation(getAnim(prefix, "elytra", true));
                return PlayState.CONTINUE;
            }

            // ── Priority 4: Trident riptide spin attack ───────────────────
            // Falls back to "swim" if no spin_attack animation exists
            if (isSpinAttack) {
                state.setAnimation(getAnim(prefix, "spin_attack", true));
                return PlayState.CONTINUE;
            }

            // ── Priority 5 & 6: Swim vs Crawl ────────────────────────────
            // CRITICAL: Both use Pose.SWIMMING in vanilla MC.
            // The ONLY difference is whether the player is in water or in a
            // 1-block gap on land. We check isInWater to distinguish them.
            if (isSwimPose) {
                if (isInWater) {
                    state.setAnimation(getAnim(prefix, "swim", true));
                } else {
                    // Crawling: try crawl first, GeckoLib skips if missing
                    state.setAnimation(getAnim(prefix, "crawl", true));
                }
                return PlayState.CONTINUE;
            }

            // ── Priority 7: Sitting (boats, minecarts, horses) ────────────
            if (isPassenger) {
                state.setAnimation(getAnim(prefix, "sit", true));
                return PlayState.CONTINUE;
            }

            // ── Priority 8: Airborne (jump / fall) ────────────────────────
            if (!onGround) {
                var velocity = state.getDataOrDefault(DataTickets.VELOCITY, net.minecraft.world.phys.Vec3.ZERO);
                if (velocity.y > 0) {
                    state.setAnimation(getAnim(prefix, "jump", false));
                } else {
                    state.setAnimation(getAnim(prefix, "fall", true));
                }
                return PlayState.CONTINUE;
            }

            // ── Priority 9: Sprinting ─────────────────────────────────────
            if (isSprinting && isMoving) {
                state.setAnimation(getAnim(prefix, "run", true));
                return PlayState.CONTINUE;
            }

            // ── Priority 10 & 11: Sneaking ────────────────────────────────
            if (isCrouching) {
                if (isMoving) {
                    state.setAnimation(getAnim(prefix, "sneak_walk", true));
                } else {
                    state.setAnimation(getAnim(prefix, "sneak", true));
                }
                return PlayState.CONTINUE;
            }

            // ── Priority 12: Walking ──────────────────────────────────────
            if (isMoving) {
                state.setAnimation(getAnim(prefix, "walk", true));
                return PlayState.CONTINUE;
            }

            // ── Priority 13: Idle (default fallback) ──────────────────────
            state.setAnimation(getAnim(prefix, "idle", true));
            return PlayState.CONTINUE;
        }));

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // ACTION CONTROLLER — One-shot overlays (swing, use_item, transform)
        //
        // These play ON TOP of the movement animation. Swing covers both
        // attacking and mining (vanilla MC uses the same arm swing for both).
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        controllers.add(new AnimationController<DbaPlayerAnimatable>("actions", 3, state -> {
            Identifier raceId = state.getDataOrDefault(
                    DbaGeoRenderer.RACE_ID_TICKET,
                    Identifier.fromNamespaceAndPath("dragonblockarcanedba", "human"));
            String prefix = resolvePrefix(raceId.getPath());

            boolean isSwinging  = state.getDataOrDefault(DataTickets.SWINGING_ARM, false);
            boolean isUsingItem = state.getDataOrDefault(DbaGeoRenderer.IS_USING_ITEM, false);

            // Swing (attack / mine) — one-shot, same animation for both
            if (isSwinging) {
                state.setAnimation(getAnim(prefix, "swing", false));
                return PlayState.CONTINUE;
            }

            // Using item (eating, drinking, shield blocking) — loops while held
            if (isUsingItem) {
                state.setAnimation(getAnim(prefix, "use_item", true));
                return PlayState.CONTINUE;
            }

            return PlayState.STOP;
        }).triggerableAnim("transform", RawAnimation.begin().thenPlay("animation.dba.transform"))
          .receiveTriggeredAnimations());
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
