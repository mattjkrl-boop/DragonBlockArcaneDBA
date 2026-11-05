package com.dragonblockarcanedba.client.mixin;

import com.dragonblockarcanedba.client.fx.DomainFog;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The one fog hook (Phase 5): post-process the FogData vanilla
 * just computed, so an active ability domain can tint the fog and
 * pull it closer (client/fx/DomainFog). setupFog has the same
 * signature on 26.2 and 26.3, so one mixin serves both; the mixin
 * plugin does not gate it. When no domain is live the hook is a
 * single weight check and vanilla fog is untouched.
 */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void dba$domainFog(Camera camera, int renderDistanceChunks, DeltaTracker deltaTracker,
                               float darkenWorldAmount, ClientLevel level,
                               CallbackInfoReturnable<FogData> cir) {
        DomainFog.apply(cir.getReturnValue());
    }
}
