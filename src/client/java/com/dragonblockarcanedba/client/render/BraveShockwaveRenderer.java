package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.BraveShockwaveEntity;
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
 * Entity Renderer for Brave Sword Heroic Impact Shockwave in Minecraft 26.2.
 * Bespoke pass: the beveled crest ring, flat rings, valor spike triangles,
 * and starburst core are rebuilt on BespokeGeometry. Broken vortex bands
 * carry the raised crest (gold) and its cyan harmonic, a wider faint band
 * runs ahead of the front as the old wisp ring did, crack stars tear the
 * ground under the front, shard fans ride the ring as the valor spikes,
 * and a spinning two tone shard fan is the epicenter burst. The seed
 * comes from the entity UUID, which is synced state, so every client
 * draws the same shockwave.
 */
public class BraveShockwaveRenderer extends EntityRenderer<BraveShockwaveEntity, BraveShockwaveRenderer.ShockwaveRenderState> {

    public BraveShockwaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ShockwaveRenderState extends EntityRenderState {
        public float maxRadius = 7.5f;
        public float age = 0;
        public int maxLifetime = 44;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(BraveShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(BraveShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ShockwaveRenderState createRenderState() {
        return new ShockwaveRenderState();
    }

    @Override
    public void extractRenderState(BraveShockwaveEntity entity, ShockwaveRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.maxRadius = entity.getMaxRadius();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();
    }

    @Override
    public void submit(ShockwaveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        // The lifetime comes from the entity (44 ticks), not a hardcoded copy:
        // the ring now has time to cross its full radius and dissipate.
        float maxLife = (float) state.maxLifetime;
        float progress = Math.min(1.0f, state.age / maxLife);
        if (progress >= 1.0f) return;

        float expansionEase = (float) Math.sin(progress * (Math.PI / 2.0));
        float currentRadius = Math.max(0.05f, expansionEase * state.maxRadius);
        float env = FxEnvelope.envelope(state.age, maxLife, 0.15f, 0.35f);
        float alpha = env * 0.92f;
        float crest = 0.85f * env;
        float distScale = LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        float age = state.age;
        long seed = state.seed;

        RenderType renderType = KiRenderHelper.kiRenderType();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // Ground anchor glow under the whole effect (standard point 1).
            LayeredRender.drawAnchorGlow(pose, buffer, currentRadius * 0.95f, 0.03f,
                1.0f, 0.84f, 0.0f, alpha * 0.45f, distScale);

            // Outer layer: a wide, faint gold wisp band ahead of the front,
            // where the old flat wisp ring ran.
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x3C6EF362L, currentRadius * 1.10f,
                0.02f, 0.10f, age * 0.045f, 1.0f, 0.84f, 0.0f, alpha * LayeredRender.ALPHA_OUTER, distScale);

            // Mid layer: the crest front as a broken vortex band, gold at
            // full crest height with a lower cyan harmonic just inside it,
            // replacing the beveled ring's gold base and cyan top.
            BespokeGeometry.brokenVortexBand(pose, buffer, seed, currentRadius,
                0.05f, 0.05f + crest, age * 0.045f, 1.0f, 0.84f, 0.0f, alpha, distScale);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x2B992DDFL, currentRadius * 0.96f,
                0.05f, 0.05f + crest * 0.55f, -age * 0.03f, 0.0f, 0.95f, 1.0f, alpha * 0.8f, distScale);

            // Ground work: crack stars tearing outward under the front,
            // gold at the front radius and a cyan set behind it.
            BespokeGeometry.crackStar(pose, buffer, seed ^ 0x51ED2701L, currentRadius,
                1.0f, 0.84f, 0.0f, 0.80f, env, distScale);
            BespokeGeometry.crackStar(pose, buffer, seed ^ 0x85EBCA6BL, currentRadius * 0.7f,
                0.0f, 0.95f, 1.0f, 0.45f, env, distScale);

            // Core layer: the bright white epicenter as a low torn band
            // where the old white flat ring sat.
            float ringWidth = Math.min(0.85f, currentRadius * 0.16f);
            float rInner = Math.max(0.1f, currentRadius - ringWidth);
            float coreOuter = Math.max(0.12f, rInner * 0.72f);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x27D4EB2FL, coreOuter,
                0.09f, 0.30f, age * 0.06f, 1.0f, 1.0f, 1.0f, alpha * 0.9f, distScale);
        });

        // Epicenter burst: a two tone shard fan spinning at the old
        // starburst rate, gold over cyan.
        float burstRadius = state.maxRadius * 0.20f * (0.75f + 0.25f * (float) Math.sin(age * 0.35f));
        if (burstRadius >= 0.05f && alpha > 0.01f) {
            poseStack.pushPose();
            poseStack.translate(0.0f, 0.08f, 0.0f);
            poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(age * 14.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.shardFan(pose, buffer, seed ^ 0x165667B1L, burstRadius,
                    1.0f, 0.88f, 0.20f, 0.85f, env, distScale);
                BespokeGeometry.shardFan(pose, buffer, seed ^ 0x1B56C4E9L, burstRadius * 0.62f,
                    0.0f, 0.95f, 1.0f, 0.85f, env, distScale);
            });
            poseStack.popPose();
        }

        // Valor spikes riding the ring front: the old twelve crystalline
        // blades at the same rotation rate, height formula, and color,
        // each now a shard fan stood upright in the radial plane with its
        // center lifted so the fan crests where the old tips did.
        if (currentRadius >= 0.4f && alpha > 0.01f) {
            int spikeCount = FxEnvelope.segments(12, distScale, 6);
            float rot = age * 0.045f;
            for (int k = 0; k < spikeCount; k++) {
                double a = rot + (k / (double) spikeCount) * Math.PI * 2.0;
                float vary = 0.75f + 0.45f * (float) Math.sin(k * 2.7f + age * 0.22f);
                float height = (0.7f + crest * 1.15f) * vary;
                double ax = Math.cos(a), az = Math.sin(a);
                float bx = (float) (ax * currentRadius), bz = (float) (az * currentRadius);
                float yawDeg = (float) Math.toDegrees(Math.atan2(-az, ax));
                long spikeSeed = seed ^ (0x9E3779B9L + k * 131L);

                poseStack.pushPose();
                poseStack.translate(bx, 0.05f + height * 0.45f, bz);
                poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(yawDeg)));
                poseStack.mulPose(new Matrix4f().rotation(Axis.XP.rotationDegrees(-90.0f)));
                collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                    BespokeGeometry.shardFan(pose, buffer, spikeSeed, height * 0.62f,
                        1.0f, 0.95f, 0.72f, 0.80f, env, distScale);
                });
                poseStack.popPose();
            }
        }

        super.submit(state, poseStack, collector, cameraState);
    }
}
