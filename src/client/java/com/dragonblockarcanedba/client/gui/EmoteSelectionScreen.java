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
 * Emote wheel. Each catalog entry is a button on the ring and still sends the emote action.
 */
public class EmoteSelectionScreen extends Screen {
    private int headerX;
    private int headerY;
    private int headerW;

    public EmoteSelectionScreen() {
        super(Component.literal("Emotes"));
    }

    @Override
    protected void init() {
        this.headerX = 8;
        this.headerY = 6;
        this.headerW = Math.max(160, this.width - 16);
        this.clearWidgets();
    }

    private int centerX() { return this.width / 2; }

    private int centerY() { return 36 + Math.max(40, (this.height - 36) / 2); }

    private int buttonW() { return Math.min(120, Math.max(84, this.width / 6)); }

    private int buttonH() { return 18; }

    private int radius() {
        int bw = buttonW();
        int room = Math.min(centerX() - bw / 2 - 12, (this.height - 36) / 2 - buttonH() / 2 - 16);
        int gap = bw / 2 + 52;
        return Math.max(gap, room);
    }

    private int ringX(int index, int count) {
        double angle = -Math.PI / 2.0 + index * (Math.PI * 2.0 / Math.max(1, count));
        return (int) Math.round(centerX() + Math.cos(angle) * radius() - buttonW() / 2.0);
    }

    private int ringY(int index, int count) {
        double angle = -Math.PI / 2.0 + index * (Math.PI * 2.0 / Math.max(1, count));
        return (int) Math.round(centerY() + Math.sin(angle) * radius() - buttonH() / 2.0);
    }

    private int stopX() { return centerX() - 40; }
    private int stopY() { return centerY() - 9; }

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
        DbaUi.title(context, this.font, headerX, headerY, headerW - 72, "Emotes");
        DbaUi.closeControl(context, this.font, headerX, headerY, headerW, mouseX, mouseY);
        context.text(this.font, Component.literal(DbaUi.clip(this.font, "Movement or an attack cancels it.", headerW - 80)),
                headerX + 8, headerY + 22, DbaUi.MUTED);
        if (EmoteCatalog.ALL.isEmpty()) {
            DbaUi.empty(context, this.font, 0, 40, this.width, this.height - 40, "No emotes are available.");
            super.extractRenderState(context, mouseX, mouseY, delta);
            return;
        }
        int count = EmoteCatalog.ALL.size();
        int bw = buttonW();
        int bh = buttonH();
        for (int i = 0; i < count; i++) {
            int bx = ringX(i, count);
            int by = ringY(i, count);
            boolean hover = DbaUi.hit(mouseX, mouseY, bx, by, bw, bh);
            DbaUi.button(context, this.font, bx, by, bw, bh, EmoteCatalog.ALL.get(i).name(), hover, DbaUi.Kind.SECONDARY);
        }
        boolean stopHover = DbaUi.hit(mouseX, mouseY, stopX(), stopY(), 80, 18);
        DbaUi.button(context, this.font, stopX(), stopY(), 80, 18, "Stop emote", stopHover, DbaUi.Kind.DANGER);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        if (event.button() != 0) {
            return super.mouseClicked(event, isRepeat);
        }
        if (DbaUi.closeHit(headerX, headerY, headerW, event.x(), event.y())) {
            DbaUi.playClick();
            this.onClose();
            return true;
        }
        if (EmoteCatalog.ALL.isEmpty()) {
            return super.mouseClicked(event, isRepeat);
        }
        if (DbaUi.hit(event.x(), event.y(), stopX(), stopY(), 80, 18)) {
            DbaUi.playClick();
            send("");
            return true;
        }
        int count = EmoteCatalog.ALL.size();
        int bw = buttonW();
        int bh = buttonH();
        for (int i = 0; i < count; i++) {
            if (DbaUi.hit(event.x(), event.y(), ringX(i, count), ringY(i, count), bw, bh)) {
                DbaUi.playClick();
                send(EmoteCatalog.ALL.get(i).id());
                return true;
            }
        }
        return super.mouseClicked(event, isRepeat);
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
