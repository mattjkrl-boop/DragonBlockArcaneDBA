package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.DarknessBladeEntity;
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
 * Entity Renderer for Darkness Blade falling execution in MC 26.2.
 * Renders a monumental 3D demonic execution greatsword with multi-beveled diamond blade,
 * 3D horned crossguard, pommel, central crimson runic fuller, and supersonic plunge wake.
 */
public class DarknessBladeRenderer extends EntityRenderer<DarknessBladeEntity, DarknessBladeRenderer.DarknessBladeRenderState> {

    public DarknessBladeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class DarknessBladeRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
    }

    @Override
    public boolean shouldRender(DarknessBladeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public DarknessBladeRenderState createRenderState() {
        return new DarknessBladeRenderState();
    }

    @Override
    public void extractRenderState(DarknessBladeEntity entity, DarknessBladeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(DarknessBladeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f)); // Point downward toward earth
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.yRot));

        float bladeLength = 7.5f;
        float bladeWidth = 1.0f;
        float age = state.age;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Demonic Blood-Purple Outer Aura Halo
            draw3DGreatsword(matrix, buffer, bladeWidth + 0.35f, bladeLength + 0.4f, 0.30f, 0.65f, 0.05f, 0.95f, 0.80f);

            // 2. Abyssal Obsidian Core Body
            draw3DGreatsword(matrix, buffer, bladeWidth, bladeLength, 0.16f, 0.06f, 0.0f, 0.12f, 0.98f);

            // 3. Central Glowing Crimson Blood Fuller / Rune
            draw3DGreatsword(matrix, buffer, bladeWidth * 0.32f, bladeLength * 0.85f, 0.18f, 1.0f, 0.05f, 0.25f, 1.0f);

            // 4. 3D Horned Crossguard
            drawCrossguard(matrix, buffer, bladeWidth * 2.2f, 0.4f, 0.22f, 0.10f, 0.0f, 0.20f, 1.0f);
            drawCrossguard(matrix, buffer, bladeWidth * 2.0f, 0.3f, 0.25f, 0.85f, 0.05f, 0.35f, 0.95f);

            // 5. 3D Hilt Grip & Pommel
            drawHilt(matrix, buffer, bladeLength, 0.16f, 1.8f, 0.12f, 0.02f, 0.18f, 1.0f);

            // 6. Supersonic Downward Plunge Wake Cones
            for (int i = 0; i < 3; i++) {
                float coneLen = 2.5f + i * 1.5f;
                float coneWidth = (bladeWidth + 0.4f) * (1.0f + i * 0.5f);
                float coneZ = -bladeLength * 0.5f - (i * 1.8f);
                float alpha = 0.55f - (i * 0.15f);

                drawPlungeCone(matrix, buffer, coneWidth, coneLen, coneZ, 0.55f, 0.05f, 0.90f, alpha);
            }
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void draw3DGreatsword(Matrix4f matrix, VertexConsumer consumer, float width, float length, float thickness, float r, float g, float b, float a) {
        float halfLen = length * 0.5f;
        float tipZ = halfLen + width * 1.6f;

        // Front Face (Double diamond cross-section)
        // Left Flank
        consumer.addVertex(matrix, -width, 0, -halfLen).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, thickness, -halfLen).setColor(r, g, b, a).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, thickness, halfLen).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, -width, 0, halfLen).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Right Flank
        consumer.addVertex(matrix, 0, thickness, -halfLen).setColor(r, g, b, a).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, width, 0, -halfLen).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, width, 0, halfLen).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, thickness, halfLen).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Back Face
        // Left Flank Back
        consumer.addVertex(matrix, -width, 0, halfLen).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, 0, -thickness, halfLen).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, 0, -thickness, -halfLen).setColor(r, g, b, a).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, -width, 0, -halfLen).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);

        // Right Flank Back
        consumer.addVertex(matrix, 0, -thickness, halfLen).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, width, 0, halfLen).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, width, 0, -halfLen).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, 0, -thickness, -halfLen).setColor(r, g, b, a).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);

        // Blade Tip (Front Point)
        consumer.addVertex(matrix, -width, 0, halfLen).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, width, 0, halfLen).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, 0, tipZ).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, 0, tipZ).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Blade Tip (Back Point)
        consumer.addVertex(matrix, 0, 0, tipZ).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, 0, 0, tipZ).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, width, 0, halfLen).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, -width, 0, halfLen).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }

    private static void drawCrossguard(Matrix4f matrix, VertexConsumer consumer, float span, float height, float thickness, float r, float g, float b, float a) {
        float z = -3.75f;
        float hHalf = height * 0.5f;

        // Front Face
        consumer.addVertex(matrix, -span, hHalf, z).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, span, hHalf, z).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, span, -hHalf, z).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, -span, -hHalf, z).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Reverse
        consumer.addVertex(matrix, -span, -hHalf, z).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, span, -hHalf, z).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, span, hHalf, z).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, -span, hHalf, z).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }

    private static void drawHilt(Matrix4f matrix, VertexConsumer consumer, float bladeLen, float gripRadius, float hiltLen, float r, float g, float b, float a) {
        float startZ = -bladeLen * 0.5f;
        float endZ = startZ - hiltLen;

        // Grip Cylinder approximation (4 quads)
        consumer.addVertex(matrix, -gripRadius, -gripRadius, startZ).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, gripRadius, -gripRadius, startZ).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, gripRadius, -gripRadius, endZ).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, -gripRadius, -gripRadius, endZ).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        consumer.addVertex(matrix, -gripRadius, gripRadius, endZ).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, gripRadius, gripRadius, endZ).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, gripRadius, gripRadius, startZ).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, -gripRadius, gripRadius, startZ).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Pommel Octahedron
        float pRadius = gripRadius * 2.2f;
        consumer.addVertex(matrix, -pRadius, 0, endZ).setColor(r * 2.0f, g * 2.0f, b * 2.0f, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, pRadius, 0, endZ).setColor(r * 2.0f, g * 2.0f, b * 2.0f, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, 0, endZ - pRadius * 1.5f).setColor(r * 2.0f, g * 2.0f, b * 2.0f, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, 0, endZ - pRadius * 1.5f).setColor(r * 2.0f, g * 2.0f, b * 2.0f, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawPlungeCone(Matrix4f matrix, VertexConsumer consumer, float width, float length, float startZ, float r, float g, float b, float a) {
        float endZ = startZ - length;

        consumer.addVertex(matrix, -width, 0, startZ).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, width, 0, startZ).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, 0, endZ).setColor(r, g, b, 0.0f).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, 0, endZ).setColor(r, g, b, 0.0f).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }
}
