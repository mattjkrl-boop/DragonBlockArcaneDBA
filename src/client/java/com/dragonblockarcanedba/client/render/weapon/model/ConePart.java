package com.dragonblockarcanedba.client.render.weapon.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public class ConePart extends ModelPart {
    private final float baseRadius;
    private final float length;
    private final int segments;

    public ConePart(float baseRadius, float length, int segments) {
        this.baseRadius = baseRadius;
        this.length = length;
        this.segments = segments;
    }

    @Override
    protected void renderGeometry(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
        PoseStack.Pose pose = poseStack.last();
        
        float angleStep = (float) (2 * Math.PI / segments);

        for (int i = 0; i < segments; i++) {
            float a1 = i * angleStep;
            float a2 = (i + 1) * angleStep;

            float x1 = (float) (baseRadius * Math.cos(a1));
            float z1 = (float) (baseRadius * Math.sin(a1));
            
            float x2 = (float) (baseRadius * Math.cos(a2));
            float z2 = (float) (baseRadius * Math.sin(a2));

            float nx = (float) Math.cos((a1 + a2) / 2);
            float nz = (float) Math.sin((a1 + a2) / 2);

            // Side triangle (pointing up to 0, length, 0)
            addVertex(pose, buffer, 0, length, 0, nx, 0.5f, nz, 0.5f, 0, light, overlay, sprite);
            addVertex(pose, buffer, x1, 0, z1, nx, 0.5f, nz, 0, 1, light, overlay, sprite);
            addVertex(pose, buffer, x2, 0, z2, nx, 0.5f, nz, 1, 1, light, overlay, sprite);
            // 4th vertex for quad-based buffers
            addVertex(pose, buffer, x2, 0, z2, nx, 0.5f, nz, 1, 1, light, overlay, sprite);

            // Bottom triangle
            addVertex(pose, buffer, 0, 0, 0, 0, -1, 0, 0.5f, 0.5f, light, overlay, sprite);
            addVertex(pose, buffer, x2, 0, z2, 0, -1, 0, 1, 1, light, overlay, sprite);
            addVertex(pose, buffer, x1, 0, z1, 0, -1, 0, 0, 1, light, overlay, sprite);
            addVertex(pose, buffer, x1, 0, z1, 0, -1, 0, 0, 1, light, overlay, sprite);
        }
    }
}
