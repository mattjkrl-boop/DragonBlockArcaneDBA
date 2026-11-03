package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.BraveChargeEntity;
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
 * Entity Renderer for Brave Sword Heroic Focus Charge-up in Minecraft 26.2.
 * Renders physical 3D ground starburst heroic runes, orbiting inward-converging valor prisms,
 * a pulsating heroic octahedron core matrix, and ascending helical valor ribbons.
 */
public class BraveChargeRenderer extends EntityRenderer<BraveChargeEntity, BraveChargeRenderer.ChargeRenderState> {

    public BraveChargeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ChargeRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public float age = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(BraveChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(BraveChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ChargeRenderState createRenderState() {
        return new ChargeRenderState();
    }

    @Override
    public void extractRenderState(BraveChargeEntity entity, ChargeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.age = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(ChargeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        float charge = state.chargeRatio;
        float age = state.age;

        float baseRadius = 1.3f + (charge * 1.5f);
        // Spawn fade-in over the first 4 ticks so the charge construct does not
        // pop in at full brightness.
        float baseAlpha = (0.65f + (charge * 0.35f)) * Math.min(1.0f, age / 4.0f);
        float pulse = 0.90f + 0.10f * (float) Math.sin(age * 0.5f);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Planar 3D Ground Heroic Runes (3 counter-rotating ground rings)
            int groundSegments = 40;
            // Outer Ring: Radiant Gold Starburst Ring
            drawRotatingRing(matrix, buffer, 0, 0.03f, 0, baseRadius * 1.15f, baseRadius * 0.98f, groundSegments, age * -16.0f,
                1.0f, 0.84f, 0.0f, baseAlpha * 0.80f);
            // Middle Ring: Heroic Cyan Plasma Accretion
            drawRotatingRing(matrix, buffer, 0, 0.06f, 0, baseRadius * 0.82f, baseRadius * 0.68f, groundSegments, age * 26.0f,
                0.0f, 0.95f, 1.0f, baseAlpha * 0.88f);
            // Inner Core Disc: Blinding White-Gold Starburst Core
            drawRotatingRing(matrix, buffer, 0, 0.09f, 0, baseRadius * 0.50f, baseRadius * 0.36f, groundSegments, age * -38.0f,
                1.0f, 0.98f, 0.80f, baseAlpha * 0.95f);

            // 2. Inward-Converging Gravitational Valor Shards (3D golden-cyan crystalline prisms)
            int shardCount = 8 + (int) (charge * 6); // 8 to 14 shards
            float orbitRadius = (2.2f - charge * 0.65f) * pulse;
            float orbitSpeed = 20.0f + (charge * 55.0f);

            for (int i = 0; i < shardCount; i++) {
                double shardAngle = (i / (double) shardCount) * Math.PI * 2.0 + Math.toRadians(age * orbitSpeed);
                float sy = 0.4f + (float) Math.sin(age * 0.35f + i * 1.3f) * 0.5f + (i % 2 == 0 ? 0.3f : 0.0f);

                float sx = (float) Math.cos(shardAngle) * orbitRadius;
                float sz = (float) Math.sin(shardAngle) * orbitRadius;

                // Angle tip directly inward toward sword core (y = 0.9)
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

                float shardLen = 0.55f + (charge * 0.25f);
                float shardTipX = sx + toCenterX * shardLen;
                float shardTipY = sy + toCenterY * shardLen;
                float shardTipZ = sz + toCenterZ * shardLen;

                float perpX = -toCenterZ * 0.14f;
                float perpZ = toCenterX * 0.14f;

                // 3D Shard geometry
                draw3DShard(matrix, buffer,
                    sx - perpX, sy, sz - perpZ,
                    sx + perpX, sy, sz + perpZ,
                    shardTipX, shardTipY, shardTipZ,
                    1.0f, 0.80f, 0.0f, baseAlpha * 0.85f,
                    0.0f, 0.95f, 1.0f, baseAlpha * 0.98f
                );
            }

            // 3. Heroic Valorous Octahedron Core Matrix (Pulsating nested 3D octahedrons around caster)
            float coreSize = (0.70f - charge * 0.20f) * pulse;
            drawOctahedron(matrix, buffer, coreSize, 0.85f, age * 22.0f, 1.0f, 0.84f, 0.0f, baseAlpha * 0.80f);
            drawOctahedron(matrix, buffer, coreSize * 0.58f, 0.85f, -age * 32.0f, 0.0f, 0.95f, 1.0f, baseAlpha * 0.95f);
            drawOctahedron(matrix, buffer, coreSize * 0.22f, 0.85f, age * 55.0f, 1.0f, 1.0f, 1.0f, baseAlpha);

            // 4. Twin Helical Valor Light Ribbons (Ascending golden-cyan ribbons)
            int ribbonSteps = 12;
            for (int strand = 0; strand < 2; strand++) {
                float strandOffset = strand * (float) Math.PI;
                boolean isGoldStrand = (strand == 0);
                float sr = isGoldStrand ? 1.0f : 0.0f;
                float sg = isGoldStrand ? 0.85f : 0.95f;
                float sb = isGoldStrand ? 0.1f : 1.0f;

                for (int s = 0; s < ribbonSteps; s++) {
                    float t1 = s / (float) ribbonSteps;
                    float t2 = (s + 1) / (float) ribbonSteps;

                    float y1 = t1 * 2.3f;
                    float y2 = t2 * 2.3f;

                    float r1 = (baseRadius * 0.65f) * (0.8f + 0.3f * (float) Math.sin(t1 * Math.PI));
                    float r2 = (baseRadius * 0.65f) * (0.8f + 0.3f * (float) Math.sin(t2 * Math.PI));

                    double ang1 = strandOffset + (t1 * Math.PI * 2.6) + (age * 0.20);
                    double ang2 = strandOffset + (t2 * Math.PI * 2.6) + (age * 0.20);

                    float x1 = (float) Math.cos(ang1) * r1;
                    float z1 = (float) Math.sin(ang1) * r1;
                    float x2 = (float) Math.cos(ang2) * r2;
                    float z2 = (float) Math.sin(ang2) * r2;

                    float rw = 0.14f * (1.0f - t1 * 0.4f);

                    drawQuad(matrix, buffer,
                        x1 - rw, y1, z1 - rw,
                        x1 + rw, y1, z1 + rw,
                        x2 + rw, y2, z2 + rw,
                        x2 - rw, y2, z2 - rw,
                        sr, sg, sb, baseAlpha * (1.0f - t1 * 0.4f)
                    );
                    float cw = rw * 0.28f;
                    drawQuad(matrix, buffer,
                        x1 - cw, y1, z1,
                        x1 + cw, y1, z1,
                        x2 + cw, y2, z2,
                        x2 - cw, y2, z2,
                        1.0f, 1.0f, 1.0f, baseAlpha * (1.0f - t1 * 0.25f)
                    );
                }
            }
        });

        // Additive halo pass: a wider faint copy of the outer gold ground ring
        // and the octahedron core so the charge blooms and anchors to the ground.
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            drawRotatingRing(matrix, buffer, 0, 0.03f, 0, baseRadius * 1.30f, baseRadius * 1.10f, 40, age * -16.0f,
                1.0f, 0.84f, 0.0f, baseAlpha * 0.15f);
            float coreSize = (0.70f - charge * 0.20f) * pulse * 1.25f;
            drawOctahedron(matrix, buffer, coreSize, 0.85f, age * 22.0f, 1.0f, 0.84f, 0.0f, baseAlpha * 0.18f);
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
        float mx = (x1 + x2) * 0.5f;
        float my = (y1 + y2) * 0.5f;
        float mz = (z1 + z2) * 0.5f;
        float hx = (x2 - x1) * 0.5f;
        float hy = (y2 - y1) * 0.5f;
        float hz = (z2 - z1) * 0.5f;
        float dx = tipX - mx;
        float dy = tipY - my;
        float dz = tipZ - mz;
        float bx = hy * dz - hz * dy;
        float by = hz * dx - hx * dz;
        float bz = hx * dy - hy * dx;
        float bl = (float) Math.sqrt(bx * bx + by * by + bz * bz);
        if (bl > 0.001f) {
            bx /= bl;
            by /= bl;
            bz /= bl;
        }
        float bowAmp = (float) Math.sqrt(hx * hx + hy * hy + hz * hz) * 1.1f;
        int steps = 5;
        for (int i = 0; i < steps; i++) {
            float t1 = i / (float) steps;
            float t2 = (i + 1) / (float) steps;
            float bow1 = (float) Math.sin(t1 * Math.PI) * bowAmp;
            float bow2 = (float) Math.sin(t2 * Math.PI) * bowAmp;
            float w1 = 1.0f - t1;
            float w2 = 1.0f - t2;
            float ax = mx + dx * t1 + bx * bow1;
            float ay = my + dy * t1 + by * bow1;
            float az = mz + dz * t1 + bz * bow1;
            float cx = mx + dx * t2 + bx * bow2;
            float cy = my + dy * t2 + by * bow2;
            float cz = mz + dz * t2 + bz * bow2;
            float ar = rBase + (rTip - rBase) * t1;
            float ag = gBase + (gTip - gBase) * t1;
            float ab = bBase + (bTip - bBase) * t1;
            float aa = aBase + (aTip - aBase) * t1;
            float br = rBase + (rTip - rBase) * t2;
            float bg = gBase + (gTip - gBase) * t2;
            float bb = bBase + (bTip - bBase) * t2;
            float ba = aBase + (aTip - aBase) * t2;
            consumer.addVertex(matrix, ax - hx * w1, ay - hy * w1, az - hz * w1).setColor(ar, ag, ab, aa).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, ax + hx * w1, ay + hy * w1, az + hz * w1).setColor(ar, ag, ab, aa).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx + hx * w2, cy + hy * w2, cz + hz * w2).setColor(br, bg, bb, ba * 0.15f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx - hx * w2, cy - hy * w2, cz - hz * w2).setColor(br, bg, bb, ba * 0.15f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            float k1 = w1 * 0.26f;
            float k2 = w2 * 0.26f;
            consumer.addVertex(matrix, ax - hx * k1, ay - hy * k1, az - hz * k1).setColor(1.0f, 1.0f, 1.0f, aa).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, ax + hx * k1, ay + hy * k1, az + hz * k1).setColor(1.0f, 1.0f, 1.0f, aa).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx + hx * k2, cy + hy * k2, cz + hz * k2).setColor(0.7f, 0.98f, 1.0f, ba * 0.2f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx - hx * k2, cy - hy * k2, cz - hz * k2).setColor(0.7f, 0.98f, 1.0f, ba * 0.2f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
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
