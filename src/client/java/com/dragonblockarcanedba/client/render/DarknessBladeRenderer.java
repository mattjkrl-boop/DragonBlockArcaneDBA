package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.DarknessBladeEntity;
import com.dragonblockarcanedba.particle.DbaMaterialOptions;
import com.dragonblockarcanedba.particle.DbaParticles;
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
 * Entity Renderer for Darkness Blade falling execution in MC 26.2.
 *
 * No-shapes pass: the falling blade geometry is gone. The plunge is now
 * carried by plasma, darkness, vapor, and soul material shed along the
 * falling path, with bright plasma sparks at the leading end. The entity
 * still falls and rolls exactly as before, and the 3 tick fade-in is
 * preserved as the emission envelope.
 *
 * Complex pass: a tendril wake trails above the falling blade as alpha
 * veil strands (darkness smoke made structured), and three dark steel
 * shard pieces ride the rolling blade arc. Ember fragments
 * and spark streaks shed at the leading tip. The material emission stays
 * as filler around the strands.
 */
public class DarknessBladeRenderer extends EntityRenderer<DarknessBladeEntity, DarknessBladeRenderer.DarknessBladeRenderState> {

    public DarknessBladeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class DarknessBladeRenderState extends EntityRenderState {
        public float yRot = 0;
        public float age = 0;
        public double px = 0, py = 0, pz = 0;
        public long seed = 0;
        public MaterialEmitters.Emitter[] emitters;
        public long emitterSeed = Long.MIN_VALUE;
        public ClientLevel emitterLevel;
        public float lastEmissionAge = Float.NaN;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(DarknessBladeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(DarknessBladeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public DarknessBladeRenderState createRenderState() {
        return new DarknessBladeRenderState();
    }

    @Override
    public void extractRenderState(DarknessBladeEntity entity, DarknessBladeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.age = entity.tickCount + partialTicks;
        state.px = entity.getX();
        state.py = entity.getY();
        state.pz = entity.getZ();
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(DarknessBladeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        submitPieces(state, poseStack, collector);
        emitMaterials(state);
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void submitPieces(DarknessBladeRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        float fade = Math.min(1.0f, Math.max(0.0f, state.age / 3.0f));
        if (fade <= 0.01f) {
            return;
        }
        float distScale = MaterialEmitters.distanceScale(new Vec3(state.px, state.py, state.pz));
        float envelope = fade;
        long seed = state.seed;
        // Tendril wake behind the falling blade: the blade drops, so the
        // wake streams upward from its upper end, strands splitting and
        // rejoining as they tear away into darkness smoke.
        ComplexGeometry.submitVeil(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) ->
            ComplexGeometry.tendrilFan(pose, buffer, seed ^ 0xB1D1L,
                new Vec3(0.0, 1.55, 0.0), new Vec3(0.0, 1.0, 0.0), 30.0f, 2.5f, 3,
                0.34f, 0x8C0DF2, 0x2A0045, 0.92f, envelope, distScale));
        // Three physical dark steel shards ride the same roll the
        // emission samples around the falling body: one near each end
        // and one at the center, each aligned to the roll tangent and
        // tipped downward so they travel with the blade arc.
        final double roll = Math.toRadians(state.yRot + state.age * 18.0f);
        final Vec3 side = new Vec3(Math.cos(roll), 0.0, Math.sin(roll));
        final Vec3 tangent = new Vec3(-Math.sin(roll), 0.0, Math.cos(roll));
        ComplexGeometry.submitSolidLit(collector, poseStack, state.lightCoords, (pose, buffer) -> {
            for (int i = 0; i < 3; i++) {
                double along = 1.25 - i * 1.25;
                Vec3 origin = side.scale(0.16).add(new Vec3(0.0, along, 0.0));
                Vec3 dir = tangent.scale(0.78).add(new Vec3(0.0, -0.46, 0.0));
                float length = 0.98f - i * 0.16f;
                ComplexGeometry.shard(pose, buffer, seed ^ (0xB1D2L + i * 131L),
                    origin, dir, length, length * 0.20f,
                    0xFF0B0A10, 0xFF8A8694, 0.95f, envelope);
            }
        });
    }

    private static void emitMaterials(DarknessBladeRenderState state) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        float fade = Math.min(1.0f, Math.max(0.0f, state.age / 3.0f));
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

    private static MaterialEmitters.Emitter[] createEmitters(DarknessBladeRenderState state, ClientLevel level) {
        int seed = (int) state.seed;
        return new MaterialEmitters.Emitter[] {
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.PLASMA, 0x8C0DF2, 2.0f,
                    seed ^ 0xB1A1, Vec3.ZERO, 0.010f),
                bodySource(state, 0.28, -2.25, 2.25, 0.12), 120.0f, 135),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.PLASMA, 0xF21459, 1.25f,
                    seed ^ 0xB1A2, Vec3.ZERO, 0.008f),
                bodySource(state, 0.16, -2.05, 2.05, 0.09), 75.0f, 85),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.PLASMA, 0xFFFFFF, 0.85f,
                    seed ^ 0xB1A3, Vec3.ZERO, 0.006f),
                bodySource(state, 0.10, -1.65, 1.65, 0.07), 38.0f, 45),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.DARKNESS, 0x17002B, 2.85f,
                    seed ^ 0xB1A4, Vec3.ZERO, 0.014f),
                bodySource(state, 0.55, -2.30, 2.30, 0.16), 95.0f, 110),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.VAPOR, 0x5B21B6, 3.20f,
                    seed ^ 0xB1A5, Vec3.ZERO, 0.020f),
                bodySource(state, 0.85, -1.90, 3.10, 0.27), 70.0f, 80),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.SOUL, 0xC4B5FD, 1.50f,
                    seed ^ 0xB1A6, Vec3.ZERO, 0.012f),
                bodySource(state, 0.45, -2.10, 2.45, 0.18), 45.0f, 55),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.PLASMA, 0xFFE4E6, 0.95f,
                    seed ^ 0xB1A7, Vec3.ZERO, 0.030f),
                tipSparkSource(state), 42.0f, 50),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.EMBER_FRAGMENT, Vec3.ZERO, 0.05f),
                tipSparkSource(state), 30.0f, 36),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.09f),
                tipSparkSource(state), 22.0f, 28)
        };
    }

    private static MaterialEmitters.Source bodySource(DarknessBladeRenderState state,
                                                      double radius, double minAlong, double maxAlong,
                                                      double upwardVelocity) {
        return random -> {
            double along = minAlong + random.nextDouble() * (maxAlong - minAlong);
            double angle = Math.toRadians(state.yRot + state.age * 18.0f) + random.nextDouble() * Math.PI * 2.0;
            double radial = Math.sqrt(random.nextDouble()) * radius;
            double x = Math.cos(angle) * radial;
            double z = Math.sin(angle) * radial;
            Vec3 pos = new Vec3(state.px + x, state.py + along, state.pz + z);
            Vec3 velocity = new Vec3(x * 0.020, upwardVelocity, z * 0.020);
            return new MaterialEmitters.Sample(pos, velocity);
        };
    }

    private static MaterialEmitters.Source tipSparkSource(DarknessBladeRenderState state) {
        return random -> {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radial = Math.sqrt(random.nextDouble()) * 0.32;
            double x = Math.cos(angle) * radial;
            double z = Math.sin(angle) * radial;
            Vec3 pos = new Vec3(state.px + x, state.py - 2.15 + random.nextDouble() * 0.35, state.pz + z);
            Vec3 velocity = new Vec3(Math.cos(angle) * 0.16, -0.10 - random.nextDouble() * 0.12,
                Math.sin(angle) * 0.16);
            return new MaterialEmitters.Sample(pos, velocity);
        };
    }
}
