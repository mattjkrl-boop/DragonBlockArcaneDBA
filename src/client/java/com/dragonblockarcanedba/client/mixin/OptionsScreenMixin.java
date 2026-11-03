package com.dragonblockarcanedba.client.mixin;

import com.dragonblockarcanedba.client.gui.DbaSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * DBA Settings is one more options-grid button, the same size as FOV and the
 * category buttons. Done stays in the footer and moves down when a new row is needed.
 */
@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {
    protected OptionsScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void dba$moveSettingsButton(CallbackInfo ci) {
        this.dba$placeSettingsButton();
    }

    private void dba$placeSettingsButton() {
        List<GuiEventListener> stale = new ArrayList<>();
        for (GuiEventListener child : this.children()) {
            if (child instanceof AbstractWidget widget && "DBA Settings".equals(widget.getMessage().getString())) {
                stale.add(child);
            }
        }
        for (GuiEventListener child : stale) {
            this.removeWidget(child);
        }

        List<AbstractWidget> cells = new ArrayList<>();
        for (GuiEventListener child : this.children()) {
            if (!(child instanceof AbstractWidget widget) || widget.getHeight() < 16) {
                continue;
            }
            int width = widget.getWidth();
            if (width >= 80 && width < 190) {
                cells.add(widget);
            }
        }
        if (cells.isEmpty()) {
            return;
        }
        cells.sort(Comparator.comparingInt(AbstractWidget::getY).thenComparingInt(AbstractWidget::getX));

        int cellW = cells.get(0).getWidth();
        int cellH = cells.get(0).getHeight();
        int minX = Integer.MAX_VALUE;
        int nextX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int nextY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (AbstractWidget cell : cells) {
            int cx = cell.getX();
            int cy = cell.getY();
            if (cx < minX) {
                nextX = minX;
                minX = cx;
            } else if (cx > minX && cx < nextX) {
                nextX = cx;
            }
            if (cy < minY) {
                nextY = minY;
                minY = cy;
            } else if (cy > minY + 2 && cy < nextY) {
                nextY = cy;
            }
            if (cy > maxY) {
                maxY = cy;
            }
        }

        int columns = nextX == Integer.MAX_VALUE ? 1 : 2;
        int vgap = nextY == Integer.MAX_VALUE ? 4 : Math.max(2, nextY - minY - cellH);
        int onLast = 0;
        for (AbstractWidget cell : cells) {
            if (Math.abs(cell.getY() - maxY) <= 2) {
                onLast++;
            }
        }

        int x;
        int y;
        if (onLast < columns && nextX != Integer.MAX_VALUE) {
            x = nextX;
            y = maxY;
        } else {
            x = minX;
            y = maxY + cellH + vgap;
            int push = cellH + vgap;
            int rowTop = y;
            for (GuiEventListener child : List.copyOf(this.children())) {
                if (child instanceof AbstractWidget widget && widget.getY() >= rowTop - 2) {
                    widget.setY(widget.getY() + push);
                }
            }
        }

        int limit = this.height - 8;
        if (y + cellH > limit) {
            int up = y + cellH - limit;
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
        int buttonW = cellW;
        int buttonH = cellH;
        this.addRenderableWidget(Button.builder(
                Component.literal("DBA Settings"),
                btn -> Minecraft.getInstance().setScreenAndShow(new DbaSettingsScreen(host))
        ).bounds(buttonX, buttonY, buttonW, buttonH).build());
    }
}
