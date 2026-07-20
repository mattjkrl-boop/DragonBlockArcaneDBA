package com.dragonblockarcanedba.client.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends net.minecraft.client.model.EntityModel<? super S>> extends net.minecraft.client.renderer.entity.EntityRenderer<T, S> {

    protected LivingEntityRendererMixin(net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
        super(context);
    }

    @Inject(method = "extractNameTags(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    protected void dba$extractNameTags(T entity, S state, float tickDelta, CallbackInfo ci) {
        // Exclude the current player in first-person
        if (entity instanceof net.minecraft.world.entity.player.Player && entity == net.minecraft.client.Minecraft.getInstance().player) {
            return;
        }

        // Add health bars on all alive, visible living entities (mobs)
        // Requires Ki Sense technique and distance <= 15 blocks
        boolean hasKiSense = false;
        net.minecraft.client.player.LocalPlayer localPlayer = net.minecraft.client.Minecraft.getInstance().player;
        if (localPlayer != null) {
            com.dragonblockarcanedba.attribute.PlayerStatsAccessor accessor = (com.dragonblockarcanedba.attribute.PlayerStatsAccessor) localPlayer;
            hasKiSense = accessor.dba$isTechniqueActive("kisense");
        }
        
        if (hasKiSense && entity.distanceTo(localPlayer) <= 15.0f && entity.isAlive() && !entity.isInvisible()) {
            float health = entity.getHealth();
            float maxHealth = entity.getMaxHealth();

            net.minecraft.network.chat.MutableComponent healthBar = dba$createHealthBarComponent(health, maxHealth);

            // Populate nameTagAttachment if missing
            if (state.nameTagAttachment == null) {
                state.nameTagAttachment = entity.getAttachments().getNullable(
                    net.minecraft.world.entity.EntityAttachment.NAME_TAG, 
                    0, 
                    entity.getYRot(tickDelta)
                );
            }

            // Populate lightCoords if missing
            if (state.lightCoords == 0) {
                state.lightCoords = this.getPackedLightCoords(entity, tickDelta);
            }

            if (state.nameTag != null) {
                // If it already has a custom name (or is a player name), render health bar as scoreText
                state.scoreText = healthBar;
            } else {
                // Otherwise, render health bar directly as the primary nameTag
                state.nameTag = healthBar;
            }
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private static net.minecraft.network.chat.MutableComponent dba$createHealthBarComponent(float health, float maxHealth) {
        int barLength = 10;
        float ratio = health / maxHealth;
        int filledCount = Math.round(ratio * barLength);
        if (filledCount < 0) filledCount = 0;
        if (filledCount > barLength) filledCount = barLength;
        if (health > 0 && filledCount == 0) filledCount = 1; // Show at least 1 bar if alive

        net.minecraft.ChatFormatting color = net.minecraft.ChatFormatting.GREEN;
        if (ratio <= 0.25f) {
            color = net.minecraft.ChatFormatting.RED;
        } else if (ratio <= 0.5f) {
            color = net.minecraft.ChatFormatting.YELLOW;
        }

        net.minecraft.network.chat.MutableComponent bar = net.minecraft.network.chat.Component.empty();
        
        // Heart symbol
        bar.append(net.minecraft.network.chat.Component.literal("❤ ").withStyle(net.minecraft.ChatFormatting.RED));

        // Filled parts
        if (filledCount > 0) {
            bar.append(net.minecraft.network.chat.Component.literal("■".repeat(filledCount)).withStyle(color));
        }
        // Empty parts
        int emptyCount = barLength - filledCount;
        if (emptyCount > 0) {
            bar.append(net.minecraft.network.chat.Component.literal("■".repeat(emptyCount)).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }

        // Numeric text
        String numStr = String.format(" %.0f/%.0f", health, maxHealth);
        bar.append(net.minecraft.network.chat.Component.literal(numStr).withStyle(net.minecraft.ChatFormatting.GRAY));

        return bar;
    }
}
