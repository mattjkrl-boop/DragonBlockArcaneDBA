package com.dragonblockarcanedba.registry;

import com.dragonblockarcanedba.attribute.Attributes;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Race {
    private final Identifier id;
    private final String displayName;
    private final Attributes baseStats;
    private final Attributes statMultipliers;
    private final List<Identifier> compatibleForms;
    
    // Asset Hooks
    private final Identifier modelId;
    private final Identifier baseTexture;
    private final Identifier soundProfile;

    public Race(Identifier id, String displayName, Attributes baseStats, Attributes statMultipliers, 
                List<Identifier> compatibleForms, Identifier modelId, Identifier baseTexture, Identifier soundProfile) {
        this.id = id;
        this.displayName = displayName;
        this.baseStats = baseStats;
        this.statMultipliers = statMultipliers;
        this.compatibleForms = compatibleForms;
        this.modelId = modelId;
        this.baseTexture = baseTexture;
        this.soundProfile = soundProfile;
    }

    public Identifier getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Attributes getBaseStats() { return baseStats; }
    public Attributes getStatMultipliers() { return statMultipliers; }
    public List<Identifier> getCompatibleForms() { return Collections.unmodifiableList(compatibleForms); }
    public Identifier getModelId() { return modelId; }
    public Identifier getBaseTexture() { return baseTexture; }
    public Identifier getSoundProfile() { return soundProfile; }

    public static Race fromJson(Identifier id, JsonObject json) {
        String displayName = json.get("displayName").getAsString();
        Attributes baseStats = Attributes.fromJson(json.getAsJsonObject("baseStats"));
        Attributes statMultipliers = Attributes.fromJson(json.getAsJsonObject("statMultipliers"));
        
        List<Identifier> compatibleForms = new ArrayList<>();
        if (json.has("compatibleForms")) {
            JsonArray formsArray = json.getAsJsonArray("compatibleForms");
            for (int i = 0; i < formsArray.size(); i++) {
                compatibleForms.add(Identifier.parse(formsArray.get(i).getAsString()));
            }
        }

        Identifier modelId = Identifier.parse(json.get("modelId").getAsString());
        Identifier baseTexture = Identifier.parse(json.get("baseTexture").getAsString());
        Identifier soundProfile = Identifier.parse(json.get("soundProfile").getAsString());

        return new Race(id, displayName, baseStats, statMultipliers, compatibleForms, modelId, baseTexture, soundProfile);
    }
}
