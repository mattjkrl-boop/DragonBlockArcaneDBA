package com.dragonblockarcanedba.attribute;

import com.dragonblockarcanedba.registry.DbaRegistries;
import com.dragonblockarcanedba.registry.Race;
import com.dragonblockarcanedba.registry.Form;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.Identifier;

public class PlayerStats {
    public static final double BASE_MAX_KI = 200.0;
    public static final double BASE_MAX_STAMINA = 100.0;
    public static final double BASE_KI_RECOVERY = 1.0; // Per second

    public static double getMaxKi(Player player) {
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
        int spirit = accessor.dba$getSpirit();
        
        // Base formula: Max Ki = 200 + (Spirit * 50)
        double baseMaxKi = BASE_MAX_KI + (spirit * 50.0);
        
        // Apply multipliers from race and active form
        double multiplier = 1.0;
        Race race = DbaRegistries.getRace(accessor.dba$getRaceId());
        if (race != null) {
            multiplier += race.getStatMultipliers().kiCapacity() / 100.0;
        }
        
        Identifier formId = accessor.dba$getActiveFormId();
        if (formId != null) {
            Form form = DbaRegistries.getForm(formId);
            if (form != null) {
                multiplier *= (form.getStatMultipliers().kiCapacity() / 100.0);
            }
        }
        
        return baseMaxKi * multiplier;
    }

    public static double getMaxStamina(Player player) {
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
        int dexterity = accessor.dba$getDexterity();
        
        // Base formula: Max Stamina = 100 + (Dexterity * 10)
        double baseMaxStamina = BASE_MAX_STAMINA + (dexterity * 10.0);
        
        // Multipliers can be added later if races/forms affect stamina
        double multiplier = 1.0;
        
        return baseMaxStamina * multiplier;
    }

    public static double getKiRecovery(Player player) {
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
        int willpower = accessor.dba$getWillpower();
        
        // Base formula: Recovery = 1.0 + (Willpower * 0.25)
        double baseRecovery = BASE_KI_RECOVERY + (willpower * 0.25);
        
        // Apply multipliers from race and active form
        double multiplier = 1.0;
        Race race = DbaRegistries.getRace(accessor.dba$getRaceId());
        if (race != null) {
            multiplier += race.getStatMultipliers().kiControl() / 100.0;
        }
        
        Identifier formId = accessor.dba$getActiveFormId();
        if (formId != null) {
            Form form = DbaRegistries.getForm(formId);
            if (form != null) {
                multiplier *= (form.getStatMultipliers().kiControl() / 100.0);
            }
        }
        
        return baseRecovery * multiplier;
    }

    public static double getEffectiveStat(Player player, String statName) {
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
        Race race = DbaRegistries.getRace(accessor.dba$getRaceId());
        if (race == null) return 1.0;

        int allocated = 0;
        int baseVal = 0;
        double raceMult = 0.0;

        switch (statName) {
            case "strength" -> {
                allocated = accessor.dba$getStrength();
                baseVal = race.getBaseStats().strength();
                raceMult = race.getStatMultipliers().strength() / 100.0;
            }
            case "dexterity" -> {
                allocated = accessor.dba$getDexterity();
                baseVal = race.getBaseStats().agility();
                raceMult = race.getStatMultipliers().agility() / 100.0;
            }
            case "defense" -> {
                allocated = accessor.dba$getDefense();
                baseVal = race.getBaseStats().defense();
                raceMult = race.getStatMultipliers().defense() / 100.0;
            }
            case "willpower" -> {
                allocated = accessor.dba$getWillpower();
                baseVal = race.getBaseStats().kiControl();
                raceMult = race.getStatMultipliers().kiControl() / 100.0;
            }
            case "spirit" -> {
                allocated = accessor.dba$getSpirit();
                // We'll reuse kiCapacity or kiControl for spirit if needed, defaulting to 0 for now
                baseVal = race.getBaseStats().kiCapacity();
                raceMult = race.getStatMultipliers().kiCapacity() / 100.0;
            }
            case "vitality" -> {
                allocated = accessor.dba$getVitality();
                baseVal = 10; // arbitrary base
                raceMult = 0.0; // no explicit multiplier in current JSONs
            }
        }

        double value = (baseVal + allocated) * (1.0 + raceMult);

        // Apply Transformation Multiplier if active
        Identifier formId = accessor.dba$getActiveFormId();
        if (formId != null) {
            Form form = DbaRegistries.getForm(formId);
            if (form != null) {
                double formMult = 1.0;
                switch (statName) {
                    case "strength" -> formMult = form.getStatMultipliers().strength() / 100.0;
                    case "dexterity" -> formMult = form.getStatMultipliers().agility() / 100.0;
                    case "defense" -> formMult = form.getStatMultipliers().defense() / 100.0;
                    case "willpower" -> formMult = form.getStatMultipliers().kiControl() / 100.0;
                }
                value *= formMult;
            }
        }

        return value;
    }

    public static int getXpToNextLevel(int currentLevel) {
        return (int) (100 * Math.pow(currentLevel, 1.1));
    }

    public static int getUpgradeCost(int currentUpgradeLevel) {
        if (currentUpgradeLevel <= 30) {
            return (int) Math.ceil(5.0 * Math.pow(1.082636, currentUpgradeLevel - 1));
        } else {
            return (int) Math.ceil(50.0 * Math.pow(1.041318, currentUpgradeLevel - 30));
        }
    }

    public static int getFormMasteryXpToNextLevel(int currentMasteryLevel) {
        return (int) (100 * Math.pow(currentMasteryLevel, 1.1));
    }

    public static double getDamageMultiplier(Player player) {
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
        int defense = accessor.dba$getDefense();

        // Scale defense for a max level of 5000. 
        // 5000 defense gives a 55% damage reduction.
        double reduction = defense * 0.00011;
        if (reduction > 0.55) {
            reduction = 0.55;
        }

        double multiplier = 1.0 - reduction;
        
        // Hard-cap damage reduction at 55% (multiplier cannot go below 0.45)
        return Math.max(0.45, multiplier);
    }
}
