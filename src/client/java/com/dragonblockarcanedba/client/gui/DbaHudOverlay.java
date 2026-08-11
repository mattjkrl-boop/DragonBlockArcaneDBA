package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.DeltaTracker;
import net.minecraft.network.chat.Component;
import com.dragonblockarcanedba.ki.KiTechnique;

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
        
        // Helper lambda for drawing styled bar with text
        java.util.function.BiConsumer<StyledBar, String> drawTechBar = (bar, labelText) -> {
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

            // Draw status text (e.g. "20 / 20 HP") centered inside/over the bar
            guiGraphics.centeredText(
                client.font,
                Component.literal(labelText),
                x + barWidth / 2,
                bar.y,
                0xFFFFFFFF
            );
        };

        // Draw Health Bar (Red)
        float healthPercent = Math.min(1.0f, (float) (Math.max(0, currentHealth) / Math.max(1, maxHealth)));
        drawTechBar.accept(
            new StyledBar(y, healthPercent, 0xFFFF2222, 0xFFFF5555),
            (int)Math.ceil(currentHealth) + " / " + (int)maxHealth + " HP"
        );

        // Draw Ki Bar (Blue)
        y += spacing;
        float kiPercent = Math.min(1.0f, (float) (Math.max(0, currentKi) / Math.max(1, maxKi)));
        drawTechBar.accept(
            new StyledBar(y, kiPercent, 0xFF00AAFF, 0xFF55FFFF),
            (int)Math.ceil(currentKi) + " / " + (int)maxKi + " KI"
        );

        // Draw Stamina Bar (Green)
        y += spacing;
        float staminaPercent = Math.min(1.0f, (float) (Math.max(0, currentStamina) / Math.max(1, maxStamina)));
        drawTechBar.accept(
            new StyledBar(y, staminaPercent, 0xFF22FF22, 0xFF55FF55),
            (int)Math.ceil(currentStamina) + " / " + (int)maxStamina + " STM"
        );

        // Draw Active Form
        y += spacing + 4;
        net.minecraft.resources.Identifier formId = accessor.dba$getActiveFormId();
        if (formId != null) {
            String formName = formId.getPath().replace("_", " ").toUpperCase();
            double mastery = accessor.dba$getFormMastery(formId);
            String masteryText = String.format("%.1f%%", mastery);
            guiGraphics.text(client.font, net.minecraft.network.chat.Component.literal("\u00A7e" + formName + " \u00A77(" + masteryText + ")"), x, y, 0xFFFFFF);
        }

        // Draw Ki Technique Slots (F7/F8/F9) at Bottom Left
        int screenHeight = client.getWindow().getGuiScaledHeight();
        int slotY = screenHeight - 60;
        
        guiGraphics.text(client.font, net.minecraft.network.chat.Component.literal("\u00A7lKi Techniques"), x, slotY - 12, 0x55FFFF);
        for (int i = 0; i < 3; i++) {
            KiTechnique tech = accessor.dba$getKiTechniqueSlot(i);
            int currentY = slotY + (i * 14);
            
            String keyBind = "F" + (7 + i);
            String text = "\u00A77[" + keyBind + "] ";
            
            if (tech.isEmpty) {
                text += "\u00A78(Empty)";
            } else {
                int cost = (int) (maxKi * (tech.usedPercent / 100.0));
                String colorPrefix = (currentKi >= cost) ? "\u00A7a" : "\u00A7c";
                text += "\u00A7b" + tech.displayName() + " " + colorPrefix + "[" + cost + " Ki]";
            }
            guiGraphics.text(client.font, net.minecraft.network.chat.Component.literal(text), x, currentY, 0xFFFFFF);
        }
    }
    
    private record StyledBar(int y, float percent, int fillColor, int borderColor) {}
}
