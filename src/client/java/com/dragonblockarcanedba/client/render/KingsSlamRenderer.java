package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.KingsSlamEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * Entity Renderer for Ox King's Slam (Normal and Flawless) in Minecraft 26.2.
 * Bespoke pass: every shape comes from BespokeGeometry. The radiating canyon
 * trenches are layered crack stars, the dome walls are Y-squashed displaced
 * domes, the crest ring is a low broken vortex band over a glow fan caldera,
 * the monoliths are elongated displaced domes with branch veins, and the
 * flawless geysers are plumes. All spectacle numbers (growth curve, fade,
 * radii, heights, counts) are unchanged.
 */
public class KingsSlamRenderer extends EntityRenderer<KingsSlamEntity, KingsSlamRenderer.KingsSlamRenderState> {

    public KingsSlamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class KingsSlamRenderState extends EntityRenderState {
        public float radius = 10.0f;
        public boolean isFlawless = false;
        public float age = 0;
        public int lifetime = 40;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(KingsSlamEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(KingsSlamEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public KingsSlamRenderState createRenderState() {
        return new KingsSlamRenderState();
    }

    @Override
    public void extractRenderState(KingsSlamEntity entity, KingsSlamRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.isFlawless = entity.isFlawless();
        state.age = entity.tickCount + partialTicks;
        state.lifetime = entity.getLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(KingsSlamRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float radius = state.radius;
        boolean isFlawless = state.isFlawless;
        float age = state.age;
        float life = Math.min(1.0f, age / (float) Math.max(1, state.lifetime));
        float growT = Math.min(1.0f, life / 0.55f);
        float grow = 1.0f - (1.0f - growT) * (1.0f - growT) * (1.0f - growT);
        float exitT = life <= 0.62f ? 0.0f : Math.min(1.0f, (life - 0.62f) / 0.38f);
        float fade = 1.0f - exitT * exitT * (3.0f - 2.0f * exitT);
        RenderType renderType = KiRenderHelper.kiRenderType();

        float rCol = isFlawless ? 1.0f : 1.0f;
        float gCol = isFlawless ? 0.85f : 0.35f;
        float bCol = isFlawless ? 0.20f : 0.02f;

        float distScale = FxEnvelope.distScale(Math.sqrt(state.distanceToCameraSq));
        long seed = state.seed;
        float shockR = radius * grow;
        float domeH = (2.5f + (isFlawless ? 3.0f : 1.5f)) * grow * (1.0f - exitT * 0.55f);

        Random rng = new Random(seed);

        // Monolith placement (positions were random in the spectacle build
        // too; the counts, distance band, heights, and leans are unchanged).
        int rockCount = FxEnvelope.segments(isFlawless ? 28 : 16, distScale, 8);
        float[] rockX = new float[rockCount];
        float[] rockZ = new float[rockCount];
        float[] rockH = new float[rockCount];
        float[] rockW = new float[rockCount];
        float[] rockRot = new float[rockCount];
        float[] rockLean = new float[rockCount];
        float[] rockTipX = new float[rockCount];
        float[] rockTipZ = new float[rockCount];
        for (int r = 0; r < rockCount; r++) {
            double rockAngle = (r / (double) rockCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.35);
            float rockDist = radius * (0.25f + rng.nextFloat() * 0.65f);

            float rx = (float) Math.cos(rockAngle) * rockDist;
            float rz = (float) Math.sin(rockAngle) * rockDist;

            float maxH = (isFlawless ? 2.5f : 1.5f) + rng.nextFloat() * (isFlawless ? 3.0f : 1.8f);
            float slabHeight = maxH * grow * (1.0f - exitT * 0.35f);
            float slabWidth = 0.45f + rng.nextFloat() * 0.40f;

            float tiltFactor = 0.35f + rng.nextFloat() * 0.25f;

            rockX[r] = rx;
            rockZ[r] = rz;
            rockH[r] = slabHeight;
            rockW[r] = slabWidth;
            rockRot[r] = (float) -Math.toDegrees(rockAngle);
            rockLean[r] = (float) Math.toDegrees(Math.atan(tiltFactor));
            rockTipX[r] = rx + (float) Math.cos(rockAngle) * (slabHeight * tiltFactor);
            rockTipZ[r] = rz + (float) Math.sin(rockAngle) * (slabHeight * tiltFactor);
        }

        // Flawless geyser placement, with the same per-geyser height flicker.
        int geyserCount = isFlawless ? 8 : 0;
        float[] geyserX = new float[geyserCount];
        float[] geyserZ = new float[geyserCount];
        float[] geyserH = new float[geyserCount];
        if (isFlawless) {
            for (int g = 0; g < geyserCount; g++) {
                double gAng = (g / (double) geyserCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.2);
                float gDist = radius * (0.45f + rng.nextFloat() * 0.40f);
                geyserX[g] = (float) Math.cos(gAng) * gDist;
                geyserZ[g] = (float) Math.sin(gAng) * gDist;
                float gHeight = 6.0f + rng.nextFloat() * 2.5f;
                gHeight *= grow * (1.0f - exitT) * (0.90f + 0.10f * (float) Math.sin(age * 0.9f + g * 1.7f));
                geyserH[g] = gHeight;
            }
        }

        float fadeF = fade;
        float distScaleF = distScale;
        boolean showDome = fade > 0.05f;
        // Cracks spread outward as the dome grows (the spectacle trenches
        // had full length from tick one but zero width; the star splits the
        // difference so tick one is not a full size decal).
        float crackSpread = 0.35f + 0.65f * grow;

        // Ground layer: anchor glow, crest band, caldera glow fan, layered
        // crack stars for the trenches, and the monolith veins.
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            if (showDome) {
                LayeredRender.drawAnchorGlow(pose, buffer, Math.max(1.0f, shockR), 0.07f,
                    rCol, gCol, bCol, LayeredRender.ALPHA_MID * fadeF, distScaleF);
                BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xC9E57L,
                    Math.max(0.2f, shockR * 0.92f), 0.04f, 0.16f, 0.0f,
                    rCol, gCol, bCol, fadeF * 0.85f, distScaleF);
                BespokeGeometry.glowFan(pose, buffer, seed ^ 0xCA1DL,
                    Math.max(0.2f, shockR * 0.65f), 0.05f,
                    1.0f, isFlawless ? 0.95f : 0.60f, 0.10f, fadeF * 0.70f, distScaleF);
            }

            float crackR = radius * 0.95f * crackSpread;
            BespokeGeometry.crackStar(pose, buffer, seed, crackR,
                rCol, gCol * 0.7f, bCol * 0.5f, fadeF * 0.95f, 1.0f, distScaleF);
            BespokeGeometry.crackStar(pose, buffer, seed, crackR * 0.97f,
                1.0f, isFlawless ? 1.0f : 0.85f, 0.40f, fadeF, 1.0f, distScaleF);
            BespokeGeometry.crackStar(pose, buffer, seed, crackR * 0.93f,
                1.0f, 0.98f, 0.72f, fadeF * 0.80f, 1.0f, distScaleF);

            for (int r = 0; r < rockCount; r++) {
                BespokeGeometry.branch(pose, buffer, seed ^ (0xBE10L + r * 131L),
                    new Vec3(rockX[r], 0.08f, rockZ[r]),
                    new Vec3(rockTipX[r], Math.max(0.10f, rockH[r] * 0.9f), rockTipZ[r]),
                    Math.max(0.03f, rockW[r] * 0.20f),
                    1.0f, isFlawless ? 0.70f : 0.30f, 0.05f, fadeF * 0.95f, 1.0f, distScaleF);
            }
        });

        // Dome walls: displaced domes squashed on Y to the spectacle dome
        // height (the builder's own height is its radius, so the pose scale
        // carries the difference). Outer wall, then the hotter inner wall.
        if (showDome && shockR > 0.2f) {
            poseStack.pushPose();
            poseStack.scale(1.0f, domeH / shockR, 1.0f);
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xD04E1L, shockR,
                    rCol, gCol, bCol, fadeF * 0.65f, 1.0f, distScaleF);
            });
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.scale(1.0f, (domeH * 0.70f) / (shockR * 0.52f), 1.0f);
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xD04E2L, shockR * 0.52f,
                    1.0f, isFlawless ? 1.0f : 0.82f, isFlawless ? 0.45f : 0.22f, fadeF * 0.55f, 1.0f, distScaleF);
            });
            poseStack.popPose();
        }

        // Monolith bodies: elongated displaced domes leaned outward.
        for (int r = 0; r < rockCount; r++) {
            float domeRadius = Math.max(0.10f, rockW[r] * 0.55f);
            float heightScale = rockH[r] / domeRadius;
            long rockSeed = seed ^ (0x9A40L + r * 0x9E37L);
            poseStack.pushPose();
            poseStack.translate(rockX[r], 0.05f, rockZ[r]);
            poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(rockRot[r])));
            poseStack.mulPose(new Matrix4f().rotation(Axis.ZP.rotationDegrees(-rockLean[r])));
            poseStack.scale(1.0f, heightScale, 1.0f);
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.displacedDome(pose, buffer, rockSeed, domeRadius,
                    0.25f, 0.20f, 0.16f, fadeF, 1.0f, distScaleF);
            });
            poseStack.popPose();
        }

        // Flawless geysers: eruption plumes at the seeded positions.
        for (int g = 0; g < geyserCount; g++) {
            if (geyserH[g] <= 0.1f) continue;
            long geyserSeed = seed ^ (0x6E15L + g * 0x9E37L);
            float gHeight = geyserH[g];
            poseStack.pushPose();
            poseStack.translate(geyserX[g], 0.0f, geyserZ[g]);
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.plume(pose, buffer, geyserSeed, 0.70f, gHeight,
                    1.0f, 0.52f, 0.07f, fadeF * 0.85f, 1.0f, distScaleF);
            });
            poseStack.popPose();
        }
    }
}
