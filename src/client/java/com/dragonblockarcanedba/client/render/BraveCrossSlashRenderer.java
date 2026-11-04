package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.BraveCrossSlashEntity;
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
 * Entity Renderer for Brave Finisher Cross Slash in Minecraft 26.2.
 * Complex pass: a layered composition. The pieces layer is the cross
 * body itself: two crossed ComplexGeometry tendrils along the diagonal
 * swing paths, bowed toward the facing direction, on the dissolve
 * shader. The complex particle layer is SPARK_STREAK sprites shed along
 * the blades and burst at the strike. The material filler stays as
 * before: valor plasma and soul light shed along the two diagonal swing
 * paths and through the effect volume, emitted through MaterialEmitters
 * in the Brave gold theme, with a plasma burst in the strike window.
 * Emission and geometry follow the same draw-out, envelope, and strike
 * timing the old cross used over its real 40 tick life.
 */
public class BraveCrossSlashRenderer extends EntityRenderer<BraveCrossSlashEntity, BraveCrossSlashRenderer.CrossSlashRenderState> {

    public BraveCrossSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class CrossSlashRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public int maxLifetime = 40;
        public long seed = 0;
        public MaterialEmitters.Emitter plasmaEmitter;
        public MaterialEmitters.Emitter coreEmitter;
        public MaterialEmitters.Emitter soulEmitter;
        public MaterialEmitters.Emitter vaporEmitter;
        public MaterialEmitters.Emitter sparkEmitter;
        public long emitterSeed = Long.MIN_VALUE;
        public ClientLevel emitterLevel;
        public float lastAge = -1f;
        public boolean burstFired = false;
    }

    private record Basis(Vec3 forward, Vec3 right, Vec3 up) {}

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(BraveCrossSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(BraveCrossSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public CrossSlashRenderState createRenderState() {
        return new CrossSlashRenderState();
    }

    @Override
    public void extractRenderState(BraveCrossSlashEntity entity, CrossSlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getSlashScale();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();
    }

    @Override
    public void submit(CrossSlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float life = (float) state.maxLifetime;
        float progress = Math.min(1.0f, state.age / life);
        if (progress >= 1.0f) return;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        float drawOut = FxEnvelope.smoothstep(Math.min(1.0f, progress / 0.2f));
        float fade = FxEnvelope.envelope(state.age, life, 0.2f, 0.3f);
        if (fade > 0.01f) {
            float dist = MaterialEmitters.distanceScale(new Vec3(state.x, state.y, state.z));
            ensureEmitters(state, level);
            float delta = frameDelta(state);
            float e = fade * drawOut * dist;
            state.plasmaEmitter.tick(delta, e);
            state.coreEmitter.tick(delta, e * 0.80f);
            state.soulEmitter.tick(delta, e * 0.75f);
            state.vaporEmitter.tick(delta, e * 0.60f);
            state.sparkEmitter.tick(delta, e * 0.90f);

            // Pieces layer: the two crossed tendrils are the cross body.
            // Local frame axes match the world basis the emission samples,
            // so the strands sit exactly on the diagonal swing paths, bowed
            // forward by the same shallow offset the blade samples use.
            final Basis gb = basis(state);
            final float gspan = span(state);
            final Vec3 diagA = gb.right().add(gb.up()).normalize();
            final Vec3 diagB = gb.right().subtract(gb.up()).normalize();
            final Vec3 ctrl = gb.forward().scale(0.35);
            final Vec3 aFrom = diagA.scale(-gspan * 0.66);
            final Vec3 aTo = diagA.scale(gspan * 0.66);
            final Vec3 bFrom = diagB.scale(-gspan * 0.66);
            final Vec3 bTo = diagB.scale(gspan * 0.66);
            final float width = 0.30f * state.scale;
            final float genv = fade * drawOut;
            final float geoDist = FxEnvelope.distScale(Math.sqrt(Math.max(0.0, state.distanceToCameraSq)));
            final long gseed = state.seed;
            ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
                ComplexGeometry.tendril(pose, buffer, gseed, aFrom, ctrl, aTo, width,
                    0xFFD700, 0xFFFFFF, 0.95f, genv, geoDist);
                ComplexGeometry.tendril(pose, buffer, gseed ^ 0x51ED2701L, bFrom, ctrl, bTo, width,
                    0xFFD700, 0x00F2FF, 0.95f, genv, geoDist);
            });
        }

        // Strike burst at the center during the same 0.28 to 0.62 window
        // the old starburst pulsed in.
        float impact = strike(progress);
        if (impact > 0.02f && !state.burstFired) {
            state.burstFired = true;
            int seed = (int) (state.seed ^ (state.seed >>> 32));
            Vec3 center = new Vec3(state.x, state.y, state.z);
            float s = state.scale;
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.plasma(0xFFD700, 3.0f, seed ^ 0x27D4EB2F, Vec3.ZERO, 0.30f),
                MaterialEmitters.point(center), Math.round(26 * impact * s));
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.plasma(0xFFFFFF, 2.0f, seed ^ 0x165667B1, Vec3.ZERO, 0.22f),
                MaterialEmitters.point(center), Math.round(14 * impact * s));
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.soul(0x00F2FF, 2.6f, seed ^ 0x85EBCA6B, Vec3.ZERO, 0.18f)
                    .withLifetimeFactor(0.5f),
                MaterialEmitters.point(center), Math.round(10 * impact * s));
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.28f),
                MaterialEmitters.point(center), Math.min(30, Math.round(12 * impact * s)));
        } else if (impact <= 0.02f) {
            state.burstFired = false;
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    private static void ensureEmitters(CrossSlashRenderState state, ClientLevel level) {
        if (state.plasmaEmitter != null && state.emitterSeed == state.seed && state.emitterLevel == level) {
            return;
        }
        int seed = (int) (state.seed ^ (state.seed >>> 32));

        // Plasma shed along the two diagonal swing paths of the finisher,
        // sampled across the same growing span the old blades drew.
        MaterialEmitters.Source blades = random -> {
            Basis b = basis(state);
            float span = span(state);
            Vec3 diag = random.nextBoolean()
                ? b.right().add(b.up()).normalize()
                : b.right().subtract(b.up()).normalize();
            double t = (random.nextDouble() * 2.0 - 1.0) * span * 0.66;
            Vec3 center = new Vec3(state.x, state.y, state.z);
            Vec3 pos = center.add(diag.scale(t))
                .add(b.forward().scale((random.nextDouble() - 0.5) * 0.35));
            Vec3 vel = b.forward().scale(0.02).add(diag.scale(t * 0.008));
            return new MaterialEmitters.Sample(pos, vel);
        };
        // Soul light and vapor drifting through the whole effect volume.
        MaterialEmitters.Source volume = random -> {
            float span = span(state);
            Vec3 center = new Vec3(state.x, state.y, state.z);
            return MaterialEmitters.sphereVolume(center, Math.max(0.3, span * 0.34)).sample(random);
        };

        state.plasmaEmitter = new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0xFFD700, 3.0f, seed, Vec3.ZERO, 0.05f),
            blades, 160f, 280);
        state.coreEmitter = new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0xFFFFFF, 2.0f, seed ^ 0x2B992DDF, Vec3.ZERO, 0.04f),
            blades, 85f, 150);
        // Merge wiring: soul (34 to 70 tick profile) and vapor (24 to 54)
        // are factored toward the finisher's short host window (soul 0.5,
        // vapor 0.6) so the cross slash does not shed an afterglow that
        // outlives the entity by seconds. Plasma dies inside the window
        // and is untouched.
        state.soulEmitter = new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.soul(0x00F2FF, 2.6f, seed ^ 0x51ED2701, Vec3.ZERO, 0.02f)
                .withLifetimeFactor(0.5f),
            volume, 55f, 110);
        state.vaporEmitter = new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.vapor(0xFFF2C0, 2.4f, seed ^ 0x85EBCA6B, new Vec3(0, 0.01, 0), 0.02f)
                .withLifetimeFactor(0.6f),
            volume, 32f, 70);
        // Complex particle layer: spark streaks shed along the same blade
        // paths as the plasma, at the standard continuous rate cap.
        state.sparkEmitter = new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.05f),
            blades, 30f, 44);
        state.emitterSeed = state.seed;
        state.emitterLevel = level;
    }

    private static float span(CrossSlashRenderState state) {
        float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
        float drawOut = FxEnvelope.smoothstep(Math.min(1.0f, progress / 0.2f));
        return 4.6f * state.scale * drawOut;
    }

    private static Basis basis(CrossSlashRenderState state) {
        Vec3 forward = Vec3.directionFromRotation(state.xRot, state.yRot);
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(forward).normalize();
        return new Basis(forward, right, up);
    }

    private static float frameDelta(CrossSlashRenderState state) {
        float prev = state.lastAge;
        state.lastAge = state.age;
        if (prev < 0f) return 1f / 20f;
        float d = state.age - prev;
        if (d <= 0f || d > 3f) return 1f / 20f;
        return d / 20f;
    }

    private static float strike(float progress) {
        if (progress <= 0.28f || progress >= 0.62f) return 0.0f;
        return (float) Math.sin(((progress - 0.28f) / 0.34f) * Math.PI);
    }
}
