package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.LavaCleaveEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;

/** Traveling lava crescent, drips under the lip, and a trail that burns out. */
public class LavaCleaveRenderer extends EntityRenderer<LavaCleaveEntity, LavaCleaveRenderer.CleaveState> {
    public LavaCleaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class CleaveState extends EntityRenderState {
        public float age;
        public float yRot;
        public float xRot;
        public float life;
        public int seed;
        public double posX;
        public double posY;
        public double posZ;
    }

    public boolean shouldRender(LavaCleaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(LavaCleaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public CleaveState createRenderState() {
        return new CleaveState();
    }

    @Override
    public void extractRenderState(LavaCleaveEntity entity, CleaveState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.age = entity.tickCount + partialTick;
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.life = entity.life();
        state.seed = entity.getSeed();
        state.posX = entity.getX();
        state.posY = entity.getY();
        state.posZ = entity.getZ();
    }

    @Override
    public void submit(CleaveState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        // The shared envelope drives the alpha (standard point 5): the
        // crescent builds as it opens and burns down over its last ticks
        // instead of starting at full alpha. Distance steps segments down.
        float scale = LayeredRender.distScale(cameraState, state.posX, state.posY, state.posZ);
        float env = FxEnvelope.envelope(state.age, state.life);
        float open = ease(Math.min(1.0f, state.age / 2.5f));
        float trailT = ease(Math.max(0.0f, 1.0f - state.age / (state.life * 0.7f)));
        long seed = state.seed;
        RenderType type = KiRenderHelper.kiRenderType();
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));
        // Bespoke arcs (BESPOKE-STANDARD rule 1): bladeArc lies in its own
        // XZ plane centered on +X, so rotate Z then X to stand it in the
        // crescent's XY plane centered on +Y, the orientation the old
        // hand-rolled crescent used. The shapes are the shared seeded
        // builder's: nicked outer edge, swelling profile, no equal segments.
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.0f, 0.15f);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(90.0f)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(-90.0f)));
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            // Three layers (standard point 1): the main mid arc and a
            // smaller bright core arc nested inside it.
            BespokeGeometry.bladeArc(pose, buffer, seed, 1.35f * (0.4f + 0.6f * open), 168.0f,
                0.72f, 0.18f, 0.04f, 0.9f, env, scale);
            BespokeGeometry.bladeArc(pose, buffer, seed ^ 0xC1EA5L, 0.78f * (0.4f + 0.6f * open), 150.0f,
                1.0f, 0.78f, 0.22f, 0.95f, env, scale);
        });
        poseStack.popPose();
        if (trailT > 0.02f) {
            // The wispy trailing arc behind the crescent, shrinking and
            // fading as the flight burns down.
            poseStack.pushPose();
            poseStack.translate(0.0f, 0.0f, -0.55f);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(90.0f)));
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(-90.0f)));
            collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
                BespokeGeometry.bladeArc(pose, buffer, seed ^ 0x7EA11L, 1.05f * trailT, 160.0f,
                    0.45f, 0.08f, 0.02f, LayeredRender.ALPHA_MID * trailT, env, scale);
            });
            poseStack.popPose();
        }
        // Molten drips under the lip: a short bespoke plume flipped to
        // hang downward off the crescent's base.
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.1f, 0.1f);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(180.0f)));
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            BespokeGeometry.plume(pose, buffer, seed ^ 0xD2195L, 0.38f, 0.85f,
                1.0f, 0.45f, 0.05f, 0.55f, env, scale);
        });
        poseStack.popPose();
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float ease(float t) {
        float c = Mth.clamp(t, 0.0f, 1.0f);
        return c * c * (3.0f - 2.0f * c);
    }
}
