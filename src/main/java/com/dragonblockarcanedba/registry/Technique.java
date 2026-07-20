package com.dragonblockarcanedba.registry;

/**
 * Represents a technique (ability/skill) in a race's skill tree.
 */
public record Technique(String id, String name, int unlockLevel, int apCost, String description) {
    public Technique(String name, int unlockLevel, String description) {
        this(name.toLowerCase().replace(" ", "_"), name, unlockLevel, 5, description);
    }
    
    public Technique(String name, int unlockLevel, int apCost, String description) {
        this(name.toLowerCase().replace(" ", "_"), name, unlockLevel, apCost, description);
    }
}
