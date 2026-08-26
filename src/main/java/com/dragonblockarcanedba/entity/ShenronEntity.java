package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.network.WishMenuOpenPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ShenronEntity extends Mob {
    private boolean wishGranted = false;

    public ShenronEntity(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 100.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, new WishMenuOpenPayload(this.getId()));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    public void grantWish(ServerPlayer player, String wishType) {
        if (this.isRemoved() || this.wishGranted) return;
        this.wishGranted = true;

        com.dragonblockarcanedba.attribute.PlayerStatsAccessor accessor = (com.dragonblockarcanedba.attribute.PlayerStatsAccessor) player;
        
        switch (wishType) {
            case "wealth" -> {
                player.addItem(new net.minecraft.world.item.ItemStack(com.dragonblockarcanedba.item.DbaItems.SILVER_ZENI, 64));
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("\u00a76Shenron: \u00a7aYour wish for wealth has been granted."));
            }
            case "power" -> {
                accessor.dba$setStatPoints(accessor.dba$getStatPoints() + 150);
                accessor.dba$syncStats();
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("\u00a76Shenron: \u00a7aYour wish for power has been granted."));
            }
            case "immortality" -> {
                player.addItem(new net.minecraft.world.item.ItemStack(com.dragonblockarcanedba.item.DbaItems.SENZU_BEAN, 16));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    com.dragonblockarcanedba.effect.DbaEffects.CELESTIAL_GRACE_HOLDER, 12000, 0
                ));
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("\u00a76Shenron: \u00a7aYour wish for immortality has been granted."));
            }
            default -> {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("\u00a76Shenron: \u00a7cThat wish is beyond my power."));
                this.wishGranted = false;
                return;
            }
        }
        
        if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            net.minecraft.world.entity.LightningBolt lightning = net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT.create(
                serverLevel, net.minecraft.world.entity.EntitySpawnReason.EVENT
            );
            if (lightning != null) {
                lightning.setPos(this.getX(), this.getY(), this.getZ());
                serverLevel.addFreshEntity(lightning);
            }

            // Audio cues for wish fulfillment & departure
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, net.minecraft.sounds.SoundSource.NEUTRAL, 2.0f, 1.2f);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sounds.SoundSource.NEUTRAL, 1.5f, 1.0f);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.ENDER_DRAGON_FLAP, net.minecraft.sounds.SoundSource.HOSTILE, 1.8f, 0.8f);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.PORTAL_TRAVEL, net.minecraft.sounds.SoundSource.NEUTRAL, 1.5f, 0.7f);

            serverLevel.getServer().getPlayerList().broadcastSystemMessage(
                net.minecraft.network.chat.Component.literal("\u00a76Shenron: \u00a7eFarewell!"), false
            );
        }
        
        this.discard();
    }

    @Override
    public void travel(Vec3 travelVector) {
        // Handled manually in tick()
        this.setDeltaMovement(Vec3.ZERO);
        this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isNoAi() {
        return true;
    }
}
