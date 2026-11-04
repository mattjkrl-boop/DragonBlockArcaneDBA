package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.BoltEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Branched lightning for BoltEntity. The whole bolt, trunk plus forks, is a
 * pure function of the synced seed, so every client draws the same strike.
 * Two passes per segment: a wide theme colored sheath and a thin near white
 * core, each drawn as crossed ribbons, alpha driven by the shared envelope
 * times a per tick flicker that never repeats the same frame twice.
 */
public class BoltRenderer extends EntityRenderer<BoltEntity, BoltRenderer.BoltState> {

    public BoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class BoltState extends EntityRenderState {
        public int seed;
        public float r, g, b;
        public float length;
        public float age;
        public int life;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(BoltEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(BoltEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public BoltState createRenderState() {
        return new BoltState();
    }

    @Override
    public void extractRenderState(BoltEntity entity, BoltState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.seed = entity.getSeed();
        int color = entity.getColor();
        state.r = KiRenderHelper.red(color);
        state.g = KiRenderHelper.green(color);
        state.b = KiRenderHelper.blue(color);
        state.length = entity.getLength();
        state.age = entity.tickCount + partialTick;
        state.life = entity.getLife();
    }

    private record Seg(float x0, float y0, float z0, float x1, float y1, float z1, float taper) {}

    @Override
    public void submit(BoltState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float env = FxEnvelope.envelope(state.age, Math.max(1, state.life), 0.12f, 0.45f);
        if (env <= 0.01f) return;
        // Flicker: reseeded per whole tick from the bolt seed.
        Random flicker = new Random(state.seed * 31L + (int) state.age);
        float fl = 0.65f + flicker.nextFloat() * 0.35f;
        float alpha = env * fl;

        // Build the bolt geometry from the seed: trunk points walk upward with
        // a jittered random walk; forks leave the trunk and dive outward.
        Random rand = new Random(state.seed);
        int steps = 12;
        float[] tx = new float[steps + 1];
        float[] ty = new float[steps + 1];
        float[] tz = new float[steps + 1];
        float px = 0f, pz = 0f;
        for (int i = 0; i <= steps; i++) {
            tx[i] = px; ty[i] = state.length * i / steps; tz[i] = pz;
            px += (rand.nextFloat() - 0.5f) * 0.55f;
            pz += (rand.nextFloat() - 0.5f) * 0.55f;
        }
        tx[steps] = px; tz[steps] = pz;
        List<Seg> segs = new ArrayList<>();
        for (int i = 0; i < steps; i++) {
            segs.add(new Seg(tx[i], ty[i], tz[i], tx[i + 1], ty[i + 1], tz[i + 1], 1f - i / (float) steps * 0.4f));
        }
        int forks = 2 + rand.nextInt(3);
        for (int f = 0; f < forks; f++) {
            int start = 3 + rand.nextInt(Math.max(1, steps - 5));
            float bx = tx[start], by = ty[start], bz = tz[start];
            float dirX = (rand.nextFloat() - 0.5f) * 2f;
            float dirZ = (rand.nextFloat() - 0.5f) * 2f;
            int blen = 3 + rand.nextInt(3);
            float drop = state.length / steps * 0.9f;
            for (int i = 0; i < blen; i++) {
                float nx = bx + dirX * (0.5f + rand.nextFloat() * 0.5f);
                float nz = bz + dirZ * (0.5f + rand.nextFloat() * 0.5f);
                float ny = by - drop * (0.7f + rand.nextFloat() * 0.5f);
                if (ny < 0.2f) break;
                segs.add(new Seg(bx, by, bz, nx, ny, nz, 0.55f * (1f - i / (float) blen)));
                bx = nx; by = ny; bz = nz;
            }
        }

        RenderType type = KiRenderHelper.kiRenderType();
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            // Sheath pass, theme color.
            for (Seg s : segs) {
                ribbon(matrix, buffer, pose, s, 0.30f * s.taper(), state.r, state.g, state.b, alpha * 0.32f);
            }
            // Core pass, near white.
            for (Seg s : segs) {
                float cr = state.r + (1f - state.r) * 0.75f;
                float cg = state.g + (1f - state.g) * 0.75f;
                float cb = state.b + (1f - state.b) * 0.75f;
                ribbon(matrix, buffer, pose, s, 0.12f * s.taper(), cr, cg, cb, alpha * 0.9f);
            }
            // Ground glow at the strike point.
            disc(matrix, buffer, pose, 1.6f, state.r, state.g, state.b, alpha * 0.4f);
            disc(matrix, buffer, pose, 0.7f, 1f, 1f, 1f, alpha * 0.5f);
        });
        super.submit(state, poseStack, collector, cameraState);
    }

    /** One segment as two crossed ribbons so the bolt reads from every angle. */
    private static void ribbon(Matrix4f matrix, VertexConsumer buffer, PoseStack.Pose pose,
                               Seg s, float halfWidth, float r, float g, float b, float a) {
        // Ribbon facing Z (width along X) and ribbon facing X (width along Z).
        quad(matrix, buffer, pose,
            s.x0() - halfWidth, s.y0(), s.z0(), s.x0() + halfWidth, s.y0(), s.z0(),
            s.x1() + halfWidth, s.y1(), s.z1(), s.x1() - halfWidth, s.y1(), s.z1(), r, g, b, a);
        quad(matrix, buffer, pose,
            s.x0(), s.y0(), s.z0() - halfWidth, s.x0(), s.y0(), s.z0() + halfWidth,
            s.x1(), s.y1(), s.z1() + halfWidth, s.x1(), s.y1(), s.z1() - halfWidth, r, g, b, a);
    }

    private static void quad(Matrix4f matrix, VertexConsumer buffer, PoseStack.Pose pose,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             float r, float g, float b, float a) {
        KiRenderHelper.vertex(buffer, matrix, pose, x0, y0, z0, r, g, b, a, 0, 0, 0, 1, 0);
        KiRenderHelper.vertex(buffer, matrix, pose, x1, y1, z1, r, g, b, a, 1, 0, 0, 1, 0);
        KiRenderHelper.vertex(buffer, matrix, pose, x2, y2, z2, r, g, b, a, 1, 1, 0, 1, 0);
        KiRenderHelper.vertex(buffer, matrix, pose, x3, y3, z3, r, g, b, a, 0, 1, 0, 1, 0);
        KiRenderHelper.vertex(buffer, matrix, pose, x3, y3, z3, r, g, b, a, 0, 1, 0, -1, 0);
        KiRenderHelper.vertex(buffer, matrix, pose, x2, y2, z2, r, g, b, a, 1, 1, 0, -1, 0);
        KiRenderHelper.vertex(buffer, matrix, pose, x1, y1, z1, r, g, b, a, 1, 0, 0, -1, 0);
        KiRenderHelper.vertex(buffer, matrix, pose, x0, y0, z0, r, g, b, a, 0, 0, 0, -1, 0);
    }

    /** Flat glowing disc in the XZ plane at local y = 0.05. */
    private static void disc(Matrix4f matrix, VertexConsumer buffer, PoseStack.Pose pose,
                             float radius, float r, float g, float b, float a) {
        int segments = 20;
        for (int i = 0; i < segments; i++) {
            double a0 = (i / (double) segments) * Math.PI * 2.0;
            double a1 = ((i + 1) / (double) segments) * Math.PI * 2.0;
            float x0 = (float) Math.cos(a0) * radius, z0 = (float) Math.sin(a0) * radius;
            float x1 = (float) Math.cos(a1) * radius, z1 = (float) Math.sin(a1) * radius;
            KiRenderHelper.vertex(buffer, matrix, pose, 0, 0.05f, 0, r, g, b, a, 0.5f, 0.5f, 0, 1, 0);
            KiRenderHelper.vertex(buffer, matrix, pose, x0, 0.05f, z0, r, g, b, a * 0.15f, 0, 0, 0, 1, 0);
            KiRenderHelper.vertex(buffer, matrix, pose, x1, 0.05f, z1, r, g, b, a * 0.15f, 1, 0, 0, 1, 0);
            KiRenderHelper.vertex(buffer, matrix, pose, x1, 0.05f, z1, r, g, b, a * 0.15f, 1, 0, 0, -1, 0);
            KiRenderHelper.vertex(buffer, matrix, pose, x0, 0.05f, z0, r, g, b, a * 0.15f, 0, 0, 0, -1, 0);
            KiRenderHelper.vertex(buffer, matrix, pose, 0, 0.05f, 0, r, g, b, a, 0.5f, 0.5f, 0, -1, 0);
        }
    }
}
