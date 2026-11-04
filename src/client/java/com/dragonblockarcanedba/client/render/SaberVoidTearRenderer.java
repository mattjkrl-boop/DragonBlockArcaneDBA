package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.SaberVoidTearEntity;
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
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Entity Renderer for Saber Void Tear in Minecraft 26.2.
 *
 * <p>Complex pass: seeded spatial shard meshes ring the tear in its
 * own plane, with two tendrils streaming inward through the shader
 * tendril canvas, plus WISP_CURL and ASH_FLAKE complex sprites. The
 * darkness vapor, void plasma, white core, and spatial glass dust
 * emission stays as the filler layer only. Pieces submit through
 * the normal depth tested collector path in the tear plane, so the
 * existing wall occlusion behavior is unchanged. Envelope timing,
 * the destination boost, colors, and alphas are unchanged in
 * effect.</p>
 *
 * <p>Attack upgrade pass: the shard set is composed as one large
 * shard crowning the tear mouth with two smaller shards flanking
 * it, and the mouth bursts double their ASH_FLAKE count and gain a
 * SHARD_CHIP burst at the same density.</p>
 */
public class SaberVoidTearRenderer extends EntityRenderer<SaberVoidTearEntity, SaberVoidTearRenderer.VoidTearRenderState> {

    private final Map<Long, Emission> emissions = new HashMap<>();

    public SaberVoidTearRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class VoidTearRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public boolean isDestination = false;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public int maxLifetime = 12;
        public double ex = 0;
        public double ey = 0;
        public double ez = 0;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(SaberVoidTearEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(SaberVoidTearEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public VoidTearRenderState createRenderState() {
        return new VoidTearRenderState();
    }

    @Override
    public void extractRenderState(SaberVoidTearEntity entity, VoidTearRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getTearScale();
        state.isDestination = entity.isDestination();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
        // Synced seed: entity id plus the synced scale and destination flag,
        // identical on every client, stable for the tear's life.
        state.seed = entity.getId() * 0x9E3779B97F4A7C15L
            ^ Float.floatToIntBits(entity.getTearScale()) * 271L
            ^ (entity.isDestination() ? 0xDE57L : 0L);
    }

    @Override
    public void submit(VoidTearRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
        long key = state.seed;
        if (progress >= 1.0f) {
            emissions.remove(key);
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        pruneEmissions();
        float env = FxEnvelope.envelope(state.age, (float) state.maxLifetime, 0.25f, 0.35f);
        float fade = (1.0f - FxEnvelope.smoothstep(progress)) * Math.min(1.0f, env * 1.6f);
        float open = FxEnvelope.smoothstep(Math.min(1.0f, progress / 0.45f));
        float impact = strike(progress);
        float ds = MaterialEmitters.distanceScale(new Vec3(state.ex, state.ey, state.ez));

        Emission emission = emissions.get(key);
        if (emission == null || emission.level != level || state.age < emission.lastAge - 0.01f) {
            emission = new Emission(level, state);
            emissions.put(key, emission);
        }
        emission.update(state, open);
        float delta = emission.delta(state.age);
        float strength = fade * ds;
        if (strength > 0.01f) {
            for (MaterialEmitters.Emitter emitter : emission.emitters) {
                emitter.tick(delta, strength);
            }
        }
        if (impact > 0.75f && !emission.flashDone) {
            emission.flashDone = true;
            Vec3 center = emission.origin;
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.glassDust(0xBFEFFF, 0.95f, (int) (state.seed ^ 0xBADD1L), Vec3.ZERO, 0.55f),
                MaterialEmitters.point(center), Math.max(4, Math.round(14 * ds)));
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.plasma(0x4DD8FF, 0.90f, (int) (state.seed ^ 0xBADD2L), Vec3.ZERO, 0.35f),
                MaterialEmitters.point(center), Math.max(3, Math.round(10 * ds)));
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.fixed(DbaParticles.WISP_CURL, Vec3.ZERO, 0.30f),
                MaterialEmitters.point(center), Math.max(3, Math.round(10 * ds)));
            // Attack upgrade: the mouth sprite layers double. The
            // ASH_FLAKE burst doubles its count, and a SHARD_CHIP
            // burst joins it at the same doubled density.
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP, Vec3.ZERO, 0.42f),
                MaterialEmitters.point(center), Math.max(4, Math.round(16 * ds)));
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.fixed(DbaParticles.ASH_FLAKE, Vec3.ZERO, 0.38f),
                MaterialEmitters.point(center), Math.max(4, Math.round(16 * ds)));
        }

        // Pieces layer in the tear plane, under the same synced
        // rotation the emission uses. Attack upgrade composition:
        // one large solid shard crowns the tear mouth and two
        // smaller shards flank it at seeded offsets (3 times 288
        // vertices), and two energy tendrils stream inward (near
        // 770 vertices), for a total near 1630, inside the class M
        // budget of 2500.
        float pieceEnv = open * fade;
        if (pieceEnv > 0.02f && ds > 0.05f) {
            float sc = emission.scale;
            float slitH = emission.slitH;
            long seed = state.seed;
            poseStack.pushPose();
            poseStack.mulPose(new Matrix4f()
                .rotationY((float) Math.toRadians(-state.yRot))
                .rotateX((float) Math.toRadians(state.xRot)));
            ComplexGeometry.submitSolid(collector, poseStack,
                (pose, buffer) -> {
                    ComplexGeometry.Rng rng = new ComplexGeometry.Rng(seed ^ 0x5A17L);
                    for (int i = 0; i < 3; i++) {
                        double ang = Math.PI * 0.5 + (i - 1) * 0.96 + rng.signed() * 0.22;
                        Vec3 radial = new Vec3(Math.cos(ang), Math.sin(ang), 0);
                        Vec3 o = radial.scale(slitH * 0.38);
                        float len = (i == 1 ? 1.02f : 0.55f) * sc * rng.range(0.9f, 1.15f);
                        ComplexGeometry.shard(pose, buffer, seed + i * 0x9E37L,
                            o, radial, len, len * 0.36f,
                            0x3D1A78, 0xBFEFFF, 0.95f, pieceEnv);
                    }
                });
            ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL,
                (pose, buffer) -> {
                    for (int i = 0; i < 2; i++) {
                        double ang = i * 2.4 + 0.7;
                        Vec3 radial = new Vec3(Math.cos(ang), Math.sin(ang), 0);
                        Vec3 tangent = new Vec3(-Math.sin(ang), Math.cos(ang), 0);
                        Vec3 from = radial.scale(slitH * 1.05);
                        Vec3 to = radial.scale(slitH * 0.10);
                        Vec3 control = radial.scale(slitH * 0.55).add(tangent.scale(slitH * 0.30));
                        ComplexGeometry.tendril(pose, buffer, seed ^ (0x7EA0L + i * 131L),
                            from, control, to,
                            0.20f * sc, 0x4DD8FF, 0xFFFFFF, 0.92f, pieceEnv, ds);
                    }
                });
            poseStack.popPose();
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    private void pruneEmissions() {
        long now = System.nanoTime();
        emissions.values().removeIf(e -> now - e.lastSeenNanos > 5_000_000_000L);
    }

    private static float strike(float progress) {
        if (progress >= 0.48f) return 0.0f;
        return (float) Math.sin((progress / 0.48f) * Math.PI);
    }

    /**
     * Per-effect emitter set, keyed by the synced seed on the renderer
     * instance because render states are recreated on every extraction.
     * Sources read the mutable fields below, refreshed each submit.
     */
    private static final class Emission {
        final ClientLevel level;
        final MaterialEmitters.Emitter[] emitters;
        float lastAge = -1.0f;
        long lastSeenNanos = System.nanoTime();
        boolean flashDone = false;
        Vec3 origin = Vec3.ZERO;
        Matrix4f rotation = new Matrix4f();
        float scale = 1.0f;
        float slitH = 1.0f;
        float slitW = 0.1f;
        float haloLen = 3.0f;
        float coreLen = 2.0f;

        Emission(ClientLevel level, VoidTearRenderState state) {
            this.level = level;
            update(state, 0.0f);
            int seed = (int) state.seed;
            float baseScale = state.scale;

            MaterialEmitters.Source darkness = random -> {
                Vec3 local = new Vec3(
                    (random.nextDouble() * 2.0 - 1.0) * 0.55 * scale,
                    (random.nextDouble() * 2.0 - 1.0) * haloLen * 0.42,
                    (random.nextDouble() * 2.0 - 1.0) * 0.55 * scale);
                Vec3 inward = toWorldDirection(new Vec3(-local.x * 0.22, -local.y * 0.03, -local.z * 0.22));
                return new MaterialEmitters.Sample(toWorld(local), inward);
            };
            MaterialEmitters.Source slit = random -> {
                double y = (random.nextDouble() * 2.0 - 1.0) * coreLen * 0.5;
                Vec3 local = new Vec3((random.nextDouble() * 2.0 - 1.0) * slitW * 2.4,
                    y, (random.nextDouble() - 0.5) * 0.08 * scale);
                Vec3 inward = toWorldDirection(new Vec3(-local.x * 0.45, -local.y * 0.02, 0));
                return new MaterialEmitters.Sample(toWorld(local), inward);
            };
            MaterialEmitters.Source core = random -> {
                double y = (random.nextDouble() * 2.0 - 1.0) * coreLen * 0.5;
                Vec3 local = new Vec3((random.nextDouble() * 2.0 - 1.0) * slitW * 1.1,
                    y, (random.nextDouble() - 0.5) * 0.05 * scale);
                Vec3 inward = toWorldDirection(new Vec3(-local.x * 0.45, -local.y * 0.02, 0));
                return new MaterialEmitters.Sample(toWorld(local), inward);
            };
            MaterialEmitters.Source glass = random -> {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double radius = scale * (0.20 + 0.95 * random.nextDouble());
                Vec3 local = new Vec3(Math.cos(angle) * radius, Math.sin(angle) * radius,
                    (random.nextDouble() - 0.5) * 0.20 * scale);
                Vec3 outward = toWorldDirection(new Vec3(Math.cos(angle) * 0.34, Math.sin(angle) * 0.34, 0));
                return new MaterialEmitters.Sample(toWorld(local), outward);
            };

            this.emitters = new MaterialEmitters.Emitter[] {
                new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.darkness(0x260052, 1.55f * baseScale, seed ^ 0x4A10, Vec3.ZERO, 0.04f),
                    darkness, 170.0f, 220),
                new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.plasma(0x4DD8FF, 1.00f * baseScale, seed ^ 0xC9AA, Vec3.ZERO, 0.04f),
                    slit, 240.0f, 300),
                new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.plasma(0xFFFFFF, 0.70f * baseScale, seed ^ 0x1117,
                        Vec3.ZERO, 0.03f),
                    core, 110.0f, 150),
                new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.glassDust(0xBFEFFF, 0.95f * baseScale, seed ^ 0x5907, Vec3.ZERO, 0.08f),
                    glass, 150.0f, 200),
                // Complex particle layer: wisp curls curling through
                // the darkness volume and ash flakes thrown outward
                // with the glass. Rates stay at or under the 40 per
                // second continuous budget.
                new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.fixed(DbaParticles.WISP_CURL, Vec3.ZERO, 0.05f),
                    darkness, 32.0f, 40),
                new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.fixed(DbaParticles.ASH_FLAKE, Vec3.ZERO, 0.10f),
                    glass, 28.0f, 40)
            };
        }

        void update(VoidTearRenderState state, float open) {
            this.origin = new Vec3(state.ex, state.ey, state.ez);
            this.rotation = new Matrix4f()
                .rotationY((float) Math.toRadians(-state.yRot))
                .rotateX((float) Math.toRadians(state.xRot));
            this.scale = state.scale * (0.55f + 0.7f * open);
            float slitBoost = state.isDestination ? 1.30f : 1.0f;
            float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
            this.slitH = 1.35f * (0.4f + 0.6f * open) * state.scale;
            this.slitW = 0.12f * (1.0f - progress * 0.55f) * state.scale * slitBoost;
            this.haloLen = this.slitH * 3.0f;
            this.coreLen = this.slitH * 2.0f;
        }

        float delta(float age) {
            this.lastSeenNanos = System.nanoTime();
            if (this.lastAge < 0.0f) {
                this.lastAge = age;
                return 1.0f / 60.0f;
            }
            float d = Math.min(0.05f, Math.max(0.0f, (age - this.lastAge) / 20.0f));
            this.lastAge = age;
            return d;
        }

        Vec3 toWorld(Vec3 local) {
            Vector3f v = this.rotation.transformPosition(new Vector3f(
                (float) local.x, (float) local.y, (float) local.z));
            return this.origin.add(v.x, v.y, v.z);
        }

        Vec3 toWorldDirection(Vec3 local) {
            Vector3f v = this.rotation.transformDirection(new Vector3f(
                (float) local.x, (float) local.y, (float) local.z));
            return new Vec3(v.x, v.y, v.z);
        }
    }
}
