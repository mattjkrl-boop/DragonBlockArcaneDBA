package com.dragonblockarcanedba.client.render.ki;

import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Bespoke procedural geometry for weapon effects (BESPOKE-STANDARD rule 1).
 * Every builder is seeded: the same seed, envelope, and scale draw the same
 * silhouette on every client, so seeds come from the entity's synced state.
 * Shapes are deliberately irregular: noise-jittered radii, torn edges, uneven
 * spacing, broken segments, and nicks, never equal-segment primitives.
 *
 * All builders draw in entity local space inside a submitCustomGeometry call,
 * write through KiRenderHelper.vertex (so render types stay shared), multiply
 * alpha by the lifecycle envelope, and step segment counts down with the
 * caller's distScale. Quads and triangles are emitted double sided, matching
 * the convention in LayeredRender.
 */
public final class BespokeGeometry {
    private BespokeGeometry() {}

    // ---------------------------------------------------------------- RNG

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    /** Deterministic splitmix stream. Same seed, same sequence, every client. */
    public static final class Rng {
        private long state;

        public Rng(long seed) {
            this.state = seed;
        }

        public long nextLong() {
            long z = (state += 0x9E3779B97F4A7C15L);
            return mix(z);
        }

        public float nextFloat() {
            return (nextLong() >>> 40) * (1.0f / (1 << 24));
        }

        public float range(float a, float b) {
            return a + (b - a) * nextFloat();
        }

        /** -1..1 */
        public float signed() {
            return nextFloat() * 2f - 1f;
        }
    }

    /** Lattice hash in 0..1 for (seed, index). */
    private static float hash1(long seed, int i) {
        return (mix(seed + i * 0x9E3779B97F4A7C15L) >>> 40) * (1.0f / (1 << 24));
    }

    /** Smooth value noise in 0..1, stable for a given seed. */
    private static float noise1(long seed, float x) {
        int i = (int) Math.floor(x);
        float f = x - i;
        float u = f * f * (3f - 2f * f);
        return hash1(seed, i) * (1f - u) + hash1(seed, i + 1) * u;
    }

    // ---------------------------------------------------------------- emit helpers

    private static void v(VertexConsumer buffer, Matrix4f mat, PoseStack.Pose pose,
                          float x, float y, float z, float r, float g, float b, float a,
                          float u, float vv, float ny) {
        KiRenderHelper.vertex(buffer, mat, pose, x, y, z, r, g, b, a, u, vv, 0, ny, 0);
    }

    /** Double sided quad; alpha a0 on the p0-p1 edge, a1 on the p3-p2 edge. */
    private static void quad(VertexConsumer buffer, Matrix4f mat, PoseStack.Pose pose,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             float r, float g, float b, float a0, float a1) {
        v(buffer, mat, pose, x0, y0, z0, r, g, b, a0, 0, 0, 1);
        v(buffer, mat, pose, x1, y1, z1, r, g, b, a0, 1, 0, 1);
        v(buffer, mat, pose, x2, y2, z2, r, g, b, a1, 1, 1, 1);
        v(buffer, mat, pose, x3, y3, z3, r, g, b, a1, 0, 1, 1);
        v(buffer, mat, pose, x3, y3, z3, r, g, b, a1, 0, 1, -1);
        v(buffer, mat, pose, x2, y2, z2, r, g, b, a1, 1, 1, -1);
        v(buffer, mat, pose, x1, y1, z1, r, g, b, a0, 1, 0, -1);
        v(buffer, mat, pose, x0, y0, z0, r, g, b, a0, 0, 0, -1);
    }

    /** Double sided triangle with one alpha. */
    private static void tri(VertexConsumer buffer, Matrix4f mat, PoseStack.Pose pose,
                            float x0, float y0, float z0, float x1, float y1, float z1,
                            float x2, float y2, float z2,
                            float r, float g, float b, float a) {
        v(buffer, mat, pose, x0, y0, z0, r, g, b, a, 0, 0, 1);
        v(buffer, mat, pose, x1, y1, z1, r, g, b, a, 1, 0, 1);
        v(buffer, mat, pose, x2, y2, z2, r, g, b, a, 0.5f, 1, 1);
        v(buffer, mat, pose, x2, y2, z2, r, g, b, a, 0.5f, 1, -1);
        v(buffer, mat, pose, x1, y1, z1, r, g, b, a, 1, 0, -1);
        v(buffer, mat, pose, x0, y0, z0, r, g, b, a, 0, 0, -1);
    }

    // ---------------------------------------------------------------- builders

    /**
     * A torn ribbon running along local +X, lying in the XZ plane and rising
     * by bend over its length. Both long edges jitter independently, so the
     * silhouette is ripped, not ruled. Rotate the pose for slashes and lashes.
     */
    public static void tornRibbon(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                                  float length, float width, float bend,
                                  float r, float g, float b, float alpha,
                                  float envelope, float distScale) {
        if (envelope <= 0.01f || alpha <= 0.01f) return;
        Matrix4f mat = pose.pose();
        int steps = FxEnvelope.segments(18, distScale, 8);
        long seedTop = seed ^ 0x51ED27L, seedBot = seed ^ 0x2545F4L;
        float px = 0, py = 0, pzT = 0, pzB = 0, pa = 0;
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            float x = length * t;
            float y = bend * t * t;
            float halfW = width * 0.5f * (1f - 0.35f * t);
            float zT = halfW * (0.7f + 0.65f * noise1(seedTop, t * 7f));
            float zB = -halfW * (0.7f + 0.65f * noise1(seedBot, t * 7f));
            float a = alpha * envelope * (1f - t * 0.55f);
            if (i > 0) {
                quad(buffer, mat, pose, px, py, pzT, px, py, pzB, x, y, zB, x, y, zT, r, g, b, pa, a);
            }
            px = x; py = y; pzT = zT; pzB = zB; pa = a;
        }
    }

    /**
     * A fan of uneven shards bursting in the XZ plane: irregular spacing,
     * lengths, widths, and tip heights, for shatters, bursts, and spikes.
     */
    public static void shardFan(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                                float radius, float r, float g, float b, float alpha,
                                float envelope, float distScale) {
        if (envelope <= 0.01f || alpha <= 0.01f) return;
        Matrix4f mat = pose.pose();
        int shards = FxEnvelope.segments(9, distScale, 5);
        Rng rng = new Rng(seed);
        float angle = rng.range(0f, (float) Math.PI * 2f);
        for (int i = 0; i < shards; i++) {
            angle += (float) (Math.PI * 2.0 / shards) * rng.range(0.55f, 1.5f);
            float len = radius * rng.range(0.5f, 1.3f);
            float w = radius * rng.range(0.05f, 0.13f);
            float lift = radius * rng.range(0f, 0.22f);
            float ca = (float) Math.cos(angle), sa = (float) Math.sin(angle);
            float px = -sa, pz = ca;
            float a = alpha * envelope * rng.range(0.7f, 1.15f);
            tri(buffer, mat, pose,
                px * -w, 0.02f, pz * -w,
                px * w, 0.02f, pz * w,
                ca * len, 0.02f + lift, sa * len,
                r, g, b, Math.min(1f, a));
        }
    }

    /**
     * A curved blade arc in the XZ plane: tapered profile that swells mid
     * sweep and pinches at both ends, with seeded nicks bitten out of the
     * outer edge. sweepDeg is the total arc; the arc is centered on +X.
     */
    public static void bladeArc(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                                float radius, float sweepDeg,
                                float r, float g, float b, float alpha,
                                float envelope, float distScale) {
        if (envelope <= 0.01f || alpha <= 0.01f) return;
        Matrix4f mat = pose.pose();
        int steps = FxEnvelope.segments(20, distScale, 10);
        float sweep = (float) Math.toRadians(sweepDeg);
        float start = -sweep / 2f;
        float nickA = hash1(seed, 71) * 0.8f + 0.1f;
        float nickB = hash1(seed, 97) * 0.8f + 0.1f;
        float pIx = 0, pIz = 0, pOx = 0, pOz = 0, pa = 0;
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            float ang = start + sweep * t;
            float profile = 0.5f + 0.62f * (float) Math.sin(Math.PI * Math.min(1f, t * 1.12f));
            profile *= 0.8f + 0.45f * noise1(seed, t * 6f);
            if (Math.abs(t - nickA) < 0.045f || Math.abs(t - nickB) < 0.035f) profile *= 0.55f;
            float inner = radius * 0.5f;
            float outer = radius * Math.max(0.2f, profile);
            float ca = (float) Math.cos(ang), sa = (float) Math.sin(ang);
            float ix = ca * inner, iz = sa * inner, ox = ca * outer, oz = sa * outer;
            float edge = (float) Math.pow(Math.sin(Math.PI * Math.min(1f, Math.max(0f, t))), 0.5f);
            float a = alpha * envelope * (0.25f + 0.75f * edge);
            if (i > 0) {
                quad(buffer, mat, pose, pIx, 0f, pIz, pOx, 0f, pOz, ox, 0f, oz, ix, 0f, iz, r, g, b, pa, a);
            }
            pIx = ix; pIz = iz; pOx = ox; pOz = oz; pa = a;
        }
    }

    /**
     * A displaced dome: a hemisphere whose vertices are pushed in and out by
     * seeded noise, with uneven column spacing, so it reads as a shockwave
     * or barrier shell, never as a sphere. Rim alpha dies to 0.35.
     */
    public static void displacedDome(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                                     float radius, float r, float g, float b, float alpha,
                                     float envelope, float distScale) {
        if (envelope <= 0.01f || alpha <= 0.01f) return;
        Matrix4f mat = pose.pose();
        int cols = FxEnvelope.segments(16, distScale, 9);
        int rings = Math.max(3, FxEnvelope.segments(5, distScale, 3));
        float[] colAng = new float[cols];
        for (int j = 0; j < cols; j++) {
            colAng[j] = (float) (Math.PI * 2.0 * j / cols) + (hash1(seed, 300 + j) - 0.5f) * 0.22f;
        }
        // Top fan: center point to the first ring.
        float topY = radius * (0.92f + 0.16f * hash1(seed, 999));
        float firstPhi = 0.32f;
        float[][] prev = new float[cols][3];
        for (int j = 0; j < cols; j++) {
            float[] p = domePoint(seed, colAng[j], firstPhi, radius, j, 0);
            prev[j] = p;
        }
        for (int j = 0; j < cols; j++) {
            float[] p0 = prev[j], p1 = prev[(j + 1) % cols];
            tri(buffer, mat, pose, 0, topY, 0, p0[0], p0[1], p0[2], p1[0], p1[1], p1[2],
                r, g, b, alpha * envelope);
        }
        for (int ring = 1; ring <= rings; ring++) {
            float phi = firstPhi + ((float) Math.PI / 2f - firstPhi) * (ring / (float) rings);
            float ringAlpha = alpha * envelope * (1f - 0.65f * (ring / (float) rings));
            float[][] cur = new float[cols][3];
            for (int j = 0; j < cols; j++) cur[j] = domePoint(seed, colAng[j], phi, radius, j, ring);
            for (int j = 0; j < cols; j++) {
                float[] a0 = prev[j], a1 = prev[(j + 1) % cols], b1 = cur[(j + 1) % cols], b0 = cur[j];
                quad(buffer, mat, pose, a0[0], a0[1], a0[2], a1[0], a1[1], a1[2],
                    b1[0], b1[1], b1[2], b0[0], b0[1], b0[2], r, g, b, ringAlpha, ringAlpha);
            }
            prev = cur;
        }
    }

    private static float[] domePoint(long seed, float ang, float phi, float radius, int col, int ring) {
        float disp = 1f + (noise1(seed ^ (ring * 131L), col * 0.85f) - 0.5f) * 0.38f;
        float rr = radius * (float) Math.sin(phi) * disp;
        float y = radius * (float) Math.cos(phi) * (0.9f + 0.2f * hash1(seed, 500 + ring));
        return new float[]{(float) Math.cos(ang) * rr, y, (float) Math.sin(ang) * rr};
    }

    /**
     * One vortex band between y0 and y1 with per-column radius jitter, a
     * height twist, and seeded gaps where the band is broken. The bespoke
     * replacement for equal-segment stacked rings.
     */
    public static void brokenVortexBand(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                                        float radius, float y0, float y1, float twist,
                                        float r, float g, float b, float alpha,
                                        float distScale) {
        if (alpha <= 0.01f) return;
        Matrix4f mat = pose.pose();
        int cols = FxEnvelope.segments(26, distScale, 10);
        for (int i = 0; i < cols; i++) {
            if (hash1(seed, 700 + i) < 0.16f) continue; // broken gap
            float a0 = (float) (Math.PI * 2.0 * i / cols) + twist;
            float a1 = (float) (Math.PI * 2.0 * (i + 1) / cols) + twist;
            float rad0 = radius * (0.8f + 0.45f * noise1(seed, i * 0.8f));
            float rad1 = radius * (0.8f + 0.45f * noise1(seed, (i + 1) * 0.8f));
            float ta0 = a0 + 0.4f, ta1 = a1 + 0.4f;
            float a = alpha * (0.65f + 0.55f * hash1(seed, 900 + i));
            quad(buffer, mat, pose,
                (float) Math.cos(a0) * rad0, y0, (float) Math.sin(a0) * rad0,
                (float) Math.cos(a1) * rad1, y0, (float) Math.sin(a1) * rad1,
                (float) Math.cos(ta1) * rad1 * 0.82f, y1, (float) Math.sin(ta1) * rad1 * 0.82f,
                (float) Math.cos(ta0) * rad0 * 0.82f, y1, (float) Math.sin(ta0) * rad0 * 0.82f,
                r, g, b, a, a * 0.55f);
        }
    }

    /**
     * A ground glow fan with a jittered rim: bright at the center, dying to
     * a tenth at the edge, each rim point pushed in or out by seeded noise so
     * the pool of light is a splash, not a disc. The bespoke anchor glow.
     */
    public static void glowFan(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                               float radius, float y,
                               float r, float g, float b, float alpha,
                               float distScale) {
        if (alpha <= 0.01f) return;
        Matrix4f mat = pose.pose();
        int segments = FxEnvelope.segments(22, distScale, 10);
        for (int i = 0; i < segments; i++) {
            float a0 = (float) (Math.PI * 2.0 * i / segments);
            float a1 = (float) (Math.PI * 2.0 * (i + 1) / segments);
            float rad0 = radius * (0.78f + 0.5f * noise1(seed, i * 0.85f));
            float rad1 = radius * (0.78f + 0.5f * noise1(seed, (i + 1) * 0.85f));
            float rimA = alpha * 0.1f * (0.6f + 0.7f * hash1(seed, 1500 + i));
            float x0 = (float) Math.cos(a0) * rad0, z0 = (float) Math.sin(a0) * rad0;
            float x1 = (float) Math.cos(a1) * rad1, z1 = (float) Math.sin(a1) * rad1;
            v(buffer, mat, pose, 0, y, 0, r, g, b, alpha, 0.5f, 0.5f, 1);
            v(buffer, mat, pose, x0, y, z0, r, g, b, rimA, 0, 0, 1);
            v(buffer, mat, pose, x1, y, z1, r, g, b, rimA, 1, 0, 1);
            v(buffer, mat, pose, x1, y, z1, r, g, b, rimA, 1, 0, -1);
            v(buffer, mat, pose, x0, y, z0, r, g, b, rimA, 0, 0, -1);
            v(buffer, mat, pose, 0, y, 0, r, g, b, alpha, 0.5f, 0.5f, -1);
        }
    }

    /**
     * A branching bolt ribbon from one point to another: midpoint-displaced
     * jagged polyline drawn as crossed ribbon strips so it reads from every
     * angle. Deterministic for a given seed; BoltRenderer-grade lightning
     * without a per-weapon copy.
     */
    public static void branch(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                              Vec3 from, Vec3 to, float width,
                              float r, float g, float b, float alpha,
                              float envelope, float distScale) {
        if (envelope <= 0.01f || alpha <= 0.01f) return;
        Matrix4f mat = pose.pose();
        int levels = Math.max(2, FxEnvelope.segments(4, distScale, 2));
        Vec3[] pts = {from, to};
        Rng rng = new Rng(seed);
        double disp = from.distanceTo(to) * 0.22;
        for (int level = 0; level < levels; level++) {
            Vec3[] next = new Vec3[pts.length * 2 - 1];
            for (int i = 0; i < pts.length; i++) {
                next[i * 2] = pts[i];
                if (i + 1 < pts.length) {
                    Vec3 a = pts[i], c = pts[i + 1];
                    Vec3 mid = a.add(c).scale(0.5);
                    Vec3 dir = c.subtract(a);
                    Vec3 perpA = dir.cross(new Vec3(0, 1, 0));
                    if (perpA.lengthSqr() < 1e-6) perpA = dir.cross(new Vec3(1, 0, 0));
                    perpA = perpA.normalize();
                    Vec3 perpB = dir.cross(perpA).normalize();
                    mid = mid.add(perpA.scale(rng.signed() * disp)).add(perpB.scale(rng.signed() * disp));
                    next[i * 2 + 1] = mid;
                }
            }
            pts = next;
            disp *= 0.55;
        }
        float a = alpha * envelope;
        for (int i = 0; i + 1 < pts.length; i++) {
            Vec3 p0 = pts[i], p1 = pts[i + 1];
            Vec3 dir = p1.subtract(p0);
            Vec3 perpA = dir.cross(new Vec3(0, 1, 0));
            if (perpA.lengthSqr() < 1e-6) perpA = dir.cross(new Vec3(1, 0, 0));
            perpA = perpA.normalize().scale(width * 0.5);
            Vec3 perpB = dir.cross(perpA).normalize().scale(width * 0.5);
            strip(buffer, mat, pose, p0, p1, perpA, r, g, b, a);
            strip(buffer, mat, pose, p0, p1, perpB, r, g, b, a);
        }
    }

    private static void strip(VertexConsumer buffer, Matrix4f mat, PoseStack.Pose pose,
                              Vec3 p0, Vec3 p1, Vec3 off, float r, float g, float b, float a) {
        quad(buffer, mat, pose,
            (float) (p0.x - off.x), (float) (p0.y - off.y), (float) (p0.z - off.z),
            (float) (p0.x + off.x), (float) (p0.y + off.y), (float) (p0.z + off.z),
            (float) (p1.x + off.x), (float) (p1.y + off.y), (float) (p1.z + off.z),
            (float) (p1.x - off.x), (float) (p1.y - off.y), (float) (p1.z - off.z),
            r, g, b, a, a);
    }

    /**
     * A star of irregular ground cracks radiating from the origin: each crack
     * is a wandering, tapering ribbon a few segments long, some with a short
     * side branch. Drawn just above ground level in local space.
     */
    public static void crackStar(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                                 float radius, float r, float g, float b, float alpha,
                                 float envelope, float distScale) {
        if (envelope <= 0.01f || alpha <= 0.01f) return;
        Matrix4f mat = pose.pose();
        Rng rng = new Rng(seed);
        int cracks = 6 + (int) (rng.nextFloat() * 4f);
        int segs = FxEnvelope.segments(5, distScale, 3);
        for (int k = 0; k < cracks; k++) {
            float ang = (float) (Math.PI * 2.0 * k / cracks) + rng.signed() * 0.35f;
            float px = 0, pz = 0;
            for (int s = 0; s < segs; s++) {
                float t = s / (float) segs;
                float stepLen = radius / segs * rng.range(0.65f, 1.3f);
                ang += rng.signed() * 0.3f;
                float nx = px + (float) Math.cos(ang) * stepLen;
                float nz = pz + (float) Math.sin(ang) * stepLen;
                float w0 = 0.055f * radius * (1f - t * 0.7f);
                float w1 = 0.055f * radius * (1f - (t + 1f / segs) * 0.7f);
                float perpX = (float) -Math.sin(ang), perpZ = (float) Math.cos(ang);
                float a = alpha * envelope * (1f - t * 0.75f);
                quad(buffer, mat, pose,
                    px - perpX * w0, 0.03f, pz - perpZ * w0,
                    px + perpX * w0, 0.03f, pz + perpZ * w0,
                    nx + perpX * w1, 0.03f, nz + perpZ * w1,
                    nx - perpX * w1, 0.03f, nz - perpZ * w1,
                    r, g, b, a, a * 0.8f);
                px = nx; pz = nz;
            }
        }
    }

    /**
     * A plume: a stack of uneven puff discs leaning with height, radii
     * bulging mid stack, rims jittered per point. For smoke columns, gouts
     * of flame, and eruption crowns where a particle emitter is too thin.
     */
    public static void plume(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                             float radius, float height,
                             float r, float g, float b, float alpha,
                             float envelope, float distScale) {
        if (envelope <= 0.01f || alpha <= 0.01f) return;
        Matrix4f mat = pose.pose();
        int levels = 5;
        int rim = FxEnvelope.segments(12, distScale, 7);
        for (int j = 0; j < levels; j++) {
            float t = levels == 1 ? 0f : j / (float) (levels - 1);
            float y = t * height;
            float rad = radius * (0.5f + 0.8f * (float) Math.sin(Math.PI * (0.15f + 0.7f * t)))
                * (0.85f + 0.3f * hash1(seed, 1100 + j));
            float cx = (noise1(seed ^ 0xC0FFEE, j * 1.7f) - 0.5f) * radius * 0.55f * t;
            float cz = (noise1(seed ^ 0xBEEF, j * 1.7f) - 0.5f) * radius * 0.55f * t;
            float a = alpha * envelope * (1f - t * 0.72f);
            float[] xs = new float[rim], zs = new float[rim];
            for (int i = 0; i < rim; i++) {
                float ang = (float) (Math.PI * 2.0 * i / rim) + (hash1(seed, 1200 + j * 31 + i) - 0.5f) * 0.3f;
                float rr = rad * (0.72f + 0.55f * noise1(seed ^ (j * 77L), i * 0.9f));
                xs[i] = cx + (float) Math.cos(ang) * rr;
                zs[i] = cz + (float) Math.sin(ang) * rr;
            }
            for (int i = 0; i < rim; i++) {
                int n = (i + 1) % rim;
                tri(buffer, mat, pose, cx, y, cz, xs[i], y, zs[i], xs[n], y, zs[n], r, g, b, a);
            }
        }
    }
}
