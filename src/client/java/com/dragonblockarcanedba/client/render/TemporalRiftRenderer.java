package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.TemporalRiftEntity;
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Entity Renderer for Temporal Rift.
 *
 * Complex pass: a layered composition. The pieces layer is three
 * shader tendrils streaming inward from the field rim to the tear
 * heart through the tendril canvas. The temporal glass shards at the
 * heart are real FragmentEntity pieces, spawned server side by the
 * host and drawn by the fragment renderer, not by this renderer. The
 * complex particle layer
 * is WISP_CURL sprites streaming inward with the soul stream. The
 * shared material system stays as filler only: pale plasma and soul
 * material stream inward through the field volume, glass dust hangs
 * in the volume and gathers on the ground with the fractured temporal
 * read, sparse mist suggests the field boundary without drawing a
 * shell, and darkness gathers at the core. The old dome, bands, clock
 * hands, steles, streams, rings, and crystal caps stay gone.
 *
 * Identity is preserved through the celestial cyan and silver palette,
 * the synced seed, the shared FxEnvelope lifecycle (build 15 percent,
 * peak, dissipate 35 percent), and the build phase radius unfold. The
 * radius unfold scales the sampled emission volume and density, the
 * shard cluster size, and the tendril rim distance alike.
 */
public class TemporalRiftRenderer extends EntityRenderer<TemporalRiftEntity, TemporalRiftRenderer.RiftRenderState> {

    /** Live material emitters, one bundle per rendered rift entity id. */
    private final Map<Integer, RiftEmission> emissions = new HashMap<>();

    public TemporalRiftRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class RiftRenderState extends EntityRenderState {
        public int entityId = -1;
        public float radius = 12.0f;
        public float ageInTicks = 0;
        public int maxLifetime = 60;
        public long seed = 0;
        public double worldX = 0;
        public double worldY = 0;
        public double worldZ = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(TemporalRiftEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(TemporalRiftEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public RiftRenderState createRenderState() {
        return new RiftRenderState();
    }

    @Override
    public void extractRenderState(TemporalRiftEntity entity, RiftRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.entityId = entity.getId();
        state.radius = entity.getRadius();
        state.ageInTicks = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        long seed = entity.getSeed();
        if (seed == 0L) {
            // Fallback for the frames before the synced seed lands (or a rift
            // spawned without the cast constructor): hash the synced position
            // and caster, which agree on every client, never a local random.
            seed = (long) entity.getBlockX() * 341873128712L
                ^ (long) entity.getBlockZ() * 132897987541L
                ^ (long) entity.getBlockY() * 42317861L
                ^ (long) entity.getCasterId() * 2654435761L;
            if (seed == 0L) seed = 0x5EEDL;
        }
        state.seed = seed;
        state.worldX = entity.getX();
        state.worldY = entity.getY();
        state.worldZ = entity.getZ();
    }

    @Override
    public void submit(RiftRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float age = state.ageInTicks;
        float maxLife = (float) state.maxLifetime;

        // Shared lifecycle envelope: build over the first 15 percent, peak,
        // dissipate over the last 35 percent. The same envelope drives the
        // entity's server particles, drift, and damage, so the material
        // density and the second burst half all agree.
        float env = FxEnvelope.envelope(age, maxLife, 0.15f, 0.35f);
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || env <= 0.001f) {
            emissions.remove(state.entityId);
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        // The radius unfolds during the build and holds through dissipation,
        // exactly as the old presentation did. Here it scales the sampled
        // emission volume rather than a drawn silhouette.
        float buildT = FxEnvelope.smoothstep(Math.min(1.0f, age / (maxLife * 0.15f)));
        float radius = state.radius * (0.35f + 0.65f * buildT);
        Vec3 center = new Vec3(state.worldX, state.worldY, state.worldZ);

        RiftEmission emission = emissions.get(state.entityId);
        if (emission != null && age < emission.lastAge - 0.01f) {
            // Age ran backwards: a new rift took this id or the level reset.
            emissions.remove(state.entityId);
            emission = null;
        }
        boolean fresh = emission == null
            || emission.level != level
            || emission.seed != state.seed
            || emission.stateRadius != state.radius
            || emission.center.distanceToSqr(center) > 0.04;
        if (fresh) {
            emission = new RiftEmission(level, center, state.radius, state.seed);
            emissions.put(state.entityId, emission);
        }

        // Game time delta from the synced age, so emission follows game
        // time (and pauses with it) while the toolkit accumulator turns it
        // into a frame-rate independent rate.
        float delta = fresh ? (1.0f / 60.0f)
            : Math.min(0.05f, Math.max(0.0f, (age - emission.lastAge) / 20.0f));
        emission.lastAge = age;

        float dist = MaterialEmitters.distanceScale(center);
        emission.tick(delta, env * dist, radius);
        pruneEmissions(state.entityId);

        // Pieces layer, class M, in entity local space: the tear heart
        // sits at the same core lift the core emission streams toward
        // (height times 0.42), and the whole composition turns slowly
        // around the field axis with age. Vertex accounting at full
        // detail: the three inward tendrils run at reduced detail
        // scale (17 spine samples, 272 vertices each, 816), inside the
        // class M budget of 2500, and follow the same lifecycle
        // envelope as the emission. The temporal glass shards that
        // used to burst here are real FragmentEntity pieces now,
        // spawned server side by the host at tick 2 and drawn by the
        // fragment renderer; their spin is server truth, so this pose
        // rotation now serves the tendrils alone.
        if (env > 0.02f) {
            final float geoEnv = env;
            final float geoDs = FxEnvelope.distScale(Math.sqrt(Math.max(0.0, state.distanceToCameraSq)));
            final float fieldH = radius * 1.05f;
            final float tearY = fieldH * 0.42f;
            final float rimR = radius * 0.88f;
            final long pseed = state.seed;
            poseStack.pushPose();
            poseStack.mulPose(new Matrix4f().rotationY((float) Math.toRadians(age * 0.4f)));
            ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
                Vec3 heart = new Vec3(0.0, tearY, 0.0);
                for (int i = 0; i < 3; i++) {
                    double ang = i * 2.399963229728653 + (pseed & 7) * 0.11;
                    double cosA = Math.cos(ang);
                    double sinA = Math.sin(ang);
                    Vec3 rim = new Vec3(cosA * rimR, tearY + (i - 1) * fieldH * 0.22, sinA * rimR);
                    Vec3 tangent = new Vec3(-sinA, 0.0, cosA);
                    Vec3 control = rim.lerp(heart, 0.5)
                        .add(tangent.scale(rimR * 0.20)).add(new Vec3(0.0, fieldH * 0.06, 0.0));
                    ComplexGeometry.tendril(pose, buffer, pseed ^ (0x7EA5L + i * 131L),
                        rim, control, heart,
                        0.36f, 0x61C2FF, 0xEAF8FF, 0.92f, geoEnv, geoDs * 0.70f);
                }
            });
            poseStack.popPose();
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    /** Drops bundles for rifts that stopped rendering several seconds ago. */
    private void pruneEmissions(int keepId) {
        long now = System.nanoTime();
        Iterator<Map.Entry<Integer, RiftEmission>> it = emissions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, RiftEmission> entry = it.next();
            if (entry.getKey() != keepId && now - entry.getValue().lastSeenNanos > 5_000_000_000L) {
                it.remove();
            }
        }
    }

    /**
     * A mutable emission volume for one rift. Sources read the current
     * radius and height when they sample, so the build phase unfold grows
     * the sampled volume without rebuilding the emitters.
     */
    private static final class FieldVolume {
        private final Vec3 center;
        private float radius;
        private float height;

        private FieldVolume(Vec3 center, float radius) {
            this.center = center;
            setRadius(radius);
        }

        private void setRadius(float radius) {
            this.radius = radius;
            this.height = radius * 1.05f;
        }
    }

    /**
     * Samples points inside the field volume (data only, nothing is
     * drawn from it) and gives each point a velocity streaming inward
     * toward the volume's core point. This is the rift method from the
     * standard: material moves into the rift instead of sitting on a
     * rendered shell.
     */
    private static MaterialEmitters.Source inwardVolume(FieldVolume volume, double strength,
                                                          double radiusFrac, double heightFrac, double coreLift) {
        return random -> {
            double yNorm = Math.pow(random.nextDouble(), 0.85);
            double height = volume.height * heightFrac;
            double y = yNorm * height;
            double shell = Math.sqrt(Math.max(0.0, 1.0 - yNorm * yNorm));
            double rr = volume.radius * radiusFrac * shell * Math.sqrt(random.nextDouble());
            double ang = random.nextDouble() * Math.PI * 2.0;
            Vec3 pos = new Vec3(volume.center.x + Math.cos(ang) * rr,
                volume.center.y + y, volume.center.z + Math.sin(ang) * rr);
            Vec3 core = new Vec3(volume.center.x,
                volume.center.y + volume.height * coreLift, volume.center.z);
            Vec3 toCore = core.subtract(pos);
            double len = toCore.length();
            Vec3 vel = len < 1.0E-4 ? Vec3.ZERO
                : toCore.scale(strength * (0.35 + 0.65 * Math.min(1.0, len / Math.max(1.0, volume.radius))) / len);
            return new MaterialEmitters.Sample(pos, vel);
        };
    }

    /** Samples points across the ground disc, drifting inward as they settle. */
    private static MaterialEmitters.Source groundInward(FieldVolume volume, double strength) {
        return random -> {
            double ang = random.nextDouble() * Math.PI * 2.0;
            double rr = volume.radius * 0.95 * Math.sqrt(random.nextDouble());
            Vec3 pos = new Vec3(volume.center.x + Math.cos(ang) * rr,
                volume.center.y + 0.05, volume.center.z + Math.sin(ang) * rr);
            Vec3 toCenter = new Vec3(volume.center.x - pos.x, 0.0, volume.center.z - pos.z);
            double len = toCenter.length();
            Vec3 vel = len < 1.0E-4 ? Vec3.ZERO : toCenter.scale(strength / len);
            return new MaterialEmitters.Sample(pos, vel);
        };
    }

    /**
     * The material emitter bundle for one rift: pale plasma at the core,
     * celestial plasma through the field, soul material streaming inward,
     * glass dust in the volume and on the ground, sparse boundary mist,
     * and darkness at the core. One complex stream rides along: wisp curl
     * sprites streaming inward with the soul stream. Every stream runs
     * on the toolkit Emitter accumulator with its own hard per-second
     * budget.
     */
    private static final class RiftEmission {
        private final ClientLevel level;
        private final Vec3 center;
        private final float stateRadius;
        private final long seed;
        private final FieldVolume volume;
        private final MaterialEmitters.Emitter plasmaCore;
        private final MaterialEmitters.Emitter plasmaField;
        private final MaterialEmitters.Emitter soul;
        private final MaterialEmitters.Emitter wispCurl;
        private final MaterialEmitters.Emitter glassVolume;
        private final MaterialEmitters.Emitter groundGlass;
        private final MaterialEmitters.Emitter mist;
        private final MaterialEmitters.Emitter darknessCore;
        private float lastAge = -1.0f;
        private long lastSeenNanos = System.nanoTime();

        private RiftEmission(ClientLevel level, Vec3 center, float stateRadius, long seed) {
            this.level = level;
            this.center = center;
            this.stateRadius = stateRadius;
            this.seed = seed;
            this.volume = new FieldVolume(center, stateRadius);
            int baseSeed = (int) seed;
            this.plasmaCore = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xEAF8FF, 0.95f, baseSeed ^ 0x11, Vec3.ZERO, 0.012f),
                inwardVolume(volume, 0.55, 0.45, 0.55, 0.30), 90.0f, 130);
            this.plasmaField = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0x61C2FF, 1.15f, baseSeed ^ 0x23, Vec3.ZERO, 0.010f),
                inwardVolume(volume, 0.42, 1.0, 1.0, 0.48), 150.0f, 220);
            this.soul = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.soul(0xCFF4FF, 1.0f, baseSeed ^ 0x37, new Vec3(0.0, 0.006, 0.0), 0.008f),
                inwardVolume(volume, 0.30, 0.85, 0.85, 0.45), 105.0f, 160);
            // Complex particle layer: wisp curls curling inward with
            // the soul stream, inside the 40 per second continuous
            // budget for complex sprites.
            this.wispCurl = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.WISP_CURL, Vec3.ZERO, 0.008f),
                inwardVolume(volume, 0.28, 0.90, 0.90, 0.44), 32.0f, 40);
            this.glassVolume = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.glassDust(0xDDF7FF, 0.9f, baseSeed ^ 0x53, Vec3.ZERO, 0.006f),
                inwardVolume(volume, 0.20, 0.95, 0.90, 0.42), 75.0f, 115);
            this.groundGlass = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.glassDust(0xBFEAFF, 1.1f, baseSeed ^ 0x71, Vec3.ZERO, 0.010f),
                groundInward(volume, 0.10), 42.0f, 70);
            this.mist = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.mist(0xA9D8F2, 1.7f, baseSeed ^ 0x89, Vec3.ZERO, 0.004f),
                inwardVolume(volume, 0.15, 1.0, 0.95, 0.50), 26.0f, 44);
            this.darknessCore = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.darkness(0x0D1526, 1.5f, baseSeed ^ 0x107, Vec3.ZERO, 0.003f),
                inwardVolume(volume, 0.10, 0.30, 0.42, 0.22), 34.0f, 54);
        }

        private void tick(float deltaSeconds, float envelope, float radius) {
            lastSeenNanos = System.nanoTime();
            volume.setRadius(radius);
            plasmaCore.tick(deltaSeconds, envelope);
            plasmaField.tick(deltaSeconds, envelope);
            soul.tick(deltaSeconds, envelope);
            wispCurl.tick(deltaSeconds, envelope);
            glassVolume.tick(deltaSeconds, envelope);
            groundGlass.tick(deltaSeconds, envelope);
            mist.tick(deltaSeconds, envelope);
            darknessCore.tick(deltaSeconds, envelope);
        }
    }
}
