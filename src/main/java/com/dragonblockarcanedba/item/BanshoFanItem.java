package com.dragonblockarcanedba.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public class BanshoFanItem extends Item {
    public BanshoFanItem(Properties properties) {
        super(properties.attributes(
            net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                .add(
                    net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        BASE_ATTACK_DAMAGE_ID,
                        5.0,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
                    ),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND
                )
                .build()
        ));
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        Vec3 diff = target.position().subtract(attacker.position()).normalize().scale(2.5);
        target.setDeltaMovement(target.getDeltaMovement().add(diff.x, 0.4, diff.z));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WIND_CHARGE_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        
        if (!level.isClientSide()) {
            WindCharge windCharge = new WindCharge(player, level, player.getX(), player.getEyeY(), player.getZ());
            windCharge.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            if (windCharge instanceof com.dragonblockarcanedba.util.BanshoWindChargeMarker marker) {
                marker.dba$setFromBanshoFan(true);
            }
            level.addFreshEntity(windCharge);
        }
        
        player.getCooldowns().addCooldown(itemStack, 20);
        return InteractionResult.SUCCESS;
    }
}
