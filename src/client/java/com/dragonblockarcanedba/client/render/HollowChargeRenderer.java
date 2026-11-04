package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.HollowChargeEntity;
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
 * Entity Renderer for Hollow Rush Charge-up in Minecraft 26.2.
 * No-shapes pass: the containment rings, converging shards, singularity
 * octahedrons, and helical ribbons are gone. The charge reads as material
 * in motion: void plasma sampled through the charge volume with a velocity
 * toward the core, so the stream visibly converges as it is emitted,
 * darkness smoke thickening around the caster, soul vapor spiraling up
 * the old ribbon paths, a dense plasma core at the singularity point, and
 * violet dust crawling across the ground the rings used to anchor. The
 * charge ratio still drives every rate and radius, on the same curve.
 */
public class HollowChargeRenderer extends EntityRenderer<HollowChargeEntity, HollowChargeRenderer.ChargeRenderState> {

    public HollowChargeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ChargeRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public float age = 0;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(HollowChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(HollowChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ChargeRenderState createRenderState() {
        return new ChargeRenderState();
    }

    @Override
    public void extractRenderState(HollowChargeEntity entity, ChargeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    /** Per-entity emission streams, keyed by the entity seed. Emission points are data only. */
    private static final class Streams {
        final ClientLevel level;
        final MaterialEmitters.Emitter gather;
        final MaterialEmitters.Emitter dark;
        final MaterialEmitters.Emitter swirl;
        final MaterialEmitters.Emitter core;
        final MaterialEmitters.Emitter coreWhite;
        final MaterialEmitters.Emitter ground;
        long lastNanos = 0L;
        long lastSeenNanos = 0L;
        Vec3 center = Vec3.ZERO;
        float charge = 0f;
        float age = 0f;
        float baseRadius = 1.4f;
        float pulse = 1f;

        private Vec3 core() {
            return center.add(0, 0.9, 0);
        }

        Streams(ClientLevel level, long seed) {
            this.level = level;
            int s = (int) seed;
            this.gather = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xA60DF2, 1.5f, s ^ 0x21, Vec3.ZERO, 0.01f),
                random -> {
                    Vec3 c = core();
                    double orbit = (2.2 - charge * 0.7) * pulse * (0.55 + 0.45 * random.nextDouble());
                    double theta = random.nextDouble() * Math.PI * 2.0;
                    double yOff = (random.nextDouble() * 2.0 - 1.0) * 1.1;
                    Vec3 p = new Vec3(c.x + Math.cos(theta) * orbit, c.y + yOff, c.z + Math.sin(theta) * orbit);
                    Vec3 toCore = c.subtract(p);
                    double len = toCore.length();
                    Vec3 vel = len < 1e-4 ? Vec3.ZERO : toCore.scale((0.55 + charge * 0.85) / len);
                    return new MaterialEmitters.Sample(p, vel);
                }, 130f, 150);
            this.dark = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.darkness(0x2E004D, 2.6f, s ^ 0x31, new Vec3(0, 0.02, 0), 0.02f),
                random -> {
                    Vec3 c = core();
                    double r = baseRadius * 0.8;
                    return new MaterialEmitters.Sample(new Vec3(
                        c.x + (random.nextDouble() * 2.0 - 1.0) * r,
                        c.y + (random.nextDouble() * 2.0 - 1.0) * 1.0,
                        c.z + (random.nextDouble() * 2.0 - 1.0) * r), Vec3.ZERO);
                }, 44f, 50);
            this.swirl = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.soul(0x7305F2, 1.25f, s ^ 0x41, Vec3.ZERO, 0.01f),
                random -> {
                    double t = random.nextDouble();
                    double r = baseRadius * 0.65 * (0.8 + 0.3 * Math.sin(t * Math.PI));
                    double ang = (random.nextBoolean() ? 0.0 : Math.PI) + t * Math.PI * 2.5 + age * 0.18;
                    Vec3 p = new Vec3(center.x + Math.cos(ang) * r, center.y + t * 2.2, center.z + Math.sin(ang) * r);
                    Vec3 tangent = new Vec3(-Math.sin(ang), 0, Math.cos(ang)).scale(0.30);
                    return new MaterialEmitters.Sample(p, tangent.add(0, 0.12, 0));
                }, 70f, 80);
            this.core = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0x26E6FF, 1.1f, s ^ 0x51, Vec3.ZERO, 0.03f),
                random -> {
                    Vec3 c = core();
                    double coreSize = (0.75 - charge * 0.25) * pulse * 0.55;
                    return new MaterialEmitters.Sample(new Vec3(
                        c.x + (random.nextDouble() * 2.0 - 1.0) * coreSize,
                        c.y + (random.nextDouble() * 2.0 - 1.0) * coreSize,
                        c.z + (random.nextDouble() * 2.0 - 1.0) * coreSize), Vec3.ZERO);
                }, 60f, 70);
            this.coreWhite = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xFFFFFF, 0.8f, s ^ 0x61, Vec3.ZERO, 0.02f),
                random -> {
                    Vec3 c = core();
                    double coreSize = (0.75 - charge * 0.25) * pulse * 0.25;
                    return new MaterialEmitters.Sample(new Vec3(
                        c.x + (random.nextDouble() * 2.0 - 1.0) * coreSize,
                        c.y + (random.nextDouble() * 2.0 - 1.0) * coreSize,
                        c.z + (random.nextDouble() * 2.0 - 1.0) * coreSize), Vec3.ZERO);
                }, 40f, 48);
            this.ground = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(0x5904A6, 1.7f, s ^ 0x71, Vec3.ZERO, 0.015f),
                random -> {
                    double ang = random.nextDouble() * Math.PI * 2.0;
                    double rr = baseRadius * 1.25 * Math.sqrt(random.nextDouble());
                    return new MaterialEmitters.Sample(new Vec3(
                        center.x + Math.cos(ang) * rr, center.y + 0.05, center.z + Math.sin(ang) * rr), Vec3.ZERO);
                }, 34f, 40);
        }
    }

    private final Map<Long, Streams> streams = new HashMap<>();

    @Override
    public void submit(ChargeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        float charge = state.chargeRatio;
        Vec3 center = new Vec3(state.x, state.y, state.z);
        float ds = MaterialEmitters.distanceScale(center);
        if (ds <= 0.01f) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        Streams found = streams.get(state.seed);
        if (found == null || found.level != level) {
            found = new Streams(level, state.seed);
            streams.put(state.seed, found);
        }
        final Streams s = found;
        long now = System.nanoTime();
        float delta = s.lastNanos == 0L ? 1f / 60f : Math.min(0.05f, (now - s.lastNanos) / 1_000_000_000f);
        s.lastNanos = now;
        s.lastSeenNanos = now;
        streams.values().removeIf(other -> other != s && now - other.lastSeenNanos > 4_000_000_000L);

        s.center = center;
        s.charge = charge;
        s.age = state.age;
        s.baseRadius = 1.4f + charge * 1.6f;
        s.pulse = 0.88f + 0.12f * (float) Math.sin(state.age * 0.45f);

        s.gather.tick(delta, (0.30f + 0.70f * charge) * ds);
        s.dark.tick(delta, (0.50f + 0.50f * charge) * ds);
        s.swirl.tick(delta, ds);
        s.core.tick(delta, ds);
        if (charge >= 0.5f) {
            // The white heart of the old singularity matrix only appeared
            // past half charge; the gate is unchanged.
            s.coreWhite.tick(delta, ds);
        }
        s.ground.tick(delta, ds);

        super.submit(state, poseStack, collector, cameraState);
    }
}
