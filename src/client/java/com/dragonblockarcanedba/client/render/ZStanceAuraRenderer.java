package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.ZStanceAuraEntity;
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
 * Entity Renderer for Z-Sword Katchin Weight & Gravity Stance in Minecraft 26.2.
 *
 * Bespoke pass: the gravity well is a stack of broken vortex bands widening
 * with height (the stance well builder named in the standard), the ground
 * disc and singularity eye are glow fans with flat broken bands, the
 * contracting event horizon rings are broken bands shrinking on the same
 * phase math, the monoliths are torn ribbons stood upright and leaned
 * outward, and the runes above them are small shard fans. All shapes take
 * the entity's synced seed and stay frame stable.
 */
public class ZStanceAuraRenderer extends EntityRenderer<ZStanceAuraEntity, ZStanceAuraRenderer.ZStanceRenderState> {

    public ZStanceAuraRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ZStanceRenderState extends EntityRenderState {
        public int heldTicks = 0;
        public float powerRatio = 0.0f;
        public float age = 0;
        public long seed = 0;
        public int fadeTicks = -1;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(ZStanceAuraEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(ZStanceAuraEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ZStanceRenderState createRenderState() {
        return new ZStanceRenderState();
    }

    @Override
    public void extractRenderState(ZStanceAuraEntity entity, ZStanceRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.heldTicks = entity.getHeldTicks();
        state.powerRatio = entity.getPowerRatio();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getSeed();
        state.fadeTicks = entity.getFadeTicks();
    }

    @Override
    public void submit(ZStanceRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        float power = state.powerRatio;
        float age = state.age;
        float dist = LayeredRender.distScale(cameraState, state.x, state.y, state.z);

        float baseRadius = 2.0f + (power * 3.5f);
        // The alpha used to have no age term at all: the field appeared at 0.70
        // alpha on its first tick and vanished whole on release. Fade in over
        // 5 ticks, fade out over the entity's FADE_LENGTH ticks.
        float fadeOut = state.fadeTicks < 0 ? 1.0f
            : Math.max(0.0f, 1.0f - state.fadeTicks / (float) ZStanceAuraEntity.FADE_LENGTH);
        float baseAlpha = (0.70f + (power * 0.28f)) * Math.min(1.0f, age / 5.0f) * fadeOut;
        float pulse = 0.94f + 0.06f * (float) Math.sin(age * 0.40f);
        float tremble = (float) Math.sin(age * 3.2f) * (0.015f + power * 0.05f);

        // 1. Gravitational Compression Ground Disc & Singularity Eye, plus
        // the inward-contracting event horizon rings on their phase math.
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.glowFan(pose, buffer, state.seed, baseRadius * 1.05f, 0.04f,
                0.40f, 0.10f, 0.80f, baseAlpha * 0.6f, dist);
            BespokeGeometry.brokenVortexBand(pose, buffer, state.seed ^ 0xD15CL, baseRadius * 0.92f,
                0.02f, 0.09f, -age * 0.14f, 0.20f, 0.05f, 0.45f, baseAlpha * 0.70f, dist);
            // Dense inner singularity eye: dark glow fan, bright rim band.
            BespokeGeometry.glowFan(pose, buffer, state.seed ^ 0xE7EL, 0.75f, 0.05f,
                0.10f, 0.0f, 0.25f, baseAlpha * 0.95f, dist);
            BespokeGeometry.brokenVortexBand(pose, buffer, state.seed ^ 0xB1A1L, 0.42f,
                0.05f, 0.14f, -age * 0.52f, 0.85f, 0.75f, 1.0f, baseAlpha, dist);

            // 2. Inward-Contracting Event Horizon Rings (gravity suction)
            int contractingRings = 4;
            for (int k = 0; k < contractingRings; k++) {
                float ringPhase = (age * 0.35f + k * (baseRadius / contractingRings)) % baseRadius;
                float currentR = baseRadius - ringPhase;
                if (currentR > 0.3f && currentR < baseRadius) {
                    float fade = (currentR / baseRadius) * baseAlpha * 0.85f;
                    float ringY = 0.06f + (1.0f - currentR / baseRadius) * 0.8f;
                    BespokeGeometry.brokenVortexBand(pose, buffer, state.seed + k * 13L, currentR,
                        ringY, ringY + 0.25f, age * 0.21f + k * 0.78f,
                        0.55f, 0.15f, 0.95f, fade, dist);
                }
            }
        });

        // 3. Katchin Weight Monoliths: torn ribbons stood upright, leaned
        // radially outward, with a shard fan rune levitating above each.
        // Placement math is unchanged from the spectacle pass.
        int monolithCount = 6 + (int) (power * 2); // 6 to 8 monoliths
        Random rng = new Random(state.seed);
        for (int m = 0; m < monolithCount; m++) {
            double mAng = (m / (double) monolithCount) * Math.PI * 2.0 + Math.toRadians(age * 3.0f);
            float mDist = 1.6f + (power * 1.8f);

            float mx = (float) Math.cos(mAng) * mDist + (rng.nextFloat() - 0.5f) * tremble;
            float mz = (float) Math.sin(mAng) * mDist + (rng.nextFloat() - 0.5f) * tremble;

            // Per monolith size variety: they used to share one height and
            // one width formula, a ring of identical slabs.
            float mHeight = (1.4f + power * 1.6f) * pulse * (0.75f + rng.nextFloat() * 0.6f);
            float mWidth = (0.35f + power * 0.15f) * (0.8f + rng.nextFloat() * 0.5f);

            // Outward lean for heavy anchor feel (absolute tip offset).
            float lean = 0.20f + power * 0.15f;
            float lx = (float) Math.cos(mAng) * lean;
            float lz = (float) Math.sin(mAng) * lean;

            long rockSeed = state.seed + m * 149L;
            float yawDeg = 180.0f - (float) Math.toDegrees(mAng);
            poseStack.pushPose();
            poseStack.translate(mx, 0.05f, mz);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(yawDeg)));
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.tornRibbon(pose, buffer, rockSeed, mHeight, mWidth * 1.1f,
                    lean, 0.15f, 0.08f, 0.30f, 1.0f, baseAlpha, dist);
                BespokeGeometry.tornRibbon(pose, buffer, rockSeed + 1L, mHeight * 0.94f, mWidth * 0.44f,
                    lean, 0.55f, 0.20f, 0.95f, 0.90f, baseAlpha, dist);
            });
            poseStack.popPose();

            // Levitating Golden Gravity Rune above the anchor.
            float runeY = mHeight + 0.25f + 0.1f * (float) Math.sin(age * 0.5f + m);
            poseStack.pushPose();
            poseStack.translate(mx + lx, runeY, mz + lz);
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                BespokeGeometry.shardFan(pose, buffer, rockSeed + 7L, 0.26f,
                    1.0f, 0.85f, 0.20f, 1.0f, baseAlpha * 0.95f, dist));
            poseStack.popPose();
        }

        // 4. Inward Dimensional Gravity Funnel (the stance well): stacked
        // broken vortex bands widening with height on the old radius and
        // twist profile, with two strongly twisted gold stream bands woven
        // through them where the spiral stream ribbons used to run.
        float funnelH = 3.0f + power * 2.0f;
        float streamH = 2.8f + power * 1.5f;
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            for (int i = 0; i < 4; i++) {
                float frac = (i + 0.5f) / 4.0f;
                float bandRadius = (0.3f + frac * baseRadius) * pulse;
                float y0 = 0.1f + funnelH * (i / 4.0f);
                float y1 = 0.1f + funnelH * ((i + 1) / 4.0f + 0.06f);
                float cr = 0.35f + (0.15f - 0.35f) * frac;
                float cg = 0.05f + (0.02f - 0.05f) * frac;
                float cb = 0.70f + (0.40f - 0.70f) * frac;
                BespokeGeometry.brokenVortexBand(pose, buffer, state.seed ^ (0xEE11L + i * 19L), bandRadius,
                    y0, y1, age * 0.12f + frac * 1.5f, cr, cg, cb,
                    baseAlpha * 0.35f * (1.0f - frac * 0.6f) * 0.75f, dist);
            }
            BespokeGeometry.brokenVortexBand(pose, buffer, state.seed ^ 0x57EAL, (0.2f + 0.5f * baseRadius * 0.8f),
                0.05f, streamH, age * 0.25f + (float) Math.PI * 3.0f,
                1.0f, 0.80f, 0.20f, baseAlpha * 0.55f, dist);
            BespokeGeometry.brokenVortexBand(pose, buffer, state.seed ^ 0x57EBL, (0.2f + 0.35f * baseRadius * 0.8f),
                0.05f, streamH * 0.9f, age * 0.25f + (float) Math.PI * 1.5f,
                1.0f, 0.80f, 0.20f, baseAlpha * 0.45f, dist);
        });

        // Additive halo pass: the singularity eye and the field footprint
        // get a faint bloom anchored to the ground under the caster.
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            BespokeGeometry.glowFan(pose, buffer, state.seed ^ 0xAA10L, 0.95f, 0.05f,
                0.55f, 0.20f, 0.95f, baseAlpha * 0.22f, dist);
            BespokeGeometry.glowFan(pose, buffer, state.seed ^ 0xAA11L, baseRadius * 1.16f, 0.04f,
                0.40f, 0.10f, 0.80f, baseAlpha * 0.12f, dist);
        });

        super.submit(state, poseStack, collector, cameraState);
    }
}
