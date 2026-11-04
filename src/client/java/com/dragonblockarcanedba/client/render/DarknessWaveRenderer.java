package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.DarknessWaveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;

/**
 * Entity Renderer for Darkness Wave in Minecraft 26.2.
 * Renders a towering, volumetric 3D physical void-crescent wave with multi-layered
 * diamond-beveled blades, glowing purple void core, obsidian armor shell, and orbiting trailing shadow shards.
 */
public class DarknessWaveRenderer extends EntityRenderer<DarknessWaveEntity, DarknessWaveRenderer.DarknessWaveRenderState> {

    public DarknessWaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class DarknessWaveRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public double px = 0, py = 0, pz = 0;
        public boolean isSecondary = false;
        public boolean isFirstPersonOwner = false;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(DarknessWaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(DarknessWaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public DarknessWaveRenderState createRenderState() {
        return new DarknessWaveRenderState();
    }

    @Override
    public void extractRenderState(DarknessWaveEntity entity, DarknessWaveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.px = entity.getX();
        state.py = entity.getY();
        state.pz = entity.getZ();
        state.isSecondary = entity.isSecondary();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null && 
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) && 
            mc.options.getCameraType().isFirstPerson());
    }

    @Override
    public void submit(DarknessWaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        float fpScale = (state.isFirstPersonOwner && state.age < 5.0f) ? (0.55f + (state.age / 5.0f) * 0.45f) : 1.0f;
        float span = (state.isSecondary ? 3.0f : 4.4f) * fpScale;
        float chord = (state.isSecondary ? 1.0f : 1.5f) * fpScale;
        float height = (state.isSecondary ? 1.4f : 2.0f) * fpScale;
        float age = state.age;

        // The wave seethes while it flies, opens over the first ticks, and
        // the fangs pull back as it dies. The fade window matches the
        // entity's 60 tick life: full strength to tick 45, gone at 60.
        float pulse = 0.90f + 0.10f * (float) Math.sin(age * 0.6f);
        float open = ease(Math.min(1.0f, age / 5.0f));
        float fadeRaw = age <= 45.0f ? 1.0f : Math.max(0.0f, 1.0f - (age - 45.0f) / 15.0f);
        float fade = ease(fadeRaw) * open;
        float drawnHeight = height * (0.55f + 0.45f * open);
        float fangReach = chord * (0.40f + 0.60f * open) * (0.22f + 0.78f * fadeRaw);
        int segments = com.dragonblockarcanedba.util.FxEnvelope.segments(28,
            com.dragonblockarcanedba.client.render.ki.LayeredRender.distScale(cameraState, state.px, state.py, state.pz), 14);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            draw3DVolumetricCrescent(matrix, buffer, span + 0.45f, chord + 0.32f, drawnHeight * 1.18f, 0.36f, segments,
                0.08f, 0.0f, 0.16f, 0.94f * fade);
            draw3DVolumetricCrescent(matrix, buffer, span, chord, drawnHeight, 0.22f, segments,
                0.58f, 0.05f, 0.90f, 0.96f * fade);
            draw3DVolumetricCrescent(matrix, buffer, span * 0.76f, chord * 0.82f, drawnHeight * 0.62f, 0.10f, segments,
                0.95f, 0.08f, 0.35f, fade * pulse);
            draw3DVolumetricCrescent(matrix, buffer, span * 0.42f, chord * 0.55f, drawnHeight * 0.28f, 0.04f, segments,
                1.0f, 0.85f, 1.0f, fade * pulse);

            drawLeadingFangs(matrix, buffer, span, chord, drawnHeight, fangReach, fade);
            drawTrailingShards(matrix, buffer, span, chord, age, fade);
        });

        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            draw3DVolumetricCrescent(matrix, buffer, span + 0.22f, chord + 0.18f, drawnHeight * 1.08f, 0.28f, segments,
                0.35f, 0.03f, 0.55f, 0.24f * fade);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float ease(float t) {
        float c = Math.max(0.0f, Math.min(1.0f, t));
        return c * c * (3.0f - 2.0f * c);
    }

    /** Curved void blade. Thick through the belly, pinched at the tips, with a wake behind the edge. */
    private static void draw3DVolumetricCrescent(Matrix4f matrix, VertexConsumer consumer,
                                                float span, float chord, float height, float thickness,
                                                int segments, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            float t1 = (i / (float) segments) * 2.0f - 1.0f;
            float t2 = ((i + 1) / (float) segments) * 2.0f - 1.0f;
            float arc1 = 1.0f - t1 * t1;
            float arc2 = 1.0f - t2 * t2;
            float x1 = t1 * span;
            float z1 = arc1 * chord;
            float x2 = t2 * span;
            float z2 = arc2 * chord;
            float h1 = (height * 0.5f) * (0.08f + 0.92f * arc1);
            float h2 = (height * 0.5f) * (0.08f + 0.92f * arc2);
            float trail = Math.max(0.12f, thickness * 2.4f);
            float z1Trail = z1 - trail * arc1;
            float z2Trail = z2 - trail * arc2;

            consumer.addVertex(matrix, x1, h1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, h2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, h2 * 0.15f, z2Trail).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, h1 * 0.15f, z1Trail).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            consumer.addVertex(matrix, x1, -h1 * 0.15f, z1Trail).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, -h2 * 0.15f, z2Trail).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, -h2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x1, -h1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);

            consumer.addVertex(matrix, x1, -h1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, -h2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x2, h2, z2).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, x1, h1, z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        }
    }

    /** Teeth along the leading curve. They jut forward and pinch shut at the tips. */
    private static void drawLeadingFangs(Matrix4f matrix, VertexConsumer consumer, float span, float chord, float height, float reach, float fade) {
        int teeth = 8;
        for (int i = 0; i < teeth; i++) {
            float t = ((i + 0.5f) / teeth) * 2.0f - 1.0f;
            float arc = 1.0f - t * t;
            if (arc < 0.12f) {
                continue;
            }
            float x = t * span * 0.90f;
            float z = arc * chord;
            float half = span * 0.045f * (0.40f + 0.60f * arc);
            float lift = (i % 2 == 0 ? 1.0f : -1.0f) * height * 0.16f * arc;
            float tipZ = z + reach * (0.45f + 0.55f * arc);
            waveTri(matrix, consumer,
                x - half, 0.0f, z, 0.58f, 0.05f, 0.90f, 0.95f * fade,
                x + half, lift * 0.35f, z, 0.58f, 0.05f, 0.90f, 0.95f * fade,
                x, lift, tipZ, 0.95f, 0.08f, 0.32f, fade);
        }
    }

    /** Small crescents slipping off the back of the wave. */
    private static void drawTrailingShards(Matrix4f matrix, VertexConsumer consumer, float span, float chord, float age, float fade) {
        for (int i = 0; i < 5; i++) {
            float side = (i % 2 == 0) ? 1.0f : -1.0f;
            float along = i / 4.0f;
            float x = side * span * (0.25f + along * 0.55f);
            float z = -0.35f - along * 1.15f;
            float y = (float) Math.sin(age * 0.45f + i) * 0.35f;
            float shard = 0.45f + along * 0.25f;
            float lead = z + chord * 0.12f;
            waveTri(matrix, consumer,
                x - shard, y, z, 0.45f, 0.04f, 0.80f, 0.70f * fade,
                x + shard, y, z, 0.45f, 0.04f, 0.80f, 0.70f * fade,
                x, y, lead, 0.85f, 0.08f, 0.95f, 0.0f);
        }
    }

    private static void waveTri(Matrix4f matrix, VertexConsumer consumer,
                                float x1, float y1, float z1, float r1, float g1, float b1, float a1,
                                float x2, float y2, float z2, float r2, float g2, float b2, float a2,
                                float x3, float y3, float z3, float r3, float g3, float b3, float a3) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r1, g1, b1, a1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r2, g2, b2, a2).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r3, g3, b3, a3).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r3, g3, b3, a3).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
    }
}
