package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.PowerPoleWhirlwindEntity;
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

import java.util.HashMap;
import java.util.Map;

/**
 * Entity Renderer for Power Pole Whirlwind in Minecraft 26.2.
 *
 * Complex pass: layered composition. The pieces layer is a
 * ComplexGeometry tendrilFan of five circling strands in the cone the
 * entity already defines: pale cyan at the root running to emerald at
 * the tips, matching the vapor theme, fanned at the cone angle along
 * the cone axis and rotated around that axis with age on a copy of
 * the submitted pose, so the strands circle with the spin the filler
 * particles carry, through the dissolve shader with the tendril
 * canvas. The complex particle layer is ASH_FLAKE sprites carried
 * airborne through the cone volume. The material filler stays as
 * before: cyan-white vapor near the core, emerald vapor through the
 * body, short plasma streaks racing with the spin, dust where the
 * moving air meets the ground near the caster, and occasional steel
 * sparks shed from contacts in the outer flow. The entity already
 * spawns a DebrisSpawner field server side for the big chunks; these
 * strands are the fast near-field structure debris cannot time.
 *
 * Lifecycle math is unchanged from the bespoke renderer and mirrors
 * PowerPoleWhirlwindEntity.currentEnvelope: build over BUILD_TICKS, hold
 * while refreshed, dissipate over DECAY_TICKS after DECAY_START.
 */
public class PowerPoleWhirlwindRenderer extends EntityRenderer<PowerPoleWhirlwindEntity, PowerPoleWhirlwindRenderer.WhirlwindRenderState> {

    private static final Map<Integer, EmissionRig> RIGS = new HashMap<>();

    public PowerPoleWhirlwindRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class WhirlwindRenderState extends EntityRenderState {
        public float yRot = 0.0f;
        public float xRot = 0.0f;
        public float range = 25.0f;
        public float coneAngle = 35.0f;
        public float age = 0.0f;
        public int decayStart = -1;
        public int entityId = -1;
        public long seed = 0;
        public boolean isFirstPersonOwner = false;
        public boolean onRight = true;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(PowerPoleWhirlwindEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(PowerPoleWhirlwindEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public WhirlwindRenderState createRenderState() {
        return new WhirlwindRenderState();
    }

    @Override
    public void extractRenderState(PowerPoleWhirlwindEntity entity, WhirlwindRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getEntityYaw();
        state.xRot = entity.getEntityPitch();
        state.range = entity.getRange();
        state.coneAngle = entity.getConeAngle();
        state.age = entity.tickCount + partialTicks;
        state.decayStart = entity.getDecayStart();
        state.entityId = entity.getId();
        // Material seed: hashed from the entity's synced state only
        // (caster id, range, cone angle), never from client random or
        // wall clock, so every client seeds the same material phases.
        state.seed = (entity.getCasterId() * 0x9E3779B97F4A7C15L)
            ^ (Float.floatToIntBits(entity.getRange()) * 31L)
            ^ Float.floatToIntBits(entity.getConeAngle());

        Minecraft mc = Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null &&
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) &&
            mc.options.getCameraType().isFirstPerson());
        if (mc.player != null) {
            boolean isRightHanded = (mc.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            boolean isOffhand = (mc.player.getOffhandItem().getItem() instanceof com.dragonblockarcanedba.item.PowerPoleItem &&
                !(mc.player.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.PowerPoleItem));
            state.onRight = isRightHanded ? !isOffhand : isOffhand;
        }
    }

    @Override
    public void submit(WhirlwindRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float range = state.range;
        if (range < 1.0f) return;

        float age = state.age;
        float alphaMult;
        if (state.decayStart >= 0) {
            float peak = state.decayStart < PowerPoleWhirlwindEntity.BUILD_TICKS
                ? com.dragonblockarcanedba.util.FxEnvelope.smoothstep(
                    state.decayStart / (float) PowerPoleWhirlwindEntity.BUILD_TICKS)
                : 1.0f;
            alphaMult = age <= state.decayStart
                ? peak
                : peak * (1.0f - com.dragonblockarcanedba.util.FxEnvelope.smoothstep(
                    (age - state.decayStart) / (float) PowerPoleWhirlwindEntity.DECAY_TICKS));
        } else if (age < PowerPoleWhirlwindEntity.BUILD_TICKS) {
            alphaMult = com.dragonblockarcanedba.util.FxEnvelope.smoothstep(
                age / (float) PowerPoleWhirlwindEntity.BUILD_TICKS);
        } else {
            alphaMult = 1.0f;
        }
        if (alphaMult <= 0.01f) {
            RIGS.remove(state.entityId);
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        Vec3 origin = new Vec3(state.x, state.y, state.z);
        float distScale = MaterialEmitters.distanceScale(origin);
        if (distScale <= 0.01f) return;

        EmissionRig rig = rigFor(level, state);
        if (rig == null) return;
        float delta = rig.deltaFor(age);
        float swell = 0.55f + 0.45f * alphaMult;
        float coneTan = (float) Math.tan(Math.toRadians(state.coneAngle));
        Vec3 forward = direction(state.yRot, state.xRot);
        float spin = age * 65.0f * (float) (Math.PI / 180.0);
        rig.frame.update(origin, forward, range, coneTan, swell, spin);
        rig.tick(delta, alphaMult * distScale);

        // Pieces layer: five tendril strands fanned at the cone angle
        // along the cone axis, length matched to the cone reach and
        // scaled by the same swell the filler cone uses. Rotation is
        // applied inside the piece on a copy of the submitted pose,
        // around the cone axis at the filler spin rate, so the strands
        // circle with the flow instead of hanging static in the cone.
        // Five strands at full tendril sampling stay inside the
        // class M vertex budget (about 1920 of 2500 at distScale 1).
        final Vec3 gForward = forward;
        final float gLen = range * 0.92f * swell;
        final float gSpread = state.coneAngle;
        final float gSpin = spin;
        final float gEnv = alphaMult;
        final float gDist = distScale;
        final long gSeed = state.seed;
        ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
            PoseStack local = new PoseStack();
            local.last().pose().set(pose.pose());
            local.last().normal().set(pose.normal());
            local.pushPose();
            local.mulPose(new org.joml.Matrix4f().rotate(gSpin,
                (float) gForward.x, (float) gForward.y, (float) gForward.z));
            ComplexGeometry.tendrilFan(local.last(), buffer, gSeed ^ 0xF4A1L, Vec3.ZERO,
                gForward, gSpread, gLen, 5, 0.34f,
                0xEFFFFF, 0x26FFA6, 0.92f, gEnv, gDist);
            local.popPose();
        });
    }

    private static EmissionRig rigFor(ClientLevel level, WhirlwindRenderState state) {
        long now = level.getGameTime();
        RIGS.values().removeIf(rig -> now - rig.lastSeenGameTime > 80L);
        EmissionRig rig = RIGS.get(state.entityId);
        if (rig != null && (rig.level != level || rig.seed != state.seed || state.age < rig.lastAge - 1.0f)) {
            RIGS.remove(state.entityId);
            rig = null;
        }
        if (rig == null) {
            rig = new EmissionRig(level, state.seed);
            rig.lastAge = state.age;
            RIGS.put(state.entityId, rig);
        }
        rig.lastSeenGameTime = now;
        return rig;
    }

    private static Vec3 direction(float yawDegrees, float pitchDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        double cp = Math.cos(pitch);
        Vec3 dir = new Vec3(-Math.sin(yaw) * cp, -Math.sin(pitch), Math.cos(yaw) * cp);
        return dir.lengthSqr() < 1.0e-8 ? new Vec3(0, 0, 1) : dir.normalize();
    }

    /** Mutable cone frame read by the emitter sources at sample time. */
    private static final class Frame {
        Vec3 origin = Vec3.ZERO;
        Vec3 forward = new Vec3(0, 0, 1);
        Vec3 basisA = new Vec3(1, 0, 0);
        Vec3 basisB = new Vec3(0, 1, 0);
        float range = 1.0f;
        float coneTan = 0.7f;
        float swell = 1.0f;
        float spin = 0.0f;

        void update(Vec3 origin, Vec3 forward, float range, float coneTan, float swell, float spin) {
            this.origin = origin;
            this.forward = forward;
            this.range = range;
            this.coneTan = coneTan;
            this.swell = swell;
            this.spin = spin;
            Vec3 ref = Math.abs(forward.y) > 0.92 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            this.basisA = forward.cross(ref).normalize();
            this.basisB = forward.cross(this.basisA).normalize();
        }

        Vec3 radial(double angle) {
            return basisA.scale(Math.cos(angle)).add(basisB.scale(Math.sin(angle)));
        }

        Vec3 tangent(Vec3 radial) {
            Vec3 tangent = forward.cross(radial);
            return tangent.lengthSqr() < 1.0e-8 ? basisB : tangent.normalize();
        }
    }

    private static MaterialEmitters.Source coneSource(Frame frame, double radiusFactor,
                                                      double forwardSpeed, double swirlSpeed) {
        return random -> {
            double t = Math.pow(random.nextDouble(), 0.72);
            double s = 0.3 + t * Math.max(0.2, frame.range - 0.3);
            double radius = s * frame.coneTan * radiusFactor * frame.swell;
            double angle = random.nextDouble() * Math.PI * 2.0 + frame.spin * (1.0 + t * 0.7) + s * 0.35;
            Vec3 radial = frame.radial(angle);
            Vec3 pos = frame.origin.add(frame.forward.scale(s)).add(radial.scale(radius));
            Vec3 vel = frame.forward.scale(forwardSpeed * (0.25 + t))
                .add(frame.tangent(radial).scale(swirlSpeed * (0.40 + t)))
                .add(radial.scale(0.02));
            return new MaterialEmitters.Sample(pos, vel);
        };
    }

    private static MaterialEmitters.Source contactDustSource(Frame frame) {
        return random -> {
            double s = 1.0 + random.nextDouble() * frame.range * 0.55;
            Vec3 center = frame.origin.add(frame.forward.scale(s));
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = (0.6 + s * frame.coneTan * 0.35 * frame.swell) * Math.sqrt(random.nextDouble());
            Vec3 radial = frame.radial(angle);
            Vec3 pos = new Vec3(
                center.x + radial.x * radius,
                frame.origin.y - 1.62 + random.nextDouble() * 0.22,
                center.z + radial.z * radius);
            Vec3 vel = frame.tangent(frame.radial(angle)).scale(0.10)
                .add(frame.forward.scale(0.05));
            return new MaterialEmitters.Sample(pos, vel);
        };
    }

    private static final class EmissionRig {
        final ClientLevel level;
        final long seed;
        final Frame frame = new Frame();
        final MaterialEmitters.Emitter vaporCore;
        final MaterialEmitters.Emitter vaporEmerald;
        final MaterialEmitters.Emitter plasmaStreaks;
        final MaterialEmitters.Emitter contactDust;
        final MaterialEmitters.Emitter sparks;
        final MaterialEmitters.Emitter ashFlakes;
        float lastAge = 0.0f;
        long lastSeenGameTime = 0L;
        boolean started = false;

        EmissionRig(ClientLevel level, long seed) {
            this.level = level;
            this.seed = seed;
            int s = (int) seed;
            this.vaporCore = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(0xEFFFFF, 1.25f, s ^ 0xC0DE, Vec3.ZERO, 0.015f),
                coneSource(frame, 0.62, 0.25, 0.55), 250.0f, 340);
            this.vaporEmerald = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(0x26FFA6, 1.45f, s ^ 0xE43, Vec3.ZERO, 0.018f),
                coneSource(frame, 0.95, 0.18, 0.45), 230.0f, 320);
            this.plasmaStreaks = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0x7DFFE8, 0.62f, s ^ 0x57EA, Vec3.ZERO, 0.012f),
                coneSource(frame, 0.84, 0.70, 1.00), 120.0f, 180);
            this.contactDust = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(0x9A8F7A, 1.00f, s ^ 0xD057, Vec3.ZERO, 0.040f),
                contactDustSource(frame), 55.0f, 90);
            this.sparks = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK, Vec3.ZERO, 0.22f),
                coneSource(frame, 1.00, 0.40, 0.80), 22.0f, 50);
            // Complex particle layer: ash flake grit carried airborne
            // through the cone volume, at the standard continuous cap.
            this.ashFlakes = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.ASH_FLAKE, Vec3.ZERO, 0.03f),
                coneSource(frame, 0.95, 0.18, 0.45), 24.0f, 40);
        }

        float deltaFor(float age) {
            if (!started) {
                started = true;
                lastAge = age;
                return 1.0f / 60.0f;
            }
            float delta = (age - lastAge) / 20.0f;
            lastAge = age;
            return Math.max(0.0f, Math.min(0.05f, delta));
        }

        void tick(float delta, float envelope) {
            if (delta <= 0.0f || envelope <= 0.01f) return;
            vaporCore.tick(delta, envelope);
            vaporEmerald.tick(delta, envelope);
            plasmaStreaks.tick(delta, envelope);
            contactDust.tick(delta, envelope);
            sparks.tick(delta, envelope);
            ashFlakes.tick(delta, envelope);
        }
    }
}
