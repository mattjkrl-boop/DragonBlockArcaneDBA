package com.dragonblockarcanedba.client.render.ability;

import com.dragonblockarcanedba.ability.AbilityDef;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.kit.BeamDrawer;
import com.dragonblockarcanedba.entity.BeamCarrierEntity;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Generic Beam renderer (Phase 6): the beam the BeamCarrierEntity
 * rides on the owner's eye, clipped per frame against the client
 * level exactly the way the server clips its damage segment, so
 * the drawn end is the damage end. Two forms, both data:
 * - energy: the kit BeamDrawer, a wiggle-chain core and halo from
 *   the eye to the clipped end, width following power.
 * - solid: a world-lit cylinder along the segment plus a disc
 *   cap at the tip (the Power Pole extension read): physical,
 *   so it goes through submitSolidLit and never glows.
 * The envelope is the beam's own: in over two ticks, out over
 * the last three ticks of the def's active phase.
 */
public class BeamCarrierRenderer extends EntityRenderer<BeamCarrierEntity, BeamCarrierRenderer.State> {

    public BeamCarrierRenderer(EntityRendererProvider.Context context) {
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
        /** Clipped beam end in local coordinates (entity pos is the eye). */
        public Vec3 endLocal = Vec3.ZERO;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BeamCarrierEntity entity, State state, float partialTicks) {
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
        state.endLocal = Vec3.ZERO;
        AbilityDef def = state.def;
        if (def != null && def.beam() != null) {
            Vec3 dir = lookDir(state.yRot, state.xRot);
            Vec3 from = entity.position();
            Vec3 fullEnd = from.add(dir.scale(def.beam().length()));
            Vec3 end = fullEnd;
            ClientLevel level = Minecraft.getInstance() != null ? Minecraft.getInstance().level : null;
            if (level != null) {
                BlockHitResult hit = level.clip(new ClipContext(from, fullEnd,
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
                if (hit.getType() == HitResult.Type.BLOCK) {
                    end = hit.getLocation();
                }
            }
            state.endLocal = end.subtract(from);
        }
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
        if (def != null && def.beam() != null) {
            float dist = (float) Math.sqrt(state.distanceToCameraSq);
            float distScale = FxEnvelope.distScale(dist);
            float age = state.ageInTicks;
            float active = def.phases().active();
            float fadeIn = Math.min(1f, Math.max(0f, age / 2f));
            float fadeOut = active <= 0f ? 0f
                    : Math.max(0f, Math.min(1f, (active - age) / 3f));
            float env = Math.min(fadeIn, fadeOut);
            if (env > 0.01f && state.endLocal.lengthSqr() > 1e-6) {
                if ("solid".equals(def.beam().form())) {
                    drawSolid(state, def, poseStack, collector, env);
                } else {
                    drawEnergy(state, def, poseStack, collector, env, distScale);
                }
            }
        }
        super.submit(state, poseStack, collector, cameraState);
    }

    private void drawEnergy(State state, AbilityDef def, PoseStack poseStack,
                            SubmitNodeCollector collector, float env, float distScale) {
        AbilityDef.VisualSpec vis = def.visual();
        final Vec3 end = state.endLocal;
        final float width = (float) (def.beam().radius() * (0.85 + 0.3 * state.power));
        final long seed = state.seed;
        final float age = state.ageInTicks;
        ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) -> {
            BeamDrawer.draw(pose, buffer, seed, Vec3.ZERO, end, age, width,
                    vis.core(), vis.mid(), env, distScale);
        });
    }

    private void drawSolid(State state, AbilityDef def, PoseStack poseStack,
                           SubmitNodeCollector collector, float env) {
        AbilityDef.VisualSpec vis = def.visual();
        final Vec3 end = state.endLocal;
        final double len = end.length();
        final Vec3 dirN = end.scale(1.0 / len);
        final int light = state.lightCoords;
        final float radius = (float) def.beam().radius();
        final float cr = CarrierGeometry.r(vis.mid()) * 0.8f;
        final float cg = CarrierGeometry.g(vis.mid()) * 0.8f;
        final float cb = CarrierGeometry.b(vis.mid()) * 0.8f;
        final float dr = CarrierGeometry.r(vis.core()) * 0.8f;
        final float dg = CarrierGeometry.g(vis.core()) * 0.8f;
        final float db = CarrierGeometry.b(vis.core()) * 0.8f;
        final float discRadius = (float) (def.beam().radius() * 2.2);
        // KiRenderHelper.drawCylinder runs along local +Z (its
        // cross-section is the XY plane, z0 to z1 along Z), so the
        // pose is rotated in two steps: first local +Z onto +Y
        // (an X rotation of -90 degrees), then +Y onto the beam
        // direction with a unit-vector quaternion. The composed
        // rotation maps the cylinder axis onto the segment.
        Quaternionf align = new Quaternionf().rotationTo(
                new Vector3f(0f, 1f, 0f),
                new Vector3f((float) dirN.x, (float) dirN.y, (float) dirN.z));

        poseStack.pushPose();
        try {
            poseStack.mulPose(new Matrix4f().rotation(align));
            poseStack.mulPose(new Matrix4f().rotation(Axis.XP.rotationDegrees(-90f)));
            ComplexGeometry.submitSolidLit(collector, poseStack, light, (pose, buffer) -> {
                KiRenderHelper.drawCylinder(pose, buffer, radius, 0f, (float) len, 14,
                        cr, cg, cb, env);
            });
        } finally {
            poseStack.popPose();
        }
        poseStack.pushPose();
        try {
            poseStack.mulPose(new Matrix4f().rotation(align));
            poseStack.mulPose(new Matrix4f().rotation(Axis.XP.rotationDegrees(-90f)));
            poseStack.translate(0, 0, len);
            ComplexGeometry.submitSolidLit(collector, poseStack, light, (pose, buffer) -> {
                KiRenderHelper.drawDisc(pose, buffer, 0f, discRadius, 20,
                        dr, dg, db, env);
            });
        } finally {
            poseStack.popPose();
        }
    }
}
