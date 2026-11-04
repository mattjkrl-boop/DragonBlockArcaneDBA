package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.HellHuntImpactEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Entity renderer for Hell Hunt impacts in Minecraft 26.2.
 *
 * No-shapes pass: this renderer draws no geometry. The impact is carried by
 * material emission over the real scar, debris, and server side particles
 * that the entity already creates: darkness smoke across the ground,
 * hellfire embers thrown from the impact area, flame and pale soul vapor in
 * the short execution column, and crimson plasma close to the ground. The
 * old shock bands, crack stars, shard fans, anchor glow, vortex bands, and
 * plume pillar are gone.
 */
public class HellHuntImpactRenderer extends EntityRenderer<HellHuntImpactEntity, HellHuntImpactRenderer.HellHuntRenderState> {

    private final Map<Long, EmissionRig> rigs = new HashMap<>();

    public HellHuntImpactRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class HellHuntRenderState extends EntityRenderState {
        public float radius = 3.0f;
        public boolean isFinal = false;
        public float age = 0;
        public int lifetime = 35;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(HellHuntImpactEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(HellHuntImpactEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public HellHuntRenderState createRenderState() {
        return new HellHuntRenderState();
    }

    @Override
    public void extractRenderState(HellHuntImpactEntity entity, HellHuntRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.isFinal = entity.isFinal();
        state.age = entity.tickCount + partialTicks;
        // The client side entity never runs the convenience constructor, so
        // the total life comes from the synced final flag and the shared
        // constants, not from the entity's server side countdown field.
        state.lifetime = state.isFinal ? HellHuntImpactEntity.LIFE_FINAL : HellHuntImpactEntity.LIFE_NORMAL;
        // The entity UUID is assigned on the server and networked, so every
        // client uses the same material seed family.
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(HellHuntRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        ClientLevel level = mc.level;
        Vec3 center = new Vec3(state.x, state.y, state.z);
        float age = state.age;
        float fade = FxEnvelope.envelope(age, state.lifetime);
        float opening = age < 2.0f ? 0.78f : 0.0f;
        float envelope = Math.max(fade, opening) * MaterialEmitters.distanceScale(center);
        if (envelope <= 0.01f) return;

        float growT = Math.min(1.0f, age / 10.0f);
        float grow = 1.0f - (1.0f - growT) * (1.0f - growT) * (1.0f - growT);
        float spreadRadius = state.radius * 1.35f * Math.max(0.25f, grow);

        float lifeT = Math.min(1.0f, age / (float) state.lifetime);
        float riseT = Math.min(1.0f, age / 8.0f);
        float rise = 1.0f - (1.0f - riseT) * (1.0f - riseT) * (1.0f - riseT);
        float sinkRaw = lifeT < 0.55f ? 0.0f : (lifeT - 0.55f) / 0.45f;
        float sink = FxEnvelope.smoothstep(sinkRaw);
        float eruptionPulse = rise * (1.0f - sink);

        EmissionRig rig = rigFor(level, state.seed, center, state.radius, state.isFinal, age);
        rig.tick(spreadRadius, envelope, eruptionPulse, age, state.isFinal);
        pruneRigs();
    }

    private EmissionRig rigFor(ClientLevel level, long seed, Vec3 center, float radius, boolean isFinal, float age) {
        EmissionRig rig = rigs.get(seed);
        if (rig != null && (rig.level != level || age + 0.01f < rig.lastAge
                || rig.center.distanceToSqr(center) > 16.0)) {
            rigs.remove(seed);
            rig = null;
        }
        if (rig == null) {
            rig = new EmissionRig(level, seed, center, radius, isFinal);
            rigs.put(seed, rig);
            rig.burstOpening();
        }
        return rig;
    }

    private void pruneRigs() {
        long now = System.nanoTime();
        rigs.entrySet().removeIf(entry -> now - entry.getValue().lastSeenNanos > 5_000_000_000L);
    }

    /**
     * Accumulator storage for one impact. Sources sample the ground area and
     * the short column volume as data; the shared emitters own the particle
     * rates, budgets, density scaling, and seed progression.
     */
    private static final class EmissionRig {
        private final ClientLevel level;
        private final Vec3 center;
        private final float radius;
        private final boolean finalImpact;
        private float spreadRadius;
        private float lastAge = 0f;
        private long lastSeenNanos = System.nanoTime();

        private final MaterialEmitters.Preset darknessPreset;
        private final MaterialEmitters.Preset emberPreset;
        private final MaterialEmitters.Preset flamePreset;
        private final MaterialEmitters.Preset plasmaPreset;
        private final MaterialEmitters.Preset soulPreset;

        private final MaterialEmitters.Source ground;
        private final MaterialEmitters.Source fullGround;
        private final MaterialEmitters.Source column;

        private final MaterialEmitters.Emitter darkness;
        private final MaterialEmitters.Emitter embers;
        private final MaterialEmitters.Emitter flame;
        private final MaterialEmitters.Emitter plasma;
        private final MaterialEmitters.Emitter soul;

        private EmissionRig(ClientLevel level, long seed, Vec3 center, float radius, boolean finalImpact) {
            this.level = level;
            this.center = center;
            this.radius = radius;
            this.finalImpact = finalImpact;
            this.spreadRadius = radius;
            int seedBase = (int) seed;

            this.darknessPreset = MaterialEmitters.Preset.darkness(0x20030A, 3.0f, seedBase ^ 0xA101,
                    new Vec3(0.0, 0.012, 0.0), 0.020f);
            this.emberPreset = MaterialEmitters.Preset.ember(new Vec3(0.0, 0.165, 0.0), 0.120f);
            this.flamePreset = MaterialEmitters.Preset.flame(new Vec3(0.0, 0.150, 0.0), 0.040f);
            this.plasmaPreset = MaterialEmitters.Preset.plasma(0xF20D33, 1.45f, seedBase ^ 0xA131,
                    new Vec3(0.0, 0.035, 0.0), 0.040f);
            this.soulPreset = MaterialEmitters.Preset.soul(0xFFD9E6, 1.20f, seedBase ^ 0xA147,
                    new Vec3(0.0, 0.060, 0.0), 0.020f);

            this.ground = this::sampleGround;
            this.fullGround = this::sampleFullGround;
            this.column = this::sampleColumn;

            this.darkness = new MaterialEmitters.Emitter(level, darknessPreset, ground, 72f, 96);
            this.embers = new MaterialEmitters.Emitter(level, emberPreset, ground, 68f, 92);
            this.flame = new MaterialEmitters.Emitter(level, flamePreset, column, 58f, 78);
            this.plasma = new MaterialEmitters.Emitter(level, plasmaPreset, ground, 62f, 84);
            this.soul = new MaterialEmitters.Emitter(level, soulPreset, column, 36f, 54);
        }

        private void burstOpening() {
            MaterialEmitters.burst(level, flamePreset, column, finalImpact ? 22 : 14);
            MaterialEmitters.burst(level, emberPreset, fullGround, finalImpact ? 30 : 20);
            MaterialEmitters.burst(level, darknessPreset, fullGround, finalImpact ? 18 : 12);
            MaterialEmitters.burst(level, plasmaPreset, fullGround, finalImpact ? 16 : 10);
        }

        private void tick(float spreadRadius, float envelope, float eruptionPulse, float age, boolean isFinal) {
            float delta = Math.min(0.05f, Math.max(0.0f, (age - lastAge) / 20.0f));
            this.spreadRadius = spreadRadius;
            this.lastAge = age;
            this.lastSeenNanos = System.nanoTime();

            float pillarLife = isFinal ? 18.0f : 12.0f;
            float columnEnvelope = envelope * (age < pillarLife ? 1.0f : 0.14f);
            float groundEnvelope = envelope * (0.55f + 0.45f * eruptionPulse);

            darkness.tick(delta, groundEnvelope);
            embers.tick(delta, envelope * (0.45f + 0.65f * eruptionPulse));
            flame.tick(delta, columnEnvelope);
            plasma.tick(delta, groundEnvelope);
            soul.tick(delta, columnEnvelope * 0.85f);
        }

        private MaterialEmitters.Sample sampleGround(RandomSource random) {
            return sampleDisc(random, spreadRadius);
        }

        private MaterialEmitters.Sample sampleFullGround(RandomSource random) {
            return sampleDisc(random, radius * 1.15);
        }

        private MaterialEmitters.Sample sampleDisc(RandomSource random, double sampleRadius) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double rr = sampleRadius * Math.sqrt(random.nextDouble());
            return new MaterialEmitters.Sample(new Vec3(
                    center.x + Math.cos(angle) * rr,
                    center.y + 0.07,
                    center.z + Math.sin(angle) * rr), Vec3.ZERO);
        }

        private MaterialEmitters.Sample sampleColumn(RandomSource random) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double rr = radius * 0.42 * Math.sqrt(random.nextDouble());
            double height = finalImpact ? 4.2 : 3.0;
            return new MaterialEmitters.Sample(new Vec3(
                    center.x + Math.cos(angle) * rr,
                    center.y + 0.10 + random.nextDouble() * height,
                    center.z + Math.sin(angle) * rr), Vec3.ZERO);
        }
    }
}
