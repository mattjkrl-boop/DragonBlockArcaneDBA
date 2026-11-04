package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.ErasureChargeOrbEntity;
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
 * Entity Renderer for Erasure Cannon Muzzle Charge Orb in MC 26.2.
 * No-shapes pass: this renderer draws no geometry. The orb is a dense,
 * churning cluster of ki plasma particles at the muzzle: a white-hot
 * core volume, a cyan plasma body, vapor drifting at the boundary,
 * and gathering motes streamed inward from the old intake radius so
 * the charge still reads as contained energy being pulled in. Past
 * 0.75 charge the cluster sheds embers and its hot layer shifts
 * toward red-hot, carrying the old shell color journey. Emission
 * runs through persistent per-entity MaterialEmitters Emitter
 * channels, so the accumulator and per-second budgets survive across
 * frames; the channels rebuild only when the charge bucket changes.
 * Position, scale, pulse, and charge growth are unchanged; the
 * entity's own server charge particles are untouched.
 */
public class ErasureChargeOrbRenderer extends EntityRenderer<ErasureChargeOrbEntity, ErasureChargeOrbRenderer.ErasureChargeRenderState> {
    private static final Map<Integer, Emission> EMISSIONS = new ConcurrentHashMap<>();

    public ErasureChargeOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ErasureChargeRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public float yRot = 0.0f;
        public float xRot = 0.0f;
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
        Vec3 center = Vec3.ZERO;
        float orbRadius = 0.0f;
        float intakeRadius = 0.0f;
        double pull = 0.0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(ErasureChargeOrbEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(ErasureChargeOrbEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ErasureChargeRenderState createRenderState() {
        return new ErasureChargeRenderState();
    }

    @Override
    public void extractRenderState(ErasureChargeOrbEntity entity, ErasureChargeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
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
    public void submit(ErasureChargeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        float charge = state.chargeRatio;
        float age = state.age;
        float pulse = 0.92f + 0.08f * (float) Math.sin(age * 0.65f);

        // Scaled down smaller in first person
        float chargeEase = charge * charge * (3.0f - 2.0f * charge);
        float orbRadius = (0.22f + chargeEase * 0.95f) * pulse * (state.isFirstPersonOwner ? 0.45f : 1.0f);
        float hot = charge <= 0.75f ? 0.0f : (charge - 0.75f) / 0.25f;

        Vec3 center = state.isFirstPersonOwner
            ? localPoint(state, (state.onRight ? -1.0f : 1.0f) * 0.42f, -0.48f, -0.60f)
            : state.origin;

        float distScale = MaterialEmitters.distanceScale(center);
        float envelope = (0.25f + chargeEase * 0.75f) * distScale * (state.isFirstPersonOwner ? 0.75f : 1.0f);
        if (envelope <= 0.01f) return;

        Emission em = EMISSIONS.computeIfAbsent(state.entityId, id -> new Emission());
        em.center = center;
        em.orbRadius = orbRadius;
        em.intakeRadius = orbRadius * (2.3f - chargeEase * 0.95f);
        em.pull = 0.07 + charge * 0.09;

        float delta;
        if (Float.isNaN(em.lastAge) || age < em.lastAge) {
            delta = 1.0f / 60.0f;
        } else {
            delta = Math.min(0.10f, Math.max(0.0f, (age - em.lastAge) / 20.0f));
        }
        em.lastAge = age;
        if (delta <= 0f) return;

        float chargeBucket = Math.round(charge * 20.0f) / 20.0f;
        String signature = System.identityHashCode(level) + "|" + Math.round(charge * 20.0f)
            + "|" + state.isFirstPersonOwner;
        if (!signature.equals(em.signature)) {
            em.signature = signature;
            em.emitters = buildEmitters(level, em, state.seed, chargeBucket, hot);
        }
        for (MaterialEmitters.Emitter emitter : em.emitters) {
            emitter.tick(delta, envelope);
        }
        if (EMISSIONS.size() > 256) EMISSIONS.clear();
    }

    private static MaterialEmitters.Emitter[] buildEmitters(ClientLevel level, Emission em, int seed,
                                                            float charge, float hot) {
        MaterialEmitters.Source coreVolume = random -> MaterialEmitters.sphereVolume(em.center,
            Math.max(0.05, em.orbRadius * 0.85)).sample(random);
        MaterialEmitters.Source bodyVolume = random -> MaterialEmitters.sphereVolume(em.center,
            Math.max(0.06, em.orbRadius * 1.05)).sample(random);
        MaterialEmitters.Source boundaryVolume = random -> MaterialEmitters.sphereVolume(em.center,
            Math.max(0.08, em.orbRadius * 1.28)).sample(random);
        // Intake: motes spawn around the intake radius, jittered so no
        // clean shell forms, and stream inward toward the core.
        MaterialEmitters.Source intake = random -> {
            double u = random.nextDouble() * 2.0 - 1.0;
            double ang = random.nextDouble() * Math.PI * 2.0;
            double rr = em.intakeRadius * (0.85 + random.nextDouble() * 0.30);
            double flat = Math.sqrt(Math.max(0.0, 1.0 - u * u)) * rr;
            Vec3 p = new Vec3(em.center.x + Math.cos(ang) * flat,
                em.center.y + u * rr, em.center.z + Math.sin(ang) * flat);
            Vec3 vel = em.center.subtract(p);
            vel = vel.lengthSqr() < 1.0e-6 ? Vec3.ZERO : vel.normalize().scale(em.pull);
            return new MaterialEmitters.Sample(p, vel);
        };

        java.util.List<MaterialEmitters.Emitter> out = new java.util.ArrayList<>();
        out.add(new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0xFFFFFF, Math.max(0.4f, em.orbRadius * 5.2f), seed ^ 0xC0E1, Vec3.ZERO, 0.03f),
            coreVolume, 210f + charge * 340f, 620));
        out.add(new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0x37E6FF, Math.max(0.5f, em.orbRadius * 7.2f), seed ^ 0x5E11, Vec3.ZERO, 0.04f),
            bodyVolume, 180f + charge * 280f, 520));
        if (hot > 0.0f) {
            out.add(new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xFFE8E0, Math.max(0.5f, em.orbRadius * 6.0f), seed ^ 0x5E22, Vec3.ZERO, 0.04f),
                bodyVolume, hot * 150f, 200));
        }
        out.add(new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.vapor(0x59F2FF, Math.max(0.8f, em.orbRadius * 10.5f), seed ^ 0x5E33, Vec3.ZERO, 0.03f),
            boundaryVolume, 95f + charge * 125f, 260));
        out.add(new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0xA5F7FF, Math.max(0.35f, em.orbRadius * 4.5f), seed ^ 0x59, Vec3.ZERO, 0.004f),
            intake, 150f + charge * 260f, 460));
        out.add(new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.ember(Vec3.ZERO, 0.06f),
            bodyVolume, charge >= 0.75f ? 130f : 35f, 160));
        out.add(new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.fixed(DbaParticles.SPARK, Vec3.ZERO, 0.07f),
            bodyVolume, 55f + charge * 105f, 190));
        return out.toArray(new MaterialEmitters.Emitter[0]);
    }

    private static Vec3 directionFromRotation(float xRot, float yRot) {
        double pitch = Math.toRadians(xRot);
        double yaw = Math.toRadians(yRot);
        double cosPitch = Math.cos(pitch);
        return new Vec3(-Math.sin(yaw) * cosPitch, -Math.sin(pitch), Math.cos(yaw) * cosPitch).normalize();
    }

    private static Vec3 localPoint(ErasureChargeRenderState state, float x, float y, float z) {
        return state.origin
            .add(state.right.scale(-x))
            .add(state.up.scale(y))
            .add(state.direction.scale(z));
    }
}
