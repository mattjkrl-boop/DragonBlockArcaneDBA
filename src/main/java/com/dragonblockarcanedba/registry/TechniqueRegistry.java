package com.dragonblockarcanedba.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry holding the specific technique trees for each race,
 * dynamically loaded from skill_tree.json with automatic fallback.
 */
public class TechniqueRegistry {
    private static final Map<String, List<Technique>> RACE_TECHNIQUES = new HashMap<>();

    static {
        loadSkillTree();
    }

    public static synchronized void loadSkillTree() {
        RACE_TECHNIQUES.clear();
        List<Technique> loadedTechs = new ArrayList<>();

        try (InputStream is = TechniqueRegistry.class.getResourceAsStream("/data/dragonblockarcanedba/skill_tree.json")) {
            if (is != null) {
                JsonObject root = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
                if (root.has("nodes")) {
                    for (JsonElement elem : root.getAsJsonArray("nodes")) {
                        JsonObject obj = elem.getAsJsonObject();
                        String id = obj.get("id").getAsString();
                        String name = obj.get("name").getAsString();
                        int unlockLevel = obj.has("unlockLevel") ? obj.get("unlockLevel").getAsInt() : 1;
                        int apCost = obj.has("apCost") ? obj.get("apCost").getAsInt() : 0;
                        String desc = obj.has("description") ? obj.get("description").getAsString() : "";
                        int x = obj.has("x") ? obj.get("x").getAsInt() : 0;
                        int y = obj.has("y") ? obj.get("y").getAsInt() : 0;
                        String group = obj.has("group") ? obj.get("group").getAsString() : "core";

                        List<String> prereqs = new ArrayList<>();
                        if (obj.has("prerequisites")) {
                            for (JsonElement p : obj.getAsJsonArray("prerequisites")) {
                                prereqs.add(p.getAsString());
                            }
                        }

                        loadedTechs.add(new Technique(id, name, unlockLevel, apCost, desc, prereqs, x, y, group));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DragonBlockArcaneDBA] Failed to load skill_tree.json: " + e.getMessage());
        }

        // Fallback if file was not found
        if (loadedTechs.isEmpty()) {
            loadedTechs.add(new Technique("ki_sense", "Ki Sense", 1, 15, "Senses entity health bars and player Ki within range.", List.of(), -150, -27));
            loadedTechs.add(new Technique("sickle_of_sorrow", "Sickle of Sorrow", 5, 25, "Summons ethereal dimensional scythe to rend reality.", List.of(), -150, 55));
        }

        for (String race : new String[]{"yardrat", "human", "namekian", "saiyan", "half_saiyan", "majin", "bio_android", "tuffle", "arcosian"}) {
            RACE_TECHNIQUES.put("dragonblockarcanedba:" + race, new ArrayList<>(loadedTechs));
        }
    }

    /**
     * Gets the list of techniques for a given race ID.
     */
    public static List<Technique> getTechniquesForRace(Identifier raceId) {
        return RACE_TECHNIQUES.getOrDefault(raceId.toString(), List.of());
    }

    public static Technique getTechnique(Identifier id) {
        if (id == null) return null;
        String searchId = id.getPath();
        for (List<Technique> techs : RACE_TECHNIQUES.values()) {
            for (Technique tech : techs) {
                if (tech.id().equals(searchId)) {
                    return tech;
                }
            }
        }
        return null;
    }

    /**
     * Gets all unique techniques across all races.
     */
    public static List<Technique> getAllTechniques() {
        List<Technique> all = new ArrayList<>();
        for (List<Technique> list : RACE_TECHNIQUES.values()) {
            for (Technique t : list) {
                if (!all.contains(t)) {
                    all.add(t);
                }
            }
        }
        return all;
    }
}
