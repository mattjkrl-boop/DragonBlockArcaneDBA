package com.dragonblockarcanedba.effect;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Bleeding — Late-game percentage-based DoT from the Bansho Fan.
 * 
 * Deals 0.5% of target's max HP as magic damage per tick interval.
 * Against a 500k HP target = 2,500 damage per tick.
 * 
 * Also applies:
 *   - Armor reduction: -10% per level (wind cuts shred defenses)
 *   - Prevents natural regeneration while active
 */
public class BleedingEffect extends MobEffect {
    public BleedingEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B0000); // Dark red color

        // Armor reduction: -10% per level (wind cuts shred defenses)
        this.addAttributeModifier(
            Attributes.ARMOR,
            DragonBlockArcaneDBA.id("effect.bleeding.armor"),
            -0.10,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true; // Tick every tick so we can spawn dripping particles and block regen
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        int interval = Math.max(1, 40 >> amplifier);

        // Prevent natural regeneration while bleeding
        // Reset the entity's last hurt timestamp to prevent regen
        if (entity.tickCount % 10 == 0) {
            entity.setLastHurtByMob(null); // Reset to block passive regen triggers
        }

        // Visual dripping effect (every 4 ticks to avoid particle spam)
        if (entity.tickCount % 4 == 0) {
            level.sendParticles(
                new DustParticleOptions(0x8B0000, 1.2F),
                entity.getX() + (level.getRandom().nextFloat() - 0.5) * entity.getBbWidth(), 
                entity.getY() + level.getRandom().nextFloat() * entity.getBbHeight(), 
                entity.getZ() + (level.getRandom().nextFloat() - 0.5) * entity.getBbWidth(),
                1, // count
                0.0, -0.05, 0.0, // delta (simulate dripping down)
                0.0 // speed
            );
            // Secondary splatter particles
            level.sendParticles(
                new DustParticleOptions(0xCC0000, 0.8F),
                entity.getX() + (level.getRandom().nextFloat() - 0.5) * entity.getBbWidth() * 0.8,
                entity.getY() + 0.1,
                entity.getZ() + (level.getRandom().nextFloat() - 0.5) * entity.getBbWidth() * 0.8,
                1,
                0.1, 0.0, 0.1,
                0.01
            );
        }

        // Apply percentage-based damage
        if (entity.tickCount % interval == 0) {
            // 0.5% of target's max HP as magic damage per tick
            // vs 500k HP = 2,500 per tick — devastating over time
            float percentDamage = entity.getMaxHealth() * 0.005f;
            // Minimum 2.0 damage so it still works on low-HP entities
            float damage = Math.max(2.0f, percentDamage);
            entity.hurtServer(level, level.damageSources().magic(), damage);
        }
        return true;
    }
}
