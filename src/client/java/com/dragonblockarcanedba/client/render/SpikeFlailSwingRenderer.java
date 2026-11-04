package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.SpikeFlailSwingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Chain and spiked ball at the end of the swing. The entity sits on the ball. */
public class SpikeFlailSwingRenderer extends EntityRenderer<SpikeFlailSwingEntity, SpikeFlailSwingRenderer.SwingState> {
    public SpikeFlailSwingRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SwingState extends EntityRenderState {
        public float age;
        public float handX;
        public float handY;
        public float handZ;
        public boolean hasHand;
    }

    public boolean shouldRender(SpikeFlailSwingEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(SpikeFlailSwingEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public SwingState createRenderState() {
        return new SwingState();
    }

    @Override
    public void extractRenderState(SpikeFlailSwingEntity entity, SwingState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.age = entity.tickCount + partialTick;
        Entity owner = entity.getOwner();
        state.hasHand = owner != null;
        if (owner != null) {
            Vec3 ball = entity.getPosition(partialTick);
            float yaw = entity.swingYaw();
            double yawRad = Math.toRadians(yaw);
            Vec3 hand = owner.getPosition(partialTick).add(-Math.sin(yawRad) * 0.35, 1.05, Math.cos(yawRad) * 0.35);
            Vec3 rel = hand.subtract(ball);
            state.handX = (float) rel.x;
            state.handY = (float) rel.y;
            state.handZ = (float) rel.z;
        }
    }

    @Override
    public void submit(SwingState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float fade = ease(Math.max(0.0f, 1.0f - Math.max(0.0f, state.age - 8.0f) / 3.0f));
        float open = ease(Math.min(1.0f, state.age / 2.0f));
        RenderType type = KiRenderHelper.kiRenderType();
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            if (state.hasHand) {
                drawChain(buffer, matrix, pose, state.handX, state.handY, state.handZ, 0.0f, 0.0f, 0.0f, fade);
            }
            drawBall(pose, buffer, 0.42f * (0.75f + 0.25f * open), fade);
        });
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float ease(float t) {
        float c = Mth.clamp(t, 0.0f, 1.0f);
        return c * c * (3.0f - 2.0f * c);
    }

    static void drawChain(VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose,
                          float hx, float hy, float hz, float bx, float by, float bz, float fade) {
        int links = 8;
        for (int i = 0; i < links; i++) {
            float t0 = i / (float) links;
            float t1 = (i + 1) / (float) links;
            float sag0 = (float) Math.sin(t0 * Math.PI) * 0.45f;
            float sag1 = (float) Math.sin(t1 * Math.PI) * 0.45f;
            float x0 = Mth.lerp(t0, hx, bx);
            float y0 = Mth.lerp(t0, hy, by) - sag0;
            float z0 = Mth.lerp(t0, hz, bz);
            float x1 = Mth.lerp(t1, hx, bx);
            float y1 = Mth.lerp(t1, hy, by) - sag1;
            float z1 = Mth.lerp(t1, hz, bz);
            float radius = i % 2 == 0 ? 0.07f : 0.1f;
            prism(buffer, matrix, pose, x0, y0, z0, x1, y1, z1, radius, 0.72f, 0.74f, 0.78f, fade);
        }
    }

    static void drawBall(PoseStack.Pose pose, VertexConsumer buffer, float radius, float fade) {
        KiRenderHelper.drawSphere(pose, buffer, radius, 5, 8, 0.78f, 0.8f, 0.84f, fade);
        KiRenderHelper.drawSphere(pose, buffer, radius * 0.45f, 4, 6, 0.95f, 0.96f, 1.0f, fade);
        cone(buffer, pose.pose(), pose, 0, radius, 0, 0, radius + 0.34f, 0, 0.11f, fade);
        cone(buffer, pose.pose(), pose, 0, -radius, 0, 0, -radius - 0.3f, 0, 0.1f, fade);
        cone(buffer, pose.pose(), pose, radius, 0, 0, radius + 0.32f, 0, 0, 0.1f, fade);
        cone(buffer, pose.pose(), pose, -radius, 0, 0, -radius - 0.32f, 0, 0, 0.1f, fade);
        cone(buffer, pose.pose(), pose, 0, 0, radius, 0, 0, radius + 0.32f, 0.1f, fade);
        cone(buffer, pose.pose(), pose, 0, 0, -radius, 0, 0, -radius - 0.32f, 0.1f, fade);
    }

    static void prism(VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose,
                      float x0, float y0, float z0, float x1, float y1, float z1,
                      float radius, float r, float g, float b, float a) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.0001f) {
            return;
        }
        float ix = dx / len;
        float iy = dy / len;
        float iz = dz / len;
        float ux = Math.abs(iy) < 0.85f ? 0.0f : 1.0f;
        float uy = Math.abs(iy) < 0.85f ? 1.0f : 0.0f;
        float uz = 0.0f;
        float cx = iy * uz - iz * uy;
        float cy = iz * ux - ix * uz;
        float cz = ix * uy - iy * ux;
        float cl = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
        cx /= cl;
        cy /= cl;
        cz /= cl;
        float bx = iy * cz - iz * cy;
        float by = iz * cx - ix * cz;
        float bz = ix * cy - iy * cx;
        int sides = 6;
        for (int i = 0; i < sides; i++) {
            double a0 = Math.PI * 2.0 * i / sides;
            double a1 = Math.PI * 2.0 * (i + 1) / sides;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            float p0x = x0 + (cx * c0 + bx * s0) * radius;
            float p0y = y0 + (cy * c0 + by * s0) * radius;
            float p0z = z0 + (cz * c0 + bz * s0) * radius;
            float p1x = x0 + (cx * c1 + bx * s1) * radius;
            float p1y = y0 + (cy * c1 + by * s1) * radius;
            float p1z = z0 + (cz * c1 + bz * s1) * radius;
            float q0x = x1 + (cx * c0 + bx * s0) * radius;
            float q0y = y1 + (cy * c0 + by * s0) * radius;
            float q0z = z1 + (cz * c0 + bz * s0) * radius;
            float q1x = x1 + (cx * c1 + bx * s1) * radius;
            float q1y = y1 + (cy * c1 + by * s1) * radius;
            float q1z = z1 + (cz * c1 + bz * s1) * radius;
            float nx = cx * (c0 + c1) * 0.5f + bx * (s0 + s1) * 0.5f;
            float ny = cy * (c0 + c1) * 0.5f + by * (s0 + s1) * 0.5f;
            float nz = cz * (c0 + c1) * 0.5f + bz * (s0 + s1) * 0.5f;
            KiRenderHelper.vertex(buffer, matrix, pose, p0x, p0y, p0z, r, g, b, a, 0, 0, nx, ny, nz);
            KiRenderHelper.vertex(buffer, matrix, pose, p1x, p1y, p1z, r, g, b, a, 1, 0, nx, ny, nz);
            KiRenderHelper.vertex(buffer, matrix, pose, q1x, q1y, q1z, r, g, b, a, 1, 1, nx, ny, nz);
            KiRenderHelper.vertex(buffer, matrix, pose, q0x, q0y, q0z, r, g, b, a, 0, 1, nx, ny, nz);
        }
    }

    static void cone(VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose,
                     float x0, float y0, float z0, float x1, float y1, float z1, float radius, float fade) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.0001f) {
            return;
        }
        float ix = dx / len;
        float iy = dy / len;
        float iz = dz / len;
        float ux = Math.abs(iy) < 0.85f ? 0.0f : 1.0f;
        float uy = Math.abs(iy) < 0.85f ? 1.0f : 0.0f;
        float cx = iy * 0 - iz * uy;
        float cy = iz * ux - ix * 0;
        float cz = ix * uy - iy * ux;
        float cl = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
        cx /= cl;
        cy /= cl;
        cz /= cl;
        float bx = iy * cz - iz * cy;
        float by = iz * cx - ix * cz;
        float bz = ix * cy - iy * cx;
        int sides = 5;
        for (int i = 0; i < sides; i++) {
            double a0 = Math.PI * 2.0 * i / sides;
            double a1 = Math.PI * 2.0 * (i + 1) / sides;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            float p0x = x0 + (cx * c0 + bx * s0) * radius;
            float p0y = y0 + (cy * c0 + by * s0) * radius;
            float p0z = z0 + (cz * c0 + bz * s0) * radius;
            float p1x = x0 + (cx * c1 + bx * s1) * radius;
            float p1y = y0 + (cy * c1 + by * s1) * radius;
            float p1z = z0 + (cz * c1 + bz * s1) * radius;
            KiRenderHelper.vertex(buffer, matrix, pose, p0x, p0y, p0z, 0.9f, 0.91f, 0.94f, fade, 0, 0, ix, iy, iz);
            KiRenderHelper.vertex(buffer, matrix, pose, p1x, p1y, p1z, 0.9f, 0.91f, 0.94f, fade, 1, 0, ix, iy, iz);
            KiRenderHelper.vertex(buffer, matrix, pose, x1, y1, z1, 1.0f, 0.97f, 0.9f, fade, 0.5f, 1, ix, iy, iz);
            KiRenderHelper.vertex(buffer, matrix, pose, x1, y1, z1, 1.0f, 0.97f, 0.9f, fade * 0.2f, 0.5f, 1, ix, iy, iz);
        }
    }
}
