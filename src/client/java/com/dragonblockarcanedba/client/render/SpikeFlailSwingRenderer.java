package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.SpikeFlailSwingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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

/**
 * Chain and spiked ball at the end of the swing. The entity sits on the ball.
 * All geometry comes from BespokeGeometry (BESPOKE-STANDARD rule 1): the chain
 * is torn ribbon spans with alternating link roll, the ball is two displaced
 * domes with shard fans for spikes, and the swing leaves a torn ribbon trail
 * along the ball's arc. Seeds derive from the entity id, so every client
 * draws the same shapes.
 */
public class SpikeFlailSwingRenderer extends EntityRenderer<SpikeFlailSwingEntity, SpikeFlailSwingRenderer.SwingState> {
    /** Trail samples behind the ball along its arc, oldest first; the last is the ball itself. */
    private static final int TRAIL_POINTS = 5;

    public SpikeFlailSwingRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SwingState extends EntityRenderState {
        public float age;
        public float handX;
        public float handY;
        public float handZ;
        public boolean hasHand;
        public long seed;
        public final float[] trailX = new float[TRAIL_POINTS];
        public final float[] trailY = new float[TRAIL_POINTS];
        public final float[] trailZ = new float[TRAIL_POINTS];
        public boolean hasTrail;
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
        state.seed = entity.getId() * 2654435761L ^ 0x5F1A11L;
        Entity owner = entity.getOwner();
        state.hasHand = owner != null;
        state.hasTrail = false;
        if (owner != null) {
            Vec3 ball = entity.getPosition(partialTick);
            float yaw = entity.swingYaw();
            double yawRad = Math.toRadians(yaw);
            Vec3 hand = owner.getPosition(partialTick).add(-Math.sin(yawRad) * 0.35, 1.05, Math.cos(yawRad) * 0.35);
            Vec3 rel = hand.subtract(ball);
            state.handX = (float) rel.x;
            state.handY = (float) rel.y;
            state.handZ = (float) rel.z;
            // Arc samples behind the ball, relative to the ball's current arc point,
            // so the trail ends exactly where the ball is drawn.
            float cur = Mth.clamp((entity.tickCount + partialTick) / (float) SpikeFlailSwingEntity.SWING_TICKS, 0.0f, 1.0f);
            Vec3 now = SpikeFlailSwingEntity.ballPos(owner, yaw, cur);
            for (int i = 0; i < TRAIL_POINTS; i++) {
                float p = Math.max(0.0f, cur - (TRAIL_POINTS - 1 - i) * 0.09f);
                Vec3 s = SpikeFlailSwingEntity.ballPos(owner, yaw, p);
                state.trailX[i] = (float) (s.x - now.x);
                state.trailY[i] = (float) (s.y - now.y);
                state.trailZ[i] = (float) (s.z - now.z);
            }
            state.hasTrail = true;
        }
    }

    @Override
    public void submit(SwingState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float fade = ease(Math.max(0.0f, 1.0f - Math.max(0.0f, state.age - 8.0f) / 3.0f));
        float open = ease(Math.min(1.0f, state.age / 2.0f));
        RenderType type = KiRenderHelper.kiRenderType();
        float distScale = LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        if (state.hasTrail && fade > 0.01f) {
            float[] widths = {0.20f, 0.28f, 0.36f, 0.44f};
            for (int i = 0; i < TRAIL_POINTS - 1; i++) {
                submitSpan(poseStack, collector, type,
                    state.trailX[i], state.trailY[i], state.trailZ[i],
                    state.trailX[i + 1], state.trailY[i + 1], state.trailZ[i + 1],
                    widths[i], 0.0f, 0.92f, 0.90f, 0.82f, fade * (0.10f + 0.07f * i),
                    state.seed ^ (0x7EA1L + i), distScale);
            }
        }
        if (state.hasHand) {
            submitChain(poseStack, collector, type, state.handX, state.handY, state.handZ,
                0.0f, 0.0f, 0.0f, fade, state.seed ^ 0xC4A1L, distScale);
        }
        submitBall(poseStack, collector, type, 0.42f * (0.75f + 0.25f * open), fade,
            state.seed ^ 0xBA11L, distScale);
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float ease(float t) {
        float c = Mth.clamp(t, 0.0f, 1.0f);
        return c * c * (3.0f - 2.0f * c);
    }

    /**
     * One torn ribbon span from one point to another: the pose is translated to
     * the start and yawed, pitched, and rolled so the ribbon's length axis lies
     * along the span. The shared strand for chain links and trail segments.
     */
    static void submitSpan(PoseStack poseStack, SubmitNodeCollector collector, RenderType type,
                           float x0, float y0, float z0, float x1, float y1, float z1,
                           float width, float rollDeg,
                           float r, float g, float b, float alpha, long seed, float distScale) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001f || alpha <= 0.01f) {
            return;
        }
        float horiz = (float) Math.sqrt(dx * dx + dz * dz);
        float yawDeg = (float) Math.toDegrees(Math.atan2(-dz, dx));
        float pitchDeg = (float) Math.toDegrees(Math.atan2(dy, horiz));
        poseStack.pushPose();
        poseStack.translate(x0, y0, z0);
        poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(yawDeg)));
        poseStack.mulPose(new Matrix4f().rotation(Axis.ZP.rotationDegrees(pitchDeg)));
        if (rollDeg != 0.0f) {
            poseStack.mulPose(new Matrix4f().rotation(Axis.XP.rotationDegrees(rollDeg)));
        }
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) ->
            BespokeGeometry.tornRibbon(pose, buffer, seed, len, width, 0.0f, r, g, b, alpha, 1.0f, distScale));
        poseStack.popPose();
    }

    /**
     * The chain between two points: eight ribbon spans along the same sag curve
     * the old prism links followed, with alternating width and roll so the
     * links read as alternating chain links instead of a smooth tube.
     */
    static void submitChain(PoseStack poseStack, SubmitNodeCollector collector, RenderType type,
                            float hx, float hy, float hz, float bx, float by, float bz,
                            float fade, long seed, float distScale) {
        int links = 8;
        float px = hx;
        float py = hy;
        float pz = hz;
        for (int i = 1; i <= links; i++) {
            float t = i / (float) links;
            float sag = (float) Math.sin(t * Math.PI) * 0.45f;
            float x = Mth.lerp(t, hx, bx);
            float y = Mth.lerp(t, hy, by) - sag;
            float z = Mth.lerp(t, hz, bz);
            float width = i % 2 == 0 ? 0.20f : 0.13f;
            float roll = i % 2 == 0 ? 75.0f : 0.0f;
            submitSpan(poseStack, collector, type, px, py, pz, x, y, z, width, roll,
                0.72f, 0.74f, 0.78f, fade, seed + i * 131L, distScale);
            px = x;
            py = y;
            pz = z;
        }
    }

    /**
     * The spiked ball: two noise-displaced domes make the lumpy iron body and
     * shard fans in three planes make the spikes, all seeded. No spheres, no
     * cones, no lat-long grids.
     */
    static void submitBall(PoseStack poseStack, SubmitNodeCollector collector, RenderType type,
                           float radius, float fade, long seed, float distScale) {
        if (fade <= 0.01f) {
            return;
        }
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) ->
            BespokeGeometry.displacedDome(pose, buffer, seed, radius,
                0.78f, 0.80f, 0.84f, fade * 0.95f, 1.0f, distScale));
        poseStack.pushPose();
        poseStack.mulPose(new Matrix4f().rotation(Axis.XP.rotationDegrees(180.0f)));
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) ->
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xD0EL, radius,
                0.78f, 0.80f, 0.84f, fade * 0.95f, 1.0f, distScale));
        poseStack.popPose();
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) ->
            BespokeGeometry.shardFan(pose, buffer, seed ^ 0x11L, radius * 1.5f,
                0.85f, 0.87f, 0.91f, fade * 0.9f, 1.0f, distScale));
        poseStack.pushPose();
        poseStack.mulPose(new Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) ->
            BespokeGeometry.shardFan(pose, buffer, seed ^ 0x22L, radius * 1.5f,
                0.85f, 0.87f, 0.91f, fade * 0.9f, 1.0f, distScale));
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.mulPose(new Matrix4f().rotation(Axis.ZP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) ->
            BespokeGeometry.shardFan(pose, buffer, seed ^ 0x33L, radius * 1.5f,
                0.85f, 0.87f, 0.91f, fade * 0.9f, 1.0f, distScale));
        poseStack.popPose();
    }
}
