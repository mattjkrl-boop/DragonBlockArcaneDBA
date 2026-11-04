package com.dragonblockarcanedba.client.render.ki;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.KiBlastEntity;
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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Complex pass: layered composition, class M. The pieces layer is a
 * tendril wake behind the bolt: a main ComplexGeometry strand in the
 * synced technique color running from the tail to a white tip at the
 * nose, plus a thinner offset strand as its torn edge, both through
 * submitEnergy with the tendril canvas. The complex particle layer is
 * SPARK_STREAK sprites shed at the nose; the renderer state carries no
 * synced impact field, so the streaks stream continuously at the head
 * rather than bursting, and the entity's own server burst stays the
 * impact event. The no-shapes emission stays as filler only: a dense
 * cluster of technique-colored plasma around the bolt position, a
 * smaller white-hot cluster at its heart, and a material trail of
 * plasma and vapor streaming off the tail as it flies. The old pulse
 * (1 + 0.12 sin of age) still drives the cluster radius.
 */
public class KiBlastRenderer extends EntityRenderer<KiBlastEntity, KiBlastRenderer.BlastRenderState> {

    public KiBlastRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class BlastRenderState extends EntityRenderState {
        public int color = 0xFFFFFF;
        public float ageInTicks = 0;
        public float yRot = 0;
        public float xRot = 0;
        public float scale = 1.0f;
        public int seed = 0;
        public double wx = 0;
        public double wy = 0;
        public double wz = 0;
    }

    @Override
    public BlastRenderState createRenderState() {
        return new BlastRenderState();
    }

    @Override
    public void extractRenderState(KiBlastEntity entity, BlastRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.color = entity.getColor();
        state.ageInTicks = entity.tickCount + partialTicks;
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.scale = entity.getScale();
        state.seed = entity.getSeed();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    /** Mutable per-frame emission data read by the sources. Data only. */
    private static final class Frame {
        Vec3 center = Vec3.ZERO;
        Vec3 dir = Vec3.ZERO;
        float radius = 0.2f;
        float tail = 1.6f;
    }

    private static final class Emission {
        final Frame frame = new Frame();
        MaterialEmitters.Emitter[] emitters = new MaterialEmitters.Emitter[0];
        float lastAge = -1f;
    }

    private final Map<Long, Emission> emissions = new HashMap<>();

    /** Points inside a small sphere around the bolt heart. */
    private static MaterialEmitters.Source cluster(Frame frame, double radiusFactor) {
        return random -> {
            double u = random.nextDouble() * 2.0 - 1.0;
            double ang = random.nextDouble() * Math.PI * 2.0;
            double rr = frame.radius * radiusFactor * Math.cbrt(random.nextDouble());
            double flat = Math.sqrt(Math.max(0.0, 1.0 - u * u)) * rr;
            Vec3 pos = new Vec3(frame.center.x + Math.cos(ang) * flat,
                    frame.center.y + u * rr, frame.center.z + Math.sin(ang) * flat);
            return new MaterialEmitters.Sample(pos, frame.dir.scale(0.03));
        };
    }

    /** Points along the wake behind the bolt, widening toward the tail. */
    private static MaterialEmitters.Source trail(Frame frame) {
        return random -> {
            double t = random.nextDouble();
            Vec3 base = frame.center.add(frame.dir.scale(-(0.25 + t * frame.tail)));
            double jitter = frame.radius * (0.35 + t * 0.85);
            Vec3 pos = base.add(new Vec3(
                    (random.nextDouble() * 2.0 - 1.0) * jitter,
                    (random.nextDouble() * 2.0 - 1.0) * jitter,
                    (random.nextDouble() * 2.0 - 1.0) * jitter));
            return new MaterialEmitters.Sample(pos, frame.dir.scale(-0.03));
        };
    }

    private static Vec3 lookDir(float yRot, float xRot) {
        double yaw = Math.toRadians(yRot);
        double pitch = Math.toRadians(xRot);
        double cosP = Math.cos(pitch);
        return new Vec3(-Math.sin(yaw) * cosP, -Math.sin(pitch), Math.cos(yaw) * cosP);
    }

    private static void updateFrame(Frame frame, BlastRenderState state) {
        frame.center = new Vec3(state.wx, state.wy, state.wz);
        frame.dir = lookDir(state.yRot, state.xRot);
        float pulse = 1.0f + 0.12f * Mth.sin(state.ageInTicks * 0.9f);
        float scale = Math.max(0.2f, state.scale);
        frame.radius = 0.20f * pulse * scale;
        frame.tail = 1.6f * scale;
    }

    private Emission buildEmission(ClientLevel level, BlastRenderState state) {
        Emission emission = new Emission();
        updateFrame(emission.frame, state);
        Frame frame = emission.frame;
        int seed = state.seed;
        float scale = Math.max(0.2f, state.scale);
        emission.emitters = new MaterialEmitters.Emitter[]{
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(state.color, 2.3f * scale, seed,
                                Vec3.ZERO, 0.02f),
                        cluster(frame, 1.15), 400f, 520),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(0xFFFFFF, 1.25f * scale, seed ^ 0x0C0FE,
                                Vec3.ZERO, 0.01f),
                        cluster(frame, 0.55), 230f, 300),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(state.color, 1.7f * scale, seed ^ 0x07A11,
                                Vec3.ZERO, 0.02f),
                        trail(frame), 320f, 420),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.vapor(state.color, 2.4f * scale, seed ^ 0x0BEEF,
                                Vec3.ZERO, 0.03f),
                        trail(frame), 75f, 105),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.14f),
                        cluster(frame, 0.7), 20f, 40),
        };
        return emission;
    }

    @Override
    public void submit(BlastRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float distScale = FxEnvelope.distScale(Math.sqrt(state.distanceToCameraSq));
        float envelope = distScale;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc != null ? mc.level : null;
        if (level != null && envelope > 0.01f) {
            long key = ((long) state.seed << 32) ^ (state.color & 0xFFFFFFFFL);
            Emission emission = emissions.get(key);
            if (emission == null || state.ageInTicks < emission.lastAge - 1.0f) {
                emission = buildEmission(level, state);
                emissions.put(key, emission);
                if (emissions.size() > 64) emissions.clear();
            }
            updateFrame(emission.frame, state);
            float delta = emission.lastAge < 0f ? 0.05f
                    : Math.min(0.05f, Math.max(0.0f, (state.ageInTicks - emission.lastAge) / 20.0f));
            emission.lastAge = state.ageInTicks;
            for (MaterialEmitters.Emitter emitter : emission.emitters) {
                emitter.tick(delta, envelope);
            }
        }

        // Pieces layer, in entity local space (the pose origin is the
        // bolt center). Two tendrils at full spine samples is 768
        // vertices, inside the class M budget of 2500. The wake follows
        // the same tail length the filler trail uses.
        if (envelope > 0.01f) {
            Vec3 dir = lookDir(state.yRot, state.xRot);
            Vec3 perpTmp = dir.cross(new Vec3(0, 1, 0));
            if (perpTmp.lengthSqr() < 1e-6) perpTmp = new Vec3(1, 0, 0);
            final Vec3 perpA = perpTmp.normalize();
            final Vec3 perpB = dir.cross(perpA).normalize();
            final float geoScale = Math.max(0.2f, state.scale);
            final float tail = 1.6f * geoScale;
            final long geoSeed = state.seed;
            final int geoColor = state.color;
            final float geoEnv = envelope;
            final float geoDs = distScale;
            final Vec3 wakeFrom = dir.scale(-(0.25f + tail));
            final Vec3 wakeControl = dir.scale(-tail * 0.45f).add(perpA.scale(0.20f * geoScale));
            final Vec3 wakeTo = dir.scale(0.12f);
            final Vec3 edgeFrom = dir.scale(-(0.20f + tail * 0.78f)).add(perpA.scale(0.13f * geoScale));
            final Vec3 edgeControl = dir.scale(-tail * 0.35f).add(perpB.scale(-0.16f * geoScale));
            final Vec3 edgeTo = dir.scale(0.05f).add(perpA.scale(-0.08f * geoScale));
            ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
                ComplexGeometry.tendril(pose, buffer, geoSeed ^ 0x7A11L,
                        wakeFrom, wakeControl, wakeTo, 0.32f * geoScale,
                        geoColor, 0xFFFFFF, 0.95f, geoEnv, geoDs);
                ComplexGeometry.tendril(pose, buffer, geoSeed ^ 0xED6EL,
                        edgeFrom, edgeControl, edgeTo, 0.17f * geoScale,
                        geoColor, 0xFFFFFF, 0.88f, geoEnv, geoDs);
            });
        }

        super.submit(state, poseStack, collector, cameraState);
    }
}
