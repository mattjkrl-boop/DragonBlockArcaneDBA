package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.BraveSlashEntity;
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
 * Entity Renderer for Brave Slash in MC 26.2.
 * Bespoke pass: the cut is drawn with BespokeGeometry blade arcs, seeded
 * crescents with nicked, noise-bitten edges, in three nested layers (gold
 * body, cyan core, white edge), replacing the hand-built parabola crescent
 * and its local draw helpers. A shard fan flashes inside the crescent
 * during the strike window where the old diamond flash sat. The seed comes
 * from the entity UUID, which is synced state, so every client draws the
 * same crescent.
 */
public class BraveSlashRenderer extends EntityRenderer<BraveSlashEntity, BraveSlashRenderer.BraveSlashRenderState> {
    public BraveSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class BraveSlashRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public int maxLifetime = BraveSlashEntity.MAX_LIFE;
        public long seed = 0;
    }

    @Override
    public BraveSlashRenderState createRenderState() {
        return new BraveSlashRenderState();
    }

    @Override
    public void extractRenderState(BraveSlashEntity entity, BraveSlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();
    }

    @Override
    public void submit(BraveSlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        // Shared lifecycle: the cut builds over the first fifth of its real
        // life, peaks, then dissipates over the last third, the same envelope
        // the entity emits its wake particles with.
        float life = (float) state.maxLifetime;
        float progress = Math.min(1.0f, state.age / life);
        float fade = FxEnvelope.envelope(state.age, life, 0.2f, 0.35f);
        float open = FxEnvelope.smoothstep(Math.min(1.0f, state.age / (life * 0.2f)));
        float span = 3.2f * (0.82f + 0.22f * open);
        float impact = strike(progress);
        float distScale = LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        long seed = state.seed;
        float radius = span * 1.35f;

        // The blade arc is centered on +X in its own frame; a quarter turn
        // about Y centers it on +Z, the crescent belly facing the flight
        // direction with its tips swept back, as the old cut read. The arc
        // band is centered on its own origin, so the whole crescent is
        // pulled back to straddle the flight line like the old cut did
        // instead of floating ahead of the hitbox.
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.0f, -span * 0.55f);
        poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(-90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.bladeArc(pose, buffer, seed, radius, 140.0f,
                1.0f, 0.84f, 0.0f, 0.90f, fade, distScale);
            BespokeGeometry.bladeArc(pose, buffer, seed ^ 0x2B992DDFL, radius * 0.8f, 140.0f,
                0.0f, 0.95f, 1.0f, 0.95f, fade, distScale);
            BespokeGeometry.bladeArc(pose, buffer, seed ^ 0x51ED2701L, radius * 0.62f, 140.0f,
                1.0f, 1.0f, 1.0f, 1.0f, fade, distScale);
        });
        poseStack.popPose();

        // Strike flash: a two tone shard fan standing in the crescent plane
        // at the belly, where the old diamond flash pulsed.
        if (impact > 0.02f) {
            poseStack.pushPose();
            poseStack.translate(0.0f, 0.0f, span * 0.55f);
            poseStack.mulPose(new Matrix4f().rotation(Axis.XP.rotationDegrees(-90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.shardFan(pose, buffer, seed ^ 0x85EBCA6BL, 0.38f * impact,
                    1.0f, 1.0f, 1.0f, 1.0f, impact, distScale);
                BespokeGeometry.shardFan(pose, buffer, seed ^ 0x27D4EB2FL, 0.24f * impact,
                    1.0f, 0.96f, 0.75f, 1.0f, impact, distScale);
            });
            poseStack.popPose();
        }

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float strike(float progress) {
        if (progress >= 0.36f) return 0.0f;
        return (float) Math.sin((progress / 0.36f) * Math.PI);
    }
}
