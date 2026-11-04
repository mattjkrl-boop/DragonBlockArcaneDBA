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
 * Attack upgrade pass: layered composition, class M, worst offender
 * rebuild. The bolt's body is no longer a bare plasma ball carried by
 * two tendril ribbons: a solid casing of four interlocked gunmetal
 * plates (ComplexGeometry shards through submitSolidLit, so the
 * casing is lit like the physical matter it is) wraps a hot core
 * shard inset on the energy path, the only emissive part of the
 * body, and the whole casing spins slowly in flight. The wake is a
 * tendrilFan of four torn strands through submitEnergy with the
 * tendril canvas. The complex particle layer sheds SPARK_STREAK
 * sprites along the flight path and bursts them at the nose on the
 * impact transition, where the casing also breaks into a shardBurst
 * of five gunmetal fragments beside the burst. The no-shapes
 * emission stays as filler only around those pieces: a dense
 * cluster of ki plasma particles sampled along the real flight
 * path, with a white-hot plasma core near the nose, vapor shedding
 * off the tail, and steel sparks at the head. Emission runs through
 * persistent per-entity MaterialEmitters Emitter channels, so the
 * accumulator and per-second budgets survive across frames; the
 * channels rebuild only when the heat or size bucket changes.
 * Size, heat palette, pulse, and the impact collapse math are
 * unchanged; the collapse drives the casing, core, wake, and
 * fragment burst as well as emission density and particle scale.
 * The entity's own server trail, impact bursts, and DebrisSpawner
 * field are untouched.
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
        // Vertex counts at full spine samples: the casing is four
        // shards at 288 each (1152 per submit), the hot core inset is
        // one shard (288), the wake fan is four tendrils at 384 each
        // (1536), and the impact fragment burst is five shards (1440).
        // Every submit stays inside the class M budget of 2500.
        final Vec3 geoDir = state.direction;
        Vec3 sideTmp = geoDir.cross(new Vec3(0, 1, 0));
        final Vec3 geoSide = sideTmp.lengthSqr() < 1.0e-6 ? new Vec3(1, 0, 0) : sideTmp.normalize();
        final Vec3 geoUp = geoDir.cross(geoSide).normalize();
        final int geoBody = argb(r, g, b);
        final float geoEnv = boltDie;
        final float geoWidth = Math.max(0.16f, drawRadius * 1.05f);
        final long geoSeed = state.seed;
        final int geoLight = state.lightCoords;

        // Solid core: four interlocked gunmetal plates around the
        // flight axis, built like sabot petals on a real slug round:
        // short and wide rather than long needles, seated at the
        // diagonals on a shell orbit so only their edges overlap,
        // shingle-staggered along the body with the two length
        // classes alternating, and nearly parallel (a slight inward
        // pinch only) so they read as armor segments, not a flower.
        // Each plate rolls its own seeded silhouette. The whole
        // casing turns slowly with age. The casing is physical
        // gunmetal, drawn lit; it never glows itself.
        final float spin = age * 0.08f;
        final float casingLen = Math.max(0.10f, drawHead * 0.78f);
        final float casingGirth = Math.max(0.05f, drawRadius * 0.66f);
        final float plateOrbit = drawRadius * 0.46f;
        ComplexGeometry.submitSolidLit(collector, poseStack, geoLight, (pose, buffer) -> {
            for (int i = 0; i < 4; i++) {
                double ang = Math.PI * 0.25 + i * Math.PI * 0.5 + spin;
                Vec3 radial = geoSide.scale(Math.cos(ang)).add(geoUp.scale(Math.sin(ang)));
                Vec3 plateDir = geoDir.add(radial.scale(-0.12)).normalize();
                float lenI = casingLen * (i % 2 == 0 ? 1.0f : 0.90f);
                float stagger = casingLen * (i % 2 == 0 ? -0.06f : 0.02f);
                Vec3 plateOrigin = radial.scale(plateOrbit)
                    .add(geoDir.scale(-lenI * 0.42 + stagger));
                ComplexGeometry.shard(pose, buffer, geoSeed ^ (0xCA5EL + i * 104729L),
                    plateOrigin, plateDir, lenI, casingGirth,
                    0x23262B, 0x565D66, 0.98f, geoEnv);
            }
        });
        // Hot core inset: a short slug of the heat palette on the
        // energy path, mostly hidden inside the plate shell, glowing
        // through the seams and just emerging at the nose.
        ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) ->
            ComplexGeometry.shard(pose, buffer, geoSeed ^ 0xC04EL,
                geoDir.scale(-casingLen * 0.30), geoDir, casingLen * 0.80f,
                Math.max(0.04f, drawRadius * 0.36f),
                geoBody, 0xFFF7D1, 0.95f, geoEnv));
        // Wake: a fan of four torn strands streaming off the casing,
        // white-hot at the body and grading to the heat color at the
        // tail ends, replacing the old two loose ribbons.
        final Vec3 fanOrigin = geoDir.scale(-drawRadius * 0.40f);
        final Vec3 fanDir = geoDir.scale(-1.0);
        final float fanLength = Math.max(0.10f, drawTail * 0.95f);
        ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) ->
            ComplexGeometry.tendrilFan(pose, buffer, geoSeed ^ 0xFA57L,
                fanOrigin, fanDir, 46.0f, fanLength, 4, geoWidth * 0.55f,
                0xFFF7D1, geoBody, 0.92f, geoEnv, distScale));
        // Impact: the casing breaks into five gunmetal fragments in
        // a wide cone at the nose over the collapse window, growing
        // as the bolt dies, beside the SPARK_STREAK burst below. The
        // fragments are the same lit gunmetal as the casing, sized
        // as chunky broken bits rather than needles.
        if (state.impactAge >= 0) {
            final Vec3 burstOrigin = geoDir.scale(drawHead * 0.55f);
            final float burstUnit = Math.max(0.18f, drawRadius * 1.28f) * (0.65f + 0.60f * impactT);
            ComplexGeometry.submitSolidLit(collector, poseStack, geoLight, (pose, buffer) ->
                ComplexGeometry.shardBurst(pose, buffer, geoSeed ^ 0xF2A6L,
                    burstOrigin, geoDir, 135.0f, burstUnit, 5,
                    0x23262B, 0x565D66, 0.98f, geoEnv, distScale));
        }

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
            // Complex particle layer: spark streaks shed along the
            // whole flight path, at the standard continuous rate.
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.05f),
                bodyPath, 30f, 44),
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
