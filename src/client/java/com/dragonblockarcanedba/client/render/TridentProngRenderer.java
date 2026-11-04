package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.TridentProngEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One shot: three needles in trident formation.
 * No-shapes pass: the renderer draws no geometry at all. Each needle head
 * is a dense hellfire plasma emission cluster at the same formation
 * offsets, radii, intro fade, and post-impact dissolve the crescent blades
 * used, with flame at the heads, embers at the needle bases where the
 * barbs were, and a plasma trail shed behind each needle. The seed comes
 * from the entity UUID, so the formation is identical on every client.
 */
public class TridentProngRenderer extends EntityRenderer<TridentProngEntity, TridentProngRenderer.ProngState> {

    /** Shell color of the old needles, (0.36, 0.02, 0.04) as an RGB int. */
    private static final int SHELL = 0x5C050A;
    /** Hot core color of the old needles, (0.96, 0.24, 0.08) as an RGB int. */
    private static final int CORE = 0xF53D14;
    private static final float[] OFFSETS = { -0.26f, 0.0f, 0.26f };
    private static final float[] RADII = { 0.95f, 1.25f, 0.95f };

    public TridentProngRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ProngState extends EntityRenderState {
        public float yRot;
        public float xRot;
        public float age;
        public int dieTick;
        /** Synced shape seed from the entity UUID. */
        public long seed = 0;
        public UUID uuid;
    }

    public boolean shouldRender(TridentProngEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(TridentProngEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ProngState createRenderState() {
        return new ProngState();
    }

    @Override
    public void extractRenderState(TridentProngEntity entity, ProngState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.age = entity.tickCount + partialTick;
        state.dieTick = entity.getDieTick();
        state.seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();
        state.uuid = entity.getUUID();
    }

    @Override
    public void submit(ProngState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        ClientLevel level = Minecraft.getInstance() == null ? null : Minecraft.getInstance().level;
        if (level == null || state.uuid == null) return;

        // Same intro fade and post-impact dissolve the geometry version used.
        float intro = Math.min(1.0f, state.age / 2.0f);
        float fade = intro * intro * (3.0f - 2.0f * intro);
        if (state.dieTick > 0) {
            float t = Math.min(1.0f, Math.max(0.0f, (state.age - state.dieTick) / 6.0f));
            float ease = t * t * (3.0f - 2.0f * t);
            fade *= 1.0f - ease;
        }
        if (fade <= 0.01f) return;

        Emission emission = EMISSIONS.get(state.uuid);
        if (emission == null || emission.level != level) {
            emission = new Emission(level, (int) state.seed);
            EMISSIONS.put(state.uuid, emission);
            pruneEmissions();
        }
        Vec3 center = new Vec3(state.x, state.y, state.z);
        Vec3 forward = Vec3.directionFromRotation(state.xRot, state.yRot);
        double yawRad = Math.toRadians(state.yRot);
        Vec3 right = new Vec3(Math.cos(yawRad), 0, Math.sin(yawRad));
        for (int i = 0; i < 3; i++) {
            emission.centers[i] = center.add(right.scale(OFFSETS[i]));
        }
        emission.forward = forward;
        float envelope = fade * MaterialEmitters.distanceScale(center);
        emission.tick(state.age, envelope);
    }

    /**
     * Per shot emitter set, one cluster group per needle. The sources are
     * data only: they read the needle centers and flight direction at
     * sample time and hand MaterialEmitters points at the heads, at the
     * needle bases, and along the wake paths. Nothing here draws anything.
     */
    private static final class Emission {
        final ClientLevel level;
        final Vec3[] centers = { Vec3.ZERO, Vec3.ZERO, Vec3.ZERO };
        Vec3 forward = new Vec3(0, 0, 1);
        float lastAge = Float.NaN;
        long lastSeenNanos = System.nanoTime();
        final MaterialEmitters.Emitter[] clusters = new MaterialEmitters.Emitter[3];
        final MaterialEmitters.Emitter[] flames = new MaterialEmitters.Emitter[3];
        final MaterialEmitters.Emitter[] trails = new MaterialEmitters.Emitter[3];
        final MaterialEmitters.Emitter[] embers = new MaterialEmitters.Emitter[3];

        Emission(ClientLevel level, int seed) {
            this.level = level;
            for (int i = 0; i < 3; i++) {
                final int idx = i;
                MaterialEmitters.Source headSource = random -> {
                    Vec3 c = this.centers[idx];
                    double r = 0.20 * RADII[idx];
                    return new MaterialEmitters.Sample(new Vec3(
                        c.x + (random.nextDouble() * 2.0 - 1.0) * r,
                        c.y + (random.nextDouble() * 2.0 - 1.0) * r,
                        c.z + (random.nextDouble() * 2.0 - 1.0) * r), Vec3.ZERO);
                };
                MaterialEmitters.Source baseSource = random -> {
                    Vec3 c = this.centers[idx].subtract(this.forward.scale(0.10));
                    return new MaterialEmitters.Sample(new Vec3(
                        c.x + (random.nextDouble() * 2.0 - 1.0) * 0.10,
                        c.y + (random.nextDouble() * 2.0 - 1.0) * 0.10,
                        c.z + (random.nextDouble() * 2.0 - 1.0) * 0.10), Vec3.ZERO);
                };
                MaterialEmitters.Source trailSource = random -> {
                    double t = random.nextDouble();
                    Vec3 p = this.centers[idx].subtract(this.forward.scale(0.08 + 0.90 * t));
                    return new MaterialEmitters.Sample(p, Vec3.ZERO);
                };
                clusters[i] = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.plasma(CORE, 1.25f * RADII[idx], seed + idx * 13, Vec3.ZERO, 0.02f),
                    headSource, 85f * RADII[idx], 150);
                flames[i] = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.flame(new Vec3(0, 0.02, 0), 0.03f),
                    headSource, 38f * RADII[idx], 70);
                trails[i] = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.plasma(SHELL, 0.95f * RADII[idx], seed + idx * 13 + 21, Vec3.ZERO, 0.015f),
                    trailSource, 65f * RADII[idx], 120);
                embers[i] = new MaterialEmitters.Emitter(level,
                    MaterialEmitters.Preset.ember(Vec3.ZERO, 0.06f),
                    baseSource, 20f, 40);
            }
        }

        void tick(float age, float envelope) {
            lastSeenNanos = System.nanoTime();
            if (Float.isNaN(lastAge)) {
                lastAge = age;
                return;
            }
            float delta = Math.min(0.25f, Math.max(0f, (age - lastAge) / 20f));
            lastAge = age;
            if (delta <= 0f || envelope <= 0.01f) return;
            for (int i = 0; i < 3; i++) {
                clusters[i].tick(delta, envelope);
                flames[i].tick(delta, envelope * 0.85f);
                trails[i].tick(delta, envelope);
                embers[i].tick(delta, envelope * 0.7f);
            }
        }
    }

    private static final Map<UUID, Emission> EMISSIONS = new HashMap<>();

    /** Drop emitter sets for shots that have not rendered in a while. */
    private static void pruneEmissions() {
        if (EMISSIONS.size() < 48) return;
        long cutoff = System.nanoTime() - 30_000_000_000L;
        EMISSIONS.values().removeIf(e -> e.lastSeenNanos < cutoff);
    }
}
