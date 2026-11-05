package com.dragonblockarcanedba.client.render.kit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * A hemisphere shell for domains: the swirling storm dome from
 * the Cursed Tempest picture, replacing the Phase 1 pair of
 * smooth full spheres in CloudCarrierRenderer. Only the upper
 * hemisphere is emitted (the lower half of the old spheres was
 * buried under the ground plane and never read), at the same
 * radii and alphas, so with the default options (2 layers, swirl
 * 0, rim glow 0, alpha scale 1) the above-ground read is the
 * Phase 1 to 3 shell exactly.
 *
 * Two authored motions live in the vertex data, not in textures:
 * - Scrolling alpha: when swirlSpeed is nonzero, each vertex's
 *   alpha is modulated by bands that rotate with age (layer 2
 *   counter-rotates against layer 1), so the shell reads as
 *   swirling cloud mass instead of a glass ball. At swirlSpeed 0
 *   the modulation factor is exactly 1.
 * - Rim brightening: rimGlow mixes the vertex color toward the
 *   core color and lifts its brightness toward the equator band,
 *   the fresnel-style silhouette edge a dome is actually judged
 *   by. At rimGlow 0 the color is the flat layer color, as
 *   before.
 *
 * Layers nest at fixed scales with fixed base alphas: layer 1 at
 * 1.00 in mid (0.10), layer 2 at 0.80 in rim (0.07), layer 3 at
 * 0.62 in core (0.05), all scaled by the caller's envelope and
 * the alphaScale option. Soft piece: draw through
 * ComplexGeometry.submitVeil.
 * Budget: 4 vertices per grid cell (single-sided, the outward
 * read, same as the spheres it replaces). At the cloud's full
 * detail of 28 slices and 6 rings that is 672 per layer, 1344
 * for the shipped two layers, 2016 at the 3 layer ceiling.
 */
public final class DomeShell {
    private DomeShell() {}

    private static final double[] LAYER_SCALE = {1.00, 0.80, 0.62};
    private static final float[] LAYER_ALPHA = {0.10f, 0.07f, 0.05f};

    public static int draw(PoseStack.Pose pose, VertexConsumer buffer,
                           double radius, int slices, int rings, int layers,
                           float swirlSpeed, float age,
                           int midArgb, int rimArgb, int coreArgb,
                           float alphaScale, float rimGlow, float env) {
        int layerCount = Math.max(1, Math.min(3, layers));
        if (radius <= 0 || slices < 3 || rings < 1 || env <= 0.001f) {
            return 0;
        }
        Matrix4f mat = pose.pose();
        float[][] layerRgb = {KitEmit.rgb(midArgb), KitEmit.rgb(rimArgb), KitEmit.rgb(coreArgb)};
        float[] core = layerRgb[2];
        int verts = 0;

        for (int layer = 0; layer < layerCount; layer++) {
            double r = radius * LAYER_SCALE[layer];
            float[] base = layerRgb[layer];
            float baseAlpha = LAYER_ALPHA[layer] * alphaScale * env;
            double dir = layer % 2 == 0 ? 1.0 : -1.0;
            double phase = age * swirlSpeed * 0.22 * dir;
            for (int j = 0; j < rings; j++) {
                double phi0 = (Math.PI * 0.5) * j / rings;
                double phi1 = (Math.PI * 0.5) * (j + 1) / rings;
                for (int i = 0; i < slices; i++) {
                    double th0 = Math.PI * 2.0 * i / slices;
                    double th1 = Math.PI * 2.0 * (i + 1) / slices;
                    Vec3 p00 = point(r, phi0, th0), p10 = point(r, phi0, th1);
                    Vec3 p11 = point(r, phi1, th1), p01 = point(r, phi1, th0);
                    float[] c00 = cellColor(base, core, phi0, rimGlow);
                    float[] c10 = cellColor(base, core, phi0, rimGlow);
                    float[] c11 = cellColor(base, core, phi1, rimGlow);
                    float[] c01 = cellColor(base, core, phi1, rimGlow);
                    float a00 = baseAlpha * scrollFactor(th0, phi0, phase, swirlSpeed);
                    float a10 = baseAlpha * scrollFactor(th1, phi0, phase, swirlSpeed);
                    float a11 = baseAlpha * scrollFactor(th1, phi1, phase, swirlSpeed);
                    float a01 = baseAlpha * scrollFactor(th0, phi1, phase, swirlSpeed);
                    Vec3 n = p00.add(p10).add(p11).add(p01).normalize();
                    // Winding matches KiRenderHelper.drawSphere:
                    // outward-facing seen from outside the dome.
                    verts += KitEmit.quadSingle(buffer, mat, pose,
                            p00, c00, a00, p01, c01, a01, p11, c11, a11, p10, c10, a10, n);
                }
            }
        }
        return verts;
    }

    /** phi is measured from the equator (0) to the pole (pi/2). */
    private static Vec3 point(double radius, double phi, double theta) {
        double rad = Math.cos(phi) * radius;
        return new Vec3(Math.cos(theta) * rad, Math.sin(phi) * radius, Math.sin(theta) * rad);
    }

    private static float scrollFactor(double theta, double phi, double phase, float swirlSpeed) {
        if (swirlSpeed == 0f) {
            return 1f;
        }
        return 0.55f + 0.45f * (float) Math.sin(theta * 3.0 + phase + phi * 2.0);
    }

    private static float[] cellColor(float[] base, float[] core, double phi, float rimGlow) {
        if (rimGlow <= 0f) {
            return base;
        }
        // Fresnel-style rim: strongest at the equator band, gone
        // at the pole.
        double rimFactor = Math.pow(1.0 - Math.sin(phi), 2.0);
        float mixAmt = (float) Math.min(1.0, rimGlow * 0.55 * rimFactor);
        float bright = 1f + rimGlow * 0.45f * (float) rimFactor;
        return KitEmit.shade(KitEmit.mix(base, core, mixAmt), bright);
    }
}
