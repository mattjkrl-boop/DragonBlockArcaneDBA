package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.HeavenSplitterEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

/**
 * Entity Renderer for Heaven Splitter in Minecraft 26.2.
 * Renders a towering, physical 3D dimensional slash model across the entire dash path:
 * - The reality cut wall in three layers: a wide cyan outer mantle, the main
 *   wall, and a blinding white inner core wall, all on the shared FxEnvelope.
 *   Each layer is a torn ribbon from the shared BespokeGeometry toolkit,
 *   stood on edge and run backwards along the path so its bright end lands
 *   at the far end of the cut, like the old wall's rising lead alpha.
 * - Angled cross cleaves alternating along the wall, as tilted blade arcs
 * - Transversal reality fracture ribs, placed from the entity seed, drawn
 *   as branching bolt ribbons across the wall face
 * - Sonic shock rings at the start, midpoint, and end, as full blade arcs
 *   peaking at the tick the cut lands (25 percent of life)
 * - An additive ground anchor glow under the midpoint of the path
 * All geometry comes from BespokeGeometry seeded from the entity UUID
 * (BESPOKE-STANDARD rule 1); no local draw helpers remain.
 */
public class HeavenSplitterRenderer extends EntityRenderer<HeavenSplitterEntity, HeavenSplitterRenderer.HeavenSplitterRenderState> {

    public HeavenSplitterRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class HeavenSplitterRenderState extends EntityRenderState {
        public Vec3 direction = new Vec3(1, 0, 0);
        public float dashLength = 16.0f;
        public float scale = 1.0f;
        public float age = 0;
        public int maxLifetime = 20;
        public long seed = 0;
        public double wx = 0, wy = 0, wz = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(HeavenSplitterEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(HeavenSplitterEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public HeavenSplitterRenderState createRenderState() {
        return new HeavenSplitterRenderState();
    }

    @Override
    public void extractRenderState(HeavenSplitterEntity entity, HeavenSplitterRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.direction = entity.getDashDirection();
        state.dashLength = entity.getDashLength();
        state.scale = entity.getSlashScale();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    @Override
    public void submit(HeavenSplitterRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        // Shared lifecycle: build over the first 20 percent, dissipate over
        // the last 35. The wall finishes rising at 25 percent of life, the
        // exact tick the entity lands its damage.
        float fade = FxEnvelope.envelope(state.age, state.maxLifetime, 0.20f, 0.35f);
        if (fade <= 0.0f) return;
        float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
        float rise = FxEnvelope.smoothstep(Math.min(1.0f, progress / 0.25f));
        float impact = strike(progress);
        float scale = state.scale * (0.92f + 0.14f * rise);
        float dist = LayeredRender.distScale(cameraState, state.wx, state.wy, state.wz);
        RenderType renderType = KiRenderHelper.kiRenderType();
        long seed = state.seed;

        Vec3 dir = state.direction.normalize();
        double horiz = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        float yaw = (float) (Math.atan2(dir.z, dir.x) * (180.0 / Math.PI)) - 90.0f;
        float pitch = (float) -(Math.atan2(dir.y, horiz) * (180.0 / Math.PI));

        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-yaw)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(pitch)));

        float len = state.dashLength;
        float slashHeight = 7.2f * scale * (0.12f + 0.88f * rise);
        float bend = 0.35f * scale;

        // The wall: three torn ribbons sharing one pose. The pose stands the
        // ribbon's width vertical and runs its length from the far end of
        // the path back to the origin, so the ribbon's bright start sits at
        // the far end and its torn taper dies at the player's feet. Width is
        // the layer height, centered on half the wall height.
        poseStack.pushPose();
        poseStack.translate(0.0f, slashHeight * 0.5f, len);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(90.0f)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.tornRibbon(pose, buffer, seed, len, slashHeight * 0.92f, bend,
                0.0f, 0.75f, 1.0f, 0.22f, fade, dist);
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0x1111L, len, slashHeight, bend,
                0.0f, 0.85f, 1.0f, 0.82f, fade, dist);
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0x2222L, len, slashHeight * 0.58f, bend,
                1.0f, 1.0f, 1.0f, 1.0f, fade, dist);
        });
        poseStack.popPose();

        // Angled cross cleaves alternating along the wall: tilted blade arcs
        // stood into the wall plane, one submit each.
        int arcs = Math.max(4, (int) (len / 3.5f));
        for (int s = 0; s <= arcs; s++) {
            float z = len * (s / (float) arcs);
            float angle = (s % 2 == 0 ? 36.0f : -36.0f);
            float arcSpan = 1.55f * scale * (0.78f + 0.22f * (float) Math.sin(s * 1.3));
            long cleaveSeed = seed ^ (0x3300L + s);
            poseStack.pushPose();
            poseStack.translate(0.0f, 1.0f, z);
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.ZP.rotationDegrees(angle)));
            poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                BespokeGeometry.bladeArc(pose, buffer, cleaveSeed, arcSpan * 0.9f, 120.0f,
                    0.0f, 0.90f, 1.0f, 0.72f, fade, dist);
            });
            poseStack.popPose();
        }

        // Fracture ribs across the wall, placed from the entity seed so every
        // cast cracks differently but deterministically: each rib is two
        // branching bolt ribbons meeting at an apex on the wall face.
        BespokeGeometry.Rng rng = new BespokeGeometry.Rng(seed);
        int ribs = Math.max(3, (int) (len / 5.0f));
        float[][] ribData = new float[ribs][3];
        for (int s = 1; s <= ribs; s++) {
            ribData[s - 1][0] = len * (s / (float) (ribs + 1)) + rng.range(-0.6f, 0.6f);
            ribData[s - 1][1] = (1.1f + rng.nextFloat() * 0.7f) * scale;
            ribData[s - 1][2] = slashHeight * (0.35f + rng.nextFloat() * 0.3f);
        }
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            for (int s = 0; s < ribData.length; s++) {
                float z = ribData[s][0], w = ribData[s][1], h = ribData[s][2];
                BespokeGeometry.branch(pose, buffer, seed ^ (0x4400L + s),
                    new Vec3(-w, 0, z), new Vec3(0, h, z), 0.10f * scale,
                    0.65f, 0.92f, 1.0f, 0.5f, fade, dist);
                BespokeGeometry.branch(pose, buffer, seed ^ (0x5500L + s),
                    new Vec3(0, h, z), new Vec3(w, 0, z), 0.10f * scale,
                    0.65f, 0.92f, 1.0f, 0.5f, fade, dist);
            }
        });

        // Sonic shock rings at the start, midpoint, and end of the path:
        // full blade arcs stood into the wall plane, peaking at the strike.
        if (impact > 0.02f) {
            float ringR = 1.7f * scale * (0.55f + 0.45f * impact);
            float[] ringZ = {0.0f, len * 0.5f, len};
            float[] ringScale = {1.0f, 1.15f, 1.25f};
            float[][] ringColor = {{0.0f, 0.85f, 1.0f}, {1.0f, 1.0f, 1.0f}, {0.0f, 0.95f, 1.0f}};
            float[] ringAlpha = {0.75f, 0.9f, 1.0f};
            for (int k = 0; k < 3; k++) {
                float zPos = ringZ[k];
                float radius = ringR * ringScale[k] * 1.25f;
                float cr = ringColor[k][0], cg = ringColor[k][1], cb = ringColor[k][2];
                float ra = ringAlpha[k];
                long ringSeed = seed ^ (0x6600L + k);
                poseStack.pushPose();
                poseStack.translate(0.0f, 0.0f, zPos);
                poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
                collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                    BespokeGeometry.bladeArc(pose, buffer, ringSeed, radius, 360.0f,
                        cr, cg, cb, ra, impact, dist);
                });
                poseStack.popPose();
            }
        }

        // Ground anchor glow under the midpoint, so the wall sits in the
        // world instead of floating over it (standard points 1 and 3).
        poseStack.pushPose();
        poseStack.translate(0, 0.03f, len * 0.5f);
        collector.submitCustomGeometry(poseStack, KiRenderHelper.kiAdditiveType(), (pose, buffer) -> {
            LayeredRender.drawAnchorGlow(pose, buffer, 2.0f * scale, 0.0f, 0.0f, 0.85f, 1.0f, fade * 0.45f, dist);
        });
        poseStack.popPose();

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    /** Sine flash peaking at 25 percent of life, when the cut lands, gone by half. */
    private static float strike(float progress) {
        if (progress >= 0.5f) return 0.0f;
        return (float) Math.sin((progress / 0.5f) * Math.PI);
    }
}
