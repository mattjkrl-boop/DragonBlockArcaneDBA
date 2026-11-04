package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.SpiritCleaveEntity;
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
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Renderer for the Spirit Sword cleave.
 *
 * Attack upgrade pass (worst offender 1 rebuild): the cleave is now a
 * layered composition with a real solid and impact layer. The pieces
 * body is a five strand ComplexGeometry tendrilFan woven along the
 * flight line (soul cyan to white) through submitEnergy with the
 * tendril canvas, replacing the old two lone tendrils. A shardBurst of
 * four soul shards rides the arc's leading edge, drawn lit through
 * submitSolidLit because crystal is physical matter. On the hit beat
 * (the tick the blade stops, known from the synced LIFE the same way
 * the server sets it) a clustered fan of five uneven soul crystal
 * spikes erupts from the
 * ground at the hit point in the swing's direction, also submitSolidLit:
 * RISE over the last 4 ticks of flight so the set is fully out as the
 * blade arrives, HOLD 3 ticks, then a crack-and-fade settle through
 * the envelope while the host fades. If a wall ends the flight early
 * the synced LIFE snaps and the rise restarts at the true hit point.
 * Sprite layers: the soul streams run at double their old rates, and
 * SPARK_STREAK plus SHARD_CHIP bursts fire at the hit point on the
 * hit beat. The no-shapes material emission stays as filler only. The
 * growth curve, lifecycle envelope, colors, and synced seed are
 * unchanged, and nothing here touches damage, range, or timing: the
 * renderer only reads the synced LIFE, seed, and motion the entity
 * already publishes.
 */
public class SpiritCleaveRenderer extends EntityRenderer<SpiritCleaveEntity, SpiritCleaveRenderer.CleaveState> {

    private static final int WHITE = 0xFFFFFF;
    private static final int CYAN = 0x00E5FF;
    private static final int GOLD = 0xFFC83C;
    // Pale cyan glass for the erupted soul crystal spikes (lit path):
    // a deeper, saturated base where the crystal is thick, running to
    // near clear at the thin tips, the way real glass reads.
    private static final int GLASS_ROOT = 0xFF3FA7C6;
    private static final int GLASS_TIP = 0xFFEFFBFF;

    public SpiritCleaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class CleaveState extends EntityRenderState {
        public float yRot;
        public float xRot;
        public float age;
        public float life = SpiritCleaveEntity.FLIGHT_TICKS + SpiritCleaveEntity.FADE_TICKS;
        public long seed;
        public Vec3 motion = Vec3.ZERO;
        // Renderer-local spike beat bookkeeping: the LIFE value seen on
        // the previous frame and the age the spike rise started at.
        public float prevLife = -1.0f;
        public float riseStart = Float.NaN;
    }

    public boolean shouldRender(SpiritCleaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(SpiritCleaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public CleaveState createRenderState() {
        return new CleaveState();
    }

    @Override
    public void extractRenderState(SpiritCleaveEntity entity, CleaveState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTick;
        state.life = entity.getLifeTicks();
        state.seed = entity.getSeed();
        state.motion = entity.getDeltaMovement();
    }

    @Override
    public void submit(CleaveState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        float open = ease(Math.min(1.0f, state.age / 3.0f));
        float fade = FxEnvelope.envelope(state.age, state.life, 0.08f, 0.30f);
        if (fade <= 0.01f) return;

        float length = 2.25f * (0.30f + 0.70f * open);
        float tipFlash = ease(Math.max(0.0f, 1.0f - Math.max(0.0f, state.age - 4.0f) / 4.0f));
        Vec3 origin = new Vec3(state.x, state.y, state.z);
        Vec3 direction = Vec3.directionFromRotation(state.xRot, state.yRot);
        float distScale = MaterialEmitters.distanceScale(origin);
        float emission = fade * distScale;
        float delta = frameDelta();

        // The hit beat: the server sets LIFE to the stop tick plus
        // FADE_TICKS when the flight ends, so the stop tick is life
        // minus FADE_TICKS both while the flight runs its full course
        // (predicted) and after an early wall hit (snapped). The stop
        // point is the entity position plus the remaining flight.
        float dieAge = state.life - SpiritCleaveEntity.FADE_TICKS;
        if (Float.isNaN(state.riseStart)) state.riseStart = dieAge - 4.0f;
        if (state.prevLife >= 0.0f && state.life < state.prevLife - 0.5f) {
            // A wall ended the flight early: restart the spike rise at
            // the true hit point instead of popping in fully risen.
            state.riseStart = state.age;
        }
        state.prevLife = state.life;
        Vec3 stopLocal = state.age >= dieAge ? Vec3.ZERO
            : state.motion.scale(Math.max(0.0f, dieAge - state.age));
        Vec3 stopWorld = origin.add(stopLocal);

        // Spike beat factors: RISE 4 ticks ending at the hit, HOLD 3,
        // then the settle falls through the envelope (sinkFactor stays
        // 0; the set cracks away standing, per the addendum recipe).
        float spikeRise = ease(Math.max(0.0f, Math.min(1.0f, (state.age - state.riseStart) / 4.0f)));
        float settleStart = dieAge + 3.0f;
        float spikeEnv = state.age <= settleStart ? 1.0f
            : 1.0f - ease(Math.min(1.0f, (state.age - settleStart) / Math.max(0.5f, state.life - settleStart)));

        Vec3 traveledFrom = origin.subtract(direction.scale(length * 1.35));
        Vec3 traveledTo = origin.add(direction.scale(length * 0.25));
        MaterialEmitters.Source traveledPath = MaterialEmitters.path(traveledFrom, traveledTo, 0.010);

        // Soul streams doubled (attack upgrade spec): rates and budgets
        // at twice the complex pass values on all three soul emitters.
        emit(level,
            MaterialEmitters.Preset.soul(WHITE, 3.00f, seedInt(state.seed, 1),
                direction.scale(0.040), 0.060f),
            MaterialEmitters.sphereVolume(origin, length * 0.42),
            430.0f, 520, delta, emission);
        emit(level,
            MaterialEmitters.Preset.soul(CYAN, 3.50f, seedInt(state.seed, 2),
                direction.scale(-0.030), 0.055f),
            traveledPath, 350.0f, 440, delta, emission);
        emit(level,
            MaterialEmitters.Preset.vapor(GOLD, 4.60f, seedInt(state.seed, 3),
                direction.scale(-0.020), 0.090f),
            traveledPath, 68.0f, 90, delta, emission);
        emit(level,
            MaterialEmitters.Preset.fixed(DbaParticles.SPARK, direction.scale(-0.080), 0.160f),
            traveledPath, 58.0f, 75, delta, emission);

        if (tipFlash > 0.02f) {
            Vec3 openingFront = origin.add(direction.scale(length * 0.76));
            emit(level,
                MaterialEmitters.Preset.soul(WHITE, 3.20f, seedInt(state.seed, 4),
                    direction.scale(0.030), 0.050f),
                MaterialEmitters.sphereVolume(openingFront, 0.22),
                330.0f, 400, delta, emission * tipFlash);
        }

        // Hit point sprite bursts: spark streaks and shard chips thrown
        // off the stop point across the hit beat, ramping in over the
        // last tick and a half of flight and settling with the spikes.
        float burstIn = ease(Math.max(0.0f, Math.min(1.0f, (state.age - (dieAge - 1.5f)) / 1.5f)));
        float burstEmission = emission * burstIn * spikeEnv;
        if (burstEmission > 0.02f) {
            emit(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, direction.scale(0.120), 0.300f),
                MaterialEmitters.point(stopWorld),
                120.0f, 140, delta, burstEmission);
            emit(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP, direction.scale(0.050), 0.300f),
                MaterialEmitters.point(stopWorld),
                90.0f, 110, delta, burstEmission);
        }

        // Complex particle layer: wisp curls shed along the traveled
        // path, inside the 40 per second continuous budget.
        emit(level,
            MaterialEmitters.Preset.fixed(DbaParticles.WISP_CURL, direction.scale(-0.050), 0.090f),
            traveledPath, 26.0f, 40, delta, emission);

        // Pieces layer, class M per submit, in entity local space
        // through the normal depth tested paths. Vertex counts at
        // distScale 1.0: the tendrilFan body is five tendrils at 384
        // (1920 of 2500) on the energy submit; the leading edge
        // shardBurst is four shards at 288 (1152 of 2500) and the spike
        // eruption is five spikes at 384 (1920 of 2500), each on its own
        // lit submit because crystal is physical matter.
        final Vec3 bodyFrom = direction.scale(-length * 1.35);
        final Vec3 bodyTo = direction.scale(length * 0.25);
        final float fanLength = length * 1.55f;
        final long geoSeed = state.seed;
        final float geoEnv = fade;
        final float geoDs = distScale;
        final int geoLight = state.lightCoords;
        ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) -> {
            ComplexGeometry.tendrilFan(pose, buffer, geoSeed ^ 0xC1EA1L,
                bodyFrom, direction, 52.0f, fanLength, 5, 0.34f,
                CYAN, WHITE, 0.95f, geoEnv, geoDs);
        });
        ComplexGeometry.submitSolidLit(collector, poseStack, geoLight, (pose, buffer) -> {
            ComplexGeometry.shardBurst(pose, buffer, geoSeed ^ 0xC1EA3L,
                bodyTo, direction, 55.0f, Math.max(0.35f, length * 0.52f), 4,
                CYAN, WHITE, 0.95f, geoEnv, geoDs);
        });

        // Impact spikes: a tight cluster of five big, uneven soul
        // crystal spikes erupting from the ground at the hit point in
        // the swing's direction, bunched close so the set reads as one
        // eruption with varied heights, not a uniform spike bar. The
        // ground is found by a client side collision scan under the
        // stop point; a swing over open air simply grows no spikes.
        double groundY = findGroundY(level, stopWorld.x, stopWorld.y, stopWorld.z);
        if (!Double.isNaN(groundY) && spikeRise > 0.0f && spikeEnv > 0.01f) {
            final Vec3 spikeCenter = new Vec3(stopLocal.x, groundY - origin.y, stopLocal.z);
            final float spikeRiseF = spikeRise;
            final float spikeEnvF = spikeEnv;
            ComplexGeometry.submitSolidLit(collector, poseStack, geoLight, (pose, buffer) -> {
                ComplexGeometry.spikeEruption(pose, buffer, geoSeed ^ 0xC1EA4L,
                    spikeCenter, direction, ComplexGeometry.SpikeLayout.FAN, 5, 1.05f, 0.85f,
                    spikeRiseF, 0.0f, GLASS_ROOT, GLASS_TIP, 0.98f, spikeEnvF, geoDs);
            });
        }
    }

    /**
     * Client side ground scan for the spike fan: the top face of the
     * first collidable block at or below the stop point, searching one
     * block above it down to ten below. NaN when the swing stops over
     * open air. Presentation only; no block is ever changed.
     */
    private static double findGroundY(ClientLevel level, double x, double y, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        int startY = (int) Math.floor(y) + 1;
        int endY = (int) Math.floor(y) - 10;
        for (int by = startY; by >= endY; by--) {
            pos.set(bx, by, bz);
            BlockState blockState = level.getBlockState(pos);
            VoxelShape shape = blockState.getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                return by + shape.max(Direction.Axis.Y);
            }
        }
        return Double.NaN;
    }

    private static void emit(ClientLevel level, MaterialEmitters.Preset preset, MaterialEmitters.Source source,
                             float ratePerSecond, int budgetPerSecond, float deltaSeconds, float envelope) {
        if (envelope <= 0.01f) return;
        new MaterialEmitters.Emitter(level, preset, source, ratePerSecond, budgetPerSecond)
            .tick(deltaSeconds, envelope);
    }

    private static int seedInt(long seed, int stream) {
        long mixed = seed ^ stream * 0x9E3779B97F4A7C15L;
        return (int) (mixed ^ (mixed >>> 32));
    }

    private static float frameDelta() {
        long frameTimeNs = Minecraft.getInstance().getFrameTimeNs();
        if (frameTimeNs <= 0L) return 1.0f / 60.0f;
        return Math.max(0.002f, Math.min(0.05f, frameTimeNs / 1_000_000_000.0f));
    }

    private static float ease(float t) {
        float c = Math.max(0.0f, Math.min(1.0f, t));
        return c * c * (3.0f - 2.0f * c);
    }
}
