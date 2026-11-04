package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.OxFissureEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * Entity Renderer for Ox King's Ground Fissure in Minecraft 26.2.
 * Bespoke pass: every shape comes from BespokeGeometry. The trench is a
 * layered crack star squashed on X (0.55) inside the yaw aligned pose, so
 * the star's arms fill the same rotated rectangle the entity damages
 * (about 4.2 long, 2.0 wide) instead of the old hand drawn straight decal.
 * The stone teeth are branch spikes along the crack's long axis, and the
 * additive pass redraws the hot core layer so the crack blooms.
 */
public class OxFissureRenderer extends EntityRenderer<OxFissureEntity, OxFissureRenderer.OxFissureRenderState> {
    public OxFissureRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class OxFissureRenderState extends EntityRenderState {
        public float ageInTicks = 0;
        public long seed = 0;
        public float yRot = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(OxFissureEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(OxFissureEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public OxFissureRenderState createRenderState() {
        return new OxFissureRenderState();
    }

    @Override
    public void extractRenderState(OxFissureEntity entity, OxFissureRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.ageInTicks = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
        state.yRot = entity.getYRot();
    }

    @Override
    public void submit(OxFissureRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        float pulse = 0.75f + 0.25f * (float) Math.sin(state.ageInTicks * 0.25f);
        float life = state.ageInTicks / (float) OxFissureEntity.MAX_LIFETIME;
        float inT = Math.min(1.0f, state.ageInTicks / 4.0f);
        float inEase = 1.0f - (1.0f - inT) * (1.0f - inT) * (1.0f - inT);
        float outT = life <= 0.70f ? 0.0f : Math.min(1.0f, (life - 0.70f) / 0.30f);
        float fade = inEase * (1.0f - outT * outT * (3.0f - 2.0f * outT));
        long seed = state.seed;

        // Stone teeth along the crack's long axis (local Z), alternating
        // sides like the spectacle build; heights from the seeded stream.
        Random rng = new Random(seed);
        int toothCount = 4;
        float[] toothFrom = new float[toothCount * 3];
        float[] toothTo = new float[toothCount * 3];
        float toothGrow = Math.min(1.0f, state.ageInTicks * 0.2f);
        for (int i = 0; i < toothCount; i++) {
            float side = (i % 2 == 0) ? 1.0f : -1.0f;
            float toothH = (0.3f + rng.nextFloat() * 0.35f) * toothGrow;
            float tx = side * 0.3f;
            float tz = -1.35f + i * 0.9f;
            toothFrom[i * 3] = tx;
            toothFrom[i * 3 + 1] = 0.05f;
            toothFrom[i * 3 + 2] = tz;
            toothTo[i * 3] = tx + side * 0.15f;
            toothTo[i * 3 + 1] = Math.max(0.08f, toothH);
            toothTo[i * 3 + 2] = tz + side * 0.15f;
        }

        float fadeF = fade;
        float pulseF = pulse;
        float coreFade = fade * (1.0f - outT);

        // Align the trench with the yaw the spawner set (radially outward
        // from the slam). Every fissure used to render pointing the same way.
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(com.mojang.math.Axis.YP.rotationDegrees(-state.yRot)));

        // Anchor glow and teeth in the unscaled yaw pose.
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            LayeredRender.drawAnchorGlow(pose, buffer, 2.0f, 0.05f,
                1.0f, 0.50f, 0.08f, LayeredRender.ALPHA_MID * fadeF, 1.0f);
            for (int i = 0; i < toothCount; i++) {
                Vec3 from = new Vec3(toothFrom[i * 3], toothFrom[i * 3 + 1], toothFrom[i * 3 + 2]);
                Vec3 to = new Vec3(toothTo[i * 3], toothTo[i * 3 + 1], toothTo[i * 3 + 2]);
                BespokeGeometry.branch(pose, buffer, seed ^ (0x7007L + i * 131L),
                    from, to, 0.16f,
                    0.25f, 0.20f, 0.16f, fadeF, 1.0f, 1.0f);
                BespokeGeometry.branch(pose, buffer, seed ^ (0x7008L + i * 131L),
                    from, new Vec3(to.x, Math.max(0.06f, to.y * 0.65f), to.z), 0.07f,
                    1.0f, 0.35f, 0.05f, fadeF * 0.95f, 1.0f, 1.0f);
            }
        });

        // The crack star itself, squashed on X so its arms span the damage
        // rectangle: radius 1.9 along Z, about 1.05 across X after the squash.
        // Four layers share one seed, so the paths coincide: dark underlay,
        // magma base, heat core, white hot center.
        poseStack.pushPose();
        poseStack.scale(0.55f, 1.0f, 1.0f);
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.crackStar(pose, buffer, seed, 1.95f,
                0.45f, 0.08f, 0.0f, fadeF * 0.95f, 1.0f, 1.0f);
            BespokeGeometry.crackStar(pose, buffer, seed, 1.90f,
                1.0f, 0.25f * pulseF, 0.0f, fadeF * 0.95f, 1.0f, 1.0f);
            BespokeGeometry.crackStar(pose, buffer, seed, 1.82f,
                1.0f, 0.90f * pulseF, 0.20f, fadeF, 1.0f, 1.0f);
            BespokeGeometry.crackStar(pose, buffer, seed, 1.74f,
                1.0f, 0.98f, 0.72f, coreFade, 1.0f, 1.0f);
        });

        // Additive glow halo: the heat core layer blooms so the crack reads
        // as molten light in the ground.
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            BespokeGeometry.crackStar(pose, buffer, seed, 1.82f,
                1.0f, 0.90f * pulseF, 0.20f, fadeF * 0.35f, 1.0f, 1.0f);
        });
        poseStack.popPose();

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
