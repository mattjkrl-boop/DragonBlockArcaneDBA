package com.dragonblockarcanedba.client.mixin;

import com.dragonblockarcanedba.effect.DbaEffects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders a dark purple vignette overlay when the local player has the Melting effect.
 * Opacity scales from barely visible at full health to very dark near death,
 * simulating the "vision turns dark purple" feel.
 */
@Mixin(Hud.class)
public class MeltingOverlayMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void dba$renderMeltingVignette(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Check if the player has the Melting effect
        if (!mc.player.hasEffect(DbaEffects.MELTING_HOLDER)) return;

        float health = mc.player.getHealth();
        float maxHealth = mc.player.getMaxHealth();
        float healthRatio = maxHealth > 0 ? health / maxHealth : 1.0f;

        // Alpha: 0.1 at full health → 0.7 near death
        // Lower health = darker purple overlay
        float alphaF = 0.1f + 0.6f * (1.0f - healthRatio);
        int alpha = (int) (alphaF * 255);
        alpha = Math.max(0, Math.min(255, alpha));

        // Dark purple color: R=64, G=0, B=96 with dynamic alpha
        int color = (alpha << 24) | (0x40 << 16) | (0x00 << 8) | 0x60;

        // Draw overlay covering the entire screen
        guiGraphics.fill(0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(), color);
    }
}
