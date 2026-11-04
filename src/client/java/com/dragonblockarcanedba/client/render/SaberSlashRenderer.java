package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.SaberSlashEntity;
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
 * Entity Renderer for Saber Slash in Minecraft 26.2.
 *
 * <p>Bespoke pass: the hand-rolled parabola crescent, curved tip spurs and
 * diamond spark are deleted. The crescent is three seeded
 * {@link BespokeGeometry} blade arcs (wispy outer, cyan mid, white core),
 * the tip spurs are jagged branch streaks, the strike flash is a crack
 * star over a shard fan, and the strong variant keeps its second arc on
 * the opposite bulge. Pose rotations, envelope timing, colors and layer
 * alphas are unchanged from the spectacle pass; only the silhouette is
 * now seeded and irregular instead of a perfect parabola.</p>
 */
public class SaberSlashRenderer extends EntityRenderer<SaberSlashEntity, SaberSlashRenderer.SaberSlashRenderState> {

    public SaberSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SaberSlashRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public float tiltAngle = 0.0f;
        public boolean isStrong = false;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public int maxLifetime = 10;
        public double ex = 0;
        public double ey = 0;
        public double ez = 0;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(SaberSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(SaberSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public SaberSlashRenderState createRenderState() {
        return new SaberSlashRenderState();
    }

    @Override
    public void extractRenderState(SaberSlashEntity entity, SaberSlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getSlashScale();
        state.tiltAngle = entity.getTiltAngle();
        state.isStrong = entity.isStrong();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
        // Synced seed: the entity id is assigned by the server and sent in
        // the spawn packet, and the tilt and scale are synced fields, so
        // every client derives the same seed and the arc profile is frame
        // stable for the life of the slash.
        state.seed = entity.getId() * 0x9E3779B97F4A7C15L
            ^ Float.floatToIntBits(entity.getTiltAngle()) * 131L
            ^ Float.floatToIntBits(entity.getSlashScale());
    }

    @Override
    public void submit(SaberSlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
        if (progress >= 1.0f) return;

        float open = FxEnvelope.smoothstep(Math.min(1.0f, progress / 0.34f));
        float fade = 1.0f - FxEnvelope.smoothstep(progress);
        float impact = strike(progress);
        float scale = state.scale * (state.isStrong ? 1.28f : 1.0f);
        float span = 2.55f * scale * (0.70f + 0.30f * open);
        float chord = 0.92f * scale * (0.70f + 0.30f * open);
        boolean strong = state.isStrong;
        // Distance scaling now feeds the builders, which step their own
        // segment counts down with range (standard point 1).
        float ds = LayeredRender.distScale(cameraState, state.ex, state.ey, state.ez);
        long seed = state.seed;
        // Blade arc radius chosen so the arc's width tracks the old span.
        // The arc bulges deeper than the old flat crescent: that deeper
        // claw is the bespoke slash silhouette.
        float radius = span * 1.5f;
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(state.tiltAngle)));

        // Main crescent. bladeArc bulges along +X, so the frame is rotated
        // to face the bulge along +Z, the old crescent's chord direction.
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-90f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // Wispy outer layer at the standard outer band alpha, cyan mid,
            // white core: the same three layer read as the spectacle pass.
            BespokeGeometry.bladeArc(pose, buffer, seed, radius * 1.16f, 130f,
                0.0f, 0.75f, 1.0f, LayeredRender.ALPHA_OUTER, fade, ds);
            BespokeGeometry.bladeArc(pose, buffer, seed ^ 0xA11CEL, radius, 130f,
                0.0f, 0.75f, 1.0f, 0.86f, fade, ds);
            BespokeGeometry.bladeArc(pose, buffer, seed ^ 0xC03EL, radius * 0.78f, 130f,
                1.0f, 1.0f, 1.0f, 1.0f, fade, ds);

            // Tip spurs: jagged branch streaks off the arc tips. At a 130
            // degree sweep the builder's tips sit near (0.21R, 0, +/-0.45R)
            // in this frame, and the spurs trail backward from them.
            float tipX = radius * 0.21f;
            float tipZ = radius * 0.45f;
            float spurW = 0.11f * scale;
            BespokeGeometry.branch(pose, buffer, seed ^ 0x5901L,
                new Vec3(tipX, 0, tipZ),
                new Vec3(tipX - radius * 0.30f, 0, tipZ + radius * 0.12f),
                spurW, 0.0f, 0.85f, 1.0f, fade, 1.0f, ds);
            BespokeGeometry.branch(pose, buffer, seed ^ 0x5902L,
                new Vec3(tipX, 0, tipZ),
                new Vec3(tipX - radius * 0.30f, 0, tipZ + radius * 0.12f),
                spurW * 0.35f, 1.0f, 1.0f, 1.0f, fade * 0.8f, 1.0f, ds);
            BespokeGeometry.branch(pose, buffer, seed ^ 0x5903L,
                new Vec3(tipX, 0, -tipZ),
                new Vec3(tipX - radius * 0.30f, 0, -(tipZ + radius * 0.12f)),
                spurW, 0.0f, 0.85f, 1.0f, fade, 1.0f, ds);
            BespokeGeometry.branch(pose, buffer, seed ^ 0x5904L,
                new Vec3(tipX, 0, -tipZ),
                new Vec3(tipX - radius * 0.30f, 0, -(tipZ + radius * 0.12f)),
                spurW * 0.35f, 1.0f, 1.0f, 1.0f, fade * 0.8f, 1.0f, ds);
        });
        poseStack.popPose();

        // Strike flash, seated mid-claw in the same rotated frame (the
        // translate lands it at z = radius * 0.55 in the slash's own axes).
        if (impact > 0.02f) {
            poseStack.pushPose();
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-90f)));
            poseStack.translate(radius * 0.55f, 0, 0);
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.crackStar(pose, buffer, seed ^ 0x57AAL,
                    chord * 0.66f, 1.0f, 1.0f, 1.0f, impact, 1.0f, ds);
                BespokeGeometry.shardFan(pose, buffer, seed ^ 0xFA11L,
                    chord * 0.5f, 1.0f, 1.0f, 1.0f, impact * 0.9f, 1.0f, ds);
            });
            poseStack.popPose();
        }

        // Strong variant: the second arc bulges the opposite way, where the
        // spectacle pass flipped the crescent's chord sign.
        if (strong) {
            poseStack.pushPose();
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(90f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                BespokeGeometry.bladeArc(pose, buffer, seed ^ 0x5709L, radius * 0.76f, 130f,
                    0.0f, 0.90f, 1.0f, 0.90f, fade, ds));
            poseStack.popPose();
        }

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float strike(float progress) {
        if (progress <= 0.22f || progress >= 0.55f) return 0.0f;
        return (float) Math.sin(((progress - 0.22f) / 0.33f) * Math.PI);
    }
}
