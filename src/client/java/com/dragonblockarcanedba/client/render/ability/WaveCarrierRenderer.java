package com.dragonblockarcanedba.client.render.ability;

import com.dragonblockarcanedba.ability.AbilityDef;
import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.entity.WaveCarrierEntity;
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
 * Generic Wave renderer, parameterized entirely by the ability
 * definition: a ground-anchored arc band in three stacked strips
 * (rim at the ground, mid body, white-hot core band, fading top),
 * a ground ring at its base, and a sparse material decay shed along
 * the band. Geometry is the hero; the particles only dissipate.
 * The shape language is Phase 1 simple on purpose; the primitive
 * kit phase enriches behind this class.
 */
public class WaveCarrierRenderer extends EntityRenderer<WaveCarrierEntity, WaveCarrierRenderer.State> {

    public WaveCarrierRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class State extends EntityRenderState {
        public int entityId = -1;
        public AbilityDef def;
        public int seed = 0;
        public float power = 1f;
        public float yRot = 0f;
        public double wx = 0, wy = 0, wz = 0;
    }

    /** Mutable per-frame anchor for the shed source; the wave travels, so the source must follow. */
    private static final class Frame {
        double cx, cy, cz, yRot;
    }

    private static final class Emission {
        final Frame frame = new Frame();
        MaterialEmitters.Emitter[] emitters = new MaterialEmitters.Emitter[0];
        float lastAge = -1f;
    }

    private final Map<Integer, Emission> emissions = new HashMap<>();

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(WaveCarrierEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.entityId = entity.getId();
        state.def = entity.def();
        state.seed = entity.getSeed();
        state.power = entity.getPower();
        state.yRot = entity.getYRot();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        AbilityDef def = state.def;
        if (def != null && def.wave() != null) {
            float dist = (float) Math.sqrt(state.distanceToCameraSq);
            float distScale = FxEnvelope.distScale(dist);
            float env = FxEnvelope.envelope(state.ageInTicks, def.phases().active()) * distScale;
            if (env > 0.01f) {
                drawBand(state, def, poseStack, collector, env, distScale);
                tickEmission(state, def, dist, distScale);
            }
        }
        super.submit(state, poseStack, collector, cameraState);
    }

    private void drawBand(State state, AbilityDef def, PoseStack poseStack,
                          SubmitNodeCollector collector, float env, float distScale) {
        AbilityDef.WaveParams wave = def.wave();
        AbilityDef.VisualSpec vis = def.visual();
        double scale = vis.scale() * (0.55 + 0.45 * state.power);
        final double radius = wave.radius() * scale;
        final double height = wave.height() * vis.scale();
        final double arc = Math.toRadians(wave.arcDegrees());
        double yawRad = Math.toRadians(state.yRot);
        final double fwdX = -Math.sin(yawRad);
        final double fwdZ = Math.cos(yawRad);
        final int segments = FxEnvelope.segments(30, distScale, 10);

        final float[] rimB = {CarrierGeometry.r(vis.rim()), CarrierGeometry.g(vis.rim()), CarrierGeometry.b(vis.rim()), env * 0.75f};
        final float[] midB = {CarrierGeometry.r(vis.mid()), CarrierGeometry.g(vis.mid()), CarrierGeometry.b(vis.mid()), env * 0.9f};
        final float[] coreB = {CarrierGeometry.r(vis.core()), CarrierGeometry.g(vis.core()), CarrierGeometry.b(vis.core()), env};
        final float[] coreT = {CarrierGeometry.r(vis.core()), CarrierGeometry.g(vis.core()), CarrierGeometry.b(vis.core()), env * 0.55f};
        final float[] fadeT = {CarrierGeometry.r(vis.rim()), CarrierGeometry.g(vis.rim()), CarrierGeometry.b(vis.rim()), 0f};

        ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) -> {
            CarrierGeometry.arcBand(pose, buffer, fwdX, fwdZ, radius, 0, height * 0.35, arc, segments, rimB, midB);
            CarrierGeometry.arcBand(pose, buffer, fwdX, fwdZ, radius, height * 0.35, height * 0.68, arc, segments, midB, coreB);
            CarrierGeometry.arcBand(pose, buffer, fwdX, fwdZ, radius, height * 0.68, height, arc, segments, coreT, fadeT);
        });
        ComplexGeometry.submitVeil(collector, poseStack, (pose, buffer) -> {
            CarrierGeometry.flatRing(pose, buffer, 0, 0, 0.06, radius * 0.96, radius * 1.10, segments,
                    rimB[0], rimB[1], rimB[2], env * 0.45f);
        });
    }

    private void tickEmission(State state, AbilityDef def, float dist, float distScale) {
        if (dist > def.caps().cullDistance()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc != null ? mc.level : null;
        if (level == null) {
            return;
        }
        Emission emission = emissions.get(state.entityId);
        if (emission == null || state.ageInTicks < emission.lastAge - 1.0f) {
            emission = buildEmission(level, state, def);
            emissions.put(state.entityId, emission);
            if (emissions.size() > 64) {
                emissions.clear();
            }
        }
        emission.frame.cx = state.wx;
        emission.frame.cy = state.wy;
        emission.frame.cz = state.wz;
        emission.frame.yRot = state.yRot;
        float delta = emission.lastAge < 0f ? 0.05f
                : Math.min(0.05f, Math.max(0f, (state.ageInTicks - emission.lastAge) / 20.0f));
        emission.lastAge = state.ageInTicks;
        for (MaterialEmitters.Emitter emitter : emission.emitters) {
            emitter.tick(delta, distScale);
        }
    }

    private Emission buildEmission(ClientLevel level, State state, AbilityDef def) {
        Emission emission = new Emission();
        AbilityDef.VisualSpec vis = def.visual();
        AbilityDef.WaveParams wave = def.wave();
        final double radius = wave.radius() * vis.scale();
        final double height = wave.height() * vis.scale();
        final double arc = Math.toRadians(wave.arcDegrees());
        final Frame frame = emission.frame;
        frame.cx = state.wx;
        frame.cy = state.wy;
        frame.cz = state.wz;
        frame.yRot = state.yRot;
        MaterialEmitters.Source alongBand = random -> {
            double yawRad = Math.toRadians(frame.yRot);
            double fwdX = -Math.sin(yawRad);
            double fwdZ = Math.cos(yawRad);
            double a = (random.nextDouble() - 0.5) * arc;
            double px = frame.cx + (fwdX * Math.cos(a) - fwdZ * Math.sin(a)) * radius;
            double pz = frame.cz + (fwdZ * Math.cos(a) + fwdX * Math.sin(a)) * radius;
            double py = frame.cy + random.nextDouble() * height;
            return new MaterialEmitters.Sample(new Vec3(px, py, pz),
                    new Vec3(fwdX * 0.05, 0.05, fwdZ * 0.05));
        };
        float rate = Math.min(def.caps().maxParticlesPerSecond(), 260f * vis.particleRate());
        emission.emitters = new MaterialEmitters.Emitter[]{
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.material(vis.particleMaterial(), vis.mid(),
                                2.1f * vis.scale(), state.seed, Vec3.ZERO, 0.03f),
                        alongBand, rate, Math.max(1, Math.round(rate)))
        };
        return emission;
    }
}
