package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.DelayedLaunchEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Telegraph for the Z Sword delayed launch. A violet gravity ring contracts
 * around the slam center as the delay runs down, with gold rune ticks that
 * spin faster near the end.
 *
 * Spectacle pass: the ring used to be hardcoded at 5.0 blocks shrinking to 1.2
 * while the slam that spawned it reached a radius of 18, so the warning covered
 * less than a third of the real area. The ring now starts at the true slam
 * radius (synced from the entity) and contracts to 35 percent of it, over the
 * true delay (40 to 80 ticks, also synced), and a faint anchor disc marks the
 * full footprint for the whole countdown.
 *
 * Bespoke pass: the footprint marker is a seeded crack star at the true
 * slam radius, the contracting rings are flat broken vortex bands, and the
 * rune ticks are small gold shard fans at the cardinal points.
 */
public class DelayedLaunchRenderer extends EntityRenderer<DelayedLaunchEntity, DelayedLaunchRenderer.DelayedLaunchRenderState> {
    public DelayedLaunchRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class DelayedLaunchRenderState extends EntityRenderState {
        public float age = 0;
        public float radius = 10.0f;
        public int delayTicks = 40;
        public long seed = 0;
    }

    @Override
    public DelayedLaunchRenderState createRenderState() {
        return new DelayedLaunchRenderState();
    }

    @Override
    public void extractRenderState(DelayedLaunchEntity entity, DelayedLaunchRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.age = entity.tickCount + partialTicks;
        state.radius = entity.getRadius();
        state.delayTicks = Math.max(1, entity.getDelayTicks());
        state.seed = entity.getSeed();
    }

    @Override
    public void submit(DelayedLaunchRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        float age = state.age;
        float progress = Math.min(1.0f, age / (float) state.delayTicks);
        float slamRadius = state.radius;

        // Contracting ring: starts at the real slam radius, closing to 35
        // percent of it at the launch tick.
        float radius = slamRadius * (1.0f - progress * 0.65f);
        float pulse = 0.60f + 0.40f * (float) Math.sin(age * (0.35 + progress * 0.85));
        float ringAlpha = (0.35f + progress * 0.45f) * pulse;
        float rot = age * (6.0f + progress * 30.0f);
        float dist = LayeredRender.distScale(cameraState, state.x, state.y, state.z);

        // Full footprint marker at the true slam radius for the whole
        // countdown, faint, so the warning size equals the hit size.
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(rot * 0.5f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.crackStar(pose, buffer, state.seed, slamRadius,
                0.58f, 0.10f, 0.98f, 1.0f, ringAlpha * 0.30f, dist));
        poseStack.popPose();

        // Contracting violet band and the tighter gold band inside it.
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.brokenVortexBand(pose, buffer, state.seed ^ 0xC7L, radius,
                0.02f, 0.16f, (float) Math.toRadians(rot), 0.58f, 0.10f, 0.98f, ringAlpha, dist);
            BespokeGeometry.brokenVortexBand(pose, buffer, state.seed ^ 0x601DL, radius * 0.62f,
                0.02f, 0.12f, (float) Math.toRadians(-rot * 0.7f), 1.0f, 0.85f, 0.30f, ringAlpha * 0.9f, dist);
        });

        // Four rune ticks at the cardinal points, rotating as the launch nears.
        for (int i = 0; i < 4; i++) {
            double a = Math.toRadians(rot + i * 90.0);
            float cx = (float) Math.cos(a) * radius;
            float cz = (float) Math.sin(a) * radius;
            long tickSeed = state.seed + i * 7L;
            poseStack.pushPose();
            poseStack.translate(cx, 0.10f, cz);
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                BespokeGeometry.shardFan(pose, buffer, tickSeed, 0.55f,
                    1.0f, 0.95f, 0.55f, Math.min(1.0f, ringAlpha + 0.25f), 1.0f, dist));
            poseStack.popPose();
        }

        // Additive halo pass: a wider faint violet glow fan under the
        // telegraph so the warning glows against the ground as the launch
        // nears, plus the shared anchor glow across the full slam footprint.
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            BespokeGeometry.glowFan(pose, buffer, state.seed ^ 0xAA10L, radius * 1.12f, 0.04f,
                0.58f, 0.10f, 0.98f, ringAlpha * 0.35f, dist);
            LayeredRender.drawAnchorGlow(pose, buffer,
                slamRadius, 0.03f, 0.58f, 0.10f, 0.98f, ringAlpha * 0.22f, dist);
        });
        super.submit(state, poseStack, collector, cameraState);
    }
}
