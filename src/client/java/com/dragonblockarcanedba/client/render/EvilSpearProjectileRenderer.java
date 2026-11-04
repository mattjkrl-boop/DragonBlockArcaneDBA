package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.EvilSpearProjectileEntity;
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
 * Entity renderer for the Evil Spear projectile in Minecraft 26.2.
 *
 * No-shapes pass: the thrown spear is spectral energy, so this renderer no
 * longer draws a spear body, head, barbs, sheath, trail ribbons, or wake
 * bands. It emits a dense material cluster at the projectile position and a
 * material trail along the actual movement segment: crimson plasma at the
 * core, darkness smoke in the wake, hellfire embers shed along the path,
 * flame at the leading cluster, and pale soul vapor through the cluster.
 * The projectile entity still owns its flight, pierce, vortex, impact
 * entities, sounds, and server side trail particles.
 */
public class EvilSpearProjectileRenderer extends EntityRenderer<EvilSpearProjectileEntity, EvilSpearProjectileRenderer.EvilSpearRenderState> {

    private final Map<Long, EmissionRig> rigs = new HashMap<>();

    public EvilSpearProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class EvilSpearRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public boolean isFirstPersonOwner = false;
        public long seed = 0;
        public Vec3 velocity = Vec3.ZERO;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(EvilSpearProjectileEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(EvilSpearProjectileEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public EvilSpearRenderState createRenderState() {
        return new EvilSpearRenderState();
    }

    @Override
    public void extractRenderState(EvilSpearProjectileEntity entity, EvilSpearRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
        state.velocity = entity.getDeltaMovement();

        Minecraft mc = Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null &&
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) &&
            mc.options.getCameraType().isFirstPerson());
    }

    @Override
    public void submit(EvilSpearRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.level != null) {
            ClientLevel level = mc.level;
            Vec3 center = new Vec3(state.x, state.y, state.z);
            float age = state.age;

            float spawnT = Math.min(1.0f, age / 3.0f);
            float spawnFade = FxEnvelope.smoothstep(spawnT);
            float endT = age > 32.0f ? Math.min(1.0f, (age - 32.0f) / 8.0f) : 0.0f;
            float bodyFade = spawnFade * (1.0f - FxEnvelope.smoothstep(endT));
            float firstPersonFactor = state.isFirstPersonOwner && age < 5.0f
                    ? 0.55f + 0.45f * spawnFade
                    : 1.0f;
            float envelope = bodyFade * firstPersonFactor * MaterialEmitters.distanceScale(center);

            if (envelope > 0.01f) {
                EmissionRig rig = rigFor(level, state.seed, center, age);
                rig.tick(center, state.velocity, envelope, age);
            }
            pruneRigs();
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    private EmissionRig rigFor(ClientLevel level, long seed, Vec3 center, float age) {
        EmissionRig rig = rigs.get(seed);
        if (rig != null && (rig.level != level || age + 0.01f < rig.lastAge
                || rig.current.distanceToSqr(center) > 64.0 * 64.0)) {
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
     * Accumulator storage for one projectile. The shared emitters sample a
     * cluster around the current position and a trail along the segment from
     * the previous rendered position to the current one.
     */
    private static final class EmissionRig {
        private final ClientLevel level;
        private Vec3 previous;
        private Vec3 current;
        private Vec3 velocity = Vec3.ZERO;
        private float lastAge = 0f;
        private long lastSeenNanos = System.nanoTime();

        private final MaterialEmitters.Emitter plasma;
        private final MaterialEmitters.Emitter darkness;
        private final MaterialEmitters.Emitter embers;
        private final MaterialEmitters.Emitter flame;
        private final MaterialEmitters.Emitter soul;

        private EmissionRig(ClientLevel level, long seed, Vec3 center) {
            this.level = level;
            this.previous = center;
            this.current = center;
            int seedBase = (int) seed;

            MaterialEmitters.Source cluster = this::sampleCluster;
            MaterialEmitters.Source trail = this::sampleTrail;
            MaterialEmitters.Source wake = this::sampleWake;

            this.plasma = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.plasma(0xF20D33, 1.35f, seedBase ^ 0x5A31,
                            Vec3.ZERO, 0.015f),
                    cluster, 86f, 112);
            this.darkness = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.darkness(0x26030B, 2.05f, seedBase ^ 0x5A47,
                            Vec3.ZERO, 0.020f),
                    wake, 48f, 68);
            this.embers = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.ember(Vec3.ZERO, 0.050f),
                    trail, 44f, 64);
            this.flame = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.flame(Vec3.ZERO, 0.020f),
                    cluster, 28f, 44);
            this.soul = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.soul(0xFFD9E6, 1.05f, seedBase ^ 0x5A61,
                            Vec3.ZERO, 0.020f),
                    cluster, 26f, 42);
        }

        private void tick(Vec3 center, Vec3 velocity, float envelope, float age) {
            float delta = Math.min(0.05f, Math.max(0.0f, (age - lastAge) / 20.0f));
            this.previous = this.current;
            this.current = center;
            this.velocity = velocity == null ? Vec3.ZERO : velocity;
            this.lastAge = age;
            this.lastSeenNanos = System.nanoTime();

            plasma.tick(delta, envelope);
            darkness.tick(delta, envelope * 0.85f);
            embers.tick(delta, envelope);
            flame.tick(delta, envelope * 0.80f);
            soul.tick(delta, envelope * 0.70f);
        }

        private MaterialEmitters.Sample sampleCluster(RandomSource random) {
            Vec3 back = velocity.lengthSqr() > 1.0e-6
                    ? velocity.normalize().scale(-0.55 + random.nextDouble() * 0.80)
                    : Vec3.ZERO;
            Vec3 pos = current.add(back).add(jitter(random, 0.17));
            return new MaterialEmitters.Sample(pos, velocity.scale(0.04));
        }

        private MaterialEmitters.Sample sampleTrail(RandomSource random) {
            Vec3 pos = previous.add(current.subtract(previous).scale(random.nextDouble()))
                    .add(jitter(random, 0.23));
            return new MaterialEmitters.Sample(pos, velocity.scale(-0.015));
        }

        private MaterialEmitters.Sample sampleWake(RandomSource random) {
            Vec3 back = velocity.lengthSqr() > 1.0e-6
                    ? velocity.normalize().scale(-0.65 - random.nextDouble() * 0.85)
                    : Vec3.ZERO;
            Vec3 pos = current.add(back).add(jitter(random, 0.42));
            return new MaterialEmitters.Sample(pos, velocity.scale(-0.03));
        }

        private static Vec3 jitter(RandomSource random, double extent) {
            return new Vec3(
                    (random.nextDouble() * 2.0 - 1.0) * extent,
                    (random.nextDouble() * 2.0 - 1.0) * extent,
                    (random.nextDouble() * 2.0 - 1.0) * extent);
        }
    }
}
