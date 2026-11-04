package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.LavaBlobEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * The lava blob (redesign ideas 2 and 6): a tumbling, lumpy ball of
 * living lava, two displaced domes back to back off the synced seed so
 * it is a rock and not a sphere, with a short plume of flame trailing
 * off the top as it falls.
 */
public class LavaBlobRenderer extends EntityRenderer<LavaBlobEntity, LavaBlobRenderer.BlobState> {
    public LavaBlobRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class BlobState extends EntityRenderState {
        public float age;
        public int seed;
        public double posX;
        public double posY;
        public double posZ;
    }

    public boolean shouldRender(LavaBlobEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(LavaBlobEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public BlobState createRenderState() {
        return new BlobState();
    }

    @Override
    public void extractRenderState(LavaBlobEntity entity, BlobState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.age = entity.tickCount + partialTick;
        state.seed = entity.getSeed();
        state.posX = entity.getX();
        state.posY = entity.getY();
        state.posZ = entity.getZ();
    }

    @Override
    public void submit(BlobState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float scale = LayeredRender.distScale(cameraState, state.posX, state.posY, state.posZ);
        float env = FxEnvelope.envelope(state.age, 240.0f);
        long seed = state.seed;
        RenderType type = KiRenderHelper.kiRenderType();
        poseStack.pushPose();
        // Tumble derived from age and seed, so every client rolls it alike.
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotation(state.age * 0.31f + (seed & 0x3F))));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotation(state.age * 0.23f + ((seed >> 6) & 0x3F))));
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            BespokeGeometry.displacedDome(pose, buffer, seed, 0.36f,
                1.0f, 0.46f, 0.06f, LayeredRender.ALPHA_CORE, env, scale);
        });
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(180.0f)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotation(state.age * 0.31f + (seed & 0x3F))));
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xD0BEL, 0.33f,
                0.92f, 0.34f, 0.03f, LayeredRender.ALPHA_MID + 0.2f, env, scale);
            BespokeGeometry.plume(pose, buffer, seed ^ 0xF17EL, 0.16f, 0.55f,
                0.85f, 0.22f, 0.02f, LayeredRender.ALPHA_MID, env, scale);
        });
        poseStack.popPose();
    }
}
