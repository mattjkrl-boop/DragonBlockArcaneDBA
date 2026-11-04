package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.PowerPoleImpactEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Power Pole Impact in Minecraft 26.2.
 * Renders physical 3D kinetic impact shockwave & shatter:
 * - Concentric expanding dual shockwave bands (radiant gold & crimson)
 * - Kinetic atmospheric compression shockwave dome
 * - 16 physical shatter fragments flying outwards
 * - Detonation shard fan spikes at the strike focal point
 *
 * Bespoke pass: every shape comes from BespokeGeometry (BESPOKE-STANDARD
 * rule 1). The old gradient ring loops, dome segment loops, needle and
 * diamond shard helpers are gone. The shard scatter seeds from the
 * entity's synced state through BespokeGeometry.Rng instead of a fresh
 * client-side random over the entity UUID.
 */
public class PowerPoleImpactRenderer extends EntityRenderer<PowerPoleImpactEntity, PowerPoleImpactRenderer.ImpactRenderState> {

    public PowerPoleImpactRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ImpactRenderState extends EntityRenderState {
        public float yRot = 0.0f;
        public float xRot = 0.0f;
        public float scale = 2.2f;
        public float age = 0.0f;
        public int maxLifetime = 14;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(PowerPoleImpactEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(PowerPoleImpactEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ImpactRenderState createRenderState() {
        return new ImpactRenderState();
    }

    @Override
    public void extractRenderState(PowerPoleImpactEntity entity, ImpactRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getEntityYaw();
        state.xRot = entity.getEntityPitch();
        state.scale = entity.getImpactScale();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        // Bespoke geometry seed: hashed from the entity's SYNCED state only
        // (scale, orientation), never from client random or wall clock.
        state.seed = (Float.floatToIntBits(entity.getImpactScale()) * 0x9E3779B97F4A7C15L)
            ^ Float.floatToIntBits(entity.getEntityYaw())
            ^ (Float.floatToIntBits(entity.getEntityPitch()) * 31L);
    }

    @Override
    public void submit(ImpactRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float age = state.age;
        float maxLife = (float) state.maxLifetime;
        float lifeT = maxLife <= 0.0f ? 1.0f : Math.min(1.0f, age / maxLife);
        float fade = 1.0f - lifeT * lifeT * (3.0f - 2.0f * lifeT);
        if (fade <= 0.01f) return;

        float scale = state.scale;
        final float finalFade = fade;
        final long seed = state.seed;
        RenderType renderType = KiRenderHelper.kiRenderType();

        // Distance scaling (standard point 1): the burst steps its segment
        // counts down at range inside the builders instead of costing full
        // geometry.
        final float distScale = com.dragonblockarcanedba.client.render.ki.LayeredRender.distScale(
            cameraState, state.x, state.y, state.z);

        // Ease-out cubic expansion: the burst snaps out fast, then settles,
        // instead of the old mechanical linear ramp. Shared by the rings,
        // the dome, the starburst, and the shard flight, as before.
        float rawProgress = Math.min(1.0f, age / 8.0f);
        final float shockProgress = 1.0f - (1.0f - rawProgress) * (1.0f - rawProgress) * (1.0f - rawProgress);
        final float outerR = scale * (0.3f + shockProgress * 1.5f);
        final float starR = scale * (0.8f * (1.0f - shockProgress * 0.4f));
        final float domeR = scale * (0.4f + shockProgress * 1.1f);

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        // Compression dome: a noise-displaced hemisphere bulging toward the
        // struck face (builder +Y turned onto local +Z).
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            com.dragonblockarcanedba.client.render.ki.BespokeGeometry.displacedDome(
                pose, buffer, seed ^ 0xD07EL, domeR,
                1.0f, 0.95f, 0.70f, finalFade * 0.30f, 1.0f, distScale);
        });
        poseStack.popPose();

        // 16 shatter fragments flying outward: the scatter draws come from
        // BespokeGeometry.Rng on the entity seed, in the same order and
        // ranges the old Random draws used (yaw, pitch, speed, size, gold).
        int shardCount = 16;
        com.dragonblockarcanedba.client.render.ki.BespokeGeometry.Rng rng =
            new com.dragonblockarcanedba.client.render.ki.BespokeGeometry.Rng(seed);
        for (int s = 0; s < shardCount; s++) {
            float yawS = rng.nextFloat() * (float) Math.PI * 2.0f;
            float pitchS = (rng.nextFloat() - 0.5f) * (float) Math.PI * 0.8f;
            float speed = 0.18f + rng.nextFloat() * 0.22f;

            float dirX = (float) (Math.cos(yawS) * Math.cos(pitchS));
            float dirY = (float) (Math.sin(yawS) * Math.cos(pitchS));
            float dirZ = (float) (Math.sin(pitchS) * 0.5 + 0.3f);

            float shardDist = speed * shockProgress * 8.0f * scale;
            final float sx = dirX * shardDist;
            final float sy = dirY * shardDist;
            final float sz = dirZ * shardDist;

            final float shardSize = (0.12f + rng.nextFloat() * 0.10f) * scale * (1.0f - (age / maxLife) * 0.3f);
            boolean isGoldShard = rng.nextFloat() < 0.5f;
            final float rCol = isGoldShard ? 1.0f : 0.85f;
            final float gCol = isGoldShard ? 0.85f : 0.08f;
            final float bCol = isGoldShard ? 0.15f : 0.12f;
            final long shardSeed = seed ^ (0x5A4DL + s * 101L);
            poseStack.pushPose();
            poseStack.translate(sx, sy, sz);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                com.dragonblockarcanedba.client.render.ki.BespokeGeometry.shardFan(
                    pose, buffer, shardSeed, shardSize * 2.0f,
                    rCol, gCol, bCol, finalFade * 0.90f, 1.0f, distScale);
            });
            poseStack.popPose();
        }

        // Planar burst (shock bands and starburst fans in the hit plane),
        // drawn twice: once facing the strike and once turned 90 degrees,
        // so the burst has volume from the side, as before. Builder +Y is
        // turned onto local -Z so band heights read as the old ring depths.
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(-90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            drawPlanarBurst(pose, buffer, seed, outerR, starR, finalFade, distScale);
        });
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotationY((float) Math.PI / 2.0f));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(-90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            drawPlanarBurst(pose, buffer, seed, outerR, starR, finalFade, distScale);
        });
        poseStack.popPose();

        poseStack.popPose();
    }

    /**
     * The flat burst in the builder's XZ plane: the gold shock bands, the
     * crimson compression pair, and the starburst shard fans. Orchestration
     * only; every vertex comes from BespokeGeometry.
     */
    private static void drawPlanarBurst(PoseStack.Pose pose, VertexConsumer buffer,
                                        long seed, float outerR, float starR,
                                        float fade, float distScale) {
        // Outer golden shockwave band (the old gradient annulus, read as one
        // torn band at its mid tone).
        com.dragonblockarcanedba.client.render.ki.BespokeGeometry.brokenVortexBand(
            pose, buffer, seed ^ 0xB01L, outerR * 0.86f, 0.0f, 0.03f, 0.0f,
            1.0f, 0.90f, 0.45f, fade * 0.85f, distScale);
        // Bright rim band just outside it, fading like the old outer gradient.
        com.dragonblockarcanedba.client.render.ki.BespokeGeometry.brokenVortexBand(
            pose, buffer, seed ^ 0xB02L, outerR * 1.02f, 0.03f, 0.06f, 0.0f,
            1.0f, 0.95f, 0.60f, fade * 0.45f, distScale);
        // Crimson compression annulus as two torn bands across its width.
        float crimsonR = outerR * 0.55f;
        com.dragonblockarcanedba.client.render.ki.BespokeGeometry.brokenVortexBand(
            pose, buffer, seed ^ 0xB03L, crimsonR * 0.72f, 0.05f, 0.08f, 0.0f,
            0.90f, 0.08f, 0.12f, fade * 0.75f, distScale);
        com.dragonblockarcanedba.client.render.ki.BespokeGeometry.brokenVortexBand(
            pose, buffer, seed ^ 0xB04L, crimsonR * 0.92f, 0.05f, 0.08f, 0.0f,
            0.90f, 0.08f, 0.12f, fade * 0.75f, distScale);
        // Detonation starburst: seeded shard fans, wide gold and tight core.
        com.dragonblockarcanedba.client.render.ki.BespokeGeometry.shardFan(
            pose, buffer, seed ^ 0x57A1L, starR,
            1.0f, 0.90f, 0.30f, fade * 0.85f, 1.0f, distScale);
        com.dragonblockarcanedba.client.render.ki.BespokeGeometry.shardFan(
            pose, buffer, seed ^ 0x57A2L, starR * 0.5f,
            1.0f, 0.98f, 0.85f, fade * 0.85f, 1.0f, distScale);
    }
}
