package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.ErasureCannonBeamEntity;
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
 * Entity Renderer for Erasure Cannon Beam in MC 26.2.
 * Renders a massive, continuous physical 3D geometric laser cylinder across the 48-meter distance:
 * - Multi-layered synchrotron laser core (superdense white core, 12-sided cyan plasma shroud, outer hex barrier)
 * - Quad counter-rotating helical synchrotron drill ribbons spanning the entire 48m raycast length
 * - Stationed hexagonal focusing aperture rings along the beam
 * - Muzzle supercharge emitter geometry with compression blades
 * - Terminus vaporization hemisphere dome, cross shock rings, and radiating splash fins
 */
public class ErasureCannonBeamRenderer extends EntityRenderer<ErasureCannonBeamEntity, ErasureCannonBeamRenderer.ErasureCannonRenderState> {

    public ErasureCannonBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ErasureCannonRenderState extends EntityRenderState {
        public float length = 48.0f;
        public float yRot = 0.0f;
        public float xRot = 0.0f;
        public float chargeRatio = 1.0f;
        public float age = 0.0f;
        public boolean isFirstPersonOwner = false;
        public boolean onRight = true;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(ErasureCannonBeamEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(ErasureCannonBeamEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ErasureCannonRenderState createRenderState() {
        return new ErasureCannonRenderState();
    }

    @Override
    public void extractRenderState(ErasureCannonBeamEntity entity, ErasureCannonRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.length = entity.getBeamLength();
        state.yRot = entity.getBeamYRot();
        state.xRot = entity.getBeamXRot();
        state.chargeRatio = entity.getChargeRatio();
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
    public void submit(ErasureCannonRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float beamLength = state.length;
        if (beamLength < 0.2f) return;

        float charge = state.chargeRatio;
        float age = state.age;
        float maxLifetime = ErasureCannonBeamEntity.MAX_LIFETIME;

        // Shared envelope (spectacle standard point 5): the renderer now uses
        // the same FxEnvelope the entity's particle counts use, so mesh alpha
        // and particle density build and dissipate together over the 24 tick
        // life instead of the old private 3 tick in, 0.58 out curve over 16.
        float env = com.dragonblockarcanedba.util.FxEnvelope.envelope(age, maxLifetime);
        float alpha = env;
        float exitT = age <= maxLifetime * 0.7f ? 0.0f : Math.min(1.0f, (age - maxLifetime * 0.7f) / (maxLifetime * 0.3f));

        float pulse = (1.0f + 0.04f * (float) Math.sin(age * 1.4f)) * (0.55f + 0.45f * env);
        float impactPulse = pulse * (1.12f - exitT * 0.50f);

        // Distance scaling (standard point 1): ribbon segment counts step
        // down beyond 32 and 64 blocks so a 48 block beam stays cheap to
        // watch from across a field.
        double camDist = cameraState.pos.distanceTo(new net.minecraft.world.phys.Vec3(state.x, state.y, state.z));
        final float distScale = com.dragonblockarcanedba.util.FxEnvelope.distScale(camDist);
        // Scaled down smaller in first person
        float baseRadius = (0.35f + charge * 0.45f) * pulse * (state.isFirstPersonOwner ? 0.50f : 1.0f);

        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        if (state.isFirstPersonOwner) {
            // Anchor beam origin to the Blaster Gun muzzle: way lower and on weapon hand side
            float sideSign = state.onRight ? -1.0f : 1.0f;
            poseStack.translate(sideSign * 0.38f, -0.46f, 0.40f);
        }

        float zStart = state.isFirstPersonOwner ? 0.20f : 0.0f;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();

            // One cyan beam. The white rod is its core. A thin darker lip is the edge.
            // Brightness runs from the muzzle to the terminus, then the impact closes.
            drawFlowBeam(matrix, buffer, baseRadius * 0.28f, zStart, beamLength, 12, age * 4.0f, 1.0f, 1.0f, 1.0f, alpha, 0.08f, 0.0f, age);
            drawFlowBeam(matrix, buffer, baseRadius * 0.72f, zStart, beamLength, 12, -age * 6.0f, 0.0f, 0.92f, 1.0f, alpha * 0.78f, 0.22f, 1.6f, age);
            drawFlowBeam(matrix, buffer, baseRadius * 0.86f, zStart, beamLength, 12, -age * 6.0f, 0.0f, 0.55f, 0.85f, alpha * 0.35f, 0.35f, 1.6f, age);

            // Helical drill ribbons riding the tapered beam. They are the curve around the core, not a second tube.
            int ribbonSegments = Math.max(12, Math.round(Math.min((int) (beamLength * 3.0f), 96) * distScale));
            float spiralRadius = baseRadius * 1.15f;
            float ribbonSize = 0.045f * pulse;
            float spiralSpeed = age * 0.45f;
            if (ribbonSegments > 1 && beamLength > zStart) {
                for (int h = 0; h < 4; h++) {
                    float phase = h * ((float) Math.PI * 0.5f);
                    boolean isWhite = (h % 2 == 1);
                    float rCol = isWhite ? 1.0f : 0.0f;
                    float gCol = isWhite ? 1.0f : 0.92f;
                    float bCol = isWhite ? 0.95f : 1.0f;
                    float aCol = (isWhite ? 0.80f : 0.62f) * alpha;
                    for (int i = 0; i < ribbonSegments; i++) {
                        float p1 = i / (float) ribbonSegments;
                        float p2 = (i + 1) / (float) ribbonSegments;
                        float z1 = zStart + p1 * (beamLength - zStart);
                        float z2 = zStart + p2 * (beamLength - zStart);
                        double a1 = (p1 * Math.PI * 10.0) + (spiralSpeed * (isWhite ? -1.0f : 1.0f)) + phase;
                        double a2 = (p2 * Math.PI * 10.0) + (spiralSpeed * (isWhite ? -1.0f : 1.0f)) + phase;
                        float taper1 = 1.0f - 0.28f * p1;
                        float taper2 = 1.0f - 0.28f * p2;
                        float x1 = (float) Math.cos(a1) * spiralRadius * taper1;
                        float y1 = (float) Math.sin(a1) * spiralRadius * taper1;
                        float x2 = (float) Math.cos(a2) * spiralRadius * taper2;
                        float y2 = (float) Math.sin(a2) * spiralRadius * taper2;
                        float flowBand = 0.5f + 0.5f * (float) Math.sin(p1 * 18.0 - age * 3.0f + h * 1.7f);
                        float segAlpha = aCol * (0.55f + 0.45f * flowBand) * (1.0f - p1 * 0.35f);
                        float w1 = ribbonSize * taper1;
                        float w2 = ribbonSize * taper2;
                        float px1 = (float) -Math.sin(a1) * w1;
                        float py1 = (float) Math.cos(a1) * w1;
                        float px2 = (float) -Math.sin(a2) * w2;
                        float py2 = (float) Math.cos(a2) * w2;
                        drawQuad(matrix, buffer,
                            x1 - px1, y1 - py1, z1,
                            x1 + px1, y1 + py1, z1,
                            x2 + px2, y2 + py2, z2,
                            x2 - px2, y2 - py2, z2,
                            rCol, gCol, bCol, segAlpha
                        );
                    }
                }
            }

            // Stationed hexagonal focusing aperture rings along the beam:
            // the class header has promised these since the renderer was
            // written and submit() never drew them. Three hex rings sit at
            // quarter, half, and three-quarter length, counter-rotating,
            // tightening slightly with distance from the muzzle, alpha on
            // the shared envelope. Far views drop the last ring first.
            float[] ringFracs = {0.25f, 0.50f, 0.75f};
            for (int ri = 0; ri < ringFracs.length; ri++) {
                if (distScale < 0.55f && ri == 2) continue;
                float frac = ringFracs[ri];
                float ringZ = zStart + frac * (beamLength - zStart);
                float ringRadius = baseRadius * (2.3f - frac * 0.7f);
                float ringRot = age * (ri % 2 == 0 ? 30.0f : -30.0f) + ri * 30.0f;
                drawPlaneRing(matrix, buffer, 0, 0, ringZ, ringRadius, ringRadius * 0.82f, 6, ringRot,
                    0.35f, 0.95f, 1.0f, alpha * 0.55f);
            }

            drawMuzzleEmitter(matrix, buffer, age, pulse, alpha);
            drawTerminusImpact(matrix, buffer, beamLength, age, impactPulse, alpha, charge);
        });

        poseStack.popPose();
    }

    // Beam tube with energy flow (T7): the length is drawn in bands, each band
    // shaded by a traveling sine brightness wave (phase runs from muzzle to
    // terminus as age advances) and the radius tapers by the taper factor
    // toward the terminus. Replaces the old single-quad uniform prism.
    private static void drawFlowBeam(Matrix4f matrix, VertexConsumer consumer, float radius, float zStart, float length, int sides, float rotDeg, float r, float g, float b, float a, float taper, float flowPhase, float age) {
        double rotRad = Math.toRadians(rotDeg);
        float span = length - zStart;
        if (span <= 0.0f) return;
        int bands = Math.max(6, Math.min(20, (int) (span / 2.5f)));

        for (int band = 0; band < bands; band++) {
            float p1 = band / (float) bands;
            float p2 = (band + 1) / (float) bands;

            float z1 = zStart + p1 * span;
            float z2 = zStart + p2 * span;

            float rad1 = radius * (1.0f - taper * p1);
            float rad2 = radius * (1.0f - taper * p2);

            float wave1 = 0.5f + 0.5f * (float) Math.sin(p1 * 14.0 - age * 2.4f + flowPhase);
            float wave2 = 0.5f + 0.5f * (float) Math.sin(p2 * 14.0 - age * 2.4f + flowPhase);
            float a1Band = a * (0.70f + 0.30f * wave1);
            float a2Band = a * (0.70f + 0.30f * wave2);

            for (int i = 0; i < sides; i++) {
                double ang1 = (i / (double) sides) * Math.PI * 2.0 + rotRad;
                double ang2 = ((i + 1) / (double) sides) * Math.PI * 2.0 + rotRad;

                float x1a = (float) Math.cos(ang1) * rad1;
                float y1a = (float) Math.sin(ang1) * rad1;
                float x2a = (float) Math.cos(ang2) * rad1;
                float y2a = (float) Math.sin(ang2) * rad1;

                float x1b = (float) Math.cos(ang1) * rad2;
                float y1b = (float) Math.sin(ang1) * rad2;
                float x2b = (float) Math.cos(ang2) * rad2;
                float y2b = (float) Math.sin(ang2) * rad2;

                consumer.addVertex(matrix, x1a, y1a, z1).setColor(r, g, b, a1Band).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x2a, y2a, z1).setColor(r, g, b, a1Band).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x2b, y2b, z2).setColor(r, g, b, a2Band).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x1b, y1b, z2).setColor(r, g, b, a2Band).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);

                // Reverse side, same gradient
                consumer.addVertex(matrix, x1b, y1b, z2).setColor(r, g, b, a2Band).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
                consumer.addVertex(matrix, x2b, y2b, z2).setColor(r, g, b, a2Band).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
                consumer.addVertex(matrix, x2a, y2a, z1).setColor(r, g, b, a1Band).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
                consumer.addVertex(matrix, x1a, y1a, z1).setColor(r, g, b, a1Band).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
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

    private static void drawMuzzleEmitter(Matrix4f matrix, VertexConsumer consumer, float age, float pulse, float alpha) {
        drawPlaneRing(matrix, consumer, 0, 0, 0.16f, 0.72f * pulse, 0.52f * pulse, 16, age * 24.0f, 0.85f, 0.98f, 1.0f, alpha * 0.95f);

        for (int s = 0; s < 4; s++) {
            double sAngle = (s / 4.0) * Math.PI * 2.0 + Math.toRadians(age * 22.0f);
            float ox = (float) Math.cos(sAngle);
            float oy = (float) Math.sin(sAngle);
            drawLash(matrix, consumer,
                ox * 1.05f * pulse, oy * 1.05f * pulse, 0.02f,
                ox * 0.55f * pulse, oy * 0.55f * pulse, 0.24f,
                ox * 0.06f * pulse, oy * 0.06f * pulse, 0.58f,
                0.055f * pulse, 5,
                0.15f, 0.75f, 1.0f,
                1.0f, 1.0f, 1.0f,
                alpha
            );
        }
    }

    private static void drawTerminusImpact(Matrix4f matrix, VertexConsumer consumer, float beamLength, float age, float pulse, float alpha, float charge) {
        float domeRadius = (1.2f + charge * 0.8f) * pulse;
        int segments = 16;

        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2.0;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1 = (float) Math.cos(a1) * domeRadius;
            float y1 = (float) Math.sin(a1) * domeRadius;
            float x2 = (float) Math.cos(a2) * domeRadius;
            float y2 = (float) Math.sin(a2) * domeRadius;

            // Outer flared impact dome
            drawQuad(matrix, consumer,
                x1 * 0.4f, y1 * 0.4f, beamLength - 0.4f,
                x2 * 0.4f, y2 * 0.4f, beamLength - 0.4f,
                x2, y2, beamLength,
                x1, y1, beamLength,
                0.0f, 0.95f, 1.0f, alpha * 0.85f
            );

            float ux1 = (float) Math.cos(a1);
            float uy1 = (float) Math.sin(a1);
            float ux2 = (float) Math.cos(a2);
            float uy2 = (float) Math.sin(a2);
            float coreA = domeRadius * 0.22f;
            float coreB = domeRadius * 0.04f;
            drawQuad(matrix, consumer,
                ux1 * coreA, uy1 * coreA, beamLength - 0.08f,
                ux2 * coreA, uy2 * coreA, beamLength - 0.08f,
                ux2 * coreB, uy2 * coreB, beamLength + 0.28f,
                ux1 * coreB, uy1 * coreB, beamLength + 0.28f,
                1.0f, 1.0f, 1.0f, alpha * 0.75f
            );
        }

        int finCount = 6;
        for (int f = 0; f < finCount; f++) {
            double fAngle = (f / (double) finCount) * Math.PI * 2.0 + Math.toRadians(age * 30.0f);
            float ox = (float) Math.cos(fAngle);
            float oy = (float) Math.sin(fAngle);
            drawLash(matrix, consumer,
                ox * domeRadius * 0.22f, oy * domeRadius * 0.22f, beamLength - 0.04f,
                ox * domeRadius * 0.80f, oy * domeRadius * 0.80f, beamLength + 0.06f,
                ox * domeRadius * 1.40f, oy * domeRadius * 1.40f, beamLength + 0.32f,
                0.07f * Math.max(0.2f, domeRadius), 6,
                0.20f, 0.85f, 1.0f,
                1.0f, 1.0f, 1.0f,
                alpha * 0.90f
            );
        }
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

        // Reverse
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }
}
