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
        int hairColor = 0xFFF08C;

        if (state instanceof DbaPlayerState dbaState) {
            Identifier raceId = dbaState.dba$getRaceId();
            if (raceId != null) {
                race = raceId.getPath().toLowerCase();
            }
            int sColor = dbaState.dba$getSkinColor();
            int hColor = dbaState.dba$getHairColor();
            if (sColor != 0) skinColor = sColor;
            if (hColor != 0) hairColor = hColor;

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

        // Hide vanilla box cuboids so they do not clip inside the 3D model
        humanoidModel.head.visible = false;
        humanoidModel.hat.visible = false;
        humanoidModel.body.visible = false;
        humanoidModel.rightArm.visible = false;
        humanoidModel.leftArm.visible = false;
        humanoidModel.rightLeg.visible = false;
        humanoidModel.leftLeg.visible = false;

        // 3D polygonal model texture
        Identifier texture = Identifier.fromNamespaceAndPath("dragonblockarcanedba", "textures/entity/model_3d/texture.png");
        RenderType renderType = RenderTypes.entityCutout(texture, false);

        float r = ((skinColor >> 16) & 0xFF) / 255.0f;
        float g = ((skinColor >> 8) & 0xFF) / 255.0f;
        float b = (skinColor & 0xFF) / 255.0f;
        float a = 1.0f;

        if (skinColor == 0) {
            r = 0.85f;
            g = 0.85f;
            b = 0.85f;
        }

        final float finalR = r;
        final float finalG = g;
        final float finalB = b;
        final float finalA = a;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            PoseStack stack = new PoseStack();
            stack.last().pose().set(pose.pose());
            stack.last().normal().set(pose.normal());

            // 1. Head (Cranium, Pointed 3D Ears, Face)
            ObjMesh.LimbGroup headLimb = mesh.getLimb("head");
            if (headLimb != null) {
                stack.pushPose();
                humanoidModel.head.translateAndRotate(stack);
                headLimb.render(stack.last().pose(), buffer, packedLight, OverlayTexture.NO_OVERLAY, finalR, finalG, finalB, finalA);
                stack.popPose();
            }

            // 2. Torso / Body
            ObjMesh.LimbGroup bodyLimb = mesh.getLimb("body");
            if (bodyLimb != null) {
                stack.pushPose();
                humanoidModel.body.translateAndRotate(stack);
                bodyLimb.render(stack.last().pose(), buffer, packedLight, OverlayTexture.NO_OVERLAY, finalR, finalG, finalB, finalA);
                stack.popPose();
            }

            // 3. Right Arm
            ObjMesh.LimbGroup rightArmLimb = mesh.getLimb("rightarm");
            if (rightArmLimb != null) {
                stack.pushPose();
                humanoidModel.rightArm.translateAndRotate(stack);
                rightArmLimb.render(stack.last().pose(), buffer, packedLight, OverlayTexture.NO_OVERLAY, finalR, finalG, finalB, finalA);
                stack.popPose();
            }

            // 4. Left Arm
            ObjMesh.LimbGroup leftArmLimb = mesh.getLimb("leftarm");
            if (leftArmLimb != null) {
                stack.pushPose();
                humanoidModel.leftArm.translateAndRotate(stack);
                leftArmLimb.render(stack.last().pose(), buffer, packedLight, OverlayTexture.NO_OVERLAY, finalR, finalG, finalB, finalA);
                stack.popPose();
            }

            // 5. Right Leg
            ObjMesh.LimbGroup rightLegLimb = mesh.getLimb("rightleg");
            if (rightLegLimb != null) {
                stack.pushPose();
                humanoidModel.rightLeg.translateAndRotate(stack);
                rightLegLimb.render(stack.last().pose(), buffer, packedLight, OverlayTexture.NO_OVERLAY, finalR, finalG, finalB, finalA);
                stack.popPose();
            }

            // 6. Left Leg
            ObjMesh.LimbGroup leftLegLimb = mesh.getLimb("leftleg");
            if (leftLegLimb != null) {
                stack.pushPose();
                humanoidModel.leftLeg.translateAndRotate(stack);
                leftLegLimb.render(stack.last().pose(), buffer, packedLight, OverlayTexture.NO_OVERLAY, finalR, finalG, finalB, finalA);
                stack.popPose();
            }
        });
    }
}
