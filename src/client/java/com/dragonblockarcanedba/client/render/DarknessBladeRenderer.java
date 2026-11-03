package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.DarknessBladeEntity;
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
 * Entity Renderer for Darkness Blade falling execution in MC 26.2.
 * Renders a monumental 3D demonic execution greatsword with multi-beveled diamond blade,
 * 3D horned crossguard, pommel, central crimson runic fuller, and supersonic plunge wake.
 */
public class DarknessBladeRenderer extends EntityRenderer<DarknessBladeEntity, DarknessBladeRenderer.DarknessBladeRenderState> {

    public DarknessBladeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class DarknessBladeRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(DarknessBladeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(DarknessBladeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public DarknessBladeRenderState createRenderState() {
        return new DarknessBladeRenderState();
    }

    @Override
    public void extractRenderState(DarknessBladeEntity entity, DarknessBladeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(DarknessBladeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f))); // Point downward toward earth
        // The renderer used to be completely frozen: fixed yaw, no age term at
        // all. The blades now roll around their fall axis as they drop (the ZP
        // rotation is the blade's own long axis) and fade in over 3 ticks.
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(state.yRot + state.age * 18.0f)));

        float bladeLength = 7.5f;
        float bladeWidth = 1.0f;
        float age = state.age;
        float fade = Math.min(1.0f, age / 3.0f);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Demonic Blood-Purple Outer Aura Halo
            draw3DGreatsword(matrix, buffer, bladeWidth + 0.35f, bladeLength + 0.4f, 0.30f, 0.65f, 0.05f, 0.95f, 0.80f * fade);

            // 2. Abyssal Obsidian Core Body
            draw3DGreatsword(matrix, buffer, bladeWidth, bladeLength, 0.16f, 0.06f, 0.0f, 0.12f, 0.98f * fade);

            // 3. Central Glowing Crimson Blood Fuller / Rune
            // T4: the fuller breathes while the blade falls, so the rune reads
            // as living energy instead of a flat red stripe.
            float fullerPulse = 0.82f + 0.18f * (float) Math.sin(age * 0.8f);
            draw3DGreatsword(matrix, buffer, bladeWidth * 0.32f, bladeLength * 0.85f, 0.18f, 1.0f, 0.05f, 0.25f, fullerPulse * fade);
            draw3DGreatsword(matrix, buffer, bladeWidth * 0.07f, bladeLength * 0.96f, 0.05f, 1.0f, 0.88f, 0.94f, fade);

            // 4. 3D Horned Crossguard
            drawCrossguard(matrix, buffer, bladeWidth * 2.2f, 0.4f, 0.22f, 0.10f, 0.0f, 0.20f, 1.0f * fade);
            drawCrossguard(matrix, buffer, bladeWidth * 2.0f, 0.3f, 0.25f, 0.85f, 0.05f, 0.35f, 0.95f * fade);

            // 5. 3D Hilt Grip & Pommel
            drawHilt(matrix, buffer, bladeLength, 0.16f, 1.8f, 0.12f, 0.02f, 0.18f, 1.0f * fade);

            // 6. Supersonic Downward Plunge Wake Cones
            float wakeFade = ease(Math.max(0.0f, 1.0f - Math.max(0.0f, age - 6.0f) / 10.0f));
            drawPlungeCone(matrix, buffer, bladeWidth + 0.4f, 2.5f, -bladeLength * 0.5f, 0.55f, 0.05f, 0.90f, 0.55f * fade);
            drawPlungeCone(matrix, buffer, (bladeWidth + 0.4f) * 0.45f, 4.2f, -bladeLength * 0.5f - 1.6f, 0.85f, 0.08f, 0.45f, 0.40f * fade * wakeFade);
            float impact = ease(Math.min(1.0f, age / 8.0f));
            float ring = 0.35f + 1.4f * impact;
            int ringSegs = 12;
            for (int i = 0; i < ringSegs; i++) {
                double a1 = (i / (double) ringSegs) * Math.PI * 2.0;
                double a2 = ((i + 1) / (double) ringSegs) * Math.PI * 2.0;
                float tip = bladeLength * 0.5f + bladeWidth;
                consumerQuad(matrix, buffer,
                    (float) Math.cos(a1) * ring * 0.7f, (float) Math.sin(a1) * ring * 0.7f, tip,
                    (float) Math.cos(a2) * ring * 0.7f, (float) Math.sin(a2) * ring * 0.7f, tip,
                    (float) Math.cos(a2) * ring, (float) Math.sin(a2) * ring, tip,
                    (float) Math.cos(a1) * ring, (float) Math.sin(a1) * ring, tip,
                    0.85f, 0.05f, 0.35f, (1.0f - impact) * fade);
            }
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float ease(float t) {
        float c = Math.max(0.0f, Math.min(1.0f, t));
        return c * c * (3.0f - 2.0f * c);
    }

    private static void consumerQuad(Matrix4f matrix, VertexConsumer consumer,
                                     float x1, float y1, float z1, float x2, float y2, float z2,
                                     float x3, float y3, float z3, float x4, float y4, float z4,
                                     float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
    }

    private static void draw3DGreatsword(Matrix4f matrix, VertexConsumer consumer, float width, float length, float thickness, float r, float g, float b, float a) {
        float halfLen = length * 0.5f;
        float tipZ = halfLen + width * 1.6f;

        // Front Face (Double diamond cross-section)
        // Left Flank
        consumer.addVertex(matrix, -width, 0, -halfLen).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, thickness, -halfLen).setColor(r, g, b, a).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, thickness, halfLen).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, -width, 0, halfLen).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Right Flank
        consumer.addVertex(matrix, 0, thickness, -halfLen).setColor(r, g, b, a).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, width, 0, -halfLen).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, width, 0, halfLen).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, thickness, halfLen).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Back Face
        // Left Flank Back
        consumer.addVertex(matrix, -width, 0, halfLen).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, 0, -thickness, halfLen).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, 0, -thickness, -halfLen).setColor(r, g, b, a).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, -width, 0, -halfLen).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);

        // Right Flank Back
        consumer.addVertex(matrix, 0, -thickness, halfLen).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, width, 0, halfLen).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, width, 0, -halfLen).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, 0, -thickness, -halfLen).setColor(r, g, b, a).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);

        // Blade Tip (Front Point)
        consumer.addVertex(matrix, -width, 0, halfLen).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, width, 0, halfLen).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, 0, tipZ).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, 0, tipZ).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Blade Tip (Back Point)
        consumer.addVertex(matrix, 0, 0, tipZ).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, 0, 0, tipZ).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, width, 0, halfLen).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, -width, 0, halfLen).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }

    private static void drawCrossguard(Matrix4f matrix, VertexConsumer consumer, float span, float height, float thickness, float r, float g, float b, float a) {
        float z = -3.75f;
        float hHalf = height * 0.5f;
        float t = Math.max(0.05f, thickness * 0.5f);
        consumerQuad(matrix, consumer, -span, hHalf, z + t, span, hHalf, z + t, span, -hHalf, z + t, -span, -hHalf, z + t, r, g, b, a);
        consumerQuad(matrix, consumer, -span, -hHalf, z - t, span, -hHalf, z - t, span, hHalf, z - t, -span, hHalf, z - t, r, g, b, a);
        consumerQuad(matrix, consumer, -span, hHalf, z - t, span, hHalf, z - t, span, hHalf, z + t, -span, hHalf, z + t, r, g, b, a);
        drawGuardHorn(matrix, consumer, -span, z, -1.0f, r, g, b, a);
        drawGuardHorn(matrix, consumer, span, z, 1.0f, r, g, b, a);
    }

    private static void drawGuardHorn(Matrix4f matrix, VertexConsumer consumer, float originX, float z, float dir, float r, float g, float b, float a) {
        int segs = 4;
        for (int i = 0; i < segs; i++) {
            float t1 = i / (float) segs;
            float t2 = (i + 1) / (float) segs;
            float x1 = originX + dir * (0.12f + t1 * 0.7f);
            float x2 = originX + dir * (0.12f + t2 * 0.7f);
            float y1 = 0.22f * (float) Math.sin(t1 * Math.PI);
            float y2 = 0.22f * (float) Math.sin(t2 * Math.PI);
            float z1 = z + t1 * 0.4f;
            float z2 = z + t2 * 0.4f;
            float w1 = 0.12f * (1.0f - t1);
            float w2 = 0.12f * (1.0f - t2);
            float a1 = a * (1.0f - t1 * 0.25f);
            float a2 = a * (1.0f - t2 * 0.25f);
            fadeQuad(matrix, consumer, x1, y1 - w1, z1, x1, y1 + w1, z1, x2, y2 + w2, z2, x2, y2 - w2, z2, r, g, b, a1, a2);
            fadeQuad(matrix, consumer, x1, y1, z1, x1, y1 + w1 * 0.35f, z1 + 0.04f, x2, y2 + w2 * 0.2f, z2 + 0.04f, x2, y2, z2, 1.0f, 0.75f, 0.9f, a1 * 0.7f, a2 * 0.2f);
        }
    }

    private static void drawHilt(Matrix4f matrix, VertexConsumer consumer, float bladeLen, float gripRadius, float hiltLen, float r, float g, float b, float a) {
        float startZ = -bladeLen * 0.5f;
        float endZ = startZ - hiltLen;

        int gripSides = 6;
        for (int i = 0; i < gripSides; i++) {
            double a1 = (i / (double) gripSides) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) gripSides) * Math.PI * 2.0;
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            float c2 = (float) Math.cos(a2);
            float s2 = (float) Math.sin(a2);
            float r1 = gripRadius;
            float r2 = gripRadius * 0.72f;
            consumerQuad(matrix, consumer,
                c1 * r1, s1 * r1, startZ,
                c2 * r1, s2 * r1, startZ,
                c2 * r2, s2 * r2, endZ,
                c1 * r2, s1 * r2, endZ,
                r, g, b, a);
        }
        consumerQuad(matrix, consumer, -gripRadius * 0.2f, 0, startZ, gripRadius * 0.2f, 0, startZ, gripRadius * 0.12f, 0, endZ, -gripRadius * 0.12f, 0, endZ, 1.0f, 0.7f, 0.85f, a * 0.8f);

        float pRadius = gripRadius * 2.2f;
        float tipZ = endZ - pRadius * 1.5f;
        for (int i = 0; i < 4; i++) {
            double a1 = i * Math.PI * 0.5;
            double a2 = (i + 1) * Math.PI * 0.5;
            float x1 = (float) Math.cos(a1) * pRadius;
            float y1 = (float) Math.sin(a1) * pRadius;
            float x2 = (float) Math.cos(a2) * pRadius;
            float y2 = (float) Math.sin(a2) * pRadius;
            float midA = (float) ((a1 + a2) * 0.5);
            float bx = (float) Math.cos(midA) * pRadius * 0.45f;
            float by = (float) Math.sin(midA) * pRadius * 0.55f;
            float bz = endZ - pRadius * 0.7f;
            fadeQuad(matrix, consumer, x1, y1, endZ, x2, y2, endZ, bx, by, bz, bx, by, bz, r * 2.0f, g * 2.0f, b * 2.0f, a, a * 0.8f);
            fadeQuad(matrix, consumer, bx, by, bz, bx, by, bz, 0.0f, 0.0f, tipZ, 0.0f, 0.0f, tipZ, 1.0f, 0.85f, 0.95f, a * 0.8f, 0.0f);
        }
    }

    /** A curved wake behind the fall. Width pinches and the tail alpha goes to zero. */
    private static void drawPlungeCone(Matrix4f matrix, VertexConsumer consumer, float width, float length, float startZ, float r, float g, float b, float a) {
        int segs = 5;
        float endZ = startZ - length;
        for (int i = 0; i < segs; i++) {
            float t1 = i / (float) segs;
            float t2 = (i + 1) / (float) segs;
            float z1 = startZ + (endZ - startZ) * t1;
            float z2 = startZ + (endZ - startZ) * t2;
            float bow1 = width * 0.28f * (float) Math.sin(t1 * Math.PI);
            float bow2 = width * 0.28f * (float) Math.sin(t2 * Math.PI);
            float w1 = width * (1.0f - t1);
            float w2 = width * (1.0f - t2);
            float a1 = a * (1.0f - t1);
            float a2 = a * (1.0f - t2);
            fadeQuad(matrix, consumer, -w1, bow1, z1, w1, bow1, z1, w2, bow2, z2, -w2, bow2, z2, r, g, b, a1, a2);
            fadeQuad(matrix, consumer, -w1 * 0.22f, bow1 + w1 * 0.15f, z1, w1 * 0.22f, bow1 + w1 * 0.15f, z1, w2 * 0.22f, bow2 + w2 * 0.15f, z2, -w2 * 0.22f, bow2 + w2 * 0.15f, z2, 1.0f, 0.85f, 0.95f, a1, a2);
            fadeQuad(matrix, consumer, -w1 * 0.5f, bow1, z1, w1 * 0.5f, bow1, z1, w2 * 0.15f, bow2 - w2 * 0.8f, z2 - length * 0.04f, -w2 * 0.15f, bow2 - w2 * 0.8f, z2 - length * 0.04f, r, g, b, a1 * 0.45f, 0.0f);
        }
    }

    private static void fadeQuad(Matrix4f matrix, VertexConsumer consumer,
                                 float x1, float y1, float z1, float x2, float y2, float z2,
                                 float x3, float y3, float z3, float x4, float y4, float z4,
                                 float r, float g, float b, float a1, float a2) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a1).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a2).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a2).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
    }
}
