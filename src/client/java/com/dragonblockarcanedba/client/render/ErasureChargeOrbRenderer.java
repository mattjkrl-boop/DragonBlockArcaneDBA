package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.ErasureChargeOrbEntity;
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
 * Entity Renderer for Erasure Cannon Muzzle Charge Orb in MC 26.2.
 * Bespoke pass: all geometry comes from BespokeGeometry (rule 1), seeded
 * from the orb entity's synced seed. The regular solids are gone:
 * - The rotating octahedron core is replaced by an irregular bespoke
 *   core: a small displaced dome pair (a noise-lumpy energy ball, so it
 *   still reads as a singularity and still turns with age) wrapped in a
 *   cluster of shard fans in three planes, a jagged crystalline cage.
 * - The geodesic sphere shells are now displaced dome pairs, one cap up
 *   and one flipped, counter-rotating per layer, with the same 3-stop
 *   color journey carried by layer choice instead of latitude bands.
 * - The three gyroscope rings are broken vortex bands in three planes:
 *   jittered radii, seeded gaps, no equal segments anywhere.
 * - The 8 intake spikes are BespokeGeometry branches, jagged ribbons
 *   spiraling into the core, each with a thin white core pass.
 */
public class ErasureChargeOrbRenderer extends EntityRenderer<ErasureChargeOrbEntity, ErasureChargeOrbRenderer.ErasureChargeRenderState> {

    public ErasureChargeOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ErasureChargeRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public float yRot = 0.0f;
        public float xRot = 0.0f;
        public float age = 0.0f;
        public int seed = 0;
        public boolean isFirstPersonOwner = false;
        public boolean onRight = true;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(ErasureChargeOrbEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(ErasureChargeOrbEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ErasureChargeRenderState createRenderState() {
        return new ErasureChargeRenderState();
    }

    @Override
    public void extractRenderState(ErasureChargeOrbEntity entity, ErasureChargeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
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
    public void submit(ErasureChargeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float charge = state.chargeRatio;
        float age = state.age;
        float pulse = 0.92f + 0.08f * (float) Math.sin(age * 0.65f);

        // Scaled down smaller in first person
        float chargeEase = charge * charge * (3.0f - 2.0f * charge);
        float orbRadius = (0.22f + chargeEase * 0.95f) * pulse * (state.isFirstPersonOwner ? 0.45f : 1.0f);
        float alpha = 0.10f + chargeEase * 0.90f;
        float hot = charge <= 0.75f ? 0.0f : (charge - 0.75f) / 0.25f;

        double camDist = cameraState.pos.distanceTo(new Vec3(state.x, state.y, state.z));
        final float distScale = com.dragonblockarcanedba.util.FxEnvelope.distScale(camDist);
        final long seed = state.seed;

        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        if (state.isFirstPersonOwner) {
            // Anchor orb realistically to the Blaster Gun muzzle: way lower and on the weapon hand side
            float sideSign = state.onRight ? -1.0f : 1.0f;
            poseStack.translate(sideSign * 0.42f, -0.48f, -0.60f);
        }

        // Core: a displaced dome pair (irregular lumpy ball) in near white,
        // turning with age like the octahedron did, plus a shard cluster
        // cage in three planes at a slightly larger radius.
        float coreRadius = orbRadius * 0.38f;
        float coreAlpha = 0.98f * (0.25f + chargeEase * 0.75f);
        domeShell(poseStack, collector, renderType, seed ^ 0xC0E1L, coreRadius, age * 36.0f, false,
            1.0f, 1.0f, 1.0f, coreAlpha, distScale);
        domeShell(poseStack, collector, renderType, seed ^ 0xC0E1L, coreRadius, age * 36.0f, true,
            1.0f, 1.0f, 1.0f, coreAlpha, distScale);
        shardCluster(poseStack, collector, renderType, seed ^ 0x51A7L, coreRadius * 1.7f,
            1.0f, 1.0f, 1.0f, coreAlpha * 0.85f, distScale);

        // Shells: displaced dome pairs, counter-rotating per layer. Colors
        // keep the old journey: the mid shell whitens toward red-hot as the
        // charge passes 0.75, the inner shell runs pale cyan, and the outer
        // wispy shell sits at the standard outer band alpha.
        float shellR = hot * 0.85f;
        domeShell(poseStack, collector, renderType, seed ^ 0x5E11L, orbRadius * 0.82f, age * -18.0f, false,
            shellR, 0.95f, 1.0f, alpha * 0.72f, distScale);
        domeShell(poseStack, collector, renderType, seed ^ 0x5E11L, orbRadius * 0.82f, age * -18.0f, true,
            shellR, 0.95f, 1.0f, alpha * 0.72f, distScale);
        domeShell(poseStack, collector, renderType, seed ^ 0x5E22L, orbRadius * 0.48f, age * 26.0f, false,
            0.85f + hot * 0.15f, 0.96f, 1.0f, alpha * (0.40f + hot * 0.45f), distScale);
        domeShell(poseStack, collector, renderType, seed ^ 0x5E22L, orbRadius * 0.48f, age * 26.0f, true,
            0.85f + hot * 0.15f, 0.96f, 1.0f, alpha * (0.40f + hot * 0.45f), distScale);
        domeShell(poseStack, collector, renderType, seed ^ 0x5E33L, orbRadius * 1.18f, age * 9.0f, false,
            0.35f, 0.90f, 1.0f, alpha * 0.18f, distScale);
        domeShell(poseStack, collector, renderType, seed ^ 0x5E33L, orbRadius * 1.18f, age * 9.0f, true,
            0.35f, 0.90f, 1.0f, alpha * 0.18f, distScale);

        // Three gyroscope rings as broken vortex bands in three planes.
        float ringR = orbRadius * (1.48f - chargeEase * 0.28f);
        float bandT = Math.max(0.012f, ringR * 0.06f);
        ringBand(poseStack, collector, renderType, seed ^ 0xB111L, ringR, bandT,
            (float) Math.toRadians(age * 22.0f), 0, hot, 0.90f, 1.0f, alpha * 0.90f, distScale);
        ringBand(poseStack, collector, renderType, seed ^ 0xB222L, ringR * 0.94f, bandT,
            (float) Math.toRadians(age * -16.0f), 1, hot * 0.6f, 0.85f, 1.0f, alpha * 0.60f, distScale);
        ringBand(poseStack, collector, renderType, seed ^ 0xB333L, ringR * 0.88f, bandT,
            (float) Math.toRadians(age * 27.0f), 2, hot * 0.6f, 0.85f, 1.0f, alpha * 0.50f, distScale);

        // Intake spikes: jagged branches spiraling into the core, positions
        // and convergence exactly as the old lash spikes, drawn as seeded
        // branch ribbons with a thin white core pass per spike.
        final float spikeAlpha = alpha * 0.92f;
        final float spikeWidth = 0.05f + charge * 0.03f;
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            int spikeCount = 8;
            float intakeRadius = orbRadius * (2.3f - chargeEase * 0.95f);
            float intakeSpeed = 30.0f + (charge * 60.0f);
            for (int s = 0; s < spikeCount; s++) {
                double angle = (s / (double) spikeCount) * Math.PI * 2.0 + Math.toRadians(age * intakeSpeed);
                float sx = (float) Math.cos(angle) * intakeRadius;
                float sy = (float) Math.sin(angle) * intakeRadius;
                float sz = (float) Math.sin(age * 0.4f + s * 1.2f) * (orbRadius * 0.6f);
                Vec3 toCenter = new Vec3(-sx, -sy, -sz).normalize();
                Vec3 tip = new Vec3(sx, sy, sz).add(toCenter.scale(orbRadius * 0.7f));
                long spikeSeed = seed ^ (0x59L + s * 977L);
                BespokeGeometry.branch(pose, buffer, spikeSeed, new Vec3(sx, sy, sz), tip,
                    spikeWidth, 0.0f, 0.72f, 1.0f, spikeAlpha, 1.0f, distScale);
                BespokeGeometry.branch(pose, buffer, spikeSeed ^ 0x11L, new Vec3(sx, sy, sz), tip,
                    spikeWidth * 0.35f, 0.90f, 0.98f, 1.0f, Math.min(1.0f, spikeAlpha * 1.1f), 1.0f, distScale);
            }
        });

        poseStack.popPose();
    }

    /**
     * One displaced dome cap. The toolkit dome is a hemisphere (rim in the
     * XZ plane, cap toward +Y); flip turns it downward so a pair of caps
     * forms the full irregular shell, and rotDeg spins it around Y.
     */
    private static void domeShell(PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType,
                                  long seed, float radius, float rotDeg, boolean flip,
                                  float r, float g, float b, float alpha, float distScale) {
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(rotDeg)));
        if (flip) {
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(180f)));
        }
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.displacedDome(pose, buffer, seed, radius, r, g, b, alpha, 1.0f, distScale));
        poseStack.popPose();
    }

    /** A shard fan in each of the three planes through the center: the core's jagged cage. */
    private static void shardCluster(PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType,
                                     long seed, float radius, float r, float g, float b, float alpha, float distScale) {
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.shardFan(pose, buffer, seed, radius, r, g, b, alpha, 1.0f, distScale));
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.shardFan(pose, buffer, seed ^ 0x77L, radius, r, g, b, alpha, 1.0f, distScale));
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(90f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.shardFan(pose, buffer, seed ^ 0xEEL, radius, r, g, b, alpha, 1.0f, distScale));
        poseStack.popPose();
    }

    /**
     * One gyroscope ring as a broken vortex band. Plane selects the ring's
     * plane: 0 for the muzzle (XY) plane, 1 for horizontal (XZ), 2 for the
     * side (ZY) plane. The band's twist carries the ring's spin.
     */
    private static void ringBand(PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType,
                                 long seed, float radius, float halfThickness, float twist,
                                 int plane, float r, float g, float b, float alpha, float distScale) {
        poseStack.pushPose();
        if (plane == 0) {
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90f)));
        } else if (plane == 2) {
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(90f)));
        }
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.brokenVortexBand(pose, buffer, seed, radius, -halfThickness, halfThickness,
                twist, r, g, b, alpha, distScale));
        poseStack.popPose();
    }
}
