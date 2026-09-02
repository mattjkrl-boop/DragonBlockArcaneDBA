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
            if (sColor != 0) skinColor = sColor;
            if (hColor != 0) hairColor = hColor;
            if (eColor != 0) eyeColor = eColor;

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

        // Uniform dynamic texture generation for all races (Yardrat, Saiyan, Half-Saiyan, etc.)
        Identifier texture = DynamicSkinManager.getOrGenerateSkin(race, skinColor, hairColor, eyeColor);
        if (texture == null) {
            texture = Identifier.fromNamespaceAndPath("dragonblockarcanedba", "textures/entity/player/" + race + "_base.png");
        }

        RenderType renderType = RenderTypes.entityCutout(texture, false);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            PoseStack stack = new PoseStack();
            stack.last().pose().set(pose.pose());
            stack.last().normal().set(pose.normal());

            renderLimb(mesh.getLimb("head"), humanoidModel.head, stack, buffer, packedLight, 1.0f, 1.0f, 1.0f, 1.0f);
            renderLimb(mesh.getLimb("body"), humanoidModel.body, stack, buffer, packedLight, 1.0f, 1.0f, 1.0f, 1.0f);
            renderLimb(mesh.getLimb("rightarm"), humanoidModel.rightArm, stack, buffer, packedLight, 1.0f, 1.0f, 1.0f, 1.0f);
            renderLimb(mesh.getLimb("leftarm"), humanoidModel.leftArm, stack, buffer, packedLight, 1.0f, 1.0f, 1.0f, 1.0f);
            renderLimb(mesh.getLimb("rightleg"), humanoidModel.rightLeg, stack, buffer, packedLight, 1.0f, 1.0f, 1.0f, 1.0f);
            renderLimb(mesh.getLimb("leftleg"), humanoidModel.leftLeg, stack, buffer, packedLight, 1.0f, 1.0f, 1.0f, 1.0f);
        });

        // 3D Dynamic Hair Spikes for Saiyan, Half-Saiyan, and humanoid races
        if (com.dragonblockarcanedba.client.render.model.DbaHairRenderer.hasCodedHair(race)) {
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
                humanoidModel.head.translateAndRotate(stack);
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
            net.minecraft.client.model.geom.ModelPart bone,
            PoseStack stack,
            com.mojang.blaze3d.vertex.VertexConsumer buffer,
            int packedLight,
            float r, float g, float b, float a
    ) {
        if (limb != null && bone != null) {
            stack.pushPose();
            bone.translateAndRotate(stack);
            limb.render(stack.last().pose(), buffer, packedLight, OverlayTexture.NO_OVERLAY, r, g, b, a);
            stack.popPose();
        }
    }
}
