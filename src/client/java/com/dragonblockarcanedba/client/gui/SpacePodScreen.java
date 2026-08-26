package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.network.SpacePodLaunchPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class SpacePodScreen extends Screen {

    private final int panelWidth = 340;
    private final int panelHeight = 290;
    private int startX;
    private int startY;

    private record Destination(String id, String icon, String name, String details, int accentColor) {}

    private final Destination[] destinations = new Destination[]{
        new Destination("namek", "🪐", "Planet Namek", "1.0G Gravity • Lush Namekian Homeland • Rich in Ki", 0xFF55FF88),
        new Destination("vegeta", "🪐", "Planet Vegeta", "1.5G Heavy Gravity • Saiyan Cradle • Extreme Combat", 0xFFFF8855),
        new Destination("yardrat", "🪐", "Planet Yardrat", "0.8G Light Gravity • Spirit Control Sanctum", 0xFF00FFCC),
        new Destination("otherworld", "🌌", "The Otherworld", "Spiritual Realm • King Kai's Road & Celestial Training", 0xFFB388FF),
        new Destination("overworld", "🌍", "Return to Earth", "1.0G Standard Gravity • Safe Overworld Haven", 0xFF55FF88)
    };

    public SpacePodScreen() {
        super(Component.literal("Space Pod — Select Destination"));
    }

    @Override
    protected void init() {
        this.startX = (this.width - panelWidth) / 2;
        this.startY = (this.height - panelHeight) / 2;
        this.clearWidgets();
    }

    private void launchTo(String destination) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.8f, 1.3f);
        }
        ClientPlayNetworking.send(new SpacePodLaunchPayload(destination));
        this.onClose();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        int cardX = startX + 15;
        int cardW = panelWidth - 30;
        int cardH = 34;

        // Check 5 planetary destination cards
        for (int i = 0; i < destinations.length; i++) {
            int cardY = startY + 48 + i * 38;
            if (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= cardY && mouseY <= cardY + cardH) {
                launchTo(destinations[i].id);
                return true;
            }
        }

        // Check Abort / Cancel Card
        int cancelY = startY + 248;
        int cancelH = 26;
        if (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= cancelY && mouseY <= cancelY + cancelH) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.8f, 0.9f);
            }
            this.onClose();
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Darkened background vignette
        context.fill(0, 0, this.width, this.height, 0x99000000);

        int bgColor = 0xEE0B1017;       // Capsule Corp Navy glassmorphism
        int borderColor = 0xFF00FFCC;   // Hologram Cyan
        int innerBorder = 0x4438EF7D;   // Sci-Fi Emerald

        // Main Panel Box
        context.fill(startX, startY, startX + panelWidth, startY + panelHeight, bgColor);
        
        // Neon Cyan Outer Borders
        context.fill(startX, startY, startX + panelWidth, startY + 2, borderColor);
        context.fill(startX, startY + panelHeight - 2, startX + panelWidth, startY + panelHeight, borderColor);
        context.fill(startX, startY + 2, startX + 2, startY + panelHeight - 2, borderColor);
        context.fill(startX + panelWidth - 2, startY + 2, startX + panelWidth, startY + panelHeight - 2, borderColor);

        // Subtle Inner Accent Frame
        context.fill(startX + 4, startY + 4, startX + panelWidth - 4, startY + 5, innerBorder);
        context.fill(startX + 4, startY + panelHeight - 5, startX + panelWidth - 4, startY + panelHeight - 4, innerBorder);
        context.fill(startX + 4, startY + 4, startX + 5, startY + panelHeight - 4, innerBorder);
        context.fill(startX + panelWidth - 5, startY + 4, startX + panelWidth - 4, startY + panelHeight - 4, innerBorder);

        // Header Background Banner
        context.fill(startX + 5, startY + 5, startX + panelWidth - 5, startY + 42, 0x66000000);
        context.fill(startX + 5, startY + 41, startX + panelWidth - 5, startY + 42, 0x4400FFCC);

        // Header Text
        context.centeredText(this.font, Component.literal("\uD83D\uDE80 CAPSULE CORP NAV-SYSTEM \uD83D\uDE80"), this.width / 2, startY + 12, 0xFF00FFCC);
        context.centeredText(this.font, Component.literal("Select planetary destination for hyperspace lock:"), this.width / 2, startY + 26, 0xFFAAAAAA);

        int cardX = startX + 15;
        int cardW = panelWidth - 30;
        int cardH = 34;

        // Render 5 Planetary Cards
        for (int i = 0; i < destinations.length; i++) {
            Destination dest = destinations[i];
            int cardY = startY + 48 + i * 38;

            boolean isHovered = (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= cardY && mouseY <= cardY + cardH);

            int cardBg = isHovered ? 0x66003333 : 0x44101824;
            int cardBorder = isHovered ? dest.accentColor : 0x4400FFCC;

            // Card Background
            context.fill(cardX, cardY, cardX + cardW, cardY + cardH, cardBg);

            // Card Borders
            context.fill(cardX, cardY, cardX + cardW, cardY + 1, cardBorder);
            context.fill(cardX, cardY + cardH - 1, cardX + cardW, cardY + cardH, cardBorder);
            context.fill(cardX, cardY, cardX + 1, cardY + cardH, cardBorder);
            context.fill(cardX + cardW - 1, cardY, cardX + cardW, cardY + cardH, cardBorder);

            // Left Neon Accent Pill
            int pillColor = isHovered ? dest.accentColor : (0x88000000 | (dest.accentColor & 0x00FFFFFF));
            context.fill(cardX, cardY, cardX + 4, cardY + cardH, pillColor);

            // Title & Subtitle
            int titleColor = isHovered ? 0xFFFFFFFF : dest.accentColor;
            context.text(this.font, dest.icon + "  " + dest.name, cardX + 12, cardY + 6, titleColor, false);
            context.text(this.font, dest.details, cardX + 12, cardY + 19, isHovered ? 0xFFE0E0E0 : 0xFF888888, false);

            // Right Chevron / Action Arrow
            if (isHovered) {
                context.text(this.font, "\u27A4", cardX + cardW - 18, cardY + 13, dest.accentColor, false);
            }
        }

        // Render Abort / Cancel Card
        int cancelY = startY + 248;
        int cancelH = 26;
        boolean isCancelHovered = (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= cancelY && mouseY <= cancelY + cancelH);

        int cancelBg = isCancelHovered ? 0x44442222 : 0x33111111;
        int cancelBorder = isCancelHovered ? 0xFFFF6666 : 0x44666666;

        context.fill(cardX, cancelY, cardX + cardW, cancelY + cancelH, cancelBg);
        context.fill(cardX, cancelY, cardX + cardW, cancelY + 1, cancelBorder);
        context.fill(cardX, cancelY + cancelH - 1, cardX + cardW, cancelY + cancelH, cancelBorder);
        context.fill(cardX, cancelY, cardX + 1, cancelY + cancelH, cancelBorder);
        context.fill(cardX + cardW - 1, cancelY, cardX + cardW, cancelY + cancelH, cancelBorder);

        int cancelTextColor = isCancelHovered ? 0xFFFF8888 : 0xFFAAAAAA;
        context.centeredText(this.font, Component.literal("\u2715  Abort Launch / Cancel"), cardX + cardW / 2, cancelY + 8, cancelTextColor);

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
