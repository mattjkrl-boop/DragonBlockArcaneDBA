package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.KatanaChargeEntity;
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
 * Entity Renderer for Katana Flashdraw Charge in Minecraft 26.2.
 * The stance rings, ground accent, hilt fans, and vortex bands are gone.
 * This renderer supplies the caster position, facing, charge ratio, and
 * lifecycle to the shared material emitters. Ki plasma and air vapor are
 * sampled through the stance volume and pulled toward the sheathed hilt,
 * vapor moves around the rim and over the ground, and a small steel spark
 * stream lives at the hilt itself. Alpha from the old lifecycle becomes
 * emission density here: a smooth build over the first four ticks and a
 * fade across FADE_TICKS once the charge ends or is cancelled.
 */
public class KatanaChargeRenderer extends EntityRenderer<KatanaChargeEntity, KatanaChargeRenderer.KatanaChargeRenderState> {

    public KatanaChargeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class KatanaChargeRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public long seed = 0;
        public UUID uuid = new UUID(0L, 0L);
        public int fadeStart = -1;
        public double wx = 0, wy = 0, wz = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(KatanaChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(KatanaChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public KatanaChargeRenderState createRenderState() {
        return new KatanaChargeRenderState();
    }

    @Override
    public void extractRenderState(KatanaChargeEntity entity, KatanaChargeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
        state.uuid = entity.getUUID();
        state.fadeStart = entity.getFadeStart();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    @Override
    public void submit(KatanaChargeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float charge = state.chargeRatio;
        float age = state.age;
        float pulse = 0.85f + 0.15f * (float) Math.sin(age * 0.4f);
        float alpha = (0.65f + (charge * 0.35f)) * FxEnvelope.smoothstep(Math.min(1.0f, age / 4.0f));
        if (state.fadeStart >= 0) {
            float fadeT = (age - state.fadeStart) / (float) KatanaChargeEntity.FADE_TICKS;
            if (fadeT >= 1.0f) return;
            alpha *= 1.0f - FxEnvelope.smoothstep(fadeT);
        }
        if (alpha <= 0.01f) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;

        Vec3 center = new Vec3(state.wx, state.wy, state.wz);
        float dist = MaterialEmitters.distanceScale(center);
        if (dist <= 0.01f) return;

        double yaw = Math.toRadians(state.yRot);
        Vec3 right = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));

        Anchor anchor = anchorFor(state, mc.level);
        anchor.center = center;
        anchor.right = right;
        anchor.forward = forward;
        anchor.charge = charge;

        // Pieces layer: inward lashes converged at the hilt, length and
        // width growing with the synced charge ratio, so a building
        // charge reads as strands pulling tighter around the blade.
        Vec3 hiltLocal = right.scale(-0.28).add(forward.scale(-0.05)).add(0, 0.75, 0);
        float lashLength = 0.55f + charge * 1.15f;
        float lashWidth = 0.09f + charge * 0.06f;
        float lashEnvelope = alpha * pulse;
        ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) ->
            ComplexGeometry.tendrilFan(pose, buffer, state.seed ^ 0xC4A6EL, hiltLocal, new Vec3(0, 1, 0),
                58.0f, lashLength, 5, lashWidth, 0xFFFFFF, 0x00D9FF, 0.92f, lashEnvelope, dist));

        float delta = anchor.deltaSeconds(mc.isPaused());
        if (delta > 0f) {
            float envelope = alpha * pulse * dist;
            anchor.gatherPlasma.tick(delta, envelope);
            anchor.gatherVapor.tick(delta, envelope * 0.8f);
            anchor.hiltPlasma.tick(delta, envelope);
            anchor.hiltSparks.tick(delta, envelope);
            anchor.sparkStreaks.tick(delta, envelope);
            anchor.wispCurls.tick(delta, envelope * 0.7f);
            anchor.groundVapor.tick(delta, envelope * 0.55f);
        }
        super.submit(state, poseStack, collector, cameraState);
    }

    private static Anchor anchorFor(KatanaChargeRenderState state, ClientLevel level) {
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
        private Vec3 center = Vec3.ZERO;
        private Vec3 right = new Vec3(1, 0, 0);
        private Vec3 forward = new Vec3(0, 0, 1);
        private float charge = 0f;
        private final MaterialEmitters.Emitter gatherPlasma;
        private final MaterialEmitters.Emitter gatherVapor;
        private final MaterialEmitters.Emitter hiltPlasma;
        private final MaterialEmitters.Emitter hiltSparks;
        private final MaterialEmitters.Emitter groundVapor;
        private final MaterialEmitters.Emitter sparkStreaks;
        private final MaterialEmitters.Emitter wispCurls;

        private Anchor(ClientLevel level, int seed) {
            this.level = level;
            this.seed = seed;
            MaterialEmitters.Source gather = random -> {
                Vec3 mid = center.add(0, 0.75, 0);
                double z = random.nextDouble() * 2.0 - 1.0;
                double ang = random.nextDouble() * Math.PI * 2.0;
                double rr = Math.cbrt(random.nextDouble());
                double flat = Math.sqrt(Math.max(0.0, 1.0 - z * z)) * rr;
                double radius = 1.30 + charge * 0.45;
                Vec3 pos = mid
                    .add(right.scale(Math.cos(ang) * flat * radius))
                    .add(forward.scale(Math.sin(ang) * flat * radius))
                    .add(0, z * 0.78, 0);
                Vec3 vel = hilt().subtract(pos).scale(0.20).add(0, 0.010, 0);
                return new MaterialEmitters.Sample(pos, vel);
            };
            MaterialEmitters.Source rim = random -> {
                double ang = random.nextDouble() * Math.PI * 2.0;
                double radius = 0.90 + charge * 0.40 + (random.nextDouble() * 2.0 - 1.0) * 0.16;
                Vec3 pos = center
                    .add(right.scale(Math.cos(ang) * radius))
                    .add(forward.scale(Math.sin(ang) * radius))
                    .add(0, 0.12 + random.nextDouble() * 0.80, 0);
                Vec3 tangent = right.scale(-Math.sin(ang)).add(forward.scale(Math.cos(ang))).scale(0.11);
                Vec3 vel = tangent.add(center.add(0, 0.75, 0).subtract(pos).scale(0.018));
                return new MaterialEmitters.Sample(pos, vel);
            };
            MaterialEmitters.Source hiltSource = random -> {
                Vec3 pos = hilt().add(
                    (random.nextDouble() * 2.0 - 1.0) * 0.07,
                    (random.nextDouble() * 2.0 - 1.0) * 0.07,
                    (random.nextDouble() * 2.0 - 1.0) * 0.07);
                return new MaterialEmitters.Sample(pos, new Vec3(0, 0.018, 0));
            };
            MaterialEmitters.Source ground = random -> {
                double ang = random.nextDouble() * Math.PI * 2.0;
                double rr = Math.sqrt(random.nextDouble()) * (1.30 + charge * 0.40);
                Vec3 pos = new Vec3(center.x + Math.cos(ang) * rr, center.y + 0.04, center.z + Math.sin(ang) * rr);
                return new MaterialEmitters.Sample(pos, new Vec3(0, 0.010, 0));
            };
            this.gatherPlasma = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0x9FE8FF, 2.1f, seed, Vec3.ZERO, 0.016f),
                gather, 108f, 128);
            this.gatherVapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(0xDBF2FF, 3.1f, seed ^ 0x1111, Vec3.ZERO, 0.018f),
                rim, 56f, 72);
            this.hiltPlasma = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xFFFFFF, 1.35f, seed ^ 0x2222, Vec3.ZERO, 0.012f),
                hiltSource, 38f, 48);
            this.hiltSparks = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK, Vec3.ZERO, 0.11f),
                hiltSource, 15f, 22);
            this.groundVapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(0x7FD8FF, 3.6f, seed ^ 0x3333, Vec3.ZERO, 0.016f),
                ground, 21f, 28);
            // Complex particle layer: spark streaks at the hilt and a
            // sparse wisp curl drift on the rim, both inside the 40 per
            // second continuous budget.
            this.sparkStreaks = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.08f),
                hiltSource, 20f, 28);
            this.wispCurls = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.WISP_CURL, Vec3.ZERO, 0.03f),
                rim, 9f, 14);
        }

        private Vec3 hilt() {
            return center.add(right.scale(-0.28)).add(forward.scale(-0.05)).add(0, 0.75, 0);
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
