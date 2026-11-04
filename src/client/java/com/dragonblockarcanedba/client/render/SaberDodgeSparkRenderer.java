package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.SaberDodgeSparkEntity;
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

/**
 * Entity Renderer for Saber Dodge Spark in Minecraft 26.2.
 *
 * <p>Bespoke pass: the hand-rolled clash rings, bowed spike quads and
 * diamond sparks are deleted. The rings are {@link BespokeGeometry}
 * broken vortex bands with the same opposed spin rates, the 8 clash
 * spikes and 12 flying shards are jagged branch streaks, and the core
 * flash is a plume, per the bespoke standard's dodge spark mapping. The
 * seeded shard scatter now runs on the toolkit's own Rng fed by the same
 * synced UUID seed. Build-in, growth, quadratic fade and the additive
 * halo are unchanged from the spectacle pass.</p>
 */
public class SaberDodgeSparkRenderer extends EntityRenderer<SaberDodgeSparkEntity, SaberDodgeSparkRenderer.DodgeSparkRenderState> {

    public SaberDodgeSparkRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class DodgeSparkRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public float age = 0;
        public int maxLifetime = 12;
        public long seed = 0;
        public double ex = 0;
        public double ey = 0;
        public double ez = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(SaberDodgeSparkEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(SaberDodgeSparkEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public DodgeSparkRenderState createRenderState() {
        return new DodgeSparkRenderState();
    }

    @Override
    public void extractRenderState(SaberDodgeSparkEntity entity, DodgeSparkRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getSparkScale();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
    }

    @Override
    public void submit(DodgeSparkRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
        if (progress >= 1.0f) return;

        // Build in over the first fifth of life: alpha rides the shared
        // envelope's opening while the old quadratic fade keeps the
        // dissipation shape, and the burst grows from a small core.
        float open = FxEnvelope.smoothstep(Math.min(1.0f, progress / 0.20f));
        float fade = (1.0f - progress * progress) * open;
        float grow = FxEnvelope.smoothstep(Math.min(1.0f, progress / 0.25f));
        float scale = state.scale * (0.55f + 0.85f * grow + progress * 0.35f);
        float ds = LayeredRender.distScale(cameraState, state.ex, state.ey, state.ez);
        long seed = state.seed;
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();

        // Spikes, flying shards and the core plume. The rings live in their
        // own submits below because the bands need a rotated frame.
        SubmitNodeCollector.CustomGeometryRenderer geometry = (pose, buffer) -> {
            // 1. 8-Point Diamond Energy Clash Spikes, as branch streaks:
            // gold body with the thin white core the old quads carried.
            int spikeCount = 8;
            for (int i = 0; i < spikeCount; i++) {
                double angle = (i / (double) spikeCount) * Math.PI * 2.0 + (state.age * 0.15);
                float spikeLen = (1.5f + (i % 2 == 0 ? 0.9f : 0.4f)) * scale;
                float spikeWidth = 0.24f * (1.0f - progress * 0.7f) * scale;
                Vec3 tip = new Vec3(Math.cos(angle) * spikeLen, Math.sin(angle) * spikeLen, 0);
                BespokeGeometry.branch(pose, buffer, seed + i * 131L, Vec3.ZERO, tip,
                    spikeWidth, 1.0f, 0.95f, 0.40f, fade * 0.85f, 1.0f, ds);
                BespokeGeometry.branch(pose, buffer, seed + i * 131L + 3L, Vec3.ZERO, tip,
                    spikeWidth * 0.28f, 1.0f, 1.0f, 1.0f, fade, 1.0f, ds);
            }

            // 2. 12 High-Velocity Flying Spark Shards, as short outward
            // branch streaks. Positions come from the toolkit Rng on the
            // same synced seed, consumed in the same order as before.
            BespokeGeometry.Rng rng = new BespokeGeometry.Rng(seed ^ 0x54A2DL);
            int sparkCount = 12;
            for (int s = 0; s < sparkCount; s++) {
                double sparkAngle = (s / (double) sparkCount) * Math.PI * 2.0 + (rng.nextFloat() * 0.4);
                float speed = 1.3f + rng.nextFloat() * 1.6f;
                float sparkDist = progress * speed * 3.0f * scale;
                float sx = (float) Math.cos(sparkAngle) * sparkDist;
                float sy = (float) Math.sin(sparkAngle) * sparkDist + (rng.nextFloat() - 0.5f) * 0.6f;
                float sz = (rng.nextFloat() - 0.5f) * sparkDist * 0.7f;
                float sparkSize = 0.15f * (1.0f - progress * 0.8f);
                Vec3 from = new Vec3(sx, sy, sz);
                Vec3 to = new Vec3(
                    sx + Math.cos(sparkAngle) * sparkSize * 2.4,
                    sy + Math.sin(sparkAngle) * sparkSize * 2.4, sz);
                BespokeGeometry.branch(pose, buffer, seed + 1000L + s * 17L, from, to,
                    sparkSize * 0.8f, 1.0f, 0.92f, 0.35f, fade, 1.0f, ds);
            }

            // 3. Radiant White-Gold Core Flash, as a plume puff.
            float coreSize = (0.8f * (1.0f - progress * 0.6f)) * scale;
            BespokeGeometry.plume(pose, buffer, seed ^ 0xC03EL, coreSize, coreSize * 0.9f,
                1.0f, 1.0f, 0.95f, fade, 1.0f, ds);
        };

        // The burst is built in one plane and nearly vanishes edge-on, so the
        // same geometry is submitted a second time rotated 90 degrees, giving
        // the clash volume from any viewing angle.
        collector.submitCustomGeometry(poseStack, renderType, geometry);
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotationY((float) Math.PI / 2.0f));
        collector.submitCustomGeometry(poseStack, renderType, geometry);
        poseStack.popPose();

        // Clash rings as broken vortex bands. The bands circle the Y axis
        // in their own frame; the X rotation lays them into the burst's XY
        // plane, and the twist arguments carry the old spin rates (age * 35
        // and age * 22 one way, age * -50 against). Radii are the means of
        // the old rings' inner and outer radii.
        SubmitNodeCollector.CustomGeometryRenderer rings = (pose, buffer) -> {
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xBADD1L,
                1.55f * scale, 0.0f, 0.12f * scale, (float) Math.toRadians(state.age * 35.0),
                1.0f, 0.85f, 0.15f, fade * 0.90f, ds);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xBADD2L,
                1.10f * scale, 0.0f, 0.12f * scale, (float) Math.toRadians(-state.age * 50.0),
                1.0f, 0.98f, 0.70f, fade * 0.98f, ds);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xBADD3L,
                0.34f * scale, 0.0f, 0.08f * scale, (float) Math.toRadians(state.age * 22.0),
                1.0f, 1.0f, 1.0f, fade, ds);
        };
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90f)));
        collector.submitCustomGeometry(poseStack, renderType, rings);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(90f)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90f)));
        collector.submitCustomGeometry(poseStack, renderType, rings);
        poseStack.popPose();

        // Additive halo pass: an oversized faint copy of the core plume and
        // one clash band, so the impact blooms.
        float haloCore = (0.8f * (1.0f - progress * 0.6f)) * scale * 1.6f;
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) ->
            BespokeGeometry.plume(pose, buffer, seed ^ 0x4A10L, haloCore, haloCore * 0.9f,
                1.0f, 1.0f, 0.95f, fade * 0.22f, 1.0f, ds));
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90f)));
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) ->
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xBEEFL,
                1.76f * scale, 0.0f, 0.12f * scale, (float) Math.toRadians(state.age * 35.0),
                1.0f, 0.85f, 0.15f, fade * 0.14f, ds));
        poseStack.popPose();

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
