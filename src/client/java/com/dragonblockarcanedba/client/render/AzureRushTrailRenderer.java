package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.AzureRushTrailEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * Held azure rush wind. One wake follows the flyer's chest: a curved cyan
 * shell, a bright core tube, a shock lip, and two ribbons with thickness.
 * The tail keeps its width and fades out. Older puff entities do not each
 * stamp another cone.
 */
public class AzureRushTrailRenderer extends EntityRenderer<AzureRushTrailEntity, AzureRushTrailRenderer.TrailRenderState> {

    private static final int STATIONS = 16;
    private static final int SHELL_SIDES = 12;
    private static final int CORE_SIDES = 8;

    private static final Map<Integer, ArrayDeque<Nose>> WAKES = new HashMap<>();
    private static final Map<Integer, Integer> WRITER = new HashMap<>();

    public AzureRushTrailRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class TrailRenderState extends EntityRenderState {
        public boolean isDoubleRush = false;
        public float trailScale = 1.0f;
        public float age = 0;
        public float yRot = 0;
        public float xRot = 0;
        public boolean isFirstPersonOwner = false;
        public boolean onRight = true;
        public int entityId = -1;
        public int casterKey = -1;
        public double renderX;
        public double renderY;
        public double renderZ;
        public float spin;
    }

    private static final class Nose {
        final double x;
        final double y;
        final double z;
        final float yaw;
        final float pitch;
        final long gameTime;
        final boolean doubleRush;
        final float scale;
        final boolean firstPerson;

        Nose(double x, double y, double z, float yaw, float pitch, long gameTime, boolean doubleRush, float scale, boolean firstPerson) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.gameTime = gameTime;
            this.doubleRush = doubleRush;
            this.scale = scale;
            this.firstPerson = firstPerson;
        }
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(AzureRushTrailEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(AzureRushTrailEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public TrailRenderState createRenderState() {
        return new TrailRenderState();
    }

    @Override
    public void extractRenderState(AzureRushTrailEntity entity, TrailRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.isDoubleRush = entity.isDoubleRush();
        state.trailScale = entity.getTrailScale();
        state.age = entity.tickCount + partialTicks;
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.renderX = Mth.lerp(partialTicks, entity.xo, entity.getX());
        state.renderY = Mth.lerp(partialTicks, entity.yo, entity.getY());
        state.renderZ = Mth.lerp(partialTicks, entity.zo, entity.getZ());
        state.spin = entity.level().getGameTime() + partialTicks;

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = mc.player != null
                && (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player)
                && mc.options.getCameraType().isFirstPerson();
        if (mc.player != null) {
            boolean isRightHanded = mc.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
            boolean isOffhand = mc.player.getOffhandItem().getItem() instanceof com.dragonblockarcanedba.item.AzureDragonSwordItem
                    && !(mc.player.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.AzureDragonSwordItem);
            state.onRight = isRightHanded ? !isOffhand : isOffhand;
        }

        int key = entity.getCasterId();
        if (key < 0) {
            key = entity.getId();
        }
        state.entityId = entity.getId();
        state.casterKey = key;

        // One new chest sample per tick, owned by a single puff. Letting go
        // stops new samples, and every other puff skips drawing so the cones
        // do not stack.
        if (entity.tickCount <= 1) {
            long time = entity.level().getGameTime();
            ArrayDeque<Nose> wake = WAKES.computeIfAbsent(key, ignored -> new ArrayDeque<>());
            while (!wake.isEmpty() && time - wake.peekFirst().gameTime > 16L) {
                wake.pollFirst();
            }
            Nose last = wake.peekLast();
            Integer writer = WRITER.get(key);
            if (last == null || last.gameTime != time) {
                wake.addLast(sampleNose(entity, partialTicks, state.isFirstPersonOwner));
                while (wake.size() > 18) {
                    wake.pollFirst();
                }
                WRITER.put(key, entity.getId());
            } else if (writer != null && writer == entity.getId()) {
                wake.pollLast();
                wake.addLast(sampleNose(entity, partialTicks, state.isFirstPersonOwner));
            }
        }
    }

    private static Nose sampleNose(AzureRushTrailEntity entity, float partial, boolean firstPerson) {
        Entity owner = entity.getOwner();
        if (owner == null && entity.getCasterId() >= 0) {
            owner = entity.level().getEntity(entity.getCasterId());
        }
        double x;
        double y;
        double z;
        float yaw;
        float pitch;
        if (owner != null) {
            x = Mth.lerp(partial, owner.xo, owner.getX());
            y = Mth.lerp(partial, owner.yo, owner.getY()) + owner.getBbHeight() * 0.58;
            z = Mth.lerp(partial, owner.zo, owner.getZ());
            yaw = Mth.rotLerp(partial, owner.yRotO, owner.getYRot());
            pitch = Mth.lerp(partial, owner.xRotO, owner.getXRot());
        } else {
            x = Mth.lerp(partial, entity.xo, entity.getX());
            y = Mth.lerp(partial, entity.yo, entity.getY()) + 0.7;
            z = Mth.lerp(partial, entity.zo, entity.getZ());
            yaw = entity.getYRot();
            pitch = entity.getXRot();
        }
        return new Nose(x, y, z, yaw, pitch, entity.level().getGameTime(), entity.isDoubleRush(), entity.getTrailScale(), firstPerson);
    }

    @Override
    public void submit(TrailRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        Integer drawer = WRITER.get(state.casterKey);
        ArrayDeque<Nose> wake = WAKES.get(state.casterKey);
        float life = 1.0F - Mth.clamp((state.age - 1.0F) / 10.0F, 0.0F, 1.0F);
        if (drawer == null || drawer != state.entityId || wake == null || wake.isEmpty() || life < 0.03F) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        Nose[] noses = wake.toArray(new Nose[0]);
        Nose newest = noses[noses.length - 1];
        final float scale = Math.max(0.35F, newest.scale);
        final boolean doubleRush = newest.doubleRush;
        final boolean firstPerson = newest.firstPerson;
        final float spin = state.spin * 0.22F;
        final float fadeLife = life;
        final float[] cx = new float[STATIONS];
        final float[] cy = new float[STATIONS];
        final float[] cz = new float[STATIONS];
        buildCenterline(noses, state.renderX, state.renderY, state.renderZ, firstPerson, scale, cx, cy, cz);

        RenderType renderType = KiRenderHelper.kiRenderType();
        poseStack.pushPose();
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                emitWake(pose.pose(), buffer, cx, cy, cz, scale, spin, fadeLife, doubleRush, firstPerson));
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void buildCenterline(Nose[] noses, double ox, double oy, double oz, boolean firstPerson, float scale, float[] cx, float[] cy, float[] cz) {
        int raw = noses.length;
        double[] xs = new double[raw + 2];
        double[] ys = new double[raw + 2];
        double[] zs = new double[raw + 2];
        int n = 0;
        for (int i = raw - 1; i >= 0; i--) {
            xs[n] = noses[i].x - ox;
            ys[n] = noses[i].y - oy;
            zs[n] = noses[i].z - oz;
            n++;
        }
        double[] forward = look(noses[raw - 1].yaw, noses[raw - 1].pitch);
        if (n == 1) {
            float len = 4.4F * scale;
            xs[1] = xs[0] - forward[0] * len;
            ys[1] = ys[0] - forward[1] * len;
            zs[1] = zs[0] - forward[2] * len;
            n = 2;
        }
        double dx = xs[n - 1] - xs[n - 2];
        double dy = ys[n - 1] - ys[n - 2];
        double dz = zs[n - 1] - zs[n - 2];
        double seg = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (seg < 0.05) {
            dx = -forward[0];
            dy = -forward[1];
            dz = -forward[2];
            seg = 1.0;
        }
        double extra = 1.7 * scale;
        xs[n] = xs[n - 1] + dx / seg * extra;
        ys[n] = ys[n - 1] + dy / seg * extra;
        zs[n] = zs[n - 1] + dz / seg * extra;
        n++;

        float[] cum = new float[n];
        for (int i = 1; i < n; i++) {
            double sx = xs[i] - xs[i - 1];
            double sy = ys[i] - ys[i - 1];
            double sz = zs[i] - zs[i - 1];
            float step = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
            if (step < 1.0e-4F) {
                step = 1.0e-4F;
            }
            cum[i] = cum[i - 1] + step;
        }
        float total = cum[n - 1];
        float minLen = 3.6F * scale;
        if (total < minLen) {
            float stretch = minLen / Math.max(total, 1.0e-3F);
            for (int i = 1; i < n; i++) {
                xs[i] = xs[0] + (xs[i] - xs[0]) * stretch;
                ys[i] = ys[0] + (ys[i] - ys[0]) * stretch;
                zs[i] = zs[0] + (zs[i] - zs[0]) * stretch;
                cum[i] *= stretch;
            }
            total = minLen;
        }
        if (firstPerson) {
            double back = 1.25;
            for (int i = 0; i < n; i++) {
                xs[i] -= forward[0] * back;
                ys[i] -= forward[1] * back;
                zs[i] -= forward[2] * back;
            }
        }
        int stations = cx.length;
        for (int s = 0; s < stations; s++) {
            float target = total * (s / (float) (stations - 1));
            int segIndex = 0;
            while (segIndex < n - 2 && cum[segIndex + 1] < target) {
                segIndex++;
            }
            float span = cum[segIndex + 1] - cum[segIndex];
            float u = span > 1.0e-4F ? (target - cum[segIndex]) / span : 0.0F;
            u = Mth.clamp(u, 0.0F, 1.0F);
            cx[s] = (float) (xs[segIndex] + (xs[segIndex + 1] - xs[segIndex]) * u);
            cy[s] = (float) (ys[segIndex] + (ys[segIndex + 1] - ys[segIndex]) * u);
            cz[s] = (float) (zs[segIndex] + (zs[segIndex + 1] - zs[segIndex]) * u);
        }
    }

    private static double[] look(float yaw, float pitch) {
        double yr = Math.toRadians(yaw);
        double pr = Math.toRadians(pitch);
        double cp = Math.cos(pr);
        return new double[] {-Math.sin(yr) * cp, -Math.sin(pr), Math.cos(yr) * cp};
    }

    private static void emitWake(Matrix4f matrix, VertexConsumer buffer, float[] cx, float[] cy, float[] cz,
                                 float scale, float spin, float life, boolean doubleRush, boolean firstPerson) {
        int stations = cx.length;
        int last = stations - 1;
        float shellCr = doubleRush ? 0.08F : 0.02F;
        float shellCg = doubleRush ? 1.0F : 0.80F;
        float shellCb = doubleRush ? 0.58F : 1.0F;
        float hotR = 0.78F;
        float hotG = 1.0F;
        float hotB = 1.0F;
        float fp = firstPerson ? 0.5F : 1.0F;

        float[] shellRadius = new float[stations];
        float[] coreRadius = new float[stations];
        float[] along = new float[stations];
        float[] sideX = new float[stations];
        float[] sideY = new float[stations];
        float[] sideZ = new float[stations];
        float[] binX = new float[stations];
        float[] binY = new float[stations];
        float[] binZ = new float[stations];
        float[] tanX = new float[stations];
        float[] tanY = new float[stations];
        float[] tanZ = new float[stations];

        float prevSx = 0.0F;
        float prevSy = 0.0F;
        float prevSz = 0.0F;
        boolean hasSide = false;
        for (int s = 0; s < stations; s++) {
            float t = s / (float) last;
            along[s] = t;
            float open = smooth(0.0F, 0.40F, t);
            float settle = 1.0F - 0.38F * smooth(0.62F, 1.0F, t);
            shellRadius[s] = (0.26F + 1.35F * (float) Math.sqrt(open)) * settle * scale * fp;
            coreRadius[s] = (0.11F + 0.10F * open) * (1.0F - 0.62F * t) * scale * fp;
            float tx;
            float ty;
            float tz;
            if (s == 0) {
                tx = cx[1] - cx[0];
                ty = cy[1] - cy[0];
                tz = cz[1] - cz[0];
            } else if (s == last) {
                tx = cx[s] - cx[s - 1];
                ty = cy[s] - cy[s - 1];
                tz = cz[s] - cz[s - 1];
            } else {
                tx = cx[s + 1] - cx[s - 1];
                ty = cy[s + 1] - cy[s - 1];
                tz = cz[s + 1] - cz[s - 1];
            }
            float tlen = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
            if (tlen < 1.0e-4F) {
                tx = 0.0F;
                ty = 0.0F;
                tz = 1.0F;
                tlen = 1.0F;
            }
            tx /= tlen;
            ty /= tlen;
            tz /= tlen;
            float sx;
            float sy;
            float sz;
            if (!hasSide) {
                sx = -tz;
                sy = 0.0F;
                sz = tx;
            } else {
                float dot = prevSx * tx + prevSy * ty + prevSz * tz;
                sx = prevSx - tx * dot;
                sy = prevSy - ty * dot;
                sz = prevSz - tz * dot;
            }
            float sl = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
            if (sl < 1.0e-4F) {
                sx = -tz;
                sy = 0.0F;
                sz = tx;
                sl = (float) Math.sqrt(sx * sx + sz * sz);
                if (sl < 1.0e-4F) {
                    sx = 1.0F;
                    sy = 0.0F;
                    sz = 0.0F;
                    sl = 1.0F;
                }
            }
            sx /= sl;
            sy /= sl;
            sz /= sl;
            float bx = ty * sz - tz * sy;
            float by = tz * sx - tx * sz;
            float bz = tx * sy - ty * sx;
            tanX[s] = tx;
            tanY[s] = ty;
            tanZ[s] = tz;
            sideX[s] = sx;
            sideY[s] = sy;
            sideZ[s] = sz;
            binX[s] = bx;
            binY[s] = by;
            binZ[s] = bz;
            prevSx = sx;
            prevSy = sy;
            prevSz = sz;
            hasSide = true;
        }

        float[] shellX = new float[stations * SHELL_SIDES];
        float[] shellY = new float[stations * SHELL_SIDES];
        float[] shellZ = new float[stations * SHELL_SIDES];
        float[] coreX = new float[stations * CORE_SIDES];
        float[] coreY = new float[stations * CORE_SIDES];
        float[] coreZ = new float[stations * CORE_SIDES];
        for (int s = 0; s < stations; s++) {
            for (int k = 0; k < SHELL_SIDES; k++) {
                float ang = (float) (k / (double) SHELL_SIDES * Math.PI * 2.0);
                float wobble = 1.0F + 0.07F * (float) Math.sin(ang * 3.0 + spin * 0.35F);
                float cs = (float) Math.cos(ang);
                float sn = (float) Math.sin(ang);
                float radius = shellRadius[s] * wobble;
                int idx = s * SHELL_SIDES + k;
                shellX[idx] = cx[s] + (sideX[s] * cs + binX[s] * sn) * radius;
                shellY[idx] = cy[s] + (sideY[s] * cs + binY[s] * sn) * radius;
                shellZ[idx] = cz[s] + (sideZ[s] * cs + binZ[s] * sn) * radius;
            }
            for (int k = 0; k < CORE_SIDES; k++) {
                float ang = (float) (k / (double) CORE_SIDES * Math.PI * 2.0 + 0.2F);
                float cs = (float) Math.cos(ang);
                float sn = (float) Math.sin(ang);
                int idx = s * CORE_SIDES + k;
                coreX[idx] = cx[s] + (sideX[s] * cs + binX[s] * sn) * coreRadius[s];
                coreY[idx] = cy[s] + (sideY[s] * cs + binY[s] * sn) * coreRadius[s];
                coreZ[idx] = cz[s] + (sideZ[s] * cs + binZ[s] * sn) * coreRadius[s];
            }
        }

        for (int s = 0; s < last; s++) {
            float t = (s + 0.5F) / last;
            float fade = (1.0F - smooth(0.55F, 1.0F, t)) * life;
            float hot = 1.0F - smooth(0.05F, 0.55F, t);
            float r = hot * hotR + (1.0F - hot) * shellCr;
            float g = hot * hotG + (1.0F - hot) * shellCg;
            float b = hot * hotB + (1.0F - hot) * shellCb;
            float shellA = fade * (firstPerson ? 0.22F : 0.34F);
            float coreA = fade * (firstPerson ? 0.55F : 0.88F);
            for (int k = 0; k < SHELL_SIDES; k++) {
                int k2 = (k + 1) % SHELL_SIDES;
                int a = s * SHELL_SIDES + k;
                int c = s * SHELL_SIDES + k2;
                int d = (s + 1) * SHELL_SIDES + k2;
                int e = (s + 1) * SHELL_SIDES + k;
                drawQuad(matrix, buffer,
                        shellX[a], shellY[a], shellZ[a],
                        shellX[c], shellY[c], shellZ[c],
                        shellX[d], shellY[d], shellZ[d],
                        shellX[e], shellY[e], shellZ[e],
                        r, g, b, shellA);
            }
            for (int k = 0; k < CORE_SIDES; k++) {
                int k2 = (k + 1) % CORE_SIDES;
                int a = s * CORE_SIDES + k;
                int c = s * CORE_SIDES + k2;
                int d = (s + 1) * CORE_SIDES + k2;
                int e = (s + 1) * CORE_SIDES + k;
                drawQuad(matrix, buffer,
                        coreX[a], coreY[a], coreZ[a],
                        coreX[c], coreY[c], coreZ[c],
                        coreX[d], coreY[d], coreZ[d],
                        coreX[e], coreY[e], coreZ[e],
                        hotR, hotG, hotB, coreA);
            }
        }

        // Shock lip: one curved band around the mouth, with radial thickness
        // and a short run down the wake. Not a second cone.
        for (int s = 0; s < last; s++) {
            float tMid = (along[s] + along[s + 1]) * 0.5F;
            if (tMid < 0.28F || tMid > 0.48F) {
                continue;
            }
            float lipA = life * (firstPerson ? 0.4F : 0.78F);
            for (int k = 0; k < SHELL_SIDES; k++) {
                int k2 = (k + 1) % SHELL_SIDES;
                float a0 = (float) (k / (double) SHELL_SIDES * Math.PI * 2.0);
                float a1 = (float) (k2 / (double) SHELL_SIDES * Math.PI * 2.0);
                float in0 = shellRadius[s];
                float out0 = shellRadius[s] * 1.22F;
                float out1 = shellRadius[s + 1] * 1.16F;
                drawQuad(matrix, buffer,
                        ringPoint(cx[s], sideX[s], binX[s], a0, in0),
                        ringPoint(cy[s], sideY[s], binY[s], a0, in0),
                        ringPoint(cz[s], sideZ[s], binZ[s], a0, in0),
                        ringPoint(cx[s], sideX[s], binX[s], a0, out0),
                        ringPoint(cy[s], sideY[s], binY[s], a0, out0),
                        ringPoint(cz[s], sideZ[s], binZ[s], a0, out0),
                        ringPoint(cx[s], sideX[s], binX[s], a1, out0),
                        ringPoint(cy[s], sideY[s], binY[s], a1, out0),
                        ringPoint(cz[s], sideZ[s], binZ[s], a1, out0),
                        ringPoint(cx[s], sideX[s], binX[s], a1, in0),
                        ringPoint(cy[s], sideY[s], binY[s], a1, in0),
                        ringPoint(cz[s], sideZ[s], binZ[s], a1, in0),
                        hotR, hotG, hotB, lipA);
                drawQuad(matrix, buffer,
                        ringPoint(cx[s], sideX[s], binX[s], a0, out0),
                        ringPoint(cy[s], sideY[s], binY[s], a0, out0),
                        ringPoint(cz[s], sideZ[s], binZ[s], a0, out0),
                        ringPoint(cx[s], sideX[s], binX[s], a1, out0),
                        ringPoint(cy[s], sideY[s], binY[s], a1, out0),
                        ringPoint(cz[s], sideZ[s], binZ[s], a1, out0),
                        ringPoint(cx[s + 1], sideX[s + 1], binX[s + 1], a1, out1),
                        ringPoint(cy[s + 1], sideY[s + 1], binY[s + 1], a1, out1),
                        ringPoint(cz[s + 1], sideZ[s + 1], binZ[s + 1], a1, out1),
                        ringPoint(cx[s + 1], sideX[s + 1], binX[s + 1], a0, out1),
                        ringPoint(cy[s + 1], sideY[s + 1], binY[s + 1], a0, out1),
                        ringPoint(cz[s + 1], sideZ[s + 1], binZ[s + 1], a0, out1),
                        hotR, hotG, hotB, lipA * 0.85F);
            }
        }

        // Rounded core nose, a cap rather than a spike down the flight line.
        float capReach = 0.2F * scale * fp;
        float capX = cx[0] - tanX[0] * capReach;
        float capY = cy[0] - tanY[0] * capReach;
        float capZ = cz[0] - tanZ[0] * capReach;
        float capA = life * (firstPerson ? 0.45F : 0.8F);
        for (int k = 0; k < CORE_SIDES; k++) {
            int k2 = (k + 1) % CORE_SIDES;
            int a = k;
            int c = k2;
            drawTriangleGrad(matrix, buffer,
                    capX, capY, capZ,
                    coreX[a], coreY[a], coreZ[a],
                    coreX[c], coreY[c], coreZ[c],
                    1.0F, 1.0F, 1.0F, capA,
                    hotR, hotG, hotB, capA * 0.7F);
        }

        for (int strand = 0; strand < 2; strand++) {
            float phase = strand * (float) Math.PI;
            float[] px = new float[stations];
            float[] py = new float[stations];
            float[] pz = new float[stations];
            float[] wx = new float[stations];
            float[] wy = new float[stations];
            float[] wz = new float[stations];
            float[] nx = new float[stations];
            float[] ny = new float[stations];
            float[] nz = new float[stations];
            float[] hw = new float[stations];
            float[] ht = new float[stations];
            for (int s = 0; s < stations; s++) {
                float t = along[s];
                float ang = phase + t * 4.4F + spin;
                float cs = (float) Math.cos(ang);
                float sn = (float) Math.sin(ang);
                float rad = shellRadius[s] * (0.55F + 0.38F * t);
                px[s] = cx[s] + (sideX[s] * cs + binX[s] * sn) * rad;
                py[s] = cy[s] + (sideY[s] * cs + binY[s] * sn) * rad;
                pz[s] = cz[s] + (sideZ[s] * cs + binZ[s] * sn) * rad;
                wx[s] = -sideX[s] * sn + binX[s] * cs;
                wy[s] = -sideY[s] * sn + binY[s] * cs;
                wz[s] = -sideZ[s] * sn + binZ[s] * cs;
                nx[s] = sideX[s] * cs + binX[s] * sn;
                ny[s] = sideY[s] * cs + binY[s] * sn;
                nz[s] = sideZ[s] * cs + binZ[s] * sn;
                hw[s] = 0.2F * (1.0F - t * 0.62F) * scale * fp;
                ht[s] = 0.055F * (1.0F - t * 0.4F) * scale * fp;
            }
            for (int s = 0; s < last; s++) {
                float t = (s + 0.5F) / last;
                float fade = (1.0F - smooth(0.5F, 1.0F, t));
                fade = fade * fade * life;
                float ribbonA = fade * (firstPerson ? 0.4F : 0.7F);
                float c0x = px[s] - wx[s] * hw[s] - nx[s] * ht[s];
                float c0y = py[s] - wy[s] * hw[s] - ny[s] * ht[s];
                float c0z = pz[s] - wz[s] * hw[s] - nz[s] * ht[s];
                float c1x = px[s] + wx[s] * hw[s] - nx[s] * ht[s];
                float c1y = py[s] + wy[s] * hw[s] - ny[s] * ht[s];
                float c1z = pz[s] + wz[s] * hw[s] - nz[s] * ht[s];
                float c2x = px[s] + wx[s] * hw[s] + nx[s] * ht[s];
                float c2y = py[s] + wy[s] * hw[s] + ny[s] * ht[s];
                float c2z = pz[s] + wz[s] * hw[s] + nz[s] * ht[s];
                float c3x = px[s] - wx[s] * hw[s] + nx[s] * ht[s];
                float c3y = py[s] - wy[s] * hw[s] + ny[s] * ht[s];
                float c3z = pz[s] - wz[s] * hw[s] + nz[s] * ht[s];
                int n = s + 1;
                float d0x = px[n] - wx[n] * hw[n] - nx[n] * ht[n];
                float d0y = py[n] - wy[n] * hw[n] - ny[n] * ht[n];
                float d0z = pz[n] - wz[n] * hw[n] - nz[n] * ht[n];
                float d1x = px[n] + wx[n] * hw[n] - nx[n] * ht[n];
                float d1y = py[n] + wy[n] * hw[n] - ny[n] * ht[n];
                float d1z = pz[n] + wz[n] * hw[n] - nz[n] * ht[n];
                float d2x = px[n] + wx[n] * hw[n] + nx[n] * ht[n];
                float d2y = py[n] + wy[n] * hw[n] + ny[n] * ht[n];
                float d2z = pz[n] + wz[n] * hw[n] + nz[n] * ht[n];
                float d3x = px[n] - wx[n] * hw[n] + nx[n] * ht[n];
                float d3y = py[n] - wy[n] * hw[n] + ny[n] * ht[n];
                float d3z = pz[n] - wz[n] * hw[n] + nz[n] * ht[n];
                float rr = shellCr * 0.35F + 0.65F;
                float rg = shellCg;
                float rb = shellCb;
                drawQuad(matrix, buffer, c0x, c0y, c0z, c1x, c1y, c1z, d1x, d1y, d1z, d0x, d0y, d0z, rr, rg, rb, ribbonA);
                drawQuad(matrix, buffer, c1x, c1y, c1z, c2x, c2y, c2z, d2x, d2y, d2z, d1x, d1y, d1z, hotR, hotG, hotB, ribbonA);
                drawQuad(matrix, buffer, c2x, c2y, c2z, c3x, c3y, c3z, d3x, d3y, d3z, d2x, d2y, d2z, rr, rg, rb, ribbonA * 0.85F);
                drawQuad(matrix, buffer, c3x, c3y, c3z, c0x, c0y, c0z, d0x, d0y, d0z, d3x, d3y, d3z, rr, rg, rb, ribbonA * 0.7F);
            }
        }
    }

    private static float ringPoint(float center, float side, float bin, float angle, float radius) {
        return center + (side * (float) Math.cos(angle) + bin * (float) Math.sin(angle)) * radius;
    }

    private static float smooth(float edge0, float edge1, float t) {
        float x = Mth.clamp((t - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    private static void drawTriangleGrad(Matrix4f matrix, VertexConsumer consumer,
                                         float x1, float y1, float z1,
                                         float x2, float y2, float z2,
                                         float x3, float y3, float z3,
                                         float r1, float g1, float b1, float a1,
                                         float r2, float g2, float b2, float a2) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r1, g1, b1, a1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r2, g2, b2, a2).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r2, g2, b2, a2).setUv(0.5F, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r2, g2, b2, a2).setUv(0.5F, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r2, g2, b2, a2).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r1, g1, b1, a1).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
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
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(0, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(0, 0).setOverlay(KiRenderHelper.NO_OVERLAY).setLight(KiRenderHelper.FULL_BRIGHT).setNormal(0, -1, 0);
    }
}
