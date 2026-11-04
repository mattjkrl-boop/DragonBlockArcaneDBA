package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.entity.SkyCracksEntity;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.mojang.math.Axis;
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
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The torn sky above the Cursed Tempest, as a layered composition whose
 * centerpiece is a real crack plane. The pieces layer is two crackFans
 * drawn on the rift plane itself: a wide crimson web at radius 21, hot
 * white at the heart, and a tighter violet web inside it, both through the
 * shader, on a pose billboarded to the camera and rotated so the crackFan
 * ground plane becomes the billboard plane. Falling from the cracks are
 * dark solid shards, real FragmentEntity pieces the host rains server
 * side on its bolt beat in 3 clusters of 2, 2, 1 (ENTITY-STANDARD rain
 * mode); their positions are world truth, so this renderer draws none
 * of them. What the renderer does draw at the tear's heart is a refill,
 * not rain: five curse glass shards hanging at the billboard center
 * and pulsing on the same twenty tick shed beat, so the heart never
 * reads empty between rain batches. The
 * complex particle layer is wisp curls drifting off the fractures. The
 * material emission stays as filler: seven main jagged paths and a web of
 * shorter ones, regenerated from the entity's synced seed in the camera
 * billboard plane on every frame, carry glowing curse plasma and a white
 * hot core, violet plasma fills the web, soul wisps drift off the
 * fractures, and a broad darkness emission behind the paths reads as the
 * shadow of the tear. Alpha still runs the shared lifecycle envelope over
 * the entity's 100 tick life times the slow electrical flicker.
 */
public class SkyCracksRenderer extends EntityRenderer<SkyCracksEntity, SkyCracksRenderer.SkyCracksRenderState> {

    public SkyCracksRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SkyCracksRenderState extends EntityRenderState {
        public float age = 0;
        public int seed = 0;
        public float lastEmitAge = -1f;
    }

    @Override
    public SkyCracksRenderState createRenderState() {
        return new SkyCracksRenderState();
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(SkyCracksEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(SkyCracksEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public void extractRenderState(SkyCracksEntity entity, SkyCracksRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getSeed();
    }

    @Override
    public void submit(SkyCracksRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float env = FxEnvelope.envelope(state.age, SkyCracksEntity.LIFE_TICKS, 0.15f, 0.35f);
        if (env <= 0.01f) return;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        float ds = MaterialEmitters.distanceScale(new Vec3(state.x, state.y, state.z));
        float flicker = 0.78f + 0.22f * (float) Math.sin(state.age * 2.1f + state.seed * 0.37f);
        float alpha = env * flicker;
        float emitEnv = alpha * ds;
        float delta = emissionDelta(state, state.age);
        int seed = state.seed;
        Vec3 center = new Vec3(state.x, state.y, state.z);

        // The billboard plane basis, from the same camera orientation the
        // drawn version used. The plane now only positions emission points.
        Quaternionf orientation = cameraState.orientation != null
            ? new Quaternionf(cameraState.orientation).conjugate() : new Quaternionf();
        Vector3f basisX = orientation.transform(new Vector3f(1, 0, 0));
        Vector3f basisY = orientation.transform(new Vector3f(0, 1, 0));

        List<List<Vec3>> paths = crackPaths(center, basisX, basisY, seed);
        MaterialEmitters.Source pathSource = pathsSource(paths);

        // The falling shards are no longer drawn here: the host rains
        // them server side as real FragmentEntity pieces (rain mode, see
        // SkyCracksEntity.tick), so their positions are world truth
        // instead of spots on this per-client billboard plane.

        // Pieces layer: the crack plane itself. The pose is
        // billboarded to the camera, and a copy rotated minus ninety
        // degrees about X turns the crackFan ground plane into the
        // billboard plane, exactly as the old crack stars were oriented.
        {
            final long crackSeed = seed;
            final float crackEnv = alpha;
            final float crackDs = ds;
            poseStack.pushPose();
            if (cameraState.orientation != null) {
                poseStack.mulPose(new org.joml.Matrix4f().rotation(
                    new Quaternionf(cameraState.orientation).conjugate()));
            }
            ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) -> {
                PoseStack.Pose flat = pose.copy();
                flat.rotate(Axis.XP.rotation((float) (-Math.PI / 2.0)));
                // Attack upgrade: two extra branches per fan. crackFan
                // derives its crack count from segments(9, detail, 6),
                // so the count is raised through the detail scale each
                // fan already takes (plus 2/9 adds exactly two base
                // cracks at any range above the detail floor).
                ComplexGeometry.crackFan(flat, buffer, crackSeed ^ 0xC4ACCL, 21.0f,
                    0xFFF7F7, 0xF20D33, 0.95f, crackEnv, crackDs + 2f / 9f);
                ComplexGeometry.crackFan(flat, buffer, crackSeed ^ 0xF04CL, 13.0f,
                    0xE9D5FF, 0x8C1AF2, 0.92f, crackEnv, crackDs * 0.7f + 2f / 9f);
            });
            poseStack.popPose();
        }

        // Pieces layer, heart refill (attack upgrade): five curse glass
        // shards hang at the tear's heart so the center never reads
        // empty. Drawn in the plain entity pose with the axis straight
        // down, the way the drawn clusters sat before the rain became
        // real entities, and pulsed on the host's shed beat: the rain
        // fires server side at tickCount % 20 == 10, and this burst's
        // envelope swells on the same phase. Curse glass is an energy
        // material, so the solid emissive path stays correct here.
        {
            float beatPhase = state.age % 20.0f;
            float shedPulse = 0.65f + 0.35f * (float) Math.cos((beatPhase - 10.0f) * (Math.PI * 2.0 / 20.0));
            final long heartSeed = seed;
            final float heartEnv = alpha * shedPulse;
            final float heartDs = ds;
            ComplexGeometry.submitSolid(collector, poseStack, (pose, buffer) ->
                ComplexGeometry.shardBurst(pose, buffer, heartSeed ^ 0x5A9DL, Vec3.ZERO,
                    new Vec3(0, -1, 0), 45f, 1.0f, 5,
                    0x2A0A4A, 0x8C1AF2, 0.95f, heartEnv, heartDs));
        }

        // Complex particle layer: wisp curls curling off the fractures.
        emit(level, MaterialEmitters.Preset.fixed(DbaParticles.WISP_CURL, Vec3.ZERO, 0.015f),
            pathSource, 28f, delta, emitEnv);

        // The fractures themselves: crimson plasma bleeding along the paths,
        // a white hot core on the same paths, violet plasma through the web,
        // and soul wisps drifting off the tear.
        emit(level, MaterialEmitters.Preset.plasma(0xF20D33, 1.55f, seed ^ 0xC4ACC, Vec3.ZERO, 0.030f),
            pathSource, 520f, delta, emitEnv);
        emit(level, MaterialEmitters.Preset.plasma(0xFFF7F7, 1.00f, seed ^ 0xB17E, Vec3.ZERO, 0.025f),
            pathSource, 220f, delta, emitEnv);
        emit(level, MaterialEmitters.Preset.plasma(0x8C1AF2, 1.30f, seed ^ 0xF04C, Vec3.ZERO, 0.030f),
            pathSource, 260f, delta, emitEnv);
        emit(level, MaterialEmitters.Preset.soul(0xC9A6FF, 1.10f, seed ^ 0x5017, Vec3.ZERO, 0.025f),
            pathSource, 90f, delta, emitEnv);

        // The shadow of the tear: broad darkness sampled across the billboard
        // disc behind the fractures, strongest near the heart.
        emit(level, MaterialEmitters.Preset.darkness(0x1A0312, 4.20f, seed ^ 0x5EED, Vec3.ZERO, 0.010f),
            discSource(center, basisX, basisY, 22.0), 110f, delta, emitEnv * 0.8f);
    }

    /** A point in the billboard plane from plane-local coordinates. */
    private static Vec3 planePoint(Vec3 center, Vector3f basisX, Vector3f basisY, double x, double y) {
        return new Vec3(
            center.x + basisX.x * x + basisY.x * y,
            center.y + basisX.y * x + basisY.y * y,
            center.z + basisX.z * x + basisY.z * y);
    }

    /**
     * The fracture set as jagged polylines in the billboard plane: seven
     * mains radiating from the heart plus a shorter web, all regenerated
     * from the synced seed. Data only; nothing here draws.
     */
    private static List<List<Vec3>> crackPaths(Vec3 center, Vector3f basisX, Vector3f basisY, int seed) {
        Random rng = new Random(seed);
        List<List<Vec3>> paths = new ArrayList<>(15);
        int mains = 7;
        for (int m = 0; m < mains; m++) {
            double ang = (m / (double) mains) * Math.PI * 2.0 + (rng.nextDouble() * 0.5 - 0.25);
            float len = 15.0f + rng.nextFloat() * 10.0f;
            paths.add(jaggedPath(center, basisX, basisY, ang, len, rng));
        }
        for (int w = 0; w < 8; w++) {
            double ang = rng.nextDouble() * Math.PI * 2.0;
            float len = 6.0f + rng.nextFloat() * 8.0f;
            paths.add(jaggedPath(center, basisX, basisY, ang, len, rng));
        }
        return paths;
    }

    private static List<Vec3> jaggedPath(Vec3 center, Vector3f basisX, Vector3f basisY, double ang, float len, Random rng) {
        int segments = 6;
        List<Vec3> path = new ArrayList<>(segments + 1);
        double dirX = Math.cos(ang);
        double dirY = Math.sin(ang);
        for (int s = 0; s <= segments; s++) {
            double frac = s / (double) segments;
            double along = len * frac;
            double side = (rng.nextDouble() * 2.0 - 1.0) * 1.15 * Math.sin(frac * Math.PI);
            path.add(planePoint(center, basisX, basisY,
                dirX * along - dirY * side, dirY * along + dirX * side));
        }
        return path;
    }

    /** Samples points across a disc in the billboard plane. Data only. */
    private static MaterialEmitters.Source discSource(Vec3 center, Vector3f basisX, Vector3f basisY, double radius) {
        return random -> {
            double ang = random.nextDouble() * Math.PI * 2.0;
            double rr = radius * Math.sqrt(random.nextDouble());
            return new MaterialEmitters.Sample(planePoint(center, basisX, basisY, Math.cos(ang) * rr, Math.sin(ang) * rr), Vec3.ZERO);
        };
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

    private static void emit(ClientLevel level, MaterialEmitters.Preset preset, MaterialEmitters.Source source,
                             float ratePerSecond, float delta, float envelope) {
        if (level == null || envelope <= 0.01f || ratePerSecond <= 0f) return;
        int budget = Math.max(1, Math.round(ratePerSecond));
        new MaterialEmitters.Emitter(level, preset, source, ratePerSecond, budget).tick(delta, envelope);
    }

    private static float emissionDelta(SkyCracksRenderState state, float age) {
        float prev = state.lastEmitAge;
        state.lastEmitAge = age;
        if (prev < 0f || age < prev) return 1.0f / 60.0f;
        return Math.max(0.001f, Math.min(0.05f, (age - prev) / 20.0f));
    }
}
