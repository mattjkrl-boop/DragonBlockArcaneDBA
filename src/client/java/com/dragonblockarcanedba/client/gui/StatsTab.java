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

/**
 * Modern RPG Character Sheet for Dragon Block Arcane:
 * - Left Dossier: Race banner, Combat Power (BP), Level/XP, Ki pool, and Movement Speed control.
 * - Right Grid: 6 spacious attribute cards with large buttons, milestone progress, and hold-to-upgrade.
 */
public class StatsTab implements MenuTab {
    private DbaMenuScreen parent;
    private final String[] stats = {"strength", "dexterity", "defense", "willpower", "spirit", "vitality"};
    private final String[] statDisplayNames = {"STRENGTH", "DEXTERITY", "DEFENSE", "WILLPOWER", "SPIRIT", "VITALITY"};
    private final String[] statIcons = {"⚔", "⚡", "🛡", "🔮", "✦", "❤"};
    private final int[] statColors = {0xFFFF5555, 0xFF00E5FF, 0xFF55FF88, 0xFFFFAA00, 0xFFDD88FF, 0xFFFF7799};

    // Continuous hold upgrade tracking
    private boolean isMouseDown = false;
    private String heldStat = null;
    private long holdStartMs = 0;
    private long lastUpgradeMs = 0;

    // Speed Slider dragging inside dossier
    private boolean isDraggingSpeed = false;

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
        this.isDraggingSpeed = false;
    }

    private void sendSpeedPercent(int percent) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("action", "set_speed_percent");
        nbt.putInt("percent", percent);
        ClientPlayNetworking.send(new ActionPayload(nbt));
    }

    private void sendUpgrade(String statName) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("action", "upgrade");
        nbt.putInt("percent", 0);
        nbt.putString("stat", statName);
        ClientPlayNetworking.send(new ActionPayload(nbt));
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) client.player;

        int contentX = parent.getContentX();
        int contentY = parent.getContentY();
        int contentW = parent.getContentWidth();
        int contentH = parent.getContentHeight();

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
            } else {
                heldStat = null;
            }
        }

        // =========================================================================
        // 1. LEFT COLUMN: CHARACTER DOSSIER (width ~ 158)
        // =========================================================================
        int dossierW = 158;
        int dossierX = contentX + 4;
        int dossierY = contentY + 4;
        int dossierH = contentH - 8;

        // Dossier Background
        context.fill(dossierX, dossierY, dossierX + dossierW, dossierY + dossierH, 0xAA0D131F);
        context.fill(dossierX, dossierY, dossierX + dossierW, dossierY + 1, 0x4400E5FF);
        context.fill(dossierX, dossierY + dossierH - 1, dossierX + dossierW, dossierY + dossierH, 0x4400E5FF);
        context.fill(dossierX, dossierY, dossierX + 1, dossierY + dossierH, 0x4400E5FF);
        context.fill(dossierX + dossierW - 1, dossierY, dossierX + dossierW, dossierY + dossierH, 0x4400E5FF);

        int cy = dossierY + 8;

        // Race Banner
        String raceName = accessor.dba$getRaceId() != null ? accessor.dba$getRaceId().getPath().replace("_", " ").toUpperCase() : "MORTAL";
        context.fill(dossierX + 8, cy, dossierX + dossierW - 8, cy + 18, 0x4400E5FF);
        context.centeredText(client.font, Component.literal("✦ " + raceName + " ✦"), dossierX + dossierW / 2, cy + 5, 0xFF00E5FF);
        cy += 24;

        // Combat Rating / Power Level Scouter Display
        long totalStats = accessor.dba$getStrength() + accessor.dba$getDexterity() + accessor.dba$getDefense()
            + accessor.dba$getWillpower() + accessor.dba$getSpirit() + accessor.dba$getVitality();
        long combatPower = totalStats * 250L + accessor.dba$getLevel() * 1000L;
        context.text(client.font, Component.literal("COMBAT POWER"), dossierX + 10, cy, 0xFFFFAA00);
        cy += 11;
        context.fill(dossierX + 8, cy, dossierX + dossierW - 8, cy + 20, 0x88000000);
        context.fill(dossierX + 8, cy, dossierX + dossierW - 8, cy + 1, 0xFFFFAA00);
        context.centeredText(client.font, Component.literal(formatNumber(combatPower) + " BP"), dossierX + dossierW / 2, cy + 6, 0xFFFFDD44);
        cy += 26;

        // Level & XP Bar
        int lvl = accessor.dba$getLevel();
        long xp = accessor.dba$getXp();
        long reqXp = PlayerStats.getXpToNextLevel(lvl);
        float xpProg = reqXp > 0 ? Math.min(1.0f, (float) xp / reqXp) : 1.0f;

        context.text(client.font, Component.literal("LEVEL " + DbaMenuScreen.formatCompactNumber(lvl)), dossierX + 10, cy, 0xFFFFFFFF);
        String pctStr = String.format(Locale.US, "%.0f%%", xpProg * 100.0f);
        context.text(client.font, Component.literal(pctStr), dossierX + dossierW - 10 - client.font.width(pctStr), cy, 0xFFAAAAAA);
        cy += 11;

        int xpBarW = dossierW - 20;
        int xpBarH = 7;
        context.fill(dossierX + 10, cy, dossierX + 10 + xpBarW, cy + xpBarH, 0x88000000);
        context.fill(dossierX + 10, cy, dossierX + 10 + (int)(xpBarW * xpProg), cy + xpBarH, 0xFF00E5FF);
        cy += 14;

        // Ki Energy Pool
        double maxKi = PlayerStats.getMaxKi(client.player);
        double curKi = accessor.dba$getCurrentKi();
        float kiProg = maxKi > 0 ? (float) Math.min(1.0, curKi / maxKi) : 1.0f;

        context.text(client.font, Component.literal("KI ENERGY"), dossierX + 10, cy, 0xFF55FF88);
        String kiNum = DbaMenuScreen.formatCompactNumber((long) curKi) + "/" + DbaMenuScreen.formatCompactNumber((long) maxKi);
        context.text(client.font, Component.literal(kiNum), dossierX + dossierW - 10 - client.font.width(kiNum), cy, 0xFFAAAAAA);
        cy += 11;

        context.fill(dossierX + 10, cy, dossierX + 10 + xpBarW, cy + xpBarH, 0x88000000);
        context.fill(dossierX + 10, cy, dossierX + 10 + (int)(xpBarW * kiProg), cy + xpBarH, 0xFF55FF88);
        cy += 18;

        // Movement Speed Controller
        context.fill(dossierX + 6, cy, dossierX + dossierW - 6, cy + 1, 0x33FFFFFF);
        cy += 6;
        int speedPct = accessor.dba$getSpeedPercent();
        context.text(client.font, Component.literal("SPEED LIMIT"), dossierX + 10, cy, 0xFF00E5FF);
        String spdStr = speedPct + "%";
        context.text(client.font, Component.literal(spdStr), dossierX + dossierW - 10 - client.font.width(spdStr), cy, 0xFF55FF88);
        cy += 12;

        int sldX = dossierX + 10;
        int sldY = cy;
        int sldW = dossierW - 20;
        int sldH = 8;
        context.fill(sldX, sldY, sldX + sldW, sldY + sldH, 0x88000000);
        int fillSpd = Math.max(2, (int) (sldW * (speedPct / 100.0f)));
        context.fill(sldX, sldY, sldX + fillSpd, sldY + sldH, 0xFF00E5FF);

        // Knob
        int knobX = sldX + fillSpd;
        context.fill(knobX - 2, sldY - 1, knobX + 2, sldY + sldH + 1, 0xFFFFFFFF);
        cy += 14;

        // Quick Speed Presets [25%, 50%, 75%, 100%]
        int presetW = (dossierW - 26) / 4;
        int[] presets = {25, 50, 75, 100};
        for (int p = 0; p < 4; p++) {
            int px = dossierX + 10 + p * (presetW + 2);
            int py = cy;
            boolean active = (speedPct == presets[p]);
            boolean hoverP = mouseX >= px && mouseX <= px + presetW && mouseY >= py && mouseY <= py + 12;
            context.fill(px, py, px + presetW, py + 12, active ? 0xDD0E3320 : (hoverP ? 0x66223344 : 0x44111822));
            context.centeredText(client.font, Component.literal(presets[p] + "%"), px + presetW / 2, py + 2, active ? 0xFF55FF88 : 0xFFAAAAAA);
        }

        // =========================================================================
        // 2. RIGHT GRID: 6 ATTRIBUTE CARDS (2 Columns of 3)
        // =========================================================================
        int gridX = dossierX + dossierW + 8;
        int gridY = contentY + 4;
        int gridW = contentW - dossierW - 16;
        int gridH = contentH - 8;

        int colW = (gridW - 8) / 2;
        int rowH = (gridH - 12) / 3;

        for (int i = 0; i < stats.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int cardX = gridX + col * (colW + 8);
            int cardY = gridY + row * (rowH + 6);

            String statName = stats[i];
            String displayName = statDisplayNames[i];
            String icon = statIcons[i];
            int themeCol = statColors[i];

            int currentVal = 0;
            switch (statName) {
                case "strength" -> currentVal = accessor.dba$getStrength();
                case "dexterity" -> currentVal = accessor.dba$getDexterity();
                case "defense" -> currentVal = accessor.dba$getDefense();
                case "willpower" -> currentVal = accessor.dba$getWillpower();
                case "spirit" -> currentVal = accessor.dba$getSpirit();
                case "vitality" -> currentVal = accessor.dba$getVitality();
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

            // Card Background & Borders
            context.fill(cardX, cardY, cardX + colW, cardY + rowH, 0xAA0D131F);
            context.fill(cardX, cardY, cardX + 3, cardY + rowH, themeCol); // Left colored accent
            context.fill(cardX, cardY, cardX + colW, cardY + 1, 0x33FFFFFF);
            context.fill(cardX, cardY + rowH - 1, cardX + colW, cardY + rowH, 0x33FFFFFF);
            context.fill(cardX + colW - 1, cardY, cardX + colW, cardY + rowH, 0x33FFFFFF);

            // Row 1 & 2: Icon, Title, Value, Milestone (dynamic zero-overlap layout)
            String titleText = icon + " " + displayName;
            String valStr = formatNumber(currentVal) + " (+" + formatNumber(gain) + ")";
            int titleW = client.font.width(titleText);
            int valW = client.font.width(valStr);

            String milestoneStr = levelMet ? ("Milestone: " + currentUpgrades + "/∞") : ("Req Player Lv. " + reqLvl);
            int mCol = levelMet ? 0xFF8899A6 : 0xFFFF5555;

            if (titleW + valW + 16 <= colW) {
                // Generous room: Title on left, Value on right
                context.text(client.font, Component.literal(titleText), cardX + 8, cardY + 5, themeCol);
                context.text(client.font, Component.literal(valStr), cardX + colW - 8 - valW, cardY + 5, 0xFFFFFFFF);
                context.text(client.font, Component.literal(milestoneStr), cardX + 8, cardY + 17, mCol);
            } else {
                // Title is wide (e.g. WILLPOWER): Title on line 1, Value & Milestone cleanly separated on line 2!
                context.text(client.font, Component.literal(titleText), cardX + 8, cardY + 4, themeCol);
                context.text(client.font, Component.literal(valStr), cardX + 8, cardY + 15, 0xFFFFFFFF);
                context.text(client.font, Component.literal(milestoneStr), cardX + colW - 8 - client.font.width(milestoneStr), cardY + 15, mCol);
            }

            // Row 3: Dedicated Upgrade Button (Spacious, zero overlap!)
            int btnW = colW - 16;
            int btnH = 16;
            int btnX = cardX + 8;
            int btnY = cardY + rowH - btnH - 6;

            boolean hoverBtn = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
            boolean isHeld = statName.equals(heldStat) && isMouseDown;

            int btnBg = canUpgrade ? (isHeld ? 0xDD00C853 : (hoverBtn ? 0xAA00C853 : 0x66009624)) : 0x33222222;
            int btnBorder = canUpgrade ? 0xFF55FF88 : 0x44FFFFFF;
            String btnTxt = canUpgrade ? ("+ UPGRADE (" + formatNumber(apCost) + " AP)") : (levelMet ? ("LOCKED (" + formatNumber(apCost) + " AP)") : ("REQ LV." + reqLvl));

            context.fill(btnX, btnY, btnX + btnW, btnY + btnH, btnBg);
            context.fill(btnX, btnY, btnX + btnW, btnY + 1, btnBorder);
            context.fill(btnX, btnY + btnH - 1, btnX + btnW, btnY + btnH, btnBorder);
            context.fill(btnX, btnY, btnX + 1, btnY + btnH, btnBorder);
            context.fill(btnX + btnW - 1, btnY, btnX + btnW, btnY + btnH, btnBorder);

            context.centeredText(client.font, Component.literal(btnTxt), btnX + btnW / 2, btnY + 4, canUpgrade ? 0xFFFFFFFF : 0xFF888888);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) client.player;

        double mx = event.x();
        double my = event.y();

        int contentX = parent.getContentX();
        int contentY = parent.getContentY();
        int contentW = parent.getContentWidth();
        int contentH = parent.getContentHeight();

        int dossierW = 185;
        int dossierX = contentX + 4;
        int dossierY = contentY + 4;

        // 1. Speed Slider Click
        int sldX = dossierX + 10;
        int sldY = dossierY + 155;
        int sldW = dossierW - 20;
        int sldH = 8;
        if (mx >= sldX - 4 && mx <= sldX + sldW + 4 && my >= sldY - 4 && my <= sldY + sldH + 4) {
            this.isDraggingSpeed = true;
            updateSpeedFromMouse(mx, sldX, sldW, accessor);
            return true;
        }

        // 2. Speed Preset Chips
        int presetW = (dossierW - 26) / 4;
        int py = dossierY + 169;
        int[] presets = {25, 50, 75, 100};
        for (int p = 0; p < 4; p++) {
            int px = dossierX + 10 + p * (presetW + 2);
            if (mx >= px && mx <= px + presetW && my >= py && my <= py + 12) {
                DbaMenuScreen.playClickSound();
                accessor.dba$setSpeedPercent(presets[p]);
                sendSpeedPercent(presets[p]);
                return true;
            }
        }

        // 3. Stat Upgrade Buttons Click
        int gridX = dossierX + dossierW + 8;
        int gridY = contentY + 4;
        int gridW = contentW - dossierW - 16;
        int gridH = contentH - 8;
        int colW = (gridW - 8) / 2;
        int rowH = (gridH - 12) / 3;

        for (int i = 0; i < stats.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int cardX = gridX + col * (colW + 8);
            int cardY = gridY + row * (rowH + 6);

            int btnW = colW - 16;
            int btnH = 16;
            int btnX = cardX + 8;
            int btnY = cardY + rowH - btnH - 6;

            if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
                String statName = stats[i];
                String raceId = accessor.dba$getRaceId().getPath();
                int currentUpgrades = accessor.dba$getStatUpgradeCount(statName);
                int apCost = PlayerStats.getUpgradeCost(raceId, statName, currentUpgrades);
                int milestone = (currentUpgrades / 5) * 5;
                int reqLvl = milestone * 2;
                boolean canAfford = accessor.dba$getStatPoints() >= apCost;
                boolean levelMet = accessor.dba$getLevel() >= reqLvl;

                if (canAfford && levelMet) {
                    DbaMenuScreen.playClickSound();
                    sendUpgrade(statName);
                    this.isMouseDown = true;
                    this.heldStat = statName;
                    this.holdStartMs = System.currentTimeMillis();
                    this.lastUpgradeMs = this.holdStartMs;
                    return true;
                }
            }
        }

        return false;
    }

    private void updateSpeedFromMouse(double mouseX, int sliderX, int sliderW, PlayerStatsAccessor accessor) {
        float frac = (float) (mouseX - sliderX) / (float) sliderW;
        int percent = Math.max(1, Math.min(100, Math.round(frac * 100.0f)));
        if (percent != accessor.dba$getSpeedPercent()) {
            accessor.dba$setSpeedPercent(percent);
            sendSpeedPercent(percent);
        }
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.isDraggingSpeed) {
            Minecraft client = Minecraft.getInstance();
            if (client.player instanceof PlayerStatsAccessor accessor) {
                int contentX = parent.getContentX();
                int dossierW = 185;
                int sldX = contentX + 4 + 10;
                int sldW = dossierW - 20;
                updateSpeedFromMouse(event.x(), sldX, sldW, accessor);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.isMouseDown = false;
        this.heldStat = null;
        this.isDraggingSpeed = false;
        return false;
    }
}
