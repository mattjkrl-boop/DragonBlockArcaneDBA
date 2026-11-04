package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.KatanaChargeEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Katana Flashdraw Charge-up in Minecraft 26.2.
 * Renders physical 3D iaido drawing stance ground focus rings (broken
 * vortex bands hugging the ground, from the shared BespokeGeometry
 * toolkit), three counter rotating layered vortex bands around the stance
 * (LayeredRender), a sheathed hilt spark fan, and converging crystalline
 * silver motes as small shard fans spiraling into the hilt. Alpha follows
 * the shared lifecycle shape: a smoothstep fade in, and a fade out over the
 * entity's FADE_TICKS once the charge ends or is cancelled. All geometry is
 * seeded from the entity UUID (BESPOKE-STANDARD rule 1); no local draw
 * helpers remain.
 */
public class KatanaChargeRenderer extends EntityRenderer<KatanaChargeEntity, KatanaChargeRenderer.KatanaChargeRenderState> {

    public KatanaChargeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class KatanaChargeRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public long seed = 0;
        public int fadeStart = -1;
        public double wx = 0, wy = 0, wz = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(KatanaChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(KatanaChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public KatanaChargeRenderState createRenderState() {
        return new KatanaChargeRenderState();
    }

    @Override
    public void extractRenderState(KatanaChargeEntity entity, KatanaChargeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
        state.fadeStart = entity.getFadeStart();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    @Override
    public void submit(KatanaChargeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        float charge = state.chargeRatio;
        float age = state.age;
        float pulse = 0.85f + 0.15f * (float) Math.sin(age * 0.4f);
        // Lifecycle: smoothstep fade in over the first 4 ticks, and a fade out
        // over FADE_TICKS once the charge ends, so the stance never pops.
        float alpha = (0.65f + (charge * 0.35f)) * FxEnvelope.smoothstep(Math.min(1.0f, age / 4.0f));
        if (state.fadeStart >= 0) {
            float fadeT = (age - state.fadeStart) / (float) KatanaChargeEntity.FADE_TICKS;
            if (fadeT >= 1.0f) return;
            alpha *= 1.0f - FxEnvelope.smoothstep(fadeT);
        }
        final float alphaF = alpha;
        float dist = LayeredRender.distScale(cameraState, state.wx, state.wy, state.wz);
        long seed = state.seed;

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));

        // 1. Planar ground iaido focus rings: three broken vortex bands lying
        // almost flat at ground level, counter rotating through the builder's
        // twist parameter (outer electric cyan, inner silver-white, white
        // core), replacing the old equal-segment ring loops.
        float rOuter = 1.3f + (charge * 0.4f);
        float coreR = rOuter * (0.22f + charge * 0.08f);
        float twistOuter = (float) Math.toRadians(age * 25.0f);
        float twistInner = (float) Math.toRadians(-age * 35.0f);
        float twistCore = (float) Math.toRadians(age * 48.0f);
        float ringAlphaOuter = alphaF * 0.75f * pulse;
        float ringAlphaInner = alphaF * 0.90f * pulse;
        float ringAlphaCore = alphaF * pulse;
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.brokenVortexBand(pose, buffer, seed, rOuter, 0.02f, 0.10f, twistOuter,
                0.15f, 0.85f, 1.0f, ringAlphaOuter, dist);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x1111L, rOuter * 0.65f, 0.04f, 0.12f, twistInner,
                0.90f, 0.95f, 1.0f, ringAlphaInner, dist);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x2222L, coreR, 0.06f, 0.16f, twistCore,
                1.0f, 1.0f, 1.0f, ringAlphaCore, dist);
        });

        // The ground arc accent in front of the stance: two nested blade
        // arcs (silver over white) bowed toward +Z, the builder bulge turned
        // forward by a -90 degree yaw, offset so the band sits where the old
        // parabola strip lay.
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.06f, -0.55f);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.bladeArc(pose, buffer, seed ^ 0x3333L, 1.3f, 80.0f,
                0.86f, 0.95f, 1.0f, 0.8f * pulse, alphaF, dist);
            BespokeGeometry.bladeArc(pose, buffer, seed ^ 0x4444L, 1.12f, 80.0f,
                1.0f, 1.0f, 1.0f, 1.0f * pulse, alphaF * 0.9f, dist);
        });
        poseStack.popPose();

        // 2. Sheathed hilt spark matrix (left hip drawing position): two
        // shard fans stood into the XY plane, white over cyan.
        float hiltX = -0.28f;
        float hiltY = 0.75f;
        float hiltZ = -0.05f;
        float sparkSize = (0.12f + charge * 0.10f) * pulse;
        poseStack.pushPose();
        poseStack.translate(hiltX, hiltY, hiltZ);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.shardFan(pose, buffer, seed ^ 0x5555L, sparkSize * 1.8f,
                1.0f, 1.0f, 1.0f, 1.0f, alphaF, dist);
            BespokeGeometry.shardFan(pose, buffer, seed ^ 0x6666L, sparkSize * 1.1f,
                0.2f, 0.9f, 1.0f, 0.8f, alphaF, dist);
        });
        poseStack.popPose();

        // 3. Converging inward crystalline silver motes: small shard fans on
        // a golden angle spiral, drifting into the hilt. Positions and the
        // height jitter come from the seeded toolkit stream, so every client
        // sees the same spiral.
        int moteCount = Math.max(4, Math.round((10 + (int) (charge * 8)) * dist));
        BespokeGeometry.Rng rng = new BespokeGeometry.Rng(seed);
        float[][] motes = new float[moteCount][4];
        for (int m = 0; m < moteCount; m++) {
            float moteProg = (age * 0.06f + (m / (float) moteCount)) % 1.0f;
            float inwardProg = 1.0f - moteProg; // Moves from outside in towards hilt
            double ang = (m * 2.39996) + (age * 0.08f); // Golden ratio angle spread
            float moteDist = 0.2f + inwardProg * (1.2f + charge * 0.5f);
            motes[m][0] = hiltX + (float) Math.cos(ang) * moteDist;
            motes[m][1] = hiltY + ((rng.nextFloat() - 0.5f) * 0.6f * inwardProg);
            motes[m][2] = hiltZ + (float) Math.sin(ang) * moteDist;
            motes[m][3] = moteProg;
        }
        for (int m = 0; m < moteCount; m++) {
            float moteProg = motes[m][3];
            float mSize = 0.04f * (0.4f + moteProg * 0.6f);
            float mAlpha = alphaF * moteProg;
            long moteSeed = seed ^ (0x7700L + m);
            poseStack.pushPose();
            poseStack.translate(motes[m][0], motes[m][1], motes[m][2]);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.shardFan(pose, buffer, moteSeed, mSize * 2.2f,
                    0.8f, 0.95f, 1.0f, 0.9f, mAlpha, dist);
            });
            poseStack.popPose();
        }

        // Layered vortex bands around the stance (standard point 1): a tight
        // bright core, a cyan mid band, and a wispy outer band, counter
        // rotating at different speeds, giving the charge real vertical depth.
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            LayeredRender.drawVortexBands(pose, buffer, 0.85f + charge * 0.3f, 1.2f + charge * 0.5f, age,
                0.15f, 0.85f, 1.0f, alphaF * 0.85f, dist);
        });

        // Additive halo pass: a wider, faint copy of the outer cyan ring and
        // the hilt spark so the charge blooms and anchors to the ground
        // instead of reading matte.
        float haloRadius = (1.3f + (charge * 0.4f)) * 1.15f;
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            BespokeGeometry.brokenVortexBand(pose, buffer, seed, haloRadius, 0.02f, 0.10f, twistOuter,
                0.15f, 0.85f, 1.0f, 0.16f * alphaF, dist);
        });
        poseStack.pushPose();
        poseStack.translate(hiltX, hiltY, hiltZ);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        float haloSparkRadius = (0.12f + charge * 0.10f) * 1.7f * 1.8f;
        float haloSparkAlpha = 0.20f * alphaF;
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            BespokeGeometry.shardFan(pose, buffer, seed ^ 0x5555L, haloSparkRadius,
                1.0f, 1.0f, 1.0f, 1.0f, haloSparkAlpha, dist);
        });
        poseStack.popPose();

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
