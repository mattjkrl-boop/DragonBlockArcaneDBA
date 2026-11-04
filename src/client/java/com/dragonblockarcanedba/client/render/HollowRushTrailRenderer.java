package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.HollowRushTrailEntity;
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

import java.util.HashMap;
import java.util.Map;

/**
 * Entity Renderer for Hollow Rush Dash Corridor in Minecraft 26.2.
 * No-shapes pass: the corridor tube, streamline ribbons, puncture rings,
 * and third-dash arc are no longer drawn. The dash path itself is the
 * emission data: void plasma fills the corridor volume the tube used to
 * occupy, a cyan plasma stream runs the center line, soul vapor sheds
 * along the two helical paths the streamlines traced, and darkness pools
 * at the origin and destination contacts. On the third dash the same arc
 * region is sampled for a dense plasma cut with a white core, with no
 * arc mesh under it. First-person narrowing, lifetime, and the fade
 * curve are unchanged.
 *
 * Complex pass: the corridor gains a pieces layer of tendrils laid along
 * the rush path itself on the dissolve shader, a thick cyan to violet
 * core strand and a thinner offset strand on normal dashes, and two
 * strands swept across the slash arc on the third dash. WISP_CURL
 * sprites shed along the helical stream paths and across the arc as the
 * complex particle layer. The material emission stays as filler.
 */
public class HollowRushTrailRenderer extends EntityRenderer<HollowRushTrailEntity, HollowRushTrailRenderer.TrailRenderState> {

    public HollowRushTrailRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class TrailRenderState extends EntityRenderState {
        public float length = 5.0f;
        public boolean isThirdDash = false;
        public float age = 0;
        public float yRot = 0;
        public float xRot = 0;
        public boolean isFirstPersonOwner = false;
        public boolean onRight = true;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(HollowRushTrailEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(HollowRushTrailEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public TrailRenderState createRenderState() {
        return new TrailRenderState();
    }

    @Override
    public void extractRenderState(HollowRushTrailEntity entity, TrailRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.length = entity.getTrailLength();
        state.isThirdDash = entity.isThirdDash();
        state.age = entity.tickCount + partialTicks;
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.seed = entity.getUUID().getMostSignificantBits();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null &&
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) &&
            mc.options.getCameraType().isFirstPerson());
        if (mc.player != null) {
            boolean isRightHanded = (mc.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            boolean isOffhand = (mc.player.getOffhandItem().getItem() instanceof com.dragonblockarcanedba.item.HollowsEdgeItem &&
                !(mc.player.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.HollowsEdgeItem));
            state.onRight = isRightHanded ? !isOffhand : isOffhand;
        }
    }

    /** Per-entity emission streams, keyed by the entity seed. Emission points are data only. */
    private static final class Streams {
        final ClientLevel level;
        final MaterialEmitters.Emitter corridor;
        final MaterialEmitters.Emitter core;
        final MaterialEmitters.Emitter stream;
        final MaterialEmitters.Emitter ends;
        final MaterialEmitters.Emitter arc;
        final MaterialEmitters.Emitter arcCore;
        final MaterialEmitters.Emitter arcVapor;
        final MaterialEmitters.Emitter wispStream;
        final MaterialEmitters.Emitter wispArc;
        long lastNanos = 0L;
        long lastSeenNanos = 0L;
        Vec3 start = Vec3.ZERO;
        Vec3 dir = new Vec3(0, 0, 1);
        Vec3 right = new Vec3(1, 0, 0);
        Vec3 up = new Vec3(0, 1, 0);
        float len = 5f;
        float widthScale = 1f;
        float age = 0f;
        float progress = 0f;

        Streams(ClientLevel level, long seed) {
            this.level = level;
            int s = (int) seed;
            this.corridor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0x40038C, 2.8f, s ^ 0x11, Vec3.ZERO, 0.01f),
                random -> {
                    double t = random.nextDouble();
                    double pinch = 0.58 + 0.42 * Math.sin(t * Math.PI);
                    double ang = random.nextDouble() * Math.PI * 2.0;
                    double rr = 0.50 * widthScale * pinch * Math.sqrt(random.nextDouble());
                    Vec3 p = start.add(dir.scale(t * len))
                        .add(right.scale(Math.cos(ang) * rr)).add(up.scale(Math.sin(ang) * rr));
                    return new MaterialEmitters.Sample(p, dir.scale(-0.05));
                }, 170f, 190);
            this.core = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0x26E6FF, 1.4f, s ^ 0x22, Vec3.ZERO, 0.05f),
                random -> new MaterialEmitters.Sample(
                    start.add(dir.scale(random.nextDouble() * len)), dir.scale(-0.05)), 100f, 120);
            this.stream = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(0xBF1AFF, 1.7f, s ^ 0x33, Vec3.ZERO, 0.01f),
                random -> {
                    double t = random.nextDouble();
                    double strand = random.nextBoolean() ? 0.0 : Math.PI;
                    double ang = strand + t * Math.PI * 3.0 + age * 0.3;
                    double r = 0.56 * widthScale * (0.75 + 0.25 * Math.sin(t * Math.PI));
                    Vec3 p = start.add(dir.scale(t * len))
                        .add(right.scale(Math.cos(ang) * r)).add(up.scale(Math.sin(ang) * r));
                    return new MaterialEmitters.Sample(p, dir.scale(0.10));
                }, 100f, 120);
            this.ends = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.darkness(0x6605D9, 1.8f, s ^ 0x44, Vec3.ZERO, 0.012f),
                random -> {
                    Vec3 anchor = random.nextBoolean() ? start : start.add(dir.scale(len));
                    double ang = random.nextDouble() * Math.PI * 2.0;
                    double rr = 0.6 * widthScale * Math.sqrt(random.nextDouble());
                    Vec3 p = anchor.add(right.scale(Math.cos(ang) * rr)).add(up.scale(Math.sin(ang) * rr));
                    return new MaterialEmitters.Sample(p, Vec3.ZERO);
                }, 50f, 60);
            this.arc = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0x730DF2, 2.4f, s ^ 0x55, Vec3.ZERO, 0.01f),
                random -> new MaterialEmitters.Sample(arcPoint(random, 1.0), dir.scale(-0.15)), 180f, 200);
            this.arcCore = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xFFFFFF, 1.0f, s ^ 0x66, Vec3.ZERO, 0.008f),
                random -> new MaterialEmitters.Sample(arcPoint(random, 0.9), dir.scale(-0.15)), 80f, 90);
            this.arcVapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(0x26F2FF, 1.8f, s ^ 0x77, Vec3.ZERO, 0.012f),
                random -> new MaterialEmitters.Sample(arcPoint(random, 1.05), dir.scale(-0.08)), 60f, 70);
            // Complex particle layer: wisp curls shed along the helical
            // stream paths on normal dashes and across the arc on the
            // third dash.
            this.wispStream = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.WISP_CURL, Vec3.ZERO, 0.03f),
                random -> {
                    double t = random.nextDouble();
                    double strand = random.nextBoolean() ? 0.0 : Math.PI;
                    double ang = strand + t * Math.PI * 3.0 + age * 0.3;
                    double r = 0.56 * widthScale * (0.75 + 0.25 * Math.sin(t * Math.PI));
                    Vec3 p = start.add(dir.scale(t * len))
                        .add(right.scale(Math.cos(ang) * r)).add(up.scale(Math.sin(ang) * r));
                    return new MaterialEmitters.Sample(p, dir.scale(0.10));
                }, 30f, 36);
            this.wispArc = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.WISP_CURL, Vec3.ZERO, 0.03f),
                random -> new MaterialEmitters.Sample(arcPoint(random, 1.05), dir.scale(-0.08)), 30f, 36);
        }

        private Vec3 arcPoint(net.minecraft.util.RandomSource random, double radiusScale) {
            float close = ease(progress);
            double slashR = 1.15 * widthScale * (0.72 + 0.28 * (1.0 - close)) * radiusScale;
            double theta = (random.nextDouble() * 2.0 - 1.0) * Math.PI * 0.625;
            double rr = slashR * (0.90 + 0.20 * random.nextDouble());
            Vec3 center = start.add(dir.scale(len * 0.62));
            return center.add(right.scale(Math.cos(theta) * rr)).add(up.scale(Math.sin(theta) * rr));
        }

        private static float ease(float t) {
            float c = Math.max(0.0f, Math.min(1.0f, t));
            return c * c * (3.0f - 2.0f * c);
        }
    }

    private final Map<Long, Streams> streams = new HashMap<>();

    @Override
    public void submit(TrailRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float maxLife = 14.0f;
        float progress = Math.min(1.0f, state.age / maxLife);
        if (progress >= 1.0f) {
            streams.remove(state.seed);
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        float alpha = ease(1.0f - progress) * (state.isThirdDash ? 0.95f : 0.80f);
        Vec3 start = new Vec3(state.x, state.y, state.z);
        float ds = MaterialEmitters.distanceScale(start);
        float env = alpha * ds;
        if (env <= 0.01f) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        Streams found = streams.get(state.seed);
        if (found == null || found.level != level) {
            found = new Streams(level, state.seed);
            streams.put(state.seed, found);
        }
        final Streams s = found;
        long now = System.nanoTime();
        float delta = s.lastNanos == 0L ? 1f / 60f : Math.min(0.05f, (now - s.lastNanos) / 1_000_000_000f);
        s.lastNanos = now;
        s.lastSeenNanos = now;
        streams.values().removeIf(other -> other != s && now - other.lastSeenNanos > 4_000_000_000L);

        s.start = start;
        s.dir = Vec3.directionFromRotation(state.xRot, state.yRot);
        Vec3 right = s.dir.cross(new Vec3(0, 1, 0));
        s.right = right.lengthSqr() < 0.001 ? new Vec3(1, 0, 0) : right.normalize();
        s.up = s.right.cross(s.dir).normalize();
        s.len = Math.max(0.5f, state.length);
        float fpScale = state.isFirstPersonOwner ? 0.50f : 1.0f;
        s.widthScale = (state.isThirdDash ? 1.4f : 1.0f) * fpScale;
        s.age = state.age;
        s.progress = progress;

        // Pieces layer: tendrils along the rush path, in local space (the
        // pose sits at the trail origin, unrotated, so the synced dash
        // direction is a plain offset). Two strands on a normal dash is
        // 768 vertices at full spine samples, four on the third dash arc
        // is 1536, both well inside the class M budget.
        {
            final Vec3 dirL = s.dir;
            final Vec3 rightL = s.right;
            final Vec3 upL = s.up;
            final float lenL = s.len;
            final float wsL = s.widthScale;
            final long pseed = state.seed;
            final float penv = alpha;
            final float pds = FxEnvelope.distScale(Math.sqrt(Math.max(0.0, state.distanceToCameraSq)));
            if (state.isThirdDash) {
                // The strands sweep the same arc the emission samples, on
                // the emission's closing radius curve. The 225 degree
                // sweep is drawn as two tendrils per arc meeting at the
                // arc mid, each control pushed out to the midpoint
                // matched distance (radius times 2 minus cos of the half
                // sweep, about 1.44 here), so the spines hug the arc
                // instead of cutting across it. Four tendrils at full
                // spine samples is 1536 vertices, inside the budget.
                float close = ease(progress);
                double slashR = 1.15 * wsL * (0.72 + 0.28 * (1.0 - close));
                Vec3 arcC = dirL.scale(lenL * 0.62);
                double th = Math.PI * 0.625;
                ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
                    for (int half = 0; half < 2; half++) {
                        double a0 = half == 0 ? -th : 0.0;
                        double a1 = half == 0 ? -th * 0.5 : th * 0.5;
                        double a2 = half == 0 ? 0.0 : th;
                        ComplexGeometry.tendril(pose, buffer, pseed ^ (0xB11L + half * 131L),
                            arcPointLocal(arcC, rightL, upL, a0, slashR),
                            arcPointLocal(arcC, rightL, upL, a1, slashR * 1.44),
                            arcPointLocal(arcC, rightL, upL, a2, slashR),
                            0.34f * wsL, 0xFFFFFF, 0x730DF2, 0.92f, penv, pds);
                        ComplexGeometry.tendril(pose, buffer, pseed ^ (0xB31L + half * 131L),
                            arcPointLocal(arcC, rightL, upL, a0, slashR * 0.88),
                            arcPointLocal(arcC, rightL, upL, a1, slashR * 0.88 * 1.44),
                            arcPointLocal(arcC, rightL, upL, a2, slashR * 0.88),
                            0.18f * wsL, 0x26F2FF, 0x730DF2, 0.92f, penv, pds);
                    }
                });
            } else {
                Vec3 mid = dirL.scale(lenL * 0.5);
                Vec3 end = dirL.scale(lenL);
                ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
                    ComplexGeometry.tendril(pose, buffer, pseed ^ 0xB33L,
                        Vec3.ZERO, mid.add(upL.scale(0.08 * wsL)), end, 0.30f * wsL,
                        0x26E6FF, 0xBF1AFF, 0.92f, penv, pds);
                    ComplexGeometry.tendril(pose, buffer, pseed ^ 0xB44L,
                        rightL.scale(0.18 * wsL), mid.add(rightL.scale(0.10 * wsL)).add(upL.scale(-0.06 * wsL)),
                        end.add(rightL.scale(-0.05 * wsL)), 0.16f * wsL,
                        0x730DF2, 0x26F2FF, 0.92f, penv, pds);
                });
            }
        }

        if (state.isThirdDash) {
            s.arc.tick(delta, env * 0.85f);
            s.arcCore.tick(delta, env);
            s.arcVapor.tick(delta, env * 0.6f);
            s.wispArc.tick(delta, env * 0.8f);
        } else {
            s.corridor.tick(delta, env * 0.70f);
            s.core.tick(delta, env * 0.85f);
            s.stream.tick(delta, env);
            s.wispStream.tick(delta, env * 0.85f);
            s.ends.tick(delta, env);
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    /** One point on the third dash arc in local space, matching the emission sampler's arc math. */
    private static Vec3 arcPointLocal(Vec3 center, Vec3 right, Vec3 up, double theta, double radius) {
        return center.add(right.scale(Math.cos(theta) * radius)).add(up.scale(Math.sin(theta) * radius));
    }

    private static float ease(float t) {
        float c = Math.max(0.0f, Math.min(1.0f, t));
        return c * c * (3.0f - 2.0f * c);
    }
}
