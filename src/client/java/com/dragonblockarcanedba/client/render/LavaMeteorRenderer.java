package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.LavaMeteorEntity;
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
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The falling meteor is carried by a dense cluster at its head and a
 * material wake behind it: flame at the core, embers and magma chunks
 * shedding along the fall path, and ash, smoke, and heat vapor in the
 * wake. No rock body is drawn as geometry.
 */
public class LavaMeteorRenderer extends EntityRenderer<LavaMeteorEntity, LavaMeteorRenderer.MeteorState> {
    private final Map<Integer, Emission> emissions = new HashMap<>();

    public LavaMeteorRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class MeteorState extends EntityRenderState {
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

    public boolean shouldRender(LavaMeteorEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(LavaMeteorEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public MeteorState createRenderState() {
        return new MeteorState();
    }

    @Override
    public void extractRenderState(LavaMeteorEntity entity, MeteorState state, float partialTick) {
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
    public void submit(MeteorState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && state.entityId != 0) {
            Vec3 center = new Vec3(state.posX, state.posY, state.posZ);
            float distance = MaterialEmitters.distanceScale(center);
            float env = Math.max(0.35f, FxEnvelope.envelope(state.age, 240.0f));
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
        private Vec3 velocity;
        private float lastAge;

        private Emission(ClientLevel level, MeteorState state) {
            this.level = level;
            this.seed = state.seed;
            this.center = position(state);
            this.velocity = velocity(state);
            this.lastAge = state.age;

            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.flame(Vec3.ZERO, 0.024f),
                    random -> sampleVolume(random, center, 0.58), 150f, 200));
            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.flame(Vec3.ZERO, 0.030f),
                    random -> sampleTrail(random, 0.45), 260f, 330));
            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.ember(Vec3.ZERO, 0.034f),
                    random -> sampleTrail(random, 0.55), 120f, 160));
            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.fixed(
                            new BlockParticleOption(DbaParticles.BLOCK_CHUNKS, Blocks.MAGMA_BLOCK.defaultBlockState()),
                            Vec3.ZERO, 0.040f),
                    random -> sampleVolume(random, center, 0.50), 22f, 32));
            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.ash(new Vec3(0.0, 0.012, 0.0), 0.032f),
                    random -> sampleTrail(random, 0.68), 38f, 55));
            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.smoke(0x2B130B, 1.18f, seed ^ 0x7EA1,
                            new Vec3(0.0, 0.020, 0.0), 0.028f),
                    random -> sampleTrail(random, 0.78), 70f, 95));
            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.vapor(0xA54D18, 1.00f, seed ^ 0xD00E,
                            new Vec3(0.0, 0.010, 0.0), 0.022f),
                    random -> sampleVolume(random, center, 0.78), 32f, 48));
        }

        private void update(MeteorState state, float strength) {
            float delta = Math.max(0.0f, Math.min(0.05f, (state.age - lastAge) / 20.0f));
            lastAge = state.age;
            center = position(state);
            velocity = velocity(state);
            for (MaterialEmitters.Emitter emitter : emitters) {
                emitter.tick(delta, strength);
            }
        }

        private MaterialEmitters.Sample sampleTrail(RandomSource random, double radius) {
            Vec3 tail = center.subtract(velocity.scale(3.2));
            Vec3 pos = center.add(tail.subtract(center).scale(random.nextDouble()));
            return sampleVolume(random, pos, radius);
        }

        private static MaterialEmitters.Sample sampleVolume(RandomSource random, Vec3 origin, double radius) {
            double u = random.nextDouble() * 2.0 - 1.0;
            double angle = random.nextDouble() * Math.PI * 2.0;
            double rr = radius * Math.cbrt(random.nextDouble());
            double flat = Math.sqrt(Math.max(0.0, 1.0 - u * u)) * rr;
            return new MaterialEmitters.Sample(new Vec3(
                    origin.x + Math.cos(angle) * flat,
                    origin.y + u * rr,
                    origin.z + Math.sin(angle) * flat), Vec3.ZERO);
        }

        private static Vec3 position(MeteorState state) {
            return new Vec3(state.posX, state.posY, state.posZ);
        }

        private static Vec3 velocity(MeteorState state) {
            return new Vec3(state.velX, state.velY, state.velZ);
        }
    }
}
