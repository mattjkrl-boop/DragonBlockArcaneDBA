package com.dragonblockarcanedba.ability;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads ability definitions from data/dragonblockarcanedba/abilities.
 * Mirrors the RaceLoader pattern. Validation is loud by design: every
 * bad file produces an ERROR log naming the file and the exact field
 * problem, and that one ability is skipped while the rest of the set
 * still loads. The raw JSON text of every accepted file is kept so
 * AbilitySyncPayload can ship the identical definitions to clients.
 */
public class AbilityLoader extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {

    /** Raw JSON of the currently loaded set, for client sync. */
    private static volatile Map<Identifier, String> RAW = Map.of();

    public static Map<Identifier, String> rawJson() {
        return RAW;
    }

    @Override
    protected Map<Identifier, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, JsonElement> prepared = new HashMap<>();
        FileToIdConverter converter = FileToIdConverter.json("abilities");
        Map<Identifier, Resource> resources = converter.listMatchingResources(resourceManager);

        resources.forEach((id, resource) -> {
            try (Reader reader = resource.openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                prepared.put(converter.fileToId(id), json);
            } catch (Exception e) {
                DragonBlockArcaneDBA.LOGGER.error("Failed to read ability JSON {}: {}", id, e.getMessage());
            }
        });

        return prepared;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, ProfilerFiller profiler) {
        AbilityRegistry.clear();
        Map<Identifier, String> raw = new HashMap<>();
        int loaded = 0;
        for (Map.Entry<Identifier, JsonElement> entry : prepared.entrySet()) {
            Identifier id = entry.getKey();
            JsonElement element = entry.getValue();
            try {
                if (!element.isJsonObject()) {
                    throw new IllegalArgumentException("top level must be a JSON object");
                }
                JsonObject json = element.getAsJsonObject();
                AbilityDef def = AbilityDef.parse(id, json);
                if (!BuiltInRegistries.ITEM.containsKey(def.weapon())) {
                    throw new IllegalArgumentException("weapon item is not registered: " + def.weapon());
                }
                String error = AbilityRegistry.register(def);
                if (error != null) {
                    DragonBlockArcaneDBA.LOGGER.error("Ability {} rejected: {}", id, error);
                    continue;
                }
                raw.put(id, json.toString());
                loaded++;
            } catch (Exception e) {
                DragonBlockArcaneDBA.LOGGER.error("Ability {} failed to load: {}", id, e.getMessage());
            }
        }
        RAW = Map.copyOf(raw);
        DragonBlockArcaneDBA.LOGGER.info("Loaded {} abilities ({} files seen)", loaded, prepared.size());
    }
}
