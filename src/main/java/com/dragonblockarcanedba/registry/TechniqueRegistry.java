package com.dragonblockarcanedba.registry;

import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry holding the specific technique trees for each race.
 */
public class TechniqueRegistry {
    private static final Map<String, List<Technique>> RACE_TECHNIQUES = new HashMap<>();

    static {
        Technique kiSense = new Technique("Ki Sense", 1, 5, "Senses entity health bars within 15 blocks. Drains 1 Ki/sec.");
        
        for (String race : new String[]{"yardrat", "human", "namekian", "saiyan", "half_saiyan", "majin", "bio_android", "tuffle", "arcosian"}) {
            List<Technique> list = new ArrayList<>();
            list.add(kiSense);
            RACE_TECHNIQUES.put("dragonblockarcanedba:" + race, list);
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
