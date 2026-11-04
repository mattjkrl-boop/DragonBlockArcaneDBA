package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.TimeShatterEntity;
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
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Entity Renderer for Time Shatter in Minecraft 26.2.
 *
 * No-shapes conversion: this renderer submits no geometry. The old dial,
 * spokes, shards, and core survive only as seeded emission data. Glass dust
 * hangs in the burst volume and drifts back against the outward flow, denser
 * clusters follow the old crack lines and shard trajectories, and pale plasma
 * and soul motes carry the core. The dial's color split becomes two offset
 * glass dust streams in its cyan and near-white tones. Local world-space
 * distortion is not faked; the Phase 1 shader finding rules it out here.
 *
 * Emission is guarded by whole tick per entity, so a high frame rate cannot
 * multiply the burst. Counts run through the existing 36 tick FxEnvelope and
 * the shared material distance scale. The entity, its lifetime, its synced
 * seed, and every Whis Staff mechanic are unchanged.
 */
public class TimeShatterRenderer extends EntityRenderer<TimeShatterEntity, TimeShatterRenderer.ShatterRenderState> {

    private record EmissionKey(int levelIdentity, int entityId, long seed) {}

    private final Map<EmissionKey, Integer> lastEmissionTick = new HashMap<>();

    public TimeShatterRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ShatterRenderState extends EntityRenderState {
        public int entityId = -1;
        public float scale = 1.2f;
        public float age = 0;
        public int maxLifetime = 36;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(TimeShatterEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(TimeShatterEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ShatterRenderState createRenderState() {
        return new ShatterRenderState();
    }

    @Override
    public void extractRenderState(TimeShatterEntity entity, ShatterRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.entityId = entity.getId();
        state.scale = entity.getShatterScale();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        long seed = entity.getSeed();
        if (seed == 0L) {
            // Fallback for the frames before the synced seed lands: hash the
            // entity UUID, which is itself synced state and agrees on every
            // client. Never a local random.
            seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();
            if (seed == 0L) seed = 0x5EEDL;
        }
        state.seed = seed;
    }

    @Override
    public void submit(ShatterRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        emitMaterials(state, cameraState);
        super.submit(state, poseStack, collector, cameraState);
    }

    private void emitMaterials(ShatterRenderState state, CameraRenderState cameraState) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || state.maxLifetime <= 0) return;

        EmissionKey key = new EmissionKey(System.identityHashCode(level), state.entityId, state.seed);
        if (state.age >= (float) state.maxLifetime) {
            lastEmissionTick.remove(key);
            return;
        }
        int tick = Math.max(0, Math.min(state.maxLifetime - 1, (int) Math.floor(state.age)));
        Integer last = lastEmissionTick.get(key);
        if (last != null && last.intValue() == tick) return;
        lastEmissionTick.put(key, tick);
        if (tick == 0) return;

        float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
        float fade = FxEnvelope.envelope(state.age, (float) state.maxLifetime, 0.14f, 0.50f);
        if (fade <= 0.01f) return;

        float travel = 1.0f - (1.0f - progress) * (1.0f - progress) * (1.0f - progress);
        float scale = state.scale * (0.55f + travel * 0.85f);
        Vec3 center = new Vec3(state.x, state.y, state.z);
        float distanceScale = MaterialEmitters.distanceScale(center);
        long seed = state.seed;

        Quaternionf billboard = new Quaternionf(cameraState.orientation).conjugate();
        Vec3 right = asVec(billboard.transform(new Vector3f(1.0f, 0.0f, 0.0f)));
        Vec3 up = asVec(billboard.transform(new Vector3f(0.0f, 1.0f, 0.0f)));
        Vec3 forward = asVec(billboard.transform(new Vector3f(0.0f, 0.0f, 1.0f)));

        float glassScale = Math.max(0.30f, state.scale * 0.62f);
        float plasmaScale = Math.max(0.35f, state.scale * 0.85f);
        float soulScale = Math.max(0.35f, state.scale * 0.80f);

        // Suspended glass dust through the whole burst volume. The sample
        // velocity points back toward the center, so the dust drifts against
        // the outward shatter flow instead of riding it. Merge wiring: every
        // glass dust preset in this renderer carries gravity scale 0.1, so
        // the GLASS_DUST profile's spray gravity (0.16) becomes a near
        // suspension drift (0.016) and the field reads as frozen in time
        // instead of raining down. Plasma and soul accents are untouched.
        burst(level, MaterialEmitters.Preset.glassDust(0xBDEFFF, glassScale,
                (int) (seed ^ 0xA001L), Vec3.ZERO, 0.004f).withGravityScale(0.1f),
            volumeSource(center, 1.45 * scale, seed ^ 0x701DL, true),
            count(3, fade, distanceScale));
        burst(level, MaterialEmitters.Preset.glassDust(0xFFFFFF, glassScale,
                (int) (seed ^ 0xA002L), Vec3.ZERO, 0.004f).withGravityScale(0.1f),
            volumeSource(center, 1.45 * scale, seed ^ 0x702EL, true),
            count(3, fade, distanceScale));
        burst(level, MaterialEmitters.Preset.glassDust(0xD8BFFF, glassScale,
                (int) (seed ^ 0xA003L), Vec3.ZERO, 0.004f).withGravityScale(0.1f),
            volumeSource(center, 1.45 * scale, seed ^ 0x703FL, true),
            count(2, fade, distanceScale));

        // Crack character: glass dust clustered along the eight old spoke
        // lines in the camera plane, with seeded length, angle, and depth
        // jitter. The lines are sampling data only; nothing connects them.
        burst(level, MaterialEmitters.Preset.glassDust(0x8FD8FF, glassScale,
                (int) (seed ^ 0xA004L), Vec3.ZERO, 0.003f).withGravityScale(0.1f),
            crackSource(center, right, up, forward, scale, seed ^ 0xC0FFEE1L),
            count(4, fade, distanceScale));
        burst(level, MaterialEmitters.Preset.glassDust(0xFFFFFF, glassScale,
                (int) (seed ^ 0xA005L), Vec3.ZERO, 0.003f).withGravityScale(0.1f),
            crackSource(center, right, up, forward, scale, seed ^ 0xC0FFEE2L),
            count(5, fade, distanceScale));

        // Shard trajectories: the sixteen old flight paths, sampled at the
        // current travel distance. Each palette third gets its own stream,
        // carrying the old cyan, diamond-white, and lilac cycle. The dust
        // velocity points inward along the path, against the flight.
        int[] shardColors = {0xFFFFFF, 0x8FE6FF, 0xD8BFFF};
        for (int palette = 0; palette < shardColors.length; palette++) {
            burst(level, MaterialEmitters.Preset.glassDust(shardColors[palette], glassScale,
                    (int) (seed ^ 0xA010L + palette), Vec3.ZERO, 0.003f).withGravityScale(0.1f),
                shardSource(center, scale, travel, seed, palette),
                count(2, fade, distanceScale));
        }

        // Core accents: pale plasma and a softer soul tone in a tight volume
        // at the center, where the old radiant core sat.
        burst(level, MaterialEmitters.Preset.plasma(0xAEE9FF, plasmaScale,
                (int) (seed ^ 0xA020L), Vec3.ZERO, 0.004f),
            volumeSource(center, 0.42 * scale, seed ^ 0xC0EEL, true),
            count(3, fade, distanceScale));
        burst(level, MaterialEmitters.Preset.soul(0xD9C2FF, soulScale,
                (int) (seed ^ 0xA021L), Vec3.ZERO, 0.004f),
            volumeSource(center, 0.55 * scale, seed ^ 0x5013L, false),
            count(2, fade, distanceScale));

        // Color-split ghosting: two glass dust streams sampled around the old
        // dial radius in the camera plane, offset to opposite sides. Cyan
        // leads one way, near-white the other; the split is carried by the
        // offset emission itself, not by any drawn dial.
        double ghostOffset = 0.055 * scale;
        burst(level, MaterialEmitters.Preset.glassDust(0x65D8FF, glassScale,
                (int) (seed ^ 0xA030L), Vec3.ZERO, 0.003f).withGravityScale(0.1f),
            ringGhostSource(center, right, up, forward, right.scale(ghostOffset), 1.8 * scale, seed ^ 0xD1A10L),
            count(3, fade, distanceScale));
        burst(level, MaterialEmitters.Preset.glassDust(0xF4FEFF, glassScale,
                (int) (seed ^ 0xA031L), Vec3.ZERO, 0.003f).withGravityScale(0.1f),
            ringGhostSource(center, right, up, forward, right.scale(-ghostOffset), 1.8 * scale, seed ^ 0xD1A11L),
            count(3, fade, distanceScale));
    }

    private static void burst(ClientLevel level, MaterialEmitters.Preset preset,
                              MaterialEmitters.Source source, int count) {
        if (count > 0) {
            MaterialEmitters.burst(level, preset, source, count);
        }
    }

    private static int count(int base, float fade, float distanceScale) {
        return Math.max(0, Math.round(base * fade * distanceScale));
    }

    private static Vec3 asVec(Vector3f v) {
        return new Vec3(v.x, v.y, v.z);
    }

    private static Vec3 randomOffset(Random random, double radius) {
        double u = random.nextDouble() * 2.0 - 1.0;
        double angle = random.nextDouble() * Math.PI * 2.0;
        double rr = radius * Math.cbrt(random.nextDouble());
        double flat = Math.sqrt(Math.max(0.0, 1.0 - u * u)) * rr;
        return new Vec3(Math.cos(angle) * flat, u * rr, Math.sin(angle) * flat);
    }

    /** Seeded points inside a softly flattened volume around the center. */
    private static MaterialEmitters.Source volumeSource(Vec3 center, double radius, long streamSeed, boolean inward) {
        Random random = new Random(streamSeed);
        return ignored -> {
            Vec3 offset = randomOffset(random, radius);
            offset = new Vec3(offset.x, offset.y * 0.72, offset.z);
            Vec3 velocity = offset.lengthSqr() < 1.0e-6 ? Vec3.ZERO
                    : offset.normalize().scale(inward ? -0.010 : 0.004).add(0.0, 0.0015, 0.0);
            return new MaterialEmitters.Sample(center.add(offset), velocity);
        };
    }

    /** Seeded points clustered along the eight old crack spoke lines. */
    private static MaterialEmitters.Source crackSource(Vec3 center, Vec3 right, Vec3 up, Vec3 forward,
                                                       float scale, long streamSeed) {
        Random stream = new Random(streamSeed);
        int spokes = 8;
        double[] angles = new double[spokes];
        double[] lengths = new double[spokes];
        double[] depths = new double[spokes];
        for (int i = 0; i < spokes; i++) {
            angles[i] = (i / (double) spokes) * Math.PI * 2.0 + stream.nextFloat() * 0.2f;
            lengths[i] = (1.5f + stream.nextFloat() * 1.0f) * scale;
            depths[i] = (stream.nextFloat() - 0.5f) * 0.4f * scale;
        }
        Random random = new Random(streamSeed ^ 0x5EED5L);
        return ignored -> {
            int i = random.nextInt(spokes);
            double angle = angles[i];
            double t = 0.12 + random.nextDouble() * 0.88;
            Vec3 radial = right.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
            Vec3 tangent = right.scale(-Math.sin(angle)).add(up.scale(Math.cos(angle)));
            Vec3 pos = center.add(radial.scale(lengths[i] * t))
                    .add(tangent.scale((random.nextDouble() - 0.5) * 0.13 * scale))
                    .add(forward.scale(depths[i] * (0.5 + random.nextDouble() * 0.5)));
            return new MaterialEmitters.Sample(pos, radial.scale(-0.007).add(0.0, 0.001, 0.0));
        };
    }

    /** Seeded points on one palette third of the sixteen old shard paths. */
    private static MaterialEmitters.Source shardSource(Vec3 center, float scale, float travel, long seed, int palette) {
        Random stream = new Random(seed ^ 0x51A9DL);
        int shards = 16;
        double[] angles = new double[shards];
        double[] speeds = new double[shards];
        double[] yOffsets = new double[shards];
        double[] zFactors = new double[shards];
        for (int s = 0; s < shards; s++) {
            angles[s] = (s / (double) shards) * Math.PI * 2.0 + stream.nextFloat() * 0.35f;
            speeds[s] = 1.4f + stream.nextFloat() * 1.8f;
            yOffsets[s] = (stream.nextFloat() - 0.5f) * 0.6f;
            zFactors[s] = (stream.nextFloat() - 0.5f) * 0.8f;
            stream.nextFloat();
            stream.nextFloat();
            stream.nextFloat();
        }
        int[] group = new int[shards];
        int groupSize = 0;
        for (int s = 0; s < shards; s++) {
            if (s % 3 == palette) group[groupSize++] = s;
        }
        int[] members = new int[groupSize];
        System.arraycopy(group, 0, members, 0, groupSize);
        Random random = new Random(seed ^ 0x51A90L + palette);
        return ignored -> {
            int s = members[random.nextInt(members.length)];
            double dist = travel * speeds[s] * 2.4f * scale;
            Vec3 base = center.add(new Vec3(Math.cos(angles[s]) * dist,
                    Math.sin(angles[s]) * dist + yOffsets[s],
                    zFactors[s] * dist));
            Vec3 dir = new Vec3(Math.cos(angles[s]), Math.sin(angles[s]), zFactors[s]).normalize();
            return new MaterialEmitters.Sample(base.add(randomOffset(random, 0.10 * scale)), dir.scale(-0.012));
        };
    }

    /** Seeded points around the old dial radius, offset for color splitting. */
    private static MaterialEmitters.Source ringGhostSource(Vec3 center, Vec3 right, Vec3 up, Vec3 forward,
                                                           Vec3 offset, double radius, long streamSeed) {
        Random random = new Random(streamSeed);
        return ignored -> {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double rr = radius * (0.90 + random.nextDouble() * 0.20);
            Vec3 pos = center.add(offset)
                    .add(right.scale(Math.cos(angle) * rr))
                    .add(up.scale(Math.sin(angle) * rr))
                    .add(forward.scale((random.nextDouble() - 0.5) * 0.24));
            return new MaterialEmitters.Sample(pos, forward.scale(0.002));
        };
    }
}
