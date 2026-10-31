package com.dragonblockarcanedba.client.mixin;

import com.dragonblockarcanedba.client.fx.DbaPostFx;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererPostMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private CrossFrameResourcePool resourcePool;
    @Shadow @Final private com.mojang.blaze3d.pipeline.RenderTarget mainRenderTarget;

    @Inject(method = "render", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V", shift = At.Shift.AFTER))
    private void dba$postEffects(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        DbaPostFx.render(this.minecraft, this.mainRenderTarget, this.resourcePool);
    }
}
