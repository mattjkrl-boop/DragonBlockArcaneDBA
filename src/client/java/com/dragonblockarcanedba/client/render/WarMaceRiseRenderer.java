package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.entity.WarMaceRiseEntity;
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

/**
 * The rising crescent, rebuilt for the complex effects redo as a layered
 * composition. At the rise point on the ground, a small fracturePlates
 * fan of lifted stone slabs (submitSolid, faceted shading) sits under a
 * crackFan of bronze energy cracks running through it on the shader
 * (submitEnergy), and a SHARD_CHIP burst plus a short chip stream throw
 * stone fragments off the base ring. Those pieces are the fast near
 * field structure of the launch; everything below stays as the filler
 * layer. The crescent itself is a path, and a path is data: bronze
 * plasma is emitted along the same 250 degree arc, in the
 * same facing plane, climbing at the same 0.22 blocks per tick the hit
 * volume climbs, so what the target sees is still what hits it. The main
 * arc, the wider wisp, and the two lagging ghost arcs below are separate
 * emission layers at their old radii, heights, colors, and relative
 * strengths. The expanding launch ring at the base is ground dust and
 * vapor rolling outward for the first ticks.
 *
 * Identity is preserved: the bronze palette, the open ease over the first
 * 3 ticks, the synced seed, and the shared FxEnvelope lifecycle (build 12
 * percent, dissipate 30 percent) all carry over from the bespoke build.
 * Only what the effect is made of changed.
 */
public class WarMaceRiseRenderer extends EntityRenderer<WarMaceRiseEntity, WarMaceRiseRenderer.RiseState> {
    /** Arc sweep in degrees, centered on the facing direction. Data for the emission path. */
    private static final float SWEEP = 250.0f;

    /** Bright bronze of the main crescent. */
    private static final int MAIN = 0xF7D180;
    /** Pale bronze of the wide wisp layer. */
    private static final int WISP = 0xFFEBB3;
    /** Theme bronze of the lower ghost layer. */
    private static final int GHOST_LOW = 0xD99E47;
    /** Theme bronze of the upper ghost layer. */
    private static final int GHOST_MID = 0xE0A852;
    /** Pale vapor bronze. */
    private static final int PALE = 0xEBC280;

    /** Live material emitters, one bundle per rendered rise entity id. */
    private final Map<Integer, RiseEmission> emissions = new HashMap<>();

    public WarMaceRiseRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class RiseState extends EntityRenderState {
        public int entityId;
        public float age;
        public float yRot;
        public int seed;
        public double ex;
        public double ey;
        public double ez;
    }

    public boolean shouldRender(WarMaceRiseEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(WarMaceRiseEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public RiseState createRenderState() {
        return new RiseState();
    }

    @Override
    public void extractRenderState(WarMaceRiseEntity entity, RiseState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.entityId = entity.getId();
        state.age = entity.tickCount + partialTick;
        state.yRot = entity.getYRot();
        state.seed = entity.getSeed();
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
    }

    @Override
    public void submit(RiseState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float env = FxEnvelope.envelope(state.age, 8.0f, 0.12f, 0.30f);
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || env <= 0.001f) {
            emissions.remove(state.entityId);
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        float open = ease(Math.min(1.0f, state.age / 3.0f));
        float lift = state.age * 0.22f;
        float radius = 1.15f * (0.45f + 0.55f * open);
        Vec3 center = new Vec3(state.ex, state.ey, state.ez);

        RiseEmission emission = emissions.get(state.entityId);
        if (emission != null && state.age < emission.lastAge - 0.01f) {
            // Age ran backwards: a new rise took this id or the level reset.
            emissions.remove(state.entityId);
            emission = null;
        }
        boolean fresh = emission == null
            || emission.level != level
            || emission.seed != state.seed;
        if (fresh) {
            emission = new RiseEmission(level, center, state.seed);
            emissions.put(state.entityId, emission);
        }

        // The frame follows the climb every submit: the arc path is
        // sampled at the live lift and radius, in the facing plane of the
        // live yRot, matching the renderer's old pose math point for point.
        Frame frame = emission.frame;
        frame.center = center;
        frame.radius = radius;
        frame.lift = lift;
        frame.baseRadius = 0.8f + state.age * 0.55f;
        double yawRad = Math.toRadians(state.yRot);
        frame.forward = new Vec3(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
        frame.right = new Vec3(Math.cos(yawRad), 0.0, Math.sin(yawRad));

        float delta = fresh ? (1.0f / 60.0f)
            : Math.min(0.05f, Math.max(0.0f, (state.age - emission.lastAge) / 20.0f));
        emission.lastAge = state.age;

        float dist = MaterialEmitters.distanceScale(center);
        float ringEnv = state.age < 3.5f ? (1.0f - state.age / 3.5f) * 0.6f : 0.0f;
        emission.tick(delta, env * dist, ringEnv * dist);
        if (fresh) {
            // Complex particle layer, burst half: stone chips thrown off
            // the base ring at the launch moment, inside the class M
            // burst budget of 30.
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP,
                    new Vec3(0.0, 0.32, 0.0), 0.18f),
                baseRing(emission.frame), 20);
        }

        // Pieces layer at the rise point, in the local ground frame:
        // small fractured plates lifted at the base, with a bronze crack
        // fan glowing through and past them. Combined vertices stay well
        // inside the class M budget: plates draw about 480 and the crack
        // fan under 900 of the 2500 allowed.
        final long geoSeed = state.seed;
        final float geoEnv = env;
        final float geoDist = dist;
        ComplexGeometry.submitSolid(collector, poseStack, (pose, buffer) ->
            ComplexGeometry.fracturePlates(pose, buffer, geoSeed ^ 0xF9A7L,
                1.05f, 0.16f, GHOST_LOW, 0x4A3826, 0.92f, geoEnv, geoDist));
        ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) ->
            ComplexGeometry.crackFan(pose, buffer, geoSeed ^ 0xC9ACL,
                1.35f, WISP, GHOST_LOW, 0.95f, geoEnv, geoDist));
        pruneEmissions(state.entityId);

        super.submit(state, poseStack, collector, cameraState);
    }

    /** Drops bundles for rises that stopped rendering several seconds ago. */
    private void pruneEmissions(int keepId) {
        long now = System.nanoTime();
        Iterator<Map.Entry<Integer, RiseEmission>> it = emissions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, RiseEmission> entry = it.next();
            if (entry.getKey() != keepId && now - entry.getValue().lastSeenNanos > 5_000_000_000L) {
                it.remove();
            }
        }
    }

    /** Mutable per-frame sampling data read by the sources. Data only. */
    private static final class Frame {
        Vec3 center = Vec3.ZERO;
        Vec3 forward = new Vec3(0.0, 0.0, 1.0);
        Vec3 right = new Vec3(1.0, 0.0, 0.0);
        float radius = 1.0f;
        float lift = 0.0f;
        float baseRadius = 0.8f;
    }

    /**
     * Points along the crescent arc: the facing plane, centered on the
     * facing direction, at the layer's radius and height on the climb. The
     * same point set the blade arc used to draw through; here it only
     * decides where plasma spawns. Each point drifts outward along the
     * arc's radial direction as it dies.
     */
    private static MaterialEmitters.Source arc(Frame frame, float radiusMul, float yOffset) {
        return random -> {
            double phi = Math.toRadians(-SWEEP / 2.0 + random.nextDouble() * SWEEP);
            double sin = Math.sin(phi);
            double cos = Math.cos(phi);
            double r = frame.radius * radiusMul;
            Vec3 pos = new Vec3(
                frame.center.x + frame.right.x * sin * r + frame.forward.x * 0.35,
                frame.center.y + frame.lift + yOffset + cos * r,
                frame.center.z + frame.right.z * sin * r + frame.forward.z * 0.35);
            Vec3 vel = new Vec3(frame.right.x * sin * 0.03, cos * 0.03 + 0.015, frame.right.z * sin * 0.03);
            return new MaterialEmitters.Sample(pos, vel);
        };
    }

    /** Points on the expanding launch ring at the base, rolling outward. */
    private static MaterialEmitters.Source baseRing(Frame frame) {
        return random -> {
            double ang = random.nextDouble() * Math.PI * 2.0;
            double cos = Math.cos(ang);
            double sin = Math.sin(ang);
            Vec3 pos = new Vec3(frame.center.x + cos * frame.baseRadius,
                frame.center.y + 0.06, frame.center.z + sin * frame.baseRadius);
            Vec3 vel = new Vec3(cos * 0.14, 0.02, sin * 0.14);
            return new MaterialEmitters.Sample(pos, vel);
        };
    }

    /**
     * The material emitter bundle for one rise: the main plasma arc, the
     * wide wisp arc, two ghost arcs below, a vapor wash along the main
     * arc, sparse steel sparks shed off the arc, the ground dust and
     * vapor of the launch ring, and the stone chips of the complex
     * particle layer off that same ring.
     */
    private static final class RiseEmission {
        private final ClientLevel level;
        private final int seed;
        private final Frame frame = new Frame();
        private final MaterialEmitters.Emitter mainArc;
        private final MaterialEmitters.Emitter wispArc;
        private final MaterialEmitters.Emitter ghostArcLow;
        private final MaterialEmitters.Emitter ghostArcMid;
        private final MaterialEmitters.Emitter arcVapor;
        private final MaterialEmitters.Emitter arcSparks;
        private final MaterialEmitters.Emitter baseDust;
        private final MaterialEmitters.Emitter baseVapor;
        private final MaterialEmitters.Emitter baseChips;
        private float lastAge = -1.0f;
        private long lastSeenNanos = System.nanoTime();

        private RiseEmission(ClientLevel level, Vec3 center, int seed) {
            this.level = level;
            this.seed = seed;
            this.frame.center = center;
            this.mainArc = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(MAIN, 1.15f, seed, Vec3.ZERO, 0.012f),
                arc(frame, 1.0f, 0.0f), 300f, 380);
            this.wispArc = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(WISP, 1.5f, seed ^ 0xA001, Vec3.ZERO, 0.015f),
                arc(frame, 1.3f, 0.06f), 95f, 140);
            this.ghostArcMid = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(GHOST_MID, 1.2f, seed ^ 0xA002, Vec3.ZERO, 0.012f),
                arc(frame, 0.96f, -0.28f), 110f, 150);
            this.ghostArcLow = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(GHOST_LOW, 1.2f, seed ^ 0xA003, Vec3.ZERO, 0.012f),
                arc(frame, 0.92f, -0.56f), 85f, 120);
            this.arcVapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(PALE, 1.9f, seed ^ 0xA004, Vec3.ZERO, 0.02f),
                arc(frame, 1.0f, 0.0f), 50f, 80);
            this.arcSparks = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK, new Vec3(0.0, 0.05, 0.0), 0.05f),
                arc(frame, 1.0f, 0.0f), 36f, 60);
            this.baseDust = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(GHOST_LOW, 1.6f, seed ^ 0xBA5E, Vec3.ZERO, 0.03f),
                baseRing(frame), 170f, 220);
            this.baseVapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(PALE, 2.1f, seed ^ 0xBA5F, Vec3.ZERO, 0.02f),
                baseRing(frame), 60f, 90);
            // Complex particle layer, continuous half: stone chips off
            // the launch ring while it rolls outward, under the
            // continuous cap of 40 per second per emitter.
            this.baseChips = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP,
                    new Vec3(0.0, 0.28, 0.0), 0.16f),
                baseRing(frame), 26f, 36);
        }

        private void tick(float deltaSeconds, float envelope, float ringEnvelope) {
            lastSeenNanos = System.nanoTime();
            mainArc.tick(deltaSeconds, envelope);
            wispArc.tick(deltaSeconds, envelope * 0.45f);
            ghostArcMid.tick(deltaSeconds, envelope * 0.55f);
            ghostArcLow.tick(deltaSeconds, envelope * 0.35f);
            arcVapor.tick(deltaSeconds, envelope * 0.5f);
            arcSparks.tick(deltaSeconds, envelope * 0.7f);
            if (ringEnvelope > 0.01f) {
                baseDust.tick(deltaSeconds, ringEnvelope);
                baseVapor.tick(deltaSeconds, ringEnvelope);
                baseChips.tick(deltaSeconds, ringEnvelope);
            }
        }
    }

    private static float ease(float t) {
        float c = Math.max(0.0f, Math.min(1.0f, t));
        return c * c * (3.0f - 2.0f * c);
    }
}
