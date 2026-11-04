package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.AbyssalDomainEntity;
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
 * The Abyssal Tempest Domain: a storm volume that reads as weather, not a
 * spell circle. The bespoke pass rebuilds every piece on BespokeGeometry:
 * the dome is two displacedDomes (outer and inner, counter-seeded so the
 * shells never align), the whirlpool floor and rim bands are
 * brokenVortexBands turning on rotated poses, the sixteen perimeter spires
 * are tornRibbons stood on end and leaning inward, the opening at the top
 * is a small bright displacedDome, and the interior strikes are branch
 * bolts flashing on an eight tick cycle. The shared LayeredRender vortex
 * bands and anchor glow stay exactly as the spectacle standard set them.
 * The envelope runs a forty tick build and the entity's thirty tick fade
 * so the domain gathers and dissipates instead of popping.
 */
public class AbyssalDomainRenderer extends EntityRenderer<AbyssalDomainEntity, AbyssalDomainRenderer.AbyssalDomainRenderState> {

    public AbyssalDomainRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class AbyssalDomainRenderState extends EntityRenderState {
        public float radius = 10f;
        public float age = 0;
        public long seed = 0;
        public int fadeRemaining = -1;
    }

    // Rendering stays camera relative: the domain must be visible from the
    // whole 10 to 30 block radius, and the default entity frustum cull would
    // hide it whenever the (tiny, invisible) entity origin leaves the screen.
    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(AbyssalDomainEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(AbyssalDomainEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public AbyssalDomainRenderState createRenderState() {
        return new AbyssalDomainRenderState();
    }

    @Override
    public void extractRenderState(AbyssalDomainEntity entity, AbyssalDomainRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
        int fade = entity.getFadeRemaining();
        if (fade >= 0) state.fadeRemaining = Math.max(0, fade - (int) Math.floor(partialTicks));
        else state.fadeRemaining = -1;
    }

    @Override
    public void submit(AbyssalDomainRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float radius = state.radius;
        long seed = state.seed;
        // Lifecycle envelope: forty tick build, then the fade comes from the
        // entity's own synced countdown once the item stops refreshing it.
        float build = FxEnvelope.smoothstep(Math.min(1.0f, state.age / 40.0f));
        float fadeFrac;
        if (state.fadeRemaining < 0) {
            fadeFrac = 1.0f;
        } else {
            fadeFrac = FxEnvelope.smoothstep(state.fadeRemaining / (float) AbyssalDomainEntity.FADE_TICKS);
        }
        float alpha = build * fadeFrac;
        if (alpha <= 0.01f) return;
        float ds = LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        float age = state.age;
        float open = FxEnvelope.smoothstep(Math.min(1.0f, Math.max(0.0f, (age - 20.0f) / 40.0f)));

        RenderType glowType = KiRenderHelper.kiRenderType();
        RenderType addType = KiRenderHelper.kiAdditiveType();

        // ---------------------------------------------------------------
        // Translucent pass: dome shells, whirlpool, rim, spires, bolts.
        // ---------------------------------------------------------------
        collector.submitCustomGeometry(poseStack, glowType, (pose, buffer) -> {
            // DOUBLE DOME: two displaced shells, inner denser and violet,
            // outer crimson and wider. The displacement is seeded, so the
            // dome wall is storm-torn instead of a smooth bubble.
            BespokeGeometry.displacedDome(pose, buffer, seed, radius,
                0.55f, 0.02f, 0.10f, 0.28f, alpha, ds);
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0x1D3EL, radius * 0.93f,
                0.45f, 0.02f, 0.65f, 0.38f, alpha, ds);

            // THE OPENING: a small bright dome cap riding the dome's top
            // point as the storm matures.
            if (open > 0.01f) {
                PoseStack.Pose cap = pose.copy();
                cap.translate(0, radius, 0);
                BespokeGeometry.displacedDome(cap, buffer, seed ^ 0xCA9L, radius * 0.25f,
                    0.45f, 0.85f, 0.95f, 0.45f, alpha * open, ds);
            }

            // STORM VORTEX BANDS: shared layered vortex language, same
            // radius, height, age, color, and envelope as the spectacle
            // pass (the toolkit now derives its own seeds from radius and
            // height, so no seed argument remains).
            LayeredRender.drawVortexBands(pose, buffer, radius * 1.05f, radius * 1.7f,
                age, 0.85f, 0.05f, 0.30f, alpha * 0.9f, ds);
            LayeredRender.drawAnchorGlow(pose, buffer, radius * 0.8f, 0.06f,
                0.85f, 0.05f, 0.20f, alpha * 0.5f * open, ds);

            // WHIRLPOOL FLOOR: four broken bands turning on rotated poses,
            // shrinking and rising toward the center.
            float[] fracs = {1.00f, 0.82f, 0.64f, 0.46f};
            float[][] palette = {
                {1.00f, 0.18f, 0.30f},
                {0.85f, 0.35f, 0.90f},
                {0.55f, 0.05f, 0.75f},
                {0.30f, 0.55f, 0.95f}
            };
            for (int b = 0; b < 4; b++) {
                PoseStack.Pose bandPose = pose.copy();
                bandPose.rotate(Axis.YP.rotation(-age * (0.05f + b * 0.02f)));
                BespokeGeometry.brokenVortexBand(bandPose, buffer, seed + b * 17L,
                    radius * fracs[b], 0.05f + b * 0.06f, 0.17f + b * 0.06f, -(0.9f + b * 0.35f),
                    palette[b][0], palette[b][1], palette[b][2], alpha * 0.85f, ds);
            }

            // MID-HEIGHT WIND RINGS: slow counter-turning broken bands.
            PoseStack.Pose midA = pose.copy();
            midA.rotate(Axis.YP.rotation(age * 0.06f));
            BespokeGeometry.brokenVortexBand(midA, buffer, seed ^ 0xA1DAL, radius * 1.02f,
                3.6f, 4.5f, 0.9f, 0.95f, 0.05f, 0.20f, alpha * 0.55f, ds);
            PoseStack.Pose midB = pose.copy();
            midB.rotate(Axis.YP.rotation(-age * 0.05f));
            BespokeGeometry.brokenVortexBand(midB, buffer, seed ^ 0xB1DBL, radius * 1.08f,
                radius * 0.42f, radius * 0.42f + 0.9f, -0.9f, 0.45f, 0.02f, 0.65f, alpha * 0.55f, ds);

            // GROUND RIM: two broken bands marking the domain's reach.
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xE1A1L, radius * 1.25f,
                0.0f, 0.3f, 0.8f, 0.68f, 0.02f, 0.14f, alpha * 0.5f, ds);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xE1A2L, radius * 1.5f,
                0.0f, 0.15f, -0.6f, 0.35f, 0.02f, 0.55f, alpha * 0.4f, ds);

            // PERIMETER SPIRES: sixteen torn ribbons stood on end, leaning
            // inward over the domain, breathing with the storm.
            int spires = 16;
            for (int i = 0; i < spires; i++) {
                float ang = (float) ((i / (double) spires) * Math.PI * 2.0);
                float sx = (float) Math.cos(ang) * radius * 1.02f;
                float sz = (float) Math.sin(ang) * radius * 1.02f;
                float spireMul = 0.95f + 0.25f * (float) Math.sin(seed * 0.13f + i * 2.39996f);
                float top = radius * 1.15f * spireMul * (1.0f + 0.05f * (float) Math.sin(age * 1.5f + i));
                PoseStack.Pose spire = pose.copy();
                spire.translate(sx, 0.2f, sz);
                spire.rotate(Axis.YP.rotation(-ang));
                spire.rotate(Axis.ZP.rotation((float) (Math.PI / 2.0 + 0.18)));
                BespokeGeometry.tornRibbon(spire, buffer, seed + i * 389L, Math.max(0.5f, top - 0.2f), 0.55f, 0.35f,
                    0.25f, 0.02f, 0.35f, 0.55f, alpha, ds);
            }

            // INTERIOR BOLTS: branch strikes flashing on an eight tick
            // cycle, seeded per cycle so the pattern never repeats within
            // a cast.
            int cycleIndex = (int) (age / 8.0f);
            float boltFlash = (age % 8.0f) < 4.0f ? 1.0f - ((age % 8.0f) / 4.0f) * 0.4f : 0.0f;
            if (boltFlash > 0.01f) {
                for (int b = 0; b < 3; b++) {
                    long boltSeed = seed + cycleIndex * 1000L + b * 7L;
                    BespokeGeometry.Rng brng = new BespokeGeometry.Rng(boltSeed);
                    float a1 = brng.nextFloat() * (float) (Math.PI * 2.0);
                    float a2 = brng.nextFloat() * (float) (Math.PI * 2.0);
                    Vec3 from = new Vec3(Math.cos(a1) * radius * 0.45f, radius * 0.75f, Math.sin(a1) * radius * 0.45f);
                    Vec3 to = new Vec3(Math.cos(a2) * radius * 0.2f, 0.2f, Math.sin(a2) * radius * 0.2f);
                    BespokeGeometry.branch(pose, buffer, boltSeed, from, to, 0.22f,
                        0.95f, 0.25f, 0.45f, alpha * boltFlash, 1.0f, ds);
                }
            }
        });

        // ---------------------------------------------------------------
        // Additive pass: the funnel halo, rim glow, and the storm's eye.
        // ---------------------------------------------------------------
        collector.submitCustomGeometry(poseStack, addType, (pose, buffer) -> {
            // Funnel: two counter-winding broken bands rising to the top.
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xF011L, radius * 0.80f,
                radius * 0.55f, radius * 1.05f, -age * 0.35f, 0.42f, 0.01f, 0.57f, alpha * 0.35f, ds);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xF022L, radius * 0.62f,
                radius * 0.60f, radius * 1.10f, age * 0.30f, 0.95f, 0.40f, 0.50f, alpha * 0.25f, ds);

            // Rim glow echo.
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xE1A1L, radius * 1.18f,
                0.0f, 0.2f, 0.8f, 0.55f, 0.03f, 0.15f, alpha * 0.30f, ds);

            // The eye: a bright small dome at the dome's top point.
            PoseStack.Pose eyePose = pose.copy();
            eyePose.translate(0, radius, 0);
            BespokeGeometry.displacedDome(eyePose, buffer, seed ^ 0xE7E9L, radius * 0.18f,
                0.70f, 0.90f, 1.0f, 0.50f, alpha, ds);

            LayeredRender.drawAnchorGlow(pose, buffer, radius * 1.1f, 0.05f,
                0.85f, 0.05f, 0.20f, alpha * 0.35f, ds);
        });
    }
}
