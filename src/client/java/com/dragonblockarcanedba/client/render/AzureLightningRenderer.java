package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.AzureLightningEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

/**
 * Telegraph renderer for AzureLightningEntity.
 *
 * Spectacle pass: the bolt itself is drawn by the shared BoltEntity now, so
 * this renderer draws the warning and the aftermath. During the telegraph
 * window it draws an anchor glow and a hard ring at exactly the strike
 * radius (the warning's size equals the hit's size), a converging inner
 * ring that closes as the strike nears, and a thin charge column. After the
 * strike tick it draws an 8 tick afterglow ring that fades out. The old
 * textured 44 block bolt mesh is retired; the BoltEntity's branched seeded
 * bolt replaces it.
 *
 * Bespoke pass: the warning ring and afterglow ring are brokenVortexBand
 * slices whose columns straddle the strike radius, over the anchor glow
 * that fills the warned circle at exactly that radius; the converging ring
 * is a shrinking glowFan splash, and the charge column is a seeded branch
 * ribbon. All shapes take the lightning entity's synced render seed.
 * The local ring, beam, and quad helpers are gone.
 */
public class AzureLightningRenderer extends EntityRenderer<AzureLightningEntity, AzureLightningRenderer.AzureLightningRenderState> {

    public AzureLightningRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class AzureLightningRenderState extends EntityRenderState {
        public boolean isRare;
        public float age;
        public long seed;
        public int telegraphTicks = 12;
        public float radius = 3.5f;
        public double wx, wy, wz;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(AzureLightningEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(AzureLightningEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public AzureLightningRenderState createRenderState() {
        return new AzureLightningRenderState();
    }

    @Override
    public void extractRenderState(AzureLightningEntity entity, AzureLightningRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.isRare = entity.isRare();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getRenderSeed();
        state.telegraphTicks = entity.getTelegraphTicks();
        state.radius = entity.getRadius();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    @Override
    public void submit(AzureLightningRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float age = state.age;
        float strikeAt = Math.max(1.0f, state.telegraphTicks);
        float radius = state.radius;
        long seed = state.seed;
        if (age >= strikeAt + AzureLightningEntity.AFTERGLOW_TICKS) return;

        float ds = LayeredRender.distScale(cameraState, state.wx, state.wy, state.wz);

        // Theme colors: azure for a normal strike, pale gold for a rare one.
        float cr = state.isRare ? 1.0f : 0.15f;
        float cg = state.isRare ? 0.85f : 0.85f;
        float cb = state.isRare ? 0.45f : 1.0f;

        RenderType glowType = KiRenderHelper.kiRenderType();

        if (age < strikeAt) {
            // Telegraph phase: t runs 0 to 1 across the warning window.
            float t = FxEnvelope.smoothstep(age / strikeAt);
            // The warning pulses faster as the strike closes in.
            float blink = (Math.sin(age * (0.6 + t * 1.6)) > 0.0) ? 1.0f : 0.72f;
            float warnAlpha = (0.35f + 0.55f * t) * blink;

            collector.submitCustomGeometry(poseStack, glowType, (pose, buffer) -> {

                // Anchor glow filling the warned circle at exactly the radius.
                LayeredRender.drawAnchorGlow(pose, buffer, radius, 0.06f,
                    cr, cg, cb, (0.16f + 0.30f * t) * blink, ds);

                // Warning band at the damage radius: a thin broken band whose
                // jittered columns straddle the exact strike circle.
                BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x12A0L,
                    radius, 0.07f, 0.13f, 0.0f,
                    cr, cg, cb, warnAlpha, ds);

                // Converging splash: a glow fan that closes to the center
                // as t reaches 1, at the old converging ring's alpha.
                float convR = radius * (1.0f - t) + 0.35f;
                BespokeGeometry.glowFan(pose, buffer, seed ^ 0xC011L,
                    convR, 0.08f, 0.95f, 1.0f, 1.0f, 0.55f * t * blink, ds);

                // Thin charge column rising from the strike point: a seeded
                // branch ribbon the old crossed quads' width and height.
                float beamAlpha = (0.10f + 0.28f * t) * blink;
                BespokeGeometry.branch(pose, buffer, seed ^ 0xBEAAL,
                    new Vec3(0, 0, 0), new Vec3(0, 30.0f, 0), 0.44f,
                    cr, cg, cb, beamAlpha, 1.0f, ds);
            });
        } else {
            // Afterglow phase: the struck circle cools and fades over 8 ticks.
            float k = (age - strikeAt) / (float) AzureLightningEntity.AFTERGLOW_TICKS;
            float fade = 1.0f - FxEnvelope.smoothstep(k);

            collector.submitCustomGeometry(poseStack, glowType, (pose, buffer) -> {
                LayeredRender.drawAnchorGlow(pose, buffer, radius, 0.06f,
                    cr, cg, cb, 0.30f * fade, ds);
                BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xAF7EL,
                    radius, 0.07f, 0.13f, 0.0f,
                    cr, cg, cb, 0.55f * fade, ds);
            });
        }
    }
}
