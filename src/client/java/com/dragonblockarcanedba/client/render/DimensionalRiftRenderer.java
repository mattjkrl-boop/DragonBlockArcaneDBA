package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.DimensionalRiftEntity;
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
 * Entity Renderer for Dimensional Rift in Minecraft 26.2.
 * Renders 3D concentric ground accretion shockwave rings, a towering vertical spatial fissure, 18 orbital crystalline void spikes, and a black hole singularity core.
 */
public class DimensionalRiftRenderer extends EntityRenderer<DimensionalRiftEntity, DimensionalRiftRenderer.DimensionalRiftRenderState> {

    public DimensionalRiftRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class DimensionalRiftRenderState extends EntityRenderState {
        public float radius = 3.0f;
        public boolean isCollapsing = false;
        public float age = 0;
        public double wx = 0, wy = 0, wz = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(DimensionalRiftEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(DimensionalRiftEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public DimensionalRiftRenderState createRenderState() {
        return new DimensionalRiftRenderState();
    }

    @Override
    public void extractRenderState(DimensionalRiftEntity entity, DimensionalRiftRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.isCollapsing = entity.isCollapsing();
        state.age = entity.tickCount + partialTicks;
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    @Override
    public void submit(DimensionalRiftRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        float maxLife = (float) DimensionalRiftEntity.MAX_LIFETIME;
        float lifeT = Math.min(1.0f, state.age / maxLife);
        float pull = lifeT * lifeT * (3.0f - 2.0f * lifeT);
        if (state.isCollapsing) {
            pull = Math.min(1.0f, pull + 0.40f);
        }
        final float pullIn = pull;
        // One shared envelope for alpha, the same fractions the entity uses for
        // its pull and particles, so the visual builds across the expansion and
        // dies across the collapse instead of popping at either end.
        final float fade = com.dragonblockarcanedba.util.FxEnvelope.envelope(
            state.age, maxLife,
            DimensionalRiftEntity.ENVELOPE_BUILD_FRAC, DimensionalRiftEntity.ENVELOPE_DECAY_FRAC);
        // Distance scaling (standard point 1): distant rifts step their segment
        // counts down, near rifts stay rich.
        final float distScale = com.dragonblockarcanedba.client.render.ki.LayeredRender.distScale(
            cameraState, state.wx, state.wy, state.wz);
        float radius = state.radius;

        float rot = state.age * (state.isCollapsing ? 25.0f : 8.0f);
        float pulse = 0.85f + 0.15f * (float) Math.sin(state.age * 0.25f);

        poseStack.pushPose();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // Ground anchor glow (shared LayeredRender): a soft violet disc that
            // grounds the rift to the world under everything else.
            com.dragonblockarcanedba.client.render.ki.LayeredRender.drawAnchorGlow(
                pose, buffer, radius * 1.05f, 0.04f, 0.75f, 0.20f, 1.0f, 0.50f * fade, distScale);

            // 1. Concentric 3D Ground Accretion Shockwave Rings (Expanding across terrain)
            int ringSegments = com.dragonblockarcanedba.util.FxEnvelope.segments(48, distScale, 20);

            // Outer Accretion Shockwave (Primary ring at current radius)
            float outerWidth = Math.min(2.0f, radius * 0.25f);
            drawBeveledShockRing(matrix, buffer, 0, 0.05f, 0, radius, radius - outerWidth, 0.55f * pulse, ringSegments,
                0.05f, 0.0f, 0.12f, 0.0f,
                0.60f, 0.0f, 0.90f, 0.90f * fade,
                1.0f, 0.05f, 0.80f, 0.95f * fade
            );
            // Thin bright lip on the outer ring, so the rift edge cuts instead of sitting flat.
            drawRotatingFlatRing(matrix, buffer, rot * 0.55f, radius * 1.04f, radius * 0.94f,
                com.dragonblockarcanedba.util.FxEnvelope.segments(48, distScale, 20),
                1.0f, 0.72f, 0.95f, 0.5f * fade
            );

            // Middle Accretion Shockwave (Counter-rotating at 65% radius)
            float midRadius = radius * 0.65f;
            float midWidth = Math.min(1.6f, midRadius * 0.28f);
            drawRotatingFlatRing(matrix, buffer, -rot * 1.4f, midRadius, midRadius - midWidth,
                com.dragonblockarcanedba.util.FxEnvelope.segments(32, distScale, 14),
                0.85f, 0.05f, 0.95f, 0.80f * fade
            );

            // Inner Accretion Compression Disk (High speed at 35% radius)
            float innerRadius = radius * 0.35f;
            float innerWidth = Math.min(1.2f, innerRadius * 0.35f);
            drawRotatingFlatRing(matrix, buffer, rot * 2.2f, innerRadius, innerRadius - innerWidth,
                com.dragonblockarcanedba.util.FxEnvelope.segments(28, distScale, 12),
                0.20f, 0.90f, 1.0f, 0.85f * fade
            );

            // 2. Vertical Spatial Fissure / Dimensional Eye Tear (Towering up to 8 blocks high)
            float vHeight = Math.min(8.0f, 2.5f + (radius * 0.4f)) * pulse;
            float vWidth = Math.min(1.8f, 0.4f + (radius * 0.1f));

            // Cross-Intersecting Dimensional Fissure Quads (Void core + radiant border)
            drawVerticalTearPlane(matrix, buffer, vWidth, vHeight, 0.0f, 0.05f, 0.0f, 0.10f, 0.95f * fade, 0.80f, 0.05f, 0.95f, 1.0f * fade);
            drawVerticalTearPlane(matrix, buffer, vWidth, vHeight, 60.0f, 0.05f, 0.0f, 0.10f, 0.95f * fade, 0.20f, 0.90f, 1.0f, 0.90f * fade);
            drawVerticalTearPlane(matrix, buffer, vWidth, vHeight, 120.0f, 0.05f, 0.0f, 0.10f, 0.95f * fade, 1.0f, 0.10f, 0.70f, 0.90f * fade);

            // Layered vortex bands (shared LayeredRender): three counter rotating
            // layers around the tears, bright core, violet mid, wispy outer, so the
            // rift body has the standard's depth instead of only flat ground rings.
            com.dragonblockarcanedba.client.render.ki.LayeredRender.drawVortexBands(
                pose, buffer, radius * 0.62f, vHeight, state.age,
                0.62f, 0.05f, 0.95f, fade, distScale);

            // 3. Orbital Gravitational Void Spikes / Soul Shards (18 3D crystalline spikes orbiting)
            int shardCount = 18;
            // The ring eases into the hole over the rift's life.
            float shardOrbitRadius = radius * (0.90f - 0.58f * pullIn);

            for (int i = 0; i < shardCount; i++) {
                // The 18 signature spikes stay, but each is now its own shard:
                // deterministic per spike variation in size, thickness, orbit
                // band, height band, and precession speed, instead of 18
                // identical pyramids on one ring at one height.
                float sizeVar = 0.75f + 0.55f * hash(i, 0.13f);
                float shardHeight = 1.4f * pulse * sizeVar;
                float bandRadius = shardOrbitRadius * (i % 2 == 0 ? 1.0f : 0.84f);
                float speedVar = 0.85f + 0.30f * hash(i, 0.47f);
                double shardAngle = (i / (double) shardCount) * Math.PI * 2.0
                    + i * 2.399963 // golden angle offset breaks the uniform ring spacing
                    + Math.toRadians(rot * 0.8f * speedVar);
                float sx = (float) Math.cos(shardAngle) * bandRadius;
                float sz = (float) Math.sin(shardAngle) * bandRadius;

                // Inward lean toward singularity core
                float leanInX = -(float) Math.cos(shardAngle) * 0.35f;
                float leanInZ = -(float) Math.sin(shardAngle) * 0.35f;

                float shardThickness = 0.22f * (0.70f + 0.60f * hash(i, 0.29f));
                float perpX = -(float) Math.sin(shardAngle) * shardThickness;
                float perpZ = (float) Math.cos(shardAngle) * shardThickness;

                // Each shard rides its own height band and bobs on its own phase
                // and amplitude instead of the whole ring sharing one flat plane.
                float bobY = 0.05f + 0.35f * hash(i, 0.71f)
                    + (0.05f + 0.25f * hash(i, 0.53f)) * (0.5f + 0.5f * (float) Math.sin(state.age * 0.35f + i * 0.7f));

                draw3DShardPyramid(matrix, buffer,
                    sx - perpX, bobY, sz - perpZ,
                    sx + perpX, bobY, sz + perpZ,
                    sx + leanInX, bobY + shardHeight, sz + leanInZ,
                    0.50f, 0.0f, 0.85f, 0.85f * fade,
                    0.95f, 0.2f, 1.0f, 0.95f * fade
                );
            }

            // 4. Black hole: dark body, a thin bright horizon, and a small white heart.
            float coreSize = Math.max(0.55f, 1.15f * pulse) * (1.0f - pullIn * 0.22f);
            drawSingularityOctahedron(matrix, buffer, coreSize, 0.02f, 0.0f, 0.05f, 0.98f * fade);
            drawRotatingFlatRing(matrix, buffer, rot * 2.2f, coreSize * 1.35f, coreSize * 1.12f, 20,
                0.85f, 0.90f, 1.0f, 0.75f * fade);
            drawSingularityOctahedron(matrix, buffer, coreSize * 0.28f, 0.95f, 0.90f, 1.0f, 1.0f * fade);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    /** Stable per index variation in 0..1 (golden ratio hash), identical every frame for a given spike. */
    private static float hash(int i, float salt) {
        float v = (i * 0.6180339887f + salt) % 1.0f;
        return v < 0 ? v + 1.0f : v;
    }

    private static void drawBeveledShockRing(Matrix4f matrix, VertexConsumer consumer,
                                             float cx, float cy, float cz,
                                             float rOuter, float rInner, float height,
                                             int segments,
                                             float rBase, float gBase, float bBase, float aBase,
                                             float rMid, float gMid, float bMid, float aMid,
                                             float rTop, float gTop, float bTop, float aTop) {
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1Out = cx + (float) Math.cos(a1) * rOuter;
            float z1Out = cz + (float) Math.sin(a1) * rOuter;
            float x2Out = cx + (float) Math.cos(a2) * rOuter;
            float z2Out = cz + (float) Math.sin(a2) * rOuter;

            float x1In = cx + (float) Math.cos(a1) * rInner;
            float z1In = cz + (float) Math.sin(a1) * rInner;
            float x2In = cx + (float) Math.cos(a2) * rInner;
            float z2In = cz + (float) Math.sin(a2) * rInner;

            // Outer rising wall
            consumer.addVertex(matrix, x1Out, cy, z1Out).setColor(rBase, gBase, bBase, aBase).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2Out, cy, z2Out).setColor(rBase, gBase, bBase, aBase).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2Out, cy + height, z2Out).setColor(rTop, gTop, bTop, aTop).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1Out, cy + height, z1Out).setColor(rTop, gTop, bTop, aTop).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Inward slope connecting to inner floor
            consumer.addVertex(matrix, x1Out, cy + height, z1Out).setColor(rTop, gTop, bTop, aTop).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2Out, cy + height, z2Out).setColor(rTop, gTop, bTop, aTop).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2In, cy, z2In).setColor(rMid, gMid, bMid, aMid).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1In, cy, z1In).setColor(rMid, gMid, bMid, aMid).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawRotatingFlatRing(Matrix4f matrix, VertexConsumer consumer, float rotDeg, float rOuter, float rInner, int segments, float r, float g, float b, float a) {
        double radOffset = Math.toRadians(rotDeg);
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0 + radOffset;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0 + radOffset;

            float x1Out = (float) Math.cos(a1) * rOuter;
            float z1Out = (float) Math.sin(a1) * rOuter;
            float x2Out = (float) Math.cos(a2) * rOuter;
            float z2Out = (float) Math.sin(a2) * rOuter;

            float x1In = (float) Math.cos(a1) * rInner;
            float z1In = (float) Math.sin(a1) * rInner;
            float x2In = (float) Math.cos(a2) * rInner;
            float z2In = (float) Math.sin(a2) * rInner;

            consumer.addVertex(matrix, x1In, 0.08f, z1In).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2In, 0.08f, z2In).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2Out, 0.08f, z2Out).setColor(r, g, b, a * 0.4f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, x1Out, 0.08f, z1Out).setColor(r, g, b, a * 0.4f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    private static void drawVerticalTearPlane(Matrix4f matrix, VertexConsumer consumer, float halfWidth, float height, float angleDeg, float rCore, float gCore, float bCore, float aCore, float rEdge, float gEdge, float bEdge, float aEdge) {
        double rad = Math.toRadians(angleDeg);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        // A vertical lens: wide through the middle, pinched at the ground and the crown.
        int segments = 12;
        for (int i = 0; i < segments; i++) {
            float t1 = i / (float) segments;
            float t2 = (i + 1) / (float) segments;
            float y1 = 0.1f + t1 * height;
            float y2 = 0.1f + t2 * height;
            float w1 = (float) Math.sin(t1 * Math.PI) * halfWidth;
            float w2 = (float) Math.sin(t2 * Math.PI) * halfWidth;
            float edgeFade1 = 0.35f + 0.65f * (float) Math.sin(t1 * Math.PI);
            float edgeFade2 = 0.35f + 0.65f * (float) Math.sin(t2 * Math.PI);

            float lx1 = -w1 * cos;
            float lz1 = -w1 * sin;
            float rx1 = w1 * cos;
            float rz1 = w1 * sin;
            float lx2 = -w2 * cos;
            float lz2 = -w2 * sin;
            float rx2 = w2 * cos;
            float rz2 = w2 * sin;

            consumer.addVertex(matrix, lx1, y1, lz1).setColor(rEdge, gEdge, bEdge, aEdge * edgeFade1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 0, y1, 0).setColor(rCore, gCore, bCore, aCore * edgeFade1).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 0, y2, 0).setColor(rCore, gCore, bCore, aCore * edgeFade2).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, lx2, y2, lz2).setColor(rEdge, gEdge, bEdge, aEdge * edgeFade2).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, lx2, y2, lz2).setColor(rEdge, gEdge, bEdge, aEdge * edgeFade2).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, 0, y2, 0).setColor(rCore, gCore, bCore, aCore * edgeFade2).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, 0, y1, 0).setColor(rCore, gCore, bCore, aCore * edgeFade1).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, lx1, y1, lz1).setColor(rEdge, gEdge, bEdge, aEdge * edgeFade1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);

            consumer.addVertex(matrix, 0, y1, 0).setColor(rCore, gCore, bCore, aCore * edgeFade1).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, rx1, y1, rz1).setColor(rEdge, gEdge, bEdge, aEdge * edgeFade1).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, rx2, y2, rz2).setColor(rEdge, gEdge, bEdge, aEdge * edgeFade2).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 0, y2, 0).setColor(rCore, gCore, bCore, aCore * edgeFade2).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 0, y2, 0).setColor(rCore, gCore, bCore, aCore * edgeFade2).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, rx2, y2, rz2).setColor(rEdge, gEdge, bEdge, aEdge * edgeFade2).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, rx1, y1, rz1).setColor(rEdge, gEdge, bEdge, aEdge * edgeFade1).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, 0, y1, 0).setColor(rCore, gCore, bCore, aCore * edgeFade1).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        }
    }

    private static void draw3DShardPyramid(Matrix4f matrix, VertexConsumer consumer,
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
            py = Math.max(elen, 0.05f);
            pz = 0.0f;
            plen = py;
        }
        float thick = Math.max(elen, 0.04f) * 0.85f;
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
            float ix = mx + (bx[i] - mx) * 0.32f + ax * 0.18f;
            float iy = my + (by[i] - my) * 0.32f + ay * 0.18f;
            float iz = mz + (bz[i] - mz) * 0.32f + az * 0.18f;
            float jx = mx + (bx[n] - mx) * 0.32f + ax * 0.18f;
            float jy = my + (by[n] - my) * 0.32f + ay * 0.18f;
            float jz = mz + (bz[n] - mz) * 0.32f + az * 0.18f;
            consumer.addVertex(matrix, ix, iy, iz).setColor(0.9f, 0.75f, 1.0f, aTip).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, jx, jy, jz).setColor(0.9f, 0.75f, 1.0f, aTip).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, mx + ax * 0.84f, my + ay * 0.84f, mz + az * 0.84f).setColor(1.0f, 0.95f, 1.0f, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, mx + ax * 0.84f, my + ay * 0.84f, mz + az * 0.84f).setColor(1.0f, 0.95f, 1.0f, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        }
        float alen = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        float back = alen < 1.0e-4f ? 0.0f : 0.4f / alen;
        consumer.addVertex(matrix, bx[0], by[0], bz[0]).setColor(rBase, gBase, bBase, aBase * 0.35f).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, bx[2], by[2], bz[2]).setColor(rBase, gBase, bBase, aBase * 0.35f).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, mx - ax * back, my - ay * back, mz - az * back).setColor(rBase, gBase, bBase, 0.0f).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, mx - ax * back, my - ay * back, mz - az * back).setColor(rBase, gBase, bBase, 0.0f).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawSingularityOctahedron(Matrix4f matrix, VertexConsumer consumer, float s, float r, float g, float b, float a) {
        float[][] corners = { {s, 0, 0}, {0, 0, s}, {-s, 0, 0}, {0, 0, -s} };
        for (int i = 0; i < 4; i++) {
            int nxt = (i + 1) % 4;

            // Top pyramid
            consumer.addVertex(matrix, corners[i][0], 0.5f, corners[i][2]).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, corners[nxt][0], 0.5f, corners[nxt][2]).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 0, 0.5f + s * 1.25f, 0).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 0, 0.5f + s * 1.25f, 0).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

            // Bottom pyramid
            consumer.addVertex(matrix, corners[i][0], 0.5f, corners[i][2]).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, corners[nxt][0], 0.5f, corners[nxt][2]).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, 0, 0.5f - s * 1.25f, 0).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
            consumer.addVertex(matrix, 0, 0.5f - s * 1.25f, 0).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        }
    }
}
