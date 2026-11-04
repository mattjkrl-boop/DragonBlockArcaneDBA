package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.BanshoCycloneEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

/**
 * Entity Renderer for Bansho Fan Cyclone in Minecraft 26.2.
 * The funnel wall is the shared layered vortex (LayeredRender.drawVortexBands):
 * a bright core band, an emerald mid band, and a wispy outer band whose edge
 * sits exactly on the cyclone's 4 block effect radius, so what you see is
 * what hurts you. Everything else is bespoke toolkit geometry seeded from the
 * entity's synced id, so every client draws the same torn silhouette: a broken
 * suction band and glow fan eye at the foot, triple helical jade branch
 * ribbons climbing the wall, 8 orbiting blade arc scythes, and a broken crown
 * outflow band, all breathing on the same FxEnvelope as the entity.
 */
public class BanshoCycloneRenderer extends EntityRenderer<BanshoCycloneEntity, BanshoCycloneRenderer.CycloneRenderState> {

    public BanshoCycloneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class CycloneRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public float age = 0;
        public int lifetime = BanshoCycloneEntity.LIFE_TICKS;
        public double ex, ey, ez;
        public long seed;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(BanshoCycloneEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(BanshoCycloneEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public CycloneRenderState createRenderState() {
        return new CycloneRenderState();
    }

    @Override
    public void extractRenderState(BanshoCycloneEntity entity, CycloneRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getScale();
        state.age = entity.tickCount + partialTicks;
        state.lifetime = entity.getLifetime();
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
        // The entity id arrives in the spawn packet, so it is synced state:
        // every client seeds the same torn shapes for the same storm.
        state.seed = entity.getId() * 0x9E3779B97F4A7C15L ^ 0xBA550FADL;
    }

    @Override
    public void submit(CycloneRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        float scale = state.scale;
        float effectRadius = scale * 4.0f;
        float fullHeight = scale * 6.5f;
        float age = state.age;
        float life = Math.max(1, state.lifetime);

        // One envelope for everything, the same fractions the entity uses for
        // its damage window and forces, so visual and numbers agree.
        float envelope = FxEnvelope.envelope(age, life, BanshoCycloneEntity.BUILD_FRAC, BanshoCycloneEntity.DECAY_FRAC);
        if (envelope <= 0.01f) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }
        float growth = FxEnvelope.smoothstep(age / (life * BanshoCycloneEntity.BUILD_FRAC));
        float radiusNow = effectRadius * (0.45f + 0.55f * growth);
        float heightNow = fullHeight * (0.35f + 0.65f * growth);
        float distScale = LayeredRender.distScale(cameraState, state.ex, state.ey, state.ez);

        final float finalAlphaMult = envelope;
        final long seed = state.seed;
        final float gRadius = radiusNow * 0.95f;
        final float crownY = heightNow;
        final float crownR = radiusNow * 1.12f;

        poseStack.pushPose();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // Ground anchor glow: the storm is planted on the world, not pasted on it.
            LayeredRender.drawAnchorGlow(pose, buffer, radiusNow * 1.05f, 0.06f,
                0.15f, 1.0f, 0.65f, 0.30f * finalAlphaMult, distScale);

            // The funnel itself: three counter-rotating layered bands. The mid
            // band radius is chosen so the wispy outer band (1.45x) lands
            // exactly on the effect radius: the visible wall IS the hit wall.
            LayeredRender.drawVortexBands(pose, buffer, radiusNow / 1.45f, heightNow, age,
                0.15f, 1.0f, 0.65f, finalAlphaMult, distScale);

            // Suction skirt at the foot: a low broken vortex band grinding the
            // ground line, its twist carrying the old ring's rotation.
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 11L,
                gRadius * 0.86f, 0.02f, 0.17f, (float) Math.toRadians(age * 28.0),
                0.0f, 0.72f, 0.38f, 0.70f * finalAlphaMult, distScale);

            // The pale eye at the core of the foot: a splash of light, not a disc.
            BespokeGeometry.glowFan(pose, buffer, seed ^ 23L,
                gRadius * 0.24f, 0.09f,
                0.85f, 1.0f, 0.92f, 0.80f * finalAlphaMult, distScale);

            // Ascending triple-helical jade ribbons, riding the funnel wall.
            // Each strand is a chain of bespoke branch segments through the
            // same helix points the old ruled quads used; the branch jitter
            // tears the ribbon instead of ruling it. Gradient along the climb:
            // deep jade at the base brightening to pale jade at the top.
            int strands = 3;
            int ribbonSteps = 8;
            for (int s = 0; s < strands; s++) {
                float strandOffset = (s / (float) strands) * (float) (Math.PI * 2.0);
                for (int step = 0; step < ribbonSteps; step++) {
                    float t1 = step / (float) ribbonSteps;
                    float t2 = (step + 1) / (float) ribbonSteps;
                    float tm = (t1 + t2) * 0.5f;

                    double ang1 = strandOffset + (age * 22.0 * (Math.PI / 180.0)) + (t1 * Math.PI * 3.8);
                    double ang2 = strandOffset + (age * 22.0 * (Math.PI / 180.0)) + (t2 * Math.PI * 3.8);
                    float r1 = radiusNow * (0.35f + 0.65f * t1 * t1);
                    float r2 = radiusNow * (0.35f + 0.65f * t2 * t2);
                    Vec3 from = new Vec3(Math.cos(ang1) * r1, t1 * heightNow, Math.sin(ang1) * r1);
                    Vec3 to = new Vec3(Math.cos(ang2) * r2, t2 * heightNow, Math.sin(ang2) * r2);
                    BespokeGeometry.branch(pose, buffer, seed ^ (s * 131L + step * 17L + 5L),
                        from, to, 0.55f * scale * (0.6f + tm * 0.7f),
                        0.45f * tm, 1.0f, 0.48f + 0.34f * tm, 0.60f, finalAlphaMult, distScale);
                }
            }

            // Upper crown outflow: a broken band flaring at the top rim,
            // counter-rotating against the foot band.
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 37L,
                crownR * 0.9f, crownY - 0.20f, crownY + 0.04f, (float) Math.toRadians(-age * 26.0),
                0.05f, 0.95f, 0.65f, 0.50f * finalAlphaMult, distScale);
        });

        // Orbiting razor blades (8): bespoke blade arcs standing vertical in
        // the tangent plane, each in its own posed submit so the arc center
        // rides the tangent at its orbit point. Two nested arcs per blade:
        // a deep jade outer crescent and a pale core crescent.
        int bladeCount = 8;
        for (int i = 0; i < bladeCount; i++) {
            float bladeProgress = (i / (float) bladeCount);
            float bladeY = bladeProgress * heightNow * 0.85f + 0.4f;
            float bladeRadius = radiusNow * (0.45f + 0.55f * (bladeY / Math.max(0.001f, heightNow)));
            double bladeAngle = (bladeProgress * Math.PI * 2.0) + (age * (32.0f + i * 3.0f) * (Math.PI / 180.0));
            float sx = (float) Math.cos(bladeAngle) * bladeRadius;
            float sz = (float) Math.sin(bladeAngle) * bladeRadius;
            final long bladeSeed = seed ^ (0xB1ADEL + i);
            poseStack.pushPose();
            poseStack.translate(sx, bladeY, sz);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees((float) -(Math.toDegrees(bladeAngle) + 90.0))));
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(-90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.bladeArc(pose, buffer, bladeSeed,
                    0.55f * scale, 130f, 0.15f, 0.78f, 0.48f, 0.90f, finalAlphaMult, distScale);
                BespokeGeometry.bladeArc(pose, buffer, bladeSeed ^ 3L,
                    0.36f * scale, 130f, 0.88f, 1.0f, 0.95f, 0.90f, finalAlphaMult, distScale);
            });
            poseStack.popPose();
        }

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
