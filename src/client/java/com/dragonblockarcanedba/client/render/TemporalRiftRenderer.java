package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.TemporalRiftEntity;
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
import org.joml.Matrix4f;

/**
 * Entity Renderer for Temporal Rift.
 *
 * Bespoke pass: every shape is built by BespokeGeometry from the entity's
 * synced seed, so the silhouette is identical on every client and holds
 * frame to frame; only alpha, radius, and band twist breathe. Same
 * composition and identity as the old hand-rolled astrolabe:
 * - Two nested noise-displaced dome shells (celestial cyan outside,
 *   diamond silver inside) in place of the lat/long primitive shells
 * - Three broken vortex bands inside the shells as the time currents
 * - A jittered glow fan anchoring the dome to the ground, ringed by an
 *   uneven shard fan of hour steles
 * - Two torn-ribbon clock hands sweeping backwards over the ground fan
 * - Twelve jagged meridian branch streams rising from the rim to the apex
 * - Two tilted broken orbital rings, and displaced-dome crystal caps at
 *   the center and the zenith in place of the octahedron jewels
 * All alpha runs on the shared FxEnvelope (build 15 percent, peak,
 * dissipate 35 percent), and builder segment counts step down with
 * camera distance.
 */
public class TemporalRiftRenderer extends EntityRenderer<TemporalRiftEntity, TemporalRiftRenderer.RiftRenderState> {

    public TemporalRiftRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class RiftRenderState extends EntityRenderState {
        public float radius = 12.0f;
        public float ageInTicks = 0;
        public int maxLifetime = 60;
        public long seed = 0;
        public double worldX = 0;
        public double worldY = 0;
        public double worldZ = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(TemporalRiftEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(TemporalRiftEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public RiftRenderState createRenderState() {
        return new RiftRenderState();
    }

    @Override
    public void extractRenderState(TemporalRiftEntity entity, RiftRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.ageInTicks = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        long seed = entity.getSeed();
        if (seed == 0L) {
            // Fallback for the frames before the synced seed lands (or a rift
            // spawned without the cast constructor): hash the synced position
            // and caster, which agree on every client, never a local random.
            seed = (long) entity.getBlockX() * 341873128712L
                ^ (long) entity.getBlockZ() * 132897987541L
                ^ (long) entity.getBlockY() * 42317861L
                ^ (long) entity.getCasterId() * 2654435761L;
            if (seed == 0L) seed = 0x5EEDL;
        }
        state.seed = seed;
        state.worldX = entity.getX();
        state.worldY = entity.getY();
        state.worldZ = entity.getZ();
    }

    @Override
    public void submit(RiftRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float age = state.ageInTicks;
        float maxLife = (float) state.maxLifetime;

        // Shared lifecycle envelope (point 5): build over the first 15
        // percent, peak, dissipate over the last 35 percent. The same
        // envelope drives the entity's particles, drift, and damage, so the
        // dome's alpha, the motes, and the second burst half all agree.
        // Nothing pops in at full alpha and nothing discards at visible alpha.
        float env = FxEnvelope.envelope(age, maxLife, 0.15f, 0.35f);
        if (env <= 0.001f) return;

        // The radius unfolds during the build and holds through dissipation;
        // the old version also shrank the radius to zero over the last 10
        // ticks on top of the fade, which read as the dome collapsing early.
        float buildT = FxEnvelope.smoothstep(Math.min(1.0f, age / (maxLife * 0.15f)));
        float radiusScale = 0.35f + 0.65f * buildT;
        float radius = state.radius * radiusScale;
        float domeHeight = radius * 1.05f;
        float pulse = 0.88f + 0.12f * (float) Math.sin(age * 0.20f);
        long seed = state.seed;

        // Point 1: geometry and alpha step down with camera distance.
        float ds = LayeredRender.distScale(cameraState, state.worldX, state.worldY, state.worldZ);
        float alphaScale = (float) Math.sqrt(ds);
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();

        // Base composition in entity space: domes, currents, ground fan,
        // stele shards, meridian streams, and the center crystal cap.
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // 1. Dome shells: two nested displaced domes. The outer shell
            // keeps the celestial cyan; the inner keeps the silver core that
            // gave the clock a hot center under the glass.
            BespokeGeometry.displacedDome(pose, buffer, seed, radius,
                0.45f, 0.80f, 1.0f, 0.30f * pulse * alphaScale, env, ds);
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0x1A2B3CL, radius * 0.72f,
                0.85f, 0.96f, 1.0f, 0.22f * pulse * alphaScale, env, ds);

            // 2. Time currents: three broken vortex bands counter-rotating
            // inside the shells, at the standard layer alphas (core, mid,
            // outer), sized and twisted as the old layered bands were.
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xBA0D1L, radius * 0.20f,
                0.15f, domeHeight * 0.72f, (float) Math.toRadians(age * 2.6f),
                0.85f, 0.97f, 1.0f, LayeredRender.ALPHA_CORE * 0.85f * env, ds);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xBA0D2L, radius * 0.52f,
                0.10f, domeHeight * 0.72f, (float) Math.toRadians(age * -1.7f),
                0.55f, 0.85f, 1.0f, LayeredRender.ALPHA_MID * 0.85f * env, ds);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xBA0D3L, radius * 0.75f,
                0.10f, domeHeight * 0.79f, (float) Math.toRadians(age * 0.9f),
                0.50f, 0.82f, 1.0f, LayeredRender.ALPHA_OUTER * 0.85f * env, ds);

            // 3. Ground anchor: the jittered glow fan grounds the dome, and
            // the uneven shard fan stands in for the twelve hour steles,
            // bursting around the same rim annulus they occupied.
            BespokeGeometry.glowFan(pose, buffer, seed ^ 0x610FAL, radius * 0.98f, 0.05f,
                0.45f, 0.78f, 1.0f, 0.50f * env * pulse, ds);
            BespokeGeometry.shardFan(pose, buffer, seed ^ 0x57E1EL, radius * 0.88f,
                0.70f, 0.92f, 1.0f, 0.85f * pulse, env, ds);

            // 4. Meridian streams: twelve jagged branch ribbons rising from
            // the rim and converging just off the apex, turning with the
            // ground fan as the old streams did (6 degrees per tick of age).
            float streamRot = (float) Math.toRadians(age * 6.0f);
            for (int stream = 0; stream < 12; stream++) {
                double ang = (stream / 12.0) * Math.PI * 2.0 + streamRot;
                double tipAng = ang + 0.5;
                Vec3 from = new Vec3(Math.cos(ang) * radius * 0.98f, 0.10f, Math.sin(ang) * radius * 0.98f);
                Vec3 to = new Vec3(Math.cos(tipAng) * radius * 0.07f, domeHeight + 0.05f, Math.sin(tipAng) * radius * 0.07f);
                BespokeGeometry.branch(pose, buffer, seed + 7919L * stream, from, to, 0.13f,
                    0.70f, 0.92f, 1.0f, 0.60f * pulse, env, ds);
            }

            // 5. Center crystal cap, where the old center jewel sat.
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xCE77EL, 0.55f * radiusScale,
                1.0f, 1.0f, 1.0f, 0.80f * pulse, env, ds);
        });

        // 6. Clock hands: torn ribbons on Y-rotated poses, sweeping backwards
        // at the old rates (hour hand 15 degrees per tick of age, minute hand
        // 45). The pose rotation degrees are the negation of the old hand
        // angles, because a Y rotation carries +X toward negative Z.
        poseStack.pushPose();
        poseStack.translate(0, 0.13, 0);
        poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(age * 15.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0x40B1L, radius * 0.42f, 0.34f, 0.10f,
                0.95f, 0.98f, 1.0f, 0.90f * pulse, env, ds));
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.15, 0);
        poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(age * 45.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.tornRibbon(pose, buffer, seed ^ 0x40B2L, radius * 0.72f, 0.22f, 0.08f,
                0.60f, 0.90f, 1.0f, 0.90f * pulse, env, ds));
        poseStack.popPose();

        // 7. Orbital rings: broken bands on X-tilted poses at the old tilt
        // (plus and minus 42 degrees) around the old ring height, twisting
        // against each other at the old rate (16 degrees per tick of age).
        poseStack.pushPose();
        poseStack.translate(0, 1.0, 0);
        poseStack.mulPose(new Matrix4f().rotation(Axis.XP.rotationDegrees(42.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x0B17AL, radius * 0.98f,
                -0.10f, 0.10f, (float) Math.toRadians(age * 16.0f),
                0.55f, 0.90f, 1.0f, 0.65f * pulse * env, ds));
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 1.0, 0);
        poseStack.mulPose(new Matrix4f().rotation(Axis.XP.rotationDegrees(-42.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0x0B17BL, radius * 0.98f,
                -0.10f, 0.10f, (float) Math.toRadians(age * -16.0f),
                0.85f, 0.70f, 1.0f, 0.65f * pulse * env, ds));
        poseStack.popPose();

        // 8. Zenith crystal cap at the apex, where the old apex jewel hung.
        poseStack.pushPose();
        poseStack.translate(0, domeHeight + 0.10f, 0);
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xA9E7L, 0.85f * radiusScale,
                0.95f, 0.98f, 1.0f, 0.80f * pulse, env, ds));
        poseStack.popPose();

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
