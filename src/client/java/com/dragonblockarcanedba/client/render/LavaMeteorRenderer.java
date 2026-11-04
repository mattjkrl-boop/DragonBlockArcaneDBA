package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.LavaMeteorEntity;
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
 * The lava meteor (redesign idea 5): a big lumpy displaced-dome rock,
 * bright at the core and darker on the underside shell, wrapped in a
 * spinning broken vortex band and dragging a tall plume of flame and
 * smoke behind it as it falls. All bespoke geometry, all off the
 * entity's synced seed.
 */
public class LavaMeteorRenderer extends EntityRenderer<LavaMeteorEntity, LavaMeteorRenderer.MeteorState> {
    public LavaMeteorRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class MeteorState extends EntityRenderState {
        public float age;
        public int seed;
        public double posX;
        public double posY;
        public double posZ;
    }

    public boolean shouldRender(LavaMeteorEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(LavaMeteorEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public MeteorState createRenderState() {
        return new MeteorState();
    }

    @Override
    public void extractRenderState(LavaMeteorEntity entity, MeteorState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.age = entity.tickCount + partialTick;
        state.seed = entity.getSeed();
        state.posX = entity.getX();
        state.posY = entity.getY();
        state.posZ = entity.getZ();
    }

    @Override
    public void submit(MeteorState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float scale = LayeredRender.distScale(cameraState, state.posX, state.posY, state.posZ);
        float env = FxEnvelope.envelope(state.age, 240.0f);
        long seed = state.seed;
        RenderType type = KiRenderHelper.kiRenderType();
        // The flame trail: a tall plume rising off the rock (the meteor
        // falls, so up is behind it).
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.4f, 0.0f);
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            BespokeGeometry.plume(pose, buffer, seed ^ 0x7EA11L, 0.55f, 2.6f,
                0.9f, 0.3f, 0.03f, LayeredRender.ALPHA_MID, env, scale);
            BespokeGeometry.plume(pose, buffer, seed ^ 0x7EA12L, 0.34f, 3.4f,
                1.0f, 0.46f, 0.06f, LayeredRender.ALPHA_CORE, env, scale);
        });
        poseStack.popPose();
        // The rock itself, spinning slowly as it falls.
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotation(state.age * 0.18f + (seed & 0x3F))));
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            BespokeGeometry.displacedDome(pose, buffer, seed, 0.78f,
                1.0f, 0.47f, 0.06f, LayeredRender.ALPHA_CORE, env, scale);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xBA0D1L, 1.02f, -0.35f, 0.55f,
                state.age * 0.4f, 1.0f, 0.5f, 0.08f, LayeredRender.ALPHA_OUTER * env, scale);
        });
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(180.0f)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotation(state.age * 0.18f + (seed & 0x3F))));
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xD00EL, 0.72f,
                0.8f, 0.26f, 0.02f, LayeredRender.ALPHA_MID + 0.2f, env, scale);
        });
        poseStack.popPose();
    }
}
