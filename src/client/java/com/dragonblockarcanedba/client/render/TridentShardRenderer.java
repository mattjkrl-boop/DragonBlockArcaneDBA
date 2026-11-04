package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.TridentShardEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;

/**
 * Entity Renderer for Trident Shard in Minecraft 26.2.
 * Bespoke pass: the shard head is a pair of crossed crescent blades built
 * with BespokeGeometry.bladeArc (one vertical, one horizontal, each with a
 * hotter inner arc), the collar barbs are a shardFan burst, and the wake
 * is two tornRibbons trailing behind. Seeds come from the entity UUID, so
 * every client draws the same shard. No local geometry helpers remain.
 */
public class TridentShardRenderer extends EntityRenderer<TridentShardEntity, TridentShardRenderer.ShardRenderState> {

    public TridentShardRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ShardRenderState extends EntityRenderState {
        public float ageInTicks = 0;
        public float yRot = 0;
        public float xRot = 0;
        /** Stable per shard size factor, 0.80 to 1.16 in five steps, from the entity id. */
        public float sizeVar = 1.0f;
        /** Synced shape seed from the entity UUID. */
        public long seed = 0;
    }

    @Override
    public ShardRenderState createRenderState() {
        return new ShardRenderState();
    }

    @Override
    public void extractRenderState(TridentShardEntity entity, ShardRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.ageInTicks = entity.tickCount + partialTicks;
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        int step = Math.floorMod(entity.getUUID().hashCode(), 5);
        state.sizeVar = 0.80f + step * 0.09f;
        state.seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();
    }

    @Override
    public void submit(ShardRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        final float segScale = com.dragonblockarcanedba.client.render.ki.LayeredRender.distScale(
            cameraState, state.x, state.y, state.z);
        final long seed = state.seed;
        final float pulse = 0.85f + 0.15f * Mth.sin(state.ageInTicks * 0.35f);

        poseStack.pushPose();
        // Orient with flight/orbit direction
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));
        // High-speed axial roll
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(state.ageInTicks * 22.0f)));
        // Whole blade pulse and spawn scale-in: the blade breathes as one
        // piece and grows in over the first 5 ticks.
        float bodyPulse = 0.93f + 0.07f * Mth.sin(state.ageInTicks * 0.35f);
        float spawnScale = 0.5f + 0.5f * Math.min(1.0f, state.ageInTicks / 5.0f);
        float shardScale = 0.85f * state.sizeVar * bodyPulse * spawnScale;
        poseStack.scale(shardScale, shardScale, shardScale);

        // The crescent head: dark shell arcs with a hotter core, crossed so
        // the blade reads from every angle.
        submitBlade(poseStack, collector, renderType, seed, 1.0f,
            0.34f, 0.02f, 0.04f, 0.96f,
            0.95f, 0.22f, 0.07f, 0.9f * pulse,
            0.92f, segScale);

        // Collar barbs: a jagged shard burst at the base of the head.
        poseStack.pushPose();
        poseStack.translate(0.0, 0.0, -0.2);
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.shardFan(pose, buffer, seed + 5, 0.34f,
                1.0f, 0.25f, 0.1f, 0.9f, 1.0f, segScale);
        });
        poseStack.popPose();

        // Wake: two torn ribbons trailing behind the blade, a wide wispy
        // outer and a thin bright core, replacing the old helical strands.
        poseStack.pushPose();
        poseStack.translate(0.0, 0.0, -0.22);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.tornRibbon(pose, buffer, seed + 21, 1.25f, 0.3f, 0.06f,
                0.62f, 0.02f, 0.05f, 0.55f, 1.0f, segScale);
            BespokeGeometry.tornRibbon(pose, buffer, seed + 27, 1.25f, 0.11f, 0.03f,
                1.0f, 0.2f, 0.08f, 0.8f, 1.0f, segScale);
        });
        poseStack.popPose();

        // Bloom on the same silhouette: the crescents again, additive, at
        // halo alpha on a slightly larger pose.
        final float halo = 0.2f * bodyPulse;
        poseStack.pushPose();
        poseStack.scale(1.14f, 1.14f, 1.08f);
        submitBlade(poseStack, collector, KiRenderHelper.kiAdditiveType(), seed, 1.0f,
            1.0f, 0.3f, 0.12f, halo,
            1.0f, 0.3f, 0.12f, halo,
            0.92f, segScale);
        poseStack.popPose();

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    /**
     * Composition only: poses the shared bladeArc builder into a crossed
     * crescent blade. The first arc is rotated into the vertical plane
     * (bulge along flight +Z, tips above and below), the second stays in
     * the horizontal plane at horizFactor of the radius. Each plane draws
     * a shell arc and a smaller hotter core arc. No geometry math here;
     * every vertex comes from BespokeGeometry.
     */
    static void submitBlade(PoseStack poseStack, SubmitNodeCollector collector, RenderType type,
                                    long seed, float radius,
                                    float sr, float sg, float sb, float shellAlpha,
                                    float cr, float cg, float cb, float coreAlpha,
                                    float horizFactor, float segScale) {
        // Vertical crescent: shift back along the flight axis, then turn the
        // arc's ground plane upright and face its bulge forward.
        poseStack.pushPose();
        poseStack.translate(0.0, 0.0, -0.32);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-90.0f)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            BespokeGeometry.bladeArc(pose, buffer, seed, radius, 150.0f,
                sr, sg, sb, shellAlpha, 1.0f, segScale);
            BespokeGeometry.bladeArc(pose, buffer, seed + 2, radius * 0.78f, 150.0f,
                cr, cg, cb, coreAlpha, 1.0f, segScale);
        });
        poseStack.popPose();

        // Horizontal crescent: same blade lying flat, for body from above
        // and below.
        if (horizFactor > 0.0f) {
            poseStack.pushPose();
            poseStack.translate(0.0, 0.0, -0.32);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-90.0f)));
            collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
                BespokeGeometry.bladeArc(pose, buffer, seed + 4, radius * horizFactor, 150.0f,
                    sr, sg, sb, shellAlpha, 1.0f, segScale);
                BespokeGeometry.bladeArc(pose, buffer, seed + 6, radius * horizFactor * 0.78f, 150.0f,
                    cr, cg, cb, coreAlpha, 1.0f, segScale);
            });
            poseStack.popPose();
        }
    }
}
