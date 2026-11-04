package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.SaberDimensionalLineSlashEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
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
 * Entity Renderer for Saber Dimensional Line Slash in Minecraft 26.2.
 *
 * <p>Bespoke pass: the hand-rolled tapered tube beam, equal segment shock
 * rings, diamond ribs and the helix edge strip are deleted. The beam is
 * two crossed pairs of {@link BespokeGeometry} torn ribbons (cyan sheath
 * and white core, one pair flat and one pair upright, so the cut has
 * volume from any angle), the traveling bands and the impact ring are
 * broken vortex bands riding the beam, and the transversal ribs are
 * shard fans. The helix is gone by design: the ribbons' torn, jittered
 * edges replace it. Cut growth, envelope fade, colors and alphas are
 * unchanged from the spectacle pass.</p>
 */
public class SaberDimensionalLineSlashRenderer extends EntityRenderer<SaberDimensionalLineSlashEntity, SaberDimensionalLineSlashRenderer.LineSlashRenderState> {

    public SaberDimensionalLineSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class LineSlashRenderState extends EntityRenderState {
        public Vec3 direction = new Vec3(1, 0, 0);
        public float lineLength = 10.0f;
        public float scale = 1.0f;
        public float age = 0;
        public int maxLifetime = 20;
        public double ex = 0;
        public double ey = 0;
        public double ez = 0;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(SaberDimensionalLineSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(SaberDimensionalLineSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public LineSlashRenderState createRenderState() {
        return new LineSlashRenderState();
    }

    @Override
    public void extractRenderState(SaberDimensionalLineSlashEntity entity, LineSlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.direction = entity.getLineDirection();
        state.lineLength = entity.getLineLength();
        state.scale = entity.getSlashScale();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
        // Synced seed: entity id plus the synced direction and length, so
        // every client tears the beam's edges the same way.
        Vec3 dir = entity.getLineDirection();
        state.seed = entity.getId() * 0x9E3779B97F4A7C15L
            ^ Float.floatToIntBits((float) dir.x) * 31L
            ^ Float.floatToIntBits((float) dir.y) * 131L
            ^ Float.floatToIntBits((float) dir.z) * 1021L
            ^ Float.floatToIntBits(entity.getLineLength());
    }

    @Override
    public void submit(LineSlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
        if (progress >= 1.0f) return;

        float cut = FxEnvelope.smoothstep(Math.min(1.0f, progress / 0.40f));
        // The shared envelope folds into the fade so beam alpha, the entity's
        // particle sheath, and the split damage window all share one curve.
        float env = FxEnvelope.envelope(state.age, (float) state.maxLifetime, 0.15f, 0.35f);
        float fade = (1.0f - FxEnvelope.smoothstep(progress)) * Math.min(1.0f, env * 1.5f);
        float impact = strike(progress);
        float scale = state.scale;
        float ds = LayeredRender.distScale(cameraState, state.ex, state.ey, state.ez);
        long seed = state.seed;
        RenderType renderType = KiRenderHelper.kiRenderType();

        Vec3 dir = state.direction.normalize();
        double horiz = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        float yaw = (float) (Math.atan2(dir.z, dir.x) * (180.0 / Math.PI)) - 90.0f;
        float pitch = (float) -(Math.atan2(dir.y, horiz) * (180.0 / Math.PI));

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-yaw)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(pitch)));

        float halfLen = ((state.lineLength * 0.5f) + 1.0f) * cut;
        float beamLen = halfLen * 2.0f;

        // Beam, flat pair. tornRibbon runs along +X in its own frame; the
        // Y rotation maps +X onto the beam's +Z and the translate centers
        // the ribbon on the snap point, so it spans -halfLen to +halfLen.
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-90f)));
        poseStack.translate(-halfLen, 0, 0);
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.tornRibbon(pose, buffer, seed, beamLen, 0.32f * scale, 0.0f,
                0.0f, 0.75f, 1.0f, 0.72f * fade, 1.0f, ds);
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0xC03EL, beamLen, 0.09f * scale, 0.0f,
                1.0f, 1.0f, 1.0f, fade, 1.0f, ds);
        });
        poseStack.popPose();

        // Beam, upright pair: the extra Z rotation turns the ribbons'
        // width vertical, so the cut reads as a cross, not a flat sheet.
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(90f)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-90f)));
        poseStack.translate(-halfLen, 0, 0);
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0x9E37L, beamLen, 0.32f * scale, 0.0f,
                0.0f, 0.75f, 1.0f, 0.72f * fade, 1.0f, ds);
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0x9E38L, beamLen, 0.09f * scale, 0.0f,
                1.0f, 1.0f, 1.0f, fade, 1.0f, ds);
        });
        poseStack.popPose();

        // Three traveling shock bands riding the beam, as broken vortex
        // bands. The bands circle the Y axis in their own frame; the
        // translate seats each at its z along the beam and the X rotation
        // wraps it around the beam axis.
        int bands = 3;
        for (int b = 0; b < bands; b++) {
            final float bandProg = (state.age * 0.08f + (b / (float) bands)) % 1.0f;
            final float bandZ = -halfLen + bandProg * beamLen;
            final float bandAlpha = fade * 0.5f * (1.0f - bandProg * 0.35f);
            final float bandTwist = state.age * 2.2f + b * 2.094f;
            final long bandSeed = seed + b * 77L;
            if (bandAlpha <= 0.01f) continue;
            poseStack.pushPose();
            poseStack.translate(0, 0, bandZ);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                BespokeGeometry.brokenVortexBand(pose, buffer, bandSeed,
                    0.38f * scale, 0.0f, 0.10f * scale, bandTwist,
                    0.55f, 0.95f, 1.0f, bandAlpha, ds));
            poseStack.popPose();
        }

        // Impact ring at the snap center, same strike window as before.
        if (impact > 0.02f) {
            poseStack.pushPose();
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x19ACL,
                    0.55f * scale * impact, 0.0f, 0.10f * scale, state.age * 3.0f,
                    1.0f, 1.0f, 1.0f, impact, ds));
            poseStack.popPose();
        }

        // Transversal reality-crack ribs across the beam: five shard fans
        // spaced along the cut, brightest at the center where the snap
        // converged, on the same sine profile as the old diamond ribs.
        int ribs = 5;
        for (int ri = 0; ri < ribs; ri++) {
            final float rt = 0.12f + 0.76f * (ri / (float) (ribs - 1));
            final float ribZ = -halfLen + beamLen * rt;
            final float ribA = fade * 0.45f * (float) Math.sin(rt * Math.PI);
            final long ribSeed = seed + 500L + ri * 13L;
            if (ribA <= 0.01f) continue;
            poseStack.pushPose();
            poseStack.translate(0, 0, ribZ);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                BespokeGeometry.shardFan(pose, buffer, ribSeed,
                    0.55f * scale, 0.55f, 0.90f, 1.0f, ribA, 1.0f, ds));
            poseStack.popPose();
        }

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float strike(float progress) {
        if (progress <= 0.32f || progress >= 0.62f) return 0.0f;
        return (float) Math.sin(((progress - 0.32f) / 0.30f) * Math.PI);
    }
}
