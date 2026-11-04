package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.AzureSonicQuakeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Sonic Quake Impact Shockwave in Minecraft 26.2.
 * Renders physical 3D supersonic ground shatter, vertical aerodynamic pressure needles, and radiant sonic wave crests.
 *
 * Bespoke pass: the beveled crest and its additive echo are brokenVortexBand
 * slices (a torn band standing at crest height), the inner compression ring
 * is a flat broken band, the pressure needles are a seeded shardFan radiating
 * at the crest's radius, and the epicenter is a crackStar with a plume dust
 * crown. All shapes take the quake entity's synced render seed. The local
 * ring and triangle helpers are gone.
 */
public class AzureSonicQuakeRenderer extends EntityRenderer<AzureSonicQuakeEntity, AzureSonicQuakeRenderer.QuakeRenderState> {

    public AzureSonicQuakeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class QuakeRenderState extends EntityRenderState {
        public float maxRadius = 8.5f;
        public float age = 0;
        public long seed = 0;
        public double wx, wy, wz;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(AzureSonicQuakeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(AzureSonicQuakeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public QuakeRenderState createRenderState() {
        return new QuakeRenderState();
    }

    @Override
    public void extractRenderState(AzureSonicQuakeEntity entity, QuakeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.maxRadius = entity.getMaxRadius();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getRenderSeed();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    @Override
    public void submit(QuakeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float maxLife = 18.0f;
        float progress = Math.min(1.0f, state.age / maxLife);
        if (progress >= 1.0f) return;

        float expansionEase = (float) Math.sin(progress * (Math.PI / 2.0));
        float currentRadius = Math.max(0.5f, expansionEase * state.maxRadius);
        float alpha = (1.0f - (progress * progress)) * 0.90f;
        long seed = state.seed;
        float ds = LayeredRender.distScale(cameraState, state.wx, state.wy, state.wz);

        RenderType renderType = KiRenderHelper.kiRenderType();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {

            // 1. High-Pressure Supersonic Shockwave Crest: a torn band
            // standing at the old beveled crest's height and radius, in a
            // blend of the old base and crest colors at the old alpha.
            float crestHeight = 0.65f * (1.0f - progress * 0.8f);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xC2E57L,
                currentRadius, 0.05f, 0.05f + crestHeight, 0.0f,
                0.30f, 0.95f, 1.0f, alpha, ds);

            // 2. Secondary Inner Sonic Compression Band, flat at the old
            // inner ring's radius and alpha.
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xC049L,
                currentRadius * 0.55f, 0.08f, 0.14f, 0.0f,
                0.0f, 1.0f, 0.8f, alpha * 0.65f, ds);

            // 3. Pressure Shards: a seeded shard fan radiating at the old
            // needle circle's radius, lengths and lift torn by the seed.
            BespokeGeometry.shardFan(pose, buffer, seed ^ 0x51A2DL,
                currentRadius * 0.85f, 0.4f, 0.95f, 1.0f, alpha * 0.85f, 1.0f, ds);

            // 4. Ground Burst Epicenter: a crack star at the old core's
            // radius in white, plus the dust crown as a plume.
            float coreRadius = Math.max(0.2f, (1.0f - progress) * 2.0f);
            BespokeGeometry.crackStar(pose, buffer, seed ^ 0xC2AC1L,
                coreRadius * 1.2f, 1.0f, 1.0f, 1.0f, alpha * 0.90f, 1.0f, ds);
            BespokeGeometry.plume(pose, buffer, seed ^ 0xD0571L,
                Math.max(0.6f, coreRadius), 0.2f + 1.4f * (1.0f - progress),
                0.45f, 0.75f, 0.95f, alpha * 0.45f, 1.0f, ds);
        });

        // T1: additive echo of the crest band, slightly wider, so the shock
        // crest glows instead of reading as a flat matte band.
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            float haloRadius = currentRadius * 1.05f;
            float haloCrest = 0.65f * (1.0f - progress * 0.8f);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xEC401L,
                haloRadius, 0.05f, 0.05f + haloCrest, 0.0f,
                0.45f, 0.55f, 0.55f, alpha * 0.28f, ds);
        });

        super.submit(state, poseStack, collector, cameraState);
    }
}
