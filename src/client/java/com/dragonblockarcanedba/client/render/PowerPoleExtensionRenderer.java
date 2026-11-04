package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
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
 * No-shapes conversion: this renderer draws no geometry. The extending
 * strike is carried by material particles sampled along the pole path:
 * pale vapor displaced by the rush, crimson plasma in three heat bands
 * along the stream, a brighter core stream, and dust plus steel sparks at
 * the leading tip. The pole item model itself remains a physical object
 * in the hand; this entity is the extension effect, so its path, length,
 * and timing are data for the emitters and nothing here renders a shaft,
 * sheath, collar, dome, plume, wake, or glint shape.
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
        }
    }
}
