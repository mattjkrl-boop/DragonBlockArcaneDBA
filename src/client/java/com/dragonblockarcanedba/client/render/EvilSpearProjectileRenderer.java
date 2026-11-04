package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.EvilSpearProjectileEntity;
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
 * Entity Renderer for Evil Spear Projectile in MC 26.2.
 * Renders a high-fidelity 3D demonic spectral spear with faceted shaft, multi-beveled diamond spearhead,
 * barbed vortex fins, supersonic Mach shock cone, layered torn trail ribbons, and expanding wake vortex rings.
 *
 * Bespoke pass (standard rule 1): the spear is rebuilt entirely from
 * BespokeGeometry, seeded from the entity UUID. Crossed torn ribbons form
 * the shaft and the diamond blade (a flat ribbon reads as a rod; two
 * crossed ribbons read as a faceted solid), angled torn ribbons form the
 * barbs, a plume forms the Mach sheath, and broken vortex bands form the
 * cone rim and the wake. The old per-renderer regular helpers (equal-sided
 * taper tube, diamond head, cone fan, rings, quads) are deleted.
 */
public class EvilSpearProjectileRenderer extends EntityRenderer<EvilSpearProjectileEntity, EvilSpearProjectileRenderer.EvilSpearRenderState> {
    public EvilSpearProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class EvilSpearRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public boolean isFirstPersonOwner = false;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(EvilSpearProjectileEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(EvilSpearProjectileEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public EvilSpearRenderState createRenderState() {
        return new EvilSpearRenderState();
    }

    @Override
    public void extractRenderState(EvilSpearProjectileEntity entity, EvilSpearRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null &&
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) &&
            mc.options.getCameraType().isFirstPerson());
    }

    @Override
    public void submit(EvilSpearRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        final float age = state.age;
        final long seed = state.seed;
        // Distance scaling (standard point 1): distant throws step their
        // segment counts down instead of costing full geometry.
        final float ds = LayeredRender.distScale(cameraState, state.x, state.y, state.z);

        poseStack.pushPose();
        poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));
        poseStack.mulPose(new Matrix4f().rotation(Axis.ZP.rotationDegrees(age * 30.0f))); // Continuous high-speed spiral spin

        float fpScale = (state.isFirstPersonOwner && state.age < 5.0f) ? (0.60f + (state.age / 5.0f) * 0.40f) : 1.0f;
        poseStack.scale(fpScale, fpScale, fpScale);

        // The throw builds in over its first 3 ticks, holds, then eases out
        // over the last part of the flight. Nothing pops in at full alpha.
        float spawnT = Math.min(1.0f, age / 3.0f);
        float spawnFade = spawnT * spawnT * (3.0f - 2.0f * spawnT);
        float endT = age > 32.0f ? Math.min(1.0f, (age - 32.0f) / 8.0f) : 0.0f;
        final float bodyFade = spawnFade * (1.0f - endT * endT * (3.0f - 2.0f * endT));

        // Spear local space: butt at z = -2.0, head tip at z = 2.85.
        // tornRibbon runs along its local +X; the rotations below lay it
        // along the spear axis with the wide end where each part needs it.

        // Shaft: crossed torn ribbons, wide at the neck under the head,
        // narrowing to the butt. Dark sheath pair, then the bright core pair.
        poseStack.pushPose();
        poseStack.translate(0, 0, 1.35f);
        poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(90.0f)));
        ribbon(poseStack, collector, renderType, seed ^ 0x5A01L, 3.35f, 0.24f, 0.0f,
            0.22f, 0.02f, 0.08f, 0.95f * bodyFade, ds);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 1.35f);
        poseStack.mulPose(new Matrix4f().rotation(Axis.ZP.rotationDegrees(-90.0f)));
        poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(90.0f)));
        ribbon(poseStack, collector, renderType, seed ^ 0x5A02L, 3.35f, 0.24f, 0.0f,
            0.22f, 0.02f, 0.08f, 0.95f * bodyFade, ds);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 1.22f);
        poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(90.0f)));
        ribbon(poseStack, collector, renderType, seed ^ 0x5A03L, 3.07f, 0.10f, 0.0f,
            1.0f, 0.55f, 0.68f, bodyFade, ds);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 1.22f);
        poseStack.mulPose(new Matrix4f().rotation(Axis.ZP.rotationDegrees(-90.0f)));
        poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(90.0f)));
        ribbon(poseStack, collector, renderType, seed ^ 0x5A04L, 3.07f, 0.10f, 0.0f,
            1.0f, 0.55f, 0.68f, bodyFade, ds);
        poseStack.popPose();

        // Diamond head: crossed wide ribbons, crimson edge pair outside,
        // pale core pair inside, both tapering to the tip at z = 2.85.
        poseStack.pushPose();
        poseStack.translate(0, 0, 1.2f);
        poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(-90.0f)));
        ribbon(poseStack, collector, renderType, seed ^ 0x5B01L, 1.65f, 0.84f, 0.0f,
            0.95f, 0.05f, 0.20f, 0.95f * bodyFade, ds);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 1.2f);
        poseStack.mulPose(new Matrix4f().rotation(Axis.ZP.rotationDegrees(90.0f)));
        poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(-90.0f)));
        ribbon(poseStack, collector, renderType, seed ^ 0x5B02L, 1.65f, 0.84f, 0.0f,
            0.95f, 0.05f, 0.20f, 0.95f * bodyFade, ds);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 1.55f);
        poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(-90.0f)));
        ribbon(poseStack, collector, renderType, seed ^ 0x5B03L, 1.13f, 0.32f, 0.0f,
            1.0f, 0.85f, 0.90f, bodyFade, ds);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 1.55f);
        poseStack.mulPose(new Matrix4f().rotation(Axis.ZP.rotationDegrees(90.0f)));
        poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(-90.0f)));
        ribbon(poseStack, collector, renderType, seed ^ 0x5B04L, 1.13f, 0.32f, 0.0f,
            1.0f, 0.85f, 0.90f, bodyFade, ds);
        poseStack.popPose();

        // Barbed crossguard fins at the butt: four angled ribbons reaching
        // out and forward from z = -2.0, plus two rear flukes at the head.
        barb(poseStack, collector, renderType, seed ^ 0x5C01L, -2.0f, -63.4f, 0.0f, 1.23f, bodyFade, ds);
        barb(poseStack, collector, renderType, seed ^ 0x5C02L, -2.0f, -116.6f, 0.0f, 1.23f, bodyFade, ds);
        poseStack.pushPose();
        poseStack.translate(0, 0, -2.0f);
        poseStack.mulPose(new Matrix4f().rotation(Axis.XP.rotationDegrees(63.4f)));
        poseStack.mulPose(new Matrix4f().rotation(Axis.ZP.rotationDegrees(90.0f)));
        ribbon(poseStack, collector, renderType, seed ^ 0x5C03L, 1.23f, 0.12f, 0.12f,
            0.85f, 0.0f, 0.15f, 0.90f * bodyFade, ds);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0, 0, -2.0f);
        poseStack.mulPose(new Matrix4f().rotation(Axis.XP.rotationDegrees(-63.4f)));
        poseStack.mulPose(new Matrix4f().rotation(Axis.ZP.rotationDegrees(-90.0f)));
        ribbon(poseStack, collector, renderType, seed ^ 0x5C04L, 1.23f, 0.12f, 0.12f,
            0.85f, 0.0f, 0.15f, 0.90f * bodyFade, ds);
        poseStack.popPose();
        barb(poseStack, collector, renderType, seed ^ 0x5C05L, 1.2f, 55.0f, 1.0f, 0.61f, bodyFade, ds);
        barb(poseStack, collector, renderType, seed ^ 0x5C06L, 1.2f, 125.0f, 1.0f, 0.61f, bodyFade, ds);

        // Mach sheath around the head: an irregular plume leaning to the
        // tip where the equal-segment cone fan used to be, rimmed by a
        // broken band at its mouth (z = 0.3).
        poseStack.pushPose();
        poseStack.translate(0, 0, 0.3f);
        poseStack.mulPose(new Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.plume(pose, buffer, seed ^ 0x5D01L, 0.70f, 2.55f,
                1.0f, 0.12f, 0.24f, 0.42f, bodyFade, ds));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x5D02L, 0.92f, 0.0f, 0.15f,
                age * 0.3f, 1.0f, 0.72f, 0.78f, 0.55f * bodyFade, ds));
        poseStack.popPose();

        // Blood ribbons trailing the throw: two dense torn strands plus a
        // third wide, faint wisp so the trail has layered depth (core,
        // mid, outer). Each strand is clocked around the spear axis.
        for (int strand = 0; strand < 3; strand++) {
            float clockDeg = strand == 1 ? 180.0f : (strand == 2 ? 90.0f : 0.0f);
            boolean wispy = strand == 2;
            poseStack.pushPose();
            poseStack.translate(0, 0, -2.0f);
            poseStack.mulPose(new Matrix4f().rotation(Axis.ZP.rotationDegrees(clockDeg)));
            poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(90.0f)));
            ribbon(poseStack, collector, renderType, seed ^ (0x5E00L + strand),
                4.2f, wispy ? 0.49f : 0.36f, 0.45f,
                1.0f, 0.18f, 0.22f, 0.85f * bodyFade * (wispy ? 0.32f : 1.0f), ds);
            poseStack.popPose();
        }

        // One wake band eases back off the spear and dies.
        float wake = (age * 0.12f) % 1.0f;
        float wakeEase = wake * wake * (3.0f - 2.0f * wake);
        float ringZ = -2.0f - wakeEase * 4.2f;
        float ringRadius = 0.28f + wakeEase * 0.7f;
        float ringAlpha = (float) Math.sin(wake * Math.PI) * 0.5f * bodyFade;
        poseStack.pushPose();
        poseStack.translate(0, 0, ringZ);
        poseStack.mulPose(new Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x5F01L, ringRadius, 0.0f, 0.18f,
                0.6f, 0.95f, 0.05f, 0.20f, ringAlpha, ds));
        poseStack.popPose();

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    /** Submit one torn ribbon in the current pose. Orchestration only. */
    private static void ribbon(PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType,
                               long seed, float length, float width, float bend,
                               float r, float g, float b, float alpha, float ds) {
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.tornRibbon(pose, buffer, seed, length, width, bend, r, g, b, alpha, 1.0f, ds));
    }

    /**
     * One horizontal barb: a torn ribbon rooted at zRoot and angled by
     * yawDeg so its length reaches out sideways and along the spear.
     * shade picks the crossguard (0) or the brighter head fluke (1) colors.
     */
    private static void barb(PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType,
                             long seed, float zRoot, float yawDeg, float shade,
                             float length, float bodyFade, float ds) {
        poseStack.pushPose();
        poseStack.translate(0, 0, zRoot);
        poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(yawDeg)));
        if (shade > 0.5f) {
            ribbon(poseStack, collector, renderType, seed, length, 0.10f, 0.06f,
                1.0f, 0.10f, 0.25f, 0.95f * bodyFade, ds);
        } else {
            ribbon(poseStack, collector, renderType, seed, length, 0.12f, 0.12f,
                0.85f, 0.0f, 0.15f, 0.90f * bodyFade, ds);
        }
        poseStack.popPose();
    }
}
