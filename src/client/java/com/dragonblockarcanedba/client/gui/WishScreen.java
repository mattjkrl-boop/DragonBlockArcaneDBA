package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.network.C2SMakeWishPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class WishScreen extends Screen {
    private final int shenronId;

    public WishScreen(int shenronId) {
        super(Component.literal("Summon Shenron"));
        this.shenronId = shenronId;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 26;
        int startY = centerY - 30;

        // Wealth Button
        addRenderableWidget(Button.builder(
            Component.literal("I wish for Wealth!"),
            btn -> makeWish("wealth")
        ).bounds(centerX - buttonWidth / 2, startY, buttonWidth, buttonHeight).build());

        // Power Button
        addRenderableWidget(Button.builder(
            Component.literal("I wish for Power!"),
            btn -> makeWish("power")
        ).bounds(centerX - buttonWidth / 2, startY + spacing, buttonWidth, buttonHeight).build());

        // Immortality Button
        addRenderableWidget(Button.builder(
            Component.literal("I wish for Immortality!"),
            btn -> makeWish("immortality")
        ).bounds(centerX - buttonWidth / 2, startY + spacing * 2, buttonWidth, buttonHeight).build());

        // Close Button
        addRenderableWidget(Button.builder(
            Component.literal("Nevermind"),
            btn -> onClose()
        ).bounds(centerX - buttonWidth / 2, startY + spacing * 3 + 10, buttonWidth, buttonHeight).build());
    }

    private void makeWish(String wishType) {
        ClientPlayNetworking.send(new C2SMakeWishPayload(this.shenronId, wishType));
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int panelWidth = 260;
        int panelHeight = 180;
        int startX = (this.width - panelWidth) / 2;
        int startY = (this.height - panelHeight) / 2;

        int bgColor = 0xF50D110D; // Deep green/black glassmorphic panel
        int borderColor = 0xFFFFD700; // Shiny gold border theme

        // Panel Background
        context.fill(startX, startY, startX + panelWidth, startY + panelHeight, bgColor);
        
        // Shiny Golden Borders
        context.fill(startX, startY, startX + panelWidth, startY + 2, borderColor);
        context.fill(startX, startY + panelHeight - 2, startX + panelWidth, startY + panelHeight, borderColor);
        context.fill(startX, startY, startX + 2, startY + panelHeight, borderColor);
        context.fill(startX + panelWidth - 2, startY, startX + panelWidth, startY + panelHeight, borderColor);
        
        // Title text
        context.centeredText(this.font, Component.literal("SHENRON'S WISH MENU"), this.width / 2, startY + 12, 0xFFFFD700);
        // Subtitle
        context.centeredText(this.font, Component.literal("State your desire:"), this.width / 2, startY + 24, 0xFFAAAAAA);

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
