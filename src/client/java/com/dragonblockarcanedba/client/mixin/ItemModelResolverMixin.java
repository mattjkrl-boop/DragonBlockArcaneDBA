package com.dragonblockarcanedba.client.mixin;

import com.dragonblockarcanedba.client.config.DbaConfig;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

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
