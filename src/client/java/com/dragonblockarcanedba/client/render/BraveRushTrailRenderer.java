package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.BraveRushTrailEntity;
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
 * Entity Renderer for Brave Sword Piercing Heroic Dash Flight Trail in Minecraft 26.2.
 * Bespoke pass: the hand-built Mach cones, helical streamline quads, and
 * wake ring are rebuilt as BespokeGeometry torn ribbons. Crossed ribbon
 * sheets (horizontal, vertical, and two diagonal) carry the old cone
 * layers (gold body, cyan inner, white-gold core beam) and the twin
 * streamline strands with their white cores, and a broken vortex band
 * replaces the expanding wake ring in the strike window. The seed comes
 * from the entity UUID, which is synced state, so every client draws the
 * same trail.
 */
public class BraveRushTrailRenderer extends EntityRenderer<BraveRushTrailEntity, BraveRushTrailRenderer.TrailRenderState> {

    public BraveRushTrailRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class TrailRenderState extends EntityRenderState {
        public float trailScale = 1.0f;
        public float trailLength = 10.0f;
        public float age = 0;
        public float yRot = 0;
        public float xRot = 0;
        public int maxLifetime = 30;
        public boolean isFirstPersonOwner = false;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(BraveRushTrailEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(BraveRushTrailEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public TrailRenderState createRenderState() {
        return new TrailRenderState();
    }

    @Override
    public void extractRenderState(BraveRushTrailEntity entity, TrailRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.trailScale = entity.getTrailScale();
        state.trailLength = entity.getTrailLength();
        state.age = entity.tickCount + partialTicks;
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.maxLifetime = entity.getMaxLifetime();
        state.seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null &&
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) &&
            mc.options.getCameraType().isFirstPerson());
    }

    @Override
    public void submit(TrailRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        // The lifetime comes from the entity, not a second hardcoded copy:
        // the trail now lives 30 ticks, long enough for the dash to read.
        float maxLife = (float) state.maxLifetime;
        float progress = Math.min(1.0f, state.age / maxLife);
        if (progress >= 1.0f) return;

        float open = FxEnvelope.smoothstep(Math.min(1.0f, progress / 0.25f));
        float alpha = FxEnvelope.envelope(state.age, maxLife, 0.25f, 0.4f) * 0.90f;
        float impact = strike(progress);
        float scale = state.trailScale * (state.isFirstPersonOwner ? 0.45f : 1.0f);
        float len = state.trailLength * (0.35f + 0.65f * open);
        float tipZ = state.isFirstPersonOwner ? (-0.6f * scale) : (0.6f * scale);
        float coneBase = (state.isFirstPersonOwner ? 1.05f : 0.90f) * scale;
        // The old cones drew at this fraction of the lifecycle alpha.
        float coneRatio = state.isFirstPersonOwner ? 0.32f : 0.72f;
        float age = state.age;
        long seed = state.seed;

        RenderType renderType = KiRenderHelper.kiRenderType();
        float distScale = LayeredRender.distScale(cameraState, state.x, state.y, state.z);

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        // Cone body sheets. A torn ribbon runs along +X in its own frame;
        // a quarter turn about Y lays it back along the flight line from
        // the tip, and rolls about Z cross the sheets into the old cone's
        // horizontal, vertical, and diagonal planes.
        // Horizontal sheet: gold body with the white-gold core beam.
        sheet(poseStack, collector, renderType, 0.0f, tipZ, (pose, buffer) -> {
            BespokeGeometry.tornRibbon(pose, buffer, seed, len, coneBase * 1.9f, 0.30f * scale,
                1.0f, 0.84f, 0.0f, coneRatio, alpha, distScale);
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0x2B992DDFL, len * 0.72f, coneBase * 0.24f, 0.12f * scale,
                1.0f, 0.98f, 0.85f, 1.0f, alpha, distScale);
        });
        // Vertical sheet: cyan inner cone with the white-gold core beam.
        sheet(poseStack, collector, renderType, 90.0f, tipZ, (pose, buffer) -> {
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0x51ED2701L, len * 0.85f, coneBase * 0.84f, 0.30f * scale,
                0.0f, 0.95f, 1.0f, coneRatio, alpha, distScale);
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0x85EBCA6BL, len * 0.72f, coneBase * 0.24f, 0.12f * scale,
                1.0f, 0.98f, 0.85f, 1.0f, alpha, distScale);
        });
        // Diagonal sheets: the twin streamline strands, gold and cyan,
        // each with its white core, as the header promises.
        sheet(poseStack, collector, renderType, 45.0f, tipZ, (pose, buffer) -> {
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0x27D4EB2FL, len, 0.20f * scale, 0.50f * scale,
                1.0f, 0.84f, 0.0f, 1.0f, alpha, distScale);
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0x165667B1L, len, 0.056f * scale, 0.50f * scale,
                1.0f, 0.98f, 0.9f, 1.0f, alpha, distScale);
        });
        sheet(poseStack, collector, renderType, -45.0f, tipZ, (pose, buffer) -> {
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0x3C6EF362L, len, 0.20f * scale, 0.50f * scale,
                0.0f, 0.95f, 1.0f, 1.0f, alpha, distScale);
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0x1B56C4E9L, len, 0.056f * scale, 0.50f * scale,
                1.0f, 0.98f, 0.9f, 1.0f, alpha, distScale);
        });

        // Strike wake: a broken vortex band standing across the flight
        // line at the tip, expanding with the strike bump, in place of the
        // old flat wake ring.
        if (impact > 0.02f) {
            float ringR = coneBase * (0.55f + 0.65f * impact);
            poseStack.pushPose();
            poseStack.translate(0.0f, 0.0f, tipZ);
            poseStack.mulPose(new Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x9E3779B9L, ringR,
                    -0.07f, 0.07f, age * 0.5f, 1.0f, 0.96f, 0.7f, impact * 0.85f, distScale);
            });
            poseStack.popPose();
        }

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    /**
     * Submits one ribbon sheet: positioned at the trail tip, run laid back
     * along the flight line, rolled rollDeg about the flight axis. The
     * roll is composed outside the quarter turn so it acts on the flight
     * axis itself. The geometry is supplied by the caller from
     * BespokeGeometry builders only.
     */
    private static void sheet(PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType,
                              float rollDeg, float tipZ,
                              net.minecraft.client.renderer.SubmitNodeCollector.CustomGeometryRenderer geometry) {
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.0f, tipZ);
        if (rollDeg != 0.0f) {
            poseStack.mulPose(new Matrix4f().rotation(Axis.ZP.rotationDegrees(rollDeg)));
        }
        poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, geometry);
        poseStack.popPose();
    }

    private static float strike(float progress) {
        if (progress >= 0.34f) return 0.0f;
        return (float) Math.sin((progress / 0.34f) * Math.PI);
    }
}
