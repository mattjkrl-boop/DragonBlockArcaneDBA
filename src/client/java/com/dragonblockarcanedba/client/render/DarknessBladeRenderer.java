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
 * Renders a massive 3D polygonal execution greatsword with dark purple runic fuller and crimson energy aura.
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

        float bladeLength = 7.0f;
        float bladeWidth = 0.9f;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Demonic Blood-Purple Outer Aura Halo
            drawSwordBlade(matrix, buffer, bladeWidth + 0.25f, bladeLength + 0.3f, 0.25f, 0.75f, 0.05f, 0.95f, 0.85f);

            // 2. Abyssal Obsidian Core Body
            drawSwordBlade(matrix, buffer, bladeWidth, bladeLength, 0.12f, 0.06f, 0.0f, 0.12f, 0.98f);

            // 3. Central Glowing Crimson Blood Fuller / Rune
            drawSwordBlade(matrix, buffer, bladeWidth * 0.3f, bladeLength * 0.85f, 0.14f, 1.0f, 0.05f, 0.2f, 1.0f);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawSwordBlade(Matrix4f matrix, VertexConsumer consumer, float width, float length, float thickness, float r, float g, float b, float a) {
        float halfLen = length * 0.5f;
        float tipZ = halfLen + width * 1.5f;

        // Front Face (Double triangle / diamond cross section)
        consumer.addVertex(matrix, -width, 0, -halfLen).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, thickness, -halfLen).setColor(r, g, b, a).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, thickness, halfLen).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, -width, 0, halfLen).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        consumer.addVertex(matrix, 0, thickness, -halfLen).setColor(r, g, b, a).setUv(0.5f, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, width, 0, -halfLen).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, width, 0, halfLen).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, thickness, halfLen).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Blade Tip Point
        consumer.addVertex(matrix, -width, 0, halfLen).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, width, 0, halfLen).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, 0, tipZ).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, 0, 0, tipZ).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }
}
