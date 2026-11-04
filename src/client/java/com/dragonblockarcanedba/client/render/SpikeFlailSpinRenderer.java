package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.SpikeFlailSpinEntity;
import com.dragonblockarcanedba.item.SpikeFlailItem;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;

/**
 * Orbiting ball, then the outward crack when the channel lets go.
 * Geometry is all BespokeGeometry (BESPOKE-STANDARD rule 1): broken vortex
 * bands at the orbit radius, crack stars for the crack marks, and the shared
 * chain and ball from SpikeFlailSwingRenderer. Seeds derive from the entity
 * id, so every client draws the same shapes.
 */
public class SpikeFlailSpinRenderer extends EntityRenderer<SpikeFlailSpinEntity, SpikeFlailSpinRenderer.SpinState> {
    public SpikeFlailSpinRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SpinState extends EntityRenderState {
        public float angle;
        public float radius;
        public boolean releasing;
        public int releaseAge;
        public float age;
        public float charge;
        public long seed;
    }

    public boolean shouldRender(SpikeFlailSpinEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(SpikeFlailSpinEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public SpinState createRenderState() {
        return new SpinState();
    }

    @Override
    public void extractRenderState(SpikeFlailSpinEntity entity, SpinState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.angle = entity.angle();
        state.radius = entity.orbitRadius();
        state.releasing = entity.releasing();
        state.releaseAge = entity.releaseAge();
        state.age = entity.tickCount + partialTick;
        state.charge = entity.charge();
        state.seed = entity.getId() * 2654435761L ^ 0x5F1A11L;
    }

    @Override
    public void submit(SpinState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        // Orbit holds full presence; release holds through the hit, then fades over the settle.
        float fade = 1.0f;
        if (state.releasing) {
            float settleT = Mth.clamp((state.releaseAge - SpikeFlailSpinEntity.RELEASE_TICKS)
                / (float) SpikeFlailSpinEntity.SETTLE_TICKS, 0.0f, 1.0f);
            fade = 1.0f - FxEnvelope.smoothstep(settleT);
        }
        double rad = Math.toRadians(state.angle);
        float bx = (float) (Math.cos(rad) * state.radius);
        float bz = (float) (Math.sin(rad) * state.radius);
        float by = 1.15f;
        RenderType type = KiRenderHelper.kiRenderType();
        float ballX = bx;
        float ballY = by;
        float ballZ = bz;
        float distScale = LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        float crackFade = fade;
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            if (!state.releasing) {
                // Broken bespoke bands at the orbit radius: a bright core, a theme
                // mid band riding the ball's path, and a wispy outer band, counter
                // rotating under the same envelope the particles use. Plus an
                // anchor glow grounding the spin to the floor.
                float env = FxEnvelope.envelope(state.age, SpikeFlailItem.CHANNEL_TICKS);
                LayeredRender.drawAnchorGlow(pose, buffer, state.radius * 0.95f, 0.07f,
                    0.85f, 0.80f, 0.68f, 0.30f * env, distScale);
                BespokeGeometry.brokenVortexBand(pose, buffer, state.seed, state.radius * 0.27f, 0.0f, 1.55f,
                    (float) Math.toRadians(state.age * 2.6f),
                    1.0f, 1.0f, 1.0f, LayeredRender.ALPHA_CORE * env, distScale);
                BespokeGeometry.brokenVortexBand(pose, buffer, state.seed ^ 0x1F123BB5L, state.radius * 0.70f, 0.0f, 1.55f,
                    (float) Math.toRadians(-state.age * 1.7f),
                    0.78f, 0.74f, 0.66f, LayeredRender.ALPHA_MID * env, distScale);
                BespokeGeometry.brokenVortexBand(pose, buffer, state.seed ^ 0x5F3759DFL, state.radius * 1.02f, 0.0f, 1.71f,
                    (float) Math.toRadians(state.age * 0.9f),
                    0.78f, 0.74f, 0.66f, LayeredRender.ALPHA_OUTER * env, distScale);
            }
            if (state.releasing) {
                // Crack marks: seeded crack stars grow with the expanding disc,
                // a hot full-radius layer over a dimmer inner layer, grounded by
                // the anchor glow.
                float crackAlpha = crackFade * (0.55f + 0.45f * state.charge);
                LayeredRender.drawAnchorGlow(pose, buffer, state.radius, 0.06f,
                    1.0f, 0.95f, 0.8f, 0.25f * crackAlpha, distScale);
                BespokeGeometry.crackStar(pose, buffer, state.seed ^ 0x27D4EB2FL, state.radius,
                    1.0f, 0.95f, 0.8f, crackAlpha, 1.0f, distScale);
                BespokeGeometry.crackStar(pose, buffer, state.seed ^ 0x165667B1L, state.radius * 0.66f,
                    0.85f, 0.80f, 0.68f, crackAlpha * 0.7f, 1.0f, distScale);
            }
        });
        SpikeFlailSwingRenderer.submitChain(poseStack, collector, type, 0.25f, 1.0f, 0.15f,
            ballX, ballY, ballZ, fade, state.seed ^ 0xC4A1L, distScale);
        poseStack.pushPose();
        poseStack.translate(ballX, ballY, ballZ);
        SpikeFlailSwingRenderer.submitBall(poseStack, collector, type, 0.4f, fade,
            state.seed ^ 0xBA11L, distScale);
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
