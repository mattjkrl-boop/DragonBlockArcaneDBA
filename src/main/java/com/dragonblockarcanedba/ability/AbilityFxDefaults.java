package com.dragonblockarcanedba.ability;

import com.dragonblockarcanedba.particle.AbilityFxEntries;

import java.util.List;

/**
 * The baseline carrier_tick recipes every carrier runs when its
 * ability data carries no fx block of its own (Phase 3). These are
 * the Phase 1 renderer emissions translated onto the universal
 * particle: the wave's band shed, the cloud's two interior weather
 * layers, the projectile's head cluster, wake, and streak shed.
 * The moment data authors a carrier_tick recipe for an ability,
 * the data replaces these defaults wholesale for that carrier (see
 * tickRecipes below); nothing ever runs both.
 *
 * Numbers mirror the Phase 1 emitters before their per-definition
 * scaling: rates here are base rates, and the client emitter
 * applies the definition's particleRate and the effects density
 * config on top, exactly as the Phase 1 emitters did. Sizes are
 * universal-particle quad sizes, matched to what the material
 * profiles produced (profile size times the preset scale).
 */
public final class AbilityFxDefaults {
    private AbilityFxDefaults() {}

    /** The carrier_tick recipes a carrier actually runs: the data's own, or the defaults. */
    public static List<AbilityDef.FxRecipe> tickRecipes(AbilityDef def) {
        List<AbilityDef.FxRecipe> authored = def.fxFor(AbilityDef.FxEvent.CARRIER_TICK);
        return authored.isEmpty() ? carrierTick(def) : authored;
    }

    public static List<AbilityDef.FxRecipe> carrierTick(AbilityDef def) {
        return switch (def.carrier()) {
            case WAVE -> List.of(recipe(AbilityFxEntries.SOFT_MOTE, "mid",
                    AbilityDef.FxShape.BAND, 260, 0.05, 0.03,
                    0.30, 0.45, 1.0, 10, 22, -0.004, 0.90, null));
            case CLOUD -> List.of(
                    recipe(AbilityFxEntries.SMOKE_PUFF, "rim",
                            AbilityDef.FxShape.DISC, 200, 0.03, 0.02,
                            0.85, 1.25, 2.0, 34, 72, -0.012, 0.94, null),
                    recipe(AbilityFxEntries.SOFT_MOTE, "mid",
                            AbilityDef.FxShape.DISC, 110, 0.03, 0.02,
                            0.30, 0.45, 1.1, 20, 44, -0.008, 0.95, null));
            case PROJECTILE -> List.of(
                    recipe(AbilityFxEntries.SOFT_MOTE, "mid",
                            AbilityDef.FxShape.POINT, 300, -0.03, 0.02,
                            0.28, 0.40, 1.0, 10, 22, -0.004, 0.90, null),
                    recipe(AbilityFxEntries.SOFT_MOTE, "core",
                            AbilityDef.FxShape.BAND, 200, 0.02, 0.02,
                            0.20, 0.32, 1.0, 10, 20, -0.004, 0.92, null),
                    recipe(AbilityFxEntries.SPARK_STREAK, "core",
                            AbilityDef.FxShape.POINT, 24, 0.05, 0.12,
                            0.13, 0.18, 0.45, 5, 10, 0.22, 0.88, 40));
            // Beam, swarm and carrierless abilities have no default tick fx:
            // their renderers draw the body, and data authors any recipes
            case BEAM, SWARM, NONE -> List.of();
        };
    }

    private static AbilityDef.FxRecipe recipe(int entry, String colorRef, AbilityDef.FxShape shape,
                                              double rate, double speed, double spread,
                                              double sizeMin, double sizeMax, double sizeEndScale,
                                              int lifetimeMin, int lifetimeMax,
                                              double gravity, double drag, Integer cap) {
        return new AbilityDef.FxRecipe(AbilityDef.FxEvent.CARRIER_TICK, entry, colorRef, null,
                shape, 0, rate, -1, speed, spread, 35,
                sizeMin, sizeMax, sizeEndScale, lifetimeMin, lifetimeMax,
                gravity, drag, 1.0, cap, null);
    }
}
