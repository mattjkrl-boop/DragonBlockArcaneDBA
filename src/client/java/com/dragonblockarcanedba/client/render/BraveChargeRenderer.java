package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.BraveChargeEntity;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

/**
 * Entity Renderer for Brave Sword Heroic Focus Charge-up in Minecraft 26.2.
 * Complex pass: a layered composition. The pieces layer is a fan of valor
 * tendrils lashing from the caster core through ComplexGeometry on the
 * dissolve shader, widening as the charge ratio climbs. The complex
 * particle layer is WISP_CURL sprites streaming inward with the gather.
 * The material filler stays as before: valor plasma and soul light motes
 * gathered inward toward the caster through MaterialEmitters, with vapor
 * drifting at the core and gold dust stirred across the ground. Emission
 * and geometry follow the same charge ratio, spawn fade-in, and synced
 * fade factor the old construct used.
 */
public class BraveChargeRenderer extends EntityRenderer<BraveChargeEntity, BraveChargeRenderer.ChargeRenderState> {

    public BraveChargeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ChargeRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public float age = 0;
        public float fadeFactor = 1.0f;
        public long seed = 0;
        public MaterialEmitters.Emitter plasmaEmitter;
        public MaterialEmitters.Emitter soulEmitter;
        public MaterialEmitters.Emitter vaporEmitter;
        public MaterialEmitters.Emitter dustEmitter;
        public MaterialEmitters.Emitter wispEmitter;
        public long emitterSeed = Long.MIN_VALUE;
        public ClientLevel emitterLevel;
        public float lastAge = -1f;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(BraveChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(BraveChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ChargeRenderState createRenderState() {
        return new ChargeRenderState();
    }

    @Override
    public void extractRenderState(BraveChargeEntity entity, ChargeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.age = entity.tickCount + partialTicks;
        state.fadeFactor = entity.getFadeFactor();
        state.seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();
    }

    @Override
    public void submit(ChargeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        // Same lifecycle as the old construct: spawn fade-in over the first
        // 4 ticks, brightness following the charge ratio, and the entity's
        // synced fade factor taking it back out over 10 ticks.
        float env = (0.65f + state.chargeRatio * 0.35f)
            * Math.min(1.0f, state.age / 4.0f) * state.fadeFactor;
        if (env > 0.01f) {
            float dist = MaterialEmitters.distanceScale(new Vec3(state.x, state.y, state.z));
            ensureEmitters(state, level);
            float delta = frameDelta(state);
            float e = env * dist;
            state.plasmaEmitter.tick(delta, e);
            state.soulEmitter.tick(delta, e * 0.85f);
            state.vaporEmitter.tick(delta, e * 0.70f);
            state.dustEmitter.tick(delta, e * 0.80f);
            state.wispEmitter.tick(delta, e * 0.90f);

            // Pieces layer: a fan of five valor tendrils lashing out of the
            // core at the caster chest, in the local frame of the entity.
            // The fan reach follows the same gather radius the motes stream
            // in from, so the strands grow with the charge ratio.
            final float geoDist = FxEnvelope.distScale(Math.sqrt(Math.max(0.0, state.distanceToCameraSq)));
            final float fanLen = 1.15f + state.chargeRatio * 1.55f;
            final long gseed = state.seed;
            final float genv = env;
            ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) ->
                ComplexGeometry.tendrilFan(pose, buffer, gseed, new Vec3(0, 0.9, 0),
                    new Vec3(0, 1, 0), 118f, fanLen, 5, 0.17f,
                    0xFFD700, 0xFFF2C0, 0.92f, genv, geoDist));
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    private static void ensureEmitters(ChargeRenderState state, ClientLevel level) {
        if (state.plasmaEmitter != null && state.emitterSeed == state.seed && state.emitterLevel == level) {
            return;
        }
        int seed = (int) (state.seed ^ (state.seed >>> 32));

        // Motes start out at the old construct's radius and stream inward
        // toward the core point at the caster's chest, faster as the
        // sampled velocity carries them the whole way in one motion.
        MaterialEmitters.Source gathering = random -> {
            Vec3 center = new Vec3(state.x, state.y, state.z);
            Vec3 core = center.add(0, 0.9, 0);
            float baseRadius = 1.3f + state.chargeRatio * 1.5f;
            double ang = random.nextDouble() * Math.PI * 2.0;
            double r = baseRadius * (0.45 + random.nextDouble() * 0.85);
            Vec3 pos = new Vec3(center.x + Math.cos(ang) * r,
                center.y + random.nextDouble() * 1.9,
                center.z + Math.sin(ang) * r);
            return new MaterialEmitters.Sample(pos, core.subtract(pos).scale(0.14));
        };
        MaterialEmitters.Source coreVolume = random -> {
            Vec3 core = new Vec3(state.x, state.y + 0.9, state.z);
            return MaterialEmitters.sphereVolume(core, 0.55 + state.chargeRatio * 0.35).sample(random);
        };
        MaterialEmitters.Source ground = random -> {
            Vec3 center = new Vec3(state.x, state.y, state.z);
            float baseRadius = 1.3f + state.chargeRatio * 1.5f;
            return MaterialEmitters.groundArea(center, baseRadius * 1.2, center.y + 0.05).sample(random);
        };

        state.plasmaEmitter = new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0xFFD700, 2.3f, seed, Vec3.ZERO, 0.015f),
            gathering, 175f, 280);
        state.soulEmitter = new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.soul(0x00F2FF, 2.5f, seed ^ 0x2B992DDF, Vec3.ZERO, 0.02f),
            gathering, 65f, 120);
        state.vaporEmitter = new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.vapor(0xFFF2C0, 2.1f, seed ^ 0x51ED2701, new Vec3(0, 0.02, 0), 0.02f),
            coreVolume, 40f, 80);
        state.dustEmitter = new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.dust(0xFFD700, 1.9f, seed ^ 0x85EBCA6B, new Vec3(0, 0.012, 0), 0.012f),
            ground, 30f, 60);
        // Complex particle layer: wisp curls riding the same inward gather
        // stream as the plasma motes, at the standard continuous rate cap.
        state.wispEmitter = new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.fixed(DbaParticles.WISP_CURL, Vec3.ZERO, 0.02f),
            gathering, 26f, 40);
        state.emitterSeed = state.seed;
        state.emitterLevel = level;
    }

    private static float frameDelta(ChargeRenderState state) {
        float prev = state.lastAge;
        state.lastAge = state.age;
        if (prev < 0f) return 1f / 20f;
        float d = state.age - prev;
        if (d <= 0f || d > 3f) return 1f / 20f;
        return d / 20f;
    }
}
