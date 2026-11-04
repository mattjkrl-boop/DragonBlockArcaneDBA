package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.VoidRiftEntity;
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
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Entity Renderer for Void Rift in Minecraft 26.2.
 * No-shapes pass: the tear is no longer drawn. The billboard plane, the
 * tall mouth proportions, and the implosion choreography survive only as
 * emission data. Void plasma streams inward across the mouth plane with a
 * velocity toward the core, darkness fills the throat volume, soul wisps
 * ride the rim with a tangential accretion drift, sparse vapor suggests
 * the old back halo, the seven lip crystal anchors shed soul plasma as
 * they are sucked toward the throat, and at peak collapse a white plasma
 * seam burns down the center column. The fade-in envelope, the implosion
 * alpha windows (body first, then rim, then seam, shards collapsing
 * earliest), and the distance scaling are unchanged from the spectacle
 * pass, and the seed is still the entity UUID.
 *
 * Complex pass: the tear gains a ring of solid void shards and two
 * energy tendrils streaming inward, drawn in the same billboard plane
 * the emission basis uses, and two tendrils run from the top and bottom
 * rim to the throat through the tendril canvas, following the rim
 * window of the implosion choreography. ASH_FLAKE sprites drift on the
 * rim as the complex particle layer. The material emission stays as
 * filler.
 *
 * Attack upgrade pass: a third rim tendril joins from the side rim.
 * The optional second fragment ring was not added: the mouth already
 * carries the five real FragmentEntity shards plus dense rim streams,
 * so a drawn burst at the same radius would duplicate the real ring.
 *
 * Entity objects pass: the shard ring is no longer drawn here. The
 * host spawns five FragmentEntity shards server side (entity-audit
 * section 4) that hang on a world ring at the mouth for the channel
 * and drop for real at implosion; the billboard anchors are world
 * anchors now, the accepted trade recorded in the audit. Everything
 * else in this renderer is unchanged.
 */
public class VoidRiftRenderer extends EntityRenderer<VoidRiftEntity, VoidRiftRenderer.RiftRenderState> {
    private static final int SHARDS = 7;

    public VoidRiftRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class RiftRenderState extends EntityRenderState {
        public float radius = 0;
        public boolean isImploding = false;
        public float age = 0;
        public int implodeStartTick = -1;
        public long seed = 0;
        public double ex, ey, ez;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(VoidRiftEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(VoidRiftEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public RiftRenderState createRenderState() {
        return new RiftRenderState();
    }

    @Override
    public void extractRenderState(VoidRiftEntity entity, RiftRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.isImploding = entity.isImploding();
        state.age = entity.tickCount + partialTicks;
        state.implodeStartTick = entity.getImplodeStartTick();
        state.seed = entity.getUUID().getMostSignificantBits();
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
    }

    /** Per-entity emission streams, keyed by the entity seed. Emission points are data only. */
    private static final class Streams {
        final ClientLevel level;
        final MaterialEmitters.Emitter halo;
        final MaterialEmitters.Emitter inward;
        final MaterialEmitters.Emitter throat;
        final MaterialEmitters.Emitter rim;
        final MaterialEmitters.Emitter seam;
        final MaterialEmitters.Emitter shards;
        final MaterialEmitters.Emitter ash;
        long lastNanos = 0L;
        long lastSeenNanos = 0L;
        Vec3 center = Vec3.ZERO;
        Vec3 right = new Vec3(1, 0, 0);
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 view = new Vec3(0, 0, 1);
        float radius = 2.5f;
        float halfH = 2.8f;
        float bellyX = 1.4f;
        float depthBase = 0.7f;
        float bandScale = 1f;
        float suck = 0.35f;
        final float[] shardX = new float[SHARDS];
        final float[] shardY = new float[SHARDS];
        final float[] shardZ = new float[SHARDS];

        private Vec3 towardCenter(Vec3 p, double speed) {
            Vec3 d = center.subtract(p);
            double len = d.length();
            return len < 1e-4 ? Vec3.ZERO : d.scale(speed / len);
        }

        Streams(ClientLevel level, long seed, float radius) {
            this.level = level;
            int s = (int) seed;
            float plasmaScale = Math.min(radius * 1.1f, 12f);
            float darkScale = Math.min(radius * 1.8f, 16f);
            float soulScale = Math.min(radius * 0.8f, 10f);
            this.halo = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(0x7305D9, Math.min(radius * 2.0f, 18f), s ^ 0x11, Vec3.ZERO, 0.01f),
                random -> {
                    double ang = random.nextDouble() * Math.PI * 2.0;
                    double rr = radius * 1.55 * Math.sqrt(random.nextDouble());
                    Vec3 p = center.add(right.scale(Math.cos(ang) * rr)).add(up.scale(Math.sin(ang) * rr))
                        .add(view.scale((random.nextDouble() * 2.0 - 1.0) * depthBase * 1.4));
                    return new MaterialEmitters.Sample(p, Vec3.ZERO);
                }, 40f, 48);
            this.inward = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0x800DE0, plasmaScale, s ^ 0x21, Vec3.ZERO, 0.008f),
                random -> {
                    double ang = random.nextDouble() * Math.PI * 2.0;
                    double rr = 0.75 + 0.25 * random.nextDouble();
                    Vec3 p = center.add(right.scale(Math.cos(ang) * bellyX * 2.2 * rr))
                        .add(up.scale(Math.sin(ang) * halfH * 1.15 * rr))
                        .add(view.scale((random.nextDouble() * 2.0 - 1.0) * depthBase * 0.5));
                    return new MaterialEmitters.Sample(p, towardCenter(p, suck));
                }, 160f, 190);
            this.throat = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.darkness(0x05000A, darkScale, s ^ 0x31, Vec3.ZERO, 0.006f),
                random -> {
                    Vec3 p = center.add(right.scale((random.nextDouble() * 2.0 - 1.0) * bellyX * 0.94))
                        .add(up.scale((random.nextDouble() * 2.0 - 1.0) * halfH * 0.97))
                        .add(view.scale((random.nextDouble() * 2.0 - 1.0) * depthBase * 0.3));
                    return new MaterialEmitters.Sample(p, towardCenter(p, 0.05));
                }, 80f, 90);
            this.rim = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.soul(0x9905F2, soulScale, s ^ 0x41, Vec3.ZERO, 0.008f),
                random -> {
                    double ang = random.nextDouble() * Math.PI * 2.0;
                    Vec3 p = center.add(right.scale(Math.cos(ang) * bellyX * bandScale * 1.16))
                        .add(up.scale(Math.sin(ang) * halfH * bandScale * 1.05))
                        .add(view.scale((random.nextDouble() * 2.0 - 1.0) * depthBase * 0.4));
                    Vec3 tangent = right.scale(-Math.sin(ang)).add(up.scale(Math.cos(ang))).scale(0.25);
                    return new MaterialEmitters.Sample(p, tangent.add(towardCenter(p, 0.05)));
                }, 120f, 140);
            this.seam = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xFFFFFF, Math.min(radius * 0.55f, 7f), s ^ 0x51, Vec3.ZERO, 0.005f),
                random -> {
                    double t = random.nextDouble() * 2.0 - 1.0;
                    Vec3 p = center.add(up.scale(t * halfH * 0.94))
                        .add(right.scale((random.nextDouble() * 2.0 - 1.0) * radius * 0.045));
                    return new MaterialEmitters.Sample(p, Vec3.ZERO);
                }, 100f, 120);
            this.shards = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.soul(0x9405F0, Math.min(radius * 0.9f, 9f), s ^ 0x61, Vec3.ZERO, 0.01f),
                random -> {
                    int i = random.nextInt(SHARDS);
                    Vec3 p = center.add(right.scale(shardX[i])).add(up.scale(shardY[i]))
                        .add(view.scale(shardZ[i] + (random.nextDouble() * 2.0 - 1.0) * radius * 0.02));
                    return new MaterialEmitters.Sample(p, towardCenter(p, suck * 0.6));
                }, 70f, 84);
            // Complex particle layer: ash flakes drifting on the rim
            // ellipse with a slow tangential drift.
            this.ash = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.ASH_FLAKE, Vec3.ZERO, 0.06f),
                random -> {
                    double ang = random.nextDouble() * Math.PI * 2.0;
                    Vec3 p = center.add(right.scale(Math.cos(ang) * bellyX * bandScale * 1.16))
                        .add(up.scale(Math.sin(ang) * halfH * bandScale * 1.05))
                        .add(view.scale((random.nextDouble() * 2.0 - 1.0) * depthBase * 0.4));
                    Vec3 tangent = right.scale(-Math.sin(ang)).add(up.scale(Math.cos(ang))).scale(0.12);
                    return new MaterialEmitters.Sample(p, tangent);
                }, 26f, 34);
        }
    }

    private final Map<Long, Streams> streams = new HashMap<>();

    @Override
    public void submit(RiftRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float radius = state.radius;
        if (radius <= 0.05f) {
            streams.remove(state.seed);
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        // Fade-in envelope and implosion progress, driven by synced state so
        // every client plays the same 30 tick collapse.
        float env = state.isImploding ? 1.0f : ease(Math.min(1.0f, state.age / 20.0f));
        float implodeT = 0.0f;
        if (state.isImploding && state.implodeStartTick >= 0) {
            implodeT = Math.min(1.0f, Math.max(0.0f,
                (state.age - state.implodeStartTick) / (float) VoidRiftEntity.IMPLOSION_TICKS));
        }
        float eased = ease(implodeT);
        float fade = state.isImploding ? (1.0f - implodeT) : env;
        // Distance scaling: the envelope fades with the square root of the
        // toolkit's distance factor, as the layered geometry version did.
        Vec3 center = new Vec3(state.ex, state.ey, state.ez);
        float ds = MaterialEmitters.distanceScale(center);
        fade *= (float) Math.sqrt(Math.max(0.05f, ds));
        if (fade <= 0.01f && !state.isImploding) {
            streams.remove(state.seed);
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        // Implosion alpha choreography, unchanged windows: the body dies
        // first, the rim outlives it, the seam is the last light standing,
        // and the shards collapse with the body.
        float bodyA = fade;
        float rimA = fade;
        float seamA = 0.0f;
        float shardA = 0.42f * env;
        if (state.isImploding) {
            bodyA = fade * (1.0f - clamp01((implodeT - 0.30f) / 0.48f));
            rimA = fade * (1.0f - clamp01((implodeT - 0.50f) / 0.36f));
            seamA = fade * clamp01((implodeT - 0.74f) / 0.26f);
            shardA = fade * 0.62f * (1.0f - clamp01(implodeT / 0.70f));
        }

        // Tear proportions, same as the cut era: a tall narrow mouth whose
        // belly pulses, narrowing as it opens and collapsing shut.
        float open = state.isImploding ? 1.0f : eased;
        float pulse = state.isImploding ? (1.0f - 0.18f * eased)
            : (0.94f + 0.06f * (float) Math.sin(state.age * 0.4f));
        float yShrink = state.isImploding ? (1.0f - 0.82f * eased) : 1.0f;
        float halfH = radius * 1.12f * yShrink;
        float belly = radius * 0.56f * pulse;
        float bellyX = belly * open;
        float depthBase = radius * 0.28f;
        float bandScale = state.isImploding ? (1.0f - 0.55f * eased) : (0.82f + 0.18f * eased);
        float age = state.age;

        Streams found = streams.get(state.seed);
        if (found == null || found.level != level) {
            found = new Streams(level, state.seed, radius);
            streams.put(state.seed, found);
        }
        final Streams s = found;
        long now = System.nanoTime();
        float delta = s.lastNanos == 0L ? 1f / 60f : Math.min(0.05f, (now - s.lastNanos) / 1_000_000_000f);
        s.lastNanos = now;
        s.lastSeenNanos = now;
        streams.values().removeIf(other -> other != s && now - other.lastSeenNanos > 4_000_000_000L);

        s.center = center;
        // Billboard plane basis in world space, the same conjugate
        // orientation pattern SkyCracksRenderer uses for its plane.
        if (cameraState.orientation != null) {
            Quaternionf orientation = new Quaternionf(cameraState.orientation).conjugate();
            Vector3f bx = orientation.transform(new Vector3f(1, 0, 0));
            Vector3f by = orientation.transform(new Vector3f(0, 1, 0));
            Vector3f bz = orientation.transform(new Vector3f(0, 0, 1));
            s.right = new Vec3(bx.x, bx.y, bx.z);
            s.up = new Vec3(by.x, by.y, by.z);
            s.view = new Vec3(bz.x, bz.y, bz.z);
        }
        s.radius = radius;
        s.halfH = halfH;
        s.bellyX = Math.max(0.02f, bellyX);
        s.depthBase = depthBase;
        s.bandScale = bandScale;
        s.suck = 0.35f + eased * 0.95f;

        // Lip crystal anchors: the old shard layout math, kept as emission
        // placement data. The anchors hug the lips, breathe with the tear,
        // and get sucked toward the throat as the collapse takes them.
        float shrink = state.isImploding ? (1.0f - 0.9f * eased) : 1.0f;
        for (int i = 0; i < SHARDS; i++) {
            float t = 0.18f + 0.64f * (i / (float) (SHARDS - 1));
            boolean left = (i & 1) == 0;
            float side = left ? -1.0f : 1.0f;
            float slim = 0.72f + 0.28f * (float) Math.sin(t * Math.PI);
            float shaped = (float) Math.pow(Math.sin(t * Math.PI), 0.55 + 1.35 * (1.0 - open));
            float y = (t - 0.5f) * 2.0f * halfH;
            float hug = radius * 0.29f * slim * (state.isImploding ? (1.0f - eased) : 1.0f);
            float x = side * (bellyX * shaped * 1.16f + hug);
            if (state.isImploding) x *= (1.0f - 0.8f * eased);
            float z = -depthBase * 0.3f * (state.isImploding ? (1.0f - eased) : 1.0f)
                + (float) Math.sin(age * 0.7f + i * 2.3f) * radius * 0.02f;
            s.shardX[i] = x * shrink;
            s.shardY[i] = y * shrink;
            s.shardZ[i] = z;
        }

        // Pieces layer, in the billboard plane: the pose is rotated by
        // the conjugate camera orientation, the same basis the emission
        // streams are built from, so local X and Y are the tear's right
        // and up. Three inward tendrils after the attack upgrade (1152
        // vertices), inside the class M budget of 2500, following the
        // rim window of the implosion choreography. The shard ring that
        // used to draw here is a set of FragmentEntity instances now
        // (see the class doc).
        float tendrilPieceEnv = state.isImploding
            ? fade * (1.0f - clamp01((implodeT - 0.50f) / 0.36f)) : env;
        if (tendrilPieceEnv > 0.02f && ds > 0.05f) {
            final float bellyL = s.bellyX;
            final float halfHL = s.halfH;
            final float radiusL = radius;
            final long pseed = state.seed;
            final float geoDist = FxEnvelope.distScale(Math.sqrt(Math.max(0.0, state.distanceToCameraSq)));
            poseStack.pushPose();
            if (cameraState.orientation != null) {
                poseStack.mulPose(new Matrix4f().rotation(
                    new Quaternionf(cameraState.orientation).conjugate()));
            }
            if (tendrilPieceEnv > 0.02f) {
                final float tEnv = tendrilPieceEnv;
                final float twidth = 0.14f + radiusL * 0.035f;
                ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
                    ComplexGeometry.tendril(pose, buffer, pseed ^ 0xE22L,
                        new Vec3(bellyL * 0.30, halfHL * 0.98, 0),
                        new Vec3(bellyL * 0.85, halfHL * 0.45, 0),
                        new Vec3(0, 0, 0), twidth, 0x9905F2, 0xD9B8FF, 0.90f, tEnv, geoDist);
                    ComplexGeometry.tendril(pose, buffer, pseed ^ 0xE33L,
                        new Vec3(-bellyL * 0.30, -halfHL * 0.98, 0),
                        new Vec3(-bellyL * 0.85, -halfHL * 0.45, 0),
                        new Vec3(0, 0, 0), twidth, 0x9905F2, 0xD9B8FF, 0.90f, tEnv, geoDist);
                    // Attack upgrade: a third strand enters from the
                    // side rim at mouth height, on the same window.
                    ComplexGeometry.tendril(pose, buffer, pseed ^ 0xE44L,
                        new Vec3(-bellyL * 1.02, halfHL * 0.10, 0),
                        new Vec3(-bellyL * 0.55, halfHL * 0.30, 0),
                        new Vec3(0, 0, 0), twidth, 0x9905F2, 0xD9B8FF, 0.90f, tEnv, geoDist);
                });
            }
            poseStack.popPose();
        }

        if (fade > 0.02f) {
            s.halo.tick(delta, fade * 0.30f);
            s.rim.tick(delta, rimA);
            s.ash.tick(delta, rimA * 0.8f);
        }
        if (bodyA > 0.02f) {
            s.inward.tick(delta, bodyA);
            s.throat.tick(delta, bodyA);
        }
        if (seamA > 0.03f) {
            s.seam.tick(delta, seamA);
        }
        if (shardA > 0.04f && open > 0.04f) {
            s.shards.tick(delta, shardA);
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    private static float clamp01(float v) {
        return v < 0.0f ? 0.0f : Math.min(1.0f, v);
    }

    private static float ease(float t) {
        float c = clamp01(t);
        return c * c * (3.0f - 2.0f * c);
    }
}
