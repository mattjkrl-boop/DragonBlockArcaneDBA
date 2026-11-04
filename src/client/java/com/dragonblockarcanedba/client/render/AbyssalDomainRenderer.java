package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.AbyssalDomainEntity;
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

/**
 * The Abyssal Tempest Domain as a layered composition. The pieces layer is
 * ten seeded rim tendrils lashing up the boundary through the shader, over a
 * crackFan base web of curse energy in the ground. The complex particle
 * layer is ash flakes falling through the volume. The material emission
 * stays as filler: the domain volume is sampled as data and filled with
 * darkness, smoke, curse plasma, and ground dust through the shared material
 * emitters, a sparse shell source suggests the boundary, interior strikes
 * are plasma emitted along seeded paths, and the bright opening at the top
 * is a dense point emission. The envelope still runs the forty tick build
 * and the entity's thirty tick fade, and the radius, colors, and lifecycle
 * are unchanged.
 */
public class AbyssalDomainRenderer extends EntityRenderer<AbyssalDomainEntity, AbyssalDomainRenderer.AbyssalDomainRenderState> {

    public AbyssalDomainRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class AbyssalDomainRenderState extends EntityRenderState {
        public float radius = 10f;
        public float age = 0;
        public long seed = 0;
        public int fadeRemaining = -1;
        public float lastEmitAge = -1f;
    }

    // Rendering stays camera relative: the domain must be visible from the
    // whole 10 to 30 block radius, and the default entity frustum cull would
    // hide it whenever the (tiny, invisible) entity origin leaves the screen.
    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(AbyssalDomainEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(AbyssalDomainEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public AbyssalDomainRenderState createRenderState() {
        return new AbyssalDomainRenderState();
    }

    @Override
    public void extractRenderState(AbyssalDomainEntity entity, AbyssalDomainRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
        int fade = entity.getFadeRemaining();
        if (fade >= 0) state.fadeRemaining = Math.max(0, fade - (int) Math.floor(partialTicks));
        else state.fadeRemaining = -1;
    }

    @Override
    public void submit(AbyssalDomainRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        float radius = state.radius;
        int seed = (int) (state.seed ^ (state.seed >>> 32));
        float build = FxEnvelope.smoothstep(Math.min(1.0f, state.age / 40.0f));
        float fadeFrac;
        if (state.fadeRemaining < 0) {
            fadeFrac = 1.0f;
        } else {
            fadeFrac = FxEnvelope.smoothstep(state.fadeRemaining / (float) AbyssalDomainEntity.FADE_TICKS);
        }
        float alpha = build * fadeFrac;
        if (alpha <= 0.01f) return;

        Vec3 center = new Vec3(state.x, state.y, state.z);
        float ds = MaterialEmitters.distanceScale(center);
        float env = alpha * ds;
        float delta = emissionDelta(state, state.age);
        float age = state.age;
        float open = FxEnvelope.smoothstep(Math.min(1.0f, Math.max(0.0f, (age - 20.0f) / 40.0f)));
        double height = radius * 1.7;
        Vec3 volumeCenter = center.add(0, height * 0.30, 0);

        // The standing volume: darkness and smoke carry the storm body, with
        // curse plasma motes drifting through it and dust creeping at ground.
        emit(level, MaterialEmitters.Preset.darkness(0x16030C, 2.0f, seed + 11, Vec3.ZERO, 0.010f),
            MaterialEmitters.sphereVolume(volumeCenter, radius * 0.95), 300f, delta, env);
        emit(level, MaterialEmitters.Preset.smoke(0x2A0714, 2.4f, seed + 23, new Vec3(0, 0.010, 0), 0.010f),
            MaterialEmitters.volume(volumeCenter, new Vec3(radius, height * 0.34, radius)), 180f, delta, env);
        emit(level, MaterialEmitters.Preset.plasma(0xF20D33, 1.25f, seed + 37, Vec3.ZERO, 0.030f),
            MaterialEmitters.sphereVolume(volumeCenter, radius * 0.92), 240f, delta, env);
        emit(level, MaterialEmitters.Preset.plasma(0x8C1AF2, 1.05f, seed + 41, Vec3.ZERO, 0.030f),
            MaterialEmitters.sphereVolume(volumeCenter, radius * 0.80), 150f, delta, env);
        emit(level, MaterialEmitters.Preset.dust(0x6E625E, 1.15f, seed + 53, Vec3.ZERO, 0.020f),
            MaterialEmitters.groundArea(center, radius * 1.18, center.y + 0.06), 120f, delta, env);

        // Pieces layer, class L at two builder types. The base web is a
        // crackFan of curse energy wandering across the ground inside the
        // rim, hot at the heart and crimson at the tips. Attack upgrade:
        // the fan runs three branches richer; crackFan derives its crack
        // count from segments(9, detail, 6), so the boost rides the
        // detail scale (plus 1/3 adds exactly three base cracks).
        final long pieceSeed = state.seed;
        final float pieceRadius = radius;
        final float pieceAlpha = alpha;
        final float pieceDs = ds;
        ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) ->
            ComplexGeometry.crackFan(pose, buffer, pieceSeed ^ 0xC4AC1L, pieceRadius * 1.02f,
                0xFFF2F7, 0xF20D33, 0.95f, pieceAlpha, pieceDs + 1.0f / 3.0f));

        // Rim tendrils: ten seeded strands stood on the boundary circle,
        // leaning inward as they climb, violet at the root and crimson at
        // the tip, drawn on the tendril canvas through the shader.
        final double tendrilHeight = height;
        ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
            float tendrilWidth = Math.min(1.3f, Math.max(0.45f, pieceRadius * 0.085f));
            for (int i = 0; i < 10; i++) {
                double ang = (i / 10.0) * Math.PI * 2.0 + (i % 3) * 0.21;
                double cos = Math.cos(ang);
                double sin = Math.sin(ang);
                Vec3 from = new Vec3(cos * pieceRadius * 0.98, 0.10, sin * pieceRadius * 0.98);
                Vec3 control = new Vec3(cos * pieceRadius * 1.09, tendrilHeight * 0.34, sin * pieceRadius * 1.09);
                Vec3 to = new Vec3(cos * pieceRadius * 0.86, tendrilHeight * (0.52 + 0.10 * (i % 4) / 3.0), sin * pieceRadius * 0.86);
                ComplexGeometry.tendril(pose, buffer, pieceSeed + i * 7919L, from, control, to,
                    tendrilWidth, 0x8C1AF2, 0xF20D33, 0.92f, pieceAlpha, pieceDs * 0.7f);
            }
        });

        // Complex particle layer: ash flakes drifting down through the
        // storm volume, the physical fallout the curse burns off.
        emit(level, MaterialEmitters.Preset.fixed(DbaParticles.ASH_FLAKE, new Vec3(0, -0.018, 0), 0.010f),
            MaterialEmitters.sphereVolume(volumeCenter, radius * 0.92), 34f, delta, env);

        // The boundary is only suggested: sparse large vapor and plasma
        // sprites sampled on the dome shell, never a drawn shell.
        MaterialEmitters.Source shell = shellSource(center, radius, radius);
        emit(level, MaterialEmitters.Preset.vapor(0x5A1230, 3.6f, seed + 67, Vec3.ZERO, 0.006f),
            shell, 60f, delta, env);
        emit(level, MaterialEmitters.Preset.plasma(0xD90D4D, 2.1f, seed + 71, Vec3.ZERO, 0.010f),
            shell, 90f, delta, env);
        emit(level, MaterialEmitters.Preset.darkness(0x0D0208, 3.0f, seed + 73, Vec3.ZERO, 0.006f),
            shell, 60f, delta, env);

        // Interior strikes: plasma along seeded paths on the same eight tick
        // flash cycle the branch bolts used.
        int cycleIndex = (int) (age / 8.0f);
        float boltFlash = (age % 8.0f) < 4.0f ? 1.0f - ((age % 8.0f) / 4.0f) * 0.4f : 0.0f;
        if (boltFlash > 0.01f) {
            java.util.Random boltRandom = new java.util.Random(state.seed + cycleIndex * 1000L);
            for (int b = 0; b < 3; b++) {
                float a1 = boltRandom.nextFloat() * (float) (Math.PI * 2.0);
                float a2 = boltRandom.nextFloat() * (float) (Math.PI * 2.0);
                Vec3 from = center.add(Math.cos(a1) * radius * 0.45f, radius * 0.75f, Math.sin(a1) * radius * 0.45f);
                Vec3 to = center.add(Math.cos(a2) * radius * 0.20f, 0.2f, Math.sin(a2) * radius * 0.20f);
                emit(level, MaterialEmitters.Preset.plasma(0xF24059, 1.15f, seed + cycleIndex * 131 + b * 17, Vec3.ZERO, 0.025f),
                    MaterialEmitters.path(from, to, 0.0), 240f, delta, env * boltFlash);
            }
        }

        // The opening and eye at the top: a bright dense point emission that
        // strengthens as the storm matures.
        if (open > 0.01f) {
            Vec3 top = center.add(0, radius, 0);
            emit(level, MaterialEmitters.Preset.plasma(0xDFF6FF, 1.8f, seed + 97, Vec3.ZERO, 0.020f),
                MaterialEmitters.point(top), 120f, delta, env * open);
            emit(level, MaterialEmitters.Preset.plasma(0x7303A6, 2.4f, seed + 101, Vec3.ZERO, 0.015f),
                MaterialEmitters.point(top), 60f, delta, env * open);
        }
    }

    /** Points sampled on the dome shell surface. Data only; draws nothing. */
    private static MaterialEmitters.Source shellSource(Vec3 center, double radius, double top) {
        return random -> {
            double yFrac = random.nextDouble();
            double ang = random.nextDouble() * Math.PI * 2.0;
            double rr = radius * Math.sqrt(Math.max(0.0, 1.0 - yFrac * yFrac)) * (0.94 + random.nextDouble() * 0.06);
            return new MaterialEmitters.Sample(new Vec3(
                center.x + Math.cos(ang) * rr, center.y + yFrac * top, center.z + Math.sin(ang) * rr), Vec3.ZERO);
        };
    }

    private static void emit(ClientLevel level, MaterialEmitters.Preset preset, MaterialEmitters.Source source,
                             float ratePerSecond, float delta, float envelope) {
        if (level == null || envelope <= 0.01f || ratePerSecond <= 0f) return;
        int budget = Math.max(1, Math.round(ratePerSecond));
        new MaterialEmitters.Emitter(level, preset, source, ratePerSecond, budget).tick(delta, envelope);
    }

    private static float emissionDelta(AbyssalDomainRenderState state, float age) {
        float prev = state.lastEmitAge;
        state.lastEmitAge = age;
        if (prev < 0f || age < prev) return 1.0f / 60.0f;
        return Math.max(0.001f, Math.min(0.05f, (age - prev) / 20.0f));
    }
}
