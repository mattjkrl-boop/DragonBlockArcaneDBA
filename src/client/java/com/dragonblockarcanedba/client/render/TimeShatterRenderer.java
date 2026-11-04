package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.TimeShatterEntity;
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
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Entity Renderer for Time Shatter in Minecraft 26.2.
 *
 * Scope 2 conversion: every shape is built by BespokeGeometry from the
 * entity's synced seed, one derived seed per layer, so the dial, spokes,
 * shards, and core draw the same on every client and never share a random
 * stream. Same composition and identity as the old hand-rolled burst:
 * - 16 prismatic glass and chrono-mirror shards tumbling in 3D space,
 *   each a small shard fan cluster with a torn ribbon wake
 * - Fractured radial chrono-mirror web and crack spokes (branch ribbons)
 * - Splintering celestial chrono-dial ring (broken vortex bands)
 * - Radiant celestial core (a displaced dome pair in place of the old
 *   octahedron, the first pass's shipped workaround for the missing
 *   octahedron builder)
 * The life is 36 ticks now, and alpha runs on the shared FxEnvelope:
 * a smoothstep build over the first 5 ticks, a hold, then the old
 * back-half smoothstep exit, instead of full alpha from frame one.
 */
public class TimeShatterRenderer extends EntityRenderer<TimeShatterEntity, TimeShatterRenderer.ShatterRenderState> {

    public TimeShatterRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ShatterRenderState extends EntityRenderState {
        public float scale = 1.2f;
        public float age = 0;
        public int maxLifetime = 36;
        public long seed = 0;
        public double worldX = 0;
        public double worldY = 0;
        public double worldZ = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(TimeShatterEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(TimeShatterEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ShatterRenderState createRenderState() {
        return new ShatterRenderState();
    }

    @Override
    public void extractRenderState(TimeShatterEntity entity, ShatterRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getShatterScale();
        state.age = entity.tickCount + partialTicks;
        state.maxLifetime = entity.getMaxLifetime();
        long seed = entity.getSeed();
        if (seed == 0L) {
            // Fallback for the frames before the synced seed lands: hash the
            // entity UUID, which is itself synced state and agrees on every
            // client. Never a local random.
            seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();
            if (seed == 0L) seed = 0x5EEDL;
        }
        state.seed = seed;
        state.worldX = entity.getX();
        state.worldY = entity.getY();
        state.worldZ = entity.getZ();
    }

    @Override
    public void submit(ShatterRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float progress = Math.min(1.0f, state.age / (float) state.maxLifetime);
        // Lifecycle envelope: smoothstep build over the first 5 ticks
        // (0.14 of the 36 tick life), hold, then dissipate over the back
        // half. The 0.50 decay fraction reproduces the old exit-only fade
        // curve exactly; the build term is what the old renderer lacked.
        float fade = FxEnvelope.envelope(state.age, (float) state.maxLifetime, 0.14f, 0.50f);
        if (fade <= 0.001f) return;

        float travel = 1.0f - (1.0f - progress) * (1.0f - progress) * (1.0f - progress);
        float scale = state.scale * (0.55f + travel * 0.85f);
        float distScale = LayeredRender.distScale(cameraState, state.worldX, state.worldY, state.worldZ);
        long seed = state.seed;
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();

        // The dial and spokes are drawn flat in the local XY plane and the
        // entity syncs no yaw, so they used to face one fixed world direction
        // and nearly vanish edge-on. That layer is billboarded to the camera
        // with the conjugated camera orientation; the 3D shards and core
        // below stay in world space.
        poseStack.pushPose();
        poseStack.mulPose(new Matrix4f().rotation(new Quaternionf(cameraState.orientation).conjugate()));

        // 1. Splintering Celestial Chrono-Dial Ring: two broken vortex
        // bands on a pose turned 90 degrees about X, which lays the band's
        // circular plane into the billboard's XY plane and its height into
        // the dial's thickness. The bright band carries the old dial color;
        // the narrower, dimmer band inside it carries the old alternating
        // segments' cooler tone. The bands' seeded column gaps are the
        // splintering. Twist runs negative because the X turn mirrors the
        // band's angle direction in the XY plane; this keeps the dial
        // spinning the old way (25 degrees per tick).
        poseStack.pushPose();
        poseStack.mulPose(new Matrix4f().rotation(Axis.XP.rotationDegrees(90.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            float ringR = 1.8f * scale;
            float ringW = 0.12f * (1.0f - progress * 0.6f);
            float twist = (float) -Math.toRadians(state.age * 25.0f);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xD1A1L, ringR, -ringW, ringW, twist,
                0.95f, 0.98f, 1.0f, fade * 0.85f, distScale);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xD1A2L, ringR * 0.93f, -ringW * 0.7f, ringW * 0.7f, twist,
                0.60f, 0.90f, 1.0f, fade * 0.55f, distScale);
        });
        poseStack.popPose();

        // 2. Fractured Chrono-Mirror Web and Radial Crack Spokes: 8 branch
        // ribbons from the center, placed by this layer's own Rng stream on
        // the synced seed (the old code restarted the dial lambda's stream
        // here, correlating every shard with a spoke). Each spoke is two
        // branch passes: a cyan body at the old tip color and alpha, and a
        // thinner near-white core at the old base color and alpha, standing
        // in for the old two-tone lerp and white core strip.
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.Rng rng = new BespokeGeometry.Rng(seed ^ 0x5907EL);
            int spokeCount = 8;
            float spokeWidth = 0.16f * (1.0f - progress * 0.8f);
            for (int i = 0; i < spokeCount; i++) {
                double angle = (i / (double) spokeCount) * Math.PI * 2.0 + rng.nextFloat() * 0.2f;
                float spokeLen = (1.5f + rng.nextFloat() * 1.0f) * scale;
                float tipX = (float) Math.cos(angle) * spokeLen;
                float tipY = (float) Math.sin(angle) * spokeLen;
                float tipZ = (rng.nextFloat() - 0.5f) * 0.4f * scale;
                Vec3 tip = new Vec3(tipX, tipY, tipZ);
                BespokeGeometry.branch(pose, buffer, seed + 101L + 7919L * i, Vec3.ZERO, tip, spokeWidth,
                    0.50f, 0.85f, 1.0f, 0.70f, fade, distScale);
                BespokeGeometry.branch(pose, buffer, seed + 202L + 7919L * i, Vec3.ZERO, tip, spokeWidth * 0.42f,
                    0.95f, 0.98f, 1.0f, 0.90f, fade, distScale);
            }
        });

        poseStack.popPose();

        // 3. 16 Physical 3D Prismatic Glass and Chrono-Mirror Shards.
        // Placement (angle, speed, offsets, size, tumble) is the old math,
        // drawn from this layer's own Rng stream on the synced seed. Each
        // shard is a shard fan cluster in its theme color with a smaller
        // white core fan, on a pose copy tumbled by the shard's rotation;
        // its wake is a torn ribbon rooted at the shard and aimed back
        // toward the center, so the ribbon's bright root sits at the shard
        // and it dies inward, the old wake's gradient direction.
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            BespokeGeometry.Rng rng = new BespokeGeometry.Rng(seed ^ 0x51A9DL);
            int shardCount = 16;
            for (int s = 0; s < shardCount; s++) {
                double shardAngle = (s / (double) shardCount) * Math.PI * 2.0 + rng.nextFloat() * 0.35f;
                float speed = 1.4f + rng.nextFloat() * 1.8f;
                float dist = travel * speed * 2.4f * scale;

                float sx = (float) Math.cos(shardAngle) * dist;
                float sy = (float) Math.sin(shardAngle) * dist + (rng.nextFloat() - 0.5f) * 0.6f;
                float sz = (rng.nextFloat() - 0.5f) * dist * 0.8f;

                float shardSize = (0.22f + rng.nextFloat() * 0.18f) * (1.0f - progress * 0.7f) * scale;
                float rot = state.age * (30.0f + rng.nextFloat() * 50.0f);
                float tilt = 35.0f + rng.nextFloat() * 50.0f;

                // Iridescent celestial colors: cyan, diamond-white, and lilac
                float cr = s % 3 == 0 ? 0.95f : (s % 3 == 1 ? 0.50f : 0.85f);
                float cg = s % 3 == 0 ? 0.98f : (s % 3 == 1 ? 0.90f : 0.75f);
                float cb = 1.0f;

                float len = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
                if (len > 0.01f) {
                    PoseStack.Pose wake = pose.copy();
                    wake.translate(sx, sy, sz);
                    wake.rotate(new Quaternionf().rotationTo(new Vector3f(1.0f, 0.0f, 0.0f),
                        new Vector3f(-sx / len, -sy / len, -sz / len)));
                    BespokeGeometry.tornRibbon(wake, buffer, seed + 303L + 7919L * s,
                        len * 0.38f, shardSize * 0.22f, len * 0.02f,
                        1.0f, 1.0f, 1.0f, 0.50f, fade, distScale);
                }

                PoseStack.Pose fan = pose.copy();
                fan.translate(sx, sy, sz);
                fan.rotate(Axis.YP.rotationDegrees(rot));
                fan.rotate(Axis.XP.rotationDegrees(tilt));
                BespokeGeometry.shardFan(fan, buffer, seed + 404L + 7919L * s, shardSize * 1.8f,
                    cr, cg, cb, 1.0f, fade, distScale);

                PoseStack.Pose coreFan = pose.copy();
                coreFan.translate(sx, sy, sz);
                coreFan.rotate(Axis.YP.rotationDegrees(rot + 16.0f));
                coreFan.rotate(Axis.ZP.rotationDegrees(48.0f));
                BespokeGeometry.shardFan(coreFan, buffer, seed + 505L + 7919L * s, shardSize * 0.75f,
                    1.0f, 1.0f, 1.0f, 0.80f, fade, distScale);
            }
        });

        // 4. Radiant Celestial Core: a displaced dome pair (one flipped)
        // in place of the old octahedron, spinning at the old rate
        // (40 degrees per tick), with a smaller brighter dome pair nested
        // inside as the hot heart where the old apex color sat.
        poseStack.pushPose();
        poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(state.age * 40.0f)));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            float coreSize = 0.85f * (1.0f - progress * 0.7f) * scale;
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xC09EL, coreSize * 0.9f,
                0.80f, 0.93f, 1.0f, 0.85f, fade, distScale);
            PoseStack.Pose flipped = pose.copy();
            flipped.rotate(Axis.XP.rotationDegrees(180.0f));
            BespokeGeometry.displacedDome(flipped, buffer, seed ^ 0xC09FL, coreSize * 0.9f,
                0.80f, 0.93f, 1.0f, 0.85f, fade, distScale);
            BespokeGeometry.displacedDome(pose, buffer, seed ^ 0xC0A0L, coreSize * 0.5f,
                0.98f, 0.98f, 1.0f, 1.0f, fade, distScale);
            PoseStack.Pose flippedInner = pose.copy();
            flippedInner.rotate(Axis.XP.rotationDegrees(180.0f));
            BespokeGeometry.displacedDome(flippedInner, buffer, seed ^ 0xC0A1L, coreSize * 0.5f,
                0.98f, 0.98f, 1.0f, 1.0f, fade, distScale);
        });
        poseStack.popPose();

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
