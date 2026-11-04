package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.ZChargeEntity;
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
 * Entity Renderer for Z-Sword Shockwave Charging in Minecraft 26.2.
 *
 * No-shapes pass: the charge draws no geometry. Gravity plasma, pale
 * vapor, and stone dust are sampled across the ground and through the
 * funnel volume by the shared material emitters, always moving toward
 * the sword core. Levitating crystal material survives as dense bright
 * plasma glints inside the same inward stream, and the overdrive stage
 * adds faster core sparks. Lifecycle, radius, and seed behavior carry
 * over from the bespoke renderer.
 */
public class ZChargeRenderer extends EntityRenderer<ZChargeEntity, ZChargeRenderer.ZChargeRenderState> {

    private static final int GOLD = 0xFFD133;
    private static final int BRIGHT_GOLD = 0xFFF2A6;
    private static final int PALE_GOLD = 0xFFE9A8;
    private static final int STONE_DUST = 0x9A8668;

    private final Map<Integer, Emission> emissions = new HashMap<>();

    public ZChargeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ZChargeRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public float age = 0;
        public long seed = 0;
        public int fadeTicks = -1;
        public int entityId = -1;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(ZChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(ZChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ZChargeRenderState createRenderState() {
        return new ZChargeRenderState();
    }

    @Override
    public void extractRenderState(ZChargeEntity entity, ZChargeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getSeed();
        state.fadeTicks = entity.getFadeTicks();
        state.entityId = entity.getId();
    }

    @Override
    public void submit(ZChargeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft == null ? null : minecraft.level;
        if (level == null) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        float charge = state.chargeRatio;
        float age = state.age;
        Vec3 center = new Vec3(state.x, state.y, state.z);
        float dist = MaterialEmitters.distanceScale(center);

        float baseRadius = 1.2f + (charge * 2.2f);
        float fadeOut = state.fadeTicks < 0 ? 1.0f
            : Math.max(0.0f, 1.0f - state.fadeTicks / (float) ZChargeEntity.FADE_LENGTH);
        float baseAlpha = (0.70f + (charge * 0.30f)) * Math.min(1.0f, age / 4.0f) * fadeOut;
        if (baseAlpha <= 0.01f || dist <= 0.01f) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        Emission emission = emissionFor(state, level);
        emission.frame.center = center;
        emission.frame.radius = baseRadius;
        emission.frame.funnelHeight = 2.4f + charge * 1.4f;
        float delta = emission.nextDelta();
        float envelope = baseAlpha * dist;
        emission.plasma.tick(delta, envelope);
        emission.vapor.tick(delta, envelope * 0.82f);
        emission.dust.tick(delta, envelope * 0.76f);
        emission.core.tick(delta, envelope * (charge >= 0.4f ? 1.0f : 0.30f));

        super.submit(state, poseStack, collector, cameraState);
    }

    private Emission emissionFor(ZChargeRenderState state, ClientLevel level) {
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
        float radius = 1.0f;
        float funnelHeight = 2.4f;
    }

    private static final class Emission {
        final long seed;
        final Frame frame = new Frame();
        final MaterialEmitters.Emitter plasma;
        final MaterialEmitters.Emitter vapor;
        final MaterialEmitters.Emitter dust;
        final MaterialEmitters.Emitter core;
        long lastSeenNanos = System.nanoTime();
        long lastTickNanos = 0L;

        Emission(ClientLevel level, long seed) {
            this.seed = seed;
            int baseSeed = (int) seed;
            Frame f = frame;

            MaterialEmitters.Source volume = random -> {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double flat = Math.sqrt(random.nextDouble()) * f.radius * 1.05;
                double y = random.nextDouble() * (f.funnelHeight + 1.35);
                Vec3 pos = f.center.add(Math.cos(angle) * flat, y, Math.sin(angle) * flat);
                Vec3 corePos = f.center.add(0.0, 1.0, 0.0);
                Vec3 inward = corePos.subtract(pos).scale(0.20);
                Vec3 swirl = new Vec3(-Math.sin(angle), 0.0, Math.cos(angle)).scale(0.055);
                return new MaterialEmitters.Sample(pos, inward.add(swirl));
            };
            MaterialEmitters.Source ground = random -> {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double flat = Math.sqrt(random.nextDouble()) * f.radius * 1.08;
                Vec3 pos = f.center.add(Math.cos(angle) * flat, 0.06, Math.sin(angle) * flat);
                Vec3 corePos = f.center.add(0.0, 1.0, 0.0);
                Vec3 inward = corePos.subtract(pos).scale(0.17).add(0.0, 0.035, 0.0);
                return new MaterialEmitters.Sample(pos, inward);
            };
            MaterialEmitters.Source coreSource = random -> new MaterialEmitters.Sample(
                f.center.add(0.0, 1.0, 0.0), Vec3.ZERO);

            plasma = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(GOLD, 2.15f, baseSeed ^ 0x51A5, Vec3.ZERO, 0.015f),
                volume, 165.0f, 300);
            vapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(PALE_GOLD, 2.75f, baseSeed ^ 0x9A70, Vec3.ZERO, 0.012f),
                volume, 82.0f, 170);
            dust = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(STONE_DUST, 1.65f, baseSeed ^ 0xD057, Vec3.ZERO, 0.010f),
                ground, 74.0f, 160);
            core = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(BRIGHT_GOLD, 1.45f, baseSeed ^ 0xC02E, Vec3.ZERO, 0.24f),
                coreSource, 74.0f, 170);
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
}
