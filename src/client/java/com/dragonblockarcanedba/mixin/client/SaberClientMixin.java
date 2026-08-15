package com.dragonblockarcanedba.mixin.client;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class SaberClientMixin {

    /**
     * Saber Flurry Visual Fix (Client-Side):
     * To make them render as ghosts ONLY for the user wielding the Saber, we must trick the local renderer into
     * believing the entity is invisible but NOT invisible to the player.
     * In 1.21.4 (Chaos Cubed), we do this by directly modifying the extracted EntityRenderState!
     */
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void dba$saberForceGhostState(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client.player != null && client.player.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.SaberItem) {
            if (entity.hasEffect(com.dragonblockarcanedba.effect.DbaEffects.MOVEMENT_CURSE_HOLDER)) {
                state.isInvisible = true; // Tell renderer it's invisible
                state.isInvisibleToPlayer = false; // But NOT to us, so it falls back to 20% translucent ghost rendering!
            }
        }
    }
}
