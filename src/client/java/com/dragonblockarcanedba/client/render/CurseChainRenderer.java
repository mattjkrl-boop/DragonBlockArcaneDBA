package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.CurseChainEntity;
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
 * Entity Renderer for Curse Chain in Minecraft 26.2.
 * Renders an intimidating, multi-segmented supernatural 3D weapon:
 * - 4 Interlocking 3D heavy iron links with gothic barbed flanges and runic inlays
 * - Heavy 3D cursed shackle collar with protruding demonic spikes
 * - 4 Floating, counter-rotating runic diamond sigils
 * - Undulating 3D helical void aura ribbons
 * - Pulsating demonic hex octahedron core
 */
public class CurseChainRenderer extends EntityRenderer<CurseChainEntity, CurseChainRenderer.CurseChainRenderState> {

    public CurseChainRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class CurseChainRenderState extends EntityRenderState {
        public float ageInTicks;
        public boolean isAttached;
        public int orbitIndex;
    }

    @Override
    public CurseChainRenderState createRenderState() {
        return new CurseChainRenderState();
    }

    @Override
    public void extractRenderState(CurseChainEntity entity, CurseChainRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.ageInTicks = entity.tickCount + partialTicks;
        state.isAttached = entity.isAttached();
        state.orbitIndex = entity.getOrbitIndex();
    }

    @Override
    public void submit(CurseChainRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();

        float age = state.ageInTicks;
        boolean attached = state.isAttached;
        int orbit = state.orbitIndex;

        // Dynamic scale & orientation
        float baseScale = attached ? 0.48f : 0.42f;
        poseStack.scale(baseScale, baseScale, baseScale);

        // Rotation & rhythmic constricting motion
        float spinSpeed = attached ? 12.0f : 24.0f;
        poseStack.mulPose(Axis.YP.rotationDegrees(age * spinSpeed + (orbit * 36.0f)));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) Math.sin(age * 0.2f + orbit) * 18.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(age * (attached ? 6.0f : 14.0f)));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            float pulse = 0.85f + 0.15f * (float) Math.sin(age * 0.35f + orbit);

            // 1. Interlocking 3D Heavy Spiked Gothic Chain Links (4 links along vertical axis)
            float linkSpacing = 0.72f;
            for (int i = 0; i < 4; i++) {
                float ly = (i - 1.5f) * linkSpacing;
                boolean isAlt = (i % 2 == 1);

                // Alternate link orientation 90 degrees
                drawSpikedChainLink(matrix, buffer, 0, ly, 0, isAlt, pulse);
            }

            // 2. Central Cursed Heavy Shackle / Demonic Collar
            drawDemonicShackleCollar(matrix, buffer, 0, 0, 0, pulse);

            // 3. 4 Orbiting 3D Runic Diamond Glyphs (Counter-rotating around chain waist)
            float runeOrbitR = 1.35f * pulse;
            float runeRot = -age * 22.0f + (orbit * 45.0f);
            for (int r = 0; r < 4; r++) {
                double rAngle = (r / 4.0) * Math.PI * 2.0 + Math.toRadians(runeRot);
                float rx = (float) Math.cos(rAngle) * runeOrbitR;
                float rz = (float) Math.sin(rAngle) * runeOrbitR;
                float ry = (float) Math.sin(age * 0.3f + r * 1.5f) * 0.25f;

                drawFloatingRunicGlyph(matrix, buffer, rx, ry, rz, age * 35.0f + r * 90.0f, pulse);
            }

            // 4. Undulating 3D Helical Void Aura Ribbons (Dark purple/crimson energy tendrils)
            drawAuraRibbons(matrix, buffer, age, orbit, pulse);

            // 5. Pulsating Demonic Hex Octahedron Core
            float coreSize = 0.38f * pulse;
            drawHexCore(matrix, buffer, 0, 0, 0, coreSize);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawSpikedChainLink(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, boolean rotated, float pulse) {
        float outerR = 0.55f;
        float innerR = 0.32f;
        float thickness = 0.14f;

        // Colors: Charcoal Obsidian metal with Corrupted Violet & Blood Highlights
        float rBase = 0.12f, gBase = 0.08f, bBase = 0.16f;
        float rRune = 0.65f * pulse, gRune = 0.05f, bRune = 0.85f * pulse;

        int segs = 8;
        for (int s = 0; s < segs; s++) {
            double a1 = (s / (double) segs) * Math.PI * 2.0;
            double a2 = ((s + 1) / (double) segs) * Math.PI * 2.0;

            float cos1 = (float) Math.cos(a1), sin1 = (float) Math.sin(a1);
            float cos2 = (float) Math.cos(a2), sin2 = (float) Math.sin(a2);

            float x1Out = cos1 * outerR, y1Out = sin1 * outerR;
            float x2Out = cos2 * outerR, y2Out = sin2 * outerR;
            float x1In = cos1 * innerR, y1In = sin1 * innerR;
            float x2In = cos2 * innerR, y2In = sin2 * innerR;

            if (rotated) {
                // Link rotated 90 degrees around Y axis (Z/Y plane)
                drawQuad(matrix, consumer,
                    cx - thickness, cy + y1In, cz + x1In,
                    cx - thickness, cy + y2In, cz + x2In,
                    cx - thickness, cy + y2Out, cz + x2Out,
                    cx - thickness, cy + y1Out, cz + x1Out,
                    rBase, gBase, bBase, 0.95f
                );
                drawQuad(matrix, consumer,
                    cx + thickness, cy + y1Out, cz + x1Out,
                    cx + thickness, cy + y2Out, cz + x2Out,
                    cx + thickness, cy + y2In, cz + x2In,
                    cx + thickness, cy + y1In, cz + x1In,
                    rRune, gRune, bRune, 0.95f
                );
                // Top/Bottom bevels
                drawQuad(matrix, consumer,
                    cx - thickness, cy + y1Out, cz + x1Out,
                    cx - thickness, cy + y2Out, cz + x2Out,
                    cx + thickness, cy + y2Out, cz + x2Out,
                    cx + thickness, cy + y1Out, cz + x1Out,
                    0.25f, 0.05f, 0.35f, 0.98f
                );
            } else {
                // Link in X/Y plane
                drawQuad(matrix, consumer,
                    cx + x1In, cy + y1In, cz - thickness,
                    cx + x2In, cy + y2In, cz - thickness,
                    cx + x2Out, cy + y2Out, cz - thickness,
                    cx + x1Out, cy + y1Out, cz - thickness,
                    rBase, gBase, bBase, 0.95f
                );
                drawQuad(matrix, consumer,
                    cx + x1Out, cy + y1Out, cz + thickness,
                    cx + x2Out, cy + y2Out, cz + thickness,
                    cx + x2In, cy + y2In, cz + thickness,
                    cx + x1In, cy + y1In, cz + thickness,
                    rRune, gRune, bRune, 0.95f
                );
                // Outer Rim
                drawQuad(matrix, consumer,
                    cx + x1Out, cy + y1Out, cz - thickness,
                    cx + x2Out, cy + y2Out, cz - thickness,
                    cx + x2Out, cy + y2Out, cz + thickness,
                    cx + x1Out, cy + y1Out, cz + thickness,
                    0.25f, 0.05f, 0.35f, 0.98f
                );
            }

            // Add Barbed Gothic Spikes at 4 diagonal corners of the link
            if (s % 2 == 0) {
                float midAngle = (float) ((a1 + a2) * 0.5);
                float mx = (float) Math.cos(midAngle) * outerR;
                float my = (float) Math.sin(midAngle) * outerR;
                float spikeLen = 0.28f * pulse;

                float tipX = (float) Math.cos(midAngle) * (outerR + spikeLen);
                float tipY = (float) Math.sin(midAngle) * (outerR + spikeLen);

                if (rotated) {
                    drawSpikePyramid(matrix, consumer,
                        cx - thickness * 0.8f, cy + my, cz + mx,
                        cx + thickness * 0.8f, cy + my, cz + mx,
                        cx, cy + tipY, cz + tipX,
                        0.85f, 0.05f, 0.25f, 1.0f
                    );
                } else {
                    drawSpikePyramid(matrix, consumer,
                        cx + mx, cy + my, cz - thickness * 0.8f,
                        cx + mx, cy + my, cz + thickness * 0.8f,
                        cx + tipX, cy + tipY, cz,
                        0.85f, 0.05f, 0.25f, 1.0f
                    );
                }
            }
        }
    }

    private static void drawDemonicShackleCollar(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float pulse) {
        int segs = 12;
        float rOut = 0.85f;
        float rIn = 0.65f;
        float halfH = 0.16f;

        for (int i = 0; i < segs; i++) {
            double a1 = (i / (double) segs) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segs) * Math.PI * 2.0;

            float x1Out = (float) Math.cos(a1) * rOut, z1Out = (float) Math.sin(a1) * rOut;
            float x2Out = (float) Math.cos(a2) * rOut, z2Out = (float) Math.sin(a2) * rOut;
            float x1In = (float) Math.cos(a1) * rIn, z1In = (float) Math.sin(a1) * rIn;
            float x2In = (float) Math.cos(a2) * rIn, z2In = (float) Math.sin(a2) * rIn;

            // Top Collar Face
            drawQuad(matrix, consumer,
                cx + x1In, cy + halfH, cz + z1In,
                cx + x2In, cy + halfH, cz + z2In,
                cx + x2Out, cy + halfH, cz + z2Out,
                cx + x1Out, cy + halfH, cz + z1Out,
                0.22f, 0.05f, 0.32f, 0.95f
            );

            // Outer Collar Rim
            drawQuad(matrix, consumer,
                cx + x1Out, cy - halfH, cz + z1Out,
                cx + x2Out, cy - halfH, cz + z2Out,
                cx + x2Out, cy + halfH, cz + z2Out,
                cx + x1Out, cy + halfH, cz + z1Out,
                0.90f * pulse, 0.05f, 0.20f, 0.98f
            );

            // 4 Demonic Horn Spikes protruding outwards from the collar
            if (i % 3 == 0) {
                float hornLen = 0.45f * pulse;
                float midA = (float) ((a1 + a2) * 0.5);
                float hx = (float) Math.cos(midA) * (rOut + hornLen);
                float hz = (float) Math.sin(midA) * (rOut + hornLen);

                drawSpikePyramid(matrix, consumer,
                    cx + x1Out, cy - halfH, cz + z1Out,
                    cx + x2Out, cy + halfH, cz + z2Out,
                    cx + hx, cy, cz + hz,
                    1.0f, 0.10f, 0.35f, 1.0f
                );
            }
        }
    }

    private static void drawFloatingRunicGlyph(Matrix4f matrix, VertexConsumer consumer, float x, float y, float z, float rotDeg, float pulse) {
        float size = 0.22f * pulse;
        double rad = Math.toRadians(rotDeg);
        float cos = (float) Math.cos(rad) * size;
        float sin = (float) Math.sin(rad) * size;

        // 3D Diamond Octahedron
        float r = 0.75f * pulse, g = 0.05f, b = 1.0f;
        drawDiamondQuad(matrix, consumer, x - cos, y, z - sin, x, y + size * 1.4f, z, x + cos, y, z + sin, x, y - size * 1.4f, z, r, g, b, 0.95f);
        drawDiamondQuad(matrix, consumer, x - sin, y, z + cos, x, y + size * 1.4f, z, x + sin, y, z - cos, x, y - size * 1.4f, z, 1.0f, 0.15f, 0.45f, 0.95f);
    }

    private static void drawAuraRibbons(Matrix4f matrix, VertexConsumer consumer, float age, int orbit, float pulse) {
        int steps = 14;
        float heightSpan = 2.4f;
        float ribbonR = 0.65f;

        for (int ribbon = 0; ribbon < 2; ribbon++) {
            float phaseOffset = ribbon * (float) Math.PI;
            float rCol = ribbon == 0 ? 0.65f : 0.95f;
            float gCol = 0.02f;
            float bCol = ribbon == 0 ? 1.0f : 0.25f;

            for (int s = 0; s < steps; s++) {
                float p1 = s / (float) steps;
                float p2 = (s + 1) / (float) steps;

                float y1 = -heightSpan * 0.5f + (p1 * heightSpan);
                float y2 = -heightSpan * 0.5f + (p2 * heightSpan);

                double a1 = (p1 * Math.PI * 4.0) + (age * 0.25f) + phaseOffset;
                double a2 = (p2 * Math.PI * 4.0) + (age * 0.25f) + phaseOffset;

                float x1 = (float) Math.cos(a1) * (ribbonR + (float) Math.sin(p1 * Math.PI) * 0.3f);
                float z1 = (float) Math.sin(a1) * (ribbonR + (float) Math.sin(p1 * Math.PI) * 0.3f);
                float x2 = (float) Math.cos(a2) * (ribbonR + (float) Math.sin(p2 * Math.PI) * 0.3f);
                float z2 = (float) Math.sin(a2) * (ribbonR + (float) Math.sin(p2 * Math.PI) * 0.3f);

                float width = 0.09f * (float) Math.sin(p1 * Math.PI) * pulse;
                renderBeam(matrix, consumer, x1, y1, z1, x2, y2, z2, width, rCol, gCol, bCol, 0.75f);
            }
        }
    }

    private static void drawHexCore(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float s) {
        float[][] pts = { {s,0,0}, {0,0,s}, {-s,0,0}, {0,0,-s} };
        for (int i = 0; i < 4; i++) {
            int nxt = (i + 1) % 4;
            // Top pyramid
            consumer.addVertex(matrix, cx + pts[i][0], cy, cz + pts[i][2]).setColor(0.95f, 0.05f, 0.45f, 1.0f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx + pts[nxt][0], cy, cz + pts[nxt][2]).setColor(0.95f, 0.05f, 0.45f, 1.0f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx, cy + s * 1.5f, cz).setColor(1.0f, 0.9f, 1.0f, 1.0f).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx, cy + s * 1.5f, cz).setColor(1.0f, 0.9f, 1.0f, 1.0f).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Bottom pyramid
            consumer.addVertex(matrix, cx + pts[i][0], cy, cz + pts[i][2]).setColor(0.65f, 0.02f, 0.95f, 1.0f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, cx + pts[nxt][0], cy, cz + pts[nxt][2]).setColor(0.65f, 0.02f, 0.95f, 1.0f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, cx, cy - s * 1.5f, cz).setColor(1.0f, 0.9f, 1.0f, 1.0f).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, cx, cy - s * 1.5f, cz).setColor(1.0f, 0.9f, 1.0f, 1.0f).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        }
    }

    private static void drawSpikePyramid(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float tipX, float tipY, float tipZ, float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(1.0f, 0.9f, 1.0f, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(1.0f, 0.9f, 1.0f, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawDiamondQuad(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float b, float a) {
        drawQuad(matrix, consumer, x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, r, g, b, a);
    }

    private static void renderBeam(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float radius, float r, float g, float b, float a) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001f) return;

        float nx = -dz / len * radius;
        float nz = dx / len * radius;
        float ny = radius;

        drawQuad(matrix, consumer, x1 - nx, y1, z1 - nz, x1 + nx, y1, z1 + nz, x2 + nx, y2, z2 + nz, x2 - nx, y2, z2 - nz, r, g, b, a);
        drawQuad(matrix, consumer, x1, y1 - ny, z1, x1, y1 + ny, z1, x2, y2 + ny, z2, x2, y2 - ny, z2, r, g, b, a);
    }

    private static void drawQuad(Matrix4f matrix, VertexConsumer consumer,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float x4, float y4, float z4,
                                 float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Reverse for backface
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }
}
