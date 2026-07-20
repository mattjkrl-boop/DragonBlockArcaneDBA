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

        int startX = parent.getX();
        int startY = parent.getY();

        for (int i = 0; i < stats.length; i++) {
            final String statName = stats[i];
            int btnY = startY + 55 + i * 22;
            Button btn = Button.builder(Component.literal("+"), b -> {
                CompoundTag nbt = new CompoundTag();
                nbt.putString("action", "upgrade");
                nbt.putString("stat", statName);
                ClientPlayNetworking.send(new ActionPayload(nbt));
            }).bounds(startX + 180, btnY - 4, 18, 18).build();
            
            upgradeButtons[i] = btn;
            parent.addTabWidget(btn);
        }
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        PlayerStatsAccessor accessor = (PlayerStatsAccessor) client.player;
        int startX = parent.getX();
        int startY = parent.getY();

        // Title
        context.text(client.font, Component.literal("Character Stats"), startX + 15, startY + 15, 0xFFFFFFFF);

        // General progress info
        String levelText = "Level: " + accessor.dba$getLevel();
        String xpText = "XP: " + accessor.dba$getXp() + "/" + PlayerStats.getXpToNextLevel(accessor.dba$getLevel());
        String apText = "AP: " + accessor.dba$getStatPoints();
        
        context.text(client.font, Component.literal(levelText), startX + 15, startY + 30, 0xFFFFFFFF);
        context.text(client.font, Component.literal(xpText), startX + 100, startY + 30, 0xFFFFFFFF);
        context.text(client.font, Component.literal(apText), startX + 210, startY + 30, 0xFFFFFFFF);

        // Separator line
        context.fill(startX + 15, startY + 45, startX + parent.getBgWidth() - 15, startY + 46, 0xFF55FF88);

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
            double effectiveValue = PlayerStats.getEffectiveStat(client.player, statName);
            
            int apCost = PlayerStats.getUpgradeCost(currentLevel);
            int milestone = (currentLevel / 5) * 5;
            int reqLvl = milestone * 2;
            boolean canAfford = accessor.dba$getStatPoints() >= apCost;
            boolean levelMet = accessor.dba$getLevel() >= reqLvl;
            
            if (upgradeButtons[i] != null) {
                upgradeButtons[i].active = canAfford && levelMet;
            }

            int textColor = levelMet ? 0xFFFFFFFF : 0xFFFF5555;
            String reqString = !levelMet ? " (Req Lvl " + reqLvl + ")" : "";
            String statString = String.format("%s: Lvl %d - Cost: %d AP%s", displayName, currentLevel, apCost, reqString);
            context.text(client.font, Component.literal(statString), startX + 15, startY + 55 + i * 22, textColor);
        }

        // Draw active Ki pool (optional, keeping it at the bottom)
        double maxKi = PlayerStats.getMaxKi(client.player);
        double curKi = accessor.dba$getCurrentKi();
        String kiString = String.format("Ki: %.1f / %.1f", curKi, maxKi);
        context.text(client.font, Component.literal(kiString), startX + 15, startY + 165, 0xFF55FFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        return false;
    }
}
