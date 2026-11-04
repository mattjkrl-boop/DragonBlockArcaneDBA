package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.BlasterBoltEntity;
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
 * Entity Renderer for Blaster Bolt in MC 26.2.
 * Bespoke pass: the regular 12-sided plasma tube, cone, and hand-rolled
 * lash fins are gone. The bolt is now built from BespokeGeometry pieces,
 * seeded from the bolt entity's synced seed:
 * - The body is crossed torn ribbons in three planes and three layers
 *   (soft edge sheath, heat-colored body, white-hot core), running from
 *   just behind the nose back to a torn point, spinning at the old
 *   body rate.
 * - The nose cone is a shard fan flare across the flight axis.
 * - The four stabilizer fins are jagged branch ribbons swept from the
 *   nose out and back, each with a thin white core pass, at the old
 *   fin positions and spin.
 * - A counter-spinning shard fan closes the tail wake.
 * Size, heat palette, pulse, and the impact collapse math are unchanged
 * from the spectacle pass; the collapse feeds the ribbon lengths and
 * the shared die envelope.
 */
public class BlasterBoltRenderer extends EntityRenderer<BlasterBoltEntity, BlasterBoltRenderer.BlasterBoltRenderState> {
    public BlasterBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class BlasterBoltRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public boolean isOvercharged = false;
        public float heatRatio = 0.0f;
        public float age = 0.0f;
        public int impactAge = -1;
        public int seed = 0;
        public boolean isFirstPersonOwner = false;
    }

    @Override
    public BlasterBoltRenderState createRenderState() {
        return new BlasterBoltRenderState();
    }

    @Override
    public void extractRenderState(BlasterBoltEntity entity, BlasterBoltRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.isOvercharged = entity.isOvercharged();
        state.heatRatio = entity.getHeatRatio();
        state.age = entity.tickCount + partialTicks;
        state.impactAge = entity.getImpactAge();
        state.seed = entity.getSeed();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null &&
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) &&
            mc.options.getCameraType().isFirstPerson());
    }

    @Override
    public void submit(BlasterBoltRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        float age = state.age;
        float pulse = 1.0f + 0.08f * (float) Math.sin(age * 2.5f);

        boolean overcharged = state.isOvercharged;
        float heat = state.heatRatio;

        float fpScale = state.isFirstPersonOwner ? 0.60f : 1.0f;
        float radius = (overcharged ? 0.36f * pulse : (0.18f + heat * 0.12f) * pulse) * fpScale;
        float headLength = (overcharged ? 1.4f : (0.85f + heat * 0.35f)) * fpScale;
        float tailLength = (overcharged ? 3.4f : (2.0f + heat * 1.0f)) * fpScale;

        // Color palette based on heat / overcharge
        float r, g, b;
        if (overcharged) {
            r = 1.0f;
            g = 0.12f;
            b = 0.32f;
        } else if (heat > 0.6f) {
            r = 1.0f;
            g = 0.45f;
            b = 0.05f;
        } else {
            r = 1.0f;
            g = 0.88f - heat * 0.30f;
            b = 0.10f;
        }

        // On impact the same bolt collapses and fades. It does not swap for a disc.
        final float impactT = state.impactAge < 0 ? 0.0f : Math.min(1.0f, state.impactAge / 5.0f);
        final float impactEase = 1.0f - (1.0f - impactT) * (1.0f - impactT) * (1.0f - impactT);
        final float boltDie = 1.0f - impactT * impactT * (3.0f - 2.0f * impactT);
        final float drawRadius = radius * (1.0f - impactEase * 0.62f);
        final float drawHead = headLength * (1.0f - impactEase * 0.70f);
        final float drawTail = tailLength * (1.0f - impactEase * 0.20f);

        double camDist = cameraState.pos.distanceTo(new Vec3(state.x, state.y, state.z));
        final float distScale = com.dragonblockarcanedba.util.FxEnvelope.distScale(camDist);
        final long seed = state.seed;
        final float spin = age * 20.0f;

        // Body: crossed torn ribbons in three planes, three layers each.
        // The ribbons start just behind the nose and tear back to a point,
        // so the bolt keeps the old extent: nose at +drawHead (covered by
        // the shard flare below), widest at the origin, gone by -drawTail.
        for (int p = 0; p < 3; p++) {
            float planeDeg = spin + p * 60.0f;
            ribbon(poseStack, collector, renderType, seed ^ 0x50F7L,
                drawHead * 0.34f, planeDeg, drawHead * 0.66f + drawTail * 0.85f, drawRadius * 2.44f, 0.06f,
                r, g, b, 0.22f, boltDie, distScale);
            ribbon(poseStack, collector, renderType, seed ^ 0xB0D1L,
                drawHead * 0.40f, planeDeg, drawHead * 0.60f + drawTail, drawRadius * 2.0f, 0.10f,
                r, g, b, 0.88f, boltDie, distScale);
            ribbon(poseStack, collector, renderType, seed ^ 0xC02EL,
                drawHead * 0.34f, planeDeg, drawHead * 0.51f + drawTail * 0.55f, drawRadius * 0.76f, 0.04f,
                1.0f, 0.97f, 0.82f, 1.0f, boltDie, distScale);
        }

        // Nose flare: a shard fan across the flight axis where the cone was.
        axisFan(poseStack, collector, renderType, drawHead * 0.55f, spin + 15.0f, (pose, buffer) ->
            BespokeGeometry.shardFan(pose, buffer, seed ^ 0x905EL, drawRadius * 1.35f,
                r, g, b, 0.90f, boltDie, distScale));

        // Stabilizer fins: branch ribbons swept from the nose out to the
        // old fin span and back toward the tail, edge color outside and a
        // thin white core pass inside, spinning at the old fin rate.
        final float finSpan = drawRadius * 2.3f;
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            for (int f = 0; f < 4; f++) {
                double angle = (f / 4.0) * Math.PI * 2.0 + Math.toRadians(age * 12.0f);
                float ox = (float) Math.cos(angle);
                float oy = (float) Math.sin(angle);
                Vec3 p0 = new Vec3(ox * drawRadius * 0.25f, oy * drawRadius * 0.25f, drawHead * 0.28f);
                Vec3 p1 = new Vec3(ox * finSpan * 0.92f, oy * finSpan * 0.92f, -drawHead * 0.2f);
                Vec3 p2 = new Vec3(ox * finSpan * 0.38f, oy * finSpan * 0.38f, -drawTail * 0.55f);
                long finSeed = seed ^ (0xF100L + f * 37L);
                BespokeGeometry.branch(pose, buffer, finSeed, p0, p1,
                    drawRadius * 0.30f, r, g, b, 0.88f, boltDie, distScale);
                BespokeGeometry.branch(pose, buffer, finSeed ^ 0x21L, p1, p2,
                    drawRadius * 0.30f, r, g, b, 0.88f, boltDie, distScale);
                BespokeGeometry.branch(pose, buffer, finSeed ^ 0x22L, p0, p1,
                    drawRadius * 0.10f, 1.0f, 0.96f, 0.78f, 1.0f, boltDie, distScale);
                BespokeGeometry.branch(pose, buffer, finSeed ^ 0x23L, p1, p2,
                    drawRadius * 0.10f, 1.0f, 0.96f, 0.78f, 1.0f, boltDie, distScale);
            }
        });

        // Tail wake: a counter-spinning shard fan near the torn tail end.
        axisFan(poseStack, collector, renderType, -drawTail * 0.82f, -age * 30.0f, (pose, buffer) ->
            BespokeGeometry.shardFan(pose, buffer, seed ^ 0x7A11L, drawRadius * 1.05f,
                r, g, b, 0.50f, boltDie, distScale));

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    /**
     * One torn ribbon of the bolt body. The pose is spun on the flight
     * axis by planeDeg, then the ribbon's local +X length is laid onto
     * -Z, so the ribbon starts at zOrigin and tears backward.
     */
    private static void ribbon(PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType,
                               long seed, float zOrigin, float planeDeg, float length, float width, float bend,
                               float r, float g, float b, float alpha, float envelope, float distScale) {
        poseStack.pushPose();
        poseStack.translate(0, 0, zOrigin);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(planeDeg)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(90f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.tornRibbon(pose, buffer, seed, length, width, bend, r, g, b, alpha, envelope, distScale));
        poseStack.popPose();
    }

    /** A shard fan standing across the flight axis at distance z, spun on the axis. */
    private interface FanDraw {
        void draw(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buffer);
    }

    private static void axisFan(PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType,
                                float z, float spinDeg, FanDraw draw) {
        poseStack.pushPose();
        poseStack.translate(0, 0, z);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(spinDeg)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> draw.draw(pose, buffer));
        poseStack.popPose();
    }
}
