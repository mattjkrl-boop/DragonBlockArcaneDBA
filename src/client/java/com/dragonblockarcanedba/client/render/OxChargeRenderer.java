package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.OxChargeEntity;
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
 * Entity Renderer for Ox King's Groundbreaker Charging in Minecraft 26.2.
 * Bespoke pass: every shape comes from BespokeGeometry or LayeredRender.
 * The ground shatter is layered crack stars, the compression rings are low
 * broken vortex bands, the tapered heart is the shared vortex bands, the
 * levitating chunks are tumbling displaced domes with branch heat cores,
 * and the ascending tendrils are branch ribbons. Counts, radii, heights,
 * the fade in, and the tremble are unchanged from the spectacle build. The
 * dead drawAscendingTendril helper from the old build is gone.
 */
public class OxChargeRenderer extends EntityRenderer<OxChargeEntity, OxChargeRenderer.OxChargeRenderState> {

    public OxChargeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class OxChargeRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public float age = 0;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(OxChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(OxChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public OxChargeRenderState createRenderState() {
        return new OxChargeRenderState();
    }

    @Override
    public void extractRenderState(OxChargeEntity entity, OxChargeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(OxChargeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        float charge = state.chargeRatio;
        float age = state.age;

        float chargeEase = charge * charge * (3.0f - 2.0f * charge);
        float baseRadius = 1.2f + (chargeEase * 2.6f);
        // Lifecycle (point 5): the charge visual used to appear at full alpha
        // on frame one. It now builds in over the first 6 ticks.
        float fadeIn = FxEnvelope.smoothstep(Math.min(1.0f, age / 6.0f));
        float baseAlpha = (0.70f + (chargeEase * 0.30f)) * fadeIn;
        float tremble = (float) Math.sin(age * 2.5f) * (0.02f + charge * 0.06f);
        float distScale = FxEnvelope.distScale(Math.sqrt(state.distanceToCameraSq));
        long seed = state.seed;

        Random rng = new Random(seed);

        // Levitating tumbling rock chunks: positions, float heights, spins.
        int rockCount = FxEnvelope.segments(10 + (int) (charge * 8), distScale, 6); // 10 to 18 rocks near
        float[] rockX = new float[rockCount];
        float[] rockY = new float[rockCount];
        float[] rockZ = new float[rockCount];
        float[] rockH = new float[rockCount];
        float[] rockW = new float[rockCount];
        float[] rockSpin = new float[rockCount];
        float[] rockTipX = new float[rockCount];
        float[] rockTipZ = new float[rockCount];
        for (int r = 0; r < rockCount; r++) {
            double rockAngle = (r / (double) rockCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.4);
            float rockDist = 0.6f + rng.nextFloat() * (baseRadius * 0.85f);

            float rx = (float) Math.cos(rockAngle) * rockDist + (rng.nextFloat() - 0.5f) * tremble;
            float rz = (float) Math.sin(rockAngle) * rockDist + (rng.nextFloat() - 0.5f) * tremble;

            float floatPhase = age * 0.25f + r * 1.4f;
            float ry = 0.08f + (chargeEase * 1.4f) * (0.5f + 0.5f * (float) Math.sin(floatPhase)) + (rng.nextFloat() * 0.2f * chargeEase);

            float slabHeight = 0.25f + rng.nextFloat() * 0.35f + charge * 0.25f;
            float slabWidth = 0.20f + rng.nextFloat() * 0.25f;

            double spin = age * 0.55 + r * 2.1;
            float leanMag = 0.15f + charge * 0.2f;
            double leanAng = rockAngle + age * 0.4 + r * 1.3;

            rockX[r] = rx;
            rockY[r] = ry;
            rockZ[r] = rz;
            rockH[r] = slabHeight;
            rockW[r] = slabWidth;
            rockSpin[r] = (float) Math.toDegrees(spin);
            rockTipX[r] = rx + (float) Math.cos(leanAng) * leanMag;
            rockTipZ[r] = rz + (float) Math.sin(leanAng) * leanMag;
        }

        // Ascending heat tendrils above 25 percent charge: branch ribbons
        // from a ground point spiraling in to a tip above the brace heart.
        int tendrils = charge >= 0.25f ? 6 + (int) (charge * 6) : 0;
        float[] tendrilFrom = new float[tendrils * 3];
        float[] tendrilTo = new float[tendrils * 3];
        for (int i = 0; i < tendrils; i++) {
            double tAng = (i / (double) tendrils) * Math.PI * 2.0 + (age * 0.08);
            float tDist = 0.4f + (float) (rng.nextDouble() * 0.8);
            float tx1 = (float) Math.cos(tAng) * tDist;
            float tz1 = (float) Math.sin(tAng) * tDist;
            float tHeight = 0.5f + chargeEase * 1.5f;
            tendrilFrom[i * 3] = tx1;
            tendrilFrom[i * 3 + 1] = 0.05f;
            tendrilFrom[i * 3 + 2] = tz1;
            tendrilTo[i * 3] = tx1 * 0.22f;
            tendrilTo[i * 3 + 1] = tHeight;
            tendrilTo[i * 3 + 2] = tz1 * 0.22f;
        }

        float baseAlphaF = baseAlpha;
        float distScaleF = distScale;
        float colH = 0.35f + chargeEase * 2.4f;
        float ringTwist = (float) Math.toRadians(age * 8.0f);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // 1. Ground shatter: layered crack stars (magma base, hot core).
            BespokeGeometry.crackStar(pose, buffer, seed, baseRadius * 0.95f,
                0.95f, 0.25f, 0.02f, baseAlphaF * 0.95f, 1.0f, distScaleF);
            BespokeGeometry.crackStar(pose, buffer, seed, baseRadius * 0.88f,
                1.0f, 0.85f, 0.20f, baseAlphaF, 1.0f, distScaleF);

            // 2. Compression rings: dark basalt lip outside, molten gold
            // along the inner edge, both as low broken vortex bands.
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xB1A6L,
                baseRadius * 0.97f, 0.03f, 0.08f, ringTwist,
                0.55f, 0.16f, 0.02f, baseAlphaF * 0.85f, distScaleF);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xB1A7L,
                baseRadius * 0.905f, 0.045f, 0.095f, ringTwist,
                1.0f, 0.78f, 0.18f, baseAlphaF * 0.95f, distScaleF);

            // 3. Tapered heart of the brace: the shared vortex bands rising
            // with the charge.
            LayeredRender.drawVortexBands(pose, buffer, 0.30f, colH, age,
                1.0f, 0.55f, 0.08f, baseAlphaF * 0.90f, distScaleF);

            // 4. Molten cores of the levitating chunks.
            for (int r = 0; r < rockCount; r++) {
                BespokeGeometry.branch(pose, buffer, seed ^ (0xBE10L + r * 131L),
                    new Vec3(rockX[r], rockY[r] + 0.02f, rockZ[r]),
                    new Vec3(rockTipX[r], rockY[r] + rockH[r], rockTipZ[r]),
                    0.07f,
                    1.0f, 0.40f, 0.05f, baseAlphaF * 0.95f, 1.0f, distScaleF);
            }

            // 5. Ascending heat tendrils: edge pass and hot core pass.
            for (int i = 0; i < tendrils; i++) {
                Vec3 from = new Vec3(tendrilFrom[i * 3], tendrilFrom[i * 3 + 1], tendrilFrom[i * 3 + 2]);
                Vec3 to = new Vec3(tendrilTo[i * 3], tendrilTo[i * 3 + 1], tendrilTo[i * 3 + 2]);
                BespokeGeometry.branch(pose, buffer, seed ^ (0x7EADL + i * 137L),
                    from, to, 0.13f,
                    1.0f, 0.42f, 0.05f, baseAlphaF * 0.80f, 1.0f, distScaleF);
                BespokeGeometry.branch(pose, buffer, seed ^ (0x7EAEL + i * 137L),
                    from, new Vec3(to.x, to.y * 0.92f, to.z), 0.045f,
                    1.0f, 0.92f, 0.40f, baseAlphaF * 0.95f, 1.0f, distScaleF);
            }
        });

        // Chunk bodies: squashed displaced domes tumbling on their own spins.
        for (int r = 0; r < rockCount; r++) {
            float domeRadius = Math.max(0.06f, rockW[r] * 0.5f);
            float heightScale = rockH[r] / domeRadius;
            long rockSeed = seed ^ (0x9A40L + r * 0x9E37L);
            float tumble = (float) Math.sin(age * 0.4f + r * 1.3f) * 18.0f;
            poseStack.pushPose();
            poseStack.translate(rockX[r], rockY[r], rockZ[r]);
            poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(rockSpin[r])));
            poseStack.mulPose(new Matrix4f().rotation(Axis.XP.rotationDegrees(tumble)));
            poseStack.scale(1.0f, heightScale, 1.0f);
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.displacedDome(pose, buffer, rockSeed, domeRadius,
                    0.25f, 0.18f, 0.12f, baseAlphaF, 1.0f, distScaleF);
            });
            poseStack.popPose();
        }

        super.submit(state, poseStack, collector, cameraState);
    }
}
