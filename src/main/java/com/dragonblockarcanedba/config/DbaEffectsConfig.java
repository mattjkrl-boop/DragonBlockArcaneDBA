package com.dragonblockarcanedba.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Server and client tunables stored in config/dragonblockarcanedba-effects.json.
 * effectDensity scales DBA particle bursts (0 disables them), screenShake scales camera shake on the client,
 * maxWeaponDamage caps any single hit dealt by DBA weapon abilities (0 or less means uncapped).
 */
public final class DbaEffectsConfig {
    private DbaEffectsConfig() {}

    public static final class Data {
        public float effectDensity = 1.0f;
        public float screenShake = 1.0f;
        public float maxWeaponDamage = 0f;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Data data = new Data();

    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("dragonblockarcanedba-effects.json");
    }

    public static void load() {
        Path p = path();
        try {
            if (Files.exists(p)) {
                try (Reader r = Files.newBufferedReader(p)) {
                    Data d = GSON.fromJson(r, Data.class);
                    if (d != null) data = d;
                }
            }
            try (Writer w = Files.newBufferedWriter(p)) {
                GSON.toJson(data, w);
            }
        } catch (Exception e) {
            com.dragonblockarcanedba.DragonBlockArcaneDBA.LOGGER.warn("Could not read {}, using defaults", p, e);
        }
    }

    public static float effectDensity() { return Math.max(0f, Math.min(4f, data.effectDensity)); }
    public static float screenShake() { return Math.max(0f, Math.min(4f, data.screenShake)); }
    public static float maxWeaponDamage() { return data.maxWeaponDamage; }

    /** Applies the configured damage cap. */
    public static float capDamage(float amount) {
        float cap = data.maxWeaponDamage;
        return cap > 0f ? Math.min(amount, cap) : amount;
    }
}
