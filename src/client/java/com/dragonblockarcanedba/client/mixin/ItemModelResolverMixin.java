package com.dragonblockarcanedba.client.mixin;

import com.dragonblockarcanedba.client.config.DbaConfig;
import com.dragonblockarcanedba.client.render.item.ProceduralWeaponRenderer;
import com.dragonblockarcanedba.client.render.weapon.WeaponModelRegistry;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModelResolver.class)
public class ItemModelResolverMixin {

    @ModifyVariable(method = "getItemModel", at = @At("HEAD"), argsOnly = true)
    private Identifier dba$modifyWeaponModelId(Identifier id) {
        if (DbaConfig.use3dWeapons && id.getNamespace().equals("dragonblockarcanedba")) {
            String path = id.getPath();
            // If it's one of our weapons, swap the identifier to the _3d variant JSON model
            if ((path.equals("devil_trident") || path.equals("power_pole") || path.equals("dimensional_sword") || path.equals("bansho_fan")) && !path.endsWith("_3d")) {
                return Identifier.parse("dragonblockarcanedba:" + path + "_3d");
            }
        }
        return id;
    }
}
