package com.dragonblockarcanedba.client.render.weapon.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public class RingPart extends ModelPart {
    private final float innerRadius;
    private final float outerRadius;
    private final float thickness;
    private final int segments;

    public RingPart(float innerRadius, float outerRadius, float thickness, int segments) {
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
        this.thickness = thickness;
        this.segments = segments;
    }

    @Override
    protected void renderGeometry(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
        PoseStack.Pose pose = poseStack.last();
        
        float angleStep = (float) (2 * Math.PI / segments);
        float h = thickness / 2.0f;

        for (int i = 0; i < segments; i++) {
            float a1 = i * angleStep;
            float a2 = (i + 1) * angleStep;

            float cos1 = (float) Math.cos(a1);
            float sin1 = (float) Math.sin(a1);
            float cos2 = (float) Math.cos(a2);
            float sin2 = (float) Math.sin(a2);

            float ix1 = innerRadius * cos1;
            float iz1 = innerRadius * sin1;
            float ix2 = innerRadius * cos2;
            float iz2 = innerRadius * sin2;

            float ox1 = outerRadius * cos1;
            float oz1 = outerRadius * sin1;
            float ox2 = outerRadius * cos2;
            float oz2 = outerRadius * sin2;

            // Top face
            addVertex(pose, buffer, ix1, h, iz1, 0, 1, 0, 0, 1, light, overlay, sprite);
            addVertex(pose, buffer, ox1, h, oz1, 0, 1, 0, 1, 1, light, overlay, sprite);
            addVertex(pose, buffer, ox2, h, oz2, 0, 1, 0, 1, 0, light, overlay, sprite);
            addVertex(pose, buffer, ix2, h, iz2, 0, 1, 0, 0, 0, light, overlay, sprite);

            // Bottom face
            addVertex(pose, buffer, ix1, -h, iz1, 0, -1, 0, 0, 1, light, overlay, sprite);
            addVertex(pose, buffer, ix2, -h, iz2, 0, -1, 0, 0, 0, light, overlay, sprite);
            addVertex(pose, buffer, ox2, -h, oz2, 0, -1, 0, 1, 0, light, overlay, sprite);
            addVertex(pose, buffer, ox1, -h, oz1, 0, -1, 0, 1, 1, light, overlay, sprite);

            // Outer side face
            float nxO = (cos1 + cos2) / 2;
            float nzO = (sin1 + sin2) / 2;
            addVertex(pose, buffer, ox1, -h, oz1, nxO, 0, nzO, 0, 1, light, overlay, sprite);
            addVertex(pose, buffer, ox2, -h, oz2, nxO, 0, nzO, 1, 1, light, overlay, sprite);
            addVertex(pose, buffer, ox2, h, oz2, nxO, 0, nzO, 1, 0, light, overlay, sprite);
            addVertex(pose, buffer, ox1, h, oz1, nxO, 0, nzO, 0, 0, light, overlay, sprite);

            // Inner side face
            float nxI = -nxO;
            float nzI = -nzO;
            addVertex(pose, buffer, ix1, -h, iz1, nxI, 0, nzI, 0, 1, light, overlay, sprite);
            addVertex(pose, buffer, ix1, h, iz1, nxI, 0, nzI, 0, 0, light, overlay, sprite);
            addVertex(pose, buffer, ix2, h, iz2, nxI, 0, nzI, 1, 0, light, overlay, sprite);
            addVertex(pose, buffer, ix2, -h, iz2, nxI, 0, nzI, 1, 1, light, overlay, sprite);
        }
    }
}
