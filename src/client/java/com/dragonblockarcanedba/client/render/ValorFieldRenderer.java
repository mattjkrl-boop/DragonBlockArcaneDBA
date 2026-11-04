package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.ValorFieldEntity;
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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entity Renderer for Grand Sword's Valor Field in Minecraft 26.2.
 * Complex effects redo: layered composition at class L (two builder
 * types, full budget, shader treatment on the energy pieces). Vertical
 * tendrils lash at the field rim (submitEnergy on the tendril canvas).
 * The slow steel shards that hang suspended in the volume and mark each
 * actually suspended projectile are real FragmentEntity pieces now,
 * spawned and held server side by the field and drawn by the fragment
 * renderer; they are not drawn here. WISP_CURL sprites drift in
 * the volume and SPARK_STREAK bursts fire when a projectile newly
 * suspends. The existing material emission stays as filler: valor
 * plasma motes, boundary vapor, ground dust, and steel spark accents,
 * plus dense plasma motes around suspended projectiles. The field
 * radius, rise, fade out, buffs, pressure, projectile stasis, and the
 * damage attribution exception in the entity are unchanged.
 */
public class ValorFieldRenderer extends EntityRenderer<ValorFieldEntity, ValorFieldRenderer.ValorFieldRenderState> {

    // Fixed theme seeds: a Valor Field always has the same radius, so these
    // keep each material stream's variation stable and client-identical.
    // The dome and ring outer seeds moved server side with the fragment
    // spawns (ValorFieldEntity.SEED_DOME and SEED_RING_OUTER); the plasma
    // stream below still reads the dome seed from there.
    private static final long SEED_RING_INNER = 0xB222L;
    private static final long SEED_RAYS = 0xBA75L;
    private static final long SEED_LIP = 0x1115L;

    private final Map<String, Stream> streams = new HashMap<>();
    private final Map<Integer, Integer> lastSuspendedCounts = new HashMap<>();

    public ValorFieldRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ValorFieldRenderState extends EntityRenderState {
        public float radius = 9.0f;
        public float ageInTicks = 0;
        public float fadeStart = -1;
        public int entityId = -1;
        public final List<Vec3> suspendedRelPositions = new ArrayList<>();
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(ValorFieldEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(ValorFieldEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ValorFieldRenderState createRenderState() {
        return new ValorFieldRenderState();
    }

    @Override
    public void extractRenderState(ValorFieldEntity entity, ValorFieldRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = ValorFieldEntity.FIELD_RADIUS;
        state.ageInTicks = entity.tickCount + partialTicks;
        state.fadeStart = entity.getFadeStart();
        state.entityId = entity.getId();

        state.suspendedRelPositions.clear();
        // Motes only around projectiles the field actually suspended: the ids
        // come from the entity's own suspended map, synced to the client.
        // This used to mark any projectile moving slowly inside the radius,
        // including ones the field never touched.
        java.util.Set<Integer> suspendedIds = entity.getSuspendedIds();
        if (suspendedIds.isEmpty()) return;
        Vec3 fieldPos = entity.position();
        float r = state.radius;
        AABB box = new AABB(
            fieldPos.x - r, fieldPos.y - r, fieldPos.z - r,
            fieldPos.x + r, fieldPos.y + r, fieldPos.z + r
        );

        List<Projectile> projectiles = entity.level().getEntitiesOfClass(
            Projectile.class, box,
            p -> p.isAlive() && p != entity && suspendedIds.contains(p.getId())
                && p.distanceToSqr(fieldPos) <= r * r
        );

        for (Projectile p : projectiles) {
            Vec3 pPos = p.position();
            state.suspendedRelPositions.add(pPos.subtract(fieldPos));
        }
    }

    @Override
    public void submit(ValorFieldRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float radius = state.radius;
        float age = state.ageInTicks;
        // Spawn fade-in over the first 10 ticks, matching the field's rise.
        float spawnIn = Math.min(1.0f, age / 10.0f);
        float rise = smooth(spawnIn);
        // Fade out over FADE_OUT_TICKS once the entity starts dissipating.
        float fadeOut = 1.0f;
        if (state.fadeStart >= 0) {
            fadeOut = 1.0f - smooth(Math.min(1.0f,
                (age - state.fadeStart) / (float) ValorFieldEntity.FADE_OUT_TICKS));
        }
        float pulse = (0.88f + 0.12f * (float) Math.sin(age * 0.18f)) * rise * fadeOut;

        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && pulse > 0.01f) {
            final Vec3 center = new Vec3(state.x, state.y, state.z);
            float distScale = MaterialEmitters.distanceScale(center);
            float envelope = pulse * distScale;
            // Same height profile as the old field volume: it grows out of
            // the ground over the spawn rise instead of snapping to full size.
            final float domeHeight = radius * 1.05f * (0.22f + 0.78f * rise);
            String base = state.entityId + ":";

            emit(level, base + "plasma",
                MaterialEmitters.Preset.plasma(0xFFD833, 1.50f, (int) ValorFieldEntity.SEED_DOME,
                    new Vec3(0.0, 0.030, 0.0), 0.016f),
                MaterialEmitters.sphereVolume(center.add(0.0, domeHeight * 0.45, 0.0), radius * 0.88),
                320f, 400, age, envelope);
            emit(level, base + "boundary",
                MaterialEmitters.Preset.vapor(0xFFE9A8, 4.6f, (int) SEED_LIP,
                    Vec3.ZERO, 0.006f),
                random -> sampleBoundary(random, center, radius, domeHeight),
                70f, 96, age, envelope * 0.82f);
            emit(level, base + "dust",
                MaterialEmitters.Preset.dust(0x8A7D68, 1.30f, (int) SEED_RAYS,
                    new Vec3(0.0, 0.010, 0.0), 0.012f),
                MaterialEmitters.groundArea(center, radius * 0.98, center.y + 0.07),
                95f, 140, age, envelope * 0.86f);
            emit(level, base + "sparks",
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK, Vec3.ZERO, 0.030f),
                MaterialEmitters.sphereVolume(center.add(0.0, 1.0, 0.0), radius * 0.80),
                48f, 72, age, envelope * 0.72f);

            if (!state.suspendedRelPositions.isEmpty()) {
                final List<Vec3> suspended = state.suspendedRelPositions;
                emit(level, base + "stasis",
                    MaterialEmitters.Preset.plasma(0xFFF3B0, 1.40f, (int) SEED_RING_INNER,
                        Vec3.ZERO, 0.020f),
                    random -> sampleSuspended(random, center, suspended),
                    135f, 180, age, envelope);
            }

            // Complex sprite layer: wisp curls drifting through the volume.
            emit(level, base + "wisp",
                MaterialEmitters.Preset.fixed(DbaParticles.WISP_CURL, new Vec3(0.0, 0.020, 0.0), 0.010f),
                MaterialEmitters.sphereVolume(center.add(0.0, domeHeight * 0.45, 0.0), radius * 0.85),
                22f, 40, age, envelope);

            // Complex sprite layer: a spark streak burst each time the
            // suspended count grows, at the newly suspended positions.
            int suspendedCount = state.suspendedRelPositions.size();
            Integer lastCount = lastSuspendedCounts.put(state.entityId, suspendedCount);
            if (lastCount != null && suspendedCount > lastCount) {
                for (int i = lastCount; i < suspendedCount; i++) {
                    Vec3 at = center.add(state.suspendedRelPositions.get(i));
                    MaterialEmitters.burst(level,
                        MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.16f),
                        MaterialEmitters.point(at), 10);
                }
            }
            if (lastSuspendedCounts.size() > 256) lastSuspendedCounts.clear();

            // Pieces layer (class L). One submit holds six vertical rim
            // tendrils that sway as the field ages. The suspended shard
            // meshes that used to share this layer are real FragmentEntity
            // pieces now (spawned server side by the field, drawn by the
            // fragment renderer), so they no longer appear here.
            final float fieldLife = rise * fadeOut;
            final float rimRadius = radius * 0.92f;
            final float rimHeight = domeHeight * 0.88f;
            ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
                int verts = 0;
                for (int i = 0; i < 6; i++) {
                    double ang = (Math.PI * 2.0 * i / 6.0) + 0.35;
                    double sway = Math.sin(age * 0.11 + i * 1.7) * 0.55;
                    Vec3 basePos = new Vec3(Math.cos(ang) * rimRadius, 0.05, Math.sin(ang) * rimRadius);
                    Vec3 tangent = new Vec3(-Math.sin(ang), 0.0, Math.cos(ang));
                    Vec3 control = basePos.add(0.0, rimHeight * 0.52, 0.0)
                        .add(tangent.scale(sway))
                        .add(new Vec3(Math.cos(ang), 0.0, Math.sin(ang)).scale(-0.35));
                    Vec3 top = basePos.add(0.0, rimHeight, 0.0)
                        .add(tangent.scale(sway * 0.4))
                        .add(new Vec3(Math.cos(ang), 0.0, Math.sin(ang)).scale(-0.85));
                    verts += ComplexGeometry.tendril(pose, buffer, SEED_RAYS + i * 7919L,
                        basePos, control, top, 0.42f,
                        0xFFD833, 0xFFF3B0, 0.92f, fieldLife, distScale);
                }
                return;
            });
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    private static MaterialEmitters.Sample sampleBoundary(RandomSource random, Vec3 center,
                                                          float radius, float domeHeight) {
        double y = random.nextDouble();
        double ang = random.nextDouble() * Math.PI * 2.0;
        double flat = Math.sqrt(Math.max(0.0, 1.0 - y * y)) * radius * (0.90 + 0.10 * random.nextDouble());
        Vec3 pos = new Vec3(
            center.x + Math.cos(ang) * flat,
            center.y + 0.10 + y * domeHeight * 0.92,
            center.z + Math.sin(ang) * flat);
        return new MaterialEmitters.Sample(pos, Vec3.ZERO);
    }

    private static MaterialEmitters.Sample sampleSuspended(RandomSource random, Vec3 center,
                                                           List<Vec3> suspendedRelPositions) {
        Vec3 rel = suspendedRelPositions.get(random.nextInt(suspendedRelPositions.size()));
        Vec3 pos = center.add(rel).add(
            (random.nextDouble() * 2.0 - 1.0) * 0.72,
            (random.nextDouble() * 2.0 - 1.0) * 0.72,
            (random.nextDouble() * 2.0 - 1.0) * 0.72);
        return new MaterialEmitters.Sample(pos, Vec3.ZERO);
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

    private static float smooth(float t) {
        float x = Math.max(0.0f, Math.min(1.0f, t));
        return x * x * (3.0f - 2.0f * x);
    }
}
