package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.LavaEruptionEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;

/** A lava wave that bows outward along the swing and burns down. */
public class LavaEruptionRenderer extends EntityRenderer<LavaEruptionEntity, LavaEruptionRenderer.EruptState> {
    public LavaEruptionRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class EruptState extends EntityRenderState {
        public float age;
        public float yRot;
        public float reach;
        public int seed;
        public double posX;
        public double posY;
        public double posZ;
    }

    public boolean shouldRender(LavaEruptionEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(LavaEruptionEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public EruptState createRenderState() {
        return new EruptState();
    }

    @Override
    public void extractRenderState(LavaEruptionEntity entity, EruptState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.age = entity.tickCount + partialTick;
        state.yRot = entity.getYRot();
        state.reach = entity.reachAt(state.age);
        state.seed = entity.getSeed();
        state.posX = entity.getX();
        state.posY = entity.getY();
        state.posZ = entity.getZ();
    }

    @Override
    public void submit(EruptState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        // The shared envelope drives the alpha, so the wave builds over its
        // first ticks and burns down over its last instead of popping in at
        // full alpha. Distance steps the segment count down (standard point 1).
        float scale = LayeredRender.distScale(cameraState, state.posX, state.posY, state.posZ);
        float env = FxEnvelope.envelope(state.age, 12.0f);
        float remain = ease(Math.max(0.0f, 1.0f - state.age / 12.0f));
        float height = 0.7f + 1.1f * ease(Math.min(1.0f, state.age / 4.0f));
        long seed = state.seed;
        RenderType type = KiRenderHelper.kiRenderType();
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        // Bespoke sheets (BESPOKE-STANDARD rule 1): the old uniform grid
        // sheet is two seeded plumes now, uneven stacks of flame puffs
        // pushed forward along the facing so the crown sits inside the
        // cone the damage and the ground burn use. The outer plume is the
        // dark mid sheet; the inner one is the brighter, taller core.
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.0f, state.reach * 0.30f);
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            BespokeGeometry.plume(pose, buffer, seed, state.reach * 0.60f, height,
                0.45f, 0.07f, 0.02f, 0.8f * remain, env, scale);
            BespokeGeometry.plume(pose, buffer, seed ^ 0xC02EL, state.reach * 0.33f, height * 1.3f,
                1.0f, 0.55f, 0.10f, 0.9f * remain, env, scale);
        });
        poseStack.popPose();
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            // Scorch mark at the base: a seeded star of wandering cracks,
            // kept inside the reach the wave and the burn actually cover.
            BespokeGeometry.crackStar(pose, buffer, seed ^ 0x5CA2L, Math.min(state.reach * 0.85f, 5.5f),
                1.0f, 0.50f, 0.08f, LayeredRender.ALPHA_MID * remain, env, scale);
            // Anchor glow at the origin only: a base splash around the caster
            // the wave erupts from, kept well inside the wave's own reach so
            // the glow never claims ground the wave does not cover.
            LayeredRender.drawAnchorGlow(pose, buffer, Math.min(state.reach * 0.45f, 2.2f), 0.03f,
                1.0f, 0.42f, 0.05f, LayeredRender.ALPHA_MID * env, scale);
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float ease(float t) {
        float c = Mth.clamp(t, 0.0f, 1.0f);
        return c * c * (3.0f - 2.0f * c);
    }
}
