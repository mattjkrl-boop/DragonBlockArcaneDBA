package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.network.ActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

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
    private final int tabHeight = 33;
    private final int tabSpacing = 2;

    // Speed slider dragging state
    private boolean isDraggingSpeedSlider = false;

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
        this.isDraggingSpeedSlider = false;

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

    public int getSpeedCardX() { return x + 6; }
    public int getSpeedCardY() { return y + 160; }
    public int getSpeedCardW() { return sidebarWidth - 12; }
    public int getSpeedCardH() { return 62; }

    public int getSliderX() { return getSpeedCardX() + 6; }
    public int getSliderY() { return getSpeedCardY() + 28; }
    public int getSliderW() { return getSpeedCardW() - 12; }
    public int getSliderH() { return 10; }

    private void updateSpeedFromMouse(double mouseX, PlayerStatsAccessor accessor) {
        int sliderX = getSliderX();
        int sliderW = getSliderW();
        float frac = (float) (mouseX - sliderX) / (float) sliderW;
        int percent = Math.max(1, Math.min(100, Math.round(frac * 100.0f)));
        if (percent != accessor.dba$getSpeedPercent()) {
            accessor.dba$setSpeedPercent(percent);
            sendSpeedPercent(percent);
        }
    }

    private void sendSpeedPercent(int percent) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("action", "set_speed_percent");
        nbt.putInt("percent", percent);
        ClientPlayNetworking.send(new ActionPayload(nbt));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        int sidebarBg = 0xAA11151A; // Dark blue/gray, but lighter alpha
        int contentBg = 0xAA1A1C20; // Slightly lighter sleek gray
        int borderColor = 0xAA55FF88; // Sleek mint green accent
        int borderThick = 2;

        // Draw sidebar background
        context.fill(x, y, x + sidebarWidth, y + bgHeight, sidebarBg);
        // Draw content background
        context.fill(x + sidebarWidth, y, x + bgWidth, y + bgHeight, contentBg);

        // Draw Tabs
        for (int i = 0; i < tabs.size(); i++) {
            int tabX = x;
            int tabY = y + 14 + i * (tabHeight + tabSpacing);
            
            boolean isActive = (i == activeTab);
            boolean isHovered = (mouseX >= tabX && mouseX <= tabX + sidebarWidth && mouseY >= tabY && mouseY <= tabY + tabHeight);
            
            int textCol = isActive ? 0xFF55FF88 : (isHovered ? 0xFFFFFFFF : 0xFFAAAAAA);

            if (isActive) {
                // Active highlight background
                context.fill(tabX, tabY, tabX + sidebarWidth, tabY + tabHeight, 0x4455FF88);
                // Active left accent line
                context.fill(tabX, tabY, tabX + 3, tabY + tabHeight, 0xFF55FF88);
            } else if (isHovered) {
                // Hover highlight background
                context.fill(tabX, tabY, tabX + sidebarWidth, tabY + tabHeight, 0x33FFFFFF);
                // Hover accent line
                context.fill(tabX, tabY, tabX + 3, tabY + tabHeight, 0xAAFFFFFF);
            } else {
                // Inactive tabs get a slightly darker backing
                context.fill(tabX, tabY, tabX + sidebarWidth, tabY + tabHeight, 0x44000000);
            }
            
            // Tab text (left-aligned with padding)
            Minecraft client = Minecraft.getInstance();
            context.text(client.font, tabNames[i], tabX + 15, tabY + (tabHeight - 8) / 2, textCol, false);
        }

        // Draw Dedicated Speed Control Card in the sidebar off to the side
        Minecraft client = Minecraft.getInstance();
        if (client.player instanceof PlayerStatsAccessor accessor) {
            int cardX = getSpeedCardX();
            int cardY = getSpeedCardY();
            int cardW = getSpeedCardW();
            int cardH = getSpeedCardH();
            int sliderX = getSliderX();
            int sliderY = getSliderY();
            int sliderW = getSliderW();
            int sliderH = getSliderH();
            int speedPct = accessor.dba$getSpeedPercent();

            boolean hoverCard = (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= cardY && mouseY <= cardY + cardH);
            boolean hoverSlider = (mouseX >= sliderX - 3 && mouseX <= sliderX + sliderW + 3 && mouseY >= sliderY - 3 && mouseY <= sliderY + sliderH + 3);

            // Card background & borders
            int cardBg = hoverCard ? 0x99111822 : 0x770D1117;
            int cardBorder = hoverCard ? 0xAA00E5FF : 0x4455FF88;
            context.fill(cardX, cardY, cardX + cardW, cardY + cardH, cardBg);
            context.fill(cardX, cardY, cardX + cardW, cardY + 1, 0xFF00E5FF); // top accent line
            context.fill(cardX, cardY + cardH - 1, cardX + cardW, cardY + cardH, cardBorder);
            context.fill(cardX, cardY, cardX + 1, cardY + cardH, cardBorder);
            context.fill(cardX + cardW - 1, cardY, cardX + cardW, cardY + cardH, cardBorder);

            // Header line: "SPEED" and percentage
            context.text(client.font, Component.literal("SPEED"), cardX + 6, cardY + 6, 0xFF00E5FF);
            String pctStr = speedPct + "%";
            int pctW = client.font.width(pctStr);
            context.text(client.font, Component.literal(pctStr), cardX + cardW - 6 - pctW, cardY + 6, 0xFF55FF88);

            // Subtitle
            context.centeredText(client.font, Component.literal("Movement Limit"), cardX + cardW / 2, cardY + 17, 0xFF8899A6);

            // Slider Track background
            context.fill(sliderX, sliderY, sliderX + sliderW, sliderY + sliderH, 0x88000000);

            // Slider Filled progress
            int fillW = Math.max(2, (int) (sliderW * (speedPct / 100.0f)));
            int fillColor = (hoverSlider || isDraggingSpeedSlider) ? 0xEE00E5FF : 0xAA00B0FF;
            context.fill(sliderX, sliderY, sliderX + fillW, sliderY + sliderH, fillColor);

            // Slider Track Borders
            int trackBorder = (hoverSlider || isDraggingSpeedSlider) ? 0xAA00E5FF : 0x5555FF88;
            context.fill(sliderX - 1, sliderY - 1, sliderX + sliderW + 1, sliderY, trackBorder);
            context.fill(sliderX - 1, sliderY + sliderH, sliderX + sliderW + 1, sliderY + sliderH + 1, trackBorder);
            context.fill(sliderX - 1, sliderY, sliderX, sliderY + sliderH, trackBorder);
            context.fill(sliderX + sliderW, sliderY, sliderX + sliderW + 1, sliderY + sliderH, trackBorder);

            // Slider Knob
            int knobX = sliderX + fillW;
            context.fill(knobX - 2, sliderY - 1, knobX + 2, sliderY + sliderH + 1, 0xFFFFFFFF);
            context.fill(knobX - 1, sliderY, knobX + 1, sliderY + sliderH, 0xFF00E5FF);

            // Footnote
            context.centeredText(client.font, Component.literal("Walk / Swim"), cardX + cardW / 2, cardY + 45, 0xFF556677);
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
            int tabY = y + 14 + i * (tabHeight + tabSpacing);
            if (mouseX >= tabX && mouseX <= tabX + sidebarWidth && mouseY >= tabY && mouseY <= tabY + tabHeight) {
                selectTab(i);
                return true;
            }
        }

        // Check for Speed Slider click
        Minecraft client = Minecraft.getInstance();
        if (client.player instanceof PlayerStatsAccessor accessor) {
            int sliderX = getSliderX();
            int sliderY = getSliderY();
            int sliderW = getSliderW();
            int sliderH = getSliderH();
            if (mouseX >= sliderX - 3 && mouseX <= sliderX + sliderW + 3 && mouseY >= sliderY - 3 && mouseY <= sliderY + sliderH + 3) {
                this.isDraggingSpeedSlider = true;
                updateSpeedFromMouse(mouseX, accessor);
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
        if (this.isDraggingSpeedSlider) {
            Minecraft client = Minecraft.getInstance();
            if (client.player instanceof PlayerStatsAccessor accessor) {
                updateSpeedFromMouse(event.x(), accessor);
            }
            return true;
        }
        if (tabs.get(activeTab).mouseDragged(event, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.isDraggingSpeedSlider) {
            this.isDraggingSpeedSlider = false;
            return true;
        }
        if (tabs.get(activeTab).mouseReleased(event)) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int cardX = getSpeedCardX();
        int cardY = getSpeedCardY();
        int cardW = getSpeedCardW();
        int cardH = getSpeedCardH();
        if (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= cardY && mouseY <= cardY + cardH) {
            Minecraft client = Minecraft.getInstance();
            if (client.player instanceof PlayerStatsAccessor accessor) {
                int current = accessor.dba$getSpeedPercent();
                int step = verticalAmount > 0 ? 5 : -5;
                int newPct = Math.max(1, Math.min(100, current + step));
                if (newPct != current) {
                    accessor.dba$setSpeedPercent(newPct);
                    sendSpeedPercent(newPct);
                }
                return true;
            }
        }

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
