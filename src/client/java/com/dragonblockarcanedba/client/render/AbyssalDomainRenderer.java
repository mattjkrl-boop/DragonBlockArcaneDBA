package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.AbyssalDomainEntity;
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
 * Entity Renderer for Abyssal Eclipse Void Domain in Minecraft 26.2.
 * Completely replaces vanilla smoke/dust particle spam with a monumental 3D supernatural environment:
 * - Double-layered volumetric 3D void cloud dome
 * - Quad-tier counter-rotating corrupted ground whirlpools
 * - 16 Orbiting 3D jagged obsidian void spires
 * - Central vertical cyclonic vortex updraft column
 * - Atmospheric 3D void lightning discharges across the dome interior
 */
public class AbyssalDomainRenderer extends EntityRenderer<AbyssalDomainEntity, AbyssalDomainRenderer.AbyssalDomainRenderState> {

    public AbyssalDomainRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class AbyssalDomainRenderState extends EntityRenderState {
        public float radius = 10.0f;
        public float age = 0;
        public float fade = 1.0f;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(AbyssalDomainEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(AbyssalDomainEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public AbyssalDomainRenderState createRenderState() {
        return new AbyssalDomainRenderState();
    }

    @Override
    public void extractRenderState(AbyssalDomainEntity entity, AbyssalDomainRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.age = entity.tickCount + partialTicks;
        state.fade = entity.getFadeFraction();
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(AbyssalDomainRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float radius = state.radius;
        float age = state.age;
        // Lifecycle envelope (point 5): the dome builds in over its first
        // 40 ticks and collapses across the entity's synced fade, instead of
        // the old constant alphas that popped in and out with the entity.
        // Every alpha below is multiplied by env.
        float build = com.dragonblockarcanedba.util.FxEnvelope.smoothstep(Math.min(1.0f, age / 40.0f));
        float env = build * com.dragonblockarcanedba.util.FxEnvelope.smoothstep(state.fade);
        if (env <= 0.01f) return;
        float ds = com.dragonblockarcanedba.client.render.ki.LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        RenderType renderType = KiRenderHelper.kiRenderType();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Quad-Tier Counter-Rotating Ground Whirlpools (4 dark corrupted energy bands)
            int groundSegments = 48;
            for (int band = 0; band < 4; band++) {
                float rOuter = radius * (0.30f + band * 0.23f);
                float rInner = rOuter * 0.83f;
                float rotSpeed = (band % 2 == 0 ? 1.0f : -1.3f) * (12.0f + band * 6.0f);
                float bandAlpha = (0.65f - (band * 0.08f)) * env;

                float red = band % 2 == 0 ? 0.95f : 0.45f;
                float green = 0.02f;
                float blue = band % 2 == 0 ? 0.20f : 0.95f;

                drawRotatingRing(matrix, buffer, 0, 0.08f + (band * 0.05f), 0, rOuter, rInner, groundSegments, age * rotSpeed, red, green, blue, bandAlpha);
            }

            // 2. Volumetric Double-Layered Void Cyclone Dome
            float domeHeight = Math.min(24.0f, radius * 1.30f);

            // Layer A: Outer Massive Rotating Void Cloud Dome
            int domeLatitudes = 12;
            int domeLongitudes = 36;
            for (int lat = 0; lat < domeLatitudes; lat++) {
                float latProgress1 = lat / (float) domeLatitudes;
                float latProgress2 = (lat + 1) / (float) domeLatitudes;

                float y1 = 1.0f + (float) Math.sin(latProgress1 * (Math.PI / 2.0)) * domeHeight;
                float y2 = 1.0f + (float) Math.sin(latProgress2 * (Math.PI / 2.0)) * domeHeight;

                float r1 = radius * (float) Math.cos(latProgress1 * (Math.PI / 2.0));
                float r2 = radius * (float) Math.cos(latProgress2 * (Math.PI / 2.0));

                float domeAlpha = (0.35f + (latProgress1 * 0.35f)) * env;
                float red = 0.15f + (latProgress1 * 0.65f);
                float green = 0.01f;
                float blue = 0.35f + (latProgress1 * 0.55f);

                for (int lon = 0; lon < domeLongitudes; lon++) {
                    double angleOffset = (age * (4.0f + lat * 1.5f)) * (Math.PI / 180.0);
                    double a1 = ((lon / (double) domeLongitudes) * Math.PI * 2.0) + angleOffset;
                    double a2 = (((lon + 1) / (double) domeLongitudes) * Math.PI * 2.0) + angleOffset;

                    float x1 = (float) Math.cos(a1) * r1;
                    float z1 = (float) Math.sin(a1) * r1;
                    float x2 = (float) Math.cos(a2) * r1;
                    float z2 = (float) Math.sin(a2) * r1;

                    float x3 = (float) Math.cos(a2) * r2;
                    float z3 = (float) Math.sin(a2) * r2;
                    float x4 = (float) Math.cos(a1) * r2;
                    float z4 = (float) Math.sin(a1) * r2;

                    drawQuad(matrix, buffer, x1, y1, z1, x2, y1, z2, x3, y2, z3, x4, y2, z4, red, green, blue, domeAlpha);
                }
            }

            // Layer B: Inner Turbulent Corrupted Stratus Ceiling (Counter-Rotating)
            int innerLats = 6;
            int innerLons = 24;
            float innerDomeHeight = domeHeight * 0.75f;
            float innerRadius = radius * 0.85f;

            for (int lat = 0; lat < innerLats; lat++) {
                float p1 = lat / (float) innerLats;
                float p2 = (lat + 1) / (float) innerLats;

                float iy1 = 2.0f + (float) Math.sin(p1 * (Math.PI / 2.0)) * innerDomeHeight;
                float iy2 = 2.0f + (float) Math.sin(p2 * (Math.PI / 2.0)) * innerDomeHeight;

                float ir1 = innerRadius * (float) Math.cos(p1 * (Math.PI / 2.0));
                float ir2 = innerRadius * (float) Math.cos(p2 * (Math.PI / 2.0));

                float alpha = (0.28f + (p1 * 0.22f)) * env;

                // T2 gradient: deep violet at the inner ceiling base rising to
                // the cursed crimson of the spire tips at the crown.
                float inR = 0.65f + (p1 * 0.40f);
                float inG = 0.03f;
                float inB = 0.25f + (p1 * 0.30f);

                for (int lon = 0; lon < innerLons; lon++) {
                    double angleOffset = (-age * (6.0f + lat * 2.5f)) * (Math.PI / 180.0);
                    double a1 = ((lon / (double) innerLons) * Math.PI * 2.0) + angleOffset;
                    double a2 = (((lon + 1) / (double) innerLons) * Math.PI * 2.0) + angleOffset;

                    float x1 = (float) Math.cos(a1) * ir1;
                    float z1 = (float) Math.sin(a1) * ir1;
                    float x2 = (float) Math.cos(a2) * ir1;
                    float z2 = (float) Math.sin(a2) * ir1;

                    float x3 = (float) Math.cos(a2) * ir2;
                    float z3 = (float) Math.sin(a2) * ir2;
                    float x4 = (float) Math.cos(a1) * ir2;
                    float z4 = (float) Math.sin(a1) * ir2;

                    drawQuad(matrix, buffer, x1, iy1, z1, x2, iy1, z2, x3, iy2, z3, x4, iy2, z4, inR, inG, inB, alpha);
                }
            }

            // 3. 16 Orbiting 3D Jagged Obsidian Void Spires (Floating & Tilting around domain perimeter)
            int spireCount = 16;
            float spireOrbitR = radius * 0.92f;
            float spireRot = age * 15.0f;

            for (int i = 0; i < spireCount; i++) {
                double sAngle = (i / (double) spireCount) * Math.PI * 2.0 + Math.toRadians(spireRot);
                float sx = (float) Math.cos(sAngle) * spireOrbitR;
                float sz = (float) Math.sin(sAngle) * spireOrbitR;
                float sy = 1.0f + (float) Math.sin(age * 0.15f + i * 0.8f) * 0.8f;

                float spireHeight = 3.2f + (i % 3 == 0 ? 1.5f : 0.0f);
                float spireWidth = 0.35f;

                // Inward lean vector toward player
                float leanX = -(float) Math.cos(sAngle) * 0.6f;
                float leanZ = -(float) Math.sin(sAngle) * 0.6f;

                // Tangent vector
                float tx = -(float) Math.sin(sAngle) * spireWidth;
                float tz = (float) Math.cos(sAngle) * spireWidth;

                draw3DShardPyramid(matrix, buffer,
                    sx - tx, sy, sz - tz,
                    sx + tx, sy, sz + tz,
                    sx + leanX, sy + spireHeight, sz + leanZ,
                    0.15f, 0.05f, 0.22f, 0.95f * env,
                    0.95f, 0.05f, 0.40f, 1.0f * env
                );
            }

            // 4. Central Abyssal Vortex Funnel Column (Towering vertical cyclonic vortex)
            int eyeLevels = 8;
            int eyeSegments = 16;
            float eyeRadius = Math.max(1.8f, radius * 0.16f);
            for (int lvl = 0; lvl < eyeLevels; lvl++) {
                float ey1 = (lvl / (float) eyeLevels) * domeHeight;
                float ey2 = ((lvl + 1) / (float) eyeLevels) * domeHeight;
                float er1 = eyeRadius * (0.6f + 0.5f * (lvl / (float) eyeLevels));
                float er2 = eyeRadius * (0.6f + 0.5f * ((lvl + 1) / (float) eyeLevels));

                // T2 gradient: violet at the funnel base shifting to the
                // crimson of the domain's lightning at the top.
                float lvlT = lvl / (float) eyeLevels;
                float eyeR = 0.55f + (0.45f * lvlT);
                float eyeG = 0.03f;
                float eyeB = 0.90f - (0.35f * lvlT);

                float rotEye = age * (24.0f + lvl * 4.0f) * (float) (Math.PI / 180.0);
                for (int i = 0; i < eyeSegments; i++) {
                    double a1 = ((i / (double) eyeSegments) * Math.PI * 2.0) + rotEye;
                    double a2 = (((i + 1) / (double) eyeSegments) * Math.PI * 2.0) + rotEye;

                    float x1 = (float) Math.cos(a1) * er1;
                    float z1 = (float) Math.sin(a1) * er1;
                    float x2 = (float) Math.cos(a2) * er1;
                    float z2 = (float) Math.sin(a2) * er1;

                    float x3 = (float) Math.cos(a2) * er2;
                    float z3 = (float) Math.sin(a2) * er2;
                    float x4 = (float) Math.cos(a1) * er2;
                    float z4 = (float) Math.sin(a1) * er2;

                    drawQuad(matrix, buffer, x1, ey1, z1, x2, ey1, z2, x3, ey2, z3, x4, ey2, z4, eyeR, eyeG, eyeB, 0.45f * env);
                }
            }

            drawRotatingRing(matrix, buffer, 0, 1.05f, 0, radius * 0.98f, radius * 0.93f, 48, age * 6.0f, 0.95f, 0.08f, 0.38f, 0.55f * env);
            drawRotatingRing(matrix, buffer, 0, 0.55f, 0, radius * 0.86f, radius * 0.78f, 40, age * 6.0f - 22.0f, 0.55f, 0.04f, 0.85f, 0.22f * env);

            // Bolts born on the cloud shell and ending at a point inside the dome.
            // Each one flashes and is gone; it is not a bar left sitting on the storm.
            float boltCycle = age % 8.0f;
            float boltOn = boltCycle < 3.5f ? (1.0f - boltCycle / 3.5f) : 0.0f;
            float boltFlash = boltOn * boltOn * (3.0f - 2.0f * boltOn) * env;
            if (boltFlash > 0.04f) {
                Random boltRng = new Random(state.seed + ((long) (age / 8.0f) * 1000L));
                for (int a = 0; a < 3; a++) {
                    double ang = boltRng.nextDouble() * Math.PI * 2.0;
                    float lat = 0.22f + boltRng.nextFloat() * 0.48f;
                    float shellR = radius * (float) Math.cos(lat * (Math.PI / 2.0));
                    float yS = 1.0f + (float) Math.sin(lat * (Math.PI / 2.0)) * domeHeight;
                    float xS = (float) Math.cos(ang) * shellR * 0.96f;
                    float zS = (float) Math.sin(ang) * shellR * 0.96f;
                    float yE = 1.2f + boltRng.nextFloat() * Math.min(4.0f, domeHeight * 0.28f);
                    float xE = (float) Math.cos(ang) * shellR * 0.16f + (boltRng.nextFloat() - 0.5f) * 1.2f;
                    float zE = (float) Math.sin(ang) * shellR * 0.16f + (boltRng.nextFloat() - 0.5f) * 1.2f;

                    float cx = xS;
                    float cy = yS;
                    float cz = zS;
                    int steps = 7;
                    for (int s = 0; s < steps; s++) {
                        float u = (s + 1) / (float) steps;
                        float wander = (float) Math.sin(u * Math.PI);
                        float nx = xS + (xE - xS) * u + (boltRng.nextFloat() - 0.5f) * 1.5f * wander;
                        float ny = yS + (yE - yS) * u;
                        float nz = zS + (zE - zS) * u + (boltRng.nextFloat() - 0.5f) * 1.5f * wander;
                        float sheath = 0.10f * (0.30f + 0.70f * wander);
                        drawBoltRibbon(matrix, buffer, cx, cy, cz, nx, ny, nz, sheath, 0.78f, 0.04f, 0.42f, 0.88f * boltFlash);
                        cx = nx;
                        cy = ny;
                        cz = nz;
                    }
                }
            }

            // Shared layered vortex bands (point 1): the same three layer
            // language every spectacle effect speaks, wrapped around the
            // funnel at mid radius, over the standard ground anchor glow.
            com.dragonblockarcanedba.client.render.ki.LayeredRender.drawVortexBands(pose, buffer,
                radius * 0.30f, domeHeight, age, 0.90f, 0.05f, 0.30f, env, ds);
            com.dragonblockarcanedba.client.render.ki.LayeredRender.drawAnchorGlow(pose, buffer,
                radius * 0.55f, 0.06f, 0.85f, 0.04f, 0.25f, 0.35f * env, ds);
        });

        // T1 plus T6: one additive pass holding a halo shell around the
        // vortex column and a soft ground glow disc, so the domain blooms at
        // its core and sits anchored in the scene.
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            float glowDomeHeight = Math.min(24.0f, radius * 1.30f);
            float glowEyeRadius = Math.max(1.8f, radius * 0.16f) * 1.45f;
            int glowLevels = 8;
            int glowSegments = 16;

            for (int lvl = 0; lvl < glowLevels; lvl++) {
                float ey1 = (lvl / (float) glowLevels) * glowDomeHeight;
                float ey2 = ((lvl + 1) / (float) glowLevels) * glowDomeHeight;
                float er1 = glowEyeRadius * (0.6f + 0.5f * (lvl / (float) glowLevels));
                float er2 = glowEyeRadius * (0.6f + 0.5f * ((lvl + 1) / (float) glowLevels));

                float rotEye = age * (24.0f + lvl * 4.0f) * (float) (Math.PI / 180.0);
                for (int i = 0; i < glowSegments; i++) {
                    double a1 = ((i / (double) glowSegments) * Math.PI * 2.0) + rotEye;
                    double a2 = (((i + 1) / (double) glowSegments) * Math.PI * 2.0) + rotEye;

                    float x1 = (float) Math.cos(a1) * er1;
                    float z1 = (float) Math.sin(a1) * er1;
                    float x2 = (float) Math.cos(a2) * er1;
                    float z2 = (float) Math.sin(a2) * er1;

                    float x3 = (float) Math.cos(a2) * er2;
                    float z3 = (float) Math.sin(a2) * er2;
                    float x4 = (float) Math.cos(a1) * er2;
                    float z4 = (float) Math.sin(a1) * er2;

                    drawQuad(matrix, buffer, x1, ey1, z1, x2, ey1, z2, x3, ey2, z3, x4, ey2, z4, 0.50f, 0.03f, 0.55f, 0.15f * env);
                }
            }

            drawRotatingRing(matrix, buffer, 0, 0.06f, 0, radius * 0.50f, 0.0f, 48, age * 10.0f, 0.45f, 0.02f, 0.30f, 0.10f * env);
        });
    }

    private static void drawRotatingRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float rotDeg, float r, float g, float b, float a) {
        double rotRad = rotDeg * (Math.PI / 180.0);
        for (int i = 0; i < segments; i++) {
            double a1 = ((i / (double) segments) * Math.PI * 2.0) + rotRad;
            double a2 = (((i + 1) / (double) segments) * Math.PI * 2.0) + rotRad;

            float x1 = cx + (float) Math.cos(a1) * rOuter;
            float z1 = cz + (float) Math.sin(a1) * rOuter;
            float x2 = cx + (float) Math.cos(a2) * rOuter;
            float z2 = cz + (float) Math.sin(a2) * rOuter;

            float ix1 = cx + (float) Math.cos(a1) * rInner;
            float iz1 = cz + (float) Math.sin(a1) * rInner;
            float ix2 = cx + (float) Math.cos(a2) * rInner;
            float iz2 = cz + (float) Math.sin(a2) * rInner;

            drawQuad(matrix, consumer, ix1, cy, iz1, ix2, cy, iz2, x2, cy, z2, x1, cy, z1, r, g, b, a);
        }
    }

    private static void draw3DShardPyramid(Matrix4f matrix, VertexConsumer consumer,
                                          float x1, float y1, float z1,
                                          float x2, float y2, float z2,
                                          float tipX, float tipY, float tipZ,
                                          float rBase, float gBase, float bBase, float aBase,
                                          float rTip, float gTip, float bTip, float aTip) {
        // Front Face
        consumer.addVertex(matrix, x1, y1, z1).setColor(rBase, gBase, bBase, aBase).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(rBase, gBase, bBase, aBase).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Back Face
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(rBase, gBase, bBase, aBase).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(rBase, gBase, bBase, aBase).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }

    /** A bowed bolt kink with a sheath, a white core, and a wake. The jagged path stays. */
    private static void drawBoltRibbon(Matrix4f matrix, VertexConsumer consumer,
                                       float x1, float y1, float z1, float x2, float y2, float z2,
                                       float radius, float r, float g, float b, float a) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001f || a < 0.02f) {
            return;
        }
        float px = -dz / len;
        float pz = dx / len;
        float bow = Math.min(radius * 3.0f, len * 0.35f);
        float mx = (x1 + x2) * 0.5f + px * bow;
        float my = (y1 + y2) * 0.5f + bow * 0.4f;
        float mz = (z1 + z2) * 0.5f + pz * bow;
        boltHalf(matrix, consumer, x1, y1, z1, mx, my, mz, radius, radius * 0.6f, r, g, b, a, a * 0.85f);
        boltHalf(matrix, consumer, mx, my, mz, x2, y2, z2, radius * 0.6f, radius * 0.25f, r, g, b, a * 0.85f, a * 0.5f);
        boltHalf(matrix, consumer, x1, y1, z1, mx, my, mz, radius * 0.26f, radius * 0.14f, 1.0f, 0.92f, 0.96f, a, a);
        boltHalf(matrix, consumer, mx, my, mz, x2, y2, z2, radius * 0.14f, radius * 0.05f, 1.0f, 0.92f, 0.96f, a, a * 0.65f);
        boltHalf(matrix, consumer, mx, my, mz, mx - px * bow * 1.4f, my - bow * 0.25f, mz - pz * bow * 1.4f, radius * 0.4f, 0.01f, r, g, b, a * 0.4f, 0.0f);
    }

    private static void boltHalf(Matrix4f matrix, VertexConsumer consumer,
                                 float x1, float y1, float z1, float x2, float y2, float z2,
                                 float w1, float w2, float r, float g, float b, float a1, float a2) {
        float dx = x2 - x1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + (y2 - y1) * (y2 - y1) + dz * dz);
        if (len < 0.001f) {
            return;
        }
        float nx = -dz / len;
        float nz = dx / len;
        consumer.addVertex(matrix, x1 - nx * w1, y1, z1 - nz * w1).setColor(r, g, b, a1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1 + nx * w1, y1, z1 + nz * w1).setColor(r, g, b, a1).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 + nx * w2, y2, z2 + nz * w2).setColor(r, g, b, a2).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 - nx * w2, y2, z2 - nz * w2).setColor(r, g, b, a2).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y1 - w1, z1).setColor(r, g, b, a1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, x1, y1 + w1, z1).setColor(r, g, b, a1).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, x2, y2 + w2, z2).setColor(r, g, b, a2).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
        consumer.addVertex(matrix, x2, y2 - w2, z2).setColor(r, g, b, a2).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(1, 0, 0);
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
