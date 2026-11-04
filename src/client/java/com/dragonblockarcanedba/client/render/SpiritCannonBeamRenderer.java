package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.SpiritCannonBeamEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
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
 * Entity Renderer for Spirit Cannon Beam in Minecraft 26.2.
 * Bespoke pass: every shape comes from BespokeGeometry seeded by the
 * entity's synced seed. The beam body is crossed torn ribbons (white core,
 * cyan flow pair, wide sheath pair), the orbital nodes are glow fan
 * apertures with counter spun shard star reticles, the muzzle is a glow
 * aperture with a shard seal, and the terminus is a displaced dome with a
 * gold splash star and a white flash fan. Layer radii, colors, envelope,
 * first person anchoring, and the fade retract are unchanged from the
 * spectacle pass.
 */
public class SpiritCannonBeamRenderer extends EntityRenderer<SpiritCannonBeamEntity, SpiritCannonBeamRenderer.SpiritCannonRenderState> {

    public SpiritCannonBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SpiritCannonRenderState extends EntityRenderState {
        public float length = 32.0f;
        public float yRot = 0.0f;
        public float xRot = 0.0f;
        public float age = 0.0f;
        public float fade = 1.0f;
        public int casterId = -1;
        public boolean isFirstPersonOwner = false;
        public boolean onRight = true;
        public long seed = 0L;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(SpiritCannonBeamEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(SpiritCannonBeamEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public SpiritCannonRenderState createRenderState() {
        return new SpiritCannonRenderState();
    }

    @Override
    public void extractRenderState(SpiritCannonBeamEntity entity, SpiritCannonRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.length = entity.getBeamLength();
        state.yRot = entity.getBeamYRot();
        state.xRot = entity.getBeamXRot();
        state.age = entity.tickCount + partialTicks;
        state.fade = entity.getFade();
        state.casterId = entity.getCasterId();
        state.seed = entity.getSeed();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null &&
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) &&
            mc.options.getCameraType().isFirstPerson());
        if (mc.player != null) {
            boolean isRightHanded = (mc.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            boolean isOffhand = (mc.player.getOffhandItem().getItem() instanceof com.dragonblockarcanedba.item.SpiritSwordItem &&
                !(mc.player.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.SpiritSwordItem));
            state.onRight = isRightHanded ? !isOffhand : isOffhand;
        }
    }

    @Override
    public void submit(SpiritCannonRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float beamLength = state.length;
        if (beamLength < 0.2f || state.fade <= 0.01f) return;

        float age = state.age;
        float pulse = 1.0f + 0.06f * (float) Math.sin(age * 1.5f);
        float fadeRaw = Math.min(1.0f, age / 5.0f);
        float fadeIn = fadeRaw * fadeRaw * (3.0f - 2.0f * fadeRaw);
        // Fade out (standard point 5): the entity counts a synced FADE down
        // over its last 6 ticks; alpha and drawn length both ride it, so the
        // beam retracts and thins instead of vanishing in one tick.
        final float env = fadeIn * state.fade;
        final float distScale = LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        final long seed = state.seed;
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        if (state.isFirstPersonOwner) {
            // Anchor beam origin to the Spirit Sword blade tip: way lower and on weapon hand side
            float sideSign = state.onRight ? -1.0f : 1.0f;
            poseStack.translate(sideSign * 0.40f, -0.46f, 0.35f);
        }

        float zStart = state.isFirstPersonOwner ? 0.25f : 0.0f;
        float fpScale = state.isFirstPersonOwner ? 0.50f : 1.0f;
        final float drawLength = zStart + (beamLength - zStart) * (0.15f + 0.85f * state.fade);
        final float fZStart = zStart;
        final float fFpScale = fpScale;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // 1. Beam body as crossed torn ribbons. Widths are the old
            // prism diameters (core 0.26, mid 0.52, sheath 0.84 across),
            // rolls advance at the old prism rotation speeds, and the
            // bends give the flow layers a slow irregular wander the
            // prisms never had.
            float runLen = drawLength - fZStart;
            beamRibbon(pose, buffer, seed + 1L, age * 5.0f, fZStart, runLen,
                0.26f * pulse * fFpScale, 0.02f, 1.0f, 1.0f, 1.0f, 0.95f, env, distScale);
            beamRibbon(pose, buffer, seed + 2L, age * 5.0f + 90.0f, fZStart, runLen,
                0.26f * pulse * fFpScale, -0.02f, 1.0f, 1.0f, 1.0f, 0.95f, env, distScale);
            beamRibbon(pose, buffer, seed + 3L, -age * 8.0f, fZStart, runLen,
                0.52f * pulse * fFpScale, 0.35f, 0.0f, 0.92f, 1.0f, 0.65f, env, distScale);
            beamRibbon(pose, buffer, seed + 4L, -age * 8.0f + 90.0f, fZStart, runLen,
                0.52f * pulse * fFpScale, -0.35f, 0.0f, 0.92f, 1.0f, 0.65f, env, distScale);
            beamRibbon(pose, buffer, seed + 5L, age * 3.0f, fZStart, runLen,
                0.84f * pulse * fFpScale, 0.55f, 0.05f, 0.70f, 1.0f, 0.30f, env, distScale);
            beamRibbon(pose, buffer, seed + 6L, age * 3.0f + 90.0f, fZStart, runLen,
                0.84f * pulse * fFpScale, -0.55f, 0.05f, 0.70f, 1.0f, 0.30f, env, distScale);

            // 2. Orbital nodes: glow fan apertures riding the beam with a
            // counter spun shard star reticle inside each. Placement,
            // alternating cyan and gold, pulse phasing, and radii are the
            // spectacle numbers; only the shapes went bespoke.
            float usable = drawLength - fZStart;
            if (usable >= 3.0f && env > 0.01f) {
                int nodes = FxEnvelope.segments(Math.min(8, Math.max(2, (int) (usable / 4.0f))), distScale, 2);
                for (int n = 0; n < nodes; n++) {
                    float frac = (n + 1) / (float) (nodes + 1);
                    float z = fZStart + usable * frac;
                    boolean gold = (n % 2 == 1);
                    float r = gold ? 1.0f : 0.0f;
                    float g = gold ? 0.82f : 0.92f;
                    float b = gold ? 0.25f : 1.0f;
                    float nodePulse = 0.72f + 0.28f * (float) Math.sin(age * 3.0f + n * 1.7f);
                    float ringR = (0.64f + 0.08f * (float) Math.sin(age * 2.0f + n * 2.3f)) * pulse * fFpScale;
                    float spin = age * (gold ? -38.0f : 38.0f) + n * 30.0f;
                    PoseStack.Pose aperture = pose.copy();
                    aperture.translate(0.0f, 0.0f, z);
                    aperture.rotate(Axis.ZP.rotationDegrees(spin));
                    aperture.rotate(Axis.XP.rotationDegrees(90.0f));
                    BespokeGeometry.glowFan(aperture, buffer, seed + 100L + n * 17L, ringR, 0.0f,
                        r, g, b, 0.50f * nodePulse * env, distScale);
                    PoseStack.Pose reticle = pose.copy();
                    reticle.translate(0.0f, 0.0f, z);
                    reticle.rotate(Axis.ZP.rotationDegrees(-spin * 1.4f));
                    reticle.rotate(Axis.XP.rotationDegrees(90.0f));
                    BespokeGeometry.shardFan(reticle, buffer, seed + 200L + n * 29L, ringR * 0.62f,
                        Math.min(1f, r + 0.4f), Math.min(1f, g + 0.4f), Math.min(1f, b + 0.4f),
                        0.55f, nodePulse * env, distScale);
                }
            }

            // 3. Muzzle aperture at the blade tip: a cyan glow fan with a
            // counter spun white-cyan shard seal inside it.
            PoseStack.Pose muzzle = pose.copy();
            muzzle.translate(0.0f, 0.0f, 0.15f);
            muzzle.rotate(Axis.ZP.rotationDegrees(age * 30.0f));
            muzzle.rotate(Axis.XP.rotationDegrees(90.0f));
            BespokeGeometry.glowFan(muzzle, buffer, seed + 300L, 0.70f * pulse, 0.0f,
                0.0f, 0.90f, 1.0f, 0.85f * env, distScale);
            PoseStack.Pose seal = pose.copy();
            seal.translate(0.0f, 0.0f, 0.20f);
            seal.rotate(Axis.ZP.rotationDegrees(-age * 45.0f));
            seal.rotate(Axis.XP.rotationDegrees(90.0f));
            BespokeGeometry.shardFan(seal, buffer, seed + 301L, 0.48f * pulse,
                0.0f, 0.95f, 1.0f, 0.90f, env, distScale);

            // 4. Terminus at the hit point: a displaced dome bellied back
            // toward the caster, a gold splash star on the contact plane,
            // and a white flash fan at its heart.
            PoseStack.Pose dome = pose.copy();
            dome.translate(0.0f, 0.0f, drawLength - 0.05f);
            dome.rotate(Axis.XP.rotationDegrees(90.0f));
            BespokeGeometry.displacedDome(dome, buffer, seed + 400L, 0.85f * pulse,
                0.0f, 0.95f, 1.0f, 0.75f, env, distScale);
            PoseStack.Pose splash = pose.copy();
            splash.translate(0.0f, 0.0f, drawLength);
            splash.rotate(Axis.ZP.rotationDegrees(age * 25.0f));
            splash.rotate(Axis.XP.rotationDegrees(90.0f));
            BespokeGeometry.shardFan(splash, buffer, seed + 401L, 1.1f * pulse,
                1.0f, 0.90f, 0.30f, 0.85f, env, distScale);
            PoseStack.Pose flash = pose.copy();
            flash.translate(0.0f, 0.0f, drawLength);
            flash.rotate(Axis.XP.rotationDegrees(90.0f));
            BespokeGeometry.glowFan(flash, buffer, seed + 402L, 0.50f * pulse, 0.0f,
                1.0f, 1.0f, 1.0f, 0.90f * env, distScale);
        });

        // Glow pass: a low alpha copy of the core and sheath so the beam
        // blooms as light instead of reading as matte ribbons.
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            float runLen = drawLength - fZStart;
            beamRibbon(pose, buffer, seed + 7L, age * 5.0f + 45.0f, fZStart, runLen,
                0.30f * pulse * fFpScale, 0.02f, 1.0f, 1.0f, 1.0f, 0.22f, env, distScale);
            beamRibbon(pose, buffer, seed + 8L, age * 3.0f + 45.0f, fZStart, runLen,
                0.88f * pulse * fFpScale, 0.55f, 0.05f, 0.70f, 1.0f, 0.10f, env, distScale);
        });

        poseStack.popPose();
    }

    /**
     * One beam layer: a torn ribbon started at zStart and run down the beam
     * axis. tornRibbon runs along its local +X in the XZ plane, so the pose
     * is rolled around the beam axis first, then turned so +X points down
     * +Z. Pose operations post-multiply, so the calls below compose as
     * translate, roll, turn when read against a vertex.
     */
    private static void beamRibbon(PoseStack.Pose base, VertexConsumer buffer, long seed,
                                   float rollDeg, float zStart, float length, float width, float bend,
                                   float r, float g, float b, float alpha, float env, float distScale) {
        PoseStack.Pose p = base.copy();
        p.translate(0.0f, 0.0f, zStart);
        p.rotate(Axis.ZP.rotationDegrees(rollDeg));
        p.rotate(Axis.YP.rotationDegrees(-90.0f));
        BespokeGeometry.tornRibbon(p, buffer, seed, length, width, bend, r, g, b, alpha, env, distScale);
    }
}
