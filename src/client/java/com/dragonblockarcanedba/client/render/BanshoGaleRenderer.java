package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.BanshoGaleEntity;
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
 * Layered presentation for the Bansho Fan gale (class M). The pieces
 * layer is three ComplexGeometry tendrils laid along the fanPoint rib
 * curves (the center rib and the two ribs at side plus and minus 2.3),
 * so the fan has a torn strand body inside the spray, drawn through the
 * shader submit helper with the tendril canvas under the same yaw and
 * pitch rotation the emission frame uses. The complex particle layer is
 * WISP_CURL sprites shed across the fan surface. The material emission
 * from the no-shapes pass stays as filler: emerald vapor across the
 * opening fan surface, pale plasma along the nine rib paths, airborne
 * dust from the tips, and mist near the origin. The open, follow, and
 * tip dissolve timing is unchanged, and the piece seeds come from the
 * entity id, the same synced state the emission already used.
 */
public class BanshoGaleRenderer extends EntityRenderer<BanshoGaleEntity, BanshoGaleRenderer.GaleState> {
    public BanshoGaleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class GaleState extends EntityRenderState {
        public float yRot;
        public float xRot;
        public float age;
        public double ex, ey, ez;
        public long seed;
        public float open;
        public float follow;
        public float spread;
        public float lift;
        public float fade;
        public Emission emission;
    }

    static final class Emission {
        private final GaleState state;
        private ClientLevel level;
        private long seed;
        private float lastAge = Float.NaN;
        private MaterialEmitters.Emitter vapor;
        private MaterialEmitters.Emitter plasma;
        private MaterialEmitters.Emitter dust;
        private MaterialEmitters.Emitter mist;
        private MaterialEmitters.Emitter wisps;

        Emission(GaleState state) {
            this.state = state;
        }

        void tick(ClientLevel level, float envelope) {
            if (this.level != level || this.seed != state.seed) {
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
            plasma.tick(delta, envelope);
            dust.tick(delta, envelope);
            mist.tick(delta, envelope);
            wisps.tick(delta, envelope);
        }

        private void create(ClientLevel level) {
            this.level = level;
            this.seed = state.seed;
            this.lastAge = Float.NaN;
            int seed = (int) state.seed;
            vapor = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.vapor(0x26FFA6, 1.05f,
                            seed ^ 11, Vec3.ZERO, 0.008f),
                    random -> fanSample(state, random, 0), 150.0f, 190);
            plasma = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.plasma(0xD8FFF2, 0.62f,
                            seed ^ 23, Vec3.ZERO, 0.004f),
                    random -> fanSample(state, random, 1), 75.0f, 100);
            dust = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.dust(0x86C9A6, 0.80f,
                            seed ^ 37, Vec3.ZERO, 0.010f),
                    random -> fanSample(state, random, 2), 45.0f, 70);
            mist = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.mist(0x9FFFE0, 1.45f,
                            seed ^ 53, Vec3.ZERO, 0.006f),
                    random -> fanSample(state, random, 3), 35.0f, 55);
            // Complex particle layer: wisp curls shed across the fan
            // surface, under the 40 per second continuous budget.
            wisps = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.fixed(DbaParticles.WISP_CURL,
                            Vec3.ZERO, 0.02f),
                    random -> fanSample(state, random, 0), 28.0f, 40);
        }
    }

    public boolean shouldRender(BanshoGaleEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(BanshoGaleEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public GaleState createRenderState() {
        return new GaleState();
    }

    @Override
    public void extractRenderState(BanshoGaleEntity entity, GaleState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTick;
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
        state.seed = entity.getId() * 0x9E3779B97F4A7C15L ^ 0x6A1E5EEDL;
    }

    @Override
    public void submit(GaleState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        // Opens over 6 ticks with an ease-out, then the tips dissolve across the last 4.
        float openT = Math.min(1.0f, state.age / 6.0f);
        float open = 1.0f - (1.0f - openT) * (1.0f - openT) * (1.0f - openT);
        float follow = openT >= 1.0f ? Math.min(1.0f, (state.age - 6.0f) / 4.0f) : 0.0f;
        float fade = 1.0f - follow * follow * (3.0f - 2.0f * follow);
        state.open = open;
        state.follow = follow;
        state.spread = (0.20f + open * 1.05f) * (1.0f + follow * 0.06f);
        state.lift = 0.10f * open * (1.0f - follow * 0.45f);
        state.fade = fade;

        float distanceScale = MaterialEmitters.distanceScale(new Vec3(state.ex, state.ey, state.ez));
        float envelope = fade * (0.30f + 0.70f * open) * distanceScale;
        if (fade > 0.01f) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                if (state.emission == null) state.emission = new Emission(state);
                state.emission.tick(level, envelope);
            }
        }

        // Pieces layer: three tendrils along the fanPoint rib curves in
        // the renderer's own local frame (x right, y up, z forward), the
        // same frame localPoint builds for the emission. The center rib
        // carries the body; the flanking ribs at side plus and minus 2.3
        // give the fan its width. Vertex count at full distance scale:
        // 3 tendrils times 24 samples times 16 vertices is 1152, inside
        // the class M budget of 2500.
        if (envelope > 0.01f && distanceScale > 0.01f) {
            final Vec3 centerFrom = fanVec(state, 0.0f, 0.0f);
            final Vec3 centerControl = fanVec(state, 0.0f, 0.5f);
            final Vec3 centerTo = fanVec(state, 0.0f, 1.0f);
            final Vec3 leftFrom = fanVec(state, -2.3f, 0.0f);
            final Vec3 leftControl = fanVec(state, -2.3f, 0.5f);
            final Vec3 leftTo = fanVec(state, -2.3f, 1.0f);
            final Vec3 rightFrom = fanVec(state, 2.3f, 0.0f);
            final Vec3 rightControl = fanVec(state, 2.3f, 0.5f);
            final Vec3 rightTo = fanVec(state, 2.3f, 1.0f);
            final long geoSeed = state.seed;
            final float geoEnv = envelope;
            final float geoDs = distanceScale;
            poseStack.pushPose();
            poseStack.mulPose(new Matrix4f()
                    .rotationY((float) Math.toRadians(-state.yRot))
                    .rotateX((float) Math.toRadians(state.xRot)));
            ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
                ComplexGeometry.tendril(pose, buffer, geoSeed ^ 0x6A11L,
                        centerFrom, centerControl, centerTo,
                        0.34f, 0xD8FFF2, 0x26FFA6, 0.95f, geoEnv, geoDs);
                ComplexGeometry.tendril(pose, buffer, geoSeed ^ 0x6A22L,
                        leftFrom, leftControl, leftTo,
                        0.20f, 0x26FFA6, 0x9FFFE0, 0.90f, geoEnv, geoDs);
                ComplexGeometry.tendril(pose, buffer, geoSeed ^ 0x6A33L,
                        rightFrom, rightControl, rightTo,
                        0.20f, 0x26FFA6, 0x9FFFE0, 0.90f, geoEnv, geoDs);
            });
            poseStack.popPose();
        }
        super.submit(state, poseStack, collector, cameraState);
    }

    /** One fanPoint rib point in the renderer's local frame. */
    private static Vec3 fanVec(GaleState state, float side, float t) {
        float[] local = new float[3];
        fanPoint(side, t, state.spread, state.open, 2.85f, 0.14f, state.lift, local);
        return new Vec3(local[0], local[1], local[2]);
    }

    private static MaterialEmitters.Sample fanSample(GaleState state, RandomSource random, int mode) {
        float side;
        float t;
        if (mode == 1) {
            side = random.nextInt(9) - 4;
            t = (float) Math.pow(random.nextDouble(), 0.80);
        } else if (mode == 2) {
            side = random.nextFloat() * 8.0f - 4.0f;
            t = 0.50f + random.nextFloat() * 0.50f;
        } else if (mode == 3) {
            side = (random.nextFloat() * 8.0f - 4.0f) * 0.70f;
            t = random.nextFloat() * 0.40f;
        } else {
            side = random.nextFloat() * 8.0f - 4.0f;
            t = (float) Math.pow(random.nextDouble(), 0.85);
        }
        float[] local = new float[3];
        fanPoint(side, t, state.spread, state.open, 2.85f, 0.14f, state.lift, local);
        Vec3 pos = localPoint(state, local);
        if (mode == 3) {
            pos = pos.add(forward(state).scale(-random.nextDouble() * 0.45));
        }
        double speed = switch (mode) {
            case 1 -> 0.13 + 0.08 * t;
            case 2 -> 0.05;
            case 3 -> 0.025;
            default -> 0.08 + 0.08 * t;
        };
        Vec3 velocity = forward(state).scale(speed)
                .add(up(state).scale(mode == 2 ? -0.006 : 0.004));
        return new MaterialEmitters.Sample(pos, velocity);
    }

    private static Vec3 right(GaleState state) {
        double yaw = Math.toRadians(state.yRot);
        return new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
    }

    private static Vec3 up(GaleState state) {
        double yaw = Math.toRadians(state.yRot);
        double pitch = Math.toRadians(state.xRot);
        return new Vec3(-Math.sin(pitch) * Math.sin(yaw), Math.cos(pitch),
                Math.sin(pitch) * Math.cos(yaw));
    }

    private static Vec3 forward(GaleState state) {
        double yaw = Math.toRadians(state.yRot);
        double pitch = Math.toRadians(state.xRot);
        return new Vec3(-Math.cos(pitch) * Math.sin(yaw), -Math.sin(pitch),
                Math.cos(pitch) * Math.cos(yaw));
    }

    private static Vec3 localPoint(GaleState state, float[] local) {
        return new Vec3(state.ex, state.ey, state.ez)
                .add(right(state).scale(local[0]))
                .add(up(state).scale(local[1]))
                .add(forward(state).scale(local[2]));
    }

    private static void fanPoint(float side, float t, float spread, float open, float len, float near, float lift, float[] o) {
        float base = side * spread * 0.30f;
        float curl = side * 0.065f * open;
        float ang = base + curl * t * t;
        float along = t * (0.88f + 0.12f * (float) Math.sin(t * (float) Math.PI));
        float rad = near + (len - near) * along;
        float bowMag = (float) Math.sin(t * Math.PI) * 0.22f * open;
        float bowSign = side < 0.0f ? -1.0f : (side > 0.0f ? 1.0f : 0.0f);
        float bow = bowMag * bowSign * Math.min(1.0f, Math.abs(side) / 3.2f);
        o[0] = (float) (Math.sin(ang) * rad + Math.cos(ang) * bow);
        o[2] = (float) (Math.cos(ang) * rad - Math.sin(ang) * bow);
        o[1] = (float) Math.sin(t * Math.PI * 0.92) * (0.05f + lift) * (0.30f + 0.70f * t);
    }
}
