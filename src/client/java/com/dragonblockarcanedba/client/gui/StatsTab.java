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
 * Character sheet. The left column is the dossier. The right column is six stat rows.
 * Render and clicks share one layout so the slider and upgrade buttons land on the pixels they draw.
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
        int gap = DbaUi.GAP;
        int dossierW = Math.min(200, Math.max(148, w * 36 / 100));
        int gridW = w - dossierW - gap;
        if (gridW < 170) {
            dossierW = Math.max(136, w - 170 - gap);
            gridW = w - dossierW - gap;
        }
        int colW = Math.max(40, (gridW - gap) / 2);
        int rowH = Math.max(1, (h - gap * 2) / 3);
        int bottom = y + h;
        int presetH = DbaUi.BUTTON_H;
        int presetY = bottom - presetH;
        int sliderY = presetY - 22;
        int speedY = sliderY - 12;
        return new Sheet(x, y, dossierW, h, x + dossierW + gap, y, colW, rowH, gap, speedY, sliderY, presetY, presetH);
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

        DbaUi.panel(context, sheet.dx, sheet.dy, sheet.dw, sheet.dh);

        int cy = sheet.dy + 6;
        String race = accessor.dba$getRaceId() != null
                ? accessor.dba$getRaceId().getPath().replace('_', ' ').toUpperCase(Locale.ROOT)
                : "UNCHOSEN";
        context.text(font, Component.literal(DbaUi.clip(font, race, sheet.dw - 12)), sheet.dx + 6, cy, DbaUi.GOLD);
        cy += 14;

        long total = accessor.dba$getStrength() + accessor.dba$getDexterity() + accessor.dba$getDefense()
                + accessor.dba$getWillpower() + accessor.dba$getSpirit() + accessor.dba$getVitality();
        long power = total * 250L + accessor.dba$getLevel() * 1000L;
        if (cy + 22 < sheet.speedY) {
            context.text(font, Component.literal("Combat power"), sheet.dx + 6, cy, DbaUi.MUTED);
            String powerText = DbaMenuScreen.formatCompactNumber(power);
            context.text(font, Component.literal(powerText), sheet.dx + sheet.dw - 6 - font.width(powerText), cy, DbaUi.TEXT);
            cy += 14;
        }

        int level = accessor.dba$getLevel();
        long xp = accessor.dba$getXp();
        long need = PlayerStats.getXpToNextLevel(level);
        float xpFrac = need > 0 ? Math.min(1.0f, (float) xp / need) : 1.0f;
        if (cy + 18 < sheet.speedY) {
            String levelText = "Level " + DbaMenuScreen.formatCompactNumber(level);
            context.text(font, Component.literal(levelText), sheet.dx + 6, cy, DbaUi.TEXT);
            String pct = String.format(Locale.US, "%.0f%%", xpFrac * 100.0f);
            context.text(font, Component.literal(pct), sheet.dx + sheet.dw - 6 - font.width(pct), cy, DbaUi.MUTED);
            cy += 11;
            int barW = sheet.dw - 12;
            context.fill(sheet.dx + 6, cy, sheet.dx + 6 + barW, cy + 4, DbaUi.INK_DEEP);
            context.fill(sheet.dx + 6, cy, sheet.dx + 6 + Math.round(barW * xpFrac), cy + 4, DbaUi.GOLD);
            cy += 12;
        }

        double maxKi = PlayerStats.getMaxKi(client.player);
        double curKi = accessor.dba$getCurrentKi();
        float kiFrac = maxKi > 0 ? (float) Math.min(1.0, curKi / maxKi) : 1.0f;
        if (cy + 18 < sheet.speedY) {
            context.text(font, Component.literal("Ki"), sheet.dx + 6, cy, DbaUi.MUTED);
            String kiText = DbaMenuScreen.formatCompactNumber((long) curKi) + " / " + DbaMenuScreen.formatCompactNumber((long) maxKi);
            context.text(font, Component.literal(DbaUi.clip(font, kiText, sheet.dw / 2)),
                    sheet.dx + sheet.dw - 6 - Math.min(font.width(kiText), sheet.dw / 2), cy, DbaUi.TEXT);
            cy += 11;
            int barW = sheet.dw - 12;
            context.fill(sheet.dx + 6, cy, sheet.dx + 6 + barW, cy + 4, DbaUi.INK_DEEP);
            context.fill(sheet.dx + 6, cy, sheet.dx + 6 + Math.round(barW * kiFrac), cy + 4, DbaUi.OK);
        }

        int speed = accessor.dba$getSpeedPercent();
        context.text(font, Component.literal("Speed"), sheet.dx + 6, sheet.speedY, DbaUi.MUTED);
        String speedText = speed + "%";
        context.text(font, Component.literal(speedText), sheet.dx + sheet.dw - 6 - font.width(speedText), sheet.speedY, DbaUi.GOLD);
        int trackX = sheet.dx + 6;
        int trackW = sheet.dw - 12;
        DbaUi.sliderTrack(context, trackX, sheet.sliderY, trackW, 20, speed / 100.0f);

        int presetW = (trackW - 6) / 4;
        for (int i = 0; i < PRESETS.length; i++) {
            int px = trackX + i * (presetW + 2);
            boolean on = speed == PRESETS[i];
            boolean hover = DbaUi.hit(mouseX, mouseY, px, sheet.presetY, presetW, sheet.presetH);
            DbaUi.button(context, font, px, sheet.presetY, presetW, sheet.presetH, PRESETS[i] + "%", hover || on,
                    on ? DbaUi.Kind.PRIMARY : DbaUi.Kind.SECONDARY);
        }

        for (int i = 0; i < STATS.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int cardX = sheet.gx + col * (sheet.colW + sheet.gap);
            int cardY = sheet.gy + row * (sheet.rowH + sheet.gap);
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
            boolean levelMet = accessor.dba$getLevel() >= requiredLevel;
            boolean affordable = accessor.dba$getStatPoints() >= cost;
            boolean ready = levelMet && affordable;

            DbaUi.panel(context, cardX, cardY, sheet.colW, sheet.rowH);

            int btnH = DbaUi.BUTTON_H;
            int btnY = cardY + sheet.rowH - btnH - 4;
            int textRight = sheet.colW - 10;
            context.text(font, Component.literal(DbaUi.clip(font, NAMES[i], textRight)), cardX + 6, cardY + 5, DbaUi.TEXT);
            String valueText = DbaMenuScreen.formatCompactNumber(value) + "  +" + DbaMenuScreen.formatCompactNumber(gain);
            if (btnY >= cardY + 16) {
                context.text(font, Component.literal(DbaUi.clip(font, valueText, textRight)), cardX + 6, cardY + 16, DbaUi.MUTED);
            }

            if (ready && btnY > cardY + 8) {
                boolean hover = DbaUi.hit(mouseX, mouseY, cardX + 6, btnY, sheet.colW - 12, btnH);
                String label = "+ " + DbaMenuScreen.formatCompactNumber(cost) + " AP";
                DbaUi.button(context, font, cardX + 6, btnY, sheet.colW - 12, btnH, label, hover, DbaUi.Kind.PRIMARY);
            } else if (btnY > cardY + 8) {
                String locked = levelMet
                        ? "Need " + DbaMenuScreen.formatCompactNumber(cost) + " AP"
                        : "Need level " + requiredLevel;
                context.text(font, Component.literal(DbaUi.clip(font, locked, textRight)), cardX + 6, btnY + 4, DbaUi.MUTED);
            }
        }
    }

    private void applySpeed(double mouseX, Sheet sheet, PlayerStatsAccessor accessor) {
        int trackX = sheet.dx + 6;
        int trackW = sheet.dw - 12;
        float frac = (float) (mouseX - trackX) / (float) trackW;
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
        int trackX = sheet.dx + 6;
        int trackW = sheet.dw - 12;
        if (DbaUi.hit(mx, my, trackX, sheet.sliderY, trackW, 20)) {
            draggingSpeed = true;
            applySpeed(mx, sheet, accessor);
            return true;
        }
        int presetW = (trackW - 6) / 4;
        for (int i = 0; i < PRESETS.length; i++) {
            int px = trackX + i * (presetW + 2);
            if (DbaUi.hit(mx, my, px, sheet.presetY, presetW, sheet.presetH)) {
                DbaUi.playClick();
                accessor.dba$setSpeedPercent(PRESETS[i]);
                sendSpeed(PRESETS[i]);
                return true;
            }
        }
        for (int i = 0; i < STATS.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int cardX = sheet.gx + col * (sheet.colW + sheet.gap);
            int cardY = sheet.gy + row * (sheet.rowH + sheet.gap);
            int btnH = DbaUi.BUTTON_H;
            int btnY = cardY + sheet.rowH - btnH - 4;
            if (btnY <= cardY + 8) {
                continue;
            }
            if (!canUpgrade(accessor, STATS[i])) {
                continue;
            }
            if (!DbaUi.hit(mx, my, cardX + 6, btnY, sheet.colW - 12, btnH)) {
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
            int dx, int dy, int dw, int dh,
            int gx, int gy, int colW, int rowH, int gap,
            int speedY, int sliderY, int presetY, int presetH
    ) {}
}
