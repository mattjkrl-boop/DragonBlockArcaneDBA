package com.dragonblockarcanedba.client.render.ki;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.KiSpiralBeamEntity;
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
 * Complex pass: layered composition, class L. The pieces layer is two
 * ComplexGeometry tendrils with opposed helical controls as the spiral
 * body: chained segments whose endpoints and controls ride the same
 * 3.5 turn winding and phase the filler emission uses, one strand in
 * the synced technique color and one white, plus a six strand
 * tendrilFan of impact lashes at the terminus, all through
 * submitEnergy with the tendril canvas. The complex particle layer is
 * SPARK_STREAK sprites at the terminus. The no-shapes emission stays
 * as filler only: a white plasma core stream along the path, wrapped
 * by technique-colored plasma whose emission points wind 3.5 turns
 * over the beam length, plus vapor winding with it and an impact plume
 * of plasma and smoke at the terminus.
 */
public class KiSpiralBeamRenderer extends EntityRenderer<KiSpiralBeamEntity, KiSpiralBeamRenderer.SpiralBeamRenderState> {

    public KiSpiralBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public SpiralBeamRenderState createRenderState() {
        return new SpiralBeamRenderState();
    }

    @Override
    public void extractRenderState(KiSpiralBeamEntity entity, SpiralBeamRenderState state, float partialTicks) {
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
        Vec3 u = new Vec3(1, 0, 0);
        Vec3 v = new Vec3(0, 1, 0);
        float length = 1f;
        float helixRadius = 0.28f;
        float phase = 0f;
    }

    private static final class Emission {
        final Frame frame = new Frame();
        MaterialEmitters.Emitter[] emitters = new MaterialEmitters.Emitter[0];
        float lastAge = -1f;
    }

    private final Map<Long, Emission> emissions = new HashMap<>();

    private static MaterialEmitters.Source beamPath(Frame frame) {
        return random -> {
            double t = random.nextDouble();
            Vec3 pos = frame.from.add(frame.to.subtract(frame.from).scale(t));
            return new MaterialEmitters.Sample(pos, frame.dir.scale(0.05));
        };
    }

    /** Points winding around the path: the spiral as emission data. */
    private static MaterialEmitters.Source helix(Frame frame) {
        return random -> {
            double t = random.nextDouble();
            double ang = frame.phase + t * Math.PI * 2.0 * 3.5;
            double rr = frame.helixRadius * (0.75 + random.nextDouble() * 0.45);
            Vec3 offset = frame.u.scale(Math.cos(ang) * rr).add(frame.v.scale(Math.sin(ang) * rr));
            Vec3 pos = frame.from.add(frame.dir.scale(t * frame.length)).add(offset);
            Vec3 tangent = frame.u.scale(-Math.sin(ang)).add(frame.v.scale(Math.cos(ang)));
            return new MaterialEmitters.Sample(pos, frame.dir.scale(0.05).add(tangent.scale(0.05)));
        };
    }

    private static Vec3 lookDir(float yRot, float xRot) {
        double yaw = Math.toRadians(yRot);
        double pitch = Math.toRadians(xRot);
        double cosP = Math.cos(pitch);
        return new Vec3(-Math.sin(yaw) * cosP, -Math.sin(pitch), Math.cos(yaw) * cosP);
    }

    private static void updateFrame(Frame frame, SpiralBeamRenderState state) {
        frame.from = new Vec3(state.wx, state.wy, state.wz);
        frame.dir = lookDir(state.yRot, state.xRot);
        frame.to = frame.from.add(frame.dir.scale(state.length));
        frame.length = state.length;
        frame.helixRadius = 0.28f * Math.max(0.2f, state.scale);
        frame.phase = state.ageInTicks * 0.35f;
        Vec3 u = frame.dir.cross(new Vec3(0, 1, 0));
        if (u.lengthSqr() < 1e-6) u = new Vec3(1, 0, 0); else u = u.normalize();
        frame.u = u;
        frame.v = u.cross(frame.dir).normalize();
    }

    private Emission buildEmission(ClientLevel level, SpiralBeamRenderState state) {
        Emission emission = new Emission();
        updateFrame(emission.frame, state);
        Frame frame = emission.frame;
        int seed = state.seed;
        float scale = Math.max(0.2f, state.scale);
        float pathRate = Math.min(1000f, Math.max(300f, state.length * 13.0f));
        emission.emitters = new MaterialEmitters.Emitter[]{
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(0xFFFFFF, 1.35f * scale, seed,
                                Vec3.ZERO, 0.02f),
                        beamPath(frame), pathRate * 0.55f, 700),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(state.color, 2.5f * scale, seed ^ 0x0E111,
                                Vec3.ZERO, 0.03f),
                        beamPath(frame), pathRate * 0.45f, 600),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(state.color, 2.1f * scale, seed ^ 0x0CA11,
                                Vec3.ZERO, 0.02f),
                        helix(frame), 620f, 760),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.vapor(state.color, 2.9f * scale, seed ^ 0x0BEEF,
                                Vec3.ZERO, 0.03f),
                        helix(frame), 110f, 150),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(state.color, 2.6f * scale, seed ^ 0x01AC7,
                                Vec3.ZERO, 0.16f),
                        random -> new MaterialEmitters.Sample(frame.to, frame.dir.scale(0.22)), 240f, 320),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.smoke(state.color, 2.8f * scale, seed ^ 0x05107,
                                new Vec3(0.0, 0.05, 0.0), 0.07f),
                        random -> new MaterialEmitters.Sample(frame.to, frame.dir.scale(0.10)), 50f, 75),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.26f),
                        random -> new MaterialEmitters.Sample(frame.to, frame.dir.scale(0.26)), 30f, 40),
        };
        return emission;
    }

    @Override
    public void submit(SpiralBeamRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
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
        // beam origin). Vertex count at distScale 1.0: the spiral body
        // is at most four segments times two strands times 384 vertices
        // (3072), plus the impact fan at six tendrils times 384 (2304),
        // for 5376 of the class L budget of 6000. Helix offsets use the
        // frame winding: 3.5 turns over the length, phase from age, the
        // same values the filler helix source uses.
        if (envelope > 0.01f) {
            Vec3 dir = lookDir(state.yRot, state.xRot);
            Vec3 uTmp = dir.cross(new Vec3(0, 1, 0));
            if (uTmp.lengthSqr() < 1e-6) uTmp = new Vec3(1, 0, 0); else uTmp = uTmp.normalize();
            final Vec3 geoU = uTmp;
            final Vec3 geoV = geoU.cross(dir).normalize();
            final Vec3 geoDir = dir;
            final Vec3 geoEnd = dir.scale(state.length);
            final long geoSeed = state.seed;
            final int geoColor = state.color;
            final float geoEnv = envelope;
            final float geoDs = distScale;
            final float geoScale = Math.max(0.2f, state.scale);
            final float helixR = 0.28f * geoScale;
            final float phase = state.ageInTicks * 0.35f;
            final float geoLength = state.length;
            final int segs = state.length <= 20f ? 3 : 4;
            ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
                for (int strand = 0; strand < 2; strand++) {
                    float sign = strand == 0 ? 1f : -1f;
                    int root = strand == 0 ? geoColor : 0xFFFFFF;
                    int tip = strand == 0 ? 0xFFFFFF : geoColor;
                    for (int i = 0; i < segs; i++) {
                        float t0 = i / (float) segs;
                        float t1 = (i + 1) / (float) segs;
                        float tm = (t0 + t1) * 0.5f;
                        double a0 = phase + t0 * Math.PI * 2.0 * 3.5;
                        double am = phase + tm * Math.PI * 2.0 * 3.5;
                        double a1 = phase + t1 * Math.PI * 2.0 * 3.5;
                        Vec3 off0 = geoU.scale(Math.cos(a0) * helixR * sign)
                                .add(geoV.scale(Math.sin(a0) * helixR * sign));
                        Vec3 offM = geoU.scale(Math.cos(am) * helixR * sign)
                                .add(geoV.scale(Math.sin(am) * helixR * sign));
                        Vec3 off1 = geoU.scale(Math.cos(a1) * helixR * sign)
                                .add(geoV.scale(Math.sin(a1) * helixR * sign));
                        Vec3 segFrom = geoDir.scale(t0 * geoLength).add(off0);
                        Vec3 segControl = geoDir.scale(tm * geoLength).add(offM);
                        Vec3 segTo = geoDir.scale(t1 * geoLength).add(off1);
                        ComplexGeometry.tendril(pose, buffer,
                                geoSeed ^ (0x5F1A1L + strand * 0x101L + i * 7919L),
                                segFrom, segControl, segTo, 0.30f * geoScale,
                                root, tip, 0.93f, geoEnv, geoDs);
                    }
                }
                ComplexGeometry.tendrilFan(pose, buffer, geoSeed ^ 0xFA11L,
                        geoEnd, geoDir, 56.0f, 2.4f * geoScale, 6,
                        0.32f * geoScale, geoColor, 0xFFFFFF, 0.92f, geoEnv, geoDs);
            });
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    public static class SpiralBeamRenderState extends EntityRenderState {
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
