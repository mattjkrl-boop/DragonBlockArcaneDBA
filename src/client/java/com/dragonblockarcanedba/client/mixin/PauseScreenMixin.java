package com.dragonblockarcanedba.client.mixin;

import com.dragonblockarcanedba.client.gui.DbaMenuScreen;
import com.dragonblockarcanedba.client.gui.DbaSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * DBA Character and DBA Settings sit in the pause column, on the row above
 * Save and Quit, at the same width as Advancements and Statistics.
 */
@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void dba$addPauseButtons(CallbackInfo ci) {
        AbstractWidget full = null;
        AbstractWidget half = null;
        for (GuiEventListener child : this.children()) {
            if (!(child instanceof AbstractWidget widget) || widget.getHeight() < 18) {
                continue;
            }
            int width = widget.getWidth();
            if (width >= 180) {
                if (full == null || widget.getY() > full.getY()) {
                    full = widget;
                }
            } else if (width >= 80 && width <= 120) {
                half = widget;
            }
        }
        if (full == null) {
            return;
        }

        int rowY = full.getY();
        int rowH = full.getHeight();
        int left = full.getX();
        int fullW = full.getWidth();
        int halfW = half == null ? 98 : half.getWidth();
        if (halfW * 2 >= fullW) {
            halfW = Math.max(40, (fullW - 8) / 2);
        }
        int right = left + fullW - halfW;
        int shift = rowH + 4;
        for (GuiEventListener child : List.copyOf(this.children())) {
            if (child instanceof AbstractWidget widget && widget.getY() >= rowY) {
                widget.setY(widget.getY() + shift);
            }
        }

        int limit = this.height - 8;
        int bottom = rowY + shift + rowH;
        if (bottom > limit) {
            int up = bottom - limit;
            for (GuiEventListener child : this.children()) {
                if (child instanceof AbstractWidget widget) {
                    widget.setY(widget.getY() - up);
                }
            }
            rowY -= up;
        }

        Screen host = (Screen) (Object) this;
        int buttonY = rowY;
        int buttonW = halfW;
        int buttonH = rowH;
        this.addRenderableWidget(Button.builder(
                Component.literal("DBA Character"),
                btn -> Minecraft.getInstance().setScreenAndShow(new DbaMenuScreen(host))
        ).bounds(left, buttonY, buttonW, buttonH).build());
        this.addRenderableWidget(Button.builder(
                Component.literal("DBA Settings"),
                btn -> Minecraft.getInstance().setScreenAndShow(new DbaSettingsScreen(host))
        ).bounds(right, buttonY, buttonW, buttonH).build());
    }
}
