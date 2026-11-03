package com.dragonblockarcanedba.client.mixin;

import com.dragonblockarcanedba.client.gui.DbaSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * DBA Settings is another full-width title button, directly under the last one,
 * and the Options / Quit row moves down with it.
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void dba$addTitleButton(CallbackInfo ci) {
        AbstractWidget anchor = null;
        for (GuiEventListener child : this.children()) {
            if (!(child instanceof AbstractWidget widget) || widget.getHeight() < 18 || widget.getWidth() < 170) {
                continue;
            }
            if (anchor == null || widget.getY() > anchor.getY()) {
                anchor = widget;
            }
        }
        if (anchor == null) {
            return;
        }

        int x = anchor.getX();
        int w = anchor.getWidth();
        int h = anchor.getHeight();
        int y = anchor.getY() + h + 4;
        int push = h + 4;
        for (GuiEventListener child : List.copyOf(this.children())) {
            if (!(child instanceof AbstractWidget widget) || widget.getY() < y - 2) {
                continue;
            }
            boolean inColumn = widget.getX() + widget.getWidth() > x && widget.getX() < x + w;
            if (inColumn) {
                widget.setY(widget.getY() + push);
            }
        }

        int limit = this.height - 8;
        if (y + h > limit) {
            int up = y + h - limit;
            for (GuiEventListener child : this.children()) {
                if (child instanceof AbstractWidget widget) {
                    widget.setY(Math.max(4, widget.getY() - up));
                }
            }
            y -= up;
        }

        Screen host = (Screen) (Object) this;
        int buttonX = x;
        int buttonY = y;
        int buttonW = w;
        int buttonH = h;
        this.addRenderableWidget(Button.builder(
                Component.literal("DBA Settings"),
                btn -> Minecraft.getInstance().setScreenAndShow(new DbaSettingsScreen(host))
        ).bounds(buttonX, buttonY, buttonW, buttonH).build());
    }
}
