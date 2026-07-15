package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.network.ActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class ReviveScreen extends Screen {

    public ReviveScreen() {
        super(Component.literal("Otherworld Guide"));
    }

    @Override
    protected void init() {
        super.init();
        int btnWidth = 120;
        int btnHeight = 20;

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.addRenderableWidget(Button.builder(Component.literal("REVIVE"), btn -> {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("action", "revive");
            ClientPlayNetworking.send(new ActionPayload(nbt));
            this.onClose();
        }).bounds(centerX - btnWidth - 10, centerY, btnWidth, btnHeight).build());

        this.addRenderableWidget(Button.builder(Component.literal("STAY IN HELL"), btn -> {
            this.onClose();
        }).bounds(centerX + 10, centerY, btnWidth, btnHeight).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        
        // Draw techy background for the panel
        int panelWidth = 300;
        int panelHeight = 120;
        int startX = (this.width - panelWidth) / 2;
        int startY = (this.height - panelHeight) / 2;

        int bgColor = 0xDD1E2024;
        int borderColor = 0xFFFF7700; // Orange theme for otherworld

        // Panel Background
        context.fill(startX, startY, startX + panelWidth, startY + panelHeight, bgColor);
        
        // Neon Borders
        context.fill(startX, startY, startX + panelWidth, startY + 2, borderColor);
        context.fill(startX, startY + panelHeight - 2, startX + panelWidth, startY + panelHeight, borderColor);
        context.fill(startX, startY, startX + 2, startY + panelHeight, borderColor);
        context.fill(startX + panelWidth - 2, startY, startX + panelWidth, startY + panelHeight, borderColor);
        
        context.centeredText(this.font, Component.literal("YOUR JOURNEY IS NOT OVER YET."), this.width / 2, startY + 20, 0xFFFFFFFF);
        context.centeredText(this.font, Component.literal("Do you wish to return to the living world?"), this.width / 2, startY + 35, 0xFFAAAAAA);
        
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }

}
