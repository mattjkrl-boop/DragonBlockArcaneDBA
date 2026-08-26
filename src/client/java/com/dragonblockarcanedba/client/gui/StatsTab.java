package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.network.ActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public class StatsTab implements MenuTab {
    private DbaMenuScreen parent;
    private final String[] stats = {"strength", "dexterity", "defense", "willpower", "spirit", "vitality"};
    private final String[] statDisplayNames = {"Strength", "Dexterity", "Defense", "Willpower", "Spirit", "Vitality"};

    // Continuous hold upgrade tracking
    private boolean isMouseDown = false;
    private String heldStat = null;
    private long holdStartMs = 0;
    private long lastUpgradeMs = 0;

    public static String formatNumber(long num) {
        if (num >= 1_000_000_000L) {
            return String.format(Locale.US, "%.2fB", num / 1_000_000_000.0);
        } else if (num >= 10_000_000L) {
            return String.format(Locale.US, "%.1fM", num / 1_000_000.0);
        } else if (num >= 100_000L) {
            return String.format(Locale.US, "%.1fk", num / 1_000.0);
        }
        return String.format(Locale.US, "%,d", num);
    }

    @Override
    public void init(DbaMenuScreen screen) {
        this.parent = screen;
        this.isMouseDown = false;
        this.heldStat = null;
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        PlayerStatsAccessor accessor = (PlayerStatsAccessor) client.player;
        int startX = parent.getContentX();
        int startY = parent.getY();
        int width = parent.getContentWidth();

        // Process continuous hold upgrading
        if (heldStat != null && isMouseDown) {
            int statIndex = -1;
            for (int s = 0; s < stats.length; s++) {
                if (stats[s].equals(heldStat)) {
                    statIndex = s;
                    break;
                }
            }

            if (statIndex >= 0) {
                int btnX = startX + width - 28;
                int y = startY + 55 + statIndex * 24;
                int btnY = y - 4;
                int btnW = 16;
                int btnH = 16;

                if (mouseX < btnX - 10 || mouseX > btnX + btnW + 10 || mouseY < btnY - 10 || mouseY > btnY + btnH + 10) {
                    heldStat = null;
                } else {
                    String raceId = accessor.dba$getRaceId().getPath();
                    int currentUpgrades = accessor.dba$getStatUpgradeCount(heldStat);
                    int apCost = PlayerStats.getUpgradeCost(raceId, heldStat, currentUpgrades);
                    int milestone = (currentUpgrades / 5) * 5;
                    int reqLvl = milestone * 2;
                    boolean canAfford = accessor.dba$getStatPoints() >= apCost;
                    boolean levelMet = accessor.dba$getLevel() >= reqLvl;

                    if (!canAfford || !levelMet) {
                        heldStat = null;
                    } else {
                        long now = System.currentTimeMillis();
                        long heldDuration = now - holdStartMs;
                        if (heldDuration >= 300) {
                            long interval = (heldDuration >= 1200) ? 20 : (heldDuration >= 700 ? 35 : 50);
                            if (now - lastUpgradeMs >= interval) {
                                sendUpgrade(heldStat);
                                lastUpgradeMs = now;
                            }
                        }
                    }
                }
            } else {
                heldStat = null;
            }
        }

        // Row 1: Title (Left) and AP Display (Right)
        String raceName = accessor.dba$getRaceId().getPath();
        String titleText = "Character Stats";
        if (raceName.length() > 0) {
            String[] words = raceName.split("-");
            StringBuilder formattedRace = new StringBuilder();
            for (String word : words) {
                if (word.length() > 0) {
                    formattedRace.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append("-");
                }
            }
            if (formattedRace.length() > 0) {
                formattedRace.setLength(formattedRace.length() - 1);
            }
            titleText = "Character Stats (" + formattedRace + ")";
        }
        context.text(client.font, Component.literal(titleText), startX + 12, startY + 14, 0xFF55FF88);

        String apText = "AP: " + formatNumber(accessor.dba$getStatPoints());
        int apWidth = client.font.width(apText);
        context.text(client.font, Component.literal(apText), startX + width - apWidth - 14, startY + 14, 0xFFFFAA00);

        // Row 2: Level (Left) and XP (Middle/Right)
        String levelText = "Level: " + formatNumber(accessor.dba$getLevel());
        String xpText = "XP: " + formatNumber(accessor.dba$getXp()) + " / " + formatNumber(PlayerStats.getXpToNextLevel(accessor.dba$getLevel()));
        
        context.text(client.font, Component.literal(levelText), startX + 12, startY + 28, 0xFFFFFFFF);
        context.text(client.font, Component.literal(xpText), startX + 115, startY + 28, 0xFFAAAAAA);

        // Separator line
        context.fill(startX + 10, startY + 42, startX + width - 10, startY + 43, 0x44FFFFFF);

        // Draw Stats list
        int btnX = startX + width - 28;

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
            
            String raceId = accessor.dba$getRaceId().getPath();
            int currentUpgrades = accessor.dba$getStatUpgradeCount(statName);
            int apCost = PlayerStats.getUpgradeCost(raceId, statName, currentUpgrades);
            int gain = PlayerStats.getStatGain(raceId, statName);
            
            int milestone = (currentUpgrades / 5) * 5;
            int reqLvl = milestone * 2;
            boolean canAfford = accessor.dba$getStatPoints() >= apCost;
            boolean levelMet = accessor.dba$getLevel() >= reqLvl;
            boolean canUpgrade = canAfford && levelMet;

            int y = startY + 54 + i * 24;
            
            // Progress bar
            int barWidth = 48;
            int barHeight = 8;
            int barX = startX + 68;
            
            float progress = (float) accessor.dba$getStatPoints() / (float) apCost;
            if (progress > 1.0f) progress = 1.0f;
            if (Float.isNaN(progress) || Float.isInfinite(progress)) progress = 0.0f;
            
            // Bar Background & Fill
            context.fill(barX, y, barX + barWidth, y + barHeight, 0x44000000);
            context.fill(barX, y, barX + (int)(barWidth * progress), y + barHeight, 0xAA55FF55);
            context.fill(barX - 1, y - 1, barX + barWidth + 1, y, 0x55FFFFFF);
            context.fill(barX - 1, y + barHeight, barX + barWidth + 1, y + barHeight + 1, 0x55FFFFFF);
            context.fill(barX - 1, y, barX, y + barHeight, 0x55FFFFFF);
            context.fill(barX + barWidth, y, barX + barWidth + 1, y + barHeight, 0x55FFFFFF);
            
            int textColor = levelMet ? 0xFFFFFFFF : 0xFFFF5555;
            String reqString = !levelMet ? " (Req " + reqLvl + ")" : "";
            
            // Display stat name (Line 1)
            context.text(client.font, Component.literal(displayName), startX + 12, y, 0xFFFFFFFF);
            
            // Display raw stat + gain (Line 1)
            String statString = String.format(Locale.US, "%s (+%s)", formatNumber(currentLevel), formatNumber(gain));
            context.text(client.font, Component.literal(statString), barX + barWidth + 6, y, 0xFFFFFFFF);
            
            // Display AP cost below the bar (Line 2)
            String apString = "Cost: " + formatNumber(apCost) + " AP";
            context.text(client.font, Component.literal(apString + reqString), barX, y + 10, textColor);
            
            // Custom Upgrade Button
            int btnY = y - 4;
            int btnW = 16;
            int btnH = 16;
            boolean hoverBtn = (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH);
            boolean isHeld = statName.equals(heldStat) && isMouseDown;
            
            if (canUpgrade) {
                int bgCol = isHeld ? 0xDD55FF88 : (hoverBtn ? 0xAA55FF88 : 0x55113322);
                context.fill(btnX, btnY, btnX + btnW, btnY + btnH, bgCol);
                context.fill(btnX, btnY, btnX + btnW, btnY + 1, 0xFF55FF88);
                context.fill(btnX, btnY + btnH - 1, btnX + btnW, btnY + btnH, 0xFF55FF88);
                context.fill(btnX, btnY, btnX + 1, btnY + btnH, 0xFF55FF88);
                context.fill(btnX + btnW - 1, btnY, btnX + btnW, btnY + btnH, 0xFF55FF88);
                
                context.centeredText(client.font, Component.literal("+"), btnX + btnW/2, btnY + 4, (hoverBtn || isHeld) ? 0xFFFFFFFF : 0xFF55FF88);
            } else {
                context.fill(btnX, btnY, btnX + btnW, btnY + btnH, 0x44111111);
                context.fill(btnX, btnY, btnX + btnW, btnY + 1, 0x44FFFFFF);
                context.fill(btnX, btnY + btnH - 1, btnX + btnW, btnY + btnH, 0x44FFFFFF);
                context.fill(btnX, btnY, btnX + 1, btnY + btnH, 0x44FFFFFF);
                context.fill(btnX + btnW - 1, btnY, btnX + btnW, btnY + btnH, 0x44FFFFFF);
                
                context.centeredText(client.font, Component.literal("+"), btnX + btnW/2, btnY + 4, 0xFF555555);
            }
        }

        // Active Ki pool at the bottom
        double maxKi = PlayerStats.getMaxKi(client.player);
        double curKi = accessor.dba$getCurrentKi();
        String kiString = String.format(Locale.US, "Ki: %.1f / %.1f", curKi, maxKi);
        
        int kiPanelY = startY + 204;
        context.fill(startX + 10, kiPanelY, startX + width - 10, kiPanelY + 15, 0x2255FFFF);
        context.centeredText(client.font, Component.literal(kiString), startX + width / 2, kiPanelY + 4, 0xFF55FFFF);
    }

    private void sendUpgrade(String statName) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("action", "upgrade");
        nbt.putInt("percent", 0);
        nbt.putString("stat", statName);
        ClientPlayNetworking.send(new ActionPayload(nbt));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;

        PlayerStatsAccessor accessor = (PlayerStatsAccessor) client.player;
        int startX = parent.getContentX();
        int startY = parent.getY();
        int width = parent.getContentWidth();
        
        double mouseX = event.x();
        double mouseY = event.y();
        
        int btnX = startX + width - 28;

        for (int i = 0; i < stats.length; i++) {
            String statName = stats[i];
            
            String raceId = accessor.dba$getRaceId().getPath();
            int currentUpgrades = accessor.dba$getStatUpgradeCount(statName);
            int apCost = PlayerStats.getUpgradeCost(raceId, statName, currentUpgrades);
            int milestone = (currentUpgrades / 5) * 5;
            int reqLvl = milestone * 2;
            boolean canAfford = accessor.dba$getStatPoints() >= apCost;
            boolean levelMet = accessor.dba$getLevel() >= reqLvl;
            boolean canUpgrade = canAfford && levelMet;

            int y = startY + 54 + i * 24;
            int btnY = y - 4;
            int btnW = 16;
            int btnH = 16;
            
            if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                if (canUpgrade) {
                    sendUpgrade(statName);
                    this.isMouseDown = true;
                    this.heldStat = statName;
                    this.holdStartMs = System.currentTimeMillis();
                    this.lastUpgradeMs = this.holdStartMs;
                    return true;
                }
            }
        }
        this.isMouseDown = false;
        this.heldStat = null;
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        this.isMouseDown = true;
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.isMouseDown = false;
        this.heldStat = null;
        return false;
    }
}
