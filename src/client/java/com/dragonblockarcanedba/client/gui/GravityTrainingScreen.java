package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.inventory.GravityTrainingMenu;
import com.dragonblockarcanedba.network.C2SSetGravityPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GravityTrainingScreen extends AbstractContainerScreen<GravityTrainingMenu> {

    public GravityTrainingScreen(GravityTrainingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        
        // Hide default title/inventory text to draw custom DBA styled text
        this.titleLabelY = -1000;
        this.inventoryLabelY = -1000;

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Gravity adjustment buttons in a clean row above fuel slot
        this.addRenderableWidget(Button.builder(Component.literal("-100"), button -> {
            sendGravityUpdate(-100);
        }).bounds(x + 10, y + 18, 36, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("-10"), button -> {
            sendGravityUpdate(-10);
        }).bounds(x + 48, y + 18, 32, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("+10"), button -> {
            sendGravityUpdate(10);
        }).bounds(x + 96, y + 18, 32, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("+100"), button -> {
            sendGravityUpdate(100);
        }).bounds(x + 130, y + 18, 36, 18).build());
    }

    private void sendGravityUpdate(int change) {
        int newGravity = Math.max(0, Math.min(1000, this.menu.getGravity() + change));
        ClientPlayNetworking.send(new C2SSetGravityPayload(newGravity));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        int bgColor = 0xDD151824; // Lighter, transparent glassmorphic background
        int borderColor = 0xFF00FFCC; // Cyan neon border theme
        int slotBgColor = 0x880A0C0E;
        int slotBorderColor = 0x6600FFCC; // Cool cyan outline

        // 1. Render Main GUI Panel Background
        context.fill(x, y, x + imageWidth, y + imageHeight, bgColor);

        // 2. Render Sci-Fi Neon Outer Borders
        context.fill(x, y, x + imageWidth, y + 2, borderColor);
        context.fill(x, y + imageHeight - 2, x + imageWidth, y + imageHeight, borderColor);
        context.fill(x, y, x + 2, y + imageHeight, borderColor);
        context.fill(x + imageWidth - 2, y, x + imageWidth, y + imageHeight, borderColor);

        // 3. Header Title Bar
        context.fill(x + 2, y + 2, x + imageWidth - 2, y + 15, 0xFF0A0C0E);
        context.centeredText(this.font, Component.literal("GRAVITY CHAMBER"), x + imageWidth / 2, y + 4, 0xFF00FFCC);

        // 4. Status Displays (Gravity & Fuel)
        String gravText = "Gravity: " + this.menu.getGravity() + "G";
        context.centeredText(this.font, Component.literal(gravText), x + imageWidth / 2, y + 38, 0xFFFF5555);

        // 5. Draw Fuel Slot Background (Slot 0 at x+80, y+53)
        int fuelSlotX = x + 79;
        int fuelSlotY = y + 52;
        context.fill(fuelSlotX, fuelSlotY, fuelSlotX + 18, fuelSlotY + 18, slotBgColor);
        drawSlotBorder(context, fuelSlotX, fuelSlotY, 18, 18, 0xFF00FFCC);

        // Label beside fuel slot
        context.text(this.font, "Fuel: " + this.menu.getFuel() + "t", x + 104, y + 57, 0xFF55FF55, false);

        // 6. Draw Player Inventory Slots (3x9 grid starting at x+8, y+84)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = x + 7 + col * 18;
                int sy = y + 83 + row * 18;
                context.fill(sx, sy, sx + 18, sy + 18, slotBgColor);
                drawSlotBorder(context, sx, sy, 18, 18, slotBorderColor);
            }
        }

        // 7. Draw Player Hotbar Slots (1x9 grid starting at x+8, y+142)
        for (int col = 0; col < 9; col++) {
            int sx = x + 7 + col * 18;
            int sy = y + 141;
            context.fill(sx, sy, sx + 18, sy + 18, slotBgColor);
            drawSlotBorder(context, sx, sy, 18, 18, slotBorderColor);
        }

        // 8. Inventory Label Above Player Slots
        context.text(this.font, "Inventory", x + 8, y + 74, 0xFFAAAAAA, false);

        // 9. Render Super (Buttons, Items & Cursor) On Top
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    private void drawSlotBorder(GuiGraphicsExtractor context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + 1, color);
        context.fill(x, y + h - 1, x + w, y + h, color);
        context.fill(x, y, x + 1, y + h, color);
        context.fill(x + w - 1, y, x + w, y + h, color);
    }
}
