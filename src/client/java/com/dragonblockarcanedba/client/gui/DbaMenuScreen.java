package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DbaMenuScreen extends Screen {
    private final List<MenuTab> tabs = new ArrayList<>();
    private final String[] tabNames = {"✦ STATS", "⚡ FORMS", "🌌 SKILLS", "🔮 CRAFT", "🎭 EMOTES"};
    private static int lastActiveTab = 2; // Default to Skill Tree
    private int activeTab = lastActiveTab;

    private int x;
    private int y;
    private int bgWidth = 540;
    private int bgHeight = 330;

    public DbaMenuScreen() {
        super(Component.literal("Dragon Block Arcane Menu"));
        tabs.add(new StatsTab());
        tabs.add(new FormsTab());
        tabs.add(new TechniquesTab());
        tabs.add(new KiCustomizerTab());
        tabs.add(new EmotesTab());
    }

    @Override
    protected void init() {
        // Responsive sizing: generous, but clamps to available screen resolution
        this.bgWidth = Math.min(560, Math.max(340, this.width - 20));
        this.bgHeight = Math.min(340, Math.max(220, this.height - 20));
        this.x = (this.width - bgWidth) / 2;
        this.y = (this.height - bgHeight) / 2;

        this.clearWidgets();
        tabs.get(activeTab).init(this);
    }

    public void selectTab(int index) {
        if (index >= 0 && index < tabs.size()) {
            this.activeTab = index;
            lastActiveTab = index;
            init();
        }
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getBgWidth() { return bgWidth; }
    public int getBgHeight() { return bgHeight; }
    public int getContentX() { return x + 6; }
    public int getContentY() { return y + 34; }
    public int getContentWidth() { return bgWidth - 12; }
    public int getContentHeight() { return bgHeight - 40; }

    public <T extends net.minecraft.client.gui.components.events.GuiEventListener & net.minecraft.client.gui.components.Renderable & net.minecraft.client.gui.narration.NarratableEntry> T addTabWidget(T widget) {
        return this.addRenderableWidget(widget);
    }

    public net.minecraft.client.gui.Font getFont() {
        return this.font;
    }

    public static String formatCompactNumber(long num) {
        if (num >= 1_000_000_000L) {
            return String.format(Locale.US, "%.1fB", num / 1_000_000_000.0);
        } else if (num >= 10_000_000L) {
            return String.format(Locale.US, "%.1fM", num / 1_000_000.0);
        } else if (num >= 100_000L) {
            return String.format(Locale.US, "%.0fk", num / 1_000.0);
        }
        return String.format(Locale.US, "%,d", num);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        Minecraft client = Minecraft.getInstance();

        // 1. Outer Dark Obsidian Glass Frame
        context.fill(x - 2, y - 2, x + bgWidth + 2, y + bgHeight + 2, 0x88000000);
        context.fill(x, y, x + bgWidth, y + bgHeight, 0xF20A0E17); // Cosmic slate

        // Glowing outer border
        int borderColor = 0xAA00E5FF;
        context.fill(x, y, x + bgWidth, y + 1, borderColor);
        context.fill(x, y + bgHeight - 1, x + bgWidth, y + bgHeight, borderColor);
        context.fill(x, y, x + 1, y + bgHeight, borderColor);
        context.fill(x + bgWidth - 1, y, x + bgWidth, y + bgHeight, borderColor);

        // 2. Top Header Navigation Bar
        int headerH = 30;
        context.fill(x + 1, y + 1, x + bgWidth - 1, y + headerH, 0xDD0D131F);
        context.fill(x + 1, y + headerH - 1, x + bgWidth - 1, y + headerH, 0x3300E5FF);

        // Right side buttons: Close [✕] and Settings [⚙]
        int closeW = 16;
        int closeH = 16;
        int closeX = x + bgWidth - closeW - 6;
        int closeY = y + 7;
        boolean hoverClose = mouseX >= closeX && mouseX <= closeX + closeW && mouseY >= closeY && mouseY <= closeY + closeH;
        context.fill(closeX, closeY, closeX + closeW, closeY + closeH, hoverClose ? 0xAAFF4444 : 0x33442222);
        context.centeredText(client.font, Component.literal("✕"), closeX + closeW / 2, closeY + 4, hoverClose ? 0xFFFFFFFF : 0xFFAAAAAA);

        int gearW = 16;
        int gearH = 16;
        int gearX = closeX - gearW - 4;
        int gearY = closeY;
        boolean hoverGear = mouseX >= gearX && mouseX <= gearX + gearW && mouseY >= gearY && mouseY <= gearY + gearH;
        context.fill(gearX, gearY, gearX + gearW, gearY + gearH, hoverGear ? 0xAA00C853 : 0x33112818);
        context.centeredText(client.font, Component.literal("⚙"), gearX + gearW / 2, gearY + 4, hoverGear ? 0xFFFFFFFF : 0xFF55FF88);

        // Calculate available space for tabs vs status pill
        int rightMargin = gearX - 6;

        // Draw Left Navigation Tabs (Compact & Responsive)
        int tabX = x + 6;
        for (int i = 0; i < tabs.size(); i++) {
            String name = tabNames[i];
            int textW = client.font.width(name);
            int tabW = textW + 10;
            int tabY = y + 4;
            int tabH = headerH - 8;

            boolean isActive = (i == activeTab);
            boolean isHovered = mouseX >= tabX && mouseX <= tabX + tabW && mouseY >= tabY && mouseY <= tabY + tabH;

            if (isActive) {
                context.fill(tabX, tabY, tabX + tabW, tabY + tabH, 0x3355FF88);
                context.fill(tabX, tabY + tabH - 2, tabX + tabW, tabY + tabH, 0xFF55FF88);
            } else if (isHovered) {
                context.fill(tabX, tabY, tabX + tabW, tabY + tabH, 0x22FFFFFF);
            }

            int textCol = isActive ? 0xFF55FF88 : (isHovered ? 0xFFFFFFFF : 0xFF8899A6);
            context.centeredText(client.font, Component.literal(name), tabX + tabW / 2, tabY + 4, textCol);
            tabX += tabW + 3;
        }

        // Consolidated Single Status Pill (Never overlaps!)
        if (client.player instanceof PlayerStatsAccessor accessor) {
            String race = accessor.dba$getRaceId() != null ? accessor.dba$getRaceId().getPath().replace("_", " ").toUpperCase() : "MORTAL";
            long lvl = accessor.dba$getLevel();
            long ap = accessor.dba$getStatPoints();

            int availableW = rightMargin - tabX - 8;
            if (availableW > 50) {
                // Try full status string: RACE • LV. X • ✦ X AP
                String fullStatus = race + " • LV." + formatCompactNumber(lvl) + " • ✦ " + formatCompactNumber(ap) + " AP";
                String displayStatus = fullStatus;

                if (client.font.width(displayStatus) > availableW) {
                    displayStatus = "LV." + formatCompactNumber(lvl) + " • ✦ " + formatCompactNumber(ap) + " AP";
                }
                if (client.font.width(displayStatus) > availableW) {
                    displayStatus = "✦ " + formatCompactNumber(ap) + " AP";
                }

                int pillW = client.font.width(displayStatus) + 12;
                int pillX = rightMargin - pillW;
                int pillY = y + 6;
                int pillH = 18;

                context.fill(pillX, pillY, pillX + pillW, pillY + pillH, 0x66162233);
                context.fill(pillX, pillY, pillX + pillW, pillY + 1, 0x5500E5FF);
                context.fill(pillX, pillY + pillH - 1, pillX + pillW, pillY + pillH, 0x5500E5FF);
                context.fill(pillX, pillY, pillX + 1, pillY + pillH, 0x5500E5FF);
                context.fill(pillX + pillW - 1, pillY, pillX + pillW, pillY + pillH, 0x5500E5FF);

                context.centeredText(client.font, Component.literal(displayStatus), pillX + pillW / 2, pillY + 5, 0xFFFFAA00);
            }
        }

        // 3. Render Active Tab Content
        tabs.get(activeTab).render(context, mouseX, mouseY, delta);
    }

    public static void playClickSound() {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.getSoundManager() != null) {
            client.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        double mouseX = event.x();
        double mouseY = event.y();
        Minecraft client = Minecraft.getInstance();

        // Close button click
        int closeW = 16;
        int closeH = 16;
        int closeX = x + bgWidth - closeW - 6;
        int closeY = y + 7;
        if (mouseX >= closeX && mouseX <= closeX + closeW && mouseY >= closeY && mouseY <= closeY + closeH) {
            playClickSound();
            this.onClose();
            return true;
        }

        // Settings gear button click
        int gearW = 16;
        int gearH = 16;
        int gearX = closeX - gearW - 4;
        int gearY = closeY;
        if (mouseX >= gearX && mouseX <= gearX + gearW && mouseY >= gearY && mouseY <= gearY + gearH) {
            playClickSound();
            client.setScreenAndShow(new DbaSettingsScreen(this));
            return true;
        }

        // Tab clicks
        int tabX = x + 6;
        int headerH = 30;
        for (int i = 0; i < tabs.size(); i++) {
            String name = tabNames[i];
            int textW = client.font.width(name);
            int tabW = textW + 10;
            int tabY = y + 4;
            int tabH = headerH - 8;

            if (mouseX >= tabX && mouseX <= tabX + tabW && mouseY >= tabY && mouseY <= tabY + tabH) {
                playClickSound();
                selectTab(i);
                return true;
            }
            tabX += tabW + 3;
        }

        // Delegate to active tab
        if (tabs.get(activeTab).mouseClicked(event, isRepeat)) {
            return true;
        }
        return super.mouseClicked(event, isRepeat);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (tabs.get(activeTab).mouseDragged(event, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (tabs.get(activeTab).mouseReleased(event)) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (tabs.get(activeTab).mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (com.dragonblockarcanedba.client.DragonBlockArcaneDBAClient.openMenuKey.matches(event)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }
}
