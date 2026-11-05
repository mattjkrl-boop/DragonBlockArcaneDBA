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
        GeometrySpec geometry
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
        WAVE, CLOUD, PROJECTILE;

        public static Carrier parse(String s) {
            return switch (s) {
                case "wave" -> WAVE;
                case "cloud" -> CLOUD;
                case "projectile" -> PROJECTILE;
                default -> throw new IllegalArgumentException("carrier must be wave, cloud, or projectile, got \"" + s + "\"");
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

    public record WaveParams(double speed, double radius, double height, double arcDegrees) {}

    public record CloudParams(double radiusStart, double radiusEnd, double height, String anchor,
                              int strikeInterval, int telegraphTicks, double strikeRadius) {}

    public record ProjectileParams(double speed, double gravityScale, double aoeRadius,
                                   String casing, double lengthScale) {}

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

    public record GeometrySpec(WaveGeometry wave, CloudGeometry cloud,
                               ChargeGeometry charge, ProjectileGeometry projectile) {
        public static final GeometrySpec DEFAULT = new GeometrySpec(
                new WaveGeometry(0f, true),
                new CloudGeometry(2, 0f, 1f, 0f, false),
                new ChargeGeometry(false, 1.1f, 0.22f),
                new ProjectileGeometry("shard"));
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
            "minPower", "kiCost", "kiCostPercent", "cooldownTicks", "damage", "sounds",
            "visual", "caps", "wave", "cloud", "projectile", "fx", "geometry");

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
        switch (carrier) {
            case WAVE -> {
                JsonObject w = reqObject(json, "wave");
                wave = new WaveParams(
                        reqDouble(w, "speed", 0.001, 100),
                        reqDouble(w, "radius", 0.1, 512),
                        reqDouble(w, "height", 0.1, 512),
                        reqDouble(w, "arcDegrees", 10, 360));
            }
            case CLOUD -> {
                JsonObject c = reqObject(json, "cloud");
                String anchor = optString(c, "anchor", "caster");
                if (!anchor.equals("caster") && !anchor.equals("target")) {
                    throw new IllegalArgumentException("cloud.anchor must be caster or target, got \"" + anchor + "\"");
                }
                int strikeInterval = (int) optDouble(c, "strikeInterval", 0, 0, 72000);
                int telegraph = (int) optDouble(c, "telegraphTicks", 10, 0, 72000);
                if (strikeInterval > 0 && telegraph >= strikeInterval) {
                    throw new IllegalArgumentException("cloud.telegraphTicks must be smaller than cloud.strikeInterval");
                }
                cloud = new CloudParams(
                        reqDouble(c, "radiusStart", 0.1, 512),
                        reqDouble(c, "radiusEnd", 0.1, 512),
                        reqDouble(c, "height", 0.1, 512),
                        anchor, strikeInterval, telegraph,
                        optDouble(c, "strikeRadius", 2.5, 0.1, 512));
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
        }

        List<FxRecipe> fx = parseFx(json);
        GeometrySpec geometry = parseGeometry(json);

        return new AbilityDef(id, weapon, slot, name, carrier, phases, animation,
                chargeRelease, minPower, kiCost, kiCostPercent, cooldownTicks,
                damage, sounds, visual, caps, wave, cloud, projectile, List.copyOf(fx),
                geometry);
    }

    private static final Set<String> GEO_BLOCKS = Set.of("wave", "cloud", "charge", "projectile");
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

        return new GeometrySpec(wave, cloud, charge, projectile);
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
