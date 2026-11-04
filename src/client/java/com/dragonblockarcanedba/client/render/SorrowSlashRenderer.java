package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.SorrowSlashEntity;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Entity Renderer for Sorrow Slash in Minecraft 26.2.
 * Complex pass: the slash gains a body again, as two shader tendrils
 * swept along the crescent arc the old crescent traced: a thick violet
 * to cyan-white strand on the crest arc and a thinner soul strand
 * trailing behind it along the travel direction. Attack upgrade pass:
 * the tendrils double to two pairs (a forward echo of the crest and a
 * deep trailing strand, seeded apart so their curl phases differ),
 * five violet crystal sorrow shards fan along the arc as solid pieces,
 * and when the slash sweeps close over solid ground its low point
 * scores a short crack fan into that ground. WISP_CURL sprites
 * sampled across the swept volume are the complex particle layer, with
 * a wisp burst added to the impact pop. The material trail stays as
 * filler only: void plasma for the crest and the cyan edge, soul wisps
 * and darkness smoke dissolving behind it, and a thin vapor wash,
 * sampled across the volume the crescent swept. Emission and pieces
 * both follow the entity's real look direction and reap tilt, and the
 * lifecycle fade is the old crescent fade, kept. The entity's own
 * server trail particles are untouched.
 */
public class SorrowSlashRenderer extends EntityRenderer<SorrowSlashEntity, SorrowSlashRenderer.SorrowSlashRenderState> {

    public SorrowSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SorrowSlashRenderState extends EntityRenderState {
        public boolean tiltRight = false;
        public boolean isImpactBurst = false;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public double wx = 0, wy = 0, wz = 0;
        public double lookX = 0, lookY = 0, lookZ = 1;
        public long seed = 0;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(SorrowSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(SorrowSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public SorrowSlashRenderState createRenderState() {
        return new SorrowSlashRenderState();
    }

    @Override
    public void extractRenderState(SorrowSlashEntity entity, SorrowSlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.tiltRight = entity.getTilt();
        state.isImpactBurst = entity.isImpactBurst();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
        Vec3 look = entity.getLookAngle();
        state.lookX = look.x;
        state.lookY = look.y;
        state.lookZ = look.z;
        // Seed from synced state only, so every client seeds the same
        // material variation for the same slash.
        state.seed = (long) entity.getId() * 2654435761L
            ^ (entity.getTilt() ? 0x51ED27L : 0x2545F4L)
            ^ (entity.isImpactBurst() ? 0x77AA55L : 0L);
    }

    /**
     * One live slash's emitter set. The sources read the rig's current
     * center, swept-volume basis, and size, which submit refreshes every
     * frame, so the shared Emitter accumulators keep their frame-rate
     * independence and budgets while the trail follows the moving entity.
     */
    private static final class SlashRig {
        final ClientLevel level;
        final boolean impact;
        final List<MaterialEmitters.Emitter> emitters = new ArrayList<>();
        Vec3 center = Vec3.ZERO;
        Vec3 forward = new Vec3(0, 0, 1);
        Vec3 right = new Vec3(1, 0, 0);
        Vec3 up = new Vec3(0, 1, 0);
        float halfSpan = 4.0f;
        float halfHeight = 2.0f;
        float halfDepth = 0.5f;
        float lastAge = 0.0f;
        long lastSeenNanos = 0L;
        long lastTickNanos = 0L;
        boolean burstDone = false;

        SlashRig(ClientLevel level, long seed, boolean impact) {
            this.level = level;
            this.impact = impact;
            emitters.add(new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xCC0DD9, 2.6f, (int) (seed ^ 0xC3E57L), Vec3.ZERO, 0.030f),
                random -> bandSample(random, 1.0f, 1.0f, 1.0f), 130.0f, 170));
            emitters.add(new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xE6FAFF, 1.9f, (int) (seed ^ 0xED6EL), Vec3.ZERO, 0.020f),
                random -> bandSample(random, 0.45f, 0.50f, 0.60f), 85.0f, 115));
            emitters.add(new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.soul(0xB84DFF, 1.8f, (int) (seed ^ 0x5017L), Vec3.ZERO, 0.025f),
                random -> bandSample(random, 1.05f, 1.10f, 1.40f), 55.0f, 75));
            emitters.add(new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.darkness(0x140026, 2.4f, (int) (seed ^ 0xDA3CL), Vec3.ZERO, 0.020f),
                random -> bandSample(random, 1.10f, 1.15f, 1.60f), 35.0f, 50));
            emitters.add(new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(0xBFEFFF, 2.2f, (int) (seed ^ 0x9A90L), Vec3.ZERO, 0.030f),
                random -> bandSample(random, 0.90f, 0.90f, 1.20f), 25.0f, 35));
            // Complex particle layer: wisp curls across the swept band,
            // on the soul sampler, inside the 40 per second budget.
            emitters.add(new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.WISP_CURL, Vec3.ZERO, 0.025f),
                random -> bandSample(random, 1.05f, 1.10f, 1.40f), 30.0f, 40));
        }

        /**
         * A point sampled across the swept volume, with a velocity trailing
         * back along the slash's travel direction so the material reads as a
         * wake, not a cloud parked on the entity.
         */
        private MaterialEmitters.Sample bandSample(RandomSource random, float spanFactor, float heightFactor, float depthFactor) {
            Vec3 pos = center
                .add(right.scale((random.nextDouble() * 2.0 - 1.0) * halfSpan * spanFactor))
                .add(up.scale((random.nextDouble() * 2.0 - 1.0) * halfHeight * heightFactor))
                .add(forward.scale((random.nextDouble() * 2.0 - 1.0) * halfDepth * depthFactor));
            Vec3 velocity = forward.scale(-0.22).add(up.scale(0.015));
            return new MaterialEmitters.Sample(pos, velocity);
        }
    }

    private final Map<Long, SlashRig> rigs = new HashMap<>();

    private SlashRig rigFor(ClientLevel level, long seed, float age, Vec3 center, boolean impact) {
        SlashRig rig = rigs.get(seed);
        if (rig == null || rig.level != level || rig.impact != impact
            || age < rig.lastAge - 0.001f || rig.center.distanceToSqr(center) > 900.0) {
            rig = new SlashRig(level, seed, impact);
            rig.center = center;
            rigs.put(seed, rig);
        }
        return rig;
    }

    private void pruneRigs(long now) {
        Iterator<Map.Entry<Long, SlashRig>> it = rigs.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().lastSeenNanos > 5_000_000_000L) {
                it.remove();
            }
        }
    }

    /**
     * The ground point under a slash low point, or null when the slash
     * is not sweeping close over solid ground: scans down up to 4
     * blocks for the first solid top and accepts it when the low point
     * sits between 0.25 above it sunk and 2.5 blocks above it.
     * Read-only client query; changes no state.
     */
    private static Vec3 groundCrackPoint(ClientLevel level, Vec3 lowPoint) {
        int startY = (int) Math.floor(lowPoint.y);
        for (int dy = 0; dy <= 4; dy++) {
            BlockPos bp = BlockPos.containing(lowPoint.x, startY - dy, lowPoint.z);
            BlockState bs = level.getBlockState(bp);
            var shape = bs.getCollisionShape(level, bp);
            if (shape.isEmpty()) continue;
            double groundTop = bp.getY() + shape.max(Direction.Axis.Y);
            double clearance = lowPoint.y - groundTop;
            return clearance >= -0.25 && clearance <= 2.5
                ? new Vec3(lowPoint.x, groundTop, lowPoint.z) : null;
        }
        return null;
    }

    private static Vec3 rotateAroundAxis(Vec3 vector, Vec3 axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return vector.scale(cos)
            .add(axis.cross(vector).scale(sin))
            .add(axis.scale(axis.dot(vector) * (1.0 - cos)));
    }

    @Override
    public void submit(SorrowSlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            float maxLife = state.isImpactBurst ? 16.0f : 30.0f;
            float progress = Math.min(1.0f, state.age / maxLife);
            float ease = progress * progress * (3.0f - 2.0f * progress);
            float fade = 1.0f - ease;
            // The flying wave eases in. The impact burst opens hot and eases shut.
            if (!state.isImpactBurst) {
                float intro = Math.min(1.0f, state.age / 4.0f);
                fade *= intro * intro * (3.0f - 2.0f * intro);
            }
            // Opens with the old ease, then the fade above closes it.
            float scale = state.isImpactBurst ? (0.85f + ease * 0.45f) : (0.72f + ease * 0.48f);

            Vec3 center = new Vec3(state.wx, state.wy, state.wz);
            Vec3 forward = new Vec3(state.lookX, state.lookY, state.lookZ);
            if (forward.lengthSqr() < 1.0e-6) {
                double yaw = Math.toRadians(state.yRot);
                double pitch = Math.toRadians(state.xRot);
                forward = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
            }
            forward = forward.normalize();
            Vec3 rightBase = new Vec3(-forward.z, 0.0, forward.x);
            if (rightBase.lengthSqr() < 1.0e-6) {
                rightBase = new Vec3(1.0, 0.0, 0.0);
            }
            rightBase = rightBase.normalize();
            Vec3 upBase = rightBase.cross(forward).normalize();

            // Diagonal reap tilt, with the old settle roll for the flying wave.
            float tiltAngle = state.tiltRight ? 42.0f : -42.0f;
            if (!state.isImpactBurst) {
                float settle = 1.0f - (float) Math.exp(-state.age * 0.22);
                tiltAngle += (state.tiltRight ? 14.0f : -14.0f) * settle;
            }
            double tiltRad = Math.toRadians(tiltAngle);
            Vec3 right = rotateAroundAxis(rightBase, forward, tiltRad);
            Vec3 up = rotateAroundAxis(upBase, forward, tiltRad);

            SlashRig rig = rigFor(level, state.seed, state.age, center, state.isImpactBurst);
            long now = System.nanoTime();
            rig.center = center;
            rig.forward = forward;
            rig.right = right;
            rig.up = up;
            rig.halfSpan = (state.isImpactBurst ? 2.9f : 4.6f) * scale;
            rig.halfHeight = (state.isImpactBurst ? 1.5f : 2.1f) * scale;
            rig.halfDepth = 0.55f * scale;
            rig.lastAge = state.age;
            rig.lastSeenNanos = now;

            float distScale = MaterialEmitters.distanceScale(center);
            float envelope = fade * distScale;

            // The arc's low point against the world, for the grounded
            // crack fan below: only a slash sweeping close over solid
            // ground scores the ground; a slash flying high does not.
            final Vec3 crackFinal = fade > 0.02f
                ? groundCrackPoint(level, center.subtract(up.scale(rig.halfHeight * 0.9)))
                : null;

            // Pieces layer, class M, in entity local space: the synced
            // right, up, and forward axes are plain offsets here, the
            // same basis the emission sampler uses, so the strands hug
            // the swept volume and inherit the reap tilt. The crest
            // strand runs end to end across the span with its control
            // pushed up the arc; the soul strand repeats the arc
            // trailing behind the travel direction. Attack upgrade:
            // a second pair rides with them, a forward echo of the
            // crest pushed toward the travel direction and a deep
            // trailing strand behind the soul strand, each with its
            // own seed so the builder curl phases sit apart. Four
            // tendrils at full spine samples are 1536 vertices worst
            // case, inside the class M budget of 2500 for this submit;
            // the shard burst and the crack fan are separate submits
            // with their own budgets (1440 and about 550 worst case).
            if (fade > 0.02f) {
                final Vec3 rightL = right;
                final Vec3 upL = up;
                final Vec3 fwdL = forward;
                final float spanL = rig.halfSpan;
                final float heightL = rig.halfHeight;
                final float scaleL = scale;
                final float geoEnv = fade;
                final float geoDs = distScale;
                final long pseed = state.seed;
                ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
                    Vec3 from = rightL.scale(-spanL * 0.95).add(upL.scale(-heightL * 0.35));
                    Vec3 control = upL.scale(heightL * 0.95).add(fwdL.scale(0.10));
                    Vec3 to = rightL.scale(spanL * 0.95).add(upL.scale(-heightL * 0.35));
                    ComplexGeometry.tendril(pose, buffer, pseed ^ 0xD11L,
                        from, control, to, 0.50f * scaleL, 0xCC0DD9, 0xE6FAFF, 0.95f, geoEnv, geoDs);
                    Vec3 back = fwdL.scale(-0.40 * scaleL);
                    ComplexGeometry.tendril(pose, buffer, pseed ^ 0xD22L,
                        from.add(back).add(upL.scale(-heightL * 0.12)),
                        control.add(back), to.add(back).add(upL.scale(-heightL * 0.12)),
                        0.28f * scaleL, 0xB84DFF, 0xCC0DD9, 0.90f, geoEnv, geoDs);
                    // Second pair, seeded apart from the first pair so
                    // their curl phases differ: a forward echo of the
                    // crest, and a deep strand trailing behind the soul
                    // strand, so the arc reads as two braided pairs.
                    Vec3 fwdOff = fwdL.scale(0.38 * scaleL);
                    ComplexGeometry.tendril(pose, buffer, pseed ^ 0xD33L,
                        from.add(fwdOff).add(upL.scale(-heightL * 0.05)),
                        control.add(fwdOff).add(upL.scale(heightL * 0.08)),
                        to.add(fwdOff).add(upL.scale(-heightL * 0.05)),
                        0.38f * scaleL, 0x9900E6, 0xB84DFF, 0.92f, geoEnv, geoDs);
                    Vec3 backOff = fwdL.scale(-0.78 * scaleL);
                    ComplexGeometry.tendril(pose, buffer, pseed ^ 0xD44L,
                        from.add(backOff).add(upL.scale(-heightL * 0.20)),
                        control.add(backOff).add(upL.scale(-heightL * 0.05)),
                        to.add(backOff).add(upL.scale(-heightL * 0.20)),
                        0.20f * scaleL, 0x9900E6, 0x3D0A66, 0.88f, geoEnv, geoDs);
                });
                // Sorrow shards: five violet crystal shards fanned in
                // the arc plane around the travel axis, deep violet at
                // the root running to bright violet at the tips.
                // submitSolid per the audit spec: energy crystal, the
                // same family call as the curse glass elsewhere.
                ComplexGeometry.submitSolid(collector, poseStack, (pose, buffer) ->
                    ComplexGeometry.shardBurst(pose, buffer, pseed ^ 0x5A11L,
                        upL.scale(heightL * 0.15), fwdL, 150f, 0.55f * scaleL, 5,
                        0x3D0A66, 0xB84DFF, 0.95f, geoEnv, geoDs));
                // Grounded crack fan at the arc's low point: short
                // violet cracks scored into the ground the slash is
                // sweeping over. The builder draws in its local XZ
                // plane, so the fan rides a copy of the pose moved to
                // the ground point under the low point.
                if (crackFinal != null) {
                    final Vec3 crackOff = crackFinal.subtract(new Vec3(state.x, state.y, state.z));
                    ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) -> {
                        PoseStack fanStack = new PoseStack();
                        fanStack.last().pose().set(pose.pose());
                        fanStack.last().normal().set(pose.normal());
                        fanStack.translate(crackOff.x, crackOff.y, crackOff.z);
                        ComplexGeometry.crackFan(fanStack.last(), buffer, pseed ^ 0xC4ACL,
                            1.15f * scaleL, 0xF5EEFF, 0x9900E6, 0.90f, geoEnv, geoDs);
                    });
                }
            }

            // The impact burst keeps its opening pop, now as material bursts
            // instead of the old crossing crescent.
            if (state.isImpactBurst && !rig.burstDone) {
                rig.burstDone = true;
                MaterialEmitters.Source core = MaterialEmitters.sphereVolume(center, 0.8 * scale);
                MaterialEmitters.burst(level,
                    MaterialEmitters.Preset.plasma(0xE6FAFF, 2.0f, (int) (state.seed ^ 0xB1257L), Vec3.ZERO, 0.10f),
                    core, Math.max(1, Math.round(24.0f * envelope)));
                MaterialEmitters.burst(level,
                    MaterialEmitters.Preset.soul(0xB84DFF, 1.9f, (int) (state.seed ^ 0xB1258L), Vec3.ZERO, 0.08f),
                    core, Math.max(1, Math.round(14.0f * envelope)));
                MaterialEmitters.burst(level,
                    MaterialEmitters.Preset.darkness(0x140026, 2.4f, (int) (state.seed ^ 0xB1259L), Vec3.ZERO, 0.06f),
                    core, Math.max(1, Math.round(9.0f * envelope)));
                MaterialEmitters.burst(level,
                    MaterialEmitters.Preset.fixed(DbaParticles.WISP_CURL, Vec3.ZERO, 0.08f),
                    core, Math.max(1, Math.round(12.0f * envelope)));
            }

            float delta = rig.lastTickNanos == 0L ? 0.0f
                : Math.min(0.05f, (now - rig.lastTickNanos) / 1_000_000_000.0f);
            rig.lastTickNanos = now;
            if (delta > 0.0f && envelope > 0.01f) {
                for (MaterialEmitters.Emitter emitter : rig.emitters) {
                    emitter.tick(delta, envelope);
                }
            }
            pruneRigs(now);
        }
        super.submit(state, poseStack, collector, cameraState);
    }
}
