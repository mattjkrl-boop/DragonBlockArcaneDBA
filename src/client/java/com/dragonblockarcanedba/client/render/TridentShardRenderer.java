package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.TridentShardEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Entity Renderer for Trident Shard in Minecraft 26.2.
 * Renders a high-detail physical 3D Demonic Trident Blade / Spearhead:
 * - Diamond-faceted dark obsidian/crimson blade body with razor-sharp cutting bevels.
 * - Raised glowing blood-red fuller spine and pulsating demonic rune core.
 * - Symmetrical barbed lateral mini-prongs extending from the collar.
 * - Volumetric 3D helical crimson energy wake vortex ribbons.
 */
public class TridentShardRenderer extends EntityRenderer<TridentShardEntity, TridentShardRenderer.ShardRenderState> {

    public TridentShardRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ShardRenderState extends EntityRenderState {
        public float ageInTicks = 0;
        public float yRot = 0;
        public float xRot = 0;
    }

    @Override
    public ShardRenderState createRenderState() {
        return new ShardRenderState();
    }

    @Override
    public void extractRenderState(TridentShardEntity entity, ShardRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.ageInTicks = entity.tickCount + partialTicks;
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
    }

    @Override
    public void submit(ShardRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        // Orient with flight/orbit direction
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
        // High-speed axial roll
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.ageInTicks * 22.0f));
        poseStack.scale(0.85f, 0.85f, 0.85f);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            float pulse = 0.85f + 0.15f * Mth.sin(state.ageInTicks * 0.35f);

            // 1. Physical 3D Demonic Trident Blade & Barbs
            drawDemonicBlade(matrix, buffer, state.ageInTicks, pulse);

            // 2. Volumetric 3D Helical Crimson Trail Ribbon Wake
            drawHelicalWake(matrix, buffer, state.ageInTicks);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void drawDemonicBlade(Matrix4f matrix, VertexConsumer consumer, float age, float pulse) {
        float bladeLength = 0.95f;
        float bladeHalfWidth = 0.20f;
        float spineThickness = 0.065f;
        float baseZ = -0.30f;

        // 1. Faceted Obsidian / Crimson Blade Faces
        // Top Left Face (Dark Demonic Steel)
        drawTriangle(matrix, consumer,
            0, spineThickness, baseZ,
            -bladeHalfWidth, 0, 0,
            0, 0, bladeLength,
            0.18f, 0.02f, 0.06f, 0.95f
        );
        // Top Right Face (Crimson Bevel)
        drawTriangle(matrix, consumer,
            0, spineThickness, baseZ,
            0, 0, bladeLength,
            bladeHalfWidth, 0, 0,
            0.45f, 0.02f, 0.08f, 0.95f
        );
        // Bottom Left Face
        drawTriangle(matrix, consumer,
            0, -spineThickness, baseZ,
            0, 0, bladeLength,
            -bladeHalfWidth, 0, 0,
            0.18f, 0.02f, 0.06f, 0.95f
        );
        // Bottom Right Face
        drawTriangle(matrix, consumer,
            0, -spineThickness, baseZ,
            bladeHalfWidth, 0, 0,
            0, 0, bladeLength,
            0.45f, 0.02f, 0.08f, 0.95f
        );

        // 2. Central Raised Blood-Red Spine / Fuller
        float fullerWidth = 0.045f;
        drawQuad3D(matrix, consumer,
            -fullerWidth, spineThickness * 1.05f, baseZ,
            fullerWidth, spineThickness * 1.05f, baseZ,
            fullerWidth, spineThickness * 0.35f, bladeLength * 0.75f,
            -fullerWidth, spineThickness * 0.35f, bladeLength * 0.75f,
            1.0f, 0.10f, 0.20f, 1.0f * pulse
        );
        // Bottom Fuller
        drawQuad3D(matrix, consumer,
            -fullerWidth, -spineThickness * 1.05f, baseZ,
            -fullerWidth, -spineThickness * 0.35f, bladeLength * 0.75f,
            fullerWidth, -spineThickness * 0.35f, bladeLength * 0.75f,
            fullerWidth, -spineThickness * 1.05f, baseZ,
            1.0f, 0.10f, 0.20f, 1.0f * pulse
        );

        // 3. Blinding Crimson Cutting Edge Lines
        drawEdgeLine(matrix, consumer, -bladeHalfWidth, 0, 0, 0, 0, bladeLength, 0.025f, 1.0f, 0.35f, 0.45f, 1.0f);
        drawEdgeLine(matrix, consumer, bladeHalfWidth, 0, 0, 0, 0, bladeLength, 0.025f, 1.0f, 0.35f, 0.45f, 1.0f);

        // 4. Symmetrical Barbed Lateral Flanking Mini-Prongs
        float barbX = 0.32f;
        float barbZ = 0.22f;
        // Left Barb
        drawTriangle(matrix, consumer,
            -0.08f, 0, baseZ,
            -barbX, 0, barbZ,
            -0.08f, 0, 0.10f,
            0.85f, 0.05f, 0.15f, 0.95f
        );
        drawEdgeLine(matrix, consumer, -0.08f, 0, baseZ, -barbX, 0, barbZ, 0.02f, 1.0f, 0.4f, 0.5f, 1.0f);
        // Right Barb
        drawTriangle(matrix, consumer,
            0.08f, 0, baseZ,
            0.08f, 0, 0.10f,
            barbX, 0, barbZ,
            0.85f, 0.05f, 0.15f, 0.95f
        );
        drawEdgeLine(matrix, consumer, 0.08f, 0, baseZ, barbX, 0, barbZ, 0.02f, 1.0f, 0.4f, 0.5f, 1.0f);

        // 5. Pulsating Demonic Core Crystal in Base Collar
        float coreRadius = 0.07f * pulse;
        drawQuad3D(matrix, consumer,
            -coreRadius, coreRadius, baseZ,
            coreRadius, coreRadius, baseZ,
            coreRadius, -coreRadius, baseZ,
            -coreRadius, -coreRadius, baseZ,
            1.0f, 0.85f, 0.90f, 1.0f
        );
    }

    private static void drawHelicalWake(Matrix4f matrix, VertexConsumer consumer, float age) {
        int steps = 10;
        float trailLength = 1.2f;

        for (int strand = 0; strand < 2; strand++) {
            double strandOffset = strand * Math.PI;
            for (int i = 0; i < steps; i++) {
                float p1 = i / (float) steps;
                float p2 = (i + 1) / (float) steps;

                float z1 = -0.25f - (p1 * trailLength);
                float z2 = -0.25f - (p2 * trailLength);

                float r1 = 0.18f * (1.0f + p1 * 0.4f);
                float r2 = 0.18f * (1.0f + p2 * 0.4f);

                double a1 = strandOffset + age * 0.7 + (p1 * Math.PI * 2.5);
                double a2 = strandOffset + age * 0.7 + (p2 * Math.PI * 2.5);

                float x1 = (float) Math.cos(a1) * r1;
                float y1 = (float) Math.sin(a1) * r1;
                float x2 = (float) Math.cos(a2) * r2;
                float y2 = (float) Math.sin(a2) * r2;

                float alpha = (1.0f - p1) * 0.75f;
                drawCrackLine(matrix, consumer, x1, y1, z1, x2, y2, z2, 0.035f, 0.95f, 0.05f, 0.20f, alpha);
            }
        }
    }

    private static void drawTriangle(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawQuad3D(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawEdgeLine(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float width, float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1 - width, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1 + width, y1, z1).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 + width, y2, z2).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 - width, y2, z2).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawCrackLine(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float width, float r, float g, float b, float a) {
        float dx = x2 - x1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001f) return;

        float nx = -dz / len * width * 0.5f;
        float nz = dx / len * width * 0.5f;

        consumer.addVertex(matrix, x1 - nx, y1, z1 - nz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1 + nx, y1, z1 + nz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 + nx, y2, z2 + nz).setColor(r, g, b, 0.0f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2 - nx, y2, z2 - nz).setColor(r, g, b, 0.0f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }
}
