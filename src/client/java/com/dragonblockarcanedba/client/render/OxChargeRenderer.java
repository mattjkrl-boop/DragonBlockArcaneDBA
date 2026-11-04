package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.OxChargeEntity;
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
 * Entity Renderer for Ox King's Groundbreaker Charging in Minecraft 26.2.
 * No-shapes pass: this renderer draws no geometry. The charge reads as
 * material being pulled in and compressed: plasma motes drifting toward the
 * brace point, ground dust across the shatter radius, vapor, rising ember
 * tendrils above 25 percent charge, and occasional physical debris chunks
 * of the actual ground block knocked loose by the pressure. Radius growth,
 * the fade in, and the charge envelope are unchanged from the bespoke build.
 */
public class OxChargeRenderer extends EntityRenderer<OxChargeEntity, OxChargeRenderer.OxChargeRenderState> {

    public OxChargeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class OxChargeRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public float age = 0;
        public long seed = 0;
        public double wx = 0;
        public double wy = 0;
        public double wz = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(OxChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(OxChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public OxChargeRenderState createRenderState() {
        return new OxChargeRenderState();
    }

    @Override
    public void extractRenderState(OxChargeEntity entity, OxChargeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    /** Mutable per-frame emission data read by the sources. Data only. */
    private static final class Frame {
        Vec3 center = Vec3.ZERO;
        float radius = 1.0f;
        float columnHeight = 1.0f;
    }

    private static final class Emission {
        final Frame frame = new Frame();
        MaterialEmitters.Emitter plasma;
        MaterialEmitters.Emitter vapor;
        MaterialEmitters.Emitter dust;
        MaterialEmitters.Emitter ember;
        MaterialEmitters.Emitter chunks;
        float lastAge = -1f;
    }

    private final Map<Long, Emission> emissions = new HashMap<>();

    /** Points across a ground disc at a low height band. */
    private static MaterialEmitters.Source disc(Frame frame, float radiusScale, float yBase, float yRange) {
        return random -> {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double rr = frame.radius * radiusScale * Math.sqrt(random.nextDouble());
            return new MaterialEmitters.Sample(new Vec3(
                    frame.center.x + Math.cos(angle) * rr,
                    frame.center.y + yBase + random.nextDouble() * yRange,
                    frame.center.z + Math.sin(angle) * rr), Vec3.ZERO);
        };
    }

    /** Points in the charge column with an inward drift velocity. */
    private static MaterialEmitters.Source gathering(Frame frame) {
        return random -> {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double rr = frame.radius * (0.35 + random.nextDouble() * 0.75);
            Vec3 pos = new Vec3(
                    frame.center.x + Math.cos(angle) * rr,
                    frame.center.y + 0.10 + random.nextDouble() * frame.columnHeight,
                    frame.center.z + Math.sin(angle) * rr);
            Vec3 vel = new Vec3(
                    (frame.center.x - pos.x) * 0.10,
                    0.015,
                    (frame.center.z - pos.z) * 0.10);
            return new MaterialEmitters.Sample(pos, vel);
        };
    }

    private static BlockState groundState(ClientLevel level, Vec3 center) {
        BlockState state = level.getBlockState(BlockPos.containing(center.x, center.y - 0.5, center.z));
        return state.isAir() ? Blocks.STONE.defaultBlockState() : state;
    }

    private Emission buildEmission(ClientLevel level, OxChargeRenderState state) {
        Emission emission = new Emission();
        Frame frame = emission.frame;
        frame.center = new Vec3(state.wx, state.wy, state.wz);
        int seed = (int) state.seed;
        BlockState ground = groundState(level, frame.center);
        emission.plasma = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xFF7A1A, 1.1f, seed ^ 0xB1A6,
                        Vec3.ZERO, 0.01f),
                gathering(frame), 210f, 280);
        emission.vapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(0xD8C6A8, 1.9f, seed ^ 0xB1A7,
                        new Vec3(0.0, 0.05, 0.0), 0.03f),
                disc(frame, 1.0f, 0.08f, 0.50f), 46f, 80);
        emission.dust = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(0xA0825F, 1.5f, seed ^ 0xD057,
                        new Vec3(0.0, 0.03, 0.0), 0.03f),
                disc(frame, 1.0f, 0.04f, 0.16f), 120f, 170);
        emission.ember = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.ember(new Vec3(0.0, 0.42, 0.0), 0.05f),
                disc(frame, 0.70f, 0.05f, 0.05f), 55f, 90);
        emission.chunks = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(new BlockParticleOption(DbaParticles.BLOCK_CHUNKS, ground),
                        new Vec3(0.0, 0.24, 0.0), 0.08f),
                disc(frame, 0.95f, 0.05f, 0.05f), 20f, 40);
        return emission;
    }

    @Override
    public void submit(OxChargeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float charge = state.chargeRatio;
        float age = state.age;

        float chargeEase = charge * charge * (3.0f - 2.0f * charge);
        float baseRadius = 1.2f + (chargeEase * 2.6f);
        // Lifecycle: the emission still builds in over the first 6 ticks,
        // matching the bespoke fade in.
        float fadeIn = FxEnvelope.smoothstep(Math.min(1.0f, age / 6.0f));
        float distScale = FxEnvelope.distScale(Math.sqrt(state.distanceToCameraSq));
        float envelope = fadeIn * (0.25f + 0.75f * chargeEase) * distScale;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc != null ? mc.level : null;
        if (level != null && envelope > 0.01f) {
            Emission emission = emissions.get(state.seed);
            if (emission == null || age < emission.lastAge - 1.0f) {
                emission = buildEmission(level, state);
                emissions.put(state.seed, emission);
                if (emissions.size() > 64) emissions.clear();
            }
            emission.frame.center = new Vec3(state.wx, state.wy, state.wz);
            emission.frame.radius = baseRadius;
            emission.frame.columnHeight = 0.35f + chargeEase * 2.4f;
            float delta = emission.lastAge < 0f ? 0.05f
                    : Math.min(0.05f, Math.max(0.0f, (age - emission.lastAge) / 20.0f));
            emission.lastAge = age;
            emission.plasma.tick(delta, envelope);
            emission.vapor.tick(delta, envelope);
            emission.dust.tick(delta, envelope);
            emission.chunks.tick(delta, envelope);
            // Rising heat tendrils only exist above 25 percent charge, as before.
            if (charge >= 0.25f) {
                emission.ember.tick(delta, envelope);
            }
        }

        super.submit(state, poseStack, collector, cameraState);
    }
}
