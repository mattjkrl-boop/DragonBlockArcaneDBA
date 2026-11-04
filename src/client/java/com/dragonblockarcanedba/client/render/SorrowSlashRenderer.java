package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.SorrowSlashEntity;
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
 * Entity Renderer for Sorrow Slash in Minecraft 26.2.
 * Every layer of the crescent is a BespokeGeometry bladeArc (BESPOKE-STANDARD
 * rule 1): a seeded, nicked scythe moon in place of the old hand built
 * parabolic bow. The layer stack is the spectacle pass layout, kept: a dim
 * wide wake, the abyssal void silhouette, the weeping magenta crest, the
 * razor cyan edge, and a thin hot core, plus the impact burst's crossing
 * crescent. Seeds derive from synced entity state, so the nicks sit in the
 * same place on every client.
 */
public class SorrowSlashRenderer extends EntityRenderer<SorrowSlashEntity, SorrowSlashRenderer.SorrowSlashRenderState> {

    public SorrowSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SorrowSlashRenderState extends EntityRenderState {
        public boolean tiltRight = false;
        public boolean isImpactBurst = false;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public double wx = 0, wy = 0, wz = 0;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(SorrowSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(SorrowSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public SorrowSlashRenderState createRenderState() {
        return new SorrowSlashRenderState();
    }

    @Override
    public void extractRenderState(SorrowSlashEntity entity, SorrowSlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.tiltRight = entity.getTilt();
        state.isImpactBurst = entity.isImpactBurst();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
        // Seed from synced state only, so every client bites the same nicks
        // out of the same crescent.
        state.seed = (long) entity.getId() * 2654435761L
            ^ (entity.getTilt() ? 0x51ED27L : 0x2545F4L)
            ^ (entity.isImpactBurst() ? 0x77AA55L : 0L);
    }

    @Override
    public void submit(SorrowSlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        float maxLife = state.isImpactBurst ? 16.0f : 30.0f;
        float progress = Math.min(1.0f, state.age / maxLife);
        float ease = progress * progress * (3.0f - 2.0f * progress);
        float fade = 1.0f - ease;
        // The flying wave eases in. The impact burst opens hot and eases shut.
        if (!state.isImpactBurst) {
            float intro = Math.min(1.0f, state.age / 4.0f);
            fade *= intro * intro * (3.0f - 2.0f * intro);
        }
        final float slashFade = fade;
        final float distScale = com.dragonblockarcanedba.client.render.ki.LayeredRender.distScale(
            cameraState, state.wx, state.wy, state.wz);

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        // Diagonal / Scythe reap tilt. The flying wave also rolls slowly in
        // its tilt direction while it travels instead of flying as a frozen
        // stamp.
        float tiltAngle = state.tiltRight ? 42.0f : -42.0f;
        if (!state.isImpactBurst) {
            // Tips into the reap and settles, instead of spinning off the sickle.
            float settle = 1.0f - (float) Math.exp(-state.age * 0.22);
            tiltAngle += (state.tiltRight ? 14.0f : -14.0f) * settle;
        }
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(tiltAngle)));

        // Opens with an ease, then the fade above closes it.
        float scale = state.isImpactBurst ? (0.85f + ease * 0.45f) : (0.72f + ease * 0.48f);
        poseStack.scale(scale, scale, scale);

        // Layer radii are the old crescent spans mapped onto bladeArc moons:
        // at a 140 degree sweep the tip to tip width is radius x sin(70), so
        // radius 8.9 spans the same 8.4 blocks the parabolic bow spanned.
        // Soft wake of the same crescent, wider and dimmer, so the reap has a trail.
        submitArc(state, poseStack, collector, renderType, distScale, slashFade,
            10.9f, false, 0.42f, 0.0f, 0.55f, 0.32f);
        // 1. Abyssal Dark Void Outer Silhouette
        submitArc(state, poseStack, collector, renderType, distScale, slashFade,
            9.85f, false, 0.08f, 0.0f, 0.15f, 0.90f);
        // 2. Weeping Sorrow Radiant Magenta / Violet Blade Crest
        submitArc(state, poseStack, collector, renderType, distScale, slashFade,
            8.9f, false, 0.80f, 0.05f, 0.85f, 0.95f);
        // 3. Piercing Razor-Sharp Cyan / White Dimensional Edge.
        // On a hit the edge burns and then eases out with the crescent.
        float edge = state.isImpactBurst ? (0.45f + 0.55f * (1.0f - ease)) : 1.0f;
        submitArc(state, poseStack, collector, renderType, distScale, slashFade,
            6.4f, false, 0.90f, 0.98f, 1.0f, edge);
        // Hot core along the crest. Thin, so the edge stays a line.
        submitArc(state, poseStack, collector, renderType, distScale, slashFade,
            5.16f, false, 1.0f, 0.94f, 1.0f, edge);

        // The hit keeps its second crescent, crossing the first.
        if (state.isImpactBurst) {
            submitArc(state, poseStack, collector, renderType, distScale, slashFade,
                4.9f, true, 0.65f, 0.0f, 0.90f, 0.75f);
        }

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static final float ARC_SWEEP_DEG = 140.0f;

    /**
     * One crescent layer. bladeArc sweeps its moon around +X with the belly
     * pointing +X; rotate Y -90 so the belly faces +Z, the old bow's
     * direction (Y +90 for the impact burst's flipped crossing crescent),
     * then recenter the moon on the entity: its tips sit at 0.5 x radius
     * along the belly axis and its belly at about 1.12 x radius.
     */
    private void submitArc(SorrowSlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                           RenderType renderType, float distScale, float envelope,
                           float radius, boolean flipped, float r, float g, float b, float alpha) {
        float center = (0.5f * (float) Math.cos(Math.toRadians(ARC_SWEEP_DEG / 2.0f)) + 1.12f) * 0.5f * radius;
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(flipped ? 90.0f : -90.0f)));
        poseStack.translate(0, 0, flipped ? center : -center);
        final long seed = state.seed ^ (long) (radius * 977.0f);
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.bladeArc(pose, buffer, seed, radius, ARC_SWEEP_DEG,
                r, g, b, alpha, envelope, distScale));
        poseStack.popPose();
    }
}
