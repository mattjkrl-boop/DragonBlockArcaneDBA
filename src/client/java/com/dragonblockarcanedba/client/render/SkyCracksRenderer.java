package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.SkyCracksEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

/**
 * The torn sky above the Cursed Tempest. This used to draw nothing at all;
 * the spectacle pass gave it a hand-rolled crack network, and the bespoke
 * pass rebuilds that network entirely on BespokeGeometry: seven main
 * fractures drawn with the branch builder (midpoint-displaced jagged
 * ribbons, a crimson sheath with a white hot core on the identical seeded
 * path), a crackStar web of wandering violet and crimson fractures woven
 * between them, and a glowFan shadow pool behind it all. Everything is
 * seeded from the entity's synced seed, so the tear is the same tear on
 * every client. The network is billboarded to the camera; the flat
 * builders (glowFan, crackStar) are drawn through a pose rotated so their
 * ground plane becomes the billboard plane. Alpha runs the shared
 * lifecycle envelope over the entity's 100 tick life times a slow
 * electrical flicker, so the tear opens, burns, and seals shut instead of
 * popping in and vanishing.
 */
public class SkyCracksRenderer extends EntityRenderer<SkyCracksEntity, SkyCracksRenderer.SkyCracksRenderState> {

    public SkyCracksRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SkyCracksRenderState extends EntityRenderState {
        public float age = 0;
        public int seed = 0;
    }

    @Override
    public SkyCracksRenderState createRenderState() {
        return new SkyCracksRenderState();
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(SkyCracksEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(SkyCracksEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public void extractRenderState(SkyCracksEntity entity, SkyCracksRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getSeed();
    }

    @Override
    public void submit(SkyCracksRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float env = FxEnvelope.envelope(state.age, SkyCracksEntity.LIFE_TICKS, 0.15f, 0.35f);
        if (env <= 0.01f) return;
        float ds = LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        float flicker = 0.78f + 0.22f * (float) Math.sin(state.age * 2.1f + state.seed * 0.37f);
        float alpha = env * flicker;
        long seed = state.seed;
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        if (cameraState.orientation != null) {
            poseStack.mulPose(new org.joml.Matrix4f().rotation(new org.joml.Quaternionf(cameraState.orientation).conjugate()));
        }

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // Flat builders draw in the XZ ground plane; rotate a copy of
            // the pose so that plane becomes the billboard XY plane.
            PoseStack.Pose flat = pose.copy();
            flat.rotate(Axis.XP.rotation((float) (-Math.PI / 2.0)));

            // Shadow of the tear: a dark pool the cracks burn through.
            BespokeGeometry.glowFan(flat, buffer, seed ^ 0x5EEDL, 24.0f, 0.0f,
                0.10f, 0.01f, 0.16f, 0.30f * alpha, ds);

            // The fracture web between the mains: two crackStars, crimson
            // wide and violet tighter, wandering and tapering.
            BespokeGeometry.crackStar(flat, buffer, seed ^ 0xC4ACCL, 20.0f,
                0.95f, 0.05f, 0.20f, 0.50f, alpha, ds);
            BespokeGeometry.crackStar(flat, buffer, seed ^ 0xF04CL, 14.0f,
                0.55f, 0.10f, 0.95f, 0.60f, alpha, ds);

            // Seven main fractures: branch bolts radiating from the heart.
            // Sheath and core share a seed, so the white core runs exactly
            // down the crimson sheath's jagged path.
            BespokeGeometry.Rng rng = new BespokeGeometry.Rng(seed);
            int mains = 7;
            for (int m = 0; m < mains; m++) {
                double ang = (m / (double) mains) * Math.PI * 2.0 + rng.range(-0.25f, 0.25f);
                float len = rng.range(15.0f, 25.0f);
                Vec3 to = new Vec3(Math.cos(ang) * len, Math.sin(ang) * len, 0);
                long crackSeed = seed * 31L + m * 101L;
                BespokeGeometry.branch(pose, buffer, crackSeed, Vec3.ZERO, to, 0.34f,
                    0.95f, 0.05f, 0.20f, 0.95f, alpha, ds);
                BespokeGeometry.branch(pose, buffer, crackSeed, Vec3.ZERO, to, 0.13f,
                    1.0f, 0.95f, 0.97f, 0.95f, alpha, ds);
            }
        });

        poseStack.popPose();
    }
}
