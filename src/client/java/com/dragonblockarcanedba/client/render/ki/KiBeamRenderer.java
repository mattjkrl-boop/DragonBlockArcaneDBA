package com.dragonblockarcanedba.client.render.ki;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.KiBeamEntity;
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
 * Complex pass: layered composition, class L. The pieces layer is a
 * chained two strand ComplexGeometry tendril core along the beam path
 * plus a six strand tendrilFan of impact lashes at the terminus, both
 * through submitEnergy with the tendril canvas in the synced technique
 * color. The complex particle layer is SPARK_STREAK sprites at the
 * terminus. The no-shapes emission stays as filler only: dense
 * technique-colored plasma emitted along the beam path, a brighter white
 * plasma concentration near the caster, vapor drifting off the path, and
 * an impact plume of plasma and smoke where the beam ends. Lifecycle
 * fade (in over the first ticks, out across the 20 tick life) is
 * unchanged from the bespoke build.
 */
public class KiBeamRenderer extends EntityRenderer<KiBeamEntity, KiBeamRenderer.BeamRenderState> {

    public KiBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public BeamRenderState createRenderState() {
        return new BeamRenderState();
    }

    @Override
    public void extractRenderState(KiBeamEntity entity, BeamRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.color = entity.getColor();
        state.length = entity.getLength();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.ageInTicks = entity.tickCount + partialTicks;
        state.scale = entity.getScale();
        state.seed = entity.getSeed();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    /** Mutable per-frame emission data read by the sources. Data only. */
    private static final class Frame {
        Vec3 from = Vec3.ZERO;
        Vec3 to = Vec3.ZERO;
        Vec3 dir = Vec3.ZERO;
    }

    private static final class Emission {
        final Frame frame = new Frame();
        MaterialEmitters.Emitter[] emitters = new MaterialEmitters.Emitter[0];
        float lastAge = -1f;
    }

    private final Map<Long, Emission> emissions = new HashMap<>();

    /** Points along the beam path between two frame-driven fractions. */
    private static MaterialEmitters.Source beamPath(Frame frame, double t0, double t1) {
        return random -> {
            double t = t0 + random.nextDouble() * (t1 - t0);
            Vec3 pos = frame.from.add(frame.to.subtract(frame.from).scale(t));
            return new MaterialEmitters.Sample(pos, frame.dir.scale(0.05));
        };
    }

    private static Vec3 lookDir(float yRot, float xRot) {
        double yaw = Math.toRadians(yRot);
        double pitch = Math.toRadians(xRot);
        double cosP = Math.cos(pitch);
        return new Vec3(-Math.sin(yaw) * cosP, -Math.sin(pitch), Math.cos(yaw) * cosP);
    }

    private Emission buildEmission(ClientLevel level, BeamRenderState state) {
        Emission emission = new Emission();
        Frame frame = emission.frame;
        frame.from = new Vec3(state.wx, state.wy, state.wz);
        frame.dir = lookDir(state.yRot, state.xRot);
        frame.to = frame.from.add(frame.dir.scale(state.length));
        int seed = state.seed;
        float scale = Math.max(0.2f, state.scale);
        float pathRate = Math.min(1150f, Math.max(320f, state.length * 15.0f));
        emission.emitters = new MaterialEmitters.Emitter[]{
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(state.color, 3.4f * scale, seed,
                                Vec3.ZERO, 0.045f),
                        beamPath(frame, 0.0, 1.0), pathRate, 1300),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(0xFFFFFF, 1.9f * scale, seed ^ 0xC0FE,
                                Vec3.ZERO, 0.02f),
                        beamPath(frame, 0.0, 0.28), 300f, 380),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.vapor(state.color, 3.2f * scale, seed ^ 0x0BEEF,
                                Vec3.ZERO, 0.05f),
                        beamPath(frame, 0.0, 1.0), 130f, 180),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(state.color, 2.6f * scale, seed ^ 0x01AC7,
                                Vec3.ZERO, 0.16f),
                        random -> new MaterialEmitters.Sample(frame.to, frame.dir.scale(0.22)), 260f, 340),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.smoke(state.color, 2.8f * scale, seed ^ 0x05107,
                                new Vec3(0.0, 0.05, 0.0), 0.07f),
                        random -> new MaterialEmitters.Sample(frame.to, frame.dir.scale(0.10)), 55f, 80),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.26f),
                        random -> new MaterialEmitters.Sample(frame.to, frame.dir.scale(0.26)), 30f, 40),
        };
        return emission;
    }

    @Override
    public void submit(BeamRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float life = Math.min(1f, state.ageInTicks / 20f);
        float alpha = 1f - life * life;
        float distScale = FxEnvelope.distScale(Math.sqrt(state.distanceToCameraSq));
        float envelope = alpha * distScale;

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
            emission.frame.from = new Vec3(state.wx, state.wy, state.wz);
            emission.frame.dir = lookDir(state.yRot, state.xRot);
            emission.frame.to = emission.frame.from.add(emission.frame.dir.scale(state.length));
            float delta = emission.lastAge < 0f ? 0.05f
                    : Math.min(0.05f, Math.max(0.0f, (state.ageInTicks - emission.lastAge) / 20.0f));
            emission.lastAge = state.ageInTicks;
            for (MaterialEmitters.Emitter emitter : emission.emitters) {
                emitter.tick(delta, envelope);
            }
        }

        // Pieces layer, in entity local space (the pose origin is the
        // beam origin). Vertex count at distScale 1.0: the core chain is
        // at most four segments times two strands times 384 vertices
        // (3072), plus the impact fan at six tendrils times 384 (2304),
        // for 5376 of the class L budget of 6000.
        if (envelope > 0.01f) {
            Vec3 dir = lookDir(state.yRot, state.xRot);
            Vec3 perpTmp = dir.cross(new Vec3(0, 1, 0));
            if (perpTmp.lengthSqr() < 1e-6) perpTmp = new Vec3(1, 0, 0);
            final Vec3 perpA = perpTmp.normalize();
            final Vec3 perpB = dir.cross(perpA).normalize();
            final Vec3 geoEnd = dir.scale(state.length);
            final Vec3 geoDir = dir;
            final long geoSeed = state.seed;
            final int geoColor = state.color;
            final float geoEnv = envelope;
            final float geoDs = distScale;
            final float geoScale = Math.max(0.2f, state.scale);
            final int coreSegments = state.length <= 12f ? 2 : state.length <= 24f ? 3 : 4;
            ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
                for (int strand = 0; strand < 2; strand++) {
                    float strandWidth = (strand == 0 ? 0.58f : 0.30f) * geoScale;
                    int strandRoot = strand == 0 ? 0xFFFFFF : geoColor;
                    int strandTip = strand == 0 ? geoColor : 0xFFFFFF;
                    float strandAlpha = strand == 0 ? 0.95f : 0.90f;
                    Vec3 strandOffset = perpA.scale(strand == 0 ? 0.0 : 0.15 * geoScale)
                            .add(perpB.scale(strand == 0 ? 0.0 : -0.10 * geoScale));
                    for (int i = 0; i < coreSegments; i++) {
                        float t0 = i / (float) coreSegments;
                        float t1 = (i + 1) / (float) coreSegments;
                        Vec3 segFrom = geoEnd.scale(t0).add(strandOffset);
                        Vec3 segTo = geoEnd.scale(t1).add(strandOffset);
                        float bowSign = (i % 2 == 0 ? 1.0f : -1.0f) * (strand == 0 ? 1.0f : -1.0f);
                        Vec3 segControl = segFrom.lerp(segTo, 0.5)
                                .add(perpA.scale(bowSign * 0.22 * geoScale))
                                .add(perpB.scale(bowSign * 0.10 * geoScale));
                        ComplexGeometry.tendril(pose, buffer,
                                geoSeed ^ (0xBEA1L + strand * 0x101L + i * 7919L),
                                segFrom, segControl, segTo, strandWidth,
                                strandRoot, strandTip, strandAlpha, geoEnv, geoDs);
                    }
                }
                ComplexGeometry.tendrilFan(pose, buffer, geoSeed ^ 0xFA11L,
                        geoEnd, geoDir, 56.0f, 2.4f * geoScale, 6,
                        0.32f * geoScale, geoColor, 0xFFFFFF, 0.92f, geoEnv, geoDs);
            });
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    public static class BeamRenderState extends EntityRenderState {
        public int color = 0xFFFFFF;
        public float length = 50.0f;
        public float yRot = 0;
        public float xRot = 0;
        public float ageInTicks = 0;
        public float scale = 1.0f;
        public int seed = 0;
        public double wx = 0;
        public double wy = 0;
        public double wz = 0;
    }
}
