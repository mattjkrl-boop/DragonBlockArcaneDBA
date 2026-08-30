package com.dragonblockarcanedba.client.render.animation;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Procedural Dragon Block Arcane animation engine for emotes and combat stances.
 * Directly shapes the skeletal joints of HumanoidModel, seamlessly animating
 * the native 3D OBJ mesh, trailing tail, weapons, and accessories.
 */
public final class EmoteAnimationHelper {

    private EmoteAnimationHelper() {}

    public static void apply(HumanoidModel<?> model, String emote, float age) {
        if (model == null || emote == null || emote.isEmpty()) {
            return;
        }

        ModelPart head = model.head;
        ModelPart body = model.body;
        ModelPart rightArm = model.rightArm;
        ModelPart leftArm = model.leftArm;
        ModelPart rightLeg = model.rightLeg;
        ModelPart leftLeg = model.leftLeg;

        switch (emote.toLowerCase()) {
            case "shout", "ki_power_shout" -> {
                // Classic Dragon Ball Ki Power-Up / Super Saiyan Roar
                // Deep rooted martial stance, elbows back, fists clenched, head screaming at the sky
                float tremor = (Mth.sin(age * 1.8F) > 0 ? 0.035F : -0.035F);
                
                body.xRot = 0.18F + tremor;
                head.xRot = -0.72F + tremor * 0.5F;
                head.yRot = tremor * 0.4F;

                // Arms flexed back with clenched fists vibrating with Ki
                rightArm.xRot = 0.55F + tremor;
                rightArm.yRot = -0.35F;
                rightArm.zRot = 0.45F + tremor;

                leftArm.xRot = 0.55F - tremor;
                leftArm.yRot = 0.35F;
                leftArm.zRot = -0.45F - tremor;

                // Wide grounded power stance
                rightLeg.xRot = -0.25F;
                rightLeg.zRot = 0.28F;
                leftLeg.xRot = -0.25F;
                leftLeg.zRot = -0.28F;
            }

            case "sit", "meditation" -> {
                // Ascended Spiritual Meditation / Lotus Float
                // Legs folded inward, hovering serenely above ground, hands resting palms-up
                float hover = Mth.sin(age * 0.08F) * 1.5F;
                
                body.y += -3.0F + hover;
                head.y += -3.0F + hover;
                rightArm.y += -3.0F + hover;
                leftArm.y += -3.0F + hover;
                rightLeg.y += -3.0F + hover;
                leftLeg.y += -3.0F + hover;

                head.xRot = 0.12F; // Calm downward meditative gaze

                // Full lotus leg fold
                rightLeg.xRot = -1.45F;
                rightLeg.yRot = 0.65F;
                rightLeg.zRot = 0.35F;

                leftLeg.xRot = -1.45F;
                leftLeg.yRot = -0.65F;
                leftLeg.zRot = -0.35F;

                // Hands resting resting gracefully on knees
                rightArm.xRot = -0.65F;
                rightArm.yRot = -0.25F;
                rightArm.zRot = -0.20F;

                leftArm.xRot = -0.65F;
                leftArm.yRot = 0.25F;
                leftArm.zRot = 0.20F;
            }

            case "wave" -> {
                // Friendly Martial Artist Salute / Greeting Wave
                float waveSway = Mth.sin(age * 0.35F) * 0.45F;
                
                rightArm.xRot = -2.65F;
                rightArm.zRot = 0.35F + waveSway;
                rightArm.yRot = -0.25F;

                leftArm.xRot = 0.05F;
                leftArm.zRot = -0.10F;

                head.zRot = Mth.sin(age * 0.12F) * 0.08F;
            }

            case "dance" -> {
                // Victory Dance / Fusion Dance Rhythm
                float bounce = Math.abs(Mth.sin(age * 0.25F)) * -2.2F;
                float sway = Mth.sin(age * 0.20F) * 0.25F;

                body.y += bounce;
                head.y += bounce;
                rightArm.y += bounce;
                leftArm.y += bounce;
                rightLeg.y += bounce;
                leftLeg.y += bounce;

                body.zRot = sway;
                head.zRot = -sway * 0.6F;

                rightArm.xRot = -1.25F + Mth.sin(age * 0.25F) * 0.65F;
                rightArm.zRot = 0.55F;
                leftArm.xRot = -1.25F - Mth.sin(age * 0.25F) * 0.65F;
                leftArm.zRot = -0.55F;

                rightLeg.xRot = Mth.sin(age * 0.25F) * 0.35F;
                leftLeg.xRot = -Mth.sin(age * 0.25F) * 0.35F;
            }

            case "arm_parry", "sword_parry", "guard" -> {
                // Classic Dragon Ball Forearm Cross Guard
                body.xRot = 0.15F;
                head.xRot = 0.20F;

                // Both forearms crossed tightly before chest/face
                rightArm.xRot = -1.25F;
                rightArm.yRot = -0.55F;
                rightArm.zRot = -0.38F;

                leftArm.xRot = -1.25F;
                leftArm.yRot = 0.55F;
                leftArm.zRot = 0.38F;

                rightLeg.xRot = -0.20F;
                leftLeg.xRot = 0.20F;
            }

            case "sword_idle", "blade_stance" -> {
                // Low-Center-of-Gravity Kendo / Martial Arts Ready Stance
                body.yRot = 0.42F;
                head.yRot = -0.38F; // Eyes remain locked directly at opponent

                leftArm.xRot = -0.85F;
                leftArm.yRot = 0.40F;
                leftArm.zRot = -0.28F;

                rightArm.xRot = -0.35F;
                rightArm.yRot = -0.20F;
                rightArm.zRot = 0.22F;

                rightLeg.xRot = -0.38F;
                rightLeg.yRot = 0.25F;
                leftLeg.xRot = 0.38F;
                leftLeg.yRot = 0.25F;
            }

            case "kick_right" -> {
                // Dynamic Martial Arts High Roundhouse Kick
                body.zRot = -0.42F;
                body.xRot = -0.12F;
                head.zRot = 0.28F;

                // High extended roundhouse kick
                rightLeg.xRot = -1.70F;
                rightLeg.zRot = 0.42F;
                rightLeg.yRot = 0.22F;

                // Planted support leg
                leftLeg.xRot = 0.25F;
                leftLeg.zRot = 0.12F;

                // Balance arms
                rightArm.xRot = -0.82F;
                rightArm.zRot = -0.32F;
                leftArm.xRot = -0.72F;
                leftArm.zRot = 0.42F;
            }

            case "cross_punch_right" -> {
                // Heavy Lunging Straight Right Cross Punch
                body.yRot = -0.48F;
                body.xRot = 0.16F;
                head.yRot = 0.42F;

                // Fully extended punch
                rightArm.xRot = -1.58F;
                rightArm.yRot = -0.18F;
                rightArm.zRot = 0.06F;

                // Guarding left fist at chin
                leftArm.xRot = -1.15F;
                leftArm.yRot = 0.52F;
                leftArm.zRot = 0.36F;

                leftLeg.xRot = -0.42F;
                rightLeg.xRot = 0.42F;
            }

            case "talk" -> {
                // Expressive Conversational Gestures
                head.xRot = Mth.sin(age * 0.15F) * 0.12F;
                head.yRot = Mth.cos(age * 0.09F) * 0.18F;

                rightArm.xRot = -0.70F + Mth.sin(age * 0.22F) * 0.28F;
                rightArm.zRot = 0.28F;
                leftArm.xRot = -0.55F + Mth.cos(age * 0.18F) * 0.22F;
                leftArm.zRot = -0.28F;
            }

            case "zombie_walk" -> {
                // Undead Stumble
                rightArm.xRot = -1.50F + Mth.sin(age * 0.10F) * 0.06F;
                leftArm.xRot = -1.50F - Mth.sin(age * 0.10F) * 0.06F;
                head.zRot = 0.22F;
                head.xRot = 0.12F;
                body.zRot = Mth.sin(age * 0.10F) * 0.08F;
            }
        }
    }
}
