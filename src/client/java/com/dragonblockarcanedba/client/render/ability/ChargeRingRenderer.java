package com.dragonblockarcanedba.client.render.ability;

import com.dragonblockarcanedba.ability.AbilityDef;
import com.dragonblockarcanedba.client.ability.CastAnimations;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The generic charge read for the Phase 2 animation layer: while
 * any player winds up a cast (state from CastAnimations, so caster
 * and observers see the same thing), a ground ring at their feet
 * grows with the charge fraction and brightens with the cast's
 * power curve, in the ability's own rim and mid colors. Two dashed
 * arcs inside the ring rotate against each other, faster as power
 * grows, which is the vortex read; at full charge a bright core
 * ring pulses to say the release is at full power. This is the
 * honest Phase 2 form of the charge vortex: flat ground geometry
 * under a posed caster. The torn Saturn ring from the Z Shockwave
 * picture is primitive kit work (Phase 4) and will replace the
 * arcs behind this same class.
 *
 * Distance behavior comes from the ability's own caps block: past
 * fullDetailDistance only the outer ring draws; past cullDistance
 * nothing draws.
 */
public final class ChargeRingRenderer {
    private ChargeRingRenderer() {}

    private static boolean registered;

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        LevelRenderEvents.COLLECT_SUBMITS.register(ChargeRingRenderer::collectSubmits);
    }

    private static void collectSubmits(LevelRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || context.levelState() == null
                || context.levelState().cameraRenderState == null
                || !context.levelState().cameraRenderState.initialized
                || context.levelState().cameraRenderState.pos == null
                || context.poseStack() == null || context.submitNodeCollector() == null) {
            return;
        }
        Vec3 cam = context.levelState().cameraRenderState.pos;
        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        long gameTime = mc.level.getGameTime();
        PoseStack stack = context.poseStack();
        SubmitNodeCollector collector = context.submitNodeCollector();

        for (AbstractClientPlayer player : mc.level.players()) {
            CastAnimations.ChargeInfo info = CastAnimations.chargeInfo(player.getId(), gameTime, partial);
            if (info == null) {
                continue;
            }
            Vec3 pos = player.getPosition(partial);
            double dist = cam.distanceTo(pos);
            AbilityDef def = info.def();
            if (dist > def.caps().cullDistance()) {
                continue;
            }
            boolean fullDetail = dist <= def.caps().fullDetailDistance();
            submitRing(stack, collector, cam, pos, player, info, fullDetail, gameTime + partial);
        }
    }

    private static void submitRing(PoseStack stack, SubmitNodeCollector collector, Vec3 cam, Vec3 pos,
                                   AbstractClientPlayer player, CastAnimations.ChargeInfo info,
                                   boolean fullDetail, float age) {
        AbilityDef def = info.def();
        float scale = def.visual().scale();
        float frac = info.frac();
        float power = info.power();
        double radius = (0.55 + 1.75 * frac) * scale;
        int rim = def.visual().rim();
        int mid = def.visual().mid();
        int core = def.visual().core();
        float rr = CarrierGeometry.r(rim), rg = CarrierGeometry.g(rim), rb = CarrierGeometry.b(rim);
        float mr = CarrierGeometry.r(mid), mg = CarrierGeometry.g(mid), mb = CarrierGeometry.b(mid);
        float cr = CarrierGeometry.r(core), cg = CarrierGeometry.g(core), cb = CarrierGeometry.b(core);
        float outerAlpha = 0.30f + 0.45f * power;
        // Vortex spin: the two dashed arcs counter-rotate, and the
        // rate follows the power curve, so the swirl visibly winds
        // up with the charge.
        double spin = age * (0.6 + 2.2 * power);
        float seedPhase = (player.getUUID().hashCode() & 0xFF) * 0.013f;

        stack.pushPose();
        try {
            stack.translate(pos.x - cam.x, pos.y - cam.y + 0.06, pos.z - cam.z);
            ComplexGeometry.submitEnergy(collector, stack, (pose, buffer) -> {
                CarrierGeometry.flatRing(pose, buffer, 0, 0, 0,
                        radius - 0.09, radius, 40, rr, rg, rb, outerAlpha);
                if (fullDetail) {
                    dashedArc(pose, buffer, radius * 0.80, 0.10, spin + seedPhase,
                            mr, mg, mb, 0.35f + 0.45f * power);
                    dashedArc(pose, buffer, radius * 0.58, 0.08, -spin * 1.35 + seedPhase,
                            mr, mg, mb, 0.28f + 0.40f * power);
                    if (power >= 0.999f) {
                        float pulse = 0.55f + 0.35f * (float) Math.sin(age * 1.4 + seedPhase);
                        CarrierGeometry.flatRing(pose, buffer, 0, 0, 0,
                                radius * 0.30, radius * 0.38, 28, cr, cg, cb, pulse);
                    }
                }
            });
        } finally {
            stack.popPose();
        }
    }

    /**
     * A flat dashed arc band: 30 segments around the circle, every
     * third segment left open, the whole pattern rotated by the
     * given angle so successive frames read as rotation.
     */
    private static void dashedArc(PoseStack.Pose pose, VertexConsumer buffer,
                                  double radius, double width, double rotation,
                                  float r, float g, float b, float a) {
        Matrix4f mat = pose.pose();
        int segments = 30;
        double rIn = radius - width * 0.5;
        double rOut = radius + width * 0.5;
        for (int i = 0; i < segments; i++) {
            if (i % 3 == 2) {
                continue;
            }
            double a0 = rotation + Math.PI * 2.0 * i / segments;
            double a1 = rotation + Math.PI * 2.0 * (i + 1) / segments;
            double c0 = Math.cos(a0), s0 = Math.sin(a0);
            double c1 = Math.cos(a1), s1 = Math.sin(a1);
            quad(buffer, mat, pose,
                    c0 * rIn, s0 * rIn, c1 * rIn, s1 * rIn,
                    c1 * rOut, s1 * rOut, c0 * rOut, s0 * rOut,
                    r, g, b, a);
        }
    }

    private static void quad(VertexConsumer buffer, Matrix4f mat, PoseStack.Pose pose,
                             double x0, double z0, double x1, double z1,
                             double x2, double z2, double x3, double z3,
                             float r, float g, float b, float a) {
        v(buffer, mat, pose, x0, z0, r, g, b, a);
        v(buffer, mat, pose, x1, z1, r, g, b, a);
        v(buffer, mat, pose, x2, z2, r, g, b, a);
        v(buffer, mat, pose, x3, z3, r, g, b, a);
        v(buffer, mat, pose, x3, z3, r, g, b, a);
        v(buffer, mat, pose, x2, z2, r, g, b, a);
        v(buffer, mat, pose, x1, z1, r, g, b, a);
        v(buffer, mat, pose, x0, z0, r, g, b, a);
    }

    private static void v(VertexConsumer buffer, Matrix4f mat, PoseStack.Pose pose,
                          double x, double z, float r, float g, float b, float a) {
        KiRenderHelper.vertex(buffer, mat, pose, (float) x, 0f, (float) z,
                r, g, b, a, 0f, 0f, 0f, 1f, 0f);
    }
}
