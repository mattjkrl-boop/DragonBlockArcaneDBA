package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.network.ActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public class ReviveScreen extends Screen {
    private int x;
    private int y;
    private int w;
    private int h;

    public ReviveScreen() {
        super(Component.literal("Otherworld"));
    }

    @Override
    protected void init() {
        this.w = Math.min(420, Math.max(300, this.width - 24));
        this.h = Math.min(196, Math.max(168, this.height - 24));
        if (this.w > this.width - 8) this.w = Math.max(240, this.width - 8);
        if (this.h > this.height - 8) this.h = Math.max(150, this.height - 8);
        this.x = (this.width - w) / 2;
        this.y = (this.height - h) / 2;
        this.clearWidgets();
    }

    private int reviveY() { return y + DbaUi.HEADER + 28; }
    private int stayY() { return reviveY() + 48; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        DbaUi.dim(context, this.width, this.height);
        DbaUi.frame(context, x, y, w, h);
        DbaUi.title(context, this.font, x, y, DbaUi.closeX(x, w) - x - 8, "Otherworld");
        DbaUi.closeControl(context, this.font, x, y, w, mouseX, mouseY);
        context.text(this.font, Component.literal(DbaUi.clip(this.font, "Your body has fallen. Choose where you go.", w - 24)),
                x + 12, y + DbaUi.HEADER + 6, DbaUi.MUTED);

        int rowW = w - 20;
        boolean reviveHover = DbaUi.hit(mouseX, mouseY, x + 10, reviveY(), rowW, 40);
        boolean stayHover = DbaUi.hit(mouseX, mouseY, x + 10, stayY(), rowW, 40);
        DbaUi.row(context, x + 10, reviveY(), rowW, 40, reviveHover, false);
        context.text(this.font, Component.literal("Return to the living world"), x + 20, reviveY() + 6, DbaUi.TEXT);
        context.text(this.font, Component.literal(DbaUi.clip(this.font, "Revive on Earth.", rowW - 20)), x + 20, reviveY() + 20, DbaUi.MUTED);

        if (stayY() + 40 <= y + h - 8) {
            DbaUi.row(context, x + 10, stayY(), rowW, 40, stayHover, false);
            context.text(this.font, Component.literal("Remain in the spirit realm"), x + 20, stayY() + 6, DbaUi.TEXT);
            context.text(this.font, Component.literal(DbaUi.clip(this.font, "Stay in Otherworld.", rowW - 20)), x + 20, stayY() + 20, DbaUi.MUTED);
        }
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        if (event.button() != 0) {
            return super.mouseClicked(event, isRepeat);
        }
        if (DbaUi.closeHit(x, y, w, event.x(), event.y())) {
            DbaUi.playClick();
            this.onClose();
            return true;
        }
        int rowW = w - 20;
        if (DbaUi.hit(event.x(), event.y(), x + 10, reviveY(), rowW, 40)) {
            DbaUi.playClick();
            CompoundTag nbt = new CompoundTag();
            nbt.putString("action", "revive");
            ClientPlayNetworking.send(new ActionPayload(nbt));
            this.onClose();
            return true;
        }
        if (stayY() + 40 <= y + h - 8 && DbaUi.hit(event.x(), event.y(), x + 10, stayY(), rowW, 40)) {
            DbaUi.playClick();
            this.onClose();
            return true;
        }
        return super.mouseClicked(event, isRepeat);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (DbaUi.escape(event)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
