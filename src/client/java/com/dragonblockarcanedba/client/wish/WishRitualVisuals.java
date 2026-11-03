package com.dragonblockarcanedba.client.wish;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.network.WishRitualStartPayload;
import com.dragonblockarcanedba.wish.DragonWishRitual;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Balls travel onto a floating heptagon, then a column, helices, and toroidal
 * rings gather until the dragon appears. Drawn with ki_white and vertex color.
 */
public final class WishRitualVisuals {
    private static final float MAX_AGE = DragonWishRitual.SCREEN_AT + 40.0f;
    private static final List<Ritual> ACTIVE = new ArrayList<>();

    private WishRitualVisuals() {}

    public static void begin(WishRitualStartPayload payload) {
        BlockPos[] balls = payload.balls();
        if (balls == null || balls.length != DragonWishRitual.BALLS) {
            return;
        }
        Vec3 center = DragonWishRitual.centerOf(balls);
        int[] slots = DragonWishRitual.assignSlots(balls, center);
        Vec3[] from = new Vec3[DragonWishRitual.BALLS];
        for (int i = 0; i < balls.length; i++) {
            BlockPos ball = balls[i];
            from[i] = new Vec3(ball.getX() + 0.5, ball.getY() + 0.5, ball.getZ() + 0.5);
        }
        ACTIVE.add(new Ritual(payload.namek(), payload.gameTime(), center, from, slots));
    }

    public static void render(LevelRenderContext context) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        CameraRenderState camera = context.levelState().cameraRenderState;
        if (camera == null || !camera.initialized || camera.pos == null || context.poseStack() == null || context.submitNodeCollector() == null) {
            return;
        }
        float partial = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        long gameTime = context.levelState().gameTime;
        ACTIVE.removeIf(ritual -> (gameTime - ritual.startGameTime) + partial > MAX_AGE);
        RenderType glow = KiRenderHelper.kiRenderType();
        Vec3 cam = camera.pos;
        for (int i = 0; i < ACTIVE.size(); i++) {
            Ritual ritual = ACTIVE.get(i);
            float age = (gameTime - ritual.startGameTime) + partial;
            if (age < 0.0f || cam.distanceToSqr(ritual.center) > 192.0 * 192.0) {
                continue;
            }
            draw(context, glow, cam, ritual, age);
        }
    }

    private static void draw(LevelRenderContext context, RenderType glow, Vec3 cam, Ritual ritual, float age) {
        PoseStack stack = context.poseStack();
        SubmitNodeCollector collector = context.submitNodeCollector();
        float travelT = Math.min(1.0f, age / DragonWishRitual.FORMATION_TICKS);
        float ease = smooth(travelT);
        double arc = Math.sin(travelT * Math.PI) * 1.7;
        float spin = age > DragonWishRitual.FORMATION_TICKS ? (age - DragonWishRitual.FORMATION_TICKS) * 0.012f : 0.0f;
        float ballAlpha = 1.0f;
        if (age > DragonWishRitual.DRAGON_AT) {
            ballAlpha = 1.0f - smooth((age - DragonWishRitual.DRAGON_AT) / 16.0f);
        }
        Palette palette = ritual.namek ? Palette.NAMEK : Palette.EARTH;
        if (ballAlpha > 0.02f) {
            for (int i = 0; i < DragonWishRitual.BALLS; i++) {
                Vec3 target = DragonWishRitual.ringPoint(ritual.center, ritual.slots[i]);
                if (spin != 0.0f) {
                    target = rotateY(target, ritual.center, spin);
                }
                Vec3 at = lerp(ritual.from[i], target, ease);
                at = new Vec3(at.x, at.y + arc, at.z);
                drawBall(stack, collector, glow, cam, at, palette, ritual.slots[i] + 1, ballAlpha, age);
            }
        }
        if (ease > 0.35f && ballAlpha > 0.02f) {
            float ringAlpha = smooth((ease - 0.35f) / 0.65f) * ballAlpha * 0.85f;
            submitTorus(stack, collector, glow, cam,
                    ritual.center.x, ritual.center.y + DragonWishRitual.RING_LIFT, ritual.center.z,
                    (float) DragonWishRitual.RING_RADIUS, 0.11f, 36, 8,
                    palette.ringR, palette.ringG, palette.ringB, ringAlpha);
        }
        if (age < DragonWishRitual.FORMATION_TICKS) {
            return;
        }
        float summonT = Math.min(1.0f, (age - DragonWishRitual.FORMATION_TICKS) / DragonWishRitual.SUMMON_TICKS);
        float columnFade = age <= DragonWishRitual.DRAGON_AT
                ? 1.0f
                : 1.0f - smooth((age - DragonWishRitual.DRAGON_AT) / (DragonWishRitual.REVEAL_TICKS + 28.0f));
        float fadeIn = smooth(Math.min(1.0f, (age - DragonWishRitual.FORMATION_TICKS) / 12.0f));
        float columnAlpha = fadeIn * Math.max(0.0f, columnFade);
        if (columnAlpha <= 0.02f) {
            return;
        }
        float height = 6.0f + 22.0f * smooth(summonT);
        double groundY = ritual.center.y - 0.5;
        float phase = age * 0.16f;
        drawColumn(stack, collector, glow, cam, ritual.center, groundY, height, phase, columnAlpha, palette);
        float groundRadius = 1.2f + 5.4f * smooth(summonT);
        submitTorus(stack, collector, glow, cam,
                ritual.center.x, groundY + 0.12, ritual.center.z,
                groundRadius, 0.14f, 40, 8,
                palette.ringR, palette.ringG, palette.ringB, columnAlpha * 0.75f);
        if (summonT > 0.2f) {
            float gate = smooth((summonT - 0.2f) / 0.8f) * columnAlpha;
            drawStandingRing(stack, collector, glow, cam, ritual.center, 3.4f + summonT * 1.4f, 0.16f, gate, palette);
        }
        for (int mote = 0; mote < 6; mote++) {
            float ang = phase * 1.35f + mote * ((float) Math.PI * 2.0f / 6.0f);
            float along = (phase * 0.85f + mote * (height / 6.0f)) % height;
            if (along < 0.0f) {
                along += height;
            }
            double mx = ritual.center.x + Math.cos(ang) * 1.45;
            double mz = ritual.center.z + Math.sin(ang) * 1.45;
            submitSphere(stack, collector, glow, cam, mx, groundY + along, mz, 0.16f,
                    palette.ringR, palette.ringG, palette.ringB, columnAlpha);
        }
        float burst = age - DragonWishRitual.DRAGON_AT;
        if (burst >= 0.0f && burst < 18.0f) {
            float u = burst / 18.0f;
            float alpha = (1.0f - u) * 0.9f;
            submitTorus(stack, collector, glow, cam,
                    ritual.center.x, ritual.center.y + 1.2, ritual.center.z,
                    0.8f + u * 8.0f, 0.2f * (1.0f - u) + 0.04f, 36, 8,
                    1.0f, 0.96f, 0.82f, alpha);
            submitSphere(stack, collector, glow, cam,
                    ritual.center.x, ritual.center.y + DragonWishRitual.RING_LIFT * 0.45, ritual.center.z,
                    0.5f + u * 2.4f, 1.0f, 1.0f, 0.92f, alpha * 0.55f);
        }
    }

    private static void drawBall(PoseStack stack, SubmitNodeCollector collector, RenderType glow, Vec3 cam,
                                 Vec3 at, Palette palette, int stars, float alpha, float age) {
        stack.pushPose();
        stack.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        stack.mulPose(Axis.YP.rotation(age * 0.07f));
        float radius = palette.radius;
        collector.submitCustomGeometry(stack, glow, (pose, buffer) -> {
            KiRenderHelper.drawSphere(pose, buffer, radius, 10, 14, palette.bodyR, palette.bodyG, palette.bodyB, alpha);
            KiRenderHelper.drawSphere(pose, buffer, radius * 0.48f, 8, 10, 1.0f, 0.94f, 0.78f, alpha * 0.95f);
        });
        float starR = radius * 0.2f;
        for (int s = 0; s < stars; s++) {
            float ang = s * ((float) Math.PI * 2.0f / stars);
            stack.pushPose();
            stack.translate(Math.cos(ang) * radius * 0.82, radius * 0.36, Math.sin(ang) * radius * 0.82);
            collector.submitCustomGeometry(stack, glow, (pose, buffer) ->
                    KiRenderHelper.drawSphere(pose, buffer, starR, 5, 6, 0.86f, 0.07f, 0.05f, alpha));
            stack.popPose();
        }
        stack.popPose();
    }

    private static void drawColumn(PoseStack stack, SubmitNodeCollector collector, RenderType glow, Vec3 cam,
                                   Vec3 center, double groundY, float height, float phase, float alpha, Palette palette) {
        stack.pushPose();
        stack.translate(center.x - cam.x, groundY - cam.y, center.z - cam.z);
        stack.mulPose(Axis.XP.rotationDegrees(90.0f));
        float shell = 0.9f;
        collector.submitCustomGeometry(stack, glow, (pose, buffer) -> {
            KiRenderHelper.drawCylinder(pose, buffer, shell, 0.0f, height, 20,
                    palette.ringR, palette.ringG, palette.ringB, alpha * 0.38f);
            KiRenderHelper.drawCylinder(pose, buffer, shell * 0.28f, 0.0f, height, 12,
                    1.0f, 0.98f, 0.9f, alpha * 0.8f);
            KiRenderHelper.drawHelix(pose, buffer, shell * 1.2f, 0.0f, height, 4.0f, phase, 0.13f, 64,
                    palette.ringR, palette.ringG, palette.ringB, alpha * 0.9f);
            KiRenderHelper.drawHelix(pose, buffer, shell * 1.2f, 0.0f, height, 4.0f, phase + (float) Math.PI, 0.13f, 64,
                    1.0f, 1.0f, 1.0f, alpha * 0.55f);
        });
        stack.popPose();
    }

    private static void drawStandingRing(PoseStack stack, SubmitNodeCollector collector, RenderType glow, Vec3 cam,
                                         Vec3 center, float radius, float tube, float alpha, Palette palette) {
        stack.pushPose();
        stack.translate(center.x - cam.x, center.y + 2.2 - cam.y, center.z - cam.z);
        stack.mulPose(Axis.XP.rotationDegrees(90.0f));
        collector.submitCustomGeometry(stack, glow, (pose, buffer) ->
                drawTorus(pose, buffer, radius, tube, 36, 8, 1.0f, 0.97f, 0.86f, alpha * 0.7f));
        collector.submitCustomGeometry(stack, glow, (pose, buffer) ->
                drawTorus(pose, buffer, radius * 0.72f, tube * 0.65f, 28, 7,
                        palette.ringR, palette.ringG, palette.ringB, alpha));
        stack.popPose();
    }

    private static void submitTorus(PoseStack stack, SubmitNodeCollector collector, RenderType glow, Vec3 cam,
                                    double x, double y, double z, float ring, float tube, int major, int minor,
                                    float r, float g, float b, float a) {
        stack.pushPose();
        stack.translate(x - cam.x, y - cam.y, z - cam.z);
        collector.submitCustomGeometry(stack, glow, (pose, buffer) ->
                drawTorus(pose, buffer, ring, tube, major, minor, r, g, b, a));
        stack.popPose();
    }

    private static void submitSphere(PoseStack stack, SubmitNodeCollector collector, RenderType glow, Vec3 cam,
                                     double x, double y, double z, float radius,
                                     float r, float g, float b, float a) {
        stack.pushPose();
        stack.translate(x - cam.x, y - cam.y, z - cam.z);
        collector.submitCustomGeometry(stack, glow, (pose, buffer) ->
                KiRenderHelper.drawSphere(pose, buffer, radius, 8, 10, r, g, b, a));
        stack.popPose();
    }

    /**
     * Horizontal torus. Tube radius keeps it a curved volume rather than a flat ring.
     */
    private static void drawTorus(PoseStack.Pose pose, VertexConsumer buffer,
                                  float ring, float tube, int major, int minor,
                                  float r, float g, float b, float a) {
        Matrix4f mat = pose.pose();
        for (int i = 0; i < major; i++) {
            double u0 = (i / (double) major) * Math.PI * 2.0;
            double u1 = ((i + 1) / (double) major) * Math.PI * 2.0;
            float cu0 = (float) Math.cos(u0);
            float su0 = (float) Math.sin(u0);
            float cu1 = (float) Math.cos(u1);
            float su1 = (float) Math.sin(u1);
            for (int j = 0; j < minor; j++) {
                double v0 = (j / (double) minor) * Math.PI * 2.0;
                double v1 = ((j + 1) / (double) minor) * Math.PI * 2.0;
                float cv0 = (float) Math.cos(v0);
                float sv0 = (float) Math.sin(v0);
                float cv1 = (float) Math.cos(v1);
                float sv1 = (float) Math.sin(v1);
                float rad0 = ring + tube * cv0;
                float rad1 = ring + tube * cv1;
                KiRenderHelper.vertex(buffer, mat, pose, cu0 * rad0, tube * sv0, su0 * rad0, r, g, b, a, 0, 0, cu0 * cv0, sv0, su0 * cv0);
                KiRenderHelper.vertex(buffer, mat, pose, cu1 * rad0, tube * sv0, su1 * rad0, r, g, b, a, 1, 0, cu1 * cv0, sv0, su1 * cv0);
                KiRenderHelper.vertex(buffer, mat, pose, cu1 * rad1, tube * sv1, su1 * rad1, r, g, b, a, 1, 1, cu1 * cv1, sv1, su1 * cv1);
                KiRenderHelper.vertex(buffer, mat, pose, cu0 * rad1, tube * sv1, su0 * rad1, r, g, b, a, 0, 1, cu0 * cv1, sv1, su0 * cv1);
            }
        }
    }

    private static Vec3 rotateY(Vec3 point, Vec3 center, float yaw) {
        double dx = point.x - center.x;
        double dz = point.z - center.z;
        double c = Math.cos(yaw);
        double s = Math.sin(yaw);
        return new Vec3(center.x + dx * c - dz * s, point.y, center.z + dx * s + dz * c);
    }

    private static Vec3 lerp(Vec3 from, Vec3 to, float t) {
        return new Vec3(
                from.x + (to.x - from.x) * t,
                from.y + (to.y - from.y) * t,
                from.z + (to.z - from.z) * t
        );
    }

    private static float smooth(float t) {
        float x = Math.max(0.0f, Math.min(1.0f, t));
        return x * x * (3.0f - 2.0f * x);
    }

    private static final class Palette {
        static final Palette EARTH = new Palette(1.0f, 0.46f, 0.06f, 1.0f, 0.80f, 0.25f, 0.38f);
        static final Palette NAMEK = new Palette(0.18f, 0.78f, 0.32f, 0.40f, 1.0f, 0.62f, 0.52f);

        final float bodyR;
        final float bodyG;
        final float bodyB;
        final float ringR;
        final float ringG;
        final float ringB;
        final float radius;

        Palette(float bodyR, float bodyG, float bodyB, float ringR, float ringG, float ringB, float radius) {
            this.bodyR = bodyR;
            this.bodyG = bodyG;
            this.bodyB = bodyB;
            this.ringR = ringR;
            this.ringG = ringG;
            this.ringB = ringB;
            this.radius = radius;
        }
    }

    private static final class Ritual {
        final boolean namek;
        final long startGameTime;
        final Vec3 center;
        final Vec3[] from;
        final int[] slots;

        Ritual(boolean namek, long startGameTime, Vec3 center, Vec3[] from, int[] slots) {
            this.namek = namek;
            this.startGameTime = startGameTime;
            this.center = center;
            this.from = from;
            this.slots = slots;
        }
    }
}
