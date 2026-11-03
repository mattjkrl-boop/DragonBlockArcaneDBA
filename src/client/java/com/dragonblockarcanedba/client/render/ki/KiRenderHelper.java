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

    /**
     * Tight glow. The eyes shader ignores vertex alpha, so a "faint" halo was a
     * second solid copy of the effect and bloomed into mush. This type honors
     * the low alpha the callers already pass, which keeps the glow on the edge.
     */
    public static RenderType kiAdditiveType() {
        return RenderTypes.entityTranslucentEmissive(WHITE_TEXTURE);
    }

    /** Emissive translucent render type, glows at full brightness and supports transparency. */
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

    public static void vertex(VertexConsumer buffer, Matrix4f mat, PoseStack.Pose pose,
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

    /** Tube along local Z, drawn from both sides so a beam reads from any angle. */
    public static void drawCylinder(PoseStack.Pose pose, VertexConsumer buffer,
                                    float radius, float z0, float z1, int sides,
                                    float r, float g, float b, float a) {
        Matrix4f mat = pose.pose();
        for (int i = 0; i < sides; i++) {
            double a0 = (i / (double) sides) * Math.PI * 2.0;
            double a1 = ((i + 1) / (double) sides) * Math.PI * 2.0;
            float x0 = (float) Math.cos(a0) * radius;
            float y0 = (float) Math.sin(a0) * radius;
            float x1 = (float) Math.cos(a1) * radius;
            float y1 = (float) Math.sin(a1) * radius;
            float nx = (float) Math.cos((a0 + a1) * 0.5);
            float ny = (float) Math.sin((a0 + a1) * 0.5);
            vertex(buffer, mat, pose, x0, y0, z0, r, g, b, a, 0, 0, nx, ny, 0);
            vertex(buffer, mat, pose, x1, y1, z0, r, g, b, a, 1, 0, nx, ny, 0);
            vertex(buffer, mat, pose, x1, y1, z1, r, g, b, a, 1, 1, nx, ny, 0);
            vertex(buffer, mat, pose, x0, y0, z1, r, g, b, a, 0, 1, nx, ny, 0);
            vertex(buffer, mat, pose, x0, y0, z1, r, g, b, a, 0, 1, -nx, -ny, 0);
            vertex(buffer, mat, pose, x1, y1, z1, r, g, b, a, 1, 1, -nx, -ny, 0);
            vertex(buffer, mat, pose, x1, y1, z0, r, g, b, a, 1, 0, -nx, -ny, 0);
            vertex(buffer, mat, pose, x0, y0, z0, r, g, b, a, 0, 0, -nx, -ny, 0);
        }
    }

    /** Flat disc in the local XY plane. inner 0 fills it; a gap makes a razor rim. */
    public static void drawDisc(PoseStack.Pose pose, VertexConsumer buffer,
                                float inner, float outer, int segments,
                                float r, float g, float b, float a) {
        Matrix4f mat = pose.pose();
        for (int i = 0; i < segments; i++) {
            double a0 = (i / (double) segments) * Math.PI * 2.0;
            double a1 = ((i + 1) / (double) segments) * Math.PI * 2.0;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            vertex(buffer, mat, pose, c0 * inner, s0 * inner, 0, r, g, b, a, 0, 0, 0, 0, 1);
            vertex(buffer, mat, pose, c1 * inner, s1 * inner, 0, r, g, b, a, 1, 0, 0, 0, 1);
            vertex(buffer, mat, pose, c1 * outer, s1 * outer, 0, r, g, b, a, 1, 1, 0, 0, 1);
            vertex(buffer, mat, pose, c0 * outer, s0 * outer, 0, r, g, b, a, 0, 1, 0, 0, 1);
            vertex(buffer, mat, pose, c0 * outer, s0 * outer, 0, r, g, b, a, 0, 1, 0, 0, -1);
            vertex(buffer, mat, pose, c1 * outer, s1 * outer, 0, r, g, b, a, 1, 1, 0, 0, -1);
            vertex(buffer, mat, pose, c1 * inner, s1 * inner, 0, r, g, b, a, 1, 0, 0, 0, -1);
            vertex(buffer, mat, pose, c0 * inner, s0 * inner, 0, r, g, b, a, 0, 0, 0, 0, -1);
        }
    }

    /** Latitude-longitude sphere. Enough segments to read as a ball, not a crate. */
    public static void drawSphere(PoseStack.Pose pose, VertexConsumer buffer,
                                  float radius, int stacks, int slices,
                                  float r, float g, float b, float a) {
        Matrix4f mat = pose.pose();
        for (int lat = 0; lat < stacks; lat++) {
            double v0 = Math.PI * lat / stacks;
            double v1 = Math.PI * (lat + 1) / stacks;
            float y0 = (float) Math.cos(v0) * radius;
            float y1 = (float) Math.cos(v1) * radius;
            float rad0 = (float) Math.sin(v0) * radius;
            float rad1 = (float) Math.sin(v1) * radius;
            for (int lon = 0; lon < slices; lon++) {
                double u0 = Math.PI * 2.0 * lon / slices;
                double u1 = Math.PI * 2.0 * (lon + 1) / slices;
                float x00 = (float) Math.cos(u0) * rad0, z00 = (float) Math.sin(u0) * rad0;
                float x10 = (float) Math.cos(u1) * rad0, z10 = (float) Math.sin(u1) * rad0;
                float x01 = (float) Math.cos(u0) * rad1, z01 = (float) Math.sin(u0) * rad1;
                float x11 = (float) Math.cos(u1) * rad1, z11 = (float) Math.sin(u1) * rad1;
                float n = radius > 0.0001f ? 1f / radius : 0f;
                vertex(buffer, mat, pose, x00, y0, z00, r, g, b, a, 0, 0, x00 * n, y0 * n, z00 * n);
                vertex(buffer, mat, pose, x10, y0, z10, r, g, b, a, 1, 0, x10 * n, y0 * n, z10 * n);
                vertex(buffer, mat, pose, x11, y1, z11, r, g, b, a, 1, 1, x11 * n, y1 * n, z11 * n);
                vertex(buffer, mat, pose, x01, y1, z01, r, g, b, a, 0, 1, x01 * n, y1 * n, z01 * n);
            }
        }
    }

    /** One helical ribbon along local Z. phase is radians. */
    public static void drawHelix(PoseStack.Pose pose, VertexConsumer buffer,
                                 float radius, float z0, float z1, float turns, float phase, float halfWidth, int steps,
                                 float r, float g, float b, float a) {
        Matrix4f mat = pose.pose();
        float px = 0, py = 0, pz = 0, prx = 0, pry = 0;
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            float z = z0 + (z1 - z0) * t;
            float ang = phase + t * turns * (float) (Math.PI * 2.0);
            float cx = (float) Math.cos(ang);
            float sy = (float) Math.sin(ang);
            float x = cx * radius;
            float y = sy * radius;
            float rx = cx * halfWidth;
            float ry = sy * halfWidth;
            if (i > 0) {
                float fade = 1f - t * 0.35f;
                vertex(buffer, mat, pose, px - prx, py - pry, pz, r, g, b, a * fade, 0, 0, cx, sy, 0);
                vertex(buffer, mat, pose, px + prx, py + pry, pz, r, g, b, a * fade, 1, 0, cx, sy, 0);
                vertex(buffer, mat, pose, x + rx, y + ry, z, r, g, b, a * fade, 1, 1, cx, sy, 0);
                vertex(buffer, mat, pose, x - rx, y - ry, z, r, g, b, a * fade, 0, 1, cx, sy, 0);
            }
            px = x;
            py = y;
            pz = z;
            prx = rx;
            pry = ry;
        }
    }
}
