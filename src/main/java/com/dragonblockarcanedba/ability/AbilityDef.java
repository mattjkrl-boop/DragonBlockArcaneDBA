package com.dragonblockarcanedba.ability;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

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
        ProjectileParams projectile
) {
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

    private static final Set<String> TOP_LEVEL = Set.of(
            "weapon", "slot", "name", "carrier", "phases", "animation", "chargeRelease",
            "minPower", "kiCost", "kiCostPercent", "cooldownTicks", "damage", "sounds",
            "visual", "caps", "wave", "cloud", "projectile");

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

        return new AbilityDef(id, weapon, slot, name, carrier, phases, animation,
                chargeRelease, minPower, kiCost, kiCostPercent, cooldownTicks,
                damage, sounds, visual, caps, wave, cloud, projectile);
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
