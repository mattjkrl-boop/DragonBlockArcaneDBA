package com.dragonblockarcanedba.client.mixin;

import com.dragonblockarcanedba.client.fx.DbaPostFx;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.3 and up variant of GameRendererPostMixin. GameRenderer.render() takes no
 * parameters on 26.3, and LevelRenderer.doEntityOutline() was replaced by
 * blitEntityOutline(), which sits at the equivalent point in the frame (after the
 * level pass, before the GUI). DbaMixinPlugin applies exactly one of the two
 * variants based on the running Minecraft version.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererPostMixin263 {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private CrossFrameResourcePool resourcePool;
    @Shadow @Final private com.mojang.blaze3d.pipeline.RenderTarget mainRenderTarget;

    @Inject(method = "render", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/LevelRenderer;blitEntityOutline()V", shift = At.Shift.AFTER))
    private void dba$postEffects(CallbackInfo ci) {
        DbaPostFx.render(this.minecraft, this.mainRenderTarget, this.resourcePool);
    }
}
