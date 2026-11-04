package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.HollowAfterimageEntity;
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
import java.util.Map;

/**
 * Entity Renderer for Hollow Afterimage in Minecraft 26.2.
 * No-shapes pass: the ghost is no longer drawn as geometry at all. It is a
 * dissolving humanoid presence of soul and darkness particles emitted
 * inside the volume the body occupies. The hair cap reads from a denser
 * soul stream in the caster's synced hair color at head height, the eye
 * slit from a small bright plasma cluster at the face, and the scanline
 * character from six thin vapor bands at the old slice heights. The
 * public entity contract is untouched: same synced skin and hair colors,
 * same seed source (the entity UUID), same 100 tick fade curve, and the
 * Dark Faded behavior in the entity is unchanged, so Katana and Saber
 * afterimages read exactly the same way.
 */
public class HollowAfterimageRenderer extends EntityRenderer<HollowAfterimageEntity, HollowAfterimageRenderer.AfterimageRenderState> {

    public HollowAfterimageRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class AfterimageRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public int skinColor = 0xFF8CC8FF;
        public int hairColor = 0xFF1EB4FF;
        public float age = 0;
        public long seed = 0;
        public double ex, ey, ez;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(HollowAfterimageEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(HollowAfterimageEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public AfterimageRenderState createRenderState() {
        return new AfterimageRenderState();
    }

    @Override
    public void extractRenderState(HollowAfterimageEntity entity, AfterimageRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getSyncYaw();
        state.xRot = entity.getSyncPitch();
        state.skinColor = entity.getSkinColor();
        state.hairColor = entity.getHairColor();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
    }

    /** Per-entity emission streams, keyed by the entity seed. Emission points are data only. */
    private static final class Streams {
        final ClientLevel level;
        final MaterialEmitters.Emitter dark;
        final MaterialEmitters.Emitter soul;
        final MaterialEmitters.Emitter hair;
        final MaterialEmitters.Emitter eye;
        final MaterialEmitters.Emitter scan;
        final MaterialEmitters.Emitter wake;
        final MaterialEmitters.Emitter ground;
        long lastNanos = 0L;
        long lastSeenNanos = 0L;
        Vec3 center = Vec3.ZERO;
        Vec3 front = new Vec3(0, 0, -1);
        Vec3 right = new Vec3(1, 0, 0);
        float age = 0f;
        float spread = 1f;
        float ringR = 0.5f;

        Streams(ClientLevel level, long seed, int bodyArgb, int hairArgb) {
            this.level = level;
            int s = (int) seed;
            this.dark = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.darkness(bodyArgb, 2.3f, s ^ 0x11, new Vec3(0, 0.02, 0), 0.015f),
                random -> {
                    Vec3 c = center.add(0, 0.95, 0);
                    return new MaterialEmitters.Sample(new Vec3(
                        c.x + (random.nextDouble() * 2.0 - 1.0) * 0.30 * spread,
                        c.y + (random.nextDouble() * 2.0 - 1.0) * 0.95,
                        c.z + (random.nextDouble() * 2.0 - 1.0) * 0.30 * spread), Vec3.ZERO);
                }, 100f, 110);
            this.soul = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.soul(0x1AD9FF, 1.5f, s ^ 0x22, new Vec3(0, 0.05, 0), 0.02f),
                random -> {
                    Vec3 c = center.add(0, 0.92, 0);
                    return new MaterialEmitters.Sample(new Vec3(
                        c.x + (random.nextDouble() * 2.0 - 1.0) * 0.27 * spread,
                        c.y + (random.nextDouble() * 2.0 - 1.0) * 0.92,
                        c.z + (random.nextDouble() * 2.0 - 1.0) * 0.27 * spread), Vec3.ZERO);
                }, 80f, 90);
            this.hair = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.soul(hairArgb, 1.15f, s ^ 0x33, new Vec3(0, 0.03, 0), 0.012f),
                random -> {
                    Vec3 c = center.add(front.scale(0.04)).add(0, 1.56, 0);
                    return new MaterialEmitters.Sample(new Vec3(
                        c.x + (random.nextDouble() * 2.0 - 1.0) * 0.24,
                        c.y + (random.nextDouble() * 2.0 - 1.0) * 0.17,
                        c.z + (random.nextDouble() * 2.0 - 1.0) * 0.24), Vec3.ZERO);
                }, 38f, 44);
            this.eye = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xD9FBFF, 0.5f, s ^ 0x44, Vec3.ZERO, 0.004f),
                random -> {
                    Vec3 c = center.add(right.scale(-0.12)).add(0, 1.60, 0).add(front.scale(0.20));
                    return new MaterialEmitters.Sample(new Vec3(
                        c.x + (random.nextDouble() * 2.0 - 1.0) * 0.05,
                        c.y + (random.nextDouble() * 2.0 - 1.0) * 0.03,
                        c.z + (random.nextDouble() * 2.0 - 1.0) * 0.05), Vec3.ZERO);
                }, 26f, 30);
            this.scan = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(0x00F2FF, 1.15f, s ^ 0x55, Vec3.ZERO, 0.006f),
                random -> {
                    int band = random.nextInt(6);
                    float y = 0.15f + band * 0.32f + (float) Math.sin(age * 0.25f + band) * 0.06f;
                    float sway = (float) Math.sin(age * 0.5f + band * 1.5f) * 0.06f;
                    Vec3 p = center.add(right.scale(sway + (random.nextDouble() * 2.0 - 1.0) * 0.35))
                        .add(0, y, 0).add(front.scale((random.nextDouble() * 2.0 - 1.0) * 0.25));
                    return new MaterialEmitters.Sample(p, Vec3.ZERO);
                }, 60f, 70);
            this.wake = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0x1AD9FF, 1.3f, s ^ 0x61, Vec3.ZERO, 0.015f),
                random -> {
                    double t = random.nextDouble();
                    Vec3 p = center.add(front.scale(-0.10 - 0.85 * t))
                        .add(0, 0.55 + 0.45 * t + (random.nextDouble() * 2.0 - 1.0) * 0.25, 0)
                        .add(right.scale((random.nextDouble() * 2.0 - 1.0) * 0.15));
                    return new MaterialEmitters.Sample(p, front.scale(-0.05));
                }, 46f, 55);
            this.ground = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(0x00D9FF, 1.6f, s ^ 0x71, new Vec3(0, 0.01, 0), 0.01f),
                random -> {
                    double ang = random.nextDouble() * Math.PI * 2.0;
                    double rr = ringR * Math.sqrt(random.nextDouble());
                    return new MaterialEmitters.Sample(new Vec3(
                        center.x + Math.cos(ang) * rr, center.y + 0.04, center.z + Math.sin(ang) * rr), Vec3.ZERO);
                }, 30f, 36);
        }
    }

    private final Map<Long, Streams> streams = new HashMap<>();

    @Override
    public void submit(AfterimageRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        // The entity lives 100 ticks; this used to stop at 80, leaving the last
        // second invisible while the afterimage still applied its debuff.
        float maxLife = 100.0f;
        float progress = Math.min(1.0f, state.age / maxLife);
        if (progress >= 1.0f) {
            streams.remove(state.seed);
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        float alpha = Math.max(0.05f, 0.90f * (1.0f - (progress * progress)));
        float pulse = 0.85f + 0.15f * (float) Math.sin(state.age * 0.35f);
        Vec3 center = new Vec3(state.ex, state.ey, state.ez);
        float ds = MaterialEmitters.distanceScale(center);
        float env = alpha * pulse * ds;
        if (env <= 0.01f) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        // Body colors, unchanged from the box era: the spectral body is
        // tinted by the caster's synced skin color, the cap by hair color.
        float skinR = KiRenderHelper.red(state.skinColor);
        float skinG = KiRenderHelper.green(state.skinColor);
        float skinB = KiRenderHelper.blue(state.skinColor);
        int bodyArgb = ((int) ((0.10f + skinR * 0.45f) * 255f) << 16)
            | ((int) ((0.04f + skinG * 0.30f) * 255f) << 8)
            | (int) ((0.22f + skinB * 0.45f) * 255f);
        int hairArgb = state.hairColor & 0xFFFFFF;

        Streams found = streams.get(state.seed);
        if (found == null || found.level != level) {
            found = new Streams(level, state.seed, bodyArgb, hairArgb);
            streams.put(state.seed, found);
        }
        final Streams s = found;
        long now = System.nanoTime();
        float delta = s.lastNanos == 0L ? 1f / 60f : Math.min(0.05f, (now - s.lastNanos) / 1_000_000_000f);
        s.lastNanos = now;
        s.lastSeenNanos = now;
        streams.values().removeIf(other -> other != s && now - other.lastSeenNanos > 4_000_000_000L);

        double yawRad = Math.toRadians(state.yRot);
        s.center = center;
        s.front = new Vec3(Math.sin(yawRad), 0, -Math.cos(yawRad));
        s.right = new Vec3(Math.cos(yawRad), 0, Math.sin(yawRad));
        s.age = state.age;
        // The presence dissolves outward as it dies: the emission volume
        // widens while the envelope thins, on the old fade curve.
        s.spread = 1.0f + progress * 0.35f;
        s.ringR = 0.5f + (state.age * 0.03f) % 0.8f;

        s.dark.tick(delta, env);
        s.soul.tick(delta, env * 0.9f);
        s.hair.tick(delta, env * 0.9f);
        s.eye.tick(delta, env * 0.95f);
        s.scan.tick(delta, env * 0.65f);
        s.wake.tick(delta, env * 0.45f);
        float ringA = alpha * (1.0f - (s.ringR / 1.3f));
        if (ringA > 0.01f) {
            s.ground.tick(delta, ringA * pulse * ds);
        }

        super.submit(state, poseStack, collector, cameraState);
    }
}
