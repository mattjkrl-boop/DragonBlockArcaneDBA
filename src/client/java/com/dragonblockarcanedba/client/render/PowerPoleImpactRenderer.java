package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.entity.PowerPoleImpactEntity;
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
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Entity Renderer for Power Pole Impact in Minecraft 26.2.
 *
 * Complex pass: layered composition. The pieces layer is a small
 * ComplexGeometry crackFan at the impact, drawn on a copy of the pose
 * rotated so the fan plane is the strike plane (its normal is the
 * strike direction), hot pale gold at the center running out to
 * crimson at the tips, growing with the same shock expansion the
 * filler wash uses, through the dissolve shader. The complex particle
 * layer is a SHARD_CHIP burst in the opening burst. The material
 * filler stays as before: an opening burst of gold and crimson
 * plasma, pale vapor, impact dust, and steel sparks, followed by a
 * brief expanding wash of the same materials in the strike plane.
 *
 * Lifecycle math is unchanged from the bespoke renderer: the expansion
 * eases out over the first 8 ticks and the whole event fades with the
 * smoothstep of age over the entity lifetime.
 */
public class PowerPoleImpactRenderer extends EntityRenderer<PowerPoleImpactEntity, PowerPoleImpactRenderer.ImpactRenderState> {

    private static final Map<Integer, EmissionRig> RIGS = new HashMap<>();

    public PowerPoleImpactRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ImpactRenderState extends EntityRenderState {
        public float yRot = 0.0f;
        public float xRot = 0.0f;
        public float scale = 2.2f;
        public float age = 0.0f;
        public int maxLifetime = 14;
        public int entityId = -1;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(PowerPoleImpactEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(PowerPoleImpactEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ImpactRenderState createRenderState() {
        return new ImpactRenderState();
    }

    @Override
    public void extractRenderState(PowerPoleImpactEntity entity, ImpactRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getEntityYaw();
        state.xRot = entity.getEntityPitch();
        state.scale = entity.getImpactScale();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.entityId = entity.getId();
        // Material seed: hashed from the entity's synced state only
        // (scale, orientation), never from client random or wall clock.
        state.seed = (Float.floatToIntBits(entity.getImpactScale()) * 0x9E3779B97F4A7C15L)
            ^ Float.floatToIntBits(entity.getEntityYaw())
            ^ (Float.floatToIntBits(entity.getEntityPitch()) * 31L);
    }

    @Override
    public void submit(ImpactRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float age = state.age;
        float maxLife = (float) state.maxLifetime;
        float lifeT = maxLife <= 0.0f ? 1.0f : Math.min(1.0f, age / maxLife);
        float fade = 1.0f - lifeT * lifeT * (3.0f - 2.0f * lifeT);
        if (fade <= 0.01f) {
            RIGS.remove(state.entityId);
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        Vec3 origin = new Vec3(state.x, state.y, state.z);
        float distScale = MaterialEmitters.distanceScale(origin);
        if (distScale <= 0.01f) return;

        float rawProgress = Math.min(1.0f, age / 8.0f);
        float shockProgress = 1.0f - (1.0f - rawProgress) * (1.0f - rawProgress) * (1.0f - rawProgress);

        EmissionRig rig = rigFor(level, state);
        if (rig == null) return;
        float delta = rig.deltaFor(age);
        rig.frame.update(origin, direction(state.yRot, state.xRot), state.scale, shockProgress);
        if (!rig.burstDone) {
            rig.burst(distScale);
            rig.burstDone = true;
        }
        rig.tick(delta, fade * distScale);

        // Pieces layer: a small crack fan on the strike plane. The
        // crackFan builder draws in its local XZ plane, so the piece
        // rotates a copy of the submitted pose to carry that plane
        // onto the strike plane, exactly as the filler wash is framed.
        final Vec3 gForward = direction(state.yRot, state.xRot);
        final Vector3f gFwd = new Vector3f((float) gForward.x, (float) gForward.y, (float) gForward.z);
        final float gRadius = state.scale * (0.70f + 0.60f * shockProgress);
        final float gEnv = fade;
        final float gDist = distScale;
        final long gSeed = state.seed;
        ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) -> {
            PoseStack.Pose flat = pose.copy();
            flat.rotate(new Quaternionf().rotateTo(new Vector3f(0.0f, 1.0f, 0.0f), gFwd));
            ComplexGeometry.crackFan(flat, buffer, gSeed ^ 0xC9ACL, gRadius,
                0xFFF7D6, 0xE32636, 0.95f, gEnv, gDist);
        });

        if (age >= maxLife) {
            RIGS.remove(state.entityId);
        }
    }

    private static EmissionRig rigFor(ClientLevel level, ImpactRenderState state) {
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

    /** Mutable strike-plane frame read by the emitter sources at sample time. */
    private static final class Frame {
        Vec3 origin = Vec3.ZERO;
        Vec3 forward = new Vec3(0, 0, 1);
        Vec3 basisA = new Vec3(1, 0, 0);
        Vec3 basisB = new Vec3(0, 1, 0);
        float scale = 2.2f;
        float shockProgress = 0.0f;

        void update(Vec3 origin, Vec3 forward, float scale, float shockProgress) {
            this.origin = origin;
            this.forward = forward;
            this.scale = scale;
            this.shockProgress = shockProgress;
            Vec3 ref = Math.abs(forward.y) > 0.92 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            this.basisA = forward.cross(ref).normalize();
            this.basisB = forward.cross(this.basisA).normalize();
        }

        double outerRadius() {
            return scale * (0.3 + shockProgress * 1.5);
        }

        Vec3 radial(double angle) {
            return basisA.scale(Math.cos(angle)).add(basisB.scale(Math.sin(angle)));
        }
    }

    private static MaterialEmitters.Source planeSource(Frame frame) {
        return random -> {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = frame.outerRadius() * Math.sqrt(random.nextDouble());
            Vec3 radial = frame.radial(angle);
            Vec3 pos = frame.origin.add(radial.scale(radius))
                .add(frame.forward.scale((random.nextDouble() - 0.5) * 0.12 * frame.scale));
            Vec3 vel = radial.scale(0.25 + frame.shockProgress * 0.55)
                .add(frame.forward.scale(0.08));
            return new MaterialEmitters.Sample(pos, vel);
        };
    }

    private static MaterialEmitters.Source centerSource(Frame frame) {
        return random -> new MaterialEmitters.Sample(frame.origin, frame.forward.scale(0.10));
    }

    private static final class EmissionRig {
        final ClientLevel level;
        final long seed;
        final Frame frame = new Frame();
        final MaterialEmitters.Emitter vapor;
        final MaterialEmitters.Emitter dust;
        final MaterialEmitters.Emitter plasma;
        final MaterialEmitters.Emitter sparks;
        float lastAge = 0.0f;
        long lastSeenGameTime = 0L;
        boolean started = false;
        boolean burstDone = false;

        EmissionRig(ClientLevel level, long seed) {
            this.level = level;
            this.seed = seed;
            int s = (int) seed;
            this.vapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(0xFFF3C4, 1.30f, s ^ 0x9A9, Vec3.ZERO, 0.030f),
                planeSource(frame), 150.0f, 200);
            this.dust = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(0xA78B5F, 1.00f, s ^ 0xD057, Vec3.ZERO, 0.045f),
                planeSource(frame), 110.0f, 160);
            this.plasma = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xFFC24D, 0.65f, s ^ 0x914, Vec3.ZERO, 0.020f),
                planeSource(frame), 120.0f, 180);
            this.sparks = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK, Vec3.ZERO, 0.42f),
                centerSource(frame), 40.0f, 70);
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

        void burst(float distScale) {
            int s = (int) seed;
            float scaleFactor = Math.max(0.5f, frame.scale / 2.2f);
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.plasma(0xFFC24D, 0.90f * scaleFactor, s ^ 0xB001, frame.forward.scale(0.15), 0.28f),
                MaterialEmitters.sphereVolume(frame.origin, 0.18 * frame.scale),
                Math.round(22 * distScale));
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.plasma(0xE32636, 0.80f * scaleFactor, s ^ 0xB002, frame.forward.scale(0.10), 0.24f),
                planeSource(frame), Math.round(14 * distScale));
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.vapor(0xFFF3C4, 1.50f * scaleFactor, s ^ 0xB003, Vec3.ZERO, 0.10f),
                MaterialEmitters.sphereVolume(frame.origin, 0.22 * frame.scale),
                Math.round(16 * distScale));
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.dust(0xA78B5F, 1.10f * scaleFactor, s ^ 0xB004, Vec3.ZERO, 0.16f),
                planeSource(frame), Math.round(18 * distScale));
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK, frame.forward.scale(0.20), 0.50f),
                MaterialEmitters.sphereVolume(frame.origin, 0.12 * frame.scale),
                Math.round(16 * distScale));
            // Complex particle layer: hard edged chip fragments thrown
            // off the strike point, inside the class M burst budget.
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP, frame.forward.scale(0.18), 0.38f),
                MaterialEmitters.sphereVolume(frame.origin, 0.12 * frame.scale),
                Math.round(18 * distScale));
        }

        void tick(float delta, float envelope) {
            if (delta <= 0.0f || envelope <= 0.01f) return;
            vapor.tick(delta, envelope);
            dust.tick(delta, envelope);
            plasma.tick(delta, envelope);
            sparks.tick(delta, envelope);
        }
    }
}
