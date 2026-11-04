package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.GrandBladeShardEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Grand Blade Shard in Minecraft 26.2.
 * Bespoke pass: every shape comes from BespokeGeometry builders, seeded
 * from the entity UUID so all clients draw the same silhouette. The blade
 * body is two crossed tornRibbons (flat gold, vertical steel-gold) with a
 * narrow white-gold core ribbon as the cutting fuller.
 * - Flying State: the crossed-ribbon blade spinning on its flight axis,
 *   with two torn ribbon wakes trailing behind it.
 * - Embedded State: crackStar fracture mandala, glowFan hazard ring, a
 *   plume beacon column, and the same blade tilted into the ground.
 */
public class GrandBladeShardRenderer extends EntityRenderer<GrandBladeShardEntity, GrandBladeShardRenderer.BladeShardRenderState> {

    public GrandBladeShardRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class BladeShardRenderState extends EntityRenderState {
        public boolean isEmbedded = false;
        public float yRot = 0;
        public float xRot = 0;
        public float ageInTicks = 0;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(GrandBladeShardEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(GrandBladeShardEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public BladeShardRenderState createRenderState() {
        return new BladeShardRenderState();
    }

    @Override
    public void extractRenderState(GrandBladeShardEntity entity, BladeShardRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.isEmbedded = entity.isEmbedded();
        state.yRot = entity.isEmbedded() ? entity.getEmbeddedYaw() : entity.getYRot();
        state.xRot = entity.isEmbedded() ? entity.getEmbeddedPitch() : entity.getXRot();
        state.ageInTicks = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();
    }

    @Override
    public void submit(BladeShardRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        float distScale = com.dragonblockarcanedba.client.render.ki.LayeredRender.distScale(
            cameraState, state.x, state.y, state.z);

        poseStack.pushPose();

        if (state.isEmbedded) {
            // --- EMBEDDED GROUND STATE ---
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));

            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                // Fade out over the last 30 embedded ticks (shared envelope
                // shape): the hazard used to vanish at full brightness.
                float fadeOut = 1.0f;
                float embeddedAge = state.ageInTicks;
                float fadeStart = GrandBladeShardEntity.MAX_EMBEDDED_TICKS - 30.0f;
                if (embeddedAge > fadeStart) {
                    fadeOut = 1.0f - com.dragonblockarcanedba.util.FxEnvelope.smoothstep(
                        (embeddedAge - fadeStart) / 30.0f);
                }
                float pulse = (0.85f + 0.15f * (float) Math.sin(state.ageInTicks * 0.25f)) * fadeOut;

                // 1. Golden ground fracture mandala: two nested crack stars.
                BespokeGeometry.crackStar(pose, buffer, state.seed, 1.10f,
                    1.0f, 0.85f, 0.20f, 0.90f, pulse, distScale);
                BespokeGeometry.crackStar(pose, buffer, state.seed ^ 0xC1A0L, 0.75f,
                    1.0f, 1.0f, 0.90f, 1.0f, pulse, distScale);

                // Ground warning ring: a seeded glow fan at hazard radius.
                BespokeGeometry.glowFan(pose, buffer, state.seed ^ 0x6100L, 0.85f, 0.04f,
                    1.0f, 0.80f, 0.10f, 0.65f * pulse, distScale);

                // 2. Vertical radiant hazard beacon: a plume column whose
                // height breathes with the same pulse as the spectacle pass.
                BespokeGeometry.plume(pose, buffer, state.seed ^ 0xBEA0L, 0.30f, 1.6f * pulse,
                    1.0f, 0.90f, 0.30f, 0.70f, pulse, distScale);

                // 3. Embedded blade, tilted into the ground as before.
                PoseStack bladeStack = new PoseStack();
                bladeStack.last().pose().set(pose.pose());
                bladeStack.last().normal().set(pose.normal());
                bladeStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(Math.max(35.0f, Math.abs(state.xRot)))));
                bladeStack.scale(0.85f, 0.85f, 0.85f);

                drawBespokeBlade(bladeStack.last(), buffer, state.seed, fadeOut, distScale);
            });

        } else {
            // --- FLYING AIRBORNE STATE ---
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(state.ageInTicks * 35.0f))); // High-speed spin along flight axis
            poseStack.scale(0.75f, 0.75f, 0.75f);

            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                // Lifecycle: builds in over 3 ticks, fades over the last 10
                // ticks of the 60 tick flight instead of popping at both ends.
                float buildIn = com.dragonblockarcanedba.util.FxEnvelope.smoothstep(state.ageInTicks / 3.0f);
                float fadeOut = 1.0f;
                float flightFadeStart = GrandBladeShardEntity.MAX_FLIGHT_TICKS - 10.0f;
                if (state.ageInTicks > flightFadeStart) {
                    fadeOut = 1.0f - com.dragonblockarcanedba.util.FxEnvelope.smoothstep(
                        (state.ageInTicks - flightFadeStart) / 10.0f);
                }
                float vis = buildIn * fadeOut;
                drawBespokeBlade(pose, buffer, state.seed, vis, distScale);

                // Torn wake: two crossed ribbons trailing behind the blade
                // (replaces the old 12 step helical line wake).
                drawWake(pose, buffer, state.seed ^ 0xAA11L, vis, distScale, false);
                drawWake(pose, buffer, state.seed ^ 0xAA22L, vis, distScale, true);
            });
        }

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    /**
     * Pose glue only: builds the shard's blade from three tornRibbons, a
     * flat gold blade, a vertical steel-gold bevel, and a narrow white-gold
     * core, each turned so the ribbon runs along +Z (tip forward, hilt back
     * at -0.30, matching the spectacle blade's footprint). Emits no vertices
     * of its own.
     */
    private static void drawBespokeBlade(PoseStack.Pose pose, VertexConsumer buffer,
                                         long seed, float alpha, float distScale) {
        PoseStack flat = new PoseStack();
        flat.last().pose().set(pose.pose());
        flat.last().normal().set(pose.normal());
        flat.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-90.0f)));
        flat.translate(-0.30f, 0.0f, 0.0f);
        BespokeGeometry.tornRibbon(flat.last(), buffer, seed, 1.40f, 0.44f, 0.03f,
            1.0f, 0.88f, 0.25f, 0.95f * alpha, 1.0f, distScale);

        PoseStack vertical = new PoseStack();
        vertical.last().pose().set(pose.pose());
        vertical.last().normal().set(pose.normal());
        vertical.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-90.0f)));
        vertical.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        vertical.translate(-0.30f, 0.0f, 0.0f);
        BespokeGeometry.tornRibbon(vertical.last(), buffer, seed ^ 0x57EEL, 1.40f, 0.44f, 0.03f,
            0.55f, 0.75f, 0.95f, 0.85f * alpha, 1.0f, distScale);

        PoseStack core = new PoseStack();
        core.last().pose().set(pose.pose());
        core.last().normal().set(pose.normal());
        core.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-90.0f)));
        core.translate(-0.30f, 0.012f, 0.0f);
        BespokeGeometry.tornRibbon(core.last(), buffer, seed ^ 0xC02EL, 1.42f, 0.10f, 0.03f,
            1.0f, 1.0f, 0.85f, alpha, 1.0f, distScale);
    }

    /**
     * Pose glue only: turns one tornRibbon to trail behind the blade along
     * -Z, optionally standing it on edge, then calls the builder.
     */
    private static void drawWake(PoseStack.Pose pose, VertexConsumer buffer,
                                 long seed, float vis, float distScale, boolean onEdge) {
        PoseStack wake = new PoseStack();
        wake.last().pose().set(pose.pose());
        wake.last().normal().set(pose.normal());
        wake.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(90.0f)));
        if (onEdge) {
            wake.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        }
        wake.translate(0.10f, 0.0f, 0.0f);
        BespokeGeometry.tornRibbon(wake.last(), buffer, seed, 2.05f, 0.32f, 0.10f,
            1.0f, 0.85f, 0.20f, onEdge ? 0.40f : 0.55f, vis, distScale);
    }
}
