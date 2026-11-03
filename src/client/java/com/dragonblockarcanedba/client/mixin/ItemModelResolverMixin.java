package com.dragonblockarcanedba.client.mixin;

import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemModelResolver.class)
public class ItemModelResolverMixin {

    @ModifyVariable(method = "getItemModel", at = @At("HEAD"), argsOnly = true)
    private Identifier dba$modifyWeaponModelId(Identifier id) {
        if (id.getNamespace().equals("dragonblockarcanedba")) {
            String path = id.getPath();
            // These four weapons ship a flat item model and a block-model _3d variant.
            // Held and world rendering always uses the 3D variant. GUI and item-frame
            // contexts stay flat inside that variant's own model selector.
            if ((path.equals("devil_trident") || path.equals("power_pole") || path.equals("dimensional_sword") || path.equals("bansho_fan")) && !path.endsWith("_3d")) {
                return Identifier.parse("dragonblockarcanedba:" + path + "_3d");
            }
        }
        return id;
    }
}
