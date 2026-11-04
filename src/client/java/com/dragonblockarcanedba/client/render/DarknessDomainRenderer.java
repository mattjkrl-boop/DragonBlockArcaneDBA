package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.DarknessDomainEntity;
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
 * Entity Renderer for Darkness Sword (World of Darkness & Abyssal Eclipse) in Minecraft 26.2.
 *
 * Bespoke pass: the hand-rolled lat/long dome mesh, ring helper, and shard
 * pyramid helper are gone. The dome is two counter-rotating
 * BespokeGeometry.displacedDome shells (noise-displaced, seeded from the
 * entity UUID), the ground whirlpools and vortex column are
 * brokenVortexBand tubes, and the 16 orbiting spires are branch shards
 * leaning inward on the same orbit math as before. The shared
 * LayeredRender.drawVortexBands call around the funnel is unchanged.
 * Lightning is still not drawn here: the domain entity fires real branched
 * BoltEntity strikes, which have their own renderer. Every alpha is
 * multiplied by the entity's synced lifecycle fade, and segment counts
 * scale with camera distance through the shared standard.
 */
public class DarknessDomainRenderer extends EntityRenderer<DarknessDomainEntity, DarknessDomainRenderer.DarknessDomainRenderState> {

    public DarknessDomainRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class DarknessDomainRenderState extends EntityRenderState {
        public float radius = 8.0f;
        public float age = 0;
        public float fade = 0;
        public double px = 0, py = 0, pz = 0;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(DarknessDomainEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(DarknessDomainEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public DarknessDomainRenderState createRenderState() {
        return new DarknessDomainRenderState();
    }

    @Override
    public void extractRenderState(DarknessDomainEntity entity, DarknessDomainRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.age = entity.tickCount + partialTicks;
        state.fade = entity.getFade();
        state.px = entity.getX();
        state.py = entity.getY();
        state.pz = entity.getZ();
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(DarknessDomainRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        final float radius = state.radius;
        final float age = state.age;
        // Lifecycle fade from the entity: the dome builds in and dissipates,
        // it never pops. Distance scale steps segment counts down and
        // softens alpha far away.
        final float fade = Math.max(0.0f, Math.min(1.0f, state.fade));
        if (fade <= 0.01f) {
            return;
        }
        final float ds = LayeredRender.distScale(cameraState, state.px, state.py, state.pz);
        final float aMul = fade * (float) Math.sqrt(ds);
        final long seed = state.seed;
        final float domeHeight = Math.min(26.0f, radius * 1.35f);
        RenderType renderType = KiRenderHelper.kiRenderType();

        // 1. Quad-tier counter-rotating ground whirlpools: broken vortex
        // bands hugging the floor, each in its own rotated pose so the gap
        // patterns sweep at the old per-band speeds and directions.
        for (int band = 0; band < 4; band++) {
            final float bandRadius = radius * (0.30f + band * 0.23f) * 0.915f;
            final float bandY = 0.08f + (band * 0.05f);
            final float rotDeg = age * (band % 2 == 0 ? 1.0f : -1.3f) * (10.0f + band * 5.0f);
            final float bandAlpha = (0.65f - (band * 0.08f)) * aMul;
            final float red = band % 2 == 0 ? 0.65f : 0.20f;
            final float blue = band % 2 == 0 ? 0.95f : 0.45f;
            final long bandSeed = seed ^ (0xBA9DL + band * 977L);
            final float bandTwist = band % 2 == 0 ? 1.5f : -1.9f;
            poseStack.pushPose();
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(rotDeg)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.brokenVortexBand(pose, buffer, bandSeed, bandRadius, bandY, bandY + 0.06f,
                    bandTwist, red, 0.02f, blue, bandAlpha, ds);
            });
            poseStack.popPose();
        }

        // 2. The dome: two displaced dome shells counter-rotating. The
        // outer shell sits at ground level, the inner crimson one a block
        // up and slightly smaller, as the old inner mesh did.
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(age * 3.5f)));
        poseStack.translate(0, 1.0f, 0);
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xD09E1L, radius,
                0.15f, 0.0f, 0.24f, 0.55f, aMul, ds);
        });
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-age * 5.5f)));
        poseStack.translate(0, 2.0f, 0);
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xD09E2L, radius * 0.85f,
                0.78f, 0.03f, 0.45f, 0.39f, aMul, ds);
        });
        poseStack.popPose();

        // 3. 16 orbiting jagged obsidian spires leaning inward, drawn as
        // branch shards on the unchanged orbit math.
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            int spireCount = 16;
            float spireOrbitR = radius * 0.92f;
            float spireRot = age * 12.0f;
            for (int i = 0; i < spireCount; i++) {
                double sAngle = (i / (double) spireCount) * Math.PI * 2.0 + Math.toRadians(spireRot);
                float sx = (float) Math.cos(sAngle) * spireOrbitR;
                float sz = (float) Math.sin(sAngle) * spireOrbitR;
                float sy = 1.0f + (float) Math.sin(age * 0.15f + i * 0.8f) * 0.8f;
                float spireHeight = 3.2f + (i % 3 == 0 ? 1.5f : 0.0f);
                float leanX = -(float) Math.cos(sAngle) * 0.6f;
                float leanZ = -(float) Math.sin(sAngle) * 0.6f;
                BespokeGeometry.branch(pose, buffer, seed ^ (0x591E1L + i * 131L),
                    new Vec3(sx, sy, sz), new Vec3(sx + leanX, sy + spireHeight, sz + leanZ),
                    0.35f, 0.75f, 0.05f, 0.95f, aMul, aMul, ds);
            }
        });

        // 4. Central vortex column: two counter-rotating broken bands, a
        // violet outer tube and a warmer crimson core, spinning at the old
        // funnel speeds.
        final float eyeRadius = Math.max(1.8f, radius * 0.16f);
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(age * 22.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xE7E1L, eyeRadius * 0.85f,
                0f, domeHeight, 2.5f, 0.68f, 0.03f, 0.70f, 0.45f * aMul, ds);
        });
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-age * 30.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xE7E2L, eyeRadius * 0.55f,
                0f, domeHeight, -2.0f, 0.90f, 0.03f, 0.60f, 0.35f * aMul, ds);
        });
        poseStack.popPose();

        // 5. Two rim bands circling the dome wall where the old rim rings
        // floated, turning together at the old slow rate.
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(age * 5.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xB191L, radius * 0.955f,
                1.0f, 1.1f, 0.8f, 0.72f, 0.05f, 0.95f, 0.50f * aMul, ds);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xB192L, radius * 0.82f,
                0.5f, 0.6f, -0.8f, 0.35f, 0.02f, 0.70f, 0.20f * aMul, ds);
        });
        poseStack.popPose();

        // Additive pass: a halo band around the vortex column, the shared
        // ground anchor glow, and the shared layered vortex bands around
        // the funnel, all driven by the same fade as the rest of the dome.
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(age * 22.0f)));
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x4A10L, eyeRadius * 1.45f * 0.85f,
                0f, domeHeight, 2.5f, 0.35f, 0.03f, 0.50f, 0.15f * aMul, ds);
        });
        poseStack.popPose();

        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            LayeredRender.drawAnchorGlow(pose, buffer, radius * 0.50f, 0.06f,
                0.30f, 0.02f, 0.45f, 0.10f * aMul, ds);
            LayeredRender.drawVortexBands(pose, buffer,
                Math.max(1.8f, radius * 0.16f) * 1.05f, domeHeight, age,
                0.45f, 0.05f, 0.85f, fade, ds);
        });
    }
}
