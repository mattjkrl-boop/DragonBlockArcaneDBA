package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.TridentProngEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * One shot: three barbed crimson needles in trident formation.
 * Bespoke pass: each needle is the same crossed crescent blade the swarm
 * shards use (TridentShardRenderer.submitBlade, BespokeGeometry.bladeArc),
 * smaller, with a shardFan barb burst at its base and a tornRibbon wake
 * that dies on impact. The seed comes from the entity UUID, so the
 * formation is identical on every client. No local geometry helpers remain.
 */
public class TridentProngRenderer extends EntityRenderer<TridentProngEntity, TridentProngRenderer.ProngState> {
    public TridentProngRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ProngState extends EntityRenderState {
        public float yRot;
        public float xRot;
        public float age;
        public int dieTick;
        /** Synced shape seed from the entity UUID. */
        public long seed = 0;
    }

    public boolean shouldRender(TridentProngEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(TridentProngEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ProngState createRenderState() {
        return new ProngState();
    }

    @Override
    public void extractRenderState(TridentProngEntity entity, ProngState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTick;
        state.dieTick = entity.getDieTick();
        state.seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();
    }

    @Override
    public void submit(ProngState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType type = KiRenderHelper.kiRenderType();
        float intro = Math.min(1.0f, state.age / 2.0f);
        float fade = intro * intro * (3.0f - 2.0f * intro);
        if (state.dieTick > 0) {
            float t = Math.min(1.0f, Math.max(0.0f, (state.age - state.dieTick) / 6.0f));
            float ease = t * t * (3.0f - 2.0f * t);
            fade *= 1.0f - ease;
        }
        final float needleFade = fade;
        final float segScale = com.dragonblockarcanedba.client.render.ki.LayeredRender.distScale(
            cameraState, state.x, state.y, state.z);
        float[] offsets = { -0.26f, 0.0f, 0.26f };
        float[] radii = { 0.95f, 1.25f, 0.95f };
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));
        for (int i = 0; i < 3; i++) {
            final long needleSeed = state.seed + i * 13L;
            poseStack.pushPose();
            poseStack.translate(offsets[i], 0.0, 0.0);

            // The needle: crossed crescent blade, slimmer in the horizontal
            // plane so the three prongs stay distinct in formation.
            TridentShardRenderer.submitBlade(poseStack, collector, type, needleSeed, radii[i],
                0.36f, 0.02f, 0.04f, 0.96f * needleFade,
                0.96f, 0.24f, 0.08f, 0.95f * needleFade,
                0.6f, segScale);

            // Barbs at the needle base.
            poseStack.pushPose();
            poseStack.translate(0.0, 0.0, -0.08);
            collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
                BespokeGeometry.shardFan(pose, buffer, needleSeed + 5, 0.16f,
                    1.0f, 0.26f, 0.1f, 0.85f * needleFade, 1.0f, segScale);
            });
            poseStack.popPose();

            // Wake: two torn ribbons trailing behind the needle.
            poseStack.pushPose();
            poseStack.translate(0.0, 0.0, -0.1);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(90.0f)));
            collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
                BespokeGeometry.tornRibbon(pose, buffer, needleSeed + 21, 0.95f, 0.12f, 0.04f,
                    0.7f, 0.03f, 0.06f, 0.5f, needleFade, segScale);
                BespokeGeometry.tornRibbon(pose, buffer, needleSeed + 27, 0.95f, 0.05f, 0.02f,
                    1.0f, 0.18f, 0.06f, 0.75f, needleFade, segScale);
            });
            poseStack.popPose();

            poseStack.popPose();
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
