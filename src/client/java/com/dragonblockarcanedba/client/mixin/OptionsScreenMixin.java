package com.dragonblockarcanedba.client.mixin;

import com.dragonblockarcanedba.client.gui.DbaSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {
    protected OptionsScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void dba$addSettingsButton(CallbackInfo ci) {
        this.addRenderableWidget(Button.builder(
            Component.literal("✦ DBA Settings"),
            btn -> Minecraft.getInstance().setScreenAndShow(new DbaSettingsScreen(this))
        ).bounds(this.width - 114, 8, 106, 20).build());
    }
}
