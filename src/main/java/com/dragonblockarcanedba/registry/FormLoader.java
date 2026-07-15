package com.dragonblockarcanedba.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.profiling.ProfilerFiller;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public class FormLoader extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {

    @Override
    protected Map<Identifier, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, JsonElement> prepared = new HashMap<>();
        FileToIdConverter converter = FileToIdConverter.json("forms");
        Map<Identifier, Resource> resources = converter.listMatchingResources(resourceManager);

        resources.forEach((id, resource) -> {
            try (Reader reader = resource.openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                prepared.put(converter.fileToId(id), json);
            } catch (Exception e) {
                System.err.println("Failed to read form JSON: " + id + " - " + e.getMessage());
            }
        });

        return prepared;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, ProfilerFiller profiler) {
        prepared.forEach((id, element) -> {
            try {
                if (element.isJsonObject()) {
                    JsonObject json = element.getAsJsonObject();
                    Form form = Form.fromJson(id, json);
                    DbaRegistries.registerForm(form);
                }
            } catch (Exception e) {
                System.err.println("Failed to load form " + id + ": " + e.getMessage());
            }
        });
    }
}
