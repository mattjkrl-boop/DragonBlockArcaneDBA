package com.dragonblockarcanedba.client.compat;

/**
 * Client-side model compatibility.
 * Replaced by DBA native 3D mesh rendering engine.
 */
public final class BpmCompatClient {

    private BpmCompatClient() {}

    public static void init() {
        // Native 3D model engine is active via Custom3DModelLayer
    }

    public static void enforceModel(String requestedId) {
        // Native 3D model engine automatically switches dynamically based on PlayerStatsAccessor raceId
    }

    public static void enforceYardratModel() {
        // Native 3D model engine is active
    }
}
