package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.LavaEruptionEntity;
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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The eruption is a layered composition at class L, with two piece
 * builders and shader treatment on every fire piece. The pieces layer
 * is a full crown of nine flame tongues at the wave front, sized from
 * the same reach and height the mechanics use, plus a crackFan web at
 * the base pushed forward into the frontal cone so the ground breaks
 * where the wave actually runs. The complex particle layer is
 * EMBER_FRAGMENT through the cone and ASH_FLAKE along the ground. The
 * material emission from the no-shapes pass stays as filler only:
 * flame and embers carrying the front, ash on the ground, and smoke
 * and heat vapor rising off the wave as it burns down. The entity
 * already spawns a DebrisSpawner field for the big physical chunks.
 */
public class LavaEruptionRenderer extends EntityRenderer<LavaEruptionEntity, LavaEruptionRenderer.EruptState> {
    private final Map<Integer, Emission> emissions = new HashMap<>();

    public LavaEruptionRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class EruptState extends EntityRenderState {
        public int entityId;
        public float age;
        public float yRot;
        public float reach;
        public int seed;
        public double posX;
        public double posY;
        public double posZ;
    }

    public boolean shouldRender(LavaEruptionEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(LavaEruptionEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public EruptState createRenderState() {
        return new EruptState();
    }

    @Override
    public void extractRenderState(LavaEruptionEntity entity, EruptState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.entityId = entity.getId();
        state.age = entity.tickCount + partialTick;
        state.yRot = entity.getYRot();
        state.reach = entity.reachAt(state.age);
        state.seed = entity.getSeed();
        state.posX = entity.getX();
        state.posY = entity.getY();
        state.posZ = entity.getZ();
    }

    @Override
    public void submit(EruptState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && state.entityId != 0) {
            Vec3 center = new Vec3(state.posX, state.posY, state.posZ);
            float distance = MaterialEmitters.distanceScale(center);
            float env = FxEnvelope.envelope(state.age, 12.0f);
            float remain = ease(Math.max(0.0f, 1.0f - state.age / 12.0f));
            float height = 0.7f + 1.1f * ease(Math.min(1.0f, state.age / 4.0f));
            float strength = distance * env * (0.25f + 0.75f * remain);
            Emission emission = emissions.get(state.entityId);
            if (emission == null || emission.level != level || emission.seed != state.seed
                    || state.age + 0.001f < emission.lastAge) {
                emission = new Emission(level, state);
                emissions.put(state.entityId, emission);
            }
            emission.update(state, strength, height);

            // Pieces layer, in local space (pose origin is the eruption
            // origin at ground level, axes world aligned). The crown
            // sits at the wave front inside the damage cone, and the
            // crack web is centered forward of the origin at 0.30 of
            // reach with a radius of 0.62 of reach, so its tips stay
            // inside the cone reach. Vertex estimate at distScale 1.0:
            // nine tongues about 432, crackFan about 560 to 700, for
            // roughly 1100 total, inside the class L budget of 6000.
            final long pieceSeed = state.seed;
            final float pieceEnv = env;
            final float pieceDs = distance;
            final float sway = state.age * 0.50f;
            if (pieceEnv > 0.01f && state.reach > 0.2f) {
                double yaw = Math.toRadians(state.yRot);
                Vec3 facingLocal = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
                final Vec3 crownBase = new Vec3(
                        facingLocal.x * state.reach * 0.52, 0.05,
                        facingLocal.z * state.reach * 0.52);
                final float crownRadius = Math.max(0.5f, state.reach * 0.42f);
                final float crownHeight = height * 1.25f + 0.35f;
                final float crackRadius = state.reach * 0.62f;
                ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.FLAME_TONGUE, (pose, buffer) ->
                        ComplexGeometry.flameTongues(pose, buffer, pieceSeed ^ 0xF1E1L,
                                crownBase, crownRadius, crownHeight, 9,
                                0xFFF2B0, 0xFF5A00, 0.95f, pieceEnv, sway, pieceDs));
                poseStack.pushPose();
                poseStack.translate(facingLocal.x * state.reach * 0.30, 0.0,
                        facingLocal.z * state.reach * 0.30);
                ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) ->
                        ComplexGeometry.crackFan(pose, buffer, pieceSeed ^ 0xC9E1L,
                                crackRadius, 0xFFF2B0, 0xFF5A00, 0.95f, pieceEnv, pieceDs));
                poseStack.popPose();
            }
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
        private Vec3 facing = new Vec3(0.0, 0.0, 1.0);
        private Vec3 side = new Vec3(1.0, 0.0, 0.0);
        private double reach = 1.0;
        private double height = 1.0;
        private float lastAge;

        private Emission(ClientLevel level, EruptState state) {
            this.level = level;
            this.seed = state.seed;
            this.center = position(state);
            this.lastAge = state.age;
            updateFrame(state, 1.0);

            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.flame(Vec3.ZERO, 0.020f),
                    random -> sampleCone(random, 0.05, 1.0), 880f, 1050));
            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.ember(Vec3.ZERO, 0.026f),
                    random -> sampleCone(random, 0.05, 1.0), 360f, 460));
            // Complex particle layer: irregular ember fragments through
            // the cone and ash flakes along the ground, each at the
            // standard continuous cap for complex sprites.
            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.fixed(DbaParticles.EMBER_FRAGMENT, Vec3.ZERO, 0.040f),
                    random -> sampleCone(random, 0.05, 1.0), 36f, 40));
            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.fixed(DbaParticles.ASH_FLAKE, new Vec3(0.0, 0.012, 0.0), 0.024f),
                    this::sampleGround, 22f, 30));
            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.ash(new Vec3(0.0, 0.010, 0.0), 0.024f),
                    this::sampleGround, 110f, 150));
            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.smoke(0x2E130B, 1.10f, seed ^ 0xE207,
                            new Vec3(0.0, 0.030, 0.0), 0.022f),
                    random -> sampleCone(random, 0.45, 0.75), 150f, 210));
            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.vapor(0xA34A16, 1.00f, seed ^ 0xA11D,
                            new Vec3(0.0, 0.016, 0.0), 0.020f),
                    random -> sampleCone(random, 0.10, 0.90), 90f, 130));
        }

        private void update(EruptState state, float strength, double newHeight) {
            float delta = Math.max(0.0f, Math.min(0.05f, (state.age - lastAge) / 20.0f));
            lastAge = state.age;
            updateFrame(state, newHeight);
            for (MaterialEmitters.Emitter emitter : emitters) {
                emitter.tick(delta, strength);
            }
        }

        private void updateFrame(EruptState state, double newHeight) {
            center = position(state);
            reach = Math.max(0.25, state.reach);
            height = Math.max(0.25, newHeight);
            double yaw = Math.toRadians(state.yRot);
            facing = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
            side = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
        }

        private MaterialEmitters.Sample sampleCone(RandomSource random, double yBase, double ySpan) {
            double t = Math.sqrt(random.nextDouble());
            double dist = reach * t;
            double angle = (random.nextDouble() * 2.0 - 1.0) * 1.15;
            Vec3 dir = facing.scale(Math.cos(angle)).add(side.scale(Math.sin(angle)));
            Vec3 pos = center.add(dir.scale(dist));
            double y = center.y + yBase + random.nextDouble() * height * ySpan * (1.10 - 0.55 * t);
            pos = new Vec3(pos.x, y, pos.z);
            Vec3 velocity = dir.scale(0.030 + 0.045 * t)
                    .add(new Vec3(0.0, 0.035 + random.nextDouble() * 0.045, 0.0));
            return new MaterialEmitters.Sample(pos, velocity);
        }

        private MaterialEmitters.Sample sampleGround(RandomSource random) {
            double t = Math.sqrt(random.nextDouble());
            double dist = reach * t;
            double angle = (random.nextDouble() * 2.0 - 1.0) * 1.15;
            Vec3 dir = facing.scale(Math.cos(angle)).add(side.scale(Math.sin(angle)));
            Vec3 pos = center.add(dir.scale(dist));
            pos = new Vec3(pos.x, center.y + 0.04, pos.z);
            return new MaterialEmitters.Sample(pos, dir.scale(0.018 + 0.020 * t));
        }

        private static Vec3 position(EruptState state) {
            return new Vec3(state.posX, state.posY, state.posZ);
        }
    }
}
