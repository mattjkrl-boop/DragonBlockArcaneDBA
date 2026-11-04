package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.SpiritImpaleEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Spirit Impale in Minecraft 26.2.
 * Bespoke pass: every shape comes from BespokeGeometry seeded by the
 * entity's UUID (a hash of synced state). The judgement array is crack
 * stars and glow fans counter rotating at the spectacle speeds, the pillar
 * is broken vortex bands wrapped in torn ribbon strands, the six plunging
 * swords are crossed torn ribbon blades with shard fan guards and pommels,
 * and the crystal perimeter is two seeded shard fans. Counts, placement
 * math, envelope, and colors are unchanged from the spectacle pass.
 */
public class SpiritImpaleRenderer extends EntityRenderer<SpiritImpaleEntity, SpiritImpaleRenderer.SpiritImpaleRenderState> {

    public SpiritImpaleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SpiritImpaleRenderState extends EntityRenderState {
        public float scale = 1.0f;
        public float age = 0;
        public int maxLifetime = 40;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(SpiritImpaleEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(SpiritImpaleEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public SpiritImpaleRenderState createRenderState() {
        return new SpiritImpaleRenderState();
    }

    @Override
    public void extractRenderState(SpiritImpaleEntity entity, SpiritImpaleRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getImpaleScale();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(SpiritImpaleRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float age = state.age;
        float scale = state.scale;
        // Shared envelope over the full 40 tick life (standard point 5):
        // builds with the slam, holds, then dissipates across the last
        // 28 percent, replacing the old hold-then-linear fade keyed to 28.
        float fade = FxEnvelope.envelope(age, state.maxLifetime, 0.12f, 0.28f);
        float slam = Math.min(1.0f, age / 4.0f);
        float pulse = 0.85f + 0.15f * (float) Math.sin(age * 0.4f);
        float distScale = LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        long seed = state.seed;

        RenderType renderType = KiRenderHelper.kiRenderType();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.Rng rng = new BespokeGeometry.Rng(seed);

            // 1. Divine judgement ground array: a crack star script ring, a
            // gold glow fan, and a white flash heart, counter rotating at
            // the old ring speeds (22, -30, and 15 degrees per tick across
            // the three layers).
            float baseRadius = 2.4f * scale * slam;
            if (fade > 0.02f) {
                PoseStack.Pose outer = pose.copy();
                outer.rotate(Axis.YP.rotationDegrees(age * 22.0f));
                BespokeGeometry.crackStar(outer, buffer, seed + 11L, baseRadius * 1.02f,
                    0.0f, 0.90f, 1.0f, 0.85f, fade, distScale);
                BespokeGeometry.glowFan(pose, buffer, seed + 12L, baseRadius * 0.62f, 0.04f,
                    1.0f, 0.82f, 0.15f, fade * 0.75f, distScale);
                PoseStack.Pose inner = pose.copy();
                inner.rotate(Axis.YP.rotationDegrees(-age * 30.0f));
                BespokeGeometry.crackStar(inner, buffer, seed + 13L, baseRadius * 0.38f * pulse,
                    1.0f, 1.0f, 1.0f, 0.80f, fade, distScale);
                BespokeGeometry.glowFan(pose, buffer, seed + 14L, baseRadius * 0.30f * pulse, 0.05f,
                    1.0f, 1.0f, 1.0f, fade * 0.55f, distScale);
            }

            // 2. Ascending divine pillar: broken vortex bands (white core,
            // two cyan shells) wrapped in four torn ribbon strands that
            // orbit at the old helix phase speed and alternate cyan and
            // gold like the two helices did.
            float pillarHeight = 6.0f * slam;
            float pillarRadius = 0.65f * scale * pulse;
            BespokeGeometry.brokenVortexBand(pose, buffer, seed + 21L, pillarRadius * 0.42f,
                0.0f, pillarHeight * 1.05f, age * 0.25f, 1.0f, 1.0f, 1.0f, fade * 0.90f, distScale);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed + 22L, pillarRadius,
                0.0f, pillarHeight * 0.55f, age * 0.25f, 0.0f, 0.92f, 1.0f, fade * 0.60f, distScale);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed + 23L, pillarRadius * 0.8f,
                pillarHeight * 0.5f, pillarHeight, age * 0.25f + 0.5f, 0.0f, 0.92f, 1.0f, fade * 0.33f, distScale);
            for (int k = 0; k < 4; k++) {
                boolean goldStrand = (k % 2 == 1);
                PoseStack.Pose strand = pose.copy();
                strand.rotate(Axis.YP.rotationDegrees(age * 14.3f + k * 90.0f));
                strand.translate(pillarRadius * 1.25f, 0.0f, 0.0f);
                strand.rotate(Axis.ZP.rotationDegrees(90.0f));
                BespokeGeometry.tornRibbon(strand, buffer, seed + 30L + k * 7L, pillarHeight, 0.12f, -0.3f,
                    goldStrand ? 1.0f : 0.0f, goldStrand ? 0.85f : 0.95f, goldStrand ? 0.30f : 1.0f,
                    0.70f, fade, distScale);
            }

            // 3. Six ethereal spirit swords plunging into the target. The
            // placement math (ring radius, drop, center, tips, vibration)
            // is the spectacle math verbatim; each sword is built from
            // bespoke pieces in a pose aligned from hilt to tip.
            int swordCount = 6;
            float swordRingRadius = 1.65f * scale * (1.3f - slam * 0.3f);
            float startDropY = 3.5f * (1.0f - slam);
            float targetCenterY = 0.75f * scale + (float) Math.sin(age * 0.3f) * 0.04f;

            for (int i = 0; i < swordCount; i++) {
                double baseAngle = (i / (double) swordCount) * Math.PI * 2.0 + (age * 0.02f);
                float hx = (float) Math.cos(baseAngle) * swordRingRadius;
                float hz = (float) Math.sin(baseAngle) * swordRingRadius;
                float hy = targetCenterY + 1.2f + startDropY;

                float tipX = (float) Math.cos(baseAngle) * (0.25f * scale);
                float tipZ = (float) Math.sin(baseAngle) * (0.25f * scale);
                float tipY = targetCenterY - 0.2f * scale;

                float vibX = (rng.nextFloat() - 0.5f) * 0.04f * (1.0f - slam * 0.5f);
                float vibZ = (rng.nextFloat() - 0.5f) * 0.04f * (1.0f - slam * 0.5f);

                float dx = tipX - hx, dy = tipY - hy, dz = tipZ - hz;
                float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (len < 0.01f) continue;
                float hLen = (float) Math.sqrt(dx * dx + dz * dz);
                float yawDeg = (float) Math.toDegrees(Math.atan2(-dz, dx));
                float pitchDeg = (float) Math.toDegrees(Math.atan2(dy, hLen));
                long sSeed = seed + 100L + i * 131L;

                // Local frame: origin at the hilt, +X running hilt to tip.
                PoseStack.Pose sword = pose.copy();
                sword.translate(hx + vibX, hy, hz + vibZ);
                sword.rotate(Axis.YP.rotationDegrees(yawDeg));
                sword.rotate(Axis.ZP.rotationDegrees(pitchDeg));

                // Grip from hilt to guard.
                BespokeGeometry.tornRibbon(sword, buffer, sSeed, len * 0.18f, 0.11f * scale, 0.0f,
                    0.05f, 0.65f, 0.90f, 0.90f, fade, distScale);
                // Blade: a bright torn ribbon from guard to tip, with a
                // crossed cyan web for thickness.
                PoseStack.Pose blade = sword.copy();
                blade.translate(len * 0.18f, 0.0f, 0.0f);
                BespokeGeometry.tornRibbon(blade, buffer, sSeed + 7L, len * 0.82f, 0.20f * scale, 0.0f,
                    0.55f, 0.97f, 1.0f, 0.95f, fade, distScale);
                PoseStack.Pose bladeCross = blade.copy();
                bladeCross.rotate(Axis.XP.rotationDegrees(90.0f));
                BespokeGeometry.tornRibbon(bladeCross, buffer, sSeed + 13L, len * 0.82f, 0.13f * scale, 0.0f,
                    0.0f, 0.95f, 1.0f, 0.90f, fade, distScale);
                // Gold shard fan guard across the blade, facing along it.
                PoseStack.Pose guard = sword.copy();
                guard.translate(len * 0.18f, 0.0f, 0.0f);
                guard.rotate(Axis.ZP.rotationDegrees(90.0f));
                BespokeGeometry.shardFan(guard, buffer, sSeed + 19L, 0.30f * scale,
                    1.0f, 0.85f, 0.20f, 0.95f, fade, distScale);
                // Pommel shard star at the hilt end.
                PoseStack.Pose pommel = sword.copy();
                pommel.rotate(Axis.ZP.rotationDegrees(90.0f));
                BespokeGeometry.shardFan(pommel, buffer, sSeed + 29L, 0.17f * scale,
                    1.0f, 0.95f, 0.40f, 1.0f, fade, distScale);
                // Energy tendril trailing back into the sky, cyan with a
                // white core, rising as the old beam tendril did.
                PoseStack.Pose tendril = sword.copy();
                tendril.rotate(Axis.YP.rotationDegrees(180.0f));
                BespokeGeometry.tornRibbon(tendril, buffer, sSeed + 37L, 1.4f * scale, 0.16f * scale, 0.3f,
                    0.0f, 0.90f, 1.0f, 0.65f, fade, distScale);
                BespokeGeometry.tornRibbon(tendril, buffer, sSeed + 41L, 1.4f * scale, 0.06f * scale, 0.3f,
                    1.0f, 1.0f, 1.0f, 0.85f, fade, distScale);
            }

            // 4. Crystal perimeter: two seeded shard fans where the eight
            // regular spikes stood, an uneven cyan crystal ring with a
            // white heart. Radius rides baseRadius, so the fans erupt
            // with the slam exactly as the spikes grew.
            if (fade > 0.02f) {
                BespokeGeometry.shardFan(pose, buffer, seed + 51L, baseRadius * 0.98f,
                    0.0f, 0.70f, 0.95f, 0.85f, fade, distScale);
                BespokeGeometry.shardFan(pose, buffer, seed + 52L, baseRadius * 0.55f,
                    1.0f, 1.0f, 1.0f, 0.55f, fade, distScale);
            }
        });

        // Additive glow halo: the shared vortex bands and anchor glow are
        // kept verbatim from the spectacle pass, and the array gets a
        // bespoke bloom echo (crack star plus gold glow) in place of the
        // old regular ring and cylinder copies.
        if (fade > 0.02f) {
            float haloRadius = 2.4f * scale * slam;
            float haloPillarHeight = 6.0f * slam;
            collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
                PoseStack.Pose outer = pose.copy();
                outer.rotate(Axis.YP.rotationDegrees(age * 22.0f));
                BespokeGeometry.crackStar(outer, buffer, seed + 61L, haloRadius * 1.04f,
                    0.0f, 0.90f, 1.0f, 0.20f, fade, distScale);
                BespokeGeometry.glowFan(pose, buffer, seed + 62L, haloRadius * 0.62f, 0.04f,
                    1.0f, 0.82f, 0.15f, fade * 0.16f, distScale);
                // Wispy outer layer and ground anchor (point 1): three
                // counter rotating shared bands wrap the pillar so the
                // array has a dissolving outer shell, and an anchor glow
                // grounds it to the floor it erupted from.
                LayeredRender.drawVortexBands(pose, buffer, 1.05f * scale, haloPillarHeight, age,
                    0.0f, 0.90f, 1.0f, fade * 0.8f, distScale);
                LayeredRender.drawAnchorGlow(pose, buffer, 2.6f * scale * slam, 0.02f,
                    0.0f, 0.90f, 1.0f, 0.5f * fade, distScale);
            });
        }
    }
}
