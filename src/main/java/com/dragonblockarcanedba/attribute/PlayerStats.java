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
        // Lowered XP scaling to make leveling faster
        return (int) (50 * Math.pow(currentLevel, 1.05));
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
        
        switch (raceId.toLowerCase().replace("dragonblockarcanedba:", "")) {
            case "saiyan": 
                // Exponential: 5 * (1.1 ^ x)
                return (int) Math.ceil(5.0 * Math.pow(1.10, x));
                
            case "half-saiyan":
                // Spiky: Cost fluctuates. Base curve is lower exponential, but spikes every 10 levels.
                double baseSpiky = 5.0 * Math.pow(1.08, x);
                if (x % 10 == 9) return (int) Math.ceil(baseSpiky * 3.0);
                return (int) Math.ceil(baseSpiky);
                
            case "human":
                // Logarithmic: Starts at 5, scales incredibly slowly early, then hits a wall.
                if (x > 40) return 500; // Hit a severe plateau
                return (int) Math.ceil(5.0 + (15.0 * Math.log(x + 1)));
                
            case "namekian":
                // Linear: Steady growth
                return 5 + (x * 2);
                
            case "arcosian":
                // Flatline: 5 for a long time, then skyrockets.
                if (x <= 15) return 5;
                return (int) Math.ceil(50.0 * Math.pow(1.5, (x - 15)));
                
            case "bio-android":
                // Stepped: Jumps every 10 levels.
                int step = x / 10;
                if (step == 0) return 5;
                if (step == 1) return 15;
                if (step == 2) return 30;
                if (step == 3) return 60;
                return 100 + (step * 50);
                
            case "majin":
                // Late Bloomer: High early AP costs, drops mid-game, then stabilizes.
                if (x <= 10) return 20 - x; // 20 -> 10
                if (x <= 20) return 10;
                return (int) Math.ceil(10.0 * Math.pow(1.05, x - 20));
                
            case "yardrat":
                // Utility Focused: Physical stats hard-cap very quickly.
                if ((statName.equals("strength") || statName.equals("defense")) && x >= 10) {
                    return 9999;
                }
                return (int) Math.ceil(5.0 * Math.pow(1.06, x));
                
            case "tuffle":
                // Reverse Exponential: Starts insanely expensive, gets progressively cheaper.
                int cost = (int) Math.ceil(100.0 * Math.pow(0.95, x));
                return Math.max(5, cost);
        }
        
        return (int) Math.ceil(5.0 * Math.pow(1.08, x));
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
}
