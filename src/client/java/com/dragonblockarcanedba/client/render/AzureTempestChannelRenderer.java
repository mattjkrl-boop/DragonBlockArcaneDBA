package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.AzureTempestChannelEntity;
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

import java.util.Random;

/**
 * Entity Renderer for Call of the Tempest Channeling in Minecraft 26.2.
 * Renders an expanding physical 3D geometric wind tunnel, orbital swirling wind-blades, storm vortex base rings, and crackling lightning arcs.
 */
public class AzureTempestChannelRenderer extends EntityRenderer<AzureTempestChannelEntity, AzureTempestChannelRenderer.ChannelRenderState> {

    public AzureTempestChannelRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ChannelRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public float age = 0;
        public long seed = 0;
        public boolean isFirstPersonOwner = false;
    }

    @Override
    public boolean shouldRender(AzureTempestChannelEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public ChannelRenderState createRenderState() {
        return new ChannelRenderState();
    }

    @Override
    public void extractRenderState(AzureTempestChannelEntity entity, ChannelRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null && 
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) && 
            mc.options.getCameraType().isFirstPerson());
    }

    @Override
    public void submit(ChannelRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        float charge = state.chargeRatio;
        float age = state.age;

        float tunnelRadius = (state.isFirstPersonOwner ? 2.4f : 1.6f) + (charge * 2.2f);
        float tunnelHeight = 3.2f + (charge * 2.4f);
        float baseAlpha = (state.isFirstPersonOwner ? 0.35f : 0.55f) + (charge * 0.35f);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Planar Runic Storm Vortex Base Array (3 counter-rotating ground rings)
            int groundSegments = 24;
            for (int b = 0; b < 3; b++) {
                float rOut = (tunnelRadius * 0.7f) * (0.6f + b * 0.35f);
                float rIn = rOut * 0.82f;
                float rotSpeed = (b % 2 == 0 ? 1.0f : -1.4f) * (20.0f + b * 12.0f);
                float bandAlpha = baseAlpha * (0.8f - b * 0.15f);

                drawRotatingRing(matrix, buffer, 0, 0.05f + (b * 0.05f), 0, rOut, rIn, groundSegments, age * rotSpeed,
                    0.0f, 0.85f + (b * 0.07f), 1.0f, bandAlpha);
            }

            // 2. Expanding Geometric Cyclonic Wind Tunnel (Faceted vertical vortex column)
            int levels = 8;
            int segments = 20;
            for (int lvl = 0; lvl < levels; lvl++) {
                float y1 = (lvl / (float) levels) * tunnelHeight;
                float y2 = ((lvl + 1) / (float) levels) * tunnelHeight;

                float r1 = tunnelRadius * (0.8f + 0.3f * (float) Math.sin((lvl / (float) levels) * Math.PI));
                float r2 = tunnelRadius * (0.8f + 0.3f * (float) Math.sin(((lvl + 1) / (float) levels) * Math.PI));

                float rotLvl = age * (18.0f + lvl * 4.0f) * (float) (Math.PI / 180.0);
                float lvlAlpha = (baseAlpha * 0.40f) * (1.0f - (lvl / (float) levels) * 0.4f);

                for (int i = 0; i < segments; i++) {
                    double a1 = ((i / (double) segments) * Math.PI * 2.0) + rotLvl;
                    double a2 = (((i + 1) / (double) segments) * Math.PI * 2.0) + rotLvl;

                    float x1 = (float) Math.cos(a1) * r1;
                    float z1 = (float) Math.sin(a1) * r1;
                    float x2 = (float) Math.cos(a2) * r1;
                    float z2 = (float) Math.sin(a2) * r1;

                    float x3 = (float) Math.cos(a2) * r2;
                    float z3 = (float) Math.sin(a2) * r2;
                    float x4 = (float) Math.cos(a1) * r2;
                    float z4 = (float) Math.sin(a1) * r2;

                    drawQuad(matrix, buffer, x1, y1, z1, x2, y1, z2, x3, y2, z3, x4, y2, z4, 0.0f, 0.90f, 1.0f, lvlAlpha);
                }
            }

            // 3. Orbital Swirling 3D Wind Blades (Razor-sharp curved wind cutters orbiting player)
            int bladeCount = 4 + (int) (charge * 4); // 4 to 8 blades
            float bladeOrbitR = tunnelRadius * 0.92f;
            float orbitSpeed = 25.0f + (charge * 60.0f);

            for (int i = 0; i < bladeCount; i++) {
                double bladeAngle = (i / (double) bladeCount) * Math.PI * 2.0 + (age * orbitSpeed * (Math.PI / 180.0));
                float bladeY = 0.8f + (float) Math.sin(age * 0.2f + i) * 0.4f + (i % 2 == 0 ? 0.3f : -0.2f);

                float bx = (float) Math.cos(bladeAngle) * bladeOrbitR;
                float bz = (float) Math.sin(bladeAngle) * bladeOrbitR;

                // Tangent vector
                float tx = (float) -Math.sin(bladeAngle);
                float tz = (float) Math.cos(bladeAngle);

                // Blade span: front tip to trailing edge
                float bladeLength = 0.9f + (charge * 0.5f);
                float bladeWidth = 0.22f;

                float tipX = bx + tx * (bladeLength * 0.6f);
                float tipZ = bz + tz * (bladeLength * 0.6f);
                float tailX = bx - tx * (bladeLength * 0.4f);
                float tailZ = bz - tz * (bladeLength * 0.4f);

                float normX = -tz * bladeWidth;
                float normZ = tx * bladeWidth;

                // 3D Crescent Wing
                drawTriangle(matrix, buffer,
                    tailX - normX, bladeY, tailZ - normZ,
                    tailX + normX, bladeY, tailZ + normZ,
                    tipX, bladeY + 0.1f, tipZ,
                    0.2f, 1.0f, 1.0f, baseAlpha * 0.95f
                );
            }

            // 4. Physical 3D Electric Storm Arcs (Intensifies with charge)
            if (charge >= 0.25f) {
                Random rng = new Random(state.seed + ((long) (age * 2.0f) * 1000));
                int arcCount = 2 + (int) (charge * 4);

                for (int a = 0; a < arcCount; a++) {
                    double arcAngle = rng.nextDouble() * Math.PI * 2.0;
                    float arcR = tunnelRadius * (0.6f + rng.nextFloat() * 0.5f);
                    float sx = (float) Math.cos(arcAngle) * arcR;
                    float sz = (float) Math.sin(arcAngle) * arcR;
                    float sy = 0.2f + rng.nextFloat() * tunnelHeight;

                    float cx = sx, cy = sy, cz = sz;
                    int steps = 4;
                    for (int s = 0; s < steps; s++) {
                        float nx = cx + (rng.nextFloat() - 0.5f) * 1.2f;
                        float ny = cy + (rng.nextFloat() - 0.5f) * 1.0f;
                        float nz = cz + (rng.nextFloat() - 0.5f) * 1.2f;

                        renderBeam(matrix, buffer, cx, cy, cz, nx, ny, nz, 0.08f, 0.0f, 0.95f, 1.0f, baseAlpha);
                        renderBeam(matrix, buffer, cx, cy, cz, nx, ny, nz, 0.03f, 1.0f, 1.0f, 1.0f, 1.0f);

                        cx = nx; cy = ny; cz = nz;
                    }
                }
            }
        });

        super.submit(state, poseStack, collector, cameraState);
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

    private static void drawTriangle(Matrix4f matrix, VertexConsumer consumer,
                                     float x1, float y1, float z1,
                                     float x2, float y2, float z2,
                                     float x3, float y3, float z3,
                                     float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Reverse
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
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
