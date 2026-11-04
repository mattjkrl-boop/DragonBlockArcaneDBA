package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.AzureStormEntity;
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
 * Entity Renderer for Azure Storm Domain in Minecraft 26.2.
 *
 * Complex pass: the storm is a layered composition. The pieces layer
 * is two tendrilFan groups of wind lashes standing in the storm
 * volume, offset to opposite sides of the eye and reseeded every
 * eight ticks so the lashes keep moving, drawn through the shader
 * submit helper with the tendril canvas. The complex particle layer
 * is ASH_FLAKE grit and WISP_CURL wisps carried on the same swirl
 * velocities as the filler. The material emission from the no-shapes
 * pass stays as filler weather: mist volumes filling the zone, vapor
 * and suspended dust driven around the center, a bright eye updraft
 * of vapor and plasma, and lightning plasma emitted along seeded
 * jagged arc paths inside the boundary. Rain inside the storm zone
 * stays the AtmosphereClient system's job, exactly as before: the
 * storm server keeps the zone open and that system renders the rain.
 * The storm's bolts stay BoltEntity spawns from the shared system;
 * this renderer owns the storm volume presentation only. All emission
 * and piece seeds take the storm entity's synced render seed, and the
 * lifetime fade envelope is the shared one the bespoke build used.
 */
public class AzureStormRenderer extends EntityRenderer<AzureStormEntity, AzureStormRenderer.AzureStormRenderState> {

    public AzureStormRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class AzureStormRenderState extends EntityRenderState {
        public float radius = 12.0f;
        public float age = 0;
        public long seed = 0;
        public int lifeTicks = 220;
        public double wx, wy, wz;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(AzureStormEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(AzureStormEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public AzureStormRenderState createRenderState() {
        return new AzureStormRenderState();
    }

    @Override
    public void extractRenderState(AzureStormEntity entity, AzureStormRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getRenderSeed();
        state.lifeTicks = entity.getLifeTicks();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    /** Mutable per-frame emission data read by the sources. Data only. */
    private static final class Frame {
        Vec3 center = Vec3.ZERO;
        float radius = 12.0f;
        float height = 12.0f;
        List<List<Vec3>> arcs = List.of();
        long arcBucket = Long.MIN_VALUE;
    }

    private static final class Emission {
        final Frame frame = new Frame();
        ClientLevel level;
        MaterialEmitters.Emitter[] emitters = new MaterialEmitters.Emitter[0];
        float lastAge = -1f;
    }

    private final Map<Long, Emission> emissions = new HashMap<>();

    /**
     * Points inside the storm cylinder, with a tangential swirl velocity
     * around the center axis and a slight upward drift.
     */
    private static MaterialEmitters.Source cylinder(Frame frame, float radiusFrac, float heightFrac,
                                                    float tangential, float up) {
        return random -> {
            double ang = random.nextDouble() * Math.PI * 2.0;
            double rr = frame.radius * radiusFrac * Math.sqrt(random.nextDouble());
            double dx = Math.cos(ang) * rr;
            double dz = Math.sin(ang) * rr;
            Vec3 pos = new Vec3(
                    frame.center.x + dx,
                    frame.center.y + random.nextDouble() * frame.height * heightFrac,
                    frame.center.z + dz);
            Vec3 vel = new Vec3(-Math.sin(ang) * tangential, up, Math.cos(ang) * tangential);
            return new MaterialEmitters.Sample(pos, vel);
        };
    }

    /** Points inside the narrow eye column, rising and swirling. */
    private static MaterialEmitters.Source eye(Frame frame) {
        return random -> {
            double ang = random.nextDouble() * Math.PI * 2.0;
            double rr = frame.radius * 0.22 * Math.sqrt(random.nextDouble());
            double dx = Math.cos(ang) * rr;
            double dz = Math.sin(ang) * rr;
            Vec3 pos = new Vec3(
                    frame.center.x + dx,
                    frame.center.y + random.nextDouble() * frame.height,
                    frame.center.z + dz);
            Vec3 vel = new Vec3(-Math.sin(ang) * 0.10, 0.20, Math.cos(ang) * 0.10);
            return new MaterialEmitters.Sample(pos, vel);
        };
    }

    /** Points along the current jagged arc paths. The paths are data only. */
    private static MaterialEmitters.Source arcPlasma(Frame frame) {
        return random -> {
            if (frame.arcs.isEmpty()) return new MaterialEmitters.Sample(frame.center, Vec3.ZERO);
            List<Vec3> path = frame.arcs.get(random.nextInt(frame.arcs.size()));
            if (path.size() < 2) return new MaterialEmitters.Sample(path.get(0), Vec3.ZERO);
            int i = random.nextInt(path.size() - 1);
            Vec3 a = path.get(i);
            Vec3 b = path.get(i + 1);
            Vec3 tangent = b.subtract(a);
            double len = tangent.length();
            Vec3 dir = len < 1e-6 ? Vec3.ZERO : tangent.scale(1.0 / len);
            return new MaterialEmitters.Sample(a.add(tangent.scale(random.nextDouble())), dir.scale(0.05));
        };
    }

    /**
     * Seeded jagged arc paths for one three-tick bucket, placed with the
     * same numbers the bespoke build used for its in-storm discharges.
     */
    private static List<List<Vec3>> buildArcs(long seed, long bucket, Vec3 center, float radius) {
        Random rng = new Random(seed ^ (bucket * 1000L));
        List<List<Vec3>> arcs = new ArrayList<>();
        int arcCount = 4;
        for (int a = 0; a < arcCount; a++) {
            double startAngle = rng.nextFloat() * Math.PI * 2.0;
            float arcR = radius * (0.25f + rng.nextFloat() * 0.65f);
            float sx = (float) Math.cos(startAngle) * arcR;
            float sz = (float) Math.sin(startAngle) * arcR;
            float sy = 7.0f + rng.nextFloat() * 9.0f;
            float ex = sx + (rng.nextFloat() * 2.0f - 1.0f) * 2.0f;
            float ez = sz + (rng.nextFloat() * 2.0f - 1.0f) * 2.0f;
            float ey = Math.max(0.5f, sy - (7.0f + rng.nextFloat() * 6.0f));
            Vec3 from = center.add(sx, sy, sz);
            Vec3 to = center.add(ex, ey, ez);
            Vec3 mid = from.add(to).scale(0.5).add(
                    (rng.nextFloat() * 2.0f - 1.0f) * 0.8f,
                    (rng.nextFloat() * 2.0f - 1.0f) * 0.4f,
                    (rng.nextFloat() * 2.0f - 1.0f) * 0.8f);
            arcs.add(List.of(from, mid, to));
            if (rng.nextFloat() < 0.6f) {
                Vec3 fork = new Vec3(
                        mid.x + (rng.nextFloat() * 2.0f - 1.0f) * 1.6f,
                        Math.max(center.y + 0.4, mid.y - (0.8f + rng.nextFloat())),
                        mid.z + (rng.nextFloat() * 2.0f - 1.0f) * 1.6f);
                arcs.add(List.of(mid, fork));
            }
        }
        return arcs;
    }

    private Emission buildEmission(ClientLevel level, AzureStormRenderState state) {
        Emission emission = new Emission();
        emission.level = level;
        Frame frame = emission.frame;
        frame.center = new Vec3(state.wx, state.wy, state.wz);
        frame.radius = state.radius;
        frame.height = state.radius;
        int seed = (int) state.seed;
        emission.emitters = new MaterialEmitters.Emitter[]{
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.mist(0xBFDCEA, 3.6f, seed ^ 0x1157,
                                new Vec3(0.0, 0.01, 0.0), 0.01f),
                        cylinder(frame, 0.95f, 0.85f, 0.03f, 0.0f), 30f, 60),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.vapor(0x9FD8E8, 2.4f, seed ^ 0x9A90,
                                new Vec3(0.0, 0.03, 0.0), 0.02f),
                        cylinder(frame, 0.90f, 0.80f, 0.16f, 0.03f), 120f, 190),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.dust(0x6E7F86, 1.2f, seed ^ 0xD057,
                                Vec3.ZERO, 0.02f),
                        cylinder(frame, 0.95f, 0.60f, 0.20f, 0.0f), 60f, 100),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.vapor(0xF2FFFF, 1.8f, seed ^ 0xE7E0,
                                new Vec3(0.0, 0.10, 0.0), 0.01f),
                        eye(frame), 80f, 130),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(0xD9FFFF, 1.0f, seed ^ 0xE7E1,
                                new Vec3(0.0, 0.08, 0.0), 0.01f),
                        eye(frame), 50f, 90),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(0x00D9FF, 1.5f, seed ^ 0xA2C0,
                                Vec3.ZERO, 0.005f),
                        arcPlasma(frame), 170f, 280),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(0xF2FFFF, 0.9f, seed ^ 0xC0DE,
                                Vec3.ZERO, 0.005f),
                        arcPlasma(frame), 90f, 150),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.fixed(DbaParticles.ASH_FLAKE,
                                Vec3.ZERO, 0.02f),
                        cylinder(frame, 0.95f, 0.85f, 0.16f, 0.02f), 24f, 40),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.fixed(DbaParticles.WISP_CURL,
                                Vec3.ZERO, 0.02f),
                        cylinder(frame, 0.90f, 0.80f, 0.16f, 0.03f), 20f, 40),
        };
        return emission;
    }

    @Override
    public void submit(AzureStormRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float radius = state.radius;
        float age = state.age;

        // Lifetime fade envelope from the shared FxEnvelope: in over the first
        // 20 ticks, out over the last 40, of whatever life this storm was
        // summoned with. The old constants assumed a fixed 220 tick life.
        float life = Math.max(20.0f, state.lifeTicks);
        float buildFrac = Math.min(0.45f, 20.0f / life);
        float decayFrac = Math.min(0.45f, 40.0f / life);
        float fade = FxEnvelope.envelope(age, life, buildFrac, decayFrac);

        Vec3 center = new Vec3(state.wx, state.wy, state.wz);
        float ds = MaterialEmitters.distanceScale(center);
        float envelope = fade * ds;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc != null ? mc.level : null;
        if (level == null || envelope <= 0.01f) return;

        Emission emission = emissions.get(state.seed);
        if (emission == null || emission.level != level || age < emission.lastAge - 1.0f) {
            emission = buildEmission(level, state);
            emissions.put(state.seed, emission);
            if (emissions.size() > 64) emissions.clear();
        }
        emission.frame.center = center;
        emission.frame.radius = radius;
        emission.frame.height = radius;
        long bucket = (long) (age / 3.0f);
        if (bucket != emission.frame.arcBucket || emission.frame.arcs.isEmpty()) {
            emission.frame.arcs = buildArcs(state.seed, bucket, center, radius);
            emission.frame.arcBucket = bucket;
        }
        float delta = emission.lastAge < 0f ? 0.05f
                : Math.min(0.05f, Math.max(0.0f, (age - emission.lastAge) / 20.0f));
        emission.lastAge = age;
        for (MaterialEmitters.Emitter emitter : emission.emitters) {
            emitter.tick(delta, envelope);
        }

        // Pieces layer: wind lashes in the storm volume. Two fans stand
        // on opposite sides of the eye, tilted with the swirl, and the
        // seed bucket rolls every eight ticks so the strands re-lash
        // instead of hanging frozen while the filler weather moves.
        final float geoR = radius;
        final float geoEnv = envelope;
        final float geoDs = ds;
        final long lashSeed = state.seed ^ 0x1A57L ^ ((long) (age / 8.0f) * 0x9E37L);
        ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
            ComplexGeometry.tendrilFan(pose, buffer, lashSeed,
                    new Vec3(geoR * 0.34, 1.0, -geoR * 0.18), new Vec3(-0.15, 1.0, 0.10),
                    38f, geoR * 0.62f, 2, 0.34f, 0x9FD8E8, 0xD9FFFF, 0.88f, geoEnv, geoDs);
            ComplexGeometry.tendrilFan(pose, buffer, lashSeed ^ 0x51EDL,
                    new Vec3(-geoR * 0.30, 1.0, geoR * 0.26), new Vec3(0.12, 1.0, -0.15),
                    38f, geoR * 0.62f, 2, 0.34f, 0x9FD8E8, 0xD9FFFF, 0.88f, geoEnv, geoDs);
        });
    }
}
