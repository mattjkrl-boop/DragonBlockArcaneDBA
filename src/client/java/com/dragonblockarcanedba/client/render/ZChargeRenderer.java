package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.ZChargeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * Entity Renderer for Z-Sword Shockwave Charging in Minecraft 26.2.
 *
 * Bespoke pass: the ground mandala is a glow fan plus a slowly rotating
 * crack star and a flat vortex band, the funnel is three stacked broken
 * vortex bands narrowing into the sword, the celestial rings are thin
 * broken vortex bands on tilted poses, the levitating prisms are shard
 * fans tumbling in place, and the overdrive flares are jagged branches.
 * All shapes take the entity's synced seed and stay frame stable.
 */
public class ZChargeRenderer extends EntityRenderer<ZChargeEntity, ZChargeRenderer.ZChargeRenderState> {

    public ZChargeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ZChargeRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public float age = 0;
        public long seed = 0;
        public int fadeTicks = -1;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(ZChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(ZChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ZChargeRenderState createRenderState() {
        return new ZChargeRenderState();
    }

    @Override
    public void extractRenderState(ZChargeEntity entity, ZChargeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getSeed();
        state.fadeTicks = entity.getFadeTicks();
    }

    @Override
    public void submit(ZChargeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        float charge = state.chargeRatio;
        float age = state.age;
        float dist = LayeredRender.distScale(cameraState, state.x, state.y, state.z);

        float baseRadius = 1.2f + (charge * 2.2f);
        // Spawn fade-in over the first 4 ticks so the charge construct does not
        // pop in at full brightness, and a matching fade out over the entity's
        // FADE_LENGTH ticks on release: the construct used to vanish in one
        // tick at full alpha when the item discarded it.
        float fadeOut = state.fadeTicks < 0 ? 1.0f
            : Math.max(0.0f, 1.0f - state.fadeTicks / (float) ZChargeEntity.FADE_LENGTH);
        float baseAlpha = (0.70f + (charge * 0.30f)) * Math.min(1.0f, age / 4.0f) * fadeOut;
        float tremble = (float) Math.sin(age * 3.5f) * (0.02f + charge * 0.08f);
        float coreY = 1.0f; // Sword position

        // 1. Radiating Divine Ground Seal Mandala: glow fan bed, a slowly
        // rotating crack star for the sacred spokes, and a flat broken band
        // as the inner seal ring.
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.glowFan(pose, buffer, state.seed, baseRadius * 1.05f, 0.04f,
                1.0f, 0.85f, 0.15f, baseAlpha * 0.5f, dist);
            BespokeGeometry.brokenVortexBand(pose, buffer, state.seed ^ 0x1B2L, baseRadius * 0.68f,
                0.03f, 0.10f, -age * 0.244f, 1.0f, 0.95f, 0.40f, baseAlpha * 0.95f, dist);
        });
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(age * 8.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.crackStar(pose, buffer, state.seed ^ 0x5EEDL, baseRadius * 1.05f,
                1.0f, 0.80f, 0.10f, 1.0f, baseAlpha * 0.80f, dist));
        poseStack.popPose();

        // 2. Vortex funnel drawing into the sword hilt: three stacked broken
        // bands, wide at the top and narrow at coreY, twisting with age.
        float funnelHeight = 2.4f + charge * 1.4f;
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            for (int i = 0; i < 3; i++) {
                float frac = (i + 0.5f) / 3.0f;
                float bandRadius = 0.2f + (1.0f - frac) * baseRadius;
                float y0 = coreY + funnelHeight * (i / 3.0f);
                float y1 = coreY + funnelHeight * ((i + 1) / 3.0f + 0.08f);
                BespokeGeometry.brokenVortexBand(pose, buffer, state.seed ^ (0xF00L + i * 17L), bandRadius,
                    y0, y1, 4.19f + age * 0.35f + i * 1.3f,
                    1.0f, 0.85f, 0.20f, baseAlpha * (0.85f - i * 0.15f), dist);
            }
        });

        // 3. Counter-rotating celestial rings: thin broken bands, the two
        // outer ones on tilted poses, the compression pair flat at coreY.
        poseStack.pushPose();
        poseStack.translate(0.0f, coreY + 0.3f, 0.0f);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(25.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.brokenVortexBand(pose, buffer, state.seed ^ 0xB101L, baseRadius * 0.90f,
                -0.08f, 0.08f, age * 0.28f, 1.0f, 0.75f, 0.05f, baseAlpha * 0.85f, dist));
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0f, coreY - 0.2f, 0.0f);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(-30.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.brokenVortexBand(pose, buffer, state.seed ^ 0xB102L, baseRadius * 0.65f,
                -0.07f, 0.07f, -age * 0.38f, 1.0f, 0.90f, 0.30f, baseAlpha * 0.90f, dist));
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0f, coreY, 0.0f);
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.brokenVortexBand(pose, buffer, state.seed ^ 0xB103L, baseRadius * 0.40f,
                -0.06f, 0.06f, age * 0.52f, 1.0f, 1.0f, 0.90f, baseAlpha, dist);
            BespokeGeometry.brokenVortexBand(pose, buffer, state.seed ^ 0xB104L, baseRadius * 0.16f,
                -0.05f, 0.05f, -age * 0.73f, 1.0f, 1.0f, 1.0f, baseAlpha, dist);
        });
        poseStack.popPose();

        // 4. Levitating crystal prisms: shard fans tumbling in place.
        // Placement math (orbit, distance, float, size) is unchanged from
        // the spectacle pass; only the octahedron shape became a fan.
        int prismCount = 8 + (int) (charge * 6); // 8 to 14 prisms
        Random rng = new Random(state.seed);
        for (int r = 0; r < prismCount; r++) {
            double prismAng = (r / (double) prismCount) * Math.PI * 2.0 + (age * 0.08f);
            float prismDist = 0.5f + rng.nextFloat() * (baseRadius * 0.75f);

            float px = (float) Math.cos(prismAng) * prismDist + (rng.nextFloat() - 0.5f) * tremble;
            float pz = (float) Math.sin(prismAng) * prismDist + (rng.nextFloat() - 0.5f) * tremble;

            float floatPhase = age * 0.3f + r * 1.6f;
            float py = coreY + (float) Math.sin(floatPhase) * (0.35f + charge * 0.45f);

            float pSize = 0.15f + rng.nextFloat() * 0.15f + charge * 0.10f;
            float pHeight = 0.25f + rng.nextFloat() * 0.20f + charge * 0.15f;

            // Each prism tumbles on its own axis at its own speed.
            float tumble = age * (0.5f + (r % 5) * 0.17f) + r * 1.7f;
            long prismSeed = state.seed + r * 17L;
            poseStack.pushPose();
            poseStack.translate(px, py, pz);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees((float) Math.toDegrees(tumble))));
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.shardFan(pose, buffer, prismSeed, pHeight * 1.05f,
                    1.0f, 0.90f, 0.30f, 1.0f, baseAlpha * 0.95f, dist);
                BespokeGeometry.shardFan(pose, buffer, prismSeed + 1L, pHeight * 0.58f,
                    1.0f, 1.0f, 0.90f, 1.0f, baseAlpha, dist);
            });
            poseStack.popPose();
        }

        // 5. Overdrive Solar Ray Flares (charge >= 0.4f): jagged branches
        // radiating from the core at sword height.
        if (charge >= 0.4f) {
            int flareCount = 6 + (int) (charge * 6);
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                for (int i = 0; i < flareCount; i++) {
                    double fAng = (i / (double) flareCount) * Math.PI * 2.0 + (age * 0.15f);
                    float fLen = (1.2f + charge * 1.8f) * (0.8f + 0.2f * (float) Math.sin(age * 0.6f + i));
                    float fx = (float) Math.cos(fAng) * fLen;
                    float fz = (float) Math.sin(fAng) * fLen;
                    float fy = coreY + (float) Math.sin(i * 2.1f) * 0.5f;
                    BespokeGeometry.branch(pose, buffer, state.seed ^ (0xF1AL + i * 31L),
                        new Vec3(0.0f, coreY, 0.0f), new Vec3(fx, fy, fz), 0.10f + charge * 0.08f,
                        1.0f, 1.0f, 0.85f, 1.0f, baseAlpha * 0.90f, dist);
                }
            });
        }

        // Additive halo pass: a wider faint glow fan on the ground and a
        // faint tilted band, so the charge blooms and anchors to the ground.
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) ->
            BespokeGeometry.glowFan(pose, buffer, state.seed ^ 0xAA10L, baseRadius * 1.18f, 0.04f,
                1.0f, 0.85f, 0.15f, baseAlpha * 0.15f, dist));
        poseStack.pushPose();
        poseStack.translate(0.0f, 1.3f, 0.0f);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(25.0f)));
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) ->
            BespokeGeometry.brokenVortexBand(pose, buffer, state.seed ^ 0xAA11L, baseRadius * 0.90f,
                -0.10f, 0.10f, age * 0.28f, 1.0f, 0.75f, 0.05f, baseAlpha * 0.16f, dist));
        poseStack.popPose();

        super.submit(state, poseStack, collector, cameraState);
    }
}
