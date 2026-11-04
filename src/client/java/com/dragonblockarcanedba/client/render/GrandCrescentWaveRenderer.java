package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.GrandCrescentWaveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Grand Crescent Wave in Minecraft 26.2.
 * Bespoke pass: the crescent is built only from BespokeGeometry builders,
 * seeded from the entity UUID so every client draws the same silhouette.
 * - Three nested bladeArc layers: golden canopy, white-gold mid, white core
 * - Torn ribbon wing wisps above and below the arc
 * - A trailing bladeArc wake behind the crescent
 * - Glowing valor glyph fans inscribed along the arc, pulsing in sequence
 */
public class GrandCrescentWaveRenderer extends EntityRenderer<GrandCrescentWaveEntity, GrandCrescentWaveRenderer.GrandCrescentWaveRenderState> {

    public GrandCrescentWaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class GrandCrescentWaveRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public float ageInTicks = 0;
        public int hitTick = -1;
        public long seed = 0;
        public boolean isFirstPersonOwner = false;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(GrandCrescentWaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(GrandCrescentWaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public GrandCrescentWaveRenderState createRenderState() {
        return new GrandCrescentWaveRenderState();
    }

    @Override
    public void extractRenderState(GrandCrescentWaveEntity entity, GrandCrescentWaveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.ageInTicks = entity.tickCount + partialTicks;
        state.hitTick = entity.getHitTick();
        state.seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null && 
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) && 
            mc.options.getCameraType().isFirstPerson());
    }

    @Override
    public void submit(GrandCrescentWaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        // Lifecycle runs on the shared envelope: the crescent opens over its
        // first ticks, peaks, and dissipates across the last 35 percent of
        // its 80 tick life. A block strike starts a separate 12 tick fade
        // from the hit tick, so the wave never vanishes at full brightness.
        float maxLife = GrandCrescentWaveEntity.MAX_LIFE_TICKS;
        float progress = Math.min(1.0f, state.ageInTicks / maxLife);
        float fade = com.dragonblockarcanedba.util.FxEnvelope.envelope(
            state.ageInTicks, maxLife, 0.04f, 0.35f);
        if (state.hitTick >= 0) {
            float hitFade = 1.0f - smooth(Math.min(1.0f,
                (state.ageInTicks - state.hitTick) / (float) GrandCrescentWaveEntity.DISSIPATE_TICKS));
            fade = Math.min(fade, hitFade);
        }
        float open = smooth(Math.min(1.0f, state.ageInTicks / 12.0f));
        float impact = strike(progress);

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        float fpScale = (state.isFirstPersonOwner && state.ageInTicks < 6.0f)
            ? (0.55f + (state.ageInTicks / 6.0f) * 0.45f)
            : 1.0f;
        float span = 4.8f * fpScale * (0.80f + 0.20f * open);
        float chord = 1.55f * fpScale * (0.80f + 0.20f * open);
        // Distance scaling (standard point 1): distant waves step their
        // segment count down instead of costing full geometry.
        float distScale = com.dragonblockarcanedba.client.render.ki.LayeredRender.distScale(
            cameraState, state.x, state.y, state.z);
        float age = state.ageInTicks;
        // fade is reassigned by the hit fade above, so the geometry lambda
        // captures this final snapshot of it.
        final float fadeFinal = fade;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // Three nested bladeArc layers form the crescent belly and core.
            drawArcLayer(pose, buffer, state.seed, span, 150.0f,
                1.0f, 0.82f, 0.15f, 0.45f, fadeFinal, distScale, 0.0f);
            drawArcLayer(pose, buffer, state.seed ^ 0x1A2B3C4DL, span * 0.80f, 142.0f,
                1.0f, 0.98f, 0.80f, 0.65f, fadeFinal, distScale, 0.0f);
            drawArcLayer(pose, buffer, state.seed ^ 0x3C4D5E6FL, span * 0.60f, 132.0f,
                1.0f, 1.0f, 1.0f, 0.85f, fadeFinal, distScale, 0.0f);

            // Trailing wake: the same arc stepped back behind the belly.
            drawArcLayer(pose, buffer, state.seed ^ 0x7E57A11L, span * 0.88f, 150.0f,
                1.0f, 0.75f, 0.10f, 0.30f, fadeFinal, distScale, 0.55f);

            // Torn ribbon wing wisps reaching toward the arc tips.
            drawWing(pose, buffer, state.seed ^ 0x11111L, span, chord, 0.18f, fadeFinal, distScale, false);
            drawWing(pose, buffer, state.seed ^ 0x22222L, span, chord, -0.18f, fadeFinal, distScale, true);

            // Inscribed valor glyphs: glow fans placed on the crescent band,
            // alpha pulsing in sequence along the arc (same flow formula as
            // the spectacle pass used for its diamond glyphs).
            final int glyphs = 7;
            for (int g = 0; g < glyphs; g++) {
                double theta = -1.05 + 2.10 * (g / (double) (glyphs - 1));
                double rm = 0.75 * span;
                float gx = (float) (-Math.sin(theta) * rm);
                float gz = (float) (Math.cos(theta) * rm - rm);
                float glyphAlpha = fadeFinal * (0.70f + 0.30f * (float) Math.sin(age * 0.6f - g * 0.9f));
                PoseStack glyphStack = new PoseStack();
                glyphStack.last().pose().set(pose.pose());
                glyphStack.last().normal().set(pose.normal());
                glyphStack.translate(gx, 0.10f, gz);
                BespokeGeometry.glowFan(glyphStack.last(), buffer, state.seed + 31L * g, 0.17f, 0.0f,
                    1.0f, 1.0f, 0.90f, glyphAlpha, distScale);
            }

            // Strike flash: a bright shard fan at the belly on the opening
            // frames, replacing the old hand drawn strike diamond.
            if (impact > 0.02f) {
                PoseStack strikeStack = new PoseStack();
                strikeStack.last().pose().set(pose.pose());
                strikeStack.last().normal().set(pose.normal());
                strikeStack.translate(0.0f, 0.0f, chord * 0.5f);
                BespokeGeometry.shardFan(strikeStack.last(), buffer, state.seed ^ 0x57A1EL, 0.55f,
                    1.0f, 0.98f, 0.80f, impact, 1.0f, distScale);
            }
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float smooth(float t) {
        float x = Math.max(0.0f, Math.min(1.0f, t));
        return x * x * (3.0f - 2.0f * x);
    }

    private static float strike(float progress) {
        if (progress >= 0.16f) return 0.0f;
        return (float) Math.sin((progress / 0.16f) * Math.PI);
    }

    /**
     * Pose glue only: turns one bladeArc so its flat XZ band stands as the
     * wave's crescent (belly through the origin, tips trailing toward -Z),
     * then calls the builder. Emits no vertices of its own.
     */
    private static void drawArcLayer(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buffer,
                                     long seed, float radius, float sweepDeg,
                                     float r, float g, float b, float alpha, float envelope,
                                     float distScale, float backOffset) {
        PoseStack arcStack = new PoseStack();
        arcStack.last().pose().set(pose.pose());
        arcStack.last().normal().set(pose.normal());
        arcStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-90.0f)));
        arcStack.translate(-(radius * 0.75f + backOffset), 0.0f, 0.0f);
        BespokeGeometry.bladeArc(arcStack.last(), buffer, seed, radius, sweepDeg,
            r, g, b, alpha, envelope, distScale);
    }

    /**
     * Pose glue only: places one tornRibbon as a wing wisp reaching from the
     * crescent belly toward a tip, then calls the builder.
     */
    private static void drawWing(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buffer,
                                 long seed, float span, float chord, float yBase,
                                 float envelope, float distScale, boolean flip) {
        PoseStack wingStack = new PoseStack();
        wingStack.last().pose().set(pose.pose());
        wingStack.last().normal().set(pose.normal());
        if (flip) {
            wingStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(180.0f)));
        }
        wingStack.translate(span * 0.10f, yBase, chord * 0.20f);
        BespokeGeometry.tornRibbon(wingStack.last(), buffer, seed, span * 0.95f, 0.20f, 0.12f,
            1.0f, 0.90f, 0.30f, 0.45f, envelope, distScale);
    }
}
