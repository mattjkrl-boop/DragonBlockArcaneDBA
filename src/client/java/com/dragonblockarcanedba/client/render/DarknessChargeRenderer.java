package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.DarknessChargeEntity;
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
 * Entity Renderer for Darkness Sword Abyssal Slash Charge-up in Minecraft 26.2.
 *
 * Bespoke pass: the hand-rolled ring, shard, and octahedron helpers are
 * gone. The ground seals are brokenVortexBand rings turning at the old
 * speeds, the converging shards are BespokeGeometry.branch crystals on
 * the unchanged orbit math, the twin helices are tall twisted
 * brokenVortexBand tubes, and the nested octahedron core is rebuilt from
 * displacedDome pairs (a noise-displaced dome and its flipped twin read
 * as a lumpy diamond core; the toolkit has no true octahedron builder,
 * noted as a gap in the bespoke report). The seed comes from the entity
 * UUID so every client draws the same shapes.
 */
public class DarknessChargeRenderer extends EntityRenderer<DarknessChargeEntity, DarknessChargeRenderer.ChargeRenderState> {

    public DarknessChargeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ChargeRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public float age = 0;
        public double px = 0, py = 0, pz = 0;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(DarknessChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(DarknessChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ChargeRenderState createRenderState() {
        return new ChargeRenderState();
    }

    @Override
    public void extractRenderState(DarknessChargeEntity entity, ChargeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.age = entity.tickCount + partialTicks;
        state.px = entity.getX();
        state.py = entity.getY();
        state.pz = entity.getZ();
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(ChargeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        final float charge = state.chargeRatio;
        final float age = state.age;
        final float ds = LayeredRender.distScale(cameraState, state.px, state.py, state.pz);
        final long seed = state.seed;

        final float baseRadius = 1.3f + (charge * 1.5f);
        final float baseAlpha = 0.65f + (charge * 0.35f);
        final float pulse = 0.90f + 0.10f * (float) Math.sin(age * 0.5f);

        // 1. Ground demonic seals: three broken vortex bands turning at
        // the old counter-rotating speeds (-14, +22, -32 degrees per tick).
        final float[] sealRadii = { baseRadius * 1.065f, baseRadius * 0.75f, baseRadius * 0.43f };
        final float[] sealSpeeds = { -14.0f, 22.0f, -32.0f };
        final float[] sealTwists = { 1.6f, -2.0f, 2.4f };
        final float[][] sealColors = {
            { 0.12f, 0.0f, 0.25f, 0.85f },
            { 0.55f, 0.05f, 0.85f, 0.92f },
            { 0.95f, 0.05f, 0.35f, 0.95f }
        };
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            poseStack.pushPose();
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(age * sealSpeeds[idx])));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ (0x5EA1L + idx * 131L),
                    sealRadii[idx], 0.02f + idx * 0.03f, 0.05f + idx * 0.03f, sealTwists[idx],
                    sealColors[idx][0], sealColors[idx][1], sealColors[idx][2],
                    baseAlpha * sealColors[idx][3], ds);
            });
            poseStack.popPose();
        }

        // 2. Inward-converging shadow shards on the unchanged orbit math,
        // each a branch crystal from its base to its inward-tipped point.
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            int shardCount = 8 + (int) (charge * 6);
            float orbitRadius = (2.2f - charge * 0.65f) * pulse;
            float orbitSpeed = 18.0f + (charge * 50.0f);
            float shardLen = 0.55f + (charge * 0.25f);
            for (int i = 0; i < shardCount; i++) {
                double shardAngle = (i / (double) shardCount) * Math.PI * 2.0 + Math.toRadians(age * orbitSpeed);
                float sy = 0.4f + (float) Math.sin(age * 0.35f + i * 1.3f) * 0.5f + (i % 2 == 0 ? 0.3f : 0.0f);
                float sx = (float) Math.cos(shardAngle) * orbitRadius;
                float sz = (float) Math.sin(shardAngle) * orbitRadius;

                float toCenterX = -sx;
                float toCenterZ = -sz;
                float toCenterY = 0.9f - sy;
                float toCenterLen = (float) Math.sqrt(toCenterX * toCenterX + toCenterZ * toCenterZ + toCenterY * toCenterY);
                if (toCenterLen > 0.001f) {
                    toCenterX /= toCenterLen;
                    toCenterY /= toCenterLen;
                    toCenterZ /= toCenterLen;
                }
                BespokeGeometry.branch(pose, buffer, seed ^ (0x51A9DL + i * 57L),
                    new Vec3(sx, sy, sz),
                    new Vec3(sx + toCenterX * shardLen, sy + toCenterY * shardLen, sz + toCenterZ * shardLen),
                    0.15f, 0.65f, 0.05f, 0.95f, baseAlpha * 0.95f, 1.0f, ds);
            }
        });

        // 3. The core: nested displaced domes and their flipped twins
        // standing in for the old octahedron stack, slowly turning.
        final float coreSize = (0.70f - charge * 0.20f) * pulse;
        poseStack.pushPose();
        poseStack.translate(0, 0.85f, 0);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(age * 20.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xC09E1L, coreSize,
                0.15f, 0.0f, 0.30f, baseAlpha * 0.80f, 1.0f, ds);
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xC09E2L, coreSize * 0.58f,
                0.60f, 0.05f, 0.90f, baseAlpha * 0.95f, 1.0f, ds);
            if (charge >= 0.4f) {
                BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xC09E3L, coreSize * 0.28f,
                    0.95f, 0.15f, 0.45f, 1.0f, 1.0f, ds);
            }
        });
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.85f, 0);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(180.0f)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-age * 20.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xC09E1L, coreSize,
                0.15f, 0.0f, 0.30f, baseAlpha * 0.56f, 1.0f, ds);
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xC09E2L, coreSize * 0.58f,
                0.60f, 0.05f, 0.90f, baseAlpha * 0.66f, 1.0f, ds);
            if (charge >= 0.4f) {
                BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xC09E3L, coreSize * 0.28f,
                    0.95f, 0.15f, 0.45f, 0.7f, 1.0f, ds);
            }
        });
        poseStack.popPose();

        // 4. Twin helical ribbons: tall twisted vortex bands, one violet
        // and one crimson, a half turn apart, winding as the charge ages.
        for (int strand = 0; strand < 2; strand++) {
            final boolean violet = strand == 0;
            final float strandRot = (float) Math.toDegrees(strand * Math.PI + age * 0.18);
            poseStack.pushPose();
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(strandRot)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ (violet ? 0x4E11L : 0x4E12L),
                    baseRadius * 0.62f, 0f, 2.3f, 8.2f,
                    violet ? 0.45f : 0.95f, 0.02f, violet ? 0.85f : 0.25f,
                    baseAlpha * 0.8f, ds);
            });
            poseStack.popPose();
        }

        // Additive pass: a halo dome pair around the core and the shared
        // ground anchor glow under the seals.
        final float haloCoreSize = coreSize * 1.35f;
        poseStack.pushPose();
        poseStack.translate(0, 0.85f, 0);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(age * 20.0f)));
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0x4A11L, haloCoreSize,
                0.35f, 0.03f, 0.50f, baseAlpha * 0.25f, 1.0f, ds);
        });
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.85f, 0);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(180.0f)));
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0x4A11L, haloCoreSize,
                0.35f, 0.03f, 0.50f, baseAlpha * 0.18f, 1.0f, ds);
        });
        poseStack.popPose();

        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            LayeredRender.drawAnchorGlow(pose, buffer, baseRadius * 1.25f, 0.04f,
                0.30f, 0.02f, 0.45f, baseAlpha * 0.22f, ds);
        });

        super.submit(state, poseStack, collector, cameraState);
    }
}
