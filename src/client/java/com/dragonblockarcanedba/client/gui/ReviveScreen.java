package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.network.ActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * Two choices side by side. Return still sends the revive action. Remain closes.
 */
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
        this.w = Math.min(520, Math.max(340, this.width - 24));
        this.h = Math.min(220, Math.max(180, this.height - 24));
        if (this.w > this.width - 8) this.w = Math.max(260, this.width - 8);
        if (this.h > this.height - 8) this.h = Math.max(160, this.height - 8);
        this.x = (this.width - w) / 2;
        this.y = (this.height - h) / 2;
        this.clearWidgets();
    }

    private int cardY() { return y + DbaUi.HEADER + 28; }
    private int cardH() { return Math.max(64, h - DbaUi.HEADER - 44); }
    private int cardW() { return (w - 28) / 2; }
    private int stayX() { return x + 14 + cardW() + 8; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        DbaUi.dim(context, this.width, this.height);
        DbaUi.frame(context, x, y, w, h);
        DbaUi.title(context, this.font, x, y, DbaUi.closeX(x, w) - x - 8, "Otherworld");
        DbaUi.closeControl(context, this.font, x, y, w, mouseX, mouseY);
        context.text(this.font, Component.literal(DbaUi.clip(this.font, "Your body has fallen. Choose where you go.", w - 24)),
                x + 12, y + DbaUi.HEADER + 6, DbaUi.MUTED);

        int cw = cardW();
        int ch = cardH();
        int cy = cardY();
        boolean reviveHover = DbaUi.hit(mouseX, mouseY, x + 10, cy, cw, ch);
        boolean stayHover = DbaUi.hit(mouseX, mouseY, stayX(), cy, cw, ch);
        DbaUi.row(context, x + 10, cy, cw, ch, reviveHover, false);
        DbaUi.row(context, stayX(), cy, cw, ch, stayHover, false);
        context.text(this.font, Component.literal("Return"), x + 20, cy + 12, DbaUi.TEXT);
        context.text(this.font, Component.literal(DbaUi.clip(this.font, "Revive on Earth.", cw - 20)), x + 20, cy + 26, DbaUi.MUTED);
        context.text(this.font, Component.literal("Remain"), stayX() + 10, cy + 12, DbaUi.TEXT);
        context.text(this.font, Component.literal(DbaUi.clip(this.font, "Stay in Otherworld.", cw - 20)), stayX() + 10, cy + 26, DbaUi.MUTED);
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
        int cw = cardW();
        int ch = cardH();
        if (DbaUi.hit(event.x(), event.y(), x + 10, cardY(), cw, ch)) {
            DbaUi.playClick();
            CompoundTag nbt = new CompoundTag();
            nbt.putString("action", "revive");
            ClientPlayNetworking.send(new ActionPayload(nbt));
            this.onClose();
            return true;
        }
        if (DbaUi.hit(event.x(), event.y(), stayX(), cardY(), cw, ch)) {
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
