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

public class DbaMenuScreen extends Screen {
    private static final String[] TAB_LABELS = {"Stats", "Forms", "Skills", "Craft", "Emotes"};
    private static int lastActiveTab = 0;

    private final Screen parent;
    private final List<MenuTab> tabs = new ArrayList<>();
    private int activeTab = lastActiveTab;

    private int x;
    private int y;
    private int bgWidth = 560;
    private int bgHeight = 360;

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
        int margin = 12;
        this.bgWidth = Math.min(620, Math.max(360, this.width - margin * 2));
        this.bgHeight = Math.min(400, Math.max(250, this.height - margin * 2));
        if (this.bgWidth > this.width - 8) {
            this.bgWidth = Math.max(200, this.width - 8);
        }
        if (this.bgHeight > this.height - 8) {
            this.bgHeight = Math.max(180, this.height - 8);
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
    public int getContentX() { return x + DbaUi.PAD; }
    public int getContentY() { return y + DbaUi.HEADER + DbaUi.TAB + DbaUi.STATUS; }
    public int getContentWidth() { return bgWidth - DbaUi.PAD * 2; }
    public int getContentHeight() { return bgHeight - DbaUi.HEADER - DbaUi.TAB - DbaUi.STATUS - DbaUi.PAD; }

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

    private int tabWidth() {
        int count = TAB_LABELS.length;
        int inner = bgWidth - DbaUi.PAD * 2;
        int gaps = DbaUi.GAP * (count - 1);
        return Math.max(28, (inner - gaps) / count);
    }

    private int tabLeft() {
        return x + DbaUi.PAD;
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

        int settingsW = 72;
        int settingsX = DbaUi.closeX(x, bgWidth) - settingsW - 4;
        int settingsY = DbaUi.closeY(y);
        DbaUi.title(context, this.font, x, y, settingsX - x - 8, "Character");
        DbaUi.button(context, this.font, settingsX, settingsY, settingsW, DbaUi.BUTTON_H, "Settings",
                DbaUi.hit(mouseX, mouseY, settingsX, settingsY, settingsW, DbaUi.BUTTON_H), DbaUi.Kind.SECONDARY);
        DbaUi.closeControl(context, this.font, x, y, bgWidth, mouseX, mouseY);

        int tabY = y + DbaUi.HEADER;
        int tabW = tabWidth();
        int tabX = tabLeft();
        for (int i = 0; i < TAB_LABELS.length; i++) {
            boolean hover = DbaUi.hit(mouseX, mouseY, tabX, tabY, tabW, DbaUi.TAB);
            DbaUi.tab(context, this.font, tabX, tabY, tabW, DbaUi.TAB, TAB_LABELS[i], i == activeTab, hover);
            tabX += tabW + DbaUi.GAP;
        }

        int statusY = y + DbaUi.HEADER + DbaUi.TAB;
        DbaUi.hairline(context, x, statusY + DbaUi.STATUS - 1, bgWidth);
        Minecraft client = Minecraft.getInstance();
        String status = "Not in a world";
        if (client.player instanceof PlayerStatsAccessor accessor) {
            String race = accessor.dba$getRaceId() != null
                    ? accessor.dba$getRaceId().getPath().replace('_', ' ')
                    : "Unchosen";
            race = race.substring(0, 1).toUpperCase(Locale.ROOT) + race.substring(1);
            status = race
                    + "    Level " + formatCompactNumber(accessor.dba$getLevel())
                    + "    " + formatCompactNumber(accessor.dba$getStatPoints()) + " AP";
        }
        context.text(this.font, Component.literal(DbaUi.clip(this.font, status, bgWidth - 24)), x + 12, statusY + 4, DbaUi.MUTED);

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
        if (DbaUi.closeHit(x, y, bgWidth, mx, my)) {
            DbaUi.playClick();
            this.onClose();
            return true;
        }
        int settingsW = 72;
        int settingsX = DbaUi.closeX(x, bgWidth) - settingsW - 4;
        int settingsY = DbaUi.closeY(y);
        if (DbaUi.hit(mx, my, settingsX, settingsY, settingsW, DbaUi.BUTTON_H)) {
            DbaUi.playClick();
            if (this.minecraft != null) {
                this.minecraft.setScreenAndShow(new DbaSettingsScreen(this));
            }
            return true;
        }
        int tabY = y + DbaUi.HEADER;
        int tabW = tabWidth();
        int tabX = tabLeft();
        for (int i = 0; i < TAB_LABELS.length; i++) {
            if (DbaUi.hit(mx, my, tabX, tabY, tabW, DbaUi.TAB)) {
                if (i != activeTab) {
                    DbaUi.playClick();
                    selectTab(i);
                }
                return true;
            }
            tabX += tabW + DbaUi.GAP;
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
