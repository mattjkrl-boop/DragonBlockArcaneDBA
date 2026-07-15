package com.dragonblockarcanedba.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LevelRenderer.class)
public class CloudTextureMixin {
    private static final Identifier YELLOW_CLOUDS = Identifier.fromNamespaceAndPath("dragonblockarcanedba", "textures/environment/yellow_clouds.png");

    @ModifyArg(method = "renderClouds", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/Identifier;)V"), index = 1)
    private Identifier changeCloudTexture(Identifier original) {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && client.level.dimension().toString().contains("otherworld")) {
            return YELLOW_CLOUDS;
        }
        return original;
    }
}
