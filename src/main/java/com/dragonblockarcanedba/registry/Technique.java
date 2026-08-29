package com.dragonblockarcanedba.registry;

import java.util.List;

/**
 * Represents a technique (ability/skill) in a race's skill tree.
 */
public record Technique(
    String id,
    String name,
    int unlockLevel,
    int apCost,
    String description,
    List<String> prerequisiteTechniqueIds,
    int x,
    int y,
    String group
) {
    public Technique(String id, String name, int unlockLevel, int apCost, String description, List<String> prerequisiteTechniqueIds, int x, int y) {
        this(id, name, unlockLevel, apCost, description, prerequisiteTechniqueIds, x, y, "core");
    }

    public Technique(String id, String name, int unlockLevel, int apCost, String description, List<String> prerequisiteTechniqueIds) {
        this(id, name, unlockLevel, apCost, description, prerequisiteTechniqueIds, 0, 0, "core");
    }

    public Technique(String id, String name, int unlockLevel, int apCost, String description) {
        this(id, name, unlockLevel, apCost, description, List.of(), 0, 0);
    }

    public Technique(String id, String name, int unlockLevel, int apCost, String description, String... prerequisites) {
        this(id, name, unlockLevel, apCost, description, List.of(prerequisites), 0, 0);
    }

    public Technique(String name, int unlockLevel, String description) {
        this(name.toLowerCase().replace(" ", "_"), name, unlockLevel, 5, description, List.of(), 0, 0);
    }

    public Technique(String name, int unlockLevel, int apCost, String description) {
        this(name.toLowerCase().replace(" ", "_"), name, unlockLevel, apCost, description, List.of(), 0, 0);
    }

    public Technique(String name, int unlockLevel, int apCost, String description, String... prerequisites) {
        this(name.toLowerCase().replace(" ", "_"), name, unlockLevel, apCost, description, List.of(prerequisites), 0, 0);
    }

    public Technique(String name, int unlockLevel, int apCost, String description, List<String> prerequisites) {
        this(name.toLowerCase().replace(" ", "_"), name, unlockLevel, apCost, description, prerequisites, 0, 0);
    }

    public boolean hasPrerequisites() {
        return prerequisiteTechniqueIds != null && !prerequisiteTechniqueIds.isEmpty();
    }
}
