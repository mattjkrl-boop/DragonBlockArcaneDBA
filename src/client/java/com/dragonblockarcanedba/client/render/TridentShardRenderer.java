package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.weapon.model.BladePart;
import com.dragonblockarcanedba.client.render.weapon.model.BoxPart;
import com.dragonblockarcanedba.client.render.weapon.model.ModelPart;
import com.dragonblockarcanedba.entity.TridentShardEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

public class TridentShardRenderer extends EntityRenderer<TridentShardEntity, TridentShardRenderer.ShardRenderState> {
    private final ModelPart shardModel;

    public TridentShardRenderer(EntityRendererProvider.Context context) {
        super(context);
        
        // Build procedural model once
        shardModel = new ModelPart();

        // Dark red crystal body (double-sided tapered blade shape to form a crystal)
        ModelPart topCrystal = new BladePart(4.8f, 4.8f, 9.6f, 0.0f, 0.0f).setColor(0xFF220000);
        ModelPart bottomCrystal = new BladePart(4.8f, 4.8f, 4.8f, 0.0f, 0.0f).setColor(0xFF220000);
        bottomCrystal.setRot((float) Math.PI, 0, 0); // Flip upside down

        // Glowing red cracks/core
        ModelPart crack1 = new BoxPart(5.12f, 6.4f, 0.8f).setColor(0xFFFF0000);
        crack1.setRot(0, (float) Math.PI / 4, (float) Math.PI / 8);
        
        ModelPart crack2 = new BoxPart(0.8f, 8.0f, 5.12f).setColor(0xFFFF0000);
        crack2.setRot((float) Math.PI / 6, 0, 0);

        shardModel.addChild(topCrystal);
        shardModel.addChild(bottomCrystal);
        shardModel.addChild(crack1);
        shardModel.addChild(crack2);
    }

    @Override
    public ShardRenderState createRenderState() {
        return new ShardRenderState();
    }

    @Override
    public void extractRenderState(TridentShardEntity entity, ShardRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.ageInTicks = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(ShardRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.scale(0.5f, 0.5f, 0.5f);
        
        // Spin the shard dynamically
        poseStack.mulPose(Axis.YP.rotationDegrees(state.ageInTicks * 15.0f));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.ageInTicks * 5.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0f));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            PoseStack localPose = new PoseStack();
            localPose.last().pose().set(pose.pose());
            localPose.last().normal().set(pose.normal());
            shardModel.render(localPose, buffer, KiRenderHelper.FULL_BRIGHT, KiRenderHelper.NO_OVERLAY, null);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    public static class ShardRenderState extends EntityRenderState {
        public float ageInTicks;
    }
}
