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
        // Yardrat
        List<Technique> yardrat = new ArrayList<>();
        yardrat.add(new Technique("Fly", 1, "Flight depletion (1.0 Ki/sec)"));
        yardrat.add(new Technique("Meditation", 5, "Posture freezing movement to increase Ki recovery by 5.0x"));
        yardrat.add(new Technique("Instant Transmission", 15, "Teleports player up to 128 blocks based on Willpower"));
        yardrat.add(new Technique("Size Alteration", 20, "Scales player size between 0.25x and 2.0x"));
        yardrat.add(new Technique("Chroma Stasis", 30, "Immobilizes target in mid-air for 4 seconds"));
        yardrat.add(new Technique("Healing", 40, "Channeled 20 Ki/sec to apply Instant Health I"));
        yardrat.add(new Technique("Cloning Technique", 50, "Spawns a movement-mimicking helper entity"));
        yardrat.add(new Technique("Spirit Fission", 60, "Strike applying fusion disruption effect"));
        RACE_TECHNIQUES.put("dragonblockarcanedba:yardrat", yardrat);

        // Human
        List<Technique> human = new ArrayList<>();
        human.add(new Technique("Fly", 1, "Flight depletion (1.0 Ki/sec)"));
        human.add(new Technique("Meditation", 5, "Posture freezing movement to increase Ki recovery by 5.0x"));
        human.add(new Technique("Dodon Ray", 10, "Willpower-scaling linear damage beam"));
        human.add(new Technique("Solar Flare", 15, "12-block blindness flash (6s)"));
        human.add(new Technique("Sky Dance", 25, "Offensive dash trail"));
        human.add(new Technique("Multi Form", 40, "Spawns 2 clones with 50% stats"));
        human.add(new Technique("Overdrive", 50, "Drains 15 stamina/sec to double stats"));
        RACE_TECHNIQUES.put("dragonblockarcanedba:human", human);

        // Namekian
        List<Technique> namekian = new ArrayList<>();
        namekian.add(new Technique("Fly", 1, "Flight depletion (1.0 Ki/sec)"));
        namekian.add(new Technique("Meditation", 5, "Posture freezing movement to increase Ki recovery by 5.0x"));
        namekian.add(new Technique("Clothes Beam", 10, "Adds clothing armor to inventory"));
        namekian.add(new Technique("Regeneration", 20, "Rapid healing (30 Ki/sec)"));
        namekian.add(new Technique("Terrestrial Whip", 30, "Melee reach extended to 8.0 blocks"));
        namekian.add(new Technique("Magic Materialize", 40, "Drops random vanilla utility item"));
        namekian.add(new Technique("Gigantification", 50, "4.0x size, +3 blocks reach, 1.5x damage"));
        namekian.add(new Technique("Namekian Fusion", 60, "Absorbs Namekian NPC for +1 AP"));
        RACE_TECHNIQUES.put("dragonblockarcanedba:namekian", namekian);

        // Saiyan
        List<Technique> saiyan = new ArrayList<>();
        saiyan.add(new Technique("Fly", 1, "Flight depletion (1.0 Ki/sec)"));
        saiyan.add(new Technique("Meditation", 5, "Posture freezing movement to increase Ki recovery by 5.0x"));
        saiyan.add(new Technique("Zenkai Boost", 15, "Combat low-health checks for +10 AP"));
        saiyan.add(new Technique("Oozaru Form", 30, "5.0x size, 3.0x health/damage"));
        saiyan.add(new Technique("Dragon Fist", 50, "Charging strike (3.0x damage, ignores 50% armor)"));
        RACE_TECHNIQUES.put("dragonblockarcanedba:saiyan", saiyan);

        // Half-Saiyan
        List<Technique> halfSaiyan = new ArrayList<>();
        halfSaiyan.add(new Technique("Fly", 1, "Flight depletion (1.0 Ki/sec)"));
        halfSaiyan.add(new Technique("Meditation", 5, "Posture freezing movement to increase Ki recovery by 5.0x"));
        halfSaiyan.add(new Technique("Zenkai Surge", 15, "Low-health chance for +50% combat stats for 60s"));
        halfSaiyan.add(new Technique("Oozaru Form", 30, "5.0x size, 3.0x health/damage"));
        halfSaiyan.add(new Technique("Dragon Fist", 50, "Charging strike (3.0x damage, ignores 50% armor)"));
        RACE_TECHNIQUES.put("dragonblockarcanedba:half_saiyan", halfSaiyan);

        // Majin
        List<Technique> majin = new ArrayList<>();
        majin.add(new Technique("Fly", 1, "Flight depletion (1.0 Ki/sec)"));
        majin.add(new Technique("Meditation", 5, "Posture freezing movement to increase Ki recovery by 5.0x"));
        majin.add(new Technique("Regeneration", 15, "Willpower-scaled recovery"));
        majin.add(new Technique("Absorb", 30, "Absorbs passive animals at low health for level XP"));
        majin.add(new Technique("Fission", 50, "Spawns 100% clone at cost of 50% health"));
        majin.add(new Technique("Pink Horizon", 70, "Traps targets in a 15x15 shell of custom blocks"));
        RACE_TECHNIQUES.put("dragonblockarcanedba:majin", majin);

        // Bio-Android
        List<Technique> bioAndroid = new ArrayList<>();
        bioAndroid.add(new Technique("Fly", 1, "Flight depletion (1.0 Ki/sec)"));
        bioAndroid.add(new Technique("Meditation", 5, "Posture freezing movement to increase Ki recovery by 5.0x"));
        bioAndroid.add(new Technique("Zenkai Boost", 15, "Combat low-health checks for +10 AP"));
        bioAndroid.add(new Technique("Regeneration", 25, "Rapid healing based on Ki"));
        bioAndroid.add(new Technique("Instant Transmission", 35, "Teleports player based on Willpower"));
        bioAndroid.add(new Technique("Multi Form", 45, "Spawns clone minions"));
        bioAndroid.add(new Technique("Swarm", 60, "Spawns 3 custom aggressive minions"));
        RACE_TECHNIQUES.put("dragonblockarcanedba:bio_android", bioAndroid);

        // Tuffle
        List<Technique> tuffle = new ArrayList<>();
        tuffle.add(new Technique("Fly", 1, "Flight depletion (1.0 Ki/sec)"));
        tuffle.add(new Technique("Meditation", 5, "Posture freezing movement to increase Ki recovery by 5.0x"));
        RACE_TECHNIQUES.put("dragonblockarcanedba:tuffle", tuffle);

        // Arcosian
        List<Technique> arcosian = new ArrayList<>();
        arcosian.add(new Technique("Universal Survival", 1, "Passive ability to breathe underwater and survive in space"));
        arcosian.add(new Technique("Fly", 5, "Flight depletion (1.0 Ki/sec)"));
        arcosian.add(new Technique("Meditation", 10, "Posture freezing movement to increase Ki recovery by 5.0x"));
        RACE_TECHNIQUES.put("dragonblockarcanedba:arcosian", arcosian);
    }

    /**
     * Gets the list of techniques for a given race ID.
     */
    public static List<Technique> getTechniquesForRace(Identifier raceId) {
        return RACE_TECHNIQUES.getOrDefault(raceId.toString(), List.of());
    }
}
