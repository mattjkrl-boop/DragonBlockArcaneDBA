package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.entity.OxFissureEntity;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Entity Renderer for Ox King's Ground Fissure in Minecraft 26.2.
 * Complex pass: a layered composition at class M. The pieces layer is two
 * ComplexGeometry builders: a crackFan through submitEnergy on the
 * dissolve shader, laid twice along the fissure line under the fissure
 * yaw so the glowing web runs the length of the trench as its core, and
 * fracturePlates through submitSolid as solid fractured ground at the
 * origin. Attack upgrade: the fissure gains its flagship body, a rock
 * wall built from two overlapping rockWall masses at uneven heights,
 * lengths, and angles along the line, drawn lit through submitSolidLit
 * so the ridge reads as a jagged rock massif heaving out of the trench,
 * not a neat row of slabs. It rises over the opening ticks, holds 4
 * ticks, then slams flat at tick 12 and leaves a full-length crackFan
 * behind. The complex particle layer is SHARD_CHIP sprites sampled along
 * the crack line. The material filler stays as before: hot plasma glowing
 * in the crack, a white-hot core stream, ground dust, embers, and
 * occasional physical debris chunks of the actual ground block, all
 * sampled along the crack line as data. The crack follows the same yaw
 * and the same 4.2 by 2.0 footprint the entity damages, and the fade in,
 * pulse, and fade out are unchanged from the bespoke build.
 */
public class OxFissureRenderer extends EntityRenderer<OxFissureEntity, OxFissureRenderer.OxFissureRenderState> {
    public OxFissureRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class OxFissureRenderState extends EntityRenderState {
        public float ageInTicks = 0;
        public long seed = 0;
        public float yRot = 0;
        public double wx = 0;
        public double wy = 0;
        public double wz = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(OxFissureEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(OxFissureEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public OxFissureRenderState createRenderState() {
        return new OxFissureRenderState();
    }

    @Override
    public void extractRenderState(OxFissureEntity entity, OxFissureRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.ageInTicks = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
        state.yRot = entity.getYRot();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    /** Mutable per-frame emission data read by the sources. Data only. */
    private static final class Frame {
        Vec3 center = Vec3.ZERO;
        double dirX = 0.0;
        double dirZ = 1.0;
    }

    private static final class Emission {
        final Frame frame = new Frame();
        MaterialEmitters.Emitter[] emitters = new MaterialEmitters.Emitter[0];
        boolean wallDustFired = false;
        boolean slamDustFired = false;
        float lastAge = -1f;
    }

    private final Map<Long, Emission> emissions = new HashMap<>();

    /**
     * Points along the crack line: length 3.9 along the fissure axis with a
     * lateral jitter inside the 2.0 wide damage footprint. Data only.
     */
    private static MaterialEmitters.Source crackLine(Frame frame, float lateral) {
        return random -> {
            double along = (random.nextDouble() * 2.0 - 1.0) * 1.95;
            double side = (random.nextDouble() * 2.0 - 1.0) * lateral;
            double px = -frame.dirZ;
            double pz = frame.dirX;
            return new MaterialEmitters.Sample(new Vec3(
                    frame.center.x + frame.dirX * along + px * side,
                    frame.center.y + 0.05 + random.nextDouble() * 0.05,
                    frame.center.z + frame.dirZ * along + pz * side), Vec3.ZERO);
        };
    }

    private static BlockState groundState(ClientLevel level, Vec3 center) {
        BlockState state = level.getBlockState(BlockPos.containing(center.x, center.y - 0.5, center.z));
        return state.isAir() ? Blocks.STONE.defaultBlockState() : state;
    }

    private Emission buildEmission(ClientLevel level, OxFissureRenderState state) {
        Emission emission = new Emission();
        Frame frame = emission.frame;
        frame.center = new Vec3(state.wx, state.wy, state.wz);
        int seed = (int) state.seed;
        BlockState ground = groundState(level, frame.center);
        emission.emitters = new MaterialEmitters.Emitter[]{
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(0xFF6A12, 1.20f, seed ^ 0x7007,
                                Vec3.ZERO, 0.008f),
                        crackLine(frame, 0.50f), 130f, 180),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(0xFFD98A, 0.70f, seed ^ 0x7008,
                                Vec3.ZERO, 0.005f),
                        crackLine(frame, 0.22f), 42f, 70),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.dust(0xA0825F, 1.4f, seed ^ 0xD057,
                                new Vec3(0.0, 0.03, 0.0), 0.02f),
                        crackLine(frame, 0.85f), 70f, 110),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.ember(new Vec3(0.0, 0.10, 0.0), 0.04f),
                        crackLine(frame, 0.45f), 26f, 45),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.fixed(new BlockParticleOption(DbaParticles.BLOCK_CHUNKS, ground),
                                new Vec3(0.0, 0.20, 0.0), 0.06f),
                        crackLine(frame, 0.60f), 12f, 25),
                // Complex particle layer: stone chips shed along the line,
                // under the continuous rate cap.
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP,
                                new Vec3(0.0, 0.18, 0.0), 0.06f),
                        crackLine(frame, 0.60f), 24f, 36),
        };
        return emission;
    }

    @Override
    public void submit(OxFissureRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float pulse = 0.75f + 0.25f * (float) Math.sin(state.ageInTicks * 0.25f);
        float life = state.ageInTicks / (float) OxFissureEntity.MAX_LIFETIME;
        float inT = Math.min(1.0f, state.ageInTicks / 4.0f);
        float inEase = 1.0f - (1.0f - inT) * (1.0f - inT) * (1.0f - inT);
        float outT = life <= 0.70f ? 0.0f : Math.min(1.0f, (life - 0.70f) / 0.30f);
        float fade = inEase * (1.0f - outT * outT * (3.0f - 2.0f * outT));
        float envelope = fade * pulse;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc != null ? mc.level : null;
        if (level != null && envelope > 0.01f) {
            Emission emission = emissions.get(state.seed);
            if (emission == null || state.ageInTicks < emission.lastAge - 1.0f) {
                emission = buildEmission(level, state);
                emissions.put(state.seed, emission);
                if (emissions.size() > 64) emissions.clear();
            }
            Frame frame = emission.frame;
            frame.center = new Vec3(state.wx, state.wy, state.wz);
            // The bespoke build rotated its pose by -yRot around Y; the same
            // rotation applied to the local +Z axis gives the crack direction.
            double a = Math.toRadians(-state.yRot);
            frame.dirX = Math.sin(a);
            frame.dirZ = Math.cos(a);
            float delta = emission.lastAge < 0f ? 0.05f
                    : Math.min(0.05f, Math.max(0.0f, (state.ageInTicks - emission.lastAge) / 20.0f));
            emission.lastAge = state.ageInTicks;
            for (MaterialEmitters.Emitter emitter : emission.emitters) {
                emitter.tick(delta, envelope);
            }
            // Wall accompaniment (attack upgrade): dust shed along the
            // line while the rock wall heaves out, and a second dust
            // burst the moment it slams flat at tick 12. One-shot bursts
            // on the same crack line source the continuous layers use;
            // the real thrown chunks are the entity's DebrisSpawner
            // fields on the same two beats.
            if (!emission.wallDustFired && state.ageInTicks >= 0.5f && state.ageInTicks <= 4.0f) {
                emission.wallDustFired = true;
                MaterialEmitters.burst(level,
                        MaterialEmitters.Preset.dust(0xA0825F, 1.5f, (int) state.seed ^ 0xD058,
                                new Vec3(0.0, 0.10, 0.0), 0.05f),
                        crackLine(frame, 0.70f), 36);
            }
            if (!emission.slamDustFired && state.ageInTicks >= 12.0f && state.ageInTicks <= 16.0f) {
                emission.slamDustFired = true;
                MaterialEmitters.burst(level,
                        MaterialEmitters.Preset.dust(0xA0825F, 1.6f, (int) state.seed ^ 0xD059,
                                new Vec3(0.0, 0.08, 0.0), 0.06f),
                        crackLine(frame, 0.90f), 30);
            }
        }

        // Pieces layer, in the local ground frame rotated by the fissure
        // yaw, the same rotation the bespoke build used, so local +Z runs
        // along the trench. Two crack fans centered a block either side of
        // the origin lay the glowing web along the full line; solid plates
        // sit at the origin where the ax struck.
        if (fade > 0.01f) {
            final long geoSeed = state.seed;
            final float geoFade = fade;
            final float geoDist = FxEnvelope.distScale(Math.sqrt(Math.max(0.0, state.distanceToCameraSq)));
            poseStack.pushPose();
            poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
            ComplexGeometry.submitSolid(collector, poseStack, (pose, buffer) ->
                    ComplexGeometry.fracturePlates(pose, buffer, geoSeed ^ 0x9A7EL,
                            1.55f, 0.26f, 0x8A6A44, 0x3A2C1E, 0.95f, geoFade, geoDist));
            poseStack.pushPose();
            poseStack.translate(0.0, 0.0, 0.95);
            ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) ->
                    ComplexGeometry.crackFan(pose, buffer, geoSeed ^ 0xC4A9L,
                            1.35f, 0xFFD98A, 0xFF6A12, 0.95f, geoFade, geoDist));
            poseStack.popPose();
            poseStack.pushPose();
            poseStack.translate(0.0, 0.0, -0.95);
            ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) ->
                    ComplexGeometry.crackFan(pose, buffer, geoSeed ^ 0xC4B0L,
                            1.35f, 0xFFD98A, 0xFF6A12, 0.95f, geoFade, geoDist));
            poseStack.popPose();

            // The flagship wall (attack upgrade): the fissure's visual
            // body. Two overlapping rockWall masses along the line, at
            // uneven heights, lengths, arcs, and angles, so the ridge
            // reads as a jagged rock massif heaving out of the ground,
            // never a neat row of even slabs. Local +Z runs along the
            // trench, so the wall facing points across it and each
            // wall's line runs along the fissure path. Beats ride the
            // fissure's own opening: the main mass rises over 7 ticks
            // and the shoulder cluster over 8 (the builder's uniform
            // drop makes taller slabs break the surface first, so each
            // mass staggers slab by slab on its own), both HOLD to tick
            // 12, then SLAM flat over 3 ticks. Physical rock, drawn
            // lit: no glow on stone. The palette stays in the fissure
            // plates' earth family but pulled grayer and darker than
            // the plates themselves, because saturated warm browns
            // read as toy rock at wall scale; real broken stone sits
            // low-chroma with a dark buried base and a weathered top.
            // The trench and its damage beat underneath are untouched
            // and outlive the wall.
            final float wallAge = state.ageInTicks;
            final float wallRiseMain = FxEnvelope.smoothstep(Math.min(1.0f, wallAge / 7.0f));
            final float wallRiseShoulder = FxEnvelope.smoothstep(Math.min(1.0f, wallAge / 8.0f));
            final float wallSink = wallAge <= 12.0f ? 0.0f
                    : FxEnvelope.smoothstep(Math.min(1.0f, (wallAge - 12.0f) / 3.0f));
            final int wallLight = state.lightCoords;
            poseStack.pushPose();
            poseStack.translate(0.0, 0.0, -0.6);
            ComplexGeometry.submitSolidLit(collector, poseStack, wallLight, (pose, buffer) ->
                    ComplexGeometry.rockWall(pose, buffer, geoSeed ^ 0xA115L,
                            Vec3.ZERO, new Vec3(1.0, 0.0, 0.0), 10.0f, 3.0f, 0.15f,
                            wallRiseMain, wallSink,
                            0x2B241E, 0x77644C, 0.95f, geoFade, geoDist));
            poseStack.popPose();
            poseStack.pushPose();
            poseStack.translate(0.0, 0.0, 2.4);
            poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(5.0f)));
            ComplexGeometry.submitSolidLit(collector, poseStack, wallLight, (pose, buffer) ->
                    ComplexGeometry.rockWall(pose, buffer, geoSeed ^ 0xA116L,
                            Vec3.ZERO, new Vec3(1.0, 0.0, 0.0), 6.0f, 2.2f, -0.12f,
                            wallRiseShoulder, wallSink,
                            0x262019, 0x685741, 0.95f, geoFade, geoDist));
            poseStack.popPose();

            // Slam web: when the wall slams flat at tick 12 it leaves a
            // full-length crack fan along the line, which then lives
            // under the fissure's normal fade like the trench's own web.
            if (wallAge >= 12.0f) {
                ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) ->
                        ComplexGeometry.crackFan(pose, buffer, geoSeed ^ 0xC4B7L,
                                5.2f, 0xFFD98A, 0xFF6A12, 0.95f, geoFade, geoDist));
            }
            poseStack.popPose();
        }

        super.submit(state, poseStack, collector, cameraState);
    }
}
