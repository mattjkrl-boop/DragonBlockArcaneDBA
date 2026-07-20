package com.dragonblockarcanedba.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

public interface MenuTab {
    void init(DbaMenuScreen screen);
    void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta);
    boolean mouseClicked(MouseButtonEvent event, boolean isRepeat);
    default boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) { return false; }
    default boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) { return false; }
}
