package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.DelayedLaunchEntity;
import com.dragonblockarcanedba.particle.DbaParticles;
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
 * Telegraph for the Z Sword delayed launch.
 *
 * Complex pass: layered composition. The pieces layer is a crackFan
 * telegraph web on the ground inside the contracting radius, gold
 * hot at the center running out to violet at the tips, drawn on the
 * shader through submitEnergy; in the last tenth of the countdown a
 * tendril lash fans upward from the center as the launch fires. The
 * complex particle layer is a one shot SPARK_STREAK burst in that
 * same launch window. The filler layer is unchanged and uses no ring,
 * disc, or tick marks: stone dust drifts across the full true slam
 * footprint for the whole countdown, violet gravity plasma and pale
 * vapor are sampled inside the contracting radius and pulled inward
 * faster as the launch approaches, and gold plasma concentrations
 * sit at the four rotating cardinal data points. The true radius,
 * true delay, pulse, and rotation behavior carry over from before.
 */
public class DelayedLaunchRenderer extends EntityRenderer<DelayedLaunchEntity, DelayedLaunchRenderer.DelayedLaunchRenderState> {

    private static final int VIOLET = 0x9419FA;
    private static final int GOLD = 0xFFD94D;
    private static final int PALE_VIOLET = 0xC9B8FF;
    private static final int STONE_DUST = 0x9A8668;

    private final Map<Integer, Emission> emissions = new HashMap<>();

    public DelayedLaunchRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class DelayedLaunchRenderState extends EntityRenderState {
        public float age = 0;
        public float radius = 10.0f;
        public int delayTicks = 40;
        public long seed = 0;
        public int entityId = -1;
    }

    @Override
    public DelayedLaunchRenderState createRenderState() {
        return new DelayedLaunchRenderState();
    }

    @Override
    public void extractRenderState(DelayedLaunchEntity entity, DelayedLaunchRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.age = entity.tickCount + partialTicks;
        state.radius = entity.getRadius();
        state.delayTicks = Math.max(1, entity.getDelayTicks());
        state.seed = entity.getSeed();
        state.entityId = entity.getId();
    }

    @Override
    public void submit(DelayedLaunchRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft == null ? null : minecraft.level;
        if (level == null) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        float age = state.age;
        float progress = Math.min(1.0f, age / (float) state.delayTicks);
        float slamRadius = state.radius;
        float radius = slamRadius * (1.0f - progress * 0.65f);
        float pulse = 0.60f + 0.40f * (float) Math.sin(age * (0.35 + progress * 0.85));
        float ringAlpha = (0.35f + progress * 0.45f) * pulse;
        float rot = age * (6.0f + progress * 30.0f);
        Vec3 center = new Vec3(state.x, state.y, state.z);
        float dist = MaterialEmitters.distanceScale(center);
        if (ringAlpha <= 0.01f || dist <= 0.01f) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        Emission emission = emissionFor(state, level);
        emission.frame.center = center;
        emission.frame.slamRadius = slamRadius;
        emission.frame.currentRadius = radius;
        emission.frame.progress = progress;
        emission.frame.rot = rot;

        float delta = emission.nextDelta();
        float envelope = ringAlpha * dist;
        emission.dust.tick(delta, envelope * 0.62f);
        emission.plasma.tick(delta, envelope);
        emission.vapor.tick(delta, envelope * 0.58f);
        emission.gold.tick(delta, envelope * 0.92f);

        // Pieces layer: the crack web telegraphs the contracting
        // radius for the whole countdown.
        long geoSeed = state.seed;
        float webRadius = radius;
        float webEnv = Math.min(1.0f, ringAlpha * 1.5f) * (0.45f + 0.55f * progress);
        float distF = dist;
        ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) ->
            ComplexGeometry.crackFan(pose, buffer, geoSeed ^ 0xC9ACL,
                webRadius, GOLD, VIOLET, 0.92f, webEnv, distF));

        // Launch window: the last tenth of the countdown fires the
        // tendril lash and the spark streak burst, once per entity.
        if (progress >= 0.90f) {
            emission.burstLaunch();
            float lashEnv = Math.min(1.0f, (progress - 0.90f) / 0.10f);
            float lashLen = slamRadius * 0.55f;
            ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) ->
                ComplexGeometry.tendrilFan(pose, buffer, geoSeed ^ 0x1A57L,
                    new Vec3(0.0, 0.10, 0.0), new Vec3(0.0, 1.0, 0.0), 55.0f, lashLen, 4,
                    0.20f, VIOLET, GOLD, 0.95f, lashEnv, distF));
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    private Emission emissionFor(DelayedLaunchRenderState state, ClientLevel level) {
        long now = System.nanoTime();
        emissions.entrySet().removeIf(entry -> now - entry.getValue().lastSeenNanos > 5_000_000_000L);
        Emission emission = emissions.get(state.entityId);
        if (emission == null || emission.seed != state.seed) {
            emission = new Emission(level, state.seed);
            emissions.put(state.entityId, emission);
        }
        emission.lastSeenNanos = now;
        return emission;
    }

    private static final class Frame {
        Vec3 center = Vec3.ZERO;
        float slamRadius = 1.0f;
        float currentRadius = 1.0f;
        float progress = 0.0f;
        float rot = 0.0f;
    }

    private static final class Emission {
        final long seed;
        final ClientLevel level;
        boolean launchBurstDone = false;
        final Frame frame = new Frame();
        final MaterialEmitters.Emitter dust;
        final MaterialEmitters.Emitter plasma;
        final MaterialEmitters.Emitter vapor;
        final MaterialEmitters.Emitter gold;
        long lastSeenNanos = System.nanoTime();
        long lastTickNanos = 0L;

        Emission(ClientLevel level, long seed) {
            this.seed = seed;
            this.level = level;
            int baseSeed = (int) seed;
            Frame f = frame;

            MaterialEmitters.Source footprint = random -> {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double flat = Math.sqrt(random.nextDouble()) * f.slamRadius;
                Vec3 pos = f.center.add(Math.cos(angle) * flat, 0.06, Math.sin(angle) * flat);
                Vec3 inward = new Vec3(-Math.cos(angle), 0.0, -Math.sin(angle))
                    .scale(0.07 + f.progress * 0.10).add(0.0, 0.020, 0.0);
                return new MaterialEmitters.Sample(pos, inward);
            };
            MaterialEmitters.Source contracting = random -> {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double flat = Math.sqrt(random.nextDouble()) * f.currentRadius;
                Vec3 pos = f.center.add(Math.cos(angle) * flat, 0.07 + random.nextDouble() * 0.42,
                    Math.sin(angle) * flat);
                Vec3 inward = new Vec3(-Math.cos(angle), 0.0, -Math.sin(angle))
                    .scale(0.15 + f.progress * 0.22).add(0.0, 0.028, 0.0);
                return new MaterialEmitters.Sample(pos, inward);
            };
            MaterialEmitters.Source cardinal = random -> {
                int index = random.nextInt(4);
                double angle = Math.toRadians(f.rot + index * 90.0);
                Vec3 pos = f.center.add(Math.cos(angle) * f.currentRadius, 0.10,
                    Math.sin(angle) * f.currentRadius);
                Vec3 inward = new Vec3(-Math.cos(angle), 0.0, -Math.sin(angle)).scale(0.12);
                return new MaterialEmitters.Sample(pos, inward);
            };

            dust = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(STONE_DUST, 1.70f, baseSeed ^ 0xD057, Vec3.ZERO, 0.010f),
                footprint, 60.0f, 135);
            plasma = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(VIOLET, 2.15f, baseSeed ^ 0xC7, Vec3.ZERO, 0.010f),
                contracting, 96.0f, 205);
            vapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(PALE_VIOLET, 2.70f, baseSeed ^ 0x9A90, Vec3.ZERO, 0.010f),
                contracting, 44.0f, 100);
            gold = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(GOLD, 1.55f, baseSeed ^ 0x601D, Vec3.ZERO, 0.045f),
                cardinal, 50.0f, 115);
        }

        /**
         * One shot SPARK_STREAK burst in the launch window: streaks
         * thrown up and outward from the contracting ring. Fires once
         * per emission instance, so once per entity and seed.
         */
        void burstLaunch() {
            if (launchBurstDone) return;
            launchBurstDone = true;
            Frame f = frame;
            MaterialEmitters.Source launchSource = random -> {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double flat = Math.sqrt(random.nextDouble()) * f.currentRadius;
                Vec3 pos = f.center.add(Math.cos(angle) * flat, 0.18, Math.sin(angle) * flat);
                Vec3 outward = new Vec3(Math.cos(angle), 0.0, Math.sin(angle)).scale(0.10);
                return new MaterialEmitters.Sample(pos,
                    outward.add(0.0, 0.30 + random.nextDouble() * 0.22, 0.0));
            };
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.16f),
                launchSource, 24);
        }

        float nextDelta() {
            long now = System.nanoTime();
            if (lastTickNanos == 0L) {
                lastTickNanos = now;
                return 1.0f / 60.0f;
            }
            float delta = Math.min(0.05f, (now - lastTickNanos) / 1_000_000_000.0f);
            lastTickNanos = now;
            return Math.max(0.001f, delta);
        }
    }
}
