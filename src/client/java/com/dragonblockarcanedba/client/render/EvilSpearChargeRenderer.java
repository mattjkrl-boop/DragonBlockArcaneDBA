package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.EvilSpearChargeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;

/**
 * Entity Renderer for Evil Spear Left-Click (Evil Impale) Charge-up in Minecraft 26.2.
 * Renders physical 3D demonic crimson ground seals, contracting energy torus gathering rings,
 * orbiting blood-crystal impalement shards, a pulsating demonic octahedron core matrix,
 * and ascending helical blood vortex ribbons.
 */
public class EvilSpearChargeRenderer extends EntityRenderer<EvilSpearChargeEntity, EvilSpearChargeRenderer.ChargeRenderState> {

    public EvilSpearChargeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ChargeRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public float age = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(EvilSpearChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(EvilSpearChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ChargeRenderState createRenderState() {
        return new ChargeRenderState();
    }

    @Override
    public void extractRenderState(EvilSpearChargeEntity entity, ChargeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.age = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(ChargeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        float charge = state.chargeRatio;
        float age = state.age;

        float baseRadius = 1.35f + (charge * 1.55f);
        float baseAlpha = 0.65f + (charge * 0.35f);
        float pulse = 0.90f + 0.10f * (float) Math.sin(age * 0.55f);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Planar 3D Demonic Ground Seals (3 counter-rotating runic rings)
            int groundSegments = 40;
            // Outer Ring: Dark Crimson Jagged Seal
            drawRotatingRing(matrix, buffer, 0, 0.03f, 0, baseRadius * 1.18f, baseRadius * 1.0f, groundSegments, age * -18.0f,
                0.80f, 0.0f, 0.10f, baseAlpha * 0.85f);
            // Middle Ring: Burning Blood-Red Accretion Ring
            drawRotatingRing(matrix, buffer, 0, 0.06f, 0, baseRadius * 0.84f, baseRadius * 0.68f, groundSegments, age * 28.0f,
                1.0f, 0.05f, 0.20f, baseAlpha * 0.92f);
            // Inner Core Disc: Blinding Crimson-White Seal Core
            drawRotatingRing(matrix, buffer, 0, 0.09f, 0, baseRadius * 0.50f, baseRadius * 0.34f, groundSegments, age * -42.0f,
                1.0f, 0.65f, 0.75f, baseAlpha * 0.98f);

            // 2. Physical 3D Inward-Gathering Demonic Energy Torus Rings
            int gatheringRingCount = 4;
            for (int r = 0; r < gatheringRingCount; r++) {
                float ringPhase = (age * 0.08f + (r / (float) gatheringRingCount)) % 1.0f;
                // Ring contracts inward and drops slightly toward the spear tip (y = 0.9)
                float currentR = (2.6f * (1.0f - ringPhase) + 0.35f * ringPhase) * pulse * (1.0f - charge * 0.3f);
                float currentY = 0.3f + ringPhase * 0.9f;
                float ringAlpha = (float) Math.sin(ringPhase * Math.PI) * baseAlpha * 0.85f;

                drawRotatingRing(matrix, buffer, 0, currentY, 0, currentR, currentR * 0.86f, 28, age * (30.0f + r * 15.0f),
                    1.0f, 0.05f + ringPhase * 0.4f, 0.15f + ringPhase * 0.5f, ringAlpha);
            }

            // 3. Inward-Converging 3D Orbiting Blood-Crystal Needles (3D sharp prisms pointing to center)
            int shardCount = 8 + (int) (charge * 6); // 8 to 14 shards
            float orbitRadius = (2.3f - charge * 0.75f) * pulse;
            float orbitSpeed = 25.0f + (charge * 60.0f);

            for (int i = 0; i < shardCount; i++) {
                double shardAngle = (i / (double) shardCount) * Math.PI * 2.0 + Math.toRadians(age * orbitSpeed);
                float sy = 0.45f + (float) Math.sin(age * 0.35f + i * 1.4f) * 0.5f + (i % 2 == 0 ? 0.35f : 0.0f);

                float sx = (float) Math.cos(shardAngle) * orbitRadius;
                float sz = (float) Math.sin(shardAngle) * orbitRadius;

                // Angle tip directly inward toward spear core (y = 0.9)
                float targetY = 0.9f;
                float toCenterX = -sx;
                float toCenterZ = -sz;
                float toCenterY = targetY - sy;
                float toCenterLen = (float) Math.sqrt(toCenterX * toCenterX + toCenterZ * toCenterZ + toCenterY * toCenterY);
                if (toCenterLen > 0.001f) {
                    toCenterX /= toCenterLen;
                    toCenterY /= toCenterLen;
                    toCenterZ /= toCenterLen;
                }

                float shardLen = 0.60f + (charge * 0.30f);
                float shardTipX = sx + toCenterX * shardLen;
                float shardTipY = sy + toCenterY * shardLen;
                float shardTipZ = sz + toCenterZ * shardLen;

                float perpX = -toCenterZ * 0.13f;
                float perpZ = toCenterX * 0.13f;

                // 3D Shard geometry
                draw3DShard(matrix, buffer,
                    sx - perpX, sy, sz - perpZ,
                    sx + perpX, sy, sz + perpZ,
                    shardTipX, shardTipY, shardTipZ,
                    0.75f, 0.0f, 0.12f, baseAlpha * 0.85f,
                    1.0f, 0.75f, 0.85f, baseAlpha * 0.98f
                );
            }

            // Nested seal: dark shell, blood mid, and a heart that brightens as the charge fills.
            float coreSize = (0.62f + charge * 0.18f) * pulse;
            drawOctahedron(matrix, buffer, coreSize, 0.85f, age * 24.0f, 0.85f, 0.0f, 0.12f, baseAlpha * 0.82f);
            drawOctahedron(matrix, buffer, coreSize * 0.58f, 0.85f, -age * 36.0f, 1.0f, 0.10f, 0.28f, baseAlpha * 0.95f);
            float heart = Math.min(1.0f, charge);
            drawOctahedron(matrix, buffer, coreSize * 0.26f, 0.85f, age * 60.0f,
                0.85f + 0.15f * heart, 0.12f + 0.78f * heart, 0.22f + 0.73f * heart, baseAlpha);
            // A thin filament tightens onto the seals while the button is held.
            float filament = baseRadius * (1.02f - charge * 0.42f);
            drawRotatingRing(matrix, buffer, 0, 0.12f, 0, filament, filament * 0.90f, 36, age * 48.0f,
                1.0f, 0.82f, 0.90f, baseAlpha * 0.72f);

            // 5. Twin Helical Blood Plasma Vortex Ribbons (Ascending dark-crimson ribbons)
            int ribbonSteps = 12;
            for (int strand = 0; strand < 2; strand++) {
                float strandOffset = strand * (float) Math.PI;
                boolean isPrimaryStrand = (strand == 0);
                float sr = isPrimaryStrand ? 1.0f : 0.85f;
                float sg = isPrimaryStrand ? 0.08f : 0.0f;
                float sb = isPrimaryStrand ? 0.22f : 0.40f;

                for (int s = 0; s < ribbonSteps; s++) {
                    float t1 = s / (float) ribbonSteps;
                    float t2 = (s + 1) / (float) ribbonSteps;

                    float y1 = t1 * 2.4f;
                    float y2 = t2 * 2.4f;

                    float r1 = (baseRadius * 0.65f) * (0.8f + 0.3f * (float) Math.sin(t1 * Math.PI));
                    float r2 = (baseRadius * 0.65f) * (0.8f + 0.3f * (float) Math.sin(t2 * Math.PI));

                    double ang1 = strandOffset + (t1 * Math.PI * 2.6) + (age * 0.22);
                    double ang2 = strandOffset + (t2 * Math.PI * 2.6) + (age * 0.22);

                    float x1 = (float) Math.cos(ang1) * r1;
                    float z1 = (float) Math.sin(ang1) * r1;
                    float x2 = (float) Math.cos(ang2) * r2;
                    float z2 = (float) Math.sin(ang2) * r2;

                    float rw1 = 0.15f * (1.0f - t1 * 0.45f);
                    float rw2 = 0.15f * (1.0f - t2 * 0.45f);
                    // Width sits across the helix, so the plasma is a ribbon instead of a stack of squares.
                    float px1 = (float) -Math.sin(ang1) * rw1;
                    float pz1 = (float) Math.cos(ang1) * rw1;
                    float px2 = (float) -Math.sin(ang2) * rw2;
                    float pz2 = (float) Math.cos(ang2) * rw2;
                    float lift1 = rw1 * 0.7f;
                    float lift2 = rw2 * 0.7f;

                    float ribbonTail = 1.0f - t1;
                    ribbonTail = ribbonTail * ribbonTail;
                    drawQuad(matrix, buffer,
                        x1 - px1, y1 - lift1, z1 - pz1,
                        x1 + px1, y1 + lift1, z1 + pz1,
                        x2 + px2, y2 + lift2, z2 + pz2,
                        x2 - px2, y2 - lift2, z2 - pz2,
                        sr, sg, sb, baseAlpha * ribbonTail
                    );
                }
            }
        });

        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawRotatingRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float rotDeg, float r, float g, float b, float a) {
        double rotRad = Math.toRadians(rotDeg);
        for (int i = 0; i < segments; i++) {
            double a1 = ((i / (double) segments) * Math.PI * 2.0) + rotRad;
            double a2 = (((i + 1) / (double) segments) * Math.PI * 2.0) + rotRad;

            float x1Out = cx + (float) Math.cos(a1) * rOuter;
            float z1Out = cz + (float) Math.sin(a1) * rOuter;
            float x2Out = cx + (float) Math.cos(a2) * rOuter;
            float z2Out = cz + (float) Math.sin(a2) * rOuter;

            float x1In = cx + (float) Math.cos(a1) * rInner;
            float z1In = cz + (float) Math.sin(a1) * rInner;
            float x2In = cx + (float) Math.cos(a2) * rInner;
            float z2In = cz + (float) Math.sin(a2) * rInner;

            drawQuad(matrix, consumer, x1In, cy, z1In, x2In, cy, z2In, x2Out, cy, z2Out, x1Out, cy, z1Out, r, g, b, a);
        }
    }

    private static void draw3DShard(Matrix4f matrix, VertexConsumer consumer,
                                   float x1, float y1, float z1,
                                   float x2, float y2, float z2,
                                   float tipX, float tipY, float tipZ,
                                   float rBase, float gBase, float bBase, float aBase,
                                   float rTip, float gTip, float bTip, float aTip) {
        crystal(matrix, consumer, x1, y1, z1, x2, y2, z2, tipX, tipY, tipZ, rBase, gBase, bBase, aBase, rTip, gTip, bTip, aTip);
    }

    /** Four-sided needle: dark edge, pale core, and a wake off the back. */
    private static void crystal(Matrix4f matrix, VertexConsumer consumer,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float tipX, float tipY, float tipZ,
                                float rBase, float gBase, float bBase, float aBase,
                                float rTip, float gTip, float bTip, float aTip) {
        float mx = (x1 + x2) * 0.5f;
        float my = (y1 + y2) * 0.5f;
        float mz = (z1 + z2) * 0.5f;
        float ex = x2 - mx;
        float ey = y2 - my;
        float ez = z2 - mz;
        float ax = tipX - mx;
        float ay = tipY - my;
        float az = tipZ - mz;
        float px = ey * az - ez * ay;
        float py = ez * ax - ex * az;
        float pz = ex * ay - ey * ax;
        float plen = (float) Math.sqrt(px * px + py * py + pz * pz);
        float elen = (float) Math.sqrt(ex * ex + ey * ey + ez * ez);
        if (plen < 1.0e-4f) {
            px = 0.0f;
            py = Math.max(elen, 0.04f);
            pz = 0.0f;
            plen = py;
        }
        float thick = Math.max(elen, 0.03f);
        px = px / plen * thick;
        py = py / plen * thick;
        pz = pz / plen * thick;
        float[] bx = { mx - ex, mx + px, mx + ex, mx - px };
        float[] by = { my - ey, my + py, my + ey, my - py };
        float[] bz = { mz - ez, mz + pz, mz + ez, mz - pz };
        for (int i = 0; i < 4; i++) {
            int n = (i + 1) % 4;
            consumer.addVertex(matrix, bx[i], by[i], bz[i]).setColor(rBase, gBase, bBase, aBase).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, bx[n], by[n], bz[n]).setColor(rBase, gBase, bBase, aBase).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            float ix = mx + (bx[i] - mx) * 0.34f + ax * 0.2f;
            float iy = my + (by[i] - my) * 0.34f + ay * 0.2f;
            float iz = mz + (bz[i] - mz) * 0.34f + az * 0.2f;
            float jx = mx + (bx[n] - mx) * 0.34f + ax * 0.2f;
            float jy = my + (by[n] - my) * 0.34f + ay * 0.2f;
            float jz = mz + (bz[n] - mz) * 0.34f + az * 0.2f;
            float kx = mx + ax * 0.86f;
            float ky = my + ay * 0.86f;
            float kz = mz + az * 0.86f;
            consumer.addVertex(matrix, ix, iy, iz).setColor(1.0f, 0.86f, 0.9f, aTip).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, jx, jy, jz).setColor(1.0f, 0.86f, 0.9f, aTip).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, kx, ky, kz).setColor(1.0f, 0.97f, 0.98f, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, kx, ky, kz).setColor(1.0f, 0.97f, 0.98f, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
        float alen = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        float back = alen < 1.0e-4f ? 0.0f : 0.45f / alen;
        consumer.addVertex(matrix, bx[0], by[0], bz[0]).setColor(rBase, gBase, bBase, aBase * 0.4f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, bx[2], by[2], bz[2]).setColor(rBase, gBase, bBase, aBase * 0.4f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, mx - ax * back, my - ay * back, mz - az * back).setColor(rBase, gBase, bBase, 0.0f).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, mx - ax * back, my - ay * back, mz - az * back).setColor(rBase, gBase, bBase, 0.0f).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawOctahedron(Matrix4f matrix, VertexConsumer consumer, float s, float centerY, float rotDeg, float r, float g, float b, float a) {
        double rad = Math.toRadians(rotDeg);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        float[][] local = { {s, 0}, {0, s}, {-s, 0}, {0, -s} };
        float[][] rotated = new float[4][2];
        for (int i = 0; i < 4; i++) {
            rotated[i][0] = local[i][0] * cos - local[i][1] * sin;
            rotated[i][1] = local[i][0] * sin + local[i][1] * cos;
        }

        for (int i = 0; i < 4; i++) {
            int nxt = (i + 1) % 4;

            // Top pyramid
            consumer.addVertex(matrix, rotated[i][0], centerY, rotated[i][1]).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, rotated[nxt][0], centerY, rotated[nxt][1]).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 0, centerY + s * 1.3f, 0).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 0, centerY + s * 1.3f, 0).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Bottom pyramid
            consumer.addVertex(matrix, rotated[i][0], centerY, rotated[i][1]).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, rotated[nxt][0], centerY, rotated[nxt][1]).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, 0, centerY - s * 1.3f, 0).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, 0, centerY - s * 1.3f, 0).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        }
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

        // Reverse
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }
}
