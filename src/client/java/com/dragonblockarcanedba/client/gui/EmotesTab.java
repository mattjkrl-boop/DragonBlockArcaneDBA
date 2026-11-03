package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.network.ActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * Emotes sit in a two-column grid. Choosing one, or Stop, still sends the emote action.
 */
public class EmotesTab implements MenuTab {
    private static final int ROW_H = 36;

    private DbaMenuScreen parent;
    private int scroll;

    @Override
    public void init(DbaMenuScreen screen) {
        this.parent = screen;
        this.scroll = 0;
    }

    private int cols(int w) {
        return w >= 340 ? 2 : 1;
    }

    private int listHeight() {
        return parent.getContentHeight() - DbaUi.BUTTON_H - 20;
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

    private void clamp(int w) {
        int cols = cols(w);
        int rows = (EmoteCatalog.ALL.size() + cols - 1) / cols;
        int content = rows * (ROW_H + DbaUi.GAP);
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
        clamp(w);
        context.text(client.font, Component.literal(DbaUi.clip(client.font, "Movement, jumping, or an attack cancels the emote.", w)),
                x, y, DbaUi.MUTED);
        int listY = y + 12;
        int cols = cols(w);
        int colW = (w - DbaUi.GAP * (cols - 1)) / cols;
        context.enableScissor(x, listY, x + w, listY + listH - 12);
        for (int i = 0; i < EmoteCatalog.ALL.size(); i++) {
            EmoteCatalog.Entry entry = EmoteCatalog.ALL.get(i);
            int col = i % cols;
            int row = i / cols;
            int cellX = x + col * (colW + DbaUi.GAP);
            int cellY = listY + row * (ROW_H + DbaUi.GAP) - scroll;
            if (cellY + ROW_H < listY || cellY > listY + listH) {
                continue;
            }
            boolean hover = DbaUi.hit(mouseX, mouseY, cellX, cellY, colW, ROW_H);
            DbaUi.row(context, cellX, cellY, colW, ROW_H, hover, false);
            context.text(client.font, Component.literal(DbaUi.clip(client.font, entry.name(), colW - 16)), cellX + 8, cellY + 6, DbaUi.TEXT);
            context.text(client.font, Component.literal(DbaUi.clip(client.font, entry.detail(), colW - 16)), cellX + 8, cellY + 18, DbaUi.MUTED);
        }
        context.disableScissor();

        int by = y + parent.getContentHeight() - DbaUi.BUTTON_H;
        boolean hover = DbaUi.hit(mouseX, mouseY, x, by, w, DbaUi.BUTTON_H);
        DbaUi.button(context, client.font, x, by, w, DbaUi.BUTTON_H, "Stop emote", hover, DbaUi.Kind.DANGER);
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
        int cols = cols(w);
        int colW = (w - DbaUi.GAP * (cols - 1)) / cols;
        clamp(w);
        for (int i = 0; i < EmoteCatalog.ALL.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int cellX = x + col * (colW + DbaUi.GAP);
            int cellY = listY + row * (ROW_H + DbaUi.GAP) - scroll;
            if (cellY + ROW_H < listY || cellY > listY + listH - 12) {
                continue;
            }
            if (DbaUi.hit(event.x(), event.y(), cellX, cellY, colW, ROW_H)) {
                DbaUi.playClick();
                send(EmoteCatalog.ALL.get(i).id());
                return true;
            }
        }
        int by = y + parent.getContentHeight() - DbaUi.BUTTON_H;
        if (DbaUi.hit(event.x(), event.y(), x, by, w, DbaUi.BUTTON_H)) {
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
        scroll -= (int) Math.signum(verticalAmount) * (ROW_H + DbaUi.GAP);
        return true;
    }
}
