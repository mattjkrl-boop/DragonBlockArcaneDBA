package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.GrandClashSparkEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Grand Clash Spark in Minecraft 26.2.
 * Bespoke pass: the deflection burst is built only from BespokeGeometry
 * builders, seeded from the entity UUID so every client draws the same
 * silhouette (the old per-frame java.util.Random is gone).
 * - A plume core flash in white-gold
 * - Two shardFan spike stars, one flat and one standing, for 3D volume
 * - Two brokenVortexBand shock rings twisting against each other
 * - An outer shardFan whose radius grows with the burst progress
 */
public class GrandClashSparkRenderer extends EntityRenderer<GrandClashSparkEntity, GrandClashSparkRenderer.ClashRenderState> {

    public GrandClashSparkRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ClashRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public float age = 0;
        public int maxLifetime = 10;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(GrandClashSparkEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(GrandClashSparkEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ClashRenderState createRenderState() {
        return new ClashRenderState();
    }

    @Override
    public void extractRenderState(GrandClashSparkEntity entity, ClashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getClashScale();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(ClashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
        // Shared envelope: builds over the first 20 percent of the 24 tick
        // life, peaks, then dissipates over the last 45 percent. The old
        // 10 tick version sat at full alpha on tick one and only faded.
        float fade = com.dragonblockarcanedba.util.FxEnvelope.envelope(
            state.age, state.maxLifetime, 0.20f, 0.45f);
        if (fade <= 0.01f) return;
        float grow = com.dragonblockarcanedba.util.FxEnvelope.smoothstep(
            Math.min(1.0f, state.age / (state.maxLifetime * 0.20f)));
        float scale = state.scale * (0.55f + grow * 1.25f);
        float distScale = com.dragonblockarcanedba.client.render.ki.LayeredRender.distScale(
            cameraState, state.x, state.y, state.z);
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();

        SubmitNodeCollector.CustomGeometryRenderer geometry = (pose, buffer) -> {
            // 1. Core flash: a white-gold plume stack.
            BespokeGeometry.plume(pose, buffer, state.seed, 0.85f * scale, 1.1f * scale,
                1.0f, 1.0f, 0.95f, fade, 1.0f, distScale);

            // 2. Spike star lying flat, plus a standing copy for volume.
            BespokeGeometry.shardFan(pose, buffer, state.seed ^ 0x5011L, 2.2f * scale,
                1.0f, 0.95f, 0.40f, fade * 0.9f, 1.0f, distScale);
            PoseStack standing = new PoseStack();
            standing.last().pose().set(pose.pose());
            standing.last().normal().set(pose.normal());
            standing.mulPose(new org.joml.Matrix4f().rotation(
                com.mojang.math.Axis.XP.rotationDegrees(90.0f)));
            BespokeGeometry.shardFan(standing.last(), buffer, state.seed ^ 0x5022L, 1.9f * scale,
                1.0f, 0.98f, 0.80f, fade * 0.8f, 1.0f, distScale);

            // 3. Dual counter-twisting broken shock rings.
            BespokeGeometry.brokenVortexBand(pose, buffer, state.seed ^ 0xB101L,
                1.6f * scale, -0.12f * scale, 0.22f * scale, state.age * 0.5f,
                1.0f, 0.85f, 0.15f, fade * 0.85f, distScale);
            BespokeGeometry.brokenVortexBand(pose, buffer, state.seed ^ 0xB202L,
                1.2f * scale, -0.08f * scale, 0.16f * scale, -state.age * 0.8f,
                1.0f, 0.98f, 0.70f, fade * 0.95f, distScale);

            // 4. Outer flying shards: a fan whose reach grows with progress
            // (scale breathing, the silhouette itself stays seeded and still).
            BespokeGeometry.shardFan(pose, buffer, state.seed ^ 0xF111L,
                (1.3f + progress * 2.2f) * scale,
                1.0f, 0.90f, 0.30f, fade * 0.8f, 1.0f, distScale);
        };

        // The burst reads best with volume from any angle: submit once flat,
        // then again rotated 90 degrees around X so a second star stands up.
        collector.submitCustomGeometry(poseStack, renderType, geometry);
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotationX((float) Math.PI / 2.0f));
        collector.submitCustomGeometry(poseStack, renderType, geometry);
        poseStack.popPose();

        // Additive halo pass (light touch for a short burst): an oversized
        // faint plume and one wide broken ring so the deflection blooms.
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            BespokeGeometry.plume(pose, buffer, state.seed ^ 0xAA10L,
                1.35f * scale, 1.7f * scale,
                1.0f, 1.0f, 0.95f, fade * 0.22f, 1.0f, distScale);
            BespokeGeometry.brokenVortexBand(pose, buffer, state.seed ^ 0xAA20L,
                1.84f * scale, -0.15f * scale, 0.25f * scale, state.age * 0.5f,
                1.0f, 0.85f, 0.15f, fade * 0.14f, distScale);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
