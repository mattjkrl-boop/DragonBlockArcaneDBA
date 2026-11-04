package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.VoidRiftEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;

/**
 * Entity Renderer for Void Rift in Minecraft 26.2.
 * Bespoke pass: the renderer no longer hand-builds its tear from polygon
 * cuts, arc bands and halo fans. The tear body is two nested
 * BespokeGeometry displaced domes (a near-black throat dome recessed
 * behind a violet shell dome, both noise-displaced so no two clients
 * disagree: the seed is the entity UUID), the accretion bands are broken
 * vortex bands with seeded gaps, the back halo is a jittered glow fan,
 * the seam is a torn ribbon, the lip flare is two torn ribbons trailing
 * off the mouth, and the crystal shards are shard fans planted along the
 * lips. The fade-in envelope, the implosion alpha choreography (body
 * first, then rim, then seam, shards collapsing earliest) and the
 * distance segment scaling are unchanged from the spectacle pass.
 */
public class VoidRiftRenderer extends EntityRenderer<VoidRiftEntity, VoidRiftRenderer.RiftRenderState> {
    private static final int SHARDS = 7;

    public VoidRiftRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class RiftRenderState extends EntityRenderState {
        public float radius = 0;
        public boolean isImploding = false;
        public float age = 0;
        public int implodeStartTick = -1;
        public long seed = 0;
        public double ex, ey, ez;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(VoidRiftEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(VoidRiftEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public RiftRenderState createRenderState() {
        return new RiftRenderState();
    }

    @Override
    public void extractRenderState(VoidRiftEntity entity, RiftRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.isImploding = entity.isImploding();
        state.age = entity.tickCount + partialTicks;
        state.implodeStartTick = entity.getImplodeStartTick();
        state.seed = entity.getUUID().getMostSignificantBits();
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
    }

    @Override
    public void submit(RiftRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float radius = state.radius;
        if (radius <= 0.05f) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        // Fade-in envelope and implosion progress, driven by synced state so
        // every client plays the same 30 tick collapse.
        float env = state.isImploding ? 1.0f : ease(Math.min(1.0f, state.age / 20.0f));
        float implodeT = 0.0f;
        if (state.isImploding && state.implodeStartTick >= 0) {
            implodeT = Math.min(1.0f, Math.max(0.0f,
                (state.age - state.implodeStartTick) / (float) VoidRiftEntity.IMPLOSION_TICKS));
        }
        float eased = ease(implodeT);
        float twistEnv = eased * eased;
        float fade = state.isImploding ? (1.0f - implodeT) : env;
        // Distance scaling: layered alpha fades with the square root of the
        // toolkit's distance factor, and the builders step their segment
        // counts down on the same factor.
        float ds = LayeredRender.distScale(cameraState, state.ex, state.ey, state.ez);
        fade *= (float) Math.sqrt(Math.max(0.05f, ds));
        if (fade <= 0.01f && !state.isImploding) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        // Implosion alpha choreography, unchanged windows: the body dies
        // first, the rim outlives it, the seam is the last light standing,
        // and the shards collapse with the body.
        float bodyA = fade;
        float rimA = fade;
        float seamA = 0.0f;
        float shardA = 0.42f * env;
        if (state.isImploding) {
            bodyA = fade * (1.0f - clamp01((implodeT - 0.30f) / 0.48f));
            rimA = fade * (1.0f - clamp01((implodeT - 0.50f) / 0.36f));
            seamA = fade * clamp01((implodeT - 0.74f) / 0.26f);
            shardA = fade * 0.62f * (1.0f - clamp01(implodeT / 0.70f));
        }

        // Tear proportions, same as the cut era: a tall narrow mouth whose
        // belly pulses, narrowing as it opens and collapsing shut.
        float open = state.isImploding ? 1.0f : eased;
        float pulse = state.isImploding ? (1.0f - 0.18f * eased)
            : (0.94f + 0.06f * (float) Math.sin(state.age * 0.4f));
        float yShrink = state.isImploding ? (1.0f - 0.82f * eased) : 1.0f;
        float halfH = radius * 1.12f * yShrink;
        float belly = radius * 0.56f * pulse;
        float bellyX = belly * open;
        float depthBase = radius * 0.28f;
        float bandScale = state.isImploding ? (1.0f - 0.55f * eased) : (0.82f + 0.18f * eased);
        float twistDeg = state.isImploding ? (float) (42.0 * twistEnv)
            : (float) (5.0 * Math.sin(state.age * 0.13f));
        long seed = state.seed;
        float age = state.age;
        // Final snapshots for the geometry lambdas below; fade and the
        // choreography alphas were reassigned above, so they are not
        // effectively final in their own names.
        final float fadeF = fade;
        final float bodyAF = bodyA;
        final float rimAF = rimA;
        final float seamAF = seamA;
        final float shardAF = shardA;
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        // Billboard: the tear faces the camera, standing upright in world
        // space. The quaternion goes through a rotation matrix rather than
        // mulPose(Quaternionf), which 26.3 removed; same rotation, both jars.
        if (cameraState.orientation != null) {
            poseStack.mulPose(new org.joml.Matrix4f().rotation(new org.joml.Quaternionf(cameraState.orientation)));
        }
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(twistDeg)));

        // Back halo: a jittered glow fan in the tear plane, behind the
        // throat, at the old halo's radius and alpha. The builder's fan
        // lies in the XZ plane, so one X rotation stands it up; its y
        // parameter becomes depth after the rotation.
        if (fade > 0.02f) {
            poseStack.pushPose();
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                BespokeGeometry.glowFan(pose, buffer, seed ^ 0x11L, radius * 1.55f, -depthBase * 1.4f,
                    0.45f, 0.08f, 0.85f, 0.30f * fadeF, ds));
            poseStack.popPose();
        }

        // Accretion bands: broken vortex bands in the tear plane (the same
        // X rotation maps their local height onto depth), counter-rotating
        // at the old radii, widths and layered alphas.
        if (fade > 0.02f) {
            poseStack.pushPose();
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x21L, radius * 1.06f * bandScale,
                    -depthBase * 0.5f - radius * 0.05f, -depthBase * 0.5f + radius * 0.05f,
                    age * 0.055f, 0.80f, 0.45f, 1.0f, LayeredRender.ALPHA_MID * fadeF, ds);
                BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x22L, radius * 1.24f * bandScale,
                    -depthBase * 0.7f - radius * 0.06f, -depthBase * 0.7f + radius * 0.06f,
                    -age * 0.038f + 2.1f, 0.55f, 0.10f, 0.92f, LayeredRender.ALPHA_OUTER * 1.5f * fadeF, ds);
                BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x23L, radius * 0.90f * bandScale,
                    -depthBase * 0.3f - radius * 0.04f, -depthBase * 0.3f + radius * 0.04f,
                    age * 0.09f + 4.0f, 0.92f, 0.70f, 1.0f, LayeredRender.ALPHA_CORE * 0.45f * fadeF, ds);
            });
            poseStack.popPose();
        }

        // The tear body: two displaced domes. The violet shell dome is
        // scaled into the mouth's tall ellipse; the near-black throat dome
        // sits inside it and reaches deeper, so the mouth reads as a hole
        // punched through the world rather than a bubble stuck on it. The
        // dome builder faces +Y, so the X rotation aims it away from the
        // camera and the scale is applied in the tear frame first.
        if (bodyA > 0.02f) {
            poseStack.pushPose();
            poseStack.mulPose(new Matrix4f().scale(bellyX * 1.10f / radius, halfH * 1.05f / radius, 0.24f));
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(-90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                BespokeGeometry.displacedDome(pose, buffer, seed ^ 0x31L, radius,
                    0.50f, 0.05f, 0.88f, 0.90f * bodyAF, 1.0f, ds));
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.mulPose(new Matrix4f().scale(bellyX * 0.94f / radius, halfH * 0.97f / radius, 0.34f));
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(-90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                BespokeGeometry.displacedDome(pose, buffer, seed ^ 0x32L, radius,
                    0.02f, 0.0f, 0.05f, 0.98f * bodyAF, 1.0f, ds));
            poseStack.popPose();
        }

        // The seam: the last light standing at peak collapse, a torn
        // ribbon hung vertically through the mouth's center column.
        if (seamA > 0.03f) {
            poseStack.pushPose();
            poseStack.translate(0.0f, -halfH * 0.94f, depthBase * 0.10f);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(90.0f)));
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(-90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0x41L, halfH * 1.88f,
                    Math.max(0.02f, radius * 0.045f), 0.0f,
                    1.0f, 1.0f, 1.0f, seamAF, 1.0f, ds));
            poseStack.popPose();
        }

        // Lip flare: two torn ribbons trailing off the mouth's edges, the
        // wake sheets the old flare triangles used to fake. The left one
        // is mirrored so its length runs outward to -X.
        if (rimA > 0.02f) {
            poseStack.pushPose();
            poseStack.translate(-bellyX, 0.0f, -depthBase * 0.15f);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(-90.0f)));
            poseStack.mulPose(new Matrix4f().scale(-1.0f, 1.0f, 1.0f));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0x51L, radius * 0.55f, halfH * 1.5f,
                    radius * 0.10f, 0.60f, 0.08f, 0.96f, 0.22f * rimAF, 1.0f, ds));
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.translate(bellyX, 0.0f, -depthBase * 0.15f);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(-90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0x52L, radius * 0.55f, halfH * 1.5f,
                    radius * 0.10f, 0.60f, 0.08f, 0.96f, 0.22f * rimAF, 1.0f, ds));
            poseStack.popPose();
        }

        // Crystal shards: shard fans planted along both lips, pointing
        // away from the mouth's center, sucked toward the throat as the
        // collapse takes them.
        if (shardA > 0.04f && open > 0.04f) {
            float shrink = state.isImploding ? (1.0f - 0.9f * eased) : 1.0f;
            for (int i = 0; i < SHARDS; i++) {
                float t = 0.18f + 0.64f * (i / (float) (SHARDS - 1));
                boolean left = (i & 1) == 0;
                float side = left ? -1.0f : 1.0f;
                float slim = 0.72f + 0.28f * (float) Math.sin(t * Math.PI);
                float shaped = (float) Math.pow(Math.sin(t * Math.PI), 0.55 + 1.35 * (1.0 - open));
                float y = (t - 0.5f) * 2.0f * halfH;
                float hug = radius * 0.29f * slim * (state.isImploding ? (1.0f - eased) : 1.0f);
                float x = side * (bellyX * shaped * 1.16f + hug);
                if (state.isImploding) x *= (1.0f - 0.8f * eased);
                float z = -depthBase * 0.3f * (state.isImploding ? (1.0f - eased) : 1.0f)
                    + (float) Math.sin(age * 0.7f + i * 2.3f) * radius * 0.02f;
                float shardR = radius * 0.34f * slim * shrink;
                if (shardR < 0.05f) continue;
                long shardSeed = seed ^ (0x60L + i);
                float angDeg = (float) Math.toDegrees(Math.atan2(y, x));
                poseStack.pushPose();
                poseStack.translate(x, y, z);
                poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(angDeg)));
                poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
                collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                    BespokeGeometry.shardFan(pose, buffer, shardSeed, shardR,
                        0.58f, 0.07f, 0.94f, shardAF, 1.0f, ds));
                poseStack.popPose();
            }
        }

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float clamp01(float v) {
        return v < 0.0f ? 0.0f : Math.min(1.0f, v);
    }

    private static float ease(float t) {
        float c = clamp01(t);
        return c * c * (3.0f - 2.0f * c);
    }
}
