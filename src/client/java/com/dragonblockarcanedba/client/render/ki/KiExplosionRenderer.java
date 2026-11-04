package com.dragonblockarcanedba.client.render.ki;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.KiExplosionEntity;
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
 * Complex pass: layered composition, class L, with three builder types.
 * The pieces layer is fracturePlates plus a crackFan at the blast center
 * (the plates through submitSolid in technique tints, the cracks through
 * submitEnergy) and short flameTongues in technique colors through
 * submitEnergy with the flame canvas. The complex particle layer is
 * SHARD_CHIP sprites blown through the blast volume. The no-shapes
 * emission stays as filler only: technique-colored plasma filling the
 * blast volume, a white-hot plasma concentration in the inner 0.38 of
 * the radius, vapor blowing off the boundary, smoke rising and
 * lingering, and a ground ring of impact dust racing outward at the old
 * ring radius. The lifecycle is unchanged: it eases open over 8 ticks
 * and fades out across 36 ticks, squared, with the same early out below
 * a fade of 0.02. Seeded per blast from the synced seed, so no two
 * explosions churn the same way. The crater itself stays the entity
 * destruction code; nothing here touches it.
 */
public class KiExplosionRenderer extends EntityRenderer<KiExplosionEntity, KiExplosionRenderer.ExplosionRenderState> {

    public KiExplosionRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ExplosionRenderState createRenderState() {
        return new ExplosionRenderState();
    }

    @Override
    public void extractRenderState(KiExplosionEntity entity, ExplosionRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.color = entity.getColor();
        state.radius = entity.getRadius();
        state.ageInTicks = entity.tickCount + partialTicks;
        state.seed = entity.getSeed();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    /** Mutable per-frame emission data read by the sources. Data only. */
    private static final class Frame {
        Vec3 center = Vec3.ZERO;
        float radius = 1f;
        float ringRadius = 1f;
    }

    private static final class Emission {
        final Frame frame = new Frame();
        MaterialEmitters.Emitter[] emitters = new MaterialEmitters.Emitter[0];
        float lastAge = -1f;
    }

    private final Map<Long, Emission> emissions = new HashMap<>();

    /** Points inside the blast volume out to a fraction of the frame radius. */
    private static MaterialEmitters.Source blastVolume(Frame frame, double radiusFactor, double outward) {
        return random -> {
            double u = random.nextDouble() * 2.0 - 1.0;
            double ang = random.nextDouble() * Math.PI * 2.0;
            double rr = frame.radius * radiusFactor * Math.cbrt(random.nextDouble());
            double flat = Math.sqrt(Math.max(0.0, 1.0 - u * u)) * rr;
            Vec3 offset = new Vec3(Math.cos(ang) * flat, u * rr, Math.sin(ang) * flat);
            Vec3 pos = frame.center.add(offset);
            Vec3 vel = rr > 1e-4 ? offset.scale(outward / Math.max(0.2, frame.radius)) : Vec3.ZERO;
            return new MaterialEmitters.Sample(pos, vel);
        };
    }

    /** Points on the blast boundary, blown outward. */
    private static MaterialEmitters.Source boundary(Frame frame) {
        return random -> {
            double u = random.nextDouble() * 2.0 - 1.0;
            double ang = random.nextDouble() * Math.PI * 2.0;
            double flat = Math.sqrt(Math.max(0.0, 1.0 - u * u));
            Vec3 dirv = new Vec3(Math.cos(ang) * flat, u, Math.sin(ang) * flat);
            double rr = frame.radius * (0.9 + random.nextDouble() * 0.18);
            return new MaterialEmitters.Sample(frame.center.add(dirv.scale(rr)), dirv.scale(0.14));
        };
    }

    /** Points on the ground ring at the frame ring radius, racing outward. */
    private static MaterialEmitters.Source groundRing(Frame frame) {
        return random -> {
            double ang = random.nextDouble() * Math.PI * 2.0;
            double rr = frame.ringRadius * (0.92 + random.nextDouble() * 0.16);
            Vec3 radial = new Vec3(Math.cos(ang), 0.0, Math.sin(ang));
            Vec3 pos = new Vec3(frame.center.x + radial.x * rr,
                    frame.center.y + 0.06 + random.nextDouble() * 0.1,
                    frame.center.z + radial.z * rr);
            return new MaterialEmitters.Sample(pos, radial.scale(0.16));
        };
    }

    /** The synced technique color mixed toward white, for hot piece cores. */
    private static int glowColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        r += (int) ((255 - r) * 0.65f);
        g += (int) ((255 - g) * 0.65f);
        b += (int) ((255 - b) * 0.65f);
        return (r << 16) | (g << 8) | b;
    }

    /** The synced technique color darkened, for plate rims. */
    private static int darkColor(int color) {
        int r = (int) (((color >> 16) & 0xFF) * 0.30f);
        int g = (int) (((color >> 8) & 0xFF) * 0.30f);
        int b = (int) ((color & 0xFF) * 0.30f);
        return (r << 16) | (g << 8) | b;
    }

    private static void updateFrame(Frame frame, ExplosionRenderState state, float easedRadius, float ring) {
        frame.center = new Vec3(state.wx, state.wy, state.wz);
        frame.radius = Math.max(0.05f, easedRadius);
        frame.ringRadius = Math.max(0.05f, ring);
    }

    private Emission buildEmission(ClientLevel level, ExplosionRenderState state, float easedRadius, float ring) {
        Emission emission = new Emission();
        updateFrame(emission.frame, state, easedRadius, ring);
        Frame frame = emission.frame;
        int seed = state.seed;
        float r = Math.max(0.4f, state.radius);
        float fillRate = Math.min(1500f, 320f + r * 300f);
        emission.emitters = new MaterialEmitters.Emitter[]{
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(state.color, 2.5f, seed,
                                Vec3.ZERO, 0.02f),
                        blastVolume(frame, 1.0, 0.10), fillRate, 1650),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(0xFFFFFF, 1.7f, seed ^ 0x2B3C,
                                Vec3.ZERO, 0.015f),
                        blastVolume(frame, 0.38, 0.05), fillRate * 0.55f, 900),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.vapor(state.color, 3.1f, seed ^ 0x1234,
                                Vec3.ZERO, 0.02f),
                        boundary(frame), 150f + r * 40f, 320),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.smoke(state.color, 3.4f, seed ^ 0x05107,
                                new Vec3(0.0, 0.07, 0.0), 0.03f),
                        blastVolume(frame, 0.95, 0.03), 65f + r * 16f, 160),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP, Vec3.ZERO, 0.20f),
                        blastVolume(frame, 1.0, 0.10), 34f, 40),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.dust(state.color, 2.2f, seed ^ 0x99,
                                Vec3.ZERO, 0.03f),
                        groundRing(frame), 240f + r * 60f, 420),
        };
        return emission;
    }

    @Override
    public void submit(ExplosionRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float open = Math.min(1f, state.ageInTicks / 8f);
        float ease = 1f - (1f - open) * (1f - open);
        float fade = Math.max(0f, 1f - state.ageInTicks / 36f);
        fade = fade * fade;
        if (fade <= 0.02f) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        float radius = ease * state.radius;
        float ring = radius * (0.85f + open * 0.35f);
        float distScale = FxEnvelope.distScale(Math.sqrt(state.distanceToCameraSq));
        float envelope = fade * distScale;
        // The old ground ring alpha also carried (1 - open): it raced out
        // early and was gone once the blast fully opened.
        float ringEnvelope = envelope * (1f - open);

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc != null ? mc.level : null;
        if (level != null && envelope > 0.01f) {
            long key = ((long) state.seed << 32) ^ (state.color & 0xFFFFFFFFL);
            Emission emission = emissions.get(key);
            if (emission == null || state.ageInTicks < emission.lastAge - 1.0f) {
                emission = buildEmission(level, state, radius, ring);
                emissions.put(key, emission);
                if (emissions.size() > 64) emissions.clear();
            }
            updateFrame(emission.frame, state, radius, ring);
            float delta = emission.lastAge < 0f ? 0.05f
                    : Math.min(0.05f, Math.max(0.0f, (state.ageInTicks - emission.lastAge) / 20.0f));
            emission.lastAge = state.ageInTicks;
            for (int i = 0; i < emission.emitters.length; i++) {
                float env = i == emission.emitters.length - 1 ? ringEnvelope : envelope;
                if (env > 0.01f) emission.emitters[i].tick(delta, env);
            }
        }

        // Pieces layer, in entity local space (the pose origin is the
        // blast center, and the plates and cracks lie in the local XZ
        // plane at the center height, the same plane the filler ground
        // ring uses). Vertex estimate at distScale 1.0: fracturePlates
        // about 480, crackFan about 700, flameTongues 336 (7 tongues at
        // 48), for about 1500 of the class L budget of 6000, with three
        // builder types. Piece sizes follow the eased blast radius, so
        // the structure opens and fades with the lifecycle above.
        if (distScale > 0.01f && radius > 0.1f) {
            final long geoSeed = state.seed;
            final int geoColor = state.color;
            final int geoGlow = glowColor(state.color);
            final int geoDark = darkColor(state.color);
            final float pieceEnv = fade;
            final float pieceDs = distScale;
            final float plateR = radius * 0.90f;
            final float crackR = radius * 0.95f;
            final float tongueR = radius * 0.55f;
            final float tongueH = Math.max(0.4f, radius * 0.70f);
            final float sway = state.ageInTicks * 0.45f;
            ComplexGeometry.submitSolid(collector, poseStack, (pose, buffer) ->
                    ComplexGeometry.fracturePlates(pose, buffer, geoSeed ^ 0xB21DL,
                            plateR, 0.35f, geoGlow, geoDark, 0.97f, pieceEnv, pieceDs));
            ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) ->
                    ComplexGeometry.crackFan(pose, buffer, geoSeed ^ 0xC4A9L,
                            crackR, 0xFFFFFF, geoColor, 0.95f, pieceEnv, pieceDs));
            ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.FLAME_TONGUE, (pose, buffer) ->
                    ComplexGeometry.flameTongues(pose, buffer, geoSeed ^ 0xF1A7L,
                            new Vec3(0, 0.04, 0), tongueR, tongueH, 7,
                            geoGlow, geoColor, 0.95f, pieceEnv, sway, pieceDs));
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    public static class ExplosionRenderState extends EntityRenderState {
        public int color = 0xFFFFFF;
        public float radius = 1.0f;
        public float ageInTicks = 0;
        public int seed = 0;
        public double wx = 0;
        public double wy = 0;
        public double wz = 0;
    }
}
