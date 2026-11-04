package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.SaberSlashEntity;
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
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Entity Renderer for Saber Slash in Minecraft 26.2.
 *
 * <p>Complex pass: a seeded tendril body now runs along the slash arc
 * (the same parabolic path the trail emission samples), drawn through
 * the shader tendril canvas, with SHARD_CHIP spatial fragments and
 * SPARK_STREAK complex sprites layered over it. The dimensional void
 * plasma, spatial glass dust, and darkness vapor emission stays as the
 * filler layer only. Pose timing, colors, scale, and the strike window
 * are unchanged.</p>
 *
 * <p>Attack upgrade pass: a second tendril pair crosses the first
 * diagonally so the cut reads woven, and the slash gains a solid edge
 * of thin pale steel blade slivers (authored shard calls through the
 * world-lit solid path, physical steel) riding the arc plane inside
 * the strike window. Terrain-free as always: no ground pieces, no
 * debris.</p>
 */
public class SaberSlashRenderer extends EntityRenderer<SaberSlashEntity, SaberSlashRenderer.SaberSlashRenderState> {

    private final Map<Long, Emission> emissions = new HashMap<>();

    public SaberSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SaberSlashRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public float tiltAngle = 0.0f;
        public boolean isStrong = false;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public int maxLifetime = 10;
        public double ex = 0;
        public double ey = 0;
        public double ez = 0;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(SaberSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(SaberSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public SaberSlashRenderState createRenderState() {
        return new SaberSlashRenderState();
    }

    @Override
    public void extractRenderState(SaberSlashEntity entity, SaberSlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getSlashScale();
        state.tiltAngle = entity.getTiltAngle();
        state.isStrong = entity.isStrong();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
        // Synced seed: the entity id is assigned by the server and sent in
        // the spawn packet, and the tilt and scale are synced fields, so
        // every client derives the same material seed variation.
        state.seed = entity.getId() * 0x9E3779B97F4A7C15L
            ^ Float.floatToIntBits(entity.getTiltAngle()) * 131L
            ^ Float.floatToIntBits(entity.getSlashScale());
    }

    @Override
    public void submit(SaberSlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
        long key = state.seed;
        if (progress >= 1.0f) {
            emissions.remove(key);
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        pruneEmissions();
        float open = FxEnvelope.smoothstep(Math.min(1.0f, progress / 0.34f));
        float fade = 1.0f - FxEnvelope.smoothstep(progress);
        float impact = strike(progress);
        float ds = MaterialEmitters.distanceScale(new Vec3(state.ex, state.ey, state.ez));

        Emission emission = emissions.get(key);
        if (emission == null || emission.level != level || state.age < emission.lastAge - 0.01f) {
            emission = new Emission(level, state);
            emissions.put(key, emission);
        }
        emission.update(state, open);
        float delta = emission.delta(state.age);
        float strength = fade * ds;
        if (strength > 0.01f) {
            for (MaterialEmitters.Emitter emitter : emission.emitters) {
                emitter.tick(delta, strength);
            }
        }
        if (impact > 0.02f && !emission.flashDone) {
            emission.flashDone = true;
            Vec3 center = emission.toWorld(new Vec3(0, 0, emission.chord * 0.55f));
            int plasmaCount = Math.max(4, Math.round(16 * ds));
            int glassCount = Math.max(3, Math.round(12 * ds));
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.plasma(0xFFFFFF, 0.85f, (int) (state.seed ^ 0x57AAL), Vec3.ZERO, 0.30f),
                MaterialEmitters.point(center), plasmaCount);
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.glassDust(0xBFEFFF, 0.95f, (int) (state.seed ^ 0xFA11L), Vec3.ZERO, 0.48f)
                    .withLifetimeFactor(0.6f),
                MaterialEmitters.point(center), glassCount);
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP, Vec3.ZERO, 0.42f),
                MaterialEmitters.point(center), Math.max(3, Math.round(13 * ds)));
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.50f),
                MaterialEmitters.point(center), Math.max(3, Math.round(11 * ds)));
        }

        // Pieces layer: torn tendril strands along the slash arc. Local
        // space matches the emission sampling exactly: x spans the
        // slash, z bulges to chord at the middle, and the same synced
        // rotation is applied to the pose. The first three strands run
        // the arc as before; the attack upgrade adds a second pair that
        // starts high and ends low (and its mirror), crossing the first
        // set at a seeded offset so the cut reads woven. Vertices: five
        // tendrils at at most 24 samples each stay near 1920 in the
        // energy submit, inside the class M budget of 2500 per submit.
        float pieceEnv = open * fade;
        if (pieceEnv > 0.02f && ds > 0.05f) {
            float span = emission.span;
            float chord = emission.chord;
            float sc = emission.scale;
            long seed = state.seed;
            poseStack.pushPose();
            poseStack.mulPose(new Matrix4f()
                .rotationY((float) Math.toRadians(-state.yRot))
                .rotateX((float) Math.toRadians(state.xRot))
                .rotateZ((float) Math.toRadians(state.tiltAngle)));
            ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL,
                (pose, buffer) -> {
                    ComplexGeometry.tendril(pose, buffer, seed ^ 0x7E11L,
                        new Vec3(-span * 0.5f, 0, 0), new Vec3(0, 0, chord * 2.0f),
                        new Vec3(span * 0.5f, 0, 0),
                        0.40f * sc, 0xFFFFFF, 0x4DD8FF, 0.95f, pieceEnv, ds);
                    ComplexGeometry.tendril(pose, buffer, seed ^ 0x7E22L,
                        new Vec3(-span * 0.46f, 0.07f * sc, -0.06f * sc),
                        new Vec3(0, 0.07f * sc, chord * 1.82f),
                        new Vec3(span * 0.46f, 0.07f * sc, -0.06f * sc),
                        0.20f * sc, 0x4DD8FF, 0x3D1A78, 0.90f, pieceEnv, ds);
                    ComplexGeometry.tendril(pose, buffer, seed ^ 0x7E33L,
                        new Vec3(-span * 0.46f, -0.07f * sc, 0.05f * sc),
                        new Vec3(0, -0.07f * sc, chord * 1.90f),
                        new Vec3(span * 0.46f, -0.07f * sc, 0.05f * sc),
                        0.18f * sc, 0x4DD8FF, 0xBFEFFF, 0.90f, pieceEnv, ds);
                    ComplexGeometry.tendril(pose, buffer, seed ^ 0x7E44L,
                        new Vec3(-span * 0.47f, 0.11f * sc, 0.03f * sc),
                        new Vec3(0, 0, chord * 1.66f),
                        new Vec3(span * 0.47f, -0.11f * sc, 0.03f * sc),
                        0.15f * sc, 0xBFEFFF, 0x4DD8FF, 0.90f, pieceEnv, ds);
                    ComplexGeometry.tendril(pose, buffer, seed ^ 0x7E55L,
                        new Vec3(-span * 0.47f, -0.11f * sc, 0.03f * sc),
                        new Vec3(0, 0, chord * 1.74f),
                        new Vec3(span * 0.47f, 0.11f * sc, 0.03f * sc),
                        0.13f * sc, 0x4DD8FF, 0xFFFFFF, 0.90f, pieceEnv, ds);
                });
            // Solid edge: five thin pale steel blade slivers ride the
            // arc inside the strike window, drawn lit because steel is
            // physical matter. They are authored shard calls, not a
            // shardBurst: the burst rolls girths at 0.30 to 0.44 of
            // length in mixed chunk classes, which reads as pebbles,
            // while a blade edge wants a 0.15 girth ratio and a
            // direction tangent to the arc, longest at the arc middle
            // where the cut is deepest. Five shards at 288 vertices is
            // 1440 in their own submit, inside the class M budget.
            float swing = strike(progress);
            if (swing > 0.02f) {
                float shardEnv = pieceEnv * swing;
                ComplexGeometry.submitSolidLit(collector, poseStack, state.lightCoords,
                    (pose, buffer) -> {
                        ComplexGeometry.Rng rng = new ComplexGeometry.Rng(seed ^ 0xB1ADEL);
                        for (int i = 0; i < 5; i++) {
                            float u = -0.8f + i * 0.4f;
                            Vec3 pos = new Vec3(u * span * 0.46f,
                                rng.signed() * 0.05f * sc,
                                chord * (1.0f - u * u) * 0.92f);
                            Vec3 tangent = new Vec3(span * 0.46f, 0,
                                -2.0 * u * chord * 0.92f).normalize();
                            float len = (0.42f + 0.22f * (1.0f - Math.abs(u))) * sc;
                            ComplexGeometry.shard(pose, buffer, seed + i * 0x9E37L,
                                pos, tangent, len, len * 0.15f,
                                0x8A93A6, 0xF2F7FF, 0.95f, shardEnv);
                        }
                    });
            }
            poseStack.popPose();
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    private void pruneEmissions() {
        long now = System.nanoTime();
        emissions.values().removeIf(e -> now - e.lastSeenNanos > 5_000_000_000L);
    }

    private static float strike(float progress) {
        if (progress <= 0.22f || progress >= 0.55f) return 0.0f;
        return (float) Math.sin(((progress - 0.22f) / 0.33f) * Math.PI);
    }

    /**
     * Per-effect emitter set. Render states are recreated on every
     * extraction, so the accumulators live here, keyed by the synced seed
     * on the renderer instance. Sources read the mutable fields below,
     * which submit refreshes from the current render state each frame.
     */
    private static final class Emission {
        final ClientLevel level;
        final MaterialEmitters.Emitter[] emitters;
        float lastAge = -1.0f;
        long lastSeenNanos = System.nanoTime();
        boolean flashDone = false;
        Vec3 origin = Vec3.ZERO;
        Matrix4f rotation = new Matrix4f();
        float span = 1.0f;
        float chord = 1.0f;
        float scale = 1.0f;

        Emission(ClientLevel level, SaberSlashRenderState state) {
            this.level = level;
            update(state, 0.0f);
            int seed = (int) state.seed;
            float baseScale = state.scale * (state.isStrong ? 1.28f : 1.0f);

            MaterialEmitters.Source trail = random -> {
                double t = random.nextDouble();
                double x = (t - 0.5) * span;
                double z = chord * (1.0 - Math.pow(2.0 * t - 1.0, 2.0));
                Vec3 local = new Vec3(x, (random.nextDouble() - 0.5) * 0.10 * scale, z);
                return new MaterialEmitters.Sample(toWorld(local), toWorldDirection(new Vec3(0, 0, 0.16)));
            };
            MaterialEmitters.Source glass = random -> {
                int pick = random.nextInt(3);
                Vec3 local;
                if (pick == 0) {
                    local = new Vec3((random.nextDouble() - 0.5) * 0.30 * scale, 0, chord * 0.55f);
                } else {
                    float side = pick == 1 ? -1.0f : 1.0f;
                    local = new Vec3(side * span * 0.5f, (random.nextDouble() - 0.5) * 0.16 * scale,
                        (random.nextDouble() - 0.5) * 0.20 * scale);
                }
                Vec3 outward = toWorldDirection(new Vec3(Math.signum(local.x) * 0.22, 0.08, 0.10));
                return new MaterialEmitters.Sample(toWorld(local), outward);
            };
            MaterialEmitters.Source darkness = random -> {
                Vec3 local = new Vec3(
                    (random.nextDouble() * 2.0 - 1.0) * 0.85 * scale,
                    (random.nextDouble() * 2.0 - 1.0) * 0.55 * scale,
                    chord * 0.35f + (random.nextDouble() * 2.0 - 1.0) * 0.50 * scale);
                return new MaterialEmitters.Sample(toWorld(local), toWorldDirection(new Vec3(0, 0.03, 0)));
            };

            // Merge wiring: the slash host lives 10 to 20 ticks, so the
            // long-profile streams are factored toward the host window
            // (glass dust 0.6, darkness 0.45) instead of trailing a void
            // haze for up to 62 ticks after the slash ends. Plasma already
            // dies inside the window and is untouched.
            this.emitters = new MaterialEmitters.Emitter[] {
                new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.plasma(0x4DD8FF, 1.05f * baseScale, seed, Vec3.ZERO, 0.05f),
                    trail, 300.0f, 380),
                new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.plasma(0xFFFFFF, 0.72f * baseScale, seed ^ 0xC03E, Vec3.ZERO, 0.04f),
                    trail, 130.0f, 180),
                new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.glassDust(0xBFEFFF, 0.95f * baseScale, seed ^ 0x5901, Vec3.ZERO, 0.10f)
                        .withLifetimeFactor(0.6f),
                    glass, 170.0f, 230),
                new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.darkness(0x3D1A78, 1.35f * baseScale, seed ^ 0x4A10, Vec3.ZERO, 0.05f)
                        .withLifetimeFactor(0.45f),
                    darkness, 90.0f, 130),
                // Complex particle layer: spatial shard chips riding the
                // arc and spark streaks shed at the tips. Rates stay at
                // or under the 40 per second continuous budget.
                new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP, Vec3.ZERO, 0.10f),
                    trail, 34.0f, 40),
                new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.14f),
                    glass, 30.0f, 40)
            };
        }

        void update(SaberSlashRenderState state, float open) {
            this.origin = new Vec3(state.ex, state.ey, state.ez);
            this.rotation = new Matrix4f()
                .rotationY((float) Math.toRadians(-state.yRot))
                .rotateX((float) Math.toRadians(state.xRot))
                .rotateZ((float) Math.toRadians(state.tiltAngle));
            float totalScale = state.scale * (state.isStrong ? 1.28f : 1.0f);
            this.scale = totalScale;
            this.span = 2.55f * totalScale * (0.70f + 0.30f * open);
            this.chord = 0.92f * totalScale * (0.70f + 0.30f * open);
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

        Vec3 toWorld(Vec3 local) {
            Vector3f v = this.rotation.transformPosition(new Vector3f(
                (float) local.x, (float) local.y, (float) local.z));
            return this.origin.add(v.x, v.y, v.z);
        }

        Vec3 toWorldDirection(Vec3 local) {
            Vector3f v = this.rotation.transformDirection(new Vector3f(
                (float) local.x, (float) local.y, (float) local.z));
            return new Vec3(v.x, v.y, v.z);
        }
    }
}
