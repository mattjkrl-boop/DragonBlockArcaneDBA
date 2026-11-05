package com.dragonblockarcanedba.client.render.ability;

import com.dragonblockarcanedba.ability.AbilityDef;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.kit.CrackDecal;
import com.dragonblockarcanedba.client.render.kit.DomeShell;
import com.dragonblockarcanedba.client.render.kit.VortexBand;
import com.dragonblockarcanedba.entity.SwarmHostEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Swarm host renderer (Phase 6): a planted seal, deliberately
 * quiet. A ground ring and an eight-branch CrackDecal at the slam
 * radius mark where the host landed, a slow VortexBand ring turns
 * at the shard orbit radius and height, and a low DomeShell
 * hemisphere caps the host itself. The shards carry the spectacle;
 * this is the anchor they read against. The envelope breathes in
 * over the first 20 ticks and out over the last 20 of the def's
 * active phase, since the host is long lived by design.
 */
public class SwarmHostRenderer extends EntityRenderer<SwarmHostEntity, SwarmHostRenderer.State> {

    public SwarmHostRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class State extends EntityRenderState {
        public int entityId = -1;
        public AbilityDef def;
        public int seed = 0;
        public float power = 1f;
        public double wx = 0, wy = 0, wz = 0;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SwarmHostEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.entityId = entity.getId();
        state.def = entity.def();
        state.seed = entity.getSeed();
        state.power = entity.getPower();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        AbilityDef def = state.def;
        if (def != null && def.swarm() != null) {
            float dist = (float) Math.sqrt(state.distanceToCameraSq);
            float distScale = FxEnvelope.distScale(dist);
            float age = state.ageInTicks;
            float active = def.phases().active();
            float fadeIn = FxEnvelope.smoothstep(age / 20f);
            float fadeOut = active <= 0f ? 0f : FxEnvelope.smoothstep((active - age) / 20f);
            float env = Math.min(fadeIn, fadeOut) * distScale;
            if (env > 0.01f && dist <= def.caps().cullDistance()) {
                drawSeal(state, def, poseStack, collector, env, distScale);
            }
        }
        super.submit(state, poseStack, collector, cameraState);
    }

    private void drawSeal(State state, AbilityDef def, PoseStack poseStack,
                          SubmitNodeCollector collector, float env, float distScale) {
        AbilityDef.VisualSpec vis = def.visual();
        final long seed = state.seed;
        final float age = state.ageInTicks;
        final double slamR = def.swarm().slamRadius() * 0.8;
        final float rimR = CarrierGeometry.r(vis.rim()), rimG = CarrierGeometry.g(vis.rim()), rimB = CarrierGeometry.b(vis.rim());
        final int segments = FxEnvelope.segments(36, distScale, 14);

        // Ground ring at the slam radius, with the crack scar inside it.
        ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) -> {
            CarrierGeometry.flatRing(pose, buffer, 0, 0, 0.06,
                    Math.max(0.1, slamR - 0.30), slamR + 0.30, segments,
                    rimR, rimG, rimB, env * 0.55f);
            CrackDecal.draw(pose, buffer, seed, slamR, 8, 0.07,
                    vis.core(), vis.rim(), env * 0.8f);
        });
        // The orbit ring: slow torn slabs where the shards circle.
        final double rotation = age * 0.05;
        ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) -> {
            VortexBand.draw(pose, buffer, seed, 1.6, 2.3, 0.5, 0.35, 18, rotation,
                    vis.rim(), vis.mid(), vis.core(), env * 0.5f);
        });
        // The cap: one quiet hemisphere over the host body.
        final int domeSlices = FxEnvelope.segments(20, distScale, 10);
        ComplexGeometry.submitVeil(collector, poseStack, (pose, buffer) -> {
            DomeShell.draw(pose, buffer, 1.3, domeSlices, 5, 1,
                    0.3f, age, vis.mid(), vis.rim(), vis.core(),
                    0.55f, 0.25f, env);
        });
    }
}
