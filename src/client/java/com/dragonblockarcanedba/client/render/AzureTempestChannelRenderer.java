package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.AzureTempestChannelEntity;
import com.dragonblockarcanedba.particle.DbaParticles;
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
 * Entity Renderer for Call of the Tempest Channeling in Minecraft 26.2.
 *
 * Complex pass: the channel is a layered composition. The pieces
 * layer is a five strand ComplexGeometry tendrilFan rising through
 * the tunnel, joined by a ring of six small storm-glass shards that
 * orbit the channel at waist height. The complex particle layer is SPARK_STREAK sprites
 * rising through the channel volume. The material emission from the
 * no-shapes pass stays as filler: vapor and plasma rising through
 * the channel volume with tangential swirl velocities, mist drifting
 * at the boundary, ground dust pulled around the base, and lightning
 * plasma emitted along seeded jagged arc paths once the charge
 * passes a quarter. Charge scaling, the spawn fade-in, and the
 * release fade-out are unchanged from the bespoke build.
 */
public class AzureTempestChannelRenderer extends EntityRenderer<AzureTempestChannelEntity, AzureTempestChannelRenderer.ChannelRenderState> {

    public AzureTempestChannelRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ChannelRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public float age = 0;
        public long seed = 0;
        public boolean isFirstPersonOwner = false;
        public boolean releasing = false;
        public int releaseAge = 0;
        public double wx, wy, wz;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(AzureTempestChannelEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(AzureTempestChannelEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ChannelRenderState createRenderState() {
        return new ChannelRenderState();
    }

    @Override
    public void extractRenderState(AzureTempestChannelEntity entity, ChannelRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getRenderSeed();
        state.releasing = entity.isReleasing();
        state.releaseAge = entity.getReleaseAge();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null &&
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) &&
            mc.options.getCameraType().isFirstPerson());
    }

    /** Mutable per-frame emission data read by the sources. Data only. */
    private static final class Frame {
        Vec3 center = Vec3.ZERO;
        float tunnelRadius = 1.6f;
        float tunnelHeight = 3.2f;
        List<List<Vec3>> arcs = List.of();
        long arcBucket = Long.MIN_VALUE;
    }

    private static final class Emission {
        final Frame frame = new Frame();
        ClientLevel level;
        MaterialEmitters.Emitter[] main = new MaterialEmitters.Emitter[0];
        MaterialEmitters.Emitter arcCyan;
        MaterialEmitters.Emitter arcWhite;
        float lastAge = -1f;
    }

    private final Map<Long, Emission> emissions = new HashMap<>();

    /**
     * Points inside the channel volume, with tangential swirl and rising
     * velocities so the particles themselves carry the rotation.
     */
    private static MaterialEmitters.Source rising(Frame frame, float radiusFrac, float tangential, float up) {
        return random -> {
            double ang = random.nextDouble() * Math.PI * 2.0;
            double rr = frame.tunnelRadius * radiusFrac * Math.sqrt(random.nextDouble());
            double dx = Math.cos(ang) * rr;
            double dz = Math.sin(ang) * rr;
            Vec3 pos = new Vec3(
                    frame.center.x + dx,
                    frame.center.y + random.nextDouble() * frame.tunnelHeight,
                    frame.center.z + dz);
            Vec3 vel = new Vec3(-Math.sin(ang) * tangential, up, Math.cos(ang) * tangential);
            return new MaterialEmitters.Sample(pos, vel);
        };
    }

    /** Points near the channel boundary shell, drifting upward. */
    private static MaterialEmitters.Source boundary(Frame frame) {
        return random -> {
            double ang = random.nextDouble() * Math.PI * 2.0;
            double rr = frame.tunnelRadius * (0.90 + random.nextDouble() * 0.15);
            double dx = Math.cos(ang) * rr;
            double dz = Math.sin(ang) * rr;
            Vec3 pos = new Vec3(
                    frame.center.x + dx,
                    frame.center.y + random.nextDouble() * frame.tunnelHeight,
                    frame.center.z + dz);
            Vec3 vel = new Vec3(-Math.sin(ang) * 0.18, 0.05, Math.cos(ang) * 0.18);
            return new MaterialEmitters.Sample(pos, vel);
        };
    }

    /** Points across the base disc, pulled around and slightly inward. */
    private static MaterialEmitters.Source groundSwirl(Frame frame) {
        return random -> {
            double ang = random.nextDouble() * Math.PI * 2.0;
            double rr = frame.tunnelRadius * 1.05 * Math.sqrt(random.nextDouble());
            double dx = Math.cos(ang) * rr;
            double dz = Math.sin(ang) * rr;
            Vec3 pos = new Vec3(
                    frame.center.x + dx,
                    frame.center.y + 0.05 + random.nextDouble() * 0.12,
                    frame.center.z + dz);
            Vec3 vel = new Vec3(
                    -Math.sin(ang) * 0.30 - Math.cos(ang) * 0.04,
                    0.03,
                    Math.cos(ang) * 0.30 - Math.sin(ang) * 0.04);
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
     * Seeded jagged arc paths for one half-tick bucket, placed with the
     * same numbers the bespoke build used for its channel arcs.
     */
    private static List<List<Vec3>> buildArcs(long seed, long bucket, Vec3 center,
                                              float tunnelRadius, float tunnelHeight, float charge) {
        Random rng = new Random(seed ^ (bucket * 1000L));
        List<List<Vec3>> arcs = new ArrayList<>();
        int arcCount = 2 + (int) (charge * 4);
        for (int a = 0; a < arcCount; a++) {
            double arcAngle = rng.nextFloat() * Math.PI * 2.0;
            float arcR = tunnelRadius * (0.6f + rng.nextFloat() * 0.5f);
            float sx = (float) Math.cos(arcAngle) * arcR;
            float sz = (float) Math.sin(arcAngle) * arcR;
            float sy = 0.2f + rng.nextFloat() * tunnelHeight;
            Vec3 from = center.add(sx, sy, sz);
            Vec3 to = center.add(
                    sx + (rng.nextFloat() * 2.0f - 1.0f) * 1.8f,
                    Math.max(0.1f, sy + (rng.nextFloat() * 2.0f - 1.0f) * 1.5f),
                    sz + (rng.nextFloat() * 2.0f - 1.0f) * 1.8f);
            Vec3 mid = from.add(to).scale(0.5).add(
                    (rng.nextFloat() * 2.0f - 1.0f) * 0.5f,
                    (rng.nextFloat() * 2.0f - 1.0f) * 0.3f,
                    (rng.nextFloat() * 2.0f - 1.0f) * 0.5f);
            arcs.add(List.of(from, mid, to));
        }
        return arcs;
    }

    private Emission buildEmission(ClientLevel level, ChannelRenderState state) {
        Emission emission = new Emission();
        emission.level = level;
        Frame frame = emission.frame;
        frame.center = new Vec3(state.wx, state.wy, state.wz);
        int seed = (int) state.seed;
        emission.main = new MaterialEmitters.Emitter[]{
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.vapor(0x9FD8E8, 2.0f, seed ^ 0x9A90,
                                new Vec3(0.0, 0.04, 0.0), 0.02f),
                        rising(frame, 0.85f, 0.22f, 0.14f), 150f, 220),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.mist(0xBFDCEA, 2.8f, seed ^ 0x1157,
                                Vec3.ZERO, 0.01f),
                        boundary(frame), 34f, 60),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(0x4DF2FF, 1.0f, seed ^ 0xBE10,
                                Vec3.ZERO, 0.01f),
                        rising(frame, 0.85f, 0.22f, 0.14f), 80f, 130),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.dust(0x7E8A8F, 1.1f, seed ^ 0xD057,
                                Vec3.ZERO, 0.02f),
                        groundSwirl(frame), 50f, 80),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK,
                                new Vec3(0.0, 0.30, 0.0), 0.02f),
                        rising(frame, 0.85f, 0.22f, 0.14f), 26f, 40),
        };
        emission.arcCyan = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0x00D9FF, 1.3f, seed ^ 0xA2C5,
                        Vec3.ZERO, 0.005f),
                arcPlasma(frame), 150f, 240);
        emission.arcWhite = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xFFFFFF, 0.85f, seed ^ 0xC02E,
                        Vec3.ZERO, 0.005f),
                arcPlasma(frame), 80f, 130);
        return emission;
    }

    @Override
    public void submit(ChannelRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float charge = state.chargeRatio;
        float age = state.age;

        float tunnelRadius = (state.isFirstPersonOwner ? 2.4f : 1.6f) + (charge * 2.2f);
        float tunnelHeight = 3.2f + (charge * 2.4f);
        // Spawn fade-in over the first 5 ticks so the channel does not
        // pop in at partial density the tick the channel starts.
        float intro = Math.min(1.0f, age / 5.0f);
        intro = intro * intro * (3.0f - 2.0f * intro);
        // Release fade-out: on release the entity collapses over
        // RELEASE_TICKS instead of vanishing in one tick at full density.
        float outro = state.releasing
            ? Math.max(0.0f, 1.0f - state.releaseAge / (float) AzureTempestChannelEntity.RELEASE_TICKS)
            : 1.0f;
        float baseAlpha = ((state.isFirstPersonOwner ? 0.35f : 0.55f) + (charge * 0.35f)) * intro * outro;
        float envelope = (baseAlpha / 0.90f);

        Vec3 center = new Vec3(state.wx, state.wy, state.wz);
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc != null ? mc.level : null;
        if (level != null && envelope > 0.01f) {
            Emission emission = emissions.get(state.seed);
            if (emission == null || emission.level != level || age < emission.lastAge - 1.0f) {
                emission = buildEmission(level, state);
                emissions.put(state.seed, emission);
                if (emissions.size() > 64) emissions.clear();
            }
            emission.frame.center = center;
            emission.frame.tunnelRadius = tunnelRadius;
            emission.frame.tunnelHeight = tunnelHeight;
            float delta = emission.lastAge < 0f ? 0.05f
                    : Math.min(0.05f, Math.max(0.0f, (age - emission.lastAge) / 20.0f));
            emission.lastAge = age;
            for (MaterialEmitters.Emitter emitter : emission.main) {
                emitter.tick(delta, envelope);
            }
            // Electric arcs intensify with charge, from a quarter charge up,
            // reseeded on the same half tick cadence the bespoke build used.
            if (charge >= 0.25f) {
                long bucket = (long) (age * 2.0f);
                if (bucket != emission.frame.arcBucket || emission.frame.arcs.isEmpty()) {
                    emission.frame.arcs = buildArcs(state.seed, bucket, center, tunnelRadius, tunnelHeight, charge);
                    emission.frame.arcBucket = bucket;
                }
                float arcEnvelope = envelope * Math.min(1.0f, charge * 1.6f);
                emission.arcCyan.tick(delta, arcEnvelope);
                emission.arcWhite.tick(delta, arcEnvelope);
            }

            // Pieces layer: one five strand fan rises through the
            // tunnel, and six physical storm-glass shards ride a waist
            // height orbit in the same direction as the swirl. The
            // shards are world lit because they read as solid glass,
            // not as another glow layer.
            final float geoR = tunnelRadius;
            final float geoH = tunnelHeight;
            final float geoAge = age;
            final float geoEnv = envelope;
            final float geoDs = MaterialEmitters.distanceScale(center);
            final float geoWidth = 0.26f + charge * 0.18f;
            final long geoSeed = state.seed;
            final int geoLight = state.lightCoords;
            if (geoEnv > 0.01f && geoDs > 0.01f) {
                ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) ->
                        ComplexGeometry.tendrilFan(pose, buffer, geoSeed ^ 0x7E00L,
                                new Vec3(0.0, 0.15, 0.0), new Vec3(0.0, 1.0, 0.0),
                                34.0f, geoH * 0.94f, 5, geoWidth,
                                0x4DF2FF, 0xFFFFFF, 0.90f, geoEnv, geoDs));
                ComplexGeometry.submitSolidLit(collector, poseStack, geoLight, (pose, buffer) -> {
                    for (int i = 0; i < 6; i++) {
                        double angle = i * (Math.PI * 2.0 / 6.0) + geoAge * 0.06;
                        double cos = Math.cos(angle);
                        double sin = Math.sin(angle);
                        Vec3 origin = new Vec3(cos * geoR * 0.78, geoH * 0.48, sin * geoR * 0.78);
                        Vec3 dir = new Vec3(-sin, 0.30, cos);
                        float length = 0.52f + (i % 3) * 0.13f;
                        ComplexGeometry.shard(pose, buffer, geoSeed ^ (0x51A0L + i * 131L),
                                origin, dir, length, length * 0.32f,
                                0x1D6F8E, 0xCFF7FF, 0.95f, geoEnv);
                    }
                });
            }
        }

        super.submit(state, poseStack, collector, cameraState);
    }
}
