package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.network.ActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class ReviveScreen extends Screen {

    private final int panelWidth = 320;
    private final int panelHeight = 180;
    private int startX;
    private int startY;

    public ReviveScreen() {
        super(Component.literal("Otherworld Guide"));
    }

    @Override
    protected void init() {
        this.startX = (this.width - panelWidth) / 2;
        this.startY = (this.height - panelHeight) / 2;
        this.clearWidgets();
    }

    private void revivePlayer() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.4f);
            this.minecraft.player.playSound(SoundEvents.BEACON_ACTIVATE, 1.0f, 1.6f);
        }
        CompoundTag nbt = new CompoundTag();
        nbt.putString("action", "revive");
        ClientPlayNetworking.send(new ActionPayload(nbt));
        this.onClose();
    }

    private void stayInHell() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.8f, 0.9f);
        }
        this.onClose();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        int cardX = startX + 15;
        int cardW = panelWidth - 30;

        // Revive Card
        int reviveY = startY + 52;
        int cardH = 50;
        if (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= reviveY && mouseY <= reviveY + cardH) {
            revivePlayer();
            return true;
        }

        // Stay Card
        int stayY = startY + 110;
        if (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= stayY && mouseY <= stayY + cardH) {
            stayInHell();
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Darkened background vignette
        context.fill(0, 0, this.width, this.height, 0x99000000);

        int bgColor = 0xEE0D1117;       // Celestial dark slate glass
        int borderColor = 0xFF00FFCC;   // Otherworld Cyan
        int innerBorder = 0x44B388FF;   // Spiritual Lavender

        // Main Panel Box
        context.fill(startX, startY, startX + panelWidth, startY + panelHeight, bgColor);
        
        // Neon Cyan Outer Borders
        context.fill(startX, startY, startX + panelWidth, startY + 2, borderColor);
        context.fill(startX, startY + panelHeight - 2, startX + panelWidth, startY + panelHeight, borderColor);
        context.fill(startX, startY, startX + 2, startY + panelHeight, borderColor);
        context.fill(startX + panelWidth - 2, startY, startX + panelWidth, startY + panelHeight, borderColor);

        // Subtle Inner Lavender Accent Frame
        context.fill(startX + 4, startY + 4, startX + panelWidth - 4, startY + 5, innerBorder);
        context.fill(startX + 4, startY + panelHeight - 5, startX + panelWidth - 4, startY + panelHeight - 4, innerBorder);
        context.fill(startX + 4, startY + 4, startX + 5, startY + panelHeight - 4, innerBorder);
        context.fill(startX + panelWidth - 5, startY + 4, startX + panelWidth - 4, startY + panelHeight - 4, innerBorder);

        // Header Background Banner
        context.fill(startX + 5, startY + 5, startX + panelWidth - 5, startY + 42, 0x66000000);
        context.fill(startX + 5, startY + 41, startX + panelWidth - 5, startY + 42, 0x4400FFCC);

        // Header Text
        context.centeredText(this.font, Component.literal("\u2728 THE GATES OF OTHERWORLD \u2728"), this.width / 2, startY + 12, 0xFF00FFCC);
        context.centeredText(this.font, Component.literal("Your earthly vessel has fallen. Choose your path:"), this.width / 2, startY + 26, 0xFFAAAAAA);

        int cardX = startX + 15;
        int cardW = panelWidth - 30;
        int cardH = 50;

        // 1. REVIVE CARD
        int reviveY = startY + 52;
        boolean isReviveHovered = (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= reviveY && mouseY <= reviveY + cardH);

        int reviveBg = isReviveHovered ? 0x66003333 : 0x44002222;
        int reviveBorder = isReviveHovered ? 0xFF00FFCC : 0x4400FFCC;

        context.fill(cardX, reviveY, cardX + cardW, reviveY + cardH, reviveBg);
        context.fill(cardX, reviveY, cardX + cardW, reviveY + 1, reviveBorder);
        context.fill(cardX, reviveY + cardH - 1, cardX + cardW, reviveY + cardH, reviveBorder);
        context.fill(cardX, reviveY, cardX + 1, reviveY + cardH, reviveBorder);
        context.fill(cardX + cardW - 1, reviveY, cardX + cardW, reviveY + cardH, reviveBorder);

        // Left Neon Accent Pill
        context.fill(cardX, reviveY, cardX + 4, reviveY + cardH, 0xFF00FFCC);

        context.text(this.font, "\uD83C\uDF1F  Return to the Living World (Revive)", cardX + 12, reviveY + 10, isReviveHovered ? 0xFFFFFFFF : 0xFF00FFCC, false);
        context.text(this.font, "Reclaim your physical form and resurrect on Earth.", cardX + 12, reviveY + 28, isReviveHovered ? 0xFFE0E0E0 : 0xFF888888, false);

        if (isReviveHovered) {
            context.text(this.font, "\u27A4", cardX + cardW - 18, reviveY + 20, 0xFF00FFCC, false);
        }

        // 2. STAY CARD
        int stayY = startY + 110;
        boolean isStayHovered = (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= stayY && mouseY <= stayY + cardH);

        int stayBg = isStayHovered ? 0x66332244 : 0x44221133;
        int stayBorder = isStayHovered ? 0xFFB388FF : 0x44B388FF;

        context.fill(cardX, stayY, cardX + cardW, stayY + cardH, stayBg);
        context.fill(cardX, stayY, cardX + cardW, stayY + 1, stayBorder);
        context.fill(cardX, stayY + cardH - 1, cardX + cardW, stayY + cardH, stayBorder);
        context.fill(cardX, stayY, cardX + 1, stayY + cardH, stayBorder);
        context.fill(cardX + cardW - 1, stayY, cardX + cardW, stayY + cardH, stayBorder);

        // Left Neon Accent Pill
        context.fill(cardX, stayY, cardX + 4, stayY + cardH, 0xFFB388FF);

        context.text(this.font, "\uD83D\uDC80  Remain in the Spirit Realm", cardX + 12, stayY + 10, isStayHovered ? 0xFFFFFFFF : 0xFFB388FF, false);
        context.text(this.font, "Stay in the afterlife to explore and meditate in Otherworld.", cardX + 12, stayY + 28, isStayHovered ? 0xFFE0E0E0 : 0xFF888888, false);

        if (isStayHovered) {
            context.text(this.font, "\u27A4", cardX + cardW - 18, stayY + 20, 0xFFB388FF, false);
        }

        super.extractRenderState(context, mouseX, mouseY, delta);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
