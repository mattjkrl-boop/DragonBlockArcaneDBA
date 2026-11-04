package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.AzureTempestChannelEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

/**
 * Entity Renderer for Call of the Tempest Channeling in Minecraft 26.2.
 * Renders an expanding physical 3D geometric wind tunnel, orbital swirling wind-blades, storm vortex base rings, and crackling lightning arcs.
 *
 * Bespoke pass: the base rings are brokenVortexBand slices, the tunnel is
 * the shared LayeredRender.drawVortexBands column, the orbiting blades are
 * bladeArc crescents in their own posed submits, and the electric arcs are
 * seeded branch ribbons. All shapes take the channel entity's synced
 * render seed. The local ring, beam, triangle, and quad helpers are gone.
 */
public class AzureTempestChannelRenderer extends EntityRenderer<AzureTempestChannelEntity, AzureTempestChannelRenderer.ChannelRenderState> {

    public AzureTempestChannelRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ChannelRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public float age = 0;
        public long seed = 0;
        public boolean isFirstPersonOwner = false;
        public boolean releasing = false;
        public int releaseAge = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(AzureTempestChannelEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(AzureTempestChannelEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ChannelRenderState createRenderState() {
        return new ChannelRenderState();
    }

    @Override
    public void extractRenderState(AzureTempestChannelEntity entity, ChannelRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getRenderSeed();
        state.releasing = entity.isReleasing();
        state.releaseAge = entity.getReleaseAge();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null && 
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) && 
            mc.options.getCameraType().isFirstPerson());
    }

    @Override
    public void submit(ChannelRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        float charge = state.chargeRatio;
        float age = state.age;
        long seed = state.seed;

        float tunnelRadius = (state.isFirstPersonOwner ? 2.4f : 1.6f) + (charge * 2.2f);
        float tunnelHeight = 3.2f + (charge * 2.4f);
        // Spawn fade-in over the first 5 ticks so the channel column does not
        // pop in at partial alpha the tick the channel starts.
        float intro = Math.min(1.0f, age / 5.0f);
        intro = intro * intro * (3.0f - 2.0f * intro);
        // Release fade-out: on release the entity collapses over
        // RELEASE_TICKS instead of vanishing in one tick at full alpha.
        float outro = state.releasing
            ? Math.max(0.0f, 1.0f - state.releaseAge / (float) AzureTempestChannelEntity.RELEASE_TICKS)
            : 1.0f;
        float baseAlpha = ((state.isFirstPersonOwner ? 0.35f : 0.55f) + (charge * 0.35f)) * intro * outro;

        // The channel sits on the caster, always near the camera that matters,
        // and the old renderer used fixed segment counts: distScale stays 1.
        float ds = 1.0f;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {

            // 1. Planar Runic Storm Vortex Base Array (3 counter-rotating
            // ground bands), now torn brokenVortexBand slices at the old
            // rings' radii, speeds, and alphas.
            for (int b = 0; b < 3; b++) {
                float rOut = (tunnelRadius * 0.7f) * (0.6f + b * 0.35f);
                float y = 0.05f + (b * 0.05f);
                float rotSpeed = (b % 2 == 0 ? 1.0f : -1.4f) * (20.0f + b * 12.0f);
                float bandAlpha = baseAlpha * (0.8f - b * 0.15f);

                BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ (0xBA5EL + b * 131L),
                    rOut, y, y + 0.06f, (float) Math.toRadians(age * rotSpeed),
                    0.0f, 0.85f + (b * 0.07f), 1.0f, bandAlpha, ds);
            }

            // 2. Expanding Cyclonic Wind Tunnel: the shared layered vortex
            // column, sized so its wispy outer band (1.45x) lands on the old
            // tunnel wall. Mid band alpha lands at the old tunnel alpha.
            LayeredRender.drawVortexBands(pose, buffer, tunnelRadius * 0.7f, tunnelHeight, age,
                0.10f, 0.80f, 1.0f, baseAlpha * 0.85f, ds);

            // 4. Physical 3D Electric Storm Arcs (Intensifies with charge):
            // seeded branch ribbons, reseeded on the same half tick cadence
            // the old beam walker used, placed by the toolkit's seeded Rng.
            if (charge >= 0.25f) {
                long bucket = (long) (age * 2.0f);
                BespokeGeometry.Rng rng = new BespokeGeometry.Rng(seed ^ (bucket * 1000L));
                int arcCount = 2 + (int) (charge * 4);

                for (int a = 0; a < arcCount; a++) {
                    double arcAngle = rng.nextFloat() * Math.PI * 2.0;
                    float arcR = tunnelRadius * (0.6f + rng.nextFloat() * 0.5f);
                    float sx = (float) Math.cos(arcAngle) * arcR;
                    float sz = (float) Math.sin(arcAngle) * arcR;
                    float sy = 0.2f + rng.nextFloat() * tunnelHeight;
                    Vec3 from = new Vec3(sx, sy, sz);
                    Vec3 to = new Vec3(sx + rng.signed() * 1.8f,
                        Math.max(0.1f, sy + rng.signed() * 1.5f),
                        sz + rng.signed() * 1.8f);

                    long arcSeed = seed ^ (bucket * 1000L) ^ (0xA2C5L + a * 131L);
                    // Cyan sheath and white core, the old beam pair's widths
                    // and alphas (the core drew at full alpha and still does).
                    BespokeGeometry.branch(pose, buffer, arcSeed, from, to, 0.16f,
                        0.0f, 0.95f, 1.0f, baseAlpha, 1.0f, ds);
                    BespokeGeometry.branch(pose, buffer, arcSeed ^ 0xC02EL, from, to, 0.06f,
                        1.0f, 1.0f, 1.0f, 1.0f, 1.0f, ds);
                }
            }
        });

        // 3. Orbital Swirling 3D Wind Blades: bladeArc crescents standing
        // vertical in the tangent plane, each in its own posed submit,
        // riding the same orbit the old triangle wings flew (same orbit
        // radius, bob, and angular speed, 4 to 8 blades by charge).
        int bladeCount = 4 + (int) (charge * 4);
        float bladeOrbitR = tunnelRadius * 0.92f;
        float orbitSpeed = 25.0f + (charge * 60.0f);
        float bladeLength = 0.9f + (charge * 0.5f);

        for (int i = 0; i < bladeCount; i++) {
            double bladeAngle = (i / (double) bladeCount) * Math.PI * 2.0 + (age * orbitSpeed * (Math.PI / 180.0));
            float bladeY = 0.8f + (float) Math.sin(age * 0.2f + i) * 0.4f + (i % 2 == 0 ? 0.3f : -0.2f);

            float bx = (float) Math.cos(bladeAngle) * bladeOrbitR;
            float bz = (float) Math.sin(bladeAngle) * bladeOrbitR;

            final long bladeSeed = seed ^ (0xB1ADEL + i * 131L);
            poseStack.pushPose();
            poseStack.translate(bx, bladeY, bz);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(com.mojang.math.Axis.YP.rotationDegrees((float) -(Math.toDegrees(bladeAngle) + 90.0))));
            poseStack.mulPose(new org.joml.Matrix4f().rotation(com.mojang.math.Axis.XP.rotationDegrees(-90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.bladeArc(pose, buffer, bladeSeed,
                    bladeLength, 130f, 0.2f, 1.0f, 1.0f, 0.95f, baseAlpha, ds);
                BespokeGeometry.bladeArc(pose, buffer, bladeSeed ^ 3L,
                    bladeLength * 0.65f, 130f, 0.8f, 1.0f, 1.0f, 0.75f, baseAlpha, ds);
            });
            poseStack.popPose();
        }

        super.submit(state, poseStack, collector, cameraState);
    }
}
