package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.DarknessWaveEntity;
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
 * Entity Renderer for Darkness Wave in Minecraft 26.2.
 *
 * No-shapes pass: the crescent, tongue, and lash geometry is gone. The
 * wave is a rolling front of darkness, hellish plasma, vapor, and soul
 * material sampled across the same moving volume the old visual occupied
 * and matched to the entity hitbox. The lifecycle math is unchanged:
 * opens over 5 ticks, full to tick 45, gone at tick 60.
 */
public class DarknessWaveRenderer extends EntityRenderer<DarknessWaveEntity, DarknessWaveRenderer.DarknessWaveRenderState> {

    public DarknessWaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class DarknessWaveRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public double px = 0, py = 0, pz = 0;
        public boolean isSecondary = false;
        public boolean isFirstPersonOwner = false;
        public long seed = 0;
        public MaterialEmitters.Emitter[] emitters;
        public long emitterSeed = Long.MIN_VALUE;
        public ClientLevel emitterLevel;
        public float lastEmissionAge = Float.NaN;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(DarknessWaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(DarknessWaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public DarknessWaveRenderState createRenderState() {
        return new DarknessWaveRenderState();
    }

    @Override
    public void extractRenderState(DarknessWaveEntity entity, DarknessWaveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.px = entity.getX();
        state.py = entity.getY();
        state.pz = entity.getZ();
        state.isSecondary = entity.isSecondary();
        state.seed = entity.getUUID().getMostSignificantBits();

        Minecraft mc = Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null &&
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) &&
            mc.options.getCameraType().isFirstPerson());
    }

    @Override
    public void submit(DarknessWaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        emitMaterials(state);
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void emitMaterials(DarknessWaveRenderState state) {
        float fade = waveFade(state);
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (fade <= 0.01f) {
            state.lastEmissionAge = state.age;
            return;
        }

        float distance = MaterialEmitters.distanceScale(new Vec3(state.px, state.py, state.pz));
        float envelope = fade * (0.35f + 0.65f * (float) Math.sqrt(Math.max(0.0f, distance)));
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

    private static MaterialEmitters.Emitter[] createEmitters(DarknessWaveRenderState state, ClientLevel level) {
        int seed = (int) state.seed;
        return new MaterialEmitters.Emitter[] {
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.DARKNESS, 0x17002B, 3.1f,
                    seed ^ 0xDA01, Vec3.ZERO, 0.012f),
                frontSource(state, -0.45, 0.70, 0.62, 0.58, 0.035), 180.0f, 200),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.PLASMA, 0xA605F2, 2.35f,
                    seed ^ 0xDA02, Vec3.ZERO, 0.018f),
                frontSource(state, -0.25, 0.50, 0.55, 0.50, 0.045), 150.0f, 170),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.PLASMA, 0xF21459, 1.45f,
                    seed ^ 0xDA03, Vec3.ZERO, 0.012f),
                frontSource(state, -0.10, 0.35, 0.38, 0.36, 0.050), 80.0f, 95),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.VAPOR, 0x6D28A8, 3.35f,
                    seed ^ 0xDA04, Vec3.ZERO, 0.020f),
                frontSource(state, -2.40, -0.25, 0.58, 0.52, -0.085), 110.0f, 125),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.SOUL, 0xD8B4FE, 1.75f,
                    seed ^ 0xDA05, Vec3.ZERO, 0.015f),
                frontSource(state, -1.15, 0.40, 0.50, 0.46, 0.018), 55.0f, 65)
        };
    }

    private static MaterialEmitters.Source frontSource(DarknessWaveRenderState state,
                                                       double minDepth, double maxDepth,
                                                       double acrossScale, double heightScale,
                                                       double forwardVelocity) {
        return random -> {
            Vec3 forward = forward(state);
            Vec3 right = right(state, forward);
            Vec3 up = right.cross(forward).normalize();
            double across = (random.nextDouble() * 2.0 - 1.0) * waveSpan(state) * acrossScale;
            double vertical = (random.nextDouble() * 2.0 - 1.0) * waveHeight(state) * heightScale;
            double depth = minDepth + random.nextDouble() * (maxDepth - minDepth);
            Vec3 pos = center(state)
                .add(right.scale(across))
                .add(up.scale(vertical))
                .add(forward.scale(depth));
            return new MaterialEmitters.Sample(pos, forward.scale(forwardVelocity));
        };
    }

    private static Vec3 center(DarknessWaveRenderState state) {
        return new Vec3(state.px, state.py + waveHeight(state) * 0.35, state.pz);
    }

    private static Vec3 forward(DarknessWaveRenderState state) {
        return Vec3.directionFromRotation(state.xRot, state.yRot);
    }

    private static Vec3 right(DarknessWaveRenderState state, Vec3 forward) {
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 0.0001) {
            right = Vec3.directionFromRotation(0.0f, state.yRot).cross(new Vec3(0, 1, 0));
        }
        return right.normalize();
    }

    private static float firstPersonScale(DarknessWaveRenderState state) {
        return state.isFirstPersonOwner && state.age < 5.0f
            ? 0.55f + (state.age / 5.0f) * 0.45f
            : 1.0f;
    }

    private static float waveSpan(DarknessWaveRenderState state) {
        return (state.isSecondary ? 3.0f : 4.4f) * firstPersonScale(state);
    }

    private static float waveHeight(DarknessWaveRenderState state) {
        return (state.isSecondary ? 1.4f : 2.0f) * firstPersonScale(state);
    }

    private static float waveFade(DarknessWaveRenderState state) {
        float open = ease(Math.min(1.0f, state.age / 5.0f));
        float fadeRaw = state.age <= 45.0f ? 1.0f : Math.max(0.0f, 1.0f - (state.age - 45.0f) / 15.0f);
        return ease(fadeRaw) * open;
    }

    private static float ease(float t) {
        float c = Math.max(0.0f, Math.min(1.0f, t));
        return c * c * (3.0f - 2.0f * c);
    }
}
