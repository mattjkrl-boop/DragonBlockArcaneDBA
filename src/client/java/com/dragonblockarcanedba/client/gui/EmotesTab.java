package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.network.ActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;

public class EmotesTab implements MenuTab {
    private static final int ROW_H = 32;

    private DbaMenuScreen parent;
    private int scroll;

    @Override
    public void init(DbaMenuScreen screen) {
        this.parent = screen;
        this.scroll = 0;
    }

    private int listHeight() {
        return parent.getContentHeight() - DbaUi.BUTTON_H - DbaUi.GAP * 2;
    }

    private void send(String id) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("action", "emote");
        nbt.putString("emote", id);
        ClientPlayNetworking.send(new ActionPayload(nbt));
        if (parent != null) {
            parent.onClose();
        }
    }

    private void clamp() {
        int content = EmoteCatalog.ALL.size() * (ROW_H + 4);
        int max = Math.max(0, content - listHeight());
        if (scroll < 0) scroll = 0;
        if (scroll > max) scroll = max;
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (parent == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        int x = parent.getContentX();
        int y = parent.getContentY();
        int w = parent.getContentWidth();
        int listH = listHeight();
        if (EmoteCatalog.ALL.isEmpty()) {
            DbaUi.empty(context, client.font, x, y, w, parent.getContentHeight(), "No emotes are available.");
            return;
        }
        clamp();
        context.text(client.font, net.minecraft.network.chat.Component.literal(
                DbaUi.clip(client.font, "Movement, jumping, or an attack cancels the emote.", w - 8)), x, y, DbaUi.MUTED);
        int listY = y + 12;
        context.enableScissor(x, listY, x + w, listY + listH - 12);
        for (int i = 0; i < EmoteCatalog.ALL.size(); i++) {
            EmoteCatalog.Entry entry = EmoteCatalog.ALL.get(i);
            int rowY = listY + i * (ROW_H + 4) - scroll;
            if (rowY + ROW_H < listY || rowY > listY + listH) {
                continue;
            }
            boolean hover = DbaUi.hit(mouseX, mouseY, x, rowY, w, ROW_H);
            DbaUi.row(context, x, rowY, w, ROW_H, hover, false);
            context.text(client.font, net.minecraft.network.chat.Component.literal(DbaUi.clip(client.font, entry.name(), w / 2)),
                    x + 10, rowY + 6, DbaUi.TEXT);
            context.text(client.font, net.minecraft.network.chat.Component.literal(DbaUi.clip(client.font, entry.detail(), w / 2 - 16)),
                    x + 10, rowY + 18, DbaUi.MUTED);
        }
        context.disableScissor();

        int bw = Math.min(160, w);
        int bx = x + (w - bw) / 2;
        int by = y + parent.getContentHeight() - DbaUi.BUTTON_H;
        boolean hover = DbaUi.hit(mouseX, mouseY, bx, by, bw, DbaUi.BUTTON_H);
        DbaUi.button(context, client.font, bx, by, bw, DbaUi.BUTTON_H, "Stop emote", hover, DbaUi.Kind.DANGER);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        if (event.button() != 0 || parent == null || EmoteCatalog.ALL.isEmpty()) {
            return false;
        }
        int x = parent.getContentX();
        int y = parent.getContentY();
        int w = parent.getContentWidth();
        int listH = listHeight();
        int listY = y + 12;
        clamp();
        for (int i = 0; i < EmoteCatalog.ALL.size(); i++) {
            int rowY = listY + i * (ROW_H + 4) - scroll;
            if (rowY + ROW_H < listY || rowY > listY + listH - 12) {
                continue;
            }
            if (DbaUi.hit(event.x(), event.y(), x, rowY, w, ROW_H)) {
                DbaUi.playClick();
                send(EmoteCatalog.ALL.get(i).id());
                return true;
            }
        }
        int bw = Math.min(160, w);
        int bx = x + (w - bw) / 2;
        int by = y + parent.getContentHeight() - DbaUi.BUTTON_H;
        if (DbaUi.hit(event.x(), event.y(), bx, by, bw, DbaUi.BUTTON_H)) {
            DbaUi.playClick();
            send("");
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (parent == null) {
            return false;
        }
        if (!DbaUi.hit(mouseX, mouseY, parent.getContentX(), parent.getContentY(), parent.getContentWidth(), listHeight())) {
            return false;
        }
        scroll -= (int) Math.signum(verticalAmount) * (ROW_H + 4);
        return true;
    }
}
