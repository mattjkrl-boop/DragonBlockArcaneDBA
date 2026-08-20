package com.dragonblockarcanedba.mixin;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(method = "usesAuthentication", at = @At("HEAD"), cancellable = true)
    private void dba$disableLanOnlineMode(CallbackInfoReturnable<Boolean> cir) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        // Only disable authentication for Integrated/LAN servers when running in the dev/script environment
        if (!server.isDedicatedServer() && FabricLoader.getInstance().isDevelopmentEnvironment()) {
            cir.setReturnValue(false);
        }
    }
}
