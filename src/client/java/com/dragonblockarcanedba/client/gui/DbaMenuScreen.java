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
    // Dimensions for the main content area (tabs stick out to the left)
    private final int bgWidth = 260;
    private final int bgHeight = 180;
    
    // Tab dimensions
    private final int tabWidth = 70;
    private final int tabHeight = 35;
    private final int tabSpacing = 5;

    public DbaMenuScreen() {
        super(Component.literal("Dragon Block Arcane Menu"));
        tabs.add(new StatsTab());
        tabs.add(new FormsTab());
        tabs.add(new TechniquesTab());
        tabs.add(new KiCustomizerTab());
    }

    @Override
    protected void init() {
        // Center the main frame slightly to the right so tabs fit on screen
        this.x = (this.width - bgWidth) / 2 + (tabWidth / 2);
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

    public <T extends net.minecraft.client.gui.components.events.GuiEventListener & net.minecraft.client.gui.components.Renderable & net.minecraft.client.gui.narration.NarratableEntry> T addTabWidget(T widget) {
        return this.addRenderableWidget(widget);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        int bgColor = 0xEE1E2024; // Dark gray, slight transparency
        int borderColor = 0xFF55FF88; // Neon mint green
        int borderThick = 2;

        // Draw main content frame background
        context.fill(x, y, x + bgWidth, y + bgHeight, bgColor);

        // Draw Tabs
        for (int i = 0; i < tabs.size(); i++) {
            int tabX = x - tabWidth;
            int tabY = y + 10 + i * (tabHeight + tabSpacing);
            
            boolean isActive = (i == activeTab);
            int currentTabBg = isActive ? bgColor : 0xEE111111;
            int textCol = isActive ? 0xFFFFFFFF : 0xFFAAAAAA;

            // Tab background
            context.fill(tabX, tabY, tabX + tabWidth, tabY + tabHeight, currentTabBg);
            
            // Tab borders
            context.fill(tabX, tabY, tabX + tabWidth, tabY + borderThick, borderColor); // Top
            context.fill(tabX, tabY + tabHeight - borderThick, tabX + tabWidth, tabY + tabHeight, borderColor); // Bottom
            context.fill(tabX, tabY, tabX + borderThick, tabY + tabHeight, borderColor); // Left
            
            if (!isActive) {
                // If inactive, draw right border so it's a closed box
                context.fill(tabX + tabWidth - borderThick, tabY, tabX + tabWidth, tabY + tabHeight, borderColor);
            }
            
            // Tab text (centered)
            Minecraft client = Minecraft.getInstance();
            int textWidth = client.font.width(tabNames[i]);
            context.text(client.font, tabNames[i], tabX + (tabWidth - textWidth) / 2, tabY + (tabHeight - 8) / 2, textCol, false);
        }

        // Main frame borders (skipping segments where the active tab connects)
        // Top border
        context.fill(x, y, x + bgWidth, y + borderThick, borderColor);
        // Bottom border
        context.fill(x, y + bgHeight - borderThick, x + bgWidth, y + bgHeight, borderColor);
        // Right border
        context.fill(x + bgWidth - borderThick, y, x + bgWidth, y + bgHeight, borderColor);
        
        // Left border logic (leave gap for active tab)
        int activeTabY = y + 10 + activeTab * (tabHeight + tabSpacing);
        context.fill(x, y, x + borderThick, activeTabY, borderColor); // Above active tab
        context.fill(x, activeTabY + tabHeight, x + borderThick, y + bgHeight, borderColor); // Below active tab

        // Delegate content rendering to active tab
        tabs.get(activeTab).render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        // Check for tab clicks
        double mouseX = event.x();
        double mouseY = event.y();
        for (int i = 0; i < tabs.size(); i++) {
            int tabX = x - tabWidth;
            int tabY = y + 10 + i * (tabHeight + tabSpacing);
            if (mouseX >= tabX && mouseX <= tabX + tabWidth && mouseY >= tabY && mouseY <= tabY + tabHeight) {
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
