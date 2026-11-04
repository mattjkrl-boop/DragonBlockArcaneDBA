package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.AzureTornadoEntity;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Entity Renderer for Azure Tornado in Minecraft 26.2.
 *
 * No-shapes pass: this renderer draws no geometry and no funnel surface.
 * The tornado is swirling particle forces: vapor and mist sampled
 * throughout the funnel volume with tangential and rising velocities,
 * wind-driven rain sprayed through the same volume, ground dust pulled
 * around the base, physical debris chunks of the actual ground block
 * caught in the lower funnel, and tempest plasma threading the flow.
 * Sizes still match the entity's physics zone exactly (radius scale x 8
 * giant / x 3 mini, height scale x 16 / x 6), and the fade is the shared
 * FxEnvelope over the entity's real max lifetime. All emission takes the
 * tornado entity's synced render seed.
 */
public class AzureTornadoRenderer extends EntityRenderer<AzureTornadoEntity, AzureTornadoRenderer.TornadoRenderState> {

    public AzureTornadoRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class TornadoRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public boolean isGiant = false;
        public float age = 0;
        public int maxLife = 60;
        public long seed = 0;
        public double wx, wy, wz;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(AzureTornadoEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(AzureTornadoEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public TornadoRenderState createRenderState() {
        return new TornadoRenderState();
    }

    @Override
    public void extractRenderState(AzureTornadoEntity entity, TornadoRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getScale();
        state.isGiant = entity.isGiant();
        state.age = entity.tickCount + partialTicks;
        state.maxLife = entity.getMaxLifetime();
        state.seed = entity.getRenderSeed();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    /** Mutable per-frame emission data read by the sources. Data only. */
    private static final class Frame {
        Vec3 center = Vec3.ZERO;
        float baseRadius = 3.0f;
        float totalHeight = 6.0f;
    }

    private static final class Emission {
        final Frame frame = new Frame();
        ClientLevel level;
        MaterialEmitters.Emitter[] emitters = new MaterialEmitters.Emitter[0];
        float lastAge = -1f;
    }

    private final Map<Long, Emission> emissions = new HashMap<>();

    /**
     * Points throughout the funnel volume. The radius profile widens with
     * height, and the sample velocity is tangential plus rising, so the
     * particles themselves carry the rotation and the updraft.
     */
    private static MaterialEmitters.Source funnel(Frame frame, boolean lowOnly, boolean rain) {
        return random -> {
            double yFrac = lowOnly ? random.nextDouble() * 0.35 : random.nextDouble();
            double radiusAt = frame.baseRadius * (0.30 + 0.85 * yFrac);
            double ang = random.nextDouble() * Math.PI * 2.0;
            double rr = radiusAt * (0.55 + 0.45 * Math.sqrt(random.nextDouble()));
            double dx = Math.cos(ang) * rr;
            double dz = Math.sin(ang) * rr;
            Vec3 pos = new Vec3(
                    frame.center.x + dx,
                    frame.center.y + yFrac * frame.totalHeight,
                    frame.center.z + dz);
            double tangent = 0.30 + 0.25 * yFrac;
            Vec3 vel = rain
                    ? new Vec3(-Math.sin(ang) * tangent * 0.7, -1.6, Math.cos(ang) * tangent * 0.7)
                    : new Vec3(-Math.sin(ang) * tangent, 0.10 + 0.18 * yFrac, Math.cos(ang) * tangent);
            return new MaterialEmitters.Sample(pos, vel);
        };
    }

    /** Points across the base disc, pulled around and slightly inward. */
    private static MaterialEmitters.Source groundSwirl(Frame frame) {
        return random -> {
            double ang = random.nextDouble() * Math.PI * 2.0;
            double rr = frame.baseRadius * 0.9 * Math.sqrt(random.nextDouble());
            double dx = Math.cos(ang) * rr;
            double dz = Math.sin(ang) * rr;
            Vec3 pos = new Vec3(
                    frame.center.x + dx,
                    frame.center.y + 0.05 + random.nextDouble() * 0.20,
                    frame.center.z + dz);
            Vec3 vel = new Vec3(
                    -Math.sin(ang) * 0.28 - Math.cos(ang) * 0.06,
                    0.03,
                    Math.cos(ang) * 0.28 - Math.sin(ang) * 0.06);
            return new MaterialEmitters.Sample(pos, vel);
        };
    }

    private static BlockState groundState(ClientLevel level, Vec3 center) {
        BlockState state = level.getBlockState(BlockPos.containing(center.x, center.y - 0.5, center.z));
        return state.isAir() ? Blocks.STONE.defaultBlockState() : state;
    }

    private Emission buildEmission(ClientLevel level, TornadoRenderState state) {
        Emission emission = new Emission();
        emission.level = level;
        Frame frame = emission.frame;
        frame.center = new Vec3(state.wx, state.wy, state.wz);
        frame.baseRadius = state.scale * (state.isGiant ? 8.0f : 3.0f);
        frame.totalHeight = state.scale * (state.isGiant ? 16.0f : 6.0f);
        int seed = (int) state.seed;
        float sf = state.isGiant ? 1.0f : 0.55f;
        BlockState ground = groundState(level, frame.center);
        emission.emitters = new MaterialEmitters.Emitter[]{
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.vapor(0xBFE3F2, 2.6f * sf, seed ^ 0x9A90,
                                Vec3.ZERO, 0.02f),
                        funnel(frame, false, false), state.isGiant ? 150f : 85f, state.isGiant ? 230 : 130),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.mist(0xCFE6F2, 3.4f * sf, seed ^ 0x1157,
                                Vec3.ZERO, 0.01f),
                        funnel(frame, false, false), state.isGiant ? 40f : 22f, state.isGiant ? 70 : 40),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.fixed(DbaParticles.RAIN,
                                new Vec3(0.0, -0.8, 0.0), 0.03f),
                        funnel(frame, false, true), state.isGiant ? 240f : 130f, state.isGiant ? 340 : 200),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.dust(0x7E8A8F, 1.3f * sf, seed ^ 0xD057,
                                Vec3.ZERO, 0.02f),
                        groundSwirl(frame), state.isGiant ? 90f : 55f, state.isGiant ? 140 : 90),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.fixed(new BlockParticleOption(DbaParticles.BLOCK_CHUNKS, ground),
                                new Vec3(0.0, 0.25, 0.0), 0.10f),
                        funnel(frame, true, false), state.isGiant ? 36f : 20f, state.isGiant ? 60 : 36),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(0x4DF2FF, 1.0f * sf, seed ^ 0xBE10,
                                Vec3.ZERO, 0.01f),
                        funnel(frame, false, false), state.isGiant ? 45f : 25f, state.isGiant ? 80 : 50),
        };
        return emission;
    }

    @Override
    public void submit(TornadoRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        // Same numbers as the entity's physics zone, so what you see is what pulls you.
        float baseRadius = state.scale * (state.isGiant ? 8.0f : 3.0f);
        float totalHeight = state.scale * (state.isGiant ? 16.0f : 6.0f);
        float fade = FxEnvelope.envelope(state.age, Math.max(1.0f, state.maxLife));

        Vec3 center = new Vec3(state.wx, state.wy, state.wz);
        float ds = MaterialEmitters.distanceScale(center);
        float envelope = fade * ds;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc != null ? mc.level : null;
        if (level != null && envelope > 0.01f) {
            Emission emission = emissions.get(state.seed);
            if (emission == null || emission.level != level || state.age < emission.lastAge - 1.0f) {
                emission = buildEmission(level, state);
                emissions.put(state.seed, emission);
                if (emissions.size() > 64) emissions.clear();
            }
            emission.frame.center = center;
            emission.frame.baseRadius = baseRadius;
            emission.frame.totalHeight = totalHeight;
            float delta = emission.lastAge < 0f ? 0.05f
                    : Math.min(0.05f, Math.max(0.0f, (state.age - emission.lastAge) / 20.0f));
            emission.lastAge = state.age;
            for (MaterialEmitters.Emitter emitter : emission.emitters) {
                emitter.tick(delta, envelope);
            }
        }

        super.submit(state, poseStack, collector, cameraState);
    }
}
