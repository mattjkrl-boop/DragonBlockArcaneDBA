package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.entity.BanshoShockwaveEntity;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Layered presentation for Bansho Fan shockwaves (class M). The pieces
 * layer is a ComplexGeometry crackFan web plus light fracturePlates at
 * a small lift, read as wind scour rather than rock heave: the cracks
 * glow in the wave colors through the shader submit helper and the
 * plates submit solid with facet shading. Radial waves draw them flat
 * in the world XZ plane at the origin; cone and orient-to-impact waves
 * draw them under the same yaw and pitch rotation the emission frame
 * uses. On top of the scour, the attack upgrade pass adds the PUSH
 * WALL: the wavefront itself heaves up as ground, a rockWall front row
 * of 7 to 9 slabs with a lower backing ridge row behind it, in a
 * palette sampled from the ground under the origin and drawn lit
 * through submitSolidLit, leaning into the travel direction and riding
 * the wave's existing expansion. Cone waves carry one wall across
 * their front; radial waves carry three arcs of it around the ring.
 * The backing row sinks first as the wave dies, the front row follows,
 * and the existing BLOCK_CHUNKS debris stream supplies the crumble.
 * The complex particle layer is a few SHARD_CHIP sprites at the
 * wave front. The material emission from the no-shapes pass stays as
 * filler: cone waves are a rushing vapor front with a pale plasma core,
 * launch dust, and mist; radial waves are a vapor front sampled across
 * the expanding area, a center vapor wash, ground dust, and real block
 * chunks where the burst is close enough to a captured ground block.
 * The 14 tick progress and exit envelope are unchanged, and the piece
 * seeds come from the entity id and max radius, the same synced state
 * the emission already used.
 */
public class BanshoShockwaveRenderer extends EntityRenderer<BanshoShockwaveEntity, BanshoShockwaveRenderer.ShockwaveRenderState> {

    public BanshoShockwaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ShockwaveRenderState extends EntityRenderState {
        public float yaw = 0;
        public float pitch = 0;
        public float maxRadius = 3.0f;
        public boolean isCone = false;
        public boolean orientToImpact = false;
        public float age = 0;
        public double ex, ey, ez;
        public long seed;
        public float progress;
        public float alpha;
        public float coneLength;
        public float coneRadius;
        public float currentRadius;
        public float crestHeight;
        public float coreRadius;
        public Emission emission;
    }

    static final class Emission {
        private final ShockwaveRenderState state;
        private ClientLevel level;
        private long seed;
        private boolean cone;
        private boolean orient;
        private float lastAge = Float.NaN;
        private MaterialEmitters.Emitter vapor;
        private MaterialEmitters.Emitter plasma;
        private MaterialEmitters.Emitter launchDust;
        private MaterialEmitters.Emitter mist;
        private MaterialEmitters.Emitter center;
        private MaterialEmitters.Emitter groundDust;
        private MaterialEmitters.Emitter debris;
        private MaterialEmitters.Emitter chips;
        private BlockState debrisGround;

        Emission(ShockwaveRenderState state) {
            this.state = state;
        }

        void tick(ClientLevel level, float envelope) {
            if (this.level != level || this.seed != state.seed
                    || this.cone != state.isCone || this.orient != state.orientToImpact) {
                create(level);
            }
            updateDebris(level);
            float delta;
            if (Float.isNaN(lastAge) || state.age < lastAge) {
                delta = 1.0f / 60.0f;
            } else {
                delta = Math.min(0.05f, (state.age - lastAge) / 20.0f);
            }
            lastAge = state.age;
            tick(vapor, delta, envelope);
            tick(plasma, delta, envelope);
            tick(launchDust, delta, envelope);
            tick(mist, delta, envelope);
            tick(center, delta, envelope);
            tick(groundDust, delta, envelope);
            tick(debris, delta, envelope);
            tick(chips, delta, envelope);
        }

        private static void tick(MaterialEmitters.Emitter emitter, float delta, float envelope) {
            if (emitter != null) emitter.tick(delta, envelope);
        }

        private void create(ClientLevel level) {
            this.level = level;
            this.seed = state.seed;
            this.cone = state.isCone;
            this.orient = state.orientToImpact;
            this.lastAge = Float.NaN;
            this.debris = null;
            this.debrisGround = null;
            int seed = (int) state.seed;
            // Complex particle layer: a few shard chips riding the wave
            // front, cone or radial, under the continuous budget.
            chips = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP,
                            Vec3.ZERO, 0.05f),
                    random -> state.isCone
                            ? coneSample(state, random, false)
                            : radialFrontSample(state, random),
                    16.0f, 30);
            if (state.isCone) {
                vapor = new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.vapor(0x26FFA6, 1.20f,
                                seed ^ 61, Vec3.ZERO, 0.010f),
                        random -> coneSample(state, random, false), 175.0f, 220);
                plasma = new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.plasma(0xD8FFF2, 0.68f,
                                seed ^ 67, Vec3.ZERO, 0.005f),
                        random -> coneSample(state, random, true), 75.0f, 100);
                launchDust = new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.dust(0x86C9A6, 0.85f,
                                seed ^ 71, Vec3.ZERO, 0.016f),
                        random -> {
                            MaterialEmitters.Sample base = MaterialEmitters.sphereVolume(
                                    new Vec3(state.ex, state.ey, state.ez),
                                    Math.max(0.20f, state.coneRadius * 0.35f)).sample(random);
                            return new MaterialEmitters.Sample(base.pos(), forward(state).scale(0.08));
                        }, 55.0f, 80);
                mist = new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.mist(0x9FFFE0, 1.50f,
                                seed ^ 79, Vec3.ZERO, 0.006f),
                        random -> MaterialEmitters.sphereVolume(
                                new Vec3(state.ex, state.ey, state.ez), 0.30f).sample(random),
                        35.0f, 55);
                center = null;
                groundDust = null;
            } else {
                vapor = new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.vapor(0x26FFA6, 1.20f,
                                seed ^ 83, Vec3.ZERO, 0.010f),
                        random -> radialFrontSample(state, random), 185.0f, 240);
                center = new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.vapor(0xCFFFEF, 1.05f,
                                seed ^ 89, Vec3.ZERO, 0.008f),
                        random -> centerSample(state, random), 65.0f, 90);
                groundDust = new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.dust(0x86C9A6, 0.90f,
                                seed ^ 97, Vec3.ZERO, 0.016f),
                        random -> groundDustSample(state, random), 85.0f, 120);
                plasma = null;
                launchDust = null;
                mist = null;
            }
        }

        private void updateDebris(ClientLevel level) {
            if (state.orientToImpact) {
                debris = null;
                debrisGround = null;
                return;
            }
            BlockPos origin = BlockPos.containing(state.ex, state.ey, state.ez);
            BlockPos groundPos = origin.below();
            BlockState ground = level.getBlockState(groundPos);
            double groundY = groundPos.getY() + 1.0;
            if (ground.isAir() || state.ey - groundY > 1.6) {
                debris = null;
                debrisGround = null;
                return;
            }
            if (debris != null && debrisGround == ground) return;
            debrisGround = ground;
            debris = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.fixed(
                            new BlockParticleOption(DbaParticles.BLOCK_CHUNKS, ground),
                            Vec3.ZERO, 0.05f),
                    random -> debrisSample(state, random, groundY),
                    24.0f, 36);
        }
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(BanshoShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(BanshoShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ShockwaveRenderState createRenderState() {
        return new ShockwaveRenderState();
    }

    @Override
    public void extractRenderState(BanshoShockwaveEntity entity, ShockwaveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yaw = entity.getEntityYaw();
        state.pitch = entity.getEntityPitch();
        state.maxRadius = entity.getMaxRadius();
        state.isCone = entity.isCone();
        state.orientToImpact = entity.isOrientToImpact();
        state.age = entity.tickCount + partialTicks;
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
        state.seed = entity.getId() * 0x9E3779B97F4A7C15L ^ (int) (entity.getMaxRadius() * 64.0f) ^ 0x510C4L;
    }

    @Override
    public void submit(ShockwaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float maxLife = 14.0f;
        float progress = Math.min(1.0f, state.age / maxLife);
        if (progress >= 1.0f) return;

        float exitT = progress <= 0.55f ? 0.0f : Math.min(1.0f, (progress - 0.55f) / 0.45f);
        float alpha = (1.0f - exitT * exitT * (3.0f - 2.0f * exitT)) * 0.95f;
        state.progress = progress;
        state.alpha = alpha;

        if (state.isCone) {
            float expansion = (float) Math.sin(progress * (Math.PI / 2.0));
            state.coneLength = state.maxRadius * (0.4f + 0.8f * expansion);
            state.coneRadius = state.maxRadius * (0.3f + 0.9f * expansion);
        } else {
            float expansionEase = (float) Math.sin(progress * (Math.PI / 2.0));
            state.currentRadius = Math.max(0.4f, expansionEase * state.maxRadius);
            state.crestHeight = 0.55f * (1.0f - progress * 0.8f);
            state.coreRadius = Math.max(0.1f, (state.maxRadius * 0.3f) * (1.0f - progress));
        }

        float distanceScale = MaterialEmitters.distanceScale(new Vec3(state.ex, state.ey, state.ez));
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && alpha > 0.01f) {
            if (state.emission == null) state.emission = new Emission(state);
            state.emission.tick(level, alpha * distanceScale);
        }

        // Pieces layer: crackFan web plus light fracturePlates sized to
        // the wave's current radius, at a 0.12 lift so they read as wind
        // scour lifting the surface, not a slam crater. Vertex count at
        // full distance scale: the crack fan stays under its 900 vertex
        // budget and the plates under about 480, for a total under 1400,
        // inside the class M budget of 2500. The push wall below draws
        // in its own lit submits: the cone wall is one submit of at
        // most 16 slabs (1152 vertices); each radial arc is its own
        // submit of at most 9 slabs (648), front row and backing row
        // separately, six submits around the ring.
        if (alpha > 0.01f && distanceScale > 0.01f) {
            final float scourRadius = state.isCone
                    ? Math.max(0.4f, state.coneRadius)
                    : state.currentRadius;
            final float plateRadius = scourRadius * (state.isCone ? 0.85f : 0.92f);
            final long geoSeed = state.seed;
            final float geoEnv = alpha * distanceScale;
            final float geoDs = distanceScale;
            // Push wall palette: sampled from the ground block under the
            // wave origin and blended toward soil, so the heaved wall
            // reads as the local earth (grass country reads browner than
            // the turf top, sand stays sandy) instead of a fixed tint.
            // Weathered stone is the fallback when there is no ground
            // under the origin to sample.
            int sampledRoot = 0x4A4238;
            int sampledTip = 0x8A7B68;
            if (level != null) {
                BlockPos groundPos = BlockPos.containing(state.ex, state.ey - 0.5, state.ez);
                BlockState ground = level.getBlockState(groundPos);
                if (!ground.isAir()) {
                    int col = ground.getMapColor(level, groundPos).col;
                    if (col != 0) {
                        int base = mixRgb(col, 0x6B5138, 0.45f);
                        sampledRoot = scaleRgb(base, 0.50f);
                        sampledTip = lightenRgb(base, 0.30f);
                    }
                }
            }
            final int wallRoot = sampledRoot;
            final int wallTip = sampledTip;
            poseStack.pushPose();
            if (state.isCone || state.orientToImpact) {
                poseStack.mulPose(new org.joml.Matrix4f()
                        .rotationY((float) Math.toRadians(-state.yaw))
                        .rotateX((float) Math.toRadians(state.pitch)));
            }
            ComplexGeometry.submitSolid(collector, poseStack, (pose, buffer) ->
                    ComplexGeometry.fracturePlates(pose, buffer, geoSeed ^ 0x5C09L,
                            plateRadius, 0.12f, 0x9FFFE0, 0x2F6B4F, 0.96f, geoEnv, geoDs));
            ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) ->
                    ComplexGeometry.crackFan(pose, buffer, geoSeed ^ 0xC4ACL,
                            scourRadius * 1.05f, 0xCFFFEF, 0x26FFA6, 0.95f, geoEnv, geoDs));
            if (state.isCone) {
                drawConeWall(state, poseStack, collector, geoSeed, geoEnv, geoDs, wallRoot, wallTip);
            } else if (state.currentRadius >= 3.0f) {
                drawRingWall(state, poseStack, collector, geoSeed, geoEnv, geoDs, wallRoot, wallTip);
            }
            poseStack.popPose();
        }
        super.submit(state, poseStack, collector, cameraState);
    }

    /**
     * The cone wave's push wall, in the rotated cone frame (local z is
     * the travel direction): a front row of 7 to 9 slabs across the
     * wavefront at 0.88 of the cone length and a lower backing ridge
     * row half a block behind it, the whole wall leaned 0.16 radians
     * into the travel direction. The wall rises over the first 3 ticks
     * and rides the cone's existing expansion; as the wave dies the
     * backing row sinks first (from progress 0.50) and the front row
     * follows (from 0.62), so the ridge collapses rear-first while the
     * front is still being shoved.
     */
    private void drawConeWall(ShockwaveRenderState state, PoseStack poseStack,
                              SubmitNodeCollector collector, long geoSeed,
                              float geoEnv, float geoDs, int wallRoot, int wallTip) {
        float progress = state.progress;
        float wallLen = Math.max(4.8f, Math.min(9.8f, state.coneRadius * 2.0f));
        float frontZ = state.coneLength * 0.88f;
        float rise = FxEnvelope.smoothstep(Math.min(1.0f, state.age / 3.0f));
        float sinkFront = progress <= 0.62f ? 0.0f
                : FxEnvelope.smoothstep(Math.min(1.0f, (progress - 0.62f) / 0.38f));
        float sinkBack = progress <= 0.50f ? 0.0f
                : FxEnvelope.smoothstep(Math.min(1.0f, (progress - 0.50f) / 0.50f));
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotateX(0.16f));
        ComplexGeometry.submitSolidLit(collector, poseStack, state.lightCoords, (pose, buffer) -> {
            ComplexGeometry.rockWall(pose, buffer, geoSeed ^ 0x5C19L,
                    new Vec3(0.0, 0.0, frontZ), new Vec3(0.0, 0.0, 1.0),
                    wallLen, 1.25f, -0.45f, rise, sinkFront,
                    wallRoot, wallTip, 0.97f, geoEnv, geoDs);
            ComplexGeometry.rockWall(pose, buffer, geoSeed ^ 0x5C23L,
                    new Vec3(0.0, 0.0, frontZ - 0.55f), new Vec3(0.0, 0.0, 1.0),
                    wallLen * 0.8f, 0.72f, -0.45f, rise, sinkBack,
                    wallRoot, wallTip, 0.97f, geoEnv, geoDs);
        });
        poseStack.popPose();
    }

    /**
     * The radial wave's push wall: three arcs of the front row around
     * the expanding ring, one centered on the wave's facing and the
     * others 120 degrees apart, each with its own height so the ring's
     * ridge line is uneven, and a lower backing arc behind each front
     * arc. The arc parameter is set from the current radius (arc equals
     * minus length over radius), so every arc hugs the ring's own
     * circle at every size and rides the existing expansion outward.
     * Wall length grows with the radius: the slab count reaches the
     * specced 7 to 9 once the front passes radius 5, and sits at the
     * builder's 6 slab floor before that, because a full-length wall
     * on a young ring would wrap past its neighbours. Rise, hold, and
     * the rear-first crumble match the cone wall's beats.
     */
    private void drawRingWall(ShockwaveRenderState state, PoseStack poseStack,
                              SubmitNodeCollector collector, long geoSeed,
                              float geoEnv, float geoDs, int wallRoot, int wallTip) {
        float progress = state.progress;
        float ringR = state.currentRadius;
        float wallLen = Math.max(4.8f, Math.min(9.8f, ringR * 1.5f));
        float wallArc = -(wallLen / ringR);
        float backR = ringR - 0.55f;
        float backLen = wallLen * 0.8f;
        float backArc = -(backLen / backR);
        float rise = FxEnvelope.smoothstep(Math.min(1.0f, Math.max(0.0f, state.age - 1.5f) / 3.0f));
        float sinkFront = progress <= 0.60f ? 0.0f
                : FxEnvelope.smoothstep(Math.min(1.0f, (progress - 0.60f) / 0.40f));
        float sinkBack = progress <= 0.48f ? 0.0f
                : FxEnvelope.smoothstep(Math.min(1.0f, (progress - 0.48f) / 0.52f));
        // In the orient-to-impact frame the pose is already rotated and
        // local z is the facing; in the plain frame the arcs phase off
        // the wave's world facing. Either way one arc centers forward.
        double baseAng = state.orientToImpact
                ? Math.PI / 2.0
                : Math.atan2(forward(state).z, forward(state).x);
        float[] heights = {1.30f, 1.48f, 1.22f};
        for (int k = 0; k < 3; k++) {
            double ang = baseAng + k * (Math.PI * 2.0 / 3.0);
            float rotY = (float) (Math.PI / 2.0 - ang);
            float h = heights[k];
            long arcSeed = geoSeed ^ (0x5C19L + k * 131L);
            poseStack.pushPose();
            poseStack.translate(Math.cos(ang) * ringR, 0.0, Math.sin(ang) * ringR);
            poseStack.mulPose(new org.joml.Matrix4f().rotationY(rotY).rotateX(0.16f));
            ComplexGeometry.submitSolidLit(collector, poseStack, state.lightCoords, (pose, buffer) ->
                    ComplexGeometry.rockWall(pose, buffer, arcSeed,
                            Vec3.ZERO, new Vec3(0.0, 0.0, 1.0),
                            wallLen, h, wallArc, rise, sinkFront,
                            wallRoot, wallTip, 0.97f, geoEnv, geoDs));
            poseStack.popPose();
            long backSeed = geoSeed ^ (0x5C23L + k * 131L);
            float backH = h * 0.55f;
            poseStack.pushPose();
            poseStack.translate(Math.cos(ang) * backR, 0.0, Math.sin(ang) * backR);
            poseStack.mulPose(new org.joml.Matrix4f().rotationY(rotY).rotateX(0.16f));
            ComplexGeometry.submitSolidLit(collector, poseStack, state.lightCoords, (pose, buffer) ->
                    ComplexGeometry.rockWall(pose, buffer, backSeed,
                            Vec3.ZERO, new Vec3(0.0, 0.0, 1.0),
                            backLen, backH, backArc, rise, sinkBack,
                            wallRoot, wallTip, 0.97f, geoEnv, geoDs));
            poseStack.popPose();
        }
    }

    /** Scales a 0xRRGGBB color's channels by a factor, clamped. */
    private static int scaleRgb(int rgb, float factor) {
        int r = Math.min(255, Math.round(((rgb >> 16) & 0xFF) * factor));
        int g = Math.min(255, Math.round(((rgb >> 8) & 0xFF) * factor));
        int b = Math.min(255, Math.round((rgb & 0xFF) * factor));
        return (r << 16) | (g << 8) | b;
    }

    /** Blends two 0xRRGGBB colors, t of the way from a to b. */
    private static int mixRgb(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) * (1.0f - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1.0f - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1.0f - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /** Moves a 0xRRGGBB color's channels toward white by t. */
    private static int lightenRgb(int rgb, float t) {
        int r = Math.round(((rgb >> 16) & 0xFF) + (255 - ((rgb >> 16) & 0xFF)) * t);
        int g = Math.round(((rgb >> 8) & 0xFF) + (255 - ((rgb >> 8) & 0xFF)) * t);
        int b = Math.round((rgb & 0xFF) + (255 - (rgb & 0xFF)) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static MaterialEmitters.Sample coneSample(ShockwaveRenderState state, RandomSource random, boolean core) {
        float axialT = random.nextFloat();
        float z = state.coneLength * (0.08f + 0.92f * axialT);
        float localRadius = state.coneRadius * (0.18f + 0.82f * axialT) * (core ? 0.36f : 1.0f);
        double angle = random.nextDouble() * Math.PI * 2.0;
        float radius = localRadius * (float) Math.sqrt(random.nextDouble());
        float[] local = new float[] {
                (float) Math.cos(angle) * radius,
                (float) Math.sin(angle) * radius,
                z
        };
        Vec3 pos = localPoint(state, local);
        Vec3 radial = localDirection(state, new Vec3(Math.cos(angle), Math.sin(angle), 0.0));
        Vec3 velocity = forward(state).scale(core ? 0.22 : 0.16)
                .add(radial.scale(0.045))
                .add(up(state).scale(0.005));
        return new MaterialEmitters.Sample(pos, velocity);
    }

    private static MaterialEmitters.Sample radialFrontSample(ShockwaveRenderState state, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        float radius = state.currentRadius * (0.86f + random.nextFloat() * 0.22f);
        float height = random.nextFloat() * (state.crestHeight + 0.18f);
        if (state.orientToImpact) {
            float[] local = new float[] {
                    (float) Math.cos(angle) * radius,
                    height,
                    (float) Math.sin(angle) * radius
            };
            Vec3 radial = localDirection(state, new Vec3(Math.cos(angle), 0.0, Math.sin(angle)));
            Vec3 velocity = radial.scale(0.15).add(up(state).scale(0.02));
            return new MaterialEmitters.Sample(localPoint(state, local), velocity);
        }
        Vec3 pos = new Vec3(
                state.ex + Math.cos(angle) * radius,
                state.ey + height,
                state.ez + Math.sin(angle) * radius);
        Vec3 velocity = new Vec3(Math.cos(angle), 0.0, Math.sin(angle)).scale(0.15)
                .add(0.0, 0.025, 0.0);
        return new MaterialEmitters.Sample(pos, velocity);
    }

    private static MaterialEmitters.Sample centerSample(ShockwaveRenderState state, RandomSource random) {
        Vec3 center = new Vec3(state.ex, state.ey, state.ez);
        MaterialEmitters.Sample base = MaterialEmitters.sphereVolume(center, state.coreRadius).sample(random);
        Vec3 offset = base.pos().subtract(center);
        Vec3 velocity = offset.lengthSqr() > 1.0E-6
                ? offset.normalize().scale(0.035)
                : Vec3.ZERO;
        Vec3 up = state.orientToImpact ? up(state) : new Vec3(0.0, 1.0, 0.0);
        return new MaterialEmitters.Sample(base.pos(), velocity.add(up.scale(0.02)));
    }

    private static MaterialEmitters.Sample groundDustSample(ShockwaveRenderState state, RandomSource random) {
        if (state.orientToImpact) {
            MaterialEmitters.Sample front = radialFrontSample(state, random);
            return new MaterialEmitters.Sample(front.pos(), front.velocity().scale(0.65));
        }
        double angle = random.nextDouble() * Math.PI * 2.0;
        float radius = state.currentRadius * (float) Math.sqrt(random.nextDouble());
        Vec3 pos = new Vec3(
                state.ex + Math.cos(angle) * radius,
                state.ey + 0.12,
                state.ez + Math.sin(angle) * radius);
        Vec3 velocity = new Vec3(Math.cos(angle), 0.0, Math.sin(angle)).scale(0.10)
                .add(0.0, 0.03, 0.0);
        return new MaterialEmitters.Sample(pos, velocity);
    }

    private static MaterialEmitters.Sample debrisSample(ShockwaveRenderState state, RandomSource random,
                                                        double groundY) {
        float debrisRadius = state.isCone
                ? Math.max(0.45f, state.coneRadius * 0.45f)
                : Math.max(0.45f, state.currentRadius * 0.92f);
        double angle = random.nextDouble() * Math.PI * 2.0;
        float radius = debrisRadius * (float) Math.sqrt(random.nextDouble());
        Vec3 pos = new Vec3(
                state.ex + Math.cos(angle) * radius,
                groundY + 0.10,
                state.ez + Math.sin(angle) * radius);
        Vec3 velocity = new Vec3(Math.cos(angle), 0.0, Math.sin(angle)).scale(0.10)
                .add(0.0, 0.07 + random.nextDouble() * 0.05, 0.0);
        return new MaterialEmitters.Sample(pos, velocity);
    }

    private static Vec3 right(ShockwaveRenderState state) {
        double yaw = Math.toRadians(state.yaw);
        return new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
    }

    private static Vec3 up(ShockwaveRenderState state) {
        double yaw = Math.toRadians(state.yaw);
        double pitch = Math.toRadians(state.pitch);
        return new Vec3(-Math.sin(pitch) * Math.sin(yaw), Math.cos(pitch),
                Math.sin(pitch) * Math.cos(yaw));
    }

    private static Vec3 forward(ShockwaveRenderState state) {
        double yaw = Math.toRadians(state.yaw);
        double pitch = Math.toRadians(state.pitch);
        return new Vec3(-Math.cos(pitch) * Math.sin(yaw), -Math.sin(pitch),
                Math.cos(pitch) * Math.cos(yaw));
    }

    private static Vec3 localPoint(ShockwaveRenderState state, float[] local) {
        return new Vec3(state.ex, state.ey, state.ez)
                .add(right(state).scale(local[0]))
                .add(up(state).scale(local[1]))
                .add(forward(state).scale(local[2]));
    }

    private static Vec3 localDirection(ShockwaveRenderState state, Vec3 local) {
        return right(state).scale(local.x)
                .add(up(state).scale(local.y))
                .add(forward(state).scale(local.z));
    }
}
