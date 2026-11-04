package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.DarknessChargeEntity;
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
 * Entity Renderer for Darkness Sword Abyssal Slash Charge-up in Minecraft 26.2.
 *
 * No-shapes pass: the seal rings, converging shards, helical ribbons, and
 * nested core geometry are gone. Darkness, plasma, soul vapor, and vapor
 * are sampled on inward paths around the caster, so the charge reads as
 * material gathering into the weapon instead of constructed shapes. The
 * charge ratio, pulse, radius growth, and core position are unchanged.
 */
public class DarknessChargeRenderer extends EntityRenderer<DarknessChargeEntity, DarknessChargeRenderer.ChargeRenderState> {

    public DarknessChargeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ChargeRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public float age = 0;
        public double px = 0, py = 0, pz = 0;
        public long seed = 0;
        public MaterialEmitters.Emitter[] emitters;
        public long emitterSeed = Long.MIN_VALUE;
        public ClientLevel emitterLevel;
        public float lastEmissionAge = Float.NaN;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(DarknessChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(DarknessChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ChargeRenderState createRenderState() {
        return new ChargeRenderState();
    }

    @Override
    public void extractRenderState(DarknessChargeEntity entity, ChargeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.age = entity.tickCount + partialTicks;
        state.px = entity.getX();
        state.py = entity.getY();
        state.pz = entity.getZ();
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(ChargeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        emitMaterials(state);
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void emitMaterials(ChargeRenderState state) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        float charge = Math.max(0.0f, Math.min(1.0f, state.chargeRatio));
        float distance = MaterialEmitters.distanceScale(new Vec3(state.px, state.py, state.pz));
        float envelope = (0.30f + 0.70f * charge)
            * (0.35f + 0.65f * (float) Math.sqrt(Math.max(0.0f, distance)));

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

    private static MaterialEmitters.Emitter[] createEmitters(ChargeRenderState state, ClientLevel level) {
        int seed = (int) state.seed;
        return new MaterialEmitters.Emitter[] {
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.DARKNESS, 0x1F0040, 3.0f,
                    seed ^ 0xC401, Vec3.ZERO, 0.008f),
                ringSource(state, 0.85, 1.30, 0.06, 0.105), 125.0f, 140),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.PLASMA, 0x8C0DD9, 1.85f,
                    seed ^ 0xC402, Vec3.ZERO, 0.010f),
                ringSource(state, 0.55, 1.05, 0.10, 0.135), 100.0f, 115),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.SOUL, 0xD8B4FE, 1.60f,
                    seed ^ 0xC403, Vec3.ZERO, 0.010f),
                sphereSource(state, 0.85, 1.05, 0.045), 85.0f, 95),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.DARKNESS, 0x0D0217, 3.45f,
                    seed ^ 0xC404, Vec3.ZERO, 0.014f),
                sphereSource(state, 1.05, 1.30, 0.018), 70.0f, 80),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.PLASMA, 0x7C3AED, 2.45f,
                    seed ^ 0xC405, Vec3.ZERO, 0.008f),
                coreSource(state, 1.35), 75.0f, 85),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.PLASMA, 0xF21459, 1.50f,
                    seed ^ 0xC406, Vec3.ZERO, 0.006f),
                coreSource(state, 0.95), 115.0f, 130),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.VAPOR, 0x7C3AED, 3.05f,
                    seed ^ 0xC407, Vec3.ZERO, 0.014f),
                helixSource(state), 80.0f, 90)
        };
    }

    private static MaterialEmitters.Source ringSource(ChargeRenderState state,
                                                      double minFactor, double maxFactor,
                                                      double y, double inwardSpeed) {
        return random -> {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = baseRadius(state) * (minFactor + random.nextDouble() * (maxFactor - minFactor));
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Vec3 pos = new Vec3(state.px + x, state.py + y, state.pz + z);
            Vec3 velocity = new Vec3(-Math.cos(angle) * inwardSpeed, 0.010, -Math.sin(angle) * inwardSpeed);
            return new MaterialEmitters.Sample(pos, velocity);
        };
    }

    private static MaterialEmitters.Source sphereSource(ChargeRenderState state,
                                                        double centerY, double radiusFactor,
                                                        double inwardFactor) {
        return random -> {
            Vec3 center = new Vec3(state.px, state.py + centerY, state.pz);
            double u = random.nextDouble() * 2.0 - 1.0;
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = baseRadius(state) * radiusFactor * Math.cbrt(random.nextDouble());
            double flat = Math.sqrt(Math.max(0.0, 1.0 - u * u)) * radius;
            Vec3 pos = new Vec3(center.x + Math.cos(angle) * flat,
                center.y + u * radius, center.z + Math.sin(angle) * flat);
            Vec3 velocity = center.subtract(pos).scale(inwardFactor).add(0, 0.006, 0);
            return new MaterialEmitters.Sample(pos, velocity);
        };
    }

    private static MaterialEmitters.Source coreSource(ChargeRenderState state, double scaleFactor) {
        return random -> {
            Vec3 center = new Vec3(state.px, state.py + 0.85, state.pz);
            double u = random.nextDouble() * 2.0 - 1.0;
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = coreRadius(state) * scaleFactor * Math.cbrt(random.nextDouble());
            double flat = Math.sqrt(Math.max(0.0, 1.0 - u * u)) * radius;
            Vec3 pos = new Vec3(center.x + Math.cos(angle) * flat,
                center.y + u * radius, center.z + Math.sin(angle) * flat);
            return new MaterialEmitters.Sample(pos, Vec3.ZERO);
        };
    }

    private static MaterialEmitters.Source helixSource(ChargeRenderState state) {
        return random -> {
            int strand = random.nextInt(2);
            double angle = strand * Math.PI + state.age * 0.18 + (random.nextDouble() - 0.5) * 0.22;
            double radius = baseRadius(state) * 0.62;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Vec3 pos = new Vec3(state.px + x, state.py + random.nextDouble() * 2.3, state.pz + z);
            Vec3 velocity = new Vec3(-Math.sin(angle) * 0.055, 0.028, Math.cos(angle) * 0.055);
            return new MaterialEmitters.Sample(pos, velocity);
        };
    }

    private static float baseRadius(ChargeRenderState state) {
        float charge = Math.max(0.0f, Math.min(1.0f, state.chargeRatio));
        return 1.3f + charge * 1.5f;
    }

    private static float coreRadius(ChargeRenderState state) {
        float charge = Math.max(0.0f, Math.min(1.0f, state.chargeRatio));
        float pulse = 0.90f + 0.10f * (float) Math.sin(state.age * 0.5f);
        return (0.70f - charge * 0.20f) * pulse;
    }
}
