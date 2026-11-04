package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.PowerPoleExtensionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;

/**
 * Entity Renderer for Power Pole Extension in Minecraft 26.2.
 * Renders a physical 3D stretching Power Pole model:
 * - Solid 12-sided crimson pole shaft stretching dynamically across the reach distance
 * - Detailed metallic 3D gold end caps and beveled collar fittings at base and tip
 * - Golden dragon bands coiled along the shaft
 * - Supersonic Mach air compression shock cones and sliding shockwave rings
 * - Leading-edge golden kinetic thrust starburst corona
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

        float currentLength = Math.max(0.8f, maxLength * extendProgress);
        final float finalAlpha = alphaMult;
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        if (state.isFirstPersonOwner) {
            // Anchor extending pole to the hand: way lower and on the weapon hand side (screen right)
            float sideSign = state.onRight ? -1.0f : 1.0f;
            poseStack.translate(sideSign * 0.38f, -0.48f, 0.20f);
        }

        float baseZStart = state.isFirstPersonOwner ? 0.20f : 0.0f;
        float fpScale = state.isFirstPersonOwner ? 0.55f : 1.0f;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Solid 3D Crimson Shaft (scaled down in first person)
            // 3-stop gradient along the length: hot bright crimson at the hand,
            // full crimson mid-shaft, deep crimson toward the striking tip.
            float shaftRadius = 0.13f * fpScale;
            float shaftZ1 = Math.max(baseZStart, 0.4f);
            float shaftZ2 = currentLength - 0.4f;
            if (shaftZ2 > shaftZ1) {
                // Outer layer of the strike trail (standard point 1): a wide,
                // faint pale gold sheath around the shaft, the displaced air
                // and energy the extension drags with it. The crimson bands
                // below are the mid layer and the thin core shaft the core.
                draw12SidedShaft(matrix, buffer, shaftRadius * 2.1f, shaftZ1, shaftZ2,
                    1.0f, 0.88f, 0.66f, finalAlpha * 0.14f);
            }
            if (shaftZ2 > shaftZ1) {
                float band1 = shaftZ1 + (shaftZ2 - shaftZ1) / 3.0f;
                float band2 = shaftZ1 + (shaftZ2 - shaftZ1) * 2.0f / 3.0f;
                draw12SidedShaft(matrix, buffer, shaftRadius, shaftZ1, band1,
                    1.0f, 0.22f, 0.16f, finalAlpha);
                draw12SidedShaft(matrix, buffer, shaftRadius, band1, band2,
                    0.85f, 0.08f, 0.08f, finalAlpha);
                draw12SidedShaft(matrix, buffer, shaftRadius, band2, shaftZ2,
                    0.58f, 0.03f, 0.05f, finalAlpha);
            }

            // Inner glowing red power core
            draw12SidedShaft(matrix, buffer, shaftRadius * 0.55f, Math.max(baseZStart, 0.2f), currentLength - 0.2f,
                1.0f, 0.35f, 0.25f, finalAlpha * 0.85f);

            // 2. Metallic 3D Gold Caps & Beveled Collars
            float capRadius = 0.165f * fpScale;
            // Base Gold Fitting (Z = baseZStart to 0.6)
            draw12SidedShaft(matrix, buffer, capRadius, baseZStart, 0.6f, 1.0f, 0.82f, 0.12f, finalAlpha);
            drawBevelRing(matrix, buffer, capRadius * 1.15f, 0.6f, 1.0f, 0.92f, 0.25f, finalAlpha);

            // Tip Gold Fitting & Heavy Strike Cap (Z = currentLength - 0.9 to currentLength)
            if (currentLength > 1.2f) {
                float tipCapStart = currentLength - 0.9f;
                draw12SidedShaft(matrix, buffer, capRadius, tipCapStart, currentLength, 1.0f, 0.82f, 0.12f, finalAlpha);
                drawBevelRing(matrix, buffer, capRadius * 1.15f, tipCapStart, 1.0f, 0.92f, 0.25f, finalAlpha);

                // Rounded/Faceted Gold Dome Tip at Leading Edge, with a thin bright lip.
                drawDomeTip(matrix, buffer, capRadius, currentLength, 1.0f, 0.90f, 0.20f, finalAlpha);
                drawBevelRing(matrix, buffer, capRadius * 1.45f, currentLength, 1.0f, 0.98f, 0.62f, finalAlpha * 0.8f);
            }

            // 3. Golden Dragon Bands / Helical Coils wrapped along pole
            int helicalSegments = Math.min((int) (currentLength * 3.5f), 70);
            for (int h = 0; h < 2; h++) {
                float strandOffset = h * (float) Math.PI;
                for (int i = 0; i < helicalSegments; i++) {
                    float z1 = (i / (float) helicalSegments) * (currentLength - 1.2f) + 0.6f;
                    float z2 = ((i + 1) / (float) helicalSegments) * (currentLength - 1.2f) + 0.6f;

                    double a1 = strandOffset + (z1 * 1.6) + (age * 0.2);
                    double a2 = strandOffset + (z2 * 1.6) + (age * 0.2);

                    float r = shaftRadius + 0.02f * fpScale;
                    float x1 = (float) Math.cos(a1) * r;
                    float y1 = (float) Math.sin(a1) * r;
                    float x2 = (float) Math.cos(a2) * r;
                    float y2 = (float) Math.sin(a2) * r;

                    float bandW = 0.045f * fpScale;
                    float tx1 = (float) -Math.sin(a1) * bandW;
                    float ty1 = (float) Math.cos(a1) * bandW;
                    float tx2 = (float) -Math.sin(a2) * bandW;
                    float ty2 = (float) Math.cos(a2) * bandW;

                    drawQuad(matrix, buffer,
                        x1 - tx1, y1 - ty1, z1,
                        x1 + tx1, y1 + ty1, z1,
                        x2 + tx2, y2 + ty2, z2,
                        x2 - tx2, y2 - ty2, z2,
                        1.0f, 0.85f, 0.15f, 0.90f * finalAlpha
                    );
                }
            }

            // Mach cones slide the length of the poke and thin out as they go.
            if (currentLength > 1.6f) {
                int shockCount = 3;
                for (int s = 0; s < shockCount; s++) {
                    float shockProgress = ((age * 0.45f) + (s / (float) shockCount)) % 1.0f;
                    float shockEase = shockProgress * shockProgress * (3.0f - 2.0f * shockProgress);
                    float shockZ = 0.7f + shockEase * (currentLength - 1.4f);
                    if (shockZ > 0.8f && shockZ < currentLength - 0.35f) {
                        float shockRadius = 0.52f * (1.0f - shockEase * 0.4f) * fpScale;
                        float coneLen = 0.68f * fpScale;
                        float shockAlpha = (float) Math.sin(shockProgress * (float) Math.PI) * 0.7f * finalAlpha;
                        drawMachCone(matrix, buffer, shockZ, shockZ - coneLen, shockRadius,
                            1.0f, 0.92f, 0.50f, shockAlpha);
                    }
                }
            }

            // Tip wake: a translucent cone trailing off the striking tip,
            // widening backward, so the extension's speed has a shape.
            if (currentLength > 1.6f) {
                drawMachCone(matrix, buffer, currentLength, currentLength - 1.3f,
                    0.5f * fpScale, 1.0f, 0.95f, 0.75f, 0.16f * finalAlpha);
            }

            // Gold strike glint on the tip. The impact entity is the hit itself.
            if (currentLength > 1.0f) {
                float tipZ = currentLength;
                float starR = 0.55f * (1.0f + 0.15f * (float) Math.sin(age * 3.0f)) * fpScale;
                drawStarburst(matrix, buffer, tipZ, starR, 1.0f, 0.95f, 0.4f, 0.90f * finalAlpha);
            }
        });

        poseStack.popPose();
    }

    private static void draw12SidedShaft(Matrix4f matrix, VertexConsumer buffer, float radius, float z1, float z2, float r, float g, float b, float a) {
        int sides = 18;
        for (int i = 0; i < sides; i++) {
            double a1 = (i / (double) sides) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) sides) * Math.PI * 2.0;

            float x1 = (float) Math.cos(a1) * radius;
            float y1 = (float) Math.sin(a1) * radius;
            float x2 = (float) Math.cos(a2) * radius;
            float y2 = (float) Math.sin(a2) * radius;

            // Subtle shading variation across facets
            float shade = 0.82f + 0.18f * (float) Math.cos(a1);
            drawQuad(matrix, buffer,
                x1, y1, z1,
                x2, y2, z1,
                x2, y2, z2,
                x1, y1, z2,
                r * shade, g * shade, b * shade, a
            );
        }
    }

    private static void drawBevelRing(Matrix4f matrix, VertexConsumer buffer, float radius, float z, float r, float g, float b, float a) {
        int sides = 18;
        float ringThick = 0.08f;
        for (int i = 0; i < sides; i++) {
            double a1 = (i / (double) sides) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) sides) * Math.PI * 2.0;

            float x1 = (float) Math.cos(a1) * radius;
            float y1 = (float) Math.sin(a1) * radius;
            float x2 = (float) Math.cos(a2) * radius;
            float y2 = (float) Math.sin(a2) * radius;

            drawQuad(matrix, buffer,
                x1, y1, z - ringThick,
                x2, y2, z - ringThick,
                x2, y2, z + ringThick,
                x1, y1, z + ringThick,
                r, g, b, a
            );
        }
    }

    private static void drawDomeTip(Matrix4f matrix, VertexConsumer buffer, float radius, float baseZ, float r, float g, float b, float a) {
        int sides = 18;
        float tipZ = baseZ + 0.25f;
        for (int i = 0; i < sides; i++) {
            double a1 = (i / (double) sides) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) sides) * Math.PI * 2.0;

            float x1 = (float) Math.cos(a1) * radius;
            float y1 = (float) Math.sin(a1) * radius;
            float x2 = (float) Math.cos(a2) * radius;
            float y2 = (float) Math.sin(a2) * radius;

            drawTriangle(matrix, buffer, x1, y1, baseZ, x2, y2, baseZ, 0, 0, tipZ, r, g, b, a);
        }
    }

    private static void drawMachCone(Matrix4f matrix, VertexConsumer buffer, float tipZ, float baseZ, float baseR, float r, float g, float b, float a) {
        int sides = 18;
        for (int i = 0; i < sides; i++) {
            double a1 = (i / (double) sides) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) sides) * Math.PI * 2.0;

            float x1 = (float) Math.cos(a1) * baseR;
            float y1 = (float) Math.sin(a1) * baseR;
            float x2 = (float) Math.cos(a2) * baseR;
            float y2 = (float) Math.sin(a2) * baseR;

            drawTriangle(matrix, buffer, x1, y1, baseZ, x2, y2, baseZ, 0, 0, tipZ, r, g, b, a);
        }
    }

    private static void drawStarburst(Matrix4f matrix, VertexConsumer buffer, float z, float radius, float r, float g, float b, float a) {
        int points = 8;
        for (int i = 0; i < points; i++) {
            double ang = (i / (double) points) * Math.PI * 2.0;
            float x = (float) Math.cos(ang) * radius;
            float y = (float) Math.sin(ang) * radius;

            float px = (float) -Math.sin(ang);
            float py = (float) Math.cos(ang);
            float reach = 1.0f;
            for (int s = 0; s < 4; s++) {
                float u1 = s / 4.0f;
                float u2 = (s + 1) / 4.0f;
                float bow1 = (float) Math.sin(u1 * Math.PI);
                float bow2 = (float) Math.sin(u2 * Math.PI);
                float w1 = radius * 0.11f * (1.0f - u1) * (0.4f + bow1);
                float w2 = radius * 0.11f * (1.0f - u2) * (0.4f + bow2);
                float x1 = x * u1;
                float y1 = y * u1;
                float x2 = x * u2;
                float y2 = y * u2;
                drawQuad(matrix, buffer,
                    x1 - px * w1, y1 - py * w1, z,
                    x1 + px * w1, y1 + py * w1, z,
                    x2 + px * w2, y2 + py * w2, z + 0.04f * u2,
                    x2 - px * w2, y2 - py * w2, z + 0.04f * u1,
                    r * 0.7f, g * 0.7f, b * 0.55f, a * (0.25f + 0.75f * (1.0f - u1)));
                drawQuad(matrix, buffer,
                    x1 - px * w1 * 0.28f, y1 - py * w1 * 0.28f, z + 0.01f,
                    x1 + px * w1 * 0.28f, y1 + py * w1 * 0.28f, z + 0.01f,
                    x2 + px * w2 * 0.28f, y2 + py * w2 * 0.28f, z + 0.05f,
                    x2 - px * w2 * 0.28f, y2 - py * w2 * 0.28f, z + 0.05f,
                    1.0f, 0.98f, 0.85f, a * (0.3f + 0.7f * u2) * reach);
            }
        }
    }

    private static void drawQuad(Matrix4f matrix, VertexConsumer buffer,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float x4, float y4, float z4,
                                 float r, float g, float b, float a) {
        buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        buffer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        buffer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
    }

    private static void drawTriangle(Matrix4f matrix, VertexConsumer buffer,
                                     float x1, float y1, float z1,
                                     float x2, float y2, float z2,
                                     float x3, float y3, float z3,
                                     float r, float g, float b, float a) {
        buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        buffer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
        buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, 1);
    }
}
