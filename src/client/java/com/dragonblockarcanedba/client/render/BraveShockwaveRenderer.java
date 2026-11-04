package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.entity.BraveShockwaveEntity;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Entity Renderer for Brave Sword Heroic Impact Shockwave in Minecraft 26.2.
 * Complex pass: a layered composition at class L. The pieces layer is
 * two ComplexGeometry builders: fracturePlates as solid fractured ground
 * filling the disc out to the expanding front, and a crackFan of energy
 * cracks on the dissolve shader running just inside the front. These are
 * the fast near-field fragments and energy structure; the big physical
 * chunks are the debris field the entity already spawns server side.
 * The complex particle layer is SHARD_CHIP sprites burst at the
 * epicenter and carried on the front. The material filler stays as
 * before: compressed ground dust, valor plasma, and soul light carried
 * on the expanding front, emitted through MaterialEmitters in the Brave
 * gold theme, with a plasma and dust burst at the epicenter. Emission
 * and geometry follow the same sine expansion ease, envelope, and 44
 * tick life the entity resolves its damage with.
 */
public class BraveShockwaveRenderer extends EntityRenderer<BraveShockwaveEntity, BraveShockwaveRenderer.ShockwaveRenderState> {

    public BraveShockwaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ShockwaveRenderState extends EntityRenderState {
        public float maxRadius = 7.5f;
        public float age = 0;
        public int maxLifetime = 44;
        public long seed = 0;
        public MaterialEmitters.Emitter dustEmitter;
        public MaterialEmitters.Emitter plasmaEmitter;
        public MaterialEmitters.Emitter soulEmitter;
        public MaterialEmitters.Emitter coreEmitter;
        public MaterialEmitters.Emitter chipEmitter;
        public long emitterSeed = Long.MIN_VALUE;
        public ClientLevel emitterLevel;
        public float lastAge = -1f;
        public boolean burstFired = false;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(BraveShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(BraveShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ShockwaveRenderState createRenderState() {
        return new ShockwaveRenderState();
    }

    @Override
    public void extractRenderState(BraveShockwaveEntity entity, ShockwaveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.maxRadius = entity.getMaxRadius();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();
    }

    @Override
    public void submit(ShockwaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float maxLife = (float) state.maxLifetime;
        float progress = Math.min(1.0f, state.age / maxLife);
        if (progress >= 1.0f) return;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        // Opening burst at the epicenter, where the old starburst core sat.
        if (state.age <= 2.0f && !state.burstFired) {
            state.burstFired = true;
            int seed = (int) (state.seed ^ (state.seed >>> 32));
            Vec3 center = new Vec3(state.x, state.y + 0.25, state.z);
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.plasma(0xFFD700, 2.6f, seed ^ 0x165667B1, Vec3.ZERO, 0.30f),
                MaterialEmitters.point(center), 20);
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.dust(0xFFD700, 2.2f, seed ^ 0x1B56C4E9, new Vec3(0, 0.03, 0), 0.22f),
                MaterialEmitters.groundArea(new Vec3(state.x, state.y, state.z), state.maxRadius * 0.25, state.y + 0.06), 14);
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP, new Vec3(0, 0.05, 0), 0.20f),
                MaterialEmitters.groundArea(new Vec3(state.x, state.y, state.z), state.maxRadius * 0.22, state.y + 0.08), 18);
        }

        float env = FxEnvelope.envelope(state.age, maxLife, 0.15f, 0.35f);
        if (env > 0.01f) {
            float dist = MaterialEmitters.distanceScale(new Vec3(state.x, state.y, state.z));
            ensureEmitters(state, level);
            float delta = frameDelta(state);
            float e = env * dist;
            state.dustEmitter.tick(delta, e);
            state.plasmaEmitter.tick(delta, e);
            state.soulEmitter.tick(delta, e * 0.80f);
            state.coreEmitter.tick(delta, e * 0.85f);
            state.chipEmitter.tick(delta, e);

            // Pieces layer, in the local ground frame: solid fracture
            // plates filling the broken disc and an energy crack fan just
            // inside the front. Both radii follow the entity sine ease,
            // the same radius the emission front samples.
            final float currentRadius = Math.max(0.05f,
                (float) Math.sin(progress * (Math.PI / 2.0)) * state.maxRadius);
            if (currentRadius >= 0.35f) {
                final float geoDist = FxEnvelope.distScale(Math.sqrt(Math.max(0.0, state.distanceToCameraSq)));
                final long gseed = state.seed;
                final float genv = env;
                final float crackRadius = currentRadius * 0.98f;
                ComplexGeometry.submitSolid(collector, poseStack, (pose, buffer) ->
                    ComplexGeometry.fracturePlates(pose, buffer, gseed, currentRadius, 0.34f,
                        0xFFD700, 0x7A5E14, 0.95f, genv, geoDist));
                ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) ->
                    ComplexGeometry.crackFan(pose, buffer, gseed ^ 0x51ED2701L, crackRadius,
                        0xFFFFFF, 0xFFD700, 0.95f, genv, geoDist));
            }
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    private static void ensureEmitters(ShockwaveRenderState state, ClientLevel level) {
        if (state.dustEmitter != null && state.emitterSeed == state.seed && state.emitterLevel == level) {
            return;
        }
        int seed = (int) (state.seed ^ (state.seed >>> 32));

        // Material carried on the expanding front itself: dust low and
        // wide, plasma at crest height, soul light just behind the front.
        MaterialEmitters.Source dustFront = random -> frontSample(state, random, 1.0, 0.38, 0.035);
        MaterialEmitters.Source plasmaFront = random -> frontSample(state, random, 1.0, 0.55, 0.020);
        MaterialEmitters.Source soulBack = random -> frontSample(state, random, 0.88, 0.30, 0.015);
        MaterialEmitters.Source core = random -> {
            Vec3 center = new Vec3(state.x, state.y + 0.3, state.z);
            return MaterialEmitters.sphereVolume(center, Math.max(0.2, state.maxRadius * 0.18)).sample(random);
        };

        state.dustEmitter = new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.dust(0xFFD700, 2.2f, seed, Vec3.ZERO, 0.02f),
            dustFront, 155f, 300);
        state.plasmaEmitter = new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0xFFD700, 2.4f, seed ^ 0x2B992DDF, Vec3.ZERO, 0.02f),
            plasmaFront, 95f, 180);
        state.soulEmitter = new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.soul(0x00F2FF, 2.2f, seed ^ 0x51ED2701, Vec3.ZERO, 0.018f),
            soulBack, 52f, 110);
        state.coreEmitter = new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0xFFFFFF, 2.0f, seed ^ 0x27D4EB2F, new Vec3(0, 0.01, 0), 0.03f),
            core, 60f, 120);
        // Complex particle layer: shard chips riding the dust front, at
        // the standard continuous rate cap.
        state.chipEmitter = new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP, new Vec3(0, 0.02, 0), 0.05f),
            dustFront, 34f, 48);
        state.emitterSeed = state.seed;
        state.emitterLevel = level;
    }

    /** One sample on the expanding front, at the entity's own sine ease radius. */
    private static MaterialEmitters.Sample frontSample(ShockwaveRenderState state, RandomSource random,
                                                       double radiusMul, double height, double outwardSpeed) {
        float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
        double radius = Math.max(0.05f, (float) Math.sin(progress * (Math.PI / 2.0)) * state.maxRadius) * radiusMul;
        double a = random.nextDouble() * Math.PI * 2.0;
        Vec3 outward = new Vec3(Math.cos(a), 0, Math.sin(a));
        Vec3 pos = new Vec3(state.x + outward.x * radius,
            state.y + 0.06 + random.nextDouble() * height,
            state.z + outward.z * radius);
        Vec3 vel = outward.scale(outwardSpeed).add(0, 0.008, 0);
        return new MaterialEmitters.Sample(pos, vel);
    }

    private static float frameDelta(ShockwaveRenderState state) {
        float prev = state.lastAge;
        state.lastAge = state.age;
        if (prev < 0f) return 1f / 20f;
        float d = state.age - prev;
        if (d <= 0f || d > 3f) return 1f / 20f;
        return d / 20f;
    }
}
