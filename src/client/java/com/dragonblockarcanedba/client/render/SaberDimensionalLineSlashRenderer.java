package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.SaberDimensionalLineSlashEntity;
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
import java.util.Map;

/**
 * Entity Renderer for Saber Dimensional Line Slash in Minecraft 26.2.
 *
 * <p>Complex pass: a seeded tendril body now runs the full cut line
 * with a second offset strand, drawn through the shader tendril
 * canvas, with SHARD_CHIP and SPARK_STREAK complex sprites layered
 * over it. The void plasma sheath, white core, spatial glass dust,
 * and darkness vapor emission stays as the filler layer only. Cut
 * growth, envelope fade, colors, and the strike window are
 * unchanged.</p>
 */
public class SaberDimensionalLineSlashRenderer extends EntityRenderer<SaberDimensionalLineSlashEntity, SaberDimensionalLineSlashRenderer.LineSlashRenderState> {

    private final Map<Long, Emission> emissions = new HashMap<>();

    public SaberDimensionalLineSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class LineSlashRenderState extends EntityRenderState {
        public Vec3 direction = new Vec3(1, 0, 0);
        public float lineLength = 10.0f;
        public float scale = 1.0f;
        public float age = 0;
        public int maxLifetime = 20;
        public double ex = 0;
        public double ey = 0;
        public double ez = 0;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(SaberDimensionalLineSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(SaberDimensionalLineSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public LineSlashRenderState createRenderState() {
        return new LineSlashRenderState();
    }

    @Override
    public void extractRenderState(SaberDimensionalLineSlashEntity entity, LineSlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.direction = entity.getLineDirection();
        state.lineLength = entity.getLineLength();
        state.scale = entity.getSlashScale();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
        // Synced seed: entity id plus the synced direction and length, so
        // every client seeds the same material variation along the cut.
        Vec3 dir = entity.getLineDirection();
        state.seed = entity.getId() * 0x9E3779B97F4A7C15L
            ^ Float.floatToIntBits((float) dir.x) * 31L
            ^ Float.floatToIntBits((float) dir.y) * 131L
            ^ Float.floatToIntBits((float) dir.z) * 1021L
            ^ Float.floatToIntBits(entity.getLineLength());
    }

    @Override
    public void submit(LineSlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
        long key = state.seed;
        if (progress >= 1.0f) {
            emissions.remove(key);
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        pruneEmissions();
        float cut = FxEnvelope.smoothstep(Math.min(1.0f, progress / 0.40f));
        float env = FxEnvelope.envelope(state.age, (float) state.maxLifetime, 0.15f, 0.35f);
        float fade = (1.0f - FxEnvelope.smoothstep(progress)) * Math.min(1.0f, env * 1.5f);
        float impact = strike(progress);
        float ds = MaterialEmitters.distanceScale(new Vec3(state.ex, state.ey, state.ez));

        Emission emission = emissions.get(key);
        if (emission == null || emission.level != level || state.age < emission.lastAge - 0.01f) {
            emission = new Emission(level, state);
            emissions.put(key, emission);
        }
        emission.update(state, cut);
        float delta = emission.delta(state.age);
        float strength = fade * ds;
        if (strength > 0.01f) {
            for (MaterialEmitters.Emitter emitter : emission.emitters) {
                emitter.tick(delta, strength);
            }
        }
        if (impact > 0.02f && !emission.flashDone) {
            emission.flashDone = true;
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.plasma(0xFFFFFF, 0.85f, (int) (state.seed ^ 0x19ACL), Vec3.ZERO, 0.38f),
                MaterialEmitters.point(emission.origin), Math.max(5, Math.round(18 * ds)));
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.glassDust(0xBFEFFF, 0.95f, (int) (state.seed ^ 0xFA11L), Vec3.ZERO, 0.55f),
                MaterialEmitters.point(emission.origin), Math.max(4, Math.round(14 * ds)));
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP, Vec3.ZERO, 0.50f),
                MaterialEmitters.point(emission.origin), Math.max(3, Math.round(13 * ds)));
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.55f),
                MaterialEmitters.point(emission.origin), Math.max(3, Math.round(12 * ds)));
        }

        // Pieces layer: the pose frame is world aligned at the entity
        // origin, so the synced line direction is used directly as
        // local geometry. Two tendril strands, near 770 vertices,
        // inside the class M budget of 2500.
        float pieceEnv = cut * fade;
        if (pieceEnv > 0.02f && ds > 0.05f && emission.halfLen > 0.10f) {
            Vec3 dir = emission.dir;
            Vec3 side = emission.side;
            Vec3 up = emission.up;
            float half = emission.halfLen;
            float sc = emission.scale;
            long seed = state.seed;
            Vec3 from = dir.scale(-half);
            Vec3 to = dir.scale(half);
            Vec3 bow = side.scale(0.16 * sc).add(up.scale(0.10 * sc));
            ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL,
                (pose, buffer) -> {
                    ComplexGeometry.tendril(pose, buffer, seed ^ 0x11E1L,
                        from, bow, to,
                        0.34f * sc, 0xFFFFFF, 0x4DD8FF, 0.95f, pieceEnv, ds);
                    ComplexGeometry.tendril(pose, buffer, seed ^ 0x11E2L,
                        from.add(up.scale(0.08 * sc)), bow.scale(-0.7).add(up.scale(0.08 * sc)),
                        to.add(up.scale(0.08 * sc)),
                        0.18f * sc, 0x4DD8FF, 0x3D1A78, 0.90f, pieceEnv, ds);
                });
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    private void pruneEmissions() {
        long now = System.nanoTime();
        emissions.values().removeIf(e -> now - e.lastSeenNanos > 5_000_000_000L);
    }

    private static float strike(float progress) {
        if (progress <= 0.32f || progress >= 0.62f) return 0.0f;
        return (float) Math.sin(((progress - 0.32f) / 0.30f) * Math.PI);
    }

    /**
     * Per-effect emitter set, keyed by the synced seed on the renderer
     * instance because render states are recreated on every extraction.
     * Sources sample the cut path and its rib stations as data only.
     */
    private static final class Emission {
        final ClientLevel level;
        final MaterialEmitters.Emitter[] emitters;
        float lastAge = -1.0f;
        long lastSeenNanos = System.nanoTime();
        boolean flashDone = false;
        Vec3 origin = Vec3.ZERO;
        Vec3 dir = new Vec3(1, 0, 0);
        Vec3 side = new Vec3(0, 0, 1);
        Vec3 up = new Vec3(0, 1, 0);
        float halfLen = 1.0f;
        float scale = 1.0f;

        Emission(ClientLevel level, LineSlashRenderState state) {
            this.level = level;
            update(state, 0.0f);
            int seed = (int) state.seed;
            float baseRate = Math.max(320.0f, Math.min(720.0f, 260.0f + state.lineLength * 18.0f));

            MaterialEmitters.Source sheath = random -> {
                double along = (random.nextDouble() * 2.0 - 1.0) * halfLen;
                Vec3 offset = jitter(random, 0.13 * scale);
                Vec3 pos = origin.add(dir.scale(along)).add(offset);
                return new MaterialEmitters.Sample(pos, dir.scale(0.05));
            };
            MaterialEmitters.Source core = random -> {
                double along = (random.nextDouble() * 2.0 - 1.0) * halfLen;
                Vec3 offset = jitter(random, 0.045 * scale);
                Vec3 pos = origin.add(dir.scale(along)).add(offset);
                return new MaterialEmitters.Sample(pos, dir.scale(0.05));
            };
            MaterialEmitters.Source glass = random -> {
                float[] stations = {0.12f, 0.31f, 0.50f, 0.69f, 0.88f};
                float station = stations[random.nextInt(stations.length)];
                double along = (station * 2.0 - 1.0) * halfLen
                    + (random.nextDouble() - 0.5) * 0.30 * scale;
                Vec3 pos = origin.add(dir.scale(along)).add(jitter(random, 0.10 * scale));
                Vec3 outward = side.scale(random.nextDouble() - 0.5)
                    .add(up.scale(random.nextDouble() - 0.5)).normalize().scale(0.42);
                return new MaterialEmitters.Sample(pos, outward);
            };
            MaterialEmitters.Source darkness = random -> {
                double along = (random.nextDouble() * 2.0 - 1.0) * halfLen;
                Vec3 offset = jitter(random, 0.42 * scale);
                Vec3 pos = origin.add(dir.scale(along)).add(offset);
                return new MaterialEmitters.Sample(pos, offset.scale(-0.18));
            };

            this.emitters = new MaterialEmitters.Emitter[] {
                new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.plasma(0x4DD8FF, 1.10f * state.scale, seed, Vec3.ZERO, 0.04f),
                    sheath, baseRate, Math.round(baseRate * 1.30f)),
                new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.plasma(0xFFFFFF, 0.75f * state.scale, seed ^ 0xC03E, Vec3.ZERO, 0.03f),
                    core, baseRate * 0.45f, Math.round(baseRate * 0.60f)),
                new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.glassDust(0xBFEFFF, 0.95f * state.scale, seed ^ 0xFA11, Vec3.ZERO, 0.08f),
                    glass, baseRate * 0.55f, Math.round(baseRate * 0.72f)),
                new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.darkness(0x3D1A78, 1.35f * state.scale, seed ^ 0x19AC, Vec3.ZERO, 0.04f),
                    darkness, baseRate * 0.35f, Math.round(baseRate * 0.46f)),
                // Complex particle layer: shard chips at the rib
                // stations and spark streaks along the core. Rates stay
                // at or under the 40 per second continuous budget.
                new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP, Vec3.ZERO, 0.12f),
                    glass, 34.0f, 40),
                new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.10f),
                    core, 30.0f, 40)
            };
        }

        void update(LineSlashRenderState state, float cut) {
            this.origin = new Vec3(state.ex, state.ey, state.ez);
            this.dir = state.direction.normalize();
            Vec3 helper = Math.abs(this.dir.y) > 0.90 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            this.side = this.dir.cross(helper).normalize();
            this.up = this.side.cross(this.dir).normalize();
            this.halfLen = ((state.lineLength * 0.5f) + 1.0f) * cut;
            this.scale = state.scale;
        }

        Vec3 jitter(net.minecraft.util.RandomSource random, double radius) {
            return side.scale((random.nextDouble() * 2.0 - 1.0) * radius)
                .add(up.scale((random.nextDouble() * 2.0 - 1.0) * radius));
        }

        float delta(float age) {
            this.lastSeenNanos = System.nanoTime();
            if (this.lastAge < 0.0f) {
                this.lastAge = age;
                return 1.0f / 60.0f;
            }
            float d = Math.min(0.05f, Math.max(0.0f, (age - this.lastAge) / 20.0f));
            this.lastAge = age;
            return d;
        }
    }
}
