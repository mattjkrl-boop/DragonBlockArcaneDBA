package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.AzureRushTrailEntity;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Held azure rush wind. One wake follows the flyer's chest. Older puff
 * entities do not each stamp another wake.
 *
 * No-shapes pass: this renderer draws no geometry. The wake sampling,
 * centerline, and frame math are unchanged, but the wake itself is now
 * sprayed material along that centerline: a bright plasma core stream,
 * vapor shed through the old shell volume, rain sprayed off the moving
 * wake, and a brighter plasma stream inside the old shock lip station
 * window. The centerline and its radius profile are data only; nothing
 * is rendered from them. Seeds derive from the trail entity's synced
 * caster id, the same key the wake maps already use.
 */
public class AzureRushTrailRenderer extends EntityRenderer<AzureRushTrailEntity, AzureRushTrailRenderer.TrailRenderState> {

    private static final int STATIONS = 16;

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

    /** Mutable per-frame emission data read by the sources. Data only. */
    private static final class Frame {
        List<Vec3> points = List.of();
        List<Vec3> tangents = List.of();
        float[] shellRadius = new float[STATIONS];
        float[] coreRadius = new float[STATIONS];
    }

    private static final class Emission {
        final Frame frame = new Frame();
        ClientLevel level;
        boolean doubleRush;
        MaterialEmitters.Emitter[] emitters = new MaterialEmitters.Emitter[0];
        float lastAge = -1f;
    }

    private final Map<Long, Emission> emissions = new HashMap<>();

    /**
     * Points along the wake centerline. Mode 0 hugs the core, mode 1 fills
     * the shell volume, and mode 2 rides the shock lip station window at
     * 1.22 times the shell radius. The sample velocity follows the wake
     * direction, from the flyer toward the tail.
     */
    private static MaterialEmitters.Source wake(Frame frame, int mode) {
        return random -> {
            int last = STATIONS - 1;
            float t;
            if (mode == 2) {
                t = 0.28f + random.nextFloat() * 0.20f;
            } else {
                t = random.nextFloat();
            }
            float sf = t * last;
            int i = Math.min(last - 1, (int) sf);
            float frac = sf - i;
            Vec3 a = frame.points.get(i);
            Vec3 b = frame.points.get(i + 1);
            Vec3 point = a.add(b.subtract(a).scale(frac));
            Vec3 tangent = frame.tangents.get(i);
            float shell = frame.shellRadius[i] + (frame.shellRadius[i + 1] - frame.shellRadius[i]) * frac;
            float core = frame.coreRadius[i] + (frame.coreRadius[i + 1] - frame.coreRadius[i]) * frac;

            double u = random.nextDouble() * 2.0 - 1.0;
            double ang = random.nextDouble() * Math.PI * 2.0;
            double flat = Math.sqrt(Math.max(0.0, 1.0 - u * u));
            Vec3 dir = new Vec3(Math.cos(ang) * flat, u, Math.sin(ang) * flat);
            double offsetLen;
            if (mode == 0) {
                offsetLen = core * Math.sqrt(random.nextDouble());
            } else if (mode == 2) {
                offsetLen = shell * 1.22 * (0.85 + random.nextDouble() * 0.30);
            } else {
                offsetLen = shell * Math.sqrt(random.nextDouble());
            }
            Vec3 pos = point.add(dir.scale(offsetLen));
            Vec3 vel = tangent.scale(0.06).add(dir.scale(0.03));
            return new MaterialEmitters.Sample(pos, vel);
        };
    }

    private Emission buildEmission(ClientLevel level, long seed, boolean doubleRush) {
        Emission emission = new Emission();
        emission.level = level;
        emission.doubleRush = doubleRush;
        Frame frame = emission.frame;
        int seedInt = (int) seed;
        // Shell colors from the bespoke wake: azure, or green on a double rush.
        int shellColor = doubleRush ? 0x14FF94 : 0x05CCFF;
        emission.emitters = new MaterialEmitters.Emitter[]{
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(0xC7FFFF, 1.1f, seedInt ^ 0xC02E,
                                Vec3.ZERO, 0.01f),
                        wake(frame, 0), 230f, 320),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.vapor(shellColor, 2.0f, seedInt ^ 0x57AA,
                                Vec3.ZERO, 0.02f),
                        wake(frame, 1), 260f, 380),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.fixed(DbaParticles.RAIN,
                                new Vec3(0.0, -0.8, 0.0), 0.03f),
                        wake(frame, 1), 150f, 230),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(0xFFFFFF, 1.0f, seedInt ^ 0x11B0,
                                Vec3.ZERO, 0.01f),
                        wake(frame, 2), 90f, 140),
        };
        return emission;
    }

    @Override
    public void submit(TrailRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        Integer drawer = WRITER.get(state.casterKey);
        ArrayDeque<Nose> wake = WAKES.get(state.casterKey);
        // Fade reaches zero exactly as the 12 tick trail entity dies; over 10
        // ticks the last visible frame ended a tick early and read as a cut.
        float life = 1.0F - Mth.clamp((state.age - 1.0F) / 11.0F, 0.0F, 1.0F);
        if (drawer == null || drawer != state.entityId || wake == null || wake.isEmpty() || life < 0.03F) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        Nose[] noses = wake.toArray(new Nose[0]);
        Nose newest = noses[noses.length - 1];
        // A caster who stopped rushing leaves no new trail entities to prune
        // against, so a stale wake drops its map entries here instead of
        // holding one entry per caster for the life of the client.
        if (state.spin - newest.gameTime > 24.0F) {
            WAKES.remove(state.casterKey);
            WRITER.remove(state.casterKey);
            super.submit(state, poseStack, collector, cameraState);
            return;
        }
        final float scale = Math.max(0.35F, newest.scale);
        final boolean doubleRush = newest.doubleRush;
        final boolean firstPerson = newest.firstPerson;
        final float[] cx = new float[STATIONS];
        final float[] cy = new float[STATIONS];
        final float[] cz = new float[STATIONS];
        buildCenterline(noses, state.renderX, state.renderY, state.renderZ, firstPerson, scale, cx, cy, cz);

        final long seed = (long) state.casterKey * 0x9E3779B97F4A7C15L ^ 0xA20E57L;
        float ds = MaterialEmitters.distanceScale(new Vec3(state.renderX, state.renderY, state.renderZ));
        float envelope = life * (firstPerson ? 0.55f : 1.0f) * ds;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc != null ? mc.level : null;
        if (level != null && envelope > 0.01f) {
            Emission emission = emissions.get(seed);
            if (emission == null || emission.level != level || emission.doubleRush != doubleRush
                    || state.age < emission.lastAge - 1.0f) {
                emission = buildEmission(level, seed, doubleRush);
                emissions.put(seed, emission);
                if (emissions.size() > 64) emissions.clear();
            }
            Frame frame = emission.frame;
            List<Vec3> points = new ArrayList<>(STATIONS);
            List<Vec3> tangents = new ArrayList<>(STATIONS);
            float fp = firstPerson ? 0.5F : 1.0F;
            for (int s = 0; s < STATIONS; s++) {
                points.add(new Vec3(state.renderX + cx[s], state.renderY + cy[s], state.renderZ + cz[s]));
                float t = s / (float) (STATIONS - 1);
                float open = smooth(0.0F, 0.40F, t);
                float settle = 1.0F - 0.38F * smooth(0.62F, 1.0F, t);
                frame.shellRadius[s] = (0.26F + 1.35F * (float) Math.sqrt(open)) * settle * scale * fp;
                frame.coreRadius[s] = (0.11F + 0.10F * open) * (1.0F - 0.62F * t) * scale * fp;
            }
            for (int s = 0; s < STATIONS; s++) {
                Vec3 a = points.get(Math.max(0, s - 1));
                Vec3 b = points.get(Math.min(STATIONS - 1, s + 1));
                Vec3 tangent = b.subtract(a);
                double len = tangent.length();
                tangents.add(len < 1e-6 ? new Vec3(0, 0, 1) : tangent.scale(1.0 / len));
            }
            frame.points = points;
            frame.tangents = tangents;
            float delta = emission.lastAge < 0f ? 0.05f
                    : Math.min(0.05f, Math.max(0.0f, (state.age - emission.lastAge) / 20.0f));
            emission.lastAge = state.age;
            for (MaterialEmitters.Emitter emitter : emission.emitters) {
                emitter.tick(delta, envelope);
            }
        }
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

    private static float smooth(float edge0, float edge1, float t) {
        float x = Mth.clamp((t - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }
}
