package com.dragonblockarcanedba.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    /**
     * Saber Flurry Target Unpickable Fix:
     * Makes the entity completely un-clickable and allows attacks to pass right through them
     * while the player is wielding the Saber, preventing the crosshair from locking onto them.
     */
    @Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
    private void dba$saberUnpickable(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof LivingEntity living) {
            if (living.hasEffect(com.dragonblockarcanedba.effect.DbaEffects.MOVEMENT_CURSE_HOLDER)) {
                cir.setReturnValue(false); // Do not let the player click or target them with the crosshair!
            }
        }
    }
}
