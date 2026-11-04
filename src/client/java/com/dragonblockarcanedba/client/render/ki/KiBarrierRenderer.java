package com.dragonblockarcanedba.client.render.ki;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.KiBarrierEntity;
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
 * Complex pass: layered composition, class L. The pieces layer is six
 * vertical ComplexGeometry tendril lashes standing on the barrier
 * boundary at full spine samples, bowed outward through the equator and
 * turning with the standing rim rate, drawn through submitEnergy with
 * the tendril canvas in the synced technique color running to the glow
 * tint at the tips. The complex particle layer is WISP_CURL sprites
 * drifting in the volume. The no-shapes emission stays as filler only:
 * a volume of technique-colored plasma motes drifting inside the old
 * shell radius, a sparse film of large vapor and plasma sprites sampled
 * on the boundary surface so the edge reads as eroded energy rather
 * than a surface, and two bright rims carried as ring emissions, one
 * flat around the equator and one standing in a vertical plane, both
 * spinning at the old rates (spin, and spin times 1.7). The radius still
 * breathes (0.5 plus 0.5 sin of age times 0.22, plus 2 percent) and still
 * grows with the used percent exactly as the bespoke shell did. Hit
 * feedback stays where it already lived: the entity's own absorb burst.
 * Seeded per cast from the synced seed, so every barrier churns
 * differently.
 */
public class KiBarrierRenderer extends EntityRenderer<KiBarrierEntity, KiBarrierRenderer.BarrierRenderState> {

    public KiBarrierRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public BarrierRenderState createRenderState() {
        return new BarrierRenderState();
    }

    @Override
    public void extractRenderState(KiBarrierEntity entity, BarrierRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.color = entity.getColor();
        state.ageInTicks = entity.tickCount + partialTicks;
        int percent = entity.getUsedPercent();
        if (percent < 1) {
            percent = 1;
        }
        if (percent > 100) {
            percent = 100;
        }
        state.usedPercent = percent;
        state.seed = entity.getSeed();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    /** Mutable per-frame emission data read by the sources. Data only. */
    private static final class Frame {
        Vec3 center = Vec3.ZERO;
        float radius = 1.3f;
        float vertPhi = 0f;
    }

    private static final class Emission {
        final Frame frame = new Frame();
        MaterialEmitters.Emitter[] emitters = new MaterialEmitters.Emitter[0];
        float lastAge = -1f;
    }

    private final Map<Long, Emission> emissions = new HashMap<>();

    /** Points inside the barrier volume, following the frame center. */
    private static MaterialEmitters.Source moteVolume(Frame frame) {
        return random -> {
            double u = random.nextDouble() * 2.0 - 1.0;
            double ang = random.nextDouble() * Math.PI * 2.0;
            double rr = frame.radius * 0.9 * Math.cbrt(random.nextDouble());
            double flat = Math.sqrt(Math.max(0.0, 1.0 - u * u)) * rr;
            Vec3 pos = new Vec3(frame.center.x + Math.cos(ang) * flat,
                    frame.center.y + u * rr, frame.center.z + Math.sin(ang) * flat);
            return new MaterialEmitters.Sample(pos, Vec3.ZERO);
        };
    }

    /** Points on the barrier boundary surface, drifting slowly outward. */
    private static MaterialEmitters.Source boundary(Frame frame, double rMin, double rMax) {
        return random -> {
            double u = random.nextDouble() * 2.0 - 1.0;
            double ang = random.nextDouble() * Math.PI * 2.0;
            double flat = Math.sqrt(Math.max(0.0, 1.0 - u * u));
            Vec3 dirv = new Vec3(Math.cos(ang) * flat, u, Math.sin(ang) * flat);
            double rr = frame.radius * (rMin + random.nextDouble() * (rMax - rMin));
            return new MaterialEmitters.Sample(frame.center.add(dirv.scale(rr)), dirv.scale(0.012));
        };
    }

    /** Points on the flat equatorial rim, born moving with the spin. */
    private static MaterialEmitters.Source flatRim(Frame frame) {
        return random -> {
            double ang = random.nextDouble() * Math.PI * 2.0;
            double rr = frame.radius * 0.97;
            Vec3 pos = new Vec3(frame.center.x + Math.cos(ang) * rr,
                    frame.center.y + (random.nextDouble() * 2.0 - 1.0) * 0.03,
                    frame.center.z + Math.sin(ang) * rr);
            Vec3 vel = new Vec3(-Math.sin(ang), 0.0, Math.cos(ang)).scale(0.07);
            return new MaterialEmitters.Sample(pos, vel);
        };
    }

    /** Points on the standing rim, in the vertical plane at the frame angle. */
    private static MaterialEmitters.Source verticalRim(Frame frame) {
        return random -> {
            double a = random.nextDouble() * Math.PI * 2.0;
            double rr = frame.radius * 0.97;
            double cp = Math.cos(frame.vertPhi);
            double sp = Math.sin(frame.vertPhi);
            Vec3 radial = new Vec3(Math.cos(a) * cp, Math.sin(a), Math.cos(a) * sp);
            Vec3 tangent = new Vec3(-Math.sin(a) * cp, Math.cos(a), -Math.sin(a) * sp);
            return new MaterialEmitters.Sample(frame.center.add(radial.scale(rr)), tangent.scale(0.07));
        };
    }

    private static int glowColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        r += (int) ((255 - r) * 0.7f);
        g += (int) ((255 - g) * 0.7f);
        b += (int) ((255 - b) * 0.7f);
        return (r << 16) | (g << 8) | b;
    }

    private static void updateFrame(Frame frame, BarrierRenderState state) {
        float breathe = 0.5f + 0.5f * Mth.sin(state.ageInTicks * 0.22f);
        frame.radius = (1.02f + 0.28f * (state.usedPercent / 100.0f)) * (1.0f + 0.02f * breathe);
        frame.center = new Vec3(state.wx, state.wy + 0.95, state.wz);
        float spinDeg = state.ageInTicks * 2.4f;
        frame.vertPhi = (float) Math.toRadians(spinDeg * 1.7f);
    }

    private Emission buildEmission(ClientLevel level, BarrierRenderState state) {
        Emission emission = new Emission();
        updateFrame(emission.frame, state);
        Frame frame = emission.frame;
        int seed = state.seed;
        int glow = glowColor(state.color);
        emission.emitters = new MaterialEmitters.Emitter[]{
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(state.color, 1.15f, seed,
                                Vec3.ZERO, 0.008f),
                        moteVolume(frame), 150f, 210),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.vapor(glow, 3.4f, seed ^ 0x2B3C,
                                Vec3.ZERO, 0.004f),
                        boundary(frame, 0.94, 1.04), 42f, 60),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(state.color, 2.3f, seed ^ 0x77AA,
                                Vec3.ZERO, 0.006f),
                        boundary(frame, 0.96, 1.02), 75f, 105),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(0xFFFFFF, 1.5f, seed ^ 0x99,
                                Vec3.ZERO, 0.01f),
                        flatRim(frame), 115f, 160),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(glow, 1.7f, seed ^ 0xB7,
                                Vec3.ZERO, 0.01f),
                        verticalRim(frame), 95f, 135),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.fixed(DbaParticles.WISP_CURL, Vec3.ZERO, 0.008f),
                        moteVolume(frame), 24f, 40),
        };
        return emission;
    }

    @Override
    public void submit(BarrierRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float breathe = 0.5f + 0.5f * Mth.sin(state.ageInTicks * 0.22f);
        float distScale = FxEnvelope.distScale(Math.sqrt(state.distanceToCameraSq));
        float envelope = distScale * (0.7f + 0.3f * breathe);

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
        // barrier entity origin, and the barrier center sits 0.95 above
        // it, matching the filler frame). Six vertical lashes at full
        // spine samples is 2304 vertices, one builder at its full
        // segment and count budget, inside the class L budget of 6000.
        // The radius math is the filler frame math: breathe and used
        // percent growth unchanged.
        if (envelope > 0.01f) {
            float geoRadius = (1.02f + 0.28f * (state.usedPercent / 100.0f)) * (1.0f + 0.02f * breathe);
            final float r = geoRadius;
            final float turn = state.ageInTicks * 0.0712f;
            final long geoSeed = state.seed;
            final int geoColor = state.color;
            final int geoGlow = glowColor(state.color);
            final float geoEnv = envelope;
            final float geoDs = distScale;
            final float h0 = -0.52f * r;
            final float h1 = 0.62f * r;
            final float flat0 = (float) Math.sqrt(Math.max(0.05f, r * r - h0 * h0));
            final float flat1 = (float) Math.sqrt(Math.max(0.05f, r * r - h1 * h1));
            ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
                for (int i = 0; i < 6; i++) {
                    double ang = turn + i * (Math.PI * 2.0 / 6.0);
                    double cos = Math.cos(ang);
                    double sin = Math.sin(ang);
                    Vec3 base = new Vec3(cos * flat0, 0.95 + h0, sin * flat0);
                    Vec3 control = new Vec3(cos * r * 1.12, 0.95 + 0.05 * r, sin * r * 1.12);
                    Vec3 top = new Vec3(cos * flat1 * 0.97, 0.95 + h1, sin * flat1 * 0.97);
                    ComplexGeometry.tendril(pose, buffer, geoSeed ^ (0xBA11L + i * 7919L),
                            base, control, top, 0.16f,
                            geoColor, geoGlow, 0.90f, geoEnv, geoDs);
                }
            });
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    public static class BarrierRenderState extends EntityRenderState {
        public int color = 0xFF55DDFF;
        public float ageInTicks;
        public int usedPercent = 50;
        public int seed = 0;
        public double wx = 0;
        public double wy = 0;
        public double wz = 0;
    }
}
