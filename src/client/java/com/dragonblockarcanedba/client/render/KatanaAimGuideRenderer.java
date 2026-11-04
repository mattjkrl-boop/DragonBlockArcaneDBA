package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.KatanaAimGuideEntity;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Entity Renderer for Katana Aim Guide in Minecraft 26.2.
 * The guide beam, reticle rings, brackets, and traveling nodes are gone.
 * This renderer supplies the guide origin, look direction, range, charge,
 * and lifecycle as data to the shared material emitters. Cyan ki plasma
 * drifts along the aim path with a tighter white plasma stream inside
 * it, pale air vapor moves around the path, and denser plasma gathers at
 * the terminal point and at the old six-block calibration distances.
 * The beam length remains the entity's synced max range. Alpha from the
 * old lifecycle becomes emission density here: a fade in over the first
 * three ticks and a fade out over FADE_TICKS once the stance ends.
 */
public class KatanaAimGuideRenderer extends EntityRenderer<KatanaAimGuideEntity, KatanaAimGuideRenderer.AimGuideRenderState> {

    public KatanaAimGuideRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class AimGuideRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public float maxRange = 16.0f;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public boolean isFirstPersonOwner = false;
        public boolean onRight = true;
        public int fadeStart = -1;
        public long seed = 0;
        public UUID uuid = new UUID(0L, 0L);
        public double wx = 0, wy = 0, wz = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(KatanaAimGuideEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(KatanaAimGuideEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public AimGuideRenderState createRenderState() {
        return new AimGuideRenderState();
    }

    @Override
    public void extractRenderState(KatanaAimGuideEntity entity, AimGuideRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.maxRange = entity.getMaxRange();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.fadeStart = entity.getFadeStart();
        state.seed = entity.getUUID().getMostSignificantBits();
        state.uuid = entity.getUUID();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null && 
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) && 
            mc.options.getCameraType().isFirstPerson());
        if (mc.player != null) {
            boolean isRightHanded = (mc.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            boolean isOffhand = (mc.player.getOffhandItem().getItem() instanceof com.dragonblockarcanedba.item.KatanaItem && 
                !(mc.player.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.KatanaItem));
            state.onRight = isRightHanded ? !isOffhand : isOffhand;
        }
    }

    @Override
    public void submit(AimGuideRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float charge = state.chargeRatio;
        float range = state.maxRange;
        float age = state.age;
        float pulse = 0.85f + 0.15f * (float) Math.sin(age * 0.5f);
        float alpha = (0.55f + charge * 0.45f) * pulse * FxEnvelope.smoothstep(Math.min(1.0f, age / 3.0f));
        if (state.fadeStart >= 0) {
            float fadeT = (age - state.fadeStart) / (float) KatanaAimGuideEntity.FADE_TICKS;
            if (fadeT >= 1.0f) return;
            alpha *= 1.0f - FxEnvelope.smoothstep(fadeT);
        }
        if (alpha <= 0.01f) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;

        Vec3 base = new Vec3(state.wx, state.wy, state.wz);
        float dist = MaterialEmitters.distanceScale(base);
        if (dist <= 0.01f) return;

        double yaw = Math.toRadians(state.yRot);
        double pitch = Math.toRadians(state.xRot);
        double cosYaw = Math.cos(yaw), sinYaw = Math.sin(yaw);
        double cosPitch = Math.cos(pitch), sinPitch = Math.sin(pitch);
        Vec3 right = new Vec3(cosYaw, 0, sinYaw);
        Vec3 up = new Vec3(-sinYaw * sinPitch, cosPitch, cosYaw * sinPitch);
        Vec3 forward = new Vec3(-sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);

        Vec3 guideOrigin = base;
        if (state.isFirstPersonOwner) {
            float sideSign = state.onRight ? -1.0f : 1.0f;
            guideOrigin = base.add(right.scale(sideSign * 0.35)).add(up.scale(-0.46)).add(forward.scale(0.30));
        }
        float zStart = state.isFirstPersonOwner ? 0.25f : 0.4f;

        Anchor anchor = anchorFor(state, mc.level);
        anchor.guideOrigin = guideOrigin;
        anchor.right = right;
        anchor.up = up;
        anchor.forward = forward;
        anchor.range = range;
        anchor.charge = charge;
        anchor.zStart = zStart;
        anchor.stationCount = (int) (range / 6.0f);

        // Pieces layer: thin gathering filaments converged at the blade
        // origin and running tight along the guide path toward the
        // terminal point. Local pose space starts at the entity origin,
        // so the fan starts at the same local offset the emission uses.
        Vec3 localStart = guideOrigin.subtract(base).add(forward.scale(zStart));
        float fanLength = (float) Math.max(1.0, range - zStart) * 0.85f;
        float fanEnvelope = alpha;
        ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) ->
            ComplexGeometry.tendrilFan(pose, buffer, state.seed ^ 0xA17DL, localStart, forward,
                7.0f, fanLength, 5, 0.09f, 0x00D9FF, 0xFFFFFF, 0.92f, fanEnvelope, dist));

        float delta = anchor.deltaSeconds(mc.isPaused());
        if (delta > 0f) {
            float envelope = alpha * dist;
            anchor.sheath.tick(delta, envelope);
            anchor.core.tick(delta, envelope);
            anchor.vapor.tick(delta, envelope * 0.7f);
            anchor.terminal.tick(delta, envelope);
            anchor.sparkStreaks.tick(delta, envelope);
            if (anchor.stationCount > 0) anchor.stations.tick(delta, envelope * 0.8f);
        }
        super.submit(state, poseStack, collector, cameraState);
    }

    private static Anchor anchorFor(AimGuideRenderState state, ClientLevel level) {
        long now = System.nanoTime();
        Anchor anchor = ANCHORS.get(state.uuid);
        if (anchor == null || anchor.level != level) {
            anchor = new Anchor(level, (int) state.seed);
            ANCHORS.put(state.uuid, anchor);
        }
        anchor.lastSeenNanos = now;
        Iterator<Map.Entry<UUID, Anchor>> it = ANCHORS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Anchor> entry = it.next();
            if (!entry.getKey().equals(state.uuid) && now - entry.getValue().lastSeenNanos > 4_000_000_000L) {
                it.remove();
            }
        }
        return anchor;
    }

    private static final Map<UUID, Anchor> ANCHORS = new HashMap<>();

    private static final class Anchor {
        private final ClientLevel level;
        private final int seed;
        private long lastSeenNanos;
        private long lastTickNanos;
        private Vec3 guideOrigin = Vec3.ZERO;
        private Vec3 right = new Vec3(1, 0, 0);
        private Vec3 up = new Vec3(0, 1, 0);
        private Vec3 forward = new Vec3(0, 0, 1);
        private float range = 16f;
        private float charge = 0f;
        private float zStart = 0.4f;
        private int stationCount = 0;
        private final MaterialEmitters.Emitter sheath;
        private final MaterialEmitters.Emitter core;
        private final MaterialEmitters.Emitter vapor;
        private final MaterialEmitters.Emitter terminal;
        private final MaterialEmitters.Emitter stations;
        private final MaterialEmitters.Emitter sparkStreaks;

        private Anchor(ClientLevel level, int seed) {
            this.level = level;
            this.seed = seed;
            this.sheath = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0x00D9FF, 1.05f, seed, Vec3.ZERO, 0.010f),
                pathSource(0.055), 176f, 202);
            this.core = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xFFFFFF, 0.72f, seed ^ 0x1111, Vec3.ZERO, 0.007f),
                pathSource(0.020), 126f, 148);
            this.vapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(0xBFEFFF, 1.80f, seed ^ 0x2222, Vec3.ZERO, 0.014f),
                pathSource(0.110), 38f, 50);
            this.terminal = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0x00F2FF, 1.50f, seed ^ 0x3333, Vec3.ZERO, 0.016f),
                volumeSource(true), 62f, 78);
            this.stations = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xD9FAFF, 1.20f, seed ^ 0x4444, Vec3.ZERO, 0.013f),
                volumeSource(false), 34f, 46);
            // Complex particle layer: steel spark streaks drifting in
            // along the guide path, inside the 40 per second budget.
            this.sparkStreaks = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.05f),
                pathSource(0.090), 24f, 32);
        }

        private Vec3 start() {
            return guideOrigin.add(forward.scale(zStart));
        }

        private Vec3 end() {
            return guideOrigin.add(forward.scale(range));
        }

        private MaterialEmitters.Source pathSource(double radiusBase) {
            return random -> {
                double t = random.nextDouble();
                double radius = (radiusBase + charge * radiusBase * 0.65) * Math.sqrt(random.nextDouble());
                double ang = random.nextDouble() * Math.PI * 2.0;
                Vec3 pos = start().add(forward.scale(Math.max(0.0, range - zStart) * t))
                    .add(right.scale(Math.cos(ang) * radius))
                    .add(up.scale(Math.sin(ang) * radius));
                return new MaterialEmitters.Sample(pos, forward.scale(0.015));
            };
        }

        private MaterialEmitters.Source volumeSource(boolean terminalPoint) {
            return random -> {
                Vec3 center;
                double radius;
                if (terminalPoint) {
                    center = end();
                    radius = 0.30 + charge * 0.20;
                } else {
                    int station = stationCount <= 0 ? 0 : 1 + random.nextInt(stationCount);
                    center = guideOrigin.add(forward.scale(station * 6.0));
                    radius = 0.24 + charge * 0.14;
                }
                double z = random.nextDouble() * 2.0 - 1.0;
                double ang = random.nextDouble() * Math.PI * 2.0;
                double rr = Math.cbrt(random.nextDouble()) * radius;
                double flat = Math.sqrt(Math.max(0.0, 1.0 - z * z)) * rr;
                Vec3 pos = center
                    .add(right.scale(Math.cos(ang) * flat))
                    .add(up.scale(Math.sin(ang) * flat))
                    .add(forward.scale(z * rr));
                return new MaterialEmitters.Sample(pos, forward.scale(0.006));
            };
        }

        private float deltaSeconds(boolean paused) {
            long now = System.nanoTime();
            if (lastTickNanos == 0L) {
                lastTickNanos = now;
                return paused ? 0f : 0.016f;
            }
            float delta = Math.min(0.05f, (now - lastTickNanos) / 1_000_000_000.0f);
            lastTickNanos = now;
            return paused ? 0f : delta;
        }
    }
}
