package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.ErasureCannonBeamEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Erasure Cannon Beam in MC 26.2.
 * Bespoke pass: all geometry comes from BespokeGeometry (rule 1), seeded
 * from the beam entity's synced seed, so every client draws the same beam:
 * - The beam body is crossed torn ribbons in three layers (white core,
 *   cyan mid, dark outer), torn edges and seeded wobble instead of the old
 *   regular 12-sided tubes and hand-rolled helical quad ribbons.
 * - Three focusing aperture stations along the beam are irregular glow
 *   fans (the bespoke stand-in for the old hexagonal rings), spinning on
 *   the beam axis.
 * - The muzzle emitter is a glow fan plus a shard fan of compression
 *   blades; the terminus is a displaced dome with a shard splash fan and
 *   a glow splash, instead of the old equal-segment dome and lash fins.
 * The local frame is unchanged: the beam runs along +Z from the muzzle.
 * Ribbon planes are posed by rotating the pose stack (spin on the beam
 * axis, then the ribbon's +X length onto +Z) once per submit.
 */
public class ErasureCannonBeamRenderer extends EntityRenderer<ErasureCannonBeamEntity, ErasureCannonBeamRenderer.ErasureCannonRenderState> {

    public ErasureCannonBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ErasureCannonRenderState extends EntityRenderState {
        public float length = 48.0f;
        public float yRot = 0.0f;
        public float xRot = 0.0f;
        public float chargeRatio = 1.0f;
        public float age = 0.0f;
        public int seed = 0;
        public boolean isFirstPersonOwner = false;
        public boolean onRight = true;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(ErasureCannonBeamEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(ErasureCannonBeamEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ErasureCannonRenderState createRenderState() {
        return new ErasureCannonRenderState();
    }

    @Override
    public void extractRenderState(ErasureCannonBeamEntity entity, ErasureCannonRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.length = entity.getBeamLength();
        state.yRot = entity.getBeamYRot();
        state.xRot = entity.getBeamXRot();
        state.chargeRatio = entity.getChargeRatio();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getSeed();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null &&
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) &&
            mc.options.getCameraType().isFirstPerson());
        if (mc.player != null) {
            boolean isRightHanded = (mc.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            boolean isOffhand = (mc.player.getOffhandItem().getItem() instanceof com.dragonblockarcanedba.item.BlasterGunItem &&
                !(mc.player.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.BlasterGunItem));
            state.onRight = isRightHanded ? !isOffhand : isOffhand;
        }
    }

    @Override
    public void submit(ErasureCannonRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float beamLength = state.length;
        if (beamLength < 0.2f) return;

        float charge = state.chargeRatio;
        float age = state.age;
        float maxLifetime = ErasureCannonBeamEntity.MAX_LIFETIME;

        // Shared envelope (spectacle standard point 5): the renderer uses
        // the same FxEnvelope the entity's particle counts use, so mesh
        // alpha and particle density build and dissipate together.
        float env = com.dragonblockarcanedba.util.FxEnvelope.envelope(age, maxLifetime);
        float exitT = age <= maxLifetime * 0.7f ? 0.0f : Math.min(1.0f, (age - maxLifetime * 0.7f) / (maxLifetime * 0.3f));

        float pulse = (1.0f + 0.04f * (float) Math.sin(age * 1.4f)) * (0.55f + 0.45f * env);
        float impactPulse = pulse * (1.12f - exitT * 0.50f);

        // Distance scaling (standard point 1): builder segment counts step
        // down with range, and far views drop the last aperture first.
        double camDist = cameraState.pos.distanceTo(new net.minecraft.world.phys.Vec3(state.x, state.y, state.z));
        final float distScale = com.dragonblockarcanedba.util.FxEnvelope.distScale(camDist);
        // Scaled down smaller in first person
        float baseRadius = (0.35f + charge * 0.45f) * pulse * (state.isFirstPersonOwner ? 0.50f : 1.0f);

        RenderType renderType = KiRenderHelper.kiRenderType();
        final long seed = state.seed;
        final float zStart = state.isFirstPersonOwner ? 0.20f : 0.0f;
        final float span = beamLength - zStart;
        if (span <= 0.0f) return;

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        if (state.isFirstPersonOwner) {
            // Anchor beam origin to the Blaster Gun muzzle: way lower and on weapon hand side
            float sideSign = state.onRight ? -1.0f : 1.0f;
            poseStack.translate(sideSign * 0.38f, -0.46f, 0.40f);
        }

        // Beam body: crossed torn ribbons. Widths match the cross-section
        // extents of the tubes they replace (core 0.28, mid 0.72, outer
        // 0.86 of baseRadius, as diameters). The core runs straight, the
        // mid and outer layers take a slight seeded bend so the beam reads
        // as torn energy, not a ruled sheet.
        ribbonPlane(poseStack, collector, renderType, seed ^ 0xC0EEEL, zStart, 0f,
            span, baseRadius * 0.55f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, env, distScale);
        ribbonPlane(poseStack, collector, renderType, seed ^ 0xC0EEEL, zStart, 90f,
            span, baseRadius * 0.55f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, env, distScale);
        ribbonPlane(poseStack, collector, renderType, seed ^ 0x111DL, zStart, 0f,
            span, baseRadius * 1.45f, 0.12f, 0.0f, 0.92f, 1.0f, 0.78f, env, distScale);
        ribbonPlane(poseStack, collector, renderType, seed ^ 0x111DL, zStart, 60f,
            span, baseRadius * 1.45f, 0.12f, 0.0f, 0.92f, 1.0f, 0.78f, env, distScale);
        ribbonPlane(poseStack, collector, renderType, seed ^ 0x111DL, zStart, 120f,
            span, baseRadius * 1.45f, 0.12f, 0.0f, 0.92f, 1.0f, 0.78f, env, distScale);
        ribbonPlane(poseStack, collector, renderType, seed ^ 0x2222L, zStart, 30f,
            span, baseRadius * 1.72f, 0.20f, 0.0f, 0.55f, 0.85f, 0.35f, env, distScale);
        ribbonPlane(poseStack, collector, renderType, seed ^ 0x2222L, zStart, 120f,
            span, baseRadius * 1.72f, 0.20f, 0.0f, 0.55f, 0.85f, 0.35f, env, distScale);

        // Focusing aperture stations: irregular glow fans standing across
        // the beam at quarter, half, and three-quarter length, spinning on
        // the beam axis in alternating directions, tightening with distance
        // from the muzzle exactly as the old rings did.
        float[] ringFracs = {0.25f, 0.50f, 0.75f};
        for (int ri = 0; ri < ringFracs.length; ri++) {
            if (distScale < 0.55f && ri == 2) continue;
            float frac = ringFracs[ri];
            float ringZ = zStart + frac * span;
            float ringRadius = baseRadius * (2.3f - frac * 0.7f);
            float ringRot = age * (ri % 2 == 0 ? 30.0f : -30.0f) + ri * 30.0f;
            final float fanRadius = ringRadius;
            final long fanSeed = seed ^ (0xA9E0L + ri * 131L);
            beamPlaneFan(poseStack, collector, renderType, ringZ, ringRot, (pose, buffer) ->
                BespokeGeometry.glowFan(pose, buffer, fanSeed, fanRadius, 0f,
                    0.35f, 0.95f, 1.0f, 0.55f * env, distScale));
        }

        // Muzzle emitter: a spinning glow fan at the barrel plus a shard
        // fan of irregular compression blades just ahead of it.
        beamPlaneFan(poseStack, collector, renderType, 0.16f, age * 24.0f, (pose, buffer) ->
            BespokeGeometry.glowFan(pose, buffer, seed ^ 0x3333L, 0.72f * pulse, 0f,
                0.85f, 0.98f, 1.0f, 0.95f * env, distScale));
        beamPlaneFan(poseStack, collector, renderType, 0.30f, -age * 18.0f, (pose, buffer) ->
            BespokeGeometry.shardFan(pose, buffer, seed ^ 0xB1ADL, 1.05f * pulse,
                0.15f, 0.75f, 1.0f, 0.85f, env, distScale));

        // Terminus impact: a displaced dome flaring at the hit point, a
        // white core dome inside it, a shard splash fan, and a glow splash.
        float domeRadius = (1.2f + charge * 0.8f) * impactPulse;
        poseStack.pushPose();
        poseStack.translate(0, 0, beamLength - 0.4f);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xD0E1L, domeRadius,
                0.0f, 0.95f, 1.0f, 0.85f, env, distScale);
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xC02EL, domeRadius * 0.35f,
                1.0f, 1.0f, 1.0f, 0.75f, env, distScale);
        });
        poseStack.popPose();

        beamPlaneFan(poseStack, collector, renderType, beamLength + 0.10f, age * 30.0f, (pose, buffer) ->
            BespokeGeometry.shardFan(pose, buffer, seed ^ 0xF115L, domeRadius * 1.40f,
                0.20f, 0.85f, 1.0f, 0.90f, env, distScale));
        beamPlaneFan(poseStack, collector, renderType, beamLength - 0.05f, age * 10.0f, (pose, buffer) ->
            BespokeGeometry.glowFan(pose, buffer, seed ^ 0x591AL, domeRadius * 1.15f, 0f,
                0.0f, 0.95f, 1.0f, 0.40f * env, distScale));

        poseStack.popPose();
    }

    /**
     * One torn ribbon plane of the beam body. The pose is spun on the beam
     * axis by planeDeg, then the ribbon's local +X length is laid onto the
     * beam's +Z, starting at zStart.
     */
    private static void ribbonPlane(PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType,
                                    long seed, float zStart, float planeDeg, float length, float width, float bend,
                                    float r, float g, float b, float alpha, float envelope, float distScale) {
        poseStack.pushPose();
        poseStack.translate(0, 0, zStart);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(planeDeg)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-90f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.tornRibbon(pose, buffer, seed, length, width, bend, r, g, b, alpha, envelope, distScale));
        poseStack.popPose();
    }

    /** A bespoke fan standing across the beam at distance z, spun on the beam axis. */
    private interface FanDraw {
        void draw(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buffer);
    }

    private static void beamPlaneFan(PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType,
                                     float z, float spinDeg, FanDraw draw) {
        poseStack.pushPose();
        poseStack.translate(0, 0, z);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(spinDeg)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> draw.draw(pose, buffer));
        poseStack.popPose();
    }
}
