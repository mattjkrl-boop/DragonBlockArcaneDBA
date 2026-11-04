package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.HellHuntImpactEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Hell Hunt Impact in MC 26.2.
 * Renders physical 3D demonic ground-shatter fissure trenches, erupting jagged blood/obsidian impalement spikes,
 * expanding shockwave execution seals, and surging vertical demonic execution pillars.
 *
 * Bespoke pass (standard rule 1): every shape comes from BespokeGeometry or
 * LayeredRender, seeded from the entity UUID. The old renderer seeded a
 * client side random generator from the same bits inside the draw code;
 * the seed now flows through the standard path instead. The equal-segment rings are
 * broken vortex bands, the trench ribbons are crack stars, the spike ring
 * is a pair of shard fans, and the tapered cone pillar is a plume. The
 * local geometry helpers (drawGroundRing, drawTrenchRibbon, drawSpike,
 * face, drawConePillar) are deleted.
 */
public class HellHuntImpactRenderer extends EntityRenderer<HellHuntImpactEntity, HellHuntImpactRenderer.HellHuntRenderState> {

    public HellHuntImpactRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class HellHuntRenderState extends EntityRenderState {
        public float radius = 3.0f;
        public boolean isFinal = false;
        public float age = 0;
        public int lifetime = 35;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(HellHuntImpactEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(HellHuntImpactEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public HellHuntRenderState createRenderState() {
        return new HellHuntRenderState();
    }

    @Override
    public void extractRenderState(HellHuntImpactEntity entity, HellHuntRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.isFinal = entity.isFinal();
        state.age = entity.tickCount + partialTicks;
        // The client side entity never runs the convenience constructor, so
        // the total life comes from the synced final flag and the shared
        // constants, not from the entity's server side countdown field.
        state.lifetime = state.isFinal ? HellHuntImpactEntity.LIFE_FINAL : HellHuntImpactEntity.LIFE_NORMAL;
        // The entity UUID is assigned on the server and networked, so every
        // client seeds the same band breaks, cracks, and shards.
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(HellHuntRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        final float radius = state.radius;
        final float age = state.age;
        final long seed = state.seed;
        final boolean isFinal = state.isFinal;
        // Shared lifecycle envelope (standard point 5): builds over the
        // first fifth of life, peaks, then dissipates over the last third,
        // so the renderer agrees with the entity side particles and scar.
        float lifeT = Math.min(1.0f, age / (float) state.lifetime);
        final float fade = FxEnvelope.envelope(age, state.lifetime);
        final float ds = LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        RenderType renderType = KiRenderHelper.kiRenderType();

        // Spike ring timing, unchanged from the old spike draw: rise over
        // the first 8 ticks, then sink away over the last 45 percent of
        // life. The fans carry the same factor through their radius.
        float riseT = Math.min(1.0f, age / 8.0f);
        float rise = 1.0f - (1.0f - riseT) * (1.0f - riseT) * (1.0f - riseT);
        float sinkRaw = lifeT < 0.55f ? 0.0f : (lifeT - 0.55f) / 0.45f;
        float sinkT = sinkRaw * sinkRaw * (3.0f - 2.0f * sinkRaw);
        final float spikeScale = rise * (1.0f - sinkT);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // 1. Expanding Demonic Ground Impact Shockwave Seals.
            // The seals counter-rotate slowly; the broken bands keep the
            // old ring radii (band at each ring's mid radius), heights,
            // colors, alphas, and spin speeds.
            float growT = Math.min(1.0f, age / 10.0f);
            float grow = 1.0f - (1.0f - growT) * (1.0f - growT) * (1.0f - growT);
            float shockR = radius * 1.35f * grow;
            if (fade > 0.05f) {
                // Outer Shock Seal: Dark Crimson
                BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xA101L,
                    shockR * 0.91f, 0.03f, 0.15f, (float) Math.toRadians(age * 10.0),
                    0.85f, 0.0f, 0.15f, fade * 0.85f, ds);
                // Inner Blood Seal
                BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xA102L,
                    shockR * 0.52f, 0.04f, 0.16f, (float) Math.toRadians(age * -14.0),
                    1.0f, 0.05f, 0.25f, fade * 0.75f, ds);
                // Tight bright core
                BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xA103L,
                    shockR * 0.17f, 0.06f, 0.18f, (float) Math.toRadians(age * 22.0),
                    1.0f, 0.88f, 0.92f, fade * 0.90f, ds);
                if (isFinal) {
                    // Extra Halo on Final Finisher Impact
                    BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xA104L,
                        shockR * 1.05f, 0.05f, 0.17f, (float) Math.toRadians(age * 6.0),
                        1.0f, 0.85f, 0.90f, fade * 0.90f, ds);
                }

                // Shared layered language (standard point 1): an anchor glow
                // grounding the seal, and a three band vortex column while
                // the execution pillar stands, so the impact reads with the
                // same depth as every other spectacle effect.
                LayeredRender.drawAnchorGlow(pose, buffer,
                    shockR * 0.95f, 0.02f, 1.0f, 0.10f, 0.22f, 0.50f * fade, ds);
                float bandEnv = fade * (age <= 14f ? 1.0f : Math.max(0f, 1.0f - (age - 14f) / 10f));
                LayeredRender.drawVortexBands(pose, buffer,
                    radius * 0.5f, isFinal ? 4.2f : 3.0f, age, 1.0f, 0.07f, 0.16f, bandEnv, ds);
            }

            // 2. Radiating fissures: layered crack stars where the straight
            // trench ribbons used to run, dark cracks below a bright branch
            // layer in the old trench colors, growing with the shock.
            BespokeGeometry.crackStar(pose, buffer, seed ^ 0xC101L,
                radius * 1.15f * grow, 0.45f, 0.0f, 0.08f, 0.95f, fade, ds);
            BespokeGeometry.crackStar(pose, buffer, seed ^ 0xC102L,
                radius * 0.8f * grow, 1.0f, 0.35f, 0.40f, 0.80f, fade, ds);

            // 3. Eruption shards: two seeded shard fans radiating where the
            // regular spike ring used to stand, dark sheath outside and
            // blood core inside, on the old rise and sink timing.
            if (spikeScale > 0.02f && fade > 0.01f) {
                BespokeGeometry.shardFan(pose, buffer, seed ^ 0x5101L,
                    radius * 0.95f * spikeScale, 0.20f, 0.0f, 0.05f, 0.95f, fade, ds);
                BespokeGeometry.shardFan(pose, buffer, seed ^ 0x5102L,
                    radius * 0.75f * spikeScale, 1.0f, 0.10f, 0.25f, 0.90f, fade, ds);
            }

            // 4. Surging Vertical Demonic Execution Pillar.
            float pillarLife = isFinal ? 18.0f : 12.0f;
            if (age < pillarLife) {
                float pillarProgress = age / pillarLife;
                // The pillar is an instant strike visual, so it builds over
                // 3 ticks instead of the full envelope build, but it no
                // longer appears at full alpha on frame one either.
                float pillarBuild = FxEnvelope.smoothstep(Math.min(1.0f, age / 3.0f));
                float pillarAlpha = (1.0f - pillarProgress) * (isFinal ? 1.0f : 0.85f) * pillarBuild;
                float pillarHeight = (isFinal ? 8.5f : 5.5f) * Math.min(1.0f, age * 0.4f);
                float pillarRadius = (isFinal ? 0.85f : 0.55f) * (1.0f + pillarProgress * 0.3f);

                // The column in two plume layers: crimson sheath, pale
                // core, same heights, radii, and alpha curve as the old
                // tapered cone pair.
                BespokeGeometry.plume(pose, buffer, seed ^ 0x9101L,
                    pillarRadius, pillarHeight, 0.85f, 0.0f, 0.15f, pillarAlpha * 0.65f, 1.0f, ds);
                BespokeGeometry.plume(pose, buffer, seed ^ 0x9102L,
                    pillarRadius * 0.38f, pillarHeight * 1.04f, 1.0f, 0.85f, 0.90f, pillarAlpha * 0.95f, 1.0f, ds);
            }
        });
    }
}
