package com.dragonblockarcanedba.mixin;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RangedAttribute.class)
public class RangedAttributeMixin {
    @Inject(method = "sanitizeValue", at = @At("HEAD"), cancellable = true)
    private void dba$uncapSanitizeValue(double value, CallbackInfoReturnable<Double> cir) {
        RangedAttribute attr = (RangedAttribute) (Object) this;
        if (Double.isNaN(value)) {
            cir.setReturnValue(attr.getMinValue());
        } else if (value < attr.getMinValue()) {
            cir.setReturnValue(attr.getMinValue());
        } else {
            // Uncap upper bound for high-scaling attributes (Max Health, Attack Damage, etc.)
            // Vanilla caps MAX_HEALTH at 1024.0; we uncap to 1,000,000,000.0 (1 Billion)
            cir.setReturnValue(Math.min(value, 1_000_000_000.0));
        }
    }

    @Inject(method = "getMaxValue", at = @At("HEAD"), cancellable = true)
    private void dba$uncapMaxValue(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(1_000_000_000.0);
    }
}
