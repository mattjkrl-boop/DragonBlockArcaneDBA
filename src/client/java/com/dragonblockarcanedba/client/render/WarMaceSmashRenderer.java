package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.WarMaceSmashEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * The smash shell, rebuilt on the bespoke toolkit (BESPOKE-STANDARD rule 1).
 * A jittered glow fan grounds it, three broken vortex bands in the standard
 * band alphas carry the dust ring at the shell's edge, a crack star draws the
 * fracture marks on the ground, an early plume carries the impact dust, and
 * the bronze shell itself is a noise displaced dome squashed to the old bow
 * height, with a brighter displaced core dome inside it. All of it is seeded
 * from the entity's synced seed, so every client draws the same silhouette.
 * Everything runs the shared FxEnvelope, so the shell builds in over the
 * first ticks instead of appearing at full alpha, peaks, and settles.
 */
public class WarMaceSmashRenderer extends EntityRenderer<WarMaceSmashEntity, WarMaceSmashRenderer.SmashState> {
    public WarMaceSmashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SmashState extends EntityRenderState {
        public float age;
        public boolean heavy;
        public float radius;
        public int seed;
        public double ex;
        public double ey;
        public double ez;
    }

    public boolean shouldRender(WarMaceSmashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(WarMaceSmashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public SmashState createRenderState() {
        return new SmashState();
    }

    @Override
    public void extractRenderState(WarMaceSmashEntity entity, SmashState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.age = entity.tickCount + partialTick;
        state.heavy = entity.isHeavy();
        state.radius = entity.radiusAt(state.age);
        state.seed = entity.getSeed();
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
    }

    @Override
    public void submit(SmashState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float life = state.heavy ? 14.0f : 10.0f;
        float env = FxEnvelope.envelope(state.age, life, 0.15f, 0.35f);
        float bow = (state.heavy ? 1.35f : 0.72f) * (0.35f + 0.65f * env);
        float distScale = LayeredRender.distScale(cameraState, state.ex, state.ey, state.ez);
        float bandsHeight = state.heavy ? 1.7f : 1.05f;
        long seed = state.seed;
        RenderType type = KiRenderHelper.kiRenderType();

        // Ground layer at natural height: glow splash, the three counter
        // rotating broken bands, the fracture star, and the impact plume.
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            BespokeGeometry.glowFan(pose, buffer, seed ^ 0x610F,
                state.radius * 1.02f, 0.05f,
                1.0f, 0.78f, 0.38f, 0.55f * env, distScale);
            float bandR = state.radius * 0.82f;
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xBA01,
                bandR, 0.10f, bandsHeight, state.age * 0.45f,
                0.85f, 0.62f, 0.28f, 0.85f * env, distScale);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xBA02,
                bandR * 1.07f, 0.06f, bandsHeight * 0.78f, -state.age * 0.6f,
                0.85f, 0.62f, 0.28f, 0.45f * env, distScale);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xBA03,
                bandR * 1.16f, 0.03f, bandsHeight * 0.55f, state.age * 0.3f,
                0.85f, 0.62f, 0.28f, 0.18f * env, distScale);
            BespokeGeometry.crackStar(pose, buffer, seed ^ 0xCA01,
                state.radius * 0.98f,
                0.16f, 0.10f, 0.05f, 0.9f, env, distScale);
            float plumeEnv = env * Math.max(0.0f, 1.0f - state.age / (life * 0.6f));
            if (plumeEnv > 0.02f) {
                BespokeGeometry.plume(pose, buffer, seed ^ 0xD057,
                    state.radius * 0.38f, state.heavy ? 2.3f : 1.5f,
                    0.92f, 0.76f, 0.50f, 0.5f, plumeEnv, distScale);
            }
        });

        // The bronze shell: a displaced dome squashed to the old bow height,
        // with a brighter, smaller displaced dome as the hot core inside it.
        float squash = bow / Math.max(0.7f, state.radius);
        poseStack.pushPose();
        poseStack.scale(1.0f, squash, 1.0f);
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            if (state.heavy) {
                BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xD001,
                    state.radius, 0.86f, 0.56f, 0.20f, 0.70f, env, distScale);
            } else {
                BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xD001,
                    state.radius, 0.85f, 0.64f, 0.32f, 0.70f, env, distScale);
            }
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xD002,
                state.radius * 0.62f, 1.0f, 0.90f, 0.62f, 0.45f, env, distScale);
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
