package com.dragonblockarcanedba.effect;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import com.dragonblockarcanedba.util.DbaDamage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Dark Faded — Void Corruption & Blindness from Hollow's Edge.
 * 
 * - Reduces speed by 35%.
 * - Inflicts Darkness.
 * - Deals periodic Void/Magic damage and spawns pitch black particles.
 */
public class DarkFadedEffect extends MobEffect {
    public DarkFadedEffect() {
        super(MobEffectCategory.HARMFUL, 0x0A0014, com.dragonblockarcanedba.particle.DbaParticles.VOID_SMOKE); // particles are spawned client side by vanilla effect particle sync

        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            DragonBlockArcaneDBA.id("effect.dark_faded.speed"),
            -0.35,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );

        this.addAttributeModifier(
            Attributes.ATTACK_DAMAGE,
            DragonBlockArcaneDBA.id("effect.dark_faded.damage"),
            -0.25,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {

        // Void tick damage every 10 ticks
        if (entity.tickCount % 10 == 0) {
            float damage = 35.0f + (amplifier * 15.0f);
            // Bespoke: Hollow's Edge void corruption is void family. The
            // effect stores no attacker, so the source anchors on the
            // afflicted entity with no attacker.
            entity.hurtServer(level, DbaDamage.voidDmg(entity, null), damage);
        }
        return true;
    }
}
