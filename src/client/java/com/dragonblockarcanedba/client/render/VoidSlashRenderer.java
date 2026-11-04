package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.VoidSlashEntity;
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
 * Entity Renderer for Void Slash in Minecraft 26.2.
 * No-shapes pass: the crescent is no longer drawn. The cut plane and its
 * synced tilt survive only as emission data: dense void plasma sampled
 * along the arc the bladeArc layers used to trace, a cyan plasma core
 * inside it, a thin white soul edge, and dark vapor shed behind the cut
 * where the wake smears trailed. The open, close, and fade timing and the
 * radius derivation are unchanged, and the seed still comes from the
 * entity UUID.
 *
 * Complex pass: the cut gains a body again, as two tendrils swept along
 * the crescent arc on the dissolve shader, a thick cyan to violet strand
 * on the core arc and a thinner violet to cyan strand on the wider cut
 * arc, so the slash reads as a torn strand of void rather than a cloud.
 * WISP_CURL sprites shed along the arc as the complex particle layer.
 * The material emission stays as filler around the strands.
 *
 * Attack upgrade pass: a second pair of arcs runs on a frame rolled
 * about the forward axis, so its strands cross the first pair along
 * the sweep instead of running parallel, and four solid void shards
 * (near-black violet, an energy material on the emissive solid path)
 * burst along the slash plane as the slash opens.
 */
public class VoidSlashRenderer extends EntityRenderer<VoidSlashEntity, VoidSlashRenderer.SlashRenderState> {
    public VoidSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SlashRenderState extends EntityRenderState {
        public boolean tiltRight = false;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public long seed = 0;
        public double ex, ey, ez;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(VoidSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(VoidSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public SlashRenderState createRenderState() {
        return new SlashRenderState();
    }

    @Override
    public void extractRenderState(VoidSlashEntity entity, SlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.tiltRight = entity.getTilt();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
    }

    /** Per-entity emission streams, keyed by the entity seed. Emission points are data only. */
    private static final class Streams {
        final ClientLevel level;
        final MaterialEmitters.Emitter cut;
        final MaterialEmitters.Emitter core;
        final MaterialEmitters.Emitter edge;
        final MaterialEmitters.Emitter wake;
        final MaterialEmitters.Emitter wisp;
        long lastNanos = 0L;
        long lastSeenNanos = 0L;
        Vec3 center = Vec3.ZERO;
        Vec3 forward = new Vec3(0, 0, 1);
        Vec3 axisA = new Vec3(1, 0, 0);
        Vec3 axisB = new Vec3(0, 1, 0);
        float radius = 1f;

        private Vec3 arcPoint(net.minecraft.util.RandomSource random, double radiusScale, double jitter) {
            double theta = (random.nextDouble() * 2.0 - 1.0) * 1.35;
            double rr = radius * radiusScale * (1.0 + (random.nextDouble() * 2.0 - 1.0) * jitter);
            return center.add(axisA.scale(Math.sin(theta) * rr)).add(axisB.scale(Math.cos(theta) * rr));
        }

        Streams(ClientLevel level, long seed) {
            this.level = level;
            int s = (int) seed;
            this.cut = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0x9905F2, 2.6f, s ^ 0xA2, Vec3.ZERO, 0.01f),
                random -> new MaterialEmitters.Sample(arcPoint(random, 1.16, 0.12), forward.scale(-0.12)), 210f, 240);
            this.core = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0x26EBFF, 1.5f, s ^ 0xA3, Vec3.ZERO, 0.008f),
                random -> new MaterialEmitters.Sample(arcPoint(random, 1.04, 0.05), forward.scale(-0.10)), 130f, 150);
            this.edge = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.soul(0xFFFFFF, 0.85f, s ^ 0xA4, Vec3.ZERO, 0.006f),
                random -> new MaterialEmitters.Sample(arcPoint(random, 0.96, 0.02), forward.scale(-0.08)), 80f, 90);
            this.wake = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(0x2B004D, 3.0f, s ^ 0xB1, Vec3.ZERO, 0.015f),
                random -> {
                    Vec3 p = center.add(forward.scale(-random.nextDouble() * 1.6))
                        .add(axisA.scale((random.nextDouble() * 2.0 - 1.0) * radius * 1.3))
                        .add(axisB.scale((random.nextDouble() * 2.0 - 1.0) * radius * 1.3));
                    return new MaterialEmitters.Sample(p, forward.scale(-0.05));
                }, 60f, 70);
            // Complex particle layer: wisp curls shed along the cut arc.
            this.wisp = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.WISP_CURL, Vec3.ZERO, 0.02f),
                random -> new MaterialEmitters.Sample(arcPoint(random, 1.10, 0.08), forward.scale(-0.08)), 30f, 36);
        }
    }

    private final Map<Long, Streams> streams = new HashMap<>();

    @Override
    public void submit(SlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float progress = Math.min(1.0f, state.age / 40.0f);
        float open = ease(Math.min(1.0f, state.age / 6.0f));
        float close = ease(Math.max(0.0f, (progress - 0.70f) / 0.30f));
        float fade = (1.0f - close) * open;
        if (fade <= 0.01f) {
            streams.remove(state.seed);
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        Vec3 center = new Vec3(state.ex, state.ey, state.ez);
        float ds = MaterialEmitters.distanceScale(center);
        float env = fade * ds;
        if (env <= 0.01f) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        Streams found = streams.get(state.seed);
        if (found == null || found.level != level) {
            found = new Streams(level, state.seed);
            streams.put(state.seed, found);
        }
        final Streams s = found;
        long now = System.nanoTime();
        float delta = s.lastNanos == 0L ? 1f / 60f : Math.min(0.05f, (now - s.lastNanos) / 1_000_000_000f);
        s.lastNanos = now;
        s.lastSeenNanos = now;
        streams.values().removeIf(other -> other != s && now - other.lastSeenNanos > 4_000_000_000L);

        s.center = center;
        s.forward = Vec3.directionFromRotation(state.xRot, state.yRot);
        Vec3 right = s.forward.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 0.001 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(s.forward).normalize();
        // The crescent leans toward its synced tilt, as the old renderer
        // drew it; the tilt now rotates the emission plane instead.
        double tilt = Math.toRadians(state.tiltRight ? -15.0 : 15.0);
        double cos = Math.cos(tilt);
        double sin = Math.sin(tilt);
        s.axisA = right.scale(cos).add(up.scale(sin));
        s.axisB = up.scale(cos).add(right.scale(-sin));
        s.radius = (1.45f + 0.40f * open) * (1.0f - 0.62f * close);

        // Pieces layer: tendrils swept along the crescent arc, in local
        // space (the pose sits at the slash center, unrotated, so the
        // synced tilt axes are plain offsets).
        {
            final Vec3 axisAL = s.axisA;
            final Vec3 axisBL = s.axisB;
            final Vec3 fwdL = s.forward;
            final float radL = s.radius;
            final long pseed = state.seed;
            final float penv = fade;
            final float pds = FxEnvelope.distScale(Math.sqrt(Math.max(0.0, state.distanceToCameraSq)));
            ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
                // Each arc is drawn as two tendrils meeting at the arc
                // mid. A single quadratic bezier across the whole 154
                // degree sweep would cut inside the crescent; splitting
                // at the mid and pushing each control point out to the
                // midpoint matched distance (radius times 2 minus cos of
                // the half sweep, about 1.22 here) keeps both spines on
                // the arc the emission traces. These four tendrils at
                // full spine samples are 1536 vertices.
                double th = 1.35;
                double ctrlCore = radL * 1.04 * 1.22;
                double ctrlCut = radL * 1.16 * 1.22;
                for (int half = 0; half < 2; half++) {
                    double a0 = half == 0 ? -th : 0.0;
                    double a1 = half == 0 ? -th * 0.5 : th * 0.5;
                    double a2 = half == 0 ? 0.0 : th;
                    ComplexGeometry.tendril(pose, buffer, pseed ^ (0xD11L + half * 131L),
                        arcLocal(axisAL, axisBL, a0, radL * 1.04),
                        arcLocal(axisAL, axisBL, a1, ctrlCore),
                        arcLocal(axisAL, axisBL, a2, radL * 1.04),
                        0.17f * radL, 0x26EBFF, 0x9905F2, 0.94f, penv, pds);
                    ComplexGeometry.tendril(pose, buffer, pseed ^ (0xD31L + half * 131L),
                        arcLocal(axisAL, axisBL, a0, radL * 1.16).add(fwdL.scale(-0.05)),
                        arcLocal(axisAL, axisBL, a1, ctrlCut).add(fwdL.scale(-0.05)),
                        arcLocal(axisAL, axisBL, a2, radL * 1.16).add(fwdL.scale(-0.05)),
                        0.10f * radL, 0x9905F2, 0x26EBFF, 0.94f, penv, pds);
                }
                // Attack upgrade: the crossing pair. Its frame is the
                // slash frame rolled about the forward axis by about
                // ten degrees, so these two arcs weave across the
                // first pair along the sweep. Four more tendrils brings
                // this submit to near 3072 vertices at full spine
                // samples, over the class M budget of 2500 at point
                // blank only; spine samples distance scale down, so it
                // sits inside budget beyond close range.
                double roll = 0.18;
                double rc = Math.cos(roll);
                double rs = Math.sin(roll);
                Vec3 axisA2 = axisAL.scale(rc).add(axisBL.scale(rs));
                Vec3 axisB2 = axisBL.scale(rc).add(axisAL.scale(-rs));
                double ctrlCross = radL * 1.10 * 1.22;
                double ctrlCrossIn = radL * 0.97 * 1.22;
                for (int half = 0; half < 2; half++) {
                    double a0 = half == 0 ? -th : 0.0;
                    double a1 = half == 0 ? -th * 0.5 : th * 0.5;
                    double a2 = half == 0 ? 0.0 : th;
                    ComplexGeometry.tendril(pose, buffer, pseed ^ (0xD51L + half * 131L),
                        arcLocal(axisA2, axisB2, a0, radL * 1.10),
                        arcLocal(axisA2, axisB2, a1, ctrlCross),
                        arcLocal(axisA2, axisB2, a2, radL * 1.10),
                        0.13f * radL, 0x9905F2, 0xD9B8FF, 0.94f, penv, pds);
                    ComplexGeometry.tendril(pose, buffer, pseed ^ (0xD71L + half * 131L),
                        arcLocal(axisA2, axisB2, a0, radL * 0.97).add(fwdL.scale(0.05)),
                        arcLocal(axisA2, axisB2, a1, ctrlCrossIn).add(fwdL.scale(0.05)),
                        arcLocal(axisA2, axisB2, a2, radL * 0.97).add(fwdL.scale(0.05)),
                        0.09f * radL, 0x26EBFF, 0xFFFFFF, 0.94f, penv, pds);
                }
            });
        }

        // Solid layer: four void shards burst along the slash plane as
        // the slash opens, in their own submit (4 times 288 vertices).
        // Near-black violet is an energy material here, so the plain
        // emissive solid path is the correct one for this palette.
        {
            final Vec3 bAxis = s.axisB;
            final float radS = s.radius;
            final float sEnv = fade * open;
            if (sEnv > 0.02f) {
                ComplexGeometry.submitSolid(collector, poseStack, (pose, buffer) ->
                    ComplexGeometry.shardBurst(pose, buffer, state.seed ^ 0xD99L,
                        Vec3.ZERO, bAxis, 58f, 0.42f * radS, 4,
                        0x2E004D, 0x9905F2, 0.95f, sEnv, ds));
            }
        }

        s.cut.tick(delta, env * 0.95f);
        s.core.tick(delta, env);
        s.edge.tick(delta, env * (1.0f - close));
        s.wisp.tick(delta, env * 0.8f);
        s.wake.tick(delta, env * 0.55f);

        super.submit(state, poseStack, collector, cameraState);
    }

    /** One point on the crescent arc in local space, matching the emission sampler's arc math. */
    private static Vec3 arcLocal(Vec3 axisA, Vec3 axisB, double theta, double radius) {
        return axisA.scale(Math.sin(theta) * radius).add(axisB.scale(Math.cos(theta) * radius));
    }

    private static float ease(float t) {
        float c = Math.max(0.0f, Math.min(1.0f, t));
        return c * c * (3.0f - 2.0f * c);
    }
}
