package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.BraveSlashEntity;
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
 * Entity Renderer for Brave Slash in MC 26.2.
 * No-shapes pass: the renderer draws no geometry at all. The cut reads as
 * valor plasma shed across the swing width and along the real flight path,
 * emitted through MaterialEmitters in the Brave gold theme with a white
 * core stream and cyan vapor in the wake. Emission follows the same
 * lifecycle envelope and strike window the old cut used.
 */
public class BraveSlashRenderer extends EntityRenderer<BraveSlashEntity, BraveSlashRenderer.BraveSlashRenderState> {
    public BraveSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class BraveSlashRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public int maxLifetime = BraveSlashEntity.MAX_LIFE;
        public long seed = 0;
        public MaterialEmitters.Emitter plasmaEmitter;
        public MaterialEmitters.Emitter coreEmitter;
        public MaterialEmitters.Emitter vaporEmitter;
        public long emitterSeed = Long.MIN_VALUE;
        public ClientLevel emitterLevel;
        public float lastAge = -1f;
        public boolean burstFired = false;
    }

    private record Basis(Vec3 forward, Vec3 right, Vec3 up) {}

    @Override
    public BraveSlashRenderState createRenderState() {
        return new BraveSlashRenderState();
    }

    @Override
    public void extractRenderState(BraveSlashEntity entity, BraveSlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();
    }

    @Override
    public void submit(BraveSlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        float life = (float) state.maxLifetime;
        float progress = Math.min(1.0f, state.age / life);
        float fade = FxEnvelope.envelope(state.age, life, 0.2f, 0.35f);
        if (fade > 0.01f) {
            float dist = MaterialEmitters.distanceScale(new Vec3(state.x, state.y, state.z));
            ensureEmitters(state, level);
            float delta = frameDelta(state);
            float e = fade * dist;
            state.plasmaEmitter.tick(delta, e);
            state.coreEmitter.tick(delta, e * 0.80f);
            state.vaporEmitter.tick(delta, e * 0.65f);
        }

        // Strike flash: a one-shot plasma burst at the belly of the cut
        // during the same early strike window the old flash pulsed in.
        float impact = strike(progress);
        if (impact > 0.02f && !state.burstFired) {
            state.burstFired = true;
            int seed = (int) (state.seed ^ (state.seed >>> 32));
            Basis b = basis(state);
            Vec3 belly = new Vec3(state.x, state.y, state.z).add(b.forward().scale(0.6));
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.plasma(0xFFFFFF, 2.0f, seed ^ 0x85EBCA6B, Vec3.ZERO, 0.22f),
                MaterialEmitters.point(belly), Math.round(14 * impact));
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.plasma(0xFFD700, 2.4f, seed ^ 0x27D4EB2F, Vec3.ZERO, 0.26f),
                MaterialEmitters.point(belly), Math.round(10 * impact));
        } else if (impact <= 0.02f) {
            state.burstFired = false;
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    private static void ensureEmitters(BraveSlashRenderState state, ClientLevel level) {
        if (state.plasmaEmitter != null && state.emitterSeed == state.seed && state.emitterLevel == level) {
            return;
        }
        int seed = (int) (state.seed ^ (state.seed >>> 32));

        // Plasma shed across the swing width at the cut's leading belly,
        // sampled across the same opening span the old cut drew.
        MaterialEmitters.Source cut = random -> {
            Basis b = basis(state);
            float span = span(state);
            double t = random.nextDouble() * 2.0 - 1.0;
            Vec3 center = new Vec3(state.x, state.y, state.z);
            Vec3 pos = center.add(b.right().scale(t * span * 0.5))
                .add(b.forward().scale(0.30 - 0.20 * t * t))
                .add(b.up().scale((random.nextDouble() - 0.5) * 0.55));
            return new MaterialEmitters.Sample(pos, b.forward().scale(0.03));
        };
        // Vapor and loose plasma trailing behind the flight line.
        MaterialEmitters.Source wake = random -> {
            Basis b = basis(state);
            float span = span(state);
            double t = random.nextDouble();
            Vec3 center = new Vec3(state.x, state.y, state.z);
            Vec3 pos = center.add(b.forward().scale(-1.5 + t * 1.8))
                .add(b.right().scale((random.nextDouble() - 0.5) * span * 0.72))
                .add(b.up().scale((random.nextDouble() - 0.5) * 0.85));
            return new MaterialEmitters.Sample(pos, b.forward().scale(0.012));
        };

        state.plasmaEmitter = new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0xFFD700, 2.6f, seed, Vec3.ZERO, 0.05f),
            cut, 135f, 240);
        state.coreEmitter = new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0xFFFFFF, 1.8f, seed ^ 0x51ED2701, Vec3.ZERO, 0.04f),
            cut, 75f, 140);
        state.vaporEmitter = new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.vapor(0x00F2FF, 2.3f, seed ^ 0x2B992DDF, Vec3.ZERO, 0.03f),
            wake, 48f, 90);
        state.emitterSeed = state.seed;
        state.emitterLevel = level;
    }

    private static float span(BraveSlashRenderState state) {
        float open = FxEnvelope.smoothstep(Math.min(1.0f, state.age / ((float) state.maxLifetime * 0.2f)));
        return 3.2f * (0.82f + 0.22f * open);
    }

    private static Basis basis(BraveSlashRenderState state) {
        Vec3 forward = Vec3.directionFromRotation(state.xRot, state.yRot);
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(forward).normalize();
        return new Basis(forward, right, up);
    }

    private static float frameDelta(BraveSlashRenderState state) {
        float prev = state.lastAge;
        state.lastAge = state.age;
        if (prev < 0f) return 1f / 20f;
        float d = state.age - prev;
        if (d <= 0f || d > 3f) return 1f / 20f;
        return d / 20f;
    }

    private static float strike(float progress) {
        if (progress >= 0.36f) return 0.0f;
        return (float) Math.sin((progress / 0.36f) * Math.PI);
    }
}
