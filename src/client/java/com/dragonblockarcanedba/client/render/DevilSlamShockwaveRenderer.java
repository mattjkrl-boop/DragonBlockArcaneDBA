package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.entity.DevilSlamShockwaveEntity;
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
 * Entity Renderer for Devil Slam Shockwave in Minecraft 26.2.
 * No-shapes pass: the renderer draws no geometry at all. The seal disk,
 * crack webs, erupting shard fans, rim bands, and plume column are gone;
 * in their place the slam is carried by MaterialEmitters over the same
 * ground area and column volume: ground dust and brimstone smoke across
 * the seal, hellfire flame and dark plasma scattered over the disk where
 * the cracks burned, a dense core cluster at the heart, a rising flame,
 * plasma, and ember column while the pillar envelope lasts, and a dust
 * front at the traveling follow radius. All radii, growth, fade, and
 * pillar timing math are the values the geometry version used; only the
 * material changed.
 */
public class DevilSlamShockwaveRenderer extends EntityRenderer<DevilSlamShockwaveEntity, DevilSlamShockwaveRenderer.DevilSlamRenderState> {

    /** Dark red of the old crack webs, (0.5, 0.01, 0.05) as an RGB int. */
    private static final int DARK_RED = 0x80030D;
    /** Hot color of the old inner web, (1.0, 0.22, 0.1) as an RGB int. */
    private static final int HOT = 0xFF381A;
    /** Bright color of the old core pool, (1.0, 0.35, 0.14) as an RGB int. */
    private static final int CORE = 0xFF5924;
    /** Near-black brimstone smoke color. */
    private static final int BRIMSTONE = 0x14060A;
    /** Ground dust color, dark scorched earth. */
    private static final int SCORCH_DUST = 0x4A2417;

    public DevilSlamShockwaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class DevilSlamRenderState extends EntityRenderState {
        public float radius = 5.0f;
        public float age = 0;
        public int lifetime = 35;
        public long seed = 0;
        public UUID uuid;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(DevilSlamShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(DevilSlamShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public DevilSlamRenderState createRenderState() {
        return new DevilSlamRenderState();
    }

    @Override
    public void extractRenderState(DevilSlamShockwaveEntity entity, DevilSlamRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = entity.getRadius();
        state.age = entity.tickCount + partialTicks;
        state.lifetime = entity.getLifetime();
        state.seed = entity.getUUID().getMostSignificantBits();
        state.uuid = entity.getUUID();
    }

    @Override
    public void submit(DevilSlamRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        ClientLevel level = Minecraft.getInstance() == null ? null : Minecraft.getInstance().level;
        if (level == null || state.uuid == null) return;

        final float radius = state.radius;
        final float age = state.age;
        final float lifeT = Math.min(1.0f, age / (float) state.lifetime);
        final float fade = 1.0f - lifeT * lifeT * (3.0f - 2.0f * lifeT);
        final float growT = Math.min(1.0f, age / 10.0f);
        final float grow = 1.0f - (1.0f - growT) * (1.0f - growT) * (1.0f - growT);
        // Telegraph honesty carries over: the emission disk edge is the hit
        // radius, not a picture wider than the damage. The traveling follow
        // front still runs past it as a fading dust echo and deals nothing.
        final float shockR = radius * 1.05f * grow;
        final float pillarT = Math.min(1.0f, age / 18.0f);
        final float pillarEase = pillarT * pillarT * (3.0f - 2.0f * pillarT);
        final float pillarAlpha = (1.0f - pillarEase) * (1.0f - pillarEase);
        final float riseT = Math.min(1.0f, age / 5.5f);
        final float pillarHeight = 6.7f * (1.0f - (1.0f - riseT) * (1.0f - riseT));
        final float pillarRadius = 0.72f * (1.0f + pillarEase * 0.28f);
        float followT = Math.min(1.0f, Math.max(0.0f, (age - 5.0f) / 14.0f));
        float followEase = followT * followT * (3.0f - 2.0f * followT);
        final float followR = radius * (1.2f + 0.9f * followEase);
        final float followA = fade * (1.0f - followEase) * (1.0f - followEase);

        Emission emission = EMISSIONS.get(state.uuid);
        if (emission == null || emission.level != level) {
            emission = new Emission(level, (int) state.seed);
            EMISSIONS.put(state.uuid, emission);
            pruneEmissions();
        }
        emission.center = new Vec3(state.x, state.y, state.z);
        emission.shockR = shockR;
        emission.pillarHeight = pillarHeight;
        emission.pillarRadius = pillarRadius;
        emission.followR = followR;
        float dist = MaterialEmitters.distanceScale(emission.center);
        emission.tick(age, fade * grow * dist, pillarAlpha * dist, followA * dist);
    }

    /**
     * Per slam emitter set. The sources are data only: they read the
     * slam's current center, seal radius, column size, and follow radius
     * at sample time and hand MaterialEmitters points across the ground
     * area, inside the column volume, and on the follow front. Nothing
     * here draws anything.
     */
    private static final class Emission {
        final ClientLevel level;
        Vec3 center = Vec3.ZERO;
        float shockR = 1f;
        float pillarHeight = 0f;
        float pillarRadius = 0.72f;
        float followR = 1f;
        float lastAge = Float.NaN;
        long lastSeenNanos = System.nanoTime();
        final MaterialEmitters.Emitter dustDisk;
        final MaterialEmitters.Emitter flameDisk;
        final MaterialEmitters.Emitter plasmaDisk;
        final MaterialEmitters.Emitter hotDisk;
        final MaterialEmitters.Emitter smokeDisk;
        final MaterialEmitters.Emitter core;
        final MaterialEmitters.Emitter columnFlame;
        final MaterialEmitters.Emitter columnPlasma;
        final MaterialEmitters.Emitter columnEmber;
        final MaterialEmitters.Emitter followDust;

        Emission(ClientLevel level, int seed) {
            this.level = level;
            MaterialEmitters.Source diskSource = random -> {
                double ang = random.nextDouble() * Math.PI * 2.0;
                double rr = this.shockR * Math.sqrt(random.nextDouble());
                return new MaterialEmitters.Sample(new Vec3(
                    this.center.x + Math.cos(ang) * rr, this.center.y + 0.14,
                    this.center.z + Math.sin(ang) * rr), Vec3.ZERO);
            };
            MaterialEmitters.Source hotSource = random -> {
                double ang = random.nextDouble() * Math.PI * 2.0;
                double rr = this.shockR * 0.8f * Math.sqrt(random.nextDouble());
                return new MaterialEmitters.Sample(new Vec3(
                    this.center.x + Math.cos(ang) * rr, this.center.y + 0.16,
                    this.center.z + Math.sin(ang) * rr), Vec3.ZERO);
            };
            MaterialEmitters.Source coreSource = random -> {
                double r = Math.max(0.2, this.shockR * 0.16);
                return new MaterialEmitters.Sample(new Vec3(
                    this.center.x + (random.nextDouble() * 2.0 - 1.0) * r, this.center.y + 0.18,
                    this.center.z + (random.nextDouble() * 2.0 - 1.0) * r), Vec3.ZERO);
            };
            MaterialEmitters.Source columnSource = random -> {
                double ang = random.nextDouble() * Math.PI * 2.0;
                double rr = this.pillarRadius * (0.4 + random.nextDouble());
                return new MaterialEmitters.Sample(new Vec3(
                    this.center.x + Math.cos(ang) * rr,
                    this.center.y + random.nextDouble() * Math.max(0.4, this.pillarHeight),
                    this.center.z + Math.sin(ang) * rr), Vec3.ZERO);
            };
            MaterialEmitters.Source followSource = random -> {
                double ang = random.nextDouble() * Math.PI * 2.0;
                double rr = this.followR * (0.84 + random.nextDouble() * 0.16);
                return new MaterialEmitters.Sample(new Vec3(
                    this.center.x + Math.cos(ang) * rr, this.center.y + 0.12,
                    this.center.z + Math.sin(ang) * rr), Vec3.ZERO);
            };
            this.dustDisk = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(SCORCH_DUST, 1.25f, seed, new Vec3(0, 0.02, 0), 0.05f),
                diskSource, 130f, 220);
            this.flameDisk = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.flame(new Vec3(0, 0.05, 0), 0.04f),
                diskSource, 150f, 260);
            this.plasmaDisk = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(DARK_RED, 1.35f, seed + 7, Vec3.ZERO, 0.02f),
                diskSource, 110f, 190);
            this.hotDisk = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(HOT, 1.05f, seed + 13, Vec3.ZERO, 0.02f),
                hotSource, 85f, 150);
            this.smokeDisk = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.smoke(BRIMSTONE, 1.7f, seed + 3, new Vec3(0, 0.03, 0), 0.03f),
                diskSource, 48f, 90);
            this.core = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(CORE, 1.9f, seed + 19, Vec3.ZERO, 0.03f),
                coreSource, 95f, 160);
            this.columnFlame = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.flame(new Vec3(0, 0.30, 0), 0.05f),
                columnSource, 175f, 300);
            this.columnPlasma = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(HOT, 1.2f, seed + 23, new Vec3(0, 0.22, 0), 0.04f),
                columnSource, 125f, 220);
            this.columnEmber = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.ember(new Vec3(0, 0.35, 0), 0.12f),
                columnSource, 58f, 110);
            this.followDust = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.dust(SCORCH_DUST, 1.1f, seed + 5, new Vec3(0, 0.02, 0), 0.04f),
                followSource, 75f, 140);
        }

        void tick(float age, float diskEnvelope, float pillarEnvelope, float followEnvelope) {
            lastSeenNanos = System.nanoTime();
            if (Float.isNaN(lastAge)) {
                lastAge = age;
                return;
            }
            float delta = Math.min(0.25f, Math.max(0f, (age - lastAge) / 20f));
            lastAge = age;
            if (delta <= 0f) return;
            if (diskEnvelope > 0.01f && shockR > 0.1f) {
                dustDisk.tick(delta, diskEnvelope);
                flameDisk.tick(delta, diskEnvelope);
                plasmaDisk.tick(delta, diskEnvelope);
                hotDisk.tick(delta, diskEnvelope * 0.85f);
                smokeDisk.tick(delta, diskEnvelope * 0.7f);
                core.tick(delta, diskEnvelope);
            }
            if (pillarEnvelope > 0.01f && pillarHeight > 0.15f) {
                columnFlame.tick(delta, pillarEnvelope);
                columnPlasma.tick(delta, pillarEnvelope);
                columnEmber.tick(delta, pillarEnvelope);
            }
            if (followEnvelope > 0.01f) {
                followDust.tick(delta, followEnvelope);
            }
        }
    }

    private static final Map<UUID, Emission> EMISSIONS = new HashMap<>();

    /** Drop emitter sets for slams that have not rendered in a while. */
    private static void pruneEmissions() {
        if (EMISSIONS.size() < 48) return;
        long cutoff = System.nanoTime() - 30_000_000_000L;
        EMISSIONS.values().removeIf(e -> e.lastSeenNanos < cutoff);
    }
}
