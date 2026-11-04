package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.OxStanceAuraEntity;
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
 * Entity Renderer for Ox King's Colossal Stance Aura in Minecraft 26.2.
 * No-shapes pass: this renderer draws no geometry. There is no titan avatar,
 * no boundary band, and no dome. The stance reads as material under
 * pressure: plasma motes and vapor gathered around the caster, ground dust
 * across the 12 block denial field, sparse plasma motes drifting at the
 * boundary radius so the edge is sensed rather than drawn, rising embers,
 * and, in the peak window, radial plasma bursts. Radius, pulse, fade in,
 * and the peak window are unchanged from the bespoke build.
 */
public class OxStanceAuraRenderer extends EntityRenderer<OxStanceAuraEntity, OxStanceAuraRenderer.OxStanceRenderState> {

    public OxStanceAuraRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class OxStanceRenderState extends EntityRenderState {
        public int heldTicks = 0;
        public boolean isPeak = false;
        public float age = 0;
        public long seed = 0;
        public double wx = 0;
        public double wy = 0;
        public double wz = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(OxStanceAuraEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(OxStanceAuraEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public OxStanceRenderState createRenderState() {
        return new OxStanceRenderState();
    }

    @Override
    public void extractRenderState(OxStanceAuraEntity entity, OxStanceRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.heldTicks = entity.getHeldTicks();
        state.isPeak = entity.isPeak();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    /** Mutable per-frame emission data read by the sources. Data only. */
    private static final class Frame {
        Vec3 center = Vec3.ZERO;
        float bodyRadius = 3.0f;
        float bodyHeight = 3.6f;
        float fieldRadius = 12.0f;
    }

    private static final class Emission {
        final Frame frame = new Frame();
        MaterialEmitters.Emitter plasma;
        MaterialEmitters.Emitter vapor;
        MaterialEmitters.Emitter boundary;
        MaterialEmitters.Emitter dust;
        MaterialEmitters.Emitter ember;
        MaterialEmitters.Emitter peak;
        boolean peakState = false;
        float lastAge = -1f;
    }

    private final Map<Long, Emission> emissions = new HashMap<>();

    /** Points in the body volume with an inward drift velocity. */
    private static MaterialEmitters.Source gathering(Frame frame) {
        return random -> {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double rr = frame.bodyRadius * (0.30 + random.nextDouble() * 0.80);
            Vec3 pos = new Vec3(
                    frame.center.x + Math.cos(angle) * rr,
                    frame.center.y + 0.10 + random.nextDouble() * frame.bodyHeight,
                    frame.center.z + Math.sin(angle) * rr);
            Vec3 vel = new Vec3(
                    (frame.center.x - pos.x) * 0.06,
                    0.03,
                    (frame.center.z - pos.z) * 0.06);
            return new MaterialEmitters.Sample(pos, vel);
        };
    }

    /** Points across the denial field at ground level. */
    private static MaterialEmitters.Source groundDisc(Frame frame) {
        return random -> {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double rr = frame.fieldRadius * Math.sqrt(random.nextDouble());
            return new MaterialEmitters.Sample(new Vec3(
                    frame.center.x + Math.cos(angle) * rr,
                    frame.center.y + 0.05 + random.nextDouble() * 0.18,
                    frame.center.z + Math.sin(angle) * rr), Vec3.ZERO);
        };
    }

    /** Sparse points at the boundary radius, drifting slowly around it. */
    private static MaterialEmitters.Source boundaryDrift(Frame frame) {
        return random -> {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double rr = frame.fieldRadius * (0.97 + random.nextDouble() * 0.05);
            Vec3 pos = new Vec3(
                    frame.center.x + Math.cos(angle) * rr,
                    frame.center.y + 0.08 + random.nextDouble() * 1.1,
                    frame.center.z + Math.sin(angle) * rr);
            Vec3 vel = new Vec3(-Math.sin(angle) * 0.05, 0.01, Math.cos(angle) * 0.05);
            return new MaterialEmitters.Sample(pos, vel);
        };
    }

    /** Points near the caster with an outward radial burst velocity. */
    private static MaterialEmitters.Source radialBurst(Frame frame) {
        return random -> {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double rr = 0.6 + random.nextDouble() * 0.6;
            Vec3 pos = new Vec3(
                    frame.center.x + Math.cos(angle) * rr,
                    frame.center.y + 0.4 + random.nextDouble() * 1.8,
                    frame.center.z + Math.sin(angle) * rr);
            Vec3 vel = new Vec3(Math.cos(angle) * 0.38, 0.02, Math.sin(angle) * 0.38);
            return new MaterialEmitters.Sample(pos, vel);
        };
    }

    private Emission buildEmission(ClientLevel level, OxStanceRenderState state) {
        Emission emission = new Emission();
        emission.peakState = state.isPeak;
        Frame frame = emission.frame;
        frame.center = new Vec3(state.wx, state.wy, state.wz);
        int seed = (int) state.seed;
        int plasmaColor = state.isPeak ? 0xFFEB59 : 0xFF9E14;
        int bodyColor = state.isPeak ? 0xFFEB59 : 0xFF7A1A;
        emission.plasma = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(bodyColor, 1.2f, seed ^ 0x4EA7,
                        Vec3.ZERO, 0.01f),
                gathering(frame), 120f, 170);
        emission.vapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(0xD8C6A8, 2.1f, seed ^ 0x4EA8,
                        new Vec3(0.0, 0.05, 0.0), 0.03f),
                gathering(frame), 50f, 85);
        emission.boundary = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(plasmaColor, 1.0f, seed ^ 0xB0DA,
                        Vec3.ZERO, 0.01f),
                boundaryDrift(frame), 55f, 90);
        emission.dust = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(0xA0825F, 1.6f, seed ^ 0xD057,
                        new Vec3(0.0, 0.02, 0.0), 0.03f),
                groundDisc(frame), 70f, 110);
        emission.ember = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.ember(new Vec3(0.0, 0.16, 0.0), 0.05f),
                gathering(frame), 24f, 45);
        emission.peak = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xFFEB59, 1.3f, seed ^ 0x591C,
                        Vec3.ZERO, 0.02f),
                radialBurst(frame), 170f, 230);
        return emission;
    }

    @Override
    public void submit(OxStanceRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        int held = state.heldTicks;
        boolean isPeak = state.isPeak;
        float age = state.age;

        float intensity = Math.min(1.0f, held / 100.0f);
        float pulse = 0.92f + 0.08f * (float) Math.sin(age * 0.45f);
        // Lifecycle: the emission still builds in over the first 8 ticks.
        float fadeIn = FxEnvelope.smoothstep(Math.min(1.0f, age / 8.0f));
        float distScale = FxEnvelope.distScale(Math.sqrt(state.distanceToCameraSq));
        float envelope = fadeIn * (0.45f + intensity * 0.55f) * distScale;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc != null ? mc.level : null;
        if (level != null && envelope > 0.01f) {
            Emission emission = emissions.get(state.seed);
            if (emission == null || emission.peakState != isPeak || age < emission.lastAge - 1.0f) {
                emission = buildEmission(level, state);
                emissions.put(state.seed, emission);
                if (emissions.size() > 64) emissions.clear();
            }
            emission.frame.center = new Vec3(state.wx, state.wy, state.wz);
            emission.frame.bodyRadius = (2.2f + intensity * 0.8f) * pulse;
            emission.frame.bodyHeight = 3.6f * pulse;
            float delta = emission.lastAge < 0f ? 0.05f
                    : Math.min(0.05f, Math.max(0.0f, (age - emission.lastAge) / 20.0f));
            emission.lastAge = age;
            emission.plasma.tick(delta, envelope);
            emission.vapor.tick(delta, envelope);
            emission.boundary.tick(delta, envelope);
            emission.dust.tick(delta, envelope);
            emission.ember.tick(delta, envelope);
            // Peak window overdrive only exists while the stance is at peak.
            if (isPeak) {
                emission.peak.tick(delta, envelope);
            }
        }

        super.submit(state, poseStack, collector, cameraState);
    }
}
