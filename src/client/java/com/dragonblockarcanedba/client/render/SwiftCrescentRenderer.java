package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.SwiftCrescentEntity;
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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Entity Renderer for Swift Crescent in Minecraft 26.2.
 * The Flashdraw cut is no longer drawn as a crescent mesh. This renderer
 * only supplies the cut position, orientation, scale, and lifecycle as
 * data to the shared material emitters. Silver and white ki plasma fills
 * the moving cut volume, the variant lip color tints a second plasma
 * stream, air vapor trails behind the cut, and steel sparks gather at the
 * belly. At the strike tick the same materials leave as a one-shot burst.
 * Variant 0 cuts with a cyan lip; variant 1 reverses the wake direction
 * and cuts with a pale silver-blue lip.
 */
public class SwiftCrescentRenderer extends EntityRenderer<SwiftCrescentEntity, SwiftCrescentRenderer.SwiftCrescentRenderState> {

    public SwiftCrescentRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SwiftCrescentRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public float tiltAngle = 0.0f;
        public int variant = 0;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public int maxLifetime = 10;
        public int strikeTick = -1;
        public long seed = 0;
        public UUID uuid = new UUID(0L, 0L);
        public double wx = 0, wy = 0, wz = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(SwiftCrescentEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(SwiftCrescentEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public SwiftCrescentRenderState createRenderState() {
        return new SwiftCrescentRenderState();
    }

    @Override
    public void extractRenderState(SwiftCrescentEntity entity, SwiftCrescentRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getSlashScale();
        state.tiltAngle = entity.getTiltAngle();
        state.variant = entity.getSlashVariant();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.strikeTick = entity.getStrikeDamage() > 0f ? entity.getStrikeTick() : -1;
        state.seed = entity.getUUID().getMostSignificantBits();
        state.uuid = entity.getUUID();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    @Override
    public void submit(SwiftCrescentRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float fade = FxEnvelope.envelope(state.age, state.maxLifetime, 0.25f, 0.35f);
        if (fade <= 0.0f) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;

        float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
        float open = FxEnvelope.smoothstep(Math.min(1.0f, progress / 0.25f));
        float baseScale = state.scale * (0.76f + 0.30f * open);
        float trail = 0.10f + 0.58f * FxEnvelope.smoothstep(Math.min(1.0f, progress / 0.65f));
        float strikeFrac = state.strikeTick >= 0
            ? state.strikeTick / (float) state.maxLifetime
            : 0.20f;
        float impact = strike(progress, strikeFrac);
        Vec3 center = new Vec3(state.wx, state.wy, state.wz);
        float dist = MaterialEmitters.distanceScale(center);
        if (dist <= 0.01f) return;

        Anchor anchor = anchorFor(state, mc.level);
        anchor.center = center;
        Vec3[] basis = basis(state);
        anchor.right = basis[0];
        anchor.up = basis[1];
        anchor.forward = basis[2].scale(state.variant == 1 ? -1.0 : 1.0);
        anchor.scale = baseScale;
        anchor.trail = trail;
        anchor.variant = state.variant;

        // Pieces layer: one torn tendril bowed along the crescent arc
        // forms the wave body, tip to tip across the cut basis, bowed
        // forward and dipped at the belly in the variant lip color.
        int arcLip = state.variant == 1 ? 0x8CCCFF : 0x00DBFF;
        Vec3 arcFrom = anchor.right.scale(-2.0 * baseScale).add(anchor.up.scale(0.38 * baseScale));
        Vec3 arcTo = anchor.right.scale(2.0 * baseScale).add(anchor.up.scale(0.38 * baseScale));
        Vec3 arcControl = anchor.forward.scale(1.05 * baseScale).add(anchor.up.scale(-0.62 * baseScale));
        ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) ->
            ComplexGeometry.tendril(pose, buffer, state.seed ^ 0xC3E5, arcFrom, arcControl, arcTo,
                0.36f * baseScale, 0xFFFFFF, arcLip, 0.92f, fade, dist));

        // Attack upgrade: the crescent gains its second lip. A mirror
        // tendril bows off the opposite side of the same two tips,
        // seeded apart and at 60 percent of the main arc's alpha, so
        // the cut reads as a bladed crescent with two edges instead of
        // one floating strand.
        Vec3 arcControl2 = anchor.forward.scale(-0.88 * baseScale).add(anchor.up.scale(0.55 * baseScale));
        ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) ->
            ComplexGeometry.tendril(pose, buffer, state.seed ^ 0x5A17, arcFrom, arcControl2, arcTo,
                0.30f * baseScale, 0xFFFFFF, arcLip, 0.55f, fade, dist));

        // Attack upgrade: steel shards ride the arc's leading edge at
        // the swing's peak tick, three seeded clusters of two along the
        // belly of the cut (six shards, size class S). Each cluster
        // sheds along the arc's own tangent at its point on the curve,
        // the way real fragments leave a moving edge, and the shards
        // are drawn lit so the steel reads as physical metal, not glow.
        if (impact > 0.02f) {
            final float shardEnv = fade * impact;
            final int shardLight = state.lightCoords;
            ComplexGeometry.submitSolidLit(collector, poseStack, shardLight, (pose, buffer) -> {
                float[] edgeT = {0.32f, 0.5f, 0.68f};
                for (int i = 0; i < edgeT.length; i++) {
                    float t = edgeT[i];
                    float s = 1.0f - t;
                    Vec3 edge = arcFrom.scale(s * s)
                        .add(arcControl.scale(2 * s * t))
                        .add(arcTo.scale(t * t));
                    Vec3 tangent = arcControl.subtract(arcFrom).scale(2 * s)
                        .add(arcTo.subtract(arcControl).scale(2 * t));
                    ComplexGeometry.shardBurst(pose, buffer, state.seed ^ (0x51B0 + i * 0x9E37L),
                        edge, tangent, 38.0f, 0.34f * baseScale, 2,
                        0xFF8E9DAE, 0xFFF2F8FF, 0.95f, shardEnv, dist);
                }
            });
        }

        // Attack upgrade: when the swing is aimed low and the cut runs
        // close over the ground, the arc's contact bites a short crack
        // fan into the ground under the belly, flashing hardest at the
        // strike tick. crackFan exposes no branch count parameter (see
        // the change request in the owner report), so the fan is kept
        // short by radius.
        if (state.xRot >= 20.0f) {
            Vec3 contact = center.add(anchor.forward.scale(0.55 * baseScale));
            int groundY = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (int) Math.floor(contact.x), (int) Math.floor(contact.z));
            double drop = state.wy - groundY;
            if (drop > 0.0 && drop <= 2.2 * baseScale + 0.5) {
                final float crackEnv = fade * open * (0.25f + 0.75f * impact);
                poseStack.pushPose();
                poseStack.translate(anchor.forward.x * 0.55 * baseScale, -drop + 0.03,
                    anchor.forward.z * 0.55 * baseScale);
                ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) ->
                    ComplexGeometry.crackFan(pose, buffer, state.seed ^ 0xC9A1, 0.85f * baseScale,
                        0xFFFFFF, arcLip, 0.85f, crackEnv, dist, 3));
                poseStack.popPose();
            }
        }

        float delta = anchor.deltaSeconds(mc.isPaused());
        if (delta > 0f) {
            float envelope = fade * dist;
            anchor.plasma.tick(delta, envelope);
            anchor.core.tick(delta, envelope);
            anchor.lip.tick(delta, envelope);
            anchor.vapor.tick(delta, envelope * 0.8f);
            anchor.sparkStreaks.tick(delta, envelope);
            if (impact > 0.02f) anchor.sparks.tick(delta, impact * dist);
        }

        float strikeAge = strikeFrac * state.maxLifetime;
        if (!anchor.strikeBurstDone && anchor.lastAge >= 0f
                && anchor.lastAge < strikeAge && state.age >= strikeAge && state.strikeTick >= 0) {
            anchor.strikeBurstDone = true;
            int lipColor = state.variant == 1 ? 0x8CCCFF : 0x00DBFF;
            MaterialEmitters.burst(mc.level,
                MaterialEmitters.Preset.plasma(0xFFFFFF, 2.0f, anchor.seed ^ 0x51A7, Vec3.ZERO, 0.16f)
                    .withLifetimeFactor(0.6f),
                MaterialEmitters.sphereVolume(center, 0.55 * baseScale), 22);
            MaterialEmitters.burst(mc.level,
                MaterialEmitters.Preset.plasma(lipColor, 2.4f, anchor.seed ^ 0x51A8, Vec3.ZERO, 0.20f)
                    .withLifetimeFactor(0.6f),
                MaterialEmitters.sphereVolume(center, 0.70 * baseScale), 14);
            MaterialEmitters.burst(mc.level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK, Vec3.ZERO, 0.24f),
                MaterialEmitters.point(center), 16);
            MaterialEmitters.burst(mc.level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.22f),
                MaterialEmitters.point(center), 12);
        }
        anchor.lastAge = state.age;
        super.submit(state, poseStack, collector, cameraState);
    }

    /** Sine flash peaking at the strike fraction of life, gone by twice it. */
    private static float strike(float progress, float strikeFrac) {
        float window = Math.min(1.0f, strikeFrac * 2.0f);
        if (progress >= window) return 0.0f;
        return (float) Math.sin((progress / window) * Math.PI);
    }

    private static Vec3[] basis(SwiftCrescentRenderState state) {
        double yaw = Math.toRadians(state.yRot);
        double pitch = Math.toRadians(state.xRot);
        double tilt = Math.toRadians(state.tiltAngle);
        double cosYaw = Math.cos(yaw), sinYaw = Math.sin(yaw);
        double cosPitch = Math.cos(pitch), sinPitch = Math.sin(pitch);
        double cosTilt = Math.cos(tilt), sinTilt = Math.sin(tilt);
        Vec3 right = new Vec3(
            cosYaw * cosTilt - sinYaw * sinPitch * sinTilt,
            cosPitch * sinTilt,
            sinYaw * cosTilt + cosYaw * sinPitch * sinTilt);
        Vec3 up = new Vec3(
            -cosYaw * sinTilt - sinYaw * sinPitch * cosTilt,
            cosPitch * cosTilt,
            -sinYaw * sinTilt + cosYaw * sinPitch * cosTilt);
        Vec3 forward = new Vec3(-sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
        return new Vec3[]{right, up, forward};
    }

    private static Anchor anchorFor(SwiftCrescentRenderState state, ClientLevel level) {
        long now = System.nanoTime();
        Anchor anchor = ANCHORS.get(state.uuid);
        if (anchor == null || anchor.level != level) {
            anchor = new Anchor(level, (int) state.seed, state.variant);
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
        private float lastAge = -1f;
        private boolean strikeBurstDone;
        private Vec3 center = Vec3.ZERO;
        private Vec3 right = new Vec3(1, 0, 0);
        private Vec3 up = new Vec3(0, 1, 0);
        private Vec3 forward = new Vec3(0, 0, 1);
        private float scale = 1f;
        private float trail = 0f;
        private int variant = 0;
        private final MaterialEmitters.Emitter plasma;
        private final MaterialEmitters.Emitter core;
        private final MaterialEmitters.Emitter lip;
        private final MaterialEmitters.Emitter vapor;
        private final MaterialEmitters.Emitter sparks;
        private final MaterialEmitters.Emitter sparkStreaks;

        private Anchor(ClientLevel level, int seed, int variant) {
            this.level = level;
            this.seed = seed;
            this.variant = variant;
            MaterialEmitters.Source cutVolume = random -> {
                double z = random.nextDouble() * 2.0 - 1.0;
                double ang = random.nextDouble() * Math.PI * 2.0;
                double rr = Math.cbrt(random.nextDouble());
                double flat = Math.sqrt(Math.max(0.0, 1.0 - z * z)) * rr;
                double lx = Math.cos(ang) * flat * 2.15 * scale;
                double ly = Math.sin(ang) * flat * 1.05 * scale;
                double lz = z * 0.28 * scale;
                Vec3 pos = center.add(right.scale(lx)).add(up.scale(ly)).add(forward.scale(lz));
                Vec3 vel = forward.scale(-0.06)
                    .add(right.scale(lx * 0.018))
                    .add(up.scale(ly * 0.018));
                return new MaterialEmitters.Sample(pos, vel);
            };
            MaterialEmitters.Source wake = random -> {
                double t = random.nextDouble();
                Vec3 pos = center
                    .add(forward.scale((0.30 - t * (0.60 + trail * 2.0)) * scale))
                    .add(right.scale((random.nextDouble() * 2.0 - 1.0) * 1.55 * scale))
                    .add(up.scale((random.nextDouble() * 2.0 - 1.0) * 0.72 * scale));
                return new MaterialEmitters.Sample(pos, forward.scale(-0.16));
            };
            int lipColor = variant == 1 ? 0x8CCCFF : 0x00DBFF;
            // Merge wiring: the host lives 10 ticks, so the material
            // lifetimes are factored down toward the host window (plasma
            // 0.6, wake vapor 0.45) instead of trailing up to 54 ticks of
            // afterglow after the crescent is gone.
            this.plasma = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xDBF2FF, 2.6f, seed, Vec3.ZERO, 0.025f)
                    .withLifetimeFactor(0.6f),
                cutVolume, 175f, 200);
            this.core = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xFFFFFF, 1.8f, seed ^ 0x1111, Vec3.ZERO, 0.018f)
                    .withLifetimeFactor(0.6f),
                cutVolume, 95f, 115);
            this.lip = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(lipColor, 2.9f, seed ^ 0x2222, Vec3.ZERO, 0.025f)
                    .withLifetimeFactor(0.6f),
                cutVolume, 70f, 90);
            this.vapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(0xDBF2FF, 3.4f, seed ^ 0x3333, Vec3.ZERO, 0.020f)
                    .withLifetimeFactor(0.45f),
                wake, 48f, 64);
            this.sparks = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK, Vec3.ZERO, 0.16f),
                random -> new MaterialEmitters.Sample(center, Vec3.ZERO), 24f, 32);
            // Complex particle layer: spark streaks shed across the cut
            // volume at the rim, inside the 40 per second budget.
            this.sparkStreaks = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.14f),
                cutVolume, 26f, 34);
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
