package com.dragonblockarcanedba.client.render.ability;

import com.dragonblockarcanedba.ability.AbilityDef;
import com.dragonblockarcanedba.ability.AbilityFxDefaults;
import com.dragonblockarcanedba.client.fx.AbilityFxExpander;
import com.dragonblockarcanedba.client.fx.RecipeEmitter;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.kit.CrackDecal;
import com.dragonblockarcanedba.client.render.kit.CrescentBand;
import com.dragonblockarcanedba.client.render.kit.TornWall;
import com.dragonblockarcanedba.entity.WaveCarrierEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic Wave renderer, parameterized entirely by the ability
 * definition: a ground-anchored arc band in three stacked strips
 * (rim at the ground, mid body, white-hot core band, fading top),
 * a ground ring at its base, and a decay shed along the band. The
 * shed runs on Phase 3 recipes (the definition's carrier_tick fx,
 * or the shared defaults), expanded locally from the carrier's
 * synced state; geometry is the hero, the particles only
 * dissipate. Since Phase 4 the band itself is the kit's
 * CrescentBand primitive, driven by the definition's geometry
 * block (turbulence for a torn wall, baseRing toggle for the
 * ground ring); at the defaults it emits the Phase 1 band
 * exactly.
 */
public class WaveCarrierRenderer extends EntityRenderer<WaveCarrierEntity, WaveCarrierRenderer.State> {

    public WaveCarrierRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class State extends EntityRenderState {
        public int entityId = -1;
        public AbilityDef def;
        public int seed = 0;
        public float power = 1f;
        public float yRot = 0f;
        public double wx = 0, wy = 0, wz = 0;
    }

    /** Mutable per-frame anchor for the shed context; the wave travels, so the context must follow. */
    private static final class Frame {
        double cx, cy, cz, yRot;
        float power = 1f;
        double radiusHint = 1.0;
    }

    private static final class Emission {
        final Frame frame = new Frame();
        List<RecipeEmitter> emitters = List.of();
        float lastAge = -1f;
    }

    private final Map<Integer, Emission> emissions = new HashMap<>();

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(WaveCarrierEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.entityId = entity.getId();
        state.def = entity.def();
        state.seed = entity.getSeed();
        state.power = entity.getPower();
        state.yRot = entity.getYRot();
        state.wx = entity.getX();
        state.wy = entity.getY();
        state.wz = entity.getZ();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        AbilityDef def = state.def;
        if (def != null && def.wave() != null) {
            float dist = (float) Math.sqrt(state.distanceToCameraSq);
            float distScale = FxEnvelope.distScale(dist);
            float env = FxEnvelope.envelope(state.ageInTicks, def.phases().active()) * distScale;
            if (env > 0.01f) {
                drawBand(state, def, poseStack, collector, env, distScale);
                drawGround(state, def, poseStack, collector, env, dist);
                tickEmission(state, def, dist, distScale);
            }
        }
        super.submit(state, poseStack, collector, cameraState);
    }

    private void drawBand(State state, AbilityDef def, PoseStack poseStack,
                          SubmitNodeCollector collector, float env, float distScale) {
        AbilityDef.WaveParams wave = def.wave();
        AbilityDef.VisualSpec vis = def.visual();
        double scale = vis.scale() * (0.55 + 0.45 * state.power);
        final double radius = wave.radius() * scale;
        final double height = wave.height() * vis.scale();
        final double arc = Math.toRadians(wave.arcDegrees());
        double yawRad = Math.toRadians(state.yRot);
        final double fwdX = -Math.sin(yawRad);
        final double fwdZ = Math.cos(yawRad);
        final int segments = FxEnvelope.segments(30, distScale, 10);

        final float[] rimB = {CarrierGeometry.r(vis.rim()), CarrierGeometry.g(vis.rim()), CarrierGeometry.b(vis.rim()), env * 0.75f};
        final AbilityDef.WaveGeometry waveGeo = def.geometry().wave();
        final float turbulence = waveGeo.turbulence();

        ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) -> {
            CrescentBand.draw(pose, buffer, state.seed, fwdX, fwdZ, radius, 0, height, arc, segments,
                    vis.rim(), vis.mid(), vis.core(), env, turbulence);
        });
        if (waveGeo.baseRing()) {
            ComplexGeometry.submitVeil(collector, poseStack, (pose, buffer) -> {
                CarrierGeometry.flatRing(pose, buffer, 0, 0, 0.06, radius * 0.96, radius * 1.10, segments,
                        rimB[0], rimB[1], rimB[2], env * 0.45f);
            });
        }
    }

    /**
     * The Phase 6 geometry.ground draws at the wave anchor: a
     * TornWall laid across the wave's path and a CrackDecal scar at
     * its base. Waves draw no spikes. Ground geometry is hero
     * geometry: skipped whole past the caps cull distance, never
     * thinned. The wall lasts while the wave lives; it is a drawn
     * wall, not a lingering physical one.
     */
    private void drawGround(State state, AbilityDef def, PoseStack poseStack,
                            SubmitNodeCollector collector, float env, float dist) {
        AbilityDef.GroundGeometry ground = def.geometry().ground();
        if (ground.wallSlabs() <= 0 && ground.crackCount() <= 0) {
            return;
        }
        if (dist > def.caps().cullDistance()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc != null ? mc.level : null;
        if (level == null) {
            return;
        }
        AbilityDef.WaveParams wave = def.wave();
        AbilityDef.VisualSpec vis = def.visual();
        double scale = vis.scale() * (0.55 + 0.45 * state.power);
        final double radius = wave.radius() * scale;
        // Local frame, matching drawBand and CrescentBand: travel
        // is fwd = (-sin yaw, cos yaw), the band centered on it.
        // TornWall lays its wall along the perpendicular of the
        // forward it is given, so passing travel forward stands
        // the wall across the wave's path, bowed toward travel.
        double yawRad = Math.toRadians(state.yRot);
        final double fwdX = -Math.sin(yawRad);
        final double fwdZ = Math.cos(yawRad);
        final long seed = state.seed;
        final int light = state.lightCoords;
        final double wx = state.wx, wy = state.wy, wz = state.wz;
        final int rockRoot = darken(vis.rim(), 0.45f);
        final int rockTip = darken(vis.mid(), 0.70f);

        if (ground.wallSlabs() > 0) {
            TornWall.GroundSampler sampler = (lx, lz) -> level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING,
                    (int) Math.floor(wx + lx), (int) Math.floor(wz + lz)) - wy;
            final double chord = 2.0 * radius * Math.sin(Math.toRadians(wave.arcDegrees()) * 0.5);
            final double h = ground.wallHeight() * env;
            ComplexGeometry.submitSolidLit(collector, poseStack, light, (pose, buffer) -> {
                TornWall.draw(pose, buffer, seed, fwdX, fwdZ, chord, ground.wallArc(), h,
                        ground.wallSlabs(), sampler, rockRoot, rockTip, 1.0f);
            });
        }
        if (ground.crackCount() > 0) {
            final double cr = ground.crackRadius() > 0 ? ground.crackRadius() : radius;
            final float crackAlpha = env * 0.7f;
            ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) -> {
                CrackDecal.draw(pose, buffer, seed, cr, ground.crackCount(), 0.06,
                        vis.core(), vis.rim(), crackAlpha);
            });
        }
    }

    /** Scales an ARGB color's RGB channels by the factor, alpha forced opaque. */
    private static int darken(int argb, float factor) {
        int r = Math.min(255, Math.round(((argb >> 16) & 0xFF) * factor));
        int g = Math.min(255, Math.round(((argb >> 8) & 0xFF) * factor));
        int b = Math.min(255, Math.round((argb & 0xFF) * factor));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private void tickEmission(State state, AbilityDef def, float dist, float distScale) {
        if (dist > def.caps().cullDistance()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc != null ? mc.level : null;
        if (level == null) {
            return;
        }
        Emission emission = emissions.get(state.entityId);
        if (emission == null || state.ageInTicks < emission.lastAge - 1.0f) {
            emission = buildEmission(level, state, def);
            emissions.put(state.entityId, emission);
            if (emissions.size() > 64) {
                emissions.clear();
            }
        }
        emission.frame.cx = state.wx;
        emission.frame.cy = state.wy;
        emission.frame.cz = state.wz;
        emission.frame.yRot = state.yRot;
        emission.frame.power = state.power;
        emission.frame.radiusHint = def.wave().radius() * def.visual().scale();
        float delta = emission.lastAge < 0f ? 0.05f
                : Math.min(0.05f, Math.max(0f, (state.ageInTicks - emission.lastAge) / 20.0f));
        emission.lastAge = state.ageInTicks;
        float detail = AbilityFxExpander.detailFactor(def, dist);
        for (RecipeEmitter emitter : emission.emitters) {
            emitter.tick(delta, detail);
        }
    }

    private Emission buildEmission(ClientLevel level, State state, AbilityDef def) {
        Emission emission = new Emission();
        final Frame frame = emission.frame;
        frame.cx = state.wx;
        frame.cy = state.wy;
        frame.cz = state.wz;
        frame.yRot = state.yRot;
        frame.power = state.power;
        frame.radiusHint = def.wave().radius() * def.visual().scale();
        List<AbilityDef.FxRecipe> recipes = AbilityFxDefaults.tickRecipes(def);
        List<RecipeEmitter> emitters = new ArrayList<>();
        for (int i = 0; i < recipes.size(); i++) {
            emitters.add(new RecipeEmitter(level, def, recipes.get(i), state.seed + i * 0x51AB,
                    () -> {
                        double yawRad = Math.toRadians(frame.yRot);
                        return new AbilityFxExpander.Ctx(
                                new Vec3(frame.cx, frame.cy, frame.cz),
                                new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad)),
                                frame.power, frame.radiusHint);
                    }));
        }
        emission.emitters = emitters;
        return emission;
    }
}
