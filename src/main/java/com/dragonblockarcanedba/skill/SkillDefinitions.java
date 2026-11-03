package com.dragonblockarcanedba.skill;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.registry.Technique;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Per-race skill trees shown by the Skills tab.
 * {@code TechniqueRegistry.getTechniquesForRace} returns these lists.
 * Ki Sense and Sickle of Sorrow stay on every tree and keep their own handlers.
 */
public final class SkillDefinitions {
    public enum Kind {
        STRENGTH,
        DEXTERITY,
        VITALITY,
        MAX_KI,
        KI_RECOVERY,
        STAMINA,
        DAMAGE_REDUCTION
    }

    private record Bonus(Kind kind, double perLevel) {}

    private static final Technique KI_SENSE = new Technique(
            "ki_sense",
            "Ki Sense",
            1,
            15,
            "Senses entity health bars and player Ki within range.",
            List.of(),
            -32,
            0,
            "core");

    private static final Technique SICKLE = new Technique(
            "sickle_of_sorrow",
            "Sickle of Sorrow",
            5,
            25,
            "Summons ethereal dimensional scythe to rend reality.",
            List.of(),
            32,
            0,
            "core");

    private static final Map<String, List<Technique>> TREES = new LinkedHashMap<>();
    private static final Map<String, Bonus> BONUSES = new LinkedHashMap<>();

    static {
        put("saiyan", saiyan());
        put("half_saiyan", halfSaiyan());
        put("human", human());
        put("namekian", namekian());
        put("majin", majin());
        put("yardrat", yardrat());
        put("tuffle", tuffle());
        put("android", android());
        put("bio_android", bioAndroid());
        put("neo_tuffle", neoTuffle());
        put("arcosian", arcosian());
    }

    private SkillDefinitions() {}

    public static Map<String, List<Technique>> byRaceKey() {
        return TREES;
    }

    public static boolean isPassive(String techniqueId) {
        if (techniqueId == null || techniqueId.isEmpty()) {
            return false;
        }
        String path = techniqueId.contains(":") ? techniqueId.substring(techniqueId.indexOf(':') + 1) : techniqueId;
        return BONUSES.containsKey(path);
    }

    public static List<Technique> forRace(Identifier raceId) {
        if (raceId == null) {
            return List.of();
        }
        List<Technique> tree = TREES.get(raceId.toString());
        return tree == null ? List.of() : tree;
    }

    /**
     * Flat bonus added after the race multiplier and before a transformation multiplier.
     * Strength feeds attack damage, Dexterity feeds movement speed, and Vitality feeds max health.
     */
    public static double flat(PlayerStatsAccessor accessor, String statName) {
        Kind kind = switch (statName) {
            case "strength" -> Kind.STRENGTH;
            case "dexterity" -> Kind.DEXTERITY;
            case "vitality" -> Kind.VITALITY;
            default -> null;
        };
        if (kind == null) {
            return 0.0;
        }
        return sum(accessor, kind);
    }

    /**
     * Multiplier for max Ki, Ki recovery, or max Stamina. Other kinds return 1.
     */
    public static double factor(PlayerStatsAccessor accessor, Kind kind) {
        if (kind != Kind.MAX_KI && kind != Kind.KI_RECOVERY && kind != Kind.STAMINA) {
            return 1.0;
        }
        return 1.0 + sum(accessor, kind);
    }

    /**
     * Extra damage reduction added on top of the defense formula, as a fraction of incoming damage.
     */
    public static double fraction(PlayerStatsAccessor accessor, Kind kind) {
        if (kind != Kind.DAMAGE_REDUCTION) {
            return 0.0;
        }
        return sum(accessor, kind);
    }

    private static double sum(PlayerStatsAccessor accessor, Kind kind) {
        if (accessor == null || kind == null) {
            return 0.0;
        }
        List<Technique> tree = forRace(accessor.dba$getRaceId());
        double total = 0.0;
        for (int i = 0; i < tree.size(); i++) {
            Technique tech = tree.get(i);
            Bonus bonus = BONUSES.get(tech.id());
            if (bonus == null || bonus.kind() != kind) {
                continue;
            }
            int level = accessor.dba$getTechniqueLevel(tech.id());
            if (level <= 0) {
                continue;
            }
            total += bonus.perLevel() * level;
        }
        return total;
    }

    private static void put(String path, List<Technique> tree) {
        TREES.put("dragonblockarcanedba:" + path, List.copyOf(tree));
    }

    private static void shared(List<Technique> tree) {
        tree.add(KI_SENSE);
        tree.add(SICKLE);
    }

    private static void add(List<Technique> tree, String group, String id, String name,
                            int unlockLevel, int apCost, int x, int y,
                            Kind kind, double perLevel, String... prereqs) {
        tree.add(new Technique(id, name, unlockLevel, apCost, describe(kind, perLevel), List.of(prereqs), x, y, group));
        BONUSES.put(id, new Bonus(kind, perLevel));
    }

    private static String describe(Kind kind, double perLevel) {
        double max = perLevel * 10.0;
        return switch (kind) {
            case STRENGTH -> "Passive. +" + num(perLevel) + " effective Strength per level (up to +" + num(max)
                    + "), raising attack damage.";
            case DEXTERITY -> "Passive. +" + num(perLevel) + " effective Dexterity per level (up to +" + num(max)
                    + "), raising movement speed.";
            case VITALITY -> "Passive. +" + num(perLevel) + " effective Vitality per level (up to +" + num(max)
                    + "), raising max health.";
            case MAX_KI -> "Passive. +" + num(perLevel * 100.0) + "% max Ki per level (up to +" + num(max * 100.0) + "%).";
            case KI_RECOVERY -> "Passive. +" + num(perLevel * 100.0) + "% Ki recovery per level (up to +" + num(max * 100.0) + "%).";
            case STAMINA -> "Passive. +" + num(perLevel * 100.0) + "% max Stamina per level (up to +" + num(max * 100.0) + "%).";
            case DAMAGE_REDUCTION -> "Passive. +" + num(perLevel * 100.0) + "% damage reduction per level (up to +"
                    + num(max * 100.0) + "%).";
        };
    }

    private static String num(double value) {
        String text = String.format(Locale.US, "%.1f", value);
        if (text.endsWith(".0")) {
            return text.substring(0, text.length() - 2);
        }
        return text;
    }

    private static List<Technique> saiyan() {
        List<Technique> tree = new ArrayList<>();
        shared(tree);
        String g = "celestial";
        add(tree, g, "saiyan_warriors_pride", "Warrior's Pride", 1, 10, -130, -100, Kind.STRENGTH, 1.0);
        add(tree, g, "saiyan_zenkai_rush", "Zenkai Rush", 5, 12, -210, -160, Kind.VITALITY, 0.4, "saiyan_warriors_pride");
        add(tree, g, "saiyan_tail_balance", "Tail Balance", 5, 12, -210, -40, Kind.DEXTERITY, 1.5, "saiyan_warriors_pride");
        add(tree, g, "saiyan_battle_hide", "Battle Hide", 8, 15, -290, -160, Kind.DAMAGE_REDUCTION, 0.005, "saiyan_zenkai_rush");
        add(tree, g, "saiyan_low_g_drill", "Low-G Drill", 8, 15, -290, -40, Kind.DEXTERITY, 1.0, "saiyan_tail_balance");
        add(tree, g, "saiyan_battle_hunger", "Battle Hunger", 10, 18, -130, 70, Kind.MAX_KI, 0.02, "saiyan_warriors_pride");
        add(tree, g, "saiyan_primal_roar", "Primal Roar", 12, 18, -210, 140, Kind.KI_RECOVERY, 0.02, "saiyan_battle_hunger");
        add(tree, g, "saiyan_overwhelming_force", "Overwhelming Force", 16, 25, -290, 70, Kind.STRENGTH, 2.0, "saiyan_battle_hide", "saiyan_low_g_drill");
        return tree;
    }

    private static List<Technique> halfSaiyan() {
        List<Technique> tree = new ArrayList<>();
        shared(tree);
        String g = "astral";
        add(tree, g, "half_saiyan_hidden_potential", "Hidden Potential", 1, 10, 130, -110, Kind.MAX_KI, 0.02);
        add(tree, g, "half_saiyan_human_grit", "Human Grit", 5, 12, 210, -170, Kind.VITALITY, 0.2, "half_saiyan_hidden_potential");
        add(tree, g, "half_saiyan_hybrid_focus", "Hybrid Focus", 5, 12, 210, -50, Kind.KI_RECOVERY, 0.02, "half_saiyan_hidden_potential");
        add(tree, g, "half_saiyan_adaptive_footwork", "Adaptive Footwork", 8, 15, 290, -170, Kind.DEXTERITY, 1.0, "half_saiyan_human_grit");
        add(tree, g, "half_saiyan_crossed_guard", "Crossed Guard", 8, 15, 290, -50, Kind.DAMAGE_REDUCTION, 0.005, "half_saiyan_hybrid_focus");
        add(tree, g, "half_saiyan_second_wind", "Second Wind", 10, 16, 210, 50, Kind.STAMINA, 0.02, "half_saiyan_human_grit");
        add(tree, g, "half_saiyan_mixed_blow", "Mixed Blow", 12, 18, 290, 50, Kind.STRENGTH, 1.5, "half_saiyan_hybrid_focus");
        add(tree, g, "half_saiyan_limit_break", "Limit Break", 16, 25, 370, -50, Kind.MAX_KI, 0.01, "half_saiyan_adaptive_footwork", "half_saiyan_mixed_blow");
        return tree;
    }

    private static List<Technique> human() {
        List<Technique> tree = new ArrayList<>();
        shared(tree);
        String g = "core";
        add(tree, g, "human_martial_discipline", "Martial Discipline", 1, 10, -170, 110, Kind.STRENGTH, 1.0);
        add(tree, g, "human_focused_breathing", "Focused Breathing", 1, 10, 0, 130, Kind.KI_RECOVERY, 0.02);
        add(tree, g, "human_measured_footwork", "Measured Footwork", 1, 10, 170, 110, Kind.DEXTERITY, 1.0);
        add(tree, g, "human_guard_stance", "Guard Stance", 6, 14, -250, 190, Kind.DAMAGE_REDUCTION, 0.005, "human_martial_discipline");
        add(tree, g, "human_spirit_well", "Spirit Well", 6, 14, 0, 210, Kind.MAX_KI, 0.02, "human_focused_breathing");
        add(tree, g, "human_stamina_reserve", "Stamina Reserve", 6, 14, 250, 190, Kind.STAMINA, 0.02, "human_measured_footwork");
        add(tree, g, "human_enduring_body", "Enduring Body", 12, 18, -120, 270, Kind.VITALITY, 0.4, "human_guard_stance", "human_spirit_well");
        add(tree, g, "human_mastered_strike", "Mastered Strike", 16, 22, 120, 270, Kind.STRENGTH, 1.5, "human_enduring_body", "human_stamina_reserve");
        return tree;
    }

    private static List<Technique> namekian() {
        List<Technique> tree = new ArrayList<>();
        shared(tree);
        String g = "arcane";
        add(tree, g, "namekian_regeneration", "Regeneration", 1, 10, -120, -140, Kind.VITALITY, 0.4);
        add(tree, g, "namekian_clan_wisdom", "Clan Wisdom", 4, 12, -200, -210, Kind.KI_RECOVERY, 0.02, "namekian_regeneration");
        add(tree, g, "namekian_swamp_stride", "Swamp Stride", 4, 12, -200, -70, Kind.DEXTERITY, 1.0, "namekian_regeneration");
        add(tree, g, "namekian_elastic_guard", "Elastic Guard", 8, 15, -290, -240, Kind.DAMAGE_REDUCTION, 0.005, "namekian_clan_wisdom");
        add(tree, g, "namekian_mystic_pool", "Mystic Pool", 8, 15, -200, -280, Kind.MAX_KI, 0.02, "namekian_clan_wisdom");
        add(tree, g, "namekian_long_reach", "Long Reach", 10, 16, -290, -70, Kind.STRENGTH, 1.0, "namekian_swamp_stride");
        add(tree, g, "namekian_dragon_heart", "Dragon Heart", 14, 20, -290, -160, Kind.VITALITY, 0.2, "namekian_elastic_guard", "namekian_mystic_pool");
        add(tree, g, "namekian_warrior_clan", "Warrior Clan", 16, 22, -380, -160, Kind.STAMINA, 0.01, "namekian_long_reach", "namekian_dragon_heart");
        return tree;
    }

    private static List<Technique> majin() {
        List<Technique> tree = new ArrayList<>();
        shared(tree);
        String g = "abyssal";
        add(tree, g, "majin_elastic_body", "Elastic Body", 1, 10, 140, 100, Kind.DAMAGE_REDUCTION, 0.01);
        add(tree, g, "majin_wicked_vitality", "Wicked Vitality", 5, 12, 220, 40, Kind.VITALITY, 0.4, "majin_elastic_body");
        add(tree, g, "majin_stolen_ki", "Stolen Ki", 5, 14, 220, 170, Kind.MAX_KI, 0.02, "majin_elastic_body");
        add(tree, g, "majin_bouncing_step", "Bouncing Step", 8, 15, 310, 40, Kind.DEXTERITY, 1.5, "majin_wicked_vitality");
        add(tree, g, "majin_bottomless_stomach", "Bottomless Stomach", 8, 15, 310, 170, Kind.STAMINA, 0.02, "majin_stolen_ki");
        add(tree, g, "majin_playful_recovery", "Playful Recovery", 10, 16, 220, 240, Kind.KI_RECOVERY, 0.01, "majin_stolen_ki");
        add(tree, g, "majin_rubber_wall", "Rubber Wall", 12, 18, 400, 40, Kind.DAMAGE_REDUCTION, 0.005, "majin_bouncing_step");
        add(tree, g, "majin_innocent_might", "Innocent Might", 16, 25, 400, 170, Kind.STRENGTH, 2.0, "majin_bottomless_stomach", "majin_rubber_wall");
        return tree;
    }

    private static List<Technique> yardrat() {
        List<Technique> tree = new ArrayList<>();
        shared(tree);
        String g = "astral";
        add(tree, g, "yardrat_spirit_step", "Spirit Step", 1, 10, 120, -150, Kind.DEXTERITY, 2.0);
        add(tree, g, "yardrat_calm_mind", "Calm Mind", 4, 12, 200, -220, Kind.KI_RECOVERY, 0.02, "yardrat_spirit_step");
        add(tree, g, "yardrat_ki_shroud", "Ki Shroud", 4, 12, 200, -80, Kind.DAMAGE_REDUCTION, 0.005, "yardrat_spirit_step");
        add(tree, g, "yardrat_forced_spirit", "Forced Spirit", 8, 16, 290, -220, Kind.MAX_KI, 0.02, "yardrat_calm_mind");
        add(tree, g, "yardrat_light_body", "Light Body", 8, 14, 290, -80, Kind.DEXTERITY, 1.0, "yardrat_ki_shroud");
        add(tree, g, "yardrat_spirit_breath", "Spirit Breath", 10, 16, 200, -290, Kind.STAMINA, 0.02, "yardrat_calm_mind");
        add(tree, g, "yardrat_spirit_palm", "Spirit Palm", 12, 18, 380, -80, Kind.STRENGTH, 1.0, "yardrat_light_body");
        add(tree, g, "yardrat_instant_rhythm", "Instant Rhythm", 16, 22, 290, -150, Kind.KI_RECOVERY, 0.01, "yardrat_spirit_palm", "yardrat_spirit_breath");
        return tree;
    }

    private static List<Technique> tuffle() {
        List<Technique> tree = new ArrayList<>();
        shared(tree);
        String g = "arcane";
        add(tree, g, "tuffle_machine_precision", "Machine Precision", 1, 12, -150, 120, Kind.DEXTERITY, 1.5);
        add(tree, g, "tuffle_alloy_plating", "Alloy Plating", 5, 14, -230, 60, Kind.DAMAGE_REDUCTION, 0.005, "tuffle_machine_precision");
        add(tree, g, "tuffle_power_cell", "Power Cell", 5, 14, -230, 180, Kind.MAX_KI, 0.02, "tuffle_machine_precision");
        add(tree, g, "tuffle_overclock", "Overclock", 8, 16, -320, 180, Kind.KI_RECOVERY, 0.02, "tuffle_power_cell");
        add(tree, g, "tuffle_servo_strike", "Servo Strike", 8, 16, -320, 60, Kind.STRENGTH, 1.5, "tuffle_alloy_plating");
        add(tree, g, "tuffle_coolant_loop", "Coolant Loop", 12, 18, -410, 180, Kind.VITALITY, 0.2, "tuffle_overclock");
        add(tree, g, "tuffle_gyro_stabilizer", "Gyro Stabilizer", 12, 16, -410, 60, Kind.DEXTERITY, 1.0, "tuffle_servo_strike");
        add(tree, g, "tuffle_prototype_heart", "Prototype Heart", 16, 22, -410, 120, Kind.STAMINA, 0.02, "tuffle_coolant_loop", "tuffle_gyro_stabilizer");
        return tree;
    }

    private static List<Technique> android() {
        List<Technique> tree = new ArrayList<>();
        shared(tree);
        String g = "celestial";
        add(tree, g, "android_infinite_reactor", "Infinite Reactor", 1, 10, -160, -30, Kind.KI_RECOVERY, 0.03);
        add(tree, g, "android_alloy_chassis", "Alloy Chassis", 5, 14, -250, -100, Kind.DAMAGE_REDUCTION, 0.005, "android_infinite_reactor");
        add(tree, g, "android_hydraulic_strike", "Hydraulic Strike", 5, 14, -250, 50, Kind.STRENGTH, 1.5, "android_infinite_reactor");
        add(tree, g, "android_servo_legs", "Servo Legs", 8, 15, -340, -100, Kind.DEXTERITY, 1.5, "android_alloy_chassis");
        add(tree, g, "android_energy_capacitor", "Energy Capacitor", 8, 16, -160, -120, Kind.MAX_KI, 0.01, "android_infinite_reactor");
        add(tree, g, "android_redundant_core", "Redundant Core", 12, 18, -160, -200, Kind.VITALITY, 0.2, "android_energy_capacitor");
        add(tree, g, "android_stamina_pump", "Stamina Pump", 12, 16, -340, 50, Kind.STAMINA, 0.02, "android_servo_legs");
        add(tree, g, "android_overdrive", "Overdrive", 16, 25, -340, -30, Kind.STRENGTH, 1.0, "android_redundant_core", "android_stamina_pump");
        return tree;
    }

    private static List<Technique> bioAndroid() {
        List<Technique> tree = new ArrayList<>();
        shared(tree);
        String g = "abyssal";
        add(tree, g, "bio_android_cellular_regen", "Cellular Regen", 1, 10, 160, -20, Kind.VITALITY, 0.5);
        add(tree, g, "bio_android_absorbed_ki", "Absorbed Ki", 5, 14, 250, -100, Kind.MAX_KI, 0.02, "bio_android_cellular_regen");
        add(tree, g, "bio_android_adaptive_carapace", "Adaptive Carapace", 5, 14, 250, 60, Kind.DAMAGE_REDUCTION, 0.005, "bio_android_cellular_regen");
        add(tree, g, "bio_android_bio_current", "Bio Current", 8, 16, 340, -100, Kind.KI_RECOVERY, 0.02, "bio_android_absorbed_ki");
        add(tree, g, "bio_android_perfect_balance", "Perfect Balance", 8, 15, 340, 60, Kind.DEXTERITY, 1.0, "bio_android_adaptive_carapace");
        add(tree, g, "bio_android_nutrient_store", "Nutrient Store", 10, 16, 250, -180, Kind.STAMINA, 0.02, "bio_android_absorbed_ki");
        add(tree, g, "bio_android_predatory_strength", "Predatory Strength", 12, 20, 430, 60, Kind.STRENGTH, 2.0, "bio_android_perfect_balance");
        add(tree, g, "bio_android_evolved_tissue", "Evolved Tissue", 16, 22, 340, -20, Kind.VITALITY, 0.2, "bio_android_nutrient_store", "bio_android_predatory_strength");
        return tree;
    }

    private static List<Technique> neoTuffle() {
        List<Technique> tree = new ArrayList<>();
        shared(tree);
        String g = "arcane";
        add(tree, g, "neo_tuffle_mutant_core", "Mutant Core", 1, 12, 0, 160, Kind.MAX_KI, 0.03);
        add(tree, g, "neo_tuffle_parasite_grip", "Parasite Grip", 5, 14, -90, 230, Kind.STRENGTH, 1.5, "neo_tuffle_mutant_core");
        add(tree, g, "neo_tuffle_liquid_metal", "Liquid Metal", 5, 14, 90, 230, Kind.DAMAGE_REDUCTION, 0.01, "neo_tuffle_mutant_core");
        add(tree, g, "neo_tuffle_host_sync", "Host Sync", 8, 16, -180, 230, Kind.DEXTERITY, 1.0, "neo_tuffle_parasite_grip");
        add(tree, g, "neo_tuffle_control_wave", "Control Wave", 8, 16, 0, 300, Kind.KI_RECOVERY, 0.02, "neo_tuffle_mutant_core");
        add(tree, g, "neo_tuffle_gel_regeneration", "Gel Regeneration", 12, 18, -90, 360, Kind.VITALITY, 0.4, "neo_tuffle_control_wave");
        add(tree, g, "neo_tuffle_blutz_engine", "Blutz Engine", 12, 16, 180, 300, Kind.STAMINA, 0.01, "neo_tuffle_liquid_metal");
        add(tree, g, "neo_tuffle_dominion", "Dominion", 16, 25, -90, 300, Kind.STRENGTH, 1.0, "neo_tuffle_host_sync", "neo_tuffle_gel_regeneration");
        return tree;
    }

    private static List<Technique> arcosian() {
        List<Technique> tree = new ArrayList<>();
        shared(tree);
        String g = "celestial";
        add(tree, g, "arcosian_royal_guard", "Royal Guard", 1, 12, 0, -150, Kind.DAMAGE_REDUCTION, 0.01);
        add(tree, g, "arcosian_cold_reservoir", "Cold Reservoir", 5, 14, -90, -220, Kind.MAX_KI, 0.02, "arcosian_royal_guard");
        add(tree, g, "arcosian_suppressed_power", "Suppressed Power", 5, 14, 90, -220, Kind.STRENGTH, 1.5, "arcosian_royal_guard");
        add(tree, g, "arcosian_tail_discipline", "Tail Discipline", 8, 15, 180, -220, Kind.DEXTERITY, 1.0, "arcosian_suppressed_power");
        add(tree, g, "arcosian_empire_conditioning", "Empire Conditioning", 8, 16, -180, -220, Kind.VITALITY, 0.2, "arcosian_cold_reservoir");
        add(tree, g, "arcosian_frozen_focus", "Frozen Focus", 12, 18, -90, -300, Kind.KI_RECOVERY, 0.02, "arcosian_cold_reservoir");
        add(tree, g, "arcosian_pride_reserve", "Pride Reserve", 12, 16, 180, -300, Kind.STAMINA, 0.02, "arcosian_tail_discipline");
        add(tree, g, "arcosian_final_reserve", "Final Reserve", 16, 25, 0, -300, Kind.STRENGTH, 1.5, "arcosian_empire_conditioning", "arcosian_pride_reserve");
        return tree;
    }
}
