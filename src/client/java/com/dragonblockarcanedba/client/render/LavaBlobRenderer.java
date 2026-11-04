package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.LavaBlobEntity;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The lava blob is presented as a dense cluster of living material rather
 * than a rendered body: flame flipbooks form the hot center, embers shed
 * along the flight path, and ash, smoke, and heat vapor drift off it.
 */
public class LavaBlobRenderer extends EntityRenderer<LavaBlobEntity, LavaBlobRenderer.BlobState> {
    private final Map<Integer, Emission> emissions = new HashMap<>();

    public LavaBlobRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class BlobState extends EntityRenderState {
        public int entityId;
        public float age;
        public int seed;
        public double posX;
        public double posY;
        public double posZ;
        public double velX;
        public double velY;
        public double velZ;
    }

    public boolean shouldRender(LavaBlobEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(LavaBlobEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public BlobState createRenderState() {
        return new BlobState();
    }

    @Override
    public void extractRenderState(LavaBlobEntity entity, BlobState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.entityId = entity.getId();
        state.age = entity.tickCount + partialTick;
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
    public void submit(BlobState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && state.entityId != 0) {
            Vec3 center = new Vec3(state.posX, state.posY, state.posZ);
            float distance = MaterialEmitters.distanceScale(center);
            float env = Math.max(0.30f, FxEnvelope.envelope(state.age, 240.0f));
            Emission emission = emissions.get(state.entityId);
            if (emission == null || emission.level != level || emission.seed != state.seed
                    || state.age + 0.001f < emission.lastAge) {
                emission = new Emission(level, state);
                emissions.put(state.entityId, emission);
            }
            emission.update(state, env * distance);
        }
        super.submit(state, poseStack, collector, cameraState);
    }

    private static final class Emission {
        private final ClientLevel level;
        private final int seed;
        private final List<MaterialEmitters.Emitter> emitters = new ArrayList<>();
        private Vec3 center;
        private Vec3 previous;
        private Vec3 velocity;
        private float lastAge;

        private Emission(ClientLevel level, BlobState state) {
            this.level = level;
            this.seed = state.seed;
            this.center = position(state);
            this.previous = this.center;
            this.velocity = velocity(state);
            this.lastAge = state.age;

            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.flame(Vec3.ZERO, 0.025f),
                    random -> sampleCluster(random, 0.30), 170f, 220));
            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.ember(Vec3.ZERO, 0.030f),
                    this::sampleTrail, 78f, 110));
            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.ash(new Vec3(0.0, 0.015, 0.0), 0.030f),
                    random -> sampleCluster(random, 0.40), 26f, 44));
            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.smoke(0x2E160D, 0.62f, seed ^ 0x51A0,
                            new Vec3(0.0, 0.025, 0.0), 0.025f),
                    random -> sampleCluster(random, 0.38), 30f, 48));
            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.vapor(0xB46226, 0.72f, seed ^ 0x0A90,
                            new Vec3(0.0, 0.010, 0.0), 0.020f),
                    random -> sampleCluster(random, 0.42), 18f, 32));
        }

        private void update(BlobState state, float strength) {
            float delta = Math.max(0.0f, Math.min(0.05f, (state.age - lastAge) / 20.0f));
            lastAge = state.age;
            previous = center;
            center = position(state);
            velocity = velocity(state);
            for (MaterialEmitters.Emitter emitter : emitters) {
                emitter.tick(delta, strength);
            }
        }

        private MaterialEmitters.Sample sampleCluster(RandomSource random, double radius) {
            double u = random.nextDouble() * 2.0 - 1.0;
            double angle = random.nextDouble() * Math.PI * 2.0;
            double rr = radius * Math.cbrt(random.nextDouble());
            double flat = Math.sqrt(Math.max(0.0, 1.0 - u * u)) * rr;
            return new MaterialEmitters.Sample(new Vec3(
                    center.x + Math.cos(angle) * flat,
                    center.y + u * rr,
                    center.z + Math.sin(angle) * flat), Vec3.ZERO);
        }

        private MaterialEmitters.Sample sampleTrail(RandomSource random) {
            double t = random.nextDouble();
            Vec3 pos = previous.add(center.subtract(previous).scale(t));
            double jitter = 0.16;
            pos = pos.add((random.nextDouble() * 2.0 - 1.0) * jitter,
                    (random.nextDouble() * 2.0 - 1.0) * jitter,
                    (random.nextDouble() * 2.0 - 1.0) * jitter);
            return new MaterialEmitters.Sample(pos, velocity.scale(-0.06));
        }

        private static Vec3 position(BlobState state) {
            return new Vec3(state.posX, state.posY, state.posZ);
        }

        private static Vec3 velocity(BlobState state) {
            return new Vec3(state.velX, state.velY, state.velZ);
        }
    }
}
