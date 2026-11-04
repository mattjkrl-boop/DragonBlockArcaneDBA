package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.DarknessDomainEntity;
import com.dragonblockarcanedba.particle.DbaMaterialOptions;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

/**
 * Entity Renderer for Darkness Domain in Minecraft 26.2.
 *
 * No-shapes pass: the dome shells, ground bands, orbiting spires, vortex
 * tubes, rim bands, and anchor glow are gone. The domain is now a volume
 * of darkness motes and soul vapor under the old dome profile, a sparse
 * vapor film sampled on that boundary, ground swirl material, and a
 * central column of hellish plasma. The synced radius and lifecycle fade
 * still drive the whole presentation. Lightning remains with the real
 * BoltEntity strikes fired by the domain entity.
 *
 * Complex pass (class L): six rim tendrils climb the
 * dome boundary as alpha veil strands, an energy crack fan webs the
 * domain base through the shader path, hot pink at the center cooling
 * to hellish purple at the tips, and a ring of ten large dark stone
 * spikes rises at the rim on the channel envelope, holds with the
 * domain, then retracts with a shard chip burst as it tears down. Ash flakes fall inside the volume. The
 * material emission stays as filler behind and between the pieces.
 */
public class DarknessDomainRenderer extends EntityRenderer<DarknessDomainEntity, DarknessDomainRenderer.DarknessDomainRenderState> {

    public DarknessDomainRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class DarknessDomainRenderState extends EntityRenderState {
        public float radius = 8.0f;
        public float age = 0;
        public float fade = 0;
        public double px = 0, py = 0, pz = 0;
        public long seed = 0;
        public MaterialEmitters.Emitter[] emitters;
        public long emitterSeed = Long.MIN_VALUE;
        public ClientLevel emitterLevel;
        public float lastEmissionAge = Float.NaN;
        public boolean spikeChipBurstDone = false;
        public long spikeChipSeed = Long.MIN_VALUE;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(DarknessDomainEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(DarknessDomainEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public DarknessDomainRenderState createRenderState() {
        return new DarknessDomainRenderState();
    }

    @Override
    public void extractRenderState(DarknessDomainEntity entity, DarknessDomainRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.age = entity.tickCount + partialTicks;
        state.fade = entity.getFade();
        state.px = entity.getX();
        state.py = entity.getY();
        state.pz = entity.getZ();
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(DarknessDomainRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        submitPieces(state, poseStack, collector);
        emitMaterials(state);
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void submitPieces(DarknessDomainRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        float fade = Math.max(0.0f, Math.min(1.0f, state.fade));
        if (fade <= 0.01f) {
            return;
        }
        float distScale = MaterialEmitters.distanceScale(new Vec3(state.px, state.py, state.pz));
        float envelope = fade;
        float radius = Math.max(1.0f, state.radius);
        float height = domeHeight(state);
        long seed = state.seed;
        // Rim tendrils through the veil path: strands rooted at the base
        // rim, climbing and curling inward as they rise along the old
        // dome profile, dark at the root and purple at the tip.
        ComplexGeometry.submitVeil(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
            for (int i = 0; i < 6; i++) {
                double angle = i * Math.PI * 2.0 / 6.0 + (seed & 63L) / 63.0 * 0.5;
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                Vec3 from = new Vec3(cos * radius * 0.98, 0.15, sin * radius * 0.98);
                Vec3 control = new Vec3(cos * radius * 1.03 - sin * radius * 0.10, height * 0.34,
                    sin * radius * 1.03 + cos * radius * 0.10);
                Vec3 to = new Vec3(cos * radius * 0.86, height * 0.62, sin * radius * 0.86);
                ComplexGeometry.tendril(pose, buffer, seed ^ (0xD500L + i * 131L),
                    from, control, to, 0.55f, 0x1E0033, 0x8C0DF2, 0.90f, envelope, distScale);
            }
        });
        // Crack fan at the domain base: the second builder type, drawn
        // as shader energy so the web glows in the ground at full radius.
        ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) ->
            ComplexGeometry.crackFan(pose, buffer, seed ^ 0xD100L,
                radius, 0xF21459, 0x8C0DF2, 0.95f, envelope, distScale));
        // Eclipse spike rim: ten large physical basalt spikes stand
        // at the rim as deliberate stones, not a uniform bar. The
        // builder's seeded length classes and per-spike rise stagger
        // vary their heights and arrival order. The synced fade is the
        // channel envelope, so it drives the rise during build and
        // the hold at full fade. Once age is past the build and fade
        // falls, the same value becomes the retract sink. The bodies
        // are world lit stone; the violet layer is a slightly larger
        // low alpha veil shell, present only as seams at the edges.
        final float spikeRise = fade;
        final float spikeSink = state.age > DarknessDomainEntity.BUILD_TICKS
            ? Math.max(0.0f, 1.0f - fade) : 0.0f;
        final float spikeRadius = radius * 0.98f;
        ComplexGeometry.submitSolidLit(collector, poseStack, state.lightCoords, (pose, buffer) ->
            ComplexGeometry.spikeEruption(pose, buffer, seed ^ 0xD512L,
                Vec3.ZERO, new Vec3(1.0, 0.0, 0.0), ComplexGeometry.SpikeLayout.RING,
                10, spikeRadius, 1.12f, spikeRise, spikeSink,
                0xFF050408, 0xFF332A3A, 0.96f, envelope, distScale));
        ComplexGeometry.submitVeil(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) ->
            ComplexGeometry.spikeEruption(pose, buffer, seed ^ 0xD512L,
                Vec3.ZERO, new Vec3(1.0, 0.0, 0.0), ComplexGeometry.SpikeLayout.RING,
                10, spikeRadius, 1.18f, spikeRise, spikeSink,
                0xFF5B21B6, 0xFFC084FC, 0.22f, envelope, distScale));
        if (spikeSink > 0.0f) {
            burstSpikeChips(state);
        }
    }

    private static void burstSpikeChips(DarknessDomainRenderState state) {
        if (state.spikeChipBurstDone && state.spikeChipSeed == state.seed) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        state.spikeChipBurstDone = true;
        state.spikeChipSeed = state.seed;
        MaterialEmitters.burst(level,
            MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP,
                new Vec3(0.0, -0.03, 0.0), 0.10f),
            random -> {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double radial = Math.max(1.0f, state.radius) * (0.94 + random.nextDouble() * 0.08);
                Vec3 pos = new Vec3(state.px + Math.cos(angle) * radial,
                    state.py + 0.12 + random.nextDouble() * 0.50,
                    state.pz + Math.sin(angle) * radial);
                Vec3 velocity = new Vec3(-Math.cos(angle) * 0.045, -0.020,
                    -Math.sin(angle) * 0.045);
                return new MaterialEmitters.Sample(pos, velocity);
            }, 36);
    }

    private static void emitMaterials(DarknessDomainRenderState state) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        float fade = Math.max(0.0f, Math.min(1.0f, state.fade));
        if (fade <= 0.01f) {
            state.lastEmissionAge = state.age;
            return;
        }

        float distance = MaterialEmitters.distanceScale(new Vec3(state.px, state.py, state.pz));
        float envelope = fade * (0.35f + 0.65f * (float) Math.sqrt(Math.max(0.0f, distance)));
        if (state.emitters == null || state.emitterSeed != state.seed || state.emitterLevel != level
                || Float.isNaN(state.lastEmissionAge) || state.age < state.lastEmissionAge) {
            state.emitters = createEmitters(state, level);
            state.emitterSeed = state.seed;
            state.emitterLevel = level;
            for (MaterialEmitters.Emitter emitter : state.emitters) {
                emitter.tick(0.05f, envelope);
            }
            state.lastEmissionAge = state.age;
            return;
        }

        float delta = Math.min(0.05f, Math.max(0.0f, (state.age - state.lastEmissionAge) / 20.0f));
        state.lastEmissionAge = state.age;
        if (delta > 0.0f) {
            for (MaterialEmitters.Emitter emitter : state.emitters) {
                emitter.tick(delta, envelope);
            }
        }
    }

    private static MaterialEmitters.Emitter[] createEmitters(DarknessDomainRenderState state, ClientLevel level) {
        int seed = (int) state.seed;
        float radius = Math.max(1.0f, state.radius);
        float interiorRate = Math.min(330.0f, 205.0f + radius * 4.0f);
        float groundRate = Math.min(190.0f, 115.0f + radius * 2.5f);
        return new MaterialEmitters.Emitter[] {
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.DARKNESS, 0x12001F, 4.15f,
                    seed ^ 0xD001, Vec3.ZERO, 0.006f),
                domeVolumeSource(state, 0.026, 0.006), interiorRate, Math.round(interiorRate) + 30),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.SOUL, 0xC4B5FD, 1.95f,
                    seed ^ 0xD002, Vec3.ZERO, 0.010f),
                domeVolumeSource(state, 0.018, 0.013), 115.0f, 130),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.DARKNESS, 0x1E0033, 3.60f,
                    seed ^ 0xD003, Vec3.ZERO, 0.006f),
                domeSurfaceSource(state, 0.016), 95.0f, 110),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.VAPOR, 0x4C1D95, 5.25f,
                    seed ^ 0xD004, Vec3.ZERO, 0.004f),
                domeSurfaceSource(state, 0.010), 62.0f, 72),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.DARKNESS, 0x2A0045, 3.05f,
                    seed ^ 0xD005, Vec3.ZERO, 0.008f),
                groundSwirlSource(state, 0.052), groundRate, Math.round(groundRate) + 25),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.PLASMA, 0x8C0DF2, 1.60f,
                    seed ^ 0xD006, Vec3.ZERO, 0.010f),
                groundSwirlSource(state, 0.040), 58.0f, 68),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.PLASMA, 0xA605F2, 2.20f,
                    seed ^ 0xD007, Vec3.ZERO, 0.008f),
                columnSource(state, 0.16, 0.125), 145.0f, 160),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.material(DbaMaterialOptions.PLASMA, 0xF21459, 1.50f,
                    seed ^ 0xD008, Vec3.ZERO, 0.006f),
                columnSource(state, 0.09, 0.165), 92.0f, 105),
            new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.ASH_FLAKE, new Vec3(0.0, -0.05, 0.0), 0.015f),
                domeVolumeSource(state, 0.012, 0.0), 32.0f, 38)
        };
    }

    private static MaterialEmitters.Source domeVolumeSource(DarknessDomainRenderState state,
                                                            double tangentSpeed, double upwardSpeed) {
        return random -> {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radial = Math.sqrt(random.nextDouble()) * state.radius * 0.97;
            double radialFraction = state.radius <= 0.0f ? 0.0 : radial / state.radius;
            double maxY = domeHeight(state) * Math.sqrt(Math.max(0.0, 1.0 - radialFraction * radialFraction));
            double y = random.nextDouble() * Math.max(0.15, maxY);
            Vec3 pos = new Vec3(state.px + Math.cos(angle) * radial,
                state.py + y, state.pz + Math.sin(angle) * radial);
            Vec3 velocity = new Vec3(-Math.sin(angle) * tangentSpeed, upwardSpeed,
                Math.cos(angle) * tangentSpeed);
            return new MaterialEmitters.Sample(pos, velocity);
        };
    }

    private static MaterialEmitters.Source domeSurfaceSource(DarknessDomainRenderState state,
                                                             double tangentSpeed) {
        return random -> {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double u = random.nextDouble();
            double radial = state.radius * Math.sqrt(Math.max(0.0, 1.0 - u * u))
                * (0.965 + random.nextDouble() * 0.045);
            Vec3 pos = new Vec3(state.px + Math.cos(angle) * radial,
                state.py + domeHeight(state) * u, state.pz + Math.sin(angle) * radial);
            Vec3 velocity = new Vec3(-Math.sin(angle) * tangentSpeed, 0.004,
                Math.cos(angle) * tangentSpeed);
            return new MaterialEmitters.Sample(pos, velocity);
        };
    }

    private static MaterialEmitters.Source groundSwirlSource(DarknessDomainRenderState state,
                                                             double tangentSpeed) {
        return random -> {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radial = Math.sqrt(random.nextDouble()) * state.radius * 0.96;
            Vec3 pos = new Vec3(state.px + Math.cos(angle) * radial,
                state.py + 0.10 + random.nextDouble() * 0.22, state.pz + Math.sin(angle) * radial);
            Vec3 velocity = new Vec3(-Math.sin(angle) * tangentSpeed, 0.010,
                Math.cos(angle) * tangentSpeed);
            return new MaterialEmitters.Sample(pos, velocity);
        };
    }

    private static MaterialEmitters.Source columnSource(DarknessDomainRenderState state,
                                                        double radiusFactor, double upwardSpeed) {
        return random -> {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radial = Math.sqrt(random.nextDouble()) * Math.max(1.8, state.radius * radiusFactor);
            Vec3 pos = new Vec3(state.px + Math.cos(angle) * radial,
                state.py + random.nextDouble() * domeHeight(state), state.pz + Math.sin(angle) * radial);
            Vec3 velocity = new Vec3(-Math.sin(angle) * 0.045, upwardSpeed,
                Math.cos(angle) * 0.045);
            return new MaterialEmitters.Sample(pos, velocity);
        };
    }

    private static float domeHeight(DarknessDomainRenderState state) {
        return Math.min(26.0f, Math.max(1.0f, state.radius) * 1.35f);
    }
}
