package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.config.DbaEffectsConfig;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The basalt wall on a lava field's edge (attack upgrade pass, audit
 * section 20). The field itself is LavaField's crust pools: real blocks
 * on a server schedule, with no entity and no renderer, so the wall is
 * a client side lingering visual in the AuraRenderer pattern.
 * LavaEruptionRenderer reports every eruption it renders (the eruption
 * is the ability that establishes the pool field), and this system
 * keeps the wall standing on the field's own envelope, the POOL
 * profile schedule in LavaField (stage ticks 60, 120, 200): a slow
 * RISE over the first 10 ticks shedding ember fragments and basalt
 * chips, a STAND while the field lives, and a SETTLE back into the
 * field over the last 6 ticks as the crust settles, shedding basalt
 * chunks as it sinks. The wall damages nothing, blocks nothing, has
 * no collision, and exists on no server: set dressing only.
 *
 * Form, held to the mod's character and form bar: seven slabs only,
 * each over a block wide and running 1.7 to 2.8 blocks tall on the
 * builder's seeded variance, laid on the arc of the field's front
 * edge (the wall's curvature radius is the eruption's final reach,
 * so the ridge follows the lip the pools were laid along instead of
 * cutting across it). The base is grounded on the heightmap at the
 * wall center and the builder buries it a further half block, so the
 * ridge reads as ground that heaved up, not a fence placed on the
 * surface. Palette is dark basalt through submitSolidLit: physical
 * rock, world lit, never emissive. The only energy on the wall is
 * one thin magma vein tendril inset along its front face, fading in
 * over the back half of the rise as if the crack opens as the rock
 * heaves, and dropping with the slabs on the settle (its offset
 * mirrors the builder's own emergence math).
 *
 * Vertex accounting per wall at distScale 1.0: rockWall 7 slabs at
 * 72 is 504, the vein tendril about 384, for roughly 890 total, one
 * submit pair per standing field.
 */
public final class LavaFieldWallRenderer {
    private LavaFieldWallRenderer() {}

    // Field envelope, mirrored from LavaField's POOL stage schedule
    // (STAGE_TICKS POOL total life 200). Renderer side mirror only:
    // if the field's schedule ever changes, this follows by hand.
    private static final int FIELD_LIFE = 200;
    private static final int RISE_TICKS = 10;
    private static final int SETTLE_START = FIELD_LIFE - 6;
    private static final int GONE_AGE = FIELD_LIFE + 2;
    /** LavaEruptionEntity.reachAt at end of life: 1.1 + 5.4. */
    private static final float EDGE_RADIUS = 6.5f;
    private static final float WALL_LENGTH = 8.5f;
    private static final float WALL_HEIGHT = 2.4f;
    private static final float WALL_ARC = WALL_LENGTH / EDGE_RADIUS;
    private static final int BASALT_ROOT = 0xFF17120E;
    private static final int BASALT_TIP = 0xFF4A4238;
    private static final int MAX_WALLS = 48;

    private static final Map<Integer, Wall> WALLS = new HashMap<>();
    private static boolean hooksRegistered;

    private static final class Wall {
        final ClientLevel level;
        final int seed;
        /** Eruption origin, with y replaced by the grounded wall base height. */
        final Vec3 origin;
        final float yRot;
        /** Packed light captured from the eruption's render state at sighting. */
        final int lightCoords;
        /** Level game time at the eruption's birth, so age reads off the clock. */
        final long bornGameTime;

        Wall(ClientLevel level, int seed, Vec3 origin, float yRot, int lightCoords, long bornGameTime) {
            this.level = level;
            this.seed = seed;
            this.origin = origin;
            this.yRot = yRot;
            this.lightCoords = lightCoords;
            this.bornGameTime = bornGameTime;
        }
    }

    /**
     * Called by LavaEruptionRenderer for every eruption it renders.
     * The first sighting of an eruption establishes its wall; repeat
     * sightings of the same eruption (same entity id and synced seed)
     * are ignored, and the birth time derives from the entity's age,
     * so a wall first seen mid flight still runs on the field's clock.
     */
    public static void noteField(ClientLevel level, int entityId, int seed,
                                 double x, double y, double z, float yRot,
                                 float age, int lightCoords) {
        ensureHooks();
        Wall existing = WALLS.get(entityId);
        if (existing != null && existing.level == level && existing.seed == seed) {
            return;
        }
        if (WALLS.size() >= MAX_WALLS) {
            WALLS.values().removeIf(w -> level.getGameTime() - w.bornGameTime > GONE_AGE);
            if (WALLS.size() >= MAX_WALLS) {
                return;
            }
        }
        double yaw = Math.toRadians(yRot);
        Vec3 facing = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 center = new Vec3(x + facing.x * EDGE_RADIUS, y, z + facing.z * EDGE_RADIUS);
        double baseY = y;
        int cx = (int) Math.floor(center.x);
        int cz = (int) Math.floor(center.z);
        if (level.hasChunk(cx >> 4, cz >> 4)) {
            baseY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, cx, cz);
        }
        long born = level.getGameTime() - (int) age;
        WALLS.put(entityId, new Wall(level, seed, new Vec3(x, baseY, z), yRot, lightCoords, born));
    }

    /** Registers the tick and submit hooks once, from the first field sighting. */
    private static void ensureHooks() {
        if (hooksRegistered) return;
        hooksRegistered = true;
        ClientTickEvents.END_CLIENT_TICK.register(LavaFieldWallRenderer::tick);
        LevelRenderEvents.COLLECT_SUBMITS.register(LavaFieldWallRenderer::collectSubmits);
    }

    // ------------------------------------------------------------------
    // Particle shed and pruning (client tick)

    private static void tick(Minecraft mc) {
        if (WALLS.isEmpty()) return;
        ClientLevel level = mc.level;
        if (level == null) {
            WALLS.clear();
            return;
        }
        if (mc.isPaused()) return;
        long now = level.getGameTime();
        RandomSource random = level.getRandom();
        boolean shed = DbaEffectsConfig.effectDensity() > 0f;
        Iterator<Wall> it = WALLS.values().iterator();
        while (it.hasNext()) {
            Wall wall = it.next();
            if (wall.level != level) {
                it.remove();
                continue;
            }
            long age = now - wall.bornGameTime;
            if (age < 0 || age > GONE_AGE) {
                it.remove();
                continue;
            }
            if (!shed) continue;
            if (age < RISE_TICKS) {
                shedRise(level, wall, random);
            } else if (age >= SETTLE_START) {
                shedSettle(level, wall, random, age);
            }
        }
    }

    /** A point on the wall's face arc, at ground level plus the given height. */
    private static Vec3 arcPoint(Wall wall, double phi, double height, double inset) {
        double ang = Math.toRadians(wall.yRot) + phi;
        Vec3 dir = new Vec3(-Math.sin(ang), 0.0, Math.cos(ang));
        return wall.origin.add(dir.scale(EDGE_RADIUS - inset)).add(0.0, height, 0.0);
    }

    private static void shedRise(ClientLevel level, Wall wall, RandomSource random) {
        // Rock heaving up sheds chips of itself and works ember
        // fragments out of the seams: two basalt chunks and one ember
        // fragment per tick, sampled along the wall line.
        for (int i = 0; i < 2; i++) {
            Vec3 pos = arcPoint(wall, (random.nextDouble() * 2.0 - 1.0) * 0.58, 0.12, 0.15);
            level.addParticle(new BlockParticleOption(DbaParticles.BLOCK_CHUNKS, Blocks.BASALT.defaultBlockState()),
                    pos.x, pos.y, pos.z,
                    (random.nextDouble() - 0.5) * 0.07, 0.05 + random.nextDouble() * 0.09,
                    (random.nextDouble() - 0.5) * 0.07);
        }
        Vec3 ember = arcPoint(wall, (random.nextDouble() * 2.0 - 1.0) * 0.58, 0.30, 0.30);
        level.addParticle(DbaParticles.EMBER_FRAGMENT, ember.x, ember.y, ember.z,
                (random.nextDouble() - 0.5) * 0.02, 0.03 + random.nextDouble() * 0.03,
                (random.nextDouble() - 0.5) * 0.02);
    }

    private static void shedSettle(ClientLevel level, Wall wall, RandomSource random, long age) {
        // The crumble: basalt chunks off the face as it sinks, and an
        // ember fragment out of the vein every other tick while the
        // seam is still open.
        for (int i = 0; i < 3; i++) {
            Vec3 pos = arcPoint(wall, (random.nextDouble() * 2.0 - 1.0) * 0.58,
                    0.25 + random.nextDouble() * 0.85, 0.30);
            level.addParticle(new BlockParticleOption(DbaParticles.BLOCK_CHUNKS, Blocks.BASALT.defaultBlockState()),
                    pos.x, pos.y, pos.z,
                    (random.nextDouble() - 0.5) * 0.06, -0.02 - random.nextDouble() * 0.05,
                    (random.nextDouble() - 0.5) * 0.06);
        }
        if ((age & 1L) == 0L) {
            Vec3 ember = arcPoint(wall, (random.nextDouble() * 2.0 - 1.0) * 0.50, 0.70, 0.34);
            level.addParticle(DbaParticles.EMBER_FRAGMENT, ember.x, ember.y, ember.z,
                    0.0, 0.02, 0.0);
        }
    }

    // ------------------------------------------------------------------
    // Pieces (level render submit pass)

    private static void collectSubmits(LevelRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || WALLS.isEmpty()) return;
        if (context.levelState() == null || context.levelState().cameraRenderState == null
                || !context.levelState().cameraRenderState.initialized
                || context.levelState().cameraRenderState.pos == null
                || context.poseStack() == null || context.submitNodeCollector() == null) return;
        if (DbaEffectsConfig.effectDensity() <= 0f) return;
        Vec3 cam = context.levelState().cameraRenderState.pos;
        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        long now = mc.level.getGameTime();
        PoseStack stack = context.poseStack();
        SubmitNodeCollector collector = context.submitNodeCollector();
        for (Wall wall : WALLS.values()) {
            if (wall.level != mc.level) continue;
            float age = (now - wall.bornGameTime) + partial;
            if (age < 0f || age >= GONE_AGE) continue;
            drawWall(collector, stack, cam, wall, age);
        }
    }

    private static void drawWall(SubmitNodeCollector collector, PoseStack stack, Vec3 cam, Wall wall, float age) {
        float riseRaw = Math.min(1f, age / RISE_TICKS);
        float rise = FxEnvelope.smoothstep(riseRaw);
        float sinkRaw = age <= SETTLE_START ? 0f
                : Math.min(1f, (age - SETTLE_START) / (float) (FIELD_LIFE - SETTLE_START));
        float sink = FxEnvelope.smoothstep(sinkRaw);
        float env = age <= SETTLE_START + 2 ? 1f
                : 1f - FxEnvelope.smoothstep((age - (SETTLE_START + 2)) / 4f);
        if (env <= 0.01f) return;
        float ds = MaterialEmitters.distanceScale(wall.origin);
        double yaw = Math.toRadians(wall.yRot);
        Vec3 facing = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));

        final long wallSeed = wall.seed;
        final float riseF = rise;
        final float sinkF = sink;
        final float envF = env;
        final float dsF = ds;
        final Vec3 wallOrigin = facing.scale(EDGE_RADIUS);
        final Vec3 wallFacing = facing.scale(-1.0);
        // The builder's emergence drop, mirrored so the vein rides the
        // slabs exactly: drop is (1 - easeOutCubic(rise)) +
        // easeInQuad(sink) over the wall's full buried depth.
        float drop = Math.min(1f, (1f - rise) * (1f - rise) * (1f - rise) + sink * sink);
        final double veinDrop = -drop * (WALL_HEIGHT * 1.16f + 0.55f);
        final float veinEnv = env * FxEnvelope.smoothstep((riseRaw - 0.45f) / 0.55f) * (1f - sinkRaw * 0.85f);
        final Vec3 veinFrom = arcPointLocal(wall, -0.55, 0.80 + veinDrop, 0.34);
        final Vec3 veinControl = arcPointLocal(wall, 0.0, 1.25 + veinDrop, 0.34);
        final Vec3 veinTo = arcPointLocal(wall, 0.55, 0.90 + veinDrop, 0.34);

        stack.pushPose();
        try {
            stack.translate(wall.origin.x - cam.x, wall.origin.y - cam.y, wall.origin.z - cam.z);
            ComplexGeometry.submitSolidLit(collector, stack, wall.lightCoords, (pose, buffer) ->
                    ComplexGeometry.rockWall(pose, buffer, wallSeed ^ 0xB411L,
                            wallOrigin, wallFacing, WALL_LENGTH, WALL_HEIGHT, WALL_ARC,
                            riseF, sinkF, BASALT_ROOT, BASALT_TIP, 1.0f, envF, dsF));
            if (veinEnv > 0.01f) {
                ComplexGeometry.submitEnergy(collector, stack, ComplexTextures.TENDRIL, (pose, buffer) ->
                        ComplexGeometry.tendril(pose, buffer, wallSeed ^ 0x5EA1L,
                                veinFrom, veinControl, veinTo, 0.14f,
                                0xFF5A00, 0xFFF2B0, 0.90f, veinEnv, dsF));
            }
        } finally {
            stack.popPose();
        }
    }

    /** Arc point in the draw pose's local space (pose origin is the eruption origin). */
    private static Vec3 arcPointLocal(Wall wall, double phi, double height, double inset) {
        double ang = Math.toRadians(wall.yRot) + phi;
        Vec3 dir = new Vec3(-Math.sin(ang), 0.0, Math.cos(ang));
        return dir.scale(EDGE_RADIUS - inset).add(0.0, height, 0.0);
    }
}
