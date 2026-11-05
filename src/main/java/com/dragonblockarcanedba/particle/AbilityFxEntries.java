package com.dragonblockarcanedba.particle;

/**
 * The texture entry table for the universal ability particle
 * (DbaParticles.ABILITY_FX). Ability data names entries; this table
 * is the single place that maps a name to a sprite block inside the
 * one registered type's flipbook list and to the entry's render
 * behavior. The particle JSON (assets/.../particles/ability_fx.json)
 * lists FRAMES sprites per entry, in exactly this order; the client
 * particle indexes frameBase(entry) + frame.
 *
 * Behavior columns, following the standing rules:
 * - lit entries are physical matter (shard chips, ash flakes, smoke
 *   puffs): they shade with the world's real light sample and never
 *   take the emissive path, and they collide with the ground.
 * - additive entries are energy (streaks, curls, ember fragments,
 *   soft motes): full bright, additive layer, no depth write (the
 *   layer is the mod's shared additive pipeline), no ground
 *   collision.
 * Sprite sources are the sprite sets the old per-effect types
 * already established: shard_chip, spark_streak, wisp_curl,
 * ember_fragment, ash_flake, ki_mote (soft mote), smoke (puff).
 */
public final class AbilityFxEntries {
    private AbilityFxEntries() {}

    public static final int SHARD_CHIP = 0;
    public static final int SPARK_STREAK = 1;
    public static final int WISP_CURL = 2;
    public static final int EMBER_FRAGMENT = 3;
    public static final int ASH_FLAKE = 4;
    public static final int SOFT_MOTE = 5;
    public static final int SMOKE_PUFF = 6;
    public static final int COUNT = 7;

    /** Flipbook frames per entry inside the universal type's sprite list. */
    public static final int FRAMES = 8;

    private static final String[] NAMES = {
            "shard_chip", "spark_streak", "wisp_curl", "ember_fragment",
            "ash_flake", "soft_mote", "smoke_puff"};

    private static final boolean[] LIT = {
            true, false, false, false, true, false, true};

    private static final boolean[] ADDITIVE = {
            false, true, true, true, false, true, false};

    public static String name(int entry) {
        return NAMES[entry];
    }

    /** Resolves a data name to an entry id, or throws naming the bad value. */
    public static int byName(String name) {
        for (int i = 0; i < COUNT; i++) {
            if (NAMES[i].equals(name)) {
                return i;
            }
        }
        throw new IllegalArgumentException("unknown particle entry \"" + name
                + "\" (known: " + String.join(", ", NAMES) + ")");
    }

    public static boolean isLit(int entry) {
        return LIT[entry];
    }

    public static boolean isAdditive(int entry) {
        return ADDITIVE[entry];
    }

    /** First sprite index of the entry's flipbook block. */
    public static int frameBase(int entry) {
        return entry * FRAMES;
    }
}
