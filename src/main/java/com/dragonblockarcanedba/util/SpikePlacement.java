package com.dragonblockarcanedba.util;

/**
 * Shared base placement for spikeEruption's RING layout: which angle
 * each spike stands at on its circle, and how far out. Clustered on
 * purpose (attack upgrade merge, Carter's steering): a ring reads as
 * groups of two to four spikes grown together with bare rim between
 * the groups, never as an even fence. The grouping, the cluster center
 * jitter, the in-cluster spacing, and the per-spike radius variance
 * are all pure functions of (seed, index, count) through SeedHash, so
 * the client builder and a server entity compute the identical ring:
 * TemporalRiftEntity drops its glass ring as real fragments at these
 * exact angles when the rift reverses.
 *
 * At distance scale 1.0 the builder draws exactly count spikes and
 * index i here is that spike. At lower detail the builder draws fewer
 * spikes grouped by the reduced count, so a far viewer's drawn ring
 * is a regrouping, not a subset; the server always drops the full
 * count's positions, which are the persistent truth.
 */
public final class SpikePlacement {

    private SpikePlacement() {}

    /** Cluster count for a ring of n spikes: tufts, not a fence. */
    public static int ringClusters(int n) {
        if (n <= 3) return 1;
        if (n <= 6) return 2;
        if (n <= 10) return 3;
        return 4;
    }

    /** Size of contiguous group g when n items split into groups. */
    public static int groupSize(int n, int groups, int g) {
        return n / groups + (g < n % groups ? 1 : 0);
    }

    /** First index of contiguous group g. */
    public static int groupStart(int n, int groups, int g) {
        int start = 0;
        for (int j = 0; j < g; j++) start += groupSize(n, groups, j);
        return start;
    }

    /** The contiguous group index i belongs to. */
    public static int groupOf(int n, int groups, int i) {
        int g = 0;
        while (i >= groupStart(n, groups, g) + groupSize(n, groups, g)) g++;
        return g;
    }

    /**
     * The angle (radians, world XZ with 0 on +X) spike i of n stands
     * at: its cluster's jittered center plus a tight in-cluster offset
     * plus a per-spike micro jitter, so no two neighbors stand alike.
     */
    public static float ringAngle(long seed, int i, int n) {
        if (n <= 1) return 0f;
        int clusters = ringClusters(n);
        int g = groupOf(n, clusters, i);
        int start = groupStart(n, clusters, g);
        int size = groupSize(n, clusters, g);
        int local = i - start;
        float mid = start + (size - 1) * 0.5f;
        float center = (float) (Math.PI * 2.0 * mid / n)
                + (SeedHash.hash1(seed, 600 + g) - 0.5f) * 0.44f;
        float spacing = 0.13f + 0.11f * SeedHash.hash1(seed, 620 + g);
        float localOff = (local - (size - 1) * 0.5f) * spacing;
        float micro = (SeedHash.hash1(seed, 640 + i) - 0.5f) * 0.06f;
        return center + localOff + micro;
    }

    /** Radius multiplier for spike i of n: plus or minus 7 percent. */
    public static float ringRadiusFrac(long seed, int i, int n) {
        return 1.0f + (SeedHash.hash1(seed, 680 + i) - 0.5f) * 0.14f;
    }
}
