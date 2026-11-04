package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.DarknessShatterEntity;
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
 * Entity Renderer for Darkness Void Shatter & Shockwave in Minecraft 26.2.
 *
 * Bespoke pass: the hand-rolled trench, slab, ring, and column helpers
 * are gone. The tiered shockwave is stacked BespokeGeometry.glowFan
 * discs, the fissures are crackStar bursts (wide violet, then a brighter
 * narrow core), the erupting slabs are branch shards leaning outward on
 * seeded placements from BespokeGeometry.Rng, and the eruption column is
 * a spinning plume. The seed still comes from the entity UUID, and the
 * corrected fade (progress over the fixed 40 tick life) is unchanged.
 */
public class DarknessShatterRenderer extends EntityRenderer<DarknessShatterEntity, DarknessShatterRenderer.ShatterRenderState> {

    public DarknessShatterRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ShatterRenderState extends EntityRenderState {
        public float radius = 4.0f;
        public float age = 0;
        public int lifetime = 40;
        public long seed = 0;
        public double px = 0, py = 0, pz = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(DarknessShatterEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(DarknessShatterEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ShatterRenderState createRenderState() {
        return new ShatterRenderState();
    }

    @Override
    public void extractRenderState(DarknessShatterEntity entity, ShatterRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.age = entity.tickCount + partialTicks;
        state.lifetime = entity.getLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
        state.px = entity.getX();
        state.py = entity.getY();
        state.pz = entity.getZ();
    }

    @Override
    public void submit(ShatterRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        final float radius = state.radius;
        final float age = state.age;
        // state.lifetime is the fixed 40 tick total (it only decrements server
        // side), so the old age / (lifetime + age) formula stranded the fade at
        // about 0.5 when the entity vanished. Progress over the total reaches 0.
        final float fade = Math.max(0.0f, 1.0f - age / (float) state.lifetime);
        final float ds = LayeredRender.distScale(cameraState, state.px, state.py, state.pz);
        final long seed = state.seed;
        RenderType renderType = KiRenderHelper.kiRenderType();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // 1. Expanding multi-tiered shockwave: stacked glow fans, the
            // wide violet disc under a crimson one under the obsidian core,
            // expanding at the old rate.
            float shockR = Math.min(radius * 1.35f, age * 0.55f);
            if (fade > 0.05f) {
                BespokeGeometry.glowFan(pose, buffer, seed ^ 0x510C1L, shockR, 0.04f,
                    0.55f, 0.05f, 0.90f, fade * 0.88f, ds);
                BespokeGeometry.glowFan(pose, buffer, seed ^ 0x510C2L, shockR * 0.71f, 0.05f,
                    0.95f, 0.08f, 0.35f, fade * 0.92f, ds);
                BespokeGeometry.glowFan(pose, buffer, seed ^ 0x510C3L, shockR * 0.58f, 0.06f,
                    0.12f, 0.0f, 0.22f, fade * 0.95f, ds);
            }

            // 2. Radiating fissures: a wide violet crack star with a
            // brighter, narrower core star inside it.
            BespokeGeometry.crackStar(pose, buffer, seed ^ 0xC9AC1L, radius * 0.95f,
                0.65f, 0.05f, 0.95f, 0.95f, fade, ds);
            BespokeGeometry.crackStar(pose, buffer, seed ^ 0xC9AC2L, radius * 0.55f,
                1.0f, 0.20f, 0.50f, 1.0f, fade, ds);

            // 3. 12 erupting obsidian slabs leaning outward, placed by the
            // seeded toolkit stream on the old distributions.
            BespokeGeometry.Rng rng = new BespokeGeometry.Rng(seed ^ 0x51ABL);
            int rockCount = 12;
            float slabGrow = Math.min(1.0f, age * 0.45f);
            for (int r = 0; r < rockCount; r++) {
                double rockAngle = (r / (double) rockCount) * Math.PI * 2.0 + (rng.nextFloat() * 0.4f);
                float rockDist = radius * (0.35f + rng.nextFloat() * 0.55f);
                float rx = (float) Math.cos(rockAngle) * rockDist;
                float rz = (float) Math.sin(rockAngle) * rockDist;
                float slabHeight = (0.6f + rng.nextFloat() * 0.9f) * slabGrow;
                float slabWidth = 0.38f + rng.nextFloat() * 0.32f;
                float tiltX = (float) Math.cos(rockAngle) * 0.35f;
                float tiltZ = (float) Math.sin(rockAngle) * 0.35f;
                BespokeGeometry.branch(pose, buffer, seed ^ (0x51ABL + r * 71L),
                    new Vec3(rx, 0.05f, rz), new Vec3(rx + tiltX, slabHeight, rz + tiltZ),
                    slabWidth * 0.5f, 0.75f, 0.05f, 0.95f, fade, 1.0f, ds);
            }
        });

        // 4. Central vertical eruption column: a spinning plume growing at
        // the old rate, its radius thinning as the shatter ages.
        if (fade > 0.1f) {
            final float colHeight = (radius * 1.8f) * Math.min(1.0f, age * 0.3f);
            final float colRadius = (radius * 0.25f) * (1.0f - (age / (float) state.lifetime) * 0.5f);
            poseStack.pushPose();
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(age * 8.594f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.plume(pose, buffer, seed ^ 0xB10E1L, colRadius, colHeight,
                    0.75f, 0.05f, 0.95f, 0.75f, fade, ds);
            });
            poseStack.popPose();
        }

        // Additive glow echo of the shock discs, slightly wider than the
        // main pass, sharing the same corrected fade.
        if (fade > 0.05f) {
            final float haloShockR = Math.min(radius * 1.35f, age * 0.55f) * 1.06f;
            collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
                BespokeGeometry.glowFan(pose, buffer, seed ^ 0x510C4L, haloShockR, 0.04f,
                    0.33f, 0.03f, 0.54f, fade * 0.30f, ds);
                BespokeGeometry.glowFan(pose, buffer, seed ^ 0x510C5L, haloShockR * 0.55f, 0.06f,
                    0.40f, 0.02f, 0.60f, fade * 0.20f, ds);
            });
        }
    }
}
