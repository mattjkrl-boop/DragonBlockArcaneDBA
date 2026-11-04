package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.EvilSpearChargeEntity;
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
 * Entity Renderer for Evil Spear Left-Click (Evil Impale) Charge-up in Minecraft 26.2.
 * Renders physical 3D demonic crimson ground seals, contracting energy gathering rings,
 * orbiting blood shards, and a pulsating demonic core matrix.
 *
 * Bespoke pass (standard rule 1): every shape comes from BespokeGeometry or
 * LayeredRender, seeded from the entity UUID so all clients draw the same
 * silhouette. The old per-renderer regular helpers (equal-segment rings, the
 * octahedron, the shard crystal) are gone: seals and gathering rings are
 * broken vortex bands, the orbiting shards are shard fans, and the core is a
 * pair of noise-displaced domes per layer.
 */
public class EvilSpearChargeRenderer extends EntityRenderer<EvilSpearChargeEntity, EvilSpearChargeRenderer.ChargeRenderState> {

    public EvilSpearChargeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ChargeRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public float age = 0;
        public int releaseTicks = -1;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(EvilSpearChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(EvilSpearChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ChargeRenderState createRenderState() {
        return new ChargeRenderState();
    }

    @Override
    public void extractRenderState(EvilSpearChargeEntity entity, ChargeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.age = entity.tickCount + partialTicks;
        state.releaseTicks = entity.getReleaseTicks();
        // Synced state hash: the entity UUID is assigned on the server and
        // networked, so every client seeds the same shapes.
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(ChargeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        final float charge = state.chargeRatio;
        final float age = state.age;
        final long seed = state.seed;

        // Lifecycle (standard point 5): the seal builds in over its first 6
        // ticks instead of popping in at alpha 0.65 or more, and after the
        // throw releases it expands and dissipates over the release window
        // the entity reports, instead of cutting out in one frame.
        float spawnEnv = FxEnvelope.smoothstep(Math.min(1.0f, age / 6.0f));
        float releaseFade = 1.0f;
        float releaseExpand = 1.0f;
        if (state.releaseTicks >= 0) {
            float rt = Math.min(1.0f, state.releaseTicks / (float) EvilSpearChargeEntity.RELEASE_FADE_TICKS);
            releaseFade = 1.0f - rt;
            releaseExpand = 1.0f + rt * 0.35f;
        }
        final float lifecycle = spawnEnv * releaseFade;
        final float ds = LayeredRender.distScale(cameraState, state.x, state.y, state.z);

        final float baseRadius = (1.35f + (charge * 1.55f)) * releaseExpand;
        final float baseAlpha = (0.65f + (charge * 0.35f)) * lifecycle;
        final float pulse = 0.90f + 0.10f * (float) Math.sin(age * 0.55f);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // Shared layered column (standard point 1): three counter
            // rotating bands around the seal heart, breathing with the
            // charge and with the same lifecycle as the rest of the seal.
            LayeredRender.drawVortexBands(pose, buffer,
                baseRadius * 0.55f, 2.6f, age, 1.0f, 0.06f, 0.18f,
                (0.30f + 0.70f * charge) * lifecycle, ds);

            // 1. Demonic ground seals: three broken bands hugging the ground
            // where the equal-segment runic rings used to be, counter
            // rotating at the old ring speeds.
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x5EA11L,
                baseRadius * 1.09f, 0.03f, 0.16f, (float) Math.toRadians(age * -18.0),
                0.80f, 0.0f, 0.10f, baseAlpha * 0.85f, ds);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x5EA12L,
                baseRadius * 0.76f, 0.05f, 0.18f, (float) Math.toRadians(age * 28.0),
                1.0f, 0.05f, 0.20f, baseAlpha * 0.92f, ds);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x5EA13L,
                baseRadius * 0.42f, 0.07f, 0.20f, (float) Math.toRadians(age * -42.0),
                1.0f, 0.65f, 0.75f, baseAlpha * 0.98f, ds);

            // 2. Inward-gathering energy rings: four broken bands that
            // contract and climb toward the spear tip (y = 0.9) on the old
            // ring phase cycle.
            for (int r = 0; r < 4; r++) {
                float ringPhase = (age * 0.08f + (r / 4.0f)) % 1.0f;
                float currentR = (2.6f * (1.0f - ringPhase) + 0.35f * ringPhase) * pulse * (1.0f - charge * 0.3f);
                float currentY = 0.3f + ringPhase * 0.9f;
                float ringAlpha = (float) Math.sin(ringPhase * Math.PI) * baseAlpha * 0.85f;
                BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ (0x6A77L + r * 131L),
                    currentR, currentY, currentY + 0.13f, (float) Math.toRadians(age * (30.0 + r * 15.0)),
                    1.0f, 0.05f + ringPhase * 0.4f, 0.15f + ringPhase * 0.5f, ringAlpha, ds);
            }

            // A thin filament band tightens onto the seals while held.
            float filament = baseRadius * (1.02f - charge * 0.42f);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xF11A7L,
                filament, 0.12f, 0.25f, (float) Math.toRadians(age * 48.0),
                1.0f, 0.82f, 0.90f, baseAlpha * 0.72f, ds);

            // 3. Twin plasma vortex strands: two tall broken bands twisting
            // up the column where the helical ribbon pair used to climb.
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x57AA1L,
                baseRadius * 0.65f, 0.0f, 2.4f, age * 0.22f,
                1.0f, 0.08f, 0.22f, baseAlpha * 0.80f, ds);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x57AA2L,
                baseRadius * 0.62f, 0.0f, 2.4f, (float) Math.PI + age * 0.22f,
                0.85f, 0.0f, 0.40f, baseAlpha * 0.70f, ds);
        });

        // 4. Orbiting blood shards: seeded shard fans circling the core at
        // spear height, one flat, one vertical, counter rotating. The fans
        // replace the eight to fourteen hand-aimed crystal needles.
        final float orbitRadius = (2.3f - charge * 0.75f) * pulse;
        final float orbitDeg = age * (25.0f + charge * 60.0f);
        poseStack.pushPose();
        poseStack.translate(0, 0.9, 0);
        poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(orbitDeg)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.shardFan(pose, buffer, seed ^ 0xB1001L,
                orbitRadius, 0.75f, 0.0f, 0.12f, baseAlpha * 0.85f, 1.0f, ds);
            BespokeGeometry.shardFan(pose, buffer, seed ^ 0xB1002L,
                orbitRadius * 0.7f, 1.0f, 0.75f, 0.85f, baseAlpha * 0.98f, 1.0f, ds);
        });
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.9, 0);
        poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(-orbitDeg * 0.7f)));
        poseStack.mulPose(new Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.shardFan(pose, buffer, seed ^ 0xB1003L,
                orbitRadius * 0.85f, 0.85f, 0.05f, 0.20f, baseAlpha * 0.80f, 1.0f, ds);
        });
        poseStack.popPose();

        // 5. The core matrix: three nested layers, each a pair of displaced
        // domes (one up, one flipped) where the octahedra used to sit.
        // Dark shell, blood mid, and a heart that brightens as charge fills.
        float coreSize = (0.62f + charge * 0.18f) * pulse;
        drawCoreLayer(collector, poseStack, renderType, seed ^ 0xC0DE1L,
            coreSize, age * 24.0f, 0.85f, 0.0f, 0.12f, baseAlpha * 0.82f, ds);
        drawCoreLayer(collector, poseStack, renderType, seed ^ 0xC0DE2L,
            coreSize * 0.58f, -age * 36.0f, 1.0f, 0.10f, 0.28f, baseAlpha * 0.95f, ds);
        float heart = Math.min(1.0f, charge);
        drawCoreLayer(collector, poseStack, renderType, seed ^ 0xC0DE3L,
            coreSize * 0.26f, age * 60.0f,
            0.85f + 0.15f * heart, 0.12f + 0.78f * heart, 0.22f + 0.73f * heart, baseAlpha, ds);

        super.submit(state, poseStack, collector, cameraState);
    }

    /**
     * One core layer at spear height: a displaced dome opening up and its
     * flipped twin opening down, spun together. Orchestration only; all
     * geometry comes from BespokeGeometry.
     */
    private static void drawCoreLayer(SubmitNodeCollector collector, PoseStack poseStack, RenderType renderType,
                                      long seed, float size, float rotDeg,
                                      float r, float g, float b, float alpha, float ds) {
        poseStack.pushPose();
        poseStack.translate(0, 0.85, 0);
        poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(rotDeg)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.displacedDome(pose, buffer, seed, size, r, g, b, alpha, 1.0f, ds));
        poseStack.mulPose(new Matrix4f().rotation(Axis.XP.rotationDegrees(180.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xF1119L, size, r, g, b, alpha, 1.0f, ds));
        poseStack.popPose();
    }
}
