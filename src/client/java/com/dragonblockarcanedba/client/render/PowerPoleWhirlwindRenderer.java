package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.PowerPoleWhirlwindEntity;
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
 * Entity Renderer for Power Pole Whirlwind Staff in Minecraft 26.2.
 * Renders a physical 3D conical aerodynamic hurricane vortex:
 * - Staff-origin kinetic jade/gold whirling sweep plane
 * - Multi-tiered fluted conical wind pressure tunnels (cyan / emerald / white)
 * - Quad counter-rotating helical wind ribbons
 * - 10 physical 3D crescent gale blades / kinetic air-cutters orbiting along the cone
 * - Traveling supersonic Mach air compression shock rings
 */
public class PowerPoleWhirlwindRenderer extends EntityRenderer<PowerPoleWhirlwindEntity, PowerPoleWhirlwindRenderer.WhirlwindRenderState> {

    public PowerPoleWhirlwindRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class WhirlwindRenderState extends EntityRenderState {
        public float yRot = 0.0f;
        public float xRot = 0.0f;
        public float range = 25.0f;
        public float coneAngle = 35.0f;
        public float age = 0.0f;
        public int maxLifetime = 8;
        public boolean isFirstPersonOwner = false;
        public boolean onRight = true;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(PowerPoleWhirlwindEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(PowerPoleWhirlwindEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public WhirlwindRenderState createRenderState() {
        return new WhirlwindRenderState();
    }

    @Override
    public void extractRenderState(PowerPoleWhirlwindEntity entity, WhirlwindRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getEntityYaw();
        state.xRot = entity.getEntityPitch();
        state.range = entity.getRange();
        state.coneAngle = entity.getConeAngle();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null && 
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) && 
            mc.options.getCameraType().isFirstPerson());
        if (mc.player != null) {
            boolean isRightHanded = (mc.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            boolean isOffhand = (mc.player.getOffhandItem().getItem() instanceof com.dragonblockarcanedba.item.PowerPoleItem && 
                !(mc.player.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.PowerPoleItem));
            state.onRight = isRightHanded ? !isOffhand : isOffhand;
        }
    }

    @Override
    public void submit(WhirlwindRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float range = state.range;
        if (range < 1.0f) return;

        float age = state.age;
        float maxLife = (float) state.maxLifetime;

        float lifeT = maxLife <= 0.0f ? 1.0f : Math.min(1.0f, age / maxLife);
        float alphaMult;
        if (lifeT < 0.25f) {
            float t = lifeT / 0.25f;
            alphaMult = t * t * (3.0f - 2.0f * t);
        } else if (lifeT > 0.6f) {
            float t = (lifeT - 0.6f) / 0.4f;
            alphaMult = 1.0f - t * t * (3.0f - 2.0f * t);
        } else {
            alphaMult = 1.0f;
        }

        final float finalAlpha = alphaMult;
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        float fpScale = state.isFirstPersonOwner ? 0.55f : 1.0f;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Staff Origin Kinetic Whirling Sweep Disc (Rapid staff spinning plane at base)
            float spinRot = age * 65.0f * (float) (Math.PI / 180.0);
            float discR1 = 2.2f * fpScale;
            drawOriginDisc(matrix, buffer, 0.4f, discR1, 28, spinRot, 0.0f, 1.0f, 0.75f, 0.70f * finalAlpha);

            // 2. Multi-Tiered Conical Aerodynamic Hurricane Vortex Tunnel
            int tiers = 12;
            int segments = 28;
            float coneTan = (float) Math.tan(Math.toRadians(state.coneAngle));

            // Layer A: Dense Inner Wind Sheath (Cyan-White)
            for (int t = 0; t < tiers; t++) {
                float z1 = (t / (float) tiers) * range + 0.3f;
                float z2 = ((t + 1) / (float) tiers) * range + 0.3f;

                float r1 = z1 * coneTan * 0.65f;
                float r2 = z2 * coneTan * 0.65f;

                float tierAlpha = 0.55f * (1.0f - (t / (float) tiers) * 0.45f) * finalAlpha;
                float twist1 = spinRot + (z1 * 0.35f);
                float twist2 = spinRot + (z2 * 0.35f);

                // 3-stop gradient: near-white hot at the staff origin, cyan-white
                // through the middle, deeper cyan at the far mouth.
                float tfA = t / (float) tiers;
                float crA, cgA, cbA;
                if (tfA < 0.5f) {
                    float k = tfA / 0.5f;
                    crA = 0.95f + (0.75f - 0.95f) * k; cgA = 1.0f; cbA = 1.0f;
                } else {
                    float k = (tfA - 0.5f) / 0.5f;
                    crA = 0.75f + (0.40f - 0.75f) * k; cgA = 1.0f + (0.92f - 1.0f) * k; cbA = 1.0f;
                }

                for (int i = 0; i < segments; i++) {
                    double a1 = ((i / (double) segments) * Math.PI * 2.0) + twist1;
                    double a2 = (((i + 1) / (double) segments) * Math.PI * 2.0) + twist1;
                    double a3 = (((i + 1) / (double) segments) * Math.PI * 2.0) + twist2;
                    double a4 = ((i / (double) segments) * Math.PI * 2.0) + twist2;

                    float x1 = (float) Math.cos(a1) * r1;
                    float y1 = (float) Math.sin(a1) * r1;
                    float x2 = (float) Math.cos(a2) * r1;
                    float y2 = (float) Math.sin(a2) * r1;
                    float x3 = (float) Math.cos(a3) * r2;
                    float y3 = (float) Math.sin(a3) * r2;
                    float x4 = (float) Math.cos(a4) * r2;
                    float y4 = (float) Math.sin(a4) * r2;

                    drawQuad(matrix, buffer,
                        x1, y1, z1,
                        x2, y2, z1,
                        x3, y3, z2,
                        x4, y4, z2,
                        crA, cgA, cbA, tierAlpha
                    );
                }
            }

            // Layer B: Outer Fluted Emerald Wind Pressure Sheath
            for (int t = 0; t < tiers; t++) {
                float z1 = (t / (float) tiers) * range + 0.5f;
                float z2 = ((t + 1) / (float) tiers) * range + 0.5f;

                float r1 = z1 * coneTan * 0.95f;
                float r2 = z2 * coneTan * 0.95f;

                float tierAlpha = 0.40f * (1.0f - (t / (float) tiers) * 0.4f) * finalAlpha;
                float twist1 = -spinRot * 0.8f + (z1 * 0.25f);
                float twist2 = -spinRot * 0.8f + (z2 * 0.25f);

                // 3-stop gradient: pale emerald at the origin, full emerald mid,
                // deep emerald at the far mouth.
                float tfB = t / (float) tiers;
                float crB, cgB, cbB;
                if (tfB < 0.5f) {
                    float k = tfB / 0.5f;
                    crB = 0.45f + (0.0f - 0.45f) * k; cgB = 1.0f; cbB = 0.80f + (0.55f - 0.80f) * k;
                } else {
                    float k = (tfB - 0.5f) / 0.5f;
                    crB = 0.0f; cgB = 1.0f + (0.80f - 1.0f) * k; cbB = 0.55f + (0.42f - 0.55f) * k;
                }

                for (int i = 0; i < segments; i++) {
                    double a1 = ((i / (double) segments) * Math.PI * 2.0) + twist1;
                    double a2 = (((i + 1) / (double) segments) * Math.PI * 2.0) + twist1;
                    double a3 = (((i + 1) / (double) segments) * Math.PI * 2.0) + twist2;
                    double a4 = ((i / (double) segments) * Math.PI * 2.0) + twist2;

                    float x1 = (float) Math.cos(a1) * r1;
                    float y1 = (float) Math.sin(a1) * r1;
                    float x2 = (float) Math.cos(a2) * r1;
                    float y2 = (float) Math.sin(a2) * r1;
                    float x3 = (float) Math.cos(a3) * r2;
                    float y3 = (float) Math.sin(a3) * r2;
                    float x4 = (float) Math.cos(a4) * r2;
                    float y4 = (float) Math.sin(a4) * r2;

                    drawQuad(matrix, buffer,
                        x1, y1, z1,
                        x2, y2, z1,
                        x3, y3, z2,
                        x4, y4, z2,
                        crB, cgB, cbB, tierAlpha
                    );
                }
            }

            // 3. Quad Counter-Rotating Helical Wind Ribbons
            int ribbons = 4;
            int ribbonSteps = 24;
            for (int r = 0; r < ribbons; r++) {
                float ribbonOffset = (r / (float) ribbons) * (float) (Math.PI * 2.0);
                boolean isCyan = (r % 2 == 0);
                float redC = isCyan ? 0.2f : 0.0f;
                float greenC = 1.0f;
                float blueC = isCyan ? 0.95f : 0.60f;

                for (int s = 0; s < ribbonSteps; s++) {
                    float t1 = s / (float) ribbonSteps;
                    float t2 = (s + 1) / (float) ribbonSteps;

                    float z1 = t1 * range + 0.4f;
                    float z2 = t2 * range + 0.4f;

                    float rad1 = z1 * coneTan * 0.85f;
                    float rad2 = z2 * coneTan * 0.85f;

                    double ang1 = ribbonOffset + spinRot + (t1 * Math.PI * 3.5);
                    double ang2 = ribbonOffset + spinRot + (t2 * Math.PI * 3.5);

                    float x1 = (float) Math.cos(ang1) * rad1;
                    float y1 = (float) Math.sin(ang1) * rad1;
                    float x2 = (float) Math.cos(ang2) * rad2;
                    float y2 = (float) Math.sin(ang2) * rad2;

                    float bandW = 0.25f + t1 * 0.35f;
                    float tx1 = (float) -Math.sin(ang1) * bandW;
                    float ty1 = (float) Math.cos(ang1) * bandW;
                    float tx2 = (float) -Math.sin(ang2) * bandW;
                    float ty2 = (float) Math.cos(ang2) * bandW;

                    float along = t1 * t1 * (3.0f - 2.0f * t1);
                    drawQuad(matrix, buffer,
                        x1 - tx1, y1 - ty1, z1,
                        x1 + tx1, y1 + ty1, z1,
                        x2 + tx2, y2 + ty2, z2,
                        x2 - tx2, y2 - ty2, z2,
                        redC, greenC, blueC, (1.0f - along) * 0.7f * finalAlpha
                    );
                }
            }

            // Crescent cutters riding the cone. Diamonds of wind, not slabs.
            int bladeCount = 10;
            for (int b = 0; b < bladeCount; b++) {
                float bProgress = ((b / (float) bladeCount) + (age * 0.18f)) % 1.0f;
                float bZ = bProgress * range + 0.6f;
                float bRadius = bZ * coneTan * 0.82f;
                double bAngle = (b / (float) bladeCount) * Math.PI * 2.0 + (spinRot * 1.5) + (bProgress * Math.PI * 2.5);
                float bx = (float) Math.cos(bAngle) * bRadius;
                float by = (float) Math.sin(bAngle) * bRadius;
                float tx = (float) -Math.sin(bAngle);
                float ty = (float) Math.cos(bAngle);
                float bladeLen = 0.85f + bProgress * 0.55f;
                float bladeW = 0.16f + bProgress * 0.08f;
                float tipX = bx + tx * (bladeLen * 0.62f);
                float tipY = by + ty * (bladeLen * 0.62f);
                float tipZb = bZ + 0.35f;
                float tailX = bx - tx * (bladeLen * 0.38f);
                float tailY = by - ty * (bladeLen * 0.38f);
                float tailZ = bZ - 0.18f;
                float nx = -ty * bladeW;
                float ny = tx * bladeW;
                float bladeFade = (1.0f - bProgress);
                bladeFade = bladeFade * bladeFade;
                float bladeAlpha = 0.9f * finalAlpha * (0.4f + 0.6f * bladeFade);
                float radX = (float) Math.cos(bAngle);
                float radY = (float) Math.sin(bAngle);
                float unx = -ty;
                float uny = tx;
                int crest = 6;
                for (int s = 0; s < crest; s++) {
                    float u1 = s / (float) crest;
                    float u2 = (s + 1) / (float) crest;
                    float bow1 = (float) Math.sin(u1 * Math.PI);
                    float bow2 = (float) Math.sin(u2 * Math.PI);
                    float xA = tailX + (tipX - tailX) * u1 + radX * bow1 * bladeW * 1.5f;
                    float yA = tailY + (tipY - tailY) * u1 + radY * bow1 * bladeW * 1.5f;
                    float zA = tailZ + (tipZb - tailZ) * u1;
                    float xB = tailX + (tipX - tailX) * u2 + radX * bow2 * bladeW * 1.5f;
                    float yB = tailY + (tipY - tailY) * u2 + radY * bow2 * bladeW * 1.5f;
                    float zB = tailZ + (tipZb - tailZ) * u2;
                    float wA = bladeW * (0.28f + 0.72f * bow1) * (1.0f - u1 * 0.7f);
                    float wB = bladeW * (0.28f + 0.72f * bow2) * (1.0f - u2 * 0.7f);
                    float aA = bladeAlpha * (0.15f + 0.85f * u1);
                    float aB = bladeAlpha * (0.15f + 0.85f * u2);
                    drawQuad(matrix, buffer,
                        xA - unx * wA, yA - uny * wA, zA,
                        xA + unx * wA, yA + uny * wA, zA,
                        xB + unx * wB, yB + uny * wB, zB,
                        xB - unx * wB, yB - uny * wB, zB,
                        0.05f, 0.82f, 0.62f, (aA + aB) * 0.5f);
                    drawQuad(matrix, buffer,
                        xA - unx * wA * 0.28f, yA - uny * wA * 0.28f, zA,
                        xA + unx * wA * 0.28f, yA + uny * wA * 0.28f, zA,
                        xB + unx * wB * 0.28f, yB + uny * wB * 0.28f, zB,
                        xB - unx * wB * 0.28f, yB - uny * wB * 0.28f, zB,
                        0.9f, 1.0f, 1.0f, (aA + aB) * 0.5f);
                }
            }

            // Shock rings run the length of the cone and die at the mouth.
            int shockWaves = 3;
            for (int w = 0; w < shockWaves; w++) {
                float waveProgress = ((age * 0.30f) + (w / (float) shockWaves)) % 1.0f;
                float waveEase = waveProgress * waveProgress * (3.0f - 2.0f * waveProgress);
                float waveZ = 0.5f + waveEase * Math.max(0.2f, range - 0.8f);
                float waveR = waveZ * coneTan * 0.92f;
                float waveAlpha = (float) Math.sin(waveProgress * (float) Math.PI) * 0.65f * finalAlpha;
                drawRingAt(matrix, buffer, waveZ, waveR * 0.86f, waveR, 0.55f, 1.0f, 0.92f, waveAlpha);
            }

            // One ring travels to the mouth of the cone and fades there.
            float mouth = lifeT * lifeT * (3.0f - 2.0f * lifeT);
            float mouthZ = 0.6f + mouth * Math.max(0.2f, range - 0.8f);
            float mouthR = mouthZ * coneTan * 0.95f;
            float mouthA = (float) Math.sin(Math.min(1.0f, lifeT) * Math.PI) * 0.55f * finalAlpha;
            drawRingAt(matrix, buffer, mouthZ, mouthR * 0.82f, mouthR, 0.85f, 1.0f, 0.95f, mouthA);
        });

        poseStack.popPose();
    }

    private static void drawRingAt(Matrix4f matrix, VertexConsumer buffer, float z, float innerR, float outerR, float r, float g, float b, float a) {
        int segments = 28;
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;
            drawQuad(matrix, buffer,
                (float) Math.cos(a1) * innerR, (float) Math.sin(a1) * innerR, z,
                (float) Math.cos(a2) * innerR, (float) Math.sin(a2) * innerR, z,
                (float) Math.cos(a2) * outerR, (float) Math.sin(a2) * outerR, z,
                (float) Math.cos(a1) * outerR, (float) Math.sin(a1) * outerR, z,
                r, g, b, a);
        }
    }

    private static void drawOriginDisc(Matrix4f matrix, VertexConsumer buffer, float innerR, float outerR, int segments, float rotZ, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            double a1 = ((i / (double) segments) * Math.PI * 2.0) + rotZ;
            double a2 = (((i + 1) / (double) segments) * Math.PI * 2.0) + rotZ;

            float x1 = (float) Math.cos(a1) * innerR;
            float y1 = (float) Math.sin(a1) * innerR;
            float x2 = (float) Math.cos(a2) * innerR;
            float y2 = (float) Math.sin(a2) * innerR;

            float x3 = (float) Math.cos(a2) * outerR;
            float y3 = (float) Math.sin(a2) * outerR;
            float x4 = (float) Math.cos(a1) * outerR;
            float y4 = (float) Math.sin(a1) * outerR;

            drawQuad(matrix, buffer, x1, y1, 0, x2, y2, 0, x3, y3, 0, x4, y4, 0, r, g, b, a);
        }
    }

    private static void drawQuad(Matrix4f matrix, VertexConsumer buffer,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float x4, float y4, float z4,
                                 float r, float g, float b, float a) {
        buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        buffer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        buffer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void drawTriangle(Matrix4f matrix, VertexConsumer buffer,
                                     float x1, float y1, float z1,
                                     float x2, float y2, float z2,
                                     float x3, float y3, float z3,
                                     float r, float g, float b, float a) {
        buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        buffer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(0.5f, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
    }
}
