package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.network.C2SMakeWishPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class WishScreen extends Screen {
    private final int shenronId;

    private final int panelWidth = 320;
    private final int panelHeight = 230;
    private int startX;
    private int startY;

    // Wish Option Definition
    private record WishOption(String id, String icon, String title, String subtitle, int accentColor) {}

    private final WishOption[] options = new WishOption[]{
        new WishOption("wealth", "\uD83D\uDCB0", "I wish for Wealth!", "+64 Silver Zeni currency", 0xFFFFD700),
        new WishOption("power", "\u26A1", "I wish for Power!", "+150 Ability Stat Points", 0xFFFF5555),
        new WishOption("immortality", "\u2728", "I wish for Immortality!", "16 Senzu Beans & Celestial Grace (10m)", 0xFF55FF88)
    };

    public WishScreen(int shenronId) {
        super(Component.literal("Summon Shenron"));
        this.shenronId = shenronId;
    }

    @Override
    protected void init() {
        this.startX = (this.width - panelWidth) / 2;
        this.startY = (this.height - panelHeight) / 2;
        this.clearWidgets();
    }

    private void makeWish(String wishType) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.8f, 1.2f);
        }
        ClientPlayNetworking.send(new C2SMakeWishPayload(this.shenronId, wishType));
        this.onClose();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        int cardX = startX + 15;
        int cardW = panelWidth - 30;

        // Check 3 main wish options
        for (int i = 0; i < options.length; i++) {
            int cardY = startY + 50 + i * 42;
            int cardH = 36;
            if (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= cardY && mouseY <= cardY + cardH) {
                makeWish(options[i].id);
                return true;
            }
        }

        // Check Cancel card
        int cancelY = startY + 184;
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

        int bgColor = 0xEE0A120D;       // Deep jade green glassmorphism
        int borderColor = 0xFFFFD700;   // Shenron Divine Gold
        int innerBorder = 0x5533AA55;   // Subtle emerald inner line

        // Main Panel Box
        context.fill(startX, startY, startX + panelWidth, startY + panelHeight, bgColor);
        
        // Shiny Golden Outer Borders
        context.fill(startX, startY, startX + panelWidth, startY + 2, borderColor);
        context.fill(startX, startY + panelHeight - 2, startX + panelWidth, startY + panelHeight, borderColor);
        context.fill(startX, startY, startX + 2, startY + panelHeight, borderColor);
        context.fill(startX + panelWidth - 2, startY, startX + panelWidth, startY + panelHeight, borderColor);

        // Subtle Inner Emerald Accent Frame
        context.fill(startX + 4, startY + 4, startX + panelWidth - 4, startY + 5, innerBorder);
        context.fill(startX + 4, startY + panelHeight - 5, startX + panelWidth - 4, startY + panelHeight - 4, innerBorder);
        context.fill(startX + 4, startY + 4, startX + 5, startY + panelHeight - 4, innerBorder);
        context.fill(startX + panelWidth - 5, startY + 4, startX + panelWidth - 4, startY + panelHeight - 4, innerBorder);

        // Header Background Banner
        context.fill(startX + 5, startY + 5, startX + panelWidth - 5, startY + 42, 0x66000000);
        context.fill(startX + 5, startY + 41, startX + panelWidth - 5, startY + 42, 0x44FFD700);

        // Header Text
        context.centeredText(this.font, Component.literal("\u2728 SHENRON'S WISH SANCTUM \u2728"), this.width / 2, startY + 12, 0xFFFFD700);
        context.centeredText(this.font, Component.literal("Speak thy wish into the storm, mortal:"), this.width / 2, startY + 26, 0xFFAAAAAA);

        int cardX = startX + 15;
        int cardW = panelWidth - 30;

        // Render 3 Interactive Wish Option Cards
        for (int i = 0; i < options.length; i++) {
            WishOption opt = options[i];
            int cardY = startY + 50 + i * 42;
            int cardH = 36;

            boolean isHovered = (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= cardY && mouseY <= cardY + cardH);

            int cardBg = isHovered ? 0x66224422 : 0x44112211;
            int cardBorder = isHovered ? opt.accentColor : 0x44FFD700;

            // Card Background
            context.fill(cardX, cardY, cardX + cardW, cardY + cardH, cardBg);

            // Card Borders
            context.fill(cardX, cardY, cardX + cardW, cardY + 1, cardBorder);
            context.fill(cardX, cardY + cardH - 1, cardX + cardW, cardY + cardH, cardBorder);
            context.fill(cardX, cardY, cardX + 1, cardY + cardH, cardBorder);
            context.fill(cardX + cardW - 1, cardY, cardX + cardW, cardY + cardH, cardBorder);

            // Left Neon Accent Pill
            int pillColor = isHovered ? opt.accentColor : (0x88000000 | (opt.accentColor & 0x00FFFFFF));
            context.fill(cardX, cardY, cardX + 4, cardY + cardH, pillColor);

            // Title & Subtitle
            int titleColor = isHovered ? 0xFFFFFFFF : opt.accentColor;
            context.text(this.font, opt.icon + "  " + opt.title, cardX + 12, cardY + 7, titleColor, false);
            context.text(this.font, opt.subtitle, cardX + 12, cardY + 20, isHovered ? 0xFFE0E0E0 : 0xFF888888, false);

            // Right Chevron / Action Arrow
            if (isHovered) {
                context.text(this.font, "\u27A4", cardX + cardW - 18, cardY + 14, opt.accentColor, false);
            }
        }

        // Render Cancel Card
        int cancelY = startY + 184;
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
        context.centeredText(this.font, Component.literal("\u2715  Nevermind / Close"), cardX + cardW / 2, cancelY + 8, cancelTextColor);

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
