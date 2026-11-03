package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.network.ActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public class EmoteSelectionScreen extends Screen {
    private static final int ROW_H = 30;

    private int x;
    private int y;
    private int w;
    private int h;
    private int scroll;

    public EmoteSelectionScreen() {
        super(Component.literal("Emotes"));
    }

    @Override
    protected void init() {
        int margin = 16;
        this.w = Math.min(420, Math.max(280, this.width - margin * 2));
        int natural = DbaUi.HEADER + 28 + EmoteCatalog.ALL.size() * (ROW_H + 4) + DbaUi.BUTTON_H + 20;
        this.h = Math.min(natural, Math.max(180, this.height - margin * 2));
        if (this.w > this.width - 8) this.w = Math.max(200, this.width - 8);
        if (this.h > this.height - 8) this.h = Math.max(160, this.height - 8);
        this.x = (this.width - w) / 2;
        this.y = (this.height - h) / 2;
        this.clearWidgets();
    }

    private int listTop() { return y + DbaUi.HEADER + 16; }
    private int listBottom() { return y + h - DbaUi.BUTTON_H - 16; }

    private void clamp() {
        int view = Math.max(1, listBottom() - listTop());
        int content = EmoteCatalog.ALL.size() * (ROW_H + 4);
        int max = Math.max(0, content - view);
        if (scroll < 0) scroll = 0;
        if (scroll > max) scroll = max;
    }

    private void send(String id) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("action", "emote");
        nbt.putString("emote", id);
        ClientPlayNetworking.send(new ActionPayload(nbt));
        this.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        DbaUi.dim(context, this.width, this.height);
        DbaUi.frame(context, x, y, w, h);
        DbaUi.title(context, this.font, x, y, DbaUi.closeX(x, w) - x - 8, "Emotes");
        DbaUi.closeControl(context, this.font, x, y, w, mouseX, mouseY);
        if (EmoteCatalog.ALL.isEmpty()) {
            DbaUi.empty(context, this.font, x, y + DbaUi.HEADER, w, h - DbaUi.HEADER, "No emotes are available.");
            super.extractRenderState(context, mouseX, mouseY, delta);
            return;
        }
        context.text(this.font, Component.literal(DbaUi.clip(this.font, "Movement or an attack cancels it.", w - 24)),
                x + 12, y + DbaUi.HEADER + 4, DbaUi.MUTED);
        clamp();
        int top = listTop();
        int bottom = listBottom();
        context.enableScissor(x + 8, top, x + w - 8, bottom);
        for (int i = 0; i < EmoteCatalog.ALL.size(); i++) {
            EmoteCatalog.Entry entry = EmoteCatalog.ALL.get(i);
            int rowY = top + i * (ROW_H + 4) - scroll;
            if (rowY + ROW_H < top || rowY > bottom) {
                continue;
            }
            boolean hover = DbaUi.hit(mouseX, mouseY, x + 10, rowY, w - 20, ROW_H);
            DbaUi.row(context, x + 10, rowY, w - 20, ROW_H, hover, false);
            context.text(this.font, Component.literal(DbaUi.clip(this.font, entry.name(), (w - 40) / 2)), x + 20, rowY + 5, DbaUi.TEXT);
            context.text(this.font, Component.literal(DbaUi.clip(this.font, entry.detail(), w - 40)), x + 20, rowY + 16, DbaUi.MUTED);
        }
        context.disableScissor();
        int bw = Math.min(160, w - 24);
        int bx = x + (w - bw) / 2;
        int by = y + h - DbaUi.BUTTON_H - 8;
        boolean hover = DbaUi.hit(mouseX, mouseY, bx, by, bw, DbaUi.BUTTON_H);
        DbaUi.button(context, this.font, bx, by, bw, DbaUi.BUTTON_H, "Stop emote", hover, DbaUi.Kind.DANGER);
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
        if (EmoteCatalog.ALL.isEmpty()) {
            return super.mouseClicked(event, isRepeat);
        }
        clamp();
        int top = listTop();
        int bottom = listBottom();
        for (int i = 0; i < EmoteCatalog.ALL.size(); i++) {
            int rowY = top + i * (ROW_H + 4) - scroll;
            if (rowY < top || rowY + ROW_H > bottom) {
                continue;
            }
            if (DbaUi.hit(event.x(), event.y(), x + 10, rowY, w - 20, ROW_H)) {
                DbaUi.playClick();
                send(EmoteCatalog.ALL.get(i).id());
                return true;
            }
        }
        int bw = Math.min(160, w - 24);
        int bx = x + (w - bw) / 2;
        int by = y + h - DbaUi.BUTTON_H - 8;
        if (DbaUi.hit(event.x(), event.y(), bx, by, bw, DbaUi.BUTTON_H)) {
            DbaUi.playClick();
            send("");
            return true;
        }
        return super.mouseClicked(event, isRepeat);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (DbaUi.hit(mouseX, mouseY, x, y, w, h)) {
            scroll -= (int) Math.signum(verticalAmount) * (ROW_H + 4);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (DbaUi.escape(event)) {
            this.onClose();
            return true;
        }
        if (com.dragonblockarcanedba.client.DragonBlockArcaneDBAClient.openEmoteMenuKey != null
                && com.dragonblockarcanedba.client.DragonBlockArcaneDBAClient.openEmoteMenuKey.matches(event)) {
            while (com.dragonblockarcanedba.client.DragonBlockArcaneDBAClient.openEmoteMenuKey.consumeClick()) {
                // Handled here so the client tick does not reopen the wheel.
            }
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
