package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.weapon.model.BladePart;
import com.dragonblockarcanedba.client.render.weapon.model.BoxPart;
import com.dragonblockarcanedba.client.render.weapon.model.ModelPart;
import com.dragonblockarcanedba.entity.GrandBladeShardEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Grand Blade Shard in Minecraft 26.2.
 * Renders a sharp, glowing golden-steel blade shard.
 */
public class GrandBladeShardRenderer extends EntityRenderer<GrandBladeShardEntity, GrandBladeShardRenderer.BladeShardRenderState> {
    private final ModelPart bladeModel;

    public GrandBladeShardRenderer(EntityRendererProvider.Context context) {
        super(context);

        // Procedural sharp greatsword shard model
        bladeModel = new ModelPart();

        // 1. Sharp steel-blue double-edged blade tip
        ModelPart bladeTip = new BladePart(3.2f, 1.2f, 9.6f, 0.0f, 0.0f).setColor(0xFF4682B4); // Steel Blue

        // 2. Radiant gold spine / core
        ModelPart goldSpine = new BoxPart(1.6f, 1.4f, 8.0f).setColor(0xFFFFD700); // Gold

        // 3. Bright white-gold cutting edge highlight
        ModelPart sharpEdge = new BoxPart(0.4f, 0.8f, 10.0f).setColor(0xFFFFFFEE);

        bladeModel.addChild(bladeTip);
        bladeModel.addChild(goldSpine);
        bladeModel.addChild(sharpEdge);
    }

    @Override
    public BladeShardRenderState createRenderState() {
        return new BladeShardRenderState();
    }

    @Override
    public void extractRenderState(GrandBladeShardEntity entity, BladeShardRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.isEmbedded = entity.isEmbedded();
        state.yRot = entity.isEmbedded() ? entity.getEmbeddedYaw() : entity.getYRot();
        state.xRot = entity.isEmbedded() ? entity.getEmbeddedPitch() : entity.getXRot();
        state.ageInTicks = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(BladeShardRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();

        if (state.isEmbedded) {
            // Stuck firmly in the ground angled outward
            poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
            poseStack.mulPose(Axis.XP.rotationDegrees(Math.max(35.0f, Math.abs(state.xRot))));
            poseStack.scale(0.65f, 0.65f, 0.65f);
        } else {
            // Flying through the air along flight direction
            poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
            poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
            poseStack.mulPose(Axis.ZP.rotationDegrees(state.ageInTicks * 25.0f)); // Spin along axis
            poseStack.scale(0.55f, 0.55f, 0.55f);
        }

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            PoseStack localPose = new PoseStack();
            localPose.last().pose().set(pose.pose());
            localPose.last().normal().set(pose.normal());
            bladeModel.render(localPose, buffer, KiRenderHelper.FULL_BRIGHT, KiRenderHelper.NO_OVERLAY, null);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    public static class BladeShardRenderState extends EntityRenderState {
        public boolean isEmbedded;
        public float yRot;
        public float xRot;
        public float ageInTicks;
    }
}
