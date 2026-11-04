package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The falling meteor is a layered composition at class L, with three
 * piece builders. In flight the pieces layer is a cluster of flame
 * tongues trailing off the head, licking upward against the fall on
 * the additive shader. In the last blocks of the fall the ground at
 * the touchdown point gains the other two builders: fracturePlates in
 * magma crust over dark basalt through submitSolid, and a crackFan
 * magma web through submitEnergy, both ramping in with proximity and
 * placed from the client heightmap at the real surface under the
 * meteor. The complex particle layer is EMBER_FRAGMENT heavy: a
 * continuous fragment trail at the standard cap plus a one shot burst
 * of 48 at touchdown proximity, once per meteor. The material emission
 * from the no-shapes pass stays as filler only: a dense cluster at the
 * head and a material wake behind it, flame at the core, embers and
 * magma chunks shedding along the fall path, and ash, smoke, and heat
 * vapor in the wake. No rock body is drawn as geometry: the entity
 * already spawns real debris and crater pools server side at impact.
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

            // Pieces layer, in local space (pose origin is the meteor
            // position). Vertex estimate at distScale 1.0: seven tail
            // tongues about 336, fracturePlates about 410, crackFan
            // about 560 to 700, for roughly 1400 total, inside the
            // class L budget of 6000, with three builder types.
            final long pieceSeed = state.seed;
            final float pieceEnv = env;
            final float pieceDs = distance;
            final float sway = state.age * 0.70f;
            ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.FLAME_TONGUE, (pose, buffer) ->
                    ComplexGeometry.flameTongues(pose, buffer, pieceSeed ^ 0xF1D1L,
                            new Vec3(0.0, 0.30, 0.0), 0.55f, 1.45f, 7,
                            0xFFF2B0, 0xFF5A00, 0.95f, pieceEnv, sway, pieceDs));

            // Impact pieces at the touchdown point: the surface under
            // the meteor from the client heightmap, ramping in over the
            // last five blocks of the fall. The meteor entity is
            // discarded at impact, so these pieces and the burst below
            // are the renderer side touchdown beat; the persistent
            // crater, pools, and debris are the entity's server side
            // impact, unchanged.
            int groundTop = level.getHeight(Heightmap.Types.MOTION_BLOCKING,
                    Mth.floor(state.posX), Mth.floor(state.posZ));
            double aboveGround = state.posY - groundTop;
            if (aboveGround > -0.5 && aboveGround < 5.0) {
                float prox = Mth.clamp((float) (1.0 - aboveGround / 5.0), 0.0f, 1.0f);
                float impactEnv = prox * prox * (3.0f - 2.0f * prox) * env;
                if (impactEnv > 0.01f) {
                    poseStack.pushPose();
                    poseStack.translate(0.0, groundTop - state.posY + 0.02, 0.0);
                    ComplexGeometry.submitSolid(collector, poseStack, (pose, buffer) ->
                            ComplexGeometry.fracturePlates(pose, buffer, pieceSeed ^ 0xB2D1L,
                                    2.6f, 0.38f, 0xFF7A1A, 0x2E0F06, 0.96f, impactEnv, pieceDs));
                    ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) ->
                            ComplexGeometry.crackFan(pose, buffer, pieceSeed ^ 0xC9D1L,
                                    2.9f, 0xFFF2B0, 0xFF5A00, 0.95f, impactEnv, pieceDs));
                    poseStack.popPose();
                    emission.burstImpact(new Vec3(state.posX, groundTop + 0.15, state.posZ));
                }
            }
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
        private boolean impactBurstDone;

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
            // Complex particle layer, heavy: irregular ember fragments
            // along the fall trail at the standard continuous cap for
            // complex sprites, on top of the round ember filler.
            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.fixed(DbaParticles.EMBER_FRAGMENT, Vec3.ZERO, 0.050f),
                    random -> sampleTrail(random, 0.60), 40f, 40));
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

        /**
         * One shot EMBER_FRAGMENT burst at the touchdown point, fired
         * once per meteor when the impact pieces ramp in. 48 fragments
         * sits inside the class L burst budget of 60.
         */
        private void burstImpact(Vec3 at) {
            if (impactBurstDone) return;
            impactBurstDone = true;
            MaterialEmitters.burst(level,
                    MaterialEmitters.Preset.fixed(DbaParticles.EMBER_FRAGMENT, Vec3.ZERO, 0.22f),
                    random -> {
                        double angle = random.nextDouble() * Math.PI * 2.0;
                        double rr = 1.6 * Math.sqrt(random.nextDouble());
                        Vec3 pos = new Vec3(at.x + Math.cos(angle) * rr,
                                at.y + random.nextDouble() * 0.25,
                                at.z + Math.sin(angle) * rr);
                        Vec3 vel = new Vec3(Math.cos(angle) * (0.10 + random.nextDouble() * 0.16),
                                0.16 + random.nextDouble() * 0.22,
                                Math.sin(angle) * (0.10 + random.nextDouble() * 0.16));
                        return new MaterialEmitters.Sample(pos, vel);
                    }, 48);
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
