package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.AzureTornadoEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Azure Tornado in Minecraft 26.2.
 *
 * Spectacle pass: the funnel itself is the shared funnel standard,
 * LayeredRender.drawVortexBands (dense bright core, theme mid band, wispy
 * outer band, counter rotating, distance scaled), over ground suction,
 * orbiting wind shards, a ground anchor glow, and the crown outflow.
 * Sizes match the entity's physics zone exactly (radius scale x 8 giant /
 * x 3 mini, height scale x 16 / x 6); the renderer used to draw the giant
 * at 7 and the mini 6.5 high, so the visible funnel and the pull zone
 * disagreed. The fade is the shared FxEnvelope over the entity's real
 * max lifetime.
 *
 * Bespoke pass: the suction discs and crown are brokenVortexBand slices
 * and the orbiting shards are bladeArc crescents standing vertical in the
 * tangent plane, each in its own posed submit, all seeded by the tornado
 * entity's synced render seed. The local ring and triangle helpers are gone.
 */
public class AzureTornadoRenderer extends EntityRenderer<AzureTornadoEntity, AzureTornadoRenderer.TornadoRenderState> {

    public AzureTornadoRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class TornadoRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public boolean isGiant = false;
        public float age = 0;
        public int maxLife = 60;
        public long seed = 0;
        public double wx, wy, wz;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(AzureTornadoEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(AzureTornadoEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public TornadoRenderState createRenderState() {
        return new TornadoRenderState();
    }

    @Override
    public void extractRenderState(AzureTornadoEntity entity, TornadoRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getScale();
        state.isGiant = entity.isGiant();
        state.age = entity.tickCount + partialTicks;
        state.maxLife = entity.getMaxLifetime();
        state.seed = entity.getRenderSeed();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    @Override
    public void submit(TornadoRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        float scale = state.scale;
        boolean giant = state.isGiant;
        // Same numbers as the entity's physics zone, so what you see is what pulls you.
        float baseRadius = scale * (giant ? 8.0f : 3.0f);
        float totalHeight = scale * (giant ? 16.0f : 6.0f);
        float age = state.age;
        long seed = state.seed;

        float fade = FxEnvelope.envelope(age, Math.max(1.0f, state.maxLife));
        float ds = LayeredRender.distScale(cameraState, state.wx, state.wy, state.wz);

        poseStack.pushPose();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {

            // 1. Ground anchor glow at the funnel base.
            LayeredRender.drawAnchorGlow(pose, buffer, baseRadius * 0.85f, 0.06f,
                0.20f, 0.80f, 1.0f, 0.32f * fade, ds);

            // 2. Ground Whirlpool Suction Bands (Base vortex): two low broken
            // bands counter rotating at the old rings' speeds and alphas.
            float gRadius = baseRadius * 0.9f;
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x50C0L,
                gRadius * 0.92f, 0.02f, 0.17f, (float) Math.toRadians(age * 35.0),
                0.0f, 0.9f, 1.0f, (giant ? 0.70f : 0.50f) * fade, ds);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x50C1L,
                gRadius * 0.69f, 0.08f, 0.22f, (float) Math.toRadians(-age * 45.0),
                0.2f, 1.0f, 0.9f, (giant ? 0.60f : 0.40f) * fade, ds);

            // 3. The funnel: three counter rotating layered bands. The mid
            // band sits at 0.7 of the physics radius so the wispy outer band
            // (1.45x) lands right at the edge of the pull zone.
            LayeredRender.drawVortexBands(pose, buffer, baseRadius * 0.7f, totalHeight, age,
                0.25f, 0.80f, 1.0f, fade, ds);

            // 5. Crown Outflow Band (Flared upper storm rim): a broken band
            // at the top rim, counter rotating at the old crown ring's speed.
            float crownY = totalHeight;
            float crownR = baseRadius * 1.15f;
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xC207L,
                crownR * 0.92f, crownY - 0.20f, crownY + 0.04f, (float) Math.toRadians(-age * 22.0),
                0.0f, 0.8f, 1.0f, (giant ? 0.60f : 0.40f) * fade, ds);
        });

        // 4. Physical Orbiting Wind Blades / Kinetic Shards: bladeArc
        // crescents standing vertical in the tangent plane, each in its own
        // posed submit, riding the same orbit the old triangles flew
        // (same heights, radii, and angular speeds). Two nested arcs per
        // blade: a theme outer crescent and a pale core crescent.
        int shardCount = giant ? 12 : 6;
        for (int i = 0; i < shardCount; i++) {
            float shardProgress = (i / (float) shardCount);
            float shardY = shardProgress * totalHeight * 0.9f + 0.5f;
            float shardRadius = baseRadius * (0.4f + 0.6f * (shardY / totalHeight));

            double shardAngle = (shardProgress * Math.PI * 2.0) + (age * (25.0f + i * 4.0f) * (Math.PI / 180.0));

            float sx = (float) Math.cos(shardAngle) * shardRadius;
            float sz = (float) Math.sin(shardAngle) * shardRadius;

            final long shardSeed = seed ^ (0x51A2DL + i * 131L);
            final float outerAlpha = giant ? 0.85f : 0.65f;
            poseStack.pushPose();
            poseStack.translate(sx, shardY, sz);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(com.mojang.math.Axis.YP.rotationDegrees((float) -(Math.toDegrees(shardAngle) + 90.0))));
            poseStack.mulPose(new org.joml.Matrix4f().rotation(com.mojang.math.Axis.XP.rotationDegrees(-90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.bladeArc(pose, buffer, shardSeed,
                    0.65f * scale, 130f, 0.4f, 1.0f, 0.95f, outerAlpha, fade, ds);
                BespokeGeometry.bladeArc(pose, buffer, shardSeed ^ 3L,
                    0.42f * scale, 130f, 0.85f, 1.0f, 1.0f, outerAlpha * 0.8f, fade, ds);
            });
            poseStack.popPose();
        }

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
