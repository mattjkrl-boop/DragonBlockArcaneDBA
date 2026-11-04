package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.ZGravitySlamEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

import java.util.Random;

/**
 * Entity Renderer for Z-Sword Gravity Slam in Minecraft 26.2.
 *
 * Bespoke pass: the dome is a seeded displaced dome squashed to the
 * spectacle pass height, the trenches are two layered crack stars (violet
 * base, gold core), the rim is a broken vortex band, the erupted monoliths
 * are torn ribbons stood upright and leaned outward, and the geysers are
 * plumes at seeded trench endpoints. All shapes take the entity's synced
 * seed, so the fracture pattern is frame stable and shared by every viewer.
 */
public class ZGravitySlamRenderer extends EntityRenderer<ZGravitySlamEntity, ZGravitySlamRenderer.ZGravitySlamRenderState> {

    public ZGravitySlamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ZGravitySlamRenderState extends EntityRenderState {
        public float radius = 10.0f;
        public float powerRatio = 1.0f;
        public float age = 0;
        public int lifetime = 50;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(ZGravitySlamEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(ZGravitySlamEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ZGravitySlamRenderState createRenderState() {
        return new ZGravitySlamRenderState();
    }

    @Override
    public void extractRenderState(ZGravitySlamEntity entity, ZGravitySlamRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.powerRatio = entity.getPowerRatio();
        state.age = entity.tickCount + partialTicks;
        state.lifetime = entity.getLifetime();
        state.seed = entity.getSeed();
    }

    @Override
    public void submit(ZGravitySlamRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float radius = state.radius;
        float power = state.powerRatio;
        float age = state.age;
        float progress = Math.min(1.0f, age / (float) state.lifetime);
        float fade = 1.0f - smooth(progress);
        float grow = smooth(Math.min(1.0f, progress / 0.48f));
        float impact = strike(progress);
        // Late decay factor: monoliths sink and trenches shorten as they fade
        // instead of standing at full height while only their alpha drops.
        float decay = smooth(Math.max(0.0f, (progress - 0.60f) / 0.40f));
        float dist = LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        RenderType renderType = KiRenderHelper.kiRenderType();

        float rCol = 1.0f;
        float gCol = 0.82f;
        float bCol = 0.20f;

        float shockR = radius * grow;
        if (fade > 0.04f && shockR > 0.05f) {
            float domeH = (2.2f + power * 2.6f) * (0.35f + 0.65f * fade);

            // 1. Shockwave dome: a displaced hemisphere squashed to domeH.
            poseStack.pushPose();
            poseStack.translate(0.0f, 0.05f, 0.0f);
            poseStack.scale(1.0f, domeH / shockR, 1.0f);
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                BespokeGeometry.displacedDome(pose, buffer, state.seed, shockR,
                    rCol, gCol, bCol, 0.5f, fade, dist));
            poseStack.popPose();

            // Rim wall and inner ground mark, then the trench crack stars.
            float trenchReach = radius * grow * (1.0f - decay * 0.45f);
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.brokenVortexBand(pose, buffer, state.seed ^ 0xB1L, shockR,
                    0.02f, 0.35f, age * 0.15f, rCol, gCol, bCol, fade * 0.72f, dist);
                BespokeGeometry.crackStar(pose, buffer, state.seed ^ 0xC2L, shockR * 0.36f,
                    1.0f, 0.96f, 0.72f, 1.0f, fade * (0.55f + 0.45f * impact), dist);
                // Radiating tectonic trenches: violet base star, gold core star.
                BespokeGeometry.crackStar(pose, buffer, state.seed ^ 0x7EL, trenchReach * 0.72f,
                    0.35f, 0.08f, 0.65f, 0.95f, fade, dist);
                BespokeGeometry.crackStar(pose, buffer, state.seed ^ 0xD3L, trenchReach * 0.58f,
                    1.0f, 0.95f, 0.45f, 1.0f, fade, dist);
            });
        }

        // 2. Erupted monolith slabs: torn ribbons stood upright, leaned
        // radially outward. Placement math is unchanged from the spectacle
        // pass; only the shape source changed.
        if (fade > 0.04f) {
            Random rng = new Random(state.seed);
            int rockCount = 18 + (int) (power * 10); // 18 to 28 monoliths
            for (int r = 0; r < rockCount; r++) {
                double rockAngle = (r / (double) rockCount) * Math.PI * 2.0 + (rng.nextDouble() * 0.35);
                float rockDist = radius * grow * (0.22f + rng.nextFloat() * 0.70f);

                float rx = (float) Math.cos(rockAngle) * rockDist;
                float rz = (float) Math.sin(rockAngle) * rockDist;

                float maxH = (1.8f + power * 2.2f) + rng.nextFloat() * (2.0f + power * 1.5f);
                // Monoliths sink back into the ground through the decay window.
                float slabHeight = maxH * grow * (1.0f - decay * 0.60f);
                float slabWidth = 0.22f + rng.nextFloat() * 0.16f;

                // Outward leaning angle
                float tiltFactor = 0.30f + rng.nextFloat() * 0.30f;
                if (slabHeight <= 0.05f) continue;

                long rockSeed = state.seed + r * 131L;
                float yawDeg = 180.0f - (float) Math.toDegrees(rockAngle);
                poseStack.pushPose();
                poseStack.translate(rx, 0.05f, rz);
                poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(yawDeg)));
                poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(90.0f)));
                collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                    BespokeGeometry.tornRibbon(pose, buffer, rockSeed, slabHeight, slabWidth * 1.1f,
                        slabHeight * tiltFactor, 0.65f, 0.20f, 0.95f, 0.95f, fade, dist);
                    BespokeGeometry.tornRibbon(pose, buffer, rockSeed + 1L, slabHeight * 0.94f, slabWidth * 0.42f,
                        slabHeight * tiltFactor, 0.85f, 0.45f, 1.0f, 0.9f, fade, dist);
                });
                poseStack.popPose();
            }
        }

        // 3. Vertical erupting singularity geysers at seeded trench
        // endpoints. They erupt on the strike envelope (peak near the damage
        // tick) and are gone by mid life.
        if (impact > 0.03f && fade > 0.04f) {
            float geyserH = (2.5f + power * 3.0f) * impact;
            float geyserW = 0.30f + power * 0.25f;
            BespokeGeometry.Rng grng = new BespokeGeometry.Rng(state.seed ^ 0x6E15L);
            for (int g = 0; g < 5; g++) {
                double gAngle = grng.range(0.0f, (float) (Math.PI * 2.0));
                float gDist = radius * grow * (1.0f - decay * 0.45f) * grng.range(0.7f, 1.05f);
                float gx = (float) Math.cos(gAngle) * gDist;
                float gz = (float) Math.sin(gAngle) * gDist;
                long geyserSeed = state.seed ^ 0x6E15L ^ (g * 77L);
                poseStack.pushPose();
                poseStack.translate(gx, 0.05f, gz);
                collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                    BespokeGeometry.plume(pose, buffer, geyserSeed, geyserW * 1.4f, geyserH,
                        1.0f, 0.90f, 0.30f, 0.95f, impact * fade, dist);
                    BespokeGeometry.plume(pose, buffer, geyserSeed + 3L, geyserW * 0.8f, geyserH * 1.15f,
                        0.60f, 0.15f, 0.95f, 0.7f, impact * fade, dist);
                });
                poseStack.popPose();
            }
        }

        // Anchor glow pass: a soft gold disc under the slam so the footprint
        // reads as grounded light, fading with the envelope.
        if (fade > 0.04f) {
            float glowR = radius * grow;
            collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) ->
                LayeredRender.drawAnchorGlow(pose, buffer,
                    glowR, 0.03f, rCol, gCol, bCol, fade * 0.30f, dist));
        }
    }

    private static float smooth(float t) {
        float x = Math.max(0.0f, Math.min(1.0f, t));
        return x * x * (3.0f - 2.0f * x);
    }

    private static float strike(float progress) {
        if (progress >= 0.42f) return 0.0f;
        return (float) Math.sin((progress / 0.42f) * Math.PI);
    }
}
