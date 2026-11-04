package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.OxShockwaveEntity;
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
 * Entity Renderer for Ox King's Groundbreaker Shockwave in Minecraft 26.2.
 * Bespoke pass: every shape comes from BespokeGeometry. The rolling dust
 * walls are broken vortex bands, the erupted perimeter crags are elongated
 * noise displaced domes leaning outward, and the molten veins, leading edge
 * teeth, and inner shards are jagged branch ribbons. Placement still follows
 * the spectacle pass's seeded Random stream (same seed, same consumption
 * order), so positions are unchanged; only the silhouettes are bespoke.
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

        // Distance scaling (point 1): far waves step their geometry down,
        // near waves stay rich. Alpha follows the square root of the scale.
        float distScale = FxEnvelope.distScale(Math.sqrt(state.distanceToCameraSq));
        float distAlpha = (float) Math.sqrt(distScale);
        long seed = state.seed;

        // Placement stream: consumed in the spectacle order (crags, teeth,
        // shards) so every position matches the previous build exactly.
        Random rng = new Random(seed);

        // 1. Towering erupted earth crags around the expanding perimeter.
        // Each crag is an elongated displaced dome; its molten vein is a
        // branch ribbon from the crag base to near its tip.
        int cragCount = FxEnvelope.segments(subWave ? 24 : (28 + (int) (charge * 8)), distScale, 12); // 28 to 36 crags near
        float[] cragX = new float[cragCount];
        float[] cragZ = new float[cragCount];
        float[] cragH = new float[cragCount];
        float[] cragW = new float[cragCount];
        float[] cragRot = new float[cragCount];
        float[] cragLean = new float[cragCount];
        float[] veinTipX = new float[cragCount];
        float[] veinTipY = new float[cragCount];
        float[] veinTipZ = new float[cragCount];
        float[] veinW = new float[cragCount];
        for (int c = 0; c < cragCount; c++) {
            double baseAngle = (c / (double) cragCount) * Math.PI * 2.0;
            double angleOffset = (rng.nextDouble() - 0.5) * 0.15;
            double angle = baseAngle + angleOffset;

            float rOffset = (rng.nextFloat() - 0.5f) * 0.8f;
            float cragDist = Math.max(0.5f, radius + rOffset);

            float cx = (float) Math.cos(angle) * cragDist;
            float cz = (float) Math.sin(angle) * cragDist;

            float hVar = 0.75f + rng.nextFloat() * 0.5f;
            float cragHeight = cragMaxHeight * hVar * crest;
            float cragWidth = (0.45f + rng.nextFloat() * 0.35f) * waveScale;

            // Outward radial tilt (crags lean violently forward as earth fractures)
            float tiltFactor = 0.35f + rng.nextFloat() * 0.25f;

            cragX[c] = cx;
            cragZ[c] = cz;
            cragH[c] = cragHeight;
            cragW[c] = cragWidth;
            cragRot[c] = (float) -Math.toDegrees(angle);
            cragLean[c] = (float) Math.toDegrees(Math.atan(tiltFactor));
            veinTipX[c] = cx + (float) Math.cos(angle) * (cragHeight * tiltFactor);
            veinTipY[c] = cragHeight * 0.88f;
            veinTipZ[c] = cz + (float) Math.sin(angle) * (cragHeight * tiltFactor);
            veinW[c] = cragWidth * 0.22f;
        }

        // Broken molten teeth along the leading edge: branch spikes leaning
        // outward, narrow and uneven, so the crest stays jagged earth.
        int teeth = FxEnvelope.segments(26, distScale, 10);
        float[] toothFrom = new float[teeth * 3];
        float[] toothTo = new float[teeth * 3];
        float[] toothW = new float[teeth];
        for (int i = 0; i < teeth; i++) {
            double a = (i / (double) teeth) * Math.PI * 2.0 + rng.nextDouble() * 0.08;
            float jag = 0.28f + rng.nextFloat() * 1.25f;
            float h = cragMaxHeight * 0.32f * jag * crest * (0.90f + 0.10f * (float) Math.sin(age * 0.9f + i));
            float dist = Math.max(0.4f, radius * (0.97f + (rng.nextFloat() - 0.5f) * 0.10f));
            float sx = (float) Math.cos(a) * dist;
            float sz = (float) Math.sin(a) * dist;
            float w = (0.06f + rng.nextFloat() * 0.07f) * waveScale;
            float lean = 0.32f;
            toothFrom[i * 3] = sx;
            toothFrom[i * 3 + 1] = 0.05f;
            toothFrom[i * 3 + 2] = sz;
            toothTo[i * 3] = sx + (float) Math.cos(a) * h * lean;
            toothTo[i * 3 + 1] = h;
            toothTo[i * 3 + 2] = sz + (float) Math.sin(a) * h * lean;
            toothW[i] = w * 2.0f;
        }

        // Inner ground shards trailing the crest: shorter branch spikes.
        int shardCount = FxEnvelope.segments(14, distScale, 6);
        float[] shardFrom = new float[shardCount * 3];
        float[] shardTo = new float[shardCount * 3];
        float[] shardW = new float[shardCount];
        for (int s = 0; s < shardCount; s++) {
            double sAngle = (s / (double) shardCount) * Math.PI * 2.0 + rng.nextDouble() * 0.3;
            float sDist = radius * (0.86f + rng.nextFloat() * 0.22f);
            float sx = (float) Math.cos(sAngle) * sDist;
            float sz = (float) Math.sin(sAngle) * sDist;
            float shardH = (0.55f + rng.nextFloat() * 1.05f) * waveScale * crest;
            float sw = 0.12f + rng.nextFloat() * 0.10f;
            float lean = 0.34f;
            shardFrom[s * 3] = sx;
            shardFrom[s * 3 + 1] = 0.05f;
            shardFrom[s * 3 + 2] = sz;
            shardTo[s * 3] = sx + (float) Math.cos(sAngle) * shardH * lean;
            shardTo[s * 3 + 1] = shardH;
            shardTo[s * 3 + 2] = sz + (float) Math.sin(sAngle) * shardH * lean;
            shardW[s] = sw * 1.6f;
        }

        float alphaF = alpha;
        float distAlphaF = distAlpha;
        float distScaleF = distScale;

        // Layered depth behind the crest: a ground anchor glow roots the
        // wave, and two broken vortex bands roll as the dust walls (mid
        // behind the crest, wispy outer trailing), turning at different
        // speeds. Veins, teeth, and shards ride in the same submission.
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            LayeredRender.drawAnchorGlow(pose, buffer, Math.max(1.0f, radius), 0.06f,
                1.0f, 0.55f, 0.10f, LayeredRender.ALPHA_MID * alphaF * distAlphaF, distScaleF);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xD057L,
                radius * 0.94f, 0.05f, 0.05f + cragMaxHeight * 0.55f * crest,
                (float) Math.toRadians(age * 6.0f),
                1.0f, 0.45f, 0.08f, LayeredRender.ALPHA_MID * alphaF * distAlphaF, distScaleF);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x7E57L,
                radius * 1.07f, 0.05f, 0.05f + cragMaxHeight * 0.34f * crest,
                (float) Math.toRadians(-age * 3.5f),
                1.0f, 0.38f, 0.05f, LayeredRender.ALPHA_OUTER * alphaF * distAlphaF, distScaleF);

            for (int c = 0; c < cragCount; c++) {
                BespokeGeometry.branch(pose, buffer, seed ^ (0xBE10L + c * 131L),
                    new Vec3(cragX[c], 0.08f, cragZ[c]),
                    new Vec3(veinTipX[c], Math.max(0.10f, veinTipY[c]), veinTipZ[c]),
                    Math.max(0.03f, veinW[c]),
                    1.0f, 0.92f, 0.28f, alphaF * 0.90f, 1.0f, distScaleF);
            }
            for (int i = 0; i < teeth; i++) {
                BespokeGeometry.branch(pose, buffer, seed ^ (0x7EEL + i * 137L),
                    new Vec3(toothFrom[i * 3], toothFrom[i * 3 + 1], toothFrom[i * 3 + 2]),
                    new Vec3(toothTo[i * 3], Math.max(0.08f, toothTo[i * 3 + 1]), toothTo[i * 3 + 2]),
                    Math.max(0.03f, toothW[i]),
                    1.0f, 0.82f, 0.20f, alphaF * 0.92f, 1.0f, distScaleF);
            }
            for (int s = 0; s < shardCount; s++) {
                BespokeGeometry.branch(pose, buffer, seed ^ (0x5AADL + s * 139L),
                    new Vec3(shardFrom[s * 3], shardFrom[s * 3 + 1], shardFrom[s * 3 + 2]),
                    new Vec3(shardTo[s * 3], Math.max(0.08f, shardTo[s * 3 + 1]), shardTo[s * 3 + 2]),
                    Math.max(0.03f, shardW[s]),
                    1.0f, 0.48f, 0.08f, alphaF * 0.90f, 1.0f, distScaleF);
            }
        });

        // Crag bodies: one submission per crag, posed at its ring position,
        // leaned outward, and stretched from base width to full height.
        for (int c = 0; c < cragCount; c++) {
            float domeRadius = Math.max(0.10f, cragW[c] * 0.5f);
            float heightScale = cragH[c] / domeRadius;
            long cragSeed = seed ^ (0xC9A6L + c * 0x9E37L);
            poseStack.pushPose();
            poseStack.translate(cragX[c], 0.05f, cragZ[c]);
            poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(cragRot[c])));
            poseStack.mulPose(new Matrix4f().rotation(Axis.ZP.rotationDegrees(-cragLean[c])));
            poseStack.scale(1.0f, heightScale, 1.0f);
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.displacedDome(pose, buffer, cragSeed, domeRadius,
                    0.28f, 0.22f, 0.18f, alphaF, 1.0f, distScaleF);
            });
            poseStack.popPose();
        }

        super.submit(state, poseStack, collector, cameraState);
    }
}
