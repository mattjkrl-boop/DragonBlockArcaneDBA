package com.dragonblockarcanedba.client.render.ki;

import com.dragonblockarcanedba.util.FxEnvelope;
import com.dragonblockarcanedba.util.SeedHash;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Complex pieces toolkit for the complex effects redo (COMPLEX-STANDARD).
 *
 * Every builder is seeded: the same seed draws the same piece on every
 * client, so seeds come from the entity's synced state, exactly as the
 * material emission does. Builders draw in the caller's local space and
 * return the number of vertices they emitted, so owners and the merge can
 * check the class budgets in the standard.
 *
 * Two rendering disciplines, wired by the submit helpers below:
 *
 * - Energy and fire pieces (branchedLightning, tendril, flameTongues,
 *   crackFan) go through submitEnergy or submitVeil onto the DbaShaders
 *   dissolve and fresnel pattern. Vertex alpha is the shader's life
 *   input, so those builders apply the life remap internally: peak alpha
 *   times envelope, boosted and clamped, so a body sits intact through
 *   build and hold and dissolves only in the decay tail. Owners pass
 *   honest peak alphas (0.85 to 1.0 for bodies, lower for wisps).
 * - Solid pieces (shard, shardBurst, fracturePlates) go through
 *   submitSolid onto the plain ki type, or submitSolidLit when the
 *   piece is physical matter in the world. Their read is faceted
 *   vertex shading over a root to tip gradient; alpha is literal
 *   opacity.
 *
 * Nothing here draws a plain primitive: no smooth boxes, cylinders,
 * discs, domes, spheres, helixes, or clean ribbons. The per builder
 * complexity arguments live in audits/COMPLEX-STANDARD.md.
 */
public final class ComplexGeometry {

    private ComplexGeometry() {}

    // ---------------------------------------------------------------- RNG

    private static long mix(long z) {
        return SeedHash.mix(z);
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

        public int nextInt(int bound) {
            return bound <= 1 ? 0 : (int) (nextFloat() * bound);
        }

        public float range(float a, float b) {
            return a + (b - a) * nextFloat();
        }

        /** -1..1 */
        public float signed() {
            return nextFloat() * 2f - 1f;
        }
    }

    /** Lattice hash in 0..1 for (seed, index). Delegates to the common
     * SeedHash so server code reproduces the identical values. */
    private static float hash1(long seed, int i) {
        return SeedHash.hash1(seed, i);
    }

    /** Smooth value noise in 0..1, stable for a given seed. */
    private static float noise1(long seed, float x) {
        return SeedHash.noise1(seed, x);
    }

    // ---------------------------------------------------------------- submit helpers

    /** One piece of geometry, drawn into the caller's pose. */
    @FunctionalInterface
    public interface Piece {
        void draw(PoseStack.Pose pose, VertexConsumer buffer);
    }

    /** Energy or fire piece on the additive shader, white canvas. One call. */
    public static void submitEnergy(SubmitNodeCollector collector, PoseStack poseStack, Piece piece) {
        collector.submitCustomGeometry(poseStack, DbaShaders.additiveType(), piece::draw);
    }

    /** Energy or fire piece on the additive shader, sampling the given canvas texture. One call. */
    public static void submitEnergy(SubmitNodeCollector collector, PoseStack poseStack, Identifier texture, Piece piece) {
        collector.submitCustomGeometry(poseStack, DbaShaders.additiveType(texture), piece::draw);
    }

    /** Soft energy piece on the alpha shader variant, white canvas. One call. */
    public static void submitVeil(SubmitNodeCollector collector, PoseStack poseStack, Piece piece) {
        collector.submitCustomGeometry(poseStack, DbaShaders.alphaType(), piece::draw);
    }

    /** Soft energy piece on the alpha shader variant, sampling the given canvas texture. One call. */
    public static void submitVeil(SubmitNodeCollector collector, PoseStack poseStack, Identifier texture, Piece piece) {
        collector.submitCustomGeometry(poseStack, DbaShaders.alphaType(texture), piece::draw);
    }

    /** Solid fragment piece on the plain ki type, faceted vertex shading. One call. */
    public static void submitSolid(SubmitNodeCollector collector, PoseStack poseStack, Piece piece) {
        RenderType type = KiRenderHelper.kiRenderType();
        collector.submitCustomGeometry(poseStack, type, piece::draw);
    }

    /**
     * Solid piece for a PHYSICAL object (a real entity shard or fragment
     * that flies, lands, and rests in the world): same builders, same
     * faceted vertex shading, but drawn on the world-lit ki type with the
     * entity's packed light coords, so the piece is lit like everything
     * else in the world instead of glowing at full brightness. Energy
     * pieces keep submitSolid; physical matter uses this one. The light
     * is applied inside the draw callback (see KiRenderHelper.withLight),
     * so it is correct whenever the submit is consumed.
     */
    public static void submitSolidLit(SubmitNodeCollector collector, PoseStack poseStack, int lightCoords, Piece piece) {
        RenderType type = KiRenderHelper.kiLitType();
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) ->
            KiRenderHelper.withLight(lightCoords, () -> piece.draw(pose, buffer)));
    }

    // ---------------------------------------------------------------- shared math

    /** Shader life remap: bodies stay intact through build and hold, dissolve in the tail. */
    private static float life(float alpha, float envelope) {
        return Math.min(1f, alpha * envelope * 1.45f);
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private static float[] gradient(int fromArgb, int toArgb, float t) {
        float r = KiRenderHelper.red(fromArgb) + (KiRenderHelper.red(toArgb) - KiRenderHelper.red(fromArgb)) * t;
        float g = KiRenderHelper.green(fromArgb) + (KiRenderHelper.green(toArgb) - KiRenderHelper.green(fromArgb)) * t;
        float b = KiRenderHelper.blue(fromArgb) + (KiRenderHelper.blue(toArgb) - KiRenderHelper.blue(fromArgb)) * t;
        return new float[]{r, g, b};
    }

    private static float[] shade(float[] rgb, float brightness) {
        return new float[]{Math.min(1f, rgb[0] * brightness), Math.min(1f, rgb[1] * brightness),
                Math.min(1f, rgb[2] * brightness)};
    }

    /** A unit vector perpendicular to dir, stable for a given dir. */
    private static Vec3 perpOf(Vec3 dir) {
        Vec3 ref = Math.abs(dir.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 p = dir.cross(ref);
        double len = p.length();
        return len < 1e-6 ? new Vec3(1, 0, 0) : p.scale(1.0 / len);
    }

    /** A random unit vector perpendicular to dir. */
    private static Vec3 randomPerp(Vec3 dir, Rng rng) {
        Vec3 a = perpOf(dir);
        Vec3 b = dir.cross(a);
        double ang = rng.nextFloat() * Math.PI * 2.0;
        return a.scale(Math.cos(ang)).add(b.scale(Math.sin(ang))).normalize();
    }

    /** Rodrigues rotation of vec around a unit axis. */
    private static Vec3 rotate(Vec3 vec, Vec3 axis, double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        return vec.scale(cos)
                .add(axis.cross(vec).scale(sin))
                .add(axis.scale(axis.dot(vec) * (1.0 - cos)));
    }

    /** dir rotated away from axis by angle radians toward a random perpendicular. */
    private static Vec3 coneDir(Vec3 axis, double angle, Rng rng) {
        Vec3 perp = randomPerp(axis, rng);
        return axis.scale(Math.cos(angle)).add(perp.scale(Math.sin(angle))).normalize();
    }

    private static Vec3 pointAt(List<Vec3> pts, float t) {
        if (pts.size() == 1) return pts.get(0);
        float f = clamp01(t) * (pts.size() - 1);
        int i = Math.min(pts.size() - 2, (int) Math.floor(f));
        return pts.get(i).lerp(pts.get(i + 1), f - i);
    }

    // ---------------------------------------------------------------- emit helpers

    private static void vtx(VertexConsumer buffer, Matrix4f mat, PoseStack.Pose pose, Vec3 p,
                            float[] rgb, float a, float u, float v, Vec3 n) {
        KiRenderHelper.vertex(buffer, mat, pose, (float) p.x, (float) p.y, (float) p.z,
                rgb[0], rgb[1], rgb[2], a, u, v, (float) n.x, (float) n.y, (float) n.z);
    }

    /** Double sided quad. Corners carry their own color and alpha; uv runs (0,0) (1,0) (1,1) (0,1). */
    private static int quad(VertexConsumer buffer, Matrix4f mat, PoseStack.Pose pose,
                            Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                            float[] c0, float a0, float[] c1, float a1,
                            float[] c2, float a2, float[] c3, float a3, Vec3 n) {
        return quadUv(buffer, mat, pose, p0, p1, p2, p3, c0, a0, c1, a1, c2, a2, c3, a3, n,
                0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f);
    }

    /** Double sided quad with explicit corner uvs, for the textured builders. */
    private static int quadUv(VertexConsumer buffer, Matrix4f mat, PoseStack.Pose pose,
                              Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                              float[] c0, float a0, float[] c1, float a1,
                              float[] c2, float a2, float[] c3, float a3, Vec3 n,
                              float u0, float v0, float u1, float v1,
                              float u2, float v2, float u3, float v3) {
        vtx(buffer, mat, pose, p0, c0, a0, u0, v0, n);
        vtx(buffer, mat, pose, p1, c1, a1, u1, v1, n);
        vtx(buffer, mat, pose, p2, c2, a2, u2, v2, n);
        vtx(buffer, mat, pose, p3, c3, a3, u3, v3, n);
        Vec3 rn = n.scale(-1.0);
        vtx(buffer, mat, pose, p3, c3, a3, u3, v3, rn);
        vtx(buffer, mat, pose, p2, c2, a2, u2, v2, rn);
        vtx(buffer, mat, pose, p1, c1, a1, u1, v1, rn);
        vtx(buffer, mat, pose, p0, c0, a0, u0, v0, rn);
        return 8;
    }

    /** Double sided quad with one color and a start and end alpha, for ribbon segments. */
    private static int quad(VertexConsumer buffer, Matrix4f mat, PoseStack.Pose pose,
                            Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                            float[] rgb, float aStart, float aEnd, Vec3 n) {
        return quad(buffer, mat, pose, p0, p1, p2, p3, rgb, aStart, rgb, aStart, rgb, aEnd, rgb, aEnd, n);
    }

    /** Double sided triangle with one color and alpha. */
    private static int tri(VertexConsumer buffer, Matrix4f mat, PoseStack.Pose pose,
                           Vec3 p0, Vec3 p1, Vec3 p2, float[] rgb, float a, Vec3 n) {
        vtx(buffer, mat, pose, p0, rgb, a, 0f, 0f, n);
        vtx(buffer, mat, pose, p1, rgb, a, 1f, 0f, n);
        vtx(buffer, mat, pose, p2, rgb, a, 0.5f, 1f, n);
        Vec3 rn = n.scale(-1.0);
        vtx(buffer, mat, pose, p2, rgb, a, 0.5f, 1f, rn);
        vtx(buffer, mat, pose, p1, rgb, a, 1f, 0f, rn);
        vtx(buffer, mat, pose, p0, rgb, a, 0f, 0f, rn);
        return 6;
    }

    // ---------------------------------------------------------------- branched lightning

    private record Polyline(List<Vec3> points, int order) {}

    /**
     * A fully branched bolt from one point to another: a midpoint displaced
     * trunk (3 to 5 displacement levels), 2 to 6 seeded forks leaving the
     * trunk at 25 to 55 degrees, and 0 to 2 sub-branches per fork. Every
     * polyline tapers, jitters width per segment, and is drawn twice, a wide
     * dim sheath and a narrow near-white core, each as crossed ribbon pairs.
     * Draw through submitEnergy. coreArgb is usually near white; glowArgb is
     * the theme color.
     */
    public static int branchedLightning(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                                        Vec3 from, Vec3 to, float width,
                                        int coreArgb, int glowArgb,
                                        float alpha, float envelope, float distScale) {
        if (envelope <= 0.01f || alpha <= 0.01f || width <= 0f) return 0;
        Matrix4f mat = pose.pose();
        Rng rng = new Rng(seed);
        double totalLen = from.distanceTo(to);
        if (totalLen < 0.05) return 0;

        // Trunk: iterative midpoint displacement, jagged at several scales.
        List<Vec3> trunk = new ArrayList<>();
        trunk.add(from);
        trunk.add(to);
        int levels = distScale >= 0.85f ? 5 : distScale >= 0.55f ? 4 : 3;
        double amp = totalLen * 0.16;
        for (int level = 0; level < levels; level++) {
            List<Vec3> next = new ArrayList<>(trunk.size() * 2 - 1);
            for (int i = 0; i < trunk.size(); i++) {
                next.add(trunk.get(i));
                if (i + 1 < trunk.size()) {
                    Vec3 a = trunk.get(i), b = trunk.get(i + 1);
                    Vec3 dir = b.subtract(a);
                    Vec3 pa = perpOf(dir);
                    Vec3 pb = dir.cross(pa).normalize();
                    Vec3 mid = a.add(b).scale(0.5)
                            .add(pa.scale(rng.signed() * amp))
                            .add(pb.scale(rng.signed() * amp));
                    next.add(mid);
                }
            }
            trunk = next;
            amp *= 0.52;
        }

        List<Polyline> lines = new ArrayList<>();
        lines.add(new Polyline(trunk, 0));
        List<Polyline> forks = addForks(rng, trunk, totalLen, to.subtract(from), distScale, lines);
        addSubBranches(rng, forks, lines);
        return drawBoltLines(pose, buffer, seed, lines, width, coreArgb, glowArgb, alpha, envelope);
    }

    /**
     * The same bolt drawn over a caller-supplied path: the trunk polyline
     * is drawn exactly as given, with no midpoint displacement of its own,
     * so a caller whose emission already walks a seeded path (the shared
     * Bolt, CurseLightning) draws its geometry over the identical line and
     * the two layers coincide by construction. Supplied branches are drawn
     * exactly as given at fork order; seeded sub-branches grow off them.
     * When no branches are supplied, seeded forks and sub-branches grow
     * off the supplied trunk exactly as the generating overload grows
     * them. Taper, per-segment width jitter, the sheath and core crossed
     * ribbon passes, and the shader life remap are the shared draw below.
     * Points are in the same local space the pose submits in.
     */
    public static int branchedLightning(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                                        List<Vec3> trunk, List<List<Vec3>> branches,
                                        float width, int coreArgb, int glowArgb,
                                        float alpha, float envelope, float distScale) {
        if (envelope <= 0.01f || alpha <= 0.01f || width <= 0f) return 0;
        if (trunk == null || trunk.size() < 2) return 0;
        List<Polyline> lines = new ArrayList<>();
        lines.add(new Polyline(List.copyOf(trunk), 0));
        List<Polyline> forks = new ArrayList<>();
        if (branches != null) {
            for (List<Vec3> branch : branches) {
                if (branch == null || branch.size() < 2) continue;
                Polyline fork = new Polyline(List.copyOf(branch), 1);
                forks.add(fork);
                lines.add(fork);
            }
        }
        Rng rng = new Rng(seed);
        if (forks.isEmpty()) {
            Vec3 axis = trunk.get(trunk.size() - 1).subtract(trunk.get(0));
            forks = addForks(rng, trunk, forkSpan(trunk), axis, distScale, lines);
        }
        addSubBranches(rng, forks, lines);
        return drawBoltLines(pose, buffer, seed, lines, width, coreArgb, glowArgb, alpha, envelope);
    }

    /** Seeded forks leaving a trunk at 25 to 55 degrees; appended to lines and returned. */
    private static List<Polyline> addForks(Rng rng, List<Vec3> trunk, double totalLen, Vec3 axisFallback,
                                           float distScale, List<Polyline> lines) {
        int forkCount = Math.max(2, Math.round((2.5f + rng.nextFloat() * 3f) * (0.5f + 0.5f * distScale)));
        List<Polyline> forks = new ArrayList<>();
        for (int f = 0; f < forkCount; f++) {
            float t = 0.15f + 0.65f * rng.nextFloat();
            Vec3 start = pointAt(trunk, t);
            Vec3 ahead = pointAt(trunk, Math.min(1f, t + 0.05f));
            Vec3 tangent = ahead.subtract(start);
            if (tangent.lengthSqr() < 1e-8) tangent = axisFallback;
            tangent = tangent.normalize();
            double branchAng = 0.45 + rng.nextFloat() * 0.5;
            Vec3 dir = coneDir(tangent, branchAng, rng);
            double forkLen = totalLen * (0.22 + 0.30 * rng.nextFloat()) * (1.0 - 0.4 * t);
            Polyline fork = new Polyline(walk(rng, start, dir, forkLen, 5 + rng.nextInt(4), 0.38), 1);
            forks.add(fork);
            lines.add(fork);
        }
        return forks;
    }

    /** Seeded sub-branches off each fork, 0 to 2 per fork; appended to lines. */
    private static void addSubBranches(Rng rng, List<Polyline> forks, List<Polyline> lines) {
        for (Polyline fork : forks) {
            float roll = rng.nextFloat();
            int subs = roll < 0.45f ? 0 : roll < 0.82f ? 1 : 2;
            for (int s = 0; s < subs; s++) {
                float t = 0.3f + 0.4f * rng.nextFloat();
                Vec3 start = pointAt(fork.points(), t);
                Vec3 ahead = pointAt(fork.points(), Math.min(1f, t + 0.08f));
                Vec3 tangent = ahead.subtract(start);
                if (tangent.lengthSqr() < 1e-8) continue;
                tangent = tangent.normalize();
                Vec3 dir = coneDir(tangent, 0.5 + rng.nextFloat() * 0.5, rng);
                double subLen = forkSpan(fork.points()) * (0.30 + 0.15 * rng.nextFloat());
                lines.add(new Polyline(walk(rng, start, dir, subLen, 3 + rng.nextInt(3), 0.42), 2));
            }
        }
    }

    /** The shared bolt draw: two passes, sheath then core, crossed ribbon pairs per segment. */
    private static int drawBoltLines(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                                     List<Polyline> lines, float width,
                                     int coreArgb, int glowArgb, float alpha, float envelope) {
        Matrix4f mat = pose.pose();
        int verts = 0;
        for (int pass = 0; pass < 2; pass++) {
            boolean core = pass == 1;
            float wScale = core ? 1.0f : 1.9f;
            float aMul = core ? 1.0f : 0.42f;
            int baseColor = core ? coreArgb : glowArgb;
            for (int li = 0; li < lines.size(); li++) {
                Polyline line = lines.get(li);
                float orderFactor = line.order() == 0 ? 1.0f : line.order() == 1 ? 0.5f : 0.27f;
                List<Vec3> pts = line.points();
                for (int i = 0; i + 1 < pts.size(); i++) {
                    Vec3 p0 = pts.get(i), p1 = pts.get(i + 1);
                    Vec3 segDir = p1.subtract(p0);
                    if (segDir.lengthSqr() < 1e-10) continue;
                    float t0 = i / (float) (pts.size() - 1);
                    float t1 = (i + 1) / (float) (pts.size() - 1);
                    float jitter = 0.8f + 0.45f * hash1(seed ^ (li * 131L), i * 7 + pass);
                    float w0 = width * orderFactor * (1f - 0.5f * t0) * jitter * wScale * 0.5f;
                    float w1 = width * orderFactor * (1f - 0.5f * t1) * jitter * wScale * 0.5f;
                    float bright = (1f - 0.35f * t0 - 0.15f * line.order());
                    float[] rgb = shade(new float[]{KiRenderHelper.red(baseColor),
                            KiRenderHelper.green(baseColor), KiRenderHelper.blue(baseColor)}, Math.max(0.2f, bright));
                    float a = life(alpha * aMul, envelope);
                    Vec3 sideA = perpOf(segDir);
                    Vec3 sideB = segDir.cross(sideA).normalize();
                    verts += quad(buffer, mat, pose,
                            p0.subtract(sideA.scale(w0)), p0.add(sideA.scale(w0)),
                            p1.add(sideA.scale(w1)), p1.subtract(sideA.scale(w1)),
                            rgb, a, a, sideB);
                    verts += quad(buffer, mat, pose,
                            p0.subtract(sideB.scale(w0)), p0.add(sideB.scale(w0)),
                            p1.add(sideB.scale(w1)), p1.subtract(sideB.scale(w1)),
                            rgb, a, a, sideA);
                }
            }
        }
        return verts;
    }

    /** A jittered walk: step forward, steering by small random perpendicular nudges. */
    private static List<Vec3> walk(Rng rng, Vec3 start, Vec3 dir, double length, int steps, double steer) {
        List<Vec3> pts = new ArrayList<>(steps + 1);
        pts.add(start);
        Vec3 pos = start;
        Vec3 d = dir;
        double stepLen = length / Math.max(1, steps);
        for (int i = 0; i < steps; i++) {
            Vec3 nudge = randomPerp(d, rng).scale(rng.signed() * steer);
            d = d.add(nudge).normalize();
            pos = pos.add(d.scale(stepLen));
            pts.add(pos);
        }
        return pts;
    }

    private static double forkSpan(List<Vec3> pts) {
        double total = 0;
        for (int i = 0; i + 1 < pts.size(); i++) total += pts.get(i).distanceTo(pts.get(i + 1));
        return total;
    }

    // ---------------------------------------------------------------- shards

    private static final float[] SHARD_RING_T = {0.10f, 0.45f, 0.76f};
    private static final float[] SHARD_RING_R = {0.55f, 1.0f, 0.60f};
    private static final int[] SHARD_RING_N = {5, 6, 5};

    /** One point on a shard ring, angle-interpolated between the ring's displaced control vertices. */
    private static Vec3 shardRingPoint(long seed, int ring, Vec3 origin, Vec3 axis, Vec3 u, Vec3 v,
                                       float girth, float length, float theta01) {
        int n = SHARD_RING_N[ring];
        float f = theta01 * n;
        int i0 = Math.min(n - 1, (int) Math.floor(f));
        int i1 = (i0 + 1) % n;
        float frac = f - i0;
        float smooth = frac * frac * (3f - 2f * frac);
        Vec3 p0 = shardControl(seed, ring, i0, origin, axis, u, v, girth, length);
        Vec3 p1 = shardControl(seed, ring, i1, origin, axis, u, v, girth, length);
        return p0.lerp(p1, smooth);
    }

    private static Vec3 shardControl(long seed, int ring, int j, Vec3 origin, Vec3 axis, Vec3 u, Vec3 v,
                                     float girth, float length) {
        int n = SHARD_RING_N[ring];
        long salt = seed ^ (ring * 0x51ED27L);
        float ang = (float) (Math.PI * 2.0 * j / n) + (hash1(salt, 31) - 0.5f) * 0.9f
                + (hash1(salt, j * 3 + 1) - 0.5f) * 0.35f;
        float radial = girth * SHARD_RING_R[ring] * (0.72f + 0.55f * noise1(salt, j * 1.7f));
        Vec3 radialVec = u.scale(Math.cos(ang)).add(v.scale(Math.sin(ang)));
        return origin.add(axis.scale(SHARD_RING_T[ring] * length)).add(radialVec.scale(radial));
    }

    /**
     * One irregular shard: a closed spindle of three displaced rings plus
     * root and tip, drawn along dir from origin. Every vertex is displaced
     * by seeded noise (0.72 to 1.27 of nominal radius), and every face
     * carries its own facet brightness step over the root to tip gradient,
     * so the chunk reads as fractured material. Draw through submitSolid.
     */
    public static int shard(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                            Vec3 origin, Vec3 dir, float length, float girth,
                            int rootArgb, int tipArgb, float alpha, float envelope) {
        if (envelope <= 0.01f || alpha <= 0.01f || length <= 0f || girth <= 0f) return 0;
        Matrix4f mat = pose.pose();
        Vec3 axis = dir.lengthSqr() < 1e-8 ? new Vec3(0, 1, 0) : dir.normalize();
        Vec3 u = perpOf(axis);
        Vec3 v = axis.cross(u).normalize();
        float a = alpha * envelope;
        Vec3 root = origin.subtract(axis.scale(length * 0.06));
        Vec3 tip = origin.add(axis.scale(length * (1.0f + 0.12f * hash1(seed, 77))));
        int verts = 0;
        int face = 0;
        int steps = 8;
        // Root fan.
        for (int k = 0; k < steps; k++) {
            Vec3 p0 = shardRingPoint(seed, 0, origin, axis, u, v, girth, length, k / (float) steps);
            Vec3 p1 = shardRingPoint(seed, 0, origin, axis, u, v, girth, length, (k + 1) / (float) steps);
            float[] rgb = shade(gradient(rootArgb, tipArgb, 0.05f), 0.68f + 0.5f * hash1(seed, face++ * 131 + 7));
            verts += tri(buffer, mat, pose, root, p1, p0, rgb, a, axis.scale(-1.0));
        }
        // Bands between rings.
        for (int ring = 0; ring + 1 < SHARD_RING_T.length; ring++) {
            float tMid = (SHARD_RING_T[ring] + SHARD_RING_T[ring + 1]) * 0.5f;
            for (int k = 0; k < steps; k++) {
                float f0 = k / (float) steps, f1 = (k + 1) / (float) steps;
                Vec3 a0 = shardRingPoint(seed, ring, origin, axis, u, v, girth, length, f0);
                Vec3 a1 = shardRingPoint(seed, ring, origin, axis, u, v, girth, length, f1);
                Vec3 b0 = shardRingPoint(seed, ring + 1, origin, axis, u, v, girth, length, f0);
                Vec3 b1 = shardRingPoint(seed, ring + 1, origin, axis, u, v, girth, length, f1);
                Vec3 n = a0.subtract(origin).normalize();
                float[] rgb = shade(gradient(rootArgb, tipArgb, tMid), 0.68f + 0.5f * hash1(seed, face++ * 131 + 7));
                verts += tri(buffer, mat, pose, a0, b0, a1, rgb, a, n);
                float[] rgb2 = shade(gradient(rootArgb, tipArgb, tMid), 0.68f + 0.5f * hash1(seed, face++ * 131 + 7));
                verts += tri(buffer, mat, pose, a1, b0, b1, rgb2, a, n);
            }
        }
        // Tip fan.
        int last = SHARD_RING_T.length - 1;
        for (int k = 0; k < steps; k++) {
            Vec3 p0 = shardRingPoint(seed, last, origin, axis, u, v, girth, length, k / (float) steps);
            Vec3 p1 = shardRingPoint(seed, last, origin, axis, u, v, girth, length, (k + 1) / (float) steps);
            float[] rgb = shade(gradient(rootArgb, tipArgb, 0.95f), 0.68f + 0.5f * hash1(seed, face++ * 131 + 7));
            verts += tri(buffer, mat, pose, tip, p0, p1, rgb, a, axis);
        }
        return verts;
    }

    /**
     * A burst of shards in a cone around axis, mixing three size classes in
     * one call: about 55 percent chips (0.3 to 0.55 of unit), 30 percent
     * shards (0.55 to 1.05), and 15 percent chunks (1.05 to 1.6), each with
     * its own direction, roll, and silhouette. unit is the shard class
     * scale in blocks. Draw through submitSolid.
     */
    public static int shardBurst(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                                 Vec3 origin, Vec3 axis, float spreadDeg, float unit, int count,
                                 int rootArgb, int tipArgb, float alpha, float envelope, float distScale) {
        if (envelope <= 0.01f || alpha <= 0.01f || count <= 0 || unit <= 0f) return 0;
        Rng rng = new Rng(seed);
        int n = FxEnvelope.segments(count, distScale, 1);
        Vec3 ax = axis.lengthSqr() < 1e-8 ? new Vec3(0, 1, 0) : axis.normalize();
        double spread = Math.toRadians(spreadDeg);
        int verts = 0;
        for (int i = 0; i < n; i++) {
            double ang = spread * Math.sqrt(rng.nextFloat());
            Vec3 dirI = coneDir(ax, ang, rng);
            float roll = rng.nextFloat();
            float len = unit * (roll < 0.55f ? rng.range(0.30f, 0.55f)
                    : roll < 0.85f ? rng.range(0.55f, 1.05f)
                    : rng.range(1.05f, 1.60f));
            float girth = len * rng.range(0.30f, 0.44f);
            Vec3 o = origin.add(randomPerp(ax, rng).scale(rng.nextFloat() * unit * 0.12));
            verts += shard(pose, buffer, seed + i * 0x9E37L, o, dirI, len, girth,
                    rootArgb, tipArgb, alpha, envelope);
        }
        return verts;
    }

    // ---------------------------------------------------------------- fracture plates

    private static Vec3 polar(float ang, float radius, float y) {
        return new Vec3(Math.cos(ang) * radius, y, Math.sin(ang) * radius);
    }

    /**
     * A fan of fractured ground plates radiating from the local origin in
     * the XZ plane: six to twelve slabs with jittered spans, seeded gaps
     * (about 15 percent missing), raised or sunken centers, tilted corners,
     * and skirts down to the ground on the outer and radial edges, so each
     * plate has real thickness. Facet brightness steps over a hot center to
     * dark rim gradient. Draw through submitSolid.
     */
    public static int fracturePlates(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                                     float radius, float lift,
                                     int hotArgb, int darkArgb,
                                     float alpha, float envelope, float distScale) {
        if (envelope <= 0.01f || alpha <= 0.01f || radius <= 0f) return 0;
        Matrix4f mat = pose.pose();
        Rng rng = new Rng(seed);
        float a = alpha * envelope;
        int plates = FxEnvelope.segments(10, distScale, 6);
        int verts = 0;
        int face = 0;
        float cursor = rng.nextFloat() * (float) Math.PI * 2f;
        Vec3 up = new Vec3(0, 1, 0);
        for (int i = 0; i < plates; i++) {
            float span = (float) (Math.PI * 2.0 / plates) * rng.range(0.75f, 1.25f);
            float a0 = cursor + span * 0.08f;
            float a1 = cursor + span * 0.92f;
            cursor += span;
            if (hash1(seed, 400 + i) < 0.15f) continue; // missing plate: a gap in the fan
            float rIn = radius * rng.range(0.06f, 0.11f);
            float rOut = radius * rng.range(0.72f, 1.10f);
            float hIn = lift * rng.range(0.70f, 1.10f);
            float hInB = hIn * rng.range(0.85f, 1.15f);
            float hOutA = lift * rng.range(0.05f, 0.40f);
            float hOutB = lift * rng.range(0.05f, 0.40f);
            if (hash1(seed, 430 + i) < 0.30f) hOutA = 0.02f; // one dropped corner
            if (hash1(seed, 460 + i) < 0.30f) hOutB = 0.02f;
            float hC = hash1(seed, 490 + i) < 0.25f ? lift * 0.40f : lift * rng.range(0.85f, 1.30f);
            float midAng = (a0 + a1) * 0.5f;
            Vec3 c0 = polar(a0, rIn, hIn);
            Vec3 c1 = polar(a1, rIn, hInB);
            Vec3 c2 = polar(a1, rOut, hOutB);
            Vec3 c3 = polar(a0, rOut, hOutA);
            Vec3 m = polar(midAng, (rIn + rOut) * 0.5f, hC);
            Vec3[][] top = {{c0, c1, m}, {c1, c2, m}, {c2, c3, m}, {c3, c0, m}};
            for (Vec3[] t : top) {
                double centroidR = Math.sqrt(
                        Math.pow((t[0].x + t[1].x + t[2].x) / 3.0, 2)
                                + Math.pow((t[0].z + t[1].z + t[2].z) / 3.0, 2));
                float grad = clamp01((float) (centroidR / radius));
                float[] rgb = shade(gradient(hotArgb, darkArgb, grad), 0.70f + 0.45f * hash1(seed, face++ * 131 + 7));
                verts += tri(buffer, mat, pose, t[0], t[1], t[2], rgb, a, up);
            }
            // Skirts: outer edge and both radial edges down to the ground.
            Vec3 c2d = new Vec3(c2.x, 0.02, c2.z);
            Vec3 c3d = new Vec3(c3.x, 0.02, c3.z);
            Vec3 c0d = new Vec3(c0.x, 0.02, c0.z);
            Vec3 c1d = new Vec3(c1.x, 0.02, c1.z);
            float[] skirtRgb = shade(gradient(hotArgb, darkArgb, 0.85f), 0.55f + 0.3f * hash1(seed, face++ * 131 + 7));
            Vec3 outN = polar(midAng, 1f, 0f).normalize();
            verts += quad(buffer, mat, pose, c3, c2, c2d, c3d, skirtRgb, a, a, outN);
            Vec3 sideN0 = polar(a0 + (float) Math.PI / 2f, 1f, 0f).normalize().scale(-1.0);
            verts += quad(buffer, mat, pose, c0, c3, c3d, c0d, skirtRgb, a, a, sideN0);
            Vec3 sideN1 = polar(a1 + (float) Math.PI / 2f, 1f, 0f).normalize();
            verts += quad(buffer, mat, pose, c1, c2, c2d, c1d, skirtRgb, a, a, sideN1);
        }
        return verts;
    }

    // ---------------------------------------------------------------- crack fan

    /**
     * A fan of wandering energy cracks in the XZ plane at local y 0.03:
     * seven to eleven cracks, each walking four to six segments with seeded
     * angle wander and width that pinches at seeded points instead of
     * tapering smoothly, with a 35 percent chance of one fork per crack.
     * Color runs hot at the center to the theme color at the tips. Draw
     * through submitEnergy.
     *
     * Radius is a walk length budget, not a hard disc bound: each step is
     * radius divided by segments times a seeded 0.60 to 1.35 factor, so a
     * crack that rolls near straight at the top of the range reaches about
     * 1.35 times the nominal radius, and a fork runs a further 0.45 of the
     * radius past its origin partway out. A telegraph whose drawn size
     * must equal its hit size should scale the radius down to compensate
     * (the Azure telegraph uses 0.78) instead of expecting a clamp here.
     */
    public static int crackFan(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                               float radius, int hotArgb, int coolArgb,
                               float alpha, float envelope, float distScale) {
        return crackFanImpl(pose, buffer, seed, radius, hotArgb, coolArgb,
                alpha, envelope, distScale, 9, true);
    }

    /**
     * Explicit-count variant (attack upgrade merge): the fan draws
     * baseCracks cracks at full detail instead of the standard nine
     * plus density jitter, for hosts whose spec names a small exact
     * count (a short fan dressing a slash line or a dash mark). The
     * deterministic fork replay contract is unchanged: each crack's
     * walk and fork seeds derive from the fan seed and the crack index
     * alone, so a fork origin recomputes exactly. Distance scaling
     * still applies through FxEnvelope.segments with a floor of
     * baseCracks minus two (never below two).
     */
    public static int crackFan(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                               float radius, int hotArgb, int coolArgb,
                               float alpha, float envelope, float distScale, int baseCracks) {
        return crackFanImpl(pose, buffer, seed, radius, hotArgb, coolArgb,
                alpha, envelope, distScale, baseCracks, false);
    }

    private static int crackFanImpl(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                                    float radius, int hotArgb, int coolArgb,
                                    float alpha, float envelope, float distScale,
                                    int baseCracks, boolean densityJitter) {
        if (envelope <= 0.01f || alpha <= 0.01f || radius <= 0f) return 0;
        Matrix4f mat = pose.pose();
        Rng rng = new Rng(seed);
        int cracks = FxEnvelope.segments(baseCracks, distScale,
                Math.max(2, baseCracks - (densityJitter ? 3 : 2)))
                + (densityJitter ? rng.nextInt(3) : 0);
        int verts = 0;
        for (int k = 0; k < cracks; k++) {
            float ang = (float) (Math.PI * 2.0 * k / cracks) + rng.signed() * 0.30f;
            int segs = FxEnvelope.segments(6, distScale, 4);
            int forkAt = rng.nextFloat() < 0.35f ? 2 + rng.nextInt(Math.max(1, segs - 3)) : -1;
            verts += crackWalk(buffer, mat, pose, seed ^ (k * 0x2545L), new Vec3(0, 0.03, 0), ang,
                    segs, radius, 1.0f, hotArgb, coolArgb, alpha, envelope);
            if (forkAt >= 0) {
                // Recompute the fork origin by replaying the walk deterministically.
                Vec3 pos = new Vec3(0, 0.03, 0);
                Rng replay = new Rng(seed ^ (k * 0x2545L));
                float replayAng = ang;
                for (int s = 0; s < forkAt; s++) {
                    replayAng += replay.signed() * 0.38f;
                    float stepLen = radius / segs * replay.range(0.60f, 1.35f);
                    pos = pos.add(Math.cos(replayAng) * stepLen, 0, Math.sin(replayAng) * stepLen);
                }
                float forkAng = ang + (rng.nextFloat() < 0.5f ? 1f : -1f) * rng.range(0.5f, 0.9f);
                verts += crackWalk(buffer, mat, pose, seed ^ (k * 0x6A09L), pos, forkAng,
                        3, radius * 0.45f, 0.55f, hotArgb, coolArgb, alpha, envelope);
            }
        }
        return verts;
    }

    private static int crackWalk(VertexConsumer buffer, Matrix4f mat, PoseStack.Pose pose, long seed,
                                 Vec3 start, float startAng, int segs, float radius, float widthScale,
                                 int hotArgb, int coolArgb, float alpha, float envelope) {
        Rng rng = new Rng(seed);
        int verts = 0;
        Vec3 up = new Vec3(0, 1, 0);
        float ang = startAng;
        Vec3 pos = start;
        float prevW = crackWidth(seed, radius, widthScale, 0f, 0);
        for (int s = 0; s < segs; s++) {
            float t = s / (float) segs;
            ang += rng.signed() * 0.38f;
            float stepLen = radius / segs * rng.range(0.60f, 1.35f);
            Vec3 next = pos.add(Math.cos(ang) * stepLen, 0, Math.sin(ang) * stepLen);
            float w = crackWidth(seed, radius, widthScale, (s + 1) / (float) segs, s + 1);
            Vec3 perp = new Vec3(-Math.sin(ang), 0, Math.cos(ang));
            float[] rgb = gradient(hotArgb, coolArgb, t);
            float a0 = life(alpha, envelope) * (1f - 0.70f * t);
            float a1 = life(alpha, envelope) * (1f - 0.70f * (t + 1f / segs));
            verts += quad(buffer, mat, pose,
                    pos.subtract(perp.scale(prevW)), pos.add(perp.scale(prevW)),
                    next.add(perp.scale(w)), next.subtract(perp.scale(w)),
                    rgb, a0, rgb, a0, rgb, a1, rgb, a1, up);
            pos = next;
            prevW = w;
        }
        return verts;
    }

    private static float crackWidth(long seed, float radius, float widthScale, float t, int step) {
        float w = radius * 0.05f * (float) Math.pow(1f - t, 0.8) * widthScale
                * (0.7f + 0.6f * noise1(seed, step * 1.3f));
        if (hash1(seed, 900 + step) < 0.18f) w *= 0.25f; // seeded pinch
        return Math.max(0.004f, w);
    }

    // ---------------------------------------------------------------- tendrils

    /**
     * One energy filament on a quadratic bezier spine (from, control, to),
     * sampled at 12 to 28 points with curl offsets in 3D whose amplitude
     * grows mid-spine. The width swells, then tears: two seeded pinch
     * windows collapse it to 15 percent, so the strand reads as splitting
     * and rejoining. Drawn as a crossed ribbon pair whose planes twist
     * 1.5 to 3.5 radians along the length. Samples the tendril canvas
     * texture: submit through submitEnergy with ComplexTextures.TENDRIL.
     */
    public static int tendril(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                              Vec3 from, Vec3 control, Vec3 to, float width,
                              int rootArgb, int tipArgb,
                              float alpha, float envelope, float distScale) {
        if (envelope <= 0.01f || alpha <= 0.01f || width <= 0f) return 0;
        Matrix4f mat = pose.pose();
        Rng rng = new Rng(seed);
        Vec3 chord = to.subtract(from);
        double chordLen = chord.length();
        if (chordLen < 0.05) return 0;
        Vec3 axis = chord.scale(1.0 / chordLen);
        Vec3 u = perpOf(axis);
        Vec3 v = axis.cross(u).normalize();
        int n = FxEnvelope.segments(24, distScale, 12);
        float freq1 = 5f + 4f * rng.nextFloat();
        float freq2 = 4f + 5f * rng.nextFloat();
        float ph1 = rng.nextFloat() * (float) Math.PI * 2f;
        float ph2 = rng.nextFloat() * (float) Math.PI * 2f;
        float twistTotal = rng.range(1.5f, 3.5f) * (rng.nextFloat() < 0.5f ? -1f : 1f);
        float twistPhase = rng.nextFloat() * (float) Math.PI * 2f;
        float pinchA = rng.range(0.2f, 0.5f);
        float pinchB = rng.range(0.55f, 0.85f);

        Vec3[] points = new Vec3[n + 1];
        float[] widths = new float[n + 1];
        Vec3[] sideAs = new Vec3[n + 1];
        Vec3[] sideBs = new Vec3[n + 1];
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            double s = 1.0 - t;
            Vec3 base = from.scale(s * s).add(control.scale(2 * s * t)).add(to.scale(t * t));
            double curlAmp = chordLen * 0.055 * Math.sin(Math.PI * Math.min(1f, t * 1.12f));
            Vec3 off = u.scale(Math.sin(t * freq1 + ph1) * curlAmp)
                    .add(v.scale(Math.cos(t * freq2 + ph2) * curlAmp));
            points[i] = base.add(off);
            float swell = (float) Math.pow(Math.sin(Math.PI * (0.12 + 0.76 * t)), 0.65);
            float w = width * (0.30f + 0.85f * swell) * (1f - 0.55f * FxEnvelope.smoothstep((t - 0.75f) / 0.25f));
            w *= 0.78f + 0.5f * noise1(seed, t * 9f);
            w *= pinchFactor(t, pinchA) * pinchFactor(t, pinchB);
            widths[i] = Math.max(0.004f, w);
            double theta = twistPhase + t * twistTotal;
            sideAs[i] = rotate(u, axis, theta);
            sideBs[i] = rotate(v, axis, theta);
        }

        int verts = 0;
        for (int i = 0; i < n; i++) {
            float t0 = i / (float) n, t1 = (i + 1) / (float) n;
            float[] c0 = gradient(rootArgb, tipArgb, t0);
            float[] c1 = gradient(rootArgb, tipArgb, t1);
            float a0 = life(alpha, envelope) * (0.45f + 0.55f * Math.min(1f, widths[i] / width));
            float a1 = life(alpha, envelope) * (0.45f + 0.55f * Math.min(1f, widths[i + 1] / width));
            Vec3 p0 = points[i], p1 = points[i + 1];
            Vec3 sA0 = sideAs[i].scale(widths[i] * 0.5f), sA1 = sideAs[i + 1].scale(widths[i + 1] * 0.5f);
            Vec3 sB0 = sideBs[i].scale(widths[i] * 0.5f), sB1 = sideBs[i + 1].scale(widths[i + 1] * 0.5f);
            // The tendril canvas runs its length along u and its band across
            // v, so u follows the spine parameter and v crosses the strand.
            verts += quadUv(buffer, mat, pose,
                    p0.subtract(sA0), p0.add(sA0), p1.add(sA1), p1.subtract(sA1),
                    c0, a0, c0, a0, c1, a1, c1, a1, sideBs[i],
                    t0, 0f, t0, 1f, t1, 1f, t1, 0f);
            verts += quadUv(buffer, mat, pose,
                    p0.subtract(sB0), p0.add(sB0), p1.add(sB1), p1.subtract(sB1),
                    c0, a0, c0, a0, c1, a1, c1, a1, sideAs[i],
                    t0, 0f, t0, 1f, t1, 1f, t1, 0f);
        }
        return verts;
    }

    /** Width multiplier near a pinch center: 0.15 at the center, 1.0 beyond 0.05 of t away. */
    private static float pinchFactor(float t, float center) {
        float d = Math.abs(t - center) / 0.05f;
        return d >= 1f ? 1f : 0.15f + 0.85f * d;
    }

    /**
     * A fan of tendrils from one origin into a cone around dir: each strand
     * gets its own direction, length (0.8 to 1.2 of length), and a curled
     * control point, so the fan reads as separate lashing filaments, not
     * copies. Submit through submitEnergy with ComplexTextures.TENDRIL.
     */
    public static int tendrilFan(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                                 Vec3 origin, Vec3 dir, float spreadDeg, float length, int count,
                                 float width, int rootArgb, int tipArgb,
                                 float alpha, float envelope, float distScale) {
        if (envelope <= 0.01f || alpha <= 0.01f || count <= 0 || length <= 0f) return 0;
        Rng rng = new Rng(seed);
        int n = FxEnvelope.segments(count, distScale, 1);
        Vec3 ax = dir.lengthSqr() < 1e-8 ? new Vec3(0, 1, 0) : dir.normalize();
        double spread = Math.toRadians(spreadDeg);
        int verts = 0;
        for (int i = 0; i < n; i++) {
            Vec3 dirI = coneDir(ax, spread * Math.sqrt(rng.nextFloat()), rng);
            double lenI = length * rng.range(0.8f, 1.2f);
            Vec3 end = origin.add(dirI.scale(lenI));
            Vec3 control = origin.add(dirI.scale(lenI * 0.5))
                    .add(randomPerp(dirI, rng).scale(lenI * rng.range(0.12f, 0.32f)));
            verts += tendril(pose, buffer, seed + i * 7919L, origin, control, end, width,
                    rootArgb, tipArgb, alpha, envelope, distScale);
        }
        return verts;
    }

    // ---------------------------------------------------------------- flame tongues

    /**
     * A cluster of flame tongues rising from a base disc: three to nine
     * tongues on alternating radial and tangential planes, each a six
     * segment strip whose centerline sways with height (swayPhase is the
     * caller's age based phase, so tongues move), whose width swells and
     * pinches by seeded noise, and whose tip vertices are pulled sideways
     * and inward, tearing the top edge in the mesh itself. Vertex colors
     * run hot at the base to the theme color at the tip. Submit through
     * submitEnergy with ComplexTextures.FLAME_TONGUE.
     */
    public static int flameTongues(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                                   Vec3 base, float radius, float height, int count,
                                   int hotArgb, int coolArgb,
                                   float alpha, float envelope, float swayPhase, float distScale) {
        if (envelope <= 0.01f || alpha <= 0.01f || height <= 0f || count <= 0) return 0;
        Matrix4f mat = pose.pose();
        Rng rng = new Rng(seed);
        int tongues = FxEnvelope.segments(count, distScale, 3);
        int segs = 6;
        int verts = 0;
        for (int i = 0; i < tongues; i++) {
            float ang = (float) (Math.PI * 2.0 * i / tongues) + rng.signed() * 0.25f;
            float radialDist = radius * rng.range(0.15f, 0.80f);
            Vec3 origin = base.add(Math.cos(ang) * radialDist, 0, Math.sin(ang) * radialDist);
            Vec3 radial = new Vec3(Math.cos(ang), 0, Math.sin(ang));
            Vec3 planeDir = (i % 2 == 0) ? radial : new Vec3(-Math.sin(ang), 0, Math.cos(ang));
            Vec3 planePerp = new Vec3(-planeDir.z, 0, planeDir.x);
            float w0 = Math.max(0.12f, radius * (2.2f / tongues) * rng.range(0.8f, 1.2f));
            float hI = height * rng.range(0.7f, 1.25f);
            float phaseI = rng.nextFloat() * (float) Math.PI * 2f;
            float lean = rng.signed() * 0.12f;
            long tongueSeed = seed + i * 104729L;
            Vec3 prevL = null, prevR = null;
            float prevT = 0f;
            for (int s = 0; s <= segs; s++) {
                float tt = s / (float) segs;
                Vec3 center = origin.add(0, hI * tt, 0)
                        .add(planePerp.scale(Math.sin(swayPhase + phaseI + tt * 1.6) * 0.16 * hI * Math.pow(tt, 1.5)))
                        .add(planeDir.scale(lean * tt * tt * hI));
                float swell = (float) Math.pow(Math.sin(Math.PI * (0.08 + 0.84 * tt)), 0.8);
                float halfW = w0 * (0.45f + 0.60f * swell) * (0.8f + 0.4f * noise1(tongueSeed, tt * 5f)) * 0.5f;
                if (tt > 0.8f) halfW *= 1f - (tt - 0.8f) / 0.2f * 0.7f;
                if (s == segs) {
                    center = center.add(planeDir.scale(rng.signed() * 0.35f * w0));
                    halfW *= 0.25f;
                }
                Vec3 l = center.subtract(planeDir.scale(halfW));
                Vec3 r = center.add(planeDir.scale(halfW));
                if (prevL != null) {
                    float[] cPrev = gradient(hotArgb, coolArgb, prevT);
                    float[] cCur = gradient(hotArgb, coolArgb, tt);
                    float aPrev = life(alpha, envelope) * (1f - 0.35f * prevT);
                    float aCur = life(alpha, envelope) * (1f - 0.35f * tt);
                    // The flame canvas has its wide base at the bottom of the
                    // file (v 1) and its torn tip at the top (v 0).
                    verts += quadUv(buffer, mat, pose, prevL, prevR, r, l,
                            cPrev, aPrev, cPrev, aPrev, cCur, aCur, cCur, aCur, planePerp,
                            0f, 1f - prevT, 1f, 1f - prevT, 1f, 1f - tt, 0f, 1f - tt);
                }
                prevL = l;
                prevR = r;
                prevT = tt;
            }
        }
        return verts;
    }

    // ---------------------------------------------------------------- animation easing

    /** Rise ease: fast emergence, soft arrival. Applied to riseFactor inside the ground builders. */
    private static float easeOutCubic(float t) {
        float x = clamp01(t);
        float u = 1f - x;
        return 1f - u * u * u;
    }

    /** Sink ease: slow start, accelerating drop. Applied to sinkFactor inside the ground builders. */
    private static float easeInQuad(float t) {
        float x = clamp01(t);
        return x * x;
    }

}
