package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.DarknessShatterEntity;
import com.dragonblockarcanedba.particle.DbaMaterialOptions;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

/**
 * Entity Renderer for Darkness Void Shatter and Shockwave in Minecraft 26.2.
 *
 * No-shapes pass: the shock discs, crack stars, erupting slabs, and plume
 * geometry are gone. Darkness and dust move outward across the expanding
 * ground area, hellish plasma is sampled along radial crack paths as
 * emission data, and the center carries an eruption of darkness, soul
 * vapor, and dust. The fixed 40 tick life and corrected fade are unchanged.
 */
public class DarknessShatterRenderer extends EntityRenderer<DarknessShatterEntity, DarknessShatterRenderer.ShatterRenderState> {

    public DarknessShatterRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ShatterRenderState extends EntityRenderState {
        public float radius = 4.0f;
        public float age = 0;
        public int lifetime = 40;
        public long seed = 0;
        public double px = 0, py = 0, pz = 0;
        public MaterialEmitters.Emitter[] emitters;
        public long emitterSeed = Long.MIN_VALUE;
        public ClientLevel emitterLevel;
        public float lastEmissionAge = Float.NaN;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(DarknessShatterEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(DarknessShatterEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ShatterRenderState createRenderState() {
        return new ShatterRenderState();
    }

    @Override
    public void extractRenderState(DarknessShatterEntity entity, ShatterRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.age = entity.tickCount + partialTicks;
        state.lifetime = entity.getLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
        state.px = entity.getX();
        state.py = entity.getY();
        state.pz = entity.getZ();
    }

    @Override
    public void submit(ShatterRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        emitMaterials(state);
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void emitMaterials(ShatterRenderState state) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        float fade = Math.max(0.0f, 1.0f - state.age / (float) Math.max(1, state.lifetime));
        if (fade <= 0.01f) {
            state.lastEmissionAge = state.age;
            return;
        }

        float build = smoothstep(Math.min(1.0f, state.age / 3.2f));
        float distance = MaterialEmitters.distanceScale(new Vec3(state.px, state.py, state.pz));
        float envelope = fade * build * (0.35f + 0.65f * (float) Math.sqrt(Math.max(0.0f, distance)));
        if (state.emitters == null || state.emitterSeed != state.seed || state.emitterLevel != level
                || Float.isNaN(state.lastEmissionAge) || state.age < state.lastEmissionAge) {
            state.emitters = createEmitters(state, level);
            state.emitterSeed = state.seed;
            state.emitterLevel = level;
            for (MaterialEmitters.Emitter emitter : state.emitters) {
                emitter.tick(0.05f, envelope);
            }
            state.lastEmissionAge = state.age;
            return;
        }

        float delta = Math.min(0.05f, Math.max(0.0f, (state.age - state.lastEmissionAge) / 20.0f));
        state.lastEmissionAge = state.age;
        if (delta > 0.0f) {
            for (MaterialEmitters.Emitter emitter : state.emitters) {
                emitter.tick(delta, envelope);
            }
        }
    }

    private static MaterialEmitters.Emitter[] createEmitters(ShatterRenderState state, ClientLevel level) {
        int seed = (int) state.seed;
        return new MaterialEmitters.Emitter[] {
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.DARKNESS, 0x1B0030, 3.15f,
                    seed ^ 0x5401, Vec3.ZERO, 0.010f),
                groundSource(state, 0.105, 0.012), 82.0f, 92),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.DUST, 0x4B3644, 2.20f,
                    seed ^ 0x5402, Vec3.ZERO, 0.018f),
                groundSource(state, 0.165, 0.032), 46.0f, 54),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.PLASMA, 0xA605F2, 1.70f,
                    seed ^ 0x5403, Vec3.ZERO, 0.008f),
                crackSource(state, 1.00), 66.0f, 76),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.PLASMA, 0xF21459, 1.25f,
                    seed ^ 0x5404, Vec3.ZERO, 0.006f),
                crackSource(state, 0.62), 40.0f, 48),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.DARKNESS, 0x26003D, 3.40f,
                    seed ^ 0x5405, Vec3.ZERO, 0.012f),
                columnSource(state, 0.225), 52.0f, 60),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.SOUL, 0xD8B4FE, 1.80f,
                    seed ^ 0x5406, Vec3.ZERO, 0.010f),
                columnSource(state, 0.285), 32.0f, 38),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.DUST, 0x58415A, 2.60f,
                    seed ^ 0x5407, Vec3.ZERO, 0.016f),
                columnSource(state, 0.165), 30.0f, 36)
        };
    }

    private static MaterialEmitters.Source groundSource(ShatterRenderState state,
                                                        double outwardSpeed, double upwardSpeed) {
        return random -> {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radial = Math.sqrt(random.nextDouble()) * shockRadius(state);
            Vec3 pos = new Vec3(state.px + Math.cos(angle) * radial,
                state.py + 0.07 + random.nextDouble() * 0.10, state.pz + Math.sin(angle) * radial);
            Vec3 velocity = new Vec3(Math.cos(angle) * outwardSpeed, upwardSpeed,
                Math.sin(angle) * outwardSpeed);
            return new MaterialEmitters.Sample(pos, velocity);
        };
    }

    private static MaterialEmitters.Source crackSource(ShatterRenderState state, double maxFraction) {
        return random -> {
            int arm = random.nextInt(8);
            double seedOffset = (state.seed & 63L) / 63.0 * 0.35;
            double angle = arm * Math.PI * 0.25 + seedOffset + (random.nextDouble() - 0.5) * 0.10;
            double fraction = 0.12 + 0.88 * Math.pow(random.nextDouble(), 1.4);
            double radial = shockRadius(state) * maxFraction * fraction;
            Vec3 pos = new Vec3(state.px + Math.cos(angle) * radial,
                state.py + 0.065, state.pz + Math.sin(angle) * radial);
            Vec3 velocity = new Vec3(Math.cos(angle) * 0.026, 0.008, Math.sin(angle) * 0.026);
            return new MaterialEmitters.Sample(pos, velocity);
        };
    }

    private static MaterialEmitters.Source columnSource(ShatterRenderState state, double upwardSpeed) {
        return random -> {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radial = Math.sqrt(random.nextDouble()) * columnRadius(state);
            double height = columnHeight(state);
            Vec3 pos = new Vec3(state.px + Math.cos(angle) * radial,
                state.py + 0.08 + random.nextDouble() * Math.max(0.10, height),
                state.pz + Math.sin(angle) * radial);
            Vec3 velocity = new Vec3(-Math.sin(angle) * 0.030, upwardSpeed,
                Math.cos(angle) * 0.030);
            return new MaterialEmitters.Sample(pos, velocity);
        };
    }

    private static float shockRadius(ShatterRenderState state) {
        return Math.min(state.radius * 1.35f, state.age * 0.55f);
    }

    private static float columnHeight(ShatterRenderState state) {
        return state.radius * 1.8f * Math.min(1.0f, state.age * 0.3f);
    }

    private static float columnRadius(ShatterRenderState state) {
        float progress = state.age / (float) Math.max(1, state.lifetime);
        return state.radius * 0.25f * (1.0f - progress * 0.5f);
    }

    private static float smoothstep(float t) {
        float c = Math.max(0.0f, Math.min(1.0f, t));
        return c * c * (3.0f - 2.0f * c);
    }
}
