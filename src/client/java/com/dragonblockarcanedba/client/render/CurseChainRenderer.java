package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.CurseChainEntity;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Curse Chain as a layered composition. The pieces layer is a paired
 * chain: two taut tendrils run along the chain path itself, quadratic
 * beziers through the first, middle, and last link centers, the second
 * strand offset sideways and bowed lower so the pair reads as one
 * braided chain, so on the bind arc it follows the arc from one side
 * of the target to the other and in flight it runs straight down the
 * trail, with the tendril builder's own mid-spine curl kept small by
 * the short chord. While the chain is bound, three curse glass shards
 * crystallize at the anchor point at the bind. It is drawn under the
 * same scale, yaw, and pitch transform the old link geometry used,
 * on the tendril canvas through the shader. The complex particle
 * layer is spark streaks shed at the link centers. The material emission stays as filler: the same seven
 * link centers on the bind arc and six on the flight trail, transformed
 * into world space, with darkness smoke and curse plasma emitted along the
 * segments between them and soul wisps gathering at the centers, glyph
 * points, and core point. The collar is a ring of sampled emission points
 * at the bind, the attach snap is a material burst, and the helical aura is
 * two polylines of plasma emission. Fade in over four ticks and the synced
 * twelve tick fade out are unchanged, as are all seeds.
 */
public class CurseChainRenderer extends EntityRenderer<CurseChainEntity, CurseChainRenderer.CurseChainRenderState> {

    public CurseChainRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class CurseChainRenderState extends EntityRenderState {
        public float ageInTicks;
        public boolean isAttached;
        public int orbitIndex;
        public float yRot;
        public float xRot;
        public float attachRemaining = -1;
        public long seed = 0;
        public float lastEmitAge = -1f;
    }

    @Override
    public CurseChainRenderState createRenderState() {
        return new CurseChainRenderState();
    }

    @Override
    public void extractRenderState(CurseChainEntity entity, CurseChainRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.ageInTicks = entity.tickCount + partialTicks;
        state.isAttached = entity.isAttached();
        state.attachRemaining = entity.getAttachRemaining();
        state.orbitIndex = entity.getOrbitIndex();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.seed = entity.getId() * 0x9E3779B97F4A7C15L;
    }

    @Override
    public void submit(CurseChainRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        float age = state.ageInTicks;
        boolean attached = state.isAttached;
        int orbit = state.orbitIndex;
        int seed = (int) (state.seed ^ (state.seed >>> 32));
        float stretch = ease(Math.min(1.0f, age / 6.0f));

        float fadeIn = FxEnvelope.smoothstep(Math.min(1.0f, age / 4.0f));
        float fadeOut = 1.0f;
        if (attached && state.attachRemaining >= 0) {
            fadeOut = FxEnvelope.smoothstep(Math.min(1.0f, state.attachRemaining / 12.0f));
        }
        float alphaMul = fadeIn * fadeOut;
        if (alphaMul <= 0.01f) return;

        Vec3 origin = new Vec3(state.x, state.y, state.z);
        float ds = MaterialEmitters.distanceScale(origin);
        float env = alphaMul * ds;
        float delta = emissionDelta(state, age);
        float pulse = 0.85f + 0.15f * (float) Math.sin(age * 0.35f + orbit);
        float baseScale = attached ? 0.55f : 0.46f;

        // Link centers: the same arc (attached) and trail (flying) layout the
        // old links used, now carried into world space as emission points.
        int links = attached ? 7 : 6;
        List<Vec3> centers = new ArrayList<>(links);
        List<Vec3> localCenters = new ArrayList<>(links);
        for (int i = 0; i < links; i++) {
            float along = i / (float) (links - 1);
            float lag = along * (0.25f + 0.75f * stretch);
            Vec3 local;
            if (attached) {
                float arc = (along - 0.5f) * 2.4f + orbit * 0.35f;
                float radius = 1.15f * (1.0f - 0.18f * stretch);
                local = new Vec3(Math.cos(arc) * radius, Math.sin(along * Math.PI) * 0.35f, Math.sin(arc) * radius);
            } else {
                local = new Vec3(0, 0, -lag * 3.4f);
            }
            localCenters.add(local);
            centers.add(worldPoint(state, origin, local, baseScale));
        }

        // Pieces layer: a paired chain along the chain path, through the
        // first, middle, and last link centers in the renderer's own local
        // space, under the same pose transform as the emission layout.
        // The second strand is the first shifted sideways by a seeded
        // perpendicular and bowed a touch lower, thinner and dimmer, so
        // the two read as one doubled chain rather than two chains.
        {
            final Vec3 tendrilFrom = localCenters.get(0);
            final Vec3 tendrilControl = localCenters.get(localCenters.size() / 2);
            final Vec3 tendrilTo = localCenters.get(localCenters.size() - 1);
            final long tendrilSeed = state.seed;
            final float tendrilAlpha = 0.95f * pulse;
            final float tendrilEnv = alphaMul;
            final float tendrilDs = ds;
            final float tendrilWidth = attached ? 0.34f : 0.27f;
            Vec3 chainDir = tendrilTo.subtract(tendrilFrom);
            Vec3 chainPerp = new Vec3(-chainDir.z, 0, chainDir.x);
            chainPerp = chainPerp.lengthSqr() < 1.0e-6 ? new Vec3(1, 0, 0) : chainPerp.normalize();
            final Vec3 pairFrom = tendrilFrom.add(chainPerp.scale(0.10));
            final Vec3 pairControl = tendrilControl.add(chainPerp.scale(0.17)).add(new Vec3(0, -0.05, 0));
            final Vec3 pairTo = tendrilTo.add(chainPerp.scale(0.10));
            final float pairWidth = tendrilWidth * 0.72f;
            final float pairAlpha = tendrilAlpha * 0.85f;
            final boolean chainAttached = attached;
            poseStack.pushPose();
            poseStack.scale(baseScale, baseScale, baseScale);
            poseStack.mulPose(new Matrix4f().rotationY((float) Math.toRadians(-state.yRot)));
            poseStack.mulPose(new Matrix4f().rotationX((float) Math.toRadians(state.xRot)));
            ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
                ComplexGeometry.tendril(pose, buffer, tendrilSeed ^ 0x7E11L, tendrilFrom, tendrilControl, tendrilTo,
                    tendrilWidth, 0xA60DD9, 0xF20D33, tendrilAlpha, tendrilEnv, tendrilDs);
                ComplexGeometry.tendril(pose, buffer, tendrilSeed ^ 0x7E22L, pairFrom, pairControl, pairTo,
                    pairWidth, 0xA60DD9, 0xF20D33, pairAlpha, tendrilEnv, tendrilDs);
            });
            // Attack upgrade: three curse glass shards crystallize at
            // the anchor point while the chain is bound. In flight
            // there is no anchor yet, so the burst waits for the bind.
            if (chainAttached) {
                ComplexGeometry.submitSolid(collector, poseStack, (pose, buffer) ->
                    ComplexGeometry.shardBurst(pose, buffer, tendrilSeed ^ 0xA11CL, new Vec3(0, 0.05, 0),
                        new Vec3(0, 1, 0), 70f, 0.55f, 3,
                        0x380852, 0xBF0DFF, 0.90f, tendrilEnv, tendrilDs));
            }
            poseStack.popPose();
        }

        // Complex particle layer: spark streaks flashing at the links.
        emit(level, MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.030f),
            pointsSource(centers), 30f, delta, env * pulse);

        // The chain body: darkness and violet plasma emitted along the
        // segment polyline, soul wisps gathering at the link centers.
        MaterialEmitters.Source segments = segmentsSource(centers);
        emit(level, MaterialEmitters.Preset.darkness(0x1F1429, 1.15f, seed + 7, Vec3.ZERO, 0.020f),
            segments, 240f, delta, env);
        emit(level, MaterialEmitters.Preset.plasma(0xA60DD9, 0.85f, seed + 61, Vec3.ZERO, 0.025f),
            segments, 180f, delta, env * pulse);
        emit(level, MaterialEmitters.Preset.soul(0xB380FF, 0.80f, seed + 103, Vec3.ZERO, 0.020f),
            pointsSource(centers), 90f, delta, env);

        // The heart of the chain and the four riding glyph points, as dense
        // soul and plasma gatherings rather than drawn cores.
        Vec3 core = worldPoint(state, origin, new Vec3(0, attached ? 0.05f : 0.0f, 0), baseScale);
        emit(level, MaterialEmitters.Preset.plasma(0xF20D73, 1.15f, seed + 0x4E11, Vec3.ZERO, 0.030f),
            MaterialEmitters.point(core), 120f, delta, env * pulse);
        List<Vec3> glyphs = new ArrayList<>(4);
        for (int g = 0; g < 4; g++) {
            Vec3 local;
            if (attached) {
                double ga = age * 0.11 + g * (Math.PI * 0.5) + orbit * 0.7;
                local = new Vec3(Math.cos(ga) * 1.5f, Math.sin(age * 0.23 + g * 1.7) * 0.3f, Math.sin(ga) * 1.5f);
            } else {
                local = new Vec3(Math.sin(age * 0.3 + g * 1.3) * 0.18f, Math.cos(age * 0.26 + g * 1.1) * 0.18f, -(0.9f + g * 0.75f));
            }
            glyphs.add(worldPoint(state, origin, local, baseScale));
        }
        emit(level, MaterialEmitters.Preset.soul(0xBF0DFF, 0.85f, seed + 211, Vec3.ZERO, 0.025f),
            pointsSource(glyphs), 90f, delta, env);

        if (attached) {
            // The shackle collar: sampled emission points on three rings at
            // the bind point, in the old collar colors.
            List<Vec3> collar = new ArrayList<>(36);
            float[] collarRadii = {0.85f, 1.00f, 1.15f};
            for (float cr : collarRadii) {
                for (int i = 0; i < 12; i++) {
                    double a = (i / 12.0) * Math.PI * 2.0;
                    collar.add(worldPoint(state, origin, new Vec3(Math.cos(a) * cr, 0, Math.sin(a) * cr), baseScale));
                }
            }
            MaterialEmitters.Source collarSource = pointsSource(collar);
            emit(level, MaterialEmitters.Preset.plasma(0xE60D33, 0.95f, seed + 0xC012, Vec3.ZERO, 0.020f),
                collarSource, 150f, delta, env * pulse);
            emit(level, MaterialEmitters.Preset.darkness(0x380852, 1.20f, seed + 0xC011, Vec3.ZERO, 0.015f),
                collarSource, 90f, delta, env);

            // The attach snap, as a short material burst at the bind point.
            float snapIn = ease(Math.min(1.0f, age / 3.0f));
            float snapOut = ease(Math.max(0.0f, 1.0f - Math.max(0.0f, age - 3.0f) / 5.0f));
            float snap = snapIn * snapOut;
            if (snap > 0.02f) {
                MaterialEmitters.burst(level, MaterialEmitters.Preset.plasma(0xFFF2F7, 1.0f, seed + 0x51A9, Vec3.ZERO, 0.22f),
                    MaterialEmitters.point(origin), Math.max(1, Math.round(8f * snap * env)));
                MaterialEmitters.burst(level, MaterialEmitters.Preset.soul(0xD9B3FF, 0.9f, seed + 0x51AA, Vec3.ZERO, 0.18f),
                    MaterialEmitters.point(origin), Math.max(1, Math.round(5f * snap * env)));
            }
        }

        // The helical aura: two winding polylines around the chain, carried
        // as plasma emission paths in the old violet and crimson.
        for (int ribbon = 0; ribbon < 2; ribbon++) {
            List<Vec3> path = auraPath(state, origin, baseScale, age, attached, ribbon);
            int color = ribbon == 0 ? 0xA60DD9 : 0xF20D33;
            emit(level, MaterialEmitters.Preset.plasma(color, 0.75f, seed + ribbon * 1000 + 307, Vec3.ZERO, 0.020f),
                MaterialEmitters.path(path, 0.0), ribbon == 0 ? 120f : 90f, delta, env * 0.75f);
        }
    }

    /** The old helix layout as a world-space polyline of emission points. */
    private static List<Vec3> auraPath(CurseChainRenderState state, Vec3 origin, float baseScale,
                                       float age, boolean attached, int ribbon) {
        int steps = 14;
        float heightSpan = 2.4f;
        float ribbonR = 0.65f;
        float phaseOffset = ribbon * (float) Math.PI;
        List<Vec3> points = new ArrayList<>(steps + 1);
        for (int s = 0; s <= steps; s++) {
            float p = s / (float) steps;
            float y = -heightSpan * 0.5f + (p * heightSpan);
            double a = (p * Math.PI * 4.0) + (age * 0.25f) + phaseOffset;
            float rr = ribbonR + (float) Math.sin(p * Math.PI) * 0.3f;
            Vec3 point = new Vec3(Math.cos(a) * rr, y, Math.sin(a) * rr);
            if (!attached) {
                point = new Vec3(point.x, -point.z, point.y - 1.7f);
            }
            points.add(worldPoint(state, origin, point, baseScale));
        }
        return points;
    }

    /** Applies the renderer's old pose transform (scale, yaw, pitch) to a local point. */
    private static Vec3 worldPoint(CurseChainRenderState state, Vec3 origin, Vec3 local, float baseScale) {
        Vector3f v = new Vector3f((float) local.x, (float) local.y, (float) local.z);
        new Matrix4f()
            .translate((float) origin.x, (float) origin.y, (float) origin.z)
            .scale(baseScale)
            .rotateY((float) Math.toRadians(-state.yRot))
            .rotateX((float) Math.toRadians(state.xRot))
            .transformPosition(v);
        return new Vec3(v.x, v.y, v.z);
    }

    /** Samples a random point along a random segment of the polyline. */
    private static MaterialEmitters.Source segmentsSource(List<Vec3> points) {
        List<Vec3> copy = List.copyOf(points);
        return random -> {
            if (copy.size() < 2) return new MaterialEmitters.Sample(copy.isEmpty() ? Vec3.ZERO : copy.get(0), Vec3.ZERO);
            int i = random.nextInt(copy.size() - 1);
            Vec3 a = copy.get(i);
            Vec3 b = copy.get(i + 1);
            return new MaterialEmitters.Sample(a.add(b.subtract(a).scale(random.nextDouble())), Vec3.ZERO);
        };
    }

    /** Samples one of the given points at random. */
    private static MaterialEmitters.Source pointsSource(List<Vec3> points) {
        List<Vec3> copy = List.copyOf(points);
        return random -> new MaterialEmitters.Sample(copy.isEmpty() ? Vec3.ZERO : copy.get(random.nextInt(copy.size())), Vec3.ZERO);
    }

    private static void emit(ClientLevel level, MaterialEmitters.Preset preset, MaterialEmitters.Source source,
                             float ratePerSecond, float delta, float envelope) {
        if (level == null || envelope <= 0.01f || ratePerSecond <= 0f) return;
        int budget = Math.max(1, Math.round(ratePerSecond));
        new MaterialEmitters.Emitter(level, preset, source, ratePerSecond, budget).tick(delta, envelope);
    }

    private static float emissionDelta(CurseChainRenderState state, float age) {
        float prev = state.lastEmitAge;
        state.lastEmitAge = age;
        if (prev < 0f || age < prev) return 1.0f / 60.0f;
        return Math.max(0.001f, Math.min(0.05f, (age - prev) / 20.0f));
    }

    private static float ease(float t) {
        float c = Math.max(0.0f, Math.min(1.0f, t));
        return c * c * (3.0f - 2.0f * c);
    }
}
