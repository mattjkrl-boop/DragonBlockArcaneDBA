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
        int spacing = 28;

        // Planet Namek
        addRenderableWidget(Button.builder(
            Component.literal("\u2605 Planet Namek"),
            btn -> launchTo("namek")
        ).bounds(centerX - buttonWidth / 2, centerY - spacing * 2, buttonWidth, buttonHeight).build());

        // Planet Vegeta
        addRenderableWidget(Button.builder(
            Component.literal("\u2605 Planet Vegeta"),
            btn -> launchTo("vegeta")
        ).bounds(centerX - buttonWidth / 2, centerY - spacing, buttonWidth, buttonHeight).build());

        // Planet Yardrat
        addRenderableWidget(Button.builder(
            Component.literal("\u2605 Planet Yardrat"),
            btn -> launchTo("yardrat")
        ).bounds(centerX - buttonWidth / 2, centerY, buttonWidth, buttonHeight).build());

        // Return to Overworld
        addRenderableWidget(Button.builder(
            Component.literal("\u2190 Return to Earth"),
            btn -> launchTo("overworld")
        ).bounds(centerX - buttonWidth / 2, centerY + spacing + 10, buttonWidth, buttonHeight).build());

        // Cancel
        addRenderableWidget(Button.builder(
            Component.literal("Cancel"),
            btn -> onClose()
        ).bounds(centerX - buttonWidth / 2, centerY + spacing * 2 + 20, buttonWidth, buttonHeight).build());
    }

    private void launchTo(String destination) {
        ClientPlayNetworking.send(new SpacePodLaunchPayload(destination));
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        // Draw a dark panel behind the buttons
        int panelX = this.width / 2 - 120;
        int panelY = this.height / 2 - 80;
        int panelW = 240;
        int panelH = 200;
        net.minecraft.resources.Identifier bgTex = net.minecraft.resources.Identifier.fromNamespaceAndPath("dragonblockarcanedba", "textures/gui/menu_background.png");
        context.blit(bgTex, panelX, panelY, panelW, panelH, 0.0F, 0.0F, (float)panelW, (float)panelH);

        // Cyan neon border
        int borderColor = 0xFF00DDFF;
        context.fill(panelX, panelY, panelX + panelW, panelY + 1, borderColor);
        context.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, borderColor);
        context.fill(panelX, panelY, panelX + 1, panelY + panelH, borderColor);
        context.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, borderColor);

        // Title text
        context.centeredText(this.font, this.title, this.width / 2, panelY + 8, 0xFF00DDFF);
        // Subtitle
        context.centeredText(this.font, Component.literal("Choose your destination:"), this.width / 2, panelY + 22, 0xFFAAAAAA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
