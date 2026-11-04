package com.dragonblockarcanedba.util;

/**
 * Deterministic seed hashing shared by client geometry builders and
 * server entities. The math is the splitmix64 finalizer family the
 * complex geometry builders have always used; it lives in common code
 * so a server entity can reproduce a client builder's seeded placement
 * exactly (same seed and index, same value, bit for bit) without ever
 * referencing a client class. Pure functions of their inputs: no state,
 * no game access, safe on any thread.
 */
public final class SeedHash {

    private SeedHash() {}

    /** Splitmix64 finalizer: avalanches a long into a well mixed long. */
    public static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    /** Lattice hash in 0..1 for (seed, index). */
    public static float hash1(long seed, int i) {
        return (mix(seed + i * 0x9E3779B97F4A7C15L) >>> 40) * (1.0f / (1 << 24));
    }

    /** Smooth value noise in 0..1, stable for a given seed. */
    public static float noise1(long seed, float x) {
        int i = (int) Math.floor(x);
        float f = x - i;
        float u = f * f * (3f - 2f * f);
        return hash1(seed, i) * (1f - u) + hash1(seed, i + 1) * u;
    }
}
