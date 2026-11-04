package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.EvilSpearChargeEntity;
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
 * Entity renderer for the Evil Spear charge presentation in Minecraft 26.2.
 *
 * No-shapes pass: this renderer draws no geometry. The charge is carried by
 * material emission around the caster: darkness smoke gathering at ground
 * level, hellfire embers and flame moving through the column, a crimson
 * plasma core at spear height, and pale soul vapor drifting through the same
 * volume. The entity still owns the charge ratio, release timing, sounds,
 * and server side particles; this class only replaces the old drawn seals,
 * bands, fans, and dome core with shared material emitters.
 */
public class EvilSpearChargeRenderer extends EntityRenderer<EvilSpearChargeEntity, EvilSpearChargeRenderer.ChargeRenderState> {

    private final Map<Long, EmissionRig> rigs = new HashMap<>();

    public EvilSpearChargeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ChargeRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public float age = 0;
        public int releaseTicks = -1;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(EvilSpearChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(EvilSpearChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ChargeRenderState createRenderState() {
        return new ChargeRenderState();
    }

    @Override
    public void extractRenderState(EvilSpearChargeEntity entity, ChargeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.age = entity.tickCount + partialTicks;
        state.releaseTicks = entity.getReleaseTicks();
        // Synced state hash: the entity UUID is assigned on the server and
        // networked, so every client uses the same material seed family.
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(ChargeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.level != null) {
            ClientLevel level = mc.level;
            Vec3 center = new Vec3(state.x, state.y, state.z);
            float charge = state.chargeRatio;
            float age = state.age;

            float spawnEnv = FxEnvelope.smoothstep(Math.min(1.0f, age / 6.0f));
            float releaseFade = 1.0f;
            float releaseExpand = 1.0f;
            if (state.releaseTicks >= 0) {
                float rt = Math.min(1.0f, state.releaseTicks / (float) EvilSpearChargeEntity.RELEASE_FADE_TICKS);
                releaseFade = 1.0f - rt;
                releaseExpand = 1.0f + rt * 0.35f;
            }
            float lifecycle = spawnEnv * releaseFade;
            float radius = (1.35f + charge * 1.55f) * releaseExpand;
            float distance = MaterialEmitters.distanceScale(center);
            float envelope = lifecycle * distance;

            if (envelope > 0.01f) {
                EmissionRig rig = rigFor(level, state.seed, center, age);
                rig.tick(center, charge, radius, envelope, age);
            }
            pruneRigs();
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    private EmissionRig rigFor(ClientLevel level, long seed, Vec3 center, float age) {
        EmissionRig rig = rigs.get(seed);
        if (rig != null && (rig.level != level || age + 0.01f < rig.lastAge
                || rig.center.distanceToSqr(center) > 64.0 * 64.0)) {
            rigs.remove(seed);
            rig = null;
        }
        if (rig == null) {
            rig = new EmissionRig(level, seed, center);
            rigs.put(seed, rig);
        }
        return rig;
    }

    private void pruneRigs() {
        long now = System.nanoTime();
        rigs.entrySet().removeIf(entry -> now - entry.getValue().lastSeenNanos > 5_000_000_000L);
    }

    /**
     * Accumulator storage for one charge entity. The emitters themselves are
     * the shared MaterialEmitters emitters; the sources only sample current
     * points inside the charge area and column as data.
     */
    private static final class EmissionRig {
        private final ClientLevel level;
        private Vec3 center;
        private float charge = 0f;
        private float radius = 1.35f;
        private float lastAge = 0f;
        private long lastSeenNanos = System.nanoTime();

        private final MaterialEmitters.Emitter darkness;
        private final MaterialEmitters.Emitter embers;
        private final MaterialEmitters.Emitter flame;
        private final MaterialEmitters.Emitter plasma;
        private final MaterialEmitters.Emitter soul;

        private EmissionRig(ClientLevel level, long seed, Vec3 center) {
            this.level = level;
            this.center = center;
            int seedBase = (int) seed;

            MaterialEmitters.Source ground = this::sampleGround;
            MaterialEmitters.Source column = this::sampleColumn;
            MaterialEmitters.Source core = this::sampleCore;

            this.darkness = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.darkness(0x26030B, 2.7f, seedBase ^ 0x0D01,
                            new Vec3(0.0, 0.015, 0.0), 0.015f),
                    column, 64f, 84);
            this.embers = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.ember(new Vec3(0.0, 0.055, 0.0), 0.060f),
                    ground, 52f, 76);
            this.flame = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.flame(new Vec3(0.0, 0.105, 0.0), 0.035f),
                    core, 36f, 56);
            this.plasma = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.plasma(0xF20D33, 1.30f, seedBase ^ 0x0D31,
                            new Vec3(0.0, 0.030, 0.0), 0.025f),
                    core, 66f, 88);
            this.soul = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.soul(0xFFD9E6, 1.15f, seedBase ^ 0x0D47,
                            new Vec3(0.0, 0.020, 0.0), 0.020f),
                    column, 30f, 48);
        }

        private void tick(Vec3 center, float charge, float radius, float envelope, float age) {
            float delta = Math.min(0.05f, Math.max(0.0f, (age - lastAge) / 20.0f));
            this.center = center;
            this.charge = charge;
            this.radius = radius;
            this.lastAge = age;
            this.lastSeenNanos = System.nanoTime();

            float gathering = 0.45f + charge * 0.75f;
            darkness.tick(delta, envelope);
            embers.tick(delta, envelope * gathering);
            flame.tick(delta, envelope * (0.35f + charge * 0.85f));
            plasma.tick(delta, envelope * (0.45f + charge * 0.80f));
            soul.tick(delta, envelope * (0.55f + charge * 0.45f));
        }

        private MaterialEmitters.Sample sampleGround(RandomSource random) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double rr = radius * Math.sqrt(random.nextDouble());
            return new MaterialEmitters.Sample(new Vec3(
                    center.x + Math.cos(angle) * rr,
                    center.y + 0.08,
                    center.z + Math.sin(angle) * rr), Vec3.ZERO);
        }

        private MaterialEmitters.Sample sampleColumn(RandomSource random) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double rr = radius * (0.20 + random.nextDouble() * 0.80);
            return new MaterialEmitters.Sample(new Vec3(
                    center.x + Math.cos(angle) * rr,
                    center.y + random.nextDouble() * 2.45,
                    center.z + Math.sin(angle) * rr), Vec3.ZERO);
        }

        private MaterialEmitters.Sample sampleCore(RandomSource random) {
            double u = random.nextDouble() * 2.0 - 1.0;
            double angle = random.nextDouble() * Math.PI * 2.0;
            double rr = (0.34 + charge * 0.30) * Math.cbrt(random.nextDouble());
            double flat = Math.sqrt(Math.max(0.0, 1.0 - u * u)) * rr;
            return new MaterialEmitters.Sample(new Vec3(
                    center.x + Math.cos(angle) * flat,
                    center.y + 0.88 + u * rr,
                    center.z + Math.sin(angle) * flat), Vec3.ZERO);
        }
    }
}
