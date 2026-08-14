package com.dragonblockarcanedba.client.render.weapon.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import java.util.ArrayList;
import java.util.List;

public class ModelPart {
    private final List<ModelPart> children = new ArrayList<>();
    public float x, y, z;
    public float xRot, yRot, zRot;
    public float xScale = 1.0f, yScale = 1.0f, zScale = 1.0f;
    protected int color = 0xFFFFFFFF; // Default white

    public ModelPart setColor(int color) {
        this.color = color;
        return this;
    }

    public ModelPart setPos(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public ModelPart setRot(float xRot, float yRot, float zRot) {
        this.xRot = xRot;
        this.yRot = yRot;
        this.zRot = zRot;
        return this;
    }

    public ModelPart setScale(float xScale, float yScale, float zScale) {
        this.xScale = xScale;
        this.yScale = yScale;
        this.zScale = zScale;
        return this;
    }

    public void addChild(ModelPart child) {
        this.children.add(child);
    }

    public void render(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
        poseStack.pushPose();
        
        poseStack.translate(this.x / 16.0f, this.y / 16.0f, this.z / 16.0f);
        if (this.zRot != 0.0F) poseStack.mulPose(Axis.ZP.rotation(this.zRot));
        if (this.yRot != 0.0F) poseStack.mulPose(Axis.YP.rotation(this.yRot));
        if (this.xRot != 0.0F) poseStack.mulPose(Axis.XP.rotation(this.xRot));
        poseStack.scale(this.xScale, this.yScale, this.zScale);

        poseStack.pushPose();
        poseStack.scale(1.0f / 16.0f, 1.0f / 16.0f, 1.0f / 16.0f);
        this.renderGeometry(poseStack, buffer, light, overlay, sprite);
        poseStack.popPose();

        for (ModelPart child : this.children) {
            child.render(poseStack, buffer, light, overlay, sprite);
        }

        poseStack.popPose();
    }

    protected void renderGeometry(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
        // To be overridden by subclasses
    }

    // Helper to get RGBA components
    protected float getRed() { return ((color >> 16) & 0xFF) / 255.0f; }
    protected float getGreen() { return ((color >> 8) & 0xFF) / 255.0f; }
    protected float getBlue() { return (color & 0xFF) / 255.0f; }
    protected float getAlpha() { return ((color >> 24) & 0xFF) / 255.0f; }
    
    // Add vertex helper
    protected void addVertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, float nx, float ny, float nz, float u, float v, int light, int overlay, net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
        float su = sprite != null ? sprite.getU0() + u * (sprite.getU1() - sprite.getU0()) : u;
        float sv = sprite != null ? sprite.getV0() + v * (sprite.getV1() - sprite.getV0()) : v;

        buffer.addVertex(pose.pose(), x, y, z)
              .setColor(getRed(), getGreen(), getBlue(), getAlpha())
              .setUv(su, sv)
              .setOverlay(overlay)
              .setLight(light)
              .setNormal(pose, nx, ny, nz);
    }
}
