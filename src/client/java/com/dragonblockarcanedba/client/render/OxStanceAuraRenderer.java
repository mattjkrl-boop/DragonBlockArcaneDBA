package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.OxStanceAuraEntity;
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
import org.joml.Matrix4f;

/**
 * Entity Renderer for Ox King's Colossal Stance Aura in Minecraft 26.2.
 * Bespoke pass: every shape comes from BespokeGeometry or LayeredRender.
 * The 12 block boundary and the repulsion ripple are broken vortex bands,
 * the pressure nodes, titan horns, and peak spikes are branch ribbons, the
 * pauldrons and heat shells are displaced domes, the aegis plates and the
 * helical ribbons are torn ribbons posed upright. The render state now also
 * carries the entity seed, so every silhouette is the bespoke toolkit's
 * seeded output. Radius, push timing, pulse, fade in, and the peak window
 * are unchanged from the spectacle build.
 *
 * Toolkit note: there is no shell or band builder for the pauldron arc or
 * the bowed plate, so those two pieces are the closest toolkit shapes
 * (squashed dome, upright torn ribbon). Recorded as a change request in
 * the bespoke report.
 */
public class OxStanceAuraRenderer extends EntityRenderer<OxStanceAuraEntity, OxStanceAuraRenderer.OxStanceRenderState> {

    public OxStanceAuraRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class OxStanceRenderState extends EntityRenderState {
        public int heldTicks = 0;
        public boolean isPeak = false;
        public float age = 0;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(OxStanceAuraEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(OxStanceAuraEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public OxStanceRenderState createRenderState() {
        return new OxStanceRenderState();
    }

    @Override
    public void extractRenderState(OxStanceAuraEntity entity, OxStanceRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.heldTicks = entity.getHeldTicks();
        state.isPeak = entity.isPeak();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(OxStanceRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        int held = state.heldTicks;
        boolean isPeak = state.isPeak;
        float age = state.age;

        float intensity = Math.min(1.0f, held / 100.0f);
        float pulse = 0.92f + 0.08f * (float) Math.sin(age * 0.45f);
        // Lifecycle (point 5): the aura used to pop in at a minimum alpha of
        // 0.70 on frame one. It now builds in over the first 8 ticks. (It
        // still ends when the stance ends; a release at 10+ ticks is covered
        // by the slam, and shorter holds simply stop, which matches the item.)
        float fadeIn = FxEnvelope.smoothstep(Math.min(1.0f, age / 8.0f));
        float alpha = (0.70f + (intensity * 0.25f)) * fadeIn;
        float distScale = FxEnvelope.distScale(Math.sqrt(state.distanceToCameraSq));
        long seed = state.seed;

        float avatarH = 3.6f * pulse;
        float avatarW = 1.6f * pulse;
        float rCol = isPeak ? 1.0f : 1.0f;
        float gCol = isPeak ? 0.92f : 0.48f;
        float bCol = isPeak ? 0.35f : 0.05f;
        float coreG = Math.min(1.0f, gCol + 0.30f);
        float coreB = Math.min(1.0f, bCol + 0.15f);
        float domeR = (2.2f + intensity * 0.8f) * pulse;

        float alphaF = alpha;
        float distScaleF = distScale;

        // Layered depth (point 1): an anchor glow grounds the 12 block
        // boundary, and three counter-rotating vortex bands wrap the titan so
        // the stance reads as moving heat, not a static statue.
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            LayeredRender.drawAnchorGlow(pose, buffer, 11.8f, 0.06f,
                1.0f, 0.62f, 0.10f, LayeredRender.ALPHA_MID * fadeIn, distScaleF);
            LayeredRender.drawVortexBands(pose, buffer, 2.8f, 3.4f, age,
                1.0f, isPeak ? 0.85f : 0.58f, isPeak ? 0.30f : 0.08f,
                fadeIn * (0.45f + intensity * 0.55f), distScaleF);
        });

        // Boundary, ripple, pressure nodes, horns, and peak spikes: all
        // origin centered or point to point, so they share one submission.
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // 1. 12-Block Battlefield Denial Repulsion Boundary
            float auraR = 12.0f;
            float cycle = (age * 0.18f) % 1.0f;
            float eased = cycle * cycle * (3.0f - 2.0f * cycle);
            float repulseWave = eased * auraR;
            float ringTwist = (float) Math.toRadians(age * 4.0f);

            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xB0DA1L,
                auraR * 0.98f, 0.04f, 0.10f, ringTwist,
                0.72f, isPeak ? 0.28f : 0.16f, 0.02f, alphaF * 0.80f, distScaleF);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xB0DA2L,
                auraR * 0.975f, 0.05f, 0.13f, ringTwist,
                1.0f, isPeak ? 0.90f : 0.62f, isPeak ? 0.30f : 0.08f,
                isPeak ? 0.95f : alphaF * 0.90f, distScaleF);

            // Expanding concentric repulsion shock ripple
            if (eased > 0.08f && eased < 0.96f) {
                float rippleFade = (1.0f - eased) * (1.0f - eased) * alphaF * 0.70f;
                BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xB0DA3L,
                    Math.max(0.2f, repulseWave), 0.05f, 0.11f, 0.0f,
                    1.0f, 0.55f, 0.08f, rippleFade, distScaleF);
            }

            // 12 Radial Pressure Nodes: low branch arcs on the boundary.
            for (int i = 0; i < 12; i++) {
                double nodeAng = (i / 12.0) * Math.PI * 2.0 + Math.toRadians(age * 6.0f);
                float nx1 = (float) Math.cos(nodeAng) * (auraR * 0.85f);
                float nz1 = (float) Math.sin(nodeAng) * (auraR * 0.85f);
                float nx2 = (float) Math.cos(nodeAng) * (auraR * 1.02f);
                float nz2 = (float) Math.sin(nodeAng) * (auraR * 1.02f);
                Vec3 from = new Vec3(nx1, 0.05f, nz1);
                Vec3 to = new Vec3(nx2, 0.08f, nz2);
                BespokeGeometry.branch(pose, buffer, seed ^ (0xA0DEL + i * 131L),
                    from, to, 0.10f,
                    0.85f, 0.35f, 0.04f, alphaF * 0.90f, 1.0f, distScaleF);
                BespokeGeometry.branch(pose, buffer, seed ^ (0xA0DFL + i * 131L),
                    from, to, 0.035f,
                    1.0f, 0.86f, 0.30f, alphaF * 0.90f, 1.0f, distScaleF);
            }

            // 2. Titan Horned Mountain Crest: gnarled branch horns.
            for (int s = -1; s <= 1; s += 2) {
                Vec3 base = new Vec3(s * 0.55f, 2.6f, 0.1f);
                Vec3 tip = new Vec3(s * 0.95f, 3.8f, 0.3f);
                BespokeGeometry.branch(pose, buffer, seed ^ (s < 0 ? 0x40A1L : 0x40A2L),
                    base, tip, 0.22f,
                    rCol, gCol, bCol, alphaF * 0.90f, 1.0f, distScaleF);
                BespokeGeometry.branch(pose, buffer, seed ^ (s < 0 ? 0x40A3L : 0x40A4L),
                    base, new Vec3(s * 0.88f, 3.55f, 0.28f), 0.08f,
                    1.0f, Math.min(1.0f, gCol + 0.28f), Math.min(1.0f, bCol + 0.16f),
                    alphaF * 0.90f, 1.0f, distScaleF);
            }

            // 4. Critical Peak Window Overdrive: radiating branch spikes.
            if (isPeak) {
                int spikeCount = 12;
                for (int i = 0; i < spikeCount; i++) {
                    double spkAng = (i / (double) spikeCount) * Math.PI * 2.0 + (age * 0.25f);
                    float spkLen = 2.5f + (float) (Math.sin(age * 0.8f + i * 1.5f) * 0.6f);
                    float sx1 = (float) Math.cos(spkAng) * 0.8f;
                    float sz1 = (float) Math.sin(spkAng) * 0.8f;
                    float sx2 = (float) Math.cos(spkAng) * spkLen;
                    float sz2 = (float) Math.sin(spkAng) * spkLen;
                    float sy = 1.0f + (float) Math.sin(i * 1.2f) * 0.6f;
                    Vec3 from = new Vec3(sx1, sy, sz1);
                    Vec3 to = new Vec3(sx2, sy * 0.4f, sz2);
                    BespokeGeometry.branch(pose, buffer, seed ^ (0x591CL + i * 137L),
                        from, to, 0.12f,
                        1.0f, 0.72f, 0.18f, 0.95f, 1.0f, distScaleF);
                    BespokeGeometry.branch(pose, buffer, seed ^ (0x591DL + i * 137L),
                        from, to, 0.04f,
                        1.0f, 0.98f, 0.70f, 0.95f, 1.0f, distScaleF);
                }
            }
        });

        // Titan Fortress Shoulders: squashed displaced domes as pauldrons.
        for (int s = -1; s <= 1; s += 2) {
            long pauldronSeed = seed ^ (s < 0 ? 0x9A01L : 0x9A02L);
            poseStack.pushPose();
            poseStack.translate(s * avatarW * 0.75f, 2.05f, 0.0f);
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.displacedDome(pose, buffer, pauldronSeed, 0.70f,
                    rCol, gCol, bCol, alphaF * 0.85f, 1.0f, distScaleF);
            });
            poseStack.popPose();
        }

        // Titan Aegis plates: upright torn ribbons bowed outward, front and
        // back, each with a hotter, smaller core ribbon in the same pose.
        for (int s = -1; s <= 1; s += 2) {
            long plateSeed = seed ^ (s < 0 ? 0x9101L : 0x9102L);
            poseStack.pushPose();
            poseStack.translate(0.0f, 0.5f, s * 0.65f);
            poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(s > 0 ? 90.0f : 270.0f)));
            poseStack.mulPose(new Matrix4f().rotation(Axis.ZP.rotationDegrees(90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.tornRibbon(pose, buffer, plateSeed, 1.4f, avatarW * 0.8f, 0.22f,
                    rCol, gCol, bCol, alphaF * 0.75f, 1.0f, distScaleF);
                BespokeGeometry.tornRibbon(pose, buffer, plateSeed ^ 0xC09EL,
                    0.85f, avatarW * 0.44f, 0.26f,
                    1.0f, coreG, coreB, alphaF * 0.85f, 1.0f, distScaleF);
            });
            poseStack.popPose();
        }

        // Swirling molten heat shells: displaced domes scaled to the old
        // dome heights.
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.1f, 0.0f);
        poseStack.scale(1.0f, (avatarH * 0.85f) / domeR, 1.0f);
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0x4EA7L, domeR,
                rCol, gCol, bCol, alphaF * 0.40f, 1.0f, distScaleF);
        });
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0f, 0.15f, 0.0f);
        poseStack.scale(1.0f, (avatarH * 0.55f) / (domeR * 0.58f), 1.0f);
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0x4EA8L, domeR * 0.58f,
                1.0f, Math.min(1.0f, gCol + 0.35f), Math.min(1.0f, bCol + 0.20f),
                alphaF * 0.55f, 1.0f, distScaleF);
        });
        poseStack.popPose();

        // Dual helical fiery ribbons: torn ribbons posed upright at the
        // helix base angle, bending tangentially as they rise.
        for (int h = 0; h < 2; h++) {
            double a0 = age * 0.18f + h * Math.PI;
            float baseX = (float) Math.cos(a0) * 1.32f * pulse;
            float baseZ = (float) Math.sin(a0) * 1.32f * pulse;
            float yawDeg = 90.0f - (float) Math.toDegrees(a0);
            long helixSeed = seed ^ (h == 0 ? 0x4E11L : 0x4E12L);
            poseStack.pushPose();
            poseStack.translate(baseX, 0.2f, baseZ);
            poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(yawDeg)));
            poseStack.mulPose(new Matrix4f().rotation(Axis.ZP.rotationDegrees(90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.tornRibbon(pose, buffer, helixSeed, avatarH, 0.16f, 0.85f,
                    rCol, gCol, bCol, alphaF * 0.85f, 1.0f, distScaleF);
                BespokeGeometry.tornRibbon(pose, buffer, helixSeed ^ 0xC09EL,
                    avatarH, 0.05f, 0.92f,
                    1.0f, Math.min(1.0f, gCol + 0.40f), Math.min(1.0f, bCol + 0.25f),
                    alphaF * 0.95f, 1.0f, distScaleF);
            });
            poseStack.popPose();
        }

        super.submit(state, poseStack, collector, cameraState);
    }
}
