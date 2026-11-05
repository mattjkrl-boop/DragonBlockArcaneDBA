package com.dragonblockarcanedba.client.fx;

import com.dragonblockarcanedba.ability.AbilityDef;
import com.dragonblockarcanedba.entity.CloudCarrierEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Domain atmosphere, client side (Phase 5): the Cataclysm pattern
 * of atmosphere as an effect, rebuilt on the Cloud carrier's
 * already-synced state. A cloud whose data carries an atmosphere
 * block tints the render fog and pulls it closer for every client
 * inside its influence. The server sends nothing for this: the
 * carrier's position, age, and definition are already synced, and
 * the radius the fog reads is CloudCarrierEntity.radiusAt, the
 * same formula the strikes and the renderer use, so the fog
 * boundary is the domain boundary.
 *
 * Influence per domain: the lifecycle envelope (FxEnvelope over
 * the carrier's age, so the fog eases in with the domain's growth
 * and out with its end) times a spatial factor: full inside the
 * current radius, feathering to zero at 1.6 times the radius.
 * With several domains live, ONE wins: the highest
 * envelope-times-spatial-times-strength score, so colors never
 * blend into mud; the winner's tint, strength, and density apply
 * whole. A smoothed weight (about a second in, about two and a
 * half seconds out) carries the transitions, including winner
 * changes, so the sky never cuts.
 *
 * FogRendererMixin calls apply() on every FogData the vanilla fog
 * renderer produces; this class only ever touches the fog while
 * the smoothed weight is above zero.
 */
public final class DomainFog {
    private DomainFog() {}

    /** How far past the domain radius the fog influence feathers out, as a radius fraction. */
    private static final double FEATHER = 0.6;
    /** The most the fog distances may shrink at full weight and density. */
    private static final float MAX_SHRINK = 0.85f;
    private static final float RATE_IN = 0.05f;
    private static final float RATE_OUT = 0.02f;

    private static float weight;
    private static float tintR, tintG, tintB;
    private static float strength;
    private static float density = 0.5f;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(DomainFog::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            weight = 0f;
        });
    }

    private static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        float target = 0f;
        if (level != null && mc.player != null) {
            Vec3 viewer = mc.player.position();
            float bestScore = 0f;
            AbilityDef.CloudAtmosphere best = null;
            float bestRaw = 0f;
            for (Entity entity : level.entitiesForRendering()) {
                if (!(entity instanceof CloudCarrierEntity cloud)) {
                    continue;
                }
                AbilityDef def = cloud.def();
                if (def == null || def.cloud() == null || def.cloud().atmosphere() == null) {
                    continue;
                }
                AbilityDef.CloudAtmosphere atmosphere = def.cloud().atmosphere();
                float env = FxEnvelope.envelope(cloud.tickCount, def.phases().active());
                if (env <= 0f) {
                    continue;
                }
                double radiusNow = CloudCarrierEntity.radiusAt(def, cloud.tickCount)
                        * def.visual().scale();
                if (radiusNow <= 0) {
                    continue;
                }
                double dist = viewer.distanceTo(cloud.position());
                double spatial;
                if (dist <= radiusNow) {
                    spatial = 1.0;
                } else {
                    spatial = Math.max(0.0, 1.0 - (dist - radiusNow) / (radiusNow * FEATHER));
                }
                if (spatial <= 0.0) {
                    continue;
                }
                float raw = env * (float) spatial;
                float score = raw * atmosphere.strength();
                if (score > bestScore) {
                    bestScore = score;
                    best = atmosphere;
                    bestRaw = raw;
                }
            }
            if (best != null) {
                target = bestRaw;
                tintR = ((best.tint() >> 16) & 0xFF) / 255f;
                tintG = ((best.tint() >> 8) & 0xFF) / 255f;
                tintB = (best.tint() & 0xFF) / 255f;
                strength = best.strength();
                density = best.density();
            }
        }
        float rate = target > weight ? RATE_IN : RATE_OUT;
        if (weight < target) {
            weight = Math.min(target, weight + rate);
        } else if (weight > target) {
            weight = Math.max(target, weight - rate);
        }
    }

    /** Applies the winning domain's atmosphere to one frame's fog, if any is active. */
    public static void apply(FogData data) {
        if (weight <= 0.002f || data == null) {
            return;
        }
        float shrink = Math.max(0.12f, 1.0f - weight * density * MAX_SHRINK);
        data.environmentalStart *= shrink;
        data.environmentalEnd *= shrink;
        data.renderDistanceStart *= shrink;
        data.renderDistanceEnd *= shrink;
        data.skyEnd *= shrink;
        data.cloudEnd *= shrink;
        float mix = weight * strength;
        if (mix > 0f && data.color != null) {
            data.color.x += (tintR - data.color.x) * mix;
            data.color.y += (tintG - data.color.y) * mix;
            data.color.z += (tintB - data.color.z) * mix;
        }
    }

    /** The current smoothed influence weight, for the harness and future UI. */
    public static float weight() {
        return weight;
    }
}
