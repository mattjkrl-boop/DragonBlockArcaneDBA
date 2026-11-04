package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.entity.AzureLightningEntity;
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

import java.util.HashMap;
import java.util.Map;

/**
 * Telegraph renderer for AzureLightningEntity.
 *
 * Complex pass: the warning is a layered composition. The pieces layer
 * is a crackFan electric web crawling outward inside the warned disc
 * plus a tendrilFan of small arcs during the telegraph, both drawn
 * through the shader submit helpers. The complex particle layer is
 * SPARK_STREAK sprites rising in the telegraph. The material emission
 * from the no-shapes pass stays as filler: lightning plasma motes
 * gather across the warned disc at exactly the strike radius (the
 * warning's size still equals the hit's size), a brighter stream
 * converges toward the center as the strike nears, vapor hangs low
 * over the disc, and a thin stream of plasma rises along the charge
 * path. The crack web is scaled to 0.78 of the disc radius so its
 * seeded wander and step overshoot stay inside the warned disc and
 * the boundary read stays exact. After the strike tick, the web cools
 * with the disc as fading plasma and vapor over the afterglow window.
 * The bolt itself stays the shared BoltEntity's job. All emission and
 * every piece seed take the lightning entity's synced render seed.
 */
public class AzureLightningRenderer extends EntityRenderer<AzureLightningEntity, AzureLightningRenderer.AzureLightningRenderState> {

    public AzureLightningRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class AzureLightningRenderState extends EntityRenderState {
        public boolean isRare;
        public float age;
        public long seed;
        public int telegraphTicks = 12;
        public float radius = 3.5f;
        public double wx, wy, wz;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(AzureLightningEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(AzureLightningEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public AzureLightningRenderState createRenderState() {
        return new AzureLightningRenderState();
    }

    @Override
    public void extractRenderState(AzureLightningEntity entity, AzureLightningRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.isRare = entity.isRare();
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getRenderSeed();
        state.telegraphTicks = entity.getTelegraphTicks();
        state.radius = entity.getRadius();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    /** Mutable per-frame emission data read by the sources. Data only. */
    private static final class Frame {
        Vec3 center = Vec3.ZERO;
        float radius = 3.5f;
        float convRadius = 3.5f;
    }

    private static final class Emission {
        final Frame frame = new Frame();
        ClientLevel level;
        MaterialEmitters.Emitter warning;
        MaterialEmitters.Emitter converge;
        MaterialEmitters.Emitter column;
        MaterialEmitters.Emitter vapor;
        MaterialEmitters.Emitter sparks;
        float lastAge = -1f;
    }

    private final Map<Long, Emission> emissions = new HashMap<>();

    /** Points across a disc at the frame's warning or converging radius. */
    private static MaterialEmitters.Source disc(Frame frame, boolean converging) {
        return random -> {
            double radius = converging ? frame.convRadius : frame.radius;
            double ang = random.nextDouble() * Math.PI * 2.0;
            double rr = radius * Math.sqrt(random.nextDouble());
            Vec3 pos = new Vec3(
                    frame.center.x + Math.cos(ang) * rr,
                    frame.center.y + 0.06 + random.nextDouble() * 0.08,
                    frame.center.z + Math.sin(ang) * rr);
            return new MaterialEmitters.Sample(pos, Vec3.ZERO);
        };
    }

    private Emission buildEmission(ClientLevel level, AzureLightningRenderState state) {
        Emission emission = new Emission();
        emission.level = level;
        Frame frame = emission.frame;
        frame.center = new Vec3(state.wx, state.wy, state.wz);
        frame.radius = state.radius;
        frame.convRadius = state.radius;
        int seed = (int) state.seed;
        // Theme colors: azure for a normal strike, pale gold for a rare one.
        int main = state.isRare ? 0xFFD973 : 0x26D9FF;
        int vaporColor = state.isRare ? 0xF2E3C2 : 0x9FD8E8;
        Vec3 top = new Vec3(state.wx, state.wy + 30.0, state.wz);
        emission.warning = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(main, 1.25f, seed ^ 0x12A0,
                        Vec3.ZERO, 0.01f),
                disc(frame, false), 170f, 240);
        emission.converge = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xFFFFFF, 0.95f, seed ^ 0xC011,
                        Vec3.ZERO, 0.01f),
                disc(frame, true), 110f, 160);
        emission.column = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(main, 1.05f, seed ^ 0xBEAA,
                        Vec3.ZERO, 0.02f),
                MaterialEmitters.path(frame.center, top, 0.30), 130f, 200);
        emission.vapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(vaporColor, 2.2f, seed ^ 0x9A90,
                        new Vec3(0.0, 0.02, 0.0), 0.02f),
                disc(frame, false), 42f, 70);
        emission.sparks = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK,
                        new Vec3(0.0, 0.28, 0.0), 0.02f),
                disc(frame, false), 26f, 40);
        return emission;
    }

    @Override
    public void submit(AzureLightningRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float age = state.age;
        float strikeAt = Math.max(1.0f, state.telegraphTicks);
        float radius = state.radius;
        if (age >= strikeAt + AzureLightningEntity.AFTERGLOW_TICKS) return;

        Vec3 center = new Vec3(state.wx, state.wy, state.wz);
        float ds = MaterialEmitters.distanceScale(center);

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc != null ? mc.level : null;
        if (level == null || ds <= 0.01f) return;

        Emission emission = emissions.get(state.seed);
        if (emission == null || emission.level != level || age < emission.lastAge - 1.0f) {
            emission = buildEmission(level, state);
            emissions.put(state.seed, emission);
            if (emissions.size() > 64) emissions.clear();
        }
        emission.frame.center = center;
        emission.frame.radius = radius;
        float delta = emission.lastAge < 0f ? 0.05f
                : Math.min(0.05f, Math.max(0.0f, (age - emission.lastAge) / 20.0f));
        emission.lastAge = age;

        int main = state.isRare ? 0xFFD973 : 0x26D9FF;
        if (age < strikeAt) {
            // Telegraph phase: t runs 0 to 1 across the warning window.
            float t = FxEnvelope.smoothstep(age / strikeAt);
            // The warning pulses faster as the strike closes in.
            float blink = (Math.sin(age * (0.6 + t * 1.6)) > 0.0) ? 1.0f : 0.72f;
            emission.frame.convRadius = radius * (1.0f - t) + 0.35f;
            emission.warning.tick(delta, (0.35f + 0.55f * t) * blink * ds);
            emission.converge.tick(delta, 0.55f * t * blink * ds);
            emission.column.tick(delta, (0.10f + 0.28f * t) * blink * ds);
            emission.vapor.tick(delta, 0.25f * blink * ds);
            emission.sparks.tick(delta, (0.30f + 0.70f * t) * ds);

            // Pieces layer: the electric web crawls outward inside the
            // warned disc as the strike nears. Its radius is capped at
            // 0.78 of the disc so the seeded walk stays inside the
            // boundary the hit will use. The geometry blink is gentler
            // than the emission blink so the boundary never strobes.
            final float webRadius = radius * 0.78f * (0.45f + 0.55f * t);
            final float geoEnv = (0.30f + 0.70f * t) * (blink > 0.9f ? 1.0f : 0.88f) * ds;
            final long crackSeed = state.seed ^ 0xC4ACL;
            ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) ->
                    ComplexGeometry.crackFan(pose, buffer, crackSeed, webRadius,
                            0xFFFFFF, main, 0.92f, geoEnv, ds));
            // Small arcs lashing up from the disc, reseeded every two
            // ticks so they jump like real discharge while every client
            // draws the same arc in the same tick window.
            final long arcSeed = state.seed ^ 0xA2C5L ^ ((long) (age / 2.0f) * 0x9E37L);
            final float arcLen = radius * 0.55f;
            ComplexGeometry.submitEnergy(collector, poseStack, ComplexTextures.TENDRIL, (pose, buffer) ->
                    ComplexGeometry.tendrilFan(pose, buffer, arcSeed, new Vec3(0.0, 0.12, 0.0),
                            new Vec3(0.0, 1.0, 0.0), 65f, arcLen, 3, 0.13f,
                            main, 0xFFFFFF, 0.90f, geoEnv, ds));
        } else {
            // Afterglow phase: the struck circle cools and fades over 8 ticks.
            float k = (age - strikeAt) / (float) AzureLightningEntity.AFTERGLOW_TICKS;
            float fade = 1.0f - FxEnvelope.smoothstep(k);
            emission.warning.tick(delta, 0.55f * fade * ds);
            emission.vapor.tick(delta, 0.20f * fade * ds);
            // The web stays drawn at full crawl while it cools, so the
            // struck area reads as spent cracks, not a vanishing warning.
            final float geoEnv = fade * ds;
            if (geoEnv > 0.01f) {
                final float webRadius = radius * 0.78f;
                final long crackSeed = state.seed ^ 0xC4ACL;
                ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) ->
                        ComplexGeometry.crackFan(pose, buffer, crackSeed, webRadius,
                                0xFFFFFF, main, 0.85f, geoEnv, ds));
            }
        }
    }
}
