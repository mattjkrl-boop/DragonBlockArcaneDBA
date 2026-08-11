package com.dragonblockarcanedba.client.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.List;

public class DbaMenuScreen extends Screen {
    private final List<MenuTab> tabs = new ArrayList<>();
    private final String[] tabNames = {"STATS", "FORMS", "TECHS", "KI CUSTOMIZER"};
    private int activeTab = 0;

    private int x;
    private int y;
    // Dimensions for the overall window
    private final int bgWidth = 370;
    private final int bgHeight = 230;
    
    // Sidebar dimensions
    private final int sidebarWidth = 100;
    private final int tabHeight = 35;
    private final int tabSpacing = 2;

    public DbaMenuScreen() {
        super(Component.literal("Dragon Block Arcane Menu"));
        tabs.add(new StatsTab());
        tabs.add(new FormsTab());
        tabs.add(new TechniquesTab());
        tabs.add(new KiCustomizerTab());
    }

    @Override
    protected void init() {
        // Center the main frame on screen
        this.x = (this.width - bgWidth) / 2;
        this.y = (this.height - bgHeight) / 2;

        this.clearWidgets();
        
        // Initialize the active tab (it may add its own widgets)
        tabs.get(activeTab).init(this);
    }

    private void selectTab(int index) {
        if (index >= 0 && index < tabs.size()) {
            this.activeTab = index;
            init(); // Re-initialize the screen for the new tab widgets
        }
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getBgWidth() { return bgWidth; }
    public int getBgHeight() { return bgHeight; }
    public int getSidebarWidth() { return sidebarWidth; }
    public int getContentX() { return x + sidebarWidth; }
    public int getContentWidth() { return bgWidth - sidebarWidth; }

    public <T extends net.minecraft.client.gui.components.events.GuiEventListener & net.minecraft.client.gui.components.Renderable & net.minecraft.client.gui.narration.NarratableEntry> T addTabWidget(T widget) {
        return this.addRenderableWidget(widget);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        int sidebarBg = 0xEE11151A; // Dark blue/gray
        int contentBg = 0xEE1A1C20; // Slightly lighter sleek gray
        int borderColor = 0xAA55FF88; // Sleek mint green accent
        int borderThick = 2;

        // Draw sidebar background
        context.fill(x, y, x + sidebarWidth, y + bgHeight, sidebarBg);
        // Draw content background
        context.fill(x + sidebarWidth, y, x + bgWidth, y + bgHeight, contentBg);

        // Draw Tabs
        for (int i = 0; i < tabs.size(); i++) {
            int tabX = x;
            int tabY = y + 20 + i * (tabHeight + tabSpacing);
            
            boolean isActive = (i == activeTab);
            boolean isHovered = (mouseX >= tabX && mouseX <= tabX + sidebarWidth && mouseY >= tabY && mouseY <= tabY + tabHeight);
            
            int textCol = isActive ? 0xFF55FF88 : (isHovered ? 0xFFFFFFFF : 0xFFAAAAAA);

            if (isActive) {
                // Active highlight background
                context.fill(tabX, tabY, tabX + sidebarWidth, tabY + tabHeight, 0x2255FF88);
                // Active left accent line
                context.fill(tabX, tabY, tabX + 3, tabY + tabHeight, 0xFF55FF88);
            } else if (isHovered) {
                // Hover highlight background
                context.fill(tabX, tabY, tabX + sidebarWidth, tabY + tabHeight, 0x11FFFFFF);
                // Hover accent line
                context.fill(tabX, tabY, tabX + 3, tabY + tabHeight, 0x66FFFFFF);
            }
            
            // Tab text (left-aligned with padding)
            Minecraft client = Minecraft.getInstance();
            context.text(client.font, tabNames[i], tabX + 15, tabY + (tabHeight - 8) / 2, textCol, false);
        }

        // Main frame borders
        // Top border
        context.fill(x, y, x + bgWidth, y + borderThick, borderColor);
        // Bottom border
        context.fill(x, y + bgHeight - borderThick, x + bgWidth, y + bgHeight, borderColor);
        // Right border
        context.fill(x + bgWidth - borderThick, y, x + bgWidth, y + bgHeight, borderColor);
        // Left border
        context.fill(x, y, x + borderThick, y + bgHeight, borderColor);
        // Divider line between sidebar and content
        context.fill(x + sidebarWidth - 1, y, x + sidebarWidth, y + bgHeight, 0x4455FF88);

        // Delegate content rendering to active tab
        tabs.get(activeTab).render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        double mouseX = event.x();
        double mouseY = event.y();
        
        // Check for tab clicks
        for (int i = 0; i < tabs.size(); i++) {
            int tabX = x;
            int tabY = y + 20 + i * (tabHeight + tabSpacing);
            if (mouseX >= tabX && mouseX <= tabX + sidebarWidth && mouseY >= tabY && mouseY <= tabY + tabHeight) {
                selectTab(i);
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
