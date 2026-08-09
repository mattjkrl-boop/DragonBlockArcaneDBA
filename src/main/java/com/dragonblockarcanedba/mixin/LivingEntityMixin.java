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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.entity.ai.attributes.Attributes;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements com.dragonblockarcanedba.util.DbaAttackerTracker {

    @org.spongepowered.asm.mixin.Unique
    private final java.util.Map<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>, Player> dba$effectInflictors = new java.util.HashMap<>();

    @Override
    public Player dba$getEffectInflictor(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        return this.dba$effectInflictors.get(effect);
    }

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"))
    private void dba$onAddEffect(net.minecraft.world.effect.MobEffectInstance effectInstance, net.minecraft.world.entity.Entity source, CallbackInfoReturnable<Boolean> cir) {
        Player inflictor = null;
        if (source instanceof Player player) {
            inflictor = player;
        } else if (source instanceof net.minecraft.world.entity.projectile.Projectile proj && proj.getOwner() instanceof Player player) {
            inflictor = player;
        } else {
            LivingEntity entity = (LivingEntity) (Object) this;
            net.minecraft.world.entity.Entity killer = entity.getKillCredit();
            if (killer instanceof Player player) {
                inflictor = player;
            }
        }
        
        if (inflictor != null) {
            this.dba$effectInflictors.put(effectInstance.getEffect(), inflictor);
        }
    }

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

    @Inject(method = "die", at = @At("HEAD"))
    private void dba$onDeathCustomXp(DamageSource damageSource, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (entity.level().isClientSide()) {
            return;
        }

        net.minecraft.world.entity.Entity killer = damageSource.getEntity();
        if (killer == null || !(killer instanceof net.minecraft.server.level.ServerPlayer)) {
            killer = entity.getKillCredit();
        }

        if (killer == null || !(killer instanceof net.minecraft.server.level.ServerPlayer)) {
            for (net.minecraft.world.effect.MobEffectInstance instance : entity.getActiveEffects()) {
                Player inflictor = this.dba$effectInflictors.get(instance.getEffect());
                if (inflictor instanceof net.minecraft.server.level.ServerPlayer) {
                    killer = inflictor;
                    break;
                }
            }
        }

        if (killer instanceof net.minecraft.server.level.ServerPlayer player) {
            double maxHealth = 0.0;
            if (entity.getAttribute(Attributes.MAX_HEALTH) != null) {
                maxHealth = entity.getAttributeValue(Attributes.MAX_HEALTH);
            }

            double armor = 0.0;
            if (entity.getAttribute(Attributes.ARMOR) != null) {
                armor = entity.getAttributeValue(Attributes.ARMOR);
            }

            double attackDamage = 0.0;
            if (entity.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                attackDamage = entity.getAttributeValue(Attributes.ATTACK_DAMAGE);
            }

            double baseSurvivability = (maxHealth * 0.5) + (armor * 1.5);
            double lethalityMultiplier = 1.0 + (attackDamage * 0.25);

            int customXpAwarded = (int) Math.round(baseSurvivability * lethalityMultiplier);
            if (customXpAwarded < 1) customXpAwarded = 1;

            PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
            accessor.dba$addXp(customXpAwarded);
            accessor.dba$syncStats();
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a+" + customXpAwarded + " DBA XP"), true);
        }
    }
}
