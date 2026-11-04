package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.KatanaAimGuideEntity;
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
 * Entity Renderer for Katana Aim Guide in Minecraft 26.2.
 * Renders the Heaven Splitter aim guide from the shared BespokeGeometry
 * toolkit (BESPOKE-STANDARD rule 1): the beam is two crossed torn ribbon
 * sheets (a cyan sheath sheet and a white core sheet, one horizontal and
 * one vertical, so the line reads from every angle), brighter ribbon bands
 * travel down it, the calibrating reticles are full blade arcs with shard
 * fan brackets, the terminal reticle is a blade arc with a white shard fan
 * cross, and the traveling energy nodes are small shard fans. The beam
 * length is the entity's synced max range, which the item keeps equal to
 * the real dash distance at the current charge. Alpha fades in over the
 * first 3 ticks and out over the entity's FADE_TICKS once the stance ends,
 * so the beam never pops. All geometry is seeded from the entity UUID; no
 * local draw helpers remain.
 */
public class KatanaAimGuideRenderer extends EntityRenderer<KatanaAimGuideEntity, KatanaAimGuideRenderer.AimGuideRenderState> {

    public KatanaAimGuideRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class AimGuideRenderState extends EntityRenderState {
        public float chargeRatio = 0.0f;
        public float maxRange = 16.0f;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public boolean isFirstPersonOwner = false;
        public boolean onRight = true;
        public int fadeStart = -1;
        public long seed = 0;
        public double wx = 0, wy = 0, wz = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(KatanaAimGuideEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(KatanaAimGuideEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public AimGuideRenderState createRenderState() {
        return new AimGuideRenderState();
    }

    @Override
    public void extractRenderState(KatanaAimGuideEntity entity, AimGuideRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chargeRatio = entity.getChargeRatio();
        state.maxRange = entity.getMaxRange();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.fadeStart = entity.getFadeStart();
        state.seed = entity.getUUID().getMostSignificantBits();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null && 
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) && 
            mc.options.getCameraType().isFirstPerson());
        if (mc.player != null) {
            boolean isRightHanded = (mc.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            boolean isOffhand = (mc.player.getOffhandItem().getItem() instanceof com.dragonblockarcanedba.item.KatanaItem && 
                !(mc.player.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.KatanaItem));
            state.onRight = isRightHanded ? !isOffhand : isOffhand;
        }
    }

    @Override
    public void submit(AimGuideRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();
        float charge = state.chargeRatio;
        float range = state.maxRange;
        float age = state.age;
        float pulse = 0.85f + 0.15f * (float) Math.sin(age * 0.5f);
        // Lifecycle: smoothstep fade in over 3 ticks, fade out over FADE_TICKS.
        float alpha = (0.55f + charge * 0.45f) * pulse * FxEnvelope.smoothstep(Math.min(1.0f, age / 3.0f));
        if (state.fadeStart >= 0) {
            float fadeT = (age - state.fadeStart) / (float) KatanaAimGuideEntity.FADE_TICKS;
            if (fadeT >= 1.0f) return;
            alpha *= 1.0f - FxEnvelope.smoothstep(fadeT);
        }
        final float alphaF = alpha;
        float distScale = LayeredRender.distScale(cameraState, state.wx, state.wy, state.wz);
        long seed = state.seed;

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        if (state.isFirstPersonOwner) {
            float sideSign = state.onRight ? -1.0f : 1.0f;
            poseStack.translate(sideSign * 0.35f, -0.46f, 0.30f);
        }

        float zStart = state.isFirstPersonOwner ? 0.25f : 0.4f;
        float beamLen = range - zStart;
        float sheathRadius = 0.035f + (charge * 0.025f);
        float coreRadius = 0.012f + (charge * 0.008f);

        // 1 and 2. The beam: crossed torn ribbon sheets run from zStart to
        // the terminal point. A -90 degree yaw turns the ribbon length down
        // +Z; the second sheet adds a 90 degree roll around the beam axis so
        // its width stands vertical. Cyan sheath outside, white core inside.
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.0f, zStart);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.tornRibbon(pose, buffer, seed, beamLen, sheathRadius * 2.4f, 0.0f,
                0.0f, 0.85f, 1.0f, 0.65f, alphaF, distScale);
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0x1111L, beamLen, coreRadius * 2.4f, 0.0f,
                1.0f, 1.0f, 1.0f, 0.95f, alphaF, distScale);
        });
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0f, 0.0f, zStart);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(90.0f)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0x2222L, beamLen, sheathRadius * 2.4f, 0.0f,
                0.0f, 0.85f, 1.0f, 0.65f, alphaF, distScale);
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0x3333L, beamLen, coreRadius * 2.4f, 0.0f,
                1.0f, 1.0f, 1.0f, 0.95f, alphaF, distScale);
        });
        poseStack.popPose();

        // Start ring at the beam origin: a full white blade arc stood into
        // the XY plane, spinning with age.
        float startRingR = (0.16f + charge * 0.10f) * 1.25f;
        float startRot = age * 28.0f;
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.0f, zStart + 0.2f);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(startRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.bladeArc(pose, buffer, seed ^ 0x4444L, startRingR, 360.0f,
                1.0f, 1.0f, 1.0f, 1.0f, alphaF, distScale);
        });
        poseStack.popPose();

        // 2b. Energy flow bands: brighter ribbon segments travel down the
        // beam so the guide reads as flowing energy, not a static line.
        int bandCount = 3;
        for (int b = 0; b < bandCount; b++) {
            float bandProg = (age * 0.10f + (b / (float) bandCount)) % 1.0f;
            float bandZ = zStart + bandProg * (range - zStart);
            float bandEnd = Math.min(range, bandZ + 1.4f);
            float bandAlpha = 0.55f * (1.0f - bandProg * 0.35f);
            long bandSeed = seed ^ (0x5500L + b);
            poseStack.pushPose();
            poseStack.translate(0.0f, 0.0f, bandZ);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.tornRibbon(pose, buffer, bandSeed, bandEnd - bandZ, sheathRadius * 2.4f * 1.2f, 0.0f,
                    0.55f, 0.95f, 1.0f, bandAlpha, alphaF, distScale);
            });
            poseStack.popPose();
        }

        // 3. Calibrating reticle stations along the guide line (every 6
        // blocks): a full cyan blade arc ring with a pale shard fan bracket,
        // counter rotating per station.
        int stationCount = (int) (range / 6.0f);
        for (int s = 1; s <= stationCount; s++) {
            float stationDist = s * 6.0f;
            if (state.isFirstPersonOwner && stationDist < 3.0f) continue;
            float ringR = (0.18f + charge * 0.12f) * (1.0f + 0.1f * (float) Math.sin(age * 0.3f + s));
            float ringRot = age * (15.0f + s * 10.0f);
            long stationSeed = seed ^ (0x6600L + s);
            poseStack.pushPose();
            poseStack.translate(0.0f, 0.0f, stationDist);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(ringRot)));
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.bladeArc(pose, buffer, stationSeed, ringR * 1.25f, 360.0f,
                    0.1f, 0.9f, 1.0f, 0.75f, alphaF, distScale);
                BespokeGeometry.shardFan(pose, buffer, stationSeed ^ 0x77L, ringR * 1.4f,
                    0.85f, 0.98f, 1.0f, 0.85f, alphaF, distScale);
            });
            poseStack.popPose();
        }

        // 4. Terminal reticle at the endpoint: a counter rotating cyan blade
        // arc with a white shard fan cross standing in for the old bracket
        // and crosshair spokes.
        float endR = 0.30f + (charge * 0.20f);
        float endRot = -age * 40.0f;
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.0f, range);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(endRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.bladeArc(pose, buffer, seed ^ 0x8888L, endR * 1.25f, 360.0f,
                0.0f, 0.95f, 1.0f, 0.90f, alphaF, distScale);
            BespokeGeometry.shardFan(pose, buffer, seed ^ 0x9999L, endR * 1.6f,
                1.0f, 1.0f, 1.0f, 1.0f, alphaF, distScale);
        });
        poseStack.popPose();

        // 5. High velocity energy nodes traveling down the beam: small shard
        // fans lying in the XZ plane at each node's z.
        int nodeCount = 4;
        for (int n = 0; n < nodeCount; n++) {
            float nodeProg = (age * 0.15f + (n / (float) nodeCount)) % 1.0f;
            float nodeZ = 0.5f + nodeProg * (range - 0.5f);
            float nodeAlpha = 1.0f - nodeProg * 0.3f;
            long nodeSeed = seed ^ (0xAA00L + n);
            poseStack.pushPose();
            poseStack.translate(0.0f, 0.0f, nodeZ);
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.shardFan(pose, buffer, nodeSeed, 0.14f * (1.0f + charge * 0.5f),
                    1.0f, 1.0f, 1.0f, nodeAlpha, alphaF, distScale);
            });
            poseStack.popPose();
        }

        // Additive halo pass: the terminal reticle and the beam sheath get a
        // soft bloom so the aim point glows against the world.
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.0f, range);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(endRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            BespokeGeometry.bladeArc(pose, buffer, seed ^ 0x8888L, endR * 1.25f * 1.25f, 360.0f,
                0.0f, 0.95f, 1.0f, 0.20f, alphaF, distScale);
        });
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0f, 0.0f, zStart);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-90.0f)));
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            BespokeGeometry.tornRibbon(pose, buffer, seed, beamLen, sheathRadius * 3.8f, 0.0f,
                0.0f, 0.85f, 1.0f, 0.10f, alphaF, distScale);
        });
        poseStack.popPose();

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
