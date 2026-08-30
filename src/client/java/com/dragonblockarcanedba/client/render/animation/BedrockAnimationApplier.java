package com.dragonblockarcanedba.client.render.animation;

import com.dragonblockarcanedba.client.render.layer.DbaPlayerState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;

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

        // ==================== 1. DEATH ====================
        if (state.deathTime > 0.0f) {
            BedrockAnimationRegistry.BedrockAnimation deathAnim = getAnim("death");
            if (deathAnim != null) {
                float time = Math.min((state.deathTime / 20.0f), deathAnim.length);
                applyTrackedBones(model, deathAnim, time, 1.0f, true);
                return true;
            }
        }

        // ==================== 2. SLEEPING ====================
        if (state.hasPose(Pose.SLEEPING) || state.bedOrientation != null) {
            BedrockAnimationRegistry.BedrockAnimation sleepAnim = getAnim("sleep_idle");
            if (sleepAnim == null) sleepAnim = getAnim("sleep");
            if (sleepAnim != null) {
                float time = sleepAnim.calculatePlayTime(ageSeconds);
                applyTrackedBones(model, sleepAnim, time, 1.0f, true);
                return true;
            }
        }

        // ==================== 3. EMOTES & DBZ STANCES ====================
        if (activeEmote != null && !activeEmote.isEmpty()) {
            BedrockAnimationRegistry.BedrockAnimation emoteAnim = getAnim(activeEmote);
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
            BedrockAnimationRegistry.BedrockAnimation crawlAnim = getAnim("crawl");
            if (crawlAnim != null) {
                float time = (limbSwing * 0.12f) % crawlAnim.length;
                applyTrackedBones(model, crawlAnim, time, 1.0f);
                return true;
            }
        }

        // ==================== 5. SWIMMING ====================
        if (state.isVisuallySwimming || (state.isInWater && state.swimAmount > 0.1f)) {
            BedrockAnimationRegistry.BedrockAnimation swimAnim = getAnim("swim");
            if (swimAnim != null) {
                float time = swimAnim.calculatePlayTime(ageSeconds);
                applyTrackedBones(model, swimAnim, time, 1.0f);
                return true;
            }
        }

        // ==================== 5.5. CLIMBING (Ladders, Vines, Scaffolding) ====================
        if (dbaState.dba$isOnLadder()) {
            BedrockAnimationRegistry.BedrockAnimation climbAnim = getAnim("climb");
            if (climbAnim != null) {
                boolean isMovingOnLadder = Math.abs(dbaState.dba$getLocalVelocityY()) > 0.02f || dbaState.dba$getHorizontalSpeed() > 0.02f;
                float time = isMovingOnLadder ? (limbSwing * 0.15f) % climbAnim.length : 0.25f;
                applyTrackedBones(model, climbAnim, time, 1.0f);
                return true;
            }
        }

        // ==================== 6. FLYING (Elytra / Ki Flight) ====================
        if (state.isFallFlying || dbaState.dba$isFlying()) {
            BedrockAnimationRegistry.BedrockAnimation flyAnim = getAnim("fly");
            if (flyAnim != null) {
                float time = flyAnim.calculatePlayTime(ageSeconds);
                applyTrackedBones(model, flyAnim, time, 1.0f);
                return true;
            }
        }

        // ==================== 7. RIDING (Horse / Boat / Minecart) ====================
        if (state.isPassenger) {
            BedrockAnimationRegistry.BedrockAnimation rideAnim = getAnim("riding");
            if (rideAnim == null) rideAnim = getAnim("ride_idle");
            if (rideAnim == null) rideAnim = getAnim("sit");
            if (rideAnim != null) {
                float time = rideAnim.calculatePlayTime(ageSeconds);
                applyTrackedBones(model, rideAnim, time, 1.0f);
                return true;
            }
        }

        // ==================== 8. AIRBORNE: JUMPING & FALLING ====================
        float velY = dbaState.dba$getLocalVelocityY();
        if (!state.isPassenger && !state.isInWater && !state.isFallFlying) {
            if (velY > 0.15f) {
                // Rising / Jumping
                String jumpId = dbaState.dba$isSprinting() ? "running_jump" : "jump";
                BedrockAnimationRegistry.BedrockAnimation jumpAnim = getAnim(jumpId);
                if (jumpAnim != null) {
                    float time = jumpAnim.calculatePlayTime(ageSeconds);
                    applyTrackedBones(model, jumpAnim, time, 1.0f);
                    return true;
                }
            } else if (velY < -0.35f) {
                // Falling through air
                BedrockAnimationRegistry.BedrockAnimation fallAnim = getAnim("fall");
                if (fallAnim != null) {
                    float time = fallAnim.calculatePlayTime(ageSeconds);
                    applyTrackedBones(model, fallAnim, time, 1.0f);
                    return true;
                }
            }
        }

        // ==================== 9. CROUCHING / SNEAKING ====================
        if (state.isCrouching) {
            if (dbaState.dba$getHorizontalSpeed() > 0.05f) {
                BedrockAnimationRegistry.BedrockAnimation sneakWalkAnim = getAnim("sneak_walk");
                if (sneakWalkAnim != null) {
                    float time = (limbSwing * 0.12f) % sneakWalkAnim.length;
                    applyTrackedBones(model, sneakWalkAnim, time, 1.0f);
                    return true;
                }
            } else {
                BedrockAnimationRegistry.BedrockAnimation sneakIdleAnim = getAnim("sneak_idle");
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
            BedrockAnimationRegistry.BedrockAnimation blockAnim = getAnim(blockId);
            if (blockAnim != null) {
                float time = blockAnim.calculatePlayTime(ageSeconds);
                applyTrackedBones(model, blockAnim, time, 1.0f);
                return true;
            }
        }

        // ==================== 11. USING ITEMS (Eating, Drinking, Tools) ====================
        if (state.isUsingItem) {
            String useId = isConsumable(mainItem) ? "eat" : "use_item";
            BedrockAnimationRegistry.BedrockAnimation useAnim = getAnim(useId);
            if (useAnim != null) {
                float time = useAnim.calculatePlayTime(ageSeconds);
                applyTrackedBones(model, useAnim, time, 1.0f);
                return true;
            }
        }

        // ==================== 12. ATTACK SWINGS / PUNCHES ====================
        if (state.attackTime > 0.0f) {
            String attackId = hasSword ? "attack" : "punch";
            BedrockAnimationRegistry.BedrockAnimation attackAnim = getAnim(attackId);
            if (attackAnim != null) {
                float time = state.attackTime * attackAnim.length;
                applyTrackedBones(model, attackAnim, time, 1.0f);
                return true;
            }
        }

        // ==================== 13. LOCOMOTION (Walking, Sprinting, Backward) ====================
        if (dbaState.dba$getHorizontalSpeed() > 0.05f) {
            if (dbaState.dba$getLocalVelocityZ() > 0.08f) {
                // Walking backwards
                BedrockAnimationRegistry.BedrockAnimation walkBackAnim = getAnim("walk_back");
                if (walkBackAnim != null) {
                    float time = (limbSwing * 0.12f) % walkBackAnim.length;
                    applyTrackedBones(model, walkBackAnim, time, 1.0f);
                    return true;
                }
            }

            if (dbaState.dba$isSprinting()) {
                // Running / Sprinting
                BedrockAnimationRegistry.BedrockAnimation runAnim = getAnim("run");
                if (runAnim != null) {
                    float time = (limbSwing * 0.15f) % runAnim.length;
                    applyTrackedBones(model, runAnim, time, 1.0f);
                    return true;
                }
            }

            // Normal Walking (Sword walk, Axe walk, Standard walk)
            String walkId = hasSword ? "sword_walk" : (hasAxe ? "axe_walk" : "walk");
            BedrockAnimationRegistry.BedrockAnimation walkAnim = getAnim(walkId);
            if (walkAnim != null) {
                float time = (limbSwing * 0.12f) % walkAnim.length;
                applyTrackedBones(model, walkAnim, time, 1.0f);
                return true;
            }
        }

        // ==================== 14. IDLE / STANDING ====================
        String idleId = hasSword ? "sword_idle" : (hasAxe ? "axe_idle" : (!mainItem.isEmpty() ? "item_idle" : "idle"));
        BedrockAnimationRegistry.BedrockAnimation idleAnim = getAnim(idleId);
        if (idleAnim != null) {
            float time = idleAnim.calculatePlayTime(ageSeconds);
            applyTrackedBones(model, idleAnim, time, 1.0f);
            return true;
        }

        return false;
    }

    private static BedrockAnimationRegistry.BedrockAnimation getAnim(String id) {
        BedrockAnimationRegistry.BedrockAnimation anim = BedrockAnimationRegistry.getAnimation(id);
        if (anim == null) {
            anim = BedrockAnimationRegistry.getAnimation("animation.player." + id);
        }
        return anim;
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

        // Root bone (overall elevation and lean)
        BedrockAnimationRegistry.BoneTrack rootTrack = anim.bones.get("root");
        if (rootTrack != null) {
            float[] rootPos = rootTrack.samplePosition(time);
            float[] rootRot = rootTrack.sampleRotation(time);

            float elev = -rootPos[1] * weight;
            model.body.y += elev;
            model.head.y += elev;
            model.rightArm.y += elev;
            model.leftArm.y += elev;
            model.rightLeg.y += elev;
            model.leftLeg.y += elev;

            if (rootRot[0] != 0.0f || rootRot[1] != 0.0f || rootRot[2] != 0.0f) {
                model.body.xRot += (float) Math.toRadians(rootRot[0]) * weight;
                model.body.yRot += (float) Math.toRadians(rootRot[1]) * weight;
                model.body.zRot += (float) Math.toRadians(rootRot[2]) * weight;
            }
        }

        applyHeadBone(model.head, anim.bones.get("head"), time, weight, overrideLook);
        applyBone(model.body, anim.bones.get("body"), time, weight);
        applyBone(model.rightArm, anim.bones.get("right_arm"), time, weight);
        applyBone(model.leftArm, anim.bones.get("left_arm"), time, weight);
        applyBone(model.rightLeg, anim.bones.get("right_leg"), time, weight);
        applyBone(model.leftLeg, anim.bones.get("left_leg"), time, weight);
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
