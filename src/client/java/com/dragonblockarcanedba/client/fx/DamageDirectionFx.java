package com.dragonblockarcanedba.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;

/**
 * Tracks where the local player's latest damage came from so the HUD can draw a
 * directional hit arc. Client only: watches the local player's health drop each
 * tick and reads the last damage source's position, converting it to an angle
 * relative to the camera (0 = ahead, positive = to the right). The indicator
 * lives for 22 ticks and fades out. DbaHudOverlay does the drawing.
 */
public final class DamageDirectionFx {
    private DamageDirectionFx() {}

    private static final int LIFE = 22;
    private static float lastHealth = -1f;
    private static double angleRad;
    private static int ticksLeft;

    public static void tick(Minecraft mc) {
        if (ticksLeft > 0) ticksLeft--;
        if (mc.player == null) {
            lastHealth = -1f;
            ticksLeft = 0;
            return;
        }
        float health = mc.player.getHealth();
        if (lastHealth >= 0f && health < lastHealth - 0.01f) {
            DamageSource source = mc.player.getLastDamageSource();
            Vec3 from = source == null ? null : source.getSourcePosition();
            if (from != null) {
                double dx = from.x - mc.player.getX();
                double dz = from.z - mc.player.getZ();
                if (dx * dx + dz * dz > 0.04) {
                    double sourceYaw = Math.toDegrees(Math.atan2(-dx, dz));
                    double rel = wrapDegrees(sourceYaw - mc.player.getYRot());
                    angleRad = Math.toRadians(rel);
                    ticksLeft = LIFE;
                }
            }
        }
        lastHealth = health;
    }

    public static boolean active() {
        return ticksLeft > 0;
    }

    public static double angleRad() {
        return angleRad;
    }

    public static float fade() {
        return ticksLeft / (float) LIFE;
    }

    private static double wrapDegrees(double deg) {
        deg %= 360.0;
        if (deg > 180.0) deg -= 360.0;
        if (deg < -180.0) deg += 360.0;
        return deg;
    }
}
