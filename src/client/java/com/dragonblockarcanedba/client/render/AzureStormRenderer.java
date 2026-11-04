package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.AzureStormEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
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
 * Entity Renderer for Azure Storm Domain in Minecraft 26.2.
 * Renders a massive, cinematic 3D tempest weather domain:
 * - Double-layered rotating volumetric cyclone cloud dome with turbulence harmonics
 * - Physical 3D downburst columns and geometric rain shafts
 * - Quad-tier counter-rotating ground whirlpools and surging wave rings
 * - Volumetric branching atmospheric lightning discharges across the dome
 * - Central cyclonic tempest eye and radiant updraft column
 */
public class AzureStormRenderer extends EntityRenderer<AzureStormEntity, AzureStormRenderer.AzureStormRenderState> {

    public AzureStormRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class AzureStormRenderState extends EntityRenderState {
        public float radius = 12.0f;
        public float age = 0;
        public long seed = 0;
        public int lifeTicks = 220;
        public double wx, wy, wz;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(AzureStormEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(AzureStormEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public AzureStormRenderState createRenderState() {
        return new AzureStormRenderState();
    }

    @Override
    public void extractRenderState(AzureStormEntity entity, AzureStormRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
        state.lifeTicks = entity.getLifeTicks();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    @Override
    public void submit(AzureStormRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float radius = state.radius;
        float age = state.age;
        RenderType renderType = KiRenderHelper.kiRenderType();

        // Distance scale: far storms step their segment counts down.
        float ds = LayeredRender.distScale(cameraState, state.wx, state.wy, state.wz);

        // Lifetime fade envelope from the shared FxEnvelope: in over the first
        // 20 ticks, out over the last 40, of whatever life this storm was
        // summoned with. The old constants assumed a fixed 220 tick life.
        float life = Math.max(20.0f, state.lifeTicks);
        float buildFrac = Math.min(0.45f, 20.0f / life);
        float decayFrac = Math.min(0.45f, 40.0f / life);
        float fade = FxEnvelope.envelope(age, life, buildFrac, decayFrac);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // Ground anchor glow: a soft disc grounding the dome to the world.
            LayeredRender.drawAnchorGlow(pose, buffer, radius * 0.92f, 0.07f,
                0.20f, 0.75f, 1.0f, 0.35f * fade, ds);

            // 1. Quad-Tier Ground Whirlpool & Wave Surges (4 counter-rotating wind bands)
            int groundSegments = FxEnvelope.segments(48, ds, 16);
            for (int band = 0; band < 4; band++) {
                float rOuter = radius * (0.30f + band * 0.23f);
                float rInner = rOuter * 0.84f;
                float rotSpeed = (band % 2 == 0 ? 1.0f : -1.35f) * (14.0f + band * 7.0f);
                float bandAlpha = (0.58f - (band * 0.09f)) * fade;

                float red = band == 0 ? 0.4f : 0.0f;
                float green = 0.75f + (band * 0.08f);
                float blue = 1.0f;

                drawRotatingRing(matrix, buffer, 0, 0.08f + (band * 0.06f), 0, rOuter, rInner, groundSegments, age * rotSpeed, red, green, blue, bandAlpha);
            }

            // 2. Volumetric Double-Layered Cyclone Cloud Dome
            float domeHeight = Math.min(22.0f, radius * 1.35f);

            // Layer A: Outer Massive Rotating Cyclone Dome
            int domeLatitudes = 12;
            int domeLongitudes = FxEnvelope.segments(36, ds, 12);
            for (int lat = 0; lat < domeLatitudes; lat++) {
                float latProgress1 = lat / (float) domeLatitudes;
                float latProgress2 = (lat + 1) / (float) domeLatitudes;

                float y1 = 2.0f + (float) Math.sin(latProgress1 * (Math.PI / 2.0)) * domeHeight;
                float y2 = 2.0f + (float) Math.sin(latProgress2 * (Math.PI / 2.0)) * domeHeight;

                float r1 = radius * (float) Math.cos(latProgress1 * (Math.PI / 2.0));
                float r2 = radius * (float) Math.cos(latProgress2 * (Math.PI / 2.0));

                // Alpha is densest low on the dome and thins toward the rim,
                // a heavy base with wispy edges instead of brightening upward.
                float domeAlpha = (0.42f - (latProgress1 * 0.20f)) * fade;
                float red = 0.02f;
                float green = 0.22f + (latProgress1 * 0.55f);
                float blue = 0.55f + (latProgress1 * 0.45f);

                for (int lon = 0; lon < domeLongitudes; lon++) {
                    double angleOffset = (age * (5.0f + lat * 2.0f)) * (Math.PI / 180.0);
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

            // Layer B: Inner Core Dome. The class header has always promised a
            // double layered dome and only Layer A existed; this second shell
            // sits at 0.8 of the radius and 0.85 of the height, brighter, and
            // counter rotates against the outer dome.
            int innerLatitudes = 10;
            int innerLongitudes = domeLongitudes;
            float innerHeight = domeHeight * 0.85f;
            for (int lat = 0; lat < innerLatitudes; lat++) {
                float lp1 = lat / (float) innerLatitudes;
                float lp2 = (lat + 1) / (float) innerLatitudes;

                float iy1 = 2.0f + (float) Math.sin(lp1 * (Math.PI / 2.0)) * innerHeight;
                float iy2 = 2.0f + (float) Math.sin(lp2 * (Math.PI / 2.0)) * innerHeight;

                float ir1 = radius * 0.8f * (float) Math.cos(lp1 * (Math.PI / 2.0));
                float ir2 = radius * 0.8f * (float) Math.cos(lp2 * (Math.PI / 2.0));

                float innerAlpha = (0.30f - (lp1 * 0.12f)) * fade;
                float iRed = 0.10f;
                float iGreen = 0.45f + (lp1 * 0.40f);
                float iBlue = 1.0f;

                for (int lon = 0; lon < innerLongitudes; lon++) {
                    double angleOffset = -(age * (6.0f + lat * 2.0f)) * (Math.PI / 180.0);
                    double a1 = ((lon / (double) innerLongitudes) * Math.PI * 2.0) + angleOffset;
                    double a2 = (((lon + 1) / (double) innerLongitudes) * Math.PI * 2.0) + angleOffset;

                    float x1 = (float) Math.cos(a1) * ir1;
                    float z1 = (float) Math.sin(a1) * ir1;
                    float x2 = (float) Math.cos(a2) * ir1;
                    float z2 = (float) Math.sin(a2) * ir1;

                    float x3 = (float) Math.cos(a2) * ir2;
                    float z3 = (float) Math.sin(a2) * ir2;
                    float x4 = (float) Math.cos(a1) * ir2;
                    float z4 = (float) Math.sin(a1) * ir2;

                    drawQuad(matrix, buffer, x1, iy1, z1, x2, iy1, z2, x3, iy2, z3, x4, iy2, z4, iRed, iGreen, iBlue, innerAlpha);
                }
            }

            // 3. Physical Geometric Rain Shafts & Downburst Streamlines
            Random rainRng = new Random(state.seed + 777);
            int shaftCount = Math.max(6, Math.round(14 * ds));
            for (int i = 0; i < shaftCount; i++) {
                double shaftAngle = rainRng.nextDouble() * Math.PI * 2.0 + (age * 0.05);
                float shaftR = radius * (0.25f + rainRng.nextFloat() * 0.65f);

                float sx = (float) Math.cos(shaftAngle) * shaftR;
                float sz = (float) Math.sin(shaftAngle) * shaftR;

                float topY = 4.0f + rainRng.nextFloat() * (domeHeight * 0.7f);
                float botY = 0.1f;

                // Rain eases down the shaft and thins out as it meets the ground.
                float fallT = ((age * 0.8f + i * 1.5f) % 4.0f) / 4.0f;
                float fallEase = fallT * fallT;
                float streakTop = Math.max(botY, topY - fallEase * 12.0f);
                float streakBot = Math.max(botY, streakTop - 3.5f);
                float groundFade = Math.min(1.0f, (streakBot - botY) / 1.4f + 0.2f);

                renderBeam(matrix, buffer, sx, streakTop, sz, sx + 0.2f, streakBot, sz + 0.2f, 0.08f, 0.5f, 0.9f, 1.0f, 0.60f * fade * groundFade);
            }

            // 4. Central Tempest Eye Updraft Column (Intense central cyclone core)
            int eyeLevels = 6;
            int eyeSegments = 16;
            float eyeRadius = radius * 0.18f;
            for (int lvl = 0; lvl < eyeLevels; lvl++) {
                float ey1 = (lvl / (float) eyeLevels) * domeHeight;
                float ey2 = ((lvl + 1) / (float) eyeLevels) * domeHeight;
                float er1 = eyeRadius * (0.6f + 0.4f * (lvl / (float) eyeLevels));
                float er2 = eyeRadius * (0.6f + 0.4f * ((lvl + 1) / (float) eyeLevels));

                // T2 gradient: near-white at the eye base cooling to a deeper
                // storm cyan as the updraft climbs.
                float lvlT = lvl / (float) eyeLevels;
                float eyeR = 0.85f - (0.60f * lvlT);
                float eyeG = 1.0f - (0.20f * lvlT);
                float eyeB = 1.0f;

                float rotEye = age * (30.0f + lvl * 5.0f) * (float) (Math.PI / 180.0);
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

                    drawQuad(matrix, buffer, x1, ey1, z1, x2, ey1, z2, x3, ey2, z3, x4, ey2, z4, eyeR, eyeG, eyeB, 0.45f * fade);
                }
            }

            // 5. Volumetric Atmospheric Lightning Discharges within Storm Boundary
            Random rng = new Random(state.seed + ((long) (age / 3.0f) * 1000));
            int arcCount = Math.max(2, Math.round(4 * ds));
            for (int a = 0; a < arcCount; a++) {
                double startAngle = rng.nextDouble() * Math.PI * 2.0;
                float arcR = radius * (0.25f + rng.nextFloat() * 0.65f);
                float sx = (float) Math.cos(startAngle) * arcR;
                float sz = (float) Math.sin(startAngle) * arcR;
                float sy = 7.0f + rng.nextFloat() * 9.0f;

                float curX = sx, curY = sy, curZ = sz;
                int arcSteps = 6;
                for (int s = 0; s < arcSteps; s++) {
                    float nxtX = curX + (rng.nextFloat() - 0.5f) * 3.5f;
                    float nxtY = curY - (1.1f + rng.nextFloat() * 1.8f);
                    float nxtZ = curZ + (rng.nextFloat() - 0.5f) * 3.5f;

                    // One bolt: cyan sheath and the white core the lightning texture uses.
                    renderBeam(matrix, buffer, curX, curY, curZ, nxtX, nxtY, nxtZ, 0.16f, 0.0f, 0.85f, 1.0f, 0.75f * fade);
                    renderBeam(matrix, buffer, curX, curY, curZ, nxtX, nxtY, nxtZ, 0.05f, 0.95f, 1.0f, 1.0f, 0.95f * fade);
                    if (s == 2) {
                        float forkX = nxtX + (rng.nextFloat() - 0.5f) * 2.4f;
                        float forkY = nxtY - (0.8f + rng.nextFloat());
                        float forkZ = nxtZ + (rng.nextFloat() - 0.5f) * 2.4f;
                        renderBeam(matrix, buffer, nxtX, nxtY, nxtZ, forkX, forkY, forkZ, 0.07f, 0.15f, 0.9f, 1.0f, 0.55f * fade);
                    }

                    curX = nxtX; curY = nxtY; curZ = nxtZ;
                }
            }
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
