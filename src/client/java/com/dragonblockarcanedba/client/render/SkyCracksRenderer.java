package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.SkyCracksEntity;
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

import java.util.Random;

/**
 * The torn sky above the Cursed Tempest. This used to draw nothing at all;
 * it now renders a branching crack network in the local XY plane,
 * billboarded to the camera the way TimeShatter's dial layer is. Seven main
 * fractures radiate from the tear's heart along jagged seeded paths, violet
 * forks branch off them, a white hot core runs down every main crack, and a
 * dark violet disc behind the network reads as the shadow of the tear.
 * Alpha runs the shared lifecycle envelope over the entity's 100 tick life
 * times a slow electrical flicker, so the tear opens, burns, and seals shut
 * instead of popping in and vanishing.
 */
public class SkyCracksRenderer extends EntityRenderer<SkyCracksEntity, SkyCracksRenderer.SkyCracksRenderState> {

    public SkyCracksRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SkyCracksRenderState extends EntityRenderState {
        public float age = 0;
        public int seed = 0;
    }

    @Override
    public SkyCracksRenderState createRenderState() {
        return new SkyCracksRenderState();
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(SkyCracksEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(SkyCracksEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public void extractRenderState(SkyCracksEntity entity, SkyCracksRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getSeed();
    }

    @Override
    public void submit(SkyCracksRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float env = FxEnvelope.envelope(state.age, SkyCracksEntity.LIFE_TICKS, 0.15f, 0.35f);
        if (env <= 0.01f) return;
        float ds = LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        float flicker = 0.78f + 0.22f * (float) Math.sin(state.age * 2.1f + state.seed * 0.37f);
        float alpha = env * flicker;
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        if (cameraState.orientation != null) {
            poseStack.mulPose(new org.joml.Matrix4f().rotation(new org.joml.Quaternionf(cameraState.orientation).conjugate()));
        }

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // Shadow of the tear: a dark disc the cracks burn through.
            drawDiscFan(matrix, buffer, 24.0f, 0.10f, 0.01f, 0.16f, 0.30f * alpha, ds);

            Random rng = new Random(state.seed);
            int mains = 7;
            for (int m = 0; m < mains; m++) {
                double ang = (m / (double) mains) * Math.PI * 2.0 + rng.nextDouble() * 0.5;
                float cx = 0.0f, cy = 0.0f;
                int segs = 5 + rng.nextInt(3);
                float step = (15.0f + rng.nextFloat() * 10.0f) / segs;
                for (int s = 0; s < segs; s++) {
                    float t = (s + 1) / (float) segs;
                    double a = ang + (rng.nextDouble() - 0.5) * 0.55;
                    float nx = cx + (float) Math.cos(a) * step;
                    float ny = cy + (float) Math.sin(a) * step;
                    float w = 0.34f * (1.0f - t * 0.8f);
                    // Crimson sheath, then the white hot core inside it.
                    ribbon(matrix, buffer, cx, cy, nx, ny, w, 0.95f, 0.05f, 0.20f, alpha);
                    ribbon(matrix, buffer, cx, cy, nx, ny, w * 0.38f, 1.0f, 0.95f, 0.97f, alpha);

                    // Violet branch forks off the main fracture.
                    if (s >= 1 && rng.nextFloat() < 0.55f) {
                        double ba = a + (rng.nextBoolean() ? 0.7 : -0.7);
                        float bx = nx, by = ny;
                        int bsegs = 2 + rng.nextInt(2);
                        for (int b = 0; b < bsegs; b++) {
                            float bt = (b + 1) / (float) bsegs;
                            double bba = ba + (rng.nextDouble() - 0.5) * 0.5;
                            float bnx = bx + (float) Math.cos(bba) * step * 0.55f;
                            float bny = by + (float) Math.sin(bba) * step * 0.55f;
                            ribbon(matrix, buffer, bx, by, bnx, bny, 0.16f * (1.0f - bt * 0.7f), 0.55f, 0.10f, 0.95f, alpha * 0.85f);
                            bx = bnx; by = bny;
                        }
                    }
                    cx = nx; cy = ny;
                }
            }
        });

        poseStack.popPose();
    }

    /** One crack segment in the XY plane: a tapered quad ribbon, drawn both ways. */
    private static void ribbon(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float x2, float y2,
                               float width, float r, float g, float b, float a) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001f || a < 0.02f) return;
        float nx = -dy / len * width * 0.5f;
        float ny = dx / len * width * 0.5f;
        v(consumer, matrix, x1 - nx, y1 - ny, r, g, b, a, 1);
        v(consumer, matrix, x1 + nx, y1 + ny, r, g, b, a, 1);
        v(consumer, matrix, x2 + nx, y2 + ny, r, g, b, a, 1);
        v(consumer, matrix, x2 - nx, y2 - ny, r, g, b, a, 1);
        v(consumer, matrix, x2 - nx, y2 - ny, r, g, b, a, -1);
        v(consumer, matrix, x2 + nx, y2 + ny, r, g, b, a, -1);
        v(consumer, matrix, x1 + nx, y1 + ny, r, g, b, a, -1);
        v(consumer, matrix, x1 - nx, y1 - ny, r, g, b, a, -1);
    }

    /** A soft disc in the XY plane: full alpha at the heart, gone at the rim. */
    private static void drawDiscFan(Matrix4f matrix, VertexConsumer consumer, float radius,
                                    float r, float g, float b, float a, float distScale) {
        if (a < 0.01f) return;
        int segments = FxEnvelope.segments(28, distScale, 10);
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;
            float x1 = (float) Math.cos(a1) * radius, y1 = (float) Math.sin(a1) * radius;
            float x2 = (float) Math.cos(a2) * radius, y2 = (float) Math.sin(a2) * radius;
            float rim = a * 0.05f;
            // Degenerate quads (heart vertex doubled), the same trick the
            // codebase's shard pyramids use for triangles.
            v(consumer, matrix, 0, 0, r, g, b, a, 1);
            v(consumer, matrix, 0, 0, r, g, b, a, 1);
            v(consumer, matrix, x2, y2, r, g, b, rim, 1);
            v(consumer, matrix, x1, y1, r, g, b, rim, 1);
            v(consumer, matrix, 0, 0, r, g, b, a, -1);
            v(consumer, matrix, 0, 0, r, g, b, a, -1);
            v(consumer, matrix, x1, y1, r, g, b, rim, -1);
            v(consumer, matrix, x2, y2, r, g, b, rim, -1);
        }
    }

    private static void v(VertexConsumer consumer, Matrix4f matrix, float x, float y,
                          float r, float g, float b, float a, float nz) {
        consumer.addVertex(matrix, x, y, 0).setColor(r, g, b, a).setUv(0.5f, 0.5f)
            .setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 0, nz);
    }
}
