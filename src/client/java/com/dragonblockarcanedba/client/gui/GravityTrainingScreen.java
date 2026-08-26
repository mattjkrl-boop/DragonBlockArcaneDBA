package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.inventory.GravityTrainingMenu;
import com.dragonblockarcanedba.network.C2SSetGravityPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
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
        this.clearWidgets();
    }

    private void sendGravityUpdate(int change) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.7f, change > 0 ? 1.3f : 0.9f);
        }
        int newGravity = Math.max(0, Math.min(1000, this.menu.getGravity() + change));
        ClientPlayNetworking.send(new C2SSetGravityPayload(newGravity));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Check custom stepper buttons
        if (mouseY >= y + 19 && mouseY <= y + 33) {
            if (mouseX >= x + 10 && mouseX <= x + 46) {
                sendGravityUpdate(-100);
                return true;
            } else if (mouseX >= x + 49 && mouseX <= x + 81) {
                sendGravityUpdate(-10);
                return true;
            } else if (mouseX >= x + 95 && mouseX <= x + 127) {
                sendGravityUpdate(10);
                return true;
            } else if (mouseX >= x + 130 && mouseX <= x + 166) {
                sendGravityUpdate(100);
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        int bgColor = 0xEE11151A;       // Dark sleek glass
        int borderColor = 0xFF00FFCC;   // Cyan neon border
        int innerBorder = 0x4438EF7D;   // Emerald accent line
        int slotBgColor = 0x880A0C0E;
        int slotBorderColor = 0x6600FFCC;

        // 1. Render Main GUI Panel Background
        context.fill(x, y, x + imageWidth, y + imageHeight, bgColor);

        // 2. Render Sci-Fi Neon Outer Borders
        context.fill(x, y, x + imageWidth, y + 2, borderColor);
        context.fill(x, y + imageHeight - 2, x + imageWidth, y + imageHeight, borderColor);
        context.fill(x, y, x + 2, y + imageHeight, borderColor);
        context.fill(x + imageWidth - 2, y, x + imageWidth, y + imageHeight, borderColor);

        // Subtle Inner Accent Frame
        context.fill(x + 3, y + 3, x + imageWidth - 3, y + 4, innerBorder);
        context.fill(x + 3, y + imageHeight - 4, x + imageWidth - 3, y + imageHeight - 3, innerBorder);
        context.fill(x + 3, y + 3, x + 4, y + imageHeight - 3, innerBorder);
        context.fill(x + imageWidth - 4, y + 3, x + imageWidth - 3, y + imageHeight - 3, innerBorder);

        // 3. Header Title Bar
        context.fill(x + 4, y + 4, x + imageWidth - 4, y + 17, 0x66000000);
        context.fill(x + 4, y + 16, x + imageWidth - 4, y + 17, 0x4400FFCC);
        context.centeredText(this.font, Component.literal("\u26A1 GRAVITY CHAMBER \u26A1"), x + imageWidth / 2, y + 6, 0xFF00FFCC);

        // 4. Render 4 Custom Stepper Buttons
        renderStepperButton(context, "-100", x + 10, y + 19, 36, 14, mouseX, mouseY, 0xFFFF6666);
        renderStepperButton(context, "-10", x + 49, y + 19, 32, 14, mouseX, mouseY, 0xFFFFAA55);
        renderStepperButton(context, "+10", x + 95, y + 19, 32, 14, mouseX, mouseY, 0xFF55FF88);
        renderStepperButton(context, "+100", x + 130, y + 19, 36, 14, mouseX, mouseY, 0xFF00FFCC);

        // 5. Gravity Status & Intensity Gauge
        int currentGrav = this.menu.getGravity();
        int gravColor = currentGrav >= 300 ? 0xFFFF4444 : (currentGrav >= 100 ? 0xFFFFAA00 : (currentGrav > 10 ? 0xFF00FFCC : 0xFF55FF88));
        String gravText = "Intensity: " + currentGrav + "G";
        context.centeredText(this.font, Component.literal(gravText), x + imageWidth / 2, y + 36, gravColor);

        // Intensity Progress Bar (0 to 500G+)
        int barX = x + 24;
        int barY = y + 46;
        int barW = imageWidth - 48;
        int barH = 3;
        context.fill(barX, barY, barX + barW, barY + barH, 0x55000000);
        float fillFrac = Math.min(1.0f, currentGrav / 500.0f);
        int fillW = Math.round(barW * fillFrac);
        if (fillW > 0) {
            context.fill(barX, barY, barX + fillW, barY + barH, gravColor);
        }

        // 6. Draw Fuel Slot Background (Slot 0 at x+80, y+53)
        int fuelSlotX = x + 79;
        int fuelSlotY = y + 52;
        int fuel = this.menu.getFuel();
        int fuelBorder = fuel > 0 ? 0xFF55FF88 : 0xFF666666;
        context.fill(fuelSlotX, fuelSlotY, fuelSlotX + 18, fuelSlotY + 18, slotBgColor);
        drawSlotBorder(context, fuelSlotX, fuelSlotY, 18, 18, fuelBorder);

        // Fuel Label
        String fuelText = fuel > 0 ? (fuel + "t") : "No Fuel";
        int fuelTextColor = fuel > 0 ? 0xFF55FF88 : 0xFF888888;
        context.text(this.font, "Fuel: " + fuelText, x + 102, y + 57, fuelTextColor, false);

        // 7. Draw Player Inventory Slots (3x9 grid starting at x+8, y+84)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = x + 7 + col * 18;
                int sy = y + 83 + row * 18;
                context.fill(sx, sy, sx + 18, sy + 18, slotBgColor);
                drawSlotBorder(context, sx, sy, 18, 18, slotBorderColor);
            }
        }

        // 8. Draw Player Hotbar Slots (1x9 grid starting at x+8, y+142)
        for (int col = 0; col < 9; col++) {
            int sx = x + 7 + col * 18;
            int sy = y + 141;
            context.fill(sx, sy, sx + 18, sy + 18, slotBgColor);
            drawSlotBorder(context, sx, sy, 18, 18, slotBorderColor);
        }

        // 9. Inventory Label Above Player Slots
        context.text(this.font, "Inventory", x + 8, y + 74, 0xFFAAAAAA, false);

        // 10. Render Super (Items & Cursor) On Top
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    private void renderStepperButton(GuiGraphicsExtractor context, String text, int bx, int by, int bw, int bh, int mouseX, int mouseY, int accent) {
        boolean isHovered = (mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh);
        int bg = isHovered ? 0x66003333 : 0x44001122;
        int border = isHovered ? accent : 0x4400FFCC;

        context.fill(bx, by, bx + bw, by + bh, bg);
        context.fill(bx, by, bx + bw, by + 1, border);
        context.fill(bx, by + bh - 1, bx + bw, by + bh, border);
        context.fill(bx, by, bx + 1, by + bh, border);
        context.fill(bx + bw - 1, by, bx + bw, by + bh, border);

        int textCol = isHovered ? 0xFFFFFFFF : accent;
        context.centeredText(this.font, Component.literal(text), bx + bw / 2, by + 3, textCol);
    }

    private void drawSlotBorder(GuiGraphicsExtractor context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + 1, color);
        context.fill(x, y + h - 1, x + w, y + h, color);
        context.fill(x, y, x + 1, y + h, color);
        context.fill(x + w - 1, y, x + w, y + h, color);
    }
}
