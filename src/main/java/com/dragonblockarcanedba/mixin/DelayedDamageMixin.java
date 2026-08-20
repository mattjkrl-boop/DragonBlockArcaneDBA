package com.dragonblockarcanedba.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class DelayedDamageMixin {

    @Unique
    private float dba$accumulatedDamage = 0.0f;

    @Unique
    private int dba$damageDelayTicks = 0;

    @Unique
    private DamageSource dba$lastDamageSource = null;

    @Unique
    private net.minecraft.world.entity.player.Player dba$lastPlayer = null;

    @Unique
    private boolean dba$isApplyingDelayedDamage = false;

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void dba$onHurtServer(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (dba$isApplyingDelayedDamage) {
            return; // Let the real damage go through when the timer pops
        }

        LivingEntity self = (LivingEntity) (Object) this;
        
        // Only apply this delayed combo logic to entities that are alive
        if (self.isAlive() && amount > 0) {
            // We apply this to any player attacks or projectiles owned by the player
            if (source.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
                dba$accumulatedDamage += amount;
                dba$damageDelayTicks = 10; // 0.5 seconds of combo time
                dba$lastDamageSource = source;
                dba$lastPlayer = player;

                // Automatically apply invisible cinematic tracking effect to track all player weapon damage
                self.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 15, 0, false, false, false
                ), player);

                // Ensure vanilla kill credit & last hurt player are set immediately
                self.setLastHurtByMob(player);

                // Make the entity flash red and play the hurt animation so it feels responsive!
                self.level().broadcastEntityEvent(self, (byte) 2); // 2 is the standard hurt animation
                
                self.hurtMarked = true;

                // Cancel the actual damage application
                cir.setReturnValue(true); // Return true so the caller thinks it succeeded
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void dba$onTick(CallbackInfo ci) {
        if (dba$damageDelayTicks > 0) {
            LivingEntity self = (LivingEntity) (Object) this;
            
            // Check for cinematic CC effects that should lock the damage combo
            boolean isCinematicallyLocked = false;
            
            // Universal Cinematic Tracking Effect (Invisible damage combo lock)
            if (self.hasEffect(com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER)) {
                isCinematicallyLocked = true;
            }


            
            if (isCinematicallyLocked) {
                // Keep the buffer at exactly 0.5s (10 ticks) so it pops immediately after the effect wears off
                dba$damageDelayTicks = 10;
            } else {
                dba$damageDelayTicks--;
            }

            if (dba$damageDelayTicks <= 0 && dba$accumulatedDamage > 0) {
                // Time to apply the massive accumulated damage!
                dba$isApplyingDelayedDamage = true;
                if (self.level() instanceof ServerLevel serverLevel) {
                    DamageSource source = dba$lastDamageSource != null 
                        ? dba$lastDamageSource 
                        : (dba$lastPlayer != null ? serverLevel.damageSources().playerAttack(dba$lastPlayer) : serverLevel.damageSources().generic());
                    
                    if (dba$lastPlayer != null) {
                        self.setLastHurtByMob(dba$lastPlayer);
                    }
                    self.hurtServer(serverLevel, source, dba$accumulatedDamage);
                }
                dba$isApplyingDelayedDamage = false;
                
                // Reset
                dba$accumulatedDamage = 0.0f;
                dba$lastDamageSource = null;
                dba$lastPlayer = null;
            }
        }
    }
}
