package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.ValorFieldEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity Renderer for Grand Sword's Valor Field in Minecraft 26.2.
 * Renders a monumental, high-quality translucent 3D protective golden dome:
 * - Volumetric 3D translucent geodesic dome / sphere shell with dynamic breathing alpha
 * - Multi-tier ground sanctum mandala with outer & inner counter-rotating runic rings and 8 cardinal ray steles
 * - Equatorial & dual tilted orbital precession runic rings
 * - 8 dynamic vertical meridian energy ribs connecting ground seal to zenith apex cap
 * - 3D glowing octahedral golden stasis containment cages for suspended projectiles
 */
public class ValorFieldRenderer extends EntityRenderer<ValorFieldEntity, ValorFieldRenderer.ValorFieldRenderState> {

    public ValorFieldRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ValorFieldRenderState extends EntityRenderState {
        public float radius = 9.0f;
        public float ageInTicks = 0;
        public float fadeStart = -1;
        public final List<Vec3> suspendedRelPositions = new ArrayList<>();
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(ValorFieldEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(ValorFieldEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ValorFieldRenderState createRenderState() {
        return new ValorFieldRenderState();
    }

    @Override
    public void extractRenderState(ValorFieldEntity entity, ValorFieldRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = ValorFieldEntity.FIELD_RADIUS;
        state.ageInTicks = entity.tickCount + partialTicks;
        state.fadeStart = entity.getFadeStart();

        state.suspendedRelPositions.clear();
        // Cages only around projectiles the field actually suspended: the ids
        // come from the entity's own suspended map, synced to the client.
        // This used to cage any projectile moving slowly inside the radius,
        // including ones the field never touched.
        java.util.Set<Integer> suspendedIds = entity.getSuspendedIds();
        if (suspendedIds.isEmpty()) return;
        Vec3 fieldPos = entity.position();
        float r = state.radius;
        AABB box = new AABB(
            fieldPos.x - r, fieldPos.y - r, fieldPos.z - r,
            fieldPos.x + r, fieldPos.y + r, fieldPos.z + r
        );

        List<Projectile> projectiles = entity.level().getEntitiesOfClass(
            Projectile.class, box,
            p -> p.isAlive() && p != entity && suspendedIds.contains(p.getId())
                && p.distanceToSqr(fieldPos) <= r * r
        );

        for (Projectile p : projectiles) {
            Vec3 pPos = p.position();
            state.suspendedRelPositions.add(pPos.subtract(fieldPos));
        }
    }

    @Override
    public void submit(ValorFieldRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        float radius = state.radius;
        float age = state.ageInTicks;
        // Spawn fade-in over the first 10 ticks: the 9 block dome used to appear
        // at full strength in a single tick when cast.
        float spawnIn = Math.min(1.0f, age / 10.0f);
        float rise = smooth(spawnIn);
        // Fade out over FADE_OUT_TICKS once the entity starts dissipating, so
        // the dome leaves the same way it arrived instead of vanishing in one
        // tick on release, Ki exhaustion, death, or disconnect.
        float fadeOut = 1.0f;
        if (state.fadeStart >= 0) {
            fadeOut = 1.0f - smooth(Math.min(1.0f,
                (age - state.fadeStart) / (float) ValorFieldEntity.FADE_OUT_TICKS));
        }
        float pulse = (0.88f + 0.12f * (float) Math.sin(age * 0.18f)) * rise * fadeOut;
        float distScale = com.dragonblockarcanedba.client.render.ki.LayeredRender.distScale(
            cameraState, state.x, state.y, state.z);

        poseStack.pushPose();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 0. Ground anchor glow (shared LayeredRender block): a soft gold
            // disc grounding the dome to the world at its base.
            com.dragonblockarcanedba.client.render.ki.LayeredRender.drawAnchorGlow(pose, buffer,
                radius * 0.95f, 0.06f, 1.0f, 0.85f, 0.20f, 0.35f * pulse, distScale);

            // 1. Volumetric 3D Translucent Golden Dome Shell (Multi-Latitude Mesh)
            int domeLatitudes = 14;
            int domeLongitudes = 36;
            float domeHeight = radius * 1.05f * (0.22f + 0.78f * rise);

            for (int lat = 0; lat < domeLatitudes; lat++) {
                float latProgress1 = lat / (float) domeLatitudes;
                float latProgress2 = (lat + 1) / (float) domeLatitudes;

                float y1 = 0.1f + (float) Math.sin(latProgress1 * (Math.PI / 2.0)) * domeHeight;
                float y2 = 0.1f + (float) Math.sin(latProgress2 * (Math.PI / 2.0)) * domeHeight;

                float r1 = radius * (float) Math.cos(latProgress1 * (Math.PI / 2.0));
                float r2 = radius * (float) Math.cos(latProgress2 * (Math.PI / 2.0));

                float domeAlpha = (0.16f + latProgress1 * 0.18f) * pulse;

                // Three-stop gradient: deep gold at the base, the main valor
                // gold mid shell, near-white gold toward the zenith.
                float bandR, bandG, bandB;
                if (latProgress1 < 0.5f) {
                    float k = latProgress1 * 2.0f;
                    bandR = 0.85f + (1.0f - 0.85f) * k;
                    bandG = 0.60f + (0.88f - 0.60f) * k;
                    bandB = 0.10f + (0.25f - 0.10f) * k;
                } else {
                    float k = (latProgress1 - 0.5f) * 2.0f;
                    bandR = 1.0f;
                    bandG = 0.88f + (0.97f - 0.88f) * k;
                    bandB = 0.25f + (0.70f - 0.25f) * k;
                }

                for (int lon = 0; lon < domeLongitudes; lon++) {
                    double angleOffset = (age * (1.2f + lat * 0.3f)) * (Math.PI / 180.0);
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

                    drawQuad(matrix, buffer, x1, y1, z1, x2, y1, z2, x3, y2, z3, x4, y2, z4,
                        bandR, bandG, bandB, domeAlpha);
                }
            }

            drawRotatingRing(matrix, buffer, 0, 0.05f, 0, radius, radius * 0.90f, 48, age * 6.0f,
                1.0f, 0.85f, 0.20f, 0.75f * pulse);
            drawRotatingRing(matrix, buffer, 0, 0.08f, 0, radius * 0.62f, radius * 0.50f, 40, -age * 12.0f,
                1.0f, 0.95f, 0.40f, 0.82f * pulse);
            drawTiltedOrbitalRing(matrix, buffer, radius * 0.96f, 35.0f, age * 18.0f,
                1.0f, 0.90f, 0.30f, 0.68f * pulse);
            drawTiltedOrbitalRing(matrix, buffer, radius * 0.96f, -35.0f, -age * 18.0f,
                1.0f, 0.95f, 0.40f, 0.68f * pulse);
            for (int ray = 0; ray < 8; ray++) {
                double rayAngle = (ray / 8.0) * Math.PI * 2.0 + Math.toRadians(age * 8.0f);
                float rx = (float) Math.cos(rayAngle) * radius;
                float rz = (float) Math.sin(rayAngle) * radius;
                drawTrenchSegment(matrix, buffer, 0, 0, rx, rz, 0.07f, 1.0f, 0.90f, 0.30f, 0.62f * pulse);
            }
            float lipY = 0.1f + domeHeight * 0.45f;
            drawRing3D(matrix, buffer, 0, lipY, 0, radius * 0.90f, radius * 0.84f, 32,
                1.0f, 0.96f, 0.62f, 0.8f * pulse);

            // 4. 8 Dynamic Vertical Meridian Energy Ribs
            for (int rib = 0; rib < 8; rib++) {
                double ribAngle = (rib / 8.0) * Math.PI * 2.0 + Math.toRadians(age * 5.0f);
                drawMeridianRib(matrix, buffer, ribAngle, radius, domeHeight, 18,
                    1.0f, 0.92f, 0.35f, 0.85f * pulse);
            }

            // 5. 3D Glowing Octahedral Stasis Containment Cages around Suspended Projectiles
            for (Vec3 relPos : state.suspendedRelPositions) {
                float sx = (float) relPos.x;
                float sy = (float) relPos.y;
                float sz = (float) relPos.z;
                drawStasisCage(matrix, buffer, sx, sy, sz, 0.65f, age * 35.0f,
                    1.0f, 0.88f, 0.20f, 0.95f * pulse);
            }
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float smooth(float t) {
        float x = Math.max(0.0f, Math.min(1.0f, t));
        return x * x * (3.0f - 2.0f * x);
    }

    private static void drawQuad(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Reverse side for double-sided visibility inside and outside the dome
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }

    private static void drawRotatingRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float rotDeg, float r, float g, float b, float a) {
        float rotRad = (float) Math.toRadians(rotDeg);
        for (int i = 0; i < segments; i++) {
            double a1 = ((i / (double) segments) * Math.PI * 2) + rotRad;
            double a2 = (((i + 1) / (double) segments) * Math.PI * 2) + rotRad;

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

    private static void drawRing3D(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float r, float g, float b, float a) {
        drawRotatingRing(matrix, consumer, cx, cy, cz, rOuter, rInner, segments, 0, r, g, b, a);
    }

    private static void drawTiltedOrbitalRing(Matrix4f matrix, VertexConsumer consumer, float radius, float tiltDeg, float rotDeg, float r, float g, float b, float a) {
        int segments = 36;
        float width = 0.14f;
        double tiltRad = Math.toRadians(tiltDeg);
        double rotRad = Math.toRadians(rotDeg);

        for (int i = 0; i < segments; i++) {
            double a1 = ((i / (double) segments) * Math.PI * 2) + rotRad;
            double a2 = (((i + 1) / (double) segments) * Math.PI * 2) + rotRad;

            float x1_flat = (float) Math.cos(a1) * radius;
            float z1_flat = (float) Math.sin(a1) * radius;
            float x2_flat = (float) Math.cos(a2) * radius;
            float z2_flat = (float) Math.sin(a2) * radius;

            // Apply tilt rotation around X axis
            float x1 = x1_flat;
            float y1 = 1.0f + (float) (-z1_flat * Math.sin(tiltRad));
            float z1 = (float) (z1_flat * Math.cos(tiltRad));

            float x2 = x2_flat;
            float y2 = 1.0f + (float) (-z2_flat * Math.sin(tiltRad));
            float z2 = (float) (z2_flat * Math.cos(tiltRad));

            consumer.addVertex(matrix, x1, y1 + width, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, y2 + width, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, y2 - width, z2).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1, y1 - width, z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawMeridianRib(Matrix4f matrix, VertexConsumer consumer, double angle, float radius, float height, int steps, float r, float g, float b, float a) {
        float ribWidth = 0.12f;
        float sinA = (float) Math.sin(angle);
        float cosA = (float) Math.cos(angle);

        float normX = -sinA * ribWidth * 0.5f;
        float normZ = cosA * ribWidth * 0.5f;

        for (int i = 0; i < steps; i++) {
            float p1 = i / (float) steps;
            float p2 = (i + 1) / (float) steps;

            float y1 = 0.1f + (float) Math.sin(p1 * (Math.PI / 2.0)) * height;
            float y2 = 0.1f + (float) Math.sin(p2 * (Math.PI / 2.0)) * height;

            float r1 = radius * (float) Math.cos(p1 * (Math.PI / 2.0));
            float r2 = radius * (float) Math.cos(p2 * (Math.PI / 2.0));

            float x1 = cosA * r1;
            float z1 = sinA * r1;
            float x2 = cosA * r2;
            float z2 = sinA * r2;

            consumer.addVertex(matrix, x1 - normX, y1, z1 - normZ).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1 + normX, y1, z1 + normZ).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2 + normX, y2, z2 + normZ).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2 - normX, y2, z2 - normZ).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawStasisCage(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float size, float rotDeg, float r, float g, float b, float a) {
        float rotRad = (float) Math.toRadians(rotDeg);
        float h = size * 1.2f;
        float w = size * 0.7f;

        // 4 Upper Octahedral Faces
        for (int i = 0; i < 4; i++) {
            double a1 = (i / 4.0) * Math.PI * 2.0 + rotRad;
            double a2 = ((i + 1) / 4.0) * Math.PI * 2.0 + rotRad;

            float x1 = cx + (float) Math.cos(a1) * w;
            float z1 = cz + (float) Math.sin(a1) * w;
            float x2 = cx + (float) Math.cos(a2) * w;
            float z2 = cz + (float) Math.sin(a2) * w;

            // Top pyramid
            consumer.addVertex(matrix, x1, cy, z1).setColor(r, g, b, a * 0.75f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, cy, z2).setColor(r, g, b, a * 0.75f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx, cy + h, cz).setColor(1.0f, 1.0f, 0.9f, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, cx, cy + h, cz).setColor(1.0f, 1.0f, 0.9f, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Bottom pyramid
            consumer.addVertex(matrix, x2, cy, z2).setColor(r, g, b, a * 0.75f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, x1, cy, z1).setColor(r, g, b, a * 0.75f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, cx, cy - h, cz).setColor(1.0f, 1.0f, 0.9f, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, cx, cy - h, cz).setColor(1.0f, 1.0f, 0.9f, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        }
    }

    private static void drawTrenchSegment(Matrix4f matrix, VertexConsumer consumer, float x1, float z1, float x2, float z2, float width, float r, float g, float b, float a) {
        float dx = x2 - x1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001f) return;

        float nx = -dz / len * width * 0.5f;
        float nz = dx / len * width * 0.5f;
        float tip = 0.3f;
        consumer.addVertex(matrix, x1 - nx, 0.07f, z1 - nz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1 + nx, 0.07f, z1 + nz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 + nx * tip, 0.07f, z2 + nz * tip).setColor(r, g, b, a * 0.12f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 - nx * tip, 0.07f, z2 - nz * tip).setColor(r, g, b, a * 0.12f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1 - nx * 0.24f, 0.085f, z1 - nz * 0.24f).setColor(1.0f, 0.96f, 0.75f, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1 + nx * 0.24f, 0.085f, z1 + nz * 0.24f).setColor(1.0f, 0.96f, 0.75f, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 + nx * 0.06f, 0.085f, z2 + nz * 0.06f).setColor(1.0f, 0.96f, 0.75f, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 - nx * 0.06f, 0.085f, z2 - nz * 0.06f).setColor(1.0f, 0.96f, 0.75f, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }
}
