package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.ErasureCannonBeamEntity;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Entity Renderer for Erasure Cannon Beam in MC 26.2.
 * No-shapes pass: this renderer draws no geometry. Following the
 * standard's beam method, the beam is dense ki plasma emitted along
 * its real path: a bright core run, a cyan body run, a darker outer
 * drift, vapor along the path, plasma bunched at the old focusing
 * stations and at the muzzle, and an impact plume of plasma, smoke,
 * vapor, and sparks at the terminus. Emission runs through persistent
 * per-entity MaterialEmitters Emitter channels, so the accumulator
 * and per-second budgets survive across frames; the channels rebuild
 * only when a size or charge bucket changes. The shared FxEnvelope
 * still drives density so emission builds and dissipates with the
 * beam's life, exactly as the mesh alpha did. The entity's own server
 * particles, damage pulses, and endpoint detonation are untouched.
 */
public class ErasureCannonBeamRenderer extends EntityRenderer<ErasureCannonBeamEntity, ErasureCannonBeamRenderer.ErasureCannonRenderState> {
    private static final Map<Integer, Emission> EMISSIONS = new ConcurrentHashMap<>();

    public ErasureCannonBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ErasureCannonRenderState extends EntityRenderState {
        public float length = 48.0f;
        public float yRot = 0.0f;
        public float xRot = 0.0f;
        public float chargeRatio = 1.0f;
        public float age = 0.0f;
        public int seed = 0;
        public boolean isFirstPersonOwner = false;
        public boolean onRight = true;
        public int entityId = -1;
        public Vec3 origin = Vec3.ZERO;
        public Vec3 direction = new Vec3(0, 0, 1);
        public Vec3 right = new Vec3(1, 0, 0);
        public Vec3 up = new Vec3(0, 1, 0);
    }

    /** Per-entity emission channels plus the live frame they sample from. */
    private static final class Emission {
        float lastAge = Float.NaN;
        String signature = "";
        MaterialEmitters.Emitter[] emitters = new MaterialEmitters.Emitter[0];
        Vec3 start = Vec3.ZERO;
        Vec3 end = Vec3.ZERO;
        Vec3 dir = new Vec3(0, 0, 1);
        float span = 0.0f;
        float baseRadius = 0.0f;
        float plumeRadius = 0.0f;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(ErasureCannonBeamEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(ErasureCannonBeamEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ErasureCannonRenderState createRenderState() {
        return new ErasureCannonRenderState();
    }

    @Override
    public void extractRenderState(ErasureCannonBeamEntity entity, ErasureCannonRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.length = entity.getBeamLength();
        state.yRot = entity.getBeamYRot();
        state.xRot = entity.getBeamXRot();
        state.chargeRatio = entity.getChargeRatio();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getSeed();
        state.entityId = entity.getId();
        state.origin = entity.getPosition(partialTicks);
        state.direction = directionFromRotation(state.xRot, state.yRot);
        Vec3 right = state.direction.cross(new Vec3(0, 1, 0));
        state.right = right.lengthSqr() < 1.0e-6 ? new Vec3(1, 0, 0) : right.normalize();
        state.up = state.right.cross(state.direction).normalize();

        Minecraft mc = Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null &&
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) &&
            mc.options.getCameraType().isFirstPerson());
        if (mc.player != null) {
            boolean isRightHanded = (mc.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            boolean isOffhand = (mc.player.getOffhandItem().getItem() instanceof com.dragonblockarcanedba.item.BlasterGunItem &&
                !(mc.player.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.BlasterGunItem));
            state.onRight = isRightHanded ? !isOffhand : isOffhand;
        }
    }

    @Override
    public void submit(ErasureCannonRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float beamLength = state.length;
        if (beamLength < 0.2f) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        float charge = state.chargeRatio;
        float age = state.age;
        float maxLifetime = ErasureCannonBeamEntity.MAX_LIFETIME;

        // Shared envelope (spectacle standard point 5): emission density
        // uses the same FxEnvelope the entity's particle counts use, so
        // the beam builds and dissipates as one effect.
        float env = com.dragonblockarcanedba.util.FxEnvelope.envelope(age, maxLifetime);
        if (env <= 0.01f) return;
        float exitT = age <= maxLifetime * 0.7f ? 0.0f : Math.min(1.0f, (age - maxLifetime * 0.7f) / (maxLifetime * 0.3f));

        float pulse = (1.0f + 0.04f * (float) Math.sin(age * 1.4f)) * (0.55f + 0.45f * env);
        float impactPulse = pulse * (1.12f - exitT * 0.50f);

        float distScale = MaterialEmitters.distanceScale(state.origin);
        float envelope = env * distScale;
        if (envelope <= 0.01f) return;

        // Scaled down smaller in first person
        float baseRadius = (0.35f + charge * 0.45f) * pulse * (state.isFirstPersonOwner ? 0.50f : 1.0f);

        final float zStart = state.isFirstPersonOwner ? 0.20f : 0.0f;
        final float span = beamLength - zStart;
        if (span <= 0.0f) return;

        Vec3 start = beamPoint(state, zStart);
        Vec3 end = start.add(state.direction.scale(span));
        float plumeRadius = (1.2f + charge * 0.8f) * impactPulse;

        Emission em = EMISSIONS.computeIfAbsent(state.entityId, id -> new Emission());
        em.start = start;
        em.end = end;
        em.dir = state.direction;
        em.span = span;
        em.baseRadius = baseRadius;
        em.plumeRadius = plumeRadius;

        float delta;
        if (Float.isNaN(em.lastAge) || age < em.lastAge) {
            delta = 1.0f / 60.0f;
        } else {
            delta = Math.min(0.10f, Math.max(0.0f, (age - em.lastAge) / 20.0f));
        }
        em.lastAge = age;
        if (delta <= 0f) return;

        boolean skipFarStation = distScale < 0.55f;
        String signature = System.identityHashCode(level) + "|" + Math.round(charge * 10.0f) + "|"
            + state.isFirstPersonOwner + "|" + Math.round(span * 2.0f) + "|"
            + Math.round(baseRadius * 8.0f) + "|" + Math.round(plumeRadius * 4.0f) + "|" + skipFarStation;
        if (!signature.equals(em.signature)) {
            em.signature = signature;
            em.emitters = buildEmitters(level, em, state.seed, baseRadius, plumeRadius, skipFarStation);
        }
        for (MaterialEmitters.Emitter emitter : em.emitters) {
            emitter.tick(delta, envelope);
        }
        if (EMISSIONS.size() > 256) EMISSIONS.clear();
    }

    private static MaterialEmitters.Emitter[] buildEmitters(ClientLevel level, Emission em, int seed,
                                                            float baseRadius, float plumeRadius,
                                                            boolean skipFarStation) {
        MaterialEmitters.Source path = random -> MaterialEmitters.path(em.start, em.end, 0.025).sample(random);
        MaterialEmitters.Source muzzle = random -> MaterialEmitters.sphereVolume(em.start,
            Math.max(0.06, em.baseRadius * 0.45)).sample(random);
        MaterialEmitters.Source plume = random -> MaterialEmitters.sphereVolume(em.end,
            Math.max(0.10, em.plumeRadius * 0.55)).sample(random);

        java.util.List<MaterialEmitters.Emitter> out = new java.util.ArrayList<>();
        // Beam body: three plasma runs along the path plus a vapor run.
        out.add(new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0xFFFFFF, Math.max(0.5f, baseRadius * 5.0f),
                seed ^ 0xC0EEE, Vec3.ZERO, 0.012f),
            path, 430f, 560));
        out.add(new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0x00EBFF, Math.max(0.7f, baseRadius * 8.0f),
                seed ^ 0x111D, Vec3.ZERO, 0.020f),
            path, 350f, 460));
        out.add(new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0x008CD9, Math.max(0.9f, baseRadius * 11.0f),
                seed ^ 0x2222, Vec3.ZERO, 0.030f),
            path, 190f, 260));
        out.add(new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.vapor(0x59F2FF, Math.max(1.0f, baseRadius * 13.0f),
                seed ^ 0x3A77, Vec3.ZERO, 0.025f),
            path, 95f, 140));

        // Focusing stations: loose plasma clusters at the old ring
        // positions, tightening with distance from the muzzle.
        float[] stationFracs = {0.25f, 0.50f, 0.75f};
        for (int ri = 0; ri < stationFracs.length; ri++) {
            if (skipFarStation && ri == 2) continue;
            final float frac = stationFracs[ri];
            MaterialEmitters.Source station = random -> MaterialEmitters.sphereVolume(
                em.start.add(em.dir.scale(frac * em.span)),
                Math.max(0.08, em.baseRadius * (2.3f - frac * 0.7f) * 0.35)).sample(random);
            out.add(new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0x59F2FF, Math.max(0.6f, baseRadius * 7.0f),
                    seed ^ (0xA9E0 + ri * 131), Vec3.ZERO, 0.03f),
                station, 70f, 100));
        }

        // Muzzle cluster.
        out.add(new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0xD9FCFF, Math.max(0.6f, baseRadius * 6.0f),
                seed ^ 0x3333, Vec3.ZERO, 0.03f),
            muzzle, 140f, 200));

        // Terminus impact plume.
        out.add(new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0x00F2FF, Math.max(0.7f, plumeRadius * 2.6f),
                seed ^ 0xD0E1, Vec3.ZERO, 0.05f),
            plume, 320f, 420));
        out.add(new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0xFFFFFF, Math.max(0.4f, plumeRadius * 1.2f),
                seed ^ 0xC02E, Vec3.ZERO, 0.04f),
            plume, 130f, 180));
        out.add(new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.smoke(0x1B3D47, Math.max(1.0f, plumeRadius * 3.2f),
                seed ^ 0x5907, new Vec3(0, 0.02, 0), 0.04f),
            plume, 75f, 110));
        out.add(new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.vapor(0x59F2FF, Math.max(1.0f, plumeRadius * 3.6f),
                seed ^ 0x591A, Vec3.ZERO, 0.05f),
            plume, 105f, 150));
        out.add(new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.fixed(DbaParticles.SPARK, Vec3.ZERO, 0.16f),
            plume, 140f, 200));

        return out.toArray(new MaterialEmitters.Emitter[0]);
    }

    private static Vec3 directionFromRotation(float xRot, float yRot) {
        double pitch = Math.toRadians(xRot);
        double yaw = Math.toRadians(yRot);
        double cosPitch = Math.cos(pitch);
        return new Vec3(-Math.sin(yaw) * cosPitch, -Math.sin(pitch), Math.cos(yaw) * cosPitch).normalize();
    }

    /**
     * A point on the beam in world space. Local beam coordinates map
     * onto the stored basis the same way the old pose stack did,
     * including the first-person muzzle offset to the weapon hand.
     */
    private static Vec3 beamPoint(ErasureCannonRenderState state, float z) {
        if (!state.isFirstPersonOwner) {
            return state.origin.add(state.direction.scale(z));
        }
        float sideSign = state.onRight ? -1.0f : 1.0f;
        return localPoint(state, sideSign * 0.38f, -0.46f, 0.40f + z);
    }

    private static Vec3 localPoint(ErasureCannonRenderState state, float x, float y, float z) {
        return state.origin
            .add(state.right.scale(-x))
            .add(state.up.scale(y))
            .add(state.direction.scale(z));
    }
}
