package com.dragonblockarcanedba.registry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DbaRegistries {
    private static final Map<Identifier, Race> RACES = new HashMap<>();
    private static final Map<Identifier, Form> FORMS = new HashMap<>();

    private static final String[] DEFAULT_RACES = {
        "android", "arcosian", "bio_android", "half_saiyan", "human",
        "majin", "namekian", "neo_tuffle", "saiyan", "tuffle", "yardrat"
    };

    private static final String[] DEFAULT_FORMS = {
        "arcosian_final_form", "arcosian_form_2", "arcosian_form_3",
        "giant_namekian", "golden_form", "kaioken", "orange_namekian",
        "super_saiyan_1", "super_saiyan_2", "super_saiyan_3",
        "super_saiyan_blue", "super_saiyan_god", "ultra_instinct"
    };

    public static void initDefaults() {
        for (String raceName : DEFAULT_RACES) {
            String path = "/data/dragonblockarcanedba/races/" + raceName + ".json";
            try (InputStream is = DbaRegistries.class.getResourceAsStream(path)) {
                if (is != null) {
                    try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                        Identifier id = Identifier.fromNamespaceAndPath("dragonblockarcanedba", raceName);
                        Race race = Race.fromJson(id, json);
                        registerRace(race);
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to load default race from classpath: " + raceName + " - " + e.getMessage());
            }
        }

        for (String formName : DEFAULT_FORMS) {
            String path = "/data/dragonblockarcanedba/forms/" + formName + ".json";
            try (InputStream is = DbaRegistries.class.getResourceAsStream(path)) {
                if (is != null) {
                    try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                        Identifier id = Identifier.fromNamespaceAndPath("dragonblockarcanedba", formName);
                        Form form = Form.fromJson(id, json);
                        registerForm(form);
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to load default form from classpath: " + formName + " - " + e.getMessage());
            }
        }
    }

    public static void registerRace(Race race) {
        RACES.put(race.getId(), race);
    }

    public static void registerForm(Form form) {
        FORMS.put(form.getId(), form);
    }

    public static Map<Identifier, Race> getRaces() {
        return Collections.unmodifiableMap(RACES);
    }

    public static Map<Identifier, Form> getForms() {
        return Collections.unmodifiableMap(FORMS);
    }

    public static Race getRace(Identifier id) {
        return RACES.get(id);
    }

    public static Form getForm(Identifier id) {
        return FORMS.get(id);
    }
    
    public static Set<Identifier> getAllFormIds() {
        return Collections.unmodifiableSet(FORMS.keySet());
    }

    public static void clearAll() {
        RACES.clear();
        FORMS.clear();
        initDefaults();
    }
}
