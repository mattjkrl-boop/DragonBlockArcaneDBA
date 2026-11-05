package com.dragonblockarcanedba.client.ability;

import com.dragonblockarcanedba.ability.AbilityDef;
import com.dragonblockarcanedba.ability.AbilityRegistry;
import com.dragonblockarcanedba.client.fx.AbilityFxExpander;
import com.dragonblockarcanedba.client.fx.FxBudget;
import com.dragonblockarcanedba.client.fx.RecipeEmitter;
import com.dragonblockarcanedba.config.DbaEffectsConfig;
import com.dragonblockarcanedba.network.AbilityFxPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client side of the seeded effect protocol (Rebuild Phase 3).
 *
 * Packet route: an AbilityFxPayload names an ability, a burst
 * event, an origin, a seed. This class resolves the definition
 * from the synced registry, folds the caps-driven distance factor,
 * the effects density, the definition's particleRate, and the cast
 * power into a count scale, asks the shared FxBudget how much of
 * the burst survives, and expands the rest through
 * AbilityFxExpander: the same expansion every other client runs
 * for the same packet.
 *
 * Charge route: charge_tick recipes never travel. A client tick
 * walks the level's players, reads who is winding up from the
 * Phase 2 cast store (CastAnimations, fed by the server's cast
 * sync), and runs one RecipeEmitter per charging player and
 * recipe, its rate ramping with the charge fraction. This is what
 * replaced the Phase 1 server-sent charge motes: the server now
 * sends no charge particles at all.
 */
public final class ClientAbilityFx {
    private ClientAbilityFx() {}

    private static final Map<Integer, List<RecipeEmitter>> CHARGE_EMITTERS = new HashMap<>();
    private static final Map<Integer, Identifier> CHARGE_DEF = new HashMap<>();

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(AbilityFxPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> handle(payload));
        });
        ClientTickEvents.END_CLIENT_TICK.register(ClientAbilityFx::tickCharges);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            CHARGE_EMITTERS.clear();
            CHARGE_DEF.clear();
            FxBudget.clear();
        });
    }

    private static void handle(AbilityFxPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc != null ? mc.level : null;
        if (level == null || mc.player == null) {
            return;
        }
        Identifier id = Identifier.tryParse(payload.abilityId());
        AbilityDef def = id == null ? null : AbilityRegistry.get(id);
        if (def == null) {
            return;
        }
        AbilityDef.FxEvent[] events = AbilityDef.FxEvent.values();
        if (payload.event() < 0 || payload.event() >= events.length) {
            return;
        }
        AbilityDef.FxEvent event = events[payload.event()];
        List<AbilityDef.FxRecipe> recipes = def.fxFor(event);
        if (recipes.isEmpty()) {
            return;
        }
        Vec3 origin = new Vec3(payload.ox(), payload.oy(), payload.oz());
        double dist = mc.player.position().distanceTo(origin);
        float detail = AbilityFxExpander.detailFactor(def, dist);
        if (detail <= 0f) {
            return;
        }
        float countScale = detail * DbaEffectsConfig.effectDensity()
                * def.visual().particleRate() * (0.4f + 0.6f * payload.power());
        AbilityFxExpander.Ctx ctx = new AbilityFxExpander.Ctx(origin,
                new Vec3(payload.dx(), payload.dy(), payload.dz()),
                payload.power(), AbilityFxExpander.defaultRadiusHint(def, event));
        int repeat = Math.max(1, payload.repeat());
        for (AbilityDef.FxRecipe recipe : recipes) {
            int raw = Math.round(recipe.count() * countScale);
            if (raw <= 0) {
                continue;
            }
            for (int r = 0; r < repeat; r++) {
                int allowed = FxBudget.consume(def, recipe,
                        Math.min(raw, FxBudget.burstLimit(recipe)));
                if (allowed <= 0) {
                    break;
                }
                for (AbilityFxExpander.Spawn spawn : AbilityFxExpander.expandBurst(
                        def, recipe, ctx, payload.seed() + r, countScale, allowed)) {
                    AbilityFxExpander.spawn(level, spawn);
                }
            }
        }
    }

    private static void tickCharges(Minecraft client) {
        ClientLevel level = client.level;
        if (level == null) {
            if (!CHARGE_EMITTERS.isEmpty()) {
                CHARGE_EMITTERS.clear();
                CHARGE_DEF.clear();
            }
            return;
        }
        long gameTime = level.getGameTime();
        for (Player player : level.players()) {
            CastAnimations.ChargeInfo info = CastAnimations.chargeInfo(player.getId(), gameTime, 0f);
            if (info == null) {
                CHARGE_EMITTERS.remove(player.getId());
                CHARGE_DEF.remove(player.getId());
                continue;
            }
            AbilityDef def = info.def();
            List<AbilityDef.FxRecipe> recipes = def.fxFor(AbilityDef.FxEvent.CHARGE_TICK);
            if (recipes.isEmpty()) {
                continue;
            }
            List<RecipeEmitter> emitters = CHARGE_EMITTERS.get(player.getId());
            if (emitters == null || !def.id().equals(CHARGE_DEF.get(player.getId()))) {
                emitters = new java.util.ArrayList<>();
                for (int i = 0; i < recipes.size(); i++) {
                    final Player p = player;
                    final AbilityDef d = def;
                    RecipeEmitter emitter = new RecipeEmitter(level, def, recipes.get(i),
                            player.getId() * 31 + i * 7919,
                            () -> {
                                CastAnimations.ChargeInfo live =
                                        CastAnimations.chargeInfo(p.getId(), level.getGameTime(), 0f);
                                if (live == null) {
                                    return null;
                                }
                                return new AbilityFxExpander.Ctx(
                                        p.position().add(0, 1.0, 0), p.getLookAngle(),
                                        live.power(),
                                        AbilityFxExpander.defaultRadiusHint(d,
                                                AbilityDef.FxEvent.CHARGE_TICK));
                            });
                    emitters.add(emitter);
                }
                CHARGE_EMITTERS.put(player.getId(), emitters);
                CHARGE_DEF.put(player.getId(), def.id());
                if (CHARGE_EMITTERS.size() > 64) {
                    CHARGE_EMITTERS.clear();
                    CHARGE_DEF.clear();
                    return;
                }
            }
            double dist = client.player == null ? 0
                    : client.player.position().distanceTo(player.position());
            // The charge ramp rides in the detail slot: a fresh
            // charge sheds at a third of its rate, a full one at
            // the whole thing, on top of the distance factor.
            float detail = AbilityFxExpander.detailFactor(def, dist)
                    * (0.35f + 0.65f * info.frac());
            for (RecipeEmitter emitter : emitters) {
                emitter.tick(1.0f / 20.0f, detail);
            }
        }
    }
}
