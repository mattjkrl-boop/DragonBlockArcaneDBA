package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
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
 * The smash, rebuilt for the complex effects redo as a layered
 * composition, the crater flagship of this weapon. The pieces layer is
 * a fracturePlates fan at the builder's full plate budget (submitSolid,
 * faceted shading), sized to the real crater the entity leaves (radius
 * 3, or 4 on the heavy smash, plus its permanent fracture ring one
 * wider) and drawn only while ground is near under the impact point, so
 * the plates sit over that crater instead of floating under a heavy
 * smash still riding its falling owner. A crackFan of bronze energy
 * cracks runs through the plates on the shader (submitEnergy). The
 * complex particle layer is a SHARD_CHIP burst at the impact bundle's
 * first frame plus a continuous chip stream off the disc. These are the
 * fast near field fragments: the entity already spawns a DebrisSpawner
 * field server side for the big physical chunks. Under all of it, the
 * material filler stays as the no-shapes pass left it: what was a filled
 * bronze dome shell is material:
 * stone dust and shock vapor sampled through the squashed volume the shell
 * occupied, hot bronze plasma motes in the core volume, three swirling
 * dust layers at the rim where the counter rotating bands used to turn,
 * dark dust and sparse plasma running along the seeded fracture lines on
 * the ground (the entity still scars the real fracture disk into the
 * surface), physical debris chunks of the actual ground block, steel
 * sparks, and an early impact plume of pale dust and embers.
 *
 * The attack upgrade pass adds the crater rim wall to the pieces
 * layer: two clustered rockWall arc segments in ground-sampled rock
 * on the lit path (submitSolidLit), a main arc of wide slabs and a
 * shorter, lower cluster arc shouldering inside it, rising with the
 * composition's build, holding through its peak, and slamming down
 * over 3 ticks as the plates settle, where the entity answers with a
 * burst of 12 real debris chunks. Sprite rates on the six largest
 * filler emitters are trimmed about a tenth so the wall stays
 * readable; no other piece or sprite counts changed.
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
        public float yRot;
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
        state.yRot = entity.getYRot();
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
        if (fresh) {
            // Complex particle layer, burst half: stone chips off the
            // impact disc on the bundle's first frame, inside the
            // class L burst budget of 60.
            MaterialEmitters.burst(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP,
                    new Vec3(0.0, 0.38, 0.0), 0.22f),
                disc(emission.frame, 0.9f, 0.04f, 0.12f), 42);
        }

        // Pieces layer, in the local ground frame, gated on ground
        // actually being near under the impact point: a heavy smash
        // rides its falling owner down, and plates drawn mid fall would
        // float in the air with no crater under them. The plate radius
        // follows the growing shock radius only up to the footprint the
        // entity really breaks: crater radius 3 (4 heavy) plus the
        // permanent fracture ring one block wider. Combined vertices
        // stay well inside the class L budget: plates draw about 480
        // and the crack fan under 900 of the 6000 allowed.
        if (groundNear(level, center)) {
            final long geoSeed = state.seed;
            final float geoEnv = env;
            final float geoDist = dist;
            final float plateRadius = Math.min(state.radius, state.heavy ? 5.0f : 4.0f);
            final float crackRadius = plateRadius * 1.08f;
            final float plateLift = state.heavy ? 0.42f : 0.30f;
            final int plateHot = state.heavy ? SHELL_HEAVY : SHELL_NORMAL;
            ComplexGeometry.submitSolid(collector, poseStack, (pose, buffer) ->
                ComplexGeometry.fracturePlates(pose, buffer, geoSeed ^ 0x9A7EL,
                    plateRadius, plateLift, plateHot, 0x4A3826, 0.96f, geoEnv, geoDist));
            ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) ->
                ComplexGeometry.crackFan(pose, buffer, geoSeed ^ 0xC4A9L,
                    crackRadius, HOT_CORE, BRONZE, 0.95f, geoEnv, geoDist));

            // Crater rim wall (attack upgrade pass): real rock heaving
            // up around the rim, in a palette sampled from the ground
            // under the impact and drawn on the lit path, never
            // emissive. Two clustered arc segments, not a neat ring: a
            // main arc of six wide slabs in front of the caster, and a
            // shorter, lower cluster arc shouldering inside its flank
            // at a smaller radius, so the rim reads as uneven heaved
            // rock with a mountain profile. The beat follows the
            // foundation factor contract on this composition's own
            // age: RISE over the first 30 percent of life, HOLD
            // through the envelope peak, then SLAM down over 3 ticks
            // as the plate and crack composition settles. At the
            // slam's end tick the entity answers with a DebrisSpawner
            // burst of 12 real chunks (see WarMaceSmashEntity), so the
            // wall ends as rubble, not as a fade.
            float wallRise = FxEnvelope.smoothstep(Math.min(1f, state.age / (life * 0.30f)));
            float wallSink = state.age <= life * 0.55f ? 0f
                : FxEnvelope.smoothstep(Math.min(1f, (state.age - life * 0.55f) / 3f));
            if (wallSink < 1f) {
                int[] wallPalette = groundWallPalette(level, center);
                double yawRad = Math.toRadians(state.yRot);
                Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
                float rimRadius = state.heavy ? 5.0f : 4.0f;
                drawWallArc(collector, poseStack, state.lightCoords, geoSeed ^ 0x9A11L,
                    forward, 0.20f, rimRadius, 7.2f, state.heavy ? 2.1f : 1.8f,
                    wallRise, wallSink, wallPalette[0], wallPalette[1], geoEnv, geoDist);
                drawWallArc(collector, poseStack, state.lightCoords, geoSeed ^ 0x9A12L,
                    forward, -0.95f, rimRadius * 0.80f, 5.6f, state.heavy ? 1.55f : 1.3f,
                    wallRise, wallSink, wallPalette[0], wallPalette[1], geoEnv, geoDist);
            }
        }
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

    /**
     * True when solid ground is within about three blocks under the
     * impact point, sampled at the center and at four points around it
     * so a cratered center column or a slope does not fail the check.
     * Depths reach past the crater bowl the entity digs (about two
     * blocks at the center), while a heavy smash still high in its fall
     * finds only air and draws no ground pieces yet.
     */
    private static boolean groundNear(ClientLevel level, Vec3 center) {
        double[][] samples = {{0.0, 0.0}, {1.2, 0.0}, {-1.2, 0.0}, {0.0, 1.2}, {0.0, -1.2}};
        for (double[] s : samples) {
            for (double drop = 0.5; drop <= 3.5; drop += 1.0) {
                BlockState below = level.getBlockState(
                    BlockPos.containing(center.x + s[0], center.y - drop, center.z + s[1]));
                if (!below.isAir()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static BlockState groundState(ClientLevel level, Vec3 center) {
        BlockState state = level.getBlockState(BlockPos.containing(center.x, center.y - 0.5, center.z));
        return state.isAir() ? Blocks.STONE.defaultBlockState() : state;
    }

    /**
     * One arc segment of the crater rim wall. The segment midpoint sits
     * on the rim circle at centerAngle from the caster's forward, the
     * wall faces the crater center so the builder's arc curvature wraps
     * the rim (length over radius is the bend), and the slabs draw
     * through submitSolidLit with the frame's light coords: physical
     * rock under world lighting, never on the emissive path.
     */
    private static void drawWallArc(SubmitNodeCollector collector, PoseStack poseStack, int lightCoords,
                                    long seed, Vec3 forward, float centerAngle, float radius, float length,
                                    float height, float rise, float sink,
                                    int rootArgb, int tipArgb, float envelope, float distScale) {
        double cos = Math.cos(centerAngle);
        double sin = Math.sin(centerAngle);
        Vec3 dir = new Vec3(forward.x * cos + forward.z * sin, 0.0, -forward.x * sin + forward.z * cos);
        Vec3 origin = dir.scale(radius);
        Vec3 facing = dir.scale(-1.0);
        float arc = length / radius;
        ComplexGeometry.submitSolidLit(collector, poseStack, lightCoords, (pose, buffer) ->
            ComplexGeometry.rockWall(pose, buffer, seed, origin, facing, length, height, arc,
                rise, sink, rootArgb, tipArgb, 0.98f, envelope, distScale));
    }

    /**
     * Ground-sampled wall palette: the map color of the block under
     * the impact point, pulled toward rock (desaturated a third of the
     * way to its own luminance) so heaved ground reads as earth and
     * stone rather than a flat colored slab. Root is the same tone
     * darkened for the buried base. Grass samples as its dirt body,
     * not the green top.
     */
    private static int[] groundWallPalette(ClientLevel level, Vec3 center) {
        BlockState state = groundState(level, center);
        if (state.is(Blocks.GRASS_BLOCK)) {
            state = Blocks.DIRT.defaultBlockState();
        }
        BlockPos pos = BlockPos.containing(center.x, center.y - 0.5, center.z);
        int col = state.getMapColor(level, pos).col;
        if (col == 0) {
            col = 0x7A7A7A;
        }
        return new int[]{rockTone(col, 0.45f), rockTone(col, 1.0f)};
    }

    /** One palette tone: the color desaturated toward its luminance, then scaled. */
    private static int rockTone(int rgb, float brightness) {
        float r = (rgb >> 16) & 0xFF;
        float g = (rgb >> 8) & 0xFF;
        float b = rgb & 0xFF;
        float lum = 0.30f * r + 0.59f * g + 0.11f * b;
        r = (r + (lum - r) * 0.35f) * brightness;
        g = (g + (lum - g) * 0.35f) * brightness;
        b = (b + (lum - b) * 0.35f) * brightness;
        int ri = Math.max(0, Math.min(255, Math.round(r)));
        int gi = Math.max(0, Math.min(255, Math.round(g)));
        int bi = Math.max(0, Math.min(255, Math.round(b)));
        return 0xFF000000 | (ri << 16) | (gi << 8) | bi;
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
        private final MaterialEmitters.Emitter discChips;
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
                disc(frame, 1.02f, 0.03f, 0.22f), 205f * big, 300);
            this.splashPlasma = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(SPLASH, 0.95f, seed ^ 0x6110, Vec3.ZERO, 0.02f),
                disc(frame, 1.02f, 0.02f, 0.08f), 60f * big, 90);
            this.rimDustA = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(BRONZE, 1.7f, seed ^ 0xBA01, Vec3.ZERO, 0.02f),
                rimSwirl(frame, 0.82f, 0.10f, 1.0f, 1.0f, 0.55f), 180f * big, 280);
            this.rimDustB = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(BRONZE, 1.9f, seed ^ 0xBA02, Vec3.ZERO, 0.02f),
                rimSwirl(frame, 0.88f, 0.06f, 0.78f, -1.0f, 0.70f), 104f * big, 170);
            this.rimVapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(PALE, 2.3f, seed ^ 0xBA03, Vec3.ZERO, 0.015f),
                rimSwirl(frame, 0.95f, 0.03f, 0.55f, 1.0f, 0.40f), 55f * big, 85);
            this.shellDust = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(BRONZE, 2.1f, seed ^ 0xD001, Vec3.ZERO, 0.03f),
                shellVolume(frame, 1.0f, 1.0f), 148f * big, 230);
            this.shellVapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(shellColor, 2.5f, seed ^ 0xD003, Vec3.ZERO, 0.02f),
                shellVolume(frame, 1.0f, 1.0f), 75f * big, 110);
            this.corePlasma = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(HOT_CORE, 1.15f, seed ^ 0xD002, Vec3.ZERO, 0.015f),
                shellVolume(frame, 0.62f, 0.85f), 108f * big, 170);
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
                plume(frame, heavy ? 2.3f : 1.5f), 135f * big, 200);
            this.plumeEmber = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.ember(new Vec3(0.0, 0.24, 0.0), 0.06f),
                plume(frame, heavy ? 2.3f : 1.5f), 38f * big, 60);
            // Complex particle layer, continuous half: stone chips off
            // the impact disc for the life of the smash, under the
            // continuous cap of 40 per second per emitter.
            this.discChips = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP,
                    new Vec3(0.0, 0.30, 0.0), 0.15f),
                disc(frame, 0.9f, 0.04f, 0.12f), 36f, 50);
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
            discChips.tick(deltaSeconds, envelope);
        }
    }
}
