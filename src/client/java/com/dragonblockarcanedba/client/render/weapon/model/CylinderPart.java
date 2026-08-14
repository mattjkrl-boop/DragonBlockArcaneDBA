package com.dragonblockarcanedba.client.render.weapon.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public class CylinderPart extends ModelPart {
    private final float radius;
    private final float length;
    private final int segments;

    public CylinderPart(float radius, float length, int segments) {
        this.radius = radius;
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

            float x1 = (float) (radius * Math.cos(a1));
            float z1 = (float) (radius * Math.sin(a1));
            
            float x2 = (float) (radius * Math.cos(a2));
            float z2 = (float) (radius * Math.sin(a2));

            float nx = (float) Math.cos((a1 + a2) / 2);
            float nz = (float) Math.sin((a1 + a2) / 2);

            // Side quad
            addVertex(pose, buffer, x1, 0, z1, nx, 0, nz, 0, 1, light, overlay, sprite);
            addVertex(pose, buffer, x2, 0, z2, nx, 0, nz, 1, 1, light, overlay, sprite);
            addVertex(pose, buffer, x2, length, z2, nx, 0, nz, 1, 0, light, overlay, sprite);
            addVertex(pose, buffer, x1, length, z1, nx, 0, nz, 0, 0, light, overlay, sprite);

            // Top triangle
            addVertex(pose, buffer, 0, length, 0, 0, 1, 0, 0.5f, 0.5f, light, overlay, sprite);
            addVertex(pose, buffer, x1, length, z1, 0, 1, 0, 0, 0, light, overlay, sprite);
            addVertex(pose, buffer, x2, length, z2, 0, 1, 0, 1, 0, light, overlay, sprite);
            // 4th vertex for quad-based buffers
            addVertex(pose, buffer, x2, length, z2, 0, 1, 0, 1, 0, light, overlay, sprite);

            // Bottom triangle
            addVertex(pose, buffer, 0, 0, 0, 0, -1, 0, 0.5f, 0.5f, light, overlay, sprite);
            addVertex(pose, buffer, x2, 0, z2, 0, -1, 0, 1, 1, light, overlay, sprite);
            addVertex(pose, buffer, x1, 0, z1, 0, -1, 0, 0, 1, light, overlay, sprite);
            addVertex(pose, buffer, x1, 0, z1, 0, -1, 0, 0, 1, light, overlay, sprite);
        }
    }
}
