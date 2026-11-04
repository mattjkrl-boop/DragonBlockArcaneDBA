package com.dragonblockarcanedba.client.render.ki;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Shared layered geometry for spectacle effects (SPECTACLE-STANDARD point 1).
 * Every big effect is drawn in three layers with the standard band alphas:
 * core 0.85, mid 0.45, outer 0.18, all multiplied by the lifecycle envelope
 * and by the distance scale, so all weapons share one visual language and
 * distant effects step down instead of costing full geometry.
 *
 * Bespoke pass: the shapes themselves come from BespokeGeometry (seeded,
 * irregular, broken bands and a jittered glow fan). The signatures here are
 * unchanged from the spectacle pass, so existing callers keep working; only
 * the silhouette changed, from stacked regular rings to torn bespoke bands.
 * Seeds derive from the layer dimensions, so a given effect's silhouette is
 * stable frame to frame and identical on every client.
 *
 * All methods draw in entity local space (origin at the effect center, ground
 * at y = 0) inside an existing submitCustomGeometry call.
 */
public final class LayeredRender {
    private LayeredRender() {}

    public static final float ALPHA_CORE = 0.85f;
    public static final float ALPHA_MID = 0.45f;
    public static final float ALPHA_OUTER = 0.18f;

    /** Distance scale from the camera state, for segment counts and alpha. */
    public static float distScale(CameraRenderState camera, double ex, double ey, double ez) {
        if (camera == null || camera.pos == null) return 1f;
        double dx = camera.pos.x - ex, dy = camera.pos.y - ey, dz = camera.pos.z - ez;
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return com.dragonblockarcanedba.util.FxEnvelope.distScale(d);
    }

    private static long seedFor(float radiusAt, float height, int layer) {
        return (long) (radiusAt * 127.0f) * 2654435761L ^ (long) (height * 511.0f) * 40503L ^ layer * 7919L;
    }

    /**
     * A standing vortex or column in three counter rotating bands:
     * a tight bright core, a theme mid band, and a wide wispy outer band whose
     * alpha dies with height. radiusAt is the mid band radius; the core is
     * 0.38 of it and the outer 1.45 of it.
     */
    public static void drawVortexBands(PoseStack.Pose pose, VertexConsumer buffer,
                                       float radiusAt, float height, float age,
                                       float r, float g, float b, float envelope,
                                       float distScale) {
        if (envelope <= 0.01f) return;
        vortexLayer(pose, buffer, seedFor(radiusAt, height, 0), radiusAt * 0.38f, height,
            (float) Math.toRadians(age * 2.6f),
            Math.min(1f, r + 0.45f), Math.min(1f, g + 0.45f), Math.min(1f, b + 0.45f),
            ALPHA_CORE * envelope, distScale);
        vortexLayer(pose, buffer, seedFor(radiusAt, height, 1), radiusAt, height,
            (float) Math.toRadians(-age * 1.7f),
            r, g, b, ALPHA_MID * envelope, distScale);
        vortexLayer(pose, buffer, seedFor(radiusAt, height, 2), radiusAt * 1.45f, height * 1.1f,
            (float) Math.toRadians(age * 0.9f),
            r, g, b, ALPHA_OUTER * envelope, distScale);
    }

    /** One vortex layer: two stacked broken bands, shrinking and fading with height. */
    private static void vortexLayer(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                                    float radius, float height, float twist,
                                    float r, float g, float b, float alpha, float distScale) {
        BespokeGeometry.brokenVortexBand(pose, buffer, seed, radius, 0f, height * 0.55f,
            twist, r, g, b, alpha, distScale);
        BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x77AA55L, radius * 0.8f,
            height * 0.5f, height, twist + height * 0.55f * 0.4f,
            r, g, b, alpha * 0.55f, distScale);
    }

    /** Ground anchor glow: a soft splash of light that grounds an effect to the world. */
    public static void drawAnchorGlow(PoseStack.Pose pose, VertexConsumer buffer,
                                      float radius, float y, float r, float g, float b,
                                      float alpha, float distScale) {
        if (alpha <= 0.01f) return;
        BespokeGeometry.glowFan(pose, buffer, (long) (radius * 997.0f) * 31L + 7L,
            radius, y, r, g, b, alpha, distScale);
    }
}
