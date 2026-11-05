package com.dragonblockarcanedba.client.render.kit;

import com.dragonblockarcanedba.util.SeedHash;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * A wall of clustered rock slabs torn out of the ground: the
 * rockWall idea from the attack upgrade pass, rebuilt as a kit
 * primitive with the two things that pass's merge asked for and
 * never got. First, the ridge profile is grouped: slab heights
 * ride a low-frequency seeded noise, so peaks come in clusters
 * of two to four slabs with saddles between them, never a
 * picket fence of equal teeth and never per-slab coin flips.
 * Second, every slab follows the ground: the caller supplies a
 * sampler (the client heightmap, in the renderer's local frame),
 * each slab samples it at both ends, and the slab's base and
 * crest tilt with the terrain instead of floating or burying at
 * one corner.
 *
 * Slabs are chunky boxes with jittered widths, depths, and crest
 * corners, shaded in seeded facet steps over a root-to-tip color
 * gradient. This is physical matter: draw through
 * ComplexGeometry.submitSolidLit with the host's light sample,
 * never the emissive path. The wall runs along the perpendicular
 * of the forward direction, bowed toward it by arcRad (positive
 * arc swings the ends toward facing, center recessed, the
 * shipped convention from the attack pass addendum).
 *
 * Budget: 32 vertices per slab (front, top, and two side faces,
 * double-sided; the back face is skipped). Callers cap slabs at
 * 16, so the ceiling is 512 vertices. There is no consumer yet:
 * Phase 6 ground chains and wall attacks build on this, and the
 * Phase 4 harness proves its counts and determinism.
 */
public final class TornWall {
    private TornWall() {}

    /** Ground height in the builder's local frame, sampled per slab end. */
    @FunctionalInterface
    public interface GroundSampler {
        double groundY(double x, double z);
    }

    public static int draw(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                           double fwdX, double fwdZ, double length, double arcRad, double height,
                           int slabs, GroundSampler ground,
                           int rootArgb, int tipArgb, float alpha) {
        if (slabs <= 0 || length <= 0 || height <= 0) {
            return 0;
        }
        Matrix4f mat = pose.pose();
        float[] root = KitEmit.rgb(rootArgb);
        float[] tip = KitEmit.rgb(tipArgb);
        double perpX = -fwdZ;
        double perpZ = fwdX;
        int verts = 0;

        for (int i = 0; i < slabs; i++) {
            double u = (i + 0.5) / slabs - 0.5;
            double along = u * length;
            double bow = arcRad * (0.25 * length - along * along / length);
            double cx = perpX * along + fwdX * bow;
            double cz = perpZ * along + fwdZ * bow;
            // Tangent along the bowed wall line.
            double slope = -2.0 * along * arcRad / length;
            double tx = perpX + fwdX * slope;
            double tz = perpZ + fwdZ * slope;
            double tLen = Math.sqrt(tx * tx + tz * tz);
            tx /= tLen;
            tz /= tLen;
            double nx = -tz;
            double nz = tx;

            double width = (length / slabs) * (0.92 + 0.35 * SeedHash.hash1(seed, i * 5 + 1));
            double depth = width * (0.50 + 0.22 * SeedHash.hash1(seed, i * 5 + 2));
            // Grouped ridge: low-frequency noise, so neighboring
            // slabs share their height class and peaks cluster.
            double ridge = 0.60 + 0.58 * SeedHash.noise1(seed ^ 0x3D91B7L, i * 0.42f);
            double slabH = height * ridge;
            float slabShade = 0.85f + 0.30f * SeedHash.hash1(seed, i * 5 + 3);

            double hw = width * 0.5;
            double hd = depth * 0.5;
            double ax = cx - tx * hw, az = cz - tz * hw;
            double bx = cx + tx * hw, bz = cz + tz * hw;
            double gA = ground.groundY(ax, az) - 0.30;
            double gB = ground.groundY(bx, bz) - 0.30;
            // Crest jitter per corner, so the top edge is torn.
            double jA = 0.90 + 0.20 * SeedHash.hash1(seed, i * 5 + 4);
            double jB = 0.90 + 0.20 * SeedHash.hash1(seed, i * 5 + 5);

            Vec3 aBotF = new Vec3(ax + nx * hd, gA, az + nz * hd);
            Vec3 bBotF = new Vec3(bx + nx * hd, gB, bz + nz * hd);
            Vec3 aTopF = new Vec3(ax + nx * hd * 0.82, gA + slabH * jA, az + nz * hd * 0.82);
            Vec3 bTopF = new Vec3(bx + nx * hd * 0.82, gB + slabH * jB, bz + nz * hd * 0.82);
            Vec3 aBotB = new Vec3(ax - nx * hd, gA, az - nz * hd);
            Vec3 bBotB = new Vec3(bx - nx * hd, gB, bz - nz * hd);
            Vec3 aTopB = new Vec3(ax - nx * hd * 0.82, gA + slabH * jA, az - nz * hd * 0.82);
            Vec3 bTopB = new Vec3(bx - nx * hd * 0.82, gB + slabH * jB, bz - nz * hd * 0.82);

            float[] rootF = KitEmit.shade(root, slabShade);
            float[] tipF = KitEmit.shade(tip, slabShade * 1.06f);
            float[] rootS = KitEmit.shade(root, slabShade * 0.78f);
            float[] tipS = KitEmit.shade(tip, slabShade * 0.84f);
            float[] rootT = KitEmit.shade(root, slabShade * 1.10f);
            float[] tipT = KitEmit.shade(tip, slabShade * 1.16f);
            Vec3 nFront = new Vec3(nx, 0, nz);
            Vec3 nSide = new Vec3(tx, 0, tz);
            Vec3 nUp = new Vec3(0, 1, 0);

            // Front face (root at the ground, tip at the crest).
            verts += KitEmit.quad(buffer, mat, pose,
                    aBotF, rootF, alpha, bBotF, rootF, alpha, bTopF, tipF, alpha, aTopF, tipF, alpha, nFront);
            // Crest face.
            verts += KitEmit.quad(buffer, mat, pose,
                    aTopF, tipT, alpha, bTopF, tipT, alpha, bTopB, tipT, alpha, aTopB, tipT, alpha, nUp);
            // The two end faces.
            verts += KitEmit.quad(buffer, mat, pose,
                    aBotB, rootS, alpha, aBotF, rootS, alpha, aTopF, tipS, alpha, aTopB, tipS, alpha,
                    nSide.scale(-1.0));
            verts += KitEmit.quad(buffer, mat, pose,
                    bBotF, rootS, alpha, bBotB, rootS, alpha, bTopB, tipS, alpha, bTopF, tipS, alpha,
                    nSide);
        }
        return verts;
    }
}
