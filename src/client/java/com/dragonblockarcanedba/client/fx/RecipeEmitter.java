package com.dragonblockarcanedba.client.fx;

import com.dragonblockarcanedba.ability.AbilityDef;
import com.dragonblockarcanedba.config.DbaEffectsConfig;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.function.Supplier;

/**
 * A continuous recipe emitter (Rebuild Phase 3): the carrier_tick
 * and charge_tick counterpart of MaterialEmitters.Emitter, driven
 * by a data recipe instead of a hardcoded preset. A frame-rate
 * independent accumulator turns the recipe's rate into spawns; each
 * spawn is one deterministic AbilityFxExpander.expandOne call with
 * a cursor-advanced seed, clipped by the shared FxBudget. The
 * context (origin, direction, power, radius) is re-read from the
 * supplier every tick, so the emitter follows its moving carrier.
 *
 * Rates scale by the definition's particleRate, the effects
 * density config, the cast power, and the caller's caps-driven
 * detail factor, the same scaling stack the Phase 1 emitters used.
 */
public final class RecipeEmitter {
    private final ClientLevel level;
    private final AbilityDef def;
    private final AbilityDef.FxRecipe recipe;
    private final int seedBase;
    private final Supplier<AbilityFxExpander.Ctx> ctxSource;
    private float accumulator = 0f;
    private int cursor = 0;

    public RecipeEmitter(ClientLevel level, AbilityDef def, AbilityDef.FxRecipe recipe,
                         int seedBase, Supplier<AbilityFxExpander.Ctx> ctxSource) {
        this.level = level;
        this.def = def;
        this.recipe = recipe;
        this.seedBase = seedBase;
        this.ctxSource = ctxSource;
    }

    public int tick(float deltaSeconds, float detailFactor) {
        if (level == null || deltaSeconds <= 0f || detailFactor <= 0.01f) {
            return 0;
        }
        AbilityFxExpander.Ctx ctx = ctxSource.get();
        if (ctx == null) {
            return 0;
        }
        float powerScale = 0.4f + 0.6f * ctx.power();
        float rate = (float) recipe.rate() * def.visual().particleRate()
                * DbaEffectsConfig.effectDensity() * powerScale;
        accumulator += rate * deltaSeconds * detailFactor;
        int want = (int) Math.floor(accumulator);
        accumulator -= want;
        if (want <= 0) {
            return 0;
        }
        if (want > 32) {
            want = 32;
            accumulator = 0f; // never build emission debt
        }
        int spawned = 0;
        for (int i = 0; i < want; i++) {
            int seed = seedBase + cursor * 1013;
            cursor++;
            if (FxBudget.consume(def, recipe, 1) < 1) {
                accumulator = 0f;
                break;
            }
            AbilityFxExpander.spawn(level, AbilityFxExpander.expandOne(def, recipe, ctx, seed));
            spawned++;
        }
        return spawned;
    }
}
