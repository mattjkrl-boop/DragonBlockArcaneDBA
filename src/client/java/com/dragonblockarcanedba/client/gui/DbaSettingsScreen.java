package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.client.config.DbaConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * One setting: how transparent the first-person hand is.
 * Removed options stay removed.
 */
public class DbaSettingsScreen extends Screen {
    private final Screen parent;
    private int x;
    private int y;
    private int bgWidth = 420;
    private int bgHeight = 168;
    private boolean dragging;

    public DbaSettingsScreen(Screen parent) {
        super(Component.literal("Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.bgWidth = Math.min(460, Math.max(320, this.width - 24));
        if (this.bgWidth > this.width - 8) {
            this.bgWidth = Math.max(220, this.width - 8);
        }
        this.bgHeight = Math.min(168, Math.max(140, this.height - 24));
        if (this.bgHeight > this.height - 8) {
            this.bgHeight = Math.max(120, this.height - 8);
        }
        this.x = (this.width - bgWidth) / 2;
        this.y = (this.height - bgHeight) / 2;
        this.dragging = false;
        this.clearWidgets();
    }

    private int sliderX() { return x + DbaUi.PAD + 4; }
    private int sliderY() { return y + DbaUi.HEADER + 18; }
    private int sliderW() { return bgWidth - DbaUi.PAD * 2 - 8; }

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
        float frac = (float) (mouseX - sliderX()) / (float) sliderW();
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

        DbaUi.slider(context, this.font, sliderX(), sliderY(), sliderW(),
                "First-person transparency", DbaConfig.firstPersonTransparency + "%",
                DbaConfig.firstPersonTransparency / 100.0f);
        context.text(this.font, Component.literal(DbaUi.clip(this.font, "How see-through your hand is in first person.", sliderW())),
                sliderX(), sliderY() + 32, DbaUi.MUTED);

        int btnW = (sliderW() - DbaUi.GAP) / 2;
        int btnY = y + bgHeight - DbaUi.BUTTON_H - DbaUi.PAD;
        boolean resetHover = DbaUi.hit(mouseX, mouseY, sliderX(), btnY, btnW, DbaUi.BUTTON_H);
        boolean doneHover = DbaUi.hit(mouseX, mouseY, sliderX() + btnW + DbaUi.GAP, btnY, btnW, DbaUi.BUTTON_H);
        DbaUi.button(context, this.font, sliderX(), btnY, btnW, DbaUi.BUTTON_H, "Reset", resetHover, DbaUi.Kind.DANGER);
        DbaUi.button(context, this.font, sliderX() + btnW + DbaUi.GAP, btnY, btnW, DbaUi.BUTTON_H, "Done", doneHover, DbaUi.Kind.PRIMARY);
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
        if (DbaUi.sliderHit(sliderX(), sliderY(), sliderW(), mx, my)) {
            dragging = true;
            setTransparency(mx);
            return true;
        }
        int btnW = (sliderW() - DbaUi.GAP) / 2;
        int btnY = y + bgHeight - DbaUi.BUTTON_H - DbaUi.PAD;
        if (DbaUi.hit(mx, my, sliderX(), btnY, btnW, DbaUi.BUTTON_H)) {
            DbaUi.playClick();
            DbaConfig.firstPersonTransparency = 0;
            DbaConfig.save();
            return true;
        }
        if (DbaUi.hit(mx, my, sliderX() + btnW + DbaUi.GAP, btnY, btnW, DbaUi.BUTTON_H)) {
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
