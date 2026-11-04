package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.entity.DebrisEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renders DebrisEntity pieces as their real block models, tumbling.
 * Bespoke pass (BESPOKE-STANDARD rule 6): the block model goes through a
 * BlockModelRenderState resolved by the context's BlockModelResolver and
 * submitted directly, the same path vanilla uses for a carried block
 * (EndermanRenderer). The old MovingBlockRenderState and submitMovingBlock
 * path is gone; the API is identical on 26.2 and 26.3, so no compat shim.
 * Documented trade: per-quad light engine sampling at the block position
 * is replaced by the entity's packed light coords and baked model tints,
 * the same trade vanilla makes for carried blocks and block displays.
 * Scale carries the lifecycle: pieces grow in over 6 ticks and crumble
 * (shrink) over their landed fade, so nothing pops.
 */
public class DebrisRenderer extends EntityRenderer<DebrisEntity, DebrisRenderer.DebrisState> {

    private static final BlockDisplayContext BLOCK_CONTEXT = BlockDisplayContext.create();

    private final BlockModelResolver blockModelResolver;

    public DebrisRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockModelResolver = context.getBlockModelResolver();
    }

    public static class DebrisState extends EntityRenderState {
        public final BlockModelRenderState blockModel = new BlockModelRenderState();
        public BlockState blockState;
        public float scaleVis = 1f;
        public float rotX = 0f;
        public float rotZ = 0f;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(DebrisEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(DebrisEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public DebrisState createRenderState() {
        return new DebrisState();
    }

    @Override
    public void extractRenderState(DebrisEntity entity, DebrisState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.blockState = entity.getBlock();
        blockModelResolver.update(state.blockModel, state.blockState, BLOCK_CONTEXT);
        float age = entity.tickCount + partialTick;
        float grow = FxEnvelope.smoothstep(Math.min(1f, age / 6f));
        float fade = 1f;
        if (entity.getPhase() == DebrisEntity.PHASE_LANDED && entity.getFadeAt() >= 0) {
            float t = (age - entity.getFadeAt()) / DebrisEntity.FADE_TICKS;
            fade = Math.max(0f, 1f - FxEnvelope.smoothstep(t));
        }
        state.scaleVis = entity.getScale() * grow * fade;
        int seed = entity.getSeed();
        state.rotX = (age * 7.3f + (seed & 0xFF)) % 360f;
        state.rotZ = (age * 5.1f + ((seed >> 8) & 0xFF)) % 360f;
    }

    @Override
    public void submit(DebrisState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (state.blockState == null) return;
        if (state.blockState.getRenderShape() != RenderShape.MODEL) return;
        if (state.blockModel.isEmpty()) return;
        if (state.scaleVis <= 0.01f) return;
        poseStack.pushPose();
        float s = state.scaleVis;
        poseStack.translate(-0.5f * s, 0f, -0.5f * s);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.rotX)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(state.rotZ)));
        poseStack.scale(s, s, s);
        state.blockModel.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, -1);
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
