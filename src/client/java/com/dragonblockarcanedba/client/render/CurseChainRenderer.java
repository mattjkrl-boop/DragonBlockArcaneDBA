package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.CurseChainEntity;
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
 * Entity Renderer for Curse Chain in Minecraft 26.2.
 * The bespoke pass rebuilds the chain entirely on BespokeGeometry: the
 * links are tornRibbon segments (dark obsidian sheath, violet rune core on
 * the identical seed) knotted with small displacedDome lumps, the shackle
 * collar is three brokenVortexBands, the snap shockwave is a glowFan, the
 * runic glyphs and the hex core are displacedDomes, and the aura is two
 * helical tornRibbon columns following the same path math the old beam
 * helpers used. The only local helper left orients a pose along a segment
 * and delegates all vertex emission to the toolkit. Layout, envelope, and
 * counts are unchanged from the spectacle pass: seven links on the bind
 * arc, six in flight, fade in over four ticks, fade out over the bind's
 * synced last twelve ticks. Seeds come from the entity id.
 */
public class CurseChainRenderer extends EntityRenderer<CurseChainEntity, CurseChainRenderer.CurseChainRenderState> {

    public CurseChainRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class CurseChainRenderState extends EntityRenderState {
        public float ageInTicks;
        public boolean isAttached;
        public int orbitIndex;
        public float yRot;
        public float xRot;
        public float attachRemaining = -1;
        public long seed = 0;
    }

    @Override
    public CurseChainRenderState createRenderState() {
        return new CurseChainRenderState();
    }

    @Override
    public void extractRenderState(CurseChainEntity entity, CurseChainRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.ageInTicks = entity.tickCount + partialTicks;
        state.isAttached = entity.isAttached();
        state.attachRemaining = entity.getAttachRemaining();
        state.orbitIndex = entity.getOrbitIndex();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.seed = entity.getId() * 0x9E3779B97F4A7C15L;
    }

    @Override
    public void submit(CurseChainRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();

        float age = state.ageInTicks;
        boolean attached = state.isAttached;
        int orbit = state.orbitIndex;
        long seed = state.seed;
        float stretch = ease(Math.min(1.0f, age / 6.0f));

        // Lifecycle envelope: fade in over the first 4 ticks of the chain's
        // life, and once attached fade out over the bind's last 12 ticks
        // (synced by the entity), so the chain never pops in or out.
        final float ds = LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        float fadeIn = FxEnvelope.smoothstep(Math.min(1.0f, age / 4.0f));
        float fadeOut = 1.0f;
        if (attached && state.attachRemaining >= 0) {
            fadeOut = FxEnvelope.smoothstep(Math.min(1.0f, state.attachRemaining / 12.0f));
        }
        final float alphaMul = fadeIn * fadeOut;

        float baseScale = attached ? 0.55f : 0.46f;
        poseStack.scale(baseScale, baseScale, baseScale);
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.yRot)));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(state.xRot)));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            if (alphaMul <= 0.01f) return;
            float pulse = 0.85f + 0.15f * (float) Math.sin(age * 0.35f + orbit);
            int links = attached ? 7 : 6;
            float linkWidth = attached ? 0.30f : 0.24f;

            // Link centers: the same arc (attached) and trail (flying)
            // layout the old torus links used.
            Vec3[] centers = new Vec3[links];
            for (int i = 0; i < links; i++) {
                float along = i / (float) (links - 1);
                float lag = along * (0.25f + 0.75f * stretch);
                if (attached) {
                    float arc = (along - 0.5f) * 2.4f + orbit * 0.35f;
                    float radius = 1.15f * (1.0f - 0.18f * stretch);
                    centers[i] = new Vec3(Math.cos(arc) * radius, Math.sin(along * Math.PI) * 0.35f, Math.sin(arc) * radius);
                } else {
                    centers[i] = new Vec3(0, 0, -lag * 3.4f);
                }
            }

            // The chain body: torn ribbon segments between centers, dark
            // sheath with a violet rune core on the same seed, plus a
            // displaced dome knot at every link center for volume.
            for (int i = 0; i < links; i++) {
                PoseStack.Pose knot = pose.copy();
                knot.translate((float) centers[i].x, (float) centers[i].y, (float) centers[i].z);
                BespokeGeometry.displacedDome(knot, buffer, seed + i * 7L, linkWidth * 0.85f,
                    0.12f, 0.08f, 0.16f, 0.95f, alphaMul, ds);
                if (i + 1 < links) {
                    long segSeed = seed + i * 61L;
                    segmentRibbon(pose, buffer, segSeed, centers[i], centers[i + 1], linkWidth, 0.06f,
                        0.12f, 0.08f, 0.16f, 0.95f, alphaMul, ds);
                    segmentRibbon(pose, buffer, segSeed, centers[i], centers[i + 1], linkWidth * 0.45f, 0.06f,
                        0.65f * pulse, 0.05f, 0.85f * pulse, 0.95f, alphaMul, ds);
                }
            }

            if (attached) {
                // Shackle collar: three broken bands at the bind point.
                float collarPulse = pulse * (0.7f + 0.3f * ease(Math.min(1.0f, age / 3.0f)));
                BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xC011L, 0.85f, -0.06f, 0.06f,
                    0.5f, 0.22f, 0.05f, 0.32f, alphaMul * 0.95f, ds);
                BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xC012L, 1.00f, -0.05f, 0.05f,
                    -0.7f, 0.90f * collarPulse, 0.05f, 0.20f, alphaMul * 0.98f, ds);
                BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xC013L, 1.15f, -0.04f, 0.04f,
                    0.9f, 0.25f, 0.05f, 0.35f, alphaMul * 0.95f, ds);

                // Snap shockwave: a pale glow fan bursting out on attach.
                float snapIn = ease(Math.min(1.0f, age / 3.0f));
                float snapOut = ease(Math.max(0.0f, 1.0f - Math.max(0.0f, age - 3.0f) / 5.0f));
                float snap = snapIn * snapOut;
                if (snap > 0.02f) {
                    BespokeGeometry.glowFan(pose, buffer, seed ^ 0x51A9L, 0.35f + 0.85f * snapIn, 0.0f,
                        1.0f, 0.85f, 0.95f, snap * alphaMul, ds);
                }
            }

            // Hex curse core at the chain's heart.
            PoseStack.Pose core = pose.copy();
            core.translate(0, attached ? 0.05f : 0.0f, 0);
            float coreSize = (attached ? 0.34f : 0.24f) * pulse;
            BespokeGeometry.displacedDome(core, buffer, seed ^ 0x4E11L, coreSize * 1.6f,
                0.95f, 0.05f, 0.45f, 1.0f, alphaMul, ds);

            // Four runic glyphs riding the chain.
            for (int g = 0; g < 4; g++) {
                float gx, gy, gz;
                if (attached) {
                    double ga = age * 0.11 + g * (Math.PI * 0.5) + orbit * 0.7;
                    gx = (float) Math.cos(ga) * 1.5f;
                    gz = (float) Math.sin(ga) * 1.5f;
                    gy = (float) Math.sin(age * 0.23 + g * 1.7) * 0.3f;
                } else {
                    gx = (float) Math.sin(age * 0.3 + g * 1.3) * 0.18f;
                    gy = (float) Math.cos(age * 0.26 + g * 1.1) * 0.18f;
                    gz = -(0.9f + g * 0.75f);
                }
                PoseStack.Pose glyph = pose.copy();
                glyph.translate(gx, gy, gz);
                BespokeGeometry.displacedDome(glyph, buffer, seed + g * 13L, 0.22f * pulse,
                    0.75f * pulse, 0.05f, 1.0f, 0.95f, alphaMul * 0.85f, ds);
            }

            // Helical aura: two torn ribbon columns winding around the
            // chain, violet and crimson, on the old helix path.
            drawAuraRibbons(pose, buffer, seed, age, attached, pulse, alphaMul * 0.6f, ds);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    /**
     * Pose glue only: copies the pose, moves it to the segment start, aims
     * its +X axis along the segment, and lets BespokeGeometry.tornRibbon do
     * all vertex emission. No geometry is built here.
     */
    private static void segmentRibbon(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buffer,
                                      long seed, Vec3 from, Vec3 to, float width, float bend,
                                      float r, float g, float b, float alpha, float envelope, float ds) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 0.001) return;
        PoseStack.Pose seg = pose.copy();
        seg.translate((float) from.x, (float) from.y, (float) from.z);
        float yaw = (float) Math.atan2(-d.z, d.x);
        float pitch = (float) Math.atan2(d.y, Math.hypot(d.x, d.z));
        seg.rotate(Axis.YP.rotation(yaw));
        seg.rotate(Axis.ZP.rotation(pitch));
        BespokeGeometry.tornRibbon(seg, buffer, seed, (float) len, width, bend, r, g, b, alpha, envelope, ds);
    }

    private static void drawAuraRibbons(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buffer,
                                        long seed, float age, boolean attached, float pulse, float alphaMul, float ds) {
        int steps = 14;
        float heightSpan = 2.4f;
        float ribbonR = 0.65f;
        for (int ribbon = 0; ribbon < 2; ribbon++) {
            float phaseOffset = ribbon * (float) Math.PI;
            float rCol = ribbon == 0 ? 0.65f : 0.95f;
            float gCol = 0.02f;
            float bCol = ribbon == 0 ? 1.0f : 0.25f;
            Vec3 prev = null;
            for (int s = 0; s <= steps; s++) {
                float p = s / (float) steps;
                float y = -heightSpan * 0.5f + (p * heightSpan);
                double a = (p * Math.PI * 4.0) + (age * 0.25f) + phaseOffset;
                float rr = ribbonR + (float) Math.sin(p * Math.PI) * 0.3f;
                Vec3 point = new Vec3(Math.cos(a) * rr, y, Math.sin(a) * rr);
                if (!attached) {
                    // In flight the column lies along the -Z trail, centered
                    // on the links: rotate X by 90 degrees, then shift back.
                    point = new Vec3(point.x, -point.z, point.y - 1.7f);
                }
                if (prev != null) {
                    float width = 0.09f * (float) Math.sin(p * Math.PI) * pulse;
                    if (width > 0.005f) {
                        segmentRibbon(pose, buffer, seed + ribbon * 1000L + s * 17L, prev, point, width, 0.03f,
                            rCol, gCol, bCol, 0.75f, alphaMul, ds);
                    }
                }
                prev = point;
            }
        }
    }

    private static float ease(float t) {
        float c = Math.max(0.0f, Math.min(1.0f, t));
        return c * c * (3.0f - 2.0f * c);
    }
}
