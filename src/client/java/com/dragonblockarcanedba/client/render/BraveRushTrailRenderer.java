package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.BraveRushTrailEntity;
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
import net.minecraft.world.phys.Vec3;

/**
 * Entity Renderer for Brave Sword Piercing Heroic Dash Flight Trail in Minecraft 26.2.
 * Complex pass: a layered composition. The pieces layer is three
 * ComplexGeometry tendril strands laid along the real dash path, a white
 * core strand and two gold side strands that converge toward the tail,
 * on the dissolve shader. The complex particle layer is WISP_CURL
 * sprites shed along the path. The material filler stays as before:
 * valor plasma and vapor shed along the dash path, emitted through
 * MaterialEmitters in the Brave gold theme, with gold dust at the ground
 * contacts. Emission and geometry follow the same opening envelope,
 * strike window, first person owner scaling, and 30 tick life the old
 * trail used.
 */
public class BraveRushTrailRenderer extends EntityRenderer<BraveRushTrailEntity, BraveRushTrailRenderer.TrailRenderState> {

    public BraveRushTrailRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class TrailRenderState extends EntityRenderState {
        public float trailScale = 1.0f;
        public float trailLength = 10.0f;
        public float age = 0;
        public float yRot = 0;
        public float xRot = 0;
        public int maxLifetime = 30;
        public boolean isFirstPersonOwner = false;
        public long seed = 0;
        public MaterialEmitters.Emitter plasmaEmitter;
        public MaterialEmitters.Emitter coreEmitter;
        public MaterialEmitters.Emitter vaporEmitter;
        public MaterialEmitters.Emitter dustEmitter;
        public MaterialEmitters.Emitter wispEmitter;
        public long emitterSeed = Long.MIN_VALUE;
        public ClientLevel emitterLevel;
        public float lastAge = -1f;
        public boolean burstFired = false;
    }

    private record Basis(Vec3 forward, Vec3 right, Vec3 up) {}

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(BraveRushTrailEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(BraveRushTrailEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public TrailRenderState createRenderState() {
        return new TrailRenderState();
    }

    @Override
    public void extractRenderState(BraveRushTrailEntity entity, TrailRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.trailScale = entity.getTrailScale();
        state.trailLength = entity.getTrailLength();
        state.age = entity.tickCount + partialTicks;
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.maxLifetime = entity.getMaxLifetime();
        state.seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null &&
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) &&
            mc.options.getCameraType().isFirstPerson());
    }

    @Override
    public void submit(TrailRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float maxLife = (float) state.maxLifetime;
        float progress = Math.min(1.0f, state.age / maxLife);
        if (progress >= 1.0f) return;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        float alpha = FxEnvelope.envelope(state.age, maxLife, 0.25f, 0.4f) * 0.90f;
        if (alpha > 0.01f) {
            float dist = MaterialEmitters.distanceScale(new Vec3(state.x, state.y, state.z));
            ensureEmitters(state, level);
            float delta = frameDelta(state);
            float e = alpha * dist * (state.isFirstPersonOwner ? 0.6f : 1.0f);
            state.plasmaEmitter.tick(delta, e);
            state.coreEmitter.tick(delta, e * 0.80f);
            state.vaporEmitter.tick(delta, e * 0.70f);
            state.dustEmitter.tick(delta, e * 0.75f);
            state.wispEmitter.tick(delta, e * 0.85f);

            // Pieces layer: three tendril strands along the real rush path,
            // in the local frame. The tip and length math matches the path
            // source the emission samples, including the first person owner
            // scale and the flipped tip. Side strands start at the cone
            // width and converge toward the tail like the shed volume does.
            final Basis gb = basis(state);
            final float gscale = state.trailScale * (state.isFirstPersonOwner ? 0.45f : 1.0f);
            final float open = FxEnvelope.smoothstep(Math.min(1.0f, progress / 0.25f));
            final float glen = state.trailLength * (0.35f + 0.65f * open);
            final float tipZ = state.isFirstPersonOwner ? (-0.6f * gscale) : (0.6f * gscale);
            final Vec3 tip = gb.forward().scale(tipZ);
            final Vec3 tail = gb.forward().scale(tipZ - glen);
            final Vec3 mid = tip.add(tail).scale(0.5);
            final float sideOff = 0.34f * gscale;
            final float genv = FxEnvelope.envelope(state.age, maxLife, 0.25f, 0.4f);
            final float geoDist = FxEnvelope.distScale(Math.sqrt(Math.max(0.0, state.distanceToCameraSq)));
            final long gseed = state.seed;
            final float coreWidth = 0.24f * gscale;
            final float sideWidth = 0.16f * gscale;
            ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
                ComplexGeometry.tendril(pose, buffer, gseed, tip,
                    mid.add(gb.up().scale(0.10 * gscale)), tail, coreWidth,
                    0xFFFFFF, 0xFFD700, 0.92f, genv, geoDist);
                ComplexGeometry.tendril(pose, buffer, gseed ^ 0x2B992DDFL,
                    tip.add(gb.right().scale(sideOff)),
                    mid.add(gb.right().scale(sideOff * 0.7)).add(gb.up().scale(-0.06 * gscale)),
                    tail.add(gb.right().scale(sideOff * 0.35)), sideWidth,
                    0xFFD700, 0x00F2FF, 0.92f, genv, geoDist);
                ComplexGeometry.tendril(pose, buffer, gseed ^ 0x51ED2701L,
                    tip.add(gb.right().scale(-sideOff)),
                    mid.add(gb.right().scale(-sideOff * 0.7)).add(gb.up().scale(0.06 * gscale)),
                    tail.add(gb.right().scale(-sideOff * 0.35)), sideWidth,
                    0xFFD700, 0xFFF2C0, 0.92f, genv, geoDist);
            });

            // Attack upgrade: the rush sheds steel as it runs. Two
            // small shards drop off the path every 6 ticks, five shed
            // sets over the trail's 30 tick life, each set appearing at
            // its station along the path as its shed tick passes and
            // holding with the trail envelope, so the dash leaves steel
            // behind it and not only light. Drawn lit as physical
            // metal.
            final float shedAge = state.age;
            final int shedLight = state.lightCoords;
            ComplexGeometry.submitSolidLit(collector, poseStack, shedLight, (pose, buffer) -> {
                for (int s = 0; s < 5; s++) {
                    float shedTick = s * 6.0f;
                    if (shedAge < shedTick) break;
                    float setEnv = genv * FxEnvelope.smoothstep(Math.min(1.0f, (shedAge - shedTick) / 2.0f));
                    if (setEnv <= 0.01f) continue;
                    // Seeded per set placement jitter and size, so the
                    // shed steel lands as scattered debris along the
                    // path, never as beads on a string.
                    ComplexGeometry.Rng shedRng = new ComplexGeometry.Rng(gseed ^ (0x77E5L + s * 0x101L));
                    Vec3 shedAt = gb.forward().scale(tipZ - glen * ((s + 1.0f) / 6.0f)
                            + (shedRng.nextFloat() - 0.5f) * 0.5f * gscale)
                        .add(gb.right().scale((shedRng.nextFloat() - 0.5f) * 0.30f * gscale))
                        .add(gb.up().scale((shedRng.nextFloat() - 0.5f) * 0.30f * gscale));
                    float shedUnit = (0.26f + shedRng.nextFloat() * 0.09f) * gscale;
                    ComplexGeometry.shardBurst(pose, buffer, gseed ^ (0x5EEDL + s * 0x101L), shedAt,
                        gb.forward().scale(-1.0), 50.0f, shedUnit, 2,
                        0xFFE8F1F8, 0xFFFFD700, 0.95f, setEnv, geoDist);
                }
            });
        }

        // Strike wake: a one-shot burst at the trail tip during the same
        // early strike window the old wake ring expanded in.
        float impact = strike(progress);
        if (impact > 0.02f && !state.burstFired) {
            state.burstFired = true;
            int seed = (int) (state.seed ^ (state.seed >>> 32));
            Basis b = basis(state);
            float scale = state.trailScale * (state.isFirstPersonOwner ? 0.45f : 1.0f);
            float tipZ = state.isFirstPersonOwner ? (-0.6f * scale) : (0.6f * scale);
            Vec3 tip = new Vec3(state.x, state.y, state.z).add(b.forward().scale(tipZ));
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.plasma(0xFFFFFF, 1.9f, seed ^ 0x9E3779B9, Vec3.ZERO, 0.20f),
                MaterialEmitters.point(tip), Math.round(10 * impact));
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.vapor(0xFFD700, 2.4f, seed ^ 0x27D4EB2F, Vec3.ZERO, 0.24f),
                MaterialEmitters.point(tip), Math.round(8 * impact));
        } else if (impact <= 0.02f) {
            state.burstFired = false;
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    private static void ensureEmitters(TrailRenderState state, ClientLevel level) {
        if (state.plasmaEmitter != null && state.emitterSeed == state.seed && state.emitterLevel == level) {
            return;
        }
        int seed = (int) (state.seed ^ (state.seed >>> 32));

        // Plasma and vapor shed along the dash path itself, widest at the
        // tip and narrowing down the trail, exactly where the old cone
        // volume sat. The pieces layer strands follow the same path data.
        MaterialEmitters.Source path = random -> {
            Basis b = basis(state);
            float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
            float open = FxEnvelope.smoothstep(Math.min(1.0f, progress / 0.25f));
            float scale = state.trailScale * (state.isFirstPersonOwner ? 0.45f : 1.0f);
            float len = state.trailLength * (0.35f + 0.65f * open);
            float tipZ = state.isFirstPersonOwner ? (-0.6f * scale) : (0.6f * scale);
            float coneBase = (state.isFirstPersonOwner ? 1.05f : 0.90f) * scale;
            double t = random.nextDouble();
            double width = coneBase * (1.15 - 0.65 * t);
            Vec3 center = new Vec3(state.x, state.y, state.z);
            Vec3 pos = center.add(b.forward().scale(tipZ - len * t))
                .add(b.right().scale((random.nextDouble() - 0.5) * width))
                .add(b.up().scale((random.nextDouble() - 0.5) * width));
            return new MaterialEmitters.Sample(pos, b.forward().scale(-0.01));
        };
        // Gold dust kicked up along the ground under the dash.
        MaterialEmitters.Source ground = random -> {
            Basis b = basis(state);
            float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
            float open = FxEnvelope.smoothstep(Math.min(1.0f, progress / 0.25f));
            float scale = state.trailScale * (state.isFirstPersonOwner ? 0.45f : 1.0f);
            float len = state.trailLength * (0.35f + 0.65f * open);
            float coneBase = (state.isFirstPersonOwner ? 1.05f : 0.90f) * scale;
            Vec3 mid = new Vec3(state.x, state.y, state.z).add(b.forward().scale(-len * 0.5));
            return MaterialEmitters.groundArea(mid, coneBase * 1.1, state.y - 0.70).sample(random);
        };

        state.plasmaEmitter = new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0xFFD700, 2.6f, seed, Vec3.ZERO, 0.04f),
            path, 175f, 300);
        state.coreEmitter = new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0xFFFFFF, 1.7f, seed ^ 0x2B992DDF, Vec3.ZERO, 0.03f),
            path, 85f, 150);
        state.vaporEmitter = new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.vapor(0x00F2FF, 2.4f, seed ^ 0x51ED2701, Vec3.ZERO, 0.05f),
            path, 60f, 110);
        state.dustEmitter = new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.dust(0xFFD700, 1.9f, seed ^ 0x85EBCA6B, new Vec3(0, 0.01, 0), 0.015f),
            ground, 30f, 60);
        // Complex particle layer: wisp curls shed along the dash path with
        // the plasma, at the standard continuous rate cap.
        state.wispEmitter = new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.fixed(DbaParticles.WISP_CURL, Vec3.ZERO, 0.04f),
            path, 28f, 40);
        state.emitterSeed = state.seed;
        state.emitterLevel = level;
    }

    private static Basis basis(TrailRenderState state) {
        Vec3 forward = Vec3.directionFromRotation(state.xRot, state.yRot);
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(forward).normalize();
        return new Basis(forward, right, up);
    }

    private static float frameDelta(TrailRenderState state) {
        float prev = state.lastAge;
        state.lastAge = state.age;
        if (prev < 0f) return 1f / 20f;
        float d = state.age - prev;
        if (d <= 0f || d > 3f) return 1f / 20f;
        return d / 20f;
    }

    private static float strike(float progress) {
        if (progress >= 0.34f) return 0.0f;
        return (float) Math.sin((progress / 0.34f) * Math.PI);
    }
}
