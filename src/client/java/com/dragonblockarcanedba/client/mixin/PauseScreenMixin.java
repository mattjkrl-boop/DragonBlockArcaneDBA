package com.dragonblockarcanedba.client.mixin;

import com.dragonblockarcanedba.client.gui.DbaMenuScreen;
import com.dragonblockarcanedba.client.gui.DbaSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void dba$addPauseButtons(CallbackInfo ci) {
        int bw = 106;
        int bx = this.width - bw - 8;

        this.addRenderableWidget(Button.builder(
            Component.literal("✦ DBA Menu (V)"),
            btn -> Minecraft.getInstance().setScreenAndShow(new DbaMenuScreen())
        ).bounds(bx, 8, bw, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("⚙ DBA Settings"),
            btn -> Minecraft.getInstance().setScreenAndShow(new DbaSettingsScreen(this))
        ).bounds(bx, 32, bw, 20).build());
    }
}
