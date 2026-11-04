package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.GrandCrescentWaveEntity;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Entity Renderer for Grand Crescent Wave in Minecraft 26.2.
 * No-shapes conversion: this renderer draws no geometry. The wave is a
 * moving front of valor plasma sampled across its real travel volume,
 * with pale vapor trailing behind it, steel sparks inside the front, and
 * a brighter plasma surge during the opening strike window. Its life,
 * hit fade, damage, and travel behavior are unchanged; only the
 * presentation moved to material emission.
 */
public class GrandCrescentWaveRenderer extends EntityRenderer<GrandCrescentWaveEntity, GrandCrescentWaveRenderer.GrandCrescentWaveRenderState> {

    private final Map<String, Stream> streams = new HashMap<>();

    public GrandCrescentWaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class GrandCrescentWaveRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public float ageInTicks = 0;
        public int hitTick = -1;
        public long seed = 0;
        public int entityId = -1;
        public boolean isFirstPersonOwner = false;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(GrandCrescentWaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(GrandCrescentWaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public GrandCrescentWaveRenderState createRenderState() {
        return new GrandCrescentWaveRenderState();
    }

    @Override
    public void extractRenderState(GrandCrescentWaveEntity entity, GrandCrescentWaveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.ageInTicks = entity.tickCount + partialTicks;
        state.hitTick = entity.getHitTick();
        state.seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();
        state.entityId = entity.getId();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null &&
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) &&
            mc.options.getCameraType().isFirstPerson());
    }

    @Override
    public void submit(GrandCrescentWaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float maxLife = GrandCrescentWaveEntity.MAX_LIFE_TICKS;
        float progress = Math.min(1.0f, state.ageInTicks / maxLife);
        float fade = com.dragonblockarcanedba.util.FxEnvelope.envelope(
            state.ageInTicks, maxLife, 0.04f, 0.35f);
        if (state.hitTick >= 0) {
            float hitFade = 1.0f - smooth(Math.min(1.0f,
                (state.ageInTicks - state.hitTick) / (float) GrandCrescentWaveEntity.DISSIPATE_TICKS));
            fade = Math.min(fade, hitFade);
        }
        float open = smooth(Math.min(1.0f, state.ageInTicks / 12.0f));
        float impact = strike(progress);

        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && fade > 0.01f) {
            float fpScale = (state.isFirstPersonOwner && state.ageInTicks < 6.0f)
                ? (0.55f + (state.ageInTicks / 6.0f) * 0.45f)
                : 1.0f;
            final float span = 4.8f * fpScale * (0.80f + 0.20f * open);
            final float chord = 1.55f * fpScale * (0.80f + 0.20f * open);
            final Vec3 center = new Vec3(state.x, state.y, state.z);
            float distScale = MaterialEmitters.distanceScale(center);

            double yaw = Math.toRadians(state.yRot);
            double pitch = Math.toRadians(state.xRot);
            final Vec3 forward = new Vec3(
                -Math.sin(yaw) * Math.cos(pitch),
                -Math.sin(pitch),
                Math.cos(yaw) * Math.cos(pitch)).normalize();
            final Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
            final Vec3 up = right.cross(forward).normalize();

            MaterialEmitters.Source frontSource = random -> sampleFront(random, center, right, up, forward, span, chord, 0.0);
            MaterialEmitters.Source trailSource = random -> sampleFront(random, center, right, up, forward, span, chord, -1.05);
            String base = state.entityId + ":";
            int seed = (int) state.seed;

            emit(level, base + "plasma",
                MaterialEmitters.Preset.plasma(0xFFD833, 1.65f, seed,
                    forward.scale(0.035), 0.035f),
                frontSource, 430f, 520, state.ageInTicks, fade * distScale);
            emit(level, base + "vapor",
                MaterialEmitters.Preset.vapor(0xFFE9A8, 3.5f, seed ^ 0x9A90,
                    forward.scale(-0.020), 0.025f),
                trailSource, 96f, 136, state.ageInTicks, fade * distScale * 0.74f);
            emit(level, base + "sparks",
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK, forward.scale(0.05), 0.10f),
                frontSource, 118f, 154, state.ageInTicks, fade * distScale * 0.90f);
            if (impact > 0.02f) {
                emit(level, base + "strike",
                    MaterialEmitters.Preset.plasma(0xFFFFFF, 2.05f, seed ^ 0x57A1E,
                        forward.scale(0.06), 0.06f),
                    frontSource, 265f, 300, state.ageInTicks, impact * distScale);
            }
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    private static MaterialEmitters.Sample sampleFront(RandomSource random, Vec3 center, Vec3 right,
                                                       Vec3 up, Vec3 forward, float span, float chord,
                                                       double backOffset) {
        double u = (random.nextDouble() + random.nextDouble()) - 1.0;
        double v = (random.nextDouble() + random.nextDouble()) - 1.0;
        double depth = (random.nextDouble() - 0.5) * 0.55 + backOffset;
        Vec3 pos = center
            .add(right.scale(u * span * 0.52))
            .add(up.scale(v * chord * 0.64))
            .add(forward.scale(depth));
        return new MaterialEmitters.Sample(pos, forward.scale(0.04));
    }

    private static float smooth(float t) {
        float x = Math.max(0.0f, Math.min(1.0f, t));
        return x * x * (3.0f - 2.0f * x);
    }

    private static float strike(float progress) {
        if (progress >= 0.16f) return 0.0f;
        return (float) Math.sin((progress / 0.16f) * Math.PI);
    }

    private void emit(ClientLevel level, String key, MaterialEmitters.Preset preset,
                      MaterialEmitters.Source source, float ratePerSecond, int budgetPerSecond,
                      float age, float envelope) {
        if (envelope <= 0.01f) return;
        long now = System.nanoTime();
        streams.entrySet().removeIf(entry -> now - entry.getValue().lastSeenNanos > 30_000_000_000L);
        Stream stream = streams.computeIfAbsent(key, ignored -> new Stream());
        if (stream.emitter == null || stream.level != level) {
            stream.emitter = new MaterialEmitters.Emitter(level, preset, stream.source, ratePerSecond, budgetPerSecond);
            stream.level = level;
            stream.lastAge = Float.NaN;
        }
        stream.source.delegate = source;
        float delta = Float.isNaN(stream.lastAge) || age < stream.lastAge
            ? 1.0f / 60.0f
            : Math.min(0.10f, Math.max(0.0f, (age - stream.lastAge) / 20.0f));
        stream.lastAge = age;
        stream.lastSeenNanos = now;
        stream.emitter.tick(delta, envelope);
    }

    private static final class Stream {
        private final MutableSource source = new MutableSource();
        private MaterialEmitters.Emitter emitter;
        private ClientLevel level;
        private float lastAge = Float.NaN;
        private long lastSeenNanos;
    }

    private static final class MutableSource implements MaterialEmitters.Source {
        private MaterialEmitters.Source delegate = MaterialEmitters.point(Vec3.ZERO);

        @Override
        public MaterialEmitters.Sample sample(RandomSource random) {
            return delegate.sample(random);
        }
    }
}
