package com.dragonblockarcanedba.registry;

import com.dragonblockarcanedba.skill.SkillDefinitions;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Race skill trees. The node lists come from {@link SkillDefinitions}.
 */
public class TechniqueRegistry {
    private static final Map<String, List<Technique>> RACE_TECHNIQUES = new HashMap<>();

    static {
        loadSkillTree();
    }

    public static synchronized void loadSkillTree() {
        RACE_TECHNIQUES.clear();
        RACE_TECHNIQUES.putAll(SkillDefinitions.byRaceKey());
    }

    /**
     * Gets the list of techniques for a given race ID.
     */
    public static List<Technique> getTechniquesForRace(Identifier raceId) {
        if (raceId == null) {
            return List.of();
        }
        return RACE_TECHNIQUES.getOrDefault(raceId.toString(), List.of());
    }

    public static Technique getTechnique(Identifier id) {
        if (id == null) {
            return null;
        }
        String searchId = id.getPath();
        for (List<Technique> techs : RACE_TECHNIQUES.values()) {
            for (int i = 0; i < techs.size(); i++) {
                Technique tech = techs.get(i);
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
            for (int i = 0; i < list.size(); i++) {
                Technique tech = list.get(i);
                if (!all.contains(tech)) {
                    all.add(tech);
                }
            }
        }
        return all;
    }
}
