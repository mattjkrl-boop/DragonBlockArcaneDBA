package com.dragonblockarcanedba.ability;

import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The loaded ability set. Server side it is filled by AbilityLoader
 * from datapack JSON; the client fills the same registry from
 * AbilitySyncPayload, parsing the server's raw JSON with the same
 * AbilityDef parser. One registry, two fill paths, identical data. Bindings are derived
 * from the definitions themselves (each def names its weapon item
 * and slot), never from item code.
 */
public final class AbilityRegistry {
    private static final Map<Identifier, AbilityDef> BY_ID = new LinkedHashMap<>();
    private static final Map<Identifier, EnumMap<AbilityDef.Slot, Identifier>> BY_ITEM = new LinkedHashMap<>();

    private AbilityRegistry() {}

    public static void clear() {
        BY_ID.clear();
        BY_ITEM.clear();
    }

    /**
     * Registers one definition. Returns an error string on a duplicate
     * ability id or a duplicate weapon plus slot binding, or null on
     * success. On error nothing is registered, so the first file to
     * claim a binding keeps it.
     */
    public static String register(AbilityDef def) {
        if (BY_ID.containsKey(def.id())) {
            return "duplicate ability id " + def.id();
        }
        if (def.bind()) {
            EnumMap<AbilityDef.Slot, Identifier> slots =
                    BY_ITEM.computeIfAbsent(def.weapon(), k -> new EnumMap<>(AbilityDef.Slot.class));
            Identifier existing = slots.get(def.slot());
            if (existing != null) {
                return "weapon " + def.weapon() + " slot " + def.slot()
                        + " is already bound to " + existing + "; " + def.id() + " loses";
            }
            slots.put(def.slot(), def.id());
        }
        BY_ID.put(def.id(), def);
        return null;
    }

    public static AbilityDef get(Identifier id) {
        return id == null ? null : BY_ID.get(id);
    }

    public static AbilityDef forItem(Identifier itemId, AbilityDef.Slot slot) {
        EnumMap<AbilityDef.Slot, Identifier> slots = BY_ITEM.get(itemId);
        if (slots == null) {
            return null;
        }
        return get(slots.get(slot));
    }

    public static Identifier binding(Identifier itemId, AbilityDef.Slot slot) {
        EnumMap<AbilityDef.Slot, Identifier> slots = BY_ITEM.get(itemId);
        return slots == null ? null : slots.get(slot);
    }

    public static Collection<AbilityDef> all() {
        return BY_ID.values();
    }

    public static Map<Identifier, EnumMap<AbilityDef.Slot, Identifier>> bindings() {
        return BY_ITEM;
    }

    public static int size() {
        return BY_ID.size();
    }
}
