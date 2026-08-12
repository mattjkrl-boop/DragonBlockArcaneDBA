package com.dragonblockarcanedba.client.render.ki;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/**
 * Shared rendering utilities for Ki attack vanilla renderers.
 * All Ki effects use a plain white texture and vertex colors for coloring.
 */
public final class KiRenderHelper {

    private KiRenderHelper() {}

    /** White texture used as a canvas for vertex-colored rendering. */
    public static final Identifier WHITE_TEXTURE = DragonBlockArcaneDBA.id("textures/entity/ki_white.png");

    /** Emissive translucent render type — glows at full brightness, supports transparency. */
    public static RenderType kiRenderType() {
        return RenderTypes.entityTranslucentEmissive(WHITE_TEXTURE);
    }

    /** Full brightness light value. */
    public static final int FULL_BRIGHT = 0xF000F0;
    /** No overlay. */
    public static final int NO_OVERLAY = 655360; // OverlayTexture.NO_OVERLAY = 10 << 16

    public static float red(int color)   { return ((color >> 16) & 0xFF) / 255.0f; }
    public static float green(int color) { return ((color >> 8) & 0xFF) / 255.0f; }
    public static float blue(int color)  { return (color & 0xFF) / 255.0f; }

    /**
     * Draws an axis-aligned colored box from (x1,y1,z1) to (x2,y2,z2).
     * Uses the pose from submitCustomGeometry's callback.
     */
    public static void drawColoredBox(PoseStack.Pose pose, VertexConsumer buffer,
                                       float x1, float y1, float z1,
                                       float x2, float y2, float z2,
                                       float r, float g, float b, float a) {
        Matrix4f mat = pose.pose();

        // Bottom (Y-)
        vertex(buffer, mat, pose, x1, y1, z1, r, g, b, a, 0, 0, 0, -1, 0);
        vertex(buffer, mat, pose, x2, y1, z1, r, g, b, a, 1, 0, 0, -1, 0);
        vertex(buffer, mat, pose, x2, y1, z2, r, g, b, a, 1, 1, 0, -1, 0);
        vertex(buffer, mat, pose, x1, y1, z2, r, g, b, a, 0, 1, 0, -1, 0);

        // Top (Y+)
        vertex(buffer, mat, pose, x1, y2, z2, r, g, b, a, 0, 0, 0, 1, 0);
        vertex(buffer, mat, pose, x2, y2, z2, r, g, b, a, 1, 0, 0, 1, 0);
        vertex(buffer, mat, pose, x2, y2, z1, r, g, b, a, 1, 1, 0, 1, 0);
        vertex(buffer, mat, pose, x1, y2, z1, r, g, b, a, 0, 1, 0, 1, 0);

        // North (Z-)
        vertex(buffer, mat, pose, x2, y2, z1, r, g, b, a, 0, 0, 0, 0, -1);
        vertex(buffer, mat, pose, x1, y2, z1, r, g, b, a, 1, 0, 0, 0, -1);
        vertex(buffer, mat, pose, x1, y1, z1, r, g, b, a, 1, 1, 0, 0, -1);
        vertex(buffer, mat, pose, x2, y1, z1, r, g, b, a, 0, 1, 0, 0, -1);

        // South (Z+)
        vertex(buffer, mat, pose, x1, y2, z2, r, g, b, a, 0, 0, 0, 0, 1);
        vertex(buffer, mat, pose, x2, y2, z2, r, g, b, a, 1, 0, 0, 0, 1);
        vertex(buffer, mat, pose, x2, y1, z2, r, g, b, a, 1, 1, 0, 0, 1);
        vertex(buffer, mat, pose, x1, y1, z2, r, g, b, a, 0, 1, 0, 0, 1);

        // West (X-)
        vertex(buffer, mat, pose, x1, y2, z2, r, g, b, a, 0, 0, -1, 0, 0);
        vertex(buffer, mat, pose, x1, y2, z1, r, g, b, a, 1, 0, -1, 0, 0);
        vertex(buffer, mat, pose, x1, y1, z1, r, g, b, a, 1, 1, -1, 0, 0);
        vertex(buffer, mat, pose, x1, y1, z2, r, g, b, a, 0, 1, -1, 0, 0);

        // East (X+)
        vertex(buffer, mat, pose, x2, y2, z1, r, g, b, a, 0, 0, 1, 0, 0);
        vertex(buffer, mat, pose, x2, y2, z2, r, g, b, a, 1, 0, 1, 0, 0);
        vertex(buffer, mat, pose, x2, y1, z2, r, g, b, a, 1, 1, 1, 0, 0);
        vertex(buffer, mat, pose, x2, y1, z1, r, g, b, a, 0, 1, 1, 0, 0);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f mat, PoseStack.Pose pose,
                                float x, float y, float z,
                                float r, float g, float b, float a,
                                float u, float v,
                                float nx, float ny, float nz) {
        buffer.addVertex(mat, x, y, z)
              .setColor(r, g, b, a)
              .setUv(u, v)
              .setOverlay(NO_OVERLAY)
              .setLight(FULL_BRIGHT)
              .setNormal(pose, nx, ny, nz);
    }
}
