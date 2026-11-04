package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.BraveChargeEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;

/**
 * Entity Renderer for Brave Sword Heroic Focus Charge-up in Minecraft 26.2.
 * Bespoke pass: the hand-built ground rings, 3D shards, octahedra, and
 * ribbon quads are rebuilt on BespokeGeometry. The ground runes are three
 * layered glow fans with torn rims, the converging valor shards are shard
 * fans aimed at the core, the octahedron core matrix is a pair of
 * displaced domes per layer (one flipped for the underside), and the twin
 * helical ribbons are torn ribbons streaming up off the ground ring. The
 * seed comes from the entity UUID, which is synced state, so every client
 * draws the same charge construct.
 */
public class BraveChargeRenderer extends EntityRenderer<BraveChargeEntity, BraveChargeRenderer.ChargeRenderState> {

    public BraveChargeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ChargeRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public float age = 0;
        public float fadeFactor = 1.0f;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(BraveChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(BraveChargeEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ChargeRenderState createRenderState() {
        return new ChargeRenderState();
    }

    @Override
    public void extractRenderState(BraveChargeEntity entity, ChargeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.age = entity.tickCount + partialTicks;
        state.fadeFactor = entity.getFadeFactor();
        state.seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();
    }

    @Override
    public void submit(ChargeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        float charge = state.chargeRatio;
        float age = state.age;
        long seed = state.seed;

        float baseRadius = 1.3f + (charge * 1.5f);
        // Spawn fade-in over the first 4 ticks so the charge construct does not
        // pop in at full brightness, and the entity's synced fade factor takes
        // it back out over 10 ticks when charging ends, so it never pops out
        // at full alpha either (standard point 5).
        float baseAlpha = (0.65f + (charge * 0.35f)) * Math.min(1.0f, age / 4.0f) * state.fadeFactor;
        float pulse = FxEnvelope.pulse(age, 0.5f, 0.90f, 0.10f);
        float distScale = LayeredRender.distScale(cameraState, state.x, state.y, state.z);

        // 1. Ground heroic runes: three layered glow fans (radiant gold,
        // heroic cyan, white-gold core) at the old ring heights, their rims
        // torn and jittered instead of perfect circles.
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.glowFan(pose, buffer, seed, baseRadius * 1.15f, 0.03f,
                1.0f, 0.84f, 0.0f, baseAlpha * 0.80f, distScale);
            BespokeGeometry.glowFan(pose, buffer, seed ^ 0x2B992DDFL, baseRadius * 0.82f, 0.06f,
                0.0f, 0.95f, 1.0f, baseAlpha * 0.88f, distScale);
            BespokeGeometry.glowFan(pose, buffer, seed ^ 0x51ED2701L, baseRadius * 0.50f, 0.09f,
                1.0f, 0.98f, 0.80f, baseAlpha * 0.95f, distScale);
        });

        // 2. Inward-converging valor shards: same orbit math as the old
        // prisms (radius closes and speed rises with charge), each shard a
        // shard fan aimed at the core point, gold body with a cyan heart.
        int shardCount = 8 + (int) (charge * 6); // 8 to 14 shards
        float orbitRadius = (2.2f - charge * 0.65f) * pulse;
        float orbitSpeed = 20.0f + (charge * 55.0f);
        float shardLen = 0.55f + (charge * 0.25f);

        for (int i = 0; i < shardCount; i++) {
            double shardAngle = (i / (double) shardCount) * Math.PI * 2.0 + Math.toRadians(age * orbitSpeed);
            float sy = 0.4f + (float) Math.sin(age * 0.35f + i * 1.3f) * 0.5f + (i % 2 == 0 ? 0.3f : 0.0f);
            float sx = (float) Math.cos(shardAngle) * orbitRadius;
            float sz = (float) Math.sin(shardAngle) * orbitRadius;

            // Aim direction toward the sword core (y = 0.9), as the old
            // shard tips pointed.
            float dx = -sx;
            float dz = -sz;
            float dy = 0.9f - sy;
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 0.001f) continue;
            dx /= len;
            dy /= len;
            dz /= len;
            float yawDeg = (float) Math.toDegrees(Math.atan2(-dz, dx));
            float pitchDeg = (float) Math.toDegrees(Math.asin(Math.max(-1.0f, Math.min(1.0f, dy))));
            long shardSeed = seed ^ (i * 0x9E3779B9L);

            poseStack.pushPose();
            poseStack.translate(sx, sy, sz);
            poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(yawDeg)));
            poseStack.mulPose(new Matrix4f().rotation(Axis.ZP.rotationDegrees(pitchDeg)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.shardFan(pose, buffer, shardSeed, shardLen * 0.85f,
                    1.0f, 0.80f, 0.0f, 0.85f, baseAlpha, distScale);
                BespokeGeometry.shardFan(pose, buffer, shardSeed ^ 0x85EBCA6BL, shardLen * 0.5f,
                    0.0f, 0.95f, 1.0f, 0.98f, baseAlpha, distScale);
            });
            poseStack.popPose();
        }

        // 3. Core matrix: three nested layers at the old octahedron sizes,
        // speeds, and colors, each a displaced dome over a flipped twin so
        // the core reads as a closed, noise-displaced shell around the
        // caster at y 0.85.
        float coreSize = (0.70f - charge * 0.20f) * pulse;
        float[] coreFactors = {1.0f, 0.58f, 0.22f};
        float[] coreSpeeds = {22.0f, -32.0f, 55.0f};
        float[][] coreColors = {{1.0f, 0.84f, 0.0f}, {0.0f, 0.95f, 1.0f}, {1.0f, 1.0f, 1.0f}};
        float[] coreAlphas = {0.80f, 0.95f, 1.0f};
        for (int layer = 0; layer < 3; layer++) {
            long layerSeed = seed ^ (0x27D4EB2FL + layer * 131L);
            float layerSize = coreSize * coreFactors[layer];
            float layerRot = age * coreSpeeds[layer];
            float cr = coreColors[layer][0];
            float cg = coreColors[layer][1];
            float cb = coreColors[layer][2];
            float layerAlpha = coreAlphas[layer];

            poseStack.pushPose();
            poseStack.translate(0.0f, 0.85f, 0.0f);
            poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(layerRot)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.displacedDome(pose, buffer, layerSeed, layerSize,
                    cr, cg, cb, layerAlpha, baseAlpha, distScale);
            });
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.translate(0.0f, 0.85f, 0.0f);
            poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(layerRot)));
            poseStack.mulPose(new Matrix4f().rotation(Axis.XP.rotationDegrees(180.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.displacedDome(pose, buffer, layerSeed, layerSize,
                    cr, cg, cb, layerAlpha, baseAlpha, distScale);
            });
            poseStack.popPose();
        }

        // 4. Twin valor streamers: torn ribbons peeling up off the ground
        // ring at the old helix base points, spinning at the old helix
        // rate, gold and cyan with white cores.
        for (int strand = 0; strand < 2; strand++) {
            boolean isGoldStrand = (strand == 0);
            float sr = isGoldStrand ? 1.0f : 0.0f;
            float sg = isGoldStrand ? 0.85f : 0.95f;
            float sb = isGoldStrand ? 0.1f : 1.0f;
            double a0 = strand * Math.PI + age * 0.20;
            float r0 = baseRadius * 0.65f * 0.8f;
            float bx = (float) Math.cos(a0) * r0;
            float bz = (float) Math.sin(a0) * r0;
            // Yaw so the ribbon's run direction follows the ring tangent.
            float yawDeg = (float) Math.toDegrees(Math.atan2(-Math.cos(a0), -Math.sin(a0)));
            long strandSeed = seed ^ (0x165667B1L + strand * 131L);

            poseStack.pushPose();
            poseStack.translate(bx, 0.05f, bz);
            poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(yawDeg)));
            poseStack.mulPose(new Matrix4f().rotation(Axis.ZP.rotationDegrees(55.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.tornRibbon(pose, buffer, strandSeed, 2.6f, 0.28f, 0.35f,
                    sr, sg, sb, 1.0f, baseAlpha, distScale);
                BespokeGeometry.tornRibbon(pose, buffer, strandSeed ^ 0x9E3779B9L, 2.6f, 0.08f, 0.35f,
                    1.0f, 1.0f, 1.0f, 1.0f, baseAlpha, distScale);
            });
            poseStack.popPose();
        }

        // Additive halo pass: a wider faint glow fan and a 1.25x dome shell
        // so the charge blooms, plus the shared ground anchor glow (standard
        // point 1), matching the old additive ring, octahedron, and anchor.
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            BespokeGeometry.glowFan(pose, buffer, seed ^ 0x3C6EF362L, baseRadius * 1.30f, 0.03f,
                1.0f, 0.84f, 0.0f, baseAlpha * 0.15f, distScale);
            LayeredRender.drawAnchorGlow(pose, buffer, baseRadius * 1.55f, 0.02f,
                1.0f, 0.84f, 0.0f, baseAlpha * 0.40f, distScale);
        });
        float haloSize = coreSize * 1.25f;
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.85f, 0.0f);
        poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(age * 22.0f)));
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0x3C6EF362L, haloSize,
                1.0f, 0.84f, 0.0f, 0.18f, baseAlpha, distScale);
        });
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.85f, 0.0f);
        poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(age * 22.0f)));
        poseStack.mulPose(new Matrix4f().rotation(Axis.XP.rotationDegrees(180.0f)));
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0x3C6EF362L, haloSize,
                1.0f, 0.84f, 0.0f, 0.18f, baseAlpha, distScale);
        });
        poseStack.popPose();

        super.submit(state, poseStack, collector, cameraState);
    }
}
