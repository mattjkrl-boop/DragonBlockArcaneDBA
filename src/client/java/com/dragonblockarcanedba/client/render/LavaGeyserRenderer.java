package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.LavaGeyserEntity;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The geyser pillar is a layered composition at class M. The pieces
 * layer is a crown of flame tongues at the top of the column, rising
 * and falling with the same height curve the emission samples, drawn
 * on the additive shader with the flame canvas. The complex particle
 * layer is EMBER_FRAGMENT rising through the column. The material
 * emission from the no-shapes pass stays as filler only: flame and
 * embers rising through the column, ash at its base, and smoke and
 * heat vapor leaving from the upper column as the eruption burns down.
 */
public class LavaGeyserRenderer extends EntityRenderer<LavaGeyserEntity, LavaGeyserRenderer.GeyserState> {
    private final Map<Integer, Emission> emissions = new HashMap<>();

    public LavaGeyserRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class GeyserState extends EntityRenderState {
        public int entityId;
        public float age;
        public int seed;
        public double posX;
        public double posY;
        public double posZ;
    }

    public boolean shouldRender(LavaGeyserEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(LavaGeyserEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public GeyserState createRenderState() {
        return new GeyserState();
    }

    @Override
    public void extractRenderState(LavaGeyserEntity entity, GeyserState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.entityId = entity.getId();
        state.age = entity.tickCount + partialTick;
        state.seed = entity.getSeed();
        state.posX = entity.getX();
        state.posY = entity.getY();
        state.posZ = entity.getZ();
    }

    @Override
    public void submit(GeyserState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && state.entityId != 0) {
            Vec3 center = new Vec3(state.posX, state.posY, state.posZ);
            float distance = MaterialEmitters.distanceScale(center);
            float env = FxEnvelope.envelope(state.age, LavaGeyserEntity.LIFE_TICKS);
            float height = 4.4f * Math.min(1.0f, state.age / 2.5f);
            Emission emission = emissions.get(state.entityId);
            if (emission == null || emission.level != level || emission.seed != state.seed
                    || state.age + 0.001f < emission.lastAge) {
                emission = new Emission(level, state);
                emissions.put(state.entityId, emission);
            }
            emission.update(state, env * distance, height);

            // Pieces layer, in local space (pose origin is the geyser
            // base). The crown base rides at the current column height,
            // so the tongues grow with the pillar. Seven tongues at six
            // segments are about 336 vertices, inside the class M
            // budget of 2500.
            final long pieceSeed = state.seed;
            final float pieceEnv = env;
            final float pieceDs = distance;
            final float sway = state.age * 0.60f;
            final float crownY = height;
            if (pieceEnv > 0.01f) {
                ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.FLAME_TONGUE, (pose, buffer) ->
                        ComplexGeometry.flameTongues(pose, buffer, pieceSeed ^ 0xF1A1L,
                                new Vec3(0.0, crownY, 0.0), 0.85f, 1.15f, 7,
                                0xFFF2B0, 0xFF5A00, 0.95f, pieceEnv, sway, pieceDs));
            }
        }
        super.submit(state, poseStack, collector, cameraState);
    }

    private static final class Emission {
        private final ClientLevel level;
        private final int seed;
        private final List<MaterialEmitters.Emitter> emitters = new ArrayList<>();
        private Vec3 center;
        private double height = 0.25;
        private float lastAge;

        private Emission(ClientLevel level, GeyserState state) {
            this.level = level;
            this.seed = state.seed;
            this.center = position(state);
            this.lastAge = state.age;

            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.flame(Vec3.ZERO, 0.020f),
                    random -> sampleColumn(random, 0.0, 1.0, 1.05), 820f, 980));
            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.ember(Vec3.ZERO, 0.030f),
                    random -> sampleColumn(random, 0.0, 1.0, 1.25), 340f, 430));
            // Complex particle layer: irregular ember fragments rising
            // through the same column as the round ember filler, at the
            // standard continuous cap for complex sprites.
            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.fixed(DbaParticles.EMBER_FRAGMENT, Vec3.ZERO, 0.045f),
                    random -> sampleColumn(random, 0.0, 1.0, 1.25), 34f, 40));
            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.ash(new Vec3(0.0, 0.010, 0.0), 0.026f),
                    this::sampleBase, 105f, 140));
            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.smoke(0x2B130B, 1.00f, seed ^ 0x6A11,
                            new Vec3(0.0, 0.028, 0.0), 0.024f),
                    random -> sampleColumn(random, 0.42, 1.0, 1.30), 145f, 200));
            emitters.add(new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.vapor(0x9E4A18, 0.92f, seed ^ 0xBEEF,
                            new Vec3(0.0, 0.014, 0.0), 0.020f),
                    random -> sampleColumn(random, 0.0, 1.0, 1.20), 85f, 120));
        }

        private void update(GeyserState state, float strength, double newHeight) {
            float delta = Math.max(0.0f, Math.min(0.05f, (state.age - lastAge) / 20.0f));
            lastAge = state.age;
            center = position(state);
            height = Math.max(0.25, newHeight);
            for (MaterialEmitters.Emitter emitter : emitters) {
                emitter.tick(delta, strength);
            }
        }

        private MaterialEmitters.Sample sampleColumn(RandomSource random, double minFrac, double maxFrac, double radius) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double rr = radius * Math.sqrt(random.nextDouble());
            double yFrac = minFrac + random.nextDouble() * Math.max(0.01, maxFrac - minFrac);
            Vec3 pos = new Vec3(center.x + Math.cos(angle) * rr,
                    center.y + height * yFrac,
                    center.z + Math.sin(angle) * rr);
            Vec3 velocity = new Vec3(0.0, 0.050 + random.nextDouble() * 0.045, 0.0);
            return new MaterialEmitters.Sample(pos, velocity);
        }

        private MaterialEmitters.Sample sampleBase(RandomSource random) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double rr = 2.3 * Math.sqrt(random.nextDouble());
            Vec3 pos = new Vec3(center.x + Math.cos(angle) * rr,
                    center.y + 0.04,
                    center.z + Math.sin(angle) * rr);
            Vec3 velocity = new Vec3(Math.cos(angle) * 0.018, 0.006, Math.sin(angle) * 0.018);
            return new MaterialEmitters.Sample(pos, velocity);
        }

        private static Vec3 position(GeyserState state) {
            return new Vec3(state.posX, state.posY, state.posZ);
        }
    }
}
