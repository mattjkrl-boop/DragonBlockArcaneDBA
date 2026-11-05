package com.dragonblockarcanedba.client.render.layer;

import com.dragonblockarcanedba.client.render.DynamicSkinManager;
import com.dragonblockarcanedba.client.render.model.Custom3DModelRegistry;
import com.dragonblockarcanedba.client.render.model.ObjMesh;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Universal Native 3D Polygonal Model Layer for Minecraft 26.2.
 * Directly renders Wavefront OBJ polygonal geometry attached to humanoid skeletal limbs.
 */
public class Custom3DModelLayer extends RenderLayer<AvatarRenderState, EntityModel<AvatarRenderState>> {

    public Custom3DModelLayer(RenderLayerParent<AvatarRenderState, EntityModel<AvatarRenderState>> renderer) {
        super(renderer);
    }

    @Override
    public void submit(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int packedLight,
            AvatarRenderState state,
            float yRot,
            float xRot
    ) {
        if (!(this.getParentModel() instanceof HumanoidModel<?> humanoidModel)) {
            return;
        }

        String race = "universal_humanoid";
        int skinColor = 0xFFE0BD;
        int hairColor = 0xFF1EB4FF;
        int eyeColor = 0xFFFFFF;
        Identifier activeFormId = null;

        if (state instanceof DbaPlayerState dbaState) {
            Identifier raceId = dbaState.dba$getRaceId();
            if (raceId != null) {
                race = raceId.getPath().toLowerCase();
            }
            activeFormId = dbaState.dba$getActiveFormId();
            int sColor = dbaState.dba$getSkinColor();
            int hColor = dbaState.dba$getHairColor();
            int eColor = dbaState.dba$getEyeColor();
            if (sColor != 0) skinColor = sColor & 0xFFFFFF;
            if (hColor != 0) hairColor = hColor & 0xFFFFFF;
            // 0xFF000000 is chosen black. Only an unset color (0) keeps the white default.
            if (eColor != 0) eyeColor = eColor & 0xFFFFFF;

            com.dragonblockarcanedba.client.render.animation.BedrockAnimationApplier.apply(
                humanoidModel,
                state,
                dbaState,
                state.walkAnimationPos,
                state.walkAnimationSpeed
            );
        }

        ObjMesh mesh = Custom3DModelRegistry.getModelForRace(race);
        if (mesh == null) {
            return;
        }

        // Hide vanilla box cuboids for 3D model races so they do not clip inside the 3D model
        humanoidModel.head.visible = false;
        humanoidModel.hat.visible = false;
        humanoidModel.body.visible = false;
        humanoidModel.rightArm.visible = false;
        humanoidModel.leftArm.visible = false;
        humanoidModel.rightLeg.visible = false;
        humanoidModel.leftLeg.visible = false;
        if (humanoidModel instanceof net.minecraft.client.model.player.PlayerModel playerModel) {
            playerModel.leftSleeve.visible = false;
            playerModel.rightSleeve.visible = false;
            playerModel.leftPants.visible = false;
            playerModel.rightPants.visible = false;
            playerModel.jacket.visible = false;
        }

        // The face atlas already contains the eyes. A short blink swaps that atlas for the
        // closed-eyelid atlas, which differs only in those eye texels. Eye color tints both.
        boolean eyesClosed = (System.currentTimeMillis() + state.id * 12345L) % 4000L < 150L;
        Identifier texture = DynamicSkinManager.getOrGenerateSkin(race, skinColor, hairColor, eyeColor, eyesClosed);
        if (texture == null) {
            texture = Identifier.fromNamespaceAndPath("dragonblockarcanedba", "textures/entity/player/" + race + "_base.png");
        }

        RenderType renderType = RenderTypes.entityCutout(texture, false);

        // The custom geometry runs later in the frame. setupAnim can run again
        // before that and put the vanilla one-arm swing back on the shared model.
        // Freeze the pose that the movement clip just wrote.
        final LimbPose headPose = LimbPose.capture(humanoidModel.head);
        final LimbPose bodyPose = LimbPose.capture(humanoidModel.body);
        final LimbPose rightArmPose = LimbPose.capture(humanoidModel.rightArm);
        final LimbPose leftArmPose = LimbPose.capture(humanoidModel.leftArm);
        final LimbPose rightLegPose = LimbPose.capture(humanoidModel.rightLeg);
        final LimbPose leftLegPose = LimbPose.capture(humanoidModel.leftLeg);

        final Identifier finalFormForHair = activeFormId;
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            PoseStack stack = new PoseStack();
            stack.last().pose().set(pose.pose());
            stack.last().normal().set(pose.normal());

            renderLimb(mesh.getLimb("head"), headPose, stack, buffer, packedLight, 1.0f, 1.0f, 1.0f, 1.0f);
            // Extra head groups ("head_<part>") follow the head. A form variant "head_<part>__<formpath>"
            // replaces "head_<part>" while that form is active (Arcosian horns per form).
            String headFormPath = finalFormForHair != null ? finalFormForHair.getPath() : null;
            for (String gname : mesh.getLimbNames()) {
                if (!gname.startsWith("head_")) continue;
                int split = gname.indexOf("__");
                if (split >= 0) {
                    if (headFormPath == null || !gname.substring(split + 2).equals(headFormPath)) continue;
                } else if (headFormPath != null && mesh.getLimb(gname + "__" + headFormPath) != null) {
                    continue;
                }
                renderLimb(mesh.getLimb(gname), headPose, stack, buffer, packedLight, 1.0f, 1.0f, 1.0f, 1.0f);
            }
            // Per-form modelled hair: SSJ3 swaps in the long mane group when the model has one
            ObjMesh.LimbGroup hairLimb = mesh.getLimb("hair");
            if (finalFormForHair != null && finalFormForHair.getPath().contains("super_saiyan_3") && mesh.getLimb("hair_ssj3") != null) {
                hairLimb = mesh.getLimb("hair_ssj3");
            } else if (finalFormForHair != null && finalFormForHair.getPath().contains("super_saiyan_2") && mesh.getLimb("hair_ssj2") != null) {
                hairLimb = mesh.getLimb("hair_ssj2");
            }
            if (hairLimb != null) {
                // Subtle hair sway: slow idle breathing bob plus a bounce that follows the walk cycle,
                // pivoting at the middle of the head so the hair stays seated on the scalp
                float age = state.ageInTicks;
                float walk = Math.min(1.0f, state.walkAnimationSpeed);
                float swayX = (float) (Math.sin(age * 0.08f) * 0.025f + Math.abs(Math.sin(state.walkAnimationPos * 0.6662f)) * 0.06f * walk);
                float swayZ = (float) (Math.sin(age * 0.05f + 1.3f) * 0.02f + Math.sin(state.walkAnimationPos * 0.6662f) * 0.03f * walk);
                stack.pushPose();
                headPose.apply(stack);
                stack.translate(0.0f, -0.25f, 0.0f);
                stack.mulPose(new org.joml.Matrix4f().rotation(com.mojang.math.Axis.XP.rotation(swayX)));
                stack.mulPose(new org.joml.Matrix4f().rotation(com.mojang.math.Axis.ZP.rotation(swayZ)));
                stack.translate(0.0f, 0.25f, 0.0f);
                hairLimb.render(stack.last().pose(), buffer, packedLight, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
                stack.popPose();
            }
            renderLimb(mesh.getLimb("body"), bodyPose, stack, buffer, packedLight, 1.0f, 1.0f, 1.0f, 1.0f);
            // Modelled-hair races (blocky Saiyan) hang their arms slightly away from the body
            float armSplay = mesh.hasModelledHair() ? 0.16f : 0.0f;
            renderLimb(mesh.getLimb("rightarm"), rightArmPose, stack, buffer, packedLight, armSplay, 1.0f, 1.0f, 1.0f, 1.0f);
            renderLimb(mesh.getLimb("leftarm"), leftArmPose, stack, buffer, packedLight, -armSplay, 1.0f, 1.0f, 1.0f, 1.0f);
            renderLimb(mesh.getLimb("rightleg"), rightLegPose, stack, buffer, packedLight, 1.0f, 1.0f, 1.0f, 1.0f);
            renderLimb(mesh.getLimb("leftleg"), leftLegPose, stack, buffer, packedLight, 1.0f, 1.0f, 1.0f, 1.0f);
        });

        // 3D Dynamic Hair Spikes for Saiyan, Half-Saiyan, and humanoid races
        // Models with their own hair cubes (hair group, tinted through the mask) skip the coded hair so it does not double up
        if (com.dragonblockarcanedba.client.render.model.DbaHairRenderer.hasCodedHair(race) && !mesh.hasModelledHair()) {
            final String finalRace = race;
            final Identifier finalActiveFormId = activeFormId;
            final int finalHairColor = hairColor;
            final DbaPlayerState finalDbaState = (state instanceof DbaPlayerState d) ? d : null;

            RenderType hairRenderType = com.dragonblockarcanedba.client.render.model.DbaHairRenderer.getHairRenderType();
            collector.submitCustomGeometry(poseStack, hairRenderType, (pose, buffer) -> {
                PoseStack stack = new PoseStack();
                stack.last().pose().set(pose.pose());
                stack.last().normal().set(pose.normal());

                stack.pushPose();
                headPose.apply(stack);
                com.dragonblockarcanedba.client.render.model.DbaHairRenderer.renderHair(
                        stack.last().pose(), buffer, packedLight, OverlayTexture.NO_OVERLAY,
                        finalRace, finalActiveFormId, finalHairColor, state, finalDbaState
                );
                stack.popPose();
            });
        }
    }

    private static void renderLimb(
            ObjMesh.LimbGroup limb,
            LimbPose bone,
            PoseStack stack,
            com.mojang.blaze3d.vertex.VertexConsumer buffer,
            int packedLight,
            float r, float g, float b, float a
    ) {
        renderLimb(limb, bone, stack, buffer, packedLight, 0.0f, r, g, b, a);
    }

    private static void renderLimb(
            ObjMesh.LimbGroup limb,
            LimbPose bone,
            PoseStack stack,
            com.mojang.blaze3d.vertex.VertexConsumer buffer,
            int packedLight,
            float extraZRot,
            float r, float g, float b, float a
    ) {
        if (limb != null && bone != null) {
            stack.pushPose();
            bone.apply(stack);
            if (extraZRot != 0.0f) stack.mulPose(new org.joml.Matrix4f().rotation(com.mojang.math.Axis.ZP.rotation(extraZRot)));
            limb.render(stack.last().pose(), buffer, packedLight, OverlayTexture.NO_OVERLAY, r, g, b, a);
            stack.popPose();
        }
    }

    /**
     * Copy of a humanoid part's transform, taken while the movement clip is
     * still on the model. Matches ModelPart.translateAndRotate.
     */
    private static final class LimbPose {
        final float x;
        final float y;
        final float z;
        final float xRot;
        final float yRot;
        final float zRot;

        private LimbPose(float x, float y, float z, float xRot, float yRot, float zRot) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.xRot = xRot;
            this.yRot = yRot;
            this.zRot = zRot;
        }

        static LimbPose capture(net.minecraft.client.model.geom.ModelPart part) {
            return new LimbPose(part.x, part.y, part.z, part.xRot, part.yRot, part.zRot);
        }

        void apply(PoseStack stack) {
            stack.translate(this.x / 16.0f, this.y / 16.0f, this.z / 16.0f);
            if (this.xRot != 0.0f || this.yRot != 0.0f || this.zRot != 0.0f) {
                stack.mulPose(new org.joml.Matrix4f().rotation(new org.joml.Quaternionf().rotationZYX(this.zRot, this.yRot, this.xRot)));
            }
        }
    }
}
