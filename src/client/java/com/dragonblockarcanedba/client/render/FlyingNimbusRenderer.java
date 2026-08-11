package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.block.DbaBlocks;
import com.dragonblockarcanedba.entity.FlyingNimbusEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;

public class FlyingNimbusRenderer extends EntityRenderer<FlyingNimbusEntity, FlyingNimbusRenderer.NimbusRenderState> {

    public FlyingNimbusRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public NimbusRenderState createRenderState() {
        return new NimbusRenderState();
    }

    @Override
    public void extractRenderState(FlyingNimbusEntity entity, NimbusRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getViewYRot(partialTicks);
        
        BlockPos pos = entity.blockPosition();
        state.movingBlockRenderState.blockState = DbaBlocks.YELLOW_CLOUD_BLOCK.defaultBlockState();
        state.movingBlockRenderState.blockPos = pos;
        state.movingBlockRenderState.randomSeedPos = pos;
        
        if (entity.level() instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel) {
            state.movingBlockRenderState.biome = clientLevel.getBiome(pos);
            state.movingBlockRenderState.cardinalLighting = clientLevel.cardinalLighting();
            state.movingBlockRenderState.lightEngine = clientLevel.getLightEngine();
        }
    }

    @Override
    public void submit(NimbusRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        // 1. Center block
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.translate(-0.5, -0.2, -0.5);
        collector.submitMovingBlock(poseStack, state.movingBlockRenderState, -1);
        poseStack.popPose();
        
        // 2. Right block
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.translate(0.5 - 0.5, -0.25, 0.0 - 0.5);
        poseStack.scale(0.8F, 0.8F, 0.8F);
        collector.submitMovingBlock(poseStack, state.movingBlockRenderState, -1);
        poseStack.popPose();

        // 3. Left block
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.translate(-0.5 - 0.5, -0.25, 0.0 - 0.5);
        poseStack.scale(0.8F, 0.8F, 0.8F);
        collector.submitMovingBlock(poseStack, state.movingBlockRenderState, -1);
        poseStack.popPose();

        // 4. Front block
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.translate(0.0 - 0.5, -0.25, 0.5 - 0.5);
        poseStack.scale(0.8F, 0.8F, 0.8F);
        collector.submitMovingBlock(poseStack, state.movingBlockRenderState, -1);
        poseStack.popPose();

        // 5. Back block
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.translate(0.0 - 0.5, -0.25, -0.5 - 0.5);
        poseStack.scale(0.8F, 0.8F, 0.8F);
        collector.submitMovingBlock(poseStack, state.movingBlockRenderState, -1);
        poseStack.popPose();

        super.submit(state, poseStack, collector, cameraRenderState);
    }

    public static class NimbusRenderState extends EntityRenderState {
        public float yRot;
        public final MovingBlockRenderState movingBlockRenderState = new MovingBlockRenderState();
    }
}
