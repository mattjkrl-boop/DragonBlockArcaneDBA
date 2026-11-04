package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.CurseGroundShatterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Curse Ground Shatter in Minecraft 26.2.
 * The bespoke pass rebuilds the shatter entirely on BespokeGeometry: the
 * fissure trenches are two crackStars (crimson sheath, pale core), the
 * shockwave is a brokenVortexBand over a glowFan pool, and the erupting
 * slabs are tornRibbons stood on end and leaning outward, all seeded from
 * the entity UUID so every client sees the same shatter.
 */
public class CurseGroundShatterRenderer extends EntityRenderer<CurseGroundShatterEntity, CurseGroundShatterRenderer.ShatterRenderState> {

    public CurseGroundShatterRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ShatterRenderState extends EntityRenderState {
        public float radius = 3.5f;
        public float age = 0;
        public int lifetime = 35;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(CurseGroundShatterEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(CurseGroundShatterEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ShatterRenderState createRenderState() {
        return new ShatterRenderState();
    }

    @Override
    public void extractRenderState(CurseGroundShatterEntity entity, ShatterRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.age = entity.tickCount + partialTicks;
        state.lifetime = entity.getLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(ShatterRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float radius = state.radius;
        float age = state.age;
        // state.lifetime is the fixed 35 tick total (it only decrements server
        // side), so the old age / (lifetime + age) formula stranded the fade at
        // about 0.5 when the entity vanished. Progress over the total reaches 0.
        float fade = Math.max(0.0f, 1.0f - age / (float) state.lifetime);
        long seed = state.seed;
        float ds = LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        RenderType renderType = KiRenderHelper.kiRenderType();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // 1. Expanding shockwave: a broken band over a violet glow pool.
            float shockR = Math.min(radius * 1.3f, age * 0.45f);
            if (fade > 0.05f && shockR > 0.1f) {
                BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x51A1L, shockR, 0.02f, 0.10f,
                    0.0f, 0.95f, 0.05f, 0.25f, fade * 0.85f, ds);
                BespokeGeometry.glowFan(pose, buffer, seed ^ 0x51A2L, shockR * 0.65f, 0.04f,
                    0.70f, 0.02f, 0.95f, fade * 0.75f, ds);
            }

            // 2. Radiating fissure trenches: crackStar sheath and core.
            BespokeGeometry.crackStar(pose, buffer, seed ^ 0xF155L, radius,
                0.95f, 0.05f, 0.20f, 0.95f, fade, ds);
            BespokeGeometry.crackStar(pose, buffer, seed ^ 0xF156L, radius * 0.85f,
                1.0f, 0.85f, 0.95f, 1.0f, fade, ds);

            // 3. Erupting slabs: torn ribbons stood on end, leaning outward
            // from the center, dark sheath with a crimson core.
            BespokeGeometry.Rng rng = new BespokeGeometry.Rng(seed);
            float growth = Math.min(1.0f, age * 0.4f);
            int rockCount = 12;
            for (int r = 0; r < rockCount; r++) {
                double rockAngle = (r / (double) rockCount) * Math.PI * 2.0 + rng.range(0.0f, 0.4f);
                float rockDist = radius * rng.range(0.35f, 0.90f);
                float rx = (float) Math.cos(rockAngle) * rockDist;
                float rz = (float) Math.sin(rockAngle) * rockDist;
                float slabHeight = rng.range(0.5f, 1.3f) * growth;
                float slabWidth = rng.range(0.35f, 0.65f);
                if (slabHeight <= 0.02f) continue;

                PoseStack.Pose slab = pose.copy();
                slab.translate(rx, 0.05f, rz);
                slab.rotate(Axis.YP.rotation((float) -rockAngle));
                slab.rotate(Axis.ZP.rotation((float) (Math.PI / 2.0 - 0.30)));
                long slabSeed = seed + r * 269L;
                BespokeGeometry.tornRibbon(slab, buffer, slabSeed, slabHeight, slabWidth, 0.15f,
                    0.15f, 0.05f, 0.22f, 0.95f, fade, ds);
                BespokeGeometry.tornRibbon(slab, buffer, slabSeed, slabHeight, slabWidth * 0.5f, 0.15f,
                    0.95f, 0.05f, 0.35f, 1.0f, fade, ds);
            }
        });

        // T1: additive glow echo of the expanding shock band, slightly wider
        // than the main pass, sharing the same corrected fade.
        if (fade > 0.05f) {
            float haloShockR = Math.min(radius * 1.3f, age * 0.45f) * 1.07f;
            if (haloShockR > 0.1f) {
                collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
                    BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x51A1L, haloShockR, 0.02f, 0.10f,
                        0.0f, 0.55f, 0.03f, 0.15f, fade * 0.30f, ds);
                    BespokeGeometry.glowFan(pose, buffer, seed ^ 0x51A2L, haloShockR * 0.60f, 0.04f,
                        0.42f, 0.01f, 0.57f, fade * 0.20f, ds);
                });
            }
        }
    }
}
