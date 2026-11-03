package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.OxShockwaveEntity;
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
 * Entity Renderer for Ox King's Groundbreaker Shockwave in Minecraft 26.2.
 * Renders towering 3D jagged earth crags, erupted basalt monoliths, tiered volcanic magma strata,
 * and expanding multi-layered 3D shockwave compression walls.
 */
public class OxShockwaveRenderer extends EntityRenderer<OxShockwaveEntity, OxShockwaveRenderer.OxShockwaveRenderState> {
    public OxShockwaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class OxShockwaveRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public boolean isSubWave = false;
        public float currentRadius = 1.0f;
        public float age = 0;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(OxShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(OxShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public OxShockwaveRenderState createRenderState() {
        return new OxShockwaveRenderState();
    }

    @Override
    public void extractRenderState(OxShockwaveEntity entity, OxShockwaveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.isSubWave = entity.isSubWave();
        state.currentRadius = entity.getCurrentRadius();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(OxShockwaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        float radius = state.currentRadius;
        float charge = state.chargeRatio;
        boolean subWave = state.isSubWave;
        float age = state.age;

        float waveScale = subWave ? 0.75f : 1.0f;
        float cragMaxHeight = (1.2f + charge * 2.8f) * waveScale;
        // The alpha used to floor at 0.2, so the wave popped out at discard.
        // Recompute the entity's real lifetime from the synced charge (maxTicks
        // = max(10, maxRadius x 1.3), maxRadius = 4 + charge x 20, x 0.75 for
        // sub waves) and fade fully to 0 across the final 30 percent.
        float maxRadius = (4.0f + Math.max(0.05f, Math.min(1.0f, charge)) * 20.0f) * waveScale;
        float maxTicks = Math.max(10.0f, maxRadius * 1.3f);
        float lifeProgress = Math.min(1.0f, age / maxTicks);
        float riseT = Math.min(1.0f, lifeProgress / 0.22f);
        float rise = 1.0f - (1.0f - riseT) * (1.0f - riseT) * (1.0f - riseT);
        float exitT = lifeProgress <= 0.70f ? 0.0f : Math.min(1.0f, (lifeProgress - 0.70f) / 0.30f);
        float endFade = 1.0f - exitT * exitT * (3.0f - 2.0f * exitT);
        float crest = rise * (1.0f - exitT * 0.62f);
        float alpha = endFade;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            Random rng = new Random(state.seed);

            // 1. Towering 3D Erupted Earth Crags & Basalt Monoliths around the expanding perimeter
            int cragCount = subWave ? 24 : (28 + (int) (charge * 8)); // 28 to 36 crags
            for (int c = 0; c < cragCount; c++) {
                double baseAngle = (c / (double) cragCount) * Math.PI * 2.0;
                double angleOffset = (rng.nextDouble() - 0.5) * 0.15;
                double angle = baseAngle + angleOffset;

                float rOffset = (rng.nextFloat() - 0.5f) * 0.8f;
                float cragDist = Math.max(0.5f, radius + rOffset);

                float cx = (float) Math.cos(angle) * cragDist;
                float cz = (float) Math.sin(angle) * cragDist;

                // Height variations
                float hVar = 0.75f + rng.nextFloat() * 0.5f;
                float cragHeight = cragMaxHeight * hVar * crest;
                float cragWidth = (0.45f + rng.nextFloat() * 0.35f) * waveScale;

                // Outward radial tilt (crags lean violently forward/outward as earth fractures)
                float tiltFactor = 0.35f + rng.nextFloat() * 0.25f;
                float tipX = cx + (float) Math.cos(angle) * (cragHeight * tiltFactor);
                float tipZ = cz + (float) Math.sin(angle) * (cragHeight * tiltFactor);
                float tipY = cragHeight;

                // Tangent vector for width
                float tx = -(float) Math.sin(angle) * cragWidth * 0.5f;
                float tz = (float) Math.cos(angle) * cragWidth * 0.5f;

                // Secondary depth offset for thick 3D rock face
                float nx = (float) Math.cos(angle) * 0.25f;
                float nz = (float) Math.sin(angle) * 0.25f;

                // Draw faceted 3D Crag (Molten volcanic base transitioning into dark basalt top)
                drawFacetedCrag(matrix, buffer,
                    cx - tx, 0.05f, cz - tz,
                    cx + tx, 0.05f, cz + tz,
                    cx - nx, 0.05f, cz - nz,
                    tipX, tipY, tipZ,
                    1.0f, subWave ? 0.6f : 0.25f, 0.02f, alpha * 0.95f, // Molten base
                    0.28f, 0.22f, 0.18f, alpha                        // Basalt peak
                );
                float vein = cragWidth * 0.22f;
                float vx = -(float) Math.sin(angle) * vein * 0.5f;
                float vz = (float) Math.cos(angle) * vein * 0.5f;
                drawFacetedCrag(matrix, buffer,
                    cx - vx, 0.08f, cz - vz,
                    cx + vx, 0.08f, cz + vz,
                    cx, 0.08f, cz,
                    tipX, tipY * 0.88f, tipZ,
                    1.0f, 0.92f, 0.28f, alpha * 0.90f,
                    1.0f, 0.98f, 0.72f, alpha * 0.40f
                );
            }

            // Broken molten teeth along the leading edge. Narrow and uneven,
            // so the crest stays jagged earth instead of a flat wall.
            int teeth = 26;
            for (int i = 0; i < teeth; i++) {
                double a = (i / (double) teeth) * Math.PI * 2.0 + rng.nextDouble() * 0.08;
                float jag = 0.28f + rng.nextFloat() * 1.25f;
                float h = cragMaxHeight * 0.32f * jag * crest * (0.90f + 0.10f * (float) Math.sin(age * 0.9f + i));
                float dist = Math.max(0.4f, radius * (0.97f + (rng.nextFloat() - 0.5f) * 0.10f));
                float sx = (float) Math.cos(a) * dist;
                float sz = (float) Math.sin(a) * dist;
                float w = (0.06f + rng.nextFloat() * 0.07f) * waveScale;
                float tx = -(float) Math.sin(a) * w;
                float tz = (float) Math.cos(a) * w;
                float lean = 0.32f;
                drawFacetedCrag(matrix, buffer,
                    sx - tx, 0.05f, sz - tz,
                    sx + tx, 0.05f, sz + tz,
                    sx, 0.05f, sz,
                    sx + (float) Math.cos(a) * h * lean, h, sz + (float) Math.sin(a) * h * lean,
                    1.0f, 0.78f, 0.12f, alpha * 0.95f,
                    1.0f, 0.98f, 0.70f, alpha * 0.35f
                );
            }

            int shardCount = 14;
            for (int s = 0; s < shardCount; s++) {
                double sAngle = (s / (double) shardCount) * Math.PI * 2.0 + rng.nextDouble() * 0.3;
                float sDist = radius * (0.86f + rng.nextFloat() * 0.22f);
                float sx = (float) Math.cos(sAngle) * sDist;
                float sz = (float) Math.sin(sAngle) * sDist;
                float shardH = (0.55f + rng.nextFloat() * 1.05f) * waveScale * crest;
                float shardW = 0.12f + rng.nextFloat() * 0.10f;
                float tx = -(float) Math.sin(sAngle) * shardW;
                float tz = (float) Math.cos(sAngle) * shardW;
                float lean = 0.34f;
                drawFacetedCrag(matrix, buffer,
                    sx - tx, 0.05f, sz - tz,
                    sx + tx, 0.05f, sz + tz,
                    sx - (float) Math.cos(sAngle) * shardW * 0.55f, 0.05f, sz - (float) Math.sin(sAngle) * shardW * 0.55f,
                    sx + (float) Math.cos(sAngle) * shardH * lean, shardH, sz + (float) Math.sin(sAngle) * shardH * lean,
                    1.0f, 0.48f, 0.08f, alpha * 0.90f,
                    0.20f, 0.16f, 0.14f, alpha
                );
            }
        });

        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawFacetedCrag(Matrix4f matrix, VertexConsumer consumer,
                                        float x1, float y1, float z1,
                                        float x2, float y2, float z2,
                                        float x3, float y3, float z3,
                                        float tipX, float tipY, float tipZ,
                                        float rBase, float gBase, float bBase, float aBase,
                                        float rTip, float gTip, float bTip, float aTip) {
        // Face 1: (v1, v2, tip)
        consumer.addVertex(matrix, x1, y1, z1).setColor(rBase, gBase, bBase, aBase).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(rBase, gBase, bBase, aBase).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Face 2: (v2, v3, tip)
        consumer.addVertex(matrix, x2, y2, z2).setColor(rBase, gBase, bBase, aBase).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(rBase, gBase, bBase, aBase).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Face 3: (v3, v1, tip)
        consumer.addVertex(matrix, x3, y3, z3).setColor(rBase, gBase, bBase, aBase).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(rBase, gBase, bBase, aBase).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, tipX, tipY, tipZ).setColor(rTip, gTip, bTip, aTip).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

}
