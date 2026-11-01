package com.dragonblockarcanedba.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ensures cloudColor is valid in the Otherworld so the cloud pass is not skipped,
 * and raises the cloud height to the Otherworld tier.
 *
 * Split out of CloudTextureMixin: the handler takes only CallbackInfo so it matches
 * LevelRenderer.render by name on both 26.2 and 26.3, whose parameter lists differ.
 * This keeps the golden tint working on 26.3 even though the multi-layer cloud
 * renderer itself is 26.2-only (see DbaMixinPlugin).
 */
@Mixin(LevelRenderer.class)
public class CloudColorMixin {
    @Shadow @Final private LevelRenderState levelRenderState;

    @Inject(method = "render", at = @At("HEAD"))
    private void ensureOtherworldCloudColor(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && client.level.dimension().toString().contains("otherworld")) {
            this.levelRenderState.cloudColor = 0xE6FFE070;
            this.levelRenderState.cloudHeight = 210.0f;
        }
    }
}
