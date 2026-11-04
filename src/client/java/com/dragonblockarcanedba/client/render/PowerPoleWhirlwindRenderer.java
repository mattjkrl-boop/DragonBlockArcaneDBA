package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.PowerPoleWhirlwindEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Power Pole Whirlwind Staff in Minecraft 26.2.
 * Renders a physical 3D conical aerodynamic hurricane vortex:
 * - Staff-origin glow splash where the pole spins
 * - A conical wind tunnel of stacked broken vortex bands (cyan / emerald / white)
 * - Torn ribbon streaks racing along the cone between the sheaths
 * - 10 crescent gale blade arcs orbiting along the cone
 * - Traveling supersonic Mach air compression shock bands
 *
 * Bespoke pass: every shape comes from BespokeGeometry or LayeredRender
 * (BESPOKE-STANDARD rule 1). The old per-renderer segment loops and local
 * draw helpers are gone. Seeds hash from the entity's synced state, so the
 * torn silhouettes are identical on every client and stable frame to frame.
 */
public class PowerPoleWhirlwindRenderer extends EntityRenderer<PowerPoleWhirlwindEntity, PowerPoleWhirlwindRenderer.WhirlwindRenderState> {

    public PowerPoleWhirlwindRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class WhirlwindRenderState extends EntityRenderState {
        public float yRot = 0.0f;
        public float xRot = 0.0f;
        public float range = 25.0f;
        public float coneAngle = 35.0f;
        public float age = 0.0f;
        public int decayStart = -1;
        public long seed = 0;
        public boolean isFirstPersonOwner = false;
        public boolean onRight = true;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(PowerPoleWhirlwindEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(PowerPoleWhirlwindEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public WhirlwindRenderState createRenderState() {
        return new WhirlwindRenderState();
    }

    @Override
    public void extractRenderState(PowerPoleWhirlwindEntity entity, WhirlwindRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getEntityYaw();
        state.xRot = entity.getEntityPitch();
        state.range = entity.getRange();
        state.coneAngle = entity.getConeAngle();
        state.age = entity.tickCount + partialTicks;
        state.decayStart = entity.getDecayStart();
        // Bespoke geometry seed: hashed from the entity's SYNCED state only
        // (caster id, range, cone angle), never from client random or wall
        // clock, so every client draws the same torn silhouette.
        state.seed = (entity.getCasterId() * 0x9E3779B97F4A7C15L)
            ^ (Float.floatToIntBits(entity.getRange()) * 31L)
            ^ Float.floatToIntBits(entity.getConeAngle());

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null &&
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) &&
            mc.options.getCameraType().isFirstPerson());
        if (mc.player != null) {
            boolean isRightHanded = (mc.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            boolean isOffhand = (mc.player.getOffhandItem().getItem() instanceof com.dragonblockarcanedba.item.PowerPoleItem &&
                !(mc.player.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.PowerPoleItem));
            state.onRight = isRightHanded ? !isOffhand : isOffhand;
        }
    }

    @Override
    public void submit(WhirlwindRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float range = state.range;
        if (range < 1.0f) return;

        float age = state.age;

        // Lifecycle envelope: the SAME phase math the entity runs server side
        // (PowerPoleWhirlwindEntity.currentEnvelope), so mesh alpha, particle
        // counts, the damage window, and the vortex forces all agree. The
        // funnel is one sustained event now: build over BUILD_TICKS, full
        // while the spin is held, dissipate over DECAY_TICKS after DECAY_START.
        // The old 8 tick envelope restarted forever because the entity was
        // respawned every 6 ticks.
        float alphaMult;
        if (state.decayStart >= 0) {
            // Decay falls from whatever height the build had reached when the
            // spin stopped (an interrupted build never jumps up to full).
            float peak = state.decayStart < PowerPoleWhirlwindEntity.BUILD_TICKS
                ? com.dragonblockarcanedba.util.FxEnvelope.smoothstep(
                    state.decayStart / (float) PowerPoleWhirlwindEntity.BUILD_TICKS)
                : 1.0f;
            alphaMult = age <= state.decayStart
                ? peak
                : peak * (1.0f - com.dragonblockarcanedba.util.FxEnvelope.smoothstep(
                    (age - state.decayStart) / (float) PowerPoleWhirlwindEntity.DECAY_TICKS));
        } else if (age < PowerPoleWhirlwindEntity.BUILD_TICKS) {
            alphaMult = com.dragonblockarcanedba.util.FxEnvelope.smoothstep(
                age / (float) PowerPoleWhirlwindEntity.BUILD_TICKS);
        } else {
            alphaMult = 1.0f;
        }
        if (alphaMult <= 0.01f) return;

        final float finalAlpha = alphaMult;
        // The funnel swells as it builds and tightens as it dies.
        final float swell = 0.55f + 0.45f * alphaMult;
        final float coneTan = (float) Math.tan(Math.toRadians(state.coneAngle));
        // Distance scaling (standard point 1): far funnels step their segment
        // counts down inside the builders instead of costing full geometry.
        final float distScale = com.dragonblockarcanedba.client.render.ki.LayeredRender.distScale(
            cameraState, state.x, state.y, state.z);
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        float fpScale = state.isFirstPersonOwner ? 0.55f : 1.0f;
        final float spinRot = age * 65.0f * (float) (Math.PI / 180.0);
        final float discR = 2.2f * fpScale * swell;
        final long seed = state.seed;
        final float rangeF = range;

        // Axial frame: the bespoke builders grow along +Y, so turn the pose
        // once and let builder height run down the cone axis (local +Z).
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // Staff origin: a jittered glow splash where the pole spins,
            // replacing the old equal-segment origin ring fan.
            com.dragonblockarcanedba.client.render.ki.LayeredRender.drawAnchorGlow(
                pose, buffer, discR, 0.0f, 0.0f, 1.0f, 0.75f, 0.70f * finalAlpha, distScale);

            // Cone tunnel: stacked broken vortex bands widening down the
            // axis. The cyan-white core sheath sits inside, the emerald
            // pressure sheath outside, and torn ribbon streaks ride the old
            // helix radius between them. Band k covers one sixth of the
            // cone, colored by the same 3-stop gradients the tier loops used.
            int bands = 6;
            for (int k = 0; k < bands; k++) {
                float t0 = k / (float) bands;
                float t1 = (k + 1) / (float) bands;
                float tm = (t0 + t1) * 0.5f;

                float crA, cgA, cbA;
                if (tm < 0.5f) {
                    float gk = tm / 0.5f;
                    crA = 0.95f + (0.75f - 0.95f) * gk; cgA = 1.0f; cbA = 1.0f;
                } else {
                    float gk = (tm - 0.5f) / 0.5f;
                    crA = 0.75f + (0.40f - 0.75f) * gk; cgA = 1.0f + (0.92f - 1.0f) * gk; cbA = 1.0f;
                }
                float crB, cgB, cbB;
                if (tm < 0.5f) {
                    float gk = tm / 0.5f;
                    crB = 0.45f + (0.0f - 0.45f) * gk; cgB = 1.0f; cbB = 0.80f + (0.55f - 0.80f) * gk;
                } else {
                    float gk = (tm - 0.5f) / 0.5f;
                    crB = 0.0f; cgB = 1.0f + (0.80f - 1.0f) * gk; cbB = 0.55f + (0.42f - 0.55f) * gk;
                }

                float y0A = 0.3f + t0 * rangeF;
                float y1A = 0.3f + t1 * rangeF;
                float alphaA = 0.55f * (1.0f - tm * 0.45f) * finalAlpha;
                com.dragonblockarcanedba.client.render.ki.BespokeGeometry.brokenVortexBand(
                    pose, buffer, seed ^ (k * 131L), y0A * coneTan * 0.65f * swell, y0A, y1A,
                    spinRot + y0A * 0.35f, crA, cgA, cbA, alphaA, distScale);

                float y0B = 0.5f + t0 * rangeF;
                float y1B = 0.5f + t1 * rangeF;
                float alphaB = 0.40f * (1.0f - tm * 0.4f) * finalAlpha;
                com.dragonblockarcanedba.client.render.ki.BespokeGeometry.brokenVortexBand(
                    pose, buffer, seed ^ 0xB1L ^ (k * 197L), y0B * coneTan * 0.95f * swell, y0B, y1B,
                    -spinRot * 0.8f + y0B * 0.25f, crB, cgB, cbB, alphaB, distScale);

                // Ribbon streak: alternating cyan and emerald, its twist
                // racing ahead down the cone like the old helical ribbons.
                boolean cyan = (k % 2 == 0);
                float alphaR = 0.55f * (1.0f - tm * 0.5f) * finalAlpha;
                com.dragonblockarcanedba.client.render.ki.BespokeGeometry.brokenVortexBand(
                    pose, buffer, seed ^ 0x1213L ^ (k * 57L), y0A * coneTan * 0.85f * swell, y0A + 0.1f, y1A,
                    spinRot + tm * (float) Math.PI * 3.5f,
                    cyan ? 0.2f : 0.0f, 1.0f, cyan ? 0.95f : 0.60f, alphaR, distScale);
            }

            // Shock rings running the length of the cone, plus the one ring
            // that keeps traveling to the mouth for as long as the funnel
            // lives: short torn bands now, same timing, radii, and alphas.
            for (int w = 0; w < 3; w++) {
                float waveProgress = ((age * 0.30f) + (w / 3.0f)) % 1.0f;
                float waveEase = waveProgress * waveProgress * (3.0f - 2.0f * waveProgress);
                float waveZ = 0.5f + waveEase * Math.max(0.2f, rangeF - 0.8f);
                float waveR = waveZ * coneTan * 0.92f * swell;
                float waveAlpha = (float) Math.sin(waveProgress * (float) Math.PI) * 0.65f * finalAlpha;
                com.dragonblockarcanedba.client.render.ki.BespokeGeometry.brokenVortexBand(
                    pose, buffer, seed ^ (0x5EEDL + w * 31L), waveR, waveZ, waveZ + 0.4f,
                    spinRot * 0.5f, 0.55f, 1.0f, 0.92f, waveAlpha, distScale);
            }
            float mouthProgress = (age * 0.12f) % 1.0f;
            float mouth = mouthProgress * mouthProgress * (3.0f - 2.0f * mouthProgress);
            float mouthZ = 0.6f + mouth * Math.max(0.2f, rangeF - 0.8f);
            float mouthR = mouthZ * coneTan * 0.95f * swell;
            float mouthA = (float) Math.sin(mouthProgress * (float) Math.PI) * 0.55f * finalAlpha;
            com.dragonblockarcanedba.client.render.ki.BespokeGeometry.brokenVortexBand(
                pose, buffer, seed ^ 0x404L, mouthR, mouthZ, mouthZ + 0.4f,
                spinRot * 0.5f, 0.85f, 1.0f, 0.95f, mouthA, distScale);
        });
        poseStack.popPose();

        // Crescent cutters riding the cone: seeded blade arcs now, an outer
        // emerald crescent with a bright core arc inside it, positioned by
        // the same orbit math the old hand-built crescents used.
        int bladeCount = 10;
        for (int b = 0; b < bladeCount; b++) {
            float bProgress = ((b / (float) bladeCount) + (age * 0.18f)) % 1.0f;
            float bZ = bProgress * range + 0.6f;
            float bRadius = bZ * coneTan * 0.82f * swell;
            double bAngle = (b / (float) bladeCount) * Math.PI * 2.0 + (spinRot * 1.5) + (bProgress * Math.PI * 2.5);
            final float bx = (float) Math.cos(bAngle) * bRadius;
            final float by = (float) Math.sin(bAngle) * bRadius;
            final float bladeLen = 0.85f + bProgress * 0.55f;
            float bladeFade = (1.0f - bProgress);
            bladeFade = bladeFade * bladeFade;
            final float bladeAlpha = 0.9f * finalAlpha * (0.4f + 0.6f * bladeFade);
            final long bladeSeed = seed ^ (b * 0x9E37L);
            final float bladeZ = bZ;
            final float angDeg = (float) Math.toDegrees(bAngle);
            poseStack.pushPose();
            poseStack.translate(bx, by, bladeZ);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(angDeg)));
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                com.dragonblockarcanedba.client.render.ki.BespokeGeometry.bladeArc(
                    pose, buffer, bladeSeed, bladeLen, 160.0f,
                    0.05f, 0.82f, 0.62f, bladeAlpha, 1.0f, distScale);
                com.dragonblockarcanedba.client.render.ki.BespokeGeometry.bladeArc(
                    pose, buffer, bladeSeed ^ 0xC0DEL, bladeLen * 0.45f, 160.0f,
                    0.9f, 1.0f, 1.0f, bladeAlpha, 1.0f, distScale);
            });
            poseStack.popPose();
        }

        // Shared layered vortex bands (standard point 1): three counter
        // rotating layers (bright core, theme emerald mid, wispy outer) laid
        // along the cone axis. The band frame is turned so the wide bright
        // base sits at the cone mouth and the column tightens back toward
        // the staff, the funnel silhouette of the reference storm.
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.0f, range * 0.98f);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(-90.0f)));
        final float bandRadius = range * coneTan * 0.55f * swell;
        final float bandHeight = range * 0.92f;
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            com.dragonblockarcanedba.client.render.ki.LayeredRender.drawVortexBands(
                pose, buffer, bandRadius, bandHeight, age, 0.15f, 1.0f, 0.65f, finalAlpha, distScale);
        });
        poseStack.popPose();

        poseStack.popPose();
    }
}
