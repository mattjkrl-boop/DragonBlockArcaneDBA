package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.DimensionalRiftEntity;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Entity Renderer for Dimensional Rift in Minecraft 26.2.
 * Complex pass: a layered composition. The pieces layer is a solid
 * shardBurst of void shards at the tear, mid fissure, plus three shader
 * tendrils streaming inward from the rim to the tear heart, following
 * the same orbit phase the emission clusters use. The complex particle
 * layer is WISP_CURL sprites streaming inward with the soul stream. The
 * shared material system stays as filler only: darkness smoke filling
 * the volume, soul wisps streaming inward with the pull, void plasma in
 * the tall fissure column and the singularity core, and orbiting soul
 * and darkness particle clusters. Lifecycle envelope, radius, pulse,
 * and collapse timing are the spectacle pass values, kept. The ground
 * scar, debris field, and server vortex particles stay in the entity,
 * untouched.
 */
public class DimensionalRiftRenderer extends EntityRenderer<DimensionalRiftEntity, DimensionalRiftRenderer.DimensionalRiftRenderState> {

    public DimensionalRiftRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class DimensionalRiftRenderState extends EntityRenderState {
        public float radius = 3.0f;
        public boolean isCollapsing = false;
        public float age = 0;
        public double wx = 0, wy = 0, wz = 0;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(DimensionalRiftEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(DimensionalRiftEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public DimensionalRiftRenderState createRenderState() {
        return new DimensionalRiftRenderState();
    }

    @Override
    public void extractRenderState(DimensionalRiftEntity entity, DimensionalRiftRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.isCollapsing = entity.isCollapsing();
        state.age = entity.tickCount + partialTicks;
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
        // Seed from synced state only (entity id and position arrive in the
        // spawn packet), so every client seeds the same material variation.
        state.seed = (long) entity.getId() * 2654435761L
            ^ (long) (entity.getX() * 64.0) * 40503L
            ^ (long) (entity.getZ() * 64.0) * 0x9E3779B9L;
    }

    /**
     * One live rift's emitter set. The sources read the rig's current center,
     * radius, height, pull, and orbit phase, which submit refreshes every
     * frame, so the shared Emitter accumulators keep their frame-rate
     * independence and budgets while the sampled volume follows the entity.
     */
    private static final class RiftRig {
        final ClientLevel level;
        final List<MaterialEmitters.Emitter> emitters = new ArrayList<>();
        Vec3 center = Vec3.ZERO;
        float radius = 3.0f;
        float height = 2.5f;
        float pull = 0.0f;
        float coreRadius = 1.6f;
        float orbitAngle = 0.0f;
        float lastAge = 0.0f;
        long lastSeenNanos = 0L;
        long lastTickNanos = 0L;

        RiftRig(ClientLevel level, long seed) {
            this.level = level;
            emitters.add(new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.darkness(0x0A0012, 2.6f, (int) (seed ^ 0xD44F1L), Vec3.ZERO, 0.015f),
                random -> volumeSample(random, false), 55.0f, 75));
            emitters.add(new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.soul(0x9900E6, 2.0f, (int) (seed ^ 0x50171L), Vec3.ZERO, 0.020f),
                random -> volumeSample(random, true), 85.0f, 115));
            emitters.add(new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xCC0DF2, 3.0f, (int) (seed ^ 0xF155EL), Vec3.ZERO, 0.020f),
                this::fissureSample, 65.0f, 90));
            emitters.add(new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xF5EEFF, 2.6f, (int) (seed ^ 0xC09EL), Vec3.ZERO, 0.010f),
                this::coreSample, 45.0f, 60));
            emitters.add(new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.soul(0xE633FF, 2.2f, (int) (seed ^ 0x0BB17L), Vec3.ZERO, 0.012f),
                random -> orbitSample(random, false), 40.0f, 55));
            emitters.add(new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.darkness(0x0D001A, 2.2f, (int) (seed ^ 0x0BB18L), Vec3.ZERO, 0.012f),
                random -> orbitSample(random, true), 25.0f, 35));
            emitters.add(new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.glassDust(0xCCF2FF, 1.4f, (int) (seed ^ 0x61A55L), Vec3.ZERO, 0.030f),
                this::coreSample, 20.0f, 30));
            // Complex particle layer: wisp curls riding the inward soul
            // stream, inside the 40 per second continuous budget.
            emitters.add(new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.WISP_CURL, Vec3.ZERO, 0.020f),
                random -> volumeSample(random, true), 30.0f, 40));
        }

        /** Points inside the rift volume, drifting with swirl and, for the soul stream, the pull itself. */
        private MaterialEmitters.Sample volumeSample(RandomSource random, boolean stream) {
            Vec3 volumeCenter = center.add(0.0, height * 0.5, 0.0);
            Vec3 half = new Vec3(Math.max(0.5, radius * 0.92), Math.max(0.4, height * 0.5), Math.max(0.5, radius * 0.92));
            MaterialEmitters.Sample base = MaterialEmitters.volume(volumeCenter, half).sample(random);
            Vec3 radial = new Vec3(base.pos().x - center.x, 0.0, base.pos().z - center.z);
            double len = radial.length();
            Vec3 radialDir = len < 1.0e-4 ? Vec3.ZERO : radial.scale(1.0 / len);
            Vec3 tangent = new Vec3(-radialDir.z, 0.0, radialDir.x);
            double inward = stream ? 0.22 + pull * 0.30 : 0.05;
            double swirl = stream ? 0.10 : 0.035;
            Vec3 velocity = radialDir.scale(-inward)
                .add(tangent.scale(swirl))
                .add(new Vec3(0.0, stream ? 0.0 : 0.012, 0.0));
            return new MaterialEmitters.Sample(base.pos(), velocity);
        }

        /** Points in the tall central fissure column, easing toward the axis. */
        private MaterialEmitters.Sample fissureSample(RandomSource random) {
            Vec3 volumeCenter = center.add(0.0, height * 0.5, 0.0);
            Vec3 half = new Vec3(Math.max(0.3, radius * 0.16), Math.max(0.4, height * 0.5), Math.max(0.3, radius * 0.16));
            MaterialEmitters.Sample base = MaterialEmitters.volume(volumeCenter, half).sample(random);
            Vec3 velocity = new Vec3((center.x - base.pos().x) * 0.02, 0.015, (center.z - base.pos().z) * 0.02);
            return new MaterialEmitters.Sample(base.pos(), velocity);
        }

        /** Points inside the singularity core volume, drifting inward. */
        private MaterialEmitters.Sample coreSample(RandomSource random) {
            Vec3 coreCenter = center.add(0.0, 0.5, 0.0);
            MaterialEmitters.Sample base = MaterialEmitters.sphereVolume(coreCenter, Math.max(0.35, coreRadius)).sample(random);
            Vec3 inward = coreCenter.subtract(base.pos());
            Vec3 velocity = inward.lengthSqr() < 1.0e-6 ? Vec3.ZERO : inward.normalize().scale(0.045);
            return new MaterialEmitters.Sample(base.pos(), velocity);
        }

        /**
         * Orbiting cluster points: nine seeded cluster centers carried around
         * the old shard orbit bands, each sampled with jitter so a cluster is
         * a loose knot of soul or darkness material, never a solid.
         */
        private MaterialEmitters.Sample orbitSample(RandomSource random, boolean dark) {
            int clusters = 9;
            int cluster = random.nextInt(clusters);
            double baseOrbit = radius * (0.90 - 0.58 * pull);
            double orbitRadius = Math.max(0.35, baseOrbit * ((cluster & 1) == 0 ? 0.90 : 0.68));
            double angle = orbitAngle + cluster * 2.399963229728653;
            double x = center.x + Math.cos(angle) * orbitRadius + (random.nextDouble() - 0.5) * 0.55;
            double z = center.z + Math.sin(angle) * orbitRadius + (random.nextDouble() - 0.5) * 0.55;
            double y = center.y + 0.55 + Math.sin(orbitAngle * 1.7 + cluster * 1.93) * 0.30
                + (random.nextDouble() - 0.5) * 0.45;
            Vec3 radialDir = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
            Vec3 tangent = new Vec3(-Math.sin(angle), 0.0, Math.cos(angle));
            Vec3 velocity = tangent.scale(dark ? 0.035 : 0.055)
                .add(radialDir.scale(-0.075))
                .add(new Vec3(0.0, 0.008, 0.0));
            return new MaterialEmitters.Sample(new Vec3(x, y, z), velocity);
        }
    }

    private final Map<Long, RiftRig> rigs = new HashMap<>();

    private RiftRig rigFor(ClientLevel level, long seed, float age) {
        RiftRig rig = rigs.get(seed);
        if (rig == null || rig.level != level || age < rig.lastAge - 0.001f) {
            rig = new RiftRig(level, seed);
            rigs.put(seed, rig);
        }
        return rig;
    }

    private void pruneRigs(long now) {
        Iterator<Map.Entry<Long, RiftRig>> it = rigs.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().lastSeenNanos > 5_000_000_000L) {
                it.remove();
            }
        }
    }

    @Override
    public void submit(DimensionalRiftRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            float maxLife = (float) DimensionalRiftEntity.MAX_LIFETIME;
            float lifeT = Math.min(1.0f, state.age / maxLife);
            float pull = lifeT * lifeT * (3.0f - 2.0f * lifeT);
            if (state.isCollapsing) {
                pull = Math.min(1.0f, pull + 0.40f);
            }
            // The same envelope the entity uses for its pull and particles, so
            // the material builds across the expansion and dies across the
            // collapse instead of popping at either end.
            float fade = com.dragonblockarcanedba.util.FxEnvelope.envelope(
                state.age, maxLife,
                DimensionalRiftEntity.ENVELOPE_BUILD_FRAC, DimensionalRiftEntity.ENVELOPE_DECAY_FRAC);
            float pulse = 0.85f + 0.15f * (float) Math.sin(state.age * 0.25f);
            float rot = state.age * (state.isCollapsing ? 25.0f : 8.0f);
            float coreSize = Math.max(0.55f, 1.15f * pulse) * (1.0f - pull * 0.22f);

            RiftRig rig = rigFor(level, state.seed, state.age);
            long now = System.nanoTime();
            rig.center = new Vec3(state.wx, state.wy, state.wz);
            rig.radius = state.radius;
            rig.height = Math.min(8.0f, 2.5f + (state.radius * 0.4f)) * pulse;
            rig.pull = pull;
            rig.coreRadius = coreSize * 1.45f;
            rig.orbitAngle = (float) Math.toRadians(rot * 0.8f);
            rig.lastAge = state.age;
            rig.lastSeenNanos = now;

            float delta = rig.lastTickNanos == 0L ? 0.0f
                : Math.min(0.05f, (now - rig.lastTickNanos) / 1_000_000_000.0f);
            rig.lastTickNanos = now;
            float envelope = fade * MaterialEmitters.distanceScale(rig.center);
            if (delta > 0.0f && envelope > 0.01f) {
                for (MaterialEmitters.Emitter emitter : rig.emitters) {
                    emitter.tick(delta, envelope);
                }
            }

            // Pieces layer, class M, in entity local space: the tear
            // heart sits mid fissure, where the fissure column emission
            // samples. Vertex accounting at full detail: the shard burst
            // is 5 shards at 288 vertices (1440), and the three inward
            // tendrils run at reduced detail scale (17 spine samples,
            // 272 vertices each, 816), for a worst case of 2256, inside
            // the class M budget of 2500. The burst and the tendrils
            // follow the same lifecycle fade as the emission, and the
            // tendril rims follow the same orbit phase as the clusters.
            if (fade > 0.02f) {
                final float geoEnv = fade;
                final float geoDs = com.dragonblockarcanedba.util.FxEnvelope.distScale(
                    Math.sqrt(Math.max(0.0, state.distanceToCameraSq)));
                final float tearY = rig.height * 0.5f;
                final float rimR = rig.radius;
                final float rimH = rig.height;
                final float orbit = rig.orbitAngle;
                final long pseed = state.seed;
                ComplexGeometry.submitSolid(collector, poseStack, (pose, buffer) ->
                    ComplexGeometry.shardBurst(pose, buffer, pseed ^ 0x5A17L,
                        new Vec3(0.0, tearY, 0.0), new Vec3(0.0, 1.0, 0.0), 100f, 0.80f, 5,
                        0x24053D, 0xB84DFF, 0.95f, geoEnv, geoDs));
                ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
                    Vec3 heart = new Vec3(0.0, tearY, 0.0);
                    for (int i = 0; i < 3; i++) {
                        double ang = orbit + i * 2.399963229728653 + (pseed & 7) * 0.11;
                        double cosA = Math.cos(ang);
                        double sinA = Math.sin(ang);
                        Vec3 rim = new Vec3(cosA * rimR * 0.88, 0.40 + i * rimH * 0.24, sinA * rimR * 0.88);
                        Vec3 tangent = new Vec3(-sinA, 0.0, cosA);
                        Vec3 control = rim.lerp(heart, 0.5)
                            .add(tangent.scale(rimR * 0.20)).add(new Vec3(0.0, 0.30, 0.0));
                        ComplexGeometry.tendril(pose, buffer, pseed ^ (0x7EA5L + i * 131L),
                            rim, control, heart,
                            0.22f, 0x9900E6, 0xE633FF, 0.92f, geoEnv, geoDs * 0.70f);
                    }
                });
            }
            pruneRigs(now);
        }
        super.submit(state, poseStack, collector, cameraState);
    }
}
