package com.dragonblockarcanedba.client.render.ki;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.KiDiskEntity;
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
 * No-shapes pass: this renderer draws no geometry and no solid disc. A ki
 * disk is a spinning cloud of technique-colored plasma sampled across the
 * disc plane as data: denser and whiter toward the rim, shedding embers
 * off the rim as it flies, with vapor bleeding off the faces. The spin
 * lives in the tangential velocity the samples are born with (the old
 * 24 degrees per tick rotation), so the cloud itself wheels.
 */
public class KiDiskRenderer extends EntityRenderer<KiDiskEntity, KiDiskRenderer.DiskRenderState> {

    public KiDiskRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public DiskRenderState createRenderState() {
        return new DiskRenderState();
    }

    @Override
    public void extractRenderState(KiDiskEntity entity, DiskRenderState state, float partialTicks) {
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
        Vec3 u = new Vec3(1, 0, 0);
        Vec3 v = new Vec3(0, 1, 0);
        float radius = 0.72f;
    }

    private static final class Emission {
        final Frame frame = new Frame();
        MaterialEmitters.Emitter[] emitters = new MaterialEmitters.Emitter[0];
        float lastAge = -1f;
    }

    private final Map<Long, Emission> emissions = new HashMap<>();

    /** Points across the disc plane, born with tangential spin velocity. */
    private static MaterialEmitters.Source discCloud(Frame frame, double rMin, double rMax) {
        return random -> {
            double ang = random.nextDouble() * Math.PI * 2.0;
            double rr = frame.radius * (rMin + (rMax - rMin) * Math.sqrt(random.nextDouble()));
            Vec3 radial = frame.u.scale(Math.cos(ang)).add(frame.v.scale(Math.sin(ang)));
            Vec3 tangent = frame.u.scale(-Math.sin(ang)).add(frame.v.scale(Math.cos(ang)));
            Vec3 pos = frame.center.add(radial.scale(rr));
            Vec3 vel = tangent.scale(0.10 + rr * 0.55).add(frame.dir.scale(0.02));
            return new MaterialEmitters.Sample(pos, vel);
        };
    }

    /** Points at the rim, shed outward with spin. */
    private static MaterialEmitters.Source rim(Frame frame) {
        return random -> {
            double ang = random.nextDouble() * Math.PI * 2.0;
            double rr = frame.radius * (0.88 + random.nextDouble() * 0.16);
            Vec3 radial = frame.u.scale(Math.cos(ang)).add(frame.v.scale(Math.sin(ang)));
            Vec3 tangent = frame.u.scale(-Math.sin(ang)).add(frame.v.scale(Math.cos(ang)));
            Vec3 pos = frame.center.add(radial.scale(rr));
            Vec3 vel = radial.scale(0.10).add(tangent.scale(0.30)).add(frame.dir.scale(-0.04));
            return new MaterialEmitters.Sample(pos, vel);
        };
    }

    private static Vec3 lookDir(float yRot, float xRot) {
        double yaw = Math.toRadians(yRot);
        double pitch = Math.toRadians(xRot);
        double cosP = Math.cos(pitch);
        return new Vec3(-Math.sin(yaw) * cosP, -Math.sin(pitch), Math.cos(yaw) * cosP);
    }

    private static void updateFrame(Frame frame, DiskRenderState state) {
        frame.center = new Vec3(state.wx, state.wy, state.wz);
        frame.dir = lookDir(state.yRot, state.xRot);
        frame.radius = 0.72f * Math.max(0.2f, state.scale);
        Vec3 u = frame.dir.cross(new Vec3(0, 1, 0));
        if (u.lengthSqr() < 1e-6) u = new Vec3(1, 0, 0); else u = u.normalize();
        frame.u = u;
        frame.v = u.cross(frame.dir).normalize();
    }

    private Emission buildEmission(ClientLevel level, DiskRenderState state) {
        Emission emission = new Emission();
        updateFrame(emission.frame, state);
        Frame frame = emission.frame;
        int seed = state.seed;
        float scale = Math.max(0.2f, state.scale);
        emission.emitters = new MaterialEmitters.Emitter[]{
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(state.color, 1.55f * scale, seed,
                                Vec3.ZERO, 0.02f),
                        discCloud(frame, 0.0, 1.0), 430f, 560),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(0xFFFFFF, 1.05f * scale, seed ^ 0x02177,
                                Vec3.ZERO, 0.015f),
                        discCloud(frame, 0.62, 1.0), 250f, 330),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.vapor(state.color, 2.6f * scale, seed ^ 0x0BEEF,
                                Vec3.ZERO, 0.03f),
                        discCloud(frame, 0.1, 1.0), 80f, 110),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.ember(Vec3.ZERO, 0.03f),
                        rim(frame), 90f, 130),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(state.color, 1.2f * scale, seed ^ 0x05107,
                                Vec3.ZERO, 0.03f),
                        rim(frame), 140f, 190),
        };
        return emission;
    }

    @Override
    public void submit(DiskRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
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

        super.submit(state, poseStack, collector, cameraState);
    }

    public static class DiskRenderState extends EntityRenderState {
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
}
