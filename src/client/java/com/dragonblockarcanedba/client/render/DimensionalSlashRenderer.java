package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.DimensionalSlashEntity;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Entity Renderer for Dimensional Slash in Minecraft 26.2.
 * Complex pass: a layered composition. The pieces layer is the cut body:
 * one thick ComplexGeometry tendril strand with torn pinches swept tip
 * to tip across the cut plane and bowed along the old crescent belly,
 * with a thinner white core strand and a reversed rim strand nested in
 * it, all through the shader tendril canvas. Attack upgrade: two more
 * strands ride the bow above and below it on seeded phase offsets so
 * the five never swell in sync, an in-air crack fan tears across the
 * slash plane at the cut's center, and crystal shards burst at the two
 * ends. The complex particle layer
 * is SHARD_CHIP spatial fragments fraying off the two pinched tips. The
 * material emission stays as filler only, sampled across the cut plane
 * the entity is actually travelling on:
 * - Void plasma in the crescent's three theme stops (abyssal violet, neon
 *   blood-magenta, white tear) streaming backward off the cut
 * - A dense white plasma core where the singularity pearl used to sit
 * - Darkness vapor wrapping the cut and spatial glass dust fraying off the
 *   two tips
 * Lifecycle, tilt, spin wobble timing, first-person scale-in, and the
 * lingering tear fade are the same numbers the geometry version used.
 */
public class DimensionalSlashRenderer extends EntityRenderer<DimensionalSlashEntity, DimensionalSlashRenderer.SlashRenderState> {

    public DimensionalSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SlashRenderState extends EntityRenderState {
        public boolean tiltRight = false;
        public float yRot = 0;
        public float xRot = 0;
        public float age = 0;
        public float fadeStart = -1;
        public int seed = 0;
        public boolean isFirstPersonOwner = false;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(DimensionalSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(DimensionalSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public SlashRenderState createRenderState() {
        return new SlashRenderState();
    }

    @Override
    public void extractRenderState(DimensionalSlashEntity entity, SlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.tiltRight = entity.getTilt();
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.fadeStart = entity.getFadeStart();
        state.seed = entity.getSeed();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.isFirstPersonOwner = (mc.player != null &&
            (entity.getCasterId() == mc.player.getId() || entity.getOwner() == mc.player) &&
            mc.options.getCameraType().isFirstPerson());
    }

    /**
     * Emission points across the slash plane: u runs along the cut, v across
     * its thickness, with the belly bow the old crescent had. The sample
     * velocity streams backward along the flight path and slightly outward
     * at the tips, so the plasma sheds off the cut instead of hovering.
     */
    private static MaterialEmitters.Source cutPlane(Vec3 center, Vec3 right, Vec3 up, Vec3 forward,
                                                    float halfWidth, float thickness, float belly, float backSpeed) {
        return random -> {
            double u = (random.nextDouble() * 2.0 - 1.0) * halfWidth;
            double bow = (1.0 - (u * u) / (halfWidth * halfWidth)) * belly;
            double v = (random.nextDouble() * 2.0 - 1.0) * thickness + bow;
            Vec3 pos = center.add(right.scale(u)).add(up.scale(v));
            Vec3 vel = forward.scale(-backSpeed).add(right.scale(u * 0.035));
            return new MaterialEmitters.Sample(pos, vel);
        };
    }

    @Override
    public void submit(SlashRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        // Lifecycle, reconciled with the entity: one shared envelope over
        // the real flight life, so the old fade out window (ages 70 to 100)
        // is exactly the envelope's last 30 percent. On a hit the entity
        // lingers instead of discarding, and the tear fades shut across the
        // linger while swelling slightly, like cut space straining open
        // before it closes. Same numbers as the geometry version.
        float distScale = FxEnvelope.distScale(Math.sqrt(Math.max(0.0, state.distanceToCameraSq)));
        float fade = FxEnvelope.envelope(state.age, DimensionalSlashEntity.MAX_LIFE_TICKS, 0.05f, 0.30f);
        float lingerT = -1.0f;
        if (state.fadeStart >= 0) {
            lingerT = Math.max(0.0f, Math.min(1.0f,
                (state.age - state.fadeStart) / (float) DimensionalSlashEntity.LINGER_TICKS));
            fade *= 1.0f - FxEnvelope.smoothstep(lingerT);
        }
        float tearScale = lingerT >= 0.0f ? 1.0f + 0.22f * FxEnvelope.smoothstep(lingerT) : 1.0f;
        float fpScale = (state.isFirstPersonOwner && state.age < 5.0f) ? (0.60f + (state.age / 5.0f) * 0.40f) : 1.0f;
        float envelope = fade * distScale;
        if (envelope <= 0.01f) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        Vec3 center = new Vec3(state.x, state.y, state.z);
        float yaw = (float) Math.toRadians(state.yRot);
        float pitch = (float) Math.toRadians(state.xRot);
        Vec3 forward = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Vec3 rightRaw = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        Vec3 upRaw = rightRaw.cross(forward).normalize();
        // The old blade tilt (plus or minus 35 degrees) applied to the
        // emission plane, plus the forward spin wobble (sin of age x 0.4,
        // 6 degrees) the geometry version posed with.
        double tilt = Math.toRadians((state.tiltRight ? 35.0 : -35.0) + Math.sin(state.age * 0.4f) * 6.0);
        double cosT = Math.cos(tilt), sinT = Math.sin(tilt);
        Vec3 right = rightRaw.scale(cosT).add(upRaw.scale(sinT));
        Vec3 up = upRaw.scale(cosT).add(rightRaw.scale(-sinT));

        float pulse = 0.90f + 0.10f * (float) Math.sin(state.age * 0.6f);
        float scaleAll = fpScale * tearScale;
        float halfWidth = 2.20f * scaleAll;
        int seed = state.seed;
        // Seeded phase offsets for the two upgrade strands: same swell
        // rate as the body pulse, phases rolled off the slash seed, so
        // the five strands breathe out of step with each other.
        float hiPulse = 0.90f + 0.10f * (float) Math.sin(state.age * 0.6f + 1.7f + (seed & 7) * 0.31f);
        float loPulse = 0.90f + 0.10f * (float) Math.sin(state.age * 0.6f + 3.9f + (seed & 7) * 0.53f);
        // During the linger the plasma dies down while the glass dust and
        // darkness of the closing tear take over.
        float plasmaEnv = envelope * (lingerT >= 0.0f ? 1.0f - 0.55f * lingerT : 1.0f);
        float residueEnv = envelope * (lingerT >= 0.0f ? 0.65f + 0.35f * (1.0f - lingerT) : 1.0f);
        float delta = 1.0f / 60.0f;

        // The cut body: three plasma stops sampled across the cut plane.
        new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0xA60DF2, 1.50f, seed ^ 0xA53A, Vec3.ZERO, 0.05f),
            cutPlane(center, right, up, forward, halfWidth, 0.30f * scaleAll, 0.55f * scaleAll, 0.30f),
            300f, 320).tick(delta, plasmaEnv);
        new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0xFF1A59, 1.25f, seed ^ 0x7E44, Vec3.ZERO, 0.04f),
            cutPlane(center, right, up, forward, halfWidth * 0.82f, 0.20f * scaleAll, 0.45f * scaleAll, 0.34f),
            240f, 260).tick(delta, plasmaEnv);
        new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0xFFFFFF, 0.95f, seed ^ 0x4A10, Vec3.ZERO, 0.03f),
            cutPlane(center, right, up, forward, halfWidth * 0.50f, 0.10f * scaleAll, 0.30f * scaleAll, 0.38f),
            170f, 190).tick(delta, plasmaEnv);

        // The singularity core: a dense white and magenta plasma cluster
        // where the pearl sat, forward of the cut's belly.
        Vec3 core = center.add(up.scale(0.38f * scaleAll)).add(forward.scale(0.30f));
        new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0xFFFFFF, 0.80f, seed ^ 0xC04E, Vec3.ZERO, 0.02f),
            MaterialEmitters.sphereVolume(core, 0.30f * pulse * scaleAll),
            130f, 150).tick(delta, plasmaEnv);
        new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.plasma(0xFF26D9, 1.05f, seed ^ 0x51E6, Vec3.ZERO, 0.03f),
            MaterialEmitters.sphereVolume(core, 0.48f * pulse * scaleAll),
            120f, 140).tick(delta, plasmaEnv);

        // Darkness vapor wrapping the whole cut, drifting backward.
        new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.darkness(0x0D0018, 1.50f, seed ^ 0xB01D, forward.scale(-0.10), 0.03f),
            MaterialEmitters.sphereVolume(center, 1.15f * scaleAll),
            110f, 130).tick(delta, residueEnv);
        new MaterialEmitters.Emitter(level,
            MaterialEmitters.Preset.vapor(0x5E0B99, 1.80f, seed ^ 0xD15C, forward.scale(-0.06), 0.03f),
            MaterialEmitters.sphereVolume(center, 1.45f * scaleAll),
            80f, 100).tick(delta, residueEnv);

        // Spatial glass dust fraying off the two pinched tips, with the
        // complex particle layer riding the same tips: SHARD_CHIP spatial
        // fragments shed at the rim, inside the 40 per second continuous
        // budget per emitter.
        for (int s = -1; s <= 1; s += 2) {
            Vec3 tip = center.add(right.scale(s * halfWidth * 0.92f)).add(up.scale(0.05f));
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.glassDust(0xE8CCFF, 1.00f, seed ^ (s < 0 ? 0x5911 : 0x5912),
                    forward.scale(-0.22).add(right.scale(s * 0.10)), 0.05f),
                MaterialEmitters.sphereVolume(tip, 0.30f * scaleAll),
                80f, 95).tick(delta, residueEnv);
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP,
                    forward.scale(-0.22).add(right.scale(s * 0.10)), 0.05f),
                MaterialEmitters.sphereVolume(tip, 0.30f * scaleAll),
                28f, 40).tick(delta, residueEnv);
        }

        // Pieces layer, class L: five tendrils in the cut plane, drawn
        // in entity local space along the same right, up, and forward
        // basis the emission samples. The bezier control sits twice as
        // far along up as the belly the cut plane samples, so the
        // strands crest on the same bowed line. The attack upgrade's
        // two extra strands crest above and sag below the bow on
        // seeded phase offsets, an in-air crack fan tears across the
        // slash plane at the center, and crystal shards burst at the
        // ends. Vertices: five tendrils stay near 1900, the crack fan
        // a few hundred, the four end shards about 1150, inside the
        // class L budget of 6000.
        float pieceEnv = fade * (lingerT >= 0.0f ? 1.0f - 0.55f * lingerT : 1.0f);
        if (pieceEnv > 0.02f) {
            ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
                Vec3 from = right.scale(-halfWidth).add(up.scale(0.05f * scaleAll));
                Vec3 to = right.scale(halfWidth).add(up.scale(0.05f * scaleAll));
                Vec3 control = up.scale(1.05f * scaleAll).add(forward.scale(0.10f * scaleAll));
                ComplexGeometry.tendril(pose, buffer, seed ^ 0xD51A,
                    from, control, to,
                    0.85f * scaleAll * pulse, 0xA60DF2, 0xFF1A59, 0.95f, pieceEnv, distScale);
                Vec3 coreFrom = right.scale(-halfWidth * 0.80f).add(up.scale(0.10f * scaleAll));
                Vec3 coreTo = right.scale(halfWidth * 0.80f).add(up.scale(0.10f * scaleAll));
                Vec3 coreControl = up.scale(0.88f * scaleAll).add(forward.scale(0.22f * scaleAll));
                ComplexGeometry.tendril(pose, buffer, seed ^ 0xD52B,
                    coreFrom, coreControl, coreTo,
                    0.32f * scaleAll * pulse, 0xFFFFFF, 0xFF26D9, 0.95f, pieceEnv, distScale);
                Vec3 rimFrom = right.scale(-halfWidth * 0.97f).add(up.scale(0.02f * scaleAll));
                Vec3 rimTo = right.scale(halfWidth * 0.97f).add(up.scale(0.02f * scaleAll));
                Vec3 rimControl = up.scale(1.12f * scaleAll).add(forward.scale(-0.14f * scaleAll));
                ComplexGeometry.tendril(pose, buffer, seed ^ 0xD53C,
                    rimFrom, rimControl, rimTo,
                    0.28f * scaleAll, 0xFF1A59, 0xA60DF2, 0.90f, pieceEnv, distScale);
                Vec3 hiFrom = right.scale(-halfWidth * 0.88f).add(up.scale(0.16f * scaleAll));
                Vec3 hiTo = right.scale(halfWidth * 0.88f).add(up.scale(0.16f * scaleAll));
                Vec3 hiControl = up.scale(1.34f * scaleAll).add(forward.scale(0.04f * scaleAll));
                ComplexGeometry.tendril(pose, buffer, seed ^ 0xD54D,
                    hiFrom, hiControl, hiTo,
                    0.20f * scaleAll * hiPulse, 0xFF1A59, 0xA60DF2, 0.88f, pieceEnv, distScale);
                Vec3 loFrom = right.scale(-halfWidth * 0.93f).add(up.scale(-0.04f * scaleAll));
                Vec3 loTo = right.scale(halfWidth * 0.93f).add(up.scale(-0.04f * scaleAll));
                Vec3 loControl = up.scale(0.62f * scaleAll).add(forward.scale(-0.20f * scaleAll));
                ComplexGeometry.tendril(pose, buffer, seed ^ 0xD55E,
                    loFrom, loControl, loTo,
                    0.22f * scaleAll * loPulse, 0xA60DF2, 0xFFFFFF, 0.85f, pieceEnv, distScale);
            });

            // The in-air dimensional crack: a crack fan laid into the
            // slash plane at the cut's center. The pose basis maps the
            // fan's ground plane onto the cut plane (local X along the
            // cut, local Z across it, local Y out along the flight
            // direction), so the cracks tear across the cut itself
            // instead of lying on the ground under it.
            poseStack.pushPose();
            Matrix4f slashBasis = new Matrix4f();
            slashBasis.setColumn(0, new Vector4f((float) right.x, (float) right.y, (float) right.z, 0.0f));
            slashBasis.setColumn(1, new Vector4f((float) forward.x, (float) forward.y, (float) forward.z, 0.0f));
            slashBasis.setColumn(2, new Vector4f((float) up.x, (float) up.y, (float) up.z, 0.0f));
            slashBasis.setColumn(3, new Vector4f(0.0f, 0.0f, 0.0f, 1.0f));
            poseStack.mulPose(slashBasis);
            final float fanRadius = halfWidth * 0.80f;
            final float fanDetail = Math.min(distScale, 0.62f);
            ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) ->
                ComplexGeometry.crackFan(pose, buffer, seed ^ 0xC4A7L, fanRadius,
                    0xFFFFFF, 0xA60DF2, 0.90f, pieceEnv, fanDetail));
            poseStack.popPose();

            // Crystal shards at the cut's ends: two per end, bursting
            // outward along the cut axis. Warp glass is an energy
            // material, so the solid emissive path stays correct here.
            ComplexGeometry.submitSolid(collector, poseStack, (pose, buffer) -> {
                for (int s = -1; s <= 1; s += 2) {
                    Vec3 cutEnd = right.scale(s * halfWidth * 0.95f).add(up.scale(0.05f * scaleAll));
                    ComplexGeometry.shardBurst(pose, buffer, seed ^ (s < 0 ? 0x5A31L : 0x5A32L), cutEnd,
                        right.scale(s), 55f, 0.50f * scaleAll, 2,
                        0x2A0A4A, 0xE8CCFF, 0.95f, pieceEnv, distScale);
                }
            });
        }

        super.submit(state, poseStack, collector, cameraState);
    }
}
