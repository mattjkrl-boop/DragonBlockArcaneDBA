package com.dragonblockarcanedba.effect;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import com.dragonblockarcanedba.particle.DbaMaterialOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Hollowed — Void Phasing & Translucency State from Hollow's Edge.
 * 
 * - Makes the player ~90% translucent / see-through.
 * - Massive movement speed boost (+60%).
 * - Mobs cannot target or see the player.
 * - Phasing through blocks without suffocation damage.
 */
public class HollowedEffect extends MobEffect {
    public HollowedEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x1A0033, DbaMaterialOptions.of(DbaMaterialOptions.PLASMA, 0x591A80, 2.0f, 0xD00D)); // particles are spawned client side by vanilla effect particle sync

        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            DragonBlockArcaneDBA.id("effect.hollowed.speed"),
            0.60,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {

        // Suppress mob targeting if player is hollowed
        if (entity instanceof Player) {
            com.dragonblockarcanedba.util.DbaCompat.setInvulnerableTime(entity, Math.max(com.dragonblockarcanedba.util.DbaCompat.getInvulnerableTime(entity), 5));
        }
        return true;
    }
}
