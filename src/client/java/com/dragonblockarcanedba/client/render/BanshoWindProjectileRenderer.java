package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.BanshoWindProjectileEntity;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
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
 * Layered presentation for the Bansho wind projectile (class M). The
 * pieces layer is a double tendril spiral wake: two ComplexGeometry
 * tendrils whose bezier control points sit on opposite sides of the
 * flight axis and rotate with the same 0.80 spin phase the body
 * sampling uses, so the pair twists around the axis behind the head,
 * drawn through the shader submit helper with the tendril canvas under
 * the same yaw and pitch rotation the emission frame uses. This is a
 * pair of torn, pinched strands, not the deleted clean helix mesh. The
 * complex particle layer is WISP_CURL sprites in the wake. The material
 * emission from the no-shapes pass stays as filler: a dense emerald
 * vapor cluster along the flight path, a pale plasma core on the axis,
 * airborne dust behind, and mist in the wake. The age envelope and
 * first-person owner scale are unchanged, and the piece seeds come
 * from the entity id, the same synced state the emission already used.
 */
public class BanshoWindProjectileRenderer extends EntityRenderer<BanshoWindProjectileEntity, BanshoWindProjectileRenderer.WindProjectileRenderState> {

    public BanshoWindProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class WindProjectileRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public float ageInTicks = 0;
        public boolean isFirstPersonOwner = false;
        public double ex, ey, ez;
        public long seed;
        public float drillRadius;
        public Emission emission;
    }

    static final class Emission {
        private final WindProjectileRenderState state;
        private ClientLevel level;
        private long seed;
        private boolean firstPersonOwner;
        private float lastAge = Float.NaN;
        private MaterialEmitters.Emitter vapor;
        private MaterialEmitters.Emitter plasma;
        private MaterialEmitters.Emitter dust;
        private MaterialEmitters.Emitter mist;
        private MaterialEmitters.Emitter wisps;

        Emission(WindProjectileRenderState state) {
            this.state = state;
        }

        void tick(ClientLevel level, float envelope) {
            if (this.level != level || this.seed != state.seed
                    || this.firstPersonOwner != state.isFirstPersonOwner) {
                create(level);
            }
            float delta;
            if (Float.isNaN(lastAge) || state.ageInTicks < lastAge) {
                delta = 1.0f / 60.0f;
            } else {
                delta = Math.min(0.05f, (state.ageInTicks - lastAge) / 20.0f);
            }
            lastAge = state.ageInTicks;
            vapor.tick(delta, envelope);
            plasma.tick(delta, envelope);
            dust.tick(delta, envelope);
            mist.tick(delta, envelope);
            wisps.tick(delta, envelope);
        }

        private void create(ClientLevel level) {
            this.level = level;
            this.seed = state.seed;
            this.firstPersonOwner = state.isFirstPersonOwner;
            this.lastAge = Float.NaN;
            int seed = (int) state.seed;
            float ownerScale = state.isFirstPersonOwner ? 0.55f : 1.0f;
            vapor = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.vapor(0x26FFA6, 0.95f * ownerScale,
                            seed ^ 31, Vec3.ZERO, 0.008f),
                    random -> bodySample(state, random, false), 135.0f, 170);
            plasma = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.plasma(0xD8FFF2, 0.58f * ownerScale,
                            seed ^ 41, Vec3.ZERO, 0.004f),
                    random -> bodySample(state, random, true), 55.0f, 80);
            dust = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.dust(0x86C9A6, 0.75f * ownerScale,
                            seed ^ 53, Vec3.ZERO, 0.012f),
                    random -> dustTrailSample(state, random), 38.0f, 60);
            mist = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.mist(0x9FFFE0, 1.35f * ownerScale,
                            seed ^ 67, Vec3.ZERO, 0.006f),
                    random -> mistSample(state, random), 26.0f, 45);
            // Complex particle layer: wisp curls shed in the wake, under
            // the 40 per second continuous budget.
            wisps = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.fixed(DbaParticles.WISP_CURL,
                            Vec3.ZERO, 0.02f),
                    random -> dustTrailSample(state, random), 24.0f, 40);
        }
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(BanshoWindProjectileEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(BanshoWindProjectileEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public WindProjectileRenderState createRenderState() {
        return new WindProjectileRenderState();
    }

    @Override
    public void extractRenderState(BanshoWindProjectileEntity entity, WindProjectileRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.ageInTicks = entity.tickCount + partialTicks;
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
        state.seed = entity.getId() * 0x9E3779B97F4A7C15L ^ 0xD4115EEDL;

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null &&
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) &&
            mc.options.getCameraType().isFirstPerson());
    }

    @Override
    public void submit(WindProjectileRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float age = state.ageInTicks;
        float ownerScale = state.isFirstPersonOwner ? 0.55f : 1.0f;
        state.drillRadius = 0.32f * ownerScale
                * (1.0f + 0.05f * (float) Math.sin(age * 1.5f));

        // Age envelope: in over the first 3 ticks, out over the last 8 before
        // the 50 tick range cap, so the projectile never pops in or vanishes.
        float inT = Math.min(1.0f, age / 3.0f);
        float inEase = 1.0f - (1.0f - inT) * (1.0f - inT) * (1.0f - inT);
        float outT = age <= 42.0f ? 0.0f : Math.min(1.0f, (age - 42.0f) / 8.0f);
        float envelope = inEase * (1.0f - outT * outT * (3.0f - 2.0f * outT));

        float distanceScale = MaterialEmitters.distanceScale(new Vec3(state.ex, state.ey, state.ez));
        if (envelope > 0.01f) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                if (state.emission == null) state.emission = new Emission(state);
                state.emission.tick(level, envelope * distanceScale);
            }
        }

        // Pieces layer: the double tendril spiral wake in the renderer's
        // local frame (x right, y up, z forward). Both strands run from
        // the head at z 0.72 to the wake tail at z minus 2.05; their
        // control points sit on opposite sides of the axis at the body
        // sampling spin phase (age times 0.80), so the pair rotates
        // around the axis as one twisting wake. Vertex count at full
        // distance scale: 2 tendrils times 24 samples times 16 vertices
        // is 768, inside the class M budget of 2500.
        if (envelope > 0.01f && distanceScale > 0.01f) {
            double spin = state.ageInTicks * 0.80;
            final float ctrlX = (float) Math.cos(spin) * 0.30f * ownerScale;
            final float ctrlY = (float) Math.sin(spin) * 0.30f * ownerScale;
            final Vec3 head = new Vec3(0.0, 0.0, 0.72f * ownerScale);
            final Vec3 tail = new Vec3(0.0, 0.0, -2.05f * ownerScale);
            final Vec3 controlA = new Vec3(ctrlX, ctrlY, -0.55f * ownerScale);
            final Vec3 controlB = new Vec3(-ctrlX, -ctrlY, -0.55f * ownerScale);
            final long geoSeed = state.seed;
            final float geoEnv = envelope * distanceScale;
            final float geoDs = distanceScale;
            final float geoWidth = 0.26f * ownerScale;
            poseStack.pushPose();
            poseStack.mulPose(new Matrix4f()
                    .rotationY((float) Math.toRadians(-state.yRot))
                    .rotateX((float) Math.toRadians(state.xRot)));
            ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
                ComplexGeometry.tendril(pose, buffer, geoSeed ^ 0xD411L,
                        head, controlA, tail,
                        geoWidth, 0xD8FFF2, 0x26FFA6, 0.92f, geoEnv, geoDs);
                ComplexGeometry.tendril(pose, buffer, geoSeed ^ 0xD422L,
                        head, controlB, tail,
                        geoWidth, 0xD8FFF2, 0x26FFA6, 0.92f, geoEnv, geoDs);
            });
            poseStack.popPose();
        }
        super.submit(state, poseStack, collector, cameraState);
    }

    private static MaterialEmitters.Sample bodySample(WindProjectileRenderState state, RandomSource random, boolean core) {
        float ownerScale = state.isFirstPersonOwner ? 0.55f : 1.0f;
        float t = random.nextFloat();
        Vec3 center = new Vec3(state.ex, state.ey, state.ez);
        Vec3 axisPoint = center.add(forward(state).scale((-0.72f + t * 1.50f) * ownerScale));
        float profile = core
                ? 0.30f
                : 0.38f + 0.62f * (float) Math.sin(Math.PI * t);
        float radius = state.drillRadius * profile
                * (core ? (float) random.nextDouble() : 0.35f + 0.65f * (float) random.nextDouble());
        double angle = random.nextDouble() * Math.PI * 2.0 + state.ageInTicks * 0.80;
        Vec3 radial = right(state).scale(Math.cos(angle)).add(up(state).scale(Math.sin(angle)));
        Vec3 tangent = right(state).scale(-Math.sin(angle)).add(up(state).scale(Math.cos(angle)));
        Vec3 pos = axisPoint.add(radial.scale(radius));
        Vec3 velocity = forward(state).scale(core ? 0.035 : 0.018)
                .add(tangent.scale(core ? 0.025 : 0.055))
                .add(radial.scale(0.008));
        return new MaterialEmitters.Sample(pos, velocity);
    }

    private static MaterialEmitters.Sample dustTrailSample(WindProjectileRenderState state, RandomSource random) {
        float ownerScale = state.isFirstPersonOwner ? 0.55f : 1.0f;
        float t = random.nextFloat();
        Vec3 center = new Vec3(state.ex, state.ey, state.ez);
        Vec3 axisPoint = center.add(forward(state).scale(-(0.55f + 1.60f * t) * ownerScale));
        double angle = random.nextDouble() * Math.PI * 2.0;
        Vec3 radial = right(state).scale(Math.cos(angle)).add(up(state).scale(Math.sin(angle)));
        Vec3 tangent = right(state).scale(-Math.sin(angle)).add(up(state).scale(Math.cos(angle)));
        Vec3 pos = axisPoint.add(radial.scale(state.drillRadius * (0.30f + random.nextDouble() * 0.55f)));
        Vec3 velocity = forward(state).scale(-0.005).add(tangent.scale(0.02));
        return new MaterialEmitters.Sample(pos, velocity);
    }

    private static MaterialEmitters.Sample mistSample(WindProjectileRenderState state, RandomSource random) {
        float ownerScale = state.isFirstPersonOwner ? 0.55f : 1.0f;
        Vec3 center = new Vec3(state.ex, state.ey, state.ez)
                .add(forward(state).scale(-0.85f * ownerScale));
        MaterialEmitters.Sample base = MaterialEmitters.sphereVolume(center, 0.38f * ownerScale).sample(random);
        return new MaterialEmitters.Sample(base.pos(), forward(state).scale(0.006));
    }

    private static Vec3 right(WindProjectileRenderState state) {
        double yaw = Math.toRadians(state.yRot);
        return new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
    }

    private static Vec3 up(WindProjectileRenderState state) {
        double yaw = Math.toRadians(state.yRot);
        double pitch = Math.toRadians(state.xRot);
        return new Vec3(-Math.sin(pitch) * Math.sin(yaw), Math.cos(pitch),
                Math.sin(pitch) * Math.cos(yaw));
    }

    private static Vec3 forward(WindProjectileRenderState state) {
        double yaw = Math.toRadians(state.yRot);
        double pitch = Math.toRadians(state.xRot);
        return new Vec3(-Math.cos(pitch) * Math.sin(yaw), -Math.sin(pitch),
                Math.cos(pitch) * Math.cos(yaw));
    }
}
