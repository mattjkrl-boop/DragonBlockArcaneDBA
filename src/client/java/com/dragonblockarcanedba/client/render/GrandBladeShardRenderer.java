package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.GrandBladeShardEntity;
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
 * Entity Renderer for Grand Blade Shard in Minecraft 26.2.
 * Attack upgrade pass (complex redo base kept): layered composition.
 * The shard body is an
 * irregular ComplexGeometry shard mesh in a steel gradient (submitSolidLit:
 * the shard is physical steel, so the world lights it, not the effect),
 * with two braided torn tendril wakes behind it in flight (submitEnergy,
 * tendril canvas; the redo's single wake strand doubled, the second
 * strand slimmer, counter-swayed, and paler). Around the pieces, the
 * existing material emission stays as
 * filler: valor plasma, steel sparks, and stone dust along the flight
 * path, and ground dust, rising plasma, and vapor for the embedded
 * hazard. The complex sprite layer adds SPARK_STREAK and SHARD_CHIP shed
 * in flight, and a one-shot streak and chip burst at the embedding
 * moment, which is the shard's shatter-into-ground beat. The entity, its
 * damage, and its flight and embedded lifetimes are unchanged.
 */
public class GrandBladeShardRenderer extends EntityRenderer<GrandBladeShardEntity, GrandBladeShardRenderer.BladeShardRenderState> {

    private final Map<String, Stream> streams = new HashMap<>();
    private final java.util.Set<Integer> embedBurstsDone = new java.util.HashSet<>();

    public GrandBladeShardRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class BladeShardRenderState extends EntityRenderState {
        public boolean isEmbedded = false;
        public float yRot = 0;
        public float xRot = 0;
        public float ageInTicks = 0;
        public long seed = 0;
        public int entityId = -1;
        public Vec3 velocity = Vec3.ZERO;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(GrandBladeShardEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(GrandBladeShardEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public BladeShardRenderState createRenderState() {
        return new BladeShardRenderState();
    }

    @Override
    public void extractRenderState(GrandBladeShardEntity entity, BladeShardRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.isEmbedded = entity.isEmbedded();
        state.yRot = entity.isEmbedded() ? entity.getEmbeddedYaw() : entity.getYRot();
        state.xRot = entity.isEmbedded() ? entity.getEmbeddedPitch() : entity.getXRot();
        state.ageInTicks = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();
        state.entityId = entity.getId();
        state.velocity = entity.getDeltaMovement();
    }

    @Override
    public void submit(BladeShardRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            Vec3 center = new Vec3(state.x, state.y, state.z);
            float distScale = MaterialEmitters.distanceScale(center);
            int seed = (int) state.seed;

            if (state.isEmbedded) {
                float fadeOut = 1.0f;
                float fadeStart = GrandBladeShardEntity.MAX_EMBEDDED_TICKS - 30.0f;
                if (state.ageInTicks > fadeStart) {
                    fadeOut = 1.0f - com.dragonblockarcanedba.util.FxEnvelope.smoothstep(
                        (state.ageInTicks - fadeStart) / 30.0f);
                }
                float pulse = (0.85f + 0.15f * (float) Math.sin(state.ageInTicks * 0.25f)) * fadeOut;
                float envelope = pulse * distScale;
                String base = state.entityId + ":embedded:";

                emit(level, base + "dust",
                    MaterialEmitters.Preset.dust(0x8A7D68, 1.15f, seed ^ 0xD057,
                        new Vec3(0.0, 0.012, 0.0), 0.018f),
                    MaterialEmitters.groundArea(center, 0.95, center.y + 0.06),
                    30f, 44, state.ageInTicks, envelope);
                emit(level, base + "plasma",
                    MaterialEmitters.Preset.plasma(0xFFD833, 1.45f, seed,
                        new Vec3(0.0, 0.035, 0.0), 0.030f),
                    MaterialEmitters.sphereVolume(center.add(0.0, 0.55, 0.0), 0.72),
                    62f, 84, state.ageInTicks, envelope);
                emit(level, base + "vapor",
                    MaterialEmitters.Preset.vapor(0xFFE9A8, 3.1f, seed ^ 0x9A90,
                        new Vec3(0.0, 0.018, 0.0), 0.010f),
                    MaterialEmitters.sphereVolume(center.add(0.0, 0.45, 0.0), 0.92),
                    17f, 28, state.ageInTicks, envelope * 0.72f);

                // Pieces layer, embedded: the same steel shard mesh, set back
                // along its flight line so it reads as driven into the ground,
                // plus one short valor tendril leaking up from its base.
                Vec3 embedDir = directionFromRot(state.yRot, state.xRot);
                float embedLife = pulse;
                if (embedLife > 0.01f) {
                    ComplexGeometry.submitSolidLit(collector, poseStack, state.lightCoords, (pose, buffer) ->
                        ComplexGeometry.shard(pose, buffer, state.seed ^ 0x5AA2DL,
                            embedDir.scale(-0.60), embedDir, 1.7f, 0.30f,
                            0x767F8C, 0xE9EDF3, 0.95f, embedLife));
                    ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) ->
                        ComplexGeometry.tendril(pose, buffer, state.seed ^ 0x7EAD1L,
                            new Vec3(0.0, 0.05, 0.0),
                            new Vec3(0.22, 0.55, 0.10),
                            new Vec3(-0.10, 1.05, -0.16), 0.20f,
                            0xFFD833, 0xFFF3B0, 0.88f, embedLife, distScale));
                }

                // Complex sprite layer: the embedding moment is the shatter
                // beat. One guarded burst per entity (tickCount resets to 0
                // on embedding, so a low age here is that first moment).
                if (state.ageInTicks < 3.0f && embedBurstsDone.add(state.entityId)) {
                    if (embedBurstsDone.size() > 512) embedBurstsDone.clear();
                    MaterialEmitters.burst(level,
                        MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.30f),
                        MaterialEmitters.sphereVolume(center, 0.35), 16);
                    MaterialEmitters.burst(level,
                        MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP, Vec3.ZERO, 0.22f),
                        MaterialEmitters.sphereVolume(center, 0.30), 10);
                }
            } else {
                float buildIn = com.dragonblockarcanedba.util.FxEnvelope.smoothstep(state.ageInTicks / 3.0f);
                float fadeOut = 1.0f;
                float flightFadeStart = GrandBladeShardEntity.MAX_FLIGHT_TICKS - 10.0f;
                if (state.ageInTicks > flightFadeStart) {
                    fadeOut = 1.0f - com.dragonblockarcanedba.util.FxEnvelope.smoothstep(
                        (state.ageInTicks - flightFadeStart) / 10.0f);
                }
                float envelope = buildIn * fadeOut * distScale;
                String base = state.entityId + ":flying:";
                Vec3 trailVelocity = state.velocity.scale(-0.18);
                MaterialEmitters.Source coreSource = random -> new MaterialEmitters.Sample(center, trailVelocity);
                MaterialEmitters.Source cloudSource = MaterialEmitters.sphereVolume(center, 0.30);

                emit(level, base + "plasma",
                    MaterialEmitters.Preset.plasma(0xFFD833, 1.55f, seed,
                        Vec3.ZERO, 0.045f),
                    coreSource, 125f, 165, state.ageInTicks, envelope);
                emit(level, base + "sparks",
                    MaterialEmitters.Preset.fixed(DbaParticles.SPARK, Vec3.ZERO, 0.16f),
                    cloudSource, 58f, 82, state.ageInTicks, envelope);
                emit(level, base + "dust",
                    MaterialEmitters.Preset.dust(0x8A7D68, 1.05f, seed ^ 0x570E,
                        Vec3.ZERO, 0.050f),
                    cloudSource, 42f, 64, state.ageInTicks, envelope * 0.85f);

                // Complex sprite layer in flight: spark streaks streaming
                // back along the path and a sparse shed of shard chips.
                emit(level, base + "streak",
                    MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, trailVelocity, 0.10f),
                    cloudSource, 28f, 40, state.ageInTicks, envelope);
                emit(level, base + "chip",
                    MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP, trailVelocity, 0.08f),
                    cloudSource, 11f, 40, state.ageInTicks, envelope * 0.85f);

                // Pieces layer in flight: steel shard mesh as the body,
                // aligned to the real velocity (rotation as fallback), and
                // a torn gold tendril wake streaming behind it.
                float flightLife = buildIn * fadeOut;
                if (flightLife > 0.01f) {
                    Vec3 dir = state.velocity.lengthSqr() > 1.0e-4
                        ? state.velocity.normalize()
                        : directionFromRot(state.yRot, state.xRot);
                    Vec3 side = dir.cross(new Vec3(0.0, 1.0, 0.0));
                    side = side.lengthSqr() < 1.0e-6 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
                    double sway = Math.sin(state.ageInTicks * 0.55f) * 0.16;
                    final Vec3 braidSide = side;
                    Vec3 wakeFrom = dir.scale(-0.80);
                    Vec3 wakeControl = dir.scale(-1.55).add(side.scale(0.28 + sway));
                    Vec3 wakeTo = dir.scale(-2.35).add(side.scale(-0.18 + sway * 0.5));
                    ComplexGeometry.submitSolidLit(collector, poseStack, state.lightCoords, (pose, buffer) ->
                        ComplexGeometry.shard(pose, buffer, state.seed ^ 0x5AA2DL,
                            Vec3.ZERO, dir, 1.7f, 0.30f,
                            0x767F8C, 0xE9EDF3, 0.95f, flightLife));
                    ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) ->
                        ComplexGeometry.tendril(pose, buffer, state.seed ^ 0x7EAD1L,
                            wakeFrom, wakeControl, wakeTo, 0.34f,
                            0xFFD833, 0xFFF3B0, 0.90f, flightLife, distScale));
                    // Second wake strand: slimmer and counter-swayed, so
                    // the two strands braid around the flight line instead
                    // of reading as one ribbon with an echo.
                    ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) ->
                        ComplexGeometry.tendril(pose, buffer, state.seed ^ 0x7EAD2L,
                            wakeFrom.add(braidSide.scale(0.16)),
                            dir.scale(-1.50).add(braidSide.scale(-0.30 - sway)),
                            dir.scale(-2.25).add(braidSide.scale(0.20 - sway * 0.5)), 0.19f,
                            0xFFD833, 0xFFE9A8, 0.80f, flightLife, distScale * 0.7f));
                }
            }
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    private static Vec3 directionFromRot(float yRot, float xRot) {
        double yaw = Math.toRadians(yRot);
        double pitch = Math.toRadians(xRot);
        return new Vec3(
            -Math.sin(yaw) * Math.cos(pitch),
            -Math.sin(pitch),
            Math.cos(yaw) * Math.cos(pitch)).normalize();
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
