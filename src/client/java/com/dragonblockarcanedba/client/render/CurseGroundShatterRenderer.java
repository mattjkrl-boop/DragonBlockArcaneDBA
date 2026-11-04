package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.entity.CurseGroundShatterEntity;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Curse Ground Shatter as a layered composition. The pieces layer is a
 * crackFan of curse energy wandering across the ground plus a fan of
 * fracturePlates pushed out toward the rim: real lifted slabs with
 * thickness, tilted corners, and seeded gaps, lit hot crimson at the
 * center and dark at the rim. Through the disc the plates vacate, a
 * cluster of large obsidian spikes bursts up on the hit beat (fast
 * four tick rise, no hold, crack-and-fade settle), drawn lit as
 * physical stone. The
 * complex particle layer is shard chips thrown along the fissure paths.
 * The material emission stays as filler: seeded jagged paths across the
 * ground carry crimson and pale curse plasma, darkness smoke vents from
 * the same paths, ground dust rolls across the area and the expanding
 * shock front, and the twelve slab positions survive as dust and darkness
 * emission points where the debris used to rise. The corrected fade over
 * the fixed 35 tick life is unchanged, and every path derives from the
 * entity UUID seed so every client sees the same shatter.
 */
public class CurseGroundShatterRenderer extends EntityRenderer<CurseGroundShatterEntity, CurseGroundShatterRenderer.ShatterRenderState> {

    public CurseGroundShatterRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ShatterRenderState extends EntityRenderState {
        public float radius = 3.5f;
        public float age = 0;
        public int lifetime = 35;
        public long seed = 0;
        public float lastEmitAge = -1f;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(CurseGroundShatterEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(CurseGroundShatterEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ShatterRenderState createRenderState() {
        return new ShatterRenderState();
    }

    @Override
    public void extractRenderState(CurseGroundShatterEntity entity, ShatterRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.age = entity.tickCount + partialTicks;
        state.lifetime = entity.getLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(ShatterRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        float radius = state.radius;
        float age = state.age;
        // state.lifetime is the fixed 35 tick total (it only decrements server
        // side), so the old age / (lifetime + age) formula stranded the fade at
        // about 0.5 when the entity vanished. Progress over the total reaches 0.
        float fade = Math.max(0.0f, 1.0f - age / (float) state.lifetime);
        if (fade <= 0.02f) return;
        int seed = (int) (state.seed ^ (state.seed >>> 32));
        Vec3 center = new Vec3(state.x, state.y, state.z);
        float ds = MaterialEmitters.distanceScale(center);
        float env = fade * ds;
        float delta = emissionDelta(state, age);

        // Pieces layer: the crackFan energy web first, then the solid
        // fracture plates over it, both seeded from the entity UUID.
        final long pieceSeed = state.seed;
        final float pieceFade = fade;
        final float pieceDs = ds;
        ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) ->
            ComplexGeometry.crackFan(pose, buffer, pieceSeed ^ 0xC4AC2L, radius * 1.02f,
                0xFFF2F7, 0xF20D33, 0.95f, pieceFade, pieceDs));

        // Attack upgrade: a cluster of obsidian spikes bursts up
        // through the disc on the hit beat. Few and big on purpose:
        // eight spikes at 1.25 times the builder's size classes,
        // scattered over a tight inner disc so they bunch and lean
        // like a real eruption instead of ringing the shatter evenly.
        // The palette is real black glass, not purple crystal: near
        // black at the root, only a faint violet sheen toward the
        // point, so the lit facets do the reading. Fast rise over
        // four ticks, no hold, then a crack-and-fade settle: the
        // spikes sink back over the next eight ticks while their
        // envelope falls, gone by about tick thirteen while the rim
        // plates and cracks carry the rest of the life. Obsidian is
        // physical matter, so the spike bodies draw lit through
        // submitSolidLit with the render state's own light, never on
        // the emissive path.
        final float spikeRise = FxEnvelope.smoothstep(Math.min(1.0f, age / 4.0f));
        final float spikeSink = age <= 4.0f ? 0.0f
            : FxEnvelope.smoothstep(Math.min(1.0f, (age - 4.0f) / 8.0f));
        final float spikeSettle = 1.0f - FxEnvelope.smoothstep(
            Math.min(1.0f, Math.max(0.0f, age - 4.0f) / 9.0f));
        final float spikeEnv = pieceFade * spikeSettle;
        final int spikeLight = state.lightCoords;
        ComplexGeometry.submitSolidLit(collector, poseStack, spikeLight, (pose, buffer) ->
            ComplexGeometry.spikeEruption(pose, buffer, pieceSeed ^ 0x5917EL, Vec3.ZERO,
                new Vec3(1, 0, 0), ComplexGeometry.SpikeLayout.SCATTER, 8, radius * 0.80f,
                1.25f, spikeRise, spikeSink,
                0xFF070409, 0xFF241832, 0.95f, spikeEnv, pieceDs));

        // The plates are pushed to the rim: the same fan drawn at 1.2
        // times the radius, so the plate bodies sit in the outer band
        // and the spikes own the disc they erupt through.
        ComplexGeometry.submitSolid(collector, poseStack, (pose, buffer) ->
            ComplexGeometry.fracturePlates(pose, buffer, pieceSeed ^ 0xF9A72L, radius * 1.2f, 0.38f,
                0xB3122E, 0x241419, 0.95f, pieceFade, pieceDs));

        // The fissures: seeded jagged ground paths carrying curse plasma,
        // with darkness venting along them.
        List<List<Vec3>> cracks = crackPaths(center, radius, state.seed);
        MaterialEmitters.Source crackSource = pathsSource(cracks);
        emit(level, MaterialEmitters.Preset.plasma(0xF20D33, 1.05f, seed + 0xF155, Vec3.ZERO, 0.020f),
            crackSource, 300f, delta, env);
        emit(level, MaterialEmitters.Preset.plasma(0xFFF2F7, 0.80f, seed + 0xF156, Vec3.ZERO, 0.020f),
            crackSource, 150f, delta, env);
        emit(level, MaterialEmitters.Preset.darkness(0x14030A, 1.60f, seed + 0xF157, new Vec3(0, 0.012, 0), 0.015f),
            crackSource, 150f, delta, env);

        // Complex particle layer: shard chips kicked up along the fissures.
        emit(level, MaterialEmitters.Preset.fixed(DbaParticles.SHARD_CHIP, new Vec3(0, 0.030, 0), 0.040f),
            crackSource, 34f, delta, env);

        // Ground dust across the whole shatter, thickening on the expanding
        // shock front the old band used to mark.
        emit(level, MaterialEmitters.Preset.dust(0x6E625E, 1.20f, seed + 0x51A1, Vec3.ZERO, 0.030f),
            MaterialEmitters.groundArea(center, radius, center.y + 0.05), 180f, delta, env);
        float shockR = Math.min(radius * 1.3f, age * 0.45f);
        if (shockR > 0.1f) {
            emit(level, MaterialEmitters.Preset.dust(0x8A7A72, 1.05f, seed + 0x51A2, Vec3.ZERO, 0.035f),
                MaterialEmitters.groundArea(center, shockR, center.y + 0.05), 220f, delta, env);
            emit(level, MaterialEmitters.Preset.plasma(0x8C1AF2, 0.90f, seed + 0x51A3, Vec3.ZERO, 0.025f),
                MaterialEmitters.groundArea(center, shockR, center.y + 0.07), 90f, delta, env);
        }

        // The twelve slab positions: dust and darkness rising where the
        // erupting slabs used to stand, growing in over the first frames.
        float growth = Math.min(1.0f, age * 0.4f);
        if (growth > 0.02f) {
            List<Vec3> slabs = slabPoints(center, radius, state.seed);
            MaterialEmitters.Source slabSource = pointsSource(slabs);
            emit(level, MaterialEmitters.Preset.dust(0x574A45, 1.35f, seed + 269, new Vec3(0, 0.030, 0), 0.020f),
                slabSource, 120f, delta, env * growth);
            emit(level, MaterialEmitters.Preset.darkness(0x26050F, 1.30f, seed + 271, new Vec3(0, 0.020, 0), 0.015f),
                slabSource, 60f, delta, env * growth);
        }
    }

    /** Seeded jagged radial fissure paths across the ground. Data only. */
    private static List<List<Vec3>> crackPaths(Vec3 center, float radius, long seed) {
        Random rng = new Random(seed ^ 0xF155L);
        int cracks = 7 + rng.nextInt(3);
        List<List<Vec3>> paths = new ArrayList<>(cracks);
        for (int k = 0; k < cracks; k++) {
            double ang = (k / (double) cracks) * Math.PI * 2.0 + (rng.nextDouble() * 2.0 - 1.0) * 0.30;
            double len = radius * (0.55 + rng.nextDouble() * 0.45);
            int segments = 5;
            List<Vec3> path = new ArrayList<>(segments + 1);
            for (int s = 0; s <= segments; s++) {
                double frac = s / (double) segments;
                double radial = len * frac;
                double side = (rng.nextDouble() * 2.0 - 1.0) * 0.16 * frac;
                path.add(new Vec3(
                    center.x + Math.cos(ang) * radial - Math.sin(ang) * side,
                    center.y + 0.04,
                    center.z + Math.sin(ang) * radial + Math.cos(ang) * side));
            }
            paths.add(path);
        }
        return paths;
    }

    /** The twelve old slab base positions, from the same seeded stream shape. */
    private static List<Vec3> slabPoints(Vec3 center, float radius, long seed) {
        Random rng = new Random(seed);
        List<Vec3> points = new ArrayList<>(12);
        for (int r = 0; r < 12; r++) {
            double ang = (r / 12.0) * Math.PI * 2.0 + rng.nextDouble() * 0.4;
            float dist = radius * (0.35f + rng.nextFloat() * 0.55f);
            points.add(new Vec3(center.x + Math.cos(ang) * dist, center.y + 0.08, center.z + Math.sin(ang) * dist));
        }
        return points;
    }

    /** Samples a random point along a random path in the set. */
    private static MaterialEmitters.Source pathsSource(List<List<Vec3>> paths) {
        List<List<Vec3>> copy = List.copyOf(paths);
        return random -> {
            if (copy.isEmpty()) return new MaterialEmitters.Sample(Vec3.ZERO, Vec3.ZERO);
            List<Vec3> path = copy.get(random.nextInt(copy.size()));
            if (path.size() < 2) return new MaterialEmitters.Sample(path.isEmpty() ? Vec3.ZERO : path.get(0), Vec3.ZERO);
            int i = random.nextInt(path.size() - 1);
            Vec3 a = path.get(i);
            Vec3 b = path.get(i + 1);
            return new MaterialEmitters.Sample(a.add(b.subtract(a).scale(random.nextDouble())), Vec3.ZERO);
        };
    }

    /** Samples one of the given points at random. */
    private static MaterialEmitters.Source pointsSource(List<Vec3> points) {
        List<Vec3> copy = List.copyOf(points);
        return random -> new MaterialEmitters.Sample(copy.isEmpty() ? Vec3.ZERO : copy.get(random.nextInt(copy.size())), Vec3.ZERO);
    }

    private static void emit(ClientLevel level, MaterialEmitters.Preset preset, MaterialEmitters.Source source,
                             float ratePerSecond, float delta, float envelope) {
        if (level == null || envelope <= 0.01f || ratePerSecond <= 0f) return;
        int budget = Math.max(1, Math.round(ratePerSecond));
        new MaterialEmitters.Emitter(level, preset, source, ratePerSecond, budget).tick(delta, envelope);
    }

    private static float emissionDelta(ShatterRenderState state, float age) {
        float prev = state.lastEmitAge;
        state.lastEmitAge = age;
        if (prev < 0f || age < prev) return 1.0f / 60.0f;
        return Math.max(0.001f, Math.min(0.05f, (age - prev) / 20.0f));
    }
}
