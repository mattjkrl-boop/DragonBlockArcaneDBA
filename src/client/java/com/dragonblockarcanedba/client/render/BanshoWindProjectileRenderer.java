package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.BanshoWindProjectileEntity;
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
 * Entity Renderer for Bansho Wind Projectile in Minecraft 26.2.
 * A spinning wind drill drawn entirely from bespoke toolkit geometry seeded
 * by the entity's synced id: two counter-posed plumes form the drill body,
 * 4 blade arc scythes orbit it, a double torn ribbon helix wraps the body,
 * two glow fan wakes trail behind, and a jagged branch spike is the luminous
 * tip. The age envelope (in over the first ticks, out at the range cap) and
 * the first-person owner scale are unchanged.
 */
public class BanshoWindProjectileRenderer extends EntityRenderer<BanshoWindProjectileEntity, BanshoWindProjectileRenderer.WindProjectileRenderState> {

    public BanshoWindProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class WindProjectileRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public float ageInTicks = 0;
        public boolean isFirstPersonOwner = false;
        public double ex, ey, ez;
        public long seed;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(BanshoWindProjectileEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(BanshoWindProjectileEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public WindProjectileRenderState createRenderState() {
        return new WindProjectileRenderState();
    }

    @Override
    public void extractRenderState(BanshoWindProjectileEntity entity, WindProjectileRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.ageInTicks = entity.tickCount + partialTicks;
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
        state.seed = entity.getId() * 0x9E3779B97F4A7C15L ^ 0xD4115EEDL;

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null &&
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) &&
            mc.options.getCameraType().isFirstPerson());
    }

    @Override
    public void submit(WindProjectileRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        float age = state.ageInTicks;
        float fpScale = state.isFirstPersonOwner ? 0.55f : 1.0f;
        float distScale = LayeredRender.distScale(cameraState, state.ex, state.ey, state.ez);
        final long seed = state.seed;

        // Age envelope: in over the first 3 ticks, out over the last 8 before
        // the 50 tick range cap, so the drill never pops in or vanishes.
        float inT = Math.min(1.0f, age / 3.0f);
        float inEase = 1.0f - (1.0f - inT) * (1.0f - inT) * (1.0f - inT);
        float outT = age <= 42.0f ? 0.0f : Math.min(1.0f, (age - 42.0f) / 8.0f);
        final float envelope = inEase * (1.0f - outT * outT * (3.0f - 2.0f * outT));

        float drillLength = 1.35f * fpScale;
        // Secondary motion (T4): the drill radius breathes as it spins.
        float drillRadius = 0.32f * fpScale * (1.0f + 0.05f * (float) Math.sin(age * 1.5f));
        float tipZ = drillLength * 0.5f;

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        // The luminous tip: a jagged branch spike of wind past the drill
        // point, a wide emerald sheath and a white core branch inside it.
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.branch(pose, buffer, seed ^ 31L,
                new Vec3(0, 0, tipZ - 0.06f), new Vec3(0, 0, tipZ + 0.45f), 0.16f * fpScale,
                0.45f, 1.0f, 0.82f, 0.75f, envelope, distScale);
            BespokeGeometry.branch(pose, buffer, seed ^ 37L,
                new Vec3(0, 0, tipZ - 0.06f), new Vec3(0, 0, tipZ + 0.45f), 0.07f * fpScale,
                1.0f, 1.0f, 1.0f, 1.0f, envelope, distScale);
        });

        // The drill body: two bespoke plumes rotated onto the flight axis and
        // spun by the same 45 degrees per tick the old cone rotated. An
        // emerald outer plume and a brighter, thinner spindle plume inside.
        poseStack.pushPose();
        poseStack.translate(0, 0, -drillLength * 0.5f);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(age * 45.0f)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.plume(pose, buffer, seed ^ 41L,
                drillRadius * 0.9f, drillLength, 0.0f, 0.82f, 0.42f, 0.85f, envelope, distScale);
            BespokeGeometry.plume(pose, buffer, seed ^ 43L,
                drillRadius * 0.5f, drillLength * 0.96f, 0.7f, 1.0f, 0.9f, 0.95f, envelope, distScale);
        });
        poseStack.popPose();

        // Revolving razor scythes (4): bespoke blade arcs in the plane across
        // the flight axis, centered on the tangent at each orbit point, on
        // the old counter-rotation of 36 degrees per tick.
        int bladeCount = 4;
        float bladeOrbitR = drillRadius * 1.35f;
        float bladeRot = -age * 36.0f * (float) (Math.PI / 180.0);
        for (int b = 0; b < bladeCount; b++) {
            double bAngle = (b / (double) bladeCount) * Math.PI * 2.0 + bladeRot;
            float bx = (float) Math.cos(bAngle) * bladeOrbitR;
            float by = (float) Math.sin(bAngle) * bladeOrbitR;
            final long bladeSeed = seed ^ (53L + b * 7L);
            poseStack.pushPose();
            poseStack.translate(bx, by, 0.06f);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees((float) Math.toDegrees(bAngle) + 90.0f)));
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(-90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.bladeArc(pose, buffer, bladeSeed,
                    0.34f * fpScale, 120f, 0.05f, 0.72f, 0.38f, 0.85f, envelope, distScale);
                BespokeGeometry.bladeArc(pose, buffer, bladeSeed ^ 5L,
                    0.24f * fpScale, 120f, 0.82f, 1.0f, 0.92f, 0.92f, envelope, distScale);
            });
            poseStack.popPose();
        }

        // The double helix wrapped on the drill: torn ribbon segments chorded
        // along the same helix curve, 5 segments per strand, each posed to
        // its chord (yaw, then pitch) so the ribbon follows the wrap.
        int helixSegments = 5;
        float helixRot = age * 28.0f * (float) (Math.PI / 180.0);
        for (int helix = 0; helix < 2; helix++) {
            float phase = helix * (float) Math.PI;
            for (int s = 0; s < helixSegments; s++) {
                float t0 = s / (float) helixSegments;
                float t1 = (s + 1) / (float) helixSegments;
                float[] p0 = helixPoint(helixRot, phase, t0, drillLength, drillRadius);
                float[] p1 = helixPoint(helixRot, phase, t1, drillLength, drillRadius);
                float dx = p1[0] - p0[0];
                float dy = p1[1] - p0[1];
                float dz = p1[2] - p0[2];
                float chord = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (chord < 0.001f) continue;
                float horiz = (float) Math.sqrt(dx * dx + dz * dz);
                final long segSeed = seed ^ (131L + helix * 17L + s);
                final float segAlpha = 0.70f - t0 * 0.35f;
                final float segWidth = 0.075f * fpScale * (1.0f - t0 * 0.35f);
                final float cr = helix == 0 ? 0.15f : 0.75f;
                final float cb = helix == 0 ? 0.62f : 0.92f;
                poseStack.pushPose();
                poseStack.translate(p0[0], p0[1], p0[2]);
                poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees((float) Math.toDegrees(Math.atan2(-dz, dx)))));
                poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees((float) Math.toDegrees(Math.atan2(dy, horiz)))));
                collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                    BespokeGeometry.tornRibbon(pose, buffer, segSeed,
                        chord, segWidth, 0.0f, cr, 1.0f, cb, segAlpha, envelope, distScale));
                poseStack.popPose();
            }
        }

        // The tapered wake: two glow fan splashes facing backward down the
        // flight axis where the ruled vapor rings used to sit.
        float wakeR = drillRadius * 0.85f;
        poseStack.pushPose();
        poseStack.translate(0, 0, -drillLength * 0.55f);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.glowFan(pose, buffer, seed ^ 151L,
                wakeR, 0.0f, 0.15f, 1.0f, 0.75f, 0.55f * envelope, distScale));
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0, 0, -drillLength * 1.15f);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.glowFan(pose, buffer, seed ^ 157L,
                wakeR * 0.45f, 0.0f, 0.55f, 1.0f, 0.88f, 0.22f * envelope, distScale));
        poseStack.popPose();

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float[] helixPoint(float helixRot, float phase, float t, float drillLength, float drillRadius) {
        double a = helixRot + phase + t * Math.PI * 4.5;
        float rad = drillRadius * (1.08f - t * 0.30f);
        float z = -drillLength * 0.42f + t * drillLength * 0.85f;
        return new float[] { (float) Math.cos(a) * rad, (float) Math.sin(a) * rad, z };
    }
}
