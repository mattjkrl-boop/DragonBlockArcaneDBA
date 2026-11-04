package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.GrandClashSparkEntity;
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
 * Entity Renderer for Grand Clash Spark in Minecraft 26.2.
 * No-shapes conversion: this renderer draws no geometry. A projectile
 * deflection reads as a short material burst at the clash point: dense
 * white-gold valor plasma, steel sparks thrown outward, pale vapor, and a
 * little stone dust. The 24 tick life and the shared lifecycle envelope
 * are unchanged; only the presentation moved to material emission.
 */
public class GrandClashSparkRenderer extends EntityRenderer<GrandClashSparkEntity, GrandClashSparkRenderer.ClashRenderState> {

    private final Map<String, Stream> streams = new HashMap<>();

    public GrandClashSparkRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ClashRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public float age = 0;
        public int maxLifetime = 10;
        public long seed = 0;
        public int entityId = -1;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(GrandClashSparkEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(GrandClashSparkEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ClashRenderState createRenderState() {
        return new ClashRenderState();
    }

    @Override
    public void extractRenderState(GrandClashSparkEntity entity, ClashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getClashScale();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
        state.entityId = entity.getId();
    }

    @Override
    public void submit(ClashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float fade = com.dragonblockarcanedba.util.FxEnvelope.envelope(
            state.age, state.maxLifetime, 0.20f, 0.45f);
        if (fade <= 0.01f) return;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        float grow = com.dragonblockarcanedba.util.FxEnvelope.smoothstep(
            Math.min(1.0f, state.age / (state.maxLifetime * 0.20f)));
        float scale = state.scale * (0.55f + grow * 1.25f);
        Vec3 center = new Vec3(state.x, state.y, state.z);
        float envelope = fade * MaterialEmitters.distanceScale(center);
        int seed = (int) state.seed;
        String base = state.entityId + ":";

        emit(level, base + "plasma",
            MaterialEmitters.Preset.plasma(0xFFF3B0, 1.85f * state.scale, seed,
                Vec3.ZERO, 0.20f),
            MaterialEmitters.sphereVolume(center, 0.48 * scale),
            285f, 340, state.age, envelope);
        emit(level, base + "sparks",
            MaterialEmitters.Preset.fixed(DbaParticles.SPARK, Vec3.ZERO, 0.34f),
            MaterialEmitters.sphereVolume(center, 0.30 * scale),
            195f, 240, state.age, envelope);
        emit(level, base + "vapor",
            MaterialEmitters.Preset.vapor(0xFFE9A8, 3.4f * state.scale, seed ^ 0x9A90,
                new Vec3(0.0, 0.025, 0.0), 0.055f),
            MaterialEmitters.sphereVolume(center, 0.82 * scale),
            62f, 88, state.age, envelope * 0.78f);
        emit(level, base + "dust",
            MaterialEmitters.Preset.dust(0x8A7D68, 1.20f, seed ^ 0xD057,
                Vec3.ZERO, 0.11f),
            MaterialEmitters.sphereVolume(center, 0.62 * scale),
            54f, 76, state.age, envelope * 0.82f);

        super.submit(state, poseStack, collector, cameraState);
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
