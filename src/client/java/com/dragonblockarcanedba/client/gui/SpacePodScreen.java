package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.network.SpacePodLaunchPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Client-side destination picker screen for the Space Pod.
 * Displays buttons for Planet Namek, Planet Vegeta, Planet Yardrat, and Return to Earth.
 */
public class SpacePodScreen extends Screen {
    public SpacePodScreen() {
        super(Component.literal("Space Pod — Select Destination"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 26;
        
        int startY = centerY - 80;

        // Planet Namek
        addRenderableWidget(Button.builder(
            Component.literal("\u2605 Planet Namek"),
            btn -> launchTo("namek")
        ).bounds(centerX - buttonWidth / 2, startY, buttonWidth, buttonHeight).build());

        // Planet Vegeta
        addRenderableWidget(Button.builder(
            Component.literal("\u2605 Planet Vegeta"),
            btn -> launchTo("vegeta")
        ).bounds(centerX - buttonWidth / 2, startY + spacing, buttonWidth, buttonHeight).build());

        // Planet Yardrat
        addRenderableWidget(Button.builder(
            Component.literal("\u2605 Planet Yardrat"),
            btn -> launchTo("yardrat")
        ).bounds(centerX - buttonWidth / 2, startY + spacing * 2, buttonWidth, buttonHeight).build());
        
        // Otherworld
        addRenderableWidget(Button.builder(
            Component.literal("\u2605 The Otherworld"),
            btn -> launchTo("otherworld")
        ).bounds(centerX - buttonWidth / 2, startY + spacing * 3, buttonWidth, buttonHeight).build());

        // Return to Overworld
        addRenderableWidget(Button.builder(
            Component.literal("\u2190 Return to Earth"),
            btn -> launchTo("overworld")
        ).bounds(centerX - buttonWidth / 2, startY + spacing * 4 + 10, buttonWidth, buttonHeight).build());

        // Cancel
        addRenderableWidget(Button.builder(
            Component.literal("Cancel"),
            btn -> onClose()
        ).bounds(centerX - buttonWidth / 2, startY + spacing * 5 + 10, buttonWidth, buttonHeight).build());
    }

    private void launchTo(String destination) {
        ClientPlayNetworking.send(new SpacePodLaunchPayload(destination));
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int panelWidth = 240;
        int panelHeight = 220;
        int startX = (this.width - panelWidth) / 2;
        int startY = (this.height - panelHeight) / 2;

        int bgColor = 0xF0101216;
        int borderColor = 0xFF00FFCC; // Cyan theme

        // Panel Background
        context.fill(startX, startY, startX + panelWidth, startY + panelHeight, bgColor);
        
        // Neon Borders
        context.fill(startX, startY, startX + panelWidth, startY + 2, borderColor);
        context.fill(startX, startY + panelHeight - 2, startX + panelWidth, startY + panelHeight, borderColor);
        context.fill(startX, startY, startX + 2, startY + panelHeight, borderColor);
        context.fill(startX + panelWidth - 2, startY, startX + panelWidth, startY + panelHeight, borderColor);
        
        // Title text
        context.centeredText(this.font, this.title, this.width / 2, startY + 10, 0xFF00FFCC);
        // Subtitle
        context.centeredText(this.font, Component.literal("Choose your destination:"), this.width / 2, startY + 22, 0xFFAAAAAA);

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
