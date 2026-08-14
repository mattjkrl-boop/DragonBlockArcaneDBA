package com.dragonblockarcanedba.mixin;

import com.dragonblockarcanedba.effect.DbaEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityTeleportMixin {

    @Inject(method = "teleportTo(DDD)V", at = @At("HEAD"), cancellable = true)
    private void dba$onTeleportTo(double x, double y, double z, CallbackInfo ci) {
        if ((Object) this instanceof LivingEntity living) {
            MobEffectInstance curse = living.getEffect(DbaEffects.MOVEMENT_CURSE_HOLDER);
            if (curse != null && curse.getAmplifier() >= 9) { // 10 stacks
                ci.cancel();
            }
        }
    }

    @Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z", at = @At("HEAD"), cancellable = true)
    private void dba$onTeleportToServerLevel(net.minecraft.server.level.ServerLevel level, double x, double y, double z, java.util.Set<?> relativeMovements, float yRot, float xRot, boolean p_343603_, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof LivingEntity living) {
            MobEffectInstance curse = living.getEffect(DbaEffects.MOVEMENT_CURSE_HOLDER);
            if (curse != null && curse.getAmplifier() >= 9) { // 10 stacks
                cir.setReturnValue(false);
            }
        }
    }
}
