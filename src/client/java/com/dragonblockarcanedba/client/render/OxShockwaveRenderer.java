package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.entity.OxShockwaveEntity;
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
 * Entity Renderer for Ox King's Groundbreaker Shockwave in Minecraft 26.2.
 * Complex pass: a layered composition at class L. The pieces layer is two
 * ComplexGeometry builders: fracturePlates as solid fractured ground at
 * the origin through submitSolid, and a crackFan of glowing energy cracks
 * through submitEnergy on the dissolve shader, spreading outward with the
 * wave front. These are the fast near-field fragments and energy
 * structure; the big physical chunks are the debris the entity already
 * spawns server side. The complex particle layer is a SHARD_CHIP burst at
 * the origin plus a continuous chip stream on the crest. The material
 * filler stays as before: ground dust and shock vapor rolling outward at
 * the crest, physical debris chunks of the actual ground block, embers,
 * and hot plasma licking along the fracture line. Wave timing, radius
 * growth, and the fade envelope are unchanged from the bespoke build.
 */
public class OxShockwaveRenderer extends EntityRenderer<OxShockwaveEntity, OxShockwaveRenderer.OxShockwaveRenderState> {
    public OxShockwaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class OxShockwaveRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public boolean isSubWave = false;
        public float currentRadius = 1.0f;
        public float age = 0;
        public long seed = 0;
        public double wx = 0;
        public double wy = 0;
        public double wz = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(OxShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(OxShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public OxShockwaveRenderState createRenderState() {
        return new OxShockwaveRenderState();
    }

    @Override
    public void extractRenderState(OxShockwaveEntity entity, OxShockwaveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.isSubWave = entity.isSubWave();
        state.currentRadius = entity.getCurrentRadius();
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
        float crestHeight = 1.0f;
    }

    private static final class Emission {
        final Frame frame = new Frame();
        MaterialEmitters.Emitter[] emitters = new MaterialEmitters.Emitter[0];
        boolean burstFired = false;
        float lastAge = -1f;
    }

    private final Map<Long, Emission> emissions = new HashMap<>();

    /** Points on the crest ring, with an outward drift velocity. The
     *  vertical spread scales with the live crest height, so the dust and
     *  vapor column grows and settles with the wave like the bespoke dust
     *  walls did. */
    private static MaterialEmitters.Source crestRing(Frame frame, float yBase, float yRange, float outward) {
        return random -> {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double rr = frame.radius + (random.nextDouble() * 2.0 - 1.0) * 0.6;
            float hScale = Math.max(0.25f, Math.min(2.5f, frame.crestHeight));
            Vec3 pos = new Vec3(
                    frame.center.x + Math.cos(angle) * rr,
                    frame.center.y + yBase + random.nextDouble() * yRange * hScale,
                    frame.center.z + Math.sin(angle) * rr);
            Vec3 vel = new Vec3(Math.cos(angle) * outward, 0.0, Math.sin(angle) * outward);
            return new MaterialEmitters.Sample(pos, vel);
        };
    }

    private static BlockState groundState(ClientLevel level, Vec3 center) {
        BlockState state = level.getBlockState(BlockPos.containing(center.x, center.y - 0.5, center.z));
        return state.isAir() ? Blocks.STONE.defaultBlockState() : state;
    }

    private Emission buildEmission(ClientLevel level, OxShockwaveRenderState state) {
        Emission emission = new Emission();
        Frame frame = emission.frame;
        frame.center = new Vec3(state.wx, state.wy, state.wz);
        int seed = (int) state.seed;
        BlockState ground = groundState(level, frame.center);
        emission.emitters = new MaterialEmitters.Emitter[]{
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.dust(0xA0825F, 1.6f, seed ^ 0xD057,
                                new Vec3(0.0, 0.05, 0.0), 0.03f),
                        crestRing(frame, 0.05f, 0.5f, 0.14f), 240f, 320),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.vapor(0xD8C6A8, 2.0f, seed ^ 0x7E57,
                                new Vec3(0.0, 0.04, 0.0), 0.03f),
                        crestRing(frame, 0.05f, 0.35f, 0.10f), 80f, 120),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(0xFF8C1F, 1.1f, seed ^ 0xBE10,
                                new Vec3(0.0, 0.05, 0.0), 0.02f),
                        crestRing(frame, 0.04f, 0.10f, 0.02f), 70f, 120),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.ember(new Vec3(0.0, 0.12, 0.0), 0.05f),
                        crestRing(frame, 0.05f, 0.25f, 0.06f), 36f, 60),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.fixed(new BlockParticleOption(DbaParticles.BLOCK_CHUNKS, ground),
                                new Vec3(0.0, 0.30, 0.0), 0.10f),
                        crestRing(frame, 0.05f, 0.10f, 0.05f), 42f, 80),
                // Complex particle layer: stone chips riding the crest,
                // under the continuous rate cap.
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP,
                                new Vec3(0.0, 0.26, 0.0), 0.11f),
                        crestRing(frame, 0.05f, 0.12f, 0.05f), 30f, 44),
        };
        return emission;
    }

    @Override
    public void submit(OxShockwaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float radius = Math.max(0.5f, state.currentRadius);
        float charge = state.chargeRatio;
        boolean subWave = state.isSubWave;
        float age = state.age;

        float waveScale = subWave ? 0.75f : 1.0f;
        float cragMaxHeight = (1.2f + charge * 2.8f) * waveScale;
        // Lifetime recomputed from the synced charge exactly as the bespoke
        // build did (maxTicks = max(10, maxRadius x 1.3)), so the emission
        // envelope fades fully to zero across the final 30 percent.
        float maxRadius = (4.0f + Math.max(0.05f, Math.min(1.0f, charge)) * 20.0f) * waveScale;
        float maxTicks = Math.max(10.0f, maxRadius * 1.3f);
        float lifeProgress = Math.min(1.0f, age / maxTicks);
        float riseT = Math.min(1.0f, lifeProgress / 0.22f);
        float rise = 1.0f - (1.0f - riseT) * (1.0f - riseT) * (1.0f - riseT);
        float exitT = lifeProgress <= 0.70f ? 0.0f : Math.min(1.0f, (lifeProgress - 0.70f) / 0.30f);
        float endFade = 1.0f - exitT * exitT * (3.0f - 2.0f * exitT);
        float crest = rise * (1.0f - exitT * 0.62f);

        float distScale = FxEnvelope.distScale(Math.sqrt(state.distanceToCameraSq));
        float envelope = endFade * (0.30f + 0.70f * crest) * distScale;

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
            emission.frame.radius = radius;
            emission.frame.crestHeight = cragMaxHeight * crest;
            float delta = emission.lastAge < 0f ? 0.05f
                    : Math.min(0.05f, Math.max(0.0f, (age - emission.lastAge) / 20.0f));
            emission.lastAge = age;
            for (MaterialEmitters.Emitter emitter : emission.emitters) {
                emitter.tick(delta, envelope);
            }
            // Complex particle layer, burst half: one chip burst at the
            // origin as the wave leaves it, inside the class L budget.
            if (!emission.burstFired && age <= 2.0f) {
                emission.burstFired = true;
                MaterialEmitters.burst(level,
                        MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP,
                                new Vec3(0.0, 0.34, 0.0), 0.22f),
                        MaterialEmitters.groundArea(emission.frame.center, 1.1,
                                emission.frame.center.y + 0.10),
                        40);
            }
        }

        // Pieces layer, in the local ground frame: solid fracture plates
        // at the origin and a glowing crack fan spreading with the wave.
        float geoEnv = endFade * Math.max(0.25f, crest);
        if (geoEnv > 0.01f) {
            final long geoSeed = state.seed;
            final float plateRadius = (2.6f + charge * 1.8f) * waveScale;
            final float plateLift = (0.30f + charge * 0.28f) * waveScale;
            final float crackRadius = Math.max(1.2f, radius * 0.85f);
            final float geoFade = geoEnv;
            final float geoDist = distScale;
            ComplexGeometry.submitSolid(collector, poseStack, (pose, buffer) ->
                    ComplexGeometry.fracturePlates(pose, buffer, geoSeed ^ 0x9A7EL,
                            plateRadius, plateLift, 0xFF8C1F, 0x4A3826, 0.95f, geoFade, geoDist));
            ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) ->
                    ComplexGeometry.crackFan(pose, buffer, geoSeed ^ 0xC4A9L,
                            crackRadius, 0xFFE9B0, 0xFF8C1F, 0.95f, geoFade, geoDist));
        }

        super.submit(state, poseStack, collector, cameraState);
    }
}
