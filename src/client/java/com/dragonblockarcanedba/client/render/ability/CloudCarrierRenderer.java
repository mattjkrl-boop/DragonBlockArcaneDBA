package com.dragonblockarcanedba.client.render.ability;

import com.dragonblockarcanedba.ability.AbilityDef;
import com.dragonblockarcanedba.ability.AbilityFxDefaults;
import com.dragonblockarcanedba.client.fx.AbilityFxExpander;
import com.dragonblockarcanedba.client.fx.RecipeEmitter;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.kit.ChainRun;
import com.dragonblockarcanedba.client.render.kit.CrackDecal;
import com.dragonblockarcanedba.client.render.kit.DomeShell;
import com.dragonblockarcanedba.client.render.kit.SpikeCluster;
import com.dragonblockarcanedba.client.render.kit.TornWall;
import com.dragonblockarcanedba.entity.CloudCarrierEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic Cloud renderer: a domain whose rim ring and translucent
 * shell grow with the same radius formula the server strikes from,
 * ambient forked lightning inside the shell (seeded per time bucket
 * so every client sees the same bolts), the synced telegraph as a
 * narrowing light pillar plus a ground ring at the strike radius,
 * and the strike itself as one thick bolt in the three ticks after
 * it fires. Interior weather runs on Phase 3 recipes (the
 * definition's carrier_tick fx, or the shared defaults), expanded
 * locally from the carrier's synced state: decay and atmosphere,
 * never the body.
 */
public class CloudCarrierRenderer extends EntityRenderer<CloudCarrierEntity, CloudCarrierRenderer.State> {

    public CloudCarrierRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class State extends EntityRenderState {
        public int entityId = -1;
        public AbilityDef def;
        public int seed = 0;
        public float power = 1f;
        public double wx = 0, wy = 0, wz = 0;
        public float yaw = 0;
        public int strikeAge = -1;
        public int strikeSeed = 0;
        public int[] latchedIds = new int[0];
        public double sx = 0, sy = 0, sz = 0;
    }

    /** Last telegraph seen per entity, so the strike flash can draw after the synced field clears. */
    private static final class SeenStrike {
        int age = -1;
        int seed = 0;
        double x, y, z;
    }

    private static final class Frame {
        double cx, cy, cz, radius;
        float power = 1f;
    }

    private static final class Emission {
        final Frame frame = new Frame();
        List<RecipeEmitter> emitters = List.of();
        float lastAge = -1f;
    }

    private final Map<Integer, Emission> emissions = new HashMap<>();
    private final Map<Integer, SeenStrike> seenStrikes = new HashMap<>();

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CloudCarrierEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.entityId = entity.getId();
        state.def = entity.def();
        state.seed = entity.getSeed();
        state.power = entity.getPower();
        state.wx = entity.getX();
        state.yaw = entity.getYRot();
        state.wy = entity.getY();
        state.wz = entity.getZ();
        state.strikeAge = entity.strikeAge();
        state.strikeSeed = entity.strikeSeed();
        state.latchedIds = entity.def() != null && entity.def().cloud() != null
                && entity.def().cloud().chains() != null ? entity.latchedIds() : new int[0];
        if (entity.hasPendingStrike()) {
            Vec3 sp = entity.strikePos();
            state.sx = sp.x - entity.getX();
            state.sy = sp.y - entity.getY();
            state.sz = sp.z - entity.getZ();
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        AbilityDef def = state.def;
        if (def != null && def.cloud() != null) {
            float dist = (float) Math.sqrt(state.distanceToCameraSq);
            float distScale = FxEnvelope.distScale(dist);
            float env = FxEnvelope.envelope(state.ageInTicks, def.phases().active()) * distScale;
            if (env > 0.01f) {
                drawDomain(state, def, poseStack, collector, env, distScale);
                drawGroundAndChains(state, def, poseStack, collector, env, dist);
                tickEmission(state, def, dist, distScale);
            }
        }
        super.submit(state, poseStack, collector, cameraState);
    }

    private void drawDomain(State state, AbilityDef def, PoseStack poseStack,
                            SubmitNodeCollector collector, float env, float distScale) {
        AbilityDef.CloudParams cloud = def.cloud();
        AbilityDef.VisualSpec vis = def.visual();
        final double radius = CloudCarrierEntity.radiusAt(def, state.ageInTicks) * vis.scale();
        final double height = cloud.height() * vis.scale();
        final int segments = FxEnvelope.segments(40, distScale, 14);
        final float rimR = CarrierGeometry.r(vis.rim()), rimG = CarrierGeometry.g(vis.rim()), rimB = CarrierGeometry.b(vis.rim());
        final float midR = CarrierGeometry.r(vis.mid()), midG = CarrierGeometry.g(vis.mid()), midB = CarrierGeometry.b(vis.mid());
        final float coreR = CarrierGeometry.r(vis.core()), coreG = CarrierGeometry.g(vis.core()), coreB = CarrierGeometry.b(vis.core());
        final int seed = state.seed;

        // Rim: the silhouette's ground anchor.
        ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) -> {
            CarrierGeometry.flatRing(pose, buffer, 0, 0, 0.08, radius - 0.45, radius + 0.45, segments,
                    rimR, rimG, rimB, env * 0.8f);
            CarrierGeometry.flatRing(pose, buffer, 0, 0, 0.05, radius * 0.90, radius * 0.97, segments,
                    midR, midG, midB, env * 0.22f);
        });
        // Shell: the kit DomeShell, layered hemispheres with
        // scrolling alpha and rim brightening from the geometry
        // block. At the defaults (2 layers, swirl 0, rim glow 0)
        // the layers sit at the old spheres' radii and alphas
        // (mid 0.10 at full radius, rim 0.07 at 0.8), and the
        // buried lower hemisphere is simply no longer emitted.
        AbilityDef.CloudGeometry cloudGeo = def.geometry().cloud();
        final int domeSlices = FxEnvelope.segments(28, distScale, 12);
        ComplexGeometry.submitVeil(collector, poseStack, (pose, buffer) -> {
            DomeShell.draw(pose, buffer, radius, domeSlices, 6, cloudGeo.shellLayers(),
                    (float) cloudGeo.swirlSpeed(), state.ageInTicks,
                    vis.mid(), vis.rim(), vis.core(),
                    (float) cloudGeo.shellAlpha(), (float) cloudGeo.rimGlow(), env);
        });
        // Ambient interior lightning, bucketed so all clients agree.
        if (distScale > 0.3f) {
            final int bucket = (int) (state.ageInTicks / 3.0f);
            final float flicker = 0.55f + 0.45f * (float) Math.sin(state.ageInTicks * 2.7f + bucket);
            ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) -> {
                int bolts = distScale > 0.6f ? 2 : 1;
                for (int k = 0; k < bolts; k++) {
                    ComplexGeometry.Rng rng = new ComplexGeometry.Rng(seed ^ (bucket * 0x9E37L) ^ (k * 7919L));
                    double a0 = rng.nextFloat() * Math.PI * 2.0;
                    double rr0 = radius * (0.15 + 0.5 * rng.nextFloat());
                    Vec3 from = new Vec3(Math.cos(a0) * rr0, height * (0.75 + 0.25 * rng.nextFloat()), Math.sin(a0) * rr0);
                    double a1 = a0 + rng.signed() * 0.5;
                    double rr1 = radius * (0.5 + 0.48 * rng.nextFloat());
                    Vec3 to = new Vec3(Math.cos(a1) * rr1, 0.15, Math.sin(a1) * rr1);
                    ComplexGeometry.branchedLightning(pose, buffer, seed ^ bucket ^ k,
                            from, to, 0.13f * vis.scale(), vis.core(), vis.mid(),
                            env * 0.7f * flicker, env, distScale);
                }
            });
        }

        drawTelegraphAndStrike(state, def, poseStack, collector, env, distScale, height);
    }

    private void drawTelegraphAndStrike(State state, AbilityDef def, PoseStack poseStack,
                                        SubmitNodeCollector collector, float env, float distScale,
                                        double height) {
        AbilityDef.CloudParams cloud = def.cloud();
        AbilityDef.VisualSpec vis = def.visual();
        SeenStrike seen = seenStrikes.computeIfAbsent(state.entityId, k -> new SeenStrike());
        if (state.strikeAge >= 0) {
            seen.age = state.strikeAge;
            seen.seed = state.strikeSeed;
            seen.x = state.sx;
            seen.y = state.sy;
            seen.z = state.sz;
        }
        final float coreR = CarrierGeometry.r(vis.core()), coreG = CarrierGeometry.g(vis.core()), coreB = CarrierGeometry.b(vis.core());
        final float rimR = CarrierGeometry.r(vis.rim()), rimG = CarrierGeometry.g(vis.rim()), rimB = CarrierGeometry.b(vis.rim());

        if (state.strikeAge >= 0) {
            float progress = cloud.telegraphTicks() <= 0 ? 1f
                    : Math.max(0f, Math.min(1f, 1f - (state.strikeAge - state.ageInTicks) / cloud.telegraphTicks()));
            final double sx = state.sx, sy = state.sy, sz = state.sz;
            final double tubeRadius = (0.95 - 0.6 * progress) * vis.scale();
            final float pillarAlpha = env * (0.22f + 0.55f * progress);
            ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) -> {
                CarrierGeometry.tube(pose, buffer, sx, sz, tubeRadius, sy, sy + height,
                        10, coreR, coreG, coreB, pillarAlpha, pillarAlpha * 0.25f);
                CarrierGeometry.flatRing(pose, buffer, sx, sz, sy + 0.07,
                        cloud.strikeRadius() * 0.92, cloud.strikeRadius() * 1.05, 22,
                        rimR, rimG, rimB, env * (0.35f + 0.5f * progress));
            });
        } else if (seen.age >= 0 && state.ageInTicks >= seen.age && state.ageInTicks < seen.age + 3) {
            final float flashEnv = env * (1f - (state.ageInTicks - seen.age) / 3f);
            final double sx = seen.x, sy = seen.y, sz = seen.z;
            final int boltSeed = seen.seed;
            // The two-hue signature from the Tempest picture,
            // opt-in per ability: strike bolts split between the
            // mid and rim hues by the strike's own synced seed,
            // so every observer sees the same strike in the same
            // hue. Off by default; bolts stay in the mid hue.
            final int boltGlow = def.geometry().cloud().strikeHueSplit() && (boltSeed & 1) != 0
                    ? vis.rim() : vis.mid();
            ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) -> {
                ComplexGeometry.branchedLightning(pose, buffer, boltSeed,
                        new Vec3(sx, sy + height + 5.0, sz), new Vec3(sx, sy + 0.2, sz),
                        0.5f * vis.scale(), vis.core(), boltGlow, flashEnv, flashEnv, distScale);
                CarrierGeometry.flatRing(pose, buffer, sx, sz, sy + 0.09,
                        cloud.strikeRadius() * 0.4, cloud.strikeRadius() * 1.15, 22,
                        coreR, coreG, coreB, flashEnv * 0.7f);
            });
        }
    }

    /**
     * The Phase 6 geometry.ground draws at the cloud anchor, plus
     * the chains module's ChainRun links. Ground geometry is hero
     * geometry: it is not thinned by distance, only skipped whole
     * past the caps cull distance. The envelope is the same
     * lifecycle value the domain alpha uses, passed in as env.
     */
    private void drawGroundAndChains(State state, AbilityDef def, PoseStack poseStack,
                                     SubmitNodeCollector collector, float env, float dist) {
        AbilityDef.GroundGeometry ground = def.geometry().ground();
        AbilityDef.CloudParams cloud = def.cloud();
        boolean hasGround = ground.wallSlabs() > 0 || ground.crackCount() > 0 || ground.spikes() > 0;
        boolean hasChains = cloud.chains() != null;
        if (!hasGround && !hasChains) {
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
        AbilityDef.VisualSpec vis = def.visual();
        final double radius = CloudCarrierEntity.radiusAt(def, state.ageInTicks) * vis.scale();
        final int light = state.lightCoords;
        final long seed = state.seed;
        final double wx = state.wx, wy = state.wy, wz = state.wz;
        // Physical rock reading of the weapon palette: the rim
        // darkened hard for the root, the mid less so for the tips.
        final int rockRoot = darken(vis.rim(), 0.45f);
        final int rockTip = darken(vis.mid(), 0.70f);

        if (hasGround) {
            // TornWall samples the ground in this renderer's local
            // frame: world column from the carrier position plus
            // the local offset, height from the client heightmap
            // relative to the carrier's own y.
            TornWall.GroundSampler sampler = (lx, lz) -> level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING,
                    (int) Math.floor(wx + lx), (int) Math.floor(wz + lz)) - wy;
            if (ground.wallSlabs() > 0) {
                // Faces the spawning yaw (wave convention: fwd = (-sin, cos)),
                // so a wall left by a wave stands across that wave's path.
                double yawRad = Math.toRadians(state.yaw);
                final double fwdX = -Math.sin(yawRad), fwdZ = Math.cos(yawRad);
                final double len = 2.0 * radius;
                final double h = ground.wallHeight() * Math.max(env, 0.15f);
                ComplexGeometry.submitSolidLit(collector, poseStack, light, (pose, buffer) -> {
                    TornWall.draw(pose, buffer, seed, fwdX, fwdZ, len, ground.wallArc(), h,
                            ground.wallSlabs(), sampler, rockRoot, rockTip, 1.0f);
                });
            }
            if (ground.crackCount() > 0) {
                final double cr = ground.crackRadius() > 0 ? ground.crackRadius() : radius;
                ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) -> {
                    CrackDecal.draw(pose, buffer, seed, cr, ground.crackCount(), 0.06,
                            vis.core(), vis.rim(), env);
                });
            }
            if (ground.spikes() > 0) {
                final double sr = ground.spikeRadius() > 0 ? ground.spikeRadius() : radius * 0.9;
                final double sh = ground.spikeHeight() * (0.35 + 0.65 * env);
                ComplexGeometry.submitSolidLit(collector, poseStack, light, (pose, buffer) -> {
                    SpikeCluster.draw(pose, buffer, seed, ground.spikes(), sr, sh, 0,
                            rockRoot, rockTip, 1.0f);
                });
            }
        }

        if (hasChains) {
            // The server's latched set, synced as entity ids: a link is
            // drawn from the cloud's anchor to every chained target.
            List<Vec3> targets = new ArrayList<>();
            for (int id : state.latchedIds) {
                net.minecraft.world.entity.Entity e = level.getEntity(id);
                if (!(e instanceof LivingEntity le) || !le.isAlive()) {
                    continue;
                }
                Vec3 rel = new Vec3(e.getX() - wx, e.getY() - wy + e.getBbHeight() * 0.6, e.getZ() - wz);
                if (rel.horizontalDistanceSqr() < 1.0) {
                    continue; // the anchor target itself: its bind model carries it
                }
                targets.add(rel);
            }
            if (!targets.isEmpty()) {
                final Vec3 from = new Vec3(0, cloud.height() * 0.55, 0);
                final float chainAlpha = 0.9f * env;
                ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) -> {
                    for (int i = 0; i < targets.size(); i++) {
                        ChainRun.draw(pose, buffer, seed ^ (i * 0x9E37L), from, targets.get(i),
                                0.8, 0.55, vis.mid(), vis.core(), chainAlpha);
                    }
                });
            }
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
        emission.frame.radius = CloudCarrierEntity.radiusAt(def, state.ageInTicks) * def.visual().scale();
        emission.frame.power = state.power;
        float delta = emission.lastAge < 0f ? 0.05f
                : Math.min(0.05f, Math.max(0f, (state.ageInTicks - emission.lastAge) / 20.0f));
        emission.lastAge = state.ageInTicks;
        // Recipes thin by the caps-driven detail factor: full rate
        // inside fullDetailDistance, the shared falloff beyond, and
        // the cull check above stops them entirely past cull.
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
        frame.radius = CloudCarrierEntity.radiusAt(def, state.ageInTicks) * def.visual().scale();
        frame.power = state.power;
        // Interior weather, sampled against the live frame radius so
        // the shed grows with the domain: the context re-reads the
        // frame on every emitter tick.
        List<AbilityDef.FxRecipe> recipes = AbilityFxDefaults.tickRecipes(def);
        List<RecipeEmitter> emitters = new ArrayList<>();
        for (int i = 0; i < recipes.size(); i++) {
            emitters.add(new RecipeEmitter(level, def, recipes.get(i), state.seed + i * 0x51AB,
                    () -> new AbilityFxExpander.Ctx(
                            new Vec3(frame.cx, frame.cy, frame.cz),
                            new Vec3(0, 1, 0), frame.power, frame.radius)));
        }
        emission.emitters = emitters;
        return emission;
    }
}
