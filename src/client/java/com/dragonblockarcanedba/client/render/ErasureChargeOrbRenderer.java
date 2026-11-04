package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.ErasureChargeOrbEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;

/**
 * Entity Renderer for Erasure Cannon Muzzle Charge Orb in MC 26.2.
 * Renders an expanding physical 3D geometric energy orb model:
 * - Blinding white-cyan octahedron energy singularity core (rotates for real)
 * - Expanding outer geodesic energy cage with counter-rotating polyhedral shells
 *   plus a faint wispy outer shell
 * - 3 synchrotron accelerator rings in three planes (muzzle, horizontal, side)
 * - 8 inward-converging 3D plasma intake spikes spiraling into the singularity
 * (Header corrected in the spectacle pass: it used to also promise helical
 * containment ribbons and corona discharge arcs that were never drawn.)
 */
public class ErasureChargeOrbRenderer extends EntityRenderer<ErasureChargeOrbEntity, ErasureChargeOrbRenderer.ErasureChargeRenderState> {

    public ErasureChargeOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ErasureChargeRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public float yRot = 0.0f;
        public float xRot = 0.0f;
        public float age = 0.0f;
        public boolean isFirstPersonOwner = false;
        public boolean onRight = true;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(ErasureChargeOrbEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(ErasureChargeOrbEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ErasureChargeRenderState createRenderState() {
        return new ErasureChargeRenderState();
    }

    @Override
    public void extractRenderState(ErasureChargeOrbEntity entity, ErasureChargeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null && 
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) && 
            mc.options.getCameraType().isFirstPerson());
        if (mc.player != null) {
            boolean isRightHanded = (mc.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            boolean isOffhand = (mc.player.getOffhandItem().getItem() instanceof com.dragonblockarcanedba.item.BlasterGunItem && 
                !(mc.player.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.BlasterGunItem));
            state.onRight = isRightHanded ? !isOffhand : isOffhand;
        }
    }

    @Override
    public void submit(ErasureChargeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float charge = state.chargeRatio;
        float age = state.age;
        float pulse = 0.92f + 0.08f * (float) Math.sin(age * 0.65f);

        // Scaled down smaller in first person
        float chargeEase = charge * charge * (3.0f - 2.0f * charge);
        float orbRadius = (0.22f + chargeEase * 0.95f) * pulse * (state.isFirstPersonOwner ? 0.45f : 1.0f);
        float alpha = 0.10f + chargeEase * 0.90f;
        float hot = charge <= 0.75f ? 0.0f : (charge - 0.75f) / 0.25f;

        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        if (state.isFirstPersonOwner) {
            // Anchor orb realistically to the Blaster Gun muzzle: way lower and on the weapon hand side
            float sideSign = state.onRight ? -1.0f : 1.0f;
            poseStack.translate(sideSign * 0.42f, -0.48f, -0.60f);
        }

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // White core, one cyan shell that whitens as the charge peaks, one muzzle ring as the edge.
            float coreRadius = orbRadius * 0.38f;
            drawRotatingOctahedron(matrix, buffer, coreRadius, age * 36.0f, age * 22.0f, 1.0f, 1.0f, 1.0f, 0.98f * (0.25f + chargeEase * 0.75f));

            float shellR = hot * 0.85f;
            float shellG = 0.95f;
            float shellB = 1.0f;
            drawGeodesicSphere(matrix, buffer, orbRadius * 0.82f, age * -18.0f, 16, shellR, shellG, shellB, alpha * 0.72f);
            drawGeodesicSphere(matrix, buffer, orbRadius * 0.48f, age * 26.0f, 12, 0.85f + hot * 0.15f, 0.96f, 1.0f, alpha * (0.40f + hot * 0.45f));

            // Outer wispy layer (standard point 1): a larger, faint shell at
            // the standard outer band alpha so the orb's edge dissolves
            // instead of ending at the mid shell.
            drawGeodesicSphere(matrix, buffer, orbRadius * 1.18f, age * 9.0f, 12, 0.35f, 0.90f, 1.0f, alpha * 0.18f);

            // Three accelerator rings in three planes, gyroscope style. The
            // header has always promised 3 concentric rings; submit() used
            // to draw only the muzzle-axis one below.
            float ringR = orbRadius * (1.48f - chargeEase * 0.28f);
            drawPlaneRing(matrix, buffer, 0, 0, 0, ringR, ringR * 0.86f, 28, age * 22.0f,
                hot, 0.90f, 1.0f, alpha * 0.90f);
            drawAxisRing(matrix, buffer, ringR * 0.94f, ringR * 0.94f * 0.86f, 24, age * -16.0f,
                1, 0, 0, 0, 0, 1, hot * 0.6f, 0.85f, 1.0f, alpha * 0.60f);
            drawAxisRing(matrix, buffer, ringR * 0.88f, ringR * 0.88f * 0.86f, 24, age * 27.0f,
                0, 1, 0, 0, 0, 1, hot * 0.6f, 0.85f, 1.0f, alpha * 0.50f);

            // Intake spikes spiral inward as the charge eases up. They are the follow-through, not a second orb.
            int spikeCount = 8;
            float intakeRadius = orbRadius * (2.3f - chargeEase * 0.95f);
            float intakeSpeed = 30.0f + (charge * 60.0f);

            for (int s = 0; s < spikeCount; s++) {
                double angle = (s / (double) spikeCount) * Math.PI * 2.0 + Math.toRadians(age * intakeSpeed);
                float sx = (float) Math.cos(angle) * intakeRadius;
                float sy = (float) Math.sin(angle) * intakeRadius;
                float sz = (float) Math.sin(age * 0.4f + s * 1.2f) * (orbRadius * 0.6f);

                float toCenterX = -sx;
                float toCenterY = -sy;
                float toCenterZ = -sz;
                float len = (float) Math.sqrt(toCenterX * toCenterX + toCenterY * toCenterY + toCenterZ * toCenterZ);
                if (len > 0.001f) {
                    toCenterX /= len;
                    toCenterY /= len;
                    toCenterZ /= len;
                }

                float spikeLen = orbRadius * 0.7f;
                float tipX = sx + toCenterX * spikeLen;
                float tipY = sy + toCenterY * spikeLen;
                float tipZ = sz + toCenterZ * spikeLen;
                float bow = orbRadius * 0.16f;
                drawLash(matrix, buffer,
                    sx, sy, sz,
                    sx * 0.55f + (float) Math.cos(angle + 1.2) * bow, sy * 0.55f + (float) Math.sin(angle + 1.2) * bow, sz * 0.35f,
                    tipX, tipY, tipZ,
                    0.045f + charge * 0.03f, 5,
                    0.0f, 0.72f, 1.0f,
                    0.90f, 0.98f, 1.0f,
                    alpha * 0.92f
                );
            }

        });

        poseStack.popPose();
    }

    // Rotates one vertex: yaw around Y first, then pitch around X.
    private static float[] rotVertex(float x, float y, float z, double cosYaw, double sinYaw, double cosPitch, double sinPitch) {
        float x1 = (float) (x * cosYaw + z * sinYaw);
        float z1 = (float) (-x * sinYaw + z * cosYaw);
        float y2 = (float) (y * cosPitch - z1 * sinPitch);
        float z2 = (float) (y * sinPitch + z1 * cosPitch);
        return new float[]{x1, y2, z2};
    }

    private static void drawRotatingOctahedron(Matrix4f matrix, VertexConsumer consumer, float r, float yawDeg, float pitchDeg, float red, float green, float blue, float alpha) {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        double cosYaw = Math.cos(yaw), sinYaw = Math.sin(yaw);
        double cosPitch = Math.cos(pitch), sinPitch = Math.sin(pitch);

        // 6 Vertices of an octahedron, actually rotated by yaw and pitch.
        // Bug fix: this method used to convert both angles to radians and
        // then never use them, so the "rotating" core was drawn unrotated
        // while the shells and ring spun around it.
        float[] top = rotVertex(0, r, 0, cosYaw, sinYaw, cosPitch, sinPitch);
        float[] bot = rotVertex(0, -r, 0, cosYaw, sinYaw, cosPitch, sinPitch);
        float[] p1 = rotVertex(r, 0, 0, cosYaw, sinYaw, cosPitch, sinPitch);
        float[] p2 = rotVertex(0, 0, r, cosYaw, sinYaw, cosPitch, sinPitch);
        float[] p3 = rotVertex(-r, 0, 0, cosYaw, sinYaw, cosPitch, sinPitch);
        float[] p4 = rotVertex(0, 0, -r, cosYaw, sinYaw, cosPitch, sinPitch);

        float topX = top[0], topY = top[1], topZ = top[2];
        float botX = bot[0], botY = bot[1], botZ = bot[2];
        float p1X = p1[0], p1Y = p1[1], p1Z = p1[2];
        float p2X = p2[0], p2Y = p2[1], p2Z = p2[2];
        float p3X = p3[0], p3Y = p3[1], p3Z = p3[2];
        float p4X = p4[0], p4Y = p4[1], p4Z = p4[2];

        // Draw 8 triangular faces
        drawTriangle(matrix, consumer, topX, topY, topZ, p1X, p1Y, p1Z, p2X, p2Y, p2Z, red, green, blue, alpha);
        drawTriangle(matrix, consumer, topX, topY, topZ, p2X, p2Y, p2Z, p3X, p3Y, p3Z, red, green, blue, alpha);
        drawTriangle(matrix, consumer, topX, topY, topZ, p3X, p3Y, p3Z, p4X, p4Y, p4Z, red, green, blue, alpha);
        drawTriangle(matrix, consumer, topX, topY, topZ, p4X, p4Y, p4Z, p1X, p1Y, p1Z, red, green, blue, alpha);

        drawTriangle(matrix, consumer, botX, botY, botZ, p2X, p2Y, p2Z, p1X, p1Y, p1Z, red, green, blue, alpha);
        drawTriangle(matrix, consumer, botX, botY, botZ, p3X, p3Y, p3Z, p2X, p2Y, p2Z, red, green, blue, alpha);
        drawTriangle(matrix, consumer, botX, botY, botZ, p4X, p4Y, p4Z, p3X, p3Y, p3Z, red, green, blue, alpha);
        drawTriangle(matrix, consumer, botX, botY, botZ, p1X, p1Y, p1Z, p4X, p4Y, p4Z, red, green, blue, alpha);
    }

    private static void drawGeodesicSphere(Matrix4f matrix, VertexConsumer consumer, float radius, float rotDeg, int segments, float r, float g, float b, float a) {
        double rotRad = Math.toRadians(rotDeg);
        int rings = 8;
        for (int i = 0; i < rings; i++) {
            double v1 = (i / (double) rings) * Math.PI - Math.PI / 2.0;
            double v2 = ((i + 1) / (double) rings) * Math.PI - Math.PI / 2.0;

            float y1 = (float) Math.sin(v1) * radius;
            float r1 = (float) Math.cos(v1) * radius;
            float y2 = (float) Math.sin(v2) * radius;
            float r2 = (float) Math.cos(v2) * radius;

            // 3-stop latitude gradient (T2): the shells used to be one flat
            // color per shell. Each band now shades from a deep shade of the
            // shell hue at the bottom pole, through the shell color at the
            // equator, to a white-hot tint at the top pole.
            float bandT = (i + 0.5f) / (float) rings;
            float rr, gg, bb;
            if (bandT < 0.5f) {
                float k = bandT / 0.5f;
                rr = r * (0.50f + 0.50f * k);
                gg = g * (0.50f + 0.50f * k);
                bb = b * (0.50f + 0.50f * k);
            } else {
                float k = (bandT - 0.5f) / 0.5f;
                rr = r + (1.0f - r) * 0.55f * k;
                gg = g + (1.0f - g) * 0.55f * k;
                bb = b + (1.0f - b) * 0.55f * k;
            }

            for (int j = 0; j < segments; j++) {
                double u1 = (j / (double) segments) * Math.PI * 2.0 + rotRad;
                double u2 = ((j + 1) / (double) segments) * Math.PI * 2.0 + rotRad;

                float x1 = (float) Math.cos(u1) * r1;
                float z1 = (float) Math.sin(u1) * r1;
                float x2 = (float) Math.cos(u2) * r1;
                float z2 = (float) Math.sin(u2) * r1;

                float x3 = (float) Math.cos(u2) * r2;
                float z3 = (float) Math.sin(u2) * r2;
                float x4 = (float) Math.cos(u1) * r2;
                float z4 = (float) Math.sin(u1) * r2;

                drawQuad(matrix, consumer, x1, y1, z1, x2, y1, z2, x3, y2, z3, x4, y2, z4, rr, gg, bb, a);
            }
        }
    }

    private static void drawPlaneRing(Matrix4f matrix, VertexConsumer consumer, float cx, float cy, float cz, float rOuter, float rInner, int segments, float rotDeg, float r, float g, float b, float a) {
        double rotRad = Math.toRadians(rotDeg);
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0 + rotRad;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0 + rotRad;

            float x1Out = cx + (float) Math.cos(a1) * rOuter;
            float y1Out = cy + (float) Math.sin(a1) * rOuter;
            float x2Out = cx + (float) Math.cos(a2) * rOuter;
            float y2Out = cy + (float) Math.sin(a2) * rOuter;

            float x1In = cx + (float) Math.cos(a1) * rInner;
            float y1In = cy + (float) Math.sin(a1) * rInner;
            float x2In = cx + (float) Math.cos(a2) * rInner;
            float y2In = cy + (float) Math.sin(a2) * rInner;

            drawQuad(matrix, consumer, x1In, y1In, cz, x2In, y2In, cz, x2Out, y2Out, cz, x1Out, y1Out, cz, r, g, b, a);
        }
    }

    // Ring in the plane spanned by two axis vectors (u, v), so the orb can
    // carry rings in the horizontal and side planes as well as the muzzle
    // plane drawPlaneRing covers.
    private static void drawAxisRing(Matrix4f matrix, VertexConsumer consumer, float rOuter, float rInner, int segments, float rotDeg,
                                     float ux, float uy, float uz, float vx, float vy, float vz,
                                     float r, float g, float b, float a) {
        double rotRad = Math.toRadians(rotDeg);
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0 + rotRad;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0 + rotRad;

            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float c2 = (float) Math.cos(a2), s2 = (float) Math.sin(a2);

            float x1Out = (ux * c1 + vx * s1) * rOuter, y1Out = (uy * c1 + vy * s1) * rOuter, z1Out = (uz * c1 + vz * s1) * rOuter;
            float x2Out = (ux * c2 + vx * s2) * rOuter, y2Out = (uy * c2 + vy * s2) * rOuter, z2Out = (uz * c2 + vz * s2) * rOuter;
            float x1In = (ux * c1 + vx * s1) * rInner, y1In = (uy * c1 + vy * s1) * rInner, z1In = (uz * c1 + vz * s1) * rInner;
            float x2In = (ux * c2 + vx * s2) * rInner, y2In = (uy * c2 + vy * s2) * rInner, z2In = (uz * c2 + vz * s2) * rInner;

            drawQuad(matrix, consumer, x1In, y1In, z1In, x2In, y2In, z2In, x2Out, y2Out, z2Out, x1Out, y1Out, z1Out, r, g, b, a);
        }
    }

    private static void drawTriangle(Matrix4f matrix, VertexConsumer consumer,
                                     float x1, float y1, float z1,
                                     float x2, float y2, float z2,
                                     float x3, float y3, float z3,
                                     float r, float g, float b, float a) {
        drawQuad(matrix, consumer, x1, y1, z1, x2, y2, z2, x3, y3, z3, x1, y1, z1, r, g, b, a);
    }

    private static void drawLash(Matrix4f matrix, VertexConsumer consumer,
                                 float x0, float y0, float z0,
                                 float xc, float yc, float zc,
                                 float x1, float y1, float z1,
                                 float width, int steps,
                                 float er, float eg, float eb,
                                 float cr, float cg, float cb,
                                 float alpha) {
        int n = Math.max(4, steps);
        float px = x0;
        float py = y0;
        float pz = z0;
        for (int i = 1; i <= n; i++) {
            float t = i / (float) n;
            float u = 1.0f - t;
            float x = u * u * x0 + 2.0f * u * t * xc + t * t * x1;
            float y = u * u * y0 + 2.0f * u * t * yc + t * t * y1;
            float z = u * u * z0 + 2.0f * u * t * zc + t * t * z1;
            float w = width * (1.0f - t * 0.9f);
            float a = alpha * (1.0f - t * t);
            lashSpan(matrix, consumer, px, py, pz, x, y, z, w, er, eg, eb, a);
            lashSpan(matrix, consumer, px, py, pz, x, y, z, w * 0.32f, cr, cg, cb, Math.min(1.0f, a * 1.2f));
            px = x;
            py = y;
            pz = z;
        }
    }

    private static void lashSpan(Matrix4f matrix, VertexConsumer consumer,
                                 float x0, float y0, float z0,
                                 float x1, float y1, float z1,
                                 float width,
                                 float r, float g, float b, float a) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float dz = z1 - z0;
        float ax = 0.0f;
        float ay = 1.0f;
        float az = 0.0f;
        if (dx * dx + dz * dz < dy * dy) {
            ax = 1.0f;
            ay = 0.0f;
            az = 0.0f;
        }
        float px = dy * az - dz * ay;
        float py = dz * ax - dx * az;
        float pz = dx * ay - dy * ax;
        float plen = (float) Math.sqrt(px * px + py * py + pz * pz);
        if (plen < 0.0001f) {
            return;
        }
        float s = width / plen;
        px *= s;
        py *= s;
        pz *= s;
        consumer.addVertex(matrix, x0 - px, y0 - py, z0 - pz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(px, py, pz);
        consumer.addVertex(matrix, x0 + px, y0 + py, z0 + pz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(px, py, pz);
        consumer.addVertex(matrix, x1 + px, y1 + py, z1 + pz).setColor(r, g, b, a * 0.65f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(px, py, pz);
        consumer.addVertex(matrix, x1 - px, y1 - py, z1 - pz).setColor(r, g, b, a * 0.65f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(px, py, pz);
        consumer.addVertex(matrix, x1 - px, y1 - py, z1 - pz).setColor(r, g, b, a * 0.65f).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-px, -py, -pz);
        consumer.addVertex(matrix, x1 + px, y1 + py, z1 + pz).setColor(r, g, b, a * 0.65f).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-px, -py, -pz);
        consumer.addVertex(matrix, x0 + px, y0 + py, z0 + pz).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-px, -py, -pz);
        consumer.addVertex(matrix, x0 - px, y0 - py, z0 - pz).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(-px, -py, -pz);
    }

    private static void drawQuad(Matrix4f matrix, VertexConsumer consumer,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float x4, float y4, float z4,
                                 float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

        // Reverse side
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }
}
