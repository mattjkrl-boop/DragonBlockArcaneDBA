package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.GrandCrescentWaveEntity;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Entity Renderer for Grand Crescent Wave in Minecraft 26.2.
 * Attack upgrade pass: the wave has a physical body now. A low arc of
 * six steel-and-stone rockWall slabs (submitSolidLit, world lit) rides
 * the leading edge as a pushing front, bowed on the same arc as the
 * energy skin (negative rockWall arc: the builder's positive arc swings
 * the slab ends toward facing, so the convex crescent bow, center
 * ahead, is the negative value, with the wall origin pushed forward to
 * the skin's bow apex). Two thin gold seams cross the slab faces as
 * separate submitEnergy tendrils (the only energy on the front; the
 * slab bodies stay lit). An escort of six valor shards (shardBurst,
 * submitSolidLit) orbits the front, and a flameTongues row burns along
 * the front's top edge. The two complex-redo tendrils stay as the
 * wave's energy skin over the wall. SPARK_STREAK sprites shed at the
 * rim and SHARD_CHIP and dust shed off the wall face form the sprite
 * layers. The existing material emission stays as filler: valor plasma
 * sampled across the real travel volume, pale vapor trailing behind,
 * steel sparks inside the front, and a brighter plasma surge during
 * the opening strike window. Piece budget at distance scale 1.0 is now
 * class L (about 4100 vertices: skin tendrils 770, wall 432, seams
 * 440, escort shards 1728, flame row 720), up from class M. Per
 * Carter's steering the front favors fewer, bigger, uneven pieces:
 * the wall runs at the builder's minimum slab count for the span (six
 * big slabs with their seeded height, tilt, and lean variance), the
 * escort is six large shards on clustered, unevenly spaced orbit
 * anchors, the flame row's stations jitter in angle and height, and
 * the upper gold seam is a short offset inset, not a second even
 * band. The wall rises on the wave's existing open factor and sinks
 * on the existing hit beat; its life, hit fade, damage, and travel
 * behavior are unchanged.
 */
public class GrandCrescentWaveRenderer extends EntityRenderer<GrandCrescentWaveEntity, GrandCrescentWaveRenderer.GrandCrescentWaveRenderState> {

    private final Map<String, Stream> streams = new HashMap<>();

    public GrandCrescentWaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class GrandCrescentWaveRenderState extends EntityRenderState {
        public float yRot = 0;
        public float xRot = 0;
        public float ageInTicks = 0;
        public int hitTick = -1;
        public long seed = 0;
        public int entityId = -1;
        public boolean isFirstPersonOwner = false;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(GrandCrescentWaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(GrandCrescentWaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public GrandCrescentWaveRenderState createRenderState() {
        return new GrandCrescentWaveRenderState();
    }

    @Override
    public void extractRenderState(GrandCrescentWaveEntity entity, GrandCrescentWaveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.ageInTicks = entity.tickCount + partialTicks;
        state.hitTick = entity.getHitTick();
        state.seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();
        state.entityId = entity.getId();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null &&
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) &&
            mc.options.getCameraType().isFirstPerson());
    }

    @Override
    public void submit(GrandCrescentWaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float maxLife = GrandCrescentWaveEntity.MAX_LIFE_TICKS;
        float progress = Math.min(1.0f, state.ageInTicks / maxLife);
        float fade = com.dragonblockarcanedba.util.FxEnvelope.envelope(
            state.ageInTicks, maxLife, 0.04f, 0.35f);
        if (state.hitTick >= 0) {
            float hitFade = 1.0f - smooth(Math.min(1.0f,
                (state.ageInTicks - state.hitTick) / (float) GrandCrescentWaveEntity.DISSIPATE_TICKS));
            fade = Math.min(fade, hitFade);
        }
        float open = smooth(Math.min(1.0f, state.ageInTicks / 12.0f));
        float impact = strike(progress);

        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && fade > 0.01f) {
            float fpScale = (state.isFirstPersonOwner && state.ageInTicks < 6.0f)
                ? (0.55f + (state.ageInTicks / 6.0f) * 0.45f)
                : 1.0f;
            final float span = 4.8f * fpScale * (0.80f + 0.20f * open);
            final float chord = 1.55f * fpScale * (0.80f + 0.20f * open);
            final Vec3 center = new Vec3(state.x, state.y, state.z);
            float distScale = MaterialEmitters.distanceScale(center);

            double yaw = Math.toRadians(state.yRot);
            double pitch = Math.toRadians(state.xRot);
            final Vec3 forward = new Vec3(
                -Math.sin(yaw) * Math.cos(pitch),
                -Math.sin(pitch),
                Math.cos(yaw) * Math.cos(pitch)).normalize();
            final Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
            final Vec3 up = right.cross(forward).normalize();

            MaterialEmitters.Source frontSource = random -> sampleFront(random, center, right, up, forward, span, chord, 0.0);
            MaterialEmitters.Source wallSource = random -> sampleFront(random, center, right, up, forward, span, chord, 0.35);
            MaterialEmitters.Source trailSource = random -> sampleFront(random, center, right, up, forward, span, chord, -1.05);
            String base = state.entityId + ":";
            int seed = (int) state.seed;

            emit(level, base + "plasma",
                MaterialEmitters.Preset.plasma(0xFFD833, 1.65f, seed,
                    forward.scale(0.035), 0.035f),
                frontSource, 430f, 520, state.ageInTicks, fade * distScale);
            emit(level, base + "vapor",
                MaterialEmitters.Preset.vapor(0xFFE9A8, 3.5f, seed ^ 0x9A90,
                    forward.scale(-0.020), 0.025f),
                trailSource, 96f, 136, state.ageInTicks, fade * distScale * 0.74f);
            emit(level, base + "sparks",
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK, forward.scale(0.05), 0.10f),
                frontSource, 118f, 154, state.ageInTicks, fade * distScale * 0.90f);
            if (impact > 0.02f) {
                emit(level, base + "strike",
                    MaterialEmitters.Preset.plasma(0xFFFFFF, 2.05f, seed ^ 0x57A1E,
                        forward.scale(0.06), 0.06f),
                    frontSource, 265f, 300, state.ageInTicks, impact * distScale);
            }

            // Complex sprite layer: spark streaks shed at the front rim.
            emit(level, base + "streak",
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, forward.scale(0.05), 0.10f),
                frontSource, 30f, 40, state.ageInTicks, fade * distScale * 0.90f);

            // The pushing front sheds as it travels: shard chips and
            // stone dust off the wall face (sampled at the wall's depth,
            // ahead of the wave center).
            emit(level, base + "wallchip",
                MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP, forward.scale(-0.04), 0.12f),
                wallSource, 85f, 120, state.ageInTicks, fade * distScale * 0.85f);
            emit(level, base + "walldust",
                MaterialEmitters.Preset.dust(0x8A7D68, 1.30f, seed ^ 0xD057,
                    forward.scale(-0.025), 0.09f),
                wallSource, 65f, 100, state.ageInTicks, fade * distScale * 0.80f);

            // Pieces layer: the crescent body itself. One thick tendril
            // whose spine runs tip to tip across the span, bowed forward
            // and up through its control point, plus a thinner echo
            // tendril trailing below and behind it. Endpoints sag below
            // center and the middle rides high, so the strand reads as a
            // crescent arc rather than a straight beam.
            final float arcSpan = span;
            final float arcChord = chord;
            final float waveFade = fade;
            final float waveFpScale = fpScale;
            // The pushing wall sinks on the existing hit beat, in step
            // with the hit fade computed above.
            final float wallSink = state.hitTick >= 0
                ? smooth(Math.min(1.0f,
                    (state.ageInTicks - state.hitTick) / (float) GrandCrescentWaveEntity.DISSIPATE_TICKS))
                : 0.0f;
            ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) ->
                ComplexGeometry.tendril(pose, buffer, state.seed ^ 0xA2CL,
                    right.scale(-arcSpan * 0.52).add(up.scale(-arcChord * 0.28)),
                    forward.scale(0.95).add(up.scale(arcChord * 0.52)),
                    right.scale(arcSpan * 0.52).add(up.scale(-arcChord * 0.28)),
                    0.58f * waveFpScale, 0xFFD833, 0xFFF3B0, 0.95f, waveFade, distScale));
            ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) ->
                ComplexGeometry.tendril(pose, buffer, state.seed ^ 0xEC40L,
                    right.scale(-arcSpan * 0.44).add(up.scale(-arcChord * 0.52)).add(forward.scale(-0.42)),
                    forward.scale(0.45).add(up.scale(arcChord * 0.05)),
                    right.scale(arcSpan * 0.44).add(up.scale(-arcChord * 0.52)).add(forward.scale(-0.42)),
                    0.30f * waveFpScale, 0xFFD833, 0xFFE9A8, 0.88f, waveFade, distScale));

            // Pushing front: a low rockWall arc of steel-and-stone slabs
            // riding the leading edge. The skin's spine bows 0.475 ahead
            // of its chord (bezier control at forward 0.95), so the wall
            // origin sits at that apex and the wall arc is negative
            // (builder positive arc swings the ends toward facing; the
            // crescent wants its center ahead). Length is the wave span
            // (six slabs at full span), height is low against the chord.
            // The wall rises on the wave's open factor and sinks on the
            // hit beat; the slabs lean on their seeded builder lean.
            // Physical bodies: submitSolidLit only.
            final Vec3 wallOrigin = forward.scale(0.475).add(up.scale(-0.20f * arcChord));
            ComplexGeometry.submitSolidLit(collector, poseStack, state.lightCoords, (pose, buffer) ->
                ComplexGeometry.rockWall(pose, buffer, state.seed ^ 0x9A11L,
                    wallOrigin, forward, arcSpan, 0.55f * arcChord, -0.8f,
                    open, wallSink,
                    0x3F3A33, 0x8B939C, 0.97f, waveFade, distScale));

            // Gold insets: thin seams across the slab faces, the only
            // energy on the front, drawn as separate submitEnergy
            // tendrils floating just proud of the proudest slab faces
            // (builder face offset plus bulge tops out at 0.21). One
            // seam runs the arc at mid height; the second is a short,
            // offset chunk higher up, so the insets read as set into
            // the stone, not as even bands.
            final float arcR = arcSpan / 0.8f;
            ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
                goldSeam(pose, buffer, state.seed ^ 0x5EAAL,
                    right, up, forward, arcR, -0.34f, 0.34f, 0.05f, 0.24f, 0.06f, 0.90f, waveFade, distScale * 0.55f);
                goldSeam(pose, buffer, state.seed ^ 0x5EABL,
                    right, up, forward, arcR, -0.17f, 0.27f, 0.33f * waveFpScale, 0.24f, 0.045f, 0.70f, waveFade, distScale * 0.55f);
            });

            // Escort: six large valor shards orbiting the front on an
            // ellipse in the plane of the wave, each leaning into the
            // travel direction. The anchors are clustered, not evenly
            // spaced (two close pairs and two singles), so the escort
            // reads as a heaving cluster, not a parade ring. A single
            // shardBurst is a cone from one origin and cannot orbit,
            // so the escort is six single-shard shardBurst calls at
            // orbiting anchors (same builder, same shard meshes,
            // per-shard seeds keep each silhouette stable while its
            // anchor travels).
            final float orbitAng = state.ageInTicks * 0.21f;
            final double[] orbitOffsets = {0.0, 0.75, 2.35, 3.05, 4.35, 5.45};
            final Vec3 orbitCenter = forward.scale(0.35).add(up.scale(-0.02f * arcChord));
            final float orbitRx = arcSpan * 0.34f;
            final float orbitRy = arcChord * 0.50f;
            final float escortEnv = waveFade * (0.35f + 0.65f * open);
            if (escortEnv > 0.01f) {
                ComplexGeometry.submitSolidLit(collector, poseStack, state.lightCoords, (pose, buffer) -> {
                    for (int i = 0; i < orbitOffsets.length; i++) {
                        double a = orbitAng + orbitOffsets[i];
                        Vec3 anchor = orbitCenter
                            .add(right.scale(Math.cos(a) * orbitRx))
                            .add(up.scale(Math.sin(a) * orbitRy));
                        Vec3 tangent = right.scale(-Math.sin(a) * orbitRx)
                            .add(up.scale(Math.cos(a) * orbitRy));
                        Vec3 dirI = tangent.add(forward.scale(0.55 * Math.max(orbitRx, orbitRy))).normalize();
                        ComplexGeometry.shardBurst(pose, buffer, state.seed ^ (0xE5C0L + i * 131L),
                            anchor, dirI, 18.0f, 1.15f * waveFpScale, 1,
                            0x767F8C, 0xFFD833, 0.95f, escortEnv, distScale);
                    }
                });
            }

            // Flame row along the front's top edge: five small tongue
            // clusters stationed on the wall arc at its top lip,
            // burning in the wave's valor gold. Station angles and
            // heights jitter per station so the burning lip is jagged,
            // not a uniform row.
            final float flameSway = state.ageInTicks * 0.55f;
            final float[] flameAngs = {-0.33f, -0.15f, 0.02f, 0.17f, 0.33f};
            final float[] flameHeights = {0.66f, 0.46f, 0.74f, 0.52f, 0.60f};
            ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.FLAME_TONGUE, (pose, buffer) -> {
                for (int i = 0; i < flameAngs.length; i++) {
                    Vec3 baseP = arcPoint(right, forward, arcR, flameAngs[i])
                        .add(up.scale(0.30f * arcChord))
                        .add(forward.scale(0.10f));
                    ComplexGeometry.flameTongues(pose, buffer, state.seed ^ (0xF1A9L + i * 131L),
                        baseP, 0.32f * waveFpScale, flameHeights[i] * waveFpScale, 3,
                        0xFFF3B0, 0xFFD833, 0.90f, waveFade, flameSway + i * 1.93f, distScale);
                }
            });
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    /**
     * A point on the pushing wall's arc at the given arc angle: the
     * wall circle has radius bigR (span over arc), its center slab sits
     * at the skin's bow apex (forward 0.475), and the ends fall back by
     * bigR times (1 minus cos), matching the rockWall placement math
     * for the negative arc used in submit.
     */
    private static Vec3 arcPoint(Vec3 right, Vec3 forward, float bigR, float ang) {
        return right.scale(bigR * Math.sin(ang))
            .add(forward.scale(0.475f - bigR * (1.0 - Math.cos(ang))));
    }

    /**
     * One thin gold seam tendril across the slab faces: a shallow arc
     * following the wall circle between the given arc angles, at the
     * given height above the wave center, floated fwdOff ahead of the
     * slab center plane so it reads as an inset energy line on the
     * stone. The control point sits at the span's midpoint angle.
     */
    private static int goldSeam(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                                Vec3 right, Vec3 up, Vec3 forward, float bigR,
                                float angFrom, float angTo,
                                float upOff, float fwdOff, float width,
                                float alpha, float envelope, float distScale) {
        Vec3 from = arcPoint(right, forward, bigR, angFrom).add(up.scale(upOff)).add(forward.scale(fwdOff));
        Vec3 control = arcPoint(right, forward, bigR, (angFrom + angTo) * 0.5f).add(up.scale(upOff + 0.02f)).add(forward.scale(fwdOff));
        Vec3 to = arcPoint(right, forward, bigR, angTo).add(up.scale(upOff)).add(forward.scale(fwdOff));
        return ComplexGeometry.tendril(pose, buffer, seed, from, control, to, width,
            0xFFD833, 0xFFF3B0, alpha, envelope, distScale);
    }

    private static MaterialEmitters.Sample sampleFront(RandomSource random, Vec3 center, Vec3 right,
                                                       Vec3 up, Vec3 forward, float span, float chord,
                                                       double backOffset) {
        double u = (random.nextDouble() + random.nextDouble()) - 1.0;
        double v = (random.nextDouble() + random.nextDouble()) - 1.0;
        double depth = (random.nextDouble() - 0.5) * 0.55 + backOffset;
        Vec3 pos = center
            .add(right.scale(u * span * 0.52))
            .add(up.scale(v * chord * 0.64))
            .add(forward.scale(depth));
        return new MaterialEmitters.Sample(pos, forward.scale(0.04));
    }

    private static float smooth(float t) {
        float x = Math.max(0.0f, Math.min(1.0f, t));
        return x * x * (3.0f - 2.0f * x);
    }

    private static float strike(float progress) {
        if (progress >= 0.16f) return 0.0f;
        return (float) Math.sin((progress / 0.16f) * Math.PI);
    }

    private void emit(ClientLevel level, String key, MaterialEmitters.Preset preset,
                      MaterialEmitters.Source source, float ratePerSecond, int budgetPerSecond,
                      float age, float envelope) {
        if (envelope <= 0.01f) return;
        long now = System.nanoTime();
        streams.entrySet().removeIf(entry -> now - entry.getValue().lastSeenNanos > 30_000_000_000L);
        Stream stream = streams.computeIfAbsent(key, ignored -> new Stream());
        if (stream.emitter == null || stream.level != level) {
            stream.emitter = new MaterialEmitters.Emitter(level, preset, stream.source, ratePerSecond, budgetPerSecond);
            stream.level = level;
            stream.lastAge = Float.NaN;
        }
        stream.source.delegate = source;
        float delta = Float.isNaN(stream.lastAge) || age < stream.lastAge
            ? 1.0f / 60.0f
            : Math.min(0.10f, Math.max(0.0f, (age - stream.lastAge) / 20.0f));
        stream.lastAge = age;
        stream.lastSeenNanos = now;
        stream.emitter.tick(delta, envelope);
    }

    private static final class Stream {
        private final MutableSource source = new MutableSource();
        private MaterialEmitters.Emitter emitter;
        private ClientLevel level;
        private float lastAge = Float.NaN;
        private long lastSeenNanos;
    }

    private static final class MutableSource implements MaterialEmitters.Source {
        private MaterialEmitters.Source delegate = MaterialEmitters.point(Vec3.ZERO);

        @Override
        public MaterialEmitters.Sample sample(RandomSource random) {
            return delegate.sample(random);
        }
    }
}
