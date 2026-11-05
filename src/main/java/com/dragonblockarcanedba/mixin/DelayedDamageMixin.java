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

    // Scope 2 (D9): the source of the largest single buffered hit. The pop used to
    // rebuild every delayed hit as a plain player attack, which silently stripped
    // bespoke damage types (death messages, armor behavior, damage type tags) from
    // every weapon ability that lands through this buffer. The dominant hit's own
    // source now survives the delay; the rebuild below stays only as a fallback.
    @Unique
    private DamageSource dba$dominantSource = null;

    @Unique
    private float dba$dominantAmount = 0.0f;

    @Unique
    private int dba$damageDelayTicks = 0;

    @Unique
    private java.util.UUID dba$lastPlayerUuid = null;

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
                dba$lastPlayerUuid = player.getUUID();
                if (amount > dba$dominantAmount) {
                    dba$dominantAmount = amount;
                    dba$dominantSource = source;
                }

                // Automatically apply invisible cinematic tracking effect to track all player weapon damage
                self.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER, 15, 0, false, false, false
                ), player);

                // Ensure vanilla kill credit & last hurt player are set immediately
                self.setLastHurtByMob(player);

                // Make the entity flash red and play the hurt animation so it feels responsive!
                self.level().broadcastEntityEvent(self, (byte) 2); // 2 is the standard hurt animation
                
                com.dragonblockarcanedba.util.DbaCompat.markHurt(self);

                // Cancel the actual damage application
                cir.setReturnValue(true); // Return true so the caller thinks it succeeded
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void dba$onTick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        
        // Check for cinematic CC effects that should lock the damage combo
        boolean isCinematicallyLocked = self.hasEffect(com.dragonblockarcanedba.effect.DbaEffects.CINEMATIC_TRACKING_HOLDER)
            || self.hasEffect(com.dragonblockarcanedba.effect.DbaEffects.TEMPORAL_STASIS_HOLDER)
            || self.hasEffect(com.dragonblockarcanedba.effect.DbaEffects.JUDGEMENT_LOCK_HOLDER)
            || self.hasEffect(com.dragonblockarcanedba.effect.DbaEffects.SPIRIT_IMPALE_HOLDER)
            || self.hasEffect(com.dragonblockarcanedba.effect.DbaEffects.VALOR_STUN_HOLDER)
            || self.hasEffect(com.dragonblockarcanedba.effect.DbaEffects.POLE_STUN_HOLDER)
            || self.hasEffect(com.dragonblockarcanedba.effect.DbaEffects.FISSURE_STUN_HOLDER)
            || self.hasEffect(com.dragonblockarcanedba.effect.DbaEffects.PETRIFICATION_CURSE_HOLDER)
            || self.hasEffect(com.dragonblockarcanedba.effect.DbaEffects.MOVEMENT_CURSE_HOLDER)
            || self.hasEffect(com.dragonblockarcanedba.effect.DbaEffects.SORROW_RIFT_HOLDER)
            || self.hasEffect(com.dragonblockarcanedba.effect.DbaEffects.DEVILS_HANDS_HOLDER)
            || self.hasEffect(com.dragonblockarcanedba.effect.DbaEffects.MARKED_BY_EVIL_HOLDER)
            || self.hasEffect(com.dragonblockarcanedba.effect.DbaEffects.SILENT_MARK_HOLDER)
            || self.hasEffect(com.dragonblockarcanedba.effect.DbaEffects.EARTH_SHATTER_HOLDER)
            || self.hasEffect(com.dragonblockarcanedba.effect.DbaEffects.ANCIENT_WEIGHT_HOLDER);

        if (isCinematicallyLocked) {
            if (dba$lastPlayerUuid == null && self.getLastHurtByMob() instanceof net.minecraft.world.entity.player.Player p) {
                dba$lastPlayerUuid = p.getUUID();
            }

        }

        if (dba$damageDelayTicks > 0) {
            if (isCinematicallyLocked) {
                // Keep the buffer at exactly 0.5s (10 ticks) so it pops immediately after the effect wears off
                dba$damageDelayTicks = 10;
            } else {
                dba$damageDelayTicks--;
            }

            if (dba$damageDelayTicks <= 0 && dba$accumulatedDamage > 0) {
                // Time to apply the massive accumulated damage!
                // The barrier flag is only set for this reapply, so the preview hurt
                // (which this mixin cancels) does not also spend ki.
                dba$isApplyingDelayedDamage = true;
                com.dragonblockarcanedba.ki.KiBarrier.deliveringDeferredDamage = true;
                try {
                    if (self.level() instanceof ServerLevel serverLevel) {
                        net.minecraft.world.entity.player.Player attacker = dba$lastPlayerUuid != null ? serverLevel.getPlayerByUUID(dba$lastPlayerUuid) : null;
                        DamageSource source;
                        net.minecraft.world.entity.Entity storedAttacker = dba$dominantSource != null ? dba$dominantSource.getEntity() : null;
                        if (dba$dominantSource != null && storedAttacker != null && !storedAttacker.isRemoved()
                                && (!(storedAttacker instanceof LivingEntity livingAttacker) || livingAttacker.isAlive())) {
                            // The dominant buffered hit lands under its own source, so its
                            // bespoke damage type survives. A despawned direct entity on the
                            // stored source is fine: vanilla sources tolerate dead projectiles.
                            source = dba$dominantSource;
                            if (storedAttacker instanceof LivingEntity livingStored) {
                                self.setLastHurtByMob(livingStored);
                            }
                        } else if (attacker != null && !attacker.isRemoved() && attacker.isAlive()) {
                            source = serverLevel.damageSources().playerAttack(attacker);
                            self.setLastHurtByMob(attacker);
                        } else {
                            source = serverLevel.damageSources().generic();
                        }

                        self.hurtServer(serverLevel, source, dba$accumulatedDamage);
                    }
                } finally {
                    dba$isApplyingDelayedDamage = false;
                    com.dragonblockarcanedba.ki.KiBarrier.deliveringDeferredDamage = false;
                }
                
                // Reset
                dba$accumulatedDamage = 0.0f;
                dba$lastPlayerUuid = null;
                dba$dominantSource = null;
                dba$dominantAmount = 0.0f;
            }
        }
    }
}
