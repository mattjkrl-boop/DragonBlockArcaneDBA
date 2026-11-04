package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.BanshoShockwaveEntity;
import com.dragonblockarcanedba.particle.DbaParticles;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Material presentation for Bansho Fan shockwaves. The renderer draws no
 * geometry and uses no disc, ring, cone, fan, or arc construction. Cone
 * waves become a rushing vapor front with a pale plasma core, launch dust,
 * and mist. Radial waves become a vapor front sampled across the expanding
 * area, a center vapor wash, ground dust, and real block chunks where the
 * burst is close enough to a captured ground block. The 14 tick progress
 * and exit envelope are unchanged.
 */
public class BanshoShockwaveRenderer extends EntityRenderer<BanshoShockwaveEntity, BanshoShockwaveRenderer.ShockwaveRenderState> {

    public BanshoShockwaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ShockwaveRenderState extends EntityRenderState {
        public float yaw = 0;
        public float pitch = 0;
        public float maxRadius = 3.0f;
        public boolean isCone = false;
        public boolean orientToImpact = false;
        public float age = 0;
        public double ex, ey, ez;
        public long seed;
        public float progress;
        public float alpha;
        public float coneLength;
        public float coneRadius;
        public float currentRadius;
        public float crestHeight;
        public float coreRadius;
        public Emission emission;
    }

    static final class Emission {
        private final ShockwaveRenderState state;
        private ClientLevel level;
        private long seed;
        private boolean cone;
        private boolean orient;
        private float lastAge = Float.NaN;
        private MaterialEmitters.Emitter vapor;
        private MaterialEmitters.Emitter plasma;
        private MaterialEmitters.Emitter launchDust;
        private MaterialEmitters.Emitter mist;
        private MaterialEmitters.Emitter center;
        private MaterialEmitters.Emitter groundDust;
        private MaterialEmitters.Emitter debris;
        private BlockState debrisGround;

        Emission(ShockwaveRenderState state) {
            this.state = state;
        }

        void tick(ClientLevel level, float envelope) {
            if (this.level != level || this.seed != state.seed
                    || this.cone != state.isCone || this.orient != state.orientToImpact) {
                create(level);
            }
            updateDebris(level);
            float delta;
            if (Float.isNaN(lastAge) || state.age < lastAge) {
                delta = 1.0f / 60.0f;
            } else {
                delta = Math.min(0.05f, (state.age - lastAge) / 20.0f);
            }
            lastAge = state.age;
            tick(vapor, delta, envelope);
            tick(plasma, delta, envelope);
            tick(launchDust, delta, envelope);
            tick(mist, delta, envelope);
            tick(center, delta, envelope);
            tick(groundDust, delta, envelope);
            tick(debris, delta, envelope);
        }

        private static void tick(MaterialEmitters.Emitter emitter, float delta, float envelope) {
            if (emitter != null) emitter.tick(delta, envelope);
        }

        private void create(ClientLevel level) {
            this.level = level;
            this.seed = state.seed;
            this.cone = state.isCone;
            this.orient = state.orientToImpact;
            this.lastAge = Float.NaN;
            this.debris = null;
            this.debrisGround = null;
            int seed = (int) state.seed;
            if (state.isCone) {
                vapor = new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.vapor(0x26FFA6, 1.20f,
                                seed ^ 61, Vec3.ZERO, 0.010f),
                        random -> coneSample(state, random, false), 175.0f, 220);
                plasma = new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(0xD8FFF2, 0.68f,
                                seed ^ 67, Vec3.ZERO, 0.005f),
                        random -> coneSample(state, random, true), 75.0f, 100);
                launchDust = new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.dust(0x86C9A6, 0.85f,
                                seed ^ 71, Vec3.ZERO, 0.016f),
                        random -> {
                            MaterialEmitters.Sample base = MaterialEmitters.sphereVolume(
                                    new Vec3(state.ex, state.ey, state.ez),
                                    Math.max(0.20f, state.coneRadius * 0.35f)).sample(random);
                            return new MaterialEmitters.Sample(base.pos(), forward(state).scale(0.08));
                        }, 55.0f, 80);
                mist = new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.mist(0x9FFFE0, 1.50f,
                                seed ^ 79, Vec3.ZERO, 0.006f),
                        random -> MaterialEmitters.sphereVolume(
                                new Vec3(state.ex, state.ey, state.ez), 0.30f).sample(random),
                        35.0f, 55);
                center = null;
                groundDust = null;
            } else {
                vapor = new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.vapor(0x26FFA6, 1.20f,
                                seed ^ 83, Vec3.ZERO, 0.010f),
                        random -> radialFrontSample(state, random), 185.0f, 240);
                center = new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.vapor(0xCFFFEF, 1.05f,
                                seed ^ 89, Vec3.ZERO, 0.008f),
                        random -> centerSample(state, random), 65.0f, 90);
                groundDust = new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.dust(0x86C9A6, 0.90f,
                                seed ^ 97, Vec3.ZERO, 0.016f),
                        random -> groundDustSample(state, random), 85.0f, 120);
                plasma = null;
                launchDust = null;
                mist = null;
            }
        }

        private void updateDebris(ClientLevel level) {
            if (state.orientToImpact) {
                debris = null;
                debrisGround = null;
                return;
            }
            BlockPos origin = BlockPos.containing(state.ex, state.ey, state.ez);
            BlockPos groundPos = origin.below();
            BlockState ground = level.getBlockState(groundPos);
            double groundY = groundPos.getY() + 1.0;
            if (ground.isAir() || state.ey - groundY > 1.6) {
                debris = null;
                debrisGround = null;
                return;
            }
            if (debris != null && debrisGround == ground) return;
            debrisGround = ground;
            debris = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.fixed(
                            new BlockParticleOption(DbaParticles.BLOCK_CHUNKS, ground),
                            Vec3.ZERO, 0.05f),
                    random -> debrisSample(state, random, groundY),
                    24.0f, 36);
        }
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(BanshoShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(BanshoShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ShockwaveRenderState createRenderState() {
        return new ShockwaveRenderState();
    }

    @Override
    public void extractRenderState(BanshoShockwaveEntity entity, ShockwaveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yaw = entity.getEntityYaw();
        state.pitch = entity.getEntityPitch();
        state.maxRadius = entity.getMaxRadius();
        state.isCone = entity.isCone();
        state.orientToImpact = entity.isOrientToImpact();
        state.age = entity.tickCount + partialTicks;
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
        state.seed = entity.getId() * 0x9E3779B97F4A7C15L ^ (int) (entity.getMaxRadius() * 64.0f) ^ 0x510C4L;
    }

    @Override
    public void submit(ShockwaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float maxLife = 14.0f;
        float progress = Math.min(1.0f, state.age / maxLife);
        if (progress >= 1.0f) return;

        float exitT = progress <= 0.55f ? 0.0f : Math.min(1.0f, (progress - 0.55f) / 0.45f);
        float alpha = (1.0f - exitT * exitT * (3.0f - 2.0f * exitT)) * 0.95f;
        state.progress = progress;
        state.alpha = alpha;

        if (state.isCone) {
            float expansion = (float) Math.sin(progress * (Math.PI / 2.0));
            state.coneLength = state.maxRadius * (0.4f + 0.8f * expansion);
            state.coneRadius = state.maxRadius * (0.3f + 0.9f * expansion);
        } else {
            float expansionEase = (float) Math.sin(progress * (Math.PI / 2.0));
            state.currentRadius = Math.max(0.4f, expansionEase * state.maxRadius);
            state.crestHeight = 0.55f * (1.0f - progress * 0.8f);
            state.coreRadius = Math.max(0.1f, (state.maxRadius * 0.3f) * (1.0f - progress));
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && alpha > 0.01f) {
            if (state.emission == null) state.emission = new Emission(state);
            float distanceScale = MaterialEmitters.distanceScale(new Vec3(state.ex, state.ey, state.ez));
            state.emission.tick(level, alpha * distanceScale);
        }
        super.submit(state, poseStack, collector, cameraState);
    }

    private static MaterialEmitters.Sample coneSample(ShockwaveRenderState state, RandomSource random, boolean core) {
        float axialT = random.nextFloat();
        float z = state.coneLength * (0.08f + 0.92f * axialT);
        float localRadius = state.coneRadius * (0.18f + 0.82f * axialT) * (core ? 0.36f : 1.0f);
        double angle = random.nextDouble() * Math.PI * 2.0;
        float radius = localRadius * (float) Math.sqrt(random.nextDouble());
        float[] local = new float[] {
                (float) Math.cos(angle) * radius,
                (float) Math.sin(angle) * radius,
                z
        };
        Vec3 pos = localPoint(state, local);
        Vec3 radial = localDirection(state, new Vec3(Math.cos(angle), Math.sin(angle), 0.0));
        Vec3 velocity = forward(state).scale(core ? 0.22 : 0.16)
                .add(radial.scale(0.045))
                .add(up(state).scale(0.005));
        return new MaterialEmitters.Sample(pos, velocity);
    }

    private static MaterialEmitters.Sample radialFrontSample(ShockwaveRenderState state, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        float radius = state.currentRadius * (0.86f + random.nextFloat() * 0.22f);
        float height = random.nextFloat() * (state.crestHeight + 0.18f);
        if (state.orientToImpact) {
            float[] local = new float[] {
                    (float) Math.cos(angle) * radius,
                    height,
                    (float) Math.sin(angle) * radius
            };
            Vec3 radial = localDirection(state, new Vec3(Math.cos(angle), 0.0, Math.sin(angle)));
            Vec3 velocity = radial.scale(0.15).add(up(state).scale(0.02));
            return new MaterialEmitters.Sample(localPoint(state, local), velocity);
        }
        Vec3 pos = new Vec3(
                state.ex + Math.cos(angle) * radius,
                state.ey + height,
                state.ez + Math.sin(angle) * radius);
        Vec3 velocity = new Vec3(Math.cos(angle), 0.0, Math.sin(angle)).scale(0.15)
                .add(0.0, 0.025, 0.0);
        return new MaterialEmitters.Sample(pos, velocity);
    }

    private static MaterialEmitters.Sample centerSample(ShockwaveRenderState state, RandomSource random) {
        Vec3 center = new Vec3(state.ex, state.ey, state.ez);
        MaterialEmitters.Sample base = MaterialEmitters.sphereVolume(center, state.coreRadius).sample(random);
        Vec3 offset = base.pos().subtract(center);
        Vec3 velocity = offset.lengthSqr() > 1.0E-6
                ? offset.normalize().scale(0.035)
                : Vec3.ZERO;
        Vec3 up = state.orientToImpact ? up(state) : new Vec3(0.0, 1.0, 0.0);
        return new MaterialEmitters.Sample(base.pos(), velocity.add(up.scale(0.02)));
    }

    private static MaterialEmitters.Sample groundDustSample(ShockwaveRenderState state, RandomSource random) {
        if (state.orientToImpact) {
            MaterialEmitters.Sample front = radialFrontSample(state, random);
            return new MaterialEmitters.Sample(front.pos(), front.velocity().scale(0.65));
        }
        double angle = random.nextDouble() * Math.PI * 2.0;
        float radius = state.currentRadius * (float) Math.sqrt(random.nextDouble());
        Vec3 pos = new Vec3(
                state.ex + Math.cos(angle) * radius,
                state.ey + 0.12,
                state.ez + Math.sin(angle) * radius);
        Vec3 velocity = new Vec3(Math.cos(angle), 0.0, Math.sin(angle)).scale(0.10)
                .add(0.0, 0.03, 0.0);
        return new MaterialEmitters.Sample(pos, velocity);
    }

    private static MaterialEmitters.Sample debrisSample(ShockwaveRenderState state, RandomSource random,
                                                        double groundY) {
        float debrisRadius = state.isCone
                ? Math.max(0.45f, state.coneRadius * 0.45f)
                : Math.max(0.45f, state.currentRadius * 0.92f);
        double angle = random.nextDouble() * Math.PI * 2.0;
        float radius = debrisRadius * (float) Math.sqrt(random.nextDouble());
        Vec3 pos = new Vec3(
                state.ex + Math.cos(angle) * radius,
                groundY + 0.10,
                state.ez + Math.sin(angle) * radius);
        Vec3 velocity = new Vec3(Math.cos(angle), 0.0, Math.sin(angle)).scale(0.10)
                .add(0.0, 0.07 + random.nextDouble() * 0.05, 0.0);
        return new MaterialEmitters.Sample(pos, velocity);
    }

    private static Vec3 right(ShockwaveRenderState state) {
        double yaw = Math.toRadians(state.yaw);
        return new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
    }

    private static Vec3 up(ShockwaveRenderState state) {
        double yaw = Math.toRadians(state.yaw);
        double pitch = Math.toRadians(state.pitch);
        return new Vec3(-Math.sin(pitch) * Math.sin(yaw), Math.cos(pitch),
                Math.sin(pitch) * Math.cos(yaw));
    }

    private static Vec3 forward(ShockwaveRenderState state) {
        double yaw = Math.toRadians(state.yaw);
        double pitch = Math.toRadians(state.pitch);
        return new Vec3(-Math.cos(pitch) * Math.sin(yaw), -Math.sin(pitch),
                Math.cos(pitch) * Math.cos(yaw));
    }

    private static Vec3 localPoint(ShockwaveRenderState state, float[] local) {
        return new Vec3(state.ex, state.ey, state.ez)
                .add(right(state).scale(local[0]))
                .add(up(state).scale(local[1]))
                .add(forward(state).scale(local[2]));
    }

    private static Vec3 localDirection(ShockwaveRenderState state, Vec3 local) {
        return right(state).scale(local.x)
                .add(up(state).scale(local.y))
                .add(forward(state).scale(local.z));
    }
}
