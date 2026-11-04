package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.SaberDodgeSparkEntity;
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
 * Entity Renderer for Saber Dodge Spark in Minecraft 26.2.
 *
 * <p>Complex pass: this is a class S burst, so no pieces layer is
 * required. It gains a complex particle layer of SPARK_STREAK and
 * SHARD_CHIP sprites over the existing gold void plasma, white
 * plasma, spatial glass dust, and darkness vapor filler. Build-in,
 * growth, quadratic fade, colors, and the bloom moment are carried
 * by emission density and one opening burst, unchanged.</p>
 */
public class SaberDodgeSparkRenderer extends EntityRenderer<SaberDodgeSparkEntity, SaberDodgeSparkRenderer.DodgeSparkRenderState> {

    private final Map<Long, Emission> emissions = new HashMap<>();

    public SaberDodgeSparkRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class DodgeSparkRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public float age = 0;
        public int maxLifetime = 12;
        public long seed = 0;
        public double ex = 0;
        public double ey = 0;
        public double ez = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(SaberDodgeSparkEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(SaberDodgeSparkEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public DodgeSparkRenderState createRenderState() {
        return new DodgeSparkRenderState();
    }

    @Override
    public void extractRenderState(SaberDodgeSparkEntity entity, DodgeSparkRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getSparkScale();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
    }

    @Override
    public void submit(DodgeSparkRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
        long key = state.seed;
        if (progress >= 1.0f) {
            emissions.remove(key);
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        pruneEmissions();
        float open = FxEnvelope.smoothstep(Math.min(1.0f, progress / 0.20f));
        float fade = (1.0f - progress * progress) * open;
        float grow = FxEnvelope.smoothstep(Math.min(1.0f, progress / 0.25f));
        float ds = MaterialEmitters.distanceScale(new Vec3(state.ex, state.ey, state.ez));

        Emission emission = emissions.get(key);
        if (emission == null || emission.level != level || state.age < emission.lastAge - 0.01f) {
            emission = new Emission(level, state);
            emissions.put(key, emission);
        }
        emission.update(state, grow, progress);
        float delta = emission.delta(state.age);
        float strength = fade * ds;
        if (!emission.burstDone) {
            emission.burstDone = true;
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.plasma(0xFFD75E, 1.05f, (int) (state.seed ^ 0xC03EL), Vec3.ZERO, 0.42f),
                MaterialEmitters.point(emission.origin), Math.max(5, Math.round(22 * ds)));
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.glassDust(0xFFF3B0, 0.95f, (int) (state.seed ^ 0x54A2DL), Vec3.ZERO, 0.55f),
                MaterialEmitters.point(emission.origin), Math.max(4, Math.round(14 * ds)));
            // Complex burst: 10 complex sprites at full density,
            // inside the class S burst budget of 12.
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.52f),
                MaterialEmitters.point(emission.origin), Math.max(2, Math.round(6 * ds)));
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP, Vec3.ZERO, 0.46f),
                MaterialEmitters.point(emission.origin), Math.max(2, Math.round(4 * ds)));
        }
        if (strength > 0.01f) {
            for (MaterialEmitters.Emitter emitter : emission.emitters) {
                emitter.tick(delta, strength);
            }
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    private void pruneEmissions() {
        long now = System.nanoTime();
        emissions.values().removeIf(e -> now - e.lastSeenNanos > 5_000_000_000L);
    }

    /**
     * Per-effect emitter set, keyed by the synced UUID seed on the
     * renderer instance because render states are recreated on every
     * extraction. The source samples the burst volume as data only.
     */
    private static final class Emission {
        final ClientLevel level;
        final MaterialEmitters.Emitter[] emitters;
        float lastAge = -1.0f;
        long lastSeenNanos = System.nanoTime();
        boolean burstDone = false;
        Vec3 origin = Vec3.ZERO;
        float radius = 1.0f;

        Emission(ClientLevel level, DodgeSparkRenderState state) {
            this.level = level;
            update(state, 0.0f, 0.0f);
            int seed = (int) state.seed;
            float baseScale = state.scale;

            MaterialEmitters.Source volume = random -> {
                double u = random.nextDouble() * 2.0 - 1.0;
                double angle = random.nextDouble() * Math.PI * 2.0;
                double rr = radius * Math.cbrt(random.nextDouble());
                double flat = Math.sqrt(Math.max(0.0, 1.0 - u * u)) * rr;
                Vec3 pos = origin.add(Math.cos(angle) * flat, u * rr, Math.sin(angle) * flat);
                return new MaterialEmitters.Sample(pos, Vec3.ZERO);
            };

            this.emitters = new MaterialEmitters.Emitter[] {
                new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.plasma(0xFFD75E, 1.05f * baseScale, seed, Vec3.ZERO, 0.30f),
                    volume, 430.0f, 520),
                new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.plasma(0xFFFFFF, 0.72f * baseScale, seed ^ 0x4A10, Vec3.ZERO, 0.24f),
                    volume, 150.0f, 200),
                new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.glassDust(0xFFF3B0, 0.95f * baseScale, seed ^ 0x54A2, Vec3.ZERO, 0.16f),
                    volume, 190.0f, 250),
                new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.darkness(0x3D1A78, 1.30f * baseScale, seed ^ 0xBADD, Vec3.ZERO, 0.10f),
                    volume, 80.0f, 120),
                // Complex particle layer (class S requirement): spark
                // streaks and shard chips inside the burst volume.
                // Rates stay at or under the 40 per second budget.
                new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.22f),
                    volume, 36.0f, 40),
                new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP, Vec3.ZERO, 0.18f),
                    volume, 30.0f, 40)
            };
        }

        void update(DodgeSparkRenderState state, float grow, float progress) {
            this.origin = new Vec3(state.ex, state.ey, state.ez);
            float animatedScale = state.scale * (0.55f + 0.85f * grow + progress * 0.35f);
            this.radius = Math.max(0.10f, animatedScale * 0.85f);
        }

        float delta(float age) {
            this.lastSeenNanos = System.nanoTime();
            if (this.lastAge < 0.0f) {
                this.lastAge = age;
                return 1.0f / 60.0f;
            }
            float d = Math.min(0.05f, Math.max(0.0f, (age - this.lastAge) / 20.0f));
            this.lastAge = age;
            return d;
        }
    }
}
