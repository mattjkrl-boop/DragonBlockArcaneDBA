package com.dragonblockarcanedba.client.render.weapon.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public class BladePart extends ModelPart {
    private final float baseHalfWidth, baseHalfDepth;
    private final float length;
    private final float tipHalfWidth, tipHalfDepth;

    public BladePart(float baseHalfWidth, float baseHalfDepth, float length, float tipHalfWidth, float tipHalfDepth) {
        this.baseHalfWidth = baseHalfWidth;
        this.baseHalfDepth = baseHalfDepth;
        this.length = length;
        this.tipHalfWidth = tipHalfWidth;
        this.tipHalfDepth = tipHalfDepth;
    }

    @Override
    protected void renderGeometry(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
        PoseStack.Pose pose = poseStack.last();
        float bw = baseHalfWidth, bd = baseHalfDepth;
        float tw = tipHalfWidth, td = tipHalfDepth;
        float h = length;

        // Front face
        addVertex(pose, buffer, -bw, 0, bd, 0, 0, 1, 0, 1, light, overlay, sprite);
        addVertex(pose, buffer, bw, 0, bd, 0, 0, 1, 1, 1, light, overlay, sprite);
        addVertex(pose, buffer, tw, h, td, 0, 0, 1, 1, 0, light, overlay, sprite);
        addVertex(pose, buffer, -tw, h, td, 0, 0, 1, 0, 0, light, overlay, sprite);

        // Back face
        addVertex(pose, buffer, bw, 0, -bd, 0, 0, -1, 0, 1, light, overlay, sprite);
        addVertex(pose, buffer, -bw, 0, -bd, 0, 0, -1, 1, 1, light, overlay, sprite);
        addVertex(pose, buffer, -tw, h, -td, 0, 0, -1, 1, 0, light, overlay, sprite);
        addVertex(pose, buffer, tw, h, -td, 0, 0, -1, 0, 0, light, overlay, sprite);

        // Top face (tip)
        if (tw > 0 || td > 0) {
            addVertex(pose, buffer, -tw, h, td, 0, 1, 0, 0, 1, light, overlay, sprite);
            addVertex(pose, buffer, tw, h, td, 0, 1, 0, 1, 1, light, overlay, sprite);
            addVertex(pose, buffer, tw, h, -td, 0, 1, 0, 1, 0, light, overlay, sprite);
            addVertex(pose, buffer, -tw, h, -td, 0, 1, 0, 0, 0, light, overlay, sprite);
        }

        // Bottom face (base)
        addVertex(pose, buffer, -bw, 0, -bd, 0, -1, 0, 0, 1, light, overlay, sprite);
        addVertex(pose, buffer, bw, 0, -bd, 0, -1, 0, 1, 1, light, overlay, sprite);
        addVertex(pose, buffer, bw, 0, bd, 0, -1, 0, 1, 0, light, overlay, sprite);
        addVertex(pose, buffer, -bw, 0, bd, 0, -1, 0, 0, 0, light, overlay, sprite);

        // Left face
        addVertex(pose, buffer, -bw, 0, -bd, -1, 0, 0, 0, 1, light, overlay, sprite);
        addVertex(pose, buffer, -bw, 0, bd, -1, 0, 0, 1, 1, light, overlay, sprite);
        addVertex(pose, buffer, -tw, h, td, -1, 0, 0, 1, 0, light, overlay, sprite);
        addVertex(pose, buffer, -tw, h, -td, -1, 0, 0, 0, 0, light, overlay, sprite);

        // Right face
        addVertex(pose, buffer, bw, 0, bd, 1, 0, 0, 0, 1, light, overlay, sprite);
        addVertex(pose, buffer, bw, 0, -bd, 1, 0, 0, 1, 1, light, overlay, sprite);
        addVertex(pose, buffer, tw, h, -td, 1, 0, 0, 1, 0, light, overlay, sprite);
        addVertex(pose, buffer, tw, h, td, 1, 0, 0, 0, 0, light, overlay, sprite);
    }
}
