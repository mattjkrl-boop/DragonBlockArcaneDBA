package com.dragonblockarcanedba.client.wish;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.network.WishRitualStartPayload;
import com.dragonblockarcanedba.util.FxEnvelope;
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
 * Balls travel onto a floating heptagon, then a column, rising ribbons, and
 * rings gather until the dragon appears. Drawn with the shared ki glow type
 * and the bespoke geometry toolkit, seeded from the synced ritual state.
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
        ACTIVE.add(new Ritual(payload.namek(), payload.gameTime(), center, from, slots,
                seedOf(payload.namek(), payload.gameTime(), center)));
    }

    /**
     * Frame-stable shape seed derived only from synced payload state: the
     * ritual start time, the Namek flag, and the center computed from the
     * synced ball positions. Every client that received the payload mixes
     * the same values, so every client draws the same silhouettes.
     */
    private static long seedOf(boolean namek, long gameTime, Vec3 center) {
        long seed = gameTime * 0x9E3779B97F4A7C15L + 0x632BE59BD9B4E019L;
        seed ^= (long) Math.floor(center.x) * 0xBF58476D1CE4E5B9L;
        seed ^= (long) Math.floor(center.y) * 0x2545F4914F6CDD1DL;
        seed ^= (long) Math.floor(center.z) * 0x94D049BB133111EBL;
        if (namek) {
            seed ^= 0x6A09E667F3BCC909L;
        }
        return seed;
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
        float distScale = FxEnvelope.distScale(cam.distanceTo(ritual.center));
        if (ballAlpha > 0.02f) {
            for (int i = 0; i < DragonWishRitual.BALLS; i++) {
                Vec3 target = DragonWishRitual.ringPoint(ritual.center, ritual.slots[i]);
                if (spin != 0.0f) {
                    target = rotateY(target, ritual.center, spin);
                }
                Vec3 at = lerp(ritual.from[i], target, ease);
                at = new Vec3(at.x, at.y + arc, at.z);
                drawBall(stack, collector, glow, cam, at, palette, ritual.slots[i] + 1, ballAlpha, age,
                        ritual.seed + 7919L * (i + 1), distScale);
            }
        }
        if (ease > 0.35f && ballAlpha > 0.02f) {
            float ringAlpha = smooth((ease - 0.35f) / 0.65f) * ballAlpha * 0.85f;
            submitRingBand(stack, collector, glow, cam,
                    ritual.center.x, ritual.center.y + DragonWishRitual.RING_LIFT, ritual.center.z,
                    (float) DragonWishRitual.RING_RADIUS, 0.11f,
                    palette.ringR, palette.ringG, palette.ringB, ringAlpha, ritual.seed + 101L, distScale);
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
        drawColumn(stack, collector, glow, cam, ritual.center, groundY, height, phase, columnAlpha, palette,
                ritual.seed + 606L, distScale);
        float groundRadius = 1.2f + 5.4f * smooth(summonT);
        submitRingBand(stack, collector, glow, cam,
                ritual.center.x, groundY + 0.12, ritual.center.z,
                groundRadius, 0.14f,
                palette.ringR, palette.ringG, palette.ringB, columnAlpha * 0.75f, ritual.seed + 202L, distScale);
        if (summonT > 0.2f) {
            float gate = smooth((summonT - 0.2f) / 0.8f) * columnAlpha;
            drawStandingRing(stack, collector, glow, cam, ritual.center, 3.4f + summonT * 1.4f, 0.16f, gate, palette,
                    ritual.seed + 707L, distScale);
        }
        for (int mote = 0; mote < 6; mote++) {
            float ang = phase * 1.35f + mote * ((float) Math.PI * 2.0f / 6.0f);
            float along = (phase * 0.85f + mote * (height / 6.0f)) % height;
            if (along < 0.0f) {
                along += height;
            }
            double mx = ritual.center.x + Math.cos(ang) * 1.45;
            double mz = ritual.center.z + Math.sin(ang) * 1.45;
            submitMote(stack, collector, glow, cam, mx, groundY + along, mz, 0.16f,
                    palette.ringR, palette.ringG, palette.ringB, columnAlpha,
                    ritual.seed + 404L + mote * 17L, distScale);
        }
        float burst = age - DragonWishRitual.DRAGON_AT;
        if (burst >= 0.0f && burst < 18.0f) {
            float u = burst / 18.0f;
            float alpha = (1.0f - u) * 0.9f;
            submitRingBand(stack, collector, glow, cam,
                    ritual.center.x, ritual.center.y + 1.2, ritual.center.z,
                    0.8f + u * 8.0f, 0.2f * (1.0f - u) + 0.04f,
                    1.0f, 0.96f, 0.82f, alpha, ritual.seed + 303L, distScale);
            submitDomePairAt(stack, collector, glow, cam,
                    ritual.center.x, ritual.center.y + DragonWishRitual.RING_LIFT * 0.45, ritual.center.z,
                    0.5f + u * 2.4f, 1.0f, 1.0f, 0.92f, alpha * 0.55f, ritual.seed + 505L, distScale);
        }
    }

    private static void drawBall(PoseStack stack, SubmitNodeCollector collector, RenderType glow, Vec3 cam,
                                 Vec3 at, Palette palette, int stars, float alpha, float age,
                                 long seed, float distScale) {
        stack.pushPose();
        stack.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        stack.mulPose(new Matrix4f().rotation(Axis.YP.rotation(age * 0.07f)));
        float radius = palette.radius;
        // Body and bright core: displaced dome pairs, so the ball is a lumpy
        // seeded shell instead of a smooth primitive sphere.
        submitDomePair(stack, collector, glow, seed, radius,
                palette.bodyR, palette.bodyG, palette.bodyB, alpha, distScale);
        submitDomePair(stack, collector, glow, seed + 31L, radius * 0.48f,
                1.0f, 0.94f, 0.78f, alpha * 0.95f, distScale);
        // Star pips: a small shard fan seated on the surface at each pip spot,
        // where the little red pip spheres sat.
        float starR = radius * 0.2f;
        for (int s = 0; s < stars; s++) {
            float ang = s * ((float) Math.PI * 2.0f / stars);
            long pipSeed = seed + 131L * (s + 1);
            stack.pushPose();
            stack.translate(Math.cos(ang) * radius * 0.82, radius * 0.36, Math.sin(ang) * radius * 0.82);
            stack.mulPose(new Matrix4f().rotation(Axis.YP.rotation(-ang)));
            stack.mulPose(new Matrix4f().rotation(Axis.XP.rotationDegrees(-58.0f)));
            collector.submitCustomGeometry(stack, glow, (pose, buffer) ->
                    BespokeGeometry.shardFan(pose, buffer, pipSeed, starR * 2.4f,
                            0.86f, 0.07f, 0.05f, alpha, 1.0f, distScale));
            stack.popPose();
        }
        stack.popPose();
    }

    /**
     * A full ball shell from two displaced domes, one up and one flipped.
     * Pose glue: the pose must already be positioned at the shell center.
     */
    private static void submitDomePair(PoseStack stack, SubmitNodeCollector collector, RenderType glow,
                                       long seed, float radius, float r, float g, float b, float alpha,
                                       float distScale) {
        collector.submitCustomGeometry(stack, glow, (pose, buffer) ->
                BespokeGeometry.displacedDome(pose, buffer, seed, radius, r, g, b, alpha, 1.0f, distScale));
        stack.pushPose();
        stack.mulPose(new Matrix4f().rotation(Axis.XP.rotationDegrees(180.0f)));
        collector.submitCustomGeometry(stack, glow, (pose, buffer) ->
                BespokeGeometry.displacedDome(pose, buffer, seed + 7L, radius, r, g, b, alpha, 1.0f, distScale));
        stack.popPose();
    }

    /** A dome pair shell centered on a world point (the arrival burst). */
    private static void submitDomePairAt(PoseStack stack, SubmitNodeCollector collector, RenderType glow, Vec3 cam,
                                         double x, double y, double z, float radius,
                                         float r, float g, float b, float a, long seed, float distScale) {
        stack.pushPose();
        stack.translate(x - cam.x, y - cam.y, z - cam.z);
        submitDomePair(stack, collector, glow, seed, radius, r, g, b, a, distScale);
        stack.popPose();
    }

    private static void drawColumn(PoseStack stack, SubmitNodeCollector collector, RenderType glow, Vec3 cam,
                                   Vec3 center, double groundY, float height, float phase, float alpha, Palette palette,
                                   long seed, float distScale) {
        stack.pushPose();
        stack.translate(center.x - cam.x, groundY - cam.y, center.z - cam.z);
        float shell = 0.9f;
        collector.submitCustomGeometry(stack, glow, (pose, buffer) -> {
            // Volumetric core of rising light where the bright core cylinder ran.
            BespokeGeometry.plume(pose, buffer, seed, shell * 0.62f, height,
                    palette.ringR, palette.ringG, palette.ringB, alpha * 0.5f, 1.0f, distScale);
            // Broken bands climb the column in two shells, turning with the
            // phase where the smooth outer cylinder stood.
            BespokeGeometry.brokenVortexBand(pose, buffer, seed + 11L, shell * 1.05f, 0.0f, height * 0.55f, phase * 0.5f,
                    palette.ringR, palette.ringG, palette.ringB, alpha * 0.38f, distScale);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed + 23L, shell * 1.0f, height * 0.45f, height, phase * 0.65f,
                    palette.ringR, palette.ringG, palette.ringB, alpha * 0.3f, distScale);
            // Bright heart band where the thin core cylinder ran.
            BespokeGeometry.brokenVortexBand(pose, buffer, seed + 37L, shell * 0.3f, 0.0f, height, -phase * 0.35f,
                    1.0f, 0.98f, 0.9f, alpha * 0.8f, distScale);
            // Twin rising ribbons where the helix pair wound.
            drawHelixRibbon(pose, buffer, seed + 41L, shell * 1.2f, height, phase, 0.2f,
                    palette.ringR, palette.ringG, palette.ringB, alpha * 0.9f, distScale);
            drawHelixRibbon(pose, buffer, seed + 53L, shell * 1.2f, height, phase + (float) Math.PI, 0.2f,
                    1.0f, 1.0f, 1.0f, alpha * 0.55f, distScale);
        });
        stack.popPose();
    }

    /**
     * Pose glue only: two branch spans chained into one rising wrap of about
     * 1.2 turns, standing in for one helix. Emits no vertices itself.
     */
    private static void drawHelixRibbon(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                                        float radius, float height, float phase, float width,
                                        float r, float g, float b, float alpha, float distScale) {
        float wrap = (float) Math.PI * 1.2f;
        Vec3 p0 = new Vec3(Math.cos(phase) * radius, 0.0, Math.sin(phase) * radius);
        Vec3 p1 = new Vec3(Math.cos(phase + wrap) * radius, height * 0.5f, Math.sin(phase + wrap) * radius);
        Vec3 p2 = new Vec3(Math.cos(phase + wrap * 2.0f) * radius, height, Math.sin(phase + wrap * 2.0f) * radius);
        BespokeGeometry.branch(pose, buffer, seed, p0, p1, width, r, g, b, alpha, 1.0f, distScale);
        BespokeGeometry.branch(pose, buffer, seed + 3L, p1, p2, width, r, g, b, alpha, 1.0f, distScale);
    }

    private static void drawStandingRing(PoseStack stack, SubmitNodeCollector collector, RenderType glow, Vec3 cam,
                                         Vec3 center, float radius, float tube, float alpha, Palette palette,
                                         long seed, float distScale) {
        stack.pushPose();
        stack.translate(center.x - cam.x, center.y + 2.2 - cam.y, center.z - cam.z);
        stack.mulPose(new Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(stack, glow, (pose, buffer) ->
                ringBandPair(pose, buffer, seed, radius, tube, 1.0f, 0.97f, 0.86f, alpha * 0.7f, distScale));
        collector.submitCustomGeometry(stack, glow, (pose, buffer) ->
                ringBandPair(pose, buffer, seed + 17L, radius * 0.72f, tube * 0.65f,
                        palette.ringR, palette.ringG, palette.ringB, alpha, distScale));
        stack.popPose();
    }

    private static void submitRingBand(PoseStack stack, SubmitNodeCollector collector, RenderType glow, Vec3 cam,
                                       double x, double y, double z, float ring, float tube,
                                       float r, float g, float b, float a, long seed, float distScale) {
        stack.pushPose();
        stack.translate(x - cam.x, y - cam.y, z - cam.z);
        collector.submitCustomGeometry(stack, glow, (pose, buffer) ->
                ringBandPair(pose, buffer, seed, ring, tube, r, g, b, a, distScale));
        stack.popPose();
    }

    /**
     * Pose glue only: a ring as two broken bands, one on each side of the
     * ring plane, standing in for the old torus tube. The band local Y is
     * the ring's thickness axis. Emits no vertices itself.
     */
    private static void ringBandPair(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                                     float ring, float tube, float r, float g, float b, float a,
                                     float distScale) {
        BespokeGeometry.brokenVortexBand(pose, buffer, seed, ring, -tube, 0.0f, 0.0f, r, g, b, a, distScale);
        BespokeGeometry.brokenVortexBand(pose, buffer, seed + 5L, ring, 0.0f, tube, 0.35f, r, g, b, a * 0.9f, distScale);
    }

    /** A rising mote of light: a small plume blob centered on the mote point. */
    private static void submitMote(PoseStack stack, SubmitNodeCollector collector, RenderType glow, Vec3 cam,
                                   double x, double y, double z, float radius,
                                   float r, float g, float b, float a, long seed, float distScale) {
        stack.pushPose();
        stack.translate(x - cam.x, y - radius - cam.y, z - cam.z);
        collector.submitCustomGeometry(stack, glow, (pose, buffer) ->
                BespokeGeometry.plume(pose, buffer, seed, radius * 1.15f, radius * 2.0f, r, g, b, a, 1.0f, distScale));
        stack.popPose();
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
        final long seed;

        Ritual(boolean namek, long startGameTime, Vec3 center, Vec3[] from, int[] slots, long seed) {
            this.namek = namek;
            this.startGameTime = startGameTime;
            this.center = center;
            this.from = from;
            this.slots = slots;
            this.seed = seed;
        }
    }
}
