package com.dragonblockarcanedba.effect;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import com.dragonblockarcanedba.particle.DbaDustOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Heroic Focus — Tapion's holy courage and agility from the Brave Sword.
 * 
 * - Armor (+6.0)
 * - Movement Speed (+20%)
 * - Jump Boost / Strength (+0.4)
 * - Safe Fall Distance (+6 blocks)
 * - Holy golden and emerald particles.
 */
public class HeroicFocusEffect extends MobEffect {
    public HeroicFocusEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFD700); // Gold, matching the Brave Sword valor palette

        this.addAttributeModifier(
            Attributes.ARMOR,
            DragonBlockArcaneDBA.id("effect.heroic_focus.armor"),
            6.0,
            AttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            DragonBlockArcaneDBA.id("effect.heroic_focus.speed"),
            0.20,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );

        this.addAttributeModifier(
            Attributes.JUMP_STRENGTH,
            DragonBlockArcaneDBA.id("effect.heroic_focus.jump"),
            0.40,
            AttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
            Attributes.SAFE_FALL_DISTANCE,
            DragonBlockArcaneDBA.id("effect.heroic_focus.fall_distance"),
            6.0,
            AttributeModifier.Operation.ADD_VALUE
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity.tickCount % 2 == 0) {
            double px = entity.getX() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
            double py = entity.getY() + level.getRandom().nextDouble() * entity.getBbHeight();
            double pz = entity.getZ() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();

            level.sendParticles(
                new DbaDustOptions(0xFFD700, 1.3f), // Gold
                px, py, pz,
                1, 0.0, 0.04, 0.0, 0.01
            );
        }
        return true;
    }
}
