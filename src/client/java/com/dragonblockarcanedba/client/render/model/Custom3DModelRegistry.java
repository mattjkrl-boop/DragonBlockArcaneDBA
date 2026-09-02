package com.dragonblockarcanedba.client.render.model;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry and cache for native 3D polygonal models in Dragon Block Arcane.
 */
public class Custom3DModelRegistry {

    private static final Map<String, ObjMesh> MODEL_CACHE = new ConcurrentHashMap<>();

    public static final Identifier BASE_OBJ_ID =
            Identifier.fromNamespaceAndPath("dragonblockarcanedba", "models/3d/base.obj");
    public static final Identifier YARDRAT_OBJ_ID =
            Identifier.fromNamespaceAndPath("dragonblockarcanedba", "models/3d/yardrat.obj");

    public static boolean hasModelForRace(String race) {
        return race != null && !race.isEmpty();
    }

    /**
     * Retrieves the 3D OBJ mesh for the specified race.
     * Loads from assets/dragonblockarcanedba/models/3d/<race>.obj if available,
     * otherwise falls back to the universal base 3D model (base.obj).
     */
    public static ObjMesh getModelForRace(String race) {
        if (race == null || race.isEmpty()) {
            race = "base";
        }
        String raceKey = race.toLowerCase(Locale.ROOT);

        ObjMesh cached = MODEL_CACHE.get(raceKey);
        if (cached != null) {
            return cached;
        }

        Identifier modelId = Identifier.fromNamespaceAndPath("dragonblockarcanedba", "models/3d/" + raceKey + ".obj");
        ObjMesh loaded = loadMesh(modelId, "/assets/dragonblockarcanedba/models/3d/" + raceKey + ".obj");

        if (loaded == null) {
            // Fallback to universal base 3D model
            loaded = loadMesh(BASE_OBJ_ID, "/assets/dragonblockarcanedba/models/3d/base.obj");
        }

        if (loaded != null) {
            MODEL_CACHE.put(raceKey, loaded);
        }

        return loaded;
    }

    private static ObjMesh loadMesh(Identifier resourceId, String classPath) {
        // Attempt loading from Minecraft ResourceManager
        try {
            var mc = Minecraft.getInstance();
            if (mc != null && mc.getResourceManager() != null) {
                var res = mc.getResourceManager().getResource(resourceId);
                if (res.isPresent()) {
                    try (InputStream in = res.get().open()) {
                        ObjMesh mesh = ObjMesh.parse(in);
                        System.out.println("[DBA] Successfully loaded native 3D mesh from resource manager: " + resourceId);
                        return mesh;
                    }
                }
            }
        } catch (Throwable t) {
            System.err.println("[DBA] Resource manager load attempt failed for " + resourceId + ": " + t.getMessage());
        }

        // ClassLoader fallback (for early GUI or init screens)
        try (InputStream in = Custom3DModelRegistry.class.getResourceAsStream(classPath)) {
            if (in != null) {
                ObjMesh mesh = ObjMesh.parse(in);
                System.out.println("[DBA] Successfully loaded native 3D mesh via ClassLoader fallback: " + classPath);
                return mesh;
            }
        } catch (Throwable t) {
            System.err.println("[DBA] ClassLoader fallback load attempt failed for " + classPath + ": " + t.getMessage());
        }

        return null;
    }

    /**
     * Clears cached meshes to allow hot-reloading.
     */
    public static void clearCache() {
        MODEL_CACHE.clear();
    }
}
