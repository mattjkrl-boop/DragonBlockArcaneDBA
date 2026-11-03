package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.entity.PorungaEntity;
import com.dragonblockarcanedba.network.C2SMakeWishPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Three wish cards. Each one still sends the wish id the dragon already grants.
 */
public class WishScreen extends Screen {
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
        this.w = Math.min(560, Math.max(340, this.width - 24));
        this.h = Math.min(230, Math.max(200, this.height - 24));
        if (this.w > this.width - 8) this.w = Math.max(260, this.width - 8);
        if (this.h > this.height - 8) this.h = Math.max(190, this.height - 8);
        this.x = (this.width - w) / 2;
        this.y = (this.height - h) / 2;
        this.clearWidgets();
    }

    private boolean across() {
        return true;
    }

    private int cardX(int index) {
        if (!across()) {
            return x + 10;
        }
        int cw = (w - 32) / 3;
        return x + 8 + index * (cw + 8);
    }

    private int cardY(int index) {
        if (across()) {
            return y + DbaUi.HEADER + 28;
        }
        return y + DbaUi.HEADER + 28 + index * 48;
    }

    private int cardW() {
        return across() ? (w - 32) / 3 : w - 20;
    }

    private int cardH() {
        return across() ? Math.max(72, h - DbaUi.HEADER - 44) : 42;
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

        int cw = cardW();
        int ch = cardH();
        for (int i = 0; i < OPTIONS.length; i++) {
            int cx = cardX(i);
            int cy = cardY(i);
            if (cy + ch > y + h - 6) {
                break;
            }
            boolean hover = DbaUi.hit(mouseX, mouseY, cx, cy, cw, ch);
            DbaUi.row(context, cx, cy, cw, ch, hover, false);
            context.text(this.font, Component.literal(OPTIONS[i].title()), cx + 8, cy + 8, DbaUi.TEXT);
            context.text(this.font, Component.literal(DbaUi.clip(this.font, OPTIONS[i].detail(), cw - 16)), cx + 8, cy + 22, DbaUi.MUTED);
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
        int cw = cardW();
        int ch = cardH();
        for (int i = 0; i < OPTIONS.length; i++) {
            int cy = cardY(i);
            if (cy + ch > y + h - 6) {
                break;
            }
            if (DbaUi.hit(event.x(), event.y(), cardX(i), cy, cw, ch)) {
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
