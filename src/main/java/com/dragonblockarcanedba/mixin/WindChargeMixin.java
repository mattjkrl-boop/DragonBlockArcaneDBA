package com.dragonblockarcanedba.mixin;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.util.BanshoWindChargeMarker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
    private void applyEnhancedEffects(EntityHitResult result, CallbackInfo ci) {
        if (this.dba$isFromBanshoFan()) {
            Entity hitEntity = result.getEntity();
            AbstractWindCharge self = (AbstractWindCharge) (Object) this;

            if (hitEntity instanceof LivingEntity living) {
                // Apply Bleeding III (amplifier 2) for 20 seconds (400 ticks)
                living.addEffect(new MobEffectInstance(DbaEffects.BLEEDING_HOLDER, 400, 2, false, false, true), (Entity) (Object) this);

                // Deal enhanced impact damage: 300 + (owner's Spirit × 1)
                if (self.getOwner() instanceof Player owner && self.level() instanceof ServerLevel serverLevel) {
                    PlayerStatsAccessor accessor = (PlayerStatsAccessor) owner;
                    float impactDamage = 300.0f + (float) accessor.dba$getSpirit();
                    living.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(owner), impactDamage);

                    // Wind explosion particles on impact
                    serverLevel.sendParticles(
                        new net.minecraft.core.particles.DustParticleOptions(0x88FFAA, 2.0F),
                        living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ(),
                        15, 0.5, 0.5, 0.5, 0.2
                    );
                }
            }
        }
    }
}
