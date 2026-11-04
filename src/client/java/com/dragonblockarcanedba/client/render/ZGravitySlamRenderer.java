package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.entity.ZGravitySlamEntity;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Entity Renderer for Z-Sword Gravity Slam in Minecraft 26.2.
 *
 * Complex pass: layered composition at class L, two piece builders.
 * The solid pieces layer is a fracturePlates fan at the slam point in
 * stone dust over dark stone, growing with the shock radius; the
 * energy pieces layer is a crackFan web at the same point, pale gold
 * hot at the center running out to violet at the tips, drawn on the
 * shader through submitEnergy. These are the fast near field structure:
 * the entity already spawns a DebrisSpawner field for the big physical
 * chunks. The complex particle layer is a one shot SHARD_CHIP burst
 * plus a SPARK_STREAK burst on the strike envelope. The filler layer
 * is unchanged: compressed stone dust and pale vapor driven outward
 * across the ground, violet and gold gravity plasma rising through
 * the trench area, physical block chunks using the struck ground
 * material, seeded falling columns at the old monolith anchors, and
 * the geyser stage eruption. Lifecycle, radius, power, and seed
 * behavior carry over from before.
 */
public class ZGravitySlamRenderer extends EntityRenderer<ZGravitySlamEntity, ZGravitySlamRenderer.ZGravitySlamRenderState> {

    private static final int GOLD = 0xFFD133;
    private static final int VIOLET = 0x5914A6;
    private static final int PALE_GOLD = 0xFFF2A6;
    private static final int STONE_DUST = 0x9A8668;
    private static final int DARK_STONE = 0x77675A;

    private final Map<Integer, Emission> emissions = new HashMap<>();

    public ZGravitySlamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ZGravitySlamRenderState extends EntityRenderState {
        public float radius = 10.0f;
        public float powerRatio = 1.0f;
        public float age = 0;
        public int lifetime = 50;
        public long seed = 0;
        public int entityId = -1;
        public BlockState groundState = Blocks.STONE.defaultBlockState();
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(ZGravitySlamEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(ZGravitySlamEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ZGravitySlamRenderState createRenderState() {
        return new ZGravitySlamRenderState();
    }

    @Override
    public void extractRenderState(ZGravitySlamEntity entity, ZGravitySlamRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.powerRatio = entity.getPowerRatio();
        state.age = entity.tickCount + partialTicks;
        state.lifetime = entity.getLifetime();
        state.seed = entity.getSeed();
        state.entityId = entity.getId();
        BlockState below = entity.level().getBlockState(entity.blockPosition().below());
        state.groundState = below == null || below.isAir() ? Blocks.STONE.defaultBlockState() : below;
    }

    @Override
    public void submit(ZGravitySlamRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft == null ? null : minecraft.level;
        if (level == null) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        float radius = state.radius;
        float power = state.powerRatio;
        float age = state.age;
        float progress = Math.min(1.0f, age / (float) state.lifetime);
        float fade = 1.0f - smooth(progress);
        float grow = smooth(Math.min(1.0f, progress / 0.48f));
        float impact = strike(progress);
        float decay = smooth(Math.max(0.0f, (progress - 0.60f) / 0.40f));
        Vec3 center = new Vec3(state.x, state.y, state.z);
        float dist = MaterialEmitters.distanceScale(center);
        float shockR = radius * grow;
        if (fade <= 0.04f || shockR <= 0.05f || dist <= 0.01f) {
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        Emission emission = emissionFor(state, level);
        emission.frame.center = center;
        emission.frame.radius = radius;
        emission.frame.grow = grow;
        emission.frame.decay = decay;
        emission.frame.impact = impact;

        float delta = emission.nextDelta();
        float envelope = fade * dist;
        emission.groundDust.tick(delta, envelope);
        emission.vapor.tick(delta, envelope * 0.82f);
        emission.plasma.tick(delta, envelope * (0.55f + 0.45f * impact));
        emission.debris.tick(delta, envelope * (0.40f + 0.60f * impact));
        emission.columnDust.tick(delta, envelope);
        emission.geyserPlasma.tick(delta, impact * envelope);

        if (progress < 0.55f) {
            emission.burstImpact();
        }

        // Pieces layer: solid fracture plates plus the shader crack web.
        // Combined vertex count stays well inside the class L budget.
        long geoSeed = state.seed;
        float plateRadius = shockR;
        float lift = 0.30f + power * 0.50f;
        float solidEnv = fade;
        float crackEnv = fade * (0.55f + 0.45f * impact);
        float distF = dist;
        ComplexGeometry.submitSolid(collector, poseStack, (pose, buffer) ->
            ComplexGeometry.fracturePlates(pose, buffer, geoSeed ^ 0xF9A7L,
                plateRadius, lift, STONE_DUST, DARK_STONE, 0.96f, solidEnv, distF));
        ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) ->
            ComplexGeometry.crackFan(pose, buffer, geoSeed ^ 0xC9ACL,
                plateRadius * 1.05f, PALE_GOLD, VIOLET, 0.95f, crackEnv, distF));

        super.submit(state, poseStack, collector, cameraState);
    }

    private Emission emissionFor(ZGravitySlamRenderState state, ClientLevel level) {
        long now = System.nanoTime();
        emissions.entrySet().removeIf(entry -> now - entry.getValue().lastSeenNanos > 5_000_000_000L);
        Emission emission = emissions.get(state.entityId);
        if (emission == null || emission.seed != state.seed) {
            emission = new Emission(level, state.seed, state.powerRatio, state.groundState);
            emissions.put(state.entityId, emission);
        }
        emission.lastSeenNanos = now;
        return emission;
    }

    private static final class Frame {
        Vec3 center = Vec3.ZERO;
        float radius = 1.0f;
        float grow = 1.0f;
        float decay = 0.0f;
        float impact = 0.0f;
    }

    private static final class Emission {
        final long seed;
        final ClientLevel level;
        boolean impactBurstDone = false;
        final Frame frame = new Frame();
        final float[] columnAngles;
        final float[] columnDistFracs;
        final float[] columnHeights;
        final float[] columnWidths;
        final float[] columnTilts;
        final MaterialEmitters.Emitter groundDust;
        final MaterialEmitters.Emitter vapor;
        final MaterialEmitters.Emitter plasma;
        final MaterialEmitters.Emitter debris;
        final MaterialEmitters.Emitter columnDust;
        final MaterialEmitters.Emitter geyserPlasma;
        long lastSeenNanos = System.nanoTime();
        long lastTickNanos = 0L;

        Emission(ClientLevel level, long seed, float power, BlockState groundState) {
            this.seed = seed;
            this.level = level;
            int baseSeed = (int) seed;
            Frame f = frame;

            int rockCount = 18 + (int) (power * 10);
            columnAngles = new float[rockCount];
            columnDistFracs = new float[rockCount];
            columnHeights = new float[rockCount];
            columnWidths = new float[rockCount];
            columnTilts = new float[rockCount];
            Random rng = new Random(seed);
            for (int r = 0; r < rockCount; r++) {
                columnAngles[r] = (float) ((r / (double) rockCount) * Math.PI * 2.0 + rng.nextDouble() * 0.35);
                columnDistFracs[r] = 0.22f + rng.nextFloat() * 0.70f;
                columnHeights[r] = (1.8f + power * 2.2f) + rng.nextFloat() * (2.0f + power * 1.5f);
                columnWidths[r] = 0.22f + rng.nextFloat() * 0.16f;
                columnTilts[r] = 0.30f + rng.nextFloat() * 0.30f;
            }

            MaterialEmitters.Source ground = random -> {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double flat = Math.sqrt(random.nextDouble()) * f.radius * f.grow;
                Vec3 pos = f.center.add(Math.cos(angle) * flat, 0.07, Math.sin(angle) * flat);
                Vec3 outward = new Vec3(Math.cos(angle), 0.0, Math.sin(angle))
                    .scale(0.22 + random.nextDouble() * 0.22).add(0.0, 0.10 + random.nextDouble() * 0.10, 0.0);
                return new MaterialEmitters.Sample(pos, outward);
            };
            MaterialEmitters.Source columns = random -> {
                int index = random.nextInt(columnAngles.length);
                Vec3 anchor = columnAnchor(f, index);
                double local = (random.nextDouble() * 2.0 - 1.0) * columnWidths[index] * 0.62;
                double localZ = (random.nextDouble() * 2.0 - 1.0) * columnWidths[index] * 0.62;
                double height = columnHeights[index] * f.grow * (1.0f - f.decay * 0.60f);
                Vec3 pos = anchor.add(local, random.nextDouble() * Math.max(0.05, height), localZ);
                Vec3 outward = new Vec3(Math.cos(columnAngles[index]), 0.0, Math.sin(columnAngles[index]))
                    .scale(columnTilts[index] * 0.05);
                Vec3 fall = outward.add((random.nextDouble() - 0.5) * 0.03,
                    -(0.17 + random.nextDouble() * 0.22), (random.nextDouble() - 0.5) * 0.03);
                return new MaterialEmitters.Sample(pos, fall);
            };
            MaterialEmitters.Source debrisSource = random -> {
                int index = random.nextInt(columnAngles.length);
                Vec3 anchor = columnAnchor(f, index);
                double local = (random.nextDouble() * 2.0 - 1.0) * columnWidths[index] * 0.80;
                double localZ = (random.nextDouble() * 2.0 - 1.0) * columnWidths[index] * 0.80;
                double height = columnHeights[index] * f.grow * (1.0f - f.decay * 0.60f);
                Vec3 pos = anchor.add(local, random.nextDouble() * Math.max(0.05, height * 0.30), localZ);
                Vec3 outward = new Vec3(Math.cos(columnAngles[index]), 0.0, Math.sin(columnAngles[index]))
                    .scale(0.16 + random.nextDouble() * 0.24);
                return new MaterialEmitters.Sample(pos, outward.add(0.0, 0.22 + random.nextDouble() * 0.25, 0.0));
            };
            MaterialEmitters.Source geyser = random -> {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double flat = Math.sqrt(random.nextDouble()) * f.radius * f.grow * 0.82;
                Vec3 pos = f.center.add(Math.cos(angle) * flat, 0.08, Math.sin(angle) * flat);
                Vec3 up = new Vec3((random.nextDouble() - 0.5) * 0.08,
                    0.42 + random.nextDouble() * 0.30, (random.nextDouble() - 0.5) * 0.08);
                return new MaterialEmitters.Sample(pos, up);
            };

            groundDust = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(STONE_DUST, 2.35f, baseSeed ^ 0xD057, Vec3.ZERO, 0.012f),
                ground, 168.0f, 340);
            vapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(PALE_GOLD, 3.05f, baseSeed ^ 0x9A90, Vec3.ZERO, 0.010f),
                ground, 82.0f, 175);
            plasma = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(VIOLET, 2.20f, baseSeed ^ 0x7E1A, Vec3.ZERO, 0.012f),
                ground, 104.0f, 220);
            debris = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(new BlockParticleOption(DbaParticles.BLOCK_CHUNKS, groundState),
                    Vec3.ZERO, 0.05f),
                debrisSource, 58.0f, 130);
            columnDust = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(DARK_STONE, 2.00f, baseSeed ^ 0xC010, Vec3.ZERO, 0.008f),
                columns, 148.0f, 315);
            geyserPlasma = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(GOLD, 2.45f, baseSeed ^ 0x6E15, Vec3.ZERO, 0.012f),
                geyser, 138.0f, 285);
        }

        /**
         * One shot complex sprite burst at the strike moment: stone
         * shard chips and steel spark streaks thrown outward and up
         * from inside the growing shock radius. Fires once per
         * emission instance, so once per entity and seed.
         */
        void burstImpact() {
            if (impactBurstDone) return;
            impactBurstDone = true;
            Frame f = frame;
            MaterialEmitters.Source burstSource = random -> {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double flat = Math.sqrt(random.nextDouble()) * f.radius * f.grow * 0.72;
                Vec3 pos = f.center.add(Math.cos(angle) * flat, 0.14, Math.sin(angle) * flat);
                Vec3 outward = new Vec3(Math.cos(angle), 0.0, Math.sin(angle))
                    .scale(0.26 + random.nextDouble() * 0.26);
                return new MaterialEmitters.Sample(pos,
                    outward.add(0.0, 0.30 + random.nextDouble() * 0.30, 0.0));
            };
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP, Vec3.ZERO, 0.14f),
                burstSource, 36);
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.20f),
                burstSource, 20);
        }

        Vec3 columnAnchor(Frame f, int index) {
            float dist = f.radius * f.grow * columnDistFracs[index];
            return new Vec3(f.center.x + Math.cos(columnAngles[index]) * dist,
                f.center.y + 0.05, f.center.z + Math.sin(columnAngles[index]) * dist);
        }

        float nextDelta() {
            long now = System.nanoTime();
            if (lastTickNanos == 0L) {
                lastTickNanos = now;
                return 1.0f / 60.0f;
            }
            float delta = Math.min(0.05f, (now - lastTickNanos) / 1_000_000_000.0f);
            lastTickNanos = now;
            return Math.max(0.001f, delta);
        }
    }

    private static float smooth(float t) {
        float x = Math.max(0.0f, Math.min(1.0f, t));
        return x * x * (3.0f - 2.0f * x);
    }

    private static float strike(float progress) {
        if (progress >= 0.42f) return 0.0f;
        return (float) Math.sin((progress / 0.42f) * Math.PI);
    }
}
