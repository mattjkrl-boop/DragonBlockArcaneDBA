package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.HellHuntImpactEntity;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Entity renderer for Hell Hunt impacts in Minecraft 26.2.
 *
 * Complex pass: a layered composition at class M. A crackFan of energy
 * cracks runs across the impact ground through the additive shader, hot
 * at the center and crimson at the tips, and four darkness tendril
 * lashes burst outward and upward from the center through the veil
 * shader. The complex particle layer is EMBER_FRAGMENT sprites, burst
 * at the opening and emitted across the ground while the impact lives,
 * plus SPARK_STREAK sprites thrown from the ground area. The material
 * emission from the no-shapes pass stays as filler over the real scar
 * and debris the entity already creates: darkness smoke across the
 * ground, hellfire embers, flame and pale soul vapor in the short
 * execution column, and crimson plasma close to the ground. The entity
 * already spawns a DebrisSpawner field server side for the big physical
 * chunks; these pieces are the fast near field structure only.
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
        float distScale = MaterialEmitters.distanceScale(center);
        float envelope = Math.max(fade, opening) * distScale;
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

        // Pieces layer, in local space (pose origin is the impact point,
        // crackFan draws in the local XZ plane). The crack fan runs to the
        // same spread radius as the ground emission, and four one-count
        // tendrilFan lashes burst outward and up. At distScale 0.66 each
        // lash is about 256 vertices and the crack fan is roughly 500 to
        // 900, for roughly 1500 to 1900 total, inside the class M budget
        // of 2500.
        final long pieceSeed = state.seed;
        final float pieceEnv = Math.max(fade, opening);
        final float pieceDs = distScale;
        final float crackRadius = spreadRadius;
        final float lashLength = Math.max(0.9f, spreadRadius * 0.62f);
        final float lashEnv = pieceEnv * Math.max(0.35f, eruptionPulse);
        ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) ->
                ComplexGeometry.crackFan(pose, buffer, pieceSeed ^ 0xC9AC1L, crackRadius,
                        0xFFFFFF, 0xF20D33, 0.95f, pieceEnv, pieceDs));
        ComplexGeometry.submitVeil(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
            for (int i = 0; i < 4; i++) {
                double ang = i * Math.PI * 0.5 + (pieceSeed & 63L) / 63.0 * 0.7;
                Vec3 lashDir = new Vec3(Math.cos(ang) * 0.82, 0.78, Math.sin(ang) * 0.82).normalize();
                ComplexGeometry.tendrilFan(pose, buffer, pieceSeed ^ (0x1A51L + i * 131L),
                        new Vec3(0.0, 0.10, 0.0), lashDir, 6f, lashLength, 1,
                        0.27f, 0xF20D33, 0x26030B, 0.92f, lashEnv, pieceDs * 0.66f);
            }
        });

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
        private final MaterialEmitters.Preset emberFragmentPreset;
        private final MaterialEmitters.Preset flamePreset;
        private final MaterialEmitters.Preset plasmaPreset;
        private final MaterialEmitters.Preset soulPreset;

        private final MaterialEmitters.Source ground;
        private final MaterialEmitters.Source fullGround;
        private final MaterialEmitters.Source column;

        private final MaterialEmitters.Emitter darkness;
        private final MaterialEmitters.Emitter embers;
        private final MaterialEmitters.Emitter emberFragments;
        private final MaterialEmitters.Emitter sparkStreaks;
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
            this.emberFragmentPreset = MaterialEmitters.Preset.fixed(DbaParticles.EMBER_FRAGMENT,
                    new Vec3(0.0, 0.165, 0.0), 0.120f);
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
            // Complex particle layer: irregular ember fragments across
            // the ground area and spark streaks thrown from the same
            // area, both inside the continuous rate cap.
            this.emberFragments = new MaterialEmitters.Emitter(level, emberFragmentPreset, ground, 26f, 34);
            this.sparkStreaks = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK,
                            new Vec3(0.0, 0.165, 0.0), 0.140f),
                    ground, 20f, 28);
            this.flame = new MaterialEmitters.Emitter(level, flamePreset, column, 58f, 78);
            this.plasma = new MaterialEmitters.Emitter(level, plasmaPreset, ground, 62f, 84);
            this.soul = new MaterialEmitters.Emitter(level, soulPreset, column, 36f, 54);
        }

        private void burstOpening() {
            MaterialEmitters.burst(level, flamePreset, column, finalImpact ? 22 : 14);
            MaterialEmitters.burst(level, emberPreset, fullGround, finalImpact ? 30 : 20);
            // Complex sprite burst at the opening beat, inside the class
            // M burst budget of 30 on its own.
            MaterialEmitters.burst(level, emberFragmentPreset, fullGround, finalImpact ? 16 : 12);
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
            emberFragments.tick(delta, envelope * (0.45f + 0.65f * eruptionPulse));
            sparkStreaks.tick(delta, envelope * (0.45f + 0.65f * eruptionPulse));
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
