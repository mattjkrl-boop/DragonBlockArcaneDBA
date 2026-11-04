package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.HellHuntImpactEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * Entity Renderer for Hell Hunt Impact in Minecraft 26.2.
 * Renders physical 3D demonic ground-shatter fissure trenches, erupting jagged blood/obsidian impalement spikes,
 * expanding shockwave execution seals, and surging vertical demonic execution pillars.
 */
public class HellHuntImpactRenderer extends EntityRenderer<HellHuntImpactEntity, HellHuntImpactRenderer.HellHuntRenderState> {

    public HellHuntImpactRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class HellHuntRenderState extends EntityRenderState {
        public float radius = 3.0f;
        public boolean isFinal = false;
        public float age = 0;
        public int lifetime = 35;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(HellHuntImpactEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(HellHuntImpactEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public HellHuntRenderState createRenderState() {
        return new HellHuntRenderState();
    }

    @Override
    public void extractRenderState(HellHuntImpactEntity entity, HellHuntRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.isFinal = entity.isFinal();
        state.age = entity.tickCount + partialTicks;
        // The client side entity never runs the convenience constructor, so
        // the total life comes from the synced final flag and the shared
        // constants, not from the entity's server side countdown field.
        state.lifetime = state.isFinal ? HellHuntImpactEntity.LIFE_FINAL : HellHuntImpactEntity.LIFE_NORMAL;
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(HellHuntRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float radius = state.radius;
        float age = state.age;
        // Shared lifecycle envelope (standard point 5): builds over the
        // first fifth of life, peaks, then dissipates over the last third,
        // so the renderer agrees with the entity side particles and scar.
        float lifeT = Math.min(1.0f, age / (float) state.lifetime);
        final float fade = com.dragonblockarcanedba.util.FxEnvelope.envelope(age, state.lifetime);
        final float ds = com.dragonblockarcanedba.client.render.ki.LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        boolean isFinal = state.isFinal;
        RenderType renderType = KiRenderHelper.kiRenderType();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);

            // 1. Expanding Demonic Ground Impact Shockwave Disks & Execution Seals
            // The seals now counter-rotate slowly (redesign T4); they used to
            // sit frozen while the trenches and spikes animated around them.
            float growT = Math.min(1.0f, age / 10.0f);
            float grow = 1.0f - (1.0f - growT) * (1.0f - growT) * (1.0f - growT);
            float shockR = radius * 1.35f * grow;
            if (fade > 0.05f) {
                // Outer Shock Ring: Dark Crimson Runes
                drawGroundRing(matrix, buffer, 0, 0.04f, 0, shockR, shockR * 0.82f, com.dragonblockarcanedba.util.FxEnvelope.segments(32, ds, 16), age * 10.0f, 0.85f, 0.0f, 0.15f, fade * 0.85f);
                // Inner Blood Ring, a ring rather than a filled disc, plus a tight bright core.
                drawGroundRing(matrix, buffer, 0, 0.05f, 0, shockR * 0.65f, shockR * 0.40f, com.dragonblockarcanedba.util.FxEnvelope.segments(24, ds, 12), -age * 14.0f, 1.0f, 0.05f, 0.25f, fade * 0.75f);
                drawGroundRing(matrix, buffer, 0, 0.07f, 0, shockR * 0.22f, shockR * 0.12f, com.dragonblockarcanedba.util.FxEnvelope.segments(20, ds, 10), age * 22.0f, 1.0f, 0.88f, 0.92f, fade * 0.9f);
                if (isFinal) {
                    // Extra Halos on Final Finisher Impact
                    drawGroundRing(matrix, buffer, 0, 0.06f, 0, shockR * 1.15f, shockR * 0.95f, com.dragonblockarcanedba.util.FxEnvelope.segments(32, ds, 16), age * 6.0f, 1.0f, 0.85f, 0.90f, fade * 0.90f);
                }

                // Shared layered language (standard point 1): an anchor glow
                // grounding the seal, and a three band vortex column while
                // the execution pillar stands, so the impact reads with the
                // same depth as every other spectacle effect.
                com.dragonblockarcanedba.client.render.ki.LayeredRender.drawAnchorGlow(pose, buffer,
                    shockR * 0.95f, 0.02f, 1.0f, 0.10f, 0.22f, 0.50f * fade, ds);
                float bandEnv = fade * (age <= 14f ? 1.0f : Math.max(0f, 1.0f - (age - 14f) / 10f));
                com.dragonblockarcanedba.client.render.ki.LayeredRender.drawVortexBands(pose, buffer,
                    radius * 0.5f, isFinal ? 4.2f : 3.0f, age, 1.0f, 0.07f, 0.16f, bandEnv, ds);
            }

            // 2. 8 Radiating 3D Demonic Tectonic Fissure Trenches
            int trenchCount = isFinal ? 10 : 8;
            for (int t = 0; t < trenchCount; t++) {
                double tAngle = (t / (double) trenchCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.25);
                float trenchLen = radius * (0.70f + rng.nextFloat() * 0.50f);

                float curX = 0, curZ = 0;
                int steps = 5;
                for (int s = 0; s < steps; s++) {
                    float nxtX = curX + (float) Math.cos(tAngle) * (trenchLen / steps) + (rng.nextFloat() - 0.5f) * 0.40f;
                    float nxtZ = curZ + (float) Math.sin(tAngle) * (trenchLen / steps) + (rng.nextFloat() - 0.5f) * 0.40f;

                    float width = 0.30f * (1.0f - (s / (float) steps)) * Math.min(1.0f, age * 0.35f);
                    float along = s / (float) steps;
                    drawTrenchRibbon(matrix, buffer, curX, curZ, nxtX, nxtZ, width, along, fade);

                    curX = nxtX; curZ = nxtZ;
                }
            }

            // 3. 12 to 18 Erupting 3D Jagged Curse Obsidian / Blood Spikes
            int spikeCount = isFinal ? 18 : 12;
            for (int r = 0; r < spikeCount; r++) {
                double spikeAngle = (r / (double) spikeCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.35);
                float spikeDist = radius * (0.30f + rng.nextFloat() * 0.60f);

                float rx = (float) Math.cos(spikeAngle) * spikeDist;
                float rz = (float) Math.sin(spikeAngle) * spikeDist;

                float riseT = Math.min(1.0f, age / 8.0f);
                float rise = 1.0f - (1.0f - riseT) * (1.0f - riseT) * (1.0f - riseT);
                float sinkT = lifeT < 0.55f ? 0.0f : (lifeT - 0.55f) / 0.45f;
                sinkT = sinkT * sinkT * (3.0f - 2.0f * sinkT);
                float spikeHeight = (0.6f + rng.nextFloat() * (isFinal ? 1.4f : 0.9f)) * rise * (1.0f - sinkT);
                float spikeWidth = 0.30f + rng.nextFloat() * 0.25f;

                // Spike leans outward from center
                float tiltX = (float) Math.cos(spikeAngle) * 0.35f;
                float tiltZ = (float) Math.sin(spikeAngle) * 0.35f;

                float tx = -(float) Math.sin(spikeAngle) * spikeWidth * 0.5f;
                float tz = (float) Math.cos(spikeAngle) * spikeWidth * 0.5f;

                drawSpike(matrix, buffer,
                    rx - tx, 0.05f, rz - tz,
                    rx + tx, 0.05f, rz + tz,
                    rx + tiltX, spikeHeight, rz + tiltZ,
                    0.20f, 0.0f, 0.05f, 0.95f * fade,
                    1.0f, 0.10f, 0.25f, 1.0f * fade
                );
            }

            // 4. Surging Vertical Demonic Execution Pillar / Impalement Beam
            float pillarLife = isFinal ? 18.0f : 12.0f;
            if (age < pillarLife) {
                float pillarProgress = age / pillarLife;
                // The pillar is an instant strike visual, so it builds over
                // 3 ticks instead of the full envelope build, but it no
                // longer appears at full alpha on frame one either.
                float pillarBuild = com.dragonblockarcanedba.util.FxEnvelope.smoothstep(Math.min(1.0f, age / 3.0f));
                float pillarAlpha = (1.0f - pillarProgress) * (isFinal ? 1.0f : 0.85f) * pillarBuild;
                float pillarHeight = (isFinal ? 8.5f : 5.5f) * Math.min(1.0f, age * 0.4f);
                float pillarRadius = (isFinal ? 0.85f : 0.55f) * (1.0f + pillarProgress * 0.3f);

                // The column comes to a point. Crimson sheath, pale core, both tapered.
                drawConePillar(matrix, buffer, 0, 0, pillarRadius, pillarHeight, 16, 0.85f, 0.0f, 0.15f, pillarAlpha * 0.65f);
                drawConePillar(matrix, buffer, 0, 0, pillarRadius * 0.38f, pillarHeight * 1.04f, 10, 1.0f, 0.85f, 0.90f, pillarAlpha * 0.95f);
            }
        });
    }

    /** Raised fissure: dark walls, a bright ridge, and a tail that sinks back into the ground. */
    private static void drawTrenchRibbon(Matrix4f matrix, VertexConsumer consumer, float x1, float z1, float x2, float z2, float width, float along, float fade) {
        float dx = x2 - x1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001f) return;

        float nx = -dz / len * width;
        float nz = dx / len * width;
        float ridge1 = width * 1.7f * (1.0f - along);
        float ridge2 = width * 1.7f * (1.0f - Math.min(1.0f, along + 0.2f));
        float a1 = fade * (1.0f - along * 0.35f);
        float a2 = fade * (1.0f - Math.min(1.0f, along + 0.2f) * 0.55f);

        face(matrix, consumer, x1 - nx, 0.03f, z1 - nz, 0.45f, 0.0f, 0.08f, a1, x2 - nx, 0.03f, z2 - nz, 0.45f, 0.0f, 0.08f, a2, x2, ridge2, z2, 1.0f, 0.35f, 0.4f, a2, x1, ridge1, z1, 1.0f, 0.35f, 0.4f, a1);
        face(matrix, consumer, x1 + nx, 0.03f, z1 + nz, 0.45f, 0.0f, 0.08f, a1, x1, ridge1, z1, 1.0f, 0.35f, 0.4f, a1, x2, ridge2, z2, 1.0f, 0.35f, 0.4f, a2, x2 + nx, 0.03f, z2 + nz, 0.45f, 0.0f, 0.08f, a2);
        face(matrix, consumer, x1 - nx * 0.22f, ridge1 * 0.55f, z1 - nz * 0.22f, 1.0f, 0.86f, 0.9f, a1, x2 - nx * 0.22f, ridge2 * 0.55f, z2 - nz * 0.22f, 1.0f, 0.86f, 0.9f, a2 * 0.15f, x2 + nx * 0.22f, ridge2 * 0.55f, z2 + nz * 0.22f, 1.0f, 0.86f, 0.9f, a2 * 0.15f, x1 + nx * 0.22f, ridge1 * 0.55f, z1 + nz * 0.22f, 1.0f, 0.86f, 0.9f, a1);
    }

    private static void drawSpike(Matrix4f matrix, VertexConsumer consumer,
                                  float x1, float y1, float z1,
                                  float x2, float y2, float z2,
                                  float tipX, float tipY, float tipZ,
                                  float rBase, float gBase, float bBase, float aBase,
                                  float rTip, float gTip, float bTip, float aTip) {
        float mx = (x1 + x2) * 0.5f;
        float my = (y1 + y2) * 0.5f;
        float mz = (z1 + z2) * 0.5f;
        float ex = (x2 - x1) * 0.5f;
        float ey = (y2 - y1) * 0.5f;
        float ez = (z2 - z1) * 0.5f;
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
            py = Math.max(elen, 0.05f);
            pz = 0.0f;
            plen = py;
        }
        float thick = Math.max(elen, 0.04f) * 0.62f;
        px = px / plen * thick;
        py = py / plen * thick;
        pz = pz / plen * thick;

        float[] bx = { mx - ex, mx + px, mx + ex, mx - px };
        float[] by = { my - ey, my + py, my + ey, my - py };
        float[] bz = { mz - ez, mz + pz, mz + ez, mz - pz };
        for (int i = 0; i < 4; i++) {
            int n = (i + 1) % 4;
            face(matrix, consumer, bx[i], by[i], bz[i], rBase, gBase, bBase, aBase, bx[n], by[n], bz[n], rBase, gBase, bBase, aBase, tipX, tipY, tipZ, rTip, gTip, bTip, aTip, tipX, tipY, tipZ, rTip, gTip, bTip, aTip);
        }
        float core = 0.38f;
        float cx = mx + ax * 0.22f;
        float cy = my + ay * 0.22f;
        float cz = mz + az * 0.22f;
        float coreTipX = mx + ax * 0.92f;
        float coreTipY = my + ay * 0.92f;
        float coreTipZ = mz + az * 0.92f;
        for (int i = 0; i < 4; i++) {
            int n = (i + 1) % 4;
            float ix = cx + (bx[i] - mx) * core;
            float iy = cy + (by[i] - my) * core;
            float iz = cz + (bz[i] - mz) * core;
            float jx = cx + (bx[n] - mx) * core;
            float jy = cy + (by[n] - my) * core;
            float jz = cz + (bz[n] - mz) * core;
            face(matrix, consumer, ix, iy, iz, 1.0f, 0.82f, 0.86f, aTip, jx, jy, jz, 1.0f, 0.82f, 0.86f, aTip, coreTipX, coreTipY, coreTipZ, 1.0f, 0.95f, 0.97f, aTip, coreTipX, coreTipY, coreTipZ, 1.0f, 0.95f, 0.97f, aTip);
        }
        float alen = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        float inv = alen < 1.0e-4f ? 0.0f : 0.55f / alen;
        float wx = mx - ax * inv;
        float wy = Math.max(0.02f, my - ay * inv);
        float wz = mz - az * inv;
        face(matrix, consumer, bx[0], by[0], bz[0], rBase, gBase, bBase, aBase * 0.45f, bx[2], by[2], bz[2], rBase, gBase, bBase, aBase * 0.45f, wx, wy, wz, rBase, gBase, bBase, 0.0f, wx, wy, wz, rBase, gBase, bBase, 0.0f);
    }

    private static void face(Matrix4f matrix, VertexConsumer consumer,
                             float x1, float y1, float z1, float r1, float g1, float b1, float a1,
                             float x2, float y2, float z2, float r2, float g2, float b2, float a2,
                             float x3, float y3, float z3, float r3, float g3, float b3, float a3,
                             float x4, float y4, float z4, float r4, float g4, float b4, float a4) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r1, g1, b1, a1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r2, g2, b2, a2).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r3, g3, b3, a3).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x4, y4, z4).setColor(r4, g4, b4, a4).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawGroundRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float rotDeg, float r, float g, float b, float a) {
        double rotRad = Math.toRadians(rotDeg);
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2 + rotRad;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2 + rotRad;

            float x1 = cx + (float) Math.cos(a1) * rOuter;
            float z1 = cz + (float) Math.sin(a1) * rOuter;
            float x2 = cx + (float) Math.cos(a2) * rOuter;
            float z2 = cz + (float) Math.sin(a2) * rOuter;

            float ix1 = cx + (float) Math.cos(a1) * rInner;
            float iz1 = cz + (float) Math.sin(a1) * rInner;
            float ix2 = cx + (float) Math.cos(a2) * rInner;
            float iz2 = cz + (float) Math.sin(a2) * rInner;

            consumer.addVertex(matrix, ix1, cy, iz1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, ix2, cy, iz2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, cy, z2).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, cy, z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawConePillar(Matrix4f matrix, VertexConsumer consumer, float cx, float cz, float radius, float height, int sides, float r, float g, float b, float a) {
        for (int i = 0; i < sides; i++) {
            double a1 = (i / (double) sides) * Math.PI * 2;
            double a2 = ((i + 1) / (double) sides) * Math.PI * 2;

            float x1 = cx + (float) Math.cos(a1) * radius;
            float z1 = cz + (float) Math.sin(a1) * radius;
            float x2 = cx + (float) Math.cos(a2) * radius;
            float z2 = cz + (float) Math.sin(a2) * radius;

            consumer.addVertex(matrix, x1, 0.05f, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, 0.05f, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx, height, cz).setColor(r, g, b, a * 0.15f).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx, height, cz).setColor(r, g, b, a * 0.15f).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            consumer.addVertex(matrix, cx, height, cz).setColor(r, g, b, a * 0.15f).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x2, 0.05f, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x1, 0.05f, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x1, 0.05f, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        }
    }
}
