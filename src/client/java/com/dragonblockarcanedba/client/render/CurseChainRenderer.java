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
        public float yRot;
        public float xRot;
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
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
    }

    @Override
    public void submit(CurseChainRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();

        float age = state.ageInTicks;
        boolean attached = state.isAttached;
        int orbit = state.orbitIndex;
        float stretch = ease(Math.min(1.0f, age / 6.0f));

        float baseScale = attached ? 0.55f : 0.46f;
        poseStack.scale(baseScale, baseScale, baseScale);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            float pulse = 0.85f + 0.15f * (float) Math.sin(age * 0.35f + orbit);
            int links = attached ? 7 : 6;
            float prevX = 0.0f;
            float prevY = 0.0f;
            float prevZ = 0.0f;
            boolean hasPrev = false;
            for (int i = 0; i < links; i++) {
                float along = i / (float) (links - 1);
                float lag = along * (0.25f + 0.75f * stretch);
                float x;
                float y;
                float z;
                if (attached) {
                    float arc = (along - 0.5f) * 2.4f + orbit * 0.35f;
                    float radius = 1.15f * (1.0f - 0.18f * stretch);
                    x = (float) Math.cos(arc) * radius;
                    y = (float) Math.sin(along * Math.PI) * 0.35f;
                    z = (float) Math.sin(arc) * radius;
                } else {
                    x = 0.0f;
                    y = 0.0f;
                    z = -lag * 3.4f;
                }
                drawSpikedChainLink(matrix, buffer, x, y, z, (i % 2) == 1, pulse);
                if (hasPrev) {
                    drawChainSpine(matrix, buffer, prevX, prevY, prevZ, x, y, z);
                }
                prevX = x;
                prevY = y;
                prevZ = z;
                hasPrev = true;
            }
            if (attached) {
                float snapIn = ease(Math.min(1.0f, age / 3.0f));
                float snapOut = ease(Math.max(0.0f, 1.0f - Math.max(0.0f, age - 3.0f) / 5.0f));
                float snap = snapIn * snapOut;
                drawDemonicShackleCollar(matrix, buffer, 0, 0, 0, pulse * (0.7f + 0.3f * snapIn));
                if (snap > 0.02f) {
                    float ring = 0.35f + 0.85f * snapIn;
                    int segs = 16;
                    for (int s = 0; s < segs; s++) {
                        double a1 = (s / (double) segs) * Math.PI * 2.0;
                        double a2 = ((s + 1) / (double) segs) * Math.PI * 2.0;
                        float in = ring * 0.72f;
                        drawQuad(matrix, buffer,
                            (float) Math.cos(a1) * in, 0.0f, (float) Math.sin(a1) * in,
                            (float) Math.cos(a2) * in, 0.0f, (float) Math.sin(a2) * in,
                            (float) Math.cos(a2) * ring, 0.0f, (float) Math.sin(a2) * ring,
                            (float) Math.cos(a1) * ring, 0.0f, (float) Math.sin(a1) * ring,
                            1.0f, 0.85f, 0.95f, snap);
                    }
                }
            }
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float ease(float t) {
        float c = Math.max(0.0f, Math.min(1.0f, t));
        return c * c * (3.0f - 2.0f * c);
    }

    private static void drawSpikedChainLink(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, boolean rotated, float pulse) {
        float outerR = 0.55f;
        float innerR = 0.32f;
        float thickness = 0.14f;

        // Colors: Charcoal Obsidian metal with Corrupted Violet & Blood Highlights
        float rBase = 0.12f, gBase = 0.08f, bBase = 0.16f;
        float rRune = 0.65f * pulse, gRune = 0.05f, bRune = 0.85f * pulse;

        int segs = 12;
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
            if (s % 3 == 0) {
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
        int segs = 16;
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
            if (i % 4 == 0) {
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

    /** A curved barb: sheath, white core, and a short wake, not one triangle. */
    private static void drawSpikePyramid(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float tipX, float tipY, float tipZ, float r, float g, float b, float a) {
        float bx = (x1 + x2) * 0.5f;
        float by = (y1 + y2) * 0.5f;
        float bz = (z1 + z2) * 0.5f;
        float dx = tipX - bx;
        float dy = tipY - by;
        float dz = tipZ - bz;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001f) {
            return;
        }
        float sx = x2 - x1;
        float sy = y2 - y1;
        float sz = z2 - z1;
        float sl = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (sl < 0.001f) {
            sx = 1.0f;
            sy = 0.0f;
            sz = 0.0f;
            sl = 1.0f;
        }
        float ux = dx / len;
        float uy = dy / len;
        float uz = dz / len;
        float px = sy * uz - sz * uy;
        float py = sz * ux - sx * uz;
        float pz = sx * uy - sy * ux;
        float pl = (float) Math.sqrt(px * px + py * py + pz * pz);
        if (pl < 0.001f) {
            px = 0.0f;
            py = 1.0f;
            pz = 0.0f;
            pl = 1.0f;
        }
        px /= pl;
        py /= pl;
        pz /= pl;
        float wx = sx / sl;
        float wy = sy / sl;
        float wz = sz / sl;
        float bow = len * 0.42f;
        float mx = bx + ux * len * 0.55f + px * bow;
        float my = by + uy * len * 0.55f + py * bow;
        float mz = bz + uz * len * 0.55f + pz * bow;
        float half = sl * 0.5f;
        spanRibbon(matrix, consumer, bx, by, bz, mx, my, mz, wx, wy, wz, half, half * 0.42f, r, g, b, a, a * 0.8f);
        spanRibbon(matrix, consumer, mx, my, mz, tipX, tipY, tipZ, wx, wy, wz, half * 0.42f, 0.012f, r, g, b, a * 0.8f, a * 0.35f);
        spanRibbon(matrix, consumer, bx, by, bz, mx, my, mz, wx, wy, wz, half * 0.22f, half * 0.1f, 1.0f, 0.9f, 0.95f, a, a * 0.9f);
        spanRibbon(matrix, consumer, mx, my, mz, tipX, tipY, tipZ, wx, wy, wz, half * 0.1f, 0.006f, 1.0f, 0.95f, 1.0f, a * 0.9f, 0.0f);
        float wake = len * 0.28f;
        spanRibbon(matrix, consumer, mx, my, mz, mx - px * wake, my - py * wake, mz - pz * wake, wx, wy, wz, half * 0.3f, 0.01f, r, g, b, a * 0.45f, 0.0f);
    }

    private static void drawChainSpine(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001f) {
            return;
        }
        float px = -dz / len;
        float pz = dx / len;
        float bow = Math.min(0.22f, len * 0.18f);
        float mx = (x1 + x2) * 0.5f + px * bow;
        float my = (y1 + y2) * 0.5f + bow * 0.35f;
        float mz = (z1 + z2) * 0.5f + pz * bow;
        spanRibbon(matrix, consumer, x1, y1, z1, mx, my, mz, px, 0.0f, pz, 0.07f, 0.05f, 0.72f, 0.06f, 0.9f, 0.9f, 0.85f);
        spanRibbon(matrix, consumer, mx, my, mz, x2, y2, z2, px, 0.0f, pz, 0.05f, 0.035f, 0.72f, 0.06f, 0.9f, 0.85f, 0.7f);
        spanRibbon(matrix, consumer, x1, y1, z1, mx, my, mz, px, 0.0f, pz, 0.02f, 0.014f, 1.0f, 0.92f, 0.98f, 1.0f, 1.0f);
        spanRibbon(matrix, consumer, mx, my, mz, x2, y2, z2, px, 0.0f, pz, 0.014f, 0.01f, 1.0f, 0.92f, 0.98f, 1.0f, 0.85f);
        spanRibbon(matrix, consumer, mx, my, mz, mx - px * bow * 1.6f, my - bow * 0.2f, mz - pz * bow * 1.6f, px, 0.0f, pz, 0.04f, 0.008f, 0.55f, 0.04f, 0.8f, 0.4f, 0.0f);
    }

    private static void spanRibbon(Matrix4f matrix, VertexConsumer consumer,
                                   float x1, float y1, float z1, float x2, float y2, float z2,
                                   float wx, float wy, float wz, float w1, float w2,
                                   float r, float g, float b, float a1, float a2) {
        float wlen = (float) Math.sqrt(wx * wx + wy * wy + wz * wz);
        if (wlen < 0.001f) {
            wx = 1.0f;
            wy = 0.0f;
            wz = 0.0f;
        } else {
            wx /= wlen;
            wy /= wlen;
            wz /= wlen;
        }
        fadeStrip(matrix, consumer, x1 - wx * w1, y1 - wy * w1, z1 - wz * w1, x1 + wx * w1, y1 + wy * w1, z1 + wz * w1, x2 + wx * w2, y2 + wy * w2, z2 + wz * w2, x2 - wx * w2, y2 - wy * w2, z2 - wz * w2, r, g, b, a1, a2);
        float upx = Math.abs(wy) > 0.8f ? 1.0f : 0.0f;
        float upy = Math.abs(wy) > 0.8f ? 0.0f : 1.0f;
        fadeStrip(matrix, consumer, x1 - upx * w1, y1 - upy * w1, z1, x1 + upx * w1, y1 + upy * w1, z1, x2 + upx * w2, y2 + upy * w2, z2, x2 - upx * w2, y2 - upy * w2, z2, r, g, b, a1, a2);
    }

    private static void fadeStrip(Matrix4f matrix, VertexConsumer consumer,
                                  float x1, float y1, float z1, float x2, float y2, float z2,
                                  float x3, float y3, float z3, float x4, float y4, float z4,
                                  float r, float g, float b, float a1, float a2) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a1).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a2).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a2).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
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
