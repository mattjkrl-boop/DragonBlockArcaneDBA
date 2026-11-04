package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.SpiritCleaveEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * The spirit sword's swing rendered bespoke (bespoke pass): the blade is a
 * seeded BespokeGeometry bladeArc standing in the flight plane, the wake is
 * three torn ribbons, the guard is a shard fan, and the opening tip flash is
 * a shard burst. All shapes come from the entity's synced seed, so the cut
 * is frame stable. Envelope, growth, and layer colors are unchanged from
 * the spectacle pass.
 */
public class SpiritCleaveRenderer extends EntityRenderer<SpiritCleaveEntity, SpiritCleaveRenderer.CleaveState> {
    public SpiritCleaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class CleaveState extends EntityRenderState {
        public float yRot;
        public float xRot;
        public float age;
        public float life = SpiritCleaveEntity.FLIGHT_TICKS + SpiritCleaveEntity.FADE_TICKS;
        public long seed;
    }

    public boolean shouldRender(SpiritCleaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(SpiritCleaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public CleaveState createRenderState() {
        return new CleaveState();
    }

    @Override
    public void extractRenderState(SpiritCleaveEntity entity, CleaveState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTick;
        state.life = entity.getLifeTicks();
        state.seed = entity.getSeed();
    }

    @Override
    public void submit(CleaveState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float open = ease(Math.min(1.0f, state.age / 3.0f));
        // One envelope for the whole life (spectacle standard point 5): the
        // same FxEnvelope the server emits particles with, driven by the
        // synced life, so the blade builds, peaks, and dissipates with its
        // trail instead of fading on a private 8 tick clock.
        float fade = FxEnvelope.envelope(state.age, state.life, 0.08f, 0.30f);
        float length = 2.25f * (0.30f + 0.70f * open);
        float tipFlash = ease(Math.max(0.0f, 1.0f - Math.max(0.0f, state.age - 4.0f) / 4.0f));
        float distScale = LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        long seed = state.seed;
        // The bespoke blade: an arc whose radius tracks the old blade
        // length, so the crescent stands about as tall as the old blade
        // was long and grows open on the same 3 tick ease.
        float radius = length * 0.76f;
        RenderType type = KiRenderHelper.kiRenderType();
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            // Blade body: bladeArc stands in its local XZ plane centered
            // on +X; rotate X then Y so the arc stands in the YZ plane
            // centered on the flight direction +Z.
            PoseStack.Pose blade = pose.copy();
            blade.rotate(Axis.YP.rotationDegrees(-90.0f));
            blade.rotate(Axis.XP.rotationDegrees(90.0f));
            BespokeGeometry.bladeArc(blade, buffer, seed, radius, 80.0f,
                1.0f, 0.95f, 0.72f, 0.95f, fade, distScale);
            // Bright core arc nested inside the body.
            BespokeGeometry.bladeArc(blade, buffer, seed ^ 0x51ABL, radius * 0.84f, 64.0f,
                1.0f, 1.0f, 1.0f, 0.95f, fade, distScale);
            // Layered trail with depth (point 1): a wide faint gold wake
            // behind a cyan mid wake behind the bright core wake. Torn
            // ribbons run along +X, so a Y turn aims them back down the
            // flight path; the negative bend keeps the old downward sag.
            PoseStack.Pose wake = pose.copy();
            wake.rotate(Axis.YP.rotationDegrees(90.0f));
            BespokeGeometry.tornRibbon(wake, buffer, seed + 11L, 1.50f, 0.68f, -0.18f,
                1.0f, 0.80f, 0.30f, 0.285f, fade, distScale);
            BespokeGeometry.tornRibbon(wake, buffer, seed + 23L, 1.00f, 0.48f, -0.12f,
                0.0f, 0.90f, 1.0f, 0.475f, fade, distScale);
            BespokeGeometry.tornRibbon(wake, buffer, seed + 37L, 0.55f, 0.32f, -0.06f,
                1.0f, 0.92f, 0.55f, 0.95f, fade, distScale);
            // Guard: a gold shard fan across the base of the blade, in the
            // XY plane facing the flight direction.
            PoseStack.Pose guard = pose.copy();
            guard.rotate(Axis.XP.rotationDegrees(90.0f));
            BespokeGeometry.shardFan(guard, buffer, seed ^ 0x6A12L, 0.45f,
                1.0f, 0.86f, 0.42f, 0.95f, fade, distScale);
            // Opening tip flash: a shard burst at the crescent's front.
            if (tipFlash > 0.02f) {
                PoseStack.Pose tip = pose.copy();
                tip.translate(0.0f, 0.0f, radius);
                tip.rotate(Axis.XP.rotationDegrees(90.0f));
                BespokeGeometry.shardFan(tip, buffer, seed ^ 0x71B5L, 0.30f,
                    1.0f, 0.95f, 0.70f, 0.95f, fade * tipFlash, distScale);
            }
        });
        // Glow halo pass: a fat, faint copy of the blade so the cut reads
        // as light at range. Alpha steps down with camera distance.
        if (fade > 0.02f) {
            float haloAlpha = 0.16f * (0.35f + 0.65f * distScale);
            collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
                PoseStack.Pose halo = pose.copy();
                halo.rotate(Axis.YP.rotationDegrees(-90.0f));
                halo.rotate(Axis.XP.rotationDegrees(90.0f));
                BespokeGeometry.bladeArc(halo, buffer, seed ^ 0x51ABL, radius * 1.06f, 80.0f,
                    0.55f, 0.95f, 1.0f, haloAlpha, fade, distScale);
            });
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float ease(float t) {
        float c = Math.max(0.0f, Math.min(1.0f, t));
        return c * c * (3.0f - 2.0f * c);
    }
}
