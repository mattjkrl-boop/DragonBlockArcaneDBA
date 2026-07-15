package com.dragonblockarcanedba.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class DbaConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "dragonblockarcanedba.json");

    public static double baseKiRecoveryMultiplier = 1.0;
    public static boolean chargeVisualsEnabled = true;
    public static double statGainMultiplier = 1.0;

    public static void load() {
        try {
            if (FILE.exists()) {
                try (FileReader reader = new FileReader(FILE)) {
                    ConfigData data = GSON.fromJson(reader, ConfigData.class);
                    if (data != null) {
                        baseKiRecoveryMultiplier = data.baseKiRecoveryMultiplier;
                        chargeVisualsEnabled = data.chargeVisualsEnabled;
                        statGainMultiplier = data.statGainMultiplier;
                    }
                }
            } else {
                save();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            try (FileWriter writer = new FileWriter(FILE)) {
                GSON.toJson(new ConfigData(baseKiRecoveryMultiplier, chargeVisualsEnabled, statGainMultiplier), writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class ConfigData {
        double baseKiRecoveryMultiplier;
        boolean chargeVisualsEnabled;
        double statGainMultiplier;

        ConfigData(double b, boolean c, double s) {
            this.baseKiRecoveryMultiplier = b;
            this.chargeVisualsEnabled = c;
            this.statGainMultiplier = s;
        }
    }
}
