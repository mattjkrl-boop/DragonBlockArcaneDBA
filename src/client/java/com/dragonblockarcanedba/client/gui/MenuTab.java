package com.dragonblockarcanedba.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

public interface MenuTab {
    void init(DbaMenuScreen screen);
    void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta);
    boolean mouseClicked(MouseButtonEvent event, boolean isRepeat);
}
