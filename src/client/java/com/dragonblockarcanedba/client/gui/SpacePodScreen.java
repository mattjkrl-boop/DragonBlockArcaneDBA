package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.network.SpacePodLaunchPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Destinations in a two-column grid. A launch still sends that destination id.
 */
public class SpacePodScreen extends Screen {
    private record Destination(String id, String name, String detail) {}

    private static final Destination[] DESTINATIONS = {
            new Destination("namek", "Namek", "0.9G  ·  Namekian homeland"),
            new Destination("vegeta", "Vegeta", "1.5G  ·  Saiyan cradle"),
            new Destination("yardrat", "Yardrat", "0.8G  ·  Spirit training"),
            new Destination("otherworld", "Otherworld", "Spirit realm"),
            new Destination("overworld", "Earth", "Return to the overworld")
    };

    private int x;
    private int y;
    private int w;
    private int h;

    public SpacePodScreen() {
        super(Component.literal("Space Pod"));
    }

    @Override
    protected void init() {
        this.w = Math.min(520, Math.max(340, this.width - 24));
        int rows = (DESTINATIONS.length + 1) / 2;
        int natural = DbaUi.HEADER + 36 + rows * 48 + 12;
        this.h = Math.min(natural, Math.max(240, this.height - 24));
        if (this.w > this.width - 8) this.w = Math.max(260, this.width - 8);
        if (this.h > this.height - 8) this.h = Math.max(200, this.height - 8);
        this.x = (this.width - w) / 2;
        this.y = (this.height - h) / 2;
        this.clearWidgets();
    }

    private boolean here(String id) {
        if (this.minecraft == null || this.minecraft.level == null) {
            return false;
        }
        return id.equals(this.minecraft.level.dimension().identifier().getPath());
    }

    private int colW() { return (w - 28) / 2; }

    private int cellX(int index) {
        return x + 10 + (index % 2) * (colW() + 8);
    }

    private int cellY(int index) {
        return y + DbaUi.HEADER + 24 + (index / 2) * 48;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        DbaUi.dim(context, this.width, this.height);
        DbaUi.frame(context, x, y, w, h);
        DbaUi.title(context, this.font, x, y, DbaUi.closeX(x, w) - x - 8, "Space Pod");
        DbaUi.closeControl(context, this.font, x, y, w, mouseX, mouseY);
        context.text(this.font, Component.literal("Choose a destination."), x + 12, y + DbaUi.HEADER + 4, DbaUi.MUTED);
        int cw = colW();
        for (int i = 0; i < DESTINATIONS.length; i++) {
            int cx = cellX(i);
            int cy = cellY(i);
            if (cy + 40 > y + h - 6) {
                break;
            }
            Destination dest = DESTINATIONS[i];
            boolean current = here(dest.id());
            boolean hover = !current && DbaUi.hit(mouseX, mouseY, cx, cy, cw, 40);
            DbaUi.row(context, cx, cy, cw, 40, hover, current);
            context.text(this.font, Component.literal(dest.name()), cx + 8, cy + 6, current ? DbaUi.GOLD : DbaUi.TEXT);
            String detail = current ? "You are here" : dest.detail();
            context.text(this.font, Component.literal(DbaUi.clip(this.font, detail, cw - 16)), cx + 8, cy + 20, DbaUi.MUTED);
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
        int cw = colW();
        for (int i = 0; i < DESTINATIONS.length; i++) {
            int cy = cellY(i);
            if (cy + 40 > y + h - 6) {
                break;
            }
            if (!DbaUi.hit(event.x(), event.y(), cellX(i), cy, cw, 40)) {
                continue;
            }
            if (here(DESTINATIONS[i].id())) {
                continue;
            }
            DbaUi.playClick();
            ClientPlayNetworking.send(new SpacePodLaunchPayload(DESTINATIONS[i].id()));
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
