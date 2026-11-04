package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.VoidRiftEntity;
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

/**
 * Hollow's Edge void rift. One dimensional tear facing the viewer: a recessed
 * black throat, bowed violet lips, a white rim with a short wake, and crystal
 * shards along the wound. The implosion closes that mouth on the entity's
 * staged 30 tick collapse instead of popping a disc. Spectacle pass: the tear
 * now fades in on the shared envelope instead of popping in at full alpha,
 * sits in a halo disc of void light, and carries three rotating accretion
 * bands (the layered band language of LayeredRender, drawn in the tear
 * plane), with segment counts and alpha stepped down by camera distance.
 */
public class VoidRiftRenderer extends EntityRenderer<VoidRiftEntity, VoidRiftRenderer.RiftRenderState> {

    private static final int SEGMENTS = 16;
    private static final int SHARDS = 7;
    private static final int CRYSTAL_SIDES = 6;

    public VoidRiftRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class RiftRenderState extends EntityRenderState {
        public float radius = 2.5f;
        public boolean isImploding = false;
        public float age = 0;
        public int implodeStartTick = -1;
        public double ex, ey, ez;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(VoidRiftEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(VoidRiftEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public RiftRenderState createRenderState() {
        return new RiftRenderState();
    }

    @Override
    public void extractRenderState(VoidRiftEntity entity, RiftRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.isImploding = entity.isImploding();
        state.implodeStartTick = entity.getImplodeStartTick();
        state.age = entity.tickCount + partialTicks;
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
    }

    @Override
    public void submit(RiftRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        float radius = state.radius;
        boolean imploding = state.isImploding;
        float age = state.age;
        // Collapse is measured from the synced trigger tick. The entity discards
        // itself after the staged collapse window, so the draw has to finish
        // inside it. The window length comes from the entity so both sides agree.
        float implodeWindow = VoidRiftEntity.IMPLOSION_TICKS;
        float implodeAge = (imploding && state.implodeStartTick >= 0)
            ? Math.min(implodeWindow, Math.max(0.0f, age - state.implodeStartTick)) : 0.0f;
        float implodeT = implodeAge / implodeWindow;

        // Lifecycle envelope (point 5): the tear fades in over its first 20
        // ticks instead of popping in at full alpha. During the collapse the
        // staged fades inside drawTear take over, so the envelope reads 1.
        float env = imploding ? 1.0f : ease(Math.min(1.0f, age / 20.0f));
        // Distance scaling (point 1): far rifts draw fewer segments and
        // slightly softer alpha instead of costing full geometry.
        float ds = LayeredRender.distScale(cameraState, state.ex, state.ey, state.ez);
        env *= (float) Math.sqrt(Math.max(0.05f, ds));
        int segments = FxEnvelope.segments(SEGMENTS, ds, 10);

        poseStack.pushPose();
        // The rift entity has no synced look direction. Camera.rotation() is copied
        // into orientation, and particles face the viewer with that quaternion.
        // Positive local Z then points back at the camera, so the throat can sit
        // behind the rim. A world-fixed sheet would vanish whenever the player
        // looked along it.
        if (cameraState.orientation != null) {
            poseStack.mulPose(new org.joml.Quaternionf(cameraState.orientation));
        }

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            drawTear(pose.pose(), buffer, radius, age, imploding, implodeT, env, ds, segments));

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static float ease(float t) {
        float c = Math.max(0.0f, Math.min(1.0f, t));
        return c * c * (3.0f - 2.0f * c);
    }

    private static void drawTear(Matrix4f matrix, VertexConsumer buffer, float radius, float age, boolean imploding, float implodeT, float env, float distScale, int segments) {
        float eased = imploding ? ease(implodeT) : 0.0f;
        float open = 1.0f - eased;
        float pulse = 0.94f + 0.06f * (float) Math.sin(age * (imploding ? 0.9f : 0.28f));
        float halfH = radius * 1.12f * (imploding ? (1.0f - 0.82f * eased) : 1.0f);
        float belly = radius * 0.56f * pulse;
        float bowAmp = radius * 0.24f * open;
        float depthBase = radius * 0.28f;
        // The curl peaks halfway through the collapse and is gone when the seam dies,
        // so the ending is a closing cut rather than a spinning leftover.
        float twistEnv = imploding ? (float) Math.sin(implodeT * Math.PI) : 1.0f;
        float twist = (float) Math.toRadians(imploding ? 42.0 * twistEnv : 5.0 * Math.sin(age * 0.13));

        float bodyA = (imploding ? (1.0f - ease((implodeT - 0.30f) / 0.48f)) : 1.0f) * env;
        float rimA = (imploding ? (1.0f - ease((implodeT - 0.50f) / 0.36f)) : 1.0f) * env;
        float seamA = (imploding
            ? (0.55f + 0.45f * eased) * (1.0f - ease((implodeT - 0.74f) / 0.26f))
            : (0.58f + 0.12f * (float) Math.sin(age * 0.28f))) * env;
        float shardA = (imploding ? (1.0f - ease(implodeT / 0.70f)) : 0.94f) * env;

        // Layered depth behind and around the tear (point 1): a halo disc of
        // void light behind the throat, and three accretion bands circling
        // the wound at different radii, speeds, and directions. Both die
        // with the collapse so nothing outlives the mouth.
        float fade = imploding ? (1.0f - implodeT) : env;
        if (fade > 0.02f) {
            drawHalo(matrix, buffer, radius * 1.55f, -depthBase * 1.4f,
                0.45f, 0.08f, 0.85f, 0.30f * fade);
            float bandScale = imploding ? (1.0f - 0.55f * eased) : 1.0f;
            drawRingBand(matrix, buffer, radius * 1.06f * bandScale, radius * 0.055f, -depthBase * 0.5f,
                age * 0.055f, 4.4f, 0.80f, 0.45f, 1.0f, LayeredRender.ALPHA_MID * fade, distScale);
            drawRingBand(matrix, buffer, radius * 1.24f * bandScale, radius * 0.075f, -depthBase * 0.7f,
                -age * 0.038f + 2.1f, 3.6f, 0.55f, 0.10f, 0.92f, LayeredRender.ALPHA_OUTER * 1.5f * fade, distScale);
            drawRingBand(matrix, buffer, radius * 0.90f * bandScale, radius * 0.04f, -depthBase * 0.3f,
                age * 0.09f + 4.0f, 5.1f, 0.92f, 0.70f, 1.0f, LayeredRender.ALPHA_CORE * 0.45f * fade, distScale);
        }

        Cut low = new Cut();
        Cut high = new Cut();
        fillCut(low, 0.0f, radius, age, open, halfH, belly, bowAmp, depthBase, twist);
        for (int i = 0; i < segments; i++) {
            float t1 = (i + 1) / (float) segments;
            fillCut(high, t1, radius, age, open, halfH, belly, bowAmp, depthBase, twist);
            if (low.width > 0.02f || high.width > 0.02f) {
                drawMouth(matrix, buffer, low, high, bodyA, rimA);
            }
            if (seamA > 0.03f) {
                float a0 = seamA * (0.40f + 0.60f * low.pinch);
                float a1 = seamA * (0.40f + 0.60f * high.pinch);
                along(buffer, matrix,
                    low.seamLX, low.seamLY, low.seamLZ,
                    low.seamRX, low.seamRY, low.seamRZ,
                    high.seamRX, high.seamRY, high.seamRZ,
                    high.seamLX, high.seamLY, high.seamLZ,
                    1.0f, 1.0f, 1.0f, a0,
                    1.0f, 1.0f, 1.0f, a1);
            }
            Cut swap = low;
            low = high;
            high = swap;
        }

        if (shardA > 0.04f && open > 0.04f) {
            Cut shardCut = new Cut();
            for (int i = 0; i < SHARDS; i++) {
                float t = 0.18f + 0.64f * (i / (float) (SHARDS - 1));
                boolean left = (i & 1) == 0;
                fillCut(shardCut, t, radius, age, open, halfH, belly, bowAmp, depthBase, twist);
                drawShard(matrix, buffer, shardCut, left, radius, age, i, eased, shardA);
            }
        }
    }

    /** Soft disc of void light behind the throat, alpha dying to the rim. Both windings. */
    private static void drawHalo(Matrix4f matrix, VertexConsumer buffer, float haloR, float z,
                                 float r, float g, float b, float alpha) {
        if (alpha <= 0.01f || haloR <= 0.05f) return;
        int segs = 20;
        for (int i = 0; i < segs; i++) {
            double a0 = (i / (double) segs) * Math.PI * 2.0;
            double a1 = ((i + 1) / (double) segs) * Math.PI * 2.0;
            float x0 = (float) Math.cos(a0) * haloR, y0 = (float) Math.sin(a0) * haloR;
            float x1 = (float) Math.cos(a1) * haloR, y1 = (float) Math.sin(a1) * haloR;
            vert(buffer, matrix, 0, 0, z, 0.5f, 0.5f, r, g, b, alpha);
            vert(buffer, matrix, x0, y0, z, 0, 0, r, g, b, 0.0f);
            vert(buffer, matrix, x1, y1, z, 1, 0, r, g, b, 0.0f);
            vert(buffer, matrix, 0, 0, z, 0.5f, 0.5f, r, g, b, alpha);
            vert(buffer, matrix, x1, y1, z, 1, 0, r, g, b, 0.0f);
            vert(buffer, matrix, x0, y0, z, 0, 0, r, g, b, 0.0f);
        }
    }

    /**
     * One accretion arc: a flat band segment circling the tear in its own
     * plane, ends tapered so the arc dissolves instead of cutting off.
     * Drawn both windings, like the rest of the tear.
     */
    private static void drawRingBand(Matrix4f matrix, VertexConsumer buffer,
                                     float ringR, float width, float z,
                                     float startAngle, float arc,
                                     float r, float g, float b, float alpha, float distScale) {
        if (alpha <= 0.01f || ringR <= 0.05f) return;
        int segs = FxEnvelope.segments(22, distScale, 8);
        float rIn = ringR - width * 0.5f;
        float rOut = ringR + width * 0.5f;
        for (int i = 0; i < segs; i++) {
            float t0 = i / (float) segs;
            float t1 = (i + 1) / (float) segs;
            float a0 = startAngle + t0 * arc;
            float a1 = startAngle + t1 * arc;
            float taper0 = Math.min(1.0f, Math.min(t0, 1.0f - t0) * 6.0f);
            float taper1 = Math.min(1.0f, Math.min(t1, 1.0f - t1) * 6.0f);
            float al0 = alpha * taper0;
            float al1 = alpha * taper1;
            float ix0 = (float) Math.cos(a0) * rIn, iy0 = (float) Math.sin(a0) * rIn;
            float ox0 = (float) Math.cos(a0) * rOut, oy0 = (float) Math.sin(a0) * rOut;
            float ix1 = (float) Math.cos(a1) * rIn, iy1 = (float) Math.sin(a1) * rIn;
            float ox1 = (float) Math.cos(a1) * rOut, oy1 = (float) Math.sin(a1) * rOut;
            vert(buffer, matrix, ix0, iy0, z, 0, 0, r, g, b, al0);
            vert(buffer, matrix, ox0, oy0, z, 0, 0, r, g, b, al0);
            vert(buffer, matrix, ox1, oy1, z, 0, 0, r, g, b, al1);
            vert(buffer, matrix, ix1, iy1, z, 0, 0, r, g, b, al1);
            vert(buffer, matrix, ix1, iy1, z, 0, 0, r, g, b, al1);
            vert(buffer, matrix, ox1, oy1, z, 0, 0, r, g, b, al1);
            vert(buffer, matrix, ox0, oy0, z, 0, 0, r, g, b, al0);
            vert(buffer, matrix, ix0, iy0, z, 0, 0, r, g, b, al0);
        }
    }

    /**
     * One height sample of the tear. Width pinches at the tips, the edge is
     * ragged, and the white lip sits toward the camera with the throat behind it.
     */
    private static void fillCut(Cut c, float t, float radius, float age, float open, float halfH, float belly, float bowAmp, float depthBase, float twist) {
        float pinch = (float) Math.sin(t * Math.PI);
        float y = (t - 0.5f) * 2.0f * halfH;
        float shaped = (float) Math.pow(Math.max(pinch, 0.0f), 0.55f + 1.35f * (1.0f - open));
        float halfL = belly * open * shaped * rag(t, age, -1.0f, open);
        float halfR = belly * open * shaped * rag(t, age, 1.0f, open);
        halfL *= 1.0f - 0.22f * bite(t, 0.36f) * open;
        halfR *= 1.0f - 0.28f * bite(t, 0.63f) * open;

        float crook = (float) (radius * 0.07 * Math.sin(t * Math.PI * 2.0 + age * 0.18)) * (0.35f + 0.65f * open);
        float depth = depthBase * pinch * (0.45f + 0.55f * open);
        float zLip = bowAmp * pinch;
        float zInner = zLip - depth;
        float zThroat = zInner - depth * 0.65f;
        zThroat -= (float) Math.sin(t * 6.2832f + age * 0.40f) * radius * 0.015f * pinch * open;
        float zBevel = zLip - depth * 0.42f;
        float zSeam = zThroat + (zLip - zThroat) * 0.55f;
        float seamHalf = radius * 0.018f * (0.15f + 0.85f * pinch);

        float ang = (t - 0.5f) * twist;
        float cos = (float) Math.cos(ang);
        float sin = (float) Math.sin(ang);

        c.width = halfL + halfR;
        c.pinch = pinch;
        put(c, crook, y, zThroat, cos, sin, 0);
        put(c, crook - halfL * 0.68f, y, zInner, cos, sin, 1);
        put(c, crook + halfR * 0.68f, y, zInner, cos, sin, 2);
        put(c, crook - halfL, y, zLip, cos, sin, 3);
        put(c, crook + halfR, y, zLip, cos, sin, 4);
        put(c, crook - halfL * 1.16f, y, zBevel, cos, sin, 5);
        put(c, crook + halfR * 1.16f, y, zBevel, cos, sin, 6);
        put(c, crook - halfL * 1.34f, y, zLip - depth * 0.12f, cos, sin, 7);
        put(c, crook + halfR * 1.34f, y, zLip - depth * 0.12f, cos, sin, 8);
        put(c, crook - seamHalf, y, zSeam, cos, sin, 9);
        put(c, crook + seamHalf, y, zSeam, cos, sin, 10);
    }

    /** slot: 0 throat, 1 innerL, 2 innerR, 3 outerL, 4 outerR, 5 flareL, 6 flareR, 7 wakeL, 8 wakeR, 9 seamL, 10 seamR */
    private static void put(Cut c, float x, float y, float z, float cos, float sin, int slot) {
        float xr = x * cos - y * sin;
        float yr = x * sin + y * cos;
        switch (slot) {
            case 0 -> { c.throatX = xr; c.throatY = yr; c.throatZ = z; }
            case 1 -> { c.innerLX = xr; c.innerLY = yr; c.innerLZ = z; }
            case 2 -> { c.innerRX = xr; c.innerRY = yr; c.innerRZ = z; }
            case 3 -> { c.outerLX = xr; c.outerLY = yr; c.outerLZ = z; }
            case 4 -> { c.outerRX = xr; c.outerRY = yr; c.outerRZ = z; }
            case 5 -> { c.flareLX = xr; c.flareLY = yr; c.flareLZ = z; }
            case 6 -> { c.flareRX = xr; c.flareRY = yr; c.flareRZ = z; }
            case 7 -> { c.wakeLX = xr; c.wakeLY = yr; c.wakeLZ = z; }
            case 8 -> { c.wakeRX = xr; c.wakeRY = yr; c.wakeRZ = z; }
            case 9 -> { c.seamLX = xr; c.seamLY = yr; c.seamLZ = z; }
            default -> { c.seamRX = xr; c.seamRY = yr; c.seamRZ = z; }
        }
    }

    private static float rag(float t, float age, float side, float open) {
        float n = (float) Math.sin(t * Math.PI * 3.0 + side * 1.3 + age * 0.31);
        float n2 = (float) Math.sin(t * Math.PI * 8.0 - side * 0.6 + age * 0.19);
        float torn = 0.86f + 0.10f * n + 0.06f * n2;
        torn = Math.max(0.72f, Math.min(1.08f, torn));
        return 1.0f + (torn - 1.0f) * (0.35f + 0.65f * open);
    }

    private static float bite(float t, float at) {
        float d = t - at;
        return (float) Math.exp(-(d * d) / 0.007f);
    }

    private static void drawMouth(Matrix4f matrix, VertexConsumer buffer, Cut a, Cut b, float bodyA, float rimA) {
        if (bodyA > 0.02f) {
            across(buffer, matrix,
                a.innerLX, a.innerLY, a.innerLZ,
                a.throatX, a.throatY, a.throatZ,
                b.throatX, b.throatY, b.throatZ,
                b.innerLX, b.innerLY, b.innerLZ,
                0.14f, 0.02f, 0.24f, 0.96f * bodyA,
                0.02f, 0.00f, 0.05f, 0.98f * bodyA);
            across(buffer, matrix,
                a.throatX, a.throatY, a.throatZ,
                a.innerRX, a.innerRY, a.innerRZ,
                b.innerRX, b.innerRY, b.innerRZ,
                b.throatX, b.throatY, b.throatZ,
                0.02f, 0.00f, 0.05f, 0.98f * bodyA,
                0.14f, 0.02f, 0.24f, 0.96f * bodyA);
        }
        if (bodyA > 0.02f || rimA > 0.02f) {
            across(buffer, matrix,
                a.innerLX, a.innerLY, a.innerLZ,
                a.outerLX, a.outerLY, a.outerLZ,
                b.outerLX, b.outerLY, b.outerLZ,
                b.innerLX, b.innerLY, b.innerLZ,
                0.50f, 0.05f, 0.88f, 0.90f * bodyA,
                1.0f, 1.0f, 1.0f, rimA);
            across(buffer, matrix,
                a.outerRX, a.outerRY, a.outerRZ,
                a.innerRX, a.innerRY, a.innerRZ,
                b.innerRX, b.innerRY, b.innerRZ,
                b.outerRX, b.outerRY, b.outerRZ,
                1.0f, 1.0f, 1.0f, rimA,
                0.50f, 0.05f, 0.88f, 0.90f * bodyA);
        }
        if (rimA > 0.02f) {
            across(buffer, matrix,
                a.outerLX, a.outerLY, a.outerLZ,
                a.flareLX, a.flareLY, a.flareLZ,
                b.flareLX, b.flareLY, b.flareLZ,
                b.outerLX, b.outerLY, b.outerLZ,
                1.0f, 1.0f, 1.0f, rimA,
                0.55f, 0.06f, 0.92f, 0.72f * rimA);
            across(buffer, matrix,
                a.flareRX, a.flareRY, a.flareRZ,
                a.outerRX, a.outerRY, a.outerRZ,
                b.outerRX, b.outerRY, b.outerRZ,
                b.flareRX, b.flareRY, b.flareRZ,
                0.55f, 0.06f, 0.92f, 0.72f * rimA,
                1.0f, 1.0f, 1.0f, rimA);
            across(buffer, matrix,
                a.flareLX, a.flareLY, a.flareLZ,
                a.wakeLX, a.wakeLY, a.wakeLZ,
                b.wakeLX, b.wakeLY, b.wakeLZ,
                b.flareLX, b.flareLY, b.flareLZ,
                0.60f, 0.08f, 0.96f, 0.22f * rimA,
                0.60f, 0.08f, 0.96f, 0.0f);
            across(buffer, matrix,
                a.wakeRX, a.wakeRY, a.wakeRZ,
                a.flareRX, a.flareRY, a.flareRZ,
                b.flareRX, b.flareRY, b.flareRZ,
                b.wakeRX, b.wakeRY, b.wakeRZ,
                0.60f, 0.08f, 0.96f, 0.0f,
                0.60f, 0.08f, 0.96f, 0.22f * rimA);
        }
    }

    private static void drawShard(Matrix4f matrix, VertexConsumer buffer, Cut c, boolean left, float radius, float age, int index, float suck, float alpha) {
        float ox = left ? c.outerLX : c.outerRX;
        float oy = left ? c.outerLY : c.outerRY;
        float oz = left ? c.outerLZ : c.outerRZ;
        float dx = ox - c.throatX;
        float dy = oy - c.throatY;
        float dz = oz - c.throatZ;
        float mag = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (mag < 1.0e-4f) {
            return;
        }
        dx /= mag;
        dy /= mag;
        dz /= mag;

        float tx = -dz;
        float tz = dx;
        float tm = (float) Math.sqrt(tx * tx + tz * tz);
        if (tm > 1.0e-4f) {
            tx /= tm;
            tz /= tm;
        }
        float slim = 0.80f + (index % 3) * 0.22f;
        float length = radius * 0.34f * slim * (1.0f - suck);
        float girth = length * 0.20f;
        if (length < 0.04f) {
            return;
        }
        float hug = girth * (3.4f + (index % 4) * 0.60f) * (1.0f - suck);
        float wob = (float) Math.sin(age * 0.55f + index * 1.7f) * girth * 1.6f * (1.0f - suck);
        float lift = (float) Math.cos(age * 0.40f + index) * girth * (1.0f - suck);
        float sx = ox + dx * hug + tx * wob;
        float sy = oy + dy * hug;
        float sz = oz + dz * hug + tz * wob + lift;
        sx += (c.throatX - sx) * suck;
        sy += (c.throatY - sy) * suck;
        sz += (c.throatZ - sz) * suck;

        drawCrystal(matrix, buffer, sx, sy, sz, dx, dy, dz, length, girth, age * 0.35f + index * 0.9f, alpha);
    }

    /**
     * Hexagonal shard with a narrow root, a wide belly, and a white tip.
     * Odd faces are darker so the facets read without a lighting pass.
     */
    private static void drawCrystal(Matrix4f matrix, VertexConsumer buffer,
                                    float ox, float oy, float oz,
                                    float dx, float dy, float dz,
                                    float length, float girth, float roll, float alpha) {
        float mag = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (mag < 1.0e-4f || length < 0.02f || girth < 0.008f) {
            return;
        }
        dx /= mag;
        dy /= mag;
        dz /= mag;

        float hx = 0.0f;
        float hy = 1.0f;
        float hz = 0.0f;
        if (Math.abs(dy) > 0.85f) {
            hx = 1.0f;
            hy = 0.0f;
            hz = 0.0f;
        }
        float ux = dy * hz - dz * hy;
        float uy = dz * hx - dx * hz;
        float uz = dx * hy - dy * hx;
        float um = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
        if (um < 1.0e-4f) {
            return;
        }
        ux /= um;
        uy /= um;
        uz /= um;
        float vx = dy * uz - dz * uy;
        float vy = dz * ux - dx * uz;
        float vz = dx * uy - dy * ux;

        float[] alongP = {-0.50f, 0.04f, 0.50f};
        float[] rad = {0.30f, 1.00f, 0.14f};
        float[] rollOff = {0.0f, 0.55f, 1.10f};
        float[] cr = {0.16f, 0.58f, 1.0f};
        float[] cg = {0.02f, 0.07f, 1.0f};
        float[] cb = {0.30f, 0.94f, 1.0f};

        int count = 3 * CRYSTAL_SIDES;
        float[] px = new float[count];
        float[] py = new float[count];
        float[] pz = new float[count];
        for (int station = 0; station < 3; station++) {
            for (int side = 0; side < CRYSTAL_SIDES; side++) {
                float ang = roll + rollOff[station] + side * ((float) Math.PI * 2.0f / CRYSTAL_SIDES);
                float c = (float) Math.cos(ang);
                float s = (float) Math.sin(ang);
                float gr = girth * rad[station];
                float ax = alongP[station] * length;
                int idx = station * CRYSTAL_SIDES + side;
                px[idx] = ox + dx * ax + (ux * c + vx * s) * gr;
                py[idx] = oy + dy * ax + (uy * c + vy * s) * gr;
                pz[idx] = oz + dz * ax + (uz * c + vz * s) * gr;
            }
        }

        for (int station = 0; station < 2; station++) {
            for (int side = 0; side < CRYSTAL_SIDES; side++) {
                int next = (side + 1) % CRYSTAL_SIDES;
                int i00 = station * CRYSTAL_SIDES + side;
                int i01 = station * CRYSTAL_SIDES + next;
                int i11 = (station + 1) * CRYSTAL_SIDES + next;
                int i10 = (station + 1) * CRYSTAL_SIDES + side;
                float shade = (side & 1) == 0 ? 1.0f : 0.58f;
                along(buffer, matrix,
                    px[i00], py[i00], pz[i00],
                    px[i01], py[i01], pz[i01],
                    px[i11], py[i11], pz[i11],
                    px[i10], py[i10], pz[i10],
                    cr[station] * shade, cg[station] * shade, cb[station] * shade, alpha,
                    cr[station + 1] * shade, cg[station + 1] * shade, cb[station + 1] * shade, alpha);
            }
        }
    }

    /** color0 stays on the v0-v3 edge, color1 on the v1-v2 edge. Both windings. */
    private static void across(VertexConsumer buffer, Matrix4f matrix,
                               float x0, float y0, float z0,
                               float x1, float y1, float z1,
                               float x2, float y2, float z2,
                               float x3, float y3, float z3,
                               float r0, float g0, float b0, float a0,
                               float r1, float g1, float b1, float a1) {
        vert(buffer, matrix, x0, y0, z0, 0.0f, 0.0f, r0, g0, b0, a0);
        vert(buffer, matrix, x1, y1, z1, 1.0f, 0.0f, r1, g1, b1, a1);
        vert(buffer, matrix, x2, y2, z2, 1.0f, 1.0f, r1, g1, b1, a1);
        vert(buffer, matrix, x3, y3, z3, 0.0f, 1.0f, r0, g0, b0, a0);
        vert(buffer, matrix, x3, y3, z3, 0.0f, 1.0f, r0, g0, b0, a0);
        vert(buffer, matrix, x2, y2, z2, 1.0f, 1.0f, r1, g1, b1, a1);
        vert(buffer, matrix, x1, y1, z1, 1.0f, 0.0f, r1, g1, b1, a1);
        vert(buffer, matrix, x0, y0, z0, 0.0f, 0.0f, r0, g0, b0, a0);
    }

    /** color0 stays on the v0-v1 edge, color1 on the v2-v3 edge. Both windings. */
    private static void along(VertexConsumer buffer, Matrix4f matrix,
                              float x0, float y0, float z0,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              float x3, float y3, float z3,
                              float r0, float g0, float b0, float a0,
                              float r1, float g1, float b1, float a1) {
        vert(buffer, matrix, x0, y0, z0, 0.0f, 0.0f, r0, g0, b0, a0);
        vert(buffer, matrix, x1, y1, z1, 1.0f, 0.0f, r0, g0, b0, a0);
        vert(buffer, matrix, x2, y2, z2, 1.0f, 1.0f, r1, g1, b1, a1);
        vert(buffer, matrix, x3, y3, z3, 0.0f, 1.0f, r1, g1, b1, a1);
        vert(buffer, matrix, x3, y3, z3, 0.0f, 1.0f, r1, g1, b1, a1);
        vert(buffer, matrix, x2, y2, z2, 1.0f, 1.0f, r1, g1, b1, a1);
        vert(buffer, matrix, x1, y1, z1, 1.0f, 0.0f, r0, g0, b0, a0);
        vert(buffer, matrix, x0, y0, z0, 0.0f, 0.0f, r0, g0, b0, a0);
    }

    private static void vert(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z, float u, float v, float r, float g, float b, float a) {
        buffer.addVertex(matrix, x, y, z)
            .setColor(r, g, b, a)
            .setUv(u, v)
            .setOverlay(KiRenderHelper.NO_OVERLAY)
            .setLight(KiRenderHelper.FULL_BRIGHT)
            .setNormal(0.0f, 1.0f, 0.0f);
    }

    private static final class Cut {
        float width;
        float pinch;
        float throatX, throatY, throatZ;
        float innerLX, innerLY, innerLZ;
        float innerRX, innerRY, innerRZ;
        float outerLX, outerLY, outerLZ;
        float outerRX, outerRY, outerRZ;
        float flareLX, flareLY, flareLZ;
        float flareRX, flareRY, flareRZ;
        float wakeLX, wakeLY, wakeLZ;
        float wakeRX, wakeRY, wakeRZ;
        float seamLX, seamLY, seamLZ;
        float seamRX, seamRY, seamRZ;
    }
}
