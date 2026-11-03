package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.network.SpacePodLaunchPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class SpacePodScreen extends Screen {
    private static final int ROW_H = 36;

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
        this.w = Math.min(440, Math.max(300, this.width - 24));
        int natural = DbaUi.HEADER + 28 + DESTINATIONS.length * (ROW_H + 4) + 16;
        this.h = Math.min(natural, Math.max(220, this.height - 24));
        if (this.w > this.width - 8) this.w = Math.max(240, this.width - 8);
        if (this.h > this.height - 8) this.h = Math.max(180, this.height - 8);
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

    private int rowY(int index) {
        return y + DbaUi.HEADER + 22 + index * (ROW_H + 4);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        DbaUi.dim(context, this.width, this.height);
        DbaUi.frame(context, x, y, w, h);
        DbaUi.title(context, this.font, x, y, DbaUi.closeX(x, w) - x - 8, "Space Pod");
        DbaUi.closeControl(context, this.font, x, y, w, mouseX, mouseY);
        context.text(this.font, Component.literal("Choose a destination."), x + 12, y + DbaUi.HEADER + 4, DbaUi.MUTED);
        for (int i = 0; i < DESTINATIONS.length; i++) {
            int ry = rowY(i);
            if (ry + ROW_H > y + h - 8) {
                break;
            }
            Destination dest = DESTINATIONS[i];
            boolean current = here(dest.id());
            boolean hover = !current && DbaUi.hit(mouseX, mouseY, x + 10, ry, w - 20, ROW_H);
            DbaUi.row(context, x + 10, ry, w - 20, ROW_H, hover, current);
            context.text(this.font, Component.literal(dest.name()), x + 20, ry + 5, current ? DbaUi.GOLD : DbaUi.TEXT);
            String detail = current ? "You are here" : dest.detail();
            context.text(this.font, Component.literal(DbaUi.clip(this.font, detail, w - 40)), x + 20, ry + 18, DbaUi.MUTED);
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
        for (int i = 0; i < DESTINATIONS.length; i++) {
            int ry = rowY(i);
            if (ry + ROW_H > y + h - 8) {
                break;
            }
            if (!DbaUi.hit(event.x(), event.y(), x + 10, ry, w - 20, ROW_H)) {
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
