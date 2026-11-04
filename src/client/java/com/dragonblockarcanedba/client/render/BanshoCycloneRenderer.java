package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.BanshoCycloneEntity;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Layered presentation for the Bansho Fan cyclone (class L). The pieces
 * layer is a tendrilFan at the full fan budget: six tall wind strands in
 * a cone around the column, rotated with the cyclone age so they swirl
 * with the flow, drawn through the shader submit helper with the tendril
 * canvas. The complex particle layer is ASH_FLAKE carried through the
 * whole volume and SHARD_CHIP orbiting in the column wall. The material
 * emission from the no-shapes pass stays as filler: emerald vapor, pale
 * mist, a bright vapor core, and ground dust sampled by the cyclone's
 * current radius and height. The lifecycle envelope remains the entity's
 * own build and decay envelope, and every seed comes from the entity id,
 * the same synced state the emission already used.
 */
public class BanshoCycloneRenderer extends EntityRenderer<BanshoCycloneEntity, BanshoCycloneRenderer.CycloneRenderState> {

    public BanshoCycloneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class CycloneRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public float age = 0;
        public int lifetime = BanshoCycloneEntity.LIFE_TICKS;
        public double ex, ey, ez;
        public long seed;
        public Emission emission;
    }

    static final class Emission {
        private final CycloneRenderState state;
        private ClientLevel level;
        private long seed;
        private float scale = Float.NaN;
        private float lastAge = Float.NaN;
        private MaterialEmitters.Emitter vapor;
        private MaterialEmitters.Emitter mist;
        private MaterialEmitters.Emitter dust;
        private MaterialEmitters.Emitter core;
        private MaterialEmitters.Emitter ash;
        private MaterialEmitters.Emitter chips;

        Emission(CycloneRenderState state) {
            this.state = state;
        }

        void tick(ClientLevel level, float envelope) {
            if (this.level != level || this.seed != state.seed || this.scale != state.scale) {
                create(level);
            }
            float delta;
            if (Float.isNaN(lastAge) || state.age < lastAge) {
                delta = 1.0f / 60.0f;
            } else {
                delta = Math.min(0.05f, (state.age - lastAge) / 20.0f);
            }
            lastAge = state.age;
            vapor.tick(delta, envelope);
            mist.tick(delta, envelope);
            dust.tick(delta, envelope);
            core.tick(delta, envelope);
            ash.tick(delta, envelope);
            chips.tick(delta, envelope);
        }

        private void create(ClientLevel level) {
            this.level = level;
            this.seed = state.seed;
            this.scale = state.scale;
            this.lastAge = Float.NaN;
            int seed = (int) state.seed;
            float rateScale = 0.75f + state.scale * 0.25f;
            vapor = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.vapor(0x26FFA6, 1.30f + state.scale * 0.25f,
                            seed ^ 11, Vec3.ZERO, 0.012f),
                    random -> cycloneSample(state, random, 1.0f, 0.15f, 1.0f, 0.11f, 0.040f),
                    120.0f * rateScale, Math.max(1, Math.round(170.0f * rateScale)));
            mist = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.mist(0x8FFFD8, 2.0f,
                            seed ^ 23, Vec3.ZERO, 0.006f),
                    random -> cycloneSample(state, random, 1.0f, 0.78f, 1.08f, 0.07f, 0.018f),
                    45.0f * rateScale, Math.max(1, Math.round(70.0f * rateScale)));
            dust = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.dust(0x86C9A6, 0.95f,
                            seed ^ 37, Vec3.ZERO, 0.018f),
                    random -> groundSample(state, random),
                    55.0f * rateScale, Math.max(1, Math.round(85.0f * rateScale)));
            core = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.vapor(0xCFFFEF, 1.0f,
                            seed ^ 53, Vec3.ZERO, 0.008f),
                    random -> cycloneSample(state, random, 0.45f, 0.0f, 0.38f, 0.08f, 0.060f),
                    35.0f * rateScale, Math.max(1, Math.round(55.0f * rateScale)));
            // Complex particle layer: ash flakes carried through the whole
            // volume and stone chips orbiting in the column wall, both
            // under the 40 per second continuous budget.
            ash = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.fixed(DbaParticles.ASH_FLAKE,
                            Vec3.ZERO, 0.02f),
                    random -> cycloneSample(state, random, 1.0f, 0.30f, 1.05f, 0.13f, 0.030f),
                    24.0f * rateScale, 40);
            chips = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP,
                            Vec3.ZERO, 0.03f),
                    random -> cycloneSample(state, random, 0.85f, 0.60f, 1.08f, 0.16f, 0.035f),
                    20.0f * rateScale, 40);
        }
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(BanshoCycloneEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(BanshoCycloneEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public CycloneRenderState createRenderState() {
        return new CycloneRenderState();
    }

    @Override
    public void extractRenderState(BanshoCycloneEntity entity, CycloneRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getScale();
        state.age = entity.tickCount + partialTicks;
        state.lifetime = entity.getLifetime();
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
        // The entity id arrives in the spawn packet, so it is synced state:
        // every client seeds the same material variation for the same storm.
        state.seed = entity.getId() * 0x9E3779B97F4A7C15L ^ 0xBA550FADL;
    }

    @Override
    public void submit(CycloneRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float life = Math.max(1, state.lifetime);
        float envelope = FxEnvelope.envelope(state.age, life,
                BanshoCycloneEntity.BUILD_FRAC, BanshoCycloneEntity.DECAY_FRAC);
        float distanceScale = MaterialEmitters.distanceScale(new Vec3(state.ex, state.ey, state.ez));
        if (envelope > 0.01f) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                if (state.emission == null) state.emission = new Emission(state);
                state.emission.tick(level, envelope * distanceScale);
            }
        }

        // Pieces layer, class L: a six strand tendrilFan at the full fan
        // budget. The 32 degree cone matches the grown funnel profile
        // (radius over height), so the strands hug the column wall instead
        // of crossing the core. Rotation is applied inside the piece on a
        // copy of the submitted pose, at a rate whose surface speed at the
        // wall matches the filler tangential velocity. Vertex count at
        // full distance scale: 6 tendrils times 24 samples times 16
        // vertices is 2304, inside the class L budget of 6000, and the
        // fan count alone meets the full budget rule for one builder.
        if (envelope > 0.01f && distanceScale > 0.01f) {
            final float geoLen = heightNow(state) * 1.10f;
            final float geoWidth = 0.55f * Math.max(0.6f, Math.min(1.6f, state.scale));
            final float geoAge = state.age;
            final float geoEnv = envelope * distanceScale;
            final float geoDs = distanceScale;
            final long geoSeed = state.seed ^ 0xC7C10L;
            ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
                PoseStack local = new PoseStack();
                local.last().pose().set(pose.pose());
                local.last().normal().set(pose.normal());
                local.pushPose();
                local.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(geoAge * 2.0f)));
                ComplexGeometry.tendrilFan(local.last(), buffer, geoSeed, Vec3.ZERO,
                        new Vec3(0.0, 1.0, 0.0), 32f, geoLen, 6, geoWidth,
                        0x26FFA6, 0xCFFFEF, 0.92f, geoEnv, geoDs);
                local.popPose();
            });
        }
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float growth(CycloneRenderState state) {
        float life = Math.max(1, state.lifetime);
        return FxEnvelope.smoothstep(state.age / (life * BanshoCycloneEntity.BUILD_FRAC));
    }

    private static float radiusNow(CycloneRenderState state) {
        return state.scale * 4.0f * (0.45f + 0.55f * growth(state));
    }

    private static float heightNow(CycloneRenderState state) {
        return state.scale * 6.5f * (0.35f + 0.65f * growth(state));
    }

    private static MaterialEmitters.Sample cycloneSample(CycloneRenderState state, RandomSource random,
                                                         float yMax, float inner, float outer,
                                                         float tangentSpeed, float upSpeed) {
        float t = random.nextFloat() * yMax;
        float profile = 0.35f + 0.65f * t * t;
        double angle = random.nextDouble() * Math.PI * 2.0 + state.age * 0.38;
        float fraction = inner + (outer - inner) * (float) Math.sqrt(random.nextDouble());
        float radius = radiusNow(state) * profile * fraction;
        Vec3 pos = new Vec3(
                state.ex + Math.cos(angle) * radius,
                state.ey + t * heightNow(state),
                state.ez + Math.sin(angle) * radius);
        Vec3 tangent = new Vec3(-Math.sin(angle), 0.0, Math.cos(angle));
        Vec3 radial = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
        Vec3 velocity = tangent.scale(tangentSpeed * (0.75 + random.nextDouble() * 0.5))
                .add(radial.scale((random.nextDouble() - 0.5) * 0.03))
                .add(0.0, upSpeed * (0.6 + random.nextDouble() * 0.8), 0.0);
        return new MaterialEmitters.Sample(pos, velocity);
    }

    private static MaterialEmitters.Sample groundSample(CycloneRenderState state, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        float radius = radiusNow(state) * 0.95f * (float) Math.sqrt(random.nextDouble());
        Vec3 pos = new Vec3(
                state.ex + Math.cos(angle) * radius,
                state.ey + 0.12,
                state.ez + Math.sin(angle) * radius);
        Vec3 tangent = new Vec3(-Math.sin(angle), 0.0, Math.cos(angle));
        Vec3 radial = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
        Vec3 velocity = tangent.scale(0.15)
                .add(radial.scale(0.05))
                .add(0.0, 0.015 + random.nextDouble() * 0.035, 0.0);
        return new MaterialEmitters.Sample(pos, velocity);
    }
}
