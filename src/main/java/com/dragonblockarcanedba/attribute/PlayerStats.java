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
        if (currentLevel <= 0) return 50;
        double calc = 40.0 + 35.0 * Math.pow(currentLevel, 1.02);
        if (calc >= Integer.MAX_VALUE || Double.isInfinite(calc) || Double.isNaN(calc)) {
            return Integer.MAX_VALUE;
        }
        return (int) calc;
    }

    public static int getStatGain(String raceId, String statName) {
        switch (raceId.toLowerCase().replace("dragonblockarcanedba:", "")) {
            case "saiyan" -> {
                switch (statName) {
                    case "strength": return 15;
                    case "dexterity": return 12;
                    case "defense": return 10;
                    case "willpower": return 8;
                    case "spirit": return 12;
                    case "vitality": return 40;
                }
            }
            case "half-saiyan" -> {
                switch (statName) {
                    case "strength": return 12;
                    case "dexterity": return 14;
                    case "defense": return 8;
                    case "willpower": return 15;
                    case "spirit": return 18;
                    case "vitality": return 38;
                }
            }
            case "human" -> {
                switch (statName) {
                    case "strength": return 8;
                    case "dexterity": return 16;
                    case "defense": return 8;
                    case "willpower": return 20;
                    case "spirit": return 14;
                    case "vitality": return 35;
                }
            }
            case "namekian" -> {
                switch (statName) {
                    case "strength": return 10;
                    case "dexterity": return 8;
                    case "defense": return 16;
                    case "willpower": return 14;
                    case "spirit": return 12;
                    case "vitality": return 55;
                }
            }
            case "arcosian" -> {
                switch (statName) {
                    case "strength": return 16;
                    case "dexterity": return 14;
                    case "defense": return 14;
                    case "willpower": return 4;
                    case "spirit": return 18;
                    case "vitality": return 45;
                }
            }
            case "bio-android" -> {
                switch (statName) {
                    case "strength": return 13;
                    case "dexterity": return 13;
                    case "defense": return 13;
                    case "willpower": return 13;
                    case "spirit": return 13;
                    case "vitality": return 48;
                }
            }
            case "majin" -> {
                switch (statName) {
                    case "strength": return 14;
                    case "dexterity": return 6;
                    case "defense": return 20;
                    case "willpower": return 6;
                    case "spirit": return 16;
                    case "vitality": return 65;
                }
            }
            case "yardrat" -> {
                switch (statName) {
                    case "strength": return 4;
                    case "dexterity": return 22;
                    case "defense": return 6;
                    case "willpower": return 18;
                    case "spirit": return 12;
                    case "vitality": return 32;
                }
            }
            case "tuffle" -> {
                switch (statName) {
                    case "strength": return 5;
                    case "dexterity": return 10;
                    case "defense": return 6;
                    case "willpower": return 22;
                    case "spirit": return 15;
                    case "vitality": return 32;
                }
            }
        }
        return 5;
    }

    public static int getUpgradeCost(String raceId, String statName, int upgradeCount) {
        int x = upgradeCount;
        double cost = 5.0;
        
        switch (raceId.toLowerCase().replace("dragonblockarcanedba:", "")) {
            case "saiyan": 
                // Exponential: 5 * (1.1 ^ x)
                cost = 5.0 * Math.pow(1.10, x);
                break;
                
            case "half-saiyan":
                // Spiky: Cost fluctuates. Base curve is lower exponential, but spikes every 10 levels.
                double baseSpiky = 5.0 * Math.pow(1.08, x);
                if (x % 10 == 9) cost = baseSpiky * 3.0;
                else cost = baseSpiky;
                break;
                
            case "human":
                // Logarithmic: Starts at 5, scales incredibly slowly early.
                cost = 5.0 + (15.0 * Math.log(x + 1));
                break;
                
            case "namekian":
                // Linear: Steady growth
                cost = 5 + (x * 2);
                break;
                
            case "arcosian":
                // Flatline: 5 for a long time, then skyrockets.
                if (x <= 15) cost = 5.0;
                else cost = 50.0 * Math.pow(1.5, (x - 15));
                break;
                
            case "bio-android":
                // Stepped: Jumps every 10 levels.
                int step = x / 10;
                if (step == 0) cost = 5.0;
                else if (step == 1) cost = 15.0;
                else if (step == 2) cost = 30.0;
                else if (step == 3) cost = 60.0;
                else cost = 100 + (step * 50);
                break;
                
            case "majin":
                // Late Bloomer: High early AP costs, drops mid-game, then stabilizes.
                if (x <= 10) cost = 20 - x; // 20 -> 10
                else if (x <= 20) cost = 10.0;
                else cost = 10.0 * Math.pow(1.05, x - 20);
                break;
                
            case "yardrat":
                // Utility Focused: Original curve kept, no hard caps.
                cost = 5.0 * Math.pow(1.06, x);
                break;
                
            case "tuffle":
                // Reverse Exponential: Starts insanely expensive, gets progressively cheaper.
                cost = 100.0 * Math.pow(0.95, x);
                cost = Math.max(5.0, cost);
                break;
                
            default:
                cost = 5.0 * Math.pow(1.08, x);
                break;
        }
        
        // Prevent overflow issues at extremely high levels by capping at 1 billion AP per upgrade
        if (cost > 1_000_000_000.0 || Double.isInfinite(cost) || Double.isNaN(cost)) {
            return 1_000_000_000;
        }
        
        return (int) Math.ceil(cost);
    }

    public static int getFormMasteryXpToNextLevel(int currentMasteryLevel) {
        return (int) (100 * Math.pow(currentMasteryLevel, 1.1));
    }

    public static double getDamageMultiplier(Player player) {
        double defense = getEffectiveStat(player, "defense");

        // Scale defense without arbitrary limits.
        // E.g. defense * 0.00011 means ~10,000 defense is 110% reduction.
        double reduction = defense * 0.00011;
        
        // Prevent negative damage by capping reduction at 1.0 (100% mitigation)
        if (reduction > 1.0) {
            reduction = 1.0;
        }

        double multiplier = 1.0 - reduction;
        
        // Cap damage reduction to at least 0.0 (100% reduction max)
        return Math.max(0.0, multiplier);
    }

    public static int getTechniqueUpgradeCost(String techId, int targetLevel) {
        int baseCost = 15;
        com.dragonblockarcanedba.registry.Technique tech = com.dragonblockarcanedba.registry.TechniqueRegistry.getTechnique(net.minecraft.resources.Identifier.tryParse(techId));
        if (tech != null) {
            baseCost = tech.apCost();
        } else if ("sickle_of_sorrow".equals(techId)) {
            baseCost = 25;
        }
        if (targetLevel <= 1) {
            return baseCost;
        }
        return (int) Math.round(baseCost * Math.pow(1.45, targetLevel - 1));
    }

    public static int getKiAttackSaveCost(com.dragonblockarcanedba.ki.KiTechniqueType type, int usedPercent, boolean isBarrage) {
        if (type == com.dragonblockarcanedba.ki.KiTechniqueType.EXPLOSION) {
            return 25;
        }
        int base = 5 + (int) Math.round(usedPercent * 0.2);
        if (isBarrage) base += 3;
        return Math.max(5, base);
    }

    public static int getSickleSummonPercent(int level) {
        int lvl = Math.min(10, Math.max(1, level));
        return Math.max(5, 25 - (lvl - 1) * 2);
    }

    public static double getSickleBaseActionDrain(int level) {
        int lvl = Math.min(10, Math.max(1, level));
        return Math.max(6.0, 25.0 - (lvl - 1) * 2.0);
    }

    public static float getKiSenseRange(int level) {
        int lvl = Math.min(10, Math.max(1, level));
        return 15.0f + (lvl - 1) * 5.0f;
    }

    public static double getKiSenseDrainPerSecond(int level) {
        int lvl = Math.min(10, Math.max(1, level));
        return Math.max(0.2, 1.0 - (lvl - 1) * 0.088);
    }
}
