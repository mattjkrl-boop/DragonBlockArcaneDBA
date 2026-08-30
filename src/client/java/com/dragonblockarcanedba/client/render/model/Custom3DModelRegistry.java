package com.dragonblockarcanedba.client.render.model;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry and cache for native 3D polygonal models in Dragon Block Arcane.
 */
public class Custom3DModelRegistry {

    private static final Map<String, ObjMesh> MODEL_CACHE = new ConcurrentHashMap<>();
    private static ObjMesh defaultMesh = null;

    public static final Identifier YARDRAT_OBJ_ID =
            Identifier.fromNamespaceAndPath("dragonblockarcanedba", "models/3d/yardrat.obj");

    /**
     * Retrieves the 3D OBJ mesh for the specified race.
     * All races currently utilize the canonical high-fidelity 3D base geometry.
     */
    public static ObjMesh getModelForRace(String race) {
        if (defaultMesh != null) {
            return defaultMesh;
        }

        // Attempt loading from Minecraft ResourceManager
        try {
            var mc = Minecraft.getInstance();
            if (mc != null && mc.getResourceManager() != null) {
                var res = mc.getResourceManager().getResource(YARDRAT_OBJ_ID);
                if (res.isPresent()) {
                    try (InputStream in = res.get().open()) {
                        defaultMesh = ObjMesh.parse(in);
                        System.out.println("[DBA] Successfully loaded native 3D mesh from resource manager: " + YARDRAT_OBJ_ID);
                        return defaultMesh;
                    }
                }
            }
        } catch (Throwable t) {
            System.err.println("[DBA] Resource manager load attempt failed: " + t.getMessage());
        }

        // ClassLoader fallback (for early GUI or init screens)
        try (InputStream in = Custom3DModelRegistry.class.getResourceAsStream("/assets/dragonblockarcanedba/models/3d/yardrat.obj")) {
            if (in != null) {
                defaultMesh = ObjMesh.parse(in);
                System.out.println("[DBA] Successfully loaded native 3D mesh via ClassLoader fallback.");
                return defaultMesh;
            }
        } catch (Throwable t) {
            System.err.println("[DBA] ClassLoader fallback load attempt failed: " + t.getMessage());
        }

        return defaultMesh;
    }

    /**
     * Clears cached meshes to allow hot-reloading.
     */
    public static void clearCache() {
        MODEL_CACHE.clear();
        defaultMesh = null;
    }
}
