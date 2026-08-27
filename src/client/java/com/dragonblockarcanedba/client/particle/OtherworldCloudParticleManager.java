package com.dragonblockarcanedba.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;

import java.util.Random;

/**
 * Atmospheric Otherworld Cloud Particle Manager.
 * Generates continuous, dense, layered streams of moving ParticleTypes.CLOUD particles
 * 150-200 blocks higher than the Check-In Station (Y ≈ 265–285), with realistic
 * vertical gap distribution between layers:
 *  - Bottom (Base): Y ≈ 265 (Fastest drift, vx ≈ 0.28)
 *  - Next: +2 to 4 blocks higher (vx ≈ 0.22)
 *  - Next: +1 to 3 blocks higher (vx ≈ 0.17)
 *  - Next: +3 to 5 blocks higher (vx ≈ 0.12)
 *  - Next: +3 to 8 blocks higher (vx ≈ 0.08)
 * Clouds are distributed across any X-Z coordinates with natural wind drift.
 */
public class OtherworldCloudParticleManager {

    private static final Random RNG = new Random();
    private static int tickCounter = 0;

    // Relative vertical offsets per cloud layer specified by user:
    // Base, then 2-4, then 1-3, then 3-5, then 3-8
    private static final double BASE_Y = 265.0;
    private static final double[] LAYER_SPEEDS = { 0.28, 0.22, 0.17, 0.12, 0.08 };

    public static void tick(Minecraft client) {
        if (client.level == null || client.player == null) return;
        if (!client.level.dimension().toString().contains("otherworld")) return;

        tickCounter++;
        ClientLevel level = client.level;
        LocalPlayer player = client.player;

        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        // 1. High-altitude flight cloud immersion (when player is near the cloud deck Y >= 180)
        if (py >= 180.0) {
            spawnHighAltitudeCloudBeds(level, px, py, pz);
        } else {
            // 2. Ambient view drift (when player is at the Check-In Station or on Snake Way)
            spawnAtmosphericSkyClouds(level, px, py, pz);
        }
    }

    /**
     * Spawns dense banks of moving cloud particles when flying through or near the clouds (Y >= 180).
     */
    private static void spawnHighAltitudeCloudBeds(ClientLevel level, double px, double py, double pz) {
        // Spawn across the 5 elevation tiers with exact vertical gaps
        double currentY = BASE_Y;

        for (int tier = 0; tier < 5; tier++) {
            // Apply vertical gap to next tier
            if (tier == 1) currentY += 2.0 + RNG.nextDouble() * 2.0;       // 2–4 blocks
            else if (tier == 2) currentY += 1.0 + RNG.nextDouble() * 2.0;  // 1–3 blocks
            else if (tier == 3) currentY += 3.0 + RNG.nextDouble() * 2.0;  // 3–5 blocks
            else if (tier == 4) currentY += 3.0 + RNG.nextDouble() * 5.0;  // 3–8 blocks

            // Only spawn if player is within vertical rendering proximity of this layer
            if (Math.abs(py - currentY) > 48.0) continue;

            double speed = LAYER_SPEEDS[tier];

            // Spawn multiple cloud particle puffs for this layer across any X-Z in a radius
            int particlesPerTier = 12;
            for (int i = 0; i < particlesPerTier; i++) {
                // Random horizontal position across X-Z
                double offsetX = (RNG.nextDouble() - 0.5) * 56.0;
                double offsetZ = (RNG.nextDouble() - 0.5) * 56.0;
                // Subtle vertical turbulence within the cloud bank (±0.4 blocks)
                double yOffset = (RNG.nextDouble() - 0.5) * 0.8;

                double spawnX = px + offsetX;
                double spawnY = currentY + yOffset;
                double spawnZ = pz + offsetZ;

                // Horizontal wind drift velocity along X with subtle Z turbulence
                double vx = speed * (0.85 + RNG.nextDouble() * 0.30);
                double vy = (RNG.nextDouble() - 0.5) * 0.005;
                double vz = (RNG.nextDouble() - 0.5) * 0.02;

                level.addParticle(
                    ParticleTypes.CLOUD,
                    spawnX, spawnY, spawnZ,
                    vx, vy, vz
                );
            }
        }
    }

    /**
     * Ambient atmospheric cloud drift visible when exploring lower altitudes in the Otherworld.
     */
    private static void spawnAtmosphericSkyClouds(ClientLevel level, double px, double py, double pz) {
        // Spawn occasional billowing cloud wisps in the open skies around the player
        if (tickCounter % 2 != 0) return;

        for (int i = 0; i < 4; i++) {
            double offsetX = (RNG.nextDouble() - 0.5) * 48.0;
            double offsetZ = (RNG.nextDouble() - 0.5) * 48.0;
            double spawnY = py + 8.0 + RNG.nextDouble() * 20.0;

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
