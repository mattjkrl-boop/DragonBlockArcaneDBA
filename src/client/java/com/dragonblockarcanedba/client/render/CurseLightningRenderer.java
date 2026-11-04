package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.CurseLightningEntity;
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
 * Curse Lightning as lightning plasma along a seeded path, not a drawn bolt.
 * The no-shapes conversion removes the branch ribbons, crack star, and glow
 * pool entirely. The strike path survives as data: a midpoint-displaced
 * jagged polyline regenerated from the entity UUID seed on every frame, so
 * every client emits along the same bolt. Layered plasma in the sheath
 * color and a white hot core rides the path, a thinner twin strike lands
 * beside it, and the ground arcs become short plasma paths crawling out
 * from the strike point over ground dust. The 2.5 tick stab and the fade
 * reaching zero at tick 9.5 are unchanged.
 * (The Cursed Tempest's live strikes ride BoltEntity; this renderer serves
 * the registered CurseLightningEntity type.)
 */
public class CurseLightningRenderer extends EntityRenderer<CurseLightningEntity, CurseLightningRenderer.CurseLightningRenderState> {

    public CurseLightningRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class CurseLightningRenderState extends EntityRenderState {
        public boolean isRare;
        public float age;
        public long seed;
        public float lastEmitAge = -1f;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(CurseLightningEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(CurseLightningEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public CurseLightningRenderState createRenderState() {
        return new CurseLightningRenderState();
    }

    @Override
    public void extractRenderState(CurseLightningEntity entity, CurseLightningRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.isRare = entity.isRare();
        state.age = (CurseLightningEntity.LIFE_TICKS - entity.life) + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(CurseLightningRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float flashTime = state.age;
        // The entity lives exactly CurseLightningEntity.LIFE_TICKS and the
        // fade below reaches zero at tick 9.5, so picture and lifetime agree.
        if (flashTime > CurseLightningEntity.LIFE_TICKS) return;

        float strike = ease(Math.min(1.0f, flashTime / 2.5f));
        float fadeT = flashTime <= 2.5f ? 1.0f : Math.max(0.0f, 1.0f - (flashTime - 2.5f) / 7.0f);
        float flashAlpha = ease(fadeT);
        if (flashAlpha <= 0.02f) return;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        boolean rare = state.isRare;
        int sheath = rare ? 0x8C0AF2 : 0xCC0A66;
        int seed = (int) (state.seed ^ (state.seed >>> 32));
        Vec3 center = new Vec3(state.x, state.y, state.z);
        float ds = MaterialEmitters.distanceScale(center);
        float env = flashAlpha * ds;
        float delta = emissionDelta(state, flashTime);
        float cloud = 16.0f;

        // The trunk stabs down as the strike eases in: its ground end travels
        // from the cloud to the strike point over 2.5 ticks, exactly as the
        // branch bolt's end did, now as an emission path.
        Vec3 top = center.add(0, cloud, 0);
        Vec3 ground = center.add(0, cloud * (1.0f - strike), 0);
        List<Vec3> trunk = boltPath(top, ground, state.seed);
        MaterialEmitters.Source trunkSource = MaterialEmitters.path(trunk, 0.0);
        emit(level, MaterialEmitters.Preset.plasma(sheath, 1.30f, seed, Vec3.ZERO, 0.030f),
            trunkSource, 360f, delta, env);
        emit(level, MaterialEmitters.Preset.plasma(0xFFF0FA, 0.85f, seed + 17, Vec3.ZERO, 0.025f),
            trunkSource, 220f, delta, env);
        emit(level, MaterialEmitters.Preset.darkness(0x16030C, 1.50f, seed + 23, Vec3.ZERO, 0.020f),
            trunkSource, 90f, delta, env * 0.7f);

        // Twin strike: a thinner companion path on its own seed, landing
        // beside the trunk at the same stab progress.
        Random twinRandom = new Random(state.seed ^ 0x7F11L);
        Vec3 twinGround = center.add(twinRandom.nextDouble() * 3.2 - 1.6, cloud * (1.0f - strike), twinRandom.nextDouble() * 3.2 - 1.6);
        List<Vec3> twin = boltPath(top, twinGround, state.seed ^ 0x7A11L);
        emit(level, MaterialEmitters.Preset.plasma(sheath, 0.95f, seed + 0x7A11, Vec3.ZERO, 0.030f),
            MaterialEmitters.path(twin, 0.0), 160f, delta, env * 0.7f);

        // Ground arcs crawling out from the strike point: short seeded
        // plasma paths over ground dust, fading with the flash.
        float groundReach = 3.2f * ease(Math.min(1.0f, flashTime / 6.0f));
        float ringAlpha = flashAlpha * Math.max(0.0f, 1.0f - flashTime / 9.0f);
        if (ringAlpha > 0.01f && groundReach > 0.1f) {
            List<List<Vec3>> arcs = groundArcs(center, groundReach, state.seed);
            emit(level, MaterialEmitters.Preset.plasma(sheath, 0.90f, seed + 0xC1A0, Vec3.ZERO, 0.025f),
                pathsSource(arcs), 180f, delta, env * ringAlpha);
            emit(level, MaterialEmitters.Preset.plasma(0xFFF7F7, 1.10f, seed + 0x610A, Vec3.ZERO, 0.020f),
                MaterialEmitters.point(center.add(0, 0.05, 0)), 90f, delta, env * ringAlpha);
            emit(level, MaterialEmitters.Preset.dust(0x6E625E, 1.10f, seed + 0xD057, Vec3.ZERO, 0.030f),
                MaterialEmitters.groundArea(center, groundReach, center.y + 0.05), 120f, delta, env * ringAlpha);
        }
    }

    /** A midpoint-displaced jagged path between two points. Data only. */
    private static List<Vec3> boltPath(Vec3 from, Vec3 to, long seed) {
        List<Vec3> points = new ArrayList<>();
        points.add(from);
        points.add(to);
        Random rng = new Random(seed);
        double displacement = from.distanceTo(to) * 0.16;
        for (int iteration = 0; iteration < 4; iteration++) {
            List<Vec3> next = new ArrayList<>(points.size() * 2 - 1);
            for (int i = 0; i + 1 < points.size(); i++) {
                Vec3 a = points.get(i);
                Vec3 b = points.get(i + 1);
                next.add(a);
                Vec3 mid = a.add(b).scale(0.5).add(
                    (rng.nextDouble() * 2.0 - 1.0) * displacement, 0, (rng.nextDouble() * 2.0 - 1.0) * displacement);
                next.add(mid);
            }
            next.add(points.get(points.size() - 1));
            points = next;
            displacement *= 0.55;
        }
        return points;
    }

    /** Short seeded plasma arc paths crawling across the ground. Data only. */
    private static List<List<Vec3>> groundArcs(Vec3 center, float reach, long seed) {
        Random rng = new Random(seed ^ 0xC1A0L);
        List<List<Vec3>> arcs = new ArrayList<>(5);
        for (int k = 0; k < 5; k++) {
            double ang = rng.nextDouble() * Math.PI * 2.0;
            double len = reach * (0.4 + rng.nextDouble() * 0.6);
            List<Vec3> arc = new ArrayList<>(4);
            for (int s = 0; s <= 3; s++) {
                double frac = s / 3.0;
                double side = (rng.nextDouble() * 2.0 - 1.0) * 0.14 * frac;
                arc.add(new Vec3(
                    center.x + Math.cos(ang) * len * frac - Math.sin(ang) * side,
                    center.y + 0.04,
                    center.z + Math.sin(ang) * len * frac + Math.cos(ang) * side));
            }
            arcs.add(arc);
        }
        return arcs;
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

    private static float emissionDelta(CurseLightningRenderState state, float age) {
        float prev = state.lastEmitAge;
        state.lastEmitAge = age;
        if (prev < 0f || age < prev) return 1.0f / 60.0f;
        return Math.max(0.001f, Math.min(0.05f, (age - prev) / 20.0f));
    }

    private static float ease(float t) {
        float c = Math.max(0.0f, Math.min(1.0f, t));
        return c * c * (3.0f - 2.0f * c);
    }
}
