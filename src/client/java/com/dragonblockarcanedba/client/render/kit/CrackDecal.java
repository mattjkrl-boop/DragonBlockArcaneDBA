package com.dragonblockarcanedba.client.render.kit;

import com.dragonblockarcanedba.util.SeedHash;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * An emissive crack fan drawn flat on the ground: energy cracks
 * radiating from an impact point, hot at the center and cooling
 * to the theme color at the tips. This is the kit's answer to the
 * oldest foundation gap on the books: the ComplexGeometry
 * crackFan never exposed its branch count, so owners speccing
 * "four short branches" got nine to eleven. Here the crack count
 * is an explicit parameter, each crack walks five segments with
 * seeded angle wander and a width that pinches at seeded points
 * instead of tapering smoothly, and about a third of the cracks
 * throw one fork. Physical fracture (lifted plates, real broken
 * ground) stays ComplexGeometry.fracturePlates on the lit path;
 * this builder is the glowing energy side of the same language.
 *
 * Energy piece: draw through ComplexGeometry.submitEnergy, with
 * the caller's envelope folded into alpha.
 * Budget: 40 vertices per crack (5 segments, double-sided) plus
 * 16 for a fork (2 segments), so at most 56 per crack and 896 at
 * the 16 crack ceiling, inside the old crackFan budget of 900.
 */
public final class CrackDecal {
    private CrackDecal() {}

    private static final int SEGMENTS = 5;
    private static final float FORK_CHANCE = 0.35f;

    public static int draw(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                           double radius, int cracks, double y,
                           int coreArgb, int rimArgb, float alpha) {
        if (cracks <= 0 || radius <= 0) {
            return 0;
        }
        Matrix4f mat = pose.pose();
        float[] core = KitEmit.rgb(coreArgb);
        float[] rim = KitEmit.rgb(rimArgb);
        int verts = 0;

        for (int c = 0; c < cracks; c++) {
            long crackSeed = seed ^ (c * 0x9E3779B9L);
            double angle = SeedHash.hash1(crackSeed, 1) * Math.PI * 2.0;
            verts += walk(mat, pose, buffer, crackSeed, 2, angle, SEGMENTS,
                    radius, y, core, rim, alpha, 1.0f);
            if (SeedHash.hash1(crackSeed, 90) < FORK_CHANCE) {
                double forkAngle = angle + (SeedHash.hash1(crackSeed, 91) < 0.5f ? 0.6 : -0.6);
                // The fork leaves from the main crack's second
                // joint: replay the walk's first two steps to
                // find the point, then branch at reduced length.
                Vec3 joint = jointAt(crackSeed, 2, angle, radius);
                verts += walkFrom(mat, pose, buffer, crackSeed ^ 0xF0B1L, 40,
                        forkAngle, 2, radius * 0.45, joint.x, y, joint.z,
                        core, rim, alpha * 0.8f, 0.6f);
            }
        }
        return verts;
    }

    /** A crack from the origin. stepBase keeps hash indices unique per walk. */
    private static int walk(Matrix4f mat, PoseStack.Pose pose, VertexConsumer buffer,
                            long crackSeed, int stepBase, double startAngle, int segments,
                            double radius, double y,
                            float[] core, float[] rim, float alpha, float widthScale) {
        return walkFrom(mat, pose, buffer, crackSeed, stepBase, startAngle, segments, radius,
                0, y, 0, core, rim, alpha, widthScale);
    }

    private static int walkFrom(Matrix4f mat, PoseStack.Pose pose, VertexConsumer buffer,
                                long crackSeed, int stepBase, double startAngle, int segments,
                                double radius, double startX, double y, double startZ,
                                float[] core, float[] rim, float alpha, float widthScale) {
        double angle = startAngle;
        double x = startX;
        double z = startZ;
        int verts = 0;
        for (int k = 0; k < segments; k++) {
            double t0 = (double) k / segments;
            double t1 = (double) (k + 1) / segments;
            double stepLen = radius / segments * (0.8 + 0.4 * SeedHash.hash1(crackSeed, stepBase + 10 + k));
            angle += (SeedHash.hash1(crackSeed, stepBase + 20 + k) - 0.5) * 0.85;
            double nx = x + Math.cos(angle) * stepLen;
            double nz = z + Math.sin(angle) * stepLen;
            double w0 = widthAt(crackSeed, stepBase, k, radius, widthScale);
            double w1 = widthAt(crackSeed, stepBase, k + 1, radius, widthScale);
            double px = -Math.sin(angle), pz = Math.cos(angle);
            Vec3 p0 = new Vec3(x + px * w0, y, z + pz * w0);
            Vec3 p1 = new Vec3(x - px * w0, y, z - pz * w0);
            Vec3 p2 = new Vec3(nx - px * w1, y, nz - pz * w1);
            Vec3 p3 = new Vec3(nx + px * w1, y, nz + pz * w1);
            float[] c0 = KitEmit.mix(core, rim, (float) t0);
            float[] c1 = KitEmit.mix(core, rim, (float) t1);
            float a0 = alpha * (1f - (float) t0 * 0.35f);
            float a1 = alpha * (1f - (float) t1 * 0.35f);
            verts += KitEmit.quad(buffer, mat, pose,
                    p0, c0, a0, p1, c0, a0, p2, c1, a1, p3, c1, a1, new Vec3(0, 1, 0));
            x = nx;
            z = nz;
        }
        return verts;
    }

    /** Replays a main crack's walk to its joint-th point, for fork placement. */
    private static Vec3 jointAt(long crackSeed, int joint, double startAngle, double radius) {
        double angle = startAngle;
        double x = 0, z = 0;
        for (int k = 0; k < joint; k++) {
            double stepLen = radius / SEGMENTS * (0.8 + 0.4 * SeedHash.hash1(crackSeed, 2 + 10 + k));
            angle += (SeedHash.hash1(crackSeed, 2 + 20 + k) - 0.5) * 0.85;
            x += Math.cos(angle) * stepLen;
            z += Math.sin(angle) * stepLen;
        }
        return new Vec3(x, 0, z);
    }

    private static double widthAt(long crackSeed, int stepBase, int step,
                                  double radius, float widthScale) {
        double base = Math.max(0.045, radius * 0.045) * widthScale;
        double pinch = 0.5 + 0.5 * SeedHash.noise1(crackSeed ^ 0xCA11L, step * 0.8f + stepBase);
        double taper = 1.0 - (step / (double) SEGMENTS) * 0.4;
        return base * (0.45 + 0.55 * pinch) * taper;
    }
}
