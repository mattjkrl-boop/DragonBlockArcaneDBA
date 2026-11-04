package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.entity.AzureSonicQuakeEntity;
import com.dragonblockarcanedba.particle.DbaParticles;
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
 * Entity Renderer for Sonic Quake Impact Shockwave in Minecraft 26.2.
 *
 * Complex pass: the quake is a layered composition. The pieces layer
 * is fracturePlates plus a crackFan, both expanding with the wave
 * radius so the ground breaks and the energy web spreads exactly as
 * far as the crest has reached, never ahead of it. The plates submit
 * solid with faceted shading; the cracks submit through the shader.
 * The complex particle layer is SHARD_CHIP: a one-shot burst at the
 * epicenter on the first frames plus chips shed along the crest. The
 * material emission from the no-shapes pass stays as filler at the
 * crest: compressed ground dust and shock vapor rolling outward,
 * physical debris chunks of the actual ground block, sparks, and
 * tempest plasma licking along the crest line. The epicenter throws
 * dust from a shrinking core area. Wave timing, radius growth, and
 * the fade envelope are unchanged from the bespoke build. The entity
 * already spawns a real debris field server side; these pieces are
 * the fast near-field fragments debris entities cannot time.
 */
public class AzureSonicQuakeRenderer extends EntityRenderer<AzureSonicQuakeEntity, AzureSonicQuakeRenderer.QuakeRenderState> {

    public AzureSonicQuakeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class QuakeRenderState extends EntityRenderState {
        public float maxRadius = 8.5f;
        public float age = 0;
        public long seed = 0;
        public double wx, wy, wz;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(AzureSonicQuakeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(AzureSonicQuakeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public QuakeRenderState createRenderState() {
        return new QuakeRenderState();
    }

    @Override
    public void extractRenderState(AzureSonicQuakeEntity entity, QuakeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.maxRadius = entity.getMaxRadius();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getRenderSeed();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    /** Mutable per-frame emission data read by the sources. Data only. */
    private static final class Frame {
        Vec3 center = Vec3.ZERO;
        float radius = 1.0f;
        float coreRadius = 1.0f;
        float crestHeight = 0.65f;
    }

    private static final class Emission {
        final Frame frame = new Frame();
        ClientLevel level;
        MaterialEmitters.Emitter[] emitters = new MaterialEmitters.Emitter[0];
        float lastAge = -1f;
        boolean shardBurstDone = false;
    }

    private final Map<Long, Emission> emissions = new HashMap<>();

    /** Points on the expanding crest, with an outward drift velocity. */
    private static MaterialEmitters.Source crestRing(Frame frame, float yBase, float yFrac, float outward) {
        return random -> {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double rr = frame.radius + (random.nextDouble() * 2.0 - 1.0) * 0.5;
            Vec3 pos = new Vec3(
                    frame.center.x + Math.cos(angle) * rr,
                    frame.center.y + yBase + random.nextDouble() * frame.crestHeight * yFrac,
                    frame.center.z + Math.sin(angle) * rr);
            Vec3 vel = new Vec3(Math.cos(angle) * outward, 0.0, Math.sin(angle) * outward);
            return new MaterialEmitters.Sample(pos, vel);
        };
    }

    /** Points across the shrinking epicenter disc. */
    private static MaterialEmitters.Source epicenter(Frame frame) {
        return random -> {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double rr = frame.coreRadius * Math.sqrt(random.nextDouble());
            Vec3 pos = new Vec3(
                    frame.center.x + Math.cos(angle) * rr,
                    frame.center.y + 0.05 + random.nextDouble() * 0.15,
                    frame.center.z + Math.sin(angle) * rr);
            return new MaterialEmitters.Sample(pos, Vec3.ZERO);
        };
    }

    private static BlockState groundState(ClientLevel level, Vec3 center) {
        BlockState state = level.getBlockState(BlockPos.containing(center.x, center.y - 0.5, center.z));
        return state.isAir() ? Blocks.STONE.defaultBlockState() : state;
    }

    private Emission buildEmission(ClientLevel level, QuakeRenderState state) {
        Emission emission = new Emission();
        emission.level = level;
        Frame frame = emission.frame;
        frame.center = new Vec3(state.wx, state.wy, state.wz);
        int seed = (int) state.seed;
        BlockState ground = groundState(level, frame.center);
        emission.emitters = new MaterialEmitters.Emitter[]{
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.dust(0x7E8A8F, 1.5f, seed ^ 0xD057,
                                new Vec3(0.0, 0.05, 0.0), 0.03f),
                        crestRing(frame, 0.04f, 0.55f, 0.14f), 250f, 340),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.vapor(0xCFE8F2, 2.0f, seed ^ 0x7E57,
                                new Vec3(0.0, 0.04, 0.0), 0.03f),
                        crestRing(frame, 0.04f, 0.40f, 0.10f), 100f, 140),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(0x4DF2FF, 1.1f, seed ^ 0xBE10,
                                new Vec3(0.0, 0.05, 0.0), 0.02f),
                        crestRing(frame, 0.03f, 0.20f, 0.03f), 90f, 130),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.fixed(new BlockParticleOption(DbaParticles.BLOCK_CHUNKS, ground),
                                new Vec3(0.0, 0.30, 0.0), 0.10f),
                        crestRing(frame, 0.04f, 0.25f, 0.05f), 46f, 80),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.fixed(DbaParticles.CRIT_SPARK,
                                new Vec3(0.0, 0.12, 0.0), 0.08f),
                        crestRing(frame, 0.04f, 0.30f, 0.06f), 40f, 70),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.dust(0xD8E2E6, 1.3f, seed ^ 0xC2AC,
                                new Vec3(0.0, 0.10, 0.0), 0.04f),
                        epicenter(frame), 70f, 100),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP,
                                new Vec3(0.0, 0.18, 0.0), 0.08f),
                        crestRing(frame, 0.04f, 0.30f, 0.06f), 20f, 40),
        };
        return emission;
    }

    @Override
    public void submit(QuakeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float maxLife = 18.0f;
        float progress = Math.min(1.0f, state.age / maxLife);
        if (progress >= 1.0f) return;

        float expansionEase = (float) Math.sin(progress * (Math.PI / 2.0));
        float currentRadius = Math.max(0.5f, expansionEase * state.maxRadius);
        float alpha = (1.0f - (progress * progress)) * 0.90f;

        Vec3 center = new Vec3(state.wx, state.wy, state.wz);
        float ds = MaterialEmitters.distanceScale(center);
        float envelope = (alpha / 0.90f) * ds;

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
            emission.frame.radius = currentRadius;
            emission.frame.coreRadius = Math.max(0.2f, (1.0f - progress) * 2.0f) * 1.2f;
            emission.frame.crestHeight = 0.65f * (1.0f - progress * 0.8f);
            float delta = emission.lastAge < 0f ? 0.05f
                    : Math.min(0.05f, Math.max(0.0f, (state.age - emission.lastAge) / 20.0f));
            emission.lastAge = state.age;
            for (MaterialEmitters.Emitter emitter : emission.emitters) {
                emitter.tick(delta, envelope);
            }
            // One-shot chip burst at the epicenter, once per quake.
            if (!emission.shardBurstDone && progress < 0.35f) {
                emission.shardBurstDone = true;
                MaterialEmitters.burst(level,
                        MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP,
                                new Vec3(0.0, 0.22, 0.0), 0.12f),
                        epicenter(emission.frame), 30);
            }
        }

        // Pieces layer, class L: two builders. Fracture plates and the
        // crack web both grow with the current wave radius, so nothing
        // shows ahead of the crest the damage follows.
        if (envelope > 0.01f) {
            final float plateRadius = currentRadius;
            final float crackRadius = currentRadius * 0.92f;
            final float lift = 0.30f * (1.0f - progress * 0.45f);
            final float geoEnv = envelope;
            final float geoDs = ds;
            final long plateSeed = state.seed ^ 0xF2ACL;
            final long crackSeed = state.seed ^ 0xC4ACL;
            ComplexGeometry.submitSolid(collector, poseStack, (pose, buffer) ->
                    ComplexGeometry.fracturePlates(pose, buffer, plateSeed, plateRadius, lift,
                            0xA8C8D2, 0x46545C, 0.95f, geoEnv, geoDs));
            ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) ->
                    ComplexGeometry.crackFan(pose, buffer, crackSeed, crackRadius,
                            0xFFFFFF, 0x4DF2FF, 0.90f, geoEnv, geoDs));
        }

        super.submit(state, poseStack, collector, cameraState);
    }
}
