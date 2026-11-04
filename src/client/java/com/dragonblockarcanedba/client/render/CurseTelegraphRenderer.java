package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.CurseTelegraphEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Curse Telegraph in Minecraft 26.2.
 * The bespoke pass rebuilds the warning marker entirely on
 * BespokeGeometry: a crackStar ground star (crimson over violet) in place
 * of the equal-segment pentagram rings, a brokenVortexBand as the
 * contracting implosion indicator, tornRibbon tendrils rising around the
 * rim, and a displacedDome hex eye at the heart that flashes as the
 * countdown completes. Seeds come from the entity id, identical on every
 * client.
 */
public class CurseTelegraphRenderer extends EntityRenderer<CurseTelegraphEntity, CurseTelegraphRenderer.TelegraphRenderState> {

    public CurseTelegraphRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class TelegraphRenderState extends EntityRenderState {
        public float radius = 1.8f;
        public float progress = 0.0f;
        public float age = 0;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(CurseTelegraphEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(CurseTelegraphEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public TelegraphRenderState createRenderState() {
        return new TelegraphRenderState();
    }

    @Override
    public void extractRenderState(CurseTelegraphEntity entity, TelegraphRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.progress = entity.getProgress(partialTicks);
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getId() * 0x9E3779B97F4A7C15L + 0x7E1L;
    }

    @Override
    public void submit(TelegraphRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float radius = state.radius;
        float progress = state.progress;
        float age = state.age;
        long seed = state.seed;
        float ds = LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        RenderType renderType = KiRenderHelper.kiRenderType();

        // Pulse rate accelerates as countdown reaches completion
        float pulseSpeed = 0.3f + (progress * 0.7f);
        float pulse = 0.8f + 0.2f * (float) Math.sin(age * pulseSpeed * 20.0f);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // 1. Corrupted ground star: two crackStars, crimson over violet.
            BespokeGeometry.crackStar(pose, buffer, seed, radius,
                0.95f, 0.02f, 0.15f, 0.85f, pulse, ds);
            BespokeGeometry.crackStar(pose, buffer, seed ^ 0x57A2L, radius * 0.75f,
                0.65f, 0.01f, 0.85f, 0.75f, pulse, ds);

            // 2. Contracting implosion indicator: a broken band shrinking
            // from the full radius to nothing as progress reaches 1.
            float shrinkR = radius * (1.0f - progress);
            if (shrinkR > 0.05f) {
                BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xB4D1L, shrinkR, 0.02f, 0.12f,
                    age * 0.7f, 1.0f, 0.9f, 0.95f, 0.95f, ds);
            }

            // 3. Rising cursed tendrils: torn ribbons stood on end around
            // the rim, crimson sheath with a white core on the same seed.
            int pillarCount = 6;
            float pillarH = 1.2f + (progress * 2.5f);
            float pillarW = 0.16f * (1.0f - progress * 0.3f);
            for (int p = 0; p < pillarCount; p++) {
                double pAngle = (p / (double) pillarCount) * Math.PI * 2.0 + Math.toRadians(age * 15.0f);
                float px = (float) Math.cos(pAngle) * radius;
                float pz = (float) Math.sin(pAngle) * radius;
                PoseStack.Pose tendril = pose.copy();
                tendril.translate(px, 0.05f, pz);
                tendril.rotate(Axis.ZP.rotation((float) (Math.PI / 2.0)));
                long tSeed = seed + p * 131L;
                BespokeGeometry.tornRibbon(tendril, buffer, tSeed, pillarH, pillarW, 0.35f,
                    0.95f, 0.02f, 0.20f, 0.80f, pulse, ds);
                BespokeGeometry.tornRibbon(tendril, buffer, tSeed, pillarH, pillarW * 0.38f, 0.35f,
                    1.0f, 0.95f, 0.95f, 0.95f, pulse, ds);
            }

            // 4. Central hex eye: a lumpy displaced dome flashing at the
            // heart of the marker.
            PoseStack.Pose eye = pose.copy();
            eye.translate(0, 0.08f, 0);
            float coreSize = (0.25f + (progress * 0.25f)) * pulse;
            BespokeGeometry.displacedDome(eye, buffer, seed ^ 0xE7E1L, coreSize * 1.6f,
                1.0f, 0.05f, 0.15f, 0.95f, pulse, ds);
        });

        // T1: additive warning glow tracing the contracting band and the
        // hex eye, so the strike marker burns brighter as the countdown
        // completes.
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            float shrinkR = radius * (1.0f - progress);
            if (shrinkR > 0.05f) {
                BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xB4D1L, shrinkR * 1.08f, 0.02f, 0.12f,
                    age * 0.7f, 0.55f, 0.50f, 0.52f, 0.25f, ds);
            }

            PoseStack.Pose eye = pose.copy();
            eye.translate(0, 0.08f, 0);
            float coreSize = (0.25f + (progress * 0.25f)) * pulse * 1.5f;
            BespokeGeometry.displacedDome(eye, buffer, seed ^ 0xE7E1L, coreSize * 1.6f,
                0.55f, 0.03f, 0.08f, 0.30f, pulse, ds);
        });
    }
}
