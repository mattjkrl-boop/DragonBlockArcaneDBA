package com.dragonblockarcanedba.client.render.animation;

import com.dragonblockarcanedba.client.render.layer.DbaPlayerState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Universal Bedrock animation engine for Dragon Block Arcane.
 * Provides 100% full movement coverage across every single Minecraft state:
 * - Death
 * - Sleeping in bed
 * - Crawling (1-block tunnels)
 * - Swimming & Water floating
 * - Elytra & Ki Flight
 * - Jumping, Falling, & Landing
 * - Crouching / Sneaking (idle & walk)
 * - Riding (Horse, Boat, Minecart)
 * - Weapon Wielding (Swords, Axes, Shields, Eating, Items)
 * - Combat Attacks & Punches
 * - Locomotion (Walk forward, Walk backward, Sprint)
 * - Idling & Item Idling
 * - All DBZ Stances & Custom Emotes
 */
public final class BedrockAnimationApplier {

    private BedrockAnimationApplier() {}

    /** Last form path seen per player. An empty string is the base form. */
    private static final Map<Integer, String> LAST_FORM = new ConcurrentHashMap<>();
    /** Clip selected when that player's existing form id changed. */
    private static final Map<Integer, String> TRANSFORM_CLIP = new ConcurrentHashMap<>();
    /** Age, in seconds, when the transform clip started. */
    private static final Map<Integer, Float> TRANSFORM_START = new ConcurrentHashMap<>();

    public static boolean apply(
            HumanoidModel<?> model,
            AvatarRenderState state,
            DbaPlayerState dbaState,
            float limbSwing,
            float limbSwingAmount
    ) {
        if (model == null || dbaState == null) {
            return false;
        }

        float ageSeconds = dbaState.dba$getTailAgeInTicks() / 20.0f;
        String activeEmote = dbaState.dba$getActiveEmote();
        ItemStack mainItem = state.getMainHandItemStack();
        boolean hasSword = isSword(mainItem);
        boolean hasAxe = isAxe(mainItem);
        final String race = raceKey(dbaState);
        watchForm(state, dbaState, race);

        // ==================== 1. DEATH ====================
        if (state.deathTime > 0.0f) {
            BedrockAnimationRegistry.BedrockAnimation deathAnim = getAnim(race, "death");
            if (deathAnim != null) {
                float time = Math.min((state.deathTime / 20.0f), deathAnim.length);
                applyTrackedBones(model, deathAnim, time, 1.0f, true);
                return true;
            }
        }

        // ==================== 2. SLEEPING ====================
        if (state.hasPose(Pose.SLEEPING) || state.bedOrientation != null) {
            BedrockAnimationRegistry.BedrockAnimation sleepAnim = getAnim(race, "sleep_idle");
            if (sleepAnim == null) sleepAnim = getAnim(race, "sleep");
            if (sleepAnim != null) {
                float time = sleepAnim.calculatePlayTime(ageSeconds);
                applyTrackedBones(model, sleepAnim, time, 1.0f, true);
                return true;
            }
        }

        // The rush forces fall-flying. A fly or transform clip would lean the
        // bones again on top of that pose, so the rush returns first.
        if (state.isFallFlying && holdsAzureDragon(state)) {
            applyAzureRushPose(model, dbaState);
            return true;
        }

        // Form id already changed on this render state. Play that clip before
        // emotes and locomotion so the power-up is the pose you see.
        if (applyActiveTransform(model, state, dbaState)) {
            if (state.isFallFlying || dbaState.dba$isFlying()) {
                float transformBank = (float) Math.toRadians(dbaState.dba$getFlightBank());
                model.body.zRot += transformBank;
                model.head.zRot -= transformBank * 0.35f;
            }
            return true;
        }

        // ==================== 3. EMOTES & DBZ STANCES ====================
        if (activeEmote != null && !activeEmote.isEmpty()) {
            BedrockAnimationRegistry.BedrockAnimation emoteAnim = getAnim(race, activeEmote);
            if (emoteAnim != null) {
                float time = emoteAnim.calculatePlayTime(ageSeconds);
                applyTrackedBones(model, emoteAnim, time, 1.0f);
                return true;
            } else {
                EmoteAnimationHelper.apply(model, activeEmote, dbaState.dba$getTailAgeInTicks());
                return true;
            }
        }

        // ==================== 4. CRAWLING (1-Block Tunnels) ====================
        if (state.hasPose(Pose.SWIMMING) && !state.isInWater) {
            BedrockAnimationRegistry.BedrockAnimation crawlAnim = getAnim(race, "crawl");
            if (crawlAnim != null) {
                float time = (limbSwing * 0.12f) % crawlAnim.length;
                applyTrackedBones(model, crawlAnim, time, 1.0f);
                return true;
            }
        }

        // ==================== 5. SWIMMING ====================
        if (state.isVisuallySwimming || (state.isInWater && state.swimAmount > 0.1f)) {
            BedrockAnimationRegistry.BedrockAnimation swimAnim = getAnim(race, "swim");
            if (swimAnim != null) {
                float time = swimAnim.calculatePlayTime(ageSeconds);
                applyTrackedBones(model, swimAnim, time, 1.0f);
                return true;
            }
        }

        // ==================== 5.5. CLIMBING (Ladders, Vines, Scaffolding) ====================
        if (dbaState.dba$isOnLadder()) {
            BedrockAnimationRegistry.BedrockAnimation climbAnim = getAnim(race, "climb");
            if (climbAnim != null) {
                boolean isMovingOnLadder = Math.abs(dbaState.dba$getLocalVelocityY()) > 0.02f || dbaState.dba$getHorizontalSpeed() > 0.02f;
                float time = isMovingOnLadder ? (limbSwing * 0.15f) % climbAnim.length : 0.25f;
                applyTrackedBones(model, climbAnim, time, 1.0f);
                return true;
            }
        }

        // ==================== 6. FLYING (Elytra / Ki Flight) ====================
        if (state.isFallFlying || dbaState.dba$isFlying()) {
            BedrockAnimationRegistry.BedrockAnimation flyAnim = getAnim(race, "fly");
            if (flyAnim != null) {
                float time = flyAnim.calculatePlayTime(ageSeconds);
                applyTrackedBones(model, flyAnim, time, 1.0f);
                // Unified flight banking: the bank angle is computed once in the shared
                // player render state, so every race and form rolls into turns the same way.
                // The head counter-rolls a third of it to keep the gaze steadier.
                float bank = (float) Math.toRadians(dbaState.dba$getFlightBank());
                model.body.zRot += bank;
                model.head.zRot -= bank * 0.35f;
                return true;
            }
        }

        // ==================== 7. RIDING (Horse / Boat / Minecart) ====================
        if (state.isPassenger) {
            BedrockAnimationRegistry.BedrockAnimation rideAnim = getAnim(race, "riding");
            if (rideAnim == null) rideAnim = getAnim(race, "ride_idle");
            if (rideAnim == null) rideAnim = getAnim(race, "sit");
            if (rideAnim != null) {
                float time = rideAnim.calculatePlayTime(ageSeconds);
                applyTrackedBones(model, rideAnim, time, 1.0f);
                return true;
            }
        }

        // ==================== 8. AIRBORNE: JUMPING & FALLING ====================
        // Anything still off the ground here is a jump or a fall. The old gap
        // between those two velocity bands fell through to the walk clip, so a
        // jump apex and an unflagged hover played the ground cycle in midair.
        float velY = dbaState.dba$getLocalVelocityY();
        if (!state.isPassenger && !state.isInWater && !state.isFallFlying && !dbaState.dba$isOnGround()) {
            if (velY > 0.12f) {
                String jumpId = dbaState.dba$isSprinting() ? "running_jump" : "jump";
                BedrockAnimationRegistry.BedrockAnimation jumpAnim = getAnim(race, jumpId);
                if (jumpAnim != null) {
                    float time = jumpAnim.calculatePlayTime(ageSeconds);
                    applyTrackedBones(model, jumpAnim, time, 1.0f);
                    return true;
                }
            }
            BedrockAnimationRegistry.BedrockAnimation fallAnim = getAnim(race, "fall");
            if (fallAnim != null) {
                float time = fallAnim.calculatePlayTime(ageSeconds);
                applyTrackedBones(model, fallAnim, time, 1.0f);
                return true;
            }
        }

        // ==================== 9. CROUCHING / SNEAKING ====================
        if (state.isCrouching) {
            if (dbaState.dba$getHorizontalSpeed() > 0.05f) {
                BedrockAnimationRegistry.BedrockAnimation sneakWalkAnim = getAnim(race, "sneak_walk");
                if (sneakWalkAnim != null) {
                    float time = (limbSwing * 0.12f) % sneakWalkAnim.length;
                    applyTrackedBones(model, sneakWalkAnim, time, 1.0f);
                    return true;
                }
            } else {
                BedrockAnimationRegistry.BedrockAnimation sneakIdleAnim = getAnim(race, "sneak_idle");
                if (sneakIdleAnim != null) {
                    float time = sneakIdleAnim.calculatePlayTime(ageSeconds);
                    applyTrackedBones(model, sneakIdleAnim, time, 1.0f);
                    return true;
                }
            }
        }

        // ==================== 10. BLOCKING / PARRYING ====================
        if (state.rightArmPose == HumanoidModel.ArmPose.BLOCK || state.leftArmPose == HumanoidModel.ArmPose.BLOCK) {
            String blockId = hasSword ? "sword_parry" : (hasAxe ? "axe_parry" : "block");
            BedrockAnimationRegistry.BedrockAnimation blockAnim = getAnim(race, blockId);
            if (blockAnim != null) {
                float time = blockAnim.calculatePlayTime(ageSeconds);
                applyTrackedBones(model, blockAnim, time, 1.0f);
                return true;
            }
        }

        // ==================== 11. USING ITEMS (Eating, Drinking, Tools) ====================
        if (state.isUsingItem) {
            String useId = isConsumable(mainItem) ? "eat" : "use_item";
            BedrockAnimationRegistry.BedrockAnimation useAnim = getAnim(race, useId);
            if (useAnim != null) {
                float time = useAnim.calculatePlayTime(ageSeconds);
                applyTrackedBones(model, useAnim, time, 1.0f);
                return true;
            }
        }

        // ==================== 12. ATTACK SWINGS / PUNCHES ====================
        if (com.dragonblockarcanedba.util.DbaCompat.swingProgress(state) > 0.0f) {
            String attackId = hasSword ? "attack" : "punch";
            BedrockAnimationRegistry.BedrockAnimation attackAnim = getAnim(race, attackId);
            if (attackAnim != null) {
                float time = com.dragonblockarcanedba.util.DbaCompat.swingProgress(state) * attackAnim.length;
                applyTrackedBones(model, attackAnim, time, 1.0f);
                return true;
            }
        }

        // ==================== 13. LOCOMOTION (Walking, Sprinting, Backward) ====================
        if (dbaState.dba$getHorizontalSpeed() > 0.05f) {
            // Local Z is positive when moving the way the body faces. The old
            // test used the opposite sign, so ordinary walking played walk_back.
            if (dbaState.dba$getLocalVelocityZ() < -0.08f) {
                BedrockAnimationRegistry.BedrockAnimation walkBackAnim = getAnim(race, "walk_back");
                if (walkBackAnim != null) {
                    float time = (limbSwing * 0.12f) % walkBackAnim.length;
                    applyTrackedBones(model, walkBackAnim, time, 1.0f);
                    return true;
                }
            }

            if (dbaState.dba$isSprinting()) {
                // Running / Sprinting
                BedrockAnimationRegistry.BedrockAnimation runAnim = getAnim(race, "run");
                if (runAnim != null) {
                    float time = (limbSwing * 0.15f) % runAnim.length;
                    applyTrackedBones(model, runAnim, time, 1.0f);
                    return true;
                }
            }

            // Normal Walking (Sword walk, Axe walk, Standard walk)
            String walkId = hasSword ? "sword_walk" : (hasAxe ? "axe_walk" : "walk");
            BedrockAnimationRegistry.BedrockAnimation walkAnim = getAnim(race, walkId);
            if (walkAnim != null) {
                float time = (limbSwing * 0.12f) % walkAnim.length;
                applyTrackedBones(model, walkAnim, time, 1.0f);
                return true;
            }
        }

        // ==================== 14. IDLE / STANDING ====================
        String idleId = hasSword ? "sword_idle" : (hasAxe ? "axe_idle" : (!mainItem.isEmpty() ? "item_idle" : "idle"));
        BedrockAnimationRegistry.BedrockAnimation idleAnim = getAnim(race, idleId);
        if (idleAnim != null) {
            float time = idleAnim.calculatePlayTime(ageSeconds);
            applyTrackedBones(model, idleAnim, time, 1.0f);
            return true;
        }

        return false;
    }

    /** Held azure rush. The pose stack already leans the body; these bones trail. */
    private static void applyAzureRushPose(HumanoidModel<?> model, DbaPlayerState dbaState) {
        float lookYaw = model.head.yRot;
        restoreBind(model);
        model.body.xRot = 0.2F;
        model.head.xRot = -0.55F;
        model.head.yRot = Mth.clamp(lookYaw, -0.7F, 0.7F) * 0.35F;

        model.rightArm.xRot = 0.9F;
        model.leftArm.xRot = 0.9F;
        model.rightArm.yRot = 0.22F;
        model.leftArm.yRot = -0.22F;
        model.rightArm.zRot = 0.5F;
        model.leftArm.zRot = -0.5F;
        model.rightArm.z += 2.0F;
        model.leftArm.z += 2.0F;

        model.rightLeg.xRot = 0.65F;
        model.leftLeg.xRot = 0.42F;
        model.rightLeg.zRot = 0.1F;
        model.leftLeg.zRot = -0.1F;
        model.rightLeg.z += 1.0F;
        model.leftLeg.z += 1.0F;

        float bank = (float) Math.toRadians(Mth.clamp(dbaState.dba$getFlightBank(), -14.0F, 14.0F));
        model.body.zRot += bank;
        model.rightArm.zRot += bank;
        model.leftArm.zRot += bank;
        model.rightLeg.zRot += bank;
        model.leftLeg.zRot += bank;
        model.head.zRot -= bank * 0.25F;
    }

    private static boolean holdsAzureDragon(AvatarRenderState state) {
        if (state.rightHandItemStack != null
                && state.rightHandItemStack.getItem() instanceof com.dragonblockarcanedba.item.AzureDragonSwordItem) {
            return true;
        }
        return state.leftHandItemStack != null
                && state.leftHandItemStack.getItem() instanceof com.dragonblockarcanedba.item.AzureDragonSwordItem;
    }

    private static BedrockAnimationRegistry.BedrockAnimation getAnim(String race, String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        if (isClipToken(race)) {
            BedrockAnimationRegistry.BedrockAnimation raceAnim =
                    BedrockAnimationRegistry.getAnimation("animation." + race + "." + id);
            if (raceAnim != null) {
                return raceAnim;
            }
        }
        BedrockAnimationRegistry.BedrockAnimation anim = BedrockAnimationRegistry.getAnimation(id);
        if (anim == null) {
            anim = BedrockAnimationRegistry.getAnimation("animation.player." + id);
        }
        return anim;
    }

    /**
     * The render state already receives the player's form id. When that id
     * changes, play the matching transform clip. This is the existing form
     * change, not a second controller.
     */
    private static void watchForm(AvatarRenderState state, DbaPlayerState dbaState, String race) {
        int id = state.id;
        Identifier form = dbaState.dba$getActiveFormId();
        String formKey = "";
        if (form != null && isClipToken(form.getPath().toLowerCase(Locale.ROOT))) {
            formKey = form.getPath().toLowerCase(Locale.ROOT);
        }
        String previous = LAST_FORM.get(id);
        if (previous == null) {
            LAST_FORM.put(id, formKey);
            return;
        }
        if (previous.equals(formKey)) {
            return;
        }
        LAST_FORM.put(id, formKey);
        String clipName = transformClipName(race, formKey);
        if (clip(clipName) == null) {
            TRANSFORM_CLIP.remove(id);
            TRANSFORM_START.remove(id);
            return;
        }
        TRANSFORM_CLIP.put(id, clipName);
        TRANSFORM_START.put(id, dbaState.dba$getTailAgeInTicks() / 20.0f);
    }

    private static boolean applyActiveTransform(
            HumanoidModel<?> model,
            AvatarRenderState state,
            DbaPlayerState dbaState
    ) {
        int id = state.id;
        String clipName = TRANSFORM_CLIP.get(id);
        Float started = TRANSFORM_START.get(id);
        if (clipName == null || started == null) {
            return false;
        }
        BedrockAnimationRegistry.BedrockAnimation anim = clip(clipName);
        if (anim == null) {
            TRANSFORM_CLIP.remove(id);
            TRANSFORM_START.remove(id);
            return false;
        }
        float elapsed = (dbaState.dba$getTailAgeInTicks() / 20.0f) - started;
        if (elapsed < 0.0f || elapsed > anim.length) {
            TRANSFORM_CLIP.remove(id);
            TRANSFORM_START.remove(id);
            return false;
        }
        applyTrackedBones(model, anim, elapsed, 1.0f, true);
        return true;
    }

    private static String transformClipName(String race, String formKey) {
        if (formKey == null || formKey.isEmpty()) {
            if (isClipToken(race)) {
                String baseClip = "animation." + race + ".transform.base";
                if (clip(baseClip) != null) {
                    return baseClip;
                }
            }
            if (clip("animation.player.revert") != null) {
                return "animation.player.revert";
            }
            return "animation.player.transform";
        }
        if (isClipToken(race)) {
            String raceClip = "animation." + race + ".transform." + formKey;
            if (clip(raceClip) != null) {
                return raceClip;
            }
        }
        String formClip = "animation.form." + formKey;
        if (clip(formClip) != null) {
            return formClip;
        }
        if (isClipToken(race)) {
            String raceGeneric = "animation." + race + ".transform";
            if (clip(raceGeneric) != null) {
                return raceGeneric;
            }
        }
        return "animation.player.transform";
    }

    private static BedrockAnimationRegistry.BedrockAnimation clip(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        return BedrockAnimationRegistry.getAnimation(name);
    }

    private static String raceKey(DbaPlayerState dbaState) {
        Identifier raceId = dbaState.dba$getRaceId();
        if (raceId == null) {
            return "";
        }
        String path = raceId.getPath().toLowerCase(Locale.ROOT);
        return isClipToken(path) ? path : "";
    }

    private static boolean isClipToken(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSword(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.is(ItemTags.SWORDS)) return true;
        String name = stack.getHoverName().getString().toLowerCase();
        return name.contains("sword") || name.contains("blade") || name.contains("katana") || name.contains("edge");
    }

    private static boolean isAxe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.is(ItemTags.AXES)) return true;
        String name = stack.getHoverName().getString().toLowerCase();
        return name.contains("axe");
    }

    private static boolean isConsumable(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.has(DataComponents.FOOD) || stack.has(DataComponents.CONSUMABLE);
    }

    private static void applyTrackedBones(
            HumanoidModel<?> model,
            BedrockAnimationRegistry.BedrockAnimation anim,
            float time,
            float weight
    ) {
        applyTrackedBones(model, anim, time, weight, false);
    }

    private static void applyTrackedBones(
            HumanoidModel<?> model,
            BedrockAnimationRegistry.BedrockAnimation anim,
            float time,
            float weight,
            boolean overrideLook
    ) {
        if (anim == null || anim.bones.isEmpty()) return;

        // Vanilla setupAnim leaves attack offsets and a one-arm swing on the
        // parts. Clear back to the humanoid bind so a clip that does not mention
        // a channel cannot inherit that pose. Head look is restored afterwards.
        float lookPitch = model.head.xRot;
        float lookYaw = model.head.yRot;
        restoreBind(model);
        model.head.xRot = lookPitch;
        model.head.yRot = lookYaw;

        applyHeadBone(model.head, anim.bones.get("head"), time, weight, overrideLook);
        applyBone(model.body, anim.bones.get("body"), time, weight);
        applyBone(model.rightArm, anim.bones.get("right_arm"), time, weight);
        applyBone(model.leftArm, anim.bones.get("left_arm"), time, weight);
        applyBone(model.rightLeg, anim.bones.get("right_leg"), time, weight);
        applyBone(model.leftLeg, anim.bones.get("left_leg"), time, weight);

        // The custom mesh draws each limb from its own part. Those parts are
        // siblings, so a lean stored only on "root" never pitched the arms or
        // the legs. Fold the root rotation into every limb after the local tracks.
        BedrockAnimationRegistry.BoneTrack rootTrack = anim.bones.get("root");
        if (rootTrack != null) {
            float[] rootPos = rootTrack.samplePosition(time);
            float[] rootRot = rootTrack.sampleRotation(time);
            float rx = (float) Math.toRadians(rootRot[0]) * weight;
            float ry = (float) Math.toRadians(rootRot[1]) * weight;
            float rz = (float) Math.toRadians(rootRot[2]) * weight;
            if (rx != 0.0f || ry != 0.0f || rz != 0.0f) {
                addRot(model.body, rx, ry, rz);
                addRot(model.head, rx, ry, rz);
                addRot(model.rightArm, rx, ry, rz);
                addRot(model.leftArm, rx, ry, rz);
                addRot(model.rightLeg, rx, ry, rz);
                addRot(model.leftLeg, rx, ry, rz);
            }
            float elev = -rootPos[1] * weight;
            float slideX = rootPos[0] * weight;
            float slideZ = rootPos[2] * weight;
            if (elev != 0.0f || slideX != 0.0f || slideZ != 0.0f) {
                shift(model.body, slideX, elev, slideZ);
                shift(model.head, slideX, elev, slideZ);
                shift(model.rightArm, slideX, elev, slideZ);
                shift(model.leftArm, slideX, elev, slideZ);
                shift(model.rightLeg, slideX, elev, slideZ);
                shift(model.leftLeg, slideX, elev, slideZ);
            }
        }
    }

    /** Humanoid bind offsets, in pixels, matching HumanoidModel.createMesh. */
    private static void restoreBind(HumanoidModel<?> model) {
        bind(model.head, 0.0f, 0.0f, 0.0f);
        bind(model.body, 0.0f, 0.0f, 0.0f);
        bind(model.rightArm, -5.0f, 2.0f, 0.0f);
        bind(model.leftArm, 5.0f, 2.0f, 0.0f);
        bind(model.rightLeg, -1.9f, 12.0f, 0.0f);
        bind(model.leftLeg, 1.9f, 12.0f, 0.0f);
        ModelPart root = model.root();
        if (root != null) {
            root.xRot = 0.0f;
            root.yRot = 0.0f;
            root.zRot = 0.0f;
        }
    }

    private static void bind(ModelPart part, float x, float y, float z) {
        part.x = x;
        part.y = y;
        part.z = z;
        part.xRot = 0.0f;
        part.yRot = 0.0f;
        part.zRot = 0.0f;
    }

    private static void addRot(ModelPart part, float x, float y, float z) {
        part.xRot += x;
        part.yRot += y;
        part.zRot += z;
    }

    private static void shift(ModelPart part, float x, float y, float z) {
        part.x += x;
        part.y += y;
        part.z += z;
    }

    private static void applyHeadBone(
            ModelPart head,
            BedrockAnimationRegistry.BoneTrack track,
            float time,
            float weight,
            boolean overrideLook
    ) {
        if (head == null) return;

        // Vanilla look angles already calculated by Minecraft setupAnim
        float vanillaPitch = head.xRot;
        float vanillaYaw = head.yRot;

        if (track != null) {
            float[] rot = track.sampleRotation(time);
            float radX = (float) Math.toRadians(rot[0]);
            float radY = (float) Math.toRadians(rot[1]);
            float radZ = (float) Math.toRadians(rot[2]);

            if (overrideLook) {
                head.xRot = Mth.lerp(weight, head.xRot, radX);
                head.yRot = Mth.lerp(weight, head.yRot, radY);
                head.zRot = Mth.lerp(weight, head.zRot, radZ);
            } else {
                // Free head rotation: preserve vanilla look pitch & yaw (independent of body up to ±75 deg)
                head.xRot = vanillaPitch + (radX * weight);
                head.yRot = vanillaYaw + (radY * weight);
                head.zRot = radZ * weight;
            }

            float[] pos = track.samplePosition(time);
            if (pos[0] != 0.0f || pos[1] != 0.0f || pos[2] != 0.0f) {
                head.x += pos[0] * weight;
                head.y += -pos[1] * weight;
                head.z += pos[2] * weight;
            }
        }
    }

    private static void applyBone(
            ModelPart part,
            BedrockAnimationRegistry.BoneTrack track,
            float time,
            float weight
    ) {
        if (part == null || track == null) return;

        float[] rot = track.sampleRotation(time);
        float radX = (float) Math.toRadians(rot[0]);
        float radY = (float) Math.toRadians(rot[1]);
        float radZ = (float) Math.toRadians(rot[2]);

        part.xRot = Mth.lerp(weight, part.xRot, radX);
        part.yRot = Mth.lerp(weight, part.yRot, radY);
        part.zRot = Mth.lerp(weight, part.zRot, radZ);

        float[] pos = track.samplePosition(time);
        if (pos[0] != 0.0f || pos[1] != 0.0f || pos[2] != 0.0f) {
            part.x += pos[0] * weight;
            part.y += -pos[1] * weight;
            part.z += pos[2] * weight;
        }
    }
}
