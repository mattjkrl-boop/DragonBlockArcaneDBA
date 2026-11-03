package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.entity.PorungaEntity;
import com.dragonblockarcanedba.network.C2SMakeWishPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class WishScreen extends Screen {
    private static final int ROW_H = 40;

    private final int shenronId;
    private int wishesMade;
    private int x;
    private int y;
    private int w;
    private int h;

    private record WishOption(String id, String title, String detail) {}

    private static final WishOption[] OPTIONS = {
            new WishOption("wealth", "Wealth", "64 Silver Zeni"),
            new WishOption("power", "Power", "150 ability points"),
            new WishOption("immortality", "Immortality", "16 Senzu Beans and 10 minutes of Celestial Grace")
    };

    public WishScreen(int shenronId) {
        super(Component.literal("Wish"));
        this.shenronId = shenronId;
        com.dragonblockarcanedba.client.render.geo.DbaDragonRenderers.markWish(shenronId);
    }

    public int dragonId() {
        return this.shenronId;
    }

    private boolean porunga() {
        return this.minecraft != null
                && this.minecraft.level != null
                && this.minecraft.level.getEntity(this.shenronId) instanceof PorungaEntity;
    }

    @Override
    protected void init() {
        this.w = Math.min(420, Math.max(300, this.width - 24));
        this.h = Math.min(DbaUi.HEADER + 36 + OPTIONS.length * (ROW_H + 6) + 36, Math.max(220, this.height - 24));
        if (this.w > this.width - 8) this.w = Math.max(240, this.width - 8);
        if (this.h > this.height - 8) this.h = Math.max(200, this.height - 8);
        this.x = (this.width - w) / 2;
        this.y = (this.height - h) / 2;
        this.clearWidgets();
    }

    private int rowY(int index) {
        return y + DbaUi.HEADER + 28 + index * (ROW_H + 6);
    }

    private void wish(String id) {
        ClientPlayNetworking.send(new C2SMakeWishPayload(this.shenronId, id));
        if (porunga()) {
            this.wishesMade++;
            if (this.wishesMade >= 3) {
                this.onClose();
            }
        } else {
            this.onClose();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        DbaUi.dim(context, this.width, this.height);
        DbaUi.frame(context, x, y, w, h);
        String title = porunga() ? "Porunga" : "Shenron";
        DbaUi.title(context, this.font, x, y, DbaUi.closeX(x, w) - x - 8, title);
        DbaUi.closeControl(context, this.font, x, y, w, mouseX, mouseY);
        String subtitle = porunga()
                ? "Wish " + (wishesMade + 1) + " of 3"
                : "One wish.";
        context.text(this.font, Component.literal(subtitle), x + 12, y + DbaUi.HEADER + 6, DbaUi.MUTED);

        for (int i = 0; i < OPTIONS.length; i++) {
            int ry = rowY(i);
            if (ry + ROW_H > y + h - 8) {
                break;
            }
            boolean hover = DbaUi.hit(mouseX, mouseY, x + 10, ry, w - 20, ROW_H);
            DbaUi.row(context, x + 10, ry, w - 20, ROW_H, hover, false);
            context.text(this.font, Component.literal(OPTIONS[i].title()), x + 20, ry + 6, DbaUi.TEXT);
            context.text(this.font, Component.literal(DbaUi.clip(this.font, OPTIONS[i].detail(), w - 40)), x + 20, ry + 20, DbaUi.MUTED);
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
        for (int i = 0; i < OPTIONS.length; i++) {
            int ry = rowY(i);
            if (ry + ROW_H > y + h - 8) {
                break;
            }
            if (DbaUi.hit(event.x(), event.y(), x + 10, ry, w - 20, ROW_H)) {
                DbaUi.playClick();
                wish(OPTIONS[i].id());
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
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
