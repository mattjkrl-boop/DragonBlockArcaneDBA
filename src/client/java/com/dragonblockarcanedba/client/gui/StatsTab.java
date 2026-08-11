package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.network.ActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public class StatsTab implements MenuTab {
    private DbaMenuScreen parent;
    private final String[] stats = {"strength", "dexterity", "defense", "willpower", "spirit", "vitality"};
    private final String[] statDisplayNames = {"Strength", "Dexterity", "Defense", "Willpower", "Spirit", "Vitality"};
    private final Button[] upgradeButtons = new Button[6];

    @Override
    public void init(DbaMenuScreen screen) {
        this.parent = screen;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        int startX = parent.getContentX();
        int startY = parent.getY();
        
        // Upgrade buttons positioned to the right of the content area
        int btnX = startX + parent.getContentWidth() - 30;

        for (int i = 0; i < stats.length; i++) {
            final String statName = stats[i];
            int btnY = startY + 55 + i * 24;
            Button btn = Button.builder(Component.literal("+"), b -> {
                CompoundTag nbt = new CompoundTag();
                nbt.putString("action", "upgrade");
                nbt.putString("stat", statName);
                ClientPlayNetworking.send(new ActionPayload(nbt));
            }).bounds(btnX, btnY - 4, 18, 18).build();
            
            upgradeButtons[i] = btn;
            parent.addTabWidget(btn);
        }
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        PlayerStatsAccessor accessor = (PlayerStatsAccessor) client.player;
        int startX = parent.getContentX();
        int startY = parent.getY();
        int width = parent.getContentWidth();

        // Title
        context.text(client.font, Component.literal("Character Stats"), startX + 15, startY + 15, 0xFF55FF88);

        // General progress info
        String levelText = "Level: " + accessor.dba$getLevel();
        String xpText = "XP: " + accessor.dba$getXp() + "/" + PlayerStats.getXpToNextLevel(accessor.dba$getLevel());
        String apText = "AP: " + accessor.dba$getStatPoints();
        
        context.text(client.font, Component.literal(levelText), startX + 15, startY + 30, 0xFFFFFFFF);
        context.text(client.font, Component.literal(xpText), startX + 85, startY + 30, 0xFFFFFFFF);
        context.text(client.font, Component.literal(apText), startX + width - 70, startY + 30, 0xFFFFAA00);

        // Subtle separator line
        context.fill(startX + 10, startY + 45, startX + width - 10, startY + 46, 0x44FFFFFF);

        // Draw Stats list
        for (int i = 0; i < stats.length; i++) {
            String statName = stats[i];
            String displayName = statDisplayNames[i];
            int currentLevel = 0;
            switch (statName) {
                case "strength" -> currentLevel = accessor.dba$getStrength();
                case "dexterity" -> currentLevel = accessor.dba$getDexterity();
                case "defense" -> currentLevel = accessor.dba$getDefense();
                case "willpower" -> currentLevel = accessor.dba$getWillpower();
                case "spirit" -> currentLevel = accessor.dba$getSpirit();
                case "vitality" -> currentLevel = accessor.dba$getVitality();
            }
            
            int apCost = PlayerStats.getUpgradeCost(currentLevel);
            int milestone = (currentLevel / 5) * 5;
            int reqLvl = milestone * 2;
            boolean canAfford = accessor.dba$getStatPoints() >= apCost;
            boolean levelMet = accessor.dba$getLevel() >= reqLvl;
            
            if (upgradeButtons[i] != null) {
                upgradeButtons[i].active = canAfford && levelMet;
            }

            int y = startY + 55 + i * 24;
            
            // Draw progress bar
            int barWidth = 90;
            int barHeight = 8;
            int barX = startX + 90;
            float progress = Math.min(1.0f, (float)currentLevel / 5000f); // 5000 is arbitrary max for display purposes
            
            // Background of bar
            context.fill(barX, y, barX + barWidth, y + barHeight, 0x44000000);
            // Fill of bar
            context.fill(barX, y, barX + (int)(barWidth * progress), y + barHeight, 0xAA55FF55);
            // Border of bar
            context.fill(barX - 1, y - 1, barX + barWidth + 1, y, 0x55FFFFFF); // Top
            context.fill(barX - 1, y + barHeight, barX + barWidth + 1, y + barHeight + 1, 0x55FFFFFF); // Bottom
            context.fill(barX - 1, y, barX, y + barHeight, 0x55FFFFFF); // Left
            context.fill(barX + barWidth, y, barX + barWidth + 1, y + barHeight, 0x55FFFFFF); // Right
            
            int textColor = levelMet ? 0xFFFFFFFF : 0xFFFF5555;
            String reqString = !levelMet ? " (Req Lvl " + reqLvl + ")" : "";
            
            // Display stat name + current
            context.text(client.font, Component.literal(displayName), startX + 15, y, 0xFFFFFFFF);
            
            // Display level centered in bar (if it fits) or next to it
            String statString = String.format("Lvl %d", currentLevel);
            context.text(client.font, Component.literal(statString), barX + barWidth + 10, y, 0xFFFFFFFF);
            
            // Display AP cost and Req level below the bar
            context.text(client.font, Component.literal("Cost: " + apCost + " AP" + reqString), barX, y + 10, textColor);
        }

        // Draw active Ki pool at the very bottom
        double maxKi = PlayerStats.getMaxKi(client.player);
        double curKi = accessor.dba$getCurrentKi();
        String kiString = String.format("Ki: %.1f / %.1f", curKi, maxKi);
        
        // Draw a nice Ki pool panel
        int kiPanelY = startY + 205;
        context.fill(startX + 10, kiPanelY, startX + width - 10, kiPanelY + 15, 0x2255FFFF);
        context.centeredText(client.font, Component.literal(kiString), startX + width / 2, kiPanelY + 4, 0xFF55FFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        return false;
    }
}
