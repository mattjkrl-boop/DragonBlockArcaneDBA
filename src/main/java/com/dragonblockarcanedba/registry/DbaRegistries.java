package com.dragonblockarcanedba.registry;

import net.minecraft.resources.Identifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DbaRegistries {
    private static final Map<Identifier, Race> RACES = new HashMap<>();
    private static final Map<Identifier, Form> FORMS = new HashMap<>();

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
    }
}
