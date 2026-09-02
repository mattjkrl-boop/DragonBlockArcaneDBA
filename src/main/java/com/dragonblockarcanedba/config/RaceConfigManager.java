package com.dragonblockarcanedba.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RaceConfigManager implements SimpleSynchronousResourceReloadListener {

    private static final Map<String, RaceConfig> CONFIGS = new ConcurrentHashMap<>();
    private static final Identifier ID = Identifier.fromNamespaceAndPath("dragonblockarcanedba", "race_configs");

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        CONFIGS.clear();
        for (Map.Entry<Identifier, Resource> entry : resourceManager.listResources("races", id -> id.getPath().endsWith(".json")).entrySet()) {
            Identifier resourceId = entry.getKey();
            try (InputStream stream = entry.getValue().open();
                 InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                float width = json.has("hitbox_width") ? json.get("hitbox_width").getAsFloat() : 0.6f;
                float height = json.has("hitbox_height") ? json.get("hitbox_height").getAsFloat() : 1.8f;
                float eye = json.has("eye_height") ? json.get("eye_height").getAsFloat() : 1.62f;
                float ox = json.has("eye_offset_x") ? json.get("eye_offset_x").getAsFloat() : 0.0f;
                float oy = json.has("eye_offset_y") ? json.get("eye_offset_y").getAsFloat() : 7.5f;
                float oz = json.has("eye_offset_z") ? json.get("eye_offset_z").getAsFloat() : -2.1f;
                
                // Extract filename without .json extension for the ID
                String path = resourceId.getPath();
                String raceName = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));
                
                CONFIGS.put(raceName, new RaceConfig(width, height, eye, ox, oy, oz));
            } catch (Exception e) {
                System.err.println("Failed to parse race config for " + resourceId);
                e.printStackTrace();
            }
        }
        System.out.println("Loaded " + CONFIGS.size() + " race configurations.");
    }

    public static RaceConfig getConfig(String raceName) {
        if (raceName == null) return RaceConfig.DEFAULT;
        return CONFIGS.getOrDefault(raceName.toLowerCase(), RaceConfig.DEFAULT);
    }
    
    public static void setConfig(String raceName, RaceConfig config) {
        CONFIGS.put(raceName, config);
    }
    
    public static Map<String, RaceConfig> getAllConfigs() {
        return new HashMap<>(CONFIGS);
    }
}
