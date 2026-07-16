package com.dragonblockarcanedba.client.render.geo;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.*;

/**
 * Defines tintable and untintable regions of a race's texture.
 * <p>
 * When an artist exports a model from Blockbench, they define named bone groups
 * (e.g., "head", "body", "hair", "tail", "wings"). This class maps those bone
 * names to tint categories so the renderer knows which bones to color with the
 * player's skin color, hair color, or leave untouched.
 * <p>
 * The mapping is stored per-race in JSON files at:
 * {@code assets/dragonblockarcanedba/geo/region_maps/{race_id}.json}
 * <p>
 * If no custom region map exists, a default humanoid map is used.
 */
public class TextureRegionMap {

    /**
     * What category a bone/region belongs to for color tinting.
     */
    public enum RegionType {
        /** Tinted with the player's chosen skin color */
        BODY,
        /** Tinted with the player's chosen hair color */
        HAIR,
        /** Never tinted — keeps its original texture color (tails, wings, armor, accessories) */
        AVOID
    }

    private final Map<String, RegionType> boneRegions;
    private final RegionType defaultRegion;

    public TextureRegionMap(Map<String, RegionType> boneRegions, RegionType defaultRegion) {
        this.boneRegions = Collections.unmodifiableMap(boneRegions);
        this.defaultRegion = defaultRegion;
    }

    /**
     * Gets the tint category for a given bone name from the GeckoLib model.
     *
     * @param boneName The bone name from the .geo.json model
     * @return The region type determining how this bone should be tinted
     */
    public RegionType getRegionForBone(String boneName) {
        // Check exact match first
        RegionType exact = boneRegions.get(boneName.toLowerCase(Locale.ROOT));
        if (exact != null) return exact;

        // Check prefix matches (e.g., "hair_front" matches "hair" prefix)
        String lowerBone = boneName.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, RegionType> entry : boneRegions.entrySet()) {
            if (lowerBone.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }

        return defaultRegion;
    }

    /**
     * Returns the default humanoid region map suitable for most races.
     * Body parts get skin tint, hair gets hair tint, everything else is avoided.
     */
    public static TextureRegionMap defaultHumanoid() {
        Map<String, RegionType> regions = new HashMap<>();

        // Body regions — tinted with skin color
        regions.put("head", RegionType.BODY);
        regions.put("body", RegionType.BODY);
        regions.put("torso", RegionType.BODY);
        regions.put("chest", RegionType.BODY);
        regions.put("left_arm", RegionType.BODY);
        regions.put("right_arm", RegionType.BODY);
        regions.put("left_leg", RegionType.BODY);
        regions.put("right_leg", RegionType.BODY);
        regions.put("arm", RegionType.BODY);
        regions.put("leg", RegionType.BODY);
        regions.put("hand", RegionType.BODY);
        regions.put("foot", RegionType.BODY);
        regions.put("neck", RegionType.BODY);
        regions.put("face", RegionType.BODY);

        // Hair regions — tinted with hair color
        regions.put("hair", RegionType.HAIR);
        regions.put("hair_front", RegionType.HAIR);
        regions.put("hair_back", RegionType.HAIR);
        regions.put("hair_side", RegionType.HAIR);
        regions.put("hair_top", RegionType.HAIR);
        regions.put("bangs", RegionType.HAIR);
        regions.put("ponytail", RegionType.HAIR);
        regions.put("eyebrow", RegionType.HAIR);

        // Avoid regions — never tinted (keep original texture colors)
        regions.put("tail", RegionType.AVOID);
        regions.put("wing", RegionType.AVOID);
        regions.put("wings", RegionType.AVOID);
        regions.put("antenna", RegionType.AVOID);
        regions.put("horn", RegionType.AVOID);
        regions.put("horns", RegionType.AVOID);
        regions.put("armor", RegionType.AVOID);
        regions.put("clothing", RegionType.AVOID);
        regions.put("clothes", RegionType.AVOID);
        regions.put("eyes", RegionType.AVOID);
        regions.put("eye", RegionType.AVOID);
        regions.put("aura", RegionType.AVOID);
        regions.put("accessory", RegionType.AVOID);
        regions.put("belt", RegionType.AVOID);
        regions.put("boots", RegionType.AVOID);
        regions.put("gloves", RegionType.AVOID);
        regions.put("scouter", RegionType.AVOID);

        return new TextureRegionMap(regions, RegionType.BODY);
    }

    /**
     * Parses a region map from a JSON object.
     * Format:
     * <pre>{
     *   "defaultRegion": "body",
     *   "body": ["head", "torso", "left_arm", "right_arm", "left_leg", "right_leg"],
     *   "hair": ["hair", "hair_front", "bangs"],
     *   "avoid": ["tail", "wings", "armor", "eyes", "scouter"]
     * }</pre>
     */
    public static TextureRegionMap fromJson(JsonObject json) {
        Map<String, RegionType> regions = new HashMap<>();
        RegionType defaultType = RegionType.BODY;

        if (json.has("defaultRegion")) {
            defaultType = parseRegionType(json.get("defaultRegion").getAsString());
        }

        // Parse each category's bone list
        parseCategory(json, "body", RegionType.BODY, regions);
        parseCategory(json, "hair", RegionType.HAIR, regions);
        parseCategory(json, "avoid", RegionType.AVOID, regions);

        return new TextureRegionMap(regions, defaultType);
    }

    private static void parseCategory(JsonObject json, String key, RegionType type, Map<String, RegionType> regions) {
        if (json.has(key) && json.get(key).isJsonArray()) {
            JsonArray bones = json.getAsJsonArray(key);
            for (int i = 0; i < bones.size(); i++) {
                regions.put(bones.get(i).getAsString().toLowerCase(Locale.ROOT), type);
            }
        }
    }

    private static RegionType parseRegionType(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "hair" -> RegionType.HAIR;
            case "avoid", "none", "skip" -> RegionType.AVOID;
            default -> RegionType.BODY;
        };
    }
}
