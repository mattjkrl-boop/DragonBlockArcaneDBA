package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.TridentShardEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Entity Renderer for Trident Shard in Minecraft 26.2.
 * No-shapes pass: the shard draws no geometry at all. The crossed crescent
 * head, collar barbs, and ribbon wake are gone; in their place the shard is
 * a dense hellfire plasma cluster carried by MaterialEmitters, with flame
 * and embers at the head position and a plasma trail shed along the flight
 * path behind it. Positions, pulse, spawn scale-in, and size steps are the
 * same ones the geometry version used; only the material changed.
 */
public class TridentShardRenderer extends EntityRenderer<TridentShardEntity, TridentShardRenderer.ShardRenderState> {

    /** Shell color of the old crescents, (0.34, 0.02, 0.04) as an RGB int. */
    private static final int SHELL = 0x57050A;
    /** Hot core color of the old crescents, (0.95, 0.22, 0.07) as an RGB int. */
    private static final int CORE = 0xF23812;

    public TridentShardRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ShardRenderState extends EntityRenderState {
        public float ageInTicks = 0;
        public float yRot = 0;
        public float xRot = 0;
        /** Stable per shard size factor, 0.80 to 1.16 in five steps, from the entity id. */
        public float sizeVar = 1.0f;
        /** Synced shape seed from the entity UUID. */
        public long seed = 0;
        public UUID uuid;
    }

    @Override
    public ShardRenderState createRenderState() {
        return new ShardRenderState();
    }

    @Override
    public void extractRenderState(TridentShardEntity entity, ShardRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.ageInTicks = entity.tickCount + partialTicks;
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        int step = Math.floorMod(entity.getUUID().hashCode(), 5);
        state.sizeVar = 0.80f + step * 0.09f;
        state.seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();
        state.uuid = entity.getUUID();
    }

    @Override
    public void submit(ShardRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        ClientLevel level = Minecraft.getInstance() == null ? null : Minecraft.getInstance().level;
        if (level == null || state.uuid == null) return;

        Emission emission = EMISSIONS.get(state.uuid);
        if (emission == null || emission.level != level) {
            emission = new Emission(level, state.sizeVar, (int) state.seed);
            EMISSIONS.put(state.uuid, emission);
            pruneEmissions();
        }
        emission.center = new Vec3(state.x, state.y, state.z);
        emission.forward = Vec3.directionFromRotation(state.xRot, state.yRot);

        // Same pulse and spawn scale-in the geometry version breathed with.
        float pulse = 0.85f + 0.15f * Mth.sin(state.ageInTicks * 0.35f);
        float bodyPulse = 0.93f + 0.07f * Mth.sin(state.ageInTicks * 0.35f);
        float spawnScale = 0.5f + 0.5f * Math.min(1.0f, state.ageInTicks / 5.0f);
        float envelope = pulse * bodyPulse * spawnScale
            * MaterialEmitters.distanceScale(emission.center);
        emission.tick(state.ageInTicks, envelope);
    }

    /**
     * Per shard emitter set. The sources are data only: they read the
     * shard's current center and flight direction at sample time and hand
     * MaterialEmitters points at the head and along the wake path. Nothing
     * here draws anything.
     */
    private static final class Emission {
        final ClientLevel level;
        Vec3 center = Vec3.ZERO;
        Vec3 forward = new Vec3(0, 0, 1);
        float lastAge = Float.NaN;
        long lastSeenNanos = System.nanoTime();
        final MaterialEmitters.Emitter cluster;
        final MaterialEmitters.Emitter flame;
        final MaterialEmitters.Emitter trail;
        final MaterialEmitters.Emitter embers;

        Emission(ClientLevel level, float sizeVar, int seed) {
            this.level = level;
            MaterialEmitters.Source headSource = random -> {
                Vec3 c = this.center;
                double r = 0.30 * sizeVar;
                return new MaterialEmitters.Sample(new Vec3(
                    c.x + (random.nextDouble() * 2.0 - 1.0) * r,
                    c.y + (random.nextDouble() * 2.0 - 1.0) * r,
                    c.z + (random.nextDouble() * 2.0 - 1.0) * r), Vec3.ZERO);
            };
            MaterialEmitters.Source trailSource = random -> {
                double t = random.nextDouble();
                Vec3 p = this.center.subtract(this.forward.scale(0.10 + 1.20 * t));
                return new MaterialEmitters.Sample(p, Vec3.ZERO);
            };
            this.cluster = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(CORE, 1.55f * sizeVar, seed, Vec3.ZERO, 0.02f),
                headSource, 100f, 170);
            this.flame = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.flame(new Vec3(0, 0.02, 0), 0.03f),
                headSource, 46f, 80);
            this.trail = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(SHELL, 1.15f * sizeVar, seed + 21, Vec3.ZERO, 0.015f),
                trailSource, 80f, 140);
            this.embers = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.ember(Vec3.ZERO, 0.07f),
                trailSource, 26f, 48);
        }

        void tick(float age, float envelope) {
            lastSeenNanos = System.nanoTime();
            if (Float.isNaN(lastAge)) {
                lastAge = age;
                return;
            }
            float delta = Math.min(0.25f, Math.max(0f, (age - lastAge) / 20f));
            lastAge = age;
            if (delta <= 0f || envelope <= 0.01f) return;
            cluster.tick(delta, envelope);
            flame.tick(delta, envelope * 0.85f);
            trail.tick(delta, envelope);
            embers.tick(delta, envelope * 0.7f);
        }
    }

    private static final Map<UUID, Emission> EMISSIONS = new HashMap<>();

    /** Drop emitter sets for shards that have not rendered in a while. */
    private static void pruneEmissions() {
        if (EMISSIONS.size() < 48) return;
        long cutoff = System.nanoTime() - 30_000_000_000L;
        EMISSIONS.values().removeIf(e -> e.lastSeenNanos < cutoff);
    }
}
