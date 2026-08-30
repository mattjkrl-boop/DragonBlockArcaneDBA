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

    @Override
    public boolean shouldRender(PowerPoleExtensionEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
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

        // Dynamic extension: Shoots forward at hypersonic speed in first 2.0 ticks, then holds and retracts
        float extendProgress;
        if (age < 2.0f) {
            float t = age / 2.0f;
            extendProgress = 1.0f - (float) Math.pow(1.0f - t, 3); // Fast ease-out thrust
        } else {
            extendProgress = 1.0f;
        }

        // Dissolve / retract fade over last 3 ticks of 12-tick lifespan
        float alphaMult = 1.0f;
        if (age > 9.0f) {
            alphaMult = Math.max(0.0f, (12.0f - age) / 3.0f);
        }

        float currentLength = Math.max(0.8f, maxLength * extendProgress);
        final float finalAlpha = alphaMult;
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

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
            float shaftRadius = 0.13f * fpScale;
            draw12SidedShaft(matrix, buffer, shaftRadius, Math.max(baseZStart, 0.4f), currentLength - 0.4f,
                0.85f, 0.08f, 0.08f, finalAlpha);

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

                // Rounded/Faceted Gold Dome Tip at Leading Edge
                drawDomeTip(matrix, buffer, capRadius, currentLength, 1.0f, 0.90f, 0.20f, finalAlpha);
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

            // 4. Supersonic Mach Shock Cones (Sliding along pole during thrust)
            if (extendProgress > 0.3f) {
                int shockCount = 3;
                for (int s = 0; s < shockCount; s++) {
                    float shockProgress = ((age * 0.45f) + (s / (float) shockCount)) % 1.0f;
                    float shockZ = shockProgress * currentLength;
                    if (shockZ > 0.5f && shockZ < currentLength - 0.5f) {
                        float shockRadius = 0.65f * (1.0f - (shockZ / currentLength) * 0.3f) * fpScale;
                        float coneLen = 0.75f * fpScale;
                        float shockAlpha = (1.0f - shockProgress) * 0.65f * finalAlpha;

                        drawMachCone(matrix, buffer, shockZ, shockZ - coneLen, shockRadius,
                            1.0f, 0.92f, 0.50f, shockAlpha);
                    }
                }
            }

            // 5. Leading-Edge Thrust Starburst Corona
            if (currentLength > 1.0f) {
                float tipZ = currentLength;
                float starR = 0.55f * (1.0f + 0.15f * (float) Math.sin(age * 3.0f)) * fpScale;
                drawStarburst(matrix, buffer, tipZ, starR, 1.0f, 0.95f, 0.4f, 0.90f * finalAlpha);
            }
        });

        poseStack.popPose();
    }

    private static void draw12SidedShaft(Matrix4f matrix, VertexConsumer buffer, float radius, float z1, float z2, float r, float g, float b, float a) {
        int sides = 12;
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
        int sides = 12;
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
        int sides = 12;
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
        int sides = 12;
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

            drawQuad(matrix, buffer,
                -x * 0.15f, -y * 0.15f, z,
                x * 0.15f, y * 0.15f, z,
                x, y, z + 0.05f,
                0, 0, z + 0.12f,
                r, g, b, a
            );
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
