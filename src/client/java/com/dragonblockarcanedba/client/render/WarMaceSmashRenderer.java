package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.WarMaceSmashEntity;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

/**
 * The smash, converted for the no-shapes realism pass. This renderer draws
 * no geometry at all. What was a filled bronze dome shell is now material:
 * stone dust and shock vapor sampled through the squashed volume the shell
 * occupied, hot bronze plasma motes in the core volume, three swirling
 * dust layers at the rim where the counter rotating bands used to turn,
 * dark dust and sparse plasma running along the seeded fracture lines on
 * the ground (the entity still scars the real fracture disk into the
 * surface), physical debris chunks of the actual ground block, steel
 * sparks, and an early impact plume of pale dust and embers.
 *
 * Identity is preserved: the bronze theme colors, the bow height profile,
 * the radius growth from the entity, the synced seed, and the shared
 * FxEnvelope lifecycle (build 15 percent, dissipate 35 percent) all carry
 * over from the bespoke build. Only what the effect is made of changed.
 */
public class WarMaceSmashRenderer extends EntityRenderer<WarMaceSmashEntity, WarMaceSmashRenderer.SmashState> {

    /** Bronze theme, matching the entity dust color and the old shell. */
    private static final int BRONZE = 0xD99E47;
    /** Bright splash bronze of the old ground glow fan. */
    private static final int SPLASH = 0xFFC761;
    /** Shell bronze, heavy variant. */
    private static final int SHELL_HEAVY = 0xDB8F33;
    /** Shell bronze, normal variant. */
    private static final int SHELL_NORMAL = 0xD9A352;
    /** Hot core bronze of the old inner dome. */
    private static final int HOT_CORE = 0xFFE69E;
    /** Pale plume bronze. */
    private static final int PALE = 0xEBC280;
    /** Dark fracture bronze of the old crack star. */
    private static final int CRACK_DARK = 0x291A0D;

    /** Live material emitters, one bundle per rendered smash entity id. */
    private final Map<Integer, SmashEmission> emissions = new HashMap<>();

    public WarMaceSmashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SmashState extends EntityRenderState {
        public int entityId;
        public float age;
        public boolean heavy;
        public float radius;
        public int seed;
        public double ex;
        public double ey;
        public double ez;
    }

    public boolean shouldRender(WarMaceSmashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(WarMaceSmashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public SmashState createRenderState() {
        return new SmashState();
    }

    @Override
    public void extractRenderState(WarMaceSmashEntity entity, SmashState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.entityId = entity.getId();
        state.age = entity.tickCount + partialTick;
        state.heavy = entity.isHeavy();
        state.radius = entity.radiusAt(state.age);
        state.seed = entity.getSeed();
        state.ex = entity.getX();
        state.ey = entity.getY();
        state.ez = entity.getZ();
    }

    @Override
    public void submit(SmashState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float life = state.heavy ? 14.0f : 10.0f;
        float env = FxEnvelope.envelope(state.age, life, 0.15f, 0.35f);
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || env <= 0.001f) {
            emissions.remove(state.entityId);
            super.submit(state, poseStack, collector, cameraState);
            return;
        }

        float bow = (state.heavy ? 1.35f : 0.72f) * (0.35f + 0.65f * env);
        float bandsHeight = state.heavy ? 1.7f : 1.05f;
        Vec3 center = new Vec3(state.ex, state.ey, state.ez);

        SmashEmission emission = emissions.get(state.entityId);
        if (emission != null && state.age < emission.lastAge - 0.01f) {
            // Age ran backwards: a new smash took this id or the level reset.
            emissions.remove(state.entityId);
            emission = null;
        }
        boolean fresh = emission == null
            || emission.level != level
            || emission.seed != state.seed
            || emission.heavy != state.heavy;
        if (fresh) {
            emission = new SmashEmission(level, center, state.seed, state.heavy);
            emissions.put(state.entityId, emission);
        }

        // The frame follows the entity every submit: a heavy smash rides
        // the falling owner down before it plants, so the sampled volumes
        // track the live position, radius, and bow instead of freezing.
        Frame frame = emission.frame;
        frame.center = center;
        frame.radius = state.radius;
        frame.bow = bow;
        frame.bandsHeight = bandsHeight;

        float delta = fresh ? (1.0f / 60.0f)
            : Math.min(0.05f, Math.max(0.0f, (state.age - emission.lastAge) / 20.0f));
        emission.lastAge = state.age;

        float dist = MaterialEmitters.distanceScale(center);
        float plumeEnv = env * Math.max(0.0f, 1.0f - state.age / (life * 0.6f));
        emission.tick(delta, env * dist, plumeEnv * dist);
        pruneEmissions(state.entityId);

        super.submit(state, poseStack, collector, cameraState);
    }

    /** Drops bundles for smashes that stopped rendering several seconds ago. */
    private void pruneEmissions(int keepId) {
        long now = System.nanoTime();
        Iterator<Map.Entry<Integer, SmashEmission>> it = emissions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, SmashEmission> entry = it.next();
            if (entry.getKey() != keepId && now - entry.getValue().lastSeenNanos > 5_000_000_000L) {
                it.remove();
            }
        }
    }

    /** Mutable per-frame sampling data read by the sources. Data only. */
    private static final class Frame {
        Vec3 center = Vec3.ZERO;
        float radius = 1.0f;
        float bow = 0.7f;
        float bandsHeight = 1.0f;
    }

    /** Points across the impact disc at a low height band. */
    private static MaterialEmitters.Source disc(Frame frame, float radiusFrac, float yBase, float yRange) {
        return random -> {
            double ang = random.nextDouble() * Math.PI * 2.0;
            double rr = frame.radius * radiusFrac * Math.sqrt(random.nextDouble());
            return new MaterialEmitters.Sample(new Vec3(
                frame.center.x + Math.cos(ang) * rr,
                frame.center.y + yBase + random.nextDouble() * yRange,
                frame.center.z + Math.sin(ang) * rr), Vec3.ZERO);
        };
    }

    /**
     * Points on the rim at a sampled height, each carrying a tangential
     * velocity. This is what replaces the counter rotating dust bands: the
     * swirl is real particle motion now, not a turning mesh.
     */
    private static MaterialEmitters.Source rimSwirl(Frame frame, float radiusFrac,
                                                    float yBase, float heightFrac, float dir, float speed) {
        return random -> {
            double ang = random.nextDouble() * Math.PI * 2.0;
            double r = frame.radius * radiusFrac;
            double yTop = frame.bandsHeight * heightFrac;
            double y = yBase + random.nextDouble() * Math.max(0.01f, yTop - yBase);
            Vec3 pos = new Vec3(frame.center.x + Math.cos(ang) * r,
                frame.center.y + y, frame.center.z + Math.sin(ang) * r);
            Vec3 vel = new Vec3(-Math.sin(ang) * dir * speed, 0.012, Math.cos(ang) * dir * speed);
            return new MaterialEmitters.Sample(pos, vel);
        };
    }

    /**
     * Points inside the squashed volume the shell occupied: horizontal
     * spread across the footprint, height following the old bow profile
     * (tallest at the center, falling to the rim).
     */
    private static MaterialEmitters.Source shellVolume(Frame frame, float radiusFrac, float heightFrac) {
        return random -> {
            double ang = random.nextDouble() * Math.PI * 2.0;
            double rr = frame.radius * radiusFrac * Math.sqrt(random.nextDouble());
            double domeT = Math.sqrt(Math.max(0.0, 1.0 - (rr * rr) / Math.max(0.01,
                frame.radius * radiusFrac * frame.radius * radiusFrac)));
            double y = frame.bow * heightFrac * domeT * (0.25 + 0.75 * random.nextDouble());
            Vec3 pos = new Vec3(frame.center.x + Math.cos(ang) * rr,
                frame.center.y + 0.04 + y, frame.center.z + Math.sin(ang) * rr);
            Vec3 vel = new Vec3(Math.cos(ang) * 0.03, 0.02, Math.sin(ang) * 0.03);
            return new MaterialEmitters.Sample(pos, vel);
        };
    }

    /** Points along the seeded fracture arms on the ground. */
    private static MaterialEmitters.Source cracks(Frame frame, float[] armAngles, float[] armLengths) {
        return random -> {
            int arm = random.nextInt(armAngles.length);
            double t = random.nextDouble();
            double len = frame.radius * armLengths[arm] * t;
            Vec3 pos = new Vec3(frame.center.x + Math.cos(armAngles[arm]) * len,
                frame.center.y + 0.05, frame.center.z + Math.sin(armAngles[arm]) * len);
            Vec3 vel = new Vec3(Math.cos(armAngles[arm]) * 0.03, 0.005, Math.sin(armAngles[arm]) * 0.03);
            return new MaterialEmitters.Sample(pos, vel);
        };
    }

    /** Points in the early impact plume column above the center. */
    private static MaterialEmitters.Source plume(Frame frame, float height) {
        return random -> {
            double ang = random.nextDouble() * Math.PI * 2.0;
            double rr = frame.radius * 0.38 * Math.sqrt(random.nextDouble());
            return new MaterialEmitters.Sample(new Vec3(
                frame.center.x + Math.cos(ang) * rr,
                frame.center.y + 0.05 + random.nextDouble() * height,
                frame.center.z + Math.sin(ang) * rr), Vec3.ZERO);
        };
    }

    private static BlockState groundState(ClientLevel level, Vec3 center) {
        BlockState state = level.getBlockState(BlockPos.containing(center.x, center.y - 0.5, center.z));
        return state.isAir() ? Blocks.STONE.defaultBlockState() : state;
    }

    /**
     * The material emitter bundle for one smash. Ground disc dust and a
     * bright plasma splash replace the glow fan; three rim swirl layers
     * replace the broken vortex bands; dust and vapor through the shell
     * volume plus core plasma replace the displaced domes; dark dust and
     * plasma along the fracture arms replace the crack star; a dust and
     * ember column replaces the plume. Debris chunks of the real ground
     * block and steel sparks carry the physical impact.
     */
    private static final class SmashEmission {
        private final ClientLevel level;
        private final int seed;
        private final boolean heavy;
        private final Frame frame = new Frame();
        private final MaterialEmitters.Emitter discDust;
        private final MaterialEmitters.Emitter splashPlasma;
        private final MaterialEmitters.Emitter rimDustA;
        private final MaterialEmitters.Emitter rimDustB;
        private final MaterialEmitters.Emitter rimVapor;
        private final MaterialEmitters.Emitter shellDust;
        private final MaterialEmitters.Emitter shellVapor;
        private final MaterialEmitters.Emitter corePlasma;
        private final MaterialEmitters.Emitter crackDust;
        private final MaterialEmitters.Emitter crackPlasma;
        private final MaterialEmitters.Emitter debris;
        private final MaterialEmitters.Emitter sparks;
        private final MaterialEmitters.Emitter plumeDust;
        private final MaterialEmitters.Emitter plumeEmber;
        private float lastAge = -1.0f;
        private long lastSeenNanos = System.nanoTime();

        private SmashEmission(ClientLevel level, Vec3 center, int seed, boolean heavy) {
            this.level = level;
            this.seed = seed;
            this.heavy = heavy;
            this.frame.center = center;
            int shellColor = heavy ? SHELL_HEAVY : SHELL_NORMAL;
            float big = heavy ? 1.25f : 1.0f;

            // Seeded fracture arms, the crack star's data without its mesh:
            // seven arms at seeded angles and lengths inside the footprint.
            Random rng = new Random(seed ^ 0xCA01L);
            float[] armAngles = new float[7];
            float[] armLengths = new float[7];
            for (int i = 0; i < armAngles.length; i++) {
                armAngles[i] = (float) ((i / (double) armAngles.length) * Math.PI * 2.0 + rng.nextDouble() * 0.5);
                armLengths[i] = 0.98f * (0.55f + 0.45f * rng.nextFloat());
            }

            BlockState ground = groundState(level, center);
            this.discDust = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(BRONZE, 1.9f, seed ^ 0x610F, Vec3.ZERO, 0.05f),
                disc(frame, 1.02f, 0.03f, 0.22f), 230f * big, 300);
            this.splashPlasma = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(SPLASH, 0.95f, seed ^ 0x6110, Vec3.ZERO, 0.02f),
                disc(frame, 1.02f, 0.02f, 0.08f), 60f * big, 90);
            this.rimDustA = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(BRONZE, 1.7f, seed ^ 0xBA01, Vec3.ZERO, 0.02f),
                rimSwirl(frame, 0.82f, 0.10f, 1.0f, 1.0f, 0.55f), 200f * big, 280);
            this.rimDustB = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(BRONZE, 1.9f, seed ^ 0xBA02, Vec3.ZERO, 0.02f),
                rimSwirl(frame, 0.88f, 0.06f, 0.78f, -1.0f, 0.70f), 115f * big, 170);
            this.rimVapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(PALE, 2.3f, seed ^ 0xBA03, Vec3.ZERO, 0.015f),
                rimSwirl(frame, 0.95f, 0.03f, 0.55f, 1.0f, 0.40f), 55f * big, 85);
            this.shellDust = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(BRONZE, 2.1f, seed ^ 0xD001, Vec3.ZERO, 0.03f),
                shellVolume(frame, 1.0f, 1.0f), 165f * big, 230);
            this.shellVapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(shellColor, 2.5f, seed ^ 0xD003, Vec3.ZERO, 0.02f),
                shellVolume(frame, 1.0f, 1.0f), 75f * big, 110);
            this.corePlasma = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(HOT_CORE, 1.15f, seed ^ 0xD002, Vec3.ZERO, 0.015f),
                shellVolume(frame, 0.62f, 0.85f), 120f * big, 170);
            this.crackDust = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(CRACK_DARK, 1.25f, seed ^ 0xCA01, Vec3.ZERO, 0.01f),
                cracks(frame, armAngles, armLengths), 90f * big, 130);
            this.crackPlasma = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(BRONZE, 0.8f, seed ^ 0xCA02, Vec3.ZERO, 0.008f),
                cracks(frame, armAngles, armLengths), 34f * big, 55);
            this.debris = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(new BlockParticleOption(DbaParticles.BLOCK_CHUNKS, ground),
                    new Vec3(0.0, 0.30, 0.0), 0.13f),
                disc(frame, 0.9f, 0.04f, 0.10f), 50f * big, 80);
            this.sparks = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK, new Vec3(0.0, 0.22, 0.0), 0.16f),
                disc(frame, 0.9f, 0.04f, 0.12f), 65f * big, 95);
            this.plumeDust = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(PALE, 1.8f, seed ^ 0xD057, new Vec3(0.0, 0.17, 0.0), 0.05f),
                plume(frame, heavy ? 2.3f : 1.5f), 150f * big, 200);
            this.plumeEmber = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.ember(new Vec3(0.0, 0.24, 0.0), 0.06f),
                plume(frame, heavy ? 2.3f : 1.5f), 38f * big, 60);
        }

        private void tick(float deltaSeconds, float envelope, float plumeEnvelope) {
            lastSeenNanos = System.nanoTime();
            discDust.tick(deltaSeconds, envelope);
            splashPlasma.tick(deltaSeconds, envelope);
            rimDustA.tick(deltaSeconds, envelope);
            rimDustB.tick(deltaSeconds, envelope * 0.6f);
            rimVapor.tick(deltaSeconds, envelope * 0.35f);
            shellDust.tick(deltaSeconds, envelope);
            shellVapor.tick(deltaSeconds, envelope * 0.8f);
            corePlasma.tick(deltaSeconds, envelope * 0.7f);
            crackDust.tick(deltaSeconds, envelope);
            crackPlasma.tick(deltaSeconds, envelope * 0.6f);
            debris.tick(deltaSeconds, envelope);
            sparks.tick(deltaSeconds, envelope);
            plumeDust.tick(deltaSeconds, plumeEnvelope);
            plumeEmber.tick(deltaSeconds, plumeEnvelope);
        }
    }
}
