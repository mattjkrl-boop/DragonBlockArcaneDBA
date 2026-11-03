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
import net.minecraft.resources.Identifier;

import java.util.Locale;

/**
 * One vertical sheet: identity, speed, then a full-width row for each stat.
 */
public class StatsTab implements MenuTab {
    private static final String[] STATS = {"strength", "dexterity", "defense", "willpower", "spirit", "vitality"};
    private static final String[] NAMES = {"Strength", "Dexterity", "Defense", "Willpower", "Spirit", "Vitality"};
    private static final int[] PRESETS = {25, 50, 75, 100};

    private DbaMenuScreen parent;
    private boolean holding;
    private String heldStat;
    private long holdStartMs;
    private long lastUpgradeMs;
    private boolean draggingSpeed;

    @Override
    public void init(DbaMenuScreen screen) {
        this.parent = screen;
        this.holding = false;
        this.heldStat = null;
        this.draggingSpeed = false;
    }

    private Sheet sheet() {
        int x = parent.getContentX();
        int y = parent.getContentY();
        int w = parent.getContentWidth();
        int h = parent.getContentHeight();
        int presetH = Math.min(DbaUi.BUTTON_H, 18);
        int raceY = y;
        int levelY = y + 12;
        int barY = y + 24;
        int kiY = y + 32;
        int kiBarY = y + 42;
        int sliderY = y + 50;
        int presetY = sliderY + 30;
        int rowTop = presetY + presetH + 6;
        int gap = 3;
        int remain = Math.max(0, y + h - rowTop);
        int rowBudget = remain - gap * 5;
        int rowH = rowBudget >= 6 * 16 ? Math.min(28, rowBudget / 6) : Math.max(14, rowBudget / 6);
        int btnW = Math.min(108, Math.max(72, w / 4));
        return new Sheet(x, y, w, h, raceY, levelY, barY, kiY, kiBarY, sliderY, presetY, presetH, rowTop, rowH, gap, btnW);
    }

    private void sendSpeed(int percent) {
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

    private static String racePath(PlayerStatsAccessor accessor) {
        Identifier race = accessor.dba$getRaceId();
        return race == null ? "human" : race.getPath();
    }

    private boolean canUpgrade(PlayerStatsAccessor accessor, String stat) {
        String raceId = racePath(accessor);
        int upgrades = accessor.dba$getStatUpgradeCount(stat);
        int cost = PlayerStats.getUpgradeCost(raceId, stat, upgrades);
        int requiredLevel = (upgrades / 5) * 5 * 2;
        return accessor.dba$getStatPoints() >= cost && accessor.dba$getLevel() >= requiredLevel;
    }

    @Override
    public void tick() {
        if (!holding || heldStat == null || parent == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (!(client.player instanceof PlayerStatsAccessor accessor)) {
            heldStat = null;
            return;
        }
        if (!canUpgrade(accessor, heldStat)) {
            heldStat = null;
            return;
        }
        long now = System.currentTimeMillis();
        long held = now - holdStartMs;
        if (held < 300) {
            return;
        }
        long interval = held >= 1200 ? 20 : (held >= 700 ? 35 : 50);
        if (now - lastUpgradeMs >= interval) {
            sendUpgrade(heldStat);
            lastUpgradeMs = now;
        }
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();
        if (!(client.player instanceof PlayerStatsAccessor accessor) || parent == null) {
            return;
        }
        Sheet sheet = sheet();
        var font = client.font;

        String race = accessor.dba$getRaceId() != null
                ? accessor.dba$getRaceId().getPath().replace('_', ' ').toUpperCase(Locale.ROOT)
                : "UNCHOSEN";
        context.text(font, Component.literal(DbaUi.clip(font, race, sheet.w / 2)), sheet.x, sheet.raceY, DbaUi.GOLD);
        long total = accessor.dba$getStrength() + accessor.dba$getDexterity() + accessor.dba$getDefense()
                + accessor.dba$getWillpower() + accessor.dba$getSpirit() + accessor.dba$getVitality();
        long power = total * 250L + accessor.dba$getLevel() * 1000L;
        String powerText = DbaMenuScreen.formatCompactNumber(power);
        context.text(font, Component.literal(powerText), sheet.x + sheet.w - font.width(powerText), sheet.raceY, DbaUi.TEXT);

        int level = accessor.dba$getLevel();
        long xp = accessor.dba$getXp();
        long need = PlayerStats.getXpToNextLevel(level);
        float xpFrac = need > 0 ? Math.min(1.0f, (float) xp / need) : 1.0f;
        context.text(font, Component.literal("Level " + DbaMenuScreen.formatCompactNumber(level)), sheet.x, sheet.levelY, DbaUi.TEXT);
        String pct = String.format(Locale.US, "%.0f%%", xpFrac * 100.0f);
        context.text(font, Component.literal(pct), sheet.x + sheet.w - font.width(pct), sheet.levelY, DbaUi.MUTED);
        context.fill(sheet.x, sheet.barY, sheet.x + sheet.w, sheet.barY + 4, DbaUi.INK_DEEP);
        context.fill(sheet.x, sheet.barY, sheet.x + Math.round(sheet.w * xpFrac), sheet.barY + 4, DbaUi.GOLD);

        double maxKi = PlayerStats.getMaxKi(client.player);
        double curKi = accessor.dba$getCurrentKi();
        float kiFrac = maxKi > 0 ? (float) Math.min(1.0, curKi / maxKi) : 1.0f;
        String kiText = "Ki  " + DbaMenuScreen.formatCompactNumber((long) curKi) + " / " + DbaMenuScreen.formatCompactNumber((long) maxKi);
        context.text(font, Component.literal(DbaUi.clip(font, kiText, sheet.w)), sheet.x, sheet.kiY, DbaUi.MUTED);
        context.fill(sheet.x, sheet.kiBarY, sheet.x + sheet.w, sheet.kiBarY + 4, DbaUi.INK_DEEP);
        context.fill(sheet.x, sheet.kiBarY, sheet.x + Math.round(sheet.w * kiFrac), sheet.kiBarY + 4, DbaUi.OK);

        int speed = accessor.dba$getSpeedPercent();
        DbaUi.slider(context, font, sheet.x, sheet.sliderY, sheet.w, "Speed", speed + "%", speed / 100.0f);
        int presetW = (sheet.w - 6) / 4;
        for (int i = 0; i < PRESETS.length; i++) {
            int px = sheet.x + i * (presetW + 2);
            boolean on = speed == PRESETS[i];
            boolean hover = DbaUi.hit(mouseX, mouseY, px, sheet.presetY, presetW, sheet.presetH);
            DbaUi.button(context, font, px, sheet.presetY, presetW, sheet.presetH, PRESETS[i] + "%", hover || on,
                    on ? DbaUi.Kind.PRIMARY : DbaUi.Kind.SECONDARY);
        }

        for (int i = 0; i < STATS.length; i++) {
            int rowY = sheet.rowTop + i * (sheet.rowH + sheet.gap);
            if (rowY + sheet.rowH > sheet.y + sheet.h) {
                break;
            }
            String stat = STATS[i];
            int value = switch (stat) {
                case "strength" -> accessor.dba$getStrength();
                case "dexterity" -> accessor.dba$getDexterity();
                case "defense" -> accessor.dba$getDefense();
                case "willpower" -> accessor.dba$getWillpower();
                case "spirit" -> accessor.dba$getSpirit();
                default -> accessor.dba$getVitality();
            };
            String raceId = racePath(accessor);
            int upgrades = accessor.dba$getStatUpgradeCount(stat);
            int cost = PlayerStats.getUpgradeCost(raceId, stat, upgrades);
            int gain = PlayerStats.getStatGain(raceId, stat);
            int requiredLevel = (upgrades / 5) * 5 * 2;
            boolean ready = accessor.dba$getLevel() >= requiredLevel && accessor.dba$getStatPoints() >= cost;

            boolean rowHover = DbaUi.hit(mouseX, mouseY, sheet.x, rowY, sheet.w, sheet.rowH);
            DbaUi.row(context, sheet.x, rowY, sheet.w, sheet.rowH, rowHover, false);
            int textY = rowY + Math.max(2, (sheet.rowH - 8) / 2);
            context.text(font, Component.literal(NAMES[i]), sheet.x + 6, textY, DbaUi.TEXT);
            String valueText = DbaMenuScreen.formatCompactNumber(value) + "  +" + DbaMenuScreen.formatCompactNumber(gain);
            int valueX = sheet.x + sheet.w - sheet.btnW - 12 - font.width(valueText);
            context.text(font, Component.literal(valueText), Math.max(sheet.x + 70, valueX), textY, DbaUi.MUTED);

            int btnX = sheet.x + sheet.w - sheet.btnW - 4;
            int btnH = Math.min(DbaUi.BUTTON_H, sheet.rowH - 2);
            int btnY = rowY + (sheet.rowH - btnH) / 2;
            if (ready) {
                boolean hover = DbaUi.hit(mouseX, mouseY, btnX, btnY, sheet.btnW, btnH);
                DbaUi.button(context, font, btnX, btnY, sheet.btnW, btnH,
                        "+ " + DbaMenuScreen.formatCompactNumber(cost) + " AP", hover, DbaUi.Kind.PRIMARY);
            } else {
                String locked = accessor.dba$getLevel() >= requiredLevel
                        ? "Need " + DbaMenuScreen.formatCompactNumber(cost) + " AP"
                        : "Need level " + requiredLevel;
                int lockedW = font.width(locked);
                context.text(font, Component.literal(DbaUi.clip(font, locked, sheet.btnW)),
                        btnX + sheet.btnW - Math.min(lockedW, sheet.btnW), textY, DbaUi.MUTED);
            }
        }
    }

    private void applySpeed(double mouseX, Sheet sheet, PlayerStatsAccessor accessor) {
        float frac = (float) (mouseX - sheet.x) / (float) sheet.w;
        int percent = Math.max(1, Math.min(100, Math.round(frac * 100.0f)));
        if (percent != accessor.dba$getSpeedPercent()) {
            accessor.dba$setSpeedPercent(percent);
            sendSpeed(percent);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        if (event.button() != 0 || parent == null) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (!(client.player instanceof PlayerStatsAccessor accessor)) {
            return false;
        }
        Sheet sheet = sheet();
        double mx = event.x();
        double my = event.y();
        if (DbaUi.sliderHit(sheet.x, sheet.sliderY, sheet.w, mx, my)) {
            draggingSpeed = true;
            applySpeed(mx, sheet, accessor);
            return true;
        }
        int presetW = (sheet.w - 6) / 4;
        for (int i = 0; i < PRESETS.length; i++) {
            int px = sheet.x + i * (presetW + 2);
            if (DbaUi.hit(mx, my, px, sheet.presetY, presetW, sheet.presetH)) {
                DbaUi.playClick();
                accessor.dba$setSpeedPercent(PRESETS[i]);
                sendSpeed(PRESETS[i]);
                return true;
            }
        }
        int btnH = Math.min(DbaUi.BUTTON_H, sheet.rowH - 2);
        for (int i = 0; i < STATS.length; i++) {
            int rowY = sheet.rowTop + i * (sheet.rowH + sheet.gap);
            if (rowY + sheet.rowH > sheet.y + sheet.h) {
                break;
            }
            if (!canUpgrade(accessor, STATS[i])) {
                continue;
            }
            int btnX = sheet.x + sheet.w - sheet.btnW - 4;
            int btnY = rowY + (sheet.rowH - btnH) / 2;
            if (!DbaUi.hit(mx, my, btnX, btnY, sheet.btnW, btnH)) {
                continue;
            }
            DbaUi.playClick();
            sendUpgrade(STATS[i]);
            holding = true;
            heldStat = STATS[i];
            holdStartMs = System.currentTimeMillis();
            lastUpgradeMs = holdStartMs;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (!draggingSpeed || parent == null) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player instanceof PlayerStatsAccessor accessor) {
            applySpeed(event.x(), sheet(), accessor);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        holding = false;
        heldStat = null;
        draggingSpeed = false;
        return false;
    }

    private record Sheet(
            int x, int y, int w, int h,
            int raceY, int levelY, int barY, int kiY, int kiBarY,
            int sliderY, int presetY, int presetH,
            int rowTop, int rowH, int gap, int btnW
    ) {}
}
