package com.dragonblockarcanedba.mixin.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin {
    @Inject(method = "usesAuthentication", at = @At("HEAD"), cancellable = true)
    private void dba$disableLanOnlineMode(CallbackInfoReturnable<Boolean> cir) {
        // Only disable Mojang session server verification when running in the development / script launch environment
        // so normal launcher / production downloads keep full vanilla Mojang authentication intact.
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            cir.setReturnValue(false);
        }
    }
}
