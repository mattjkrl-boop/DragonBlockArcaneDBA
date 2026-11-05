package com.dragonblockarcanedba.client.render.ability;

import com.dragonblockarcanedba.ability.AbilityDef;
import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
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
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic Cloud renderer: a domain whose rim ring and translucent
 * shell grow with the same radius formula the server strikes from,
 * ambient forked lightning inside the shell (seeded per time bucket
 * so every client sees the same bolts), the synced telegraph as a
 * narrowing light pillar plus a ground ring at the strike radius,
 * and the strike itself as one thick bolt in the three ticks after
 * it fires. Interior weather is material particles only: decay and
 * atmosphere, never the body.
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
        public int strikeAge = -1;
        public int strikeSeed = 0;
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
    }

    private static final class Emission {
        final Frame frame = new Frame();
        MaterialEmitters.Emitter[] emitters = new MaterialEmitters.Emitter[0];
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
        state.wy = entity.getY();
        state.wz = entity.getZ();
        state.strikeAge = entity.strikeAge();
        state.strikeSeed = entity.strikeSeed();
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
        // Shell: two nested translucent spheres (the ground hides the lower half).
        ComplexGeometry.submitVeil(collector, poseStack, (pose, buffer) -> {
            KiRenderHelper.drawSphere(pose, buffer, (float) radius, 10, 18, midR, midG, midB, env * 0.10f);
        });
        ComplexGeometry.submitVeil(collector, poseStack, (pose, buffer) -> {
            KiRenderHelper.drawSphere(pose, buffer, (float) (radius * 0.8), 8, 14, rimR, rimG, rimB, env * 0.07f);
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
            ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) -> {
                ComplexGeometry.branchedLightning(pose, buffer, boltSeed,
                        new Vec3(sx, sy + height + 5.0, sz), new Vec3(sx, sy + 0.2, sz),
                        0.5f * vis.scale(), vis.core(), vis.mid(), flashEnv, flashEnv, distScale);
                CarrierGeometry.flatRing(pose, buffer, sx, sz, sy + 0.09,
                        cloud.strikeRadius() * 0.4, cloud.strikeRadius() * 1.15, 22,
                        coreR, coreG, coreB, flashEnv * 0.7f);
            });
        }
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
        float delta = emission.lastAge < 0f ? 0.05f
                : Math.min(0.05f, Math.max(0f, (state.ageInTicks - emission.lastAge) / 20.0f));
        emission.lastAge = state.ageInTicks;
        float rateScale = dist <= def.caps().fullDetailDistance() ? 1f : distScale;
        for (MaterialEmitters.Emitter emitter : emission.emitters) {
            emitter.tick(delta, rateScale);
        }
    }

    private Emission buildEmission(ClientLevel level, State state, AbilityDef def) {
        Emission emission = new Emission();
        AbilityDef.VisualSpec vis = def.visual();
        final Frame frame = emission.frame;
        frame.cx = state.wx;
        frame.cy = state.wy;
        frame.cz = state.wz;
        frame.radius = CloudCarrierEntity.radiusAt(def, state.ageInTicks) * vis.scale();
        // Interior weather, sampled against the live frame radius so
        // the shed grows with the domain.
        MaterialEmitters.Source interior = random -> {
            double a = random.nextDouble() * Math.PI * 2.0;
            double rr = frame.radius * Math.sqrt(random.nextDouble());
            Vec3 pos = new Vec3(frame.cx + Math.cos(a) * rr,
                    frame.cy + 0.3 + random.nextDouble() * 1.2,
                    frame.cz + Math.sin(a) * rr);
            return new MaterialEmitters.Sample(pos, new Vec3(0, 0.03, 0));
        };
        float budget = def.caps().maxParticlesPerSecond();
        float groundRate = Math.min(budget * 0.6f, 200f * vis.particleRate());
        float driftRate = Math.min(budget * 0.4f, 110f * vis.particleRate());
        emission.emitters = new MaterialEmitters.Emitter[]{
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.material(vis.particleMaterial(), vis.rim(),
                                3.2f * vis.scale(), state.seed, Vec3.ZERO, 0.02f),
                        interior, groundRate, Math.max(1, Math.round(groundRate))),
                new MaterialEmitters.Emitter(level,
                        MaterialEmitters.Preset.material(vis.particleMaterial(), vis.mid(),
                                2.2f * vis.scale(), state.seed ^ 0x51AB, Vec3.ZERO, 0.02f),
                        interior, driftRate, Math.max(1, Math.round(driftRate)))
        };
        return emission;
    }
}
