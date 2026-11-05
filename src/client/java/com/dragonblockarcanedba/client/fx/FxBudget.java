package com.dragonblockarcanedba.client.fx;

import com.dragonblockarcanedba.ability.AbilityDef;

import java.util.HashMap;
import java.util.Map;

/**
 * The shared per-second particle budget for ability recipes
 * (Rebuild Phase 3). Every recipe emission on this client, packet
 * burst or local tick, draws from the same two windows:
 *
 * - the ability window: caps.maxParticlesPerSecond from the
 *   definition's data, shared by every recipe and every live
 *   instance of that ability, so an effect at full tilt can never
 *   exceed the number its own data declares;
 * - the cap-group window, when the recipe names a capGroup: every
 *   recipe in the group shares one window whose budget is the
 *   recipe's cap when set, else the ability budget.
 *
 * A recipe's own cap additionally clamps a single burst outright
 * (see burstLimit). Windows are one second, wall clock, matching
 * the MaterialEmitters budget discipline. Budgeting is local
 * presentation: it may clip spawns under load, it never changes
 * what the deterministic expansion computed.
 */
public final class FxBudget {
    private FxBudget() {}

    private static final class Window {
        long startNanos;
        int used;
    }

    private static final Map<String, Window> WINDOWS = new HashMap<>();

    /** How many of {@code want} particles this recipe may spawn right now; consumes them. */
    public static synchronized int consume(AbilityDef def, AbilityDef.FxRecipe recipe, int want) {
        if (want <= 0) {
            return 0;
        }
        int abilityBudget = Math.max(1, def.caps().maxParticlesPerSecond());
        int allowed = room("a:" + def.id(), abilityBudget, want);
        if (recipe.capGroup() != null) {
            int groupBudget = recipe.cap() != null ? recipe.cap() : abilityBudget;
            allowed = Math.min(allowed,
                    room("g:" + def.id() + ":" + recipe.capGroup(), groupBudget, want));
        }
        if (allowed > 0) {
            spend("a:" + def.id(), allowed);
            if (recipe.capGroup() != null) {
                spend("g:" + def.id() + ":" + recipe.capGroup(), allowed);
            }
        }
        return allowed;
    }

    /** The outright per-burst clamp a recipe's cap imposes, before any window. */
    public static int burstLimit(AbilityDef.FxRecipe recipe) {
        return recipe.cap() != null ? recipe.cap() : Integer.MAX_VALUE;
    }

    private static int room(String key, int budget, int want) {
        Window w = window(key);
        return Math.max(0, Math.min(want, budget - w.used));
    }

    private static void spend(String key, int n) {
        window(key).used += n;
    }

    private static Window window(String key) {
        long now = System.nanoTime();
        Window w = WINDOWS.get(key);
        if (w == null) {
            if (WINDOWS.size() > 256) {
                WINDOWS.clear();
            }
            w = new Window();
            w.startNanos = now;
            WINDOWS.put(key, w);
            return w;
        }
        if (now - w.startNanos >= 1_000_000_000L) {
            w.startNanos = now;
            w.used = 0;
        }
        return w;
    }

    public static void clear() {
        WINDOWS.clear();
    }
}
