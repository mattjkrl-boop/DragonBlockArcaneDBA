package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.DimensionalWarpRiftEntity;
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
 * Entity Renderer for Dimensional Warp Rift in Minecraft 26.2.
 * Complex pass: a layered composition. The pieces layer carries three
 * builders: a solid shardBurst fan of spatial glass shards at the tear,
 * two shader tendrils streaming inward from the rim to the heart, and a
 * shader crackFan on the rift's ground plane at the same radius as the
 * marked disk. The complex particle layer is WISP_CURL sprites pulled
 * inward with the stream. The material emission stays as filler only,
 * following the standard's rift method:
 * - Void plasma and darkness streaming inward from the rim toward the
 *   tear, matching the pull the entity's VortexPhysics applies
 * - A dense white and magenta plasma cluster at the tear's heart where
 *   the singularity pearl used to sit
 * - Spatial glass dust and plasma flecks across the ground the entity
 *   marks, over the real permanent crying obsidian conversion disk
 * - Darkness vapor drifting at the boundary, suggesting the torn volume
 * Screen-space distortion stays where it already honestly lives: the
 * entity's own DbaImpactFx burst call at the destination. The renderer
 * adds no fake local refraction. Lifecycle shaping (entrance ease,
 * expansion, pulse, tear open and shut) is the same math the geometry
 * version used.
 */
public class DimensionalWarpRiftRenderer extends EntityRenderer<DimensionalWarpRiftEntity, DimensionalWarpRiftRenderer.WarpRiftRenderState> {

    public DimensionalWarpRiftRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class WarpRiftRenderState extends EntityRenderState {
        public float radius = 3.5f;
        public boolean isDestination = false;
        public float age = 0;
        public int maxLifetime = 24;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(DimensionalWarpRiftEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(DimensionalWarpRiftEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public WarpRiftRenderState createRenderState() {
        return new WarpRiftRenderState();
    }

    @Override
    public void extractRenderState(DimensionalWarpRiftEntity entity, WarpRiftRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRiftRadius();
        state.isDestination = entity.isDestination();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.seed = entity.getSeed();
    }

    /**
     * Emission points on a cylinder around the tear, each carrying a
     * velocity toward the tear's heart, so the material visibly streams
     * inward and feeds the rift instead of orbiting it as a band.
     */
    private static MaterialEmitters.Source inwardStream(Vec3 center, double radius, double height, double speed) {
        return random -> {
            double ang = random.nextDouble() * Math.PI * 2.0;
            double rr = radius * (0.45 + 0.55 * random.nextDouble());
            Vec3 pos = new Vec3(center.x + Math.cos(ang) * rr,
                center.y + random.nextDouble() * height,
                center.z + Math.sin(ang) * rr);
            Vec3 target = new Vec3(center.x, center.y + height * 0.45, center.z);
            Vec3 toHeart = target.subtract(pos);
            double len = Math.max(1.0e-4, toHeart.length());
            return new MaterialEmitters.Sample(pos, toHeart.scale(speed / len));
        };
    }

    @Override
    public void submit(WarpRiftRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float life = (float) state.maxLifetime;
        float progress = Math.min(1.0f, state.age / life);
        if (progress >= 1.0f) return;

        // Shared lifecycle envelope: build over the first 20 percent, full
        // presence through the burst, dissipate over the last 35 percent.
        // Same envelope and tear shaping as the geometry version.
        float env = FxEnvelope.envelope(state.age, life, 0.20f, 0.35f);
        if (env <= 0.001f) return;
        float distScale = FxEnvelope.distScale(Math.sqrt(Math.max(0.0, state.distanceToCameraSq)));

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        float entrance = ease(Math.min(1.0f, state.age / 6.0f));
        float expansion = state.isDestination ? (0.6f + progress * 0.7f) : (1.0f - progress * 0.4f);
        float radius = state.radius * entrance * expansion;
        float pulse = 0.88f + 0.12f * (float) Math.sin(state.age * 0.5f);
        float shut = ease(Math.max(0.0f, (progress - 0.55f) / 0.45f));
        float openTear = 1.0f - 0.88f * shut;
        float tearH = (2.8f + pulse * 0.35f) * entrance * openTear * (state.isDestination ? (0.55f + 0.55f * ease(progress)) : (1.0f - 0.45f * ease(progress)));
        float tearW = 0.42f * entrance * openTear * (state.isDestination ? (0.35f + 0.75f * ease(progress)) : (1.0f - 0.7f * ease(progress)));

        Vec3 base = new Vec3(state.x, state.y, state.z);
        Vec3 heart = base.add(0.0, 1.4, 0.0);
        int seed = (int) state.seed;
        float envelope = env * distScale;
        float tearEnv = envelope * openTear;
        float radiusFactor = Math.max(0.35f, radius / 3.5f);
        float delta = 1.0f / 60.0f;

        // The inward stream: void plasma and darkness pulled from the rim
        // toward the heart, the visual half of the entity's vortex pull.
        new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0x990DE6, 1.45f, seed, Vec3.ZERO, 0.03f),
            inwardStream(base, radius, 3.2, 0.55),
            280f * radiusFactor, 420).tick(delta, envelope);
        new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.darkness(0x05000D, 1.60f, seed ^ 0x51AB, Vec3.ZERO, 0.03f),
            inwardStream(base, radius * 0.95, 3.0, 0.38),
            190f * radiusFactor, 300).tick(delta, envelope);

        // The tear seam itself: white plasma sampled inside the torn
        // volume, narrowing and dimming as the tear shuts.
        new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0xFFFFFF, 0.85f, seed ^ 0x7EA1, Vec3.ZERO, 0.02f),
            MaterialEmitters.volume(heart, new Vec3(Math.max(0.06, tearW * 2.2), Math.max(0.10, tearH * 0.85), Math.max(0.06, tearW * 2.2))),
            120f, 150).tick(delta, tearEnv);
        new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0xFF26D9, 1.20f, seed ^ 0x7EA2, Vec3.ZERO, 0.03f),
            MaterialEmitters.volume(heart, new Vec3(Math.max(0.10, tearW * 3.2), Math.max(0.12, tearH), Math.max(0.10, tearW * 3.2))),
            150f, 180).tick(delta, tearEnv);

        // The singularity heart: a dense plasma cluster, pulsing on the
        // same 0.88 plus 0.12 sin(age x 0.5) curve the pearl used.
        new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0xFFFFFF, 0.75f, seed ^ 0xC08E, Vec3.ZERO, 0.02f),
            MaterialEmitters.sphereVolume(heart, 0.30f * pulse),
            140f, 160).tick(delta, envelope);
        new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0xFF26D9, 1.00f, seed ^ 0x5166, Vec3.ZERO, 0.02f),
            MaterialEmitters.sphereVolume(heart, 0.46f * pulse),
            120f, 140).tick(delta, envelope);

        // Boundary vapor: sparse large darkness vapor drifting through the
        // torn volume, suggesting the film without drawing a shell.
        new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.vapor(0x3D0A66, 2.40f, seed ^ 0x81A6, new Vec3(0.0, 0.02, 0.0), 0.02f),
            MaterialEmitters.sphereVolume(base.add(0.0, 1.5, 0.0), radius * 0.95),
            90f * radiusFactor, 140).tick(delta, envelope);

        // Complex particle layer: wisp curls pulled inward with the
        // stream, inside the 40 per second continuous budget.
        new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.fixed(DbaParticles.WISP_CURL, Vec3.ZERO, 0.03f),
            inwardStream(base, radius * 0.95, 3.0, 0.30),
            30f, 40).tick(delta, envelope);

        // Ground cracks: spatial glass dust and violet plasma flecks across
        // the marked disk, at the same radius the fissure work covered.
        // The crack mark itself stays the entity's real permanent disk.
        new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.glassDust(0xD9B3FF, 1.05f, seed ^ 0x08B1, new Vec3(0.0, 0.03, 0.0), 0.02f),
            MaterialEmitters.groundArea(base, radius * 1.02, state.y + 0.05),
            130f * radiusFactor, 200).tick(delta, envelope);
        new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0x8C1AFF, 0.90f, seed ^ 0x6910, new Vec3(0.0, 0.02, 0.0), 0.02f),
            MaterialEmitters.groundArea(base, radius * 0.80, state.y + 0.05),
            90f * radiusFactor, 140).tick(delta, envelope);

        // Pieces layer, class M with three builders, drawn in entity
        // local space (the heart sits at local y 1.4, as in the emission).
        // Vertex accounting at full detail: the shard fan is 4 shards at
        // 288 vertices (1152), the two inward tendrils are at most 384
        // each (768), and the crack fan runs at reduced detail scale as
        // the third builder (at most about 450), for a worst case near
        // 2370, inside the class M budget of 2500. The shard fan follows
        // the tear open and shut shaping; the tendrils and cracks follow
        // the shared lifecycle envelope.
        float geoEnv = env;
        float tearGeoEnv = env * openTear;
        float crackRadius = radius * 1.02f;
        float crackDetail = distScale * 0.70f;
        if (tearGeoEnv > 0.02f) {
            ComplexGeometry.submitSolid(collector, poseStack, (pose, buffer) ->
                ComplexGeometry.shardBurst(pose, buffer, seed ^ 0x5A17L,
                    new Vec3(0.0, 1.4, 0.0), new Vec3(0.0, 1.0, 0.0), 68f, 0.70f, 4,
                    0x1A0533, 0xE8CCFF, 0.95f, tearGeoEnv, distScale));
        }
        if (geoEnv > 0.02f) {
            ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
                Vec3 heartLocal = new Vec3(0.0, 1.4, 0.0);
                for (int i = 0; i < 2; i++) {
                    double ang = 0.6 + i * 2.35 + (seed & 7) * 0.13;
                    double cosA = Math.cos(ang), sinA = Math.sin(ang);
                    Vec3 rim = new Vec3(cosA * radius * 0.85, 0.35 + i * 1.05, sinA * radius * 0.85);
                    Vec3 tangent = new Vec3(-sinA, 0.0, cosA);
                    Vec3 control = rim.lerp(heartLocal, 0.5)
                        .add(tangent.scale(radius * 0.22)).add(new Vec3(0.0, 0.35, 0.0));
                    ComplexGeometry.tendril(pose, buffer, seed ^ (0x7EA5L + i * 131L),
                        rim, control, heartLocal,
                        0.20f, 0x990DE6, 0xFF26D9, 0.92f, geoEnv, distScale);
                }
            });
            ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) ->
                ComplexGeometry.crackFan(pose, buffer, seed ^ 0xC4ACL,
                    crackRadius, 0xFFFFFF, 0x8C1AFF, 0.90f, geoEnv, crackDetail));
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    private static float ease(float t) {
        float c = Math.max(0.0f, Math.min(1.0f, t));
        return c * c * (3.0f - 2.0f * c);
    }
}
