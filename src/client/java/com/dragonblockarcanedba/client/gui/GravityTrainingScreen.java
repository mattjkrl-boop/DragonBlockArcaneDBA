package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.inventory.GravityTrainingMenu;
import com.dragonblockarcanedba.network.C2SSetGravityPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Gravity steps flank the reading. The fuel slot and the player inventory stay on the menu grid.
 */
public class GravityTrainingScreen extends AbstractContainerScreen<GravityTrainingMenu> {
    private static final int[][] STEPS = {
            {8, 28, 44, -100},
            {8, 50, 44, -10},
            {144, 28, 44, 10},
            {144, 50, 44, 100}
    };

    public GravityTrainingScreen(GravityTrainingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, Component.literal("Gravity"), 196, 210);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelY = -1000;
        this.inventoryLabelY = -1000;
    }

    private void change(int amount) {
        int next = Math.max(0, Math.min(1000, this.menu.getGravity() + amount));
        ClientPlayNetworking.send(new C2SSetGravityPayload(next));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int left = (this.width - this.imageWidth) / 2;
            int top = (this.height - this.imageHeight) / 2;
            if (DbaUi.closeHit(left, top, this.imageWidth, event.x(), event.y())) {
                DbaUi.playClick();
                this.onClose();
                return true;
            }
            for (int[] step : STEPS) {
                if (DbaUi.hit(event.x(), event.y(), left + step[0], top + step[1], step[2], DbaUi.BUTTON_H)) {
                    DbaUi.playClick();
                    change(step[3]);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int left = (this.width - this.imageWidth) / 2;
        int top = (this.height - this.imageHeight) / 2;
        DbaUi.panel(context, left, top, this.imageWidth, this.imageHeight);
        DbaUi.title(context, this.font, left, top, DbaUi.closeX(left, this.imageWidth) - left - 14, "Gravity");
        DbaUi.closeControl(context, this.font, left, top, this.imageWidth, mouseX, mouseY);

        for (int[] step : STEPS) {
            String label = (step[3] > 0 ? "+" : "") + step[3];
            boolean hover = DbaUi.hit(mouseX, mouseY, left + step[0], top + step[1], step[2], DbaUi.BUTTON_H);
            DbaUi.Kind kind = step[3] < 0 ? DbaUi.Kind.SECONDARY : DbaUi.Kind.PRIMARY;
            DbaUi.button(context, this.font, left + step[0], top + step[1], step[2], DbaUi.BUTTON_H, label, hover, kind);
        }

        int gravity = this.menu.getGravity();
        String reading = gravity + "G";
        context.centeredText(this.font, Component.literal(reading), left + this.imageWidth / 2, top + 32, DbaUi.GOLD);
        int barX = left + 58;
        int barW = 80;
        context.fill(barX, top + 56, barX + barW, top + 60, DbaUi.INK_DEEP);
        int fill = Math.round(barW * Math.min(1.0f, gravity / 500.0f));
        if (fill > 0) {
            context.fill(barX, top + 56, barX + fill, top + 60, gravity >= 300 ? DbaUi.DANGER : DbaUi.GOLD);
        }

        drawSlot(context, left + 15, top + 79);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(context, left + 7 + col * 18, top + 113 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(context, left + 7 + col * 18, top + 171);
        }

        int fuel = this.menu.getFuel();
        String fuelText = fuel > 0 ? "Fuel  " + fuel + "t" : "No fuel";
        context.text(this.font, Component.literal(fuelText), left + 40, top + 84, fuel > 0 ? DbaUi.OK : DbaUi.MUTED);
        context.text(this.font, Component.literal("Inventory"), left + 8, top + 102, DbaUi.MUTED);

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    private void drawSlot(GuiGraphicsExtractor context, int sx, int sy) {
        DbaUi.slot(context, sx, sy);
    }
}
