package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.SpikeFlailSwingEntity;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chain and spiked ball at the end of the swing. The entity sits on the ball.
 * Complex pass: layered composition. The pieces layer is a tendril
 * body through ComplexGeometry.submitEnergy with the tendril canvas,
 * chained along the swing arc exactly as sampled: one strand from
 * the first trail sample to the middle sample with the second
 * sample as its control, a second strand from the middle sample to
 * the ball with the fourth sample as its control, and a thinner
 * echo strand over the whole arc with the middle sample as its
 * control, so the body follows the same five points the emission
 * samples and tears into strands along them. The complex particle
 * layer is SPARK_STREAK sprites shed along the same trail path. The
 * material filler stays as before: air vapor and steel sparks shed
 * along the real arc, hot plasma glints at the moving head, sparse
 * vapor along the chain path, and warm dust kicked up under the low
 * points of the arc. The physical Spike Flail item model in the
 * holder's hand is untouched. Seeds derive from the entity id, so
 * every client emits the same material variation.
 */
public class SpikeFlailSwingRenderer extends EntityRenderer<SpikeFlailSwingEntity, SpikeFlailSwingRenderer.SwingState> {
    /** Trail samples behind the ball along its arc, oldest first; the last is the ball itself. */
    private static final int TRAIL_POINTS = 5;
    /** Warm steel dust, the flail's particle theme color. */
    private static final int THEME_DUST = 0xD8C9A3;

    private final Map<Integer, Emission> emissions = new HashMap<>();

    public SpikeFlailSwingRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SwingState extends EntityRenderState {
        public float age;
        public int entityId;
        public long seed;
        public boolean hasTrail;
        public Vec3 center = Vec3.ZERO;
        public Vec3 hand = Vec3.ZERO;
        public double groundY;
        public final List<Vec3> trail = new ArrayList<>(TRAIL_POINTS);
    }

    public boolean shouldRender(SpikeFlailSwingEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(SpikeFlailSwingEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public SwingState createRenderState() {
        return new SwingState();
    }

    @Override
    public void extractRenderState(SpikeFlailSwingEntity entity, SwingState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.age = entity.tickCount + partialTick;
        state.entityId = entity.getId();
        state.seed = entity.getId() * 2654435761L ^ 0x5F1A11L;
        state.center = new Vec3(state.x, state.y, state.z);
        state.hasTrail = false;
        state.trail.clear();
        Entity owner = entity.getOwner();
        if (owner != null) {
            float yaw = entity.swingYaw();
            double yawRad = Math.toRadians(yaw);
            Vec3 ball = entity.getPosition(partialTick);
            state.center = ball;
            state.hand = owner.getPosition(partialTick).add(-Math.sin(yawRad) * 0.35, 1.05, Math.cos(yawRad) * 0.35);
            state.groundY = owner.getY();
            float cur = Mth.clamp((entity.tickCount + partialTick) / (float) SpikeFlailSwingEntity.SWING_TICKS, 0.0f, 1.0f);
            for (int i = 0; i < TRAIL_POINTS; i++) {
                float p = Math.max(0.0f, cur - (TRAIL_POINTS - 1 - i) * 0.09f);
                state.trail.add(SpikeFlailSwingEntity.ballPos(owner, yaw, p));
            }
            state.hasTrail = state.trail.size() >= 2;
        }
    }

    @Override
    public void submit(SwingState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);
        if (!state.hasTrail) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc == null ? null : mc.level;
        if (level == null) {
            return;
        }
        float fade = ease(Math.max(0.0f, 1.0f - Math.max(0.0f, state.age - 8.0f) / 3.0f));
        float open = ease(Math.min(1.0f, state.age / 2.0f));
        if (fade <= 0.01f) {
            return;
        }
        float distScale = MaterialEmitters.distanceScale(state.center);
        float envelope = fade * (0.35f + 0.65f * open) * distScale;
        if (envelope <= 0.01f) {
            return;
        }
        Emission emission = emissions.get(state.entityId);
        if (emission == null || emission.level != level || emission.seed != state.seed) {
            emission = new Emission(level, (int) state.seed);
            emissions.put(state.entityId, emission);
            pruneEmissions();
        }
        emission.frame.trail.clear();
        emission.frame.trail.addAll(state.trail);
        emission.frame.hand = state.hand;
        emission.frame.center = state.center;
        emission.frame.groundY = state.groundY;
        submitPieces(state, poseStack, collector, fade * (0.35f + 0.65f * open), distScale);
        float delta = emission.delta(state.age);
        float dustEnvelope = envelope * (state.center.y - state.groundY <= 1.25 ? 1.0f : 0.30f);
        emission.tick(delta, envelope, dustEnvelope);
    }

    /**
     * Pieces layer: a tendril body in local space chained along the
     * sampled swing arc. Local points are the trail samples minus
     * the render origin (the ball, which is also the last sample).
     * The first strand runs sample 0 to sample 2 controlled by
     * sample 1, the second runs sample 2 to sample 4 controlled by
     * sample 3, and a thinner echo runs the whole arc controlled by
     * sample 2, so every strand is anchored on sampled points only
     * and no new path is invented. Peak alphas are honest body
     * alphas; the gradient runs warm dust at the tail to near white
     * at the head, matching the existing plasma glint color.
     */
    private void submitPieces(SwingState state, PoseStack poseStack, SubmitNodeCollector collector,
                              float envelope, float distScale) {
        if (envelope <= 0.01f || state.trail.size() < TRAIL_POINTS) {
            return;
        }
        final Vec3 origin = new Vec3(state.x, state.y, state.z);
        final Vec3 p0 = state.trail.get(0).subtract(origin);
        final Vec3 p1 = state.trail.get(1).subtract(origin);
        final Vec3 p2 = state.trail.get(2).subtract(origin);
        final Vec3 p3 = state.trail.get(3).subtract(origin);
        final Vec3 p4 = state.trail.get(4).subtract(origin);
        final float gEnv = envelope;
        final float gDist = distScale;
        final long gSeed = state.seed;
        ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
            ComplexGeometry.tendril(pose, buffer, gSeed ^ 0x5A11L, p0, p1, p2,
                0.30f, THEME_DUST, 0xEDE8DA, 0.92f, gEnv, gDist);
            ComplexGeometry.tendril(pose, buffer, gSeed ^ 0x5B22L, p2, p3, p4,
                0.28f, 0xEDE8DA, 0xFFF6D8, 0.92f, gEnv, gDist);
            ComplexGeometry.tendril(pose, buffer, gSeed ^ 0x5C33L, p0, p2, p4,
                0.15f, 0xEDE8DA, 0xFFF6D8, 0.88f, gEnv, gDist);
        });
    }

    private void pruneEmissions() {
        if (emissions.size() < 48) {
            return;
        }
        long cutoff = System.nanoTime() - 30_000_000_000L;
        emissions.values().removeIf(e -> e.lastSeenNanos < cutoff);
    }

    private static float ease(float t) {
        float c = Mth.clamp(t, 0.0f, 1.0f);
        return c * c * (3.0f - 2.0f * c);
    }

    /** Mutable per-frame emission data read by the sources. Data only. */
    private static final class Frame {
        final List<Vec3> trail = new ArrayList<>(TRAIL_POINTS);
        Vec3 hand = Vec3.ZERO;
        Vec3 center = Vec3.ZERO;
        double groundY;
    }

    private static final class Emission {
        final ClientLevel level;
        final long seed;
        final Frame frame = new Frame();
        final MaterialEmitters.Emitter trailVapor;
        final MaterialEmitters.Emitter trailSparks;
        final MaterialEmitters.Emitter trailStreaks;
        final MaterialEmitters.Emitter corePlasma;
        final MaterialEmitters.Emitter chainVapor;
        final MaterialEmitters.Emitter groundDust;
        float lastAge = Float.NaN;
        long lastSeenNanos = System.nanoTime();

        Emission(ClientLevel level, int seed) {
            this.level = level;
            this.seed = seed;
            MaterialEmitters.Source trailPath = random -> frame.trail.size() < 2
                ? new MaterialEmitters.Sample(frame.center, Vec3.ZERO)
                : MaterialEmitters.path(frame.trail, 0.0).sample(random);
            MaterialEmitters.Source chainPath = random ->
                MaterialEmitters.path(frame.hand, frame.center, 0.0).sample(random);
            MaterialEmitters.Source ballPoint = random -> new MaterialEmitters.Sample(frame.center, Vec3.ZERO);
            MaterialEmitters.Source ground = random ->
                MaterialEmitters.groundArea(new Vec3(frame.center.x, 0.0, frame.center.z), 0.55, frame.groundY + 0.10).sample(random);
            this.trailVapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(0xEDE8DA, 1.50f, seed ^ 0xA17, Vec3.ZERO, 0.035f),
                trailPath, 150f, 220);
            this.trailSparks = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK, Vec3.ZERO, 0.14f),
                trailPath, 65f, 100);
            // Complex particle layer: steel streak slivers shed along
            // the same sampled trail path, inside the continuous cap.
            this.trailStreaks = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.14f),
                trailPath, 26f, 34);
            this.corePlasma = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xFFF6D8, 0.65f, seed ^ 0xC09E, Vec3.ZERO, 0.050f),
                ballPoint, 70f, 110);
            this.chainVapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(0xDAD4C6, 1.05f, seed ^ 0xC4A1, Vec3.ZERO, 0.025f),
                chainPath, 55f, 90);
            this.groundDust = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(THEME_DUST, 1.10f, seed ^ 0xD057, new Vec3(0.0, 0.015, 0.0), 0.050f),
                ground, 30f, 55);
        }

        float delta(float age) {
            lastSeenNanos = System.nanoTime();
            if (Float.isNaN(lastAge) || age < lastAge) {
                lastAge = age;
                return 1.0f / 20.0f;
            }
            float delta = Math.min(0.10f, Math.max(0.0f, (age - lastAge) / 20.0f));
            lastAge = age;
            return delta;
        }

        void tick(float delta, float envelope, float dustEnvelope) {
            if (delta <= 0.0f || envelope <= 0.01f) {
                return;
            }
            trailVapor.tick(delta, envelope);
            trailSparks.tick(delta, envelope);
            trailStreaks.tick(delta, envelope);
            corePlasma.tick(delta, envelope);
            chainVapor.tick(delta, envelope * 0.80f);
            groundDust.tick(delta, dustEnvelope);
        }
    }
}
