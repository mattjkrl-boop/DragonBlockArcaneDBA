package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.SpiritCleaveEntity;
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
 * Renderer for the Spirit Sword cleave.
 *
 * No-shapes pass: the renderer draws no blade, arc, ribbon, fan, or halo.
 * The projectile was created by the swing and follows a real flight path;
 * soul material is emitted inside its moving volume and along the path it
 * has just traveled, with vapor and sparks dissolving behind it. The growth
 * curve, lifecycle envelope, colors, and synced seed carry over from the
 * bespoke pass.
 */
public class SpiritCleaveRenderer extends EntityRenderer<SpiritCleaveEntity, SpiritCleaveRenderer.CleaveState> {

    private static final int WHITE = 0xFFFFFF;
    private static final int CYAN = 0x00E5FF;
    private static final int GOLD = 0xFFC83C;

    public SpiritCleaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class CleaveState extends EntityRenderState {
        public float yRot;
        public float xRot;
        public float age;
        public float life = SpiritCleaveEntity.FLIGHT_TICKS + SpiritCleaveEntity.FADE_TICKS;
        public long seed;
    }

    public boolean shouldRender(SpiritCleaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(SpiritCleaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public CleaveState createRenderState() {
        return new CleaveState();
    }

    @Override
    public void extractRenderState(SpiritCleaveEntity entity, CleaveState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTick;
        state.life = entity.getLifeTicks();
        state.seed = entity.getSeed();
    }

    @Override
    public void submit(CleaveState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        float open = ease(Math.min(1.0f, state.age / 3.0f));
        float fade = FxEnvelope.envelope(state.age, state.life, 0.08f, 0.30f);
        if (fade <= 0.01f) return;

        float length = 2.25f * (0.30f + 0.70f * open);
        float tipFlash = ease(Math.max(0.0f, 1.0f - Math.max(0.0f, state.age - 4.0f) / 4.0f));
        Vec3 origin = new Vec3(state.x, state.y, state.z);
        Vec3 direction = Vec3.directionFromRotation(state.xRot, state.yRot);
        float emission = fade * MaterialEmitters.distanceScale(origin);
        float delta = frameDelta();

        Vec3 traveledFrom = origin.subtract(direction.scale(length * 1.35));
        Vec3 traveledTo = origin.add(direction.scale(length * 0.25));
        MaterialEmitters.Source traveledPath = MaterialEmitters.path(traveledFrom, traveledTo, 0.010);

        emit(level,
            MaterialEmitters.Preset.soul(WHITE, 3.00f, seedInt(state.seed, 1),
                direction.scale(0.040), 0.060f),
            MaterialEmitters.sphereVolume(origin, length * 0.42),
            215.0f, 260, delta, emission);
        emit(level,
            MaterialEmitters.Preset.soul(CYAN, 3.50f, seedInt(state.seed, 2),
                direction.scale(-0.030), 0.055f),
            traveledPath, 175.0f, 220, delta, emission);
        emit(level,
            MaterialEmitters.Preset.vapor(GOLD, 4.60f, seedInt(state.seed, 3),
                direction.scale(-0.020), 0.090f),
            traveledPath, 68.0f, 90, delta, emission);
        emit(level,
            MaterialEmitters.Preset.fixed(DbaParticles.SPARK, direction.scale(-0.080), 0.160f),
            traveledPath, 58.0f, 75, delta, emission);

        if (tipFlash > 0.02f) {
            Vec3 openingFront = origin.add(direction.scale(length * 0.76));
            emit(level,
                MaterialEmitters.Preset.soul(WHITE, 3.20f, seedInt(state.seed, 4),
                    direction.scale(0.030), 0.050f),
                MaterialEmitters.sphereVolume(openingFront, 0.22),
                165.0f, 200, delta, emission * tipFlash);
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

    private static float ease(float t) {
        float c = Math.max(0.0f, Math.min(1.0f, t));
        return c * c * (3.0f - 2.0f * c);
    }
}
