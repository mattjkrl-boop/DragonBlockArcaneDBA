package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.PowerPoleExtensionEntity;
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
 * Entity Renderer for Power Pole Extension in Minecraft 26.2.
 *
 * Complex pass: layered composition. The pieces layer is a wind strand
 * along the extension path: a ComplexGeometry tendril in pale gold vapor
 * colors running to crimson at the tip, plus a thinner crimson echo
 * strand bowed to the other side of the path, both through the dissolve
 * shader with the tendril canvas, so the rush of air around the pole
 * reads as coiling strands with torn pinches instead of a glow tube.
 * Attack upgrade pass: a third strand crosses the shaft diagonally so
 * the three strands braid around the pole instead of lying in one
 * plane, and SPARK_STREAK sprites now shed along the whole shaft at
 * stream speed, not only at the tip.
 * The complex particle layer is SPARK_STREAK sprites at the leading
 * tip. The material filler stays as before: pale vapor displaced by
 * the rush, crimson plasma in three heat bands along the stream, a
 * brighter core stream, and dust plus steel sparks at the tip, all
 * sampled along the pole path. The pole item model itself remains a
 * physical object in the hand; this entity is the extension effect.
 *
 * Lifecycle math is unchanged from the bespoke renderer: ease out over
 * 3 ticks, hold, retract over the last 3 ticks, gone at tick 12.
 */
public class PowerPoleExtensionRenderer extends EntityRenderer<PowerPoleExtensionEntity, PowerPoleExtensionRenderer.ExtensionRenderState> {

    private static final Map<Integer, EmissionRig> RIGS = new HashMap<>();

    public PowerPoleExtensionRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ExtensionRenderState extends EntityRenderState {
        public float yRot = 0.0f;
        public float xRot = 0.0f;
        public float maxLength = 30.0f;
        public float age = 0.0f;
        public int casterId = -1;
        public int entityId = -1;
        public long seed = 0;
        public boolean isFirstPersonOwner = false;
        public boolean onRight = true;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(PowerPoleExtensionEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(PowerPoleExtensionEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ExtensionRenderState createRenderState() {
        return new ExtensionRenderState();
    }

    @Override
    public void extractRenderState(PowerPoleExtensionEntity entity, ExtensionRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getEntityYaw();
        state.xRot = entity.getEntityPitch();
        state.maxLength = entity.getMaxLength();
        state.age = entity.tickCount + partialTicks;
        state.casterId = entity.getCasterId();
        state.entityId = entity.getId();
        // Material seed: hashed from the entity's synced state only
        // (caster id, max length), never from client random or wall clock.
        state.seed = (entity.getCasterId() * 0x9E3779B97F4A7C15L)
            ^ Float.floatToIntBits(entity.getMaxLength());

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
    public void submit(ExtensionRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float maxLength = state.maxLength;
        if (maxLength < 0.5f) return;

        float age = state.age;
        float extendProgress;
        float alphaMult = 1.0f;
        if (age < 3.0f) {
            float t = age / 3.0f;
            extendProgress = 1.0f - (1.0f - t) * (1.0f - t) * (1.0f - t);
        } else if (age > 9.0f) {
            float t = Math.min(1.0f, (age - 9.0f) / 3.0f);
            float back = t * t * (3.0f - 2.0f * t);
            extendProgress = 1.0f - back;
            alphaMult = 1.0f - back;
        } else {
            extendProgress = 1.0f;
        }
        if (alphaMult <= 0.01f) {
            RIGS.remove(state.entityId);
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        Vec3 forward = direction(state.yRot, state.xRot);
        Vec3 origin = new Vec3(state.x, state.y, state.z);
        float fpScale = state.isFirstPersonOwner ? 0.55f : 1.0f;
        if (state.isFirstPersonOwner) {
            Vec3 right = rightVector(state.yRot);
            Vec3 up = right.cross(forward).normalize();
            double side = state.onRight ? 0.38 : -0.38;
            origin = origin.add(right.scale(side)).add(up.scale(-0.48)).add(forward.scale(0.20));
        }

        float currentLength = Math.max(0.8f, maxLength * extendProgress);
        float distScale = MaterialEmitters.distanceScale(origin);
        if (distScale <= 0.01f) return;

        EmissionRig rig = rigFor(level, state);
        if (rig == null) return;
        float delta = rig.deltaFor(age);
        rig.frame.update(origin, forward, currentLength, fpScale);
        float envelope = alphaMult * distScale * Math.min(1.0f, 0.35f + currentLength / 18.0f);
        rig.tick(delta, envelope);

        // Pieces layer: three tendril wind strands in local space, from
        // the local start offset to the tip at the live extension
        // length. The builder curl and pinch windows do the coiling;
        // the caller bows the first two strands to opposite sides of
        // the path, and the third crosses the shaft diagonally (starts
        // left and high, ends right and low, control pushed left), so
        // the three strands braid around the pole as air wrapping it,
        // not one line. Each strand carries its own seed, so the
        // builder curl phases sit apart around the shaft.
        Vec3 localFrom = origin.subtract(new Vec3(state.x, state.y, state.z));
        Vec3 localTo = localFrom.add(forward.scale(currentLength));
        Vec3 right = rightVector(state.yRot);
        Vec3 up = right.cross(forward).normalize();
        Vec3 mid = localFrom.add(forward.scale(currentLength * 0.5));
        final Vec3 gFrom = localFrom;
        final Vec3 gTo = localTo;
        final Vec3 gCtrl = mid.add(up.scale(currentLength * 0.05));
        final Vec3 gFrom2 = localFrom.add(right.scale(0.10 * fpScale));
        final Vec3 gTo2 = localTo.add(right.scale(0.10 * fpScale));
        final Vec3 gCtrl2 = mid.add(right.scale(0.10 * fpScale)).add(up.scale(-currentLength * 0.04));
        final Vec3 gFrom3 = localFrom.add(right.scale(-0.10 * fpScale)).add(up.scale(0.10 * fpScale));
        final Vec3 gTo3 = localTo.add(right.scale(0.06 * fpScale)).add(up.scale(-0.10 * fpScale));
        final Vec3 gCtrl3 = mid.add(right.scale(-0.16 * fpScale)).add(up.scale(currentLength * 0.035));
        final float gEnv = alphaMult;
        final float gDist = distScale;
        final long gSeed = state.seed;
        ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
            ComplexGeometry.tendril(pose, buffer, gSeed ^ 0x7E57L, gFrom, gCtrl, gTo, 0.30f,
                0xF7D774, 0xFF3829, 0.92f, gEnv, gDist);
            ComplexGeometry.tendril(pose, buffer, gSeed ^ 0xE11DL, gFrom2, gCtrl2, gTo2, 0.15f,
                0xFF5940, 0x94141C, 0.90f, gEnv, gDist);
            ComplexGeometry.tendril(pose, buffer, gSeed ^ 0xB9A1DL, gFrom3, gCtrl3, gTo3, 0.22f,
                0xFFC46B, 0xD91414, 0.91f, gEnv, gDist);
        });

        if (age >= 12.0f) {
            RIGS.remove(state.entityId);
        }
    }

    private static EmissionRig rigFor(ClientLevel level, ExtensionRenderState state) {
        long now = level.getGameTime();
        RIGS.values().removeIf(rig -> now - rig.lastSeenGameTime > 60L);
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

    private static Vec3 rightVector(float yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        return new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
    }

    /** Mutable path frame read by the emitter sources at sample time. */
    private static final class Frame {
        Vec3 origin = Vec3.ZERO;
        Vec3 forward = new Vec3(0, 0, 1);
        float length = 1.0f;
        float fpScale = 1.0f;

        void update(Vec3 origin, Vec3 forward, float length, float fpScale) {
            this.origin = origin;
            this.forward = forward;
            this.length = length;
            this.fpScale = fpScale;
        }

        Vec3 pointAt(double t) {
            return origin.add(forward.scale(length * t));
        }

        Vec3 tip() {
            return origin.add(forward.scale(length));
        }
    }

    private static MaterialEmitters.Source segmentSource(Frame frame, double from, double to, double pathSpeed) {
        return random -> {
            double t = from + (to - from) * random.nextDouble();
            return new MaterialEmitters.Sample(frame.pointAt(t), frame.forward.scale(pathSpeed));
        };
    }

    private static MaterialEmitters.Source tipSource(Frame frame) {
        return random -> new MaterialEmitters.Sample(frame.tip(), frame.forward.scale(0.22));
    }

    private static final class EmissionRig {
        final ClientLevel level;
        final long seed;
        final Frame frame = new Frame();
        final MaterialEmitters.Emitter vapor;
        final MaterialEmitters.Emitter plasmaHot;
        final MaterialEmitters.Emitter plasmaMid;
        final MaterialEmitters.Emitter plasmaTip;
        final MaterialEmitters.Emitter plasmaCore;
        final MaterialEmitters.Emitter tipDust;
        final MaterialEmitters.Emitter tipSparks;
        final MaterialEmitters.Emitter tipStreaks;
        final MaterialEmitters.Emitter shaftStreaks;
        float lastAge = 0.0f;
        long lastSeenGameTime = 0L;
        boolean started = false;

        EmissionRig(ClientLevel level, long seed) {
            this.level = level;
            this.seed = seed;
            int s = (int) seed;
            this.vapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(0xF7D774, 1.45f, s ^ 0x9A9, Vec3.ZERO, 0.035f),
                segmentSource(frame, 0.0, 1.0, 0.30), 250.0f, 330);
            this.plasmaHot = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xFF3829, 0.72f, s ^ 0xA11, Vec3.ZERO, 0.020f),
                segmentSource(frame, 0.0, 0.36, 0.55), 105.0f, 150);
            this.plasmaMid = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xD91414, 0.78f, s ^ 0xA22, Vec3.ZERO, 0.020f),
                segmentSource(frame, 0.33, 0.68, 0.55), 105.0f, 150);
            this.plasmaTip = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0x94141C, 0.72f, s ^ 0xA33, Vec3.ZERO, 0.020f),
                segmentSource(frame, 0.65, 1.0, 0.55), 95.0f, 140);
            this.plasmaCore = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xFF5940, 0.45f, s ^ 0xA44, Vec3.ZERO, 0.012f),
                segmentSource(frame, 0.0, 1.0, 0.65), 85.0f, 120);
            this.tipDust = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(0xB09A72, 0.90f, s ^ 0xD57, Vec3.ZERO, 0.16f),
                tipSource(frame), 70.0f, 100);
            this.tipSparks = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK, Vec3.ZERO, 0.30f),
                tipSource(frame), 55.0f, 90);
            // Complex particle layer: steel streak slivers at the tip,
            // at the standard continuous rate cap for class M.
            this.tipStreaks = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.18f),
                tipSource(frame), 28.0f, 40);
            // Attack upgrade: the same streak slivers shed along the
            // whole shaft at stream speed, so the pole's rush throws
            // sparks off its full length while it extends, not just
            // off the leading tip.
            this.shaftStreaks = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.16f),
                segmentSource(frame, 0.05, 1.0, 0.55), 26.0f, 40);
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
            vapor.tick(delta, envelope);
            plasmaHot.tick(delta, envelope);
            plasmaMid.tick(delta, envelope);
            plasmaTip.tick(delta, envelope);
            plasmaCore.tick(delta, envelope);
            tipDust.tick(delta, envelope * frame.fpScale);
            tipSparks.tick(delta, envelope);
            tipStreaks.tick(delta, envelope);
            shaftStreaks.tick(delta, envelope);
        }
    }
}
