package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.entity.DebrisEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;

/**
 * Renders DebrisEntity pieces as their real block models, tumbling.
 * Mirrors vanilla FallingBlockRenderer: the block goes through a
 * MovingBlockRenderState so lighting, tint, and biome color match the world.
 * Scale carries the lifecycle: pieces grow in over 6 ticks and crumble
 * (shrink) over their landed fade, so nothing pops.
 */
public class DebrisRenderer extends EntityRenderer<DebrisEntity, DebrisRenderer.DebrisState> {

    public DebrisRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class DebrisState extends EntityRenderState {
        public final MovingBlockRenderState moving = new MovingBlockRenderState();
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
        BlockPos pos = BlockPos.containing(entity.getX(), entity.getBoundingBox().maxY, entity.getZ());
        state.moving.randomSeedPos = pos;
        state.moving.blockPos = pos;
        state.moving.blockState = entity.getBlock();
        if (entity.level() instanceof ClientLevel clientLevel) {
            state.moving.biome = clientLevel.getBiome(pos);
            state.moving.cardinalLighting = clientLevel.cardinalLighting();
            state.moving.lightEngine = clientLevel.getLightEngine();
        }
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
        if (state.moving.blockState == null) return;
        if (state.moving.blockState.getRenderShape() != RenderShape.MODEL) return;
        if (state.scaleVis <= 0.01f) return;
        poseStack.pushPose();
        float s = state.scaleVis;
        poseStack.translate(-0.5f * s, 0f, -0.5f * s);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.rotX)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(state.rotZ)));
        poseStack.scale(s, s, s);
        collector.submitMovingBlock(poseStack, state.moving, -1);
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
