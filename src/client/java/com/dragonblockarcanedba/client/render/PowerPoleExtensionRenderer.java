package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.PowerPoleExtensionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Power Pole Extension in Minecraft 26.2.
 * Renders a physical 3D stretching Power Pole model:
 * - Crimson shaft stretching dynamically across the reach distance, built
 *   from crossed torn ribbons inside a torn gold sheath
 * - Gold end caps and collar fittings at base and tip, a faceted dome tip
 * - Golden dragon bands coiled along the shaft as stepped torn bands
 * - Supersonic Mach compression plumes sliding the shaft and a torn wake
 * - Leading-edge golden shard fan glint at the striking tip
 *
 * Bespoke pass: every shape comes from BespokeGeometry (BESPOKE-STANDARD
 * rule 1). The old equal-sided shaft, bevel ring, dome, cone, and
 * starburst loops and their local draw helpers are gone. Seeds hash from
 * the entity's synced state, so the torn silhouettes agree on every
 * client and hold still frame to frame.
 */
public class PowerPoleExtensionRenderer extends EntityRenderer<PowerPoleExtensionEntity, PowerPoleExtensionRenderer.ExtensionRenderState> {

    public PowerPoleExtensionRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ExtensionRenderState extends EntityRenderState {
        public float yRot = 0.0f;
        public float xRot = 0.0f;
        public float maxLength = 30.0f;
        public float age = 0.0f;
        public int casterId = -1;
        public long seed = 0;
        public boolean isFirstPersonOwner = false;
        public boolean onRight = true;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(PowerPoleExtensionEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(PowerPoleExtensionEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ExtensionRenderState createRenderState() {
        return new ExtensionRenderState();
    }

    @Override
    public void extractRenderState(PowerPoleExtensionEntity entity, ExtensionRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getEntityYaw();
        state.xRot = entity.getEntityPitch();
        state.maxLength = entity.getMaxLength();
        state.age = entity.tickCount + partialTicks;
        state.casterId = entity.getCasterId();
        // Bespoke geometry seed: hashed from the entity's SYNCED state only
        // (caster id, max length), never from client random or wall clock.
        state.seed = (entity.getCasterId() * 0x9E3779B97F4A7C15L)
            ^ Float.floatToIntBits(entity.getMaxLength());

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null &&
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) &&
            mc.options.getCameraType().isFirstPerson());
        if (mc.player != null) {
            boolean isRightHanded = (mc.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            boolean isOffhand = (mc.player.getOffhandItem().getItem() instanceof com.dragonblockarcanedba.item.PowerPoleItem &&
                !(mc.player.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.PowerPoleItem));
            state.onRight = isRightHanded ? !isOffhand : isOffhand;
        }
    }

    @Override
    public void submit(ExtensionRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float maxLength = state.maxLength;
        if (maxLength < 0.5f) return;

        float age = state.age;

        // The staff eases out to the poke, holds, then eases back in, fading
        // fully to zero across the retract so the pole is gone exactly when
        // the entity discards at tick 12. The alpha used to bottom out at
        // 0.65, so the pole vanished at 65 percent opacity: a visible cut.
        float extendProgress;
        float alphaMult = 1.0f;
        if (age < 3.0f) {
            float t = age / 3.0f;
            extendProgress = 1.0f - (1.0f - t) * (1.0f - t) * (1.0f - t);
        } else if (age > 9.0f) {
            float t = Math.min(1.0f, (age - 9.0f) / 3.0f);
            float back = t * t * (3.0f - 2.0f * t);
            extendProgress = 1.0f - back;
            alphaMult = 1.0f - back;
        } else {
            extendProgress = 1.0f;
        }

        final float currentLength = Math.max(0.8f, maxLength * extendProgress);
        final float finalAlpha = alphaMult;
        final float distScale = com.dragonblockarcanedba.client.render.ki.LayeredRender.distScale(
            cameraState, state.x, state.y, state.z);
        final long seed = state.seed;
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        if (state.isFirstPersonOwner) {
            // Anchor extending pole to the hand: way lower and on the weapon hand side (screen right)
            float sideSign = state.onRight ? -1.0f : 1.0f;
            poseStack.translate(sideSign * 0.38f, -0.48f, 0.20f);
        }

        final float baseZStart = state.isFirstPersonOwner ? 0.20f : 0.0f;
        final float fpScale = state.isFirstPersonOwner ? 0.55f : 1.0f;
        final float shaftRadius = 0.13f * fpScale;
        final float capRadius = 0.165f * fpScale;
        final float shaftZ1 = Math.max(baseZStart, 0.4f);
        final float shaftZ2 = currentLength - 0.4f;

        // Axial frame (builder +Y down the pole axis): the torn sheath, the
        // gold cap collars, and the stepped coil bands all position by
        // height, so they share one submit.
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            if (shaftZ2 > shaftZ1) {
                // Outer layer of the strike trail (standard point 1): a wide,
                // faint pale gold torn sheath around the shaft, the displaced
                // air and energy the extension drags with it.
                com.dragonblockarcanedba.client.render.ki.BespokeGeometry.brokenVortexBand(
                    pose, buffer, seed ^ 0x5EA7L, shaftRadius * 2.1f, shaftZ1, shaftZ2, 0.0f,
                    1.0f, 0.88f, 0.66f, finalAlpha * 0.14f, distScale);
            }
            // Base gold fitting collar.
            com.dragonblockarcanedba.client.render.ki.BespokeGeometry.brokenVortexBand(
                pose, buffer, seed ^ 0xCA9L, capRadius, baseZStart, 0.6f, 0.0f,
                1.0f, 0.82f, 0.12f, finalAlpha, distScale);
            if (currentLength > 1.2f) {
                // Tip gold fitting collar.
                com.dragonblockarcanedba.client.render.ki.BespokeGeometry.brokenVortexBand(
                    pose, buffer, seed ^ 0xCA7L, capRadius, currentLength - 0.9f, currentLength, 0.0f,
                    1.0f, 0.82f, 0.12f, finalAlpha, distScale);
                // Golden dragon bands: stepped torn gold slices whose twist
                // advances slice by slice, the coil read of the old helix.
                int slices = 4;
                float coilFrom = 0.6f;
                float coilSpan = currentLength - 1.2f;
                for (int k = 0; k < slices; k++) {
                    float y0 = coilFrom + coilSpan * (k / (float) slices);
                    float y1 = coilFrom + coilSpan * ((k + 1) / (float) slices);
                    com.dragonblockarcanedba.client.render.ki.BespokeGeometry.brokenVortexBand(
                        pose, buffer, seed ^ 0xC011L ^ (k * 41L), shaftRadius + 0.02f * fpScale, y0, y1,
                        age * 0.2f + k * 1.1f, 1.0f, 0.85f, 0.15f, 0.90f * finalAlpha, distScale);
                }
            }
        });
        poseStack.popPose();

        // Crimson shaft: crossed torn ribbons in the 3-stop gradient the old
        // sided shaft used (hot bright at the hand, full crimson mid, deep
        // crimson at the tip), plus a thinner bright power core pair.
        if (shaftZ2 > shaftZ1) {
            float band1 = shaftZ1 + (shaftZ2 - shaftZ1) / 3.0f;
            float band2 = shaftZ1 + (shaftZ2 - shaftZ1) * 2.0f / 3.0f;
            drawShaftRibbons(poseStack, collector, renderType, seed ^ 0xA1L,
                shaftZ1, band1, shaftRadius * 2.0f * 1.15f, 1.0f, 0.22f, 0.16f, 1.0f, finalAlpha, distScale);
            drawShaftRibbons(poseStack, collector, renderType, seed ^ 0xA2L,
                band1, band2, shaftRadius * 2.0f * 1.15f, 0.85f, 0.08f, 0.08f, 1.0f, finalAlpha, distScale);
            drawShaftRibbons(poseStack, collector, renderType, seed ^ 0xA3L,
                band2, shaftZ2, shaftRadius * 2.0f * 1.15f, 0.58f, 0.03f, 0.05f, 1.0f, finalAlpha, distScale);
        }
        float coreZ1 = Math.max(baseZStart, 0.2f);
        float coreZ2 = currentLength - 0.2f;
        if (coreZ2 > coreZ1) {
            drawShaftRibbons(poseStack, collector, renderType, seed ^ 0xA4L,
                coreZ1, coreZ2, shaftRadius * 1.3f, 1.0f, 0.35f, 0.25f, 0.85f, finalAlpha, distScale);
        }

        // Faceted dome tip with its bright lip ring, seated at the leading edge.
        if (currentLength > 1.2f) {
            poseStack.pushPose();
            poseStack.translate(0.0f, 0.0f, currentLength);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                com.dragonblockarcanedba.client.render.ki.BespokeGeometry.brokenVortexBand(
                    pose, buffer, seed ^ 0x117L, capRadius * 1.45f, -0.05f, 0.05f, 0.0f,
                    1.0f, 0.98f, 0.62f, finalAlpha * 0.8f, distScale);
                com.dragonblockarcanedba.client.render.ki.BespokeGeometry.displacedDome(
                    pose, buffer, seed ^ 0xD04EL, capRadius,
                    1.0f, 0.90f, 0.20f, finalAlpha, 1.0f, distScale);
            });
            poseStack.popPose();
        }

        // Mach compression plumes slide the length of the poke and thin out
        // as they go: stacked torn puff discs where the cones used to be.
        if (currentLength > 1.6f) {
            int shockCount = 3;
            for (int s = 0; s < shockCount; s++) {
                float shockProgress = ((age * 0.45f) + (s / (float) shockCount)) % 1.0f;
                float shockEase = shockProgress * shockProgress * (3.0f - 2.0f * shockProgress);
                float shockZ = 0.7f + shockEase * (currentLength - 1.4f);
                if (shockZ > 0.8f && shockZ < currentLength - 0.35f) {
                    final float plumeR = 0.52f * (1.0f - shockEase * 0.4f) * fpScale;
                    final float plumeH = 0.68f * fpScale;
                    final float plumeA = (float) Math.sin(shockProgress * (float) Math.PI) * 0.7f * finalAlpha;
                    final long plumeSeed = seed ^ (0x91E5L + s * 67L);
                    poseStack.pushPose();
                    poseStack.translate(0.0f, 0.0f, shockZ - plumeH);
                    poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
                    collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                        com.dragonblockarcanedba.client.render.ki.BespokeGeometry.plume(
                            pose, buffer, plumeSeed, plumeR, plumeH,
                            1.0f, 0.92f, 0.50f, plumeA, 1.0f, distScale);
                    });
                    poseStack.popPose();
                }
            }

            // Tip wake: crossed torn ribbons trailing off the striking tip,
            // widening backward, so the extension's speed has a shape.
            drawWakeRibbons(poseStack, collector, renderType, seed ^ 0x9A7EL,
                currentLength, 1.0f * fpScale, finalAlpha, distScale);
        }

        // Gold strike glint on the tip: two seeded shard fans, a wide gold
        // burst and a tight bright core. The impact entity is the hit itself.
        if (currentLength > 1.0f) {
            final float starR = 0.55f * (1.0f + 0.15f * (float) Math.sin(age * 3.0f)) * fpScale;
            poseStack.pushPose();
            poseStack.translate(0.0f, 0.0f, currentLength);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                com.dragonblockarcanedba.client.render.ki.BespokeGeometry.shardFan(
                    pose, buffer, seed ^ 0x57A1L, starR,
                    1.0f, 0.95f, 0.40f, 0.90f * finalAlpha, 1.0f, distScale);
                com.dragonblockarcanedba.client.render.ki.BespokeGeometry.shardFan(
                    pose, buffer, seed ^ 0x57A2L, starR * 0.55f,
                    1.0f, 0.98f, 0.85f, 0.90f * finalAlpha, 1.0f, distScale);
            });
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    /**
     * One shaft stretch as two crossed torn ribbons: the builder runs along
     * +X, so the pose turns +X onto the pole axis (+Z) and one ribbon's
     * width spans local X, the other's local Y. Orchestration only; all
     * geometry comes from BespokeGeometry.tornRibbon.
     */
    private static void drawShaftRibbons(PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType,
                                         long seed, float zFrom, float zTo, float width,
                                         float r, float g, float b, float alpha,
                                         float envelope, float distScale) {
        final float len = zTo - zFrom;
        if (len <= 0.01f) return;
        for (int cross = 0; cross < 2; cross++) {
            final boolean crossed = cross == 1;
            final long ribbonSeed = seed ^ (crossed ? 0xC2055L : 0L);
            poseStack.pushPose();
            poseStack.translate(0.0f, 0.0f, zFrom);
            if (crossed) {
                poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(90.0f)));
            }
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                com.dragonblockarcanedba.client.render.ki.BespokeGeometry.tornRibbon(
                    pose, buffer, ribbonSeed, len, width, 0.0f,
                    r, g, b, alpha, envelope, distScale);
            });
            poseStack.popPose();
        }
    }

    /**
     * The tip wake: two crossed torn ribbons starting at the tip and
     * running back toward the hand (builder +X turned onto local -Z).
     */
    private static void drawWakeRibbons(PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType,
                                        long seed, float tipZ, float width,
                                        float envelope, float distScale) {
        for (int cross = 0; cross < 2; cross++) {
            final boolean crossed = cross == 1;
            final long ribbonSeed = seed ^ (crossed ? 0xC2055L : 0L);
            poseStack.pushPose();
            poseStack.translate(0.0f, 0.0f, tipZ);
            if (crossed) {
                poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(90.0f)));
            }
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                com.dragonblockarcanedba.client.render.ki.BespokeGeometry.tornRibbon(
                    pose, buffer, ribbonSeed, 1.3f, width, 0.12f,
                    1.0f, 0.95f, 0.75f, 0.16f, envelope, distScale);
            });
            poseStack.popPose();
        }
    }
}
