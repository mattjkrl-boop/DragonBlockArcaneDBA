package com.dragonblockarcanedba.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> {
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;
    @Shadow protected int imageHeight;
    @Shadow protected T menu;

    private boolean dba$shouldApplyTheme() {
        // Safe explicit instanceof checks for custom screens
        if (this.getClass().getName().contains("GravityTrainingScreen")) return false;
        if (this.getClass().getName().contains("DbaMenuScreen")) return false;
        if (this.getClass().getName().contains("WishScreen")) return false;
        if (this.getClass().getName().contains("SpacePodScreen")) return false;
        if (this.getClass().getName().contains("ReviveScreen")) return false;
        if (this.getClass().getName().contains("RaceSelectionScreen")) return false;
        return true;
    }

}
