package com.dragonblockarcanedba.client.ability;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import com.dragonblockarcanedba.ability.AbilityDef;
import com.dragonblockarcanedba.ability.AbilityRegistry;
import com.dragonblockarcanedba.client.render.animation.BedrockAnimationRegistry;
import com.dragonblockarcanedba.network.CastSyncPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client store and phase sampler for ability cast poses (Phase 2).
 * The server decides everything: AbilityService broadcasts cast
 * start, fire, and cancel (CastSyncPayload) on its own game-time
 * clock, and every client, the caster included, reconstructs the
 * same pose timeline locally from the synced definition. There is
 * no client prediction; a pose begins when the server's start
 * signal arrives, exactly like the carrier spawn does.
 *
 * The timeline is the research's phase model mapped onto one
 * Bedrock clip per animation id, Spell Engine style: the clip is
 * stretched or compressed so its strike frame (animation.strikeAt)
 * lands on the fire tick.
 * - Startup: the wind-up gear. With a charge clip, that clip plays
 *   across startup on a smoothstep, so the gather deepens as the
 *   cast's power curve grows. Without one, the release clip's own
 *   segment before the strike frame plays across startup.
 * - Fire tick: the strike keyframe. The carrier spawns and the
 *   fire sound plays on this same server tick.
 * - After the strike: the rest of the clip plays across followTicks
 *   plus recovery on a cubic ease-out, fast at the gear change and
 *   settling into the recovery, the second gear. A channel
 *   animation instead holds just past its strike frame for the
 *   whole active phase, then settles across recovery.
 */
public final class CastAnimations {
    private CastAnimations() {}

    /** A resolved clip and the clip-local time to sample it at. */
    public record PoseSample(BedrockAnimationRegistry.BedrockAnimation clip, float timeSeconds) {}

    /** A cast still winding up, with the charge read for visuals. */
    public record ChargeInfo(AbilityDef def, float power, float frac) {}

    private static final class Entry {
        final AbilityDef def;
        final long startTick;
        long fireTick = -1;

        Entry(AbilityDef def, long startTick) {
            this.def = def;
            this.startTick = startTick;
        }
    }

    private static final Map<Integer, Entry> CASTS = new ConcurrentHashMap<>();
    private static final Set<String> WARNED_MISSING = ConcurrentHashMap.newKeySet();

    /** A held charge whose cancel was somehow lost still ends. */
    private static final long MAX_WINDUP_TICKS = 3600;

    public static void onCastSync(CastSyncPayload payload) {
        switch (payload.action()) {
            case CastSyncPayload.ACTION_START -> {
                AbilityDef def = resolve(payload.abilityId());
                if (def == null || def.animation() == null) {
                    CASTS.remove(payload.entityId());
                    return;
                }
                CASTS.put(payload.entityId(), new Entry(def, payload.tick()));
            }
            case CastSyncPayload.ACTION_FIRE -> {
                Entry entry = CASTS.get(payload.entityId());
                if (entry != null && entry.def.id().toString().equals(payload.abilityId())) {
                    entry.fireTick = payload.tick();
                    return;
                }
                // The start was never seen (the observer came into
                // range mid-cast): reconstruct the post-strike entry
                // from the fire tick and the definition's startup.
                AbilityDef def = resolve(payload.abilityId());
                if (def == null || def.animation() == null) {
                    return;
                }
                Entry late = new Entry(def, payload.tick() - def.phases().startup());
                late.fireTick = payload.tick();
                CASTS.put(payload.entityId(), late);
            }
            case CastSyncPayload.ACTION_CANCEL -> CASTS.remove(payload.entityId());
            default -> { /* unknown action: ignore */ }
        }
    }

    public static void clear() {
        CASTS.clear();
    }

    private static AbilityDef resolve(String abilityId) {
        Identifier id = Identifier.tryParse(abilityId);
        return id == null ? null : AbilityRegistry.get(id);
    }

    /**
     * The pose for one player at one frame, or null when no cast
     * pose owns that player right now. gameTime is the client
     * level's game time (the server's clock), partial the frame's
     * partial tick.
     */
    public static PoseSample sample(int entityId, long gameTime, float partial) {
        Entry entry = CASTS.get(entityId);
        if (entry == null) {
            return null;
        }
        AbilityDef def = entry.def;
        AbilityDef.AnimationSpec spec = def.animation();
        if (spec == null) {
            CASTS.remove(entityId);
            return null;
        }
        double now = gameTime + partial;

        if (entry.fireTick < 0) {
            double elapsed = now - entry.startTick;
            if (elapsed < 0) {
                return null;
            }
            int startup = def.phases().startup();
            if (elapsed > startup + MAX_WINDUP_TICKS) {
                CASTS.remove(entityId);
                return null;
            }
            float frac = startup <= 0 ? 1.0f
                    : Mth.clamp((float) (elapsed / startup), 0.0f, 1.0f);
            float eased = frac * frac * (3.0f - 2.0f * frac);
            if (spec.charge() != null) {
                BedrockAnimationRegistry.BedrockAnimation chargeClip = clip(spec.charge());
                if (chargeClip == null) {
                    return null;
                }
                return new PoseSample(chargeClip, eased * chargeClip.length);
            }
            BedrockAnimationRegistry.BedrockAnimation clip = clip(spec.id());
            if (clip == null) {
                return null;
            }
            return new PoseSample(clip, eased * spec.strikeAt() * clip.length);
        }

        double post = now - entry.fireTick;
        if (post < 0) {
            post = 0;
        }
        BedrockAnimationRegistry.BedrockAnimation clip = clip(spec.id());
        if (clip == null) {
            return null;
        }
        float strikeTime = spec.strikeAt() * clip.length;

        if (spec.channel()) {
            float holdPoint = strikeTime + (clip.length - strikeTime) * 0.15f;
            int active = def.phases().active();
            if (post < active) {
                float sway = (float) Math.sin(post * 0.30) * 0.02f * clip.length;
                return new PoseSample(clip, Mth.clamp(holdPoint + sway, 0.0f, clip.length));
            }
            double settle = post - active;
            int recovery = def.phases().recovery();
            if (recovery <= 0 || settle >= recovery) {
                CASTS.remove(entityId);
                return null;
            }
            float e = easeOutCubic((float) (settle / recovery));
            return new PoseSample(clip, holdPoint + e * (clip.length - holdPoint));
        }

        int total = spec.followTicks() + def.phases().recovery();
        if (total <= 0 || post >= total + 1) {
            CASTS.remove(entityId);
            return null;
        }
        float e = easeOutCubic(Mth.clamp((float) (post / total), 0.0f, 1.0f));
        return new PoseSample(clip, strikeTime + e * (clip.length - strikeTime));
    }

    /**
     * The charge read for one player: the definition and the power
     * the cast is currently at, or null when the player is not
     * winding up. For a charge-release cast this is the literal
     * power curve AbilityService will fire with (minPower floor,
     * full at startup); for a timed cast it is startup progress.
     */
    public static ChargeInfo chargeInfo(int entityId, long gameTime, float partial) {
        Entry entry = CASTS.get(entityId);
        if (entry == null || entry.fireTick >= 0) {
            return null;
        }
        AbilityDef def = entry.def;
        double elapsed = (gameTime + partial) - entry.startTick;
        if (elapsed < 0) {
            return null;
        }
        int startup = def.phases().startup();
        float frac = startup <= 0 ? 1.0f
                : Mth.clamp((float) (elapsed / startup), 0.0f, 1.0f);
        float power = def.chargeRelease()
                ? def.minPower() + (1.0f - def.minPower()) * frac
                : frac;
        return new ChargeInfo(def, power, frac);
    }

    private static float easeOutCubic(float x) {
        float inv = 1.0f - x;
        return 1.0f - inv * inv * inv;
    }

    /**
     * Resolves a data clip name to a loaded Bedrock animation. The
     * generic ability set lives under animation.ability.*; data may
     * also name any other loaded clip by its full name. A missing
     * clip warns once and yields no pose, never an error: clip
     * existence is presentation, and presentation fails soft.
     */
    private static BedrockAnimationRegistry.BedrockAnimation clip(String name) {
        BedrockAnimationRegistry.BedrockAnimation anim =
                BedrockAnimationRegistry.getAnimation("animation.ability." + name);
        if (anim == null) {
            anim = BedrockAnimationRegistry.getAnimation(name);
        }
        if (anim == null && WARNED_MISSING.add(name)) {
            DragonBlockArcaneDBA.LOGGER.warn(
                    "Ability animation clip not found: {} (pose skipped)", name);
        }
        return anim;
    }
}
