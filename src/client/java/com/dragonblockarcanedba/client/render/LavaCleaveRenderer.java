package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.LavaCleaveEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The traveling cleave is a swept volume of material data, not a drawn
 * crescent. Flame, embers, ash, smoke, and heat vapor are sampled across
 * the area the swing has just passed through and die with its envelope.
 */
public class LavaCleaveRenderer extends EntityRenderer<LavaCleaveEntity, LavaCleaveRenderer.CleaveState> {
    private final Map<Integer, Emission> emissions = new HashMap<>();

    public LavaCleaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class CleaveState extends EntityRenderState {
        public int entityId;
        public float age;
        public float yRot;
        public float xRot;
        public float life;
        public int seed;
        public double posX;
        public double posY;
        public double posZ;
        public double velX;
        public double velY;
        public double velZ;
    }

    public boolean shouldRender(LavaCleaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(LavaCleaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public CleaveState createRenderState() {
        return new CleaveState();
    }

    @Override
    public void extractRenderState(LavaCleaveEntity entity, CleaveState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.entityId = entity.getId();
        state.age = entity.tickCount + partialTick;
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.life = entity.life();
        state.seed = entity.getSeed();
        state.posX = entity.getX();
        state.posY = entity.getY();
        state.posZ = entity.getZ();
        Vec3 velocity = entity.getDeltaMovement();
        state.velX = velocity.x;
        state.velY = velocity.y;
        state.velZ = velocity.z;
    }

    @Override
    public void submit(CleaveState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && state.entityId != 0) {
            Vec3 center = new Vec3(state.posX, state.posY, state.posZ);
            float distance = MaterialEmitters.distanceScale(center);
            float env = FxEnvelope.envelope(state.age, state.life);
            float open = ease(Math.min(1.0f, state.age / 2.5f));
            float trailT = ease(Math.max(0.0f, 1.0f - state.age / (state.life * 0.7f)));
            float strength = distance * Math.max(env, 0.30f * open * trailT);
            float radius = 1.30f * (0.4f + 0.6f * open);
            Emission emission = emissions.get(state.entityId);
            if (emission == null || emission.level != level || emission.seed != state.seed
                    || state.age + 0.001f < emission.lastAge) {
                emission = new Emission(level, state);
                emissions.put(state.entityId, emission);
            }
            emission.update(state, strength, radius);
        }
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float ease(float t) {
        float c = Mth.clamp(t, 0.0f, 1.0f);
        return c * c * (3.0f - 2.0f * c);
    }

    private static final class Emission {
        private final ClientLevel level;
        private final int seed;
        private final List<MaterialEmitters.Emitter> emitters = new ArrayList<>();
        private Vec3 center;
        private Vec3 previous;
        private Vec3 velocity;
        private Vec3 travel = new Vec3(0.0, 0.0, 1.0);
        private Vec3 diameter = new Vec3(0.0, 1.0, 0.0);
        private double step;
        private double radius = 1.0;
        private float lastAge;

        private Emission(ClientLevel level, CleaveState state) {
            this.level = level;
            this.seed = state.seed;
            this.center = position(state);
            this.previous = this.center;
            this.velocity = velocity(state);
            this.lastAge = state.age;
            updateDirections();

            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.flame(Vec3.ZERO, 0.018f),
                    random -> sampleSwept(random, 1.0), 620f, 760));
            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.ember(Vec3.ZERO, 0.025f),
                    random -> sampleSwept(random, 1.0), 240f, 320));
            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.ash(new Vec3(0.0, -0.020, 0.0), 0.022f),
                    this::sampleDrip, 62f, 90));
            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.smoke(0x35150B, 0.84f, seed ^ 0xC1EA,
                            new Vec3(0.0, 0.012, 0.0), 0.020f),
                    random -> sampleSwept(random, 1.15), 105f, 150));
            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.vapor(0xB46226, 0.92f, seed ^ 0x9A11,
                            new Vec3(0.0, 0.008, 0.0), 0.018f),
                    random -> sampleSwept(random, 1.05), 72f, 110));
        }

        private void update(CleaveState state, float strength, float swathRadius) {
            float delta = Math.max(0.0f, Math.min(0.05f, (state.age - lastAge) / 20.0f));
            lastAge = state.age;
            previous = center;
            center = position(state);
            velocity = velocity(state);
            radius = swathRadius;
            step = center.distanceTo(previous);
            if (step < 0.001) {
                step = velocity.length();
            }
            updateDirections();
            for (MaterialEmitters.Emitter emitter : emitters) {
                emitter.tick(delta, strength);
            }
        }

        private void updateDirections() {
            if (velocity.lengthSqr() > 0.000001) {
                travel = velocity.normalize();
            }
            Vec3 upRef = Math.abs(travel.y) > 0.85 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
            Vec3 projected = upRef.subtract(travel.scale(upRef.dot(travel)));
            if (projected.lengthSqr() > 0.000001) {
                diameter = projected.normalize();
            }
        }

        private MaterialEmitters.Sample sampleSwept(RandomSource random, double radialScale) {
            double along = (random.nextDouble() * 2.0 - 1.0) * radius * radialScale;
            double back = random.nextDouble();
            Vec3 pos = center.add(diameter.scale(along)).add(travel.scale(-back * step));
            pos = pos.add(jitter(random, 0.08));
            return new MaterialEmitters.Sample(pos, diameter.scale(along * 0.010));
        }

        private MaterialEmitters.Sample sampleDrip(RandomSource random) {
            double along = -radius * (0.20 + random.nextDouble() * 0.80);
            double back = random.nextDouble();
            Vec3 pos = center.add(diameter.scale(along)).add(travel.scale(-back * step));
            pos = pos.add(jitter(random, 0.10));
            return new MaterialEmitters.Sample(pos, new Vec3(0.0, -0.055, 0.0));
        }

        private static Vec3 jitter(RandomSource random, double amount) {
            return new Vec3((random.nextDouble() * 2.0 - 1.0) * amount,
                    (random.nextDouble() * 2.0 - 1.0) * amount,
                    (random.nextDouble() * 2.0 - 1.0) * amount);
        }

        private static Vec3 position(CleaveState state) {
            return new Vec3(state.posX, state.posY, state.posZ);
        }

        private static Vec3 velocity(CleaveState state) {
            return new Vec3(state.velX, state.velY, state.velZ);
        }
    }
}
