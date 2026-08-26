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

    @Override
    public boolean shouldRender(PowerPoleWhirlwindEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
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

        // Smooth fade in & fade out
        float alphaMult = 1.0f;
        if (age < 1.5f) {
            alphaMult = age / 1.5f;
        } else if (age > maxLife - 2.5f) {
            alphaMult = Math.max(0.0f, (maxLife - age) / 2.5f);
        }

        final float finalAlpha = alphaMult;
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));

        float fpScale = state.isFirstPersonOwner ? 0.55f : 1.0f;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // 1. Staff Origin Kinetic Whirling Sweep Disc (Rapid staff spinning plane at base)
            float spinRot = age * 65.0f * (float) (Math.PI / 180.0);
            float discR1 = 2.2f * fpScale;
            float discR2 = 1.6f * fpScale;
            drawOriginDisc(matrix, buffer, 0.4f, discR1, 20, spinRot, 0.0f, 1.0f, 0.75f, 0.70f * finalAlpha);
            drawOriginDisc(matrix, buffer, 0.8f, discR2, 16, -spinRot * 1.3f, 1.0f, 0.85f, 0.2f, 0.60f * finalAlpha);

            // 2. Multi-Tiered Conical Aerodynamic Hurricane Vortex Tunnel
            int tiers = 12;
            int segments = 20;
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
                        0.75f, 1.0f, 1.0f, tierAlpha
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
                        0.0f, 1.0f, 0.55f, tierAlpha
                    );
                }
            }

            // 3. Quad Counter-Rotating Helical Wind Ribbons
            int ribbons = 4;
            int ribbonSteps = 18;
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

                    drawQuad(matrix, buffer,
                        x1 - tx1, y1 - ty1, z1,
                        x1 + tx1, y1 + ty1, z1,
                        x2 + tx2, y2 + ty2, z2,
                        x2 - tx2, y2 - ty2, z2,
                        redC, greenC, blueC, 0.65f * finalAlpha
                    );
                }
            }

            // 4. Physical 3D Orbiting Crescent Gale Blades (10 blades flying forward in the gale)
            int bladeCount = 10;
            for (int b = 0; b < bladeCount; b++) {
                float bProgress = ((b / (float) bladeCount) + (age * 0.18f)) % 1.0f;
                float bZ = bProgress * range + 0.6f;
                float bRadius = bZ * coneTan * 0.82f;

                double bAngle = (b / (float) bladeCount) * Math.PI * 2.0 + (spinRot * 1.5) + (bProgress * Math.PI * 2.5);

                float bx = (float) Math.cos(bAngle) * bRadius;
                float by = (float) Math.sin(bAngle) * bRadius;

                // Tangent vector
                float tx = (float) -Math.sin(bAngle);
                float ty = (float) Math.cos(bAngle);

                float bladeLen = 0.8f + bProgress * 0.6f;
                float bladeW = 0.22f + bProgress * 0.12f;

                float tipX = bx + tx * (bladeLen * 0.6f);
                float tipY = by + ty * (bladeLen * 0.6f);
                float tipZ = bZ + 0.3f;

                float tailX = bx - tx * (bladeLen * 0.4f);
                float tailY = by - ty * (bladeLen * 0.4f);
                float tailZ = bZ - 0.2f;

                float nx = -ty * bladeW;
                float ny = tx * bladeW;

                // 3D Diamond Wind Blade
                drawTriangle(matrix, buffer,
                    tailX - nx, tailY - ny, tailZ,
                    tailX + nx, tailY + ny, tailZ,
                    tipX, tipY, tipZ,
                    0.8f, 1.0f, 1.0f, 0.85f * finalAlpha
                );
                drawTriangle(matrix, buffer,
                    tailX - nx, tailY - ny, tailZ,
                    tipX, tipY, tipZ,
                    tailX + nx, tailY + ny, tailZ,
                    0.0f, 1.0f, 0.70f, 0.85f * finalAlpha
                );
            }

            // 5. Supersonic Mach Pressure Shock Rings (Concentric compression wavefronts)
            int shockWaves = 3;
            for (int w = 0; w < shockWaves; w++) {
                float waveProgress = ((age * 0.30f) + (w / (float) shockWaves)) % 1.0f;
                float waveZ = waveProgress * range + 0.5f;
                float waveR = waveZ * coneTan * 0.90f;
                float waveAlpha = (1.0f - waveProgress) * 0.75f * finalAlpha;

                drawOriginDisc(matrix, buffer, waveR * 0.85f, waveR * 1.05f, 20, waveZ, 0.4f, 1.0f, 0.9f, waveAlpha);
            }
        });

        poseStack.popPose();
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
