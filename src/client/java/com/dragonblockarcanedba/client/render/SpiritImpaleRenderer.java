package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.SpiritImpaleEntity;
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
 * Entity renderer for Spirit Impale.
 *
 * Complex pass: the impale is a layered composition. The pieces layer is
 * a ComplexGeometry shard mesh as the central spirit spike body (soul
 * gradient, submitSolid) with four smaller seeded ring shards around it,
 * plus a two strand tendril wake curling up the pillar through
 * submitEnergy with the tendril canvas. The complex particle layer is
 * WISP_CURL sprites rising through the pillar volume. The no-shapes
 * material emission stays as filler only: soul material fills the impact
 * volume and rises through the old pillar space, plasma concentrates
 * where the plunging impacts were distributed, and vapor softens the
 * perimeter. The server-owned ground burst, debris field, and permanent
 * crystal conversion remain untouched. Envelope, slam timing, pulse,
 * scale, colors, and the UUID seed are unchanged, and all pieces draw
 * through the normal depth tested path so wall occlusion is unchanged.
 */
public class SpiritImpaleRenderer extends EntityRenderer<SpiritImpaleEntity, SpiritImpaleRenderer.SpiritImpaleRenderState> {

    private static final int WHITE = 0xFFFFFF;
    private static final int CYAN = 0x00E5FF;
    private static final int GOLD = 0xFFD23C;

    public SpiritImpaleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SpiritImpaleRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public float age = 0;
        public int maxLifetime = 40;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(SpiritImpaleEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(SpiritImpaleEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public SpiritImpaleRenderState createRenderState() {
        return new SpiritImpaleRenderState();
    }

    @Override
    public void extractRenderState(SpiritImpaleEntity entity, SpiritImpaleRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getImpaleScale();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(SpiritImpaleRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        float age = state.age;
        float scale = state.scale;
        float fade = FxEnvelope.envelope(age, state.maxLifetime, 0.12f, 0.28f);
        if (fade <= 0.02f) return;

        float slam = Math.min(1.0f, age / 4.0f);
        float pulse = 0.85f + 0.15f * (float) Math.sin(age * 0.4f);
        Vec3 origin = new Vec3(state.x, state.y, state.z);
        float distScale = MaterialEmitters.distanceScale(origin);
        float emission = fade * distScale;
        float delta = frameDelta();

        float baseRadius = 2.4f * scale * slam;
        float pillarHeight = 6.0f * slam;
        float pillarRadius = 0.65f * scale * pulse;
        float swordRingRadius = 1.65f * scale * (1.3f - slam * 0.3f);
        float targetCenterY = 0.75f * scale + (float) Math.sin(age * 0.3f) * 0.04f;

        emit(level,
            MaterialEmitters.Preset.soul(CYAN, 3.10f, seedInt(state.seed, 1),
                new Vec3(0.0, 0.035, 0.0), 0.025f),
            MaterialEmitters.groundArea(origin, baseRadius * 0.98, origin.y + 0.06),
            95.0f, 120, delta, emission);
        emit(level,
            MaterialEmitters.Preset.vapor(GOLD, 4.80f, seedInt(state.seed, 2),
                new Vec3(0.0, 0.015, 0.0), 0.020f),
            MaterialEmitters.groundArea(origin, baseRadius * 0.80, origin.y + 0.05),
            36.0f, 50, delta, emission);
        emit(level,
            MaterialEmitters.Preset.soul(WHITE, 2.90f, seedInt(state.seed, 3),
                new Vec3(0.0, 0.140, 0.0), 0.020f),
            MaterialEmitters.volume(origin.add(new Vec3(0.0, pillarHeight * 0.52, 0.0)),
                new Vec3(pillarRadius * 0.48, Math.max(0.05, pillarHeight * 0.52), pillarRadius * 0.48)),
            118.0f, 145, delta, emission);
        emit(level,
            MaterialEmitters.Preset.soul(CYAN, 3.45f, seedInt(state.seed, 4),
                new Vec3(0.0, 0.100, 0.0), 0.035f),
            MaterialEmitters.volume(origin.add(new Vec3(0.0, pillarHeight * 0.50, 0.0)),
                new Vec3(pillarRadius * 1.30, Math.max(0.05, pillarHeight * 0.50), pillarRadius * 1.30)),
            88.0f, 110, delta, emission);
        emit(level,
            MaterialEmitters.Preset.plasma(WHITE, 2.70f, seedInt(state.seed, 5),
                new Vec3(0.0, -0.160, 0.0), 0.050f),
            MaterialEmitters.volume(origin.add(new Vec3(0.0, targetCenterY, 0.0)),
                new Vec3(swordRingRadius, Math.max(0.05, 0.62 * scale), swordRingRadius)),
            78.0f, 98, delta, emission);
        emit(level,
            MaterialEmitters.Preset.vapor(CYAN, 5.20f, seedInt(state.seed, 6),
                new Vec3(0.0, 0.020, 0.0), 0.025f),
            MaterialEmitters.volume(origin.add(new Vec3(0.0, pillarHeight * 0.45, 0.0)),
                new Vec3(baseRadius * 0.72, Math.max(0.05, pillarHeight * 0.45), baseRadius * 0.72)),
            40.0f, 55, delta, emission);

        // Complex particle layer: wisp curls rising through the pillar
        // volume, inside the 40 per second continuous budget.
        if (pillarHeight > 0.30f) {
            emit(level,
                MaterialEmitters.Preset.fixed(DbaParticles.WISP_CURL, new Vec3(0.0, 0.040, 0.0), 0.050f),
                MaterialEmitters.volume(origin.add(new Vec3(0.0, pillarHeight * 0.50, 0.0)),
                    new Vec3(baseRadius * 0.55, Math.max(0.05, pillarHeight * 0.50), baseRadius * 0.55)),
                24.0f, 40, delta, emission);
        }

        // Pieces layer, class M, in entity local space through the
        // normal depth tested paths. Vertex count at distScale 1.0:
        // five shards at 288 vertices (1440) plus two tendrils at 384
        // (768), for 2208 of the 2500 class M budget.
        if (slam > 0.05f && pillarHeight > 0.30f) {
            final long geoSeed = state.seed;
            final float geoEnv = fade;
            final float geoDs = distScale;
            final float spikeLength = pillarHeight;
            final float spikeGirth = pillarRadius;
            final float ringRadius = swordRingRadius;
            final float ringLength = 1.85f * scale * slam;
            final float ringGirth = 0.30f * scale;
            final float wakeRadius = baseRadius * 0.55f;
            ComplexGeometry.submitSolid(collector, poseStack, (pose, buffer) -> {
                ComplexGeometry.shard(pose, buffer, geoSeed ^ 0x5AA1L,
                    Vec3.ZERO, new Vec3(0.0, 1.0, 0.0), spikeLength, spikeGirth,
                    CYAN, WHITE, 0.96f, geoEnv);
                ComplexGeometry.Rng ringRng = new ComplexGeometry.Rng(geoSeed ^ 0xB161L);
                float angleOffset = ringRng.nextFloat() * 0.6f;
                for (int i = 0; i < 4; i++) {
                    double ang = angleOffset + i * (Math.PI * 2.0 / 4.0);
                    double cos = Math.cos(ang);
                    double sin = Math.sin(ang);
                    Vec3 ringPos = new Vec3(cos * ringRadius, 0.0, sin * ringRadius);
                    Vec3 ringDir = new Vec3(-cos * 0.35, 1.0, -sin * 0.35).normalize();
                    ComplexGeometry.shard(pose, buffer, geoSeed + i * 0x9E37L + 0x5AA2L,
                        ringPos, ringDir, ringLength, ringGirth,
                        GOLD, WHITE, 0.94f, geoEnv);
                }
            });
            ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
                for (int s = 0; s < 2; s++) {
                    double a0 = s * Math.PI + 0.7;
                    double a1 = a0 + 0.9;
                    double a2 = a0 + 1.6;
                    Vec3 wakeFrom = new Vec3(Math.cos(a0) * wakeRadius, 0.05, Math.sin(a0) * wakeRadius);
                    Vec3 wakeControl = new Vec3(Math.cos(a1) * wakeRadius * 0.70,
                        spikeLength * 0.55, Math.sin(a1) * wakeRadius * 0.70);
                    Vec3 wakeTo = new Vec3(Math.cos(a2) * spikeGirth * 0.40,
                        spikeLength * 0.96, Math.sin(a2) * spikeGirth * 0.40);
                    ComplexGeometry.tendril(pose, buffer, geoSeed ^ (0xACE0L + s * 0x101L),
                        wakeFrom, wakeControl, wakeTo, 0.30f * scale,
                        CYAN, WHITE, 0.90f, geoEnv, geoDs);
                }
            });
        }
    }

    private static void emit(ClientLevel level, MaterialEmitters.Preset preset, MaterialEmitters.Source source,
                             float ratePerSecond, int budgetPerSecond, float deltaSeconds, float envelope) {
        if (envelope <= 0.01f) return;
        new MaterialEmitters.Emitter(level, preset, source, ratePerSecond, budgetPerSecond)
            .tick(deltaSeconds, envelope);
    }

    private static int seedInt(long seed, int stream) {
        long mixed = seed ^ stream * 0x9E3779B97F4A7C15L;
        return (int) (mixed ^ (mixed >>> 32));
    }

    private static float frameDelta() {
        long frameTimeNs = Minecraft.getInstance().getFrameTimeNs();
        if (frameTimeNs <= 0L) return 1.0f / 60.0f;
        return Math.max(0.002f, Math.min(0.05f, frameTimeNs / 1_000_000_000.0f));
    }
}
