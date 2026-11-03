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

    public static int firstPersonTransparency = 0; // 0 to 100 (0% transparent = fully opaque)

    public static void load() {
        try {
            if (FILE.exists()) {
                try (FileReader reader = new FileReader(FILE)) {
                    ConfigData data = GSON.fromJson(reader, ConfigData.class);
                    if (data != null) {
                        firstPersonTransparency = data.firstPersonTransparency;
                        // Migration support from legacy boolean setting
                        if (firstPersonTransparency == 0 && data.firstPersonHalfTransparency) {
                            firstPersonTransparency = 50;
                        }
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
                GSON.toJson(new ConfigData(firstPersonTransparency), writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class ConfigData {
        int firstPersonTransparency;
        boolean firstPersonHalfTransparency; // for backwards compatibility reading

        ConfigData(int f) {
            this.firstPersonTransparency = f;
        }
    }
}
