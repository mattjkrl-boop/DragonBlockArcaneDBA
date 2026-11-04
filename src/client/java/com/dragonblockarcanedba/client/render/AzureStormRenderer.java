package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.AzureStormEntity;
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
 * Entity Renderer for Azure Storm Domain in Minecraft 26.2.
 * Renders a massive, cinematic 3D tempest weather domain:
 * - Double-layered displaced cyclone cloud dome (BespokeGeometry.displacedDome)
 * - Quad-tier counter-rotating ground whirlpool bands (brokenVortexBand)
 * - Branching atmospheric lightning discharges across the dome (branch)
 * - Central cyclonic tempest eye column of stacked broken bands
 *
 * Bespoke pass: every shape comes from BespokeGeometry, seeded by the
 * storm entity's synced render seed, so all clients draw the same torn,
 * irregular storm. Rain inside the storm zone is not drawn here at all:
 * the AtmosphereClient system renders storm rain for the zone the storm
 * server keeps open through AtmosphereServer.
 */
public class AzureStormRenderer extends EntityRenderer<AzureStormEntity, AzureStormRenderer.AzureStormRenderState> {

    public AzureStormRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class AzureStormRenderState extends EntityRenderState {
        public float radius = 12.0f;
        public float age = 0;
        public long seed = 0;
        public int lifeTicks = 220;
        public double wx, wy, wz;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(AzureStormEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(AzureStormEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public AzureStormRenderState createRenderState() {
        return new AzureStormRenderState();
    }

    @Override
    public void extractRenderState(AzureStormEntity entity, AzureStormRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getRenderSeed();
        state.lifeTicks = entity.getLifeTicks();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    @Override
    public void submit(AzureStormRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float radius = state.radius;
        float age = state.age;
        long seed = state.seed;
        RenderType renderType = KiRenderHelper.kiRenderType();

        // Distance scale: far storms step their segment counts down.
        float ds = LayeredRender.distScale(cameraState, state.wx, state.wy, state.wz);

        // Lifetime fade envelope from the shared FxEnvelope: in over the first
        // 20 ticks, out over the last 40, of whatever life this storm was
        // summoned with. The old constants assumed a fixed 220 tick life.
        float life = Math.max(20.0f, state.lifeTicks);
        float buildFrac = Math.min(0.45f, 20.0f / life);
        float decayFrac = Math.min(0.45f, 40.0f / life);
        float fade = FxEnvelope.envelope(age, life, buildFrac, decayFrac);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {

            // Ground anchor glow: a soft splash grounding the dome to the world.
            LayeredRender.drawAnchorGlow(pose, buffer, radius * 0.92f, 0.07f,
                0.20f, 0.75f, 1.0f, 0.35f * fade, ds);

            // 1. Quad-Tier Ground Whirlpool (4 counter-rotating wind bands),
            // now torn brokenVortexBand slices instead of perfect flat rings.
            // Twist is the same spin the rings had: age times the band speed.
            for (int band = 0; band < 4; band++) {
                float rOuter = radius * (0.30f + band * 0.23f);
                float y = 0.08f + (band * 0.06f);
                float rotSpeed = (band % 2 == 0 ? 1.0f : -1.35f) * (14.0f + band * 7.0f);
                float bandAlpha = (0.58f - (band * 0.09f)) * fade;

                float red = band == 0 ? 0.4f : 0.0f;
                float green = 0.75f + (band * 0.08f);
                float blue = 1.0f;

                BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ (0xBA0DL + band * 131L),
                    rOuter, y, y + 0.06f, (float) Math.toRadians(age * rotSpeed),
                    red, green, blue, bandAlpha, ds);
            }

            // 2. Volumetric Double-Layered Cyclone Cloud Dome: two displaced
            // shells, the inner at 0.8 of the radius and brighter, as before.
            // A displaced dome's height equals its radius, so the shell tops
            // out at the storm radius instead of the old 1.35x hemisphere.
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xD09EAL,
                radius, 0.04f, 0.48f, 0.82f, 0.34f, fade, ds);
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xD09EBL,
                radius * 0.8f, 0.10f, 0.62f, 1.0f, 0.26f, fade, ds);

            // 3. Rain: not drawn by this renderer. The AtmosphereClient system
            // renders storm rain inside the zone for the storm's whole life.

            // 4. Central Tempest Eye Updraft Column: six stacked broken bands
            // widening with height, near-white at the base cooling to a deeper
            // storm cyan as the updraft climbs, same gradient as before.
            float eyeHeight = radius;
            float eyeRadius = radius * 0.18f;
            int eyeLevels = 6;
            for (int lvl = 0; lvl < eyeLevels; lvl++) {
                float lt0 = lvl / (float) eyeLevels;
                float lt1 = (lvl + 1) / (float) eyeLevels;
                float ltMid = (lvl + 0.5f) / (float) eyeLevels;
                float er = eyeRadius * (0.6f + 0.4f * ltMid);
                float eyeR = 0.85f - (0.60f * ltMid);
                float eyeG = 1.0f - (0.20f * ltMid);
                float eyeB = 1.0f;
                float twist = (float) Math.toRadians(age * (30.0f + lvl * 5.0f));
                BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ (0xE7E0L + lvl * 131L),
                    er, lt0 * eyeHeight, lt1 * eyeHeight, twist,
                    eyeR, eyeG, eyeB, 0.45f * fade, ds);
            }

            // 5. Atmospheric Lightning Discharges within the Storm Boundary:
            // seeded branch ribbons, cyan sheath plus white core, reseeded on
            // the same 3 tick cadence the old beam walker used. Placement comes
            // from the toolkit's own seeded Rng fed by the synced storm seed.
            long bucket = (long) (age / 3.0f);
            BespokeGeometry.Rng rng = new BespokeGeometry.Rng(seed ^ (bucket * 1000L));
            int arcCount = Math.max(2, Math.round(4 * ds));
            for (int a = 0; a < arcCount; a++) {
                double startAngle = rng.nextFloat() * Math.PI * 2.0;
                float arcR = radius * (0.25f + rng.nextFloat() * 0.65f);
                float sx = (float) Math.cos(startAngle) * arcR;
                float sz = (float) Math.sin(startAngle) * arcR;
                float sy = 7.0f + rng.nextFloat() * 9.0f;
                float ex = sx + rng.signed() * 2.0f;
                float ez = sz + rng.signed() * 2.0f;
                float ey = Math.max(0.5f, sy - (7.0f + rng.nextFloat() * 6.0f));

                long arcSeed = seed ^ (bucket * 1000L) ^ (0xA2C0L + a * 977L);
                Vec3 from = new Vec3(sx, sy, sz);
                Vec3 to = new Vec3(ex, ey, ez);
                // Cyan sheath and the white core the lightning texture uses.
                BespokeGeometry.branch(pose, buffer, arcSeed, from, to, 0.32f,
                    0.0f, 0.85f, 1.0f, 0.75f, fade, ds);
                BespokeGeometry.branch(pose, buffer, arcSeed ^ 0xC0DEL, from, to, 0.10f,
                    0.95f, 1.0f, 1.0f, 0.95f, fade, ds);
                // A shorter fork off the middle of the arc, as the old walker drew.
                if (rng.nextFloat() < 0.6f) {
                    Vec3 mid = from.add(to).scale(0.5);
                    Vec3 fork = new Vec3(mid.x + rng.signed() * 1.6f,
                        Math.max(0.4f, mid.y - (0.8f + rng.nextFloat())),
                        mid.z + rng.signed() * 1.6f);
                    BespokeGeometry.branch(pose, buffer, arcSeed ^ 0xF02EL, mid, fork, 0.14f,
                        0.15f, 0.9f, 1.0f, 0.55f, fade, ds);
                }
            }
        });
    }
}
