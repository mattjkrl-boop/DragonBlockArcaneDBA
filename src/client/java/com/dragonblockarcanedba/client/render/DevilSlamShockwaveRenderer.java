package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.DevilSlamShockwaveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Devil Slam Shockwave in Minecraft 26.2.
 * Bespoke pass: the whole picture is composed from BespokeGeometry builders
 * seeded by the entity UUID, so every client draws the same seal. A dark
 * glow pool and a wandering crack web form the seal disk, two shard fans
 * erupt as the brimstone spikes, a broken vortex band burns as the seal
 * rim at the hit radius, and stacked plumes replace the old cylinder
 * pillar as the hellfire column. No local geometry helpers remain.
 */
public class DevilSlamShockwaveRenderer extends EntityRenderer<DevilSlamShockwaveEntity, DevilSlamShockwaveRenderer.DevilSlamRenderState> {

    public DevilSlamShockwaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class DevilSlamRenderState extends EntityRenderState {
        public float radius = 5.0f;
        public float age = 0;
        public int lifetime = 35;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(DevilSlamShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(DevilSlamShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public DevilSlamRenderState createRenderState() {
        return new DevilSlamRenderState();
    }

    @Override
    public void extractRenderState(DevilSlamShockwaveEntity entity, DevilSlamRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.age = entity.tickCount + partialTicks;
        state.lifetime = entity.getLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(DevilSlamRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        final float radius = state.radius;
        final float age = state.age;
        final float lifeT = Math.min(1.0f, age / (float) state.lifetime);
        final float fade = 1.0f - lifeT * lifeT * (3.0f - 2.0f * lifeT);
        final long seed = state.seed;
        final float growT = Math.min(1.0f, age / 10.0f);
        final float grow = 1.0f - (1.0f - growT) * (1.0f - growT) * (1.0f - growT);
        // Telegraph honesty (standard point 5): the seal's outer edge IS the hit
        // radius now, not a picture a third wider than the damage. The traveling
        // follow band still runs past it as a fading echo, and deals nothing.
        final float shockR = radius * 1.05f * grow;
        // Distance scaling (standard point 1): builder segment counts step down
        // beyond 32 and 64 blocks; near views keep the full seal.
        final float segScale = com.dragonblockarcanedba.client.render.ki.LayeredRender.distScale(
            cameraState, state.x, state.y, state.z);
        final float pillarT = Math.min(1.0f, age / 18.0f);
        final float pillarEase = pillarT * pillarT * (3.0f - 2.0f * pillarT);
        final float pillarAlpha = (1.0f - pillarEase) * (1.0f - pillarEase);
        final float riseT = Math.min(1.0f, age / 5.5f);
        final float pillarHeight = 6.7f * (1.0f - (1.0f - riseT) * (1.0f - riseT));
        final float pillarRadius = 0.72f * (1.0f + pillarEase * 0.28f);
        final float twist = age * 0.05f;
        float followT = Math.min(1.0f, Math.max(0.0f, (age - 5.0f) / 14.0f));
        float followEase = followT * followT * (3.0f - 2.0f * followT);
        final float followR = radius * (1.2f + 0.9f * followEase);
        final float followA = fade * (1.0f - followEase) * (1.0f - followEase);
        RenderType renderType = KiRenderHelper.kiRenderType();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // Seal disk: a dark glow pool under a wandering crack web, with a
            // hotter crack web nested inside. The cracks are the fissure
            // trenches and the brimstone disk marks in one bespoke system.
            if (fade > 0.04f) {
                BespokeGeometry.glowFan(pose, buffer, seed + 11, shockR * 0.55f, 0.045f,
                    0.42f, 0.0f, 0.05f, fade * 0.5f, segScale);
                BespokeGeometry.crackStar(pose, buffer, seed, shockR,
                    0.5f, 0.01f, 0.05f, 0.9f, fade, segScale);
                BespokeGeometry.crackStar(pose, buffer, seed + 7, shockR * 0.8f,
                    1.0f, 0.22f, 0.1f, 0.7f, fade, segScale);
                // Erupting brimstone shards across the disk, dark shells with a
                // hotter inner field: the spikes, grown in with the seal.
                BespokeGeometry.shardFan(pose, buffer, seed + 13, shockR * 0.98f,
                    0.35f, 0.02f, 0.05f, fade, grow, segScale);
                BespokeGeometry.shardFan(pose, buffer, seed + 29, shockR * 0.6f,
                    1.0f, 0.25f, 0.1f, fade * 0.85f, grow, segScale);
                // The seal rim: a broken, slowly turning band of burning wall
                // at the hit edge, plus the traveling follow echo outside it.
                BespokeGeometry.brokenVortexBand(pose, buffer, seed + 3, shockR * 0.85f,
                    0.03f, 0.17f, twist, 1.0f, 0.22f, 0.1f, fade, segScale);
                if (followA > 0.03f) {
                    BespokeGeometry.brokenVortexBand(pose, buffer, seed + 5, followR * 0.85f,
                        0.03f, 0.1f, twist, 0.7f, 0.02f, 0.06f, followA, segScale);
                }
                // Bright core pool at the heart of the seal.
                BespokeGeometry.glowFan(pose, buffer, seed + 19, Math.max(0.2f, shockR * 0.16f), 0.09f,
                    1.0f, 0.35f, 0.14f, fade, segScale);
            }

            // Hellfire column: stacked leaning plumes instead of nested
            // cylinders, a dark gout outside and a hotter stack inside.
            if (pillarAlpha > 0.025f && pillarHeight > 0.15f) {
                BespokeGeometry.plume(pose, buffer, seed + 17, pillarRadius * 2.4f, pillarHeight,
                    0.55f, 0.0f, 0.06f, 0.55f, pillarAlpha, segScale);
                BespokeGeometry.plume(pose, buffer, seed + 23, pillarRadius * 1.15f, pillarHeight * 1.02f,
                    0.85f, 0.05f, 0.08f, 0.8f, pillarAlpha, segScale);
            }
        });

        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            if (fade > 0.04f) {
                BespokeGeometry.glowFan(pose, buffer, seed + 19, Math.max(0.2f, shockR * 0.16f), 0.08f,
                    1.0f, 0.3f, 0.12f, fade * 0.28f, segScale);
                float followAdd = followA * 0.22f;
                if (followAdd > 0.02f) {
                    BespokeGeometry.brokenVortexBand(pose, buffer, seed + 5, followR * 0.85f,
                        0.03f, 0.09f, twist, 0.9f, 0.08f, 0.08f, followAdd, segScale);
                }
            }
            if (pillarAlpha > 0.025f && pillarHeight > 0.15f) {
                BespokeGeometry.plume(pose, buffer, seed + 31, pillarRadius * 0.5f, pillarHeight * 1.05f,
                    1.0f, 0.22f, 0.1f, 0.35f, pillarAlpha, segScale);
            }
        });
    }
}
