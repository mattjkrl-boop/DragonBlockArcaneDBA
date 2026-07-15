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
        int spacing = 16; // spacing between bars
        
        // Colors
        int bgColor = 0xAA1E2024;
        
        // Helper lambda for drawing styled bar
        java.util.function.Consumer<StyledBar> drawTechBar = (bar) -> {
            int fillWidth = (int) (barWidth * bar.percent);
            
            // Background
            guiGraphics.fill(x, bar.y, x + barWidth, bar.y + barHeight, bgColor);
            
            // Fill
            if (fillWidth > 0) {
                guiGraphics.fill(x, bar.y, x + fillWidth, bar.y + barHeight, bar.fillColor);
            }
            
            // Tech Borders
            guiGraphics.fill(x, bar.y, x + barWidth, bar.y + 1, bar.borderColor); // Top
            guiGraphics.fill(x, bar.y + barHeight - 1, x + barWidth, bar.y + barHeight, bar.borderColor); // Bottom
            guiGraphics.fill(x, bar.y, x + 2, bar.y + barHeight, bar.borderColor); // Left
            guiGraphics.fill(x + barWidth - 2, bar.y, x + barWidth, bar.y + barHeight, bar.borderColor); // Right
            
            // Corner accents
            guiGraphics.fill(x - 2, bar.y + 2, x, bar.y + barHeight - 2, bar.borderColor);
            guiGraphics.fill(x + barWidth, bar.y + 2, x + barWidth + 2, bar.y + barHeight - 2, bar.borderColor);
        };

        // Draw Health Bar (Red)
        float healthPercent = (float) (Math.max(0, currentHealth) / Math.max(1, maxHealth));
        drawTechBar.accept(new StyledBar(y, healthPercent, 0xFFFF2222, 0xFFFF5555));

        // Draw Ki Bar (Blue)
        y += spacing;
        float kiPercent = (float) (Math.max(0, currentKi) / Math.max(1, maxKi));
        drawTechBar.accept(new StyledBar(y, kiPercent, 0xFF00AAFF, 0xFF55FFFF));

        // Draw Stamina Bar (Green)
        y += spacing;
        float staminaPercent = (float) (Math.max(0, currentStamina) / Math.max(1, maxStamina));
        drawTechBar.accept(new StyledBar(y, staminaPercent, 0xFF22FF22, 0xFF55FF55));
    }
    
    private record StyledBar(int y, float percent, int fillColor, int borderColor) {}
}
