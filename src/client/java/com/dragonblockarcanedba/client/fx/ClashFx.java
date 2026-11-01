package com.dragonblockarcanedba.client.fx;

import com.dragonblockarcanedba.entity.GrandClashSparkEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/**
 * Clash camera pressure. When a Grand Sword clash spark is live near the local
 * player, their clash throws off visible energy, so the camera gets a small
 * repeating nudge from the existing shake system. The nudge is deliberately weak
 * (0.35, far below real hits) so it never overrides genuine impact shakes, and it
 * stops the moment no clash spark is nearby. Client only, no networking.
 */
public final class ClashFx {
    private ClashFx() {}

    private static int cooldown;

    public static void tick(Minecraft mc) {
        if (cooldown > 0) cooldown--;
        if (mc.level == null || mc.player == null || cooldown > 0) return;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof GrandClashSparkEntity && entity.distanceToSqr(mc.player) < 144.0) {
                ScreenShakeHandler.trigger(0.35f, 4, 0);
                cooldown = 7;
                return;
            }
        }
    }
}
