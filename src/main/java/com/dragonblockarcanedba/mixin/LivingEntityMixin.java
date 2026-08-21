package com.dragonblockarcanedba.mixin;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.util.TimeTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements com.dragonblockarcanedba.util.DbaAttackerTracker, TimeTracker, com.dragonblockarcanedba.util.DbaLivingEntityInput {

    @org.spongepowered.asm.mixin.Shadow
    protected float xxa;

    @org.spongepowered.asm.mixin.Shadow
    protected float zza;

    @org.spongepowered.asm.mixin.Shadow
    protected boolean jumping;

    // ========== Effect Inflictor Tracking ==========

    @Unique
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

    // ========== Whis Staff Auto-Dodge (50% chance to negate damage) ==========
    // ========== + Damage Mitigation ==========

    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float dba$modifyIncomingDamage(float amount, net.minecraft.server.level.ServerLevel level, DamageSource source) {
        if ((Object) this instanceof Player player) {
            // Hollowed Phasing Immunity: Negate all block suffocation damage
            if (player.hasEffect(com.dragonblockarcanedba.effect.DbaEffects.HOLLOWED_HOLDER) && 
                source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)) {
                return 0.0f;
            }

            // Azure Dragon Rush Fall Damage Immunity
            if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)) {
                if (com.dragonblockarcanedba.item.AzureDragonSwordItem.IS_RUSHING_MAP.containsKey(player.getUUID()) ||
                    player.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.AzureDragonSwordItem) {
                    return 0.0f;
                }
            }

            // Whis Staff Auto-Dodge: 50% chance to negate ALL incoming damage while held
            if (player.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.WhisStaffItem) {
                if (level.getRandom().nextFloat() < 0.50f) {
                    // Dodged! Show white particle burst to indicate the dodge
                    level.sendParticles(
                        new net.minecraft.core.particles.DustParticleOptions(0xFFFFFF, 1.5F),
                        player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(),
                        10, 0.3, 0.5, 0.3, 0.1
                    );
                    // Give brief invulnerability frames (10 ticks = 0.5 seconds)
                    player.invulnerableTime = Math.max(player.invulnerableTime, 10);
                    return 0.0f; // Negate all damage
                }
            }

            // Saber Perfect Dodge: Dodging an incoming attack negates damage and resets Flash Step charges
            if (com.dragonblockarcanedba.item.SaberItem.isPerfectDodgeActive(player)) {
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    com.dragonblockarcanedba.item.SaberItem.triggerPerfectDodge(serverPlayer);
                }
                player.invulnerableTime = Math.max(player.invulnerableTime, 12);
                return 0.0f;
            }

            // Standard damage mitigation if not dodged (defense-based)
            if (!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
                double multiplier = PlayerStats.getDamageMultiplier(player);
                return (float) (amount * multiplier);
            }
        }
        return amount;
    }

    // ========== Arcosian Underwater Breathing ==========

    @Inject(method = "canBreatheUnderwater", at = @At("HEAD"), cancellable = true)
    private void dba$canBreatheUnderwater(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player) {
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
            if (accessor.dba$getRaceId() != null && accessor.dba$getRaceId().getPath().equals("arcosian")) {
                cir.setReturnValue(true);
            }
        }
    }

    // ========== Movement Curse Teleport Blocking ==========

    @Inject(method = "randomTeleport", at = @At("HEAD"), cancellable = true)
    private void dba$onRandomTeleport(double x, double y, double z, boolean showParticles, CallbackInfoReturnable<Boolean> cir) {
        net.minecraft.world.effect.MobEffectInstance curse = ((LivingEntity)(Object)this).getEffect(com.dragonblockarcanedba.effect.DbaEffects.MOVEMENT_CURSE_HOLDER);
        if (curse != null && curse.getAmplifier() >= 9) { // 10 stacks
            cir.setReturnValue(false);
        }
    }

    // ========== Phasing: Allow clicking through chained enemies ==========
    
    @Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
    private void dba$isPickable(CallbackInfoReturnable<Boolean> cir) {
        if (((LivingEntity)(Object)this).hasEffect(com.dragonblockarcanedba.effect.DbaEffects.MOVEMENT_CURSE_HOLDER)) {
            cir.setReturnValue(false); // Unpickable: Crosshair ignores them, allowing clicks to pass through to the next enemy!
        }
    }

    // ========== Custom XP on Kill ==========

    // ========== Prevent Saber Arm Shake ==========

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void dba$onSwingWithBoolean(net.minecraft.world.InteractionHand hand, boolean updateSelf, CallbackInfo ci) {
        if ((Object) this instanceof Player player) {
            if (hand == net.minecraft.world.InteractionHand.MAIN_HAND && player.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.SaberItem) {
                ci.cancel();
            }
        }
    }
    
    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void dba$onSwing(net.minecraft.world.InteractionHand hand, CallbackInfo ci) {
        if ((Object) this instanceof Player player) {
            if (hand == net.minecraft.world.InteractionHand.MAIN_HAND && player.getMainHandItem().getItem() instanceof com.dragonblockarcanedba.item.SaberItem) {
                ci.cancel();
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
            if (entity.getLastHurtByMob() instanceof net.minecraft.server.level.ServerPlayer lastHurtPlayer) {
                killer = lastHurtPlayer;
            }
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

    // ========== Time Reversal System (Whis Staff) ==========

    /** Rolling buffer of recent positions (most recent first). Lazily initialized to prevent heap bloat. */
    @Unique
    private Deque<Vec3> dba$positionHistory = null;

    /** Whether this entity is currently having its movement reversed. */
    @Unique
    private boolean dba$reversing = false;

    /** How many ticks of reversal remain. */
    @Unique
    private int dba$reverseTicks = 0;

    @Override
    public void dba$pushPosition(Vec3 pos) {
        if (dba$positionHistory == null) {
            dba$positionHistory = new ArrayDeque<>(200);
        }
        if (dba$positionHistory.size() >= 200) {
            dba$positionHistory.removeLast();
        }
        dba$positionHistory.addFirst(pos);
    }

    @Override
    public Deque<Vec3> dba$getPositionHistory() {
        return dba$positionHistory;
    }

    @Override
    public void dba$startReversing(int ticks) {
        LivingEntity self = (LivingEntity) (Object) this;
        // If history is empty, synthesize a starting baseline so reversal takes effect immediately
        if (dba$positionHistory == null || dba$positionHistory.isEmpty()) {
            dba$pushPosition(self.position());
            Vec3 back = self.getDeltaMovement().scale(-1.5);
            if (back.lengthSqr() < 0.01) {
                back = self.getLookAngle().scale(-0.8);
            }
            for (int i = 1; i <= Math.min(ticks, 40); i++) {
                dba$pushPosition(self.position().add(back.scale(i * 0.2)));
            }
        }
        dba$reversing = true;
        dba$reverseTicks = ticks;
    }

    @Override
    public boolean dba$isReversing() {
        return dba$reversing;
    }

    /**
     * Tick injection on LivingEntity.tick():
     * - Only tracks positions for players and entities in active temporal distortion.
     * - While reversing: pop positions from history and apply them with noPhysics.
     * - Discards buffer on mobs after reversal to preserve server memory.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void dba$onTick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;

        if (dba$reversing) {
            if (dba$reverseTicks > 0 && dba$positionHistory != null && !dba$positionHistory.isEmpty()) {
                Vec3 pos = dba$positionHistory.pollFirst();
                self.noPhysics = true;
                self.setPos(pos.x, pos.y, pos.z);
                self.setDeltaMovement(Vec3.ZERO);
                self.hurtMarked = true; // Force position sync to clients

                // Stop mob AI pathing during reversal
                if (self instanceof Mob mob) {
                    mob.getNavigation().stop();
                }

                dba$reverseTicks--;
            } else {
                // Reversal complete — restore normal physics
                dba$reversing = false;
                self.noPhysics = false;
                if (!(self instanceof Player)) {
                    dba$positionHistory = null; // Free memory on server mobs
                }
            }
        } else if (self instanceof Player || dba$positionHistory != null) {
            // Track position for players and active combat entities
            dba$pushPosition(self.position());
        }
    }

    @Override
    public float dba$getXxa() {
        return this.xxa;
    }

    @Override
    public float dba$getZza() {
        return this.zza;
    }

    @Override
    public boolean dba$isJumping() {
        return this.jumping;
    }
}

