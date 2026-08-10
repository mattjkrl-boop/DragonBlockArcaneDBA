package com.dragonblockarcanedba.mixin;

import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.util.BanshoWindChargeMarker;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractWindCharge.class)
public class WindChargeMixin implements BanshoWindChargeMarker {
    
    @Unique
    private boolean dba$isFromBanshoFan = false;

    @Override
    public void dba$setFromBanshoFan(boolean fromBanshoFan) {
        this.dba$isFromBanshoFan = fromBanshoFan;
    }

    @Override
    public boolean dba$isFromBanshoFan() {
        return this.dba$isFromBanshoFan;
    }

    @Inject(method = "onHitEntity", at = @At("HEAD"))
    private void applyBleedingEffect(EntityHitResult result, CallbackInfo ci) {
        if (this.dba$isFromBanshoFan()) {
            Entity hitEntity = result.getEntity();
            if (hitEntity instanceof LivingEntity living) {
                // Apply Bleeding effect for 10 seconds (200 ticks), level 1 (amplifier 0)
                // We pass `(Entity) (Object) this` so our global mixin can automatically track the inflictor!
                living.addEffect(new MobEffectInstance(DbaEffects.BLEEDING_HOLDER, 200, 0, false, false, true), (Entity) (Object) this);
            }
        }
    }
}
