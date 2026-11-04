package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.BoltEntity;
import com.dragonblockarcanedba.particle.DbaMaterialOptions;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Branched lightning for BoltEntity.
 *
 * The seeded branch path is emission data only. Nothing here draws a
 * ribbon, a polyline strip, or a ground disc: the strike is layered
 * plasma material emitted along the trunk and fork segments by the
 * shared MaterialEmitters toolkit, in a theme colored sheath and a near
 * white core, with spark particles and a ground glow of plasma sprites
 * at the strike point. The path itself is still a pure function of the
 * synced seed, built with the exact random walk the old geometry used,
 * so every client emits along the same trunk and forks. Alpha timing is
 * the shared envelope times the same per tick flicker as before.
 */
public class BoltRenderer extends EntityRenderer<BoltEntity, BoltRenderer.BoltState> {

    public BoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class BoltState extends EntityRenderState {
        public int entityId;
        public int seed;
        public int color;
        public float length;
        public float age;
        public int life;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(BoltEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(BoltEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public BoltState createRenderState() {
        return new BoltState();
    }

    @Override
    public void extractRenderState(BoltEntity entity, BoltState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.entityId = entity.getId();
        state.seed = entity.getSeed();
        state.color = entity.getColor();
        state.length = entity.getLength();
        state.age = entity.tickCount + partialTick;
        state.life = entity.getLife();
    }

    /** One branch segment in world coordinates, with the old geometry taper kept as data. */
    private record Seg(Vec3 from, Vec3 to, float taper) {
        double length() {
            return from.distanceTo(to);
        }
    }

    /** A taper group of segments, sampled by length. */
    private static final class SegmentSet {
        final List<Seg> segments;
        final Vec3 fallback;
        final double totalLength;

        private SegmentSet(List<Seg> segments, Vec3 fallback) {
            this.segments = List.copyOf(segments);
            this.fallback = fallback;
            double total = 0.0;
            for (Seg s : segments) total += s.length();
            this.totalLength = total;
        }

        static SegmentSet group(List<Seg> all, Vec3 fallback, boolean strong) {
            List<Seg> picked = new ArrayList<>();
            for (Seg s : all) {
                if ((s.taper() >= 0.72f) == strong) picked.add(s);
            }
            return new SegmentSet(picked, fallback);
        }

        float share(double total) {
            return total <= 0.0 ? 0f : (float) (totalLength / total);
        }
    }

    private static MaterialEmitters.Source segmentSource(SegmentSet set) {
        return random -> {
            if (set.segments.isEmpty() || set.totalLength <= 0.0) {
                return new MaterialEmitters.Sample(set.fallback, Vec3.ZERO);
            }
            double pick = random.nextDouble() * set.totalLength;
            Seg chosen = set.segments.get(set.segments.size() - 1);
            for (Seg s : set.segments) {
                pick -= s.length();
                if (pick <= 0.0) {
                    chosen = s;
                    break;
                }
            }
            double t = random.nextDouble();
            return new MaterialEmitters.Sample(chosen.from().lerp(chosen.to(), t), Vec3.ZERO);
        };
    }

    /**
     * Per strike emission state. Render states are rebuilt every frame,
     * so the toolkit emitters live here, keyed by entity id and validated
     * against the synced seed, color, length, life, and strike point. An
     * entry that stops being submitted is dropped after two seconds.
     */
    private static final class Emission {
        final int seed;
        final int color;
        final float length;
        final int life;
        final Vec3 base;
        float lastAge;
        long lastSeenNanos;

        final MaterialEmitters.Source strongSource;
        final MaterialEmitters.Source weakSource;
        final MaterialEmitters.Source groundSource;
        final MaterialEmitters.Source groundCoreSource;
        final MaterialEmitters.Preset strongSheathPreset;
        final MaterialEmitters.Preset strongCorePreset;
        final MaterialEmitters.Preset weakSheathPreset;
        final MaterialEmitters.Preset weakCorePreset;
        final MaterialEmitters.Preset groundPreset;
        final MaterialEmitters.Preset groundCorePreset;
        final MaterialEmitters.Preset sparkPreset;
        final MaterialEmitters.Emitter strongSheath;
        final MaterialEmitters.Emitter strongCore;
        final MaterialEmitters.Emitter weakSheath;
        final MaterialEmitters.Emitter weakCore;
        final MaterialEmitters.Emitter ground;
        final MaterialEmitters.Emitter groundCore;
        final MaterialEmitters.Emitter sparks;
        final float strongShare;
        final float weakShare;
        final float lengthFactor;

        Emission(ClientLevel level, BoltState state, List<Seg> segments) {
            this.seed = state.seed;
            this.color = state.color;
            this.length = state.length;
            this.life = state.life;
            this.base = new Vec3(state.x, state.y, state.z);
            this.lastAge = state.age;

            SegmentSet strong = SegmentSet.group(segments, base, true);
            SegmentSet weak = SegmentSet.group(segments, base, false);
            double total = strong.totalLength + weak.totalLength;
            this.strongShare = strong.share(total);
            this.weakShare = weak.share(total);
            this.lengthFactor = (float) Math.max(0.5, Math.min(1.75, state.length / 16.0));
            float pathScale = (float) Math.max(0.35, total / Math.max(1.0, state.length));

            this.strongSource = segmentSource(strong);
            this.weakSource = segmentSource(weak);
            this.groundSource = MaterialEmitters.groundArea(base, 1.6, base.y + 0.05);
            this.groundCoreSource = MaterialEmitters.groundArea(base, 0.7, base.y + 0.05);

            int core = coreColor(state.color);
            this.strongSheathPreset = MaterialEmitters.Preset.material(DbaMaterialOptions.PLASMA,
                    state.color, 1.55f, state.seed ^ 0x5EA7, Vec3.ZERO, 0.025f);
            this.strongCorePreset = MaterialEmitters.Preset.material(DbaMaterialOptions.PLASMA,
                    core, 0.85f, state.seed ^ 0xC02E, Vec3.ZERO, 0.015f);
            this.weakSheathPreset = MaterialEmitters.Preset.material(DbaMaterialOptions.PLASMA,
                    state.color, 1.12f, state.seed ^ 0x9EA7, Vec3.ZERO, 0.025f);
            this.weakCorePreset = MaterialEmitters.Preset.material(DbaMaterialOptions.PLASMA,
                    core, 0.58f, state.seed ^ 0xF117, Vec3.ZERO, 0.015f);
            this.groundPreset = MaterialEmitters.Preset.material(DbaMaterialOptions.PLASMA,
                    state.color, 2.1f, state.seed ^ 0x6A11, Vec3.ZERO, 0.01f);
            this.groundCorePreset = MaterialEmitters.Preset.material(DbaMaterialOptions.PLASMA,
                    0xFFFFFF, 1.25f, state.seed ^ 0x6A22, Vec3.ZERO, 0.01f);
            this.sparkPreset = MaterialEmitters.Preset.fixed(DbaParticles.SPARK,
                    new Vec3(0, 0.10, 0), 0.30f);

            float sheathRate = 170f * pathScale;
            float coreRate = 300f * pathScale;
            this.strongSheath = new MaterialEmitters.Emitter(level, strongSheathPreset, strongSource,
                    sheathRate * strongShare, budget(sheathRate * strongShare));
            this.strongCore = new MaterialEmitters.Emitter(level, strongCorePreset, strongSource,
                    coreRate * strongShare, budget(coreRate * strongShare));
            this.weakSheath = new MaterialEmitters.Emitter(level, weakSheathPreset, weakSource,
                    sheathRate * weakShare, budget(sheathRate * weakShare));
            this.weakCore = new MaterialEmitters.Emitter(level, weakCorePreset, weakSource,
                    coreRate * weakShare, budget(coreRate * weakShare));
            this.ground = new MaterialEmitters.Emitter(level, groundPreset, groundSource,
                    62f, budget(62f));
            this.groundCore = new MaterialEmitters.Emitter(level, groundCorePreset, groundCoreSource,
                    42f, budget(42f));
            this.sparks = new MaterialEmitters.Emitter(level, sparkPreset,
                    MaterialEmitters.point(base), 90f, budget(90f));
        }

        private static int budget(float rate) {
            return Math.max(1, Math.round(rate * 1.8f) + 16);
        }

        /** The first frame of a strike: one burst along the whole path so the bolt is present at once. */
        void burstInitial(ClientLevel level, float strength) {
            MaterialEmitters.burst(level, strongSheathPreset, strongSource,
                    Math.round(9f * lengthFactor * strength * strongShare));
            MaterialEmitters.burst(level, strongCorePreset, strongSource,
                    Math.round(15f * lengthFactor * strength * strongShare));
            MaterialEmitters.burst(level, weakSheathPreset, weakSource,
                    Math.round(7f * lengthFactor * strength * weakShare));
            MaterialEmitters.burst(level, weakCorePreset, weakSource,
                    Math.round(11f * lengthFactor * strength * weakShare));
            MaterialEmitters.burst(level, groundPreset, groundSource,
                    Math.round(5f * strength));
            MaterialEmitters.burst(level, groundCorePreset, groundCoreSource,
                    Math.round(4f * strength));
            MaterialEmitters.burst(level, sparkPreset, MaterialEmitters.point(base),
                    Math.round(8f * strength));
        }

        void tick(float deltaSeconds, float strength) {
            strongSheath.tick(deltaSeconds, strength);
            strongCore.tick(deltaSeconds, strength);
            weakSheath.tick(deltaSeconds, strength);
            weakCore.tick(deltaSeconds, strength);
            ground.tick(deltaSeconds, strength * 0.8f);
            groundCore.tick(deltaSeconds, strength * 0.8f);
            sparks.tick(deltaSeconds, strength);
        }
    }

    private final Map<Integer, Emission> emissions = new HashMap<>();

    @Override
    public void submit(BoltState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float env = FxEnvelope.envelope(state.age, Math.max(1, state.life), 0.12f, 0.45f);
        if (env <= 0.01f) return;
        // Flicker: reseeded per whole tick from the bolt seed.
        Random flicker = new Random(state.seed * 31L + (int) state.age);
        float fl = 0.65f + flicker.nextFloat() * 0.35f;
        float dist = FxEnvelope.distScale(Math.sqrt(Math.max(0.0, state.distanceToCameraSq)));
        float strength = env * fl * dist;

        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && strength > 0.01f) {
            long now = System.nanoTime();
            emissions.values().removeIf(e -> now - e.lastSeenNanos > 2_000_000_000L);
            Vec3 base = new Vec3(state.x, state.y, state.z);
            Emission emission = emissions.get(state.entityId);
            boolean fresh = emission == null
                    || emission.seed != state.seed
                    || emission.color != state.color
                    || emission.length != state.length
                    || emission.life != state.life
                    || !emission.base.equals(base)
                    || emission.lastAge > state.age + 0.001f;
            if (fresh) {
                emission = new Emission(level, state, buildSegments(state));
                emissions.put(state.entityId, emission);
                emission.burstInitial(level, strength);
                emission.lastAge = state.age;
            } else {
                float deltaTicks = Math.max(0f, state.age - emission.lastAge);
                if (deltaTicks > 0f) {
                    emission.tick(deltaTicks / 20.0f, strength);
                    emission.lastAge = state.age;
                }
            }
            emission.lastSeenNanos = now;
            if (state.age >= state.life) emissions.remove(state.entityId);
        }
        super.submit(state, poseStack, collector, cameraState);
    }

    /** The old near white core color: the theme color pushed 75 percent toward white. */
    private static int coreColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        r += Math.round((255 - r) * 0.75f);
        g += Math.round((255 - g) * 0.75f);
        b += Math.round((255 - b) * 0.75f);
        return (r << 16) | (g << 8) | b;
    }

    /**
     * Builds the branch path from the synced seed. This is the exact
     * random walk the old ribbon geometry used, kept unchanged as data:
     * the trunk points climb with a jittered walk and the forks leave the
     * trunk and dive outward, consuming the seed in the same order.
     */
    private static List<Seg> buildSegments(BoltState state) {
        Vec3 base = new Vec3(state.x, state.y, state.z);
        Random rand = new Random(state.seed);
        int steps = 12;
        float[] tx = new float[steps + 1];
        float[] ty = new float[steps + 1];
        float[] tz = new float[steps + 1];
        float px = 0f, pz = 0f;
        for (int i = 0; i <= steps; i++) {
            tx[i] = px; ty[i] = state.length * i / steps; tz[i] = pz;
            px += (rand.nextFloat() - 0.5f) * 0.55f;
            pz += (rand.nextFloat() - 0.5f) * 0.55f;
        }
        tx[steps] = px; tz[steps] = pz;
        List<Seg> segs = new ArrayList<>();
        for (int i = 0; i < steps; i++) {
            segs.add(new Seg(base.add(tx[i], ty[i], tz[i]), base.add(tx[i + 1], ty[i + 1], tz[i + 1]),
                    1f - i / (float) steps * 0.4f));
        }
        int forks = 2 + rand.nextInt(3);
        for (int f = 0; f < forks; f++) {
            int start = 3 + rand.nextInt(Math.max(1, steps - 5));
            float bx = tx[start], by = ty[start], bz = tz[start];
            float dirX = (rand.nextFloat() - 0.5f) * 2f;
            float dirZ = (rand.nextFloat() - 0.5f) * 2f;
            int blen = 3 + rand.nextInt(3);
            float drop = state.length / steps * 0.9f;
            for (int i = 0; i < blen; i++) {
                float nx = bx + dirX * (0.5f + rand.nextFloat() * 0.5f);
                float nz = bz + dirZ * (0.5f + rand.nextFloat() * 0.5f);
                float ny = by - drop * (0.7f + rand.nextFloat() * 0.5f);
                if (ny < 0.2f) break;
                segs.add(new Seg(base.add(bx, by, bz), base.add(nx, ny, nz),
                        0.55f * (1f - i / (float) blen)));
                bx = nx; by = ny; bz = nz;
            }
        }
        return segs;
    }
}
