package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Character menu. A left rail holds the five sections, Settings, and Close.
 * The sheet itself fills the rest of the window on the pause-menu background.
 */
public class DbaMenuScreen extends Screen {
    private static final String[] TAB_LABELS = {"Stats", "Forms", "Skills", "Craft", "Emotes"};
    private static int lastActiveTab = 0;

    private final Screen parent;
    private final List<MenuTab> tabs = new ArrayList<>();
    private int activeTab = lastActiveTab;

    private int x;
    private int y;
    private int bgWidth = 640;
    private int bgHeight = 380;

    public DbaMenuScreen() {
        this(null);
    }

    public DbaMenuScreen(Screen parent) {
        super(Component.literal("Character"));
        this.parent = parent;
        tabs.add(new StatsTab());
        tabs.add(new FormsTab());
        tabs.add(new TechniquesTab());
        tabs.add(new KiCustomizerTab());
        tabs.add(new EmotesTab());
        if (activeTab < 0 || activeTab >= tabs.size()) {
            activeTab = 0;
        }
    }

    @Override
    protected void init() {
        int margin = 8;
        this.bgWidth = Math.min(680, Math.max(440, this.width - margin * 2));
        this.bgHeight = Math.min(460, Math.max(300, this.height - margin * 2));
        if (this.bgWidth > this.width - 8) {
            this.bgWidth = Math.max(280, this.width - 8);
        }
        if (this.bgHeight > this.height - 8) {
            this.bgHeight = Math.max(200, this.height - 8);
        }
        this.x = (this.width - bgWidth) / 2;
        this.y = (this.height - bgHeight) / 2;
        this.clearWidgets();
        tabs.get(activeTab).init(this);
    }

    public void selectTab(int index) {
        if (index >= 0 && index < tabs.size() && index != activeTab) {
            this.activeTab = index;
            lastActiveTab = index;
            this.clearWidgets();
            tabs.get(activeTab).init(this);
        }
    }

    public void openCraft(int slot) {
        this.activeTab = 3;
        lastActiveTab = 3;
        this.clearWidgets();
        tabs.get(3).init(this);
        if (tabs.get(3) instanceof KiCustomizerTab craft) {
            craft.focusSlot(slot);
        }
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getBgWidth() { return bgWidth; }
    public int getBgHeight() { return bgHeight; }

    private int railWidth() { return 104; }
    private int railX() { return x + 6; }

    private int settingsY() { return y + bgHeight - DbaUi.BUTTON_H * 2 - 12; }
    private int closeButtonY() { return y + bgHeight - DbaUi.BUTTON_H - 6; }

    private int tabTop() { return y + 8; }
    private int tabBottom() { return settingsY() - 6; }

    private int tabHeight() {
        int gaps = DbaUi.GAP * (TAB_LABELS.length - 1);
        int available = tabBottom() - tabTop() - gaps;
        return Math.max(16, Math.min(26, available / TAB_LABELS.length));
    }

    private int tabY(int index) {
        return tabTop() + index * (tabHeight() + DbaUi.GAP);
    }

    private int railButtonW() { return railWidth() - 8; }

    public int getContentX() { return x + railWidth() + 12; }
    public int getContentY() { return y + 28; }
    public int getContentWidth() { return bgWidth - railWidth() - 20; }
    public int getContentHeight() { return bgHeight - 36; }

    public <T extends net.minecraft.client.gui.components.events.GuiEventListener
            & net.minecraft.client.gui.components.Renderable
            & net.minecraft.client.gui.narration.NarratableEntry> T addTabWidget(T widget) {
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

    public static void playClickSound() {
        DbaUi.playClick();
    }

    private String statusLine() {
        Minecraft client = Minecraft.getInstance();
        if (!(client.player instanceof PlayerStatsAccessor accessor)) {
            return "Not in a world";
        }
        String race = accessor.dba$getRaceId() != null
                ? accessor.dba$getRaceId().getPath().replace('_', ' ')
                : "Unchosen";
        race = race.substring(0, 1).toUpperCase(Locale.ROOT) + race.substring(1);
        return race
                + "    Level " + formatCompactNumber(accessor.dba$getLevel())
                + "    " + formatCompactNumber(accessor.dba$getStatPoints()) + " AP";
    }

    @Override
    public void tick() {
        super.tick();
        tabs.get(activeTab).tick();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        DbaUi.dim(context, this.width, this.height);
        DbaUi.frame(context, x, y, bgWidth, bgHeight);
        context.fill(x + railWidth() + 6, y + 8, x + railWidth() + 7, y + bgHeight - 8, DbaUi.LINE);

        int tabW = railButtonW();
        int tabH = tabHeight();
        for (int i = 0; i < TAB_LABELS.length; i++) {
            int ty = tabY(i);
            boolean hover = DbaUi.hit(mouseX, mouseY, railX(), ty, tabW, tabH);
            DbaUi.tab(context, this.font, railX(), ty, tabW, tabH, TAB_LABELS[i], i == activeTab, hover);
        }

        boolean settingsHover = DbaUi.hit(mouseX, mouseY, railX(), settingsY(), tabW, DbaUi.BUTTON_H);
        boolean closeHover = DbaUi.hit(mouseX, mouseY, railX(), closeButtonY(), tabW, DbaUi.BUTTON_H);
        DbaUi.button(context, this.font, railX(), settingsY(), tabW, DbaUi.BUTTON_H, "Settings", settingsHover, DbaUi.Kind.SECONDARY);
        DbaUi.button(context, this.font, railX(), closeButtonY(), tabW, DbaUi.BUTTON_H, "Close", closeHover, DbaUi.Kind.SECONDARY);

        context.text(this.font, Component.literal("Character"), getContentX(), y + 8, DbaUi.TEXT);
        context.text(this.font, Component.literal(DbaUi.clip(this.font, statusLine(), getContentWidth())),
                getContentX(), y + 18, DbaUi.MUTED);

        tabs.get(activeTab).render(context, mouseX, mouseY, delta);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        double mx = event.x();
        double my = event.y();
        if (event.button() != 0) {
            return super.mouseClicked(event, isRepeat);
        }
        int tabW = railButtonW();
        if (DbaUi.hit(mx, my, railX(), closeButtonY(), tabW, DbaUi.BUTTON_H)) {
            DbaUi.playClick();
            this.onClose();
            return true;
        }
        if (DbaUi.hit(mx, my, railX(), settingsY(), tabW, DbaUi.BUTTON_H)) {
            DbaUi.playClick();
            if (this.minecraft != null) {
                this.minecraft.setScreenAndShow(new DbaSettingsScreen(this));
            }
            return true;
        }
        int tabH = tabHeight();
        for (int i = 0; i < TAB_LABELS.length; i++) {
            if (DbaUi.hit(mx, my, railX(), tabY(i), tabW, tabH)) {
                if (i != activeTab) {
                    DbaUi.playClick();
                    selectTab(i);
                }
                return true;
            }
        }
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
    public boolean keyPressed(KeyEvent event) {
        if (DbaUi.escape(event)) {
            this.onClose();
            return true;
        }
        if (com.dragonblockarcanedba.client.DragonBlockArcaneDBAClient.openMenuKey != null
                && com.dragonblockarcanedba.client.DragonBlockArcaneDBAClient.openMenuKey.matches(event)) {
            while (com.dragonblockarcanedba.client.DragonBlockArcaneDBAClient.openMenuKey.consumeClick()) {
                // The press is handled here so the client tick does not open the menu again.
            }
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        if (this.minecraft == null) {
            return;
        }
        if (this.parent != null) {
            this.minecraft.setScreenAndShow(this.parent);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
