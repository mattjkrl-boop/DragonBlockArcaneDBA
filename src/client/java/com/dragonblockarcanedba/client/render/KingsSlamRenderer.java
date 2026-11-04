package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.entity.KingsSlamEntity;
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
import java.util.Random;

/**
 * Entity Renderer for Ox King's Slam (Normal and Flawless) in Minecraft 26.2.
 * Complex pass: a layered composition at class L. The pieces layer is two
 * ComplexGeometry builders at full disc radius: fracturePlates as solid
 * fractured ground plates through submitSolid, and a crackFan of glowing
 * energy cracks through submitEnergy on the dissolve shader. The attack
 * upgrade layers a second, shorter crackFan inside the web and stands
 * six short rock spikes at the disc rim through submitSolidLit, erupting
 * as the slam front reaches them. These are the
 * fast near-field fragments and energy structure; the big physical chunks
 * are the DebrisSpawner field the entity already spawns server side. The
 * complex particle layer is a large SHARD_CHIP burst at impact plus a
 * continuous chip stream across the disc. The material filler stays as
 * before: compressed ground dust and shock vapor rolling across the impact
 * disc, physical debris chunks of the actual ground block, embers, and hot
 * plasma. The flawless variant adds plasma and vapor geyser emission at
 * seeded anchor points inside the disc. Growth curve, fade, radii, and
 * lifetime are unchanged from the bespoke build.
 */
public class KingsSlamRenderer extends EntityRenderer<KingsSlamEntity, KingsSlamRenderer.KingsSlamRenderState> {

    public KingsSlamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class KingsSlamRenderState extends EntityRenderState {
        public float radius = 10.0f;
        public boolean isFlawless = false;
        public float age = 0;
        public int lifetime = 40;
        public long seed = 0;
        public double wx = 0;
        public double wy = 0;
        public double wz = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(KingsSlamEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(KingsSlamEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public KingsSlamRenderState createRenderState() {
        return new KingsSlamRenderState();
    }

    @Override
    public void extractRenderState(KingsSlamEntity entity, KingsSlamRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.isFlawless = entity.isFlawless();
        state.age = entity.tickCount + partialTicks;
        state.lifetime = entity.getLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    /** Mutable per-frame emission data read by the sources. Data only. */
    private static final class Frame {
        Vec3 center = Vec3.ZERO;
        float radius = 1.0f;
    }

    private static final class Emission {
        final Frame frame = new Frame();
        MaterialEmitters.Emitter[] emitters = new MaterialEmitters.Emitter[0];
        Vec3[] geyserOffsets = new Vec3[0];
        boolean flawless = false;
        boolean burstFired = false;
        float lastAge = -1f;
    }

    private final Map<Long, Emission> emissions = new HashMap<>();

    /** Points across the impact disc at a fixed height band. */
    private static MaterialEmitters.Source disc(Frame frame, float yBase, float yRange) {
        return random -> {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double rr = frame.radius * Math.sqrt(random.nextDouble());
            return new MaterialEmitters.Sample(new Vec3(
                    frame.center.x + Math.cos(angle) * rr,
                    frame.center.y + yBase + random.nextDouble() * yRange,
                    frame.center.z + Math.sin(angle) * rr), Vec3.ZERO);
        };
    }

    /** Points at one of the seeded geyser anchors, chosen per sample. */
    private static MaterialEmitters.Source geyserAnchors(Frame frame, Emission emission) {
        return random -> {
            Vec3[] offsets = emission.geyserOffsets;
            if (offsets.length == 0) return new MaterialEmitters.Sample(frame.center, Vec3.ZERO);
            return new MaterialEmitters.Sample(frame.center.add(offsets[random.nextInt(offsets.length)]), Vec3.ZERO);
        };
    }

    private static BlockState groundState(ClientLevel level, Vec3 center) {
        BlockState state = level.getBlockState(BlockPos.containing(center.x, center.y - 0.5, center.z));
        return state.isAir() ? Blocks.STONE.defaultBlockState() : state;
    }

    private Emission buildEmission(ClientLevel level, KingsSlamRenderState state) {
        Emission emission = new Emission();
        emission.flawless = state.isFlawless;
        Frame frame = emission.frame;
        frame.center = new Vec3(state.wx, state.wy, state.wz);
        int seed = (int) state.seed;
        int hotColor = state.isFlawless ? 0xFFD24A : 0xFF8C1F;

        // Flawless geyser anchors: 8 seeded points in the same distance band
        // the bespoke geysers used (0.45 to 0.85 of the radius).
        if (state.isFlawless) {
            Random rng = new Random(state.seed ^ 0x6E15L);
            emission.geyserOffsets = new Vec3[8];
            for (int g = 0; g < emission.geyserOffsets.length; g++) {
                double ang = (g / (double) emission.geyserOffsets.length) * Math.PI * 2.0 + rng.nextDouble() * 0.2;
                float dist = state.radius * (0.45f + rng.nextFloat() * 0.40f);
                emission.geyserOffsets[g] = new Vec3(
                        Math.cos(ang) * dist, 0.05, Math.sin(ang) * dist);
            }
        }

        BlockState ground = groundState(level, frame.center);
        emission.emitters = new MaterialEmitters.Emitter[]{
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.dust(0xA0825F, 1.8f, seed ^ 0xD057,
                                new Vec3(0.0, 0.04, 0.0), 0.04f),
                        disc(frame, 0.05f, 0.30f), 260f, 340),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.vapor(0xD8C6A8, 2.2f, seed ^ 0x7E57,
                                new Vec3(0.0, 0.06, 0.0), 0.03f),
                        disc(frame, 0.10f, 0.60f), 70f, 110),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(hotColor, 1.15f, seed ^ 0xCA1D,
                                new Vec3(0.0, 0.01, 0.0), 0.01f),
                        disc(frame, 0.04f, 0.06f), 110f, 160),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.ember(new Vec3(0.0, 0.14, 0.0), 0.06f),
                        disc(frame, 0.05f, 0.20f), 44f, 70),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.fixed(new BlockParticleOption(DbaParticles.BLOCK_CHUNKS, ground),
                                new Vec3(0.0, 0.32, 0.0), 0.12f),
                        disc(frame, 0.05f, 0.10f), 54f, 90),
                // Complex particle layer: stone chips thrown off the plates
                // for the whole life of the slam, under the continuous cap.
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP,
                                new Vec3(0.0, 0.30, 0.0), 0.13f),
                        disc(frame, 0.05f, 0.12f), 30f, 44),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(hotColor, 1.5f, seed ^ 0x6E15,
                                new Vec3(0.0, 0.62, 0.0), 0.06f),
                        geyserAnchors(frame, emission), state.isFlawless ? 170f : 0f, 220),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.vapor(0xE8D9BE, 2.4f, seed ^ 0x6E16,
                                new Vec3(0.0, 0.40, 0.0), 0.08f),
                        geyserAnchors(frame, emission), state.isFlawless ? 60f : 0f, 90),
        };
        return emission;
    }

    @Override
    public void submit(KingsSlamRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float radius = state.radius;
        float age = state.age;
        float life = Math.min(1.0f, age / (float) Math.max(1, state.lifetime));
        float growT = Math.min(1.0f, life / 0.55f);
        float grow = 1.0f - (1.0f - growT) * (1.0f - growT) * (1.0f - growT);
        float exitT = life <= 0.62f ? 0.0f : Math.min(1.0f, (life - 0.62f) / 0.38f);
        float fade = 1.0f - exitT * exitT * (3.0f - 2.0f * exitT);

        float distScale = FxEnvelope.distScale(Math.sqrt(state.distanceToCameraSq));
        float shockR = Math.max(0.2f, radius * grow);
        float envelope = fade * distScale;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc != null ? mc.level : null;
        if (level != null && envelope > 0.01f) {
            Emission emission = emissions.get(state.seed);
            if (emission == null || emission.flawless != state.isFlawless || age < emission.lastAge - 1.0f) {
                emission = buildEmission(level, state);
                emissions.put(state.seed, emission);
                if (emissions.size() > 64) emissions.clear();
            }
            Frame frame = emission.frame;
            frame.center = new Vec3(state.wx, state.wy, state.wz);
            frame.radius = shockR;
            float delta = emission.lastAge < 0f ? 0.05f
                    : Math.min(0.05f, Math.max(0.0f, (age - emission.lastAge) / 20.0f));
            emission.lastAge = age;
            for (MaterialEmitters.Emitter emitter : emission.emitters) {
                emitter.tick(delta, envelope);
            }
            // Complex particle layer, burst half: one large chip burst at
            // impact, inside the class L burst budget of 60.
            if (!emission.burstFired && age <= 2.0f) {
                emission.burstFired = true;
                MaterialEmitters.burst(level,
                        MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP,
                                new Vec3(0.0, 0.38, 0.0), 0.24f),
                        MaterialEmitters.groundArea(frame.center, Math.max(0.4f, shockR * 0.55f),
                                frame.center.y + 0.10),
                        48);
            }
        }

        // Pieces layer, in the local ground frame: solid fracture plates
        // across the whole impact disc and a glowing crack fan just inside
        // it. Both follow the same growth curve as the emission disc.
        if (fade > 0.01f && shockR >= 0.35f) {
            final long geoSeed = state.seed;
            final int hotColor = state.isFlawless ? 0xFFD24A : 0xFF8C1F;
            final float geoRadius = shockR;
            final float geoFade = fade;
            final float geoDist = distScale;
            ComplexGeometry.submitSolid(collector, poseStack, (pose, buffer) ->
                    ComplexGeometry.fracturePlates(pose, buffer, geoSeed ^ 0x9A7EL,
                            geoRadius, 0.42f, hotColor, 0x4A3826, 0.95f, geoFade, geoDist));
            ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) ->
                    ComplexGeometry.crackFan(pose, buffer, geoSeed ^ 0xC4A9L,
                            geoRadius * 0.94f, 0xFFE9B0, hotColor, 0.95f, geoFade, geoDist));
            // Attack upgrade: a second, shorter crack fan layered
            // inside the main web, so the disc's crack structure reads
            // branched and dense at the center instead of one clean
            // fan. crackFan has no branch-count parameter, so the
            // spec's extra branches land as this inner fan (change
            // request filed in the owner report).
            ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) ->
                    ComplexGeometry.crackFan(pose, buffer, geoSeed ^ 0xC4B1L,
                            geoRadius * 0.55f, 0xFFE9B0, hotColor, 0.95f, geoFade, geoDist));
            // Attack upgrade: six short rock spikes scattered at the
            // disc rim, erupting as the slam front reaches them. Each
            // spike is its own SCATTER call of one, centered on a
            // seeded rim point, so the six stand at the rim instead of
            // filling the disc. They rise on the disc's own growth
            // (full height just after the front passes), hold, and
            // crack away with the slam's fade envelope. Physical rock
            // in the same low-chroma stone family as the fissure wall,
            // drawn lit through submitSolidLit: no glow on stone.
            final float spikeRise = FxEnvelope.smoothstep(
                    Math.max(0.0f, Math.min(1.0f, (life - 0.38f) / 0.20f)));
            final int slamLight = state.lightCoords;
            ComplexGeometry.submitSolidLit(collector, poseStack, slamLight, (pose, buffer) -> {
                ComplexGeometry.Rng rimRng = new ComplexGeometry.Rng(geoSeed ^ 0x5917L);
                for (int i = 0; i < 6; i++) {
                    double ang = i * (Math.PI * 2.0 / 6.0) + rimRng.signed() * 0.35;
                    float rimR = geoRadius * 0.93f;
                    Vec3 spikeCenter = new Vec3(Math.cos(ang) * rimR, 0.0, Math.sin(ang) * rimR);
                    ComplexGeometry.spikeEruption(pose, buffer, geoSeed ^ 0x5917L + i * 0x9E37L,
                            spikeCenter, new Vec3(0.0, 0.0, 1.0), ComplexGeometry.SpikeLayout.SCATTER, 1, 0.55f,
                            0.55f, spikeRise, 0.0f,
                            0x2B241E, 0x77644C, 0.95f, geoFade, geoDist);
                }
            });
        }
    }
}
