package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.ZStanceAuraEntity;
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
 * Entity Renderer for Z-Sword Katchin Weight and Gravity Stance in Minecraft 26.2.
 *
 * No-shapes pass: the stance draws no geometry. The gravity well is a
 * volume of violet plasma and pale vapor pulled toward the axis and
 * downward. The old monolith ring is not rebuilt as slabs or ribbons:
 * each anchor position carries a dense falling column of stone dust,
 * deep gravity plasma, and gold glints at its top, so the anchors read
 * as heavy material hanging in the well rather than as objects. Ground
 * dust gathers inward under the caster, and the singularity stays a
 * tight bright plasma concentration at the center.
 */
public class ZStanceAuraRenderer extends EntityRenderer<ZStanceAuraEntity, ZStanceAuraRenderer.ZStanceRenderState> {

    private static final int VIOLET = 0x661ACC;
    private static final int DEEP_VIOLET = 0x5914A6;
    private static final int PALE_VIOLET = 0xC9B8FF;
    private static final int EYE = 0xE6D8FF;
    private static final int GOLD = 0xFFD133;
    private static final int STONE_DUST = 0x9A8668;
    private static final int DARK_STONE = 0x77675A;

    private final Map<Integer, Emission> emissions = new HashMap<>();

    public ZStanceAuraRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ZStanceRenderState extends EntityRenderState {
        public int heldTicks = 0;
        public float powerRatio = 0.0f;
        public float age = 0;
        public long seed = 0;
        public int fadeTicks = -1;
        public int entityId = -1;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(ZStanceAuraEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(ZStanceAuraEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ZStanceRenderState createRenderState() {
        return new ZStanceRenderState();
    }

    @Override
    public void extractRenderState(ZStanceAuraEntity entity, ZStanceRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.heldTicks = entity.getHeldTicks();
        state.powerRatio = entity.getPowerRatio();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getSeed();
        state.fadeTicks = entity.getFadeTicks();
        state.entityId = entity.getId();
    }

    @Override
    public void submit(ZStanceRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft == null ? null : minecraft.level;
        if (level == null) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        float power = state.powerRatio;
        float age = state.age;
        Vec3 center = new Vec3(state.x, state.y, state.z);
        float dist = MaterialEmitters.distanceScale(center);

        float baseRadius = 2.0f + (power * 3.5f);
        float fadeOut = state.fadeTicks < 0 ? 1.0f
            : Math.max(0.0f, 1.0f - state.fadeTicks / (float) ZStanceAuraEntity.FADE_LENGTH);
        float baseAlpha = (0.70f + (power * 0.28f)) * Math.min(1.0f, age / 5.0f) * fadeOut;
        if (baseAlpha <= 0.01f || dist <= 0.01f) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        Emission emission = emissionFor(state, level);
        emission.frame.center = center;
        emission.frame.baseRadius = baseRadius;
        emission.frame.funnelHeight = 3.0f + power * 2.0f;
        emission.frame.power = power;
        emission.frame.age = age;
        emission.frame.pulse = 0.94f + 0.06f * (float) Math.sin(age * 0.40f);
        emission.frame.monolithCount = 6 + (int) (power * 2);

        float delta = emission.nextDelta();
        float envelope = baseAlpha * dist;
        emission.wellPlasma.tick(delta, envelope);
        emission.wellVapor.tick(delta, envelope * 0.80f);
        emission.groundDust.tick(delta, envelope * 0.78f);
        emission.columnDust.tick(delta, envelope);
        emission.columnPlasma.tick(delta, envelope * 0.92f);
        emission.glints.tick(delta, envelope * 0.90f);
        emission.eye.tick(delta, envelope);

        super.submit(state, poseStack, collector, cameraState);
    }

    private Emission emissionFor(ZStanceRenderState state, ClientLevel level) {
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
        float baseRadius = 2.0f;
        float funnelHeight = 3.0f;
        float power = 0.0f;
        float age = 0.0f;
        float pulse = 1.0f;
        int monolithCount = 6;

        Vec3 columnAnchor(int index) {
            double angle = (index / (double) Math.max(1, monolithCount)) * Math.PI * 2.0
                + Math.toRadians(age * 3.0f);
            float dist = 1.6f + (power * 1.8f);
            return new Vec3(center.x + Math.cos(angle) * dist, center.y,
                center.z + Math.sin(angle) * dist);
        }

        float columnHeight() {
            return (1.4f + power * 1.6f) * pulse;
        }

        float columnWidth() {
            return 0.35f + power * 0.15f;
        }
    }

    private static final class Emission {
        final long seed;
        final Frame frame = new Frame();
        final MaterialEmitters.Emitter wellPlasma;
        final MaterialEmitters.Emitter wellVapor;
        final MaterialEmitters.Emitter groundDust;
        final MaterialEmitters.Emitter columnDust;
        final MaterialEmitters.Emitter columnPlasma;
        final MaterialEmitters.Emitter glints;
        final MaterialEmitters.Emitter eye;
        long lastSeenNanos = System.nanoTime();
        long lastTickNanos = 0L;

        Emission(ClientLevel level, long seed) {
            this.seed = seed;
            int baseSeed = (int) seed;
            Frame f = frame;

            MaterialEmitters.Source well = random -> {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double flat = Math.sqrt(random.nextDouble()) * f.baseRadius;
                double y = random.nextDouble() * f.funnelHeight;
                Vec3 pos = f.center.add(Math.cos(angle) * flat, y, Math.sin(angle) * flat);
                Vec3 inward = new Vec3(-Math.cos(angle), 0.0, -Math.sin(angle)).scale(0.16);
                Vec3 swirl = new Vec3(-Math.sin(angle), 0.0, Math.cos(angle)).scale(0.055);
                return new MaterialEmitters.Sample(pos, inward.add(swirl).add(0.0, -0.045, 0.0));
            };
            MaterialEmitters.Source ground = random -> {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double flat = Math.sqrt(random.nextDouble()) * f.baseRadius * 1.05;
                Vec3 pos = f.center.add(Math.cos(angle) * flat, 0.06, Math.sin(angle) * flat);
                Vec3 inward = new Vec3(-Math.cos(angle), 0.0, -Math.sin(angle))
                    .scale(0.13 + random.nextDouble() * 0.08).add(0.0, 0.025, 0.0);
                return new MaterialEmitters.Sample(pos, inward);
            };
            MaterialEmitters.Source columns = random -> {
                int index = random.nextInt(Math.max(1, f.monolithCount));
                Vec3 anchor = f.columnAnchor(index);
                double local = (random.nextDouble() * 2.0 - 1.0) * f.columnWidth() * 0.58;
                double localZ = (random.nextDouble() * 2.0 - 1.0) * f.columnWidth() * 0.58;
                double y = random.nextDouble() * f.columnHeight();
                Vec3 pos = anchor.add(local, y, localZ);
                Vec3 fall = new Vec3((random.nextDouble() - 0.5) * 0.03,
                    -(0.16 + random.nextDouble() * 0.20), (random.nextDouble() - 0.5) * 0.03);
                return new MaterialEmitters.Sample(pos, fall);
            };
            MaterialEmitters.Source tops = random -> {
                int index = random.nextInt(Math.max(1, f.monolithCount));
                Vec3 anchor = f.columnAnchor(index);
                double local = (random.nextDouble() * 2.0 - 1.0) * f.columnWidth() * 0.40;
                double localZ = (random.nextDouble() * 2.0 - 1.0) * f.columnWidth() * 0.40;
                double y = f.columnHeight() + 0.22 + 0.10 * Math.sin(f.age * 0.5f + index);
                return new MaterialEmitters.Sample(anchor.add(local, y, localZ),
                    new Vec3(0.0, -0.018, 0.0));
            };
            MaterialEmitters.Source eyeSource = random -> new MaterialEmitters.Sample(
                f.center.add(0.0, 0.12, 0.0), Vec3.ZERO);

            wellPlasma = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(VIOLET, 2.05f, baseSeed ^ 0x9E11, Vec3.ZERO, 0.012f),
                well, 150.0f, 300);
            wellVapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(PALE_VIOLET, 2.65f, baseSeed ^ 0x9A90, Vec3.ZERO, 0.010f),
                well, 76.0f, 165);
            groundDust = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(STONE_DUST, 1.55f, baseSeed ^ 0xD057, Vec3.ZERO, 0.010f),
                ground, 72.0f, 155);
            columnDust = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(DARK_STONE, 1.85f, baseSeed ^ 0xC010, Vec3.ZERO, 0.008f),
                columns, 138.0f, 290);
            columnPlasma = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(DEEP_VIOLET, 1.95f, baseSeed ^ 0xC011, Vec3.ZERO, 0.008f),
                columns, 84.0f, 190);
            glints = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(GOLD, 1.25f, baseSeed ^ 0x6117, Vec3.ZERO, 0.055f),
                tops, 38.0f, 95);
            eye = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(EYE, 1.55f, baseSeed ^ 0xE7E1, Vec3.ZERO, 0.085f),
                eyeSource, 44.0f, 105);
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
