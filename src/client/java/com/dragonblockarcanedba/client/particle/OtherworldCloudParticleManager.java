package com.dragonblockarcanedba.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Atmospheric Otherworld Cloud Particle Manager.
 * Spawns individual, volumetric moving cloud formations in the Otherworld sky.
 *
 * Each individual cloud formation generates its own Y-level within the specific tier range:
 *  - First Layer (Tier 0): 0 to 2 blocks above the base invisible line (Y = 265.0 .. 267.0)
 *  - Layer 1: +2 to 4 blocks higher per cloud
 *  - Layer 2: +1 to 3 blocks higher per cloud (overlapping/sharing height with layer 1 clouds)
 *  - Layer 3: +3 to 5 blocks higher per cloud
 *  - Layer 4: +3 to 8 blocks higher per cloud
 *
 * Clouds in lower layers move faster across the sky (0.28 down to 0.08).
 * Clouds are horizontally scattered (50-100+ block offsets) so they never stack on top of each other.
 */
public class OtherworldCloudParticleManager {

    private static final Random RNG = new Random();
    private static int tickCounter = 0;

    // Base invisible line where clouds start generating (set to 200.0)
    private static final double BASE_LINE_Y = 200.0;

    // Speeds: fastest at bottom, progressively slower at top
    private static final double[] TIER_SPEEDS = { 0.28, 0.22, 0.17, 0.12, 0.08 };

    /**
     * An individual moving cloud formation in the sky.
     */
    private static class CloudFormation {
        final int tier;
        double relX;
        double relZ;
        double y;
        double radius;
        double speed;

        CloudFormation(int tier, double relX, double relZ, double y, double radius, double speed) {
            this.tier = tier;
            this.relX = relX;
            this.relZ = relZ;
            this.y = y;
            this.radius = radius;
            this.speed = speed;
        }

        void randomizeY() {
            this.y = calculateTierCloudY(this.tier);
        }
    }

    private static final List<CloudFormation> CLOUD_FORMATIONS = new ArrayList<>();
    private static boolean initialized = false;

    /**
     * Calculates an individual cloud's Y level based on its tier's range:
     * Tier 0: Base line (265.0) + (0..2)
     * Tier 1: Tier 0 base + (2..4)
     * Tier 2: Tier 1 base + (1..3) (can equal or overlap Tier 1 clouds)
     * Tier 3: Tier 2 base + (3..5)
     * Tier 4: Tier 3 base + (3..8)
     */
    private static double calculateTierCloudY(int tier) {
        double y0 = BASE_LINE_Y + RNG.nextDouble() * 2.0;       // 0 to 2 blocks above base line
        if (tier == 0) return y0;

        double y1 = y0 + 2.0 + RNG.nextDouble() * 2.0;          // +2 to 4 blocks
        if (tier == 1) return y1;

        double y2 = y1 + 1.0 + RNG.nextDouble() * 2.0;          // +1 to 3 blocks
        if (tier == 2) return y2;

        double y3 = y2 + 3.0 + RNG.nextDouble() * 2.0;          // +3 to 5 blocks
        if (tier == 3) return y3;

        return y3 + 3.0 + RNG.nextDouble() * 5.0;               // +3 to 8 blocks
    }

    private static void initFormations() {
        CLOUD_FORMATIONS.clear();

        // Spawn 4 distinct cloud formations per tier (20 clouds total)
        // Staggered with 50-100+ block offsets across X-Z so they never stack
        for (int tier = 0; tier < 5; tier++) {
            double speed = TIER_SPEEDS[tier];

            for (int i = 0; i < 4; i++) {
                // Wide horizontal dispersion (-90 to +90)
                double relX = (RNG.nextDouble() - 0.5) * 180.0;
                double relZ = (RNG.nextDouble() - 0.5) * 180.0;
                double y = calculateTierCloudY(tier);
                double radius = 14.0 + RNG.nextDouble() * 10.0; // 14 to 24 block cloud radius

                CLOUD_FORMATIONS.add(new CloudFormation(tier, relX, relZ, y, radius, speed));
            }
        }
        initialized = true;
    }

    public static void tick(Minecraft client) {
        if (client.level == null || client.player == null) return;
        if (!client.level.dimension().toString().contains("otherworld")) return;

        if (!initialized) {
            initFormations();
        }

        tickCounter++;
        ClientLevel level = client.level;
        LocalPlayer player = client.player;

        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        // 1. High-altitude flight: render through volumetric cloud formations (Y >= 140)
        if (py >= 140.0) {
            tickVolumetricClouds(level, px, py, pz);
        } else {
            // 2. Ambient view drift: occasional gentle wisps near the Check-In Station or Snake Way
            spawnAmbientLowWisps(level, px, py, pz);
        }
    }

    private static void tickVolumetricClouds(ClientLevel level, double px, double py, double pz) {
        for (CloudFormation cloud : CLOUD_FORMATIONS) {
            // Drift individual cloud with horizontal wind
            cloud.relX += cloud.speed;

            // Wrap around when drifting beyond view horizon
            if (cloud.relX > 110.0) {
                cloud.relX = -110.0;
                cloud.relZ = (RNG.nextDouble() - 0.5) * 180.0;
                cloud.randomizeY();
            }

            // Only spawn particles if player is within vertical proximity of this individual cloud
            if (Math.abs(py - cloud.y) > 42.0) continue;

            // Spawn cloud puffs inside this cloud's volume
            int puffs = 3;
            for (int p = 0; p < puffs; p++) {
                double offsetX = (RNG.nextDouble() - 0.5) * cloud.radius;
                double offsetZ = (RNG.nextDouble() - 0.5) * cloud.radius;
                // Vertical puff variation within the cloud bank
                double yOffset = (RNG.nextDouble() - 0.5) * 0.9;

                double spawnX = px + cloud.relX + offsetX;
                double spawnY = cloud.y + yOffset;
                double spawnZ = pz + cloud.relZ + offsetZ;

                // Drift velocity along X with subtle turbulence
                double vx = cloud.speed * (0.85 + RNG.nextDouble() * 0.30);
                double vy = (RNG.nextDouble() - 0.5) * 0.004;
                double vz = (RNG.nextDouble() - 0.5) * 0.02;

                level.addParticle(
                    ParticleTypes.CLOUD,
                    spawnX, spawnY, spawnZ,
                    vx, vy, vz
                );
            }
        }
    }

    private static void spawnAmbientLowWisps(ClientLevel level, double px, double py, double pz) {
        if (tickCounter % 2 != 0) return;

        for (int i = 0; i < 3; i++) {
            double offsetX = (RNG.nextDouble() - 0.5) * 50.0;
            double offsetZ = (RNG.nextDouble() - 0.5) * 50.0;
            double spawnY = py + 8.0 + RNG.nextDouble() * 22.0;

            double vx = 0.12 * (0.8 + RNG.nextDouble() * 0.4);
            double vy = (RNG.nextDouble() - 0.5) * 0.003;
            double vz = (RNG.nextDouble() - 0.5) * 0.015;

            level.addParticle(
                ParticleTypes.CLOUD,
                px + offsetX, spawnY, pz + offsetZ,
                vx, vy, vz
            );
        }
    }
}
