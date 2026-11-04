package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.SpikeFlailSpinEntity;
import com.dragonblockarcanedba.item.SpikeFlailItem;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Orbiting ball, then the outward crack when the channel lets go.
 * No-shapes pass: this renderer submits no effect geometry. The orbit reads
 * as air vapor and steel sparks shed along the ball's real path, sparse
 * vapor along the chain path, and warm dust at the ground contact. The
 * release reads as dust, vapor, and warm plasma material riding the
 * expanding crack edge, then a one-time burst of ground dust, vapor, steel
 * sparks, and physical block chunks on the tick the damage lands. The
 * physical Spike Flail item model in the holder's hand is untouched. Seeds
 * derive from the entity id, so every client emits the same variation.
 */
public class SpikeFlailSpinRenderer extends EntityRenderer<SpikeFlailSpinEntity, SpikeFlailSpinRenderer.SpinState> {
    /** Warm steel dust, the flail's particle theme color. */
    private static final int THEME_DUST = 0xD8C9A3;

    private final Map<Integer, Emission> emissions = new HashMap<>();

    public SpikeFlailSpinRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SpinState extends EntityRenderState {
        public float angle;
        public float radius;
        public boolean releasing;
        public int releaseAge;
        public float age;
        public float charge;
        public long seed;
        public int entityId;
        public Vec3 center = Vec3.ZERO;
        public Vec3 ball = Vec3.ZERO;
        public Vec3 prevBall = Vec3.ZERO;
        public Vec3 chainStart = Vec3.ZERO;
        public double groundY;
    }

    public boolean shouldRender(SpikeFlailSpinEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(SpikeFlailSpinEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public SpinState createRenderState() {
        return new SpinState();
    }

    @Override
    public void extractRenderState(SpikeFlailSpinEntity entity, SpinState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.angle = entity.angle();
        state.radius = entity.orbitRadius();
        state.releasing = entity.releasing();
        state.releaseAge = entity.releaseAge();
        state.age = entity.tickCount + partialTick;
        state.charge = entity.charge();
        state.seed = entity.getId() * 2654435761L ^ 0x5F1A11L;
        state.entityId = entity.getId();
        state.center = new Vec3(state.x, state.y, state.z);
        state.groundY = state.center.y;
        double rad = Math.toRadians(state.angle);
        state.ball = state.center.add(Math.cos(rad) * state.radius, 1.15, Math.sin(rad) * state.radius);
        double prevRad = Math.toRadians(state.angle - 32.0f);
        state.prevBall = state.center.add(Math.cos(prevRad) * state.radius, 1.15, Math.sin(prevRad) * state.radius);
        state.chainStart = state.center.add(0.25, 1.0, 0.15);
    }

    @Override
    public void submit(SpinState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc == null ? null : mc.level;
        if (level == null) {
            return;
        }
        // Orbit holds full presence; release holds through the hit, then fades over the settle.
        float fade = 1.0f;
        if (state.releasing) {
            float settleT = Mth.clamp((state.releaseAge - SpikeFlailSpinEntity.RELEASE_TICKS)
                / (float) SpikeFlailSpinEntity.SETTLE_TICKS, 0.0f, 1.0f);
            fade = 1.0f - FxEnvelope.smoothstep(settleT);
        }
        if (fade <= 0.01f) {
            return;
        }
        float distScale = MaterialEmitters.distanceScale(state.center);
        Emission emission = emissions.get(state.entityId);
        if (emission == null || emission.level != level || emission.seed != state.seed) {
            emission = new Emission(level, (int) state.seed);
            emissions.put(state.entityId, emission);
            pruneEmissions();
        }
        emission.frame.center = state.center;
        emission.frame.ball = state.ball;
        emission.frame.prevBall = state.prevBall;
        emission.frame.chainStart = state.chainStart;
        emission.frame.radius = state.radius;
        emission.frame.groundY = state.groundY;
        float delta = emission.delta(state.age);

        if (!state.releasing) {
            emission.crackBurstFired = false;
            float env = FxEnvelope.envelope(state.age, SpikeFlailItem.CHANNEL_TICKS) * fade * distScale;
            emission.tick(delta, env, 0.0f, 0.0f);
            return;
        }
        float orbitEnv = fade * 0.45f * distScale;
        if (state.releaseAge < SpikeFlailSpinEntity.RELEASE_TICKS) {
            emission.crackBurstFired = false;
            float crackEnv = fade * (0.55f + 0.45f * state.charge) * distScale;
            emission.tick(delta, orbitEnv, crackEnv, 0.0f);
        } else if (state.releaseAge == SpikeFlailSpinEntity.RELEASE_TICKS) {
            emission.tick(delta, orbitEnv, 0.0f, 0.0f);
            if (!emission.crackBurstFired) {
                emission.crackBurstFired = true;
                fireCrackBurst(level, state, distScale);
            }
        } else if (state.releaseAge <= SpikeFlailSpinEntity.RELEASE_TICKS + SpikeFlailSpinEntity.SETTLE_TICKS) {
            emission.tick(delta, orbitEnv * 0.5f, 0.0f, fade * distScale);
        }
    }

    private void fireCrackBurst(ClientLevel level, SpinState state, float distScale) {
        int seed = (int) state.seed;
        MaterialEmitters.Source ground = MaterialEmitters.groundArea(state.center, state.radius * 0.95, state.groundY + 0.12);
        MaterialEmitters.burst(level,
            MaterialEmitters.Preset.dust(THEME_DUST, 1.60f, seed ^ 0xB057, new Vec3(0.0, 0.05, 0.0), 0.05f),
            ground, Math.round((34.0f + 26.0f * state.charge) * distScale));
        MaterialEmitters.burst(level,
            MaterialEmitters.Preset.vapor(0xF5EFE0, 2.40f, seed ^ 0x7E57, new Vec3(0.0, 0.04, 0.0), 0.03f),
            MaterialEmitters.sphereVolume(state.center.add(0.0, 0.35, 0.0), state.radius * 0.55),
            Math.round((16.0f + 10.0f * state.charge) * distScale));
        MaterialEmitters.burst(level,
            MaterialEmitters.Preset.fixed(DbaParticles.SPARK, new Vec3(0.0, 0.10, 0.0), 0.18f),
            ground, Math.round((18.0f + 12.0f * state.charge) * distScale));
        BlockState groundState = level.getBlockState(BlockPos.containing(state.center.x, state.center.y - 0.5, state.center.z));
        if (groundState.isAir()) {
            groundState = Blocks.GRAVEL.defaultBlockState();
        }
        MaterialEmitters.burst(level,
            MaterialEmitters.Preset.fixed(new BlockParticleOption(DbaParticles.BLOCK_CHUNKS, groundState),
                new Vec3(0.0, 0.32, 0.0), 0.16f),
            MaterialEmitters.groundArea(state.center, state.radius * 0.80, state.groundY + 0.10),
            Math.round((14.0f + 10.0f * state.charge) * distScale));
    }

    private void pruneEmissions() {
        if (emissions.size() < 48) {
            return;
        }
        long cutoff = System.nanoTime() - 30_000_000_000L;
        emissions.values().removeIf(e -> e.lastSeenNanos < cutoff);
    }

    /** Mutable per-frame emission data read by the sources. Data only. */
    private static final class Frame {
        Vec3 center = Vec3.ZERO;
        Vec3 ball = Vec3.ZERO;
        Vec3 prevBall = Vec3.ZERO;
        Vec3 chainStart = Vec3.ZERO;
        float radius = 1.0f;
        double groundY;
    }

    private static final class Emission {
        final ClientLevel level;
        final long seed;
        final Frame frame = new Frame();
        final MaterialEmitters.Emitter orbitVapor;
        final MaterialEmitters.Emitter orbitSparks;
        final MaterialEmitters.Emitter chainVapor;
        final MaterialEmitters.Emitter groundDust;
        final MaterialEmitters.Emitter edgeDust;
        final MaterialEmitters.Emitter edgeVapor;
        final MaterialEmitters.Emitter edgePlasma;
        final MaterialEmitters.Emitter settleDust;
        float lastAge = Float.NaN;
        long lastSeenNanos = System.nanoTime();
        boolean crackBurstFired;

        Emission(ClientLevel level, int seed) {
            this.level = level;
            this.seed = seed;
            MaterialEmitters.Source ballPath = random ->
                MaterialEmitters.path(frame.prevBall, frame.ball, 0.0).sample(random);
            MaterialEmitters.Source chainPath = random ->
                MaterialEmitters.path(frame.chainStart, frame.ball, 0.0).sample(random);
            MaterialEmitters.Source groundBall = random ->
                MaterialEmitters.groundArea(new Vec3(frame.ball.x, 0.0, frame.ball.z), 0.65, frame.groundY + 0.12).sample(random);
            MaterialEmitters.Source edge = random -> {
                double a = random.nextDouble() * Math.PI * 2.0;
                double rr = frame.radius * (0.94 + random.nextDouble() * 0.10);
                Vec3 pos = new Vec3(frame.center.x + Math.cos(a) * rr, frame.groundY + 0.15, frame.center.z + Math.sin(a) * rr);
                Vec3 vel = new Vec3(Math.cos(a) * 0.09, 0.025, Math.sin(a) * 0.09);
                return new MaterialEmitters.Sample(pos, vel);
            };
            MaterialEmitters.Source disc = random ->
                MaterialEmitters.groundArea(frame.center, frame.radius * 0.95, frame.groundY + 0.12).sample(random);
            this.orbitVapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(0xEDE8DA, 1.50f, seed ^ 0xA17, Vec3.ZERO, 0.035f),
                ballPath, 130f, 190);
            this.orbitSparks = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK, Vec3.ZERO, 0.12f),
                ballPath, 58f, 90);
            this.chainVapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(0xDAD4C6, 1.00f, seed ^ 0xC4A1, Vec3.ZERO, 0.025f),
                chainPath, 48f, 80);
            this.groundDust = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(THEME_DUST, 1.15f, seed ^ 0xD057, new Vec3(0.0, 0.015, 0.0), 0.050f),
                groundBall, 38f, 65);
            this.edgeDust = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(THEME_DUST, 1.50f, seed ^ 0xE06E, Vec3.ZERO, 0.020f),
                edge, 160f, 230);
            this.edgeVapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(0xF5EFE0, 2.00f, seed ^ 0x7E57, Vec3.ZERO, 0.020f),
                edge, 58f, 95);
            this.edgePlasma = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xFFF2CC, 0.80f, seed ^ 0xCA1D, Vec3.ZERO, 0.015f),
                edge, 72f, 115);
            this.settleDust = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(THEME_DUST, 1.05f, seed ^ 0x5E77, new Vec3(0.0, 0.010, 0.0), 0.040f),
                disc, 38f, 65);
        }

        float delta(float age) {
            lastSeenNanos = System.nanoTime();
            if (Float.isNaN(lastAge) || age < lastAge) {
                lastAge = age;
                return 1.0f / 20.0f;
            }
            float delta = Math.min(0.10f, Math.max(0.0f, (age - lastAge) / 20.0f));
            lastAge = age;
            return delta;
        }

        void tick(float delta, float orbitEnvelope, float crackEnvelope, float settleEnvelope) {
            if (delta <= 0.0f) {
                return;
            }
            if (orbitEnvelope > 0.01f) {
                orbitVapor.tick(delta, orbitEnvelope);
                orbitSparks.tick(delta, orbitEnvelope);
                chainVapor.tick(delta, orbitEnvelope * 0.80f);
                groundDust.tick(delta, orbitEnvelope);
            }
            if (crackEnvelope > 0.01f) {
                edgeDust.tick(delta, crackEnvelope);
                edgeVapor.tick(delta, crackEnvelope);
                edgePlasma.tick(delta, crackEnvelope);
            }
            if (settleEnvelope > 0.01f) {
                settleDust.tick(delta, settleEnvelope);
            }
        }
    }
}
