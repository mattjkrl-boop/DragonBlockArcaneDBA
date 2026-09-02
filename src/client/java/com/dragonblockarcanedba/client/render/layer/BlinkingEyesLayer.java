package com.dragonblockarcanedba.client.render.layer;

import com.dragonblockarcanedba.config.RaceConfig;
import com.dragonblockarcanedba.config.RaceConfigManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class BlinkingEyesLayer extends RenderLayer<AvatarRenderState, EntityModel<AvatarRenderState>> {

    private static final Identifier EYES_OPEN = Identifier.fromNamespaceAndPath("dragonblockarcanedba", "textures/entity/player/eyes_open.png");
    private static final Identifier EYES_CLOSED = Identifier.fromNamespaceAndPath("dragonblockarcanedba", "textures/entity/player/eyes_closed.png");

    private final ModelPart eyeOverlay;

    public BlinkingEyesLayer(RenderLayerParent<AvatarRenderState, EntityModel<AvatarRenderState>> renderer) {
        super(renderer);
        this.eyeOverlay = createEyeOverlay();
    }

    private ModelPart createEyeOverlay() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // A flat plane sitting precisely on the front face of the head (Z = -4.01F)
        root.addOrReplaceChild("eyes", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.01F, 8.0F, 8.0F, 0.0F), PartPose.ZERO);
        return root.bake(8, 8);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, AvatarRenderState state, float yRot, float xRot) {
        if (!(this.getParentModel() instanceof HumanoidModel<?> humanoidModel)) {
            return;
        }

        if (!(state instanceof DbaPlayerState dbaState)) {
            return;
        }

        Identifier raceId = dbaState.dba$getRaceId();
        if (raceId == null) return;

        String race = raceId.getPath().toLowerCase();
        com.dragonblockarcanedba.client.render.model.ObjMesh mesh = com.dragonblockarcanedba.client.render.model.Custom3DModelRegistry.getModelForRace(race);

        // Determine blink state (closed for 150ms every 3-6 seconds randomly based on entity ID to offset it)
        boolean isBlinking = false;
        long time = System.currentTimeMillis() + (state.id * 12345L);
        long cycle = time % 4000; // 4 seconds cycle
        if (cycle < 150) {
            isBlinking = true;
        }

        Identifier texture = isBlinking ? EYES_CLOSED : EYES_OPEN;
        RenderType renderType = RenderTypes.entityTranslucent(texture);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            PoseStack stack = new PoseStack();
            stack.last().pose().set(pose.pose());
            stack.last().normal().set(pose.normal());

            stack.pushPose();
            humanoidModel.head.translateAndRotate(stack);

            int eyeColor = dbaState.dba$getEyeColor();
            if (eyeColor == 0) eyeColor = 0xFFFFFF;
            int argb = 0xFF000000 | eyeColor;

            if (mesh != null) {
                // Dynamically anchor eyes a set distance down from the top of the skull
                float topY = mesh.topOfHeadY;
                float height = mesh.headHeight;
                float width = mesh.headWidth;
                float faceZ = mesh.faceFrontZ - 0.002f;

                float eyeCenterY = topY + (height * 0.50f);
                float halfH = height * 0.25f;
                float halfW = width * 0.5f;

                org.joml.Matrix4f mat = stack.last().pose();
                buffer.addVertex(mat, -halfW, eyeCenterY - halfH, faceZ).setColor(argb).setUv(0.0f, 0.0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0f, 0.0f, -1.0f);
                buffer.addVertex(mat, halfW, eyeCenterY - halfH, faceZ).setColor(argb).setUv(1.0f, 0.0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0f, 0.0f, -1.0f);
                buffer.addVertex(mat, halfW, eyeCenterY + halfH, faceZ).setColor(argb).setUv(1.0f, 1.0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0f, 0.0f, -1.0f);
                buffer.addVertex(mat, -halfW, eyeCenterY + halfH, faceZ).setColor(argb).setUv(0.0f, 1.0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0f, 0.0f, -1.0f);
            } else {
                this.eyeOverlay.render(stack, buffer, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, argb);
            }

            stack.popPose();
        });
    }
}
