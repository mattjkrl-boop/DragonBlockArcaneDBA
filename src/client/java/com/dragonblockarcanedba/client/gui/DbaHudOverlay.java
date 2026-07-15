package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.DeltaTracker;
import net.minecraft.network.chat.Component;

public class DbaHudOverlay implements HudElement {
    
    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;

        // Render safety: Hide if in spectator
        if (player == null || player.isSpectator()) {
            return;
        }

        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
        double currentKi = accessor.dba$getCurrentKi();
        double maxKi = PlayerStats.getMaxKi(player);
        
        double currentStamina = accessor.dba$getCurrentStamina();
        double maxStamina = PlayerStats.getMaxStamina(player);

        float currentHealth = player.getHealth();
        float maxHealth = player.getMaxHealth();

        int x = 15;
        int y = 15;

        int barWidth = 120;
        int barHeight = 8;
        int spacing = 22; // spacing between bars

        // Colors
        int bgColor = 0xAA1E2024;
        
        // Draw Health Bar (Red)
        float healthPercent = (float) (Math.max(0, currentHealth) / Math.max(1, maxHealth));
        int healthFillWidth = (int) (barWidth * healthPercent);
        
        String healthText = String.format("HP: %.0f / %.0f", currentHealth, maxHealth);
        guiGraphics.text(client.font, Component.literal(healthText), x, y, 0xFFFF5555);
        int barY = y + 10;
        guiGraphics.fill(x, barY, x + barWidth, barY + barHeight, bgColor);
        guiGraphics.fill(x, barY, x + healthFillWidth, barY + barHeight, 0xFFFF2222);
        // Border
        guiGraphics.fill(x - 1, barY - 1, x + barWidth + 1, barY, 0xFFFF5555);
        guiGraphics.fill(x - 1, barY + barHeight, x + barWidth + 1, barY + barHeight + 1, 0xFFFF5555);
        guiGraphics.fill(x - 1, barY, x, barY + barHeight, 0xFFFF5555);
        guiGraphics.fill(x + barWidth, barY, x + barWidth + 1, barY + barHeight, 0xFFFF5555);

        // Draw Ki Bar (Blue)
        y += spacing;
        float kiPercent = (float) (Math.max(0, currentKi) / Math.max(1, maxKi));
        int kiFillWidth = (int) (barWidth * kiPercent);
        
        String kiText = String.format("KI: %.0f / %.0f", currentKi, maxKi);
        guiGraphics.text(client.font, Component.literal(kiText), x, y, 0xFF55FFFF);
        barY = y + 10;
        guiGraphics.fill(x, barY, x + barWidth, barY + barHeight, bgColor);
        guiGraphics.fill(x, barY, x + kiFillWidth, barY + barHeight, 0xFF00AAFF);
        // Border
        guiGraphics.fill(x - 1, barY - 1, x + barWidth + 1, barY, 0xFF55FFFF);
        guiGraphics.fill(x - 1, barY + barHeight, x + barWidth + 1, barY + barHeight + 1, 0xFF55FFFF);
        guiGraphics.fill(x - 1, barY, x, barY + barHeight, 0xFF55FFFF);
        guiGraphics.fill(x + barWidth, barY, x + barWidth + 1, barY + barHeight, 0xFF55FFFF);

        // Draw Stamina Bar (Green)
        y += spacing;
        float staminaPercent = (float) (Math.max(0, currentStamina) / Math.max(1, maxStamina));
        int staminaFillWidth = (int) (barWidth * staminaPercent);

        String staminaText = String.format("SP: %.0f / %.0f", currentStamina, maxStamina);
        guiGraphics.text(client.font, Component.literal(staminaText), x, y, 0xFF55FF55);
        barY = y + 10;
        guiGraphics.fill(x, barY, x + barWidth, barY + barHeight, bgColor);
        guiGraphics.fill(x, barY, x + staminaFillWidth, barY + barHeight, 0xFF22FF22);
        // Border
        guiGraphics.fill(x - 1, barY - 1, x + barWidth + 1, barY, 0xFF55FF55);
        guiGraphics.fill(x - 1, barY + barHeight, x + barWidth + 1, barY + barHeight + 1, 0xFF55FF55);
        guiGraphics.fill(x - 1, barY, x, barY + barHeight, 0xFF55FF55);
        guiGraphics.fill(x + barWidth, barY, x + barWidth + 1, barY + barHeight, 0xFF55FF55);
    }
}
