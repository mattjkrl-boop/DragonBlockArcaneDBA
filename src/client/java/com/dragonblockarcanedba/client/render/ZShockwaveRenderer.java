package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.ZShockwaveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Entity Renderer for Z-Sword Massive Kinetic Shockwave in Minecraft 26.2.
 *
 * No-shapes pass: the wave draws no geometry. Its span, chord, and
 * height survive only as emission data in the entity orientation
 * frame. Dense gold gravity plasma carries the front, a narrower
 * near-white plasma stream carries the hot center, pale vapor trails
 * in the wake, and stone dust is compressed along the lower edge. The
 * old apex burst becomes a short plasma concentration at the chord
 * point on the strike envelope. Fade, growth, first-person scaling,
 * orientation, and seed behavior carry over from the bespoke renderer.
 */
public class ZShockwaveRenderer extends EntityRenderer<ZShockwaveEntity, ZShockwaveRenderer.ZShockwaveRenderState> {

    private static final int GOLD = 0xFFD133;
    private static final int CORE = 0xFFFFEB;
    private static final int PALE_GOLD = 0xFFE9A8;
    private static final int STONE_DUST = 0x9A8668;

    private final Map<Integer, Emission> emissions = new HashMap<>();

    public ZShockwaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ZShockwaveRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public boolean isSubWave = false;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public long seed = 0;
        public boolean isFirstPersonOwner = false;
        public int entityId = -1;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(ZShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(ZShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ZShockwaveRenderState createRenderState() {
        return new ZShockwaveRenderState();
    }

    @Override
    public void extractRenderState(ZShockwaveEntity entity, ZShockwaveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.isSubWave = entity.isSubWave();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getSeed();
        state.entityId = entity.getId();

        Minecraft mc = Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null &&
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) &&
            mc.options.getCameraType().isFirstPerson());
    }

    @Override
    public void submit(ZShockwaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft == null ? null : minecraft.level;
        if (level == null) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        float charge = state.chargeRatio;
        boolean sub = state.isSubWave;
        float age = state.age;

        float fadeIn = smooth(Math.min(1.0f, age / 6.0f));
        float fadeOut = age <= 55.0f ? 1.0f : 1.0f - smooth((age - 55.0f) / 25.0f);
        float fade = fadeIn * Math.max(0.0f, fadeOut);
        float impact = strike(Math.min(1.0f, age / 18.0f));
        Vec3 center = new Vec3(state.x, state.y, state.z);
        float dist = MaterialEmitters.distanceScale(center);
        if (fade <= 0.01f || dist <= 0.01f) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        float fpScale = (state.isFirstPersonOwner && state.age < 6.0f) ? (0.55f + (state.age / 6.0f) * 0.45f) : 1.0f;
        float grow = 0.8f + 0.2f * fadeIn;
        float span = (sub ? (3.2f + charge * 3.2f) : (4.8f + charge * 6.2f)) * fpScale * grow;
        float chord = (sub ? (1.0f + charge * 0.6f) : (1.6f + charge * 1.4f)) * fpScale;
        float height = (sub ? (0.9f + charge * 0.8f) : (1.5f + charge * 1.8f)) * fpScale;

        Matrix4f rotation = new Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot));
        rotation.mul(new Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));
        Vector3f rightV = rotation.transformDirection(new Vector3f(1.0f, 0.0f, 0.0f));
        Vector3f upV = rotation.transformDirection(new Vector3f(0.0f, 1.0f, 0.0f));
        Vector3f forwardV = rotation.transformDirection(new Vector3f(0.0f, 0.0f, 1.0f));

        Emission emission = emissionFor(state, level);
        emission.frame.center = center;
        emission.frame.right = new Vec3(rightV.x, rightV.y, rightV.z);
        emission.frame.up = new Vec3(upV.x, upV.y, upV.z);
        emission.frame.forward = new Vec3(forwardV.x, forwardV.y, forwardV.z);
        emission.frame.span = span;
        emission.frame.chord = chord;
        emission.frame.height = height;
        emission.frame.bandZ = chord * 0.6f;

        float delta = emission.nextDelta();
        float envelope = fade * dist;
        emission.front.tick(delta, envelope);
        emission.core.tick(delta, envelope);
        emission.wake.tick(delta, envelope * 0.78f);
        emission.dust.tick(delta, envelope * 0.72f);
        emission.apex.tick(delta, impact * envelope);

        super.submit(state, poseStack, collector, cameraState);
    }

    private Emission emissionFor(ZShockwaveRenderState state, ClientLevel level) {
        long now = System.nanoTime();
        emissions.entrySet().removeIf(entry -> now - entry.getValue().lastSeenNanos > 5_000_000_000L);
        Emission emission = emissions.get(state.entityId);
        if (emission == null || emission.seed != state.seed) {
            emission = new Emission(level, state.seed);
            emissions.put(state.entityId, emission);
        }
        emission.lastSeenNanos = now;
        return emission;
    }

    private static final class Frame {
        Vec3 center = Vec3.ZERO;
        Vec3 right = new Vec3(1.0, 0.0, 0.0);
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        Vec3 forward = new Vec3(0.0, 0.0, 1.0);
        float span = 1.0f;
        float chord = 1.0f;
        float height = 1.0f;
        float bandZ = 0.0f;
    }

    private static final class Emission {
        final long seed;
        final Frame frame = new Frame();
        final MaterialEmitters.Emitter front;
        final MaterialEmitters.Emitter core;
        final MaterialEmitters.Emitter wake;
        final MaterialEmitters.Emitter dust;
        final MaterialEmitters.Emitter apex;
        long lastSeenNanos = System.nanoTime();
        long lastTickNanos = 0L;

        Emission(ClientLevel level, long seed) {
            this.seed = seed;
            int baseSeed = (int) seed;

            MaterialEmitters.Source frontSource = random -> sampleFront(random);
            MaterialEmitters.Source coreSource = random -> sampleCore(random);
            MaterialEmitters.Source wakeSource = random -> sampleWake(random);
            MaterialEmitters.Source dustSource = random -> sampleDust(random);
            MaterialEmitters.Source apexSource = random -> new MaterialEmitters.Sample(
                local(0.0, 0.0, frame.chord), frame.forward.scale(0.10));

            front = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(GOLD, 2.85f, baseSeed, Vec3.ZERO, 0.010f),
                frontSource, 188.0f, 380);
            core = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(CORE, 1.75f, baseSeed ^ 0xC0DE, Vec3.ZERO, 0.008f),
                coreSource, 96.0f, 210);
            wake = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(PALE_GOLD, 3.30f, baseSeed ^ 0xACE1, Vec3.ZERO, 0.010f),
                wakeSource, 84.0f, 180);
            dust = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(STONE_DUST, 1.75f, baseSeed ^ 0xD057, Vec3.ZERO, 0.010f),
                dustSource, 60.0f, 135);
            apex = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(CORE, 2.05f, baseSeed ^ 0xA9E5, Vec3.ZERO, 0.16f),
                apexSource, 126.0f, 260);
        }

        Vec3 local(double x, double y, double z) {
            return frame.center.add(frame.right.scale(x)).add(frame.up.scale(y)).add(frame.forward.scale(z));
        }

        Vec3 localVelocity(double x, double y, double z) {
            return frame.right.scale(x).add(frame.up.scale(y)).add(frame.forward.scale(z));
        }

        MaterialEmitters.Sample sampleFront(RandomSource random) {
            double x = (random.nextDouble() * 2.0 - 1.0) * frame.span * 0.50;
            double y = random.nextDouble() * frame.height;
            double z = frame.bandZ + (random.nextDouble() - 0.5) * 0.38;
            Vec3 velocity = localVelocity(x / Math.max(0.01, frame.span) * 0.10, 0.020, 0.22);
            return new MaterialEmitters.Sample(local(x, y, z), velocity);
        }

        MaterialEmitters.Sample sampleCore(RandomSource random) {
            double x = (random.nextDouble() * 2.0 - 1.0) * frame.span * 0.15;
            double y = random.nextDouble() * frame.height * 0.78;
            double z = frame.bandZ + 0.05 + (random.nextDouble() - 0.5) * 0.25;
            return new MaterialEmitters.Sample(local(x, y, z), localVelocity(0.0, 0.012, 0.28));
        }

        MaterialEmitters.Sample sampleWake(RandomSource random) {
            double x = (random.nextDouble() * 2.0 - 1.0) * frame.span * 0.46;
            double y = random.nextDouble() * frame.height * 0.48;
            double z = frame.bandZ - 0.85 + (random.nextDouble() - 0.5) * 0.55;
            return new MaterialEmitters.Sample(local(x, y, z), localVelocity(0.0, 0.015, 0.10));
        }

        MaterialEmitters.Sample sampleDust(RandomSource random) {
            double x = (random.nextDouble() * 2.0 - 1.0) * frame.span * 0.50;
            double y = random.nextDouble() * 0.28;
            double z = frame.bandZ + (random.nextDouble() - 0.5) * 0.70;
            Vec3 velocity = localVelocity(x / Math.max(0.01, frame.span) * 0.16, 0.030, 0.25);
            return new MaterialEmitters.Sample(local(x, y, z), velocity);
        }

        float nextDelta() {
            long now = System.nanoTime();
            if (lastTickNanos == 0L) {
                lastTickNanos = now;
                return 1.0f / 60.0f;
            }
            float delta = Math.min(0.05f, (now - lastTickNanos) / 1_000_000_000.0f);
            lastTickNanos = now;
            return Math.max(0.001f, delta);
        }
    }

    private static float smooth(float t) {
        float x = Math.max(0.0f, Math.min(1.0f, t));
        return x * x * (3.0f - 2.0f * x);
    }

    private static float strike(float progress) {
        if (progress >= 0.7f) return 0.0f;
        return (float) Math.sin((progress / 0.7f) * Math.PI);
    }
}
