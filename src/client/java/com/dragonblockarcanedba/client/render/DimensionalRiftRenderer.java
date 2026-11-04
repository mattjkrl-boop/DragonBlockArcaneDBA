package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.DimensionalRiftEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;

/**
 * Entity Renderer for Dimensional Rift in Minecraft 26.2.
 * All geometry is bespoke (BESPOKE-STANDARD rule 1): broken vortex bands for
 * the accretion rings, torn ribbons for the vertical fissures, shard fans for
 * the orbital soul shards, and displaced domes for the singularity core. Every
 * builder seed derives from the entity's synced state, so the silhouette is
 * the same on every client. The three counter rotating accretion rings, the
 * towering tears, the 18 spike character (now two irregular shard fans), and
 * the black hole core are the spectacle pass layout, kept.
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
        public long seed = 0;
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
        // Seed from synced state only (entity id and position arrive in the
        // spawn packet), so every client draws the same bespoke silhouette.
        state.seed = (long) entity.getId() * 2654435761L
            ^ (long) (entity.getX() * 64.0) * 40503L
            ^ (long) (entity.getZ() * 64.0) * 0x9E3779B9L;
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
        final float radius = state.radius;
        final long seed = state.seed;

        float rot = state.age * (state.isCollapsing ? 25.0f : 8.0f);
        float pulse = 0.85f + 0.15f * (float) Math.sin(state.age * 0.25f);

        poseStack.pushPose();

        // Ground work in the base pose: anchor glow, the three accretion rings
        // as broken vortex bands, and the shared layered vortex bands.
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // Ground anchor glow (shared LayeredRender): a soft violet splash
            // that grounds the rift to the world under everything else.
            com.dragonblockarcanedba.client.render.ki.LayeredRender.drawAnchorGlow(
                pose, buffer, radius * 1.05f, 0.04f, 0.75f, 0.20f, 1.0f, 0.50f * fade, distScale);

            // Outer accretion shockwave: a rising broken band wall at the
            // current radius, with a thin bright lip band riding its top edge.
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xA11CEL,
                radius, 0.0f, 0.55f * pulse, 0.0f,
                0.60f, 0.0f, 0.90f, 0.90f * fade, distScale);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xB22DL,
                radius * 1.03f, 0.02f, 0.16f, (float) Math.toRadians(rot * 0.55f),
                1.0f, 0.72f, 0.95f, 0.50f * fade, distScale);

            // Middle accretion ring, counter rotating at 65 percent radius.
            float midRadius = radius * 0.65f;
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xC33EL,
                midRadius, 0.02f, 0.12f, (float) Math.toRadians(-rot * 1.4f),
                0.85f, 0.05f, 0.95f, 0.80f * fade, distScale);

            // Inner compression ring, high speed at 35 percent radius.
            float innerRadius = radius * 0.35f;
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xD44FL,
                innerRadius, 0.02f, 0.10f, (float) Math.toRadians(rot * 2.2f),
                0.20f, 0.90f, 1.0f, 0.85f * fade, distScale);

            // Layered vortex bands (shared LayeredRender): three counter
            // rotating layers around the tears, bright core, violet mid, wispy
            // outer, so the rift body has the standard's depth.
            float vHeight = Math.min(8.0f, 2.5f + (radius * 0.4f)) * pulse;
            com.dragonblockarcanedba.client.render.ki.LayeredRender.drawVortexBands(
                pose, buffer, radius * 0.62f, vHeight, state.age,
                0.62f, 0.05f, 0.95f, fade, distScale);
        });

        // Vertical spatial fissures: three crossed torn ribbons, each a wide
        // radiant sheet with a dark void strip down its middle. The ribbon
        // runs along +X lying flat; rotate it upright (Z +90) and then around
        // Y to the plane's angle.
        float vHeight = Math.min(8.0f, 2.5f + (radius * 0.4f)) * pulse;
        float vWidth = Math.min(1.8f, 0.4f + (radius * 0.1f));
        float[][] tearColors = {
            {0.80f, 0.05f, 0.95f, 1.00f},
            {0.20f, 0.90f, 1.0f, 0.90f},
            {1.00f, 0.10f, 0.70f, 0.90f},
        };
        for (int plane = 0; plane < 3; plane++) {
            final float[] col = tearColors[plane];
            final long planeSeed = seed ^ (0x7E57L + plane * 0x1F123L);
            poseStack.pushPose();
            poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(plane * 60.0f)));
            poseStack.mulPose(new Matrix4f().rotation(Axis.ZP.rotationDegrees(90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.tornRibbon(pose, buffer, planeSeed,
                    vHeight, vWidth * 2.0f, 0.5f,
                    col[0], col[1], col[2], col[3], fade, distScale);
                BespokeGeometry.tornRibbon(pose, buffer, planeSeed ^ 0xC0DEL,
                    vHeight, vWidth * 1.05f, 0.5f,
                    0.05f, 0.0f, 0.10f, 0.95f, fade, distScale);
            });
            poseStack.popPose();
        }

        // Orbital soul shards: two shard fans at the old orbit bands, their
        // irregular spacing keeping the spikes' varied golden angle character
        // instead of 18 identical pyramids on one ring. The fans orbit at the
        // old spikes' angular speed.
        float shardOrbitRadius = radius * (0.90f - 0.58f * pullIn);
        poseStack.pushPose();
        poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(rot * 0.8f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.shardFan(pose, buffer, seed ^ 0x5A9DL,
                shardOrbitRadius * 0.90f, 0.50f, 0.0f, 0.85f, 0.85f, fade, distScale);
            BespokeGeometry.shardFan(pose, buffer, seed ^ 0x6A9DL,
                shardOrbitRadius * 0.68f, 0.95f, 0.2f, 1.0f, 0.95f, fade, distScale);
        });
        poseStack.popPose();

        // Black hole core: a displaced dome pair (dark body), a broken band
        // horizon, and a small white heart dome pair, centered at y 0.5 where
        // the old octahedron sat.
        float coreSize = Math.max(0.55f, 1.15f * pulse) * (1.0f - pullIn * 0.22f);
        poseStack.pushPose();
        poseStack.translate(0, 0.5, 0);
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xC09EL,
                coreSize, 0.02f, 0.0f, 0.05f, 0.98f, fade, distScale);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xB091L,
                coreSize * 1.3f, -0.05f, 0.10f, (float) Math.toRadians(rot * 2.2f),
                0.85f, 0.90f, 1.0f, 0.75f * fade, distScale);
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xBEA7L,
                coreSize * 0.30f, 0.95f, 0.90f, 1.0f, 1.0f, fade, distScale);
        });
        poseStack.popPose();

        // Bottom halves of the core domes, mirrored through the center plane.
        poseStack.pushPose();
        poseStack.translate(0, 0.5, 0);
        poseStack.mulPose(new Matrix4f().scale(1f, -1f, 1f));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xC09EL,
                coreSize, 0.02f, 0.0f, 0.05f, 0.98f, fade, distScale);
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xBEA7L,
                coreSize * 0.30f, 0.95f, 0.90f, 1.0f, 1.0f, fade, distScale);
        });
        poseStack.popPose();

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
