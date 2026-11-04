package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.CurseTelegraphEntity;
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
 * Curse Telegraph as gathering cursed material, not a drawn marker. The
 * no-shapes conversion removes the crack stars, contracting band, tendril
 * ribbons, and hex eye dome entirely. The warning now reads through
 * material behavior: curse plasma and darkness gather across the ground
 * area, the shrinking implosion radius is carried by a tightening ground
 * source of hot plasma, rising rim emission replaces the tendrils, and the
 * center gathers soul and plasma that thickens as the countdown completes.
 * The accelerating pulse, the radius, the progress curve, and the entity id
 * seed are unchanged.
 */
public class CurseTelegraphRenderer extends EntityRenderer<CurseTelegraphEntity, CurseTelegraphRenderer.TelegraphRenderState> {

    public CurseTelegraphRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class TelegraphRenderState extends EntityRenderState {
        public float radius = 1.8f;
        public float progress = 0.0f;
        public float age = 0;
        public long seed = 0;
        public float lastEmitAge = -1f;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(CurseTelegraphEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(CurseTelegraphEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public TelegraphRenderState createRenderState() {
        return new TelegraphRenderState();
    }

    @Override
    public void extractRenderState(CurseTelegraphEntity entity, TelegraphRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.progress = entity.getProgress(partialTicks);
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getId() * 0x9E3779B97F4A7C15L + 0x7E1L;
    }

    @Override
    public void submit(TelegraphRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        float radius = state.radius;
        float progress = state.progress;
        float age = state.age;
        int seed = (int) (state.seed ^ (state.seed >>> 32));
        Vec3 center = new Vec3(state.x, state.y, state.z);
        float ds = MaterialEmitters.distanceScale(center);
        float delta = emissionDelta(state, age);

        // Pulse rate accelerates as countdown reaches completion, exactly as
        // the drawn marker pulsed.
        float pulseSpeed = 0.3f + (progress * 0.7f);
        float pulse = 0.8f + 0.2f * (float) Math.sin(age * pulseSpeed * 20.0f);
        float env = ds * pulse;

        // The corrupted ground: plasma and darkness gathering across the
        // full marker area, with a little ground dust in the drift.
        MaterialEmitters.Source ground = MaterialEmitters.groundArea(center, radius, center.y + 0.05);
        emit(level, MaterialEmitters.Preset.plasma(0xF20D33, 0.95f, seed, Vec3.ZERO, 0.020f),
            ground, 150f, delta, env);
        emit(level, MaterialEmitters.Preset.plasma(0xA60DD9, 0.85f, seed + 0x57A2, Vec3.ZERO, 0.020f),
            ground, 110f, delta, env);
        emit(level, MaterialEmitters.Preset.darkness(0x14030A, 1.45f, seed + 0xD415, new Vec3(0, 0.010, 0), 0.015f),
            ground, 120f, delta, env);
        emit(level, MaterialEmitters.Preset.dust(0x6E625E, 0.95f, seed + 0xD057, Vec3.ZERO, 0.025f),
            ground, 60f, delta, env * 0.7f);

        // The implosion indicator: hot plasma gathering inside a ground
        // source that shrinks from the full radius to nothing as progress
        // reaches one, the same contraction the band used to draw.
        float shrinkR = radius * (1.0f - progress);
        if (shrinkR > 0.05f) {
            MaterialEmitters.Source shrinking = MaterialEmitters.groundArea(center, shrinkR, center.y + 0.07);
            emit(level, MaterialEmitters.Preset.plasma(0xFFF2F7, 0.90f, seed + 0xB4D1, Vec3.ZERO, 0.020f),
                shrinking, 170f, delta, env);
            emit(level, MaterialEmitters.Preset.plasma(0x8C1AF2, 1.00f, seed + 0xB4D2, Vec3.ZERO, 0.020f),
                shrinking, 90f, delta, env);
        }

        // Rising cursed drift at the rim: emission sampled around the rim
        // circle and up to the old tendril height, which grows with progress.
        float pillarH = 1.2f + (progress * 2.5f);
        MaterialEmitters.Source rim = rimSource(center, radius, pillarH);
        emit(level, MaterialEmitters.Preset.plasma(0xF20D33, 0.90f, seed + 131, Vec3.ZERO, 0.020f),
            rim, 220f, delta, env);
        emit(level, MaterialEmitters.Preset.plasma(0xFFF2F7, 0.70f, seed + 263, Vec3.ZERO, 0.020f),
            rim, 100f, delta, env);
        emit(level, MaterialEmitters.Preset.darkness(0x1F0510, 1.30f, seed + 397, new Vec3(0, 0.014, 0), 0.015f),
            rim, 90f, delta, env);

        // The heart of the marker: soul and plasma gathering at the center,
        // thickening and brightening as the countdown completes.
        Vec3 heart = center.add(0, 0.10, 0);
        float coreRate = 120f * (0.5f + progress);
        emit(level, MaterialEmitters.Preset.plasma(0xF20D26, 1.00f + progress * 0.8f, seed + 0xE7E1, Vec3.ZERO, 0.030f),
            MaterialEmitters.point(heart), coreRate, delta, env);
        emit(level, MaterialEmitters.Preset.soul(0xD9B3FF, 0.85f, seed + 0xE7E2, Vec3.ZERO, 0.025f),
            MaterialEmitters.point(heart), 80f, delta, env);
    }

    /** Points sampled around the rim and up to the given height. Data only. */
    private static MaterialEmitters.Source rimSource(Vec3 center, double radius, double height) {
        return random -> {
            double ang = random.nextDouble() * Math.PI * 2.0;
            double rr = radius * (0.90 + random.nextDouble() * 0.20);
            return new MaterialEmitters.Sample(new Vec3(
                center.x + Math.cos(ang) * rr,
                center.y + 0.05 + random.nextDouble() * height,
                center.z + Math.sin(ang) * rr), Vec3.ZERO);
        };
    }

    private static void emit(ClientLevel level, MaterialEmitters.Preset preset, MaterialEmitters.Source source,
                             float ratePerSecond, float delta, float envelope) {
        if (level == null || envelope <= 0.01f || ratePerSecond <= 0f) return;
        int budget = Math.max(1, Math.round(ratePerSecond));
        new MaterialEmitters.Emitter(level, preset, source, ratePerSecond, budget).tick(delta, envelope);
    }

    private static float emissionDelta(TelegraphRenderState state, float age) {
        float prev = state.lastEmitAge;
        state.lastEmitAge = age;
        if (prev < 0f || age < prev) return 1.0f / 60.0f;
        return Math.max(0.001f, Math.min(0.05f, (age - prev) / 20.0f));
    }
}
