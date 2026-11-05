package com.dragonblockarcanedba.client.render.kit;

import com.dragonblockarcanedba.util.SeedHash;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * A flat-plate casing for a projectile body: four flat petals
 * shingled around the flight axis in two length classes, the
 * sabot form the old Blaster casing converged on after its first
 * version read as a flower bud. This builder is the named answer
 * to the attack pass's flat plate builder gap: the shard body is
 * a faceted spindle, and some projectiles (shells, bolts with a
 * physical jacket) want authored flat plates instead. Each plate
 * is two quads (root to mid, mid to tip) bowing outward at the
 * mid and pulling in at the tip, with a seeded roll for the whole
 * set, alternating length classes (full and 0.78), and a shingle
 * stagger on the short class. Colors run root to tip; facet
 * shade steps per plate keep the set from reading as one lathed
 * surface.
 *
 * Material discipline belongs to the caller, exactly as with
 * the shard it alternates with: a physical jacket draws through
 * ComplexGeometry.submitSolidLit, an energy jacket through
 * submitEnergy. The renderer's inset hot core draws separately
 * and is unchanged by the casing form.
 * Budget: 16 vertices per plate (2 quads, double-sided), 4
 * plates, 64 vertices total, against the shard's 288.
 */
public final class PlateCasing {
    private PlateCasing() {}

    private static final int PLATES = 4;

    public static int draw(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                           Vec3 back, Vec3 dir, float length, float width,
                           int rootArgb, int tipArgb, float alpha) {
        if (length <= 0 || width <= 0) {
            return 0;
        }
        Matrix4f mat = pose.pose();
        float[] root = KitEmit.rgb(rootArgb);
        float[] tip = KitEmit.rgb(tipArgb);
        Vec3 ref = Math.abs(dir.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 p1 = dir.cross(ref).normalize();
        Vec3 p2 = dir.cross(p1).normalize();
        double roll0 = SeedHash.hash1(seed, 1) * Math.PI * 0.5;
        int verts = 0;

        for (int k = 0; k < PLATES; k++) {
            double ang = roll0 + Math.PI * 0.25 + k * Math.PI * 0.5;
            Vec3 radial = p1.scale(Math.cos(ang)).add(p2.scale(Math.sin(ang))).normalize();
            Vec3 tangent = p1.scale(-Math.sin(ang)).add(p2.scale(Math.cos(ang))).normalize();
            float plateShade = 0.80f + 0.30f * SeedHash.hash1(seed, k * 3 + 2);
            double lenK = length * (k % 2 == 0 ? 1.0 : 0.78) * (0.94 + 0.10 * SeedHash.hash1(seed, k * 3 + 3));
            Vec3 s0 = back.add(dir.scale(k % 2 == 0 ? 0.0 : length * 0.10));
            Vec3 sMid = s0.add(dir.scale(lenK * 0.55));
            Vec3 sTip = s0.add(dir.scale(lenK));
            double rRoot = width * 0.10;
            double rMid = width * 0.52;
            double rTip = width * 0.30;
            double wRoot = width * 0.30;
            double wMid = width * 0.36;
            double wTip = width * 0.16;
            Vec3 a0 = edge(s0, radial, tangent, rRoot, -wRoot * 0.5);
            Vec3 a1 = edge(s0, radial, tangent, rRoot, wRoot * 0.5);
            Vec3 b0 = edge(sMid, radial, tangent, rMid, -wMid * 0.5);
            Vec3 b1 = edge(sMid, radial, tangent, rMid, wMid * 0.5);
            Vec3 c0 = edge(sTip, radial, tangent, rTip, -wTip * 0.5);
            Vec3 c1 = edge(sTip, radial, tangent, rTip, wTip * 0.5);
            float[] cRoot = KitEmit.shade(root, plateShade);
            float[] cMid = KitEmit.shade(KitEmit.mix(root, tip, 0.55f), plateShade);
            float[] cTip = KitEmit.shade(tip, plateShade);
            verts += KitEmit.quad(buffer, mat, pose,
                    a0, cRoot, alpha, a1, cRoot, alpha, b1, cMid, alpha, b0, cMid, alpha, tangent);
            verts += KitEmit.quad(buffer, mat, pose,
                    b0, cMid, alpha, b1, cMid, alpha, c1, cTip, alpha, c0, cTip, alpha, tangent);
        }
        return verts;
    }

    private static Vec3 edge(Vec3 center, Vec3 radial, Vec3 tangent, double r, double w) {
        return center.add(radial.scale(r)).add(tangent.scale(w));
    }
}
