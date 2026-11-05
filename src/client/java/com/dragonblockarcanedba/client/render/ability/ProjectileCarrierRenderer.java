package com.dragonblockarcanedba.client.render.ability;

import com.dragonblockarcanedba.ability.AbilityDef;
import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.entity.ProjectileCarrierEntity;
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
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic Projectile renderer: a shard body whose material is the
 * definition's casing choice (a world-lit physical casing through
 * the lit path, an emissive energy body, or nothing), an energy
 * core along the flight axis showing through it, and a material
 * wake shed behind. Physical casings never take the emissive path:
 * that is the standing world-lit rule, enforced here by casing
 * choice rather than per weapon.
 */
public class ProjectileCarrierRenderer extends EntityRenderer<ProjectileCarrierEntity, ProjectileCarrierRenderer.State> {

    public ProjectileCarrierRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class State extends EntityRenderState {
        public int entityId = -1;
        public AbilityDef def;
        public int seed = 0;
        public float power = 1f;
        public float yRot = 0f;
        public float xRot = 0f;
        public double wx = 0, wy = 0, wz = 0;
    }

    private static final class Frame {
        Vec3 center = Vec3.ZERO;
        Vec3 dir = Vec3.ZERO;
    }

    private static final class Emission {
        final Frame frame = new Frame();
        MaterialEmitters.Emitter[] emitters = new MaterialEmitters.Emitter[0];
        float lastAge = -1f;
    }

    private final Map<Integer, Emission> emissions = new HashMap<>();

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ProjectileCarrierEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.entityId = entity.getId();
        state.def = entity.def();
        state.seed = entity.getSeed();
        state.power = entity.getPower();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    private static Vec3 lookDir(float yRot, float xRot) {
        double yaw = Math.toRadians(yRot);
        double pitch = Math.toRadians(xRot);
        double cosP = Math.cos(pitch);
        return new Vec3(-Math.sin(yaw) * cosP, -Math.sin(pitch), Math.cos(yaw) * cosP);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        AbilityDef def = state.def;
        if (def != null && def.projectile() != null) {
            float dist = (float) Math.sqrt(state.distanceToCameraSq);
            float distScale = FxEnvelope.distScale(dist);
            if (distScale > 0.01f) {
                drawBody(state, def, poseStack, collector, distScale);
                tickEmission(state, def, dist, distScale);
            }
        }
        super.submit(state, poseStack, collector, cameraState);
    }

    private void drawBody(State state, AbilityDef def, PoseStack poseStack,
                          SubmitNodeCollector collector, float distScale) {
        AbilityDef.ProjectileParams proj = def.projectile();
        AbilityDef.VisualSpec vis = def.visual();
        final Vec3 dir = lookDir(state.yRot, state.xRot);
        final float scale = vis.scale() * (0.7f + 0.3f * state.power);
        final float length = (float) (1.35 * proj.lengthScale() * scale);
        final long seed = state.seed;
        final int light = state.lightCoords;

        switch (proj.casing()) {
            case "lit" -> ComplexGeometry.submitSolidLit(collector, poseStack, light, (pose, buffer) -> {
                ComplexGeometry.shard(pose, buffer, seed, dir.scale(-0.15), dir, length, 0.17f * scale,
                        vis.rim(), vis.mid(), 1.0f, distScale);
            });
            case "energy" -> ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) -> {
                ComplexGeometry.shard(pose, buffer, seed, dir.scale(-0.15), dir, length, 0.17f * scale,
                        vis.mid(), vis.core(), 0.95f, distScale);
            });
            default -> {
                // "none": no casing; the core alone carries the bolt.
            }
        }
        // The hot core, inset so it reads as showing through the casing.
        ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) -> {
            ComplexGeometry.shard(pose, buffer, seed ^ 0xC0DE, dir.scale(-0.05), dir, length * 1.1f, 0.075f * scale,
                    vis.core(), 0xFFFFFFFF, 0.95f, distScale);
        });
    }

    private void tickEmission(State state, AbilityDef def, float dist, float distScale) {
        if (dist > def.caps().cullDistance()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc != null ? mc.level : null;
        if (level == null) {
            return;
        }
        Emission emission = emissions.get(state.entityId);
        if (emission == null || state.ageInTicks < emission.lastAge - 1.0f) {
            emission = buildEmission(level, state, def);
            emissions.put(state.entityId, emission);
            if (emissions.size() > 64) {
                emissions.clear();
            }
        }
        emission.frame.center = new Vec3(state.wx, state.wy, state.wz);
        emission.frame.dir = lookDir(state.yRot, state.xRot);
        float delta = emission.lastAge < 0f ? 0.05f
                : Math.min(0.05f, Math.max(0f, (state.ageInTicks - emission.lastAge) / 20.0f));
        emission.lastAge = state.ageInTicks;
        for (MaterialEmitters.Emitter emitter : emission.emitters) {
            emitter.tick(delta, distScale);
        }
    }

    private Emission buildEmission(ClientLevel level, State state, AbilityDef def) {
        Emission emission = new Emission();
        AbilityDef.VisualSpec vis = def.visual();
        final Frame frame = emission.frame;
        frame.center = new Vec3(state.wx, state.wy, state.wz);
        frame.dir = lookDir(state.yRot, state.xRot);
        MaterialEmitters.Source cluster = random -> {
            Vec3 pos = frame.center.add(new Vec3(
                    (random.nextDouble() * 2 - 1) * 0.14,
                    (random.nextDouble() * 2 - 1) * 0.14,
                    (random.nextDouble() * 2 - 1) * 0.14));
            return new MaterialEmitters.Sample(pos, frame.dir.scale(-0.04));
        };
        MaterialEmitters.Source wake = random -> {
            double t = random.nextDouble();
            Vec3 pos = frame.center.add(frame.dir.scale(-(0.2 + t * 1.5))).add(new Vec3(
                    (random.nextDouble() * 2 - 1) * 0.12 * (1 + t),
                    (random.nextDouble() * 2 - 1) * 0.12 * (1 + t),
                    (random.nextDouble() * 2 - 1) * 0.12 * (1 + t)));
            return new MaterialEmitters.Sample(pos, frame.dir.scale(-0.02));
        };
        float budget = def.caps().maxParticlesPerSecond();
        float clusterRate = Math.min(budget * 0.6f, 300f * vis.particleRate());
        float wakeRate = Math.min(budget * 0.4f, 200f * vis.particleRate());
        emission.emitters = new MaterialEmitters.Emitter[]{
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.material(vis.particleMaterial(), vis.mid(),
                                1.9f * vis.scale(), state.seed, Vec3.ZERO, 0.02f),
                        cluster, clusterRate, Math.max(1, Math.round(clusterRate))),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.material(vis.particleMaterial(), vis.core(),
                                1.4f * vis.scale(), state.seed ^ 0x77, Vec3.ZERO, 0.02f),
                        wake, wakeRate, Math.max(1, Math.round(wakeRate))),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.12f),
                        cluster, 24f, 40)
        };
        return emission;
    }
}
