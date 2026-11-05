package com.dragonblockarcanedba.client.render.ability;

import com.dragonblockarcanedba.ability.AbilityDef;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.entity.SwarmShardEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

/**
 * Swarm shard renderer (Phase 6): one shard of the Devil Trident
 * swarm, a world-lit physical body with a hot core showing
 * through it, the Projectile carrier's material language at swarm
 * scale. ComplexGeometry.shard takes its forward axis as a
 * parameter (it draws along dir from origin), so orientation is
 * the shard's per-frame motion direction captured in
 * extractRenderState, passed straight as dir with the origin
 * pulled back half a length so the body centers on the entity.
 * No pose rotation is needed, and none is faked.
 */
public class SwarmShardRenderer extends EntityRenderer<SwarmShardEntity, SwarmShardRenderer.State> {

    public SwarmShardRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class State extends EntityRenderState {
        public int entityId = -1;
        public AbilityDef def;
        public int seed = 0;
        public float power = 1f;
        public double wx = 0, wy = 0, wz = 0;
        /** Unit flight direction for this frame. */
        public Vec3 orient = new Vec3(0, 0, 1);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SwarmShardEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.entityId = entity.getId();
        state.def = entity.def();
        state.seed = entity.getSeed();
        state.power = entity.getPower();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
        Vec3 motion = new Vec3(entity.getX() - entity.xOld,
                entity.getY() - entity.yOld, entity.getZ() - entity.zOld);
        if (motion.lengthSqr() > 1e-6) {
            state.orient = motion.normalize();
        } else {
            double yaw = Math.toRadians(entity.getYRot());
            state.orient = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        AbilityDef def = state.def;
        if (def != null) {
            float dist = (float) Math.sqrt(state.distanceToCameraSq);
            float distScale = FxEnvelope.distScale(dist);
            // A ride model set replaces the procedural shard body.
            boolean modeled = def.models() != null && "ride".equals(def.models().behavior());
            if (!modeled && distScale > 0.01f && dist <= def.caps().cullDistance()) {
                drawShard(state, def, poseStack, collector, distScale);
            }
        }
        super.submit(state, poseStack, collector, cameraState);
    }

    private void drawShard(State state, AbilityDef def, PoseStack poseStack,
                           SubmitNodeCollector collector, float distScale) {
        AbilityDef.VisualSpec vis = def.visual();
        final Vec3 dir = state.orient;
        final long seed = state.seed;
        final int light = state.lightCoords;
        final float length = 0.95f;
        final float girth = 0.16f;
        final Vec3 origin = dir.scale(-length * 0.5);
        // Physical body on the lit path: rim at the root, mid at the tip.
        ComplexGeometry.submitSolidLit(collector, poseStack, light, (pose, buffer) -> {
            ComplexGeometry.shard(pose, buffer, seed, origin, dir, length, girth,
                    vis.rim(), vis.mid(), 1.0f, distScale);
        });
        // Inset energy core at 0.55 scale, showing through the body.
        final float coreLen = length * 0.55f;
        final Vec3 coreOrigin = dir.scale(-coreLen * 0.5);
        ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) -> {
            ComplexGeometry.shard(pose, buffer, seed ^ 0xC0DE, coreOrigin, dir,
                    coreLen, girth * 0.55f, vis.core(), 0xFFFFFFFF, 0.9f, distScale);
        });
    }
}
