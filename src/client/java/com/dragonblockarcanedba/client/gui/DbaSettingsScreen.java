package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.client.config.DbaConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * One setting, laid out as a single options column: the transparency slider, then Reset, then Done.
 */
public class DbaSettingsScreen extends Screen {
    private final Screen parent;
    private int x;
    private int y;
    private int bgWidth = 360;
    private int bgHeight = 188;
    private boolean dragging;

    public DbaSettingsScreen(Screen parent) {
        super(Component.literal("Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.bgWidth = Math.min(380, Math.max(280, this.width - 48));
        if (this.bgWidth > this.width - 8) {
            this.bgWidth = Math.max(220, this.width - 8);
        }
        this.bgHeight = Math.min(200, Math.max(168, this.height - 48));
        if (this.bgHeight > this.height - 8) {
            this.bgHeight = Math.max(150, this.height - 8);
        }
        this.x = (this.width - bgWidth) / 2;
        this.y = (this.height - bgHeight) / 2;
        this.dragging = false;
        this.clearWidgets();
    }

    private int bodyX() { return x + 12; }
    private int bodyW() { return bgWidth - 24; }
    private int sliderY() { return y + 52; }
    private int resetY() { return y + bgHeight - DbaUi.BUTTON_H * 2 - 14; }
    private int doneY() { return y + bgHeight - DbaUi.BUTTON_H - 8; }

    private void close() {
        if (this.minecraft == null) {
            return;
        }
        if (this.parent != null) {
            this.minecraft.setScreenAndShow(this.parent);
        } else {
            super.onClose();
        }
    }

    private void setTransparency(double mouseX) {
        float frac = (float) (mouseX - bodyX()) / (float) bodyW();
        int percent = Math.max(0, Math.min(100, Math.round(frac * 100.0f)));
        DbaConfig.firstPersonTransparency = percent;
        DbaConfig.save();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        DbaUi.dim(context, this.width, this.height);
        DbaUi.frame(context, x, y, bgWidth, bgHeight);
        DbaUi.title(context, this.font, x, y, DbaUi.closeX(x, bgWidth) - x - 8, "Settings");
        DbaUi.closeControl(context, this.font, x, y, bgWidth, mouseX, mouseY);
        context.text(this.font, Component.literal(DbaUi.clip(this.font, "How see-through your hand is in first person.", bodyW())),
                bodyX(), y + 36, DbaUi.MUTED);
        DbaUi.slider(context, this.font, bodyX(), sliderY(), bodyW(),
                "First-person transparency", DbaConfig.firstPersonTransparency + "%",
                DbaConfig.firstPersonTransparency / 100.0f);

        boolean resetHover = DbaUi.hit(mouseX, mouseY, bodyX(), resetY(), bodyW(), DbaUi.BUTTON_H);
        boolean doneHover = DbaUi.hit(mouseX, mouseY, bodyX(), doneY(), bodyW(), DbaUi.BUTTON_H);
        DbaUi.button(context, this.font, bodyX(), resetY(), bodyW(), DbaUi.BUTTON_H, "Reset", resetHover, DbaUi.Kind.DANGER);
        DbaUi.button(context, this.font, bodyX(), doneY(), bodyW(), DbaUi.BUTTON_H, "Done", doneHover, DbaUi.Kind.PRIMARY);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        if (event.button() != 0) {
            return super.mouseClicked(event, isRepeat);
        }
        double mx = event.x();
        double my = event.y();
        if (DbaUi.closeHit(x, y, bgWidth, mx, my)) {
            DbaUi.playClick();
            close();
            return true;
        }
        if (DbaUi.sliderHit(bodyX(), sliderY(), bodyW(), mx, my)) {
            dragging = true;
            setTransparency(mx);
            return true;
        }
        if (DbaUi.hit(mx, my, bodyX(), resetY(), bodyW(), DbaUi.BUTTON_H)) {
            DbaUi.playClick();
            DbaConfig.firstPersonTransparency = 0;
            DbaConfig.save();
            return true;
        }
        if (DbaUi.hit(mx, my, bodyX(), doneY(), bodyW(), DbaUi.BUTTON_H)) {
            DbaUi.playClick();
            close();
            return true;
        }
        return super.mouseClicked(event, isRepeat);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging) {
            setTransparency(event.x());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (DbaUi.escape(event)) {
            close();
            return true;
        }
        return super.keyPressed(event);
    }
}
