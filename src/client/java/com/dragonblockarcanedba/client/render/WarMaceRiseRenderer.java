package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.WarMaceRiseEntity;
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
 * An upward crescent with a trail, rebuilt on the bespoke toolkit
 * (BESPOKE-STANDARD rule 1). Each layer is the toolkit's blade arc: a
 * tapered, nicked irregular arc, stood upright into the facing plane. The
 * main crescent climbs as before; two lagging ghost arcs below it and one
 * wider, fainter wisp arc around it give the swing layered depth, and an
 * expanding glow fan at the base marks the launch for the first few ticks.
 * All layers are seeded from the entity's synced seed and run the shared
 * FxEnvelope, so the swing builds in and settles instead of popping.
 */
public class WarMaceRiseRenderer extends EntityRenderer<WarMaceRiseEntity, WarMaceRiseRenderer.RiseState> {
    /** Arc sweep in degrees, centered on the facing direction. */
    private static final float SWEEP = 250.0f;

    public WarMaceRiseRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class RiseState extends EntityRenderState {
        public float age;
        public float yRot;
        public int seed;
        public double ex;
        public double ey;
        public double ez;
    }

    public boolean shouldRender(WarMaceRiseEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(WarMaceRiseEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public RiseState createRenderState() {
        return new RiseState();
    }

    @Override
    public void extractRenderState(WarMaceRiseEntity entity, RiseState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.age = entity.tickCount + partialTick;
        state.yRot = entity.getYRot();
        state.seed = entity.getSeed();
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
    }

    @Override
    public void submit(RiseState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float open = ease(Math.min(1.0f, state.age / 3.0f));
        float env = FxEnvelope.envelope(state.age, 8.0f, 0.12f, 0.30f);
        float lift = state.age * 0.22f;
        float radius = 1.15f * (0.45f + 0.55f * open);
        float distScale = LayeredRender.distScale(cameraState, state.ex, state.ey, state.ez);
        long seed = state.seed;
        RenderType type = KiRenderHelper.kiRenderType();

        // Impact feedback at the base: an expanding glow fan for the first
        // ticks, drawn without the climb so it stays on the launch point.
        if (state.age < 3.5f) {
            float ringAlpha = (1.0f - state.age / 3.5f) * 0.6f;
            poseStack.pushPose();
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
            collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
                BespokeGeometry.glowFan(pose, buffer, seed ^ 0xBA5E,
                    0.8f + state.age * 0.55f, 0.05f,
                    1.0f, 0.8f, 0.45f, ringAlpha, distScale);
            });
            poseStack.popPose();
        }

        // Trail ghosts first, then the wide wisp, then the main crescent.
        drawArcLayer(state, poseStack, collector, type, seed ^ 0xA003,
            radius * 0.92f, lift - 0.56f, 0.85f, 0.62f, 0.28f, 0.55f, env * 0.20f, distScale);
        drawArcLayer(state, poseStack, collector, type, seed ^ 0xA002,
            radius * 0.96f, lift - 0.28f, 0.88f, 0.66f, 0.32f, 0.60f, env * 0.40f, distScale);
        drawArcLayer(state, poseStack, collector, type, seed ^ 0xA001,
            radius * 1.3f, lift + 0.06f, 1.0f, 0.92f, 0.70f, 0.50f, env * 0.30f, distScale);
        drawArcLayer(state, poseStack, collector, type, seed,
            radius, lift, 0.97f, 0.82f, 0.50f, 0.95f, env, distScale);
        super.submit(state, poseStack, collector, cameraState);
    }

    /**
     * One blade arc layer: the pose faces the entity's way, sits at the
     * layer's height on the climb, and tips the toolkit's ground plane arc
     * upright so it sweeps in the facing plane like the old crescent.
     */
    private static void drawArcLayer(RiseState state, PoseStack poseStack, SubmitNodeCollector collector,
                                     RenderType type, long seed, float radius, float y,
                                     float r, float g, float b, float alpha, float envelope,
                                     float distScale) {
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.translate(0.0f, y, 0.35f);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(-90.0f)));
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            BespokeGeometry.bladeArc(pose, buffer, seed, radius, SWEEP,
                r, g, b, alpha, envelope, distScale);
        });
        poseStack.popPose();
    }

    private static float ease(float t) {
        float c = Math.max(0.0f, Math.min(1.0f, t));
        return c * c * (3.0f - 2.0f * c);
    }
}
