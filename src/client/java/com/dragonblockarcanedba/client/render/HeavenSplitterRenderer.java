package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.HeavenSplitterEntity;
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Entity Renderer for Heaven Splitter in Minecraft 26.2.
 * The reality cut is no longer rendered as a wall, cleave arcs, fracture
 * ribs, or shock rings. This renderer turns the synced dash path, wall
 * height, scale, and lifecycle into emission data for the shared material
 * toolkit. Cyan and white ki plasma fills the cut volume, pale vapor
 * drifts through it, steel sparks spray from the cut face, and a low
 * vapor stream runs over the ground under the midpoint. At the strike
 * fraction, plasma and vapor bursts answer at the start, midpoint, and
 * end of the path.
 */
public class HeavenSplitterRenderer extends EntityRenderer<HeavenSplitterEntity, HeavenSplitterRenderer.HeavenSplitterRenderState> {

    public HeavenSplitterRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class HeavenSplitterRenderState extends EntityRenderState {
        public Vec3 direction = new Vec3(1, 0, 0);
        public float dashLength = 16.0f;
        public float scale = 1.0f;
        public float age = 0;
        public int maxLifetime = 20;
        public long seed = 0;
        public UUID uuid = new UUID(0L, 0L);
        public double wx = 0, wy = 0, wz = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(HeavenSplitterEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(HeavenSplitterEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public HeavenSplitterRenderState createRenderState() {
        return new HeavenSplitterRenderState();
    }

    @Override
    public void extractRenderState(HeavenSplitterEntity entity, HeavenSplitterRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.direction = entity.getDashDirection();
        state.dashLength = entity.getDashLength();
        state.scale = entity.getSlashScale();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
        state.uuid = entity.getUUID();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    @Override
    public void submit(HeavenSplitterRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float fade = FxEnvelope.envelope(state.age, state.maxLifetime, 0.20f, 0.35f);
        if (fade <= 0.0f) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;

        float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
        float rise = FxEnvelope.smoothstep(Math.min(1.0f, progress / 0.25f));
        float impact = strike(progress);
        float scale = state.scale * (0.92f + 0.14f * rise);
        Vec3 origin = new Vec3(state.wx, state.wy, state.wz);
        float dist = MaterialEmitters.distanceScale(origin);
        if (dist <= 0.01f) return;

        Vec3 forward = state.direction.normalize();
        Vec3 right = new Vec3(0, 1, 0).cross(forward);
        if (right.lengthSqr() < 1.0e-6) right = new Vec3(1, 0, 0);
        right = right.normalize();
        Vec3 up = forward.cross(right).normalize();

        Anchor anchor = anchorFor(state, mc.level);
        anchor.origin = origin;
        anchor.forward = forward;
        anchor.right = right;
        anchor.up = up;
        anchor.length = state.dashLength;
        anchor.scale = scale;
        anchor.wallHeight = 7.2f * scale * (0.12f + 0.88f * rise);

        float delta = anchor.deltaSeconds(mc.isPaused());
        if (delta > 0f) {
            float envelope = fade * dist;
            anchor.mantle.tick(delta, envelope);
            anchor.main.tick(delta, envelope);
            anchor.core.tick(delta, envelope);
            anchor.vapor.tick(delta, envelope * 0.75f);
            anchor.groundVapor.tick(delta, envelope * 0.6f);
            if (impact > 0.02f) anchor.sparks.tick(delta, impact * dist);
        }

        float strikeAge = state.maxLifetime * 0.25f;
        if (!anchor.strikeBurstDone && anchor.lastAge >= 0f
                && anchor.lastAge < strikeAge && state.age >= strikeAge) {
            anchor.strikeBurstDone = true;
            float[] fractions = {0f, 0.5f, 1f};
            int[] colors = {0x00D9FF, 0xFFFFFF, 0x00F2FF};
            for (int i = 0; i < fractions.length; i++) {
                Vec3 at = origin.add(forward.scale(state.dashLength * fractions[i]));
                MaterialEmitters.burst(mc.level,
                    MaterialEmitters.Preset.plasma(colors[i], 3.0f, anchor.seed ^ (0x71A0 + i), Vec3.ZERO, 0.22f),
                    MaterialEmitters.sphereVolume(at, 1.15 * scale), 18);
                MaterialEmitters.burst(mc.level,
                    MaterialEmitters.Preset.vapor(0xBFEFFF, 4.2f, anchor.seed ^ (0x71B0 + i), Vec3.ZERO, 0.16f),
                    MaterialEmitters.sphereVolume(at, 1.45 * scale), 8);
            }
        }
        anchor.lastAge = state.age;
        super.submit(state, poseStack, collector, cameraState);
    }

    /** Sine flash peaking at 25 percent of life, when the cut lands, gone by half. */
    private static float strike(float progress) {
        if (progress >= 0.5f) return 0.0f;
        return (float) Math.sin((progress / 0.5f) * Math.PI);
    }

    private static Anchor anchorFor(HeavenSplitterRenderState state, ClientLevel level) {
        long now = System.nanoTime();
        Anchor anchor = ANCHORS.get(state.uuid);
        if (anchor == null || anchor.level != level) {
            anchor = new Anchor(level, (int) state.seed);
            ANCHORS.put(state.uuid, anchor);
        }
        anchor.lastSeenNanos = now;
        Iterator<Map.Entry<UUID, Anchor>> it = ANCHORS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Anchor> entry = it.next();
            if (!entry.getKey().equals(state.uuid) && now - entry.getValue().lastSeenNanos > 4_000_000_000L) {
                it.remove();
            }
        }
        return anchor;
    }

    private static final Map<UUID, Anchor> ANCHORS = new HashMap<>();

    private static final class Anchor {
        private final ClientLevel level;
        private final int seed;
        private long lastSeenNanos;
        private long lastTickNanos;
        private float lastAge = -1f;
        private boolean strikeBurstDone;
        private Vec3 origin = Vec3.ZERO;
        private Vec3 forward = new Vec3(0, 0, 1);
        private Vec3 right = new Vec3(1, 0, 0);
        private Vec3 up = new Vec3(0, 1, 0);
        private float length = 16f;
        private float scale = 1f;
        private float wallHeight = 1f;
        private final MaterialEmitters.Emitter mantle;
        private final MaterialEmitters.Emitter main;
        private final MaterialEmitters.Emitter core;
        private final MaterialEmitters.Emitter vapor;
        private final MaterialEmitters.Emitter sparks;
        private final MaterialEmitters.Emitter groundVapor;

        private Anchor(ClientLevel level, int seed) {
            this.level = level;
            this.seed = seed;
            MaterialEmitters.Source cutVolume = random -> {
                double along = random.nextDouble() * length;
                double height = Math.pow(random.nextDouble(), 0.85) * wallHeight;
                double thick = (random.nextDouble() * 2.0 - 1.0) * 0.30 * scale;
                Vec3 pos = origin.add(forward.scale(along)).add(up.scale(height)).add(right.scale(thick));
                Vec3 vel = right.scale((random.nextDouble() * 2.0 - 1.0) * 0.030)
                    .add(up.scale(0.012))
                    .add(forward.scale((random.nextDouble() * 2.0 - 1.0) * 0.012));
                return new MaterialEmitters.Sample(pos, vel);
            };
            MaterialEmitters.Source ground = random -> {
                Vec3 mid = origin.add(forward.scale(length * 0.5));
                double ang = random.nextDouble() * Math.PI * 2.0;
                double rr = Math.sqrt(random.nextDouble()) * 2.0 * scale;
                Vec3 pos = new Vec3(
                    mid.x + Math.cos(ang) * rr,
                    origin.y + 0.04,
                    mid.z + Math.sin(ang) * rr);
                return new MaterialEmitters.Sample(pos, new Vec3(0, 0.012, 0));
            };
            this.mantle = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0x00BFFF, 4.2f, seed, Vec3.ZERO, 0.030f),
                cutVolume, 112f, 132);
            this.main = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0x00D9FF, 3.4f, seed ^ 0x1111, Vec3.ZERO, 0.026f),
                cutVolume, 164f, 190);
            this.core = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xFFFFFF, 2.5f, seed ^ 0x2222, Vec3.ZERO, 0.020f),
                cutVolume, 82f, 100);
            this.vapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(0xBFEFFF, 5.0f, seed ^ 0x3333, Vec3.ZERO, 0.024f),
                cutVolume, 56f, 72);
            this.sparks = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK, Vec3.ZERO, 0.20f),
                cutVolume, 32f, 44);
            this.groundVapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(0x7FD8FF, 4.0f, seed ^ 0x4444, Vec3.ZERO, 0.018f),
                ground, 22f, 30);
        }

        private float deltaSeconds(boolean paused) {
            long now = System.nanoTime();
            if (lastTickNanos == 0L) {
                lastTickNanos = now;
                return paused ? 0f : 0.016f;
            }
            float delta = Math.min(0.05f, (now - lastTickNanos) / 1_000_000_000.0f);
            lastTickNanos = now;
            return paused ? 0f : delta;
        }
    }
}
