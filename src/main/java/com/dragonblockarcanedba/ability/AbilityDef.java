package com.dragonblockarcanedba.ability;

import com.dragonblockarcanedba.particle.AbilityFxEntries;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * One weapon attack, as data. Parsed and validated in exactly one
 * place (here), used identically by the server loader and by the
 * client sync receiver, so both sides read the same definition. See
 * audits/REBUILD-ARCHITECTURE.md for the schema contract. Any invalid
 * field throws IllegalArgumentException naming the field; the loader
 * logs it loudly and skips that one ability.
 */
public record AbilityDef(
        Identifier id,
        Identifier weapon,
        Slot slot,
        String name,
        Carrier carrier,
        Phases phases,
        AnimationSpec animation,
        boolean chargeRelease,
        float minPower,
        double kiCost,
        double kiCostPercent,
        int cooldownTicks,
        DamageSpec damage,
        Sounds sounds,
        VisualSpec visual,
        Caps caps,
        WaveParams wave,
        CloudParams cloud,
        ProjectileParams projectile,
        List<FxRecipe> fx,
        GeometrySpec geometry,
        FeedbackSpec feedback,
        boolean bind,
        String sneakVariant,
        String airVariant,
        boolean rooted,
        int charges,
        int chargeRegenTicks,
        MoveSpec move,
        MeleeSpec melee,
        OnHitSpec onHit,
        SwingSpec swing,
        List<SummonSpec> summons,
        AuraSpec aura,
        SustainSpec sustain,
        AutoFireSpec autoFire,
        ComboSpec combo,
        CriticalSpec criticalWindow,
        VolleySpec volley,
        TerrainSpec terrain,
        BeamParams beam,
        SwarmParams swarm,
        java.util.List<ModelSpec> models,
        int kiDrainTicks
) {
    /** The recipes keyed to one event, in data order. Empty when the ability has none. */
    public List<FxRecipe> fxFor(FxEvent event) {
        List<FxRecipe> out = new ArrayList<>();
        for (FxRecipe recipe : fx) {
            if (recipe.event() == event) {
                out.add(recipe);
            }
        }
        return out;
    }

    public boolean hasFx(FxEvent event) {
        for (FxRecipe recipe : fx) {
            if (recipe.event() == event) {
                return true;
            }
        }
        return false;
    }
    public enum Slot {
        LEFT, RIGHT;

        public static Slot parse(String s) {
            return switch (s) {
                case "left" -> LEFT;
                case "right" -> RIGHT;
                default -> throw new IllegalArgumentException("slot must be \"left\" or \"right\", got \"" + s + "\"");
            };
        }
    }

    public enum Carrier {
        WAVE, CLOUD, PROJECTILE, BEAM, SWARM, NONE;

        public static Carrier parse(String s) {
            return switch (s) {
                case "wave" -> WAVE;
                case "cloud" -> CLOUD;
                case "projectile" -> PROJECTILE;
                case "beam" -> BEAM;
                case "swarm" -> SWARM;
                case "none" -> NONE;
                default -> throw new IllegalArgumentException("carrier must be wave, cloud, projectile, beam, swarm, or none, got \"" + s + "\"");
            };
        }
    }

    public record Phases(int startup, int active, int recovery) {}

    /**
     * The Phase 2 animation block. id names the release clip (a
     * Bedrock animation the client resolves; clips for the generic
     * set live in animations/abilities.animation.json). charge, when
     * present, names a separate wind-up clip played across startup
     * instead of the release clip's own wind-up segment. strikeAt is
     * the fraction of the release clip where the strike frame sits;
     * that frame lands exactly on the fire tick. followTicks caps
     * the follow-through after the strike (the carrier's active
     * lifetime is not a body pose); channel instead holds the raised
     * pose for the whole active phase (sustained channels).
     */
    public record AnimationSpec(String id, String charge, float strikeAt,
                                int followTicks, boolean channel) {}

    public record DamageSpec(double base, String stat, double perStat, String type,
                             String falloff, float falloffFloor) {}

    public record Sounds(String charge, String fire, String hit) {}

    public record VisualSpec(int core, int mid, int rim, float scale,
                             int particleMaterial, float particleRate) {}

    public record Caps(int maxParticlesPerSecond, double fullDetailDistance, double cullDistance) {}

    public record WaveParams(double speed, double radius, double height, double arcDegrees,
                             KnockbackSpec knockback, WaveDeflectSpec deflect, double selfLaunch,
                             double reach) {}

    /** Wave knockback module (Phase 6): out is horizontal push away from the wave center, up is vertical. */
    public record KnockbackSpec(double out, double up) {}

    /** Wave projectile deflection module (Phase 6): incoming projectiles are reflected at scale; claim re-owns them. */
    public record WaveDeflectSpec(double scale, boolean claim) {}

    public record CloudParams(double radiusStart, double radiusEnd, double height, String anchor,
                              int strikeInterval, int telegraphTicks, double strikeRadius,
                              CloudAtmosphere atmosphere,
                              String follow, double pull, VortexSpec vortex, StasisSpec stasis,
                              InsideEffectSpec insideEffect, int insideDamageInterval,
                              ChainsSpec chains, int freezeTicks, int rewindTicks,
                              BurstSpec onSpawnBurst, FinaleSpec finale) {}

    /** Cloud vortex module: lift raises insiders toward a hover speed, swirl pushes them tangentially. */
    public record VortexSpec(double lift, double swirl) {}

    /** Cloud projectile stasis module: freeze stores and zeroes velocity, slow scales it per tick. */
    public record StasisSpec(String mode, double factor) {}

    /** Cloud inside-effect module: an effect reapplied to insiders on an interval. */
    public record InsideEffectSpec(Identifier effectId, int durationTicks, int intervalTicks) {}

    /** Cloud chains module: only the latched nearest entities are fully affected by pull and inside damage. */
    public record ChainsSpec(int maxLinks) {}

    /** Cloud on-spawn burst: one area hit at spawn, scaled from the def damage. */
    public record BurstSpec(double damageScale, double knockback) {}

    /** Cloud finale: the closing burst when the domain expires naturally. */
    public record FinaleSpec(double damageScale, double radiusScale, double knockback,
                             double slamDown, int strikes) {}

    /**
     * The Phase 5 atmosphere block, inside the cloud block: while
     * this domain is active, clients inside its influence see the
     * render fog lerped toward tint and pulled closer. strength
     * (0 to 1) is how far the fog color travels toward the tint at
     * full influence; density (0 to 1) is how far the fog distances
     * shrink at full influence. Pure presentation, evaluated
     * client-side from the carrier's already-synced state (age,
     * radius formula, position); the server sends nothing for it.
     */
    public record CloudAtmosphere(int tint, float strength, float density) {}

    /**
     * One screen shake, as data (Phase 5): strength in degrees of
     * camera jitter at the event origin, durationTicks of
     * trauma-squared decay, radius in blocks. The server sends one
     * packet per event to the players inside radius; each client
     * scales strength by the distance-squared falloff from the
     * origin (ability/FeedbackMath) and arbitrates strongest-wins
     * in its shake store. The parser caps are the feel ceiling:
     * data cannot ask for more than 3 degrees, 40 ticks, or a
     * 64-block radius, so no datapack can make players sick.
     */
    public record ShakeSpec(float strength, int durationTicks, float radius) {}

    /**
     * The Phase 5 feedback block: one optional shake per ability
     * event. A null member means that event is silent. Events are
     * the same moments the fx recipes key to: fire (the carrier
     * spawns), carrier_hit (a wave sweep connects, a strike lands,
     * a projectile hits), carrier_end (the carrier expires). The
     * service that sends them is ability/AbilityFeedbackService.
     */
    public record FeedbackSpec(ShakeSpec onFire, ShakeSpec onCarrierHit, ShakeSpec onCarrierEnd) {
        public static final FeedbackSpec NONE = new FeedbackSpec(null, null, null);

        public ShakeSpec forEvent(FxEvent event) {
            return switch (event) {
                case FIRE -> onFire;
                case CARRIER_HIT -> onCarrierHit;
                case CARRIER_END -> onCarrierEnd;
                default -> null;
            };
        }
    }

    public record ProjectileParams(double speed, double gravityScale, double aoeRadius,
                                   String casing, double lengthScale) {}

    /** Caster movement module (Phase 6). mode is one of dash, chain, step, warp, launch, slam, rush. */
    public record MoveSpec(String mode, double distance, double maxDistance, int targets,
                           double range, int stepTicks, boolean damageOnPass, double forward) {}

    /** Instant cone hit module (Phase 6), resolved at the fire tick. */
    public record MeleeSpec(double range, double arcDegrees) {}

    /** On-hit application module (Phase 6), applied at the one damage choke point. */
    public record OnHitSpec(Identifier effectId, int durationTicks, int amplifier,
                            double healFraction, String appliesTo) {}

    /** Passive swing module (Phase 6): the effect this weapon's vanilla melee applies. */
    public record SwingSpec(Identifier effectId, int durationTicks, int amplifier) {}

    /** One summons entry (Phase 6): spawn another def's carrier from this cast or hit. */
    public record SummonSpec(String ability, String at, String when, boolean once) {}

    /** Aura module (Phase 6): active while this def's cast is held. */
    public record AuraSpec(double radius, double pull, boolean deflect, double deflectScale,
                           int damageInterval, double damageScale,
                           Identifier selfEffectId, int selfEffectDuration) {}

    /** Sustain channel module (Phase 6): one beam carrier alive while held, draining Ki. */
    public record SustainSpec(double kiPercentPerSecond) {}

    /** AutoFire channel module (Phase 6): repeated projectile spawns, heat builds to an overcharge. */
    public record AutoFireSpec(int intervalTicks, double heatPerShot, double overchargeHeat,
                               double overchargePower, double overchargeAoeScale) {}

    /** Combo module (Phase 6): counted hits arm the carrier on completion. */
    public record ComboSpec(int hits, int windowTicks, boolean carrierOnComplete) {}

    /** Critical release window (Phase 6): releasing inside the window scales power and may summon. */
    public record CriticalSpec(int fromTicks, int toTicks, double powerScale, String summonAbility) {}

    /** Volley module (Phase 6): several projectiles per fire, spread across an arc. */
    public record VolleySpec(int count, double spreadDegrees, int delayTicks) {}

    /** Terrain module (Phase 6): one PermanentDestruction event per def. */
    public record TerrainSpec(String event, double radius, int cap, double craterDepth,
                              String placeId, String crustId) {}

    /** Beam carrier block (Phase 6): required exactly when carrier is beam. */
    public record BeamParams(double length, double radius, int damageInterval,
                             String form, double endBurstRadius) {}

    /** Swarm carrier block (Phase 6): required exactly when carrier is swarm. */
    public record SwarmParams(int count, double budget, double shardBase, double shardPerStat,
                              String shardStat, double slamRadius, double interceptRadius,
                              double assignRadius, double drainPercentPerMinute,
                              int recallCooldownTicks, int starveCooldownTicks, int lockoutTicks) {}

    /**
     * The models block (Models Pass): a modeled presentation set
     * rendered by the one generic ModelDisplayEntity, never by
     * per-weapon code. The model names a GeckoLib asset set under
     * geckolib/models/entity/ (plus its texture, glowmask, and
     * animation file of the same name). Behaviors are presentation
     * only: descend plants pieces from the air onto the ground,
     * fall rains them, hang freezes them mid-air before they drop,
     * bolt flies them along the cast direction, and escort carries
     * them with the caster, orbit circles the caster, ride glues a
     * piece to a host entity, and burst plants pieces at a fixed
     * point in the air (a ring around the anchor faces outward) for
     * their lifetime. The block may be one object or an array of up
     * to four sets. Trigger "step" spawns at each chain move hop
     * from the spot the caster leaves; minPower gates a fire set on
     * the release power (1.0 means full charge only).
     * Trigger "finale" fires the set from a
     * cloud carrier's finale instead of at cast fire. Damage,
     * timing, and costs never read this block.
     */
    public record ModelSpec(String model, int count, String formation, String behavior,
                            String trigger, double radius, double height, float scale,
                            int lifetimeTicks, int descendTicks, double fallSpeed,
                            double spin, int hangTicks, double speed, double forward,
                            double up, String animSpawn, String animHold, String animEnd,
                            float minPower, String animAlt, int spawnTicks) {

        /** The same set with a different hold clip (animAlt alternation). */
        public ModelSpec withHold(String hold) {
            return new ModelSpec(model, count, formation, behavior, trigger, radius, height, scale,
                    lifetimeTicks, descendTicks, fallSpeed, spin, hangTicks, speed, forward, up,
                    animSpawn, hold, animEnd, minPower, animAlt, spawnTicks);
        }

        /** The same set with a resolved lifetime. */
        public ModelSpec withLifetime(int ticks) {
            return new ModelSpec(model, count, formation, behavior, trigger, radius, height, scale,
                    ticks, descendTicks, fallSpeed, spin, hangTicks, speed, forward, up,
                    animSpawn, animHold, animEnd, minPower, animAlt, spawnTicks);
        }
    }

    /** The first ride set (swarm shard bodies), or null when none rides. */
    public ModelSpec rideModel() {
        for (ModelSpec spec : models) {
            if ("ride".equals(spec.behavior())) {
                return spec;
            }
        }
        return null;
    }

    /**
     * The Phase 4 geometry block: per-surface options for the
     * primitive kit behind the carrier renderers. Every default
     * reproduces the Phase 1 to 3 rendering exactly (smooth band,
     * two flat shells, ground-only charge ring, shard casing), so
     * an ability without the block renders as it always has; an
     * ability opts into torn bands, raised rings, swirling
     * shells, split strike hues, and plate casings purely in
     * data. Sub-blocks are keyed by the surface they dress, not
     * by the carrier, because the charge ring belongs to no
     * carrier.
     */
    public record WaveGeometry(float turbulence, boolean baseRing) {}

    public record CloudGeometry(int shellLayers, float swirlSpeed, float shellAlpha,
                                float rimGlow, boolean strikeHueSplit) {}

    public record ChargeGeometry(boolean raisedRing, float ringHeight, float ringWidth) {}

    public record ProjectileGeometry(String casingForm) {}

    /**
     * The geometry.ground block (Phase 6, parsed here in common code;
     * drawn by the client renderers): seeded ground geometry at the
     * carrier anchor. All counts zero is off, which is the default.
     */
    public record GroundGeometry(int wallSlabs, double wallHeight, double wallArc,
                                 int crackCount, double crackRadius,
                                 int spikes, double spikeRadius, double spikeHeight) {
        public static final GroundGeometry OFF = new GroundGeometry(0, 3.0, 0.45, 0, 0.0, 0, 0.0, 2.2);
    }

    public record GeometrySpec(WaveGeometry wave, CloudGeometry cloud,
                               ChargeGeometry charge, ProjectileGeometry projectile,
                               GroundGeometry ground) {
        public static final GeometrySpec DEFAULT = new GeometrySpec(
                new WaveGeometry(0f, true),
                new CloudGeometry(2, 0f, 1f, 0f, false),
                new ChargeGeometry(false, 1.1f, 0.22f),
                new ProjectileGeometry("shard"),
                GroundGeometry.OFF);
    }

    /**
     * The moments an fx recipe can key to (Phase 3). cast_start and
     * fire are cast moments (fire is the strike tick of Phase 2: the
     * tick the carrier spawns); carrier_hit and carrier_end are
     * carrier moments; those four are bursts and travel by packet
     * (network/AbilityFxPayload). carrier_tick and charge_tick are
     * continuous: they never travel, each client expands them
     * locally from the carrier's synced state or the cast store.
     * "strike" is accepted as a data alias for fire.
     */
    public enum FxEvent {
        CAST_START, FIRE, CARRIER_TICK, CARRIER_HIT, CARRIER_END, CHARGE_TICK;

        public boolean isBurst() {
            return this == CAST_START || this == FIRE || this == CARRIER_HIT || this == CARRIER_END;
        }

        public static FxEvent parse(String s) {
            return switch (s) {
                case "cast_start" -> CAST_START;
                case "fire", "strike" -> FIRE;
                case "carrier_tick" -> CARRIER_TICK;
                case "carrier_hit" -> CARRIER_HIT;
                case "carrier_end" -> CARRIER_END;
                case "charge_tick" -> CHARGE_TICK;
                default -> throw new IllegalArgumentException("event must be one of cast_start, fire, "
                        + "carrier_tick, carrier_hit, carrier_end, charge_tick, got \"" + s + "\"");
            };
        }

        public String dataName() {
            return switch (this) {
                case CAST_START -> "cast_start";
                case FIRE -> "fire";
                case CARRIER_TICK -> "carrier_tick";
                case CARRIER_HIT -> "carrier_hit";
                case CARRIER_END -> "carrier_end";
                case CHARGE_TICK -> "charge_tick";
            };
        }
    }

    /** Emission shapes a recipe samples its spawn points from. */
    public enum FxShape {
        POINT, RING, DISC, CONE, SPHERE_SHELL, BAND;

        public static FxShape parse(String s) {
            return switch (s) {
                case "point" -> POINT;
                case "ring" -> RING;
                case "disc" -> DISC;
                case "cone" -> CONE;
                case "sphere_shell" -> SPHERE_SHELL;
                case "band" -> BAND;
                default -> throw new IllegalArgumentException("shape must be one of point, ring, disc, "
                        + "cone, sphere_shell, band, got \"" + s + "\"");
            };
        }
    }

    /**
     * One emission recipe (Phase 3). Burst events use count;
     * continuous events use rate (particles per second). colorRef
     * names a visual color ("core", "mid", "rim"); colorLiteral,
     * when non-null, overrides it with a parsed ARGB value; alpha
     * multiplies the color's alpha. radius of -1 means the shape
     * takes the carrier's own radius (the cloud's live radius, the
     * wave's band radius, the event's natural radius) as resolved
     * by the expander. cap, when non-null, bounds this recipe (per
     * burst, or per second while continuous); capGroup, when
     * non-null, makes every recipe in the group share one
     * per-second budget on top of the ability-wide caps budget.
     * gravity and drag are handed to the particle in the material
     * profiles' units (see AbilityFxOptions).
     */
    public record FxRecipe(FxEvent event, int entry, String colorRef, Integer colorLiteral,
                           FxShape shape, int count, double rate, double radius,
                           double speed, double spread, double angle,
                           double sizeMin, double sizeMax, double sizeEndScale,
                           int lifetimeMin, int lifetimeMax,
                           double gravity, double drag, double alpha,
                           Integer cap, String capGroup) {
        /** The tint this recipe emits: the literal if set, else the named visual color, with alpha applied. */
        public int resolveColor(VisualSpec vis) {
            int base = colorLiteral != null ? colorLiteral : switch (colorRef) {
                case "core" -> vis.core();
                case "rim" -> vis.rim();
                default -> vis.mid();
            };
            int a = Math.round((float) (((base >>> 24) & 0xFF) * alpha));
            return (a << 24) | (base & 0xFFFFFF);
        }
    }

    private static final Set<String> TOP_LEVEL = Set.of(
            "weapon", "slot", "name", "carrier", "phases", "animation", "chargeRelease",
            "minPower", "kiCost", "kiCostPercent", "kiDrainTicks", "cooldownTicks", "damage", "sounds",
            "visual", "caps", "wave", "cloud", "projectile", "fx", "geometry", "feedback",
            "bind", "sneakVariant", "airVariant", "rooted", "charges", "chargeRegenTicks",
            "move", "melee", "onHit", "swing", "summons", "aura", "sustain", "autoFire",
            "combo", "criticalWindow", "volley", "terrain", "beam", "swarm", "models");

    private static final Set<String> STATS = Set.of(
            "strength", "dexterity", "spirit", "willpower", "defense", "none");

    private static final Set<String> DAMAGE_TYPES = Set.of(
            "ki", "void", "curse", "darkness", "hellfire", "tempest",
            "spirit", "temporal", "gravity", "lava", "valor", "crush");

    public static AbilityDef parse(Identifier id, JsonObject json) {
        for (String key : json.keySet()) {
            if (!TOP_LEVEL.contains(key)) {
                throw new IllegalArgumentException("unknown top-level field \"" + key + "\"");
            }
        }
        Identifier weapon = Identifier.parse(reqString(json, "weapon"));
        Slot slot = Slot.parse(reqString(json, "slot"));
        String name = reqString(json, "name");
        Carrier carrier = Carrier.parse(reqString(json, "carrier"));

        JsonObject phasesJson = reqObject(json, "phases");
        Phases phases = new Phases(
                reqInt(phasesJson, "startup", 0, 72000),
                reqInt(phasesJson, "active", 1, 72000),
                reqInt(phasesJson, "recovery", 0, 72000));

        AnimationSpec animation = parseAnimation(json);
        boolean chargeRelease = optBool(json, "chargeRelease", false);
        float minPower = (float) optDouble(json, "minPower", 0.4, 0.0, 1.0);
        double kiCost = optDouble(json, "kiCost", 0, 0, Double.MAX_VALUE);
        double kiCostPercent = optDouble(json, "kiCostPercent", 0, 0, 100);
        // kiDrainTicks > 0: the price is paid in equal parts once per tick
        // over that many ticks starting at fire, instead of all at fire.
        int kiDrainTicks = (int) optDouble(json, "kiDrainTicks", 0, 0, 200);
        int cooldownTicks = (int) optDouble(json, "cooldownTicks", 0, 0, 72000);

        JsonObject dmg = reqObject(json, "damage");
        String stat = reqString(dmg, "stat");
        if (!STATS.contains(stat)) {
            throw new IllegalArgumentException("damage.stat must be one of " + STATS + ", got \"" + stat + "\"");
        }
        String type = reqString(dmg, "type");
        if (!DAMAGE_TYPES.contains(type)) {
            throw new IllegalArgumentException("damage.type must be one of " + DAMAGE_TYPES + ", got \"" + type + "\"");
        }
        String falloff = optString(dmg, "falloff", "none");
        if (!falloff.equals("none") && !falloff.equals("linear")) {
            throw new IllegalArgumentException("damage.falloff must be none or linear, got \"" + falloff + "\"");
        }
        DamageSpec damage = new DamageSpec(
                reqDouble(dmg, "base", 0, Double.MAX_VALUE),
                stat,
                optDouble(dmg, "perStat", 0, -Double.MAX_VALUE, Double.MAX_VALUE),
                type, falloff,
                (float) optDouble(dmg, "falloffFloor", 0.4, 0.0, 1.0));

        Sounds sounds = new Sounds(null, null, null);
        if (json.has("sounds")) {
            JsonObject s = reqObject(json, "sounds");
            sounds = new Sounds(optString(s, "charge", null), optString(s, "fire", null), optString(s, "hit", null));
        }

        JsonObject vis = reqObject(json, "visual");
        VisualSpec visual = new VisualSpec(
                parseColor(reqString(vis, "core")),
                parseColor(reqString(vis, "mid")),
                parseColor(reqString(vis, "rim")),
                (float) optDouble(vis, "scale", 1.0, 0.01, 100),
                parseMaterial(optString(vis, "particleMaterial", "plasma")),
                (float) optDouble(vis, "particleRate", 1.0, 0.0, 100));

        Caps caps = new Caps(400, 48, 128);
        if (json.has("caps")) {
            JsonObject c = reqObject(json, "caps");
            caps = new Caps(
                    (int) optDouble(c, "maxParticlesPerSecond", 400, 0, 100000),
                    optDouble(c, "fullDetailDistance", 48, 1, 1024),
                    optDouble(c, "cullDistance", 128, 1, 2048));
        }

        WaveParams wave = null;
        CloudParams cloud = null;
        ProjectileParams projectile = null;
        BeamParams beam = null;
        SwarmParams swarm = null;
        switch (carrier) {
            case WAVE -> {
                JsonObject w = reqObject(json, "wave");
                wave = new WaveParams(
                        reqDouble(w, "speed", 0.001, 100),
                        reqDouble(w, "radius", 0.1, 512),
                        reqDouble(w, "height", 0.1, 512),
                        reqDouble(w, "arcDegrees", 10, 360),
                        parseWaveKnockback(w),
                        parseWaveDeflect(w),
                        optDouble(w, "wave.selfLaunch", "selfLaunch", 0, 0, 4),
                        // reach: when set, hits are gated to a true cone, within
                        // reach blocks of the spawn point and arcDegrees of the aim
                        optDouble(w, "wave.reach", "reach", 0, 0, 512));
            }
            case CLOUD -> {
                JsonObject c = reqObject(json, "cloud");
                String anchor = optString(c, "anchor", "caster");
                if (!anchor.equals("caster") && !anchor.equals("target") && !anchor.equals("entity")) {
                    throw new IllegalArgumentException("cloud.anchor must be caster, target, or entity, got \"" + anchor + "\"");
                }
                int strikeInterval = (int) optDouble(c, "strikeInterval", 0, 0, 72000);
                int telegraph = (int) optDouble(c, "telegraphTicks", 10, 0, 72000);
                if (strikeInterval > 0 && telegraph >= strikeInterval) {
                    throw new IllegalArgumentException("cloud.telegraphTicks must be smaller than cloud.strikeInterval");
                }
                String follow = optString(c, "follow", "none");
                if (!follow.equals("none") && !follow.equals("caster") && !follow.equals("entity")) {
                    throw new IllegalArgumentException("cloud.follow must be none, caster, or entity, got \"" + follow + "\"");
                }
                cloud = new CloudParams(
                        reqDouble(c, "radiusStart", 0.1, 512),
                        reqDouble(c, "radiusEnd", 0.1, 512),
                        reqDouble(c, "height", 0.1, 512),
                        anchor, strikeInterval, telegraph,
                        optDouble(c, "strikeRadius", 2.5, 0.1, 512),
                        parseAtmosphere(c),
                        follow,
                        optDouble(c, "cloud.pull", "pull", 0, -4, 4),
                        parseVortex(c),
                        parseStasis(c),
                        parseInsideEffect(c),
                        (int) optDouble(c, "cloud.insideDamageInterval", "insideDamageInterval", 0, 0, 1000),
                        parseChains(c),
                        (int) optDouble(c, "cloud.freezeTicks", "freezeTicks", 0, 0, 72000),
                        (int) optDouble(c, "cloud.rewindTicks", "rewindTicks", 0, 0, 72000),
                        parseBurst(c),
                        parseFinale(c));
            }
            case PROJECTILE -> {
                JsonObject p = reqObject(json, "projectile");
                String casing = optString(p, "casing", "energy");
                if (!casing.equals("lit") && !casing.equals("energy") && !casing.equals("none")) {
                    throw new IllegalArgumentException("projectile.casing must be lit, energy, or none, got \"" + casing + "\"");
                }
                projectile = new ProjectileParams(
                        reqDouble(p, "speed", 0.001, 100),
                        optDouble(p, "gravityScale", 1.0, 0, 100),
                        optDouble(p, "aoeRadius", 0, 0, 512),
                        casing,
                        optDouble(p, "lengthScale", 1.0, 0.01, 100));
            }
            case BEAM -> beam = parseBeam(json);
            case SWARM -> swarm = parseSwarm(json);
            case NONE -> {
            }
        }

        List<FxRecipe> fx = parseFx(json);
        GeometrySpec geometry = parseGeometry(json);
        FeedbackSpec feedback = parseFeedback(json);

        boolean bind = optBool(json, "bind", true);
        String sneakVariant = optIdString(json, "sneakVariant", "sneakVariant");
        String airVariant = optIdString(json, "airVariant", "airVariant");
        boolean rooted = optBool(json, "rooted", false);
        int charges = (int) optDouble(json, "charges", 1, 1, 16);
        int chargeRegenTicks = (int) optDouble(json, "chargeRegenTicks", 0, 0, 72000);
        MoveSpec move = parseMove(json);
        MeleeSpec melee = parseMelee(json);
        OnHitSpec onHit = parseOnHit(json);
        SwingSpec swing = parseSwing(json);
        List<SummonSpec> summons = parseSummons(json);
        AuraSpec aura = parseAura(json);
        SustainSpec sustain = parseSustain(json);
        AutoFireSpec autoFire = parseAutoFire(json);
        ComboSpec combo = parseCombo(json);
        CriticalSpec criticalWindow = parseCritical(json);
        VolleySpec volley = parseVolley(json);
        TerrainSpec terrain = parseTerrain(json);
        java.util.List<ModelSpec> models = parseModels(json);

        return new AbilityDef(id, weapon, slot, name, carrier, phases, animation,
                chargeRelease, minPower, kiCost, kiCostPercent, cooldownTicks,
                damage, sounds, visual, caps, wave, cloud, projectile, List.copyOf(fx),
                geometry, feedback, bind, sneakVariant, airVariant, rooted, charges,
                chargeRegenTicks, move, melee, onHit, swing, List.copyOf(summons), aura,
                sustain, autoFire, combo, criticalWindow, volley, terrain, beam, swarm,
                models, kiDrainTicks);
    }


    // ------------------------------------------------------------------
    // Phase 6 blocks. Every parser below validates by name in the same
    // style as the rest of the schema: unknown fields are rejected
    // naming the field, ranges are checked, and identifier strings
    // are format-checked only (resolution is the loader's job).
    // ------------------------------------------------------------------

    private static boolean namedBool(JsonObject json, String displayName, String field, boolean fallback) {
        JsonElement el = json.get(field);
        if (el == null) {
            return fallback;
        }
        if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException("field \"" + displayName + "\" must be a boolean");
        }
        return el.getAsBoolean();
    }

    /** An optional ability or block id string, format-checked only. Null when absent. */
    private static String optIdString(JsonObject json, String displayName, String field) {
        JsonElement el = json.get(field);
        if (el == null) {
            return null;
        }
        if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("field \"" + displayName + "\" must be a string");
        }
        String v = el.getAsString();
        if (Identifier.tryParse(v) == null) {
            throw new IllegalArgumentException("field \"" + displayName
                    + "\" must be a valid identifier, got \"" + v + "\"");
        }
        return v;
    }

    private static String reqIdString(JsonObject json, String displayName, String field) {
        String v = optIdString(json, displayName, field);
        if (v == null) {
            throw new IllegalArgumentException("field \"" + displayName + "\" is required");
        }
        return v;
    }

    /** An effect id as an Identifier, format-checked only. Null when absent and not required. */
    private static Identifier optEffectId(JsonObject json, String displayName, String field, boolean required) {
        String v = optIdString(json, displayName, field);
        if (v == null) {
            if (required) {
                throw new IllegalArgumentException("field \"" + displayName + "\" is required");
            }
            return null;
        }
        return Identifier.parse(v);
    }

    private static void rejectUnknown(JsonObject json, Set<String> fields, String blockDisplay) {
        for (String key : json.keySet()) {
            if (!fields.contains(key)) {
                throw new IllegalArgumentException("unknown field \"" + blockDisplay + "." + key + "\"");
            }
        }
    }

    private static final Set<String> MOVE_FIELDS = Set.of(
            "mode", "distance", "maxDistance", "targets", "range", "stepTicks", "damageOnPass", "forward");
    private static final Set<String> MOVE_MODES = Set.of(
            "dash", "chain", "step", "warp", "launch", "slam", "rush");

    private static MoveSpec parseMove(JsonObject json) {
        if (!json.has("move")) {
            return null;
        }
        JsonObject m = reqObject(json, "move");
        rejectUnknown(m, MOVE_FIELDS, "move");
        String mode = reqString(m, "mode");
        if (!MOVE_MODES.contains(mode)) {
            throw new IllegalArgumentException("move.mode must be one of dash, chain, step, warp, "
                    + "launch, slam, rush, got \"" + mode + "\"");
        }
        return new MoveSpec(mode,
                optDouble(m, "move.distance", "distance", 8, 0, 128),
                optDouble(m, "move.maxDistance", "maxDistance", 128, 16, 1024),
                (int) optDouble(m, "move.targets", "targets", 5, 1, 16),
                optDouble(m, "move.range", "range", 24, 4, 64),
                (int) optDouble(m, "move.stepTicks", "stepTicks", 2, 1, 20),
                namedBool(m, "move.damageOnPass", "damageOnPass", false),
                // launch only: horizontal push along the caster's aim, in blocks per tick
                optDouble(m, "move.forward", "forward", 0, 0, 3));
    }

    private static final Set<String> MELEE_FIELDS = Set.of("range", "arcDegrees");

    private static MeleeSpec parseMelee(JsonObject json) {
        if (!json.has("melee")) {
            return null;
        }
        JsonObject m = reqObject(json, "melee");
        rejectUnknown(m, MELEE_FIELDS, "melee");
        return new MeleeSpec(
                optDouble(m, "melee.range", "range", 4, 1, 12),
                optDouble(m, "melee.arcDegrees", "arcDegrees", 90, 10, 360));
    }

    private static final Set<String> ONHIT_FIELDS = Set.of(
            "effect", "durationTicks", "amplifier", "healFraction", "appliesTo");

    private static OnHitSpec parseOnHit(JsonObject json) {
        if (!json.has("onHit")) {
            return null;
        }
        JsonObject o = reqObject(json, "onHit");
        rejectUnknown(o, ONHIT_FIELDS, "onHit");
        Identifier effectId = optEffectId(o, "onHit.effect", "effect", false);
        double heal = optDouble(o, "onHit.healFraction", "healFraction", 0, 0, 1);
        if (effectId == null && heal <= 0) {
            throw new IllegalArgumentException("onHit must set an effect or a healFraction above 0 "
                    + "(onHit.effect and onHit.healFraction are both empty)");
        }
        String appliesTo = optString(o, "appliesTo", "any");
        if (!appliesTo.equals("any") && !appliesTo.equals("melee") && !appliesTo.equals("carrier")) {
            throw new IllegalArgumentException("onHit.appliesTo must be any, melee, or carrier, got \""
                    + appliesTo + "\"");
        }
        return new OnHitSpec(effectId,
                (int) optDouble(o, "onHit.durationTicks", "durationTicks", 100, 1, 72000),
                (int) optDouble(o, "onHit.amplifier", "amplifier", 0, 0, 10),
                heal, appliesTo);
    }

    private static final Set<String> SWING_FIELDS = Set.of("effect", "durationTicks", "amplifier");

    private static SwingSpec parseSwing(JsonObject json) {
        if (!json.has("swing")) {
            return null;
        }
        JsonObject sw = reqObject(json, "swing");
        rejectUnknown(sw, SWING_FIELDS, "swing");
        return new SwingSpec(
                optEffectId(sw, "swing.effect", "effect", true),
                (int) optDouble(sw, "swing.durationTicks", "durationTicks", 100, 1, 72000),
                (int) optDouble(sw, "swing.amplifier", "amplifier", 0, 0, 10));
    }

    private static final Set<String> SUMMON_FIELDS = Set.of("ability", "at", "when", "once");
    private static final Set<String> SUMMON_AT = Set.of("caster", "look", "hit");
    private static final Set<String> SUMMON_WHEN = Set.of("fire", "fullPower", "critical", "meleeHit", "carrierHit", "carrierEnd");

    private static List<SummonSpec> parseSummons(JsonObject json) {
        if (!json.has("summons")) {
            return List.of();
        }
        JsonElement el = json.get("summons");
        if (el == null || !el.isJsonArray()) {
            throw new IllegalArgumentException("field \"summons\" must be an array of summon objects");
        }
        List<SummonSpec> out = new ArrayList<>();
        JsonArray array = el.getAsJsonArray();
        for (int i = 0; i < array.size(); i++) {
            String at = "summons[" + i + "]";
            JsonElement item = array.get(i);
            if (item == null || !item.isJsonObject()) {
                throw new IllegalArgumentException(at + " must be an object");
            }
            JsonObject sm = item.getAsJsonObject();
            rejectUnknown(sm, SUMMON_FIELDS, at);
            String ability = reqIdString(sm, at + ".ability", "ability");
            String atPos = optString(sm, "at", "caster");
            if (!SUMMON_AT.contains(atPos)) {
                throw new IllegalArgumentException(at + ".at must be caster, look, or hit, got \"" + atPos + "\"");
            }
            String when = optString(sm, "when", "fire");
            if (!SUMMON_WHEN.contains(when)) {
                throw new IllegalArgumentException(at + ".when must be fire, fullPower, critical, "
                        + "meleeHit, carrierHit, or carrierEnd, got \"" + when + "\"");
            }
            out.add(new SummonSpec(ability, atPos, when,
                    namedBool(sm, at + ".once", "once", true)));
        }
        return out;
    }

    private static final Set<String> AURA_FIELDS = Set.of(
            "radius", "pull", "deflect", "deflectScale", "damageInterval", "damageScale",
            "selfEffect", "selfEffectDuration");

    private static AuraSpec parseAura(JsonObject json) {
        if (!json.has("aura")) {
            return null;
        }
        JsonObject a = reqObject(json, "aura");
        rejectUnknown(a, AURA_FIELDS, "aura");
        return new AuraSpec(
                reqDouble(a, "aura.radius", "radius", 1, 32),
                optDouble(a, "aura.pull", "pull", 0, -4, 4),
                namedBool(a, "aura.deflect", "deflect", false),
                optDouble(a, "aura.deflectScale", "deflectScale", 2.2, 0.1, 4),
                (int) optDouble(a, "aura.damageInterval", "damageInterval", 0, 0, 1000),
                optDouble(a, "aura.damageScale", "damageScale", 0.25, 0, 2),
                optEffectId(a, "aura.selfEffect", "selfEffect", false),
                (int) optDouble(a, "aura.selfEffectDuration", "selfEffectDuration", 60, 10, 72000));
    }

    private static final Set<String> SUSTAIN_FIELDS = Set.of("kiPercentPerSecond");

    private static SustainSpec parseSustain(JsonObject json) {
        if (!json.has("sustain")) {
            return null;
        }
        JsonObject su = reqObject(json, "sustain");
        rejectUnknown(su, SUSTAIN_FIELDS, "sustain");
        return new SustainSpec(optDouble(su, "sustain.kiPercentPerSecond", "kiPercentPerSecond", 5, 0, 100));
    }

    private static final Set<String> AUTOFIRE_FIELDS = Set.of(
            "intervalTicks", "heatPerShot", "overchargeHeat", "overchargePower", "overchargeAoeScale");

    private static AutoFireSpec parseAutoFire(JsonObject json) {
        if (!json.has("autoFire")) {
            return null;
        }
        JsonObject af = reqObject(json, "autoFire");
        rejectUnknown(af, AUTOFIRE_FIELDS, "autoFire");
        return new AutoFireSpec(
                (int) reqDouble(af, "autoFire.intervalTicks", "intervalTicks", 1, 100),
                optDouble(af, "autoFire.heatPerShot", "heatPerShot", 1, 0, 100),
                optDouble(af, "autoFire.overchargeHeat", "overchargeHeat", 0, 0, 10000),
                optDouble(af, "autoFire.overchargePower", "overchargePower", 2.5, 1, 5),
                optDouble(af, "autoFire.overchargeAoeScale", "overchargeAoeScale", 2, 1, 4));
    }

    private static final Set<String> COMBO_FIELDS = Set.of("hits", "windowTicks", "carrierOnComplete");

    private static ComboSpec parseCombo(JsonObject json) {
        if (!json.has("combo")) {
            return null;
        }
        JsonObject c = reqObject(json, "combo");
        rejectUnknown(c, COMBO_FIELDS, "combo");
        return new ComboSpec(
                (int) reqDouble(c, "combo.hits", "hits", 2, 100),
                (int) optDouble(c, "combo.windowTicks", "windowTicks", 200, 20, 72000),
                namedBool(c, "combo.carrierOnComplete", "carrierOnComplete", false));
    }

    private static final Set<String> CRITICAL_FIELDS = Set.of(
            "fromTicks", "toTicks", "powerScale", "summonAbility");

    private static CriticalSpec parseCritical(JsonObject json) {
        if (!json.has("criticalWindow")) {
            return null;
        }
        JsonObject c = reqObject(json, "criticalWindow");
        rejectUnknown(c, CRITICAL_FIELDS, "criticalWindow");
        int from = (int) reqDouble(c, "criticalWindow.fromTicks", "fromTicks", 0, 72000);
        JsonElement toEl = c.get("toTicks");
        if (toEl == null || !toEl.isJsonPrimitive() || !toEl.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("field \"criticalWindow.toTicks\" is required and must be a number");
        }
        int to = toEl.getAsInt();
        if (to < from) {
            throw new IllegalArgumentException("criticalWindow.toTicks must be >= criticalWindow.fromTicks, got "
                    + to + " < " + from);
        }
        return new CriticalSpec(from, to,
                optDouble(c, "criticalWindow.powerScale", "powerScale", 1.5, 1, 5),
                optIdString(c, "criticalWindow.summonAbility", "summonAbility"));
    }

    private static final Set<String> VOLLEY_FIELDS = Set.of("count", "spreadDegrees", "delayTicks");

    private static VolleySpec parseVolley(JsonObject json) {
        if (!json.has("volley")) {
            return null;
        }
        JsonObject v = reqObject(json, "volley");
        rejectUnknown(v, VOLLEY_FIELDS, "volley");
        return new VolleySpec(
                (int) optDouble(v, "volley.count", "count", 1, 1, 16),
                optDouble(v, "volley.spreadDegrees", "spreadDegrees", 0, 0, 180),
                (int) optDouble(v, "volley.delayTicks", "delayTicks", 0, 0, 100));
    }

    private static final Set<String> TERRAIN_FIELDS = Set.of(
            "event", "radius", "cap", "craterDepth", "place", "crust");
    private static final Set<String> TERRAIN_EVENTS = Set.of("spawn", "finale", "impact", "trail", "field");

    private static TerrainSpec parseTerrain(JsonObject json) {
        if (!json.has("terrain")) {
            return null;
        }
        JsonObject t = reqObject(json, "terrain");
        rejectUnknown(t, TERRAIN_FIELDS, "terrain");
        String event = reqString(t, "event");
        if (!TERRAIN_EVENTS.contains(event)) {
            throw new IllegalArgumentException("terrain.event must be spawn, finale, impact, trail, "
                    + "or field, got \"" + event + "\"");
        }
        return new TerrainSpec(event,
                optDouble(t, "terrain.radius", "radius", 4, 0.5, 32),
                (int) optDouble(t, "terrain.cap", "cap", 200, 1, 4096),
                optDouble(t, "terrain.craterDepth", "craterDepth", 0, 0, 2),
                optIdString(t, "terrain.place", "place"),
                optIdString(t, "terrain.crust", "crust"));
    }

    private static final Set<String> MODELS_FIELDS = Set.of(
            "model", "count", "formation", "behavior", "trigger", "radius", "height",
            "scale", "lifetimeTicks", "descendTicks", "fallSpeed", "spin", "hangTicks",
            "speed", "forward", "up", "animSpawn", "animHold", "animEnd", "minPower", "animAlt", "spawnTicks");
    private static final Set<String> MODELS_BEHAVIORS = Set.of(
            "descend", "fall", "escort", "bolt", "hang", "orbit", "ride", "burst");
    private static final Set<String> MODELS_FORMATIONS = Set.of(
            "ring", "rain", "escort", "point");
    private static final Set<String> MODELS_TRIGGERS = Set.of("fire", "finale", "cast", "step", "latch");

    private static java.util.List<ModelSpec> parseModels(JsonObject json) {
        if (!json.has("models")) {
            return java.util.List.of();
        }
        com.google.gson.JsonElement el = json.get("models");
        java.util.List<ModelSpec> out = new java.util.ArrayList<>();
        if (el.isJsonArray()) {
            if (el.getAsJsonArray().size() < 1 || el.getAsJsonArray().size() > 4) {
                throw new IllegalArgumentException("models array must hold 1 to 4 sets");
            }
            for (com.google.gson.JsonElement e : el.getAsJsonArray()) {
                if (!e.isJsonObject()) {
                    throw new IllegalArgumentException("models array entries must be objects");
                }
                out.add(parseModelSet(e.getAsJsonObject()));
            }
        } else {
            out.add(parseModelSet(reqObject(json, "models")));
        }
        return java.util.List.copyOf(out);
    }

    private static ModelSpec parseModelSet(JsonObject m) {
        rejectUnknown(m, MODELS_FIELDS, "models");
        String model = reqString(m, "model");
        if (!isClipName(model) || model.contains(".")) {
            throw new IllegalArgumentException(
                    "models.model must be a lowercase asset name (letters, digits, underscores), got \"" + model + "\"");
        }
        String behavior = optString(m, "behavior", "descend");
        if (!MODELS_BEHAVIORS.contains(behavior)) {
            throw new IllegalArgumentException("models.behavior must be descend, fall, escort, "
                    + "bolt, hang, orbit, ride, or burst, got \"" + behavior + "\"");
        }
        String formation = optString(m, "formation", switch (behavior) {
            case "fall" -> "rain";
            case "escort" -> "escort";
            case "descend", "orbit", "burst" -> "ring";
            default -> "point";
        });
        if (!MODELS_FORMATIONS.contains(formation)) {
            throw new IllegalArgumentException("models.formation must be ring, rain, escort, "
                    + "or point, got \"" + formation + "\"");
        }
        String trigger = optString(m, "trigger", "fire");
        if (!MODELS_TRIGGERS.contains(trigger)) {
            throw new IllegalArgumentException(
                    "models.trigger must be fire, finale, cast, step, or latch, got \"" + trigger + "\"");
        }
        if ("latch".equals(trigger) && !"orbit".equals(behavior)) {
            throw new IllegalArgumentException("models.trigger latch needs behavior orbit (it binds each latched target)");
        }
        String animSpawn = optString(m, "animSpawn", null);
        String animHold = optString(m, "animHold", null);
        String animEnd = optString(m, "animEnd", null);
        // animAlt: every other fire of the def by one caster holds this
        // clip instead of animHold (alternating tilts, left and right swings)
        String animAlt = optString(m, "animAlt", null);
        for (String[] pair : new String[][]{{"animSpawn", animSpawn}, {"animHold", animHold},
                {"animEnd", animEnd}, {"animAlt", animAlt}}) {
            if (pair[1] != null && !isClipName(pair[1])) {
                throw new IllegalArgumentException("models." + pair[0]
                        + " must be a Bedrock clip name (dotted lowercase tokens), got \"" + pair[1] + "\"");
            }
        }
        return new ModelSpec(model,
                (int) optDouble(m, "models.count", "count", 1, 1, 64),
                formation, behavior, trigger,
                optDouble(m, "models.radius", "radius", 0, 0, 64),
                optDouble(m, "models.height", "height", 8, 0, 64),
                (float) optDouble(m, "models.scale", "scale", 1.0, 0.1, 10),
                (int) optDouble(m, "models.lifetimeTicks", "lifetimeTicks", 0, 0, 72000),
                (int) optDouble(m, "models.descendTicks", "descendTicks", 10, 1, 7200),
                optDouble(m, "models.fallSpeed", "fallSpeed", 1.2, 0.05, 8),
                optDouble(m, "models.spin", "spin", 0, 0, 720),
                (int) optDouble(m, "models.hangTicks", "hangTicks", 0, 0, 7200),
                optDouble(m, "models.speed", "speed", 0, 0, 16),
                optDouble(m, "models.forward", "forward", 0, -32, 32),
                optDouble(m, "models.up", "up", 0, -32, 32),
                animSpawn, animHold, animEnd,
                (float) optDouble(m, "models.minPower", "minPower", 0, 0, 1), animAlt,
                // spawnTicks: how long orbit, ride and escort pieces play
                // their spawn clip before holding (a titan's full rise)
                (int) optDouble(m, "models.spawnTicks", "spawnTicks", 6, 1, 200));
    }

    private static final Set<String> BEAM_FIELDS = Set.of(
            "length", "radius", "damageInterval", "form", "endBurstRadius");

    private static BeamParams parseBeam(JsonObject json) {
        JsonObject b = reqObject(json, "beam");
        rejectUnknown(b, BEAM_FIELDS, "beam");
        String form = optString(b, "form", "energy");
        if (!form.equals("energy") && !form.equals("solid")) {
            throw new IllegalArgumentException("beam.form must be energy or solid, got \"" + form + "\"");
        }
        return new BeamParams(
                reqDouble(b, "beam.length", "length", 1, 128),
                optDouble(b, "beam.radius", "radius", 0.6, 0.1, 8),
                (int) optDouble(b, "beam.damageInterval", "damageInterval", 4, 1, 100),
                form,
                optDouble(b, "beam.endBurstRadius", "endBurstRadius", 0, 0, 16));
    }

    private static final Set<String> SWARM_FIELDS = Set.of(
            "count", "budget", "shardBase", "shardPerStat", "shardStat", "slamRadius",
            "interceptRadius", "assignRadius", "drainPercentPerMinute", "recallCooldownTicks",
            "starveCooldownTicks", "lockoutTicks");

    private static SwarmParams parseSwarm(JsonObject json) {
        JsonObject sw = reqObject(json, "swarm");
        rejectUnknown(sw, SWARM_FIELDS, "swarm");
        String shardStat = optString(sw, "shardStat", "strength");
        if (!STATS.contains(shardStat)) {
            throw new IllegalArgumentException("swarm.shardStat must be one of " + STATS
                    + ", got \"" + shardStat + "\"");
        }
        return new SwarmParams(
                (int) optDouble(sw, "swarm.count", "count", 10, 1, 16),
                optDouble(sw, "swarm.budget", "budget", 15000, 1, 1e6),
                optDouble(sw, "swarm.shardBase", "shardBase", 100, 0, 1e6),
                optDouble(sw, "swarm.shardPerStat", "shardPerStat", 0.5, -100, 100),
                shardStat,
                optDouble(sw, "swarm.slamRadius", "slamRadius", 6, 0, 16),
                optDouble(sw, "swarm.interceptRadius", "interceptRadius", 12, 0, 32),
                optDouble(sw, "swarm.assignRadius", "assignRadius", 40, 0, 64),
                optDouble(sw, "swarm.drainPercentPerMinute", "drainPercentPerMinute", 20, 0, 100),
                (int) optDouble(sw, "swarm.recallCooldownTicks", "recallCooldownTicks", 600, 0, 72000),
                (int) optDouble(sw, "swarm.starveCooldownTicks", "starveCooldownTicks", 3600, 0, 72000),
                (int) optDouble(sw, "swarm.lockoutTicks", "lockoutTicks", 20, 0, 72000));
    }

    private static final Set<String> WAVE_KNOCKBACK_FIELDS = Set.of("out", "up");
    private static final Set<String> WAVE_DEFLECT_FIELDS = Set.of("scale", "claim");

    private static KnockbackSpec parseWaveKnockback(JsonObject waveJson) {
        if (!waveJson.has("knockback")) {
            return null;
        }
        JsonObject k = reqObject(waveJson, "knockback");
        rejectUnknown(k, WAVE_KNOCKBACK_FIELDS, "wave.knockback");
        return new KnockbackSpec(
                optDouble(k, "wave.knockback.out", "out", 0, 0, 4),
                optDouble(k, "wave.knockback.up", "up", 0, 0, 4));
    }

    private static WaveDeflectSpec parseWaveDeflect(JsonObject waveJson) {
        if (!waveJson.has("deflect")) {
            return null;
        }
        JsonObject d = reqObject(waveJson, "deflect");
        rejectUnknown(d, WAVE_DEFLECT_FIELDS, "wave.deflect");
        return new WaveDeflectSpec(
                optDouble(d, "wave.deflect.scale", "scale", 2.2, 0.1, 4),
                namedBool(d, "wave.deflect.claim", "claim", false));
    }

    private static final Set<String> VORTEX_FIELDS = Set.of("lift", "swirl");
    private static final Set<String> STASIS_FIELDS = Set.of("mode", "factor");
    private static final Set<String> INSIDE_EFFECT_FIELDS = Set.of("effect", "durationTicks", "intervalTicks");
    private static final Set<String> CHAINS_FIELDS = Set.of("maxLinks");
    private static final Set<String> BURST_FIELDS = Set.of("damageScale", "knockback");
    private static final Set<String> FINALE_FIELDS = Set.of(
            "damageScale", "radiusScale", "knockback", "slamDown", "strikes");

    private static VortexSpec parseVortex(JsonObject cloudJson) {
        if (!cloudJson.has("vortex")) {
            return null;
        }
        JsonObject v = reqObject(cloudJson, "vortex");
        rejectUnknown(v, VORTEX_FIELDS, "cloud.vortex");
        return new VortexSpec(
                optDouble(v, "cloud.vortex.lift", "lift", 0, 0, 2),
                optDouble(v, "cloud.vortex.swirl", "swirl", 0, 0, 4));
    }

    private static StasisSpec parseStasis(JsonObject cloudJson) {
        if (!cloudJson.has("stasis")) {
            return null;
        }
        JsonObject st = reqObject(cloudJson, "stasis");
        rejectUnknown(st, STASIS_FIELDS, "cloud.stasis");
        String mode = reqString(st, "mode");
        if (!mode.equals("freeze") && !mode.equals("slow")) {
            throw new IllegalArgumentException("cloud.stasis.mode must be freeze or slow, got \"" + mode + "\"");
        }
        return new StasisSpec(mode, optDouble(st, "cloud.stasis.factor", "factor", 0.05, 0, 1));
    }

    private static InsideEffectSpec parseInsideEffect(JsonObject cloudJson) {
        if (!cloudJson.has("insideEffect")) {
            return null;
        }
        JsonObject ie = reqObject(cloudJson, "insideEffect");
        rejectUnknown(ie, INSIDE_EFFECT_FIELDS, "cloud.insideEffect");
        return new InsideEffectSpec(
                optEffectId(ie, "cloud.insideEffect.effect", "effect", true),
                (int) optDouble(ie, "cloud.insideEffect.durationTicks", "durationTicks", 40, 1, 72000),
                (int) optDouble(ie, "cloud.insideEffect.intervalTicks", "intervalTicks", 20, 1, 72000));
    }

    private static ChainsSpec parseChains(JsonObject cloudJson) {
        if (!cloudJson.has("chains")) {
            return null;
        }
        JsonObject ch = reqObject(cloudJson, "chains");
        rejectUnknown(ch, CHAINS_FIELDS, "cloud.chains");
        return new ChainsSpec((int) reqDouble(ch, "cloud.chains.maxLinks", "maxLinks", 1, 16));
    }

    private static BurstSpec parseBurst(JsonObject cloudJson) {
        if (!cloudJson.has("onSpawnBurst")) {
            return null;
        }
        JsonObject b = reqObject(cloudJson, "onSpawnBurst");
        rejectUnknown(b, BURST_FIELDS, "cloud.onSpawnBurst");
        return new BurstSpec(
                optDouble(b, "cloud.onSpawnBurst.damageScale", "damageScale", 1, 0, 10),
                optDouble(b, "cloud.onSpawnBurst.knockback", "knockback", 0, 0, 8));
    }

    private static FinaleSpec parseFinale(JsonObject cloudJson) {
        if (!cloudJson.has("finale")) {
            return null;
        }
        JsonObject f = reqObject(cloudJson, "finale");
        rejectUnknown(f, FINALE_FIELDS, "cloud.finale");
        return new FinaleSpec(
                optDouble(f, "cloud.finale.damageScale", "damageScale", 1, 0, 10),
                optDouble(f, "cloud.finale.radiusScale", "radiusScale", 1, 0.1, 4),
                optDouble(f, "cloud.finale.knockback", "knockback", 0, -8, 8),
                optDouble(f, "cloud.finale.slamDown", "slamDown", 0, 0, 4),
                (int) optDouble(f, "cloud.finale.strikes", "strikes", 0, 0, 16));
    }

    private static final Set<String> ATMOSPHERE_FIELDS = Set.of("tint", "strength", "density");

    /**
     * Parses the optional atmosphere object inside the cloud block
     * (Phase 5). tint is required inside the object (a fog shift
     * with no color is meaningless); strength and density default
     * to 0.6 and 0.5. Errors name the exact field
     * (cloud.atmosphere.strength and so on).
     */
    private static CloudAtmosphere parseAtmosphere(JsonObject cloudJson) {
        if (!cloudJson.has("atmosphere")) {
            return null;
        }
        JsonObject a = reqObject(cloudJson, "atmosphere");
        for (String key : a.keySet()) {
            if (!ATMOSPHERE_FIELDS.contains(key)) {
                throw new IllegalArgumentException("unknown field \"cloud.atmosphere." + key + "\"");
            }
        }
        int tint;
        try {
            tint = parseColor(reqString(a, "tint"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("cloud.atmosphere.tint: " + e.getMessage());
        }
        return new CloudAtmosphere(tint,
                (float) optDouble(a, "cloud.atmosphere.strength", "strength", 0.6, 0.0, 1.0),
                (float) optDouble(a, "cloud.atmosphere.density", "density", 0.5, 0.0, 1.0));
    }

    private static final Set<String> FEEDBACK_EVENTS = Set.of("onFire", "onCarrierHit", "onCarrierEnd");
    private static final Set<String> SHAKE_FIELDS = Set.of("strength", "durationTicks", "radius");

    /** Hard ceilings for data-authored shakes; see ShakeSpec. */
    public static final float MAX_SHAKE_STRENGTH = 3.0f;
    public static final int MAX_SHAKE_DURATION = 40;
    public static final float MAX_SHAKE_RADIUS = 64.0f;

    /**
     * Parses the optional feedback block (Phase 5): up to one shake
     * per event key. Unknown event keys, unknown shake fields,
     * wrong types, and out-of-range values are rejected naming the
     * exact field (feedback.onFire.strength and so on), like every
     * other block. strength is required inside a shake object;
     * durationTicks defaults to 10 and radius to 16.
     */
    private static FeedbackSpec parseFeedback(JsonObject json) {
        if (!json.has("feedback")) {
            return FeedbackSpec.NONE;
        }
        JsonObject f = reqObject(json, "feedback");
        for (String key : f.keySet()) {
            if (!FEEDBACK_EVENTS.contains(key)) {
                throw new IllegalArgumentException("unknown feedback event \"" + key
                        + "\" (expected onFire, onCarrierHit, or onCarrierEnd)");
            }
        }
        return new FeedbackSpec(
                parseShake(f, "onFire"),
                parseShake(f, "onCarrierHit"),
                parseShake(f, "onCarrierEnd"));
    }

    private static ShakeSpec parseShake(JsonObject feedbackJson, String eventKey) {
        if (!feedbackJson.has(eventKey)) {
            return null;
        }
        String at = "feedback." + eventKey;
        JsonObject s = reqObject(feedbackJson, eventKey);
        for (String key : s.keySet()) {
            if (!SHAKE_FIELDS.contains(key)) {
                throw new IllegalArgumentException("unknown field \"" + at + "." + key + "\"");
            }
        }
        return new ShakeSpec(
                (float) reqDouble(s, at + ".strength", "strength", 0.01, MAX_SHAKE_STRENGTH),
                (int) optDouble(s, at + ".durationTicks", "durationTicks", 10, 1, MAX_SHAKE_DURATION),
                (float) optDouble(s, at + ".radius", "radius", 16, 1, MAX_SHAKE_RADIUS));
    }

    /** A required numeric field whose errors report displayName instead of the raw key. */
    private static double reqDouble(JsonObject json, String displayName, String field, double min, double max) {
        JsonElement el = json.get(field);
        if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("field \"" + displayName + "\" is required and must be a number");
        }
        double v = el.getAsDouble();
        if (Double.isNaN(v) || v < min || v > max) {
            throw new IllegalArgumentException("field \"" + displayName + "\" must be between " + min + " and " + max + ", got " + v);
        }
        return v;
    }

    private static final Set<String> GEO_BLOCKS = Set.of("wave", "cloud", "charge", "projectile", "ground");
    private static final Set<String> GEO_GROUND_FIELDS = Set.of(
            "wallSlabs", "wallHeight", "wallArc", "crackCount", "crackRadius",
            "spikes", "spikeRadius", "spikeHeight");
    private static final Set<String> GEO_WAVE_FIELDS = Set.of("turbulence", "baseRing");
    private static final Set<String> GEO_CLOUD_FIELDS = Set.of(
            "shellLayers", "swirlSpeed", "shellAlpha", "rimGlow", "strikeHueSplit");
    private static final Set<String> GEO_CHARGE_FIELDS = Set.of("raisedRing", "ringHeight", "ringWidth");
    private static final Set<String> GEO_PROJECTILE_FIELDS = Set.of("casingForm");

    /**
     * Parses the optional geometry block (Phase 4): the primitive
     * kit options per surface. Validated like every other block:
     * unknown blocks, unknown fields, wrong types, and
     * out-of-range values are rejected naming the exact field
     * (geometry.wave.turbulence and so on). An absent block, or
     * an absent sub-block, yields GeometrySpec.DEFAULT for that
     * surface, which is the Phase 1 to 3 rendering.
     */
    private static GeometrySpec parseGeometry(JsonObject json) {
        if (!json.has("geometry")) {
            return GeometrySpec.DEFAULT;
        }
        JsonObject g = reqObject(json, "geometry");
        for (String key : g.keySet()) {
            if (!GEO_BLOCKS.contains(key)) {
                throw new IllegalArgumentException("unknown geometry block \"" + key + "\"");
            }
        }
        GeometrySpec d = GeometrySpec.DEFAULT;

        WaveGeometry wave = d.wave();
        if (g.has("wave")) {
            JsonObject w = reqObject(g, "wave");
            for (String key : w.keySet()) {
                if (!GEO_WAVE_FIELDS.contains(key)) {
                    throw new IllegalArgumentException("unknown field \"geometry.wave." + key + "\"");
                }
            }
            wave = new WaveGeometry(
                    (float) optDouble(w, "geometry.wave.turbulence", "turbulence", 0.0, 0.0, 1.0),
                    geoBool(w, "geometry.wave.baseRing", "baseRing", true));
        }

        CloudGeometry cloud = d.cloud();
        if (g.has("cloud")) {
            JsonObject c = reqObject(g, "cloud");
            for (String key : c.keySet()) {
                if (!GEO_CLOUD_FIELDS.contains(key)) {
                    throw new IllegalArgumentException("unknown field \"geometry.cloud." + key + "\"");
                }
            }
            cloud = new CloudGeometry(
                    (int) optDouble(c, "geometry.cloud.shellLayers", "shellLayers", 2, 1, 3),
                    (float) optDouble(c, "geometry.cloud.swirlSpeed", "swirlSpeed", 0.0, 0.0, 4.0),
                    (float) optDouble(c, "geometry.cloud.shellAlpha", "shellAlpha", 1.0, 0.0, 2.0),
                    (float) optDouble(c, "geometry.cloud.rimGlow", "rimGlow", 0.0, 0.0, 2.0),
                    geoBool(c, "geometry.cloud.strikeHueSplit", "strikeHueSplit", false));
        }

        ChargeGeometry charge = d.charge();
        if (g.has("charge")) {
            JsonObject c = reqObject(g, "charge");
            for (String key : c.keySet()) {
                if (!GEO_CHARGE_FIELDS.contains(key)) {
                    throw new IllegalArgumentException("unknown field \"geometry.charge." + key + "\"");
                }
            }
            charge = new ChargeGeometry(
                    geoBool(c, "geometry.charge.raisedRing", "raisedRing", false),
                    (float) optDouble(c, "geometry.charge.ringHeight", "ringHeight", 1.1, 0.2, 4.0),
                    (float) optDouble(c, "geometry.charge.ringWidth", "ringWidth", 0.22, 0.05, 1.0));
        }

        ProjectileGeometry projectile = d.projectile();
        if (g.has("projectile")) {
            JsonObject p = reqObject(g, "projectile");
            for (String key : p.keySet()) {
                if (!GEO_PROJECTILE_FIELDS.contains(key)) {
                    throw new IllegalArgumentException(
                            "unknown field \"geometry.projectile." + key + "\"");
                }
            }
            String casingForm = optString(p, "casingForm", "shard");
            if (!casingForm.equals("shard") && !casingForm.equals("plate")) {
                throw new IllegalArgumentException("geometry.projectile.casingForm must be shard "
                        + "or plate, got \"" + casingForm + "\"");
            }
            projectile = new ProjectileGeometry(casingForm);
        }

        GroundGeometry ground = d.ground();
        if (g.has("ground")) {
            JsonObject gr = reqObject(g, "ground");
            for (String key : gr.keySet()) {
                if (!GEO_GROUND_FIELDS.contains(key)) {
                    throw new IllegalArgumentException("unknown field \"geometry.ground." + key + "\"");
                }
            }
            ground = new GroundGeometry(
                    (int) optDouble(gr, "geometry.ground.wallSlabs", "wallSlabs", 0, 0, 16),
                    optDouble(gr, "geometry.ground.wallHeight", "wallHeight", 3.0, 0.5, 12),
                    optDouble(gr, "geometry.ground.wallArc", "wallArc", 0.45, -2, 2),
                    (int) optDouble(gr, "geometry.ground.crackCount", "crackCount", 0, 0, 16),
                    optDouble(gr, "geometry.ground.crackRadius", "crackRadius", 0, 0, 64),
                    (int) optDouble(gr, "geometry.ground.spikes", "spikes", 0, 0, 24),
                    optDouble(gr, "geometry.ground.spikeRadius", "spikeRadius", 0, 0, 64),
                    optDouble(gr, "geometry.ground.spikeHeight", "spikeHeight", 2.2, 0.5, 6));
        }

        return new GeometrySpec(wave, cloud, charge, projectile, ground);
    }

    /** A boolean geometry field, with errors naming the full geometry path. */
    private static boolean geoBool(JsonObject json, String displayName, String field, boolean fallback) {
        JsonElement el = json.get(field);
        if (el == null) {
            return fallback;
        }
        if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException("field \"" + displayName + "\" must be a boolean");
        }
        return el.getAsBoolean();
    }

    private static final Set<String> FX_FIELDS = Set.of(
            "event", "entry", "color", "shape", "count", "rate", "radius",
            "speed", "spread", "angle", "size", "sizeEndScale", "lifetime",
            "gravity", "drag", "alpha", "cap", "capGroup");

    /**
     * Parses the optional fx block: a list of emission recipes
     * (Phase 3). Every field is validated like the rest of the
     * schema; errors name the recipe index and the exact field
     * (fx[2].entry and so on), and unknown events, entries, shapes,
     * and fields are rejected by name. Burst events require a
     * count and forbid a rate; continuous events the reverse, so a
     * recipe that could never emit anything fails loudly here
     * instead of silently doing nothing in game.
     */
    private static List<FxRecipe> parseFx(JsonObject json) {
        List<FxRecipe> out = new ArrayList<>();
        if (!json.has("fx")) {
            return out;
        }
        JsonElement el = json.get("fx");
        if (el == null || !el.isJsonArray()) {
            throw new IllegalArgumentException("field \"fx\" must be an array of recipe objects");
        }
        JsonArray array = el.getAsJsonArray();
        for (int i = 0; i < array.size(); i++) {
            String at = "fx[" + i + "]";
            JsonElement item = array.get(i);
            if (item == null || !item.isJsonObject()) {
                throw new IllegalArgumentException(at + " must be an object");
            }
            JsonObject r = item.getAsJsonObject();
            for (String key : r.keySet()) {
                if (!FX_FIELDS.contains(key)) {
                    throw new IllegalArgumentException("unknown field \"" + at + "." + key + "\"");
                }
            }
            FxEvent event;
            try {
                event = FxEvent.parse(reqString(r, "event"));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(at + ".event: " + e.getMessage());
            }
            int entry;
            try {
                entry = AbilityFxEntries.byName(reqString(r, "entry"));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(at + ".entry: " + e.getMessage());
            }
            FxShape shape;
            try {
                shape = FxShape.parse(reqString(r, "shape"));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(at + ".shape: " + e.getMessage());
            }

            String colorRef = "mid";
            Integer colorLiteral = null;
            if (r.has("color")) {
                String color = reqString(r, "color");
                if (color.startsWith("#")) {
                    try {
                        colorLiteral = parseColor(color);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException(at + ".color: " + e.getMessage());
                    }
                } else if (color.equals("core") || color.equals("mid") || color.equals("rim")) {
                    colorRef = color;
                } else {
                    throw new IllegalArgumentException(at + ".color must be core, mid, rim, or a "
                            + "#RRGGBB color, got \"" + color + "\"");
                }
            }

            int count = (int) optDouble(r, at + ".count", "count", 0, 0, 4096);
            double rate = optDouble(r, at + ".rate", "rate", 0, 0, 100000);
            if (event.isBurst()) {
                if (count < 1) {
                    throw new IllegalArgumentException(at + ".count must be >= 1 for burst event "
                            + event.dataName());
                }
                if (rate > 0) {
                    throw new IllegalArgumentException(at + ".rate only applies to continuous "
                            + "events (carrier_tick, charge_tick), not " + event.dataName());
                }
            } else {
                if (rate <= 0) {
                    throw new IllegalArgumentException(at + ".rate must be > 0 for continuous event "
                            + event.dataName());
                }
                if (count > 0) {
                    throw new IllegalArgumentException(at + ".count only applies to burst events, not "
                            + event.dataName());
                }
            }

            double radius = optDouble(r, at + ".radius", "radius", -1, -1, 512);
            if (radius == 0 || (radius > -1 && radius < 0)) {
                throw new IllegalArgumentException(at + ".radius must be > 0, or -1 for the carrier default");
            }
            double[] size = optRange(r, at + ".size", "size", 0.2, 0.3, 0.01, 64);
            double[] lifetime = optRange(r, at + ".lifetime", "lifetime", 10, 20, 1, 72000);
            Integer cap = null;
            if (r.has("cap")) {
                int capValue = (int) optDouble(r, at + ".cap", "cap", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
                if (capValue < 1) {
                    throw new IllegalArgumentException(at + ".cap must be >= 1, got " + capValue);
                }
                cap = capValue;
            }
            String capGroup = optString(r, "capGroup", null);
            if (capGroup != null && capGroup.isEmpty()) {
                throw new IllegalArgumentException(at + ".capGroup must be a non-empty name");
            }

            out.add(new FxRecipe(event, entry, colorRef, colorLiteral, shape, count, rate, radius,
                    optDouble(r, at + ".speed", "speed", 0.1, -10, 10),
                    optDouble(r, at + ".spread", "spread", 0.02, 0, 10),
                    optDouble(r, at + ".angle", "angle", 35, 1, 179),
                    size[0], size[1],
                    optDouble(r, at + ".sizeEndScale", "sizeEndScale", 1.0, 0.0, 10),
                    (int) lifetime[0], (int) lifetime[1],
                    optDouble(r, at + ".gravity", "gravity", 0.0, -1, 1),
                    optDouble(r, at + ".drag", "drag", 0.98, 0.0, 1.0),
                    optDouble(r, at + ".alpha", "alpha", 1.0, 0.0, 1.0),
                    cap, capGroup));
        }
        return out;
    }

    /**
     * A [min, max] range field: either a bare number (both ends) or
     * a two-element array. Each end must sit inside [min, max] and
     * the ends must be ordered; errors name the field.
     */
    private static double[] optRange(JsonObject json, String displayName, String field,
                                     double defMin, double defMax, double min, double max) {
        JsonElement el = json.get(field);
        if (el == null) {
            return new double[]{defMin, defMax};
        }
        double lo;
        double hi;
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
            lo = hi = el.getAsDouble();
        } else if (el.isJsonArray() && el.getAsJsonArray().size() == 2
                && el.getAsJsonArray().get(0).isJsonPrimitive()
                && el.getAsJsonArray().get(0).getAsJsonPrimitive().isNumber()
                && el.getAsJsonArray().get(1).isJsonPrimitive()
                && el.getAsJsonArray().get(1).getAsJsonPrimitive().isNumber()) {
            lo = el.getAsJsonArray().get(0).getAsDouble();
            hi = el.getAsJsonArray().get(1).getAsDouble();
        } else {
            throw new IllegalArgumentException("field \"" + displayName
                    + "\" must be a number or a [min, max] pair");
        }
        if (Double.isNaN(lo) || Double.isNaN(hi) || lo < min || hi > max) {
            throw new IllegalArgumentException("field \"" + displayName + "\" must be between "
                    + min + " and " + max + ", got [" + lo + ", " + hi + "]");
        }
        if (lo > hi) {
            throw new IllegalArgumentException("field \"" + displayName
                    + "\" must have min <= max, got [" + lo + ", " + hi + "]");
        }
        return new double[]{lo, hi};
    }

    private static final Set<String> ANIMATION_FIELDS = Set.of(
            "id", "charge", "strikeAt", "followTicks", "channel");

    /**
     * Parses the optional animation block. Clip ids are Bedrock
     * animation names (dotted lowercase tokens); existence of the
     * clip is a client concern (the client logs and skips a missing
     * clip), but a malformed id or an out-of-range phase value is a
     * data error and is rejected here, naming the exact field.
     */
    private static AnimationSpec parseAnimation(JsonObject json) {
        if (!json.has("animation")) {
            return null;
        }
        JsonObject a = reqObject(json, "animation");
        for (String key : a.keySet()) {
            if (!ANIMATION_FIELDS.contains(key)) {
                throw new IllegalArgumentException("unknown animation field \"" + key + "\"");
            }
        }
        String id = reqString(a, "id");
        if (!isClipName(id)) {
            throw new IllegalArgumentException(
                    "animation.id must be a Bedrock clip name (dotted lowercase tokens), got \"" + id + "\"");
        }
        String charge = optString(a, "charge", null);
        if (charge != null && !isClipName(charge)) {
            throw new IllegalArgumentException(
                    "animation.charge must be a Bedrock clip name (dotted lowercase tokens), got \"" + charge + "\"");
        }
        float strikeAt = (float) optDouble(a, "animation.strikeAt", "strikeAt", 0.5, 0.05, 0.95);
        int followTicks = (int) optDouble(a, "animation.followTicks", "followTicks", 10, 0, 200);
        boolean channel = optBool(a, "channel", false);
        return new AnimationSpec(id, charge, strikeAt, followTicks, channel);
    }

    private static boolean isClipName(String s) {
        if (s == null || s.isEmpty() || s.startsWith(".") || s.endsWith(".") || s.contains("..")) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '.';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    /** Total Ki price for a caster with the given max Ki. */
    public double kiPrice(double maxKi) {
        return kiCost + maxKi * kiCostPercent / 100.0;
    }

    static int parseColor(String s) {
        if (s == null || !s.startsWith("#") || (s.length() != 7 && s.length() != 9)) {
            throw new IllegalArgumentException("color must be #RRGGBB or #AARRGGBB, got \"" + s + "\"");
        }
        try {
            long v = Long.parseLong(s.substring(1), 16);
            if (s.length() == 7) {
                v |= 0xFF000000L;
            }
            return (int) v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("color is not valid hex: \"" + s + "\"");
        }
    }

    static int parseMaterial(String name) {
        return switch (name) {
            case "plasma" -> 0;
            case "smoke" -> 1;
            case "vapor" -> 2;
            case "dust" -> 3;
            case "mist" -> 4;
            case "soul" -> 5;
            case "darkness" -> 6;
            case "glass_dust" -> 7;
            default -> throw new IllegalArgumentException("visual.particleMaterial is not a known material: \"" + name + "\"");
        };
    }

    private static String reqString(JsonObject json, String field) {
        JsonElement el = json.get(field);
        if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("field \"" + field + "\" is required and must be a string");
        }
        return el.getAsString();
    }

    private static String optString(JsonObject json, String field, String fallback) {
        JsonElement el = json.get(field);
        if (el == null) {
            return fallback;
        }
        if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("field \"" + field + "\" must be a string");
        }
        return el.getAsString();
    }

    private static boolean optBool(JsonObject json, String field, boolean fallback) {
        JsonElement el = json.get(field);
        if (el == null) {
            return fallback;
        }
        if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException("field \"" + field + "\" must be a boolean");
        }
        return el.getAsBoolean();
    }

    private static JsonObject reqObject(JsonObject json, String field) {
        JsonElement el = json.get(field);
        if (el == null || !el.isJsonObject()) {
            throw new IllegalArgumentException("field \"" + field + "\" is required and must be an object");
        }
        return el.getAsJsonObject();
    }

    private static int reqInt(JsonObject json, String field, int min, int max) {
        return (int) reqDouble(json, field, min, max);
    }

    private static double reqDouble(JsonObject json, String field, double min, double max) {
        JsonElement el = json.get(field);
        if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("field \"" + field + "\" is required and must be a number");
        }
        double v = el.getAsDouble();
        if (Double.isNaN(v) || v < min || v > max) {
            throw new IllegalArgumentException("field \"" + field + "\" must be between " + min + " and " + max + ", got " + v);
        }
        return v;
    }

    private static double optDouble(JsonObject json, String field, double fallback, double min, double max) {
        return optDouble(json, field, field, fallback, min, max);
    }

    /** As optDouble, but errors report displayName instead of the raw key. */
    private static double optDouble(JsonObject json, String displayName, String field,
                                    double fallback, double min, double max) {
        JsonElement el = json.get(field);
        if (el == null) {
            return fallback;
        }
        if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("field \"" + displayName + "\" must be a number");
        }
        double v = el.getAsDouble();
        if (Double.isNaN(v) || v < min || v > max) {
            throw new IllegalArgumentException("field \"" + displayName + "\" must be between " + min + " and " + max + ", got " + v);
        }
        return v;
    }
}
