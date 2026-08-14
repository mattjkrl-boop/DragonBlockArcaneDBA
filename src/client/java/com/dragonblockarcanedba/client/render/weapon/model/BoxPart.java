package com.dragonblockarcanedba.client.render.weapon.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public class BoxPart extends ModelPart {
    private final float halfWidth, halfHeight, halfDepth;

    public BoxPart(float halfWidth, float halfHeight, float halfDepth) {
        this.halfWidth = halfWidth;
        this.halfHeight = halfHeight;
        this.halfDepth = halfDepth;
    }

    @Override
    protected void renderGeometry(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
        PoseStack.Pose pose = poseStack.last();
        float w = halfWidth, h = halfHeight, d = halfDepth;

        // Front face
        addVertex(pose, buffer, -w, -h, d, 0, 0, 1, 0, 1, light, overlay, sprite);
        addVertex(pose, buffer, w, -h, d, 0, 0, 1, 1, 1, light, overlay, sprite);
        addVertex(pose, buffer, w, h, d, 0, 0, 1, 1, 0, light, overlay, sprite);
        addVertex(pose, buffer, -w, h, d, 0, 0, 1, 0, 0, light, overlay, sprite);

        // Back face
        addVertex(pose, buffer, w, -h, -d, 0, 0, -1, 0, 1, light, overlay, sprite);
        addVertex(pose, buffer, -w, -h, -d, 0, 0, -1, 1, 1, light, overlay, sprite);
        addVertex(pose, buffer, -w, h, -d, 0, 0, -1, 1, 0, light, overlay, sprite);
        addVertex(pose, buffer, w, h, -d, 0, 0, -1, 0, 0, light, overlay, sprite);

        // Top face
        addVertex(pose, buffer, -w, h, d, 0, 1, 0, 0, 1, light, overlay, sprite);
        addVertex(pose, buffer, w, h, d, 0, 1, 0, 1, 1, light, overlay, sprite);
        addVertex(pose, buffer, w, h, -d, 0, 1, 0, 1, 0, light, overlay, sprite);
        addVertex(pose, buffer, -w, h, -d, 0, 1, 0, 0, 0, light, overlay, sprite);

        // Bottom face
        addVertex(pose, buffer, -w, -h, -d, 0, -1, 0, 0, 1, light, overlay, sprite);
        addVertex(pose, buffer, w, -h, -d, 0, -1, 0, 1, 1, light, overlay, sprite);
        addVertex(pose, buffer, w, -h, d, 0, -1, 0, 1, 0, light, overlay, sprite);
        addVertex(pose, buffer, -w, -h, d, 0, -1, 0, 0, 0, light, overlay, sprite);

        // Left face
        addVertex(pose, buffer, -w, -h, -d, -1, 0, 0, 0, 1, light, overlay, sprite);
        addVertex(pose, buffer, -w, -h, d, -1, 0, 0, 1, 1, light, overlay, sprite);
        addVertex(pose, buffer, -w, h, d, -1, 0, 0, 1, 0, light, overlay, sprite);
        addVertex(pose, buffer, -w, h, -d, -1, 0, 0, 0, 0, light, overlay, sprite);

        // Right face
        addVertex(pose, buffer, w, -h, d, 1, 0, 0, 0, 1, light, overlay, sprite);
        addVertex(pose, buffer, w, -h, -d, 1, 0, 0, 1, 1, light, overlay, sprite);
        addVertex(pose, buffer, w, h, -d, 1, 0, 0, 1, 0, light, overlay, sprite);
        addVertex(pose, buffer, w, h, d, 1, 0, 0, 0, 0, light, overlay, sprite);
    }
}
