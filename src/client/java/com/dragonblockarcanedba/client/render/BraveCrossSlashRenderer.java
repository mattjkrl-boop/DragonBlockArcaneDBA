package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.BraveCrossSlashEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
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
 * Entity Renderer for Brave Finisher Cross Slash in Minecraft 26.2.
 * Bespoke pass: the two rolled parabola crescents are rebuilt as
 * BespokeGeometry blade arcs stood upright and rolled to plus and minus
 * 45 degrees, layered gold, cyan, and white per blade, with shard fans
 * sparking at the four arm ends and a two tone shard fan starburst in the
 * strike window where the old geometric starburst sat. The seed comes
 * from the entity UUID, which is synced state, so every client draws the
 * same cross.
 */
public class BraveCrossSlashRenderer extends EntityRenderer<BraveCrossSlashEntity, BraveCrossSlashRenderer.CrossSlashRenderState> {

    public BraveCrossSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class CrossSlashRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public int maxLifetime = 40;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(BraveCrossSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(BraveCrossSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public CrossSlashRenderState createRenderState() {
        return new CrossSlashRenderState();
    }

    @Override
    public void extractRenderState(BraveCrossSlashEntity entity, CrossSlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getSlashScale();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();
    }

    @Override
    public void submit(CrossSlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float life = (float) state.maxLifetime;
        float progress = Math.min(1.0f, state.age / life);
        if (progress >= 1.0f) return;

        // Shared lifecycle over the entity's real 40 tick life: blades draw
        // out across the build fifth, hold through the damage pulses, then
        // dissipate across the last 30 percent.
        float drawOut = FxEnvelope.smoothstep(Math.min(1.0f, progress / 0.2f));
        float fade = FxEnvelope.envelope(state.age, life, 0.2f, 0.3f);
        float impact = strike(progress);
        float scale = state.scale;
        float distScale = LayeredRender.distScale(cameraState, state.x, state.y, state.z);

        // span is the half-width of the old finisher blades; the blade arc
        // radius is sized off it so the cross keeps its footprint.
        float span = 4.6f * scale * drawOut;
        float radius = span * 1.35f;

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        // Main structure pass.
        drawCross(poseStack, collector, KiRenderHelper.kiRenderType(),
            state.seed, radius, span, scale, fade, impact, distScale, 1.0f, true);

        // Outer layer (standard point 1): a wider, faint copy of both blades
        // so the cross has a wispy rim around its dense core, the mod's
        // layered translucency technique for glow.
        drawCross(poseStack, collector, KiRenderHelper.kiAdditiveType(),
            state.seed, radius * 1.16f, span, scale, fade, 0.0f, distScale,
            LayeredRender.ALPHA_OUTER, false);

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    /**
     * Composes one cross out of BespokeGeometry builders: two blade arcs
     * rolled to an X, tip fans at the four arm ends, and, on the main pass
     * only, the strike starburst. alphaMul scales every layer alpha for
     * the additive pass.
     */
    private static void drawCross(PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType,
                                  long seed, float radius, float span, float scale,
                                  float fade, float impact, float distScale, float alphaMul,
                                  boolean withStarburst) {
        // Each blade: stand the flat arc upright (rotate about X), roll it
        // in its own plane (rotate about Z), and pull it back along its
        // belly direction so the band straddles the origin like the old
        // centered crescents did.
        float[] rolls = {45.0f, -45.0f};
        for (int b = 0; b < rolls.length; b++) {
            long bladeSeed = seed + b * 0x9E3779B9L;
            poseStack.pushPose();
            poseStack.mulPose(new Matrix4f().rotation(Axis.ZP.rotationDegrees(rolls[b])));
            poseStack.mulPose(new Matrix4f().rotation(Axis.XP.rotationDegrees(-90.0f)));
            poseStack.translate(-span * 0.55f, 0.0f, 0.0f);
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.bladeArc(pose, buffer, bladeSeed, radius, 140.0f,
                    1.0f, 0.84f, 0.0f, 0.90f * alphaMul, fade, distScale);
                BespokeGeometry.bladeArc(pose, buffer, bladeSeed ^ 0x2B992DDFL, radius * 0.84f, 140.0f,
                    0.0f, 0.95f, 1.0f, 0.95f * alphaMul, fade, distScale);
                BespokeGeometry.bladeArc(pose, buffer, bladeSeed ^ 0x51ED2701L, radius * 0.72f, 140.0f,
                    1.0f, 1.0f, 1.0f, 1.0f * alphaMul, fade, distScale);
            });
            poseStack.popPose();
        }

        // Tip fans at the four arm ends of the X, where the old rolled
        // spurs quilled out from the crescent tips.
        float arm = span * 0.66f;
        float[][] tips = {{arm, arm}, {-arm, arm}, {arm, -arm}, {-arm, -arm}};
        for (int t = 0; t < tips.length; t++) {
            long tipSeed = seed ^ (0x85EBCA6BL + t * 131L);
            float tx = tips[t][0];
            float ty = tips[t][1];
            poseStack.pushPose();
            poseStack.translate(tx, ty, 0.0f);
            poseStack.mulPose(new Matrix4f().rotation(Axis.XP.rotationDegrees(-90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.shardFan(pose, buffer, tipSeed, 0.34f * scale,
                    0.85f, 0.98f, 1.0f, 0.85f * alphaMul, fade, distScale);
            });
            poseStack.popPose();
        }

        // Strike starburst at the center during the impact window.
        if (withStarburst && impact > 0.02f) {
            poseStack.pushPose();
            poseStack.translate(0.0f, 0.0f, 0.1f);
            poseStack.mulPose(new Matrix4f().rotation(Axis.XP.rotationDegrees(-90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.shardFan(pose, buffer, seed ^ 0x27D4EB2FL, 1.15f * scale * impact,
                    1.0f, 0.87f, 0.25f, 1.0f, impact, distScale);
                BespokeGeometry.shardFan(pose, buffer, seed ^ 0x165667B1L, 1.15f * scale * impact * 0.62f,
                    0.55f, 0.97f, 1.0f, 0.85f, impact, distScale);
            });
            poseStack.popPose();
        }
    }

    private static float strike(float progress) {
        if (progress <= 0.28f || progress >= 0.62f) return 0.0f;
        return (float) Math.sin(((progress - 0.28f) / 0.34f) * Math.PI);
    }
}
