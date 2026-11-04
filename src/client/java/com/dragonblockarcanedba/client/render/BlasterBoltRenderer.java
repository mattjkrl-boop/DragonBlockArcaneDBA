package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.BlasterBoltEntity;
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
 * Entity Renderer for Blaster Bolt in MC 26.2.
 * Complex pass: layered composition, class M. The pieces layer is a
 * short tendril wake behind the bolt: two torn ComplexGeometry strands
 * through submitEnergy with the tendril canvas, a main strand in the
 * heat palette running from the tail to a near-white tip at the nose,
 * and a thinner offset strand as its torn edge, both collapsing with
 * the bolt on impact. The complex particle layer is a SPARK_STREAK
 * burst at the nose on the impact transition. The no-shapes emission
 * stays as filler only: a dense cluster of ki plasma particles sampled
 * along the real flight path, with a white-hot plasma core near the
 * nose, vapor shedding off the tail, and steel sparks at the head.
 * Emission runs through persistent per-entity MaterialEmitters
 * Emitter channels, so the accumulator and per-second budgets survive
 * across frames; the channels rebuild only when the heat or size
 * bucket changes. Size, heat palette, pulse, and the impact collapse
 * math are unchanged; the collapse drives the tendril lengths and
 * widths as well as emission density and particle scale. The entity's
 * own server trail, impact bursts, and DebrisSpawner field are
 * untouched.
 */
public class BlasterBoltRenderer extends EntityRenderer<BlasterBoltEntity, BlasterBoltRenderer.BlasterBoltRenderState> {
    private static final Map<Integer, Emission> EMISSIONS = new ConcurrentHashMap<>();

    public BlasterBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class BlasterBoltRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public boolean isOvercharged = false;
        public float heatRatio = 0.0f;
        public float age = 0.0f;
        public int impactAge = -1;
        public int seed = 0;
        public boolean isFirstPersonOwner = false;
        public int entityId = -1;
        public Vec3 origin = Vec3.ZERO;
        public Vec3 direction = new Vec3(0, 0, 1);
    }

    /** Per-entity emission channels plus the live frame they sample from. */
    private static final class Emission {
        float lastAge = Float.NaN;
        int lastImpactAge = -1;
        String signature = "";
        MaterialEmitters.Emitter[] emitters = new MaterialEmitters.Emitter[0];
        Vec3 center = Vec3.ZERO;
        Vec3 dir = new Vec3(0, 0, 1);
        float drawHead = 0.0f;
        float drawTail = 0.0f;
        float drawRadius = 0.0f;
    }

    @Override
    public BlasterBoltRenderState createRenderState() {
        return new BlasterBoltRenderState();
    }

    @Override
    public void extractRenderState(BlasterBoltEntity entity, BlasterBoltRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.isOvercharged = entity.isOvercharged();
        state.heatRatio = entity.getHeatRatio();
        state.age = entity.tickCount + partialTicks;
        state.impactAge = entity.getImpactAge();
        state.seed = entity.getSeed();
        state.entityId = entity.getId();
        state.origin = entity.getPosition(partialTicks);
        Vec3 look = entity.getLookAngle();
        state.direction = look.lengthSqr() > 1.0e-6 ? look.normalize() : new Vec3(0, 0, 1);

        Minecraft mc = Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null &&
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) &&
            mc.options.getCameraType().isFirstPerson());
    }

    @Override
    public void submit(BlasterBoltRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        float age = state.age;
        float pulse = 1.0f + 0.08f * (float) Math.sin(age * 2.5f);

        boolean overcharged = state.isOvercharged;
        float heat = state.heatRatio;

        float fpScale = state.isFirstPersonOwner ? 0.60f : 1.0f;
        float radius = (overcharged ? 0.36f * pulse : (0.18f + heat * 0.12f) * pulse) * fpScale;
        float headLength = (overcharged ? 1.4f : (0.85f + heat * 0.35f)) * fpScale;
        float tailLength = (overcharged ? 3.4f : (2.0f + heat * 1.0f)) * fpScale;

        // Color palette based on heat / overcharge
        float r, g, b;
        if (overcharged) {
            r = 1.0f;
            g = 0.12f;
            b = 0.32f;
        } else if (heat > 0.6f) {
            r = 1.0f;
            g = 0.45f;
            b = 0.05f;
        } else {
            r = 1.0f;
            g = 0.88f - heat * 0.30f;
            b = 0.10f;
        }

        // On impact the same bolt collapses and fades. It does not swap for a disc.
        final float impactT = state.impactAge < 0 ? 0.0f : Math.min(1.0f, state.impactAge / 5.0f);
        final float impactEase = 1.0f - (1.0f - impactT) * (1.0f - impactT) * (1.0f - impactT);
        final float boltDie = 1.0f - impactT * impactT * (3.0f - 2.0f * impactT);
        final float drawRadius = radius * (1.0f - impactEase * 0.62f);
        final float drawHead = headLength * (1.0f - impactEase * 0.70f);
        final float drawTail = tailLength * (1.0f - impactEase * 0.20f);

        float distScale = MaterialEmitters.distanceScale(state.origin);
        float envelope = boltDie * distScale * (state.isFirstPersonOwner ? 0.75f : 1.0f);
        if (envelope <= 0.01f) return;

        Emission em = EMISSIONS.computeIfAbsent(state.entityId, id -> new Emission());
        em.center = state.origin;
        em.dir = state.direction;
        em.drawHead = drawHead;
        em.drawTail = drawTail;
        em.drawRadius = drawRadius;

        // Pieces layer, in entity local space (Vec3.ZERO is the bolt
        // center, the same frame the pose stack is translated into).
        // Two tendrils at full spine samples is 768 vertices, inside
        // the class M budget of 2500.
        final Vec3 geoDir = state.direction;
        Vec3 sideTmp = geoDir.cross(new Vec3(0, 1, 0));
        final Vec3 geoSide = sideTmp.lengthSqr() < 1.0e-6 ? new Vec3(1, 0, 0) : sideTmp.normalize();
        final Vec3 geoUp = geoDir.cross(geoSide).normalize();
        final int geoBody = argb(r, g, b);
        final float geoEnv = boltDie;
        final float geoWidth = Math.max(0.16f, drawRadius * 1.05f);
        final Vec3 wakeFrom = geoDir.scale(-drawTail * 0.90);
        final Vec3 wakeControl = geoDir.scale((drawHead * 0.50f - drawTail * 0.90f) * 0.5f)
            .add(geoSide.scale(drawRadius * 0.55f));
        final Vec3 wakeTo = geoDir.scale(drawHead * 0.50f);
        final Vec3 edgeFrom = geoDir.scale(-drawTail * 0.75).add(geoSide.scale(drawRadius * 0.45f));
        final Vec3 edgeControl = geoDir.scale(-drawTail * 0.10f).add(geoUp.scale(-drawRadius * 0.60f));
        final Vec3 edgeTo = geoDir.scale(drawHead * 0.35f).add(geoSide.scale(-drawRadius * 0.30f));
        final long geoSeed = state.seed;
        ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
            ComplexGeometry.tendril(pose, buffer, geoSeed ^ 0x7A11L,
                wakeFrom, wakeControl, wakeTo, geoWidth,
                geoBody, 0xFFF7D1, 0.95f, geoEnv, distScale);
            ComplexGeometry.tendril(pose, buffer, geoSeed ^ 0xED6EL,
                edgeFrom, edgeControl, edgeTo, geoWidth * 0.55f,
                geoBody, 0xFFF7D1, 0.88f, geoEnv, distScale);
        });

        // Complex particle layer: a SPARK_STREAK burst at the nose on
        // the frame the synced impact age first turns non-negative,
        // 18 to 26 sprites inside the class M burst budget of 30. The
        // entity's own server side impact field stays untouched.
        if (state.impactAge >= 0 && em.lastImpactAge < 0) {
            Vec3 head = state.origin.add(state.direction.scale(drawHead * 0.55f));
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.30f),
                MaterialEmitters.sphereVolume(head, Math.max(0.10, drawRadius * 0.90)),
                overcharged ? 26 : 18);
        }
        em.lastImpactAge = state.impactAge;

        float delta;
        if (Float.isNaN(em.lastAge) || age < em.lastAge) {
            delta = 1.0f / 60.0f;
        } else {
            delta = Math.min(0.10f, Math.max(0.0f, (age - em.lastAge) / 20.0f));
        }
        em.lastAge = age;
        if (delta <= 0f) return;

        int bodyColor = argb(r, g, b);
        String signature = System.identityHashCode(level) + "|" + bodyColor + "|"
            + Math.round(drawRadius * 16.0f) + "|" + overcharged;
        if (!signature.equals(em.signature)) {
            em.signature = signature;
            em.emitters = buildEmitters(level, em, state.seed, bodyColor, drawRadius, overcharged);
        }
        for (MaterialEmitters.Emitter emitter : em.emitters) {
            emitter.tick(delta, envelope);
        }
        if (EMISSIONS.size() > 256) EMISSIONS.clear();
    }

    private static MaterialEmitters.Emitter[] buildEmitters(ClientLevel level, Emission em, int seed,
                                                            int bodyColor, float radius, boolean overcharged) {
        MaterialEmitters.Source bodyPath = random -> MaterialEmitters.path(
            em.center.add(em.dir.scale(-em.drawTail * 0.90)),
            em.center.add(em.dir.scale(em.drawHead * 0.65)), 0.0).sample(random);
        MaterialEmitters.Source corePath = random -> MaterialEmitters.path(
            em.center.add(em.dir.scale(-em.drawTail * 0.45)),
            em.center.add(em.dir.scale(em.drawHead * 0.50)), 0.0).sample(random);
        MaterialEmitters.Source noseVolume = random -> MaterialEmitters.sphereVolume(
            em.center.add(em.dir.scale(em.drawHead * 0.55)),
            Math.max(0.05, em.drawRadius * 0.75)).sample(random);
        MaterialEmitters.Source tailPath = random -> MaterialEmitters.path(
            em.center.add(em.dir.scale(-em.drawTail)),
            em.center.add(em.dir.scale(-em.drawTail * 0.35)), 0.0).sample(random);

        return new MaterialEmitters.Emitter[] {
            // Body: ki plasma along the whole flight segment.
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(bodyColor, Math.max(0.6f, radius * 11.0f),
                    seed ^ 0xB0D1, Vec3.ZERO, radius * 0.05f),
                bodyPath, overcharged ? 340f : 260f, 420),
            // Core: a shorter, brighter plasma run through the middle.
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xFFF7D1, Math.max(0.4f, radius * 6.5f),
                    seed ^ 0xC02E, Vec3.ZERO, radius * 0.03f),
                corePath, 170f, 240),
            // Nose cluster: plasma bunched where the cone used to be.
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(bodyColor, Math.max(0.5f, radius * 8.0f),
                    seed ^ 0x905E, Vec3.ZERO, radius * 0.04f),
                noseVolume, 110f, 160),
            // Steel sparks thrown off the head.
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK, new Vec3(0, 0, 0), 0.10f),
                noseVolume, 65f, 100),
            // Tail wake: heat vapor drifting backward off the tail end.
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(bodyColor, Math.max(0.8f, radius * 14.0f),
                    seed ^ 0x7A11, new Vec3(0, 0, 0), 0.025f),
                tailPath, 60f, 90)
        };
    }

    private static int argb(float r, float g, float b) {
        int ri = Math.max(0, Math.min(255, Math.round(r * 255f)));
        int gi = Math.max(0, Math.min(255, Math.round(g * 255f)));
        int bi = Math.max(0, Math.min(255, Math.round(b * 255f)));
        return (ri << 16) | (gi << 8) | bi;
    }
}
