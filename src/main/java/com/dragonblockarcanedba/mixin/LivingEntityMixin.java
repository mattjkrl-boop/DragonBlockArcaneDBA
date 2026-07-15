package com.dragonblockarcanedba.mixin;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float dba$modifyIncomingDamage(float amount, net.minecraft.server.level.ServerLevel level, DamageSource source) {
        if ((Object) this instanceof Player player) {
            // Mitigate damage if it does not bypass armor (like magic, void, fire etc)
            if (!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
                double multiplier = PlayerStats.getDamageMultiplier(player);
                return (float) (amount * multiplier);
            }
        }
        return amount;
    }

    @Inject(method = "canBreatheUnderwater", at = @At("HEAD"), cancellable = true)
    private void dba$canBreatheUnderwater(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player) {
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
            if (accessor.dba$getRaceId() != null && accessor.dba$getRaceId().getPath().equals("arcosian")) {
                cir.setReturnValue(true);
            }
        }
    }
}
