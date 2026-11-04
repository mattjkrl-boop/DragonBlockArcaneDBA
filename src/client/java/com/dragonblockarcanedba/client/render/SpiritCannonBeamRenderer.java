package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.SpiritCannonBeamEntity;
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

/**
 * Entity renderer for the Spirit Cannon beam.
 *
 * No-shapes pass: the renderer draws no geometry. The synced beam path is
 * emission data only. Soul material carries the white core and cyan sheath,
 * vapor softens the outside, gold souls gather toward the terminus, and the
 * contact point sheds plasma, vapor, and sparks. The lifecycle envelope,
 * first person anchor, fade retract, pulse, colors, and synced seed are the
 * bespoke pass values; only the presentation material changed.
 */
public class SpiritCannonBeamRenderer extends EntityRenderer<SpiritCannonBeamEntity, SpiritCannonBeamRenderer.SpiritCannonRenderState> {

    private static final int WHITE = 0xFFFFFF;
    private static final int CYAN = 0x00E5FF;
    private static final int GOLD = 0xFFC83C;

    public SpiritCannonBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SpiritCannonRenderState extends EntityRenderState {
        public float length = 32.0f;
        public float yRot = 0.0f;
        public float xRot = 0.0f;
        public float age = 0.0f;
        public float fade = 1.0f;
        public int casterId = -1;
        public boolean isFirstPersonOwner = false;
        public boolean onRight = true;
        public long seed = 0L;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(SpiritCannonBeamEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(SpiritCannonBeamEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public SpiritCannonRenderState createRenderState() {
        return new SpiritCannonRenderState();
    }

    @Override
    public void extractRenderState(SpiritCannonBeamEntity entity, SpiritCannonRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.length = entity.getBeamLength();
        state.yRot = entity.getBeamYRot();
        state.xRot = entity.getBeamXRot();
        state.age = entity.tickCount + partialTicks;
        state.fade = entity.getFade();
        state.casterId = entity.getCasterId();
        state.seed = entity.getSeed();

        Minecraft mc = Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null &&
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) &&
            mc.options.getCameraType().isFirstPerson());
        if (mc.player != null) {
            boolean isRightHanded = (mc.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            boolean isOffhand = (mc.player.getOffhandItem().getItem() instanceof com.dragonblockarcanedba.item.SpiritSwordItem &&
                !(mc.player.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.SpiritSwordItem));
            state.onRight = isRightHanded ? !isOffhand : isOffhand;
        }
    }

    @Override
    public void submit(SpiritCannonRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || state.length < 0.2f || state.fade <= 0.01f) return;

        float age = state.age;
        float pulse = 1.0f + 0.06f * (float) Math.sin(age * 1.5f);
        float fadeIn = FxEnvelope.smoothstep(Math.min(1.0f, age / 5.0f));
        float env = fadeIn * state.fade;
        if (env <= 0.01f) return;

        Vec3 origin = new Vec3(state.x, state.y, state.z);
        float distScale = MaterialEmitters.distanceScale(origin);
        float emission = env * distScale;
        float delta = frameDelta();

        Matrix4f rotation = new Matrix4f()
            .rotateY((float) Math.toRadians(-state.yRot))
            .rotateX((float) Math.toRadians(state.xRot));
        Vec3 direction = transformDirection(rotation, new Vec3(0.0, 0.0, 1.0));

        float sideSign = state.onRight ? -1.0f : 1.0f;
        Vec3 localOffset = state.isFirstPersonOwner
            ? new Vec3(sideSign * 0.40f, -0.46f, 0.35f)
            : Vec3.ZERO;
        float zStart = state.isFirstPersonOwner ? 0.25f : 0.0f;
        float fpScale = state.isFirstPersonOwner ? 0.50f : 1.0f;
        float drawLength = zStart + (state.length - zStart) * (0.15f + 0.85f * state.fade);
        if (drawLength <= zStart + 0.05f) return;

        Vec3 start = origin.add(transformPosition(rotation, localOffset.add(new Vec3(0.0, 0.0, zStart))));
        Vec3 end = origin.add(transformPosition(rotation, localOffset.add(new Vec3(0.0, 0.0, drawLength))));
        Vec3 goldStart = start.add(end.subtract(start).scale(0.60));
        float runLength = (float) start.distanceTo(end);
        float lengthScale = Math.max(0.35f, Math.min(1.6f, runLength / 16.0f));
        int seed = seedInt(state.seed, 1);

        MaterialEmitters.Source beamPath = MaterialEmitters.path(start, end, 0.025);
        emit(level,
            MaterialEmitters.Preset.soul(WHITE, 2.65f * pulse * fpScale, seed,
                direction.scale(0.030), 0.035f),
            beamPath, 185.0f * lengthScale, 230, delta, emission);
        emit(level,
            MaterialEmitters.Preset.soul(CYAN, 3.55f * pulse * fpScale, seedInt(state.seed, 2),
                direction.scale(0.022), 0.055f),
            beamPath, 115.0f * lengthScale, 150, delta, emission);
        emit(level,
            MaterialEmitters.Preset.vapor(CYAN, 5.10f * pulse * fpScale, seedInt(state.seed, 3),
                direction.scale(0.012), 0.085f),
            beamPath, 42.0f * lengthScale, 60, delta, emission);
        emit(level,
            MaterialEmitters.Preset.soul(GOLD, 3.30f * pulse * fpScale, seedInt(state.seed, 4),
                direction.scale(0.018), 0.060f),
            MaterialEmitters.path(goldStart, end, 0.012),
            38.0f * lengthScale, 55, delta, emission);

        emit(level,
            MaterialEmitters.Preset.soul(CYAN, 3.15f * pulse * fpScale, seedInt(state.seed, 5),
                direction.scale(0.045), 0.045f),
            MaterialEmitters.sphereVolume(start, 0.26 * pulse * fpScale),
            58.0f, 75, delta, emission);
        emit(level,
            MaterialEmitters.Preset.plasma(WHITE, 3.25f * pulse * fpScale, seedInt(state.seed, 6),
                direction.scale(0.20), 0.30f),
            MaterialEmitters.sphereVolume(end, 0.48 * pulse * fpScale),
            95.0f, 120, delta, emission);
        emit(level,
            MaterialEmitters.Preset.vapor(CYAN, 4.65f * pulse * fpScale, seedInt(state.seed, 7),
                direction.scale(0.05), 0.16f),
            MaterialEmitters.sphereVolume(end, 0.72 * pulse * fpScale),
            34.0f, 50, delta, emission);
        emit(level,
            MaterialEmitters.Preset.fixed(DbaParticles.SPARK, direction.scale(0.10), 0.30f),
            MaterialEmitters.point(end),
            42.0f, 60, delta, emission);
    }

    private static void emit(ClientLevel level, MaterialEmitters.Preset preset, MaterialEmitters.Source source,
                             float ratePerSecond, int budgetPerSecond, float deltaSeconds, float envelope) {
        if (envelope <= 0.01f) return;
        new MaterialEmitters.Emitter(level, preset, source, ratePerSecond, budgetPerSecond)
            .tick(deltaSeconds, envelope);
    }

    private static Vec3 transformPosition(Matrix4f rotation, Vec3 local) {
        Vector3f transformed = rotation.transformPosition(new Vector3f(
            (float) local.x, (float) local.y, (float) local.z));
        return new Vec3(transformed.x(), transformed.y(), transformed.z());
    }

    private static Vec3 transformDirection(Matrix4f rotation, Vec3 local) {
        Vector3f transformed = rotation.transformDirection(new Vector3f(
            (float) local.x, (float) local.y, (float) local.z));
        return new Vec3(transformed.x(), transformed.y(), transformed.z());
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
