package com.dragonblockarcanedba.registry;

import com.dragonblockarcanedba.attribute.Attributes;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Form {
    private final Identifier id;
    private final List<Identifier> compatibleRaces;
    private final int stage;
    private final Attributes statMultipliers;
    private final double baseKiDrain;
    private final double maxMasteryReduction;
    private final UnlockRequirements unlockRequirements;

    // Asset Hooks
    private final Identifier modelOverride;
    private final Identifier auraVisualId;
    private final Identifier transformationSound;

    // Color overrides for transformations (hex strings like "#FFD700" or null for no override)
    private final String hairColorOverride;
    private final String skinColorOverride;

    public Form(Identifier id, List<Identifier> compatibleRaces, int stage, Attributes statMultipliers,
                double baseKiDrain, double maxMasteryReduction, UnlockRequirements unlockRequirements,
                Identifier modelOverride, Identifier auraVisualId, Identifier transformationSound,
                String hairColorOverride, String skinColorOverride) {
        this.id = id;
        this.compatibleRaces = compatibleRaces;
        this.stage = stage;
        this.statMultipliers = statMultipliers;
        this.baseKiDrain = baseKiDrain;
        this.maxMasteryReduction = maxMasteryReduction;
        this.unlockRequirements = unlockRequirements;
        this.modelOverride = modelOverride;
        this.auraVisualId = auraVisualId;
        this.transformationSound = transformationSound;
        this.hairColorOverride = hairColorOverride;
        this.skinColorOverride = skinColorOverride;
    }

    public Identifier getId() { return id; }
    public List<Identifier> getCompatibleRaces() { return Collections.unmodifiableList(compatibleRaces); }
    public int getStage() { return stage; }
    public Attributes getStatMultipliers() { return statMultipliers; }
    public double getBaseKiDrain() { return baseKiDrain; }
    public double getMaxMasteryReduction() { return maxMasteryReduction; }
    public UnlockRequirements getUnlockRequirements() { return unlockRequirements; }
    public Identifier getModelOverride() { return modelOverride; }
    public Identifier getAuraVisualId() { return auraVisualId; }
    public Identifier getTransformationSound() { return transformationSound; }
    /** Hair color override hex string (e.g. "#FFD700" for SSJ gold), or null for no override */
    public String getHairColorOverride() { return hairColorOverride; }
    /** Skin color override hex string, or null for no override */
    public String getSkinColorOverride() { return skinColorOverride; }

    public record UnlockRequirements(
        int minLevel,
        Attributes minStats
    ) {
        public static UnlockRequirements fromJson(JsonObject json) {
            if (json == null) {
                return new UnlockRequirements(0, new Attributes(0, 0, 0, 0, 0));
            }
            int minLevel = json.has("minLevel") ? json.get("minLevel").getAsInt() : 0;
            Attributes minStats = json.has("minStats") 
                ? Attributes.fromJson(json.getAsJsonObject("minStats"))
                : new Attributes(0, 0, 0, 0, 0);
            return new UnlockRequirements(minLevel, minStats);
        }
    }

    public static Form fromJson(Identifier id, JsonObject json) {
        List<Identifier> compatibleRaces = new ArrayList<>();
        if (json.has("compatibleRaces")) {
            JsonArray racesArray = json.getAsJsonArray("compatibleRaces");
            for (int i = 0; i < racesArray.size(); i++) {
                compatibleRaces.add(Identifier.parse(racesArray.get(i).getAsString()));
            }
        }

        int stage = json.get("stage").getAsInt();
        Attributes statMultipliers = Attributes.fromJson(json.getAsJsonObject("statMultipliers"));
        double baseKiDrain = json.get("baseKiDrain").getAsDouble();
        double maxMasteryReduction = json.get("maxMasteryReduction").getAsDouble();
        
        UnlockRequirements unlockRequirements = json.has("unlockRequirements")
            ? UnlockRequirements.fromJson(json.getAsJsonObject("unlockRequirements"))
            : new UnlockRequirements(0, new Attributes(0, 0, 0, 0, 0));

        Identifier modelOverride = json.has("modelOverride") && !json.get("modelOverride").isJsonNull()
            ? Identifier.parse(json.get("modelOverride").getAsString())
            : null;
        
        Identifier auraVisualId = Identifier.parse(json.get("auraVisualId").getAsString());
        Identifier transformationSound = Identifier.parse(json.get("transformationSound").getAsString());

        // Color overrides for transformations (optional, null = no override)
        String hairColorOverride = json.has("hairColorOverride") && !json.get("hairColorOverride").isJsonNull()
            ? json.get("hairColorOverride").getAsString()
            : null;
        String skinColorOverride = json.has("skinColorOverride") && !json.get("skinColorOverride").isJsonNull()
            ? json.get("skinColorOverride").getAsString()
            : null;

        return new Form(id, compatibleRaces, stage, statMultipliers, baseKiDrain, maxMasteryReduction, 
                        unlockRequirements, modelOverride, auraVisualId, transformationSound,
                        hairColorOverride, skinColorOverride);
    }
}
